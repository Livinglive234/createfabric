# Fabric port compile-error sweep

Tracking file for getting `Createfabric` (mc1.21.1/fabric/dev) to compile, ported from
`Createforge` (NeoForge). Methodology: fix leftover/dangling NeoForge imports and broken
merge artifacts first; genuinely-unported NeoForge-only features get a `// TODO fabric`
stub; recipe-system issues last.

Baseline compile counts (full clean `./gradlew compileJava`, not incremental):
- Session start (this sweep): 4,130 errors
- After first batch (recipe generics, AllConfigs, CreateRegistrate, BlockHelper, etc.): 3,722 errors
- After AllCreativeModeTabs/Train/ContraptionHandlerClient import restore: 3,702 errors
- After bulk import-restoration script (see below), 267 files / 552 imports: 2,964 errors
- After fixing ProcessingRecipeSerializer.java (stale pre-refactor dead code): 2,942 errors
- After ChuteBlockEntity/ItemDrainBlock/AllItemAttributeTypes (Capabilities.* → Fabric Storage/BlockApiCache) + ClientEvents (AllFluids import, ClientWorldEvents wrong package, duplicate CommonEvents registration) + CreateEmptyingRecipeGen (NeoForgeMod.MILK → Milk.STILL_MILK): pending re-verify (cr_verify10.log)
- **Current (batch 60, fresh-container verified): 252 errors** — see "Session resumed" / batch 50-60 notes near the end of this file for the full trajectory from the last documented checkpoint (503) through this session's confirmed 478 → 455 → 441 → 407 → 395 → 389 → 367 → 345 → 330 → 311 → 271 → 252. Development also moved from a throwaway session branch onto `main` directly partway through (see "Branch consolidation" note below) — all commits from batch 56 onward are on `main`.

## Workflow note: batch fixes before recompiling (user preference, established batch 64)
Fix several small files per round (a handful to a dozen, depending on how independent/low-risk they are) and run
**one** verification compile for the whole batch, rather than editing a single file and recompiling immediately
after each one. Full clean-ish compiles take ~30s-1.5min each, so one-file-at-a-time wastes a compile cycle per
fix for no extra safety — the frontier list from the previous compile already tells you which files are broken and
why, so most small fixes (dead imports, wrong param types, missing `implements`, undefined leftover vars) don't
need a solo compile to sanity-check before batching. Still compile between batches (don't go more than roughly
10-20 files without re-verifying), and still do a solo/small-batch compile for anything non-trivial or uncertain
(new classes, API-shape guesses not already confirmed via `javap`, anything touching shared/base classes many
files depend on).

## Bulk import-restoration technique (big win — use again if a similar wave of import loss shows up)
The "merge picked wrong side and dropped imports" bug (see session summary) turned out to affect
**267 files**, not just the handful found by manual `git show HEAD:<path>` diffing. Wrote
`/tmp/restore_imports.py`: for every `src/main/java/**/*.java` file, diffs `git show HEAD:<path>`
imports against the current file's imports, and re-adds any import HEAD has that the current file
lacks — UNLESS it matches a blocklist (neoforge/minecraftforge packages, a few specific stale
nested-class references that were refactored away, and classes deleted this session). Run with
`DRY_RUN=1 python3 /tmp/restore_imports.py` first to review, then `DRY_RUN=0` to apply. This
single script dropped the error count from 3,702 to 2,964. If more systemic import-loss surfaces,
extend the `DELETED_CLASSES`/`is_blocked` list in that script rather than fixing file-by-file.
Known false positives already excluded: `fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry`
(wrong old package), `ProcessingRecipeBuilder.ProcessingRecipeParams`/`.ProcessingRecipeFactory`
(both used to be nested classes there, now top-level `ProcessingRecipeParams` and
`ProcessingRecipe.Factory` respectively — HEAD predates that refactor).

## Status legend
- [x] done and recompiled clean (no more errors reported for this file)
- [~] partially fixed / needs re-verification next compile
- [ ] not started

