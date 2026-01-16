#!/usr/bin/env python3
"""
State Machine Tracer for Hytale Game Phase Transitions
Track and validate game state transitions to find race conditions

⚠️ IMPORTANT NOTES ON VULNERABILITY DETECTION:
- This module provides HEURISTIC/INDICATIVE detection of potential issues
- "VULNERABILITY" results mean: "This transition violates the state machine"
- They do NOT prove the server accepts these invalid transitions
- Always verify findings against a live server before reporting

This module provides:
1. Game phase state tracking
2. Valid/invalid transition detection (based on spec)
3. State diagram visualization
4. Race condition identification (heuristic)
"""

from enum import Enum
from typing import List, Dict, Any, Optional, Tuple
from dataclasses import dataclass, field
import time
import json
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class GamePhase(Enum):
    """Game phases based on PROTOCOL_SPEC.md"""
    INIT = 0
    AUTH_PENDING = 1
    AUTH_COMPLETE = 2
    LOBBY = 3
    DAY = 4
    VOTING = 5
    NIGHT = 6
    END = 7
    DEAD = 8
    
    @classmethod
    def from_string(cls, phase_str: str):
        """Convert string to GamePhase enum"""
        phase_map = {
            "INIT": cls.INIT,
            "AUTH_PENDING": cls.AUTH_PENDING,
            "AUTH_COMPLETE": cls.AUTH_COMPLETE,
            "LOBBY": cls.LOBBY,
            "DAY": cls.DAY,
            "VOTING": cls.VOTING,
            "NIGHT": cls.NIGHT,
            "END": cls.END,
            "DEAD": cls.DEAD
        }
        return phase_map.get(phase_str.upper(), cls.INIT)


@dataclass
class StateTransition:
    """Represents a state transition event"""
    from_phase: GamePhase
    to_phase: GamePhase
    packet_type: str
    timestamp: float
    valid: bool
    player_id: Optional[str] = None
    additional_data: Dict[str, Any] = field(default_factory=dict)
    
    def to_dict(self):
        return {
            "from_phase": self.from_phase.name,
            "to_phase": self.to_phase.name,
            "packet_type": self.packet_type,
            "timestamp": self.timestamp,
            "valid": self.valid,
            "player_id": self.player_id,
            "additional_data": self.additional_data
        }


