package com.cecilia.heavygear.item;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

/** Heavy axe: the netherite axe plus the vanilla mace smash attack. */
public class HeavyAxeItem extends AxeItem {
    public HeavyAxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        if (HeavySmash.smashActive(source.getDirectEntity() instanceof LivingEntity living ? living : null)) {
            return HeavySmash.attackDamageBonus(target, damage, source);
        }
        return super.getAttackDamageBonus(target, damage, source);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (HeavySmash.smashActive(attacker)) {
            HeavySmash.hurtEnemy(stack, target, attacker);
        }
        return result;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        if (HeavySmash.smashActive(attacker)) {
            HeavySmash.postHurtEnemy(stack, target, attacker);
        }
    }
}
