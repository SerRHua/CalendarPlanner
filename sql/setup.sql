-- Run this while signed in as a MySQL administrator.
CREATE DATABASE IF NOT EXISTS calendar_planner
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'calendar_user'@'localhost'
  IDENTIFIED BY 'replace_with_a_strong_password';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX
  ON calendar_planner.* TO 'calendar_user'@'localhost';

FLUSH PRIVILEGES;
