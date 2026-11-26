package com.cecilia.heavygear.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Heavy spear.
 *
 * <p>1.21.1 has no spear item family. The port therefore uses the Backported Spears mod
 * ({@code spears}): {@link SpearComponents} copies the {@code netherite_spear} components
 * ({@code minecraft:piercing_weapon}, {@code kinetic_weapon}, {@code attack_range},
 * {@code swing_animation}, {@code use_effects}, {@code minimum_attack_charge}, the attributes) onto
 * this item, which turns it into a real spear - piercing stab, kinetic charge attack, reach, spear
 * sounds, spear model and the Lunge enchantment hook. This class only reproduces the item level
 * behaviour that the backport's own spear items declare.</p>
 */
public class HeavySpearItem extends TieredItem {
    public HeavySpearItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    /**
     * The backport's spear model predicate ({@code spears:in_gui}) renders the {@code *_in_hand} model
     * whenever the held stack carries an entity representation, so a selected spear has to register
     * its holder every tick - exactly what the backport's own spear items do.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        stack.setEntityRepresentation(isSelected ? entity : null);
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    /** Starts the kinetic charge attack; the Backported Spears mod drives the rest. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }
}
