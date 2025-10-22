<?php
// Obter a string de consulta
$query_string = $_SERVER['QUERY_STRING'] ?? '';

// Inicializar a URL
$url = '';

// Verificar diferentes formatos de parâmetro
if (strpos($query_string, 'url=') === 0) {
    // Formato: url=encoded_url
    $url = substr($query_string, 4);
} elseif (preg_match('/^[^=]+$/', $query_string)) {
    // Formato: encoded_url (sem parâmetro)
    $url = $query_string;
} else {
    // Tentar extrair de outros formatos de parâmetro
    parse_str($query_string, $params);
    $url = $params['url'] ?? '';
}

// Decodificar a URL
$url = urldecode($url);

// Validações de segurança
if (empty($url)) {
    http_response_code(400);
    echo "Error: No URL provided";
    exit;
}

// Validar formato da URL
if (!preg_match('#^https?://#i', $url)) {
    $url = 'https://' . $url;
}

// Validar se é uma URL válida
if (!filter_var($url, FILTER_VALIDATE_URL)) {
    http_response_code(400);
    echo "Error: Invalid URL provided";
    exit;
}

// Restrições de segurança - evitar SSRF
$disallowed_hosts = ['localhost', '127.0.0.1', '0.0.0.0', '::1', '192.168.', '10.', '172.16.', '169.254.'];
$host = parse_url($url, PHP_URL_HOST);

foreach ($disallowed_hosts as $disallowed) {
    if (stripos($host, $disallowed) !== false) {
        http_response_code(403);
        echo "Error: Access to internal resources is not allowed";
        exit;
    }
}

// Configurar cURL
$ch = curl_init();
curl_setopt_array($ch, [
    CURLOPT_URL => $url,
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_FOLLOWLOCATION => true,
    CURLOPT_TIMEOUT => 10,
]);

// Executar a requisição
$response = curl_exec($ch);
$http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$content_type = curl_getinfo($ch, CURLINFO_CONTENT_TYPE);

// Verificar erros
if (curl_errno($ch)) {
    http_response_code(502);
    echo "cURL Error: " . curl_error($ch);
    curl_close($ch);
    exit;
}

curl_close($ch);

// Definir headers de resposta
if ($content_type) {
    header("Content-Type: $content_type");
}

http_response_code($http_code);
echo $response;