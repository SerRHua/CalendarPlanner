package com.serena.calendar.service;

import com.serena.calendar.db.Database;
import com.serena.calendar.model.Reminder;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.concurrent.*;

/** Checks MySQL periodically and publishes due reminders through the OS tray. */
public final class NotificationService implements AutoCloseable {
    private final Database db;
    private final JFrame frame;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "reminder-checker"); t.setDaemon(false); return t; });
    private TrayIcon trayIcon;

    public NotificationService(Database db, JFrame frame, Runnable quitAction) {
        this.db = db; this.frame = frame;
        if (SystemTray.isSupported()) {
            PopupMenu menu = new PopupMenu();
            MenuItem open = new MenuItem("Open Calendar Planner"); open.addActionListener(e -> SwingUtilities.invokeLater(() -> { frame.setVisible(true); frame.setExtendedState(JFrame.NORMAL); frame.toFront(); }));
            MenuItem quit = new MenuItem("Quit"); quit.addActionListener(e -> SwingUtilities.invokeLater(quitAction)); menu.add(open); menu.addSeparator(); menu.add(quit);
            trayIcon = new TrayIcon(icon(), "Calendar Planner", menu); trayIcon.setImageAutoSize(true); trayIcon.addActionListener(open.getActionListeners()[0]);
            try { SystemTray.getSystemTray().add(trayIcon); } catch (AWTException e) { trayIcon = null; }
        }
    }
    public boolean keepsRunningInTray() { return trayIcon != null; }
    public void start() { scheduler.scheduleWithFixedDelay(this::check, 2, 30, TimeUnit.SECONDS); }
    private void check() {
        try {
            for (Reminder reminder : db.dueReminders()) {
                if (trayIcon != null) trayIcon.displayMessage(reminder.title(), reminder.message(), TrayIcon.MessageType.INFO);
                db.markNotified(reminder);
            }
        } catch (SQLException ignored) { /* Try again after the next interval. */ }
    }
    private static Image icon() {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB); Graphics2D g = image.createGraphics();
        g.setColor(new Color(65,105,225)); g.fillRoundRect(2,4,28,26,7,7); g.setColor(Color.WHITE); g.fillRect(6,11,20,15); g.setColor(new Color(65,105,225)); g.drawString("17",9,23); g.dispose(); return image;
    }
    @Override public void close() {
        scheduler.shutdownNow(); if (trayIcon != null) SystemTray.getSystemTray().remove(trayIcon);
    }
}
