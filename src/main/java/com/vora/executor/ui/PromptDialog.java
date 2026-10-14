package com.vora.executor.ui;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;







public final class PromptDialog {

    private PromptDialog() {
    }





    public static String ask(Component anchor, String title, String message, String initialValue) {
        Window owner = anchor != null ? SwingUtilities.getWindowAncestor(anchor) : null;

        final String[] result = { null };

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

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.ui(Font.BOLD, 13));
        titleLabel.setForeground(Theme.TEXT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(Theme.ui(Font.PLAIN, 11));
        messageLabel.setForeground(Theme.MUTED);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        body.add(titleLabel);
        body.add(messageLabel);
        body.add(Box.createVerticalStrut(12));

        RoundedPanel fieldBox = new RoundedPanel(Theme.PANEL, 9).border(Theme.BORDER);
        fieldBox.setLayout(new BorderLayout());
        JTextField field = new JTextField(initialValue == null ? "" : initialValue);
        field.setOpaque(false);
        field.setBorder(new EmptyBorder(8, 10, 8, 10));
        field.setFont(Theme.ui(Font.PLAIN, 12));
        field.setForeground(Theme.TEXT);
        field.setCaretColor(Theme.CARET);
        field.setSelectionColor(Theme.SELECTION);
        field.setSelectedTextColor(Theme.TEXT);
        fieldBox.add(field, BorderLayout.CENTER);

        body.add(fieldBox);
        card.add(body, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);

        GButton cancel = new GButton("Cancel", GButton.Style.GHOST);
        cancel.setBorder(new EmptyBorder(7, 18, 7, 18));
        cancel.addActionListener(e -> dialog.dispose());

        GButton ok = new GButton("Rename", GButton.Style.ACCENT);
        ok.setBorder(new EmptyBorder(7, 18, 7, 18));
        ok.addActionListener(e -> {
            String text = field.getText() == null ? "" : field.getText().trim();
            if (!text.isEmpty()) result[0] = text;
            dialog.dispose();
        });

        footer.add(cancel);
        footer.add(ok);
        card.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(card);
        dialog.pack();
        if (dialog.getWidth() < 360) {
            dialog.setSize(360, dialog.getHeight());
        }


        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "vora.close");
        dialog.getRootPane().getActionMap().put("vora.close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        dialog.getRootPane().setDefaultButton(ok);

        if (owner != null && owner.isShowing()) {
            dialog.setLocationRelativeTo(owner);
        } else {
            dialog.setLocationRelativeTo(null);
        }


        SwingUtilities.invokeLater(() -> {
            field.selectAll();
            field.requestFocusInWindow();
        });

        dialog.setVisible(true);
        return result[0];
    }
}