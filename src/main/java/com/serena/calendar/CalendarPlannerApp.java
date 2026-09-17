package com.serena.calendar;

import com.serena.calendar.db.Database;
import com.serena.calendar.ui.MainFrame;

import javax.swing.*;

public final class CalendarPlannerApp {
    private CalendarPlannerApp() {}
    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                Database database = new Database();
                MainFrame frame = new MainFrame(database);
                frame.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Could not start Calendar Planner:\n" + e.getMessage(), "Startup error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
