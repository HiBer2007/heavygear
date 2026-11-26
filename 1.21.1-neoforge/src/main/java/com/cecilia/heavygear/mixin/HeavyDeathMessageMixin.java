package com.cecilia.heavygear.mixin;

import com.cecilia.heavygear.HeavyGearMod;
import com.cecilia.heavygear.combat.ArmorSmash;
import com.cecilia.heavygear.combat.SmashAttribution;
import com.cecilia.heavygear.entity.HeavyArrow;
import com.cecilia.heavygear.entity.ThrownLonginus;
import com.cecilia.heavygear.item.HeavyGear;
import com.cecilia.heavygear.item.HeavySmash;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Weapon specific death messages for the heavy gear.
 *
 * <p>Vanilla picks the message from the damage type, and every heavy smash rides on the ordinary
 * {@code player_attack} / {@code arrow} types (1.21.1 has no dedicated mace smash type), so all of
 * them would read as a plain melee or arrow kill. The message is intercepted instead: the Spear of
 * Longinus keeps its own text, everything else reports the heavy item that actually dealt the blow.</p>
 */
@Mixin(DamageSource.class)
public abstract class HeavyDeathMessageMixin {
    private static final String PREFIX = "death.attack.heavygear.";
    /** Any arrow hit (heavy or plain) uses the arrow wording. */
    private static final ResourceKey<DamageType> ARROW_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.withDefaultNamespace("arrow"));

    @Inject(method = "getLocalizedDeathMessage", at = @At("HEAD"), cancellable = true)
    private void heavygear$deathMessage(LivingEntity victim, CallbackInfoReturnable<Component> cir) {
        DamageSource source = (DamageSource) (Object) this;
        Entity killer = source.getEntity();
        if (killer == null || killer == victim) {
            return;
        }
        ItemStack weapon = heavygear$weapon(source);
        if (weapon.isEmpty() && !com.cecilia.heavygear.effect.HeavyEffect.has(killer)) {
            // a bare fist still smashes under the heavy potion, so it must not bail out here
            return;
        }
        if (heavygear$isLance(source)) {
            boolean named = weapon.has(DataComponents.CUSTOM_NAME);
            cir.setReturnValue(named
                    ? Component.translatable(PREFIX + "lance.named",
                    victim.getDisplayName(), killer.getDisplayName(), weapon.getHoverName())
                    : Component.translatable(PREFIX + "lance",
                    victim.getDisplayName(), killer.getDisplayName()));
            return;
        }
        if (!heavygear$wasSmash(source, victim)) {
            // an ordinary hit with a heavy weapon keeps the vanilla message
            return;
        }
        String key = heavygear$key(source);
        if (key == null) {
            return;
        }
        if ("generic".equals(key)) {
            cir.setReturnValue(Component.translatable(PREFIX + "smash.generic",
                    victim.getDisplayName(), killer.getDisplayName()));
            return;
        }
        // the item only names itself when it was renamed, like the vanilla wording
        boolean renamed = weapon.has(DataComponents.CUSTOM_NAME);
        cir.setReturnValue(renamed
                ? Component.translatable(PREFIX + "smash." + key + ".named",
                victim.getDisplayName(), killer.getDisplayName(), weapon.getHoverName())
                : Component.translatable(PREFIX + "smash." + key,
                victim.getDisplayName(), killer.getDisplayName()));
    }

    /** A smash claim is only valid when the smash attribution was recorded for this victim. */
    private static boolean heavygear$wasSmash(DamageSource source, LivingEntity victim) {
        if (source.getDirectEntity() instanceof HeavyArrow arrow && arrow.heavygear$didSmash(victim)) {
            return true;
        }
        if (SmashAttribution.didSmash(victim)) {
            return true;
        }
        return source.is(ARROW_TYPE) && com.cecilia.heavygear.effect.HeavyEffect.has(source.getEntity());
    }

    /** The stack that dealt the blow (bows report their own name for an arrow kill). */
    private static ItemStack heavygear$weapon(DamageSource source) {
        if (source.getDirectEntity() instanceof HeavyArrow arrow) {
            return arrow.getWeaponItem();
        }
        if (!ArmorSmash.pendingSmashStack().isEmpty()) {
            // an armour smash swaps in a mace for the hit, so the real weapon is kept aside
            return ArmorSmash.pendingSmashStack();
        }
        ItemStack weapon = source.getWeaponItem();
        return weapon == null ? ItemStack.EMPTY : weapon;
    }

    private static boolean heavygear$isLance(DamageSource source) {
        if (source.getWeaponItem() != null && source.getWeaponItem().is(HeavyGearMod.LONGINUS_SPEAR)) {
            return true;
        }
        return source.getDirectEntity() instanceof ThrownLonginus;
    }

    /** {@code sword}, {@code axe}, ... or null when the blow was not heavy gear. */
    private static String heavygear$key(DamageSource source) {
        if (source.is(ARROW_TYPE)
                || source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow) {
            return "arrow";
        }
        if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.FishingHook) {
            return "fishing_rod";
        }
        // under the heavy potion any item works: classify by the vanilla tool tags
        if (com.cecilia.heavygear.effect.HeavyEffect.has(source.getEntity())) {
            String byTag = heavygear$tagKey(source.getWeaponItem());
            return byTag == null ? "generic" : byTag;
        }
        ItemStack held = source.getWeaponItem();
        String key = held == null ? null : heavygear$keyOf(held);
        if (key != null) {
            return key;
        }
        return heavygear$keyOf(ArmorSmash.pendingSmashStack());
    }

    /** Classifies any item by the vanilla tool tags (used while the heavy potion is active). */
    private static String heavygear$tagKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "generic";
        }
        String own = heavygear$keyOf(stack);
        if (own != null) {
            return own;
        }
        if (heavygear$isIn(stack, "swords")) {
            return "sword";
        }
        if (heavygear$isIn(stack, "axes")) {
            return "axe";
        }
        if (heavygear$isIn(stack, "pickaxes")) {
            return "pickaxe";
        }
        if (heavygear$isIn(stack, "shovels")) {
            return "shovel";
        }
        if (heavygear$isIn(stack, "hoes")) {
            return "hoe";
        }
        String heldPath = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).getPath();
        if (heldPath.contains("bow") || heldPath.contains("crossbow")) {
            return "arrow";
        }
        if (heldPath.contains("spear") || heldPath.contains("trident") || heldPath.contains("lance")) {
            return "spear";
        }
        return null;
    }

    private static boolean heavygear$isIn(ItemStack stack, String path) {
        return stack.is(net.minecraft.tags.TagKey.create(Registries.ITEM,
                ResourceLocation.withDefaultNamespace(path)));
    }

    private static String heavygear$keyOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (item == HeavyGear.HEAVY_SWORD) {
            return "sword";
        }
        if (item == HeavyGear.HEAVY_AXE) {
            return "axe";
        }
        if (item == HeavyGear.HEAVY_PICKAXE) {
            return "pickaxe";
        }
        if (item == HeavyGear.HEAVY_HOE) {
            return "hoe";
        }
        if (item == HeavyGear.HEAVY_SHOVEL) {
            return "shovel";
        }
        if (item == HeavyGearMod.HEAVY_SPEAR) {
            return "spear";
        }
        if (item == HeavyGear.HEAVY_FISHING_ROD) {
            return "fishing_rod";
        }
        return null;
    }
}
