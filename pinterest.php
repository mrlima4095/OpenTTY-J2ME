<?php
/**
 * pinterest.php — Pinterest JSON proxy for J2ME clients (CLDC/MIDP devices).
 *
 * NOW WITH SESSION/LOGIN support:
 *   - login → Pinterest username/email + password (sets a per-IP cookie jar)
 *   - qrstart / qrpoll → login by Pinterest QR code (no password/CAPTCHA)
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
 * Password login can be rate-limited by Pinterest when the proxy runs on a
 * datacenter IP. qrstart/qrpoll implement Pinterest's cross-device QR login:
 * the user confirms the login in the Pinterest app on their phone, so the
 * proxy never needs to solve a CAPTCHA or send the password.
 *
 * QR flow:
 *   GET ?op=qrstart
 *     → {"ok":1,"k":"qr","qr":{"token":"...","url":"https://www.pinterest.com/qr_code_login/..."}}
 *   GET ?op=qrpoll&token=...
 *     → {"ok":1,"k":"qr","qr":{"status":"unscanned|scanned|authenticated", ...}}
 *     → when authenticated, the proxy saves the cookies and answers like login:
 *       {"ok":1,"k":"login","u":{"us":"username"}}
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
const PIN_APP_VERSION = 'd601c5e';
const PIN_CACHE_TTL = 600;   // seconds
const PIN_RATE_LIMIT = 90;   // requests per minute per client IP
const PIN_LOGIN_HANDLER = 'www/login/index.js';
const PIN_LOGIN_UI = 'login_page.login_form';
const PIN_QR_TTL = 300;      // seconds a QR token stays valid

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

/**
 * Performs one HTTP request and keeps every Set-Cookie header in $cookies.
 * Unlike pin_curl(), this also returns the HTTP status and the raw body, so
 * login can tell a bad password (200 + JSON error) from a Pinterest block
 * (429 + api_error_code 3051).
 */
function pin_http_request($url, $method = 'GET', $cookies = array(), $headers = array(), $body = null) {
    $jar = is_array($cookies) ? $cookies : array();
    $send = array_merge(array(
        'User-Agent: ' . PIN_UA,
        'Accept-Language: pt-BR,pt;q=0.9,en;q=0.8',
    ), $headers);
    if (!empty($jar)) { $send[] = 'Cookie: ' . pin_cookie_header($jar); }

    $opts = array(
        CURLOPT_URL => $url,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_MAXREDIRS => 5,
        CURLOPT_TIMEOUT => 30,
        CURLOPT_CONNECTTIMEOUT => 10,
        CURLOPT_ENCODING => '',
        CURLOPT_HTTPHEADER => $send,
        CURLOPT_HEADERFUNCTION => function ($ch, $line) use (&$jar) {
            if (preg_match('/^Set-Cookie:\s*([^=]+)=([^;]*)/i', $line, $m)) {
                $jar[trim($m[1])] = trim($m[2], " \t\"");
            }
            return strlen($line);
        },
    );
    if ($method === 'POST') {
        $opts[CURLOPT_POST] = true;
        $opts[CURLOPT_POSTFIELDS] = ($body === null ? '' : $body);
    }

    $ch = curl_init();
    curl_setopt_array($ch, $opts);
    $raw = curl_exec($ch);
    $code = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $err = (string)curl_error($ch);
    curl_close($ch);

    return array('body' => $raw, 'code' => $code, 'cookies' => $jar, 'error' => $err);
}

/**
 * Calls /resource/<Resource>/create/ with the same form fields the Pinterest
 * web app uses: source_url, data=(options,context) and module_path. This works
 * both for UserSessionResource and for ApiResource (which wraps /v3/... URLs).
 */
function pin_resource_post($resource, $options, $cookies, $source_url = '/login/', $module_path = null) {
    $data = json_encode(array('options' => $options, 'context' => array()), JSON_UNESCAPED_SLASHES);
    $form = array('source_url' => $source_url, 'data' => $data);
    if ($module_path !== null) { $form['module_path'] = $module_path; }

    $headers = array(
        'Content-Type: application/x-www-form-urlencoded',
        'Accept: application/json, text/javascript, */*; q=0.01',
        'X-CSRFToken: ' . ($cookies['csrftoken'] ?? ''),
        'X-Requested-With: XMLHttpRequest',
        'X-Pinterest-AppState: active',
        'X-Pinterest-PWS-Handler: ' . PIN_LOGIN_HANDLER,
        'X-Pinterest-Source-Url: ' . $source_url,
        'X-APP-VERSION: ' . PIN_APP_VERSION,
        'Origin: https://www.pinterest.com',
        'Referer: https://www.pinterest.com' . ($source_url === '/login/' ? '/login/' : $source_url),
    );
    return pin_http_request(
        'https://www.pinterest.com/resource/' . $resource . '/create/',
        'POST', $cookies, $headers, http_build_query($form)
    );
}

