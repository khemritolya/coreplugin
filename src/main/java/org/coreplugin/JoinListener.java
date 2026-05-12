package org.coreplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class JoinListener implements Listener {

    private final Coreplugin plugin;

    public JoinListener(Coreplugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();

        // CraftBukkit 1.8 calls getHighestBlockYAt() for first-time players, landing them
        // on top of the dome shell. Force-teleport to the exact stored spawn instead.
        if (!player.hasPlayedBefore() && plugin.isGeneratorWorld(player.getWorld().getName())) {
            player.teleport(player.getWorld().getSpawnLocation());
        }

        String message = plugin.getConfig().getString("welcome-message", "Welcome!")
                .replace("{player}", player.getName());
        player.sendMessage(message);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (!plugin.isGeneratorWorld(event.getWorld().getName())) return;
        plugin.applyOasisSpawn(event.getWorld());
    }
}