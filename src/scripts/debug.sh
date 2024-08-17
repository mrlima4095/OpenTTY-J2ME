#!/java/bin/sh

echo OpenTTY Debug
echo -------------------
echo

basename /java/bin/basename
locale
date
hostname
whoami
uname
echo PATH=$PATH
if $PATH / echo you are in root directory
if not $USERNAME echo you are not logged
!
tty
ttysize

echo -------------------