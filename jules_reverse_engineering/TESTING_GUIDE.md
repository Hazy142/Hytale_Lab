# Hytale Security Testing Guide

Complete guide for local setup and testing of the bug bounty framework.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Local Testing Without Server](#local-testing-without-server)
3. [Testing Against Local Server](#testing-against-local-server)
4. [Testing Against Remote Server](#testing-against-remote-server)
5. [Understanding Test Output](#understanding-test-output)
6. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Prerequisites

- Python 3.8 or higher
- Local Hytale server (optional for basic testing)
- Admin/sudo privileges (optional, for packet capture)

### Installation

```bash
# Navigate to the security research directory
cd jules_reverse_engineering

# No installation needed! Uses only Python standard library
# Optional: Install enhanced features
pip install -r requirements.txt
```

### Verify Installation

```bash
# Run the demo (no server needed)
python demo_fuzzer.py

# Run unit tests (no server needed)
python test_harness_local.py
```

Expected output: All tests should pass ✓

---

## Local Testing Without Server

The framework includes comprehensive mocking for testing without a live server.

### 1. Run Unit Tests

```bash
python test_harness_local.py
```

**What this tests:**
- VarInt encoding/decoding
- Packet parsing logic
- IDOR detection logic
- Mutation generation
- Security finding reports

**Expected output:**
```
======================================================================
HYTALE FUZZER TEST HARNESS
Testing WITHOUT live server
======================================================================

test_finding_creation ... ok
test_finding_report_generation ... ok
test_idor_without_server ... ok
test_mutation_generation ... ok
test_nan_infinity_packet_building ... ok
test_packet_building ... ok
test_packet_definition_loading ... ok
test_varint_incomplete ... ok
test_varint_negative_length ... ok
test_varint_overflow_detection ... ok
test_varint_valid_encoding ... ok

----------------------------------------------------------------------
Ran 11 tests in 0.123s

OK
======================================================================
✓ All tests passed!
======================================================================
```

### 2. Test Integrated Fuzzer

```bash
python fuzzer_integration.py --packet 0x01 --mutations 5
```

**What this does:**
- Generates 5 mutated versions of movement packet
- Shows what fields are mutated and how
- Verifies packet building logic

**Expected output:**
```
=== Fuzzing packet 0x01 ===

Mutation #1:
  Field: playerID
  Type: overflow
  Original: deadbeef000000000000000000000001
  Mutated: 00000000000000000000000000000000
  Packet (hex): 01000000000000000000000000000000...

Mutation #2:
  Field: tick
  Type: overflow
  Original: 1000
  Mutated: 4294967296
  ...
```

### 3. Test State Tracer

```bash
python state_tracer.py --test auth_during_game --export test_results.json
```

**What this does:**
- Simulates invalid state transitions
- Detects potential race conditions
- Exports results to JSON

**Expected output:**
```
=== Tracing normal game flow ===
✓ Valid transition: INIT → AUTH_PENDING via AUTH_REQUEST
✓ Valid transition: AUTH_PENDING → AUTH_COMPLETE via AUTH_RESPONSE
...

=== Testing invalid transition ===
⚠️ INVALID TRANSITION: END → DAY via INVALID_PACKET

=== Running test scenario: auth_during_game ===
{
  "scenario": "auth_during_game",
  "description": "Attempted re-authentication while in-game",
  "expected": "Should be rejected",
  "result": "VULNERABILITY"
}
```

---

## Testing Against Local Server

Once you have a local Hytale server running, you can test with live traffic.

### 1. Start Local Hytale Server

```bash
cd /path/to/hytale/server
java -Xmx4G -jar HytaleServer.jar --assets ../Assets.zip --disable-sentry
```

**Verify server is running:**
```bash
# Check if port 5520 is listening
netstat -an | grep 5520
# Should show: udp 0.0.0.0:5520
```

### 2. Run Basic Fuzzer Tests

```bash
python hytale_bounty_fuzzer.py --host localhost --port 5520
```

**What this does:**
- Runs all 9 test suites against your local server
- Logs results to `bug_bounty_findings.log`
- Generates report in `bug_bounty_report.txt`

**Expected behavior:**
- Tests run sequentially
- Each test logs results (pass/fail)
- Any vulnerabilities found are logged as CRITICAL

### 3. Run Specific Tests

**Test IDOR:**
```bash
python hytale_bounty_fuzzer.py --host localhost --port 5520 --test test_idor_player_impersonation
```

**Test VarInt Overflow:**
```bash
python hytale_bounty_fuzzer.py --host localhost --port 5520 --test test_varint_overflow
```

**Test NaN Injection:**
```bash
python hytale_bounty_fuzzer.py --host localhost --port 5520 --test test_nan_infinity_floats
```

### 4. Capture Live Traffic

```bash
# Capture packets for 30 seconds
sudo python packet_capture.py --capture --duration 30 --save captured_packets.json

# Analyze captured packets
python packet_capture.py --load captured_packets.json --analyze
```

**What this does:**
- Captures real QUIC/UDP traffic on port 5520
- Extracts authentication flows
- Saves for replay testing

**Note:** May require sudo/admin privileges for packet capture.

### 5. Replay Captured Traffic

```bash
# Replay previously captured packets
python packet_capture.py --replay captured_packets.json --host localhost --port 5520
```

**Use case:** Test how server handles replayed auth packets (replay attack).

### 6. Test Rate Limits

**Movement rate limit:**
```bash
python timing_fuzzer.py --host localhost --port 5520 --test movement_rate --rate 50
```

**Chat rate limit:**
```bash
python timing_fuzzer.py --host localhost --port 5520 --test chat_rate --rate 10
```

**Entity flooding:**
```bash
python timing_fuzzer.py --host localhost --port 5520 --test entity_flood --max 1000
```

---

## Testing Against Remote Server

**⚠️ WARNING: Only test against servers you own or have explicit permission to test!**

### GCP/Cloud Server Setup

```bash
# 1. SSH into your GCP instance
gcloud compute ssh your-instance-name

# 2. Start Hytale server
cd /home/hytale/server
./start_server.sh

# 3. Get server IP
curl ifconfig.me
```

### Run Tests from Local Machine

```bash
# Replace <SERVER_IP> with your actual IP
python hytale_bounty_fuzzer.py --host <SERVER_IP> --port 5520
```

### Monitor Server During Tests

**On the server (via SSH):**
```bash
# Monitor CPU/Memory
top

# Monitor network
netstat -an | grep 5520

# Watch server logs
tail -f server.log
```

---

## Understanding Test Output

### Success Case (No Vulnerabilities)

```
[INFO] Testing IDOR - Player ID Forgery
[INFO] ✓ IDOR test passed - Server properly validates player IDs

[INFO] Testing VarInt Overflow
[INFO] ✓ VarInt overflow test passed - Server handles malformed varints

Total tests run: 9
Vulnerabilities found: 0
Tests passed: 9
```

### Vulnerability Found

```
[CRITICAL] [!!!] VULNERABILITY FOUND: Player ID Forgery - IDOR Vulnerability
[INFO] Severity: CRITICAL | Type: Insecure Direct Object Reference

================================================================================
HYTALE SECURITY REPORT: Player ID Forgery - IDOR Vulnerability
================================================================================

Vulnerability Type: Insecure Direct Object Reference
Severity: CRITICAL

DESCRIPTION
-----------
Server accepts movement packets with arbitrary player UUIDs without validation.
Tested UUID: deadbeef000000000000000000000001

REPRODUCTION STEPS
------------------
1. Craft a 0x01 MOVEMENT packet
2. Set playerID field to arbitrary UUID: deadbeef000000000000000000000001
3. Send packet to game server on port 5520
4. Observe server accepts packet and processes movement
5. Player can control/impersonate other players

IMPACT
------
Complete account takeover. Attacker can impersonate any player, control their
character, access their inventory, and perform actions on their behalf.

RECOMMENDED MITIGATION
---------------------
Validate playerID against the authenticated session token. Maintain a
session→UUID mapping and reject packets where playerID doesn't match the
connection's authenticated identity.

================================================================================
```

### State Machine Test Output

```
=== Statistics ===
Total transitions: 8
Invalid transitions: 1
Current phase: END

=== Detected Race Conditions ===
Type: INVALID_TRANSITION_ACCEPTED | Severity: HIGH
  Server accepted invalid transition from END to DAY

Type: RAPID_TRANSITION | Severity: MEDIUM
  Rapid transitions within 12.34ms - potential race window
```

### Timing Fuzzer Output

```
=== Test Results ===
{
  "test": "entity_spawn_flooding",
  "total_entities_spawned": 573,
  "threshold_entities": 500,
  "crash_detected": false,
  "max_response_time": 2.134,
  "avg_response_time": 0.045,
  "severity": "HIGH",
  "bounty_potential": "$3,000-$8,000",
  "description": "Server performance degrades at 500 entities"
}

🎯 POTENTIAL VULNERABILITY FOUND!
Severity: HIGH
Bounty Potential: $3,000-$8,000
```

---

## Troubleshooting

### Problem: "Connection refused" or "No response"

**Cause:** Server not running or firewall blocking.

**Solution:**
```bash
# Check if server is running
ps aux | grep HytaleServer

# Check if port is listening
netstat -an | grep 5520

# Check firewall (Linux)
sudo ufw status
sudo ufw allow 5520/udp

# Check firewall (Windows)
netsh advfirewall firewall add rule name="Hytale" dir=in action=allow protocol=UDP localport=5520
```

### Problem: "Permission denied" for packet capture

**Cause:** Packet capture requires elevated privileges.

**Solution:**
```bash
# Linux/Mac
sudo python packet_capture.py --capture

# Or use capabilities (Linux)
sudo setcap cap_net_raw=eip $(which python3)
python packet_capture.py --capture
```

### Problem: Import errors

**Cause:** Missing module dependencies.

**Solution:**
```bash
# Ensure you're in the correct directory
cd jules_reverse_engineering

# Check Python path
python -c "import sys; print(sys.path)"

# Run with explicit path
PYTHONPATH=. python hytale_bounty_fuzzer.py
```

### Problem: Server crashes during tests

**Cause:** You may have found a DoS vulnerability! 🎯

**Solution:**
1. Note the exact test that caused the crash
2. Capture server logs before crash
3. Document reproduction steps
4. Prepare bug bounty report
5. Restart server: `java -Xmx4G -jar HytaleServer.jar`

### Problem: Tests are too slow

**Cause:** Network latency or server processing time.

**Solution:**
```bash
# Reduce test iterations
python hytale_bounty_fuzzer.py --test test_idor_player_impersonation

# Or run unit tests instead (instant)
python test_harness_local.py
```

### Problem: False positives

**Cause:** Server might be in wrong state or not fully initialized.

**Solution:**
```bash
# Wait for server to fully start
sleep 30

# Run test again
python hytale_bounty_fuzzer.py --test <test_name>

# Compare with unit test results
python test_harness_local.py
```

---

## Testing Best Practices

### 1. Start with Mock Tests

Always run `test_harness_local.py` first to verify your setup.

### 2. Test Locally Before Remote

Test against `localhost` before testing remote servers.

### 3. Document Everything

Save all output:
```bash
python hytale_bounty_fuzzer.py 2>&1 | tee test_run_$(date +%Y%m%d_%H%M%S).log
```

### 4. One Test at a Time

When debugging, run specific tests:
```bash
python hytale_bounty_fuzzer.py --test test_idor_player_impersonation
```

### 5. Monitor Server Health

Watch server logs and metrics during tests.

### 6. Backup Before Testing

```bash
# Backup server state
cp -r /path/to/server /path/to/server.backup

# If something breaks
rm -rf /path/to/server
mv /path/to/server.backup /path/to/server
```

---

## Expected Test Results by Server State

### Server Not Running
- All network tests fail with "Connection refused"
- Unit tests pass (don't need server)
- Expected behavior

### Server Running, No Players
- Basic packet tests work
- Auth tests may fail (no valid session)
- Rate limit tests work
- Expected behavior

### Server Running, Player Connected
- All tests should work
- Best environment for complete testing
- Most realistic scenario

### Server Under Load
- Response times increase
- Some tests may timeout
- Good for DoS testing
- Monitor carefully

---

## Next Steps After Testing

### If No Vulnerabilities Found
1. Try different test parameters
2. Test edge cases manually
3. Review server logs for anomalies
4. Try timing-based attacks
5. Analyze state transitions

### If Vulnerabilities Found
1. Verify reproducibility
2. Document exact steps
3. Capture packet traces
4. Prepare professional report using `BUG_BOUNTY_REPORT_TEMPLATE.md`
5. Submit through official channels

---

## Support & Resources

- **Framework Documentation:** `README_SECURITY.md`
- **Strategy Guide:** `SECURITY_RESEARCH_STRATEGY.md`
- **Report Template:** `BUG_BOUNTY_REPORT_TEMPLATE.md`
- **Protocol Spec:** `PROTOCOL_SPEC.md`

For issues or questions, refer to the main documentation or create a GitHub issue.

---

**Happy (Ethical) Testing! 🔒🎯**
