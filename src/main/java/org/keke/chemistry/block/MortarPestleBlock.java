package org.keke.chemistry.block;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.keke.chemistry.entity.AbstractChemistryContainer;
import org.keke.chemistry.item.AbstractContainerItem;
import org.keke.chemistry.item.BeakerItem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Mortar & Pestle — grinds solid compounds into powder form.
 *
 * Right-click with a container holding solid compounds to grind them.
 * This converts chunks of solid reagent into finer powder form
 * (represented by doubling the effective surface area / reaction rate).
 *
 * Gameplay: solids that have been ground react faster in containers.
 * Implementation: adds "ground_" prefix to NBT contents for affected solids.
 */
public class MortarPestleBlock extends Block {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Stream.of(
            Block.createCuboidShape(3, 0, 3, 13, 4, 13),   // mortar bowl
            Block.createCuboidShape(6, 4, 6, 10, 8, 10)    // pestle
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

    public MortarPestleBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        ItemStack heldItem = player.getStackInHand(hand);

        // Accept beakers and container items
        Map<String, Double> contents = null;
        if (heldItem.getItem() instanceof BeakerItem) {
            contents = BeakerItem.getContents(heldItem);
        } else if (heldItem.getItem() instanceof AbstractContainerItem) {
            contents = AbstractContainerItem.getContents(heldItem);
        }

        if (contents != null && !contents.isEmpty()) {
            Map<String, Double> ground = new LinkedHashMap<>();
            boolean anyGround = false;
            for (var entry : contents.entrySet()) {
                String formula = entry.getKey();
                double moles = entry.getValue();
                // Only grind non-ground solids (no gas/liquid formulas)
                if (!formula.startsWith("ground_") && isSolidCompound(formula)) {
                    ground.put("ground_" + formula, moles);
                    anyGround = true;
                } else {
                    ground.put(formula, moles);
                }
            }

            if (anyGround) {
                if (heldItem.getItem() instanceof BeakerItem) {
                    BeakerItem.setContents(heldItem, ground);
                } else if (heldItem.getItem() instanceof AbstractContainerItem) {
                    AbstractContainerItem.setContents(heldItem, ground);
                }
                world.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE,
                        SoundCategory.BLOCKS, 0.8f, 1.2f);
                player.sendMessage(Text.literal("Ground to powder").formatted(Formatting.GREEN), true);
                return ActionResult.SUCCESS;
            } else {
                player.sendMessage(Text.literal("Nothing to grind").formatted(Formatting.GRAY), true);
            }
        }

        return ActionResult.PASS;
    }

    /**
     * Check whether a formula represents a compound that's solid at room temperature.
     * Uses elemental + structural analysis rather than hardcoded lists.
     */
    private boolean isSolidCompound(String formula) {
        // Most salts, metals, and oxides are solid
        // Gases/liquids at RT: H2O, HCl, H2SO4, etc. are excluded
        org.keke.chemistry.utils.StateOfMatter som =
                org.keke.chemistry.utils.CompoundPhysicalData.getStateAtTemperature(formula, 293.15);
        return som == org.keke.chemistry.utils.StateOfMatter.SOLID;
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
