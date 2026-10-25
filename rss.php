<?php
/**
 * rss.php - RSS/Atom/JSON Feed reader endpoint for MIDPlay.
 *
 * The J2ME client never parses XML and never opens HTTPS directly. This
 * endpoint fetches the feed, converts it to a compact JSON document, strips
 * unsafe HTML, extracts a small summary and the first image, and caches the
 * result for a few minutes.
 *
 * Operations:
 *   ?op=discover&url=<site-or-feed>
 *   ?op=feed&url=<feed>&lang=pt
 *   ?op=article&url=<feed>&id=<item-id>&lang=pt
 */

@set_time_limit(25);
@ini_set('memory_limit', '160M');
@ini_set('display_errors', '0');
date_default_timezone_set('UTC');

const RSS_MAX_FETCH = 2097152;
const RSS_MAX_ITEMS = 60;
const RSS_MAX_SUMMARY = 700;
const RSS_MAX_CONTENT = 16000;
const RSS_MAX_REDIRECTS = 4;
const RSS_CACHE_TTL = 300;
const RSS_CACHE_MAX_AGE = 86400;

rss_main();

function rss_main() {
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store, max-age=0');
    header('X-Robots-Tag: noindex, nofollow');
    header('X-Content-Type-Options: nosniff');

    $op = strtolower(trim((string)($_GET['op'] ?? 'feed')));
    $input = trim((string)($_GET['url'] ?? ''));
    $id = trim((string)($_GET['id'] ?? ''));

    if ($input === '') {
        rss_fail('missing_url');
    }
    $url = rss_normalize_url($input);
    if ($url === null) {
        rss_fail('invalid_url');
    }

    if ($op === 'discover') {
        $feed = rss_load_feed($url, false, '');
        if ($feed === null) {
            rss_fail('not_a_feed');
        }
        rss_send(array('ok' => 1, 'f' => $feed['feed']));
    }

    if ($op === 'article' || $op === 'item') {
        if ($id === '') {
            rss_fail('missing_id');
        }
        $feed = rss_load_feed($url, true, $id);
        if ($feed === null) {
            rss_fail('not_a_feed');
        }
        $article = null;
        foreach ($feed['items'] as $item) {
            if (($item['id'] ?? '') === $id) {
                $article = $item;
                break;
            }
        }
        if ($article === null) {
            rss_fail('not_found');
        }
        rss_send(array('ok' => 1, 'i' => $article['article']));
    }

    $feed = rss_load_feed($url, false, '');
    if ($feed === null) {
        rss_fail('not_a_feed');
    }
    $items = array();
    foreach ($feed['items'] as $item) {
        $items[] = $item['row'];
    }
    rss_send(array('ok' => 1, 'f' => $feed['feed'], 'it' => $items));
}

/* ------------------------------------------------------------------------- */
/* Feed loading                                                              */
/* ------------------------------------------------------------------------- */

function rss_load_feed($url, $includeContent, $wantId) {
    $fetched = rss_fetch_cached($url);
    if (!empty($fetched['error'])) {
        return null;
    }

    $body = $fetched['body'];
    $type = strtolower((string)$fetched['type']);
    $final = $fetched['final'];

    // The user may have pasted a site URL instead of the feed URL.
    if (rss_is_html($body, $type)) {
        $candidate = rss_discover_in_html($body, $final);
        if ($candidate === null) {
            $candidate = rss_try_common_paths($final);
        }
        if ($candidate === null) {
            return null;
        }
        $fetched = rss_fetch_cached($candidate);
        if (!empty($fetched['error'])) {
            return null;
        }
        $body = $fetched['body'];
        $type = strtolower((string)$fetched['type']);
        $final = $fetched['final'];
    }

    $body = rss_to_utf8($body, $fetched['type']);

    if (rss_is_json_feed($body, $type)) {
        return rss_parse_json_feed($body, $final, $includeContent, $wantId);
    }

    return rss_parse_xml_feed($body, $final, $includeContent, $wantId);
}

function rss_is_html($body, $type) {
    $head = ltrim(substr($body, 0, 512));
    if (strpos($type, 'html') !== false) {
        return true;
    }
    return stripos($head, '<!doctype html') === 0
        || stripos($head, '<html') === 0
        || stripos($head, '<head') === 0;
}

