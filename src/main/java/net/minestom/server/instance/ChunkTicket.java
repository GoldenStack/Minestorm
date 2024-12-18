package net.minestom.server.instance;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minestom.server.coordinate.CoordConversion;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps a set of chunks within an instance permanently loaded.
 */
public final class ChunkTicket {

    private final Instance instance;
    private final LongSet chunks = new LongOpenHashSet();

    public ChunkTicket(@NotNull Instance instance) {
        this.instance = instance;
    }

    /**
     * Adds a chunk to this ticket. This will load it if not loaded already.
     *
     * @param chunkX the x index of the chunk
     * @param chunkZ the z index of the chunk
     * @return whether or not the chunk was added (false indicates it was already in this ticket)
     */
    public boolean addChunk(int chunkX, int chunkZ) {
        if (!chunks.add(CoordConversion.chunkIndex(chunkX, chunkZ))) {
            return false;
        }

        instance.loadChunk(chunkX, chunkZ).thenAccept(Chunk::pushTicket);
        return true;
    }

    /**
     * Removes a chunk from this ticket. The chunk should already be loaded in this instance, since it has to have been
     * added.
     *
     * @param chunkX the x index of the chunk
     * @param chunkZ the z index of the chunk
     * @return whether or not the chunk was removed (false indicates it was already removed from this ticket)
     */
    public boolean removeChunk(int chunkX, int chunkZ) {
        if (!chunks.remove(CoordConversion.chunkIndex(chunkX, chunkZ))) {
            return false;
        }

        Chunk chunk = instance.getChunk(chunkX, chunkZ);
        if (chunk != null) {
            chunk.popTicket();
        }

        return true;
    }

    /**
     * Clears all chunks from this ticket, removing their handle on each chunk.
     */
    public void clear() {
        for (long index : this.chunks) {
            int x = CoordConversion.chunkIndexGetX(index);
            int z = CoordConversion.chunkIndexGetZ(index);

            Chunk chunk = instance.getChunk(x, z);
            if (chunk != null) {
                chunk.popTicket();
            }
        }

        this.chunks.clear();
    }

}
