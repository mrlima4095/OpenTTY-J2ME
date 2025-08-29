#!/java/bin/sh
# -*- coding: utf-8 -*-
#

echo OpenTTY Debug Script
echo "-------------------------"
echo

log add debug Debug script had been executed

echo "[ MIDlet ]"
pkg MIDlet-1
echo

uname -a

echo
echo "Current Thread:"
mmspt

echo
echo "[ Device ]"

locale
hostid
hostname

echo 
echo "[ TTY ]"

tty
ttysize

echo "-------------------------"