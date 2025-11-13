import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;

public class FairBillingTest {

    private FairBilling billing = new FairBilling();

    @Test
    public void testTimeToSecondsValid() {
        assertEquals(0, billing.timeToSeconds("00:00:00"));
        assertEquals(50523, billing.timeToSeconds("14:02:03"));
        assertEquals(86399, billing.timeToSeconds("23:59:59"));
    }

    @Test
    public void testTimeToSecondsInvalid() {
        assertEquals(-1, billing.timeToSeconds("25:00:00"));
        assertEquals(-1, billing.timeToSeconds("14:60:00"));
        assertEquals(-1, billing.timeToSeconds("14:02:60"));
        assertEquals(-1, billing.timeToSeconds("invalid"));
        assertEquals(-1, billing.timeToSeconds("14:02"));
    }

    @Test
    public void testParseLogFileValid() throws IOException {
        File tempFile = File.createTempFile("test", ".log");
        tempFile.deleteOnExit();
        
        try (PrintWriter writer = new PrintWriter(tempFile)) {
            writer.println("14:02:03 ALICE99 Start");
            writer.println("14:02:34 ALICE99 End");
        }
        
        List<FairBilling.LogEntry> entries = billing.parseLogFile(tempFile.getAbsolutePath());
        assertEquals(2, entries.size());
        assertEquals("ALICE99", entries.get(0).username);
        assertEquals("Start", entries.get(0).eventType);
    }

    @Test
    public void testParseLogFileInvalidEntries() throws IOException {
        File tempFile = File.createTempFile("test", ".log");
        tempFile.deleteOnExit();
        
        try (PrintWriter writer = new PrintWriter(tempFile)) {
            writer.println("14:02:03 ALICE99 Start");
            writer.println("invalid line");
            writer.println("14:02:34 ALICE99 End");
        }
        
        List<FairBilling.LogEntry> entries = billing.parseLogFile(tempFile.getAbsolutePath());
        assertEquals(2, entries.size());
    }

    @Test
    public void testProcessSessionsSimplePair() {
        List<FairBilling.LogEntry> entries = new ArrayList<>();
        entries.add(new FairBilling.LogEntry(50523, "ALICE99", "Start"));
        entries.add(new FairBilling.LogEntry(50554, "ALICE99", "End"));
        
        Map<String, FairBilling.UserSession> sessions = billing.processSessions(entries);
        assertEquals(1, sessions.size());
        assertEquals(1, sessions.get("ALICE99").sessionCount);
        assertEquals(31, sessions.get("ALICE99").totalDuration);
    }

    @Test
    public void testProcessSessionsConcurrent() {
        List<FairBilling.LogEntry> entries = new ArrayList<>();
        entries.add(new FairBilling.LogEntry(100, "ALICE", "Start"));
        entries.add(new FairBilling.LogEntry(110, "ALICE", "Start"));
        entries.add(new FairBilling.LogEntry(120, "ALICE", "End"));
        entries.add(new FairBilling.LogEntry(130, "ALICE", "End"));
        
        Map<String, FairBilling.UserSession> sessions = billing.processSessions(entries);
        assertEquals(2, sessions.get("ALICE").sessionCount);
        // Greedy matching: 110-120 (10s) + 100-130 (30s) = 40s
        assertEquals(40, sessions.get("ALICE").totalDuration);
    }

    @Test
    public void testProcessSessionsOrphanedStart() {
        List<FairBilling.LogEntry> entries = new ArrayList<>();
        entries.add(new FairBilling.LogEntry(100, "ALICE", "Start"));
        
        Map<String, FairBilling.UserSession> sessions = billing.processSessions(entries);
        assertEquals(1, sessions.get("ALICE").sessionCount);
        assertEquals(0, sessions.get("ALICE").totalDuration);
    }

    @Test
    public void testProcessSessionsOrphanedEnd() {
        List<FairBilling.LogEntry> entries = new ArrayList<>();
        entries.add(new FairBilling.LogEntry(100, "ALICE", "End"));
        
        Map<String, FairBilling.UserSession> sessions = billing.processSessions(entries);
        // Orphaned End counts as a session
        assertEquals(1, sessions.get("ALICE").sessionCount);
        assertEquals(0, sessions.get("ALICE").totalDuration);
    }