function rss_is_json_feed($body, $type) {
    if (strpos($type, 'json') !== false) {
        return true;
    }
    $head = ltrim(substr($body, 0, 256));
    return isset($head[0]) && $head[0] === '{';
}

function rss_discover_in_html($html, $base) {
    $dom = new DOMDocument();
    libxml_use_internal_errors(true);
    $ok = $dom->loadHTML(
        '<?xml encoding="UTF-8">' . substr($html, 0, 500000),
        LIBXML_NOERROR | LIBXML_NOWARNING | LIBXML_COMPACT | LIBXML_NONET
    );
    libxml_clear_errors();
    if (!$ok) {
        return null;
    }
    $fallback = null;
    foreach ($dom->getElementsByTagName('link') as $link) {
        $rel = strtolower(trim($link->getAttribute('rel')));
        if ($rel !== 'alternate') {
            continue;
        }
        $type = strtolower(trim($link->getAttribute('type')));
        if (strpos($type, 'rss') === false
            && strpos($type, 'atom') === false
            && strpos($type, 'feed+json') === false
            && strpos($type, 'rdf') === false) {
            continue;
        }
        $href = trim($link->getAttribute('href'));
        if ($href === '') {
            continue;
        }
        $resolved = rss_resolve_url($base, $href);
        if ($resolved === null) {
            continue;
        }
        if (strpos($type, 'rss') !== false || strpos($type, 'rdf') !== false) {
            return $resolved;
        }
        if ($fallback === null) {
            $fallback = $resolved;
        }
    }
    return $fallback;
}

function rss_try_common_paths($base) {
    $paths = array('/feed/', '/rss/', '/feed.xml', '/rss.xml');
    foreach ($paths as $path) {
        $candidate = rss_resolve_url($base, $path);
        if ($candidate === null) {
            continue;
        }
        $fetched = rss_fetch($candidate, 5);
        if (!empty($fetched['error'])) {
            continue;
        }
        $head = ltrim(substr($fetched['body'], 0, 512));
        if (stripos($head, '<?xml') === 0
            || stripos($head, '<rss') !== false
            || stripos($head, '<feed') !== false
            || stripos($head, '<rdf') !== false) {
            return $candidate;
        }
    }
    return null;
}

function rss_parse_xml_feed($body, $final, $includeContent, $wantId) {
    $body = preg_replace('/^\s*<\?xml[^>]*\?>/i', '', $body);
    $body = str_replace(
        array('&nbsp;', '&copy;', '&reg;', '&trade;', '&hellip;', '&mdash;',
              '&ndash;', '&rsquo;', '&lsquo;', '&ldquo;', '&rdquo;'),
        array('&#160;', '&#169;', '&#174;', '&#8482;', '&#8230;', '&#8212;',
              '&#8211;', '&#8217;', '&#8216;', '&#8220;', '&#8221;'),
        $body
    );

    $dom = new DOMDocument();
    libxml_use_internal_errors(true);
    $ok = $dom->loadXML(
        $body,
        LIBXML_NOERROR | LIBXML_NOWARNING | LIBXML_COMPACT | LIBXML_NONET | LIBXML_NOCDATA
    );
    libxml_clear_errors();
    if (!$ok || $dom->documentElement === null) {
        return null;
    }

    $root = $dom->documentElement;
    $rootName = rss_local_name($root);
    if ($rootName === 'rss') {
        $channel = rss_child($root, array('channel'));
        if ($channel === null) {
            return null;
        }
        $feed = rss_feed_meta_rss($channel, $final);
        $nodes = rss_children($channel, array('item'));
        return rss_build_feed($feed, $nodes, $final, false, $includeContent, $wantId);
    }
    if ($rootName === 'feed') {
        $feed = rss_feed_meta_atom($root, $final);
        $nodes = rss_children($root, array('entry'));
        return rss_build_feed($feed, $nodes, $final, true, $includeContent, $wantId);
    }
    if ($rootName === 'rdf' || $rootName === 'rdf:rdf') {
        $channel = rss_child($root, array('channel'));
        $feed = $channel !== null ? rss_feed_meta_rss($channel, $final) : rss_feed_meta_atom($root, $final);
        $nodes = rss_children($root, array('item'));
        return rss_build_feed($feed, $nodes, $final, false, $includeContent, $wantId);
    }

    // Some feeds are a bare sequence of item/entry elements.
    $items = rss_children($root, array('item', 'entry'));
    if (count($items) > 0) {
        $feed = array(
            't' => rss_clean($root->textContent),
            'u' => $final,
            's' => rss_origin($final),
            'i' => '',
            'd' => '',
        );
        return rss_build_feed($feed, $items, $final, false, $includeContent, $wantId);
    }
    return null;
}

