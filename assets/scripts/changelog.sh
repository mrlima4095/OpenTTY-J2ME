#!/java/bin/sh
# -*- coding: utf-8 -*-

tick Downloading...
install nano
getty
netstat 
set WLAN_STATE=$OUTPUT
rraw
if (WLAN_STATE != true) execute echo Download failed - OpenTTY cant connect to Internet!; warn Check you network connect and try again; tick;
if (WLAN_STATE == true) execute proxy github.com/mrlima4095/OpenTTY-J2ME/raw/refs/tags/$VERSION/CHANGELOG.txt; add <title>CHANGELOG - $VERSION</title>; tick; html; get nano;