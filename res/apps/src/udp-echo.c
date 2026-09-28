/* UDP echo server for OpenTTY.
 * Build: ./build-elf.sh res/apps/src/udp-echo.c -lib -o res/apps/dist/udp-echo
 * Run:   /mnt/opentty/udp-echo
 */
#include "../../lib/opentty.h"

int main(void)
{
    char buffer[256];
    struct sockaddr_in peer;
    int peer_length = sizeof(peer);
    int fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    int received;

    if (fd < 0) { printf("udp-echo: socket failed: %d\n", fd); return 1; }
    sockaddr_in_init(&peer, 8080, 0, 0, 0, 0);
    if (bind(fd, &peer, sizeof(peer)) < 0) { printf("udp-echo: bind failed\n"); close(fd); return 1; }

    printf("udp-echo: listening on UDP 8080\n");
    received = recvfrom(fd, buffer, sizeof(buffer), 0, &peer, &peer_length);
    if (received > 0) { sendto(fd, buffer, received, 0, &peer, peer_length); }
    printf("udp-echo: echoed %d bytes\n", received);
    close(fd);
    return received < 0;
}