/**
 * Translates Pinterest's internal error codes into short Portuguese messages.
 * Code 3051 + HTTP 429 is Pinterest's anti-bot/rate-limit response; it is not
 * a wrong password, so telling the user to check their credentials would only
 * cause more failed attempts.
 */
function pin_login_error($rr, $http_code = 0) {
    $e = (is_array($rr) && isset($rr['error']) && is_array($rr['error'])) ? $rr['error'] : array();
    $api = (int)($e['api_error_code'] ?? 0);
    $http = (int)($e['http_status'] ?? $http_code);
    $msg = trim((string)($e['message_detail'] ?? ($e['message'] ?? '')));

    if ($http === 429 || $api === 3051) {
        return 'Pinterest bloqueou temporariamente o login deste servidor. Aguarde alguns minutos e tente de novo.';
    }
    switch ($api) {
        case 6061: return 'Nao foi possivel gerar o QR code. Tente novamente.';
        case 6062: return 'O QR code expirou. Gere um novo.';
        case 6068: return 'QR code ainda nao confirmado no aplicativo do Pinterest.';
        case 30:   return 'Conta do Pinterest nao encontrada.';
        case 82:   return 'Esta conta do Pinterest esta desativada.';
        case 4951: return 'Esta conta usa login SSO; entre pelo site do Pinterest.';
    }
    if ($msg === '' || strcasecmp($msg, 'None') === 0) { $msg = 'credenciais invalidas'; }
    return $msg;
}

function pin_qr_url($token) {
    return 'https://www.pinterest.com/qr_code_login/' . rawurlencode((string)$token);
}

// ---------------------------------------------------------------------------
// Transport

