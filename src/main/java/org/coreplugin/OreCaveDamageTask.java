package org.coreplugin;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.coreplugin.worldgen.DesertWorldGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OreCaveDamageTask extends BukkitRunnable {

    private final CorePlugin plugin;
    private final double damagePerInterval;
    private final long warningCooldownMs;
    private final Map<UUID, Long> lastWarnedMs = new HashMap<>();

    private OreCaveDamageTask(CorePlugin plugin, double damagePerSecond, long intervalTicks, long warningCooldownTicks) {
        this.plugin = plugin;
        this.damagePerInterval  = damagePerSecond * intervalTicks / 20.0;
        this.warningCooldownMs  = warningCooldownTicks * 50L;
    }

    @Override
    public void run() {
        DesertWorldGenerator generator = plugin.getGenerator();
        for (World world : plugin.getServer().getWorlds()) {
            if (!plugin.isGeneratorWorld(world.getName())) continue;
            for (LivingEntity entity : world.getLivingEntities()) {
                Location loc = entity.getLocation();
                if (generator == null || !generator.isInOreCave(
                        world.getSeed(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) continue;

                if (entity instanceof Player) {
                    Player player = (Player) entity;
                    long now = System.currentTimeMillis();
                    Long last = lastWarnedMs.get(player.getUniqueId());
                    if (last == null || now - last >= warningCooldownMs) {
                        player.sendMessage("Danger! The rock is emitting radiation above safety threshold!");
                        lastWarnedMs.put(player.getUniqueId(), now);
                    }
                }
                entity.damage(damagePerInterval);
            }
        }
    }

    public static void register(CorePlugin plugin) {
        ConfigurationSection cfg = plugin.getConfig()
                .getConfigurationSection("world-gen.rocks.ore-cave.radiation");
        double dps     = cfg != null ? cfg.getDouble("damage-per-second",  1.0)   : 1.0;
        long interval  = cfg != null ? cfg.getLong("check-interval",       20L)   : 20L;
        long cooldown  = cfg != null ? cfg.getLong("warning-cooldown",     1200L) : 1200L;
        new OreCaveDamageTask(plugin, dps, interval, cooldown).runTaskTimer(plugin, 0L, interval);
    }
}