package com.cecilia.heavygear.item;

import com.cecilia.heavygear.HeavyGearMod;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Feeds the spear components of the Backported Spears mod to the heavy spear and the Spear of
 * Longinus.
 *
 * <p>1.21.1 has no spear item family, and this port deliberately does not link against the backport's
 * Java API: the components ({@code minecraft:piercing_weapon}, {@code kinetic_weapon},
 * {@code attack_range}, {@code swing_animation}, {@code use_effects},
 * {@code minimum_attack_charge} and the netherite spear attributes) are copied from the vanilla
 * {@code minecraft:netherite_spear} item once every mod has registered its items, so the heavy spear
 * behaves exactly like the backported spears - including future rebalances of that mod - and this mod
 * only needs the {@code spears} mod at runtime (declared as a required dependency in
 * {@code neoforge.mods.toml}).</p>
 */
public final class SpearComponents {
    private static final ResourceLocation VANILLA_SPEAR = ResourceLocation.withDefaultNamespace("netherite_spear");

    private SpearComponents() {
    }

    public static void copyFromVanillaSpear(ModifyDefaultComponentsEvent event) {
        Item vanillaSpear = BuiltInRegistries.ITEM.get(VANILLA_SPEAR);
        if (vanillaSpear == null) {
            HeavyGearMod.LOGGER.warn("Backported Spears is missing, so the heavy spear and the lance get no "
                    + "spear components (piercing stab / kinetic charge attack / reach)");
            return;
        }
        DataComponentMap source = vanillaSpear.components();
        patch(event, HeavyGearMod.HEAVY_SPEAR, source);
        patch(event, HeavyGearMod.LONGINUS_SPEAR, source);
    }

    private static void patch(ModifyDefaultComponentsEvent event, Item item, DataComponentMap source) {
        event.modify(item, builder -> {
            for (DataComponentType<?> type : source.keySet()) {
                copy(builder, type, source);
            }
            // the heavy gear is epic and never burns, whatever the vanilla spear says
            builder.set(DataComponents.RARITY, Rarity.EPIC);
        });
    }

    private static <T> void copy(DataComponentPatch.Builder builder, DataComponentType<T> type, DataComponentMap source) {
        T value = source.get(type);
        if (value != null) {
            builder.set(type, value);
        }
    }
}
