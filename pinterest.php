<?php
/**
 * pinterest.php — Pinterest JSON proxy for J2ME clients (CLDC/MIDP devices).
 *
 * The device cannot talk to Pinterest directly (TLS, cookies, CSRF and the
 * X-Pinterest-PWS-Handler header are all out of reach for MIDP HttpConnection,
 * and the generic proxy.php only forwards the User-Agent). This script does
 * the whole handshake server-side and returns trimmed JSON built for a small
 * screen and a slow link.
 *
 * It uses Pinterest's internal web resources anonymously (no login, no
 * cookies needed for the resource calls). When Pinterest changes its
 * internals this file is the only thing that needs updating — no J2ME
 * reinstall.
 *
 * Endpoints (all GET, all return application/json):
 *
 *   ?op=search&q=<query>&scope=pins|boards&n=<count>&bm=<bookmark>
 *   ?op=user&u=<username>
 *   ?op=userpins&u=<username>&n=<count>&bm=<bookmark>
 *   ?op=userboards&u=<username>&n=<count>&bm=<bookmark>
 *   ?op=board&u=<username>&s=<board-slug>
 *   ?op=boardpins&u=<username>&s=<board-slug>&n=<count>&bm=<bookmark>
 *   ?op=pin&id=<numeric-pin-id>
 *
 * Response envelope:
 *
 *   {"ok":1,"k":"<kind>","it":[ ...items... ],"nx":"<next bookmark, or empty>"}
 *   {"ok":0,"e":"<error message>"}
 *
 * The "nx" bookmark is opaque: the client echoes it back as &bm= to fetch
 * the next page. Item fields are short on purpose (every byte crosses GPRS):
 *
 *   pins:   id, ti (title), de (description), im (236x image), im4 (474x),
 *           un (pinner full name), us (pinner username),
 *           bd (board name), bus (board owner), bs (board slug), ct (created)
 *   boards: id, n (name), d (description), c (cover 236x), us (owner),
 *           sl (slug), pc (pin count), fc (follower count)
 *   user:   us, un, ab (about), im (75x75 avatar), fc, pc, bc (board count)
 *   board:  id, n, d, c, us, sl, pc, fc
 *
 * Images are served straight from i.pinimg.com; the client fetches them
 * through proxy.php. Nothing is relayed through this server.
 */

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

const PIN_UA = 'Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36';
const PIN_APP_VERSION = '9710e8b';
const PIN_CACHE_TTL = 600;   // seconds
const PIN_RATE_LIMIT = 90;   // requests per minute per client IP

// ---------------------------------------------------------------------------
// Small helpers

function pin_fail($msg, $http = 200) {
    http_response_code($http);
    echo json_encode(array('ok' => 0, 'e' => (string)$msg));
    exit;
}

function pin_rate_limit() {
    $ip = $_SERVER['REMOTE_ADDR'] ?? 'cli';
    $dir = sys_get_temp_dir() . '/pinrl';
    if (!is_dir($dir)) { @mkdir($dir, 0777, true); }
    $file = $dir . '/' . md5($ip) . '.json';
    $now = time();
    $window = array();
    if (is_file($file)) {
        $window = json_decode(@file_get_contents($file), true);
        if (!is_array($window)) { $window = array(); }
    }
    $window = array_values(array_filter($window, function ($t) use ($now) { return $t > $now - 60; }));
    if (count($window) >= PIN_RATE_LIMIT) {
        header('Retry-After: 60');
        pin_fail('rate limited', 429);
    }
    $window[] = $now;
    @file_put_contents($file, json_encode($window));
}

function pin_cache_get($key) {
    $file = sys_get_temp_dir() . '/pincache/' . md5($key) . '.json';
    if (is_file($file) && (time() - filemtime($file)) < PIN_CACHE_TTL) {
        $data = @file_get_contents($file);
        if ($data !== false) { return $data; }
    }
    return null;
}

function pin_cache_put($key, $data) {
    $dir = sys_get_temp_dir() . '/pincache';
    if (!is_dir($dir)) { @mkdir($dir, 0777, true); }
    @file_put_contents($dir . '/' . md5($key) . '.json', $data);
}

