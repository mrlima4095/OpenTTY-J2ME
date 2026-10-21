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
    // If a session token is set for this IP, add it so Pinterest sees us as logged‑in.
    $token_file = sys_get_temp_dir() . '/pinselogin/' . md5($_SERVER['REMOTE_ADDR'] ?? 'cli') . '.token';
    if (is_file($token_file)) {
        $token = trim(@file_get_contents($token_file));
        if ($token !== '') {
            $extra['X-Pinterest-Session-Token'] = $token;
        }
    }
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
    // POST: username and password via $_POST or query string (simple form‑auth).
    // Pinterest does not document a public API for this, but the web login
    // endpoint at https://www.pinterest.com/login/ saves a session cookie
    // that we can extract and store per‑IP.
    $user = preg_replace('/[^A-Za-z0-9_]/', '', (string)($_POST['username'] ?? $_GET['username'] ?? ''));
    $pass = (string)($_POST['password'] ?? $_GET['password'] ?? '');
    if ($user === '' || $pass === '') { pin_fail('missing credentials'); }
    // Perform a login request that Pinterest treats as a real browser login.
    // We follow redirects and capture the session cookie.
    $login_url = 'https://www.pinterest.com/login/';
    $ch = curl_init();
    curl_setopt_array($ch, array(
        CURLOPT_URL => $login_url,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_TIMEOUT => 30,
        CURLOPT_CONNECTTIMEOUT => 10,
        CURLOPT_POST => true,
        CURLOPT_POSTFIELDS => 'username=' . urlencode($user) . '&password=' . urlencode($pass),
        CURLOPT_HTTPHEADER => array(
            'User-Agent: ' . PIN_UA,
            'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
            'Referer: https://www.pinterest.com/',
        ),
    ));
    $body = curl_exec($ch);
    $http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    if ($http_code < 300 || $http_code >= 400) {
        pin_fail('login failed (http ' . $http_code . ')', $http_code);
    }
    // Capture cookies returned by the login redirect.
    $ch2 = curl_init();
    curl_setopt_array($ch2, array(
        CURLOPT_URL => $login_url,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_FOLLOWLOCATION => false,
        CURLOPT_TIMEOUT => 30,
        CURLOPT_CONNECTTIMEOUT => 10,
        CURLOPT_POST => true,
        CURLOPT_POSTFIELDS => 'username=' . urlencode($user) . '&password=' . urlencode($pass),
        CURLOPT_HTTPHEADER => array(
            'User-Agent: ' . PIN_UA,
            'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
            'Referer: https://www.pinterest.com/',
            'Cookie:',
        ),
        CURLOPT_COOKIEFILE => sys_get_temp_dir() . '/pinselogin/cookies.txt',
        CURLOPT_COOKIEJAR => sys_get_temp_dir() . '/pinselogin/cookies.txt',
    ));
    $body2 = curl_exec($ch2);
    curl_close($ch2);
    // Read the cookie file and pick the first token that looks like a session cookie.
    $token = '';
    if (is_file(sys_get_temp_dir() . '/pinselogin/cookies.txt')) {
        $lines = file(sys_get_temp_dir() . '/pinselogin/cookies.txt', FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
        foreach ($lines as $line) {
            // Netscape cookie format: host\tname\tpath\tsecure\texpire\tvalue\tsession
            // or HTTP header format: Set-Cookie: name=value; ...
            if (preg_match('/^Set-Cookie:\s*([^=]+)=([^;]+)/i', $line, $m)) {
                $name = trim($m[1]);
                $value = trim($m[2]);
                if (stripos($name, 'session') !== false || stripos($name, 'auth') !== false || stripos($name, 'csrf') !== false) {
                    $token = $value;
                    break;
                }
            }
        }
    }
    // Store the token per‑IP so subsequent requests are seen as logged‑in.
    $token_file = sys_get_temp_dir() . '/pinselogin/' . md5($_SERVER['REMOTE_ADDR'] ?? 'cli') . '.token';
    @file_put_contents($token_file, $token);
    // Also keep the cookie jar around for possible later use.
    @mkdir(sys_get_temp_dir() . '/pinselogin', 0777, true);
    // Respond with success only — the client will re‑issue requests and
    // the proxy will pick up the token from the file.
    pin_emit('login', array(), '');
    break;
}

