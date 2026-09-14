<?php
declare(strict_types=1);

$query = trim((string) ($_GET['q'] ?? ''));
if ($query === '' || strlen($query) > 100) {
    http_response_code(400);
    header('Content-Type: text/plain; charset=UTF-8');
    exit('Missing or invalid query');
}

$url = 'http://s60tube.io.vn/search?q=' . rawurlencode($query);
$curl = curl_init($url);
curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
curl_setopt($curl, CURLOPT_FOLLOWLOCATION, true);
curl_setopt($curl, CURLOPT_CONNECTTIMEOUT, 10);
curl_setopt($curl, CURLOPT_TIMEOUT, 20);
curl_setopt($curl, CURLOPT_USERAGENT, 'Mozilla/5.0 (Nokia; U; Series60/3.2)');
$body = curl_exec($curl);
$status = (int) curl_getinfo($curl, CURLINFO_RESPONSE_CODE);

if ($body === false || $status >= 400) {
    http_response_code(502);
    header('Content-Type: text/plain; charset=UTF-8');
    exit('S60Tube is unavailable');
}

curl_close($curl);
header('Content-Type: text/html; charset=UTF-8');
header('Cache-Control: no-store');
echo $body;
