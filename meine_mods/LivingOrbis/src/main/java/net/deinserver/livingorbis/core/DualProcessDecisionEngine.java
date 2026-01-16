package net.deinserver.livingorbis.core;

import net.deinserver.livingorbis.GeminiService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * DualProcessDecisionEngine - Dual-Process Decision Making
 * 
 * System 1: BehaviorTree für instant Reflexe (<10ms)
 * System 2: LLM für strategische Entscheidungen (async, 500-2000ms)
 * 
 * Nutzt GeminiService für LLM-Inferenz (vereinfachte Version ohne LangChain4j)
 * 
 * @author André Soul Algorithm Lab
 */
public class DualProcessDecisionEngine {
    
    private static final Logger LOGGER = Logger.getLogger(DualProcessDecisionEngine.class.getName());
    
    // LLM Client - nutzt bestehenden GeminiService
    private final GeminiService geminiService;
    
    // Async Executor for LLM calls
    private final ExecutorService llmExecutor;
    
    // Configuration
    private final boolean useLocalModel;
    
    /**
     * Konstruktor
     * 
     * @param geminiApiKey API Key für Google Gemini
     * @param ollamaBaseUrl URL für lokalen Ollama Server (unused in this version)
     * @param useLocalModel Ob lokales Modell bevorzugt werden soll (unused in this version)
     */
    public DualProcessDecisionEngine(String geminiApiKey, String ollamaBaseUrl, boolean useLocalModel) {
        // Updated to use the single-argument constructor of the new SDK-based Service
        this.geminiService = new GeminiService(geminiApiKey);
        this.useLocalModel = useLocalModel;
        this.llmExecutor = Executors.newFixedThreadPool(4);
        
        LOGGER.info("[DecisionEngine] Initialized with GeminiService");
    }
    
    // ==================== SYSTEM 1: BEHAVIOR TREE ====================
    
    /**
     * Evaluiert den BehaviorTree für schnelle Reflex-Entscheidungen
     * Latenz: <10ms
     * 
     * @param context Aktueller Spielkontext
     * @return BehaviorTreeResult mit der zu ausführenden Aktion
     */
    public BehaviorTreeResult evaluateBehaviorTree(GameContext context) {
        long startTime = System.currentTimeMillis();
        
        // Selector Node: Prioritätsbasierte Entscheidung
        BehaviorTreeResult result = selector(
            // Höchste Priorität: Dodge bei Bedrohung
            () -> context.hasImminentThreat() 
                ? BehaviorTreeResult.dodge(new BipedalAgent.Location(
                    Math.random() * 2 - 1,  // Random dodge direction
                    0,
                    Math.random() * 2 - 1
                  ))
                : null,
            
            // Mittlere Priorität: Direkt angesprochen
            () -> context.isDirectlyAddressed()
                ? BehaviorTreeResult.idle() // Warte auf LLM für Response
                : null,
            
            // Niedrige Priorität: Voting Phase
            () -> context.isVotingPhase()
                ? BehaviorTreeResult.idle() // Warte auf LLM für Vote
                : null,
            
            // Default: Idle
            () -> BehaviorTreeResult.idle()
        );
        
        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.fine("[SYSTEM 1] BehaviorTree evaluated in " + elapsed + "ms");
        
        return result;
    }
    
    /**
     * Selector Node: Führt Kinder aus bis eines erfolgreich ist
     */
    @SafeVarargs
    private final BehaviorTreeResult selector(java.util.function.Supplier<BehaviorTreeResult>... children) {
        for (var child : children) {
            BehaviorTreeResult result = child.get();
            if (result != null) {
                return result;
            }
        }
        return BehaviorTreeResult.idle();
    }
    
    // ==================== SYSTEM 2: LLM INFERENCE ====================
    
    /**
     * Startet asynchrone LLM-Inferenz
     * Latenz: 500-2000ms (wird durch LatencyMasking verschleiert)
     * 
     * @param prompt Der vollständige Prompt mit Memory Context
     * @return CompletableFuture mit der LLM-Entscheidung
     */
    public CompletableFuture<String> inferAsync(String prompt) {
        return geminiService.askGemini(prompt)
            .thenApply(response -> {
                LOGGER.info("[SYSTEM 2] Gemini inference completed");
                return parseResponse(response);
            })
            .exceptionally(ex -> {
                LOGGER.warning("[SYSTEM 2] Inference failed: " + ex.getMessage());
                return "IDLE:::error";
            });
    }
    
    /**
     * Parsed LLM Response in Action-Format (TYPE:::DATA)
     */
    private String parseResponse(String response) {
        // LLM sollte Responses in folgendem Format geben:
        // ACTION: CHAT
        // MESSAGE: Ich war in der Cafeteria...
        
        String normalized = response.trim().toLowerCase();
        
        if (normalized.contains("action: chat") || normalized.contains("message:")) {
            String message = extractAfter(response, "message:", "\n");
            if (message == null) message = extractAfter(response, "MESSAGE:", "\n");
            if (message == null) message = response; // Use full response
            return "CHAT:::" + message.trim();
        }
        
        if (normalized.contains("action: vote")) {
            String target = extractAfter(response, "target:", "\n");
            if (target == null) target = extractAfter(response, "TARGET:", "\n");
            return "VOTE:::" + (target != null ? target.trim() : "skip");
        }
        
        if (normalized.contains("action: move")) {
            String location = extractAfter(response, "location:", "\n");
            return "MOVE:::" + (location != null ? location.trim() : "0,64,0");
        }
        
        // Default: Treat as chat message
        return "CHAT:::" + response.trim();
    }
    
    /**
     * Extrahiert Text zwischen Marker und Delimiter
     */
    private String extractAfter(String text, String marker, String delimiter) {
        int start = text.toLowerCase().indexOf(marker.toLowerCase());
        if (start == -1) return null;
        
        start += marker.length();
        int end = text.indexOf(delimiter, start);
        if (end == -1) end = text.length();
        
        return text.substring(start, end).trim();
    }
    
    /**
     * Shutdown
     */
    public void shutdown() {
        llmExecutor.shutdown();
    }
}
