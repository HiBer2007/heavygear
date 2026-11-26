package com.cecilia.heavygear.mixin;

import com.cecilia.heavygear.item.HeavySmash;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives heavy spear stabs the mace smash bonus (the 1.21.11 port had this in {@code DiveStabMixin},
 * hooked on the vanilla spear stab).
 *
 * <p>1.21.1 spears come from the Backported Spears mod, whose {@code PiercingWeapon#stab} walks every
 * pierced target and hands the base attack damage to {@code SpearUser#pierce}. The extra mace smash
 * damage of the attacker's fall (or elytra dive) is folded into that argument, so it keeps the
 * vanilla curve, Density and the attack charge scaling of a spear stab.</p>
 *
 * <p>The target is a mod class, so it is addressed by name and this mixin lives in its own config
 * ({@code heavygear.spears.mixins.json}, {@code required: false}): without the spears mod the mixin is
 * skipped instead of crashing the game.</p>
 */
@Mixin(targets = "com.notunanancyowen.spears.components.PiercingWeapon")
public abstract class SpearStabSmashMixin {
    @Unique
    private static final ThreadLocal<Float> HEAVYGEAR_SMASH_BONUS = ThreadLocal.withInitial(() -> 0.0F);

    @Inject(method = "stab", at = @At("HEAD"))
    private void heavygear$beginStab(LivingEntity attacker, EquipmentSlot slot, CallbackInfo ci) {
        HEAVYGEAR_SMASH_BONUS.set(HeavySmash.stabSmashBonus(attacker));
    }

    @ModifyArg(
            method = "stab",
            at = @At(value = "INVOKE",
                    target = "Lcom/notunanancyowen/spears/dataholders/SpearUser;pierce"
                            + "(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/Entity;FZZZ)Z"),
            index = 2)
    private float heavygear$addSmashBonus(float damage) {
        float bonus = HEAVYGEAR_SMASH_BONUS.get();
        return bonus <= 0.0F ? damage : damage + bonus;
    }

    @Inject(method = "stab", at = @At("RETURN"))
    private void heavygear$endStab(LivingEntity attacker, EquipmentSlot slot, CallbackInfoReturnable<Boolean> cir) {
        HEAVYGEAR_SMASH_BONUS.remove();
    }
}
