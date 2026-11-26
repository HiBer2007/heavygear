package com.cecilia.heavygear.client;

import com.cecilia.heavygear.HeavyGearMod;
import com.cecilia.heavygear.item.LonginusMode;
import com.cecilia.heavygear.item.LonginusSpearItem;
import com.cecilia.heavygear.network.LonginusModePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Pressing the configured key toggles the held Longinus between normal and trident mode. */
@EventBusSubscriber(modid = HeavyGearMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class HeavyGearClientEvents {
    private HeavyGearClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || HeavyGearClient.THROW_KEY == null) {
            return;
        }
        while (HeavyGearClient.THROW_KEY.consumeClick()) {
            ItemStack held = minecraft.player.getMainHandItem();
            if (!(held.getItem() instanceof LonginusSpearItem)) {
                continue;
            }
            // Server is authoritative: it flips the item's component and broadcasts the new state back,
            // which also refreshes the client mirror.
            PacketDistributor.sendToServer(new LonginusModePayload(InteractionHand.MAIN_HAND));
            HeavyGearClient.tridentMode = !LonginusMode.isTrident(held);
            HeavyGearClient.lastModeToggleTick = minecraft.player.tickCount;
            HeavyGearMod.LOGGER.info("longinus mode toggle key pressed (was trident={})",
                    LonginusMode.isTrident(held));
        }
    }
}
