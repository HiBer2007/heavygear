package com.cecilia.heavygear.combat;

import com.cecilia.heavygear.dive.DiveTracker;
import com.cecilia.heavygear.enchant.HeavyGearEnchantments;
import com.cecilia.heavygear.item.HeavyGear;
import com.cecilia.heavygear.item.HeavySmash;
import com.cecilia.heavygear.mixin.EntitySharedFlagAccessor;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Heavy armour "meteor landing": falling onto a creature triggers a mace smash whose damage is
 * {@link #PER_PIECE} of what a mace would deal at the same fall height, for every worn heavy piece.
 *
 * <p>The damage comes from {@code MaceItem#getAttackDamageBonus} and the damage type from the normal
 * player attack source (1.21.1 has no separate mace smash type), so the curve (4·d / 12+2(d−3) /
 * 22+d−8), Density and Breach are exactly vanilla.</p>
 */
public final class ArmorSmash {
    /** Share of an equally high mace smash granted by each worn heavy armour piece. */
    public static final float PER_PIECE = 0.25F;
    /** Attack damage of a mace swing (mace 7 + player base 1); fixed so the held item cannot matter. */
    private static final float SMASH_ATTACK_DAMAGE = 8.0F;
    /** Minimum fall distance before a landing counts as a smash (vanilla mace uses 1.5). */
    private static final double MIN_FALL_DISTANCE = 3.0D;
    /**
     * A riptide dash is a horizontal, fast impact rather than a real fall, so the requirement is
     * relaxed to just above the vanilla mace threshold while the player is dashing.
     */
    private static final double RIPTIDE_MIN_FALL_DISTANCE = 1.6D;
    /** Upper bound for the impact height (vanilla mace caps the fall bonus at 8 blocks of growth). */
    private static final double MAX_IMPACT_HEIGHT = 20.0D;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Player -> entity id of the creature currently being smashed through (one hit per pass). */
    private static final Map<UUID, Integer> LAST_TARGET = new HashMap<>();
    /** Creature uuid -> the creature's own tickCount at its last armour smash. */
    private static final Map<UUID, Integer> LAST_SMASH_TICK = new HashMap<>();
    /** How long a creature is immune to further armour smashes (0.6 s). */
    public static final int SMASH_IMMUNITY_TICKS = 12;

    /** Pickup delay applied to arrows that were just reforged (1 second, in ticks). */
    private static final int ARROW_REFORGE_PICKUP_DELAY = 20;
    /** A landing on a dropped arrow reforges it into a heavy arrow after this much descent. */
    private static final double ARROW_REFORGE_DROP = 1.5D;

    /** Weapon that caused the smash currently being applied, for the weapon-specific death message. */
    private static ItemStack pendingSmashStack = ItemStack.EMPTY;

    private ArmorSmash() {
    }

    /** Empty unless a heavy-armour smash is being applied right now. */
    public static ItemStack pendingSmashStack() {
        return pendingSmashStack;
    }

    /** Called every server tick from {@link com.cecilia.heavygear.HeavyGearEvents}. */
    public static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        // One smash per pass: the guard is evaluated every tick, so it clears as soon as the player is
        // no longer inside the creature (bounced off by Wind Burst, knockback, walking away).
        Integer lastId = LAST_TARGET.get(id);
        if (lastId != null) {
            Entity last = player.level().getEntity(lastId);
            boolean stillTouching = last != null && last.isAlive()
                    && player.getBoundingBox().intersects(last.getBoundingBox().inflate(0.25));
            if (!stillTouching) {
                LAST_TARGET.remove(id);
                lastId = null;
            }
        }
        if (player.onGround()) {
            LAST_TARGET.remove(id);
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        // A smash is an impact coming *down*: moving upwards is not a landing.
        if (player.getDeltaMovement().y > 0.0D) {
            return;
        }
        int pieces = heavyPieces(player);
        if (com.cecilia.heavygear.effect.HeavyEffect.has(player)) {
            // the heavy potion makes any landing count as a full set of heavy armour
            pieces = 4;
        }
        if (pieces == 0) {
            return;
        }
        // Reforging dropped arrows only needs a 1.5 block drop, so this runs before the 3 block gate.
        double arrowDrop = player.isAutoSpinAttack()
                ? Math.max(player.fallDistance, player.getDeltaMovement().length())
                : (player.isFallFlying() ? DiveTracker.descended(player) : player.fallDistance);
        if (arrowDrop >= ARROW_REFORGE_DROP && convertDroppedArrows(player, pieces)) {
            return;
        }
        double impactHeight;
        boolean glideSmash = false;
        if (player.isAutoSpinAttack()) {
            impactHeight = Math.max(player.fallDistance, player.getDeltaMovement().length());
            impactHeight = Math.max(impactHeight, RIPTIDE_MIN_FALL_DISTANCE);
            impactHeight = Math.min(impactHeight, MAX_IMPACT_HEIGHT);
        } else if (player.isFallFlying()) {
            if (HeavyGearEnchantments.diveBombingLevel(player.getItemBySlot(EquipmentSlot.HEAD)) <= 0) {
                return;
            }
            double descended = DiveTracker.descended(player);
            if (descended < 1.5D) {
                return;
            }
            impactHeight = Math.min(Math.max(descended, RIPTIDE_MIN_FALL_DISTANCE), MAX_IMPACT_HEIGHT);
            glideSmash = true;
        } else {
            if (player.fallDistance < MIN_FALL_DISTANCE) {
                return;
            }
            impactHeight = player.fallDistance;
        }
        LivingEntity target = findTarget(player);
        if (target == null) {
            return;
        }
        if (isImmune(target)) {
            // a creature just got smashed by heavy armour: 0.6 s of immunity
            return;
        }
        if (lastId != null && lastId == target.getId()) {
            return;
        }
        LAST_TARGET.put(id, target.getId());
        smash(player, target, pieces, impactHeight, glideSmash);
        if (glideSmash) {
            // A dive is chained: every creature must be hit with the height fallen *since the last
            // smash*, otherwise damage keeps growing with the whole glide distance.
            DiveTracker.clear(player);
        }
    }

    /** How many of the four heavy armour pieces the player is wearing. */
    public static int heavyPieces(Player player) {
        int pieces = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (isHeavyArmour(player.getItemBySlot(slot).getItem())) {
                pieces++;
            }
        }
        return pieces;
    }

    public static boolean isHeavyArmour(Item item) {
        return item == HeavyGear.HEAVY_HELMET
                || item == HeavyGear.HEAVY_CHESTPLATE
                || item == HeavyGear.HEAVY_LEGGINGS
                || item == HeavyGear.HEAVY_BOOTS;
    }

    /** True while the creature is still immune to another heavy-armour smash (0.6 s). */
    public static boolean isImmune(LivingEntity target) {
        Integer last = LAST_SMASH_TICK.get(target.getUUID());
        return last != null && target.tickCount - last < SMASH_IMMUNITY_TICKS;
    }

    /**
     * The creature the player is falling through/onto: nearest one at or below their feet. The box is
     * swept along the current movement, because a riptide dash can cross a whole creature between two
     * ticks.
     */
    private static LivingEntity findTarget(ServerPlayer player) {
        AABB box = player.getBoundingBox().expandTowards(player.getDeltaMovement()).inflate(0.3, 0.5, 0.3);
        LivingEntity best = null;
        double bestOffset = Double.MAX_VALUE;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !e.isSpectator() && e.canBeSeenAsEnemy())) {
            if (candidate.getBoundingBox().minY > player.getY() + 0.5) {
                continue; // above the player: not something we land on
            }
            double offset = Math.abs(candidate.getY() - player.getY());
            if (offset < bestOffset) {
                bestOffset = offset;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Landing on dropped arrows with heavy armour reforges them into heavy arrows (needs a 1.5 block
     * drop). Returns true when at least one stack was converted.
     */
    private static boolean convertDroppedArrows(ServerPlayer player, int pieces) {
        AABB box = player.getBoundingBox().expandTowards(0.0D, -1.0D, 0.0D).inflate(0.5, 0.6, 0.5);
        boolean converted = false;
        for (net.minecraft.world.entity.item.ItemEntity item : player.level()
                .getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
            ItemStack stack = item.getItem();
            if (!stack.is(Items.ARROW)) {
                continue;
            }
            item.setItem(new ItemStack(com.cecilia.heavygear.item.HeavyGear.HEAVY_ARROW, stack.getCount()));
            // freshly forged arrows cannot be vacuumed up straight away: 1 second pickup delay
            item.setPickUpDelay(ARROW_REFORGE_PICKUP_DELAY);
            if (player.level() instanceof ServerLevel level) {
                // vanilla smash feedback, scaled subtly with the number of worn pieces
                float volume = 0.6F + 0.1F * pieces;
                level.playSound(null, item.getX(), item.getY(), item.getZ(),
                        net.minecraft.sounds.SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, volume, 1.0F);
                level.levelEvent(2013, item.getOnPos(), 750);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                        item.getX(), item.getY() + 0.2D, item.getZ(), 12, 0.35D, 0.2D, 0.35D, 0.1D);
            }
            converted = true;
        }
        return converted;
    }

    private static void smash(ServerPlayer player, LivingEntity target, int pieces, double impactHeight,
                              boolean glideSmash) {
        // the vanilla bonus is derived from fallDistance, so present the impact height as one
        double savedFallDistance = player.fallDistance;
        player.fallDistance = (float) Math.max(savedFallDistance, impactHeight);
        // Heavy armour grants Density and Breach to the smash. Vanilla reads both from the weapon of
        // the damage source (DamageSource#getWeaponItem -> attacker.getMainHandItem()), so the worn
        // pieces are folded into a temporary copy of the held stack for the duration of the hit.
        if (player.isFallFlying()) {
            // an elytra dive smash ends the firework boost that was pushing the player along
            HeavySmash.cancelFirework(player);
        }
        ItemStack realHand = player.getMainHandItem();
        ItemStack smashWeapon = grantSmashEnchantments(player);
        pendingSmashStack = realHand.copy();
        player.setItemSlot(EquipmentSlot.MAINHAND, smashWeapon);
        try {
            DamageSource source = player.damageSources().playerAttack(player);
            float maceBonus = Items.MACE.getAttackDamageBonus(target, 1.0F, source);
            if (maceBonus <= 0.0F) {
                return;
            }
            // The smash must only reflect the worn armour, so no value is taken from the held stack.
            float damage = (maceBonus + SMASH_ATTACK_DAMAGE) * criticalMultiplier(player)
                    * PER_PIECE * pieces;
            target.hurt(source, damage);
            LAST_SMASH_TICK.put(target.getUUID(), target.tickCount);
            if (LAST_SMASH_TICK.size() > 512) {
                LAST_SMASH_TICK.clear();
            }
            if (!target.isAlive()) {
                // "用沉重装备砸死一个生物": the worn armour killed it, so award the advancement directly
                grantKillAdvancement(player, "heavy_smash_kill", "killed_by_heavy_armour");
                if (glideSmash) {
                    // "借过一下": the kill came from an elytra dive with a dive bombing helmet
                    grantKillAdvancement(player, "glide_smash_kill", "glide_kill");
                }
            }
            // vanilla smash feedback (sound, particles, small upward impulse)
            Items.MACE.hurtEnemy(smashWeapon, target, player);
            // Wind Burst on the heavy boots: hand the boots to the vanilla post-attack pipeline so the
            // launch height, gust sound and particle amount are exactly the ones a mace smash produces.
            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            // Sneaking is the "brace for impact" stance: the landing still smashes, but Wind Burst on
            // the boots is suppressed so the player is not launched again.
            if (!boots.isEmpty() && !player.isShiftKeyDown()
                    && player.level() instanceof ServerLevel level) {
                // Vanilla's Wind Burst requirement forbids gliding and a dive always is gliding. The
                // glide is hidden for the duration of the pipeline, then restored right afterwards.
                boolean gliding = player.isFallFlying();
                if (gliding) {
                    ((EntitySharedFlagAccessor) player).heavygear$setSharedFlag(7, false);
                }
                try {
                    EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, source, boots);
                } finally {
                    if (gliding) {
                        ((EntitySharedFlagAccessor) player).heavygear$setSharedFlag(7, true);
                    }
                }
            }
            // consumes the fall / dash so one pass cannot smash twice
            Items.MACE.postHurtEnemy(smashWeapon, target, player);
        } finally {
            player.setItemSlot(EquipmentSlot.MAINHAND, realHand);
            player.fallDistance = (float) Math.min(player.fallDistance, savedFallDistance);
            pendingSmashStack = ItemStack.EMPTY;
        }
    }

    /**
     * A fresh mace stack carrying only the Density and Breach levels enchanted onto the worn heavy
     * armour (summed over the pieces, capped at the vanilla maximum of each enchantment).
     */
    private static ItemStack grantSmashEnchantments(ServerPlayer player) {
        ItemStack stack = new ItemStack(Items.MACE);
        HolderGetter<Enchantment> lookup = player.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);
        Optional<Holder.Reference<Enchantment>> densityHolder = lookup.get(Enchantments.DENSITY);
        Optional<Holder.Reference<Enchantment>> breachHolder = lookup.get(Enchantments.BREACH);
        int density = densityHolder.map(h -> wornLevel(player, h)).orElse(0);
        int breach = breachHolder.map(h -> wornLevel(player, h)).orElse(0);
        EnchantmentHelper.updateEnchantments(stack, enchantments -> {
            densityHolder.ifPresent(holder -> enchantments.set(holder, density));
            breachHolder.ifPresent(holder -> enchantments.set(holder, breach));
        });
        return stack;
    }

    /** Awards one of the smash kill advancements when a worn heavy armour smash kills something. */
    private static void grantKillAdvancement(ServerPlayer player, String path, String criterion) {
        if (!(player.level() instanceof ServerLevel level) || level.getServer() == null) {
            return;
        }
        var advancement = level.getServer().getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath("heavygear", path));
        if (advancement == null) {
            return;
        }
        player.getAdvancements().award(advancement, criterion);
    }

    /** The vanilla falling-attack critical multiplier (1.5), applied to a falling smash. */
    private static float criticalMultiplier(ServerPlayer player) {
        boolean critical = player.fallDistance > 0.0F
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.isSprinting()
                && !player.isPassenger()
                && !player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
        return critical ? 1.5F : 1.0F;
    }

    /** Total level of {@code enchantment} on the worn heavy pieces, capped at its vanilla maximum. */
    private static int wornLevel(Player player, Holder<Enchantment> enchantment) {
        int total = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack piece = player.getItemBySlot(slot);
            if (isHeavyArmour(piece.getItem())) {
                total += EnchantmentHelper.getItemEnchantmentLevel(enchantment, piece);
            }
        }
        return Math.min(total, enchantment.value().getMaxLevel());
    }
}
