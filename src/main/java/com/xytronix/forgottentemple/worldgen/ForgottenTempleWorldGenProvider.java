package com.xytronix.forgottentemple.worldgen;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;
import javax.annotation.Nonnull;

public class ForgottenTempleWorldGenProvider implements IWorldGenProvider {

    public static final BuilderCodec<ForgottenTempleWorldGenProvider> CODEC = BuilderCodec.builder(
                    ForgottenTempleWorldGenProvider.class, ForgottenTempleWorldGenProvider::new)
            .build();

    public ForgottenTempleWorldGenProvider() {}

    @Override
    @Nonnull
    public IWorldGen getGenerator() {
        return new ForgottenTempleWorldGen();
    }
}
