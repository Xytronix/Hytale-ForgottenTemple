package com.xytronix.forgottentemple.protection;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TempleDamageSystem extends EntityEventSystem<EntityStore, Damage> {

    private static final Logger LOGGER = Logger.getLogger(TempleDamageSystem.class.getName());

    private final TempleProtectionManager protectionManager;
    private final Supplier<Boolean> disablePvPSupplier;
    private final Supplier<Boolean> disablePvPAtSpawnSupplier;
    private final Supplier<Boolean> disableFallDamageSupplier;
    private final Supplier<Boolean> godModeSupplier;
    private final Supplier<Boolean> debugSupplier;

    public TempleDamageSystem(
            TempleProtectionManager protectionManager,
            Supplier<Boolean> disablePvPSupplier,
            Supplier<Boolean> disablePvPAtSpawnSupplier,
            Supplier<Boolean> disableFallDamageSupplier,
            Supplier<Boolean> godModeSupplier,
            Supplier<Boolean> debugSupplier) {
        super(Damage.class);
        this.protectionManager = protectionManager;
        this.disablePvPSupplier = disablePvPSupplier;
        this.disablePvPAtSpawnSupplier = disablePvPAtSpawnSupplier;
        this.disableFallDamageSupplier = disableFallDamageSupplier;
        this.godModeSupplier = godModeSupplier;
        this.debugSupplier = debugSupplier;
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage event) {

        boolean debug = debugSupplier.get();

        if (event.isCancelled()) {
            return;
        }

        World world = store.getExternalData().getWorld();
        if (world == null || !protectionManager.isTempleWorld(world)) {
            return;
        }

        Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(index);

        Player victimPlayer = store.getComponent(victimRef, Player.getComponentType());
        PlayerRef victimPlayerRef = store.getComponent(victimRef, PlayerRef.getComponentType());
        if (victimPlayer == null || victimPlayerRef == null) {
            return;
        }

        DamageCause cause = DamageCause.getAssetMap().getAsset(event.getDamageCauseIndex());
        if (debug) {
            LOGGER.log(Level.INFO, "[Damage] cause={0} godMode={1}", new Object[] {cause, godModeSupplier.get()});
        }

        if (godModeSupplier.get()) {
            if (debug) {
                LOGGER.log(Level.INFO, "[Damage] DENIED - god mode active");
            }
            event.setCancelled(true);
            return;
        }

        if (cause != null && "Fall".equalsIgnoreCase(cause.getId()) && disableFallDamageSupplier.get()) {
            event.setCancelled(true);
            return;
        }

        Damage.Source source = event.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return;
        }

        Store<EntityStore> attackerStore = attackerRef.getStore();
        if (attackerStore == null) {
            return;
        }

        Player attackerPlayer;
        PlayerRef attackerPlayerRef;
        try {
            attackerPlayer = attackerStore.getComponent(attackerRef, Player.getComponentType());
            attackerPlayerRef = attackerStore.getComponent(attackerRef, PlayerRef.getComponentType());
        } catch (Exception e) {
            return;
        }

        if (attackerPlayer == null || attackerPlayerRef == null) {
            return;
        }

        if (disablePvPSupplier.get()) {
            if (debug) {
                LOGGER.log(Level.INFO, "[Damage] DENIED - PvP globally disabled");
            }
            event.setCancelled(true);
            return;
        }

        if (disablePvPAtSpawnSupplier.get()) {
            Transform attackerTransform = attackerPlayerRef.getTransform();
            Transform victimTransform = victimPlayerRef.getTransform();

            if (attackerTransform == null || victimTransform == null) {
                return;
            }

            var attackerPos = attackerTransform.getPosition();
            var victimPos = victimTransform.getPosition();

            if (attackerPos == null || victimPos == null) {
                return;
            }

            boolean attackerInSpawn = protectionManager.isInProtectedLandArea(attackerPos.x, attackerPos.z);
            boolean victimInSpawn = protectionManager.isInProtectedLandArea(victimPos.x, victimPos.z);

            if (debug) {
                LOGGER.log(
                        Level.INFO,
                        "[Damage] PvP spawn check: attacker=({0},{1}) inSpawn={2}, victim=({3},{4}) inSpawn={5}",
                        new Object[] {
                            attackerPos.x, attackerPos.z, attackerInSpawn, victimPos.x, victimPos.z, victimInSpawn
                        });
            }

            if (attackerInSpawn || victimInSpawn) {
                if (debug) {
                    LOGGER.log(Level.INFO, "[Damage] DENIED - PvP at spawn area");
                }
                event.setCancelled(true);
            }
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }
}
