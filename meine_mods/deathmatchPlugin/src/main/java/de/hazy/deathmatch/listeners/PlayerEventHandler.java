package de.hazy.deathmatch.listeners;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Player;
import com.hypixel.hytale.server.core.event.PlayerReadyEvent;
import de.hazy.deathmatch.GameManager;

/**
 * Handles player connection events.
 */
public class PlayerEventHandler {

    private final GameManager gameManager;

    public PlayerEventHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Called when a player is ready (fully connected to the server).
     */
    public void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();

        // Welcome message
        player.sendMessage(Message.raw(""));
        player.sendMessage(Message.raw("§6§l=== DEATHMATCH SERVER ==="));
        player.sendMessage(Message.raw("§7Commands:"));
        player.sendMessage(Message.raw("§f/register1 §7- Register as Player 1"));
        player.sendMessage(Message.raw("§f/register2 §7- Register as Player 2"));
        player.sendMessage(Message.raw("§f/setspawn1 §7- Set spawn for Player 1"));
        player.sendMessage(Message.raw("§f/setspawn2 §7- Set spawn for Player 2"));
        player.sendMessage(Message.raw("§f/dmstart §7- Start the match"));
        player.sendMessage(Message.raw("§f/dmstop §7- Stop the match"));
        player.sendMessage(Message.raw("§f/score §7- Show current score"));
        player.sendMessage(Message.raw("§6§l========================"));
        player.sendMessage(Message.raw(""));
    }
}