class StateTracer:
    """
    Track valid/invalid phase transitions → Race Condition Finder
    
    Based on PROTOCOL_SPEC.md and SERVER_ARCHITECTURE.md from existing docs
    """
    
    def __init__(self):
        self.transitions: List[StateTransition] = []
        self.invalid_transitions: List[StateTransition] = []
        self.current_phase = GamePhase.INIT
        
        # Define valid state machine based on protocol spec
        self.valid_transitions = {
            GamePhase.INIT: [GamePhase.AUTH_PENDING],
            GamePhase.AUTH_PENDING: [GamePhase.AUTH_COMPLETE, GamePhase.INIT],
            GamePhase.AUTH_COMPLETE: [GamePhase.LOBBY, GamePhase.INIT],
            GamePhase.LOBBY: [GamePhase.DAY, GamePhase.INIT],
            GamePhase.DAY: [GamePhase.VOTING, GamePhase.DEAD, GamePhase.END],
            GamePhase.VOTING: [GamePhase.NIGHT, GamePhase.END],
            GamePhase.NIGHT: [GamePhase.DAY, GamePhase.END, GamePhase.DEAD],
            GamePhase.END: [GamePhase.LOBBY, GamePhase.INIT],
            GamePhase.DEAD: [GamePhase.LOBBY, GamePhase.INIT]
        }
        
        # Packets that should NOT be allowed in certain phases
        self.phase_restricted_packets = {
            GamePhase.INIT: ["MOVEMENT", "CHAT", "BLOCK_INTERACTION"],
            GamePhase.AUTH_PENDING: ["MOVEMENT", "BLOCK_INTERACTION"],
            GamePhase.LOBBY: ["BLOCK_INTERACTION"],  # Can't place blocks in lobby
            GamePhase.VOTING: ["MOVEMENT", "BLOCK_INTERACTION"],  # Frozen during voting
        }
    
    def trace(self, from_phase: GamePhase, to_phase: GamePhase, 
              packet_type: str, player_id: Optional[str] = None,
              additional_data: Optional[Dict[str, Any]] = None) -> StateTransition:
        """
        Log each phase change and validate it
        
        Args:
            from_phase: Current phase
            to_phase: New phase
            packet_type: Packet that triggered transition
            player_id: Optional player identifier
            additional_data: Any extra data to store
            
        Returns:
            StateTransition object with validation result
        """
        timestamp = time.time()
        is_valid = self._is_valid_transition(from_phase, to_phase)
        
        transition = StateTransition(
            from_phase=from_phase,
            to_phase=to_phase,
            packet_type=packet_type,
            timestamp=timestamp,
            valid=is_valid,
            player_id=player_id,
            additional_data=additional_data or {}
        )
        
        self.transitions.append(transition)
        
        if not is_valid:
            self.invalid_transitions.append(transition)
            logger.warning(
                f"⚠️ INVALID TRANSITION: {from_phase.name} → {to_phase.name} "
                f"via {packet_type}"
            )
        else:
            logger.info(
                f"✓ Valid transition: {from_phase.name} → {to_phase.name} "
                f"via {packet_type}"
            )
        
        self.current_phase = to_phase
        return transition
    
    def _is_valid_transition(self, from_phase: GamePhase, to_phase: GamePhase) -> bool:
        """Validate if transition is allowed according to state machine"""
        allowed_phases = self.valid_transitions.get(from_phase, [])
        return to_phase in allowed_phases
    
    def check_packet_validity(self, phase: GamePhase, packet_type: str) -> bool:
        """
        Check if a packet type is valid in the current phase
        Used for race condition detection
        """
        restricted = self.phase_restricted_packets.get(phase, [])
        is_valid = packet_type not in restricted
        
        if not is_valid:
            logger.warning(
                f"⚠️ INVALID PACKET: {packet_type} sent during {phase.name} phase"
            )
        
        return is_valid
    
    def detect_race_conditions(self) -> List[Dict[str, Any]]:
        """
        Analyze transitions to find potential race conditions
        
        Race conditions occur when:
        1. Invalid transitions are accepted by server
        2. Packets sent during phase transitions
        3. Multiple rapid state changes
        """
        race_conditions = []
        
        # Check for invalid transitions (potential vulnerabilities)
        for transition in self.invalid_transitions:
            race_conditions.append({
                "type": "INVALID_TRANSITION_ACCEPTED",
                "severity": "HIGH",
                "from_phase": transition.from_phase.name,
                "to_phase": transition.to_phase.name,
                "packet_type": transition.packet_type,
                "timestamp": transition.timestamp,
                "description": f"Server accepted invalid transition from {transition.from_phase.name} to {transition.to_phase.name}"
            })
        
        # Check for rapid consecutive transitions (potential race window)
        for i in range(len(self.transitions) - 1):
            t1 = self.transitions[i]
            t2 = self.transitions[i + 1]
            
            time_diff = t2.timestamp - t1.timestamp
            
            # Transitions within 50ms might indicate race condition window
            if time_diff < 0.05:
                race_conditions.append({
                    "type": "RAPID_TRANSITION",
                    "severity": "MEDIUM",
                    "transition_1": f"{t1.from_phase.name} → {t1.to_phase.name}",
                    "transition_2": f"{t2.from_phase.name} → {t2.to_phase.name}",
                    "time_diff_ms": time_diff * 1000,
                    "description": f"Rapid transitions within {time_diff*1000:.2f}ms - potential race window"
                })
        
        # Check for unexpected phase loops
        for i in range(len(self.transitions) - 2):
            t1, t2, t3 = self.transitions[i:i+3]
            
            # A → B → A pattern might indicate state confusion
            if (t1.from_phase == t3.to_phase and 
                t1.to_phase == t2.from_phase and
                t2.to_phase == t3.from_phase):
                race_conditions.append({
                    "type": "PHASE_LOOP",
                    "severity": "MEDIUM",
                    "pattern": f"{t1.from_phase.name} → {t1.to_phase.name} → {t1.from_phase.name}",
                    "description": "Detected phase loop - possible state confusion"
                })
        
        return race_conditions
    
    def get_statistics(self) -> Dict[str, Any]:
        """Get statistics about traced transitions"""
        total = len(self.transitions)
        invalid = len(self.invalid_transitions)
        
        phase_counts = {}
        for transition in self.transitions:
            phase_counts[transition.to_phase.name] = phase_counts.get(transition.to_phase.name, 0) + 1
        
        return {
            "total_transitions": total,
            "invalid_transitions": invalid,
            "invalid_percentage": (invalid / total * 100) if total > 0 else 0,
            "phase_distribution": phase_counts,
            "current_phase": self.current_phase.name
        }
    
    def plot_graph(self, output_file: str = "state_machine.dot"):
        """
        Generate state machine diagram using DOT language
        
        Can be rendered with: dot -Tpng state_machine.dot -o state_machine.png
        Or viewed online at: http://www.webgraphviz.com/
        """
        dot_content = ["digraph StateMachine {"]
        dot_content.append('  rankdir=LR;')
        dot_content.append('  node [shape=circle];')
        dot_content.append('')
        
        # Add all unique transitions
        transitions_set = set()
        for t in self.transitions:
            edge = (t.from_phase.name, t.to_phase.name, t.packet_type)
            transitions_set.add(edge)
        
        # Generate edges
        for from_phase, to_phase, packet_type in transitions_set:
            # Find if this transition was ever invalid
            is_invalid = any(
                t.from_phase.name == from_phase and 
                t.to_phase.name == to_phase and 
                not t.valid
                for t in self.transitions
            )
            
            style = "dashed" if is_invalid else "solid"
            color = "red" if is_invalid else "black"
            
            dot_content.append(
                f'  {from_phase} -> {to_phase} '
                f'[label="{packet_type}", style={style}, color={color}];'
            )
        
        dot_content.append('}')
        
        with open(output_file, 'w') as f:
            f.write('\n'.join(dot_content))
        
        logger.info(f"State diagram saved to {output_file}")
        logger.info(f"Render with: dot -Tpng {output_file} -o state_machine.png")
        
        return '\n'.join(dot_content)
    
    def export_to_json(self, filename: str):
        """Export all transitions to JSON for analysis"""
        data = {
            "statistics": self.get_statistics(),
            "transitions": [t.to_dict() for t in self.transitions],
            "invalid_transitions": [t.to_dict() for t in self.invalid_transitions],
            "race_conditions": self.detect_race_conditions()
        }
        
        with open(filename, 'w') as f:
            json.dump(data, f, indent=2)
        
        logger.info(f"Exported transitions to {filename}")
    
    def simulate_race_condition_test(self, test_scenario: str) -> Dict[str, Any]:
        """
        Simulate common race condition scenarios
        
        ⚠️ IMPORTANT: These are HEURISTIC tests that detect POTENTIAL vulnerabilities
        based on state machine logic. They do NOT prove the server accepts invalid
        transitions - that requires actual server testing.
        
        Use these results as:
        - Indicators of where to focus testing
        - Test cases for fuzzer development
        - Validation of fuzzer logic
        
        To confirm a vulnerability, you MUST test against a live server.
        
        Test scenarios:
        - "auth_during_game": Send auth packet while in-game
        - "block_during_voting": Send block placement during voting phase
        - "movement_during_transition": Send movement during phase change
        """
        logger.info(f"Simulating race condition test: {test_scenario}")
        
        if test_scenario == "auth_during_game":
            # Simulate: Already authenticated, try to auth again
            self.trace(GamePhase.DAY, GamePhase.AUTH_COMPLETE, "AUTH_RESPONSE")
            return {
                "scenario": test_scenario,
                "description": "Attempted re-authentication while in-game",
                "expected": "Should be rejected",
                "result": "VULNERABILITY" if self.transitions[-1].valid else "SECURE"
            }
        
        elif test_scenario == "block_during_voting":
            # Simulate: Try to place block during voting phase
            is_valid = self.check_packet_validity(GamePhase.VOTING, "BLOCK_INTERACTION")
            return {
                "scenario": test_scenario,
                "description": "Attempted block placement during voting",
                "expected": "Should be rejected",
                "result": "VULNERABILITY" if is_valid else "SECURE"
            }
        
        elif test_scenario == "movement_during_transition":
            # Simulate: Send movement packet exactly during phase change
            self.trace(GamePhase.DAY, GamePhase.VOTING, "PHASE_CHANGE")
            time.sleep(0.001)  # Tiny delay
            is_valid = self.check_packet_validity(GamePhase.VOTING, "MOVEMENT")
            return {
                "scenario": test_scenario,
                "description": "Sent movement packet during phase transition",
                "expected": "Should be rejected or queued",
                "result": "VULNERABILITY" if is_valid else "SECURE"
            }
        
        return {"scenario": test_scenario, "result": "UNKNOWN"}


