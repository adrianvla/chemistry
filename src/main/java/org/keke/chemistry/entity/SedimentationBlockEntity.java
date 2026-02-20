package org.keke.chemistry.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.keke.chemistry.block.SedimentationBlock;

/**
 * Block entity for the sedimentation machine.
 * Each tick, checks for a chemistry container (BeakerLiquidBlockEntity, etc.)
 * above it. If found, accelerates sedimentation of solid compounds towards 1.0.
 *
 * Rate: 0.0167 per tick → fully settled in ~3 seconds (60 ticks).
 */
public class SedimentationBlockEntity extends BlockEntity {

    /** Sedimentation rate for the machine (0.0167/tick → ~3s). */
    private static final double SEDIMENTATION_RATE = 0.0167;

    public SedimentationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SEDIMENTATION, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, SedimentationBlockEntity self) {
        if (world.isClient) return;

        BlockPos above = pos.up();
        BlockEntity beAbove = world.getBlockEntity(above);

        boolean active = false;

        if (beAbove instanceof AbstractChemistryContainer container) {
            // Check if there are any solid compounds that aren't fully settled
            boolean hasSolids = false;
            for (var entry : container.getContents().entrySet()) {
                if (container.getCompoundState(entry.getKey()) == org.keke.chemistry.utils.StateOfMatter.SOLID) {
                    if (container.getSedimentationProgress(entry.getKey()) < 1.0) {
                        hasSolids = true;
                        break;
                    }
                }
            }

            if (hasSolids) {
                container.accelerateSedimentation(SEDIMENTATION_RATE);
                active = true;
            }
        }

        // Update ACTIVE state if changed
        if (state.get(SedimentationBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(SedimentationBlock.ACTIVE, active));
        }
    }
}
