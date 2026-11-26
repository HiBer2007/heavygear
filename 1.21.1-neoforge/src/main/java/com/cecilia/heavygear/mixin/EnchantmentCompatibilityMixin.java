package com.cecilia.heavygear.mixin;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Vanilla keeps Impaling in {@code #minecraft:exclusive_set/damage}, and the anvil enforces that with
 * {@link Enchantment#areCompatible}, which has no awareness of the item being worked on. Impaling is
 * therefore only ever compatible with tridents, and the Spear of Longinus (which is the only item in
 * the game that supports both Impaling and the sword damage enchantments) could never hold them
 * together.
 *
 * <p>This relaxes exactly the three impaling pairs. Tridents are unaffected in practice: they cannot
 * take Sharpness / Smite / Bane at all, so the vanilla behaviour is preserved for everything except
 * the lance.</p>
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentCompatibilityMixin {
    @Unique
    private static final Set<String> HEAVYGEAR_IMPALING = Set.of(
            "enchantment.minecraft.impaling");

    @Unique
    private static final Set<String> HEAVYGEAR_SWORD_DAMAGE = Set.of(
            "enchantment.minecraft.sharpness",
            "enchantment.minecraft.smite",
            "enchantment.minecraft.bane_of_arthropods");

    @Unique
    private static String heavygear$key(Holder<Enchantment> holder) {
        if (!(holder.value().description().getContents() instanceof TranslatableContents contents)) {
            return null;
        }
        return contents.getKey();
    }

    @Inject(method = "areCompatible", at = @At("RETURN"), cancellable = true)
    private static void heavygear$allowImpalingWithSwordDamage(Holder<Enchantment> first,
                                                              Holder<Enchantment> second,
                                                              CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return;
        }
        String a = heavygear$key(first);
        String b = heavygear$key(second);
        if (a == null || b == null) {
            return;
        }
        boolean impalingPair = (HEAVYGEAR_IMPALING.contains(a) && HEAVYGEAR_SWORD_DAMAGE.contains(b))
                || (HEAVYGEAR_IMPALING.contains(b) && HEAVYGEAR_SWORD_DAMAGE.contains(a));
        if (impalingPair) {
            cir.setReturnValue(true);
        }
    }
}
