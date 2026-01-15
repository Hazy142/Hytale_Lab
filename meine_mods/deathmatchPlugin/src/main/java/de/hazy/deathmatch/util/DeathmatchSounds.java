package de.hazy.deathmatch.util;

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
 * Utility class for playing sounds to players in the deathmatch game.
 */
public class DeathmatchSounds {

    private static final Logger LOGGER = Logger.getLogger("DeathmatchSounds");

    // Sound names (from Hytale asset map)
    public static final String SOUND_MATCH_START = "SFX_UI_Confirm";
    public static final String SOUND_MATCH_END = "SFX_UI_Victory";
    public static final String SOUND_COUNTDOWN = "SFX_UI_Tick";
    public static final String SOUND_KILL = "SFX_Hit_Critical";
    public static final String SOUND_DEATH = "SFX_Death";

    /**
     * Plays a sound to a specific player.
     */
    public static void playSound(Player player, String soundName) {
        if (player == null || soundName == null)
            return;

        World world = player.getWorld();
        if (world == null)
            return;

        Ref<EntityStore> playerRef = player.getReference();
        if (playerRef == null)
            return;

        try {
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
                                SoundCategory.UI,
                                transform.getPosition(),
                                store);
                    }
                } catch (Exception e) {
                    LOGGER.warning("[DeathmatchSounds] Failed to play sound: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.warning("[DeathmatchSounds] Sound not found: " + soundName);
        }
    }

    /**
     * Plays the match start sound.
     */
    public static void playMatchStart(Player player) {
        playSound(player, SOUND_MATCH_START);
    }

    /**
     * Plays the match end/victory sound.
     */
    public static void playMatchEnd(Player player) {
        playSound(player, SOUND_MATCH_END);
    }

    /**
     * Plays a countdown tick sound.
     */
    public static void playCountdown(Player player) {
        playSound(player, SOUND_COUNTDOWN);
    }

    /**
     * Plays the kill confirmation sound.
     */
    public static void playKill(Player player) {
        playSound(player, SOUND_KILL);
    }
}
