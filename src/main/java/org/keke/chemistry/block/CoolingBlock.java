package org.keke.chemistry.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Ice-bath cooling block. When placed under a beaker, increases the
 * beaker's effective heat capacity (10×) and cooling rate,
 * preventing thermal-shock cracking from exothermic reactions.
 *
 * No block entity needed — the beaker entity detects this block below it.
 */
public class CoolingBlock extends Block {

    private static final VoxelShape SHAPE = Block.createCuboidShape(1, 0, 1, 15, 6, 15);

    public CoolingBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // Occasionally spawn snowflake-like particles
        if (random.nextInt(5) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            double y = pos.getY() + 0.4;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            world.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0.0, 0.02, 0.0);
        }
    }
}
