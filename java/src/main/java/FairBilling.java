import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Fair Billing System - Process session log files to generate billing reports.
 * 
 * <p>This class implements a billing system that processes session log files containing
 * Start and End events for users. It calculates the total session duration and count
 * for each user using a greedy matching algorithm that ensures minimum possible duration.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Matches Start-End pairs chronologically using LIFO (stack) approach</li>
 *   <li>Handles orphaned Start events (assume session ended at latest timestamp)</li>
 *   <li>Handles orphaned End events (assume session started at earliest timestamp)</li>
 *   <li>Supports concurrent sessions for the same user</li>
 *   <li>Silently ignores invalid log entries</li>
 * </ul>
 * 
 * <p>Usage:</p>
 * <pre>
 * java FairBilling &lt;log_file_path&gt;
 * </pre>
 * 
 * <p>Example:</p>
 * <pre>
 * java FairBilling logs/session.log
 * </pre>
 * 
 * <p>Output Format:</p>
 * <pre>
 * USERNAME SESSION_COUNT TOTAL_DURATION
 * </pre>
 * 
 * @author Fair Billing System
 * @version 1.0.0
 */
public class FairBilling {

    /**
     * Represents a single parsed log entry from the session log file.
     * 
     * <p>Each log entry contains a timestamp (seconds since midnight),
     * a username, and an event type (Start or End).</p>
     */
    static class LogEntry {
        /** Seconds since midnight (0-86399) */
        int timestamp;
        
        /** Alphanumeric username identifier */
        String username;
        
        /** Event type: "Start" or "End" */
        String eventType;

        /**
         * Constructs a new LogEntry.
         * 
         * @param timestamp Seconds since midnight (0-86399)
         * @param username User identifier
         * @param eventType "Start" or "End"
         */
        public LogEntry(int timestamp, String username, String eventType) {
            this.timestamp = timestamp;
            this.username = username;
            this.eventType = eventType;
        }
    }

    /**
     * Represents aggregated billing information for a single user.
     * 
     * <p>Tracks the number of sessions (Start events) and total duration
     * across all sessions for a specific user.</p>
     */
    static class UserSession {
        /** The user's identifier */
        String username;
        
        /** Number of Start events (billable sessions) */
        int sessionCount;
        
        /** Sum of all session durations in seconds */
        int totalDuration;

        /**
         * Constructs a new UserSession with zero sessions and duration.
         * 
         * @param username User identifier
         */
        public UserSession(String username) {
            this.username = username;
            this.sessionCount = 0;
            this.totalDuration = 0;
        }
    }

    /**
     * Converts HH:MM:SS time format to seconds since midnight.
     * 
     * <p>Validates the time format and range constraints:</p>
     * <ul>
     *   <li>Hours: 0-23</li>
     *   <li>Minutes: 0-59</li>
     *   <li>Seconds: 0-59</li>
     * </ul>
     * 
     * @param timeStr Time in HH:MM:SS format (e.g., "14:02:03")
     * @return Seconds since midnight (0-86399), or -1 if invalid
     * 
     * @example
     * <pre>
     * timeToSeconds("00:00:00") // returns 0
     * timeToSeconds("14:02:03") // returns 50523
     * timeToSeconds("23:59:59") // returns 86399
     * timeToSeconds("25:00:00") // returns -1 (invalid)
     * </pre>
     */
    public static int timeToSeconds(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            if (parts.length != 3) {
                return -1;
            }
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            
            if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59 || seconds < 0 || seconds > 59) {
                return -1;
            }
            
