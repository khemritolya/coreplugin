package org.coreplugin;

import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class CustomItemsTask extends BukkitRunnable {

    private static final int CHECK_INTERVAL = 20;
    private static final int EFFECT_DURATION = 40;

    private static final double BASE_MAX_HEALTH = 20.0;

    private static final PotionEffect SPEED      = new PotionEffect(PotionEffectType.SPEED,  EFFECT_DURATION, 19, false, false);
    private static final PotionEffect JUMP_BOOST = new PotionEffect(PotionEffectType.JUMP,   EFFECT_DURATION,  2, false, false);
    private static final PotionEffect HUNGER     = new PotionEffect(PotionEffectType.HUNGER, EFFECT_DURATION,  4, false, false);

    private final CorePlugin plugin;

    private CustomItemsTask(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            handleSpeedBoots(player);
            handleNanoCrystal(player);
        }
    }

    private static void handleSpeedBoots(Player player) {
        ItemStack boots = player.getInventory().getBoots();
        if (boots == null) return;
        ItemMeta meta = boots.getItemMeta();
        if (meta != null && meta.hasDisplayName()
                && meta.getDisplayName().equals(CustomItems.SPEED_BOOTS_NAME)) {
            player.addPotionEffect(SPEED,      true);
            player.addPotionEffect(JUMP_BOOST, true);
            player.addPotionEffect(HUNGER,     true);
        }
    }

    private static void handleNanoCrystal(Player player) {
        int hearts = sumCrystalHearts(player.getInventory().getContents())
                   + sumCrystalHearts(player.getInventory().getArmorContents());
        double newMax = BASE_MAX_HEALTH + hearts * 2.0;
        if (player.getMaxHealth() == newMax) return;
        player.setMaxHealth(newMax);
        if (player.getHealth() > newMax)
            player.setHealth(newMax);
    }

    private static int sumCrystalHearts(ItemStack[] slots) {
        int total = 0;
        for (ItemStack stack : slots) {
            if (stack == null) continue;
            net.minecraft.server.v1_8_R3.ItemStack nms = CraftItemStack.asNMSCopy(stack);
            if (nms == null || !nms.hasTag()) continue;
            NBTTagCompound tag = nms.getTag();
            if (tag.hasKey("NanoCrystalHearts"))
                total += tag.getByte("NanoCrystalHearts");
        }
        return total;
    }

    public static void register(CorePlugin plugin) {
        new CustomItemsTask(plugin).runTaskTimer(plugin, 0L, CHECK_INTERVAL);
    }
}