package com.cecilia.heavygear;

import com.cecilia.heavygear.common.LonginusThrow;
import com.cecilia.heavygear.effect.HeavyEffect;
import com.cecilia.heavygear.entity.CompressedWindCharge;
import com.cecilia.heavygear.entity.DoubleCompressedWindCharge;
import com.cecilia.heavygear.entity.HeavyArrow;
import com.cecilia.heavygear.entity.ThrownLonginus;
import com.cecilia.heavygear.item.HeavyGear;
import com.cecilia.heavygear.item.LonginusMode;
import com.cecilia.heavygear.item.LonginusSpearItem;
import com.cecilia.heavygear.network.LonginusCancelPayload;
import com.cecilia.heavygear.network.LonginusModePayload;
import com.cecilia.heavygear.network.LonginusModeSyncPayload;
import com.cecilia.heavygear.network.LonginusThrowPayload;
import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Heavy Gear: the NeoForge 1.21.1 port of the Fabric 1.21.11 build.
 *
 * <p>Registration follows the upstream mod: every item, entity type, potion and data component is
 * registered straight into the vanilla registries from the mod constructor. NeoForge 1.21.1 runs mod
 * construction before the registries are frozen, so plain {@code Registry.register} calls are valid
 * and keep the static item fields usable from everywhere else in the mod.</p>
 */
