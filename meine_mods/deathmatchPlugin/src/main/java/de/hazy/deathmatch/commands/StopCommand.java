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
 * Command: /dmstop
 * Stops the current deathmatch game.
 */
public class StopCommand extends AbstractPlayerCommand {

    private final GameManager gameManager;

    public StopCommand(GameManager gameManager) {
        super("dmstop", "Stop the current deathmatch game");
        this.gameManager = gameManager;
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());

        if (gameManager.getState() == GameState.WAITING) {
            player.sendMessage(Message.raw("§7No match running."));
            return;
        }

        gameManager.stop();
        player.sendMessage(Message.raw("§cMatch stopped. Scores reset."));
    }
}
