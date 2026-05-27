package org.coreplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class JoinListener implements Listener {

    private final CorePlugin plugin;
    private static List<String> cachedCrimes = null;

    public JoinListener(CorePlugin plugin) {
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
            ItemStack book = CustomItems.loadBook(plugin, player);
            if (book != null) {
                player.getInventory().addItem(book);
            }

            ItemStack bed = CustomItems.loadBed();

            player.getInventory().addItem(bed);

            ItemStack food = CustomItems.loadCookies(5);
            player.getInventory().addItem(food);
        }

        satSend("Detected Exile Biosignature!", plugin);
        satSend("Nickname: " + player.getName(), plugin);
        satSend("Crime: " + ChatColor.RED + getCrime(plugin, player), plugin);
    }

    public static void satSend(String text, Plugin plugin) {
        String newMessage = "<satellite> " + text;
        plugin.getServer().broadcastMessage(newMessage);
    }

    public static String getCrime(Plugin plugin, Player p) {
        if (cachedCrimes == null) {
            File f = new File(plugin.getDataFolder(), "crimes.txt");
            try {
                List<String> lines = Files.readAllLines(f.toPath());
                lines.removeIf(s -> s.trim().isEmpty());
                cachedCrimes = lines;
            } catch (IOException e) {
                plugin.getLogger().warning("Could not read crimes.txt: " + e.getMessage());
                cachedCrimes = Collections.singletonList("Treason against the Emperor");
            }
        }
        if (cachedCrimes.isEmpty()) return "Treason against the Emperor";

        long seed = p.getUniqueId().getMostSignificantBits() ^ p.getUniqueId().getLeastSignificantBits() ^ p.getWorld().getSeed();
        return cachedCrimes.get(new Random(seed).nextInt(cachedCrimes.size()));
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (!plugin.isGeneratorWorld(event.getWorld().getName())) return;
        plugin.applyOasisSpawn(event.getWorld());
    }
}