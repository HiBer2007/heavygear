package com.cecilia.heavygear.combat;

import com.cecilia.heavygear.effect.HeavyEffect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Adds the missing part of a mace smash to any melee hit made by a player under the heavy potion.
 *
 * <p>Works on the health delta of the hit, so it behaves the same for mobs and for players (players
 * have their own {@code actuallyHurt} override, which is why mob and player victims are hooked in two
 * separate mixins): the weapon's own attack damage stays in the total and only the smash bonus is
 * added, and a weapon that already folded the bonus into its damage (mace, heavy spear) is left
 * alone.</p>
 */
public final class SmashTopUp {
    private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Float> HEALTH = new ThreadLocal<>();

    private SmashTopUp() {
    }

    public static void onHurtStart(LivingEntity victim) {
        HEALTH.set(victim.getHealth());
    }

    public static void onHurtEnd(LivingEntity victim, DamageSource source) {
        Float before = HEALTH.get();
        HEALTH.remove();
        if (before == null || BUSY.get()) {
            return;
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player player) || player == victim || !HeavyEffect.has(player)) {
            return;
        }
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        double fall = Math.min(player.fallDistance, 20.0D);
        // ranged hits are far away by definition, so the melee reach check only applies to melee
        boolean ranged = source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow;
        if (fall < 1.5D || player.isFallFlying()
                || (!ranged && player.distanceToSqr(victim) > 36.0D)) {
            return;
        }
        if (SmashAttribution.didSmash(victim)) {
            // an armour style smash or a mace weapon already dealt its own smash
            return;
        }
        float dealt = before - victim.getHealth();
        float bonus = maceCurve(fall);
        float weaponDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (dealt >= weaponDamage + bonus - 1.0F) {
            return;
        }
        float missing = bonus - Math.max(0.0F, dealt - weaponDamage);
        if (missing <= 0.5F) {
            return;
        }
        DamageSource smash = level.damageSources().playerAttack(player);
        BUSY.set(Boolean.TRUE);
        try {
            victim.invulnerableTime = 0;
            if (!victim.hurt(smash, missing)) {
                return;
            }
            SmashAttribution.record(victim);
            level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0F, 1.0F);
            var ground = victim.getOnPos();
            level.levelEvent(2013, ground, 750);
            level.sendParticles(ParticleTypes.EXPLOSION,
                    ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
        } finally {
            BUSY.set(Boolean.FALSE);
        }
    }

    /** Vanilla mace smash curve: 4·d up to 3, 12+2·(d-3) up to 8, 22+(d-8) above. */
    public static float maceCurve(double distance) {
        if (distance <= 3.0D) {
            return (float) (4.0D * distance);
        }
        if (distance <= 8.0D) {
            return (float) (12.0D + 2.0D * (distance - 3.0D));
        }
        return (float) (22.0D + (distance - 8.0D));
    }
}
