package org.keke.chemistry.block;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.keke.chemistry.entity.EvaporationDishBlockEntity;
import org.keke.chemistry.entity.ModBlockEntities;
import org.keke.chemistry.item.BeakerItem;
import org.keke.chemistry.item.ModItems;

/**
 * Evaporation dish — slowly evaporates solvent (water), leaving dissolved solid behind.
 */
public class EvaporationDishBlock extends BlockWithEntity implements BlockEntityProvider {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 4, 14);

    public EvaporationDishBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EvaporationDishBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.EVAPORATION_DISH,
                (World w, BlockPos p, BlockState s, EvaporationDishBlockEntity be) -> EvaporationDishBlockEntity.tick(w, p, s, be));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof EvaporationDishBlockEntity dish) {
                ItemStack heldItem = player.getStackInHand(hand);

                if (heldItem.getItem() instanceof BeakerItem) {
                    java.util.Map<String, Double> contents = BeakerItem.getContents(heldItem);

                    if (!contents.isEmpty() && dish.isEmpty()) {
                        var entry = contents.entrySet().iterator().next();
                        dish.setInput(entry.getKey(), entry.getValue());
                        BeakerItem.clearContents(heldItem);
                        world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0f, 1.0f);
                        return ActionResult.SUCCESS;
                    }
                } else if (heldItem.isEmpty() && player.isSneaking()) {
                    String residue = dish.getResidueFormula();
                    double residueMoles = dish.getResidueMoles();
                    if (!residue.isEmpty() && residueMoles > 0) {
                        ItemStack beaker = ModItems.createFilledBeaker(residue, residueMoles);
                        player.setStackInHand(hand, beaker);
                        dish.clearResidue();
                        world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
                        return ActionResult.SUCCESS;
                    }
                } else if (heldItem.isEmpty()) {
                    dish.reportStatus(player);
                }
            }
        }
        return ActionResult.success(world.isClient);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
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
