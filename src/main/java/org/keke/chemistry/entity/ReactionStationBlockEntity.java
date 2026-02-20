package org.keke.chemistry.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.keke.chemistry.reaction.*;
import org.keke.chemistry.utils.FormulaUtils;

import java.util.*;

public class ReactionStationBlockEntity extends BlockEntity {
    private String input1Formula = "";
    private double input1Moles = 0.0;
    private String input2Formula = "";
    private double input2Moles = 0.0;
    private String outputFormula = "";
    private double outputMoles = 0.0;
    private String leftoverFormula = "";
    private double leftoverMoles = 0.0;

    private boolean reacting = false;
    private int progress = 0;
    private int reactionTime = 60; // ticks (3 seconds)
    private int temperature = 20; // ambient

    public ReactionStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTION_STATION, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, ReactionStationBlockEntity be) {
        if (!be.reacting) return;

        be.progress++;

        // Spawn particles during reaction
        if (world instanceof ServerWorld serverWorld) {
            if (be.progress % 5 == 0) {
                serverWorld.spawnParticles(ParticleTypes.BUBBLE_POP,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        2, 0.2, 0.1, 0.2, 0.01);
            }
        }

        if (be.progress >= be.reactionTime) {
            be.completeReaction();
        }
    }

    public boolean addInput(String formula, double moles) {
        if (reacting) return false;

        if (input1Formula.isEmpty()) {
            input1Formula = formula;
            input1Moles = moles;
            markDirty();
            return true;
        } else if (input2Formula.isEmpty()) {
            input2Formula = formula;
            input2Moles = moles;
            markDirty();
            return true;
        }
        return false;
    }

    public void tryStartReaction() {
        if (reacting) return;
        if (input1Formula.isEmpty() || input2Formula.isEmpty()) return;

        Reaction reaction = ReactionRegistry.findReaction(input1Formula, input2Formula);
        if (reaction != null) {
            // Check conditions
            if (reaction.getConditions().requiresHeat() && temperature < reaction.getConditions().getMinTemp()) {
                return; // needs heat
            }
            reacting = true;
            progress = 0;
            markDirty();
        }
    }

    private void completeReaction() {
        Reaction reaction = ReactionRegistry.findReaction(input1Formula, input2Formula);
        if (reaction == null) {
            reacting = false;
            return;
        }

        Map<String, Double> inputMoles = new LinkedHashMap<>();
        inputMoles.put(input1Formula, input1Moles);
        inputMoles.put(input2Formula, input2Moles);

        ReactionResult result = ReactionProcessor.process(reaction, inputMoles, temperature);

        if (result.isSuccess()) {
            // Take first product as main output
            Map.Entry<String, Double> firstProduct = result.getProducts().entrySet().iterator().next();
            outputFormula = firstProduct.getKey();
            outputMoles = firstProduct.getValue();

            // Take first leftover if any
            if (!result.getLeftovers().isEmpty()) {
                Map.Entry<String, Double> firstLeftover = result.getLeftovers().entrySet().iterator().next();
                leftoverFormula = firstLeftover.getKey();
                leftoverMoles = firstLeftover.getValue();
            }

            // Spawn effect particles
            if (world instanceof ServerWorld serverWorld) {
                if (result.getEffects().contains(ReactionEffect.GAS_EVOLUTION)) {
                    serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                            pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                            10, 0.2, 0.3, 0.2, 0.02);
                }
                if (result.getEffects().contains(ReactionEffect.EXOTHERMIC)) {
                    serverWorld.spawnParticles(ParticleTypes.FLAME,
                            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                            5, 0.2, 0.1, 0.2, 0.01);
                }
            }
        }

        input1Formula = "";
        input1Moles = 0.0;
        input2Formula = "";
        input2Moles = 0.0;
        reacting = false;
        progress = 0;
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    public void reportStatus(PlayerEntity player) {
        if (reacting) {
            int percent = (progress * 100) / reactionTime;
            player.sendMessage(Text.literal("Reaction in progress: " + percent + "%").formatted(Formatting.YELLOW), true);
        } else if (!outputFormula.isEmpty()) {
            player.sendMessage(Text.literal("Output: " + FormulaUtils.formatFormula(outputFormula) +
                    String.format(" (%.2f mol)", outputMoles)).formatted(Formatting.GREEN), true);
        } else if (!input1Formula.isEmpty() && input2Formula.isEmpty()) {
            player.sendMessage(Text.literal("Input 1: " + FormulaUtils.formatFormula(input1Formula) +
                    " — add second reactant").formatted(Formatting.AQUA), true);
        } else if (!input1Formula.isEmpty()) {
            player.sendMessage(Text.literal("Ready — waiting for reaction start").formatted(Formatting.AQUA), true);
        } else {
            player.sendMessage(Text.literal("Empty — add reactants with beakers").formatted(Formatting.GRAY), true);
        }
    }

    public String getOutputFormula() { return outputFormula; }
    public double getOutputMoles() { return outputMoles; }
    public boolean isReacting() { return reacting; }
    public int getProgress() { return progress; }
    public int getReactionTime() { return reactionTime; }

    public void clearOutput() {
        outputFormula = "";
        outputMoles = 0.0;
        leftoverFormula = "";
        leftoverMoles = 0.0;
        markDirty();
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getTemperature() { return temperature; }

    @Override
    public void writeNbt(NbtCompound nbt) {
        nbt.putString("input1Formula", input1Formula);
        nbt.putDouble("input1Moles", input1Moles);
        nbt.putString("input2Formula", input2Formula);
        nbt.putDouble("input2Moles", input2Moles);
        nbt.putString("outputFormula", outputFormula);
        nbt.putDouble("outputMoles", outputMoles);
        nbt.putString("leftoverFormula", leftoverFormula);
        nbt.putDouble("leftoverMoles", leftoverMoles);
        nbt.putBoolean("reacting", reacting);
        nbt.putInt("progress", progress);
        nbt.putInt("temperature", temperature);
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        input1Formula = nbt.getString("input1Formula");
        input1Moles = nbt.getDouble("input1Moles");
        input2Formula = nbt.getString("input2Formula");
        input2Moles = nbt.getDouble("input2Moles");
        outputFormula = nbt.getString("outputFormula");
        outputMoles = nbt.getDouble("outputMoles");
        leftoverFormula = nbt.getString("leftoverFormula");
        leftoverMoles = nbt.getDouble("leftoverMoles");
        reacting = nbt.getBoolean("reacting");
        progress = nbt.getInt("progress");
        temperature = nbt.getInt("temperature");
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}
