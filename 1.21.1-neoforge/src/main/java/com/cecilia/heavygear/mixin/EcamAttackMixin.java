package com.cecilia.heavygear.mixin;

import com.cecilia.heavygear.enchant.HeavyGearEnchantments;
import com.cecilia.heavygear.item.HeavyGear;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ecaM / the reverse mace (锤重).
 *
 * <p>The swing never damages the victim: the damage a mace would have dealt is paid by the wielder and
 * knocks the player away from the target. The numbers mirror the mace exactly: the base part is the
 * attack damage scaled by the attack strength and, when falling more than 1.5 blocks, the whole smash
 * value of that height (Density and Breach included) is added on top, together with the mace hit stop
 * and effect set. Wind Burst launches the player up.</p>
 */
@Mixin(Player.class)
public abstract class EcamAttackMixin {
    @Unique
    private static final double HEAVYGEAR_MIN_DROP = 1.5D;
    @Unique
    private static final double HEAVYGEAR_MAX_DROP = 20.0D;
    @Unique
    private static final double HEAVYGEAR_HEAVY_FALL = 5.0D;

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void heavygear$ecamSwing(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        ItemStack weapon = self.getMainHandItem();
        if (!weapon.is(HeavyGear.ECAM)) {
            return;
        }
        // the reverse mace never damages what it hits
        ci.cancel();
        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }
        self.resetAttackStrengthTicker();
        if (!(target instanceof LivingEntity) || !target.isAlive()) {
            return;
        }
        // the ordinary player attack source, so the vanilla bonus helper sees this player as attacker
        DamageSource source = level.damageSources().playerAttack(self);
        double fall = Math.min(self.fallDistance, HEAVYGEAR_MAX_DROP);
        boolean smashing = fall >= HEAVYGEAR_MIN_DROP && !self.isFallFlying();

        // vanilla swing scaling: a hit without charge is weak, exactly like the mace
        float strength = self.getAttackStrengthScale(0.5F);
        float scale = 0.2F + strength * strength * 0.8F;
        float selfDamage = (float) self.getAttributeValue(Attributes.ATTACK_DAMAGE) * scale;
        if (smashing) {
            selfDamage += Items.MACE.getAttackDamageBonus(self, 1.0F, source);
        }
        self.invulnerableTime = 0;
        self.hurt(source, selfDamage);

        // dying to your own reverse mace earns the why bother advancement
        if (!self.isAlive() && self instanceof ServerPlayer dying && level.getServer() != null) {
            var holder = level.getServer().getAdvancements().get(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("heavygear", "why_bother"));
            if (holder != null) {
                dying.getAdvancements().award(holder, "self_smash");
            }
        }

        // knocked back, away from whatever was hit
        double dx = self.getX() - target.getX();
        double dz = self.getZ() - target.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-3D) {
            dx = 0.0D;
            dz = 1.0D;
            length = 1.0D;
        }
        self.push(dx / length * 0.55D, 0.3D, dz / length * 0.55D);
        self.hurtMarked = true;

        if (!smashing) {
            level.playSound(null, self.getX(), self.getY(), self.getZ(),
                    SoundEvents.MACE_SMASH_AIR, self.getSoundSource(), 0.8F, 1.2F);
            level.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                    8, 0.3, 0.3, 0.3, 0.0);
            return;
        }

        // ---- complete vanilla mace smash effect set ----
        if (self instanceof ServerPlayer serverPlayer) {
            serverPlayer.setSpawnExtraParticlesOnFall(true);
        }
        SoundEvent sound = fall > HEAVYGEAR_HEAVY_FALL ? SoundEvents.MACE_SMASH_GROUND_HEAVY
                : SoundEvents.MACE_SMASH_GROUND;
        level.playSound(null, self.getX(), self.getY(), self.getZ(), sound,
                self.getSoundSource(), 1.0F, 1.0F);
        level.levelEvent(2013, self.getOnPos(), 750);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                self.getX(), self.getY() + 0.2D, self.getZ(), 1, 0.0, 0.0, 0.0, 0.0);

        // hit stop: the fall ends dead
        self.fallDistance = 0.0F;
        self.setDeltaMovement(0.0D, 0.01D, 0.0D);
        self.setSprinting(false);
        self.hurtMarked = true;

        // Wind Burst throws the wielder up, exactly like the mace launches its user
        int windBurst = HeavyGearEnchantments.level(weapon, Enchantments.WIND_BURST);
        if (windBurst > 0) {
            double up = 1.2D + 0.55D * (windBurst - 1);
            self.setDeltaMovement(0.0D, up, 0.0D);
            self.hurtMarked = true;
            level.playSound(null, self.getX(), self.getY(), self.getZ(),
                    SoundEvents.WIND_CHARGE_BURST, self.getSoundSource(), 1.0F, 1.0F);
            level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
                    self.getX(), self.getY(), self.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
