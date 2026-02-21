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
import org.keke.chemistry.entity.CondenserBlockEntity;
import org.keke.chemistry.entity.ModBlockEntities;
import org.keke.chemistry.item.ModItems;

import java.util.stream.Stream;

/**
 * Condenser — cools gas-phase compounds back to liquid.
 * Place next to or above a heated container.
 * Gases that enter the condenser are cooled and collected as liquids.
 */
public class CondenserBlock extends BlockWithEntity implements BlockEntityProvider {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Stream.of(
            Block.createCuboidShape(4, 0, 4, 12, 2, 12),    // base
            Block.createCuboidShape(5, 2, 5, 11, 14, 11),   // column
            Block.createCuboidShape(3, 4, 6, 5, 12, 10),    // cooling jacket left
            Block.createCuboidShape(11, 4, 6, 13, 12, 10)   // cooling jacket right
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    public CondenserBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CondenserBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.CONDENSER,
                (World w, BlockPos p, BlockState s, CondenserBlockEntity entity) ->
                        CondenserBlockEntity.tick(w, p, s, entity));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof CondenserBlockEntity condenser) {
                ItemStack heldItem = player.getStackInHand(hand);
                if (player.isSneaking() && heldItem.isEmpty()) {
                    ItemStack drop = new ItemStack(ModItems.CONDENSER_ITEM);
                    if (!condenser.isEmpty()) {
                        NbtCompound entityNbt = condenser.getContentsNbt();
                        drop.getOrCreateNbt().copyFrom(entityNbt);
                    }
                    world.removeBlock(pos, false);
                    player.setStackInHand(hand, drop);
                    world.playSound(null, pos, SoundEvents.BLOCK_GLASS_BREAK,
                            SoundCategory.BLOCKS, 0.3f, 1.3f);
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
            if (be instanceof CondenserBlockEntity condenser) {
                ItemStack drop = new ItemStack(ModItems.CONDENSER_ITEM);
                if (!condenser.isEmpty()) {
                    NbtCompound entityNbt = condenser.getContentsNbt();
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
