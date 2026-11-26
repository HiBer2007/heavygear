package com.cecilia.heavygear.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntitySharedFlagAccessor {
    @Invoker("setSharedFlag")
    void heavygear$setSharedFlag(int flag, boolean value);
}
