#!/bin/bash
# build-elf.sh — Monta .s/.S (e opcionalmente .c) em ELF ARM32 para o emulador do OpenTTY.
#
# O emulador exige ET_EXEC, ELFCLASS32, little-endian, EM_ARM (40), sem Thumb,
# static, e com o entry (e_entry) dentro de 1 MB (a RAM virtual e' um byte[1MB]).
#
# Uso:
#   ./build-elf.sh programa.s                 -> ./programa
#   ./build-elf.sh a.s b.s -o app             -> ./app
#   ./build-elf.sh app.s -lib                 -> linka com res/lib/lib32.s
#                                              (programa define main; a lib
#                                              fornece _start/puts/printf/...)
#   ./build-elf.sh app.s -T 0x8000            -> texto comeca em 0x8000
#   CROSS=arm-linux-gnueabi- ./build-elf.sh x.s
#
# Opcoes:
#   -o <arquivo>   nome do ELF final (default: basename do 1o fonte)
#   -T <addr>      endereco do inicio do .text (default 0x10000)
#   -lib           inclui a runtime res/lib/lib32.s
#   -entry <sym>   simbolo de entrada (default _start)
#   -keep          mantem os .o intermediarios
#   -h             mostra o help

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB32="$HERE/res/lib/lib32.s"

TEXT="0x10000"
ENTRY="_start"
USE_LIB=0
KEEP=0
OUTPUT=""
INPUTS=()

usage() { sed -n '2,14p' "$0"; exit 0; }

# --- localiza o toolchain ARM (binutils) ---------------------------------
pick_toolchain() {
    for p in "${CROSS:-}" arm-none-eabi- arm-linux-gnueabi- arm-linux-gnueabihf-; do
        [ -z "$p" ] && continue
        if command -v "${p}as" >/dev/null 2>&1; then
            AS="${p}as"; LD="${p}ld"
            GCC=""; command -v "${p}gcc" >/dev/null 2>&1 && GCC="${p}gcc"
            READELF="${p}readelf"; command -v "$READELF" >/dev/null 2>&1 || READELF=""
            return 0
        fi
    done
    echo "Erro: toolchain ARM nao encontrado." >&2
    echo "Instale os binutils:  sudo apt install binutils-arm-none-eabi" >&2
    exit 1
}

# --- valida o header ELF conforme o que o src/ELF.java exige ---------------
validate_elf() {
    local f="$1"
    command -v python3 >/dev/null 2>&1 || { echo "Aviso: python3 ausente; pulando validacao." >&2; return 0; }
    python3 - "$f" <<'PY'
import struct, sys
b = open(sys.argv[1], 'rb').read(64)
err = []
if b[0:4] != b'\x7fELF':  err.append("magic nao-ELF")
if b[4] != 1:             err.append("ei_class!=ELFCLASS32")
if b[5] != 1:             err.append("ei_data!=LSB (little-endian)")
if struct.unpack('<H', b[16:18])[0] != 2: err.append("e_type!=ET_EXEC")
if struct.unpack('<H', b[18:20])[0] != 40: err.append("e_machine!=EM_ARM (40)")
entry = struct.unpack('<I', b[24:28])[0]
if err:
    print("FALHA na validacao:", ", ".join(err)); sys.exit(1)
print("OK: ELF32 LE, ET_EXEC, EM_ARM")
print("e_entry (PC inicial): 0x%.8x" % entry)
if entry >= 0x100000:
    print("AVISO: entry fora da RAM de 1MB do emulador (>=0x100000), nao vai executar."); sys.exit(1)
PY
    if [ -n "$READELF" ]; then "$READELF" -h -l "$f" | sed -n '1,40p'; fi
}

# --- parse de opcoes --------------------------------------------------------
while [ $# -gt 0 ]; do
    case "$1" in
        -h|--help) usage ;;
        -o) shift; OUTPUT="$1" ;;
        -T) shift; TEXT="$1" ;;
        -lib) USE_LIB=1 ;;
        -entry) shift; ENTRY="$1" ;;
        -keep) KEEP=1 ;;
        *) INPUTS+=("$1") ;;
    esac
    shift
done

[ ${#INPUTS[@]} -eq 0 ] && { echo "Uso: $0 <programa.s> [outros.s ...] [-o saida] [-T addr] [-lib]" >&2; exit 2; }

pick_toolchain
[ -z "$OUTPUT" ] && OUTPUT="$(basename "${INPUTS[0]}")"
case "$OUTPUT" in
    *.s|*.S|*.c|*.o) OUTPUT="${OUTPUT%.*}" ;;
esac

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

OBJS=()

# runtime lib32.s antes dos fontes do programa (o entry _start dela chama main)
if [ "$USE_LIB" -eq 1 ]; then
    [ -f "$LIB32" ] || { echo "Erro: $LIB32 nao existe (o -lib precisa dela)." >&2; exit 1; }
    awk 'BEGIN{drop=0} /^main:/{drop=1} !drop{print}' "$LIB32" > "$WORK/lib32.lib.s"
    "$AS" -o "$WORK/0.o" "$WORK/lib32.lib.s"
    OBJS+=("$WORK/0.o")
fi

n=1
for src in "${INPUTS[@]}"; do
    [ -f "$src" ] || { echo "Erro: fonte '$src' nao encontrado." >&2; exit 1; }
    case "$src" in
        *.S|*.sx)
            if [ -n "$GCC" ]; then
                "$GCC" -x assembler-with-cpp -c -o "$WORK/$n.o" "$src"
            else
                cpp -P "$src" | "$AS" -o "$WORK/$n.o" -
            fi ;;
        *.s)
            "$AS" -o "$WORK/$n.o" "$src" ;;
        *.c)
            [ -n "$GCC" ] || { echo "Erro: preciso de ${AS%as}gcc para compilar .c." >&2; exit 1; }
            "$GCC" -nostdlib -static -marm -fno-builtin -c -o "$WORK/$n.o" "$src" ;;
        *) echo "Erro: extensao nao suportada em '$src' (use .s/.S/.sx/.c)." >&2; exit 1 ;;
    esac
    OBJS+=("$WORK/$n.o")
    n=$((n + 1))
done

"$LD" -Ttext="$TEXT" --entry="$ENTRY" -o "$OUTPUT" "${OBJS[@]}"

if [ "$KEEP" -eq 1 ]; then cp "$WORK"/*.o "$(dirname "$OUTPUT")/" && echo "Objetos .o preservados em: $(dirname "$OUTPUT")/"; fi

echo "Gerado: $OUTPUT"
validate_elf "$OUTPUT"