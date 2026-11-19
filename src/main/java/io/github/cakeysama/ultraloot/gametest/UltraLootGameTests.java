package io.github.cakeysama.ultraloot.gametest;

import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.config.Balance;
import io.github.cakeysama.ultraloot.experience.ExperienceCapability;
import io.github.cakeysama.ultraloot.experience.IExperienceStorage;
import io.github.cakeysama.ultraloot.inventory.InventoryScanner;
import io.github.cakeysama.ultraloot.loot.LootEngine;
import io.github.cakeysama.ultraloot.loot.LootFilter;
import io.github.cakeysama.ultraloot.loot.LootProfile;
import io.github.cakeysama.ultraloot.loot.LootProfileManager;
import io.github.cakeysama.ultraloot.machine.LootGeneratorBlockEntity;
import io.github.cakeysama.ultraloot.machine.MachineFamily;
import io.github.cakeysama.ultraloot.machine.MachineStatus;
import io.github.cakeysama.ultraloot.machine.MachineTier;
import io.github.cakeysama.ultraloot.machine.MobLootGeneratorBlockEntity;
import io.github.cakeysama.ultraloot.machine.RelativeSide;
import io.github.cakeysama.ultraloot.machine.SideMode;
import io.github.cakeysama.ultraloot.mob.MobCardItem;
import io.github.cakeysama.ultraloot.mob.MobLootSimulator;
import io.github.cakeysama.ultraloot.mob.SimulatedEntityCache;
import io.github.cakeysama.ultraloot.registry.ModBlocks;
import io.github.cakeysama.ultraloot.registry.ModItems;
import io.github.cakeysama.ultraloot.upgrade.UpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;
import java.util.Set;

/**
 * In-world behaviour of both machines. Every test uses the empty 15x6x15 template {@code ultraloot:empty} with the
 * machine in its centre, so a Tier I Loot Generator (range 5) can never reach a neighbouring test.
 * <p>
 * The batch lowers the base operation times so tests finish quickly and restores them afterwards.
 */
