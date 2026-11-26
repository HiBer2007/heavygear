package com.cecilia.heavygear.mixin;

import com.cecilia.heavygear.dive.DiveState;
import com.cecilia.heavygear.dive.DiveTracker;
import com.cecilia.heavygear.enchant.HeavyGearEnchantments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wind Burst only triggers when the attacker is not flying and has fallen at least 1.5 blocks. During
 * an elytra dive neither is true, so hide the gliding state and expose the tracked dive distance for
 * the duration of the post-attack effects.
 */
@Mixin(EnchantmentHelper.class)
public abstract class DiveWindBurstMixin {
    @Inject(method = "doPostAttackEffectsWithItemSource", at = @At("HEAD"))
    private static void heavygear$beginDiveEffects(ServerLevel level, Entity target, DamageSource source,
                                                   ItemStack stack, CallbackInfo ci) {
        Entity direct = source.getDirectEntity();
        if (!(direct instanceof LivingEntity attacker)
                || !attacker.isFallFlying()
                || HeavyGearEnchantments.diveBombingLevel(stack) <= 0) {
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
        ((EntitySharedFlagAccessor) attacker).heavygear$setSharedFlag(7, false);
    }

    @Inject(method = "doPostAttackEffectsWithItemSource", at = @At("RETURN"))
    private static void heavygear$endDiveEffects(ServerLevel level, Entity target, DamageSource source,
                                                 ItemStack stack, CallbackInfo ci) {
        double saved = DiveState.pop();
        if (Double.isNaN(saved)) {
            return;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof LivingEntity attacker) {
            attacker.fallDistance = (float) saved;
            ((EntitySharedFlagAccessor) attacker).heavygear$setSharedFlag(7, true);
            DiveTracker.clear(attacker);
        }
    }
}
