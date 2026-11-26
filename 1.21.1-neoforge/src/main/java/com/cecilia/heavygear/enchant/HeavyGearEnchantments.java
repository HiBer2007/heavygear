package com.cecilia.heavygear.enchant;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Enchantment lookup helper. 1.21.1 ships the mace enchantments (Density, Breach, Wind Burst) as
 * vanilla entries, so only the mod's own dive bombing enchantment needs a dedicated lookup.
 */
public final class HeavyGearEnchantments {
    public static final ResourceLocation DIVE_BOMBING_ID = ResourceLocation.fromNamespaceAndPath("heavygear", "dive_bombing");

    private HeavyGearEnchantments() {
    }

    public static int level(ItemStack stack, net.minecraft.resources.ResourceKey<Enchantment> key) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            boolean match = holder.unwrapKey()
                    .map(k -> k.equals(key))
                    .orElse(false);
            if (match) {
                return enchantments.getLevel(holder);
            }
        }
        return 0;
    }

    public static int diveBombingLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            boolean match = holder.unwrapKey()
                    .map(key -> key.location().equals(DIVE_BOMBING_ID))
                    .orElse(false);
            if (match) {
                return enchantments.getLevel(holder);
            }
        }
        return 0;
    }
}
