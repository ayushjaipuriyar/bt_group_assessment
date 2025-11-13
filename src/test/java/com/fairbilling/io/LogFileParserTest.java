package com.fairbilling.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fairbilling.domain.LogEntry;

public class LogFileParserTest {

    private LogFileParser parser;

    @Before
    public void setUp() {
        parser = new LogFileParser();
    }

    @Test
    public void ignoresEntriesWithDecreasingTimestamps() throws IOException {
        Path tempFile = Files.createTempFile("fair-billing", ".log");
        Files.write(tempFile, Arrays.asList(
                "23:59:59 BRAVO End",
                "00:00:00 CHARLIE Start"));

        List<LogEntry> entries = parser.parse(tempFile);

        assertEquals(1, entries.size());
        assertEquals("BRAVO", entries.get(0).getUsername());
        assertEquals(86399, entries.get(0).getSecondsSinceMidnight());
        assertTrue(entries.get(0).isEndEvent());
    }
}