function rss_feed_meta_rss(DOMNode $channel, $final) {
    $title = rss_clean(rss_child_text($channel, array('title')));
    $site = rss_child_text($channel, array('link'));
    if ($site === '') {
        $site = rss_origin($final);
    }
    $description = rss_clean(rss_child_text($channel, array('description', 'subtitle')));
    $image = '';
    $imageNode = rss_child($channel, array('image'));
    if ($imageNode !== null) {
        $image = rss_child_text($imageNode, array('url'));
    }
    if ($image === '') {
        $itunes = rss_child($channel, array('image'));
        if ($itunes !== null) {
            $image = $itunes->getAttribute('href');
        }
    }
    if ($image !== '') {
        $image = rss_resolve_url($final, $image);
    }
    return array(
        't' => $title !== '' ? $title : rss_host($final),
        'u' => $final,
        's' => $site,
        'i' => $image === null ? '' : $image,
        'd' => rss_truncate($description, 240),
    );
}

function rss_feed_meta_atom(DOMNode $feedNode, $final) {
    $title = rss_clean(rss_child_text($feedNode, array('title')));
    $site = rss_atom_link($feedNode);
    if ($site === '') {
        $site = rss_origin($final);
    }
    $description = rss_clean(rss_child_text($feedNode, array('subtitle')));
    $image = rss_child_text($feedNode, array('logo', 'icon'));
    if ($image === '') {
        $imageNode = rss_child($feedNode, array('image'));
        if ($imageNode !== null) {
            $image = $imageNode->getAttribute('href');
        }
    }
    if ($image !== '') {
        $image = rss_resolve_url($final, $image);
    }
    return array(
        't' => $title !== '' ? $title : rss_host($final),
        'u' => $final,
        's' => $site,
        'i' => $image === null ? '' : $image,
        'd' => rss_truncate($description, 240),
    );
}

function rss_build_feed(array $feed, array $nodes, $final, $isAtom, $includeContent, $wantId) {
    $items = array();
    $count = 0;
    foreach ($nodes as $node) {
        if ($count >= RSS_MAX_ITEMS) {
            break;
        }
        $link = $isAtom ? rss_atom_link($node) : rss_child_text($node, array('link'));
        if ($link === '' && !$isAtom) {
            $guid = rss_child($node, array('guid'));
            if ($guid !== null && strtolower($guid->getAttribute('isPermaLink')) !== 'false') {
                $link = trim($guid->textContent);
            }
        }
        $link = rss_resolve_url($final, $link);
        if ($link === null) {
            $link = '';
        }
        $title = rss_clean(rss_child_text($node, array('title')));
        if ($title === '') {
            $title = $link !== '' ? $link : '&nbsp;';
        }
        $id = substr(sha1($link !== '' ? $link : $title), 0, 16);
        $timestamp = rss_item_time($node);
        $author = rss_item_author($node);
        $rawContent = rss_item_content($node);

        $image = rss_item_image($node, $rawContent, $link !== '' ? $link : $final);
        $summary = rss_html_text($rawContent, RSS_MAX_SUMMARY, $ignored);

        if ($includeContent && ($wantId === '' || $wantId === $id)) {
            $content = rss_html_text($rawContent, RSS_MAX_CONTENT, $ignored);
            if ($content === '') {
                $content = $summary;
            }
            $article = array(
                't' => rss_entity_decode($title),
                'l' => $link,
                'a' => $author,
                'ts' => $timestamp,
                'c' => $content,
                'm' => $image,
            );
        } else {
            $article = null;
        }

        $items[] = array(
            'id' => $id,
            'row' => array(
                $id,
                rss_entity_decode($title),
                $link,
                $timestamp,
                $author,
                $summary,
                $image,
            ),
            'article' => $article,
        );
        $count++;
    }

    return array('feed' => $feed, 'items' => $items);
}

