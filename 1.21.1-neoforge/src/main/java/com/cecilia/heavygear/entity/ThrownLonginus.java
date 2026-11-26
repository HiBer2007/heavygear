package com.cecilia.heavygear.entity;

import com.cecilia.heavygear.HeavyGearMod;
import com.cecilia.heavygear.enchant.HeavyGearEnchantments;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Thrown Spear of Longinus. Uses the trident projectile logic, and additionally performs a mace style
 * smash on entity hits and returns to the owner when it has the Loyalty enchantment.
 */
public class ThrownLonginus extends ThrownTrident {
    private static final EntityDataAccessor<Byte> DATA_LOYALTY =
            SynchedEntityData.defineId(ThrownLonginus.class, EntityDataSerializers.BYTE);

    private double heavygear$peakY = Double.NEGATIVE_INFINITY;
    private Entity heavygear$smashTarget;
    private boolean heavygear$smashed;
    private boolean heavygear$hit;
    private int heavygear$returnTicks;
    private int heavygear$hitDelay;

    public ThrownLonginus(EntityType<? extends ThrownLonginus> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LOYALTY, (byte) 0);
    }

    public static ThrownLonginus create(Level level, LivingEntity owner, ItemStack stack) {
        ThrownLonginus spear = new ThrownLonginus(HeavyGearMod.THROWN_LONGINUS_TYPE, level);
        spear.setOwner(owner);
        spear.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        spear.setPickupItemStack(stack.copy());
        spear.heavygear$peakY = spear.getY();
        if (level instanceof ServerLevel serverLevel) {
            int loyalty = EnchantmentHelper.getTridentReturnToOwnerAcceleration(serverLevel, stack, owner);
            spear.entityData.set(DATA_LOYALTY, (byte) net.minecraft.util.Mth.clamp(loyalty, 0, 127));
        }
        return spear;
    }

    /**
     * Vanilla keeps the glint in a synced flag that only its own constructor fills, and a custom entity
     * type cannot use that constructor - so the flag stayed false and an enchanted spear flew without
     * the enchantment glint. Deriving it from the carried stack fixes both sides at once.
     */
    @Override
    public boolean isFoil() {
        return this.getPickupItemStackOrigin().hasFoil() || super.isFoil();
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.heavygear$hit = true;
        }
        if (this.heavygear$hitDelay > 0) {
            this.heavygear$hitDelay--;
        }
        if (this.heavygear$hitDelay <= 0) {
            this.heavygear$loyaltyReturn();
        }
        super.tick();
        if (this.getY() > this.heavygear$peakY) {
            this.heavygear$peakY = this.getY();
        }
    }

    private void heavygear$loyaltyReturn() {
        int loyalty = this.entityData.get(DATA_LOYALTY);
        if (loyalty <= 0 || (!this.heavygear$hit && !this.isNoPhysics())) {
            return;
        }
        Entity owner = this.getOwner();
        if (owner == null || !owner.isAlive()) {
            return;
        }
        this.setNoPhysics(true);
        Vec3 toOwner = owner.getEyePosition().subtract(this.getX(), this.getY(), this.getZ());
        this.setPosRaw(this.getX(), this.getY() + toOwner.y * 0.015D * loyalty, this.getZ());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.95D)
                .add(toOwner.normalize().scale(0.05D * loyalty)));
        if (this.heavygear$returnTicks == 0 && !this.level().isClientSide()) {
            this.playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
        }
        this.heavygear$returnTicks++;
        if (!this.level().isClientSide() && owner instanceof ServerPlayer player
                && this.distanceTo(player) < 1.5F) {
            player.getInventory().placeItemBackInInventory(this.getPickupItemStackOrigin().copy());
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        this.heavygear$smashTarget = result.getEntity();
        this.heavygear$hit = true;
        this.heavygear$hitDelay = 8;
        super.onHitEntity(result);
        this.heavygear$smashTarget = null;
        if (this.level() instanceof ServerLevel serverLevel) {
            this.heavygear$smash(serverLevel, result.getEntity());
            if (!result.getEntity().isAlive() && this.getOwner() instanceof ServerPlayer owner) {
                // "百发百中": only a thrown spear that kills counts, so it is awarded from the projectile
                // itself.
                var advancement = serverLevel.getServer().getAdvancements()
                        .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                "heavygear", "thrown_lance_kill"));
                if (advancement != null) {
                    owner.getAdvancements().award(advancement, "thrown_kill");
                }
            }
            int windBurst = HeavyGearEnchantments.level(this.getPickupItemStackOrigin(), Enchantments.WIND_BURST);
            if (windBurst > 0) {
                Vec3 delta = this.getDeltaMovement();
                double vy = (0.5D + 0.2D * windBurst) * 1.732D;
                this.setDeltaMovement(delta.x * 0.6D, vy, delta.z * 0.6D);
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.0F, 1.0F);
                serverLevel.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
                        this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                serverLevel.sendParticles(ParticleTypes.GUST_EMITTER_SMALL,
                        this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        this.heavygear$hit = true;
        this.heavygear$hitDelay = 8;
    }

    private void heavygear$smash(ServerLevel level, Entity target) {
        if (this.heavygear$smashed) {
            return;
        }
        double fall = this.heavygear$fallHeight();
        if (fall < 1.0) {
            return;
        }
        float bonus = this.heavygear$smashBonus();
        if (bonus <= 0.0F) {
            return;
        }
        this.heavygear$smashed = true;
        HeavyGearMod.LOGGER.info("longinus smash: fall={} bonus={}", fall, bonus);

        // Mace smash sound at the target.
        var sound = fall > 5.0 ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND;
        level.playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        // Smash particles on the block the target stands on, like the mace.
        net.minecraft.core.BlockPos ground = target.getOnPos();
        level.levelEvent(2013, ground, 750);
        level.sendParticles(ParticleTypes.EXPLOSION, ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5,
                1, 0.0, 0.0, 0.0, 0.0);
    }

    public boolean heavygear$isSmashArmed() {
        return this.heavygear$smashTarget != null && this.heavygear$fallHeight() > 0.0;
    }

    public double heavygear$fallHeight() {
        return Math.max(0.0, this.heavygear$peakY - this.getY());
    }

    /** Same curve as MaceItem#getAttackDamageBonus, plus the Density enchantment. */
    public float heavygear$smashBonus() {
        double fall = this.heavygear$fallHeight();
        if (fall <= 0.0) {
            return 0.0F;
        }
        double bonus;
        if (fall <= 3.0) {
            bonus = 4.0 * fall;
        } else if (fall <= 8.0) {
            bonus = 12.0 + 2.0 * (fall - 3.0);
        } else {
            bonus = fall + 14.0;
        }
        if (this.level() instanceof ServerLevel serverLevel && this.heavygear$smashTarget != null) {
            Entity owner = this.getOwner();
            DamageSource source = this.damageSources().trident(this, owner == null ? this : owner);
            float density = EnchantmentHelper.modifyFallBasedDamage(serverLevel, this.getWeaponItem(),
                    this.heavygear$smashTarget, source, 0.0F);
            bonus += density * fall;
        }
        return (float) bonus;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(HeavyGearMod.LONGINUS_SPEAR);
    }
}
