package com.fairbilling.io;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fairbilling.domain.LogEntry;
import com.fairbilling.domain.SessionEventType;
import com.fairbilling.util.TimeParser;

/**
 * Parses raw log lines into structured {@link LogEntry} instances.
 */
public class LogEntryParser {

    private static final Pattern LOG_PATTERN = Pattern.compile("^(\\d{2}:\\d{2}:\\d{2})\\s+(\\w+)\\s+(Start|End)\\s*$");

    public Optional<LogEntry> parse(String line) {
        if (line == null) {
            return Optional.empty();
        }

        Matcher matcher = LOG_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        OptionalInt seconds = TimeParser.parseToSeconds(matcher.group(1));
        if (!seconds.isPresent()) {
            return Optional.empty();
        }

        Optional<SessionEventType> eventType = SessionEventType.fromToken(matcher.group(3));
        if (!eventType.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(new LogEntry(seconds.getAsInt(), matcher.group(2), eventType.get()));
    }
}
