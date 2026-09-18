package com.serena.calendar.ui;

import com.serena.calendar.db.Database;
import com.serena.calendar.model.*;
import com.serena.calendar.service.Money;
import com.serena.calendar.service.NotificationService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.time.*;
import java.time.format.*;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

public final class MainFrame extends JFrame {
    private static final Color INK = new Color(29, 39, 59), MUTED = new Color(113, 124, 145);
    private static final Color BG = new Color(245, 247, 251), CARD = Color.WHITE, BORDER = new Color(226, 231, 240);
    private static final Color BLUE = new Color(76, 111, 255), BLUE_SOFT = new Color(235, 239, 255);
    private static final Color GREEN = new Color(31, 157, 96), GREEN_SOFT = new Color(231, 248, 239);
    private static final Color RED = new Color(220, 75, 75), RED_SOFT = new Color(255, 238, 238);
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter REMINDER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Database db;
    private final NotificationService notifications;
    private YearMonth displayedMonth = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();
    private LocalDate taskAnchor = LocalDate.now(), financeAnchor = LocalDate.now(), trendAnchor = LocalDate.now();
    private PeriodRange taskRange = PeriodRange.MONTH, financeRange = PeriodRange.MONTH;
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pages = new JPanel(pageLayout);
    private final Map<String,JButton> navButtons = new LinkedHashMap<>();
    private final JLabel pageTitle = label("Calendar", 27, Font.BOLD, INK);
    private final JLabel pageSubtitle = label("Plan your day and stay on track", 13, Font.PLAIN, MUTED);
    private final JLabel monthTitle = label("", 19, Font.BOLD, INK);
    private final JLabel clockLabel = label("", 12, Font.BOLD, MUTED);
    private final JLabel selectedDateLabel = label("", 16, Font.BOLD, INK);
    private final JLabel taskPeriodLabel = label("", 14, Font.BOLD, INK);
    private final JLabel financeDateLabel = label("", 15, Font.BOLD, INK);
    private final JLabel trendMonthLabel = label("", 15, Font.BOLD, INK);
    private final JPanel calendarGrid = new JPanel(new GridLayout(0, 7, 7, 7));
    private final DefaultListModel<CalendarEvent> eventModel = new DefaultListModel<>();
    private final DefaultListModel<TodoItem> todoModel = new DefaultListModel<>();
    private final DefaultListModel<Expense> expenseModel = new DefaultListModel<>();
    private final DefaultListModel<Income> incomeModel = new DefaultListModel<>();
    private final JList<CalendarEvent> eventList = modernList(eventModel);
    private final JList<TodoItem> todoList = modernList(todoModel);
    private final JList<Expense> expenseList = modernList(expenseModel);
    private final JList<Income> incomeList = modernList(incomeModel);
    private final JLabel dayTotal = valueLabel(INK), weekTotal = valueLabel(INK), monthTotal = valueLabel(INK);
    private final JLabel overallSpent = valueLabel(RED), overallEarned = valueLabel(GREEN), overallSaved = valueLabel(GREEN);
    private final JButton todayButton = outlineButton("Go to today");
    private final MoneyTrendChart trendChart = new MoneyTrendChart();
    private TrendRange trendRange = TrendRange.MONTH;
    private final Timer clockTimer = new Timer(1000, e -> updateClock());

