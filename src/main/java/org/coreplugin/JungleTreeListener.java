package org.coreplugin;

import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.Random;

public class JungleTreeListener implements Listener {

    private static final Random RNG = new Random();
    private final double glowstoneChance;

    public JungleTreeListener(double glowstoneChance) {
        this.glowstoneChance = glowstoneChance;
    }

    @EventHandler
    public void onStructureGrow(StructureGrowEvent event) {
        TreeType species = event.getSpecies();
        if (species != TreeType.JUNGLE && species != TreeType.SMALL_JUNGLE && species != TreeType.JUNGLE_BUSH) {
            return;
        }
        for (BlockState state : event.getBlocks()) {
            if (state.getType() == Material.LEAVES && RNG.nextDouble() < glowstoneChance) {
                state.setType(Material.GLOWSTONE);
                state.setRawData((byte) 0);
            }
        }
    }
}