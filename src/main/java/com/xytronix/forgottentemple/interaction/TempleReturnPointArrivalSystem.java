package com.xytronix.forgottentemple.interaction;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class TempleReturnPointArrivalSystem extends RefSystem<EntityStore> {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), TempleReturnPoint.getComponentType());
    }

    @Override
    public void onEntityAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        TempleReturnPoint point = store.getComponent(ref, TempleReturnPoint.getComponentType());
        if (isHomeArrival(
                point, store.getExternalData().getWorld().getWorldConfig().getUuid())) {
            commandBuffer.tryRemoveComponent(ref, TempleReturnPoint.getComponentType());
        }
    }

    @Override
    public void onEntityRemove(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {}

    static boolean isHomeArrival(TempleReturnPoint point, UUID arrivalWorld) {
        return point != null && point.worldUuid() != null && point.worldUuid().equals(arrivalWorld);
    }
}
