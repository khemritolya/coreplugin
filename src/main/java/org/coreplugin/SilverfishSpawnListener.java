package org.coreplugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Silverfish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class SilverfishSpawnListener implements Listener {

    private static final String TAG = "dangerousCritter";
    private static final String spiceName = ChatColor.RESET + "Blue Dust";

    private final Coreplugin plugin;
    private final double spawnChance;
    private final double dropChance;
    private final int duration;
    private final double lambda;
    private final Random rng = new Random();
    private final Set<String> recentBreaks = new HashSet<>();

    public SilverfishSpawnListener(Coreplugin plugin) {
        this.plugin = plugin;
        spawnChance = plugin.getConfig().getDouble("silverfish.spawn-chance", 0.5);
        dropChance  = plugin.getConfig().getDouble("silverfish.potion.drop-chance", 0.5);
        duration   = plugin.getConfig().getInt("silverfish.potion.duration", 200);
        lambda     = plugin.getConfig().getDouble("silverfish.potion.level-lambda", 1.0);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SAND || block.getData() != 1) return;

        // CraftBukkit 1.8 can fire multiple dig packets for one block; deduplicate by location.
        String key = block.getWorld().getName() + block.getX() + "," + block.getY() + "," + block.getZ();
        if (!recentBreaks.add(key)) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> recentBreaks.remove(key), 1L);

        if (rng.nextDouble() >= spawnChance) return;

        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        Silverfish fish = (Silverfish) block.getWorld().spawnEntity(loc, EntityType.SILVERFISH);
        fish.setMetadata(TAG, new FixedMetadataValue(plugin, true));
        fish.addPotionEffect(
            new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 1, false, false), true);
        fish.addPotionEffect(
            new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 3, false, false), true);
        fish.addPotionEffect(
                new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, false, false), true);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Silverfish)) return;
        if (!event.getEntity().hasMetadata(TAG)) return;
        if (rng.nextDouble() >= dropChance) return;

        Location loc = event.getEntity().getLocation();
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        int mark = poissonSample(lambda);
        meta.setDisplayName(spiceName);

        meta.addCustomEffect(
                new PotionEffect(PotionEffectType.SATURATION, duration, mark, false, false), true);
        meta.addCustomEffect(
                new PotionEffect(PotionEffectType.REGENERATION, duration, mark, false, false), true);
        meta.addCustomEffect(
                new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, mark/2, false, false), true);
        meta.addCustomEffect(
                new PotionEffect(PotionEffectType.NIGHT_VISION, duration, 0, false, false), true);

        meta.setLore(Collections.singletonList(ChatColor.RESET + "" + ChatColor.GRAY + "Grade: " + ChatColor.GOLD + toRoman(mark + 1)));
        potion.setItemMeta(meta);
        loc.getWorld().dropItemNaturally(loc, potion);
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() != Material.POTION) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getDisplayName().equals(spiceName)) return;

        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ItemStack hand = player.getItemInHand();
            if (hand == null || hand.getType() != Material.GLASS_BOTTLE) return;
            int amt = hand.getAmount() - 1;
            player.setItemInHand(amt <= 0 ? null : new ItemStack(Material.GLASS_BOTTLE, amt));
        }, 1L);
    }

    private static String toRoman(int n) {
        String[] M  = {"", "M", "MM", "MMM"};
        String[] C  = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] X  = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] I  = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        return M[n / 1000] + C[(n % 1000) / 100] + X[(n % 100) / 10] + I[n % 10];
    }

    private int poissonSample(double lam) {
        return RngUtils.poissonSample(rng, lam);
    }
}