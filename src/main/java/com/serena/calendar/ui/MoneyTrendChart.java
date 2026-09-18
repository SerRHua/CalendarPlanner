package com.serena.calendar.ui;

import com.serena.calendar.model.DailyMoney;
import com.serena.calendar.service.Money;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/** Cumulative spent/earned chart with daily or monthly points and hover details. */
public final class MoneyTrendChart extends JPanel {
    private static final Color GREEN=new Color(33,150,83), RED=new Color(220,70,70), GRID=new Color(235,238,243);
    private static final int LEFT=72, RIGHT=24, TOP=52, BOTTOM=48;
    private List<Point> points=List.of();
    private boolean monthly;
    private int hoverIndex=-1;

    public MoneyTrendChart(){setBackground(Color.WHITE);setPreferredSize(new Dimension(500,320));ToolTipManager.sharedInstance().registerComponent(this);addMouseMotionListener(new MouseMotionAdapter(){@Override public void mouseMoved(MouseEvent e){int next=nearestIndex(e.getX());if(next!=hoverIndex){hoverIndex=next;repaint();}}});addMouseListener(new MouseAdapter(){@Override public void mouseExited(MouseEvent e){hoverIndex=-1;repaint();}});}

    public void setData(List<DailyMoney> data,boolean groupByMonth){monthly=groupByMonth;List<DailyMoney> buckets=groupByMonth?monthlyBuckets(data):data;long spent=0,earned=0;List<Point> built=new ArrayList<>();for(DailyMoney item:buckets){spent+=item.spentCents();earned+=item.earnedCents();built.add(new Point(item.date(),spent,earned));}points=List.copyOf(built);hoverIndex=-1;repaint();}
    public void setData(List<DailyMoney> data){setData(data,false);}

    private static List<DailyMoney> monthlyBuckets(List<DailyMoney> source){Map<YearMonth,long[]> grouped=new LinkedHashMap<>();for(DailyMoney d:source){long[] values=grouped.computeIfAbsent(YearMonth.from(d.date()),key->new long[2]);values[0]+=d.spentCents();values[1]+=d.earnedCents();}List<DailyMoney> result=new ArrayList<>();grouped.forEach((month,values)->result.add(new DailyMoney(month.atDay(1),values[0],values[1])));return result;}

    @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);int width=Math.max(1,getWidth()-LEFT-RIGHT),height=Math.max(1,getHeight()-TOP-BOTTOM);long max=1;for(Point p:points)max=Math.max(max,Math.max(p.spent,p.earned));
        g.setColor(GRID);for(int i=0;i<=4;i++){int y=TOP+i*height/4;g.drawLine(LEFT,y,LEFT+width,y);long value=Math.round(max*(4-i)/4.0);g.setColor(Color.GRAY);g.drawString(Money.format(value),6,y+5);g.setColor(GRID);}
        if(!points.isEmpty()){drawSeries(g,width,height,max,false,RED);drawSeries(g,width,height,max,true,GREEN);drawXAxis(g,width,height);if(hoverIndex>=0&&hoverIndex<points.size()){int x=xAt(hoverIndex,width);g.setColor(new Color(120,130,150,100));g.drawLine(x,TOP,x,TOP+height);drawHighlight(g,x,yAt(points.get(hoverIndex).spent,height,max),RED);drawHighlight(g,x,yAt(points.get(hoverIndex).earned,height,max),GREEN);}}
        g.setColor(RED);g.fillOval(LEFT,14,10,10);g.setColor(Color.DARK_GRAY);g.drawString("Cumulative spent",LEFT+16,24);g.setColor(GREEN);g.fillOval(LEFT+145,14,10,10);g.setColor(Color.DARK_GRAY);g.drawString("Cumulative earned / interest",LEFT+161,24);g.dispose();}

    private void drawSeries(Graphics2D g,int width,int height,long max,boolean earned,Color color){g.setColor(color);g.setStroke(new BasicStroke(2.7f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));int oldX=-1,oldY=-1;for(int i=0;i<points.size();i++){long value=earned?points.get(i).earned:points.get(i).spent;int x=xAt(i,width),y=yAt(value,height,max);if(oldX>=0)g.drawLine(oldX,oldY,x,y);if(points.size()<=40)g.fillOval(x-3,y-3,6,6);oldX=x;oldY=y;}}
    private void drawXAxis(Graphics2D g,int width,int height){g.setColor(Color.GRAY);int labels=Math.min(points.size(),monthly?12:7);for(int j=0;j<labels;j++){int i=labels==1?0:(int)Math.round(j*(points.size()-1.0)/(labels-1));String text=monthly?points.get(i).date.format(DateTimeFormatter.ofPattern("MMM yy")):points.get(i).date.format(DateTimeFormatter.ofPattern("MMM d"));int x=xAt(i,width);g.drawString(text,Math.max(LEFT,x-g.getFontMetrics().stringWidth(text)/2),TOP+height+24);}}
    private void drawHighlight(Graphics2D g,int x,int y,Color color){g.setColor(Color.WHITE);g.fillOval(x-6,y-6,12,12);g.setColor(color);g.setStroke(new BasicStroke(3f));g.drawOval(x-5,y-5,10,10);}
    private int xAt(int index,int width){return LEFT+(points.size()<=1?0:(int)Math.round(index*width/(points.size()-1.0)));}private int yAt(long value,int height,long max){return TOP+height-(int)Math.round(value*(double)height/max);}
    private int nearestIndex(int mouseX){if(points.isEmpty()||mouseX<LEFT||mouseX>getWidth()-RIGHT)return-1;double ratio=(mouseX-LEFT)/(double)Math.max(1,getWidth()-LEFT-RIGHT);return Math.max(0,Math.min(points.size()-1,(int)Math.round(ratio*(points.size()-1))));}
    @Override public String getToolTipText(MouseEvent e){int i=nearestIndex(e.getX());if(i<0)return null;Point p=points.get(i);String period=monthly?p.date.format(DateTimeFormatter.ofPattern("MMMM yyyy")):p.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"));return "<html><b>"+period+"</b><br><font color='#dc4b4b'>Cumulative spent: "+Money.format(p.spent)+"</font><br><font color='#1f9d60'>Cumulative earned: "+Money.format(p.earned)+"</font></html>";}
    private record Point(LocalDate date,long spent,long earned){}
}
