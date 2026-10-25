<?php
/**
 * MIDPlay network speed test endpoint.
 *
 * GET  ?op=ping
 *   -> {"ok":1}
 * GET  ?op=download&bytes=N
 *   -> N zero bytes (application/octet-stream)
 * POST ?op=upload
 *   -> {"ok":1,"n":<received bytes>}
 *
 * The phone measures the elapsed time around each request. The download is
 * capped so a single test cannot pull more than 8 MiB from the server; the
 * client is expected to stop reading once it has enough data.
 */

header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');

$op = isset($_GET['op']) ? (string)$_GET['op'] : 'ping';

if ($op === 'ping') {
    header('Content-Type: application/json; charset=utf-8');
    echo '{"ok":1}';
    exit;
}

if ($op === 'upload') {
    $received = 0;
    $in = fopen('php://input', 'rb');
    if ($in !== false) {
        while (!feof($in)) {
            $chunk = fread($in, 65536);
            if ($chunk === false || $chunk === '') { break; }
            $received += strlen($chunk);
        }
        fclose($in);
    }
    header('Content-Type: application/json; charset=utf-8');
    echo '{"ok":1,"n":' . $received . '}';
    exit;
}

if ($op === 'download') {
    $bytes = isset($_GET['bytes']) ? intval($_GET['bytes']) : 2097152;
    if ($bytes < 65536) { $bytes = 65536; }
    if ($bytes > 8388608) { $bytes = 8388608; }

    @ini_set('output_buffering', 'off');
    @ini_set('zlib.output_compression', '0');
    while (ob_get_level() > 0) { @ob_end_flush(); }

    header('Content-Type: application/octet-stream');
    header('Content-Length: ' . $bytes);
    header('X-Accel-Buffering: no');

    $block = str_repeat('0', 65536);
    $sent = 0;
    while ($sent < $bytes) {
        if (connection_aborted()) { break; }
        $size = min(65536, $bytes - $sent);
        echo ($size === 65536) ? $block : substr($block, 0, $size);
        flush();
        $sent += $size;
    }
    exit;
}

header('Content-Type: application/json; charset=utf-8');
echo '{"ok":0,"e":"bad_op"}';
