#!/java/bin/sh
# -*- coding: utf-8 -*-

set ONLINE_VERSION=1.17


if ($VERSION == $ONLINE_VERSION) echo already up to date.
if ($VERSION != $ONLINE_VERSION) execute echo A new version was released!; echo; echo Local - OpenTTY $VERSION; echo Server - OpenTTY $ONLINE_VERSION; open http://opentty.xyz/dist/versions/OpenTTY-$ONLINE_VERSION.jar;

