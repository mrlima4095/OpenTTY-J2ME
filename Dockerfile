FROM php:8.3-fpm-alpine

RUN apk add --no-cache \
    nginx \
    python3 \
    supervisor \
    curl

WORKDIR /app

COPY . .

RUN mkdir -p /run/nginx /var/log/supervisor /var/log/nginx

COPY docker/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY docker/nginx.conf /etc/nginx/nginx.conf
COPY docker/php.ini /usr/local/etc/php/conf.d/opentty.ini

EXPOSE 80
EXPOSE 31522
EXPOSE 4096
EXPOSE 8080

CMD ["supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
