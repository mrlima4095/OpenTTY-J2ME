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

touch
add quest.title=Remote
add quest.label=IP Adress
add quest.cmd=nc $ADDRESS
add quest.key=ADDRESS

alias xterm=execute exit; true 

x11 quest nano
gc

