package com.xytronix.forgottentemple;

import static org.junit.jupiter.api.Assertions.*;

import com.hypixel.hytale.server.core.util.BsonUtil;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TempleWorldTemplateTest {
    @TempDir
    Path temporary;

    @Test
    void freshImportCopiesWholeAuthoredTreeAndAdaptsOnlyPersistentWorldSettings() throws Exception {
        Path assets = temporary.resolve("assets");
        Path template = authorTemplate(assets);
        Path universe = temporary.resolve("universe");
        Path world = universe.resolve("worlds/ForgottenTemple");
        Files.createDirectories(universe.resolve("resources"));
        Path flags = universe.resolve("resources/GameFlags.json");
        Files.writeString(flags, "{\"goblin_lab_power\":true}");
        BsonDocument authored = BsonUtil.readDocumentNow(template.resolve("instance.bson"));
        BsonDocument options = new BsonDocument("GameplayConfig", new BsonString("ForgottenTemple_P"))
                .append("ForcedWeather", new BsonString("Weather_User_Choice"))
                .append("IsGameTimePaused", BsonBoolean.FALSE)
                .append("IsSpawningNPC", BsonBoolean.FALSE);

        TempleWorldTemplate.create(assets, world, options, TempleLayout.CHAPTER1);

        assertAuthoredFilesCopied(template, world);
        BsonDocument config = BsonUtil.readDocumentNow(world.resolve("config.json"));
        assertNotNull(config);
        assertNotEquals(authored.get("UUID"), config.get("UUID"));
        assertEquals(authored.get("SpawnProvider"), config.get("SpawnProvider"));
        assertEquals(authored.get("WorldGen"), config.get("WorldGen"));
        assertEquals(authored.get("Seed"), config.get("Seed"));
        assertEquals(authored.get("ClientEffects"), config.get("ClientEffects"));
        assertEquals(authored.get("IsTicking"), config.get("IsTicking"));
        assertEquals(authored.get("IsObjectiveMarkersEnabled"), config.get("IsObjectiveMarkersEnabled"));
        assertFalse(config.getBoolean("DeleteOnUniverseStart").getValue());
        assertFalse(config.getBoolean("DeleteOnRemove").getValue());
        assertTrue(config.getBoolean("IsSavingPlayers").getValue());
        assertTrue(config.getBoolean("IsSavingChunks").getValue());
        assertTrue(config.getBoolean("SaveNewChunks").getValue());
        assertFalse(config.getDocument("Plugin").containsKey("Instance"));
        assertEquals(
                authored.getDocument("Plugin").get("OtherAuthoredPlugin"),
                config.getDocument("Plugin").get("OtherAuthoredPlugin"));
        for (var option : options.entrySet()) {
            assertEquals(option.getValue(), config.get(option.getKey()));
        }
        assertEquals(authored, BsonUtil.readDocumentNow(template.resolve("instance.bson")));
        assertEquals("{\"goblin_lab_power\":true}", Files.readString(flags));
        assertFalse(Files.exists(world.resolve("instance.bson")));
        assertEquals(TempleLayout.CHAPTER1, TempleWorldTemplate.existingLayout(world));
        assertNoStagingWorld(universe);
    }

    @Test
    void mountedZipAssetRootImportsIntoDiskWithoutProviderMismatch() throws Exception {
        Path zip = temporary.resolve("assets.zip");
        Path world = temporary.resolve("universe/worlds/ForgottenTemple");
        try (var filesystem = FileSystems.newFileSystem(URI.create("jar:" + zip.toUri()), Map.of("create", "true"))) {
            Path assetRoot = filesystem.getPath("/");
            Path template = authorTemplate(assetRoot);
            TempleWorldTemplate.create(assetRoot, world, new BsonDocument(), TempleLayout.CHAPTER1);
            assertAuthoredFilesCopied(template, world);
        }
        assertEquals(TempleLayout.CHAPTER1, TempleWorldTemplate.existingLayout(world));
    }

    @Test
    void existingLegacyWorldIsNeverReplacedEvenWhenItsConfigIsIncomplete() throws Exception {
        Path assets = temporary.resolve("assets");
        authorTemplate(assets);
        Path world = temporary.resolve("universe/worlds/ForgottenTemple");
        Files.createDirectories(world.resolve("resources"));
        Path triggerState = world.resolve("resources/TriggerVolumeData.json");
        Path build = world.resolve("player-build.region.bin");
        byte[] savedBuild = {42, 19, 7};
        Files.write(build, savedBuild);
        Files.writeString(triggerState, "{\"Volumes\":{\"saved\":{\"Tags\":{\"Initialized\":true}}}}");
        assertNull(TempleWorldTemplate.existingLayout(world));

        assertThrows(
                FileAlreadyExistsException.class,
                () -> TempleWorldTemplate.create(assets, world, new BsonDocument(), TempleLayout.CHAPTER0));

        assertArrayEquals(savedBuild, Files.readAllBytes(build));
        assertEquals("{\"Volumes\":{\"saved\":{\"Tags\":{\"Initialized\":true}}}}", Files.readString(triggerState));
        assertFalse(Files.exists(world.resolve("config.json")));
        assertNull(TempleWorldTemplate.existingLayout(world));
    }

    @Test
    void missingEssentialTriggerContentNeverPublishesAVoidWorld() throws Exception {
        Path assets = temporary.resolve("assets");
        Path template = authorTemplate(assets);
        Files.delete(template.resolve("resources/TriggerVolumeData.json"));
        Path universe = temporary.resolve("universe");
        Path world = universe.resolve("worlds/ForgottenTemple");

        assertThrows(
                IOException.class,
                () -> TempleWorldTemplate.create(assets, world, new BsonDocument(), TempleLayout.CHAPTER1));

        assertFalse(Files.exists(world));
        assertNoStagingWorld(universe);
    }

    @Test
    void chapter0ImportsTheTempleFromBeforeChapterOneWithoutTriggerVolumes() throws Exception {
        Path assets = temporary.resolve("assets");
        Path template = authorTemplate(assets, TempleLayout.CHAPTER0);
        Files.writeString(template.resolve("resources/TriggerVolumeData.json"), "{}");
        Path world = temporary.resolve("universe/worlds/ForgottenTemple");

        TempleWorldTemplate.create(assets, world, new BsonDocument(), TempleLayout.CHAPTER0);

        assertAuthoredFilesCopied(template, world);
        assertEquals(TempleLayout.CHAPTER0, TempleWorldTemplate.existingLayout(world));
        assertFalse(Files.exists(assets.resolve("Server/Instances/" + TempleLayout.CHAPTER1.templateName())));
    }

    @Test
    void layoutConfigValuesAreCaseInsensitiveAndUnknownValuesAreRejected() {
        assertEquals(TempleLayout.CHAPTER1, TempleLayout.fromConfig("Chapter1"));
        assertEquals(TempleLayout.CHAPTER0, TempleLayout.fromConfig(" chapter0 "));
        assertEquals(TempleLayout.CHAPTER1, TempleLayout.DEFAULT);
        assertNull(TempleLayout.fromConfig("Goblins"));
        assertNull(TempleLayout.fromConfig("Classic"));
        assertNull(TempleLayout.fromConfig(null));
    }

    @Test
    void existingWorldMarkersMapToChapterNames() throws Exception {
        Path world = temporary.resolve("universe/worlds/ForgottenTemple");
        Files.createDirectories(world);
        Files.writeString(world.resolve(".forgotten-temple-template"), "Forgotten_Temple_Goblins\n");
        assertEquals("Chapter1", TempleWorldTemplate.existingLayout(world).configName());
        Files.writeString(world.resolve(".forgotten-temple-template"), "Forgotten_Temple\n");
        assertEquals("Chapter0", TempleWorldTemplate.existingLayout(world).configName());
    }

    @Test
    void failedConfigPublicationRollsBackCopiedFilesAndKeepsOtherSaves() throws Exception {
        Path assets = temporary.resolve("assets");
        Path template = authorTemplate(assets);
        Files.writeString(template.resolve("config.json"), "authored conflict");
        Path universe = temporary.resolve("universe");
        Path world = universe.resolve("worlds/ForgottenTemple");
        Path existing = universe.resolve("worlds/OtherWorld/resources/TriggerVolumeData.json");
        Files.createDirectories(existing.getParent());
        Files.writeString(existing, "existing runtime state");

        assertThrows(
                IOException.class,
                () -> TempleWorldTemplate.create(assets, world, new BsonDocument(), TempleLayout.CHAPTER1));

        assertFalse(Files.exists(world));
        assertEquals("existing runtime state", Files.readString(existing));
        assertNoStagingWorld(universe);
    }

    @Test
    void unsupportedAuthoredFileAbortsCopyInsteadOfLeavingPartialWorld() throws Exception {
        Path assets = temporary.resolve("assets");
        Path template = authorTemplate(assets);
        Files.createSymbolicLink(template.resolve("resources/broken-link"), Path.of("absent"));
        Path universe = temporary.resolve("universe");
        Path world = universe.resolve("worlds/ForgottenTemple");

        assertThrows(
                IOException.class,
                () -> TempleWorldTemplate.create(assets, world, new BsonDocument(), TempleLayout.CHAPTER1));

        assertFalse(Files.exists(world));
        assertNoStagingWorld(universe);
    }

    private Path authorTemplate(Path assets) throws IOException {
        return authorTemplate(assets, TempleLayout.CHAPTER1);
    }

    private Path authorTemplate(Path assets, TempleLayout layout) throws IOException {
        Path template = assets.resolve("Server/Instances/" + layout.templateName());
        Files.createDirectories(template.resolve("chunks"));
        Files.createDirectories(template.resolve("resources/nested"));
        Files.write(template.resolve("chunks/4.4.region.bin"), new byte[] {1, 2, 3});
        Files.write(template.resolve("chunks/-1.-1.region.bin"), new byte[] {4, 5});
        Files.write(template.resolve("chunks/6.4.region.bin"), new byte[] {6, 7});
        Files.writeString(
                template.resolve("resources/TriggerVolumeData.json"),
                "{\"Volumes\":{\"authored\":{\"Tags\":{\"Initialized\":true},\"Effects\":[\"PlaceBlock\"]}}}");
        Files.writeString(template.resolve("resources/InstanceData.json"), "{\"HadPlayer\":false}");
        Files.writeString(template.resolve("resources/nested/native-state.json"), "{\"authored\":true}");
        Files.writeString(template.resolve("resources/Time.json"), "{\"GameTime\":\"0001-01-01T08:00:00Z\"}");
        Files.writeString(template.resolve("instance.bson"), """
                {
                  "Version": 4,
                  "UUID": {"$binary": "x1hEYRHeT1GDR3SDxvmzJQ==", "$type": "04"},
                  "DisplayName": "Authored Temple",
                  "SpawnProvider": {"Id":"Global","SpawnPoint":{"X":5095,"Y":169,"Z":4983.5,"Yaw":90}},
                  "WorldGen": {"Type":"Hytale","Name":"Instance_Forgotten_Temple"},
                  "Seed": 1618917989368,
                  "ClientEffects": {"SunAngleDegrees":130,"SunHeightPercent":33},
                  "IsTicking": false,
                  "IsObjectiveMarkersEnabled": false,
                  "DeleteOnUniverseStart": true,
                  "DeleteOnRemove": true,
                  "IsSavingPlayers": false,
                  "IsSavingChunks": false,
                  "SaveNewChunks": false,
                  "Plugin": {
                    "Instance": {"InstanceKey":"authored-temporary-key","RespawnWhenTargeted":true,
                                 "RemovalConditions":[{"Type":"WorldEmpty","TimeoutSeconds":20}]},
                    "OtherAuthoredPlugin": {"Keep":true}
                  }
                }
                """);
        return template;
    }

    private void assertAuthoredFilesCopied(Path template, Path world) throws IOException {
        try (var files = Files.walk(template)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String relative = template.relativize(file).toString();
                if (!relative.equals("instance.bson")) {
                    assertArrayEquals(Files.readAllBytes(file), Files.readAllBytes(world.resolve(relative)), relative);
                }
            }
        }
    }

    private void assertNoStagingWorld(Path universe) throws IOException {
        if (!Files.exists(universe)) return;
        try (var children = Files.list(universe)) {
            assertFalse(
                    children.anyMatch(path -> path.getFileName().toString().startsWith(".forgotten-temple-import-")));
        }
    }
}
