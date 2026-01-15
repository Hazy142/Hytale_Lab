package de.hazy.deathmatch.listeners;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ecs.Store;
import com.hypixel.hytale.server.core.ecs.Ref;
import com.hypixel.hytale.server.core.ecs.EntityStore;
import com.hypixel.hytale.server.core.ecs.system.EntityEventSystem;
import com.hypixel.hytale.server.core.ecs.Archetype;
import com.hypixel.hytale.server.core.ecs.ArchetypeChunk;
import com.hypixel.hytale.server.core.ecs.CommandBuffer;
import com.hypixel.hytale.server.core.ecs.Query;
import com.hypixel.hytale.server.core.ecs.component.UUIDComponent;
import com.hypixel.hytale.server.core.entity.Player;
import com.hypixel.hytale.server.core.event.ecs.Damage;
import de.hazy.deathmatch.GameManager;
import de.hazy.deathmatch.game.GameState;
import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Handles damage events to detect kills and update scores automatically.
 * This replaces the manual /kill1 and /kill2 commands during live gameplay.
 */
public class DamageEventHandler extends EntityEventSystem<EntityStore, Damage> {

    private final GameManager gameManager;

    public DamageEventHandler(GameManager gameManager) {
        super(Damage.class);
        this.gameManager = gameManager;
    }

    @Override
    public void handle(int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage damageEvent) {

        // Only track kills during a live match
        if (gameManager.getState() != GameState.LIVE) {
            return;
        }

        // Check if the damage is lethal (entity would die)
        if (!damageEvent.isLethal()) {
            return;
        }

        // Get the victim's UUID
        Ref<EntityStore> victimRef = damageEvent.getTarget();
        if (victimRef == null)
            return;

        UUIDComponent victimUuidComponent = store.getComponent(victimRef, UUIDComponent.getComponentType());
        if (victimUuidComponent == null)
            return;

        UUID victimUuid = victimUuidComponent.getUuid();

        // Get the attacker's UUID (if exists)
        Ref<EntityStore> attackerRef = damageEvent.getSource();
        UUID attackerUuid = null;
        if (attackerRef != null) {
            UUIDComponent attackerUuidComponent = store.getComponent(attackerRef, UUIDComponent.getComponentType());
            if (attackerUuidComponent != null) {
                attackerUuid = attackerUuidComponent.getUuid();
            }
        }

        // Determine which player died and register the kill for the other
        UUID player1Uuid = gameManager.getPlayer1Uuid();
        UUID player2Uuid = gameManager.getPlayer2Uuid();

        if (victimUuid.equals(player1Uuid)) {
            // Player 1 died - Point for Player 2
            gameManager.registerKill("2");
        } else if (victimUuid.equals(player2Uuid)) {
            // Player 2 died - Point for Player 1
            gameManager.registerKill("1");
        }
        // If neither, it's not a duel participant - ignore
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
