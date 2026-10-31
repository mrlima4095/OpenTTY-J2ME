<?php
declare(strict_types=1);

/**
 * s60img — image proxy used by MIDPlay for list thumbnails and sprite sheets.
 *
 * Endpoints:
 *   convert.php?url=...&w=36&h=36   resized/cropped single image
 *   sprite.php  (POST JSON)         grid of images in one PNG
 *
 * Images are fetched with cURL (parallel for sprites), resized with GD and
 * cached on disk. The cache lives in the system temp directory because the
 * web root is read-only inside the container.
 */

const S60IMG_CACHE_DIR = '/tmp/s60img-cache';
const S60IMG_CACHE_TTL = 604800;      // 7 days
const S60IMG_SPRITE_TTL = 21600;      // 6 hours
const S60IMG_MAX_BYTES = 4194304;     // 4 MiB per source image
const S60IMG_MAX_SIDE = 6000;         // reject absurd source dimensions
const S60IMG_CONNECT_TIMEOUT = 4;
const S60IMG_TIMEOUT = 8;
const S60IMG_CONCURRENCY = 6;
const S60IMG_RATE_LIMIT = 240;        // requests per IP per window
const S60IMG_RATE_WINDOW = 60;
const S60IMG_MAX_URLS = 64;
const S60IMG_MIN_SIZE = 16;
const S60IMG_MAX_SIZE = 600;
const S60IMG_MAX_COLS = 16;

function s60img_fail(int $code, string $message): void
{
    http_response_code($code);
    header('Content-Type: text/plain; charset=UTF-8');
    header('Cache-Control: no-store');
    echo $message;
    exit;
}

function s60img_cache_dir(): string
{
    $dir = S60IMG_CACHE_DIR;
    if (!is_dir($dir)) {
        @mkdir($dir, 0775, true);
    }
    return $dir;
}

function s60img_cache_read(string $key, string $ext): ?string
{
    $path = s60img_cache_dir() . '/' . $key . '.' . $ext;
    if (!is_file($path)) {
        return null;
    }
    if (filemtime($path) + S60IMG_CACHE_TTL < time()) {
        @unlink($path);
        return null;
    }
    $body = @file_get_contents($path);
    return $body === false ? null : $body;
}

function s60img_cache_write(string $key, string $ext, string $body): void
{
    $path = s60img_cache_dir() . '/' . $key . '.' . $ext;
    $tmp = $path . '.' . bin2hex(random_bytes(4)) . '.tmp';
    if (@file_put_contents($tmp, $body) !== false) {
        @rename($tmp, $path);
    } else {
        @unlink($tmp);
    }
}

/** Drops cache files older than the TTL; runs on ~0.5% of the requests. */
function s60img_gc(): void
{
    if (random_int(1, 200) !== 1) {
        return;
    }
    $dir = s60img_cache_dir();
    $limit = time() - S60IMG_CACHE_TTL;
    foreach ((array) @scandir($dir) as $file) {
        if ($file === '.' || $file === '..') {
            continue;
        }
        $path = $dir . '/' . $file;
        if (is_file($path) && @filemtime($path) < $limit) {
            @unlink($path);
        }
    }
}

/** Small per-IP throttle so the endpoint does not become an open proxy. */
function s60img_rate_limit(): void
{
    $ip = isset($_SERVER['REMOTE_ADDR']) ? (string) $_SERVER['REMOTE_ADDR'] : '0.0.0.0';
    $file = s60img_cache_dir() . '/rate_' . sha1($ip);
    $now = time();
    $fp = @fopen($file, 'c+');
    if ($fp === false) {
        return;
    }
    if (!flock($fp, LOCK_EX)) {
        fclose($fp);
        return;
    }
    $raw = trim((string) stream_get_contents($fp));
    $start = 0;
    $count = 0;
    if ($raw !== '') {
        $parts = explode(':', $raw);
        $start = (int) ($parts[0] ?? 0);
        $count = (int) ($parts[1] ?? 0);
    }
    if ($now - $start >= S60IMG_RATE_WINDOW) {
        $start = $now;
        $count = 0;
    }
    $count++;
    ftruncate($fp, 0);
    rewind($fp);
    fwrite($fp, $start . ':' . $count);
    fflush($fp);
    flock($fp, LOCK_UN);
    fclose($fp);

    if ($count > S60IMG_RATE_LIMIT) {
        s60img_fail(429, 'Too many requests');
    }
}

function s60img_is_public_ip(string $ip): bool
{
    return filter_var(
        $ip,
        FILTER_VALIDATE_IP,
        FILTER_FLAG_NO_PRIV_RANGE | FILTER_FLAG_NO_RES_RANGE
    ) !== false;
}

