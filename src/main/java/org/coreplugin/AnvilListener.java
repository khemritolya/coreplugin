package org.coreplugin;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

public class AnvilListener implements Listener {

    private static final Set<String> UNREPAIRABLE = new HashSet<>(Arrays.asList(
            CustomItems.MONOMOLECULAR_BLADE_NAME,
            CustomItems.PROSPECTOR_NAME,
            CustomItems.HARD_HAT_NAME,
            CustomItems.SPEED_BOOTS_NAME,
            CustomItems.PLASMA_CHARGE_NAME,
            CustomItems.IMPERIAL_TACHI_NAME,
            CustomItems.PHASE_DEVICE_NAME,
            CustomItems.NANO_CRYSTAL_NAME,
            CustomItems.UPLINK_CARD_NAME,
            CustomItems.DIAMONDOID_CHESTPLATE_NAME,
            CustomItems.DIAMONDOID_LEGGINGS_NAME,
            CustomItems.DIAMONDOID_BOOTS_NAME,
            CustomItems.DIAMONDOID_SWORD_NAME,
            CustomItems.UPLINK_BEACON_DROP_NAME,
            CustomItems.UPLINK_GUARDIAN_HEAD_NAME,
            CustomItems.SPICE_NAME
    ));

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient == null) continue;
            ItemMeta meta = ingredient.getItemMeta();
            if (meta != null && meta.hasDisplayName() && UNREPAIRABLE.contains(meta.getDisplayName())) {
                event.getInventory().setResult(new ItemStack(Material.AIR));
                return;
            }
        }
    }

    @EventHandler
    public void onAnvilClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.ANVIL) return;
        if (event.getRawSlot() != 2) return;
        ItemStack input = event.getInventory().getItem(0);
        if (input == null) return;
        ItemMeta meta = input.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        if (UNREPAIRABLE.contains(meta.getDisplayName())) {
            event.setCancelled(true);
        }
    }
}