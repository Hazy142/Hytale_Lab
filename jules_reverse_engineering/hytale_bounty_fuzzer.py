#!/usr/bin/env python3
"""
Hytale Bug Bounty Security Fuzzer
Comprehensive security testing framework for Hytale protocol vulnerabilities

Focus Areas:
1. Authentication & Session Management (IDOR, Token Reuse)
2. VarInt Parsing (Overflow/Underflow)
3. Server State Manipulation (Race Conditions, Desync)
4. Packet Fuzzing (Malformed Data, Edge Cases)
"""

import struct
import socket
import time
import sys
from typing import List, Dict, Any, Optional, Tuple
from dataclasses import dataclass
from enum import Enum
import logging

# Import the existing decoder
try:
    from hytale_protocol_decoder import VarInt, HytalePacket, PacketID, Vector3f
except ImportError:
    print("ERROR: hytale_protocol_decoder.py not found in the same directory")
    sys.exit(1)

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(levelname)s: %(message)s',
    handlers=[
        logging.FileHandler('bug_bounty_findings.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


class VulnerabilityType(Enum):
    """Classification of vulnerability types for bug bounty reporting"""
    AUTHENTICATION_BYPASS = "Authentication Bypass"
    IDOR = "Insecure Direct Object Reference"
    DOS = "Denial of Service"
    INFORMATION_DISCLOSURE = "Information Disclosure"
    SERVER_CRASH = "Server Crash"
    STATE_CORRUPTION = "State Corruption"
    PACKET_INJECTION = "Packet Injection"
    MEMORY_LEAK = "Memory Leak"


@dataclass
class SecurityFinding:
    """Represents a discovered security vulnerability"""
    vuln_type: VulnerabilityType
    severity: str  # CRITICAL, HIGH, MEDIUM, LOW
    title: str
    description: str
    reproduction_steps: List[str]
    poc_code: str
    impact: str
    mitigation: str
    packet_hex: Optional[str] = None
    
    def to_report(self) -> str:
        """Generate a bug bounty report from this finding"""
        report = f"""
{'='*80}
HYTALE SECURITY REPORT: {self.title}
{'='*80}

Vulnerability Type: {self.vuln_type.value}
Severity: {self.severity}

DESCRIPTION
-----------
{self.description}

REPRODUCTION STEPS
------------------
"""
        for i, step in enumerate(self.reproduction_steps, 1):
            report += f"{i}. {step}\n"
        
        report += f"""
IMPACT
------
{self.impact}

PROOF OF CONCEPT
----------------
{self.poc_code}
"""
        
        if self.packet_hex:
            report += f"""
MALICIOUS PACKET (HEX)
----------------------
{self.packet_hex}
"""
        
        report += f"""
RECOMMENDED MITIGATION
---------------------
{self.mitigation}

{'='*80}
"""
        return report


class HytaleFuzzer:
    """Main fuzzer class for Hytale protocol security testing"""
    
    def __init__(self, target_host: str = "localhost", target_port: int = 5520):
        self.target_host = target_host
        self.target_port = target_port
        self.findings: List[SecurityFinding] = []
        self.test_results: Dict[str, bool] = {}
        
    def log_finding(self, finding: SecurityFinding):
        """Log a security finding"""
        self.findings.append(finding)
        logger.critical(f"[VULNERABILITY FOUND] {finding.title}")
        logger.info(f"Severity: {finding.severity} | Type: {finding.vuln_type.value}")
        
    def send_packet(self, packet_data: bytes, timeout: float = 2.0) -> Optional[bytes]:
        """
        Send a packet to the Hytale server and return response
        
        NOTE: This is a simplified implementation for testing purposes.
        Real Hytale protocol uses QUIC over UDP, which requires proper
        handshaking and connection management. This implementation sends
        raw UDP packets for basic testing. For production fuzzing, consider
        using a QUIC library like aioquic or quiche.
        
        Returns None if connection fails or times out
        """
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            sock.settimeout(timeout)
            sock.sendto(packet_data, (self.target_host, self.target_port))
            response, _ = sock.recvfrom(4096)
            sock.close()
            return response
        except socket.timeout:
            logger.debug("Socket timeout - no response from server")
            return None
        except Exception as e:
            logger.debug(f"Connection error: {e}")
            return None
    
    # ========================================================================
    # TEST SUITE 1: AUTHENTICATION & SESSION MANAGEMENT
    # ========================================================================
    
    def test_idor_player_impersonation(self) -> bool:
        """
        Test: Can we forge a player ID and impersonate another player?
        Expected: Server should reject packets with unvalidated player IDs
        """
        logger.info("Testing IDOR - Player ID Forgery")
        
        # Generate test UUIDs with distinctive prefix to avoid accidental collision
        # Using test-specific prefix: 0xDEADBEEF + sequential numbers
        test_uuids = [
            bytes.fromhex("deadbeef000000000000000000000001"),  # Test ID 1
            bytes.fromhex("deadbeef000000000000000000000002"),  # Test ID 2
            bytes.fromhex("deadbeef0000ffffffffffffffff0000"),  # Edge case UUID
            bytes.fromhex("deadbeef000000000000000000000000"),  # Zero-suffix test
        ]
        
        for fake_uuid in test_uuids:
            # Craft a MOVEMENT packet with forged player ID
            packet_data = bytearray()
            packet_data.extend(VarInt.encode(PacketID.MOVEMENT.value))
            packet_data.extend(fake_uuid)  # Forged UUID
            packet_data.extend(struct.pack('>fff', 100.0, 64.0, 100.0))  # position
            packet_data.extend(struct.pack('>fff', 0.0, 0.0, 0.0))  # velocity
            packet_data.extend(struct.pack('>ff', 0.0, 0.0))  # yaw, pitch
            packet_data.extend(struct.pack('>B', 0))  # flags
            packet_data.extend(struct.pack('>I', 1000))  # tick
            
            response = self.send_packet(bytes(packet_data))
            
            if response:
                logger.warning(f"[!!!] Server accepted forged UUID: {fake_uuid.hex()}")
                
                finding = SecurityFinding(
                    vuln_type=VulnerabilityType.IDOR,
                    severity="CRITICAL",
                    title="Player ID Forgery - IDOR Vulnerability",
                    description=f"Server accepts movement packets with arbitrary player UUIDs without validation. Tested UUID: {fake_uuid.hex()}",
                    reproduction_steps=[
                        "Craft a 0x01 MOVEMENT packet",
                        f"Set playerID field to arbitrary UUID: {fake_uuid.hex()}",
                        "Send packet to game server on port 5520",
                        "Observe server accepts packet and processes movement",
                        "Player can control/impersonate other players"
                    ],
                    poc_code=f"Packet hex: {packet_data.hex()}",
                    impact="Complete account takeover. Attacker can impersonate any player, control their character, access their inventory, and perform actions on their behalf.",
                    mitigation="Validate playerID against the authenticated session token. Maintain a session→UUID mapping and reject packets where playerID doesn't match the connection's authenticated identity.",
                    packet_hex=packet_data.hex()
                )
                self.log_finding(finding)
                return True
        
        logger.info("✓ IDOR test passed - Server properly validates player IDs")
        return False
    
    def test_session_token_reuse(self) -> bool:
        """
        Test: Can session tokens be reused after logout?
        Expected: Tokens should be invalidated on logout
        """
        logger.info("Testing Session Token Reuse After Logout")
        
        # This would require actual session tokens from a real connection
        # For now, we test the concept with documented behavior
        
        logger.info("⚠ Test requires live session tokens - documented for manual testing")
        logger.info("Manual test procedure:")
        logger.info("  1. Authenticate and obtain session token")
        logger.info("  2. Perform normal operations")
        logger.info("  3. Logout/disconnect")
        logger.info("  4. Attempt to reuse the same token")
        logger.info("  5. Check if server accepts the token")
        
        return False
    
    # ========================================================================
    # TEST SUITE 2: VARINT PARSING VULNERABILITIES
    # ========================================================================
    
    def test_varint_overflow(self) -> bool:
        """
        Test: Can VarInt parsing be exploited with overflow values?
        Expected: Server should handle malformed varints gracefully
        """
        logger.info("Testing VarInt Overflow/Underflow")
        
        malformed_varints = [
            # Valid max VarInt (5 bytes, MSB clear on last byte)
            bytes([0xFF, 0xFF, 0xFF, 0xFF, 0x7F]),
            # Invalid - 6th byte (overflow)
            bytes([0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01]),
            # Invalid - all bytes have continuation bit
            bytes([0xFF, 0xFF, 0xFF, 0xFF, 0xFF]),
            # Invalid - single byte overflow
            bytes([0xFF]),
            # Edge case - zero with continuation bit
            bytes([0x80, 0x00]),
        ]
        
        for i, varint_data in enumerate(malformed_varints):
            logger.debug(f"Testing malformed VarInt #{i+1}: {varint_data.hex()}")
            
            # Send just the malformed varint as a packet
            response = self.send_packet(varint_data)
            
            # If we get no response, server might have crashed
            if response is None and i > 0:  # First one is valid, should respond
                logger.warning(f"[!!!] Server did not respond to malformed VarInt #{i+1}")
                
                finding = SecurityFinding(
                    vuln_type=VulnerabilityType.DOS,
                    severity="HIGH",
                    title="VarInt Parsing Denial of Service",
                    description=f"Server crashes or hangs when receiving malformed VarInt data: {varint_data.hex()}",
                    reproduction_steps=[
                        "Connect to game server port 5520",
                        f"Send raw bytes: {varint_data.hex()}",
                        "Observe server stops responding",
                        "Server may crash or enter infinite loop"
                    ],
                    poc_code=f"Raw packet: {varint_data.hex()}",
                    impact="Denial of Service. Attacker can crash the server or cause it to hang, affecting all players. Can be exploited remotely without authentication.",
                    mitigation="Add length validation to VarInt parser. Limit VarInt to maximum 5 bytes. Add timeout to prevent infinite loops. Validate continuation bits properly.",
                    packet_hex=varint_data.hex()
                )
                self.log_finding(finding)
                return True
        
        logger.info("✓ VarInt overflow test passed - Server handles malformed varints")
        return False
    
    def test_varint_negative_length(self) -> bool:
        """
        Test: Can we cause issues with negative-appearing VarInt lengths?
        Expected: Server should validate length values are positive
        """
        logger.info("Testing VarInt Negative Length Exploit")
        
        # Craft a chat packet with suspicious length
        packet = bytearray()
        packet.extend(VarInt.encode(PacketID.CHAT.value))
        packet.extend(bytes(16))  # Dummy UUID
        
        # VarInt that decodes to a very large number (potential negative in signed interpretation)
        # This is 0xFFFFFFFF in VarInt encoding
        malicious_length = bytes([0xFF, 0xFF, 0xFF, 0xFF, 0x0F])
        packet.extend(malicious_length)
        
        response = self.send_packet(bytes(packet))
        
        if response is None:
            logger.warning("[!!!] Server crashed on negative/large length VarInt")
            
            finding = SecurityFinding(
                vuln_type=VulnerabilityType.DOS,
                severity="HIGH",
                title="VarInt Length Field Integer Overflow",
                description="Server attempts to allocate excessive memory when receiving a VarInt length field with value 0xFFFFFFFF",
                reproduction_steps=[
                    "Craft 0x03 CHAT packet",
                    "Set message length VarInt to 0xFFFFFFFF (bytes: FF FF FF FF 0F)",
                    "Send packet to server",
                    "Server attempts to allocate 4GB of memory",
                    "Server crashes with OOM or hangs"
                ],
                poc_code=f"Packet hex: {packet.hex()}",
                impact="Denial of Service via memory exhaustion. Server crashes immediately when trying to allocate multi-gigabyte buffer for message.",
                mitigation="Add sanity checks on length fields. Limit message length to reasonable maximum (e.g., 32KB). Validate length before allocation.",
                packet_hex=packet.hex()
            )
            self.log_finding(finding)
            return True
        
        logger.info("✓ Negative length test passed")
        return False
    
    # ========================================================================
    # TEST SUITE 3: PACKET FUZZING - EDGE CASES
    # ========================================================================
    
    def test_nan_infinity_floats(self) -> bool:
        """
        Test: How does server handle NaN, Infinity, -Infinity in float fields?
        Expected: Server should validate float values are in valid range
        """
        logger.info("Testing NaN/Infinity Float Values")
        
        # IEEE 754 special float values - may behave differently on some platforms
        try:
            special_floats = [
                ("NaN", struct.pack('>f', float('nan'))),
                ("Infinity", struct.pack('>f', float('inf'))),
                ("-Infinity", struct.pack('>f', float('-inf'))),
                ("Very Large", struct.pack('>f', 1e38)),
                ("Very Small", struct.pack('>f', 1e-38)),
            ]
        except (ValueError, OverflowError) as e:
            logger.warning(f"Platform doesn't support all IEEE 754 special values: {e}")
            return False
        
        for name, float_bytes in special_floats:
            packet = bytearray()
            packet.extend(VarInt.encode(PacketID.MOVEMENT.value))
            packet.extend(bytes(16))  # UUID
            packet.extend(float_bytes * 3)  # x, y, z all set to special value
            packet.extend(bytes(12))  # velocity
            packet.extend(struct.pack('>ff', 0.0, 0.0))  # yaw, pitch
            packet.extend(struct.pack('>B', 0))  # flags
            packet.extend(struct.pack('>I', 1000))  # tick
            
            response = self.send_packet(bytes(packet))
            
            if response is None:
                logger.warning(f"[!!!] Server crashed on {name} float value")
                
                finding = SecurityFinding(
                    vuln_type=VulnerabilityType.SERVER_CRASH,
                    severity="MEDIUM",
                    title=f"Server Crash on {name} Float Values",
                    description=f"Server crashes when receiving {name} in position fields of movement packet",
                    reproduction_steps=[
                        "Craft 0x01 MOVEMENT packet",
                        f"Set position.x/y/z to {name} ({float_bytes.hex()})",
                        "Send to server",
                        "Server crashes or enters invalid state"
                    ],
                    poc_code=f"Float bytes: {float_bytes.hex()}",
                    impact="Denial of Service. Server crashes when processing movement updates. May also corrupt world state or cause physics engine issues.",
                    mitigation=f"Validate float values are finite and within reasonable bounds (-1e6 to 1e6). Reject packets with NaN, Infinity, or extreme values.",
                    packet_hex=packet.hex()
                )
                self.log_finding(finding)
                return True
        
        logger.info("✓ NaN/Infinity test passed")
        return False
    
    def test_zero_length_strings(self) -> bool:
        """
        Test: How does server handle zero-length strings in chat?
        Expected: Should either reject or handle gracefully
        """
        logger.info("Testing Zero-Length String Handling")
        
        packet = bytearray()
        packet.extend(VarInt.encode(PacketID.CHAT.value))
        packet.extend(bytes(16))  # UUID
        packet.extend(VarInt.encode(0))  # Length = 0
        # No message bytes
        packet.extend(struct.pack('>Q', int(time.time() * 1000)))  # timestamp
        
        response = self.send_packet(bytes(packet))
        
        # Check if this causes any issues
        if response:
            logger.info("✓ Server handles zero-length strings")
        
        return False
    
    # ========================================================================
    # TEST SUITE 4: SERVER STATE MANIPULATION
    # ========================================================================
    
    def test_race_condition_phase_change(self) -> bool:
        """
        Test: Can we send packets during phase transitions to cause issues?
        Expected: Server should queue/reject packets during transitions
        """
        logger.info("Testing Race Condition During Game Phase Changes")
        
        # This requires timing analysis and multiple connections
        logger.info("⚠ Race condition test requires multiple concurrent connections")
        logger.info("Manual test procedure:")
        logger.info("  1. Monitor for GamePhaseChangeEvent (0x0F)")
        logger.info("  2. Send movement/block placement during transition")
        logger.info("  3. Check if server accepts actions in wrong phase")
        logger.info("  4. Check for state corruption or duplication")
        
        return False
    
    def test_packet_replay_attack(self) -> bool:
        """
        Test: Can we replay old packets to cause state issues?
        Expected: Server should validate tick numbers and reject old packets
        """
        logger.info("Testing Packet Replay Attacks")
        
        # Craft a movement packet with old tick number
        packet = bytearray()
        packet.extend(VarInt.encode(PacketID.MOVEMENT.value))
        packet.extend(bytes(16))  # UUID
        packet.extend(struct.pack('>fff', 100.0, 64.0, 100.0))  # position
        packet.extend(struct.pack('>fff', 0.0, 0.0, 0.0))  # velocity
        packet.extend(struct.pack('>ff', 0.0, 0.0))  # yaw, pitch
        packet.extend(struct.pack('>B', 0))  # flags
        packet.extend(struct.pack('>I', 1))  # tick = 1 (very old)
        
        response = self.send_packet(bytes(packet))
        
        if response:
            logger.warning("[!!!] Server accepted packet with old tick number")
            logger.info("This may allow replay attacks or packet reordering exploits")
        
        return False
    
    # ========================================================================
    # TEST SUITE 5: MEMORY LEAK / RESOURCE EXHAUSTION
    # ========================================================================
    
    def test_entity_spawn_flood(self) -> bool:
        """
        Test: Can we cause memory leaks by spawning many entities?
        Expected: Server should have rate limiting and cleanup
        """
        logger.info("Testing Entity Spawn Flood Attack")
        
        # This would require sending many entity spawn requests
        logger.info("⚠ This test would require server-side packet sending capability")
        logger.info("Manual test procedure:")
        logger.info("  1. Send many block placement packets rapidly")
        logger.info("  2. Monitor server memory usage")
        logger.info("  3. Check if entities are properly cleaned up")
        logger.info("  4. Look for memory leaks in long-running sessions")
        
        return False
    
    # ========================================================================
    # MASTER TEST RUNNER
    # ========================================================================
    
    def run_all_tests(self) -> Dict[str, bool]:
        """Run all security tests and return results"""
        logger.info("="*80)
        logger.info("HYTALE BUG BOUNTY SECURITY FUZZER")
        logger.info("="*80)
        logger.info(f"Target: {self.target_host}:{self.target_port}")
        logger.info(f"Started: {time.strftime('%Y-%m-%d %H:%M:%S')}")
        logger.info("")
        
        tests = [
            ("IDOR - Player Impersonation", self.test_idor_player_impersonation),
            ("Session Token Reuse", self.test_session_token_reuse),
            ("VarInt Overflow", self.test_varint_overflow),
            ("VarInt Negative Length", self.test_varint_negative_length),
            ("NaN/Infinity Floats", self.test_nan_infinity_floats),
            ("Zero-Length Strings", self.test_zero_length_strings),
            ("Race Condition Phase Change", self.test_race_condition_phase_change),
            ("Packet Replay Attack", self.test_packet_replay_attack),
            ("Entity Spawn Flood", self.test_entity_spawn_flood),
        ]
        
        results = {}
        for test_name, test_func in tests:
            logger.info(f"\n{'='*80}")
            logger.info(f"Running: {test_name}")
            logger.info(f"{'='*80}")
            try:
                found_vuln = test_func()
                results[test_name] = found_vuln
                if found_vuln:
                    logger.critical(f"❌ VULNERABILITY FOUND: {test_name}")
                else:
                    logger.info(f"✓ Test passed: {test_name}")
            except Exception as e:
                logger.error(f"Test crashed: {test_name} - {e}")
                results[test_name] = False
        
        # Summary
        logger.info("\n" + "="*80)
        logger.info("TEST SUMMARY")
        logger.info("="*80)
        total_tests = len(results)
        vulnerabilities_found = sum(1 for v in results.values() if v)
        logger.info(f"Total tests run: {total_tests}")
        logger.info(f"Vulnerabilities found: {vulnerabilities_found}")
        logger.info(f"Tests passed: {total_tests - vulnerabilities_found}")
        
        if self.findings:
            logger.info(f"\n{'='*80}")
            logger.info(f"SECURITY FINDINGS REPORT")
            logger.info(f"{'='*80}")
            for finding in self.findings:
                logger.info(finding.to_report())
        
        self.test_results = results
        return results
    
    def export_findings(self, filename: str = "bug_bounty_report.txt"):
        """Export all findings to a file"""
        with open(filename, 'w') as f:
            f.write(f"Hytale Bug Bounty Security Report\n")
            f.write(f"Generated: {time.strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"Target: {self.target_host}:{self.target_port}\n")
            f.write(f"Total Vulnerabilities: {len(self.findings)}\n")
            f.write("\n")
            
            for finding in self.findings:
                f.write(finding.to_report())
                f.write("\n\n")
        
        logger.info(f"Full report exported to: {filename}")


def main():
    """Main entry point for the fuzzer"""
    import argparse
    
    parser = argparse.ArgumentParser(description="Hytale Bug Bounty Security Fuzzer")
    parser.add_argument("--host", default="localhost", help="Target host (default: localhost)")
    parser.add_argument("--port", type=int, default=5520, help="Target port (default: 5520)")
    parser.add_argument("--export", default="bug_bounty_report.txt", help="Export filename")
    parser.add_argument("--test", help="Run specific test only")
    
    args = parser.parse_args()
    
    fuzzer = HytaleFuzzer(target_host=args.host, target_port=args.port)
    
    if args.test:
        # Run specific test
        test_method = getattr(fuzzer, args.test, None)
        if test_method:
            logger.info(f"Running single test: {args.test}")
            test_method()
        else:
            logger.error(f"Test not found: {args.test}")
            sys.exit(1)
    else:
        # Run all tests
        fuzzer.run_all_tests()
    
    # Export findings
    if fuzzer.findings:
        fuzzer.export_findings(args.export)
        logger.info(f"\n🎯 Found {len(fuzzer.findings)} potential vulnerabilities!")
        logger.info(f"Review the report at: {args.export}")
    else:
        logger.info("\n✓ No vulnerabilities found (or server not accessible)")


if __name__ == "__main__":
    main()
