package com.cecilia.heavygear.item;

import com.cecilia.heavygear.dive.DiveTracker;
import com.cecilia.heavygear.enchant.HeavyGearEnchantments;
import com.cecilia.heavygear.mixin.FireworkRocketAccessor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * Gives the heavy sword, axe, shovel, pickaxe and hoe the vanilla mace smash attack.
 *
 * <p>Every effect is delegated to the real {@code minecraft:mace} instance instead of being
 * re-implemented, so the numbers stay in sync with vanilla and nothing is lost: the fall distance
 * damage curve {@code MaceItem#getAttackDamageBonus} (which applies Density through
 * {@code EnchantmentHelper.modifyFallBasedDamage} on the <em>attacker's actual weapon</em>) and the
 * smash sound / particles / impulse of {@code MaceItem#hurtEnemy}.</p>
 *
 * <p>1.21.1 has no separate {@code mace_smash} damage type, the smash rides on the ordinary
 * {@code player_attack} hit, so the attribution of the landed smash is kept here (see
 * {@link com.cecilia.heavygear.combat.SmashAttribution}) for the death messages.</p>
 */
public final class HeavySmash {
    private HeavySmash() {
    }

    public static boolean isSmashTool(ItemStack stack) {
        return stack != null && !stack.isEmpty() && isSmashTool(stack.getItem());
    }

    /** True while the heavy potion effect is active on {@code entity}. */
    public static boolean hasHeavyEffect(Entity entity) {
        return com.cecilia.heavygear.effect.HeavyEffect.has(entity);
    }

    public static boolean isSmashTool(Item item) {
        return item == HeavyGear.HEAVY_SWORD
                || item == HeavyGear.HEAVY_AXE
                || item == HeavyGear.HEAVY_SHOVEL
                || item == HeavyGear.HEAVY_PICKAXE
                || item == HeavyGear.HEAVY_HOE;
    }

    /** True when this attacker performs a mace style smash with whatever it is holding. */
    public static boolean smashActive(LivingEntity attacker) {
        return attacker != null && (isSmashTool(attacker.getWeaponItem()) || hasHeavyEffect(attacker));
    }

    /** Smash damage bonus, i.e. {@code MaceItem#getAttackDamageBonus} on this attacker. */
    public static float attackDamageBonus(Entity target, float damage, DamageSource source) {
        if (!(source.getDirectEntity() instanceof LivingEntity attacker)) {
            return 0.0F;
        }
        double saved = prepareDive(attacker);
        try {
            float bonus = Items.MACE.getAttackDamageBonus(target, damage, source);
            if (bonus > 0.0F) {
                com.cecilia.heavygear.combat.SmashAttribution.record(target);
            }
            return bonus;
        } finally {
            restoreFallDistance(attacker, saved);
        }
    }

    /** Smash sound, particles and the small upward impulse, in the single vanilla hit. */
    public static void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        double saved = prepareDive(attacker);
        try {
            Items.MACE.hurtEnemy(stack, target, attacker);
        } finally {
            restoreFallDistance(attacker, saved);
        }
    }

    /** Vanilla resets the fall distance after a smash, so the bonus cannot be reused. */
    public static void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (net.minecraft.world.item.MaceItem.canSmashAttack(attacker)) {
            attacker.resetFallDistance();
        }
    }

    /**
     * While gliding {@code fallDistance} stays at 0, so the smash bonus would be 0. When the weapon
     * carries the dive bombing enchantment the tracked dive distance is substituted for the duration
     * of the call.
     *
     * @return the fall distance that has to be restored, or {@link Double#NaN} when untouched
     */
    private static double prepareDive(LivingEntity attacker) {
        if (!attacker.isFallFlying()
                || HeavyGearEnchantments.diveBombingLevel(attacker.getWeaponItem()) <= 0) {
            return Double.NaN;
        }
        double descended = DiveTracker.descended(attacker);
        if (descended < 1.5) {
            return Double.NaN;
        }
        cancelFirework(attacker);
        float saved = attacker.fallDistance;
        attacker.fallDistance = (float) Math.min(descended, 20.0);
        return saved;
    }

    private static void restoreFallDistance(LivingEntity attacker, double saved) {
        if (!Double.isNaN(saved)) {
            attacker.fallDistance = (float) saved;
        }
    }

    /** Stops the elytra fireworks from lifting the attacker back up during a dive smash. */
    public static void cancelFirework(LivingEntity attacker) {
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

    /**
     * Smash bonus for a spear stab (the Backported Spears mod runs the stab itself): the same mace
     * curve a swing of the same fall height would use, plus Density from the spear. Gliding does not
     * count as falling, so a dive needs the dive bombing enchantment and the tracked descent instead.
     *
     * @return the extra damage to add to the stab, or 0 when this is not a smash
     */
    public static float stabSmashBonus(LivingEntity attacker) {
        float fall;
        if (attacker.isFallFlying()) {
            if (HeavyGearEnchantments.diveBombingLevel(attacker.getWeaponItem()) <= 0) {
                return 0.0F;
            }
            double descended = DiveTracker.descended(attacker);
            if (descended < 1.5) {
                return 0.0F;
            }
            cancelFirework(attacker);
            fall = (float) Math.min(descended, 20.0);
        } else if (attacker.fallDistance > 1.5F) {
            fall = Math.min(attacker.fallDistance, 20.0F);
        } else {
            return 0.0F;
        }
        float bonus = maceCurve(fall);
        if (attacker.level() instanceof ServerLevel level) {
            var source = attacker instanceof net.minecraft.world.entity.player.Player player
                    ? level.damageSources().playerAttack(player)
                    : level.damageSources().mobAttack(attacker);
            bonus += net.minecraft.world.item.enchantment.EnchantmentHelper.modifyFallBasedDamage(
                    level, attacker.getWeaponItem(), attacker, source, 0.0F) * fall;
        }
        return bonus;
    }

    /** Vanilla mace smash bonus curve: 4·d up to 3, 12+2·(d-3) up to 8, 22+(d-8) above. */
    public static float maceCurve(double distance) {
        if (distance <= 3.0D) {
            return (float) (4.0D * distance);
        }
        if (distance <= 8.0D) {
            return (float) (12.0D + 2.0D * (distance - 3.0D));
        }
        return (float) (22.0D + (distance - 8.0D));
    }
}
