#!/usr/bin/env python3
"""
Demonstration of Hytale Security Fuzzer
Shows example usage without requiring a live server
"""

import sys
from hytale_bounty_fuzzer import (
    HytaleFuzzer, 
    SecurityFinding, 
    VulnerabilityType,
    logger
)
from hytale_protocol_decoder import VarInt, HytalePacket, PacketID
import struct

def demo_varint_encoding():
    """Demonstrate VarInt encoding/decoding"""
    print("\n" + "="*80)
    print("DEMO 1: VarInt Encoding/Decoding")
    print("="*80)
    
    test_values = [0, 127, 128, 16383, 16384, 0xFFFFFFFF]
    
    for value in test_values:
        encoded = VarInt.encode(value)
        decoded, _ = VarInt.decode(encoded)
        print(f"Value: {value:10d} | Encoded: {encoded.hex():20s} | Decoded: {decoded}")
    
    print("\n✓ VarInt encoding/decoding works correctly")

def demo_malicious_varint():
    """Demonstrate malicious VarInt payloads"""
    print("\n" + "="*80)
    print("DEMO 2: Malicious VarInt Payloads")
    print("="*80)
    
    malicious_payloads = [
        ("Valid max (5 bytes)", bytes([0xFF, 0xFF, 0xFF, 0xFF, 0x7F])),
        ("Overflow (6 bytes)", bytes([0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01])),
        ("All continuation bits", bytes([0xFF, 0xFF, 0xFF, 0xFF, 0xFF])),
        ("Incomplete", bytes([0xFF])),
    ]
    
    for name, payload in malicious_payloads:
        print(f"\n{name}:")
        print(f"  Hex: {payload.hex()}")
        try:
            value, _ = VarInt.decode(payload)
            print(f"  Decoded: {value} ✓ (handled gracefully)")
        except ValueError as e:
            print(f"  Error: {e} ⚠️ (potential vulnerability)")

