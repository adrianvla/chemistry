package org.keke.chemistry.item;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.keke.chemistry.block.BeakerBase;
import org.keke.chemistry.block.ModBlocks;
import org.keke.chemistry.entity.BeakerLiquidBlockEntity;
import org.keke.chemistry.screen.PouringScreenHandler;
import org.keke.chemistry.utils.Compound;
import org.keke.chemistry.utils.BeakerSize;
import org.keke.chemistry.utils.CompoundPhysicalData;
import org.keke.chemistry.utils.DrinkingEffects;
import org.keke.chemistry.utils.FormulaUtils;
import org.keke.chemistry.utils.IUPACNamer;
import org.keke.chemistry.utils.StateOfMatter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BeakerItem — a placeable, drinkable item that stores multiple chemical contents in NBT.
 *
 * NBT format:
 *   contents: { "H2O": 1.0, "NaCl": 0.5, ... }
 *   temperatureK: 293.15
 *
 * Backward-compatible with old format: formula/moles top-level tags.
 *
 * Interactions:
 *   Right-click on placed beaker → pour contents
 *   Right-click on surface → place beaker
 *   Sneak + right-click (not on beaker) → drink contents
 */
public class BeakerItem extends Item {

    public BeakerItem(Settings settings) {
        super(settings);
    }

    // ── Contents helpers ───────────────────────────────────────────────

    /**
     * Read the multi-reactant contents map from item NBT.
     * Handles backward compat with old single-formula format.
     */
    public static Map<String, Double> getContents(ItemStack stack) {
        Map<String, Double> map = new LinkedHashMap<>();
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return map;

        if (nbt.contains("contents", NbtElement.COMPOUND_TYPE)) {
            NbtCompound contentsNbt = nbt.getCompound("contents");
            for (String key : contentsNbt.getKeys()) {
                double val = contentsNbt.getDouble(key);
                if (val > 0.001) map.put(key, val);
            }
        } else if (nbt.contains("formula")) {
            String formula = nbt.getString("formula");
            double moles = nbt.getDouble("moles");
            if (!formula.isEmpty() && moles > 0.001) map.put(formula, moles);
        }
        return map;
    }

    /**
     * Write the contents map to item NBT.
     */
    public static void setContents(ItemStack stack, Map<String, Double> contents) {
        NbtCompound nbt = stack.getOrCreateNbt();
        NbtCompound contentsNbt = new NbtCompound();
        for (var entry : contents.entrySet()) {
            if (entry.getValue() > 0.001) {
                contentsNbt.putDouble(entry.getKey(), entry.getValue());
            }
        }
        nbt.put("contents", contentsNbt);
        // Remove legacy keys
        nbt.remove("formula");
        nbt.remove("moles");
    }

    /**
     * Clear all contents from the item.
     */
    public static void clearContents(ItemStack stack) {
        setContents(stack, Map.of());
    }

    public static boolean hasContents(ItemStack stack) {
        return !getContents(stack).isEmpty();
    }

