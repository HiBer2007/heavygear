package com.cecilia.heavygear.mixin;

import com.cecilia.heavygear.combat.RiptideContact;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brackets the riptide dash contact hit ({@code Player#doAutoAttackOnTouch}, called once per creature
 * the spinning player touches) so that only this hit is stripped of the mace smash — deliberate
 * charged attacks keep theirs.
 */
@Mixin(Player.class)
public abstract class RiptideContactMixin {
    @Inject(method = "doAutoAttackOnTouch", at = @At("HEAD"))
    private void heavygear$beginRiptideContact(LivingEntity target, CallbackInfo ci) {
        RiptideContact.begin();
    }

    @Inject(method = "doAutoAttackOnTouch", at = @At("RETURN"))
    private void heavygear$endRiptideContact(LivingEntity target, CallbackInfo ci) {
        RiptideContact.end();
    }
}