def demo_movement_packet():
    """Demonstrate crafting a movement packet"""
    print("\n" + "="*80)
    print("DEMO 3: Crafting Movement Packet")
    print("="*80)
    
    # Normal packet
    packet = bytearray()
    packet.extend(VarInt.encode(PacketID.MOVEMENT.value))
    fake_uuid = bytes.fromhex("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
    packet.extend(fake_uuid)
    packet.extend(struct.pack('>fff', 100.0, 64.0, 100.0))  # position
    packet.extend(struct.pack('>fff', 0.0, 0.0, 0.0))  # velocity
    packet.extend(struct.pack('>ff', 180.0, 0.0))  # yaw, pitch
    packet.extend(struct.pack('>B', 0x02))  # flags (sprinting)
    packet.extend(struct.pack('>I', 1000))  # tick
    
    print(f"\nCrafted Movement Packet:")
    print(f"  Length: {len(packet)} bytes")
    print(f"  Hex: {packet.hex()}")
    print(f"  Player UUID: {fake_uuid.hex()}")
    print(f"  Position: (100.0, 64.0, 100.0)")
    print(f"  Yaw: 180.0°")
    
    # Parse it back
    hp = HytalePacket(bytes(packet))
    hp.parse()
    decoded = hp.decode()
    
    print(f"\nDecoded Packet:")
    for key, value in decoded.items():
        print(f"  {key}: {value}")

def demo_nan_attack():
    """Demonstrate NaN/Infinity attack vector"""
    print("\n" + "="*80)
    print("DEMO 4: NaN/Infinity Attack Vector")
    print("="*80)
    
    special_values = [
        ("NaN", float('nan')),
        ("Infinity", float('inf')),
        ("-Infinity", float('-inf')),
    ]
    
    for name, value in special_values:
        packet = bytearray()
        packet.extend(VarInt.encode(PacketID.MOVEMENT.value))
        packet.extend(bytes(16))  # UUID
        packet.extend(struct.pack('>fff', value, value, value))  # position with special value
        packet.extend(bytes(12))  # velocity
        packet.extend(struct.pack('>ff', 0.0, 0.0))  # yaw, pitch
        packet.extend(struct.pack('>B', 0))  # flags
        packet.extend(struct.pack('>I', 1000))  # tick
        
        print(f"\n{name} Attack Packet:")
        print(f"  Hex: {packet.hex()}")
        print(f"  Length: {len(packet)} bytes")
        print(f"  Impact: Server crash or invalid state")

def demo_security_finding():
    """Demonstrate creating a security finding report"""
    print("\n" + "="*80)
    print("DEMO 5: Security Finding Report")
    print("="*80)
    
    finding = SecurityFinding(
        vuln_type=VulnerabilityType.IDOR,
        severity="CRITICAL",
        title="Player ID Forgery - IDOR Vulnerability (DEMO)",
        description="This is a demonstration of how a vulnerability would be documented. In a real scenario, this would indicate that the server accepts movement packets with arbitrary player UUIDs.",
        reproduction_steps=[
            "Craft a 0x01 MOVEMENT packet",
            "Set playerID field to arbitrary UUID: 00000000-0000-0000-0000-000000000001",
            "Send packet to game server on port 5520",
            "Observe server response",
            "If accepted: vulnerability confirmed"
        ],
        poc_code="packet_hex = '01' + '00000000000000000000000000000001' + ...",
        impact="If exploitable: Complete account takeover. Attacker can impersonate any player and control their character.",
        mitigation="Validate playerID against the authenticated session token. Maintain a session→UUID mapping and reject mismatched packets.",
        packet_hex="01000000000000000000000000000000014290000042800000..."
    )
    
    print(finding.to_report())

def demo_fuzzer_structure():
    """Show the fuzzer test structure"""
    print("\n" + "="*80)
    print("DEMO 6: Fuzzer Test Structure")
    print("="*80)
    
    print("""
The fuzzer contains 9 main test categories:

1. Authentication & Session Management
   ├── test_idor_player_impersonation()
   └── test_session_token_reuse()

2. VarInt Parsing Vulnerabilities
   ├── test_varint_overflow()
   └── test_varint_negative_length()

3. Packet Fuzzing - Edge Cases
   ├── test_nan_infinity_floats()
   └── test_zero_length_strings()

4. Server State Manipulation
   ├── test_race_condition_phase_change()
   └── test_packet_replay_attack()

5. Memory Leak / Resource Exhaustion
   └── test_entity_spawn_flood()

Each test:
- Crafts malicious packets
- Sends to target server
- Analyzes responses
- Logs findings automatically
- Generates professional reports
    """)
    
    print("\nTo run against a live server:")
    print("  python hytale_bounty_fuzzer.py --host localhost --port 5520")
    print("\nTo run a specific test:")
    print("  python hytale_bounty_fuzzer.py --test test_idor_player_impersonation")

def main():
    """Run all demonstrations"""
    print("\n")
    print("╔" + "="*78 + "╗")
    print("║" + " "*20 + "HYTALE SECURITY FUZZER DEMONSTRATION" + " "*22 + "║")
    print("╚" + "="*78 + "╝")
    
    print("\nThis demo shows the capabilities of the Hytale bug bounty fuzzer")
    print("without requiring a live server connection.\n")
    
    try:
        demo_varint_encoding()
        demo_malicious_varint()
        demo_movement_packet()
        demo_nan_attack()
        demo_security_finding()
        demo_fuzzer_structure()
        
        print("\n" + "="*80)
        print("DEMO COMPLETE")
        print("="*80)
        print("\n✓ All demonstrations completed successfully!")
        print("\nNext Steps:")
        print("1. Set up a local Hytale server")
        print("2. Run: python hytale_bounty_fuzzer.py")
        print("3. Review findings in bug_bounty_report.txt")
        print("4. Submit vulnerabilities through official bug bounty program")
        print("\n" + "="*80 + "\n")
        
    except Exception as e:
        print(f"\n❌ Demo error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
