package com.xytronix.forgottentemple.interaction;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

public final class TempleReturnPoint implements Component<EntityStore> {

    public static final BuilderCodec<TempleReturnPoint> CODEC = BuilderCodec.builder(
                    TempleReturnPoint.class, TempleReturnPoint::new)
            .<UUID>append(new KeyedCodec<>("World", Codec.UUID_BINARY), (o, v) -> o.worldUuid = v, o -> o.worldUuid)
            .add()
            .<Transform>append(
                    new KeyedCodec<>("Transform", Transform.CODEC), (o, v) -> o.transform = v, o -> o.transform)
            .add()
            .<Integer>append(new KeyedCodec<>("PortalX", Codec.INTEGER), (o, v) -> o.portalX = v, o -> o.portalX)
            .add()
            .<Integer>append(new KeyedCodec<>("PortalY", Codec.INTEGER), (o, v) -> o.portalY = v, o -> o.portalY)
            .add()
            .<Integer>append(new KeyedCodec<>("PortalZ", Codec.INTEGER), (o, v) -> o.portalZ = v, o -> o.portalZ)
            .add()
            .build();

    private static ComponentType<EntityStore, TempleReturnPoint> componentType;

    private UUID worldUuid;
    private Transform transform = new Transform();
    private int portalX;
    private int portalY;
    private int portalZ;

    private TempleReturnPoint() {}

    public TempleReturnPoint(UUID worldUuid, Transform transform, int portalX, int portalY, int portalZ) {
        this.worldUuid = worldUuid;
        this.transform = transform.clone();
        this.portalX = portalX;
        this.portalY = portalY;
        this.portalZ = portalZ;
    }

    public static void register(ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(TempleReturnPoint.class, "ForgottenTempleReturnPoint", CODEC);
    }

    public static ComponentType<EntityStore, TempleReturnPoint> getComponentType() {
        return componentType;
    }

    public UUID worldUuid() {
        return worldUuid;
    }

    public Transform transform() {
        return transform.clone();
    }

    public int portalX() {
        return portalX;
    }

    public int portalY() {
        return portalY;
    }

    public int portalZ() {
        return portalZ;
    }

    @Override
    public TempleReturnPoint clone() {
        return new TempleReturnPoint(worldUuid, transform, portalX, portalY, portalZ);
    }
}
