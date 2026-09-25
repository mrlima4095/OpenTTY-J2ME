/* memtest.c - smoke test for gc()/mem_*()/time() in res/lib/libc.s. */
int printf(const char *fmt, ...);
void gc(void);
int mem_free(void);
int mem_total(void);
int mem_used(void);
long time(long *tloc);

int main(void)
{
    gc();
    printf("free=%dKB total=%dKB used=%dKB\n", mem_free(), mem_total(), mem_used());
    printf("time=%d\n", (int) time(0));
    return 0;
}