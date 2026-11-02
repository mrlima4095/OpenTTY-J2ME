# bench/ — Lua vs RISC-V ELF benchmark twins, plus the Lua Canvas benchmark

Two programs that do **exactly the same work**: `bench.lua` (run by
`src/Lua.java`) and `bench.c` (built for the RISC-V emulator in
`src/ELF.java`). Every test prints `name n=<size> ms=<time> checksum=<value>`;
the checksums must match between the two reports, otherwise the comparison is
meaningless.

`canvas.lua` (+ `run_canvas.sh`) is a third program: a benchmark of the
`graphics.canvas` drawing API itself, described in
[Lua Canvas benchmark](#lua-canvas-benchmark-canvaslua) below.

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

## Lua Canvas benchmark (`canvas.lua`)

`bench/canvas.lua` measures the `graphics.canvas` API (see
[`docs/LUACANVAS.md`](../docs/LUACANVAS.md)): what one call costs from Lua,
through the native dispatch in `src/Lua.java` and the `ensureBuffer()` guard in
`src/LuaCanvas.java`, down to the MIDP `Graphics` call. One persistent 240x320
canvas is reused by every test (the headless default size of the shim), and
each test grows its own batch until it has run for at least 25 ms, because
`os.clock()` ticks in milliseconds and a single primitive is far below that.

```sh
bench/run_canvas.sh            # scale=1, passes=4
bench/run_canvas.sh 2 6        # bigger starting batches, more warm-up
```

The raw report lands in `data/mnt/opentty/bench-canvas.txt` (the console log in
`.../bench-canvas.log`); the runner prints it and then a best-pass table. The
shim never exits on its own after a scripted command, so the runner polls the
report for the `done` marker and stops the JVM as soon as the suite finished.

### Tests

| Test | Call | What it exercises |
| --- | --- | --- |
| `native_call` | `c:color()` | marshalling + a native return value, no drawing |
| `color` | `c:color(0xRRGGBB)` | `ensureBuffer` + `Graphics.setColor` |
| `clear` | `c:clear()` | full-canvas background `fillRect` |
| `clear_rect` | `c:clear(x,y,w,h)` | small fill (HUD-style erase) |
| `pixel` | `c:pixel(x,y)` | one `drawLine` of a single pixel |
| `line` | `c:line(x1,y1,x2,y2)` | `drawLine` |
| `rect` | `c:rect(...,true)` | `fillRect` |
| `circle` | `c:circle(...,true)` | `fillArc` (full sweep) |
| `polygon` | `c:polygon(t,true)` | caller-built table + Java scanline fill |
| `text` | `c:text(s,x,y)` | `drawString` |
| `print` | `c:print(s)` | cursor text: `charWidth` + `drawChar` per char |
| `metrics` | `c:metrics()` + `c:textWidth` | font metrics |
| `state` | `c:save()` + `c:restore()` | state stack push/pop |
| `clip` | `c:clip()` + `c:resetClip()` | clip change |
| `keys` | `c:keys()` | held-key table (polled every game frame) |
| `new` | `graphics.canvas.new` | handle + metatable + off-screen `Image` |
| `frame` | a full game frame | clear + HUD text + 24 shapes + paddle |

### Measured result

Desktop shim, OpenJDK 21 (JIT enabled), AMD EPYC 9354P, headless, 240x320,
scale 1, best of 8 passes (2026-11-01). The host is shared, so a single run
moves by up to ~2x between tests; the shape of the table is what to read.

| Test | n | ms | us/call | fps |
| --- | --- | --- | --- | --- |
| `native_call` | 20000 | 40 | 2.0 | - |
| `color` | 20000 | 49 | 2.5 | - |
| `clear` | 5000 | 38 | 7.6 | - |
| `clear_rect` | 9090 | 27 | 3.0 | - |
| `pixel` | 8333 | 26 | 3.1 | - |
| `line` | 8332 | 46 | 5.5 | - |
| `rect` | 10000 | 47 | 4.7 | - |
| `circle` | 6250 | 42 | 6.7 | - |
| `polygon` | 3125 | 29 | 9.3 | - |
| `text` | 5000 | 25 | 5.0 | - |
| `print` | 5000 | 39 | 7.8 | - |
| `metrics` | 7142 | 25 | 3.5 | - |
| `state` | 6250 | 26 | 4.2 | - |
| `clip` | 7142 | 25 | 3.5 | - |
| `keys` | 16666 | 44 | 2.6 | - |
| `new` | 40 | 2 | 50.0 | - |
| `frame` | 250 | 35 | 140.0 | 7143 |

Reading the table: ~2.0 us is the floor of *any* canvas call — the Lua
interpreter plus the native dispatch — so `color`, `keys`, `clip`, `metrics`
and `state` are essentially free, and the pixel work only shows up where the
fill area is large (`clear` 7.6 us for 76800 pixels, `circle` 6.7 us for a
filled disc). `print` costs ~0.5 us per character because `LuaCanvas` draws
character by character (`charWidth` + `drawChar`), and `polygon` carries both a
table allocation and the Java scanline fill. A whole 27-call game frame is
~140 us warm, three orders of magnitude below the 33 ms frame budget of the
loop.

Caveats, because they matter:

- **This is the desktop shim, not a phone.** HotSpot compiles the interpreter's
  Java, and the shim's `Graphics` is a `BufferedImage` — a CLDC/MIDP VM
  interprets the Lua and the device rasterizes into its own frame buffer. The
  per-call floor will be higher on hardware; the ratios between calls should
  hold.
- **No blit is measured.** Nothing here calls `flush()`/`repaint()`, and
  headless mode never paints, so the buffer-to-screen transfer (the part that
  actually limits fps on a phone) is outside these numbers.
- **Canvas size matters for the pixel-bound tests.** 240x320 is the shim's
  headless default; `clear`, `circle` and `frame` grow with the real window or
  screen size, the call-bound tests do not.
- **The first pass is warm-up.** A cold `frame` batch measured 1050 us/frame
  against 140 us warm; that is why the runner reports the best pass and why
  `passes` defaults to 4.
- **1 ms resolution.** Each test re-runs its batch with a bigger `n` until it
  clears 25 ms, so the reported `us/call` comes from a batch long enough to
  time; `new` is capped at 40 canvases (memory) and stays at 2 ms, which is
  coarse.
