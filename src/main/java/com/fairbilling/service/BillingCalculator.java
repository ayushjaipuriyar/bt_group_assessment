package com.fairbilling.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fairbilling.domain.LogEntry;
import com.fairbilling.domain.UserBillingSummary;

/**
 * Coordinates session aggregation and produces per-user billing summaries.
 */
public class BillingCalculator {

    public List<UserBillingSummary> calculate(List<LogEntry> logEntries) {
        Objects.requireNonNull(logEntries, "logEntries");
        if (logEntries.isEmpty()) {
            return new ArrayList<>();
        }

        int earliest = logEntries.stream().mapToInt(LogEntry::getSecondsSinceMidnight).min().orElse(0);
        int latest = logEntries.stream().mapToInt(LogEntry::getSecondsSinceMidnight).max().orElse(0);

        Map<String, List<LogEntry>> entriesByUser = groupEntriesByUser(logEntries);
        List<UserBillingSummary> summaries = new ArrayList<>(entriesByUser.size());

        for (Map.Entry<String, List<LogEntry>> entry : entriesByUser.entrySet()) {
            UserSessionAccumulator accumulator = new UserSessionAccumulator(earliest, latest);
            entry.getValue().forEach(accumulator::accept);
            summaries.add(accumulator.toSummary(entry.getKey()));
        }

        return summaries;
    }

    private Map<String, List<LogEntry>> groupEntriesByUser(List<LogEntry> entries) {
        Map<String, List<LogEntry>> grouped = new LinkedHashMap<>();
        for (LogEntry entry : entries) {
            grouped.computeIfAbsent(entry.getUsername(), key -> new ArrayList<>()).add(entry);
        }
        return grouped;
    }

    private static final class UserSessionAccumulator {
        private final int earliestTimestamp;
        private final int latestTimestamp;
        private final Deque<Integer> unmatchedStarts = new ArrayDeque<>();
        private final Deque<Integer> unmatchedEnds = new ArrayDeque<>();
        private int sessionCount;
        private int totalDurationSeconds;

        private UserSessionAccumulator(int earliestTimestamp, int latestTimestamp) {
            this.earliestTimestamp = earliestTimestamp;
            this.latestTimestamp = latestTimestamp;
        }

        private void accept(LogEntry entry) {
            int timestamp = entry.getSecondsSinceMidnight();
            if (entry.isStartEvent()) {
                unmatchedStarts.push(timestamp);
            } else {
                if (!unmatchedStarts.isEmpty()) {
                    int startTimestamp = unmatchedStarts.pop();
                    addSession(durationBetween(startTimestamp, timestamp));
                } else {
                    unmatchedEnds.push(timestamp);
                }
            }
        }

        private UserBillingSummary toSummary(String username) {
            Objects.requireNonNull(username, "username");
            settleOrphanedEnds();
            settleOrphanedStarts();
            return new UserBillingSummary(username, sessionCount, totalDurationSeconds);
        }

        private void settleOrphanedEnds() {
            while (!unmatchedEnds.isEmpty()) {
                int endTimestamp = unmatchedEnds.pop();
                addSession(durationBetween(earliestTimestamp, endTimestamp));
            }
        }

        private void settleOrphanedStarts() {
            while (!unmatchedStarts.isEmpty()) {
                int startTimestamp = unmatchedStarts.pop();
                addSession(durationBetween(startTimestamp, latestTimestamp));
            }
        }

        private void addSession(int duration) {
            sessionCount++;
            totalDurationSeconds += Math.max(0, duration);
        }

        private int durationBetween(int start, int end) {
            return end - start;
        }
    }
}