/** Rejects loopback/private hosts to keep the proxy from reaching the LAN. */
function s60img_is_public_host(string $host): bool
{
    $host = strtolower(trim($host, '[]'));
    if ($host === '') {
        return false;
    }
    if (
        $host === 'localhost'
        || str_ends_with($host, '.localhost')
        || str_ends_with($host, '.local')
        || str_ends_with($host, '.internal')
    ) {
        return false;
    }
    if (filter_var($host, FILTER_VALIDATE_IP) !== false) {
        return s60img_is_public_ip($host);
    }

    $ips = @gethostbynamel($host);
    if (!is_array($ips) || $ips === []) {
        $ips = [];
        foreach ((array) @dns_get_record($host, DNS_AAAA) as $record) {
            if (!empty($record['ipv6'])) {
                $ips[] = (string) $record['ipv6'];
            }
        }
    }
    if ($ips === []) {
        return false;
    }
    foreach ($ips as $ip) {
        if (!s60img_is_public_ip($ip)) {
            return false;
        }
    }
    return true;
}

/** Returns the URL when it is a public http(s) URL, otherwise ''. */
function s60img_check_url(string $url): string
{
    $url = trim($url);
    if ($url === '' || strlen($url) > 2048) {
        return '';
    }
    $parts = parse_url($url);
    if (!is_array($parts) || empty($parts['scheme']) || empty($parts['host'])) {
        return '';
    }
    $scheme = strtolower((string) $parts['scheme']);
    if ($scheme !== 'http' && $scheme !== 'https') {
        return '';
    }
    if (!s60img_is_public_host((string) $parts['host'])) {
        return '';
    }
    return $url;
}

function s60img_common_curl_options(string $url): array
{
    return [
        CURLOPT_URL => $url,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_MAXREDIRS => 2,
        CURLOPT_CONNECTTIMEOUT => S60IMG_CONNECT_TIMEOUT,
        CURLOPT_TIMEOUT => S60IMG_TIMEOUT,
        CURLOPT_PROTOCOLS => CURLPROTO_HTTP | CURLPROTO_HTTPS,
        CURLOPT_REDIR_PROTOCOLS => CURLPROTO_HTTP | CURLPROTO_HTTPS,
        CURLOPT_SSL_VERIFYPEER => true,
        CURLOPT_SSL_VERIFYHOST => 2,
        CURLOPT_ENCODING => '',
        CURLOPT_USERAGENT => 'Mozilla/5.0 (Series60/3.2; Nokia) MIDPlay/s60img',
        CURLOPT_HTTPHEADER => ['Accept: image/*,*/*;q=0.8'],
    ];
}

/** Downloads a single image, aborting once the size cap is reached. */
function s60img_download(string $url): ?string
{
    $body = '';
    $tooBig = false;
    $ch = curl_init();
    if ($ch === false) {
        return null;
    }
    $options = s60img_common_curl_options($url);
    $options[CURLOPT_RETURNTRANSFER] = false;
    $options[CURLOPT_WRITEFUNCTION] = function ($handle, string $chunk) use (&$body, &$tooBig): int {
        if (strlen($body) + strlen($chunk) > S60IMG_MAX_BYTES) {
            $tooBig = true;
            return 0;
        }
        $body .= $chunk;
        return strlen($chunk);
    };
    curl_setopt_array($ch, $options);
    $ok = curl_exec($ch);
    $status = (int) curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
    curl_close($ch);

    if ($ok === false || $tooBig || $status < 200 || $status >= 300 || $body === '') {
        return null;
    }
    return $body;
}

/**
 * Downloads several URLs in parallel, reusing identical URLs.
 *
 * @param string[] $urls
 * @return array<string, string|null> map of the original URL to its body
 */
function s60img_download_many(array $urls): array
{
    $results = [];
    $multi = curl_multi_init();
    if ($multi === false) {
        foreach ($urls as $url) {
            $results[$url] = null;
        }
        return $results;
    }

    // Validate and deduplicate. Invalid URLs are resolved to null right away.
    $targets = [];
    foreach ($urls as $url) {
        if (!array_key_exists($url, $targets)) {
            $targets[$url] = s60img_check_url((string) $url);
            if ($targets[$url] === '') {
                $results[$url] = null;
            }
        }
    }

    $active = [];
    $startNext = function () use (&$targets, &$active, &$results, $multi): void {
        if (count($active) >= S60IMG_CONCURRENCY) {
            return;
        }
        foreach ($targets as $original => $target) {
            if (count($active) >= S60IMG_CONCURRENCY) {
                return;
            }
            if ($target === '' || isset($active[$original]) || array_key_exists($original, $results)) {
                continue;
            }
            $ch = curl_init();
            if ($ch === false) {
                $results[$original] = null;
                continue;
            }
            $options = s60img_common_curl_options($target);
            $options[CURLOPT_RETURNTRANSFER] = true;
            $options[CURLOPT_MAXFILESIZE] = S60IMG_MAX_BYTES;
            curl_setopt_array($ch, $options);
            curl_multi_add_handle($multi, $ch);
            $active[$original] = $ch;
        }
    };

    $startNext();
    $guard = 0;
    do {
        curl_multi_exec($multi, $running);
        while (($info = curl_multi_info_read($multi)) !== false) {
            $ch = $info['handle'];
            $original = null;
            foreach ($active as $key => $handle) {
                if ($handle === $ch) {
                    $original = $key;
                    break;
                }
            }
            if ($original === null) {
                continue;
            }
            $code = (int) curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
            $body = (string) curl_multi_getcontent($ch);
            $results[$original] =
                $info['result'] === CURLE_OK
                && $code >= 200
                && $code < 300
                && $body !== ''
                && strlen($body) <= S60IMG_MAX_BYTES
                    ? $body
                    : null;
            unset($active[$original]);
            curl_multi_remove_handle($multi, $ch);
            curl_close($ch);
            $startNext();
        }
        if ($running > 0) {
            curl_multi_select($multi, 0.5);
        }
        // Safety net: never spin forever if libcurl misbehaves.
        if (++$guard > 200) {
            break;
        }
    } while ($running > 0 || count($active) > 0);

    foreach ($active as $original => $ch) {
        curl_multi_remove_handle($multi, $ch);
        curl_close($ch);
        if (!array_key_exists($original, $results)) {
            $results[$original] = null;
        }
    }
    curl_multi_close($multi);

    foreach ($targets as $original => $target) {
        if (!array_key_exists($original, $results)) {
            $results[$original] = null;
        }
    }
    return $results;
}

