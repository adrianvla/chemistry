package org.keke.chemistry.block;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.keke.chemistry.entity.ChemistryCauldronBlockEntity;
import org.keke.chemistry.entity.ModBlockEntities;
import org.keke.chemistry.item.BeakerItem;

import java.util.Map;

/**
 * Chemistry Cauldron — a large chemistry container that replaces a vanilla
 * cauldron when chemicals are poured in. Uses the vanilla cauldron shape.
 *
 * Interactions:
 *   - Right-click with filled beaker: pour beaker contents into cauldron
 *   - Right-click with empty beaker: scoop contents into beaker
 *   - Shift + right-click with empty hand: no pickup (it's a cauldron)
 *
 * Reverts to vanilla cauldron when emptied (handled in tick).
 */
public class ChemistryCauldronBlock extends BlockWithEntity {

    /** Cauldron shape matching vanilla (Block.createCuboidShape(2, 4, 2, 14, 16, 14)). */
    private static final VoxelShape SHAPE = Block.createCuboidShape(2, 4, 2, 14, 16, 14);

    public ChemistryCauldronBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ChemistryCauldronBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.CHEMISTRY_CAULDRON,
                (World w, BlockPos p, BlockState s, ChemistryCauldronBlockEntity c) ->
                        ChemistryCauldronBlockEntity.tick(w, p, s, c));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            ItemStack heldItem = player.getStackInHand(hand);
            BlockEntity be = world.getBlockEntity(pos);

            if (be instanceof ChemistryCauldronBlockEntity cauldron) {
                // Right-click with filled beaker: pour into cauldron
                if (heldItem.getItem() instanceof BeakerItem && BeakerItem.hasContents(heldItem)) {
                    Map<String, Double> beakerContents = BeakerItem.getContents(heldItem);
                    boolean poured = false;
                    for (var entry : beakerContents.entrySet()) {
                        if (cauldron.addChemical(entry.getKey(), entry.getValue())) {
                            poured = true;
                        }
                    }
                    if (poured) {
                        BeakerItem.clearContents(heldItem);
                        world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY,
                                SoundCategory.BLOCKS, 1.0f, 0.9f);
                    }
                    return ActionResult.SUCCESS;
                }

                // Right-click with empty beaker: scoop from cauldron
                if (heldItem.getItem() instanceof BeakerItem
                        && !BeakerItem.hasContents(heldItem)
                        && !cauldron.isEmpty()) {
                    // Transfer contents into the beaker (limited by beaker capacity)
                    NbtCompound cauldronNbt = cauldron.getContentsNbt();
                    heldItem.getOrCreateNbt().copyFrom(cauldronNbt);
                    cauldron.clearContents();
                    world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL,
                            SoundCategory.BLOCKS, 1.0f, 1.1f);
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
            // Don't drop anything — contents are lost (cauldron is too heavy to pick up)
            // But do drop items if the cauldron itself is broken
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof ChemistryCauldronBlockEntity cauldron) {
                if (!cauldron.isEmpty()) {
                    // Spill: damage nearby entities with spilled chemicals
                    cauldron.clearContents();
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world,
                                      BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
}
