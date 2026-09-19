#!/bin/sh
# Build and run OpenTTY on the desktop (stubs + real J2ME source).
set -e
cd "$(dirname "$0")"

sh stubs/build.sh "$@"