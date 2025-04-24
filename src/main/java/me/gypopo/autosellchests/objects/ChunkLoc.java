package me.gypopo.autosellchests.objects;

import org.bukkit.Chunk;

public class ChunkLoc {

    public final String world;
    public final int x1;
    public final int z1;

    public ChunkLoc(Chunk chunk) {
        this.world = chunk.getWorld().getName();
        this.x1 = chunk.getX();
        this.z1 = chunk.getZ();
    }

    public boolean contains(ChestLocation loc) {
        return this.world.equals(loc.getLeftLocation().world) && ((this.x1 == loc.getLeftLocation().x >> 4 && this.z1 == loc.getLeftLocation().z >> 4) ||
                (loc.isDoubleChest() && this.x1 == loc.getRightLocation().x >> 4 && this.z1 == loc.getRightLocation().z >> 4));
    }
}
