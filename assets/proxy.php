<?php
$url = '';
if (strpos($queryString, 'url=') === 0) {
    $url = substr($queryString, 4);
} elseif (preg_match('/url=([^&]+)/', $queryString, $matches)) {
    $url = $matches[1];
}

// Decodificação múltipla e limpeza
$url = urldecode(urldecode($url));
$url = trim($url);
$url = filter_var($url, FILTER_SANITIZE_URL);

if (empty($url)) {
    http_response_code(400);
    echo "Error: No URL provided";
    exit;
}

if (!preg_match('#^https?://#i', $url)) { $url = 'https://' . $url; }

$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);

if (isset($_SERVER['HTTP_USER_AGENT'])) { curl_setopt($ch, CURLOPT_USERAGENT, $_SERVER['HTTP_USER_AGENT']); }

$response = curl_exec($ch);
$http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$content_type = curl_getinfo($ch, CURLINFO_CONTENT_TYPE);

if (curl_errno($ch)) {
    http_response_code(502);
    echo "" . curl_error($ch);
    echo "$url";
    curl_close($ch);
    exit;
}

curl_close($ch);


if ($content_type) {
    header("Content-Type: $content_type");
}

http_response_code($http_code);
echo $response;
