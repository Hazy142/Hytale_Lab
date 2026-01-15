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
 * Command: /kill2
 * Manually registers a kill for Player 2 (for testing/admin).
 */
public class Kill2Command extends AbstractPlayerCommand {

    private final GameManager gameManager;

    public Kill2Command(GameManager gameManager) {
        super("kill2", "Register a kill for Player 2");
        this.gameManager = gameManager;
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());

        if (gameManager.getState() != GameState.LIVE) {
            player.sendMessage(Message.raw("§cNo match in progress! Use /dmstart first."));
            return;
        }

        boolean matchEnded = gameManager.registerKill("2");

        String p1 = gameManager.getPlayer1Name();
        String p2 = gameManager.getPlayer2Name();
        int s1 = gameManager.getPlayer1Score();
        int s2 = gameManager.getPlayer2Score();

        if (matchEnded) {
            String winner = s1 > s2 ? p1 : p2;
            player.sendMessage(Message.raw(""));
            player.sendMessage(Message.raw("§6§l========= MATCH OVER! ========="));
            player.sendMessage(Message.raw("§e§lWINNER: §f§l" + winner));
            player.sendMessage(Message.raw("§7Final: §f" + s1 + " - " + s2));
            player.sendMessage(Message.raw("§6§l================================"));
            player.sendMessage(Message.raw("§7Use /dmstart to play again!"));
        } else {
            player.sendMessage(Message.raw("§a+1 " + p2 + "! §7Score: §f" + s1 + " - " + s2));
        }
    }
}
