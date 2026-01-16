package net.deinserver.livingorbis.core;

import net.deinserver.livingorbis.memory.FederatedMemoryRepository;
import net.deinserver.livingorbis.persona.PersonaProfile;
import net.deinserver.livingorbis.movement.PhysicalMimesisEngine;
import net.deinserver.livingorbis.masking.LatencyMaskingOrchestrator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * BipedalAgent - Master Orchestrator für KI-Agenten im Hytale Soul Algorithm Framework
 * 
 * Fusioniert Soul Algorithm (Gedanken-Ebene) mit Projekt Aletheia (Bewegungs-Ebene)
 * durch einen 4-Phasen Loop: Perception → Decision → Execution → Masking
 * 
 * @author André Soul Algorithm Lab
 * @version 1.0 - Phase 1 Implementation
 */
public class BipedalAgent implements Runnable {
    
    private static final Logger LOGGER = Logger.getLogger(BipedalAgent.class.getName());
    private static final long TICK_INTERVAL_MS = 50; // 50ms = 20 ticks/second
    
    // Agent Identity
    private final String agentId;
    private final PersonaProfile persona;
    
    // Core Components
    private final FederatedMemoryRepository memory;
    private final DualProcessDecisionEngine decisionEngine;
    private final PhysicalMimesisEngine physicalMimesis;
    private final LatencyMaskingOrchestrator maskingOrchestrator;
    
    // Async Infrastructure
    private final ScheduledExecutorService scheduler;
    private CompletableFuture<String> pendingLLMDecision;
    
    // State
    private volatile boolean isRunning = false;
    private long lastQuestionTime = 0;
    private GameContext currentContext;
    
    /**
     * Konstruktor für BipedalAgent
     * 
     * @param agentId Eindeutige ID des Agenten
     * @param persona Persona-Profil (Archetype, Faction, Hidden Agendas)
     * @param memory Federated Memory Repository
     * @param decisionEngine Dual-Process Decision Engine
     * @param physicalMimesis Physical Mimesis Engine
     * @param maskingOrchestrator Latency Masking Orchestrator
     */
    public BipedalAgent(
            String agentId,
            PersonaProfile persona,
            FederatedMemoryRepository memory,
            DualProcessDecisionEngine decisionEngine,
            PhysicalMimesisEngine physicalMimesis,
            LatencyMaskingOrchestrator maskingOrchestrator) {
        
        this.agentId = agentId;
        this.persona = persona;
        this.memory = memory;
        this.decisionEngine = decisionEngine;
        this.physicalMimesis = physicalMimesis;
        this.maskingOrchestrator = maskingOrchestrator;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.currentContext = new GameContext();
        
        LOGGER.info("[BipedalAgent] Initialized: " + agentId + " with persona: " + persona.getArchetype());
    }
    
