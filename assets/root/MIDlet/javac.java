private byte[] generateClass(String code) {
    String className = extractClassName(code);
    String[] imports = extractImports(code);
    String mnemonics = extractMnemonics(code);

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    try {
        byte[] nameBytes = className.getBytes();
        int nameLen = nameBytes.length;
        byte[] codeBytes = mnemonicsToBytes(mnemonics);
        int codeLen = codeBytes.length;
        int codeAttrLen = 12 + codeLen;

        int constantPoolSize = 11 + imports.length * 2;
        int cpCount = constantPoolSize;

        // Header
        out.write(0xCA); out.write(0xFE); out.write(0xBA); out.write(0xBE);
        out.write(0x00); out.write(0x00); out.write(0x00); out.write(0x2E); // Java 1.6

        out.write(0x00); out.write(cpCount);

        // Constant Pool Entries
        // #1 Class (this)
        out.write(0x07); out.write(0x00); out.write(0x02);
        // #2 UTF8 (class name)
        out.write(0x01); out.write((nameLen >> 8) & 0xFF); out.write(nameLen & 0xFF);
        for (int i = 0; i < nameLen; i++) out.write(nameBytes[i]);

        // #3 Class java/lang/Object
        out.write(0x07); out.write(0x00); out.write(0x04);
        // #4 UTF8 java/lang/Object
        byte[] obj = "java/lang/Object".getBytes();
        out.write(0x01); out.write(0x00); out.write(obj.length);
        for (int i = 0; i < obj.length; i++) out.write(obj[i]);

        // #5 UTF8 <init>
        byte[] init = "<init>".getBytes();
        out.write(0x01); out.write(0x00); out.write(init.length);
        for (int i = 0; i < init.length; i++) out.write(init[i]);

        // #6 UTF8 ()V
        byte[] desc = "()V".getBytes();
        out.write(0x01); out.write(0x00); out.write(desc.length);
        for (int i = 0; i < desc.length; i++) out.write(desc[i]);

        // #7 UTF8 Code
        byte[] codeStr = "Code".getBytes();
        out.write(0x01); out.write(0x00); out.write(codeStr.length);
        for (int i = 0; i < codeStr.length; i++) out.write(codeStr[i]);

        // #8 Methodref Object.<init>()
        out.write(0x0A); out.write(0x00); out.write(0x03); out.write(0x00); out.write(0x09);
        // #9 NameAndType <init>()V
        out.write(0x0C); out.write(0x00); out.write(0x05); out.write(0x00); out.write(0x06);

        // #10 UTF8 main
        byte[] main = "main".getBytes();
        out.write(0x01); out.write(0x00); out.write(main.length);
        for (int i = 0; i < main.length; i++) out.write(main[i]);

        // Add UTF8s for each import (constant pool only — not referenced yet)
        for (int i = 0; i < imports.length; i++) {
            byte[] b = imports[i].getBytes();
            out.write(0x01); out.write((b.length >> 8) & 0xFF); out.write(b.length & 0xFF);
            for (int j = 0; j < b.length; j++) out.write(b[j]);
        }

        // Access, This, Super
        out.write(0x00); out.write(0x21); // public super
        out.write(0x00); out.write(0x01); // this
        out.write(0x00); out.write(0x03); // super = java/lang/Object

        // interfaces, fields
        out.write(0x00); out.write(0x00);
        out.write(0x00); out.write(0x00);

        // methods_count = 2
        out.write(0x00); out.write(0x02);

        // <init>
        out.write(0x00); out.write(0x01); // access_flags
        out.write(0x00); out.write(0x05); // name_index
        out.write(0x00); out.write(0x06); // descriptor_index
        out.write(0x00); out.write(0x01); // attributes_count
        out.write(0x00); out.write(0x07); // Code attr index
        out.write(0x00); out.write(0x00); out.write(0x00); out.write(0x11); // attr length
        out.write(0x00); out.write(0x01); // max_stack
        out.write(0x00); out.write(0x01); // max_locals
        out.write(0x00); out.write(0x00); out.write(0x00); out.write(0x05); // code length
        out.write(0x2A); // aload_0
        out.write(0xB7); // invokespecial
        out.write(0x00); out.write(0x08); // #8 Methodref
        out.write(0xB1); // return
        out.write(0x00); out.write(0x00); // exception_table_length
        out.write(0x00); out.write(0x00); // attributes_count

        // main()
        out.write(0x00); out.write(0x09); // access_flags: public static
        out.write(0x00); out.write(0x0A); // name_index: main
        out.write(0x00); out.write(0x06); // descriptor_index: ()V
        out.write(0x00); out.write(0x01); // attributes_count
        out.write(0x00); out.write(0x07); // Code attr index

        out.write((codeAttrLen >> 24) & 0xFF);
        out.write((codeAttrLen >> 16) & 0xFF);
        out.write((codeAttrLen >> 8) & 0xFF);
        out.write(codeAttrLen & 0xFF);

        out.write(0x00); out.write(0x02); // max_stack (ajustado)
        out.write(0x00); out.write(0x01); // max_locals

        out.write((codeLen >> 24) & 0xFF);
        out.write((codeLen >> 16) & 0xFF);
        out.write((codeLen >> 8) & 0xFF);
        out.write(codeLen & 0xFF);
        for (int i = 0; i < codeLen; i++) out.write(codeBytes[i]);

        out.write(0x00); out.write(0x00); // exception_table_length
        out.write(0x00); out.write(0x00); // attributes_count

        // Class Attributes
        out.write(0x00); out.write(0x00);

    } catch (Exception e) {
        echoCommand("Build failed: " + e.getMessage());
        return null;
    }

    return out.toByteArray();
}

