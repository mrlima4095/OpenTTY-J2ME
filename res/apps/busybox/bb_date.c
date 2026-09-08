/* bb_date.c - applet date (imprime data/hora UTC a partir do syscall time).
 * Conversao dias->civil com algoritmo inteiro de 32 bits (sem long long).
 */
#include "busybox.h"

static void epoch_to_civil(int t, int *y, int *mo, int *d,
                           int *h, int *mi, int *s, int *wd)
{
    int r1 = t / 60, days;
    *s = t % 60;
    *mi = r1 % 60;
    r1 = r1 / 60;
    *h = r1 % 24;
    days = r1 / 24;
    *wd = (days + 4) % 7;                  /* 1970-01-01 = quinta (4) */

    /* algoritmo de Howard Hinnant, 32-bit */
    {
        unsigned doe, yoe, doy, mp;
        int era, yy;
        int z = days + 719468;
        era = (z >= 0 ? z : z - 146096) / 146097;
        doe = (unsigned)(z - era * 146097);
        yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        yy = (int)yoe + era * 400;
        doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        mp = (5 * doy + 2) / 153;
        *d = (int)(doy - (153 * mp + 2) / 5 + 1);
        *mo = (int)(mp < 10 ? mp + 3 : mp - 9);
        *y = yy + (*mo <= 2);
    }
}

int app_date(int argc, char **argv)
{
    static const char *wdn[] = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
    static const char *mon[] = { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                 "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
    int t, y, mo, d, h, mi, s, wd;
    if (argc > 1 && strcmp(argv[1], "--help") == 0) {
        bb_out("date: uso: date [-u]\n");
        return 0;
    }
    t = bb_time();
    epoch_to_civil(t, &y, &mo, &d, &h, &mi, &s, &wd);
    printf("%s %s %d %d:%d:%d UTC %d\n", wdn[wd], mon[mo - 1], d, h, mi, s, y);
    return 0;
}