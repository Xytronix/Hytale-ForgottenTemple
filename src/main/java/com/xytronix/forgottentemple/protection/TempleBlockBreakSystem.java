package com.xytronix.forgottentemple.protection;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TempleBlockBreakSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private static final Logger LOGGER = Logger.getLogger(TempleBlockBreakSystem.class.getName());

    private final TempleProtectionManager protectionManager;
    private final Supplier<Boolean> debugSupplier;

    public TempleBlockBreakSystem(TempleProtectionManager protectionManager, Supplier<Boolean> debugSupplier) {
        super(BreakBlockEvent.class);
        this.protectionManager = protectionManager;
        this.debugSupplier = debugSupplier;
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull BreakBlockEvent event) {

        if (event.isCancelled()) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        boolean debug = debugSupplier.get();

        if (player == null || playerRef == null) {
            if (debug) {
                LOGGER.log(Level.INFO, "[BlockBreak] No player/playerRef found");
            }
            return;
        }

        if (playerRef.hasPermission(TempleProtectionManager.BYPASS_PERMISSION, false)) {
            if (debug) {
                LOGGER.log(Level.INFO, "[BlockBreak] Player has bypass permission");
            }
            return;
        }

        World world = store.getExternalData().getWorld();
        if (world == null) {
            if (debug) {
                LOGGER.log(Level.INFO, "[BlockBreak] World is null");
            }
            return;
        }

        int x = event.getTargetBlock().x();
        int y = event.getTargetBlock().y();
        int z = event.getTargetBlock().z();

        if (debug) {
            boolean isTemple = protectionManager.isTempleWorld(world);
            boolean inSpawn = protectionManager.isInProtectedLandArea(x, z);
            var blockType = event.getBlockType();
            String blockId = blockType != null ? blockType.getId() : "null";
            LOGGER.log(
                    Level.INFO,
                    "[BlockBreak] world={0} isTemple={1} pos=({2},{3},{4}) blockId={5} inSpawn={6} protectWorld={7} protectSpawn={8}",
                    new Object[] {
                        world.getName(),
                        isTemple,
                        x,
                        y,
                        z,
                        blockId,
                        inSpawn,
                        protectionManager.isProtectWorld(),
                        protectionManager.isProtectSpawn()
                    });
        }

        if (protectionManager.shouldDenyBlockBreak(world, x, y, z, event.getBlockType())) {
            if (debug) {
                LOGGER.log(Level.INFO, "[BlockBreak] DENIED at ({0},{1},{2})", new Object[] {x, y, z});
            }
            event.setCancelled(true);
        } else if (debug) {
            LOGGER.log(Level.INFO, "[BlockBreak] ALLOWED at ({0},{1},{2})", new Object[] {x, y, z});
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }
}
