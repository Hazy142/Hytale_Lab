# Analysis Notes: Hytale Soul Algorithm Specification

## 1. Game Systems
- **Movement:**
  - Standard movement (A* pathfinding).
  - Procedural smoothing (Catmull-Rom splines).
  - "Lazy A*" with intentional imperfections (overshooting, hesitation).
  - Saccadic eye movement (mouse look) with fixation and jitter.
- **Cognitive:**
  - Federated Memory (Short-term in-memory + Long-term Vector DB).
  - Dual-Process Decision Engine (System 1: Instant/Behavior Tree, System 2: LLM).
- **Social Deduction (Meta-Game):**
  - "Projekt Aletheia" / "Sektor 04".
  - Factions: Humans vs. Architects.
  - Cognitive Verification Tasks (ARC puzzles, Association networks, Motor glitches).
  - Entropy Meter (Game timer/win condition).
- **Communication:**
  - Chat (Text-based).
  - Voice chat analysis (mentioned in future mods).
  - Emotes/Animations.
- **Latency Masking:**
  - 3-Tier system: Emote -> Filler Text -> Animation loop.

## 2. Entity Types
- **Agents/NPCs:**
  - `BipedalAgent` (The core implementation).
  - Persona Archetypes: The Imitator, The Blade Runner, The Operator, The Glitch.
- **Humans:**
  - Players participating in the Turing Trial.
- **Mobs/World Entities:**
  - Standard Hytale entities (Kweebec, Trork, etc. - inferred context).

## 3. Game Phases
- **LOBBY:** Pre-game assembly.
- **STARTING:** Initialization.
- **DAY:** General gameplay, task completion.
- **VOTING:** Discussion and elimination phase.
- **NIGHT:** Action phase (kills, abilities).
- **END:** Win condition resolution.

## 4. Memory Structures
- **FederatedMemoryRepository:**
  - **Short-term:** `LinkedList<Event>` (50-event capacity).
  - **Long-term:** MongoDB Atlas Vector Search.
  - **Retrieval:** Score based on Recency, Importance, Relevance.

## 5. Decision Systems
- **System 1 (Instant):**
  - BehaviorTree.java.
  - Latency: <10ms.
  - Uses: Dodge, emergency meeting.
- **System 2 (Strategic):**
  - LLM Inference (Local Phi-3-mini + Cloud GPT-4o).
  - Latency: 2-4 seconds (masked).
  - Uses: Dialogue, kill planning, narrative construction.

## 6. Network Components (Inferred)
- **Events:**
  - `PlayerMoveEvent`: Tracking movement for "Chrono-Echoes" or general perception.
  - `ChatEvent` / `PlayerChatEvent`: Input for LLM and "Tower of Babel" mod.
  - `EntitySpawnEvent`: Perception of new entities.
  - `PlayerInteractEntityEvent`: Trigger for agent focus/reaction.
  - `GamePhaseChangeEvent`: Transition logic.
- **Packets:**
  - **Movement:** Position (x,y,z), Rotation (yaw, pitch), Velocity.
  - **Chat:** Message content, sender ID.
  - **Interaction:** Block placement/breaking, entity clicking.
  - **Metadata:** Skin/Appearance updates (Masking).
