package org.coreplugin;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.coreplugin.worldgen.EndWorldGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UplinkDestinationListener implements Listener {

    private static final String CORRECT_ADDRESS = "QE374MN";
    private static final long TIMEOUT_TICKS = 20L * 60;

    private final CorePlugin plugin;
    private final Map<UUID, BukkitTask> pending = new HashMap<>();

    public UplinkDestinationListener(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = e.getPlayer().getItemInHand();
        if (!isUplinkBeacon(item)) return;

        e.setCancelled(true);

        Player player = e.getPlayer();
        UUID id = player.getUniqueId();

        BukkitTask existing = pending.remove(id);
        if (existing != null) existing.cancel();

        player.sendMessage(ChatColor.YELLOW + "<hyperbridge> Please enter the target Hyperbridge Address in the chat.");

        BukkitTask timeout = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pending.remove(id);
            player.sendMessage(ChatColor.YELLOW + "<hyperbridge> Beacon timed out.");
        }, TIMEOUT_TICKS);

        pending.put(id, timeout);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        UUID id = player.getUniqueId();
        if (!pending.containsKey(id)) return;

        e.setCancelled(true);

        BukkitTask task = pending.remove(id);
        if (task != null) task.cancel();

        if (!e.getMessage().equals(CORRECT_ADDRESS)) {
            player.sendMessage(ChatColor.YELLOW + "<hyperbridge> Unknown address.");
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack slot = player.getInventory().getItem(i);
                if (!isUplinkBeacon(slot)) continue;
                if (slot.getAmount() > 1) slot.setAmount(slot.getAmount() - 1);
                else player.getInventory().setItem(i, null);
                break;
            }

            player.sendMessage(ChatColor.YELLOW + "<hyperbridge> Address Verified.");

            World end = plugin.getEndWorld();
            if (end == null) return;

            Location dest = new Location(end,
                    EndWorldGenerator.STRUCT_X + 0.5,
                    EndWorldGenerator.STRUCT_Y + 1,
                    EndWorldGenerator.STRUCT_Z + 0.5);

            Location origin = player.getLocation();
            for (Player nearby : player.getWorld().getPlayers()) {
                Location nearbyLocation = nearby.getLocation();
                if (nearbyLocation.distanceSquared(origin) <= 100) {
                    JoinListener.satSend("Exile Biosignature Tracking Lost!", plugin);
                    JoinListener.satSend("Nickname: " + ChatColor.AQUA + nearby.getName(), plugin);
                    nearby.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "Space unfurls and Time stops. You are pulled through a hole in Euclidean space, and find yourself on the other side of the Universe.");

                    PhaseDeviceListener.spawnExplosion(nearbyLocation);
                    nearby.teleport(dest);
                    nearby.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 0, false, false));
                    PhaseDeviceListener.spawnExplosion(dest);
                    nearby.sendMessage("<" + ILSAN_NAME + "> Hey " + nearby.getName() + "! Welcome to the `Sorae'. You're finally among friends and probably deserve some rest. Whenever you feel rested however, remember that there are still others trapped on HD 31174 c. Click on the Obsidian Hyperbridge Generator to return to help them out!");
                }
            }
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> ensureIlsanPresent(end, dest), 2L);
        });
    }

    @EventHandler
    public void onEndInteract(PlayerInteractEvent e) {
        World end = plugin.getEndWorld();
        if (end == null || !e.getPlayer().getWorld().equals(end)) return;
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) return;

        e.setCancelled(true);

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK
                && e.getClickedBlock() != null
                && e.getClickedBlock().getType() == Material.OBSIDIAN) {
            Player player = e.getPlayer();
            player.removePotionEffect(PotionEffectType.SATURATION);
            World overworld = plugin.getServer().getWorlds().get(0);
            player.teleport(overworld.getSpawnLocation());
            player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "Space unfurls and Time stops. You are pulled through a hole in Euclidean space, and find yourself on the other side of the Universe.");
            JoinListener.satSend("Detected Exile Biosignature!", plugin);
            JoinListener.satSend("Nickname: " + ChatColor.AQUA + player.getName(), plugin);
            JoinListener.satSend("Crime: " + ChatColor.RED + JoinListener.getCrime(plugin, player), plugin);
        }
    }

    @EventHandler
    public void onEndDamage(EntityDamageByEntityEvent e) {
        World end = plugin.getEndWorld();
        if (end == null || !e.getEntity().getWorld().equals(end)) return;
        if (e.getDamager() instanceof Player && ((Player) e.getDamager()).getGameMode() == GameMode.CREATIVE) return;
        if (e.getDamager() instanceof Player) e.setCancelled(true);
    }

    @EventHandler
    public void onEndBlockBreak(BlockBreakEvent e) {
        World end = plugin.getEndWorld();
        if (end == null || !e.getPlayer().getWorld().equals(end)) return;
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.getEntityType() != EntityType.ENDER_DRAGON) return;
        World end = plugin.getEndWorld();
        if (end != null && e.getEntity().getWorld().equals(end)) {
            e.setCancelled(true);
        }
    }

    private static final String ILSAN_NAME = "Ilsan Melchizedek";

    private void ensureIlsanPresent(World end, Location dest) {
        for (Villager v : end.getEntitiesByClass(Villager.class)) {
            if (ILSAN_NAME.equals(v.getCustomName()) && !v.isDead()) return;
        }
        end.loadChunk(dest.getBlockX() >> 4, dest.getBlockZ() >> 4);
        Villager ilsan = (Villager) end.spawnEntity(dest, EntityType.VILLAGER);
        ilsan.setCustomName(ILSAN_NAME);
        ilsan.setCustomNameVisible(true);
        ilsan.setRemoveWhenFarAway(false);
        ilsan.setCanPickupItems(false);
        ilsan.setProfession(Villager.Profession.LIBRARIAN);
        net.minecraft.server.v1_8_R3.MerchantRecipeList offers =
                ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftVillager) ilsan).getHandle().getOffers(null);
        if (offers != null) offers.clear();
    }

    private static boolean isUplinkBeacon(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) return false;
        if (item.getItemMeta() == null) return false;
        return CustomItems.UPLINK_BEACON_DROP_NAME.equals(item.getItemMeta().getDisplayName());
    }
}