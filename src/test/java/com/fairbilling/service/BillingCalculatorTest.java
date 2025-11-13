package com.fairbilling.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.fairbilling.domain.LogEntry;
import com.fairbilling.domain.SessionEventType;
import com.fairbilling.domain.UserBillingSummary;

public class BillingCalculatorTest {

    private BillingCalculator calculator;

    @Before
    public void setUp() {
        calculator = new BillingCalculator();
    }

    @Test
    public void processesSimplePair() {
        List<LogEntry> entries = Arrays.asList(
                entry(50523, "ALICE99", SessionEventType.START),
                entry(50554, "ALICE99", SessionEventType.END));

        Map<String, UserBillingSummary> summary = summarize(entries);

        assertEquals(1, summary.size());
        assertEquals(1, summary.get("ALICE99").getSessionCount());
        assertEquals(31, summary.get("ALICE99").getTotalDurationSeconds());
    }

    @Test
    public void handlesConcurrentSessions() {
        List<LogEntry> entries = Arrays.asList(
                entry(100, "ALICE", SessionEventType.START),
                entry(110, "ALICE", SessionEventType.START),
                entry(120, "ALICE", SessionEventType.END),
                entry(130, "ALICE", SessionEventType.END));

        Map<String, UserBillingSummary> summary = summarize(entries);

        assertEquals(2, summary.get("ALICE").getSessionCount());
        assertEquals(40, summary.get("ALICE").getTotalDurationSeconds());
    }

    @Test
    public void handlesOrphanedStart() {
        List<LogEntry> entries = Collections.singletonList(entry(100, "ALICE", SessionEventType.START));

        Map<String, UserBillingSummary> summary = summarize(entries);

        assertEquals(1, summary.get("ALICE").getSessionCount());
        assertEquals(0, summary.get("ALICE").getTotalDurationSeconds());
    }

    @Test
    public void handlesOrphanedEnd() {
        List<LogEntry> entries = Collections.singletonList(entry(100, "ALICE", SessionEventType.END));

        Map<String, UserBillingSummary> summary = summarize(entries);

        assertEquals(1, summary.get("ALICE").getSessionCount());
        assertEquals(0, summary.get("ALICE").getTotalDurationSeconds());
    }

    @Test
    public void integrationScenarioAcrossUsers() {
        List<LogEntry> entries = new ArrayList<>();
        entries.add(entry(50523, "ALICE99", SessionEventType.START));
        entries.add(entry(50525, "CHARLIE", SessionEventType.END));
        entries.add(entry(50554, "ALICE99", SessionEventType.END));
        entries.add(entry(50578, "ALICE99", SessionEventType.START));
        entries.add(entry(50582, "CHARLIE", SessionEventType.START));
        entries.add(entry(50593, "ALICE99", SessionEventType.START));
        entries.add(entry(50595, "ALICE99", SessionEventType.END));
        entries.add(entry(50617, "CHARLIE", SessionEventType.END));
        entries.add(entry(50645, "ALICE99", SessionEventType.END));
        entries.add(entry(50663, "ALICE99", SessionEventType.END));
        entries.add(entry(50681, "CHARLIE", SessionEventType.START));

        Map<String, UserBillingSummary> summary = summarize(entries);

        assertEquals(4, summary.get("ALICE99").getSessionCount());
        assertEquals(240, summary.get("ALICE99").getTotalDurationSeconds());
        assertEquals(3, summary.get("CHARLIE").getSessionCount());
        assertEquals(37, summary.get("CHARLIE").getTotalDurationSeconds());
    }

    @Test
    public void preservesUserOrder() {
        List<LogEntry> entries = Arrays.asList(
                entry(100, "CHARLIE", SessionEventType.START),
                entry(110, "ALICE", SessionEventType.START),
                entry(120, "BOB", SessionEventType.START));

        List<UserBillingSummary> results = calculator.calculate(entries);
        List<String> usernames = results.stream()
                .map(UserBillingSummary::getUsername)
                .collect(Collectors.toList());

        assertEquals(Arrays.asList("CHARLIE", "ALICE", "BOB"), usernames);
    }

    @Test
    public void handlesEmptyInput() {
        Collection<UserBillingSummary> summaries = calculator.calculate(Collections.emptyList());
        assertTrue(summaries.isEmpty());
    }

    private Map<String, UserBillingSummary> summarize(List<LogEntry> entries) {
        return calculator.calculate(entries).stream()
                .collect(Collectors.toMap(UserBillingSummary::getUsername, summary -> summary));
    }

    private LogEntry entry(int seconds, String user, SessionEventType type) {
        return new LogEntry(seconds, user, type);
    }
}