    /**
     * Get the beaker size (capacity) stored in NBT, defaulting to MEDIUM (250 mL).
     */
    public static BeakerSize getBeakerSize(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("maxCapacityMl")) {
            return BeakerSize.fromCapacity(nbt.getDouble("maxCapacityMl"));
        }
        return BeakerSize.MEDIUM;
    }

    /**
     * Set the beaker size by storing maxCapacityMl in NBT.
     */
    public static void setBeakerSize(ItemStack stack, BeakerSize size) {
        stack.getOrCreateNbt().putDouble("maxCapacityMl", size.getCapacityMl());
    }

    /**
     * Get the temperature in Kelvin stored in item NBT (defaults to ambient 293.15 K).
     */
    public static double getTemperatureK(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return (nbt != null && nbt.contains("temperatureK"))
                ? nbt.getDouble("temperatureK") : 293.15;
    }

    // ── Auto-naming ────────────────────────────────────────────────────

    @Override
    public Text getName(ItemStack stack) {
        Map<String, Double> contents = getContents(stack);
        BeakerSize size = getBeakerSize(stack);
        String sizeLabel = (size == BeakerSize.MEDIUM) ? "Beaker" : size.getDisplayName();
        if (contents.isEmpty()) {
            return Text.literal(sizeLabel);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(sizeLabel).append(" (");
        boolean first = true;
        for (String formula : contents.keySet()) {
            if (!first) sb.append(", ");
            String iupac = IUPACNamer.getName(formula);
            sb.append(iupac.equals(formula) ? FormulaUtils.formatFormula(formula) : iupac);
            first = false;
        }
        sb.append(")");
        return Text.literal(sb.toString());
    }

    // ── Drinking ───────────────────────────────────────────────────────

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return hasContents(stack) ? UseAction.DRINK : UseAction.NONE;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return hasContents(stack) ? 32 : 0;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (hasContents(stack) && user.isSneaking()) {
            user.setCurrentHand(hand);
            return TypedActionResult.consume(stack);
        }
        return TypedActionResult.pass(stack);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient) {
            Map<String, Double> contents = getContents(stack);
            if (!contents.isEmpty()) {
                DrinkingEffects.applyEffects(user, contents);

                // Apply temperature-based drinking effects
                double tempK = getTemperatureK(stack);
                double totalMoles = contents.values().stream()
                        .mapToDouble(Double::doubleValue).sum();
                DrinkingEffects.applyTemperatureEffects(user, tempK, totalMoles);

                clearContents(stack);
                world.playSound(null, user.getBlockPos(),
                        SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS,
                        1.0f, 1.0f);
            }
        }
        return stack;
    }

    // ── Block interaction ──────────────────────────────────────────────

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState targetState = world.getBlockState(pos);
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();

        // If clicking on a placed beaker with a filled beaker, open pouring GUI
        if (targetState.getBlock() instanceof BeakerBase) {
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof BeakerLiquidBlockEntity) {
                    Map<String, Double> itemContents = getContents(stack);
                    if (!itemContents.isEmpty()) {
                        // Open the pouring screen
                        serverPlayer.openHandledScreen(new net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory() {
                            @Override
                            public Text getDisplayName() {
                                return Text.literal("Pour Beaker");
                            }

                            @Override
                            public ScreenHandler createMenu(int syncId,
                                    net.minecraft.entity.player.PlayerInventory inventory,
                                    PlayerEntity p) {
                                return new PouringScreenHandler(syncId, inventory,
                                        ScreenHandlerContext.create(world, pos), pos);
                            }

                            @Override
                            public void writeScreenOpeningData(ServerPlayerEntity p, PacketByteBuf buf) {
                                buf.writeBlockPos(pos);
                            }
                        });
                    }
                }
            }
            return ActionResult.success(world.isClient);
        }

        // If sneaking on non-beaker → pass through to use() for drinking
        if (player != null && player.isSneaking() && hasContents(stack)) {
            return ActionResult.PASS;
        }

        // Place a new beaker block
        BlockPos placePos = pos.offset(context.getSide());
        if (world.getBlockState(placePos).isReplaceable()) {
            if (!world.isClient) {
                BlockState beakerState = ModBlocks.BEAKER_BASE.getDefaultState()
                        .with(BeakerBase.FACING, player != null
                                ? player.getHorizontalFacing().getOpposite()
                                : context.getHorizontalPlayerFacing().getOpposite());
                world.setBlockState(placePos, beakerState);
                world.playSound(null, placePos, SoundEvents.BLOCK_GLASS_PLACE,
                        SoundCategory.BLOCKS, 1.0f, 1.0f);

                // Transfer item contents, temperature, and capacity to placed block entity
                BlockEntity be = world.getBlockEntity(placePos);
                if (be instanceof BeakerLiquidBlockEntity beakerEntity) {
                    // Set capacity before contents so volume checking uses correct limit
                    BeakerSize size = getBeakerSize(stack);
                    beakerEntity.setMaxCapacityMl(size.getCapacityMl());

                    Map<String, Double> contents = getContents(stack);
                    if (!contents.isEmpty()) {
                        beakerEntity.setContents(contents);
                    }
                    NbtCompound itemNbt = stack.getNbt();
                    if (itemNbt != null && itemNbt.contains("temperatureK")) {
                        beakerEntity.setTemperatureK(itemNbt.getDouble("temperatureK"));
                    }
                }

                if (player != null && !player.isCreative()) {
                    stack.decrement(1);
                }
            }
            return ActionResult.success(world.isClient);
        }

        return ActionResult.PASS;
    }

    // ── Tooltips ───────────────────────────────────────────────────────

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        Map<String, Double> contents = getContents(stack);

        if (contents.isEmpty()) {
            tooltip.add(Text.literal("Empty").formatted(Formatting.GRAY));
        } else {
            tooltip.add(Text.literal("Contents:").formatted(Formatting.AQUA));
            double totalMassG = 0;
            double totalVolumeMl = 0;
            NbtCompound itemNbt = stack.getNbt();
            double tempK = (itemNbt != null && itemNbt.contains("temperatureK"))
                    ? itemNbt.getDouble("temperatureK") : 293.15;
            for (var entry : contents.entrySet()) {
                String formatted = FormulaUtils.formatFormula(entry.getKey());
                double moles = entry.getValue();
                double molarMass = Compound.computeMolarMass(entry.getKey());
                double massG = moles * molarMass;
                double density = CompoundPhysicalData.getDensity(entry.getKey(), tempK);
                if (density <= 0) density = 1.0;
                double volumeMl = massG / density;
                totalMassG += massG;
                totalVolumeMl += volumeMl;
                tooltip.add(Text.literal(String.format("  %s — %.2f mol, %.2f g, %.2f mL",
                        formatted, moles, massG, volumeMl)).formatted(Formatting.YELLOW));
                String iupacName = IUPACNamer.getName(entry.getKey());
                StateOfMatter state = CompoundPhysicalData.getStateAtTemperature(entry.getKey(), tempK);
                String stateTag = " [" + state.getDisplayName() + "]";
                if (!iupacName.equals(entry.getKey())) {
                    tooltip.add(Text.literal("    " + iupacName + stateTag).formatted(Formatting.GRAY));
                } else {
                    tooltip.add(Text.literal("    " + stateTag.trim()).formatted(Formatting.GRAY));
                }
            }
            double maxCapMl = (itemNbt != null && itemNbt.contains("maxCapacityMl"))
                    ? itemNbt.getDouble("maxCapacityMl") : 250.0;
            tooltip.add(Text.literal(String.format("Total: %.2f g / %.2f mL (%.0f mL beaker)",
                    totalMassG, totalVolumeMl, maxCapMl))
                    .formatted(Formatting.GOLD));

            NbtCompound nbt = stack.getNbt();
            if (nbt != null && nbt.contains("temperatureK")) {
                double tempC = nbt.getDouble("temperatureK") - 273.15;
                Formatting tempColor = tempC > 50 ? Formatting.RED
                        : tempC < 10 ? Formatting.BLUE : Formatting.GRAY;
                tooltip.add(Text.literal(String.format("Temp: %.1f °C", tempC))
                        .formatted(tempColor));
            }

            tooltip.add(Text.literal("Sneak + Use to drink").formatted(Formatting.DARK_GRAY));
        }
    }
}
