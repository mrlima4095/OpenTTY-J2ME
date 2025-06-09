#!/java/bin/sh
# -*- coding: utf-8 -*-
#

set TTY=/java/optty1

set HOSTNAME=localhost
set RESPONSE=/java/etc/index.html

set REPO=31.97.20.160:31522
set PORT=31522
set QUERY=nano

mount /java/etc/fstab

x11 init
x11 term

start sh
