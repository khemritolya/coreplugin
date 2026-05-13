package org.coreplugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class JoinListener implements Listener {

    private final Coreplugin plugin;

    private static final String[] CRIMES = {
            "Disliking the Emperor", "Insulting the Emperor", "Mocking the Emperor", "Annoying the Emperor",
            "Indifference towards the Emperor", "Laughing at the Emperor", "Ruining the Emperor's Day",
            "Sneezing close to the Emperor", "Treason against the Emperor", "Betraying the Emperor",
            "Fooling the Emperor", "Tricking the Emperor"
    };

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
            ItemStack book = WelcomeBook.load(plugin, player);
            if (book != null) {
                player.getInventory().addItem(book);
            }
        }

        String joinMessage = "<" +
                ChatColor.GOLD + "I.a.R. Satellite" +
                ChatColor.RESET + "> Detected Exile!Nickname: ``" +
                ChatColor.BOLD + ChatColor.DARK_RED + player.getName() +
                ChatColor.RESET + "'' Crime: " +
                ChatColor.RED + getCrime(player);
        player.sendMessage(joinMessage);
    }

    public static String getCrime(Player p) {
        long seed = p.getUniqueId().getMostSignificantBits() ^ p.getUniqueId().getLeastSignificantBits();
        Random rng = new Random(seed);
        return CRIMES[rng.nextInt(CRIMES.length)];
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (!plugin.isGeneratorWorld(event.getWorld().getName())) return;
        plugin.applyOasisSpawn(event.getWorld());
    }
}