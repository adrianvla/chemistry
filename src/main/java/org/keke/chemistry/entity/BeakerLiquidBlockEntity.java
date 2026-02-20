package org.keke.chemistry.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.keke.chemistry.block.CoolingBlock;
import org.keke.chemistry.reaction.ReactionEffect;
import org.keke.chemistry.reaction.ReactionResult;
import org.keke.chemistry.utils.DrinkingEffects;
import org.keke.chemistry.utils.GasCloudHelper;

import java.util.List;
import java.util.Map;

/**
 * Block entity for placed beakers. Extends the shared chemistry container
 * with beaker-specific behavior: Newton's-law cooling, steam particles,
 * thermal-shock cracking, and reaction visual/sound effects.
 *
 * Temperature model:
 *   - Default ambient: 293.15 K (20 °C)
 *   - Newton cooling: dT/dt = -k × (T - T_ambient)
 *   - k = 0.01 (normal), k = 0.1 (on cooling block)
 *   - Beaker cracks if T > 573 K (300 °C) or single-step ΔT > 100 K
 */
public class BeakerLiquidBlockEntity extends AbstractChemistryContainer {

    /** Default mass of solution in grams for heat calculations. */
    private static final double SOLUTION_MASS_G = 250.0;

    /** Newton cooling constant without cooling block. */
    private static final double COOLING_K_NORMAL = 0.01;

    /** Newton cooling constant with cooling block underneath. */
    private static final double COOLING_K_COOLED = 0.1;

    /** How often (in ticks) to apply proximity temperature damage. */
    private static final int DAMAGE_INTERVAL_TICKS = 20;

    /** Temperature threshold for hot contact damage (100 °C). */
    private static final double HOT_DAMAGE_THRESHOLD_K = 373.15;

    /** Temperature threshold for cold contact damage (−20 °C). */
    private static final double COLD_DAMAGE_THRESHOLD_K = 253.15;

    /** Effective solution mass for cooling block adjustment. */
    private double effectiveMassG = SOLUTION_MASS_G;

    /** Tick counter for temperature damage interval. */
    private int damageTick = 0;