function rss_parse_json_feed($body, $final, $includeContent, $wantId) {
    $data = json_decode($body, true);
    if (!is_array($data)) {
        return null;
    }
    $title = rss_clean((string)($data['title'] ?? ''));
    $site = (string)($data['home_page_url'] ?? rss_origin($final));
    $description = rss_clean((string)($data['description'] ?? ''));
    $icon = (string)($data['icon'] ?? ($data['favicon'] ?? ''));
    if ($icon !== '') {
        $resolved = rss_resolve_url($final, $icon);
        $icon = $resolved === null ? '' : $resolved;
    }
    $feed = array(
        't' => $title !== '' ? $title : rss_host($final),
        'u' => $final,
        's' => $site,
        'i' => $icon,
        'd' => rss_truncate($description, 240),
    );

    $items = array();
    $entries = isset($data['items']) && is_array($data['items']) ? $data['items'] : array();
    $count = 0;
    foreach ($entries as $entry) {
        if ($count >= RSS_MAX_ITEMS) {
            break;
        }
        if (!is_array($entry)) {
            continue;
        }
        $link = (string)($entry['url'] ?? ($entry['external_url'] ?? ''));
        $linkResolved = rss_resolve_url($final, $link);
        if ($linkResolved !== null) {
            $link = $linkResolved;
        }
        $itemTitle = rss_clean((string)($entry['title'] ?? ''));
        if ($itemTitle === '') {
            $itemTitle = $link;
        }
        $id = substr(sha1($link !== '' ? $link : $itemTitle), 0, 16);
        $timestamp = 0;
        $date = (string)($entry['date_published'] ?? ($entry['date_modified'] ?? ''));
        if ($date !== '') {
            $parsed = strtotime($date);
            if ($parsed !== false) {
                $timestamp = $parsed;
            }
        }
        $author = '';
        if (isset($entry['authors']) && is_array($entry['authors']) && count($entry['authors']) > 0) {
            $author = rss_clean((string)($entry['authors'][0]['name'] ?? ''));
        } elseif (isset($entry['author'])) {
            if (is_array($entry['author'])) {
                $author = rss_clean((string)($entry['author']['name'] ?? ''));
            } else {
                $author = rss_clean((string)$entry['author']);
            }
        }
        $rawContent = (string)($entry['content_html'] ?? ($entry['content_text'] ?? ($entry['summary'] ?? '')));
        $image = (string)($entry['image'] ?? '');
        if ($image === '') {
            $image = rss_first_image($rawContent);
        }
        if ($image !== '') {
            $resolved = rss_resolve_url($link !== '' ? $link : $final, $image);
            $image = $resolved === null ? '' : $resolved;
        }
        $summary = rss_html_text($rawContent, RSS_MAX_SUMMARY, $ignored);
        $article = null;
        if ($includeContent && ($wantId === '' || $wantId === $id)) {
            $content = rss_html_text($rawContent, RSS_MAX_CONTENT, $ignored);
            if ($content === '') {
                $content = $summary;
            }
            $article = array(
                't' => rss_entity_decode($itemTitle),
                'l' => $link,
                'a' => $author,
                'ts' => $timestamp,
                'c' => $content,
                'm' => $image,
            );
        }
        $items[] = array(
            'id' => $id,
            'row' => array(
                $id,
                rss_entity_decode($itemTitle),
                $link,
                $timestamp,
                $author,
                $summary,
                $image,
            ),
            'article' => $article,
        );
        $count++;
    }
    return array('feed' => $feed, 'items' => $items);
}

/* ------------------------------------------------------------------------- */
/* Item helpers                                                              */
/* ------------------------------------------------------------------------- */

function rss_item_time(DOMNode $node) {
    $value = rss_child_text($node, array('pubdate', 'published', 'updated', 'date', 'created', 'modified'));
    if ($value === '') {
        return 0;
    }
    $parsed = strtotime($value);
    return $parsed === false ? 0 : $parsed;
}

function rss_item_author(DOMNode $node) {
    $value = rss_child_text($node, array('creator', 'author'));
    if ($value === '') {
        return '';
    }
    $value = rss_clean($value);
    return rss_truncate($value, 80);
}