/**
 * @return array{img: GdImage, w: int, h: int, type: int}|null
 */
function s60img_decode(string $body): ?array
{
    $info = @getimagesizefromstring($body);
    if (!is_array($info)) {
        return null;
    }
    $width = (int) $info[0];
    $height = (int) $info[1];
    $type = (int) $info[2];
    if (
        $width < 1
        || $height < 1
        || $width > S60IMG_MAX_SIDE
        || $height > S60IMG_MAX_SIDE
    ) {
        return null;
    }
    if (
        !in_array(
            $type,
            [IMAGETYPE_JPEG, IMAGETYPE_PNG, IMAGETYPE_GIF, IMAGETYPE_WEBP, IMAGETYPE_BMP],
            true
        )
    ) {
        return null;
    }
    $img = @imagecreatefromstring($body);
    if ($img === false) {
        return null;
    }
    return ['img' => $img, 'w' => $width, 'h' => $height, 'type' => $type];
}

/** Center-crops the source to the destination aspect ratio (cover). */
function s60img_cover(GdImage $src, int $sw, int $sh, int $dw, int $dh): GdImage
{
    $dst = imagecreatetruecolor($dw, $dh);
    imagealphablending($dst, false);
    imagesavealpha($dst, true);
    $transparent = imagecolorallocatealpha($dst, 0, 0, 0, 127);
    imagefilledrectangle($dst, 0, 0, $dw - 1, $dh - 1, $transparent);
    imagealphablending($dst, true);

    $srcRatio = $sw / $sh;
    $dstRatio = $dw / $dh;
    if ($srcRatio > $dstRatio) {
        $cropH = $sh;
        $cropW = max(1, (int) round($sh * $dstRatio));
    } else {
        $cropW = $sw;
        $cropH = max(1, (int) round($sw / $dstRatio));
    }
    $srcX = max(0, (int) floor(($sw - $cropW) / 2));
    $srcY = max(0, (int) floor(($sh - $cropH) / 2));

    imagecopyresampled($dst, $src, 0, 0, $srcX, $srcY, $dw, $dh, $cropW, $cropH);
    return $dst;
}

function s60img_has_alpha(int $type): bool
{
    return in_array($type, [IMAGETYPE_PNG, IMAGETYPE_GIF, IMAGETYPE_WEBP], true);
}

function s60img_clamp(int $value, int $min, int $max, int $fallback): int
{
    if ($value <= 0) {
        return $fallback;
    }
    return max($min, min($max, $value));
}

function s60img_serve(string $body, string $mime, int $ttl): void
{
    $etag = '"' . sha1($body) . '"';
    $ifNoneMatch = isset($_SERVER['HTTP_IF_NONE_MATCH'])
        ? trim((string) $_SERVER['HTTP_IF_NONE_MATCH'])
        : '';
    header('Content-Type: ' . $mime);
    header('Cache-Control: public, max-age=' . $ttl);
    header('ETag: ' . $etag);
    if ($ifNoneMatch !== '' && $ifNoneMatch === $etag) {
        http_response_code(304);
        return;
    }
    header('Content-Length: ' . strlen($body));
    echo $body;
}

/** Encodes, emits and (optionally) caches the result. */
function s60img_emit(GdImage $img, bool $alpha, string $cacheKey, int $ttl): void
{
    ob_start();
    if ($alpha) {
        imagesavealpha($img, true);
        imagepng($img, null, 9);
    } else {
        imagejpeg($img, null, 82);
    }
    $body = (string) ob_get_clean();
    $mime = $alpha ? 'image/png' : 'image/jpeg';
    if ($cacheKey !== '' && $ttl > 0) {
        s60img_cache_write($cacheKey, $alpha ? 'png' : 'jpg', $body);
    }
    s60img_serve($body, $mime, $ttl);
}
