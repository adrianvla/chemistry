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
import org.keke.chemistry.entity.AmpuleBlockEntity;
import org.keke.chemistry.entity.ModBlockEntities;
import org.keke.chemistry.item.AmpuleItem;
import org.keke.chemistry.item.ModItems;
import org.keke.chemistry.item.PliersItem;

import java.util.stream.Stream;

/**
 * Ampule — a small sealed glass container placed as a block.
 * Holds 10 mL. Can be opened with Pliers.
 * Drops as item when shift+clicked with empty hand.
 */
public class AmpuleBlock extends BlockWithEntity implements BlockEntityProvider {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Stream.of(
            Block.createCuboidShape(6, 0, 6, 10, 1, 10),    // base
            Block.createCuboidShape(6.5, 1, 6.5, 9.5, 8, 9.5),  // body
            Block.createCuboidShape(7, 8, 7, 9, 10, 9)      // neck
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    public AmpuleBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AmpuleBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.AMPULE,
                (World w, BlockPos p, BlockState s, AmpuleBlockEntity entity) ->
                        AmpuleBlockEntity.tick(w, p, s, entity));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof AmpuleBlockEntity ampule) {
                ItemStack heldItem = player.getStackInHand(hand);

                // Open sealed ampule with pliers
                if (heldItem.getItem() instanceof PliersItem && ampule.isSealed()) {
                    ampule.setSealed(false);
                    heldItem.damage(1, player, (entity) ->
                            entity.sendToolBreakStatus(hand));
                    world.playSound(null, pos, SoundEvents.BLOCK_GLASS_BREAK,
                            SoundCategory.BLOCKS, 0.5f, 1.8f);
                    return ActionResult.SUCCESS;
                }

                // Shift + empty hand: pick up
                if (player.isSneaking() && heldItem.isEmpty()) {
                    ItemStack drop = new ItemStack(ModItems.AMPULE);
                    if (!ampule.isEmpty()) {
                        NbtCompound entityNbt = ampule.getContentsNbt();
                        drop.getOrCreateNbt().copyFrom(entityNbt);
                    }
                    AmpuleItem.setSealed(drop, ampule.isSealed());
                    world.removeBlock(pos, false);
                    player.setStackInHand(hand, drop);
                    world.playSound(null, pos, SoundEvents.BLOCK_GLASS_BREAK,
                            SoundCategory.BLOCKS, 0.3f, 1.6f);
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
            if (be instanceof AmpuleBlockEntity ampule) {
                ItemStack drop = new ItemStack(ModItems.AMPULE);
                if (!ampule.isEmpty()) {
                    NbtCompound entityNbt = ampule.getContentsNbt();
                    drop.getOrCreateNbt().copyFrom(entityNbt);
                }
                AmpuleItem.setSealed(drop, ampule.isSealed());
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
