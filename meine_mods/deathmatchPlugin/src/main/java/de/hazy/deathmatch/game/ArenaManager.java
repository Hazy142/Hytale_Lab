package de.hazy.deathmatch.game;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.logging.Logger;

public class ArenaManager {
    private final Arena arena;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Logger LOGGER = Logger.getLogger("ArenaManager");

    public ArenaManager() {
        // Initialize default arena
        this.arena = new Arena("default_arena");
    }

    public Arena getArena() {
        return arena;
    }

    public boolean registerPlayer1(UUID uuid, String name) {
        if (arena.getState() != GameState.WAITING)
            return false;
        arena.registerPlayerA(uuid, name);
        return true;
    }

    public boolean registerPlayer2(UUID uuid, String name) {
        if (arena.getState() != GameState.WAITING)
            return false;
        arena.registerPlayerB(uuid, name);
        return true;
    }

    public void setSpawn1(Location loc) {
        arena.setSpawnA(loc);
    }

    public void setSpawn2(Location loc) {
        arena.setSpawnB(loc);
    }

    public boolean startMatch() {
        if (!arena.isConfigured() || !arena.isFull())
            return false;
        if (arena.getState() != GameState.WAITING)
            return false;

        startCountdown();
        return true;
    }

    private void startCountdown() {
        arena.setState(GameState.COUNTDOWN);
        LOGGER.info("[Deathmatch] Countdown started!");

        // 3... 2... 1... GO!
        // For simplicity, we just log here. In a real plugin, we would send titles to
        // players.
        // Since we don't have easy Player object access in this manager without passing
        // it in,
        // we'll rely on the GameManager/Command layer to broadcast messages or we can
        // add broadcast/callback hooks later.

        scheduler.schedule(() -> {
            LOGGER.info("[Deathmatch] 3...");
        }, 0, TimeUnit.SECONDS);

        scheduler.schedule(() -> {
            LOGGER.info("[Deathmatch] 2...");
        }, 1, TimeUnit.SECONDS);

        scheduler.schedule(() -> {
            LOGGER.info("[Deathmatch] 1...");
        }, 2, TimeUnit.SECONDS);

        scheduler.schedule(() -> {
            arena.setState(GameState.LIVE);
            LOGGER.info("[Deathmatch] GO! FIGHT!");
        }, 3, TimeUnit.SECONDS);
    }

    public void stopMatch() {
        arena.reset();
        LOGGER.info("[Deathmatch] Match stopped and arena reset.");
    }

    public void checkWinCondition(int killsToWin) {
        if (arena.getScoreA() >= killsToWin) {
            endMatch(arena.getPlayerAName());
        } else if (arena.getScoreB() >= killsToWin) {
            endMatch(arena.getPlayerBName());
        }
    }

    private void endMatch(String winner) {
        arena.setState(GameState.ENDED);
        LOGGER.info("[Deathmatch] MATCH ENDED! Winner: " + winner);
        // Reset after delay
        scheduler.schedule(() -> {
            stopMatch();
        }, 5, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