// ---------------------------------------------------------------------------
// Transport

function pin_curl($url, $extra_headers = array()) {
    $headers = array_merge(array(
        'User-Agent: ' . PIN_UA,
        'Accept: */*',
    ), $extra_headers);
    for ($try = 0; $try < 2; $try++) {
        $ch = curl_init();
        curl_setopt_array($ch, array(
            CURLOPT_URL => $url,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_MAXREDIRS => 5,
            CURLOPT_TIMEOUT => 25,
            CURLOPT_CONNECTTIMEOUT => 10,
            CURLOPT_HTTPHEADER => $headers,
        ));
        $body = curl_exec($ch);
        $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        if ($body !== false && $code >= 200 && $code < 300) {
            return $body;
        }
    }
    return false;
}

/**
 * Calls one of Pinterest's internal JSON resources. No cookies are required;
 * the handler header is what maps the request to the resource.
 */
function pin_api($resource, $handler, $options) {
    $data = json_encode(array('options' => $options, 'context' => array()));
    $url = 'https://www.pinterest.com/resource/' . $resource . '/get/?source_id=&data=' . rawurlencode($data);
    $body = pin_curl($url, array(
        'X-Pinterest-PWS-Handler: ' . $handler,
        'X-Requested-With: XMLHttpRequest',
        'X-Pinterest-AppState: active',
        'Referer: https://www.pinterest.com/',
    ));
    if ($body === false) { pin_fail('pinterest request failed'); }
    $json = json_decode($body, true);
    if (!is_array($json) || !isset($json['resource_response'])) {
        pin_fail('bad resource response');
    }
    $rr = $json['resource_response'];
    if (($rr['status'] ?? '') !== 'success') {
        $msg = $rr['error']['message'] ?? ($rr['message'] ?? 'resource error');
        pin_fail('pinterest: ' . $msg);
    }
    return $rr;
}

// ---------------------------------------------------------------------------
// Field extractors

/** Search results carry signed ids ("e1_xxx"); the numeric id hides in node_id. */
function pin_numeric_id($item) {
    if (isset($item['id']) && ctype_digit((string)$item['id'])) { return (string)$item['id']; }
    if (isset($item['node_id'])) {
        $decoded = base64_decode($item['node_id'], true);
        if (is_string($decoded) && strpos($decoded, 'Pin:') === 0) {
            return substr($decoded, 4);
        }
    }
    return (string)($item['id'] ?? '');
}

function pin_image_url($item, $size = '236x') {
    $images = $item['images'] ?? array();
    if (isset($images[$size]['url'])) { return $images[$size]['url']; }
    foreach ($images as $img) {
        if (is_array($img) && isset($img['url'])) { return $img['url']; }
    }
    return '';
}

function pin_trim($s, $max) {
    $s = trim(preg_replace('/\s+/u', ' ', (string)$s));
    if (function_exists('mb_substr')) {
        return mb_strlen($s) > $max ? mb_substr($s, 0, $max) . '...' : $s;
    }
    return strlen($s) > $max ? substr($s, 0, $max) . '...' : $s;
}

function pin_compact_pin($p) {
    $pinner = $p['pinner'] ?? ($p['closeup_attribution'] ?? array());
    $board = $p['board'] ?? array();
    $slug = '';
    if (isset($board['url']) && preg_match('#^/([^/]+)/([^/]+)/?$#', $board['url'], $m)) {
        $slug = $m[2];
    } elseif (isset($board['slug'])) {
        $slug = (string)$board['slug'];
    }
    return array(
        'id' => pin_numeric_id($p),
        'ti' => pin_trim($p['grid_title'] ?? ($p['title'] ?? ''), 80),
        'de' => pin_trim($p['description'] ?? ($p['closeup_description'] ?? ''), 400),
        'im' => pin_image_url($p, '236x'),
        'im4' => pin_image_url($p, '474x'),
        'un' => (string)($pinner['full_name'] ?? ''),
        'us' => (string)($pinner['username'] ?? ''),
        'bd' => (string)($board['name'] ?? ''),
        'bus' => (string)($board['owner']['username'] ?? ''),
        'bs' => $slug,
        'ct' => (string)($p['created_at'] ?? ''),
    );
}

