# Test Data Documentation

This directory contains comprehensive test cases for the Fair Billing system. Each test file validates different aspects of session log parsing and billing calculation.

## File Format

Log files follow the format:
```
HH:MM:SS USERNAME Action
```

Where:
- `HH:MM:SS` - Timestamp in 24-hour format
- `USERNAME` - User identifier (alphanumeric)
- `Action` - Either "Start" or "End"

## Test Categories

### Basic Functionality Tests (1-10)

#### test1_basic.log (example.log)
**Purpose:** Standard mixed scenario with multiple users and sessions  
**Coverage:**
- Multiple users (ALICE99, CHARLIE)
- Matched and unmatched Start/End events
- Multiple sessions per user
- Out-of-order events

**Expected Output:**
- ALICE99: 4 sessions, 240 seconds
- CHARLIE: 3 sessions, 37 seconds

#### test2_only_ends.log
**Purpose:** Handle logs with only End events  
**Coverage:**
- All End events without matching Starts
- Multiple users
- Minimum session duration (0 seconds)

**Expected Output:**
- ALICE: 1 session, 292 seconds
- BOB: 1 session, 0 seconds
- CHARLIE: 1 session, 607 seconds

#### test3_only_starts.log
**Purpose:** Handle logs with only Start events  
**Coverage:**
- All Start events without matching Ends
- Maximum session duration (until end of log)
- Multiple users

**Expected Output:**
- ALICE: 1 session, 3600 seconds
- BOB: 1 session, 1800 seconds
- CHARLIE: 1 session, 0 seconds

#### test4_with_invalid.log
**Purpose:** Resilience to malformed log entries  
**Coverage:**
- Invalid/malformed lines mixed with valid entries
- Incomplete data
- Garbage text
- Valid sessions between invalid entries

**Expected Output:**
- ALICE: 1 session, 930 seconds
- BOB: 1 session, 1500 seconds
- CHARLIE: 2 sessions, 900 seconds

#### test5_concurrent.log
**Purpose:** Multiple concurrent sessions for single user  
**Coverage:**
- Multiple Start events before any End
- LIFO (Last In, First Out) session matching
- Single user with overlapping sessions

**Expected Output:**
- ALICE: 3 sessions, 1920 seconds

#### test6_boundaries.log
**Purpose:** Mixed matched and unmatched events  
**Coverage:**
- Unmatched End at beginning
- Unmatched Start at end
- Multiple users with different patterns

**Expected Output:**
- MIKE: 2 sessions, 90 seconds
- ZOE: 3 sessions, 180 seconds

#### test7_same_time.log
**Purpose:** Zero-duration sessions  
**Coverage:**
- Start and End at identical timestamps
- Multiple users with same pattern
- Minimum valid session duration

**Expected Output:**
- ALICE: 1 session, 0 seconds
- BOB: 1 session, 0 seconds
- CHARLIE: 1 session, 0 seconds

#### test8_complex.log
**Purpose:** Complex multi-user scenario  
**Coverage:**
- 5 different users
- Mix of matched and unmatched events
- Various session durations
- Interleaved user activities

**Expected Output:**
- ALICE: 2 sessions, 1500 seconds
- BOB: 1 session, 900 seconds
- CHARLIE: 2 sessions, 2400 seconds
- DAVID: 1 session, 300 seconds
- EVE: 1 session, 4400 seconds

#### test9_single_user.log
**Purpose:** Single user with multiple sessions  
**Coverage:**
- One user, multiple sessions
- Mix of matched and unmatched events
- Session stacking behavior

**Expected Output:**
- ZACK: 4 sessions, 2400 seconds

#### test10_extreme.log
**Purpose:** Boundary timestamp handling  
**Coverage:**
- Near-midnight timestamps
- 1-second sessions
- Day boundary crossing (23:59:59 to 00:00:00)

**Expected Output:**
- ALPHA: 1 session, 1 second
- BRAVO: 1 session, 0 seconds
- CHARLIE: 1 session, 1 second

### Edge Cases (11-30)

#### test11_multi_unmatched_starts.log
**Purpose:** Multiple unmatched Start events  
**Coverage:**
- Only Start events for single user
- Multiple consecutive Starts
- Session duration calculation to end of log

**Expected Output:**
- ALICE: 3 sessions, 1800 seconds

#### test12_multi_unmatched_ends.log
**Purpose:** Multiple unmatched End events  
**Coverage:**
- Only End events for single user
- Multiple consecutive Ends
- Session duration calculation from beginning of log

**Expected Output:**
- ALICE: 3 sessions, 1800 seconds

#### test13_single_pair.log
**Purpose:** Simplest valid scenario  
**Coverage:**
- Single user
- Single matched Start-End pair
- Basic duration calculation

**Expected Output:**
- ALICE: 1 session, 600 seconds

#### test14_unmatched_before_matched.log
**Purpose:** Unmatched End before matched pair  
**Coverage:**
- Unmatched End at beginning
- Followed by matched Start-End pair
- Mixed session types for single user

