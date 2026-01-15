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
 * Command: /score
 * Shows the current match score.
 */
public class ScoreCommand extends AbstractPlayerCommand {

    private final GameManager gameManager;

    public ScoreCommand(GameManager gameManager) {
        super("score", "Show current deathmatch score");
        this.gameManager = gameManager;
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());

        String p1 = gameManager.getPlayer1Name();
        String p2 = gameManager.getPlayer2Name();
        int s1 = gameManager.getPlayer1Score();
        int s2 = gameManager.getPlayer2Score();

        player.sendMessage(Message.raw("§6Score: §f" + p1 + " " + s1 + " - " + s2 + " " + p2));
    }
}
