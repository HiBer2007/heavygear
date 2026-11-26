package com.cecilia.heavygear;

import com.cecilia.heavygear.combat.ArmorSmash;
import com.cecilia.heavygear.dive.DiveTracker;
import com.cecilia.heavygear.item.CompressedWindChargeImpulse;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server side per-tick work the 1.21.11 build did with entity mixins: the dive tracker, the heavy
 * armour landing smash, the "fly higher" advancement and the compressed wind charge fall protection.
 */
@EventBusSubscriber(modid = HeavyGearMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class HeavyGearEvents {
    private HeavyGearEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DiveTracker.tick(player);
            ArmorSmash.tick(player);
            CompressedWindChargeImpulse.checkFlyHigher(player);
        }
    }

    /**
     * A compressed wind charge launch must not cause fall damage when the player comes back down to
     * the launch height - but the fall distance itself has to stay intact, because the heavy gear
     * smash reads it. Only the fall damage itself is cancelled here.
     */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && CompressedWindChargeImpulse.protects(player.getUUID(), player.getY())) {
            event.setCanceled(true);
        }
    }
}
