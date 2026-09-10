#include "../../lib/lcdui.h"

int main(void)
{
    struct lcdui_event event;
    int form = lcdui_new(LCDUI_FORM, "Hello", 0, 0);
    int name = lcdui_append_field(form, "Your name", "", 32, LCDUI_TEXT_ANY);
    int greet = lcdui_command("Say hello", LCDUI_COMMAND_OK, 1);
    int tasks = lcdui_command("Tasks", LCDUI_COMMAND_SCREEN, 2);

    opentty_setproc_name("hello");
    lcdui_append_text(form, 0, "What is your name?");
    lcdui_add_command(form, greet);
    lcdui_add_command(form, tasks);
    lcdui_display(form);

    while (1) {
        if (!lcdui_wait_event(&event)) { continue; }
        if (event.type != LCDUI_EVENT_COMMAND) { continue; }
        if (event.command == tasks) { graphics_taskmngr(); continue; }
        if (event.command == greet) {
            char value[33];
            int alert, close;
            lcdui_get_text(name, value, sizeof(value));
            alert = lcdui_new(LCDUI_ALERT, "Hello", value, 0);
            close = lcdui_command("Close", LCDUI_COMMAND_EXIT, 1);
            lcdui_add_command(alert, close);
            lcdui_display(alert);
            while (!lcdui_wait_event(&event)) { }
            if (event.type == LCDUI_EVENT_COMMAND && event.command == close) { return 0; }
        }
    }
}
