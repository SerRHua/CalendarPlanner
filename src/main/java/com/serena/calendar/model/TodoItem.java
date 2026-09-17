package com.serena.calendar.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.Serializable;

public record TodoItem(long id, String task, LocalDate dueDate, boolean completed, LocalDateTime reminderAt) implements Serializable {
    @Override public String toString() {
        return (completed ? "✓  " : "○  ") + task + (dueDate == null ? "" : "  ·  " + dueDate) + (reminderAt == null ? "" : "  🔔");
    }
}
