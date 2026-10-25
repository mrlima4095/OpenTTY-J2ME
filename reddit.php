<?php
/**
 * reddit.php — Reddit JSON proxy for J2ME clients (CLDC/MIDP devices).
 *
 * Reddit closed self-service API app creation (Responsible Builder Policy), so
 * there is no OAuth client to use. This proxy instead reads Reddit's public data
 * from the Arctic Shift archive (https://arctic-shift.photon-reddit.com), which
 * needs no credentials and no login.
 *
 * Everything here is READ ONLY and anonymous: subreddits, posts, comments and
 * users. There is no user session, no voting and no posting.
 *
 * Responses are compacted to short keys so the payload stays small for old
 * devices. Shape: {ok:1, it:[...], nx:"<next bookmark>"} or {ok:0, e:"..."}.
 *
 * Ops:
 *   search    &type=subreddits|users|posts &q=... [&sub=...] [&n=] [&bm=]
 *   subposts  &sub=... [&sort=new|top] [&n=] [&bm=]
 *   userposts &u=... [&n=] [&bm=]
 *   post      &id=...
 *   comments  &id=... [&n=]
 *   subreddit &s=...
 *   user      &u=...
 *   translate &tl=<lang> &q=...
 *
 * Posts are paginated by the created_utc of the oldest item of the page: the
 * client sends it back as bm, the proxy maps it to Arctic Shift's before param.
 *
 * The archive takes a while to ingest brand-new posts and their comments, so
 * when a post has no comments in the archive yet the comments op falls back to
 * Reddit's public comment RSS (flat, newest page only). Reddit rate limits that
 * feed hard, so the fallback is cached per post and throttled to one upstream
 * request per minute across all clients.
 */

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

const RS_BASE = 'https://arctic-shift.photon-reddit.com';
const RS_UA = 'MIDPlay/1.0 (J2ME; +http://opentty.fun)';
const RS_CACHE_TTL = 300;   // seconds
const RS_RATE_LIMIT = 90;   // requests per minute per client IP
const RS_DEFAULT_N = 20;
const RS_MAX_N = 50;

// ---------------------------------------------------------------------------
// Helpers

function rs_fail($msg, $http = 200) {
    http_response_code($http);
    echo json_encode(array('ok' => 0, 'e' => (string)$msg));
    exit;
}

function rs_rate_limit() {
    $ip = $_SERVER['REMOTE_ADDR'] ?? 'cli';
    $dir = sys_get_temp_dir() . '/redditrl';
    if (!is_dir($dir)) { @mkdir($dir, 0777, true); }
    $file = $dir . '/' . md5($ip) . '.json';
    $now = time();
    $window = array();
    if (is_file($file)) {
        $window = json_decode(@file_get_contents($file), true);
        if (!is_array($window)) { $window = array(); }
    }
    $window = array_values(array_filter($window, function ($t) use ($now) { return $t > $now - 60; }));
    if (count($window) >= RS_RATE_LIMIT) {
        header('Retry-After: 60');
        rs_fail('rate limited', 429);
    }
    $window[] = $now;
    @file_put_contents($file, json_encode($window));
}

function rs_cache_get($key) {
    $file = sys_get_temp_dir() . '/redditcache/' . md5($key) . '.json';
    if (is_file($file) && (time() - filemtime($file)) < RS_CACHE_TTL) {
        $data = @file_get_contents($file);
        if ($data !== false) { return $data; }
    }
    return null;
}

function rs_cache_put($key, $data) {
    $dir = sys_get_temp_dir() . '/redditcache';
    if (!is_dir($dir)) { @mkdir($dir, 0777, true); }
    @file_put_contents($dir . '/' . md5($key) . '.json', $data);
}

