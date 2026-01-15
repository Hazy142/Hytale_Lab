package de.hazy.deathmatch.game;

import java.util.UUID;

public class Arena {
    private final String id;
    private Location spawnA;
    private Location spawnB;
    private GameState state;

    private UUID playerA;
    private UUID playerB;
    private String playerAName;
    private String playerBName;

    private int scoreA;
    private int scoreB;

    private int timer;

    public Arena(String id) {
        this.id = id;
        this.state = GameState.WAITING;
    }

    public String getId() {
        return id;
    }

    public void setSpawnA(Location loc) {
        this.spawnA = loc;
    }

    public void setSpawnB(Location loc) {
        this.spawnB = loc;
    }

    public Location getSpawnA() {
        return spawnA;
    }

    public Location getSpawnB() {
        return spawnB;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public UUID getPlayerA() {
        return playerA;
    }

    public UUID getPlayerB() {
        return playerB;
    }

    public String getPlayerAName() {
        return playerAName;
    }

    public String getPlayerBName() {
        return playerBName;
    }

    public int getScoreA() {
        return scoreA;
    }

    public int getScoreB() {
        return scoreB;
    }

    public void registerPlayerA(UUID uuid, String name) {
        this.playerA = uuid;
        this.playerAName = name;
    }

    public void registerPlayerB(UUID uuid, String name) {
        this.playerB = uuid;
        this.playerBName = name;
    }

    public void addScoreA() {
        scoreA++;
    }

    public void addScoreB() {
        scoreB++;
    }

    public void resetScores() {
        scoreA = 0;
        scoreB = 0;
    }

    public void reset() {
        state = GameState.WAITING;
        scoreA = 0;
        scoreB = 0;
        // Don't clear spawns, but maybe clear player refs if we want persistent arenas
        // For simple 1v1, we might clear players
        playerA = null;
        playerB = null;
        playerAName = null;
        playerBName = null;
    }

    public boolean isConfigured() {
        return spawnA != null && spawnB != null;
    }

    public boolean isFull() {
        return playerA != null && playerB != null;
    }
}
