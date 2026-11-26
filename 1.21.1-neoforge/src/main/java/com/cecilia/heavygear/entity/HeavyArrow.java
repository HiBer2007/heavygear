package com.cecilia.heavygear.entity;

import com.cecilia.heavygear.item.HeavyGear;
import com.cecilia.heavygear.mixin.AbstractArrowPierceAccessor;
import com.cecilia.heavygear.mixin.AbstractArrowWeaponAccessor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The heavy arrow. Behaves like a vanilla arrow, but when it has dropped at least 1.5 blocks below the
 * highest point of its flight it also performs a share of a same-height mace smash on the target it
 * hits.
 *
 * <p>The smash uses the vanilla mace damage curve, with Density from the firing weapon stretching the
 * effective fall distance (the same way vanilla Density works) and Breach coming along automatically
 * through {@link DamageSource#getWeaponItem()} (the bow is the arrow's weapon stack).</p>
 */
public class HeavyArrow extends Arrow {
    /** player uuid -> {tick, arrows fired this tick}, used to keep only the first multishot arrow. */
    private static final java.util.Map<java.util.UUID, int[]> HEAVYGEAR_MULTISHOT =
            new java.util.concurrent.ConcurrentHashMap<>();
    /** Same threshold the vanilla mace uses for a smash attack. */
    private static final double MIN_DROP = 1.5D;
    /** Attack damage of a mace swing (mace 7 + player base 1), matching the armour smash. */
    private static final float SMASH_ATTACK_DAMAGE = 8.0F;
    /** Share of a same-height mace smash the heavy arrow deals. */
    private static final float SMASH_SHARE = 0.25F;
    /** Upper bound for the effective fall distance. */
    private static final double MAX_DROP = 20.0D;

    private double heavygear$peakY = Double.NEGATIVE_INFINITY;
    /** Smash damage of the previous pierced target, so a piercing arrow never hits softer. */
    private float heavygear$lastSmash;
    /** Victim of the most recent landed smash, for the heavy death message. */
    private int heavygear$smashVictimId = -1;
    private int heavygear$smashTick = -100;

    public HeavyArrow(EntityType<? extends HeavyArrow> type, Level level) {
        super(type, level);
    }

    public HeavyArrow(EntityType<? extends HeavyArrow> type, LivingEntity owner, Level level,
                      ItemStack pickupItemStack, ItemStack firedFromWeapon) {
        super(type, level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
        this.setPickupItemStack(pickupItemStack);
        ((AbstractArrowWeaponAccessor) this).heavygear$setFiredFromWeapon(firedFromWeapon);
        // Vanilla computes the pierce level inside the full AbstractArrow constructor, which a custom
        // entity type cannot use. Mirror it here so Piercing on the crossbow lets the arrow pass
        // through creatures with vanilla's own "pierce level + 1 targets" rules.
        if (level instanceof ServerLevel serverLevel) {
            int pierce = EnchantmentHelper.getPiercingCount(serverLevel, firedFromWeapon, pickupItemStack);
            if (pierce > 0) {
                ((AbstractArrowPierceAccessor) this).heavygear$setPierceLevel((byte) pierce);
            }
        }
        // Same rule the vanilla bow uses: arrows fired from infinite material (creative) or with
        // Infinity are not handed to any inventory.
        boolean infinite = owner instanceof net.minecraft.world.entity.player.Player player
                && player.getAbilities().instabuild;
        if (!infinite) {
            infinite = com.cecilia.heavygear.enchant.HeavyGearEnchantments.level(
                    firedFromWeapon, Enchantments.INFINITY) > 0;
        }
        this.pickup = infinite ? Pickup.CREATIVE_ONLY : Pickup.ALLOWED;
        heavygear$multishotPickup(firedFromWeapon, owner);
        this.heavygear$peakY = this.getY();
    }

    /**
     * Multishot fires several arrows from a single charge. Only the first one of that volley may be
     * picked up again, otherwise one arrow becomes three every time it is fired.
     */
    private void heavygear$multishotPickup(ItemStack weapon, Entity owner) {
        if (this.pickup == Pickup.CREATIVE_ONLY || !(owner instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        if (com.cecilia.heavygear.enchant.HeavyGearEnchantments.level(weapon, Enchantments.MULTISHOT) <= 0) {
            return;
        }
        int[] state = HEAVYGEAR_MULTISHOT.computeIfAbsent(player.getUUID(), key -> new int[]{-1, 0});
        if (state[0] != player.tickCount) {
            state[0] = player.tickCount;
            state[1] = 0;
        }
        if (state[1]++ > 0) {
            this.pickup = Pickup.CREATIVE_ONLY;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getY() > this.heavygear$peakY) {
            this.heavygear$peakY = this.getY();
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(HeavyGear.HEAVY_ARROW);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        double drop = this.heavygear$peakY - this.getY();
        Vec3 motion = this.getDeltaMovement();
        // Density of the firing weapon adds the vanilla mace fall damage to the arrow hit itself
        // (+0.5 damage per level per fallen block). Restored after the vanilla hit so piercing arrows
        // do not stack the bonus from one target to the next.
        double originalBaseDamage = ((AbstractArrowWeaponAccessor) this).heavygear$getBaseDamage();
        int density = com.cecilia.heavygear.enchant.HeavyGearEnchantments.level(
                this.getWeaponItem(), Enchantments.DENSITY);
        if (density > 0) {
            double fall = Math.min(Math.max(drop, 0.0D), MAX_DROP);
            if (fall > 0.0D) {
                this.setBaseDamage(originalBaseDamage + 0.5D * density * fall);
            }
        }
        // The smash is applied *before* the vanilla arrow damage: a target whose remaining health is
        // below the arrow's own damage used to die from that hit and swallow the smash entirely.
        boolean smashKill = heavygear$applySmash(target, drop);
        // the smash consumed the hurt window of this tick, so reopen it for the vanilla arrow hit
        if (target instanceof LivingEntity living) {
            living.invulnerableTime = 0;
        }
        super.onHitEntity(result);
        this.setBaseDamage(originalBaseDamage);
        // Wind Burst has to be applied *after* the vanilla hit: vanilla's own knockback overwrites the
        // vertical motion of every mob that is not knockback resistant.
        heavygear$windBurst(target, drop);
        if (this.getPierceLevel() > 0) {
            // a piercing arrow must keep flying: vanilla sees a dead body, treats the hit as failed and
            // deflects the arrow, so the original velocity is restored
            if (smashKill) {
                this.setDeltaMovement(motion);
            }
            return;
        }
        // Vanilla consumes a non piercing arrow on every entity hit. When the smash already killed the
        // target, vanilla's hurt call fails and that path is skipped, so consume the arrow here.
        if (this.isAlive()) {
            this.discard();
        }
    }

    /**
     * Wind Burst on the bow / crossbow: the creature the arrow hits is thrown into the air, using the
     * vanilla gust strength (1.2 / 1.75 / 2.2 for levels 1-3), but only after a real fall.
     */
    private void heavygear$windBurst(Entity target, double drop) {
        if (!(target instanceof LivingEntity living) || drop < MIN_DROP) {
            return;
        }
        int windBurst = com.cecilia.heavygear.enchant.HeavyGearEnchantments.level(
                this.getWeaponItem(), Enchantments.WIND_BURST);
        if (windBurst <= 0) {
            return;
        }
        double up = 1.2D + 0.55D * (windBurst - 1);
        Vec3 motion = living.getDeltaMovement();
        living.setDeltaMovement(motion.x, Math.max(motion.y, up), motion.z);
        living.hurtMarked = true;
        if (this.level() instanceof ServerLevel level) {
            level.playSound(null, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
                    living.getX(), living.getY(), living.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /**
     * Vanilla mace smash on {@code target} when the arrow has fallen far enough.
     *
     * @return true when the smash killed the target
     */
    private boolean heavygear$applySmash(Entity target, double drop) {
        if (!(target instanceof LivingEntity living) || drop < MIN_DROP) {
            return false;
        }
        int density = com.cecilia.heavygear.enchant.HeavyGearEnchantments.level(
                this.getWeaponItem(), Enchantments.DENSITY);
        // Vanilla Density is "smash_damage_per_fallen_block": +0.5 damage per level per fallen block.
        // The arrow hit itself already carries that bonus (see onHitEntity), so the smash keeps the
        // plain mace curve to avoid counting Density twice.
        double height = Math.min(drop, MAX_DROP);
        float bonus = heavygear$maceCurve(height);
        if (bonus <= 0.0F) {
            return false;
        }
        DamageSource source = this.damageSources().arrow(this, this.getOwner());
        living.invulnerableTime = 0;
        // Piercing walks the arrow down its arc, so the drop (and therefore the damage) grows with every
        // target it passes through. Clamp to the previous hit so the ramp never dips.
        // the heavy potion doubles the arrow smash (25% -> 50% of the mace value)
        float share = com.cecilia.heavygear.effect.HeavyEffect.has(this.getOwner())
                ? SMASH_SHARE * 2.0F : SMASH_SHARE;
        float smash = Math.max(bonus * share, this.heavygear$lastSmash);
        // record the attribution first: the death message is built inside this very hurt call
        int previousVictim = this.heavygear$smashVictimId;
        int previousTick = this.heavygear$smashTick;
        this.heavygear$smashVictimId = living.getId();
        this.heavygear$smashTick = this.tickCount;
        float healthBefore = living.getHealth();
        boolean applied = living.hurt(source, smash);
        // require the health to actually move too: some mobs (endermen dodging projectiles) report a
        // successful hurt while cancelling the damage, and must not get the smash effects either
        boolean playerTarget = living instanceof net.minecraft.world.entity.player.Player;
        if (!applied || (!playerTarget && living.getHealth() >= healthBefore - 0.001F)) {
            this.heavygear$smashVictimId = previousVictim;
            this.heavygear$smashTick = previousTick;
            return false;
        }
        this.heavygear$lastSmash = smash;
        if (this.level() instanceof ServerLevel serverLevel) {
            var sound = height > 5.0D ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND;
            serverLevel.playSound(null, living.getX(), living.getY(), living.getZ(), sound,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            var ground = living.getOnPos();
            serverLevel.levelEvent(2013, ground, 750);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
        }
        return !living.isAlive();
    }

    /** True when this arrow's smash damage actually landed on {@code victim} (used for death messages). */
    public boolean heavygear$didSmash(Entity victim) {
        return victim.getId() == this.heavygear$smashVictimId
                && this.tickCount - this.heavygear$smashTick <= 1;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        // A stuck arrow must not keep the height it fell from: otherwise it silently accumulates a
        // huge smash height and hits whatever frees it later with a full-height smash.
        this.heavygear$peakY = this.getY();
    }

    /** Vanilla mace smash bonus curve: 4·d up to 3, 12+2·(d-3) up to 8, 22+(d-8) above. */
    private static float heavygear$maceCurve(double distance) {
        if (distance <= 3.0D) {
            return (float) (4.0D * distance);
        }
        if (distance <= 8.0D) {
            return (float) (12.0D + 2.0D * (distance - 3.0D));
        }
        return (float) (22.0D + (distance - 8.0D));
    }
}
