package de.hazy.deathmatch.util;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Player;
import com.hypixel.hytale.server.core.entity.PlayerRef;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.network.PacketHandler;
import com.hypixel.hytale.server.core.network.packet.ItemWithAllMetadata;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import java.util.logging.Logger;

/**
 * Utility class for sending game notifications to players.
 * Uses the NotificationUtil API for item-pickup style notifications.
 */
public class DeathmatchNotifications {

    private static final Logger LOGGER = Logger.getLogger("DeathmatchNotifications");

    /**
     * Sends a kill notification to a player.
     */
    public static void sendKillNotification(Player player, String victimName) {
        if (player == null)
            return;

        try {
            PlayerRef playerRef = Universe.get().getPlayer(player.getUuid());
            if (playerRef == null)
                return;

            PacketHandler packetHandler = playerRef.getPacketHandler();

            Message primaryMessage = Message.raw("KILL!").color("#FF4444");
            Message secondaryMessage = Message.raw("Eliminated " + victimName).color("#AAAAAA");
            ItemWithAllMetadata icon = (ItemWithAllMetadata) new ItemStack("Weapon_Sword_Mithril", 1).toPacket();

            NotificationUtil.sendNotification(
                    packetHandler,
                    primaryMessage,
                    secondaryMessage,
                    icon);
        } catch (Exception e) {
            LOGGER.warning("[DeathmatchNotifications] Failed to send kill notification: " + e.getMessage());
        }
    }

    /**
     * Sends a death notification to a player.
     */
    public static void sendDeathNotification(Player player, String killerName) {
        if (player == null)
            return;

        try {
            PlayerRef playerRef = Universe.get().getPlayer(player.getUuid());
            if (playerRef == null)
                return;

            PacketHandler packetHandler = playerRef.getPacketHandler();

            Message primaryMessage = Message.raw("YOU DIED").color("#FF0000");
            Message secondaryMessage = Message.raw("Killed by " + killerName).color("#AAAAAA");
            ItemWithAllMetadata icon = (ItemWithAllMetadata) new ItemStack("Item_Skull", 1).toPacket();

            NotificationUtil.sendNotification(
                    packetHandler,
                    primaryMessage,
                    secondaryMessage,
                    icon);
        } catch (Exception e) {
            LOGGER.warning("[DeathmatchNotifications] Failed to send death notification: " + e.getMessage());
        }
    }

    /**
     * Sends a match start notification.
     */
    public static void sendMatchStartNotification(Player player, String opponentName) {
        if (player == null)
            return;

        try {
            PlayerRef playerRef = Universe.get().getPlayer(player.getUuid());
            if (playerRef == null)
                return;

            PacketHandler packetHandler = playerRef.getPacketHandler();

            Message primaryMessage = Message.raw("FIGHT!").color("#FFD700");
            Message secondaryMessage = Message.raw("vs " + opponentName).color("#FFFFFF");
            ItemWithAllMetadata icon = (ItemWithAllMetadata) new ItemStack("Trophy_Gold", 1).toPacket();

            NotificationUtil.sendNotification(
                    packetHandler,
                    primaryMessage,
                    secondaryMessage,
                    icon);
        } catch (Exception e) {
            LOGGER.warning("[DeathmatchNotifications] Failed to send match start notification: " + e.getMessage());
        }
    }

    /**
     * Sends a victory notification.
     */
    public static void sendVictoryNotification(Player player, int finalScore, int opponentScore) {
        if (player == null)
            return;

        try {
            PlayerRef playerRef = Universe.get().getPlayer(player.getUuid());
            if (playerRef == null)
                return;

            PacketHandler packetHandler = playerRef.getPacketHandler();

            Message primaryMessage = Message.raw("VICTORY!").color("#00FF00");
            Message secondaryMessage = Message.raw("Final: " + finalScore + " - " + opponentScore).color("#FFFFFF");
            ItemWithAllMetadata icon = (ItemWithAllMetadata) new ItemStack("Trophy_Gold", 1).toPacket();

            NotificationUtil.sendNotification(
                    packetHandler,
                    primaryMessage,
                    secondaryMessage,
                    icon);
        } catch (Exception e) {
            LOGGER.warning("[DeathmatchNotifications] Failed to send victory notification: " + e.getMessage());
        }
    }

    /**
     * Sends a defeat notification.
     */
    public static void sendDefeatNotification(Player player, int finalScore, int opponentScore) {
        if (player == null)
            return;

        try {
            PlayerRef playerRef = Universe.get().getPlayer(player.getUuid());
            if (playerRef == null)
                return;

            PacketHandler packetHandler = playerRef.getPacketHandler();

            Message primaryMessage = Message.raw("DEFEAT").color("#FF4444");
            Message secondaryMessage = Message.raw("Final: " + finalScore + " - " + opponentScore).color("#AAAAAA");
            ItemWithAllMetadata icon = (ItemWithAllMetadata) new ItemStack("Trophy_Silver", 1).toPacket();

            NotificationUtil.sendNotification(
                    packetHandler,
                    primaryMessage,
                    secondaryMessage,
                    icon);
        } catch (Exception e) {
            LOGGER.warning("[DeathmatchNotifications] Failed to send defeat notification: " + e.getMessage());
        }
    }

    /**
     * Sends a score update notification.
     */
    public static void sendScoreNotification(Player player, String scorerName, int newScore) {
        if (player == null)
            return;

        try {
            PlayerRef playerRef = Universe.get().getPlayer(player.getUuid());
            if (playerRef == null)
                return;

            PacketHandler packetHandler = playerRef.getPacketHandler();

            Message primaryMessage = Message.raw("+1 " + scorerName).color("#44FF44");
            Message secondaryMessage = Message.raw("Score: " + newScore).color("#FFFFFF");
            ItemWithAllMetadata icon = (ItemWithAllMetadata) new ItemStack("Item_Coin_Gold", 1).toPacket();

            NotificationUtil.sendNotification(
                    packetHandler,
                    primaryMessage,
                    secondaryMessage,
                    icon);
        } catch (Exception e) {
            LOGGER.warning("[DeathmatchNotifications] Failed to send score notification: " + e.getMessage());
        }
    }
}
