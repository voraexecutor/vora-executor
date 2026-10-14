package com.vora.executor.ui;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.concurrent.Executors;

public final class Toast {

    private Toast() {
    }

    public static void show(JFrame owner, String title, String message) {
        show(owner, title, message, null);
    }

    public static void show(JFrame owner, String title, String message, String avatarUrl) {
        if (owner == null || !owner.isShowing()) return;

        JWindow window = new JWindow(owner);
        try {
            window.setBackground(new Color(0, 0, 0, 0));
        } catch (Throwable ignored) {
        }

        RoundedPanel card = new RoundedPanel(Theme.ELEVATED, 12).border(Theme.BORDER);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));


        JLabel avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(32, 32));
        avatarLabel.setMinimumSize(new Dimension(32, 32));
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            loadAvatarAsync(avatarUrl, avatarLabel);
        }


        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.ui(Font.BOLD, 12));
        titleLabel.setForeground(Theme.TEXT);
        titleLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(Theme.ui(Font.PLAIN, 11));
        messageLabel.setForeground(Theme.MUTED);
        messageLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);

        text.add(titleLabel);
        text.add(messageLabel);


        JPanel center = new JPanel(new BorderLayout(12, 0));
        center.setOpaque(false);
        center.add(avatarLabel, BorderLayout.WEST);
        center.add(text, BorderLayout.CENTER);

        card.add(center, BorderLayout.CENTER);

        window.setContentPane(card);
        window.pack();
        if (window.getWidth() < 300) window.setSize(300, window.getHeight());

        Dimension size = window.getSize();
        Point location = owner.getLocationOnScreen();
        window.setLocation(
                location.x + owner.getWidth() - size.width - 20,
                location.y + owner.getHeight() - size.height - 20);

        window.setAlwaysOnTop(true);

        boolean fade;
        try {
            window.setOpacity(0f);
            fade = true;
        } catch (Throwable t) {
            fade = false;
        }
        window.setVisible(true);

        if (!fade) {
            Timer close = new Timer(2600, e -> window.dispose());
            close.setRepeats(false);
            close.start();
            return;
        }

        final int inSteps = 10;
        final int holdSteps = 150;
        final float[] tick = { 0f };
        Timer timer = new Timer(16, null);
        timer.addActionListener(e -> {
            tick[0]++;
            float t = tick[0];
            try {
                if (t <= inSteps) {
                    window.setOpacity(t / inSteps);
                } else if (t <= inSteps + holdSteps) {
                    window.setOpacity(1f);
                } else {
                    float out = (t - inSteps - holdSteps) / inSteps;
                    if (out >= 1f) {
                        timer.stop();
                        window.dispose();
                    } else {
                        window.setOpacity(1f - out);
                    }
                }
            } catch (Throwable ex) {
                timer.stop();
                window.dispose();
            }
        });
        timer.start();
    }

    private static void loadAvatarAsync(String url, JLabel label) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                BufferedImage img = ImageIO.read(new URL(url));
                if (img != null) {
                    Image scaled = img.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
                    BufferedImage rounded = makeRounded(scaled, 32, 32);
                    ImageIcon icon = new ImageIcon(rounded);
                    SwingUtilities.invokeLater(() -> label.setIcon(icon));
                }
            } catch (Throwable ignored) {
            }
        });
    }

    private static BufferedImage makeRounded(Image img, int w, int h) {
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        java.awt.geom.RoundRectangle2D clip = new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, 6, 6);
        g2.setClip(clip);
        g2.drawImage(img, 0, 0, w, h, null);
        g2.dispose();
        return output;
    }
}
