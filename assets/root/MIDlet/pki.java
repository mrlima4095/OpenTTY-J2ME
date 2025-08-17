// |
        // Certificates
        else if (mainCommand.equals("pki")) {
            if (argument.equals("")) { }
            else {
                String content = getcontent(argument);
                if (content.equals("")) { }
                else {
                    int count = 0, idx = 0;
                    while (true) {
                        int beg = content.indexOf("-----BEGIN CERTIFICATE-----", idx);
                        if (beg < 0) { break; }
                        int end = content.indexOf("-----END CERTIFICATE-----", beg);
                        if (end < 0) { break; }

                        String base64 = content.substring(beg + "-----BEGIN CERTIFICATE-----".length(), end);
                        StringBuffer sbNoNl = new StringBuffer();
                        for (int i = 0; i < base64.length(); i++) {
                            char c = base64.charAt(i);
                            if (c!='\r' && c!='\n') sbNoNl.append(c);
                        }
                        String b64 = sbNoNl.toString().trim();

                        final String B64CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
                        int[] T = new int[256]; for (int i=0;i<256;i++) T[i] = -1;
                        for (int i=0;i<B64CHARS.length();i++) T[B64CHARS.charAt(i)] = i;
                        T['='] = -2;
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                        int val = 0, valb = -8;
                        for (int i=0;i<b64.length();i++) {
                            int ch = b64.charAt(i);
                            if (ch==' '||ch=='\t') continue;
                            if (ch=='\r'||ch=='\n') continue;
                            int d = (ch<256)?T[ch]:-1;
                            if (d == -1) continue;
                            if (d == -2) break;
                            val = (val << 6) + d;
                            valb += 6;
                            if (valb >= 0) {
                                baos.write((val >> valb) & 0xFF);
                                valb -= 8;
                            }
                        }
                        byte[] der = baos.toByteArray();

                        count++;
                        echoCommand("cert#" + count + ": DER " + der.length + " bytes");
                        idx = end + "-----END CERTIFICATE-----".length();
                    }

                    if (count == 0) { echoCommand("pki: " + argument + ": don't have PEM certs"); }
                }
            }       
        }
