package com.cecilia.heavygear.item;

import com.cecilia.heavygear.HeavyGearMod;
import net.minecraft.world.item.ItemStack;

/** Helper for the Longinus mode component (false = normal mode, true = trident mode). */
public final class LonginusMode {
    private LonginusMode() {
    }

    public static boolean isTrident(ItemStack stack) {
        if (stack == null || stack.isEmpty() || HeavyGearMod.LONGINUS_TRIDENT_MODE == null) {
            return false;
        }
        return stack.getOrDefault(HeavyGearMod.LONGINUS_TRIDENT_MODE, Boolean.FALSE);
    }

    public static void setTrident(ItemStack stack, boolean trident) {
        if (stack == null || stack.isEmpty() || HeavyGearMod.LONGINUS_TRIDENT_MODE == null) {
            return;
        }
        stack.set(HeavyGearMod.LONGINUS_TRIDENT_MODE, trident);
    }
}
