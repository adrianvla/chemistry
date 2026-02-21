package org.keke.chemistry.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.keke.chemistry.entity.AbstractChemistryContainer;

/**
 * Wash Bottle — a squeezable bottle that dispenses distilled H₂O in small amounts.
 *
 * Holds a fixed reservoir of water. Right-click on a chemistry container (block entity)
 * to squirt a configurable amount of water into it.
 *
 * NBT:
 *   waterMl: remaining water in the bottle (starts at 500 mL)
 *
 * The wash bottle can be refilled by sneak+right-clicking a water source block.
 */
public class WashBottleItem extends Item {

    private static final double MAX_WATER_ML = 500.0;
    private static final double SQUIRT_ML = 5.0;  // ~0.28 mol H2O per squirt
    private static final double WATER_DENSITY = 1.0; // g/mL
    private static final double WATER_MOLAR_MASS = 18.015; // g/mol

    public WashBottleItem(Settings settings) {
        super(settings);
    }

    // ── Water level ────────────────────────────────────────────────────

    public static double getWaterMl(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains("waterMl")) return MAX_WATER_ML;
        return nbt.getDouble("waterMl");
    }

    public static void setWaterMl(ItemStack stack, double ml) {
        stack.getOrCreateNbt().putDouble("waterMl", Math.max(0, Math.min(MAX_WATER_ML, ml)));
    }

    // ── Use on container blocks ────────────────────────────────────────

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();

        if (world.isClient) return ActionResult.SUCCESS;

        double waterMl = getWaterMl(stack);
        if (waterMl < SQUIRT_ML) {
            if (player != null) {
                player.sendMessage(Text.literal("Wash bottle is empty").formatted(Formatting.RED), true);
            }
            return ActionResult.FAIL;
        }

        // Try to dispense water into a chemistry container
        var be = world.getBlockEntity(context.getBlockPos());
        if (be instanceof AbstractChemistryContainer container) {
            // Calculate moles of water to add
            double massG = SQUIRT_ML * WATER_DENSITY;
            double molesH2O = massG / WATER_MOLAR_MASS;

            container.addChemical("H2O", molesH2O);
            setWaterMl(stack, waterMl - SQUIRT_ML);

            world.playSound(null, context.getBlockPos(),
                    SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.PLAYERS,
                    0.5f, 1.5f);

            if (player != null) {
                player.sendMessage(Text.literal(String.format("Dispensed %.1f mL H₂O (%.0f mL remaining)",
                        SQUIRT_ML, waterMl - SQUIRT_ML)).formatted(Formatting.AQUA), true);
            }

            return ActionResult.SUCCESS;
        }

        // Sneak + right-click water-source block → refill
        if (player != null && player.isSneaking()) {
            var blockState = world.getBlockState(context.getBlockPos());
            if (blockState.getBlock() == net.minecraft.block.Blocks.WATER) {
                setWaterMl(stack, MAX_WATER_ML);
                world.playSound(null, context.getBlockPos(),
                        SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.PLAYERS,
                        1.0f, 1.0f);
                if (player != null) {
                    player.sendMessage(Text.literal("Wash bottle refilled").formatted(Formatting.GREEN), true);
                }
                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.PASS;
    }

    @Override
    public Text getName(ItemStack stack) {
        double waterMl = getWaterMl(stack);
        return Text.literal(String.format("Wash Bottle (%.0f mL)", waterMl));
    }
}
