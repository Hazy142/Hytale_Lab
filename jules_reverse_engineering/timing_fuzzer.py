#!/usr/bin/env python3
"""
Timing Attack & Rate Limit Fuzzer for Hytale
Find exact rate limit thresholds and timing side-channels

This module provides:
1. Rate limit discovery
2. Timing attack detection
3. DoS threshold identification
4. Response time analysis
"""

import socket
import time
import statistics
from typing import List, Dict, Any, Optional, Tuple
from dataclasses import dataclass
import logging
import struct

try:
    from hytale_protocol_decoder import VarInt, PacketID
except ImportError:
    print("Warning: hytale_protocol_decoder.py not found")

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class TimingMeasurement:
    """Represents a timing measurement"""
    packet_count: int
    response_time: float
    success: bool
    timestamp: float


class RateLimitFuzzer:
    """
    Find exact rate limit thresholds + timing side-channels
    
    Based on PROTOCOL_SPEC.md:
    - Movement Packets: Max 20/sec
    - Chat Packets: Max 5/sec
    """
    
    def __init__(self, host: str = "localhost", port: int = 5520):
        self.host = host
        self.port = port
        self.measurements: List[TimingMeasurement] = []
    
    def _send_and_measure(self, packet_data: bytes, timeout: float = 2.0) -> Tuple[Optional[bytes], float]:
        """Send packet and measure response time"""
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.settimeout(timeout)
        
        start = time.perf_counter()
        try:
            sock.sendto(packet_data, (self.host, self.port))
            response, _ = sock.recvfrom(4096)
            end = time.perf_counter()
            sock.close()
            return response, end - start
        except socket.timeout:
            end = time.perf_counter()
            sock.close()
            return None, end - start
        except Exception as e:
            end = time.perf_counter()
            sock.close()
            logger.debug(f"Error: {e}")
            return None, end - start
    
    def _build_entity_spawn_packet(self, entity_id: int) -> bytes:
        """Build an EntitySpawn packet for testing"""
        packet = bytearray()
        
        # Packet ID for EntitySpawn (0x08)
        packet.extend(VarInt.encode(0x08))
        
        # Entity ID (u32)
        packet.extend(struct.pack('>I', entity_id))
        
        # Entity type (u16) - NPC
        packet.extend(struct.pack('>H', 2))
        
        # Position (Vector3f)
        packet.extend(struct.pack('>fff', 100.0, 64.0, 100.0))
        
        # Rotation (Vector2f - yaw, pitch)
        packet.extend(struct.pack('>ff', 0.0, 0.0))
        
        return bytes(packet)
    
    def _build_movement_packet(self, sequence: int) -> bytes:
        """Build a Movement packet for rate limit testing"""
        packet = bytearray()
        
        # Packet ID for Movement (0x01)
        packet.extend(VarInt.encode(0x01))
        
        # Player UUID (test UUID)
        packet.extend(bytes.fromhex("deadbeef000000000000000000000001"))
        
        # Position (Vector3f)
        packet.extend(struct.pack('>fff', 100.0 + sequence * 0.1, 64.0, 100.0))
        
        # Velocity (Vector3f)
        packet.extend(struct.pack('>fff', 0.0, 0.0, 0.0))
        
        # Yaw, Pitch (f32)
        packet.extend(struct.pack('>ff', 0.0, 0.0))
        
        # Flags (u8)
        packet.extend(struct.pack('>B', 0))
        
        # Tick (u32)
        packet.extend(struct.pack('>I', 1000 + sequence))
        
        return bytes(packet)
    
    def _build_chat_packet(self, message: str, sequence: int) -> bytes:
        """Build a Chat packet for rate limit testing"""
        packet = bytearray()
        
        # Packet ID for Chat (0x03)
        packet.extend(VarInt.encode(0x03))
        
        # Player UUID
        packet.extend(bytes.fromhex("deadbeef000000000000000000000001"))
        
        # Message length (VarInt) and message
        msg_with_seq = f"{message}_{sequence}"
        msg_bytes = msg_with_seq.encode('utf-8')
        packet.extend(VarInt.encode(len(msg_bytes)))
        packet.extend(msg_bytes)
        
        # Timestamp (u64)
        packet.extend(struct.pack('>Q', int(time.time() * 1000)))
        
        return bytes(packet)
    
    def test_entity_spawn_flooding(self, max_entities: int = 1000, 
                                   alert_threshold: float = 2.0) -> Dict[str, Any]:
        """
        Test: How many entities can you spawn before server breaks?
        
        Args:
            max_entities: Maximum entities to test
            alert_threshold: Response time multiplier to detect slowdown
            
        Returns:
            Dictionary with test results including threshold
        """
        logger.info(f"Testing entity spawn flooding (max: {max_entities})")
        
        response_times = []
        entity_count = 0
        threshold_found = None
        crash_detected = False
        
        for entity_count in range(max_entities):
            # Build and send EntitySpawn packet
            pkt = self._build_entity_spawn_packet(entity_id=entity_count)
            response, response_time = self._send_and_measure(pkt)
            
            response_times.append(response_time)
            
            # Check if server is slowing down (potential DoS)
            if len(response_times) > 10:
                recent_median = statistics.median(response_times[-10:])
                overall_median = statistics.median(response_times[:-10])
                
                if response_time > overall_median * alert_threshold:
                    if threshold_found is None:
                        threshold_found = entity_count
                        logger.warning(
                            f"⚠️ THRESHOLD DETECTED: Server slowing at {entity_count} entities"
                        )
                        logger.warning(
                            f"   Response time: {response_time:.3f}s "
                            f"(median: {overall_median:.3f}s)"
                        )
            
            # Check for server crash/timeout
            if response_time > 5.0:
                logger.critical(
                    f"💥 SERVER CRASH/TIMEOUT at {entity_count} entities! "
                    f"(response time: {response_time:.3f}s)"
                )
                crash_detected = True
                break
            
            # No response = possible crash
            if response is None and entity_count > 10:
                logger.warning(f"No response for entity #{entity_count}")
            
            # Small delay to avoid overwhelming the test
            time.sleep(0.01)
        
        result = {
            "test": "entity_spawn_flooding",
            "total_entities_spawned": entity_count,
            "threshold_entities": threshold_found,
            "crash_detected": crash_detected,
            "max_response_time": max(response_times) if response_times else 0,
            "avg_response_time": statistics.mean(response_times) if response_times else 0,
            "response_times": response_times[:100]  # First 100 for analysis
        }
        
        if crash_detected:
            result["severity"] = "CRITICAL"
            result["bounty_potential"] = "$5,000-$10,000"
            result["description"] = f"Server crashes at {entity_count} entity spawns"
        elif threshold_found:
            result["severity"] = "HIGH"
            result["bounty_potential"] = "$3,000-$8,000"
            result["description"] = f"Server performance degrades at {threshold_found} entities"
        else:
            result["severity"] = "INFO"
            result["description"] = "No obvious DoS threshold found"
        
        return result
    
    def test_movement_rate_limit(self, packets_per_second: int = 50, 
                                 duration: int = 5) -> Dict[str, Any]:
        """
        Test movement packet rate limiting
        
        PROTOCOL_SPEC.md says: Max 20/sec
        Test with higher rate to find enforcement
        """
        logger.info(f"Testing movement rate limit: {packets_per_second} pkt/s for {duration}s")
        
        total_packets = packets_per_second * duration
        interval = 1.0 / packets_per_second
        
        sent_count = 0
        accepted_count = 0
        rejected_count = 0
        
        start_time = time.time()
        
        for i in range(total_packets):
            pkt = self._build_movement_packet(sequence=i)
            response, response_time = self._send_and_measure(pkt, timeout=0.5)
            
            sent_count += 1
            if response:
                accepted_count += 1
            else:
                rejected_count += 1
            
            # Maintain target packet rate
            elapsed = time.time() - start_time
            target_time = (i + 1) * interval
            sleep_time = target_time - elapsed
            if sleep_time > 0:
                time.sleep(sleep_time)
        
        actual_duration = time.time() - start_time
        actual_rate = sent_count / actual_duration
        acceptance_rate = accepted_count / sent_count if sent_count > 0 else 0
        
        result = {
            "test": "movement_rate_limit",
            "target_rate": packets_per_second,
            "actual_rate": actual_rate,
            "sent_packets": sent_count,
            "accepted_packets": accepted_count,
            "rejected_packets": rejected_count,
            "acceptance_rate": acceptance_rate,
            "duration": actual_duration
        }
        
        # Check if rate limiting is properly enforced
        if acceptance_rate > 0.95 and packets_per_second > 20:
            result["severity"] = "MEDIUM"
            result["bounty_potential"] = "$2,000-$5,000"
            result["description"] = "Rate limiting not enforced - DoS possible"
        else:
            result["severity"] = "INFO"
            result["description"] = "Rate limiting appears to be working"
        
        return result
    
    def test_chat_rate_limit(self, messages_per_second: int = 10, 
                            duration: int = 5) -> Dict[str, Any]:
        """
        Test chat packet rate limiting
        
        PROTOCOL_SPEC.md says: Max 5/sec
        """
        logger.info(f"Testing chat rate limit: {messages_per_second} msg/s for {duration}s")
        
        total_messages = messages_per_second * duration
        interval = 1.0 / messages_per_second
        
        sent_count = 0
        accepted_count = 0
        
        start_time = time.time()
        
        for i in range(total_messages):
            pkt = self._build_chat_packet("spam", sequence=i)
            response, response_time = self._send_and_measure(pkt, timeout=0.5)
            
            sent_count += 1
            if response:
                accepted_count += 1
            
            elapsed = time.time() - start_time
            target_time = (i + 1) * interval
            sleep_time = target_time - elapsed
            if sleep_time > 0:
                time.sleep(sleep_time)
        
        actual_duration = time.time() - start_time
        actual_rate = sent_count / actual_duration
        acceptance_rate = accepted_count / sent_count if sent_count > 0 else 0
        
        result = {
            "test": "chat_rate_limit",
            "target_rate": messages_per_second,
            "actual_rate": actual_rate,
            "sent_messages": sent_count,
            "accepted_messages": accepted_count,
            "acceptance_rate": acceptance_rate,
            "duration": actual_duration
        }
        
        if acceptance_rate > 0.95 and messages_per_second > 5:
            result["severity"] = "MEDIUM"
            result["bounty_potential"] = "$1,000-$3,000"
            result["description"] = "Chat spam protection insufficient"
        else:
            result["severity"] = "INFO"
            result["description"] = "Chat rate limiting working"
        
        return result
    
    def test_timing_side_channel(self, test_uuids: List[str]) -> Dict[str, Any]:
        """
        Test for timing side-channels in authentication/validation
        
        If server takes different time for valid vs invalid UUIDs,
        this can leak information
        """
        logger.info("Testing timing side-channel")
        
        timing_results = {}
        
        for uuid in test_uuids:
            # Build movement packet with this UUID
            packet = bytearray()
            packet.extend(VarInt.encode(0x01))
            packet.extend(bytes.fromhex(uuid.replace('-', '')))
            packet.extend(struct.pack('>fff', 100.0, 64.0, 100.0))
            packet.extend(struct.pack('>fff', 0.0, 0.0, 0.0))
            packet.extend(struct.pack('>ff', 0.0, 0.0))
            packet.extend(struct.pack('>B', 0))
            packet.extend(struct.pack('>I', 1000))
            
            # Measure multiple times for accuracy
            times = []
            for _ in range(10):
                _, response_time = self._send_and_measure(bytes(packet))
                times.append(response_time)
                time.sleep(0.1)
            
            timing_results[uuid] = {
                "mean": statistics.mean(times),
                "stdev": statistics.stdev(times) if len(times) > 1 else 0,
                "samples": times
            }
        
        # Analyze if there's significant timing difference
        means = [v["mean"] for v in timing_results.values()]
        
        result = {
            "test": "timing_side_channel",
            "uuid_count": len(test_uuids),
            "timing_results": timing_results,
            "mean_of_means": statistics.mean(means),
            "stdev_of_means": statistics.stdev(means) if len(means) > 1 else 0
        }
        
        # If standard deviation of means is high, there might be a timing leak
        if len(means) > 1 and statistics.stdev(means) > 0.1:
            result["severity"] = "LOW"
            result["bounty_potential"] = "$500-$2,000"
            result["description"] = "Potential timing side-channel detected"
        else:
            result["severity"] = "INFO"
            result["description"] = "No obvious timing side-channel"
        
        return result


