package com.cecilia.heavygear.item;

import com.cecilia.heavygear.HeavyGearMod;

import java.util.List;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;

/**
 * Heavy variants of the netherite loadout: sword, axe, shovel, pickaxe and hoe plus the four armour
 * pieces. Every number (durability, damage, mining speed, defence, toughness, knockback resistance,
 * enchantability, repair ingredient, fire resistance) is taken from the vanilla netherite equivalent
 * (1.21.1 registers those through the {@link Tiers#NETHERITE} tier), so the only difference is the
 * model, the name, the armour layer and the mace smash the weapons gain.
 *
 * <p>1.21.1 still ships the dedicated tool classes ({@link SwordItem}, {@link AxeItem}, ...) and
 * {@link ArmorItem}, so the heavy gear extends them instead of baking components by hand as the
 * 1.21.11 build had to.</p>
 */
public final class HeavyGear {
    private HeavyGear() {
    }

    /** Netherite armour material, re-skinned with our own {@code models/armor/heavy} layer set. */
    public static final ArmorMaterial HEAVY_ARMOR_MATERIAL = new ArmorMaterial(
            Map.of(
                    ArmorItem.Type.HELMET, 3,
                    ArmorItem.Type.CHESTPLATE, 8,
                    ArmorItem.Type.LEGGINGS, 6,
                    ArmorItem.Type.BOOTS, 3),
            15,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(Items.NETHERITE_INGOT),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(HeavyGearMod.MOD_ID, "heavy"))),
            3.0F,
            0.1F);

    public static final Holder<ArmorMaterial> HEAVY_ARMOR = Registry.registerForHolder(
            BuiltInRegistries.ARMOR_MATERIAL,
            ResourceLocation.fromNamespaceAndPath(HeavyGearMod.MOD_ID, "heavy"),
            HEAVY_ARMOR_MATERIAL);

    public static final ResourceKey<Item> SWORD_KEY = key("heavy_sword");
    public static final ResourceKey<Item> AXE_KEY = key("heavy_axe");
    public static final ResourceKey<Item> SHOVEL_KEY = key("heavy_shovel");
    public static final ResourceKey<Item> PICKAXE_KEY = key("heavy_pickaxe");
    public static final ResourceKey<Item> HOE_KEY = key("heavy_hoe");
    public static final ResourceKey<Item> HELMET_KEY = key("heavy_helmet");
    public static final ResourceKey<Item> CHESTPLATE_KEY = key("heavy_chestplate");
    public static final ResourceKey<Item> LEGGINGS_KEY = key("heavy_leggings");
    public static final ResourceKey<Item> BOOTS_KEY = key("heavy_boots");

    // Vanilla netherite tool numbers, verbatim from Items (netherite_*).
    public static final Item HEAVY_SWORD = new HeavySwordItem(Tiers.NETHERITE, base()
            .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 3, -2.4F)));
    public static final Item HEAVY_AXE = new HeavyAxeItem(Tiers.NETHERITE, base()
            .attributes(AxeItem.createAttributes(Tiers.NETHERITE, 5.0F, -3.0F)));
    public static final Item HEAVY_SHOVEL = new HeavyShovelItem(Tiers.NETHERITE, base()
            .attributes(ShovelItem.createAttributes(Tiers.NETHERITE, 1.5F, -3.0F)));
    public static final Item HEAVY_PICKAXE = new HeavyPickaxeItem(Tiers.NETHERITE, base()
            .attributes(PickaxeItem.createAttributes(Tiers.NETHERITE, 1.0F, -2.8F)));
    public static final Item HEAVY_HOE = new HeavyHoeItem(Tiers.NETHERITE, base()
            .attributes(HoeItem.createAttributes(Tiers.NETHERITE, -4.0F, 0.0F)));

    public static final Item HEAVY_HELMET = new HeavyArmorItem(HEAVY_ARMOR, ArmorItem.Type.HELMET,
            base().durability(ArmorItem.Type.HELMET.getDurability(37)));
    public static final Item HEAVY_CHESTPLATE = new HeavyArmorItem(HEAVY_ARMOR, ArmorItem.Type.CHESTPLATE,
            base().durability(ArmorItem.Type.CHESTPLATE.getDurability(37)));
    public static final Item HEAVY_LEGGINGS = new HeavyArmorItem(HEAVY_ARMOR, ArmorItem.Type.LEGGINGS,
            base().durability(ArmorItem.Type.LEGGINGS.getDurability(37)));
    public static final Item HEAVY_BOOTS = new HeavyArmorItem(HEAVY_ARMOR, ArmorItem.Type.BOOTS,
            base().durability(ArmorItem.Type.BOOTS.getDurability(37)));

    public static final ResourceKey<Item> BOW_KEY = key("heavy_bow");
    public static final ResourceKey<Item> CROSSBOW_KEY = key("heavy_crossbow");
    public static final ResourceKey<Item> FISHING_ROD_KEY = key("heavy_fishing_rod");
    public static final ResourceKey<Item> ARROW_KEY = key("heavy_arrow");
    public static final ResourceKey<Item> ECAM_KEY = key("ecam");
    public static final ResourceKey<Item> COMPRESSED_WIND_CHARGE_KEY = key("compressed_wind_charge");
    public static final ResourceKey<Item> POTION_KEY = key("heavy_potion");
    public static final ResourceKey<Item> SPLASH_POTION_KEY = key("heavy_splash_potion");
    public static final ResourceKey<Item> LINGERING_POTION_KEY = key("heavy_lingering_potion");

    /** Vanilla bow numbers (durability 384, 1 durability per shot). */
    public static final Item HEAVY_BOW = new HeavyBowItem(base().durability(384));
    /** Vanilla crossbow numbers (durability 465, 1.25 s charge). */
    public static final Item HEAVY_CROSSBOW = new HeavyCrossbowItem(base().durability(465));
    /** Vanilla fishing rod numbers (durability 64). */
    public static final Item HEAVY_FISHING_ROD = new HeavyFishingRodItem(base().durability(64));
    /** Ammo for the heavy bow / crossbow. */
    public static final Item HEAVY_ARROW = new HeavyArrowItem(base());
    /** 压缩风弹: nine wind charges, five times the impact of one. */
    public static final Item COMPRESSED_WIND_CHARGE = new CompressedWindChargeItem(
            base().stacksTo(64).rarity(Rarity.COMMON), 10,
            (level) -> new com.cecilia.heavygear.entity.CompressedWindCharge(
                    com.cecilia.heavygear.HeavyGearMod.COMPRESSED_WIND_CHARGE_TYPE, level));

    /** 二重压缩风弹: four compressed wind charges, four times the impact of one wind charge. */
    public static final ResourceKey<Item> DOUBLE_COMPRESSED_WIND_CHARGE_KEY = key("double_compressed_wind_charge");
    public static final Item DOUBLE_COMPRESSED_WIND_CHARGE = new CompressedWindChargeItem(
            base().stacksTo(64).rarity(Rarity.EPIC), 10,
            (level) -> new com.cecilia.heavygear.entity.DoubleCompressedWindCharge(
                    com.cecilia.heavygear.HeavyGearMod.DOUBLE_COMPRESSED_WIND_CHARGE_TYPE, level));

    /** 锤重 / ecaM: the reverse mace - the smash lands on the wielder. */
    public static final Item ECAM = new MaceItem(new Item.Properties()
            .rarity(Rarity.EPIC)
            .stacksTo(1)
            .durability(500)
            .fireResistant()
            .component(net.minecraft.core.component.DataComponents.TOOL, MaceItem.createToolProperties())
            .attributes(MaceItem.createAttributes()));

    /** Drinkable heavy potion (potion bottle look, tinted with the effect colour). */
    public static final Item HEAVY_POTION = new HeavyPotionItem(potionProperties());
    /** Splash variant. */
    public static final Item HEAVY_SPLASH_POTION = new HeavySplashPotionItem(potionProperties());
    /** Lingering variant. */
    public static final Item HEAVY_LINGERING_POTION = new HeavyLingeringPotionItem(potionProperties());

    /** Creative tab {@code combat}: weapons and armour. */
    public static final List<Item> COMBAT = List.of(
            HEAVY_SWORD, HEAVY_AXE, HEAVY_HELMET, HEAVY_CHESTPLATE, HEAVY_LEGGINGS, HEAVY_BOOTS);
    /** Creative tab {@code tools}. */
    public static final List<Item> TOOLS = List.of(HEAVY_PICKAXE, HEAVY_SHOVEL, HEAVY_HOE);

    private record Entry(ResourceKey<Item> key, Item item) {
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry(SWORD_KEY, HEAVY_SWORD),
            new Entry(AXE_KEY, HEAVY_AXE),
            new Entry(SHOVEL_KEY, HEAVY_SHOVEL),
            new Entry(PICKAXE_KEY, HEAVY_PICKAXE),
            new Entry(HOE_KEY, HEAVY_HOE),
            new Entry(HELMET_KEY, HEAVY_HELMET),
            new Entry(CHESTPLATE_KEY, HEAVY_CHESTPLATE),
            new Entry(LEGGINGS_KEY, HEAVY_LEGGINGS),
            new Entry(BOOTS_KEY, HEAVY_BOOTS),
            new Entry(BOW_KEY, HEAVY_BOW),
            new Entry(CROSSBOW_KEY, HEAVY_CROSSBOW),
            new Entry(FISHING_ROD_KEY, HEAVY_FISHING_ROD),
            new Entry(ARROW_KEY, HEAVY_ARROW),
            new Entry(ECAM_KEY, ECAM),
            new Entry(COMPRESSED_WIND_CHARGE_KEY, COMPRESSED_WIND_CHARGE),
            new Entry(DOUBLE_COMPRESSED_WIND_CHARGE_KEY, DOUBLE_COMPRESSED_WIND_CHARGE),
            new Entry(POTION_KEY, HEAVY_POTION),
            new Entry(SPLASH_POTION_KEY, HEAVY_SPLASH_POTION),
            new Entry(LINGERING_POTION_KEY, HEAVY_LINGERING_POTION));

    private static ResourceKey<Item> key(String path) {
        return ResourceKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(HeavyGearMod.MOD_ID, path));
    }

    private static Item.Properties base() {
        return new Item.Properties()
                .rarity(Rarity.EPIC)
                .fireResistant();
    }

    private static Item.Properties potionProperties() {
        return new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant()
                .component(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                        new net.minecraft.world.item.alchemy.PotionContents(
                                com.cecilia.heavygear.effect.HeavyEffect.POTION));
    }

    /** Registers every heavy tool and armour piece into the item registry. */
    public static void register() {
        for (Entry entry : ENTRIES) {
            Registry.register(BuiltInRegistries.ITEM, entry.key(), entry.item());
        }
    }
}