/** Fetches a URL and returns the decoded JSON array, or null on failure. */
function rs_fetch($url) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 30);
    curl_setopt($ch, CURLOPT_USERAGENT, RS_UA);
    curl_setopt($ch, CURLOPT_HTTPHEADER, array('Accept: application/json'));
    $body = curl_exec($ch);
    $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $err = curl_error($ch);
    curl_close($ch);
    if ($body === false || $err) { return null; }
    if ($code < 200 || $code >= 300) { return null; }
    $data = json_decode($body, true);
    return is_array($data) ? $data : null;
}

/**
 * Fetches a URL, retrying on upstream failure. Arctic Shift's full-text search is
 * load dependent and often answers with a fast "Timeout" error, so a couple of
 * quick retries make keyword searches usable without stalling the client.
 */
/** Fetches a URL as text (RSS/XML), or null on failure. */
function rs_fetch_text($url, $accept) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 20);
    curl_setopt($ch, CURLOPT_USERAGENT, RS_UA);
    curl_setopt($ch, CURLOPT_HTTPHEADER, array('Accept: ' . $accept));
    $body = curl_exec($ch);
    $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    if ($body === false || $code < 200 || $code >= 300) { return null; }
    return $body;
}

/**
 * Reddit's comment RSS returns the newest page of comments as flat entries, with
 * HTML bodies. It is the only public Reddit endpoint reachable from the server
 * for posts that the archive has not ingested yet, so it is used as a fallback.
 */
function rs_rss_comments($id) {
    $key = 'rss_comments:' . $id;
    $cached = rs_cache_get($key);
    if ($cached !== null) {
        $rows = json_decode($cached, true);
        return is_array($rows) ? $rows : array();
    }
    if (!rs_rss_throttle_ok()) { return null; }
    $url = 'https://www.reddit.com/comments/' . rawurlencode($id) . '.rss';
    $body = rs_fetch_text($url, 'application/atom+xml, application/xml, text/xml');
    if ($body === null) {
        // Negative-cache the failure briefly so a removed post or a rate limit
        // does not keep burning the fallback budget.
        rs_cache_put($key, '[]');
        return array();
    }
    $rows = rs_parse_rss_comments($body);
    rs_cache_put($key, json_encode($rows));
    return $rows;
}

/** One upstream request per minute, shared by every client, to respect Reddit's limit. */
function rs_rss_throttle_ok() {
    $file = sys_get_temp_dir() . '/redditrss_last';
    $fp = @fopen($file, 'c+');
    if (!$fp) { return true; }
    $ok = false;
    if (flock($fp, LOCK_EX)) {
        $last = (int)trim((string)stream_get_contents($fp));
        $now = time();
        if ($last === 0 || ($now - $last) >= 60) {
            rewind($fp);
            ftruncate($fp, 0);
            fwrite($fp, (string)$now);
            $ok = true;
        }
        flock($fp, LOCK_UN);
    }
    fclose($fp);
    return $ok;
}

function rs_parse_rss_comments($body) {
    $xml = @simplexml_load_string($body);
    if ($xml === false) { return array(); }
    $rows = array();
    foreach ($xml->entry as $entry) {
        $eid = (string)$entry->id;
        if (strpos($eid, 't1_') !== 0) { continue; }
        $author = (string)$entry->author->name;
        if (strpos($author, '/u/') === 0) { $author = substr($author, 3); }
        $text = rs_html_text((string)$entry->content);
        if ($text === '') { continue; }
        $created = strtotime((string)$entry->updated);
        $rows[] = array(
            'b' => $text,
            'a' => $author,
            'sc' => 0,
            'dp' => 0,
            'ct' => $created === false ? time() : $created,
        );
    }
    return $rows;
}

/** Turns the escaped HTML in an RSS comment into plain text, keeping paragraphs. */
function rs_html_text($html) {
    $s = html_entity_decode((string)$html, ENT_QUOTES | ENT_HTML5, 'UTF-8');
    $s = preg_replace('#<br\s*/?>#i', "\n", $s);
    $s = preg_replace('#</p>#i', "\n\n", $s);
    $s = strip_tags($s);
    $s = html_entity_decode($s, ENT_QUOTES | ENT_HTML5, 'UTF-8');
    $s = trim(preg_replace("/\n{3,}/", "\n\n", $s));
    if ($s === '[removed]' || $s === '[deleted]') { return ''; }
    return $s;
}

