<?php
/**
 * pinterest.php — Pinterest JSON proxy for J2ME clients (CLDC/MIDP devices).
 *
 * NOW WITH SESSION/LOGIN support:
 *   - Pinterest login (POST username+password) → sets a per-IP cookie token
 *   - logout → clears the token
 *   - me → returns the authenticated user's profile
 *   - home → the logged-in user's feed (pins from followed boards)
 *
 * Session handling:
 *   The script stores a per-IP opaque "token" (the session cookie value
 *   Pinterest returns) in a temp-file. The client sends this token back
 *   as the header X-Pinterest-Session-Token on subsequent requests.
 *   The proxy then adds that header to every Pinterest API call so that
 *   Pinterest resources see the user as logged‑in.
 *
 * All existing ops (search, user, userpins, userboards, board, boardpins, pin)
 * continue to work ANONYMOUSLY — no cookies needed for those.
 *
 * The "home" op uses the same internal resources as userpins but filtered
 * to the authenticated user's feed.
 *
 * Image serving still goes straight from i.pinimg.com; the client fetches
 * them through the generic proxy.php. Nothing is relayed through this server.
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


function pin_guest_profile($msg) {
    return array('us' => 'guest', 'un' => 'Guest', 'ab' => $msg,
        'im' => '', 'fc' => 0, 'pc' => 0, 'bc' => 0);
}

// ---------------------------------------------------------------------------
// Per-client session state (cookie jar + username)

function pin_state_dir() {
    $d = sys_get_temp_dir() . '/pinselogin';
    if (!is_dir($d)) { @mkdir($d, 0777, true); }
    return $d;
}

function pin_state_file() {
    return pin_state_dir() . '/' . md5($_SERVER['REMOTE_ADDR'] ?? 'cli') . '.json';
}

function pin_state_load() {
    $f = pin_state_file();
    if (!is_file($f)) { return null; }
    $j = json_decode((string)@file_get_contents($f), true);
    return is_array($j) ? $j : null;
}

function pin_state_save($arr) {
    @file_put_contents(pin_state_file(), json_encode($arr));
}

function pin_state_clear() {
    @unlink(pin_state_file());
}

function pin_logged_user() {
    $s = pin_state_load();
    return ($s && !empty($s['user'])) ? (string)$s['user'] : '';
}

function pin_state_cookies() {
    $s = pin_state_load();
    return ($s && !empty($s['cookies']) && is_array($s['cookies'])) ? $s['cookies'] : array();
}

function pin_cookie_header($cookies) {
    $parts = array();
    foreach ($cookies as $k => $v) { $parts[] = $k . '=' . $v; }
    return implode('; ', $parts);
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
        $opts = array(
            CURLOPT_URL => $url,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_MAXREDIRS => 5,
            CURLOPT_TIMEOUT => 25,
            CURLOPT_CONNECTTIMEOUT => 10,
            CURLOPT_ENCODING => '',
            CURLOPT_HTTPHEADER => $headers,
        );
        $cookies = pin_state_cookies();
        if (!empty($cookies)) {
            $headers[] = 'Cookie: ' . pin_cookie_header($cookies);
            $opts[CURLOPT_HTTPHEADER] = $headers;
        }
        curl_setopt_array($ch, $opts);
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
 * Calls one of Pinterest's internal JSON resources. No cookies are required
 * for anonymous calls; if a session token is set (via login) the handler
 * header includes it so Pinterest treats the request as logged‑in.
 */
