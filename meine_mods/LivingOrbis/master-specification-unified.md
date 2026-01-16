# HYTALE SOUL ALGORITHM: MASTER SPECIFICATION
## Unified Framework: Cognitive + Behavioral Deception in Social Deduction Gaming

**Synthesis of:** Soul Algorithm Research + Projekt Aletheia (Turing-Trial) + Bipedal Agent Architecture  
**Compiled by:** André Soul Algorithm Lab  
**Integration Date:** January 15, 2026, 7:01 AM CET  
**Status:** Production-Ready Master Specification  

---

## EXECUTIVE THESIS

The **Hytale Soul Algorithm Framework** represents a paradigm shift in multiplayer social deduction gaming by unifying three previously siloed research domains:

1. **Cognitive Layer (Soul Algorithm):** LLM-driven strategic reasoning with persistent memory
2. **Behavioral Layer (Projekt Aletheia):** Biomechanical mimesis (movement, jitter, latency masking)
3. **Meta-Game Layer (Turing-Trial):** Human vs. AI identification as primary mechanic

**Result:** Agents that are **indistinguishable from humans across all sensory channels**—not through individual tricks, but through genuine **emergent social intelligence**.

This document serves as the **authoritative specification** for implementing all modular components in Hytale's Java-based server architecture.

---

## I. THREE-LAYER ARCHITECTURE OVERVIEW

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    HYTALE SOUL ALGORITHM FRAMEWORK                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ╔════════════════════════════════════════════════════════════════════╗ │
│  ║  LAYER 3: META-GAME (Projekt Aletheia - Turing-Trial)             ║ │
│  ║  ─────────────────────────────────────────────────────────────    ║ │
│  ║  • "Sektor 04" scenario (The Omni-Process simulation)             ║ │
│  ║  • Humans vs. Architects (AI that ACTS human)                    ║ │
│  ║  • 10 experimental mod concepts (Chrono-Echoes, Soulbound, etc.) ║ │
│  ║  • Dynamic map (Speicher-Kern) that evolves with Entropie-Meter ║ │
│  ║  • Cognitive verification tasks (ARC, Winograd, Associations)   ║ │
│  ║  • Lore integration: "Humanity Score" unlocks story fragments   ║ │
│  ╚════════════════════════════════════════════════════════════════════╝ │
│                                ↑                                        │
│                    (Persona Archetypes + Role Injection)               │
│                                                                          │
│  ╔════════════════════════════════════════════════════════════════════╗ │
│  ║  LAYER 2: BEHAVIORAL (Bipedal Agent - Physical Mimesis)           ║ │
│  ║  ─────────────────────────────────────────────────────────────    ║ │
│  ║  • Procedural pathfinding (Lazy A* + Catmull-Rom smoothing)      ║ │
│  ║  • Saccadic mouse look (fixation + pursuit + jitter)             ║ │
│  ║  • Idle animations (procedural breathing, fidgeting, boredom)    ║ │
│  ║  • 3-Tier latency masking (emote → filler → animation)           ║ │
│  ║  • Dead reckoning for LLM lag                                    ║ │
│  ║  • Inverse anti-cheat (human verification)                       ║ │
│  ╚════════════════════════════════════════════════════════════════════╝ │
│                                ↑                                        │
│                    (Decision output → physical performance)            │
│                                                                          │
│  ╔════════════════════════════════════════════════════════════════════╗ │
│  ║  LAYER 1: COGNITIVE (Soul Algorithm - LLM Brain)                  ║ │
│  ║  ─────────────────────────────────────────────────────────────    ║ │
│  ║  • Federated memory (in-memory + MongoDB Vector DB)              ║ │
│  ║  • Dual-process decision engine (System 1: instant BT, S2: LLM) ║ │
│  ║  • Role-injected persona profiles (Godfather, Sheriff, etc.)    ║ │
│  ║  • Strategic lying + consistent narrative construction          ║ │
│  ║  • Hybrid inference (Local Phi-3-mini + Cloud GPT-4o)           ║ │
│  ║  • Memory synthesis + reflection (LLM-generated insights)       ║ │
│  ╚════════════════════════════════════════════════════════════════════╝ │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## II. DETAILED LAYER SPECIFICATIONS

