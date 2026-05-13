package org.coreplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class JoinListener implements Listener {

    private final Coreplugin plugin;

    public JoinListener(Coreplugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // CraftBukkit 1.8 calls getHighestBlockYAt() for first-time players, landing them
        // on top of the dome shell. Force-teleport to the exact stored spawn instead.
        if (!player.hasPlayedBefore()) {
            if (plugin.isGeneratorWorld(player.getWorld().getName())) {
                player.teleport(player.getWorld().getSpawnLocation());
            }
            ItemStack book = WelcomeItems.loadBook(plugin, player);
            if (book != null) {
                player.getInventory().addItem(book);
            }

            ItemStack bed = new ItemStack(Material.BED);
            player.getInventory().addItem(bed);

            ItemStack food = WelcomeItems.loadFood(plugin, player);
            player.getInventory().addItem(food);
        }

        satSend("Detected Exile Biosignature!", plugin);
        satSend("Nickname: " + ChatColor.RED + player.getName() + ChatColor.RESET, plugin);
        satSend("Crime: " + ChatColor.RED + getCrime(plugin, player), plugin);
    }

    public static void satSend(String text, Plugin plugin) {
        String newMessage = "<" +ChatColor.GOLD + "I.a.R. Satellite" + ChatColor.RESET + "> " + text;
        plugin.getServer().broadcastMessage(newMessage);
    }

    public static String getCrime(Plugin plugin, Player p) {
        File f = new File(plugin.getDataFolder(), "crimes.txt");
        List<String> crimes;
        try {
            crimes = Files.readAllLines(f.toPath());
            crimes.removeIf(s -> s.trim().isEmpty());
        } catch (IOException e) {
            plugin.getLogger().warning("Could not read crimes.txt: " + e.getMessage());
            crimes = Collections.singletonList("Treason against the Emperor");
        }
        if (crimes.isEmpty()) return "Treason against the Emperor";
        long seed = p.getUniqueId().getMostSignificantBits() ^ p.getUniqueId().getLeastSignificantBits();
//        return crimes.get(crimes.size() - 1);
        return crimes.get(new Random(seed).nextInt(crimes.size()));
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (!plugin.isGeneratorWorld(event.getWorld().getName())) return;
        plugin.applyOasisSpawn(event.getWorld());
    }
}