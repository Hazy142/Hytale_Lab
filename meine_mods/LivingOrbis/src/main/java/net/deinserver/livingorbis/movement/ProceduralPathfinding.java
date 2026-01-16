package net.deinserver.livingorbis.movement;

import net.deinserver.livingorbis.core.BipedalAgent.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * ProceduralPathfinding - Lazy A* mit intentionalen Fehlern
 * 
 * Erzeugt Pfade die menschlich aussehen:
 * - Catmull-Rom Smoothing (keine harten Winkel)
 * - Overshooting (10% Chance)
 * - Sub-optimale Routen (bevorzugt bekannte Wege)
 * - Perlin Noise Jitter
 */
public class ProceduralPathfinding {
    
    private static final Logger LOGGER = Logger.getLogger(ProceduralPathfinding.class.getName());
    
    private List<Location> currentPath = new ArrayList<>();
    private int currentWaypointIndex = 0;
    
    // Configuration
    private double jitterAmount = 0.05;         // 5% Perlin noise
    private double overshootChance = 0.10;      // 10% Overshooting-Chance
    private double suboptimalChance = 0.15;     // 15% Chance für Umwege
    
    /**
     * Berechnet einen Pfad mit menschlichen Unvollkommenheiten
     */
    public void computePath(Location start, Location goal) {
        currentPath.clear();
        currentWaypointIndex = 0;
        
        // Schritt 1: Einfacher Pfad (Grid-basiert)
        List<Location> rawPath = computeRawPath(start, goal);
        
        // Schritt 2: Catmull-Rom Smoothing
        List<Location> smoothPath = applyCatmullRomSmoothing(rawPath);
        
        // Schritt 3: Intentionale Fehler einfügen
        smoothPath = injectOvershootingWaypoints(smoothPath);
        smoothPath = addPerlinNoiseJitter(smoothPath);
        
        // Schritt 4: Ggf. sub-optimale Route wählen
        if (Math.random() < suboptimalChance) {
            smoothPath = addDetour(smoothPath);
        }
        
        currentPath = smoothPath;
        LOGGER.fine("[Pathfinding] Computed path with " + currentPath.size() + " waypoints");
    }
    
    /**
     * Einfacher Grid-Pfad (A* Vereinfacht)
     */
    private List<Location> computeRawPath(Location start, Location goal) {
        List<Location> path = new ArrayList<>();
        path.add(start);
        
        // Einfache Linie mit Zwischenpunkten alle 2 Blöcke
        double distance = Math.sqrt(
            Math.pow(goal.x - start.x, 2) + 
            Math.pow(goal.y - start.y, 2) + 
            Math.pow(goal.z - start.z, 2)
        );
        
        int numWaypoints = Math.max(2, (int)(distance / 2.0));
        
        for (int i = 1; i < numWaypoints; i++) {
            double t = (double) i / numWaypoints;
            path.add(new Location(
                start.x + (goal.x - start.x) * t,
                start.y + (goal.y - start.y) * t,
                start.z + (goal.z - start.z) * t
            ));
        }
        
        path.add(goal);
        return path;
    }
    
    /**
     * Catmull-Rom Spline Smoothing
     * Konvertiert harte Winkel in flüssige Kurven
     */
    private List<Location> applyCatmullRomSmoothing(List<Location> path) {
        if (path.size() < 4) return path;
        
        List<Location> smoothed = new ArrayList<>();
        smoothed.add(path.get(0));
        
        for (int i = 0; i < path.size() - 3; i++) {
            Location p0 = path.get(i);
            Location p1 = path.get(i + 1);
            Location p2 = path.get(i + 2);
            Location p3 = path.get(i + 3);
            
            // Interpoliere zwischen p1 und p2 mit 3 Zwischenpunkten
            for (double t = 0.25; t < 1.0; t += 0.25) {
                double tt = t * t;
                double ttt = tt * t;
                
                double x = 0.5 * ((2*p1.x) + 
                                 (-p0.x + p2.x) * t + 
                                 (2*p0.x - 5*p1.x + 4*p2.x - p3.x) * tt + 
                                 (-p0.x + 3*p1.x - 3*p2.x + p3.x) * ttt);
                
                double y = 0.5 * ((2*p1.y) + 
                                 (-p0.y + p2.y) * t + 
                                 (2*p0.y - 5*p1.y + 4*p2.y - p3.y) * tt + 
                                 (-p0.y + 3*p1.y - 3*p2.y + p3.y) * ttt);
                
                double z = 0.5 * ((2*p1.z) + 
                                 (-p0.z + p2.z) * t + 
                                 (2*p0.z - 5*p1.z + 4*p2.z - p3.z) * tt + 
                                 (-p0.z + 3*p1.z - 3*p2.z + p3.z) * ttt);
                
                smoothed.add(new Location(x, y, z));
            }
        }
        
        smoothed.add(path.get(path.size() - 1));
        return smoothed;
    }
    
