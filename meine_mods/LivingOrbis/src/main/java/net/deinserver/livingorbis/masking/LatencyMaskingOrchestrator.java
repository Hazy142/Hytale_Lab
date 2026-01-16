package net.deinserver.livingorbis.masking;

import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.function.Consumer;

/**
 * LatencyMaskingOrchestrator - 3-Tier Latency Masking System
 * 
 * Verschleiert LLM-Inferenz-Latenz (500-2000ms) durch:
 * - Tier 1 (0-500ms): Emote + Typing Indicator
 * - Tier 2 (500-1500ms): Filler Words im Chat
 * - Tier 3 (1500-4000ms): Animation Masking
 * 
 * Ergebnis: LLM-Verzögerung wirkt wie natürliches Nachdenken
 */
public class LatencyMaskingOrchestrator {
    
    private static final Logger LOGGER = Logger.getLogger(LatencyMaskingOrchestrator.class.getName());
    
    // Timing Configuration
    private static final long TIER1_DURATION_MS = 500;
    private static final long TIER2_DURATION_MS = 1000;
    private static final long TIER3_DURATION_MS = 2000;
    
    // Filler Words/Phrases
    private static final String[] FILLER_PHRASES = {
        "Hmm...",
        "Lass mich nachdenken...",
        "Moment...",
        "Gute Frage...",
        "Also...",
        "Interessant...",
        "Hmm, mal überlegen...",
        "Warte kurz...",
        "Well...",
        "Uh..."
    };
    
    // State
    private boolean isMasking = false;
    private long maskingStartTime;
    private String currentFillerMessageId;
    private ScheduledFuture<?> tier1Task;
    private ScheduledFuture<?> tier2Task;
    private ScheduledFuture<?> tier3Task;
    
    // Scheduler
    private final ScheduledExecutorService scheduler;
    
    // Callbacks für Hytale-Integration
    private Consumer<String> emotePlayer;       // emote name
    private Consumer<String> sendChatPlayer;    // message
    private Consumer<String> typingIndicator;   // "start" oder "stop"
    private Runnable playIdleAnimation;
    
    public LatencyMaskingOrchestrator() {
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    /**
     * Setzt Callbacks für Hytale-Integration
     */
    public void setCallbacks(
            Consumer<String> emotePlayer,
            Consumer<String> sendChatPlayer,
            Consumer<String> typingIndicator,
            Runnable playIdleAnimation) {
        this.emotePlayer = emotePlayer;
        this.sendChatPlayer = sendChatPlayer;
        this.typingIndicator = typingIndicator;
        this.playIdleAnimation = playIdleAnimation;
    }
    
    /**
     * Startet Masking (bevor LLM aufgerufen wird)
     */
    public void initiateMasking() {
        if (isMasking) {
            LOGGER.fine("[Masking] Already masking, skipping");
            return;
        }
        
        isMasking = true;
        maskingStartTime = System.currentTimeMillis();
        LOGGER.info("[Masking] Initiating 3-tier latency masking");
        
        // === TIER 1: Immediate (0-500ms) ===
        executeTier1();
        
        // === TIER 2: Scheduled (500ms) ===
        tier2Task = scheduler.schedule(this::executeTier2, TIER1_DURATION_MS, TimeUnit.MILLISECONDS);
        
        // === TIER 3: Scheduled (1500ms) ===
        tier3Task = scheduler.schedule(this::executeTier3, TIER1_DURATION_MS + TIER2_DURATION_MS, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Tier 1: Emote + Typing Indicator
     */
    private void executeTier1() {
        LOGGER.fine("[Masking] Tier 1: Emote + Typing");
        
        // Thinking Emote abspielen
        if (emotePlayer != null) {
            emotePlayer.accept("thinking");
        }
        
        // Typing Indicator starten
        if (typingIndicator != null) {
            typingIndicator.accept("start");
        }
    }
    
    /**
     * Tier 2: Filler Words
     */
    private void executeTier2() {
        LOGGER.fine("[Masking] Tier 2: Filler Words");
        
        // Zufälligen Filler senden
        String filler = FILLER_PHRASES[(int)(Math.random() * FILLER_PHRASES.length)];
        
        if (sendChatPlayer != null) {
            currentFillerMessageId = filler; // In echter Impl: Message ID speichern
            sendChatPlayer.accept(filler);
        }
    }
    
    /**
     * Tier 3: Animation Loop
     */
    private void executeTier3() {
        LOGGER.fine("[Masking] Tier 3: Animation Masking");
        
        // Idle Animation Loop starten
        if (playIdleAnimation != null) {
            playIdleAnimation.run();
        }
    }
    
    /**
     * Wird aufgerufen wenn LLM-Response bereit ist
     * Ersetzt Filler-Message mit echter Response
     */
    public void replaceFillerWithRealMessage(String realMessage) {
        if (!isMasking) {
            // Direkt senden wenn kein Masking aktiv
            if (sendChatPlayer != null) {
                sendChatPlayer.accept(realMessage);
            }
            return;
        }
        
        long elapsedMs = System.currentTimeMillis() - maskingStartTime;
        LOGGER.info("[Masking] LLM ready after " + elapsedMs + "ms");
        
        // Pending Tasks canceln
        cancelPendingTasks();
        
        // Typing Indicator stoppen
        if (typingIndicator != null) {
            typingIndicator.accept("stop");
        }
        
        // Echte Message senden
        // In echter Impl: editMessage(fillerMessageId, realMessage)
        if (sendChatPlayer != null) {
            sendChatPlayer.accept(realMessage);
        }
        
        // Reset
        isMasking = false;
        currentFillerMessageId = null;
    }
    
    /**
     * Cancelt pending Masking Tasks
     */
    private void cancelPendingTasks() {
        if (tier2Task != null && !tier2Task.isDone()) {
            tier2Task.cancel(false);
        }
        if (tier3Task != null && !tier3Task.isDone()) {
            tier3Task.cancel(false);
        }
    }
    
    /**
     * Orchestriert Latency Masking (per-tick Update)
     * Wird vom BipedalAgent in Phase 4 aufgerufen
     */
    public void orchestrateLatency() {
        if (!isMasking) return;
        
        long elapsedMs = System.currentTimeMillis() - maskingStartTime;
        
        // Timeout nach 4 Sekunden
        if (elapsedMs > TIER1_DURATION_MS + TIER2_DURATION_MS + TIER3_DURATION_MS) {
            LOGGER.warning("[Masking] Timeout - LLM took too long");
            cancelMasking();
        }
    }
    
    /**
     * Bricht Masking ab (z.B. bei Timeout)
     */
    public void cancelMasking() {
        if (!isMasking) return;
        
        cancelPendingTasks();
        
        if (typingIndicator != null) {
            typingIndicator.accept("stop");
        }
        
        isMasking = false;
        LOGGER.info("[Masking] Cancelled");
    }
    
    /**
     * Prüft ob Masking aktiv ist
     */
    public boolean isMasking() {
        return isMasking;
    }
    
    /**
     * Gibt vergangene Zeit seit Masking-Start zurück
     */
    public long getElapsedMaskingTime() {
        if (!isMasking) return 0;
        return System.currentTimeMillis() - maskingStartTime;
    }
    
    /**
     * Shutdown
     */
    public void shutdown() {
        cancelPendingTasks();
        scheduler.shutdown();
    }
}
