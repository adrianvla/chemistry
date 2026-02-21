package org.keke.chemistry.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.keke.chemistry.item.AbstractContainerItem;
import org.keke.chemistry.item.BeakerItem;
import org.keke.chemistry.item.ModItems;
import org.keke.chemistry.utils.CompoundPhysicalData;
import org.keke.chemistry.utils.DrinkingEffects;
import org.keke.chemistry.utils.ExplosiveCompounds;
import org.keke.chemistry.utils.GasCloudHelper;
import org.keke.chemistry.utils.MaterialInteraction;
import org.keke.chemistry.utils.ModDamageSources;
import org.keke.chemistry.utils.StateOfMatter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thrown chemistry container entity.
 * 
 * Extends ThrownItemEntity — when it hits a block or entity, the container
 * shatters and its contents interact with the environment:
 *
 *   - Explosive compounds detonate (radius based on ExplosiveCompounds algorithm)
 *   - Corrosive/toxic compounds apply splash effects via DrinkingEffects
 *   - Material interactions via MaterialInteraction (fire, corrosion, freezing)
 *   - Gas clouds spawned via GasCloudHelper
 *   - Glass shatter particles and sound
 *
 * NBT carried from the item stack includes the full contents map and temperature.
 */
public class ThrownContainerEntity extends ThrownItemEntity {

    public ThrownContainerEntity(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    public ThrownContainerEntity(World world, LivingEntity owner) {
        super(ModEntityTypes.THROWN_CONTAINER, owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BEAKER;
    }

    // ── Contents from item stack ───────────────────────────────────────

    private Map<String, Double> getContents() {
        ItemStack stack = this.getStack();
        if (stack.getItem() instanceof BeakerItem) {
            return BeakerItem.getContents(stack);
        } else if (stack.getItem() instanceof AbstractContainerItem) {
            return AbstractContainerItem.getContents(stack);
        }

        // Fallback: read directly from NBT
        Map<String, Double> map = new LinkedHashMap<>();
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("contents", NbtElement.COMPOUND_TYPE)) {
            NbtCompound contentsNbt = nbt.getCompound("contents");
            for (String key : contentsNbt.getKeys()) {
                double val = contentsNbt.getDouble(key);
                if (val > 0.001) map.put(key, val);
            }
        }
        return map;
    }

    private double getTemperatureK() {
        ItemStack stack = this.getStack();
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("temperatureK")) {
            return nbt.getDouble("temperatureK");
        }
        return 293.15;
    }

    // ── Impact handling ────────────────────────────────────────────────

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);

        if (this.getWorld().isClient) return;

        Map<String, Double> contents = getContents();
        double tempK = getTemperatureK();
        BlockPos impactPos = BlockPos.ofFloored(hitResult.getPos());

        // Glass break effect
        spawnBreakParticles();
        this.getWorld().playSound(null, impactPos,
                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS,
                1.0f, 1.2f);

        if (!contents.isEmpty()) {
            // 1. Check for explosive detonation
            handleExplosion(impactPos, contents);

            // 2. Apply splash damage to nearby entities
            handleSplashDamage(impactPos, contents, tempK);

            // 3. Material interactions (fire, corrosion, freezing)
            MaterialInteraction.applySplashEffects(
                    this.getWorld(), impactPos, contents, tempK, 3);

            // 4. Gas cloud generation
            Map<String, Double> gases = new LinkedHashMap<>();
            for (var entry : contents.entrySet()) {
                StateOfMatter som = CompoundPhysicalData.getStateAtTemperature(
                        entry.getKey(), tempK);
                if (som == StateOfMatter.GAS) {
                    gases.put(entry.getKey(), entry.getValue());
                }
            }
            if (!gases.isEmpty()) {
                GasCloudHelper.spawnGasClouds(this.getWorld(), impactPos, gases);
            }
        }

        this.discard();
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        // Direct hit damage: 1.0 base + chemical effects applied in onCollision
        if (entityHitResult.getEntity() instanceof LivingEntity target) {
            DamageSource source = ModDamageSources.labAccident(this.getWorld());
            target.damage(source, 1.0f);
        }
    }

    // ── Explosion handling ─────────────────────────────────────────────

    private void handleExplosion(BlockPos pos, Map<String, Double> contents) {
        double explosionPower = ExplosiveCompounds.calculateTotalExplosionPower(contents);

        // Also check thermite
        if (MaterialInteraction.isThermiteMixture(contents)) {
            double thermitePowerKJ = MaterialInteraction.getThermitePower(contents);
            // Thermite: incendiary rather than explosive
            // Creates intense fire but less blast
            double thermiteBlast = Math.min(6.0, thermitePowerKJ / 200.0);
            explosionPower = Math.max(explosionPower, thermiteBlast);
        }

        if (explosionPower > 0.5) {
            // Create explosion with appropriate power
            this.getWorld().createExplosion(
                    this.getOwner(),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    (float) explosionPower,
                    explosionPower > 3.0, // causes fire for powerful explosions
                    World.ExplosionSourceType.TNT
            );
        }
    }

    // ── Splash damage ──────────────────────────────────────────────────

    private void handleSplashDamage(BlockPos pos, Map<String, Double> contents, double tempK) {
        World world = this.getWorld();
        Box splashBox = new Box(pos).expand(3.0);
        List<LivingEntity> entities = world.getEntitiesByClass(
                LivingEntity.class, splashBox, e -> true);

        for (LivingEntity entity : entities) {
            double distance = entity.squaredDistanceTo(
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            double distanceFactor = Math.max(0.1, 1.0 - Math.sqrt(distance) / 4.0);

            // Scale contents by distance
            Map<String, Double> scaledContents = new LinkedHashMap<>();
            for (var entry : contents.entrySet()) {
                scaledContents.put(entry.getKey(), entry.getValue() * distanceFactor);
            }

            // Apply chemical effects
            DrinkingEffects.applyEffects(entity, scaledContents);
            DrinkingEffects.applyTemperatureEffects(entity, tempK,
                    scaledContents.values().stream().mapToDouble(Double::doubleValue).sum());
        }
    }

    // ── Particles ──────────────────────────────────────────────────────

    private void spawnBreakParticles() {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            ItemStack stack = this.getStack();
            serverWorld.spawnParticles(
                    new ItemStackParticleEffect(ParticleTypes.ITEM, stack),
                    this.getX(), this.getY(), this.getZ(),
                    8, 0.2, 0.2, 0.2, 0.05);
        }
    }
}
