package net.deinserver.livingorbis.movement;

import net.deinserver.livingorbis.core.BipedalAgent.Location;

import java.util.logging.Logger;

/**
 * MouseLookController - Saccadische Augenbewegung
 * 
 * Simuliert menschliche Augenbewegungen:
 * - Saccade (schneller Flick zum Ziel, ~100ms)
 * - Fixation (Halten mit Mikro-Tremor, 500-1000ms)
 * - Micro-Correction (gelegentliche Über-Korrektur, 5% Chance)
 */
public class MouseLookController {
    
    private static final Logger LOGGER = Logger.getLogger(MouseLookController.class.getName());
    
    // Timing
    private static final long SACCADE_DURATION_MS = 100;
    private static final long FIXATION_MIN_MS = 500;
    private static final long FIXATION_MAX_MS = 1000;
    private static final double MICRO_CORRECTION_CHANCE = 0.05;
    
    // State
    private Location currentDirection;
    private Location targetDirection;
    private Location startDirection;
    
    private LookPhase currentPhase = LookPhase.IDLE;
    private long phaseStartTime;
    private long phaseDuration;
    
    // Tremor parameters
    private double tremorFrequency = 10.0;  // Hz (human hand tremor ~8-12Hz)
    private double tremorAmplitude = 0.02;  // Radians
    
    public MouseLookController() {
        this.currentDirection = new Location(0, 0, 1);
    }
    
    /**
     * Startet Blickrichtungs-Transition
     */
    public void startLookTransition(Location from, Location to) {
        this.startDirection = from;
        this.targetDirection = to;
        this.currentPhase = LookPhase.SACCADE;
        this.phaseStartTime = System.currentTimeMillis();
        this.phaseDuration = SACCADE_DURATION_MS;
        
        LOGGER.fine("[MouseLook] Starting saccade to target");
    }
    
    /**
     * Update-Tick
     */
    public void update(PhysicalMimesisEngine engine) {
        long elapsed = System.currentTimeMillis() - phaseStartTime;
        
        switch (currentPhase) {
            case SACCADE:
                updateSaccade(elapsed, engine);
                break;
            case FIXATION:
                updateFixation(elapsed, engine);
                break;
            case MICRO_CORRECTION:
                updateMicroCorrection(elapsed, engine);
                break;
            case IDLE:
            default:
                // Gelegentlicher random Look
                if (Math.random() < 0.001) { // ~0.1% pro Tick
                    engine.performRandomLook();
                }
                break;
        }
    }
    
    /**
     * Saccade Phase: Schnelle Bewegung zum Ziel
     * Nutzt Cubic Bezier statt linearer Interpolation
     */
    private void updateSaccade(long elapsed, PhysicalMimesisEngine engine) {
        if (elapsed >= phaseDuration) {
            // Saccade abgeschlossen -> Fixation
            currentDirection = targetDirection;
            engine.setCurrentLookDirection(currentDirection);
            
            // Starte Fixation
            currentPhase = LookPhase.FIXATION;
            phaseStartTime = System.currentTimeMillis();
            phaseDuration = FIXATION_MIN_MS + (long)(Math.random() * (FIXATION_MAX_MS - FIXATION_MIN_MS));
            
            LOGGER.fine("[MouseLook] Saccade complete, entering fixation");
            return;
        }
        
        // Cubic Bezier Interpolation (ease-out)
        double t = (double) elapsed / phaseDuration;
        double easedT = cubicEaseOut(t);
        
        currentDirection = lerp(startDirection, targetDirection, easedT);
        engine.setCurrentLookDirection(currentDirection);
    }
    
    /**
     * Fixation Phase: Halten mit Mikro-Tremor
     */
    private void updateFixation(long elapsed, PhysicalMimesisEngine engine) {
        if (elapsed >= phaseDuration) {
            // Fixation abgeschlossen
            
            // Mit 5% Chance: Micro-Correction
            if (Math.random() < MICRO_CORRECTION_CHANCE) {
                currentPhase = LookPhase.MICRO_CORRECTION;
                phaseStartTime = System.currentTimeMillis();
                phaseDuration = 50; // 50ms Korrektur
                LOGGER.fine("[MouseLook] Triggering micro-correction");
            } else {
                currentPhase = LookPhase.IDLE;
            }
            return;
        }
        
        // Mikro-Tremor hinzufügen (simuliert Muskel-Tremor)
        double time = System.currentTimeMillis() / 1000.0;
        double tremorX = Math.sin(time * tremorFrequency * 2 * Math.PI) * tremorAmplitude;
        double tremorY = Math.cos(time * tremorFrequency * 1.3 * 2 * Math.PI) * tremorAmplitude * 0.5;
        
        Location tremorDir = new Location(
            targetDirection.x + tremorX,
            targetDirection.y + tremorY,
            targetDirection.z
        );
        
        engine.setCurrentLookDirection(tremorDir);
    }
    
    /**
     * Micro-Correction Phase: Kleine Korrektur nach Overshoot
     */
    private void updateMicroCorrection(long elapsed, PhysicalMimesisEngine engine) {
        if (elapsed >= phaseDuration) {
            currentPhase = LookPhase.IDLE;
            currentDirection = targetDirection;
            engine.setCurrentLookDirection(currentDirection);
            return;
        }
        
        // Kleine Korrektur zurück zum Ziel
        double t = (double) elapsed / phaseDuration;
        
        // Overshoot-Position
        Location overshoot = new Location(
            targetDirection.x + (Math.random() - 0.5) * 0.1,
            targetDirection.y + (Math.random() - 0.5) * 0.05,
            targetDirection.z
        );
        
        currentDirection = lerp(overshoot, targetDirection, t);
        engine.setCurrentLookDirection(currentDirection);
    }
    
    // ==================== UTILITIES ====================
    
    /**
     * Cubic ease-out für natürliche Bewegung
     */
    private double cubicEaseOut(double t) {
        return 1 - Math.pow(1 - t, 3);
    }
    
    /**
     * Linear interpolation zwischen zwei Directions
     */
    private Location lerp(Location a, Location b, double t) {
        return new Location(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t,
            a.z + (b.z - a.z) * t
        );
    }
    
    /**
     * Aktuelle Phase
     */
    public LookPhase getCurrentPhase() {
        return currentPhase;
    }
    
    // ==================== PHASE ENUM ====================
    
    public enum LookPhase {
        IDLE,
        SACCADE,
        FIXATION,
        MICRO_CORRECTION
    }
}
