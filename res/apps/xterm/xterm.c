#include "../../lib/lcdui.h"
#include "../../lib/opentty.h"

static void read_text(const char *path, char *buffer, int size)
{
    int fd = open(path, O_RDONLY, 0);
    int count = fd < 0 ? -1 : read(fd, buffer, size - 1);
    if (fd >= 0) { close(fd); }
    buffer[count < 0 ? 0 : count] = 0;
    while (count > 0 && (buffer[count - 1] == '\n' || buffer[count - 1] == '\r')) { buffer[--count] = 0; }
}

static void prompt(int field)
{
    char user[64], hostname[64], pwd[128], label[280];
    opentty_getenv("USER", user, sizeof(user));
    opentty_getenv("PWD", pwd, sizeof(pwd));
    read_text("/etc/hostname", hostname, sizeof(hostname));
    snprintf(label, sizeof(label), "[%s@%s %s] %s%s", user, hostname, pwd,
        strcmp(user, "root") == 0 ? "#" : "$", strcmp(user, "root") == 0 ? " (root)" : "");
    lcdui_set_label(field, label);
}

int main(void)
{
    struct lcdui_event event;
    int terminal = lcdui_new(LCDUI_FORM, "Terminal", 0, 0);
    char motd[512];
    read_text("/etc/motd", motd, sizeof(motd));
    opentty_expand_env(motd, motd, sizeof(motd));
    int output = lcdui_append_text(terminal, 0, motd);
    int input = lcdui_append_field(terminal, "Command", "", 256, LCDUI_TEXT_ANY);
    int run = lcdui_command("Run", LCDUI_COMMAND_OK, 1);
    int tasks = lcdui_command("Switch to...", LCDUI_COMMAND_SCREEN, 2);

    opentty_setproc("name", "xterm");
    opentty_setproc("stdout", output);
    opentty_setproc("screen", terminal);
    prompt(input);
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
        prompt(input);
    }
}
