package com.fairbilling.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.OptionalInt;

import org.junit.Test;

public class TimeParserTest {

    @Test
    public void parsesValidTimes() {
        OptionalInt midnight = TimeParser.parseToSeconds("00:00:00");
        assertTrue(midnight.isPresent());
        assertEquals(0, midnight.getAsInt());

        OptionalInt random = TimeParser.parseToSeconds("14:02:03");
        assertTrue(random.isPresent());
        assertEquals(50523, random.getAsInt());

        OptionalInt endOfDay = TimeParser.parseToSeconds("23:59:59");
        assertTrue(endOfDay.isPresent());
        assertEquals(86399, endOfDay.getAsInt());
    }

    @Test
    public void rejectsInvalidTimes() {
        assertFalse(TimeParser.parseToSeconds("25:00:00").isPresent());
        assertFalse(TimeParser.parseToSeconds("14:60:00").isPresent());
        assertFalse(TimeParser.parseToSeconds("14:02:60").isPresent());
        assertFalse(TimeParser.parseToSeconds("invalid").isPresent());
        assertFalse(TimeParser.parseToSeconds("14:02").isPresent());
    }
}
