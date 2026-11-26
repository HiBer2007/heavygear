package com.cecilia.heavygear.client;

import com.cecilia.heavygear.HeavyGearMod;
import com.cecilia.heavygear.item.LonginusMode;
import com.cecilia.heavygear.item.LonginusSpearItem;
import com.cecilia.heavygear.network.LonginusModePayload;
import com.cecilia.heavygear.network.LonginusModeSyncPayload;
import com.cecilia.heavygear.network.LonginusThrowPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Client half of Heavy Gear: the throw/mode key, the entity renderers and the client bound payload.
 * Runs on the NeoForge mod bus, and only on the physical client.
 */
@EventBusSubscriber(modid = HeavyGearMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HeavyGearClient {
    public static final String THROW_KEY_ID = "key.heavygear.throw";

    /** Toggles the Spear of Longinus between normal and trident mode. */
    public static KeyMapping THROW_KEY;

    public static InteractionHand throwHand = InteractionHand.MAIN_HAND;

    /** Cached Longinus mode for instant client side decisions (normal = false). */
    public static boolean tridentMode;
    public static int lastModeToggleTick;

    private HeavyGearClient() {
    }

    public static boolean isTridentMode() {
        return tridentMode;
    }

    public static boolean isThrowKeyHeld() {
        return THROW_KEY != null && THROW_KEY.isDown();
    }

    /** Called from the release hook on the client; the caller has already decided that this is a throw. */
    public static void finishThrowCharge(int timeLeft) {
        int charge = 72000 - timeLeft;
        HeavyGearMod.LOGGER.info("longinus throw release charge={}", charge);
        PacketDistributor.sendToServer(new LonginusThrowPayload(throwHand, charge));
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        THROW_KEY = new KeyMapping(
                THROW_KEY_ID,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                // 86 = keyboard code for V (GLFW_KEY_V), written as a literal so that no LWJGL module
                // is needed on the compile classpath.
                86,
                "key.categories.gameplay");
        event.register(THROW_KEY);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HeavyGearMod.THROWN_LONGINUS_TYPE, LonginusTridentRenderer::new);
        event.registerEntityRenderer(HeavyGearMod.HEAVY_ARROW_TYPE, HeavyArrowRenderer::new);
        event.registerEntityRenderer(HeavyGearMod.COMPRESSED_WIND_CHARGE_TYPE,
                context -> new DarkWindChargeRenderer(context,
                        ResourceLocation.fromNamespaceAndPath("heavygear",
                                "textures/entity/projectiles/compressed_wind_charge.png")));
        event.registerEntityRenderer(HeavyGearMod.DOUBLE_COMPRESSED_WIND_CHARGE_TYPE,
                context -> new DarkWindChargeRenderer(context,
                        ResourceLocation.fromNamespaceAndPath("heavygear",
                                "textures/entity/projectiles/double_compressed_wind_charge.png")));
    }

    @SubscribeEvent
    public static void registerClientPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(LonginusModeSyncPayload.TYPE, LonginusModeSyncPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> tridentMode = payload.trident()));
    }
}