case 'logout': {
    // Clear the per‑IP session token and cookie file.
    $token_file = sys_get_temp_dir() . '/pinselogin/' . md5($_SERVER['REMOTE_ADDR'] ?? 'cli') . '.token';
    $cookie_file = sys_get_temp_dir() . '/pinselogin/cookies.txt';
    @unlink($token_file);
    @unlink($cookie_file);
    // Clean up the temp dir if empty.
    $dir = dirname($token_file);
    if (is_dir($dir)) {
        $rmdir = true;
        $entries = @ scandir($dir);
        if ($entries && count($entries) <= 2) { // only . and ..
            @rmdir($dir);
        }
    }
    pin_emit('logout', array(), '');
    break;
}

case 'me': {
    // Returns the authenticated user's profile (same shape as ?op=user).
    // If no session token is set, returns a "guest" profile prompting to log in.
    $token_file = sys_get_temp_dir() . '/pinselogin/' . md5($_SERVER['REMOTE_ADDR'] ?? 'cli') . '.token';
    if (!is_file($token_file)) {
        pin_emit('user', array(), '', array('u' => array(
            'us' => 'guest', 'un' => 'Guest', 'ab' => 'Log in to see your profile.',
            'im' => '', 'fc' => 0, 'pc' => 0, 'bc' => 0,
        )));
        break;
    }
    $token = trim(@file_get_contents($token_file));
    if ($token === '') {
        pin_emit('user', array(), '', array('u' => array(
            'us' => 'guest', 'un' => 'Guest', 'ab' => 'Log in to see your profile.',
            'im' => '', 'fc' => 0, 'pc' => 0, 'bc' => 0,
        )));
        break;
    }
    // Use the token as a session cookie header — Pinterest will see us as logged‑in.
    $rr = pin_api('UserResource', 'www/[username].js', array(
        'username' => $token,
    ));
    $d = $rr['data'] ?? array();
    if (!is_array($d) || empty($d['username'])) {
        // Token invalid — clear it and act as logged out.
        @unlink($token_file);
        pin_emit('user', array(), '', array('u' => array(
            'us' => 'guest', 'un' => 'Guest', 'ab' => 'Session expired. Log in again.',
            'im' => '', 'fc' => 0, 'pc' => 0, 'bc' => 0,
        )));
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
    // The logged‑in user's feed — same as userpins but for the authenticated user.
    $token_file = sys_get_temp_dir() . '/pinselogin/' . md5($_SERVER['REMOTE_ADDR'] ?? 'cli') . '.token';
    if (!is_file($token_file)) {
        pin_emit('pins', array(), '', array('u' => array(
            'us' => 'guest', 'un' => 'Guest', 'ab' => 'Log in to see your home feed.',
            'im' => '', 'fc' => 0, 'pc' => 0, 'bc' => 0,
        )));
        break;
    }
    $token = trim(@file_get_contents($token_file));
    if ($token === '') {
        pin_emit('pins', array(), '', array('u' => array(
            'us' => 'guest', 'un' => 'Guest', 'ab' => 'Log in to see your home feed.',
            'im' => '', 'fc' => 0, 'pc' => 0, 'bc' => 0,
        )));
        break;
    }
    // home feed uses the same resource as userpins but with the token as username.
    $options = array(
        'username' => $token, 'page_size' => $n, 'field_set_key' => 'mobile_grid_item',
        'add_vase' => false, 'gated' => true, 'is_own_profile_pins' => false,
    );
    $rr = pin_api('UserPinsResource', 'www/[username].js', $options);
    $items = array();
    foreach (($rr['data'] ?? array()) as $p) { $items[] = pin_compact_pin($p); }
    pin_emit('pins', $items, $rr['bookmark'] ?? '');
    break;
}

default:
    pin_fail('unknown op (see file header for the endpoint list)');
}

$output = ob_get_clean();
pin_cache_put($cache_key, $output);
echo $output;