package org.keke.chemistry.entity;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.keke.chemistry.reaction.ReactionEffect;
import org.keke.chemistry.reaction.ReactionResult;
import org.keke.chemistry.utils.GasCloudHelper;

import java.util.Map;

/**
 * Block entity for the Ampule — a small sealed glass container.
 *
 * Key properties:
 *   - Very small capacity: 10 mL
 *   - Can be sealed (no gas escape, no reactions when sealed)
 *   - Fragile: cracks at low thermal overload
 *   - When sealed, acts as inert storage
 */
public class AmpuleBlockEntity extends AbstractChemistryContainer {

    private static final double AMPULE_CAPACITY_ML = 10.0;
    private static final double COOLING_K = 0.02;
    private static final double EFFECTIVE_MASS_G = 10.0;

    private boolean sealed = true;

    public AmpuleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AMPULE, pos, state);
        this.maxCapacityMl = AMPULE_CAPACITY_ML;
    }

    public boolean isSealed() { return sealed; }

    public void setSealed(boolean sealed) {
        this.sealed = sealed;
        markDirty();
        syncToClient();
    }

    @Override
    protected double getEffectiveMassG() {
        return EFFECTIVE_MASS_G;
    }

    @Override
    protected void onReactionEffects(ReactionResult result) {
        if (world == null || world.isClient) return;
        world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW,
                SoundCategory.BLOCKS, 0.2f, 1.6f);
    }

    @Override
    protected boolean onThermalOverload(double currentTempK, double deltaT) {
        if (world == null || world.isClient) return false;

        // Sealed ampule under pressure can pop
        world.playSound(null, pos, SoundEvents.BLOCK_GLASS_BREAK,
                SoundCategory.BLOCKS, 1.2f, 1.8f);
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.CRIT,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    15, 0.2, 0.3, 0.2, 0.15);
        }

        Map<String, Double> spilled = Map.copyOf(contents);
        contents.clear();
        onOverflow(spilled);
        world.breakBlock(pos, false);
        return true;
    }

    @Override
    protected double getMaxSafeTemperature() {
        return 423.15; // 150 °C if sealed (pressure build-up)
    }

    @Override
    protected double getThermalShockThreshold() {
        return 40.0; // very fragile
    }

    // ── NBT ────────────────────────────────────────────────────────────

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.sealed = nbt.getBoolean("sealed");
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("sealed", sealed);
    }

    // ── Tick ────────────────────────────────────────────────────────────

    public static void tick(World world, BlockPos pos, BlockState state, AmpuleBlockEntity ampule) {
        if (world.isClient) return;
        if (ampule.isEmpty()) return;

        // Sealed ampules do not process reactions or lose gases
        if (ampule.sealed) return;

        ampule.tickHeatingFromBelow();

        if (Math.abs(ampule.temperatureK - AMBIENT_TEMP_K) > 0.01) {
            ampule.temperatureK -= COOLING_K * (ampule.temperatureK - AMBIENT_TEMP_K);
            ampule.markDirty();
            ampule.syncToClient();
        }

        Map<String, Double> gases = ampule.removeGases();
        if (!gases.isEmpty()) {
            GasCloudHelper.spawnGasClouds(world, pos, gases);
        }

        ampule.tickSedimentation();
        ampule.checkAndHandleOverflow();
    }
}
