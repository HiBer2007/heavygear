package com.cecilia.heavygear.mixin.client;

import com.cecilia.heavygear.client.HeavyGearClient;
import com.cecilia.heavygear.enchant.HeavyGearEnchantments;
import com.cecilia.heavygear.item.LonginusMode;
import com.cecilia.heavygear.item.LonginusSpearItem;
import com.cecilia.heavygear.network.LonginusCancelPayload;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Release behaviour of the Spear of Longinus:
 * <ul>
 *   <li>Normal mode: the normal smash / charged attack only (no Riptide, no throw).</li>
 *   <li>Trident mode: Riptide (in water/rain) runs the vanilla dash; without Riptide the spear is
 *       thrown. No key press is required in this mode.</li>
 * </ul>
 */
@Mixin(MultiPlayerGameMode.class)
public class ReleaseUsingItemMixin {
    @Inject(method = "releaseUsingItem", at = @At("HEAD"), cancellable = true)
    private void heavygear$release(Player player, CallbackInfo ci) {
        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof LonginusSpearItem)) {
            return;
        }
        // The item's own component is the source of truth: it is synced to the client and survives
        // death / world reload, unlike a static client cache.
        boolean trident = LonginusMode.isTrident(stack);
        if (!trident) {
            return;
        }
        boolean riptide = HeavyGearEnchantments.level(stack, Enchantments.RIPTIDE) > 0;
        com.cecilia.heavygear.HeavyGearMod.LOGGER.info(
                "longinus release: trident={} riptide={} water={}", trident, riptide, player.isInWaterOrRain());
        if (riptide) {
            if (player.isInWaterOrRain()) {
                // Let the vanilla release run the Riptide dash.
                return;
            }
            // End the use on both sides without the charged attack.
            PacketDistributor.sendToServer(new LonginusCancelPayload());
            player.stopUsingItem();
            ci.cancel();
            return;
        }
        int charge = 72000 - player.getUseItemRemainingTicks();
        if (charge < LonginusSpearItem.THROW_THRESHOLD_TIME) {
            // Not charged long enough (70% of the vanilla trident charge time).
            player.stopUsingItem();
            ci.cancel();
            return;
        }
        HeavyGearClient.throwHand = player.getUsedItemHand();
        HeavyGearClient.finishThrowCharge(player.getUseItemRemainingTicks());
        player.stopUsingItem();
        ci.cancel();
    }
}
