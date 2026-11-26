package com.cecilia.heavygear.mixin;

import com.cecilia.heavygear.dive.DiveState;
import com.cecilia.heavygear.dive.DiveTracker;
import com.cecilia.heavygear.enchant.HeavyGearEnchantments;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.MaceItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While gliding, fallDistance stays 0, so the vanilla mace smash bonus is 0. Feed the tracked dive
 * distance into the vanilla calculation so the smash bonus (and Density) stay inside the normal attack
 * and Smite/Sharpness are not lost. The value is restored before the critical-hit check so dive hits do
 * not silently become critical hits.
 */
@Mixin(MaceItem.class)
public abstract class DiveSmashDamageMixin {
    @Inject(method = "getAttackDamageBonus", at = @At("HEAD"))
    private void heavygear$beginDiveBonus(Entity target, float damage, DamageSource source,
                                          CallbackInfoReturnable<Float> cir) {
        Entity direct = source.getDirectEntity();
        if (!(direct instanceof LivingEntity attacker)
                || !attacker.isFallFlying()
                || HeavyGearEnchantments.diveBombingLevel(attacker.getWeaponItem()) <= 0) {
            DiveState.push(Double.NaN);
            return;
        }
        double descended = DiveTracker.descended(attacker);
        if (descended < 1.5) {
            DiveState.push(Double.NaN);
            return;
        }
        DiveState.push(attacker.fallDistance);
        attacker.fallDistance = (float) Math.min(descended, 20.0);
    }

    @Inject(method = "getAttackDamageBonus", at = @At("RETURN"))
    private void heavygear$endDiveBonus(Entity target, float damage, DamageSource source,
                                        CallbackInfoReturnable<Float> cir) {
        double saved = DiveState.pop();
        if (Double.isNaN(saved)) {
            return;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof LivingEntity attacker) {
            attacker.fallDistance = (float) saved;
        }
    }
}
