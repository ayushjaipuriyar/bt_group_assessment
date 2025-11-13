# Fair Billing System: Java Implementation

[![CI](https://github.com/ayushjaipuriyar/bt_group_test/actions/workflows/ci.yml/badge.svg)](https://github.com/ayushjaipuriyar/bt_group_test/actions/workflows/ci.yml)

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

