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
 * Block entity for the Bottle — a medium-capacity container.
 *
 * Key properties:
 *   - Medium capacity: 100 mL
 *   - Standard glass durability
 *   - Open-top: gases escape normally
 */
public class BottleBlockEntity extends AbstractChemistryContainer {

    private static final double BOTTLE_CAPACITY_ML = 100.0;
    private static final double COOLING_K = 0.025;
    private static final double EFFECTIVE_MASS_G = 100.0;

    public BottleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOTTLE, pos, state);
        this.maxCapacityMl = BOTTLE_CAPACITY_ML;
    }

    @Override
    protected double getEffectiveMassG() {
        return EFFECTIVE_MASS_G;
    }

    @Override
    protected void onReactionEffects(ReactionResult result) {
        if (world == null || world.isClient) return;

        world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW,
                SoundCategory.BLOCKS, 0.4f, 1.0f);

        if (world instanceof ServerWorld serverWorld) {
            if (result.getEffects().contains(ReactionEffect.GAS_EVOLUTION)) {
                serverWorld.spawnParticles(ParticleTypes.BUBBLE_POP,
                        pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                        4, 0.08, 0.1, 0.08, 0.02);
            }
        }
    }

    @Override
    protected boolean onThermalOverload(double currentTempK, double deltaT) {
        if (world == null || world.isClient) return false;

        world.playSound(null, pos, SoundEvents.BLOCK_GLASS_BREAK,
                SoundCategory.BLOCKS, 1.0f, 1.0f);
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.CRIT,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    12, 0.3, 0.3, 0.3, 0.1);
        }

        Map<String, Double> spilled = Map.copyOf(contents);
        contents.clear();
        onOverflow(spilled);
        world.breakBlock(pos, false);
        return true;
    }

    @Override
    protected double getMaxSafeTemperature() {
        return 523.15; // 250 °C
    }

    @Override
    protected double getThermalShockThreshold() {
        return 80.0;
    }

    public static void tick(World world, BlockPos pos, BlockState state, BottleBlockEntity bottle) {
        if (world.isClient) return;
        if (bottle.isEmpty()) return;

        bottle.tickHeatingFromBelow();

        if (Math.abs(bottle.temperatureK - AMBIENT_TEMP_K) > 0.01) {
            bottle.temperatureK -= COOLING_K * (bottle.temperatureK - AMBIENT_TEMP_K);
            bottle.markDirty();
            bottle.syncToClient();
        }

        Map<String, Double> gases = bottle.removeGases();
        if (!gases.isEmpty()) {
            GasCloudHelper.spawnGasClouds(world, pos, gases);
        }

        bottle.tickSedimentation();
        bottle.checkAndHandleOverflow();
    }
}
