# CI/CD Integration for Security Framework

## GitHub Actions Workflow

The security framework includes automated CI testing via GitHub Actions.

### Workflow: `security-tests.yml`

**Location:** `.github/workflows/security-tests.yml`

**Triggers:**
- Push to `copilot/reverse-engineering-bug-bounty` branch
- Pull requests to `main` branch
- Only when files in `jules_reverse_engineering/` change

### What Gets Tested

#### 1. Core Import Verification
Verifies all modules can be imported without errors:
- `hytale_protocol_decoder`
- `hytale_bounty_fuzzer`
- `fuzzer_integration`
- `state_tracer`
- `timing_fuzzer`
- `packet_capture`

#### 2. Unit Tests
Runs the complete test suite:
```bash
python test_harness_local.py
```
- 15 unit tests covering VarInt, packet parsing, IDOR logic, mutations, etc.
- Must complete in < 1 second
- All tests must pass

#### 3. PACKET_STRUCTURES.json Validation
Verifies the packet definitions file:
- Can be loaded as valid JSON
- Contains at least 5 packet definitions
- Structure is valid

#### 4. Fuzzer Integration Test
Verifies mutation generation works:
```bash
python fuzzer_integration.py --packet 0x01 --mutations 3
```
- Must generate exactly 3 mutations
- Output must indicate successful generation

#### 5. State Tracer Test
Verifies state machine simulation:
```bash
python state_tracer.py --test auth_during_game
```
- Must run without errors
- Must output scenario results

## Local Testing

Run the same tests locally before pushing:

```bash
cd jules_reverse_engineering

# Test 1: Import verification
python -c "
from hytale_protocol_decoder import VarInt, HytalePacket
from hytale_bounty_fuzzer import HytaleFuzzer
from fuzzer_integration import IntegratedFuzzer
from state_tracer import StateTracer
from timing_fuzzer import RateLimitFuzzer
from packet_capture import PacketCapture
print('✓ All core imports working correctly')
"

# Test 2: Unit tests
python test_harness_local.py

# Test 3: Packet structures
python -c "
import json
with open('PACKET_STRUCTURES.json', 'r') as f:
    structures = json.load(f)
print(f'✓ Loaded {len(structures)} packet definitions')
"

# Test 4: Fuzzer integration
python fuzzer_integration.py --packet 0x01 --mutations 3

# Test 5: State tracer
python state_tracer.py --test auth_during_game
```

## CI Status Badge

Add to README.md:

```markdown
![Security Framework Tests](https://github.com/Hazy142/Hytale_Lab/workflows/Security%20Framework%20Tests/badge.svg?branch=copilot/reverse-engineering-bug-bounty)
```

## Benefits

### 1. Early Detection
- Catches import errors immediately
- Detects breaking changes before merge
- Validates framework integrity on every push

### 2. Confidence
- Green checkmarks = ready to use
- Red X = needs fixing before testing against live server
- No surprises when running locally

### 3. Documentation
- Workflow serves as executable documentation
- Shows exactly what needs to work
- Clear success/failure criteria

### 4. Maintenance
- Prevents regressions when adding new features
- Ensures backward compatibility
- Validates module interdependencies

## Workflow Execution Time

Expected runtime: **~30-45 seconds**

Breakdown:
- Checkout & Python setup: ~20s
- Import verification: ~1s
- Unit tests: <1s
- PACKET_STRUCTURES validation: <1s
- Fuzzer integration test: ~2s
- State tracer test: ~2s

## Extending the Workflow

To add more tests, edit `.github/workflows/security-tests.yml`:

```yaml
- name: Your new test
  run: |
    cd jules_reverse_engineering
    python your_test.py
    echo "✓ Your test passed"
```

## Troubleshooting

### Workflow fails on import
- Check module names are consistent (use underscores)
- Verify all files are committed
- Check for typos in import statements

### Unit tests fail
- Run locally first: `python test_harness_local.py`
- Check for platform-specific issues
- Review test output for specific failures

### Fuzzer test fails
- Verify `PACKET_STRUCTURES.json` exists and is valid
- Check mutation count matches (should be 3)
- Run locally to debug

## Future Enhancements

Potential additions to CI:
- Code coverage reporting
- Linting (pylint, black)
- Type checking (mypy)
- Security scanning (bandit)
- Documentation building
- Performance benchmarks

For now, keeping it minimal ensures fast feedback and low maintenance.
