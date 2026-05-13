package org.coreplugin;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class SandstormManager extends BukkitRunnable {

    private final Coreplugin plugin;
    private final int durationMin;
    private final int durationMax;
    private final int checkInterval;
    private final double damage;
    private final int damageInterval;
    private final int skyLightThreshold;
    private final int meanArrivalInterval;
    private final int warningTicks;
    private final Random rng = new Random();

    private boolean active = false;
    private long remainingTicks = 0;
    private long pendingTicks = 0;
    private long damageAccumulator = 0;

    private SandstormManager(Coreplugin plugin) {
        this.plugin = plugin;
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("sandstorm");
        durationMin    = cfg.getInt("duration-min", 2400);
        durationMax    = cfg.getInt("duration-max", 6000);
        checkInterval  = cfg.getInt("check-interval", 20);
        damage            = cfg.getDouble("damage", 1.0);
        damageInterval    = cfg.getInt("damage-interval", 60);
        skyLightThreshold    = cfg.getInt("sky-light-threshold", 12);
        meanArrivalInterval  = cfg.getInt("mean-arrival-interval", 24000);
        warningTicks         = cfg.getInt("warning-ticks", 1200);
    }

    public static SandstormManager register(Coreplugin plugin) {
        SandstormManager manager = new SandstormManager(plugin);
        manager.runTaskTimer(plugin, 0L, manager.checkInterval);
        return manager;
    }

    @Override
    public void run() {
        if (!active) {
            if (pendingTicks > 0) {
                pendingTicks -= checkInterval;
                if (pendingTicks <= 0) startStorm();
            } else if (rng.nextDouble() < (double) checkInterval / meanArrivalInterval) {
                pendingTicks = warningTicks;
                JoinListener.satSend("Warning! Firestorm approaching!", plugin);
            }
            return;
        }

        remainingTicks -= checkInterval;
        if (remainingTicks <= 0) {
            stopStorm();
            return;
        }

        damageAccumulator += checkInterval;
        boolean doDamage = damageAccumulator >= damageInterval;
        if (doDamage) damageAccumulator = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.isGeneratorWorld(player.getWorld().getName())) continue;
            GameMode gm = player.getGameMode();
            if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) continue;

            Location loc = player.getLocation();
            int skyLight = loc.getWorld().getBlockAt(loc).getLightFromSky();
            boolean exposed = skyLight >= skyLightThreshold;

            if (exposed) {
                player.addPotionEffect(
                        new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false), true);
                player.addPotionEffect(
                        new PotionEffect(PotionEffectType.SLOW, 40, 1, false, false), true);
                spawnSandParticles(player);
                if (doDamage) player.damage(damage);
            }
        }
    }

    private void spawnSandParticles(Player player) {
        Location base = player.getLocation().add(0, 1, 0);
        for (int i = 0; i < 12; i++) {
            Location loc = base.clone().add(
                    (rng.nextDouble() - 0.5) * 5,
                    rng.nextDouble() * 2.5,
                    (rng.nextDouble() - 0.5) * 5);
            player.getWorld().playEffect(loc, Effect.MOBSPAWNER_FLAMES, 0);
        }
    }

    public void startStorm() {
        int range = durationMax - durationMin;
        startStorm(durationMin + (range > 0 ? rng.nextInt(range) : 0));
    }

    public void startStorm(int durationTicks) {
        active = true;
        remainingTicks = durationTicks;
        damageAccumulator = 0;
        JoinListener.satSend("Severe Danger! Firestorm Imminent!", plugin);
    }

    public void stopStorm() {
        active = false;
        pendingTicks = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.resetPlayerWeather();
        }
        JoinListener.satSend("All clear! Firestorm is dissipated!", plugin);
    }

    public boolean isActive() {
        return active;
    }

    public long getRemainingTicks() {
        return remainingTicks;
    }
}