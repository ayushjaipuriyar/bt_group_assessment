package com.fairbilling.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.fairbilling.domain.LogEntry;
import com.fairbilling.domain.SessionEventType;

public class LogEntryParserTest {

    private LogEntryParser parser;

    @Before
    public void setUp() {
        parser = new LogEntryParser();
    }

    @Test
    public void parsesWellFormedLine() {
        Optional<LogEntry> entry = parser.parse("14:02:03 ALICE99 Start");
        assertTrue(entry.isPresent());
        LogEntry logEntry = entry.get();
        assertEquals("ALICE99", logEntry.getUsername());
        assertEquals(50523, logEntry.getSecondsSinceMidnight());
        assertEquals(SessionEventType.START, logEntry.getEventType());
    }

    @Test
    public void ignoresMalformedLines() {
        assertFalse(parser.parse("invalid line").isPresent());
        assertFalse(parser.parse("14:02:03 ALICE99 Unknown").isPresent());
    }
}
