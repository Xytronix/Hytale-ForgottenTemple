package com.xytronix.forgottentemple;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.codec.lookup.BuilderCodecMapCodec;
import com.hypixel.hytale.codec.lookup.Priority;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.config.CrashRecoveryConfig;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.events.AllWorldsLoadedEvent;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;
import com.hypixel.hytale.server.npc.components.NPCComponent;
import com.xytronix.forgottentemple.commands.ForgottenTempleCommand;
import com.xytronix.forgottentemple.interaction.ReturnFromTempleInteraction;
import com.xytronix.forgottentemple.interaction.TeleportToTempleInteraction;
import com.xytronix.forgottentemple.interaction.TemplePortalRemovalSystem;
import com.xytronix.forgottentemple.interaction.TempleReturnPoint;
import com.xytronix.forgottentemple.interaction.TempleReturnPointArrivalSystem;
import com.xytronix.forgottentemple.interaction.TempleReturnPoints;
import com.xytronix.forgottentemple.protection.TempleBlockBreakSystem;
import com.xytronix.forgottentemple.protection.TempleBlockDamageSystem;
import com.xytronix.forgottentemple.protection.TempleBlockPlaceSystem;
import com.xytronix.forgottentemple.protection.TempleDamageSystem;
import com.xytronix.forgottentemple.protection.TempleNPCDamageSystem;
import com.xytronix.forgottentemple.protection.TempleProtectionManager;
import com.xytronix.forgottentemple.worldgen.ForgottenTempleWorldGenProvider;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.Level;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonString;

public final class ForgottenTempleMod extends JavaPlugin {

    private static ForgottenTempleMod instance;

    public static ForgottenTempleMod getInstance() {
        return instance;
    }

    private static final String TEMPLE_WORLD_NAME = "ForgottenTemple";
    private static final String TEMPLE_DISPLAY_NAME = "Forgotten Temple";
    private static final String TEMPLE_GENERATOR_TYPE = "ForgottenTemple";

    private static final String INTERACTION_TELEPORT_TO_TEMPLE = "TeleportToTemple";
    private static final String INTERACTION_RETURN_FROM_TEMPLE = "ReturnFromTemple";

    private static final String CONFIG_FILE_NAME = "config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String MERCHANT_KEYWORD = "merchant";
    private static final String KLOP_KEYWORD = "klop";

    private static final String FREEZE_TIME_DAY = "day";
    private static final String FREEZE_TIME_NIGHT = "night";

    private Path configPath;
    private boolean debugMode = false;
    private boolean protectKlopMerchant = true;
    private boolean protectTeleporter = true;
    private boolean protectElectrumTeleporter = true;
    private boolean protectFragmentPortal = true;
    private boolean protectEntryPortal = true;
    private boolean protectExitPortal = true;
    private boolean allowBreakEntryPortal = false;
    private boolean allowBreakExitPortal = false;
    private boolean protectMemory = true;
    private boolean protectSpawn = true;
    private boolean protectWorld = true;
    private boolean disableMobs = true;
    private boolean disablePvP = true;
    private boolean disablePvPAtSpawn = true;
    private boolean disableFallDamage = true;
    private boolean itemLoss = false;
    private boolean npcDamageable = false;
    private boolean godMode = false;
    private boolean freezeNPCs = false;
    private boolean worldClosed = false;
    private String forcedWeather = "Forgotten_Temple";
    private String freezeTime = FREEZE_TIME_NIGHT;
    private TempleLayout templeLayout = TempleLayout.DEFAULT;
    private String templeWorldLayout;
    private static final String LEGACY_LAYOUT = "Legacy";

    private TempleProtectionManager protectionManager;

    public ForgottenTempleMod(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void setup() {
        TempleReturnPoints.enableTeleports();
        getEntityStoreRegistry().registerSystem(new TemplePortalRemovalSystem());
        protectionManager = new TempleProtectionManager();

        registerWorldGenerator();

        registerInteractions();
        TempleReturnPoint.register(getEntityStoreRegistry());
        getEntityStoreRegistry().registerSystem(new TempleReturnPointArrivalSystem());

        registerProtectionSystems();

        loadConfig();

        getCommandRegistry().registerCommand(new ForgottenTempleCommand(this));
        getLogger().at(Level.INFO).log("Registered /ft command");

        getEventRegistry().registerGlobal(StartWorldEvent.class, event -> handleWorldStart(event.getWorld()));
        getEventRegistry().registerGlobal(RemoveWorldEvent.class, this::handleWorldRemove);
        getEventRegistry().registerGlobal(AllWorldsLoadedEvent.class, this::handleAllWorldsLoaded);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::handlePlayerDisconnect);
    }

