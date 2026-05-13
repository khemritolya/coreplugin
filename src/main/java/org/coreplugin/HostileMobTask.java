package org.coreplugin;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.coreplugin.worldgen.DesertWorldGenerator;

import java.util.List;
import java.util.Random;

public class HostileMobTask extends BukkitRunnable {

    private static final String TAG = "dangerousCritter";

    private final Coreplugin plugin;
    private final SandstormManager sandstorm;
    private final int spawnRadius;
    private final double rockExclusionFactor;
    private final double ghastChance;
    private final int ghastMaxPerPlayer;
    private final double silverfishChance;
    private final int silverfishMaxPerPlayer;
    private final Random rng = new Random();

    private HostileMobTask(Coreplugin plugin, SandstormManager sandstorm) {
        this.plugin = plugin;
        this.sandstorm = sandstorm;
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("night-mobs");
        spawnRadius         = cfg.getInt("spawn-radius", 48);
        rockExclusionFactor = cfg.getDouble("rock-exclusion-factor", 2.0);
        ConfigurationSection ghastCfg  = cfg.getConfigurationSection("ghast");
        ghastChance         = ghastCfg.getDouble("chance", 0.3);
        ghastMaxPerPlayer   = ghastCfg.getInt("max-per-player", 2);
        ConfigurationSection silverfishCfg = cfg.getConfigurationSection("silverfish");
        silverfishChance       = silverfishCfg.getDouble("chance", 0.5);
        silverfishMaxPerPlayer = silverfishCfg.getInt("max-per-player", 3);
    }

    @Override
    public void run() {
        DesertWorldGenerator generator = plugin.getGenerator();

        for (World world : plugin.getServer().getWorlds()) {
            if (!plugin.isGeneratorWorld(world.getName())) continue;

            if (sandstorm.isActive()) {
                for (LivingEntity e : world.getLivingEntities()) {
                    if (e.hasMetadata(TAG)) e.remove();
                }
                continue;
            }

            List<Player> players = world.getPlayers();
            double cullRangeSq = (spawnRadius * 3.0) * (spawnRadius * 3.0);
            for (LivingEntity e : world.getLivingEntities()) {
                if (!e.hasMetadata(TAG)) continue;
                boolean inRange = false;
                for (Player p : players) {
                    if (e.getLocation().distanceSquared(p.getLocation()) <= cullRangeSq) {
                        inRange = true;
                        break;
                    }
                }
                if (!inRange) e.remove();
            }

            int cap = players.size();
            for (Player player : players) {
                if (countTagged(Ghast.class, world) < ghastMaxPerPlayer * cap
                        && rng.nextDouble() < ghastChance) {
                    trySpawn(EntityType.GHAST, player, world, generator, true);
                }
                if (countTagged(Silverfish.class, world) < silverfishMaxPerPlayer * cap
                        && rng.nextDouble() < silverfishChance) {
                    trySpawn(EntityType.SILVERFISH, player, world, generator, false);
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

            LivingEntity mob = (LivingEntity) world.spawnEntity(
                    new Location(world, wx + 0.5, spawnY, wz + 0.5), type);

            if (mob instanceof Silverfish) {
                mob.addPotionEffect(
                        new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 1, false, false), true);
                mob.addPotionEffect(
                        new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 3, false, false), true);
                mob.addPotionEffect(
                        new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, false, false), true);
            }

            mob.setMetadata(TAG, new FixedMetadataValue(plugin, true));
            return;
        }
    }

    private long countTagged(Class<? extends Entity> type, World world) {
        long count = 0;
        for (LivingEntity e : world.getLivingEntities()) {
            if (type.isInstance(e) && e.hasMetadata(TAG)) count++;
        }
        return count;
    }

    public static void register(Coreplugin plugin, SandstormManager sandstorm) {
        long interval = plugin.getConfig().getLong("night-mobs.check-interval", 40L);
        new HostileMobTask(plugin, sandstorm).runTaskTimer(plugin, 0L, interval);
    }
}