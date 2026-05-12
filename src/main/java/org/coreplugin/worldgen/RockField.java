package org.coreplugin.worldgen;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RockField {

    private final long     seed;
    private final int      cellSize;
    private final int      radius;
    private final double   spawnChance;
    private final double   embedFactor;
    private final double   oasisThreshold;
    private final double   oreCaveThreshold;
    private final double[] oreThresholds;
    private final FBMNoise noise;
    private final int      minHeight;
    private final int      maxHeight;

    private static final Material[] ORE_TYPES = {
        Material.COAL_ORE, Material.IRON_ORE, Material.REDSTONE_ORE,
        Material.GOLD_ORE, Material.LAPIS_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE
    };

    public RockField(long seed, int cellSize, int radius, double spawnChance, double embedFactor,
                     double oasisChance, double oreCaveChance, double[] oreWeights,
                     FBMNoise noise, int minHeight, int maxHeight) {
        this.seed        = seed;
        this.cellSize    = cellSize;
        this.radius      = radius;
        this.spawnChance = spawnChance;
        this.embedFactor = embedFactor;
        this.noise       = noise;
        this.minHeight   = minHeight;
        this.maxHeight   = maxHeight;

        this.oasisThreshold   = oasisChance;
        this.oreCaveThreshold = oasisChance + oreCaveChance;

        double sum = 0;
        for (double w : oreWeights) sum += w;
        oreThresholds = new double[oreWeights.length];
        double cumulative = 0;
        for (int i = 0; i < oreWeights.length - 1; i++) {
            cumulative += oreWeights[i] / sum;
            oreThresholds[i] = cumulative;
        }
        oreThresholds[oreWeights.length - 1] = 1.0;
    }

    public List<RockSpec> getRocksNear(int chunkX, int chunkZ) {
        List<RockSpec> result = new ArrayList<>();

        int worldX = chunkX * 16;
        int worldZ = chunkZ * 16;
        int searchRadius = (radius + 16) / cellSize + 1;
        int centerCellX  = Math.floorDiv(worldX, cellSize);
        int centerCellZ  = Math.floorDiv(worldZ, cellSize);

        for (int cx = centerCellX - searchRadius; cx <= centerCellX + searchRadius; cx++) {
            for (int cz = centerCellZ - searchRadius; cz <= centerCellZ + searchRadius; cz++) {
                RockSpec rock = getRockInCell(cx, cz);
                if (rock != null) result.add(rock);
            }
        }

        return result;
    }

    public RockSpec findNearestOasis(int maxCellRadius) {
        for (int r = 0; r <= maxCellRadius; r++) {
            for (int cx = -r; cx <= r; cx++) {
                for (int cz = -r; cz <= r; cz++) {
                    if (Math.abs(cx) != r && Math.abs(cz) != r) continue; // ring boundary only
                    RockSpec rock = getRockInCell(cx, cz);
                    if (rock != null && rock.type == FormationType.OASIS) return rock;
                }
            }
        }
        return null;
    }

    private RockSpec getRockInCell(int cellX, int cellZ) {
        long hash = seed
            ^ (long) cellX * 0x9E3779B97F4A7C15L
            ^ (long) cellZ * 0x6C62272E07BB0142L;
        Random rng = new Random(hash);

        if (rng.nextDouble() > spawnChance) return null;

        int rockX = cellX * cellSize + rng.nextInt(cellSize);
        int rockZ = cellZ * cellSize + rng.nextInt(cellSize);

        double noiseVal = noise.evaluate(rockX, rockZ);
        int surface = minHeight + (int) ((noiseVal + 1.0) / 2.0 * (maxHeight - minHeight));
        int rockY   = (int) (surface + radius * embedFactor);

        double typeRoll = rng.nextDouble();
        FormationType type;
        if      (typeRoll < oasisThreshold)   type = FormationType.OASIS;
        else if (typeRoll < oreCaveThreshold)  type = FormationType.ORE_CAVE;
        else                                   type = FormationType.VILLAGE;

        Material oreType = null;
        if (type == FormationType.ORE_CAVE) {
            double oreRoll = rng.nextDouble();
            oreType = ORE_TYPES[ORE_TYPES.length - 1];
            for (int i = 0; i < oreThresholds.length - 1; i++) {
                if (oreRoll < oreThresholds[i]) {
                    oreType = ORE_TYPES[i];
                    break;
                }
            }
        }

        return new RockSpec(rockX, rockY, rockZ, radius, type, oreType, surface);
    }
}