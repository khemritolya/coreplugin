package org.coreplugin;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SpeedBootsTask extends BukkitRunnable {

    // Duration is longer than the check interval so effects never flicker while boots are worn.
    private static final int CHECK_INTERVAL = 20;
    private static final int EFFECT_DURATION = 40;

    private static final PotionEffect SPEED      = new PotionEffect(PotionEffectType.SPEED,      EFFECT_DURATION, 19, false, false);
    private static final PotionEffect JUMP_BOOST = new PotionEffect(PotionEffectType.JUMP,       EFFECT_DURATION,  1, false, false);
    private static final PotionEffect HUNGER     = new PotionEffect(PotionEffectType.HUNGER,     EFFECT_DURATION,  4, false, false);

    private final CorePlugin plugin;

    private SpeedBootsTask(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (isWearingSpeedBoots(player)) {
                player.addPotionEffect(SPEED,      true);
                player.addPotionEffect(JUMP_BOOST, true);
                player.addPotionEffect(HUNGER,     true);
            }
        }
    }

    private static boolean isWearingSpeedBoots(Player player) {
        ItemStack boots = player.getInventory().getBoots();
        if (boots == null) return false;
        ItemMeta meta = boots.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && meta.getDisplayName().equals(CustomItems.SPEED_BOOTS_NAME);
    }

    public static void register(CorePlugin plugin) {
        new SpeedBootsTask(plugin).runTaskTimer(plugin, 0L, CHECK_INTERVAL);
    }
}