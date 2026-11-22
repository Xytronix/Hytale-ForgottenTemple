package com.xytronix.forgottentemple;

import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.AssetUpdateQuery;
import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.event.IEventBus;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

final class TestBlockAssets implements AutoCloseable {
    private final Field field;
    private final Object previous;

    TestBlockAssets() throws ReflectiveOperationException {
        field = BlockType.class.getDeclaredField("ASSET_STORE");
        field.setAccessible(true);
        previous = field.get(null);
        field.set(
                null,
                new Store.Builder()
                        .setCodec(BlockType.CODEC)
                        .setKeyFunction(BlockType::getId)
                        .setReplaceOnRemove(id -> BlockType.UNKNOWN)
                        .build());
    }

    @Override
    public void close() throws IllegalAccessException {
        field.set(null, previous);
    }

    private static final class Store extends AssetStore<String, BlockType, BlockTypeAssetMap<String, BlockType>> {
        Store(Builder builder) {
            super(builder);
        }

        @Override
        protected IEventBus getEventBus() {
            throw new UnsupportedOperationException("This fixture only supports block lookup");
        }

        @Override
        public void addFileMonitor(String packKey, Path path) {
            throw new UnsupportedOperationException("This fixture does not monitor files");
        }

        @Override
        public void removeFileMonitor(Path path) {
            throw new UnsupportedOperationException("This fixture does not monitor files");
        }

        @Override
        protected void handleRemoveOrUpdate(
                Set<String> removed, Map<String, BlockType> updated, AssetUpdateQuery query) {
            throw new UnsupportedOperationException("This fixture is read-only");
        }

        private static final class Builder
                extends AssetStore.Builder<String, BlockType, BlockTypeAssetMap<String, BlockType>, Builder> {
            Builder() {
                super(String.class, BlockType.class, new BlockTypeAssetMap<>(BlockType[]::new, BlockType::getGroup) {
                    @Override
                    public BlockType getAsset(int index) {
                        return switch (index) {
                            case 0 -> BlockType.EMPTY;
                            case 2 -> BlockType.DEBUG_CUBE;
                            default -> null;
                        };
                    }
                });
            }

            @Override
            public Store build() {
                return new Store(this);
            }
        }
    }
}