function pin_compact_board($b) {
    $cover = '';
    foreach (array('cover_images', 'images') as $key) {
        if (isset($b[$key]['236x']['url'])) { $cover = $b[$key]['236x']['url']; break; }
        if (isset($b[$key]['236x'][0]['url'])) { $cover = $b[$key]['236x'][0]['url']; break; }
    }
    if ($cover === '' && !empty($b['image_cover_url'])) { $cover = (string)$b['image_cover_url']; }
    $slug = '';
    if (isset($b['url']) && preg_match('#^/([^/]+)/([^/]+)/?$#', $b['url'], $m)) {
        $slug = $m[2];
    } elseif (isset($b['slug'])) {
        $slug = (string)$b['slug'];
    }
    return array(
        'id' => (string)($b['id'] ?? ''),
        'n' => pin_trim($b['name'] ?? '', 80),
        'd' => pin_trim($b['description'] ?? '', 200),
        'c' => $cover,
        'us' => (string)($b['owner']['username'] ?? ''),
        'sl' => $slug,
        'pc' => (int)($b['pin_count'] ?? 0),
        'fc' => (int)($b['follower_count'] ?? 0),
    );
}

function pin_emit($kind, $items, $next, $extra = array()) {
    $out = array_merge(array(
        'ok' => 1,
        'k' => $kind,
        'it' => array_values($items),
        'nx' => (string)($next ?? ''),
    ), $extra);
    echo json_encode($out);
    exit;
}

// ---------------------------------------------------------------------------
// Operations

pin_rate_limit();

$op = isset($_GET['op']) ? (string)$_GET['op'] : '';
$n = isset($_GET['n']) ? (int)$_GET['n'] : 10;
if ($n < 1) { $n = 10; }
if ($n > 25) { $n = 25; }
$bm = isset($_GET['bm']) ? (string)$_GET['bm'] : '';
$cache_key = $_SERVER['QUERY_STRING'] ?? $op;

$cached = pin_cache_get($cache_key);
if ($cached !== null) { echo $cached; exit; }

ob_start();

