package com.xytronix.forgottentemple;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.codec.CollectingExtraInfo;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.server.core.asset.type.gameplay.WorldConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GameplayConfigKeysTest {
    private static void assertKnown(BuilderCodec<?> codec, JsonObject section, Path file) throws Exception {
        var extraInfo = new CollectingExtraInfo();
        codec.decodeJson(RawJsonReader.fromJsonString(section.toString()), extraInfo);
        assertTrue(extraInfo.getUnknownKeys().isEmpty(), file + " " + extraInfo.getUnknownKeys());
    }

    @Test
    void everyVariantUsesOnlyKeysTheServerReads() throws Exception {
        try (var paths = Files.list(Path.of("src/main/resources/Server/GameplayConfigs"))) {
            var files = paths.filter(p -> p.toString().endsWith(".json")).toList();
            assertEquals(64, files.size());
            for (var file : files) {
                var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (json.has("World")) {
                    assertKnown(WorldConfig.CODEC, json.getAsJsonObject("World"), file);
                }
            }
        }
    }

    @Test
    void everyVariantThatReplacesTheWorldSectionStillBlocksBeds() throws Exception {
        try (var paths = Files.list(Path.of("src/main/resources/Server/GameplayConfigs"))) {
            for (var file : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
                var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (json.has("World")) {
                    var tags = json.getAsJsonObject("World").getAsJsonArray("BlockedRootInteractionTags");
                    assertNotNull(tags, file.toString());
                    assertTrue(tags.toString().contains("\"Type=Bed\""), file.toString());
                }
            }
        }
    }
}
