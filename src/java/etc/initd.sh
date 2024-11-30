#!/java/bin/sh
# -*- coding: utf-8 -*-
#    
#    . /java/etc/initd.sh 
#    

set TTY=/java/optty1

set HOSTNAME=localhost
set RESPONSE=/java/etc/index.html

set PORT=31522
set QUERY=nano


mount /java/etc/fstab

x11 init
x11 term

start sh