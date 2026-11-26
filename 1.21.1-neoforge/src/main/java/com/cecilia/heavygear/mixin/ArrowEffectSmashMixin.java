package com.cecilia.heavygear.mixin;

import com.cecilia.heavygear.effect.HeavyEffect;
import com.cecilia.heavygear.entity.HeavyArrow;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the heavy potion is active, ordinary arrows smash too: an arrow whose shooter carries the
 * effect deals the heavy arrow's 25% mace smash when it has fallen at least 1.5 blocks since it was
 * fired. The heavy arrow entity has its own implementation, so it is skipped here.
 */
@Mixin(AbstractArrow.class)
public abstract class ArrowEffectSmashMixin {
    @Unique
    private static final double HEAVYGEAR_MIN_DROP = 1.5D;
    @Unique
    private static final float HEAVYGEAR_ATTACK_DAMAGE = 8.0F;
    @Unique
    private static final float HEAVYGEAR_SHARE = 0.25F;

    @Unique
    private double heavygear$peakY = Double.NEGATIVE_INFINITY;

    @Inject(method = "tick", at = @At("HEAD"))
    private void heavygear$trackArrow(CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (self.getY() > this.heavygear$peakY) {
            this.heavygear$peakY = self.getY();
        }
    }

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void heavygear$smashOnHit(net.minecraft.world.phys.EntityHitResult result, CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (self instanceof HeavyArrow) {
            return;
        }
        Entity target = result.getEntity();
        if (!(target instanceof LivingEntity living) || !(self.level() instanceof ServerLevel level)) {
            return;
        }
        if (!HeavyEffect.has(self.getOwner())) {
            return;
        }
        double drop = this.heavygear$peakY - self.getY();
        if (drop < HEAVYGEAR_MIN_DROP) {
            return;
        }
        float bonus = heavygear$maceCurve(Math.min(drop, 20.0D));
        if (bonus <= 0.0F) {
            return;
        }
        DamageSource source = self.damageSources().arrow(self, self.getOwner());
        living.invulnerableTime = 0;
        float healthBefore = living.getHealth();
        boolean applied = living.hurt(source, (bonus + HEAVYGEAR_ATTACK_DAMAGE) * HEAVYGEAR_SHARE);
        if (!applied || (!(living instanceof net.minecraft.world.entity.player.Player)
                && living.getHealth() >= healthBefore - 0.001F)) {
            return;
        }
        com.cecilia.heavygear.combat.SmashAttribution.record(living);
        level.playSound(null, living.getX(), living.getY(), living.getZ(),
                SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0F, 1.0F);
        var ground = living.getOnPos();
        level.levelEvent(2013, ground, 750);
        level.sendParticles(ParticleTypes.EXPLOSION,
                ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
    }

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
}
