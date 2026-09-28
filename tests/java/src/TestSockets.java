import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.Hashtable;

/** Exercises the ELF network syscall handlers against localhost. */
public class TestSockets {
    private static final int A0 = 10, A1 = 11, A2 = 12, A3 = 13, A4 = 14, A5 = 15;
    private static int passes;

    public static void main(String[] args) throws Exception {
        testUdp();
        testTcp();
        System.out.println("TestSockets: " + passes + " passed");
    }

    private static ELF elf() {
        Hashtable scope = new Hashtable();
        scope.put("USER", "root");
        TestELF.FakeTTY mid = new TestELF.FakeTTY();
        mid.sys.put("900", new java.util.Vector());
        return new ELF(mid, new Hashtable(), new StringBuffer(), scope, 1000, "900", null);
    }

    private static void testUdp() throws Exception {
        ELF e = elf();
        int port = freePort();
        int server = socket(e, 2, 17), client = socket(e, 2, 17);
        byte[] mem = memory(e);
        putInt(mem, 420, 1);
        stack(e, 4, 0);
        call(e, "handleSetsockopt", client, 1, 2, 420);
        check(result(e) == 0, "setsockopt");
        stack(e, 4, 0);
        call(e, "handleGetsockopt", client, 1, 2, 424);
        check(result(e) == 0 && mem[424] == 1, "getsockopt");
        bind(e, server, port);
        putSockaddr(mem, 128, "127.0.0.1", port);
        mem[256] = 'u'; mem[257] = 'd'; mem[258] = 'p';
        stack(e, 128, 16);
        call(e, "handleSendto", client, 256, 3, 0);
        check(result(e) == 3, "UDP sendto");
        stack(e, 384, 400);
        call(e, "handleRecvfrom", server, 300, 16, 0);
        check(result(e) == 3 && mem[300] == 'u' && mem[301] == 'd' && mem[302] == 'p', "UDP recvfrom");
        check((mem[400] & 0xff) == 16 && mem[401] == 0 && mem[402] == 0 && mem[403] == 0, "UDP source sockaddr length");
        close(e, server); close(e, client);
    }

    private static void testTcp() throws Exception {
        ELF e = elf();
        int port = freePort();
        int server = socket(e, 1, 6), client = socket(e, 1, 6);
        bind(e, server, port);
        call(e, "handleListen", server, 1, 0, 0);
        check(result(e) == 0, "TCP listen");
        byte[] mem = memory(e);
        putSockaddr(mem, 128, "127.0.0.1", port);
        call(e, "handleConnect", client, 128, 16, 0);
        check(result(e) == 0, "TCP connect");
        call(e, "handleAccept", server, 160, 180, 0);
        int accepted = result(e);
        check(accepted >= 3, "TCP accept");
        mem[256] = 't'; mem[257] = 'c'; mem[258] = 'p';
        stack(e, 0, 0);
        call(e, "handleSendto", client, 256, 3, 0);
        check(result(e) == 3, "TCP sendto");
        stack(e, 0, 0);
        call(e, "handleRecvfrom", accepted, 300, 16, 0);
        check(result(e) == 3 && mem[300] == 't' && mem[301] == 'c' && mem[302] == 'p', "TCP recvfrom");
        close(e, accepted); close(e, client); close(e, server);
    }

    private static int socket(ELF e, int type, int protocol) throws Exception {
        call(e, "handleSocket", 2, type, protocol, 0);
        int fd = result(e);
        check(fd >= 3, "socket creation");
        return fd;
    }

    private static void bind(ELF e, int fd, int port) throws Exception {
        putSockaddr(memory(e), 64, "127.0.0.1", port);
        call(e, "handleBind", fd, 64, 16, 0);
        check(result(e) == 0, "bind");
    }

    private static void close(ELF e, int fd) throws Exception { call(e, "handleClose", fd, 0, 0, 0); check(result(e) == 0, "close"); }

    private static void call(ELF e, String name, int a0, int a1, int a2, int a3) throws Exception {
        int[] r = registers(e);
        r[A0] = a0; r[A1] = a1; r[A2] = a2; r[A3] = a3;
        Method method = ELF.class.getDeclaredMethod(name, new Class[0]);
        method.setAccessible(true);
        method.invoke(e, new Object[0]);
    }

    private static void stack(ELF e, int fourth, int fifth) throws Exception {
        int[] r = registers(e);
        r[A4] = fourth; r[A5] = fifth;
    }

    private static int result(ELF e) throws Exception { return registers(e)[A0]; }
    private static int[] registers(ELF e) throws Exception { return (int[]) field(e, "registers"); }
    private static byte[] memory(ELF e) throws Exception { return (byte[]) field(e, "memory"); }
    private static Object field(ELF e, String name) throws Exception { Field f = ELF.class.getDeclaredField(name); f.setAccessible(true); return f.get(e); }

    private static void putSockaddr(byte[] m, int at, String ip, int port) {
        m[at] = 2; m[at + 1] = 0;
        m[at + 2] = (byte) (port >>> 8); m[at + 3] = (byte) port;
        String[] parts = ip.split("\\.");
        for (int i = 0; i < 4; i++) { m[at + 4 + i] = (byte) Integer.parseInt(parts[i]); }
    }

    private static void putInt(byte[] m, int at, int value) { m[at] = (byte) value; m[at + 1] = (byte) (value >>> 8); m[at + 2] = (byte) (value >>> 16); m[at + 3] = (byte) (value >>> 24); }
    private static int freePort() throws Exception { ServerSocket s = new ServerSocket(0); int port = s.getLocalPort(); s.close(); return port; }
    private static void check(boolean condition, String name) { if (!condition) { throw new RuntimeException("FAIL " + name); } passes++; System.out.println("PASS " + name); }
}
