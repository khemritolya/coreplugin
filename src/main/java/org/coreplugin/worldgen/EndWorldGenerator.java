package org.coreplugin.worldgen;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

public class EndWorldGenerator extends ChunkGenerator {

    public static final int STRUCT_X = 0;
    public static final int STRUCT_Y = 64;
    public static final int STRUCT_Z = 0;

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        ChunkData chunk = createChunkData(world);
        if (chunkX == 0 && chunkZ == 0) {
            chunk.setBlock(STRUCT_X, STRUCT_Y, STRUCT_Z, Material.STONE);
        }
        return chunk;
    }
}