/* abitest.c - guest-side test for the extended OpenTTY C ABI.
 *
 * Covers the string helpers, qsort/bsearch, the heap allocator and the LCDUI
 * widget layer added on top of res/lib/libc.s. Built by tests/java/build_rv.sh
 * and executed by tests/java/src/TestLibc.java on the real src/ELF.java.
 *
 * Output format: one "ok <name>" or "FAIL <name>" line per check, then
 * "abitest done, N failure(s)". The trailing lcdui_wait_event() call must
 * suspend the process with an empty event queue, so the last line printed is
 * "waiting for an event". */
#include "../../res/lib/opentty.h"
#include "../../res/lib/lcdui.h"

static int fails;

static void check(const char *name, int ok)
{
    if (!ok) { fails++; }
    printf("%s %s\n", ok ? "ok" : "FAIL", name);
}

static int cmp_int(const void *a, const void *b)
{
    int x = *(const int *)a, y = *(const int *)b;
    return x < y ? -1 : x > y ? 1 : 0;
}

struct pair { int key; int tag; };

static int cmp_pair(const void *a, const void *b)
{
    return ((const struct pair *)a)->key - ((const struct pair *)b)->key;
}

int main(int argc, char **argv)
{
    char line[200];
    char hay[32], path[32];

    /* ---- strings ---- */
    strcpy(hay, "hello world");
    strcpy(path, "/a/b/c.txt");
    check("strstr substring", strstr(hay, "world") == hay + 6);
    check("strstr empty needle", strstr(hay, "") == hay);
    check("strstr miss", strstr(hay, "z") == 0);
    check("strstr at start", strstr(hay, "hello") == hay);
    check("strrchr last dot", strrchr(path, '.') == path + 6);
    check("strrchr terminator", strrchr(path, 0) == path + strlen(path));
    check("strrchr miss", strrchr(path, 'z') == 0);
    check("strcasecmp equal", strcasecmp("Hello", "hELLO") == 0);
    check("strcasecmp order", strcasecmp("abc", "abd") < 0);
    check("strncasecmp prefix", strncasecmp("abcX", "ABCY", 3) == 0);
    check("strncasecmp full", strncasecmp("abcX", "ABCY", 4) != 0);

    {
        char *end = 0;
        check("strtol value", strtol("  -42xyz", &end, 10) == -42);
        check("strtol endptr", end != 0 && *end == 'x');
        check("strtol base 0 hex", strtol("0x1f", 0, 0) == 31);
        check("strtol base 0 octal", strtol("0755", 0, 0) == 493);
        check("strtol base 2", strtol("1011", 0, 2) == 11);
        check("strtol no digits", strtol("xyz", &end, 10) == 0 && end != 0 && *end == 'x');
        check("strtoul max", strtoul("4294967295", 0, 10) == 4294967295UL);
        check("strtoul hex prefix", strtoul("ff", 0, 16) == 255);
    }

    /* ---- qsort / bsearch ---- */
    {
        int a[10] = { 5, 3, 9, 1, 7, 2, 8, 6, 4, 0 };
        int i, sorted = 1, *hit, missing = 42;
        qsort(a, 10, sizeof(int), cmp_int);
        for (i = 1; i < 10; i++) { if (a[i - 1] > a[i]) { sorted = 0; } }
        check("qsort sorts ints", sorted && a[0] == 0 && a[9] == 9);
        hit = (int *)bsearch(&a[3], a, 10, sizeof(int), cmp_int);
        check("bsearch finds", hit != 0 && *hit == 3);
        check("bsearch miss", bsearch(&missing, a, 10, sizeof(int), cmp_int) == 0);
        {
            struct pair p[3] = { { 3, 30 }, { 1, 10 }, { 2, 20 } };
            qsort(p, 3, sizeof(struct pair), cmp_pair);
            check("qsort struct size", p[0].key == 1 && p[0].tag == 10 && p[2].key == 3 && p[2].tag == 30);
        }
    }

    /* ---- heap ---- */
    {
        char *p = (char *)malloc(1000);
        check("malloc", p != 0);
        if (p) {
            memset(p, 'x', 1000);
            check("malloc writable", p[0] == 'x' && p[999] == 'x');
            free(p);
        }
        {
            char *q = (char *)malloc(16);
            int ok = q != 0, i;
            for (i = 0; q && i < 16; i++) { q[i] = (char)('a' + i); }
            q = (char *)realloc(q, 4096);
            ok = ok && q != 0;
            for (i = 0; q && i < 16; i++) { if (q[i] != (char)('a' + i)) { ok = 0; } }
            check("realloc grow keeps data", ok);
            q = (char *)realloc(q, 8);
            check("realloc shrink keeps data", q != 0 && q[0] == 'a' && q[7] == 'h');
            free(q);
        }
        {
            char *a = (char *)malloc(64), *b = (char *)malloc(64), *c = (char *)malloc(64), *d;
            check("heap blocks", a && b && c && b == a + 72);
            free(b);
            free(a);
            d = (char *)malloc(120);
            check("free coalesces neighbours", d == a);
            free(c);
            free(d);
        }
        {
            char *e = (char *)malloc(32), *f;
            free(e);
            f = (char *)malloc(32);
            check("free returns top to the region", f == e);
            free(f);
        }
        {
            void *z = calloc(4, 16);
            check("calloc zeroes", z != 0 && ((char *)z)[0] == 0 && ((char *)z)[63] == 0);
            free(z);
        }
    }

    /* ---- LCDUI widgets ---- */
    {
        int form = lcdui_new(LCDUI_FORM, "Widgets", 0, 0);
        int gauge, spacer, choice, button, text, cmd, alert, list, item;
        check("form created", form > 0);
        check("ticker set", lcdui_set_ticker(form, "OpenTTY native widgets") == 0);

        gauge = lcdui_append_gauge(form, "Progress", 0, 100, 25);
        check("gauge append", gauge > 0);
        check("gauge initial value", lcdui_get_gauge(gauge) == 25);
        check("gauge set", lcdui_set_gauge(gauge, 70) == 0);
        check("gauge value after set", lcdui_get_gauge(gauge) == 70);
        check("gauge clamps", lcdui_set_gauge(gauge, 500) == 0 && lcdui_get_gauge(gauge) == 100);

        spacer = lcdui_append_spacer(form, 10, 20);
        check("spacer append", spacer > 0);

        choice = lcdui_append_choice(form, "Pick", LCDUI_CHOICE_EXCLUSIVE);
        check("choice append", choice > 0);
        lcdui_choice_append(choice, "one");
        lcdui_choice_append(choice, "two");
        lcdui_choice_append(choice, "three");
        check("choice count", lcdui_choice_count(choice) == 3);
        check("choice set selected", lcdui_choice_set_selected(choice, 1, 1) == 0);
        check("choice selected", lcdui_choice_selected(choice, 1) == 1);
        check("choice not selected", lcdui_choice_selected(choice, 0) == 0);

        button = lcdui_append_button(form, "Tap me");
        check("button append", button > 0);
        cmd = lcdui_command("Tap", LCDUI_COMMAND_ITEM, 1);
        check("item command", lcdui_item_command(button, cmd) == 0);
        check("default command", lcdui_set_default_command(button, cmd) == 0);

        text = lcdui_append_text(form, "Status", "Ready");
        check("text append", text > 0);
        check("font style", lcdui_set_font(text, "monospace bold") == 0);
        check("layout", lcdui_set_layout(text, LCDUI_LAYOUT_CENTER) == 0);
        check("get label", lcdui_get_label(text, line, sizeof(line)) == 6 && strcmp(line, "Status") == 0);

        check("form size", lcdui_size(form) == 5);
        item = lcdui_insert_text(form, 0, "Top", "inserted");
        check("insert text", item > 0 && lcdui_size(form) == 6);
        check("delete item", lcdui_delete(form, 0) == 0 && lcdui_size(form) == 5);

        check("display form", lcdui_display(form) == 0);
        check("get current", lcdui_get_current() == form);

        alert = lcdui_alert("Warning", "Careful now", LCDUI_ALERT_WARNING, 0);
        check("alert created", alert > 0);
        check("alert title", lcdui_set_title(alert, "Heads up") == 0);

        list = lcdui_new(LCDUI_LIST, "Items", 0, LCDUI_LIST_IMPLICIT);
        check("list created", list > 0);
        check("list append", lcdui_list_append(list, "first") == 0);
        lcdui_list_append(list, "second");
        check("list size", lcdui_size(list) == 2);
        check("list selected", lcdui_list_selected(list) == 0);
        check("list image append", lcdui_list_append_image(list, "third", "/nonexistent.png") == 2);

        check("vibrate", lcdui_vibrate(50) == 0);
        check("clear form", lcdui_clear(form) == 0 && lcdui_size(form) == 0);
        check("destroy alert", lcdui_destroy(alert) == 0);
        check("bad handle", lcdui_set_text(9999, "x") == -1);
    }

    printf("abitest done, %d failure(s)\n", fails);

    /* An empty queue must suspend the process instead of returning an event. */
    {
        struct lcdui_event event;
        printf("waiting for an event\n");
        while (lcdui_wait_event(&event) == 0) { }
        printf("unexpected event %d\n", event.type);
    }
    return fails == 0 ? 0 : 1;
}
