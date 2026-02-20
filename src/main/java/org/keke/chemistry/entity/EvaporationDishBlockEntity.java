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
import org.keke.chemistry.utils.FormulaUtils;

/**
 * Evaporation dish block entity. Slowly evaporates solvent over time,
 * leaving residue behind.
 */
public class EvaporationDishBlockEntity extends BlockEntity {
    private String inputFormula = "";
    private double inputMoles = 0.0;
    private String residueFormula = "";
    private double residueMoles = 0.0;
    private boolean processing = false;
    private int progress = 0;
    private static final int EVAPORATION_TIME = 200; // 10 seconds

    public EvaporationDishBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EVAPORATION_DISH, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, EvaporationDishBlockEntity be) {
        if (!be.processing) return;

        be.progress++;

        // Spawn evaporation particles
        if (world instanceof ServerWorld serverWorld && be.progress % 10 == 0) {
            serverWorld.spawnParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    1, 0.3, 0.1, 0.3, 0.01);
        }

        if (be.progress >= EVAPORATION_TIME) {
            be.completeEvaporation();
        }
    }

    public void setInput(String formula, double moles) {
        this.inputFormula = formula;
        this.inputMoles = moles;
        this.processing = true;
        this.progress = 0;
        markDirty();
    }

    private void completeEvaporation() {
        // After evaporation, the dissolved solid remains (same formula for simplicity)
        residueFormula = inputFormula;
        residueMoles = inputMoles;
        inputFormula = "";
        inputMoles = 0.0;
        processing = false;
        progress = 0;
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    public boolean isEmpty() { return inputFormula.isEmpty() && residueFormula.isEmpty(); }
    public String getResidueFormula() { return residueFormula; }
    public double getResidueMoles() { return residueMoles; }

    public void clearResidue() {
        residueFormula = "";
        residueMoles = 0.0;
        markDirty();
    }

    public void reportStatus(PlayerEntity player) {
        if (processing) {
            int percent = (progress * 100) / EVAPORATION_TIME;
            player.sendMessage(Text.literal("Evaporating: " + percent + "%").formatted(Formatting.YELLOW), true);
        } else if (!residueFormula.isEmpty()) {
            player.sendMessage(Text.literal("Residue: " + FormulaUtils.formatFormula(residueFormula) +
                    String.format(" (%.2f mol)", residueMoles)).formatted(Formatting.GREEN), true);
        } else {
            player.sendMessage(Text.literal("Empty — pour solution to evaporate").formatted(Formatting.GRAY), true);
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        nbt.putString("inputFormula", inputFormula);
        nbt.putDouble("inputMoles", inputMoles);
        nbt.putString("residueFormula", residueFormula);
        nbt.putDouble("residueMoles", residueMoles);
        nbt.putBoolean("processing", processing);
        nbt.putInt("progress", progress);
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        inputFormula = nbt.getString("inputFormula");
        inputMoles = nbt.getDouble("inputMoles");
        residueFormula = nbt.getString("residueFormula");
        residueMoles = nbt.getDouble("residueMoles");
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
