package org.coreplugin;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class DarknessListener extends BukkitRunnable {

    private final int threshold;
    private final int duration;

    private DarknessListener(int threshold, int duration) {
        this.threshold = threshold;
        this.duration = duration;
    }

    @Override
    public void run() {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            org.bukkit.GameMode gm = player.getGameMode();
            if (gm == org.bukkit.GameMode.CREATIVE || gm == org.bukkit.GameMode.SPECTATOR) continue;
            Block eyeBlock = player.getEyeLocation().getBlock();
            int effectiveLight = effectiveLightAt(eyeBlock, player.getWorld());
            if (effectiveLight <= threshold) {
                player.addPotionEffect(
                    new PotionEffect(PotionEffectType.BLINDNESS, duration, 0, false, false),
                    true
                );
            }
        }
    }

    // Mirrors vanilla's time-of-day sky-light darkening so that outdoor players
    // at night are treated as being in darkness.
    private static int effectiveLightAt(Block block, org.bukkit.World world) {
        int blockLight = block.getLightFromBlocks();
        int skyLight   = block.getLightFromSky();
        int reduction  = skyLightReduction(world.getTime());
        return Math.max(blockLight, Math.max(0, skyLight - reduction));
    }

    // Returns the sky-light subtraction for a given world time (0–23999).
    // Vanilla uses 0 during full day and 11 during full night, with linear
    // ramps at dusk (~12000–14000) and dawn (~22000–24000).
    private static int skyLightReduction(long time) {
        if (time < 12000) return 0;
        if (time < 14000) return (int) ((time - 12000) * 11 / 2000);
        if (time < 22000) return 11;
        if (time < 24000) return (int) ((24000 - time) * 11 / 2000);
        return 0;
    }

    public static void register(JavaPlugin plugin) {
        int threshold = plugin.getConfig().getInt("darkness.threshold", 3);
        int duration  = plugin.getConfig().getInt("darkness.duration", 40);
        long interval = plugin.getConfig().getLong("darkness.check-interval", 10L);
        new DarknessListener(threshold, duration).runTaskTimer(plugin, 0L, interval);
    }
}