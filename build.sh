#!/bin/sh
# build.sh — compila o OpenTTY (jar/jad) com o SDKCLI.
#
# Uso:
#   ./build.sh                 git pull --ff-only + compila como está local
#   ./build.sh --local         NÃO faz git pull; usa o que está no disco
#   ./build.sh --lite          compila com o STUB lite do emulador (jar menor)
#                               e restaura o emulador completo ao final
#   ~/build.sh --local --lite  combina os dois

set -e

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
cd "$ROOT"

LITE=0
LOCAL=0
for arg in "$@"; do
    case "$arg" in
        --lite)  LITE=1 ;;
        --local) LOCAL=1 ;;
        *) echo "build.sh: flag desconhecida '$arg' (use --lite ou --local)" >&2; exit 2 ;;
    esac
done

[ "$LOCAL" -eq 1 ] || { echo "=> git pull --ff-only"; git pull --ff-only; }

# Marker do stub lite: "ELF Lite" so existe no res/archive/elf/Lite.java.
if grep -q "ELF Lite" "$ROOT/src/ELF.java"; then
    WAS_LITE=1
else
    WAS_LITE=0
fi

# Garante o modo desejado para a build (swap_lite alterna o estado).
if [ "$LITE" -eq 1 ] && [ "$WAS_LITE" -eq 0 ]; then
    echo "=> ativando stub lite do emulador"
    "$ROOT/res/swap_lite.sh"
fi
if [ "$LITE" -eq 0 ] && [ "$WAS_LITE" -eq 1 ]; then
    echo "=> restaurando emulador completo (estava em lite)"
    "$ROOT/res/swap_lite.sh"
fi

java -jar "$ROOT/sdkcli.jar" "$ROOT/" OpenTTY.jar OpenTTY.jad
mv "$ROOT"/dist/OpenTTY.* /var/www/opentty/dist
echo "=> dist copiado para /var/www/opentty/dist"

# Nunca deixa a arvore em modo lite sem pedido explicito futuro.
if [ "$LITE" -eq 1 ] && [ "$WAS_LITE" -eq 0 ]; then
    cd "$ROOT"
    echo "=> restaurando emulador completo"
    "$ROOT/res/swap_lite.sh"
fi
