package com.serena.calendar.db;

import com.serena.calendar.model.*;
import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/** MySQL-backed repository. All user values are passed through prepared statements. */
public final class Database implements AutoCloseable {
    private final Connection connection;

    public Database() throws SQLException {
        Properties config = loadConfig();
        String url = value(config, "db.url", "CALENDAR_DB_URL");
        String user = value(config, "db.user", "CALENDAR_DB_USER");
        String password = System.getenv().getOrDefault("CALENDAR_DB_PASSWORD", config.getProperty("db.password", ""));
        connection = DriverManager.getConnection(url, user, password);
        initializeSchema();
    }

    private static Properties loadConfig() throws SQLException {
        Properties p = new Properties();
        try (InputStream in = Database.class.getResourceAsStream("/database.properties")) {
            if (in == null) throw new IOException("database.properties was not found on the classpath");
            p.load(in); return p;
        } catch (IOException e) { throw new SQLException("Could not load database configuration", e); }
    }
    private static String value(Properties p, String key, String env) throws SQLException {
        String value = System.getenv().getOrDefault(env, p.getProperty(key, "")).trim();
        if (value.isEmpty()) throw new SQLException("Missing database setting: " + key); return value;
    }
    private void initializeSchema() throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS events (id BIGINT PRIMARY KEY AUTO_INCREMENT, event_date DATE NOT NULL, event_time TIME NULL, title VARCHAR(255) NOT NULL, notes TEXT NOT NULL, reminder_at DATETIME NULL, notified BOOLEAN NOT NULL DEFAULT FALSE, INDEX(event_date), INDEX(reminder_at))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS todos (id BIGINT PRIMARY KEY AUTO_INCREMENT, task VARCHAR(500) NOT NULL, due_date DATE NULL, completed BOOLEAN NOT NULL DEFAULT FALSE, reminder_at DATETIME NULL, notified BOOLEAN NOT NULL DEFAULT FALSE, INDEX(due_date), INDEX(reminder_at))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS expenses (id BIGINT PRIMARY KEY AUTO_INCREMENT, expense_date DATE NOT NULL, description VARCHAR(500) NOT NULL, amount_cents BIGINT NOT NULL, INDEX(expense_date), CONSTRAINT positive_amount CHECK(amount_cents >= 0))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS income (id BIGINT PRIMARY KEY AUTO_INCREMENT, income_date DATE NOT NULL, description VARCHAR(500) NOT NULL, amount_cents BIGINT NOT NULL, INDEX(income_date), CONSTRAINT positive_income CHECK(amount_cents >= 0))");
        }
    }

    public synchronized void addEvent(LocalDate date, LocalTime time, String title, String notes, LocalDateTime reminder) throws SQLException {
        try (PreparedStatement p = connection.prepareStatement("INSERT INTO events(event_date,event_time,title,notes,reminder_at) VALUES(?,?,?,?,?)")) {
            p.setDate(1, Date.valueOf(date)); setTime(p, 2, time); p.setString(3, title.trim()); p.setString(4, notes == null ? "" : notes.trim()); setTimestamp(p, 5, reminder); p.executeUpdate();
        }
    }
    public synchronized List<CalendarEvent> eventsOn(LocalDate date) throws SQLException {
        List<CalendarEvent> items = new ArrayList<>();
        try (PreparedStatement p = connection.prepareStatement("SELECT * FROM events WHERE event_date=? ORDER BY event_time IS NULL,event_time,title")) {
            p.setDate(1, Date.valueOf(date)); try (ResultSet r = p.executeQuery()) { while (r.next()) items.add(new CalendarEvent(r.getLong("id"), r.getDate("event_date").toLocalDate(), localTime(r.getTime("event_time")), r.getString("title"), r.getString("notes"), localDateTime(r.getTimestamp("reminder_at")))); }
        } return items;
    }
    public synchronized void deleteEvent(long id) throws SQLException { delete("events", id); }

    public synchronized void addTodo(String task, LocalDate dueDate, LocalDateTime reminder) throws SQLException {
        try (PreparedStatement p = connection.prepareStatement("INSERT INTO todos(task,due_date,reminder_at) VALUES(?,?,?)")) {
            p.setString(1, task.trim()); if (dueDate == null) p.setNull(2, Types.DATE); else p.setDate(2, Date.valueOf(dueDate)); setTimestamp(p, 3, reminder); p.executeUpdate();
        }
    }
    public synchronized List<TodoItem> todos() throws SQLException {
        List<TodoItem> items = new ArrayList<>();
        try (Statement s = connection.createStatement(); ResultSet r = s.executeQuery("SELECT * FROM todos ORDER BY completed,due_date IS NULL,due_date,id DESC")) {
            while (r.next()) items.add(new TodoItem(r.getLong("id"), r.getString("task"), localDate(r.getDate("due_date")), r.getBoolean("completed"), localDateTime(r.getTimestamp("reminder_at"))));
        } return items;
    }
    public synchronized void setTodoCompleted(long id, boolean completed) throws SQLException {
        try (PreparedStatement p = connection.prepareStatement("UPDATE todos SET completed=? WHERE id=?")) { p.setBoolean(1, completed); p.setLong(2, id); p.executeUpdate(); }
    }
    public synchronized void deleteTodo(long id) throws SQLException { delete("todos", id); }

    public synchronized void addExpense(LocalDate date, String description, long cents) throws SQLException {
        try (PreparedStatement p = connection.prepareStatement("INSERT INTO expenses(expense_date,description,amount_cents) VALUES(?,?,?)")) { p.setDate(1, Date.valueOf(date)); p.setString(2, description.trim()); p.setLong(3, cents); p.executeUpdate(); }
    }
    public synchronized List<Expense> expensesOn(LocalDate date) throws SQLException {
        List<Expense> items = new ArrayList<>();
        try (PreparedStatement p = connection.prepareStatement("SELECT * FROM expenses WHERE expense_date=? ORDER BY LOWER(description),id")) {
            p.setDate(1, Date.valueOf(date)); try (ResultSet r = p.executeQuery()) { while (r.next()) items.add(new Expense(r.getLong("id"), r.getDate("expense_date").toLocalDate(), r.getString("description"), r.getLong("amount_cents"))); }
        } return items;
    }
    public synchronized void deleteExpense(long id) throws SQLException { delete("expenses", id); }
    public synchronized void addIncome(LocalDate date, String description, long cents) throws SQLException {
        try (PreparedStatement p = connection.prepareStatement("INSERT INTO income(income_date,description,amount_cents) VALUES(?,?,?)")) { p.setDate(1, Date.valueOf(date)); p.setString(2, description.trim()); p.setLong(3, cents); p.executeUpdate(); }
    }
    public synchronized List<Income> incomeOn(LocalDate date) throws SQLException {
        List<Income> items = new ArrayList<>();
        try (PreparedStatement p = connection.prepareStatement("SELECT * FROM income WHERE income_date=? ORDER BY LOWER(description),id")) {
            p.setDate(1, Date.valueOf(date)); try (ResultSet r = p.executeQuery()) { while (r.next()) items.add(new Income(r.getLong("id"), r.getDate("income_date").toLocalDate(), r.getString("description"), r.getLong("amount_cents"))); }
        } return items;
    }
    public synchronized void deleteIncome(long id) throws SQLException { delete("income", id); }
    public synchronized long dayTotal(LocalDate date) throws SQLException { return totalBetween(date, date); }
    public synchronized long weekTotal(LocalDate date) throws SQLException { LocalDate monday = date.minusDays(date.getDayOfWeek().getValue() - 1L); return totalBetween(monday, monday.plusDays(6)); }
    public synchronized long monthTotal(LocalDate date) throws SQLException { return totalBetween(date.withDayOfMonth(1), date.withDayOfMonth(date.lengthOfMonth())); }
    public synchronized long overallTotal() throws SQLException { try (Statement s = connection.createStatement(); ResultSet r = s.executeQuery("SELECT COALESCE(SUM(amount_cents),0) FROM expenses")) { r.next(); return r.getLong(1); } }
    public synchronized long overallEarned() throws SQLException { try (Statement s = connection.createStatement(); ResultSet r = s.executeQuery("SELECT COALESCE(SUM(amount_cents),0) FROM income")) { r.next(); return r.getLong(1); } }
    public synchronized List<DailyMoney> moneyTrend(YearMonth month) throws SQLException {
        LocalDate start = month.atDay(1), end = month.atEndOfMonth();
        Map<LocalDate,long[]> totals = new TreeMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) totals.put(d, new long[2]);
        try (PreparedStatement p = connection.prepareStatement("SELECT expense_date,SUM(amount_cents) total FROM expenses WHERE expense_date BETWEEN ? AND ? GROUP BY expense_date")) {
            p.setDate(1, Date.valueOf(start)); p.setDate(2, Date.valueOf(end)); try (ResultSet r = p.executeQuery()) { while (r.next()) totals.get(r.getDate(1).toLocalDate())[0] = r.getLong(2); }
        }
        try (PreparedStatement p = connection.prepareStatement("SELECT income_date,SUM(amount_cents) total FROM income WHERE income_date BETWEEN ? AND ? GROUP BY income_date")) {
            p.setDate(1, Date.valueOf(start)); p.setDate(2, Date.valueOf(end)); try (ResultSet r = p.executeQuery()) { while (r.next()) totals.get(r.getDate(1).toLocalDate())[1] = r.getLong(2); }
        }
        List<DailyMoney> result = new ArrayList<>(); totals.forEach((date, values) -> result.add(new DailyMoney(date, values[0], values[1]))); return result;
    }
    public synchronized int eventCount(LocalDate date) throws SQLException { return count("events", "event_date", date); }
    public synchronized int expenseCount(LocalDate date) throws SQLException { return count("expenses", "expense_date", date); }

    public synchronized List<Reminder> dueReminders() throws SQLException {
        List<Reminder> result = new ArrayList<>();
        try (Statement s = connection.createStatement(); ResultSet r = s.executeQuery("SELECT id,title,event_date,event_time FROM events WHERE reminder_at<=NOW() AND notified=FALSE")) {
            while (r.next()) result.add(new Reminder("event", r.getLong("id"), "Upcoming event: " + r.getString("title"), "Scheduled for " + r.getDate("event_date") + (r.getTime("event_time") == null ? "" : " at " + r.getTime("event_time").toLocalTime())));
        }
        try (Statement s = connection.createStatement(); ResultSet r = s.executeQuery("SELECT id,task,due_date FROM todos WHERE reminder_at<=NOW() AND notified=FALSE AND completed=FALSE")) {
            while (r.next()) result.add(new Reminder("todo", r.getLong("id"), "To-do reminder", r.getString("task") + (r.getDate("due_date") == null ? "" : " · due " + r.getDate("due_date"))));
        } return result;
    }
    public synchronized void markNotified(Reminder reminder) throws SQLException {
        String table = reminder.type().equals("event") ? "events" : "todos";
        try (PreparedStatement p = connection.prepareStatement("UPDATE " + table + " SET notified=TRUE WHERE id=?")) { p.setLong(1, reminder.itemId()); p.executeUpdate(); }
    }
    private long totalBetween(LocalDate start, LocalDate end) throws SQLException { try (PreparedStatement p = connection.prepareStatement("SELECT COALESCE(SUM(amount_cents),0) FROM expenses WHERE expense_date BETWEEN ? AND ?")) { p.setDate(1, Date.valueOf(start)); p.setDate(2, Date.valueOf(end)); try (ResultSet r = p.executeQuery()) { r.next(); return r.getLong(1); } } }
    private int count(String table, String column, LocalDate date) throws SQLException { try (PreparedStatement p = connection.prepareStatement("SELECT COUNT(*) FROM " + table + " WHERE " + column + "=?")) { p.setDate(1, Date.valueOf(date)); try (ResultSet r = p.executeQuery()) { r.next(); return r.getInt(1); } } }
    private void delete(String table, long id) throws SQLException { try (PreparedStatement p = connection.prepareStatement("DELETE FROM " + table + " WHERE id=?")) { p.setLong(1, id); p.executeUpdate(); } }
    private static void setTimestamp(PreparedStatement p, int i, LocalDateTime v) throws SQLException { if (v == null) p.setNull(i, Types.TIMESTAMP); else p.setTimestamp(i, Timestamp.valueOf(v)); }
    private static void setTime(PreparedStatement p, int i, LocalTime v) throws SQLException { if (v == null) p.setNull(i, Types.TIME); else p.setTime(i, Time.valueOf(v)); }
    private static LocalDate localDate(Date v) { return v == null ? null : v.toLocalDate(); }
    private static LocalTime localTime(Time v) { return v == null ? null : v.toLocalTime(); }
    private static LocalDateTime localDateTime(Timestamp v) { return v == null ? null : v.toLocalDateTime(); }
    @Override public synchronized void close() throws SQLException { connection.close(); }
}