    @Test
    public void testIntegrationExample() throws IOException {
        File tempFile = File.createTempFile("test", ".log");
        tempFile.deleteOnExit();
        
        try (PrintWriter writer = new PrintWriter(tempFile)) {
            writer.println("14:02:03 ALICE99 Start");
            writer.println("14:02:05 CHARLIE End");
            writer.println("14:02:34 ALICE99 End");
            writer.println("14:02:58 ALICE99 Start");
            writer.println("14:03:02 CHARLIE Start");
            writer.println("14:03:33 ALICE99 Start");
            writer.println("14:03:35 ALICE99 End");
            writer.println("14:03:37 CHARLIE End");
            writer.println("14:04:05 ALICE99 End");
            writer.println("14:04:23 ALICE99 End");
            writer.println("14:04:41 CHARLIE Start");
        }
        
        List<FairBilling.LogEntry> entries = billing.parseLogFile(tempFile.getAbsolutePath());
        Map<String, FairBilling.UserSession> sessions = billing.processSessions(entries);
        
        assertEquals(4, sessions.get("ALICE99").sessionCount);
        assertEquals(240, sessions.get("ALICE99").totalDuration);
        assertEquals(3, sessions.get("CHARLIE").sessionCount);
        assertEquals(37, sessions.get("CHARLIE").totalDuration);
    }

    @Test
    public void testOnlyEndsMultipleUsers() {
        // test2_only_ends.txt scenario
        List<FairBilling.LogEntry> entries = new ArrayList<>();
        entries.add(new FairBilling.LogEntry(36923, "BOB", "End"));      // 10:15:23
        entries.add(new FairBilling.LogEntry(37215, "ALICE", "End"));    // 10:20:15
        entries.add(new FairBilling.LogEntry(37530, "CHARLIE", "End"));  // 10:25:30
        
        Map<String, FairBilling.UserSession> sessions = billing.processSessions(entries);
        
        assertEquals(1, sessions.get("BOB").sessionCount);
        assertEquals(0, sessions.get("BOB").totalDuration);
        assertEquals(1, sessions.get("ALICE").sessionCount);
        assertEquals(37215 - 36923, sessions.get("ALICE").totalDuration);
        assertEquals(1, sessions.get("CHARLIE").sessionCount);
        assertEquals(37530 - 36923, sessions.get("CHARLIE").totalDuration);
    }

    @Test
    public void testOnlyStartsMultipleUsers() {
        // test3_only_starts.txt scenario
        List<FairBilling.LogEntry> entries = new ArrayList<>();
        entries.add(new FairBilling.LogEntry(32400, "ALICE", "Start"));    // 09:00:00
        entries.add(new FairBilling.LogEntry(34200, "BOB", "Start"));      // 09:30:00
        entries.add(new FairBilling.LogEntry(36000, "CHARLIE", "Start"));  // 10:00:00
        
        Map<String, FairBilling.UserSession> sessions = billing.processSessions(entries);
        
        assertEquals(1, sessions.get("ALICE").sessionCount);
        assertEquals(36000 - 32400, sessions.get("ALICE").totalDuration);
        assertEquals(1, sessions.get("BOB").sessionCount);
        assertEquals(36000 - 34200, sessions.get("BOB").totalDuration);
        assertEquals(1, sessions.get("CHARLIE").sessionCount);
        assertEquals(0, sessions.get("CHARLIE").totalDuration);
    }

    @Test
    public void testThreeConcurrentSessions() {
        // test5_concurrent.txt scenario
        List<FairBilling.LogEntry> entries = new ArrayList<>();
        entries.add(new FairBilling.LogEntry(28800, "ALICE", "Start"));  // 08:00:00
        entries.add(new FairBilling.LogEntry(29100, "ALICE", "Start"));  // 08:05:00
        entries.add(new FairBilling.LogEntry(29400, "ALICE", "Start"));  // 08:10:00
        entries.add(new FairBilling.LogEntry(29520, "ALICE", "End"));    // 08:12:00
        entries.add(new FairBilling.LogEntry(29700, "ALICE", "End"));    // 08:15:00
        entries.add(new FairBilling.LogEntry(30000, "ALICE", "End"));    // 08:20:00
        
        Map<String, FairBilling.UserSession> sessions = billing.processSessions(entries);
        
        assertEquals(3, sessions.get("ALICE").sessionCount);
        assertEquals(1920, sessions.get("ALICE").totalDuration);
    }

