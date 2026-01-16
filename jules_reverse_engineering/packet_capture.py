#!/usr/bin/env python3
"""
Network Packet Capture & Replay for Hytale Protocol
Live QUIC/UDP traffic capture and replay for fuzzing

This module provides functionality to:
1. Capture live Hytale protocol traffic from a running server
2. Store and analyze captured packets
3. Replay packets for testing and fuzzing
"""

import socket
import time
import json
import struct
from typing import List, Dict, Any, Optional
from dataclasses import dataclass, asdict
import logging

try:
    from hytale_protocol_decoder import VarInt, HytalePacket
except ImportError:
    print("Warning: hytale_protocol_decoder.py not found. Limited functionality.")

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Note: Using socket-based capture instead of Scapy to avoid external dependencies
# For production use, consider using Scapy or tcpdump for more robust packet capture


@dataclass
class CapturedPacket:
    """Represents a captured network packet"""
    raw_hex: str
    timestamp: float
    source: str
    destination: str
    decoded: Optional[Dict[str, Any]] = None
    packet_type: Optional[str] = None
    
    def to_dict(self):
        return asdict(self)


class PacketCapture:
    """
    Live QUIC/UDP traffic capture & replay for fuzzing
    
    ⚠️ IMPORTANT LIMITATIONS:
    - This uses UDP socket binding (not true packet sniffing)
    - Will only capture packets SENT TO this machine on port 5520
    - Will NOT capture traffic between other hosts
    - For production/realistic capture, use Scapy/tcpdump/Wireshark:
        sudo tcpdump -i any -w capture.pcap udp port 5520
        OR: pip install scapy (see requirements.txt)
    
    This implementation is best-effort for:
    - Testing packet replay functionality
    - Capturing packets from a local server
    - Extracting ground truth from controlled environments
    
    For real client↔server traffic analysis, use proper packet sniffing tools.
    """
    
    def __init__(self, port: int = 5520, interface: str = "0.0.0.0"):
        self.port = port
        self.interface = interface
        self.packets: List[CapturedPacket] = []
        self.capture_active = False
        
    def capture_packets(self, duration: int = 30, max_packets: int = 100) -> List[CapturedPacket]:
        """
        Capture Hytale protocol packets for specified duration
        
        Args:
            duration: Time in seconds to capture packets
            max_packets: Maximum number of packets to capture
            
        Returns:
            List of captured packets
        """
        logger.info(f"Starting packet capture on port {self.port} for {duration}s")
        
        # Create UDP socket for listening
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.settimeout(1.0)  # 1 second timeout for non-blocking
        
        try:
            sock.bind((self.interface, self.port))
        except OSError as e:
            logger.error(f"Failed to bind to {self.interface}:{self.port} - {e}")
            logger.info("Note: You may need to run with sudo/admin privileges or use a different port")
            return []
        
        self.capture_active = True
        start_time = time.time()
        captured_count = 0
        
        logger.info("Listening for packets... (waiting for traffic)")
        
        while self.capture_active and (time.time() - start_time) < duration:
            if captured_count >= max_packets:
                logger.info(f"Reached max packet limit ({max_packets})")
                break
            
            try:
                data, addr = sock.recvfrom(4096)
                timestamp = time.time()
                
                # Parse the packet
                packet = self._parse_packet(data, addr, timestamp)
                self.packets.append(packet)
                captured_count += 1
                
                logger.info(f"Captured packet #{captured_count} from {addr} ({len(data)} bytes)")
                
            except socket.timeout:
                # No packet received in this interval, continue
                continue
            except Exception as e:
                logger.error(f"Error capturing packet: {e}")
                continue
        
        sock.close()
        self.capture_active = False
        
        logger.info(f"Capture complete. Captured {len(self.packets)} packets")
        return self.packets
    
    def _parse_packet(self, data: bytes, addr: tuple, timestamp: float) -> CapturedPacket:
        """Parse raw packet data into CapturedPacket structure"""
        packet = CapturedPacket(
            raw_hex=data.hex(),
            timestamp=timestamp,
            source=f"{addr[0]}:{addr[1]}",
            destination=f"{self.interface}:{self.port}"
        )
        
        # Try to decode using hytale_protocol_decoder
        try:
            hp = HytalePacket(data)
            hp.parse()
            decoded = hp.decode()
            packet.decoded = decoded
            packet.packet_type = decoded.get('type', 'UNKNOWN')
        except Exception as e:
            logger.debug(f"Could not decode packet: {e}")
            packet.decoded = {"error": str(e)}
        
        return packet
    
    def stop_capture(self):
        """Stop active packet capture"""
        self.capture_active = False
        logger.info("Stopping packet capture...")
    
    def save_capture(self, filename: str):
        """Save captured packets to JSON file"""
        data = {
            "capture_time": time.strftime("%Y-%m-%d %H:%M:%S"),
            "port": self.port,
            "packet_count": len(self.packets),
            "packets": [p.to_dict() for p in self.packets]
        }
        
        with open(filename, 'w') as f:
            json.dump(data, f, indent=2)
        
        logger.info(f"Saved {len(self.packets)} packets to {filename}")
    
    def load_capture(self, filename: str) -> List[CapturedPacket]:
        """Load previously captured packets from JSON file"""
        with open(filename, 'r') as f:
            data = json.load(f)
        
        self.packets = []
        for pkt_data in data.get('packets', []):
            packet = CapturedPacket(**pkt_data)
            self.packets.append(packet)
        
        logger.info(f"Loaded {len(self.packets)} packets from {filename}")
        return self.packets
    
    def replay_packets(self, host: str, port: int, packets: Optional[List[CapturedPacket]] = None,
                       delay: float = 0.1) -> List[bytes]:
        """
        Replay captured packets to target server
        
        Args:
            host: Target host
            port: Target port
            packets: List of packets to replay (uses self.packets if None)
            delay: Delay between packets in seconds
            
        Returns:
            List of responses received
        """
        packets_to_replay = packets or self.packets
        
        if not packets_to_replay:
            logger.warning("No packets to replay")
            return []
        
        logger.info(f"Replaying {len(packets_to_replay)} packets to {host}:{port}")
        
        responses = []
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.settimeout(2.0)
        
        for i, packet in enumerate(packets_to_replay):
            try:
                # Send packet
                raw_data = bytes.fromhex(packet.raw_hex)
                sock.sendto(raw_data, (host, port))
                logger.info(f"Replayed packet #{i+1} ({len(raw_data)} bytes)")
                
                # Try to receive response
                try:
                    response, _ = sock.recvfrom(4096)
                    responses.append(response)
                    logger.info(f"  Received response: {len(response)} bytes")
                except socket.timeout:
                    logger.debug(f"  No response for packet #{i+1}")
                    responses.append(b'')
                
                # Delay before next packet
                if delay > 0 and i < len(packets_to_replay) - 1:
                    time.sleep(delay)
                    
            except Exception as e:
                logger.error(f"Error replaying packet #{i+1}: {e}")
                responses.append(b'')
        
        sock.close()
        logger.info(f"Replay complete. Received {sum(1 for r in responses if r)} responses")
        return responses
    
    def get_packets_by_type(self, packet_type: str) -> List[CapturedPacket]:
        """Filter packets by type"""
        return [p for p in self.packets if p.packet_type == packet_type]
    
    def analyze_auth_flow(self) -> Dict[str, Any]:
        """
        Analyze captured packets to extract authentication flow
        This is useful for understanding the Ground Truth for auth tokens
        """
        analysis = {
            "total_packets": len(self.packets),
            "packet_types": {},
            "auth_packets": [],
            "movement_packets": [],
            "chat_packets": []
        }
        
        for packet in self.packets:
            pkt_type = packet.packet_type or "UNKNOWN"
            analysis["packet_types"][pkt_type] = analysis["packet_types"].get(pkt_type, 0) + 1
            
            # Categorize important packet types
            if pkt_type in ["AUTH", "AUTH_REQUEST", "AUTH_RESPONSE", "LOGIN"]:
                analysis["auth_packets"].append(packet)
            elif pkt_type == "MOVEMENT":
                analysis["movement_packets"].append(packet)
            elif pkt_type == "CHAT":
                analysis["chat_packets"].append(packet)
        
        logger.info(f"Analysis: {analysis['total_packets']} total packets")
        for pkt_type, count in analysis["packet_types"].items():
            logger.info(f"  {pkt_type}: {count}")
        
        return analysis
    
    def extract_ground_truth(self) -> Dict[str, Any]:
        """
        Extract Ground Truth data from captured packets
        - Token formats
        - VarInt length patterns
        - Common field values
        """
        ground_truth = {
            "varint_lengths": set(),
            "uuid_patterns": set(),
            "token_formats": [],
            "common_values": {}
        }
        
        for packet in self.packets:
            if packet.decoded and isinstance(packet.decoded, dict):
                # Extract UUIDs
                if 'player_id' in packet.decoded:
                    ground_truth["uuid_patterns"].add(packet.decoded['player_id'])
                
                # Look for token-like fields
                for key, value in packet.decoded.items():
                    if 'token' in key.lower() or 'auth' in key.lower():
                        ground_truth["token_formats"].append({
                            "field": key,
                            "value": str(value),
                            "length": len(str(value))
                        })
        
        return {
            "varint_lengths": list(ground_truth["varint_lengths"]),
            "uuid_patterns": list(ground_truth["uuid_patterns"]),
            "token_formats": ground_truth["token_formats"]
        }


