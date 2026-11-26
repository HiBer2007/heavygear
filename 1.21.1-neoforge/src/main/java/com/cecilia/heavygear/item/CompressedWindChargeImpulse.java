package com.cecilia.heavygear.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Remembers where a compressed-wind-charge launch started, so landing back there is free. */
public final class CompressedWindChargeImpulse {
    private static final Map<UUID, Double> LAUNCH_Y = new HashMap<>();
    /** Launches caused by the double compressed charge, for the "fly higher" advancement. */
    private static final Map<UUID, Double> DOUBLE_LAUNCH_Y = new HashMap<>();
    private static final double FLY_HIGHER_HEIGHT = 65.0D;
    private static final ResourceLocation FLY_HIGHER =
            ResourceLocation.fromNamespaceAndPath("heavygear", "fly_higher");

    private CompressedWindChargeImpulse() {
    }

    public static void markLaunch(UUID player, double y) {
        LAUNCH_Y.put(player, y);
    }

    /** Marks a launch caused by the double compressed charge (impact scale 4). */
    public static void markDoubleLaunch(UUID player, double y) {
        DOUBLE_LAUNCH_Y.put(player, y);
    }

    /**
     * Awards the "fly higher" advancement once a double-charge launch has carried the player at least
     * 50 blocks above the launch point.
     */
    public static void checkFlyHigher(ServerPlayer player) {
        UUID id = player.getUUID();
        Double launch = DOUBLE_LAUNCH_Y.get(id);
        if (launch == null) {
            return;
        }
        if (player.getY() + 0.5D < launch) {
            // landed without reaching the height
            DOUBLE_LAUNCH_Y.remove(id);
            return;
        }
        if (player.getY() - launch < FLY_HIGHER_HEIGHT) {
            return;
        }
        DOUBLE_LAUNCH_Y.remove(id);
        if (!(player.level() instanceof ServerLevel level) || level.getServer() == null) {
            return;
        }
        var holder = level.getServer().getAdvancements().get(FLY_HIGHER);
        if (holder != null) {
            player.getAdvancements().award(holder, "impossible");
        }
    }

    /** True while the player is still at or above the height they were launched from. */
    public static boolean protects(UUID player, double currentY) {
        Double launch = LAUNCH_Y.get(player);
        if (launch == null) {
            return false;
        }
        if (currentY + 0.5D < launch) {
            LAUNCH_Y.remove(player);
            return false;
        }
        return true;
    }

    public static void clear(UUID player) {
        LAUNCH_Y.remove(player);
        DOUBLE_LAUNCH_Y.remove(player);
    }
}
