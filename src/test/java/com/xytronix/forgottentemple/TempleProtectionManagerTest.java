package com.xytronix.forgottentemple;

import static org.junit.jupiter.api.Assertions.*;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.xytronix.forgottentemple.protection.TempleProtectionManager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TempleProtectionManagerTest {
    private final TempleProtectionManager temple = new TempleProtectionManager() {
        @Override
        public boolean isTempleWorld(World world) {
            return true;
        }
    };

    @Test
    void loreNoteExceptionUsesNativeExpandedTagsNotBlockNames() {
        assertFalse(denied(new TaggedBlock("AnAuthoredReadableNote", "Lore_Note")));
        assertTrue(denied(new TaggedBlock("Lore_Note_NotActuallyTagged", "Dungeon_Breakable")));
        assertTrue(denied(new BlockType("PlainStone")));
        assertTrue(denied(null));
    }

    @Test
    void loreTagDoesNotDisableGlobalPortalProtectionOutsideTemple() {
        var protection = new TempleProtectionManager();
        var portal = new TaggedBlock("Forgotten_Temple_Portal_Enter", "Lore_Note");
        assertTrue(protection.shouldDenyBlockBreak(null, 5095, 169, 4983, portal));
    }

    @Test
    void teleporterSwitchCoversEveryLabStateWithoutUnlockingOtherLabBlocks() {
        String id = "GoblinLab_Teleporter";
        setProtection(false, true, true);
        assertFalse(denied(new BlockType(id)));
        assertFalse(denied(new BlockType("hytale:" + id)));
        assertFalse(denied(new StateBlock(id, "Active")));
        for (String other : List.of(
                "Teleporter", "Goblin_Painting_Portal", "GoblinLab_Teleporter_Travel", "GoblinLab_Fragment_Portal")) {
            assertTrue(denied(new BlockType(other)), other);
        }
        assertTrue(denied(new BlockType("Bench_Memories")));
        setProtection(true, true, true);
        assertTrue(denied(new BlockType(id)));
        assertTrue(denied(new StateBlock(id, "Active")));
    }

    @Test
    void electrumSwitchCoversOnlyPlacedElectrumTeleporters() {
        setProtection(true, false, true);
        assertFalse(denied(new BlockType("Teleporter")));
        assertFalse(denied(new BlockType("hytale:Teleporter")));
        assertTrue(denied(new StateBlock("GoblinLab_Teleporter", "Active")));
        assertTrue(denied(new StateBlock("GoblinLab_Fragment_Portal", "Fixed")));
        setProtection(true, true, true);
        assertTrue(denied(new BlockType("Teleporter")));
    }

    @Test
    void fragmentSwitchCoversEveryFragmentStateOnItsOwn() {
        for (String state : List.of("Active", "Fixed", "Spawning", "ScientistPortal")) {
            setProtection(true, true, false);
            assertFalse(denied(new StateBlock("GoblinLab_Fragment_Portal", state)), state);
            assertTrue(denied(new BlockType("GoblinLab_Teleporter")), state);
            assertTrue(denied(new BlockType("Teleporter")), state);
            setProtection(true, true, true);
            assertTrue(denied(new StateBlock("GoblinLab_Fragment_Portal", state)), state);
            setProtection(false, false, true);
            assertTrue(denied(new StateBlock("GoblinLab_Fragment_Portal", state)), state);
            assertFalse(denied(new BlockType("GoblinLab_Teleporter")), state);
        }
    }

    @Test
    void teleportersAndFragmentPortalAreProtectedByDefault() {
        var defaults = new TempleProtectionManager() {
            @Override
            public boolean isTempleWorld(World world) {
                return true;
            }
        };
        for (String id : List.of("GoblinLab_Teleporter", "Teleporter", "GoblinLab_Fragment_Portal")) {
            assertTrue(defaults.shouldDenyBlockBreak(null, 4985, 161, 5040, new StateBlock(id, "Active")), id);
        }
    }

    private boolean denied(BlockType block) {
        return temple.shouldDenyBlockBreak(null, 5095, 169, 4983, block);
    }

    private void setProtection(boolean teleporter, boolean electrum, boolean fragment) {
        temple.updateConfig(true, true, true, teleporter, electrum, fragment, true, true, false, false);
    }

    private static final class TaggedBlock extends BlockType {
        TaggedBlock(String id, String family) {
            super(id);
            data = new AssetExtraInfo.Data(BlockType.class, id, null);
            data.putTags(Map.of("Family", new String[] {family}));
        }
    }

    private static final class StateBlock extends BlockType {
        StateBlock(String baseId, String state) {
            super("*" + baseId + "_State_Definitions_" + state);
            data = new AssetExtraInfo.Data(
                    new AssetExtraInfo.Data(BlockType.class, baseId, null), BlockType.class, getId(), null, true);
        }
    }
}
