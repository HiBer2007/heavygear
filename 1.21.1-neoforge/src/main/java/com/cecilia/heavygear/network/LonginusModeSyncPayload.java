package com.cecilia.heavygear.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server tells the client the authoritative Longinus mode. */
public record LonginusModeSyncPayload(boolean trident) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LonginusModeSyncPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("heavygear", "longinus_mode_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LonginusModeSyncPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBoolean(payload.trident()),
            buf -> new LonginusModeSyncPayload(buf.readBoolean())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
