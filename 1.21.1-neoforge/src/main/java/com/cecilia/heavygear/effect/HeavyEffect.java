package com.cecilia.heavygear.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.Potion;

/**
 * The heavy potion: while it is active every weapon behaves like a heavy one (mace smashes with any
 * item, armour style landings, arrow smashes and a smashing fishing bobber).
 *
 * <p>The effect colour is the breeze rod palette (lavender blue) so the potion liquid, its particles
 * and the effect icon all match the heavy gear art.</p>
 */
public final class HeavyEffect {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("heavygear", "heavy");
    public static final ResourceLocation POTION_ID = ResourceLocation.fromNamespaceAndPath("heavygear", "heavy");

    /** Breeze rod lavender blue, used for the potion liquid and the effect particles. */
    public static final int COLOR = 0x7A82C4;

    public static final Holder<MobEffect> HEAVY = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT, ID, new HeavyMobEffect());

    /** One hour of heavy gear: awkward potion + heavy core. */
    public static final Holder<Potion> POTION = Registry.registerForHolder(
            BuiltInRegistries.POTION, POTION_ID,
            new Potion("heavy", new MobEffectInstance(HEAVY, 3600, 0)));

    /** Redstone doubled variant (8 minutes). */
    public static final ResourceLocation POTION_LONG_ID =
            ResourceLocation.fromNamespaceAndPath("heavygear", "heavy_long");

    public static final Holder<Potion> POTION_LONG = Registry.registerForHolder(
            BuiltInRegistries.POTION, POTION_LONG_ID,
            new Potion("heavy_long", new MobEffectInstance(HEAVY, 9600, 0)));

    private HeavyEffect() {
    }

    public static boolean has(Entity entity) {
        return entity instanceof LivingEntity living && living.hasEffect(HEAVY);
    }

    /** Touching the class registers the effect and the potion. */
    public static void bootstrap() {
    }

    /** {@code MobEffect}'s constructor is protected, so the effect needs its own subclass. */
    public static final class HeavyMobEffect extends MobEffect {
        public HeavyMobEffect() {
            super(MobEffectCategory.BENEFICIAL, COLOR);
        }
    }
}
