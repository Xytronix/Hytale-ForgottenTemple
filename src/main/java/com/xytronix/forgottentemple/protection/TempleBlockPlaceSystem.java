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
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TempleBlockPlaceSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    private static final Logger LOGGER = Logger.getLogger(TempleBlockPlaceSystem.class.getName());

    private final TempleProtectionManager protectionManager;
    private final Supplier<Boolean> debugSupplier;

    public TempleBlockPlaceSystem(TempleProtectionManager protectionManager, Supplier<Boolean> debugSupplier) {
        super(PlaceBlockEvent.class);
        this.protectionManager = protectionManager;
        this.debugSupplier = debugSupplier;
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull PlaceBlockEvent event) {

        if (event.isCancelled()) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null) {
            return;
        }

        UUID playerUuid = playerRef.getUuid();

        if (playerRef.hasPermission(TempleProtectionManager.BYPASS_PERMISSION, false)) {
            return;
        }

        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        int x = event.getTargetBlock().x();
        int y = event.getTargetBlock().y();
        int z = event.getTargetBlock().z();

        if (protectionManager.shouldDenyBlockPlace(world, x, y, z, playerUuid)) {
            if (debugSupplier.get()) {
                boolean isTemple = protectionManager.isTempleWorld(world);
                boolean inSpawn = protectionManager.isInProtectedLandArea(x, z);
                LOGGER.log(Level.INFO, "[BlockPlace] DENIED at ({0},{1},{2}) isTemple={3} inSpawn={4}", new Object[] {
                    x, y, z, isTemple, inSpawn
                });
            }
            event.setCancelled(true);
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
