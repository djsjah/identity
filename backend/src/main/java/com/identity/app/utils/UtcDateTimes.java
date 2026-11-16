package com.identity.app.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public final class UtcDateTimes {
    private UtcDateTimes() { }

    public static LocalDateTime nowMicros() { return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS); }
}
