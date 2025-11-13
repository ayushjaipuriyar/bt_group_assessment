package com.fairbilling.util;

import java.util.OptionalInt;

/**
 * Utility for parsing HH:MM:SS values into seconds since midnight.
 */
public final class TimeParser {

    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    private TimeParser() {
    }

    public static OptionalInt parseToSeconds(String value) {
        if (value == null) {
            return OptionalInt.empty();
        }
        String[] parts = value.trim().split(":");
        if (parts.length != 3) {
            return OptionalInt.empty();
        }

        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);

            if (hours < 0 || hours > 23) {
                return OptionalInt.empty();
            }
            if (minutes < 0 || minutes > 59) {
                return OptionalInt.empty();
            }
            if (seconds < 0 || seconds > 59) {
                return OptionalInt.empty();
            }

            int totalSeconds = hours * 3600 + minutes * 60 + seconds;
            if (totalSeconds < 0 || totalSeconds >= SECONDS_PER_DAY) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(totalSeconds);
        } catch (NumberFormatException ex) {
            return OptionalInt.empty();
        }
    }
}