function rss_item_content(DOMNode $node) {
    $best = '';
    foreach (rss_children($node, array('encoded', 'content', 'summary', 'description')) as $child) {
        $value = rss_element_html($child);
        if (strlen($value) > strlen($best)) {
            $best = $value;
        }
    }
    return $best;
}

function rss_item_image(DOMNode $node, $rawContent, $base) {
    foreach (rss_children($node, array('enclosure', 'content', 'thumbnail')) as $child) {
        $url = trim($child->getAttribute('url'));
        $type = strtolower($child->getAttribute('type'));
        $medium = strtolower($child->getAttribute('medium'));
        if ($url === '') {
            continue;
        }
        $local = rss_local_name($child);
        if ($local === 'enclosure' && $type !== '' && strpos($type, 'image') === false) {
            continue;
        }
        if ($local === 'content' && $medium !== '' && $medium !== 'image' && strpos($type, 'image') === false) {
            continue;
        }
        $resolved = rss_resolve_url($base, $url);
        if ($resolved !== null) {
            return $resolved;
        }
    }
    $image = rss_first_image($rawContent);
    if ($image === '') {
        return '';
    }
    $resolved = rss_resolve_url($base, $image);
    return $resolved === null ? '' : $resolved;
}

function rss_first_image($html) {
    if ($html === '' || stripos($html, '<img') === false) {
        return '';
    }
    if (preg_match('/<img[^>]+(?:data-src|data-original|src)\s*=\s*["\']([^"\']+)["\']/i', $html, $m)) {
        $url = trim(html_entity_decode($m[1], ENT_QUOTES | ENT_HTML5, 'UTF-8'));
        if ($url !== '' && stripos($url, 'data:') !== 0 && stripos($url, 'svg') === false) {
            return $url;
        }
    }
    return '';
}

function rss_html_text($html, $limit, &$image) {
    $image = '';
    if ($html === null || $html === '') {
        return '';
    }
    $image = rss_first_image($html);
    $text = preg_replace('/<\s*(script|style|noscript|iframe|svg|math|template)[^>]*>.*?<\s*\/\s*\1\s*>/is', ' ', $html);
    if ($text === null) {
        $text = $html;
    }
    $text = preg_replace('/<\s*br\s*\/?>/i', "\n", $text);
    $text = preg_replace('/<\s*\/(p|div|section|article|header|footer|li|tr|h[1-6]|blockquote|pre)\s*>/i', "\n", $text);
    $text = preg_replace('/<\s*li[^>]*>/i', "\n* ", $text);
    $text = strip_tags($text);
    $text = rss_entity_decode($text);
    $text = str_replace("\r", '', $text);
    $text = preg_replace('/[ \t\x{00A0}]+/u', ' ', $text);
    if ($text === null) {
        $text = preg_replace('/[ \t]+/', ' ', $text);
    }
    $text = preg_replace('/ *\n */', "\n", $text);
    $text = preg_replace('/\n{3,}/', "\n\n", $text);
    $text = trim($text);
    return rss_truncate($text, $limit);
}

function rss_element_html(DOMNode $element) {
    $hasElement = false;
    foreach ($element->childNodes as $child) {
        if ($child->nodeType === XML_ELEMENT_NODE) {
            $hasElement = true;
            break;
        }
    }
    if (!$hasElement) {
        return trim($element->textContent);
    }
    $out = '';
    foreach ($element->childNodes as $child) {
        $out .= $element->ownerDocument->saveXML($child);
    }
    return trim($out);
}

function rss_atom_link(DOMNode $node) {
    $fallback = '';
    foreach ($node->childNodes as $child) {
        if ($child->nodeType !== XML_ELEMENT_NODE || rss_local_name($child) !== 'link') {
            continue;
        }
        $href = trim($child->getAttribute('href'));
        if ($href === '') {
            continue;
        }
        $rel = strtolower(trim($child->getAttribute('rel')));
        if ($rel === '' || $rel === 'alternate') {
            return $href;
        }
        if ($fallback === '') {
            $fallback = $href;
        }
    }
    return $fallback;
}

/* ------------------------------------------------------------------------- */
/* Fetching and caching                                                      */
/* ------------------------------------------------------------------------- */

