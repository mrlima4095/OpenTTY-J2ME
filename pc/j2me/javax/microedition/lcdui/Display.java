package javax.microedition.lcdui;

import javax.microedition.midlet.MIDlet;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Desktop implementation of the MIDP Display. Renders the current
 * Displayable inside a small phone-shaped AWT/Swing window; commands become
 * buttons that dispatch to the screen's CommandListener. src/ is untouched.
 *
 * Threading: all screen manipulation must run on the AWT event thread,
 * mirroring J2ME's single-threaded display model. touch()/setCurrent are
 * safe from any thread (they marshal to the EDT).
 */
public class Display {
    private static Display instance = null;
    private MIDlet owner = null;
    private Displayable current = null;
    private Displayable prevScreen = null;
    private Displayable nextAfterAlert = null;

    public static Display getDisplay(MIDlet midlet) {
        if (instance == null) { instance = new Display(midlet); }
        return instance;
    }

    public static Displayable staticCurrent() {
        return instance == null ? null : instance.current;
    }

    private Display(MIDlet midlet) { owner = midlet; }

    public static boolean isCurrent(Displayable d) { return instance != null && instance.current == d; }

    private final JFrame frame = new JFrame("OpenTTY");
    private final JPanel body = new JPanel();
    private final JPanel commands = new JPanel();
    private final JLabel titleLabel = new JLabel("OpenTTY");
    private final JScrollPane scroll = new JScrollPane(body);
    // Reused across rebuilds so typing focus survives output updates.
    private final Map widgetCache = new HashMap();
    private Timer alertTimer = null;

