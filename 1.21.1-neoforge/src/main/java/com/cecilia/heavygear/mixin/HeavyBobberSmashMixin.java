package com.cecilia.heavygear.mixin;

import com.cecilia.heavygear.enchant.HeavyGearEnchantments;
import com.cecilia.heavygear.item.HeavyGear;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The heavy fishing rod's bobber smashes whatever creature it hooks, using the same rule as the heavy
 * arrow: the bobber must have fallen at least 1.5 blocks below its cast apex. The damage is half of a
 * same-height mace smash and the vanilla smash sound / particles are played.
 *
 * <p>Wind Burst on the rod throws the hooked creature into the air with the vanilla gust strength
 * (1.2 / 1.75 / 2.2 blocks for levels 1-3), in addition to the smash.</p>
 */
@Mixin(FishingHook.class)
public abstract class HeavyBobberSmashMixin {
    @Unique
    private static final double HEAVYGEAR_MIN_DROP = 1.5D;
    @Unique
    private static final float HEAVYGEAR_SMASH_ATTACK_DAMAGE = 8.0F;
    @Unique
    private static final float HEAVYGEAR_SMASH_SHARE = 0.5F;

    @Unique
    private double heavygear$peakY = Double.NEGATIVE_INFINITY;
    @Unique
    private int heavygear$smashedEntity = -1;
    @Unique
    private int heavygear$launchedEntity = -1;

    @Inject(method = "tick", at = @At("HEAD"))
    private void heavygear$trackCast(CallbackInfo ci) {
        FishingHook self = (FishingHook) (Object) this;
        if (self.getY() > this.heavygear$peakY) {
            this.heavygear$peakY = self.getY();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void heavygear$smashHooked(CallbackInfo ci) {
        FishingHook self = (FishingHook) (Object) this;
        Entity hooked = self.getHookedIn();
        if (!(hooked instanceof LivingEntity living) || !(self.level() instanceof ServerLevel level)) {
            return;
        }
        Player owner = self.getPlayerOwner();
        if (owner == null) {
            return;
        }
        ItemStack rod = heavygear$rod(owner);
        if (!rod.is(HeavyGear.HEAVY_FISHING_ROD)
                && !com.cecilia.heavygear.effect.HeavyEffect.has(owner)) {
            return;
        }
        double drop = this.heavygear$peakY - self.getY();
        if (drop < HEAVYGEAR_MIN_DROP) {
            // both the smash and the wind burst require the bobber to have fallen far enough
            return;
        }
        if (hooked.getId() != this.heavygear$smashedEntity
                && heavygear$smash(owner, living, drop, rod, level)) {
            this.heavygear$smashedEntity = hooked.getId();
        }
        // Wind Burst: the hooked creature is thrown upwards with the vanilla gust strength
        int windBurst = HeavyGearEnchantments.level(rod, Enchantments.WIND_BURST);
        if (windBurst > 0 && hooked.getId() != this.heavygear$launchedEntity) {
            this.heavygear$launchedEntity = hooked.getId();
            double up = 1.2D + 0.55D * (windBurst - 1);
            var motion = living.getDeltaMovement();
            living.setDeltaMovement(motion.x, Math.max(motion.y, up), motion.z);
            living.hurtMarked = true;
            level.playSound(null, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
                    living.getX(), living.getY(), living.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Unique
    private static boolean heavygear$smash(Player owner, LivingEntity victim, double drop,
                                           ItemStack rod, ServerLevel level) {
        // The smash is computed here instead of through MaceItem: vanilla refuses a smash while the
        // attacker stands in water or on a ladder, which is exactly where a fishing rod is used.
        int density = HeavyGearEnchantments.level(rod, Enchantments.DENSITY);
        double effective = Math.min(drop * (1.0D + 0.5D * density), 20.0D);
        float bonus = heavygear$maceCurve(effective);
        if (bonus <= 0.0F) {
            return false;
        }
        DamageSource source = owner.damageSources().playerAttack(owner);
        float damage = (bonus + HEAVYGEAR_SMASH_ATTACK_DAMAGE) * HEAVYGEAR_SMASH_SHARE;
        float healthBefore = victim.getHealth();
        boolean applied = victim.hurt(source, damage);
        // players often soak the blow with absorption hearts (health does not move) and some mobs
        // (endermen) report a success while cancelling it: only the health change can tell them apart
        boolean landed = applied && (victim instanceof Player
                ? true
                : victim.getHealth() < healthBefore - 0.001F);
        if (!landed) {
            return false;
        }
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0F, 1.0F);
        var ground = victim.getOnPos();
        level.levelEvent(2013, ground, 750);
        level.sendParticles(ParticleTypes.EXPLOSION,
                ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
        return true;
    }

    /** Vanilla mace smash bonus curve: 4·d up to 3, 12+2·(d-3) up to 8, 22+(d-8) above. */
    @Unique
    private static float heavygear$maceCurve(double distance) {
        if (distance <= 3.0D) {
            return (float) (4.0D * distance);
        }
        if (distance <= 8.0D) {
            return (float) (12.0D + 2.0D * (distance - 3.0D));
        }
        return (float) (22.0D + (distance - 8.0D));
    }

    @Unique
    private static ItemStack heavygear$rod(Player player) {
        ItemStack main = player.getMainHandItem();
        return main.is(HeavyGear.HEAVY_FISHING_ROD) ? main : player.getOffhandItem();
    }
}