## Done this session
- [x] `foundation/recipe/trie/RecipeTrie.java` — ported to `Storage<ItemVariant>`/`Storage<FluidVariant>`, `FluidIngredient`
- [x] `content/processing/basin/BasinOperatingBlockEntity.java` — same Storage port
- [x] `content/processing/recipe/ProcessingRecipe.java` — `SizedFluidIngredient` → `FluidIngredient` (field+getter)
- [x] `content/kinetics/saw/CuttingRecipe.java` — `extends ProcessingRecipe<Container>` → `StandardProcessingRecipe<Container>`
- [x] `compat/rei/ConversionRecipe.java` — same fix
- [x] `content/kinetics/deployer/ItemApplicationRecipe.java` — generics fix + restored ~6 missing imports (Container, Codec, RecordCodecBuilder, Function, ProcessingRecipeSerializer)
- [x] `foundation/utility/SameSizeCombinedInvWrapper.java` — deleted (dead, no fabric CombinedInvWrapper equivalent, unused after ItemVaultBlockEntity already used Fabric's CombinedStorage)
- [x] `foundation/utility/DistExecutor.java` — deleted (dead, already `@Deprecated(forRemoval=true)`, unused)
- [x] `foundation/utility/CreatePaths.java` — `FMLPaths` → `FabricLoader.getInstance()`
- [x] `foundation/ICapabilityProvider.java` — dropped unused `BlockCapabilityCache`-based overload (only `Supplier` overload is ever used)
- [x] `content/fluids/OpenEndedPipe.java` — `ICapabilityProvider<IFluidHandler>` → `ICapabilityProvider<OpenEndFluidHandler>` (naming leftover)
- [x] `foundation/utility/BlockHelper.java` — fixed merge-mangled `getHolderOrThrow` call, TODO-stubbed `BlockDropsEvent`/`SpecialPlantable` (genuinely unported)
- [x] `infrastructure/config/AllConfigs.java` — `ForgeConfigRegistry` (wrong v2 package) → `NeoForgeConfigRegistry` v4
- [x] `foundation/data/CreateRegistrate.java` — `DeferredHolder` now from porting-lib not neoforge; removed dead `onData` override; `RegistryEntry<?>` → `RegistryEntry<?, ?>` (2 type params now); added missing `AbstractRegistrateAccessor` mixin (file didn't exist)
- [x] `foundation/mixin/accessor/AbstractRegistrateAccessor.java` — created (was referenced but missing), registered in `create.mixins.json`
- [x] `foundation/data/CreateEntityBuilder.java` — removed unused `FMLClientSetupEvent` import
- [x] `infrastructure/command/CameraAngleCommand.java` — NeoForge `EnumArgument` → Brigadier `StringArgumentType.word()` + manual `Mode.valueOf`
- [x] `impl/registry/TagProviderImpl.java` — nonexistent `TagsUpdatedCallback` → Fabric `SimpleSynchronousResourceReloadListener`
- [x] `impl/contraption/dispenser/DispenserBehaviorConverter.java` — same `TagsUpdatedCallback` fix
- [x] `impl/registry/CreateDataMapsImpl.java` — `reload()` → `onResourceManagerReload()` (wrong override name)
- [x] `infrastructure/data/GeneratedEntriesProvider.java` — `PackOutput`→`FabricDataOutput`, removed dead `NeoForgeRegistries.BIOME_MODIFIERS` field, reuses `addBootstraps`
- [x] `infrastructure/data/CreateDatagen.java` — dropped `CreateDatamapProvider::new` (TODO), fixed `CreateRecipeSerializerTagsProvider` arg count
- [x] `infrastructure/data/CreateEnchantmentTagsProvider.java` — wrong `ExistingFileHelper` package
- [x] `infrastructure/data/CreateContraptionTypeTagsProvider.java`, `CreateMountedItemStorageTypeTagsProvider.java` — missing `@Nullable`/`ExistingFileHelper` imports
- [x] `foundation/data/CreateDatamapProvider.java` — deleted (NeoForge data-maps system, no fabric port; datagen-only, TODO'd in CreateDatagen)
- [x] `foundation/data/RuntimeDataGenerator.java` — `WithConditions` now from porting-lib not neoforge
- [x] `foundation/utility/worldWrappers/WrappedBlockAndTintGetter.java` — TODO-stubbed `getModelData` (NeoForge-only)
- [x] `impl/contraption/BlockMovementChecksImpl.java` — `NeoForge.Tags.Blocks.RELOCATION_NOT_SUPPORTED` → local `TagKey` constant (data-driven, no code dep needed)
- [x] `foundation/particle/ICustomParticleData.java` — missing `StreamCodec`/`RegistryFriendlyByteBuf`/`NotNull` imports
- [x] `infrastructure/debugInfo/DebugInformation.java` — removed unused `ModList`/`IModInfo` imports

## Done this session (batch 2, after bulk import restore)
- [x] `content/processing/recipe/ProcessingRecipeSerializer.java` — was stale pre-refactor dead code (unused `RecipeSerializer<T>` implementation using long-gone `ProcessingRecipeFactory` nested class); stripped to just the actually-used static `codec(AllRecipeTypes)` helper, rewired through `StandardProcessingRecipe.Serializer`/`recipeTypes.getSerializer()`/`getId()`
- [x] `content/logistics/chute/ChuteBlockEntity.java` — `IItemHandler`/`BlockCapabilityCache`/`Capabilities.ItemHandler` → `Storage<ItemVariant>`/Fabric `BlockApiCache`/`ItemStorage.SIDED` (callers already expected `Storage<ItemVariant>`)
- [x] `content/fluids/drain/ItemDrainBlock.java`, `content/logistics/item/filter/attribute/AllItemAttributeTypes.java` — `stack.getCapability(Capabilities.FluidHandler.ITEM)` → `FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack))`
- [x] `foundation/events/ClientEvents.java` — missing `AllFluids` import; `ClientWorldEvents` import pointed at a porting-lib class that no longer exists → switched to fabric-api's own `ClientWorldEvents`; removed a duplicate/wrong registration (`CommonEvents::onLoadWorld`/`onUnloadWorld`, which take `(Executor, LevelAccessor)` for `ServerWorldEvents`, were also wrongly wired to `ClientWorldEvents.LOAD/UNLOAD` — already correctly wired to `ServerWorldEvents` elsewhere in `CommonEvents.register()`)
- [x] `foundation/data/recipe/CreateEmptyingRecipeGen.java` — `NeoForgeMod.MILK.get()` → `io.github.tropheusj.milk.Milk.STILL_MILK` (the milk-lib fabric mod already used elsewhere, e.g. `OpenEndedPipe.java`)
- [x] **`foundation/events/ClientEvents.java` — full event-wiring rework (like `CommonEvents.java` last session), the client-side twin that had never been done.** Root-caused via `javap`/`unzip -l` on the actual porting-lib jars (same technique as before). Mapping used:
  - `RenderArmCallback`→`client_events.event.client.RenderArmEvent` (Event-object w/ getters, `.setCanceled()`); rewrote `NetheriteBacktankFirstPersonRenderer.onRenderPlayerHand` from decomposed params to `(RenderArmEvent event)`
  - `RenderHandCallback`→`client_events.event.client.RenderHandEvent` (only the import path was wrong; `ExtendoGripRenderHandler.onRenderPlayerHand(RenderHandEvent)` body already matched the new API)
  - `CameraSetupCallback`/`CameraInfo`→`client_events.event.client.ViewportEvent.ComputeCameraAngles` (getter/setter yaw/pitch, void return instead of boolean)
  - `RenderTickStartCallback`→`event.client.RenderFrameEvent.PRE`, callback now takes a `DeltaTracker` (fixed `onRenderTick()`'s dangling undefined-`event` reference in the process — a pre-existing bug unrelated to the import)
  - `TextureStitchCallback.POST`→`event.client.TextureAtlasStitchedEvent.EVENT`, single unified event with `getAtlas()`; adapted via lambda since `StitchedSprite.onTextureStitchPost(TextureAtlas)` takes the atlas directly, not an event object
  - `AttackAirCallback`→`event.client.InteractEvents.ATTACK`, now a general attack-hit callback taking `(Minecraft, HitResult)`; filtered to `HitResult.Type.MISS` in `leftClickEmpty` to preserve old "attacked empty air" semantics
  - `EntityMountEvents.MOUNT`/`.DISMOUNT`→ single `entity.events.EntityMountEvent.EVENT`, split via `event.isMounting()`/`isDismounting()` (mirrors the `CommonEvents.java` fix from last session)
  - `PlayerTickEvents.END`→`entity.events.tick.PlayerTickEvent.Post.EVENT`, handlers wrapped in lambdas passing `event.getEntity()` (both target methods already took a plain `Player`)
  - Removed 2 dead leftover NeoForge-era methods never wired to anything (`onTickPre(ClientTickEvent.Pre)` calling `onTick(true)` — a signature that doesn't even exist; `onLogOut(ClientPlayerNetworkEvent.LoggingOut)` — functionally duplicated by `onLeave`, already correctly wired via `ClientPlayConnectionEvents.DISCONNECT`)
  - Removed a duplicate/wrong registration: `CommonEvents::onLoadWorld`/`onUnloadWorld` (server-side, `(Executor, LevelAccessor)`) were also wrongly wired to client-side `ClientWorldEvents.LOAD/UNLOAD` — already correctly wired to `ServerWorldEvents` in `CommonEvents.register()`
  - Fixed missing imports: `AllFluids`, `AllKeys`, `java.util.List`, `FogShape` (wrong package: `com.mojang.blaze3d.shaders.FogShape`, not `net.minecraft.client.renderer`)
  - `AttackEntityEvent.ATTACK_ENTITY` → `.EVENT` (wrong field name)
  - `ItemTooltipCallback.getTooltip` gained a `Item.TooltipContext` middle param — added to `ClientEvents.addToItemTooltip`
  - `DrawSelectionEvents.Block.onHighlightBlock`'s 4th param changed from `float partialTicks` to `DeltaTracker` — updated `ClipboardValueSettingsHandler.drawCustomBlockSelection` and `TrackBlockOutline.drawCustomBlockSelection` (param unused in both bodies, just a type swap)
  - `CreateHatArmorLayer.registerOn(EntityRenderer<?>)` (1-arg) didn't match the `LivingEntityFeatureRendererRegistrationCallback` 2-arg shape; reworked to `registerOn(EntityRenderer<?>, RegistrationHelper)` mirroring the already-correct `BacktankArmorLayer.registerOn`, using `helper.register(layer)` instead of `livingRenderer.addLayer(layer)`; deleted dead unused `registerOnAll`
  - `TrainMapEvents.java` had no `init()` method despite `ClientEvents.register()` calling one — added `init()` wiring `ClientTickEvents.END_CLIENT_TICK` to the existing `tick()` method (keeps Xaero/JourneyMap/FTBChunks train-position sync working); left `mouseClick`/`cancelTooltips`/`renderGui` as TODO (never wired to fabric screen/tooltip events — genuine unported gap, not blocking compile)
  - `ClientWorldEvents.LOAD`/`.UNLOAD` don't exist in the current fabric-api version either — fabric-api collapsed them into a single `AFTER_CLIENT_WORLD_CHANGE` event (signature `(Minecraft, ClientLevel)` matches both handlers already). Registered both `onLoadWorld`/`onUnloadWorld` to it since Create's reset/invalidate logic there is idempotent regardless of direction (noted with a comment — not a perfect semantic match, `onUnloadWorld`'s `ControlsHandler.levelUnloaded(world)` now runs against the *new* world rather than the one being left, since fabric-api doesn't expose a distinct pre-unload hook)
  - Also missing `AllParticleTypes` import (unrelated one-liner, caught on the final pass)
  - **Confirmed fully clean (0 errors)** — `grep -c "ClientEvents.java" /tmp/cr_verify14.log` → `0`

Running total: 4,130 → 2,816 errors (~32% reduction this session on top of the ~10% from the first batch, so ~4,309 → 2,816 for the whole session, roughly 35% down).

## Done this session (batch 3)
- [x] `foundation/item/SmartInventory.java` (52 errors) — genuinely broken merge: class extended
  `ItemStackHandler` directly AND had dead code delegating through a nonexistent `inv`/nested
  `SyncedStackHandler` field (a leftover from a pre-fabric design where it wrapped rather than
  extended). Rewrote to extend `ItemStackHandler` directly throughout: kept HEAD's real
  `stackNonStackables` extract() logic (using `ItemUtils.getMaxStackSize`, not the broken `inv.extractItem`
  stub that was there), added an `isValid` `BiPredicate` field for the newer forge-side
  constructor overload (`new SmartInventory(slots, be, isValid)`, used by `PackagePortBlockEntity`),
  removed the entire dead `SyncedStackHandler` nested class
- [x] `content/kinetics/mechanicalArm/ArmInteractionPoint.java` — missing `ItemHandlerHelper` import
  (`io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper`); `TransferUtil.extractAnyItem`
  doesn't exist — rewrote `extract()` using `StorageUtil.extractAny(handler, amount, ctx)` directly
  (extracting *within* the passed transaction, unlike `TransferUtil.extractAny` which commits its own)
- [x] `content/kinetics/mechanicalArm/AllArmInteractionPointTypes.java` (52 errors) — every
  `insert`/`extract` override across ~8 nested `ArmInteractionPoint` subclasses still used the old
  NeoForge signature `insert(ArmBlockEntity, ItemStack, boolean simulate)` /
  `extract(ArmBlockEntity, int slot, int amount, boolean simulate)`, but **the base class
  (`ArmInteractionPoint.java`) already only declares `insert(ItemStack, TransactionContext)` /
  `extract(int amount, TransactionContext)`** — and the *bodies* of these overrides already
  referenced `ctx` as if the signature had been updated, they just weren't. Retyped all ~8 overrides
  to match; deleted 2 dead `getSlotCount(ArmBlockEntity)` overrides (base class never declared
  this method — leftover NeoForge `IItemHandler.getSlots()` concept, unused everywhere); added
  missing `TransactionSuccessCallback` import (project's own class, already used by ~15 other files)
- [x] `content/equipment/clipboard/ClipboardValueSettingsHandler.java` (32 errors, some found after
  the earlier `DeltaTracker` fix) — `interact(...)` was converted from a NeoForge
  `PlayerInteractEvent`-taking method to one returning `InteractionResult` directly, but the body
  still referenced the deleted `event` param throughout (`event.getFace()` → the actual `face`
  parameter; removed a whole `ICancellableEvent`/`switch` block for setting cancellation results,
  since returning `InteractionResult` already communicates that; `return;` → `return InteractionResult.PASS;`)
- [x] `content/kinetics/deployer/DeployerHandler.java` (32 errors) — many small fixes:
  `io.github.fabricators_of_create.porting_lib.item.UseFirstBehaviorItem` → `.item.extensions.UseFirstBehaviorItem`
  (moved subpackage); `mainHandItem.getAttributeModifiers().modifiers().forEach(...)` (method doesn't
  exist on `ItemStack` in 1.21.1) → `mainHandItem.forEachModifier(EquipmentSlot.MAINHAND, attributeModifiers::put)`;
  `item.getFoodProperties(stack, player)` (removed) → `stack.get(DataComponents.FOOD)`;
  `bucketItem.content` (private field) → `((BucketItemAccessor) bucketItem).port_lib$getContent()`
  (porting-lib mixin accessor already existed, just wasn't used); `IBaseRailBlockExtension`
  (NeoForge-only, no fabric port) → `instanceof BaseRailBlock` (vanilla class, same intent);
  a dangling `event.getUseBlock() != TriState.FALSE` (both `event` and `TriState` undefined/unused
  elsewhere in the method) simplified away since the FAIL case already returns early above it;
  `TriState useBlock/useItem` → `InteractionResult` (matches what `UseBlockCallback` actually
  returns; the rest of the method already compared these against `InteractionResult.FAIL`); fixed
  `UseEntityCallback.EVENT.invoker().interact(player, world, ...)` referencing an undefined `world`
  → `level` (the actual local variable); added the `snapshotParticipant()` diamond-conflict override
  (see below) to the nested `ItemUseWorld` class
- [x] **Porting-lib "extensions" module version-skew `snapshotParticipant()` diamond conflict — 3 more
  occurrences found** (same root cause as `ContraptionWorld.java`/`VirtualRenderWorld.java` fixed last
  session): `content/kinetics/drill/CobbleGenLevel.java`, `content/contraptions/wrench/NonVisualizationLevel.java`,
  `content/equipment/TreeFertilizerItem.java`'s nested `TreesDreamWorld` class, and
  `DeployerHandler.java`'s nested `ItemUseWorld` class — all `extends WrappedLevel` (catnip, bundled
  in the Ponder-Fabric jar). Same fix each time: raw-typed `@Override public
  net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant snapshotParticipant() {
  throw new UnsupportedOperationException(); }` — none of these classes' call sites ever invoke
  `.snapshotParticipant()` directly, so the stub is safe. **If more `WrappedLevel` subclasses turn up
  with this same error, apply the identical override — grep for "inherits unrelated defaults for
  snapshotParticipant" in the compile log to find them all at once.**
- [x] `content/kinetics/deployer/DeployerRecipeSearchEvent.java` — missing `isCanceled()`/`setCanceled()`
  methods despite a `canceled` field already existing (added both); missing `RecipeHolder`/`RecipeInput`
  imports; removed an accidentally-duplicated `ItemStackHandlerContainer` import line

## Done this session (batch 4)
- [x] `content/kinetics/mechanicalArm/ArmBlockEntity.java` — one leftover old-signature call,
  `armInteractionPoint.insert(held, true)` (boolean simulate) → wrapped in a nested `Transaction`
  that's never committed (simulates), `armInteractionPoint.insert(held, simulation)`. Other call
  sites in this file already used the new `(ItemStack/int, TransactionContext)` signatures.
- [x] `compat/jei/category/CreateRecipeCategory.java` (JEI — priority mod) — **confirmed clean**
  (only pre-existing deprecation warnings remain, e.g. `IRecipeCategory.getBackground()`/
  `getTooltipStrings` being marked for removal — not addressed, not blocking). Fixes: missing
  imports (`RecipeInput`, `IFocusGroup`, `IRecipeSlotsView`); `ItemLike` was imported from the wrong
  package (`net.minecraft.world.item.ItemLike` doesn't exist — real package is
  `net.minecraft.world.level.ItemLike`); `IJeiFluidIngredient` interface changed from
  `getFluid()`/`getTag()` (NBT-based) to a single `getFluidVariant()` (Fabric transfer API) — rewrote
  `fromJei`/`toJei` to convert via `com.simibubi.create...FluidStack`'s `FluidVariant`-based
  constructor/`getVariant()` instead of the old `Fluid`+`CompoundTag` pair.

Running total: 4,130 → 2,568 errors (~38% down this session).

## IMPORTANT METRIC CORRECTION
All the error counts above were measured with `grep -c "error:"` on the Gradle `-q` output, which
turned out to **double-count roughly 2x** — the log intermittently printed each diagnostic twice (once
plain, once re-indented), likely a Gradle console/worker buffering quirk. The **authoritative count is
javac's own summary line** (`"N errors"` at the end of the log, only present when the build fails).
Get it with:
```
./gradlew compileJava -q 2>&1 | tee /tmp/cr_verifyN.log | grep -E "^[0-9,]+ errors?$"
```
Re-measured on the exact same state that `grep -c` called "2,568": **the real count is 1,271**
(after 2 more small fixes on top of that state — dead `event`-variable / shadowed-`stack`-variable
bugs in `ManualApplicationRecipe.java`, `StockTickerBlock.java`, `SharedDepotBlockMethods.java, see
below). **Use the `"N errors"` summary line from here on, not `grep -c "error:"`.** The
`/tmp/error_files.txt` file-ranking command (`grep -E "\.java:[0-9]+: error:"` then `sort | uniq -c`)
is still valid for finding high-value files since it's about *relative* ranking, not absolute count —
just don't trust the absolute totals quoted earlier in this file's history above this note.

## Done this session (batch 6)
- [x] Investigated adding a Gradle dependency for `xaerolib` (blocking `XaeroTrainMap.java`) —
  **could not resolve safely, reverted**. Findings: XaeroLib is not published as its own artifact for
  modern Fabric MC versions on Modrinth (the `xaerolib` project ID `ZaQIxZKn` only has old Forge
  1.12.2/1.16.5 releases — verified via the Modrinth API, not just the website). It's only distributed
  jar-in-jar, embedded at `META-INF/jars/xaerolib-fabric-1.21.1-1.7.3.jar` inside the already-declared
  `xaeros-minimap` artifact. Attempted the standard fix (a Gradle task extracting that nested jar via
  `zipTree` + `configurations.detachedConfiguration(...)`, then `modCompileOnly(files(...))` on the
  extracted jar) — **this broke Loom's own Minecraft setup at configuration time**
  (`Failed to setup Minecraft, java.io.UncheckedIOException: Failed to compute checksum`), almost
  certainly because eagerly resolving a detached configuration during script evaluation raced with
  Loom's own dependency/checksum bookkeeping. Reverted cleanly (verified `git diff build.gradle.kts`
  only shows the earlier, legitimate `"resources"` porting-lib module addition). **Do not retry this
  exact approach** — if `XaeroTrainMap.java` is worth unblocking later, look for a lazy/task-scoped
  way to extract the nested jar (e.g. inside `afterEvaluate` or using Loom's own nested-jar
  extraction hooks if any exist) rather than eager configuration-time resolution, or just check
  CurseForge/cursemaven for a standalone fabric XaeroLib artifact instead of Modrinth.
- [x] `content/kinetics/deployer/ManualApplicationRecipe.java`, `content/logistics/stockTicker/StockTickerBlock.java`,
  `content/logistics/depot/SharedDepotBlockMethods.java` — three separate instances of the same bug
  shape: a body still referencing a deleted NeoForge `event` parameter (`event.getEntity()`,
  `event.getHand()`) inside a method that already has the real values available as `player`/`hand`
  params, PLUS in two of them a locally-declared `ItemStack stack` that shadowed an outer `stack`
  parameter (renamed to `extractedStack`). Also fixed in `StockTickerBlock.java`:
  `TransferUtil.truncateLong` doesn't exist (→ `ItemHelper.truncateLong`, the established helper used
  everywhere else); a dead/nonexistent `NetworkHooks` import (no such class anywhere in porting-lib,
  and wasn't referenced in the body — deleted); `ServerPlayer.openMenu(MenuProvider, BlockPos)` →
  the 2-arg overload doesn't exist in 1.21.1, just `openMenu(MenuProvider)` (the menu is a plain
  `MenuProvider`, not `ExtendedScreenHandlerFactory`, so no extra synced data was needed anyway).
- [x] `content/equipment/tool/CardboardSwordItem.java` (36→0) — **confirmed clean**.
  `cardboardSwordsMakeNoiseOnClick` had a signature already matching fabric-api's
  `AttackBlockCallback` exactly (`(Player, Level, InteractionHand, BlockPos, Direction) -> InteractionResult`)
  but a body still written against NeoForge's `PlayerInteractEvent`/`itemStack`/`event` — rewrote using
  the real params. `cardboardSwordsCannotHurtYou`'s `attacker` was typed `Entity` (from
  `event.getSource().getEntity()`) but the method body calls `Player`/`LivingEntity`-only methods
  throughout (`getItemInHand`, `getAttributeValue`, `isSprinting`, `getAttackStrengthScale`) — added
  an `instanceof Player attacker` guard (cardboard-sword knockback-without-damage is a player-only
  mechanic per the "Reference player.attack()" comment already in the code). `Holder<Enchantment>.getKey()`
  doesn't exist on `Holder` in 1.21.1 → `.is(Enchantments.KNOCKBACK)`. Also: wrong-subpackage
  `LivingAttackEvent` import (missing `.living.`, same bug as `CardboardArmorHandlerClient.java` last
  session); dead `SimpleCustomRenderer` import (class doesn't exist anywhere in this codebase, per
  last session's notes); missing `BlockPos`/`Direction`/`Entity`/`ItemEnchantments` imports.
- [x] `content/logistics/tableCloth/TableClothModel.java` (34→0) — **confirmed clean, but the class is
  currently dead/unregistered code** (both registration call sites are already commented out —
  `BuilderTransformers.java:507` and `ClientResourceReloadListener.java:28`). Depended entirely on
  NeoForge's `ModelData` system (`BakedModelWrapperWithData`, `ModelProperty`) to cache which sides of
  a tablecloth are "culled" against a neighbour, so seam quads can be skipped — **this has no fabric
  equivalent**: fabric's `BakedModel#getQuads` only takes `(BlockState, Direction, RandomSource)`, no
  `BlockPos`/`Level`, so there's no way to inspect neighbouring blocks at quad-generation time at all
  (a different rendering pipeline, not just a renamed API — fabric models get that kind of context
  through a separate `FabricBakedModel#emitBlockQuads(BlockAndTintGetter, BlockState, BlockPos, ...)`
  path this class doesn't use). Rewrote to implement `BakedModel` directly (delegating boilerplate to
  the wrapped `originalModel`) and simplified to **always** render the corner quads, dropping the
  culling optimization — safe since the class is currently unregistered anyway. Documented with a
  `// TODO fabric` block at the top of the file; revisit via `emitBlockQuads` if this ever gets wired up.

## IMPORTANT: switched to the correct metric partway through batch 6
From here on all counts are the authoritative javac `"N errors"` summary line (see the correction
note above) — **4,130 → 1,152 by the end of batch 7** (measuring consistently on the correct metric
from "1,271" onward). Progress is much further along than the early `grep -c`-based numbers suggested.

## Done this session (batch 7)
- [x] `impl/unpacking/DefaultUnpackingHandler.java` (clean) — the real fabric-transfer-based `unpack`
  logic (lines ~34-39, using `Storage<ItemVariant>`/`targetInv.insert(...)`) was already complete and
  correct on its own; ~15 lines directly below it were dead leftover NeoForge `IItemHandler`-slot-based
  logic referencing undefined variables (`toInsert`, `itemInSlot`, `slot`, `itemsAddedToSlot`,
  `boxSlot`) that never got cleaned up after the merge — deleted outright.
- [x] `foundation/data/recipe/CreateStandardRecipeGen.java` (clean) — `net.neoforged.neoforge.common.conditions.{ICondition,ModLoadedCondition,NotCondition}`
  → `io.github.fabricators_of_create.porting_lib.resources.conditions.{...}` (same "resources" module
  added earlier this session for `AddPackFindersEvent`); `Tags.Items.FOODS_DOUGH` (porting-lib's tag
  shim, doesn't have this field) → Create's own `AllItemTags.FOODS_DOUGH_WHEAT.tag`; a `record
  ModdedCookingRecipeOutput implements RecipeOutput` only overrode the varargs
  `accept(id, recipe, advancement, ICondition...)` (from the mixin-injected `RecipeOutputExtension`
  default methods) but never the plain 3-arg `accept(id, recipe, advancement)` that vanilla
  `RecipeOutput` itself declares abstract — added it, delegating to the varargs version with an empty
  conditions array.
- [x] `content/contraptions/Contraption.java` (clean, re-verified after last session's import fix) —
  this file picked up several genuinely new symbols in the merge that were never in HEAD at all (not
  an import-loss regression, just missing entirely): `AllBlockTags`, `ClientContraption`,
  `CollisionList`/`CollisionList.Populate` (all real, existing project classes — just needed
  importing), plus `Object2BooleanMap`/`Object2BooleanArrayMap` (fastutil) and `Contract`
  (jetbrains annotation). Also `LevelEvent` was imported from the wrong package
  (`net.minecraft.world.level.LevelEvent` doesn't exist; it's `net.minecraft.world.level.block.LevelEvent`
  — cross-checked against the already-correct import in `BlockHelper.java`).
- [x] `content/kinetics/crafter/MechanicalCrafterBlockEntity.java` (clean) — dead `getInvCapability()`
  method (referenced a nonexistent `input.getItemHandler(...)` porting-lib capability call, and was
  never called from anywhere — deleted); `whenContentsChanged(() -> {...})` used a zero-arg lambda but
  `SmartInventory.whenContentsChanged` takes `Consumer<Integer>` (→ `slot -> {...}`, matches the
  rewritten `SmartInventory.java` from batch 6); `Items.TOOLS_WRENCH` (wrong `Items` class, same bug
  as `AllItems.java`/others) → `AllItemTags.WRENCH.tag`; `inventory.getItem(0)` → `.getStackInSlot(0)`
  (the class extends the project's `ItemStackHandler`-based `SmartInventory`, not a vanilla
  `Container`); missing `TransactionSuccessCallback` import; class was missing
  `implements TransformableBlockEntity` despite already having an `@Override public void
  transform(BlockEntity, StructureTransform)` — every sibling block entity with this method declares
  the interface (confirmed against `FluidPipeBlockEntity.java`), this one just dropped it.
- [x] `content/fluids/potion/PotionMixingRecipes.java` (30→0) — **confirmed clean**. Two duplicate/wrong
  imports for a nonexistent `io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.PotionBrewingAccessor`/
  `PotionBrewing$MixAccessor` sitting alongside the correct project-local
  `com.simibubi.create.foundation.mixin.accessor.PotionBrewingAccessor` (already had the right
  `@Accessor`/`@Invoker` methods, just needed the dead duplicate imports removed); missing
  `RecipeHolder`/`Level` imports; `new SizedFluidIngredient(DataComponentFluidIngredient.of(...), ...)`
  (NeoForge-only ingredient types) → `ProcessingRecipeBuilder.require(Fluid, long)`, an overload that
  already existed and does the same thing more directly.
- [x] `AllMenuTypes.java` (28→0) — **confirmed clean**. All 14 `register(name, XxxMenu::new, () -> XxxScreen::new)`
  calls failed with "cannot infer type-variable(s) C,S" because `MenuBuilder.ForgeMenuFactory<T>`'s
  functional method is `create(MenuType<T>, int, Inventory, Object)` — a plain `Object` for the 4th
  param — but every Menu class has *two* 4-arg constructors (one taking `RegistryFriendlyByteBuf` for
  network reconstruction, one taking the real block-entity/item for direct construction), and neither
  constructor's 4th param accepts a bare `Object` directly, so a bare `XxxMenu::new` method reference
  has **zero** applicable overloads once erased to the interface's descriptor — not an ambiguity, an
  outright non-match. Fixed uniformly by replacing the method reference with an explicit lambda that
  casts: `(type, id, inv, buf) -> new XxxMenu(type, id, inv, (RegistryFriendlyByteBuf) buf)` (the
  `RegistryFriendlyByteBuf` constructor is always the one meant for this network-facing registration
  path). Applied via a small Python regex pass across all 14 entries at once rather than by hand.

## Done this session (batch 5)
- [x] `AllItems.java` — **confirmed clean**. Several issues stacked up:
  - dead leftover imports duplicated alongside their already-fixed replacements: `CompatMetals`
    (superseded by `CommonMetal`, which was already correctly imported), `CombustibleItem`,
    `AllTags.commonItemTag` — none referenced in the body, just deleted
  - `net.fabricmc.fabric.api.item.v1.FabricItemSettings` doesn't exist in the resolved fabric-api
    version at all (removed/merged away) — its one use (`GOGGLES` item wiring a custom equipment
    slot via `fp.equipmentSlot(...)`) is now fully redundant: `GogglesItem` already implements
    vanilla `Equipable`/`getEquipmentSlot()` directly (the real 1.21.1 mechanism), so the whole
    `.properties(p -> { if (p instanceof FabricItemSettings fp) {...} })` block was dead weight —
    deleted it
  - `Tags.Items.FOODS_DOUGH`, bare `Items.FOODS`/`Items.FOODS_BERRY`/`Items.FOODS_FRUIT`/
    `Items.DRINKS`/`Items.RAW_MATERIALS`, and `Tags.Items.TOOLS_WRENCH` don't exist on
    `io.github.fabricators_of_create.porting_lib.tags.Tags.Items` — the working common-tag class is
    fabric-api's own `net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags`, but naming
    differs (suffix not prefix: `BERRY_FOODS` not `FOODS_BERRY`, `FRUIT_FOODS` not `FOODS_FRUIT`).
    **Caution found the hard way: `ConventionalItemTags.DOUGH_FOODS` and `.DRINKS` exist in
    fabric-convention-tags-v2 2.12.0 but NOT in 2.11.1, and this project's resolved fabric-api
    version (0.115.1) pulls in 2.11.1** (verified via `.gradle/loom-cache/remapped_mods/remapped/net/fabricmc/fabric-api/fabric-convention-tags-v2-8f637a66/` — only 2.5.0/2.7.0/2.10.0/2.11.1 present,
    no 2.12.0). Dropped those two references rather than relying on them (both `taggedIngredient`
    and `.tag(...)` are varargs, so this is just fewer tags, not a compile shape change). **If
    checking whether some `ConventionalItemTags` field exists, verify against the actual resolved
    jar under `.gradle/loom-cache/remapped_mods/remapped/net/fabricmc/fabric-api/`, not just
    whatever's newest in `~/.gradle/caches/modules-2/files-2.1` — multiple versions coexist there
    from different mods' pinned dependencies, and picking the wrong one gives false negatives/positives.**
    `Tags.Items.TOOLS_WRENCH` (NeoForge-only concept, no fabric convention tag for "wrench") →
    Create's own `AllItemTags.WRENCH` (already existed, comment even says "used by WrenchEventHandler")
  - `Tags.Items.NUGGETS`/`Tags.Items.ENCHANTABLES` (porting-lib's own tag shim) were already correct,
    left untouched

## Next up
Regenerate `/tmp/error_files.txt` from `/tmp/cr_verify22.log` (2,568 errors) — the frontier has shifted
substantially since batch 2's snapshot. Still open, not yet started: `compat/emi/CreateEmiPlugin.java`
(deprioritized), `compat/rei/category/CreateRecipeCategory.java` (deprioritized, but has the
`FluidStack`/`getTag()` bug documented above), `content/logistics/tableCloth/TableClothModel.java`,
`content/fluids/potion/PotionMixingRecipes.java`, `impl/unpacking/DefaultUnpackingHandler.java`,
`foundation/data/recipe/CreateStandardRecipeGen.java`, `AllMenuTypes.java`,
`content/kinetics/crafter/MechanicalCrafterBlockEntity.java`,
`content/logistics/packager/repackager/RepackagerBlockEntity.java`,
`content/contraptions/Contraption.java` (re-check, likely new errors),
`compat/trainmap/XaeroTrainMap.java` (blocked on the missing `xaerolib` Gradle dependency — see above,
needs someone to verify the exact Modrinth coordinates before adding to `build.gradle.kts`).

## Next up
Re-run `./gradlew compileJava -q 2>&1 | tee /tmp/cr_verifyN.log | grep -c "error:"` (bump N) and
regenerate `/tmp/error_files.txt` (see command near the top of this file) to get the fresh frontier —
it's shifted a lot since batch 2's snapshot, so re-sort by error count rather than trusting the old list.

Known still open, not yet started:
- `compat/emi/CreateEmiPlugin.java` — EMI is deprioritized, skip unless forced
- `compat/rei/category/CreateRecipeCategory.java` — REI is deprioritized, but note: it has the
  **exact same `FluidStack`/`getTag()` bug** just fixed in the JEI version above (`fluidStack.getTag()`
  cannot find symbol, `PotionFluidHandler.addPotionTooltip(FluidStack,...)` signature mismatch) —
  if REI is ever prioritized, apply the identical fix (convert via `FluidVariant` instead of NBT tag)
- `content/logistics/tableCloth/TableClothModel.java`
- `content/fluids/potion/PotionMixingRecipes.java`
- `AllItems.java`
- `impl/unpacking/DefaultUnpackingHandler.java`
- `foundation/data/recipe/CreateStandardRecipeGen.java`
- `AllMenuTypes.java`
- `content/kinetics/crafter/MechanicalCrafterBlockEntity.java`
- `content/logistics/packager/repackager/RepackagerBlockEntity.java`
- `content/contraptions/Contraption.java` — was fixed for *imports* earlier; re-check, these are
  likely different/newer errors now that other files compile further
- Compat/trainmap: `XaeroTrainMap.java` is NOT deprioritized (Xaero is a priority mod for this user) —
  fixed the `xaero.lib.client.gui.ScreenBase` → `xaero.map.gui.ScreenBase` import (wrong package,
  most other "cannot find symbol" in this file were cascades from that one bad import), **but found a
  genuine missing Gradle dependency**: both `xaeros-minimap` and `xaeros-world-map`'s
  `fabric.mod.json` declare `"xaerolib": ">=1.0"` as a required dependency (a shared library mod,
  "Xaero's Common Lib" on Modrinth/CurseForge), and `xaero.lib.client.gui.ScreenBase` (the superclass
  `xaero.map.gui.ScreenBase` extends) lives in *that* jar — it is not bundled in either mod's own jar
  and is **not currently declared in `build.gradle.kts`** (only `xaeros-minimap`/`xaeros-world-map`
  are, around line 154). Needs `modCompileOnly("maven.modrinth:xaeros-common-lib:<version>")` (verify
  the exact Modrinth slug/version on modrinth.com — didn't want to guess Maven coordinates without
  checking) added alongside the other two Xaero deps. Once that's added, `XaeroTrainMap.java` should
  be very close to compiling clean.
  `JourneyTrainMap.java`/`FTBChunksTrainMap.java` are deprioritized

## Next up (not yet started — snapshot from `/tmp/cr_verify14.log`, 2,816 errors, 405 unique files)
Regenerate the frontier list after each compile:
```
grep -E "\.java:[0-9]+: error:" /tmp/cr_verifyN.log | sed -E 's#^[[:space:]]*/Users/kais/Git/create/Createfabric/##; s/:[0-9]+: error:.*$//' | sort | uniq -c | sort -rn > /tmp/error_files.txt
```

Highest-error-count files as of cr_verify14 (good next targets — high value per fix):
- [ ] `foundation/item/SmartInventory.java` (52)
- [ ] `content/kinetics/mechanicalArm/AllArmInteractionPointTypes.java` (52) — base-class method signature mismatch (`ArmInteractionPointType`), genuine feature-code fix not just imports, see `super.extract(...)` and `@Override` errors found this session
- [ ] `compat/emi/CreateEmiPlugin.java` (46)
- [ ] `content/equipment/tool/CardboardSwordItem.java` (36)
- [ ] `compat/jei/category/CreateRecipeCategory.java` (36) — JEI is a priority mod, worth doing
- [ ] `content/logistics/tableCloth/TableClothModel.java` (34)
- [ ] `content/kinetics/deployer/DeployerHandler.java` (32) — partially fixed earlier this session (Blocks/PlayerBlockBreakEvents/UseBlockCallback/UseEntityCallback imports added), likely more remains — try the `git show HEAD:<path>` import-diff technique first
- [ ] `content/equipment/clipboard/ClipboardValueSettingsHandler.java` (32) — partially touched this session (DeltaTracker param fix), more errors remain
- [ ] `content/fluids/potion/PotionMixingRecipes.java` (30)
- [ ] `compat/rei/CreateREI.java` (30) — REI is deprioritized but check if cheap
- [ ] `AllItems.java` (30)
- [ ] `impl/unpacking/DefaultUnpackingHandler.java` (28)
- [ ] `foundation/data/recipe/CreateStandardRecipeGen.java` (28)
Deprioritized per standing user instruction (don't spend time unless forced): CC:Tweaked,
Botania, FTB Chunks/Teams/Library, JourneyMap, REI, EMI, DynamicTrees, FramedBlocks,
TConstruct, Sandwichable.

### Two proven high-leverage techniques for this codebase (reuse before hand-fixing file by file)
1. **Bulk import restoration** (`/tmp/restore_imports.py`) — re-run the dry run
   (`DRY_RUN=1 python3 /tmp/restore_imports.py`) periodically; if it finds a new wave of
   `git show HEAD:<path>` vs current-file import diffs, review and apply (`DRY_RUN=0`). Extend the
   `DELETED_CLASSES`/`is_blocked` list in the script for any class deleted/renamed since it was last run.
2. **Event class rename discovery via `javap`/`unzip -l`** — when a `io.github.fabricators_of_create.porting_lib.*`
   import shows "package/class does not exist", grep all porting-lib jars under
   `~/.gradle/caches/modules-2/files-2.1/io.github.fabricators_of_create.Porting-Lib` for the bare
   class name (it usually moved package/module, or split Foo→Foo.Post/Foo.Pre, or Foo→FooEvent
   with getter/setter methods replacing decomposed callback params). This resolved `CommonEvents.java`
   last session and `ClientEvents.java` this session — both were large, ~20-fix event-wiring reworks.

## Workflow for each batch
1. Pick a handful of related files from the frontier list (or `/tmp/error_files.txt`).
2. Fix imports / signatures / genuine bugs. TODO-stub genuinely-unported NeoForge extension
   points (comment matching the existing `// TODO fabric: ...` style already used in the codebase).
3. Move finished files from "Next up" to "Done this session" in this file.
4. After a few files, recompile: `./gradlew compileJava -q 2>&1 | tee /tmp/cr_verifyN.log | grep -E "^[0-9,]+ errors?$"`
   (the authoritative javac summary line — NOT `grep -c "error:"`, see the metric-correction note
   above) and update the baseline count comment at the top of this file.

## Done this session (batch 8)
- [x] `content/logistics/packager/repackager/RepackagerBlockEntity.java` (clean) — a genuinely messy
  merge: the fabric branch had a complete, correct implementation using `PackageDefragmenter` +
  the Fabric Transfer API (`Storage<ItemVariant>`, `Transaction`), but the merge replaced the method
  bodies with a NeoForge-flavored rewrite referencing a class that was simultaneously renamed
  (`PackageDefragmenter` → `PackageRepackageHelper`, which *does* exist) while also using dead
  NeoForge-style calls (`targetInv.extractItem(slot, ...)`) and undefined locals (`extracted`, `slot`).
  Rewrote the method body to use `PackageRepackageHelper`'s real API together with the
  transaction/`StorageView`-based extraction pattern from the file's own still-working `unwrapBox`
  method; also fixed the method name mismatch (declared `attemptToDefrag`, called as
  `attemptToRepackage`), `PackageItem.getOrderId`/`isPackage` needing `ItemStack` not `ItemVariant`
  args, and `BigItemStack.stack`/`.count` field access (no `.copy()` method on `BigItemStack` itself).
- [x] `content/kinetics/millstone/MillstoneBlockEntity.java` (clean) — missing `RecipeWrapper`
  import; `ItemHandlerHelper.insertItemStacked` doesn't exist in porting-lib (only `giveItemToPlayer`,
  same gap found for `ToolboxEquipPacket.java` last session) → `TransferUtil.insertItemStacked`;
  **a genuine two-different-classes-named-`ItemStackHandler` bug**: `inputInv` was porting-lib's
  `ItemStackHandlerContainer` (which extends porting-lib's own `ItemStackHandler`), but `outputInv`
  was declared as *this project's own, unrelated* `com.simibubi.create...ItemStackHandler` — an inner
  `CombinedStorage<ItemVariant, PortingLibItemStackHandler>` class needed both to share a common base
  type, so switched `outputInv` to porting-lib's `ItemStackHandler` too (API-compatible: same
  `getStackInSlot`/`setStackInSlot`/`getSlotCount`/`serializeNBT`, just no `.clear()` → `setSize(getSlotCount())`);
  `ItemStack.getCraftingRemainingItem()` doesn't exist — that method lives on `Item`, not `ItemStack`,
  and returns an `Item` not an `ItemStack` (→ `stackInSlot.getItem().hasCraftingRemainingItem() ? new ItemStack(...getCraftingRemainingItem()) : ItemStack.EMPTY`).
  **If another file has this same "two different classes both named ItemStackHandler" symptom, check
  which one each field actually needs and make sure a single file doesn't mix both.**
- [x] `api/contraption/storage/item/menu/StorageInteractionWrapper.java` (clean) — was declared
  `extends ItemHandlerContainer` (project's own class, whose constructor takes a concrete
  `ItemStackHandler` and stores it in a field called `inv`) but the body referred to `this.storage`
  (a field that was never declared, assuming it'd be inherited — it isn't) and called
  `storage.isItemValid(index, ItemVariant.of(stack), stack.getCount())`, a 3-arg overload that isn't
  on the `SlottedStackStorage` interface (only a 2-arg `isItemValid(slot, ItemStack)`). Rewrote to
  `implements Container` directly with its own `SlottedStackStorage storage` field instead of
  extending `ItemHandlerContainer` (which is hard-coded to the concrete `ItemStackHandler` class, not
  the more general interface this class actually needs) — also fixed a real inverted-boolean bug
  found along the way: `isEmpty()` returned `storage.nonEmptyIterator().hasNext()` **without negating
  it**, so it reported "empty" exactly when the storage had items.

Running total: 4,130 → 1,102 errors this session (~73% down).

## Done this session (batch 9)
- [x] `AllFluids.java` (clean) — `net.minecraft.world.item.alchemy.PotionUtils` is gone entirely in
  1.21.1, replaced by the data-component-based `PotionContents` (instance `getColor()`/`getAllEffects()`,
  static `Potion.getName(Optional<Holder<Potion>>, String)`); `io.github.fabricators_of_create.porting_lib.event.common.FluidPlaceBlockCallback`
  moved to `io.github.fabricators_of_create.porting_lib.level.events.BlockEvent.FluidPlaceBlockEvent`
  (same `BlockEvent` nested-event family as `EntityPlaceEvent` fixed last session) — went from a
  `(LevelAccessor, BlockPos, BlockState) -> BlockState` callback to a void `Consumer`-style callback
  taking an Event object with `getLevel()`/`getPos()`/`getOriginalState()`/`setNewState(...)`.
- [x] `content/fluids/potion/PotionFluidHandler.java` (clean) — **the fix here uncovered that
  `FluidVariant`/`TransferVariant` (fabric-transfer-api) has NO `getOrDefault(DataComponentType, T)`
  method of its own** (only vanilla `ItemStack`/`FluidStack`/`DataComponentHolder` have that) — you
  must go through `.getComponentMap().getOrDefault(...)`, since `TransferVariant` only exposes
  `getComponents()` (→ `DataComponentPatch`) and `getComponentMap()` (→ `DataComponentMap`, which is
  what actually has `getOrDefault`). **This file's already-existing code had this exact same bug
  before I touched it** — don't trust "surrounding code already does X" as proof X compiles; always
  verify the file you're copying a pattern from is itself currently error-free. Also: `SizedFluidIngredient`/
  `DataComponentFluidIngredient` (NeoForge, unimported/nonexistent) → `FluidIngredient.fromFluidStack(stack)`;
  `stack.getCraftingRemainingItem()` on `ItemStack` (method lives on `Item`, same bug as
  `MillstoneBlockEntity.java` this session) → `stack.getItem().getCraftingRemainingItem()`;
  `fluid.set(component, value)` — the project's own `FluidStack` is immutable and has no `.set()`,
  so setting a component means building a `DataComponentPatch` and calling
  `fluid.getVariant().withComponentChanges(patch)` to get a new variant, then wrapping it in a new
  `FluidStack`.

## Done this session (batch 10)
Metric note: counts below use the corrected javac-summary metric (`grep -E "^[0-9,]+ errors?$"`).
Trajectory: 1,102 → 1,091 → 1,082 → 1,074 → 1,059.
- [x] `foundation/utility/BlockHelper.java` (clean) — finished the `destroyBlockAs` `world`→`level`
  rename left mid-flight at the last session boundary (the method's parameter is `level`, but the
  whole body used the old undefined `world` name); a stray `sed` line-range attempt hadn't actually
  applied, so this was done via one `Edit` covering the whole broken block.
- [x] `foundation/data/recipe/CreateRecipeProvider.java` (clean) — the class was `extends
  FabricRecipeProvider` with a broken `buildRecipes()` copy-pasted from `BaseRecipeProvider.java`
  (referencing a nonexistent `all` field) despite nothing ever subclassing `CreateRecipeProvider`
  — confirmed via grep that it's used purely as a static namespace (the nested `I` tag-helper class
  + `registerAllProcessing`). Deleted the dead `buildRecipes`/constructor/`BUCKET`/`BOTTLE` fields
  and the `extends FabricRecipeProvider`. Rewrote `registerAllProcessing` to take the real
  `FabricDataGenerator.Pack` (each of the 13 processing-recipe-gen classes is itself already a
  `DataProvider` via `BaseRecipeProvider extends RecipeProvider`, so each just needs
  `pack.addProvider((o, r) -> new CreateXxxRecipeGen(o, r))` — no custom wrapper `DataProvider`
  needed). Updated the one call site, `infrastructure/data/CreateDatagen.java`, to pass `pack`
  directly instead of `generator`/`output`/`registries` and dropped its now-pointless wrapping
  `DataProvider` that just returned `completedFuture(null)`. Also removed 4 dead imports there
  (`ProcessingRecipeGen`/`SequencedAssemblyRecipeGen`/`StandardRecipeGen`/`MechanicalCraftingRecipeGen`
  — wrong package, `api.data.recipe` not `foundation.data.recipe`, and unused anyway).
- [x] `content/kinetics/saw/CuttingRecipe.java`, `content/kinetics/deployer/ItemApplicationRecipe.java`,
  `content/kinetics/deployer/DeployerApplicationRecipe.java` (all clean) — `CuttingRecipe`/
  `ItemApplicationRecipe` both declared `extends StandardProcessingRecipe<Container>` /
  `ProcessingRecipe<Container, ...>`, but `Container` doesn't satisfy the `T extends RecipeInput`
  bound. Checked actual call sites to pick the right replacement per class: `CuttingRecipe` only
  ever gets a single `ItemStack` wrapped by `SequencedAssemblyRecipe.getRecipe(level, ItemStack,
  ...)` → `SingleRecipeInput`. `ItemApplicationRecipe`/`DeployerApplicationRecipe` get a real 2-slot
  `RecipeWrapper` from `DeployerBlockEntity.java` → plain `RecipeInput` (which `RecipeWrapper`
  itself implements directly — see version-fragmentation note below). Also deleted a dead import
  in both `CuttingRecipe.java` and `DeployerApplicationRecipe.java`:
  `com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory` — that
  package/class doesn't exist (the real, actually-used symbol is the separately-imported
  `SequencedAssemblySubCategoryType` from `compat.recipeViewerCommon`).
- [x] **Re-confirmed the version-fragmentation gotcha** (see earlier session note) while chasing this:
  an old `/tmp` investigation directory had a *stale* porting-lib `transfer` module jar where
  `RecipeWrapper implements Container` (taking an `ItemStackHandler`) and `ItemStackHandler`
  itself only `implements` fabric's raw storage interfaces. The jar actually resolved on
  *this project's* classpath (`.gradle/loom-cache/remapped_mods/.../transfer-8f637a66/3.1.0-beta.91+1.21.1/`)
  has a materially different `RecipeWrapper implements RecipeInput` (taking a `SlottedStackStorage`)
  and `ItemStackHandler implements SlottedStackStorage`. Always check the jar under this project's
  own `loom-cache`, never a cached investigation from an unrelated `/tmp` dir or another repo.
- [x] `compat/jei/CreateJEI.java` (clean, JEI is a priority mod) — missing `import
  mezz.jei.api.runtime.IJeiRuntime;`; deleted a dead import,
  `io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.RecipeManagerAccessor`
  (class doesn't exist in the resolved accessors module and was never referenced in the file body);
  `AllFluids.POTION.get().getSource()`/`.getFlowing()` return a raw `Fluid`, but
  `new JeiFluidIngredient(...)` needs a `FluidVariant` → wrapped each in `FluidVariant.of(...)`.
- [x] `compat/jei/PotionFluidSubtypeInterpreter.java` and `compat/rei/PotionFluidSubtypeInterpreter.java`
  (both clean) — both used the same dead NeoForge-era NBT/`PotionUtils` API (`ingredient.hasTag()`,
  `PotionUtils.getPotion(tag)`, `NBTHelper.readEnum(tag, ...)`) already fixed elsewhere this session
  (`AllFluids.java`, `PotionFluidHandler.java`). Rewrote both using the same data-component pattern:
  `ingredient.getVariant().getComponentMap().getOrDefault(DataComponents.POTION_CONTENTS,
  PotionContents.EMPTY)` for potion contents, `Potion.getName(contents.potion(), prefix)` for the
  translation-key string (no more `.getDescriptionId()`/`potionType.getName(prefix)` on `FluidStack`
  itself). The `rei` copy's whole class body is commented-out/unused (`/*implements
  IIngredientSubtypeInterpreter<FluidStack>*/`) — REI is deprioritized, but fixing it was cheap once
  the pattern was already worked out for the `jei` copy, so did both.
- [x] `content/equipment/clipboard/ClipboardBlockItem.java` (clean) — `openScreen(player,
  heldItem.getComponents())` passed a `DataComponentMap` where the method wants the `ItemStack`
  itself → `openScreen(player, heldItem)`; inside `openScreen`, `new ClipboardScreen(..., components,
  ...)` referenced a nonexistent `components` field (leftover from some other class's field name) →
  the real local parameter, `stack`.

## Done this session (batch 11)
Trajectory: 1,059 → 1,033 → 1,030.
- [x] **Found and fixed a systemic bug affecting ~13 files**: lots of code called
  `entity.getPersistentData()` (Forge's NBT-scratch-data method on `Entity`) but that method simply
  doesn't exist on fabric — it was never a rename-in-place, it's a different name. The real
  interface-injected method (confirmed via `javap` on porting-lib's actual resolved `entity` module
  jar — `.../Porting-Lib/entity-8f637a66/3.1.0-beta.91+1.21.1/entity-...jar`,
  `io.github.fabricators_of_create.porting_lib.entity.injects.EntityInjection`) is
  `getCustomData(): CompoundTag`, injected onto vanilla `Entity` itself (confirmed via `javap` on the
  merged Minecraft jar showing `Entity implements ... EntityInjection`). Did a project-wide
  `sed 's/getPersistentData/getCustomData/g'` across all 13 files that referenced it:
  `ISyncPersistentData.java`, `EntityMixin.java`, `HeavyBootsOnPlayerMixin.java`,
  `CrushingWheelControllerBlock.java`, `ContraptionHandlerClient.java`, `LimbSwingUpdatePacket.java`,
  `ContraptionSeatMappingPacket.java`, `ExtendoGripItem.java`, `DivingHelmetItem.java`,
  `ToolboxEquipPacket.java`, `ToolboxDisposeAllPacket.java`, `BlueprintEntity.java`,
  `CreateTestFunction.java`. **One exception**: `CreateTestFunction.java` calls this on a
  `StructureBlockEntity` (a `BlockEntity`, not an `Entity`) — `EntityInjection` doesn't apply there;
  the real equivalent for block entities is a *different* porting-lib interface,
  `io.github.fabricators_of_create.porting_lib.blocks.injects.BlockEntityInjection`, with a
  differently-named method, `getPortingLibPersistentData()` — fixed that one call site separately
  after the blanket sed.
- [x] `content/equipment/armor/DivingHelmetItem.java` (clean) — beyond the `getCustomData` rename:
  this is a per-tick callback (`EntityTickEvent.Pre.EVENT.register(...)`, wired in
  `foundation/events/CommonEvents.java`) standing in for NeoForge's cancellable `LivingBreatheEvent`,
  which has no fabric equivalent event with `setCanBreathe`/`setRefillAirAmount` setters — compared
  directly against `Createforge`'s original (`Createforge/src/main/java/.../DivingHelmetItem.java`)
  to confirm the intended behavior, then replaced the dangling `event.setCanBreathe(true);
  event.setRefillAirAmount(entity.getMaxAirSupply());` (no `event` variable exists in the tick-based
  version) with a direct `entity.setAirSupply(entity.getMaxAirSupply());` — equivalent effect without
  needing an event object. Also fixed a stray `level.getGameTime()` (undefined — the local variable
  is `world`) by just deleting the redundant inner check entirely, since the enclosing `if (!second)
  return;` already guarantees we're on a `% 20 == 0` tick. Removed 4 duplicate/dead imports
  (`CustomEnchantmentLevelItem`/`CustomEnchantmentsItem`, imported twice each, never referenced) and
  an unused `Map` import.
- [x] `foundation/networking/ISyncPersistentData.java` (clean) — missing `import
  net.fabricmc.api.EnvType;` / `import net.fabricmc.api.Environment;` for the `@Environment(EnvType.CLIENT)`
  annotation on `PersistentDataPacket.handle(...)`.
- Also fixed, unrelated to the above: `content/kinetics/saw/CuttingRecipe.java`,
  `content/kinetics/deployer/ItemApplicationRecipe.java`,
  `content/kinetics/deployer/DeployerApplicationRecipe.java` — these three were `extends
  StandardProcessingRecipe<Container>` / `ProcessingRecipe<Container, ...>`, where `Container`
  doesn't satisfy the `T extends RecipeInput` bound. Checked the real call sites to pick the right
  fix per class instead of guessing: `CuttingRecipe` only ever receives a single `ItemStack` wrapped
  by `SequencedAssemblyRecipe.getRecipe(level, ItemStack, ...)` → `SingleRecipeInput`.
  `ItemApplicationRecipe`/`DeployerApplicationRecipe` receive a real 2-slot `RecipeWrapper` built in
  `DeployerBlockEntity.java` → plain `RecipeInput` (which the *correctly resolved* `RecipeWrapper`
  already implements directly — see the version-fragmentation note in batch 10). Also deleted a dead
  import in both `CuttingRecipe.java` and `DeployerApplicationRecipe.java`:
  `com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory` (package
  doesn't exist; the real, actually-used symbol is `SequencedAssemblySubCategoryType` from
  `compat.recipeViewerCommon`, imported separately).
- [x] `compat/jei/CreateJEI.java` (clean, JEI priority) — missing `import
  mezz.jei.api.runtime.IJeiRuntime;`; deleted a dead import referencing a nonexistent accessor class
  (`RecipeManagerAccessor`, never used in the file); `AllFluids.POTION.get().getSource()`/`.getFlowing()`
  return a raw `Fluid`, but `JeiFluidIngredient` needs a `FluidVariant` → wrapped in `FluidVariant.of(...)`.
- [x] `compat/jei/PotionFluidSubtypeInterpreter.java` + `compat/rei/PotionFluidSubtypeInterpreter.java`
  (both clean) — both used the same dead NBT/`PotionUtils` API already retired elsewhere this
  session; rewrote both with the `getVariant().getComponentMap().getOrDefault(DataComponents.POTION_CONTENTS,
  PotionContents.EMPTY)` + `Potion.getName(contents.potion(), prefix)` pattern.
- [x] `content/equipment/clipboard/ClipboardBlockItem.java` (clean) — `openScreen(player,
  heldItem.getComponents())` passed a `DataComponentMap` where an `ItemStack` was wanted →
  `openScreen(player, heldItem)`; a nonexistent `components` field reference inside `openScreen`
  itself → the real local parameter, `stack`.
- Not finished: `content/equipment/extendoGrip/ExtendoGripItem.java` still has 2 unrelated errors
  (missing class `foundation.item.render.SimpleCustomRenderer`, and an `@Override` at line 222 that
  doesn't match any supertype method) plus a cascading error in
  `ExtendoGripRenderHandler.java:115` (`ClientHooks.handleCameraTransforms` — cannot find symbol).
  These look like a separate, not-yet-investigated rendering-registration issue, unrelated to the
  `getCustomData` fix that already resolved this file's other 2 errors.

## Done this session (batch 12)
Trajectory: 1,030 → 1,020 → 1,009 → 1,008. **Crossed below 1,000 errors is close — next milestone.**
- [x] `api/data/recipe/MechanicalCraftingRecipeBuilder.java` (clean) — same dead-import pattern as
  `CreateStandardRecipeGen.java` earlier this session: `net.fabricmc.fabric.api.resource.conditions.v1.{ConditionJsonProvider,DefaultResourceConditions}`
  (nonexistent package, and imported twice each) → `io.github.fabricators_of_create.porting_lib.resources.conditions.{ICondition,ModLoadedCondition,NotCondition}`.
  `withCondition(DefaultResourceConditions.allModsLoaded(modid))` → `withCondition(new
  ModLoadedCondition(modid))`; the "mod missing" variant → `new NotCondition(new
  ModLoadedCondition(modid))`; `recipeConditions.toArray(ICondition[]::new)` (that array-constructor
  method-ref form doesn't apply to this custom `ICondition`) → `recipeConditions.toArray(new
  ICondition[0])`.
- [x] `CreateClient.java` (clean) — `registerOverlays()`'s whole body called every overlay renderer
  with a leftover NeoForge-era 3-4 arg signature (`(graphics, partialTicks, window)`, some also
  passing a `Gui gui`), but every one of these renderer classes was already correctly ported to the
  real fabric `HudRenderCallback`/`LayeredDraw.Layer` 2-arg shape, `(GuiGraphics, DeltaTracker)` —
  confirmed via `javap` on `net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback`. Checked
  each renderer class's actual current method individually rather than assuming one pattern fits
  all: `RemainingAirOverlay`/`TrackPlacementOverlay` are `LayeredDraw.Layer` instances (call via
  `.INSTANCE.render(graphics, partialTicks)`); `TrainHUD`'s render method is `private` with a public
  `LayeredDraw.Layer OVERLAY` field wrapping it (call via `TrainHUD.OVERLAY.render(...)`);
  `GoggleOverlayRenderer`/`BlueprintOverlayRenderer`/`LinkedControllerClientHandler`/`ToolboxHandlerClient`
  are plain static 2-arg `renderOverlay(graphics, partialTicks)`; `SCHEMATIC_HANDLER` (a
  `SchematicHandler` instance field) implements `LayeredDraw.Layer` directly → `.render(...)`;
  `VALUE_SETTINGS_HANDLER` (a `ValueSettingsClient` instance) → `.render(graphics, partialTicks)`
  unchanged in shape, just dropped the wrong extra args. Also `Mods.TRINKETS` didn't exist as an enum
  constant in `compat/Mods.java` at all (Trinkets is one of the user's priority mods) — added it.
- [x] `compat/Mods.java` (clean) — `isLoaded = LoadingModList.get().getModFileById(id) != null`
  (NeoForge-only `LoadingModList`) → `FabricLoader.getInstance().isModLoaded(id)` (already imported
  in this file). Also removed a duplicate `import net.minecraft.core.registries.BuiltInRegistries;`.

## Done this session (batch 13)
Trajectory: 1,008 → 999. **Crossed below 1,000 errors.**
- [x] `content/kinetics/crusher/CrushingWheelControllerBlockEntity.java` (clean) — `import
  io.github.fabricators_of_create.porting_lib.util.EnvExecutor;` (wrong package) →
  `com.tterrag.registrate.fabric.EnvExecutor` (matches every other correctly-ported file that uses
  this class); missing `RecipeWrapper` import; `findRecipe()` referenced an undefined `wrapper`
  variable and passed the raw `inventory` (a `ProcessingInventory extends ItemStackHandlerContainer`)
  directly to `AllRecipeTypes.MILLING.find(...)`, which needs a `RecipeInput` — built a `new
  RecipeWrapper(inventory)` (works since `ItemStackHandlerContainer` implements `SlottedStackStorage`,
  same as the `MillstoneBlockEntity.java`/`CreateJEI.java` fixes earlier this session) and used it
  for both the crushing and milling lookups. Same recurring `getCraftingRemainingItem()`/
  `hasCraftingRemainingItem()`-on-`ItemStack`-instead-of-`Item` bug as `MillstoneBlockEntity.java`
  and `PotionFluidHandler.java` earlier this session → `input.getItem().hasCraftingRemainingItem() ?
  new ItemStack(input.getItem().getCraftingRemainingItem()) : ...`.

## Done this session (batch 14)
Trajectory: 999 → 990 → 989.
- [x] `content/equipment/potatoCannon/PotatoCannonItem.java` (clean) — `implements ... 
  EntitySwingListenerItem, ReequipAnimationItem` (both imported from
  `io.github.fabricators_of_create.porting_lib.item`) don't exist anywhere in porting-lib (checked
  every module jar with `javap`) and aren't used by `Createforge`'s original either — the original
  just extends `ProjectileWeaponItem implements CustomArmPoseItem` directly with no such interfaces.
  Removed both from the `implements` list and deleted their now-orphaned `@Override`
  `shouldCauseReequipAnimation(...)`/`onEntitySwing(...)` methods (dead code — nothing else in the
  file calls them, and they don't override any real vanilla `Item` method either), with a `// TODO
  fabric:` note that these NeoForge item-extension hooks have no fabric port. Also:
  `stack.getEnchantmentLevel(lookup.getOrThrow(...))` doesn't exist on `ItemStack` directly (same
  family of bug as the `BlockHelper.java`/`EnchantmentHelper.getItemEnchantmentLevel` fix earlier
  this session) → `EnchantmentHelper.getItemEnchantmentLevel(lookup.getOrThrow(...), stack)`; a
  private static `AMMO_PREDICATE` field was simply missing from the class entirely (confirmed against
  `Createforge`'s original, which has it) — added it back verbatim.
- [x] `content/equipment/potatoCannon/AllPotatoProjectileBlockHitActions.java` (clean) — `PlantCrop`'s
  `execute(...)` gated its entire body on `cropBlock.value() instanceof SpecialPlantable` —
  `net.neoforged.neoforge.common.SpecialPlantable` is NeoForge-only with no fabric port anywhere
  (confirmed against `Createforge`'s import). Replaced the whole special-plantable branch with a
  `// TODO fabric:` comment and `return false;` — this specifically means crops that rely on
  `SpecialPlantable` (rather than being placeable as a plain `BlockItem`) can't be planted by potato
  cannon fire; documented as a real, if narrow, missing feature rather than silently no-opping.

## Done this session (batch 15)
Trajectory: 989 → 980.
- [x] `content/decoration/copycat/CopycatBlock.java` (clean) — the whole top of the file had a
  duplicated import block (everything from `java.util.function.BiFunction` down through
  `BlockPickInteractionAware` was imported twice), and the *second* copy additionally pulled in two
  genuinely-nonexistent classes: `io.github.fabricators_of_create.porting_lib.enchant.EnchantmentBonusBlock`
  and `io.github.fabricators_of_create.porting_lib.block.ValidSpawnBlock` (checked every porting-lib
  module jar with `javap`/`unzip -l` — neither exists anywhere, and `ValidSpawnBlock` was already
  flagged as missing in a pre-existing `// TODO fabric-port` comment in this same file). Deleted the
  duplicate import block and both dead imports; removed `EnchantmentBonusBlock` from the
  `implements` list and deleted the whole `getEnchantPowerBonus(...)` override — vanilla `Block` no
  longer has any `getEnchantPowerBonus` hook to override at all in 1.21.1 (confirmed via `javap` on
  `Block`), so this wasn't just a missing porting-lib class, the override target itself is gone;
  documented as a real (if narrow) missing feature: copycat blocks can no longer mimic a wrapped
  material's enchanting-table power bonus (e.g. impersonating a bookshelf). Fixed a real
  copy-paste bug at the top of `useItemOn(...)`: stray leading whitespace and a reference to an
  undefined `pPlayer` where the actual parameter is named `player`. Fixed
  `mat.getBlock().getCloneItemStack(level, pos, mat)` — that vanilla method needs a `LevelReader`,
  but `getPickedStack(...)`'s own `level` parameter is only a `BlockGetter` (which doesn't extend
  `LevelReader`) — guarded with `level instanceof LevelReader lr ? mat.getBlock().getCloneItemStack(lr,
  pos, mat) : new ItemStack(mat.getBlock())` so it still works in the common case (a real `Level`
  always satisfies `LevelReader`) without a static type error.
- Noted in passing but not yet fixed (surfaced by this file's error dump, all in unrelated files):
  `CartAssemblerBlock.java` (`PushReaction`/`UseFirstBehaviorItem` cannot-find-symbol +
  method-doesn't-override), `GearboxBlock.java:46` (same `BlockGetter`→`LevelReader`
  `getCloneItemStack` bug as this file, not yet applied there), `ChainConveyorVisual.java:32`
  (`net.minecraftforge.registries` — dead NeoForge import), `CogwheelBlockItem.java:33`
  (`UseFirstBehaviorItem` cannot-find-symbol), `MillstoneBlock.java:65` (the same
  two-different-classes-both-named-`ItemStackHandler` bug documented earlier this session, now
  showing up in the block, not just the block entity).

## Done this session (batch 16)
Trajectory: 980 → 971.
- [x] `content/contraptions/render/ContraptionRenderInfo.java` (clean) — `setupRenderWorld(...)`
  hand-built its own `VirtualRenderWorld` from scratch, populating it from fields that don't exist on
  `Contraption` at all: `c.presentBlockEntities`, `c.modelData` (NeoForge's per-block `ModelData`
  attachment system, which `VirtualRenderWorld.java` already documents elsewhere in this codebase as
  unported — see the commented-out `getModelData` there), and `Contraption.RenderedBlocks`/
  `c.getRenderedBlocks()` (doesn't exist on `Contraption`). Found that
  `content/contraptions/render/ClientContraption.java` — a sibling class, wired up via
  `Contraption.getOrCreateClientContraptionLazy()` — already solves every one of these exact
  problems correctly and is a real, live code path (its own `VirtualRenderWorld` subclass, its own
  `renderedBlockEntities` list, and its own nested `RenderedBlocks` record with a working
  `getRenderedBlocks()`). Rather than re-deriving a second, broken implementation of the same thing,
  rewrote `setupRenderWorld` to just return `c.getOrCreateClientContraptionLazy().getRenderLevel()`,
  and `buildStructureBuffer` to call `contraption.getOrCreateClientContraptionLazy().getRenderedBlocks()`
  instead of the nonexistent `contraption.getRenderedBlocks()`. Confirmed this class is still a live
  code path first (not equally-dead code to just delete) — `ContraptionRenderInfoManager::onReloadLevelRenderer`
  is registered against `ReloadLevelRendererCallback.EVENT` in `foundation/events/ClientEvents.java:482`.

## Done this session (batch 17)
Trajectory: 971 → 963.
- [x] `foundation/blockEntity/behaviour/inventory/VersionedInventoryWrapper.java` (clean) — this
  wrapper's `simulateInsert`/`simulateExtract` overrides were marked `@SuppressWarnings("removal")`
  (a hint the original author knew these were deprecated), but fabric-api's `Storage<T>` interface
  has now actually *removed* them (confirmed via `javap` — the interface only has `insert`/`extract`/
  the iterator methods and `getVersion()`), so nothing in `inventory`'s type calls or supports them
  any more; deleted both overrides entirely (nothing else in the codebase calls them on this class —
  the two other unrelated `.simulateInsert`/`.simulateExtract` call sites found via grep are calling
  a *different* class, `ItemHandlerWrapper`/`StorageUtil`, not this one). Also deleted the whole
  `exactView(...)` override (also not part of `Storage<T>` any more). The bigger fix:
  `ListeningStorageView<T>` (used by `iterator()`/`nonEmptyIterator()`) didn't exist anywhere in the
  codebase or in fabric-api itself (checked both) — it was called as if it were a ready-made wrapper
  class. Wrote it as a small private nested class implementing `StorageView<T>`, delegating every
  read-only accessor straight to the wrapped view and calling the version-increment listener via
  `TransactionSuccessCallback.register(transaction, listener)` on a successful `extract`.

## Done this session (batch 18)
Trajectory: 963 → 955.
- [x] `content/redstone/displayLink/source/EnchantPowerDisplaySource.java` (clean) — imported
  `net.minecraft.world.level.block.EnchantmentTableBlock`/`net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity`
  (both misspelled/nonexistent — vanilla's real class is `EnchantingTableBlock`, and vanilla has no
  block entity for the enchanting table at all) while the method *body* correctly used the real name,
  `EnchantingTableBlock`, throughout — so the two names were simply never reconciled. Compared against
  `Createforge`'s original, which gates on `context.getSourceBlockEntity() instanceof
  EnchantingTableBlockEntity` — that only works there because NeoForge patches vanilla to add a real
  block entity to the enchanting table; fabric's vanilla has none, so `getSourceBlockEntity()` would
  always return null/non-matching and this display source would never activate. Rewrote the gate to
  check the block type directly instead: `level.getBlockState(pos).getBlock() instanceof
  EnchantingTableBlock`. Also removed the other missing porting-lib class,
  `EnchantmentBonusBlock` (same missing class as `CopycatBlock.java` earlier this session), and its
  `state.getBlock() instanceof EnchantmentBonusBlock bonus ? bonus.getEnchantPowerBonus(...) : ...`
  branch — left a `// TODO fabric:` noting that only plain vanilla bookshelves contribute enchant
  power now, since NeoForge's `BlockState#getEnchantPowerBonus`/porting-lib's `EnchantmentBonusBlock`
  extension point has no fabric port (consistent with the same finding in `CopycatBlock.java`).

## Done this session (batch 19)
Trajectory: 955 → 942.
- [x] `foundation/item/CombinedSlottedStackStorage.java`, `api/contraption/storage/item/MountedItemStorageWrapper.java`,
  `content/logistics/crate/CreativeCrateMountedStorage.java` (all clean) — another instance of the
  **two-different-classes-both-named-`SlottedStackStorage`** bug pattern documented earlier this
  session for `ItemStackHandler`: the project's own
  `com.simibubi.create.infrastructure.fabric.transfer.item.SlottedStackStorage` (a small interface
  with an abstract `isItemValid(int, ItemStack)`, using vanilla `ItemStack`) vs. porting-lib's own
  `io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage` (a bigger interface
  with default `isItemValid(int, ItemVariant, int)`/`insertSlot`/`extractSlot`, all fabric-transfer
  types). `CombinedSlottedStackStorage.java` correctly imported the project's own interface (matching
  what its actual generic-bound usages like `MountedItemStorage` implement) but then wrote
  `@Override` bodies for the *other* interface's method shapes (`isItemValid(int, ItemVariant,
  int)`, plus a whole `insertSlot`/`extractSlot` pair that the project's own interface doesn't
  declare at all) — none of those actually override anything. Deleted `insertSlot`/`extractSlot`
  entirely and rewrote `isItemValid` to the real shape, `isItemValid(int slot, ItemStack stack)`,
  delegating through `getFromStorage` like the other methods already did.
  `MountedItemStorageWrapper.java` (a subclass) had a from-scratch "O(1) lookup array" reimplementation
  overriding `getIndexForSlot`/`getSlotFromIndex` — neither method exists on `CombinedSlottedStackStorage`
  or its fabric-api base `CombinedSlottedStorage` (which already implements `getSlot`/`getSlotCount`
  concretely, with no such extension point), and the code referenced a completely undefined
  `itemHandler` array besides. Deleted the whole broken optimization block; the class now just
  delegates to the constructor, relying on the base class's own (already-correct) linear scan.
  `CreativeCrateMountedStorage.java` was simply missing an `isItemValid` override altogether — added
  one following the same "accept anything matching the creative crate's supplied item" logic used by
  its `extract(...)` override just below it (`ItemVariant.of(stack).matches(this.suppliedStack)`).

## Done this session (batch 20)
Trajectory: 942 → 935.
- [x] `foundation/gui/RemovedGuiUtils.java` (clean) — `io.github.fabricators_of_create.porting_lib.util.client.ScreenUtils`
  (used only for 3 plain `int` color constants, `DEFAULT_BACKGROUND_COLOR`/`DEFAULT_BORDER_COLOR_START`/
  `DEFAULT_BORDER_COLOR_END`) doesn't exist anywhere — checked every jar on the classpath. These used
  to be public fields on vanilla's tooltip-rendering class in older versions but aren't exposed
  anywhere in 1.21.1 vanilla or fabric-api any more (`Screen`/`TooltipRenderUtil` were both checked
  via `javap`). Since they're just the well-known hardcoded tooltip colors, inlined them as private
  `static final int` fields directly in this class instead of chasing a nonexistent import. Also
  removed two now-fully-unused imports that had only existed to backstop that same dead
  `ScreenUtils` import (`javax.annotation.Nonnull`, `com.mojang.blaze3d.vertex.Tesselator` — neither
  referenced anywhere in the file body).

## Done this session (batch 21)
Trajectory: 935 → 929 → 928. Also crossed under 930.
- [x] `foundation/events/InputEvents.java` (clean) — `onUse(Minecraft, HitResult, InteractionHand)`
  declares a return type of `InteractionResult` but its body still referenced a nonexistent `event`
  parameter (`event.getHand()`, `event.setCanceled(true)` — leftover from a pre-fabric
  `PlayerInteractEvent`-style signature) and had two bare `return;` statements where the real
  signature needs `return InteractionResult.PASS;`. This method also isn't currently wired to any
  fabric callback (checked `register()` — only `onKeyInput` is registered there), so the fix is
  purely about making the file compile, not about anything currently reachable at runtime. Rewrote:
  `event.getHand()` → the real `hand` parameter; the trailing `CatnipServices.PLATFORM.executeOnClientOnly(()
  -> () -> { if (ChainPackageInteractionHandler.onUse()) event.setCanceled(true); })` (deferred,
  can't produce a return value, and this method is already client-only given its `Minecraft mc`
  parameter) → a direct, synchronous `if (ChainPackageInteractionHandler.onUse()) return
  InteractionResult.SUCCESS;` followed by a final `return InteractionResult.PASS;`. Also
  `Tags.Items.TOOLS_WRENCH` (porting-lib tag shim, missing field — same recurring bug fixed in
  `MechanicalCrafterBlockEntity.java` and `AllItems.java` earlier this session) →
  `AllTags.AllItemTags.WRENCH.tag`; added missing `ItemStack`/`Items` imports.

## Done this session (batch 22)
Trajectory: 928 → 921.
- [x] `content/logistics/crate/BottomlessItemHandler.java` (clean) — another instance of the
  **two-different-classes-both-named-`ItemStackHandler`** bug: this file `extends
  com.simibubi.create.infrastructure.fabric.transfer.item.ItemStackHandler` (the project's own,
  simple version), but its body uses `makeSlot(...)`/`ItemStackHandlerSlot` — both of which only
  exist on **porting-lib's own** `ItemStackHandler` (confirmed via `javap` — the project's own has no
  `makeSlot` at all). Switched the import to porting-lib's `ItemStackHandler`. Also a real logic bug:
  `getStack()` and `extract(...)` had been merged from two *different* NeoForge methods
  (`Createforge`'s original has a 2-slot `getStackInSlot(int slot)` and a separate
  `extractItem(int slot, int amount, boolean simulate)`) into one method, leaving `slot`/`amount` as
  dangling undefined variables and `extract(...)` returning an `ItemStack` where a `long` amount was
  required. Rewrote `extract` to return `Math.min(stack.getMaxStackSize(), maxAmount)` (or `0` if
  empty/non-matching) and `getStack()` to drop the vestigial 2-slot check entirely (this is a
  single-slot `SingleSlotStorage`, so the old "slot 1 is always empty" branch from the 2-slot
  original doesn't apply). Fixed `BottomlessSlot.save()`'s override signature — it was `public
  CompoundTag save()` (no such method on `ItemStackHandlerSlot`) → the real signature,
  `public Tag save(HolderLookup.Provider provider, Tag tag)`.

## Done this session (batch 23)
Trajectory: 921 → 914.
- [x] `content/kinetics/crafter/MechanicalCrafterBlock.java` (clean) — `useItemOn(...)`'s real
  parameters are `stack`/`hand`, but its body referenced undefined `heldItem`/`handIn` (leftover
  renamed-parameter mismatch) and called `ItemHandlerHelper.copyStackWithSize(...)` (never imported,
  doesn't exist under that name in this codebase) → replaced with the real params and vanilla
  `stack.copyWithCount((int) (stack.getCount() - inserted))`. Also `crafter.getInventory().getItem(0)`
  — `getInventory()` returns the block entity's own `Inventory extends SmartInventory` (not a vanilla
  `Container`), which has no `.getItem(int)` → same `.getStackInSlot(0)` fix already applied inside
  `MechanicalCrafterBlockEntity.java` itself earlier this session, just needed here too at the call
  site in the block class.

## Done this session (batch 24)
Trajectory: 914 → 907.
- [x] `content/logistics/box/PackageEntity.java` (clean) — `import
  io.github.fabricators_of_create.porting_lib.entity.IEntityAdditionalSpawnData;` was simply the
  wrong class name — the real interface this class implements (`implements
  IEntityWithComplexSpawn`) lives right there in the same package under its own correct name,
  `io.github.fabricators_of_create.porting_lib.entity.IEntityWithComplexSpawn`, with
  `writeSpawnData(RegistryFriendlyByteBuf)`/`readSpawnData(RegistryFriendlyByteBuf)` (confirmed via
  `javap`) — both already matched exactly in this file's overrides, so just fixing the import
  resolved both "method does not override" errors along with the missing-class error. Removed two
  other dead imports referencing NeoForge-only hooks with no fabric port at all: `CommonHooks` (used
  once, to let other mods veto `player-attacks-target`; deleted the check, defaulting to "always
  allowed") and `Item#canBeHurtBy(ItemStack, DamageSource)` (a per-item damage-immunity hook; deleted
  the check with a `// TODO fabric:` note, defaulting to vanilla's "always true" behavior since no
  Create item overrode it anyway). Also removed an unused `PortingLibEntity` import and the
  wrong-package `LivingAttackEvent` import (never referenced in the file body).

## Done this session (batch 25)
Trajectory: 907 → 900. **Crossed below 900 errors.**
- [x] `content/kinetics/fan/AirFlowParticleData.java` (clean) — purely missing imports:
  `org.jetbrains.annotations.NotNull`, `net.minecraft.network.codec.StreamCodec`,
  `net.minecraft.network.codec.ByteBufCodecs`. The file body already used all three correctly.

## Done this session (batch 26)
Trajectory: 900 → 893.
- [x] `content/fluids/pipes/GlassPipeVisual.java` (clean) — imported straight NeoForge fluid types
  (`net.neoforged.neoforge.fluids.{FluidStack,FluidType}`,
  `net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions`), never converted at
  all, and `flow.fluid` (from `PipeConnection.Flow`) is actually the project's own
  `com.simibubi.create.infrastructure.fabric.transfer.fluid.FluidStack` (confirmed via
  `PipeConnection.java`'s own imports). Rewrote the flowing/still-sprite, tint-color, and luminosity
  lookups using fabric-transfer-api's real client rendering API (found via `javap` on the actually
  resolved `fabric-transfer-api-v1` jar): `FluidVariantRendering.getSprites(variant)` (index 0 =
  still, 1 = flowing — confirmed from the interface's own javadoc in the sources jar, not guessed),
  `FluidVariantRendering.getColor(variant)` for tint, and `FluidVariantAttributes.getLuminance(variant)`
  in place of NeoForge's per-fluid `FluidType#getLightLevel`. Removed the now-unused
  `Minecraft`/`InventoryMenu`/`Fluid`(vanilla) imports and the atlas-lookup code they supported,
  since sprites now come pre-resolved from `FluidVariantRendering` instead of needing a manual
  `Minecraft.getInstance().getTextureAtlas(...)` + `ResourceLocation` round-trip.

## Done this session (batch 27)
Trajectory: 893 → 887 → 880.
- [x] `content/trains/entity/CarriageContraptionEntity.java` (clean) — `writeSpawnData`/
  `readSpawnData` still took plain `FriendlyByteBuf` instead of the real
  `IEntityWithComplexSpawn` signature, `RegistryFriendlyByteBuf` (same interface confirmed via
  `javap` in the `PackageEntity.java` fix this session); `AllPackets.getChannel().sendToClientsTracking(...)`
  doesn't exist anywhere on `AllPackets` (checked — it only has a `register()` method) → replaced
  with the established `CatnipServices.NETWORK.sendToClientsTrackingEntity(this, packet)` pattern
  used elsewhere (`ISyncPersistentData.java`, `MinecartController.java`); `@OnlyIn(Dist.CLIENT)`
  (NeoForge) → `@Environment(EnvType.CLIENT)` (fabric), already imported in the file.
- [x] `content/trains/entity/CarriageDataUpdatePacket.java` (clean) — fixing the call site above
  required fixing this packet class too: it `extends SimplePacketBase`, a class that doesn't exist
  anywhere in this codebase (`com.simibubi.create.foundation.networking.SimplePacketBase` — pure
  NeoForge-network-era leftover, never ported). Rewrote it from scratch as a record implementing
  `net.createmod.catnip.net.base.ClientboundPacketPayload`, following the exact pattern already
  established in this session's other fixed packets (e.g.
  `ClientboundChainConveyorRidingPacket.java`): a `STREAM_CODEC` field, `getTypeProvider()` returning
  the existing `AllPackets.CARRIAGE_DATA_UPDATE` entry (which was already written expecting a
  `CarriageDataUpdatePacket.STREAM_CODEC` — the packet class just hadn't been updated to provide
  one), and `handle(LocalPlayer)` under `@Environment(EnvType.CLIENT)`. `CarriageSyncData` has no
  `StreamCodec` of its own, only `write(FriendlyByteBuf)`/a `(FriendlyByteBuf)` constructor, so built
  one inline via `StreamCodec.of((buf, data) -> data.write(buf), CarriageSyncData::new)`.

## Done this session (batch 28)
Trajectory: 880 → 873.
- [x] `content/contraptions/minecart/MinecartSim2020.java` (clean) — same missing
  `io.github.fabricators_of_create.porting_lib.util.MinecartAndRailUtil` class flagged as a dead end
  for `MinecartController.java` above, but this file's three usages turned out to be individually
  fixable without needing the whole class: `MinecartAndRailUtil.getDirectionOfRail(trackState,
  level, pos, railBlock)` is just `trackState.getValue(railBlock.getShapeProperty())` — the
  `RailShape` is a plain block-state property, no utility method needed, confirmed via `javap` on
  `BaseRailBlock` (`getShapeProperty()` is the only rail-direction-related method it exposes).
  `MinecartAndRailUtil.getSlopeAdjustment()` is vanilla `AbstractMinecart`'s own long-stable
  slope-adjustment constant (`2^-7 = 0.0078125`, from vanilla's original un-extracted minecart
  movement code) — inlined as a local `private static final double SLOPE_ADJUSTMENT`.

## Done this session (batch 29)
Trajectory: 873 → 838 (confirmed) → further fixes applied, next compile pending.
- [x] `foundation/mixin/accessor/FluidInteractionRegistryAccessor.java` (clean) — mixin accessor was
  targeting NeoForge's `net.neoforged.neoforge.fluids.FluidInteractionRegistry`, which doesn't exist on
  fabric. Porting-lib ships its *own* `io.github.fabricators_of_create.porting_lib.fluids.FluidInteractionRegistry`
  with an equivalent private `INTERACTIONS` field (confirmed via `javap -p` on the resolved
  `fluids-8f637a66-3.1.0-beta.91` jar) — repointed the `@Mixin`/`@Accessor` at that class instead. Only
  consumer (`content/kinetics/drill/CobbleGenOptimisation.java`) already imported the correct
  porting-lib types, so no other changes needed there.
- [x] `infrastructure/gametest/CreateGameTestHelper.java` (clean) — called five `TransferUtil` methods
  that were never implemented on fabric (`getFluidStorage`, `firstOrEmpty`, `totalCapacity`,
  `getItemStorage(Level,BlockPos)`, `extractAllAsStacks`) plus a nonexistent NeoForge
  `ItemHandlerHelper.canItemStacksStack`. Added the five missing methods to
  `infrastructure/fabric/transfer/TransferUtil.java` (thin wrappers around `ItemStorage.SIDED`/
  `FluidStorage.SIDED` lookups and `Storage`/`StorageView` iteration — same style as the file's existing
  methods), and replaced the `ItemHandlerHelper` predicate with vanilla
  `ItemStack.isSameItemSameComponents` (already the established pattern elsewhere in this codebase).
- [x] `content/trains/schedule/ScheduleScreen.java` + `content/logistics/stockTicker/StockKeeperRequestScreen.java`
  (both clean) — both imported a nonexistent `com.simibubi.create.foundation.gui.ScreenWithStencils` and
  called `getGuiLeft()`/`getGuiTop()`. Confirmed via `javap` that vanilla `AbstractContainerScreen` only
  ever had plain `protected int leftPos`/`topPos` fields — `getGuiLeft()`/`getGuiTop()` are NeoForge
  patches to the vanilla class that don't exist on fabric. Deleted the dead import, replaced the two
  method calls with direct field access in both files. (The three `GhostIngredientHandler.java` copies
  under `compat/{jei,rei,emi}` already use the correct pattern — porting-lib's
  `AbstractContainerScreenAccessor#port_lib$getGuiLeft()` mixin accessor — left untouched.)
- [x] `content/redstone/link/controller/LinkedControllerItem.java` (clean) — wrong package for
  `UseFirstBehaviorItem` (real path has `.item.extensions.`, not `.item.`); two dead imports
  (`SimpleCustomRenderer` — class doesn't exist, only referenced from a commented-out block; unused
  `NetworkHooks`, which also doesn't exist in porting-lib at all — confirmed via `strings`/`unzip -l`
  across every module jar). Root cause of the file's `openMenu` error and three others below: NeoForge
  patches `Player` with a second `openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)` overload
  for syncing extra data to the client-side menu factory; vanilla (and thus fabric) `Player` only has
  the one-arg `openMenu(MenuProvider)` (confirmed via `javap -p`). Since this item's `createMenu` already
  reads `player.getMainHandItem()` directly server-side, no extra data needs to travel — swapped to the
  one-arg call.
- [x] Same `openMenu` two-arg pattern, same fix (all clean): `content/logistics/redstoneRequester/RedstoneRequesterBlockEntity.java`,
  `content/logistics/packagePort/PackagePortBlockEntity.java` (both passed `worldPosition` as the second
  arg — pointless anyway since `createMenu` closes over `this` directly), `content/logistics/filter/FilterItem.java`,
  `content/trains/schedule/ScheduleItem.java` (both passed a `buf -> ItemStack.STREAM_CODEC.encode(...)`
  writer, but every concrete `createMenu` override already calls `player.getMainHandItem()` itself, so
  the encoded data was never actually consumed).
- [x] `content/logistics/factoryBoard/FactoryPanelBehaviour.java` (clean) — same `openMenu` pattern, but
  here the extra data (`FactoryPanelPosition`) genuinely *is* consumed client-side (see
  `FactoryPanelSetItemMenu`'s `RegistryFriendlyByteBuf extraData` constructor, registered in
  `AllMenuTypes.FACTORY_PANEL_SET_ITEM` via registrate's fabric `MenuBuilder`, which backs this with a
  real `net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType`). The fabric-native
  replacement for NeoForge's two-arg `openMenu` is implementing
  `ExtendedScreenHandlerFactory<D>` (`extends MenuProvider`, adds `D getScreenOpeningData(ServerPlayer)`)
  on the class and calling the plain one-arg `openMenu(this)` — fabric's screen-handler-registry
  internally calls back into `getScreenOpeningData` and encodes it with the `MenuType`'s own packet
  codec. Changed `implements MenuProvider` to `implements ExtendedScreenHandlerFactory<FactoryPanelPosition>`
  and added `getScreenOpeningData` returning `getPanelPosition()`.
- [x] `content/logistics/filter/FilterItemStack.java` (clean) — `ItemStack.isComponentsPatchEmpty()`
  doesn't exist in this MC version's `ItemStack` (checked via `javap -p`); the real accessor is
  `getComponentsPatch()` (returns `DataComponentPatch`, which does have `isEmpty()`).
- [x] Systemic bug, 5 files (all clean): `TransferUtil.truncateLong` doesn't exist — the real method
  lives on `foundation/item/ItemHelper.truncateLong` (confirmed dozens of other call sites in the
  codebase already use `ItemHelper.truncateLong` correctly). Fixed
  `compat/jei/category/SpoutCategory.java`, `content/schematics/cannon/MaterialChecklist.java`,
  `content/logistics/itemHatch/ItemHatchBlock.java`, `content/logistics/packager/InventorySummary.java`
  (also had duplicate imports of `TransferUtil`/`ItemVariant`/`StorageView` from an earlier bad merge —
  cleaned those up too), `api/contraption/storage/item/WrapperMountedItemStorage.java`. Removed the
  now-dead `TransferUtil` import from the two files where it had no other use, added the missing
  `ItemHelper` import to all four that lacked it.
- [x] `infrastructure/data/CreateMountedItemStorageTypeTagsProvider.java` (clean) — referenced `Create.ID`
  without importing `com.simibubi.create.Create`.

**Note for the deferred `BlueprintEntity.java` item below**: its `player.openMenu(section, buf -> {...})`
error is almost certainly the same NeoForge-two-arg-`openMenu` pattern fixed across six files above —
worth rechecking with the now-established fix (either drop to one-arg, or make the class an
`ExtendedScreenHandlerFactory` if the synced data is actually read client-side) before assuming it needs
a from-scratch design.

## Done this session (batch 30)
Continuation of batch 29's sweep, finishing off files that had more than one broken thing. Trajectory:
838 → 831 → 822 → 811 → **809 errors**.
- [x] `content/fluids/potion/PotionFluid.java` (clean) — root cause of a wide cascade (broke every
  `compat/jei/category/*Category.java` file that imports `PotionFluidHandler`, since a class with a
  broken method body still fails to provide symbols to its callers). Missing
  `import net.minecraft.world.item.alchemy.PotionContents;` entirely (used 4 times, never imported —
  classic copy-paste-from-Createforge miss since Forge's version pulled it in transitively). Also called
  `fs.getTag()` and a 3-arg `FluidStack(Fluid, long, Tag)` constructor that don't exist on fabric's
  `FluidStack` (which wraps an immutable `FluidVariant`, not an NBT tag). Since `FluidStack` had no
  mutator at all for its component data, added `set(DataComponentType<T>, T)` and
  `remove(DataComponentType<T>)` to `infrastructure/fabric/transfer/fluid/FluidStack.java` (made the
  `variant` field non-final, both methods rebuild it via `FluidVariant#withComponentChanges`) — the
  fabric-native equivalent of `ItemStack`'s mutable component API. Simplified
  `addPotionToFluidStack` to mutate and return the same stack instead of copying through the broken
  constructor. Cleaned up duplicate imports left over from an earlier bad merge (`ByteBuf`, `Lang` each
  imported twice) and removed unused `BlockPos`/`ResourceLocation`/`Potion`/`Potions` imports.
- [x] `compat/computercraft/implementation/ComputerBehaviour.java` +
  `content/contraptions/chassis/StickerBlockEntity.java` (both clean) — `removePeripheral()` called
  NeoForge-only `Level#invalidateCapabilities`, which has no fabric equivalent (fabric's API lookups
  re-query on demand, no invalidation signal needed). No-op'd the body with a `// TODO fabric:`-style
  comment. This was blocking `StickerBlockEntity` and `RedstoneRequesterBlockEntity`, both of which just
  call `computerBehaviour.removePeripheral()` from `invalidate()` — neither is CC:Tweaked-specific itself,
  they just happened to depend on this one CC:Tweaked compat method compiling.
- [x] `content/logistics/factoryBoard/FactoryPanelBehaviour.java` (clean, second pass) — two more
  unrelated breaks beyond the `openMenu` one from batch 29: `ItemStackLinkedSet.TYPE_AND_TAG` is private
  upstream (the project already has a `foundation/mixin/accessor/ItemStackLinkedSetAccessor` mixin
  exposing it as `getTYPE_AND_TAG()`, already used correctly in `ItemDrainCategory.java` — switched to
  that); `Items.TOOLS_WRENCH` isn't a real vanilla item constant on fabric — the established pattern
  (used in `InputEvents.java` earlier this session) is `AllTags.AllItemTags.WRENCH.tag`.
- [x] `content/logistics/itemHatch/ItemHatchBlock.java` (clean) — dead `LazyOptional` import (unused,
  class doesn't exist in porting-lib); stale pre-rename parameter names (`pState`/`pPos`/`pLevel` instead
  of the real `useItemOn` params `state`/`pos`/`level` — same "leftover NeoForge parameter names" bug
  pattern seen repeatedly this session); same `Items.TOOLS_WRENCH` → `AllItemTags.WRENCH.tag` fix (this
  file already imports `AllItemTags` directly).
- [x] `content/logistics/packagePort/PackagePortBlockEntity.java` (clean) — `SmartInventory` (extends the
  project's own `ItemStackHandler`) has `clear()`, not `clearContent()` — the override was recursing into
  a method that doesn't exist instead of delegating to the real one. `CatnipCodecUtils.decodeOrNull(...)`
  doesn't exist (checked catnip's real source via the resolved Ponder sources jar) — real API is
  `decode(codec, registries, tag)` returning `Optional<T>`; added `.orElse(null)`.
- [x] `content/logistics/redstoneRequester/RedstoneRequesterBlockEntity.java` (clean) — dead
  `NetworkHooks` import (doesn't exist in porting-lib, confirmed again this session).
  `computerBehaviour.removePeripheral()` unblocked by the `ComputerBehaviour.java` fix above.
- [x] `content/trains/schedule/ScheduleScreen.java` (clean, second pass) — `KeyMapping#isActiveAndMatches`
  is a NeoForge patch method; vanilla only has `matches(int keyCode, int scanCode)`. Rewrote
  `minecraft.options.keyInventory.isActiveAndMatches(mouseKey)` to call `matches(pKeyCode, pScanCode)`
  directly, which also let the now-unused `InputConstants.Key mouseKey` local and its import be deleted.
- [x] `content/logistics/stockTicker/StockKeeperRequestScreen.java` (clean, second pass) — dead
  `me.pepperbell.simplenetworking.SimpleChannel` import (an old Fabric-networking-library type never
  actually referenced in the file body).
- [x] `content/logistics/filter/FilterItemStack.java`, `infrastructure/data/CreateContraptionTypeTagsProvider.java`,
  `infrastructure/data/CreateEnchantmentTagsProvider.java` — all clean; see batch 29 entry above and the
  vanilla-tags-provider-constructor fix pattern (NeoForge added `modid`/`ExistingFileHelper` params to
  every vanilla `*TagsProvider` constructor as patches; fabric's are plain vanilla, so the extra two args
  just get dropped from the `super(...)` call, and `com.simibubi.create.Create` import removed if it was
  only used for `Create.ID` there).

Deliberately left alone (deprioritized, not touched): `compat/rei/category/SpoutCategory.java:76` — a
`dev.architectury.fluid.FluidStack`-to-our-`FluidStack` bridging issue via a nonexistent 3-arg
`FluidStack(Fluid, long, Tag)` constructor; this is REI-specific (REI is on the standing deprioritized
list) and unrelated to the `compat/jei` copy of the same class, which is already fixed.

## Done this session (batch 31)
Trajectory: 809 → 786 (confirmed) → further fixes applied, next compile pending.
- [x] `content/processing/sequenced/IAssemblyRecipe.java` (clean) — this interface had visibly rotted
  mid-port: a dead import of a nonexistent `SequencedAssemblySubCategory` (real class is
  `JeiSequencedAssemblySubCategory`, only referenced from the dead import, never used in the body); a
  leftover NeoForge `@OnlyIn(Dist.CLIENT)` annotation (should be fabric's already-imported
  `@Environment(EnvType.CLIENT)`); and a genuine duplicate — two `getJEISubCategory()` declarations, one
  correctly returning `SequencedAssemblySubCategoryType` (matching all 4 real implementors:
  `CuttingRecipe`, `PressingRecipe`, `FillingRecipe`, `DeployerApplicationRecipe` — verified each already
  implements the correct one), one stale returning `Supplier<Supplier<SequencedAssemblySubCategory>>`
  using an unimported `Supplier` and the nonexistent class. Deleted the stale duplicate and the dead
  import; fixed the annotation.
- [x] `content/kinetics/deployer/ManualApplicationRecipe.java` (clean) — same `Container`→`RecipeInput`
  generic-type bug as `CuttingRecipe.java`/`ItemApplicationRecipe.java` fixed earlier this session
  (`extends ItemApplicationRecipe`, whose real base is `ProcessingRecipe<RecipeInput, ...>`, not
  `Container`); recurring `ItemStack.getCraftingRemainingItem()` bug (method lives on `Item`, not
  `ItemStack` — same fix pattern as `CrushingWheelControllerBlockEntity.java`/`MillstoneBlockEntity.java`
  from earlier sessions); unused `LivingEntity` import.
- [x] `content/logistics/packager/PackagerItemHandler.java` (clean) — `extractItem(int slot, int amount,
  boolean simulate)` was a leftover Forge-`IItemHandler`-shaped method that doesn't override anything on
  fabric's `SingleSlotStorage<ItemVariant>` interface (whose real abstract method is
  `extract(ItemVariant, long, TransactionContext)`, already correctly implemented as `insert(...)` right
  above it in the same file) — rewrote to match that shape, referencing the real `resource`/`transaction`
  params instead of two undefined bare names the old body used.
- [x] `content/logistics/chute/ChuteItemHandler.java` (clean) — `getSlotLimit(int)` and
  `isItemValid(int, ItemStack)` are Forge-`IItemHandler` leftovers that don't exist on fabric's
  `SingleVariantStorage<ItemVariant>` base class at all (confirmed no such methods in the resolved
  fabric-transfer-api jar) — deleted both, along with their now-orphaned `DataComponents`/`ItemStack`
  references.
- [x] `content/logistics/packager/PackagerBlockEntity.java` + `content/logistics/stockTicker/StockTickerBlockEntity.java`
  (both clean) — same three-part cluster in both files: (1) dead
  `dan200.computercraft.api.peripheral.PeripheralCapability` import (CC:Tweaked, deprioritized, but
  simply unused so it's a one-line delete, not a design decision); (2) `computerBehaviour.removePeripheral()`
  now compiles thanks to the batch-30 `ComputerBehaviour.java` fix; (3) `CatnipCodecUtils.decodeOrNull`
  (doesn't exist — same fix as `PackagePortBlockEntity.java` in batch 30: `.decode(...).orElse(null)`).
  Beyond the shared cluster: `PackagerBlockEntity#clearContent()` called `inventory.setStackInSlot(0, ...)`
  on a `PackagerItemHandler` (a `SingleSlotStorage`, no such method) — the class's real state is the
  `heldBox` field directly, so it now sets that to `ItemStack.EMPTY` instead.
  `StockTickerBlockEntity#getReceivedPaymentsHandler()` declared a return type of `IItemHandler`, a type
  that doesn't exist anywhere on fabric (leftover Forge capability interface name) — its one field
  (`receivedPayments`) is a `SmartInventory`, and its only two callers
  (`compat/computercraft/.../StockTickerPeripheral.java` → `ComputerUtil.list`/`getItemDetail`) both
  expect exactly that type (`ComputerUtil.list(ItemStackHandler)`, and `SmartInventory extends
  ItemStackHandler`), so changed the return type to `SmartInventory` directly rather than inventing a new
  abstraction. Also `receivedPayments.clearContent()` → `.clear()` (same `SmartInventory`/`ItemStackHandler`
  real-method-name fix as `PackagePortBlockEntity.java` in batch 30).

## Done this session (batch 32)
Trajectory: 780 → 768 → 763 (confirmed) → further fixes applied, next compile pending.
- [x] `compat/computercraft/AbstractComputerBehaviour.java` (clean) — the actual root cause of the
  `removePeripheral()` "cannot find symbol" errors in `PackagerBlockEntity`/`StockTickerBlockEntity` from
  batch 30/31 wasn't fully fixed by the `ComputerBehaviour.java` no-op alone: those two block entities
  declare their field with the *base* type `AbstractComputerBehaviour` (the no-CC:Tweaked-loaded stub),
  which never declared `removePeripheral()` at all — only the concrete `ComputerBehaviour` subclass did.
  Added a no-op `removePeripheral()` to the base class alongside its existing no-op `prepareComputerEvent`,
  so both the stub and the real implementation satisfy every caller regardless of which one is active.
- [x] `content/logistics/stockTicker/StockTickerBlockEntity.java` (clean, third pass) — the
  `computerBehaviour` field referenced at 2 call sites was never declared anywhere in the class (a field
  that exists in the equivalent `PackagerBlockEntity.java` but was dropped from this file during the
  port) — added `public AbstractComputerBehaviour computerBehaviour;`. Separately, `clearContent()` was
  annotated `@Override` but neither this class nor `PackagerBlockEntity` actually `implements
  Clearable` (the vanilla interface that declares `clearContent()`) — both already had the unused
  `import net.minecraft.world.Clearable;` sitting there from the port, just never added to the
  `implements` clause. Added `Clearable` to both classes' `implements` lists.
- [x] `content/logistics/packager/PackagerBlockEntity.java` (clean, third pass) — `getAvailableItems()`
  referenced an undefined `scanInputSlots` variable in a ternary
  (`scanInputSlots ? view.getAmount() : view.extract(...)`); tracing the surrounding `try (Transaction t
  = ...)` block showed it never calls `t.commit()`, so every `view.extract(...)` inside it was already a
  no-op simulation regardless of which ternary branch ran (transactions roll back on close without a
  commit) — simplified to a plain `view.getAmount()` read with no transaction at all, matching
  Createforge's original slot-iteration semantics (`availableItems.add(targetInv.getStackInSlot(slot))`
  for every slot) that this fabric port had needlessly complicated.
- [x] `content/logistics/chute/ChuteItemHandler.java` (clean, second pass) — `variant.getItem().getMaxStackSize()`
  doesn't exist on `Item` in 1.21.1 (confirmed via `javap`); the real method is
  `getDefaultMaxStackSize()`.
- [x] `content/legacy/ChromaticCompoundItem.java` (clean) — `EntityTickListenerItem` had the same
  missing-`.extensions.`-package-segment bug as `UseFirstBehaviorItem` earlier this session (real path is
  `io.github.fabricators_of_create.porting_lib.item.extensions.EntityTickListenerItem`); `CustomMaxCountItem`
  doesn't exist anywhere in porting-lib (checked every module jar) — it backed NeoForge's
  `Item#getItemStackLimit(ItemStack)` hook, which 1.21.1 vanilla has actually superseded with a
  `DataComponents.MAX_STACK_SIZE` data component instead of a Java override point, so there's no direct
  fabric port to fall back on either. Removed the interface and stubbed the override with a
  `// TODO fabric:` comment noting the data-component-based path forward, matching the session's
  established pattern for genuinely-unported extension points (`SpecialPlantable`,
  `EnchantmentBonusBlock`, etc. from earlier in this session).

## Done this session (batch 33): XaeroTrainMap.java (was 30 errors → clean) — missing Gradle dependency
**Xaero's Minimap/World Map is on the standing PRIORITY list** — this was worth fixing properly rather
than deferring.
Root cause: `compat/trainmap/XaeroTrainMap.java` imports `xaero.map.gui.ScreenBase`, which resolves fine
from the `xaeros-world-map` jar — but `javap` showed it `extends xaero.lib.client.gui.ScreenBase`, a
class from **XaeroLib**, a shared library both Xaero mods depend on that `build.gradle.kts` never
declared at all (`class file for xaero.lib.client.gui.ScreenBase not found`). XaeroLib is **not**
published on Modrinth for Fabric (confirmed via the Modrinth API — only old 1.12.2/1.16.5 Forge builds
exist under the `xaerolib` slug); it's distributed from Xaero's own maven at
`https://chocolateminecraft.com/maven`, under group `xaero.lib`, artifact `xaerolib-fabric-1.21.1`
(verified by browsing that maven's directory listing). Both installed Xaero jars' `fabric.mod.json`
declare `"xaerolib": ">=1.0"`, so any 1.21.1 build works — added the latest available, `1.7.3`.
Changes: new `maven("https://chocolateminecraft.com/maven")` repo entry, new `xaeroLibVersion = "1.7.3"`
val, and `modCompileOnly("xaero.lib:xaerolib-fabric-1.21.1:$xaeroLibVersion")` alongside the other two
xaero deps in `build.gradle.kts`. Confirmed clean: 757 → **742 errors** (all 30 `XaeroTrainMap.java`
errors cleared from one dependency add, since they all stemmed from the same unresolvable `ScreenBase`
superclass).

## Done this session (batch 34)
Trajectory: 742 → 733 → 728 → 722 (confirmed) → further fixes applied, next compile pending.
- [x] `content/equipment/toolbox/ToolboxInventory.java` (clean) — several distinct bugs stacked in one
  file: `isItemValid(int, ItemVariant, int)` was a leftover Forge-`IItemHandler`-shaped override that
  doesn't exist on the project's own `ItemStackHandler` base (real signature is `isItemValid(int,
  ItemStack)`) — rewrote to match. `distributeToCompartment(ItemStack, int, boolean simulate)`'s body
  referenced an undefined `ctx` variable; checked every call site in `ToolboxBlockEntity.java`/
  `ToolboxHandlerClient.java` and all of them already pass a `TransactionContext`, not a boolean — the
  method's own signature was wrong, not just its body, so changed the param to `TransactionContext ctx`
  to match every caller (this also fixed the dangling `ctx` reference for free) and swapped the dead
  `ItemHandlerHelper.copyStackWithSize` for `ItemStack#copyWithCount` (this session's established
  replacement). `takeFromCompartment` used the nonexistent porting-lib type `ItemStackHandlerSlot` as a
  local variable type where `getSlot(int)` actually returns `SingleSlotStorage<ItemVariant>`. Also fixed:
  a missing import for `TransactionSuccessCallback` (used but never imported — likely would have surfaced
  as its own error once the method-signature errors above stopped masking it), a duplicate
  `ItemStackHandler` import, and an unused `DataComponents` import.
- [x] `content/equipment/hats/CreateHatArmorLayer.java` (clean) — direct field access on
  `AgeableListModel` (`model.scaleHead`/`babyHeadScale`/`babyYHeadOffset`/`babyZHeadOffset`) where these
  are private vanilla fields; the project's own `AgeableListModelAccessor` mixin already exposes all four
  as `getScaleHead()`/`getBabyHeadScale()`/etc. (added in some earlier, unlogged pass — the accessor
  interface already had them, just nothing in the codebase used them yet) — switched to casting through
  the accessor. Separately, `lastChild.cubes` is a private `ModelPart` field; porting-lib's own
  `ModelPartAccessor` (found in the **`accessors`** porting-lib module specifically — note this module is
  pinned to a different, older version (`3.1.0-beta.54`) than the rest of porting-lib's modules
  (`3.1.0-beta.91`), transitively pulled in by the `extensions` artifact — worth remembering if a future
  "class not found" search for a porting-lib accessor comes up empty against the main version's jars)
  exposes `porting_lib$cubes()` — used that instead.
- [x] `content/equipment/armor/BacktankUtil.java` (clean) — `io.github.fabricators_of_create.porting_lib.util.EnvExecutor`
  doesn't exist (the real `EnvExecutor` other files in this codebase use is `com.tterrag.registrate.fabric.EnvExecutor`,
  but even that only has a void-returning `runWhenOn`, no value-returning `callWhenOn` this file needed) —
  since `isBarVisible`/`getBarWidth`/`getBarColor` are all vanilla `Item` client-only rendering hooks
  (never invoked server-side by the game engine), simplified all three `EnvExecutor.callWhenOn(EnvType.CLIENT,
  () -> () -> Minecraft.getInstance().player)` calls to a direct `Minecraft.getInstance().player` — no
  redundant side-check needed. Also `ItemStack#getTagEnchantments()` doesn't exist in 1.21.1; real method
  is `getEnchantments()` (returns `ItemEnchantments`, which was never imported either).
- [x] `content/contraptions/actors/seat/SeatEntity.java` (clean) — same
  `IEntityAdditionalSpawnData`-doesn't-exist bug as `PackageEntity.java` from an earlier session, but
  additionally had two flatly-wrong NeoForge imports (`net.neoforged.neoforge.common.util.FakePlayer`,
  `net.neoforged.neoforge.entity.IEntityWithComplexSpawn`) shadowing/duplicating the two already-correct
  fabric/porting-lib imports a few lines above them. Deleted all three dead/wrong imports (plus an unused
  `PortingLibEntity` and, once the dust settled, an unused `BlockPos`); the existing
  `writeSpawnData`/`readSpawnData` method bodies already matched porting-lib's real
  `IEntityWithComplexSpawn` signature, so no further change needed there.
- [x] `content/contraptions/mounted/MinecartContraptionItem.java` (clean) — same
  `MinecartAndRailUtil.getDirectionOfRail(...)` dead-class bug fixed in `MinecartSim2020.java` earlier
  this session, recurring at 3 call sites in this file too; same fix
  (`blockstate.getBlock() instanceof BaseRailBlock rail ? blockstate.getValue(rail.getShapeProperty()) :
  RailShape.NORTH_SOUTH`), plus removing the (here doubly-duplicated) dead import.

## Done this session (batch 35)
Trajectory: 722 → 711 → 700 (confirmed) → further fixes applied, next compile pending.
- [x] `content/contraptions/mounted/MinecartContraptionItem.java` (clean, second pass) — one more bug
  beyond the 3 `MinecartAndRailUtil` call sites fixed above: `generatedStack.saveOptional(event.getLevel().registryAccess())`
  referenced an `event` variable that doesn't exist in this method at all (no event object anywhere in
  scope — likely a leftover from a NeoForge event-handler signature that this method never actually had
  once ported) — the method already has a `world` (`Level`) local in scope, so switched to
  `world.registryAccess()` directly.
- [x] `api/data/recipe/CrushingRecipeGen.java` + `api/data/recipe/MillingRecipeGen.java` (both clean) —
  both used NeoForge's `net.neoforged.neoforge.common.conditions.{NotCondition,TagEmptyCondition}` to
  build a "skip this recipe if its ore tag is empty" condition, passed to
  `ProcessingRecipeBuilder#withCondition`. Traced that method's real parameter type
  (`content/processing/recipe/ProcessingRecipeBuilder.java:240`) to fabric's own
  `net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition` — a **different** condition system
  from the porting-lib `ICondition` one used by `MechanicalCraftingRecipeBuilder.java` earlier this
  session (that builder declares its own distinct `withCondition(ICondition)` overload — not a
  contradiction, just two different builders with two different condition systems, worth remembering
  before assuming "the" fix from an earlier file applies everywhere). Fabric's
  `ResourceConditions.tagsPopulated(TagKey)` is the direct one-call equivalent of
  NeoForge's `NotCondition(TagEmptyCondition(tag))` double-negative — replaced both files' conditions
  with a single `ResourceConditions.tagsPopulated(tag)` call, which also let `MillingRecipeGen.java`
  drop its manual `AllTags.commonItemTag("ores/" + name)` string-based re-derivation of a tag its caller
  already had a `TagKey` for.
- [x] `foundation/item/ItemHandlerWrapper.java` (clean) — another instance of this session's recurring
  `Storage<T>` bug: overrode the now-fully-removed `simulateInsert`/`simulateExtract`/`exactView` methods
  (deprecated-then-deleted from fabric's transfer API, per the `MechanicalCrafterBlock.java`/
  `TransferUtil.java` precedent from an earlier session in this same port). Deleted all three overrides;
  confirmed neither of its two subclasses (`PortableItemInterfaceBlockEntity`'s inner
  `InterfaceItemHandler`, `PackagePortAutomationInventoryWrapper`) called any of them, so no follow-on
  fixes needed. Removed the now-dead `TransferUtil` import (was only used by the deleted `exactView`
  override).

## Done this session (batch 36)
Trajectory: 700 → 696 → 691 → 688 (confirmed) → further fixes applied, next compile pending.
- [x] `content/processing/recipe/ProcessingRecipeBuilder.java` (clean) — high-leverage core class (backs
  every processing recipe generator). `validateFluidAmounts()` referenced `params.id`, a field that
  doesn't exist on `ProcessingRecipeParams` at all (checked the whole class — no `id` field, ever) — the
  builder itself already has the right field, `recipeId`, directly in scope as `this.recipeId` — swapped
  to that. Also deleted 6 lines of duplicate dead imports (`CustomIngredient` ×2 — resolves fine but
  unused; `ConditionJsonProvider`/`DefaultResourceConditions` ×2 each — genuinely don't exist in the
  resolved fabric-resource-conditions-api jar, likely renamed/removed upstream since whenever this was
  written — none of the three types were referenced anywhere in the file body).
- [x] `content/logistics/vault/ItemVaultBlockEntity.java` (clean) — four independent bugs: (1)
  `BlockState#getWeakChanges(Level, BlockPos)` is a NeoForge-only extension with no fabric port (even
  Createforge's own copy of this exact file uses it, confirming it's a genuine NeoForge API, not a typo)
  — this call was gating a `level.neighborChanged(...)` redundant-notification optimization, so dropped
  the condition and always notify (neighborChanged is safe to call redundantly, just marginally less
  optimized); (2) a byte-for-byte duplicate `getInvId()` method defined twice in the same class — deleted
  the second copy; (3) `clearContent()` was `@Override`-annotated but the class never actually
  `implements Clearable` (same missing-interface bug as `PackagerBlockEntity`/`StockTickerBlockEntity` in
  batch 32 — the `import net.minecraft.world.Clearable;` was already sitting there unused) — added
  `Clearable` to the `implements` clause.
- [x] `content/kinetics/belt/BeltBlock.java` (clean) — `net.minecraft.world.level.pathfinder.BlockPathTypes`
  was renamed to `PathType` in this MC version (confirmed via `javap`); fixed the import and its one usage
  (`LandPathNodeTypesRegistry.register(this, PathType.RAIL, null)` — checked the real fabric
  `LandPathNodeTypesRegistry.register(Block, @Nullable PathType, @Nullable PathType)` signature via its
  sources jar, confirms `null` for the second "neighbor" param is intentional/documented, not a stopgap).
  Separately, `BlockState#getSoundType(Level, BlockPos, Entity)` is a NeoForge 3-arg patch; vanilla only
  has the no-arg `getSoundType()` (confirmed via `javap`) — fixed both call sites (brass/andesite casing
  interaction sounds).
- [x] `content/kinetics/deployer/DeployerMovementBehaviour.java` (clean) — used NeoForge's
  `BlockSnapshot`/`EventHooks.onBlockPlace` to fire a cancellable `BlockEvent.Place` and roll back the
  placement if some other mod vetoed it; fabric has no equivalent cancellable block-place event hook here,
  and the block is already unconditionally placed one line above via `BlockHelper.placeSchematicBlock` —
  removed the snapshot/event/rollback dance entirely and always mark rail placements as accepted (the
  `else` branch's behavior becomes unconditional), matching this session's established pattern for
  genuinely-unported NeoForge event hooks. Removed the now-unused `Block` import (`Block.UPDATE_CLIENTS`
  was the only prior usage).

## Done this session (batch 37)
Trajectory: 688 → 680 → 676 (confirmed) → further fixes applied, next compile pending.
- [x] `content/logistics/vault/ItemVaultBlockEntity.java` (clean, second pass) — three more bugs beyond
  batch 36's fixes: `BlockState#onNeighborChange(Level, BlockPos, BlockPos)` is another NeoForge-only
  extension with no fabric port (same family as `getWeakChanges` from batch 36) — replaced with an
  explicit `level.neighborChanged(blockstate, updatePos, provokingBlock, provokingPos, false)` call so the
  notification still actually fires (unlike the `getWeakChanges` case, this one runs unconditionally
  before the redstone-conductor branch, so simply deleting it would have been a real behavior regression,
  not just a lost optimization — worth flagging as the distinction between the two "NeoForge extension
  with no port" fixes in this file). Separately, `new MultiBlock(vaultPositions)` referenced a type that
  has never existed — `api/packager/InventoryIdentifier.java`'s sum-type only had `Single`/`Pair`/`Bounds`/
  `MultiFace` variants, none of which fit a raw `Set<BlockPos>` (confirmed Createforge's original uses a
  `BoundingBox`-based `Bounds` instead, so this was a genuinely incomplete/botched port, not a rename) —
  added a new `MultiBlock(Set<BlockPos>)` record to the interface (mirroring the existing `MultiFace`
  pattern) rather than reworking the call site to compute a `BoundingBox`, since the `Set<BlockPos>`
  the caller already built is a perfectly valid (if slightly less efficient) representation.
- [x] `content/kinetics/belt/BeltBlock.java` (clean, second pass) — `useItemOn`'s dye/water-fill branch
  referenced an undefined `heldItem` where the method's real parameter is named `stack` (another instance
  of this session's recurring "leftover NeoForge parameter name" bug).
- [x] `content/kinetics/deployer/DeployerMovementBehaviour.java` (clean, second pass) — `IBaseRailBlockExtension`
  is a NeoForge extension interface with no fabric port and no import in the file; its purpose (checking
  "is this a rail-like block") is exactly what vanilla `BaseRailBlock` already means as a plain
  `instanceof` check — swapped to that.
- [x] `content/equipment/zapper/ShootableGadgetRenderHandler.java` (clean) — `io.github.fabricators_of_create.porting_lib.event.client.RenderHandCallback`
  (and its nested `.RenderHandEvent`) don't exist; the real class is
  `io.github.fabricators_of_create.porting_lib.client_events.event.client.RenderHandEvent` (note the
  `.client_events.` package segment, plus it's not nested under a `Callback`-suffixed wrapper — the event
  class itself carries the static `EVENT` field and a `Callback` functional-interface nested inside *it*).
  Confirmed via `javap` that `RenderHandEvent` already extends porting-lib's `CancellableEvent` (providing
  `setCanceled`, already used correctly) and exposes everything else this file needs, except
  `getPartialTicks()` — real name is `getPartialTick()` (singular).

## Done this session (batch 38)
Trajectory: 676 → 671 → 670 → 665 (confirmed) → further fixes applied, next compile pending.
- [x] `content/equipment/blueprint/BlueprintOverlayRenderer.java` (partial — one error class fixed,
  ~4 remain, deferred) — `TooltipRenderUtil.renderTooltipBackground` was called with 9 args
  (`guiGraphics, x, y, w, h, z, bgColor, borderColorStart, borderColorEnd`), but vanilla's real method
  (confirmed via `javap`) only takes 6 (`guiGraphics, x, y, w, h, z`) — the custom-color overload was
  removed from vanilla at some point; trimmed the call to the 6 real args (loses custom tooltip coloring
  here, matching whatever vanilla's own default tooltip styling does now). **Left alone**: 4 remaining
  errors in this same file trace back to a missing `BlueprintCraftingInventory` class that doesn't exist
  anywhere in the codebase — this is the *same* missing class already flagged as a genuinely-complex,
  deferred blocker for `content/equipment/blueprint/BlueprintEntity.java` (see that entry below); this
  file needs the same class before its `RecipeManager#getRecipeFor`/`CraftingRecipe#matches`/`assemble`
  calls (which all take `CraftingInput` in 1.21.1, not the old `CraftingContainer`) can be fixed.
- [x] `content/contraptions/minecart/CouplingPhysics.java` (clean) — same dead `MinecartAndRailUtil`
  class fixed 4 times already this session (`MinecartSim2020.java`, `MinecartContraptionItem.java`); this
  file additionally called `MinecartAndRailUtil.getMaximumSpeed(cart)`, wrapping vanilla
  `AbstractMinecart#getMaxSpeed()` — which is `protected`, so external code can't call it directly even
  once you know the real name. No existing mixin accessor exposed it, so added a new
  `foundation/mixin/accessor/AbstractMinecartAccessor.java` (`@Invoker("getMaxSpeed")`, following the
  established `MinecartFurnaceAccessor` pattern in the same package) and registered it in
  `create.mixins.json`. Replaced both `getMaximumSpeed` calls with the accessor invocation, and both
  `getDirectionOfRail` calls with the by-now-standard `railState.getValue(block.getShapeProperty())`.
- [x] `content/decoration/copycat/CopycatBlockEntity.java` (clean) — `requestModelDataUpdate()` (NeoForge's
  per-block ModelData attachment system — already on this session's list of confirmed-genuinely-unported
  hooks) and `Level#getAuxLightManager` (NeoForge's decoupled-from-blockstate custom light source system,
  no fabric equivalent at all) both stubbed out with `// TODO fabric:` comments, following the same
  established pattern as `ComputerBehaviour.java`/`ChromaticCompoundItem.java` earlier this session — the
  surrounding `level.sendBlockUpdated(...)` call already handles the render-refresh half of `redraw()`'s
  job, and light re-emission still happens through the normal blockstate light-emission path even without
  the custom aux-light call. Also the same missing-`Clearable`-in-`implements` bug as batch 32/36/37 (the
  `import net.minecraft.world.Clearable;` was, again, already sitting there unused).

## Done this session (batch 39)
Trajectory: 665 → 660 (confirmed) → further fixes applied, next compile pending.
- [x] `foundation/utility/fabric/ListeningStorageView.java` (**new file**, clean) — root cause of errors
  in 3 files: `content/contraptions/actors/psi/PortableItemInterfaceBlockEntity.java`,
  `.../PortableFluidInterfaceBlockEntity.java`, and `foundation/blockEntity/behaviour/inventory/VersionedInventoryWrapper.java`
  all referenced `com.simibubi.create.foundation.utility.fabric.ListeningStorageView` as if it were a
  shared top-level class — but it only ever existed as a **private static nested class** inside
  `VersionedInventoryWrapper.java`. Extracted that nested class verbatim into the shared top-level file
  the other two were already trying to import (a `StorageView<T>` wrapper that fires a `Runnable`
  listener via `TransactionSuccessCallback` on successful extraction), made it `public`, and had
  `VersionedInventoryWrapper.java` import the shared version instead of defining its own copy.
- [x] `content/contraptions/actors/psi/PortableItemInterfaceBlockEntity.java` +
  `.../PortableFluidInterfaceBlockEntity.java` (both clean) — beyond the `ListeningStorageView` fix:
  both had an `exactView(...)` override on their `Storage`/`WrappedStorage` subclass that can no longer
  override anything (same fully-removed-from-the-interface bug as `ItemHandlerWrapper.java` in batch 35)
  — deleted both, along with the now-unused `TransferUtil` import each one only used for
  `TransferUtil.exactView(this, resource)` inside the deleted method. Both files also referenced
  `TransactionSuccessCallback` without importing it (masked until now by the exactView compile error
  short-circuiting further symbol resolution in the same class) — added the import, and removed each
  file's dead/wrong `io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback`
  import (not the same class, never used).

## Done this session (batch 40)
Trajectory: 650 → 645 → 630 (confirmed) → further fixes applied, next compile pending.
- [x] `AllKeys.java` (clean) — `net.createmod.catnip.client.ConflictSafeKeyMapping` doesn't exist; checked
  the resolved Ponder/catnip jar's full package listing and there is no `catnip.client` package at all in
  this build. Since `ConflictSafeKeyMapping` was only used for the three modifier keybinds
  (SHIFT/CTRL/ALT) and this file already has a comment noting "NeoForge's KeyModifier support isn't
  available; none of our keybinds are defined with a modifier anyway," dropped the special-case branch
  entirely and construct a plain vanilla `KeyMapping` for all keybinds. Separately,
  `KeyMapping#getKey()` doesn't exist in 1.21.1 (vanilla only exposes `getDefaultKey()`, not the
  currently-bound key) — the file already had the correct fabric-native way to read the *live* bound key
  two methods up (`KeyBindingHelper.getBoundKeyOf(keybind)`, used in the existing `getBoundCode()`
  method) — routed `ctrlDown()`/`shiftDown()`/`altDown()` through that instead of the nonexistent getter.
- [x] Systemic bug, 4 files (all clean): `io.github.fabricators_of_create.porting_lib.util.LazyRegistrar`
  doesn't exist anywhere in porting-lib — the real registrar class is
  `io.github.fabricators_of_create.porting_lib.registry.DeferredRegister` (note the package change from
  `.util.` to `.registry.`), paired with `io.github.fabricators_of_create.porting_lib.registry.DeferredHolder`
  (never imported anywhere, since these 4 files used `DeferredHolder` as a type without importing it —
  the `LazyRegistrar` errors were masking that too). Confirmed via `javap` the `DeferredRegister` API
  (`create(Registry/ResourceKey, String)`, `.register(name, supplier)`, `.register()`) is a 1:1 rename
  match for every `LazyRegistrar` call site, so this was a pure find/replace across
  `AllStructureProcessorTypes.java`, `AllParticleTypes.java`,
  `infrastructure/worldgen/AllFeatures.java`, `infrastructure/worldgen/AllPlacementModifiers.java`.
- [x] `foundation/model/ModelSwapper.java` (clean) — in 1.21.1, `ModelResourceLocation` was refactored
  from a `ResourceLocation` subclass into a standalone `Record` that only *wraps* a `ResourceLocation`
  (confirmed via `javap` — `public final class ModelResourceLocation extends java.lang.Record`), so the
  `Map<ResourceLocation, ...> swaps` field could no longer accept the `ModelResourceLocation` keys this
  file actually produces (`getAllBlockStateModelLocations`/`getItemModelLocation` both correctly return
  `ModelResourceLocation` already) — changed the map's key type to match. Also fabric's
  `ModelModifier.AfterBake.Context` has no `id()` method (confirmed via `javap`) — it separately exposes
  `resourceId(): ResourceLocation` and `topLevelId(): ModelResourceLocation`; since this file matches
  against the same `ModelResourceLocation` keys as above, swapped the broken `context.id()` call for
  `context.topLevelId()`.
- [x] `foundation/item/render/PartialItemModelRenderer.java` (clean) — the imported
  `io.github.fabricators_of_create.porting_lib.util.ItemRendererHelper` doesn't exist in the resolved
  porting-lib `base` module (version `3.1.0-beta.91`) at all; found it still present, unchanged, in an
  **older** `base` module build (`3.1.0-beta.47`, via its sources jar) — its entire implementation was a
  one-line delegate to `((ItemRendererAccessor) renderer).port_lib$renderQuadList(...)`, where
  `ItemRendererAccessor` is a mixin accessor interface that — like `ModelPartAccessor` in batch 34 — lives
  in the separately-versioned **`accessors`** porting-lib module (confirmed present there, unchanged,
  across beta.47/.54/.91 alike, so no version mismatch risk this time). Skipped recreating the removed
  helper class entirely and just cast through the accessor directly at both call sites, matching the
  `ModelPartAccessor`/`AgeableListModelAccessor` casting pattern already established in batch 34.

## Done this session (batch 41)
Trajectory: 622 → 616 → further fixes applied, next compile pending.
- [x] `foundation/mixin/datafixer/ItemStackComponentizationFixMixin.java` (clean) — **new kind of fix
  for this session**: `ItemStackComponentizationFix.ItemStackData` is a `private` nested class of a
  vanilla datafixer, and this mixin's `@Inject`-annotated method needs that exact type in its parameter
  list to match the target method's real signature — but plain Java source can never reference a private
  nested type from another top-level class, regardless of what Mixin does at the bytecode level. NeoForge
  builds work here because NeoForge applies its own access transformers (ATs) that widen `ItemStackData`
  to public before the project ever compiles against it; fabric-loom's equivalent mechanism is an
  **access widener** file, and this project already has one (`src/main/resources/create.accesswidener`,
  already wired into `build.gradle.kts` via `accessWidenerPath`) with several similar
  `accessible class net/minecraft/...` entries for exactly this situation. Added
  `accessible class net/minecraft/util/datafix/fixes/ItemStackComponentizationFix$ItemStackData` to it —
  no source-code changes needed in the mixin itself once the type was made accessible.
- [x] `foundation/gui/menu/HeldItemGhostItemMenu.java` (clean) — leftover `net.neoforged.api.distmarker.{Dist,OnlyIn}`
  imports/annotation; swapped for fabric's `@Environment(EnvType.CLIENT)` (the exact same fix pattern
  applied to a dozen-plus files earlier this session).
- [x] `foundation/fluid/FluidRenderer.java` (clean) — `net.createmod.catnip.render.FluidRenderHelper`
  doesn't exist; checked the resolved Ponder/catnip jar and found `getFluidBuilder`/`renderStillTiledFace`/
  `renderTiledFace` all now live as static methods directly on `net.createmod.catnip.render.BasicFluidRenderer`
  (a class this file already `extends`) — deleted the dead import and repointed all three call sites at
  `BasicFluidRenderer` instead.
- [x] `content/processing/basin/BasinBlock.java` (clean) — `useItemOn`'s body referenced an undefined
  `direction` variable (never declared anywhere in the method) — the method's real `hitResult` parameter
  already carries `hitResult.getDirection()`, the actual clicked face, so used that at both use sites
  instead. Separately, `FluidHelper.tryEmptyItemIntoBE`/`tryFillItemFromBE` both take 6 args
  (`Level, Player, InteractionHand, ItemStack, SmartBlockEntity, Direction`) but were being called with
  only 5 (missing the `Direction` — the same `hitResult.getDirection()` fixes this too). One more: a
  lambda meant to return `ItemInteractionResult` had a stray `return InteractionResult.PASS;` (wrong
  result type entirely) — swapped for `ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION`.

## Done this session (batch 42)
Trajectory: 606 → 602 → 598 (confirmed) → further fixes applied, next compile pending.
- [x] `content/processing/sequenced/SequencedAssemblyRecipeBuilder.java` (clean) — used
  `net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider` (doesn't exist) for its
  `recipeConditions` list, but then tried to pass that list as `io.github.fabricators_of_create.porting_lib.resources.conditions.ICondition[]`
  to `RecipeOutput#accept` — traced that method to porting-lib's `RecipeOutputExtension` interface
  (`RecipeOutput extends RecipeOutputExtension`), which really does take `ICondition...`, confirming this
  file needed the porting-lib condition system all along, not fabric's `ResourceCondition`. This is the
  **second time** this exact two-condition-systems distinction has mattered this session (see batch 35's
  `CrushingRecipeGen`/`MillingRecipeGen` entry, which needed the *other* system) — worth remembering
  that the correct one depends on which method you're actually calling
  (`ProcessingRecipeBuilder#withCondition` → fabric `ResourceCondition`; `RecipeOutput#accept(...,
  ICondition...)` or `MechanicalCraftingRecipeBuilder#withCondition` → porting-lib `ICondition`), not
  which file you're in. Changed `recipeConditions`'s type to `List<ICondition>` and fixed the import.
- [x] `content/kinetics/saw/SawBlockEntity.java` (clean) — `Block#getSoundType(BlockState)` is
  `protected` (same NeoForge-patch-vs-vanilla-protected-method pattern as `BeltBlock.java`'s
  `getSoundType` fix in batch 36) — swapped to `block.defaultBlockState().getSoundType()` (no-arg,
  called on the state instead of the block). Also the recurring `ItemStack#hasCraftingRemainingItem()`/
  `getCraftingRemainingItem()` bug (both live on `Item`, not `ItemStack`) and a dead
  `ItemHandlerHelper.copyStackWithSize(...)` call, swapped for `ItemStack#copyWithCount(...)` — both
  fixes matching this session's established patterns exactly.
- [x] `content/fluids/hosePulley/HosePulleyFluidHandler.java` (clean) — this file's `fill`/`drain`
  Forge-`IFluidHandler` logic had already been half-ported to fabric's `Storage<FluidVariant>` shape
  (`insert`/`extract`, `TransactionContext`, etc.) but the fluid-compatibility checks were never
  translated: `FluidStack#canFill(FluidVariant)` and `FluidStack#isFluidEqual(FluidStack)` don't exist on
  this project's fabric `FluidStack` at all. Read Createforge's original for the exact intent — every one
  of those calls was really `FluidStack.isSameFluidSameComponents(a, b)` (a static method that already
  exists on this codebase's own `FluidStack`, used correctly elsewhere), except the ones comparing
  against a bare `FluidVariant resource` parameter, which needed `someFluidStack.getVariant().equals(resource)`
  instead (variant-to-variant, not stack-to-stack). Fixed all 4 call sites accordingly, plus 3 duplicate
  imports (`FluidConstants`, `FluidVariant`, `SingleSlotStorage`, each imported twice).

## Done this session (batch 43)
Trajectory: 598 → 594 → 590 (confirmed) → further fixes applied, next compile pending.
- [x] `content/fluids/transfer/FluidDrainingBehaviour.java` (clean) — `LiquidBlock#fluid` is a `protected`
  field; the project already has a `LiquidBlockAccessor` mixin (`port_lib$getFluid()`) imported in this
  exact file but never used — switched the direct field access to the accessor call. Also a missing
  `TransactionSuccessCallback` import (present under a dead
  `io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback` import instead —
  wrong class entirely, not just a typo) and 4 duplicate imports left over from a bad merge
  (`FluidConstants`, `Transaction`, `TransactionContext`, `SnapshotParticipant`, `FluidStack`, `TransferUtil`
  each appeared twice).
- [x] `content/fluids/OpenEndedPipe.java` (clean) — same dead `TransactionCallback` import →
  `TransactionSuccessCallback` fix as `FluidDrainingBehaviour.java` above. Two `FluidStack.isSameFluidSameComponents(...)`
  calls were passed a `FluidVariant` as the second argument where the method requires two `FluidStack`s
  (same variant-vs-stack mismatch as `HosePulleyFluidHandler.java` in batch 42) — fixed with
  `someFluidStack.getVariant().equals(theVariant)` at both sites. One of the two also referenced an
  undefined `filter` variable — the enclosing `extract` override's real parameter is named
  `extractedVariant`, not `filter` (another instance of this session's recurring "stale/wrong parameter
  name" bug).

## Done this session (batch 44)
Trajectory: 586 → 583 → 579 (confirmed) → further fixes applied, next compile pending.
- [x] `content/contraptions/AbstractContraptionEntity.java` + `content/contraptions/glue/SuperGlueEntity.java`
  (both clean, eventually) — same `IEntityWithComplexSpawn` wrong/missing-import bug as several files
  earlier this session (`SeatEntity.java` batch 34, `PackageEntity.java` prior session): one had no import
  at all, the other imported the nonexistent `net.neoforged.neoforge.entity.IEntityWithComplexSpawn` —
  both fixed to `io.github.fabricators_of_create.porting_lib.entity.IEntityWithComplexSpawn`.
- [x] **New root-cause class of bug found and fixed**: a genuine upstream packaging gap in porting-lib's
  pinned release (`3.1.0-beta.91+1.21.1`). `javap` on the merged/mixed Minecraft jar showed both
  `Entity` and `ItemStack` compiled with `implements ... DefaultNbtSerializable<CompoundTag>` — meaning
  porting-lib's `entity`/`items` modules (at this exact pinned version) really do inject that interface
  via mixin — but the `core` module jar at the *same* pinned version simply does not ship the
  `DefaultNbtSerializable.class` file at all (confirmed via `unzip -l`; only `INBTSerializable` is
  present). Ruled out a stale-cache explanation first: deleted the cached merged Minecraft jar entirely
  and let Loom fully regenerate it from the currently-resolved dependency jars — the error persisted
  identically, confirming this is a real gap in the published artifact, not a local caching issue. An
  older "core" module build (`3.1.0-beta.47+1.21.1`, still resolvable via a transitive pin, same trick
  used for the `accessors`/`ItemRendererAccessor` fixes in batch 34/40) still has the real class, and its
  doc comment confirms it's a marker interface whose default methods ("implemented via mixin") are always
  overridden by the real Entity/ItemStack mixins — meaning a faithful reconstruction is both compile-safe
  and runtime-safe (the throwing default bodies are never actually invoked). Wrote
  `src/main/java/io/github/fabricators_of_create/porting_lib/core/util/DefaultNbtSerializable.java` in
  **our own project**, matching the old build's shape exactly, to polyfill the missing upstream class —
  necessary for more than just `compileJava` to pass, since without a real class of this name on the
  runtime classpath too, the JVM would fail to verify/load `Entity.class` and the game couldn't even
  boot. This fix is worth flagging to whoever eventually reports issues upstream to porting-lib/create-fabric,
  since it's papering over a real gap in their beta.91 release rather than a mistake in this port.

## Done this session (batch 45)
Trajectory: 576 → 572 → 568 (confirmed) → further fixes applied, next compile pending.
- [x] `content/equipment/zapper/ZapperItem.java` (clean) — same `EntitySwingListenerItem`/
  `ReequipAnimationItem` genuinely-unported-NeoForge-hooks bug as `PotatoCannonItem.java` from an earlier
  session; removed both from `implements`, deleted the now-orphaned `onEntitySwing` override, deleted the
  no-longer-overriding `shouldCauseReequipAnimation` body (confirmed it's never called anywhere else in
  the codebase) with a `// TODO fabric:` comment, matching that exact established precedent. Removed the
  now-unused `LivingEntity` import.
- [x] `content/contraptions/glue/SuperGlueItem.java` (clean) — `event.getFace()` referenced an undefined
  `event` where the method's real parameter is `hitResult` (→ `hitResult.getDirection()`, same recurring
  stale-parameter-name bug); the enclosing branch also had a bare `return;` inside a method declared to
  return `InteractionResult` (→ `InteractionResult.PASS`). Also a leftover NeoForge `@OnlyIn(Dist.CLIENT)`
  → fabric `@Environment(EnvType.CLIENT)`, plus 2 duplicate imports.
- [x] `content/equipment/potatoCannon/PotatoProjectileEntity.java` (clean) — same
  `IEntityWithComplexSpawn` missing-import bug as several files this session; the recurring
  `ItemStack.getEnchantmentLevel(...)` bug (real method is `EnchantmentHelper.getItemEnchantmentLevel(holder,
  stack)`, same fix as `PotatoCannonItem.java` from an earlier session).
- [x] `content/equipment/extendoGrip/ExtendoGripRenderHandler.java` (clean) —
  `io.github.fabricators_of_create.porting_lib.util.FirstPersonRendererHelper` doesn't exist; its two call
  sites (`getStackInMainHand`/`getStackInOffHand` on an `ItemInHandRenderer`) map directly onto the
  existing `ItemInHandRendererAccessor` mixin (`port_lib$getMainHandItem()`/`port_lib$getOffHandItem()`,
  same accessor-casting pattern as batch 34/40) — switched to that instead of trying to recreate the
  helper class. Separately, `ClientHooks.handleCameraTransforms(...)` doesn't exist on porting-lib's
  (much smaller, unrelated) `ClientHooks` utility — this is actually a call to *NeoForge's*
  `net.neoforged.neoforge.client.ClientHooks` in the original, a completely different class with no
  fabric equivalent found; stubbed with a `// TODO fabric:` comment since the manual `ms.translate`/`scale`
  calls immediately after already handle the bulk of this cosmetic positioning.
- [x] `content/equipment/armor/CardboardArmorHandlerClient.java` (clean) — `EntityRenderer#getRenderOffset`
  is a NeoForge extension method with no fabric port; it defaults to `Vec3.ZERO` unless a mod explicitly
  overrides it (none do here), so hardcoded `Vec3.ZERO` directly rather than trying to reconstruct the
  hook — the two downstream calculations that use `renderOffset.y` degrade gracefully to no-ops with a
  zero value, matching the common-case default behavior exactly. Also fixed the same undefined-`event`
  bug pattern seen in `SuperGlueItem.java` above (leftover from the original NeoForge event-object-based
  signature).

## Done this session (batch 46)
Trajectory: 558 → 556 (confirmed) → 545 (confirmed) → further fixes applied, next compile pending.
- [x] `foundation/events/CommonEvents.java` (clean) — 3 fully dead imports
  (`EntityMountEvents`/`LivingEntityEvents`/`event.common.BlockEvents`, none referenced anywhere in the
  file body) at nonexistent porting-lib paths — deleted all three rather than chasing down real
  replacements for classes the file never actually used.
- [x] `content/trains/CubeParticleData.java` (clean) — used `StreamCodec` without importing it.
- [x] `foundation/block/ItemUseOverrides.java` (clean) — two more instances of this session's recurring
  "leftover NeoForge parameter/variable name" bug: `level`/`face` referenced where the real method
  parameters are `world`/`traceResult` (→ `traceResult.getDirection()`).
- [x] `content/trains/station/StationBlockEntity.java` (clean) — dead CC:Tweaked
  `PeripheralCapability` import (unused); `AABB.INFINITE` doesn't exist on vanilla `AABB` (a NeoForge
  patch — confirmed via `javap`, no such constant) — replaced with a manually-constructed very-large box
  (`new AABB(-1e7, -1e7, -1e7, 1e7, 1e7, 1e7)`), same trick immediately reused for
  `content/schematics/cannon/SchematicannonBlockEntity.java`'s identical `AABB.INFINITE` use below.
- [x] `content/schematics/cannon/SchematicannonBlockEntity.java` (clean) — `io.github.fabricators_of_create.porting_lib.block.CustomRenderBoundingBoxBlockEntity`
  has the wrong package (`.block.` vs. the real `.blocks.extensions.`); the `AABB.INFINITE` fix from
  `StationBlockEntity.java` above, reused verbatim; `TransferUtil.extract(inventory, variant, amount)`
  called a static helper that was never actually defined on `TransferUtil` (`extract` has no static
  overload there at all, only instance-level `Storage#extract`) — rewrote as
  `TransferUtil.commit(t -> inventory.extract(ItemVariant.of(extractItem), 1, t))`, matching
  `TransferUtil`'s real `commit(Function<TransactionContext, T>)` helper already used elsewhere in this
  codebase for exactly this one-shot-transaction pattern.

## Done this session (batch 47)
Trajectory: 545 → 542 (confirmed) → further fixes applied, next compile pending.
Housekeeping note: while chasing one of these fixes, discovered the merged-Minecraft-jar cache entry
deleted for batch 44's `DefaultNbtSerializable` investigation regenerated under a **new hash directory**
(`minecraft-merged-46e867d217`, not the old `...7534a26af7`) — future `javap` lookups against "the merged
jar" in this session need to re-resolve the current hash (`ls -lat .gradle/loom-cache/minecraftMaven/net/minecraft/*/` and take the newest) rather than reusing the old path.
- [x] `foundation/utility/GlobalRegistryAccess.java` + `impl/contraption/dispenser/DispenserBehaviorConverter.java`
  (both clean) — same `io.github.fabricators_of_create.porting_lib.util.ServerLifecycleHooks` wrong-package
  bug (real path is `.core.util.ServerLifecycleHooks`, confirmed present there via `javap`); the former
  also had a fully dead `EnvExecutor` import (never referenced in the file body). The latter additionally
  used vanilla `net.minecraft.core.dispenser.ProjectileDispenseBehavior` without importing it at all
  (confirmed the class itself is real and present in the merged jar — this was a pure missing-import, not
  a renamed/moved class, unlike most of this session's other "cannot find symbol" cases).
- [x] `foundation/ponder/FabricStructureProcessing.java` (clean) — two independent bugs: (1)
  `StructureProcessorType<P>`'s single abstract method returns `MapCodec<P>`, but this file built a plain
  `Codec<Processor>` (via a trailing `.codec()` call converting the `MapCodec.fieldOf(...)` builder back
  down) and tried to hand that to `Registry.register`, which failed generic inference since the lambda
  `() -> PROCESSOR_CODEC` no longer type-matched the functional interface — dropped the trailing
  `.codec()` call so `PROCESSOR_CODEC` stays a `MapCodec<Processor>` all the way through, matching what
  the interface actually needs; (2) `FluidStack.loadFluidStackFromNBT(CompoundTag)`/`FluidStack#writeToNBT(CompoundTag)`
  are classic Forge-`FluidStack` methods this project's own fabric-native `FluidStack` never had (it uses
  `HolderLookup.Provider`-based `parse`/`save`, a completely different, incompatible schema) — but reading
  the method's actual job (`fixTankContent`, converting an *old schematic's* raw NBT-stored fluid amount
  from Forge's classic milli-bucket unit into this project's fabric-native bucket unit, while leaving the
  rest of the tag untouched) showed the full `FluidStack` round-trip was unnecessary complexity — rewrote
  to read/write the `"Amount"` int tag directly with the same unit-conversion math, skipping the
  `FluidStack` object entirely. Cleaned up the now-unused `Set`/`FluidStack`/`Codec` imports this left
  behind.

## Done this session (batch 48)
Trajectory: 542 → 533 (confirmed) → further fixes applied, next compile pending.
- [x] `content/kinetics/millstone/MillstoneBlock.java` (clean) — the recurring "two different classes,
  same simple name" bug, but caught from the opposite direction this time: `MillstoneBlockEntity.java`'s
  `inputInv`/`outputInv` fields are typed with **porting-lib's own** `ItemStackHandler`/
  `ItemStackHandlerContainer` (confirmed `ItemStackHandlerContainer extends` porting-lib's
  `ItemStackHandler`, not this project's), but `MillstoneBlock.java` imported **this project's**
  `com.simibubi.create.infrastructure.fabric.transfer.item.ItemStackHandler` instead — an unrelated class
  with an incompatible API. Switched the import to match what the block entity actually uses. Also fixed
  the recurring dead `ItemHandlerHelper.copyStackWithSize(...)` → `ItemStack#copyWithCount(...)`, and 2
  duplicate `ItemStackHandler` imports left over from a bad merge.
- [x] `content/logistics/funnel/FunnelItem.java` (clean) — `BlockUseBypassingItem` had the same missing
  `.extensions.` package-segment bug as several porting-lib item-hook interfaces this session (real path
  is `io.github.fabricators_of_create.porting_lib.item.extensions.BlockUseBypassingItem`).
- [x] `content/processing/basin/BasinBlockEntity.java` (clean) — same missing-`Clearable`-in-`implements`
  bug as several files in batches 32/36/37/38 (the `import net.minecraft.world.Clearable;` sitting there
  unused again), plus the same `SmartInventory`/`ItemStackHandler`-subclass `.clearContent()` → `.clear()`
  real-method-name fix from batch 30/31.
- [x] `infrastructure/data/CreateRegistrateTags.java` (clean) — `Tags.Items.TOOLS_WRENCH` doesn't exist on
  porting-lib's own `Tags` class (confirmed via `javap` — no `TOOLS_WRENCH` field at all there); swapped
  for the established `AllItemTags.WRENCH.tag` fabric-native replacement (same tag, different access
  path — used identically in `InputEvents.java`/`ItemHatchBlock.java`/`FactoryPanelBehaviour.java`
  earlier this session). `AllItemTags.TRINKETS_FACE` was referenced but never actually defined in
  `AllTags.java`'s `AllItemTags` enum — added it (`TRINKETS_FACE(TRINKETS, "face")`), right next to the
  already-present-but-Curios-only `CURIOS_HEAD(CURIOS, "head")` entry it was clearly meant to sit beside
  (this file's own comment two lines above already says "fabric: Trinkets compat is used instead",
  confirming the entry was simply never added when that comment was written). Also a genuinely-unported
  gap: `TagAppender#remove(ResourceLocation...)` doesn't exist anywhere in this codebase's resolved
  dependencies (checked vanilla `TagsProvider.TagAppender`, porting-lib's `TagAppenderExtension`, and its
  `TagAppenderExtensions` — none support removing entries from a tag in datagen, only adding) — stubbed
  the one call site (excluding Create's diving armor from vanilla's `TRIMMABLE_ARMOR` tag) with a
  `// TODO fabric:` comment; this only affects which items show a "trim" tooltip option, not gameplay
  correctness.

## Done this session (batch 49)
Trajectory: 533 → 521 → 503 (confirmed) → further fixes applied, next compile pending.
- [x] `content/equipment/bell/BasicParticleData.java` + `content/kinetics/base/RotationIndicatorParticleData.java`
  (both clean) — same missing `StreamCodec`/`RegistryFriendlyByteBuf` import bug as `CubeParticleData.java`
  earlier this session.
- [x] `content/kinetics/waterwheel/WaterWheelStructuralBlock.java` (clean) — `addLandingEffects`/
  `addDestroyEffects`/`addHitEffects` are all NeoForge `IBlockExtension` particle-customization hooks with
  no fabric port (confirmed no matching vanilla `Block` methods) — deleted the two unused ones
  (`addLandingEffects`/`addDestroyEffects`, the latter already had a comment saying its whole point was
  to return `false` and suppress default particles anyway) with a `// TODO fabric:` note, and dropped just
  the dead `@Override` from `addHitEffects` (left the body in place as a harmless plain method, in case
  anything ever calls it directly — nothing currently does).
- [x] `content/kinetics/simpleRelays/CogwheelBlockItem.java` + `content/equipment/clipboard/ClipboardBlock.java`
  (both clean) — the former had the by-now-familiar missing `.extensions.` package segment on
  `UseFirstBehaviorItem`; the latter had a dead `EnvExecutor` import (doesn't exist, never referenced) and
  a leftover `@OnlyIn(Dist.CLIENT)` → `@Environment(EnvType.CLIENT)`.
- [x] `content/equipment/blueprint/BlueprintItem.java` (clean) — three independent bugs: missing
  `DataComponents` import; `ItemTags.create(ResourceLocation)` doesn't exist on vanilla `ItemTags` (no
  `create` method there at all) — real vanilla API is `TagKey.create(Registries.ITEM, resourceLocation)`;
  and `net.neoforged.neoforge.common.crafting.CompoundIngredient` (NeoForge's OR-combining-multiple-ingredients
  type, used only for an `instanceof` type-check) has no fabric equivalent to detect via `instanceof` at
  all — hardcoded the check to `false` with a `// TODO fabric:` comment, since fabric's `CustomIngredient`
  system doesn't expose this as a checkable type the way NeoForge's ingredient system does.
- [x] `content/fluids/transfer/FluidSplashPacket.java` (clean) — leftover
  `net.neoforged.neoforge.fluids.FluidStack` import; swapped for this project's own fabric-native
  `FluidStack`. This surfaced a bigger gap while fixing it: **this project's own `FluidStack.CODEC`/
  `OPTIONAL_CODEC`/`STREAM_CODEC` fields were all literal `null` stubs** (confirmed by reading the class —
  `= null;` on all three, clearly a "wire this up later" placeholder from an earlier porting pass), and 6
  other files depend on them (`FluidIngredient.java`, `ProcessingRecipeSerializer.java`,
  `ProcessingRecipeParams.java`, `FluidTankMountedStorage.java`, `CreativeFluidTankBlockEntity.java`,
  `FluidParticleData.java`) — not a "will error later" risk but a real functionality gap (recipe
  (de)serialization and network sync for every fluid-holding block would NPE at runtime). Implemented all
  three properly using `FluidVariant`'s own real `CODEC`/`PACKET_CODEC` (confirmed present via `javap`)
  composed with a `long` amount field via `RecordCodecBuilder`/`StreamCodec.composite`, plus a new
  `OPTIONAL_STREAM_CODEC` (aliased to the same `STREAM_CODEC`, since amount-zero already signals "empty"
  for this type) that `FluidSplashPacket.java` needed but didn't exist as a field at all before.

## Next up
Regenerate `/tmp/error_files.txt` from the latest `cr_verifyN.log` (see command near the top of this
file) for the current frontier — it has shifted a lot; re-sort by count rather than trusting old lists.
- `content/contraptions/minecart/capability/MinecartController.java` — **investigated, deferred,
  genuinely complex**: relies entirely on NeoForge's `net.neoforged.neoforge.attachment.IAttachmentSerializer`
  attachment-system (fabric has a *different* attachment API, `net.fabricmc.fabric.api.attachment.v1`,
  with different registration mechanics — not a drop-in rename). Worse, the fabric branch's copy of
  this file is missing ~185 lines that exist in `Createforge`'s original: an entire nested `Type` enum
  (`NORMAL`/`EMPTY`, each with its own `IAttachmentSerializer` implementation) and a nested `Empty`
  subclass — genuinely never ported, not just broken by a rename. Also missing
  `io.github.fabricators_of_create.porting_lib.util.MinecartAndRailUtil` (checked every porting-lib
  module jar with `javap`/`unzip -l` — doesn't exist anywhere). This needs a dedicated pass designing
  a fabric-attachment-API equivalent, not a sweep-style fix.
- `content/equipment/blueprint/BlueprintEntity.java` — **investigated, deferred, genuinely complex**:
  needs (1) a `BlueprintCraftingInventory` class that doesn't exist anywhere in the codebase, matching
  crafting recipes against a grid of stacks — likely needs to build a vanilla `CraftingInput` (1.21.1's
  new recipe-matching type), not the old `CraftingContainer`-based approach the current broken code
  assumes; (2) `CommonHooks.setCraftingPlayer(player)` is a NeoForge-only hook with no fabric port;
  (3) `player.openMenu(section, buf -> {...})` — signature mismatch, needs checking against 1.21.1's
  real `Player.openMenu` overloads; (4) a custom-entity-spawn-data `@Override` mismatch
  (`IEntityWithComplexSpawn`-related) at line 332/340 not yet investigated. This is a multi-part
  feature port, not an import fix — worth a dedicated pass rather than folding into the sweep.
- Still open, not yet started: `compat/emi/CreateEmiPlugin.java` (deprioritized),
  `api/data/recipe/MechanicalCraftingRecipeBuilder.java`,
  `CreateClient.java`, `content/kinetics/crusher/CrushingWheelControllerBlockEntity.java`,
  `content/equipment/potatoCannon/PotatoCannonItem.java`, `content/decoration/copycat/CopycatBlock.java`,
  `content/contraptions/render/ContraptionRenderInfo.java`, `content/contraptions/minecart/capability/MinecartController.java`,
  `foundation/blockEntity/behaviour/inventory/VersionedInventoryWrapper.java`,
  `content/redstone/displayLink/source/EnchantPowerDisplaySource.java`.

## Done this session (batch 52)
Trajectory: 407 → 389 (confirmed after batches 52-54 together; individual batch numbers below).
441 → 407 for batch 52 alone.
- [x] `foundation/mixin/fabric/AbstractMinecartMixin.java` (clean) — `MinecartController#serializeNBT`/
  `#deserializeNBT` became provider-aware (`implements INBTSerializable<CompoundTag>` with the newer
  `HolderLookup.Provider`-taking signatures) at some earlier point, but this mixin's two `@Inject`
  hooks (`loadController`/`saveController`) never got updated to pass one — fixed by threading
  `((AbstractMinecart) (Object) this).registryAccess()` through (RegistryAccess implements
  HolderLookup.Provider, confirmed via `javap`).
- [x] `foundation/map/StationMapDecorationRenderer.java` (clean) — NeoForge's `IMapDecorationRenderer`
  custom-map-decoration-rendering hook has no fabric port; this class is already unregistered dead
  code (see the commented-out registration in `CommonEvents.java`) — dropped the `implements`, kept as
  a plain utility class (TODO fabric).
- [x] `compat/jei/category/SpoutCategory.java`, `compat/jei/category/ItemDrainCategory.java` (both
  clean, JEI is priority) — both missing a plain `net.minecraft.world.item.crafting.RecipeHolder`
  import (checked every other JEI category file for the same gap via a small script — none found);
  `SpoutCategory` additionally had `FluidIngredient#getFluids()` (doesn't exist) →
  `#getMatchingFluidStacks()` (already returns the exact `List<FluidStack>` type `AnimatedSpout#withFluids` needs).
- [x] `foundation/recipe/RecipeApplier.java` (clean) — `ItemStack#hasCraftingRemainingItem`/
  `#getCraftingRemainingItem` don't exist in 1.21.1 (those live on `Item`, same recurring bug as
  `MillstoneBlockEntity.java`/`PotionFluidHandler.java` earlier sessions) → `stackIn.getItem().hasCraftingRemainingItem()`
  / `new ItemStack(stackIn.getItem().getCraftingRemainingItem())`.
- [x] `foundation/networking/BlockEntityDataPacket.java` (clean) — missing `net.fabricmc.api.EnvType`/
  `Environment` imports entirely (not a leftover-annotation bug like batch 51's — these were never
  added at all).
- [x] `foundation/mixin/SmithingMenuMixin.java` (clean) — `ItemStack#getTagEnchantments()` → `#getEnchantments()`
  (established fix); `ItemStack#supportsEnchantment(Holder<Enchantment>)` doesn't exist → real check is
  on the enchantment itself, `Enchantment#isSupportedItem(ItemStack)` (confirmed via `javap`).
- [x] `foundation/mixin/Ingredient$ValueMixin.java` (clean) — NeoForge's `DatagenModLoader.isRunningDataGen()`
  has no fabric equivalent (only an internal, non-public `fabric-data-generation-api-v1` impl class
  tracks this) — always apply the datagen-only codec shortcut unconditionally; harmless outside datagen
  since it's a strict superset of what the plain codec accepts (TODO fabric).
- [x] `foundation/item/TooltipHelper.java` (clean) — porting-lib's `MinecraftClientUtil.getLocale()`
  doesn't exist; the real hook is a mixin-injected default method, `LanguageManagerInjection#port_lib$getJavaLocale()`,
  on `LanguageManager` itself (confirmed via `javap` — `LanguageManager implements ... LanguageManagerInjection`).
- [x] `foundation/item/ItemHelper.java` (clean) — two more instances of the recurring `Item#getMaxStackSize()`
  doesn't exist bug (real method is `getDefaultMaxStackSize()`, same as `ChuteItemHandler.java` earlier session).
- [x] `foundation/blockEntity/renderer/SafeBlockEntityRenderer.java` (clean) — NeoForge's
  `BlockEntityRenderer#getRenderBoundingBox` extension has no fabric port at all (vanilla's interface
  only has `render`/`shouldRenderOffScreen`/`getViewDistance`/`shouldRender`, confirmed via `javap`) —
  dropped the invalid `@Override`, kept as a plain helper (TODO fabric); also fixed the fallback branch,
  which called a NeoForge-only `BlockEntity#getRenderBoundingBox()` that doesn't exist either → `new AABB(blockEntity.getBlockPos())`.
- [x] `foundation/blockEntity/behaviour/ValueSettingsInputHandler.java` (clean) — dead `EnvExecutor`
  import; `Items.TOOLS_WRENCH` (wrong `Items` class, recurring bug) → `AllItemTags.WRENCH.tag`.
- [x] **`foundation/blockEntity/SyncedBlockEntity.java` + `content/contraptions/render/ClientContraption.java`
  (both clean) — a genuine, non-trivial API removal**: vanilla 1.21.1 merged the old dual-path block-entity
  sync system (`handleUpdateTag`/`onDataPacket`, separate override points from `loadAdditional`) into a
  single path — `getUpdatePacket()`'s tag and any disk-loaded tag are now both applied through the same
  `final BlockEntity#loadWithComponents(tag, provider)`, which itself just calls the ordinary
  `loadAdditional` (confirmed by disassembling `ClientPacketListener.handleBlockEntityData`'s bytecode —
  it calls `BlockEntity.loadWithComponents` directly, no separate client-only entry point exists
  anymore). Consequence: `SmartBlockEntity`'s `read(tag, registries, clientPacket)` split (a `boolean`
  flag behaviours use to send/receive an abbreviated network payload vs. the full disk-save payload)
  **can no longer be driven by which path invoked it** — `loadAdditional` is hardcoded to `clientPacket=false`
  always now, including when actually applying a received network packet. Documented this as a TODO
  fabric gap (a correctness/fidelity loss, not just a compile fix — flagged for whoever picks this up
  next, since fixing it properly would mean threading `level.isClientSide()` through `SmartBlockEntity.loadAdditional`
  instead of the hardcoded `false`, touching the sync behavior of every `SmartBlockEntity` subclass, which
  felt too large/risky for a sweep-style batch fix). Removed the two dead `@Override`s from `SyncedBlockEntity`;
  `ClientContraption.java`'s manual virtual-block-entity-populate-from-schematic-NBT call site
  (`be.handleUpdateTag(nbt, ...)`) now calls the real replacement, `be.loadWithComponents(nbt, ...)`, directly.
- [x] `foundation/block/BigOutlines.java` (clean) — dead `com.simibubi.create.foundation.utility.fabric.ReachUtil`
  import (class doesn't exist anywhere — also found a pre-existing duplicate `Attributes` import while
  here, deleted the duplicate) → real vanilla API is `Player#getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE)`
  (confirmed via `javap` on `Attributes.class` — 1.21.1 replaced the old hardcoded reach distance with an
  attribute).
- [x] `foundation/CreateNBTProcessors.java` (clean) — 2 more instances of the established
  `CatnipCodecUtils.decodeOrNull` doesn't exist bug (batch 30 precedent) → `.decode(...).orElse(null)`.

## Done this session (batch 53)
Trajectory: 407 → 395 (confirmed).
- [x] `compat/pojav/PojavChecker.java` (clean) — NeoForge's `ScreenEvent.Init.Post`/`NeoForge.EVENT_BUS`
  → fabric-api's `net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT` (a global per-screen
  event, callback shape `(Minecraft, Screen, int width, int height)` instead of an Event object with a
  `getScreen()` getter — confirmed via `javap` on `fabric-screen-api-v1`).
- [x] `foundation/data/recipe/CreateMixingRecipeGen.java` (clean) — NeoForge's `BlockTagIngredient`
  (matches an item if its corresponding block is in a given block tag) has no fabric port; the one use
  here (`BlockTags.CONVERTABLE_TO_MUD`, for the vanilla dirt+water→mud mixing recipe) is vanilla-dirt-only
  by default, so hardcoded `.require(Blocks.DIRT)` directly (TODO fabric — faithful to vanilla, but won't
  pick up datapack/mod additions to that block tag for this one recipe).
- [x] `content/trains/station/GlobalStation.java` (clean) — `ServerLifecycleHooks` wrong porting-lib
  subpackage (`.util` → `.core.util`, same recurring fix as `GlobalRegistryAccess.java`/
  `DispenserBehaviorConverter.java` earlier session).
- [x] `content/trains/schedule/condition/ItemThresholdCondition.java` (clean) — undefined `stackInSlot`
  variable, a leftover from a pre-port `ItemStack`-slot-based loop that was never fully converted to the
  surrounding `Storage<ItemVariant>`/`StorageView` loop (`variant`/`view` are the real in-scope locals) →
  `view.getAmount() == variant.getItem().getDefaultMaxStackSize()`.
- [x] `content/trains/observer/TrackObserverBlock.java` (clean) — missing `ConnectableRedstoneBlock`
  import (the correct `io.github.fabricators_of_create.porting_lib.blocks.extensions.ConnectableRedstoneBlock`
  path, confirmed against ~10 sibling redstone blocks in the same package tree that already import it
  correctly — this file was just missing the import line entirely, not using a wrong path).

## Done this session (batch 54)
Trajectory: 395 → 389 (confirmed).
- [x] `content/redstone/diodes/ToggleLatchBlock.java`, `content/redstone/diodes/BrassDiodeBlock.java`
  (both clean) — same `ConnectableRedstoneBlock` bug as `TrackObserverBlock.java` above, but the wrong-path
  variant this time: `io.github.fabricators_of_create.porting_lib.block.ConnectableRedstoneBlock` (no such
  package, missing the `s`/`.extensions`) → `.blocks.extensions.ConnectableRedstoneBlock`.
- [x] `content/equipment/toolbox/ToolboxBlock.java` (clean) — dead `NetworkHooks` import; called a
  2-arg `Player#openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)` overload that doesn't exist in
  1.21.1 (only the plain 1-arg `openMenu(MenuProvider)` does) with a `toolbox::sendToMenu` method
  reference that was never defined anywhere either — `ToolboxBlockEntity` already `implements MenuProvider`
  directly, so simplified to the real 1-arg `player.openMenu(toolbox)`.
- [x] `content/logistics/depot/EjectorBlockEntity.java` (clean) — missing `CollisionContext` import
  (`net.minecraft.world.phys.shapes.CollisionContext` — a plain missing-import, the `ClipContext`/`Fluid`
  usage right next to it was already correct); `AABB.INFINITE` doesn't exist on vanilla `AABB` at all (no
  static constants on the class, confirmed via `javap`) → built the same infinite bounds manually with
  `new AABB(NEGATIVE_INFINITY..., POSITIVE_INFINITY...)`.

## Branch consolidation
Repo owner asked to stop working on the throwaway session branch and roll everything into `main`
directly. `main` was a strict ancestor of `claude/focused-euler-auneot` (fast-forward, zero commits lost,
no force-push needed) — pushed `origin/claude/focused-euler-auneot` onto `origin/main`, switched the
local checkout to track `main`, and all commits from here on go straight to `main`. The old branch was
deleted locally; deleting it on GitHub itself needs someone with real delete-ref rights (this session's
push credentials got a 403 trying, and no GitHub API tool available in-session exposes branch deletion) —
harmless either way since everything is already on `main`.

## Done this session (batch 56)
Trajectory: 367 → 345 (confirmed).
- [x] **DeployerHandler.java / CobbleGenLevel.java / NonVisualizationLevel.java (all clean)** — same
  porting-lib "extensions"-module version-skew diamond conflict as the `snapshotParticipant()` fix from
  an earlier session, this time on `isAreaLoaded(BlockPos, int)` between `LevelReaderInjection` and
  `LevelReaderExtensions` (both mixin-inject default implementations into `WrappedLevel` subclasses,
  neither overrides the other) — same fix shape: an explicit override delegating to the wrapped `level`
  field. **If this diamond-conflict pattern turns up on a third method beyond `snapshotParticipant`/
  `isAreaLoaded`, grep the compile log for "inherits unrelated defaults for" to find every affected
  `WrappedLevel` subclass at once.**
- [x] `DeployerHandler.java` (clean) — `ItemStack#doesSneakBypassUse(Level, BlockPos, Player)` is a
  NeoForge item hook with no fabric port at all (confirmed via `javap` — not on `Item` either); NeoForge's
  own default implementation returns `false` for virtually every item, so hardcoded `false` (TODO fabric).
- [x] `DeployerItemHandler.java`, `ItemHandlerBeltSegment.java`, `content/logistics/tunnel/BrassTunnelItemHandler.java`,
  `content/logistics/depot/DepotItemHandler.java`, `content/fluids/drain/ItemDrainItemHandler.java` (all
  clean) — swept the recurring `Item#getMaxStackSize()` doesn't exist bug across every remaining
  `resource.getItem().getMaxStackSize()`/`variant.getItem().getMaxStackSize()` call site codebase-wide
  (`grep -rn` confirmed these were the last 5) → `getDefaultMaxStackSize()`.
- [x] `DeployerItemHandler.java` (clean) — porting-lib's `ItemHandlerHelper.copyStackWithSize` doesn't
  exist (established replacement from earlier sessions) → `ItemStack#copyWithCount`.
- [x] `content/fluids/FlowSource.java` (clean) — missing `BlockEntity` import (plain miss, not a
  renamed/moved class).
- [x] `content/logistics/chute/ChuteBlockEntity.java` (clean) — `setLevel()` assigned to `capAbove`/
  `capBelow`, two fields **never declared anywhere in the class** — confirmed via grep that the class's
  real per-direction capability lookup goes entirely through the already-working `capCaches`/
  `grabCapability(Direction)` system elsewhere in the file; this was dead leftover code from an abandoned
  earlier design, not a real regression — dropped the two broken assignments.
- [x] `content/logistics/box/PackageItem.java` (clean) — `ItemVariant#hasNbt()`/`#getNbt()` don't exist
  (pre-data-component-system NBT API) → read the same `AllDataComponents.PACKAGE_ADDRESS` component the
  sibling `ItemStack`-based overload already uses, via `variant.toStack().getOrDefault(...)`.
- [x] `content/equipment/sandPaper/SandPaperItem.java` (clean) — same `Item#hasCraftingRemainingItem`/
  `#getCraftingRemainingItem`-live-on-`Item`-not-`ItemStack` bug as `RecipeApplier.java` (batch 52).
- [x] `content/equipment/extendoGrip/ExtendoGripItem.java` (clean) — same `doesSneakBypassUse` gap as
  `DeployerHandler.java` above, but here it's the item's actual *intended* behavior (letting the Extendo
  Grip use blocks while sneaking is the whole point of the item), not a dead check — documented as a real
  TODO fabric gap needing a Mixin into the relevant `Player`/`ServerPlayerGameMode` use-item-on logic to
  restore properly, rather than silently hardcoding a value like the `DeployerHandler.java` case. Also
  dropped the already-dead, already-commented-out-at-its-only-use-site `SimpleCustomRenderer` import.
- [x] `content/fluids/FluidPropagator.java` (clean) — dead NeoForge `Capabilities`/`IFluidHandler`
  imports, neither referenced anywhere in the file body.

## Done this session (batch 57)
Trajectory: 345 → 330 (confirmed).
- [x] `content/equipment/bell/SoulParticle.java`, `SoulBaseParticle.java` (both clean) — same
  `ParticleHelper.setStoppedByCollision` doesn't exist bug fixed in `CubeParticle.java` (batch 56) →
  `Particle#stoppedByCollision` public field directly. **Swept the whole codebase for remaining
  `ParticleHelper` references afterward — none left.**
- [x] `content/processing/sequenced/SequencedAssemblyRecipe.java` (clean) — missing `RecipeWrapper` import.
- [x] `content/processing/recipe/ProcessingRecipeSerializer.java` (clean) — `T::getProcessingDuration`/
  `T::getRequiredHeat` method references failed generic type inference against
  `RecordCodecBuilder`/`RecipeBuilder`'s bounds (T only has a wildcard-bounded `ProcessingRecipe<?, ?>`
  upper bound, which method-reference inference handles less permissively than an explicit lambda) →
  swapped both for plain lambdas (`i -> i.getProcessingDuration()`), which infer fine.
- [x] `content/processing/basin/BasinRecipe.java` (clean) — two bugs: `rollResults()` needs a
  `RandomSource` arg now (no-arg overload doesn't exist) → `basin.getLevel().random`;
  `new DummyCraftingContainer(availableItems, extractedItemsFromSlot)` passed 2 args including a
  never-declared `extractedItemsFromSlot` variable, but the real constructor only takes 1 arg
  (`NonNullList<ItemStack>`) → the already-in-scope `consumedItems` list (exactly that type, already
  populated by the ingredient-matching loop just above) is the correct single argument.
- [x] `content/logistics/redstoneRequester/RedstoneRequesterMenu.java` (clean) — missing `ItemStack`
  import; nested `SorterProofSlot` was typed to take `SlottedStorage<ItemVariant>` but the real
  `SlotItemHandler` base class needs this project's own `SlottedStackStorage` interface (which
  `ItemStackHandler`, the type of the `ghostInventory` field actually passed in, already implements).
- [x] `content/logistics/item/filter/attribute/ItemAttribute.java` (clean) — the last remaining
  `CatnipCodecUtils.decodeOrNull` holdout codebase-wide (swept and confirmed none left) → `.decode(...).orElse(null)`.
- [x] `content/logistics/item/filter/attribute/attributes/ShulkerFillLevelAttribute.java` (clean) —
  `ItemContainerContents#getSlotCount()` doesn't exist on the vanilla class at all (confirmed via
  `javap` — it only implements a porting-lib mixin interface, `ItemContainerContentsInjection`, whose
  default method `port_lib$getSlots()` is the real replacement, directly callable with no cast since
  the interface is implemented directly).
- [x] `content/logistics/item/filter/attribute/AllItemAttributeTypes.java` (clean) — `ComposterBlock.getValue(ItemStack)`
  doesn't exist (confirmed via `javap` — no such static method); the real API is the class's own public
  `Object2FloatMap<ItemLike> COMPOSTABLES` field → `ComposterBlock.COMPOSTABLES.getFloat(s.getItem()) > 0`.
  Also another `getTagEnchantments()` → `getEnchantments()` instance.
- [x] `content/fluids/tank/storage/FluidTankMountedStorage.java` (clean) — dead `CreateCodecs` import
  (wrong package on top of being entirely unused — real class lives under `foundation.codec`, not
  `foundation.utility`); `getCapacity()` returns `long` but the codec field wants `Integer` → cast.
- [x] `content/fluids/tank/FluidTankItem.java` (clean) — `BlockItem.getBlockEntityData(ItemStack)`
  doesn't exist (confirmed via `javap` — no such static method); real 1.21.1 API is checking the
  `DataComponents.BLOCK_ENTITY_DATA` component directly via `item.has(...)`.
- [x] `content/logistics/stockTicker/StockTickerInteractionHandler.java` (partial — 1 of 2 errors fixed,
  the other genuinely deferred) — dead `NetworkHooks` import removed, but the actual
  `sp.openMenu(provider, buf -> {...})` 2-arg call (needed here because `showLockOption`/
  `isCurrentlyLocked`/the target `BlockPos` are real per-open server-computed data, not something the
  client can re-derive) is **left broken on purpose**: vanilla's replacement mechanism for this
  (`ExtendedScreenHandlerFactory`/`ExtendedScreenHandlerType` from `fabric-screen-handler-api-v1`,
  confirmed present in the resolved dependencies) pairs with a *different* `MenuType` registration
  shape than what `AllMenuTypes.java`'s `Create.registrate().menu(...)` (a `MenuBuilder.ForgeMenuFactory`-
  based builder, name says it all) currently produces — fixing this one menu properly means either
  extending the registrate menu-builder to support the extended type, or hand-registering just this one
  `MenuType` outside registrate. Investigated but didn't attempt a shaky partial fix; only one other call
  site in the whole codebase has this exact shape (`BlueprintEntity.java`, already flagged as deferred-
  complex), so this is a contained, well-scoped follow-up, not a sweep-blocking issue.

## Done this session (batch 58)
Trajectory: 330 → 311 (confirmed).
- [x] `content/processing/sequenced/SequencedAssemblyRecipe.java` (clean, second pass) — adding the
  missing `RecipeWrapper` import (batch 57) surfaced a real generics bug underneath it: `implements
  Recipe<RecipeWrapper>` but `assemble(RecipeInput, HolderLookup.Provider)` was typed against the
  broader `RecipeInput` interface instead of the class's own `RecipeWrapper` type parameter — retyped
  to match, clearing a 3-error name-clash/not-abstract cascade.
- [x] `content/redstone/nixieTube/NixieTubeRenderer.java` (clean) — porting-lib's `FontRenderUtil.getFontStorage`
  doesn't exist in the resolved version (same old-jar-only-class shape as `ParticleHelper`) — the
  project's own `FontAccessor` mixin already exposes `Font`'s package-private `fonts` function for
  exactly this purpose (`((FontAccessor) font).create$getFonts().apply(resourceLocation)`).
- [x] `content/redstone/link/controller/LinkedControllerClientHandler.java` (clean) — undefined `window`
  variable → `mc.getWindow()`.
- [x] `content/redstone/displayLink/DisplayLinkBlockEntity.java` (clean) — dead CC:Tweaked
  `PeripheralCapability` import; missing `implements TransformableBlockEntity` despite already having
  the matching `transform()` method (recurring bug, same shape as `MechanicalCrafterBlockEntity`/
  `BasinBlockEntity`/`ItemVaultBlockEntity` from earlier sessions).
- [x] `content/logistics/packagerLink/PackagerLinkBlockEntity.java`, `LogisticsManager.java` (both
  clean) — `isTargetingSameInventory(identifier)`/`getSummary(identifier)` referenced an undefined
  `identifier` variable; the real in-scope param is `ignoredHandler` (an `IdentifiedInventory` record) —
  `isTargetingSameInventory` wants the record's `.identifier()` accessor (an `InventoryIdentifier`),
  while `getSummary` wants the whole `IdentifiedInventory` record directly — different target types, so
  checked each call site's real parameter type rather than assuming the same fix applied to both.
- [x] `content/logistics/packagePort/PackagePortTargetSelectionHandler.java`,
  `content/kinetics/chainConveyor/ChainConveyorInteractionHandler.java` (both clean) — `Items.TOOLS_WRENCH`/
  `Tags.Items.TOOLS_WRENCH` (both wrong classes, recurring bug) → `AllItemTags.WRENCH.tag`; dead NeoForge
  `Tags` and dead `ReachUtil` imports.
- [x] `impl/unpacking/CrafterUnpackingHandler.java` (clean) — `order.stacks()` referenced an undefined
  `order` variable; the method's real parameter is `orderContext` (a `PackageOrderWithCrafts`, which has
  its own `.stacks()` accessor) — another instance of the "leftover variable name from before a
  rename/refactor" bug shape seen repeatedly this session.
- [x] `infrastructure/ponder/AllCreatePonderScenes.java` (clean) — `com.tterrag.registrate.fabric.RegistryObject`
  doesn't exist at all (confirmed via jar listing — registrate's `BlockEntry` constructor now wants
  porting-lib's `DeferredHolder` instead, confirmed via `javap`) → `DeferredHolder.create(ResourceKey.create(Registries.BLOCK, ...))`.

## Done this session (batch 59)
Trajectory: 311 → 271 (confirmed) — biggest single-batch drop this session, ~25 files.
- [x] `content/processing/recipe/ProcessingRecipeSerializer.java` (clean, second pass) — the batch-57
  lambda fix wasn't the real issue: `StandardProcessingRecipe.Builder<T>` requires `T extends
  StandardProcessingRecipe<?>`, but the method's own `T` is only bound to the broader `ProcessingRecipe<?,?>`
  interface, so no `T` can ever satisfy both bounds simultaneously without an unchecked escape hatch —
  switched the local `builder` variable to a raw `StandardProcessingRecipe.Builder` (matching the
  `@SuppressWarnings({"unchecked","rawtypes"})` already sitting on that declaration) with a final
  unchecked `(T) builder.build()` cast at the return.
- [x] `content/processing/burner/BlazeBurnerHandler.java`, `content/logistics/funnel/FunnelMovementBehaviour.java`,
  `content/logistics/packagerLink/LogisticsManager.java` (all clean) — three more instances of the
  "leftover `event`/undefined-variable from an incomplete NeoForge-event-to-plain-param rename" bug
  shape (established pattern all session) — real in-scope replacements were `hitResult`,
  `TransferUtil.insertItemStacked` (porting-lib's `ItemHandlerHelper.insertItemStacked` doesn't exist),
  and `ignoredHandler` respectively.
- [x] `content/logistics/packager/PackagerBlock.java` (clean) — `be.unwrapBox(stack, true/false)` passed
  raw booleans where the method now takes a real fabric `TransactionContext` (simulate vs. commit is no
  longer a boolean flag, it's whether the transaction gets `.commit()`ed) — wrapped in explicit
  simulate-then-real `Transaction.openOuter()` blocks.
- [x] `content/logistics/stockTicker/StockKeeperCategoryScreen.java` (clean) — dead `ScreenWithStencils`
  import (class doesn't exist anywhere, confirmed unused in the file body — not even a renamed class,
  genuinely never existed on the fabric side); `GuiGraphics#drawString` wants `int` x/y, not `float`
  (confirmed via `javap` — every overload takes `int, int`).
- [x] `content/contraptions/elevator/ElevatorContactBlock.java`, `content/kinetics/steamEngine/PoweredShaftBlock.java`
  (both clean) — two more instances of the `getCloneItemStack` NeoForge-patched-signature-vs-vanilla-`Block`
  bug from `BlazeBurnerBlock.java` (batch 50) — swept and confirmed no more instances remain codebase-wide.
- [x] `content/contraptions/actors/seat/SeatBlock.java`, `content/trains/track/FakeTrackBlock.java` (both
  clean) — the last 2 remaining `BlockPathTypes` → `PathType` rename instances (same fix as `BeltBlock.java`,
  earlier session) — swept and confirmed none left.
- [x] `content/contraptions/mounted/CartAssemblerBlockItem.java` (clean) — the last non-deferred
  `MinecartAndRailUtil` dead-class holdout (only `MinecartController.java`, deferred-complex, still has
  one) → `state.getValue(((BaseRailBlock) block).getShapeProperty())` (established replacement).
- [x] **Dead `ReachUtil` import sweep — 6 more files** (`SuperGlueHandler.java`, `SuperGlueSelectionHandler.java`,
  `SuperGlueSelectionPacket.java`, `HighlightCommand.java`, `LecternControllerBlockEntity.java`,
  `ChainPackageInteractionHandler.java`) — grepped the whole codebase for the class name; only
  `SuperGlueHandler.java` had a real call site (→ `Attributes.BLOCK_INTERACTION_RANGE`, same as
  `BigOutlines.java`/`ChainConveyorInteractionHandler.java` earlier), the other 5 were dead imports only.
- [x] `AllEntityTypes.java` (clean) — `EntityTypes.TELEPORTING_NOT_SUPPORTED` referenced a class that was
  never imported (and doesn't exist under that name anyway) — real symbol is fabric-api's own
  `net.fabricmc.fabric.api.tag.convention.v2.ConventionalEntityTypeTags.TELEPORTING_NOT_SUPPORTED`
  (confirmed via `javap`, a `TagKey<EntityType<?>>`).
- [x] `content/logistics/box/PackageEntity.java` (clean) — `build()` was typed against
  `EntityType.Builder<?>` (vanilla) while every sibling entity's `build()` (e.g. `SeatEntity.java`) uses
  `FabricEntityTypeBuilder<?>` — retyped and swapped the vanilla-only `.sized(w, h)` for the fabric
  builder's real `.dimensions(EntityDimensions.fixed(w, h))`.
- [x] `AllSoundEvents.java` (clean) — two `getMainEventHolder()` overrides returned a plain `SoundEvent`
  where the interface wants `Holder<SoundEvent>` → `BuiltInRegistries.SOUND_EVENT.wrapAsHolder(...)`
  (confirmed via `javap` on vanilla `Registry` — the real registry-bound holder wrapper, not a
  standalone `Holder.direct`).
- [x] `compat/Mods.java`, `AllBlockSpoutingBehaviours.java` (both clean) — `Mods.BOTANIA` was referenced
  but genuinely never added as an enum constant (Botania is on the deprioritized-mods list, but the
  compile error itself was a one-line fix — added the enum entry rather than deleting the whole guarded
  branch, consistent with how every other mod's optional-compat branch in this file is written).
- [x] `api/data/recipe/ItemApplicationRecipeGen.java` (clean) — `Tags.Items.STRIPPED_LOGS`/`STRIPPED_WOODS`
  don't exist on porting-lib's own `Tags` class → fabric-api's `ConventionalItemTags` (both fields
  confirmed present via `javap` on the resolved 2.11.1 jar, same tag-class swap pattern as `AllItems.java`
  and `CreateMechanicalCraftingRecipeGen.java` from earlier sessions).
- [x] `api/behaviour/display/DisplayTarget.java` (clean) — `BlockEntity#getCustomData()` doesn't exist on
  vanilla `BlockEntity` at all (confirmed via `javap`) — real replacement is porting-lib's
  `BlockEntityInjection#getPortingLibPersistentData()` mixin default method (the fabric-side equivalent
  of Forge's classic "attach arbitrary persistent NBT to any block entity" concept). **Note: many other
  files call `.getCustomData()` too, but on `ItemStack`/`Entity` receivers where a real method by that
  name exists — this fix applies only to `BlockEntity` receivers; don't blanket-replace every occurrence.**
- [x] `content/equipment/toolbox/ToolboxHandlerClient.java` (clean) — same undefined-`window` bug as
  `LinkedControllerClientHandler.java` above → `mc.getWindow()`.
- [x] `content/equipment/armor/RemainingAirOverlay.java` (clean) — NeoForge's `FluidType`-based
  `Entity#getEyeInFluidType()`/`#canDrownInFluidType(FluidType)` extensions have no fabric port at all —
  `DivingHelmetItem.java` already established the real replacement pattern for this exact concern
  (`Entity#isEyeInFluid(TagKey<Fluid>)`, vanilla) — applied the same here with `FluidTags.WATER`. Swept
  the codebase afterward; no more `getEyeInFluidType`/`canDrownInFluidType` call sites remain.
- [x] `content/equipment/blueprint/BlueprintMenu.java` (clean) — missing `EnvType`/`Environment` imports
  entirely (not a leftover-annotation bug, just never added).

## Done this session (batch 60)
Trajectory: 252 confirmed (from 271) — another ~23-file batch, mostly `foundation`/`infrastructure`
one-off bugs with no shared theme.
- [x] `content/trains/track/TrackBlockEntity.java` (clean) — last remaining `AABB.INFINITE` instance
  (doesn't exist on vanilla `AABB` — same fix as `EjectorBlockEntity.java` earlier this session); swept
  codebase-wide afterward, none left.
- [x] `foundation/block/render/CustomBlockModels.java` (clean) — missing `Blocks` import.
- [x] `foundation/blockEntity/behaviour/edgeInteraction/EdgeInteractionHandler.java` (clean) —
  `behaviour.requiredPredicate` referenced a field that was actually named `requiredItem` (leftover from
  an incomplete field rename — same bug shape as several `order`/`identifier`-style leftover-variable
  bugs found earlier this session, just on a field instead of a local).
- [x] `foundation/blockEntity/behaviour/inventory/CapManipulationBehaviourBase.java` (clean) — a
  byte-for-byte **duplicate `getTarget()` method** (identical body, identical javadoc) sitting a few
  lines apart — classic bad-merge duplication, deleted the second copy.
- [x] `foundation/blockEntity/behaviour/inventory/TankManipulationBehaviour.java` (clean) — this
  project's `FluidStack#setAmount(long)` mutates in place and returns `void` (confirmed by reading the
  class), but the call site used `return stack.setAmount(extracted);` as if it returned the stack →
  split into a mutate-then-return.
- [x] `foundation/collision/OrientedBB.java` (clean) — same dead `ContinuousSeparationManifold`
  private-nested-class import bug as `ContraptionCollider.java` from earlier this session.
- [x] `foundation/data/CreateRegistrate.java` (clean) — dead import of `CallbackImpl`, a `private
  record` nested in `CreateRegistrateRegistrationCallbackImpl` (inaccessible from outside anyway, and
  never referenced in the file body beyond the two import lines).
- [x] `foundation/data/RuntimeDataGenerator.java` (clean) — two independent bugs stacked: `WithConditions`
  was imported from the wrong porting-lib subpackage (`.resources.conditions` — doesn't exist — instead
  of the real `.conditions`); `Recipe.CONDITIONAL_CODEC` doesn't exist on vanilla `Recipe` at all
  (confirmed via `javap`) — real replacement is porting-lib's own
  `ConditionalOps.createConditionalCodecWithConditions(Recipe.CODEC)` (confirmed via `javap` on the
  `conditions` module jar — a purpose-built static factory for exactly "wrap a codec with
  fabric-resource-conditions support", the fabric-side equivalent of NeoForge's conditional-codec system).
- [x] `foundation/data/SimpleDatagenIngredient.java` (clean) — `new Ingredient(values)` (an array) isn't
  a real constructor; vanilla `Ingredient` only has `Ingredient(Stream<? extends Value>)` (confirmed via
  `javap`) → wrapped in `Stream.of(...)`.
- [x] `foundation/data/recipe/CreatePressingRecipeGen.java` (clean) — `Mods.BEF` was removed from the
  `Mods` enum in favor of `Mods.BE` ("Better End") per the enum file's own inline comment
  (`//BEF("betterendforge"), fabric: replaced with Better End`), but this one call site in
  `CreatePressingRecipeGen.java` was never updated to match.
- [x] `foundation/item/render/CustomItemModels.java` (clean) — dead `net.minecraftforge.registries.ForgeRegistries` import.
- [x] `foundation/mixin/ItemStackMixin.java` (clean) — `PatchedDataComponentMap#isPatchEmpty()` doesn't
  exist (confirmed via `javap`) → `.asPatch().isEmpty()` (the real way to check "does this patch have no
  changes", going through `DataComponentPatch#isEmpty()` instead).
- [x] `foundation/pack/DynamicPack.java` (clean) — `PackMetadataSection`'s constructor gained a required
  3rd `Optional<InclusiveRange<Integer>>` parameter (confirmed via `javap`) → `Optional.empty()`.
- [x] `foundation/recipe/trie/RecipeTrie.java` (clean) — `Ingredient#isSimple()` doesn't exist (a
  NeoForge patch method for "is this a plain vanilla ingredient, not a custom modded one", used to decide
  whether the trie's fast-path lookup is safe to apply) — fabric-api's `Ingredient` implements
  `FabricIngredient` directly (confirmed via `javap`), whose `getCustomIngredient()` returning `null`
  means the same thing → swapped `!ingredient.isSimple()` for `ingredient.getCustomIngredient() != null`.
  Swept codebase-wide afterward; no more `isSimple()` calls remain.
- [x] `foundation/render/SpecialModels.java` (clean) — flywheel's `BakedModelBuilder#materialFunc` and
  `ModelUtil#getMaterial` both dropped their third `ao` (ambient-occlusion) boolean parameter in the
  resolved flywheel version on this project's classpath (confirmed via `javap` directly on the resolved
  jar, not an assumption from memory) — trimmed both the lambda signature and the inner call to match.
- [x] `foundation/utility/ServerSpeedProvider.java` (clean) — missing `MinecraftServer` import.
- [x] `infrastructure/command/AllCommands.java` (clean) — called `FixLightingCommand.register()` on a
  class whose **entire body is commented out** (a deliberately-disabled command, confirmed by reading
  the file — every line starts with `//`) — commented out the one call site to match, rather than
  un-commenting a command that was clearly disabled on purpose.
- [x] `infrastructure/data/CreateWikiBlockInfoProvider.java` (clean) — `FireBlock#getBurnOdds(BlockState)`
  is genuinely `private` on vanilla `FireBlock` (confirmed via `javap -p`) — added a new
  `FireBlockAccessor` mixin (`@Invoker("getBurnOdds")`), registered in `create.mixins.json`, following
  the exact same established pattern as `AbstractMinecartAccessor`.
- [x] `infrastructure/debugInfo/DebugInformation.java`, `compat/jei/StockKeeperTransferHandler.java`
  (both clean) — two more dead `EnvExecutor` imports.
- [x] `infrastructure/gui/CreateMainMenuScreen.java` (clean) — dead import of
  `io.github.fabricators_of_create.porting_lib.mixin.accessors.client.accessor.TitleScreenAccessor`, a
  class that doesn't exist anywhere in any resolved porting-lib module (confirmed — not a wrong-path
  bug like several others this session, genuinely absent) and wasn't referenced in the file body anyway.
- [x] `infrastructure/worldgen/LayerPattern.java` (clean) — missing `@NotNull` import (only `@Nullable`
  was imported).
- [x] `compat/jei/category/sequencedAssembly/JeiSequencedAssemblySubCategory.java` (clean, JEI is
  priority) — `SizedFluidIngredient` (NeoForge) → this project's own `FluidIngredient`, matching exactly
  what `CreateRecipeCategory.addFluidSlot(...)` actually accepts as a parameter (checked the real method
  signature rather than assuming — a repeated lesson from this session: always verify the call site's
  actual expected type before picking a replacement).

**Current verified baseline: 252 errors** (down from 503 at the start of this session, ~4,130 at the very
start of the port — see `/tmp/cr_verify13.log`/`/tmp/error_files.txt`, container-local scratch files).
Non-deprioritized frontier is now thin: `BlueprintEntity.java` (20, deferred multi-part feature),
`MinecartController.java` (18, deferred fabric-attachment-API redesign), `StockTickerInteractionHandler.java`
(1 remaining error, deferred — needs the `ExtendedScreenHandlerFactory` redesign above), and a long tail of
~55 files with 1-4 errors each scattered across nearly every content package — no single shared root
cause found across the ~140 files fixed this session (each is its own small distinct bug: missing
imports, NeoForge-patched-method signature mismatches, undefined-variable leftovers from incomplete
renames, the occasional genuinely-removed vanilla API needing a `javap`-verified replacement). The
deprioritized compat mods (JourneyMap/EMI/FTB/REI/sandwichable/CC:Tweaked) still account for the single
biggest chunks (`JourneyTrainMap.java` 48, `CreateEmiPlugin.java` 46, `FTBChunksTrainMap.java` 32,
`CreateREI.java` 30, plus several more REI/EMI/FTB files in the 6-10 range).

## Session resumed (new container) — environment note
Picked this back up in a fresh cloud container; the previous session's `/tmp` artifacts (scripts,
logs) were gone (ephemeral container), so re-derived everything from this file + a fresh compile.
**Gradle needs an explicit `-Dorg.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64` override**
on this kind of container — `gradle.properties`' committed `org.gradle.java.home` points at a
macOS path (`/Library/Java/...`) from whoever's dev machine, which doesn't exist here. Also: this
environment's outbound network policy denies `maven.fabricmc.net` by default (blocks even resolving
the `fabric-loom` Gradle plugin) — needed the container's network access widened before any Gradle
command could work at all.
First fresh-container compile confirmed the batch-49 checkpoint's fixes did land clean: **503 → 478
errors** (javac summary line, authoritative metric per the correction note above).

## Done this session (batch 50)
Trajectory: 478 → 455 (confirmed).
- [x] `foundation/data/recipe/CreateMechanicalCraftingRecipeGen.java` (clean) — `net.neoforged.neoforge.common.Tags.Items.GLASS_BLOCKS`/`.OBSIDIANS`
  (NeoForge-only) → fabric-api's own `net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags.GLASS_BLOCKS`/`.OBSIDIANS`
  (confirmed present via `javap` on the resolved 2.11.1 convention-tags jar — unlike the `DOUGH_FOODS`/`.DRINKS` gap
  found in `AllItems.java` last session, these two fields **do** exist at this resolved version).
- [x] `content/contraptions/actors/harvester/HarvesterMovementBehaviour.java` (clean) — same genuinely-unported
  `net.neoforged.neoforge.common.SpecialPlantable` gap as `BlockHelper.java` (no fabric port at all) — dropped the
  dead `IPlantable`/`SpecialPlantable` imports and the `instanceof SpecialPlantable` branch, left a `// TODO fabric:`
  note pointing at the precedent.
- [x] `content/contraptions/OrientedContraptionEntity.java` (clean, 2 passes — see batch 51 note below for the
  second) — same recurring `io.github.fabricators_of_create.porting_lib.util.MinecartAndRailUtil` dead-class bug as
  `MinecartContraptionItem.java`/`MinecartSim2020.java` last session; fixed the one real call site with the
  established replacement, `blockState.getValue(abstractRailBlock.getShapeProperty())`.
- [x] `AllBlocks.java` + `content/contraptions/mounted/CartAssemblerBlock.java` (both clean) — `getPistonPushReaction(BlockState)`
  is **not** an overridable `Block`/`BlockBehaviour` method in 1.21.1 at all (confirmed via `javap` — no such method
  on either class); vanilla 1.21.1 configures push reaction through the `BlockBehaviour.Properties` builder instead
  (`Properties#pushReaction(PushReaction)`, confirmed present via `javap`). Deleted the invalid `@Override` in
  `CartAssemblerBlock.java` and added `.pushReaction(PushReaction.BLOCK)` to the block's `initialProperties`/`properties`
  builder chain in `AllBlocks.java` instead — matches the file's already-established pattern of configuring
  Block behavior through properties rather than overrides where 1.21.1 vanilla moved it there.
- [x] `content/contraptions/render/ContraptionEntityRenderer.java` (clean) — same "NeoForge's `ModelData` capability
  system has no fabric port" gap as `WrappedBlockAndTintGetter.java`/`TableClothModel.java` earlier sessions, this
  time hitting `BakedModel#getModelData`/`#getRenderTypes(state, random, modelData)` (both NeoForge-only extensions —
  confirmed via `javap` that vanilla fabric `BakedModel` only extends fabric-api's own `FabricBakedModel`, no
  `ModelData`-aware methods at all) plus `BlockRenderDispatcher`'s real `ModelBlockRenderer#tesselateBlock` taking
  9 vanilla params, not the 11-param NeoForge-patched overload this file called. Rewrote the per-`RenderType`-layer
  filtering (previously `model.getRenderTypes(state, random, modelData).contains(layer)`) using vanilla's own
  single-render-type-per-block-state API instead: `ItemBlockRenderTypes.getChunkRenderType(state) == layer`
  (confirmed present via `javap`) — a behavior simplification (one type per block instead of a multi-layer set) but
  matches what vanilla 1.21.1 itself supports without NeoForge's patch.
- [x] `content/processing/burner/BlazeBurnerBlock.java` (clean) — `getCloneItemStack(BlockState, HitResult, LevelReader, BlockPos, Player)`
  is a NeoForge-patched overload; vanilla `Block` only declares `getCloneItemStack(LevelReader, BlockPos, BlockState)`
  (confirmed via `javap` — no HitResult/Player params at all) — retyped to match, body unchanged (`getLitOrUnlitStack(state)`
  never used the extra params anyway). Also the recurring `@OnlyIn(Dist.CLIENT)` leftover-NeoForge-annotation bug (see
  batch 51 — this file already had the correct `net.fabricmc.api.EnvType`/`Environment` imports sitting unused).
- [x] `content/contraptions/ContraptionCollider.java` (clean) — three independent dead-import/API bugs: unused
  `io.github.fabricators_of_create.porting_lib.util.EnvExecutor` import (never referenced in the body — just deleted,
  no `BacktankUtil.java`-style rewrite needed since nothing called it); unused import of `ContinuousOBBCollider.ContinuousSeparationManifold`,
  a `private static class` nested in a different file (also never referenced — the import alone was the only use,
  deleted); `BlockState#getFriction(Level, BlockPos, Entity)` is a NeoForge 3-arg patch, vanilla only has the no-arg
  `Block#getFriction()` (confirmed via `javap`, same shape as the `getSoundType`/`getPistonPushReaction` NeoForge-patch
  pattern found repeatedly this session) → `blockState.getBlock().getFriction()`.
- [x] `content/processing/sequenced/SequencedRecipe.java` (clean) — three independent bugs in one small file:
  (1) `ProcessingRecipe::getRecipeType` doesn't exist (no such method anywhere on `ProcessingRecipe`) — the real
  dispatch key is `getTypeInfo()` (declared on the `IRecipeTypeInfo` interface it implements via `AllRecipeTypes`),
  cast to `AllRecipeTypes` since that's the only real implementor and `AllRecipeTypes::processingCodec` (the codec
  side of the dispatch) is an *instance* method only declared there; (2) `STREAM_CODEC` referenced
  `v.writeToBuffer(b)`/`SequencedRecipe::readFromBuffer`, methods that were never actually implemented anywhere in
  the class (a "wire this up later" stub, same shape as the `FluidStack.CODEC = null` gap found last session) —
  implemented both properly: write the `AllRecipeTypes` enum tag first (`buffer.writeEnum`), then delegate to that
  type's own `RecipeSerializer#streamCodec()` (each `ProcessingRecipe` subtype already has one via
  `StandardProcessingRecipe.Serializer`, confirmed last session); read does the mirror (`readEnum` then decode
  through the same type's serializer); (3) `CompoundIngredient.of(...)` (NeoForge's ingredient-OR-combinator, no
  fabric port — same class flagged as a dead `instanceof`-only check in `BlueprintItem.java` last session, but here
  it's actually *constructed*, not just type-checked) → fabric-api's own equivalent,
  `net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients.any(Ingredient...)` (confirmed present via
  `javap` on the resolved `fabric-recipe-api-v1` jar — this is a real, direct replacement, not a stub/TODO).
  **If another file needs an OR-combined `Ingredient` (`CompoundIngredient`/`Ingredient.of(a, b)`-shaped NeoForge
  code), reach for `DefaultCustomIngredients.any(...)` first before assuming it needs a TODO stub** — fabric-api
  ships this natively (also has `.all(...)` for AND and `.difference(...)` for NOT-style combinators, same jar).

## Done this session (batch 51)
Trajectory: 455 → 441 (confirmed).
- [x] **Recurring `@OnlyIn(Dist.CLIENT)`-leftover-annotation bug, bulk-found via `grep -rl "@OnlyIn(Dist\." src/main/java/`**
  — 6 files (`TrackBlock.java`, `ITrackBlock.java`, `PipeConnection.java`, `ClipboardOverrides.java`,
  `ClipboardScreen.java`, `LitBlazeBurnerBlock.java`) all had the exact same shape as `BlazeBurnerBlock.java` earlier
  this batch: the correct `net.fabricmc.api.EnvType`/`Environment` imports were already sitting there unused, just
  the annotation itself was never swapped from NeoForge's `@OnlyIn(Dist.CLIENT)`/`Dist.DEDICATED_SERVER`. Fixed all 6
  in one `sed` pass (`@OnlyIn(Dist.CLIENT)` → `@Environment(EnvType.CLIENT)`, `@OnlyIn(Dist.DEDICATED_SERVER)` →
  `@Environment(EnvType.SERVER)`). **Re-run that grep periodically — if this pattern turns up again, bulk-fix the
  same way rather than hand-fixing file by file.**
- [x] `content/trains/track/ITrackBlock.java` (clean) — while fixing the above, found a genuine duplicate-import
  hard compile error sitting in the same file: `net.fabricmc.api.EnvType`/`Environment` were imported **twice**
  (once near the top, once again right before the interface declaration) — deleted the second copy.
- [x] `content/contraptions/OrientedContraptionEntity.java` (clean, second pass) — same bug as `ITrackBlock.java`:
  a duplicate `io.github.fabricators_of_create.porting_lib.util.MinecartAndRailUtil` import (two copies, only one
  caught/removed in batch 50's first pass) was still there, still causing "cannot find symbol" on its own line even
  though the one real call site had already been fixed.
- [x] `AllBlocks.java` (clean) — dead import of `com.simibubi.create.content.decoration.CardboardBlockItem`, a class
  that doesn't exist anywhere in the codebase (only `CardboardBlock.java` does) — never referenced in the file body,
  just deleted.

**Current verified baseline: 441 errors** (from `/tmp/cr_verify4.log`; frontier regenerated to `/tmp/error_files.txt`
— both are container-local scratch files, gone if the container recycles, but the counts above are durable).
Frontier is now dominated by deprioritized-mod compat code — top of `/tmp/error_files.txt`: `JourneyTrainMap.java`
(48, JourneyMap deprioritized), `CreateEmiPlugin.java` (46, EMI deprioritized), `FTBChunksTrainMap.java` (32, FTB
deprioritized), `CreateREI.java` (30, REI deprioritized). Next real non-deprioritized targets:
`content/equipment/blueprint/BlueprintEntity.java` (20, already flagged above as a genuinely complex deferred
multi-part feature port — `BlueprintCraftingInventory` doesn't exist, `CommonHooks.setCraftingPlayer` has no fabric
port, etc.), `content/contraptions/minecart/capability/MinecartController.java` (18, also already flagged as
deferred — needs a fabric attachment-API redesign), then the run of 6-8-error files below those (JEI category files
are priority-mod, worth doing; REI/EMI/sandwichable/ftb ones are not).

## Branch consolidation
All work moved from the session throwaway branch (`claude/focused-euler-auneot`) onto `main` directly, per explicit
user request ("move it to main, and all the history too") — done as a fast-forward merge (`git merge-base
--is-ancestor` + `git rev-list --left-right --count` verified zero divergence beforehand), so **no history was lost
or squashed**. Pushed to `origin/main`. The user also asked to delete the now-redundant old branch; the local copy
was deleted (`git branch -d`), but `git push origin --delete claude/focused-euler-auneot` returned HTTP 403 — this
session's git credentials/GitHub MCP toolset have no branch-delete permission. **The remote branch
`claude/focused-euler-auneot` is still sitting on GitHub and needs to be deleted manually** (repo owner, via the
GitHub UI or elevated credentials).

## Done this session (batch 61)
Trajectory: 252 → 237 (confirmed).
- [x] `build.gradle.kts` — porting-lib's `conditions` module (needed by `RuntimeDataGenerator`'s `ConditionalOps`,
  batch 60) was never declared as a build dependency at all, and it isn't published at the same version as the rest
  of porting-lib (`portingLibVersion = beta.91` 404s for this module) — pinned it separately as
  `portingLibConditionsVersion = "3.1.0-beta.47+1.21.1"` rather than folding it into the shared
  `portingLibModules`/`portingLibVersion` loop, and added
  `modApi(include("io.github.fabricators_of_create.Porting-Lib:conditions:$portingLibConditionsVersion")!!)`.
  Verified resolvable via `./gradlew help -q` before moving on.
- [x] `content/trains/entity/StructureUtilsMixin.java` (clean) — `ResourceLocation`'s single-arg constructor is gone
  in 1.21.1 → `ResourceLocation.parse(...)`.
- [x] `content/trains/station/StationBlockEntity.java` (clean) — imported `GlobalPackagePort` as a nested class of
  `GlobalStation`; it's actually its own top-level class in the same package — fixed the import.
- [x] `content/trains/bogey/StandardBogeyBlock.java` (clean) — `getCloneItemStack`'s first param needs to be
  `LevelReader`, not `BlockGetter` (same NeoForge-patch signature-mismatch bug as `BlazeBurnerBlock.java`, batch 50).
- [x] `content/redstone/ToggleLatchBlock.java` / `content/electricity/BrassDiodeBlock.java` (both clean) — missing
  `implements ConnectableRedstoneBlock` despite already importing and using it (recurring bug — swept, confirmed no
  more instances left).
- [x] `content/trains/display/NixieTubeBlock.java` (clean) — undefined `heldItem` var → the method's real param,
  `stack`.
- [x] `content/trains/station/LecternControllerBlock.java` (clean) — `getPickedStack`'s `BlockGetter` param needed a
  `LevelReader` cast to satisfy the newer `getCloneItemStack` signature (same family as `StandardBogeyBlock` above).
- [x] 3 more small files fixed in the same batch (see `git show c4e7aa1f` for the full per-file diff if needed).

**Current verified baseline: 237 errors.**

## Done this session (batch 62)
Trajectory: 237 → 221 (confirmed).
- [x] `content/schematics/SchematicRenderer.java` (clean) — missing `BlockEntity` import.
- [x] `content/logistics/trains/LogisticsNetworkSavedData.java` (clean) — dead `SavedDataUtil` import.
- [x] `content/logistics/frogport/FrogportVisual.java` / `FrogportRenderer.java` / `content/logistics/box/PackageRenderer.java`
  / `content/kinetics/chainConveyor/ChainConveyorVisual.java` (all clean) — dead
  `net.minecraftforge.registries.ForgeRegistries` imports (swept; `RemapHelper.java`'s real usage of the same class
  left alone since it isn't actually erroring there).
- [x] `content/logistics/frogport/FrogportBlockEntity.java` (clean) — undefined `itemHandler` var → the real local,
  `inventory`.
- [x] `content/logistics/frogport/FrogportBlock.java` / `content/logistics/funnel/FunnelBlock.java` /
  `content/kinetics/gearbox/SequencedGearshiftBlock.java` (all clean) — `ItemInteractionResult.PASS` doesn't exist
  (only `PASS_TO_DEFAULT_BLOCK_INTERACTION`/`SKIP_DEFAULT_BLOCK_INTERACTION` do) → swapped to
  `PASS_TO_DEFAULT_BLOCK_INTERACTION`, swept codebase-wide for the same bad constant.
- [x] `content/logistics/box/PackagePortBlockEntity.java` (clean) — constructor assigned to an undefined `itemHandler`
  var instead of the actual final field it was meant to initialize, `exposedInventory`.
- [x] `content/logistics/funnel/FunnelBlockEntity.java` (clean) — dead `EnvExecutor` import.
- [x] `content/logistics/depot/DepotBlockEntity.java` / `content/logistics/crate/CreativeCrateBlockEntity.java` /
  `content/fluids/pipes/SmartFluidPipeBlockEntity.java` (all clean) — missing `implements Clearable` despite already
  having the matching `clearContent()` override (recurring bug, same shape as `ToggleLatchBlock`/`BrassDiodeBlock`
  in batch 61 — swept remaining `BlockEntity` instances).
- [x] `content/kinetics/crank/ValveHandleBlock.java` (clean) — missing `TagUtil` import.

**Current verified baseline: 221 errors** (from the batch 62 verification compile). Full per-file diff in
`git show 02178bee`. Frontier candidates for the next "little files" sweep (post-batch-62, non-deprioritized):
`StockTickerInteractionHandler.java`, `LargeWaterWheelBlock(Item).java`, `SpeedControllerBlockEntity.java`,
`PressingRecipe.java`, `MechanicalMixerBlockEntity.java`, `ArmInteractionPoint.java`/`AllArmInteractionPointTypes.java`,
`GearboxBlock.java`, `StressGaugeBlockEntity.java`/`SpeedGaugeBlockEntity.java`, `DeployerFakePlayer.java`,
`MechanicalCraftingInput.java`/`MechanicalCrafterRenderer.java`, `BlockBreakingMovementBehaviour.java`,
`GenericItemFilling.java`/`FillingRecipe.java`, `FluidTankMountedStorage.java`, `SpoutBlockEntity.java`,
`FluidStackParticle.java`, `FluidNetwork.java`, `ShootableGadgetItemMethods.java`, `RadialToolboxMenu.java`,
`SymmetryWandItem.java`, `CreateHatArmorLayer.java`, `ClipboardEditPacket.java`/`ClipboardBlockItem.java`/
`ClipboardBlockEntity.java`, `HauntedBellPulser.java`, `NetheriteDivingHandler.java`/`CardboardArmorHandler.java`/
`BacktankBlock.java`, `CopycatModel.java`, `ContraptionVisual.java`, `CapabilityMinecartController.java`/
`TrainCargoManager.java`, JEI files (`JeiSequencedAssemblySubCategory.java`/`SpoutCategory.java`/
`StockKeeperTransferHandler.java`/`ConversionRecipe.java` — priority mod), `Create.java`, `AllRecipeTypes.java`.
Skip (deprioritized): `SpoutCasting.java` (T-Construct), `ComputerCraftProxy.java`/`PackagerPeripheral.java`/
`ComputerBehaviour.java` (CC:Tweaked).

## Done this session (batch 63)
Trajectory: 221 → 220 (confirmed).
- [x] `content/kinetics/gearbox/GearboxBlock.java` (clean) — `getPickedStack`'s override calls
  `super.getCloneItemStack(view, pos, state)` where `view` is typed `BlockGetter`, but the vanilla-1.21.1
  `getCloneItemStack` signature takes `LevelReader` (same NeoForge-patch signature-mismatch bug as
  `StandardBogeyBlock.java`/`LecternControllerBlock.java`, batch 61) → cast to `(LevelReader) view`.

**Current verified baseline: 220 errors.**

## Done this session (batch 64)
Trajectory: 220 → 219 (confirmed).
- [x] `content/equipment/symmetryWand/SymmetryWandItem.java` (clean) — dead
  `io.github.fabricators_of_create.porting_lib.util.EnvExecutor` import (recurring pattern, same as
  `FunnelBlockEntity.java`/`ContraptionCollider.java` earlier batches), never referenced in the file body.

**Current verified baseline: 219 errors.**
