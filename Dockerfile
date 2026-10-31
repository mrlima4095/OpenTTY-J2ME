FROM php:8.3-fpm-alpine

RUN apk add --no-cache \
    nginx \
    python3 \
    py3-pip \
    supervisor \
    git \
    curl

# GD powers s60img (thumbnail resize + sprite sheets).
RUN apk add --no-cache freetype libjpeg-turbo libpng \
    && apk add --no-cache --virtual .gd-build \
        $PHPIZE_DEPS freetype-dev libjpeg-turbo-dev libpng-dev \
    && docker-php-ext-configure gd --with-freetype --with-jpeg \
    && docker-php-ext-install -j"$(nproc)" gd \
    && apk del .gd-build

WORKDIR /app

COPY . .

# pproxy is a git submodule; if Coolify fetched the repo without --recursive,
# /app/pproxy would be empty, so fall back to a clone to build a usable image.
RUN if [ ! -f /app/pproxy/app.py ]; then \
        git clone --depth 1 https://github.com/mrlima4095/pproxy.git /app/pproxy; \
    fi

RUN mkdir -p /run/nginx /var/log/supervisor /var/log/nginx \
    && pip3 install --no-cache-dir --break-system-packages -r /app/pproxy/requirements.txt flask_cors requests

COPY docker/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY docker/nginx.conf /etc/nginx/nginx.conf
COPY docker/php.ini /usr/local/etc/php/conf.d/opentty.ini

EXPOSE 80
EXPOSE 31522
EXPOSE 4096
EXPOSE 10141

CMD ["supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
