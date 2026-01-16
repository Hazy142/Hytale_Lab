# Hytale Security Report Template

**Use this template when submitting vulnerabilities to Hytale's bug bounty program**

---

## Vulnerability Information

**Report ID:** [Auto-generated or leave blank]  
**Submitted By:** [Your name/handle]  
**Date:** [YYYY-MM-DD]  
**Hytale Version:** [e.g., Alpha 1.0.0]  

---

## Vulnerability Type

Select the applicable category:

- [ ] Authentication Bypass
- [ ] Authorization Issue (IDOR)
- [ ] Server-Side Request Forgery (SSRF)
- [ ] Information Disclosure
- [ ] Denial of Service (DoS)
- [ ] Remote Code Execution (RCE)
- [ ] Memory Corruption
- [ ] Logic Bug / State Corruption
- [ ] Other: _____________

---

## Severity Assessment

**Severity Level:** [CRITICAL / HIGH / MEDIUM / LOW]

**CVSS Score (Optional):** [If calculated]

**Justification:**
[Explain why you assigned this severity level based on impact and exploitability]

---

## Summary

[One-paragraph summary of the vulnerability - what is it and why does it matter?]

---

## Affected Components

**Packet Type(s):** [e.g., 0x01 MOVEMENT, 0x03 CHAT]  
**Server Component:** [e.g., NetworkManager, AuthenticationHandler]  
**Endpoint(s):** [e.g., login.hytale.com, game server port 5520]  
**Code Location (if known):** [e.g., com.hypixel.hytale.server.network.PacketHandler]

---

## Technical Details

### Root Cause

[Detailed technical explanation of what causes the vulnerability]

### Vulnerability Mechanism

[Explain how the vulnerability can be exploited - the attack vector]

### Prerequisites

[What conditions must be met to exploit this? e.g., "Requires unauthenticated access", "Requires valid player account", etc.]

---

## Reproduction Steps

Provide step-by-step instructions that allow the security team to reproduce the issue:

1. [First step - be specific]
2. [Second step]
3. [Continue with detailed steps]
4. [Expected result]
5. [Actual result - the vulnerability]

**Environment:**
- OS: [e.g., Windows 11, Ubuntu 22.04]
- Java Version: [e.g., Java 25]
- Network: [e.g., Local server, Production server]

---

## Proof of Concept

### Code

```python
# Paste your exploit code here
# Make it runnable and well-commented

from hytale_protocol_decoder import VarInt
import socket

def exploit():
    # Your exploit implementation
    pass
```

### Packet Data (if applicable)

**Malicious Packet (Hex):**
```
01 a1b2c3d4e5f6a7b8 c9d0e1f2a3b4c5d6
[Continue with hex dump]
```

**Decoded Packet:**
```json
{
  "packet_id": "0x01",
  "type": "MOVEMENT",
  "player_id": "a1b2c3d4-e5f6-a7b8-c9d0-e1f2a3b4c5d6",
  "malicious_field": "value"
}
```

### Screenshots / Video

[If applicable, include links to screenshots or video demonstrations]

---

## Impact Analysis

### Security Impact

[Describe the security implications]

Examples:
- Account takeover
- Unauthorized data access
- Server crash affecting all players
- Ability to execute arbitrary commands
- Resource exhaustion

### Business Impact

[Describe the impact on the game/business]

Examples:
- Players can grief others without detection
- Game economy can be manipulated
- Server infrastructure at risk
- Player trust and retention affected

### Attack Scenarios

**Scenario 1: [Title]**
[Describe a realistic attack scenario]

**Scenario 2: [Title]**
[Describe another scenario if applicable]

---

## Recommended Remediation

### Immediate Mitigation

[What can be done right now to reduce risk? e.g., "Disable feature X", "Add rate limiting"]

### Long-term Fix

[Proper fix that addresses the root cause]

### Code Example (Optional)

```java
// Before (vulnerable)
public void handleMovement(MovementPacket packet) {
    UUID playerId = packet.getPlayerId();
    // Process movement without validation
    movePlayer(playerId, packet.getPosition());
}

// After (fixed)
public void handleMovement(MovementPacket packet) {
    UUID playerId = packet.getPlayerId();
    UUID authenticatedId = session.getAuthenticatedPlayerId();
    
    // Validate player ID matches session
    if (!playerId.equals(authenticatedId)) {
        throw new SecurityException("Player ID mismatch");
    }
    
    movePlayer(playerId, packet.getPosition());
}
```

---

## References

### Related CVEs or Security Advisories

[If this is similar to known vulnerabilities in other games/systems]

### Technical Documentation

- [Link to relevant protocol documentation]
- [Link to API documentation]

### Research Papers / Articles

[If you referenced any security research]

---

## Additional Information

### Discovery Method

[How did you find this vulnerability? Fuzzing, code review, accident?]

### Exploitation Difficulty

[Easy / Medium / Hard - and why]

### Public Disclosure Timeline

[Your intended disclosure timeline if the issue isn't fixed]

---

## Appendix

### Full Packet Capture

[If you have PCAP files or full packet logs, reference them here]

### Fuzzer Output

[If you used automated tools, include relevant output]

### Test Environment Details

[Any additional information about your testing setup]

---

## Submission Checklist

Before submitting, ensure:

- [ ] Vulnerability is reproducible
- [ ] Steps are clear and detailed
- [ ] Proof of concept code is included
- [ ] Impact is accurately described
- [ ] Remediation suggestions are provided
- [ ] No sensitive data (passwords, tokens) is included
- [ ] You have tested against the latest version
- [ ] You have not publicly disclosed this vulnerability

---

**Reporter Contact Information**

**Email:** [Your email for communication]  
**Bug Bounty Platform:** [e.g., HackerOne handle]  
**Preferred Contact Method:** [Email / Platform]

---

**Signature / Verification**

[If required by the program, sign your report with PGP or provide verification]

---

## For Hytale Security Team Use Only

**Triage Date:** ___________  
**Assigned To:** ___________  
**Status:** [ ] New / [ ] Confirmed / [ ] Fixed / [ ] Won't Fix  
**Bounty Amount:** $___________  
**Payment Date:** ___________  

**Internal Notes:**
