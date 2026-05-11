package org.coreplugin.worldgen;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
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
    private final int    oasisMinTrees;
    private final int    oasisMaxTrees;
    private final double oasisTreeSpread;
    private final int    oasisSlopeRise;
    private final int    oasisDirtLayers;
    private final double oasisFloorNoiseScale;
    private final int    oasisFloorNoiseAmplitude;
    private final int    oasisPondBoundaryAmplitude;
    private final double oasisClayRadius;
    private final double   oasisDecorationDensity;
    private final double[] oasisDecorationThresholds;

    private static final String[] DECORATION_KEYS = {
        "tall-grass", "fern", "dandelion", "poppy",
        "blue-orchid", "allium", "azure-bluet", "oxeye-daisy"
    };
    private static final double[] DECORATION_DEFAULTS = {
        0.40, 0.10, 0.12, 0.10, 0.08, 0.08, 0.07, 0.05
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
    };

    // Ore cave interior
    private final double[] oreDensities;

    // Runtime
    private FBMNoise     noise;
    private RockField    rockField;
    private SimplexNoise rockNoise;
    private long         cachedSeed = Long.MIN_VALUE;

    public DesertWorldGenerator(JavaPlugin plugin) {
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
        oasisMinTrees              = oasisCfg.getInt("min-trees",                 2);
        oasisMaxTrees              = oasisCfg.getInt("max-trees",                 3);
        oasisTreeSpread            = oasisCfg.getDouble("tree-spread",            0.75);
        oasisSlopeRise             = oasisCfg.getInt("slope-rise",               10);
        oasisDirtLayers            = oasisCfg.getInt("dirt-layers",              1);
        oasisFloorNoiseScale          = oasisCfg.getDouble("floor-noise-scale",       0.15);
        oasisFloorNoiseAmplitude      = oasisCfg.getInt("floor-noise-amplitude",     2);
        oasisPondBoundaryAmplitude    = oasisCfg.getInt("pond-boundary-amplitude",   4);
        oasisClayRadius = oasisCfg.getDouble("clay-radius",              1.0);
        oasisDecorationDensity = oasisCfg.getDouble("decoration-density", 0.25);
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

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return Collections.singletonList(new OasisPopulator());
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        if (world.getSeed() != cachedSeed) {
            cachedSeed = world.getSeed();
            noise     = new FBMNoise(cachedSeed, octaves, frequency, lacunarity, persistence, warpStrength);
            rockField = new RockField(cachedSeed, rockCellSize, rockRadius, rockSpawnChance, rockEmbedFactor,
                                      rockOasisChance, rockOreCaveChance, rockOreWeights,
                                      noise, minHeight, maxHeight);
            rockNoise = new SimplexNoise(cachedSeed + 1);
        }

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

    private void placeShell(RockSpec rock, ChunkData chunk, int chunkX, int chunkZ, World world) {
        int totalMargin = (int) Math.ceil(rockDeformStrength);

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
                        chunk.setBlock(lx, y, lz, Material.HARD_CLAY);
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

                if (horizDist >= innerRadius) continue;

                // Per-column noisy pool radius and shore edge
                double pondBn      = rockNoise.eval(wx * oasisFloorNoiseScale + 900.0, wz * oasisFloorNoiseScale + 900.0);
                double effPoolR    = Math.max(1.0, poolR + pondBn * oasisPondBoundaryAmplitude);
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
                    double localSlopeRange = Math.max(1.0, footprintR - effSandEdge);
                    double tSlope = Math.min(1.0, (horizDist - effSandEdge) / localSlopeRange);
                    double fn = rockNoise.eval(wx * oasisFloorNoiseScale + 500.0, wz * oasisFloorNoiseScale + 500.0);
                    int localFloorY = Math.max(1, floorY + (int)(tSlope * tSlope * oasisSlopeRise) + (int)(fn * oasisFloorNoiseAmplitude));

                    // Stone fill between entrance level and dirt layer
                    for (int fy = floorY; fy < localFloorY - oasisDirtLayers; fy++) {
                        chunk.setBlock(lx, fy, lz, Material.STONE);
                    }
                    // Dirt layer (may extend below floorY when slope is shallow)
                    for (int fy = Math.max(1, localFloorY - oasisDirtLayers); fy < localFloorY; fy++) {
                        chunk.setBlock(lx, fy, lz, Material.DIRT);
                    }
                    chunk.setBlock(lx, localFloorY, lz, Material.GRASS);

                    // Per-block seeded decorations (independent of chunk bounds)
                    long bSeed = rockSeed ^ ((long) wx * 0xB5EE8E3FL) ^ ((long) wz * 0xC7537B51L);
                    Random blockRng = new Random(bSeed);
                    if (blockRng.nextDouble() < oasisDecorationDensity) {
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

    private void fillOreCave(RockSpec rock, ChunkData chunk, int chunkX, int chunkZ) {
        long rockSeed = cachedSeed ^ ((long) rock.centerX * 0x9E3779B97F4A7C15L) ^ ((long) rock.centerZ * 0x6C62272E07BB0142L);
        double innerRadius = rock.radius - rockShellThickness;
        double density     = oreDensity(rock.oreType);
        int ir = (int) innerRadius;

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

                    if (dist >= innerRadius) continue;

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

    private void placeTrees(RockSpec rock, World world, int chunkX, int chunkZ) {
        long rockSeed = cachedSeed ^ ((long) rock.centerX * 0x9E3779B97F4A7C15L) ^ ((long) rock.centerZ * 0x6C62272E07BB0142L);
        Random rng = new Random(rockSeed);

        double innerRadius = rock.radius - rockShellThickness;
        int    floorY      = rock.entranceY;

        double dyC        = rock.centerY - rock.entranceY;
        double footprintR = Math.sqrt(Math.max(0, innerRadius * innerRadius - dyC * dyC));
        double poolR      = footprintR * oasisPoolRadius;

        double treeInnerR = poolR + 2.0;
        double treeOuterR = footprintR * oasisTreeSpread;
        if (treeOuterR <= treeInnerR) treeOuterR = treeInnerR + 2.0;

        int treeCount = oasisMinTrees + (oasisMaxTrees > oasisMinTrees
                        ? rng.nextInt(oasisMaxTrees - oasisMinTrees + 1) : 0);
        for (int i = 0; i < treeCount; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double dist  = treeInnerR + rng.nextDouble() * (treeOuterR - treeInnerR);
            int tx = rock.centerX + (int) Math.round(Math.cos(angle) * dist);
            int tz = rock.centerZ + (int) Math.round(Math.sin(angle) * dist);

            if (tx < chunkX * 16 || tx > chunkX * 16 + 15) continue;
            if (tz < chunkZ * 16 || tz > chunkZ * 16 + 15) continue;

            double horiz = Math.sqrt((tx - rock.centerX) * (tx - rock.centerX)
                                   + (tz - rock.centerZ) * (tz - rock.centerZ));
            if (horiz >= footprintR) continue;

            double treeBn          = rockNoise.eval(tx * oasisFloorNoiseScale + 900.0, tz * oasisFloorNoiseScale + 900.0);
            double treeEffSandEdge = Math.max(1.0, poolR + treeBn * oasisPondBoundaryAmplitude) + oasisClayRadius;
            if (horiz < treeEffSandEdge) continue;
            double treeSlopeRange  = Math.max(1.0, footprintR - treeEffSandEdge);
            double tSlopeTree      = Math.min(1.0, Math.max(0, (horiz - treeEffSandEdge) / treeSlopeRange));
            double treeFn          = rockNoise.eval(tx * oasisFloorNoiseScale + 500.0, tz * oasisFloorNoiseScale + 500.0);
            int treeFloorY = Math.max(1, floorY + (int)(tSlopeTree * tSlopeTree * oasisSlopeRise)
                                                 + (int)(treeFn * oasisFloorNoiseAmplitude));

            int ceilY = rock.centerY + (int) Math.sqrt(Math.max(0, innerRadius * innerRadius - horiz * horiz));
            if (treeFloorY + 11 > ceilY) continue;

            world.generateTree(new Location(world, tx, treeFloorY + 1, tz), TreeType.SMALL_JUNGLE);
        }
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
                    if (world.getBlockAt(wx, y + 1, wz).getType() == Material.HARD_CLAY) {
                        world.getBlockAt(wx, y, wz).setType(Material.GLOWSTONE);
                    }
                    break;
                }
            }
        }
    }

    private class OasisPopulator extends BlockPopulator {
        @Override
        public void populate(World world, Random random, Chunk chunk) {
            if (rockField == null || world.getSeed() != cachedSeed) return;
            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();
            for (RockSpec rock : rockField.getRocksNear(chunkX, chunkZ)) {
                if (rock.type == FormationType.OASIS) {
                    placeTrees(rock, world, chunkX, chunkZ);
                    placeGlowstoneFlowers(rock, world, chunkX, chunkZ);
                    placeCeilingGlowstone(rock, world, chunkX, chunkZ);
                }
            }
        }
    }
}