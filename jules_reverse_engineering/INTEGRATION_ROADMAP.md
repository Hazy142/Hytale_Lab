# BipedalAgent Integration Roadmap

## Overview
This document maps each BipedalAgent component from the master specification
to specific Hytale API hooks and network packets.

## Integration Layers

### Layer 1: Event Subscription (Server-Side)

```java
// BipedalAgentPlugin.java
public class BipedalAgentPlugin implements HytalePlugin {
    private BipedalAgent agent;

    @Override
    public void onEnable() {
        agent = new BipedalAgent(...);

        // Hook: Perception updates
        server.getEventBus().subscribe(PlayerMoveEvent.class, event -> {
            agent.updatePerception(event);
        });

        server.getEventBus().subscribe(ChatEvent.class, event -> {
            agent.onPlayerSpeak(event.getPlayerID(), event.getMessage());
        });

        server.getEventBus().subscribe(GamePhaseChangeEvent.class, event -> {
            agent.onPhaseChange(event.getNewPhase());
        });

        // Start agent tick loop (50ms)
        scheduler.scheduleRepeating(agent::tick, 50, TimeUnit.MILLISECONDS);
    }
}
```

### Layer 2: Packet Transmission (Agent Actions)

```java
// BipedalAgent.executeActions()
private void executeActions() {
    List<Action> actions = decisionEngine.getQueuedActions();

    for (Action action : actions) {
        switch (action.type) {
            case MOVE:
                sendMovementPacket(action.targetPosition);
                break;
            case CHAT:
                sendChatPacket(action.message);
                break;
            case INTERACT:
                sendBlockInteractionPacket(action.blockPos);
                break;
        }
    }
}

private void sendMovementPacket(Vector3f targetPos) {
    // Build packet 0x01
    ByteBuffer buffer = ByteBuffer.allocate(53);
    buffer.put(VarInt.encode(0x01));  // Packet ID
    buffer.put(agentPlayerID.toBytes());  // UUID
    buffer.putFloat(targetPos.x);
    buffer.putFloat(targetPos.y);
    buffer.putFloat(targetPos.z);
    // ... rest of fields

    networkManager.sendPacket(buffer.array());
}
```

### Layer 3: Component Mapping

| BipedalAgent Component | Hytale Event | Network Packet | Latency |
|------------------------|--------------|----------------|---------|
| **updatePerception()** | PlayerMoveEvent | 0x02 (Movement Update) | 10-50ms |
| **onPlayerSpeak()** | ChatEvent | 0x04 (Chat Broadcast) | 20-100ms |
| **onPhaseChange()** | GamePhaseChangeEvent | 0x0F (Phase Change) | N/A (instant) |
| **executeMovement()** | N/A (action) | 0x01 (Movement Input) | 10-50ms |
| **executeChat()** | N/A (action) | 0x03 (Chat Message) | 20-100ms |
| **ProceduralPathfinding** | N/A (internal) | 0x01 (repeated) | 50ms/step |
| **MouseLookController** | N/A (internal) | 0x01 (rotation fields) | 50ms |
| **LatencyMasking Tier 1** | N/A (internal) | emote packets | 0-500ms |
| **LatencyMasking Tier 2** | N/A (internal) | 0x03 (filler chat) | 500-1500ms |
| **FederatedMemory.record()** | All events | N/A (internal) | <5ms |

### Layer 4: Critical Path Analysis

**Perception → Decision → Execution Loop (50ms cycle)**

```
[t=0ms]    PlayerMoveEvent fires
[t=5ms]    BipedalAgent.updatePerception() called
[t=10ms]   FederatedMemory.recordEvent() completes
[t=15ms]   DualProcessDecisionEngine.evaluate()
           ├─ System 1 (BehaviorTree): 5ms
           └─ System 2 (LLM): async (500-2000ms)
[t=20ms]   If System 1 decision: executeActions()
[t=25ms]   sendMovementPacket() → NetworkManager
[t=30ms]   Packet transmitted to server
[t=40ms]   Server processes, broadcasts to other clients
[t=50ms]   Next tick begins
```

**LLM Decision Path (with masking)**

```
[t=0ms]    Strategic decision needed
[t=5ms]    LatencyMaskingOrchestrator.initiate()
[t=10ms]   Tier 1: Play "thinking" emote
[t=100ms]  LLM inference started (async)
[t=500ms]  Tier 2: Send filler chat "Hmm..."
[t=1500ms] Tier 3: Loop idle animations
[t=2000ms] LLM inference complete
[t=2010ms] Replace filler chat with real response
[t=2020ms] Stop animation loop
```

