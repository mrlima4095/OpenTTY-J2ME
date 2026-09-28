/* Build: ./build-elf.sh res/apps/src/nc-c.c -stdlib -o res/apps/dist/nc-c */
#include "../../lib/opentty.h"
#include "../../lib/lcdui.h"

static int parse_ipv4(const char *text, int *a, int *b, int *c, int *d) {
    int part = 0, value = 0, count = 0;
    int values[4];
    while (1) {
        char ch = text[count++];
        if (ch >= '0' && ch <= '9') { value = value * 10 + ch - '0'; if (value > 255) return 0; }
        else if (ch == '.' || ch == 0) {
            if (part >= 4) return 0;
            values[part++] = value; value = 0;
            if (ch == 0) break;
        } else return 0;
    }
    if (part != 4) return 0;
    *a = values[0]; *b = values[1]; *c = values[2]; *d = values[3];
    return 1;
}

int main(int argc, char **argv) {
    struct sockaddr_in peer; struct lcdui_event event; char line[257];
    int fd, form, output, input, send, tasks, quit, port, a, b, c, d, result;
    if (argc < 3) { printf("usage: nc-c HOST PORT\n"); return 2; }
    port = atoi(argv[2]);
    if (!parse_ipv4(argv[1], &a, &b, &c, &d)) { printf("nc-c: IPv4 address required\n"); return 2; }
    sockaddr_in_init(&peer, port, a, b, c, d);
    fd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (fd < 0) { printf("nc-c: socket failed: %d\n", fd); return 1; }
    result = connect(fd, &peer, sizeof(peer));
    if (result < 0) { printf("nc-c: connect failed: %d\n", result); close(fd); return 1; }
    form = lcdui_new(LCDUI_FORM, "nc-c", 0, 0);
    output = lcdui_append_text(form, 0, "[nc-c] Connected.\n");
    input = lcdui_append_field(form, "Remote >", "", 256, LCDUI_TEXT_ANY);
    send = lcdui_command("Send", LCDUI_COMMAND_OK, 1); tasks = lcdui_command("Switch to...", LCDUI_COMMAND_SCREEN, 2); quit = lcdui_command("Disconnect", LCDUI_COMMAND_EXIT, 1);
    lcdui_add_command(form, send); lcdui_add_command(form, tasks); lcdui_add_command(form, quit);
    opentty_setproc("name", "nc-c"); lcdui_display(form); opentty_socket_reader_start(fd, output);
    while (1) {
        if (!lcdui_wait_event(&event)) continue;
        if (event.type != LCDUI_EVENT_COMMAND) continue;
        if (event.command == tasks) { graphics_taskmngr(); continue; }
        if (event.command == quit) break;
        if (event.command == send && lcdui_get_text(input, line, sizeof(line)) > 0) {
            sendto(fd, line, strlen(line), 0, &peer, sizeof(peer));
            sendto(fd, "\n", 1, 0, &peer, sizeof(peer));
            lcdui_set_text(input, "");
        }
    }
    opentty_socket_reader_stop(fd); close(fd); return 0;
}
