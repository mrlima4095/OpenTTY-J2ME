/* bench.c - CPU benchmark suite, the C/RISC-V twin of bench/bench.lua.
 *
 * Both files run the same eight micro-benchmarks with the same sizes and
 * produce the same checksum per test, so a diff of the two reports shows
 * only the elapsed time. Keep the two files in sync when editing.
 *
 * Usage: bench [scale] [passes] [report]
 *   scale   multiplies the loop counts (caps below)
 *   passes  repeats the whole suite (default 1)
 *   report  report path (default /mnt/opentty/bench-c.txt)
 *
 * Build: ./build-elf.sh bench/bench.c -stdlib -o bench/bench
 */

int printf(const char *fmt, ...);
int sprintf(char *dst, const char *fmt, ...);
int atoi(const char *s);
int strlen(const char *s);
char *strcat(char *dst, const char *src);
void *malloc(unsigned int size);
void free(void *ptr);
int open(const char *path, int flags, int mode);
int write(int fd, const void *buf, unsigned int count);
int close(int fd);
void exit(int status);

#define O_WRONLY 1
#define O_CREAT 64
#define O_TRUNC 512

/* Sizes: must match the SIZE block in bench.lua. */
#define BASE_SUM    50000
#define CAP_SUM     60000
#define BASE_CALLS  20000
#define CAP_CALLS   60000
#define BASE_SIEVE  20000
#define CAP_SIEVE   60000
#define BASE_SORT   400
#define CAP_SORT    4000
#define BASE_STRING 600
#define CAP_STRING  4000
#define BASE_ARRAY  20000
#define CAP_ARRAY   40000
#define BASE_ALLOC  2000
#define CAP_ALLOC   20000

static unsigned char composite[CAP_SIEVE + 1];
static int slots[CAP_SORT];
static int values[CAP_ARRAY];
static char text[CAP_STRING * 2 + 1];
static char report[8192];
static char line[160];

static int g_scale = 1;

static int scaled(int base, int cap)
{
    int n = base * g_scale;
    return n > cap ? cap : n;
}

/* Clock in milliseconds. time() has a one second resolution, so read the
 * gettimeofday syscall (78) directly; the seconds are masked to keep the
 * arithmetic inside 32 bits. */
struct timeval { int tv_sec; int tv_usec; };

static int now_ms(void)
{
    struct timeval tv;
    register int a0 __asm__("a0") = (int) &tv;
    register int a1 __asm__("a1") = 0;
    register int a7 __asm__("a7") = 78;
    __asm__ volatile ("ecall" : "+r"(a0) : "r"(a1), "r"(a7) : "memory");
    return ((tv.tv_sec % 100000) * 1000) + (tv.tv_usec / 1000);
}

static void emit(const char *text)
{
    printf("%s", text);
    strcat(report, text);
}

/* 1. Integer loop: add 1..n without any function call. */
static int test_sum(int n)
{
    int s = 0, i = 1;
    while (i <= n) { s = s + i; i = i + 1; }
    return s % 1000003;
}

/* 2. Function call per iteration. */
static int add(int a, int b) { return a + b; }

static int test_calls(int n)
{
    int s = 0, i = 1;
    while (i <= n) { s = add(s, i); i = i + 1; }
    return s % 1000003;
}

/* 3. Recursive Fibonacci (fib(20) = 21891 calls). */
static int fib(int k)
{
    if (k < 2) { return k; }
    return fib(k - 1) + fib(k - 2);
}

static int test_fib(int n) { return fib(n) % 1000003; }

/* 4. Sieve of Eratosthenes over a byte array. */
static int test_sieve(int n)
{
    int i, j, count = 0, sum = 0;
    for (i = 0; i <= n; i++) { composite[i] = 0; }
    i = 2;
    while (i <= n) {
        if (!composite[i]) {
            count = count + 1;
            sum = sum + i;
            /* i * i overflows 32 bits above 46340, and there is nothing to
             * cross out past sqrt(n) anyway. */
            if (i <= n / i) {
                j = i * i;
                while (j <= n) { composite[j] = 1; j = j + i; }
            }
        }
        i = i + 1;
    }
    return (count * 1000 + sum) % 1000003;
}

