package com.vora.executor.ui;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;







public final class ConfirmDialog {

    private ConfirmDialog() {
    }

    public static boolean ask(Window anchor, String title, String message) {
        Window owner = anchor != null ? SwingUtilities.getWindowAncestor(anchor) : null;

        final boolean[] choice = { false };

        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setAlwaysOnTop(true);
        try {
            dialog.setBackground(new Color(0, 0, 0, 0));
        } catch (Throwable ignored) {
        }

        RoundedPanel card = new RoundedPanel(Theme.ELEVATED, 14).border(Theme.BORDER);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(18, 20, 14, 20));


        JPanel head = new JPanel(new BorderLayout(12, 0));
        head.setOpaque(false);

        JLabel icon = new JLabel(warningIcon());
        head.add(icon, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.ui(Font.BOLD, 13));
        titleLabel.setForeground(Theme.TEXT);

        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setFocusable(false);
        messageArea.setOpaque(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(Theme.ui(Font.PLAIN, 12));
        messageArea.setForeground(Theme.MUTED);
        messageArea.setColumns(46);
        messageArea.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        text.add(titleLabel);
        text.add(messageArea);
        head.add(text, BorderLayout.CENTER);

        card.add(head, BorderLayout.CENTER);


        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);

        GButton cancel = new GButton("Cancel", GButton.Style.GHOST);
        cancel.setBorder(new EmptyBorder(7, 18, 7, 18));
        cancel.addActionListener(e -> dialog.dispose());

        GButton confirm = new GButton("Remove", GButton.Style.SUBTLE);
        confirm.danger();
        confirm.setBorder(new EmptyBorder(7, 18, 7, 18));
        confirm.addActionListener(e -> {
            choice[0] = true;
            dialog.dispose();
        });

        footer.add(cancel);
        footer.add(confirm);
        card.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(card);
        dialog.pack();

        if (dialog.getWidth() < 380) {
            dialog.setSize(380, dialog.getHeight());
        }


        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "vora.close");
        dialog.getRootPane().getActionMap().put("vora.close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        dialog.getRootPane().setDefaultButton(confirm);

        if (owner != null && owner.isShowing()) {
            dialog.setLocationRelativeTo(owner);
        } else {
            dialog.setLocationRelativeTo(null);
        }

        dialog.setVisible(true);
        return choice[0];
    }

    private static Icon warningIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                Ui.aa(g);
                int s = 36;
                g.setColor(new Color(Theme.AMBER.getRed(), Theme.AMBER.getGreen(),
                        Theme.AMBER.getBlue(), 36));
                g.fill(Ui.round(x, y, s, s, 11));
                g.setColor(Theme.AMBER);
                g.setFont(Theme.ui(Font.BOLD, 17));
                FontMetrics fm = g.getFontMetrics();
                String mark = "!";
                int tw = fm.stringWidth(mark);
                int th = fm.getAscent();
                g.drawString(mark, x + (s - tw) / 2f, y + (s + th) / 2f - 2);
                g.dispose();
            }

            @Override
            public int getIconWidth() {
                return 36;
            }

            @Override
            public int getIconHeight() {
                return 36;
            }
        };
    }
}