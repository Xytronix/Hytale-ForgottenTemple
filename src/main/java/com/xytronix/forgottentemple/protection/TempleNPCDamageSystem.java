package com.xytronix.forgottentemple.protection;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.components.NPCComponent;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TempleNPCDamageSystem extends EntityEventSystem<EntityStore, Damage> {

    private static final Logger LOGGER = Logger.getLogger(TempleNPCDamageSystem.class.getName());

    private final TempleProtectionManager protectionManager;
    private final Supplier<Boolean> npcDamageableSupplier;
    private final Supplier<Boolean> debugSupplier;

    public TempleNPCDamageSystem(
            TempleProtectionManager protectionManager,
            Supplier<Boolean> npcDamageableSupplier,
            Supplier<Boolean> debugSupplier) {
        super(Damage.class);
        this.protectionManager = protectionManager;
        this.npcDamageableSupplier = npcDamageableSupplier;
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

        NPCComponent victimNPC = store.getComponent(victimRef, NPCComponent.getComponentType());
        if (victimNPC == null) {
            return;
        }

        if (!npcDamageableSupplier.get()) {
            if (debug) {
                LOGGER.log(Level.INFO, "[NPCDamage] DENIED - NPC damage disabled");
            }
            event.setCancelled(true);
        } else if (debug) {
            LOGGER.log(Level.INFO, "[NPCDamage] ALLOWED - NPC damage enabled");
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return NPCComponent.getComponentType();
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }
}
