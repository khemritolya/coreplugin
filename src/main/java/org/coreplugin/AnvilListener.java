package org.coreplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

public class AnvilListener implements Listener {

    private static final Set<String> UNREPAIRABLE = new HashSet<>(Arrays.asList(
            CustomItems.MONOMOLECULAR_BLADE_NAME,
            CustomItems.PROSPECTOR_NAME,
            CustomItems.HARD_HAT_NAME
    ));

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