package com.cecilia.heavygear.mixin;

import com.cecilia.heavygear.dive.DiveTracker;
import com.cecilia.heavygear.enchant.HeavyGearEnchantments;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.MaceItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MaceItem.class)
public abstract class MaceItemMixin {
    @Inject(method = "canSmashAttack", at = @At("HEAD"), cancellable = true)
    private static void heavygear$allowDiveBombing(LivingEntity attacker, CallbackInfoReturnable<Boolean> cir) {
        // Vanilla refuses a smash while gliding; the dive bombing enchantment lifts that restriction.
        // It may sit on the weapon (heavy weapons) or on the heavy helmet (armour dive), so both the
        // weapon and the worn head slot are accepted.
        if (attacker.isFallFlying()
                && DiveTracker.descended(attacker) >= 1.5
                && (HeavyGearEnchantments.diveBombingLevel(attacker.getWeaponItem()) > 0
                        || HeavyGearEnchantments.diveBombingLevel(
                                attacker.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)) > 0)) {
            cir.setReturnValue(true);
        }
    }
}