function rs_fetch_retry($url, $attempts = 2) {
    for ($i = 0; $i < $attempts; $i++) {
        $data = rs_fetch($url);
        if ($data !== null) {
            return $data;
        }
        usleep(250000);
    }
    return null;
}

/**
 * Translates text into $tl. Uses Google's public dictionary endpoint (the one
 * browser extensions call, no key) and falls back to MyMemory. Multi-line input
 * (titles joined by "\n") keeps its line breaks, so one call can translate a
 * whole page of titles.
 */
function rs_translate($text, $tl) {
    $text = trim((string)$text);
    if ($text === '') { return ''; }
    if ($tl === '') { $tl = 'en'; }
    $g = rs_fetch('https://clients5.google.com/translate_a/t?client=dict-chrome-ex'
        . '&sl=auto&tl=' . urlencode($tl) . '&q=' . urlencode($text));
    if (is_array($g)) {
        $out = rs_translate_segments($g);
        if ($out !== '') { return $out; }
    }
    $m = rs_fetch('https://api.mymemory.translated.net/get?q=' . urlencode($text)
        . '&langpair=Autodetect|' . urlencode($tl));
    if (is_array($m) && isset($m['responseData']['translatedText'])) {
        $t = (string)$m['responseData']['translatedText'];
        if ($t !== '') { return $t; }
    }
    return '';
}

/** Joins the segments of a Google translate response ([["seg","en"],...]). */
function rs_translate_segments($data) {
    $out = '';
    if (!is_array($data)) { return ''; }
    foreach ($data as $seg) {
        if (is_array($seg) && isset($seg[0]) && is_string($seg[0])) { $out .= $seg[0]; }
    }
    return $out;
}

function rs_n($name, $default = RS_DEFAULT_N) {
    $v = isset($_GET[$name]) ? intval($_GET[$name]) : $default;
    if ($v < 1) { $v = 1; }
    if ($v > RS_MAX_N) { $v = RS_MAX_N; }
    return $v;
}

function rs_str($name, $max = 200) {
    $v = isset($_GET[$name]) ? trim((string)$_GET[$name]) : '';
    if (strlen($v) > $max) { $v = substr($v, 0, $max); }
    return $v;
}

function rs_trim($s, $max) {
    $s = (string)$s;
    if (strlen($s) > $max) { $s = substr($s, 0, $max) . '...'; }
    return $s;
}

/** Best-effort thumbnail: reddit's own thumbnail, or the link when it is an image. */
function rs_thumb($p) {
    $t = isset($p['thumbnail']) ? $p['thumbnail'] : '';
    if (is_string($t) && preg_match('#^https?://#i', $t)) { return $t; }
    $url = isset($p['url']) ? (string)$p['url'] : '';
    if (preg_match('#^https?://#i', $url) && preg_match('#\.(jpg|jpeg|png|gif|webp)(\?.*)?$#i', $url)) {
        return $url;
    }
    return '';
}

function rs_clean_text($s) {
    $s = (string)$s;
    if ($s === '[removed]' || $s === '[deleted]') { return ''; }
    return $s;
}

// ---------------------------------------------------------------------------
// Compaction