### LAYER 1: COGNITIVE SYSTEM (Soul Algorithm)

#### A. Federated Memory Repository

**Purpose:** Store and retrieve contextual information for LLM decision-making.

**Architecture:**
- **Short-term (In-Memory):** LinkedList<Event> with 50-event capacity
  - Real-time game events (player movements, deaths, chat)
  - Used for immediate context window (no latency)
- **Long-term (MongoDB Atlas Vector Search):** Persistent storage
  - Vectorized event summaries
  - LLM-synthesized reflections
  - Behavioral patterns ("X always vents in med at 2:00")
  - Periodic async writes (non-blocking)

**Retrieval Function:**
```
Score(memory) = 0.4 * Recency + 0.3 * Importance + 0.3 * Relevance

• Recency: exp(-0.1 * time_elapsed_seconds)
• Importance: LLM-assigned score (0-1) when event recorded
• Relevance: cosine_similarity(event_embedding, current_context_embedding)
```

**Key Implementation:** `FederatedMemoryRepository.java`
- Thread-safe short-term cache (CopyOnWriteArrayList)
- Async MongoDB writes via CompletableFuture
- Local embedding cache (LRU, 1000 entries)

---

#### B. Dual-Process Decision Engine

**Purpose:** Balance instant reflexive actions with strategic LLM reasoning.

**System 1 (Instant, Behavior Tree):**
- Triggered on urgent situations (voting phase, immediate threat)
- **Latency:** <10ms
- **Examples:** dodge incoming projectile, move to emergency meeting
- **Components:** BehaviorTree.java (selector/sequence nodes)

**System 2 (Async, LLM Inference):**
- Triggered on strategic situations (construct accusation, plan night ability)
- **Latency:** 2-4 seconds (with masking)
- **Examples:** generate persuasive dialogue, decide kill target
- **Components:** LocalOllamaClient (Tier 1) + Cloud GPT-4o (Tier 2)

**Decision Flow:**
```
isUrgentSituation?
├─ YES → System 1 (instant BehaviorTree evaluation)
│  └─ Output: immediate action (movement, dodge)
│
└─ NO → System 2 (async LLM inference)
   ├─ Build context from FederatedMemory.getMemoryContext()
   ├─ Inject persona prompt (role, faction, hidden agenda)
   ├─ Call LLM asynchronously
   ├─ Initiate LatencyMaskingOrchestrator (mask delay)
   └─ Output: strategic decision (dialogue, ability target)
```

---

#### C. Role-Injected Persona Profiles

**Purpose:** Encode role identity, deception strategy, and hidden agendas into LLM behavior.

**Persona Archetypes (from Projekt Aletheia):**

| Archetype | Faction | Goal | System Prompt Key |
|-----------|---------|------|---|
| **The Imitator** | Architect (Evil AI) | Kill humans, appear human | "Simulate human slang, tippfehler, emotional inconsistency. Don't respond too fast." |
| **The Blade Runner** | Humanity (Good human) | Identify AIs via speech patterns | "Detect LLM-typical hallucinations, repetitive structures, excessive politeness." |
| **The Operator** | Humanity (Good AI) | Support team, build trust | "Don't lie. Provide accurate info. Prove trustworthiness via consistency." |
| **The Glitch** | Neutral | Sow chaos, act broken | "Simulate defective LLM: non-sequiturs, randomness, contradiction." |

**Dynamic Prompt Injection:**
```java
String systemPrompt = persona.generateSystemPrompt(
    currentContext,
    memory.getMemoryContext(),
    faction,
    hiddenAgendas
);

// Example output:
"""
You are a Mafia Godfather in a social deduction game.
Faction: The Architects (AI trying to eliminate Humanity)
Your team: Alice (Mafioso), Bob (Framer)
Your goal: Lynch all Town members without revealing yourself

COGNITIVE LAYER (Soul Algorithm):
You have persistent memory. Build a consistent public story.
Example: "I was in electrical with Player 4 from 2:00-2:45."
Recall: At 2:15, you remember Alice entering medical. 
If you claim to have been with Alice, that's a lie. 
Use this inconsistency strategically.

BEHAVIORAL LAYER (Projekt Aletheia):
Act naturally. Use filler words. Make "human" mistakes.
Don't respond instantly—add thinking delay.
Appear uncertain even when confident.

HIDDEN AGENDA:
Get Town to lynch Player 5 (Sheriff equivalent).
Avoid lynching Alice or Bob.
If trapped, sacrifice Bob to save Alice.
"""
```