    /**
     * Start des Agent-Loops (50ms Tick-Intervall)
     */
    public void start() {
        if (isRunning) {
            LOGGER.warning("[BipedalAgent] Already running: " + agentId);
            return;
        }
        
        isRunning = true;
        scheduler.scheduleAtFixedRate(this, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
        LOGGER.info("[BipedalAgent] Started: " + agentId);
    }
    
    /**
     * Stop des Agent-Loops
     */
    public void stop() {
        isRunning = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("[BipedalAgent] Stopped: " + agentId);
    }
    
    /**
     * Hauptloop - wird alle 50ms ausgeführt
     * 4 Phasen: Perception → Decision → Execution → Masking
     */
    @Override
    public void run() {
        if (!isRunning) return;
        
        try {
            // PHASE 1: PERCEPTION (beide Systeme)
            updatePerception();
            
            // PHASE 2: DECISION (Dual-Process)
            updateDecision();
            
            // PHASE 3: EXECUTION (Multimodal)
            executeActions();
            
            // PHASE 4: MASKING (Orchestriert)
            maskingOrchestrator.orchestrateLatency();
            
        } catch (Exception e) {
            LOGGER.severe("[BipedalAgent] Error in tick: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== PHASE 1: PERCEPTION ====================
    
    /**
     * Erfasst alle relevanten Game Events und aktualisiert den Context
     */
    private void updatePerception() {
        // TODO: Integration mit Hytale GameStateManager
        // - Nearby players
        // - Recent chat messages
        // - Game phase changes
        // - Death events
        
        currentContext.setTimestamp(System.currentTimeMillis());
        
        // Beispiel: Perception von umgebenden Spielern
        // List<Player> nearbyPlayers = gameStateManager.getNearbyPlayers(agentId, 50);
        // currentContext.setNearbyPlayers(nearbyPlayers);
    }
    
    // ==================== PHASE 2: DECISION ====================
    
    /**
     * Dual-Process Decision: System 1 (Reflex) oder System 2 (Strategic)
     */
    private void updateDecision() {
        if (!shouldMakeDecision()) return;
        
        if (isUrgentSituation()) {
            // System 1: Instant Reflex (<10ms)
            long startTime = System.currentTimeMillis();
            BehaviorTreeResult action = decisionEngine.evaluateBehaviorTree(currentContext);
            long elapsed = System.currentTimeMillis() - startTime;
            
            LOGGER.fine("[SYSTEM 1] Decision in " + elapsed + "ms: " + action.getActionType());
            executeReflexAction(action);
            
        } else {
            // System 2: Strategic Thinking (async, 500-2000ms)
            if (pendingLLMDecision == null || pendingLLMDecision.isDone()) {
                requestLLMDecision();
                LOGGER.fine("[SYSTEM 2] LLM inference started...");
            }
        }
    }
    
    /**
     * Prüft ob eine Entscheidung notwendig ist
     */
    private boolean shouldMakeDecision() {
        // Entscheidung nur wenn Context sich geändert hat oder Timer abgelaufen
        return currentContext.hasChangedSince(memory.getLastDecisionTimestamp());
    }
    
    /**
     * Prüft ob eine dringende Situation vorliegt (System 1 erforderlich)
     */
    private boolean isUrgentSituation() {
        // Urgent wenn:
        // - Voting Phase aktiv
        // - Spieler in unmittelbarer Nähe
        // - Kürzlich angesprochen
        return currentContext.isVotingPhase() 
            || currentContext.hasImminentThreat()
            || (System.currentTimeMillis() - lastQuestionTime) < 500;
    }
    
    /**
     * Startet asynchrone LLM-Inferenz
     */
    private void requestLLMDecision() {
        String memoryContext = memory.getMemoryContext();
        String prompt = persona.generatePrompt(currentContext, memoryContext);
        
        // Masking starten BEVOR LLM aufgerufen wird
        maskingOrchestrator.initiateMasking();
        
        pendingLLMDecision = decisionEngine.inferAsync(prompt)
            .thenApply(decision -> {
                executeLLMAction(decision);
                return decision;
            })
            .exceptionally(ex -> {
                LOGGER.warning("[SYSTEM 2] LLM failed: " + ex.getMessage());
                return "IDLE:::fallback";
            });
    }
    
    // ==================== PHASE 3: EXECUTION ====================
    
    /**
     * Führt alle ausstehenden Aktionen aus
     */
    private void executeActions() {
        // Physical Mimesis Updates (kontinuierlich)
        physicalMimesis.update();
    }
    
    /**
     * Führt System 1 Reflex-Aktion aus
     */
    private void executeReflexAction(BehaviorTreeResult action) {
        switch (action.getActionType()) {
            case MOVE:
                physicalMimesis.moveToTarget(action.getTargetLocation());
                break;
            case DODGE:
                physicalMimesis.executeDodge(action.getDodgeDirection());
                break;
            case LOOK:
                physicalMimesis.lookAt(action.getLookTarget());
                break;
            case IDLE:
            default:
                physicalMimesis.triggerIdleBehavior();
                break;
        }
    }
    
    /**
     * Führt LLM-generierte Aktion aus
     */
    private void executeLLMAction(String llmDecision) {
        String[] parts = llmDecision.split(":::");
        String actionType = parts[0];
        String actionData = parts.length > 1 ? parts[1] : "";
        
        switch (actionType) {
            case "CHAT":
                executeChatAction(actionData);
                break;
            case "MOVE":
                // Parse coordinates and move
                physicalMimesis.moveToTarget(parseLocation(actionData));
                break;
            case "VOTE":
                executeVoteAction(actionData);
                break;
            case "ABILITY":
                executeAbilityAction(actionData);
                break;
            case "IDLE":
            default:
                physicalMimesis.triggerIdleBehavior();
                break;
        }
        
        memory.recordDecision(llmDecision);
    }
    
    /**
     * Führt Chat-Aktion aus mit Tell-Detection
     */
    private void executeChatAction(String message) {
        // Prüfe auf Widerspruch zu früheren Aussagen
        if (memory.contradictsPreviousStatement(message)) {
            // 5% Chance: Visual Glitch (Tell für aufmerksame Spieler)
            if (Math.random() < 0.05) {
                spawnVisualGlitch();
                LOGGER.info("[TELL] Visual glitch triggered due to contradiction");
            }
        }
        
        // Timing-Tell Check
        long responseTime = System.currentTimeMillis() - lastQuestionTime;
        if (responseTime < 100 && Math.random() < 0.10) {
            LOGGER.info("[TELL] Response too fast (" + responseTime + "ms)");
        }
        
        // Chat senden (ersetzt Filler-Message)
        // npcPlayer.chat(message);
        maskingOrchestrator.replaceFillerWithRealMessage(message);
    }
    
    /**
     * Spawned visuellen Glitch (Tell für Spieler)
     */
    private void spawnVisualGlitch() {
        // TODO: Integration mit Hytale Particle/Effect System
        // Textur-Flicker für 200ms um Avatar
        LOGGER.fine("[TELL] Spawning visual glitch for agent: " + agentId);
    }
    
    private void executeVoteAction(String targetPlayer) {
        // TODO: Hytale Voting System Integration
        LOGGER.fine("[ACTION] Vote for: " + targetPlayer);
    }
    
    private void executeAbilityAction(String abilityData) {
        // TODO: Aletheia Ability System (Architect abilities, etc.)
        LOGGER.fine("[ACTION] Ability: " + abilityData);
    }
    
    private Location parseLocation(String locationData) {
        // Parse "x,y,z" format
        String[] coords = locationData.split(",");
        return new Location(
            Double.parseDouble(coords[0].trim()),
            Double.parseDouble(coords[1].trim()),
            Double.parseDouble(coords[2].trim())
        );
    }
    
    // ==================== EVENT HANDLERS ====================
    
    /**
     * Handler für eingehende Chat-Nachrichten
     */
    public void onPlayerSpeak(String speakerId, String message) {
        lastQuestionTime = System.currentTimeMillis();
        memory.recordEvent("Player " + speakerId + " said: " + message);
        
        // Prüfe ob Agent angesprochen wurde
        if (message.toLowerCase().contains(agentId.toLowerCase())) {
            currentContext.setDirectlyAddressed(true);
        }
    }
    
    /**
     * Handler für Spielerbewegungen in der Nähe
     */
    public void onPlayerMove(String playerId, Location newLocation) {
        memory.recordEvent("Player " + playerId + " moved to " + newLocation);
        currentContext.updatePlayerPosition(playerId, newLocation);
    }
    
    /**
     * Handler für Phasenwechsel (Day/Night/Voting)
     */
    public void onPhaseChange(GamePhase newPhase) {
        memory.recordEvent("Phase changed to: " + newPhase);
        currentContext.setCurrentPhase(newPhase);
        
        if (newPhase == GamePhase.VOTING) {
            // Trigger immediate voting strategy via LLM
            requestLLMDecision();
        }
    }
    
    /**
     * Handler für Spieler-Tod
     */
    public void onPlayerDeath(String playerId, String killerId) {
        memory.recordEvent("Player " + playerId + " was killed by " + killerId);
        currentContext.recordDeath(playerId, killerId);
    }
    
    // ==================== GETTERS ====================
    
    public String getAgentId() {
        return agentId;
    }
    
    public PersonaProfile getPersona() {
        return persona;
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * Einfache Location-Klasse (TODO: Ersetzen durch Hytale Vector3)
     */
    public static class Location {
        public final double x, y, z;
        
        public Location(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        @Override
        public String toString() {
            return String.format("(%.2f, %.2f, %.2f)", x, y, z);
        }
    }
    
    /**
     * Game Phase Enum
     */
    public enum GamePhase {
        DAY,
        NIGHT,
        VOTING,
        EMERGENCY,
        ENDED
    }
}
