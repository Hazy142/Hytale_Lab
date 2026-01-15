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
import javax.annotation.Nonnull;

/**
 * Command: /registerbot
 * Registers a bot as Player 2 for solo testing.
 */
public class RegisterBotCommand extends AbstractPlayerCommand {

    private final GameManager gameManager;

    public RegisterBotCommand(GameManager gameManager) {
        super("registerbot", "Register a bot as Player 2");
        this.gameManager = gameManager;
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());

        gameManager.registerBot2();
        player.sendMessage(Message.raw("§a[Deathmatch] Bot registered as Player 2!"));
    }
}
