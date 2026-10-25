<?php
/**
 * lyrics.php — song lyrics proxy for J2ME clients (CLDC/MIDP devices).
 *
 *   GET ?op=lyrics&a=<artist>&t=<title>
 *     → {"ok":1,"a":"Legião Urbana","t":"Tempo Perdido","l":"...","src":"letras"}
 *     → {"ok":0,"e":"not found"}
 *
 * The song is resolved through letras.mus.br's public search index
 * (solr.sscdn.co). The site itself refuses requests from this server, so the
 * page is read through r.jina.ai, which returns the rendered HTML. When that
 * fails, api.lyrics.ovh is used as a fallback. Results are cached for a month
 * (misses for half an hour) so the upstream services are only touched once per
 * song.
 *
 * Artist/title come from ID3 tags or file names, so they are cleaned before
 * the search: track numbers, extensions and "(Official Video)" style suffixes
 * are dropped, accents and punctuation are folded, and "Artist - Title" is
 * split when the artist is missing.
 */

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
@set_time_limit(60);

const LYR_SOLR = 'https://solr.sscdn.co/letras/m1/';
const LYR_JINA = 'https://r.jina.ai/';
const LYR_OVH = 'https://api.lyrics.ovh/v1/';
const LYR_UA = 'MIDPlay/1.0 (J2ME; +http://opentty.fun)';
const LYR_CACHE_TTL = 2592000;  // 30 days for a hit from letras.mus.br
const LYR_OVH_TTL = 259200;     // 3 days for the fallback, to retry letras later
const LYR_MISS_TTL = 1800;      // 30 minutes for a miss
const LYR_RATE_LIMIT = 60;      // requests per minute per client IP

// ---------------------------------------------------------------------------
// Small helpers

function lyr_fail($msg, $http = 200) {
    http_response_code($http);
    echo json_encode(array('ok' => 0, 'e' => (string)$msg));
    exit;
}

function lyr_rate_limit() {
    $ip = $_SERVER['REMOTE_ADDR'] ?? 'cli';
    $dir = sys_get_temp_dir() . '/lyricsrl';
    if (!is_dir($dir)) { @mkdir($dir, 0777, true); }
    $file = $dir . '/' . md5($ip) . '.json';
    $now = time();
    $window = array();
    if (is_file($file)) {
        $window = json_decode(@file_get_contents($file), true);
        if (!is_array($window)) { $window = array(); }
    }
    $window = array_values(array_filter($window, function ($t) use ($now) { return $t > $now - 60; }));
    if (count($window) >= LYR_RATE_LIMIT) {
        header('Retry-After: 60');
        lyr_fail('rate limited', 429);
    }
    $window[] = $now;
    @file_put_contents($file, json_encode($window));
}

function lyr_str($name, $max = 200) {
    $v = isset($_GET[$name]) ? trim((string)$_GET[$name]) : '';
    if (strlen($v) > $max) { $v = substr($v, 0, $max); }
    return $v;
}

function lyr_cache_file($key) {
    $dir = sys_get_temp_dir() . '/lyricscache';
    if (!is_dir($dir)) { @mkdir($dir, 0777, true); }
    return $dir . '/' . md5($key) . '.json';
}

function lyr_cache_get($key) {
    $file = lyr_cache_file($key);
    if (!is_file($file)) { return null; }
    $data = json_decode(@file_get_contents($file), true);
    if (!is_array($data) || !isset($data['e']) || !isset($data['j'])) { return null; }
    if ((int)$data['e'] < time()) { @unlink($file); return null; }
    return (string)$data['j'];
}

function lyr_cache_put($key, $json, $ttl) {
    $data = array('e' => time() + $ttl, 'j' => (string)$json);
    @file_put_contents(lyr_cache_file($key), json_encode($data));
}

/** Fetches a URL and returns the body, or null on HTTP/transport failure. */
function lyr_fetch($url, $accept = null, $timeout = 30, $extraHeaders = array()) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, $timeout);
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 10);
    curl_setopt($ch, CURLOPT_USERAGENT, LYR_UA);
    $headers = array();
    if ($accept !== null) { $headers[] = 'Accept: ' . $accept; }
    foreach ($extraHeaders as $h) { $headers[] = $h; }
    if (count($headers) > 0) { curl_setopt($ch, CURLOPT_HTTPHEADER, $headers); }
    $body = curl_exec($ch);
    $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    if ($body === false || $code < 200 || $code >= 300) { return null; }
    return $body;
}