    private void registerProtectionSystems() {
        java.util.function.Supplier<Boolean> debugSupplier = () -> debugMode;

        getEntityStoreRegistry()
                .registerSystem((ISystem<EntityStore>) new TempleBlockDamageSystem(protectionManager, debugSupplier));
        getLogger().at(Level.INFO).log("Registered TempleBlockDamageSystem");

        getEntityStoreRegistry()
                .registerSystem((ISystem<EntityStore>) new TempleBlockBreakSystem(protectionManager, debugSupplier));
        getLogger().at(Level.INFO).log("Registered TempleBlockBreakSystem");

        getEntityStoreRegistry()
                .registerSystem((ISystem<EntityStore>) new TempleBlockPlaceSystem(protectionManager, debugSupplier));
        getLogger().at(Level.INFO).log("Registered TempleBlockPlaceSystem");

        getEntityStoreRegistry().registerSystem((ISystem<EntityStore>) new TempleDamageSystem(
                protectionManager,
                () -> disablePvP,
                () -> disablePvPAtSpawn,
                () -> disableFallDamage,
                () -> godMode,
                debugSupplier));
        getLogger().at(Level.INFO).log("Registered TempleDamageSystem");

        getEntityStoreRegistry().registerSystem((ISystem<EntityStore>)
                new TempleNPCDamageSystem(protectionManager, () -> npcDamageable, debugSupplier));
        getLogger().at(Level.INFO).log("Registered TempleNPCDamageSystem");
    }

    public void reloadConfig() {
        loadConfig();
        getLogger().at(Level.INFO).log("Configuration reloaded");

        updateProtectionManager();

        World templeWorld = Universe.get().getWorld(TEMPLE_WORLD_NAME);
        if (templeWorld != null) {
            applyTempleWorldConfig(templeWorld);
            getLogger().at(Level.INFO).log("Temple world config updated");
        }
    }

    private void updateProtectionManager() {
        if (protectionManager != null) {
            protectionManager.updateConfig(
                    protectWorld,
                    protectSpawn,
                    protectMemory,
                    protectTeleporter,
                    protectElectrumTeleporter,
                    protectFragmentPortal,
                    protectEntryPortal,
                    protectExitPortal,
                    allowBreakEntryPortal,
                    allowBreakExitPortal);
        }
    }

    private void registerWorldGenerator() {
        BuilderCodecMapCodec<IWorldGenProvider> codec = IWorldGenProvider.CODEC;
        codec.register(
                Priority.DEFAULT,
                TEMPLE_GENERATOR_TYPE,
                ForgottenTempleWorldGenProvider.class,
                ForgottenTempleWorldGenProvider.CODEC);
        getLogger().at(Level.INFO).log("Registered ForgottenTemple world generator type");
    }

    private void registerInteractions() {
        getCodecRegistry(Interaction.CODEC)
                .register(
                        INTERACTION_TELEPORT_TO_TEMPLE,
                        TeleportToTempleInteraction.class,
                        TeleportToTempleInteraction.CODEC)
                .register(
                        INTERACTION_RETURN_FROM_TEMPLE,
                        ReturnFromTempleInteraction.class,
                        ReturnFromTempleInteraction.CODEC);

        getLogger().at(Level.INFO).log("Registered temple portal interaction types");
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("ForgottenTempleMod started");
    }

    @Override
    protected void shutdown() {
        TempleReturnPoints.disableTeleports();
        getLogger().at(Level.INFO).log("ForgottenTempleMod shutting down");
    }

    private void handleAllWorldsLoaded(AllWorldsLoadedEvent event) {
        ensureTempleWorld();
    }

    private void ensureTempleWorld() {
        Universe universe = Universe.get();
        Path worldPath = universe.getPath().resolve("worlds").resolve(TEMPLE_WORLD_NAME);

        World existing = universe.getWorld(TEMPLE_WORLD_NAME);
        if (existing != null) {
            reportExistingLayout(worldPath);
            getLogger().at(Level.INFO).log("Temple world already loaded: %s", TEMPLE_WORLD_NAME);
            return;
        }

        if (universe.isWorldLoadable(TEMPLE_WORLD_NAME)) {
            reportExistingLayout(worldPath);
            getLogger().at(Level.INFO).log("Loading existing temple world: %s", TEMPLE_WORLD_NAME);
            universe.loadWorld(TEMPLE_WORLD_NAME).exceptionally(ex -> {
                getLogger().at(Level.WARNING).log("Failed to load temple world: %s", ex.getMessage());
                return null;
            });
            return;
        }

        getLogger().at(Level.INFO).log("Creating new temple world: %s", TEMPLE_WORLD_NAME);
        createTempleWorld(universe, worldPath);
    }