    public BeakerLiquidBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BEAKER_LIQUID, pos, state);
    }

    // ── Hooks ──────────────────────────────────────────────────────────

    @Override
    protected double getEffectiveMassG() {
        return effectiveMassG;
    }

    @Override
    protected void onReactionEffects(ReactionResult result) {
        if (world == null || world.isClient) return;

        world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW,
                SoundCategory.BLOCKS, 0.6f, 1.0f + (float)(Math.random() * 0.4 - 0.2));

        if (world instanceof ServerWorld serverWorld) {
            if (result.getEffects().contains(ReactionEffect.GAS_EVOLUTION)) {
                serverWorld.spawnParticles(ParticleTypes.BUBBLE_POP,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        8, 0.15, 0.15, 0.15, 0.02);
            }
            if (result.getEffects().contains(ReactionEffect.EXOTHERMIC)) {
                serverWorld.spawnParticles(ParticleTypes.FLAME,
                        pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                        3, 0.1, 0.05, 0.1, 0.01);
            }
            if (result.getEffects().contains(ReactionEffect.PRECIPITATE)) {
                serverWorld.spawnParticles(ParticleTypes.WHITE_ASH,
                        pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                        5, 0.15, 0.1, 0.15, 0.0);
            }
        }
    }

    @Override
    protected boolean onThermalOverload(double currentTempK, double deltaT) {
        crackBeaker();
        return true;
    }

    @Override
    protected void onOverflow(Map<String, Double> spilled) {
        if (world == null || world.isClient) return;

        // Play spill sound
        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH,
                SoundCategory.BLOCKS, 0.7f, 1.2f);

        // Spawn drip particles around the beaker
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.DRIPPING_WATER,
                    pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                    10, 0.3, 0.1, 0.3, 0.01);
        }

        // Apply contact damage to nearby entities from spilled chemicals
        Box spillBox = new Box(pos).expand(1.0);
        List<LivingEntity> entities = world.getEntitiesByClass(
                LivingEntity.class, spillBox, e -> true);

        for (LivingEntity entity : entities) {
            // Classify worst spilled chemical and apply effects
            DrinkingEffects.HazardType worst = DrinkingEffects.HazardType.SAFE;
            for (String formula : spilled.keySet()) {
                DrinkingEffects.HazardType h = DrinkingEffects.classifyHazard(formula);
                if (h.ordinal() > worst.ordinal()) worst = h;
            }

            switch (worst) {
                case CORROSIVE_ACID, CORROSIVE_BASE -> {
                    entity.damage(entity.getDamageSources().magic(), 2.0f);
                    entity.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.NAUSEA, 60, 0));
                }
                case TOXIC_METAL, OXIDIZER -> {
                    entity.damage(entity.getDamageSources().magic(), 1.5f);
                }
                case TOXIC_GAS -> {
                    entity.damage(entity.getDamageSources().magic(), 1.0f);
                    entity.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.POISON, 40, 0));
                }
                default -> { /* safe or mild — no contact damage */ }
            }
        }
    }

    // ── Tick ────────────────────────────────────────────────────────────

    public static void tick(World world, BlockPos pos, BlockState state, BeakerLiquidBlockEntity be) {
        if (world.isClient) return;
        if (be.isEmpty()) return;

        // Newton's law of cooling each tick
        boolean onCoolingBlock = world.getBlockState(pos.down()).getBlock() instanceof CoolingBlock;
        double k = onCoolingBlock ? COOLING_K_COOLED : COOLING_K_NORMAL;
        be.effectiveMassG = onCoolingBlock ? SOLUTION_MASS_G * 10.0 : SOLUTION_MASS_G;

        if (Math.abs(be.temperatureK - AMBIENT_TEMP_K) > 0.01) {
            be.temperatureK -= k * (be.temperatureK - AMBIENT_TEMP_K);
            be.markDirty();
            be.syncToClient();
        }

        // Spawn steam particles if hot
        if (be.temperatureK > 373.15 && world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                    1, 0.1, 0.1, 0.1, 0.01);
        }

        // Check for thermal-expansion overflow
        be.checkAndHandleOverflow();

        // Remove gaseous compounds (they escape from an open beaker)
        Map<String, Double> gases = be.removeGases();
        if (!gases.isEmpty()) {
            // Spawn lingering gas cloud with appropriate effects
            GasCloudHelper.spawnGasClouds(world, pos, gases);
        }

        // Natural sedimentation of solid compounds
        be.tickSedimentation();

        // Temperature proximity damage (every DAMAGE_INTERVAL_TICKS)
        be.damageTick++;
        if (be.damageTick >= DAMAGE_INTERVAL_TICKS) {
            be.damageTick = 0;
            applyProximityTemperatureDamage(world, pos, be);
        }
    }

    /**
     * Apply damage/effects to living entities near a hot or cold beaker.
     * Hot beakers (>100 °C) cause fire damage; cold beakers (<−20 °C) cause
     * slowness and frost damage. Damage scales with temperature extremity.
     */
    private static void applyProximityTemperatureDamage(World world, BlockPos pos, BeakerLiquidBlockEntity be) {
        double temp = be.temperatureK;
        if (temp <= HOT_DAMAGE_THRESHOLD_K && temp >= COLD_DAMAGE_THRESHOLD_K) return;

        // Search for entities within 1.5 blocks of the beaker
        Box damageBox = new Box(pos).expand(0.5);
        List<LivingEntity> entities = world.getEntitiesByClass(
                LivingEntity.class, damageBox, e -> true);

        if (entities.isEmpty()) return;

        if (temp > HOT_DAMAGE_THRESHOLD_K) {
            // Hot damage: scales from 1 at 100°C to 6 at 600°C
            float damage = Math.min(6.0f, 1.0f + (float) ((temp - HOT_DAMAGE_THRESHOLD_K) / 100.0));
            for (LivingEntity entity : entities) {
                entity.damage(entity.getDamageSources().hotFloor(), damage);
                entity.setOnFireFor(1); // brief ignition
            }
        } else {
            // Cold damage: scales from 1 at −20°C to 4 at −200°C
            float damage = Math.min(4.0f, 1.0f + (float) ((COLD_DAMAGE_THRESHOLD_K - temp) / 80.0));
            int slowDuration = 40 + (int) ((COLD_DAMAGE_THRESHOLD_K - temp) / 5.0);
            int slowAmplifier = Math.min(3, (int) ((COLD_DAMAGE_THRESHOLD_K - temp) / 60.0));
            for (LivingEntity entity : entities) {
                entity.damage(entity.getDamageSources().freeze(), damage);
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS, slowDuration, slowAmplifier));
                entity.setFrozenTicks(Math.max(entity.getFrozenTicks(), 80));
            }
        }
    }

    // ── Beaker-specific ────────────────────────────────────────────────

    /**
     * Beaker cracks from thermal stress — destroys the block and contents.
     */
    private void crackBeaker() {
        if (world == null || world.isClient) return;

        world.playSound(null, pos, SoundEvents.BLOCK_GLASS_BREAK,
                SoundCategory.BLOCKS, 1.5f, 0.8f);

        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.CRIT,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    20, 0.3, 0.3, 0.3, 0.1);
            if (temperatureK > 373.15) {
                serverWorld.spawnParticles(ParticleTypes.CLOUD,
                        pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                        10, 0.2, 0.2, 0.2, 0.05);
            }
        }

        contents.clear();
        temperatureK = AMBIENT_TEMP_K;

        // Remove the block without dropping the beaker item (it's broken)
        world.removeBlock(pos, false);
    }
}