    {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        top.add(titleLabel, BorderLayout.CENTER);
        frame.add(top, BorderLayout.NORTH);
        frame.add(scroll, BorderLayout.CENTER);
        commands.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 4));
        frame.add(commands, BorderLayout.SOUTH);
        frame.setSize(560, 640);
        frame.setMinimumSize(new Dimension(360, 320));
        frame.setLocationRelativeTo(null);
    }

    public void setCurrent(Displayable d) {
        if (d == null) { return; }
        prevScreen = current instanceof Alert ? prevScreen : current;
        nextAfterAlert = null;
        current = d;
        scheduleAlertDismiss();
        repaintAll();
    }

    public void setCurrent(Alert alert, Displayable next) {
        if (alert == null) { setCurrent(next); return; }
        prevScreen = current;
        nextAfterAlert = next;
        current = alert;
        scheduleAlertDismiss();
        repaintAll();
    }

    public void setCurrent(Alert alert) { setCurrent(alert, null); }

    private void scheduleAlertDismiss() {
        if (alertTimer != null) { alertTimer.stop(); alertTimer = null; }
        if (current instanceof Alert) {
            final Alert a = (Alert) current;
            final int t = a.getTimeout();
            if (t != Alert.FOREVER && a.commands.size() == 0) {
                alertTimer = new Timer(Math.max(100, t), new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                        if (current == a) {
                            setCurrent(nextAfterAlert != null ? nextAfterAlert : prevScreen);
                        }
                    }
                });
                alertTimer.setRepeats(false);
                alertTimer.start();
            }
        }
    }

    public Displayable getCurrent() { return current; }

    public void vibrate(int duration) {
        try { java.awt.Toolkit.getDefaultToolkit().beep(); } catch (Throwable t) { }
    }

    public MIDlet getOwner() { return owner; }

    static void touch() {
        if (instance == null) { return; }
        instance.repaintAll();
    }

    private void repaintAll() {
        if (SwingUtilities.isEventDispatchThread()) {
            rebuild();
        } else {
            SwingUtilities.invokeLater(new Runnable() { public void run() { rebuild(); } });
        }
    }

    private boolean rebuilding = false;

    private void rebuild() {
        if (rebuilding) { return; }
        rebuilding = true;
        frame.setVisible(true);
        try {
            coreRebuild();
        } finally {
            rebuilding = false;
        }
    }

    private void coreRebuild() {
        Displayable d = current;
        titleLabel.setText(d == null ? "" : d.getTitle());
        body.removeAll();
        commands.removeAll();

        if (d instanceof Alert) { renderAlert((Alert) d); }
        else if (d instanceof Form) { renderForm((Form) d); }
        else if (d instanceof List) { renderList((List) d); }
        else if (d instanceof TextBox) { renderTextBox((TextBox) d); }
        else if (d != null) { body.add(new JLabel("Unsupported screen: " + d.getClass().getName())); }
        else { body.add(new JLabel(" ")); }

        pruneWidgetCache(d);

        if (d != null) {
            Enumeration cs = d.commands.elements();
            while (cs.hasMoreElements()) {
                final Command c = (Command) cs.nextElement();
                final Displayable screen = d;
                if (c == List.SELECT_COMMAND) { continue; }
                JButton b = new JButton(c.getLabel());
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                        CommandListener l = screen.listener;
                        if (l != null) { l.commandAction(c, screen); }
                    }
                });
                commands.add(b);
            }
        }

        body.validate();
        body.repaint();
        frame.validate();
        frame.repaint();
        JTextField f = focusTarget();
        if (f != null) { f.requestFocusInWindow(); }
    }

    private void pruneWidgetCache(Displayable d) {
        if (!(d instanceof Form)) {
            widgetCache.clear();
            return;
        }
        Form f = (Form) d;
        boolean has;
        for (Iterator it = widgetCache.keySet().iterator(); it.hasNext(); ) {
            Object key = it.next();
            has = false;
            for (int i = 0; i < f.items.size(); i++) {
                if (f.items.elementAt(i) == key) { has = true; break; }
            }
            if (!has) { it.remove(); }
        }
    }

    private JTextField focusTarget() {
        if (!(current instanceof Form)) { return null; }
        Form f = (Form) current;
        for (int i = 0; i < f.items.size(); i++) {
            Object o = f.items.elementAt(i);
            if (o instanceof TextField) {
                JTextField tf = (JTextField) widgetCache.get(o);
                if (tf != null) { return tf; }
            }
        }
        return null;
    }

    // | (Form)
    private void renderForm(Form f) {
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(java.awt.Color.WHITE);
        for (int i = 0; i < f.items.size(); i++) {
            Object obj = f.items.elementAt(i);
            if (obj instanceof TextField) {
                textFieldWidget((TextField) obj);
            } else if (obj instanceof StringItem) {
                final StringItem si = (StringItem) obj;
                if ((si.layout & StringItem.BUTTON) != 0 && si.itemCommandListener != null && si.defaultCommand != null) {
                    JButton b = new JButton(si.getText().length() == 0 ? si.getLabel() : si.getText());
                    b.setAlignmentX(Component.LEFT_ALIGNMENT);
                    final Command cmd = si.defaultCommand;
                    final Item item = si;
                    final ItemCommandListener l = si.itemCommandListener;
                    b.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ev) { l.commandAction(cmd, item); }
                    });
                    body.add(b);
                } else {
                    String txt = (si.getLabel() == null || si.getLabel().length() == 0) ? si.getText() : si.getLabel() + ": " + si.getText();
                    body.add(textWidget(txt));
                }
            } else if (obj instanceof Image) {
                final Image img = (Image) obj;
                JLabel lab = new JLabel(img.awt() != null ? new javax.swing.ImageIcon(img.awt()) : new javax.swing.ImageIcon());
                lab.setAlignmentX(Component.LEFT_ALIGNMENT);
                body.add(lab);
            } else if (obj instanceof Item) {
                JLabel lab = new JLabel(((Item) obj).getLabel());
                lab.setAlignmentX(Component.LEFT_ALIGNMENT);
                body.add(lab);
            } else {
                body.add(textWidget(String.valueOf(obj)));
            }
        }
        if (f.items.size() == 0) { body.add(new JLabel(" ")); }
    }

    private javax.swing.JComponent textWidget(String txt) {
        javax.swing.JComponent w;
        if (txt.indexOf('\n') >= 0) {
            JTextArea area = new JTextArea(txt);
            area.setEditable(false);
            area.setLineWrap(false);
            area.setWrapStyleWord(false);
            area.setFocusable(false);
            area.setBorder(null);
            w = area;
        } else {
            w = new JLabel(txt);
        }
        w.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        w.setAlignmentX(Component.LEFT_ALIGNMENT);
        w.setBackground(java.awt.Color.WHITE);
        w.setOpaque(true);
        return w;
    }

    private void textFieldWidget(final TextField tf) {
        JPanel row = new JPanel(new BorderLayout(6, 2));
        row.setOpaque(true);
        row.setBackground(java.awt.Color.WHITE);
        String lab = tf.getLabel();
        if (lab != null && lab.length() > 0) {
            JLabel l = new JLabel(lab);
            l.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
            row.add(l, BorderLayout.NORTH);
        }
        JTextField field = (JTextField) widgetCache.get(tf);
        if (field == null) {
            field = (tf.getConstraints() & TextField.PASSWORD) != 0 ? new JPasswordField() : new JTextField();
            field.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
            final JTextField f2 = field;
            field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { tf.updateFromUi(f2.getText()); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { tf.updateFromUi(f2.getText()); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { tf.updateFromUi(f2.getText()); }
            });
            widgetCache.put(tf, field);
        }
        String model = tf.getString();
        if (!field.getText().equals(model)) { field.setText(model); }
        row.add(field, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) field.getPreferredSize().getHeight() + 26));
        body.add(row);
    }

    // | (List)
    private void renderList(final List list) {
        body.setLayout(new BorderLayout());
        body.setBackground(java.awt.Color.WHITE);
        final java.util.Vector rows = list.elements;
        int n = rows.size();
        final String[] data = new String[n];
        for (int i = 0; i < n; i++) {
            boolean sel = list.isSelected(i) || (list.getSelectedIndex() >= 0 && list.getSelectedIndex() == i);
            data[i] = (sel ? "> " : "  ") + rows.elementAt(i);
        }
        final JList render = new JList(data);
        render.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 13));
        render.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (list.getSelectedIndex() >= 0 && list.getSelectedIndex() < n) { render.setSelectedIndex(list.getSelectedIndex()); }
        render.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                list.setSelectionFromUi(render.getSelectedIndex());
            }
        });
        render.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent ev) {
                int idx = render.locationToIndex(ev.getPoint());
                if (idx >= 0) { list.setSelectionFromUi(idx); }
                if (ev.getClickCount() >= 2 && list.listener != null) {
                    list.listener.commandAction(list.getSelectCommand(), list);
                }
            }
        });
        render.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ev) {
                if (ev.getKeyCode() == KeyEvent.VK_ENTER && list.listener != null) {
                    list.listener.commandAction(list.getSelectCommand(), list);
                }
            }
        });
        body.add(render, BorderLayout.CENTER);
    }

    // | (TextBox)
    private void renderTextBox(final TextBox box) {
        body.setLayout(new BorderLayout());
        body.setBackground(java.awt.Color.WHITE);
        final JTextArea area = new JTextArea(box.getString());
        area.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { sync(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { sync(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { sync(); }
            private void sync() { box.field.updateFromUi(area.getText()); }
        });
        body.add(area, BorderLayout.CENTER);
    }

    // | (Alert)
    private void renderAlert(Alert a) {
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(java.awt.Color.WHITE);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(true);
        p.setBackground(new java.awt.Color(255, 250, 220));
        String type = a.getType() == null ? "" : a.getType().toString();
        JLabel tl = new JLabel(a.getTitle() + (type.length() > 0 ? "  [" + type + "]" : ""));
        tl.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
        tl.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(tl);
        JTextArea msg = new JTextArea(a.getString());
        msg.setEditable(false);
        msg.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        msg.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(msg);
        body.add(p);
    }
}