package com.serena.calendar.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.io.Serializable;

public record CalendarEvent(long id, LocalDate date, LocalTime time, String title, String notes, LocalDateTime reminderAt) implements Serializable {
    @Override public String toString() {
        return (time == null ? "" : time + "  ") + title + (reminderAt == null ? "" : "  🔔");
    }
}
