package com.xytronix.forgottentemple;

import static org.junit.jupiter.api.Assertions.*;

import com.hypixel.hytale.math.vector.Transform;
import com.xytronix.forgottentemple.interaction.TempleReturnPoints;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TempleReturnPointsTest {
    private final UUID player = UUID.randomUUID();
    private final UUID world = UUID.randomUUID();

    @AfterEach
    void cleanup() {
        TempleReturnPoints.clearPlayer(player);
    }

    @Test
    void savedJomlPositionAndRotationAreDefensiveCopies() {
        var transform = new Transform(1, 2, 3, 0.1f, 0.2f, 0.3f);
        TempleReturnPoints.saveReturnPoint(player, world, transform, -1, 2, 3);
        transform.getPosition().x = 99;
        transform.getRotation().y = 99;
        var saved = TempleReturnPoints.getReturnPoint(player);
        assertEquals(world, saved.worldUuid());
        assertEquals(-1, saved.portalX());
        assertEquals(1, saved.transform().getPosition().x);
        assertEquals(0.2f, saved.transform().getRotation().y);
        saved.transform().getPosition().x = 50;
        saved.transform().getRotation().y = 50;
        var fetchedAgain = TempleReturnPoints.getReturnPoint(player);
        assertEquals(1, fetchedAgain.transform().getPosition().x);
        assertEquals(0.2f, fetchedAgain.transform().getRotation().y);
    }

    @Test
    void returnPointIsConsumedOnce() {
        TempleReturnPoints.saveReturnPoint(player, world, new Transform(1, 2, 3), 1, 2, 3);
        assertTrue(TempleReturnPoints.hasReturnPoint(player));
        assertNotNull(TempleReturnPoints.getAndRemoveReturnPoint(player));
        assertNull(TempleReturnPoints.getAndRemoveReturnPoint(player));
        assertFalse(TempleReturnPoints.hasReturnPoint(player));
    }

    @Test
    void disconnectClearsReturnPointAndCooldown() {
        TempleReturnPoints.saveReturnPoint(player, world, new Transform(1, 2, 3), 1, 2, 3);
        TempleReturnPoints.setCooldown(player);
        assertTrue(TempleReturnPoints.isOnCooldown(player));
        assertTrue(TempleReturnPoints.getRemainingCooldown(player) > 0);
        TempleReturnPoints.clearPlayer(player);
        assertFalse(TempleReturnPoints.isOnCooldown(player));
        assertEquals(0, TempleReturnPoints.getRemainingCooldown(player));
        assertNull(TempleReturnPoints.getReturnPoint(player));
    }

    @Test
    void nullPlayerIsIgnoredSafely() {
        TempleReturnPoints.setCooldown(null);
        TempleReturnPoints.clearPlayer(null);
        assertFalse(TempleReturnPoints.isOnCooldown(null));
        assertNull(TempleReturnPoints.getReturnPoint(null));
    }
}
