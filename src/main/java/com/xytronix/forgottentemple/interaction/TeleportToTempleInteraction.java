package com.xytronix.forgottentemple.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.PendingTeleport;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.xytronix.forgottentemple.ForgottenTempleMod;
import com.xytronix.forgottentemple.protection.TempleProtectionManager;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

public class TeleportToTempleInteraction extends SimpleInstantInteraction {

    public static final String WORLD_NAME = "ForgottenTemple";

    @SuppressWarnings("unchecked")
    public static final BuilderCodec<TeleportToTempleInteraction> CODEC = ((BuilderCodec.Builder<
                            TeleportToTempleInteraction>)
                    BuilderCodec.builder(
                            TeleportToTempleInteraction.class,
                            TeleportToTempleInteraction::new,
                            SimpleInstantInteraction.CODEC))
            .build();

    public TeleportToTempleInteraction() {}

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

        ForgottenTempleMod mod = ForgottenTempleMod.getInstance();
        if (mod != null && mod.isWorldClosed()) {
            Player player = context.getCommandBuffer().getComponent(entityRef, Player.getComponentType());
            if (player == null || !playerRef.hasPermission(TempleProtectionManager.BYPASS_PERMISSION, false)) {
                context.getState().state = InteractionState.Failed;
                return;
            }
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

        World templeWorld = Universe.get().getWorld(WORLD_NAME);
        if (templeWorld == null) {
            System.out.println("[ForgottenTemple] TeleportToTemple: Temple world not found");
            context.getState().state = InteractionState.Failed;
            return;
        }

        if (isTempleWorld(currentWorldUuid)) {
            context.getState().state = InteractionState.Failed;
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
            TransformComponent transformComponent =
                    context.getCommandBuffer().getComponent(entityRef, TransformComponent.getComponentType());
            if (transformComponent != null) {
                Vector3d position = transformComponent.getPosition();
                Rotation3f rotation = transformComponent.getRotation();
                if (position != null && rotation != null) {
                    int portalX = (int) Math.floor(position.x);
                    int portalY = (int) Math.floor(position.y);
                    int portalZ = (int) Math.floor(position.z);
                    double offsetX = position.x - Math.sin(rotation.y) * 2.5;
                    double offsetZ = position.z - Math.cos(rotation.y) * 2.5;
                    Transform returnTransform =
                            new Transform(offsetX, position.y, offsetZ, rotation.x, rotation.y, rotation.z);
                    TempleReturnPoints.saveReturnPoint(
                            playerUuid, currentWorldUuid, returnTransform, portalX, portalY, portalZ);
                    context.getCommandBuffer()
                            .putComponent(
                                    entityRef,
                                    TempleReturnPoint.getComponentType(),
                                    new TempleReturnPoint(
                                            currentWorldUuid, returnTransform, portalX, portalY, portalZ));
                }
            }
            boolean accepted = attempt.teleportWhenResolved(
                    playerRef, templeWorld, () -> getSpawnTransform(templeWorld, playerUuid), false);
            context.getState().state = accepted ? InteractionState.Finished : InteractionState.Failed;
        }
    }

    private boolean isTempleWorld(UUID worldUuid) {
        if (worldUuid == null) {
            return false;
        }

        World world = Universe.get().getWorld(worldUuid);
        if (world == null) {
            return false;
        }
        return WORLD_NAME.equalsIgnoreCase(world.getName());
    }

    private CompletableFuture<Transform> getSpawnTransform(World world, UUID playerUuid) {
        ISpawnProvider spawnProvider = world.getWorldConfig().getSpawnProvider();
        if (spawnProvider == null) {
            return CompletableFuture.completedFuture(templeSpawn());
        }
        return spawnProvider
                .getSpawnPointAsync(world, playerUuid)
                .thenApply(point -> point != null ? point.clone() : templeSpawn());
    }

    private Transform templeSpawn() {
        return new Transform(5095, 169, 4983.5, 0, (float) (Math.PI / 2), 0);
    }
}
