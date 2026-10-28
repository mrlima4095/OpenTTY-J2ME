# bench/ — Lua vs RISC-V ELF benchmark twins

Two programs that do **exactly the same work**: `bench.lua` (run by
`src/Lua.java`) and `bench.c` (built for the RISC-V emulator in
`src/ELF.java`). Every test prints `name n=<size> ms=<time> checksum=<value>`;
the checksums must match between the two reports, otherwise the comparison is
meaningless.

## Tests

The same eight tests, in the same order, with the same sizes:

| Test | Work | n at scale 1 |
| --- | --- | --- |
| `sum_loop` | `s = s + i` in a `while` loop (7.2 ops/iteration) | 50000 |
| `calls` | one function call per iteration, adding a counter | 20000 |
| `fib` | recursive `fib(20)`, 21891 calls | 20 |
| `sieve` | sieve of Eratosthenes + prime count/sum | 20000 |
| `sort` | insertion sort of an LCG-filled array + weighted checksum | 400 |
| `string` | build a 2n-char string, then hash it byte by byte | 600 |
| `array` | fill `i*i % 1000`, then sum the slots | 20000 |
| `alloc` | allocate/fill/drop n blocks of 8 slots | 2000 |

`scale` multiplies every size (with caps that keep 32-bit arithmetic exact in
the C twin); `passes` repeats the whole suite in the same process. Sizes are
defined twice — once in each file — and must be edited together.

## Running

On the desktop (headless, via the `pc/` shim), one command builds the ELF, runs
both twins and prints a comparison table:

```sh
bench/run.sh            # scale=4, passes=3
bench/run.sh 2 1        # smaller and quicker
```

Raw reports land in `data/mnt/opentty/bench-lua.txt` and
`.../bench-c.txt`; each process also prints its report to the console.

On the device (or in the shim directly):

```sh
./build-elf.sh bench/bench.c -stdlib -o /tmp/bench   # needs the RISC-V toolchain
cp bench/bench.lua /tmp/
lua /tmp/bench.lua 4 3        # Lua twin
/tmp/bench 4 3                # ELF twin
```

Both try to write their report to `/mnt/opentty/bench-<lang>.txt` and fall back
to `/tmp/bench-<lang>.txt`.

## Measured result

Desktop shim, OpenJDK 21 (JIT enabled), AMD EPYC 9354P, scale 4, passes 3,
fastest pass per test (2026-10-28):

| Test | n | C/RV32 | Lua | C speedup |
| --- | --- | --- | --- | --- |
| `sum_loop` | 60000 | 7 ms | 146 ms | 20.9x |
| `calls` | 60000 | 20 ms | 202 ms | 10.1x |
| `fib` | 23 | 25 ms | 218 ms | 8.7x |
| `sieve` | 60000 | 49 ms | 479 ms | 9.8x |
| `sort` | 1600 | 249 ms | 2851 ms | 11.4x |
| `string` | 2400 | 7 ms | 38 ms | 5.4x |
| `array` | 40000 | 27 ms | 265 ms | 9.8x |
| `alloc` | 8000 | 38 ms | 294 ms | 7.7x |
| **total** | | **421 ms** | **4493 ms** | **10.7x** |

All eight checksums matched (`24600 24600 28657 905207 810680 771809 459946
319232`).

Reading the table: the ELF side wins every CPU-bound test by roughly one order
of magnitude, and the tightest arithmetic loop (`sum_loop`) wins the most.
Where the work goes through the host anyway (`string`, whose bytes are copied
by Java on both sides) the gap shrinks to ~5x.

Caveats, because they matter:

- **This is the desktop shim, not a phone.** HotSpot compiles both runtimes;
  a CLDC/MIDP VM interprets bytecode, and the emulator pays several method
  calls plus a `Hashtable` lookup (`midlet.sys.containsKey(pid)`) per guest
  instruction, which no inlining hides. Expect the ELF advantage to *grow* on
  a real device. Not measured here.
- **Best of N is reported.** A single cold pass is dominated by JVM warm-up:
  the first C pass in a fresh JVM measured 532 ms against 421 ms warm, and
  individual tests moved by 2-6x between cold and warm runs.
- **1 ms resolution.** `os.clock()` (Lua) and the `gettimeofday` syscall (C)
  both tick in milliseconds, so small tests are coarse — use `scale` to make
  them longer instead of trusting single-digit milliseconds.
- **`string` is not a fair string-library test.** Lua `..` allocates a new
  Java `String` per step and C uses `strcat` over the guest runtime, so both
  are quadratic; it measures the runtime, not a real formatter.

## Runtime fixes found while writing this

Two real bugs in `src/Lua.java` surfaced here and are fixed:

- `os.clock()` returned a `java.lang.Long`, which is not a Lua number, so
  `os.clock() - t0` failed with "Arithmetic operation on non-number types".
  It now returns `luaNumber(...)`, like `java.midlet.uptime` already did.
- `string.byte(s, i)` ignored the start index for the default end (it kept
  `end = 1`), so any `i > 1` returned `nil`. With two arguments the end now
  defaults to the start, as in Lua 5.x.
