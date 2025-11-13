package com.fairbilling.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fairbilling.domain.LogEntry;

/**
 * Reads log files and produces a collection of {@link LogEntry} instances.
 */
public class LogFileParser {

    private final LogEntryParser entryParser;

    public LogFileParser() {
        this(new LogEntryParser());
    }

    public LogFileParser(LogEntryParser entryParser) {
        this.entryParser = Objects.requireNonNull(entryParser, "entryParser");
    }

    public List<LogEntry> parse(Path filePath) throws IOException {
        Objects.requireNonNull(filePath, "filePath");

        List<LogEntry> entries = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            int lastTimestamp = -1;
            while ((line = reader.readLine()) != null) {
                Optional<LogEntry> maybeEntry = entryParser.parse(line);
                if (!maybeEntry.isPresent()) {
                    continue;
                }

                LogEntry entry = maybeEntry.get();
                int timestamp = entry.getSecondsSinceMidnight();
                if (lastTimestamp <= timestamp) {
                    entries.add(entry);
                    lastTimestamp = timestamp;
                }
            }
        }
        return entries;
    }
}
