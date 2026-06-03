package org.coreplugin;

import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import net.minecraft.server.v1_8_R3.GenericAttributes;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UplinkBeaconListener implements Listener {

    private static final String GUARDIAN_NAME = ChatColor.DARK_RED + "Uplink Guardian";

    // Maps beacon center "x,z" → UUID of the currently living guardian
    private final Map<String, UUID> activeGuardians = new HashMap<>();

    public UplinkBeaconListener() {
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = e.getPlayer().getItemInHand();
        if (!isUplinkCard(item)) return;

        Block block = e.getClickedBlock();
        if (block == null || block.getType() != Material.OBSIDIAN) return;

        e.setCancelled(true);

        String key = block.getX() + "," + block.getZ();
        UUID existing = activeGuardians.get(key);
        if (existing != null) {
            World world = block.getWorld();
            for (Entity candidate : world.getEntities()) {
                if (candidate.getUniqueId().equals(existing) && !candidate.isDead()) return;
            }
            // Guardian is gone — clear stale entry so we can respawn
            activeGuardians.remove(key);
        }

        ItemStack hand = e.getPlayer().getItemInHand();
        if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
        else e.getPlayer().setItemInHand(null);

        spawnGuardian(block, key);
    }

    private void spawnGuardian(Block block, String key) {
        World world = block.getWorld();
        int topY = findTowerTop(block);
        Location loc = new Location(world, block.getX() + 0.5, topY, block.getZ() + 0.5);

        Zombie zombie = (Zombie) world.spawnEntity(loc, EntityType.ZOMBIE);

        zombie.setCustomName(GUARDIAN_NAME);
        zombie.setCustomNameVisible(true);
        zombie.setBaby(false);
        zombie.setCanPickupItems(false);
        zombie.setRemoveWhenFarAway(false);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 2, false, false), true);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 3, false, false), true);

        EntityEquipment eq = zombie.getEquipment();
        eq.setHelmet(new ItemStack(Material.SKULL_ITEM, 1, (short) 1)); // wither skeleton skull
        eq.setChestplate(CustomItems.loadDiamondoidChestplate());
        eq.setLeggings(CustomItems.loadDiamondoidLeggings());
        eq.setItemInHand(CustomItems.loadDiamondoidSword());
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);
        eq.setItemInHandDropChance(0f);

        ((CraftLivingEntity) zombie).getHandle()
                .getAttributeInstance(GenericAttributes.c)
                .setValue(1.0);

        activeGuardians.put(key, zombie.getUniqueId());
    }

    // Scan downward from y=255 to find the topmost obsidian block at this column.
    private int findTowerTop(Block base) {
        World world = base.getWorld();
        int x = base.getX(), z = base.getZ();
        for (int y = 255; y >= base.getY(); y--) {
            if (world.getBlockAt(x, y, z).getType() == Material.OBSIDIAN) {
                return y + 1;
            }
        }
        return base.getY() + 1;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        ItemStack weapon = ((Player) e.getDamager()).getItemInHand();
        if (weapon == null || weapon.getItemMeta() == null) return;
        if (!CustomItems.MONOMOLECULAR_BLADE_NAME.equals(weapon.getItemMeta().getDisplayName())) return;

        if (!(e.getEntity() instanceof LivingEntity)) return;
        EntityEquipment eq = ((LivingEntity) e.getEntity()).getEquipment();
        if (eq == null || eq.getChestplate() == null || eq.getChestplate().getItemMeta() == null) return;
        if (!CustomItems.DIAMONDOID_CHESTPLATE_NAME.equals(eq.getChestplate().getItemMeta().getDisplayName())) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        UUID id = e.getEntity().getUniqueId();
        String key = null;
        for (Map.Entry<String, UUID> entry : activeGuardians.entrySet()) {
            if (entry.getValue().equals(id)) { key = entry.getKey(); break; }
        }
        if (key == null) return;

        activeGuardians.remove(key);
        e.getDrops().clear();
        e.getDrops().add(CustomItems.loadUplinkGuardianHead());
        e.getDrops().add(CustomItems.loadDiamondoidChestplate());
        e.getDrops().add(CustomItems.loadDiamondoidLeggings());
        e.getDrops().add(CustomItems.loadDiamondoidSword());
        e.getDrops().add(CustomItems.loadUplinkBeaconDrop());
        e.setDroppedExp(0);
    }

    private static boolean isUplinkCard(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        net.minecraft.server.v1_8_R3.ItemStack nms = CraftItemStack.asNMSCopy(item);
        if (!nms.hasTag()) return false;
        NBTTagCompound tag = nms.getTag();
        return tag.hasKey("UplinkCard") && tag.getByte("UplinkCard") == 1;
    }
}