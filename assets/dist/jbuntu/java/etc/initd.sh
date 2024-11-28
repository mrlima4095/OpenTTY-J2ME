#!/java/bin/sh
# -*- coding: utf-8 -*-
#    
#    . /java/etc/initd.sh 
#    

x11 init
mount /java/etc/fstab

set TTY=/java/optty1

set HOSTNAME=localhost
set RESPONSE=/java/etc/index.html

set PORT=31522
set QUERY=nano

import jbuntu
bg execute true; sleep 1; x11 make /java/lib/jbuntu