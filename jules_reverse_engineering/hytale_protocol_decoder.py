#!/usr/bin/env python3
"""
Hytale Protocol Decoder
Parses raw packet data based on PACKET_STRUCTURES.json
"""

import struct
import json
from enum import Enum
from dataclasses import dataclass
from typing import Dict, Any, Tuple

class PacketID(Enum):
    MOVEMENT = 0x01
    CHAT = 0x03
    BLOCK_INTERACTION = 0x05
    ITEM_USE = 0x07
    ENTITY_SPAWN = 0x08
    GAME_PHASE_CHANGE = 0x0F

class VarInt:
    @staticmethod
    def decode(data: bytes, offset: int = 0) -> Tuple[int, int]:
        """Decode LEB128 varint, return (value, new_offset)"""
        result = 0
        shift = 0
        i = offset
        while i < len(data):
            byte = data[i]
            result |= (byte & 0x7F) << shift
            if byte & 0x80 == 0:
                return result, i + 1
            shift += 7
            i += 1
        raise ValueError(f"Incomplete varint at offset {offset}")

    @staticmethod
    def encode(value: int) -> bytes:
        """Encode integer as LEB128 varint"""
        result = bytearray()
        while True:
            byte = value & 0x7F
            value >>= 7
            if value != 0:
                byte |= 0x80
            result.append(byte)
            if value == 0:
                break
        return bytes(result)

@dataclass
class Vector3f:
    x: float
    y: float
    z: float

    @classmethod
    def from_bytes(cls, data: bytes, offset: int = 0):
        x, y, z = struct.unpack_from('>fff', data, offset)
        return cls(x, y, z), offset + 12

@dataclass
class Vector3i:
    x: int
    y: int
    z: int

    @classmethod
    def from_bytes(cls, data: bytes, offset: int = 0):
        x, y, z = struct.unpack_from('>iii', data, offset)
        return cls(x, y, z), offset + 12

