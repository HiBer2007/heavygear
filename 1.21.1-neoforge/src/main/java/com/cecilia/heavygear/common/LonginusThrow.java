package com.cecilia.heavygear.common;

import com.cecilia.heavygear.HeavyGearMod;
import com.cecilia.heavygear.entity.ThrownLonginus;
import com.cecilia.heavygear.item.LonginusSpearItem;
import com.cecilia.heavygear.network.LonginusThrowPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;

public final class LonginusThrow {
    private LonginusThrow() {
    }

    /** Runs on the server thread; spawns the thrown trident for the given hand. */
    public static void perform(ServerPlayer player, LonginusThrowPayload payload) {
        InteractionHand hand = payload.hand();
        ItemStack stack = player.getItemInHand(hand);
        HeavyGearMod.LOGGER.info("longinus perform hand={} item={} charge={}", hand, stack.getItem(), payload.chargeTicks());
        if (!(stack.getItem() instanceof LonginusSpearItem)) {
            return;
        }
        if (payload.chargeTicks() < LonginusSpearItem.THROW_THRESHOLD_TIME) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        ItemStack thrownStack = stack.copy();
        thrownStack.setCount(1);
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        ThrownLonginus trident = ThrownLonginus.create(level, player, thrownStack);
        trident.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                LonginusSpearItem.SHOOT_POWER, 1.0F);
        if (player.hasInfiniteMaterials()) {
            trident.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        }
        level.addFreshEntity(trident);
        // End the server side use that was started by the right click, so a lingering charge cannot
        // fire later without a client side animation.
        player.stopUsingItem();
        HeavyGearMod.LOGGER.info("longinus spawned {} at {},{},{}", trident.getClass().getSimpleName(),
                trident.getX(), trident.getY(), trident.getZ());
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
