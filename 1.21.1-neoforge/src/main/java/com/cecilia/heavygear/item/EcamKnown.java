package com.cecilia.heavygear.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Remembers whether the player has ever held an ecaM, so the tooltip hint can change from the cryptic
 * line to the real one once the item has actually been crafted.
 */
public final class EcamKnown {
    private static boolean known;

    private EcamKnown() {
    }

    public static boolean isKnown(Player player) {
        if (known) {
            return true;
        }
        if (player == null) {
            return false;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(HeavyGear.ECAM)) {
                known = true;
                return true;
            }
        }
        return false;
    }

    public static void mark() {
        known = true;
    }
}
