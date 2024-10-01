#!/java/bin/sh
# -*- coding: utf-8 -*-
#    
#    . /scripts/debug.sh 
#    
#  Copyright (C) 2024 "Mr. Lima"
#  

echo OpenTTY Debug 
echo -------------------
echo

log add debug Executed debug script 

basename /java/bin/basename
locale
date
hostname
whoami
uname
echo PATH=$PATH
if ($PATH /) echo you are in root directory
!
tty
ttysize

echo -------------------