            return hours * 3600 + minutes * 60 + seconds;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Parses a log file and returns a list of valid LogEntry objects.
     * 
     * <p>Expected log format: HH:MM:SS USERNAME Start|End</p>
     * 
     * <p>Invalid entries are silently ignored. An entry is considered invalid if:</p>
     * <ul>
     *   <li>It doesn't match the expected format</li>
     *   <li>The timestamp is invalid (out of range)</li>
     *   <li>The username is not alphanumeric</li>
     *   <li>The event type is not exactly "Start" or "End"</li>
     * </ul>
     * 
     * @param filePath Path to the log file to process
     * @return List of valid LogEntry objects in chronological order
     * @throws IOException If file cannot be read
     * @throws FileNotFoundException If the specified file does not exist
     * 
     * @example
     * <pre>
     * List&lt;LogEntry&gt; entries = parseLogFile("session.log");
     * System.out.println(entries.size()); // Number of valid entries
     * </pre>
     */
    public static List<LogEntry> parseLogFile(String filePath) throws IOException {
        List<LogEntry> entries = new ArrayList<>();
        Pattern pattern = Pattern.compile("^(\\d{2}:\\d{2}:\\d{2})\\s+(\\w+)\\s+(Start|End)\\s*$");
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    String timeStr = matcher.group(1);
                    String username = matcher.group(2);
                    String eventType = matcher.group(3);
                    
                    int timestamp = timeToSeconds(timeStr);
                    if (timestamp != -1) {
                        entries.add(new LogEntry(timestamp, username, eventType));
                    }
                }
            }
        }
        
        return entries;
    }

    /**
     * Processes log entries and calculates session data for each user.
     * 
     * <p>Algorithm:</p>
     * <ol>
     *   <li>Group entries by username while preserving order of first occurrence</li>
     *   <li>For each user, process events chronologically:
     *     <ul>
     *       <li>Match each End with the most recent unmatched Start (LIFO/stack)</li>
     *       <li>Track orphaned Start and End events</li>
     *     </ul>
     *   </li>
     *   <li>Handle orphaned events:
     *     <ul>
     *       <li>Orphaned End: Assume session started at earliest timestamp in file</li>
     *       <li>Orphaned Start: Assume session ended at latest timestamp in file</li>
     *     </ul>
     *   </li>
     *   <li>Count sessions based on number of Start events (including orphaned)</li>
     * </ol>
     * 
     * <p>This greedy matching approach ensures minimum possible total duration
     * while supporting concurrent sessions for the same user.</p>
     * 
     * @param logEntries List of parsed log entries
     * @return Map of username to UserSession data, ordered by first occurrence in log file
     * 
     * @example
     * <pre>
     * List&lt;LogEntry&gt; entries = new ArrayList&lt;&gt;();
     * entries.add(new LogEntry(100, "ALICE", "Start"));
     * entries.add(new LogEntry(200, "ALICE", "End"));
     * Map&lt;String, UserSession&gt; sessions = processSessions(entries);
     * System.out.println(sessions.get("ALICE").sessionCount); // 1
     * System.out.println(sessions.get("ALICE").totalDuration); // 100
     * </pre>
     */
    public static Map<String, UserSession> processSessions(List<LogEntry> logEntries) {
        if (logEntries.isEmpty()) {
            return new LinkedHashMap<>();
        }

        // Find earliest and latest timestamps
        int earliestTime = logEntries.get(0).timestamp;
        int latestTime = logEntries.get(0).timestamp;
        for (LogEntry entry : logEntries) {
            if (entry.timestamp < earliestTime) {
                earliestTime = entry.timestamp;
            }
            if (entry.timestamp > latestTime) {
                latestTime = entry.timestamp;
            }
        }

        // Group entries by username
        Map<String, List<LogEntry>> userEntries = new LinkedHashMap<>();
        for (LogEntry entry : logEntries) {
            userEntries.putIfAbsent(entry.username, new ArrayList<>());
            userEntries.get(entry.username).add(entry);
        }

        // Process each user's sessions
        Map<String, UserSession> userSessions = new LinkedHashMap<>();
        for (Map.Entry<String, List<LogEntry>> userEntry : userEntries.entrySet()) {
            String username = userEntry.getKey();
            List<LogEntry> entries = userEntry.getValue();
            
            UserSession session = new UserSession(username);
            Deque<Integer> unmatchedStarts = new ArrayDeque<>();
            Deque<Integer> unmatchedEnds = new ArrayDeque<>();

            // Process entries chronologically
            for (LogEntry entry : entries) {
                if (entry.eventType.equals("Start")) {
                    unmatchedStarts.push(entry.timestamp); // Push to stack
                } else { // End
                    if (!unmatchedStarts.isEmpty()) {
                        int startTime = unmatchedStarts.pop(); // LIFO: pop from stack
                        session.totalDuration += (entry.timestamp - startTime);
                        session.sessionCount++;
                    } else {
                        unmatchedEnds.push(entry.timestamp);
                    }
                }
            }

            // Handle orphaned ends (use earliest time as start)
            while (!unmatchedEnds.isEmpty()) {
                int endTime = unmatchedEnds.pop();
                session.totalDuration += (endTime - earliestTime);
                session.sessionCount++;
            }

            // Handle orphaned starts (use latest time as end)
            while (!unmatchedStarts.isEmpty()) {
                int startTime = unmatchedStarts.pop();
                session.totalDuration += (latestTime - startTime);
                session.sessionCount++;
            }

            userSessions.put(username, session);
        }

        return userSessions;
    }

    /**
     * Generates and prints the billing report to stdout.
     * 
     * <p>Output format: USERNAME SESSION_COUNT TOTAL_DURATION</p>
     * <p>Users are printed in order of first occurrence in the log file.</p>
     * 
     * @param userSessions Map of user session data
     * 
     * @example
     * <pre>
     * // Output:
     * // ALICE99 4 240
     * // CHARLIE 3 37
     * </pre>
     */
    public static void generateReport(Map<String, UserSession> userSessions) {
        for (UserSession session : userSessions.values()) {
            System.out.println(session.username + " " + session.sessionCount + " " + session.totalDuration);
        }
    }

    /**
     * Main entry point for the Fair Billing System.
     * 
     * <p>Processes command-line arguments, reads the log file, calculates billing
     * information, and outputs the report. Handles errors gracefully with
     * appropriate error messages and exit codes.</p>
     * 
     * <p>Exit Codes:</p>
     * <ul>
     *   <li>0: Success</li>
     *   <li>1: Error (file not found, cannot read, or invalid arguments)</li>
     * </ul>
     * 
     * @param args Command-line arguments (expects one argument: log file path)
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java FairBilling <log_file_path>");
            System.exit(1);
        }

        String filePath = args[0];

        try {
            List<LogEntry> logEntries = parseLogFile(filePath);
            Map<String, UserSession> userSessions = processSessions(logEntries);
            generateReport(userSessions);
        } catch (FileNotFoundException e) {
            System.err.println("Error: File not found: " + filePath);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error: Unable to read file: " + filePath);
            System.exit(1);
        }
    }
}
