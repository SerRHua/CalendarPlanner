package com.serena.calendar.model;

import java.time.LocalDate;

public record DailyMoney(LocalDate date, long spentCents, long earnedCents) { }
