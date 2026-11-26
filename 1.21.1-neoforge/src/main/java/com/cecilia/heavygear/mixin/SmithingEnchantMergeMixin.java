package com.cecilia.heavygear.mixin;

import com.cecilia.heavygear.HeavyGearMod;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The trident upgrade keeps the enchantments of both ingredients.
 *
 * <p>Vanilla smithing transformation only copies the components of the <em>base</em> stack, so the
 * trident used as the addition would lose its enchantments. When the result is the Spear of Destiny
 * this mixes the base (heavy spear) and the addition (trident) enchantments together — taking the
 * higher level on duplicates and skipping anything that would clash with the inherited exclusive sets
 * (e.g. Riptide vs Loyalty).</p>
 */
@Mixin(SmithingTransformRecipe.class)
public abstract class SmithingEnchantMergeMixin {
    @Inject(method = "assemble", at = @At("RETURN"), cancellable = true)
    private void heavygear$mergeEnchantments(SmithingRecipeInput input, HolderLookup.Provider registries,
                                             CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (result == null || result.isEmpty() || !result.is(HeavyGearMod.LONGINUS_SPEAR)) {
            return;
        }
        ItemStack base = input.base();
        ItemStack addition = input.addition();
        if (addition.isEmpty()) {
            return;
        }

        Map<Holder<Enchantment>, Integer> merged = new LinkedHashMap<>();
        ItemEnchantments baseEnchantments = base.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : baseEnchantments.entrySet()) {
            merged.put(entry.getKey(), entry.getIntValue());
        }

        ItemEnchantments additionEnchantments = addition.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        // Deterministic order so an unresolvable conflict (riptide vs loyalty) always resolves the same
        // way: reverse id order puts "riptide" before "loyalty", which suits the trident mode.
        var additionEntries = new java.util.ArrayList<>(additionEnchantments.entrySet());
        additionEntries.sort(java.util.Comparator.<Object2IntMap.Entry<Holder<Enchantment>>, String>comparing(
                entry -> entry.getKey().unwrapKey()
                        .map(key -> key.location().toString()).orElse(""))
                .reversed());
        for (var entry : additionEntries) {
            Holder<Enchantment> enchantment = entry.getKey();
            int level = entry.getIntValue();
            boolean compatible = merged.keySet().stream().allMatch(
                    other -> other.equals(enchantment) || Enchantment.areCompatible(other, enchantment));
            if (!compatible) {
                continue;
            }
            merged.merge(enchantment, level, Math::max);
        }

        if (merged.isEmpty()) {
            return;
        }
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        merged.forEach(mutable::set);
        result.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
        cir.setReturnValue(result);
    }
}
