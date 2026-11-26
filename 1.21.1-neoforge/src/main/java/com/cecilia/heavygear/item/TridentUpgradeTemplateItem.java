package com.cecilia.heavygear.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.SmithingTemplateItem;

/**
 * Trident upgrade smithing template. 1.21.1 builds the tooltip from the components passed to the
 * constructor, so no {@code appendHoverText} override is needed.
 */
public class TridentUpgradeTemplateItem extends SmithingTemplateItem {
    private static final String SUFFIX_KEY = "item.heavygear.trident_upgrade_smithing_template";
    public static final String APPLIES_TO_KEY = SUFFIX_KEY + ".applies_to";
    public static final String INGREDIENTS_KEY = SUFFIX_KEY + ".ingredients";
    public static final String BASE_SLOT_KEY = SUFFIX_KEY + ".base_slot_description";
    public static final String ADDITIONS_SLOT_KEY = SUFFIX_KEY + ".additions_slot_description";

    public TridentUpgradeTemplateItem() {
        super(
                Component.translatable(APPLIES_TO_KEY),
                Component.translatable(INGREDIENTS_KEY),
                Component.translatable("item.heavygear.trident_upgrade_smithing_template.upgrade"),
                Component.translatable(BASE_SLOT_KEY),
                Component.translatable(ADDITIONS_SLOT_KEY),
                List.of(ResourceLocation.withDefaultNamespace("item/empty_slot_sword")),
                List.of(ResourceLocation.withDefaultNamespace("item/empty_slot_ingot"))
        );
    }
}