// ---------------------------------------------------------------------------
// Metadata cleanup and matching

/** Lowercase ASCII letters and digits only, so accents/punctuation do not matter. */
function lyr_norm($s) {
    $s = (string)$s;
    if (function_exists('iconv')) {
        $converted = @iconv('UTF-8', 'ASCII//TRANSLIT//IGNORE', $s);
        if ($converted !== false) { $s = $converted; }
    }
    $s = strtolower($s);
    $s = preg_replace('/[^a-z0-9]+/', ' ', $s);
    return trim(preg_replace('/\s+/', ' ', $s));
}

/** Removes file-name noise that would otherwise break the search. */
function lyr_clean($s) {
    $s = str_replace('_', ' ', (string)$s);
    $s = preg_replace('/\.(mp3|m4a|ogg|wav|flac|aac|opus|wma)$/i', '', $s);
    $cleaned = preg_replace(
        '/[\(\[]([^\)\]]*(official|oficial|v[ií]deo|audio|[aá]udio|lyric|letra|clipe|remaster|ao vivo|live|hd|hq|feat|part)[^\)\]]*)[\)\]]/iu',
        '',
        $s);
    if ($cleaned !== null) { $s = $cleaned; }
    $s = preg_replace('/^\s*\d{1,3}[\s.\-]+/', '', $s);
    $s = trim(preg_replace('/\s+/', ' ', $s));
    return trim($s, " -_");
}

/** True when the normalized title is present in the result title (or vice versa). */
function lyr_title_score($query, $candidate) {
    if ($query === '' || strlen($query) < 2) { return -1; }
    if ($candidate === $query) { return 20; }
    if (strpos($candidate, $query) === 0) { return 12; }
    if (strpos(' ' . $candidate . ' ', ' ' . $query . ' ') !== false) { return 12; }
    if (strlen($candidate) >= 3 && strpos($query, $candidate) !== false) { return 8; }
    return -1;
}

function lyr_artist_score($query, $candidate) {
    if ($query === '') { return 0; }
    if ($candidate === $query) { return 15; }
    if (strpos($candidate, $query) !== false || strpos($query, $candidate) !== false) { return 6; }
    return 0;
}

/** Queries the letras.mus.br search index and returns {artist,title,path} or null. */
function lyr_resolve($artist, $title) {
    $query = lyr_norm(trim($artist . ' ' . $title));
    if ($query === '') { return null; }
    $url = LYR_SOLR . '?q=' . urlencode($query) . '&limit=10';
    $body = lyr_fetch($url, 'application/json, text/javascript, */*', 15);
    if ($body === null) { return null; }
    $open = strpos($body, '(');
    $close = strrpos($body, ')');
    if ($open === false || $close === false || $close <= $open) { return null; }
    $data = json_decode(substr($body, $open + 1, $close - $open - 1), true);
    if (!is_array($data) || !isset($data['response']['docs']) || !is_array($data['response']['docs'])) {
        return null;
    }
    $wantArtist = lyr_norm($artist);
    $wantTitle = lyr_norm($title);
    $best = null;
    $bestScore = -1.0;
    foreach ($data['response']['docs'] as $doc) {
        if (!isset($doc['art'], $doc['txt'], $doc['dns'], $doc['url'])) { continue; }
        $titleScore = lyr_title_score($wantTitle, lyr_norm($doc['txt']));
        if ($titleScore < 0) { continue; }
        $score = $titleScore + lyr_artist_score($wantArtist, lyr_norm($doc['art']));
        if (isset($doc['score'])) { $score += ((float)$doc['score']) / 100.0; }
        if ($score > $bestScore) {
            $bestScore = $score;
            $best = $doc;
        }
    }
    if ($best === null) { return null; }
    $dns = preg_replace('/[^a-z0-9\-]/', '', strtolower((string)$best['dns']));
    $id = preg_replace('/[^a-z0-9\-]/', '', strtolower((string)$best['url']));
    if ($dns === '' || $id === '') { return null; }
    return array(
        'artist' => (string)$best['art'],
        'title' => (string)$best['txt'],
        'path' => $dns . '/' . $id . '/',
    );
}

// ---------------------------------------------------------------------------
// Lyrics sources

