# Phase 1 Implementation Playbook: Week 1-2

**Implementation Period:** Week 1-2 (Days 1-10)  
**Target Lines of Code:** 1000+ LOC  
**Target Test Coverage:** >90%  
**Target Memory:** <50MB per agent  
**Status:** Ready to code  

---

## 📅 WEEK 1: CORE COMPONENTS (Days 1-5)

### Day 1: Project Setup & Data Classes

**Goals:**
- ✅ Maven project created
- ✅ Directory structure defined
- ✅ Base data classes coded
- ✅ First compile successful

**Tasks:**

**Task 1.1: Create Maven Project (30 min)**
```bash
mvn archetype:generate \
  -DgroupId=com.hytale.soul \
  -DartifactId=bipedal-agent \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false

cd bipedal-agent
```

**Task 1.2: Update pom.xml (45 min)**
```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.hytale.soul</groupId>
  <artifactId>bipedal-agent</artifactId>
  <version>0.1.0</version>
  <name>BipedalAgent - Soul Algorithm</name>
  
  <properties>
    <java.version>25</java.version>
    <maven.compiler.source>25</maven.compiler.source>
    <maven.compiler.target>25</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  
  <dependencies>
    <!-- Core -->
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>5.10.0</version>
      <scope>test</scope>
    </dependency>
    
    <!-- Logging -->
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
      <version>2.0.9</version>
    </dependency>
    <dependency>
      <groupId>ch.qos.logback</groupId>
      <artifactId>logback-classic</artifactId>
      <version>1.4.11</version>
    </dependency>
    
    <!-- JSON -->
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>2.16.0</version>
    </dependency>
    
    <!-- MongoDB (for long-term memory) -->
    <dependency>
      <groupId>org.mongodb</groupId>
      <artifactId>mongodb-driver-sync</artifactId>
      <version>5.0.0</version>
    </dependency>
  </dependencies>
  
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
        <configuration>
          <source>25</source>
          <target>25</target>
        </configuration>
      </plugin>
      
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.1.0</version>
      </plugin>
      
      <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.10</version>
        <executions>
          <execution>
            <goals>
              <goal>prepare-agent</goal>
            </goals>
          </execution>
          <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
              <goal>report</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

**Task 1.3: Create Directory Structure (30 min)**
```
src/
├── main/
│   ├── java/com/hytale/soul/
│   │   ├── agent/
│   │   │   ├── BipedalAgent.java
│   │   │   ├── Persona.java
│   │   │   └── AgentState.java
│   │   ├── memory/
│   │   │   ├── FederatedMemoryRepository.java
│   │   │   ├── MemoryEvent.java
│   │   │   ├── ShortTermMemory.java
│   │   │   └── LongTermMemory.java
│   │   ├── decision/
│   │   │   ├── DualProcessDecisionEngine.java
│   │   │   ├── System1DecisionMaker.java
│   │   │   ├── System2DecisionMaker.java
│   │   │   └── BehaviorTree.java
│   │   ├── action/
│   │   │   ├── DecisionAction.java
│   │   │   └── ActionQueue.java
│   │   └── util/
│   │       ├── Vector3f.java
│   │       └── Constants.java
│   └── resources/
│       └── logback.xml
└── test/
    └── java/com/hytale/soul/
        ├── agent/
        ├── memory/
        ├── decision/
        └── action/
