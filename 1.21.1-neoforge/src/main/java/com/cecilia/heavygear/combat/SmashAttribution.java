package com.cecilia.heavygear.combat;

import net.minecraft.world.entity.Entity;

/** Remembers the victim of the most recent arrow smash so the death message can attribute it. */
public final class SmashAttribution {
    private static int victimId = -1;
    private static int victimTick = -100;

    private SmashAttribution() {
    }

    public static void record(Entity victim) {
        victimId = victim.getId();
        victimTick = victim.tickCount;
    }

    /** True when the recorded smash landed on {@code victim} during the current tick window. */
    public static boolean didSmash(Entity victim) {
        return victim.getId() == victimId && victim.tickCount - victimTick <= 1;
    }
}
