package net.deinserver.livingorbis.memory;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * FederatedMemoryRepository - Zwei-Schicht Memory System
 * 
 * Short-Term: LinkedList (in RAM, 50 Events, fast access)
 * Long-Term: Vector DB (MongoDB, persistent, semantic search)
 * 
 * Ermöglicht konsistente Erinnerungen für glaubwürdige KI-Agenten
 * 
 * @author André Soul Algorithm Lab
 */
public class FederatedMemoryRepository {
    
    private static final Logger LOGGER = Logger.getLogger(FederatedMemoryRepository.class.getName());
    private static final int SHORT_TERM_CAPACITY = 50;
    
    // Short-Term Memory (in-RAM)
    private final LinkedList<MemoryEvent> shortTermMemory;
    private final Object shortTermLock = new Object();
    
    // Long-Term Memory (Vector DB) - TODO: MongoDB Integration
    private final Map<String, List<MemoryEvent>> longTermMemory;
    
    // Suspicion Scores für andere Spieler
    private final Map<String, Double> suspicionScores;
    
    // Entscheidungs-History
    private final List<String> decisionHistory;
    
    // Timestamps
    private long lastDecisionTimestamp = 0;
    
    // Async Executor für Vector DB Writes
    private final ExecutorService asyncExecutor;
    
    public FederatedMemoryRepository() {
        this.shortTermMemory = new LinkedList<>();
        this.longTermMemory = new ConcurrentHashMap<>();
        this.suspicionScores = new ConcurrentHashMap<>();
        this.decisionHistory = new CopyOnWriteArrayList<>();
        this.asyncExecutor = Executors.newSingleThreadExecutor();
    }
    
    // ==================== EVENT RECORDING ====================
    
    /**
     * Zeichnet ein Game Event auf (Short-Term + async Long-Term)
     */
    public void recordEvent(String description) {
        MemoryEvent event = new MemoryEvent(description, System.currentTimeMillis());
        
        synchronized (shortTermLock) {
            shortTermMemory.addLast(event);
            
            // Kapazität begrenzen
            while (shortTermMemory.size() > SHORT_TERM_CAPACITY) {
                MemoryEvent removed = shortTermMemory.removeFirst();
                // Async in Long-Term speichern
                asyncExecutor.submit(() -> storeInLongTerm(removed));
            }
        }
        
        LOGGER.fine("[Memory] Recorded: " + description);
    }
    
    /**
     * Zeichnet eine getroffene Entscheidung auf
     */
    public void recordDecision(String decision) {
        decisionHistory.add(decision);
        lastDecisionTimestamp = System.currentTimeMillis();
        
        // Auch als Event aufzeichnen
        recordEvent("I decided: " + decision);
    }
    
    /**
     * Speichert Event in Long-Term Memory (async)
     */
    private void storeInLongTerm(MemoryEvent event) {
        // TODO: MongoDB Vector DB Integration
        // - Embedding berechnen
        // - In MongoDB speichern mit Vector Index
        
        String category = categorizeEvent(event.description);
        longTermMemory.computeIfAbsent(category, k -> new CopyOnWriteArrayList<>()).add(event);
        
        LOGGER.fine("[Memory] Stored in long-term: " + event.description);
    }
    
    /**
     * Kategorisiert Events für Long-Term Storage
     */
    private String categorizeEvent(String description) {
        String lower = description.toLowerCase();
        if (lower.contains("killed") || lower.contains("death") || lower.contains("dead")) {
            return "deaths";
        }
        if (lower.contains("said") || lower.contains("chat") || lower.contains("message")) {
            return "communications";
        }
        if (lower.contains("moved") || lower.contains("location") || lower.contains("went")) {
            return "movements";
        }
        if (lower.contains("vote") || lower.contains("accuse") || lower.contains("suspect")) {
            return "accusations";
        }
        return "general";
    }
    
    // ==================== CONTEXT BUILDING ====================
    