    private void createTempleWorld(Universe universe, Path worldPath) {
        AssetPack baseAssetPack = AssetModule.get().getBaseAssetPack();
        if (baseAssetPack == null) {
            getLogger()
                    .at(Level.SEVERE)
                    .log("Cannot create temple: base asset pack is unavailable; no world was created");
            return;
        }
        try {
            TempleWorldTemplate.create(baseAssetPack.getRoot(), worldPath, templeCreationOptions(), templeLayout);
            templeWorldLayout = templeLayout.configName();
            getLogger()
                    .at(Level.INFO)
                    .log(
                            "Temple layout in use: %s (new world from %s)",
                            templeLayout.configName(), templeLayout.templateName());
            universe.loadWorld(TEMPLE_WORLD_NAME).exceptionally(ex -> {
                getLogger().at(Level.SEVERE).withCause(ex).log("Failed to load imported temple world");
                return null;
            });
        } catch (IOException | RuntimeException failure) {
            getLogger()
                    .at(Level.SEVERE)
                    .withCause(failure)
                    .log(
                            "Failed to import temple; existing saves are preserved and no incomplete world will be loaded");
        }
    }

    private BsonDocument templeCreationOptions() {
        BsonDocument options = new BsonDocument()
                .append("GameplayConfig", new BsonString(calculateGameplayConfigName()))
                .append("IsPvpEnabled", BsonBoolean.valueOf(!disablePvP))
                .append("IsSpawningNPC", BsonBoolean.valueOf(!disableMobs))
                .append("IsSpawnMarkersEnabled", BsonBoolean.valueOf(!disableMobs))
                .append("IsAllNPCFrozen", BsonBoolean.valueOf(freezeNPCs));
        boolean paused = freezeTime != null && !freezeTime.isEmpty();
        options.put("IsGameTimePaused", BsonBoolean.valueOf(paused));
        if (paused) {
            java.time.LocalTime time = parseTimeString(freezeTime);
            if (time != null) {
                options.put(
                        "GameTime",
                        new BsonString(time.atDate(java.time.LocalDate.of(1, 1, 1))
                                .toInstant(java.time.ZoneOffset.UTC)
                                .toString()));
            }
        }
        if (forcedWeather != null && !forcedWeather.isEmpty()) {
            options.put("ForcedWeather", new BsonString(forcedWeather));
        }
        return options;
    }

    private void reportExistingLayout(Path worldPath) {
        TempleLayout existingLayout = null;
        try {
            existingLayout = TempleWorldTemplate.existingLayout(worldPath);
        } catch (IOException failure) {
            getLogger().at(Level.WARNING).withCause(failure).log("Cannot read temple template marker");
        }
        templeWorldLayout = existingLayout != null ? existingLayout.configName() : LEGACY_LAYOUT;
        getLogger()
                .at(Level.INFO)
                .log(
                        "Temple layout in use: %s (existing world, config TempleLayout is %s)",
                        templeWorldLayout, templeLayout.configName());
        if (existingLayout == templeLayout) {
            return;
        }
        String reason = existingLayout == null
                ? "was made by a version before 1.1.0 and has no layout marker"
                : "uses the " + existingLayout.configName() + " layout";
        getLogger()
                .at(Level.WARNING)
                .log(
                        "Config TempleLayout is %s but the existing temple %s %s. The existing world is kept as it is. "
                                + "To switch, stop the server, back up the full universe and player data, "
                                + "move this world folder out of worlds and restart. A new temple with a new UUID is then created.",
                        templeLayout.configName(), worldPath, reason);
    }

    private void handleWorldStart(World world) {
        if (world == null) {
            return;
        }

        String worldName = world.getName();
        getLogger().at(Level.INFO).log("World started: %s", worldName);

        if (isTempleWorld(world)) {
            applyTempleWorldConfig(world);
            if (protectKlopMerchant) {
                world.execute(() -> protectMerchantNpcs(world));
            }
        }
    }

