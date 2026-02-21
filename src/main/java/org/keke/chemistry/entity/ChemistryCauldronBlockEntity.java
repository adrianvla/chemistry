package org.keke.chemistry.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
 * Block entity for the Chemistry Cauldron — a large, sturdy container for
 * bulk chemical reactions. Replaces a vanilla cauldron when chemicals are added.
 *
 * Properties:
 *   - Large capacity: 4000 mL (4 liters)
 *   - Very heat-resistant: withstands up to 1273 K (1000 °C)
 *   - Slow cooling rate (large thermal mass)
 *   - Reverts to vanilla cauldron when emptied
 */
public class ChemistryCauldronBlockEntity extends AbstractChemistryContainer {

    /** Cauldron capacity in mL. */
    private static final double CAULDRON_CAPACITY_ML = 4000.0;

    /** Newton cooling constant — slow because of iron mass. */
    private static final double COOLING_K = 0.005;

    /** Effective mass for heat calculations (iron + liquid). */
    private static final double EFFECTIVE_MASS_G = 1000.0;

    public ChemistryCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEMISTRY_CAULDRON, pos, state);
        this.maxCapacityMl = CAULDRON_CAPACITY_ML;
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
                SoundCategory.BLOCKS, 0.8f, 0.8f);

        if (world instanceof ServerWorld serverWorld) {
            if (result.getEffects().contains(ReactionEffect.GAS_EVOLUTION)) {
                serverWorld.spawnParticles(ParticleTypes.BUBBLE_POP,
                        pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                        12, 0.25, 0.15, 0.25, 0.02);
            }
            if (result.getEffects().contains(ReactionEffect.EXOTHERMIC)) {
                serverWorld.spawnParticles(ParticleTypes.FLAME,
                        pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                        5, 0.2, 0.05, 0.2, 0.01);
            }
            if (result.getEffects().contains(ReactionEffect.PRECIPITATE)) {
                serverWorld.spawnParticles(ParticleTypes.WHITE_ASH,
                        pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                        8, 0.2, 0.15, 0.2, 0.0);
            }
        }
    }

    @Override
    protected boolean onThermalOverload(double currentTempK, double deltaT) {
        // Iron cauldron doesn't crack, but may release a burst of steam/sparks
        if (world != null && !world.isClient && world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.LAVA,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    8, 0.3, 0.2, 0.3, 0.05);
            world.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH,
                    SoundCategory.BLOCKS, 1.0f, 0.5f);
        }
        // Cap temperature but don't destroy
        temperatureK = Math.min(temperatureK, getMaxSafeTemperature());
        return false;
    }

    @Override
    protected double getMaxSafeTemperature() {
        return 1273.15; // 1000 °C — iron melting point is ~1538 °C
    }

    @Override
    protected double getThermalShockThreshold() {
        return 300.0; // iron is very resistant
    }

    // ── Tick ────────────────────────────────────────────────────────────

    public static void tick(World world, BlockPos pos, BlockState state,
                            ChemistryCauldronBlockEntity cauldron) {
        if (world.isClient) return;

        // If empty, revert to vanilla cauldron
        if (cauldron.isEmpty()) {
            world.removeBlockEntity(pos);
            world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
            return;
        }

        // Heating from bunsen burner below
        cauldron.tickHeatingFromBelow();

        // Newton cooling
        if (Math.abs(cauldron.temperatureK - AMBIENT_TEMP_K) > 0.01) {
            cauldron.temperatureK -= COOLING_K * (cauldron.temperatureK - AMBIENT_TEMP_K);
            cauldron.markDirty();
            cauldron.syncToClient();
        }

        // Steam from hot contents
        if (cauldron.temperatureK > 373.15 && world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                    2, 0.2, 0.1, 0.2, 0.01);
        }

        // Gas escape
        Map<String, Double> gases = cauldron.removeGases();
        if (!gases.isEmpty()) {
            GasCloudHelper.spawnGasClouds(world, pos, gases);
        }

        // Natural sedimentation
        cauldron.tickSedimentation();

        // Overflow check
        cauldron.checkAndHandleOverflow();
    }
}