function rs_post($p) {
    return array(
        'id' => (string)($p['id'] ?? ''),
        'ti' => (string)($p['title'] ?? ''),
        'au' => (string)($p['author'] ?? ''),
        'sr' => (string)($p['subreddit'] ?? ''),
        'ct' => (int)($p['created_utc'] ?? 0),
        'sc' => (int)($p['score'] ?? 0),
        'nc' => (int)($p['num_comments'] ?? 0),
        'tx' => rs_trim(rs_clean_text($p['selftext'] ?? ''), 6000),
        'u'  => (string)($p['url'] ?? ''),
        'ns' => !empty($p['over_18']) ? 1 : 0,
        'th' => rs_thumb($p),
        'pl' => (string)($p['permalink'] ?? ''),
        'fl' => (string)($p['link_flair_text'] ?? ''),
        'sf' => !empty($p['is_self']) ? 1 : 0,
    );
}

function rs_sub($s) {
    $icon = (string)($s['community_icon'] ?? '');
    if ($icon === '') { $icon = (string)($s['icon_img'] ?? ''); }
    return array(
        'n'  => (string)($s['display_name'] ?? ''),
        'ti' => (string)($s['title'] ?? ''),
        'd'  => rs_trim((string)($s['public_description'] ?? ''), 500),
        'sc' => (int)($s['subscribers'] ?? 0),
        'ns' => !empty($s['over18']) ? 1 : 0,
        'ic' => $icon,
        'ct' => (int)($s['created_utc'] ?? 0),
    );
}

function rs_user($u) {
    $m = isset($u['_meta']) && is_array($u['_meta']) ? $u['_meta'] : array();
    return array(
        'n'  => (string)($u['author'] ?? ''),
        'ka' => (int)($m['total_karma'] ?? 0),
        'pc' => (int)($m['num_posts'] ?? 0),
        'cc' => (int)($m['num_comments'] ?? 0),
    );
}

/**
 * Full profile of a subreddit. Reddit models user profiles as pseudo-subreddits
 * named u_<username>, so the same function serves both: the bio is the public
 * description and the picture is the community icon.
 */
function rs_profile($s) {
    $icon = (string)($s['community_icon'] ?? '');
    if ($icon === '') { $icon = (string)($s['icon_img'] ?? ''); }
    $banner = (string)($s['banner_img'] ?? '');
    if ($banner === '') { $banner = (string)($s['banner_background_image'] ?? ''); }
    $bio = (string)($s['public_description'] ?? '');
    if (trim($bio) === '') { $bio = (string)($s['description'] ?? ''); }
    return array(
        'n'  => (string)($s['display_name'] ?? ''),
        'ti' => (string)($s['title'] ?? ''),
        'pd' => rs_trim(trim($bio), 4000),
        'ic' => $icon,
        'bn' => $banner,
        'sc' => (int)($s['subscribers'] ?? 0),
        'ct' => (int)($s['created_utc'] ?? 0),
        'ns' => !empty($s['over18']) ? 1 : 0,
    );
}

/** Finds a subreddit (or u_<name> profile) by its exact display name. */
function rs_sub_by_name($name, $prefix) {
    $url = RS_BASE . '/api/subreddits/search?limit=10&subreddit_prefix=' . urlencode($prefix);
    $data = rs_fetch($url);
    if ($data === null || !isset($data['data']) || !is_array($data['data'])) { return null; }
    foreach ($data['data'] as $s) {
        if (strcasecmp((string)($s['display_name'] ?? ''), $name) === 0) { return $s; }
    }
    return null;
}

function rs_comment($c, $depth) {
    return array(
        'b'  => rs_trim(rs_clean_text($c['body'] ?? ''), 4000),
        'a'  => (string)($c['author'] ?? ''),
        'sc' => (int)($c['score'] ?? 0),
        'dp' => $depth,
        'ct' => (int)($c['created_utc'] ?? 0),
    );
}

/**
 * Turns Arctic Shift's flat comment list into a threaded, depth-annotated order.
 * Comments whose parent is missing from the page are treated as top level.
 */