    @Test
    public void testMixedOrphanedEvents() {
        // test6_boundaries.txt scenario
        List<FairBilling.LogEntry> entries = new ArrayList<>();
        entries.add(new FairBilling.LogEntry(43200, "ZOE", "End"));      // 12:00:00
        entries.add(new FairBilling.LogEntry(43260, "MIKE", "Start"));   // 12:01:00
        entries.add(new FairBilling.LogEntry(43350, "MIKE", "End"));     // 12:02:30
        entries.add(new FairBilling.LogEntry(43380, "ZOE", "Start"));    // 12:03:00
        entries.add(new FairBilling.LogEntry(43440, "ZOE", "Start"));    // 12:04:00
        entries.add(new FairBilling.LogEntry(43500, "MIKE", "Start"));   // 12:05:00
        
        Map<String, FairBilling.UserSession> sessions = billing.processSessions(entries);
        
        assertEquals(3, sessions.get("ZOE").sessionCount);
        assertEquals(180, sessions.get("ZOE").totalDuration);
        assertEquals(2, sessions.get("MIKE").sessionCount);
        assertEquals(90, sessions.get("MIKE").totalDuration);
    }

    @Test
    public void testZeroDurationSessions() {
        // test7_same_time.txt scenario
        List<FairBilling.LogEntry> entries = new ArrayList<>();
        entries.add(new FairBilling.LogEntry(36000, "ALICE", "Start"));   // 10:00:00
        entries.add(new FairBilling.LogEntry(36000, "ALICE", "End"));     // 10:00:00
        entries.add(new FairBilling.LogEntry(36060, "BOB", "Start"));     // 10:01:00
        entries.add(new FairBilling.LogEntry(36060, "BOB", "End"));       // 10:01:00
        entries.add(new FairBilling.LogEntry(36120, "CHARLIE", "Start")); // 10:02:00
        entries.add(new FairBilling.LogEntry(36120, "CHARLIE", "End"));   // 10:02:00
        
        Map<String, FairBilling.UserSession> sessions = billing.processSessions(entries);
        
        assertEquals(1, sessions.get("ALICE").sessionCount);
        assertEquals(0, sessions.get("ALICE").totalDuration);
        assertEquals(1, sessions.get("BOB").sessionCount);
        assertEquals(0, sessions.get("BOB").totalDuration);
        assertEquals(1, sessions.get("CHARLIE").sessionCount);
        assertEquals(0, sessions.get("CHARLIE").totalDuration);
    }

    @Test
    public void testComplexMultiUser() {
        // test8_complex.txt scenario
        List<FairBilling.LogEntry> entries = new ArrayList<>();
        entries.add(new FairBilling.LogEntry(25200, "ALICE", "Start"));   // 07:00:00
        entries.add(new FairBilling.LogEntry(25800, "BOB", "Start"));     // 07:10:00
        entries.add(new FairBilling.LogEntry(26100, "ALICE", "End"));     // 07:15:00
        entries.add(new FairBilling.LogEntry(26400, "CHARLIE", "End"));   // 07:20:00
        entries.add(new FairBilling.LogEntry(26700, "BOB", "End"));       // 07:25:00
        entries.add(new FairBilling.LogEntry(27000, "ALICE", "Start"));   // 07:30:00
        entries.add(new FairBilling.LogEntry(27300, "CHARLIE", "Start")); // 07:35:00
        entries.add(new FairBilling.LogEntry(27600, "ALICE", "End"));     // 07:40:00
        entries.add(new FairBilling.LogEntry(27900, "DAVID", "Start"));   // 07:45:00
        entries.add(new FairBilling.LogEntry(28200, "DAVID", "End"));     // 07:50:00
        entries.add(new FairBilling.LogEntry(28500, "EVE", "End"));       // 07:55:00
        
        Map<String, FairBilling.UserSession> sessions = billing.processSessions(entries);
        
        assertEquals(2, sessions.get("ALICE").sessionCount);
        assertEquals(1500, sessions.get("ALICE").totalDuration);
        assertEquals(1, sessions.get("BOB").sessionCount);
        assertEquals(900, sessions.get("BOB").totalDuration);
        assertEquals(2, sessions.get("CHARLIE").sessionCount);
        assertEquals(1, sessions.get("DAVID").sessionCount);
        assertEquals(300, sessions.get("DAVID").totalDuration);
        assertEquals(1, sessions.get("EVE").sessionCount);
    }

