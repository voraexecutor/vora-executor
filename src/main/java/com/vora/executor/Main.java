package com.vora.executor;
import com.vora.agent.a;
import com.vora.executor.ui.*;
import com.vora.executor.ui.Button;
import com.vora.common.VoraTemp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;


public final class Main {

    private final Settings settings = new Settings();
    private final ScriptHistory history = new ScriptHistory();
    private final JTextArea editor = new JTextArea(DocsPanel.STARTER);
    private final JTextPane console = new JTextPane();
    private final JPanel content = new JPanel(new BorderLayout());
    private final List<String> consoleLines = new ArrayList<>();


    private String scriptSource = DocsPanel.STARTER;
    private String mixinSource = """
            // Vora executor    
            // https://discord.gg/Nm2K5Mk72M
            mc.player.sendMessage(net.minecraft.text.Text.literal("hi"), false);
            """;
    private boolean mixinTab;
    private JLabel scriptTabLabel;
    private JLabel mixinTabLabel;
    private TabBar scriptTabBar;
    private TabBar mixinTabBar;

    private ProcessSelector processSelector;
    private StatusPill statusPill;
    private GButton executeButton;
    private RoundedPanel editorCard;
    private RoundedPanel consoleCard;
    private StatusPill.State pillState = StatusPill.State.IDLE;

    private RoundedPanel root;
    private JFrame frame;
    private UndoManager undo;
    private RoundedPanel docsCard;
    private ScriptsPanel scriptsCard;
    private SettingsPanel settingsCard;
    private InjectModPanel injectModCard;

    private NavButton editorNav;
    private NavButton consoleNav;
    private NavButton docsNav;
    private NavButton scriptsNav;
    private NavButton injectModNav;
    private NavButton settingsNav;

    private boolean showEditor = true;
    private boolean showConsole = false;
    private boolean showDocs = false;
    private boolean showScripts = false;
    private boolean showInjectMod = false;
    private boolean showSettings = false;
    private boolean savedEditor = true;
    private boolean savedConsole = true;

    private long lastTabSwitch = 0;
    private static final long TAB_COOLDOWN_MS = 200;
    private boolean animating = false;
    private javax.swing.Timer autoAttachTimer;
    private String lastAttachedId;

    public static void main(String[] args) throws InterruptedException {
        if (args.length > 0) {
            a.main(args);
            return;
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        applyLookAndFeel();

        SwingUtilities.invokeLater(() -> new Main().show());

    }

    private static void applyLookAndFeel() {
        UIManager.put("PopupMenu.background", Theme.ELEVATED);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(Theme.BORDER));
        UIManager.put("ToolTip.background", Theme.ELEVATED);
        UIManager.put("ToolTip.foreground", Theme.TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(Theme.BORDER));
    }

