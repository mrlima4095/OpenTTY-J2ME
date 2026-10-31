<?php
declare(strict_types=1);

/**
 * GET /s60img/convert.php?url=<image>&w=36&h=36
 *
 * Fetches the source image, center-crops it to the requested size and returns
 * JPEG (photos) or PNG (sources with transparency). Results are cached.
 */

require __DIR__ . '/lib.php';

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'GET') {
    header('Allow: GET');
    s60img_fail(405, 'Method not allowed');
}

s60img_rate_limit();
s60img_gc();

$url = s60img_check_url(isset($_GET['url']) ? (string) $_GET['url'] : '');
if ($url === '') {
    s60img_fail(400, 'Invalid url');
}

$size = s60img_clamp((int) ($_GET['w'] ?? 0), S60IMG_MIN_SIZE, S60IMG_MAX_SIZE, 96);
$height = s60img_clamp((int) ($_GET['h'] ?? 0), S60IMG_MIN_SIZE, S60IMG_MAX_SIZE, $size);

$key = 'c_' . sha1($url . '|' . $size . '|' . $height);

$cached = s60img_cache_read($key, 'jpg');
if ($cached !== null) {
    header('X-S60Img: hit');
    s60img_serve($cached, 'image/jpeg', S60IMG_CACHE_TTL);
    exit;
}
$cached = s60img_cache_read($key, 'png');
if ($cached !== null) {
    header('X-S60Img: hit');
    s60img_serve($cached, 'image/png', S60IMG_CACHE_TTL);
    exit;
}

$body = s60img_download($url);
if ($body === null) {
    s60img_fail(404, 'Image unavailable');
}

$decoded = s60img_decode($body);
if ($decoded === null) {
    s60img_fail(415, 'Unsupported image');
}

$output = s60img_cover($decoded['img'], $decoded['w'], $decoded['h'], $size, $height);
header('X-S60Img: miss');
s60img_emit($output, s60img_has_alpha($decoded['type']), $key, S60IMG_CACHE_TTL);