### Layer 5: Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                  HYTALE SERVER                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐                    ┌──────────────┐      │
│  │  EventBus    │───────────────────→│ BipedalAgent │      │
│  └──────────────┘                    └──────────────┘      │
│         │                                     │              │
│         │ PlayerMoveEvent                     │              │
│         │ ChatEvent                           │              │
│         │ GamePhaseChangeEvent                │              │
│         │                                     │              │
│         ▼                                     ▼              │
│  ┌──────────────────────────────────────────────────┐      │
│  │         BipedalAgent.updatePerception()          │      │
│  │                                                   │      │
│  │  ├─ FederatedMemory.recordEvent()               │      │
│  │  ├─ DualProcessDecisionEngine.evaluate()        │      │
│  │  │  ├─ System 1 (BehaviorTree) <10ms            │      │
│  │  │  └─ System 2 (LLM) async 500-2000ms          │      │
│  │  └─ LatencyMaskingOrchestrator (if async)       │      │
│  └──────────────────────────────────────────────────┘      │
│         │                                     │              │
│         ▼                                     ▼              │
│  ┌──────────────────────────────────────────────────┐      │
│  │         BipedalAgent.executeActions()            │      │
│  │                                                   │      │
│  │  ├─ Movement: sendPacket(0x01)                  │      │
│  │  ├─ Chat: sendPacket(0x03)                      │      │
│  │  ├─ Interaction: sendPacket(0x05)               │      │
│  │  └─ ProceduralPathfinding.step()                │      │
│  └──────────────────────────────────────────────────┘      │
│         │                                                    │
│         ▼                                                    │
│  ┌──────────────┐                                           │
│  │NetworkManager│──────────────────→ UDP:5520               │
│  └──────────────┘                                           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   OTHER CLIENTS     │
                    │  (Humans + Agents)  │
                    └─────────────────────┘
```

### Layer 6: Implementation Checklist

- [ ] **Phase 1:** Core BipedalAgent class (Week 1-2)
  - [ ] FederatedMemoryRepository.java
  - [ ] DualProcessDecisionEngine.java
  - [ ] BipedalAgent.java (master orchestrator)

- [ ] **Phase 2:** Event subscription (Week 2)
  - [ ] HytalePlugin interface implementation
  - [ ] EventBus subscription for all relevant events
  - [ ] 50ms tick loop scheduler

- [ ] **Phase 3:** Packet transmission (Week 3)
  - [ ] PacketBuilder utility (VarInt, Vector3f serialization)
  - [ ] sendMovementPacket() implementation
  - [ ] sendChatPacket() implementation
  - [ ] NetworkManager integration

- [ ] **Phase 4:** Physical Mimesis (Week 4-5)
  - [ ] ProceduralPathfinding.java (Lazy A*)
  - [ ] MouseLookController.java (saccadic)
  - [ ] IdleAnimationController.java (fidgeting)
  - [ ] Integration with packet transmission

- [ ] **Phase 5:** Latency Masking (Week 5)
  - [ ] LatencyMaskingOrchestrator.java
  - [ ] 3-tier masking implementation
  - [ ] Emote/animation API integration

- [ ] **Phase 6:** Testing & Optimization (Week 6)
  - [ ] Unit tests for each component
  - [ ] Integration test with live server
  - [ ] Latency profiling
  - [ ] Bot detection rate measurement

### Layer 7: API Surface Requirements

**Minimum Hytale API needed:**

```java
// Event system
interface EventBus {
    <T extends Event> void subscribe(Class<T> eventType, Consumer<T> handler);
}

// Network
interface NetworkManager {
    void sendPacket(byte[] data);
}

// Entity control
interface NPCPlayer {
    UUID getID();
    Vector3f getPosition();
    void setPosition(Vector3f pos);
    void setRotation(float yaw, float pitch);
    void playEmote(String emoteName);
    void chat(String message);
    void editMessage(int messageID, String newText);
}

// World interaction
interface WorldManager {
    Block getBlock(Vector3i pos);
    void setBlock(Vector3i pos, BlockType type);
    List<Entity> getEntitiesInRadius(Vector3f center, float radius);
}

// Scheduler
interface TaskScheduler {
    void scheduleRepeating(Runnable task, long interval, TimeUnit unit);
}
```
