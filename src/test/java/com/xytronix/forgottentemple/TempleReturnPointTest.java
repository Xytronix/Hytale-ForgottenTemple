package com.xytronix.forgottentemple;

import static org.junit.jupiter.api.Assertions.*;

import com.hypixel.hytale.codec.EmptyExtraInfo;
import com.hypixel.hytale.math.vector.Transform;
import com.xytronix.forgottentemple.interaction.TempleReturnPoint;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TempleReturnPointTest {
    @Test
    void persistedReturnPointSurvivesSaveAndLoad() {
        var world = UUID.randomUUID();
        var saved = new TempleReturnPoint(world, new Transform(1.5, 64, -2.25, 0.1f, 1.2f, 0f), -3, 64, 7);
        var loaded = TempleReturnPoint.CODEC.decode(
                TempleReturnPoint.CODEC.encode(saved, EmptyExtraInfo.EMPTY), EmptyExtraInfo.EMPTY);
        assertEquals(world, loaded.worldUuid());
        assertEquals(-3, loaded.portalX());
        assertEquals(64, loaded.portalY());
        assertEquals(7, loaded.portalZ());
        assertEquals(-2.25, loaded.transform().getPosition().z);
        assertEquals(1.2f, loaded.transform().getRotation().y, 1e-6f);
    }

    @Test
    void transformIsCopiedInAndOut() {
        var source = new Transform(1, 2, 3);
        var point = new TempleReturnPoint(UUID.randomUUID(), source, 1, 2, 3);
        source.getPosition().x = 99;
        point.transform().getPosition().x = 50;
        assertEquals(1, point.transform().getPosition().x);
        assertEquals(1, point.clone().transform().getPosition().x);
    }
}
