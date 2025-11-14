# Fair Billing System: Java Implementation

[![CI](https://github.com/ayushjaipuriyar/bt_group_assessment/actions/workflows/ci.yml/badge.svg)](https://github.com/ayushjaipuriyar/bt_group_assessment/actions/workflows/ci.yml)

<!-- Badges: CI status, Codecov, Latest Release -->
[![Codecov](https://codecov.io/gh/ayushjaipuriyar/bt_group_assessment/branch/main/graph/badge.svg?token=)](https://codecov.io/gh/ayushjaipuriyar/bt_group_assessment)
[![Release](https://img.shields.io/github/v/release/ayushjaipuriyar/bt_group_assessment.svg)](https://github.com/ayushjaipuriyar/bt_group_assessment/releases/latest)

## Problem Description

As specified in *fair-billing 1.pdf*, the Fair Billing challenge requires processing session log files to calculate billing summaries for users. Log entries are in the format `HH:MM:SS USERNAME Start|End`. For each user, compute the number of billable sessions and total duration in seconds. Handle irregularities such as unmatched events, concurrent sessions, and temporal anomalies.

## Solution Overview

This Java implementation processes log files and generates billing reports. It adheres to *CodeTestGenericRules.pdf* for code quality, modularity, and testing. The solution uses a stack-based algorithm for event pairing, ensuring accurate duration calculations even with imperfect data.

## Architecture

The project is structured into packages:

- `app/`: Main application entry point.
- `io/`: Log file parsing and validation.
- `service/`: Billing calculation logic.
- `domain/`: Data models (LogEntry, UserBillingSummary, etc.).
- `report/`: Output formatting.
- `util/`: Time parsing utilities.

## Algorithm

A stack-based approach pairs "Start" and "End" events per user:

- "Start" events push timestamps onto a stack.
- "End" events pop the latest "Start" to calculate duration.
- Orphaned "End" events assume start at 00:00:00.
- Unmatched "Start" events end at 23:59:59.
- Concurrent sessions are handled via LIFO pairing.

This method ensures fairness and handles edge cases robustly.

## Implementation

- **Language**: Java (standard library only).
- **Build Tools**: Gradle and Maven (both supported).
- **Compatibility**: Java 8+ runtime (bytecode target 8, compiled with 21 in CI).
- **Error Handling**: Validates inputs, skips invalid entries, provides usage messages.
- **CI/CD**: Automated testing and security checks via GitHub Actions across multiple JDK versions.

## Testing

Comprehensive test suite covers:

- Unit tests for individual components.
- Integration tests for end-to-end functionality.
- Edge cases: orphans, concurrency, boundaries.

Run tests with `./gradlew test` or `mvn test`.

## Usage

### Using Gradle

Build and run:
```bash
./gradlew build
./gradlew run --args='test-data/example.log'
```

Run tests:
```bash
./gradlew test
```

### Using Maven

Build and run:
```bash
mvn clean package
java -jar target/fair-billing-1.0.0.jar test-data/example.log
```

Run tests:
```bash
mvn test
```

### Direct JAR Execution

After building with either tool:
```bash
java -jar build/libs/fair-billing-1.0.0.jar test-data/example.log
# or
java -jar target/fair-billing-1.0.0.jar test-data/example.log
```

Example output:
```
ALICE99 4 240
CHARLIE 3 37
```

## Assumptions

- Logs are processed in file order.
- Timestamps within one day.
- Usernames alphanumeric.
- Events "Start" or "End" (case-insensitive).

## Java-oriented algorithm

The following is a concise, Java-focused description of the algorithm and the data structures to implement it.

1) Data structures

```java
Map<String, Deque<Integer>> activeStarts = new LinkedHashMap<>(); // preserves insertion order
Map<String, List<int[]>> sessions = new HashMap<>();              // username -> list of {start, end}
Map<String, Integer> sessionCount = new HashMap<>();             // username -> count of Start events
List<String> userOrder = new ArrayList<>();                      // preserves order of appearance

int earliestTimestamp = Integer.MAX_VALUE;
int latestTimestamp = Integer.MIN_VALUE;
```

2) Time parsing utility

```java
private static int parseTime(String ts) {
	String[] p = ts.split(":");
	int h = Integer.parseInt(p[0]);
	int m = Integer.parseInt(p[1]);
	int s = Integer.parseInt(p[2]);
	return h * 3600 + m * 60 + s;
}
```

3) Per-line parsing and setup

For each input line:
- If it matches the regex `^([0-9]{2}:[0-9]{2}:[0-9]{2})\s+([A-Za-z0-9]+)\s+(Start|End)$` (case-sensitive for token matching but you may normalize), parse time, username and event type.
- Convert the timestamp to seconds via `parseTime` and update `earliestTimestamp` and `latestTimestamp`.
- If the username is new: `userOrder.add(user); activeStarts.put(user, new ArrayDeque<>()); sessions.put(user, new ArrayList<>()); sessionCount.put(user, 0);`.

4) Processing events

Start event:

```java
activeStarts.get(user).push(time);
sessionCount.put(user, sessionCount.getOrDefault(user, 0) + 1);
```

End event:

```java
Deque<Integer> stack = activeStarts.get(user);
if (stack != null && !stack.isEmpty()) {
	int start = stack.pop();
	sessions.get(user).add(new int[]{start, time});
} else {
	// Orphaned End: assume earliestTimestamp as start
	sessions.get(user).add(new int[]{earliestTimestamp == Integer.MAX_VALUE ? 0 : earliestTimestamp, time});
}
```

The stack semantics (LIFO) ensure minimal duration pairing for concurrent sessions.

5) Close remaining open starts after all lines processed

```java
for (String user : userOrder) {
	Deque<Integer> stack = activeStarts.get(user);
	while (stack != null && !stack.isEmpty()) {
		int start = stack.pop();
		sessions.get(user).add(new int[]{start, latestTimestamp == Integer.MIN_VALUE ? 24*3600 - 1 : latestTimestamp});
	}
}
```

6) Compute totals and emit report

```java
for (String user : userOrder) {
	int total = 0;
	for (int[] s : sessions.get(user)) {
		total += Math.max(0, s[1] - s[0]);
	}
	int count = sessionCount.getOrDefault(user, 0);
	System.out.println(user + " " + count + " " + total);
}
```

Notes
- Use `LinkedHashMap` or maintain `userOrder` to preserve user first-seen ordering in the report.
- Handle invalid lines by skipping them; do not throw. Keep parsing robust.
- `earliestTimestamp` fallback uses `0` if no valid timestamps were parsed; `latestTimestamp` fallback uses `23:59:59` (`24*3600 - 1`) if needed.

This block provides the exact Java data shapes and per-step operations to implement the stack-based pairing algorithm described above.


