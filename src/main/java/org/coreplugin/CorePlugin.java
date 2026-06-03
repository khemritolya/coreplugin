package org.coreplugin;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.coreplugin.worldgen.DesertWorldGenerator;
import org.coreplugin.worldgen.EndWorldGenerator;

import java.util.HashSet;
import java.util.Set;

public final class CorePlugin extends JavaPlugin {

    DesertWorldGenerator generator;
    private final Set<String> generatorWorldNames = new HashSet<>();
    private SandstormManager sandstorm;
    private World endWorld;

    @Override
    public void onLoad() {
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        saveResource("welcome_book.txt", false);
        saveResource("crimes.txt", false);
        sandstorm = SandstormManager.register(this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new BedSpawnListener(), this);
        getServer().getPluginManager().registerEvents(new GhastSoundListener(), this);
        getServer().getPluginManager().registerEvents(new SilverfishSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new MobSpawnListener(), this);
        getServer().getPluginManager().registerEvents(new MobListener(this), this);
        getServer().getPluginManager().registerEvents(new WeatherListener(sandstorm), this);
        getServer().getPluginManager().registerEvents(new FoodListener(), this);
        getServer().getPluginManager().registerEvents(new AnvilListener(), this);
        double glowstoneChance = getConfig().getDouble("jungle-trees.glowstone-leaf-chance", 0.05);
        getServer().getPluginManager().registerEvents(new JungleTreeListener(glowstoneChance), this);
        HostileMobTask.register(this, sandstorm);
        OreCaveDamageTask.register(this);
        CustomItemsTask.register(this);
        getServer().getPluginManager().registerEvents(new PhaseDeviceListener(this), this);
        getServer().getPluginManager().registerEvents(new UplinkBeaconListener(), this);
        getServer().getPluginManager().registerEvents(new UplinkDestinationListener(this), this);
        DarknessListener.register(this);
        getCommand("sandstorm").setExecutor(new SandstormCommand(sandstorm));

        getServer().getScheduler().runTask(this, () -> {
            endWorld = new WorldCreator("game_end")
                    .environment(World.Environment.THE_END)
                    .generator(new EndWorldGenerator())
                    .generateStructures(false)
                    .createWorld();
        });
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onExplode(EntityExplodeEvent e) { e.setYield(1.0f); }
            @EventHandler
            public void onDoorClick(PlayerInteractEvent e) {
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK
                        && e.getClickedBlock().getType() == Material.WOODEN_DOOR
                        && endWorld != null
                        && e.getPlayer().getWorld().equals(endWorld)) {
                    e.setCancelled(true);
                }
            }
        }, this);

        // Main world is already loaded before onEnable on CraftBukkit 1.8;
        // set spawn now for any matching world that is already up.
        for (World world : getServer().getWorlds()) {
            if (generatorWorldNames.contains(world.getName())) {
                applyOasisSpawn(world);
            }
        }
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        if (generator == null) {
            generator = new DesertWorldGenerator(this);
        }
        generatorWorldNames.add(worldName);
        return generator;
    }

    public void applyOasisSpawn(World world) {
        world.setTime(6000);
        world.setGameRuleValue("doDaylightCycle", "false");
        if (generator == null) return;
        int[] pos = generator.findOasisSpawn(world.getSeed());
        if (pos == null) return;
        world.setSpawnLocation(pos[0], pos[1], pos[2]);
        getLogger().info("Spawn set inside oasis at " + pos[0] + ", " + pos[1] + ", " + pos[2]
                         + " in world '" + world.getName() + "'");
    }

    public boolean isGeneratorWorld(String worldName) {
        return generatorWorldNames.contains(worldName);
    }

    public DesertWorldGenerator getGenerator() {
        return generator;
    }

    public World getEndWorld() {
        return endWorld;
    }
}