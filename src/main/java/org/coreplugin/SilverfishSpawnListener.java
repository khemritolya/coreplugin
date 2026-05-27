package org.coreplugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Silverfish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class SilverfishSpawnListener implements Listener {

    private static final String TAG       = "dangerousCritter";
    private static final String SPICE_TAG = "fieldCritter";
    
    private final CorePlugin plugin;
    private final double spawnChance;
    private final double dropChance;
    private final int    duration;
    private final double lambda;
    private final double spiceSpawnChance;
    private final double spiceDropChance;
    private final int    spiceDuration;
    private final double spiceGradeLambda;
    private final double ghastStringLambda;
    private final float  prospectorExplosionPower;
    private final float  snowballExplosionPower;
    private final double explosionRateFactor;
    private final Random rng = new Random();
    private final Set<String> recentBreaks = new HashSet<>();

    public SilverfishSpawnListener(CorePlugin plugin) {
        this.plugin = plugin;
        spawnChance              = plugin.getConfig().getDouble("silverfish.red-sand.spawn-chance",            0.5);
        dropChance               = plugin.getConfig().getDouble("silverfish.red-sand.potion.drop-chance",      0.2);
        duration                 = plugin.getConfig().getInt(   "silverfish.red-sand.potion.duration",         2000);
        lambda                   = plugin.getConfig().getDouble("silverfish.red-sand.potion.level-lambda",     1.0);
        spiceSpawnChance         = plugin.getConfig().getDouble("silverfish.spice-field.spawn-chance",         0.9);
        spiceDropChance          = plugin.getConfig().getDouble("silverfish.spice-field.potion.drop-chance",   0.5);
        spiceDuration            = plugin.getConfig().getInt(   "silverfish.spice-field.potion.duration",      2000);
        spiceGradeLambda         = plugin.getConfig().getDouble("silverfish.spice-field.potion.level-lambda",  3.0);
        ghastStringLambda        = plugin.getConfig().getDouble("night-mobs.ghast.string-drop-lambda",         1.0);
        prospectorExplosionPower = (float) plugin.getConfig().getDouble("prospector-pickaxe.explosion-power", 2.0);
        snowballExplosionPower   = (float) plugin.getConfig().getDouble("snowball.explosion-power",           2.0);
        explosionRateFactor      = plugin.getConfig().getDouble("silverfish.explosion-rate-factor",           0.2);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack held = event.getPlayer().getItemInHand();
        if (held != null && held.getType() == Material.IRON_PICKAXE) {
            ItemMeta heldMeta = held.getItemMeta();
            if (heldMeta != null && heldMeta.hasDisplayName()
                    && heldMeta.getDisplayName().equals(CustomItems.PROSPECTOR_NAME)) {
                Location loc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
                Map<Block, Byte> nearbySand = collectSandAround(loc, (int) prospectorExplosionPower + 2);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(),
                            prospectorExplosionPower, false, true);
                    spawnSilverfishFromExplosion(nearbySand);
                });
            }
        }

        Block block = event.getBlock();
        if (block.getType() != Material.SAND) return;
        byte data = block.getData();
        boolean isSpice = data == 0;  // white sand
        if (!isSpice && data != 1)    return;  // not red sand either

        // CraftBukkit 1.8 can fire multiple dig packets for one block; deduplicate by location.
        String key = block.getWorld().getName() + block.getX() + "," + block.getY() + "," + block.getZ();
        if (!recentBreaks.add(key)) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> recentBreaks.remove(key), 1L);

        double chance = isSpice ? spiceSpawnChance : spawnChance;
        if (rng.nextDouble() >= chance) return;

        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        Silverfish fish = (Silverfish) block.getWorld().spawnEntity(loc, EntityType.SILVERFISH);
        initSilverfish(fish, plugin);
        if (isSpice) fish.setMetadata(SPICE_TAG, new FixedMetadataValue(plugin, true));
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            if (block.getType() != Material.SAND) continue;
            byte data = block.getData();
            boolean isSpice = data == 0;
            if (!isSpice && data != 1) continue;

            double chance = (isSpice ? spiceSpawnChance : spawnChance) * explosionRateFactor;
            if (rng.nextDouble() >= chance) continue;

            Location loc = block.getLocation().add(0.5, 0.5, 0.5);
            Silverfish fish = (Silverfish) block.getWorld().spawnEntity(loc, EntityType.SILVERFISH);
            initSilverfish(fish, plugin);
            if (isSpice) fish.setMetadata(SPICE_TAG, new FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity().getType() != EntityType.SNOWBALL) return;
        Location loc = event.getEntity().getLocation();
        Map<Block, Byte> nearbySand = collectSandAround(loc, (int) snowballExplosionPower + 2);
        loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(),
                snowballExplosionPower, false, true);
        spawnSilverfishFromExplosion(nearbySand);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getType() == EntityType.GHAST) {
            int count = poissonSample(ghastStringLambda);
            if (count > 0) event.getDrops().add(new ItemStack(Material.STRING, count));
            return;
        }
        if (!(event.getEntity() instanceof Silverfish)) return;
        if (!event.getEntity().hasMetadata(TAG)) return;
        boolean fromField = event.getEntity().hasMetadata(SPICE_TAG);
        if (rng.nextDouble() >= (fromField ? spiceDropChance : dropChance)) return;

        Location loc = event.getEntity().getLocation();
        int mark = poissonSample(fromField ? spiceGradeLambda : lambda);
        int d = fromField ? spiceDuration : duration;
        double bonus = rng.nextDouble() + rng.nextDouble() + rng.nextDouble() + rng.nextDouble() + rng.nextDouble();
        int potionDuration = (int) (d * (0.75 + bonus * 0.1));

        loc.getWorld().dropItemNaturally(loc, CustomItems.loadSpice(mark, potionDuration));
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() != Material.POTION) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getDisplayName().equals(CustomItems.SPICE_NAME)) return;

        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ItemStack hand = player.getItemInHand();
            if (hand == null || hand.getType() != Material.GLASS_BOTTLE) return;
            int amt = hand.getAmount() - 1;
            player.setItemInHand(amt <= 0 ? null : new ItemStack(Material.GLASS_BOTTLE, amt));
        }, 1L);
    }

    private Map<Block, Byte> collectSandAround(Location center, int radius) {
        Map<Block, Byte> result = new HashMap<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block b = center.getWorld().getBlockAt(
                            center.getBlockX() + dx, center.getBlockY() + dy, center.getBlockZ() + dz);
                    if (b.getType() == Material.SAND) {
                        byte data = b.getData();
                        if (data == 0 || data == 1) result.put(b, data);
                    }
                }
            }
        }
        return result;
    }

    private void spawnSilverfishFromExplosion(Map<Block, Byte> candidates) {
        for (Map.Entry<Block, Byte> entry : candidates.entrySet()) {
            Block b = entry.getKey();
            if (b.getType() != Material.AIR) continue;
            boolean isSpice = entry.getValue() == 0;
            double chance = (isSpice ? spiceSpawnChance : spawnChance) * explosionRateFactor;
            if (rng.nextDouble() >= chance) continue;
            Location loc = b.getLocation().add(0.5, 0.5, 0.5);
            Silverfish fish = (Silverfish) loc.getWorld().spawnEntity(loc, EntityType.SILVERFISH);
            initSilverfish(fish, plugin);
            if (isSpice) fish.setMetadata(SPICE_TAG, new FixedMetadataValue(plugin, true));
        }
    }

    static void initSilverfish(Silverfish fish, CorePlugin plugin) {
        fish.setRemoveWhenFarAway(false);
        fish.setMetadata(TAG, new FixedMetadataValue(plugin, true));
        fish.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 1, false, false), true);
        fish.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 3, false, false), true);
        fish.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 3, false, false), true);
    }

    private int poissonSample(double lam) {
        return RngUtils.poissonSample(rng, lam);
    }
}