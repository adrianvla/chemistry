package org.keke.chemistry.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.keke.chemistry.Chemistry;
import org.keke.chemistry.block.CompositionBlock;
import org.keke.chemistry.block.PipetteBlock;
import org.keke.chemistry.entity.BeakerLiquidBlockEntity;
import org.keke.chemistry.item.BeakerItem;
import org.keke.chemistry.item.ModItems;
import org.keke.chemistry.utils.Compound;
import org.keke.chemistry.utils.FormulaParser;

/**
 * Server-side network handler registration for composition and pipette actions.
 */
public class ModNetworking {

    public static final Identifier COMPOSITION_ADD = new Identifier(Chemistry.MOD_ID, "composition_add");
    public static final Identifier PIPETTE_EXTRACT = new Identifier(Chemistry.MOD_ID, "pipette_extract");
    public static final Identifier BEAKER_POUR = new Identifier(Chemistry.MOD_ID, "beaker_pour");

    public static void registerServerReceivers() {
        // Composition block: add chemical to beaker above
        ServerPlayNetworking.registerGlobalReceiver(COMPOSITION_ADD, (server, player, handler, buf, responseSender) -> {
            BlockPos blockPos = buf.readBlockPos();
            String formula = buf.readString(256);
            double amount = buf.readDouble();
            boolean isGrams = buf.readBoolean();

            server.execute(() -> {
                if (!player.isCreative()) return;
                if (player.getWorld() == null) return;

                // Validate the block is a composition block
                if (!(player.getWorld().getBlockState(blockPos).getBlock() instanceof CompositionBlock)) return;

                // Validate formula is parseable
                try {
                    var parsed = FormulaParser.parse(formula);
                    if (parsed.isEmpty()) return;
                } catch (Exception e) {
                    return;
                }

                // Find beaker above
                BeakerLiquidBlockEntity beaker = CompositionBlock.findBeakerAbove(player.getWorld(), blockPos);
                if (beaker == null) {
                    player.sendMessage(net.minecraft.text.Text.literal("No beaker found above the composer."), true);
                    return;
                }

                // Convert grams to moles if needed
                double moles = amount;
                if (isGrams) {
                    double molarMass = Compound.computeMolarMass(formula);
                    if (molarMass <= 0) return;
                    moles = amount / molarMass;
                }

                if (moles <= 0) return;

                // Add to beaker
                boolean added = beaker.addChemical(formula, moles);
                if (added) {
                    player.sendMessage(net.minecraft.text.Text.literal(
                            String.format("Added %.3f mol of %s", moles, formula)), true);
                } else {
                    player.sendMessage(net.minecraft.text.Text.literal("Beaker is full."), true);
                }
            });
        });

        // Pipette block: extract chemical from beaker above
        ServerPlayNetworking.registerGlobalReceiver(PIPETTE_EXTRACT, (server, player, handler, buf, responseSender) -> {
            BlockPos blockPos = buf.readBlockPos();
            String formula = buf.readString(256);
            double amount = buf.readDouble();
            boolean isGrams = buf.readBoolean();

            server.execute(() -> {
                if (!player.isCreative()) return;
                if (player.getWorld() == null) return;

                // Validate the block is a pipette block
                if (!(player.getWorld().getBlockState(blockPos).getBlock() instanceof PipetteBlock)) return;

                // Find beaker above
                BeakerLiquidBlockEntity beaker = PipetteBlock.findBeakerAbove(player.getWorld(), blockPos);
                if (beaker == null) {
                    player.sendMessage(net.minecraft.text.Text.literal("No beaker found above the pipette."), true);
                    return;
                }

                var contents = beaker.getContents();
                if (!contents.containsKey(formula)) {
                    player.sendMessage(net.minecraft.text.Text.literal(
                            formula + " not found in beaker."), true);
                    return;
                }

                double currentMoles = contents.get(formula);

                // Convert grams to moles if needed
                double molesToRemove = amount;
                if (isGrams) {
                    double molarMass = Compound.computeMolarMass(formula);
                    if (molarMass <= 0) return;
                    molesToRemove = amount / molarMass;
                }

                if (molesToRemove <= 0) return;

                double actualRemoved = Math.min(molesToRemove, currentMoles);
                beaker.removeChemical(formula, actualRemoved);

                player.sendMessage(net.minecraft.text.Text.literal(
                        String.format("Removed %.3f mol of %s", actualRemoved, formula)), true);
            });
        });

        // Beaker pouring: transfer chemicals from held beaker item to placed beaker
        ServerPlayNetworking.registerGlobalReceiver(BEAKER_POUR, (server, player, handler, buf, responseSender) -> {
            BlockPos blockPos = buf.readBlockPos();
            String formula = buf.readString(256);
            double amount = buf.readDouble();
            boolean pourAll = buf.readBoolean();

            server.execute(() -> {
                if (player.getWorld() == null) return;

                // Validate target is a beaker block entity
                var be = player.getWorld().getBlockEntity(blockPos);
                if (!(be instanceof BeakerLiquidBlockEntity target)) return;

                // Get source contents from held item
                var heldStack = player.getMainHandStack();
                if (!(heldStack.getItem() instanceof BeakerItem)) return;

                var sourceContents = BeakerItem.getContents(heldStack);
                if (sourceContents.isEmpty()) {
                    player.sendMessage(net.minecraft.text.Text.literal("Source beaker is empty."), true);
                    return;
                }

                if (pourAll) {
                    // Pour all compounds from source to target
                    boolean anyPoured = false;
                    java.util.Map<String, Double> remaining = new java.util.LinkedHashMap<>(sourceContents);
                    for (var entry : sourceContents.entrySet()) {
                        if (target.addChemical(entry.getKey(), entry.getValue())) {
                            remaining.remove(entry.getKey());
                            anyPoured = true;
                        }
                    }
                    if (anyPoured) {
                        BeakerItem.setContents(heldStack, remaining);
                        player.sendMessage(net.minecraft.text.Text.literal("Poured contents into beaker."), true);
                    } else {
                        player.sendMessage(net.minecraft.text.Text.literal("Target beaker is full."), true);
                    }
                } else {
                    // Pour specific compound
                    if (!sourceContents.containsKey(formula)) {
                        player.sendMessage(net.minecraft.text.Text.literal(
                                formula + " not found in source beaker."), true);
                        return;
                    }

                    double available = sourceContents.get(formula);
                    double toPour = (amount <= 0) ? available : Math.min(amount, available);

                    if (target.addChemical(formula, toPour)) {
                        // Remove poured amount from source item
                        double leftover = available - toPour;
                        java.util.Map<String, Double> updated = new java.util.LinkedHashMap<>(sourceContents);
                        if (leftover <= 0.001) {
                            updated.remove(formula);
                        } else {
                            updated.put(formula, leftover);
                        }
                        BeakerItem.setContents(heldStack, updated);
                        player.sendMessage(net.minecraft.text.Text.literal(
                                String.format("Poured %.3f mol of %s", toPour, formula)), true);
                    } else {
                        player.sendMessage(net.minecraft.text.Text.literal("Target beaker is full."), true);
                    }
                }
            });
        });
    }
}