def main():
    """Example usage"""
    import argparse
    import json
    
    parser = argparse.ArgumentParser(description="Hytale Timing & Rate Limit Fuzzer")
    parser.add_argument("--host", default="localhost", help="Target host")
    parser.add_argument("--port", type=int, default=5520, help="Target port")
    parser.add_argument("--test", required=True, 
                       choices=["entity_flood", "movement_rate", "chat_rate", "timing"],
                       help="Test to run")
    parser.add_argument("--rate", type=int, help="Packets per second for rate tests")
    parser.add_argument("--max", type=int, help="Maximum entities for flood test")
    
    args = parser.parse_args()
    
    fuzzer = RateLimitFuzzer(host=args.host, port=args.port)
    
    if args.test == "entity_flood":
        max_entities = args.max or 1000
        result = fuzzer.test_entity_spawn_flooding(max_entities=max_entities)
    elif args.test == "movement_rate":
        rate = args.rate or 50
        result = fuzzer.test_movement_rate_limit(packets_per_second=rate)
    elif args.test == "chat_rate":
        rate = args.rate or 10
        result = fuzzer.test_chat_rate_limit(messages_per_second=rate)
    elif args.test == "timing":
        test_uuids = [
            "deadbeef000000000000000000000001",
            "deadbeef000000000000000000000002",
            "00000000000000000000000000000000",
            "ffffffffffffffffffffffffffffffff"
        ]
        result = fuzzer.test_timing_side_channel(test_uuids)
    
    print("\n=== Test Results ===")
    print(json.dumps(result, indent=2))
    
    if result.get("severity") in ["CRITICAL", "HIGH", "MEDIUM"]:
        print(f"\n🎯 POTENTIAL VULNERABILITY FOUND!")
        print(f"Severity: {result['severity']}")
        print(f"Bounty Potential: {result.get('bounty_potential', 'Unknown')}")


if __name__ == "__main__":
    main()