function rs_thread($flat, $linkId, $max) {
    $byId = array();
    foreach ($flat as $c) {
        if (isset($c['id'])) { $byId[(string)$c['id']] = $c; }
    }
    // Arctic Shift stores ids bare ("abc123") but parent_id is a fullname
    // ("t1_abc123", or "t3_<post>" for top level), so strip the prefix before
    // matching or every comment would look like a root and lose its depth.
    $bareLink = (strpos($linkId, 't3_') === 0) ? substr($linkId, 3) : $linkId;
    $children = array();
    $roots = array();
    foreach ($flat as $c) {
        if (!isset($c['id'])) { continue; }
        $pid = (string)($c['parent_id'] ?? '');
        if (strpos($pid, 't1_') === 0 || strpos($pid, 't3_') === 0) { $pid = substr($pid, 3); }
        if ($pid === $bareLink || !isset($byId[$pid])) {
            $roots[] = $c;
        } else {
            $children[$pid][] = $c;
        }
    }
    $out = array();
    $emit = function ($node, $depth) use (&$emit, &$out, &$children, $max) {
        if (count($out) >= $max) { return; }
        $out[] = rs_comment($node, $depth);
        $id = (string)$node['id'];
        if (isset($children[$id])) {
            $kids = $children[$id];
            usort($kids, function ($a, $b) {
                return ((int)($b['score'] ?? 0)) <=> ((int)($a['score'] ?? 0));
            });
            foreach ($kids as $k) { $emit($k, $depth + 1); }
        }
    };
    usort($roots, function ($a, $b) {
        return ((int)($b['score'] ?? 0)) <=> ((int)($a['score'] ?? 0));
    });
    foreach ($roots as $r) { $emit($r, 0); }
    return $out;
}

// ---------------------------------------------------------------------------
// Dispatch

rs_rate_limit();

$op = isset($_GET['op']) ? (string)$_GET['op'] : '';

