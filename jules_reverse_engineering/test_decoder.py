import unittest
from hytale_protocol_decoder import HytalePacket, PacketID

class TestHytaleDecoder(unittest.TestCase):
    def test_movement_packet(self):
        # Construct hex for movement
        # ID: 01
        # UUID: a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6
        # Pos: 100.5, 64.0, 200.0 -> 42c90000 42800000 43480000
        # Vel: 0, 0, 0 -> 00000000 00000000 00000000
        # Yaw: 180.0 -> 43340000
        # Pitch: 0.0 -> 00000000
        # Flags: 2 (sprinting) -> 02
        # Tick: 12345 -> 00003039
        hex_data = "01a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d642c90000428000004348000000000000000000000000000043340000000000000200003039"
        packet = HytalePacket(bytes.fromhex(hex_data))
        packet.parse()
        decoded = packet.decode()

        self.assertEqual(packet.packet_id, PacketID.MOVEMENT.value)
        self.assertEqual(decoded["type"], "MOVEMENT")
        self.assertEqual(decoded["player_id"], "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6")
        self.assertAlmostEqual(decoded["position"]["x"], 100.5)
        self.assertTrue(decoded["flags"]["is_sprinting"])
        self.assertFalse(decoded["flags"]["is_jumping"])

    def test_chat_packet(self):
        # ID: 03
        # UUID: ...
        # Length: 11 (0x0B) -> "Hello World"
        # Timestamp
        hex_data = "03a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d60b48656c6c6f20576f726c640000018d1234abcd"
        packet = HytalePacket(bytes.fromhex(hex_data))
        packet.parse()
        decoded = packet.decode()

        self.assertEqual(decoded["type"], "CHAT")
        self.assertEqual(decoded["message"], "Hello World")

if __name__ == '__main__':
    unittest.main()
