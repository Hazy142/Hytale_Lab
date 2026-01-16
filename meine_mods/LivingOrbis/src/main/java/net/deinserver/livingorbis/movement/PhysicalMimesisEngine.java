package net.deinserver.livingorbis.movement;

import net.deinserver.livingorbis.core.BipedalAgent.Location;

import java.util.logging.Logger;

/**
 * PhysicalMimesisEngine - Zentrale Koordination aller Physical Mimesis Komponenten
 * 
 * Kombiniert:
 * - ProceduralPathfinding (Lazy A* mit intentionalen Fehlern)
 * - MouseLookController (Saccadische Augenbewegung)
 * - IdleAnimationController (Fidgeting & Breathing)
 * 
 * Ziel: Bewegungen, die weder perfekt noch zufällig sind - sondern MENSCHLICH
 */
public class PhysicalMimesisEngine {
    
    private static final Logger LOGGER = Logger.getLogger(PhysicalMimesisEngine.class.getName());
    
    // Sub-Components
    private final ProceduralPathfinding pathfinding;
    private final MouseLookController mouseController;
    private final IdleAnimationController idleController;
    
    // Current State
    private Location currentPosition;
    private Location currentLookDirection;
    private Location targetPosition;
    private Location targetLookDirection;
    
    // Movement State
    private boolean isMoving = false;
    private long movementStartTime;
    private double movementSpeed = 4.3; // blocks per second (walking speed)
    
    public PhysicalMimesisEngine(Location initialPosition) {
        this.pathfinding = new ProceduralPathfinding();
        this.mouseController = new MouseLookController();
        this.idleController = new IdleAnimationController();
        
        this.currentPosition = initialPosition;
        this.currentLookDirection = new Location(0, 0, 1); // Looking forward
        
        LOGGER.info("[PhysicalMimesis] Engine initialized at " + initialPosition);
    }
    
    /**
     * Update-Tick - wird jeden Frame aufgerufen
     */
    public void update() {
        if (isMoving) {
            updateMovement();
        } else {
            idleController.update(this);
        }
        
        mouseController.update(this);
    }
    
    // ==================== MOVEMENT ====================
    
    /**
     * Startet Bewegung zu einem Ziel
     */
    public void moveToTarget(Location target) {
        if (target == null) return;
        
        this.targetPosition = target;
        this.isMoving = true;
        this.movementStartTime = System.currentTimeMillis();
        
        // Pathfinding mit intentionalen Fehlern
        pathfinding.computePath(currentPosition, target);
        
        LOGGER.fine("[PhysicalMimesis] Moving to " + target);
    }
    
    /**
     * Aktualisiert die Bewegung entlang des Pfades
     */
    private void updateMovement() {
        Location nextWaypoint = pathfinding.getNextWaypoint();
        
        if (nextWaypoint == null) {
            // Ziel erreicht
            isMoving = false;
            LOGGER.fine("[PhysicalMimesis] Reached destination");
            return;
        }
        
        // Bewegung mit menschlichen Unvollkommenheiten
        double distance = distanceTo(nextWaypoint);
        double stepSize = movementSpeed * 0.05; // 50ms tick = 0.05s
        
        if (distance < stepSize) {
            // Waypoint erreicht
            currentPosition = nextWaypoint;
            pathfinding.advanceWaypoint();
            
            // Gelegentliches Overshooting (10% Chance)
            if (Math.random() < 0.10) {
                applyOvershooting();
            }
        } else {
            // Bewegung zum Waypoint mit Perlin Noise Jitter
            double dx = (nextWaypoint.x - currentPosition.x) / distance * stepSize;
            double dy = (nextWaypoint.y - currentPosition.y) / distance * stepSize;
            double dz = (nextWaypoint.z - currentPosition.z) / distance * stepSize;
            
            // Jitter hinzufügen
            dx += (Math.random() - 0.5) * 0.05;
            dz += (Math.random() - 0.5) * 0.05;
            
            currentPosition = new Location(
                currentPosition.x + dx,
                currentPosition.y + dy,
                currentPosition.z + dz
            );
        }
        
        // Blickrichtung zum Ziel
        lookAt(nextWaypoint);
    }
    
