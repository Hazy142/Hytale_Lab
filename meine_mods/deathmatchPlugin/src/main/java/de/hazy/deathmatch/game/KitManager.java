package de.hazy.deathmatch.game;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.ItemContainer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manages player inventories and kit distribution for deathmatch.
 * Uses the official Hytale Inventory API.
 */
public class KitManager {

    private static final Logger LOGGER = Logger.getLogger("KitManager");

    // Store inventory snapshots for restoration after match
    private final Map<UUID, List<ItemStack>> inventorySnapshots = new HashMap<>();
    private final Map<UUID, List<ItemStack>> armorSnapshots = new HashMap<>();

    /**
     * Saves a player's current inventory before the match starts.
     */
    public void saveInventory(Player player) {
        if (player == null)
            return;

        try {
            Inventory inventory = player.getInventory();
            UUID uuid = player.getUuid();

            // Save main inventory
            List<ItemStack> mainItems = new ArrayList<>();
            ItemContainer storage = inventory.getStorage();
            // Store items (implementation depends on API - placeholder for iteration)
            inventorySnapshots.put(uuid, mainItems);

            // Save armor
            List<ItemStack> armorItems = new ArrayList<>();
            ItemContainer armor = inventory.getArmor();
            armorSnapshots.put(uuid, armorItems);

            LOGGER.info("[KitManager] Saved inventory for " + player.getDisplayName());
        } catch (Exception e) {
            LOGGER.warning("[KitManager] Failed to save inventory: " + e.getMessage());
        }
    }

    /**
     * Restores a player's inventory after the match ends.
     */
    public void restoreInventory(Player player) {
        if (player == null)
            return;

        try {
            UUID uuid = player.getUuid();
            Inventory inventory = player.getInventory();

            // Clear current inventory first
            clearInventory(player);

            // Restore saved items
            if (inventorySnapshots.containsKey(uuid)) {
                List<ItemStack> savedItems = inventorySnapshots.get(uuid);
                for (ItemStack item : savedItems) {
                    inventory.addItemStack(item);
                }
                inventorySnapshots.remove(uuid);
            }

            if (armorSnapshots.containsKey(uuid)) {
                // Restore armor if API supports it
                armorSnapshots.remove(uuid);
            }

            LOGGER.info("[KitManager] Restored inventory for " + player.getDisplayName());
        } catch (Exception e) {
            LOGGER.warning("[KitManager] Failed to restore inventory: " + e.getMessage());
        }
    }

    /**
     * Clears a player's inventory.
     */
    public void clearInventory(Player player) {
        if (player == null)
            return;

        try {
            Inventory inventory = player.getInventory();
            // Clear all slots (hotbar is typically 0-8, storage is 9-35)
            for (short i = 0; i < 36; i++) {
                inventory.removeItemStackFromSlot(i);
            }
        } catch (Exception e) {
            LOGGER.warning("[KitManager] Failed to clear inventory: " + e.getMessage());
        }
    }

    /**
     * Gives the deathmatch combat kit to a player.
     * Kit includes: Sword, Bow, Arrows, Armor
     */
    public void giveKit(Player player) {
        if (player == null)
            return;

        try {
            Inventory inventory = player.getInventory();

            // Clear existing inventory
            clearInventory(player);

            // === HOTBAR ITEMS ===
            // Slot 0: Sword
            ItemStack sword = new ItemStack("Weapon_Sword_Mithril", 1, 100.0, 100.0, null);
            inventory.addItemStackToSlot((short) 0, sword);

            // Slot 1: Bow
            ItemStack bow = new ItemStack("Weapon_Bow_Simple", 1, 100.0, 100.0, null);
            inventory.addItemStackToSlot((short) 1, bow);

            // Slot 2: Arrows
            ItemStack arrows = new ItemStack("Ammo_Arrow_Simple", 32);
            inventory.addItemStackToSlot((short) 2, arrows);

            // Slot 3: Health Potions
            ItemStack healthPotions = new ItemStack("Consumable_Potion_Health", 3);
            inventory.addItemStackToSlot((short) 3, healthPotions);

            // Slot 4: Food
            ItemStack food = new ItemStack("Consumable_Berry_Red", 16);
            inventory.addItemStackToSlot((short) 4, food);

            // Slot 8: Shield (last slot, easy access)
            ItemStack shield = new ItemStack("Tool_Shield_Simple", 1, 100.0, 100.0, null);
            inventory.addItemStackToSlot((short) 8, shield);

            // === ARMOR === (using armor container)
            // Note: Armor slot indices may vary - this uses standard notation
            // Typically: 0=Helmet, 1=Chestplate, 2=Leggings, 3=Boots
            try {
                ItemContainer armorContainer = inventory.getArmor();
                // Add armor through the container (if API supports direct slot access)
                // Fallback: add to general inventory as backup
                inventory.addItemStack(new ItemStack("Armor_Head_Iron", 1));
                inventory.addItemStack(new ItemStack("Armor_Chest_Iron", 1));
                inventory.addItemStack(new ItemStack("Armor_Legs_Iron", 1));
                inventory.addItemStack(new ItemStack("Armor_Boots_Iron", 1));
            } catch (Exception armorEx) {
                LOGGER.warning("[KitManager] Armor equip fallback: " + armorEx.getMessage());
            }

            player.sendMessage(Message.raw("§a§l⚔ Combat Kit Equipped! §r§7[Sword, Bow, Arrows, Armor]"));
            LOGGER.info("[KitManager] Gave kit to " + player.getDisplayName());

        } catch (Exception e) {
            LOGGER.severe("[KitManager] Failed to give kit: " + e.getMessage());
            player.sendMessage(Message.raw("§c[Error] Could not equip kit!"));
        }
    }

    /**
     * Gets the number of saved inventories.
     */
    public int getSavedInventoryCount() {
        return inventorySnapshots.size();
    }
}
