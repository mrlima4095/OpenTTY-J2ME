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

echo [ MIDlet ]
pkg MIDlet-1
echo

!
version
build

echo
echo [ Device ]

locale
hostname
hostid

echo 
echo [ TTY ]

tty
ttysize

echo -------------------------