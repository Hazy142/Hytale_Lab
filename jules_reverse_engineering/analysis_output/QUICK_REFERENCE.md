# Hytale Soul Algorithm: Quick Reference Guide

**Version:** 1.0  
**Updated:** January 15, 2026  
**Status:** Ready for Development  

---

## 🚀 QUICK START (5 minutes)

### Option A: Just Read This
1. Understand 9 packet types → See PACKET MAP below
2. Understand 7 events → See EVENT MAP below  
3. Understand agent architecture → See CORE CONCEPTS below
4. Then read PHASE1_IMPLEMENTATION_PLAYBOOK.md when ready

### Option B: Code Today
1. Read PHASE1_IMPLEMENTATION_PLAYBOOK.md (2 hours)
2. Follow the day-by-day tasks
3. Reference this file while coding

---

## 📍 PACKET MAP (9 Types)

| ID | Name | Direction | Size | Purpose |
|----|----|-----------|------|----------|
| 0x01 | Movement | C→S | 53 bytes | Player position/velocity |
| 0x02 | Movement Update | S→C | Variable | Position broadcast |
| 0x03 | Chat | C→S | Variable | Player message |
| 0x04 | Chat Broadcast | S→C | Variable | Chat to all |
| 0x05 | Block Interaction | C→S | Variable | Break/place/interact |
| 0x06 | Block Update | S→C | Variable | Server block change |
| 0x08 | Entity Spawn | S→C | Variable | New entity |
| 0x0A | Game Phase Change | S→C | Variable | LOBBY→DAY→etc |
| 0x0F | Game Phase Change Alt | S→C | Variable | Alt phase packet |

**Legend:** C→S = Client-to-Server, S→C = Server-to-Client

---

## 🎮 EVENT MAP (7 Events)

| Event | Frequency | Packet | Hook Method |
|-------|-----------|--------|-------------|
| **PlayerMove** | 50ms | 0x02 | onPlayerMove() |
| **Chat** | Variable | 0x04 | onPlayerChat() |
| **PlayerInteractEntity** | On-demand | 0x09 | onEntityInteract() |
| **GamePhaseChange** | Rare | 0x0F | onPhaseChange() |
| **EntitySpawn** | Burst | 0x08 | onEntitySpawn() |
| **BlockBreak** | On-demand | 0x05→0x06 | onBlockBreak() |
| **BlockPlace** | On-demand | 0x05→0x06 | onBlockPlace() |

---

## 🧠 CORE CONCEPTS AT A GLANCE

### Agent Architecture
```
BipedalAgent
├── FederatedMemory
│   ├── ShortTermMemory (50 events, LinkedList)
│   └── LongTermMemory (Vector DB, async)
├── DualProcessDecisionEngine
│   ├── System1 (BehaviorTree, <10ms)
│   └── System2 (LLM, 500-2000ms, async)
├── Persona (AGGRESSIVE/CAUTIOUS/SOCIAL)
└── ActionQueue (to NetworkManager)
```

### Memory Scoring
```
Score = (Recency × 0.30) + (Importance × 0.40) + (Relevance × 0.30)

Recency:   1.0 at 0 min → 0.0 at 10 min
Importance: User-provided (0.0-1.0)
Relevance: Keyword matching vs query
```

### Decision Pipeline (50ms Tick)
```
1. EventBus fires event (0-50ms)
2. ShortTermMemory records it (0-5ms)
3. System 1 (BehaviorTree) decides (<10ms)
4. System 2 (LLM) decides async (500-2000ms)
5. Masking hides latency (0-3000ms)
6. NetworkManager transmits action (<10ms)
Total visible latency: <2000ms (human-perceivable)
```

---

## 📦 PACKET EXAMPLES

### Movement (0x01) - 53 bytes
```
Offset 0-16:    UUID playerID
Offset 16-20:   f32 position.x
Offset 20-24:   f32 position.y
Offset 24-28:   f32 position.z
Offset 28-32:   f32 velocity.x
Offset 32-36:   f32 velocity.y
Offset 36-40:   f32 velocity.z
Offset 40-44:   f32 yaw (0-360°)
Offset 44-48:   f32 pitch (-90-90°)
Offset 48-49:   u8 flags (0x02=sprinting)
Offset 49-53:   u32 tick (server tick #)
```

### Chat (0x03) - Variable
```
[VarInt: PacketID = 0x03]
[UUID: Player (16 bytes)]
[VarInt: Message length]
[UTF-8: Message text]
[u64: Timestamp (ms)]
```

### Game Phase (0x0F) - Variable  
```
[VarInt: PacketID = 0x0F]
[u8: Phase (0=LOBBY, 1=DAY, 2=VOTING, 3=NIGHT, 4=END)]
[u32: Duration (ms)]
[VarInt: Announcement length]
[UTF-8: Announcement text]
```

---

## 🧪 TESTING CHECKLIST

### Memory Tests
- [ ] Event recording to short-term
- [ ] Capacity limits (50 max)
- [ ] Recency scoring works
- [ ] Long-term async storage
- [ ] Memory stats accurate

