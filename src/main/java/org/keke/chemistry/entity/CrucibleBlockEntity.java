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
 * Block entity for the Crucible — a high-temperature container.
 *
 * Key properties:
 *   - Capacity: 200 mL
 *   - Extreme heat resistance: up to 1473 K (1200 °C)
 *   - Suitable for thermite reactions, metal smelting
 *   - Slower cooling rate (thick ceramic walls)
 */
public class CrucibleBlockEntity extends AbstractChemistryContainer {

    private static final double CRUCIBLE_CAPACITY_ML = 200.0;
    private static final double COOLING_K = 0.01; // slow cooling — thick ceramic
    private static final double EFFECTIVE_MASS_G = 200.0;

    public CrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE, pos, state);
        this.maxCapacityMl = CRUCIBLE_CAPACITY_ML;
    }

    @Override
    protected double getEffectiveMassG() {
        return EFFECTIVE_MASS_G;
    }

    @Override
    protected void onReactionEffects(ReactionResult result) {
        if (world == null || world.isClient) return;

        if (world instanceof ServerWorld serverWorld) {
            if (result.getEffects().contains(ReactionEffect.INCENDIARY)) {
                serverWorld.spawnParticles(ParticleTypes.FLAME,
                        pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                        15, 0.15, 0.3, 0.15, 0.05);
                serverWorld.spawnParticles(ParticleTypes.LAVA,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        5, 0.1, 0.1, 0.1, 0.01);
                world.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE,
                        SoundCategory.BLOCKS, 1.0f, 0.8f);
            }
            if (result.getEffects().contains(ReactionEffect.EXOTHERMIC)) {
                serverWorld.spawnParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                        4, 0.1, 0.15, 0.1, 0.01);
            }
            if (result.getEffects().contains(ReactionEffect.GAS_EVOLUTION)) {
                serverWorld.spawnParticles(ParticleTypes.BUBBLE_POP,
                        pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                        3, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    @Override
    protected boolean onThermalOverload(double currentTempK, double deltaT) {
        // Crucibles are extremely heat-resistant — almost never crack
        // Just cap temperature at max safe level
        temperatureK = Math.min(temperatureK, getMaxSafeTemperature());
        return false;
    }

    @Override
    protected double getMaxSafeTemperature() {
        return 1473.15; // 1200 °C — handles thermite
    }

    @Override
    protected double getThermalShockThreshold() {
        return 500.0; // extremely resistant
    }

    public static void tick(World world, BlockPos pos, BlockState state, CrucibleBlockEntity crucible) {
        if (world.isClient) return;
        if (crucible.isEmpty()) return;

        crucible.tickHeatingFromBelow();

        // Slow Newton cooling
        if (Math.abs(crucible.temperatureK - AMBIENT_TEMP_K) > 0.01) {
            crucible.temperatureK -= COOLING_K * (crucible.temperatureK - AMBIENT_TEMP_K);
            crucible.markDirty();
            crucible.syncToClient();
        }

        Map<String, Double> gases = crucible.removeGases();
        if (!gases.isEmpty()) {
            GasCloudHelper.spawnGasClouds(world, pos, gases);
        }

        // Glowing particles when very hot
        if (crucible.temperatureK > 873.15 && world instanceof ServerWorld serverWorld) { // > 600°C
            serverWorld.spawnParticles(ParticleTypes.FLAME,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    1, 0.1, 0.05, 0.1, 0.0);
        }

        crucible.tickSedimentation();
        crucible.checkAndHandleOverflow();
    }
}
