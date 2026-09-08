#!/bin/bash
# build.sh — compila o multi-tool busybox para o emulador ELF ARM32 do OpenTTY.
#
# Uso: ./res/apps/busybox/build.sh
#   -> res/apps/busybox/busybox         (ELF unico)
#   -> res/apps/busybox/applets/<name>  (copias do mesmo ELF, uma por comando)
#
# Depois, no dispositivo, instale applets/<cmd> em /bin/<cmd> (o shell resolve
# /bin/<nome>, e o busybox despacha pelo basename(argv[0])).
#
# Requisitos: binutils-arm-none-eabi (arm-none-eabi-gcc/as/ld) + build-elf.sh.
set -e

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$HERE/../../.."
APPLETS="basename base64 cat clear cmp cp date dirname du echo env false head id ls md5sum mkdir mv printf rev rm rmdir seq sort sum tail touch tr true uname wc whoami"

cd "$ROOT"

# sanity sintatico (gcc do host, sem a parte ARM)
if command -v gcc >/dev/null 2>&1; then
    gcc -fsyntax-only -fno-builtin -Wall \
        res/apps/busybox/busybox.c res/apps/busybox/bb_file.c \
        res/apps/busybox/bb_crypto.c res/apps/busybox/bb_date.c
fi

./build-elf.sh \
    res/apps/busybox/busybox.c res/apps/busybox/bb_file.c \
    res/apps/busybox/bb_crypto.c res/apps/busybox/bb_date.c \
    res/apps/busybox/bb_sys.s -stdlib \
    -o res/apps/busybox/busybox

rm -rf res/apps/busybox/applets
mkdir -p res/apps/busybox/applets
for t in $APPLETS; do
    cp -f res/apps/busybox/busybox "res/apps/busybox/applets/$t"
done

echo "OK: res/apps/busybox/busybox"
echo "    $(echo $APPLETS | wc -w) applets (copias do mesmo ELF) em res/apps/busybox/applets/"