class HytalePacket:
    def __init__(self, raw_data: bytes):
        self.raw_data = raw_data
        self.packet_id = None
        self.payload = None
        self.decoded = None

    def parse(self):
        """Parse packet ID and extract payload"""
        self.packet_id, offset = VarInt.decode(self.raw_data)
        self.payload = self.raw_data[offset:]
        return self

    def decode(self) -> Dict[str, Any]:
        """Decode payload based on packet ID"""
        if self.packet_id == PacketID.MOVEMENT.value:
            return self._decode_movement()
        elif self.packet_id == PacketID.CHAT.value:
            return self._decode_chat()
        elif self.packet_id == PacketID.BLOCK_INTERACTION.value:
            return self._decode_block_interaction()
        elif self.packet_id == PacketID.ENTITY_SPAWN.value:
            return self._decode_entity_spawn()
        elif self.packet_id == PacketID.GAME_PHASE_CHANGE.value:
            return self._decode_game_phase_change()
        else:
            return {"error": f"Unknown packet ID: 0x{self.packet_id:02X}"}

    def _decode_movement(self) -> Dict[str, Any]:
        """Decode 0x01 Movement packet"""
        offset = 0

        # UUID (16 bytes)
        player_id = self.payload[offset:offset+16].hex()
        offset += 16

        # Position (Vector3f, 12 bytes)
        position, offset = Vector3f.from_bytes(self.payload, offset)

        # Velocity (Vector3f, 12 bytes)
        velocity, offset = Vector3f.from_bytes(self.payload, offset)

        # Yaw (f32, 4 bytes)
        yaw = struct.unpack_from('>f', self.payload, offset)[0]
        offset += 4

        # Pitch (f32, 4 bytes)
        pitch = struct.unpack_from('>f', self.payload, offset)[0]
        offset += 4

        # Flags (u8, 1 byte)
        flags = self.payload[offset]
        offset += 1

        # Tick (u32, 4 bytes)
        tick = struct.unpack_from('>I', self.payload, offset)[0]

        return {
            "type": "MOVEMENT",
            "player_id": player_id,
            "position": {"x": position.x, "y": position.y, "z": position.z},
            "velocity": {"x": velocity.x, "y": velocity.y, "z": velocity.z},
            "yaw": yaw,
            "pitch": pitch,
            "flags": {
                "is_jumping": bool(flags & 0x01),
                "is_sprinting": bool(flags & 0x02),
                "is_crouching": bool(flags & 0x04),
                "is_swimming": bool(flags & 0x08),
                "is_flying": bool(flags & 0x10)
            },
            "tick": tick
        }

    def _decode_chat(self) -> Dict[str, Any]:
        """Decode 0x03 Chat packet"""
        offset = 0

        player_id = self.payload[offset:offset+16].hex()
        offset += 16

        msg_length, offset = VarInt.decode(self.payload, offset)
        message = self.payload[offset:offset+msg_length].decode('utf-8')
        offset += msg_length

        timestamp = struct.unpack_from('>Q', self.payload, offset)[0]

        return {
            "type": "CHAT",
            "player_id": player_id,
            "message": message,
            "timestamp": timestamp
        }

    def _decode_block_interaction(self) -> Dict[str, Any]:
        """Decode 0x05 Block Interaction packet"""
        offset = 0

        player_id = self.payload[offset:offset+16].hex()
        offset += 16

        block_pos, offset = Vector3i.from_bytes(self.payload, offset)

        face = self.payload[offset]
        offset += 1

        action = self.payload[offset]

        faces = ["top", "bottom", "north", "south", "east", "west"]
        actions = ["place", "break", "interact"]

        return {
            "type": "BLOCK_INTERACTION",
            "player_id": player_id,
            "block_position": {"x": block_pos.x, "y": block_pos.y, "z": block_pos.z},
            "face": faces[face] if face < len(faces) else f"unknown_{face}",
            "action": actions[action] if action < len(actions) else f"unknown_{action}"
        }

    def _decode_entity_spawn(self) -> Dict[str, Any]:
        """Decode 0x08 Entity Spawn packet"""
        offset = 0

        entity_id = struct.unpack_from('>I', self.payload, offset)[0]
        offset += 4

        entity_type = struct.unpack_from('>H', self.payload, offset)[0]
        offset += 2

        position, offset = Vector3f.from_bytes(self.payload, offset)

        yaw, pitch = struct.unpack_from('>ff', self.payload, offset)
        offset += 8

        entity_types = ["player", "monster", "npc", "item"]

        return {
            "type": "ENTITY_SPAWN",
            "entity_id": entity_id,
            "entity_type": entity_types[entity_type] if entity_type < len(entity_types) else f"unknown_{entity_type}",
            "position": {"x": position.x, "y": position.y, "z": position.z},
            "rotation": {"yaw": yaw, "pitch": pitch}
        }

    def _decode_game_phase_change(self) -> Dict[str, Any]:
        """Decode 0x0F Game Phase Change packet"""
        offset = 0

        phase = self.payload[offset]
        offset += 1

        duration = struct.unpack_from('>I', self.payload, offset)[0]
        offset += 4

        announcement_length, offset = VarInt.decode(self.payload, offset)
        announcement = self.payload[offset:offset+announcement_length].decode('utf-8')

        phases = ["LOBBY", "DAY", "VOTING", "NIGHT", "END"]

        return {
            "type": "GAME_PHASE_CHANGE",
            "new_phase": phases[phase] if phase < len(phases) else f"unknown_{phase}",
            "duration_ms": duration,
            "announcement": announcement
        }

    def pretty_print(self):
        """Print decoded packet in readable format"""
        if self.decoded is None:
            self.decoded = self.decode()

        print(f"\n{'='*60}")
        print(f"Packet ID: 0x{self.packet_id:02X}")
        print(f"Type: {self.decoded.get('type', 'UNKNOWN')}")
        print(f"{'='*60}")

        for key, value in self.decoded.items():
            if key != 'type':
                print(f"  {key}: {value}")

        print(f"{'='*60}\n")

def decode_hex_packet(hex_string: str):
    """Decode packet from hex string"""
    data = bytes.fromhex(hex_string.replace(' ', ''))
    packet = HytalePacket(data)
    packet.parse()
    packet.decoded = packet.decode()
    packet.pretty_print()
    return packet

# Example usage and tests
if __name__ == "__main__":
    print("Hytale Protocol Decoder - Test Suite\n")

    # Test 1: Movement packet
    print("Test 1: Movement Packet")
    movement_hex = "01" + \
                   "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6" + \
                   "42c90000" + "42800000" + "43480000" + \
                   "00000000" + "00000000" + "00000000" + \
                   "43340000" + "00000000" + \
                   "02" + \
                   "00003039"
    decode_hex_packet(movement_hex)

    # Test 2: Chat packet
    print("Test 2: Chat Packet")
    # Length of "Hello World" is 11 (0x0B)
    chat_hex = "03" + \
               "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6" + \
               "0b" + \
               "48656c6c6f20576f726c64" + \
               "0000018d1234abcd"
    decode_hex_packet(chat_hex)