    public MainFrame(Database db) {
        super("Calendar Planner"); this.db = db;
        setMinimumSize(new Dimension(1120, 720)); setSize(1280, 820); setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); setContentPane(buildApp());
        notifications = new NotificationService(db, this, this::quitApplication);
        if (notifications.keepsRunningInTray()) setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent e) { quitApplication(); } });
        notifications.start(); updateClock(); clockTimer.start(); refreshAll(); showPage("calendar", "Calendar", "Plan your day and stay on track");
    }

    private JComponent buildApp() {
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(BG); root.add(sidebar(), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout(0, 18)); main.setOpaque(false); main.setBorder(new EmptyBorder(24, 26, 24, 26));
        JPanel heading = new JPanel(new BorderLayout()); heading.setOpaque(false);
        JPanel words = stack(pageTitle, pageSubtitle, 3); heading.add(words, BorderLayout.WEST);
        todayButton.addActionListener(e -> { selectedDate=LocalDate.now(); displayedMonth=YearMonth.now(); refreshAll(); }); heading.add(todayButton, BorderLayout.EAST);
        main.add(heading, BorderLayout.NORTH);
        pages.setOpaque(false); pages.add(calendarPage(), "calendar"); pages.add(tasksPage(), "tasks"); pages.add(financePage(), "finances"); pages.add(trendsPage(), "trends"); main.add(pages, BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER); return root;
    }

    private JComponent sidebar() {
        JPanel side = new JPanel(); side.setBackground(new Color(22, 31, 50)); side.setPreferredSize(new Dimension(210, 0)); side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS)); side.setBorder(new EmptyBorder(28, 18, 22, 18));
        JLabel brand = label("Dayflow", 25, Font.BOLD, Color.WHITE); brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tagline = label("Personal planner", 12, Font.PLAIN, new Color(157, 168, 190)); tagline.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(brand); side.add(Box.createVerticalStrut(2)); side.add(tagline); side.add(Box.createVerticalStrut(38));
        side.add(navButton("Calendar", "calendar", "Plan your day and stay on track")); side.add(Box.createVerticalStrut(8));
        side.add(navButton("Tasks", "tasks", "Your focused to-do list")); side.add(Box.createVerticalStrut(8));
        side.add(navButton("Finances", "finances", "Track spending, earnings, and savings")); side.add(Box.createVerticalStrut(8));
        side.add(navButton("Trends", "trends", "See your monthly money patterns")); side.add(Box.createVerticalGlue());
        JLabel note = label("Close the window to keep\nreminders running", 11, Font.PLAIN, new Color(157,168,190)); note.setAlignmentX(Component.LEFT_ALIGNMENT); side.add(note); return side;
    }

    private JButton navButton(String text, String page, String subtitle) {
        JButton b = new JButton(text); b.setAlignmentX(Component.LEFT_ALIGNMENT); b.setMaximumSize(new Dimension(174, 44)); b.setHorizontalAlignment(SwingConstants.LEFT); b.setBorder(new EmptyBorder(0,15,0,15)); b.setForeground(new Color(218,224,238)); b.setBackground(new Color(22,31,50)); b.setOpaque(true); b.setFocusPainted(false); b.setFont(b.getFont().deriveFont(Font.BOLD,14f));
        navButtons.put(page,b); b.addActionListener(e -> showPage(page,text,subtitle)); return b;
    }
    private void showPage(String page, String title, String subtitle) { pageTitle.setText(title); pageSubtitle.setText(subtitle); todayButton.setVisible(page.equals("calendar")); navButtons.forEach((name,button)->{boolean active=name.equals(page);button.setBackground(active?new Color(49,63,91):new Color(22,31,50));button.setForeground(active?Color.WHITE:new Color(218,224,238));}); pageLayout.show(pages,page); }

    private JComponent calendarPage() {
        JPanel p = new JPanel(new BorderLayout(18,0)); p.setOpaque(false); p.add(calendarCard(), BorderLayout.CENTER);
        RoundedPanel details = card(new BorderLayout(0,12)); details.setPreferredSize(new Dimension(330,0));
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false); top.add(selectedDateLabel, BorderLayout.CENTER); JButton add = primaryButton("+ Add event"); add.addActionListener(e -> addEvent()); top.add(add,BorderLayout.SOUTH); details.add(top,BorderLayout.NORTH);
        eventList.setCellRenderer(new EventRenderer()); details.add(scroll(eventList),BorderLayout.CENTER); JButton delete=outlineButton("Delete selected"); delete.addActionListener(e -> deleteEvent()); details.add(delete,BorderLayout.SOUTH); p.add(details,BorderLayout.EAST); return p;
    }
    private JComponent calendarCard() {
        RoundedPanel p=card(new BorderLayout(0,15)); JPanel nav=new JPanel(new BorderLayout()); nav.setOpaque(false);
        JButton previous=outlineButton("‹"), next=outlineButton("›"),choose=outlineButton("Choose date"); previous.addActionListener(e->{displayedMonth=displayedMonth.minusMonths(1);renderCalendar();}); next.addActionListener(e->{displayedMonth=displayedMonth.plusMonths(1);renderCalendar();});choose.addActionListener(e->{LocalDate date=chooseDate("Choose calendar date",selectedDate);if(date!=null){selectedDate=date;displayedMonth=YearMonth.from(date);refreshAll();}});nav.add(previous,BorderLayout.WEST); JPanel center=stack(monthTitle,clockLabel,3);monthTitle.setHorizontalAlignment(SwingConstants.CENTER);clockLabel.setHorizontalAlignment(SwingConstants.CENTER);nav.add(center,BorderLayout.CENTER);JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));right.setOpaque(false);right.add(choose);right.add(next);nav.add(right,BorderLayout.EAST); p.add(nav,BorderLayout.NORTH); calendarGrid.setOpaque(false); p.add(calendarGrid,BorderLayout.CENTER); return p;
    }

    private JComponent tasksPage() {
        RoundedPanel p=card(new BorderLayout(0,14)); JPanel top=new JPanel(new BorderLayout(12,0)); top.setOpaque(false); top.add(taskRangeControls(),BorderLayout.WEST);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); actions.setOpaque(false); JButton add=primaryButton("+ Add task"), toggle=outlineButton("Done / undo"), delete=outlineButton("Delete"); add.addActionListener(e->addTodo()); toggle.addActionListener(e->toggleTodo()); delete.addActionListener(e->deleteTodo()); actions.add(toggle);actions.add(delete);actions.add(add);top.add(actions,BorderLayout.EAST);p.add(top,BorderLayout.NORTH);
        todoList.setCellRenderer(new TodoRenderer()); todoList.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){if(e.getClickCount()==2)toggleTodo();}});p.add(scroll(todoList),BorderLayout.CENTER);return p;
    }

    private JComponent financePage() {
        JPanel p=new JPanel(new BorderLayout(0,16));p.setOpaque(false); JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);top.add(financeRangeControls(),BorderLayout.WEST);p.add(top,BorderLayout.NORTH);
        JPanel summary=new JPanel(new GridLayout(1,3,14,0));summary.setOpaque(false);summary.setPreferredSize(new Dimension(0,100));summary.add(metricCard("Overall spent",overallSpent,RED_SOFT));summary.add(metricCard("Overall earned",overallEarned,GREEN_SOFT));summary.add(metricCard("Overall saved",overallSaved,BLUE_SOFT));
        JPanel lower=new JPanel(new GridLayout(1,2,16,0));lower.setOpaque(false);lower.add(transactionCard("Money spent",expenseList,"+ Add expense",this::addExpense,this::deleteExpense,false));lower.add(transactionCard("Money earned",incomeList,"+ Add income",this::addIncome,this::deleteIncome,true));
        JPanel periods=new JPanel(new GridLayout(1,3,12,0));periods.setOpaque(false);periods.setPreferredSize(new Dimension(0,88));periods.add(metricCard("Period spent",dayTotal,CARD));periods.add(metricCard("Period earned",weekTotal,CARD));periods.add(metricCard("Period saved",monthTotal,CARD));
        JPanel body=new JPanel(new BorderLayout(0,14));body.setOpaque(false);body.add(summary,BorderLayout.NORTH);body.add(lower,BorderLayout.CENTER);body.add(periods,BorderLayout.SOUTH);p.add(body,BorderLayout.CENTER);return p;
    }
    private JComponent transactionCard(String title,JList<?> list,String addText,Runnable addAction,Runnable deleteAction,boolean income) {
        RoundedPanel p=card(new BorderLayout(0,10));JLabel heading=label(title,17,Font.BOLD,income?GREEN:INK);p.add(heading,BorderLayout.NORTH);if(income)list.setCellRenderer(new IncomeRenderer());else list.setCellRenderer(new ExpenseRenderer());p.add(scroll(list),BorderLayout.CENTER);
        JPanel actions=new JPanel(new GridLayout(1,2,8,0));actions.setOpaque(false);JButton add=primaryButton(addText),delete=outlineButton("Delete");add.addActionListener(e->addAction.run());delete.addActionListener(e->deleteAction.run());actions.add(add);actions.add(delete);p.add(actions,BorderLayout.SOUTH);return p;
    }

    private JComponent trendsPage() {
        RoundedPanel p=card(new BorderLayout(0,14));JPanel top=new JPanel(new BorderLayout());top.setOpaque(false);JPanel navigation=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));navigation.setOpaque(false);JButton previous=outlineButton("‹"),next=outlineButton("›"),choose=outlineButton("Choose date");previous.addActionListener(e->{shiftTrend(-1);refreshTrend();});next.addActionListener(e->{shiftTrend(1);refreshTrend();});choose.addActionListener(e->{LocalDate date=chooseDate("Choose trend date",trendAnchor);if(date!=null){trendAnchor=date;refreshTrend();}});navigation.add(previous);navigation.add(trendMonthLabel);navigation.add(next);navigation.add(choose);top.add(navigation,BorderLayout.WEST);
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));controls.setOpaque(false);ButtonGroup group=new ButtonGroup();for(TrendRange range:TrendRange.values()){JToggleButton button=trendButton(range);group.add(button);controls.add(button);if(range==trendRange)button.setSelected(true);}top.add(controls,BorderLayout.EAST);p.add(top,BorderLayout.NORTH);p.add(trendChart,BorderLayout.CENTER);return p;
    }
    private JToggleButton trendButton(TrendRange range){JToggleButton b=new JToggleButton(range.label);b.setFocusPainted(false);b.setBackground(CARD);b.setForeground(INK);b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER,1,true),new EmptyBorder(7,12,7,12)));b.addActionListener(e->{trendRange=range;refreshTrend();});return b;}
    private JComponent taskRangeControls(){JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));p.setOpaque(false);JButton previous=outlineButton("‹"),next=outlineButton("›"),choose=outlineButton("Choose date");previous.addActionListener(e->{taskAnchor=shift(taskAnchor,taskRange,-1);refreshTasks();});next.addActionListener(e->{taskAnchor=shift(taskAnchor,taskRange,1);refreshTasks();});choose.addActionListener(e->{LocalDate date=chooseDate("Choose task date",taskAnchor);if(date!=null){taskAnchor=date;refreshTasks();}});p.add(previous);p.add(taskPeriodLabel);p.add(next);p.add(choose);ButtonGroup group=new ButtonGroup();for(PeriodRange range:PeriodRange.values()){JToggleButton button=periodButton(range,()->{taskRange=range;refreshTasks();});group.add(button);p.add(button);if(range==taskRange)button.setSelected(true);}return p;}
    private JComponent financeRangeControls(){JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));p.setOpaque(false);JButton previous=outlineButton("‹"),next=outlineButton("›"),choose=outlineButton("Choose date");previous.addActionListener(e->{financeAnchor=shift(financeAnchor,financeRange,-1);refreshMoney();});next.addActionListener(e->{financeAnchor=shift(financeAnchor,financeRange,1);refreshMoney();});choose.addActionListener(e->{LocalDate date=chooseDate("Choose finance date",financeAnchor);if(date!=null){financeAnchor=date;refreshMoney();}});p.add(previous);p.add(financeDateLabel);p.add(next);p.add(choose);ButtonGroup group=new ButtonGroup();for(PeriodRange range:PeriodRange.values()){JToggleButton button=periodButton(range,()->{financeRange=range;refreshMoney();});group.add(button);p.add(button);if(range==financeRange)button.setSelected(true);}return p;}
    private JToggleButton periodButton(PeriodRange range,Runnable action){JToggleButton b=new JToggleButton(range.label);b.setFocusPainted(false);b.setBackground(CARD);b.setForeground(INK);b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER,1,true),new EmptyBorder(7,11,7,11)));b.addActionListener(e->action.run());return b;}

    private RoundedPanel metricCard(String title,JLabel value,Color bg){RoundedPanel p=new RoundedPanel(18,bg,new BorderLayout());p.setBorder(new EmptyBorder(16,18,16,18));p.add(label(title,12,Font.BOLD,MUTED),BorderLayout.NORTH);p.add(value,BorderLayout.CENTER);return p;}

    private void renderCalendar() {
        calendarGrid.removeAll();monthTitle.setText(displayedMonth.getMonth().getDisplayName(TextStyle.FULL,Locale.getDefault())+" "+displayedMonth.getYear());
        Map<LocalDate,DailyMoney> moneyByDay=new LinkedHashMap<>();try{for(DailyMoney money:db.moneyTrend(displayedMonth))moneyByDay.put(money.date(),money);}catch(SQLException ignored){}
        for(DayOfWeek day:new DayOfWeek[]{DayOfWeek.MONDAY,DayOfWeek.TUESDAY,DayOfWeek.WEDNESDAY,DayOfWeek.THURSDAY,DayOfWeek.FRIDAY,DayOfWeek.SATURDAY,DayOfWeek.SUNDAY}){JLabel l=label(day.getDisplayName(TextStyle.SHORT,Locale.getDefault()).toUpperCase(),11,Font.BOLD,MUTED);l.setHorizontalAlignment(SwingConstants.CENTER);calendarGrid.add(l);}
        LocalDate first=displayedMonth.atDay(1);int offset=first.getDayOfWeek().getValue()-1;for(int i=0;i<offset;i++)calendarGrid.add(new JLabel());for(int d=1;d<=displayedMonth.lengthOfMonth();d++){LocalDate date=displayedMonth.atDay(d);calendarGrid.add(dayButton(date,moneyByDay.getOrDefault(date,new DailyMoney(date,0,0))));}for(int i=offset+displayedMonth.lengthOfMonth();i<42;i++)calendarGrid.add(new JLabel());calendarGrid.revalidate();calendarGrid.repaint();
    }
    private JButton dayButton(LocalDate date,DailyMoney money){int events=safeCount(true,date);String html="<html><b>"+date.getDayOfMonth()+"</b>"+(events>0?"<br><font color='#4c6fff'>• "+events+" event"+(events==1?"":"s")+"</font>":"")+(money.spentCents()>0?"<br><font color='#dc4b4b'>− "+Money.format(money.spentCents())+" spent</font>":"")+(money.earnedCents()>0?"<br><font color='#1f9d60'>+ "+Money.format(money.earnedCents())+" earned</font>":"")+"</html>";JButton b=new JButton(html);b.setHorizontalAlignment(SwingConstants.LEFT);b.setVerticalAlignment(SwingConstants.TOP);b.setMargin(new Insets(8,9,6,5));b.setFocusPainted(false);b.setOpaque(true);b.setBackground(date.equals(selectedDate)?BLUE_SOFT:CARD);b.setBorder(BorderFactory.createLineBorder(date.equals(selectedDate)?BLUE:BORDER,date.equals(selectedDate)?2:1,true));b.addActionListener(e->{selectedDate=date;refreshAll();});return b;}

    private void refreshAll(){String date=selectedDate.format(FULL_DATE);selectedDateLabel.setText(date);renderCalendar();load(eventModel,()->db.eventsOn(selectedDate));refreshTasks();refreshMoney();refreshTrend();}
    private void refreshTasks(){LocalDate[] range=bounds(taskAnchor,taskRange);taskPeriodLabel.setText(periodText(range[0],range[1]));load(todoModel,()->db.todosBetween(range[0],range[1]));}
    private void refreshMoney(){LocalDate[] range=bounds(financeAnchor,financeRange);financeDateLabel.setText(periodText(range[0],range[1]));load(expenseModel,()->db.expensesBetween(range[0],range[1]));load(incomeModel,()->db.incomeBetween(range[0],range[1]));try{long spent=db.overallTotal(),earned=db.overallEarned(),saved=earned-spent,periodSpent=db.spentBetween(range[0],range[1]),periodEarned=db.earnedBetween(range[0],range[1]);dayTotal.setText(Money.format(periodSpent));weekTotal.setText(Money.format(periodEarned));monthTotal.setText(Money.format(periodEarned-periodSpent));monthTotal.setForeground(periodEarned-periodSpent>=0?GREEN:RED);overallSpent.setText(Money.format(spent));overallEarned.setText(Money.format(earned));overallSaved.setText(Money.format(saved));overallSaved.setForeground(saved>=0?GREEN:RED);}catch(SQLException e){showError(e);}}
    private void refreshTrend(){try{LocalDate start,end;boolean monthly=false;switch(trendRange){case WEEK->{start=trendAnchor.minusDays(trendAnchor.getDayOfWeek().getValue()-1L);end=start.plusDays(6);}case MONTH->{start=trendAnchor.withDayOfMonth(1);end=trendAnchor.withDayOfMonth(trendAnchor.lengthOfMonth());}case YEAR->{start=trendAnchor.minusYears(1).plusDays(1);end=trendAnchor;monthly=true;}case THREE_YEARS->{start=trendAnchor.minusYears(3).plusDays(1);end=trendAnchor;monthly=true;}default->throw new IllegalStateException();}trendMonthLabel.setText(periodText(start,end));trendChart.setData(db.moneyTrend(start,end),monthly);}catch(SQLException e){showError(e);}}
    private static LocalDate[] bounds(LocalDate anchor,PeriodRange range){return switch(range){case DAY->new LocalDate[]{anchor,anchor};case WEEK->{LocalDate start=anchor.minusDays(anchor.getDayOfWeek().getValue()-1L);yield new LocalDate[]{start,start.plusDays(6)};}case MONTH->new LocalDate[]{anchor.withDayOfMonth(1),anchor.withDayOfMonth(anchor.lengthOfMonth())};case YEAR->new LocalDate[]{anchor.withDayOfYear(1),anchor.withDayOfYear(anchor.lengthOfYear())};};}
    private static LocalDate shift(LocalDate anchor,PeriodRange range,int direction){return switch(range){case DAY->anchor.plusDays(direction);case WEEK->anchor.plusWeeks(direction);case MONTH->anchor.plusMonths(direction);case YEAR->anchor.plusYears(direction);};}
    private void shiftTrend(int direction){trendAnchor=switch(trendRange){case WEEK->trendAnchor.plusWeeks(direction);case MONTH->trendAnchor.plusMonths(direction);case YEAR->trendAnchor.plusYears(direction);case THREE_YEARS->trendAnchor.plusYears(3L*direction);};}
    private static String periodText(LocalDate start,LocalDate end){DateTimeFormatter f=DateTimeFormatter.ofPattern("MMM d, yyyy");return start.equals(end)?start.format(f):start.format(f)+" – "+end.format(f);}
    private LocalDate chooseDate(String title,LocalDate current){JComboBox<Month> month=new JComboBox<>(Month.values());month.setSelectedItem(current.getMonth());JSpinner day=new JSpinner(new SpinnerNumberModel(current.getDayOfMonth(),1,31,1));JSpinner year=new JSpinner(new SpinnerNumberModel(current.getYear(),1900,2200,1));JPanel panel=new JPanel(new GridLayout(3,2,10,10));panel.add(new JLabel("Month"));panel.add(month);panel.add(new JLabel("Day"));panel.add(day);panel.add(new JLabel("Year"));panel.add(year);if(JOptionPane.showConfirmDialog(this,panel,title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return null;try{return LocalDate.of((Integer)year.getValue(),(Month)month.getSelectedItem(),(Integer)day.getValue());}catch(DateTimeException e){warn("That date does not exist. Check the month and day.");return null;}}

    private void addEvent(){JTextField title=new JTextField(),time=new JTextField(),notes=new JTextField(),reminder=new JTextField();if(form("Add event",new String[]{"Title","Time (HH:mm, optional)","Notes","Reminder (YYYY-MM-DD HH:mm, optional)"},new JComponent[]{title,time,notes,reminder})){if(title.getText().isBlank()){warn("Please enter an event title.");return;}try{db.addEvent(selectedDate,time.getText().isBlank()?null:LocalTime.parse(time.getText().trim()),title.getText(),notes.getText(),parseReminder(reminder.getText()));refreshAll();}catch(DateTimeException e){warn("Use HH:mm for time and YYYY-MM-DD HH:mm for reminders.");}catch(SQLException e){showError(e);}}}
    private void deleteEvent(){CalendarEvent x=eventList.getSelectedValue();if(x!=null&&confirm("Delete “"+x.title()+"”?"))try{db.deleteEvent(x.id());refreshAll();}catch(SQLException e){showError(e);}}
    private void addTodo(){JTextField task=new JTextField(),due=new JTextField(taskAnchor.toString()),reminder=new JTextField();if(form("Add task",new String[]{"Task","Due date (YYYY-MM-DD, optional)","Reminder (YYYY-MM-DD HH:mm, optional)"},new JComponent[]{task,due,reminder})){if(task.getText().isBlank()){warn("Please enter a task.");return;}try{db.addTodo(task.getText(),due.getText().isBlank()?null:LocalDate.parse(due.getText().trim()),parseReminder(reminder.getText()));refreshAll();}catch(DateTimeException e){warn("Check the date and reminder formats.");}catch(SQLException e){showError(e);}}}
    private void toggleTodo(){TodoItem x=todoList.getSelectedValue();if(x!=null)try{db.setTodoCompleted(x.id(),!x.completed());refreshAll();}catch(SQLException e){showError(e);}}
    private void deleteTodo(){TodoItem x=todoList.getSelectedValue();if(x!=null&&confirm("Delete “"+x.task()+"”?"))try{db.deleteTodo(x.id());refreshAll();}catch(SQLException e){showError(e);}}
    private void addExpense(){moneyForm("Add expense for "+financeAnchor,"Description",(description,cents)->db.addExpense(financeAnchor,description,cents));}
    private void addIncome(){moneyForm("Add money earned for "+financeAnchor,"Source (salary, interest, etc.)",(description,cents)->db.addIncome(financeAnchor,description,cents));}
    private void moneyForm(String title,String descriptionLabel,MoneyWriter writer){JTextField description=new JTextField(),amount=new JTextField();if(form(title,new String[]{descriptionLabel,"Amount"},new JComponent[]{description,amount})){if(description.getText().isBlank()){warn("Please enter a description.");return;}try{writer.write(description.getText(),Money.parseCents(amount.getText()));refreshAll();}catch(NumberFormatException|ArithmeticException e){warn("Enter a valid amount, such as 12.50.");}catch(IllegalArgumentException e){warn(e.getMessage());}catch(SQLException e){showError(e);}}}
    private void deleteExpense(){Expense x=expenseList.getSelectedValue();if(x!=null&&confirm("Delete “"+x.description()+"”?"))try{db.deleteExpense(x.id());refreshAll();}catch(SQLException e){showError(e);}}
    private void deleteIncome(){Income x=incomeList.getSelectedValue();if(x!=null&&confirm("Delete “"+x.description()+"”?"))try{db.deleteIncome(x.id());refreshAll();}catch(SQLException e){showError(e);}}

    private boolean form(String title,String[] labels,JComponent[] fields){JPanel p=new JPanel(new GridLayout(labels.length,2,10,12));p.setBorder(new EmptyBorder(8,4,8,4));for(int i=0;i<labels.length;i++){p.add(new JLabel(labels[i]));p.add(fields[i]);}return JOptionPane.showConfirmDialog(this,p,title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION;}
    private boolean confirm(String m){return JOptionPane.showConfirmDialog(this,m,"Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;}
    private void warn(String m){JOptionPane.showMessageDialog(this,m,"Check entry",JOptionPane.WARNING_MESSAGE);}private void showError(Exception e){JOptionPane.showMessageDialog(this,e.getMessage(),"Database error",JOptionPane.ERROR_MESSAGE);}
    private static LocalDateTime parseReminder(String v){return v.isBlank()?null:LocalDateTime.parse(v.trim(),REMINDER_FORMAT);}private void updateClock(){clockLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm:ss a")));}private void quitApplication(){clockTimer.stop();notifications.close();try{db.close();}catch(SQLException ignored){}dispose();}
    private int safeCount(boolean events,LocalDate d){try{return events?db.eventCount(d):db.expenseCount(d);}catch(SQLException e){return 0;}}
    private interface SqlList<T>{List<T> get()throws SQLException;}private interface MoneyWriter{void write(String d,long c)throws SQLException;}private <T>void load(DefaultListModel<T>m,SqlList<T>s){try{m.clear();for(T x:s.get())m.addElement(x);}catch(SQLException e){showError(e);}}

    private static RoundedPanel card(LayoutManager l){RoundedPanel p=new RoundedPanel(20,CARD,l);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER,1,true),new EmptyBorder(16,16,16,16)));return p;}
    private static JScrollPane scroll(JComponent c){JScrollPane s=new JScrollPane(c,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);s.setBorder(BorderFactory.createLineBorder(BORDER));s.getViewport().setBackground(CARD);s.getVerticalScrollBar().setUnitIncrement(16);return s;}
    private static JButton primaryButton(String t){JButton b=new JButton(t);b.setBackground(BLUE);b.setForeground(Color.WHITE);b.setOpaque(true);b.setFocusPainted(false);b.setBorder(new EmptyBorder(10,16,10,16));b.setFont(b.getFont().deriveFont(Font.BOLD,13f));return b;}
    private static JButton outlineButton(String t){JButton b=new JButton(t);b.setBackground(CARD);b.setForeground(INK);b.setOpaque(true);b.setFocusPainted(false);b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER,1,true),new EmptyBorder(8,14,8,14)));return b;}
    private static JLabel label(String t,float size,int style,Color color){JLabel l=new JLabel("<html>"+t.replace("\n","<br>")+"</html>");l.setFont(l.getFont().deriveFont(style,size));l.setForeground(color);return l;}
    private static JLabel valueLabel(Color c){JLabel l=label("$0.00",25,Font.BOLD,c);l.setBorder(new EmptyBorder(5,0,0,0));return l;}
    private static JPanel stack(JComponent a,JComponent b,int gap){JPanel p=new JPanel();p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));a.setAlignmentX(Component.LEFT_ALIGNMENT);b.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(a);p.add(Box.createVerticalStrut(gap));p.add(b);return p;}
    private static <T>JList<T> modernList(ListModel<T>m){JList<T>l=new JList<>(m);l.setBackground(CARD);l.setSelectionBackground(BLUE_SOFT);l.setSelectionForeground(INK);l.setFixedCellHeight(44);l.setBorder(new EmptyBorder(4,4,4,4));return l;}

    private static class PaddedRenderer extends DefaultListCellRenderer{private final Color normal;PaddedRenderer(Color c){normal=c;}@Override public Component getListCellRendererComponent(JList<?>l,Object v,int i,boolean s,boolean f){JLabel x=(JLabel)super.getListCellRendererComponent(l,v,i,s,f);x.setBorder(new EmptyBorder(9,10,9,10));if(!s)x.setForeground(normal);return x;}}
    private static final class EventRenderer extends PaddedRenderer{EventRenderer(){super(INK);}}
    private static final class ExpenseRenderer extends PaddedRenderer{ExpenseRenderer(){super(INK);}@Override public Component getListCellRendererComponent(JList<?>l,Object v,int i,boolean s,boolean f){JLabel x=(JLabel)super.getListCellRendererComponent(l,v,i,s,f);if(v instanceof Expense item)x.setText(item.date()+"  ·  "+item.description()+"  ·  "+Money.format(item.amountCents()));return x;}}
    private static final class IncomeRenderer extends PaddedRenderer{IncomeRenderer(){super(GREEN);}@Override public Component getListCellRendererComponent(JList<?>l,Object v,int i,boolean s,boolean f){JLabel x=(JLabel)super.getListCellRendererComponent(l,v,i,s,f);if(v instanceof Income item)x.setText(item.date()+"  ·  "+item.description()+"  ·  +"+Money.format(item.amountCents()));return x;}}
    private static final class TodoRenderer extends PaddedRenderer{TodoRenderer(){super(INK);}@Override public Component getListCellRendererComponent(JList<?>l,Object v,int i,boolean s,boolean f){JLabel x=(JLabel)super.getListCellRendererComponent(l,v,i,s,f);if(v instanceof TodoItem t&&t.completed()&&!s)x.setForeground(MUTED);return x;}}
    private static final class RoundedPanel extends JPanel{private final int radius;private final Color fill;RoundedPanel(int r,Color f,LayoutManager l){super(l);radius=r;fill=f;setOpaque(false);}@Override protected void paintComponent(Graphics g){Graphics2D x=(Graphics2D)g.create();x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);x.setColor(fill);x.fillRoundRect(0,0,getWidth(),getHeight(),radius,radius);x.dispose();super.paintComponent(g);}}
    private enum PeriodRange{DAY("Day"),WEEK("Week"),MONTH("Month"),YEAR("Year");private final String label;PeriodRange(String label){this.label=label;}}
    private enum TrendRange{WEEK("Week"),MONTH("Month"),YEAR("1 Year"),THREE_YEARS("3 Years");private final String label;TrendRange(String label){this.label=label;}}
}
