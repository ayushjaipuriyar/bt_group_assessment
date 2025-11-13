package com.fairbilling.domain;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Represents a single event captured in the log file.
 */
public final class LogEntry {

    private final int secondsSinceMidnight;
    private final String username;
    private final SessionEventType eventType;

    public LogEntry(int secondsSinceMidnight, String username, SessionEventType eventType) {
        this.secondsSinceMidnight = validateSeconds(secondsSinceMidnight);
        this.username = normalizeUsername(username);
        this.eventType = Objects.requireNonNull(eventType, "eventType");
    }

    public int getSecondsSinceMidnight() {
        return secondsSinceMidnight;
    }

    public LocalTime getEventTime() {
        return LocalTime.ofSecondOfDay(secondsSinceMidnight);
    }

    public String getUsername() {
        return username;
    }

    public SessionEventType getEventType() {
        return eventType;
    }

    public boolean isStartEvent() {
        return eventType.isStart();
    }

    public boolean isEndEvent() {
        return eventType.isEnd();
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "secondsSinceMidnight=" + secondsSinceMidnight +
                ", username='" + username + '\'' +
                ", eventType=" + eventType +
                '}';
    }

    private static int validateSeconds(int seconds) {
        if (seconds < 0 || seconds >= 24 * 60 * 60) {
            throw new IllegalArgumentException("secondsSinceMidnight must be within a single day");
        }
        return seconds;
    }

    private static String normalizeUsername(String username) {
        Objects.requireNonNull(username, "username");
        String trimmed = username.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("username cannot be blank");
        }
        return trimmed;
    }
}
