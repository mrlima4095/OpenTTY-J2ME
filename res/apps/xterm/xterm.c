#include "../../lib/lcdui.h"
#include "../../lib/opentty.h"

int main(void)
{
    struct lcdui_event event;
    int terminal = lcdui_new(LCDUI_FORM, "OpenTTY xterm", 0, 0);
    int output = lcdui_append_text(terminal, 0, "OpenTTY ELF xterm\n");
    int input = lcdui_append_field(terminal, "Command", "", 256, LCDUI_TEXT_ANY);
    int run = lcdui_command("Run", LCDUI_COMMAND_OK, 1);
    int tasks = lcdui_command("Switch to...", LCDUI_COMMAND_SCREEN, 2);

    opentty_setproc("name", "xterm");
    opentty_setproc("stdout", output);
    opentty_setproc("screen", terminal);
    lcdui_add_command(terminal, run);
    lcdui_add_command(terminal, tasks);
    lcdui_display(terminal);

    while (1) {
        char command[257];
        if (!lcdui_wait_event(&event)) { continue; }
        if (event.type != LCDUI_EVENT_COMMAND) { continue; }
        if (event.command == tasks) { graphics_taskmngr(); continue; }
        if (event.command != run) { continue; }
        lcdui_get_text(input, command, sizeof(command));
        if (command[0] == 0) { continue; }
        lcdui_set_text(input, "");
        opentty_shell(command);
    }
}
