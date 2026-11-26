package com.cecilia.heavygear.client;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the heavy arrow with its own projectile texture.
 *
 * <p>1.21.1 resolves an arrow texture through {@code ArrowRenderer#getTextureLocation(T)}, so the
 * renderer is typed directly on the heavy arrow entity and the texture is picked there.</p>
 */
public class HeavyArrowRenderer extends ArrowRenderer<com.cecilia.heavygear.entity.HeavyArrow> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "heavygear", "textures/entity/projectiles/heavy_arrow.png");

    public HeavyArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(com.cecilia.heavygear.entity.HeavyArrow entity) {
        return TEXTURE;
    }
}