    /**
     * Fügt Overshooting-Waypoints ein
     * Simuliert menschliches "zu weit laufen" mit anschließender Korrektur
     */
    private List<Location> injectOvershootingWaypoints(List<Location> path) {
        List<Location> result = new ArrayList<>();
        
        for (int i = 0; i < path.size(); i++) {
            result.add(path.get(i));
            
            // Bei manchen Waypoints Overshooting einfügen
            if (i < path.size() - 1 && Math.random() < overshootChance) {
                Location current = path.get(i);
                Location next = path.get(i + 1);
                
                // Overshoot: Etwas weiter als nötig
                double overshootFactor = 1.1 + Math.random() * 0.2; // 110-130%
                Location overshoot = new Location(
                    current.x + (next.x - current.x) * overshootFactor,
                    next.y,
                    current.z + (next.z - current.z) * overshootFactor
                );
                
                result.add(overshoot);
                LOGGER.fine("[Pathfinding] Added overshooting waypoint");
            }
        }
        
        return result;
    }
    
    /**
     * Fügt Perlin-Noise-ähnlichen Jitter hinzu
     */
    private List<Location> addPerlinNoiseJitter(List<Location> path) {
        List<Location> result = new ArrayList<>();
        
        for (int i = 0; i < path.size(); i++) {
            Location p = path.get(i);
            
            // Ersten und letzten Punkt nicht verändern
            if (i == 0 || i == path.size() - 1) {
                result.add(p);
                continue;
            }
            
            // Zufälliger Jitter
            double jitterX = (Math.random() - 0.5) * 2 * jitterAmount;
            double jitterZ = (Math.random() - 0.5) * 2 * jitterAmount;
            
            result.add(new Location(
                p.x + jitterX,
                p.y,
                p.z + jitterZ
            ));
        }
        
        return result;
    }
    
    /**
     * Fügt einen kleinen Umweg ein (sub-optimal)
     */
    private List<Location> addDetour(List<Location> path) {
        if (path.size() < 3) return path;
        
        List<Location> result = new ArrayList<>();
        int detourIndex = 1 + (int)(Math.random() * (path.size() - 2));
        
        for (int i = 0; i < path.size(); i++) {
            if (i == detourIndex) {
                Location p = path.get(i);
                
                // Seitlicher Versatz (1-2 Blöcke)
                double offset = 1.0 + Math.random() * 1.0;
                double direction = Math.random() < 0.5 ? 1 : -1;
                
                // Detour-Punkt
                result.add(new Location(
                    p.x + offset * direction,
                    p.y,
                    p.z
                ));
                
                LOGGER.fine("[Pathfinding] Added detour waypoint");
            }
            result.add(path.get(i));
        }
        
        return result;
    }
    
    /**
     * Gibt den nächsten Waypoint zurück
     */
    public Location getNextWaypoint() {
        if (currentWaypointIndex >= currentPath.size()) {
            return null;
        }
        return currentPath.get(currentWaypointIndex);
    }
    
    /**
     * Geht zum nächsten Waypoint
     */
    public void advanceWaypoint() {
        currentWaypointIndex++;
    }
    
    /**
     * Prüft ob Pfad abgeschlossen
     */
    public boolean isComplete() {
        return currentWaypointIndex >= currentPath.size();
    }
    
    /**
     * Gibt verbleibende Distanz zurück
     */
    public double getRemainingDistance() {
        double total = 0;
        for (int i = currentWaypointIndex; i < currentPath.size() - 1; i++) {
            Location a = currentPath.get(i);
            Location b = currentPath.get(i + 1);
            total += Math.sqrt(Math.pow(b.x-a.x, 2) + Math.pow(b.y-a.y, 2) + Math.pow(b.z-a.z, 2));
        }
        return total;
    }
}
