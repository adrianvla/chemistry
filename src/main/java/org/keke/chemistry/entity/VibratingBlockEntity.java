package org.keke.chemistry.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.keke.chemistry.block.VibratingBlock;
import org.keke.chemistry.utils.StateOfMatter;

/**
 * Block entity for the vibrating machine.
 * Each tick, checks for a chemistry container above it. If found,
 * decreases sedimentation progress of solid compounds towards 0.0,
 * re-mixing them.
 *
 * Rate: 0.025 per tick → fully re-mixed in ~2 seconds (40 ticks).
 */
public class VibratingBlockEntity extends BlockEntity {

    /** Vibrating/remixing rate (0.025/tick → ~2s). */
    private static final double VIBRATING_RATE = 0.025;

    public VibratingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VIBRATING, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, VibratingBlockEntity self) {
        if (world.isClient) return;

        BlockPos above = pos.up();
        BlockEntity beAbove = world.getBlockEntity(above);

        boolean active = false;

        if (beAbove instanceof AbstractChemistryContainer container) {
            // Check if there are any settled solid compounds
            boolean hasSettled = false;
            for (var entry : container.getContents().entrySet()) {
                if (container.getCompoundState(entry.getKey()) == StateOfMatter.SOLID) {
                    if (container.getSedimentationProgress(entry.getKey()) > 0.0) {
                        hasSettled = true;
                        break;
                    }
                }
            }

            if (hasSettled) {
                container.decreaseSedimentation(VIBRATING_RATE);
                active = true;
            }
        }

        // Update ACTIVE state if changed
        if (state.get(VibratingBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(VibratingBlock.ACTIVE, active));
        }
    }
}