function pin_api($resource, $handler, $options) {
    $data = json_encode(array('options' => $options, 'context' => array()));
    $url = 'https://www.pinterest.com/resource/' . $resource . '/get/?source_id=&data=' . rawurlencode($data);
    $extra = array(
        'X-Pinterest-PWS-Handler: ' . $handler,
        'X-Requested-With: XMLHttpRequest',
        'X-Pinterest-AppState: active',
        'Referer: https://www.pinterest.com/',
    );
    $body = pin_curl($url, $extra);
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
// Field extractors (same as before, unchanged)

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
    if (function_exists('mb_strlen')) {
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

$no_cache = in_array($op, array('login', 'logout', 'me', 'home'), true) || pin_logged_user() !== '';
$cached = $no_cache ? null : pin_cache_get($cache_key);
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
        if (count($items) >= $n) { break; }
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

case 'login': {
    // Current Pinterest web flow: GET /login/ for csrf + cookies, then POST
    // UserSessionResource/create/. Cookies are stored per client and reused by
    // every later pin_api() call, so the feed/profile come back authenticated.
    $user = trim((string)($_POST['username'] ?? $_GET['username'] ?? ''));
    $pass = (string)($_POST['password'] ?? $_GET['password'] ?? '');
    if ($user === '' || $pass === '') { pin_fail('missing credentials'); }
    if (strlen($pass) > 200) { pin_fail('bad credentials'); }

    $cookies = array();
    $collect = function ($ch, $line) use (&$cookies) {
        if (preg_match('/^Set-Cookie:\s*([^=]+)=([^;]*)/i', $line, $m)) {
            $cookies[trim($m[1])] = trim($m[2], " \t\"");
        }
        return strlen($line);
    };

    $ch = curl_init();
    curl_setopt_array($ch, array(
        CURLOPT_URL => 'https://www.pinterest.com/login/',
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_MAXREDIRS => 5,
        CURLOPT_TIMEOUT => 30,
        CURLOPT_CONNECTTIMEOUT => 10,
        CURLOPT_USERAGENT => PIN_UA,
        CURLOPT_ENCODING => '',
        CURLOPT_HEADERFUNCTION => $collect,
        CURLOPT_HTTPHEADER => array(
            'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
            'Accept-Language: pt-BR,pt;q=0.9,en;q=0.8',
        ),
    ));
    curl_exec($ch);
    curl_close($ch);

    $csrf = $cookies['csrftoken'] ?? '';
    if ($csrf === '') { pin_fail('login: no csrf token'); }

    $options = array('username_or_email' => $user, 'password' => $pass);
    $payload = 'source_url=' . rawurlencode('/login/')
        . '&data=' . rawurlencode(json_encode(array('options' => $options, 'context' => array())));

    $ch = curl_init();
    curl_setopt_array($ch, array(
        CURLOPT_URL => 'https://www.pinterest.com/resource/UserSessionResource/create/',
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT => 30,
        CURLOPT_CONNECTTIMEOUT => 10,
        CURLOPT_POST => true,
        CURLOPT_POSTFIELDS => $payload,
        CURLOPT_USERAGENT => PIN_UA,
        CURLOPT_ENCODING => '',
        CURLOPT_HEADERFUNCTION => $collect,
        CURLOPT_HTTPHEADER => array(
            'Content-Type: application/x-www-form-urlencoded',
            'Accept: application/json, text/javascript, */*; q=0.01',
            'Accept-Language: pt-BR,pt;q=0.9,en;q=0.8',
            'Cookie: ' . pin_cookie_header($cookies),
            'X-CSRFToken: ' . $csrf,
            'X-Requested-With: XMLHttpRequest',
            'X-Pinterest-AppState: active',
            'X-Pinterest-Source-Url: /login/',
            'Origin: https://www.pinterest.com',
            'Referer: https://www.pinterest.com/login/',
        ),
    ));
    $body = curl_exec($ch);
    curl_close($ch);

    $json = json_decode((string)$body, true);
    $rr = (is_array($json) && isset($json['resource_response'])) ? $json['resource_response'] : array();
    if (($rr['status'] ?? '') !== 'success') {
        $msg = $rr['error']['message_detail'] ?? ($rr['error']['message'] ?? 'credenciais invalidas');
        pin_fail('login: ' . $msg);
    }

    if (empty($cookies['_pinterest_sess'])) {
        pin_fail('login: sessao nao criada');
    }

    $name = $user;
    $d = $rr['data'] ?? array();
    if (is_array($d)) {
        if (!empty($d['username'])) { $name = (string)$d['username']; }
        elseif (!empty($d['user']['username'])) { $name = (string)$d['user']['username']; }
    }
    pin_state_save(array('user' => $name, 'at' => time(), 'cookies' => $cookies));
    pin_emit('login', array(), '', array('u' => array('us' => $name)));
    break;
}

case 'logout': {
    pin_state_clear();
    pin_emit('logout', array(), '');
    break;
}

case 'me': {
    // Authenticated profile; guests get a stub explaining they must log in.
    $u = pin_logged_user();
    if ($u === '') {
        pin_emit('user', array(), '', array('u' => pin_guest_profile('Entre para ver seu perfil.')));
        break;
    }
    try {
        $rr = pin_api('UserResource', 'www/[username].js', array('username' => $u));
    } catch (Throwable $e) {
        pin_state_clear();
        pin_emit('user', array(), '', array('u' => pin_guest_profile('Sessao expirada. Entre de novo.')));
        break;
    }
    $d = $rr['data'] ?? array();
    if (!is_array($d) || empty($d['username'])) {
        pin_state_clear();
        pin_emit('user', array(), '', array('u' => pin_guest_profile('Sessao expirada. Entre de novo.')));
        break;
    }
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

case 'home': {
    // Logged-in home feed (UserHomefeedResource requires session cookies).
    if (pin_logged_user() === '') {
        pin_emit('pins', array(), '', array('u' => pin_guest_profile('Entre para ver seu feed.')));
        break;
    }
    try {
        $rr = pin_api('UserHomefeedResource', 'www/index.js', array(
            'page_size' => (int)$n,
            'field_set_key' => 'mobile_grid_item',
        ));
    } catch (Throwable $e) {
        pin_emit('pins', array(), '', array('u' => pin_guest_profile('Sessao expirada. Entre de novo.')));
        break;
    }
    $items = array();
    foreach (($rr['data'] ?? array()) as $p) {
        if (is_array($p)) { $items[] = pin_compact_pin($p); }
    }
    pin_emit('pins', $items, $rr['bookmark'] ?? '');
    break;
}

default:
    pin_fail('unknown op (see file header for the endpoint list)');
}

$output = ob_get_clean();
if (!$no_cache) { pin_cache_put($cache_key, $output); }
echo $output;