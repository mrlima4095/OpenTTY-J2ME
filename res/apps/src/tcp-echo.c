/* TCP echo server for OpenTTY.
 * Build: ./build-elf.sh res/apps/src/tcp-echo.c -lib -o res/apps/dist/tcp-echo
 * Run:   /mnt/opentty/tcp-echo
 */
#include "../../lib/opentty.h"

int main(void)
{
    char buffer[256];
    struct sockaddr_in local;
    struct sockaddr_in peer;
    int one = 1;
    int peer_length = sizeof(peer);
    int server = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    int client;
    int received;

    if (server < 0) { printf("tcp-echo: socket failed: %d\n", server); return 1; }
    setsockopt(server, SOL_SOCKET, SO_REUSEADDR, &one, sizeof(one));
    sockaddr_in_init(&local, 8081, 0, 0, 0, 0);
    if (bind(server, &local, sizeof(local)) < 0 || listen(server, 1) < 0) {
        printf("tcp-echo: bind or listen failed\n"); close(server); return 1;
    }

    printf("tcp-echo: listening on TCP 8081\n");
    client = accept(server, &peer, &peer_length);
    if (client < 0) { printf("tcp-echo: accept failed\n"); close(server); return 1; }
    received = recvfrom(client, buffer, sizeof(buffer), 0, &peer, &peer_length);
    if (received > 0) { sendto(client, buffer, received, 0, &peer, peer_length); }
    printf("tcp-echo: echoed %d bytes\n", received);
    close(client);
    close(server);
    return received < 0;
}
