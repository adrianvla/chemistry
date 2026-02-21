package org.keke.chemistry.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.keke.chemistry.reaction.*;
import org.keke.chemistry.utils.CalculateColor;
import org.keke.chemistry.utils.Compound;
import org.keke.chemistry.utils.CompoundPhysicalData;
import org.keke.chemistry.utils.StateOfMatter;

import java.util.*;

/**
 * Abstract base for any block entity that holds a mixture of chemicals.
 * Shared logic: contents map, temperature, volume/mass calculations,
 * reaction processing, color blending, and NBT serialization.
 *
 * Subclasses implement container-specific behavior via hooks:
 *   {@link #onReactionEffects(ReactionResult)}  — particles / sounds
 *   {@link #onThermalOverload(double, double)}   — cracking / explosion
 *   {@link #getEffectiveMassG()}                 — mass for heat-capacity calc
 */
public abstract class AbstractChemistryContainer extends BlockEntity {

    // ── Constants ──────────────────────────────────────────────────────

    /** Ambient temperature in Kelvin (20 °C). */
    protected static final double AMBIENT_TEMP_K = 293.15;

    // ── Fields ─────────────────────────────────────────────────────────

    /** Formula → moles map. */
    protected final Map<String, Double> contents = new LinkedHashMap<>();

    /** Current temperature in Kelvin. */
    protected double temperatureK = AMBIENT_TEMP_K;

    /** Cached blended color for rendering. */
    protected int cachedColor = 0x00000000;

    /** Maximum capacity in mL (default 250 mL, overridden by container size). */
    protected double maxCapacityMl = 250.0;

    /**
     * Sedimentation progress per solid compound (formula → 0.0..1.0).
     * 0.0 = fully suspended, 1.0 = fully settled.
     */
    protected final Map<String, Double> sedimentationProgress = new LinkedHashMap<>();

    /** Natural sedimentation rate per tick (0.005 → ~10 seconds to settle). */
    protected static final double NATURAL_SEDIMENTATION_RATE = 0.005;

    // ── Constructor ────────────────────────────────────────────────────

    protected AbstractChemistryContainer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ── Hooks for subclasses ───────────────────────────────────────────

    /**
     * Called after a reaction produces effects. Subclasses spawn particles,
     * play sounds, etc. based on the result.
     */
    protected abstract void onReactionEffects(ReactionResult result);

    /**
     * Called when temperature exceeds safe limits. Subclasses decide what
     * happens (crack beaker, explode cauldron, etc.).
     *
     * @param currentTempK  temperature after reaction
     * @param deltaT        temperature change from this reaction step
     * @return true if the container was destroyed (stops further processing)
     */
    protected abstract boolean onThermalOverload(double currentTempK, double deltaT);

    /**
     * Effective solution mass in grams for heat-capacity calculations.
     * Subclasses may adjust this based on cooling blocks or container type.
     */
    protected abstract double getEffectiveMassG();

    /**
     * Maximum single-step ΔT before thermal overload triggers.
     * Defaults to 100 K; subclasses may override.
     */
    protected double getThermalShockThreshold() {
        return 100.0;
    }

    /**
     * Temperature above which thermal overload triggers.
     * Defaults to 573.15 K (300 °C); subclasses may override.
     */
    protected double getMaxSafeTemperature() {
        return 573.15;
    }

    /**
     * Called when the container overflows. Subclasses handle the spilled
     * contents (damage, particles, ground effects).
     *
     * @param spilled map of formula → moles that spilled out
     */
    protected void onOverflow(Map<String, Double> spilled) {
        // default: no-op; subclasses override
    }

    // ── Contents API ───────────────────────────────────────────────────

    public Map<String, Double> getContents() {
        return Collections.unmodifiableMap(contents);
    }

    public double getTemperatureK() { return temperatureK; }
    public double getTemperatureC() { return temperatureK - 273.15; }

    public void setTemperatureK(double tempK) {
        this.temperatureK = tempK;
        markDirty();
        syncToClient();
    }

    public int getCachedColor() { return cachedColor; }
    public boolean isEmpty() { return contents.isEmpty(); }

    /**
     * Total moles currently in the container.
     */
    public double getTotalMoles() {
        double total = 0;
        for (double v : contents.values()) total += v;
        return total;
    }

