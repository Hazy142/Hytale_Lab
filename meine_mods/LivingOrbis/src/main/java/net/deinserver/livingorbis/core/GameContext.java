package net.deinserver.livingorbis.core;

import net.deinserver.livingorbis.core.BipedalAgent.GamePhase;
import net.deinserver.livingorbis.core.BipedalAgent.Location;

import java.util.HashMap;
import java.util.Map;

/**
 * GameContext - Enthält den aktuellen Spielzustand für Decision-Making
 */
public class GameContext {
    
    private long timestamp;
    private GamePhase currentPhase = GamePhase.DAY;
    private boolean directlyAddressed = false;
    private boolean hasImminentThreat = false;
    private Map<String, Location> playerPositions = new HashMap<>();
    private Map<String, String> recentDeaths = new HashMap<>();
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public GamePhase getCurrentPhase() {
        return currentPhase;
    }
    
    public void setCurrentPhase(GamePhase currentPhase) {
        this.currentPhase = currentPhase;
    }
    
    public boolean isVotingPhase() {
        return currentPhase == GamePhase.VOTING || currentPhase == GamePhase.EMERGENCY;
    }
    
    public boolean isDirectlyAddressed() {
        return directlyAddressed;
    }
    
    public void setDirectlyAddressed(boolean directlyAddressed) {
        this.directlyAddressed = directlyAddressed;
    }
    
    public boolean hasImminentThreat() {
        return hasImminentThreat;
    }
    
    public void setHasImminentThreat(boolean hasImminentThreat) {
        this.hasImminentThreat = hasImminentThreat;
    }
    
    public void updatePlayerPosition(String playerId, Location location) {
        playerPositions.put(playerId, location);
    }
    
    public Location getPlayerPosition(String playerId) {
        return playerPositions.get(playerId);
    }
    
    public void recordDeath(String victimId, String killerId) {
        recentDeaths.put(victimId, killerId);
    }
    
    public boolean hasChangedSince(long lastTimestamp) {
        return timestamp > lastTimestamp || directlyAddressed;
    }
    
    public void reset() {
        directlyAddressed = false;
        hasImminentThreat = false;
    }
}
