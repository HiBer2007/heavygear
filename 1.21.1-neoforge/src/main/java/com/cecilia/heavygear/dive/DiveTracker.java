package com.cecilia.heavygear.dive;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class DiveTracker {
    private static final Map<Entity, Double> DESCENDED = new WeakHashMap<>();
    public static final double MAX_USED_DISTANCE = 20.0;

    private DiveTracker() {
    }

    /** Called every tick from the PlayerTickDiveMixin. */
    public static void tick(LivingEntity entity) {
        double dy = entity.getDeltaMovement().y;
        if (entity.isFallFlying() && dy < 0.0) {
            DESCENDED.merge(entity, -dy, Double::sum);
        } else if (!entity.isFallFlying()) {
            DESCENDED.remove(entity);
        }
    }

    public static double descended(Entity entity) {
        return DESCENDED.getOrDefault(entity, 0.0);
    }

    public static void clear(Entity entity) {
        DESCENDED.remove(entity);
    }
}
