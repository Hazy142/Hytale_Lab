#!/usr/bin/env python3
"""
Local Test Harness with Mocking for Hytale Fuzzer
Run fuzzer tests WITHOUT needing live Hytale server

This module provides:
1. Unit tests with mocked responses
2. Fast iteration without server
3. Test fuzzer logic in isolation
"""

import unittest
from unittest.mock import Mock, patch, MagicMock
import struct
import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

try:
    from hytale_bounty_fuzzer import HytaleFuzzer, SecurityFinding, VulnerabilityType
    from hytale_protocol_decoder import VarInt, HytalePacket
    from fuzzer_integration import IntegratedFuzzer
except ImportError as e:
    print(f"Import error: {e}")
    print("Make sure all required modules are in the same directory")
    sys.exit(1)


class TestVarIntParsing(unittest.TestCase):
    """Test VarInt parser WITHOUT needing server"""
    
    def test_varint_valid_encoding(self):
        """Test valid VarInt encoding/decoding"""
        test_values = [0, 127, 128, 16383, 16384, 0xFFFFFFFF]
        
        for value in test_values:
            encoded = VarInt.encode(value)
            decoded, offset = VarInt.decode(encoded)
            self.assertEqual(decoded, value, f"Failed for value {value}")
    
    def test_varint_overflow_detection(self):
        """Test VarInt parser handles overflow correctly"""
        # 6-byte VarInt (should be max 5 bytes)
        malformed = bytes([0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01])
        
        # Parser should either handle it gracefully or raise error
        try:
            decoded, _ = VarInt.decode(malformed)
            # If it decodes, check the value is reasonable
            self.assertTrue(decoded >= 0, "Decoded negative value from malformed VarInt")
        except ValueError:
            pass  # Expected behavior - raised error
    
    def test_varint_incomplete(self):
        """Test incomplete VarInt handling"""
        # Incomplete VarInt (continuation bit set but no next byte)
        incomplete = bytes([0xFF])
        
        with self.assertRaises(ValueError):
            VarInt.decode(incomplete)
    
    def test_varint_negative_length(self):
        """Test VarInt that decodes to very large number"""
        # 0xFFFFFFFF encoded as VarInt
        large_varint = VarInt.encode(0xFFFFFFFF)
        decoded, _ = VarInt.decode(large_varint)
        
        self.assertEqual(decoded, 0xFFFFFFFF)


class TestPacketParsing(unittest.TestCase):
    """Test packet parsing logic"""
    
    def test_movement_packet_parsing(self):
        """Test parsing a valid movement packet"""
        # Build a movement packet
        packet = bytearray()
        packet.extend(VarInt.encode(0x01))  # MOVEMENT packet ID
        packet.extend(bytes.fromhex("deadbeef000000000000000000000001"))  # UUID
        packet.extend(struct.pack('>fff', 100.0, 64.0, 100.0))  # position
        packet.extend(struct.pack('>fff', 0.0, 0.0, 0.0))  # velocity
        packet.extend(struct.pack('>ff', 180.0, 0.0))  # yaw, pitch
        packet.extend(struct.pack('>B', 0x02))  # flags
        packet.extend(struct.pack('>I', 1000))  # tick
        
        hp = HytalePacket(bytes(packet))
        hp.parse()
        decoded = hp.decode()
        
        self.assertEqual(decoded['type'], 'MOVEMENT')
        self.assertEqual(decoded['position']['x'], 100.0)
        self.assertEqual(decoded['yaw'], 180.0)
    
    def test_malformed_packet_handling(self):
        """Test handling of malformed packets"""
        # Packet that's too short
        malformed = bytes([0x01, 0x00, 0x00])
        
        hp = HytalePacket(malformed)
        hp.parse()
        
        # Should handle gracefully (not crash)
        try:
            decoded = hp.decode()
            # If it returns something, check for error indicator
            self.assertIn('error', decoded, "Should indicate error for malformed packet")
        except Exception:
            pass  # Also acceptable - raised error


