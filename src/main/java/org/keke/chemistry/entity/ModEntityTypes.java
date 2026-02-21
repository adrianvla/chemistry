package org.keke.chemistry.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.keke.chemistry.Chemistry;

/**
 * Registry for custom entity types (projectiles, etc.).
 */
public class ModEntityTypes {

    public static final EntityType<ThrownContainerEntity> THROWN_CONTAINER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(Chemistry.MOD_ID, "thrown_container"),
            FabricEntityTypeBuilder.<ThrownContainerEntity>create(SpawnGroup.MISC, ThrownContainerEntity::new)
                    .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(10)
                    .build()
    );

    public static void registerAll() {
        Chemistry.LOGGER.info("Registering entity types for " + Chemistry.MOD_ID);
    }
}
