#!/java/bin/sh
# -*- coding: utf-8 -*-
#    
#    . /scripts/debug.sh 
#    
#  Copyright (C) 2025 "Mr. Lima"
#  

echo OpenTTY Debug Script
echo -------------------------
echo

log add debug Debug script had been executed 

echo [ Version ]

!
version
build

echo 
echo [ MIDlet ]

pkg MIDlet-1
pkg MIDlet-Proxy

echo
echo Current Thread: 
mmspt

echo 
echo [ Device ]

locale
hostname
hostid

echo

echo PATH=$PATH
if ($PATH == /) echo You are in root directory

echo
echo [ TTY ]

tty
ttysize

echo -------------------------