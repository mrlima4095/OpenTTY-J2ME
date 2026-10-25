<?php
/**
 * MIDPlay translation proxy.
 *
 * POST (or GET) q=<text>&tl=<language>
 *   -> {"ok":1,"t":"translated text"}
 *   -> {"ok":0,"e":"code"}
 *
 * Google's public dictionary endpoint (the one browser extensions call, no key)
 * is tried first; MyMemory is the fallback. Long notes are split into chunks at
 * line boundaries so the upstream GET URL stays small, and the translated
 * chunks are joined back with the original line breaks.
 */

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');

const TR_MAX_CHARS = 16000;
const TR_CHUNK_CHARS = 1400;
const TR_MAX_CHUNKS = 16;
const TR_RATE_LIMIT = 30; // requests per minute per client IP
const TR_UA = 'MIDPlay/1.0 (J2ME; +http://opentty.fun)';

function tr_send(array $data) {
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function tr_fail($code) {
    tr_send(array('ok' => 0, 'e' => (string)$code));
}

function tr_rate_limit() {
    $ip = isset($_SERVER['REMOTE_ADDR']) ? $_SERVER['REMOTE_ADDR'] : 'cli';
    $dir = sys_get_temp_dir() . '/midplay-translate';
    if (!is_dir($dir)) { @mkdir($dir, 0777, true); }
    $file = $dir . '/' . md5($ip) . '.json';
    $now = time();
    $window = array();
    if (is_file($file)) {
        $window = json_decode((string)@file_get_contents($file), true);
        if (!is_array($window)) { $window = array(); }
    }
    $fresh = array();
    foreach ($window as $t) {
        if (is_numeric($t) && $t > $now - 60) { $fresh[] = (int)$t; }
    }
    if (count($fresh) >= TR_RATE_LIMIT) {
        http_response_code(429);
        header('Retry-After: 60');
        tr_fail('rate_limited');
    }
    $fresh[] = $now;
    @file_put_contents($file, json_encode($fresh));
}

function tr_fetch_json($url) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 25);
    curl_setopt($ch, CURLOPT_USERAGENT, TR_UA);
    curl_setopt($ch, CURLOPT_HTTPHEADER, array('Accept: application/json'));
    $body = curl_exec($ch);
    $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $err = curl_error($ch);
    curl_close($ch);
    if ($body === false || $err !== '' || $code < 200 || $code >= 300) {
        return null;
    }
    $data = json_decode($body, true);
    return is_array($data) ? $data : null;
}

/** Joins Google's segment response ([["text","en"],...]) into one string. */
function tr_segments($data) {
    $out = '';
    if (!is_array($data)) { return ''; }
    foreach ($data as $seg) {
        if (is_array($seg) && isset($seg[0]) && is_string($seg[0])) {
            $out .= $seg[0];
        }
    }
    return $out;
}

/** One upstream round trip; returns the translated string or ''. */
function tr_translate($text, $tl) {
    $text = (string)$text;
    if (trim($text) === '') { return ''; }
    $g = tr_fetch_json(
        'https://clients5.google.com/translate_a/t?client=dict-chrome-ex'
        . '&sl=auto&tl=' . urlencode($tl) . '&q=' . urlencode($text)
    );
    if (is_array($g)) {
        $out = tr_segments($g);
        if ($out !== '') { return $out; }
    }
    $m = tr_fetch_json(
        'https://api.mymemory.translated.net/get?q=' . urlencode($text)
        . '&langpair=Autodetect|' . urlencode($tl)
    );
    if (is_array($m) && isset($m['responseData']['translatedText'])) {
        $t = (string)$m['responseData']['translatedText'];
        if ($t !== '') { return $t; }
    }
    return '';
}

/**
 * Splits text into chunks of at most $max characters, breaking at line
 * boundaries when possible. Each chunk is returned as an array of lines so the
 * caller can join translated chunks with exactly one line break.
 */
function tr_chunks($text, $max) {
    $lines = preg_split('/\r\n|\r|\n/', (string)$text);
    if (!is_array($lines)) { $lines = array((string)$text); }
    $chunks = array();
    $current = array();
    $size = 0;
    foreach ($lines as $line) {
        while (mb_strlen($line, 'UTF-8') > $max) {
            if (count($current) > 0) {
                $chunks[] = implode("\n", $current);
                $current = array();
                $size = 0;
            }
            $chunks[] = mb_substr($line, 0, $max, 'UTF-8');
            $line = mb_substr($line, $max, mb_strlen($line, 'UTF-8'), 'UTF-8');
        }
        $len = mb_strlen($line, 'UTF-8') + (count($current) > 0 ? 1 : 0);
        if ($size + $len > $max && count($current) > 0) {
            $chunks[] = implode("\n", $current);
            $current = array();
            $size = 0;
            $len = mb_strlen($line, 'UTF-8');
        }
        $current[] = $line;
        $size += $len;
    }
    $chunks[] = implode("\n", $current);
    return $chunks;
}

function tr_target($value) {
    $tl = trim((string)$value);
    if (!preg_match('/^[A-Za-z][A-Za-z-]{1,9}$/', $tl)) { return 'en'; }
    return strtolower($tl);
}

function tr_clean($value) {
    $text = (string)$value;
    if (!mb_check_encoding($text, 'UTF-8')) {
        $text = @mb_convert_encoding($text, 'UTF-8', 'UTF-8');
    }
    $text = str_replace("\r\n", "\n", $text);
    $text = str_replace("\r", "\n", $text);
    return trim($text);
}

tr_rate_limit();

$q = isset($_POST['q']) ? $_POST['q'] : (isset($_GET['q']) ? $_GET['q'] : '');
$text = tr_clean($q);
if ($text === '') { tr_fail('missing_text'); }
if (mb_strlen($text, 'UTF-8') > TR_MAX_CHARS) { tr_fail('too_large'); }

$tl = tr_target(isset($_POST['tl']) ? $_POST['tl'] : (isset($_GET['tl']) ? $_GET['tl'] : 'en'));
$chunks = tr_chunks($text, TR_CHUNK_CHARS);
if (count($chunks) > TR_MAX_CHUNKS) { tr_fail('too_large'); }

$parts = array();
foreach ($chunks as $chunk) {
    $translated = tr_translate($chunk, $tl);
    if ($translated === '') { tr_fail('translate_failed'); }
    $parts[] = trim($translated, "\n");
}
tr_send(array('ok' => 1, 't' => implode("\n", $parts)));