def main():
    """Example usage of PacketCapture"""
    import argparse
    
    parser = argparse.ArgumentParser(description="Hytale Packet Capture & Replay")
    parser.add_argument("--capture", action="store_true", help="Capture packets")
    parser.add_argument("--replay", help="Replay packets from file")
    parser.add_argument("--port", type=int, default=5520, help="Port to capture/replay on")
    parser.add_argument("--duration", type=int, default=30, help="Capture duration in seconds")
    parser.add_argument("--host", default="localhost", help="Target host for replay")
    parser.add_argument("--save", help="Save capture to file")
    parser.add_argument("--load", help="Load capture from file")
    parser.add_argument("--analyze", action="store_true", help="Analyze captured packets")
    
    args = parser.parse_args()
    
    capturer = PacketCapture(port=args.port)
    
    if args.load:
        capturer.load_capture(args.load)
        if args.analyze:
            analysis = capturer.analyze_auth_flow()
            ground_truth = capturer.extract_ground_truth()
            print("\n=== Ground Truth Extraction ===")
            print(json.dumps(ground_truth, indent=2))
    
    if args.capture:
        packets = capturer.capture_packets(duration=args.duration)
        
        if args.save:
            capturer.save_capture(args.save)
        
        if args.analyze:
            analysis = capturer.analyze_auth_flow()
            print("\n=== Packet Analysis ===")
            print(f"Total packets: {analysis['total_packets']}")
            print(f"Packet types: {analysis['packet_types']}")
    
    if args.replay:
        capturer.load_capture(args.replay)
        responses = capturer.replay_packets(args.host, args.port)
        print(f"\nReplayed {len(capturer.packets)} packets, received {sum(1 for r in responses if r)} responses")


if __name__ == "__main__":
    main()
