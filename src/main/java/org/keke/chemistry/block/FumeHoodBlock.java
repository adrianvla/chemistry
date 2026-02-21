package org.keke.chemistry.block;

import net.minecraft.block.*;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fume Hood — absorbs and neutralizes gas clouds in a radius around it.
 *
 * Passive block — no block entity needed. Each tick, scans for
 * AreaEffectCloudEntity instances within its radius and removes them.
 * This protects players working with volatile/toxic reactions.
 *
 * Absorption radius: 5 blocks.
 */
public class FumeHoodBlock extends Block {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    private static final int ABSORPTION_RADIUS = 5;

    private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 16);

    public FumeHoodBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos,
                               Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        absorbNearbyClouds(world, pos);
    }

    /**
     * Called via random tick to periodically absorb gas clouds.
     */
    @Override
    public boolean hasRandomTicks(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, net.minecraft.server.world.ServerWorld world,
                           BlockPos pos, net.minecraft.util.math.random.Random random) {
        absorbNearbyClouds(world, pos);
    }

    private void absorbNearbyClouds(World world, BlockPos pos) {
        if (world.isClient) return;

        Box absorptionBox = new Box(pos).expand(ABSORPTION_RADIUS);
        List<AreaEffectCloudEntity> clouds = world.getEntitiesByClass(
                AreaEffectCloudEntity.class, absorptionBox, cloud -> true);

        for (AreaEffectCloudEntity cloud : clouds) {
            // Reduce cloud radius rapidly
            float newRadius = cloud.getRadius() - 0.5f;
            if (newRadius <= 0.2f) {
                cloud.discard();
            } else {
                cloud.setRadius(newRadius);
            }
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world,
                                      BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING,
                ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
