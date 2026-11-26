package com.cecilia.heavygear.mixin;

import com.cecilia.heavygear.item.HeavyBowItem;
import com.cecilia.heavygear.item.HeavyCrossbowItem;
import com.cecilia.heavygear.item.HeavyGear;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Creative mode has no heavy arrows: vanilla falls back to a plain arrow when the player carries no
 * matching ammo, which the heavy bow / crossbow reject (they only load heavy arrows). Hand creative
 * players heavy arrows instead so the weapons fire in creative exactly like the vanilla ones do.
 */
@Mixin(Player.class)
public abstract class CreativeHeavyAmmoMixin {
    @Inject(method = "getProjectile", at = @At("RETURN"), cancellable = true)
    private void heavygear$creativeAmmo(ItemStack weapon, CallbackInfoReturnable<ItemStack> cir) {
        if (!(weapon.getItem() instanceof HeavyBowItem)
                && !(weapon.getItem() instanceof HeavyCrossbowItem)) {
            return;
        }
        Player self = (Player) (Object) this;
        if (!self.getAbilities().instabuild) {
            return;
        }
        ItemStack result = cir.getReturnValue();
        if (result == null || result.isEmpty() || result.is(Items.ARROW)) {
            cir.setReturnValue(new ItemStack(HeavyGear.HEAVY_ARROW));
        }
    }
}
