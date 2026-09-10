#!/bin/bash
# OpenTTY - swap_lite.sh
# Alterna src/ELF.java entre o emulador RISC-V completo e o stub "lite"
# (que valida o ELF e informa que o emulador nao esta presente na build).
#
# Uso: res/swap_lite.sh
#   - Se src/ELF.java e o emulador completo  -> troca para o stub lite
#   - Se src/ELF.java e o stub lite          -> restaura o emulador completo

set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FULL="$ROOT/src/ELF.java"
STUB="$ROOT/res/archive/elf/Lite.java"      # versao lite (stub)
SAVE="$ROOT/res/archive/elf/Full.java"      # backup do emulador completo

[ -f "$FULL" ] || { echo "swap_lite: $FULL nao encontrado"; exit 1; }
[ -f "$STUB" ] || { echo "swap_lite: $STUB nao encontrado"; exit 1; }

# O marcador "ELF Lite" so existe no stub.
if grep -q "ELF Lite" "$FULL"; then
    # src/ELF.java atual e o stub -> restaurar o emulador completo
    [ -f "$SAVE" ] || { echo "swap_lite: backup do emulador nao existe ($SAVE). Copie o ELF.java completo para ele."; exit 1; }
    cp -f "$SAVE" "$FULL"
    echo "swap_lite: emulador RISC-V completo restaurado em src/ELF.java"
else
    # src/ELF.java atual e o emulador -> trocar para o stub lite
    cp -f "$FULL" "$SAVE"
    cp -f "$STUB" "$FULL"
    echo "swap_lite: stub lite ativo em src/ELF.java (emulador salvo em $SAVE)"
fi