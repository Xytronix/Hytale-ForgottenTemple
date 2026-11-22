package com.xytronix.forgottentemple.interaction;

import static org.junit.jupiter.api.Assertions.*;

import com.hypixel.hytale.math.vector.Transform;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TempleReturnPointArrivalTest {
    @Test
    void fragmentTripThenRelogKeepsTheReturnPointUntilThePlayerIsHome() {
        UUID home = UUID.randomUUID();
        UUID temple = UUID.randomUUID();
        UUID fragment = UUID.randomUUID();
        var point = new TempleReturnPoint(home, new Transform(1, 2, 3), 1, 2, 3);
        assertFalse(TempleReturnPointArrivalSystem.isHomeArrival(point, temple));
        assertFalse(TempleReturnPointArrivalSystem.isHomeArrival(point, fragment));
        assertFalse(TempleReturnPointArrivalSystem.isHomeArrival(point, temple));
        assertFalse(TempleReturnPointArrivalSystem.isHomeArrival(point, temple));
        assertTrue(TempleReturnPointArrivalSystem.isHomeArrival(point, home));
        assertFalse(TempleReturnPointArrivalSystem.isHomeArrival(null, home));
    }
}
