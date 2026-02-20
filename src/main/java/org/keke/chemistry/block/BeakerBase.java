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
import org.keke.chemistry.entity.BeakerLiquidBlockEntity;
import org.keke.chemistry.entity.ModBlockEntities;
import org.keke.chemistry.item.ModItems;

import java.util.stream.Stream;

public class BeakerBase extends BlockWithEntity implements BlockEntityProvider {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public BeakerBase(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BeakerLiquidBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.BEAKER_LIQUID, (World world1, BlockPos pos, BlockState state1, BeakerLiquidBlockEntity be) -> BeakerLiquidBlockEntity.tick(world1, pos, state1, be));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof BeakerLiquidBlockEntity beakerEntity) {
                ItemStack heldItem = player.getStackInHand(hand);

                // Shift + empty hand: pick up the beaker
                if (player.isSneaking() && heldItem.isEmpty()) {
                    ItemStack beakerStack = new ItemStack(ModItems.BEAKER);
                    if (!beakerEntity.isEmpty()) {
                        NbtCompound entityNbt = beakerEntity.getContentsNbt();
                        beakerStack.getOrCreateNbt().copyFrom(entityNbt);
                    } else {
                        // Preserve beaker size even when empty
                        beakerStack.getOrCreateNbt().putDouble("maxCapacityMl", beakerEntity.getMaxCapacityMl());
                    }
                    world.removeBlock(pos, false);
                    player.setStackInHand(hand, beakerStack);
                    world.playSound(null, pos, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 0.5f, 1.2f);
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof BeakerLiquidBlockEntity beakerEntity) {
                ItemStack drop = new ItemStack(ModItems.BEAKER);
                if (!beakerEntity.isEmpty()) {
                    NbtCompound entityNbt = beakerEntity.getContentsNbt();
                    drop.getOrCreateNbt().copyFrom(entityNbt);
                } else {
                    // Preserve beaker size even when empty
                    drop.getOrCreateNbt().putDouble("maxCapacityMl", beakerEntity.getMaxCapacityMl());
                }
                dropStack(world, pos, drop);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
    private static final VoxelShape SHAPE_N = Stream.of(
            Block.createCuboidShape(5, 1, 6, 6, 7, 10),
            Block.createCuboidShape(6, 1, 5, 10, 6, 6),
            Block.createCuboidShape(10, 1, 6, 11, 7, 10),
            Block.createCuboidShape(6, 1, 10, 10, 7, 11),
            Block.createCuboidShape(6, 0, 6, 10, 1, 10),
            Block.createCuboidShape(8.5, 6, 5, 10, 7, 6),
            Block.createCuboidShape(6, 6, 5, 7.5, 7, 6),
            Block.createCuboidShape(7.5, 6, 4, 8.5, 7, 5)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape SHAPE_E = Stream.of(
            Block.createCuboidShape(6, 1, 5, 10, 7, 6),
            Block.createCuboidShape(10, 1, 6, 11, 6, 10),
            Block.createCuboidShape(6, 1, 10, 10, 7, 11),
            Block.createCuboidShape(5, 1, 6, 6, 7, 10),
            Block.createCuboidShape(6, 0, 6, 10, 1, 10),
            Block.createCuboidShape(10, 6, 8.5, 11, 7, 10),
            Block.createCuboidShape(10, 6, 6, 11, 7, 7.5),
            Block.createCuboidShape(11, 6, 7.5, 12, 7, 8.5)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    private static final VoxelShape SHAPE_S = Stream.of(
            Block.createCuboidShape(10, 1, 6, 11, 7, 10),
            Block.createCuboidShape(6, 1, 10, 10, 6, 11),
            Block.createCuboidShape(5, 1, 6, 6, 7, 10),
            Block.createCuboidShape(6, 1, 5, 10, 7, 6),
            Block.createCuboidShape(6, 0, 6, 10, 1, 10),
            Block.createCuboidShape(6, 6, 10, 7.5, 7, 11),
            Block.createCuboidShape(8.5, 6, 10, 10, 7, 11),
            Block.createCuboidShape(7.5, 6, 11, 8.5, 7, 12)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();


    private static final VoxelShape SHAPE_W = Stream.of(
            Block.createCuboidShape(6, 1, 10, 10, 7, 11),
            Block.createCuboidShape(5, 1, 6, 6, 6, 10),
            Block.createCuboidShape(6, 1, 5, 10, 7, 6),
            Block.createCuboidShape(10, 1, 6, 11, 7, 10),
            Block.createCuboidShape(6, 0, 6, 10, 1, 10),
            Block.createCuboidShape(5, 6, 6, 6, 7, 7.5),
            Block.createCuboidShape(5, 6, 8.5, 6, 7, 10),
            Block.createCuboidShape(4, 6, 7.5, 5, 7, 8.5)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        switch (state.get(FACING)){
            case NORTH:
                return SHAPE_N;
            case EAST:
                return SHAPE_E;
            case SOUTH:
                return SHAPE_S;
            case WEST:
                return SHAPE_W;
            default:
                return SHAPE_N;
        }
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING,ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING,rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }


    //    public static final Block BEAKER_BLOCK  = registerBlock("beaker_block",new Block(FabricBlockSettings.create().strength(0.5f)));
}
