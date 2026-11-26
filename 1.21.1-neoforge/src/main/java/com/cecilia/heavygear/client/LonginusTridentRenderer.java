package com.cecilia.heavygear.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.ThrownTrident;

/**
 * Render the thrown Spear of Longinus with the vanilla trident model but its own texture
 * (heavy-core head, breeze-rod handle).
 */
public class LonginusTridentRenderer extends ThrownTridentRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("heavygear", "textures/entity/longinus_trident.png");

    public LonginusTridentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownTrident entity) {
        return TEXTURE;
    }
}
