package org.keke.chemistry.block;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.keke.chemistry.entity.GasCollectorBlockEntity;
import org.keke.chemistry.entity.ModBlockEntities;
import org.keke.chemistry.item.ModItems;

import java.util.stream.Stream;

/**
 * Gas Collector — captures gases produced by the container below it.
 * Place above a beaker, test tube, or other chemistry container.
 * Collected gases are stored and can be extracted as items.
 * Capacity: 500 mL (gas volume at STP).
 */
public class GasCollectorBlock extends BlockWithEntity implements BlockEntityProvider {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Stream.of(
            Block.createCuboidShape(2, 0, 2, 14, 2, 14),    // funnel bottom
            Block.createCuboidShape(3, 2, 3, 13, 10, 13),   // collection chamber
            Block.createCuboidShape(4, 10, 4, 12, 14, 12)   // top cap
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    public GasCollectorBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GasCollectorBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.GAS_COLLECTOR,
                (World w, BlockPos p, BlockState s, GasCollectorBlockEntity entity) ->
                        GasCollectorBlockEntity.tick(w, p, s, entity));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof GasCollectorBlockEntity collector) {
                ItemStack heldItem = player.getStackInHand(hand);
                if (player.isSneaking() && heldItem.isEmpty()) {
                    ItemStack drop = new ItemStack(ModItems.GAS_COLLECTOR_ITEM);
                    if (!collector.isEmpty()) {
                        NbtCompound entityNbt = collector.getContentsNbt();
                        drop.getOrCreateNbt().copyFrom(entityNbt);
                    }
                    world.removeBlock(pos, false);
                    player.setStackInHand(hand, drop);
                    world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_CLOSE,
                            SoundCategory.BLOCKS, 0.5f, 1.5f);
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos,
                                BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof GasCollectorBlockEntity collector) {
                ItemStack drop = new ItemStack(ModItems.GAS_COLLECTOR_ITEM);
                if (!collector.isEmpty()) {
                    NbtCompound entityNbt = collector.getContentsNbt();
                    drop.getOrCreateNbt().copyFrom(entityNbt);
                }
                dropStack(world, pos, drop);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
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
