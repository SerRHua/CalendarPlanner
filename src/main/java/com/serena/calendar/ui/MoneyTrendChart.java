package com.serena.calendar.ui;

import com.serena.calendar.model.DailyMoney;
import com.serena.calendar.service.Money;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Lightweight Swing line chart; no chart library is required. */
public final class MoneyTrendChart extends JPanel {
    private static final Color GREEN = new Color(33, 150, 83);
    private static final Color RED = new Color(220, 70, 70);
    private List<DailyMoney> data = List.of();

    public MoneyTrendChart() { setBackground(Color.WHITE); setPreferredSize(new Dimension(360, 280)); }
    public void setData(List<DailyMoney> data) { this.data = List.copyOf(data); repaint(); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics); Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int left = 58, right = 18, top = 42, bottom = 42, width = getWidth()-left-right, height = getHeight()-top-bottom;
        g.setColor(new Color(235,238,243));
        for (int i=0;i<=4;i++) { int y=top+i*height/4; g.drawLine(left,y,left+width,y); }
        long max = 1; for (DailyMoney d : data) max = Math.max(max, Math.max(d.spentCents(), d.earnedCents()));
        g.setColor(Color.GRAY); g.drawString(Money.format(max), 4, top+5); g.drawString("$0", 25, top+height+5);
        if (!data.isEmpty()) {
            drawLine(g, left, top, width, height, max, false, RED);
            drawLine(g, left, top, width, height, max, true, GREEN);
            g.setColor(Color.GRAY); g.drawString("1", left-3, top+height+20); g.drawString(String.valueOf(data.size()), left+width-8, top+height+20);
        }
        g.setColor(RED); g.fillOval(left,12,10,10); g.setColor(Color.DARK_GRAY); g.drawString("Spent", left+16,22);
        g.setColor(GREEN); g.fillOval(left+85,12,10,10); g.setColor(Color.DARK_GRAY); g.drawString("Earned / interest", left+101,22);
        g.dispose();
    }
    private void drawLine(Graphics2D g, int left, int top, int width, int height, long max, boolean earned, Color color) {
        g.setColor(color); g.setStroke(new BasicStroke(2.5f)); int previousX=-1, previousY=-1;
        for (int i=0;i<data.size();i++) {
            long value = earned ? data.get(i).earnedCents() : data.get(i).spentCents();
            int x = left + (data.size()==1 ? 0 : i*width/(data.size()-1)); int y = top+height-(int)Math.round(value*(double)height/max);
            if (previousX>=0) g.drawLine(previousX,previousY,x,y); g.fillOval(x-3,y-3,6,6); previousX=x; previousY=y;
        }
    }
}
