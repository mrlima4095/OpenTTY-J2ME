<?php
/**
 * lyrics.php — song lyrics proxy for J2ME clients (CLDC/MIDP devices).
 *
 *   GET ?op=lyrics&a=<artist>&t=<title>
 *     → {"ok":1,"a":"Legião Urbana","t":"Tempo Perdido","l":"...","src":"letras"}
 *     → {"ok":0,"e":"not found"}
 *
 *   GET ?op=lyrics&p=<artist-slug>/<song-slug>/&a=<artist>&t=<title>
 *     Same reply, but reads the exact page the user picked in a manual search.
 *     Old songs use a numeric id there (legiao-urbana/22489/) and newer ones the
 *     title slug (vmz/plutao/); both forms are accepted.
 *
 *   GET ?op=search&q=<text>&n=<1..25>
 *     → {"ok":1,"r":[["Legião Urbana","Tempo Perdido","legiao-urbana/22489/"], ...]}
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
 * the search: track numbers, extensions and "(Official Video)"/"[Prod. X]"
 * suffixes are dropped, featured artists are removed from the artist, accents
 * and punctuation are folded, and "Artist - Title" is split when the artist is
 * missing. The index needs every term of a query, so one leftover credit word
 * would hide the song; when the artist tag is unknown, the title alone is tried
 * as a last resort (only with a doc that still matches the artist loosely).
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
const LYR_CACHE_VERSION = 'v2'; // bump to ignore entries cached by older matching code

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
    // iconv's //TRANSLIT is not reliable in the server image (it may emit a
    // combining mark instead of the plain letter), so fold the common Latin
    // accents explicitly. This is what made "Plutão" become "plut ao".
    static $fold = null;
    if ($fold === null) {
        $fold = array(
            'á' => 'a', 'à' => 'a', 'â' => 'a', 'ã' => 'a', 'ä' => 'a', 'å' => 'a',
            'ā' => 'a', 'ă' => 'a', 'ą' => 'a',
            'Á' => 'a', 'À' => 'a', 'Â' => 'a', 'Ã' => 'a', 'Ä' => 'a', 'Å' => 'a',
            'Ā' => 'a', 'Ă' => 'a', 'Ą' => 'a',
            'ç' => 'c', 'ć' => 'c', 'č' => 'c', 'Ç' => 'c', 'Ć' => 'c', 'Č' => 'c',
            'ď' => 'd', 'đ' => 'd', 'Ď' => 'd', 'Đ' => 'd',
            'é' => 'e', 'è' => 'e', 'ê' => 'e', 'ë' => 'e', 'ē' => 'e', 'ė' => 'e',
            'ę' => 'e',
            'É' => 'e', 'È' => 'e', 'Ê' => 'e', 'Ë' => 'e', 'Ē' => 'e', 'Ė' => 'e',
            'Ę' => 'e',
            'ğ' => 'g', 'Ğ' => 'g',
            'í' => 'i', 'ì' => 'i', 'î' => 'i', 'ï' => 'i', 'ī' => 'i', 'į' => 'i',
            'Í' => 'i', 'Ì' => 'i', 'Î' => 'i', 'Ï' => 'i', 'Ī' => 'i', 'Į' => 'i',
            'ł' => 'l', 'Ł' => 'l',
            'ñ' => 'n', 'ń' => 'n', 'ň' => 'n', 'Ñ' => 'n', 'Ń' => 'n', 'Ň' => 'n',
            'ó' => 'o', 'ò' => 'o', 'ô' => 'o', 'õ' => 'o', 'ö' => 'o', 'ø' => 'o',
            'ō' => 'o', 'ő' => 'o',
            'Ó' => 'o', 'Ò' => 'o', 'Ô' => 'o', 'Õ' => 'o', 'Ö' => 'o', 'Ø' => 'o',
            'Ō' => 'o', 'Ő' => 'o',
            'ř' => 'r', 'Ř' => 'r',
            'ś' => 's', 'š' => 's', 'ş' => 's', 'Ś' => 's', 'Š' => 's', 'Ş' => 's',
            'ť' => 't', 'ŧ' => 't', 'Ť' => 't', 'Ŧ' => 't',
            'ú' => 'u', 'ù' => 'u', 'û' => 'u', 'ü' => 'u', 'ū' => 'u', 'ů' => 'u',
            'ű' => 'u',
            'Ú' => 'u', 'Ù' => 'u', 'Û' => 'u', 'Ü' => 'u', 'Ū' => 'u', 'Ů' => 'u',
            'Ű' => 'u',
            'ý' => 'y', 'ÿ' => 'y', 'Ý' => 'y', 'Ÿ' => 'y',
            'ź' => 'z', 'ż' => 'z', 'ž' => 'z', 'Ź' => 'z', 'Ż' => 'z', 'Ž' => 'z',
            'æ' => 'ae', 'Æ' => 'ae', 'œ' => 'oe', 'Œ' => 'oe',
            'ß' => 'ss', 'ð' => 'd', 'Ð' => 'd', 'þ' => 'th', 'Þ' => 'th',
        );
    }
    $s = strtr($s, $fold);
    // Drop any combining mark that survived a decomposed input.
    $stripped = preg_replace('/[\x{0300}-\x{036f}]/u', '', $s);
    if ($stripped !== null) { $s = $stripped; }
    $s = strtolower($s);
    $s = preg_replace('/[^a-z0-9]+/', ' ', $s);
    return trim(preg_replace('/\s+/', ' ', $s));
}

/**
 * Removes file-name noise that would otherwise break the search: extensions,
 * track numbers, "(Official Video)"/"[Prod. X]" style suffixes, featured
 * artists and YouTube "| ..." tails. Bracketed text without a known credit
 * keyword (a series or anime name, for example) is kept as a hint.
 */
