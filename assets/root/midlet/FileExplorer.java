public class FileExplorer implements CommandListener {
    private String path = "file:///";
    private List screen = new List(form.getTitle(), List.IMPLICIT);
    private Command BACK = new Command("Back", Command.BACK, 1),
                    OPEN = new Command("Open", Command.OK, 1);

    public FileExplorer(String args) {
        if (!args.equals("")) {
            path = path + args;
        }
        screen.addCommand(BACK);
        screen.addCommand(OPEN);
        screen.setCommandListener(this);
        load();
        display.setCurrent(screen);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == BACK) {
            processCommand("xterm");
        } else if (c == OPEN) {
            int selectedIndex = screen.getSelectedIndex();
            if (selectedIndex >= 0) {
                String selected = screen.getString(selectedIndex);
                if (selected.equals("..")) {
                    int lastSlash = path.lastIndexOf('/', path.length() - 2);
                    if (lastSlash != -1) {
                        path = path.substring(0, lastSlash + 1);
                        load();
                    }
                } else if (selected.endsWith("/")) {
                    path = path + selected;
                    load();
                } else {
                    screen.setTicker(new Ticker("Downloading '" + selected + "'"));
                    writeRMS(selected, read(path + selected));
                    screen.setTicker(null);
                    warnCommand(null, "File '" + selected + "' saved!");
                }
            }
        }
    }

    private void load() {
        screen.deleteAll();
        if (!path.equals("file:///")) {
            screen.append("..", null);
        }
        try {
            if (path.equals("file:///")) {
                Enumeration roots = FileSystemRegistry.listRoots();
                while (roots.hasMoreElements()) {
                    screen.append((String) roots.nextElement(), null);
                }
            } else {
                FileConnection dir = (FileConnection) Connector.open(path, Connector.READ);
                Enumeration content = dir.list();
                Vector dirs = new Vector(), files = new Vector();
                while (content.hasMoreElements()) {
                    String filename = (String) content.nextElement();
                    if (filename.endsWith("/")) {
                        dirs.addElement(filename);
                    } else {
                        files.addElement(filename);
                    }
                }
                while (!dirs.isEmpty()) {
                    screen.append(getFirstString(dirs), null);
                }
                while (!files.isEmpty()) {
                    screen.append(getFirstString(files), null);
                }
                dir.close();
            }
        } catch (IOException e) {
        }
    }

    private static String getFirstString(Vector v) {
        String result = null;
        for (int i = 0; i < v.size(); i++) {
            String cur = (String) v.elementAt(i);
            if (result == null || cur.compareTo(result) < 0) {
                result = cur;
            }
        }
        v.removeElement(result);
        return result;
    }

    private String read(String file) {
        try {
            FileConnection fileConn = (FileConnection) Connector.open(file, Connector.READ);
            InputStream is = fileConn.openInputStream();
            StringBuffer content = new StringBuffer();
            int ch;
            while ((ch = is.read()) != -1) {
                content.append((char) ch);
            }
            is.close();
            fileConn.close();
            return content.toString();
        } catch (IOException e) {
            return "";
        }
    }
}