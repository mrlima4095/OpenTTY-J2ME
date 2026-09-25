#!/bin/sh
# J2ME release build test: runs the device SDK build (sdkcli.jar) and verifies
# the produced jar/jad are 1.18.2 and carry the full RISC-V emulator.
#
# Requires a JDK on PATH. This is slow (the SDK compile takes ~2-3 min) and is
# therefore NOT part of the default test run.

set -e
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$ROOT"

command -v java >/dev/null 2>&1 || { echo "run_j2me_build: no JDK on PATH" >&2; exit 2; }
[ -f sdkcli.jar ] || { echo "run_j2me_build: sdkcli.jar missing" >&2; exit 2; }

rm -rf "$ROOT/build/compiled" "$ROOT/build/preverified"
echo "== sdkcli build =="
java -jar "$ROOT/sdkcli.jar" "$ROOT/" OpenTTY.jar OpenTTY.jad

fails=0

# Version gate.
grep -q 'MIDlet-Version: 1.18.2' dist/OpenTTY.jad || { echo "FAIL: JAD MIDlet-Version != 1.18.2"; fails=1; }

# The Nokia-MIDlet-Background-Event attribute must not leak into the release
# manifest (it was removed from nbproject/project.properties in 1.18.2).
if grep -q 'Nokia-MIDlet-Background-Event' dist/OpenTTY.jad; then
    echo "FAIL: Nokia-MIDlet-Background-Event present in JAD"; fails=1
else
    echo "PASS: no Nokia-MIDlet-Background-Event in JAD"
fi

# Full emulator (not the lite stub): ELF class present.
if unzip -l dist/OpenTTY.jar | grep -q 'ELF.class'; then
    echo "PASS: ELF.class (full emulator) in jar"
else
    echo "FAIL: ELF.class missing from jar"; fails=1
fi

# Seed files that must ship (ramdisk, boot loader, lua core).
for resource in boot/grub.cfg boot/initrd.img boot/vmlinuz bin/init etc/sources lib/libcore.so; do
    if unzip -l dist/OpenTTY.jar | grep -q "$resource"; then
        echo "PASS: $resource in jar"
    else
        echo "FAIL: $resource missing from jar"; fails=1
    fi
done

# /etc/sources seeded for the released appstore version.
if unzip -p dist/OpenTTY.jar etc/sources | grep -q 'version = "1.18.2"'; then
    echo "PASS: etc/sources seeds appstore 1.18.2"
else
    echo "FAIL: etc/sources is not 1.18.2"; fails=1
fi

echo "run_j2me_build: $([ "$fails" -eq 0 ] && echo ALL CHECKS PASSED || echo FAILED)"
exit "$fails"