#!/usr/bin/env bash
set -euo pipefail
mkdir -p bin
find src/main/java -name '*.java' -print0 | xargs -0 javac --release 17 -d bin
cp src/main/resources/database.properties bin/
java -cp "bin:lib/mysql-connector-j.jar" com.serena.calendar.CalendarPlannerApp