    @Test
    public void testSingleUserComplexPattern() {
        // test9_single_user.txt scenario
        List<FairBilling.LogEntry> entries = new ArrayList<>();
        entries.add(new FairBilling.LogEntry(46800, "ZACK", "Start"));  // 13:00:00
        entries.add(new FairBilling.LogEntry(47100, "ZACK", "End"));    // 13:05:00
        entries.add(new FairBilling.LogEntry(47400, "ZACK", "Start"));  // 13:10:00
        entries.add(new FairBilling.LogEntry(47700, "ZACK", "Start"));  // 13:15:00
        entries.add(new FairBilling.LogEntry(48000, "ZACK", "End"));    // 13:20:00
        entries.add(new FairBilling.LogEntry(48300, "ZACK", "Start"));  // 13:25:00
        entries.add(new FairBilling.LogEntry(48600, "ZACK", "End"));    // 13:30:00
        entries.add(new FairBilling.LogEntry(48900, "ZACK", "End"));    // 13:35:00
        
        Map<String, FairBilling.UserSession> sessions = billing.processSessions(entries);
        
        assertEquals(4, sessions.get("ZACK").sessionCount);
        assertEquals(2400, sessions.get("ZACK").totalDuration);
    }

    @Test
    public void testMidnightBoundary() {
        // test10_extreme.txt scenario
        List<FairBilling.LogEntry> entries = new ArrayList<>();
        entries.add(new FairBilling.LogEntry(86398, "ALPHA", "Start"));   // 23:59:58
        entries.add(new FairBilling.LogEntry(86399, "ALPHA", "End"));     // 23:59:59
        entries.add(new FairBilling.LogEntry(86399, "BRAVO", "Start"));   // 23:59:59
        entries.add(new FairBilling.LogEntry(0, "CHARLIE", "End"));       // 00:00:00
        
        Map<String, FairBilling.UserSession> sessions = billing.processSessions(entries);
        
        assertEquals(1, sessions.get("ALPHA").sessionCount);
        assertEquals(1, sessions.get("ALPHA").totalDuration);
        assertEquals(1, sessions.get("BRAVO").sessionCount);
        assertEquals(0, sessions.get("BRAVO").totalDuration);
        assertEquals(1, sessions.get("CHARLIE").sessionCount);
        assertEquals(0, sessions.get("CHARLIE").totalDuration);
    }

    @Test
    public void testEmptyLog() {
        List<FairBilling.LogEntry> entries = new ArrayList<>();
        Map<String, FairBilling.UserSession> sessions = billing.processSessions(entries);
        
        assertEquals(0, sessions.size());
    }

    @Test
    public void testUserOrderPreservation() {
        List<FairBilling.LogEntry> entries = new ArrayList<>();
        entries.add(new FairBilling.LogEntry(100, "CHARLIE", "Start"));
        entries.add(new FairBilling.LogEntry(110, "ALICE", "Start"));
        entries.add(new FairBilling.LogEntry(120, "BOB", "Start"));
        entries.add(new FairBilling.LogEntry(130, "CHARLIE", "End"));
        entries.add(new FairBilling.LogEntry(140, "ALICE", "End"));
        entries.add(new FairBilling.LogEntry(150, "BOB", "End"));
        
        Map<String, FairBilling.UserSession> sessions = billing.processSessions(entries);
        List<String> userList = new ArrayList<>(sessions.keySet());
        
        assertEquals(Arrays.asList("CHARLIE", "ALICE", "BOB"), userList);
    }
}