switch ($op) {

case 'search': {
    $type = rs_str('type', 20);
    $q = rs_str('q', 200);
    $n = rs_n('n');
    $bm = rs_str('bm', 20);
    if ($q === '') { rs_fail('missing query'); }

    if ($type === 'subreddits') {
        $url = RS_BASE . '/api/subreddits/search?limit=' . $n
             . '&subreddit_prefix=' . urlencode($q);
        $data = rs_fetch($url);
        if ($data === null || !isset($data['data'])) { rs_fail('upstream error'); }
        $items = array();
        foreach ($data['data'] as $s) { $items[] = rs_sub($s); }
        echo json_encode(array('ok' => 1, 'it' => $items, 'nx' => ''));
        exit;
    }

    if ($type === 'users') {
        $url = RS_BASE . '/api/users/search?limit=' . $n
             . '&author_prefix=' . urlencode($q);
        $data = rs_fetch($url);
        if ($data === null || !isset($data['data'])) { rs_fail('upstream error'); }
        $items = array();
        foreach ($data['data'] as $u) { $items[] = rs_user($u); }
        echo json_encode(array('ok' => 1, 'it' => $items, 'nx' => ''));
        exit;
    }

    // type=posts: keyword search inside a subreddit (Arctic Shift requires one).
    $sub = rs_str('sub', 100);
    if ($sub === '') { rs_fail('posts search needs a subreddit'); }
    $url = RS_BASE . '/api/posts/search?limit=' . $n . '&sort=desc'
         . '&subreddit=' . urlencode($sub)
         . '&title=' . urlencode($q);
    if ($bm !== '') { $url .= '&before=' . urlencode($bm); }
    $data = rs_fetch_retry($url, 3);
    if ($data !== null && isset($data['data'])) {
        $items = array();
        $last = '';
        foreach ($data['data'] as $p) { $items[] = rs_post($p); $last = (string)($p['created_utc'] ?? ''); }
        $nx = (count($items) >= $n && $last !== '') ? $last : '';
        echo json_encode(array('ok' => 1, 'it' => $items, 'nx' => $nx));
        exit;
    }
    // Fallback: scan the newest posts and filter by keyword locally. Covers less
    // history than the title search, but always answers instead of erroring.
    $scan = RS_BASE . '/api/posts/search?limit=100&sort=desc&subreddit=' . urlencode($sub);
    $data = rs_fetch_retry($scan, 2);
    if ($data === null || !isset($data['data'])) { rs_fail('upstream error'); }
    $items = array();
    foreach ($data['data'] as $p) {
        $haystack = (string)($p['title'] ?? '') . ' ' . (string)($p['selftext'] ?? '');
        if (stripos($haystack, $q) !== false) {
            $items[] = rs_post($p);
            if (count($items) >= $n) { break; }
        }
    }
    echo json_encode(array('ok' => 1, 'it' => $items, 'nx' => ''));
    exit;
}

case 'subposts': {
    $sub = rs_str('sub', 100);
    if ($sub === '') { rs_fail('missing subreddit'); }
    $n = rs_n('n');
    $bm = rs_str('bm', 20);
    $sort = rs_str('sort', 10);
    if ($sort === 'top') {
        $url = RS_BASE . '/api/posts/search?limit=' . $n . '&sort=desc'
             . '&after=1month&subreddit=' . urlencode($sub);
        $data = rs_fetch($url);
        if ($data === null || !isset($data['data'])) { rs_fail('upstream error'); }
        $rows = $data['data'];
        usort($rows, function ($a, $b) {
            return ((int)($b['score'] ?? 0)) <=> ((int)($a['score'] ?? 0));
        });
        $items = array();
        foreach ($rows as $p) { $items[] = rs_post($p); }
        echo json_encode(array('ok' => 1, 'it' => $items, 'nx' => ''));
        exit;
    }
    $url = RS_BASE . '/api/posts/search?limit=' . $n . '&sort=desc'
         . '&subreddit=' . urlencode($sub);
    if ($bm !== '') { $url .= '&before=' . urlencode($bm); }
    $data = rs_fetch($url);
    if ($data === null || !isset($data['data'])) { rs_fail('upstream error'); }
    $items = array();
    $last = '';
    foreach ($data['data'] as $p) { $items[] = rs_post($p); $last = (string)($p['created_utc'] ?? ''); }
    $nx = (count($items) >= $n && $last !== '') ? $last : '';
    echo json_encode(array('ok' => 1, 'it' => $items, 'nx' => $nx));
    exit;
}

case 'userposts': {
    $u = rs_str('u', 100);
    if ($u === '') { rs_fail('missing user'); }
    $n = rs_n('n');
    $bm = rs_str('bm', 20);
    $url = RS_BASE . '/api/posts/search?limit=' . $n . '&sort=desc'
         . '&author=' . urlencode($u);
    if ($bm !== '') { $url .= '&before=' . urlencode($bm); }
    $data = rs_fetch($url);
    if ($data === null || !isset($data['data'])) { rs_fail('upstream error'); }
    $items = array();
    $last = '';
    foreach ($data['data'] as $p) { $items[] = rs_post($p); $last = (string)($p['created_utc'] ?? ''); }
    $nx = (count($items) >= $n && $last !== '') ? $last : '';
    echo json_encode(array('ok' => 1, 'it' => $items, 'nx' => $nx));
    exit;
}

case 'post': {
    $id = rs_str('id', 20);
    if ($id === '') { rs_fail('missing id'); }
    $url = RS_BASE . '/api/posts/ids?ids=' . urlencode($id);
    $data = rs_fetch($url);
    if ($data === null || !isset($data['data']) || !is_array($data['data']) || count($data['data']) === 0) {
        rs_fail('post not found');
    }
    echo json_encode(array('ok' => 1, 'it' => array(rs_post($data['data'][0])), 'nx' => ''));
    exit;
}

case 'comments': {
    $id = rs_str('id', 20);
    if ($id === '') { rs_fail('missing id'); }
    $n = rs_n('n', 50);
    $linkId = 't3_' . $id;
    $url = RS_BASE . '/api/comments/search?limit=' . $n . '&link_id=' . urlencode($linkId);
    $data = rs_fetch($url);
    $thread = array();
    if ($data !== null && isset($data['data'])) {
        $thread = rs_thread($data['data'], $linkId, $n);
    }
    $pending = 0;
    if (count($thread) === 0) {
        // Brand-new posts are often not in the archive yet; use Reddit's public
        // comment RSS instead. null means the throttled feed was not consulted.
        $rss = rs_rss_comments($id);
        if ($rss === null) {
            $pending = 1;
        } else {
            $thread = $rss;
        }
    }
    echo json_encode(array('ok' => 1, 'it' => $thread, 'nx' => '', 'pd' => $pending));
    exit;
}

case 'subreddit': {
    $s = rs_str('s', 100);
    if ($s === '') { rs_fail('missing subreddit'); }
    $key = 'profile_sub:' . strtolower($s);
    $cached = rs_cache_get($key);
    if ($cached !== null) { echo $cached; exit; }
    $doc = rs_sub_by_name($s, $s);
    if ($doc === null) { rs_fail('subreddit not found'); }
    $json = json_encode(array('ok' => 1, 'p' => rs_profile($doc), 'nx' => ''));
    rs_cache_put($key, $json);
    echo $json;
    exit;
}

case 'user': {
    $u = rs_str('u', 100);
    if ($u === '') { rs_fail('missing user'); }
    $key = 'profile_user:' . strtolower($u);
    $cached = rs_cache_get($key);
    if ($cached !== null) { echo $cached; exit; }
    $profile = array(
        'n' => $u, 'ti' => '', 'pd' => '', 'ic' => '', 'bn' => '',
        'sc' => 0, 'ct' => 0, 'ns' => 0,
        'ka' => 0, 'pc' => 0, 'cc' => 0, 'fa' => 0, 'la' => 0,
    );
    $doc = rs_sub_by_name('u_' . $u, 'u_' . $u);
    if ($doc !== null) {
        // Reddit names the profile pseudo-subreddit u_<name>; the client wants
        // the plain username back.
        $profile = array_merge($profile, rs_profile($doc));
        $profile['n'] = $u;
    }
    $data = rs_fetch(RS_BASE . '/api/users/search?limit=1&author=' . urlencode($u));
    if ($data !== null && isset($data['data'][0]['_meta']) && is_array($data['data'][0]['_meta'])) {
        $meta = $data['data'][0]['_meta'];
        $profile['ka'] = (int)($meta['total_karma'] ?? 0);
        $profile['pc'] = (int)($meta['num_posts'] ?? 0);
        $profile['cc'] = (int)($meta['num_comments'] ?? 0);
        $first = (int)($meta['earliest_post_at'] ?? 0);
        $firstComment = (int)($meta['earliest_comment_at'] ?? 0);
        if ($first === 0 || ($firstComment > 0 && $firstComment < $first)) { $first = $firstComment; }
        $last = (int)($meta['last_post_at'] ?? 0);
        $lastComment = (int)($meta['last_comment_at'] ?? 0);
        if ($lastComment > $last) { $last = $lastComment; }
        $profile['fa'] = $first;
        $profile['la'] = $last;
    }
    $json = json_encode(array('ok' => 1, 'p' => $profile, 'nx' => ''));
    rs_cache_put($key, $json);
    echo $json;
    exit;
}

case 'translate': {
    $tl = rs_str('tl', 10);
    $q = rs_str('q', 6000);
    if ($q === '') { echo json_encode(array('ok' => 1, 't' => '', 'nx' => '')); exit; }
    $t = rs_translate($q, $tl);
    echo json_encode(array('ok' => 1, 't' => $t, 'nx' => ''));
    exit;
}

default:
    rs_fail('unknown op (search, subposts, userposts, post, comments, subreddit, user, translate)');
}
