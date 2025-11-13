package com.fairbilling.domain;

import java.util.Objects;

/**
 * Immutable billing summary for a single user.
 */
public final class UserBillingSummary {

    private final String username;
    private final int sessionCount;
    private final int totalDurationSeconds;

    public UserBillingSummary(String username, int sessionCount, int totalDurationSeconds) {
        this.username = Objects.requireNonNull(username, "username");
        this.sessionCount = validateNonNegative(sessionCount, "sessionCount");
        this.totalDurationSeconds = validateNonNegative(totalDurationSeconds, "totalDurationSeconds");
    }

    public String getUsername() {
        return username;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public int getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    private static int validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
        return value;
    }
}