    private void handleWorldRemove(RemoveWorldEvent event) {
        if (event == null) {
            return;
        }
        World world = event.getWorld();
        if (world == null || !isTempleWorld(world)) {
            return;
        }

        RemoveWorldEvent.RemovalReason reason = event.getRemovalReason();
        getLogger().at(Level.WARNING).log("Temple world removal requested (%s): %s", reason, world.getName());
    }

    private void handlePlayerDisconnect(PlayerDisconnectEvent event) {
        if (event == null) {
            return;
        }
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef != null) {
            TempleReturnPoints.clearPlayer(playerRef.getUuid());
        }
    }

    private boolean isTempleWorld(World world) {
        if (world == null) {
            return false;
        }
        return TEMPLE_WORLD_NAME.equalsIgnoreCase(world.getName());
    }

    private void applyTempleWorldConfig(World world) {
        world.execute(() -> {
            WorldConfig worldConfig = world.getWorldConfig();
            worldConfig.setDeleteOnUniverseStart(false);
            worldConfig.setDeleteOnRemove(false);
            worldConfig.setSavingConfig(true);
            worldConfig.setSavingPlayers(true);
            worldConfig.setCanSaveChunks(true);
            worldConfig.setSaveNewChunks(true);
            worldConfig.setDisplayName(TEMPLE_DISPLAY_NAME);
            if (worldConfig.getCrashRecovery() == null) {
                worldConfig.setCrashRecovery(new CrashRecoveryConfig());
            }

            String gameplayConfigName = calculateGameplayConfigName();
            String currentConfig = worldConfig.getGameplayConfig();
            if (!gameplayConfigName.equals(currentConfig)) {
                worldConfig.setGameplayConfig(gameplayConfigName);
                getLogger().at(Level.INFO).log("Applied world rules update");
            }

            if (freezeTime != null && !freezeTime.isEmpty()) {
                worldConfig.setGameTimePaused(true);
                java.time.LocalTime time = parseTimeString(freezeTime);
                if (time != null) {
                    java.time.Instant gameTimeInstant =
                            time.atDate(java.time.LocalDate.of(1, 1, 1)).toInstant(java.time.ZoneOffset.UTC);
                    worldConfig.setGameTime(gameTimeInstant);
                    getLogger().at(Level.INFO).log("Frozen time to: %s (%s)", freezeTime, time);
                } else {
                    getLogger()
                            .at(Level.WARNING)
                            .log("Invalid freeze time format: %s (expected 'day', 'night', or 'HH:MM')", freezeTime);
                }
            } else {
                worldConfig.setGameTimePaused(false);
            }

            worldConfig.setFallDamageEnabled(!disableFallDamage);

            boolean targetSpawning = !disableMobs;
            if (worldConfig.isSpawningNPC() != targetSpawning) {
                worldConfig.setSpawningNPC(targetSpawning);
                if (disableMobs) {
                    getLogger().at(Level.INFO).log("Mob spawning disabled");
                } else {
                    getLogger().at(Level.INFO).log("Mob spawning enabled");
                }
            }
            if (worldConfig.isSpawnMarkersEnabled() != targetSpawning) {
                worldConfig.setIsSpawnMarkersEnabled(targetSpawning);
                if (disableMobs) {
                    getLogger().at(Level.INFO).log("Spawn markers disabled");
                } else {
                    getLogger().at(Level.INFO).log("Spawn markers enabled");
                }
            }

            if (worldConfig.isAllNPCFrozen() != freezeNPCs) {
                worldConfig.setIsAllNPCFrozen(freezeNPCs);
                if (freezeNPCs) {
                    getLogger().at(Level.INFO).log("All NPCs frozen");
                } else {
                    getLogger().at(Level.INFO).log("All NPCs unfrozen");
                }
            }

            if (forcedWeather != null && !forcedWeather.isEmpty()) {
                worldConfig.setForcedWeather(forcedWeather);
                getLogger().at(Level.INFO).log("Forced weather set to: %s", forcedWeather);
            } else {
                worldConfig.setForcedWeather(null);
                getLogger().at(Level.INFO).log("Forced weather cleared (dynamic weather)");
            }

            worldConfig.setPvpEnabled(!disablePvP);
            if (disablePvP) {
                getLogger().at(Level.INFO).log("PvP disabled globally");
            }
            if (disablePvPAtSpawn) {
                getLogger().at(Level.INFO).log("Additional PvP protection at spawn area enabled (via ECS system)");
            }

            worldConfig.markChanged();

            getLogger().at(Level.INFO).log("Applied temple world config to: %s", world.getName());
        });
    }

    private void protectMerchantNpcs(World world) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        Archetype<EntityStore> npcQuery = Archetype.of(NPCComponent.getComponentType());
        store.forEachChunk(npcQuery, (chunk, buffer) -> {
            int size = chunk.size();
            for (int i = 0; i < size; i++) {
                NPCComponent npc = chunk.getComponent(i, NPCComponent.getComponentType());
                if (npc == null || !isKlopMerchantNpc(npc)) {
                    continue;
                }
                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                if (ref != null) {
                    buffer.ensureComponent(ref, Invulnerable.getComponentType());
                }
            }
        });
    }

    private boolean isKlopMerchantNpc(NPCComponent npc) {
        String roleName = npc.getRoleName();
        if (containsKlopMerchant(roleName)) {
            return true;
        }
        String npcTypeId = npc.getNPCTypeId();
        return containsKlopMerchant(npcTypeId);
    }

    private boolean containsKlopMerchant(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains(KLOP_KEYWORD) && lower.contains(MERCHANT_KEYWORD);
    }

    private java.time.LocalTime parseTimeString(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }

        if (FREEZE_TIME_DAY.equalsIgnoreCase(timeStr)) {
            return java.time.LocalTime.of(12, 0);
        }
        if (FREEZE_TIME_NIGHT.equalsIgnoreCase(timeStr)) {
            return java.time.LocalTime.of(3, 0);
        }

        try {
            return java.time.LocalTime.parse(timeStr, java.time.format.DateTimeFormatter.ofPattern("H:mm"));
        } catch (java.time.format.DateTimeParseException e) {
            try {
                return java.time.LocalTime.parse(timeStr, java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            } catch (java.time.format.DateTimeParseException e2) {
                return null;
            }
        }
    }

    private void loadConfig() {
        Path dataDirectory = getDataDirectory();
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            getLogger().at(Level.WARNING).log("Failed to create data directory: %s", e.getMessage());
            return;
        }
        configPath = dataDirectory.resolve(CONFIG_FILE_NAME);

        boolean needsSave = false;
        ModConfig config = new ModConfig();
        if (Files.exists(configPath)) {
            try (BufferedReader reader = Files.newBufferedReader(configPath)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) {
                    config = loaded;
                } else {
                    needsSave = true;
                }
            } catch (IOException | JsonParseException e) {
                getLogger().at(Level.WARNING).log("Failed to load config: %s", e.getMessage());
                needsSave = true;
            }
        } else {
            needsSave = true;
        }

        debugMode = config.debug;
        protectKlopMerchant = config.protectKlopMerchant;
        protectTeleporter = config.protectTeleporter;
        protectElectrumTeleporter = config.protectElectrumTeleporter;
        protectFragmentPortal = config.protectFragmentPortal;
        protectEntryPortal = config.protectEntryPortal;
        protectExitPortal = config.protectExitPortal;
        allowBreakEntryPortal = config.allowBreakEntryPortal;
        allowBreakExitPortal = config.allowBreakExitPortal;
        protectMemory = config.protectMemory;
        protectSpawn = config.protectSpawn;
        protectWorld = config.protectWorld;
        disableMobs = config.disableMobs;
        disablePvP = config.disablePvP;
        disablePvPAtSpawn = config.disablePvPAtSpawn;
        disableFallDamage = config.disableFallDamage;
        itemLoss = config.itemLoss;
        npcDamageable = config.npcDamageable;
        godMode = config.godMode;
        freezeNPCs = config.freezeNPCs;
        worldClosed = config.worldClosed;
        forcedWeather = config.forcedWeather;
        freezeTime = config.freezeTime;
        TempleLayout configuredLayout = TempleLayout.fromConfig(config.templeLayout);
        if (configuredLayout == null) {
            getLogger()
                    .at(Level.WARNING)
                    .log(
                            "Unknown TempleLayout '%s'. Use Chapter1 or Chapter0. Using %s.",
                            config.templeLayout, TempleLayout.DEFAULT.configName());
            configuredLayout = TempleLayout.DEFAULT;
        }
        templeLayout = configuredLayout;

        updateProtectionManager();

        saveConfig();
    }

    private void saveConfig() {
        if (configPath == null) {
            return;
        }
        ModConfig config = new ModConfig();
        config.debug = debugMode;
        config.protectKlopMerchant = protectKlopMerchant;
        config.protectTeleporter = protectTeleporter;
        config.protectElectrumTeleporter = protectElectrumTeleporter;
        config.protectFragmentPortal = protectFragmentPortal;
        config.protectEntryPortal = protectEntryPortal;
        config.protectExitPortal = protectExitPortal;
        config.allowBreakEntryPortal = allowBreakEntryPortal;
        config.allowBreakExitPortal = allowBreakExitPortal;
        config.protectMemory = protectMemory;
        config.protectSpawn = protectSpawn;
        config.protectWorld = protectWorld;
        config.disableMobs = disableMobs;
        config.disablePvP = disablePvP;
        config.disablePvPAtSpawn = disablePvPAtSpawn;
        config.disableFallDamage = disableFallDamage;
        config.itemLoss = itemLoss;
        config.npcDamageable = npcDamageable;
        config.godMode = godMode;
        config.freezeNPCs = freezeNPCs;
        config.worldClosed = worldClosed;
        config.forcedWeather = forcedWeather;
        config.freezeTime = freezeTime;
        config.templeLayout = templeLayout.configName();
        try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            getLogger().at(Level.WARNING).log("Failed to save config: %s", e.getMessage());
        }
    }

    public String getTempleLayout() {
        return templeLayout.configName();
    }

    public String getTempleWorldLayout() {
        return templeWorldLayout;
    }

    public boolean isProtectWorld() {
        return protectWorld;
    }

    public boolean isProtectSpawn() {
        return protectSpawn;
    }

    public boolean isProtectMemory() {
        return protectMemory;
    }

    public boolean isPvPDisabled() {
        return disablePvP;
    }

    public boolean isPvPAtSpawnDisabled() {
        return disablePvPAtSpawn;
    }

    public boolean isFallDamageDisabled() {
        return disableFallDamage;
    }

    public boolean isItemLossEnabled() {
        return itemLoss;
    }

    public boolean isNPCDamageable() {
        return npcDamageable;
    }

    public boolean isGodModeEnabled() {
        return godMode;
    }

    public boolean isMobsDisabled() {
        return disableMobs;
    }

    public boolean isNPCsFrozen() {
        return freezeNPCs;
    }

    public boolean isWorldClosed() {
        return worldClosed;
    }

    public String getForcedWeather() {
        return forcedWeather;
    }

    public String getFreezeTime() {
        return freezeTime;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean enabled) {
        if (this.debugMode != enabled) {
            this.debugMode = enabled;
            saveConfig();
        }
    }

    public boolean isProtectKlopMerchant() {
        return protectKlopMerchant;
    }

    public boolean isProtectTeleporter() {
        return protectTeleporter;
    }

    public boolean isProtectElectrumTeleporter() {
        return protectElectrumTeleporter;
    }

    public boolean isProtectFragmentPortal() {
        return protectFragmentPortal;
    }

    public boolean isProtectEntryPortal() {
        return protectEntryPortal;
    }

    public boolean isProtectExitPortal() {
        return protectExitPortal;
    }

    public boolean isAllowBreakEntryPortal() {
        return allowBreakEntryPortal;
    }

    public boolean isAllowBreakExitPortal() {
        return allowBreakExitPortal;
    }

    public void setProtectWorld(boolean protect) {
        if (this.protectWorld != protect) {
            this.protectWorld = protect;
            updateProtectionManager();
            applyToTempleWorld();
            saveConfig();
            getLogger().at(Level.INFO).log("World protection set to: %s", protect);
        }
    }

    public void setDisablePvP(boolean disable) {
        if (this.disablePvP != disable) {
            this.disablePvP = disable;
            applyToTempleWorld();
            saveConfig();
            getLogger().at(Level.INFO).log("PvP disabled set to: %s", disable);
        }
    }

    public void setDisableFallDamage(boolean disable) {
        if (this.disableFallDamage != disable) {
            this.disableFallDamage = disable;
            applyToTempleWorld();
            saveConfig();
            getLogger().at(Level.INFO).log("Fall damage disabled set to: %s", disable);
        }
    }

    public void setDisableMobs(boolean disable) {
        if (this.disableMobs != disable) {
            this.disableMobs = disable;
            applyToTempleWorld();
            saveConfig();
            getLogger().at(Level.INFO).log("Mob spawning disabled set to: %s", disable);
        }
    }

    public void setItemLoss(boolean enabled) {
        if (this.itemLoss != enabled) {
            this.itemLoss = enabled;
            applyToTempleWorld();
            saveConfig();
            getLogger().at(Level.INFO).log("Item loss set to: %s", enabled);
        }
    }

    public void setNPCDamageable(boolean damageable) {
        if (this.npcDamageable != damageable) {
            this.npcDamageable = damageable;
            applyToTempleWorld();
            saveConfig();
            getLogger().at(Level.INFO).log("NPC damageable set to: %s", damageable);
        }
    }

    public void setGodMode(boolean enabled) {
        if (this.godMode != enabled) {
            this.godMode = enabled;
            applyToTempleWorld();
            saveConfig();
            getLogger().at(Level.INFO).log("God mode set to: %s", enabled);
        }
    }

    public void setDisablePvPAtSpawn(boolean disable) {
        if (this.disablePvPAtSpawn != disable) {
            this.disablePvPAtSpawn = disable;
            saveConfig();
            getLogger().at(Level.INFO).log("PvP at spawn disabled set to: %s", disable);
        }
    }

    public void setFreezeNPCs(boolean freeze) {
        if (this.freezeNPCs != freeze) {
            this.freezeNPCs = freeze;
            applyToTempleWorld();
            saveConfig();
            getLogger().at(Level.INFO).log("Freeze NPCs set to: %s", freeze);
        }
    }

    public void setWorldClosed(boolean closed) {
        if (this.worldClosed != closed) {
            this.worldClosed = closed;
            saveConfig();
            getLogger().at(Level.INFO).log("World closed set to: %s", closed);
        }
    }

    public void setForcedWeather(String weather) {
        String normalized = (weather == null || weather.trim().isEmpty()) ? "" : weather.trim();
        if (!normalized.equals(this.forcedWeather == null ? "" : this.forcedWeather)) {
            this.forcedWeather = normalized.isEmpty() ? null : normalized;
            applyToTempleWorld();
            saveConfig();
            getLogger().at(Level.INFO).log("Forced weather set to: %s", forcedWeather);
        }
    }

    public void setFreezeTime(String time) {
        String normalized = (time == null || time.trim().isEmpty()) ? "" : time.trim();
        if (!normalized.equals(this.freezeTime == null ? "" : this.freezeTime)) {
            this.freezeTime = normalized.isEmpty() ? null : normalized;
            applyToTempleWorld();
            saveConfig();
            getLogger().at(Level.INFO).log("Freeze time set to: %s", freezeTime);
        }
    }

    public void setProtectSpawn(boolean protect) {
        if (this.protectSpawn != protect) {
            this.protectSpawn = protect;
            updateProtectionManager();
            saveConfig();
            getLogger().at(Level.INFO).log("Spawn protection set to: %s", protect);
        }
    }

    public void setProtectMemory(boolean protect) {
        if (this.protectMemory != protect) {
            this.protectMemory = protect;
            updateProtectionManager();
            saveConfig();
            getLogger().at(Level.INFO).log("Memory protection set to: %s", protect);
        }
    }

    public void setProtectKlopMerchant(boolean protect) {
        if (this.protectKlopMerchant != protect) {
            this.protectKlopMerchant = protect;
            if (protect) {
                World templeWorld = Universe.get().getWorld(TEMPLE_WORLD_NAME);
                if (templeWorld != null) {
                    templeWorld.execute(() -> protectMerchantNpcs(templeWorld));
                }
            }
            saveConfig();
            getLogger().at(Level.INFO).log("Klop merchant protection set to: %s", protect);
        }
    }

    public void setProtectTeleporter(boolean protect) {
        if (this.protectTeleporter != protect) {
            this.protectTeleporter = protect;
            updateProtectionManager();
            saveConfig();
            getLogger().at(Level.INFO).log("Lab teleporter protection set to: %s", protect);
        }
    }

    public void setProtectElectrumTeleporter(boolean protect) {
        if (this.protectElectrumTeleporter != protect) {
            this.protectElectrumTeleporter = protect;
            updateProtectionManager();
            saveConfig();
            getLogger().at(Level.INFO).log("Electrum teleporter protection set to: %s", protect);
        }
    }

    public void setProtectFragmentPortal(boolean protect) {
        if (this.protectFragmentPortal != protect) {
            this.protectFragmentPortal = protect;
            updateProtectionManager();
            saveConfig();
            getLogger().at(Level.INFO).log("Fragment portal protection set to: %s", protect);
        }
    }

    public void setProtectEntryPortal(boolean protect) {
        if (this.protectEntryPortal != protect) {
            this.protectEntryPortal = protect;
            updateProtectionManager();
            saveConfig();
            getLogger().at(Level.INFO).log("Entry portal protection set to: %s", protect);
        }
    }

    public void setProtectExitPortal(boolean protect) {
        if (this.protectExitPortal != protect) {
            this.protectExitPortal = protect;
            updateProtectionManager();
            saveConfig();
            getLogger().at(Level.INFO).log("Exit portal protection set to: %s", protect);
        }
    }

    public void setAllowBreakEntryPortal(boolean allow) {
        if (this.allowBreakEntryPortal != allow) {
            this.allowBreakEntryPortal = allow;
            updateProtectionManager();
            saveConfig();
            getLogger().at(Level.INFO).log("Allow break entry portal set to: %s", allow);
        }
    }

    public void setAllowBreakExitPortal(boolean allow) {
        if (this.allowBreakExitPortal != allow) {
            this.allowBreakExitPortal = allow;
            updateProtectionManager();
            saveConfig();
            getLogger().at(Level.INFO).log("Allow break exit portal set to: %s", allow);
        }
    }

    private void applyToTempleWorld() {
        World templeWorld = Universe.get().getWorld(TEMPLE_WORLD_NAME);
        if (templeWorld != null) {
            applyTempleWorldConfig(templeWorld);
        }
    }

    private String calculateGameplayConfigName() {
        boolean pvpEnabled = !disablePvP;
        boolean buildEnabled = !protectWorld;
        boolean fallDamageEnabled = !disableFallDamage;
        boolean dropEnabled = itemLoss;
        boolean npcDamageEnabled = npcDamageable;
        boolean godModeEnabled = godMode;

        if (!pvpEnabled
                && !buildEnabled
                && !fallDamageEnabled
                && !dropEnabled
                && !npcDamageEnabled
                && !godModeEnabled) {
            return "ForgottenTemple";
        }

        StringBuilder name = new StringBuilder("ForgottenTemple_");
        if (pvpEnabled) name.append("P");
        if (buildEnabled) name.append("B");
        if (fallDamageEnabled) name.append("F");
        if (dropEnabled) name.append("D");
        if (npcDamageEnabled) name.append("N");
        if (godModeEnabled) name.append("G");

        return name.toString();
    }

    private static final class ModConfig {
        @SerializedName("Debug")
        private boolean debug = false;

        @SerializedName("ProtectKlopMerchant")
        private boolean protectKlopMerchant = true;

        @SerializedName("ProtectTeleporter")
        private boolean protectTeleporter = true;

        @SerializedName("ProtectElectrumTeleporter")
        private boolean protectElectrumTeleporter = true;

        @SerializedName("ProtectFragmentPortal")
        private boolean protectFragmentPortal = true;

        @SerializedName("ProtectEntryPortal")
        private boolean protectEntryPortal = true;

        @SerializedName("ProtectExitPortal")
        private boolean protectExitPortal = true;

        @SerializedName("AllowBreakEntryPortal")
        private boolean allowBreakEntryPortal = false;

        @SerializedName("AllowBreakExitPortal")
        private boolean allowBreakExitPortal = false;

        @SerializedName("ProtectMemory")
        private boolean protectMemory = true;

        @SerializedName("ProtectSpawn")
        private boolean protectSpawn = true;

        @SerializedName("ProtectWorld")
        private boolean protectWorld = true;

        @SerializedName("DisableMobs")
        private boolean disableMobs = true;

        @SerializedName("DisablePvP")
        private boolean disablePvP = true;

        @SerializedName("DisablePvPAtSpawn")
        private boolean disablePvPAtSpawn = true;

        @SerializedName("DisableFallDamage")
        private boolean disableFallDamage = true;

        @SerializedName("ItemLoss")
        private boolean itemLoss = false;

        @SerializedName("NPCDamageable")
        private boolean npcDamageable = false;

        @SerializedName("GodMode")
        private boolean godMode = false;

        @SerializedName("FreezeNPCs")
        private boolean freezeNPCs = false;

        @SerializedName("WorldClosed")
        private boolean worldClosed = false;

        @SerializedName("ForcedWeather")
        private String forcedWeather = "Forgotten_Temple";

        @SerializedName("FreezeTime")
        private String freezeTime = "night";

        @SerializedName("TempleLayout")
        private String templeLayout = TempleLayout.DEFAULT.configName();
    }
}
