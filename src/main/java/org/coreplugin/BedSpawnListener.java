package org.coreplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class BedSpawnListener implements Listener {

    @EventHandler
    public void onBedClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.BED_BLOCK) return;

        Player player = event.getPlayer();
        event.setCancelled(true);
        player.setBedSpawnLocation(event.getClickedBlock().getLocation());
        player.sendMessage("Spawn point set.");
    }
}