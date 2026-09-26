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
    private java.awt.GridBagConstraints bodyGbc = new java.awt.GridBagConstraints();

    /** Reset the shared body panel and its row counter for a fresh render.
     *  GridBagLayout fills every row across the full column width, so console
     *  text and fields are always flush with the left edge (BoxLayout Y_AXIS
     *  left a large empty margin beside the console JTextArea). */
    private void resetBody() {
        body.removeAll();
        body.setLayout(new java.awt.GridBagLayout());
        bodyGbc = new java.awt.GridBagConstraints();
        bodyGbc.gridx = 0;
        bodyGbc.gridy = -1;
        bodyGbc.weightx = 1.0;
        bodyGbc.weighty = 0.0;
        bodyGbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        bodyGbc.anchor = java.awt.GridBagConstraints.NORTHWEST;
    }

    private void addBodyRow(java.awt.Component c) {
        bodyGbc.gridy++;
        body.add(c, bodyGbc);
    }

    /** Give the whole extra vertical space to the last row's cell so the grid
     *  stays pinned to the top instead of GridBagLayout centering it. */
    private void pinGridTop() {
        int n = body.getComponentCount();
        if (n == 0) { return; }
        java.awt.GridBagLayout gb = (java.awt.GridBagLayout) body.getLayout();
        java.awt.GridBagConstraints g = gb.getConstraints(body.getComponent(n - 1));
        g.weighty = 1.0;
        gb.setConstraints(body.getComponent(n - 1), g);
    }
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
        // Tiling WMs (bspwm, i3...) resize the toplevel behind our back (and
        // without a compositor they may not send Expose). Re-run a full pass
        // so Swing revalidates and paints the content at the real size.
        frame.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent ev) { repaintAll(); }
            public void componentShown(java.awt.event.ComponentEvent ev) { repaintAll(); }
        });
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

    // All rebuilds run on the EDT. touch()/setCurrent may be called from any
    // thread (the Lua boot touches StringItems on its own thread), so marshal
    // every request and coalesce storms. A request arriving while a rebuild is
    // already queued must still produce a <i>later</i> pass, otherwise output
    // written to a console StringItem never gets validated/repainted — which
    // shows up as a blank window on WMs without a compositor (bspwm).
    private boolean rebuildQueued = false;

    private void repaintAll() {
        if (rebuildQueued) { return; }
        rebuildQueued = true;
        SwingUtilities.invokeLater(new Runnable() { public void run() { rebuild(); } });
    }

    private void rebuild() {
        rebuildQueued = false;
        if (frame.isVisible()) {
            coreRebuild();
        } else {
            // Build the content before the first map so the WM's first expose
            // already sees real widgets.
            coreRebuild();
            frame.setVisible(true);
            frame.repaint();
        }
        scroll.revalidate();
        scroll.repaint();
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
                // Enter on a focused command button activates it (JButton only
                // responds to Space when not the root pane default button).
                b.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "activate");
                b.getActionMap().put("activate", new javax.swing.AbstractAction() {
                    public void actionPerformed(ActionEvent ev) { b.doClick(); }
                });
                commands.add(b);
            }
        }

        body.validate();
        body.repaint();
frame.revalidate();
            frame.repaint();
            JTextField f = focusTarget();
            if (f != null) { f.requestFocusInWindow(); }
    }

    private void fireRunCommand() {
        Displayable d = current;
        if (!(d instanceof Form)) { return; }
        Form f = (Form) d;
        Command run = null;
        for (int i = 0; i < f.commands.size(); i++) {
            Command c = (Command) f.commands.elementAt(i);
            if (c.getLabel().equals("Run")) { run = c; break; }
        }
        if (run == null || f.listener == null) { return; }
        f.listener.commandAction(run, f);
    }

    public static void notifyDestroyed() {
        if (instance == null) { return; }
        final JFrame f = instance.frame;
        if (f == null) { return; }
        Runnable close = new Runnable() {
            public void run() { f.dispose(); }
        };
        if (SwingUtilities.isEventDispatchThread()) { close.run(); }
        else { SwingUtilities.invokeLater(close); }
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
        resetBody();
        body.setBackground(java.awt.Color.WHITE);
        for (int i = 0; i < f.items.size(); i++) {
            Object obj = f.items.elementAt(i);
            if (obj instanceof TextField) {
                textFieldWidget((TextField) obj);
            } else if (obj instanceof StringItem) {
                final StringItem si = (StringItem) obj;
                if ((si.layout & StringItem.BUTTON) != 0 && si.itemCommandListener != null && si.defaultCommand != null) {
                    JButton b = new JButton(si.getText().length() == 0 ? si.getLabel() : si.getText());
                    final Command cmd = si.defaultCommand;
                    final Item item = si;
                    final ItemCommandListener l = si.itemCommandListener;
                    b.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ev) { l.commandAction(cmd, item); }
                    });
                    addBodyRow(b);
                } else {
                    String txt = (si.getLabel() == null || si.getLabel().length() == 0) ? si.getText() : si.getLabel() + ": " + si.getText();
                    addBodyRow(textWidget(txt));
                }
            } else if (obj instanceof Image) {
                final Image img = (Image) obj;
                JLabel lab = new JLabel(img.awt() != null ? new javax.swing.ImageIcon(img.awt()) : new javax.swing.ImageIcon());
                addBodyRow(lab);
            } else if (obj instanceof Item) {
                JLabel lab = new JLabel(((Item) obj).getLabel());
                addBodyRow(lab);
            } else {
                addBodyRow(textWidget(String.valueOf(obj)));
            }
        }
        if (f.items.size() == 0) { addBodyRow(new JLabel(" ")); }
        pinGridTop();
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
        w.setBackground(java.awt.Color.WHITE);
        w.setAlignmentX(Component.LEFT_ALIGNMENT);
        // The console/description area must fill the whole form column, with
        // the text starting at the left edge. BoxLayout Y_AXIS does not
        // reliably stretch a raw JTextArea child (it was laid out with a
        // big empty margin on the left), so wrap it in a full-width panel
        // that centers the area, keeping its own left alignment.
        javax.swing.JPanel wrapper = new javax.swing.JPanel(new java.awt.BorderLayout());
        wrapper.setOpaque(true);
        wrapper.setBackground(java.awt.Color.WHITE);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) w.getPreferredSize().getHeight()));
        w.setOpaque(true);
        wrapper.add(w, java.awt.BorderLayout.CENTER);
        return wrapper;
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
            // Enter in the input row runs the form's default Run command, like
            // pressing the (focused) Run button on the command bar.
            field.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent ev) { fireRunCommand(); }
            });
            widgetCache.put(tf, field);
        }
        String model = tf.getString();
        if (!field.getText().equals(model)) { field.setText(model); }
        row.add(field, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) field.getPreferredSize().getHeight() + 26));
        addBodyRow(row);
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