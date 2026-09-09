/* Build with:
 * ./build-elf.sh docs/ELF/example.c -stdlib -o example
 */
#include "../../res/lib/opentty.h"

int main(int argc, char **argv)
{
    char message[64];
    char *copy;

    snprintf(message, sizeof(message), "argc=%d", argc);
    copy = strdup(message);
    printf("OpenTTY ELF: %s\n", copy ? copy : "out of memory");
    free(copy);

    if (argc > 1) {
        printf("argument: %s\n", argv[1]);
    }
    return 0;
}
