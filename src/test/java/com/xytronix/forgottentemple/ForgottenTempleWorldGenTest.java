package com.xytronix.forgottentemple;

import static org.junit.jupiter.api.Assertions.*;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.worldgen.GeneratedBlockChunk;
import com.hypixel.hytale.server.core.work.WorkPool;
import com.xytronix.forgottentemple.worldgen.ForgottenTempleWorldGen;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ForgottenTempleWorldGenTest {
    private final ForgottenTempleWorldGen generator = new ForgottenTempleWorldGen();
    private TestBlockAssets blockAssets;
    private int previousEnvironment;
    private int previousSpawnBlock;

    private static Field cachedId(String name) throws Exception {
        var field = ForgottenTempleWorldGen.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @BeforeEach
    void useDeterministicAssetIds() throws Exception {
        blockAssets = new TestBlockAssets();
        previousEnvironment = cachedId("environmentId").getInt(null);
        previousSpawnBlock = cachedId("spawnBlockId").getInt(null);
        cachedId("environmentId").setInt(null, 1);
        cachedId("spawnBlockId").setInt(null, 2);
    }

    @AfterEach
    void restoreAssetIds() throws Exception {
        if (blockAssets != null) blockAssets.close();
        cachedId("environmentId").setInt(null, previousEnvironment);
        cachedId("spawnBlockId").setInt(null, previousSpawnBlock);
    }

    private GeneratedBlockChunk generate(int x, int z) throws Exception {
        var pool = new WorkPool(1);
        try {
            return pool.getChunksBucket()
                    .newScope("ForgottenTempleWorldGenTest")
                    .submit(generator.generate(42, ChunkUtil.indexChunk(x, z), x, z))
                    .get(5, TimeUnit.SECONDS)
                    .getBlockChunk();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void tintAndEnvironmentCoverEveryColumnOfARealChunk() throws Exception {
        var chunk = generate(-2, -3);
        for (int x = 0; x < ChunkUtil.SIZE; x++) {
            for (int z = 0; z < ChunkUtil.SIZE; z++) {
                assertEquals(0xFF5b9e28, chunk.getTint(x, z), "tint at " + x + "," + z);
                assertEquals(1, chunk.getEnvironment(x, 169, z), "environment at " + x + "," + z);
            }
        }
    }

    @Test
    void spawnPlatformUsesServerChunkCoordinates() throws Exception {
        int chunkX = Math.floorDiv(5095, ChunkUtil.SIZE);
        int chunkZ = Math.floorDiv(4983, ChunkUtil.SIZE);
        var chunk = generate(chunkX, chunkZ);
        int x = Math.floorMod(5095, ChunkUtil.SIZE);
        int z = Math.floorMod(4983, ChunkUtil.SIZE);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                assertEquals(2, chunk.getBlock(x + dx, 168, z + dz));
            }
        }
        assertEquals(0, chunk.getBlock(x, 169, z));
    }

    @Test
    void spawnPointKeepsOriginalLocationAndFacing() {
        var spawn = generator.getSpawnPoints(42)[0];
        assertEquals(5095, spawn.getPosition().x);
        assertEquals(169, spawn.getPosition().y);
        assertEquals(4983.5, spawn.getPosition().z);
        assertEquals((float) (Math.PI / 2), spawn.getRotation().y);
    }
}
