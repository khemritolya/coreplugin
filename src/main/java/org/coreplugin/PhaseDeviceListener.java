package org.coreplugin;

import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;


public class PhaseDeviceListener implements Listener {

    private final Plugin plugin;

    public PhaseDeviceListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        if (player.getWorld().getEnvironment() == World.Environment.THE_END) return;

        ItemStack item = player.getItemInHand();
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        if (!meta.getDisplayName().equals(CustomItems.PHASE_DEVICE_NAME)) return;

        event.setCancelled(true);

        net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsStack.hasTag() ? nmsStack.getTag() : new NBTTagCompound();

        if (!tag.hasKey("PhaseIdMost")) return; // not a real Phase Device

        long cooldownMs    = tag.getLong("PhaseCooldownMs");
        long durationTicks = tag.getLong("PhaseDurationTicks");

        long now = System.currentTimeMillis();
        long readyAt = tag.getLong("PhaseReadyAt");
        if (now < readyAt) {
            long remaining = (readyAt - now + 999) / 1000;
            player.sendMessage("The " + CustomItems.PHASE_DEVICE_NAME + ChatColor.RESET +
                    " is cooling down. Try again in " + ChatColor.AQUA + remaining + "s" + ChatColor.RESET + ".");
            return;
        }

        tag.setLong("PhaseReadyAt", now + cooldownMs);
        nmsStack.setTag(tag);
        player.setItemInHand(CraftItemStack.asBukkitCopy(nmsStack));

        player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "Reality bends, and you rotate out of phase with Euclidean existence.");
        player.setGameMode(GameMode.SPECTATOR);
        spawnExplosion(player.getLocation());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.getGameMode() == GameMode.SPECTATOR) {
                    player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "Reality bends again, and Euclidean existence reasserts itself.");
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 400, 0), true);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 400, 4), true);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 400, 1), true);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 400, 4), true);

                    spawnExplosion(player.getLocation());
                    player.setGameMode(GameMode.SURVIVAL);
                }
            }
        }.runTaskLater(plugin, durationTicks + 1L);
    }

    public static void spawnExplosion(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta fwMeta = fw.getFireworkMeta();
        fwMeta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(org.bukkit.Color.fromRGB(170, 0, 170))
                .withFade(org.bukkit.Color.fromRGB(255, 85, 255))
                .build());
        fw.setFireworkMeta(fwMeta);
        fw.detonate();
    }
}