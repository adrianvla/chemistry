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
import org.keke.chemistry.entity.VibratingBlockEntity;

/**
 * Vibrating machine — when a chemistry container is placed on top, it resets
 * sedimentation progress back to 0.0, re-mixing settled solid compounds.
 *
 * Rate: 0.025 per tick → fully re-mixed in ~2 seconds (40 ticks).
 */
public class VibratingBlock extends BlockWithEntity {

    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    private static final VoxelShape SHAPE = Block.createCuboidShape(1, 0, 1, 15, 8, 15);

    public VibratingBlock(Settings settings) {
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
        return new VibratingBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return checkType(type, ModBlockEntities.VIBRATING,
                (World w, BlockPos p, BlockState s, VibratingBlockEntity e) ->
                        VibratingBlockEntity.tick(w, p, s, e));
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
        if (state.get(ACTIVE)) {
            // Vibrating particles rising upward
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            double y = pos.getY() + 0.6;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            world.addParticle(ParticleTypes.CLOUD, x, y, z,
                    (random.nextDouble() - 0.5) * 0.01,
                    0.03,
                    (random.nextDouble() - 0.5) * 0.01);
        }
    }
}