def main():
    """Example usage and testing"""
    import argparse
    
    parser = argparse.ArgumentParser(description="Hytale State Machine Tracer")
    parser.add_argument("--test", help="Run test scenario", 
                       choices=["auth_during_game", "block_during_voting", "movement_during_transition"])
    parser.add_argument("--export", help="Export results to JSON file")
    parser.add_argument("--graph", help="Generate state diagram DOT file")
    
    args = parser.parse_args()
    
    tracer = StateTracer()
    
    # Example: Trace a normal game flow
    logger.info("=== Tracing normal game flow ===")
    tracer.trace(GamePhase.INIT, GamePhase.AUTH_PENDING, "AUTH_REQUEST")
    tracer.trace(GamePhase.AUTH_PENDING, GamePhase.AUTH_COMPLETE, "AUTH_RESPONSE")
    tracer.trace(GamePhase.AUTH_COMPLETE, GamePhase.LOBBY, "JOIN_LOBBY")
    tracer.trace(GamePhase.LOBBY, GamePhase.DAY, "GAME_START")
    tracer.trace(GamePhase.DAY, GamePhase.VOTING, "PHASE_CHANGE")
    tracer.trace(GamePhase.VOTING, GamePhase.NIGHT, "VOTE_COMPLETE")
    tracer.trace(GamePhase.NIGHT, GamePhase.END, "GAME_END")
    
    # Example: Try invalid transition
    logger.info("\n=== Testing invalid transition ===")
    tracer.trace(GamePhase.END, GamePhase.DAY, "INVALID_PACKET")  # Should be invalid
    
    # Statistics
    stats = tracer.get_statistics()
    print(f"\n=== Statistics ===")
    print(f"Total transitions: {stats['total_transitions']}")
    print(f"Invalid transitions: {stats['invalid_transitions']}")
    print(f"Current phase: {stats['current_phase']}")
    
    # Race condition detection
    race_conditions = tracer.detect_race_conditions()
    if race_conditions:
        print(f"\n=== Detected Race Conditions ===")
        for rc in race_conditions:
            print(f"Type: {rc['type']} | Severity: {rc['severity']}")
            print(f"  {rc['description']}")
    
    # Run test scenario if specified
    if args.test:
        print(f"\n=== Running test scenario: {args.test} ===")
        result = tracer.simulate_race_condition_test(args.test)
        print(json.dumps(result, indent=2))
    
    # Export if requested
    if args.export:
        tracer.export_to_json(args.export)
    
    # Generate graph if requested
    if args.graph:
        tracer.plot_graph(args.graph)


if __name__ == "__main__":
    main()
