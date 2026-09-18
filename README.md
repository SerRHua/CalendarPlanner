# Calendar Planner — Eclipse + MySQL

A Java 17 Swing calendar with events, to-dos, expense tracking, MySQL
persistence, and computer reminder notifications. It does not use Maven or
Gradle.

The interface uses an app-style sidebar with dedicated Calendar, Tasks,
Finances, and Trends pages, rounded dashboard cards, and color-coded financial
information.

Tasks and Finances have independent Day, Week, Month, and Year selectors with
previous/next navigation. Trends can also be moved backward or forward without
changing the selected calendar day. The “Go to today” action appears only on
the Calendar page.

Every page also provides a month/day/year date chooser for jumping directly to
older or future periods. Tasks and Finances support Day, Week, Month, and Year
views; choosing any date within a week opens that date's Monday–Sunday week.

## 1. Install and configure MySQL

1. Install MySQL Community Server and make sure it is running.
2. Open `sql/setup.sql` and replace the sample password.
3. Run that script as a MySQL administrator (in MySQL Workbench, for example).
4. Open `src/main/resources/database.properties` and enter the same username
   and password.

The program creates its three tables and indexes automatically the first time
it connects. You can alternatively provide `CALENDAR_DB_URL`,
`CALENDAR_DB_USER`, and `CALENDAR_DB_PASSWORD` environment variables; these
override the values in the properties file.

## 2. Add MySQL Connector/J

1. Download the platform-independent MySQL Connector/J archive from
   <https://dev.mysql.com/downloads/connector/j/>.
2. Extract it and locate `mysql-connector-j-*.jar`.
3. Copy that JAR into this project's `lib` folder.
4. Rename the copy to `mysql-connector-j.jar`.

The included Eclipse `.classpath` already points to that filename. Connector/J
is distributed separately by Oracle and therefore is not included in this ZIP.

## 3. Import and run in Eclipse

1. Select **File → Import → General → Existing Projects into Workspace**.
2. Choose the extracted `CalendarPlanner` folder and click **Finish**.
3. If needed, press **F5** on the project to refresh it after adding the JAR.
4. Open `src/main/java/com/serena/calendar/CalendarPlannerApp.java`.
5. Right-click it and choose **Run As → Java Application**.

Use Java 17 or newer. If necessary, select it under **Project → Properties →
Java Build Path → Libraries**.

## Notifications

When adding an event or task, enter an optional reminder using this format:

```text
2026-09-20 14:30
```

The application checks MySQL every 30 seconds. When a reminder becomes due it
shows a computer notification and records it as sent so it is not repeated.
Closing the main window hides the application in the system tray so reminders
continue. Use **Quit** from the tray icon to stop it completely.

Notifications require operating-system tray support and permission to show
notifications. They cannot appear if the program is not running. To receive
them after restarting the computer, add Calendar Planner to your operating
system's login/startup applications.

## Features

- Monthly calendar with event and expense indicators
- Live clock in the calendar header
- Red daily spending totals and green daily earnings totals inside calendar days
- Events with times, notes, and reminder timestamps
- To-do tasks with due dates, completion state, and reminders
- Daily expense entries sorted alphabetically by description
- Money-earned entries for salary, interest, refunds, or other income
- Selected-day, Monday–Sunday week, month, and overall spending totals
- Overall spent, earned, and saved (`earned - spent`) summaries
- Cumulative trend chart with separate red spending and green earnings/interest
- Week, Month, 1 Year, and 3 Years trend ranges
- Hover details showing the period and exact cumulative amounts at each point
- Prepared JDBC statements and automatic MySQL table setup
- System-tray background mode and one-time desktop notifications
- Always-scrollable event, spending, and earnings lists

## Database design

| Table | Main data |
| --- | --- |
| `events` | Date, time, title, notes, reminder, notification status |
| `todos` | Task, due date, completion, reminder, notification status |
| `expenses` | Date, description, exact amount in integer cents |
| `income` | Date, source/description, exact amount in integer cents |

Database passwords should not be committed to a public repository. For a
public project, leave a placeholder in `database.properties` and use the
`CALENDAR_DB_PASSWORD` environment variable locally.
