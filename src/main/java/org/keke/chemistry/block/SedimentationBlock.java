package org.keke.chemistry.block;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.keke.chemistry.entity.ModBlockEntities;
import org.keke.chemistry.entity.SedimentationBlockEntity;

/**
 * Sedimentation machine — when a chemistry container (beaker, petri dish, etc.)
 * is placed on top, solid compounds inside settle to the bottom at an accelerated
 * rate (0.0167/tick → fully settled in ~3 seconds).
 *
 * Has an ACTIVE boolean property indicating whether it is currently processing
 * a container above it.
 */
public class SedimentationBlock extends BlockWithEntity {

    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    private static final VoxelShape SHAPE = Block.createCuboidShape(1, 0, 1, 15, 8, 15);

    public SedimentationBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(ACTIVE, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SedimentationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return checkType(type, ModBlockEntities.SEDIMENTATION,
                (World w, BlockPos p, BlockState s, SedimentationBlockEntity e) ->
                        SedimentationBlockEntity.tick(w, p, s, e));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
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
        if (state.get(ACTIVE) && random.nextInt(3) == 0) {
            // Slow downward particles mimicking settling
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            double y = pos.getY() + 1.2;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            world.addParticle(ParticleTypes.FALLING_WATER, x, y, z, 0.0, -0.02, 0.0);
        }
    }
}
