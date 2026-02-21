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
import org.keke.chemistry.entity.CrucibleBlockEntity;
import org.keke.chemistry.entity.ModBlockEntities;
import org.keke.chemistry.item.ModItems;

import java.util.stream.Stream;

/**
 * Crucible — a high-temperature resistant container for extreme heat reactions.
 * Made of ceramic/refractory material. Capacity: 200 mL.
 * Can withstand temperatures up to 1200 °C (thermite, molten metals).
 */
public class CrucibleBlock extends BlockWithEntity implements BlockEntityProvider {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Stream.of(
            Block.createCuboidShape(3, 0, 3, 13, 1, 13),   // base
            Block.createCuboidShape(3, 1, 3, 13, 7, 4),    // south wall
            Block.createCuboidShape(3, 1, 12, 13, 7, 13),  // north wall
            Block.createCuboidShape(3, 1, 4, 4, 7, 12),    // west wall
            Block.createCuboidShape(12, 1, 4, 13, 7, 12)   // east wall
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    public CrucibleBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.CRUCIBLE,
                (World w, BlockPos p, BlockState s, CrucibleBlockEntity entity) ->
                        CrucibleBlockEntity.tick(w, p, s, entity));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof CrucibleBlockEntity crucible) {
                ItemStack heldItem = player.getStackInHand(hand);
                if (player.isSneaking() && heldItem.isEmpty()) {
                    ItemStack drop = new ItemStack(ModItems.CRUCIBLE_ITEM);
                    if (!crucible.isEmpty()) {
                        NbtCompound entityNbt = crucible.getContentsNbt();
                        drop.getOrCreateNbt().copyFrom(entityNbt);
                    }
                    world.removeBlock(pos, false);
                    player.setStackInHand(hand, drop);
                    world.playSound(null, pos, SoundEvents.BLOCK_STONE_BREAK,
                            SoundCategory.BLOCKS, 0.5f, 1.0f);
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
            if (be instanceof CrucibleBlockEntity crucible) {
                ItemStack drop = new ItemStack(ModItems.CRUCIBLE_ITEM);
                if (!crucible.isEmpty()) {
                    NbtCompound entityNbt = crucible.getContentsNbt();
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
