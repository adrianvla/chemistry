package org.keke.chemistry.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.keke.chemistry.utils.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for all chemistry container items (test tube, ampule, bottle).
 * Shares the NBT contents format with BeakerItem for interoperability.
 *
 * NBT format:
 *   contents: { "H2O": 1.0, "NaCl": 0.5, ... }
 *   temperatureK: 293.15
 *   containerType: "TEST_TUBE" | "AMPULE" | "BOTTLE"
 *   sealed: true/false (ampules only)
 */
public abstract class AbstractContainerItem extends Item {

    protected final ContainerType containerType;

    protected AbstractContainerItem(Settings settings, ContainerType containerType) {
        super(settings);
        this.containerType = containerType;
    }

    public ContainerType getContainerType() {
        return containerType;
    }

    // ── Contents helpers (shared with BeakerItem format) ───────────────

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
        }
        return map;
    }

    public static void setContents(ItemStack stack, Map<String, Double> contents) {
        NbtCompound nbt = stack.getOrCreateNbt();
        NbtCompound contentsNbt = new NbtCompound();
        for (var entry : contents.entrySet()) {
            if (entry.getValue() > 0.001) {
                contentsNbt.putDouble(entry.getKey(), entry.getValue());
            }
        }
        nbt.put("contents", contentsNbt);
    }

    public static void clearContents(ItemStack stack) {
        setContents(stack, Map.of());
    }

    public static boolean hasContents(ItemStack stack) {
        return !getContents(stack).isEmpty();
    }

    public static double getTemperatureK(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return (nbt != null && nbt.contains("temperatureK"))
                ? nbt.getDouble("temperatureK") : 293.15;
    }

    public static void setTemperatureK(ItemStack stack, double tempK) {
        stack.getOrCreateNbt().putDouble("temperatureK", tempK);
    }

    // ── Display name ───────────────────────────────────────────────────

    @Override
    public Text getName(ItemStack stack) {
        Map<String, Double> contents = getContents(stack);
        String baseName = containerType.getDisplayName();

        if (contents.isEmpty()) {
            return Text.literal(baseName);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(baseName).append(" (");
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

    // ── Tooltip ────────────────────────────────────────────────────────

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        Map<String, Double> contents = getContents(stack);

        tooltip.add(Text.literal(String.format("Capacity: %.0f mL", containerType.getDefaultCapacityMl()))
                .formatted(Formatting.DARK_GRAY));

        if (contents.isEmpty()) {
            tooltip.add(Text.literal("Empty").formatted(Formatting.GRAY));
        } else {
            tooltip.add(Text.literal("Contents:").formatted(Formatting.AQUA));
            double totalMassG = 0;
            double totalVolumeMl = 0;
            double tempK = getTemperatureK(stack);

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
                tooltip.add(Text.literal(String.format("  %s — %.2f mol, %.2f g",
                        formatted, moles, massG)).formatted(Formatting.YELLOW));

                String iupacName = IUPACNamer.getName(entry.getKey());
                StateOfMatter state = CompoundPhysicalData.getStateAtTemperature(entry.getKey(), tempK);
                String stateTag = " [" + state.getDisplayName() + "]";
                if (!iupacName.equals(entry.getKey())) {
                    tooltip.add(Text.literal("    " + iupacName + stateTag).formatted(Formatting.GRAY));
                } else {
                    tooltip.add(Text.literal("    " + stateTag.trim()).formatted(Formatting.GRAY));
                }
            }
            tooltip.add(Text.literal(String.format("Total: %.2f g / %.2f mL",
                    totalMassG, totalVolumeMl)).formatted(Formatting.GOLD));

            double tempC = getTemperatureK(stack) - 273.15;
            Formatting tempColor = tempC > 50 ? Formatting.RED
                    : tempC < 10 ? Formatting.BLUE : Formatting.GRAY;
            tooltip.add(Text.literal(String.format("Temp: %.1f °C", tempC))
                    .formatted(tempColor));
        }
    }

    // ── Drinking (only non-sealed containers) ──────────────────────────

    @Override
    public UseAction getUseAction(ItemStack stack) {
        if (!hasContents(stack)) return UseAction.NONE;
        if (!canDrinkFrom(stack)) return UseAction.NONE;
        return UseAction.DRINK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return (hasContents(stack) && canDrinkFrom(stack)) ? 32 : 0;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (hasContents(stack) && user.isSneaking() && canDrinkFrom(stack)) {
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

    /**
     * Whether the container can be drunk from in its current state.
     * Sealed ampules cannot be drunk from.
     */
    protected boolean canDrinkFrom(ItemStack stack) {
        return true;
    }
}