function lyr_clean($s) {
    $s = str_replace('_', ' ', (string)$s);
    $s = preg_replace('/\.(mp3|m4a|ogg|wav|flac|aac|opus|wma)$/i', '', $s);
    $s = lyr_strip_noise($s);
    // "01 - Title" and "01 Title" are track numbers, but "7 Minutoz" is a name,
    // so a plain number is only dropped when zero-padded or followed by a dash.
    $s = preg_replace('/^\s*(?:\d{1,3}\s*[.\-\x{2013}]\s+|0\d{1,2}\s+)/u', '', $s);
    $s = trim(preg_replace('/\s+/', ' ', $s));
    return trim($s, " -_");
}

/** Drops bracketed credits and the usual after-the-title noise from a tag. */
function lyr_strip_noise($s) {
    $noise = 'official|oficial|v[ií]deo|audio|[aá]udio|lyric|letra|clipe|remaster|ao vivo'
        . '|live|hd|hq|4k|8d|feat|part|prod|produzid[oa]|beat|instrumental|remix'
        . '|vers[aã]o|version|cover|sped up|slowed|visualizer|edit|topic';
    $out = preg_replace('/[\(\[](?=[^\)\]]*(' . $noise . '))[^\)\]]*[\)\]]/iu', ' ', $s);
    if ($out !== null) { $s = $out; }
    // Everything after a pipe is usually the artist, the channel or a credit.
    $out = preg_replace('/\s*\|.*$/u', '', $s);
    if ($out !== null) { $s = $out; }
    // Unbracketed credits ("Song feat. X", "Song prod. Y", "Song - Official Video").
    $out = preg_replace('/\s+(feat\.?|ft\.?|prod\.?|part\.|produzid[oa] por)\s+.*$/iu', '', $s);
    if ($out !== null) { $s = $out; }
    $out = preg_replace(
        '/\s*[-\x{2013}]\s*(official|oficial)\s+'
        . '(video|v[ií]deo|audio|[aá]udio|lyric video|clipe|visualizer).*$/iu',
        '',
        $s);
    if ($out !== null) { $s = $out; }
    return $s;
}

