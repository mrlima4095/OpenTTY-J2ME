#!/java/bin/sh
# -*- coding: utf-8 -*-
#

set TTY=/java/optty1
set HOSTNAME=localhost

set PORT=31522
set QUERY=nano

mount /java/etc/fstab

x11 init
x11 term

start sh
