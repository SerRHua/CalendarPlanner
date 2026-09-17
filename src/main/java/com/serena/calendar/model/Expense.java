package com.serena.calendar.model;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.io.Serializable;

public record Expense(long id, LocalDate date, String description, long amountCents) implements Serializable {
    public BigDecimal amount() { return BigDecimal.valueOf(amountCents, 2); }
    @Override public String toString() { return description + "  ·  $" + amount().toPlainString(); }
}
