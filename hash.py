import itertools
import string
import sys

def java_string_hashcode(s: str) -> int:
    h = 0
    for ch in s:
        h = (31 * h + ord(ch)) & 0xFFFFFFFF
    if h & 0x80000000:
        h = -((~h + 1) & 0xFFFFFFFF)
    return h

print(java_string_hashcode(' '.join(sys.argv[1:])))