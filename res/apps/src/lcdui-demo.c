/* lcdui-demo.c - ELF LCDUI widget showcase.
 *
 * Exercises the guest widget ABI from res/lib/lcdui.h: a scrolling ticker, a
 * StringItem with a custom font and layout, a Gauge, a ChoiceGroup with item
 * state events, a button item with its own ITEM command, a spacer, a text
 * field and an alert with a type and a "next" screen.
 *
 * Build: ./build-elf.sh res/apps/src/lcdui-demo.c -stdlib -o res/apps/dist/lcdui-demo
 */
#include "../../lib/opentty.h"
#include "../../lib/lcdui.h"

static int form, status, gauge, choice, button, name;

int main(void)
{
    struct lcdui_event event;
    int ok, close, bump;
    char buffer[64];

    form = lcdui_new(LCDUI_FORM, "ELF widgets", 0, 0);
    lcdui_set_ticker(form, "OpenTTY native LCDUI: ticker, gauge, choice and alert");

    status = lcdui_append_text(form, "Status", "Ready");
    lcdui_set_font(status, "bold");
    lcdui_set_layout(status, LCDUI_LAYOUT_CENTER);

    gauge = lcdui_append_gauge(form, "Progress", 0, 100, 30);
    choice = lcdui_append_choice(form, "Mode", LCDUI_CHOICE_EXCLUSIVE);
    lcdui_choice_append(choice, "Simple");
    lcdui_choice_append(choice, "Detailed");
    lcdui_choice_set_selected(choice, 0, 1);

    button = lcdui_append_button(form, "Advance gauge");
    bump = lcdui_command("Advance", LCDUI_COMMAND_ITEM, 1);
    lcdui_set_default_command(button, bump);
    lcdui_item_command(button, bump);

    lcdui_append_spacer(form, 8, 8);
    name = lcdui_append_field(form, "Name", "OpenTTY", 32, LCDUI_TEXT_ANY);

    ok = lcdui_command("OK", LCDUI_COMMAND_OK, 1);
    close = lcdui_command("Close", LCDUI_COMMAND_EXIT, 2);
    lcdui_add_command(form, ok);
    lcdui_add_command(form, close);

    opentty_setproc("name", "lcdui-demo");
    lcdui_display(form);

    for (;;) {
        if (!lcdui_wait_event(&event)) { continue; }
        if (event.type == LCDUI_EVENT_ITEM_COMMAND && event.command == bump) {
            int value = lcdui_get_gauge(gauge) + 10;
            if (value > 100) { value = 0; }
            lcdui_set_gauge(gauge, value);
            snprintf(buffer, sizeof(buffer), "Gauge at %d", value);
            lcdui_set_text(status, buffer);
        } else if (event.type == LCDUI_EVENT_ITEM_STATE && event.screen == choice) {
            lcdui_set_text(status, event.index == 1 ? "Detailed mode" : "Simple mode");
        } else if (event.type == LCDUI_EVENT_COMMAND && event.command == ok) {
            int alert;
            lcdui_get_text(name, buffer, sizeof(buffer));
            alert = lcdui_alert("Hello", buffer, LCDUI_ALERT_INFO, 0);
            lcdui_add_command(alert, lcdui_command("Back", LCDUI_COMMAND_BACK, 1));
            lcdui_show_alert(alert, form);
        } else if (event.type == LCDUI_EVENT_COMMAND && event.command == close) {
            printf("lcdui-demo closed\n");
            return 0;
        }
    }
}
