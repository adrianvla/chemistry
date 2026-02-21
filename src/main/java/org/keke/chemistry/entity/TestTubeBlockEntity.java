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
 * Block entity for the Test Tube — a narrow container for small-volume reactions.
 *
 * Key properties:
 *   - Small capacity: 25 mL
 *   - Fragile: cracks at lower temperatures than beakers
 *   - Fast heating: narrow profile means rapid temperature changes
 *   - Does not interact with cooling blocks
 */
public class TestTubeBlockEntity extends AbstractChemistryContainer {

    private static final double TEST_TUBE_CAPACITY_ML = 25.0;
    private static final double COOLING_K = 0.03;
    private static final double EFFECTIVE_MASS_G = 25.0;

    public TestTubeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEST_TUBE, pos, state);
        this.maxCapacityMl = TEST_TUBE_CAPACITY_ML;
    }

    @Override
    protected double getEffectiveMassG() {
        return EFFECTIVE_MASS_G;
    }

    @Override
    protected void onReactionEffects(ReactionResult result) {
        if (world == null || world.isClient) return;

        world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW,
                SoundCategory.BLOCKS, 0.3f, 1.4f);

        if (world instanceof ServerWorld serverWorld) {
            if (result.getEffects().contains(ReactionEffect.GAS_EVOLUTION)) {
                serverWorld.spawnParticles(ParticleTypes.BUBBLE_POP,
                        pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                        3, 0.05, 0.1, 0.05, 0.02);
            }
            if (result.getEffects().contains(ReactionEffect.EXOTHERMIC)) {
                serverWorld.spawnParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                        2, 0.05, 0.1, 0.05, 0.01);
            }
        }
    }

    @Override
    protected boolean onThermalOverload(double currentTempK, double deltaT) {
        if (world == null || world.isClient) return false;

        // Test tubes crack more easily — lower threshold
        world.playSound(null, pos, SoundEvents.BLOCK_GLASS_BREAK,
                SoundCategory.BLOCKS, 1.0f, 1.5f);
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.CRIT,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    10, 0.2, 0.3, 0.2, 0.1);
        }

        // Spill contents and break
        Map<String, Double> spilled = Map.copyOf(contents);
        contents.clear();
        onOverflow(spilled);
        world.breakBlock(pos, false);
        return true;
    }

    @Override
    protected double getMaxSafeTemperature() {
        return 473.15; // 200 °C — lower than beakers
    }

    @Override
    protected double getThermalShockThreshold() {
        return 60.0; // fragile
    }

    public static void tick(World world, BlockPos pos, BlockState state, TestTubeBlockEntity tube) {
        if (world.isClient) return;
        if (tube.isEmpty()) return;

        tube.tickHeatingFromBelow();

        if (Math.abs(tube.temperatureK - AMBIENT_TEMP_K) > 0.01) {
            tube.temperatureK -= COOLING_K * (tube.temperatureK - AMBIENT_TEMP_K);
            tube.markDirty();
            tube.syncToClient();
        }

        Map<String, Double> gases = tube.removeGases();
        if (!gases.isEmpty()) {
            GasCloudHelper.spawnGasClouds(world, pos, gases);
        }

        tube.tickSedimentation();
        tube.checkAndHandleOverflow();
    }
}
