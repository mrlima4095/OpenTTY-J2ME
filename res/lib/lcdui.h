#ifndef OPENTTY_LCDUI_H
#define OPENTTY_LCDUI_H

/* LCDUI guest ABI for ELF RV32IM apps. Handles are positive integers. */

#define LCDUI_FORM 1
#define LCDUI_LIST 2
#define LCDUI_TEXTBOX 3
#define LCDUI_ALERT 4

#define LCDUI_LIST_IMPLICIT 0
#define LCDUI_LIST_EXCLUSIVE 1
#define LCDUI_LIST_MULTIPLE 2

#define LCDUI_TEXT_ANY 0
#define LCDUI_TEXT_PASSWORD 1

#define LCDUI_COMMAND_BACK 1
#define LCDUI_COMMAND_OK 2
#define LCDUI_COMMAND_CANCEL 3
#define LCDUI_COMMAND_HELP 4
#define LCDUI_COMMAND_STOP 5
#define LCDUI_COMMAND_EXIT 6
#define LCDUI_COMMAND_ITEM 7
#define LCDUI_COMMAND_SCREEN 8

#define LCDUI_EVENT_COMMAND 1
#define LCDUI_EVENT_LIST_SELECT 2

struct lcdui_event {
    int type;
    int screen;
    int command;
    int index;
};

int lcdui_new(int kind, const char *title, const char *content, int mode);
int lcdui_append_text(int form, const char *label, const char *text);
int lcdui_append_field(int form, const char *label, const char *value, int max_length, int mode);
int lcdui_list_append(int list, const char *text);
int lcdui_command(const char *label, int type, int priority);
int lcdui_add_command(int screen, int command);
int lcdui_display(int screen);
int lcdui_set_text(int item, const char *text);
int lcdui_get_text(int item, char *buffer, int size);
int lcdui_set_title(int screen, const char *title);
int lcdui_clear(int screen);

/* Returns 1 after filling event. With no event it suspends this ELF process;
 * call it again after resumption. */
int lcdui_wait_event(struct lcdui_event *event);
int lcdui_destroy(int handle);

/* ELF equivalents for graphics.taskmngr() and os.setproc(...). Displaying a
 * screen registers it automatically; opentty_setproc_screen is for a screen
 * that must be registered before it is displayed. */
int graphics_taskmngr(void);
int opentty_setproc_name(const char *name);
int opentty_setproc_screen(int screen);

#endif