    public void show() {
        frame = new JFrame("Vora Executor");
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Theme.apply(settings.theme());
        applyLookAndFeel();

        buildUi();

        frame.setMinimumSize(new Dimension(780, 560));
        frame.setSize(1060, 720);
        frame.setLocationRelativeTo(null);
        applyShape();

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyShape();
            }
        });
        new FrameResizer(frame);
        Scrollhelper.enableMiddleDrag(frame);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                VoraTemp.cleanup();
            }
        });

        frame.setVisible(true);

        appendConsole("[client] executor ready");
        if (Injector.agentJar() == null) {
            setStatus(StatusPill.State.ERROR);
            appendConsole("[client] agent jar not found - build it with: gradlew jar");
        }

        startAutoAttachTimer();
        if (processSelector != null) onProcessSelected(processSelector.getSelected());
    }

    private void buildUi() {
        detach(editor);
        detach(console);

        root = new RoundedPanel(Theme.WINDOW, 16).border(Theme.BORDER);
        root.setLayout(new BorderLayout());
        root.setBorder(new EmptyBorder(1, 1, 1, 1));
        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);

        frame.setBackground(Theme.WINDOW);
        frame.setContentPane(root);

        editorCard = new RoundedPanel(Theme.CODE_BG, 12).border(Theme.BORDER_SOFT);
        consoleCard = new RoundedPanel(Theme.CODE_BG, 12).border(Theme.BORDER_SOFT);
        buildEditorCard();
        buildConsoleCard();

        docsCard = new DocsPanel(this::loadStarter);
        scriptsCard = new ScriptsPanel(history, this::openScript);
        settingsCard = new SettingsPanel(settings, this::onSettingChanged);
        injectModCard = new InjectModPanel();

        applyShape();
        applyViews();
        root.revalidate();
        root.repaint();
    }



    private JComponent buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(7, 14, 7, 10));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        brand.setOpaque(false);
        brand.add(logo());

        JLabel name = new JLabel("VORA");
        name.setFont(Theme.ui(Font.BOLD, 14));
        name.setForeground(Theme.TEXT);

        JLabel sub = new JLabel("executor");
        sub.setFont(Theme.ui(Font.PLAIN, 11));
        sub.setForeground(Theme.FAINT);

        brand.add(name);
        brand.add(sub);
        bar.add(brand, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        controls.setOpaque(false);

        Button minimize = new Button(Button.Glyph.MINIMIZE, "Minimize",
                false, () -> frame.setState(JFrame.ICONIFIED));
        Button close = new Button(Button.Glyph.CLOSE, "Close", true, () -> {
            VoraTemp.cleanup();
            System.exit(0);
        });


        controls.add(minimize);
        controls.add(close);
        bar.add(controls, BorderLayout.EAST);

        FrameResizer.enableDrag(frame, bar);
        FrameResizer.enableDrag(frame, brand);
        FrameResizer.enableDrag(frame, name);
        FrameResizer.enableDrag(frame, sub);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(bar, BorderLayout.CENTER);

        JPanel line = new JPanel();
        line.setBackground(Theme.BORDER_SOFT);
        line.setPreferredSize(new Dimension(0, 1));
        wrapper.add(line, BorderLayout.SOUTH);
        return wrapper;
    }

    private JComponent logo() {
        JComponent logo = new JComponent() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Ui.aa(g);
                g.setPaint(new GradientPaint(0, 0, Theme.ACCENT, 26, 26, Theme.ACCENT_2));
                g.fill(Ui.round(0, 0, 26, 26, 8));
                g.setColor(Color.WHITE);
                g.setFont(Theme.ui(Font.BOLD, 14));
                FontMetrics fm = g.getFontMetrics();
                g.drawString("V", (26 - fm.stringWidth("V")) / 2, (26 - fm.getHeight()) / 2 + fm.getAscent());
                g.dispose();
            }
        };
        logo.setPreferredSize(new Dimension(26, 26));
        logo.setMaximumSize(new Dimension(26, 26));
        return logo;
    }



    private JComponent buildBody() {
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(buildSidebar(), BorderLayout.WEST);
        body.add(buildMain(), BorderLayout.CENTER);
        return body;
    }

    private JComponent buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setOpaque(false);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(14, 12, 14, 12));

        editorNav = addNav(sidebar, NavButton.Icon.CODE, "Code editor", this::toggleEditor);
        consoleNav = addNav(sidebar, NavButton.Icon.CONSOLE, "Console output", this::toggleConsole);
        injectModNav = addNav(sidebar, NavButton.Icon.PLUGIN, "Inject mod", this::toggleInjectMod);
        docsNav = addNav(sidebar, NavButton.Icon.BOOK, "Documentation", this::toggleDocs);
        scriptsNav = addNav(sidebar, NavButton.Icon.LIST, "Executed scripts", this::toggleScripts);
        settingsNav = addNav(sidebar, NavButton.Icon.GEAR, "Settings", this::toggleSettings);

        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private NavButton addNav(JPanel sidebar, NavButton.Icon icon, String tip, Runnable onClick) {
        NavButton button = new NavButton(icon, tip, onClick);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(6));
        return button;
    }

    private JComponent buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(buildToolbar(), BorderLayout.NORTH);

        content.setOpaque(false);
        content.setBorder(new EmptyBorder(0, 16, 14, 16));
        main.add(content, BorderLayout.CENTER);
        return main;
    }

    private JComponent buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(10, 16, 10, 16));

        processSelector = new ProcessSelector();
        processSelector.setPreferredSize(new Dimension(430, 34));
        processSelector.setOnSelect(this::onProcessSelected);
        processSelector.setOnRefresh(() -> appendConsole("[client] refreshed process list"));
        processSelector.setAutoAttach(settings.autoAttach());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(processSelector);
        toolbar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        statusPill = new StatusPill();
        statusPill.setState(pillState);
        right.add(statusPill);

        executeButton = new GButton("EXECUTE", GButton.Style.BLACK);
        executeButton.setIcon(new PlayIcon(11, Theme.BUTTON_TEXT));
        executeButton.setIconTextGap(9);
        executeButton.setPreferredSize(new Dimension(146, 36));
        executeButton.addActionListener(e -> onExecute());
        right.add(executeButton);
        toolbar.add(right, BorderLayout.EAST);
        return toolbar;
    }

    private void buildEditorCard() {
        editorCard.setLayout(new BorderLayout());
        editorCard.add(tabStrip(), BorderLayout.NORTH);

        editor.setFont(Theme.mono(Font.PLAIN, 13));
        editor.setTabSize(4);
        editor.setBackground(Theme.CODE_BG);
        editor.setForeground(Theme.TEXT);
        editor.setCaretColor(Theme.CARET);
        editor.setSelectionColor(Theme.SELECTION);
        editor.setSelectedTextColor(Theme.TEXT);
        editor.setBorder(new EmptyBorder(8, 10, 8, 10));
        editor.setLineWrap(false);

        JScrollPane scroll = new JScrollPane(editor);
        scroll.setRowHeaderView(new LineNumber(editor));
        ScrollBar.apply(scroll);
        scroll.getViewport().setBackground(Theme.CODE_BG);
        editorCard.add(scroll, BorderLayout.CENTER);

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setOpaque(false);
        searchPanel.setVisible(false);
        searchPanel.setBorder(new EmptyBorder(4, 8, 4, 8));
        JTextField searchField = new JTextField();
        searchField.setFont(Theme.mono(Font.PLAIN, 11));
        searchField.setBackground(Theme.ELEVATED);
        searchField.setForeground(Theme.TEXT);
        searchField.setCaretColor(Theme.CARET);
        searchField.setBorder(new EmptyBorder(4, 8, 4, 8));
        searchField.putClientProperty("JTextField.placeholderText", "Search...");
        JButton nextBtn = new GButton("Next", GButton.Style.SUBTLE);
        nextBtn.setBorder(new EmptyBorder(2, 10, 2, 10));
        nextBtn.setFont(Theme.ui(Font.PLAIN, 11));
        JButton prevBtn = new GButton("Prev", GButton.Style.SUBTLE);
        prevBtn.setBorder(new EmptyBorder(2, 10, 2, 10));
        prevBtn.setFont(Theme.ui(Font.PLAIN, 11));
        JButton closeBtn = new GButton("×", GButton.Style.SUBTLE);
        closeBtn.setBorder(new EmptyBorder(2, 10, 2, 10));
        closeBtn.setFont(Theme.ui(Font.BOLD, 14));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.setOpaque(false);
        actions.add(prevBtn);
        actions.add(nextBtn);
        actions.add(closeBtn);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(actions, BorderLayout.EAST);

        JPanel editorWrap = new JPanel(new BorderLayout());
        editorWrap.setOpaque(false);
        editorWrap.add(searchPanel, BorderLayout.NORTH);
        editorWrap.add(scroll, BorderLayout.CENTER);
        editorCard.add(editorWrap, BorderLayout.CENTER);

        if (undo == null) {
            undo = new UndoManager();
            editor.getDocument().addUndoableEditListener(undo);
        }
        editor.getActionMap().put("vora.undo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                try {
                    if (undo.canUndo()) undo.undo();
                } catch (Exception ignored) {
                }
            }
        });
        editor.getActionMap().put("vora.redo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                try {
                    if (undo.canRedo()) undo.redo();
                } catch (Exception ignored) {
                }
            }
        });
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "vora.undo");
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "vora.redo");
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "vora.redo");

        KeyStroke run = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK);
        editor.getInputMap().put(run, "execute");
        editor.getActionMap().put("execute", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onExecute();
            }
        });

        KeyStroke find = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK);
        editor.getInputMap().put(find, "find");
        editor.getActionMap().put("find", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                searchPanel.setVisible(true);
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });

        closeBtn.addActionListener(e -> {
            searchPanel.setVisible(false);
            editor.requestFocusInWindow();
        });

        ActionListener findNext = e -> {
            String text = searchField.getText();
            if (text.isEmpty()) return;
            String content = editor.getText();
            int pos = editor.getCaretPosition();
            int found = content.indexOf(text, pos + 1);
            if (found == -1) found = content.indexOf(text);
            if (found != -1) {
                editor.setCaretPosition(found + text.length());
                editor.moveCaretPosition(found);
            }
        };

        ActionListener findPrev = e -> {
            String text = searchField.getText();
            if (text.isEmpty()) return;
            String content = editor.getText();
            int pos = editor.getSelectionStart();
            if (pos == editor.getSelectionEnd()) pos = editor.getCaretPosition();
            int found = content.lastIndexOf(text, pos - 1);
            if (found == -1) found = content.lastIndexOf(text);
            if (found != -1) {
                editor.setCaretPosition(found);
                editor.moveCaretPosition(found + text.length());
            }
        };

        nextBtn.addActionListener(findNext);
        prevBtn.addActionListener(findPrev);
        searchField.addActionListener(findNext);
        searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "findPrev");
        searchField.getActionMap().put("findPrev", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                findPrev.actionPerformed(e);
            }
        });

        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeFind");
        editor.getActionMap().put("closeFind", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                searchPanel.setVisible(false);
                editor.requestFocusInWindow();
            }
        });
    }

    private JComponent tabStrip() {
        JPanel strip = new JPanel(new BorderLayout());
        strip.setOpaque(false);
        strip.setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER_SOFT));

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabs.setOpaque(false);

        scriptTabLabel = new JLabel("script.java");
        scriptTabBar = new TabBar(true);
        tabs.add(fileTab(scriptTabLabel, scriptTabBar, false));

        mixinTabLabel = new JLabel("mixin.java");
        mixinTabBar = new TabBar(false);
        tabs.add(fileTab(mixinTabLabel, mixinTabBar, true));

        strip.add(tabs, BorderLayout.WEST);

        styleFileTabs();

        JPanel line = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Ui.aa(g2);
                g2.setPaint(new GradientPaint(0, 0, Theme.ACCENT, getWidth(), 0, Theme.ACCENT_2));
                g2.fill(Ui.round(0, 0, getWidth(), 2, 1));
                g2.dispose();
            }
        };
        line.setPreferredSize(new Dimension(0, 2));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        strip.add(line, BorderLayout.SOUTH);

        return strip;
    }

    private JComponent fileTab(JLabel label, TabBar bar, boolean mixin) {
        label.setFont(Theme.mono(Font.PLAIN, 11));

        JPanel tab = new JPanel(new BorderLayout());
        tab.setOpaque(false);
        tab.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        tab.add(label, BorderLayout.CENTER);
        tab.add(bar, BorderLayout.SOUTH);
        label.setBorder(new EmptyBorder(9, 14, 6, 14));
        tab.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                switchEditorFileTab(mixin);
            }
        });
        return tab;
    }

    private void switchEditorFileTab(boolean mixin) {
        if (mixin == mixinTab) return;
        if (mixinTab) {
            mixinSource = editor.getText();
        } else {
            scriptSource = editor.getText();
        }
        mixinTab = mixin;
        editor.setText(mixin ? mixinSource : scriptSource);
        styleFileTabs();
        if (settings.soundEffects()) Sound.tabSwitch();
        editor.requestFocusInWindow();
    }

    private void styleFileTabs() {
        boolean script = !mixinTab;
        scriptTabLabel.setForeground(script ? Theme.TEXT : Theme.FAINT);
        mixinTabLabel.setForeground(mixinTab ? Theme.TEXT : Theme.FAINT);
        scriptTabBar.setOn(script);
        mixinTabBar.setOn(mixinTab);
    }

    private void setEditorText(String text) {
        editor.setText(text);
        if (mixinTab) {
            mixinSource = text;
        } else {
            scriptSource = text;
        }
    }

    private static final class TabBar extends JPanel {
        private boolean on;

        TabBar(boolean on) {
            this.on = on;
            setOpaque(false);
            setPreferredSize(new Dimension(0, 2));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        }

        void setOn(boolean on) {
            this.on = on;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!on) return;
            Graphics2D g2 = (Graphics2D) g.create();
            Ui.aa(g2);
            g2.setPaint(new GradientPaint(0, 0, Theme.ACCENT, getWidth(), 0, Theme.ACCENT_2));
            g2.fill(Ui.round(0, 0, getWidth(), 2, 1));
            g2.dispose();
        }
    }

    private void buildConsoleCard() {
        consoleCard.setLayout(new BorderLayout());
        consoleCard.add(consoleHeader(), BorderLayout.NORTH);

        console.setEditable(false);
        console.setBackground(Theme.CODE_BG);
        console.setForeground(Theme.TEXT);
        console.setFont(Theme.mono(Font.PLAIN, 12));
        console.setBorder(new EmptyBorder(8, 12, 8, 12));

        JScrollPane scroll = new JScrollPane(console);
        ScrollBar.apply(scroll);
        scroll.getViewport().setBackground(Theme.CODE_BG);
        consoleCard.add(scroll, BorderLayout.CENTER);
    }

    private JComponent consoleHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER_SOFT));

        JLabel title = new JLabel("CONSOLE");
        title.setFont(Theme.ui(Font.BOLD, 11));
        title.setForeground(Theme.MUTED);
        title.setBorder(new EmptyBorder(9, 14, 9, 14));
        header.add(title, BorderLayout.WEST);

        GButton clear = new GButton("Clear", GButton.Style.SUBTLE);
        clear.setBorder(new EmptyBorder(7, 14, 7, 14));
        clear.addActionListener(e -> clearConsole());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 3));
        right.setOpaque(false);
        right.add(clear);
        header.add(right, BorderLayout.EAST);
        return header;
    }



    private void applyViews() {
        detach(editorCard);
        detach(consoleCard);
        detach(docsCard);
        detach(scriptsCard);
        detach(settingsCard);
        detach(injectModCard);
        content.removeAll();

        if (showInjectMod) {
            content.add(injectModCard, BorderLayout.CENTER);
        } else if (showDocs) {
            content.add(docsCard, BorderLayout.CENTER);
        } else if (showScripts) {
            content.add(scriptsCard, BorderLayout.CENTER);
        } else if (showSettings) {
            content.add(settingsCard, BorderLayout.CENTER);
        } else if (showEditor && showConsole) {
            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorCard, consoleCard);
            split.setBorder(null);
            split.setDividerSize(8);
            split.setResizeWeight(0.62);
            split.setContinuousLayout(true);
            split.setOpaque(false);
            content.add(split, BorderLayout.CENTER);
            SwingUtilities.invokeLater(() -> split.setDividerLocation(0.62));
        } else if (showEditor) {
            content.add(editorCard, BorderLayout.CENTER);
        } else if (showConsole) {
            content.add(consoleCard, BorderLayout.CENTER);
        } else {
            content.add(emptyView(), BorderLayout.CENTER);
        }

        content.revalidate();
        content.repaint();
        boolean base = !showDocs && !showScripts && !showSettings && !showInjectMod;
        if (editorNav != null) editorNav.setActive(showEditor && base);
        if (consoleNav != null) consoleNav.setActive(showConsole && base);
        if (docsNav != null) docsNav.setActive(showDocs);
        if (scriptsNav != null) scriptsNav.setActive(showScripts);
        if (injectModNav != null) injectModNav.setActive(showInjectMod);
        if (settingsNav != null) settingsNav.setActive(showSettings);
    }

    private void toggleEditor() {
        if (animating || System.currentTimeMillis() - lastTabSwitch < TAB_COOLDOWN_MS) return;
        lastTabSwitch = System.currentTimeMillis();
        showDocs = false;
        showScripts = false;
        showSettings = false;
        showInjectMod = false;
        showEditor = !showEditor;
        animateViewChange();
        playTabSound();
    }

    private void toggleConsole() {
        if (animating || System.currentTimeMillis() - lastTabSwitch < TAB_COOLDOWN_MS) return;
        lastTabSwitch = System.currentTimeMillis();
        showDocs = false;
        showScripts = false;
        showSettings = false;
        showInjectMod = false;
        showConsole = !showConsole;
        animateViewChange();
        playTabSound();
    }


    private void toggleDocs() {
        if (animating || System.currentTimeMillis() - lastTabSwitch < TAB_COOLDOWN_MS) return;
        lastTabSwitch = System.currentTimeMillis();
        if (showDocs) {
            showDocs = false;
            restorePanels();
        } else {
            savePanels();
            showDocs = true;
            showScripts = false;
            showSettings = false;
            showInjectMod = false;
            showEditor = false;
            showConsole = false;
        }
        animateViewChange();
        playTabSound();
    }


    private void toggleScripts() {
        if (animating || System.currentTimeMillis() - lastTabSwitch < TAB_COOLDOWN_MS) return;
        lastTabSwitch = System.currentTimeMillis();
        if (showScripts) {
            showScripts = false;
            restorePanels();
        } else {
            savePanels();
            scriptsCard.refresh();
            showScripts = true;
            showDocs = false;
            showSettings = false;
            showInjectMod = false;
            showEditor = false;
            showConsole = false;
        }
        animateViewChange();
        playTabSound();
    }


    private void toggleInjectMod() {
        if (animating || System.currentTimeMillis() - lastTabSwitch < TAB_COOLDOWN_MS) return;
        lastTabSwitch = System.currentTimeMillis();
        if (showInjectMod) {
            showInjectMod = false;
            restorePanels();
        } else {
            savePanels();
            showInjectMod = true;
            showDocs = false;
            showScripts = false;
            showSettings = false;
            showEditor = false;
            showConsole = false;
        }
        animateViewChange();
        playTabSound();
    }


    private void toggleSettings() {
        if (animating || System.currentTimeMillis() - lastTabSwitch < TAB_COOLDOWN_MS) return;
        lastTabSwitch = System.currentTimeMillis();
        if (showSettings) {
            showSettings = false;
            restorePanels();
        } else {
            savePanels();
            showSettings = true;
            showDocs = false;
            showScripts = false;
            showEditor = false;
            showConsole = false;
        }
        animateViewChange();
        playTabSound();
    }


    private void savePanels() {
        if (!showDocs && !showScripts && !showSettings && !showInjectMod) {
            savedEditor = showEditor;
            savedConsole = showConsole;
        }
    }

    private void restorePanels() {
        if (!showDocs && !showScripts && !showSettings && !showInjectMod) {
            showEditor = savedEditor;
            showConsole = savedConsole;
        }
    }

    private void animateViewChange() {
        if (animating) return;
        animating = true;

        JComponent oldContent = (JComponent) content.getComponent(0);
        content.removeAll();
        content.setOpaque(false);

        applyViews();

        JComponent newContent = (JComponent) content.getComponent(0);

        if (oldContent != null) {
            newContent.setOpaque(false);
            newContent.putClientProperty("alpha", 0f);

            Timer fadeTimer = new Timer(16, null);
            fadeTimer.addActionListener(e -> {
                float alpha = (float) newContent.getClientProperty("alpha");
                alpha += 0.1f;
                if (alpha >= 1f) {
                    alpha = 1f;
                    ((Timer) e.getSource()).stop();
                    animating = false;
                }
                newContent.putClientProperty("alpha", alpha);
                newContent.repaint();
            });
            fadeTimer.start();
        } else {
            animating = false;
        }
    }

    private void loadStarter(String code) {
        loadScript(code);
        appendConsole("[client] loaded the starter script into the editor");
    }

    private void loadScript(String code) {
        setEditorText(code);
        showDocs = false;
        showScripts = false;
        showSettings = false;
        showEditor = true;
        showConsole = true;
        applyViews();
    }





    private void openScript(ScriptHistory.Entry entry) {
        switchEditorFileTab("mixin".equals(entry.mode));
        loadScript(entry.code);
    }

    private JComponent emptyView() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        JLabel label = new JLabel("Choose a panel on the left");
        label.setFont(Theme.ui(Font.PLAIN, 13));
        label.setForeground(Theme.FAINT);
        panel.add(label);
        return panel;
    }

    private void detach(JComponent component) {
        if (component != null && component.getParent() != null) component.getParent().remove(component);
    }

    private void applyShape() {
        try {
            frame.setShape(new RoundRectangle2D.Double(
                    0, 0, frame.getWidth(), frame.getHeight(), 16, 16));
        } catch (Exception ignored) {
        }
    }



    private void onSettingChanged(String key) {
        if (key == null) return;
        switch (key) {
            case "theme" -> {
                Theme.apply(settings.theme());
                applyLookAndFeel();
                buildUi();
                renderConsole();
                appendConsole("[client] theme set to " + settings.theme().name().toLowerCase());
            }
            case "autoAttach" -> {
                if (processSelector != null) {
                    processSelector.setAutoAttach(settings.autoAttach());
                    if (settings.autoAttach()) processSelector.refresh();
                }
                appendConsole("[client] auto attach " + (settings.autoAttach() ? "enabled" : "disabled"));
            }
            default -> {
            }
        }
    }

    private void onProcessSelected(ProcessSelector.Proc proc) {
        if (proc == null || !proc.minecraft()) return;
        if (proc.id().equals(lastAttachedId)) return;
        if (frame == null || !frame.isShowing()) return;
        lastAttachedId = proc.id();
        

        fetchAndCacheUsername(proc, () -> {
            if (!settings.notifications()) return;
            Toast.show(frame, "Attached to Minecraft", "Ready to execute scripts", avatarUrlFor(proc));
        });
    }

    private void fetchAndCacheUsername(ProcessSelector.Proc proc, Runnable onDone) {
        new Thread(() -> {
            try {
                String usernameScript = "String __ign = mc.player != null ? mc.player.getGameProfile().getName() : \"\"; log(__ign);";
                String username = Injector.eval(proc.id(), usernameScript, 10);
                if (username != null && !username.isEmpty() && username.matches("[A-Za-z0-9_]{1,16}")) {
                    currentUsername = username;
                }
            } catch (Throwable t) {
            } finally {
                if (onDone != null) SwingUtilities.invokeLater(onDone);
            }
        }, "username-fetcher").start();
    }

    private void playTabSound() {
        if (settings.soundEffects()) Sound.tabSwitch();
    }

    private void startAutoAttachTimer() {
        autoAttachTimer = new Timer(4000, e -> {
            if (!settings.autoAttach() || processSelector == null) return;
            processSelector.refresh();
        });
        autoAttachTimer.start();
    }

    private void setStatus(StatusPill.State state) {
        pillState = state;
        if (statusPill != null) statusPill.setState(state);
    }

    private void onExecute() {
        ProcessSelector.Proc selected = processSelector.getSelected();
        if (selected == null && settings.autoAttach()) {
            processSelector.refresh();
            selected = processSelector.getSelected();
        }
        if (selected == null) {
            setStatus(StatusPill.State.ERROR);
            appendConsole("[client] no process selected");
            return;
        }
        final ProcessSelector.Proc target = selected;

        String code = editor.getText();
        if (code.isBlank()) {
            appendConsole("[client] nothing to execute");
            return;
        }

        final String mode = mixinTab ? "mixin" : "code";

        if (showDocs || showScripts || showSettings || !showConsole) {
            showDocs = false;
            showScripts = false;
            showSettings = false;
            showConsole = true;
            applyViews();
        }

        if (settings.soundEffects()) Sound.execute();

        String id = Long.toString(System.currentTimeMillis());
        executeButton.setEnabled(false);
        setStatus(StatusPill.State.WORKING);

        new Thread(() -> {
            boolean ok = true;
            try {
                appendConsole("[client] running as " + (mode.equals("mixin") ? "mixin (tick hook)" : "script code"));
                Injector.execute(target.id(), id, code, mode, this::appendConsole);
            } catch (Throwable t) {
                ok = false;
                appendConsole("[client] " + t.getClass().getSimpleName() + ": " + t.getMessage());
            } finally {
                boolean success = ok;
                SwingUtilities.invokeLater(() -> {
                    if (executeButton != null) executeButton.setEnabled(true);
                    setStatus(success ? StatusPill.State.ATTACHED : StatusPill.State.ERROR);
                    if (success) {
                        history.add(id, code, mode);
                        if (scriptsCard != null) scriptsCard.refresh();
                    }
                    if (settings.notifications()) {
                        String title = success ? "Executed" : "Execution failed";
                        String msg = success ? "Script ran on PID " + target.id()
                                : "See the console for details.";
                        Toast.show(frame, title, msg, success ? avatarUrlFor(target) : null);
                    }
                });
            }
        }, "executor-run").start();
    }

    private String currentUsername = null;

    private String avatarUrlFor(ProcessSelector.Proc proc) {
        String username = currentUsername;
        if (username == null || username.isEmpty()) {
            username = extractUsername(proc.name());
        }
        if (username != null && !username.isEmpty()) {
            return "https://mc-heads.net/avatar/" + username + "/32";
        }
        return "https://mc-heads.net/avatar/Steve/32";
    }

    private static String extractUsername(String processName) {
        if (processName == null) return null;





        String[] patterns = {
            " - ",
            " – ",
        };
        for (String sep : patterns) {
            int idx = processName.lastIndexOf(sep);
            if (idx >= 0 && idx + sep.length() < processName.length()) {
                String candidate = processName.substring(idx + sep.length()).trim();
                if (isValidUsername(candidate)) return candidate;
            }

            idx = processName.indexOf(sep);
            if (idx > 0) {
                String candidate = processName.substring(0, idx).trim();
                if (isValidUsername(candidate)) return candidate;
            }
        }
        return null;
    }

    private static boolean isValidUsername(String s) {
        if (s == null || s.isEmpty()) return false;
        if (s.length() > 16) return false;

        return s.matches("[A-Za-z0-9_]+");
    }

    private void clearConsole() {
        SwingUtilities.invokeLater(() -> {
            consoleLines.clear();
            console.setText("");
        });
    }

    private void appendConsole(String line) {
        SwingUtilities.invokeLater(() -> {
            consoleLines.add(line);
            appendConsoleRun(line);
        });
    }

    private void renderConsole() {
        console.setText("");
        for (String line : consoleLines) appendConsoleRun(line);
    }

    private void appendConsoleRun(String line) {
        try {
            StyledDocument document = console.getStyledDocument();
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setForeground(style, colorFor(line));
            StyleConstants.setFontFamily(style, Theme.mono(Font.PLAIN, 12).getFamily());
            StyleConstants.setFontSize(style, 12);
            document.insertString(document.getLength(), line + "\n", style);
            console.setCaretPosition(document.getLength());
        } catch (Exception ignored) {
        }
    }

    private Color colorFor(String line) {
        if (line.startsWith("[client]")) return Theme.MUTED;
        if (line.startsWith("[agent]")) return Theme.BLUE;
        if (line.startsWith("=====")) return Theme.ACCENT_HOVER;
        return Theme.TEXT;
    }
}