    /**
     * Simuliert Overshooting (menschlicher Fehler)
     */
    private void applyOvershooting() {
        double overshootAmount = 0.3 + Math.random() * 0.5; // 0.3-0.8 Blöcke
        
        // Overshoot in Bewegungsrichtung
        if (targetPosition != null) {
            double dx = targetPosition.x - currentPosition.x;
            double dz = targetPosition.z - currentPosition.z;
            double dist = Math.sqrt(dx*dx + dz*dz);
            
            if (dist > 0) {
                currentPosition = new Location(
                    currentPosition.x + (dx/dist) * overshootAmount,
                    currentPosition.y,
                    currentPosition.z + (dz/dist) * overshootAmount
                );
                LOGGER.fine("[PhysicalMimesis] Applied overshooting");
            }
        }
    }
    
    /**
     * Führt Ausweichmanöver aus
     */
    public void executeDodge(Location direction) {
        if (direction == null) return;
        
        double dodgeDistance = 2.0 + Math.random() * 1.0; // 2-3 Blöcke
        
        currentPosition = new Location(
            currentPosition.x + direction.x * dodgeDistance,
            currentPosition.y,
            currentPosition.z + direction.z * dodgeDistance
        );
        
        LOGGER.fine("[PhysicalMimesis] Dodge executed");
    }
    
    // ==================== LOOK DIRECTION ====================
    
    /**
     * Startet Blickrichtungswechsel zu einem Ziel
     */
    public void lookAt(Location target) {
        if (target == null) return;
        
        // Berechne Richtung
        double dx = target.x - currentPosition.x;
        double dy = target.y - currentPosition.y;
        double dz = target.z - currentPosition.z;
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        
        if (dist > 0) {
            targetLookDirection = new Location(dx/dist, dy/dist, dz/dist);
            mouseController.startLookTransition(currentLookDirection, targetLookDirection);
        }
    }
    
    /**
     * Aktualisiert die aktuelle Blickrichtung
     */
    public void setCurrentLookDirection(Location direction) {
        this.currentLookDirection = direction;
    }
    
    // ==================== IDLE BEHAVIOR ====================
    
    /**
     * Triggert Idle-Verhalten manuell
     */
    public void triggerIdleBehavior() {
        idleController.triggerRandomFidget(this);
    }
    
    /**
     * Führt einen Blickrichtungswechsel aus (für Idle)
     */
    public void performRandomLook() {
        double yaw = (Math.random() - 0.5) * 2.0; // -1 to 1
        double pitch = (Math.random() - 0.5) * 0.5; // -0.25 to 0.25
        
        targetLookDirection = new Location(yaw, pitch, 1);
        mouseController.startLookTransition(currentLookDirection, targetLookDirection);
    }
    
    /**
     * Springt (Fidgeting)
     */
    public void performJump() {
        // TODO: Hytale Jump API
        LOGGER.fine("[PhysicalMimesis] Performing jump");
    }
    
    /**
     * Wechselt Hotbar-Slot (Fidgeting)
     */
    public void cycleHotbar() {
        // TODO: Hytale Inventory API
        LOGGER.fine("[PhysicalMimesis] Cycling hotbar");
    }
    
    // ==================== UTILITIES ====================
    
    private double distanceTo(Location target) {
        double dx = target.x - currentPosition.x;
        double dy = target.y - currentPosition.y;
        double dz = target.z - currentPosition.z;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }
    
    // Getters
    public Location getCurrentPosition() { return currentPosition; }
    public Location getCurrentLookDirection() { return currentLookDirection; }
    public boolean isMoving() { return isMoving; }
    public IdleAnimationController getIdleController() { return idleController; }
}
