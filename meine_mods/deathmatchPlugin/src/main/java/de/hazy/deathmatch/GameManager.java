package de.hazy.deathmatch;

import com.hypixel.hytale.server.core.ecs.Store;
import com.hypixel.hytale.server.core.ecs.Ref;
import com.hypixel.hytale.server.core.ecs.EntityStore;
import com.hypixel.hytale.server.core.ecs.component.Teleport;
import com.hypixel.hytale.server.core.ecs.component.Transform;
import com.hypixel.hytale.server.core.entity.Player;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.entity.PlayerRef;
import com.hypixel.hytale.server.core.world.World;
import de.hazy.deathmatch.game.Arena;
import de.hazy.deathmatch.game.GameState;
import de.hazy.deathmatch.game.KitManager;
import de.hazy.deathmatch.game.Location;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Manages the 1v1 Deathmatch game state, players, and scoring.
 * Includes teleportation and kit management using official Hytale APIs.
 */
public class GameManager {

    private final KitManager kitManager;
    private final Arena arena;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Logger LOGGER = Logger.getLogger("GameManager");
    private static final int KILLS_TO_WIN = 5;

    // Store references for teleportation
    private World currentWorld;

    public GameManager() {
        this.arena = new Arena("default_arena");
        this.kitManager = new KitManager();
        LOGGER.info("[Deathmatch] GameManager initialized. First to " + KILLS_TO_WIN + " kills wins!");
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public Arena getArena() {
        return arena;
    }

    // ===== SPAWN MANAGEMENT =====

    public void setSpawn(int team, double x, double y, double z) {
        Location loc = new Location(x, y, z);
        if (team == 1) {
            arena.setSpawnA(loc);
            LOGGER.info("[Deathmatch] Spawn 1 set to: " + loc);
        } else if (team == 2) {
            arena.setSpawnB(loc);
            LOGGER.info("[Deathmatch] Spawn 2 set to: " + loc);
        }
    }

    public void setSpawn(int team, Location loc) {
        if (team == 1)
            arena.setSpawnA(loc);
        else if (team == 2)
            arena.setSpawnB(loc);
        LOGGER.info("[Deathmatch] Spawn " + team + " set to: " + loc);
    }

    public boolean hasSpawns() {
        return arena.isConfigured();
    }

    // ===== PLAYER REGISTRATION =====

    public void registerPlayer1(String name, UUID uuid) {
        arena.registerPlayerA(uuid, name);
        LOGGER.info("[Deathmatch] Player 1 registered: " + name);
    }

    public void registerPlayer2(String name, UUID uuid) {
        arena.registerPlayerB(uuid, name);
        LOGGER.info("[Deathmatch] Player 2 registered: " + name);
    }

    public void registerBot2() {
        if (arena.getState() != GameState.WAITING && arena.getState() != GameState.ENDED) {
            return;
        }
        arena.setPlayerB(UUID.randomUUID());
        arena.setPlayerBName("Bot_Player");
        LOGGER.info("[Deathmatch] Bot registered as Player 2.");
    }

    public String getPlayer1Name() {
        return arena.getPlayerAName() != null ? arena.getPlayerAName() : "Player 1";
    }

    public String getPlayer2Name() {
        return arena.getPlayerBName() != null ? arena.getPlayerBName() : "Player 2";
    }

    public UUID getPlayer1Uuid() {
        return arena.getPlayerA();
    }

    public UUID getPlayer2Uuid() {
        return arena.getPlayerB();
    }

    public int getPlayer1Score() {
        return arena.getScoreA();
    }

    public int getPlayer2Score() {
        return arena.getScoreB();
    }

    public boolean hasPlayer1() {
        return arena.getPlayerA() != null;
    }

    public boolean hasPlayer2() {
        return arena.getPlayerB() != null;
    }

    // ===== GAME STATE =====

    public GameState getState() {
        return arena.getState();
    }

    /**
     * Starts the match with teleportation support.
     * 
     * @param world The world to use for teleportation
     * @return true if match started successfully
     */
    public boolean start(World world) {
        if (arena.getState() != GameState.WAITING && arena.getState() != GameState.ENDED)
            return false;
        if (!arena.isConfigured())
            return false;
        if (!arena.isFull())
            return false;

        this.currentWorld = world;
        startCountdown();
        return true;
    }

    /**
     * Legacy start method without world (for backward compatibility).
     */
    public boolean start() {
        return start(null);
    }

    private void startCountdown() {
        arena.setState(GameState.COUNTDOWN);
        LOGGER.info("[Deathmatch] Countdown started!");

        scheduler.schedule(() -> LOGGER.info("[Deathmatch] 3..."), 0, TimeUnit.SECONDS);
        scheduler.schedule(() -> LOGGER.info("[Deathmatch] 2..."), 1, TimeUnit.SECONDS);
        scheduler.schedule(() -> LOGGER.info("[Deathmatch] 1..."), 2, TimeUnit.SECONDS);
        scheduler.schedule(() -> {
            arena.setState(GameState.LIVE);
            LOGGER.info("[Deathmatch] GO! FIGHT!");

            // Teleport players to spawn points and give kits
            teleportAndEquipPlayers();
        }, 3, TimeUnit.SECONDS);
    }

    /**
     * Teleports players to their spawn points and gives them kits.
     */
    private void teleportAndEquipPlayers() {
        if (currentWorld == null) {
            LOGGER.warning("[Deathmatch] No world reference - skipping teleport");
            return;
        }

        UUID player1Uuid = arena.getPlayerA();
        UUID player2Uuid = arena.getPlayerB();
        Location spawn1 = arena.getSpawnA();
        Location spawn2 = arena.getSpawnB();

        // Teleport Player 1
        if (player1Uuid != null && spawn1 != null) {
            PlayerRef playerRef1 = Universe.get().getPlayer(player1Uuid);
            if (playerRef1 != null) {
                Player player1 = playerRef1.getPlayer();
                teleportPlayer(player1, spawn1);
                kitManager.saveInventory(player1);
                kitManager.giveKit(player1);
            }
        }

        // Teleport Player 2 (skip if it's a bot)
        if (player2Uuid != null && spawn2 != null && !getPlayer2Name().equals("Bot_Player")) {
            PlayerRef playerRef2 = Universe.get().getPlayer(player2Uuid);
            if (playerRef2 != null) {
                Player player2 = playerRef2.getPlayer();
                teleportPlayer(player2, spawn2);
                kitManager.saveInventory(player2);
                kitManager.giveKit(player2);
            }
        }
    }

    /**
     * Teleports a player to a specific location using the Teleport component.
     */
    public void teleportPlayer(Player player, Location location) {
        if (player == null || location == null)
            return;

        World world = player.getWorld();
        if (world == null)
            return;

        world.execute(() -> {
            Ref<EntityStore> playerRef = player.getReference();
            if (playerRef == null)
                return;

            Store<EntityStore> store = playerRef.getStore();
            Teleport teleport = new Teleport(new Transform(
                    (int) location.getX(),
                    (int) location.getY(),
                    (int) location.getZ()));

            store.addComponent(playerRef, Teleport.getComponentType(), teleport);
            LOGGER.info("[Deathmatch] Teleported " + player.getDisplayName() + " to " + location);
        });
    }

    /**
     * Teleports a player using coordinates.
     */
    public void teleportPlayer(Player player, double x, double y, double z) {
        teleportPlayer(player, new Location(x, y, z));
    }

    public void stop() {
        // Restore inventories for both players
        if (currentWorld != null) {
            UUID player1Uuid = arena.getPlayerA();
            UUID player2Uuid = arena.getPlayerB();

            if (player1Uuid != null) {
                PlayerRef playerRef1 = Universe.get().getPlayer(player1Uuid);
                if (playerRef1 != null) {
                    kitManager.restoreInventory(playerRef1.getPlayer());
                }
            }
            if (player2Uuid != null && !getPlayer2Name().equals("Bot_Player")) {
                PlayerRef playerRef2 = Universe.get().getPlayer(player2Uuid);
                if (playerRef2 != null) {
                    kitManager.restoreInventory(playerRef2.getPlayer());
                }
            }
        }

        arena.reset();
        currentWorld = null;
        LOGGER.info("[Deathmatch] Match stopped.");
    }

    // ===== SCORING =====

    public boolean registerKill(String killerIdentifier) {
        if (arena.getState() != GameState.LIVE)
            return false;

        if (killerIdentifier.equals("1") || killerIdentifier.equalsIgnoreCase(getPlayer1Name())) {
            arena.addScoreA();
            LOGGER.info("[Deathmatch] " + getPlayer1Name() + " scores!");
        } else if (killerIdentifier.equals("2") || killerIdentifier.equalsIgnoreCase(getPlayer2Name())) {
            arena.addScoreB();
            LOGGER.info("[Deathmatch] " + getPlayer2Name() + " scores!");
        } else {
            return false;
        }

        // Check for win condition
        return checkWin();
    }

    private boolean checkWin() {
        if (arena.getScoreA() >= KILLS_TO_WIN) {
            endMatch(getPlayer1Name());
            return true;
        } else if (arena.getScoreB() >= KILLS_TO_WIN) {
            endMatch(getPlayer2Name());
            return true;
        }
        return false;
    }

    private void endMatch(String winnerName) {
        arena.setState(GameState.ENDED);
        LOGGER.info("[Deathmatch] MATCH OVER! Winner: " + winnerName);
        LOGGER.info("[Deathmatch] Final Score: " + arena.getScoreA() + " - " + arena.getScoreB());

        // Restore inventories
        stop();
    }

    public int getKillsToWin() {
        return KILLS_TO_WIN;
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
