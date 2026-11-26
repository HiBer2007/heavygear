package com.cecilia.heavygear.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.WindChargeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WindChargeRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;

/** Renders a wind charge with its own texture; used for the darkened compressed variants. */
public class DarkWindChargeRenderer extends WindChargeRenderer {
    private static final float MIN_CAMERA_DISTANCE_SQUARED = Mth.square(3.5F);

    private final WindChargeModel heavygear$model;
    private final ResourceLocation heavygear$texture;

    public DarkWindChargeRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        super(context);
        this.heavygear$model = new WindChargeModel(context.bakeLayer(ModelLayers.WIND_CHARGE));
        this.heavygear$texture = texture;
    }

    @Override
    public void render(AbstractWindCharge entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (entity.tickCount < 2
                && this.entityRenderDispatcher.camera.getEntity().distanceToSqr(entity) < (double) MIN_CAMERA_DISTANCE_SQUARED) {
            return;
        }
        float f = (float) entity.tickCount + partialTick;
        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.breezeWind(this.heavygear$texture, this.xOffset(f) % 1.0F, 0.0F));
        this.heavygear$model.setupAnim(entity, 0.0F, 0.0F, f, 0.0F, 0.0F);
        this.heavygear$model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
