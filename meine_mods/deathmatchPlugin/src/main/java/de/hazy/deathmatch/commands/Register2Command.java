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
 * Command: /register2
 * Registers the executing player as Player 2.
 */
public class Register2Command extends AbstractPlayerCommand {

    private final GameManager gameManager;

    public Register2Command(GameManager gameManager) {
        super("register2", "Register as Player 2 for deathmatch");
        this.gameManager = gameManager;
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());

        if (player == null) {
            commandContext.sender().sendMessage(Message.raw("§cCould not identify player!"));
            return;
        }

        String playerName = player.getDisplayName();
        gameManager.registerPlayer2(playerName, player.getUuid());
        player.sendMessage(Message.raw("§a✔ Registered as Player 2!"));
    }
}