@GameTestHolder(UltraLoot.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UltraLootGameTests {
    private static final String BATCH = "ultraloot";
    private static final String TEMPLATE = "empty";
    private static final int FAST_TICKS = 10;

    private static final BlockPos MACHINE = new BlockPos(7, 1, 7);
    /** Shipped profile for the dungeon chest table; the auto-discovered id is only used when no shipped profile covers it. */
    private static final ResourceLocation DUNGEON = UltraLoot.id("simple_dungeon");
    private static final ResourceLocation DUNGEON_AUTO = ResourceLocation.withDefaultNamespace("auto/chests/simple_dungeon");

    private static int savedLootTicks;
    private static int savedMobTicks;

    private UltraLootGameTests() {}

    @BeforeBatch(batch = BATCH)
    public static void speedUp(ServerLevel level) {
        savedLootTicks = Balance.lootBaseTicks;
        savedMobTicks = Balance.mobBaseTicks;
        Balance.lootBaseTicks = FAST_TICKS;
        Balance.mobBaseTicks = FAST_TICKS;
    }

    @AfterBatch(batch = BATCH)
    public static void restore(ServerLevel level) {
        Balance.lootBaseTicks = savedLootTicks;
        Balance.mobBaseTicks = savedMobTicks;
    }

    // ---- Helpers -----------------------------------------------------------------------------------

    private static LootGeneratorBlockEntity lootGenerator(GameTestHelper helper, boolean anyInventory) {
        LootGeneratorBlockEntity be = bareLootGenerator(helper);
        if (anyInventory) be.setTargetMode(InventoryScanner.TargetMode.ANY_INVENTORY);
        helper.assertTrue(be.selectProfile(DUNGEON) || be.selectProfile(DUNGEON_AUTO),
                "neither " + DUNGEON + " nor " + DUNGEON_AUTO + " is available");
        be.requestRescan();
        return be;
    }

    /** Tier I Loot Generator with full FE, no profile and the default target mode. */
    private static LootGeneratorBlockEntity bareLootGenerator(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModBlocks.machine(MachineFamily.LOOT_GENERATOR, MachineTier.T1).get());
        LootGeneratorBlockEntity be = helper.getBlockEntity(MACHINE);
        be.energy().setEnergy(be.energy().getMaxEnergyStored());
        return be;
    }

    private static MobLootGeneratorBlockEntity mobGenerator(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModBlocks.machine(MachineFamily.MOB_LOOT_GENERATOR, MachineTier.T1).get());
        return helper.getBlockEntity(MACHINE);
    }

    private static void insertZombieCard(GameTestHelper helper, MobLootGeneratorBlockEntity be) {
        ItemStack rest = be.cardSlot().insertItem(0, MobCardItem.create(EntityType.ZOMBIE), false);
        helper.assertTrue(rest.isEmpty(), "zombie Mob Card was rejected by the card slot");
    }

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        return helper.getBlockEntity(pos);
    }

    private static ChestBlockEntity fullChest(GameTestHelper helper, BlockPos pos) {
        ChestBlockEntity chest = chest(helper, pos);
        for (int i = 0; i < chest.getContainerSize(); i++) chest.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        return chest;
    }

    private static int itemCount(ChestBlockEntity chest) {
        int total = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) total += chest.getItem(i).getCount();
        return total;
    }

    private static int itemCount(ChestBlockEntity chest, Item item) {
        int total = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (chest.getItem(i).is(item)) total += chest.getItem(i).getCount();
        }
        return total;
    }

    private static int itemCount(IItemHandler handler) {
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) total += handler.getStackInSlot(i).getCount();
        return total;
    }

    private static boolean onlyCobblestone(ChestBlockEntity chest) {
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack s = chest.getItem(i);
            if (!s.is(Items.COBBLESTONE) || s.getCount() != 64) return false;
        }
        return true;
    }

    /** Living entities and dropped items inside the test area (players excluded). */
    private static List<Entity> worldEntities(GameTestHelper helper) {
        return helper.getLevel().getEntities((Entity) null, helper.getBounds().inflate(1),
                e -> !(e instanceof Player) && (e instanceof LivingEntity || e instanceof ItemEntity));
    }

    /** Every entity inside the test area except players (projectiles such as fishing hooks included). */
    private static List<Entity> allEntities(GameTestHelper helper) {
        return helper.getLevel().getEntities((Entity) null, helper.getBounds().inflate(1), e -> !(e instanceof Player));
    }

    private static int count(List<ItemStack> stacks, Item item) {
        return stacks.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static BlockPos abs(GameTestHelper helper, BlockPos relative) {
        return helper.absolutePos(relative);
    }

    /** FE and stats must agree: nothing is spent without being recorded, nothing recorded without being spent. */
    private static void assertEnergyAccounting(GameTestHelper helper, LootGeneratorBlockEntity be, int initial) {
        helper.assertValueEqual((long) be.energy().getEnergyStored(), initial - be.statistics().energyConsumed(), "stored FE");
        helper.assertValueEqual(be.statistics().energyConsumed(), be.statistics().operations() * be.energyPerOperation(), "FE consumed");
    }

    // ---- Loot Generator ------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void lootGeneratorFillsChestInRange(GameTestHelper helper) {
        LootGeneratorBlockEntity be = lootGenerator(helper, true);
        int initial = be.energy().getEnergyStored();
        ChestBlockEntity chest = chest(helper, MACHINE.offset(0, 0, -3));
        helper.failIfEver(() -> helper.assertTrue(worldEntities(helper).isEmpty(), "the Loot Generator must never drop items"));
        helper.succeedWhen(() -> {
            helper.assertTrue(be.statistics().operations() >= 2, "waiting for two operations");
            helper.assertTrue(itemCount(chest) > 0, "chest is still empty");
            helper.assertValueEqual((long) itemCount(chest), be.statistics().itemsGenerated(), "items in chest");
            assertEnergyAccounting(helper, be, initial);
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void lootGeneratorRespectsRange(GameTestHelper helper) {
        LootGeneratorBlockEntity be = lootGenerator(helper, true);
        int range = be.modifiers().range();
        ChestBlockEntity inRange = chest(helper, MACHINE.offset(range, 0, 0));
        ChestBlockEntity outOfRange = chest(helper, MACHINE.offset(-(range + 1), 0, 0));
        helper.failIfEver(() -> helper.assertTrue(itemCount(outOfRange) == 0, "chest outside the range received items"));
        helper.succeedWhen(() -> {
            helper.assertTrue(be.statistics().operations() >= 3, "waiting for three operations");
            helper.assertTrue(itemCount(inRange) > 0, "chest at the edge of the range is empty");
            helper.assertValueEqual(be.scanResult().targets().size(), 1, "target count");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void lootGeneratorSkipsFullChest(GameTestHelper helper) {
        LootGeneratorBlockEntity be = lootGenerator(helper, true);
        ChestBlockEntity full = fullChest(helper, MACHINE.offset(0, 0, -2));
        ChestBlockEntity empty = chest(helper, MACHINE.offset(0, 0, 3));
        helper.failIfEver(() -> helper.assertTrue(onlyCobblestone(full), "the full chest was modified"));
        helper.succeedWhen(() -> {
            helper.assertTrue(be.statistics().operations() >= 3, "waiting for three operations");
            helper.assertValueEqual((long) itemCount(empty), be.statistics().itemsGenerated(), "items in the free chest");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void lootGeneratorKeepsEnergyWhenTargetsFull(GameTestHelper helper) {
        LootGeneratorBlockEntity be = lootGenerator(helper, true);
        int initial = be.energy().getEnergyStored();
        ChestBlockEntity full = fullChest(helper, MACHINE.offset(2, 0, 0));
        helper.failIfEver(() -> helper.assertValueEqual(be.energy().getEnergyStored(), initial, "stored FE"));
        helper.runAfterDelay(80, () -> {
            helper.assertValueEqual(be.status(), MachineStatus.TARGETS_FULL, "status");
            helper.assertValueEqual(be.statistics().operations(), 0L, "operations");
            helper.assertTrue(onlyCobblestone(full), "the full chest was modified");
            helper.assertTrue(worldEntities(helper).isEmpty(), "items were dropped");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void lootGeneratorWithoutTargets(GameTestHelper helper) {
        LootGeneratorBlockEntity be = lootGenerator(helper, true);
        int initial = be.energy().getEnergyStored();
        helper.failIfEver(() -> helper.assertValueEqual(be.energy().getEnergyStored(), initial, "stored FE"));
        helper.runAfterDelay(60, () -> {
            helper.assertValueEqual(be.status(), MachineStatus.NO_TARGETS, "status");
            helper.assertValueEqual(be.statistics().operations(), 0L, "operations");
            helper.assertTrue(worldEntities(helper).isEmpty(), "items were dropped");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void lootGeneratorRoundRobin(GameTestHelper helper) {
        LootGeneratorBlockEntity be = lootGenerator(helper, true);
        ChestBlockEntity a = chest(helper, MACHINE.offset(-3, 0, 0));
        ChestBlockEntity b = chest(helper, MACHINE.offset(3, 0, 0));
        helper.succeedWhen(() -> {
            helper.assertTrue(be.statistics().operations() >= 4, "waiting for four operations");
            helper.assertTrue(itemCount(a) > 0, "first chest received nothing");
            helper.assertTrue(itemCount(b) > 0, "second chest received nothing");
            helper.assertValueEqual((long) (itemCount(a) + itemCount(b)), be.statistics().itemsGenerated(), "items in chests");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void lootGeneratorHonoursFaceConfiguration(GameTestHelper helper) {
        LootGeneratorBlockEntity be = lootGenerator(helper, true);
        // Facing north: the front (north, z-1) is an Item Target face, the back (south, z+1) is Energy Input.
        ChestBlockEntity front = chest(helper, MACHINE.north());
        ChestBlockEntity back = chest(helper, MACHINE.south());
        helper.failIfEver(() -> helper.assertTrue(itemCount(back) == 0, "chest on the Energy Input face received items"));
        helper.succeedWhen(() -> {
            helper.assertTrue(be.statistics().operations() >= 2, "waiting for two operations");
            helper.assertTrue(itemCount(front) > 0, "chest on the Item Target face is empty");
            helper.assertTrue(be.scanResult().entries().stream().anyMatch(e -> e.state() == InventoryScanner.EntryState.FACE_DISABLED
                    && e.pos().equals(abs(helper, MACHINE.south()))), "back chest is not reported as FACE_DISABLED");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void lootGeneratorStopsUsingFaceSetToNone(GameTestHelper helper) {
        LootGeneratorBlockEntity be = lootGenerator(helper, true);
        be.setSide(RelativeSide.FRONT, SideMode.NONE);
        ChestBlockEntity front = chest(helper, MACHINE.north());
        helper.failIfEver(() -> helper.assertTrue(itemCount(front) == 0, "chest on a disabled face received items"));
        helper.runAfterDelay(60, () -> {
            helper.assertValueEqual(be.status(), MachineStatus.NO_TARGETS, "status");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void lootGeneratorCountsDoubleChestOnce(GameTestHelper helper) {
        LootGeneratorBlockEntity be = lootGenerator(helper, true);
        BlockPos left = MACHINE.offset(0, 0, -3);
        // Facing north, a LEFT half connects to the east (clockwise).
        helper.setBlock(left, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.LEFT));
        helper.setBlock(left.east(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.RIGHT));
        ChestBlockEntity a = helper.getBlockEntity(left);
        ChestBlockEntity b = helper.getBlockEntity(left.east());
        helper.succeedWhen(() -> {
            helper.assertTrue(be.statistics().operations() >= 1, "waiting for an operation");
            helper.assertValueEqual(be.scanResult().targets().size(), 1, "targets for one double chest");
            helper.assertValueEqual((long) (itemCount(a) + itemCount(b)), be.statistics().itemsGenerated(), "items in the double chest");
        });
    }

    /** Default target mode: only {@code #ultraloot:storage_targets} blocks (requires the shipped block tag). */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void lootGeneratorStorageOnlyModeUsesChests(GameTestHelper helper) {
        LootGeneratorBlockEntity be = lootGenerator(helper, false);
        helper.assertValueEqual(be.targetMode(), InventoryScanner.TargetMode.STORAGE_ONLY, "default target mode");
        ChestBlockEntity chest = chest(helper, MACHINE.offset(0, 0, 3));
        helper.succeedWhen(() -> {
            helper.assertTrue(be.statistics().operations() >= 1, "no operation ran (status " + be.status() + ")");
            helper.assertTrue(itemCount(chest) > 0, "chest is empty");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void lootGeneratorNeedsEnergy(GameTestHelper helper) {
        LootGeneratorBlockEntity be = lootGenerator(helper, true);
        be.energy().setEnergy(be.energyPerOperation() - 1);
        ChestBlockEntity chest = chest(helper, MACHINE.offset(0, 0, 3));
        helper.runAfterDelay(40, () -> {
            helper.assertValueEqual(be.status(), MachineStatus.NO_ENERGY, "status");
            helper.assertValueEqual(be.progress(), 0, "progress");
            helper.assertTrue(itemCount(chest) == 0, "items were generated without energy");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void lootGeneratorProfileValidation(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModBlocks.machine(MachineFamily.LOOT_GENERATOR, MachineTier.T1).get());
        LootGeneratorBlockEntity be = helper.getBlockEntity(MACHINE);
        be.energy().setEnergy(be.energy().getMaxEnergyStored());
        helper.assertFalse(be.selectProfile(UltraLoot.id("does_not_exist")), "an unknown profile was accepted");
        helper.assertTrue(be.profile() == null, "profile set after a rejected selection");
        helper.runAfterDelay(5, () -> {
            helper.assertValueEqual(be.status(), MachineStatus.NO_PROFILE, "status");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void lootGeneratorCapabilities(GameTestHelper helper) {
        LootGeneratorBlockEntity be = lootGenerator(helper, true);
        be.energy().setEnergy(0);
        BlockPos pos = abs(helper, MACHINE);
        ServerLevel level = helper.getLevel();
        IEnergyStorage back = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, Direction.SOUTH);
        helper.assertTrue(back != null, "no energy capability on the back (Energy Input) face");
        helper.assertValueEqual(back.receiveEnergy(100_000, false), MachineTier.T1.energyInput(), "accepted FE per call");
        helper.assertValueEqual(back.extractEnergy(100, false), 0, "extracted FE");
        helper.assertTrue(level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, Direction.NORTH) == null,
                "energy capability exposed on an Item Target face");
        for (Direction d : Direction.values()) {
            helper.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK, pos, d) == null, "item handler exposed on " + d);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void lootGeneratorPersistence(GameTestHelper helper) {
        LootGeneratorBlockEntity be = lootGenerator(helper, true);
        be.upgrades().insertItem(UpgradeType.LOOT.slot(), new ItemStack(ModItems.card(UpgradeType.LOOT, 2).get()), false);
        be.upgrades().insertItem(UpgradeType.SPEED.slot(), new ItemStack(ModItems.card(UpgradeType.SPEED, 1).get()), false);
        helper.assertTrue(be.setFilter(LootFilter.FOOD), "filter rejected despite a Loot Card");
        be.setSide(RelativeSide.TOP, SideMode.NONE);
        be.setSide(RelativeSide.FRONT, SideMode.ENERGY_INPUT);
        be.energy().setEnergy(4_321);
        be.statistics().recordOperation(800, 0, 12, 3);

        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        CompoundTag tag = be.saveWithFullMetadata(registries);
        BlockEntity loaded = BlockEntity.loadStatic(be.getBlockPos(), be.getBlockState(), tag, registries);
        helper.assertTrue(loaded instanceof LootGeneratorBlockEntity, "loaded block entity has the wrong type");
        LootGeneratorBlockEntity copy = (LootGeneratorBlockEntity) loaded;

        helper.assertValueEqual(copy.energy().getEnergyStored(), 4_321, "energy");
        helper.assertValueEqual(copy.upgrades().levels(), be.upgrades().levels(), "cards");
        helper.assertValueEqual(copy.modifiers(), be.modifiers(), "modifiers");
        helper.assertValueEqual(copy.filter(), LootFilter.FOOD, "filter");
        helper.assertValueEqual(copy.targetMode(), InventoryScanner.TargetMode.ANY_INVENTORY, "target mode");
        helper.assertValueEqual(java.util.Arrays.toString(copy.sides().toArray()), java.util.Arrays.toString(be.sides().toArray()), "sides");
        helper.assertValueEqual(copy.statistics().save(), be.statistics().save(), "statistics");
        helper.assertValueEqual(copy.saveWithFullMetadata(registries), tag, "full NBT round trip");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void lootGeneratorFishingProfileFillsChest(GameTestHelper helper) {
        LootGeneratorBlockEntity be = bareLootGenerator(helper);
        be.setTargetMode(InventoryScanner.TargetMode.ANY_INVENTORY);
        ResourceLocation fishing = UltraLoot.id("fishing");
        helper.assertTrue(be.selectProfile(fishing), "profile " + fishing + " is not available");
        ChestBlockEntity chest = chest(helper, MACHINE.offset(0, 0, 3));
        be.requestRescan();
        helper.assertTrue(allEntities(helper).isEmpty(), "test area is not empty");
        helper.failIfEver(() -> helper.assertTrue(allEntities(helper).isEmpty(),
                "an entity (fishing hook, item...) was added to the world: " + allEntities(helper)));
        helper.succeedWhen(() -> {
            helper.assertTrue(be.statistics().operations() >= 3, "waiting for three operations (status " + be.status() + ")");
            helper.assertTrue(itemCount(chest) > 0, "chest is empty");
            helper.assertValueEqual((long) itemCount(chest), be.statistics().itemsGenerated(), "items in chest");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void autoProfilesAreGatedByTierAndCost(GameTestHelper helper) {
        List<LootProfile> auto = LootProfileManager.get().all(helper.getLevel().getServer()).stream()
                .filter(LootProfile::autoDiscovered).toList();
        if (auto.isEmpty()) {
            UltraLoot.LOGGER.warn("GameTest autoProfilesAreGatedByTierAndCost skipped: no auto-discovered profile exists "
                    + "(every chests/* table is covered by a shipped profile)");
            helper.succeed();
            return;
        }
        for (LootProfile profile : auto) {
            helper.assertTrue(profile.id().getPath().startsWith("auto/"), "auto profile id without auto/ prefix: " + profile.id());
            helper.assertValueEqual(profile.minTier(), Balance.autoProfileMinTier, "min tier of " + profile.id());
            helper.assertValueEqual(profile.energyMultiplier(), Balance.autoProfileEnergyMultiplier, "energy multiplier of " + profile.id());
        }
        LootGeneratorBlockEntity be = bareLootGenerator(helper);
        LootProfile first = auto.getFirst();
        if (Balance.autoProfileMinTier > MachineTier.T1.number()) {
            helper.assertFalse(be.selectProfile(first.id()), "a Tier I machine selected the auto profile " + first.id());
            helper.assertTrue(be.profile() == null, "profile set after a rejected selection");
        } else {
            helper.assertTrue(be.selectProfile(first.id()), "a Tier I machine could not select " + first.id());
        }
        UltraLoot.LOGGER.info("GameTest autoProfilesAreGatedByTierAndCost checked {} auto profiles", auto.size());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void explorationMapTablesAreNeverProfiles(GameTestHelper helper) {
        Set<ResourceLocation> forbidden = Set.of(
                ResourceLocation.withDefaultNamespace("chests/shipwreck_map"),
                ResourceLocation.withDefaultNamespace("chests/underwater_ruin_small"),
                ResourceLocation.withDefaultNamespace("chests/underwater_ruin_big"));
        var profiles = LootProfileManager.get().all(helper.getLevel().getServer());
        helper.assertFalse(profiles.isEmpty(), "no loot profiles loaded");
        for (LootProfile profile : profiles) {
            helper.assertFalse(forbidden.contains(profile.table().location()),
                    "profile " + profile.id() + " uses exploration-map table " + profile.table().location());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void lootGeneratorCannotSelectLockedProfile(GameTestHelper helper) {
        ResourceLocation id = UltraLoot.id("end_city_treasure");
        LootProfile profile = LootProfileManager.get().get(helper.getLevel().getServer(), id);
        helper.assertTrue(profile != null, "profile " + id + " is not available");
        helper.assertValueEqual(profile.minTier(), 4, "min tier of " + id);
        LootGeneratorBlockEntity be = bareLootGenerator(helper);
        helper.assertFalse(be.selectProfile(id), "a Tier I machine selected a Tier IV profile");
        int fe0 = be.energy().getEnergyStored();
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(be.profile() == null, "profile set after a rejected selection");
            helper.assertValueEqual(be.status(), MachineStatus.NO_PROFILE, "status");
            helper.assertValueEqual(be.energy().getEnergyStored(), fe0, "stored FE");
            helper.succeed();
        });
    }

    // ---- Mob simulation -------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void magmaCubeSimulationDropsMagmaCream(GameTestHelper helper) {
        LootEngine.Result result = MobLootSimulator.simulate(helper.getLevel(), abs(helper, MACHINE), EntityType.MAGMA_CUBE,
                40, 3, LootFilter.ALL, new SimulatedEntityCache());
        helper.assertTrue(result != null, "magma cube cannot be simulated");
        helper.assertTrue(count(result.items(), Items.MAGMA_CREAM) > 0,
                "40 simulated magma cube kills with Looting III dropped no magma cream: " + result.items());
        helper.assertTrue(allEntities(helper).isEmpty(), "simulation added an entity to the world");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void slimeSimulationDropsSlimeBalls(GameTestHelper helper) {
        LootEngine.Result result = MobLootSimulator.simulate(helper.getLevel(), abs(helper, MACHINE), EntityType.SLIME,
                40, 3, LootFilter.ALL, new SimulatedEntityCache());
        helper.assertTrue(result != null, "slime cannot be simulated");
        helper.assertTrue(count(result.items(), Items.SLIME_BALL) > 0,
                "40 simulated slime kills with Looting III dropped no slime balls: " + result.items());
        helper.assertTrue(allEntities(helper).isEmpty(), "simulation added an entity to the world");
        helper.succeed();
    }

    // ---- Mob Loot Generator -------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 300)
    public static void mobGeneratorProducesDropsWithoutSpawning(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        insertZombieCard(helper, be);
        be.experience().setExperience(be.experience().getCapacity());
        be.energy().setEnergy(be.energy().getMaxEnergyStored());
        int xp0 = be.experience().getExperience();
        int fe0 = be.energy().getEnergyStored();
        int xpPerOp = be.experiencePerOperation();
        helper.assertTrue(xpPerOp > 0, "a zombie operation costs no XP");
        helper.assertTrue(worldEntities(helper).isEmpty(), "test area is not empty");
        helper.failIfEver(() -> helper.assertTrue(worldEntities(helper).isEmpty(), "an entity or item was added to the world"));
        helper.succeedWhen(() -> {
            long ops = be.statistics().operations();
            helper.assertTrue(ops >= 6, "waiting for six operations");
            helper.assertTrue(itemCount(be.output()) > 0, "no drops in the output");
            helper.assertValueEqual((long) itemCount(be.output()), be.statistics().itemsGenerated(), "items in output");
            helper.assertValueEqual((long) be.experience().getExperience(), xp0 - ops * xpPerOp, "stored XP");
            helper.assertValueEqual(be.statistics().experienceUsed(), ops * xpPerOp, "XP used");
            helper.assertValueEqual((long) be.energy().getEnergyStored(), fe0 - be.statistics().energyConsumed(), "stored FE");
            helper.assertValueEqual(be.statistics().energyConsumed(), ops * be.energyPerOperation(), "FE consumed");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void mobGeneratorPausesWhenOutputFull(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        insertZombieCard(helper, be);
        be.experience().setExperience(500);
        be.energy().setEnergy(be.energy().getMaxEnergyStored());
        for (int i = 0; i < be.output().getSlots(); i++) be.output().setStackInSlot(i, new ItemStack(Items.COBBLESTONE, 64));
        int fe0 = be.energy().getEnergyStored();
        helper.failIfEver(() -> {
            helper.assertValueEqual(be.energy().getEnergyStored(), fe0, "stored FE");
            helper.assertValueEqual(be.experience().getExperience(), 500, "stored XP");
        });
        helper.runAfterDelay(60, () -> {
            helper.assertValueEqual(be.status(), MachineStatus.OUTPUT_FULL, "status");
            helper.assertValueEqual(be.statistics().operations(), 0L, "operations");
            helper.assertTrue(worldEntities(helper).isEmpty(), "items were dropped");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void mobGeneratorExportsThroughItemOutputFace(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        // Default configuration: the bottom face is Item Output.
        ChestBlockEntity below = chest(helper, MACHINE.below());
        be.output().setStackInSlot(0, new ItemStack(Items.ROTTEN_FLESH, 40));
        be.output().setStackInSlot(5, new ItemStack(Items.IRON_INGOT, 3));
        be.output().setStackInSlot(9, new ItemStack(Items.IRON_SWORD));
        helper.succeedWhen(() -> {
            helper.assertValueEqual(itemCount(below, Items.ROTTEN_FLESH), 40, "rotten flesh exported");
            helper.assertValueEqual(itemCount(below, Items.IRON_INGOT), 3, "iron exported");
            helper.assertValueEqual(itemCount(below, Items.IRON_SWORD), 1, "sword exported");
            helper.assertValueEqual(itemCount(be.output()), 0, "items left in output");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void mobGeneratorDoesNotExportThroughOtherFaces(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        be.setSide(RelativeSide.BOTTOM, SideMode.NONE);
        ChestBlockEntity below = chest(helper, MACHINE.below());
        ChestBlockEntity front = chest(helper, MACHINE.north());
        be.output().setStackInSlot(0, new ItemStack(Items.ROTTEN_FLESH, 10));
        helper.failIfEver(() -> helper.assertTrue(itemCount(below) + itemCount(front) == 0, "items left through a non-output face"));
        helper.runAfterDelay(40, () -> {
            helper.assertValueEqual(itemCount(be.output()), 10, "items in output");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void mobGeneratorNeedsXp(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        insertZombieCard(helper, be);
        be.energy().setEnergy(be.energy().getMaxEnergyStored());
        be.experience().setExperience(be.experiencePerOperation() - 1);
        int fe0 = be.energy().getEnergyStored();
        helper.runAfterDelay(40, () -> {
            helper.assertValueEqual(be.status(), MachineStatus.NO_XP, "status");
            helper.assertValueEqual(be.energy().getEnergyStored(), fe0, "stored FE");
            helper.assertValueEqual(itemCount(be.output()), 0, "items in output");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void mobGeneratorNeedsCard(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        be.energy().setEnergy(be.energy().getMaxEnergyStored());
        be.experience().setExperience(500);
        helper.assertFalse(be.cardSlot().isItemValid(0, new ItemStack(ModItems.MOB_CARD.get())), "an unbound Mob Card was accepted");
        helper.assertFalse(be.cardSlot().isItemValid(0, new ItemStack(Items.STONE)), "stone was accepted as a Mob Card");
        helper.runAfterDelay(20, () -> {
            helper.assertValueEqual(be.status(), MachineStatus.NO_MOB_CARD, "status");
            helper.assertValueEqual(be.experience().getExperience(), 500, "stored XP");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void mobGeneratorDrainsExperienceBottles(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        helper.assertTrue(be.xpSlot().insertItem(0, new ItemStack(Items.EXPERIENCE_BOTTLE, 3), false).isEmpty(), "bottles rejected");
        helper.succeedWhen(() -> {
            helper.assertValueEqual(be.experience().getExperience(), 3 * Balance.XP_BOTTLE_POINTS, "stored XP");
            helper.assertTrue(be.xpSlot().getStackInSlot(0).isEmpty(), "bottles left in the slot");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void mobGeneratorRangeCardCollectsOrbs(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        be.upgrades().insertItem(UpgradeType.RANGE.slot(), new ItemStack(ModItems.card(UpgradeType.RANGE, 1).get()), false);
        helper.assertValueEqual(be.modifiers().orbCollectRadius(), Balance.ORB_COLLECT_RADIUS[0], "orb radius");
        BlockPos pos = abs(helper, MACHINE);
        ExperienceOrb orb = new ExperienceOrb(helper.getLevel(), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 10);
        orb.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); // vanilla orbs spawn with random motion
        helper.getLevel().addFreshEntity(orb);
        helper.succeedWhen(() -> {
            helper.assertValueEqual(be.experience().getExperience(), 10, "stored XP (orb removed=" + orb.isRemoved()
                    + ", orb at " + helper.relativeVec(orb.position()) + ", count=" + orb.count + ", radius=" + be.modifiers().orbCollectRadius() + ")");
            helper.assertTrue(orb.isRemoved(), "orb still in the world");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void mobGeneratorIgnoresOrbsWithoutRangeCard(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        BlockPos pos = abs(helper, MACHINE);
        ExperienceOrb orb = new ExperienceOrb(helper.getLevel(), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 10);
        orb.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); // vanilla orbs spawn with random motion
        helper.getLevel().addFreshEntity(orb);
        helper.runAfterDelay(50, () -> {
            helper.assertValueEqual(be.experience().getExperience(), 0, "stored XP");
            helper.assertFalse(orb.isRemoved(), "orb was absorbed without a Range Card");
            orb.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void mobGeneratorCapabilities(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        BlockPos pos = abs(helper, MACHINE);
        ServerLevel level = helper.getLevel();
        int tier = MachineTier.T1.index();

        // Facing north: back = south (Energy Input), left = east (XP Input), bottom = Item Output, front = none.
        IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, Direction.SOUTH);
        helper.assertTrue(energy != null, "no energy capability on the back face");
        helper.assertValueEqual(energy.receiveEnergy(1_000_000, false), MachineTier.T1.energyInput(), "accepted FE per call");
        helper.assertTrue(level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, Direction.NORTH) == null, "energy on the front face");

        IExperienceStorage xp = level.getCapability(ExperienceCapability.BLOCK, pos, Direction.EAST);
        helper.assertTrue(xp != null, "no XP capability on the XP Input face");
        helper.assertValueEqual(xp.receiveExperience(1_000_000, false), Balance.MOB_XP_INPUT[tier], "accepted XP per call");
        helper.assertValueEqual(xp.extractExperience(10, false), 0, "extracted XP");
        helper.assertTrue(level.getCapability(ExperienceCapability.BLOCK, pos, Direction.NORTH) == null, "XP capability on the front face");

        IFluidHandler fluid = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.EAST);
        helper.assertTrue(fluid != null, "no fluid adapter on the XP Input face");
        helper.assertValueEqual(fluid.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE), 0, "water accepted");

        IItemHandler xpItems = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.EAST);
        helper.assertTrue(xpItems != null, "no item handler on the XP Input face");
        helper.assertValueEqual(xpItems.insertItem(0, new ItemStack(Items.STONE), false).getCount(), 1, "stone rejected count");
        helper.assertTrue(xpItems.insertItem(0, new ItemStack(Items.EXPERIENCE_BOTTLE), false).isEmpty(), "bottle rejected");
        helper.assertTrue(xpItems.extractItem(0, 1, false).isEmpty(), "a filled XP item was extracted through the XP Input face");

        be.output().setStackInSlot(0, new ItemStack(Items.ROTTEN_FLESH, 5));
        IItemHandler out = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
        helper.assertTrue(out != null, "no item handler on the Item Output face");
        helper.assertValueEqual(out.insertItem(1, new ItemStack(Items.DIAMOND), false).getCount(), 1, "insert through output face");
        helper.assertValueEqual(out.extractItem(0, 2, false).getCount(), 2, "extracted from output");
        helper.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.NORTH) == null, "item handler on the front face");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void mobGeneratorPersistence(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        insertZombieCard(helper, be);
        be.upgrades().insertItem(UpgradeType.QUANTITY.slot(), new ItemStack(ModItems.card(UpgradeType.QUANTITY, 3).get()), false);
        be.upgrades().insertItem(UpgradeType.EFFICIENCY.slot(), new ItemStack(ModItems.card(UpgradeType.EFFICIENCY, 1).get()), false);
        be.experience().setExperience(777);
        be.energy().setEnergy(6_543);
        be.output().setStackInSlot(3, new ItemStack(Items.ROTTEN_FLESH, 17));
        be.output().setStackInSlot(26, new ItemStack(Items.IRON_INGOT, 2));
        be.xpSlot().setStackInSlot(0, new ItemStack(Items.EXPERIENCE_BOTTLE, 4));
        be.setSide(RelativeSide.FRONT, SideMode.ITEM_OUTPUT);
        be.setSide(RelativeSide.TOP, SideMode.ENERGY_INPUT);
        be.statistics().recordOperation(1_200, 8, 2, 0);

        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        CompoundTag tag = be.saveWithFullMetadata(registries);
        BlockEntity loaded = BlockEntity.loadStatic(be.getBlockPos(), be.getBlockState(), tag, registries);
        helper.assertTrue(loaded instanceof MobLootGeneratorBlockEntity, "loaded block entity has the wrong type");
        MobLootGeneratorBlockEntity copy = (MobLootGeneratorBlockEntity) loaded;

        helper.assertValueEqual(copy.experience().getExperience(), 777, "XP");
        helper.assertValueEqual(copy.energy().getEnergyStored(), 6_543, "energy");
        helper.assertValueEqual(copy.mob().orElse(null), EntityType.ZOMBIE, "mob card");
        helper.assertValueEqual(copy.upgrades().levels(), be.upgrades().levels(), "cards");
        helper.assertValueEqual(copy.modifiers(), be.modifiers(), "modifiers");
        helper.assertTrue(ItemStack.matches(copy.output().getStackInSlot(3), be.output().getStackInSlot(3)), "output slot 3");
        helper.assertTrue(ItemStack.matches(copy.output().getStackInSlot(26), be.output().getStackInSlot(26)), "output slot 26");
        helper.assertTrue(ItemStack.matches(copy.xpSlot().getStackInSlot(0), be.xpSlot().getStackInSlot(0)), "XP slot");
        helper.assertValueEqual(java.util.Arrays.toString(copy.sides().toArray()), java.util.Arrays.toString(be.sides().toArray()), "sides");
        helper.assertValueEqual(copy.statistics().save(), be.statistics().save(), "statistics");
        helper.assertValueEqual(copy.saveWithFullMetadata(registries), tag, "full NBT round trip");
        helper.succeed();
    }
}
