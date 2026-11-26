package com.cecilia.heavygear.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

/**
 * Sent by the client when the player releases the right click after charging the
 * Spear of Longinus. The server performs the actual throw so the charge key can
 * stay fully client side (the lunge attack still uses the normal use packet).
 */
public record LonginusThrowPayload(InteractionHand hand, int chargeTicks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LonginusThrowPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("heavygear", "longinus_throw"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LonginusThrowPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeEnum(payload.hand());
                buf.writeVarInt(payload.chargeTicks());
            },
            buf -> new LonginusThrowPayload(buf.readEnum(InteractionHand.class), buf.readVarInt())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