function rss_fetch_cached($url) {
    $cached = rss_cache_read($url);
    if ($cached !== null) {
        return $cached;
    }
    $fetched = rss_fetch($url, 12);
    if (empty($fetched['error'])) {
        rss_cache_write($url, $fetched);
    }
    return $fetched;
}

function rss_fetch($url, $timeout) {
    $current = $url;
    $hops = 0;
    while (true) {
        $parts = @parse_url($current);
        if (!$parts || empty($parts['scheme']) || empty($parts['host'])) {
            return array('error' => 'invalid_url');
        }
        $scheme = strtolower($parts['scheme']);
        if ($scheme !== 'http' && $scheme !== 'https') {
            return array('error' => 'invalid_url');
        }
        if (!rss_host_allowed($parts['host'])) {
            return array('error' => 'blocked');
        }

        $headers = array();
        $ch = curl_init();
        curl_setopt_array($ch, array(
            CURLOPT_URL => $current,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => false,
            CURLOPT_MAXREDIRS => 0,
            CURLOPT_CONNECTTIMEOUT => min(8, $timeout),
            CURLOPT_TIMEOUT => $timeout,
            CURLOPT_USERAGENT => 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 '
                . '(KHTML, like Gecko) Chrome/122.0 Mobile Safari/537.36 MIDPlayRSS/1.0',
            CURLOPT_HTTPHEADER => array(
                'Accept: application/rss+xml,application/atom+xml,application/xml,'
                    . 'text/xml,application/feed+json,application/json,text/html;q=0.8,*/*;q=0.5',
                'Accept-Language: pt-BR,pt;q=0.9,en;q=0.7',
                'Cache-Control: no-cache',
            ),
            CURLOPT_ENCODING => '',
            CURLOPT_SSL_VERIFYPEER => true,
            CURLOPT_SSL_VERIFYHOST => 2,
            CURLOPT_PROTOCOLS => CURLPROTO_HTTP | CURLPROTO_HTTPS,
            CURLOPT_REDIR_PROTOCOLS => CURLPROTO_HTTP | CURLPROTO_HTTPS,
            CURLOPT_HEADERFUNCTION => function ($ch, $line) use (&$headers) {
                $pos = strpos($line, ':');
                if ($pos !== false) {
                    $name = strtolower(trim(substr($line, 0, $pos)));
                    $headers[$name] = trim(substr($line, $pos + 1));
                }
                return strlen($line);
            },
        ));
        $body = curl_exec($ch);
        $code = (int)curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
        $type = (string)curl_getinfo($ch, CURLINFO_CONTENT_TYPE);
        $err = curl_error($ch);
        curl_close($ch);

        if ($err !== '') {
            return array('error' => 'fetch_failed');
        }
        if (is_string($body) && strlen($body) > RSS_MAX_FETCH) {
            return array('error' => 'too_large');
        }

        if (in_array($code, array(301, 302, 303, 307, 308), true)) {
            if ($hops >= RSS_MAX_REDIRECTS) {
                return array('error' => 'too_many_redirects');
            }
            $location = $headers['location'] ?? '';
            if ($location === '') {
                return array('error' => 'fetch_failed');
            }
            $next = rss_resolve_url($current, $location);
            if ($next === null) {
                return array('error' => 'invalid_url');
            }
            $current = $next;
            $hops++;
            continue;
        }

        if ($code < 200 || $code >= 300) {
            return array('error' => 'http_' . $code);
        }

        return array(
            'body' => is_string($body) ? $body : '',
            'type' => $type,
            'final' => $current,
            'ts' => time(),
        );
    }
}

function rss_cache_read($url) {
    $path = rss_cache_path($url);
    if (!is_file($path)) {
        return null;
    }
    if (time() - @filemtime($path) > RSS_CACHE_TTL) {
        return null;
    }
    $raw = @file_get_contents($path);
    if ($raw === false) {
        return null;
    }
    $data = @unserialize($raw);
    return is_array($data) ? $data : null;
}

