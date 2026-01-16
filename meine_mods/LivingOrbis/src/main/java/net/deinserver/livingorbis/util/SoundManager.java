package net.deinserver.livingorbis.util;

import com.hypixel.hytale.server.core.ecs.Store;
import com.hypixel.hytale.server.core.ecs.Ref;
import com.hypixel.hytale.server.core.ecs.EntityStore;
import com.hypixel.hytale.server.core.ecs.component.TransformComponent;
import com.hypixel.hytale.server.core.entity.Player;
import com.hypixel.hytale.server.core.entity.EntityModule;
import com.hypixel.hytale.server.core.world.World;
import com.hypixel.hytale.server.core.audio.SoundEvent;
import com.hypixel.hytale.server.core.audio.SoundCategory;
import com.hypixel.hytale.server.core.audio.SoundUtil;
import java.util.logging.Logger;

/**
 * Generic Utility class for playing sounds to players.
 * Extracted from DeathmatchSounds.java.
 */
public class SoundManager {

    private static final Logger LOGGER = Logger.getLogger("SoundManager");

    /**
     * Plays a sound to a specific player at their current location.
     *
     * @param player The player to play the sound for.
     * @param soundName The name of the sound asset (e.g., "SFX_UI_Confirm").
     * @param category The sound category (e.g., UI, MASTER).
     */
    public static void playSound(Player player, String soundName, SoundCategory category) {
        if (player == null || soundName == null)
            return;

        World world = player.getWorld();
        if (world == null)
            return;

        Ref<EntityStore> playerRef = player.getReference();
        if (playerRef == null)
            return;

        try {
            // Note: In a real implementation, we might cache this index lookup
            int soundIndex = SoundEvent.getAssetMap().getIndex(soundName);
            EntityStore entityStore = world.getEntityStore();

            world.execute(() -> {
                try {
                    Store<EntityStore> store = entityStore.getStore();
                    TransformComponent transform = store.getComponent(
                            playerRef,
                            EntityModule.get().getTransformComponentType());

                    if (transform != null) {
                        SoundUtil.playSoundEvent3dToPlayer(
                                playerRef,
                                soundIndex,
                                category,
                                transform.getPosition(),
                                store);
                    }
                } catch (Exception e) {
                    LOGGER.warning("[SoundManager] Failed to play sound: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.warning("[SoundManager] Sound not found: " + soundName);
        }
    }

    /**
     * Overload for default category (UI).
     */
    public static void playSound(Player player, String soundName) {
        playSound(player, soundName, SoundCategory.UI);
    }
}