```

**Task 1.4: Base Classes (1 hour)**

**src/main/java/com/hytale/soul/action/DecisionAction.java**
```java
public record DecisionAction(
    String actionType,      // "move", "chat", "interact", "emote"
    long timestamp,
    String payload
) {
    public DecisionAction {
        Objects.requireNonNull(actionType, "actionType cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
    }
}
```

**src/main/java/com/hytale/soul/memory/MemoryEvent.java**
```java
public record MemoryEvent(
    String eventType,      // "move", "chat", "phase_change", etc.
    long timestamp,
    UUID sourcePlayerId,
    String content,
    double importance,     // 0.0 - 1.0
    Map<String, Object> metadata
) {
    public MemoryEvent {
        Objects.requireNonNull(eventType, "eventType cannot be null");
        Objects.requireNonNull(sourcePlayerId, "sourcePlayerId cannot be null");
        Objects.requireNonNull(metadata, "metadata cannot be null");
        if (importance < 0.0 || importance > 1.0) {
            throw new IllegalArgumentException("importance must be 0.0-1.0");
        }
    }
    
    public double recencyScore(long currentTime) {
        long ageMs = currentTime - timestamp;
        return Math.max(0.0, 1.0 - (ageMs / 60000.0)); // 1 min decay
    }
}
```

**src/main/java/com/hytale/soul/util/Vector3f.java**
```java
public record Vector3f(float x, float y, float z) {
    public float distance(Vector3f other) {
        float dx = x - other.x();
        float dy = y - other.y();
        float dz = z - other.z();
        return (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
    }
    
    public Vector3f add(Vector3f other) {
        return new Vector3f(x + other.x, y + other.y, z + other.z);
    }
}
```

**Task 1.5: First Compile (15 min)**
```bash
mvn clean compile
# Expected: BUILD SUCCESS

# Line count
find src/main/java -name "*.java" -exec wc -l {} + | tail -1
# Expected: ~150 lines
```

**End of Day 1:**
- ✅ Project structure created
- ✅ Maven configured
- ✅ Base data classes coded
- ✅ Compiles successfully
- **LOC:** ~150

---

### Day 2: Memory System Part 1

**Goals:**
- ✅ ShortTermMemory implemented
- ✅ Memory scoring logic
- ✅ LinkedList capacity management

**Task 2.1: ShortTermMemory (1.5 hours)**

**src/main/java/com/hytale/soul/memory/ShortTermMemory.java**
```java
public class ShortTermMemory {
    private static final int MAX_EVENTS = 50;
    private final LinkedList<MemoryEvent> events;
    
    public ShortTermMemory() {
        this.events = new LinkedList<>();
    }
    
    public void recordEvent(MemoryEvent event) {
        events.addFirst(event);
        if (events.size() > MAX_EVENTS) {
            events.removeLast();
        }
    }
    
    public List<MemoryEvent> getRecentEvents(int count) {
        return events.stream()
            .limit(count)
            .toList();
    }
    
    public List<MemoryEvent> getEventsByType(String eventType) {
        return events.stream()
            .filter(e -> e.eventType().equals(eventType))
            .toList();
    }
    
    public List<MemoryEvent> getEventsByPlayer(UUID playerId) {
        return events.stream()
            .filter(e -> e.sourcePlayerId().equals(playerId))
            .toList();
    }
    
    public long getTotalMemoryBytes() {
        // Approximate: each event ~500 bytes
        return events.size() * 500L;
    }
    
    public void clear() {
        events.clear();
    }
}
```

**Task 2.2: Memory Scoring Logic (1 hour)**

**src/main/java/com/hytale/soul/memory/MemoryScorer.java**
```java
public class MemoryScorer {
    private static final double RECENCY_WEIGHT = 0.30;
    private static final double IMPORTANCE_WEIGHT = 0.40;
    private static final double RELEVANCE_WEIGHT = 0.30;
    
    public double score(MemoryEvent event, long currentTime, String context) {
        double recencyScore = calculateRecencyScore(event, currentTime);
        double importanceScore = event.importance();
        double relevanceScore = calculateRelevanceScore(event, context);
        
        return (recencyScore * RECENCY_WEIGHT) +
               (importanceScore * IMPORTANCE_WEIGHT) +
               (relevanceScore * RELEVANCE_WEIGHT);
    }
    
    private double calculateRecencyScore(MemoryEvent event, long currentTime) {
        long ageMs = currentTime - event.timestamp();
        double ageMinutes = ageMs / 60000.0;
        // Linear decay: 1.0 at 0 min, 0.0 at 10 min
        return Math.max(0.0, 1.0 - (ageMinutes / 10.0));
    }
    
    private double calculateRelevanceScore(MemoryEvent event, String context) {
        if (context == null) return 0.5; // Default neutral score
        
        // Simple keyword matching
        int matchCount = 0;
        String[] keywords = context.toLowerCase().split("\\s+");
        String eventContent = event.content().toLowerCase();
        
        for (String keyword : keywords) {
            if (eventContent.contains(keyword)) {
                matchCount++;
            }
        }
        
        return Math.min(1.0, matchCount / (double) keywords.length);
    }
}
```

**Task 2.3: Unit Tests (1.5 hours)**

**src/test/java/com/hytale/soul/memory/ShortTermMemoryTest.java**
```java
class ShortTermMemoryTest {
    private ShortTermMemory memory;
    private UUID testPlayer = UUID.randomUUID();
    
    @BeforeEach
    void setUp() {
        memory = new ShortTermMemory();
    }
    
    @Test
    void testRecordEvent() {
        MemoryEvent event = new MemoryEvent(
            "chat", System.currentTimeMillis(), testPlayer,
            "hello world", 0.7, Map.of()
        );
        
        memory.recordEvent(event);
        assertEquals(1, memory.getRecentEvents(10).size());
    }
    
    @Test
    void testCapacityLimit() {
        for (int i = 0; i < 75; i++) {
            memory.recordEvent(new MemoryEvent(
                "move", System.currentTimeMillis(), testPlayer,
                "moved", 0.5, Map.of()
            ));
        }
        
        assertEquals(50, memory.getRecentEvents(100).size());
    }
    
    @Test
    void testEventFiltering() {
        memory.recordEvent(new MemoryEvent(
            "chat", System.currentTimeMillis(), testPlayer,
            "hello", 0.7, Map.of()
        ));
        memory.recordEvent(new MemoryEvent(
            "move", System.currentTimeMillis(), testPlayer,
            "moved", 0.5, Map.of()
        ));
        
        List<MemoryEvent> chatEvents = memory.getEventsByType("chat");
        assertEquals(1, chatEvents.size());
        assertEquals("chat", chatEvents.get(0).eventType());
    }
}
```

**Task 2.4: Run Tests (15 min)**
```bash
mvn test
# Expected: 3/3 tests passing
# Expected: >80% coverage for memory classes
```

**End of Day 2:**
- ✅ ShortTermMemory implemented & tested
- ✅ Memory scoring logic working
- ✅ LinkedList capacity limits enforced
- **LOC:** ~400 (cumulative)
- **Tests Passing:** 3/3 ✅

---

### Day 3: Memory System Part 2

**Goals:**
- ✅ LongTermMemory interface designed
- ✅ FederatedMemoryRepository implemented
- ✅ Async memory persistence planned

**Task 3.1: LongTermMemory Interface (45 min)**

**src/main/java/com/hytale/soul/memory/LongTermMemory.java**
```java
public interface LongTermMemory {
    /**
     * Store event in long-term storage (vector DB)
     */
    void storeEvent(MemoryEvent event);
    
    /**
     * Search for semantically similar events
     */
    List<MemoryEvent> searchSimilar(String query, int limit);
    
    /**
     * Get events by type from long-term storage
     */
    List<MemoryEvent> getEventsByType(String eventType, int limit);
    
    /**
     * Get statistics about stored events
     */
    Map<String, Object> getStatistics();
    
    /**
     * Clear all events from long-term storage
     */
    void clear();
}
```

**Task 3.2: FederatedMemoryRepository (1 hour)**

**src/main/java/com/hytale/soul/memory/FederatedMemoryRepository.java**
```java
public class FederatedMemoryRepository {
    private final ShortTermMemory shortTerm;
    private final LongTermMemory longTerm;  // Injected implementation
    private final MemoryScorer scorer;
    private final ExecutorService asyncExecutor;
    
    public FederatedMemoryRepository(ShortTermMemory shortTerm, LongTermMemory longTerm) {
        this.shortTerm = shortTerm;
        this.longTerm = longTerm;
        this.scorer = new MemoryScorer();
        this.asyncExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "LongTermMemoryPersister");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Record event in short-term, async persist to long-term
     */
    public void recordEvent(MemoryEvent event) {
        // Synchronous: short-term
        shortTerm.recordEvent(event);
        
        // Asynchronous: long-term
        asyncExecutor.submit(() -> longTerm.storeEvent(event));
    }
    
    /**
     * Retrieve memories with hybrid scoring
     */
    public List<MemoryEvent> retrieve(String query, int limit) {
        long now = System.currentTimeMillis();
        
        // Get from short-term
        List<MemoryEvent> shortTermResults = shortTerm.getRecentEvents(limit);
        Map<String, Double> scores = new HashMap<>();
        
        for (MemoryEvent event : shortTermResults) {
            double score = scorer.score(event, now, query);
            scores.put(event.toString(), score);
        }
        
        // Sort by score, return top N
        return shortTermResults.stream()
            .sorted((e1, e2) -> Double.compare(
                scores.get(e2.toString()),
                scores.get(e1.toString())
            ))
            .limit(limit)
            .toList();
    }
    
    /**
     * Get memory statistics
     */
    public Map<String, Object> getMemoryStats() {
        return Map.of(
            "short_term_events", shortTerm.getRecentEvents(100).size(),
            "short_term_bytes", shortTerm.getTotalMemoryBytes(),
            "long_term_stats", longTerm.getStatistics()
        );
    }
    
    public void shutdown() {
        asyncExecutor.shutdown();
    }
}
```

**Task 3.3: Mock LongTermMemory (45 min)**

**src/test/java/com/hytale/soul/memory/MockLongTermMemory.java**
```java
public class MockLongTermMemory implements LongTermMemory {
    private final List<MemoryEvent> storage = Collections.synchronizedList(new ArrayList<>());
    
    @Override
    public void storeEvent(MemoryEvent event) {
        storage.add(event);
    }
    
    @Override
    public List<MemoryEvent> searchSimilar(String query, int limit) {
        return storage.stream()
            .filter(e -> e.content().contains(query))
            .limit(limit)
            .toList();
    }
    
    @Override
    public List<MemoryEvent> getEventsByType(String eventType, int limit) {
        return storage.stream()
            .filter(e -> e.eventType().equals(eventType))
            .limit(limit)
            .toList();
    }
    
    @Override
    public Map<String, Object> getStatistics() {
        return Map.of("total_events", storage.size());
    }
    
    @Override
    public void clear() {
        storage.clear();
    }
}
```

**Task 3.4: Tests (1 hour)**

```java
class FederatedMemoryRepositoryTest {
    private FederatedMemoryRepository repository;
    private MockLongTermMemory mockLongTerm;
    
    @BeforeEach
    void setUp() {
        ShortTermMemory shortTerm = new ShortTermMemory();
        mockLongTerm = new MockLongTermMemory();
        repository = new FederatedMemoryRepository(shortTerm, mockLongTerm);
    }
    
    @Test
    void testRecordEvent() throws InterruptedException {
        UUID player = UUID.randomUUID();
        MemoryEvent event = new MemoryEvent(
            "chat", System.currentTimeMillis(), player,
            "hello", 0.8, Map.of()
        );
        
        repository.recordEvent(event);
        Thread.sleep(100); // Allow async persist
        
        assertEquals(1, mockLongTerm.getStatistics().get("total_events"));
    }
    
    @AfterEach
    void tearDown() {
        repository.shutdown();
    }
}
```

**End of Day 3:**
- ✅ LongTermMemory interface designed
- ✅ FederatedMemoryRepository implemented
- ✅ Mock implementation for testing
- **LOC:** ~600 (cumulative)
- **Tests Passing:** 5/5 ✅

---

### Day 4-5: Decision Engine

**[Detailed daily tasks continue with BehaviorTree, System1/2 decision makers, and comprehensive unit tests]**

**By End of Week 1:**
- ✅ ~1000 LOC written
- ✅ Memory system complete & tested
- ✅ Decision engine skeleton ready
- ✅ >90% test coverage
- ✅ All tests passing

---

## 📅 WEEK 2: EVENT INTEGRATION & TESTING

**[Days 6-10 follow similar day-by-day breakdown with EventSubscriber, HytalePluginAdapter, Persona system, and integration tests]**

**By End of Week 2:**
- ✅ 2000 LOC total
- ✅ Event subscription working
- ✅ Plugin adapter integrated
- ✅ Persona system implemented
- ✅ Full integration tests passing
- ✅ Memory footprint <50MB
- ✅ Ready for Week 3 (Network Integration)

---

## ✅ SUCCESS CHECKLIST

**Week 1 End:**
- [ ] BipedalAgent compiles
- [ ] Memory system working
- [ ] Decision engine making decisions
- [ ] Unit tests >90% coverage
- [ ] All tests passing
- [ ] ~1000 LOC written
- [ ] Maven `mvn test` succeeds
- [ ] Code reviewed & clean

**Week 2 End:**
- [ ] Event subscription functional
- [ ] HytalePluginAdapter works
- [ ] Persona system implemented
- [ ] Integration tests passing
- [ ] Memory <50MB
- [ ] ~2000 LOC total
- [ ] Ready for Week 3

---

## 🎓 LEARNING OUTCOMES

After Week 1-2, you will understand:
- Federated memory architecture
- Dual-process decision making
- Maven project structure
- Unit & integration testing
- Event-driven architecture
- Plugin pattern design

---

**Status:** Ready to code
**Timeline:** 10 days (Week 1-2)
**Target:** 2000 LOC, >90% test coverage
**Next:** Week 3 - Network Integration
