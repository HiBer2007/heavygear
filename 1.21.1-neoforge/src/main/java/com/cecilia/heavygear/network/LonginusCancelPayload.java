package com.cecilia.heavygear.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent by the client to end an ongoing spear use on the server without running the
 * charge attack release (used when a Riptide spear is released without the throw key).
 */
public record LonginusCancelPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LonginusCancelPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("heavygear", "longinus_cancel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LonginusCancelPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> new LonginusCancelPayload()
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
