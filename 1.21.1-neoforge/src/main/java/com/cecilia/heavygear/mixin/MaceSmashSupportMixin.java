package com.cecilia.heavygear.mixin;

import com.cecilia.heavygear.combat.SmashAttribution;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records an actual mace style smash on the victim, so only a real smash claims the weapon specific
 * death message (1.21.1 has no dedicated mace smash damage type to read it from).
 */
@Mixin(MaceItem.class)
public abstract class MaceSmashSupportMixin {
    @Inject(method = "hurtEnemy", at = @At("HEAD"))
    private void heavygear$recordSmash(ItemStack stack, LivingEntity target, LivingEntity attacker,
                                       CallbackInfo ci) {
        // only a real smash (the attacker qualifies) may claim the death message
        if (MaceItem.canSmashAttack(attacker)) {
            SmashAttribution.record(target);
        }
    }
}