function rss_cache_write($url, array $data) {
    $path = rss_cache_path($url);
    if (!is_dir(dirname($path))) {
        return;
    }
    $raw = serialize($data);
    if (strlen($raw) > RSS_MAX_FETCH + 8192) {
        return;
    }
    @file_put_contents($path, $raw, LOCK_EX);
    if (mt_rand(1, 150) === 1) {
        rss_cache_gc();
    }
}

function rss_cache_path($url) {
    return rss_cache_dir() . '/' . sha1($url) . '.cache';
}

function rss_cache_dir() {
    $dir = rtrim(sys_get_temp_dir(), '/') . '/midplay-rss';
    if (!is_dir($dir)) {
        @mkdir($dir, 0775, true);
    }
    return $dir;
}

function rss_cache_gc() {
    $dir = rss_cache_dir();
    $files = @glob($dir . '/*.cache');
    if (!is_array($files)) {
        return;
    }
    $now = time();
    foreach ($files as $file) {
        if ($now - @filemtime($file) > RSS_CACHE_MAX_AGE) {
            @unlink($file);
        }
    }
}

/* ------------------------------------------------------------------------- */
/* URL, encoding and text helpers                                            */
/* ------------------------------------------------------------------------- */

function rss_normalize_url($raw) {
    $raw = trim($raw);
    if ($raw === '') {
        return null;
    }
    if (!preg_match('#^[a-z][a-z0-9+.-]*://#i', $raw)) {
        $raw = 'https://' . ltrim($raw, '/');
    }
    $parts = @parse_url($raw);
    if (!$parts || empty($parts['scheme']) || empty($parts['host'])) {
        return null;
    }
    $scheme = strtolower($parts['scheme']);
    if ($scheme !== 'http' && $scheme !== 'https') {
        return null;
    }
    return $raw;
}

function rss_host_allowed($host) {
    $host = strtolower(trim($host, " \t\n\r\0\x0B[]"));
    if ($host === '' || $host === 'localhost' || $host === '0.0.0.0') {
        return false;
    }
    foreach (array('.local', '.internal', '.lan', '.home', '.localdomain') as $suffix) {
        if (substr($host, -strlen($suffix)) === $suffix) {
            return false;
        }
    }
    $ips = array();
    if (filter_var($host, FILTER_VALIDATE_IP)) {
        $ips[] = $host;
    } else {
        $v4 = @gethostbynamel($host);
        if (is_array($v4)) {
            $ips = array_merge($ips, $v4);
        }
        if (function_exists('dns_get_record')) {
            $v6 = @dns_get_record($host, DNS_AAAA);
            if (is_array($v6)) {
                foreach ($v6 as $record) {
                    if (!empty($record['ipv6'])) {
                        $ips[] = $record['ipv6'];
                    }
                }
            }
        }
    }
    if (count($ips) === 0) {
        return false;
    }
    foreach ($ips as $ip) {
        if (!filter_var($ip, FILTER_VALIDATE_IP, FILTER_FLAG_NO_PRIV_RANGE | FILTER_FLAG_NO_RES_RANGE)) {
            return false;
        }
    }
    return true;
}

function rss_to_utf8($body, $contentType) {
    $charset = null;
    if (preg_match('/charset\s*=\s*["\']?([a-zA-Z0-9._-]+)/i', (string)$contentType, $m)) {
        $charset = $m[1];
    }
    if ($charset === null) {
        $head = substr($body, 0, 8192);
        if (preg_match('/<\?xml[^>]+encoding\s*=\s*["\']([^"\']+)/i', $head, $m)) {
            $charset = $m[1];
        } elseif (preg_match('/<meta[^>]+charset\s*=\s*["\']?([a-zA-Z0-9._-]+)/i', $head, $m)) {
            $charset = $m[1];
        }
    }
    if ($charset !== null) {
        $upper = strtoupper($charset);
        if ($upper !== 'UTF-8' && $upper !== 'UTF8') {
            $converted = @mb_convert_encoding($body, 'UTF-8', $charset);
            if (is_string($converted)) {
                $body = $converted;
            }
        }
    }
    if (!mb_check_encoding($body, 'UTF-8')) {
        $body = @mb_convert_encoding($body, 'UTF-8', 'UTF-8');
    }
    return preg_replace('/^\xEF\xBB\xBF/', '', $body);
}

