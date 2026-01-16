#!/usr/bin/env python3
"""
Fuzzer Integration with Existing Hytale Protocol Decoder
Bridge to existing decoders to avoid code duplication

This module:
1. Uses PACKET_STRUCTURES.json as ground truth
2. Leverages hytale_protocol_decoder.py for parsing
3. Provides systematic field mutation capabilities
"""

import json
import struct
from typing import Dict, Any, List, Generator, Optional
from dataclasses import dataclass
import logging

try:
    from hytale_protocol_decoder import VarInt, HytalePacket, Vector3f, Vector3i, PacketID
except ImportError:
    print("ERROR: hytale_protocol_decoder.py not found")
    raise

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class FieldMutation:
    """Represents a field mutation for fuzzing"""
    field_name: str
    original_value: Any
    mutated_value: Any
    mutation_type: str  # overflow, underflow, null, edge_case, etc.


class IntegratedFuzzer:
    """
    Utilize existing decoders instead of rewriting
    Leverages PACKET_STRUCTURES.json as ground truth
    """
    
    def __init__(self, packet_structures_file: str = "PACKET_STRUCTURES.json"):
        self.packet_definitions = self._load_packet_definitions(packet_structures_file)
        self.mutations_generated = 0
    
    def _load_packet_definitions(self, filename: str) -> Dict[str, Any]:
        """Load packet structures from JSON file"""
        try:
            with open(filename, 'r') as f:
                definitions = json.load(f)
            logger.info(f"Loaded {len(definitions)} packet definitions from {filename}")
            return definitions
        except FileNotFoundError:
            logger.error(f"Packet structures file not found: {filename}")
            logger.info("Using built-in minimal definitions")
            return self._get_minimal_definitions()
    
    def _get_minimal_definitions(self) -> Dict[str, Any]:
        """Fallback minimal packet definitions if file not found"""
        return {
            "0x01": {
                "name": "MovementInput",
                "fields": [
                    {"name": "playerID", "type": "UUID", "size": 16},
                    {"name": "position", "type": "Vector3f", "size": 12},
                    {"name": "velocity", "type": "Vector3f", "size": 12},
                    {"name": "yaw", "type": "f32", "size": 4},
                    {"name": "pitch", "type": "f32", "size": 4},
                    {"name": "flags", "type": "u8", "size": 1},
                    {"name": "tick", "type": "u32", "size": 4}
                ]
            },
            "0x03": {
                "name": "ChatMessage",
                "fields": [
                    {"name": "playerID", "type": "UUID", "size": 16},
                    {"name": "messageLength", "type": "varint"},
                    {"name": "message", "type": "UTF-8"},
                    {"name": "timestamp", "type": "u64", "size": 8}
                ]
            }
        }
    
    def get_packet_definition(self, packet_id: str) -> Optional[Dict[str, Any]]:
        """Get packet definition by ID"""
        return self.packet_definitions.get(packet_id)
    
    def build_packet(self, packet_id: str, field_values: Dict[str, Any]) -> bytes:
        """
        Build a packet using the definition and provided field values
        
        Args:
            packet_id: Packet ID (e.g., "0x01")
            field_values: Dictionary of field names to values
            
        Returns:
            Bytes representing the packet
        """
        definition = self.get_packet_definition(packet_id)
        if not definition:
            raise ValueError(f"Unknown packet ID: {packet_id}")
        
        packet = bytearray()
        
        # Add packet ID
        packet_id_int = int(packet_id, 16)
        packet.extend(VarInt.encode(packet_id_int))
        
        # Add fields according to definition
        for field in definition.get("fields", []):
            field_name = field["name"]
            field_type = field["type"]
            
            if field_name not in field_values:
                logger.warning(f"Missing field: {field_name}, using default")
                value = self._get_default_value(field_type)
            else:
                value = field_values[field_name]
            
            # Encode field based on type
            packet.extend(self._encode_field(field_type, value, field))
        
        return bytes(packet)
    
    def _get_default_value(self, field_type: str) -> Any:
        """Get default value for a field type"""
        defaults = {
            "UUID": bytes(16),
            "u8": 0,
            "u16": 0,
            "u32": 0,
            "u64": 0,
            "f32": 0.0,
            "f64": 0.0,
            "Vector3f": (0.0, 0.0, 0.0),
            "Vector3i": (0, 0, 0),
            "UTF-8": "",
            "varint": 0
        }
        return defaults.get(field_type, 0)
    
    def _encode_field(self, field_type: str, value: Any, field_def: Dict[str, Any]) -> bytes:
        """Encode a field value based on its type"""
        if field_type == "UUID":
            if isinstance(value, str):
                return bytes.fromhex(value.replace('-', ''))
            return value if isinstance(value, bytes) else bytes(16)
        
        elif field_type == "u8":
            return struct.pack('>B', value)
        elif field_type == "u16":
            return struct.pack('>H', value)
        elif field_type == "u32":
            return struct.pack('>I', value)
        elif field_type == "u64":
            return struct.pack('>Q', value)
        elif field_type == "f32":
            return struct.pack('>f', value)
        elif field_type == "f64":
            return struct.pack('>d', value)
        
        elif field_type == "Vector3f":
            if isinstance(value, (list, tuple)) and len(value) == 3:
                return struct.pack('>fff', *value)
            return struct.pack('>fff', 0.0, 0.0, 0.0)
        
        elif field_type == "Vector3i":
            if isinstance(value, (list, tuple)) and len(value) == 3:
                return struct.pack('>iii', *value)
            return struct.pack('>iii', 0, 0, 0)
        
        elif field_type == "varint":
            return VarInt.encode(value)
        
        elif field_type == "UTF-8":
            if isinstance(value, str):
                value_bytes = value.encode('utf-8')
                return VarInt.encode(len(value_bytes)) + value_bytes
            return VarInt.encode(0)
        
        else:
            logger.warning(f"Unknown field type: {field_type}")
            return b''
    
    def fuzz_packet(self, packet_id: str, 
                   base_values: Optional[Dict[str, Any]] = None,
                   mutations: Optional[List[str]] = None) -> Generator[tuple, None, None]:
        """
        Generate fuzzing variations of a packet
        
        Args:
            packet_id: Packet ID to fuzz
            base_values: Base field values (uses defaults if None)
            mutations: List of mutation types to apply
            
        Yields:
            Tuple of (mutated_packet_bytes, mutation_description)
        """
        definition = self.get_packet_definition(packet_id)
        if not definition:
            logger.error(f"Cannot fuzz unknown packet: {packet_id}")
            return
        
        base_values = base_values or {}
        mutations = mutations or ["overflow", "underflow", "null", "edge_case"]
        
        # Get all fields
        fields = definition.get("fields", [])
        
        for field in fields:
            field_name = field["name"]
            field_type = field["type"]
            
            # Get base value
            base_value = base_values.get(field_name, self._get_default_value(field_type))
            
            # Generate mutations for this field
            for mutation in mutations:
                mutated_value = self._mutate_value(field_type, base_value, mutation)
                
                if mutated_value is not None:
                    # Build packet with mutated field
                    field_values = base_values.copy()
                    field_values[field_name] = mutated_value
                    
                    # Fill in any missing fields with defaults
                    for f in fields:
                        if f["name"] not in field_values:
                            field_values[f["name"]] = self._get_default_value(f["type"])
                    
                    try:
                        mutated_packet = self.build_packet(packet_id, field_values)
                        self.mutations_generated += 1
                        
                        yield mutated_packet, {
                            "packet_id": packet_id,
                            "packet_name": definition["name"],
                            "mutated_field": field_name,
                            "mutation_type": mutation,
                            "original_value": base_value,
                            "mutated_value": mutated_value
                        }
                    except Exception as e:
                        logger.debug(f"Failed to build mutated packet: {e}")
    
    def _mutate_value(self, field_type: str, base_value: Any, mutation_type: str) -> Optional[Any]:
        """Generate a mutated value based on field type and mutation type"""
        
        if mutation_type == "overflow":
            if field_type == "u8":
                return 256  # Overflow by 1
            elif field_type == "u16":
                return 65536
            elif field_type == "u32":
                return 0xFFFFFFFF + 1
            elif field_type == "u64":
                return 0xFFFFFFFFFFFFFFFF + 1
            elif field_type == "f32":
                return float('inf')
            elif field_type == "varint":
                return 0xFFFFFFFF
        
        elif mutation_type == "underflow":
            if field_type in ["u8", "u16", "u32", "u64"]:
                return -1  # Negative for unsigned
            elif field_type == "f32":
                return float('-inf')
        
        elif mutation_type == "null":
            if field_type == "UUID":
                return bytes(16)  # All zeros
            elif field_type == "UTF-8":
                return ""  # Empty string
            elif field_type in ["Vector3f", "Vector3i"]:
                return (0, 0, 0)
        
        elif mutation_type == "edge_case":
            if field_type == "f32":
                return float('nan')
            elif field_type == "u32":
                return 0xFFFFFFFF  # Max value
            elif field_type == "UTF-8":
                return "A" * 10000  # Very long string
            elif field_type == "Vector3f":
                return (float('inf'), float('nan'), 0.0)
        
        return None
    
    def analyze_packet(self, packet_bytes: bytes) -> Dict[str, Any]:
        """
        Analyze a packet using the existing decoder
        
        Args:
            packet_bytes: Raw packet bytes
            
        Returns:
            Analysis dictionary
        """
        try:
            packet = HytalePacket(packet_bytes)
            packet.parse()
            decoded = packet.decode()
            
            return {
                "success": True,
                "packet_id": f"0x{packet.packet_id:02X}",
                "decoded": decoded
            }
        except Exception as e:
            return {
                "success": False,
                "error": str(e),
                "raw_hex": packet_bytes.hex()
            }