    /**
     * Set the full contents map (replaces existing). Used when placing from item.
     */
    public void setContents(Map<String, Double> newContents) {
        contents.clear();
        for (var entry : newContents.entrySet()) {
            if (entry.getValue() > 0.001) {
                contents.put(entry.getKey(), entry.getValue());
            }
        }
        recomputeColor();
        markDirty();
        syncToClient();
    }

    /**
     * Legacy single-formula setter for backward compatibility.
     */
    public void setContents(String formula, double moles) {
        contents.clear();
        if (!formula.isEmpty() && moles > 0.001) {
            contents.put(formula, moles);
        }
        recomputeColor();
        markDirty();
        syncToClient();
    }

    /**
     * Add a chemical to the container. Triggers reaction check.
     * Uses volume-based capacity checking.
     *
     * @return the actual moles added (may be less than requested if near capacity), or 0 if nothing fit
     */
    public double addChemical(String formula, double moles) {
        if (moles <= 0) return 0;

        // Check remaining volume capacity
        double currentVolume = getTotalVolumeMl();
        double remainingMl = maxCapacityMl - currentVolume;
        if (remainingMl <= 0.01) return 0;

        // Calculate how many moles of this compound fit in the remaining volume
        double molarMass = Compound.computeMolarMass(formula);
        if (molarMass <= 0) molarMass = 18.015;  // fallback
        double density = CompoundPhysicalData.getDensity(formula, temperatureK);
        if (density <= 0) density = 1.0;
        double mlPerMol = molarMass / density;
        double maxMolesToFit = remainingMl / mlPerMol;

        double toAdd = Math.min(moles, maxMolesToFit);
        if (toAdd <= 0.001) return 0;

        contents.merge(formula, toAdd, Double::sum);
        recomputeColor();
        markDirty();
        syncToClient();

        // Trigger reaction check
        processReactions();

        return toAdd;
    }

    /**
     * Remove a specific amount of a chemical from the container.
     *
     * @return the amount actually removed (may be less than requested)
     */
    public double removeChemical(String formula, double moles) {
        if (moles <= 0 || !contents.containsKey(formula)) return 0;

        double current = contents.get(formula);
        double toRemove = Math.min(moles, current);

        double remaining = current - toRemove;
        if (remaining <= 0.001) {
            contents.remove(formula);
        } else {
            contents.put(formula, remaining);
        }

        recomputeColor();
        markDirty();
        syncToClient();
        return toRemove;
    }

    /**
     * Clear all contents and reset temperature.
     */
    public void clearContents() {
        contents.clear();
        temperatureK = AMBIENT_TEMP_K;
        cachedColor = 0x00000000;
        markDirty();
        syncToClient();
    }

    // ── Reaction Processing ────────────────────────────────────────────

    /**
     * Check all contents for possible reactions and process them.
     * Iterates until no more reactions occur (cascading reactions).
     */
    protected void processReactions() {
        if (world == null || world.isClient) return;

        int maxIterations = 10;
        for (int iter = 0; iter < maxIterations; iter++) {
            List<String> formulas = new ArrayList<>(contents.keySet());
            if (formulas.size() < 1) break;

            Reaction reaction = ReactionRegistry.findReactionAmong(formulas);
            if (reaction == null) break;

            // Build input moles map from current contents
            Map<String, Double> inputMoles = new LinkedHashMap<>(contents);

            // Process the reaction
            ReactionResult result = ReactionProcessor.process(reaction, inputMoles,
                    getTemperatureC(), getEffectiveMassG());

            if (!result.isSuccess()) break;

            // Apply temperature change
            double deltaT = result.getTemperatureChangeK();
            temperatureK += deltaT;

            // Check for thermal overload
            if (Math.abs(deltaT) > getThermalShockThreshold()
                    || temperatureK > getMaxSafeTemperature()) {
                if (onThermalOverload(temperatureK, deltaT)) {
                    return; // container was destroyed
                }
            }

            // Update contents: leftovers + products
            contents.clear();
            for (var entry : result.getLeftovers().entrySet()) {
                if (entry.getValue() > 0.001) {
                    contents.put(entry.getKey(), entry.getValue());
                }
            }
            for (var entry : result.getProducts().entrySet()) {
                if (entry.getValue() > 0.001) {
                    contents.merge(entry.getKey(), entry.getValue(), Double::sum);
                }
            }

            // Subclass hook for particles / sounds
            onReactionEffects(result);

            recomputeColor();
            markDirty();
            syncToClient();
        }

        // After all reactions, check for overflow (products may exceed capacity)
        checkAndHandleOverflow();
    }