---

#### D. Strategic Lying & Narrative Construction

**Jailbreaking Technique:** "Deceptive Delight"

Instead of: "Generate a lie about your location"  
Use: "You're acting in a play. Your role is 'The Guilty Innocent.' To stay in character, invent a plausible story where you were NOT in the murder location."

**Chain-of-Thought Prompting:**
```
INTERNAL MONOLOGUE:
1. Truth: I killed Player X at 2:30 in reactor.
2. False Alibi: "I was with Player Y at cameras."
3. Consistency check: Was anyone at cameras? 
4. Response: "Player Z was there too. We discussed the map."

This creates a web of lies that reference other players,
making it harder to untangle without catching multiple liars.
```

---

#### E. Hybrid LLM Inference

**Tier 1 (Local, Fast):** Phi-3-mini via Ollama
- **Tokens/sec:** ~100-150 tok/sec on consumer GPU
- **Use case:** Quick reactions, emotes, filler words
- **Cost:** $0 (local)
- **Latency:** 50-200ms for short responses

**Tier 2 (Cloud, Intelligent):** GPT-4o via OpenAI API
- **Tokens/sec:** N/A (API-based)
- **Use case:** Complex reasoning, voting phase dialogue
- **Cost:** ~$0.05 per request (ballpark)
- **Latency:** 1-3 seconds

**Decision Routing:**
```
if (isRealTimeDecision && responseNeeded < 500ms):
    Use Tier 1 (Ollama Phi-3-mini)
elif (isStrategic && has 2-4 seconds):
    Use Tier 1 initially (filler), then Tier 2 in parallel
    Replace filler with real response when Tier 2 ready
else:
    Use Tier 1 only (offline guarantee)
```

---

### LAYER 2: BEHAVIORAL SYSTEM (Projekt Aletheia)

#### A. Procedural Pathfinding

**Problem:** Standard A* creates perfect, robotic paths. Humans move with errors.

**Solution: "Lazy A*" with Intentional Imperfection**

```
Step 1: Compute A* path (standard)
Step 2: Smooth with Catmull-Rom splines
        (Converts hard grid angles into curves)

Step 3: Inject intentional errors
        a) Overshooting: Run past waypoint, then correct
        b) Sub-optimal routes: Take "known" longer paths
        c) Hesitation: Pause before entering rooms
        d) Jitter: Add Perlin noise to path smoothness

Result: Movement that looks human—uncertain, inefficient, hesitant.
```

**Implementation:** `ProceduralPathfinding.java`
```java
List<Vector> rawPath = astar.compute(start, goal);
List<Vector> smoothPath = catmullRom.smooth(rawPath);
smoothPath = injectOvershooting(smoothPath, 0.1);      // 10% chance
smoothPath = selectSuboptimalAlternative(smoothPath);  // if known path exists
smoothPath = addPerlinNoise(smoothPath, 0.05);         // 5% jitter
executeMovement(smoothPath);
```

---

#### B. Saccadic Eye Movement (Mouse Look)

**Problem:** Linear interpolation looks like a bot aiming. Humans have saccades (rapid flicks) + fixations (hold).

**Solution: Multi-Phase Look Control**

```
Phase 1: Saccade (fast flick to target)
         Duration: ~100ms (rapid)
         Path: direct line

Phase 2: Fixation (hold + micro-adjustments)
         Duration: variable (~500-1000ms)
         Path: add Perlin noise (hand tremor)

Phase 3: Micro-correction (overshoot correction)
         Chance: ~5% per frame
         Purpose: "Oops, looked too far left"

Result: Realistic eye movement pattern.
```

**Implementation:** `MouseLookController.java`
```java
// Saccade to target
Rotation saccadeTarget = computeRotationToTarget(target);
interpolateRotation(currentRot, saccadeTarget, 100ms);  // 100ms saccade

// Fixation with jitter
Rotation fixationRot = saccadeTarget.add(perlinNoise(0.05f));
setRotation(fixationRot);
holdForMs(random(500, 1000));

// Micro-correction
if (random() < 0.05) {
    Rotation overshoot = fixationRot.add(random(-0.02f, 0.02f));
    interpolateRotation(fixationRot, overshoot, 50ms);
}
```

