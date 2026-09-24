/* cal.c - Gregorian calendar on stdout.
 *
 * cal             -> current month (UTC, via the time syscall)
 * cal [MONTH YEAR]-> calendar for one month
 * cal YEAR        -> all 12 months of the year
 * Weekday comes from Sakamoto's algorithm; days per month respect leap
 * years. The current date uses Howard Hinnant's civil conversion (32-bit).
 * Build:
 *   ./build-elf.sh res/apps/src/cal.c -stdlib -o res/apps/dist/cal
 */
int printf(const char *fmt, ...);
int atoi(const char *s);
void exit(int status);
long time(long *t);

static const char *months[12] = {
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
};

static int leap(int y) { return (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)); }

static int dim(int m, int y)
{
    static const int d[12] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    if (m == 2 && leap(y)) { return 29; }
    return d[m - 1];
}

/* Sakamoto: 0 = Sunday */
static int dow(int y, int m, int d)
{
    static const int t[12] = { 0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4 };
    if (m < 3) { y--; }
    return (y + y / 4 - y / 100 + y / 400 + t[m - 1] + d) % 7;
}

/* epoch (segundos UTC) -> ano/mes, algoritmo de Howard Hinnant (32-bit) */
static void epoch_to_ym(int t, int *y, int *mo)
{
    int days = t / 86400, z, era, yy;
    unsigned doe, yoe, doy, mp;
    z = days + 719468;
    era = (z >= 0 ? z : z - 146096) / 146097;
    doe = (unsigned)(z - era * 146097);
    yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
    yy = (int)yoe + era * 400;
    doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
    mp = (5 * doy + 2) / 153;
    *mo = (int)(mp < 10 ? mp + 3 : mp - 9);
    *y = yy + (*mo <= 2);
}

static void show(int m, int y)
{
    int first, days, c, d;

    printf("      %s %d\n", months[m - 1], y);
    printf("Su Mo Tu We Th Fr Sa\n");

    first = dow(y, m, 1);
    days = dim(m, y);
    for (c = 0; c < first; c++) { printf("   "); }
    for (d = 1; d <= days; d++) {
        printf("%2d", d);
        if (++c > 6) { printf("\n"); c = 0; }
        else { printf(" "); }
    }
    if (c != 0) { printf("\n"); }
    printf("\n");
}

int main(int argc, char **argv)
{
    int m, y;

    if (argc == 1) {
        int t = (int) time(0);
        epoch_to_ym(t, &y, &m);
        show(m, y);
    } else if (argc == 2) {
        y = atoi(argv[1]);
        if (y < 1600) { printf("cal: year %d too small\n", y); exit(1); }
        for (m = 1; m <= 12; m++) { show(m, y); }
    } else if (argc == 3) {
        m = atoi(argv[1]);
        y = atoi(argv[2]);
        if (y < 1600) { printf("cal: year %d too small\n", y); exit(1); }
        if (m < 1 || m > 12) { printf("cal: month %d out of range\n", m); exit(1); }
        show(m, y);
    } else {
        printf("Usage: cal [MONTH YEAR] | cal YEAR\n");
        exit(1);
    }

    exit(0);
    return 0;
}