package javax.microedition.lcdui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.ImageIcon;

/** Functional Swing renderer for the desktop J2ME MIDP LCDUI stubs. */
final class DesktopLcdui {
    static final boolean headless = GraphicsEnvironment.isHeadless();

    static volatile boolean updating = false;

    private static JFrame frame;
    private static JLabel titleBar;
    private static JScrollPane scroller;
    private static JPanel commandsBar;
    private static Displayable current;

    private DesktopLcdui() { }

    static void ensure() {
        if (headless || frame != null || current != null) { return; }
        onEdt(new Runnable() { public void run() { build(); } });
    }

    private static void build() {
        if (headless) { System.err.println("[LC] WARN: build() skipped, headless=" + headless); return; }
        System.err.println("[LC] build() DISPLAY=" + System.getenv("DISPLAY"));
        frame = new JFrame("OpenTTY");
        titleBar = new JLabel(" ", SwingConstants.CENTER);
        titleBar.setFont(titleBar.getFont().deriveFont(java.awt.Font.BOLD, 15f));
        titleBar.setOpaque(true);
        titleBar.setBackground(new Color(0x224466));
        titleBar.setForeground(Color.WHITE);
        titleBar.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        scroller = new JScrollPane();
        scroller.getVerticalScrollBar().setUnitIncrement(16);

        commandsBar = new JPanel();
        commandsBar.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 6));
        commandsBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        frame.setLayout(new BorderLayout());
        frame.add(titleBar, BorderLayout.NORTH);
        frame.add(scroller, BorderLayout.CENTER);
        frame.add(commandsBar, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                Display d = Display.getDisplay();
                if (d != null) { d._windowClosed(); }
            }
            public void windowOpened(WindowEvent e) {
                System.err.println("[LC] windowOpened bounds=" + frame.getBounds());
            }
        });
        frame.setSize(500, 760);
        frame.setLocation(60, 40);
        frame.setVisible(true);
        frame.toFront();
        System.err.println("[LC] after setVisible bounds=" + frame.getBounds() + " visible=" + frame.isVisible());
    }

    static void onEdt(Runnable r) {
        if (headless) { return; }
        if (SwingUtilities.isEventDispatchThread()) { r.run(); }
        else { SwingUtilities.invokeLater(r); }
    }

    static void _show(final Displayable d) {
        if (headless) { return; }
        ensure();
        onEdt(new Runnable() { public void run() { publish(d); } });
    }

    static void _repaint(final Displayable d) {
        if (headless || d == null) { return; }
        Display dis = d.display;
        if (dis == null || dis.getCurrent() != d) { return; }
        onEdt(new Runnable() { public void run() {
            if (frame == null) { return; }
            if (d.display == null || d.display.getCurrent() != d) { return; }
            publish(d);
        } });
    }

    static void _itemSync(final Item item) {
        if (headless) { return; }
        Object c = item._comp;
        if (!(c instanceof ItemRow)) { return; }
        final ItemRow row = (ItemRow) c;
        onEdt(new Runnable() { public void run() {
            if (!row.isShowing() && !(item._owner != null && item._owner.display != null)) { return; }
            row.refresh();
        } });
    }

    private static void publish(Displayable d) {
        if (headless) { return; }
        System.err.println("[LC] publish " + d.getClass().getName() + " title=" + d.getTitle());
        if (current == d && frame.isVisible() && scroller.getViewport().getView() != null) {
            // still current; full refresh below anyway
        }
        current = d;
        if (titleBar != null) {
            String t = d.getTitle();
            titleBar.setText(t == null || t.length() == 0 ? "OpenTTY" : t);
        }
        JComponent body = buildBody(d);
        scroller.setViewportView(body);
        buildCommands(d);
        scroller.revalidate();
        scroller.repaint();
        frame.repaint();
    }

    private static void buildCommands(Displayable d) {
        commandsBar.removeAll();
        final Command[] cmds = d._commands();
        for (int i = 0; i < cmds.length; i++) {
            final Command cmd = cmds[i];
            JButton b = new JButton(cmd.getLabel());
            b.setFont(b.getFont().deriveFont(14f));
            b.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                dispatch(d, cmd);
            } });
            commandsBar.add(b);
        }
        if (d instanceof Alert && cmds.length == 0) {
            JButton b = new JButton("OK");
            b.setFont(b.getFont().deriveFont(14f));
            b.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                dispatch(d, new Command("OK", Command.OK, 0));
            } });
            commandsBar.add(b);
        }
        commandsBar.revalidate();
        commandsBar.repaint();
    }

    private static void dispatch(Displayable d, Command c) {
        Display dis = d.display;
        if (dis == null) { dis = Display.getDisplay(); }
        if (dis != null) { dis._fireCommand(d, c); }
        else { d._fire(c); }
    }

    // | ------- body builders ----------

    private static JComponent buildBody(Displayable d) {
        if (d instanceof List) { return buildList((List) d); }
        if (d instanceof Form) { return buildForm((Form) d); }
        if (d instanceof TextBox) { return buildTextBox((TextBox) d); }
        if (d instanceof Alert) { return buildAlert((Alert) d); }
        JPanel p = new JPanel(new BorderLayout());
        p.add(wrapped(getTitleOr(d), null), BorderLayout.CENTER);
        return p;
    }

    private static String getTitleOr(Displayable d) {
        String t = d.getTitle();
        return t == null || t.length() == 0 ? " " : t;
    }

    private static JComponent buildList(final List list) {
        DefaultListModel model = new DefaultListModel();
        for (int i = 0; i < list.size(); i++) { model.addElement(list.getString(i)); }
        JList jlist = new JList(model);
        jlist.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 14));
        jlist.setSelectionMode(list._isMultiple() ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
                                                  : ListSelectionModel.SINGLE_SELECTION);
        jlist.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        jlist.setFixedCellHeight(32);

        boolean[] flags = list._selectedFlags();
        int[] selIdx = new int[flags.length];
        int n = 0;
        for (int i = 0; i < flags.length; i++) { if (flags[i]) { selIdx[n++] = i; } }
        int[] primed = new int[n];
        System.arraycopy(selIdx, 0, primed, 0, n);
        if (n == 0 && !list._isMultiple() && list.size() > 0) { primed = new int[] { 0 }; }
        if (primed.length > 0) {
            updating = true;
            try { jlist.setSelectedIndices(primed); }
            finally { updating = false; }
        }

        jlist.addListSelectionListener(new ListSelectionListener() { public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting() || updating) { return; }
            boolean[] f = new boolean[list.size()];
            int[] idxs = jlist.getSelectedIndices();
            for (int i = 0; i < idxs.length; i++) { f[idxs[i]] = true; }
            list._setSelection(f);
        } });

        MouseAdapter sel = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) { list._select(); }
            }
        };
        jlist.addMouseListener(sel);
        jlist.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "select");
        jlist.getActionMap().put("select", new AbstractAction() { public void actionPerformed(ActionEvent e) {
            list._select();
        } });

        final JScrollPane sub = new JScrollPane(jlist);
        list._comp = sub;
        return sub;
    }

    private static JComponent buildTextBox(final TextBox tb) {
        JTextArea area = new JTextArea(tb._rawString());
        area.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 13));
        area.setLineWrap(true);
        area.setWrapStyleWord(false);
        area.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        area.getDocument().addDocumentListener(new DocumentListener() {
            private void ud(DocumentEvent e) {
                if (updating) { return; }
                String v = area.getText();
                if (v != null) { tb._setTextNoRepaint(v); }
            }
            public void insertUpdate(DocumentEvent e) { ud(e); }
            public void removeUpdate(DocumentEvent e) { ud(e); }
            public void changedUpdate(DocumentEvent e) { ud(e); }
        });
        JScrollPane sp = new JScrollPane(area);
        tb._comp = area;
        return sp;
    }

    private static JComponent buildAlert(Alert alert) {
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JLabel msg = wrapped(alert.getString(), new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 15));
        msg.setForeground(alert.getType() == AlertType.WARNING || alert.getType() == AlertType.ERROR
            ? new Color(0xAA0000) : Color.DARK_GRAY);
        p.add(msg, BorderLayout.CENTER);
        Image img = alert.getImage();
        if (img != null && img._buf() != null) {
            Image scaled = img._scaled(128);
            JLabel icon = new JLabel(new ImageIcon(scaled._buf()));
            icon.setHorizontalAlignment(SwingConstants.CENTER);
            p.add(icon, BorderLayout.NORTH);
        }
        return p;
    }

    private static JComponent buildForm(Form form) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        for (int i = 0; i < form.size(); i++) {
            Item item = form.get(i);
            p.add(itemRow(item));
            p.add(Box.createVerticalStrut(4));
        }
        p.add(Box.createVerticalGlue());
        form._comp = p;
        return p;
    }

    private static JComponent itemRow(Item item) {
        if (item._comp instanceof ItemRow) { return (ItemRow) item._comp; }
        ItemRow row = new ItemRow(item);
        item._comp = row;
        return row;
    }

    // | ------- item row ----------

    static final class ItemRow extends JPanel {
        final Item item;
        Component labelComp;
        Component main;
        JLabel iconComp;

        ItemRow(Item it) {
            this.item = it;
            setLayout(new BorderLayout(6, 4));
            Border b = BorderFactory.createMatteBorder(0, 0, (it.getLayout() & Item.LAYOUT_NEWLINE_AFTER) != 0 ? 2 : 0, 0, Color.LIGHT_GRAY);
            Border e = BorderFactory.createEmptyBorder(2, 2, 2, 2);
            setBorder(BorderFactory.createCompoundBorder(b, e));
            build();
        }

        private java.awt.Font awt() { return item.getFont()._awt(); }

        private void setLabelComp(JComponent c) {
            labelComp = c;
            if (labelComp != null) {
                add((Component) labelComp, BorderLayout.NORTH);
            }
        }

        private void setMain(JComponent c) {
            main = c;
            if (main != null) { add((Component) main, BorderLayout.CENTER); }
        }

        private void build() {
            String label = item.getLabel();
            if (item instanceof StringItem) {
                StringItem si = (StringItem) item;
                boolean button = si.getAppearanceMode() == StringItem.BUTTON
                    || item.getDefaultCommand() != null || item._commands().size() > 0;
                if (button) {
                    JButton b = new JButton(combine(label, si.getText()));
                    b.setFont(awt());
                    b.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
                        item._activate();
                    } });
                    setMain(b);
                } else {
                    JLabel l = wrapped(combine(label, si.getText()), awt());
                    setMain(l);
                }
            } else if (item instanceof TextField) {
                final TextField tf = (TextField) item;
                if (label != null && label.length() > 0) {
                    JLabel l = new JLabel(label);
                    l.setFont(awt());
                    l.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                    setLabelComp(l);
                }
                JTextField field = (tf.getConstraints() & TextField.PASSWORD) != 0
                    ? new JPasswordField() : new JTextField();
                field.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 14));
                updating = true;
                try { field.setText(tf.getString()); }
                finally { updating = false; }
                field.getDocument().addDocumentListener(new DocumentListener() {
                    private void ud(DocumentEvent de) {
                        if (updating) { return; }
                        String v = field.getText();
                        if (v != null) { tf._setModelText(v); }
                    }
                    public void insertUpdate(DocumentEvent e) { ud(e); }
                    public void removeUpdate(DocumentEvent e) { ud(e); }
                    public void changedUpdate(DocumentEvent e) { ud(e); }
                });
                setMain(field);
            } else if (item instanceof Gauge) {
                final Gauge gauge = (Gauge) item;
                if (label != null && label.length() > 0) {
                    JLabel l = new JLabel(label);
                    l.setFont(awt());
                    l.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                    setLabelComp(l);
                }
                if (gauge.isInteractive()) {
                    final JSlider slider = new JSlider(0, Math.max(1, gauge.getMaxValue()), gauge.getValue());
                    slider.addChangeListener(new javax.swing.event.ChangeListener() {
                        public void stateChanged(javax.swing.event.ChangeEvent e) {
                            if (updating) { return; }
                            gauge.setValue(slider.getValue());
                        }
                    });
                    setMain(slider);
                } else {
                    final JProgressBar bar = new JProgressBar(0, Math.max(1, gauge.getMaxValue()));
                    bar.setValue(gauge.getValue());
                    bar.setStringPainted(true);
                    setMain(bar);
                }
            } else if (item instanceof ChoiceGroup) {
                final ChoiceGroup cg = (ChoiceGroup) item;
                if (label != null && label.length() > 0) {
                    JLabel l = new JLabel(label);
                    l.setFont(awt());
                    l.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                    setLabelComp(l);
                }
                if (cg._type() == Choice.POPUP) {
                    DefaultComboBoxModel m = new DefaultComboBoxModel();
                    for (int i = 0; i < cg.size(); i++) { m.addElement(cg.getString(i)); }
                    final JComboBox combo = new JComboBox(m);
                    int selIdx = cg.getSelectedIndex();
                    updating = true;
                    try { if (selIdx >= 0) { combo.setSelectedIndex(selIdx); } }
                    finally { updating = false; }
                    combo.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if (updating) { return; }
                            int i = combo.getSelectedIndex();
                            if (i >= 0) { cg.setSelectedIndex(i, true); }
                        }
                    });
                    setMain(combo);
                } else if (cg._type() == Choice.MULTIPLE) {
                    JPanel box = new JPanel();
                    box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
                    for (int i = 0; i < cg.size(); i++) {
                        final int idx = i;
                        final JCheckBox cb = new JCheckBox(cg.getString(i), cg.isSelected(i));
                        cb.setFont(awt());
                        cb.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                if (updating) { return; }
                                cg.setSelectedIndex(idx, cb.isSelected());
                            }
                        });
                        box.add(cb);
                    }
                    setMain(box);
                } else {
                    JPanel box = new JPanel();
                    box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
                    ButtonGroup group = new ButtonGroup();
                    for (int i = 0; i < cg.size(); i++) {
                        final int idx = i;
                        JRadioButton rb = new JRadioButton(cg.getString(i), cg.isSelected(i));
                        rb.setFont(awt());
                        group.add(rb);
                        rb.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                if (updating) { return; }
                                cg.setSelectedIndex(idx, true);
                            }
                        });
                        box.add(rb);
                    }
                    box.add(Box.createVerticalGlue());
                    setMain(box);
                }
            } else if (item instanceof ImageItem) {
                ImageItem ii = (ImageItem) item;
                javax.swing.ImageIcon icon = ii.getImage() != null && ii.getImage()._buf() != null
                    ? new javax.swing.ImageIcon(ii.getImage()._scaled(96)._buf()) : null;
                JLabel l = icon != null ? new JLabel(icon) : new JLabel(" ");
                l.setHorizontalAlignment(SwingConstants.CENTER);
                setMain(l);
            } else if (item instanceof Spacer) {
                Spacer sp = (Spacer) item;
                JPanel p = new JPanel();
                p.setPreferredSize(new Dimension(sp.getMinimumWidth(), sp.getMinimumHeight()));
                p.setMaximumSize(new Dimension(Integer.MAX_VALUE, sp.getMinimumHeight()));
                setMain(p);
            } else {
                JLabel l = new JLabel(item instanceof Item ? item.getLabel() : "");
                l.setFont(awt());
                setMain(l);
            }
        }

        private static String combine(String label, String text) {
            if (label == null || label.length() == 0) { return text == null ? "" : text; }
            if (text == null || text.length() == 0) { return label; }
            return label + "\n" + text;
        }

        void refresh() {
            if (item instanceof StringItem) {
                StringItem si = (StringItem) item;
                String combined = combine(item.getLabel(), si.getText());
                if (main instanceof JButton) { ((JButton) main).setText(combined.replace('\n', ' ')); }
                else if (main instanceof JLabel) { ((JLabel) main).setText(html(combined)); }
            } else if (item instanceof TextField) {
                if (main instanceof javax.swing.text.JTextComponent) {
                    javax.swing.text.JTextComponent comp = (javax.swing.text.JTextComponent) main;
                    if (!comp.hasFocus()) {
                        updating = true;
                        try {
                            if (!comp.getText().equals(((TextField) item).getString())) {
                                comp.setText(((TextField) item).getString());
                            }
                        } finally { updating = false; }
                    }
                }
            } else if (item instanceof Gauge) {
                Gauge g = (Gauge) item;
                updating = true;
                try {
                    if (main instanceof JSlider && ((JSlider) main).getValue() != g.getValue()) { ((JSlider) main).setValue(g.getValue()); }
                    if (main instanceof JProgressBar && ((JProgressBar) main).getValue() != g.getValue()) { ((JProgressBar) main).setValue(g.getValue()); }
                } finally { updating = false; }
            } else if (item instanceof ChoiceGroup) {
                final ChoiceGroup cg = (ChoiceGroup) item;
                updating = true;
                try {
                    if (main instanceof JPanel) {
                        Component[] kids = ((JPanel) main).getComponents();
                        for (int i = 0; i < kids.length && i < cg.size(); i++) {
                            if (kids[i] instanceof JCheckBox) { ((JCheckBox) kids[i]).setSelected(cg.isSelected(i)); }
                            else if (kids[i] instanceof JRadioButton) { ((JRadioButton) kids[i]).setSelected(cg.isSelected(i)); }
                        }
                    } else if (main instanceof JComboBox) {
                        JComboBox combo = (JComboBox) main;
                        int si = cg.getSelectedIndex();
                        if (si >= 0 && combo.getSelectedIndex() != si) { combo.setSelectedIndex(si); }
                    }
                } finally { updating = false; }
            }
            if (labelComp instanceof javax.swing.text.JTextComponent) { }
            if (labelComp instanceof JLabel) {
                updateLabelText((JLabel) labelComp, item.getLabel());
            }
        }

        private void updateLabelText(JLabel l, String text) {
            String want = text == null ? "" : text;
            if (!want.equals(l.getText())) { l.setText(want); }
        }
    }

    // | ------- text helpers ----------

    static JLabel wrapped(String text, java.awt.Font f) {
        JLabel l = new JLabel(html(text));
        if (f != null) { l.setFont(f); }
        l.setVerticalAlignment(SwingConstants.TOP);
        l.setHorizontalAlignment(SwingConstants.LEFT);
        return l;
    }

    static String html(String text) {
        if (text == null) { return ""; }
        if (text.length() > 200000) { text = text.substring(0, 200000) + "..."; }
        StringBuffer sb = new StringBuffer("<html><body style='width:430px'>");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '&': sb.append("&amp;"); break;
                case '\n': sb.append("<br/>"); break;
                case '\t': sb.append("&nbsp;&nbsp;&nbsp;&nbsp;"); break;
                case ' ': sb.append(' '); break;
                default: sb.append(c);
            }
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    static String _limit(String text, int max) {
        if (text == null) { return ""; }
        if (max < 0) { return text; }
        return text.length() <= max ? text : text.substring(0, max);
    }

    static boolean _isFocused(TextBox tb) {
        if (headless) { return false; }
        Object c = tb._comp;
        return c instanceof javax.swing.text.JTextComponent && ((javax.swing.text.JTextComponent) c).hasFocus();
    }

    static String _liveText(TextBox tb) {
        Object c = tb._comp;
        return c instanceof javax.swing.text.JTextComponent ? ((javax.swing.text.JTextComponent) c).getText() : null;
    }
}