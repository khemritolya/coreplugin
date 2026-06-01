package org.coreplugin.worldgen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class StructurePlacer {

    public static final class BlockDef {
        public final int      dx, dy, dz;
        public final Material material;

        BlockDef(int dx, int dy, int dz, Material material) {
            this.dx = dx; this.dy = dy; this.dz = dz;
            this.material = material;
        }
    }

    public static List<BlockDef> load(File file) throws IOException {
        try (Reader reader = new FileReader(file)) {
            JsonArray arr = new Gson().fromJson(reader, JsonArray.class);
            List<BlockDef> blocks = new ArrayList<>(arr.size());
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                blocks.add(new BlockDef(
                    obj.get("dx").getAsInt(),
                    obj.get("dy").getAsInt(),
                    obj.get("dz").getAsInt(),
                    Material.getMaterial(obj.get("material").getAsString())
                ));
            }
            return blocks;
        }
    }

    // Places the subset of blocks that fall within the given chunk.
    // Structures spanning multiple chunks are handled correctly — each chunk
    // independently places the portion that belongs to it.
    public static void place(ChunkGenerator.ChunkData chunk, int chunkX, int chunkZ,
                              int originX, int originY, int originZ, List<BlockDef> blocks) {
        for (BlockDef b : blocks) {
            int wx = originX + b.dx;
            int wy = originY + b.dy;
            int wz = originZ + b.dz;
            int lx = wx - chunkX * 16;
            int lz = wz - chunkZ * 16;
            if (lx < 0 || lx > 15 || lz < 0 || lz > 15 || wy < 0 || wy > 255) continue;
            chunk.setBlock(lx, wy, lz, b.material);
        }
    }
}