package com.vora.executor.ui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.function.Consumer;


public class DocsPanel extends RoundedPanel {

    
    public static final String STARTER = """
            // Vora executor
            // https://discord.gg/Nm2K5Mk72M
            mc.player.sendMessage(net.minecraft.text.Text.literal("hi"), false);
            """;

    public static final String MIXIN_EXAMPLE = """
            // Vora executor
            // https://discord.gg/Nm2K5Mk72M
            mc.player.sendMessage(net.minecraft.text.Text.literal("hi"), false);
            """;

    public DocsPanel(Consumer<String> loadStarter) {
        super(Theme.CODE_BG, 12);
        border(Theme.BORDER_SOFT);
        setLayout(new BorderLayout());
        add(header(loadStarter), BorderLayout.NORTH);
        add(body(), BorderLayout.CENTER);
    }

    private JComponent header(Consumer<String> loadStarter) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER_SOFT));

        JLabel title = new JLabel("DOCUMENTATION");
        title.setFont(Theme.ui(Font.BOLD, 11));
        title.setForeground(Theme.MUTED);
        title.setBorder(new EmptyBorder(9, 14, 9, 14));
        header.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 3));
        right.setOpaque(false);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JComponent body() {
        JTextPane pane = new JTextPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setBackground(Theme.CODE_BG);
        pane.setForeground(Theme.TEXT);
        pane.setBorder(new EmptyBorder(16, 20, 20, 20));

        String text = hex(Theme.TEXT);
        StyleSheet sheet = ((HTMLDocument) pane.getDocument()).getStyleSheet();
        sheet.addRule("body { font-family: SansSerif; font-size: 14pt; color: " + text + "; }");
        sheet.addRule("p { margin-top: 6pt; margin-bottom: 6pt; }");
        sheet.addRule("li { margin-top: 3pt; margin-bottom: 3pt; }");
        sheet.addRule("ul { margin-top: 4pt; margin-bottom: 8pt; }");
        sheet.addRule("ol { margin-top: 4pt; margin-bottom: 8pt; }");
        sheet.addRule("h1 { font-size: 25pt; margin-bottom: 10pt; }");
        sheet.addRule("h2 { font-size: 17pt; margin-top: 18pt; margin-bottom: 6pt; }");
        sheet.addRule("h3 { font-size: 14pt; margin-top: 10pt; margin-bottom: 4pt; }");
        sheet.addRule("pre { font-family: Monospaced; font-size: 12pt; color: " + text + "; }");
        sheet.addRule("code { font-family: Monospaced; color: " + text + "; }");
        sheet.addRule("td { padding: 6pt; }");

        pane.setText(load());

        StyledDocument document = pane.getStyledDocument();
        MutableAttributeSet textAttrs = new SimpleAttributeSet();
        StyleConstants.setForeground(textAttrs, Theme.TEXT);
        document.setCharacterAttributes(0, document.getLength(), textAttrs, false);
        document.setParagraphAttributes(0, document.getLength(), textAttrs, false);

        pane.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(pane);
        ScrollBar.apply(scroll);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.CODE_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private static String hex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static String load() {
        return DocsContent.HTML
                .replace("{{STARTER}}", escape(STARTER))
                .replace("{{MIXIN_EXAMPLE}}", escape(MIXIN_EXAMPLE))
                .replace("{{CODE_BG}}", hex(Theme.CODE_BG))
                .replace("{{BORDER}}", hex(Theme.BORDER))
                .replace("{{MUTED}}", hex(Theme.MUTED));
    }

    private static String escape(String code) {
        return code.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
