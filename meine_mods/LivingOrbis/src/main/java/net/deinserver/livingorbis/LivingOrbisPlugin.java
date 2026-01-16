package net.deinserver.livingorbis;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import net.deinserver.livingorbis.core.BipedalAgent;
import net.deinserver.livingorbis.core.DualProcessDecisionEngine;
import net.deinserver.livingorbis.memory.FederatedMemoryRepository;
import net.deinserver.livingorbis.persona.PersonaProfile;
import net.deinserver.livingorbis.movement.PhysicalMimesisEngine;
import net.deinserver.livingorbis.masking.LatencyMaskingOrchestrator;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * LivingOrbisPlugin - Hytale Soul Algorithm Framework
 * 
 * Haupt-Plugin für das Aletheia KI-Agenten-System
 * Fusioniert Soul Algorithm + Projekt Aletheia
 */
public class LivingOrbisPlugin extends JavaPlugin {
    
    // Services
    private GeminiService geminiService;
    
    // Agent Management
    private final Map<String, BipedalAgent> activeAgents = new HashMap<>();
    private DualProcessDecisionEngine sharedDecisionEngine;
    
    // Configuration
    private static final String GEMINI_API_KEY = "AIzaSyAWHvcSTtVqWXFjX_4aAyzuwwtaZ0aTCNA";
    private static final boolean USE_LOCAL_OLLAMA = false; // Set true if Ollama available
    
    public LivingOrbisPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("╔══════════════════════════════════════════════════════════╗");
        getLogger().at(Level.INFO).log("║  LIVING ORBIS - Hytale Soul Algorithm Framework          ║");
        getLogger().at(Level.INFO).log("║  Projekt Aletheia: Reverse Turing Test                   ║");
        getLogger().at(Level.INFO).log("╚══════════════════════════════════════════════════════════╝");
        
        // Initialize Gemini Service
        this.geminiService = new GeminiService(GEMINI_API_KEY);
        getLogger().at(Level.INFO).log("[LivingOrbis] Gemini Service initialized");
        
        // Initialize shared Decision Engine
        this.sharedDecisionEngine = new DualProcessDecisionEngine(
            GEMINI_API_KEY,
            "http://localhost:11434",  // Ollama URL
            USE_LOCAL_OLLAMA
        );
        getLogger().at(Level.INFO).log("[LivingOrbis] DualProcessDecisionEngine initialized");
        
        // Register Commands
        this.getCommandRegistry().registerCommand(new GeminiCommand(geminiService));
        this.getCommandRegistry().registerCommand(new AletheiaCommand(this));
        
        getLogger().at(Level.INFO).log("✅ Living Orbis Setup Complete!");
        getLogger().at(Level.INFO).log("Commands: /ask, /aletheia spawn, /aletheia list, /aletheia stop");
    }
    
    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("[LivingOrbis] Shutting down...");
        
        // Stop all agents
        for (BipedalAgent agent : activeAgents.values()) {
            agent.stop();
        }
        activeAgents.clear();
        
        // Shutdown decision engine
        if (sharedDecisionEngine != null) {
            sharedDecisionEngine.shutdown();
        }
        
        getLogger().at(Level.INFO).log("[LivingOrbis] Shutdown complete");
    }
    
    // ==================== AGENT MANAGEMENT ====================
    
    /**
     * Spawnt einen neuen BipedalAgent
     */
    public BipedalAgent spawnAgent(String name, PersonaProfile persona, BipedalAgent.Location location) {
        if (activeAgents.containsKey(name)) {
            getLogger().at(Level.WARNING).log("[LivingOrbis] Agent '" + name + "' already exists!");
            return activeAgents.get(name);
        }
        
        // Create components
        FederatedMemoryRepository memory = new FederatedMemoryRepository();
        PhysicalMimesisEngine mimesis = new PhysicalMimesisEngine(location);
        LatencyMaskingOrchestrator masking = new LatencyMaskingOrchestrator();
        
        // Setup masking callbacks (TODO: Integrate with Hytale API)
        masking.setCallbacks(
            emote -> getLogger().at(Level.FINE).log("[Agent:" + name + "] Emote: " + emote),
            msg -> getLogger().at(Level.INFO).log("[Agent:" + name + "] Chat: " + msg),
            typing -> getLogger().at(Level.FINE).log("[Agent:" + name + "] Typing: " + typing),
            () -> getLogger().at(Level.FINE).log("[Agent:" + name + "] Playing idle animation")
        );
        
        // Create agent
        BipedalAgent agent = new BipedalAgent(
            name,
            persona,
            memory,
            sharedDecisionEngine,
            mimesis,
            masking
        );
        
        // Start agent
        agent.start();
        activeAgents.put(name, agent);
        
        getLogger().at(Level.INFO).log("[LivingOrbis] Spawned agent: " + name + " (" + persona.getArchetype() + ")");
        return agent;
    }
    
    /**
     * Stoppt einen Agent
     */
    public void stopAgent(String name) {
        BipedalAgent agent = activeAgents.remove(name);
        if (agent != null) {
            agent.stop();
            getLogger().at(Level.INFO).log("[LivingOrbis] Stopped agent: " + name);
        }
    }
    
    /**
     * Gibt alle aktiven Agents zurück
     */
    public Map<String, BipedalAgent> getActiveAgents() {
        return new HashMap<>(activeAgents);
    }
    
    /**
     * Gibt einen Agent per Name zurück
     */
    public BipedalAgent getAgent(String name) {
        return activeAgents.get(name);
    }
    
    // ==================== EVENT HANDLERS ====================
    
    /**
     * Wird aufgerufen wenn ein Spieler chattet
     * Leitet an alle Agents weiter
     */
    public void onPlayerChat(String playerId, String message) {
        for (BipedalAgent agent : activeAgents.values()) {
            agent.onPlayerSpeak(playerId, message);
        }
    }
    
    /**
     * Wird aufgerufen wenn ein Spieler sich bewegt
     */
    public void onPlayerMove(String playerId, BipedalAgent.Location newLocation) {
        for (BipedalAgent agent : activeAgents.values()) {
            agent.onPlayerMove(playerId, newLocation);
        }
    }
    
    /**
     * Wird aufgerufen bei Phasenwechsel
     */
    public void onPhaseChange(BipedalAgent.GamePhase newPhase) {
        for (BipedalAgent agent : activeAgents.values()) {
            agent.onPhaseChange(newPhase);
        }
    }
    
    // ==================== GETTERS ====================
    
    public GeminiService getGeminiService() {
        return geminiService;
    }
    
    public DualProcessDecisionEngine getDecisionEngine() {
        return sharedDecisionEngine;
    }
}
