package com.cecilia.heavygear.item;

import java.util.List;

import com.cecilia.heavygear.combat.RiptideContact;
import com.cecilia.heavygear.enchant.HeavyGearEnchantments;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Spear of Longinus. Two modes, toggled with the configured key:
 * <ul>
 *   <li>Normal mode: the smash / charged attack only. No Riptide, cannot be thrown.</li>
 *   <li>Trident mode: the smash attack still works; Riptide (in water/rain) runs the vanilla dash on
 *       release, and without Riptide the spear is thrown on release.</li>
 * </ul>
 *
 * <p>1.21.1 has no spear item family, so the vanilla trident's charge/dash numbers are reproduced
 * here (see {@code TridentItem#releaseUsing}): 8.0 dash damage, 2.5 throw power, 10 tick minimum
 * charge before a riptide launch.</p>
 */
public class LonginusSpearItem extends HeavySpearItem {
    /** 70% of the vanilla trident's 10 tick charge, in ticks. */
    public static final int THROW_THRESHOLD_TIME = 7;
    public static final float SHOOT_POWER = 2.5F;

    public LonginusSpearItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    private static boolean riptide(ItemStack stack) {
        return HeavyGearEnchantments.level(stack, Enchantments.RIPTIDE) > 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            return InteractionResultHolder.fail(stack);
        }
        if (LonginusMode.isTrident(stack) && riptide(stack) && !player.isInWaterOrRain()) {
            // Riptide only works in water or rain; without it this would be a normal stab.
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        if (!LonginusMode.isTrident(stack) || !riptide(stack) || !player.isInWaterOrRain()) {
            // normal mode / charged smash: nothing to release, the throw is client driven
            return;
        }
        int charge = this.getUseDuration(stack, entity) - timeLeft;
        if (charge < 10) {
            return;
        }
        float spinStrength = EnchantmentHelper.getTridentSpinAttackStrength(stack, player);
        if (!level.isClientSide) {
            if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
                return;
            }
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        }
        // vanilla riptide dash: push the player along the look direction and start the spin attack
        // (mirrors TridentItem#releaseUsing, which does this on both sides)
        if (spinStrength > 0.0F) {
            float f7 = player.getYRot();
            float f1 = player.getXRot();
            float f2 = -Mth.sin(f7 * (float) (Math.PI / 180.0)) * Mth.cos(f1 * (float) (Math.PI / 180.0));
            float f3 = -Mth.sin(f1 * (float) (Math.PI / 180.0));
            float f4 = Mth.cos(f7 * (float) (Math.PI / 180.0)) * Mth.cos(f1 * (float) (Math.PI / 180.0));
            float f5 = Mth.sqrt(f2 * f2 + f3 * f3 + f4 * f4);
            f2 *= spinStrength / f5;
            f3 *= spinStrength / f5;
            f4 *= spinStrength / f5;
            player.push(f2, f3, f4);
            player.startAutoSpinAttack(20, 8.0F, stack);
            if (player.onGround()) {
                player.move(MoverType.SELF, new Vec3(0.0, 1.1999999F, 0.0));
            }
            Holder<SoundEvent> sound = EnchantmentHelper.pickHighestLevel(stack, EnchantmentEffectComponents.TRIDENT_SOUND)
                    .orElse(SoundEvents.TRIDENT_THROW);
            level.playSound(null, player, sound.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    /**
     * The riptide dash contact hit must behave like a vanilla trident (plain contact damage, no mace
     * smash), while charged attacks keep their smash - hence the narrow {@link RiptideContact} bracket.
     */
    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        if (RiptideContact.isActive()) {
            return 0.0F;
        }
        return super.getAttackDamageBonus(target, damage, source);
    }

    /** Same for the smash feedback (sound, particles, impulse) of the dash contact hit. */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (RiptideContact.isActive()) {
            return true;
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    /** Shows the current mode in the item tooltip (same wording / colours as the toggle message). */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.heavygear.lance_of_longinus.rune")
                .withStyle(ChatFormatting.GRAY));
        boolean trident = LonginusMode.isTrident(stack);
        tooltip.add(Component.translatable("message.heavygear.longinus_mode.prefix")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.translatable(trident
                                ? "message.heavygear.longinus_mode.trident"
                                : "message.heavygear.longinus_mode.normal")
                        .withStyle(trident ? ChatFormatting.AQUA : ChatFormatting.GREEN)));
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            // client-only: shows the key currently bound to the mode toggle
            tooltip.add(com.cecilia.heavygear.client.LanceTooltipSupport.toggleHint());
        }
    }
}