@Mod(HeavyGearMod.MOD_ID)
public final class HeavyGearMod {
    public static final String MOD_ID = "heavygear";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final DataComponentType<Boolean> LONGINUS_TRIDENT_MODE = DataComponentType.<Boolean>builder()
            .persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL)
            .build();

    public static final ResourceLocation HEAVY_SPEAR_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "heavy_spear");
    public static final ResourceKey<Item> HEAVY_SPEAR_KEY = ResourceKey.create(Registries.ITEM, HEAVY_SPEAR_ID);
    public static final ResourceLocation TEMPLATE_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "heavy_upgrade_smithing_template");
    public static final ResourceKey<Item> TEMPLATE_KEY = ResourceKey.create(Registries.ITEM, TEMPLATE_ID);

    public static final ResourceLocation LONGINUS_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "lance_of_longinus");
    public static final ResourceKey<Item> LONGINUS_KEY = ResourceKey.create(Registries.ITEM, LONGINUS_ID);
    public static final ResourceLocation TRIDENT_TEMPLATE_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "trident_upgrade_smithing_template");
    public static final ResourceKey<Item> TRIDENT_TEMPLATE_KEY = ResourceKey.create(Registries.ITEM, TRIDENT_TEMPLATE_ID);

    public static final ResourceKey<CreativeModeTab> HEAVY_GEAR_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(MOD_ID, "heavy_gear"));

    public static final ResourceKey<EntityType<?>> THROWN_LONGINUS_KEY = ResourceKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "thrown_longinus"));
    public static final EntityType<ThrownLonginus> THROWN_LONGINUS_TYPE = EntityType.Builder
            .<ThrownLonginus>of((type, level) -> new ThrownLonginus(type, level), MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .clientTrackingRange(4)
            .updateInterval(20)
            .build("thrown_longinus");

    public static final ResourceKey<EntityType<?>> COMPRESSED_WIND_CHARGE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, "compressed_wind_charge"));
    public static final ResourceKey<EntityType<?>> DOUBLE_COMPRESSED_WIND_CHARGE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, "double_compressed_wind_charge"));

    public static final EntityType<CompressedWindCharge> COMPRESSED_WIND_CHARGE_TYPE = EntityType.Builder
            .<CompressedWindCharge>of((type, level) -> new CompressedWindCharge(type, level), MobCategory.MISC)
            .sized(0.3125F, 0.3125F)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build("compressed_wind_charge");

    public static final EntityType<DoubleCompressedWindCharge> DOUBLE_COMPRESSED_WIND_CHARGE_TYPE = EntityType.Builder
            .<DoubleCompressedWindCharge>of(
                    (type, level) -> new DoubleCompressedWindCharge(type, level), MobCategory.MISC)
            .sized(0.3125F, 0.3125F)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build("double_compressed_wind_charge");

    public static final ResourceKey<EntityType<?>> HEAVY_ARROW_KEY = ResourceKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "heavy_arrow"));
    public static final EntityType<HeavyArrow> HEAVY_ARROW_TYPE = EntityType.Builder
            .<HeavyArrow>of((type, level) -> new HeavyArrow(type, level), MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .clientTrackingRange(4)
            .updateInterval(20)
            .build("heavy_arrow");

    public static final Item HEAVY_SPEAR =
            new com.cecilia.heavygear.item.HeavySpearItem(net.minecraft.world.item.Tiers.NETHERITE, spearProperties());
    public static final Item LONGINUS_SPEAR = new LonginusSpearItem(spearProperties());
    public static final Item HEAVY_UPGRADE_SMITHING_TEMPLATE =
            new com.cecilia.heavygear.item.HeavyUpgradeTemplateItem();
    public static final Item TRIDENT_UPGRADE_SMITHING_TEMPLATE =
            new com.cecilia.heavygear.item.TridentUpgradeTemplateItem();

    /**
     * 1.21.1 has no spear item family: the spear components (piercing stab, kinetic charge attack,
     * reach, sounds, model) are copied from the Backported Spears mod's {@code minecraft:netherite_spear}
     * by {@code SpearComponents} once every mod has registered its items, so only the tier, the
     * rarity and the fire resistance are set here.
     */
    private static Item.Properties spearProperties() {
        return new Item.Properties()
                .rarity(Rarity.EPIC)
                .fireResistant();
    }

    public HeavyGearMod(IEventBus modEventBus, ModContainer modContainer) {
        registerContent();
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::addVanillaTabEntries);
        modEventBus.addListener(com.cecilia.heavygear.item.SpearComponents::copyFromVanillaSpear);
        NeoForge.EVENT_BUS.addListener(this::addBrewingRecipes);
    }

    private void registerContent() {
        Registry.register(BuiltInRegistries.ITEM, HEAVY_SPEAR_KEY, HEAVY_SPEAR);
        Registry.register(BuiltInRegistries.ITEM, LONGINUS_KEY, LONGINUS_SPEAR);
        Registry.register(BuiltInRegistries.ITEM, TEMPLATE_KEY, HEAVY_UPGRADE_SMITHING_TEMPLATE);
        Registry.register(BuiltInRegistries.ITEM, TRIDENT_TEMPLATE_KEY, TRIDENT_UPGRADE_SMITHING_TEMPLATE);
        HeavyGear.register();
        HeavyEffect.bootstrap();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, HEAVY_GEAR_TAB_KEY, CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.heavygear.heavy_gear"))
                .icon(() -> new ItemStack(HEAVY_SPEAR))
                .displayItems((params, output) -> {
                    // weapons -> tools -> ranged -> fishing -> armour -> templates
                    output.accept(HEAVY_SPEAR);
                    output.accept(LONGINUS_SPEAR);
                    output.accept(HeavyGear.HEAVY_SWORD);
                    output.accept(HeavyGear.HEAVY_AXE);
                    output.accept(HeavyGear.HEAVY_PICKAXE);
                    output.accept(HeavyGear.HEAVY_SHOVEL);
                    output.accept(HeavyGear.HEAVY_HOE);
                    output.accept(HeavyGear.HEAVY_BOW);
                    output.accept(HeavyGear.HEAVY_CROSSBOW);
                    output.accept(HeavyGear.HEAVY_ARROW);
                    output.accept(HeavyGear.HEAVY_FISHING_ROD);
                    output.accept(HeavyGear.HEAVY_HELMET);
                    output.accept(HeavyGear.HEAVY_CHESTPLATE);
                    output.accept(HeavyGear.HEAVY_LEGGINGS);
                    output.accept(HeavyGear.HEAVY_BOOTS);
                    output.accept(HEAVY_UPGRADE_SMITHING_TEMPLATE);
                    output.accept(TRIDENT_UPGRADE_SMITHING_TEMPLATE);
                    output.accept(HeavyGear.ECAM);
                    output.accept(HeavyGear.COMPRESSED_WIND_CHARGE);
                    output.accept(HeavyGear.DOUBLE_COMPRESSED_WIND_CHARGE);
                    output.accept(HeavyGear.HEAVY_POTION);
                    output.accept(HeavyGear.HEAVY_SPLASH_POTION);
                    output.accept(HeavyGear.HEAVY_LINGERING_POTION);
                })
                .build());

        Registry.register(BuiltInRegistries.ENTITY_TYPE, THROWN_LONGINUS_KEY, THROWN_LONGINUS_TYPE);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, HEAVY_ARROW_KEY, HEAVY_ARROW_TYPE);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, COMPRESSED_WIND_CHARGE_KEY, COMPRESSED_WIND_CHARGE_TYPE);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, DOUBLE_COMPRESSED_WIND_CHARGE_KEY,
                DOUBLE_COMPRESSED_WIND_CHARGE_TYPE);

        // dispensers must throw the compressed charges too
        DispenserBlock.registerBehavior(HeavyGear.COMPRESSED_WIND_CHARGE,
                new ProjectileDispenseBehavior(HeavyGear.COMPRESSED_WIND_CHARGE));
        DispenserBlock.registerBehavior(HeavyGear.DOUBLE_COMPRESSED_WIND_CHARGE,
                new ProjectileDispenseBehavior(HeavyGear.DOUBLE_COMPRESSED_WIND_CHARGE));

        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "longinus_trident_mode"), LONGINUS_TRIDENT_MODE);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(LonginusThrowPayload.TYPE, LonginusThrowPayload.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            LOGGER.info("longinus payload received hand={} charge={} player={}", payload.hand(),
                    payload.chargeTicks(), player.getName().getString());
            context.enqueueWork(() -> LonginusThrow.perform(player, payload));
        });

        registrar.playToServer(LonginusCancelPayload.TYPE, LonginusCancelPayload.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(player::stopUsingItem);
        });

        registrar.playToServer(LonginusModePayload.TYPE, LonginusModePayload.CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> {
                ItemStack stack = player.getItemInHand(payload.hand());
                if (stack.getItem() instanceof LonginusSpearItem) {
                    boolean trident = !LonginusMode.isTrident(stack);
                    LonginusMode.setTrident(stack, trident);
                    LOGGER.info("longinus mode set trident={} player={}", trident, player.getName().getString());
                    PacketDistributor.sendToPlayer(player, new LonginusModeSyncPayload(trident));
                    player.displayClientMessage(Component.translatable(
                            "message.heavygear.longinus_mode.prefix").append(Component.translatable(trident
                            ? "message.heavygear.longinus_mode.trident"
                            : "message.heavygear.longinus_mode.normal")
                            .withStyle(trident ? net.minecraft.ChatFormatting.AQUA
                                    : net.minecraft.ChatFormatting.GREEN)), true);
                }
            });
        });
    }

    /** Mirrors the upstream {@code ItemGroupEvents} calls with the NeoForge tab contents event. */
    private void addVanillaTabEntries(BuildCreativeModeTabContentsEvent event) {
        CreativeModeTab.TabVisibility visible = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        ResourceKey<CreativeModeTab> key = event.getTabKey();
        if (key == CreativeModeTabs.COMBAT) {
            // the heavy spear belongs behind the netherite spear (Backported Spears), and the lance
            // right in front of the mace, like the 1.21.11 spear family
            Item netheriteSpear = BuiltInRegistries.ITEM
                    .getOptional(ResourceLocation.withDefaultNamespace("netherite_spear"))
                    .orElse(Items.AIR);
            Item spearAnchor = netheriteSpear == Items.AIR ? Items.TRIDENT : netheriteSpear;
            event.insertAfter(new ItemStack(spearAnchor), new ItemStack(HEAVY_SPEAR), visible);
            event.insertBefore(new ItemStack(Items.MACE), new ItemStack(LONGINUS_SPEAR), visible);
            event.insertAfter(new ItemStack(Items.NETHERITE_SWORD), new ItemStack(HeavyGear.HEAVY_SWORD), visible);
            event.insertAfter(new ItemStack(Items.NETHERITE_AXE), new ItemStack(HeavyGear.HEAVY_AXE), visible);
            event.insertAfter(new ItemStack(Items.NETHERITE_BOOTS), new ItemStack(HeavyGear.HEAVY_HELMET), visible);
            event.insertAfter(new ItemStack(HeavyGear.HEAVY_HELMET), new ItemStack(HeavyGear.HEAVY_CHESTPLATE), visible);
            event.insertAfter(new ItemStack(HeavyGear.HEAVY_CHESTPLATE), new ItemStack(HeavyGear.HEAVY_LEGGINGS), visible);
            event.insertAfter(new ItemStack(HeavyGear.HEAVY_LEGGINGS), new ItemStack(HeavyGear.HEAVY_BOOTS), visible);
            event.insertAfter(new ItemStack(Items.BOW), new ItemStack(HeavyGear.HEAVY_BOW), visible);
            event.insertAfter(new ItemStack(Items.CROSSBOW), new ItemStack(HeavyGear.HEAVY_CROSSBOW), visible);
            event.insertAfter(new ItemStack(Items.ARROW), new ItemStack(HeavyGear.HEAVY_ARROW), visible);
        } else if (key == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.insertAfter(new ItemStack(Items.NETHERITE_HOE), new ItemStack(HeavyGear.HEAVY_SHOVEL), visible);
            event.insertAfter(new ItemStack(HeavyGear.HEAVY_SHOVEL), new ItemStack(HeavyGear.HEAVY_PICKAXE), visible);
            event.insertAfter(new ItemStack(HeavyGear.HEAVY_PICKAXE), new ItemStack(HeavyGear.HEAVY_AXE), visible);
            event.insertAfter(new ItemStack(HeavyGear.HEAVY_AXE), new ItemStack(HeavyGear.HEAVY_HOE), visible);
            event.insertAfter(new ItemStack(Items.FISHING_ROD), new ItemStack(HeavyGear.HEAVY_FISHING_ROD), visible);
            event.insertAfter(new ItemStack(HeavyGear.HEAVY_FISHING_ROD),
                    new ItemStack(HeavyGear.HEAVY_POTION), visible);
            event.insertAfter(new ItemStack(HeavyGear.HEAVY_POTION),
                    new ItemStack(HeavyGear.HEAVY_SPLASH_POTION), visible);
            event.insertAfter(new ItemStack(HeavyGear.HEAVY_SPLASH_POTION),
                    new ItemStack(HeavyGear.HEAVY_LINGERING_POTION), visible);
        } else if (key == CreativeModeTabs.INGREDIENTS) {
            event.insertAfter(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                    new ItemStack(HEAVY_UPGRADE_SMITHING_TEMPLATE), visible);
            event.insertAfter(new ItemStack(HEAVY_UPGRADE_SMITHING_TEMPLATE),
                    new ItemStack(TRIDENT_UPGRADE_SMITHING_TEMPLATE), visible);
        }
    }

    private void addBrewingRecipes(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addMix(Potions.AWKWARD, Items.HEAVY_CORE, HeavyEffect.POTION);
        event.getBuilder().addMix(HeavyEffect.POTION, Items.REDSTONE, HeavyEffect.POTION_LONG);
    }
}