function rss_resolve_url($base, $relative) {
    $relative = trim((string)$relative);
    if ($relative === '') {
        return null;
    }
    if (preg_match('#^[a-z][a-z0-9+.-]*:#i', $relative)) {
        return preg_match('#^https?://#i', $relative) ? $relative : null;
    }
    $parts = @parse_url($base);
    if (!$parts || empty($parts['scheme']) || empty($parts['host'])) {
        return null;
    }
    $scheme = strtolower($parts['scheme']);
    if ($scheme !== 'http' && $scheme !== 'https') {
        return null;
    }
    $authority = $parts['host'];
    if (!empty($parts['port'])) {
        $authority .= ':' . $parts['port'];
    }
    if (substr($relative, 0, 2) === '//') {
        return $scheme . ':' . $relative;
    }
    if ($relative[0] === '#') {
        return null;
    }
    if ($relative[0] === '?') {
        return $scheme . '://' . $authority . ($parts['path'] ?? '/') . $relative;
    }
    if ($relative[0] === '/') {
        $path = $relative;
    } else {
        $basePath = $parts['path'] ?? '/';
        $directory = substr($basePath, 0, strrpos($basePath, '/') + 1);
        if ($directory === '') {
            $directory = '/';
        }
        $path = $directory . $relative;
    }
    return $scheme . '://' . $authority . rss_normalize_path($path);
}

function rss_normalize_path($path) {
    $suffix = '';
    $cut = strcspn($path, '?#');
    if ($cut < strlen($path)) {
        $suffix = substr($path, $cut);
        $path = substr($path, 0, $cut);
    }
    $segments = explode('/', $path);
    $out = array();
    foreach ($segments as $segment) {
        if ($segment === '' || $segment === '.') {
            continue;
        }
        if ($segment === '..') {
            array_pop($out);
            continue;
        }
        $out[] = $segment;
    }
    $normalized = '/' . implode('/', $out);
    if (substr($path, -1) === '/' && substr($normalized, -1) !== '/') {
        $normalized .= '/';
    }
    return $normalized . $suffix;
}

function rss_origin($url) {
    $parts = @parse_url($url);
    if (!$parts || empty($parts['scheme']) || empty($parts['host'])) {
        return '';
    }
    return strtolower($parts['scheme']) . '://' . $parts['host'];
}

function rss_host($url) {
    $parts = @parse_url($url);
    return $parts && !empty($parts['host']) ? strtolower($parts['host']) : 'RSS';
}

function rss_local_name(DOMNode $node) {
    return strtolower($node->localName !== null ? $node->localName : $node->nodeName);
}

function rss_child(DOMNode $node, array $names) {
    foreach ($node->childNodes as $child) {
        if ($child->nodeType === XML_ELEMENT_NODE && in_array(rss_local_name($child), $names, true)) {
            return $child;
        }
    }
    return null;
}

function rss_children(DOMNode $node, array $names) {
    $out = array();
    foreach ($node->childNodes as $child) {
        if ($child->nodeType === XML_ELEMENT_NODE && in_array(rss_local_name($child), $names, true)) {
            $out[] = $child;
        }
    }
    return $out;
}

function rss_child_text(DOMNode $node, array $names) {
    $child = rss_child($node, $names);
    return $child === null ? '' : trim($child->textContent);
}

function rss_clean($text) {
    $text = rss_entity_decode((string)$text);
    $text = preg_replace('/[\s\x{00A0}]+/u', ' ', $text);
    if ($text === null) {
        $text = preg_replace('/\s+/', ' ', (string)$text);
    }
    return trim($text);
}

function rss_entity_decode($text) {
    return html_entity_decode((string)$text, ENT_QUOTES | ENT_HTML5, 'UTF-8');
}

function rss_truncate($text, $max) {
    $text = (string)$text;
    if (strlen($text) <= $max) {
        return $text;
    }
    $cut = substr($text, 0, $max);
    $space = strrpos($cut, ' ');
    if ($space !== false && $space > $max - 40) {
        $cut = substr($cut, 0, $space);
    }
    return rtrim($cut) . '...';
}

function rss_fail($code) {
    rss_send(array('ok' => 0, 'e' => $code));
}

function rss_send(array $data) {
    echo json_encode(
        $data,
        JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_INVALID_UTF8_SUBSTITUTE
    );
    exit;
}
