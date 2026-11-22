package com.xytronix.forgottentemple;

import com.hypixel.hytale.server.core.util.BsonUtil;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import org.bson.BsonBinary;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonString;

final class TempleWorldTemplate {
    private static final String IMPORT_MARKER = ".forgotten-temple-template";

    private TempleWorldTemplate() {}

    static TempleLayout existingLayout(Path worldPath) throws IOException {
        Path marker = worldPath.resolve(IMPORT_MARKER);
        return Files.isRegularFile(marker)
                ? TempleLayout.fromTemplate(Files.readString(marker).strip())
                : null;
    }

    static void create(Path assetRoot, Path worldPath, BsonDocument userOptions, TempleLayout layout)
            throws IOException {
        if (Files.exists(worldPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileAlreadyExistsException(worldPath.toString());
        }
        Path template = assetRoot.resolve("Server/Instances/" + layout.templateName());
        BsonDocument config = BsonUtil.readDocumentNow(template.resolve("instance.bson"));
        if (config == null || !config.containsKey("SpawnProvider") || !config.containsKey("WorldGen")) {
            throw new IOException("Missing authored temple configuration: " + template);
        }
        requireTemplateContent(template, layout.requiresTriggerVolumes());

        config.putAll(userOptions);
        config.put("UUID", new BsonBinary(UUID.randomUUID()));
        config.put("DisplayName", new BsonString("Forgotten Temple"));
        config.put("DeleteOnUniverseStart", BsonBoolean.FALSE);
        config.put("DeleteOnRemove", BsonBoolean.FALSE);
        config.put("IsSavingPlayers", BsonBoolean.TRUE);
        config.put("IsSavingChunks", BsonBoolean.TRUE);
        config.put("SaveNewChunks", BsonBoolean.TRUE);
        config.put("ResourceStorage", new BsonDocument("Type", new BsonString("Hytale")));
        BsonDocument plugins = config.getDocument("Plugin", null);
        if (plugins != null) {
            plugins.remove("Instance");
        }

        Path worldsPath = worldPath.getParent();
        Files.createDirectories(worldsPath);
        Path staging = Files.createTempDirectory(worldsPath.getParent(), ".forgotten-temple-import-");
        try {
            copyTemplate(template, staging);
            Files.writeString(
                    staging.resolve("config.json"), config.toJson(BsonUtil.SETTINGS), StandardOpenOption.CREATE_NEW);
            Files.writeString(
                    staging.resolve(IMPORT_MARKER), layout.templateName() + "\n", StandardOpenOption.CREATE_NEW);
            Files.move(staging, worldPath);
        } catch (IOException | RuntimeException failure) {
            try {
                deleteStaging(staging);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private static void requireTemplateContent(Path template, boolean requiresTriggerVolumes) throws IOException {
        Path chunks = template.resolve("chunks");
        if (!Files.isDirectory(chunks) || !Files.isDirectory(template.resolve("resources"))) {
            throw new IOException("Missing authored temple chunks/resources: " + template);
        }
        boolean hasRegions = false;
        try (var files = Files.walk(chunks)) {
            var iterator = files.iterator();
            while (iterator.hasNext()) {
                Path file = iterator.next();
                if (Files.isRegularFile(file) && file.getFileName().toString().endsWith(".region.bin")) {
                    if (Files.size(file) == 0) {
                        throw new IOException("Empty authored temple region: " + file);
                    }
                    hasRegions = true;
                }
            }
        }
        if (!hasRegions) {
            throw new IOException("Missing authored temple regions: " + template);
        }
        if (requiresTriggerVolumes) {
            BsonDocument triggers = BsonUtil.readDocumentNow(template.resolve("resources/TriggerVolumeData.json"));
            if (triggers == null
                    || triggers.getDocument("Volumes", new BsonDocument()).isEmpty()) {
                throw new IOException("Missing authored temple trigger volumes: " + template);
            }
        }
    }

    private static void copyTemplate(Path template, Path staging) throws IOException {
        Files.walkFileTree(template, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                    throws IOException {
                Files.createDirectories(
                        staging.resolve(template.relativize(directory).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Path relative = template.relativize(file);
                if (relative.toString().equals("instance.bson")) {
                    return FileVisitResult.CONTINUE;
                }
                if (!attributes.isRegularFile()) {
                    throw new IOException("Unsupported authored temple file: " + file);
                }
                Files.copy(file, staging.resolve(relative.toString()));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteStaging(Path staging) throws IOException {
        Files.walkFileTree(staging, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException failure) throws IOException {
                if (failure != null) throw failure;
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
