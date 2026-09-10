#include "../../lib/lcdui.h"

int printf(const char *fmt, ...);

int main(void)
{
    struct lcdui_event event;
    int form = lcdui_new(LCDUI_FORM, "ELF LCDUI", 0, 0);
    int field = lcdui_append_field(form, "Name", "OpenTTY", 32, LCDUI_TEXT_ANY);
    int ok = lcdui_command("OK", LCDUI_COMMAND_OK, 1);

    lcdui_append_text(form, 0, "RISC-V LCDUI form");
    lcdui_add_command(form, ok);
    lcdui_display(form);

    while (1) {
        if (!lcdui_wait_event(&event)) { continue; }
        if (event.type == LCDUI_EVENT_COMMAND && event.command == ok) {
            char name[33];
            lcdui_get_text(field, name, sizeof(name));
            printf("LCDUI OK: %s\n", name);
            return 0;
        }
    }
}
