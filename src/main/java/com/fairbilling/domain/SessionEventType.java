package com.fairbilling.domain;

import java.util.Locale;
import java.util.Optional;

/**
 * Enumerates supported session event types within the billing log.
 */
public enum SessionEventType {
    START("Start"),
    END("End");

    private final String token;

    SessionEventType(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public boolean isStart() {
        return this == START;
    }

    public boolean isEnd() {
        return this == END;
    }

    public static Optional<SessionEventType> fromToken(String value) {
        if (value == null) {
            return Optional.empty();
        }
        for (SessionEventType type : values()) {
            if (type.token.equalsIgnoreCase(value.trim())) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return token.toUpperCase(Locale.ROOT);
    }
}