/** Reads the lyrics block out of the rendered letras.mus.br page. */
function lyr_parse_page($html) {
    if (!preg_match('/<div[^>]*class="[^"]*lyric-original[^"]*"[^>]*>/i', $html, $m, PREG_OFFSET_CAPTURE)) {
        return '';
    }
    $start = $m[0][1] + strlen($m[0][0]);
    $end = strpos($html, '<div class="viewFractions"', $start);
    if ($end === false) { $end = strpos($html, '<nav class="lyricMenu', $start); }
    if ($end === false) { $end = strpos($html, '</div><nav', $start); }
    if ($end === false) { $end = min(strlen($html), $start + 200000); }
    $frag = substr($html, $start, $end - $start);
    $frag = preg_replace('/<br\s*\/?>/i', "\n", $frag);
    $frag = preg_replace('/<\/p>/i', "\n\n", $frag);
    $text = html_entity_decode(strip_tags($frag), ENT_QUOTES | ENT_HTML5, 'UTF-8');
    $text = str_replace(array("\r\n", "\r"), "\n", $text);
    $text = preg_replace('/[ \t]+\n/', "\n", $text);
    $text = preg_replace('/\n{3,}/', "\n\n", trim($text));
    return $text;
}

function lyr_fetch_letras($resolved) {
    $page = 'https://www.letras.mus.br/' . $resolved['path'];
    $html = lyr_fetch(
        LYR_JINA . $page,
        'text/html',
        28,
        array('X-Return-Format: html', 'X-Timeout: 20'));
    if ($html === null) { return ''; }
    return lyr_parse_page($html);
}

function lyr_fetch_ovh($artist, $title) {
    if (trim($artist) === '' || trim($title) === '') { return ''; }
    $url = LYR_OVH . rawurlencode($artist) . '/' . rawurlencode($title);
    $body = lyr_fetch($url, 'application/json', 15);
    if ($body === null) { return ''; }
    $data = json_decode($body, true);
    if (!is_array($data) || empty($data['lyrics'])) { return ''; }
    $text = str_replace(array("\r\n", "\r"), "\n", (string)$data['lyrics']);
    // Some entries prepend a localized "Paroles de la chanson ..." line.
    $text = preg_replace('/^\s*Paroles de la chanson[^\n]*\n+/iu', '', $text);
    return trim($text);
}

// ---------------------------------------------------------------------------
// Request

$op = isset($_GET['op']) ? (string)$_GET['op'] : 'lyrics';
if ($op !== 'lyrics') {
    lyr_fail('unknown op (lyrics)');
}

$artist = lyr_clean(lyr_str('a', 160));
$title = lyr_clean(lyr_str('t', 200));

// Most downloaded files without tags are named "Artist - Title".
if (lyr_norm($artist) === '' && strpos($title, ' - ') !== false) {
    $parts = explode(' - ', $title, 2);
    $artist = trim($parts[0]);
    $title = trim($parts[1]);
}
// And a tagged title sometimes repeats the artist in front.
if (lyr_norm($artist) !== '' && strpos(lyr_norm($title), lyr_norm($artist) . ' ') === 0) {
    $title = lyr_clean(substr($title, strlen($artist)));
    if ($title === '') { $title = lyr_str('t', 200); }
}

if (lyr_norm($title) === '' && lyr_norm($artist) === '') {
    lyr_fail('missing artist or title');
}

$cacheKey = lyr_norm($artist) . '|' . lyr_norm($title);
$cached = lyr_cache_get($cacheKey);
if ($cached !== null) {
    echo $cached;
    exit;
}

lyr_rate_limit();

$resolved = lyr_resolve($artist, $title);
$displayArtist = $resolved !== null ? $resolved['artist'] : $artist;
$displayTitle = $resolved !== null ? $resolved['title'] : $title;

$lyrics = '';
$source = '';
if ($resolved !== null) {
    $lyrics = lyr_fetch_letras($resolved);
    if ($lyrics !== '') { $source = 'letras'; }
}
if ($lyrics === '') {
    $lyrics = lyr_fetch_ovh($displayArtist, $displayTitle);
    if ($lyrics !== '') { $source = 'ovh'; }
}

if ($lyrics === '') {
    $json = json_encode(array('ok' => 0, 'e' => 'not found'));
    lyr_cache_put($cacheKey, $json, LYR_MISS_TTL);
    echo $json;
    exit;
}

$json = json_encode(array(
    'ok' => 1,
    'a' => $displayArtist,
    't' => $displayTitle,
    'l' => $lyrics,
    'src' => $source,
));
lyr_cache_put($cacheKey, $json, $source === 'letras' ? LYR_CACHE_TTL : LYR_OVH_TTL);
echo $json;
