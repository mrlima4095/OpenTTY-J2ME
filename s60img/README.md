# s60img

Servidor de imagens do MIDPlay. Ele existe porque o backend de imagens do
S60Tube (`/convert` e `/sprite`) depende de uma conta AWS que está suspensa e
responde `403 {"message":"This account is suspended"}`.

Os outros endpoints (`/search`, `/playlist`, `/tracks`, `/theme`, `/takumi`,
`/update`) continuam sendo usados direto do `music.s60tube.io.vn`.

## Rotas

### `GET /s60img/convert.php?url=<url>&w=<largura>&h=<altura>`

Baixa a imagem, faz **center-crop** (cover) para o tamanho pedido e responde
JPEG (fotos) ou PNG (origens com transparência).

- `w`/`h`: 16 a 600, padrão 96.
- Cache em disco por 7 dias (`/tmp/s60img-cache`).
- `400` URL inválida/privada, `404` origem indisponível, `415` não é imagem.

### `POST /s60img/sprite.php`

Corpo:

```json
{"urls":["https://...", "https://..."], "width":36, "height":36, "cols":4}
```

Responde **um PNG** com as imagens em grade (linha por linha, `cols` colunas).
Células que falharam ficam transparentes para o cliente continuar fatiando por
offset fixo. Máximo de 64 URLs; `width`/`height` de 16 a 600; `cols` de 1 a 16.

- Cache do PNG final por 6 horas, somente quando todas as origens carregaram.
- Download em paralelo (`curl_multi`, 6 conexões).

## Configuração no MIDPlay

`Configurações → Proxy de imagens`:

- `s60tube.io.vn` (padrão): usa `http://music.s60tube.io.vn/convert` e `/sprite`.
- `opentty.fun`: usa `http://opentty.fun/s60img/convert.php` e `/sprite.php`.

## Manutenção

- Não precisa de banco, fila nem cron. O cache é limpo sozinho (GC em ~0,5%
  das requisições apaga arquivos com mais de 7 dias).
- Limite de 4 MiB por imagem, timeouts de 4 s (conexão) e 8 s (total).
- Hosts privados/loopback são recusados (evita SSRF).
- Throttle de 240 requisições por minuto por IP.
- Se o servidor reiniciar, o cache em `/tmp` se perde: sem problema, ele é
  reconstruído sob demanda.
- Requer a extensão **GD** do PHP (instalada no `Dockerfile`).

## Teste rápido

```sh
curl -o /tmp/t.jpg 'http://opentty.fun/s60img/convert.php?url=https%3A%2F%2Fi.ytimg.com%2Fvi%2FdQw4w9WgXcQ%2Fmqdefault.jpg&w=36&h=36'

curl -X POST -H 'Content-Type: application/json' \
  --data '{"urls":["https://i.ytimg.com/vi/dQw4w9WgXcQ/mqdefault.jpg","https://i.ytimg.com/vi/9bZkp7q19f0/mqdefault.jpg"],"width":36,"height":36,"cols":4}' \
  -o /tmp/s.png http://opentty.fun/s60img/sprite.php
```
