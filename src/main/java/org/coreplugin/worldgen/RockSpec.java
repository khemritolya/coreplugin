package org.coreplugin.worldgen;

import org.bukkit.Material;

public class RockSpec {
    public final int          centerX, centerY, centerZ, radius;
    public final FormationType type;
    public final Material      oreType;   // null unless ORE_CAVE
    public final int           entranceY; // ground surface level at this rock

    public RockSpec(int centerX, int centerY, int centerZ, int radius,
                    FormationType type, Material oreType, int entranceY) {
        this.centerX   = centerX;
        this.centerY   = centerY;
        this.centerZ   = centerZ;
        this.radius    = radius;
        this.type      = type;
        this.oreType   = oreType;
        this.entranceY = entranceY;
    }
}