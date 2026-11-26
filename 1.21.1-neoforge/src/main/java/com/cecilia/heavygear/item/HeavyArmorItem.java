package com.cecilia.heavygear.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Heavy armour piece. Behaves exactly like a vanilla {@link ArmorItem} (all stats come from the
 * armour material in {@link HeavyGear}); the only addition is the grey hint line that explains the
 * meteor-smash bonus shared by the whole set.
 */
public class HeavyArmorItem extends ArmorItem {
    public HeavyArmorItem(Holder<ArmorMaterial> material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.heavygear.heavy_armor.smash")
                .withStyle(ChatFormatting.GRAY));
    }
}
