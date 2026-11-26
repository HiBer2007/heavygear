package com.cecilia.heavygear.mixin;

import com.cecilia.heavygear.combat.SmashTopUp;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Heavy potion smash for mob victims. */
@Mixin(LivingEntity.class)
public abstract class EffectTopUpMixin {
    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void heavygear$rememberHealth(DamageSource source, float amount, CallbackInfo ci) {
        SmashTopUp.onHurtStart((LivingEntity) (Object) this);
    }

    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void heavygear$topUp(DamageSource source, float amount, CallbackInfo ci) {
        SmashTopUp.onHurtEnd((LivingEntity) (Object) this, source);
    }
}