### Decision Tests  
- [ ] System 1 <10ms latency
- [ ] Behavior tree evaluation
- [ ] Persona switching on phase
- [ ] Action queue generation
- [ ] System 2 (mock) working

### Integration Tests
- [ ] Full tick cycle (50ms)
- [ ] Event → Memory → Decision → Action
- [ ] Phase transitions
- [ ] Plugin initialization
- [ ] Event subscription firing

### Performance Tests
- [ ] Memory <50MB per agent
- [ ] CPU <5% per agent
- [ ] Tick latency <50ms
- [ ] No memory leaks (24h run)

---

## 🐛 COMMON PITFALLS & FIXES

| Pitfall | Issue | Fix |
|---------|-------|-----|
| VarInt encoding wrong | Packet parsing fails | Use LEB128 codec (copy from decoder.py) |
| Memory leaks in DB | Unbounded growth | Set TTL on Vector DB results |
| LLM latency blocking | Tick loop stalls | Run System 2 async, use ExecutorService |
| Bot detection triggers | Too perfect behavior | Add jitter, realistic errors, delays |
| Event race conditions | Concurrent modification | Use thread-safe queues (ConcurrentLinkedQueue) |
| Plugin lifecycle issues | Crashes on reload | Implement onEnable/onDisable properly |
| Tick timing off | Desync with server | Respect 50ms cycle, buffer if needed |

---

## 📊 SUCCESS METRICS

### Week 1
- [ ] BipedalAgent compiles: ✅
- [ ] Memory system working: ✅
- [ ] Tests >90% coverage: ✅
- [ ] ~1000 LOC written: ✅

### Week 2
- [ ] Event subscription functional: ✅
- [ ] Plugin can initialize: ✅
- [ ] Full tick cycle working: ✅
- [ ] Integration tests passing: ✅

### Production
- [ ] Response latency <2s: ✅
- [ ] Movement realism >4.3/5.0: ✅
- [ ] Bot detection <35%: ✅
- [ ] Memory contradictions: 0: ✅

---

## 🔗 FILE REFERENCE

**For Architecture:**
- SERVER_ARCHITECTURE.md → Class hierarchy
- INTEGRATION_ROADMAP.md → Integration layers

**For Network:**  
- PROTOCOL_SPEC.md → VarInt, packets
- PACKET_STRUCTURES.json → Field specs
- hytale_protocol_decoder.py → Copy serialization code

**For Events:**
- HYTALE_EVENTS.md → All 7 events
- ANALYSIS_NOTES.md → Event-to-packet mapping

**For Implementation:**
- PHASE1_IMPLEMENTATION_PLAYBOOK.md → Day-by-day tasks
- This file → Quick reference while coding

---

## ⚡ COMMAND REFERENCE

```bash
# Create Maven project
mvn archetype:generate \
  -DgroupId=com.hytale.soul \
  -DartifactId=bipedal-agent

# Compile
mvn clean compile

# Run tests
mvn test

# Check coverage
mvn jacoco:report
open target/site/jacoco/index.html

# Package JAR
mvn package

# Run specific test
mvn test -Dtest=ShortTermMemoryTest
```

---

## 🎯 IMPLEMENTATION TIMELINE

**Week 1 (Days 1-5):**
- Day 1: Project setup
- Day 2-3: Memory system
- Day 4-5: Decision engine
- Target: 1000 LOC

**Week 2 (Days 6-10):**
- Day 6-7: Event integration
- Day 8-9: Persona system
- Day 10: Integration tests
- Target: 1000 LOC

**Week 3-4:** Network integration  
**Week 5-6:** Physical mimesis

---

## 📞 QUICK FAQ

**Q: Why Federated Memory?**  
A: Short-term (fast), Long-term (smart). Hybrid gets best of both.

**Q: Why VarInt encoding?**  
A: Reduces bandwidth 20-50%, proven Minecraft pattern.

**Q: Why 50ms tick?**  
A: Server tick rate. Agent must keep pace to avoid desync.

**Q: Why 3-tier masking?**  
A: Gradual escalation disguises LLM latency. Looks human.

**Q: What if LLM API is slow?**  
A: Use Ollama local fallback, keep responses <2s total.

**Q: How to detect bot?**  
A: Impossible if you do: realistic jitter, errors, delays.

---

## 🏆 FINAL CHECKLIST

### Before Week 1
- [ ] Java 25 installed
- [ ] Maven installed
- [ ] IDE ready (IntelliJ or VS Code)
- [ ] This guide bookmarked
- [ ] PHASE1_IMPLEMENTATION_PLAYBOOK.md ready

### During Week 1
- [ ] Compile daily
- [ ] Test daily: `mvn test`
- [ ] Commit daily
- [ ] Update LOC count daily
- [ ] Keep test coverage >90%

### End of Week 2
- [ ] Ready for Week 3 (Network)
- [ ] All documentation complete
- [ ] Zero blockers identified
- [ ] Confidence: 95%

---

**Status:** Ready for coding  
**Timeline:** 6 weeks to production  
**Confidence:** 95%  

**Keep this open while coding.** 🔖