/** Artist tag cleaned like a title, with featured artists and "- Topic" removed. */
function lyr_clean_artist($s) {
    $s = lyr_clean($s);
    $out = preg_replace('/\s+(feat\.?|ft\.?|part\.|oficial|official|topic)$/iu', '', $s);
    if ($out !== null) { $s = $out; }
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

/** Queries the letras.mus.br search index and returns its raw docs (may be empty). */
function lyr_search_docs($query, $limit = 10) {
    $query = lyr_norm($query);
    if ($query === '') { return array(); }
    $limit = (int)$limit;
    if ($limit < 1) { $limit = 10; }
    if ($limit > 25) { $limit = 25; }
    $url = LYR_SOLR . '?q=' . urlencode($query) . '&limit=' . $limit;
    $body = lyr_fetch($url, 'application/json, text/javascript, */*', 15);
    if ($body === null) { return array(); }
    $open = strpos($body, '(');
    $close = strrpos($body, ')');
    if ($open === false || $close === false || $close <= $open) { return array(); }
    $data = json_decode(substr($body, $open + 1, $close - $open - 1), true);
    if (!is_array($data) || !isset($data['response']['docs']) || !is_array($data['response']['docs'])) {
        return array();
    }
    return $data['response']['docs'];
}

/**
 * Builds the "artist-slug/song-slug/" page path of a search doc, or '' when unusable.
 *
 * Older songs use a numeric id (legiao-urbana/22489/) and newer ones use the title
 * slug (vmz/plutao/); both are valid pages. Only song docs (t=2) have the url field;
 * artists, albums and posts would otherwise shadow the song with an unusable path.
 */
function lyr_doc_path($doc) {
    if (!isset($doc['dns'], $doc['url'])) { return ''; }
    if (isset($doc['t']) && (int)$doc['t'] !== 2) { return ''; }
    $dns = preg_replace('/[^a-z0-9\-]/', '', strtolower((string)$doc['dns']));
    $id = preg_replace('/[^a-z0-9\-]/', '', strtolower((string)$doc['url']));
    if ($dns === '' || $id === '') { return ''; }
    return $dns . '/' . $id . '/';
}

/**
 * Queries the letras.mus.br search index and returns {artist,title,path} or null.
 *
 * The index requires every term of the query, so a leftover credit word makes it
 * return nothing; tags are cleaned first. When the artist tag is a channel name
 * or an unusual collab that the index does not know, the search is retried with
 * the title alone, but only a doc that also matches the artist loosely is used.
 */
function lyr_resolve($artist, $title) {
    if (lyr_norm(trim($artist . ' ' . $title)) === '') { return null; }
    $titles = array($title);
    $core = lyr_core_title($title);
    if ($core !== '' && lyr_norm($core) !== lyr_norm($title)) { $titles[] = $core; }
    // First with the artist, then (when the artist tag is a channel or an
    // unusual collab the index does not know) with the title alone, keeping
    // only docs that still match the artist loosely.
    foreach (array(false, true) as $titleOnly) {
        foreach ($titles as $try) {
            if ($titleOnly && (lyr_norm($artist) === '' || lyr_norm($try) === '')) { continue; }
            $query = $titleOnly ? $try : trim($artist . ' ' . $try);
            $best = lyr_best_doc(lyr_search_docs($query, 10), $artist, $try, $titleOnly);
            if ($best !== null) { return $best; }
        }
    }
    return null;
}

/** The title without parenthetical extras and without a "- suffix" tail. */
function lyr_core_title($title) {
    $core = preg_replace('/\s*[\(\[].*$/u', '', $title);
    if ($core !== null) { $title = $core; }
    $core = preg_replace('/\s+[-\x{2013}]\s+.*$/u', '', $title);
    if ($core !== null && trim($core) !== '') { $title = $core; }
    return trim($title);
}

/** Scores the search docs and returns the best {artist,title,path}, or null. */
function lyr_best_doc($docs, $artist, $title, $requireArtist) {
    $wantArtist = lyr_norm($artist);
    $wantTitle = lyr_norm($title);
    $best = null;
    $bestPath = '';
    $bestScore = -1.0;
    foreach ($docs as $doc) {
        if (!isset($doc['art'], $doc['txt'])) { continue; }
        $path = lyr_doc_path($doc);
        if ($path === '') { continue; }
        $titleScore = lyr_title_score($wantTitle, lyr_norm($doc['txt']));
        if ($titleScore < 0) { continue; }
        $artistScore = lyr_artist_score($wantArtist, lyr_norm($doc['art']));
        if ($requireArtist && $artistScore === 0) { continue; }
        $score = $titleScore + $artistScore;
        if (isset($doc['score'])) { $score += ((float)$doc['score']) / 100.0; }
        if ($score > $bestScore) {
            $bestScore = $score;
            $best = $doc;
            $bestPath = $path;
        }
    }
    if ($best === null) { return null; }
    return array(
        'artist' => (string)$best['art'],
        'title' => (string)$best['txt'],
        'path' => $bestPath,
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
if ($op !== 'lyrics' && $op !== 'search') {
    lyr_fail('unknown op (lyrics, search)');
}

if ($op === 'search') {
    $query = lyr_str('q', 200);
    if (lyr_norm($query) === '') { lyr_fail('missing query'); }
    $limit = isset($_GET['n']) ? (int)$_GET['n'] : 15;
    if ($limit < 1) { $limit = 15; }
    if ($limit > 25) { $limit = 25; }
    $cacheKey = LYR_CACHE_VERSION . '|s|' . lyr_norm($query) . '|' . $limit;
    $cached = lyr_cache_get($cacheKey);
    if ($cached !== null) { echo $cached; exit; }
    lyr_rate_limit();
    $rows = array();
    $seen = array();
    foreach (lyr_search_docs($query, $limit) as $doc) {
        if (!isset($doc['art'], $doc['txt'])) { continue; }
        $path = lyr_doc_path($doc);
        if ($path === '' || isset($seen[$path])) { continue; }
        $seen[$path] = true;
        $rows[] = array((string)$doc['art'], (string)$doc['txt'], $path);
    }
    if (count($rows) === 0) {
        $json = json_encode(array('ok' => 0, 'e' => 'not found'));
        lyr_cache_put($cacheKey, $json, LYR_MISS_TTL);
        echo $json;
        exit;
    }
    $json = json_encode(array('ok' => 1, 'r' => $rows));
    lyr_cache_put($cacheKey, $json, LYR_CACHE_TTL);
    echo $json;
    exit;
}

$rawArtist = lyr_str('a', 160);
$rawTitle = lyr_str('t', 200);
$path = isset($_GET['p']) ? trim((string)$_GET['p']) : '';

// A song picked from a manual search comes with its letras.mus.br page path.
if ($path !== '') {
    if (!preg_match('#^[a-z0-9\-]+/[a-z0-9\-]+/$#', $path)) {
        lyr_fail('bad path');
    }
    $cacheKey = LYR_CACHE_VERSION . '|p|' . $path;
    $cached = lyr_cache_get($cacheKey);
    if ($cached !== null) { echo $cached; exit; }
    lyr_rate_limit();

    $lyrics = lyr_fetch_letras(array('path' => $path));
    $source = $lyrics !== '' ? 'letras' : '';
    if ($lyrics === '') {
        $lyrics = lyr_fetch_ovh(lyr_clean_artist($rawArtist), lyr_clean($rawTitle));
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
        'a' => $rawArtist,
        't' => $rawTitle,
        'l' => $lyrics,
        'src' => $source,
    ));
    lyr_cache_put($cacheKey, $json, $source === 'letras' ? LYR_CACHE_TTL : LYR_OVH_TTL);
    echo $json;
    exit;
}

$artist = lyr_clean_artist($rawArtist);
$title = lyr_clean($rawTitle);

// Most downloaded files without tags are named "Artist - Title".
if (lyr_norm($artist) === '' && strpos($title, ' - ') !== false) {
    $parts = explode(' - ', $title, 2);
    $artist = lyr_clean_artist($parts[0]);
    $title = lyr_clean($parts[1]);
}
// And a tagged title sometimes repeats the artist in front.
if (lyr_norm($artist) !== '' && strpos(lyr_norm($title), lyr_norm($artist) . ' ') === 0) {
    $title = lyr_clean(substr($title, strlen($artist)));
    if ($title === '') { $title = lyr_str('t', 200); }
}

if (lyr_norm($title) === '' && lyr_norm($artist) === '') {
    lyr_fail('missing artist or title');
}

$cacheKey = LYR_CACHE_VERSION . '|' . lyr_norm($artist) . '|' . lyr_norm($title);
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
