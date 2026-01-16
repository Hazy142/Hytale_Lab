package net.deinserver.livingorbis.movement;

import java.util.logging.Logger;

/**
 * IdleAnimationController - Fidgeting & Procedural Idle Behavior
 * 
 * Simuliert menschliches Verhalten wenn der Agent nichts tut:
 * - Procedural Breathing (kontinuierlich)
 * - Stochastic Fidgets (basierend auf boredom_level)
 * - Fidget-Typen: Hotbar wechseln, Springen, Umdrehen, Hoch/Runter schauen
 */
public class IdleAnimationController {
    
    private static final Logger LOGGER = Logger.getLogger(IdleAnimationController.class.getName());
    
    // Timing
    private long lastMovementTime = System.currentTimeMillis();
    private long lastFidgetTime = 0;
    
    // Breathing
    private double breathPhase = 0;
    private static final double BREATH_FREQUENCY = 0.2; // ~0.2 Hz = 5 Sekunden pro Atemzug
    private static final double BREATH_AMPLITUDE = 0.1; // 0.1 Einheiten Bewegung
    
    // Boredom
    private double boredomLevel = 0;
    private static final double BOREDOM_INCREASE_RATE = 0.02; // Pro Sekunde
    private static final double BOREDOM_DECREASE_ON_ACTION = 0.5;
    
    // Fidget Configuration
    private static final long MIN_FIDGET_INTERVAL_MS = 2000; // Mindestens 2s zwischen Fidgets
    
    public IdleAnimationController() {
        this.lastMovementTime = System.currentTimeMillis();
    }
    
    /**
     * Update-Tick
     */
    public void update(PhysicalMimesisEngine engine) {
        long currentTime = System.currentTimeMillis();
        
        // Breathing aktualisieren
        updateBreathing(engine);
        
        // Boredom erhöhen
        double deltaSeconds = (currentTime - lastMovementTime) / 1000.0;
        boredomLevel = Math.min(1.0, boredomLevel + BOREDOM_INCREASE_RATE * 0.05); // 0.05 = 50ms tick
        
        // Stochastic Fidget Check
        if (currentTime - lastFidgetTime > MIN_FIDGET_INTERVAL_MS) {
            double fidgetProbability = boredomLevel * 0.01; // Max 1% pro Tick bei max boredom
            
            if (Math.random() < fidgetProbability) {
                performRandomFidget(engine);
                lastFidgetTime = currentTime;
            }
        }
    }
    
    /**
     * Procedural Breathing Animation
     */
    private void updateBreathing(PhysicalMimesisEngine engine) {
        // Sinusförmiges Atmen
        breathPhase += BREATH_FREQUENCY * 0.05 * 2 * Math.PI; // 50ms tick
        if (breathPhase > 2 * Math.PI) {
            breathPhase -= 2 * Math.PI;
        }
        
        double breathOffset = Math.sin(breathPhase) * BREATH_AMPLITUDE;
        
        // TODO: Apply breath offset to camera/head position
        // Dies würde in Hytale über die Avatar-Animation erfolgen
    }
    
    /**
     * Führt einen zufälligen Fidget aus
     */
    public void performRandomFidget(PhysicalMimesisEngine engine) {
        FidgetType fidget = selectRandomFidget();
        
        switch (fidget) {
            case LOOK_AROUND:
                engine.performRandomLook();
                LOGGER.fine("[Idle] Fidget: Look around");
                break;
                
            case JUMP:
                engine.performJump();
                LOGGER.fine("[Idle] Fidget: Jump");
                break;
                
            case HOTBAR_CYCLE:
                engine.cycleHotbar();
                LOGGER.fine("[Idle] Fidget: Cycle hotbar");
                break;
                
            case TURN_180:
                performTurn180(engine);
                LOGGER.fine("[Idle] Fidget: Turn 180°");
                break;
                
            case LOOK_UP_DOWN:
                performLookUpDown(engine);
                LOGGER.fine("[Idle] Fidget: Look up/down");
                break;
                
            case CROUCH_JUMP:
                performCrouchJump(engine);
                LOGGER.fine("[Idle] Fidget: Crouch-jump");
                break;
        }
        
        // Boredom reduzieren
        boredomLevel = Math.max(0, boredomLevel - BOREDOM_DECREASE_ON_ACTION);
    }
    
    /**
     * Triggert manuell einen Fidget
     */
    public void triggerRandomFidget(PhysicalMimesisEngine engine) {
        performRandomFidget(engine);
        lastFidgetTime = System.currentTimeMillis();
    }
    
    /**
     * Wählt zufälligen Fidget basierend auf Gewichtung
     */
    private FidgetType selectRandomFidget() {
        double roll = Math.random();
        
        // Gewichtete Auswahl
        if (roll < 0.35) return FidgetType.LOOK_AROUND;       // 35%
        if (roll < 0.55) return FidgetType.HOTBAR_CYCLE;     // 20%
        if (roll < 0.70) return FidgetType.JUMP;              // 15%
        if (roll < 0.82) return FidgetType.LOOK_UP_DOWN;     // 12%
        if (roll < 0.92) return FidgetType.TURN_180;          // 10%
        return FidgetType.CROUCH_JUMP;                         // 8%
    }
    
    /**
     * 180° Drehung
     */
    private void performTurn180(PhysicalMimesisEngine engine) {
        // TODO: Initiate 180° turn via lookAt
        // engine.lookAt(behindPosition);
    }
    
    /**
     * Hoch/Runter schauen
     */
    private void performLookUpDown(PhysicalMimesisEngine engine) {
        // Zufällig hoch oder runter
        double pitch = Math.random() < 0.5 ? 0.5 : -0.5; // Radians
        // TODO: Apply via MouseLookController
    }
    
    /**
     * Crouch-Jump (Teabagging-ähnlich)
     */
    private void performCrouchJump(PhysicalMimesisEngine engine) {
        // TODO: Crouch then jump
        engine.performJump();
    }
    
    /**
     * Signalisiert dass Agent sich bewegt hat (reset boredom)
     */
    public void onMovement() {
        lastMovementTime = System.currentTimeMillis();
        boredomLevel = Math.max(0, boredomLevel - BOREDOM_DECREASE_ON_ACTION);
    }
    
    /**
     * Gibt aktuelles Boredom-Level zurück
     */
    public double getBoredomLevel() {
        return boredomLevel;
    }
    
    // ==================== FIDGET TYPES ====================
    
    public enum FidgetType {
        LOOK_AROUND,    // Umherschauen
        JUMP,           // Zufälliges Springen
        HOTBAR_CYCLE,   // Hotbar-Slot wechseln
        TURN_180,       // 180° Drehung
        LOOK_UP_DOWN,   // Hoch/Runter schauen
        CROUCH_JUMP     // Crouch-Jump
    }
}