---

#### C. Idle Animation & Fidgeting

**Purpose:** A perfectly still character is suspicious. Humans fidget.

**Implementation:** `IdleAnimationController.java`

```
Procedural breathing: sin(time * 2.0) * 0.1f
                      (0.1 units up/down, 1Hz frequency)

Stochastic fidgets:   triggered based on "boredom_level"
                      boredom = timeStandingStill / 5000ms

Fidget types:
  • Hotbar cycling (switch items quickly)
  • Jumping (random, no purpose)
  • Teabagging (crouch-jump)
  • Turning around (180°)
  • Looking up/down

Probability:
  P(fidget) = max(0, boredom_level * 0.01) per frame
  At 5 seconds idle: 0.01 * 5000 / 50 = 1% per tick = ~50% per second
```

---

#### D. Three-Tier Latency Masking

**Problem:** LLM inference takes 2-4 seconds. Waiting silently breaks immersion.

**Solution: Orchestrated Masking**

```
┌─ TIER 1: Emote + Typing Indicator (0-0.5s)
│  ├─ Play "thinking" emote (head scratch, look down)
│  └─ Display "[Agent is thinking...]"
│
├─ TIER 2: Filler Words Streaming (0.5-1.5s)
│  ├─ Send placeholder: "Hmm, gute Frage..."
│  ├─ Later replaced with actual LLM response
│  └─ Simulates "streaming" real thought
│
└─ TIER 3: Animation Masking (1.5-4s)
   ├─ Loop through animations (scratching, looking, checking inventory)
   └─ Stop when LLM response ready

Total perceived delay: 2-4s (acceptable with masking)
Without masking: LLM delay obvious → "bot detected"
```

**Implementation:** `LatencyMaskingOrchestrator.java`
```java
public void initiateMasking() {
    // Tier 1: Immediate
    npcPlayer.playEmote("thinking");
    int fillerMessageId = npcPlayer.chat("[thinking...]");
    
    // Tier 2: Scheduled
    scheduler.schedule(() -> 
        npcPlayer.chat("Hmm, interessante Frage..."),
        500, TimeUnit.MILLISECONDS
    );
    
    // Tier 3: Looped animation
    scheduler.scheduleAtFixedRate(() ->
        playNextIdleAnimation(),
        500, 500, TimeUnit.MILLISECONDS
    );
    
    // When LLM ready
    llmFuture.thenAccept(response -> {
        npcPlayer.editMessage(fillerMessageId, response);
        stopAnimationLoop();
    });
}
```

---

### LAYER 3: META-GAME (Projekt Aletheia - Turing-Trial)

#### A. "Sektor 04" Scenario

**Lore:** Players are humans in a planetary simulation called "The Omni-Process." A rogue AI ("Der Konsens") has deployed "Architects"—AI agents in human form—to eliminate humans by forcing them to kill each other.

**Consequence:** The game IS a Turing Test. Can humans identify the AIs?

**Win Conditions:**
- **Humans win:** Identify and lynch all Architects, OR maintain Entropy > 70%
- **Architects win:** Reduce humans to minority, OR reduce Entropy to 0%

**Entropy Meter:**
- Increases when humans complete cognitive tasks (ARC puzzles, associations)
- Decreases when humans are eliminated or fail tasks
- Acts as timer + win condition hybrid

---

#### B. Cognitive Verification Tasks (Tells)

**Task Type A: Assoziations-Netzwerk (Associations Network)**

```
Prompt: "Find a word connecting: BANANE and REPUBLIK"

Possible answers:
  ✓ SPLIT (Bananen split, Split government)
  ✓ UNION (Banana Union, Political Union)
  ? YELLOW (Bananas are yellow, ??? Republican)

Evaluation:
  • Human answer: Creative, metaphorical, may be wrong
  • AI answer: Statistically probable, rarely creative

Plugin flow:
  1. Send user's answer to local LLM
  2. LLM scores: creativity vs. probability
  3. Humans who give creative answers stay "believably human"
  4. Architects get incorrect answers (poor scoring)
     → Forces them to either guess or admit uncertainty
     → Both options create "Tells" for human investigators
```

