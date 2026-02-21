package org.keke.chemistry.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.keke.chemistry.reaction.ReactionResult;
import org.keke.chemistry.utils.CompoundPhysicalData;
import org.keke.chemistry.utils.StateOfMatter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Block entity for the Condenser — cools gases from adjacent containers into liquids.
 *
 * Tick behavior:
 *   - Checks containers below for gas-phase compounds
 *   - Captures gas, cools it below boiling point → stored as liquid condensate
 *   - Produces drip particles
 *   - Capacity: 250 mL of condensate
 */
public class CondenserBlockEntity extends AbstractChemistryContainer {

    private static final double CONDENSER_CAPACITY_ML = 250.0;
    private static final double EFFECTIVE_MASS_G = 50.0;
    /** Condenser always operates near ambient temperature. */
    private static final double CONDENSER_TEMP_K = 293.15;

    public CondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONDENSER, pos, state);
        this.maxCapacityMl = CONDENSER_CAPACITY_ML;
        this.temperatureK = CONDENSER_TEMP_K;
    }

    @Override
    protected double getEffectiveMassG() {
        return EFFECTIVE_MASS_G;
    }

    @Override
    protected void onReactionEffects(ReactionResult result) {
        // Condensers do not process reactions
    }

    @Override
    protected boolean onThermalOverload(double currentTempK, double deltaT) {
        return false;
    }

    @Override
    protected double getMaxSafeTemperature() {
        return 573.15;
    }

    public static void tick(World world, BlockPos pos, BlockState state, CondenserBlockEntity condenser) {
        if (world.isClient) return;

        // Keep temperature at ambient
        condenser.temperatureK = CONDENSER_TEMP_K;

        // Every 10 ticks, check below for gases to condense
        if (world.getTime() % 10 != 0) return;

        BlockPos belowPos = pos.down();
        BlockEntity belowEntity = world.getBlockEntity(belowPos);

        if (belowEntity instanceof AbstractChemistryContainer containerBelow) {
            Map<String, Double> belowContents = containerBelow.getContents();
            Map<String, Double> toCondense = new LinkedHashMap<>();

            for (var entry : belowContents.entrySet()) {
                String formula = entry.getKey();
                double moles = entry.getValue();

                // Check if compound is gas at container's temperature
                StateOfMatter somBelow = CompoundPhysicalData.getStateAtTemperature(
                        formula, containerBelow.getTemperatureK());
                if (somBelow != StateOfMatter.GAS) continue;

                // Check if it would be liquid at condenser temperature
                StateOfMatter somCondensed = CompoundPhysicalData.getStateAtTemperature(
                        formula, CONDENSER_TEMP_K);
                if (somCondensed == StateOfMatter.LIQUID || somCondensed == StateOfMatter.SOLID) {
                    // Transfer and condense
                    double transferMoles = Math.min(moles, 0.1); // gradual condensation
                    toCondense.put(formula, transferMoles);
                }
            }

            if (!toCondense.isEmpty()) {
                for (var entry : toCondense.entrySet()) {
                    containerBelow.removeChemical(entry.getKey(), entry.getValue());
                    condenser.addChemical(entry.getKey(), entry.getValue());
                }

                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.DRIPPING_WATER,
                            pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5,
                            2, 0.1, 0.0, 0.1, 0.0);
                }
                world.playSound(null, pos, SoundEvents.WEATHER_RAIN,
                        SoundCategory.BLOCKS, 0.1f, 2.0f);
            }
        }
    }
}
