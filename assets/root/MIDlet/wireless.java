else if (mainCommand.equals("send")) {
    String[] args = split(argument, ' ');
    if (args.length < 2) { echoCommand("wrl: missing..."); return 2; }

    String address = args[0];
    String msg = argument.substring(argument.indexOf(' ') + 1).trim();
    try {
        MessageConnection conn = (MessageConnection) Connector.open(address);
        TextMessage message = (TextMessage) conn.newMessage(MessageConnection.TEXT_MESSAGE);
        message.setPayloadText(msg);
        conn.send(message);
        conn.close();
        echoCommand("wrl: message sent to '" + address + "'");
    } catch (Exception e) {
        echoCommand(e.toString());
        return 1;
    }
} 
else if (mainCommand.equals("listen")) {
    String port = argument;
    MessageConnection conn = null;
    try {
        conn = (MessageConnection) Connector.open("sms://:" + port);
        echoCommand("[+] listening at port " + port); MIDletLogs("add info Server listening at port " + port);
        start("wireless");
        try {
            while (trace.containsKey("wireless")) {
                Message msg = conn.receive();
                String sender = "unknown";
                if (msg instanceof TextMessage) {
                    TextMessage tmsg = (TextMessage) msg;
                    try { sender = tmsg.getAddress(); } catch (Exception ex) { }
                    String payload = tmsg.getPayloadText();
                    echoCommand("[+] " + sender + " -> " + payload);
                } else {
                    echoCommand("[+] " + sender + " -> binary payload.");
                }
            }
        } catch (Exception e) { echoCommand("[-] " + e.toString()); stop("wireless"); }
    } catch (Exception e) { echoCommand("[-] " + e.toString()); MIDletLogs("add info Server crashed '" + port + "'"); } 
    finally {
        if (conn != null) { try { conn.close(); } catch (IOException e) { } }
        echoCommand("[-] Server stopped");
        MIDletLogs("add info Server was stopped");
    }
}