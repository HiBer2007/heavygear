package com.cecilia.heavygear.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/** Heavy crossbow: only loads heavy arrows, everything else behaves like a vanilla crossbow. */
public class HeavyCrossbowItem extends CrossbowItem {
    public HeavyCrossbowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Predicate<ItemStack> getSupportedHeldProjectiles() {
        return stack -> stack.is(HeavyGear.HEAVY_ARROW);
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return stack -> stack.is(HeavyGear.HEAVY_ARROW);
    }
}
