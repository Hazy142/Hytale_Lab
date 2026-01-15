# Hytale Event System Documentation

This file describes the event system used by the BipedalAgent to interact with the Hytale Server. It complements the `SERVER_ARCHITECTURE.md` file.

## Confirmed Events

### Event: PlayerMoveEvent
- **Category:** Player Action
- **Trigger Condition:** Player position changes
- **Frequency:** Every tick (50ms)
- **Fields:**
  - `playerID`: UUID
  - `oldPosition`: Vector3f
  - `newPosition`: Vector3f
  - `velocity`: Vector3f
  - `yaw`: float
  - `pitch`: float
- **Related Packets:** 0x01 (Movement Input)
- **BipedalAgent Hook:** `updatePerception()`

### Event: ChatEvent
- **Category:** Communication
- **Trigger Condition:** Player sends a chat message
- **Frequency:** Variable
- **Fields:**
  - `playerID`: UUID
  - `message`: String
  - `timestamp`: u64
- **Related Packets:** 0x03 (Chat Message)
- **BipedalAgent Hook:** `onPlayerSpeak()`

### Event: PlayerInteractEntityEvent
- **Category:** Interaction
- **Trigger Condition:** Player clicks on an entity
- **Frequency:** On-demand
- **Fields:**
  - `playerID`: UUID
  - `targetEntityID`: u32
  - `interactionType`: u8 (0=Interact, 1=Attack)
- **Related Packets:** 0x09 (Player Action)
- **BipedalAgent Hook:** `onInteraction()`

### Event: GamePhaseChangeEvent
- **Category:** Game State
- **Trigger Condition:** Server transitions game phase
- **Frequency:** Rare (Round-based)
- **Fields:**
  - `oldPhase`: u8
  - `newPhase`: u8
- **Related Packets:** 0x0A (Game Phase Change)
- **BipedalAgent Hook:** `onPhaseChange()`

### Event: EntitySpawnEvent
- **Category:** World
- **Trigger Condition:** Entity is created/loaded
- **Frequency:** Burst (Chunk load)
- **Fields:**
  - `entityID`: u32
  - `type`: u16
  - `position`: Vector3f
- **Related Packets:** 0x08 (Entity Spawn)
- **BipedalAgent Hook:** `updateWorldModel()`

## Inferred Events

### Event: BlockBreakEvent
- **Category:** World Interaction
- **Trigger Condition:** Player breaks a block
- **Fields:** `playerID`, `blockPosition`, `blockType`
- **BipedalAgent Hook:** `observeWorldChange()`

### Event: BlockPlaceEvent
- **Category:** World Interaction
- **Trigger Condition:** Player places a block
- **Fields:** `playerID`, `blockPosition`, `blockType`
- **BipedalAgent Hook:** `observeWorldChange()`
