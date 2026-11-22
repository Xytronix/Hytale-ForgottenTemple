package com.xytronix.forgottentemple.interaction;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.modules.entity.teleport.PendingTeleport;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TempleReturnPoints {

    private static final Map<UUID, ReturnPoint> returnPoints = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> teleportCooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, PortalAttempt> pendingTeleports = new ConcurrentHashMap<>();
    private static final Logger LOGGER = Logger.getLogger(TempleReturnPoints.class.getName());
    private static volatile boolean teleportsEnabled;

    public static final long TELEPORT_COOLDOWN_MS = 3000L;

    private TempleReturnPoints() {}

    public static boolean isOnCooldown(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        Long cooldownEnd = teleportCooldowns.get(playerUuid);
        if (cooldownEnd == null) {
            return false;
        }
        return System.currentTimeMillis() < cooldownEnd;
    }

    public static void setCooldown(UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }
        teleportCooldowns.put(playerUuid, System.currentTimeMillis() + TELEPORT_COOLDOWN_MS);
    }

    public static long getRemainingCooldown(UUID playerUuid) {
        if (playerUuid == null) {
            return 0;
        }
        Long cooldownEnd = teleportCooldowns.get(playerUuid);
        if (cooldownEnd == null) {
            return 0;
        }
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public static void saveReturnPoint(
            UUID playerUuid, UUID worldUuid, Transform transform, int portalX, int portalY, int portalZ) {
        if (playerUuid == null || worldUuid == null || transform == null) {
            return;
        }
        returnPoints.put(playerUuid, new ReturnPoint(worldUuid, transform.clone(), portalX, portalY, portalZ));
    }

    public static ReturnPoint getReturnPoint(UUID playerUuid) {
        if (playerUuid == null) {
            return null;
        }
        ReturnPoint point = returnPoints.get(playerUuid);
        if (point == null) {
            return null;
        }
        return new ReturnPoint(
                point.worldUuid(), point.transform().clone(), point.portalX(), point.portalY(), point.portalZ());
    }

    public static ReturnPoint getAndRemoveReturnPoint(UUID playerUuid) {
        if (playerUuid == null) {
            return null;
        }
        ReturnPoint point = returnPoints.remove(playerUuid);
        if (point == null) {
            return null;
        }
        return new ReturnPoint(
                point.worldUuid(), point.transform().clone(), point.portalX(), point.portalY(), point.portalZ());
    }

    public static boolean hasReturnPoint(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        return returnPoints.containsKey(playerUuid);
    }

    public static void removeReturnPoint(UUID playerUuid) {
        if (playerUuid != null) {
            returnPoints.remove(playerUuid);
            teleportCooldowns.remove(playerUuid);
        }
    }

    public static void clearPlayer(UUID playerUuid) {
        if (playerUuid != null) {
            PortalAttempt attempt = pendingTeleports.get(playerUuid);
            if (attempt != null) {
                attempt.cancel();
            }
            returnPoints.remove(playerUuid);
            teleportCooldowns.remove(playerUuid);
        }
    }

    public static synchronized void enableTeleports() {
        teleportsEnabled = true;
    }

    public static synchronized void disableTeleports() {
        teleportsEnabled = false;
        for (PortalAttempt attempt : pendingTeleports.values()) {
            attempt.cancel();
        }
    }

    static synchronized PortalAttempt beginAttempt(UUID playerUuid, Ref<EntityStore> sourceRef) {
        if (!teleportsEnabled || pendingTeleports.containsKey(playerUuid)) {
            return null;
        }
        PortalAttempt attempt = new PortalAttempt(playerUuid, sourceRef);
        return pendingTeleports.putIfAbsent(playerUuid, attempt) == null ? attempt : null;
    }

    static void cancelResolvingAttempt(UUID playerUuid, Ref<EntityStore> sourceRef) {
        PortalAttempt attempt = pendingTeleports.get(playerUuid);
        if (attempt != null) {
            attempt.sourceRemoved(sourceRef);
        }
    }

    static final class PortalAttempt {
        private final UUID playerUuid;
        private final Ref<EntityStore> sourceRef;
        private boolean dispatching;
        private boolean cancelled;
        private CompletableFuture<Transform> resolution;
        private CompletableFuture<Void> delivery;

        private PortalAttempt(UUID playerUuid, Ref<EntityStore> sourceRef) {
            this.playerUuid = playerUuid;
            this.sourceRef = sourceRef;
        }

        synchronized boolean isCurrent() {
            return teleportsEnabled && !cancelled && pendingTeleports.get(playerUuid) == this;
        }

        synchronized boolean beginDispatch() {
            if (!isCurrent() || dispatching) return false;
            dispatching = true;
            return true;
        }

        synchronized void sourceRemoved(Ref<EntityStore> removedRef) {
            if (removedRef == sourceRef && !dispatching) {
                cancel();
            }
        }

        synchronized void cancel() {
            cancelled = true;
            pendingTeleports.remove(playerUuid, this);
            if (delivery != null) delivery.cancel(false);
            if (resolution != null) resolution.cancel(false);
        }

        synchronized void finish() {
            pendingTeleports.remove(playerUuid, this);
        }

        synchronized void trackDelivery(CompletableFuture<Transform> resolution, CompletableFuture<Void> delivery) {
            this.resolution = resolution;
            this.delivery = delivery;
            delivery.whenComplete((ignored, failure) -> finish());
            if (!isCurrent()) cancel();
        }

        synchronized boolean teleportWhenResolved(
                PlayerRef playerRef,
                World destination,
                Supplier<CompletableFuture<Transform>> resolver,
                boolean consumeReturnPoint) {
            if (!isCurrent()) return false;
            Store<EntityStore> sourceStore = sourceRef.getStore();
            World sourceWorld = sourceStore.getExternalData().getWorld();
            UUID sourceWorldUuid = sourceWorld.getWorldConfig().getUuid();
            UUID destinationUuid = destination.getWorldConfig().getUuid();
            try {
                CompletableFuture<Transform> resolved = resolver.get();
                CompletableFuture<Void> applied = resolved.thenAcceptAsync(
                        transform -> {
                            synchronized (this) {
                                if (!isCurrent()
                                        || !sourceRef.isValid()
                                        || !sourceWorld.isAlive()
                                        || !destination.isAlive()
                                        || Universe.get().getWorld(sourceWorldUuid) != sourceWorld
                                        || Universe.get().getWorld(destinationUuid) != destination
                                        || playerRef.getReference() != sourceRef
                                        || !sourceWorldUuid.equals(playerRef.getWorldUuid())) {
                                    return;
                                }
                                Store<EntityStore> currentStore = sourceRef.getStore();
                                if (currentStore != sourceStore
                                        || currentStore.getExternalData().getWorld() != sourceWorld
                                        || currentStore.getComponent(sourceRef, PlayerRef.getComponentType())
                                                != playerRef) {
                                    return;
                                }
                                Archetype<EntityStore> archetype = currentStore.getArchetype(sourceRef);
                                if (archetype == null
                                        || archetype.contains(Teleport.getComponentType())
                                        || archetype.contains(PendingTeleport.getComponentType())
                                        || !beginDispatch()) {
                                    return;
                                }
                                if (consumeReturnPoint) {
                                    returnPoints.remove(playerUuid);
                                    currentStore.tryRemoveComponent(sourceRef, TempleReturnPoint.getComponentType());
                                }
                                currentStore.addComponent(
                                        sourceRef,
                                        Teleport.getComponentType(),
                                        Teleport.createForPlayer(destination, transform));
                            }
                        },
                        sourceWorld);
                trackDelivery(resolved, applied);
                applied.whenComplete((ignored, failure) -> {
                    if (failure != null) logFailure(destination, failure);
                });
                return true;
            } catch (RuntimeException failure) {
                finish();
                logFailure(destination, failure);
                return false;
            }
        }

        private void logFailure(World destination, Throwable failure) {
            Throwable cause = failure;
            while (cause instanceof CompletionException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (!(cause instanceof CancellationException)) {
                LOGGER.log(
                        Level.WARNING,
                        "Failed temple portal request for " + playerUuid + " to world " + destination.getName(),
                        cause);
            }
        }
    }

    public record ReturnPoint(UUID worldUuid, Transform transform, int portalX, int portalY, int portalZ) {}
}