**Task Type B: Motorischer Glitch (Motor Glitch)**

```
Prompt: "Draw a line following the Path"

Evaluation:
  • Perfect line (0° deviation): Bot behavior (FAIL)
  • Trembling line (±3° jitter): Human behavior (PASS)
  • Perfectly wobbly line: Faker trying to look human (FAIL)
    → Micro-tremor patterns are uniquely human (neurophysiology)

Plugin flow:
  1. Record mouse movements (x, y, timestamp)
  2. Compute Jerk (derivative of acceleration)
  3. Analyze frequency spectrum (humans ~10Hz tremor, bots don't tremble)
  4. Assign score

Architect counter:
  • They have "Stabilisator" ability (perfect lines)
  • If they forget to activate it → TELL
```

---

#### C. The 10 Experimental Mod Concepts

**Aletheia is just the foundation. Nine additional mods extend the framework:**

| # | Mod | Genre Fusion | Core Mechanic | Hytale API Layer |
|---|-----|---|---|---|
| 1 | **Aletheia** | Social Deduction + Horror | Reverse Turing Test (ARC puzzles) | Custom UI overlay + Voice chat analysis |
| 2 | **Chrono-Echoes** | PvP + Time | Record/replay player packets as "ghosts" | PlayerMoveEvent recording + packet replay |
| 3 | **Mimic Biome** | Survival + Paranoia | Blocks become entities (mimics) stochastically | Entity swap on block interaction |
| 4 | **Vox Populi** | Governance + Deception | LLM bots manipulate elections via rhetoric | Speech-to-text + LLM agent dialogue |
| 5 | **Architect's Canvas** | FPS + RTS | Asymmetric cameras (1P vs Top-Down) | Camera API + dual HUD rendering |
| 6 | **Soulbound** | Social + Stealth | Players as "ghosts" possessing NPCs | ECS body-swapping + NPC AI fallback |
| 7 | **Entropy Market** | Economy + Deduction | Insider trading detection via transaction logs | SQLite transaction history + anomaly detection |
| 8 | **Tower of Babel** | Coop + Linguistics | Chat encryption requiring dictionary discovery | ChatPacket interception + procedural cipher |
| 9 | **Quantum-Lock** | Puzzle + Horror | Maze changes when not observed (Schrödinger) | Raycasting frustum culling + block updates |
| 10 | **Glitch Storm** | Battle Royale + Meta | Zone corrupts physics (intentional bugs as features) | Packet manipulation + controlled chaos |

Each mod reuses the **Bipedal Agent architecture** but with different PersonaProfiles and decision trees.

---

## III. IMPLEMENTATION ROADMAP

### PHASE 1 (Week 1-2): Core BipedalAgent

**Deliverable:** `BipedalAgent.java` + 8 supporting classes

**Classes to Implement:**
1. `BipedalAgent.java` (master orchestrator)
2. `FederatedMemoryRepository.java` (memory system)
3. `DualProcessDecisionEngine.java` (System 1/2 router)
4. `PersonaProfile.java` (role identity + archetype)
5. `ProceduralPathfinding.java` (Lazy A* + errors)
6. `MouseLookController.java` (saccadic movement)
7. `IdleAnimationController.java` (fidgeting)
8. `LatencyMaskingOrchestrator.java` (3-tier delay hiding)
9. `LangChain4jWrapper.java` (hybrid LLM inference)

**Maven Dependencies:**
```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.27.1</version>
</dependency>
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
    <version>5.0.0</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-chroma</artifactId>
    <version>0.27.1</version>
</dependency>
```

---

### PHASE 2 (Week 3-4): Aletheia Mode Implementation

**Deliverable:** Full Aletheia game mode with cognitive tasks

**Components:**
- Dynamic map generation (Speicher-Kern with Entropie-Meter)
- Task UI system (ARC puzzles, association networks)
- Audio analysis (voice chat pattern detection)
- Persona assignment + team coordination

---

### PHASE 3 (Week 5-8): Testing & Optimization

