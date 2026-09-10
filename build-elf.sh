#!/bin/bash
# build-elf.sh — Monta .s/.S (e opcionalmente .c) em ELF RISC-V RV32IM para o
# emulador do OpenTTY (src/ELF.java, EM_RISCV 243).
#
# O emulador exige ET_EXEC, ELFCLASS32, little-endian, EM_RISCV (243), sem
# extensao C, static, e com o entry (e_entry) dentro de 1 MB (a RAM virtual e'
# um byte[1MB]). O core do emulador implementa RV32I + extensao M (mul/div).
#
# Uso:
#   ./build-elf.sh programa.s                 -> ./programa
#   ./build-elf.sh a.s b.s -o app             -> ./app
#   ./build-elf.sh app.c -T 0x8000            -> texto comeca em 0x8000
#   CROSS=riscv64-unknown-elf- ./build-elf.sh x.s
#
#   ./build-elf.sh app.s -lib                   -> linka com res/lib/lib32.s
#                                              (programa define main; a lib
#                                              fornece _start/puts/printf/...)
#   ./build-elf.sh demo.c -stdlib               -> linka com res/lib/libc.s:
#                                              wrappers RISC-V (li a7,#LIB; ecall
#                                              -> LIB no EMULADOR). Suporta .c:
#                                              printf/sprintf/malloc/free/
#                                              memcpy/divisao 64-bit/etc.
#
# Opcoes:
#   -o <arquivo>   nome do ELF final (default: basename do 1o fonte)
#   -T <addr>      endereco do inicio do .text (default 0x10000)
#   -lib           inclui a runtime res/lib/lib32.s (stdlib em asm RV32I + syscalls)
#   -stdlib        inclui res/lib/libc.s (stdlib no emulador, wrappers LIB_* via ecall)
#   -shared        gera um shared object (ET_DYN, .so) em vez de executavel
#   -entry <sym>   simbolo de entrada (default _start)
#   -keep          mantem os .o intermediarios
#   -h             mostra o help

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB32="$HERE/res/lib/lib32.s"
LIBC="$HERE/res/lib/libc.s"

TEXT=""
ENTRY="_start"
USE_LIB=0
USE_STDLIB=0
SHARED=0
KEEP=0
OUTPUT=""
INPUTS=()

usage() { sed -n '2,16p' "$0"; exit 0; }

# --- localiza o toolchain RISC-V (binutils) --------------------------------
pick_toolchain() {
    for p in "${CROSS:-}" riscv64-unknown-elf- riscv64-unknown-linux-gnu- riscv64-linux-gnu- riscv32-unknown-elf-; do
        [ -z "$p" ] && continue
        if command -v "${p}as" >/dev/null 2>&1; then
            AS="${p}as"; LD="${p}ld"
            GCC=""; command -v "${p}gcc" >/dev/null 2>&1 && GCC="${p}gcc"
            READELF="${p}readelf"; command -v "$READELF" >/dev/null 2>&1 || READELF=""
            ASFLAGS=(-march=rv32im -mabi=ilp32)
            LDFLAGS=(-m elf32lriscv)
            return 0
        fi
    done
    echo "Erro: toolchain RISC-V nao encontrado." >&2
    echo "Instale:  sudo apt install gcc-riscv64-unknown-elf  (ou riscv64-linux-gnu)" >&2
    exit 1
}

# --- valida o header ELF conforme o que o src/ELF.java exige ---------------
validate_elf() {
    local f="$1" et="${2:-2}"
    command -v python3 >/dev/null 2>&1 || { echo "Aviso: python3 ausente; pulando validacao." >&2; return 0; }
    python3 - "$f" "$et" <<'PY'
import struct, sys
b = open(sys.argv[1], 'rb').read(64)
want = int(sys.argv[2])
err = []
if b[0:4] != b'\x7fELF':  err.append("magic nao-ELF")
if b[4] != 1:             err.append("ei_class!=ELFCLASS32")
if b[5] != 1:             err.append("ei_data!=LSB (little-endian)")
if struct.unpack('<H', b[16:18])[0] != want: err.append("e_type!=ET_%s" % ("DYN" if want == 3 else "EXEC"))
if struct.unpack('<H', b[18:20])[0] != 243: err.append("e_machine!=EM_RISCV (243)")
entry = struct.unpack('<I', b[24:28])[0]
if err:
    print("FALHA na validacao:", ", ".join(err)); sys.exit(1)
print("OK: ELF32 LE, ET_%s, EM_RISCV (243)" % ("DYN" if want == 3 else "EXEC"))
print("e_entry (PC inicial): 0x%.8x" % entry)
if want == 2 and entry >= 0x100000:
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
        -stdlib) USE_STDLIB=1 ;;
        -shared) SHARED=1 ;;
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
LIBFLAGS=()