    // ── Color ──────────────────────────────────────────────────────────

    protected void recomputeColor() {
        if (contents.isEmpty()) {
            cachedColor = 0x00000000;
            return;
        }
        cachedColor = CalculateColor.calculateBlendedColor(contents);
    }

    // ── Volume / Mass ──────────────────────────────────────────────────

    /**
     * Get the state of matter of a compound at the current temperature.
     */
    public StateOfMatter getCompoundState(String formula) {
        return CompoundPhysicalData.getStateAtTemperature(formula, temperatureK);
    }

    /**
     * Get a map of formula → current state of matter.
     */
    public Map<String, StateOfMatter> getContentsStates() {
        Map<String, StateOfMatter> states = new LinkedHashMap<>();
        for (String formula : contents.keySet()) {
            states.put(formula, getCompoundState(formula));
        }
        return states;
    }

    /**
     * Remove any gaseous compounds from the container (they escape).
     * Called during tick by subclasses that support gas escape.
     *
     * @return map of escaped formulas and their moles (for gas cloud spawning)
     */
    protected Map<String, Double> removeGases() {
        Map<String, Double> escaped = new LinkedHashMap<>();
        Iterator<Map.Entry<String, Double>> it = contents.entrySet().iterator();
        boolean changed = false;
        while (it.hasNext()) {
            Map.Entry<String, Double> entry = it.next();
            if (getCompoundState(entry.getKey()) == StateOfMatter.GAS) {
                escaped.put(entry.getKey(), entry.getValue());
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            recomputeColor();
            markDirty();
            syncToClient();
        }
        return escaped;
    }

    /**
     * Calculate total volume in mL across all contents using density-based calculation.
     * V = Σ (moles × molarMass / density)
     */
    public double getTotalVolumeMl() {
        double total = 0;
        for (var entry : contents.entrySet()) {
            total += getCompoundVolumeMl(entry.getKey(), entry.getValue());
        }
        return total;
    }

    /** Alias for backward compat. */
    public double getVolumeMl() {
        return getTotalVolumeMl();
    }

    /**
     * Get volume of a single compound in mL.
     */
    public double getCompoundVolumeMl(String formula, double moles) {
        double molarMass = Compound.computeMolarMass(formula);
        if (molarMass <= 0) molarMass = 18.015;
        double density = CompoundPhysicalData.getDensity(formula, temperatureK);
        if (density <= 0) density = 1.0;
        return (moles * molarMass) / density;
    }

    /**
     * Check if the container is overflowing.
     */
    public boolean isOverflowing() {
        return getTotalVolumeMl() > maxCapacityMl;
    }

    /**
     * If the container is over capacity, remove excess chemicals and invoke
     * the {@link #onOverflow(Map)} hook. Removes chemicals proportionally
     * from all current contents until volume fits within capacity.
     */
    protected void checkAndHandleOverflow() {
        double totalVol = getTotalVolumeMl();
        if (totalVol <= maxCapacityMl) return;

        double excessFraction = 1.0 - (maxCapacityMl / totalVol);
        Map<String, Double> spilled = new LinkedHashMap<>();

        // Remove a proportional fraction of each compound
        for (var it = contents.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            double spillMoles = entry.getValue() * excessFraction;
            if (spillMoles > 0.001) {
                spilled.put(entry.getKey(), spillMoles);
                double remaining = entry.getValue() - spillMoles;
                if (remaining <= 0.001) {
                    it.remove();
                } else {
                    entry.setValue(remaining);
                }
            }
        }

        if (!spilled.isEmpty()) {
            recomputeColor();
            markDirty();
            syncToClient();
            onOverflow(spilled);
        }
    }

    /**
     * Get the maximum capacity in mL.
     */
    public double getMaxCapacityMl() {
        return maxCapacityMl;
    }

    /**
     * Set the maximum capacity in mL (used when placing from different container sizes).
     */
    public void setMaxCapacityMl(double ml) {
        this.maxCapacityMl = ml;
        markDirty();
    }

    // ── Sedimentation ──────────────────────────────────────────────────

    /**
     * Get the sedimentation progress for a compound (0.0 = suspended, 1.0 = settled).
     */
    public double getSedimentationProgress(String formula) {
        return sedimentationProgress.getOrDefault(formula, 0.0);
    }

    /**
     * Set the sedimentation progress for a specific compound.
     */
    public void setSedimentationProgress(String formula, double progress) {
        progress = Math.max(0.0, Math.min(1.0, progress));
        if (progress <= 0.001) {
            sedimentationProgress.remove(formula);
        } else {
            sedimentationProgress.put(formula, progress);
        }
        markDirty();
        syncToClient();
    }

    /**
     * Get the full sedimentation progress map.
     */
    public Map<String, Double> getAllSedimentationProgress() {
        return Collections.unmodifiableMap(sedimentationProgress);
    }

    /**
     * Naturally advance sedimentation for all solid compounds by one tick.
     * Solid compounds settle at NATURAL_SEDIMENTATION_RATE per tick.
     */
    protected void tickSedimentation() {
        boolean changed = false;
        for (var entry : contents.entrySet()) {
            StateOfMatter state = getCompoundState(entry.getKey());
            if (state == StateOfMatter.SOLID) {
                double current = sedimentationProgress.getOrDefault(entry.getKey(), 0.0);
                if (current < 1.0) {
                    double next = Math.min(1.0, current + NATURAL_SEDIMENTATION_RATE);
                    sedimentationProgress.put(entry.getKey(), next);
                    changed = true;
                }
            } else {
                // Non-solid compounds lose sedimentation progress
                if (sedimentationProgress.containsKey(entry.getKey())) {
                    sedimentationProgress.remove(entry.getKey());
                    changed = true;
                }
            }
        }
        // Clean up entries for removed compounds
        sedimentationProgress.keySet().retainAll(contents.keySet());
        if (changed) {
            markDirty();
        }
    }

    /**
     * Accelerate sedimentation — set all solid compounds towards 1.0 by the given increment.
     *
     * @param increment amount to add per compound per call
     */
    public void accelerateSedimentation(double increment) {
        boolean changed = false;
        for (var entry : contents.entrySet()) {
            if (getCompoundState(entry.getKey()) == StateOfMatter.SOLID) {
                double current = sedimentationProgress.getOrDefault(entry.getKey(), 0.0);
                if (current < 1.0) {
                    double next = Math.min(1.0, current + increment);
                    sedimentationProgress.put(entry.getKey(), next);
                    changed = true;
                }
            }
        }
        if (changed) {
            markDirty();
            syncToClient();
        }
    }

    /**
     * Decrease sedimentation — move all solid compounds towards 0.0 by the given decrement.
     *
     * @param decrement amount to subtract per compound per call
     */
    public void decreaseSedimentation(double decrement) {
        boolean changed = false;
        for (var entry : contents.entrySet()) {
            if (getCompoundState(entry.getKey()) == StateOfMatter.SOLID) {
                double current = sedimentationProgress.getOrDefault(entry.getKey(), 0.0);
                if (current > 0.0) {
                    double next = Math.max(0.0, current - decrement);
                    if (next <= 0.001) {
                        sedimentationProgress.remove(entry.getKey());
                    } else {
                        sedimentationProgress.put(entry.getKey(), next);
                    }
                    changed = true;
                }
            }
        }
        if (changed) {
            markDirty();
            syncToClient();
        }
    }

    // ── Heating from below ─────────────────────────────────────────────

    /** Bunsen burner heating rate in K/tick (reaches ~600°C in ~15 seconds ≈ 300 ticks). */
    protected static final double BUNSEN_HEATING_RATE = 2.0;

    /** Target temperature when heated by a lit bunsen burner (≈800°C). */
    protected static final double BUNSEN_TARGET_TEMP_K = 1073.15;

    /**
     * Check if a lit bunsen burner is below and apply heating.
     * Call from each subclass's tick method.
     *
     * @return true if heating was applied
     */
    protected boolean tickHeatingFromBelow() {
        if (world == null || world.isClient) return false;
        if (contents.isEmpty()) return false;

        net.minecraft.block.BlockState below = world.getBlockState(pos.down());
        if (below.getBlock() instanceof org.keke.chemistry.block.BunsenBurnerBlock
                && below.get(org.keke.chemistry.block.BunsenBurnerBlock.LIT)) {
            if (temperatureK < BUNSEN_TARGET_TEMP_K) {
                temperatureK = Math.min(BUNSEN_TARGET_TEMP_K,
                        temperatureK + BUNSEN_HEATING_RATE);
                markDirty();
                syncToClient();

                // Check for thermal overload
                if (temperatureK > getMaxSafeTemperature()) {
                    onThermalOverload(temperatureK, BUNSEN_HEATING_RATE);
                }

                // Re-process reactions (heat may trigger decomposition)
                processReactions();
            }
            return true;
        }
        return false;
    }

    /**
     * Calculate total mass in grams across all contents using molar masses.
     */
    public double getTotalMassGrams() {
        double total = 0;
        for (var entry : contents.entrySet()) {
            double molarMass = Compound.computeMolarMass(entry.getKey());
            total += entry.getValue() * molarMass;
        }
        return total;
    }

    // ── Legacy getters for backward compat with renderer ──────────────

    public String getFormula() {
        if (contents.isEmpty()) return "";
        return contents.keySet().iterator().next();
    }

    public double getMoles() {
        return getTotalMoles();
    }

    // ── NBT Serialization ──────────────────────────────────────────────

    @Override
    public void writeNbt(NbtCompound nbt) {
        NbtCompound contentsNbt = new NbtCompound();
        for (var entry : contents.entrySet()) {
            contentsNbt.putDouble(entry.getKey(), entry.getValue());
        }
        nbt.put("contents", contentsNbt);
        nbt.putDouble("temperatureK", temperatureK);
        nbt.putInt("cachedColor", cachedColor);
        nbt.putDouble("maxCapacityMl", maxCapacityMl);

        if (!sedimentationProgress.isEmpty()) {
            NbtCompound sedNbt = new NbtCompound();
            for (var entry : sedimentationProgress.entrySet()) {
                sedNbt.putDouble(entry.getKey(), entry.getValue());
            }
            nbt.put("sedimentation", sedNbt);
        }

        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        contents.clear();

        // New format: "contents" compound
        if (nbt.contains("contents", NbtElement.COMPOUND_TYPE)) {
            NbtCompound contentsNbt = nbt.getCompound("contents");
            for (String key : contentsNbt.getKeys()) {
                double val = contentsNbt.getDouble(key);
                if (val > 0.001) {
                    contents.put(key, val);
                }
            }
        }
        // Backward compat: old single-formula format
        else if (nbt.contains("formula")) {
            String formula = nbt.getString("formula");
            double moles = nbt.getDouble("moles");
            if (!formula.isEmpty() && moles > 0.001) {
                contents.put(formula, moles);
            }
        }

        temperatureK = nbt.contains("temperatureK")
                ? nbt.getDouble("temperatureK") : AMBIENT_TEMP_K;
        maxCapacityMl = nbt.contains("maxCapacityMl")
                ? nbt.getDouble("maxCapacityMl") : 250.0;

        sedimentationProgress.clear();
        if (nbt.contains("sedimentation", NbtElement.COMPOUND_TYPE)) {
            NbtCompound sedNbt = nbt.getCompound("sedimentation");
            for (String key : sedNbt.getKeys()) {
                double val = sedNbt.getDouble(key);
                if (val > 0.001) {
                    sedimentationProgress.put(key, val);
                }
            }
        }

        recomputeColor();
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

    /**
     * Build an NBT compound representing current contents (for item stacks).
     */
    public NbtCompound getContentsNbt() {
        NbtCompound nbt = new NbtCompound();
        NbtCompound contentsNbt = new NbtCompound();
        for (var entry : contents.entrySet()) {
            contentsNbt.putDouble(entry.getKey(), entry.getValue());
        }
        nbt.put("contents", contentsNbt);
        nbt.putDouble("temperatureK", temperatureK);
        nbt.putInt("cachedColor", cachedColor);
        nbt.putDouble("maxCapacityMl", maxCapacityMl);

        if (!sedimentationProgress.isEmpty()) {
            NbtCompound sedNbt = new NbtCompound();
            for (var entry : sedimentationProgress.entrySet()) {
                sedNbt.putDouble(entry.getKey(), entry.getValue());
            }
            nbt.put("sedimentation", sedNbt);
        }

        return nbt;
    }

    protected void syncToClient() {
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }
}
