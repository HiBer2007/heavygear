package com.cecilia.heavygear.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Client-only tooltip helpers. Kept in the client package so the common item class never loads the
 * key mapping classes on a dedicated server.
 */
public final class LanceTooltipSupport {
    private LanceTooltipSupport() {
    }

    /** "按 [键] 切换模式" in grey, using whatever key is currently bound to the toggle. */
    public static Component toggleHint() {
        return Component.translatable("tooltip.heavygear.lance_of_longinus.toggle",
                        HeavyGearClient.THROW_KEY.getTranslatedKeyMessage())
                .withStyle(ChatFormatting.GRAY);
    }
}