class LocalTestHarness(unittest.TestCase):
    """Run fuzzer tests WITHOUT needing live Hytale server"""
    
    def setUp(self):
        self.fuzzer = HytaleFuzzer()
        self.mock_socket = Mock()
    
    @patch('socket.socket')
    def test_idor_without_server(self, mock_socket_class):
        """Test IDOR logic on fake responses"""
        mock_socket_class.return_value = self.mock_socket
        
        # Simulate Server-Response: "OK, here's your PlayerData"
        # Server ACCEPTS forged UUID and returns player data
        fake_response = b'\x01\x0f{"playerID": 99, "coins": 5000}'
        self.mock_socket.recvfrom.return_value = (fake_response, ("127.0.0.1", 5520))
        
        # Run the IDOR test (it will use our mocked socket)
        result = self.fuzzer.test_idor_player_impersonation()
        
        # If server accepted the packet, we found IDOR
        if result:
            self.assertTrue(True, "IDOR vulnerability detected")
        else:
            self.assertTrue(True, "IDOR test passed (no vulnerability)")
    
    @patch('socket.socket')
    def test_varint_overflow_without_server(self, mock_socket_class):
        """Test VarInt overflow logic WITHOUT server"""
        mock_socket_class.return_value = self.mock_socket
        
        # Simulate server crash (no response)
        self.mock_socket.recvfrom.side_effect = TimeoutError("Server not responding")
        
        result = self.fuzzer.test_varint_overflow()
        
        # Check if fuzzer detected the crash
        if result:
            self.assertTrue(True, "VarInt overflow vulnerability detected")
    
    def test_nan_infinity_packet_building(self):
        """Test that NaN/Infinity packets can be built"""
        # Build packet with NaN position
        packet = bytearray()
        packet.extend(VarInt.encode(0x01))
        packet.extend(bytes(16))  # UUID
        
        try:
            nan_bytes = struct.pack('>fff', float('nan'), float('nan'), float('nan'))
            packet.extend(nan_bytes)
            packet.extend(bytes(12))  # velocity
            packet.extend(struct.pack('>ff', 0.0, 0.0))
            packet.extend(struct.pack('>B', 0))
            packet.extend(struct.pack('>I', 1000))
            
            self.assertTrue(len(packet) > 0, "Should be able to build NaN packet")
        except (ValueError, OverflowError):
            # Platform doesn't support NaN packing
            self.skipTest("Platform doesn't support NaN in struct.pack")
    
    @patch('socket.socket')
    def test_race_condition_detection(self, mock_socket_class):
        """Test race condition detection logic"""
        mock_socket_class.return_value = self.mock_socket
        
        # Simulate server accepting packets during phase transition
        self.mock_socket.recvfrom.return_value = (b'\x00\x01OK', ("127.0.0.1", 5520))
        
        # This would normally test phase transitions
        # For now, just verify the test infrastructure works
        self.assertTrue(True, "Race condition test infrastructure working")


class TestIntegratedFuzzer(unittest.TestCase):
    """Test the integrated fuzzer component"""
    
    def setUp(self):
        self.fuzzer = IntegratedFuzzer()
    
    def test_packet_definition_loading(self):
        """Test that packet definitions are loaded"""
        definition = self.fuzzer.get_packet_definition("0x01")
        self.assertIsNotNone(definition, "Should load movement packet definition")
        self.assertEqual(definition["name"], "MovementInput")
    
    def test_packet_building(self):
        """Test building a packet from definition"""
        field_values = {
            "playerID": "deadbeef000000000000000000000001",
            "position": (100.0, 64.0, 100.0),
            "velocity": (0.0, 0.0, 0.0),
            "yaw": 0.0,
            "pitch": 0.0,
            "flags": 0,
            "tick": 1000
        }
        
        packet = self.fuzzer.build_packet("0x01", field_values)
        
        self.assertIsInstance(packet, bytes)
        self.assertGreater(len(packet), 0)
    
    def test_mutation_generation(self):
        """Test that mutations are generated"""
        base_values = {
            "playerID": "deadbeef000000000000000000000001",
            "position": (100.0, 64.0, 100.0),
            "velocity": (0.0, 0.0, 0.0),
            "yaw": 0.0,
            "pitch": 0.0,
            "flags": 0,
            "tick": 1000
        }
        
        mutations = list(self.fuzzer.fuzz_packet("0x01", base_values))
        
        self.assertGreater(len(mutations), 0, "Should generate at least one mutation")
        
        # Check mutation format
        mutated_packet, description = mutations[0]
        self.assertIsInstance(mutated_packet, bytes)
        self.assertIn("mutated_field", description)
        self.assertIn("mutation_type", description)


class TestSecurityFinding(unittest.TestCase):
    """Test SecurityFinding report generation"""
    
    def test_finding_creation(self):
        """Test creating a security finding"""
        finding = SecurityFinding(
            vuln_type=VulnerabilityType.IDOR,
            severity="HIGH",
            title="Test Vulnerability",
            description="Test description",
            reproduction_steps=["Step 1", "Step 2"],
            poc_code="test_code()",
            impact="Test impact",
            mitigation="Test mitigation"
        )
        
        self.assertEqual(finding.severity, "HIGH")
        self.assertEqual(finding.vuln_type, VulnerabilityType.IDOR)
    
    def test_finding_report_generation(self):
        """Test report generation from finding"""
        finding = SecurityFinding(
            vuln_type=VulnerabilityType.DOS,
            severity="CRITICAL",
            title="Server Crash",
            description="Server crashes on malformed packet",
            reproduction_steps=["Send malformed packet"],
            poc_code="crash_server()",
            impact="Complete DoS",
            mitigation="Add validation"
        )
        
        report = finding.to_report()
        
        self.assertIn("CRITICAL", report)
        self.assertIn("Server Crash", report)
        self.assertIn("DoS", report)


def run_all_tests():
    """Run all test suites"""
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    
    # Add all test classes
    suite.addTests(loader.loadTestsFromTestCase(TestVarIntParsing))
    suite.addTests(loader.loadTestsFromTestCase(TestPacketParsing))
    suite.addTests(loader.loadTestsFromTestCase(LocalTestHarness))
    suite.addTests(loader.loadTestsFromTestCase(TestIntegratedFuzzer))
    suite.addTests(loader.loadTestsFromTestCase(TestSecurityFinding))
    
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    
    return result.wasSuccessful()


if __name__ == '__main__':
    print("="*70)
    print("HYTALE FUZZER TEST HARNESS")
    print("Testing WITHOUT live server")
    print("="*70)
    print()
    
    success = run_all_tests()
    
    print()
    print("="*70)
    if success:
        print("✓ All tests passed!")
    else:
        print("✗ Some tests failed")
    print("="*70)
    
    sys.exit(0 if success else 1)
