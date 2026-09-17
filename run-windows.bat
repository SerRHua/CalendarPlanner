@echo off
if not exist bin mkdir bin
dir /s /b src\main\java\*.java > sources.txt
javac --release 17 -d bin @sources.txt
del sources.txt
if errorlevel 1 exit /b 1
copy /Y src\main\resources\database.properties bin\database.properties >nul
java -cp "bin;lib\mysql-connector-j.jar" com.serena.calendar.CalendarPlannerApp
