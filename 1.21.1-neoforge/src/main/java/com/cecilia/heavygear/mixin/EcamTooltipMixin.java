package com.cecilia.heavygear.mixin;

import com.cecilia.heavygear.item.EcamKnown;
import com.cecilia.heavygear.item.HeavyGear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/** Adds the little gray hint line to the ecaM tooltip, before and after it has been obtained. */
@Mixin(ItemStack.class)
public abstract class EcamTooltipMixin {
    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void heavygear$ecamTooltip(Item.TooltipContext context, Player player, TooltipFlag flag,
                                       CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!stack.is(HeavyGear.ECAM)) {
            return;
        }
        List<Component> lines = cir.getReturnValue();
        if (lines == null || lines.isEmpty()) {
            return;
        }
        boolean known = EcamKnown.isKnown(player);
        Component hint = Component.translatable(known
                        ? "item.heavygear.ecam.hint"
                        : "item.heavygear.ecam.hint_locked")
                .withStyle(ChatFormatting.GRAY)
                .withStyle(style -> style.withItalic(false));
        List<Component> wrapped = new ArrayList<>(lines);
        wrapped.add(Math.min(1, wrapped.size()), hint);
        cir.setReturnValue(wrapped);
    }
}
