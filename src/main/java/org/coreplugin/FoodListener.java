package org.coreplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class FoodListener implements Listener {

    private static final String SLOP_LORE_LINE = ChatColor.RESET + "" + ChatColor.GRAY + "by Nitchisu Inc.";

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() != Material.COOKIE) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return;

        List<String> lore = meta.getLore();
        if (lore == null || !lore.contains(SLOP_LORE_LINE)) return;

        Player player = event.getPlayer();
        // 20 ticks per second: nausea 10s = 200 ticks, saturation 3s = 60 ticks
        player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 200, 0), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 60, 0), true);
    }
}