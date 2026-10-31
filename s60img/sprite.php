<?php
declare(strict_types=1);

/**
 * POST /s60img/sprite.php
 *
 * Body: {"urls":["...","..."],"width":36,"height":36,"cols":4}
 * Response: one PNG with the images laid out in a row-major grid. Cells whose
 * image could not be fetched stay transparent, so the client can keep slicing
 * by fixed offsets.
 */

require __DIR__ . '/lib.php';

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'POST') {
    header('Allow: POST');
    s60img_fail(405, 'Method not allowed');
}

s60img_rate_limit();
s60img_gc();

$raw = file_get_contents('php://input', false, null, 0, 262144);
if ($raw === false || trim($raw) === '') {
    s60img_fail(400, 'Missing JSON body');
}

try {
    $payload = json_decode($raw, true, 8, JSON_THROW_ON_ERROR);
} catch (JsonException $e) {
    s60img_fail(400, 'Invalid JSON body');
}

if (!is_array($payload) || !isset($payload['urls']) || !is_array($payload['urls'])) {
    s60img_fail(400, 'Missing urls');
}

$urls = [];
foreach ($payload['urls'] as $url) {
    if (is_string($url) && $url !== '') {
        $urls[] = $url;
    }
    if (count($urls) >= S60IMG_MAX_URLS) {
        break;
    }
}
if ($urls === []) {
    s60img_fail(400, 'No urls');
}

$cellWidth = s60img_clamp(
    (int) ($payload['width'] ?? 0),
    S60IMG_MIN_SIZE,
    S60IMG_MAX_SIZE,
    36
);
$cellHeight = s60img_clamp(
    (int) ($payload['height'] ?? 0),
    S60IMG_MIN_SIZE,
    S60IMG_MAX_SIZE,
    $cellWidth
);
$cols = s60img_clamp((int) ($payload['cols'] ?? 0), 1, S60IMG_MAX_COLS, 4);
$cols = min($cols, count($urls));
$rows = (int) ceil(count($urls) / $cols);

$key = 's_' . sha1(implode("\n", $urls) . '|' . $cellWidth . '|' . $cellHeight . '|' . $cols);
$cached = s60img_cache_read($key, 'png');
if ($cached !== null) {
    header('X-S60Img: hit');
    s60img_serve($cached, 'image/png', S60IMG_SPRITE_TTL);
    exit;
}

$bodies = s60img_download_many($urls);

$canvas = imagecreatetruecolor($cellWidth * $cols, $cellHeight * $rows);
imagealphablending($canvas, false);
imagesavealpha($canvas, true);
$transparent = imagecolorallocatealpha($canvas, 0, 0, 0, 127);
imagefilledrectangle(
    $canvas,
    0,
    0,
    $cellWidth * $cols - 1,
    $cellHeight * $rows - 1,
    $transparent
);
imagealphablending($canvas, true);

$complete = true;
foreach ($urls as $index => $url) {
    $body = $bodies[$url] ?? null;
    if ($body === null) {
        $complete = false;
        continue;
    }
    $decoded = s60img_decode($body);
    if ($decoded === null) {
        $complete = false;
        continue;
    }
    $cell = s60img_cover(
        $decoded['img'],
        $decoded['w'],
        $decoded['h'],
        $cellWidth,
        $cellHeight
    );
    $x = ($index % $cols) * $cellWidth;
    $y = (int) (floor($index / $cols) * $cellHeight);
    imagecopy($canvas, $cell, $x, $y, 0, 0, $cellWidth, $cellHeight);
    imagedestroy($cell);
    imagedestroy($decoded['img']);
}

header('X-S60Img: miss');
// Partial sheets are served but not cached: the client may retry soon and the
// missing cells are usually a transient upstream failure.
s60img_emit($canvas, true, $complete ? $key : '', S60IMG_SPRITE_TTL);