    /**
     * Baut den Memory Context für LLM Prompts
     * Kombiniert Short-Term + relevante Long-Term Erinnerungen
     */
    public String getMemoryContext() {
        StringBuilder context = new StringBuilder();
        
        // Tier 1: Jüngste Ereignisse (Short-Term)
        context.append("=== RECENT EVENTS ===\n");
        synchronized (shortTermLock) {
            int start = Math.max(0, shortTermMemory.size() - 10);
            for (int i = start; i < shortTermMemory.size(); i++) {
                MemoryEvent e = shortTermMemory.get(i);
                long agoMs = System.currentTimeMillis() - e.timestamp;
                context.append(String.format("[%ds ago] %s\n", agoMs / 1000, e.description));
            }
        }
        
        // Tier 2: Relevante Langzeit-Erinnerungen
        context.append("\n=== RELEVANT BACKGROUND ===\n");
        // TODO: Vector DB semantic search
        // Für jetzt: Zeige wichtigste Events pro Kategorie
        for (Map.Entry<String, List<MemoryEvent>> entry : longTermMemory.entrySet()) {
            List<MemoryEvent> events = entry.getValue();
            if (!events.isEmpty()) {
                MemoryEvent latest = events.get(events.size() - 1);
                context.append("- [").append(entry.getKey()).append("] ")
                       .append(latest.description).append("\n");
            }
        }
        
        // Tier 3: Persönliche Einschätzung (Suspicion Scores)
        context.append("\n=== MY SUSPICIONS ===\n");
        for (Map.Entry<String, Double> entry : suspicionScores.entrySet()) {
            context.append(String.format("- %s: %.0f%% suspicious\n", 
                entry.getKey(), entry.getValue() * 100));
        }
        
        return context.toString();
    }
    
    // ==================== CONSISTENCY CHECKING ====================
    
    /**
     * Prüft ob eine neue Aussage früheren Aussagen widerspricht
     * 
     * @param newStatement Die neue Aussage
     * @return true wenn Widerspruch erkannt
     */
    public boolean contradictsPreviousStatement(String newStatement) {
        // Einfache Heuristik: Prüfe auf direkte Negationen
        String lowerNew = newStatement.toLowerCase();
        
        synchronized (shortTermLock) {
            for (MemoryEvent event : shortTermMemory) {
                if (!event.description.startsWith("I decided:")) continue;
                
                String lowerOld = event.description.toLowerCase();
                
                // Prüfe auf Location-Widerspruch
                // "I was in X" vs "I was not in X"
                if (containsLocationContradiction(lowerOld, lowerNew)) {
                    LOGGER.info("[Memory] Contradiction detected!");
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Erkennt Location-Widersprüche
     */
    private boolean containsLocationContradiction(String oldStatement, String newStatement) {
        // Extrahiere Location-Aussagen
        // "i was in X" vs "i was not in X" oder "i wasn't in X"
        
        String[] locations = {"cafeteria", "reactor", "medical", "electrical", "admin", 
                             "navigation", "shields", "weapons", "oxygen", "storage"};
        
        for (String location : locations) {
            boolean oldWasIn = oldStatement.contains("was in " + location) 
                            && !oldStatement.contains("was not in " + location)
                            && !oldStatement.contains("wasn't in " + location);
            
            boolean newWasNotIn = newStatement.contains("was not in " + location)
                               || newStatement.contains("wasn't in " + location);
            
            boolean newWasIn = newStatement.contains("was in " + location)
                            && !newWasNotIn;
            
            boolean oldWasNotIn = oldStatement.contains("was not in " + location)
                               || oldStatement.contains("wasn't in " + location);
            
            if ((oldWasIn && newWasNotIn) || (oldWasNotIn && newWasIn)) {
                return true;
            }
        }
        
        return false;
    }
    
    // ==================== SUSPICION MANAGEMENT ====================
    
    /**
     * Aktualisiert den Suspicion Score für einen Spieler
     */
    public void updateSuspicion(String playerId, double delta) {
        suspicionScores.merge(playerId, delta, (old, d) -> Math.max(0, Math.min(1, old + d)));
    }
    
    /**
     * Gibt alle Suspicion Scores zurück
     */
    public Map<String, Double> getSuspicionScores() {
        return new HashMap<>(suspicionScores);
    }
    
    // ==================== GETTERS ====================
    
    public long getLastDecisionTimestamp() {
        return lastDecisionTimestamp;
    }
    
    public int getShortTermSize() {
        synchronized (shortTermLock) {
            return shortTermMemory.size();
        }
    }
    
    /**
     * Shutdown
     */
    public void shutdown() {
        asyncExecutor.shutdown();
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Einzelnes Memory Event
     */
    public static class MemoryEvent {
        public final String description;
        public final long timestamp;
        
        public MemoryEvent(String description, long timestamp) {
            this.description = description;
            this.timestamp = timestamp;
        }
    }
}