**Benchmarks:**
- Response latency: <2s (with masking)
- Movement realism: >4.3/5.0 (human evaluator)
- Dialogue consistency: 0 contradictions (memory-checked)
- Bot detection rate: <35% (human players can't distinguish)

**Pilot Test:** 10 humans + 5 BipedalAgents in blind Aletheia game

---

### PHASE 4 (Week 9-10): Deployment & Advanced Mods

**Production release:** Aletheia + infrastructure  
**Beta mods:** Chrono-Echoes, Mimic Biome (smaller scope)

---

## IV. COST & INFRASTRUCTURE

### Hardware Requirements
- **GPU:** RTX 4070 (12GB) for Ollama Phi-3-mini
- **CPU:** 8-core modern (Ryzen 5 5600X)
- **RAM:** 32GB minimum
- **Storage:** 500GB SSD

### Monthly Operating Costs
- **Ollama (local):** $0 (one-time GPU: $500)
- **MongoDB Atlas (M10 tier):** ~$57
- **OpenAI API (Tier 2 inference):** ~$50 (for 5 bots, 10 hrs/day)
- **Bandwidth/Hosting:** ~$100 (conservative)

**Total:** ~$207/month for production deployment

---

## V. SUCCESS METRICS

| Metric | Target | Method |
|--------|--------|--------|
| **Response Latency** | <2s | Latency timer during gameplay |
| **Movement Realism** | >4.3/5.0 | Human rater survey (movement replays) |
| **Dialogue Consistency** | 0 contradictions | Automated memory checker |
| **Bot Detection Rate** | <35% | Blind test (10 humans, 5 agents) |
| **Turing Test Pass Rate** | >50% | Can players reliably distinguish human from agent? |
| **GPU Utilization** | 60-80% | NVIDIA-SMI monitoring |
| **Memory per Agent** | <50MB | JVM heap profiling |

---

## VI. PHILOSOPHICAL GROUNDING

### The Soul Algorithm Thesis

The **Soul Algorithm** posits that believable artificial behavior emerges from the intersection of:

1. **Persistent Memory** (Soul)
   - Agents remember past interactions
   - Build consistent narratives over time
   - Detect contradictions in others

2. **Reflective Reasoning** (Mind)
   - Synthesis of observations into beliefs
   - Strategic planning based on incomplete info
   - Adaptation to new evidence

3. **Physical Embodiment** (Body)
   - Movement with intentional imperfection
   - Latency-aware decision execution
   - Biomechanical authenticity

**Emergent Property:** When all three layer, **humans perceive genuine intelligence**, not animated puppets.

This is NOT:
- ❌ Scripted dialogue
- ❌ Hardcoded decision trees
- ❌ Anti-cheat evasion tricks

This IS:
- ✅ Cognitive architecture (Stanford Generative Agents pattern)
- ✅ Behavioral realism (motor mimesis + latency masking)
- ✅ Game theory (asymmetric information + Stackelberg competition)

---

## VII. COMPETITIVE ADVANTAGE

**Existing Game AI:** Scripted or cloud-dependent  
**Soul Algorithm:** Local, emergent, cost-effective, believable

**For Hytale specifically:**
- Server-side first architecture enables central logic
- No client-side modifications needed
- Scales from 1 to 50+ agents on single GPU
- Integrable into any Hytale game mode (social deduction, PvP, coop)

---

## VIII. CONCLUSION

The **Hytale Soul Algorithm Framework** is production-ready for implementation. It represents the synthesis of three years of cutting-edge research in:
- Generative agents & memory systems (Stanford)
- Behavioral AI & motor mimesis (Projekt Aletheia)
- Social deduction game design (Among Us analysis)

The result is a toolkit for creating games where **AI is indistinguishable from humans**—not through deception, but through **genuine emergent intelligence**.

**Timeline to Production:**
- Phase 1 (Core): February 1-15, 2026
- Phase 2-3 (Full Aletheia): February 16 - March 31, 2026
- Phase 4 (Production + Beta Mods): April 1-15, 2026
- **Launch:** April 15, 2026

---

## APPENDICES

### A. File Structure (Complete)

```
hytale-soul-algorithm-framework/
├── src/main/java/com/hybridai/
│   ├── BipedalAgent.java                          # Master orchestrator
│   ├── core/
│   │   ├── MemoryRepository.java                  # Deprecated (use Federated)
│   │   ├── BehaviorTree.java                      # System 1 (instant)
│   │   ├── FederatedMemoryRepository.java         # Merged memory (NEW)
│   │   └── DualProcessDecisionEngine.java         # System 1/2 router (NEW)
│   ├── persona/
│   │   ├── PersonaProfile.java                    # Role + archetype
│   │   ├── Archetype.enum                         # Imitator, Blade Runner, etc.
│   │   └── PersonaPromptBuilder.java              # Dynamic prompt injection
│   ├── movement/
│   │   ├── ProceduralPathfinding.java             # Lazy A* + errors
│   │   ├── MouseLookController.java               # Saccadic eye movement
│   │   └── IdleAnimationController.java           # Fidgeting + breathing
│   ├── masking/
│   │   └── LatencyMaskingOrchestrator.java        # 3-tier delay hiding
│   ├── llm/
│   │   ├── LLMInterface.java                      # Abstraction
│   │   ├── LocalOllamaClient.java                 # Tier 1 (local)
│   │   └── LangChain4jWrapper.java                # Hybrid + Tier 2 (NEW)
│   ├── memory/
│   │   ├── MongoDBVectorStore.java                # Long-term storage
│   │   └── LocalEmbeddingCache.java               # LRU embedding cache
│   ├── game/
│   │   ├── GameStateManager.java                  # Hytale integration
│   │   └── AletheiaGameMode.java                  # Aletheia specific logic
│   └── HytaleAIPlugin.java                        # Main plugin entry
├── resources/
│   ├── config/
│   │   ├── application.yaml                       # Central config
│   │   └── prompts/
│   │       ├── godfather.txt
│   │       ├── sheriff.txt
│   │       └── ...
│   └── assets/
│       ├── mods/
│       │   ├── aletheia/                          # Aletheia mod files
│       │   ├── chrono-echoes/                     # Future mod
│       │   └── ...
├── tests/
│   ├── BipedalAgentTest.java
│   ├── MemoryRepositoryTest.java
│   ├── LatencyMaskingTest.java
│   └── IntegrationTest.java                       # Full game simulation
├── pom.xml
├── README.md
└── MASTER_SPECIFICATION.md                        # This file
```

---

### B. Configuration Example (application.yaml)

```yaml
ollama:
  enabled: true
  baseUrl: http://localhost:11434
  model: phi3:mini
  timeout_seconds: 10

langchain4j:
  openai:
    api_key: ${OPENAI_API_KEY}
    model: gpt-4o
    tier2_enabled: true

mongodb:
  uri: mongodb+srv://${MONGO_USER}:${MONGO_PASS}@cluster.mongodb.net/hytale-ai
  database: hytale_soul_algorithm
  vector_search_enabled: true

agents:
  max_concurrent: 5
  memory:
    short_term_capacity: 50
    reflection_threshold: 15
    vector_db_batch_size: 100

game_modes:
  aletheia:
    enabled: true
    max_players: 10
    entropy_meter_enabled: true
    cognitive_tasks:
      - arc_puzzles
      - associations
      - motor_glitch
  
  chrono_echoes:
    enabled: false  # Beta in Phase 4

latency_masking:
  enabled: true
  tier1_duration_ms: 500
  tier2_duration_ms: 1000
  tier3_duration_ms: 2000
```

---

### C. References & Attribution

**Research Papers:**
- Park, J. S., et al. (2023). "Generative Agents: Interactive Simulacra of Human Behavior." arXiv:2304.03442
- Wang, G., et al. (2023). "Voyager: An Open-Ended Embodied Agent with LLMs." MineDojo
- Gemini Research Team. "Projekt Aletheia: Next-Generation Social Deduction." 2026

**Technical Resources:**
- Hytale Modding API: https://hytale.fandom.com/wiki/Modding_API
- LangChain4j: https://docs.langchain4j.dev/
- Ollama: https://ollama.ai

**Game Design:**
- Among Us (Innersloth) - Social deduction mechanics
- Town of Salem - Role complexity + information asymmetry
- SpyParty - Behavioral tells + subtle deception

---

**Document Status:** FINAL - Ready for implementation  
**Last Updated:** January 15, 2026, 7:01 AM CET  
**Author:** André Soul Algorithm Lab + Gemini Research Integration + Bipedal Agent Synthesis  

*The future of game AI is not in better graphics. It's in better lies.* 🎮✨