package org.coreplugin;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.coreplugin.worldgen.DesertWorldGenerator;

import java.util.Random;

public class NightMobTask extends BukkitRunnable {

    private static final String TAG = "nightmob";

    private final Coreplugin plugin;
    private final int lightThreshold;
    private final int spawnRadius;
    private final double rockExclusionFactor;
    private final double ghastChance;
    private final int ghastMaxPerPlayer;
    private final double spiderChance;
    private final int spiderMaxPerPlayer;
    private final Random rng = new Random();

    private NightMobTask(Coreplugin plugin) {
        this.plugin = plugin;
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("night-mobs");
        lightThreshold      = cfg.getInt("light-threshold", 7);
        spawnRadius         = cfg.getInt("spawn-radius", 48);
        rockExclusionFactor = cfg.getDouble("rock-exclusion-factor", 2.0);
        ConfigurationSection ghastCfg  = cfg.getConfigurationSection("ghast");
        ghastChance         = ghastCfg.getDouble("chance", 0.3);
        ghastMaxPerPlayer   = ghastCfg.getInt("max-per-player", 2);
        ConfigurationSection spiderCfg = cfg.getConfigurationSection("cave-spider");
        spiderChance        = spiderCfg.getDouble("chance", 0.5);
        spiderMaxPerPlayer  = spiderCfg.getInt("max-per-player", 3);
    }

    @Override
    public void run() {
        DesertWorldGenerator generator = plugin.getGenerator();

        for (World world : plugin.getServer().getWorlds()) {
            if (!plugin.isGeneratorWorld(world.getName())) continue;

            if (world.getTime() < 13000) {
                for (LivingEntity e : world.getLivingEntities()) {
                    if (e.hasMetadata(TAG)) e.remove();
                }
                continue;
            }

            for (Player player : world.getPlayers()) {
                if (countNearby(Ghast.class, player) < ghastMaxPerPlayer
                        && rng.nextDouble() < ghastChance) {
                    trySpawn(EntityType.GHAST, player, world, generator, true);
                }
                if (countNearby(CaveSpider.class, player) < spiderMaxPerPlayer
                        && rng.nextDouble() < spiderChance) {
                    trySpawn(EntityType.CAVE_SPIDER, player, world, generator, false);
                }
            }
        }
    }

    private void trySpawn(EntityType type, Player player, World world,
                          DesertWorldGenerator generator, boolean aerial) {
        Location origin = player.getLocation();
        for (int attempt = 0; attempt < 5; attempt++) {
            double angle = rng.nextDouble() * 2 * Math.PI;
            int dist = spawnRadius / 2 + rng.nextInt(Math.max(1, spawnRadius / 2));
            int wx = origin.getBlockX() + (int) (Math.cos(angle) * dist);
            int wz = origin.getBlockZ() + (int) (Math.sin(angle) * dist);

            if (!world.isChunkLoaded(wx >> 4, wz >> 4)) continue;
            if (generator != null && generator.isNearAnyRock(world.getSeed(), wx, wz, rockExclusionFactor)) continue;

            int surfaceY = world.getHighestBlockYAt(wx, wz);
            int spawnY = aerial ? surfaceY + 12 + rng.nextInt(10) : surfaceY + 1;

            if (world.getBlockAt(wx, spawnY, wz).getLightLevel() > lightThreshold) continue;

            LivingEntity mob = (LivingEntity) world.spawnEntity(
                    new Location(world, wx + 0.5, spawnY, wz + 0.5), type);

            if (mob instanceof CaveSpider) {
                mob.addPotionEffect(
                        new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 1, false, false), true);
                mob.addPotionEffect(
                        new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1, false, false), true);
                mob.addPotionEffect(
                        new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false), true);
            }

            mob.setMetadata(TAG, new FixedMetadataValue(plugin, true));
            return;
        }
    }

    private long countNearby(Class<? extends Entity> type, Player player) {
        double rangeSq = (spawnRadius * 3.0) * (spawnRadius * 3.0);
        long count = 0;
        for (LivingEntity e : player.getWorld().getLivingEntities()) {
            if (type.isInstance(e) && e.hasMetadata(TAG)
                    && e.getLocation().distanceSquared(player.getLocation()) < rangeSq) {
                count++;
            }
        }
        return count;
    }

    public static void register(Coreplugin plugin) {
        long interval = plugin.getConfig().getLong("night-mobs.check-interval", 40L);
        new NightMobTask(plugin).runTaskTimer(plugin, 0L, interval);
    }
}