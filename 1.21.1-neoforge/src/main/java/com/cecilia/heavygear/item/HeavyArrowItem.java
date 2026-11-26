package com.cecilia.heavygear.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** The heavy arrow item: spawns a {@link com.cecilia.heavygear.entity.HeavyArrow} so the smash and the recoloured entity apply. */
public class HeavyArrowItem extends ArrowItem {
    public HeavyArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @javax.annotation.Nullable ItemStack weapon) {
        return new com.cecilia.heavygear.entity.HeavyArrow(
                com.cecilia.heavygear.HeavyGearMod.HEAVY_ARROW_TYPE, shooter, level,
                ammo.copyWithCount(1), weapon);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.heavygear.heavy_arrow.smash")
                .withStyle(ChatFormatting.GRAY));
    }
}
