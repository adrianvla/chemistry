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
import org.keke.chemistry.utils.FormulaUtils;

/**
 * Filter block entity — handles filtration of solutions.
 * For simplicity, passes the solution through directly (no precipitate separation logic
 * unless a precipitation reaction has occurred).
 */
public class FilterBlockEntity extends BlockEntity {
    private String inputFormula = "";
    private double inputMoles = 0.0;
    private String filtrateFormula = "";
    private double filtrateMoles = 0.0;
    private String residueFormula = "";
    private double residueMoles = 0.0;
    private boolean processing = false;
    private int progress = 0;
    private static final int FILTER_TIME = 40; // 2 seconds

    public FilterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FILTER, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, FilterBlockEntity be) {
        if (!be.processing) return;

        be.progress++;
        if (be.progress >= FILTER_TIME) {
            be.completeFiltering();
        }
    }

    public void setInput(String formula, double moles) {
        this.inputFormula = formula;
        this.inputMoles = moles;
        this.processing = true;
        this.progress = 0;
        markDirty();
    }

    private void completeFiltering() {
        // Simple filtration: pass liquid through
        filtrateFormula = inputFormula;
        filtrateMoles = inputMoles;
        inputFormula = "";
        inputMoles = 0.0;
        processing = false;
        progress = 0;
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    public boolean isEmpty() { return inputFormula.isEmpty() && filtrateFormula.isEmpty(); }
    public String getFiltrateFormula() { return filtrateFormula; }
    public double getFiltrateMoles() { return filtrateMoles; }

    public void clearFiltrate() {
        filtrateFormula = "";
        filtrateMoles = 0.0;
        markDirty();
    }

    public void reportStatus(PlayerEntity player) {
        if (processing) {
            int percent = (progress * 100) / FILTER_TIME;
            player.sendMessage(Text.literal("Filtering: " + percent + "%").formatted(Formatting.YELLOW), true);
        } else if (!filtrateFormula.isEmpty()) {
            player.sendMessage(Text.literal("Filtrate: " + FormulaUtils.formatFormula(filtrateFormula) +
                    String.format(" (%.2f mol)", filtrateMoles)).formatted(Formatting.GREEN), true);
        } else {
            player.sendMessage(Text.literal("Empty — pour solution to filter").formatted(Formatting.GRAY), true);
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        nbt.putString("inputFormula", inputFormula);
        nbt.putDouble("inputMoles", inputMoles);
        nbt.putString("filtrateFormula", filtrateFormula);
        nbt.putDouble("filtrateMoles", filtrateMoles);
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
        filtrateFormula = nbt.getString("filtrateFormula");
        filtrateMoles = nbt.getDouble("filtrateMoles");
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
