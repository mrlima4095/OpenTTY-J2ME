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
 *   translate &tl=<lang> &q=...
 *
 * Posts are paginated by the created_utc of the oldest item of the page: the
 * client sends it back as bm, the proxy maps it to Arctic Shift's before param.
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
    if ($data === null || !isset($data['data'])) { rs_fail('upstream error'); }
    $thread = rs_thread($data['data'], $linkId, $n);
    echo json_encode(array('ok' => 1, 'it' => $thread, 'nx' => ''));
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
    rs_fail('unknown op (search, subposts, userposts, post, comments, translate)');
}
