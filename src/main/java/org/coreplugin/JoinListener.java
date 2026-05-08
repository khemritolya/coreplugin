package org.coreplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final Coreplugin plugin;

    public JoinListener(Coreplugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String message = plugin.getConfig().getString("welcome-message", "Welcome!")
                .replace("{player}", event.getPlayer().getName());
        event.getPlayer().sendMessage(message);
    }
}