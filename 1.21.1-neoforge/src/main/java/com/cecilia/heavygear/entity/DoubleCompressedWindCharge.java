package com.cecilia.heavygear.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.level.Level;

/** Twice compressed: the darkened model again, quadruple impact. */
public class DoubleCompressedWindCharge extends CompressedWindCharge {
    public DoubleCompressedWindCharge(EntityType<? extends WindCharge> type, Level level) {
        super(type, level, 4.0D);
    }
}
