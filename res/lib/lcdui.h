#ifndef OPENTTY_LCDUI_H
#define OPENTTY_LCDUI_H

/* LCDUI guest ABI for ELF RV32IM apps. Handles are positive integers.
 *
 * This is the ELF counterpart of the Lua `graphics` package: screens, alerts
 * with a type, tickers, gauges, spacers, images, choice groups, button items,
 * item commands and item state events all exist here too. */

#define LCDUI_FORM 1
#define LCDUI_LIST 2
#define LCDUI_TEXTBOX 3
#define LCDUI_ALERT 4

#define LCDUI_LIST_IMPLICIT 0
#define LCDUI_LIST_EXCLUSIVE 1
#define LCDUI_LIST_MULTIPLE 2

#define LCDUI_TEXT_ANY 0
#define LCDUI_TEXT_PASSWORD 1

/* Alert types for lcdui_alert() and the `mode` argument of lcdui_new(). */
#define LCDUI_ALERT_INFO 0
#define LCDUI_ALERT_WARNING 1
#define LCDUI_ALERT_ERROR 2
#define LCDUI_ALERT_ALARM 3
#define LCDUI_ALERT_CONFIRMATION 4

/* ChoiceGroup layout for lcdui_append_choice(). */
#define LCDUI_CHOICE_EXCLUSIVE 0
#define LCDUI_CHOICE_MULTIPLE 1
#define LCDUI_CHOICE_POPUP 2

/* Item layout bits for lcdui_set_layout(), matching MIDP Item.LAYOUT_*. */
#define LCDUI_LAYOUT_DEFAULT 0
#define LCDUI_LAYOUT_LEFT 1
#define LCDUI_LAYOUT_RIGHT 2
#define LCDUI_LAYOUT_CENTER 3
#define LCDUI_LAYOUT_VCENTER 16
#define LCDUI_LAYOUT_BOTTOM 32
#define LCDUI_LAYOUT_TOP 64
#define LCDUI_LAYOUT_NEWLINE_BEFORE 256
#define LCDUI_LAYOUT_NEWLINE_AFTER 512
#define LCDUI_LAYOUT_EXPAND 2048
#define LCDUI_LAYOUT_VEXPAND 4096

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
#define LCDUI_EVENT_ITEM_COMMAND 3
#define LCDUI_EVENT_ITEM_STATE 4

struct lcdui_event {
    int type;     /* LCDUI_EVENT_* */
    int screen;   /* Displayable handle, or the Item handle for item events */
    int command;  /* Command handle, 0 for list selection / item state */
    int index;    /* selected index, or -1 when the event has none */
};

/* Screens. `mode` selects the list style, the TextBox mode, or the alert type;
 * lcdui_alert() is the explicit alert constructor with a timeout in ms
 * (pass 0 for a permanent alert). */
int lcdui_new(int kind, const char *title, const char *content, int mode);
int lcdui_alert(const char *title, const char *text, int type, int timeout);

/* Form items. Every append returns an Item handle, or -1. */
int lcdui_append_text(int form, const char *label, const char *text);
int lcdui_append_field(int form, const char *label, const char *value, int max_length, int mode);
int lcdui_append_gauge(int form, const char *label, int interactive, int max_value, int value);
int lcdui_append_spacer(int form, int width, int height);
int lcdui_append_image(int form, const char *path);
int lcdui_append_choice(int form, const char *label, int mode);
int lcdui_append_button(int form, const char *label);

/* Lists. */
int lcdui_list_append(int list, const char *text);
int lcdui_list_append_image(int list, const char *text, const char *path);
int lcdui_list_selected(int list);

/* Gauge and choice accessors. */
int lcdui_set_gauge(int gauge, int value);
int lcdui_get_gauge(int gauge);
int lcdui_choice_append(int choice, const char *text);
int lcdui_choice_count(int choice);
int lcdui_choice_selected(int choice, int index);
int lcdui_choice_set_selected(int choice, int index, int selected);

/* Item text, label, font and layout. */
int lcdui_set_text(int item, const char *text);
int lcdui_get_text(int item, char *buffer, int size);
int lcdui_set_label(int item, const char *label);
int lcdui_get_label(int item, char *buffer, int size);
/* Fonts: MIDP only supports them on StringItem (text/button items). */
int lcdui_set_font(int item, const char *style);
int lcdui_set_layout(int item, int layout);

/* Form/List/ChoiceGroup size and element removal. */
int lcdui_size(int handle);
int lcdui_delete(int handle, int index);
int lcdui_insert_text(int form, int index, const char *label, const char *text);
int lcdui_clear(int screen);

/* Commands, tickers and screens. */
int lcdui_command(const char *label, int type, int priority);
int lcdui_add_command(int screen, int command);
int lcdui_item_command(int item, int command);
int lcdui_set_default_command(int item, int command);
int lcdui_set_ticker(int screen, const char *text);
int lcdui_set_title(int screen, const char *title);
int lcdui_display(int screen);
int lcdui_show_alert(int alert, int next);
int lcdui_get_current(void);
int lcdui_vibrate(int millis);

/* Returns 1 after filling event. With no event it suspends this ELF process;
 * call it again after resumption. */
int lcdui_wait_event(struct lcdui_event *event);
int lcdui_destroy(int handle);

/* ELF equivalents for graphics.taskmngr() and os.setproc(...). Displaying a
 * screen registers it automatically. `key` supports name, screen, cmd and
 * string values stored in the process database; pass 0 to remove a db key. */
int graphics_taskmngr(void);
int opentty_setproc(const char *key, ...);

#endif
