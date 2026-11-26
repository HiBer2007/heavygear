package com.cecilia.heavygear.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Vanilla computes the pierce level inside the full {@code AbstractArrow(EntityType, x, y, z, Level,
 * pickup, weapon)} constructor ({@code EnchantmentHelper.getPiercingCount} → {@code setPierceLevel}).
 * The heavy arrow has to build itself through {@code Arrow(EntityType, Level)} instead, so it needs a
 * way to write the same value afterwards — that keeps vanilla's own "pierce through N+1 targets" logic.
 */
@Mixin(AbstractArrow.class)
public interface AbstractArrowPierceAccessor {
    @Invoker("setPierceLevel")
    void heavygear$setPierceLevel(byte level);
}