# runtime libc.s do emulador (stdlib via LIB_* + ecall) antes dos fontes
if [ "$USE_STDLIB" -eq 1 ]; then
    [ "$USE_LIB" -eq 1 ] && { echo "Erro: -lib e -stdlib sao mutuamente exclusivos." >&2; exit 1; }
    [ -f "$LIBC" ] || { echo "Erro: $LIBC nao existe (o -stdlib precisa dela)." >&2; exit 1; }
    "$AS" "${ASFLAGS[@]}" -o "$WORK/0.o" "$LIBC"
    OBJS+=("$WORK/0.o")
fi

# runtime lib32.s antes dos fontes do programa (o entry _start dela chama main)
if [ "$USE_LIB" -eq 1 ]; then
    [ -f "$LIB32" ] || { echo "Erro: $LIB32 nao existe (o -lib precisa dela)." >&2; exit 1; }
    awk 'BEGIN{drop=0} /^main:/{drop=1} !drop{print}' "$LIB32" > "$WORK/lib32.lib.s"
    "$AS" "${ASFLAGS[@]}" -o "$WORK/0.o" "$WORK/lib32.lib.s"
    OBJS+=("$WORK/0.o")
fi

n=1
for src in "${INPUTS[@]}"; do
    [ -f "$src" ] || { echo "Erro: fonte '$src' nao encontrado." >&2; exit 1; }
    case "$src" in
        *.so)
            libdir="$(dirname "$src")"; libbase="$(basename "$src")"; libname="${libbase%.so}"
            [ "${libname:0:3}" = "lib" ] && libname="${libname:3}"
            [ "$libdir" = "." ] && libdir="$PWD"
            LIBFLAGS+=("-L" "$libdir" "-l$libname")
            n=$((n + 1)); continue ;;
        *.S|*.sx)
            if [ -n "$GCC" ]; then
                "$GCC" -march=rv32im -mabi=ilp32 -x assembler-with-cpp -c -o "$WORK/$n.o" "$src"
            else
                cpp -P "$src" | "$AS" "${ASFLAGS[@]}" -o "$WORK/$n.o" -
            fi ;;
        *.s)
            "$AS" "${ASFLAGS[@]}" -o "$WORK/$n.o" "$src" ;;
        *.c)
            [ -n "$GCC" ] || { echo "Erro: preciso de ${AS%as}gcc para compilar .c." >&2; exit 1; }
            if [ "$SHARED" -eq 1 ]; then
                "$GCC" -march=rv32im -mabi=ilp32 -fPIC -nostdlib -static -fno-builtin -c -o "$WORK/$n.o" "$src"
            else
                "$GCC" -march=rv32im -mabi=ilp32 -nostdlib -static -fno-builtin -c -o "$WORK/$n.o" "$src"
            fi ;;
        *) echo "Erro: extensao nao suportada em '$src' (use .s/.S/.sx/.c)." >&2; exit 1 ;;
    esac
    OBJS+=("$WORK/$n.o")
    n=$((n + 1))
done

if [ -z "$TEXT" ]; then
    if [ "$SHARED" -eq 1 ] || [ ${#LIBFLAGS[@]} -gt 0 ]; then TEXT="0x20000"; else TEXT="0x10000"; fi
fi

if [ "$SHARED" -eq 1 ]; then
    "$LD" "${LDFLAGS[@]}" -shared --hash-style=sysv --no-as-needed -Ttext="$TEXT" -o "$OUTPUT" "${OBJS[@]}" "${LIBFLAGS[@]}"
else
    # The emulator resolves DT_NEEDED itself; an ELF interpreter is neither
    # available nor useful and would overlap .text at the fixed guest address.
    "$LD" "${LDFLAGS[@]}" --hash-style=sysv -Ttext="$TEXT" --no-dynamic-linker --no-as-needed --allow-shlib-undefined --entry="$ENTRY" -o "$OUTPUT" "${OBJS[@]}" "${LIBFLAGS[@]}"
fi

if [ "$KEEP" -eq 1 ]; then cp "$WORK"/*.o "$(dirname "$OUTPUT")/" && echo "Objetos .o preservados em: $(dirname "$OUTPUT")/"; fi

if [ "$SHARED" -eq 1 ]; then
    echo "Gerado: $OUTPUT (shared object)"
    validate_elf "$OUTPUT" 3
else
    echo "Gerado: $OUTPUT"
    validate_elf "$OUTPUT" 2
fi
