import java.io.FileInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Hashtable;

/** Runs the compiled C socket examples against localhost clients. */
public class TestSocketApps {
    public static void main(String[] args) throws Exception {
        testUdpEcho();
        testTcpEcho();
        System.out.println("TestSocketApps: passed");
    }

    private static void testUdpEcho() throws Exception {
        Thread app = start("udp-echo");
        Thread.sleep(150);
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(3000);
        byte[] sent = "udp-c".getBytes("US-ASCII");
        socket.send(new DatagramPacket(sent, sent.length, InetAddress.getByName("127.0.0.1"), 18080));
        byte[] received = new byte[32];
        DatagramPacket reply = new DatagramPacket(received, received.length);
        socket.receive(reply);
        socket.close();
        check(new String(reply.getData(), 0, reply.getLength(), "US-ASCII").equals("udp-c"), "UDP C echo");
        app.join(3000);
        check(!app.isAlive(), "UDP C app exit");
    }

    private static void testTcpEcho() throws Exception {
        Thread app = start("tcp-echo");
        Thread.sleep(150);
        Socket socket = new Socket("127.0.0.1", 18081);
        socket.setSoTimeout(3000);
        socket.getOutputStream().write("tcp-c".getBytes("US-ASCII"));
        byte[] received = new byte[32];
        int count = socket.getInputStream().read(received);
        socket.close();
        check(count == 5 && new String(received, 0, count, "US-ASCII").equals("tcp-c"), "TCP C echo");
        app.join(3000);
        check(!app.isAlive(), "TCP C app exit");
    }

    private static Thread start(String appName) throws Exception {
        final ELF elf = elf(appName);
        Thread thread = new Thread(new Runnable() { public void run() { elf.run(); } }, "TestSocketApps");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static ELF elf(String appName) throws Exception {
        Hashtable scope = new Hashtable();
        scope.put("USER", "root");
        TestELF.FakeTTY mid = new TestELF.FakeTTY();
        mid.sys.put("900", new java.util.Vector());
        ELF elf = new ELF(mid, new Hashtable(), new StringBuffer(), scope, 1000, "900", null);
        String path = System.getProperty("opentty.repo", ".") + "/res/apps/dist/" + appName;
        InputStream input = new FileInputStream(path);
        if (!elf.load(input)) { throw new RuntimeException("could not load " + appName); }
        return elf;
    }

    private static void check(boolean condition, String name) {
        if (!condition) { throw new RuntimeException("FAIL " + name); }
        System.out.println("PASS " + name);
    }
}