/* 5. Insertion sort of an LCG-filled array, then a weighted checksum. */
static int test_sort(int n)
{
    unsigned int seed = 12345u;
    int i, j, v, s = 0;
    for (i = 0; i < n; i++) {
        seed = (seed * 75u + 74u) % 65537u;
        slots[i] = (int) (seed % 1000u);
    }
    for (i = 1; i < n; i++) {
        v = slots[i];
        j = i - 1;
        while (j >= 0 && slots[j] > v) { slots[j + 1] = slots[j]; j = j - 1; }
        slots[j + 1] = v;
    }
    for (i = 0; i < n; i++) { s = (s + slots[i] * (i + 1)) % 1000003; }
    return s;
}

/* 6. String building (n * 2 chars) and a byte-by-byte hash. */
static int test_string(int n)
{
    int i, h = 0, len;
    text[0] = 0;
    for (i = 0; i < n; i++) { strcat(text, "ab"); }
    len = strlen(text);
    for (i = 0; i < len; i++) { h = (h * 31 + (text[i] & 0xFF)) % 1000003; }
    return h;
}

/* 7. Array fill: i * i % 1000 for each slot, then the sum. */
static int test_array(int n)
{
    int i, s = 0;
    for (i = 0; i < n; i++) { values[i] = (i * i) % 1000; }
    for (i = 0; i < n; i++) { s = (s + values[i]) % 1000003; }
    return s;
}

/* 8. Allocation: n blocks of 8 ints, filled and freed one at a time. */
static int test_alloc(int n)
{
    int i, k, s = 0;
    for (i = 1; i <= n; i++) {
        int *block = (int *) malloc(8 * sizeof(int));
        if (block == 0) { return -1; }
        for (k = 1; k <= 8; k++) {
            block[k - 1] = k + i;
            s = (s + block[k - 1]) % 1000003;
        }
        free(block);
    }
    return s;
}

static int run(const char *name, int n, int (*fn)(int), int total)
{
    int started = now_ms();
    int checksum = fn(n);
    int ms = now_ms() - started;
    sprintf(line, "%s n=%d ms=%d checksum=%d\n", name, n, ms, checksum);
    emit(line);
    return total + ms;
}

int main(int argc, char **argv)
{
    int scale = 1, passes = 1, pass, fd, len;
    const char *outpath = "/mnt/opentty/bench-c.txt";
    int n_sum, n_calls, n_fib, n_sieve, n_sort, n_string, n_array, n_alloc;
    int total = 0;

    if (argc > 1) { scale = atoi(argv[1]); }
    if (argc > 2) { passes = atoi(argv[2]); }
    if (argc > 3) { outpath = argv[3]; }
    if (scale < 1) { scale = 1; }
    if (passes < 1) { passes = 1; }
    g_scale = scale;

    n_sum = scaled(BASE_SUM, CAP_SUM);
    n_calls = scaled(BASE_CALLS, CAP_CALLS);
    n_fib = 20 + scale - 1;
    if (n_fib > 24) { n_fib = 24; }
    n_sieve = scaled(BASE_SIEVE, CAP_SIEVE);
    n_sort = scaled(BASE_SORT, CAP_SORT);
    n_string = scaled(BASE_STRING, CAP_STRING);
    n_array = scaled(BASE_ARRAY, CAP_ARRAY);
    n_alloc = scaled(BASE_ALLOC, CAP_ALLOC);

    report[0] = 0;
    for (pass = 1; pass <= passes; pass++) {
        total = 0;
        sprintf(line, "== bench (c/rv32) pass %d ==\n", pass);
        emit(line);
        total = run("sum_loop", n_sum, test_sum, total);
        total = run("calls", n_calls, test_calls, total);
        total = run("fib", n_fib, test_fib, total);
        total = run("sieve", n_sieve, test_sieve, total);
        total = run("sort", n_sort, test_sort, total);
        total = run("string", n_string, test_string, total);
        total = run("array", n_array, test_array, total);
        total = run("alloc", n_alloc, test_alloc, total);
        sprintf(line, "total ms=%d\n", total);
        emit(line);
    }

    len = strlen(report);
    fd = open(outpath, O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (fd < 0) { fd = open("/tmp/bench-c.txt", O_WRONLY | O_CREAT | O_TRUNC, 0644); }
    if (fd >= 0) { write(fd, report, len); close(fd); }

    exit(0);
    return 0;
}
