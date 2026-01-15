package de.hazy.deathmatch.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.ecs.Store;
import com.hypixel.hytale.server.core.ecs.Ref;
import com.hypixel.hytale.server.core.ecs.EntityStore;
import com.hypixel.hytale.server.core.entity.Player;
import com.hypixel.hytale.server.core.entity.PlayerRef;
import com.hypixel.hytale.server.core.world.World;
import de.hazy.deathmatch.GameManager;
import de.hazy.deathmatch.game.GameState;
import javax.annotation.Nonnull;

/**
 * Command: /dmstart
 * Starts the deathmatch game.
 */
public class StartCommand extends AbstractPlayerCommand {

    private final GameManager gameManager;

    public StartCommand(GameManager gameManager) {
        super("dmstart", "Start a deathmatch game");
        this.gameManager = gameManager;
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());

        if (gameManager.getState() == GameState.LIVE) {
            player.sendMessage(Message.raw("§cMatch already running! /dmstop first."));
            return;
        }

        if (!gameManager.hasSpawns()) {
            player.sendMessage(Message.raw("§cRun /setspawn1 and /setspawn2 first!"));
            return;
        }

        // Pass the world to GameManager for teleportation
        boolean started = gameManager.start(world);

        if (started) {
            player.sendMessage(Message.raw(""));
            player.sendMessage(Message.raw("§6§l====== DEATHMATCH STARTED ======"));
            player.sendMessage(Message.raw("§7First to §f" + gameManager.getKillsToWin() + " §7kills wins!"));
            player.sendMessage(Message.raw("§6§l================================="));
            player.sendMessage(Message.raw(""));
        } else {
            player.sendMessage(Message.raw("§cFailed to start match. Register both players first!"));
        }
    }
}
