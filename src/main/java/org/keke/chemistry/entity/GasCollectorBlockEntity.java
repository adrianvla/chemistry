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
 * Block entity for the Gas Collector — captures gases from the container below.
 *
 * Tick behavior:
 *   - Checks the block entity directly below (pos.down())
 *   - If it's an AbstractChemistryContainer, intercepts gas escape
 *   - Stores captured gases in its own contents map
 *   - Capacity: 500 mL (measured in moles at STP via ideal gas law)
 *
 * The gas collector overrides removeGases() of the container below
 * by periodically checking and transferring gas compounds.
 */
public class GasCollectorBlockEntity extends AbstractChemistryContainer {

    private static final double GAS_COLLECTOR_CAPACITY_ML = 500.0;
    private static final double EFFECTIVE_MASS_G = 50.0;

    /** Moles of gas at STP per mL (ideal gas: 1 mol = 22400 mL at STP). */
    private static final double MOLES_PER_ML_STP = 1.0 / 22400.0;

    public GasCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_COLLECTOR, pos, state);
        this.maxCapacityMl = GAS_COLLECTOR_CAPACITY_ML;
    }

    @Override
    protected double getEffectiveMassG() {
        return EFFECTIVE_MASS_G;
    }

    @Override
    protected void onReactionEffects(ReactionResult result) {
        // Gas collector doesn't process reactions, just stores gases
    }

    @Override
    protected boolean onThermalOverload(double currentTempK, double deltaT) {
        // Glass gas collector can crack
        return false;
    }

    @Override
    protected double getMaxSafeTemperature() {
        return 573.15; // 300 °C
    }

    /**
     * Calculate remaining gas capacity in moles.
     */
    public double getRemainingCapacityMoles() {
        double maxMoles = GAS_COLLECTOR_CAPACITY_ML * MOLES_PER_ML_STP;
        double currentMoles = getTotalMoles();
        return Math.max(0, maxMoles - currentMoles);
    }

    // ── Tick ────────────────────────────────────────────────────────────

    public static void tick(World world, BlockPos pos, BlockState state, GasCollectorBlockEntity collector) {
        if (world.isClient) return;

        // Every 10 ticks, check the container below for gas compounds
        if (world.getTime() % 10 != 0) return;

        BlockPos belowPos = pos.down();
        BlockEntity belowEntity = world.getBlockEntity(belowPos);

        if (belowEntity instanceof AbstractChemistryContainer containerBelow) {
            Map<String, Double> belowContents = containerBelow.getContents();
            Map<String, Double> gasesToCapture = new LinkedHashMap<>();

            // Identify gas-phase compounds in the container below
            for (var entry : belowContents.entrySet()) {
                String formula = entry.getKey();
                double moles = entry.getValue();

                StateOfMatter som = CompoundPhysicalData.getStateAtTemperature(
                        formula, containerBelow.getTemperatureK());
                if (som == StateOfMatter.GAS) {
                    // Transfer up to remaining capacity
                    double remaining = collector.getRemainingCapacityMoles();
                    if (remaining <= 0) break;

                    double transferMoles = Math.min(moles, remaining);
                    gasesToCapture.put(formula, transferMoles);
                }
            }

            // Transfer gases
            if (!gasesToCapture.isEmpty()) {
                for (var entry : gasesToCapture.entrySet()) {
                    containerBelow.removeChemical(entry.getKey(), entry.getValue());
                    collector.addChemical(entry.getKey(), entry.getValue());
                }

                if (world instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.CLOUD,
                            pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                            2, 0.1, 0.05, 0.1, 0.0);
                }
                world.playSound(null, pos, SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP,
                        SoundCategory.BLOCKS, 0.2f, 1.5f);
            }
        }
    }
}
