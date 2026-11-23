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
import io.github.cakeysama.ultraloot.machine.CardRestorerBlockEntity;
import io.github.cakeysama.ultraloot.machine.CardRestorerStatus;
import io.github.cakeysama.ultraloot.machine.MachineFamily;
import io.github.cakeysama.ultraloot.machine.MachineStatus;
import io.github.cakeysama.ultraloot.machine.MachineTier;
import io.github.cakeysama.ultraloot.machine.MobLootGeneratorBlockEntity;
import io.github.cakeysama.ultraloot.machine.RelativeSide;
import io.github.cakeysama.ultraloot.machine.SideMode;
import io.github.cakeysama.ultraloot.mob.CardWear;
import io.github.cakeysama.ultraloot.mob.HybridLootResolver;
import io.github.cakeysama.ultraloot.mob.MobCardItem;
import io.github.cakeysama.ultraloot.mob.MobLootSimulator;
import io.github.cakeysama.ultraloot.mob.MobSimulationData;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

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

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 240)
    public static void lootGeneratorPickedOnlyUsesPickedChest(GameTestHelper helper) {
        LootGeneratorBlockEntity be = lootGenerator(helper, true);
        ChestBlockEntity picked = chest(helper, MACHINE.offset(-3, 0, 0));
        ChestBlockEntity other = chest(helper, MACHINE.offset(3, 0, 0));
        be.setPickedOnly(true);
        helper.failIfEver(() -> helper.assertTrue(itemCount(other) == 0, "an unpicked chest received items"));
        helper.runAfterDelay(5, () -> helper.assertTrue(be.togglePicked(abs(helper, MACHINE.offset(-3, 0, 0))),
                "picking a scanned chest was refused"));
        helper.succeedWhen(() -> {
            helper.assertTrue(be.statistics().operations() >= 3, "waiting for three operations");
            helper.assertValueEqual((long) itemCount(picked), be.statistics().itemsGenerated(), "items in the picked chest");
            helper.assertTrue(be.scanResult().entries().stream().anyMatch(e -> e.state() == InventoryScanner.EntryState.NOT_PICKED
                    && e.pos().equals(abs(helper, MACHINE.offset(3, 0, 0)))), "unpicked chest is not reported as NOT_PICKED");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void lootGeneratorPickedOnlyWithNothingPicked(GameTestHelper helper) {
        LootGeneratorBlockEntity be = lootGenerator(helper, true);
        int initial = be.energy().getEnergyStored();
        ChestBlockEntity chest = chest(helper, MACHINE.offset(0, 0, -3));
        be.setPickedOnly(true);
        helper.runAfterDelay(60, () -> {
            helper.assertValueEqual(be.status(), MachineStatus.NO_PICKED_TARGETS, "status");
            helper.assertValueEqual(be.energy().getEnergyStored(), initial, "stored FE");
            helper.assertValueEqual(itemCount(chest), 0, "items in the unpicked chest");
            // Back to every inventory in range: the same chest is used again.
            be.setPickedOnly(false);
            helper.succeedWhen(() -> helper.assertTrue(itemCount(chest) > 0, "chest unused after switching back to all"));
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void lootGeneratorPickedSelectionRules(GameTestHelper helper) {
        LootGeneratorBlockEntity be = lootGenerator(helper, true);
        BlockPos left = MACHINE.offset(0, 0, -3);
        helper.setBlock(left, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.LEFT));
        helper.setBlock(left.east(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.RIGHT));
        be.setPickedOnly(true);
        helper.runAfterDelay(5, () -> {
            // A position the scan did not list can't be picked (a client can't store arbitrary positions).
            helper.assertFalse(be.togglePicked(abs(helper, MACHINE.offset(1, 0, 1))), "picked a position that is not an inventory");
            helper.assertValueEqual(be.scanResult().entries().size(), 1, "double chest listed once");
            BlockPos listed = be.scanResult().entries().get(0).pos();
            helper.assertTrue(be.togglePicked(listed), "picking the double chest was refused");
            helper.assertValueEqual(be.pickedCount(), 1, "picked count");

            // The choice is saved and loaded with the machine.
            var registries = helper.getLevel().registryAccess();
            var saved = be.saveWithoutMetadata(registries);
            helper.assertTrue(be.togglePicked(listed), "unpicking was refused");
            helper.assertValueEqual(be.pickedCount(), 0, "picked count after unpicking");
            be.loadWithComponents(saved, registries);
            helper.assertValueEqual(be.pickedCount(), 1, "picked count after loading");
            helper.assertTrue(be.pickedOnly(), "Picked only mode lost on load");
            helper.assertTrue(be.pickedPositions().contains(listed), "picked position lost on load");
            helper.succeedWhen(() -> helper.assertTrue(be.statistics().operations() >= 1, "picked double chest not used"));
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

    /**
     * P3-03: target capability caches are created once per target and reused by later scans; targets that leave
     * the scan are pruned and a reused cache follows the block that replaced the old one.
     */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void inventoryScanReusesCapabilityCaches(GameTestHelper helper) {
        LootGeneratorBlockEntity be = bareLootGenerator(helper);
        BlockPos aRel = MACHINE.offset(-3, 0, 0);
        BlockPos bRel = MACHINE.offset(3, 0, 0);
        chest(helper, aRel);
        chest(helper, bRel);
        ServerLevel level = helper.getLevel();
        BlockPos origin = abs(helper, MACHINE);
        int range = be.modifiers().range();
        InventoryScanner.TargetMode mode = InventoryScanner.TargetMode.ANY_INVENTORY;
        int scans = 20;

        // Previous behaviour: a fresh BlockCapabilityCache per target on every scan.
        long start = InventoryScanner.totalCacheCreations();
        for (int i = 0; i < scans; i++) InventoryScanner.scan(level, origin, be.facing(), be.sides(), range, mode);
        long uncached = InventoryScanner.totalCacheCreations() - start;
        helper.assertValueEqual(uncached, 2L * scans, "creations without persistent caches");

        // Persistent caches: creations stop growing once every target has one.
        InventoryScanner.TargetCaches caches = new InventoryScanner.TargetCaches();
        start = InventoryScanner.totalCacheCreations();
        InventoryScanner.Result result = InventoryScanner.Result.EMPTY;
        for (int i = 0; i < scans; i++) {
            result = InventoryScanner.scan(level, origin, be.facing(), be.sides(), range, mode, caches);
            helper.assertValueEqual(result.targets().size(), 2, "targets in scan " + i);
            helper.assertValueEqual(caches.creations(), 2L, "creations after scan " + i);
        }
        long cached = InventoryScanner.totalCacheCreations() - start;
        helper.assertValueEqual(cached, 2L, "creations with persistent caches");
        helper.assertValueEqual(caches.size(), 2, "caches held");
        UltraLoot.LOGGER.info("GameTest inventoryScanReusesCapabilityCaches: {} scans x 2 targets -> {} cache creations before, {} after",
                scans, uncached, cached);

        // A target that disappears is pruned; the remaining cache is kept.
        helper.setBlock(bRel, Blocks.AIR);
        result = InventoryScanner.scan(level, origin, be.facing(), be.sides(), range, mode, caches);
        helper.assertValueEqual(result.targets().size(), 1, "targets after removing a chest");
        helper.assertValueEqual(caches.size(), 1, "caches held after removing a chest");
        helper.assertValueEqual(caches.creations(), 2L, "creations after removing a chest");

        // Replacing the remaining chest reuses its cache, which NeoForge invalidated: it now sees the new chest.
        helper.setBlock(aRel, Blocks.AIR);
        ChestBlockEntity replacement = chest(helper, aRel);
        replacement.setItem(0, new ItemStack(Items.DIAMOND, 5));
        result = InventoryScanner.scan(level, origin, be.facing(), be.sides(), range, mode, caches);
        helper.assertValueEqual(result.targets().size(), 1, "targets after replacing a chest");
        helper.assertValueEqual(caches.creations(), 2L, "creations after replacing a chest");
        IItemHandler handler = result.targets().getFirst().handler();
        helper.assertTrue(handler != null && handler.getStackInSlot(0).is(Items.DIAMOND), "reused cache does not see the new chest");

        // A target coming back gets a new cache; clear() drops everything.
        chest(helper, bRel);
        InventoryScanner.scan(level, origin, be.facing(), be.sides(), range, mode, caches);
        helper.assertValueEqual(caches.creations(), 3L, "creations after a chest came back");
        caches.clear();
        helper.assertValueEqual(caches.size(), 0, "caches held after clear");

        // The machine owns its caches and drops them when its faces change.
        be.setTargetMode(mode);
        be.tickServer(level);
        helper.assertTrue(be.targetCaches().size() > 0, "machine scan did not populate its caches (status " + be.status() + ")");
        be.setSide(RelativeSide.TOP, SideMode.NONE);
        helper.assertValueEqual(be.targetCaches().size(), 0, "machine caches after a face change");
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

    /** Simulates at the test machine position; null when the mob cannot be simulated. */
    @Nullable
    private static LootEngine.Result simulate(GameTestHelper helper, EntityType<?> type, int kills, int looting,
                                              MobSimulationData data, double decay) {
        return MobLootSimulator.simulate(helper.getLevel(), abs(helper, MACHINE), type, kills, looting, LootFilter.ALL,
                new SimulatedEntityCache(), data, decay).result();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void magmaCubeSimulationDropsMagmaCream(GameTestHelper helper) {
        LootEngine.Result result = simulate(helper, EntityType.MAGMA_CUBE, 40, 3, MobSimulationData.of(EntityType.MAGMA_CUBE), 0);
        helper.assertTrue(result != null, "magma cube cannot be simulated");
        helper.assertTrue(count(result.items(), Items.MAGMA_CREAM) > 0,
                "40 simulated magma cube kills with Looting III dropped no magma cream: " + result.items());
        helper.assertTrue(allEntities(helper).isEmpty(), "simulation added an entity to the world");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void slimeSimulationDropsSlimeBalls(GameTestHelper helper) {
        LootEngine.Result result = simulate(helper, EntityType.SLIME, 40, 3, MobSimulationData.of(EntityType.SLIME), 0);
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

    /**
     * Dynamic FE buffer (BALANCE 7.2, replaces the P1-01 deadlock): an evoker card costs 1,200 x 10 = 12,000 FE per
     * operation on a Tier I machine, more than the 10,000 FE base buffer. The buffer must grow to one operation, the
     * machine must run, and the FE must be accounted exactly.
     */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void mobGeneratorBufferGrowsToOneOperation(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        helper.assertTrue(be.cardSlot().insertItem(0, MobCardItem.create(EntityType.EVOKER), false).isEmpty(), "evoker card rejected");
        int base = be.energy().baseCapacity();
        int cost = be.energyPerOperation();
        helper.assertTrue(cost > base, "test setup: cost " + cost + " FE should exceed the " + base + " FE base buffer");
        be.experience().setExperience(be.experience().getCapacity());
        be.tickServer(helper.getLevel());
        helper.assertTrue(be.energy().getMaxEnergyStored() >= cost, "buffer did not grow: " + be.energy().getMaxEnergyStored());
        IEnergyStorage input = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, abs(helper, MACHINE), Direction.SOUTH);
        helper.assertTrue(input != null && input.getMaxEnergyStored() >= cost, "external networks see the old capacity");
        be.energy().setEnergy(cost);
        helper.succeedWhen(() -> {
            helper.assertTrue(be.statistics().operations() >= 1, "no operation with a grown buffer");
            helper.assertValueEqual(be.statistics().energyConsumed(), be.statistics().operations() * (long) cost, "FE consumed");
            helper.assertValueEqual(be.energy().getEnergyStored(), (int) (cost - be.statistics().energyConsumed()), "stored FE");
        });
    }

    /** Removing the expensive card shrinks the buffer back without discarding stored FE. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void mobGeneratorBufferShrinkKeepsEnergy(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        be.cardSlot().insertItem(0, MobCardItem.create(EntityType.EVOKER), false);
        be.tickServer(helper.getLevel());
        be.energy().setEnergy(11_000);
        be.cardSlot().extractItem(0, 1, false);
        be.tickServer(helper.getLevel());
        helper.assertValueEqual(be.energy().capacity(), be.energy().baseCapacity(), "capacity after removing the card");
        helper.assertValueEqual(be.energy().getEnergyStored(), 11_000, "stored FE after the buffer shrank");
        helper.assertValueEqual(be.energy().getMaxEnergyStored(), 11_000, "reported max is never below stored FE");
        helper.assertValueEqual(be.energy().receiveEnergy(100, false), 0, "FE accepted above capacity");
        helper.succeed();
    }

    /** ENERGY_BUFFER_TOO_SMALL only above Balance.MAX_OPERATION_ENERGY; nothing is consumed. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void mobGeneratorReportsCostAboveHardCap(GameTestHelper helper) {
        int saved = Balance.mobEnergyPerKill[0];
        Balance.mobEnergyPerKill[0] = 20_000_000; // x 10 (evoker) = 200,000,000 FE, above the 100,000,000 cap
        try {
            MobLootGeneratorBlockEntity be = mobGenerator(helper);
            be.cardSlot().insertItem(0, MobCardItem.create(EntityType.EVOKER), false);
            be.experience().setExperience(be.experience().getCapacity());
            be.energy().setEnergy(Balance.MAX_OPERATION_ENERGY);
            int xp0 = be.experience().getExperience();
            be.tickServer(helper.getLevel());
            helper.assertValueEqual(be.status(), MachineStatus.ENERGY_BUFFER_TOO_SMALL, "status");
            helper.assertValueEqual(be.energy().getEnergyStored(), Balance.MAX_OPERATION_ENERGY, "stored FE");
            helper.assertValueEqual(be.experience().getExperience(), xp0, "stored XP");
        } finally {
            Balance.mobEnergyPerKill[0] = saved;
        }
        helper.succeed();
    }

    // ---- Mob Card wear ----------------------------------------------------------------------------------

    /** Wear grows by one per simulated kill, only with successful operations, and never resets progress. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 300)
    public static void mobCardWearIsCommittedPerKill(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        insertZombieCard(helper, be);
        be.upgrades().insertItem(UpgradeType.QUANTITY.slot(), new ItemStack(ModItems.card(UpgradeType.QUANTITY, 1).get()), false);
        be.experience().setExperience(be.experience().getCapacity());
        be.energy().setEnergy(be.energy().getMaxEnergyStored());
        int kills = be.modifiers().attempts();
        helper.assertTrue(kills > 1, "test setup: Quantity Card should add kills");
        helper.succeedWhen(() -> {
            long ops = be.statistics().operations();
            helper.assertTrue(ops >= 3, "waiting for three operations");
            helper.assertValueEqual((long) be.cardWear(), ops * kills, "card wear");
            helper.assertTrue(be.mob().isPresent(), "card lost");
        });
    }

    /** A failed operation (output full) adds no wear. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void mobCardWearUnchangedOnFailure(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        insertZombieCard(helper, be);
        be.experience().setExperience(500);
        be.energy().setEnergy(be.energy().getMaxEnergyStored());
        for (int i = 0; i < be.output().getSlots(); i++) be.output().setStackInSlot(i, new ItemStack(Items.COBBLESTONE, 64));
        helper.runAfterDelay(60, () -> {
            helper.assertValueEqual(be.cardWear(), 0, "wear after failed operations");
            helper.succeed();
        });
    }

    /** A corrupted card yields only Rotten Flesh (one per kill) at twice the FE, and still works. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 300)
    public static void corruptedMobCardYieldsRottenFleshAtDoubleCost(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        helper.assertTrue(be.cardSlot().insertItem(0, MobCardItem.create(EntityType.COW), false).isEmpty(), "cow card rejected");
        int freshCost = be.energyPerOperation();
        CardWear.setWear(be.cardSlot().getStackInSlot(0), CardWear.maxWear());
        helper.assertValueEqual(be.energyPerOperation(), (int) Math.ceil(freshCost * (1 + Balance.MOB_CARD_DECAY_ENERGY) - 1e-6),
                "FE per operation of a corrupted card");
        be.experience().setExperience(be.experience().getCapacity());
        be.energy().setEnergy(be.energy().getMaxEnergyStored());
        helper.succeedWhen(() -> {
            long ops = be.statistics().operations();
            helper.assertTrue(ops >= 3, "waiting for three operations");
            int flesh = itemCount(be.output());
            helper.assertValueEqual((long) flesh, ops * be.modifiers().attempts(), "one Rotten Flesh per kill");
            for (int i = 0; i < be.output().getSlots(); i++) {
                ItemStack stack = be.output().getStackInSlot(i);
                helper.assertTrue(stack.isEmpty() || stack.is(Items.ROTTEN_FLESH), "corrupted card produced " + stack);
            }
            helper.assertValueEqual(be.cardWear(), CardWear.maxWear(), "wear saturates");
        });
    }

    /** Simulation with partial decay: some kills are Rotten Flesh, the rest keep their loot. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void decayingCardMixesRottenFleshAndLoot(GameTestHelper helper) {
        MobLootSimulator.Outcome outcome = MobLootSimulator.simulate(helper.getLevel(), abs(helper, MACHINE), EntityType.COW,
                400, 0, LootFilter.ALL, new SimulatedEntityCache(), MobSimulationData.of(EntityType.COW), 0.5);
        helper.assertTrue(outcome.result() != null, "cow cannot be simulated");
        int corrupted = outcome.corruptedKills();
        helper.assertTrue(corrupted > 120 && corrupted < 280, "decay 0.5 corrupted " + corrupted + " of 400 kills");
        helper.assertValueEqual(count(outcome.result().items(), Items.ROTTEN_FLESH), corrupted, "rotten flesh");
        helper.assertTrue(count(outcome.result().items(), Items.BEEF) > 0, "no beef from the intact kills");
        helper.succeed();
    }

    // ---- Hybrid loot ------------------------------------------------------------------------------------

    private static ResourceKey<LootTable> table(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace(path));
    }

    private static MobSimulationData withSupplemental(MobSimulationData.SupplementalMode mode, ResourceKey<LootTable> key) {
        return new MobSimulationData(8, 1.0, 2, Optional.empty(),
                Optional.of(new MobSimulationData.Supplemental(mode, key)), Optional.empty());
    }

    private static MobLootSimulator.Outcome simulateWith(GameTestHelper helper, EntityType<?> type, int kills, MobSimulationData data) {
        return MobLootSimulator.simulate(helper.getLevel(), abs(helper, MACHINE), type, kills, 0, LootFilter.ALL,
                new SimulatedEntityCache(), data, 0);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void hybridLootNativeVanillaTable(GameTestHelper helper) {
        MobLootSimulator.Outcome outcome = simulateWith(helper, EntityType.COW, 20, new MobSimulationData(8, 1.0));
        helper.assertValueEqual(outcome.source(), HybridLootResolver.Source.NATIVE, "source");
        helper.assertTrue(count(outcome.result().items(), Items.LEATHER) + count(outcome.result().items(), Items.BEEF) > 0, "no cow drops");
        helper.succeed();
    }

    /** Goat: its table exists but has no pools. Valid empty result, not a fallback and not an error. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void hybridLootValidEmptyTable(GameTestHelper helper) {
        MobSimulationData data = new MobSimulationData(8, 1.0, 1, Optional.empty(), Optional.empty(), Optional.of(table("entities/cow")));
        MobLootSimulator.Outcome outcome = simulateWith(helper, EntityType.GOAT, 20, data);
        helper.assertTrue(outcome.result() != null, "goat was rejected");
        helper.assertValueEqual(outcome.source(), HybridLootResolver.Source.NATIVE, "source (fallback must not trigger)");
        helper.assertTrue(outcome.result().items().isEmpty(), "goat produced " + outcome.result().items());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void hybridLootSupplementalAdd(GameTestHelper helper) {
        MobLootSimulator.Outcome outcome = simulateWith(helper, EntityType.ZOMBIE, 40,
                withSupplemental(MobSimulationData.SupplementalMode.ADD, table("entities/cow")));
        helper.assertValueEqual(outcome.source(), HybridLootResolver.Source.NATIVE_AND_SUPPLEMENTAL, "source");
        List<ItemStack> items = outcome.result().items();
        helper.assertTrue(count(items, Items.ROTTEN_FLESH) > 0, "native zombie loot missing");
        helper.assertTrue(count(items, Items.BEEF) > 0, "supplemental loot missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void hybridLootSupplementalReplace(GameTestHelper helper) {
        MobLootSimulator.Outcome outcome = simulateWith(helper, EntityType.ZOMBIE, 40,
                withSupplemental(MobSimulationData.SupplementalMode.REPLACE, table("entities/cow")));
        helper.assertValueEqual(outcome.source(), HybridLootResolver.Source.SUPPLEMENTAL_ONLY, "source");
        List<ItemStack> items = outcome.result().items();
        helper.assertValueEqual(count(items, Items.ROTTEN_FLESH), 0, "native loot rolled despite replace");
        helper.assertTrue(count(items, Items.BEEF) > 0, "replacement loot missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void hybridLootMissingTableIsReported(GameTestHelper helper) {
        MobLootSimulator.Outcome outcome = simulateWith(helper, EntityType.ZOMBIE, 5,
                withSupplemental(MobSimulationData.SupplementalMode.ADD, table("entities/does_not_exist")));
        helper.assertTrue(outcome.result() == null, "simulation ran with a missing supplemental table");
        helper.assertValueEqual(outcome.problem(), HybridLootResolver.Problem.SUPPLEMENTAL_MISSING, "problem");
        helper.succeed();
    }

    /** The modded-mob path is the same code as vanilla (any namespace); the default data is tier II / 8 XP / 1.0x. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void unlistedMobUsesTierTwoDefaults(GameTestHelper helper) {
        MobSimulationData data = MobSimulationData.of(EntityType.BAT);
        helper.assertValueEqual(data.tier(), MobSimulationData.DEFAULT_TIER, "tier");
        helper.assertValueEqual(data.xpCost(), Balance.defaultXpPerKill, "xp");
        helper.assertValueEqual(data.energyMultiplier(), 1.0, "energy multiplier");
        helper.assertTrue(data.supplemental().isEmpty(), "a supplemental table was added automatically");
        helper.succeed();
    }

    // ---- Card automation --------------------------------------------------------------------------------

    /** XP Input faces load a Mob Card into an empty card slot and never give it back to automation. */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void mobCardInsertedThroughXpInputFace(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        IItemHandler face = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs(helper, MACHINE), Direction.EAST);
        helper.assertTrue(face != null, "no item handler on the XP Input face");
        helper.assertValueEqual(face.getSlots(), 2, "slots on the XP Input face");
        ItemStack card = MobCardItem.create(EntityType.ZOMBIE);
        helper.assertTrue(face.insertItem(1, card.copy(), false).isEmpty(), "card rejected");
        helper.assertValueEqual(be.mob().orElse(null), EntityType.ZOMBIE, "installed mob");
        helper.assertValueEqual(face.insertItem(1, MobCardItem.create(EntityType.COW), false).getCount(), 1, "second card accepted");
        helper.assertTrue(face.extractItem(1, 1, false).isEmpty(), "card extracted by automation");
        helper.assertValueEqual(face.insertItem(1, new ItemStack(Items.EXPERIENCE_BOTTLE), false).getCount(), 1, "bottle in card slot");
        IItemHandler output = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs(helper, MACHINE), Direction.DOWN);
        for (int i = 0; i < output.getSlots(); i++) helper.assertTrue(!output.getStackInSlot(i).is(ModItems.MOB_CARD.get()), "card visible on output");
        helper.succeed();
    }

    // ---- Orb collection (P3-02: an oversized orb must not stop the scan) -------------------------------

    private static ExperienceOrb orb(GameTestHelper helper, int value) {
        BlockPos pos = abs(helper, MACHINE);
        ExperienceOrb orb = new ExperienceOrb(helper.getLevel(), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, value);
        orb.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        helper.getLevel().addFreshEntity(orb);
        return orb;
    }

    private static MobLootGeneratorBlockEntity orbCollector(GameTestHelper helper, int stored) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        be.upgrades().insertItem(UpgradeType.RANGE.slot(), new ItemStack(ModItems.card(UpgradeType.RANGE, 1).get()), false);
        be.experience().setExperience(stored);
        return be;
    }

    private static void finishOrbTest(GameTestHelper helper, int expectedXp, List<ExperienceOrb> mustRemain, List<ExperienceOrb> mustBeGone) {
        helper.runAfterDelay(Balance.ORB_COLLECT_INTERVAL_TICKS * 2 + 5, () -> {
            MobLootGeneratorBlockEntity be = helper.getBlockEntity(MACHINE);
            helper.assertValueEqual(be.experience().getExperience(), expectedXp, "stored XP");
            for (ExperienceOrb o : mustBeGone) helper.assertTrue(o.isRemoved(), "orb of " + o.getValue() + " still in the world");
            for (ExperienceOrb o : mustRemain) {
                helper.assertFalse(o.isRemoved(), "orb of " + o.getValue() + " was destroyed without being stored");
                o.discard();
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void orbsSmallOrbIsAbsorbed(GameTestHelper helper) {
        orbCollector(helper, 0);
        finishOrbTest(helper, 5, List.of(), List.of(orb(helper, 5)));
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void orbsExactFill(GameTestHelper helper) {
        int cap = Balance.MOB_XP_CAPACITY[0];
        orbCollector(helper, cap - 10);
        finishOrbTest(helper, cap, List.of(), List.of(orb(helper, 10)));
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void orbsOversizedThenSmall(GameTestHelper helper) {
        int cap = Balance.MOB_XP_CAPACITY[0];
        orbCollector(helper, cap - 5);
        ExperienceOrb big = orb(helper, 50);
        ExperienceOrb small = orb(helper, 3);
        finishOrbTest(helper, cap - 2, List.of(big), List.of(small));
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void orbsFullTankLeavesOrbs(GameTestHelper helper) {
        int cap = Balance.MOB_XP_CAPACITY[0];
        orbCollector(helper, cap);
        finishOrbTest(helper, cap, List.of(orb(helper, 2)), List.of());
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void orbsMixedSizes(GameTestHelper helper) {
        int cap = Balance.MOB_XP_CAPACITY[0];
        orbCollector(helper, cap - 15);
        ExperienceOrb big = orb(helper, 20);
        ExperienceOrb a = orb(helper, 7);
        ExperienceOrb b = orb(helper, 7);
        // Whatever the scan order (and even if the two 7s merge), 14 points fit and the 20 stays.
        helper.runAfterDelay(Balance.ORB_COLLECT_INTERVAL_TICKS * 2 + 5, () -> {
            MobLootGeneratorBlockEntity be = helper.getBlockEntity(MACHINE);
            helper.assertValueEqual(be.experience().getExperience(), cap - 1, "stored XP");
            helper.assertTrue(a.isRemoved() && b.isRemoved(), "small orbs left behind");
            helper.assertFalse(big.isRemoved(), "the 20-point orb was destroyed without being stored");
            big.discard();
            helper.succeed();
        });
    }

    // ---- Card Restorer ----------------------------------------------------------------------------------

    private static CardRestorerBlockEntity restorer(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModBlocks.CARD_RESTORER.get());
        return helper.getBlockEntity(MACHINE);
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 200)
    public static void cardRestorerRepairsForExactEnergy(GameTestHelper helper) {
        CardRestorerBlockEntity be = restorer(helper);
        ItemStack card = MobCardItem.create(EntityType.ZOMBIE);
        CardWear.setWear(card, 100);
        long cost = CardRestorerBlockEntity.costPerWear(card);
        be.energy().setEnergy(be.energy().getMaxEnergyStored());
        int fe0 = be.energy().getEnergyStored();
        helper.assertTrue(be.inventory().insertItem(CardRestorerBlockEntity.INPUT, card, false).isEmpty(), "card rejected");
        helper.succeedWhen(() -> {
            ItemStack out = be.inventory().getStackInSlot(CardRestorerBlockEntity.OUTPUT);
            helper.assertTrue(!out.isEmpty(), "card not in the output yet");
            helper.assertValueEqual(CardWear.wear(out), 0, "wear after repair");
            helper.assertValueEqual(MobCardItem.getEntityType(out).orElse(null), EntityType.ZOMBIE, "mob kept");
            helper.assertTrue(be.inventory().getStackInSlot(CardRestorerBlockEntity.INPUT).isEmpty(), "input not empty");
            helper.assertValueEqual((long) (fe0 - be.energy().getEnergyStored()), 100 * cost, "FE spent");
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void cardRestorerWithoutEnergyChangesNothing(GameTestHelper helper) {
        CardRestorerBlockEntity be = restorer(helper);
        ItemStack card = MobCardItem.create(EntityType.ZOMBIE);
        CardWear.setWear(card, 500);
        be.inventory().insertItem(CardRestorerBlockEntity.INPUT, card, false);
        helper.runAfterDelay(20, () -> {
            helper.assertValueEqual(be.status(), CardRestorerStatus.NO_ENERGY, "status");
            helper.assertValueEqual(CardWear.wear(be.inventory().getStackInSlot(CardRestorerBlockEntity.INPUT)), 500, "wear");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void cardRestorerAutomationNeverStealsTheInput(GameTestHelper helper) {
        CardRestorerBlockEntity be = restorer(helper);
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs(helper, MACHINE), Direction.UP);
        helper.assertTrue(handler != null, "no item handler");
        ItemStack card = MobCardItem.create(EntityType.ZOMBIE);
        CardWear.setWear(card, 50);
        helper.assertTrue(handler.insertItem(CardRestorerBlockEntity.INPUT, card, false).isEmpty(), "card rejected by automation");
        helper.assertTrue(handler.extractItem(CardRestorerBlockEntity.INPUT, 1, false).isEmpty(), "input extracted by automation");
        helper.assertValueEqual(handler.insertItem(CardRestorerBlockEntity.INPUT, new ItemStack(Items.STONE), false).getCount(), 1, "stone accepted");
        helper.succeed();
    }

    /** Regression (ISSUE P1-01, XP side): an XP cost per operation above the tank capacity is reported, not "Not enough experience". */
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void mobGeneratorReportsXpTankTooSmall(GameTestHelper helper) {
        MobLootGeneratorBlockEntity be = mobGenerator(helper);
        // The bat has no ultraloot:mob_simulation entry, so it uses Balance.defaultXpPerKill (no other test does).
        int savedDefault = Balance.defaultXpPerKill;
        Balance.defaultXpPerKill = be.experience().getCapacity() + 1;
        try {
            helper.assertTrue(be.cardSlot().insertItem(0, MobCardItem.create(EntityType.BAT), false).isEmpty(), "bat card rejected");
            be.energy().setEnergy(be.energy().getMaxEnergyStored());
            be.experience().setExperience(be.experience().getCapacity());
            be.tickServer(helper.getLevel());
            helper.assertValueEqual(be.status(), MachineStatus.XP_TANK_TOO_SMALL, "status");
            helper.assertValueEqual(be.experience().getExperience(), be.experience().getCapacity(), "stored XP");
        } finally {
            Balance.defaultXpPerKill = savedDefault;
        }
        helper.succeed();
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
