package com.serena.calendar.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public record Income(long id, LocalDate date, String description, long amountCents) implements Serializable {
    public BigDecimal amount() { return BigDecimal.valueOf(amountCents, 2); }
    @Override public String toString() { return description + "  ·  +$" + amount().toPlainString(); }
}
