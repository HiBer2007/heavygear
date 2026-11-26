package com.cecilia.heavygear.mixin;

import com.cecilia.heavygear.item.HeavySmash;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code Item} is the class {@code Player.attack} dispatches the smash hooks on, so injecting into the
 * base implementation gives every plain item (any item at all while the heavy potion is active) the
 * mace smash without touching the vanilla classes. The heavy tools implement the same three hooks in
 * their own subclasses because the vanilla tool classes override them.
 */
@Mixin(Item.class)
public abstract class HeavySmashMixin {
    @Inject(method = "getAttackDamageBonus", at = @At("RETURN"), cancellable = true)
    private void heavygear$smashToolBonus(Entity target, float damage, DamageSource source,
                                          CallbackInfoReturnable<Float> cir) {
        if (HeavySmash.isSmashTool((Item) (Object) this)
                || HeavySmash.hasHeavyEffect(source.getDirectEntity())) {
            cir.setReturnValue(HeavySmash.attackDamageBonus(target, damage, source));
        }
    }

    @Inject(method = "hurtEnemy", at = @At("HEAD"))
    private void heavygear$smashToolHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker,
                                              CallbackInfo ci) {
        if (HeavySmash.isSmashTool(stack) || HeavySmash.hasHeavyEffect(attacker)) {
            HeavySmash.hurtEnemy(stack, target, attacker);
        }
    }

    @Inject(method = "postHurtEnemy", at = @At("HEAD"))
    private void heavygear$smashToolPostHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker,
                                                  CallbackInfo ci) {
        if (HeavySmash.isSmashTool(stack) || HeavySmash.hasHeavyEffect(attacker)) {
            HeavySmash.postHurtEnemy(stack, target, attacker);
        }
    }
}
