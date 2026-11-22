package com.xytronix.forgottentemple.interaction;

import static org.junit.jupiter.api.Assertions.*;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ComponentRegistry;
import com.hypixel.hytale.component.EmptyResourceStorage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TemplePortalAttemptTest {
    private final UUID player = UUID.randomUUID();
    private ComponentRegistry<EntityStore> registry;
    private Store<EntityStore> store;
    private Ref<EntityStore> source;

    @BeforeEach
    void setup() {
        TempleReturnPoints.enableTeleports();
        registry = new ComponentRegistry<>();
        store = registry.addStore(new EntityStore(null), EmptyResourceStorage.get());
        source = addEntity();
    }

    @AfterEach
    void cleanup() {
        TempleReturnPoints.clearPlayer(player);
        TempleReturnPoints.enableTeleports();
        registry.shutdown();
    }

    @Test
    void unresolvedAttemptBlocksRepeatAdmissionEvenWithoutCooldown() {
        var first = TempleReturnPoints.beginAttempt(player, source);
        assertNotNull(first);
        assertFalse(TempleReturnPoints.isOnCooldown(player));
        assertNull(TempleReturnPoints.beginAttempt(player, source));
        first.finish();
        assertNotNull(TempleReturnPoints.beginAttempt(player, source));
    }

    @Test
    void disconnectCancelsLocalDependentsWithoutCancellingProviderLoad() {
        var attempt = TempleReturnPoints.beginAttempt(player, source);
        CompletableFuture<Transform> provider = new CompletableFuture<>();
        var resolution = provider.thenApply(Transform::clone);
        AtomicInteger dispatches = new AtomicInteger();
        var delivery = resolution.thenAccept(transform -> {
            if (attempt.beginDispatch()) dispatches.incrementAndGet();
        });
        attempt.trackDelivery(resolution, delivery);
        saveReturnPointAndCooldown();
        TempleReturnPoints.clearPlayer(player);
        assertTrue(resolution.isCancelled());
        assertTrue(delivery.isCancelled());
        assertFalse(provider.isCancelled());
        provider.complete(new Transform(1, 2, 3));
        assertEquals(0, dispatches.get());
        assertNull(TempleReturnPoints.getReturnPoint(player));
        assertFalse(TempleReturnPoints.isOnCooldown(player));
    }

    @Test
    void resolvingSourceRemovalPreservesReturnDataAndIgnoresOtherReferences() {
        var attempt = TempleReturnPoints.beginAttempt(player, source);
        saveReturnPointAndCooldown();
        TempleReturnPoints.cancelResolvingAttempt(player, addEntity());
        assertTrue(attempt.isCurrent());
        TempleReturnPoints.cancelResolvingAttempt(player, source);
        assertFalse(attempt.isCurrent());
        assertFalse(attempt.beginDispatch());
        assertNotNull(TempleReturnPoints.getReturnPoint(player));
        assertTrue(TempleReturnPoints.isOnCooldown(player));
    }

    @Test
    void ownTransferDoesNotInvalidateDispatchedAttempt() {
        var attempt = TempleReturnPoints.beginAttempt(player, source);
        assertTrue(attempt.beginDispatch());
        assertFalse(attempt.beginDispatch());
        TempleReturnPoints.cancelResolvingAttempt(player, source);
        assertTrue(attempt.isCurrent());
        attempt.finish();
        assertFalse(attempt.isCurrent());
    }

    @Test
    void providerFailureReleasesGuardButKeepsAdmissionCooldownAndReturnPoint() {
        var attempt = TempleReturnPoints.beginAttempt(player, source);
        CompletableFuture<Transform> provider = new CompletableFuture<>();
        var resolution = provider.thenApply(Transform::clone);
        AtomicInteger dispatches = new AtomicInteger();
        var delivery = resolution.thenAccept(transform -> dispatches.incrementAndGet());
        attempt.trackDelivery(resolution, delivery);
        saveReturnPointAndCooldown();
        provider.completeExceptionally(new IllegalStateException("provider unavailable"));
        assertTrue(delivery.isCompletedExceptionally());
        assertEquals(0, dispatches.get());
        assertFalse(attempt.isCurrent());
        assertNotNull(TempleReturnPoints.getReturnPoint(player));
        assertTrue(TempleReturnPoints.isOnCooldown(player));
        assertNotNull(TempleReturnPoints.beginAttempt(player, source));
    }

    @Test
    void rejectedSourceExecutorReleasesGuardWithoutDispatch() {
        var attempt = TempleReturnPoints.beginAttempt(player, source);
        CompletableFuture<Transform> resolution = new CompletableFuture<>();
        AtomicInteger dispatches = new AtomicInteger();
        var delivery = resolution.thenAcceptAsync(transform -> dispatches.incrementAndGet(), command -> {
            throw new RejectedExecutionException("source world stopped");
        });
        attempt.trackDelivery(resolution, delivery);
        resolution.complete(new Transform(1, 2, 3));
        assertTrue(delivery.isCompletedExceptionally());
        assertEquals(0, dispatches.get());
        assertNotNull(TempleReturnPoints.beginAttempt(player, source));
    }

    @Test
    void lateOldCompletionCannotClearReconnectedAttempt() throws Exception {
        var old = TempleReturnPoints.beginAttempt(player, source);
        CompletableFuture<Void> delivery = new CompletableFuture<>();
        old.trackDelivery(new CompletableFuture<>(), delivery);
        Thread finisher = null;
        try {
            TempleReturnPoints.PortalAttempt reconnected;
            synchronized (old) {
                finisher = Thread.ofPlatform().start(() -> delivery.complete(null));
                delivery.get(5, TimeUnit.SECONDS);
                TempleReturnPoints.cancelResolvingAttempt(player, source);
                reconnected = TempleReturnPoints.beginAttempt(player, addEntity());
                assertNotNull(reconnected);
            }
            finisher.join(5000);
            assertFalse(finisher.isAlive());
            assertTrue(reconnected.isCurrent());
            assertNull(TempleReturnPoints.beginAttempt(player, source));
        } finally {
            if (finisher != null) finisher.join(5000);
        }
    }

    @Test
    void shutdownCancelsResolutionAndRejectsNewRequests() {
        var attempt = TempleReturnPoints.beginAttempt(player, source);
        CompletableFuture<Transform> provider = new CompletableFuture<>();
        var resolution = provider.thenApply(Transform::clone);
        var delivery = resolution.thenAccept(transform -> fail("shutdown request dispatched"));
        attempt.trackDelivery(resolution, delivery);
        TempleReturnPoints.disableTeleports();
        assertTrue(resolution.isCancelled());
        assertTrue(delivery.isCancelled());
        assertFalse(provider.isCancelled());
        assertFalse(attempt.isCurrent());
        assertNull(TempleReturnPoints.beginAttempt(player, source));
    }

    private Ref<EntityStore> addEntity() {
        var ref = store.addEntity(Archetype.of(registry.getNonSerializedComponentType()), AddReason.SPAWN);
        assertNotNull(ref);
        assertTrue(ref.isValid());
        return ref;
    }

    private void saveReturnPointAndCooldown() {
        TempleReturnPoints.saveReturnPoint(player, UUID.randomUUID(), new Transform(1, 2, 3), 1, 2, 3);
        TempleReturnPoints.setCooldown(player);
    }
}
