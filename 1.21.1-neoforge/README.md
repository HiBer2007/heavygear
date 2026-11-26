# Heavy Gear — NeoForge 1.21.1 移植

本目录是 [Heavy Gear](https://github.com/CeciliAqwq/heavygear)（Fabric 1.21.11 / 26.x 多版本工程）到
**NeoForge 1.21.1** 的移植工程。它是**独立 Gradle 构建**：仓库根目录的 `settings.gradle` 仍然只包含原有的
Fabric 子工程，本目录用自己的 `settings.gradle` / `build.gradle`，互不影响。

## 构建

```powershell
cd 1.21.1-neoforge
.\gradlew.bat build
```

产物：`build/libs/heavygear-1.0.0+1.21.1-neoforge.jar`

- 工具链：Gradle 9.2.1（wrapper 指向阿里云镜像）、JDK 21、ModDevGradle `2.0.141`、NeoForge `21.1.266`。
- 镜像源（`settings.gradle` 里统一改写，插件运行时新加的仓库也会被覆盖）：
  - `https://maven.neoforged.net` → `https://neoforged.forgecdn.net`（本站直连超时）
  - `https://repo1.maven.org/maven2`、`https://maven.minecraftforge.net` → `https://maven.aliyun.com/repository/public`
- 首次构建会由 NeoFormRuntime 复用本机已缓存的 1.21.1 client/server 产物，不需要重新反编译。

## 版本差异处理（1.21.11 → 1.21.1）

1.21.11 的一部分 API 在 1.21.1 并不存在，移植时逐个改写，原则是"能用 1.21.1 原生机制表达的就按原生写，
表达不了的用等价的 NeoForge 事件/实体逻辑替代"。

| 1.21.11（Fabric 版） | 1.21.1（本移植） |
| --- | --- |
| `ModInitializer` / `ClientModInitializer` | `@Mod` 构造器 + `@EventBusSubscriber` |
| `Registry.register(BuiltInRegistries.*)` | 保留（NeoForge 构造期早于注册表冻结），行为一致 |
| `FabricItemGroup` / `ItemGroupEvents` | `CreativeModeTab.builder()` + `BuildCreativeModeTabContentsEvent` |
| `PayloadTypeRegistry` / `ServerPlayNetworking` | `RegisterPayloadHandlersEvent` + `PayloadRegistrar` + `PacketDistributor` |
| `EntityRendererRegistry` | `EntityRenderersEvent.RegisterRenderers` |
| `KeyBindingHelper` | `RegisterKeyMappingsEvent` |
| `PlayerTickDiveMixin`、`PlayerFlyHigherMixin`、`CompressedWindChargeFallMixin` | `PlayerTickEvent.Post`、`LivingFallEvent` |
| `PotionBrewingHeavyMixin` | `RegisterBrewingRecipesEvent` |
| 风弹 `CompressedWindChargeMarker/Knockback` 三个 mixin | `CompressedWindCharge#explode` 内先走原版爆炸，再按 `impactScale` 缩放速度增量 |
| `Item.Properties.setId()` / `.spear()` / `.sword()` / `.pickaxe()` | 1.21.1 的 `Tiers.NETHERITE` + `SwordItem`/`AxeItem`/… 与 `TridentItem.createAttributes()`（重矛） |
| `ArmorMaterial`（装备资源 `equipment/*.json`） | 1.21.1 `ArmorMaterial` record + `textures/models/armor/heavy_layer_{1,2}.png` |
| 物品模型 `assets/heavygear/items/*.json`（item model definition） | 删除；`models/item/*.json` 改用 `overrides`（弓/弩/钓竿的 pulling/cast/charged 状态） |
| 矛的双持/显示上下文模型（`display_context`） | 1.21.1 无该机制，重矛/圣枪使用 2D 图标模型，`*_in_hand.json` 保留但暂未被引用 |
| `spear` 家族的刺击/突进附魔钩子（`DiveStabMixin`、`RiptideLungeGuardMixin`） | 删除：1.21.1 没有矛的 `stabAttack` / `post_piercing_attack` |
| 三叉戟渲染状态 mixin（`ThrownTridentRendererMixin`、`TridentRenderStateMixin`） | 改为 `LonginusTridentRenderer extends ThrownTridentRenderer`（1.21.1 走 `getTextureLocation`） |
| 重型钓竿浮标贴图 mixin（`HeavyFishingBobberMixin`） | 删除：1.21.1 的 `FishingHookRenderer` 没有渲染状态钩子；浮标仍用原版贴图 |
| `mace_smash` 伤害类型 | 1.21.1 不存在；砸击伤害挂在普通 `player_attack` / `arrow` 上，靠 `SmashAttribution` 记录归属，死亡消息混入判断 |
| `method_64193`（mace 伤害源）、`method_5997` 等中介名 | 换成本映射名：`canSmashAttack`、`doAutoAttackOnTouch`、`actualHurt` 等 |
| `hurtOrSimulate`、`fallDistance` 为 double | 1.21.1 用 `hurt(...)`，`fallDistance` 为 float |

保留并改写的方法级 mixin：`HeavySmashMixin`（任意物品在沉重药水下的砸击）、`MaceItemMixin`（滑翔俯冲解除砸击限制）、
`MaceSmashSupportMixin`、`DiveSmashDamageMixin`、`DiveWindBurstMixin`、`ArrowEffectSmashMixin`、
`EffectTopUpMixin` / `EffectPlayerTopUpMixin`、`EcamAttackMixin`、`EcamTooltipMixin`、`HeavyBobberSmashMixin`、
`HeavyRodMeleeGuardMixin`、`HeavyDeathMessageMixin`、`RiptideContactMixin`、`RiptideContactNoBurstMixin`、
`SmithingEnchantMergeMixin`、`ThrownTridentSmashMixin`、`VanillaItemEquivalenceMixin`、
`EnchantmentCompatibilityMixin`，以及 4 个 accessor（箭头武器/穿透、实体 shared flag、烟花挂载实体）。

## 数据包改动

- 删除 `data/minecraft/tags/item/spears.json`（1.21.11 才有 `spears` 标签，且内容全是 1.21.11 物品）。
- `recipe/heavy_spear.json` 的底材由 `minecraft:netherite_spear` 改为 `minecraft:trident`。
- 末地城战利品表里的 `minecraft:diamond_spear` 改为 `minecraft:trident`。
- `advancement/final_offering.json` 的 `item_used_on_block` 条件从列表改为 1.21.1 的单对象形式。
- 附魔（density / breach / wind_burst / infinity / channeling）与标签格式本身就是 1.21.1 结构，未改。
- 物品/方块/附魔/实体引用已全部与 1.21.1 注册表核对，所有 JSON 通过解析校验。

## 验证程度

- `gradlew build` 通过，产出 269 项的 jar；`neoforge.mods.toml` 占位符展开正确、编码 UTF-8。
- 100 个资源 JSON 全部解析通过；每个物品都有对应 `models/item/*.json`，贴图引用齐全。
- 24 个 mixin 目标类/方法已对照 1.21.1 反编译源码逐个核对过签名。
- **未启动游戏实测**（与既有约定一致：模组改动只做编译验证）。因此运行时行为（模型显示、附魔生效、
  砸击数字）还需要一次实机确认。
