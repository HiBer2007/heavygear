# Heavy Gear — NeoForge 1.21.1 移植

本目录是 [Heavy Gear](https://github.com/CeciliAqwq/heavygear)（Fabric 1.21.11 / 26.x 多版本工程）到
**NeoForge 1.21.1** 的移植工程。它是**独立 Gradle 构建**：仓库根目录的 `settings.gradle` 仍然只包含原有的
Fabric 子工程，本目录用自己的 `settings.gradle` / `build.gradle`，互不影响。

## 前置依赖：Backported Spears

1.21.1 没有矛（spear）这一物品家族，原版的 `minecraft:netherite_spear`、矛的刺击（piercing stab）、蓄力突进
（kinetic charge）都不存在。本移植因此**依赖**
[Backported Spears](https://github.com/Unknowneth/Backported-Spears)（mod id `spears`）：

- `neoforge.mods.toml` 里声明为 `required`（`versionRange = "[1.0.0,)"`）。
- `item/SpearComponents` 在 `ModifyDefaultComponentsEvent` 阶段把 `minecraft:netherite_spear` 的组件
  （`minecraft:piercing_weapon`、`kinetic_weapon`、`attack_range`、`swing_animation`、`use_effects`、
  `minimum_attack_charge`、攻击力/攻速修饰符）整组拷到重矛和圣枪上——于是两把矛拥有和 backport 原版矛
  完全一致的刺击、蓄力、攻击距离、音效和手持模型，而且 backport 以后调数值时这里会自动跟随。
- 代码层面**不链接** backport 的 Java API（本机拿不到它的 jar/Modrinth API），只按 id 读取
  `minecraft:netherite_spear` 并拷贝组件，因此没有编译期依赖，缺失时只打警告并退化成普通工具。
- 重矛/圣枪通过 `data/minecraft/tags/item/spears.json` 加入 `#minecraft:spears`，从而自动获得
  sharpness / durability / fire_aspect / lunge 附魔支持、客户端"手持 vs 图标"模型切换。
- 实机/`runClient` 时需要把 Backported Spears 的 NeoForge 1.21.1 jar 一起装进 `mods`（本机拿不到它的
  Modrinth jar，所以构建本身不下载它，只在运行时按 modId 校验依赖）。

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
表达不了的用等价的 NeoForge 事件 / 实体逻辑 / backport 模组替代"。

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
| `Item.Properties.setId()` / `.sword()` / `.pickaxe()` | 1.21.1 的 `Tiers.NETHERITE` + `SwordItem`/`AxeItem`/… |
| `Item.Properties.spear(...)`（1.21.11 原版矛） | `TieredItem` + 从 Backported Spears 的 `minecraft:netherite_spear` 拷贝矛组件（见上节） |
| `DiveStabMixin`（刺击带砸击伤害） | `SpearStabSmashMixin`：在 backport 的 `PiercingWeapon#stab` 里给 `SpearUser#pierce` 的伤害参数加砸击加成 |
| `RiptideLungeGuardMixin`（突进时屏蔽 Lunge 附魔） | 删除：1.21.1 的圣枪突进走原版三叉戟 `startAutoSpinAttack`，不经过矛的刺击/`post_piercing_attack` |
| `ArmorMaterial`（装备资源 `equipment/*.json`） | 1.21.1 `ArmorMaterial` record + `textures/models/armor/heavy_layer_{1,2}.png` |
| 物品模型 `assets/heavygear/items/*.json`（item model definition） | 删除；`models/item/*.json` 改用 `overrides`（弓/弩/钓竿的 pulling/cast/charged 状态，矛的 `spears:in_gui`） |
| 三叉戟渲染状态 mixin（`ThrownTridentRendererMixin`、`TridentRenderStateMixin`） | 改为 `LonginusTridentRenderer extends ThrownTridentRenderer`（1.21.1 走 `getTextureLocation`） |
| 重型钓竿浮标贴图 mixin（`HeavyFishingBobberMixin`） | 删除：1.21.1 的 `FishingHookRenderer` 没有渲染状态钩子；浮标仍用原版贴图 |
| `mace_smash` 伤害类型 | 1.21.1 不存在；砸击伤害挂在普通 `player_attack` / `arrow` 上，靠 `SmashAttribution` 记录归属，死亡消息混入判断 |
| `method_64193`（mace 伤害源）、`method_5997` 等中介名 | 换成本映射名：`canSmashAttack`、`doAutoAttackOnTouch`、`actualHurt` 等 |
| `hurtOrSimulate`、`fallDistance` 为 double | 1.21.1 用 `hurt(...)`，`fallDistance` 为 float |

mixin 划分：

- `heavygear.mixins.json`（服务端/通用，24 个）：`HeavySmashMixin`（任意物品在沉重药水下的砸击）、
  `MaceItemMixin`（滑翔俯冲解除砸击限制）、`MaceSmashSupportMixin`、`DiveSmashDamageMixin`、
  `DiveWindBurstMixin`、`ArrowEffectSmashMixin`、`EffectTopUpMixin` / `EffectPlayerTopUpMixin`、
  `EcamAttackMixin`、`EcamTooltipMixin`、`HeavyBobberSmashMixin`、`HeavyRodMeleeGuardMixin`、
  `HeavyDeathMessageMixin`、`RiptideContactMixin`、`RiptideContactNoBurstMixin`、`SmithingEnchantMergeMixin`、
  `ThrownTridentSmashMixin`、`VanillaItemEquivalenceMixin`、`EnchantmentCompatibilityMixin`，
  以及 4 个 accessor（箭头武器/穿透、实体 shared flag、烟花挂载实体）。
- `heavygear.client.mixins.json`：`ReleaseUsingItemMixin`（右键松开时的蓄力突进/投掷分流）。
- `heavygear.spears.mixins.json`（`required: false`，针对 Backported Spears 的类）：`SpearStabSmashMixin`。

## 数据包改动

- `data/minecraft/tags/item/spears.json`：把重矛与圣枪加入 `#minecraft:spears`（与 backport 的同名标签合并）。
- `recipe/heavy_spear.json`：底材是 `minecraft:netherite_spear`（由 backport 提供）。
- 末地城战利品表恢复 `minecraft:diamond_spear`（由 backport 提供）。
- `advancement/final_offering.json` 的 `item_used_on_block` 条件从列表改为 1.21.1 的单对象形式。
- 附魔（density / breach / wind_burst / infinity / channeling）与标签格式本身就是 1.21.1 结构，未改。
- 物品/方块/附魔/实体引用已全部与 1.21.1 注册表 + backport 提供的物品核对，所有 JSON 通过解析校验。

## 验证程度

- `gradlew build` 通过；`neoforge.mods.toml` 占位符展开正确、编码 UTF-8；依赖 `spears` 已声明。
- 资源 JSON 全部解析通过；每个物品都有对应 `models/item/*.json`，贴图引用齐全。
- 通用 mixin 的目标类/方法已对照 1.21.1 反编译源码逐个核对签名；`SpearStabSmashMixin` 的目标是
  backport 自己的 `com.notunanancyowen.spears.components.PiercingWeapon#stab`
  （方法描述符按 1.21.1 Mojang 映射写成 `pierce(EquipmentSlot, Entity, float, boolean, boolean, boolean)`）。
- **未启动游戏实测**（与既有约定一致：模组改动只做编译验证）。运行时行为（模型显示、矛的刺击/蓄力、
  砸击数字、附魔生效）还需要一次实机确认。
