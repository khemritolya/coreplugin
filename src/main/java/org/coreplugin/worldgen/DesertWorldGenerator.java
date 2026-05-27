package org.coreplugin.worldgen;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.coreplugin.CustomItems;
import org.coreplugin.RngUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DesertWorldGenerator extends ChunkGenerator {

    // Terrain
    private final int    minHeight, maxHeight;
    private final int    octaves;
    private final double frequency, lacunarity, persistence, warpStrength;

    // Rock placement
    private final int    rockCellSize;
    private final int    rockRadius;
    private final int    rockShellThickness;
    private final double rockSpawnChance;
    private final double rockDeformStrength;
    private final double rockDeformScale;
    private final double rockEmbedFactor;
    private final double rockOasisChance;
    private final double rockOreCaveChance;
    private final double[] rockOreWeights;

    // Oasis interior
    private final double oasisPoolRadius;
    private final int    oasisPoolDepth;
    private final double oasisGlowstoneDensity;
    private final double oasisCeilingGlowstoneChance;
    private final double oasisCeilingVineChance;
    private final int    oasisVineMinLength;
    private final int    oasisVineMaxLength;
    private final int    oasisSlopeRise;
    private final int    oasisDirtLayers;
    private final double oasisFloorNoiseScale;
    private final int    oasisFloorNoiseAmplitude;
    private final int    oasisPondBoundaryAmplitude;
    private final double oasisClayRadius;
    private final double   oasisDecorationDensity;
    private final double[] oasisDecorationThresholds;
    private final double[] oasisMobRates;
    private final double   oasisCacheChance;
    private final double   oasisCacheCellFillChance;
    private final double[] oasisCacheItemThresholds;

    private static final String[] CACHE_ITEM_KEYS = {
        "cookies", "hard-hat", "prospector-pickaxe", "monomolecular-blade", "water-bucket",
        "cow-egg", "pig-egg", "sheep-egg", "chicken-egg"
    };
    private static final double[] CACHE_ITEM_DEFAULTS = { 5.0, 10.0, 10.0, 1.0, 4.0, 2.0, 2.0, 2.0, 2.0 };

    private static final String[] DECORATION_KEYS = {
        "tall-grass", "fern", "dandelion", "poppy",
        "blue-orchid", "allium", "azure-bluet", "oxeye-daisy", "jungle-sapling"
    };
    private static final double[] DECORATION_DEFAULTS = {
        0.35, 0.09, 0.11, 0.09, 0.07, 0.07, 0.06, 0.04, 0.12
    };
    private static final MaterialData[] DECORATION_TYPES = {
        new MaterialData(Material.LONG_GRASS,    (byte) 1),
        new MaterialData(Material.LONG_GRASS,    (byte) 2),
        new MaterialData(Material.YELLOW_FLOWER, (byte) 0),
        new MaterialData(Material.RED_ROSE,      (byte) 0),
        new MaterialData(Material.RED_ROSE,      (byte) 1),
        new MaterialData(Material.RED_ROSE,      (byte) 2),
        new MaterialData(Material.RED_ROSE,      (byte) 3),
        new MaterialData(Material.RED_ROSE,      (byte) 8),
        new MaterialData(Material.SAPLING,       (byte) 3),
    };

    // Shell clay type
    private final double[] shellClayThresholds;

    private static final String[]       SHELL_CLAY_KEYS     = { "regular", "red", "orange", "pink", "black" };
    private static final double[]       SHELL_CLAY_DEFAULTS = {     0.50,   0.25,    0.15,   0.07,   0.03  };
    private static final MaterialData[] SHELL_CLAY_TYPES    = {
        new MaterialData(Material.HARD_CLAY,    (byte)  0),
        new MaterialData(Material.STAINED_CLAY, (byte) 14),
        new MaterialData(Material.STAINED_CLAY, (byte)  1),
        new MaterialData(Material.STAINED_CLAY, (byte)  6),
        new MaterialData(Material.STAINED_CLAY, (byte) 15),
    };

    // Spice fields
    private final double spiceFieldRadius;
    private final double spiceFieldMaxChance;
    private final int    spiceFieldDepth;

    // Ore cave interior
    private final double[] oreDensities;

    // Runtime
    private final JavaPlugin plugin;
    private FBMNoise     noise;
    private RockField    rockField;
    private SimplexNoise rockNoise;
    private long         cachedSeed = Long.MIN_VALUE;

    public DesertWorldGenerator(JavaPlugin plugin) {
        this.plugin = plugin;
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("world-gen");
        minHeight = cfg.getInt("min-height", 40);
        maxHeight = cfg.getInt("max-height", 80);

        ConfigurationSection noiseCfg = cfg.getConfigurationSection("noise");
        octaves      = noiseCfg.getInt("octaves", 3);
        frequency    = noiseCfg.getDouble("frequency", 0.003);
        lacunarity   = noiseCfg.getDouble("lacunarity", 2.0);
        persistence  = noiseCfg.getDouble("persistence", 0.5);
        warpStrength = noiseCfg.getDouble("warp-strength", 32.0);

        ConfigurationSection rockCfg = cfg.getConfigurationSection("rocks");
        rockCellSize       = rockCfg.getInt("cell-size", 200);
        rockRadius         = rockCfg.getInt("radius", 30);
        rockShellThickness = rockCfg.getInt("shell-thickness", 3);
        rockSpawnChance    = rockCfg.getDouble("spawn-chance", 0.5);
        rockDeformStrength = rockCfg.getDouble("deform-strength", 4.0);
        rockDeformScale    = rockCfg.getDouble("deform-scale", 0.08);
        rockEmbedFactor    = rockCfg.getDouble("embed-factor", 0.2);

        ConfigurationSection typeChances = rockCfg.getConfigurationSection("type-chances");
        rockOasisChance    = typeChances.getDouble("oasis", 0.90);
        rockOreCaveChance  = typeChances.getDouble("ore-cave", 0.10);

        ConfigurationSection oreWeightsCfg = rockCfg.getConfigurationSection("ore-cave")
                                                    .getConfigurationSection("ore-weights");
        rockOreWeights = new double[]{
            oreWeightsCfg.getDouble("coal",     0.30),
            oreWeightsCfg.getDouble("iron",     0.25),
            oreWeightsCfg.getDouble("redstone", 0.15),
            oreWeightsCfg.getDouble("gold",     0.10),
            oreWeightsCfg.getDouble("lapis",    0.10),
            oreWeightsCfg.getDouble("diamond",  0.05),
            oreWeightsCfg.getDouble("emerald",  0.05)
        };

        ConfigurationSection oasisCfg = rockCfg.getConfigurationSection("oasis");
        oasisPoolRadius            = oasisCfg.getDouble("pool-radius",            0.40);
        oasisPoolDepth             = oasisCfg.getInt("pool-depth",                3);
        oasisGlowstoneDensity          = oasisCfg.getDouble("glowstone-density",           0.05);
        oasisCeilingGlowstoneChance    = oasisCfg.getDouble("ceiling-glowstone-chance",    0.05);
        oasisCeilingVineChance         = oasisCfg.getDouble("ceiling-vine-chance",         0.08);
        oasisVineMinLength             = oasisCfg.getInt("vine-min-length",                2);
        oasisVineMaxLength             = oasisCfg.getInt("vine-max-length",                7);
        oasisSlopeRise             = oasisCfg.getInt("slope-rise",               10);
        oasisDirtLayers            = oasisCfg.getInt("dirt-layers",              1);
        oasisFloorNoiseScale          = oasisCfg.getDouble("floor-noise-scale",       0.15);
        oasisFloorNoiseAmplitude      = oasisCfg.getInt("floor-noise-amplitude",     2);
        oasisPondBoundaryAmplitude    = oasisCfg.getInt("pond-boundary-amplitude",   4);
        oasisClayRadius = oasisCfg.getDouble("clay-radius",              1.0);
        oasisDecorationDensity = oasisCfg.getDouble("decoration-density", 0.25);
        oasisCacheChance           = oasisCfg.getDouble("cache-chance",            0.85);
        oasisCacheCellFillChance   = oasisCfg.getDouble("cache-cell-fill-chance", 0.40);
        ConfigurationSection cacheItemCfg = oasisCfg.getConfigurationSection("cache-item-weights");
        double[] cacheItemWeights = new double[CACHE_ITEM_KEYS.length];
        for (int i = 0; i < CACHE_ITEM_KEYS.length; i++) {
            cacheItemWeights[i] = cacheItemCfg != null
                ? cacheItemCfg.getDouble(CACHE_ITEM_KEYS[i], CACHE_ITEM_DEFAULTS[i])
                : CACHE_ITEM_DEFAULTS[i];
        }
        double cacheItemSum = 0;
        for (double w : cacheItemWeights) cacheItemSum += w;
        oasisCacheItemThresholds = new double[cacheItemWeights.length];
        double cacheItemCum = 0;
        for (int i = 0; i < cacheItemWeights.length - 1; i++) {
            cacheItemCum += cacheItemWeights[i] / cacheItemSum;
            oasisCacheItemThresholds[i] = cacheItemCum;
        }
        oasisCacheItemThresholds[cacheItemWeights.length - 1] = 1.0;
        ConfigurationSection mobRatesCfg = oasisCfg.getConfigurationSection("mob-spawn-rates");
        oasisMobRates = new double[MOB_RATE_KEYS.length];
        for (int i = 0; i < MOB_RATE_KEYS.length; i++) {
            oasisMobRates[i] = mobRatesCfg != null
                ? mobRatesCfg.getDouble(MOB_RATE_KEYS[i], MOB_RATE_DEFAULTS[i])
                : MOB_RATE_DEFAULTS[i];
        }
        ConfigurationSection decoWeightsCfg = oasisCfg.getConfigurationSection("decoration-weights");
        double[] decoWeights = new double[DECORATION_KEYS.length];
        for (int i = 0; i < DECORATION_KEYS.length; i++) {
            decoWeights[i] = decoWeightsCfg != null
                ? decoWeightsCfg.getDouble(DECORATION_KEYS[i], DECORATION_DEFAULTS[i])
                : DECORATION_DEFAULTS[i];
        }
        double decoSum = 0;
        for (double w : decoWeights) decoSum += w;
        oasisDecorationThresholds = new double[decoWeights.length];
        double decoCum = 0;
        for (int i = 0; i < decoWeights.length - 1; i++) {
            decoCum += decoWeights[i] / decoSum;
            oasisDecorationThresholds[i] = decoCum;
        }
        oasisDecorationThresholds[decoWeights.length - 1] = 1.0;

        ConfigurationSection shellClayCfg = rockCfg.getConfigurationSection("shell-clay-weights");
        double[] shellClayWeights = new double[SHELL_CLAY_KEYS.length];
        for (int i = 0; i < SHELL_CLAY_KEYS.length; i++) {
            shellClayWeights[i] = shellClayCfg != null
                ? shellClayCfg.getDouble(SHELL_CLAY_KEYS[i], SHELL_CLAY_DEFAULTS[i])
                : SHELL_CLAY_DEFAULTS[i];
        }
        double shellClaySum = 0;
        for (double w : shellClayWeights) shellClaySum += w;
        shellClayThresholds = new double[shellClayWeights.length];
        double shellClayCum = 0;
        for (int i = 0; i < shellClayWeights.length - 1; i++) {
            shellClayCum += shellClayWeights[i] / shellClaySum;
            shellClayThresholds[i] = shellClayCum;
        }
        shellClayThresholds[shellClayWeights.length - 1] = 1.0;

        ConfigurationSection spiceCfg = cfg.getConfigurationSection("spice-fields");
        spiceFieldRadius    = spiceCfg != null ? spiceCfg.getDouble("radius",             80.0) : 80.0;
        spiceFieldMaxChance = spiceCfg != null ? spiceCfg.getDouble("max-replace-chance", 0.6)  : 0.6;
        spiceFieldDepth     = spiceCfg != null ? spiceCfg.getInt(   "depth",              5)    : 5;

        ConfigurationSection oreCaveCfg = rockCfg.getConfigurationSection("ore-cave");
        ConfigurationSection densitiesCfg = oreCaveCfg.getConfigurationSection("densities");
        oreDensities = new double[]{
            densitiesCfg.getDouble("coal",     0.25),
            densitiesCfg.getDouble("iron",     0.20),
            densitiesCfg.getDouble("redstone", 0.18),
            densitiesCfg.getDouble("gold",     0.15),
            densitiesCfg.getDouble("lapis",    0.12),
            densitiesCfg.getDouble("diamond",  0.10),
            densitiesCfg.getDouble("emerald",  0.08)
        };
    }

    private void initIfNeeded(long seed) {
        if (seed == cachedSeed) return;
        cachedSeed = seed;
        noise     = new FBMNoise(seed, octaves, frequency, lacunarity, persistence, warpStrength);
        rockField = new RockField(seed, rockCellSize, rockRadius, rockSpawnChance, rockEmbedFactor,
                                  rockOasisChance, rockOreCaveChance, rockOreWeights,
                                  noise, minHeight, maxHeight);
        rockNoise = new SimplexNoise(seed + 1);
    }

    public boolean isNearAnyRock(long seed, int wx, int wz, double radiusFactor) {
        initIfNeeded(seed);
        if (rockField == null) return false;
        int chunkX = Math.floorDiv(wx, 16);
        int chunkZ = Math.floorDiv(wz, 16);
        for (RockSpec rock : rockField.getRocksNear(chunkX, chunkZ)) {
            double dx = wx - rock.centerX;
            double dz = wz - rock.centerZ;
            if (Math.sqrt(dx * dx + dz * dz) <= radiusFactor * rock.radius) return true;
        }
        return false;
    }

    public boolean isInOreCave(long seed, int wx, int wy, int wz) {
        initIfNeeded(seed);
        if (rockField == null) return false;
        int chunkX = Math.floorDiv(wx, 16);
        int chunkZ = Math.floorDiv(wz, 16);
        for (RockSpec rock : rockField.getRocksNear(chunkX, chunkZ)) {
            if (rock.type != FormationType.ORE_CAVE) continue;
            double dx = wx - rock.centerX;
            double dy = wy - rock.centerY;
            double dz = wz - rock.centerZ;
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) <= rock.radius) return true;
        }
        return false;
    }

    public int[] findOasisSpawn(long seed) {
        initIfNeeded(seed);
        RockSpec rock = rockField.findNearestOasis(50);
        if (rock == null) return null;

        double innerRadius = rock.radius - rockShellThickness;
        double dyC         = rock.centerY - rock.entranceY;
        double footprintR  = Math.sqrt(Math.max(1.0, innerRadius * innerRadius - dyC * dyC));

        // Pick a column 60% of the footprint radius from center — well into the grass area
        int    wx        = rock.centerX + (int) (footprintR * 0.6);
        int    wz        = rock.centerZ;
        double horizDist = footprintR * 0.6;

        // Mirror fillOasis floor calculation exactly so the Y is never inside a block
        double pondBn      = rockNoise.eval(wx * oasisFloorNoiseScale + 900.0, wz * oasisFloorNoiseScale + 900.0);
        double effPoolR    = Math.max(0.5, footprintR * oasisPoolRadius + pondBn * oasisPondBoundaryAmplitude);
        double effSandEdge = effPoolR + oasisClayRadius;
        double slopeRange  = Math.max(1.0, footprintR - 1.0 - effSandEdge);
        double tSlope      = Math.min(1.0, (horizDist - effSandEdge) / slopeRange);
        double fn          = rockNoise.eval(wx * oasisFloorNoiseScale + 500.0, wz * oasisFloorNoiseScale + 500.0);
        int localFloorY    = Math.max(1, rock.entranceY + (int) (tSlope * tSlope * oasisSlopeRise) + (int) (fn * oasisFloorNoiseAmplitude));

        // +1 clears the grass block, +1 clears any decoration placed on top of it
        return new int[]{ wx, localFloorY + 2, wz };
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return Collections.singletonList(new OasisPopulator());
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        initIfNeeded(world.getSeed());

        ChunkData chunk = createChunkData(world);
        int heightRange = maxHeight - minHeight;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;

                double noiseVal = noise.evaluate(worldX, worldZ);
                int surface = minHeight + (int) ((noiseVal + 1.0) / 2.0 * heightRange);

                chunk.setBlock(x, 0, z, Material.BEDROCK);
                for (int y = 1; y <= surface; y++) {
                    chunk.setBlock(x, y, z, new MaterialData(Material.SAND, (byte) 1));
                }

                double spiceFactor = rockField.getSpiceFieldFactor(worldX, worldZ, spiceFieldRadius);
                if (spiceFactor > 0) {
                    long colSeed = cachedSeed ^ ((long) worldX * 0xB5EE8E3FL) ^ ((long) worldZ * 0xC7537B51L);
                    int bottom = Math.max(1, surface - spiceFieldDepth + 1);
                    for (int y = surface; y >= bottom; y--) {
                        int depthBelow = surface - y;
                        double chance = spiceFieldMaxChance * spiceFactor
                                * (1.0 - (double) depthBelow / spiceFieldDepth);
                        long blockSeed = colSeed ^ ((long) y * 0x6C62272E07BB0142L);
                        if (new Random(blockSeed).nextDouble() < chance) {
                            chunk.setBlock(x, y, z, Material.SAND); // white sand
                        }
                    }
                }

                biome.setBiome(x, z, Biome.JUNGLE);
            }
        }

        List<RockSpec> rocks = rockField.getRocksNear(chunkX, chunkZ);
        for (RockSpec rock : rocks) {
            placeShell(rock, chunk, chunkX, chunkZ, world);
            switch (rock.type) {
                case OASIS:    fillOasis(rock, chunk, chunkX, chunkZ);   break;
                case ORE_CAVE: fillOreCave(rock, chunk, chunkX, chunkZ); break;
                default: break;
            }
        }

        return chunk;
    }

    private MaterialData shellClayFor(RockSpec rock) {
        long rockSeed = cachedSeed ^ ((long) rock.centerX * 0x9E3779B97F4A7C15L) ^ ((long) rock.centerZ * 0x6C62272E07BB0142L);
        double roll = new Random(rockSeed ^ 0x534845_4C4C_594CL).nextDouble();
        MaterialData result = SHELL_CLAY_TYPES[SHELL_CLAY_TYPES.length - 1];
        for (int i = 0; i < shellClayThresholds.length - 1; i++) {
            if (roll < shellClayThresholds[i]) {
                result = SHELL_CLAY_TYPES[i];
                break;
            }
        }
        return result;
    }

    private void placeShell(RockSpec rock, ChunkData chunk, int chunkX, int chunkZ, World world) {
        int totalMargin = (int) Math.ceil(rockDeformStrength);
        MaterialData shellClay = shellClayFor(rock);

        int minX = Math.max(0,  rock.centerX - rock.radius - totalMargin - chunkX * 16);
        int maxX = Math.min(15, rock.centerX + rock.radius + totalMargin - chunkX * 16);
        int minZ = Math.max(0,  rock.centerZ - rock.radius - totalMargin - chunkZ * 16);
        int maxZ = Math.min(15, rock.centerZ + rock.radius + totalMargin - chunkZ * 16);
        int minY = Math.max(1,  rock.centerY - rock.radius - totalMargin);
        int maxY = Math.min(world.getMaxHeight() - 1, rock.centerY + rock.radius + totalMargin);

        for (int lx = minX; lx <= maxX; lx++) {
            for (int lz = minZ; lz <= maxZ; lz++) {
                for (int y = minY; y <= maxY; y++) {
                    int wx = chunkX * 16 + lx;
                    int wz = chunkZ * 16 + lz;
                    int dx = wx - rock.centerX;
                    int dy = y  - rock.centerY;
                    int dz = wz - rock.centerZ;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                    double s      = rockDeformScale;
                    double deform = (rockNoise.eval(wx * s, y  * s)
                                   + rockNoise.eval(y  * s, wz * s)
                                   + rockNoise.eval(wx * s, wz * s)) / 3.0;
                    double effective = rock.radius + deform * rockDeformStrength;

                    if (dist <= effective - rockShellThickness) {
                        Material interior;
                        if (rock.type == FormationType.ORE_CAVE) {
                            interior = Material.STONE;
                        } else if (rock.type == FormationType.OASIS) {
                            interior = (y < rock.entranceY) ? Material.STONE : Material.AIR;
                        } else {
                            interior = Material.AIR;
                        }
                        chunk.setBlock(lx, y, lz, interior);
                    } else if (dist <= effective) {
                        chunk.setBlock(lx, y, lz, shellClay);
                    }
                }
            }
        }
    }

    private void fillOasis(RockSpec rock, ChunkData chunk, int chunkX, int chunkZ) {
        long rockSeed = cachedSeed ^ ((long) rock.centerX * 0x9E3779B97F4A7C15L) ^ ((long) rock.centerZ * 0x6C62272E07BB0142L);

        double innerRadius = rock.radius - rockShellThickness;
        int    floorY      = rock.entranceY;

        // True footprint radius at floor level (half-sphere cross-section)
        double dyC        = rock.centerY - rock.entranceY;
        double footprintR = Math.sqrt(Math.max(0, innerRadius * innerRadius - dyC * dyC));
        double poolR      = footprintR * oasisPoolRadius;

        int ir   = (int) innerRadius + 2;
        int minX = Math.max(0,  rock.centerX - ir - chunkX * 16);
        int maxX = Math.min(15, rock.centerX + ir - chunkX * 16);
        int minZ = Math.max(0,  rock.centerZ - ir - chunkZ * 16);
        int maxZ = Math.min(15, rock.centerZ + ir - chunkZ * 16);

        // Floor pass: noisy pool boundary, sandy shore, sloped grass with stone/dirt fill
        for (int lx = minX; lx <= maxX; lx++) {
            for (int lz = minZ; lz <= maxZ; lz++) {
                int wx = chunkX * 16 + lx;
                int wz = chunkZ * 16 + lz;
                double horizDist = Math.sqrt((wx - rock.centerX) * (wx - rock.centerX)
                                           + (wz - rock.centerZ) * (wz - rock.centerZ));

                if (horizDist >= innerRadius + 2) continue;

                // Per-column noisy pool radius and shore edge
                double pondBn      = rockNoise.eval(wx * oasisFloorNoiseScale + 900.0, wz * oasisFloorNoiseScale + 900.0);
                double effPoolR    = Math.max(0.5, poolR + pondBn * oasisPondBoundaryAmplitude);
                double effSandEdge = effPoolR + oasisClayRadius;

                if (horizDist < effPoolR) {
                    // Parabolic depression scaled to noisy pool radius
                    double t = horizDist / effPoolR;
                    int depression = Math.max(0, (int) ((1.0 - t * t) * oasisPoolDepth));
                    int sandY = Math.max(1, floorY - depression);
                    chunk.setBlock(lx, sandY, lz, Material.CLAY);
                    for (int y = sandY + 1; y < floorY; y++) {
                        chunk.setBlock(lx, y, lz, Material.STATIONARY_WATER);
                    }
                } else if (horizDist < effSandEdge) {
                    // Sandy shore strip
                    chunk.setBlock(lx, floorY, lz, Material.CLAY);
                } else {
                    // Grass floor: slope from noisy shore edge to dome wall + noise undulation
                    double localSlopeRange = Math.max(1.0, footprintR - 1.0 - effSandEdge);
                    double tSlope = Math.min(1.0, (horizDist - effSandEdge) / localSlopeRange);
                    double fn = rockNoise.eval(wx * oasisFloorNoiseScale + 500.0, wz * oasisFloorNoiseScale + 500.0);
                    int localFloorY = Math.max(1, floorY + (int)(tSlope * tSlope * oasisSlopeRise) + (int)(fn * oasisFloorNoiseAmplitude));

                    // Stone fill between entrance level and dirt layer
                    for (int fy = floorY; fy < localFloorY - oasisDirtLayers; fy++) {
                        if (chunk.getType(lx, fy, lz) != Material.AIR) continue;
                        chunk.setBlock(lx, fy, lz, Material.STONE);
                    }
                    // Dirt layer (may extend below floorY when slope is shallow)
                    for (int fy = Math.max(1, localFloorY - oasisDirtLayers); fy < localFloorY; fy++) {
                        if (chunk.getType(lx, fy, lz) != Material.AIR) continue;
                        chunk.setBlock(lx, fy, lz, Material.DIRT);
                    }


                    if (chunk.getType(lx, localFloorY, lz) != Material.AIR &&
                            chunk.getType(lx, localFloorY, lz) != Material.STONE) continue;
                    chunk.setBlock(lx, localFloorY, lz, Material.GRASS);

                    // Per-block seeded decorations (independent of chunk bounds)
                    long bSeed = rockSeed ^ ((long) wx * 0xB5EE8E3FL) ^ ((long) wz * 0xC7537B51L);
                    Random blockRng = new Random(bSeed);
                    if (blockRng.nextDouble() < oasisDecorationDensity) {
                        long glowSeed = rockSeed ^ ((long) wx * 0x6C62272E07BB0142L) ^ ((long) wz * 0x9E3779B97F4A7C15L);
                        if (new Random(glowSeed).nextDouble() >= oasisGlowstoneDensity) {
                            double typeRoll = blockRng.nextDouble();
                            MaterialData deco = DECORATION_TYPES[DECORATION_TYPES.length - 1];
                            for (int d = 0; d < oasisDecorationThresholds.length - 1; d++) {
                                if (typeRoll < oasisDecorationThresholds[d]) {
                                    deco = DECORATION_TYPES[d];
                                    break;
                                }
                            }
                            chunk.setBlock(lx, localFloorY + 1, lz, deco);
                        }
                    }
                }
            }
        }

    }

    private void fillOreCave(RockSpec rock, ChunkData chunk, int chunkX, int chunkZ) {
        long rockSeed = cachedSeed ^ ((long) rock.centerX * 0x9E3779B97F4A7C15L) ^ ((long) rock.centerZ * 0x6C62272E07BB0142L);
        double density    = oreDensity(rock.oreType);
        int    totalMargin = (int) Math.ceil(rockDeformStrength);
        int    ir          = rock.radius - rockShellThickness + totalMargin;

        int minX = Math.max(0,   rock.centerX - ir - chunkX * 16);
        int maxX = Math.min(15,  rock.centerX + ir - chunkX * 16);
        int minZ = Math.max(0,   rock.centerZ - ir - chunkZ * 16);
        int maxZ = Math.min(15,  rock.centerZ + ir - chunkZ * 16);
        int minY = Math.max(1,   rock.centerY - ir);
        int maxY = Math.min(255, rock.centerY + ir);

        for (int lx = minX; lx <= maxX; lx++) {
            for (int lz = minZ; lz <= maxZ; lz++) {
                for (int y = minY; y <= maxY; y++) {
                    int wx = chunkX * 16 + lx;
                    int wz = chunkZ * 16 + lz;
                    int dx = wx - rock.centerX;
                    int dy = y  - rock.centerY;
                    int dz = wz - rock.centerZ;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                    double s      = rockDeformScale;
                    double deform = (rockNoise.eval(wx * s, y  * s)
                                   + rockNoise.eval(y  * s, wz * s)
                                   + rockNoise.eval(wx * s, wz * s)) / 3.0;
                    double effectiveInner = rock.radius - rockShellThickness + deform * rockDeformStrength;

                    if (dist >= effectiveInner) continue;

                    long blockSeed = rockSeed ^ ((long) wx * 397L) ^ ((long) y * 1031L) ^ ((long) wz * 587L);
                    Random blockRng = new Random(blockSeed);
                    if (blockRng.nextDouble() >= density) continue;

                    chunk.setBlock(lx, y, lz, rock.oreType);
                }
            }
        }
    }

    private double oreDensity(Material oreType) {
        if (oreType == Material.COAL_ORE)     return oreDensities[0];
        if (oreType == Material.IRON_ORE)     return oreDensities[1];
        if (oreType == Material.REDSTONE_ORE) return oreDensities[2];
        if (oreType == Material.GOLD_ORE)     return oreDensities[3];
        if (oreType == Material.LAPIS_ORE)    return oreDensities[4];
        if (oreType == Material.DIAMOND_ORE)  return oreDensities[5];
        return oreDensities[6];
    }

    private void placeGlowstoneFlowers(RockSpec rock, World world, int chunkX, int chunkZ) {
        long rockSeed = cachedSeed ^ ((long) rock.centerX * 0x9E3779B97F4A7C15L) ^ ((long) rock.centerZ * 0x6C62272E07BB0142L);

        double innerRadius = rock.radius - rockShellThickness;
        int    floorY      = rock.entranceY;

        double dyC        = rock.centerY - rock.entranceY;
        double footprintR = Math.sqrt(Math.max(0, innerRadius * innerRadius - dyC * dyC));
        double poolR      = footprintR * oasisPoolRadius;

        int ir   = (int) innerRadius + 2;
        int minX = Math.max(chunkX * 16,      rock.centerX - ir);
        int maxX = Math.min(chunkX * 16 + 15, rock.centerX + ir);
        int minZ = Math.max(chunkZ * 16,      rock.centerZ - ir);
        int maxZ = Math.min(chunkZ * 16 + 15, rock.centerZ + ir);

        for (int wx = minX; wx <= maxX; wx++) {
            for (int wz = minZ; wz <= maxZ; wz++) {
                double horizDist = Math.sqrt((wx - rock.centerX) * (wx - rock.centerX)
                                           + (wz - rock.centerZ) * (wz - rock.centerZ));
                if (horizDist >= innerRadius) continue;

                double pondBn      = rockNoise.eval(wx * oasisFloorNoiseScale + 900.0, wz * oasisFloorNoiseScale + 900.0);
                double effPoolR    = Math.max(1.0, poolR + pondBn * oasisPondBoundaryAmplitude);
                double effSandEdge = effPoolR + oasisClayRadius;
                if (horizDist < effSandEdge) continue;

                long bSeed = rockSeed ^ ((long) wx * 0x6C62272E07BB0142L) ^ ((long) wz * 0x9E3779B97F4A7C15L);
                if (new Random(bSeed).nextDouble() >= oasisGlowstoneDensity) continue;

                double localSlopeRange = Math.max(1.0, footprintR - effSandEdge);
                double tSlope = Math.min(1.0, (horizDist - effSandEdge) / localSlopeRange);
                double fn = rockNoise.eval(wx * oasisFloorNoiseScale + 500.0, wz * oasisFloorNoiseScale + 500.0);
                int localFloorY = Math.max(1, floorY + (int)(tSlope * tSlope * oasisSlopeRise) + (int)(fn * oasisFloorNoiseAmplitude));

                world.getBlockAt(wx, localFloorY + 1, wz).setType(Material.GLOWSTONE);
            }
        }
    }

    private void placeCeilingGlowstone(RockSpec rock, World world, int chunkX, int chunkZ) {
        long rockSeed = cachedSeed ^ ((long) rock.centerX * 0x9E3779B97F4A7C15L) ^ ((long) rock.centerZ * 0x6C62272E07BB0142L);

        double innerRadius = rock.radius - rockShellThickness;
        int ir   = (int) innerRadius + 2;
        int minX = Math.max(chunkX * 16,      rock.centerX - ir);
        int maxX = Math.min(chunkX * 16 + 15, rock.centerX + ir);
        int minZ = Math.max(chunkZ * 16,      rock.centerZ - ir);
        int maxZ = Math.min(chunkZ * 16 + 15, rock.centerZ + ir);

        for (int wx = minX; wx <= maxX; wx++) {
            for (int wz = minZ; wz <= maxZ; wz++) {
                double horizDist = Math.sqrt((wx - rock.centerX) * (wx - rock.centerX)
                                           + (wz - rock.centerZ) * (wz - rock.centerZ));
                if (horizDist >= innerRadius) continue;

                long bSeed = rockSeed ^ ((long) wx * 0xABCDEF01L) ^ ((long) wz * 0x89ABCDEFL);
                if (new Random(bSeed).nextDouble() >= oasisCeilingGlowstoneChance) continue;

                // Scan from top of interior downward; first air block is the ceiling surface
                int topY       = rock.centerY + (int) Math.sqrt(Math.max(0, innerRadius * innerRadius - horizDist * horizDist));
                int searchFrom = Math.min(topY + rockShellThickness + 2, world.getMaxHeight() - 1);

                for (int y = searchFrom; y >= rock.entranceY; y--) {
                    if (world.getBlockAt(wx, y, wz).getType() != Material.AIR) continue;
                    // Only place if this air block is actually inside the cave (ceiling above is shell)
                    Material above = world.getBlockAt(wx, y + 1, wz).getType();
                    if (above == Material.HARD_CLAY || above == Material.STAINED_CLAY) {
                        world.getBlockAt(wx, y, wz).setType(Material.GLOWSTONE);
                    }
                    break;
                }
            }
        }
    }

    private static final byte[] VINE_FACES = { 0x1, 0x2, 0x4, 0x8 }; // S, W, N, E

    private void placeCeilingVines(RockSpec rock, World world, int chunkX, int chunkZ) {
        long rockSeed = cachedSeed ^ ((long) rock.centerX * 0x9E3779B97F4A7C15L) ^ ((long) rock.centerZ * 0x6C62272E07BB0142L);

        double innerRadius = rock.radius - rockShellThickness;
        int ir   = (int) innerRadius + 2;
        int minX = Math.max(chunkX * 16,      rock.centerX - ir);
        int maxX = Math.min(chunkX * 16 + 15, rock.centerX + ir);
        int minZ = Math.max(chunkZ * 16,      rock.centerZ - ir);
        int maxZ = Math.min(chunkZ * 16 + 15, rock.centerZ + ir);

        int lengthRange = Math.max(1, oasisVineMaxLength - oasisVineMinLength + 1);

        for (int wx = minX; wx <= maxX; wx++) {
            for (int wz = minZ; wz <= maxZ; wz++) {
                double horizDist = Math.sqrt((wx - rock.centerX) * (wx - rock.centerX)
                                           + (wz - rock.centerZ) * (wz - rock.centerZ));
                if (horizDist >= innerRadius) continue;

                long bSeed = rockSeed ^ ((long) wx * 0x1234ABCD5678EFL) ^ ((long) wz * 0xFEDCBA9876543210L);
                Random rng = new Random(bSeed);
                if (rng.nextDouble() >= oasisCeilingVineChance) continue;

                // Find ceiling: scan down from top of interior to find first air under solid
                int topY       = rock.centerY + (int) Math.sqrt(Math.max(0, innerRadius * innerRadius - horizDist * horizDist));
                int searchFrom = Math.min(topY + rockShellThickness + 2, world.getMaxHeight() - 1);

                int ceilingAirY = -1;
                for (int y = searchFrom; y >= rock.entranceY; y--) {
                    if (world.getBlockAt(wx, y, wz).getType() != Material.AIR) continue;
                    Material above = world.getBlockAt(wx, y + 1, wz).getType();
                    if (above == Material.HARD_CLAY || above == Material.STAINED_CLAY) {
                        ceilingAirY = y;
                    }
                    break;
                }
                if (ceilingAirY < 0) continue;

                byte face   = VINE_FACES[rng.nextInt(VINE_FACES.length)];
                int  length = oasisVineMinLength + rng.nextInt(lengthRange);

                for (int i = 0; i < length; i++) {
                    int vineY = ceilingAirY - i;
                    if (vineY < rock.entranceY) break;
                    if (world.getBlockAt(wx, vineY, wz).getType() != Material.AIR) break;
                    world.getBlockAt(wx, vineY, wz).setTypeIdAndData(Material.VINE.getId(), face, false);
                }
            }
        }
    }

    private static final String[]      MOB_RATE_KEYS     = { "cow", "pig", "sheep", "chicken" };
    private static final double[]      MOB_RATE_DEFAULTS = {  1.5,   1.0,   1.5,     2.0     };
    private static final EntityType[]  PASSIVE_MOBS      = {
        EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN
    };

    private void spawnOasisMobs(RockSpec rock, World world, Random rng) {
        double innerRadius = rock.radius - rockShellThickness;
        double dyC        = rock.centerY - rock.entranceY;
        double footprintR = Math.sqrt(Math.max(0, innerRadius * innerRadius - dyC * dyC));

        for (int t = 0; t < PASSIVE_MOBS.length; t++) {
            int count = RngUtils.poissonSample(rng, oasisMobRates[t]);
            for (int i = 0; i < count; i++) {
                double angle = rng.nextDouble() * 2.0 * Math.PI;
                double r     = footprintR * (0.45 + rng.nextDouble() * 0.30);
                int wx = rock.centerX + (int) (r * Math.cos(angle));
                int wz = rock.centerZ + (int) (r * Math.sin(angle));

                // Compute interior ceiling at this horizontal position, then scan down
                double hd = Math.sqrt((double)(wx - rock.centerX) * (wx - rock.centerX)
                                    + (double)(wz - rock.centerZ) * (wz - rock.centerZ));
                int topY   = rock.centerY + (int) Math.sqrt(Math.max(0, innerRadius * innerRadius - hd * hd));
                int spawnY = rock.entranceY + 1;
                for (int y = topY; y >= rock.entranceY; y--) {
                    if (world.getBlockAt(wx, y, wz).getType() != Material.AIR) {
                        spawnY = y + 1;
                        break;
                    }
                }

                world.spawnEntity(new Location(world, wx + 0.5, spawnY, wz + 0.5), PASSIVE_MOBS[t]);
            }
        }
    }

    private void placeEquipmentCache(RockSpec rock, World world) {
        long rockSeed = cachedSeed ^ ((long) rock.centerX * 0x9E3779B97F4A7C15L) ^ ((long) rock.centerZ * 0x6C62272E07BB0142L);
        Random rng = new Random(rockSeed ^ 0x4341434845L);

        if (rng.nextDouble() >= oasisCacheChance) return;

        double innerRadius = rock.radius - rockShellThickness;
        double dyC         = rock.centerY - rock.entranceY;
        double footprintR  = Math.sqrt(Math.max(0, innerRadius * innerRadius - dyC * dyC));
        double poolR       = footprintR * oasisPoolRadius;

        double angle = rng.nextDouble() * 2.0 * Math.PI;
        double r     = footprintR * (0.50 + rng.nextDouble() * 0.25);
        int wx = rock.centerX + (int) (r * Math.cos(angle));
        int wz = rock.centerZ + (int) (r * Math.sin(angle));

        // Push outside pool/shore if needed
        double horizDist   = Math.sqrt((wx - rock.centerX) * (double) (wx - rock.centerX)
                                     + (wz - rock.centerZ) * (double) (wz - rock.centerZ));
        double pondBn      = rockNoise.eval(wx * oasisFloorNoiseScale + 900.0, wz * oasisFloorNoiseScale + 900.0);
        double effPoolR    = Math.max(0.5, poolR + pondBn * oasisPondBoundaryAmplitude);
        double effSandEdge = effPoolR + oasisClayRadius;
        if (horizDist <= effSandEdge) {
            double pushR = effSandEdge + 2.0;
            wx = rock.centerX + (int) (pushR * Math.cos(angle));
            wz = rock.centerZ + (int) (pushR * Math.sin(angle));
        }

        int topScanY = rock.entranceY + oasisSlopeRise + oasisFloorNoiseAmplitude + 5;
        int floorY   = -1;
        for (int y = topScanY; y >= rock.entranceY; y--) {
            Material m = world.getBlockAt(wx, y, wz).getType();
            if (m == Material.GRASS || m == Material.DIRT || m == Material.STONE) {
                floorY = y;
                break;
            }
        }
        if (floorY < 0) return;

        int chestY = floorY + 1;
        world.getBlockAt(wx, chestY, wz).setType(Material.CHEST);
        BlockState state = world.getBlockAt(wx, chestY, wz).getState();
        if (!(state instanceof Chest)) return;

        Inventory inv = ((Chest) state).getBlockInventory();
        for (ItemStack item : CustomItems.buildCacheContents(rng, oasisCacheCellFillChance, CACHE_ITEM_KEYS, oasisCacheItemThresholds)) {
            inv.addItem(item);
        }
        state.update(true);
    }

    private class OasisPopulator extends BlockPopulator {
        @Override
        public void populate(World world, Random random, Chunk chunk) {
            if (rockField == null || world.getSeed() != cachedSeed) return;
            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();
            for (RockSpec rock : rockField.getRocksNear(chunkX, chunkZ)) {
                if (rock.type == FormationType.OASIS) {
                    placeGlowstoneFlowers(rock, world, chunkX, chunkZ);
                    placeCeilingGlowstone(rock, world, chunkX, chunkZ);
                    placeCeilingVines(rock, world, chunkX, chunkZ);

                    // One-per-rock actions deferred so tile entities and entities are added
                    // after the chunk is fully tracked.
                    if (chunkX == Math.floorDiv(rock.centerX, 16) && chunkZ == Math.floorDiv(rock.centerZ, 16)) {
                        final RockSpec r = rock;
                        final long rockSeed = cachedSeed
                            ^ ((long) rock.centerX * 0x9E3779B97F4A7C15L)
                            ^ ((long) rock.centerZ * 0x6C62272E07BB0142L);
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            placeEquipmentCache(r, world);
                            spawnOasisMobs(r, world, new Random(rockSeed ^ 0x4D4F425350574EL));
                        });
                    }
                }
            }
        }
    }
}