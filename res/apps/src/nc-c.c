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

static int show_error(const char *message, int code) {
    struct lcdui_event event;
    char text[96];
    int alert = lcdui_new(LCDUI_ALERT, "nc-c error", 0, 0);
    int close = lcdui_command("Close", LCDUI_COMMAND_EXIT, 1);
    snprintf(text, sizeof(text), "%s: %d", message, code);
    lcdui_append_text(alert, 0, text);
    lcdui_add_command(alert, close);
    lcdui_display(alert);
    while (1) {
        if (!lcdui_wait_event(&event)) continue;
        if (event.type == LCDUI_EVENT_COMMAND && event.command == close) return 1;
    }
}

int main(int argc, char **argv) {
    struct sockaddr_in peer; struct lcdui_event event; char line[257];
    int fd, form, output, input, send, tasks, quit, port, a, b, c, d, result;
    if (argc < 3) return show_error("usage: nc-c HOST PORT", argc);
    port = atoi(argv[2]);
    if (port < 1 || port > 65535) return show_error("invalid port", port);
    if (!parse_ipv4(argv[1], &a, &b, &c, &d)) return show_error("IPv4 address required", 2);
    sockaddr_in_init(&peer, port, a, b, c, d);
    fd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (fd < 0) return show_error("socket failed", fd);
    result = connect(fd, &peer, sizeof(peer));
    if (result < 0) { close(fd); return show_error("connect failed", result); }
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
