package com.xytronix.forgottentemple.worldgen;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.universe.world.worldgen.GeneratedBlockChunk;
import com.hypixel.hytale.server.core.universe.world.worldgen.GeneratedChunk;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.core.universe.world.worldgen.WorldGenTimingsCollector;
import com.hypixel.hytale.server.core.work.WorkItem;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ForgottenTempleWorldGen implements IWorldGen {

    private static final int SPAWN_X = 5095;
    private static final int SPAWN_Y = 169;
    private static final int SPAWN_Z = 4983;

    private static final String SPAWN_BLOCK_KEY = "Rock_Bedrock";

    private static final String ENVIRONMENT_KEY = "Env_Forgotten_Temple_Exterior";

    private static final int TEMPLE_TINT_COLOR = 0xFF5b9e28;

    private static volatile int environmentId = -1;
    private static volatile int spawnBlockId = -1;

    public ForgottenTempleWorldGen() {}

    @Override
    @Nonnull
    public WorkItem<GeneratedChunk> generate(int seed, long index, int chunkX, int chunkZ) {
        return WorkItem.completed(generateChunk(index, chunkX, chunkZ));
    }

    private GeneratedChunk generateChunk(long index, int chunkX, int chunkZ) {
        GeneratedChunk chunk = new GeneratedChunk();
        GeneratedBlockChunk blockChunk = chunk.getBlockChunk();
        blockChunk.setCoordinates(index, chunkX, chunkZ);

        ensureIdsResolved();

        for (int localX = 0; localX < ChunkUtil.SIZE; localX++) {
            for (int localZ = 0; localZ < ChunkUtil.SIZE; localZ++) {
                blockChunk.setTint(localX, localZ, TEMPLE_TINT_COLOR);
                blockChunk.setEnvironmentColumn(localX, localZ, environmentId);
            }
        }

        int chunkMinX = chunkX * ChunkUtil.SIZE;
        int chunkMinZ = chunkZ * ChunkUtil.SIZE;

        if (SPAWN_X >= chunkMinX
                && SPAWN_X < chunkMinX + ChunkUtil.SIZE
                && SPAWN_Z >= chunkMinZ
                && SPAWN_Z < chunkMinZ + ChunkUtil.SIZE) {

            int localSpawnX = SPAWN_X - chunkMinX;
            int localSpawnZ = SPAWN_Z - chunkMinZ;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int lx = localSpawnX + dx;
                    int lz = localSpawnZ + dz;
                    if (lx >= 0 && lx < ChunkUtil.SIZE && lz >= 0 && lz < ChunkUtil.SIZE) {
                        blockChunk.setBlock(lx, SPAWN_Y - 1, lz, spawnBlockId, 0, 0);
                    }
                }
            }
        }

        return chunk;
    }

    private void ensureIdsResolved() {
        if (environmentId < 0) {
            synchronized (ForgottenTempleWorldGen.class) {
                if (environmentId < 0) {
                    environmentId = Environment.getAssetMap().getIndex(ENVIRONMENT_KEY);
                    if (environmentId < 0) {
                        environmentId = Environment.getAssetMap().getIndex("Env_Default_Void");
                    }

                    spawnBlockId = BlockType.getBlockIdOrUnknown(
                            SPAWN_BLOCK_KEY, "Failed to find block key {0}, using fallback for spawn platform");
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    @Nonnull
    public Transform[] getSpawnPoints(int count) {
        return new Transform[] {new Transform(SPAWN_X, SPAWN_Y, SPAWN_Z + 0.5, 0, (float) (Math.PI / 2), 0)};
    }

    @Override
    @Nullable
    public WorldGenTimingsCollector getTimings() {
        return null;
    }
}
