package org.keke.chemistry.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.keke.chemistry.block.BunsenBurnerBlock;
import org.keke.chemistry.utils.FormulaUtils;

/**
 * Distillation block entity. Requires heat source below.
 * Processes input solution over time.
 */
public class DistillationBlockEntity extends BlockEntity {
    private String inputFormula = "";
    private double inputMoles = 0.0;
    private String outputFormula = "";
    private double outputMoles = 0.0;
    private boolean processing = false;
    private int progress = 0;
    private static final int DISTILL_TIME = 100; // 5 seconds

    public DistillationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISTILLATION, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, DistillationBlockEntity be) {
        if (!be.processing) return;

        // Check for heat source below
        BlockState below = world.getBlockState(pos.down());
        if (!(below.getBlock() instanceof BunsenBurnerBlock) || !below.get(BunsenBurnerBlock.LIT)) {
            return; // needs heat
        }

        be.progress++;
        if (be.progress >= DISTILL_TIME) {
            be.completeDistillation();
        }
    }

    public void setInput(String formula, double moles) {
        this.inputFormula = formula;
        this.inputMoles = moles;
        this.processing = true;
        this.progress = 0;
        markDirty();
    }

    private void completeDistillation() {
        // Simple distillation: output is the same as input (purified)
        outputFormula = inputFormula;
        outputMoles = inputMoles;
        inputFormula = "";
        inputMoles = 0.0;
        processing = false;
        progress = 0;
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    public boolean isEmpty() { return inputFormula.isEmpty() && outputFormula.isEmpty(); }
    public String getOutputFormula() { return outputFormula; }
    public double getOutputMoles() { return outputMoles; }

    public void clearOutput() {
        outputFormula = "";
        outputMoles = 0.0;
        markDirty();
    }

    public void reportStatus(PlayerEntity player) {
        if (processing) {
            // Check heat
            if (world != null) {
                BlockState below = world.getBlockState(pos.down());
                if (!(below.getBlock() instanceof BunsenBurnerBlock) || !below.get(BunsenBurnerBlock.LIT)) {
                    player.sendMessage(Text.literal("Needs heat source below!").formatted(Formatting.RED), true);
                    return;
                }
            }
            int percent = (progress * 100) / DISTILL_TIME;
            player.sendMessage(Text.literal("Distilling: " + percent + "%").formatted(Formatting.YELLOW), true);
        } else if (!outputFormula.isEmpty()) {
            player.sendMessage(Text.literal("Output: " + FormulaUtils.formatFormula(outputFormula) +
                    String.format(" (%.2f mol)", outputMoles)).formatted(Formatting.GREEN), true);
        } else {
            player.sendMessage(Text.literal("Empty — add solution to distill").formatted(Formatting.GRAY), true);
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        nbt.putString("inputFormula", inputFormula);
        nbt.putDouble("inputMoles", inputMoles);
        nbt.putString("outputFormula", outputFormula);
        nbt.putDouble("outputMoles", outputMoles);
        nbt.putBoolean("processing", processing);
        nbt.putInt("progress", progress);
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        inputFormula = nbt.getString("inputFormula");
        inputMoles = nbt.getDouble("inputMoles");
        outputFormula = nbt.getString("outputFormula");
        outputMoles = nbt.getDouble("outputMoles");
        processing = nbt.getBoolean("processing");
        progress = nbt.getInt("progress");
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
