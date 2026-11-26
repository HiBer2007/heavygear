package com.cecilia.heavygear.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

/** Client asks the server to toggle the held Longinus mode. */
public record LonginusModePayload(InteractionHand hand) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LonginusModePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("heavygear", "longinus_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LonginusModePayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeEnum(payload.hand()),
            buf -> new LonginusModePayload(buf.readEnum(InteractionHand.class))
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
