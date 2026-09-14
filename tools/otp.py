#!/usr/bin/env python3
"""otp.py - OpenTTY Packer v1 (OTP1) reference implementation.

Host-side twin of res/apps/src/otp_*.c. Produces/reads the exact same
format so the ELF build can be round-trip tested on the desktop.

Format:
  [0..3]   magic "OTP1"
  [4..7]   original size (u32 LE)
  [8..11]  packed size (u32 LE, 0 = stored/uncompressed)
  [12..15] crc32 (u32 LE, of original data)
  [16..]   payload (LZSS packed or raw)

LZSS: flag byte (8 bits, MSB first; 1=literal, 0=match).
  match token = byte0 dist&0xFF, byte1 ((dist>>8)&0xF)<<4 | (len-3).
  dist 0..4095, len 3..18.

Usage:
  python3 tools/otp.py c <input> <output>
  python3 tools/otp.py d <input> <output>
  python3 tools/otp.py i <file>
"""

import struct
import sys
import zlib

HDRSZ = 16
WSIZE = 4096
MXLEN = 18
MNLEN = 3


def _h3(b: bytes):
    return (b[0] * 31 + b[1]) * 31 + b[2] & (WSIZE - 1)


def pack(data: bytes) -> bytes:
    out = bytearray()
    i = 0
    n = len(data)
    ht = [-1] * WSIZE

    while i < n:
        flagpos = len(out)
        out.append(0)
        flag = 0

        for bits in range(8):
            if i >= n:
                break

            best_len = 0
            best_dist = 0
            if i + MNLEN <= n:
                h = _h3(data[i:])
                prev = ht[h]
                if prev >= 0 and 0 < i - prev <= WSIZE:
                    maxlen = min(MXLEN, n - i)
                    ln = 0
                    while ln < maxlen and data[prev + ln] == data[i + ln]:
                        ln += 1
                    if ln >= MNLEN:
                        best_len = ln
                        best_dist = i - prev
                ht[h] = i

            if best_len >= MNLEN:
                out.append(best_dist & 0xFF)
                out.append(((best_dist >> 8) & 0xF) << 4 | (best_len - MNLEN))
                for j in range(best_len):
                    if i + j + 2 < n:
                        ht[_h3(data[i + j:])] = i + j
                i += best_len
            else:
                flag |= 0x80 >> bits
                out.append(data[i])
                i += 1

        out[flagpos] = flag

    return bytes(out)


def unpack(packed: bytes) -> bytes:
    out = bytearray()
    ip = 0
    n = len(packed)

    while ip < n:
        flag = packed[ip]
        ip += 1
        for bits in range(8):
            if ip >= n:
                break
            if flag & (0x80 >> bits):
                out.append(packed[ip])
                ip += 1
            else:
                lo = packed[ip]
                hi = packed[ip + 1]
                ip += 2
                dist = lo | ((hi >> 4) << 8)
                ln = (hi & 0xF) + MNLEN
                if dist == 0:
                    raise ValueError("corrupt stream: zero distance")
                if dist > len(out):
                    raise ValueError("corrupt stream: distance past start")
                for _ in range(ln):
                    out.append(out[len(out) - dist])

    return bytes(out)


def make_header(orig_size: int, packed_size: int, crc: int) -> bytes:
    return b"OTP1" + struct.pack("<III", orig_size, packed_size, crc)


def parse_header(h: bytes):
    if h[:4] != b"OTP1":
        raise ValueError("not an OTP1 file")
    orig, packed, crc = struct.unpack("<III", h[4:16])
    return orig, packed, crc


def do_compress(inp, outp):
    data = open(inp, "rb").read()
    if not data:
        print("otp: empty input", file=sys.stderr)
        return 1
    crc = zlib.crc32(data) & 0xFFFFFFFF
    packed = pack(data)
    if len(packed) >= len(data):
        packed = b""
    hdr = make_header(len(data), len(packed), crc)
    with open(outp, "wb") as f:
        f.write(hdr)
        f.write(packed if packed else data)
    print("otp: %d -> %d bytes (%d%%)" % (
        len(data), len(hdr) + len(packed or data),
        (len(hdr) + len(packed or data)) * 100 // len(data)))
    return 0


def do_decompress(inp, outp):
    blob = open(inp, "rb").read()
    if len(blob) < HDRSZ:
        print("otp: file too small", file=sys.stderr)
        return 1
    orig, packed_len, crc = parse_header(blob[:HDRSZ])
    payload = blob[HDRSZ:]
    if packed_len == 0:
        raw = payload[:orig]
    else:
        raw = unpack(payload[:packed_len])
    if len(raw) != orig:
        print("otp: size mismatch: expected %d got %d" % (orig, len(raw)),
              file=sys.stderr)
        return 1
    if zlib.crc32(raw) & 0xFFFFFFFF != crc:
        print("otp: CRC mismatch", file=sys.stderr)
        return 1
    with open(outp, "wb") as f:
        f.write(raw)
    print("otp: decompressed to %d bytes" % len(raw))
    return 0


def do_info(path):
    blob = open(path, "rb").read()
    if len(blob) < HDRSZ:
        print("otp: file too small", file=sys.stderr)
        return 1
    orig, packed_len, crc = parse_header(blob[:HDRSZ])
    print("OTP1 file: %s" % path)
    print("  original:  %d bytes" % orig)
    if packed_len == 0:
        print("  mode:      stored (uncompressed)")
    else:
        print("  packed:    %d bytes" % packed_len)
        print("  ratio:     %d%%" % ((packed_len + HDRSZ) * 100 // (orig + HDRSZ)))
    print("  crc32:     0x%08x" % crc)
    return 0


def main():
    if len(sys.argv) < 2:
        print("otp - OpenTTY Packer v1 (LZSS + CRC32)")
        print("usage: otp.py c <input> <output>   compress")
        print("       otp.py d <input> <output>   decompress")
        print("       otp.py i <file>             show info")
        return 1
    cmd = sys.argv[1]
    if cmd == "c" and len(sys.argv) >= 4:
        return do_compress(sys.argv[2], sys.argv[3])
    if cmd == "d" and len(sys.argv) >= 4:
        return do_decompress(sys.argv[2], sys.argv[3])
    if cmd == "i" and len(sys.argv) >= 3:
        return do_info(sys.argv[2])
    print("otp.py: unknown command or args", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())