private byte[] mnemonicsToBytes(String mnemonics) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    Hashtable opcodes = new Hashtable();
    opcodes.put("nop", new Integer(0x00));
    opcodes.put("aconst_null", new Integer(0x01));
    opcodes.put("iconst_0", new Integer(0x03));
    opcodes.put("iconst_1", new Integer(0x04));
    opcodes.put("iconst_2", new Integer(0x05));
    opcodes.put("iload_0", new Integer(0x1A));
    opcodes.put("aload_0", new Integer(0x2A));
    opcodes.put("istore_0", new Integer(0x3B));
    opcodes.put("astore_0", new Integer(0x4B));
    opcodes.put("pop", new Integer(0x57));
    opcodes.put("iadd", new Integer(0x60));
    opcodes.put("return", new Integer(0xB1));
    opcodes.put("invokespecial", new Integer(0xB7));
    // ...adicione outras conforme precisar

    // Quebrar linhas simples, sem regex
    int start = 0;
    int length = mnemonics.length();
    while (start < length) {
        int end = mnemonics.indexOf('\n', start);
        if (end == -1) end = length;
        String line = mnemonics.substring(start, end).trim();
        start = end + 1;

        if (line.length() == 0) continue;

        // Quebrar por espaço simples
        int spaceIndex = line.indexOf(' ');
        String instr = line;
        String arg = null;
        if (spaceIndex != -1) {
            instr = line.substring(0, spaceIndex);
            arg = line.substring(spaceIndex + 1).trim();
        }

        Integer opcodeInt = (Integer) opcodes.get(instr);
        if (opcodeInt == null) throw new Exception("Opcode desconhecido: " + instr);
        out.write(opcodeInt.intValue());

        if (arg != null) {
            String[] args = split(arg, ' ');
            for (int i = 0; i < args.length; i++) {
                int val = Integer.parseInt(args[i]);
                out.write(val & 0xFF);
            }
        }

    }

    return out.toByteArray();
}

private String extractClassName(String code) {
    int idx = code.indexOf("class ");
    if (idx == -1) return "Unnamed";
    idx += 6;
    int end = code.indexOf(' ', idx);
    if (end == -1) end = code.indexOf('{', idx);
    return code.substring(idx, end).trim();
}

private String[] extractImports(String code) {
    Vector imports = new Vector();
    int start = 0;
    while (start < code.length()) {
        int end = code.indexOf('\n', start);
        if (end == -1) end = code.length();
        String line = code.substring(start, end).trim();
        if (line.startsWith("import ")) {
            int semi = line.indexOf(';');
            if (semi != -1) {
                String imp = line.substring(7, semi).trim();
                imports.addElement(replace(imp, ".", "/"));
            }
        }
        start = end + 1;
    }
    String[] result = new String[imports.size()];
    for (int i = 0; i < result.length; i++) result[i] = (String) imports.elementAt(i);
    return result;
}

private String extractMnemonics(String code) {
    int idx = code.indexOf("main");
    if (idx == -1) return "";

    int braceStart = code.indexOf('{', idx);
    if (braceStart == -1) return "";

    int braceCount = 1;
    int i = braceStart + 1;

    while (i < code.length() && braceCount > 0) {
        char c = code.charAt(i);
        if (c == '{') braceCount++;
        else if (c == '}') braceCount--;
        i++;
    }

    if (braceCount != 0) return "";

    return code.substring(braceStart + 1, i - 1).trim();
}



