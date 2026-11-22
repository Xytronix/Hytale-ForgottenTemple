package com.xytronix.forgottentemple.interaction;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.teleport.PendingTeleport;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class ReturnFromTempleInteraction extends SimpleInstantInteraction {

    private static final String TEMPLE_WORLD_NAME = "ForgottenTemple";

    @SuppressWarnings("unchecked")
    public static final BuilderCodec<ReturnFromTempleInteraction> CODEC = ((BuilderCodec.Builder<
                            ReturnFromTempleInteraction>)
                    BuilderCodec.builder(
                            ReturnFromTempleInteraction.class,
                            ReturnFromTempleInteraction::new,
                            SimpleInstantInteraction.CODEC))
            .build();

    public ReturnFromTempleInteraction() {}

    @Override
    protected void firstRun(
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {

        Ref<EntityStore> entityRef = context.getEntity();
        if (entityRef == null || !entityRef.isValid()) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        PlayerRef playerRef = context.getCommandBuffer().getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        UUID playerUuid = playerRef.getUuid();

        if (TempleReturnPoints.isOnCooldown(playerUuid)) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Archetype<EntityStore> archetype = context.getCommandBuffer().getArchetype(entityRef);
        if (archetype != null
                && (archetype.contains(Teleport.getComponentType())
                        || archetype.contains(PendingTeleport.getComponentType()))) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        UUID currentWorldUuid = playerRef.getWorldUuid();
        if (currentWorldUuid == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        if (!isTempleWorld(currentWorldUuid)) {
            exitVanillaInstance(context, entityRef);
            return;
        }

        TempleReturnPoints.PortalAttempt attempt = TempleReturnPoints.beginAttempt(playerUuid, entityRef);
        if (attempt == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        synchronized (attempt) {
            if (!attempt.isCurrent()) {
                context.getState().state = InteractionState.Failed;
                return;
            }
            TempleReturnPoints.setCooldown(playerUuid);
            TempleReturnPoints.ReturnPoint returnPoint = TempleReturnPoints.getReturnPoint(playerUuid);
            if (returnPoint == null) {
                TempleReturnPoint persisted =
                        context.getCommandBuffer().getComponent(entityRef, TempleReturnPoint.getComponentType());
                if (persisted != null) {
                    returnPoint = new TempleReturnPoints.ReturnPoint(
                            persisted.worldUuid(),
                            persisted.transform(),
                            persisted.portalX(),
                            persisted.portalY(),
                            persisted.portalZ());
                }
            }
            World targetWorld = null;
            Transform targetTransform = null;
            if (returnPoint != null) {
                targetWorld = Universe.get().getWorld(returnPoint.worldUuid());
                if (targetWorld != null) {
                    Transform storedTransform = returnPoint.transform();
                    float rotX = storedTransform != null ? storedTransform.getRotation().x : 0;
                    float rotY = storedTransform != null ? storedTransform.getRotation().y : 0;
                    float rotZ = storedTransform != null ? storedTransform.getRotation().z : 0;
                    targetTransform = new Transform(
                            returnPoint.portalX() + 0.5,
                            returnPoint.portalY(),
                            returnPoint.portalZ() + 0.5,
                            rotX,
                            rotY,
                            rotZ);
                }
            }
            if (targetWorld == null || isTempleWorld(targetWorld)) {
                targetWorld = Universe.get().getDefaultWorld();
                targetTransform = null;
            }
            if (targetWorld == null || targetWorld.getName() == null || isTempleWorld(targetWorld)) {
                attempt.finish();
                context.getState().state = InteractionState.Failed;
                return;
            }
            World destination = targetWorld;
            Transform savedTarget = targetTransform;
            boolean accepted = attempt.teleportWhenResolved(
                    playerRef,
                    destination,
                    () -> savedTarget != null
                            ? CompletableFuture.completedFuture(savedTarget)
                            : getSpawnTransform(destination, playerUuid),
                    true);
            context.getState().state = accepted ? InteractionState.Finished : InteractionState.Failed;
        }
    }

    private void exitVanillaInstance(InteractionContext context, Ref<EntityStore> entityRef) {
        try {
            InstancesPlugin.exitInstance(entityRef, context.getCommandBuffer());
            context.getState().state = InteractionState.Finished;
        } catch (IllegalArgumentException notInInstance) {
            context.getState().state = InteractionState.Failed;
        }
    }

    private boolean isTempleWorld(UUID worldUuid) {
        if (worldUuid == null) {
            return false;
        }
        World world = Universe.get().getWorld(worldUuid);
        return isTempleWorld(world);
    }

    private boolean isTempleWorld(World world) {
        if (world == null) {
            return false;
        }
        return TEMPLE_WORLD_NAME.equalsIgnoreCase(world.getName());
    }

    private CompletableFuture<Transform> getSpawnTransform(World world, UUID playerUuid) {
        ISpawnProvider spawnProvider = world.getWorldConfig().getSpawnProvider();
        if (spawnProvider == null) {
            return CompletableFuture.completedFuture(new Transform(0, 100, 0, 0, 0, 0));
        }
        return spawnProvider
                .getSpawnPointAsync(world, playerUuid)
                .thenApply(point -> point != null ? point.clone() : new Transform(0, 100, 0, 0, 0, 0));
    }
}
