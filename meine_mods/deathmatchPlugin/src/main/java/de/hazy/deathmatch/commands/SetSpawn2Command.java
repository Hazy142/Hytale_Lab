package de.hazy.deathmatch.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.ecs.Store;
import com.hypixel.hytale.server.core.ecs.Ref;
import com.hypixel.hytale.server.core.ecs.EntityStore;
import com.hypixel.hytale.server.core.ecs.component.TransformComponent;
import com.hypixel.hytale.server.core.entity.Player;
import com.hypixel.hytale.server.core.entity.PlayerRef;
import com.hypixel.hytale.server.core.entity.EntityModule;
import com.hypixel.hytale.server.core.world.World;
import de.hazy.deathmatch.GameManager;
import javax.annotation.Nonnull;

/**
 * Command: /setspawn2
 * Sets spawn for Player 2 at sender's current location.
 */
public class SetSpawn2Command extends AbstractPlayerCommand {

    private final GameManager gameManager;

    public SetSpawn2Command(GameManager gameManager) {
        super("setspawn2", "Set spawn point for Player 2");
        this.gameManager = gameManager;
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        TransformComponent transform = store.getComponent(ref, EntityModule.get().getTransformComponentType());

        if (player == null || transform == null) {
            commandContext.sender().sendMessage(Message.raw("§cCould not get player position!"));
            return;
        }

        var position = transform.getPosition();
        gameManager.setSpawn(2, position.x(), position.y(), position.z());
        player.sendMessage(Message.raw("§a✔ Spawn 2 set to your location! (" +
                String.format("%.1f, %.1f, %.1f", position.x(), position.y(), position.z()) + ")"));
    }
}