**Expected Output:**
- ALICE: 2 sessions, 600 seconds

#### test15_unmatched_after_matched.log
**Purpose:** Unmatched Start after matched pair  
**Coverage:**
- Matched Start-End pair
- Followed by unmatched Start
- Mixed session types for single user

**Expected Output:**
- ALICE: 2 sessions, 600 seconds

#### test16_alternating_unmatched.log
**Purpose:** Alternating unmatched events  
**Coverage:**
- Unmatched End, matched pair, unmatched Start
- Complex session pattern for single user

**Expected Output:**
- ALICE: 3 sessions, 1200 seconds

#### test17_nested_sessions.log
**Purpose:** Nested/stacked session handling  
**Coverage:**
- Multiple Starts before Ends
- LIFO matching behavior
- Nested session duration calculation

**Expected Output:**
- ALICE: 3 sessions, 2700 seconds

#### test18_keyword_usernames.log
**Purpose:** Usernames that match keywords  
**Coverage:**
- Username "Start" with Start/End actions
- Username "End" with Start/End actions
- Normal username for comparison
- Parser robustness with confusing names

**Expected Output:**
- End: 1 session, 600 seconds
- NORMAL: 1 session, 300 seconds
- Start: 1 session, 600 seconds

#### test19_empty.log
**Purpose:** Empty file handling  
**Coverage:**
- Completely empty log file
- No output expected

**Expected Output:**
- [no output]

#### test20_all_invalid.log
**Purpose:** File with only invalid entries  
**Coverage:**
- All lines are malformed
- No valid log entries
- Graceful handling of completely invalid input

**Expected Output:**
- [no output]

#### test21_single_start.log
**Purpose:** Single unmatched Start  
**Coverage:**
- Only one Start event
- No End event
- Minimum valid input

**Expected Output:**
- SOLO: 1 session, 0 seconds

#### test22_single_end.log
**Purpose:** Single unmatched End  
**Coverage:**
- Only one End event
- No Start event
- Minimum valid input

**Expected Output:**
- SOLO: 1 session, 0 seconds

#### test23_same_timestamp.log
**Purpose:** Multiple events at same timestamp  
**Coverage:**
- Multiple Start/End events at identical times
- Same user with multiple zero-duration sessions
- Different users at same timestamp

**Expected Output:**
- ALICE: 2 sessions, 0 seconds
- BOB: 1 session, 0 seconds
- CHARLIE: 1 session, 0 seconds

#### test24_full_day.log
**Purpose:** Maximum session duration  
**Coverage:**
- Session spanning nearly entire day
- Start at 00:00:00, End at 23:59:59
- Maximum valid duration (86399 seconds)

**Expected Output:**
- ALICE: 1 session, 86399 seconds

#### test25_multi_user_boundary.log
**Purpose:** Multiple users with boundary conditions  
**Coverage:**
- Large time spans
- Multiple users with different patterns
- High-duration sessions

**Expected Output:**
- ZOE: 2 sessions, 57598 seconds
- MIKE: 1 session, 2 seconds
- CHARLIE: 1 session, 1 second

#### test26_high_concurrency.log
**Purpose:** High concurrency for single user  
**Coverage:**
- Many rapid Start/End events
- Short session durations
- High session count

**Expected Output:**
- ALICE: 4 sessions, 16 seconds

#### test27_boundary_precision.log
**Purpose:** Precise timestamp handling  
**Coverage:**
- 1-second precision sessions
- Multiple users with minimal durations

**Expected Output:**
- ALPHA: 1 session, 1 second
- BOB: 1 session, 1 second

#### test28_max_duration.log
**Purpose:** Maximum possible session duration  
**Coverage:**
- Full day session (86399 seconds)
- Single user, single session

**Expected Output:**
- ZZZ: 1 session, 86399 seconds

#### test29_tab_separated.log
**Purpose:** Mixed whitespace handling  
**Coverage:**
- Tab-separated values
- Space-separated values
- Mixed valid and invalid entries

**Expected Output:**
- USER1: 1 session, 600 seconds
- USER2: 1 session, 600 seconds

#### test30_long_username.log
**Purpose:** Long username handling  
**Coverage:**
- Very long username (36+ characters)
- Normal session behavior with long identifier

**Expected Output:**
- USER123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ: 1 session, 600 seconds

## Expected Results

The `EXPECTED_RESULTS.log` file contains the expected output for all test cases. Each test result shows:
- Username
- Number of sessions
- Total billable seconds

## Testing Strategy

These tests cover:
1. **Normal Operations:** Standard use cases with multiple users and sessions
2. **Edge Cases:** Boundary conditions, empty files, single events
3. **Error Handling:** Invalid data, malformed entries, missing fields
4. **Session Matching:** LIFO behavior, unmatched events, concurrent sessions
5. **Time Handling:** Zero duration, maximum duration, same timestamps, day boundaries
6. **Data Validation:** Special characters in usernames, whitespace variations, keyword conflicts