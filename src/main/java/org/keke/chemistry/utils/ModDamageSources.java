package org.keke.chemistry.utils;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.keke.chemistry.Chemistry;

/**
 * Custom damage sources for chemistry-related incidents.
 *
 * Damage type "lab_accident" produces the death message:
 *   "<Player> had a lab accident."
 *
 * Registered as a data-driven DamageType via JSON at:
 *   data/chemistry/damage_type/lab_accident.json
 *
 * Death message translation key:
 *   death.attack.lab_accident = "%1$s had a lab accident."
 */
public class ModDamageSources {

    public static final RegistryKey<DamageType> LAB_ACCIDENT =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier(Chemistry.MOD_ID, "lab_accident"));

    /**
     * Create a DamageSource for lab accident damage.
     * @param world the world (needed to look up the registry entry)
     * @return a DamageSource that produces "had a lab accident" death messages
     */
    public static DamageSource labAccident(World world) {
        return new DamageSource(
                world.getRegistryManager()
                        .get(RegistryKeys.DAMAGE_TYPE)
                        .entryOf(LAB_ACCIDENT)
        );
    }
}