def main():
    """Example usage"""
    import argparse
    
    parser = argparse.ArgumentParser(description="Integrated Fuzzer")
    parser.add_argument("--packet", default="0x01", help="Packet ID to fuzz")
    parser.add_argument("--mutations", type=int, default=10, help="Number of mutations to generate")
    parser.add_argument("--structures", default="PACKET_STRUCTURES.json", 
                       help="Path to packet structures file")
    
    args = parser.parse_args()
    
    fuzzer = IntegratedFuzzer(packet_structures_file=args.structures)
    
    # Example: Fuzz movement packet
    print(f"=== Fuzzing packet {args.packet} ===\n")
    
    base_values = {
        "playerID": "deadbeef000000000000000000000001",
        "position": (100.0, 64.0, 100.0),
        "velocity": (0.0, 0.0, 0.0),
        "yaw": 0.0,
        "pitch": 0.0,
        "flags": 0,
        "tick": 1000
    }
    
    count = 0
    for mutated_packet, description in fuzzer.fuzz_packet(args.packet, base_values):
        count += 1
        
        print(f"Mutation #{count}:")
        print(f"  Field: {description['mutated_field']}")
        print(f"  Type: {description['mutation_type']}")
        print(f"  Original: {description['original_value']}")
        print(f"  Mutated: {description['mutated_value']}")
        print(f"  Packet (hex): {mutated_packet.hex()[:80]}...")
        print()
        
        # Analyze the mutated packet
        analysis = fuzzer.analyze_packet(mutated_packet)
        if not analysis["success"]:
            print(f"  ⚠️ Parser error: {analysis['error']}")
        print()
        
        if count >= args.mutations:
            break
    
    print(f"Generated {count} mutations")


if __name__ == "__main__":
    main()
