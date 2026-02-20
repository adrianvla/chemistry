package org.keke.chemistry.entity;

import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.keke.chemistry.reaction.ReactionEffect;
import org.keke.chemistry.reaction.ReactionResult;
import org.keke.chemistry.utils.GasCloudHelper;

import java.util.Map;

/**
 * Block entity for the Petri Dish — a shallow, open container used for
 * evaporation, crystallization, and small-volume reactions.
 *
 * Key differences from BeakerLiquidBlockEntity:
 *   - Small capacity: 50 mL (fixed)
 *   - Heat-resistant: no thermal overload / cracking
 *   - Faster evaporation: liquids evaporate at a higher rate
 *   - No cooling block interaction
 *   - Newton cooling constant is higher (open, shallow dish)
 */
public class PetriDishBlockEntity extends AbstractChemistryContainer {

    /** Petri dish capacity in mL. */
    private static final double PETRI_CAPACITY_ML = 50.0;

    /** Newton cooling constant — higher because dish is shallow and open. */
    private static final double COOLING_K = 0.05;

    /** Effective mass for heat calculations (much less solution). */
    private static final double EFFECTIVE_MASS_G = 50.0;

    public PetriDishBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PETRI_DISH, pos, state);
        this.maxCapacityMl = PETRI_CAPACITY_ML;
    }

    // ── Hooks ──────────────────────────────────────────────────────────

    @Override
    protected double getEffectiveMassG() {
        return EFFECTIVE_MASS_G;
    }

    @Override
    protected void onReactionEffects(ReactionResult result) {
        if (world == null || world.isClient) return;

        world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW,
                SoundCategory.BLOCKS, 0.4f, 1.2f);

        if (world instanceof ServerWorld serverWorld) {
            if (result.getEffects().contains(ReactionEffect.GAS_EVOLUTION)) {
                serverWorld.spawnParticles(ParticleTypes.BUBBLE_POP,
                        pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                        4, 0.1, 0.05, 0.1, 0.02);
            }
            if (result.getEffects().contains(ReactionEffect.PRECIPITATE)) {
                serverWorld.spawnParticles(ParticleTypes.WHITE_ASH,
                        pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                        3, 0.1, 0.05, 0.1, 0.0);
            }
        }
    }

    @Override
    protected boolean onThermalOverload(double currentTempK, double deltaT) {
        // Petri dishes are heat-resistant (borosilicate glass or ceramic)
        // Just cap the temperature instead of cracking
        temperatureK = Math.min(temperatureK, getMaxSafeTemperature());
        return false;
    }

    /** Petri dishes tolerate higher temperatures. */
    @Override
    protected double getMaxSafeTemperature() {
        return 773.15; // 500 °C
    }

    @Override
    protected double getThermalShockThreshold() {
        return 200.0; // more resistant to thermal shock
    }

    // ── Tick ────────────────────────────────────────────────────────────

    public static void tick(World world, BlockPos pos, BlockState state, PetriDishBlockEntity dish) {
        if (world.isClient) return;
        if (dish.isEmpty()) return;

        // Newton's law of cooling (faster for shallow dish)
        if (Math.abs(dish.temperatureK - AMBIENT_TEMP_K) > 0.01) {
            dish.temperatureK -= COOLING_K * (dish.temperatureK - AMBIENT_TEMP_K);
            dish.markDirty();
            dish.syncToClient();
        }

        // Gas escape
        Map<String, Double> gases = dish.removeGases();
        if (!gases.isEmpty()) {
            GasCloudHelper.spawnGasClouds(world, pos, gases);
        }

        // Natural sedimentation
        dish.tickSedimentation();

        // Steam from hot liquids
        if (dish.temperatureK > 373.15 && world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                    1, 0.15, 0.05, 0.15, 0.01);
        }

        // Overflow check (thermal expansion)
        dish.checkAndHandleOverflow();
    }
}
