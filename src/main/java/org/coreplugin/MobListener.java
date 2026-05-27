package org.coreplugin;

import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Silverfish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

public class MobListener implements Listener {

    private final CorePlugin plugin;

    public MobListener(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) return;
        if (!(event.getEntity() instanceof Ageable)) return;
        Ageable animal = (Ageable) event.getEntity();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!animal.isValid()) return;
            animal.setCustomName(null);
            animal.setCustomNameVisible(false);
            animal.setBaby();
        }, 1L);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof Fireball) {
            event.blockList().clear();
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        EntityDamageEvent cause = event.getEntity().getLastDamageCause();
        if (!(cause instanceof EntityDamageByEntityEvent)) return;

        Entity damager = ((EntityDamageByEntityEvent) cause).getDamager();

        // Unwrap projectiles (e.g. ghast fireballs) to their shooter.
        Entity attacker = damager;
        if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Entity) attacker = (Entity) shooter;
        }

        String msg = event.getDeathMessage();
        if (msg == null) return;

        if (attacker instanceof Silverfish && attacker.hasMetadata("dangerousCritter")) {
            event.setDeathMessage(msg.replace("Silverfish", "Sandfish"));
        } else if (attacker instanceof Ghast && attacker.hasMetadata("dangerousCritter")) {
            event.setDeathMessage(msg.replace("Ghast", "Cubozoa"));
        }
    }
}