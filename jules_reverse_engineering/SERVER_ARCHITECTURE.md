# Hytale Server Architecture & Event Documentation

## Server Architecture

The Hytale server architecture is designed as a modular, event-driven system, facilitating deep integration for plugins like the **BipedalAgent**. The following hierarchy outlines the core components relevant to the Soul Algorithm integration.

```
HytaleServer
├── EventBus
│   └── EventManager (Handles @Subscribe registrations)
├── NetworkManager
│   ├── PacketHandler (Routes incoming 0x.. packets)
│   └── ConnectionManager (Manages QUIC streams/Sessions)
├── GameStateManager
│   ├── WorldManager (Chunks, Blocks, Physics)
│   ├── PlayerManager (Connected clients, UUID mapping)
│   └── EntityManager (NPCs, Mobs, Items)
└── PluginManager (Lifecycle management: onEnable/onDisable)
```

### Core Interfaces

- **`IEventListener`**: Marker interface for classes containing `@Subscribe` methods.
- **`IPacketHandler`**: Interface for decoding and processing raw packet data.
- **`IGameState`**: Interface representing different phases of the game loop (LOBBY, DAY, NIGHT).
- **`IEntity`**: Base interface for all world objects, extended by `IPlayer` and `INPC`.

### BipedalAgent Integration Mapping

- **EventBus**: The `BipedalAgent` subscribes here to receive `PlayerMoveEvent` and `ChatEvent` for its **Perception Layer**.
- **EntityManager**: The `BipedalAgent` controls an `INPC` instance (or `NpcEntity`) to execute movement and animation for its **Behavioral Layer**.
- **NetworkManager**: The agent may bypass high-level APIs to inject raw packets (e.g., for specific latency simulation or "glitch" effects) via the **Action Layer**.

---

## Hytale Event Documentation

The following events are critical for the BipedalAgent's perception and state tracking.

### Event: PlayerMoveEvent
- **Category:** Player Action
- **Trigger Condition:** Player position or rotation changes significantly (> epsilon).
- **Frequency:** High (potentially every tick/50ms).
- **Fields:**
  - `playerID`: UUID (The moving player)
  - `oldPosition`: Vector3f
  - `newPosition`: Vector3f
  - `velocity`: Vector3f
  - `yaw`: float
  - `pitch`: float
- **Related Packets:** 0x01 (Movement Input)
- **BipedalAgent Hook:** `updatePerception()` - Updates the internal spatial model of the agent.

### Event: ChatEvent
- **Category:** Communication
- **Trigger Condition:** A player sends a chat message.
- **Frequency:** Variable (Low to Medium).
- **Fields:**
  - `playerID`: UUID (The sender)
  - `message`: String (UTF-8 content)
  - `timestamp`: long
- **Related Packets:** 0x03 (Chat Message)
- **BipedalAgent Hook:** `onPlayerSpeak()` - Feeds into the `FederatedMemory` and triggers the `DualProcessDecisionEngine` (System 2) if addressed.

### Event: PlayerInteractEntityEvent
- **Category:** Interaction
- **Trigger Condition:** A player right-clicks (interacts) with an entity.
- **Frequency:** Low (On demand).
- **Fields:**
  - `playerID`: UUID (The interactor)
  - `targetEntityID`: int (The agent/NPC)
  - `interactionType`: Enum (INTERACT, ATTACK)
- **Related Packets:** 0x09 (Player Action) / 0x05 (Block/Entity Interaction variant)
- **BipedalAgent Hook:** `focusAttention()` - Triggers immediate System 1 reaction (turning to look) or System 2 dialogue initiation.

### Event: GamePhaseChangeEvent
- **Category:** Meta-Game
- **Trigger Condition:** The server game state transitions (e.g., Lobby -> Day).
- **Frequency:** Very Low.
- **Fields:**
  - `oldPhase`: GamePhase (Enum)
  - `newPhase`: GamePhase (Enum)
  - `duration`: int (ms)
- **Related Packets:** 0x0F (Game Phase Change)
- **BipedalAgent Hook:** `onPhaseChange()` - Switches the active `PersonaProfile` strategy (e.g., from "Friendly Lobby" to "Deceptive Night").

### Event: EntitySpawnEvent
- **Category:** World State
- **Trigger Condition:** A new entity enters the loaded chunks/world.
- **Frequency:** Medium.
- **Fields:**
  - `entityID`: int
  - `entityType`: EntityType
  - `location`: Vector3f
- **Related Packets:** 0x08 (Entity Spawn)
- **BipedalAgent Hook:** `updatePerception()` - Adds the entity to the tracked world model.