switch ($op) {

case 'search': {
    $q = trim((string)($_GET['q'] ?? ''));
    $scope = ($_GET['scope'] ?? 'pins') === 'boards' ? 'boards' : 'pins';
    if ($q === '') { pin_fail('missing q'); }
    $options = array('query' => $q, 'scope' => $scope, 'page_size' => $n);
    if ($bm !== '') { $options['bookmarks'] = array($bm); }
    $rr = pin_api('BaseSearchResource', 'www/search-index/search-page.v2.ts', $options);
    $results = $rr['data']['results'] ?? array();
    $items = array();
    foreach ($results as $r) {
        if (($r['type'] ?? '') === 'promotion' || !empty($r['is_promoted'])) { continue; }
        $items[] = $scope === 'boards' ? pin_compact_board($r) : pin_compact_pin($r);
        if (count($items) >= $n) { break; }   // boards scope ignores page_size
    }
    pin_emit($scope === 'boards' ? 'boards' : 'pins', $items, $rr['bookmark'] ?? '');
    break;
}

case 'user': {
    $u = preg_replace('/[^A-Za-z0-9_]/', '', (string)($_GET['u'] ?? ''));
    if ($u === '') { pin_fail('missing u'); }
    $rr = pin_api('UserResource', 'www/[username].js', array('username' => $u));
    $d = $rr['data'] ?? array();
    if (!is_array($d) || empty($d['username'])) { pin_fail('user not found'); }
    pin_emit('user', array(), '', array('u' => array(
        'us' => (string)$d['username'],
        'un' => (string)($d['full_name'] ?? ''),
        'ab' => pin_trim($d['about'] ?? '', 300),
        'im' => (string)($d['image_medium_url'] ?? ''),
        'fc' => (int)($d['follower_count'] ?? 0),
        'pc' => (int)($d['pin_count'] ?? 0),
        'bc' => (int)($d['board_count'] ?? 0),
    )));
    break;
}

case 'userpins':
case 'userboards': {
    $u = preg_replace('/[^A-Za-z0-9_]/', '', (string)($_GET['u'] ?? ''));
    if ($u === '') { pin_fail('missing u'); }
    if ($op === 'userpins') {
        $options = array(
            'username' => $u, 'page_size' => $n, 'field_set_key' => 'mobile_grid_item',
            'add_vase' => false, 'gated' => true, 'is_own_profile_pins' => false,
        );
        if ($bm !== '') { $options['bookmarks'] = array($bm); }
        $rr = pin_api('UserPinsResource', 'www/[username].js', $options);
        $items = array();
        foreach (($rr['data'] ?? array()) as $p) { $items[] = pin_compact_pin($p); }
        pin_emit('pins', $items, $rr['bookmark'] ?? '');
    } else {
        $options = array(
            'username' => $u, 'field_set_key' => 'profile_grid_item',
            'filter_stories' => false, 'sort' => 'last_pinned_to', 'page_size' => $n,
        );
        if ($bm !== '') { $options['bookmarks'] = array($bm); }
        $rr = pin_api('BoardsFeedResource', 'www/[username].js', $options);
        $items = array();
        foreach (($rr['data'] ?? array()) as $b) {
            if (($b['type'] ?? '') === 'board') { $items[] = pin_compact_board($b); }
        }
        pin_emit('boards', $items, $rr['bookmark'] ?? '');
    }
    break;
}

case 'board':
case 'boardpins': {
    $u = preg_replace('/[^A-Za-z0-9_]/', '', (string)($_GET['u'] ?? ''));
    $s = preg_replace('/[^A-Za-z0-9-]/', '', (string)($_GET['s'] ?? ''));
    if ($u === '' || $s === '') { pin_fail('missing u/s'); }
    if ($op === 'board') {
        $rr = pin_api('BoardResource', 'www/[username]/[slug].js', array('username' => $u, 'slug' => $s));
        $d = $rr['data'] ?? array();
        if (!is_array($d) || empty($d['id'])) { pin_fail('board not found'); }
        $d['url'] = '/' . $u . '/' . $s . '/';
        pin_emit('board', array(), '', array('b' => pin_compact_board($d)));
    } else {
        // BoardFeedResource insists on a numeric board_id; resolve it from
        // username/slug once (the caller may also pass it as &bid=).
        $bid = preg_replace('/[^0-9]/', '', (string)($_GET['bid'] ?? ''));
        if ($bid === '') {
            $info = pin_api('BoardResource', 'www/[username]/[slug].js', array('username' => $u, 'slug' => $s));
            $bid = (string)($info['data']['id'] ?? '');
            if ($bid === '') { pin_fail('board not found'); }
        }
        $options = array(
            'board_id' => $bid, 'page_size' => $n, 'field_set_key' => 'grid_item',
            'add_vase' => false, 'gated' => true,
        );
        if ($bm !== '') { $options['bookmarks'] = array($bm); }
        $rr = pin_api('BoardFeedResource', 'www/[username]/[slug].js', $options);
        $items = array();
        foreach (($rr['data'] ?? array()) as $p) {
            if (($p['type'] ?? '') === 'pin') { $items[] = pin_compact_pin($p); }
        }
        pin_emit('pins', $items, $rr['bookmark'] ?? '');
    }
    break;
}

case 'pin': {
    $id = preg_replace('/[^0-9]/', '', (string)($_GET['id'] ?? ''));
    if ($id === '') { pin_fail('missing id'); }
    // PinResource wants the numeric id under the (oddly named) "id" option.
    // No field_set_key: the closeup field sets come back trimmed; omitting it
    // returns the full pin (images, pinner, board, description).
    $rr = pin_api('PinResource', 'www/pin/[id].js', array('id' => $id));
    $d = $rr['data'] ?? array();
    if (!is_array($d) || empty($d['id'])) { pin_fail('pin not found'); }
    pin_emit('pin', array(pin_compact_pin($d)), '');
    break;
}

default:
    pin_fail('unknown op (see file header for the endpoint list)');
}

$output = ob_get_clean();
pin_cache_put($cache_key, $output);
echo $output;
