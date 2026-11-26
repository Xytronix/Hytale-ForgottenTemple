package com.xytronix.forgottentemple;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import com.hypixel.hytale.common.semver.Semver;
import com.hypixel.hytale.common.semver.SemverRange;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CompatibilityTest {
    @Test
    void packagedManifestTargetsHytale080Pre7AndMatchesProjectVersion() throws Exception {
        try (var stream = getClass().getResourceAsStream("/manifest.json")) {
            assertNotNull(stream);
            var manifest = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            assertEquals(">=0.8.0-pre.7", manifest.get("ServerVersion").getAsString());
            var range = SemverRange.fromString(manifest.get("ServerVersion").getAsString());
            assertTrue(range.satisfies(Semver.fromString("0.8.0-pre.7")));
            assertTrue(range.satisfies(Semver.fromString("0.8.0")));
            assertFalse(range.satisfies(Semver.fromString("0.8.0-pre.6")));
            assertFalse(range.satisfies(Semver.fromString("0.7.7")));
            assertEquals(
                    System.getProperty("project.version"),
                    manifest.get("Version").getAsString());
            assertEquals(
                    ForgottenTempleMod.class.getName(), manifest.get("Main").getAsString());
            assertTrue(manifest.get("IncludesAssetPack").getAsBoolean());
        }
    }

    @Test
    void allBundledJsonAssetsRemainValid() throws Exception {
        try (var paths = Files.walk(Path.of("src/main/resources"))) {
            var assets = paths.filter(p -> p.toString().endsWith(".json")).toList();
            assertFalse(assets.isEmpty());
            for (var path : assets) {
                try (var reader = Files.newBufferedReader(path)) {
                    assertTrue(JsonParser.parseReader(reader).isJsonObject(), path.toString());
                }
            }
        }
    }
}
