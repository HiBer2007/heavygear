package com.cecilia.heavygear.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/** Heavy bow: only fires heavy arrows, everything else behaves like a vanilla bow. */
public class HeavyBowItem extends BowItem {
    public HeavyBowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return stack -> stack.is(HeavyGear.HEAVY_ARROW);
    }
}
