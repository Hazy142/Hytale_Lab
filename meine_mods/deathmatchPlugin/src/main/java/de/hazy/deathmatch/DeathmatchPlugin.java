package de.hazy.deathmatch;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.event.PlayerReadyEvent;
import de.hazy.deathmatch.commands.*;
import de.hazy.deathmatch.listeners.DamageEventHandler;
import de.hazy.deathmatch.listeners.PlayerEventHandler;
import java.util.logging.Level;
import javax.annotation.Nonnull;

/**
 * 1v1 Deathmatch Plugin
 * A CS:GO-style deathmatch game mode for Hytale.
 * 
 * Features:
 * - Player registration and spawn point setup
 * - Automatic kill detection via Damage events
 * - Kit system with inventory save/restore
 * - Teleportation to spawn points
 * - Score tracking with configurable win condition
 */
public class DeathmatchPlugin extends JavaPlugin {

    private GameManager gameManager;
    private PlayerEventHandler playerEventHandler;

    public DeathmatchPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("[Deathmatch] Initializing...");

        // Initialize game manager
        this.gameManager = new GameManager();

        // Initialize event handlers
        this.playerEventHandler = new PlayerEventHandler(gameManager);

        // ===== REGISTER COMMANDS =====
        getCommandRegistry().registerCommand(new Register1Command(gameManager));
        getCommandRegistry().registerCommand(new Register2Command(gameManager));
        getCommandRegistry().registerCommand(new SetSpawn1Command(gameManager));
        getCommandRegistry().registerCommand(new SetSpawn2Command(gameManager));
        getCommandRegistry().registerCommand(new StartCommand(gameManager));
        getCommandRegistry().registerCommand(new StopCommand(gameManager));
        getCommandRegistry().registerCommand(new ScoreCommand(gameManager));
        getCommandRegistry().registerCommand(new Kill1Command(gameManager));
        getCommandRegistry().registerCommand(new Kill2Command(gameManager));
        getCommandRegistry().registerCommand(new RegisterBotCommand(gameManager));

        // ===== REGISTER EVENTS =====
        // Register PlayerReadyEvent for welcome messages
        getEventRegistry().registerGlobal(
                PlayerReadyEvent.class,
                playerEventHandler::onPlayerReady);

        // Register Damage ECS Event for automatic kill detection
        getEntityStoreRegistry().registerSystem(new DamageEventHandler(gameManager));

        getLogger().at(Level.INFO).log("[Deathmatch] Commands registered:");
        getLogger().at(Level.INFO).log("  /register1, /register2 - Register as player");
        getLogger().at(Level.INFO).log("  /setspawn1, /setspawn2 - Set spawn points");
        getLogger().at(Level.INFO).log("  /dmstart, /dmstop - Start/stop match");
        getLogger().at(Level.INFO).log("  /score - Show current score");
        getLogger().at(Level.INFO).log("  /registerbot - Register bot as Player 2");
        getLogger().at(Level.INFO).log("[Deathmatch] Events registered: Damage, PlayerReady");
        getLogger().at(Level.INFO).log("[Deathmatch] Ready! First to 5 kills wins!");
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("[Deathmatch] Shutting down...");
        if (gameManager != null) {
            gameManager.stop();
            gameManager.shutdown();
        }
    }
}
