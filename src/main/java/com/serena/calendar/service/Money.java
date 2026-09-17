package com.serena.calendar.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;

public final class Money {
    private Money() {}
    public static long parseCents(String text) {
        BigDecimal value = new BigDecimal(text.trim().replace("$", "").replace(",", ""));
        if (value.signum() < 0) throw new IllegalArgumentException("Amount cannot be negative.");
        return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }
    public static String format(long cents) {
        return NumberFormat.getCurrencyInstance().format(BigDecimal.valueOf(cents, 2));
    }
}
