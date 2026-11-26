package com.cecilia.heavygear.item;

import com.cecilia.heavygear.mixin.FireworkRocketAccessor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.AABB;

/**
 * Heavy spear. The 1.21.1 build keeps the mace smash attack (the spear is a two-handed smash weapon)
 * and the dive bonus, which is folded into the vanilla smash calculation by
 * {@code DiveSmashDamageMixin} so damage enchantments keep working.
 */
public class HeavySpearItem extends MaceItem {
    public HeavySpearItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean diving = attacker.isFallFlying()
                && com.cecilia.heavygear.enchant.HeavyGearEnchantments.diveBombingLevel(stack) > 0
                && com.cecilia.heavygear.dive.DiveTracker.descended(attacker) >= 1.5;
        float savedFallDistance = attacker.fallDistance;
        if (diving) {
            cancelFireworkBoost(attacker);
            attacker.fallDistance = (float) Math.min(
                    com.cecilia.heavygear.dive.DiveTracker.descended(attacker), 20.0);
        }
        boolean result = super.hurtEnemy(stack, target, attacker);
        attacker.fallDistance = savedFallDistance;
        return result;
    }

    private static void cancelFireworkBoost(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        AABB box = attacker.getBoundingBox().inflate(8.0);
        for (FireworkRocketEntity rocket : level.getEntitiesOfClass(FireworkRocketEntity.class, box)) {
            if (((FireworkRocketAccessor) rocket).heavygear$attachedTo() == attacker) {
                rocket.discard();
            }
        }
    }
}
