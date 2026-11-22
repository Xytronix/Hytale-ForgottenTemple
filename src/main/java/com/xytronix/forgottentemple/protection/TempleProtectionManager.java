package com.xytronix.forgottentemple.protection;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.Set;
import java.util.UUID;

public class TempleProtectionManager {

    public static final String BYPASS_PERMISSION = "forgottentemple.bypass";

    private static final String TEMPLE_WORLD_NAME = "ForgottenTemple";

    private static final int LAND_MIN_X = 4881;
    private static final int LAND_MAX_X = 5113;
    private static final int LAND_MIN_Z = 4902;
    private static final int LAND_MAX_Z = 5086;

    private static final Set<String> MEMORY_BLOCK_TYPES = Set.of("Bench_Memories");
    private static final Set<String> TELEPORTER_BLOCK_TYPES = Set.of("GoblinLab_Teleporter");
    private static final Set<String> ELECTRUM_TELEPORTER_BLOCK_TYPES = Set.of("Teleporter");
    private static final Set<String> FRAGMENT_PORTAL_BLOCK_TYPES = Set.of("GoblinLab_Fragment_Portal");
    private static final String TEMPLE_PORTAL_ENTRY = "Forgotten_Temple_Portal_Enter";
    private static final String TEMPLE_PORTAL_EXIT = "Forgotten_Temple_Portal_Exit";

    private volatile boolean protectWorld = true;
    private volatile boolean protectSpawn = true;
    private volatile boolean protectMemory = true;
    private volatile boolean protectTeleporter = true;
    private volatile boolean protectElectrumTeleporter = true;
    private volatile boolean protectFragmentPortal = true;
    private volatile boolean protectEntryPortal = true;
    private volatile boolean protectExitPortal = true;
    private volatile boolean allowBreakEntryPortal = false;
    private volatile boolean allowBreakExitPortal = false;

    public TempleProtectionManager() {}

    public void updateConfig(
            boolean protectWorld,
            boolean protectSpawn,
            boolean protectMemory,
            boolean protectTeleporter,
            boolean protectElectrumTeleporter,
            boolean protectFragmentPortal,
            boolean protectEntryPortal,
            boolean protectExitPortal,
            boolean allowBreakEntryPortal,
            boolean allowBreakExitPortal) {
        this.protectWorld = protectWorld;
        this.protectSpawn = protectSpawn;
        this.protectMemory = protectMemory;
        this.protectTeleporter = protectTeleporter;
        this.protectElectrumTeleporter = protectElectrumTeleporter;
        this.protectFragmentPortal = protectFragmentPortal;
        this.protectEntryPortal = protectEntryPortal;
        this.protectExitPortal = protectExitPortal;
        this.allowBreakEntryPortal = allowBreakEntryPortal;
        this.allowBreakExitPortal = allowBreakExitPortal;
    }

    public boolean isTempleWorld(World world) {
        if (world == null) {
            return false;
        }
        return TEMPLE_WORLD_NAME.equalsIgnoreCase(world.getName());
    }

    public boolean shouldDenyBlockBreak(World world, int x, int y, int z, BlockType blockType) {
        boolean inTempleWorld = isTempleWorld(world);

        if (inTempleWorld && isLoreNote(blockType)) {
            return false;
        }
        String blockId = null;
        if (blockType != null) {
            String baseKey = blockType.getDefaultStateKey();
            blockId = baseKey != null ? baseKey : blockType.getId();
        }
        return shouldProtectBlock(x, y, z, blockId, inTempleWorld);
    }

    private boolean isLoreNote(BlockType blockType) {
        if (blockType == null || blockType.getData() == null) {
            return false;
        }
        int tagIndex = AssetRegistry.getTagIndex("Lore_Note");
        return tagIndex != AssetRegistry.TAG_NOT_FOUND
                && blockType.getData().getExpandedTagIndexes().contains(tagIndex);
    }

    public boolean shouldDenyBlockPlace(World world, int x, int y, int z, UUID playerUuid) {
        boolean inTempleWorld = isTempleWorld(world);

        if (protectWorld && inTempleWorld) {
            return true;
        }

        if (protectSpawn && inTempleWorld && isInProtectedLandArea(x, z)) {
            return true;
        }

        return false;
    }

    private boolean shouldProtectBlock(int x, int y, int z, String blockId, boolean inTempleWorld) {

        if (blockId == null) {
            return shouldProtectGenericBlock(x, z, inTempleWorld);
        }

        if (matchesBlockId(blockId, TEMPLE_PORTAL_ENTRY)) {
            if (allowBreakEntryPortal) {
                return false;
            }
            if (inTempleWorld) {
                return protectEntryPortal;
            }
            return true;
        }

        if (matchesBlockId(blockId, TEMPLE_PORTAL_EXIT)) {
            if (allowBreakExitPortal) {
                return false;
            }
            if (inTempleWorld) {
                return protectExitPortal;
            }
            return true;
        }

        if (inTempleWorld) {
            if (matchesAnyBlockId(blockId, TELEPORTER_BLOCK_TYPES)) {
                return protectTeleporter;
            }
            if (matchesAnyBlockId(blockId, ELECTRUM_TELEPORTER_BLOCK_TYPES)) {
                return protectElectrumTeleporter;
            }
            if (matchesAnyBlockId(blockId, FRAGMENT_PORTAL_BLOCK_TYPES)) {
                return protectFragmentPortal;
            }

            if (matchesAnyBlockId(blockId, MEMORY_BLOCK_TYPES)) {
                return protectMemory;
            }
        }

        return shouldProtectGenericBlock(x, z, inTempleWorld);
    }

    private boolean shouldProtectGenericBlock(int x, int z, boolean inTempleWorld) {
        if (!inTempleWorld) {
            return false;
        }

        if (protectSpawn && isInProtectedLandArea(x, z)) {
            return true;
        }

        if (protectWorld) {
            return true;
        }

        return false;
    }

    private boolean matchesBlockId(String actualId, String expectedId) {
        if (actualId == null || expectedId == null) {
            return false;
        }
        if (actualId.equals(expectedId)) {
            return true;
        }
        if (actualId.endsWith(":" + expectedId)) {
            return true;
        }
        if (actualId.endsWith("/" + expectedId)) {
            return true;
        }
        String actualBaseName = actualId;
        int colonIndex = actualId.lastIndexOf(':');
        int slashIndex = actualId.lastIndexOf('/');
        int separatorIndex = Math.max(colonIndex, slashIndex);
        if (separatorIndex >= 0 && separatorIndex < actualId.length() - 1) {
            actualBaseName = actualId.substring(separatorIndex + 1);
        }
        return actualBaseName.equalsIgnoreCase(expectedId);
    }

    private boolean matchesAnyBlockId(String actualId, Set<String> expectedIds) {
        if (actualId == null || expectedIds == null || expectedIds.isEmpty()) {
            return false;
        }
        for (String expectedId : expectedIds) {
            if (matchesBlockId(actualId, expectedId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInProtectedLandArea(int x, int z) {
        return x >= LAND_MIN_X && x <= LAND_MAX_X && z >= LAND_MIN_Z && z <= LAND_MAX_Z;
    }

    public boolean isInProtectedLandArea(double x, double z) {
        return x >= LAND_MIN_X && x <= LAND_MAX_X && z >= LAND_MIN_Z && z <= LAND_MAX_Z;
    }

    public boolean isProtectWorld() {
        return protectWorld;
    }

    public boolean isProtectSpawn() {
        return protectSpawn;
    }
}
