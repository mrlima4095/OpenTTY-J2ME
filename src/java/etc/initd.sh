#!/java/bin/sh
# -*- coding: utf-8 -*-
#

set TTY=/java/optty1

set HOSTNAME=localhost
set RESPONSE=/java/etc/index.html

set PORT=31522
set QUERY=nano

mount /java/etc/fstab

x11 init
x11 term

bind
ifconfig
warn Listening at $OUTPUT:31522

start sh