function pin_curl($url, $extra_headers = array()) {
    $headers = array_merge(array(
        'User-Agent: ' . PIN_UA,
        'Accept: */*',
    ), $extra_headers);
    $cookies = pin_state_cookies();
    if (!empty($cookies)) {
        $headers[] = 'Cookie: ' . pin_cookie_header($cookies);
    }
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

$no_cache = in_array($op, array('login', 'logout', 'me', 'home', 'qrstart', 'qrpoll'), true) || pin_logged_user() !== '';
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
    //
    // Pinterest's current front-end sends extra options (get_user,
    // app_type_from_client, ui_component, handler). Older payloads containing
    // only username_or_email/password are treated as bot traffic and often
    // answered with HTTP 429 / api_error_code 3051.
    $user = trim((string)($_POST['username'] ?? $_GET['username'] ?? ''));
    $pass = (string)($_POST['password'] ?? $_GET['password'] ?? '');
    if ($user === '' || $pass === '') { pin_fail('missing credentials'); }
    if (strlen($pass) > 200) { pin_fail('bad credentials'); }

    $page = pin_http_request('https://www.pinterest.com/login/', 'GET', array(), array(
        'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    ));
    if ($page['body'] === false || $page['code'] >= 400) {
        pin_fail('login: nao foi possivel abrir o Pinterest. Tente de novo.');
    }
    $cookies = $page['cookies'];
    if (($cookies['csrftoken'] ?? '') === '') {
        pin_fail('login: Pinterest nao retornou o token de seguranca. Tente de novo.');
    }

    $options = array(
        'username_or_email' => $user,
        'password' => $pass,
        'get_user' => true,
        'app_type_from_client' => 5,
        'visited_pages_before_login' => '[]',
        'ui_component' => PIN_LOGIN_UI,
        'handler' => PIN_LOGIN_HANDLER,
    );
    $res = pin_resource_post(
        'UserSessionResource', $options, $cookies, '/login/',
        'App()>LoginPage()>Login()>Button(class_name=primary, text=Log in, type=submit, size=large)'
    );
    $json = json_decode((string)$res['body'], true);
    $rr = (is_array($json) && isset($json['resource_response'])) ? $json['resource_response'] : array();
    if (($rr['status'] ?? '') !== 'success') {
        if (!is_array($json) && $res['code'] >= 400) {
            $msg = pin_login_error(array(), $res['code']);
        } else {
            $msg = pin_login_error($rr, $res['code']);
        }
        pin_fail('login: ' . $msg);
    }

    $cookies = $res['cookies'];
    $name = $user;
    $d = $rr['data'] ?? array();
    $hasUser = is_array($d) && (!empty($d['user']['id']) || !empty($d['user']['username'])
        || !empty($d['username']) || !empty($d['id']));
    if (is_array($d)) {
        if (!empty($d['username'])) { $name = (string)$d['username']; }
        elseif (!empty($d['user']['username'])) { $name = (string)$d['user']['username']; }
    }
    if (($cookies['_auth'] ?? '') !== '1' && !$hasUser) {
        pin_fail('login: Pinterest nao confirmou a sessao. Tente de novo.');
    }
    pin_state_save(array('user' => $name, 'at' => time(), 'cookies' => $cookies));
    pin_emit('login', array(), '', array('u' => array('us' => $name)));
    break;
}

case 'qrstart': {
    // Cross-device login. The proxy asks Pinterest for a QR token and returns
    // the URL the phone should open/scan. No password or CAPTCHA is involved.
    $page = pin_http_request('https://www.pinterest.com/login/', 'GET', array(), array(
        'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    ));
    if ($page['body'] === false || $page['code'] >= 400) {
        pin_fail('qr: nao foi possivel abrir o Pinterest. Tente de novo.');
    }
    $cookies = $page['cookies'];
    if (($cookies['csrftoken'] ?? '') === '') {
        pin_fail('qr: Pinterest nao retornou o token de seguranca. Tente de novo.');
    }

    $res = pin_resource_post('ApiResource', array('url' => '/v3/qr_auth/'), $cookies);
    $json = json_decode((string)$res['body'], true);
    $rr = (is_array($json) && isset($json['resource_response'])) ? $json['resource_response'] : array();
    $token = (string)($rr['data']['qr_token'] ?? '');
    if (($rr['status'] ?? '') !== 'success' || $token === '') {
        pin_fail('qr: ' . pin_login_error($rr, $res['code']));
    }

    $state = pin_state_load();
    if (!is_array($state)) { $state = array(); }
    $state['qr_token'] = $token;
    $state['qr_at'] = time();
    $state['qr_cookies'] = $cookies;
    pin_state_save($state);

    $url = pin_qr_url($token);
    pin_emit('qr', array(), '', array('qr' => array(
        'token' => $token,
        'url' => $url,
        'status' => 'unscanned',
        'expires' => time() + PIN_QR_TTL,
    )));
    break;
}

case 'qrpoll': {
    // Polled by the client after showing the QR code. When the phone confirms,
    // exchange the token for an authenticated cookie jar and save it.
    $state = pin_state_load();
    if (!is_array($state)) { $state = array(); }
    $stored = (string)($state['qr_token'] ?? '');
    $token = trim((string)($_GET['token'] ?? $stored));
    if ($stored === '' || $token === '') {
        pin_fail('qr: nenhum codigo ativo; comece de novo.');
    }
    if (!hash_equals($stored, $token)) {
        pin_fail('qr: codigo invalido.');
    }

    if (!empty($state['qr_at']) && (time() - (int)$state['qr_at']) > PIN_QR_TTL) {
        unset($state['qr_token'], $state['qr_at'], $state['qr_cookies']);
        pin_state_save($state);
        pin_fail('qr: codigo expirado; gere um novo.');
    }

    $cookies = (isset($state['qr_cookies']) && is_array($state['qr_cookies']))
        ? $state['qr_cookies'] : array();

    $res = pin_resource_post('ApiResource', array(
        'url' => '/v3/qr_auth/status/',
        'data' => array('qr_token' => $token),
    ), $cookies);
    $json = json_decode((string)$res['body'], true);
    $rr = (is_array($json) && isset($json['resource_response'])) ? $json['resource_response'] : array();
    if (($rr['status'] ?? '') !== 'success') {
        pin_fail('qr: ' . pin_login_error($rr, $res['code']));
    }

    $status = (string)($rr['data']['status'] ?? 'unscanned');
    if ($status !== 'authenticated') {
        pin_emit('qr', array(), '', array('qr' => array(
            'token' => $token,
            'url' => pin_qr_url($token),
            'status' => $status,
            'expires' => (int)($state['qr_at'] ?? time()) + PIN_QR_TTL,
        )));
        break;
    }

    $res = pin_resource_post('ApiResource', array(
        'url' => '/v3/login/qr_auth/',
        'data' => array('qr_token' => $token),
    ), $cookies);
    $json = json_decode((string)$res['body'], true);
    $rr = (is_array($json) && isset($json['resource_response'])) ? $json['resource_response'] : array();
    if (($rr['status'] ?? '') !== 'success') {
        pin_fail('qr: ' . pin_login_error($rr, $res['code']));
    }

    $name = (string)($state['user'] ?? '');
    $d = $rr['data'] ?? array();
    $hasUser = is_array($d) && (!empty($d['user']['id']) || !empty($d['user']['username'])
        || !empty($d['username']) || !empty($d['id']));
    if (is_array($d)) {
        if (!empty($d['username'])) { $name = (string)$d['username']; }
        elseif (!empty($d['user']['username'])) { $name = (string)$d['user']['username']; }
    }
    if (($res['cookies']['_auth'] ?? '') !== '1' && !$hasUser && $name === '') {
        pin_fail('qr: Pinterest confirmou o QR code, mas nao devolveu a sessao. Tente de novo.');
    }
    if ($name === '') { $name = 'qr'; }

    $state['user'] = $name;
    $state['at'] = time();
    $state['cookies'] = $res['cookies'];
    unset($state['qr_token'], $state['qr_at'], $state['qr_cookies']);
    pin_state_save($state);
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