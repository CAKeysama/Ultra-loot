package io.github.cakeysama.ultraloot.machine;

import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.config.Balance;
import io.github.cakeysama.ultraloot.experience.ExperienceCapsuleItem;
import io.github.cakeysama.ultraloot.experience.ExperienceTank;
import io.github.cakeysama.ultraloot.experience.IExperienceStorage;
import io.github.cakeysama.ultraloot.inventory.RestrictedItemHandler;
import io.github.cakeysama.ultraloot.inventory.TransactionalInserter;
import io.github.cakeysama.ultraloot.loot.LootEngine;
import io.github.cakeysama.ultraloot.menu.MobLootGeneratorMenu;
import io.github.cakeysama.ultraloot.mob.MobCardItem;
import io.github.cakeysama.ultraloot.mob.MobLootSimulator;
import io.github.cakeysama.ultraloot.mob.MobSimulationData;
import io.github.cakeysama.ultraloot.mob.SimulatedEntityCache;
import io.github.cakeysama.ultraloot.registry.ModBlockEntities;
import io.github.cakeysama.ultraloot.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Mob Loot Generator: simulates kills of the mob on its Mob Card (no entity is ever added to the world), paying FE
 * and stored XP, and stores the drops in a 27-slot output inventory. Drops can be taken by hand and/or pushed out
 * through Item Output faces at the same time. When the output cannot hold a whole operation the machine pauses.
 */
public class MobLootGeneratorBlockEntity extends AbstractMachineBlockEntity {
    private final ExperienceTank experience;
    private final ItemStackHandler output = new ItemStackHandler(Balance.MOB_OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler cardSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidCard(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            entityCache.clear();
            progress = 0;
            setChanged();
        }
    };
    private final ItemStackHandler xpSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isExperienceItem(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler outputView = RestrictedItemHandler.extractOnly(output);
    /** XP Input faces accept bottles and filled capsules and give back only empty capsules. */
    private final IItemHandler xpSlotView = new RestrictedItemHandler(xpSlot,
            MobLootGeneratorBlockEntity::isExperienceItem,
            stack -> stack.is(ModItems.EXPERIENCE_CAPSULE.get()) && ExperienceCapsuleItem.stored(stack) == 0);

    private final SimulatedEntityCache entityCache = new SimulatedEntityCache();
    private final Map<Direction, BlockCapabilityCache<IItemHandler, Direction>> exportTargets = new EnumMap<>(Direction.class);
    private int tickCounter;

    public MobLootGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOB_LOOT_GENERATOR.get(), pos, state, MachineFamily.MOB_LOOT_GENERATOR);
        this.experience = new ExperienceTank(Balance.MOB_XP_CAPACITY[tier.index()], Balance.MOB_XP_INPUT[tier.index()], this::setChanged);
    }

    public static boolean isValidCard(ItemStack stack) {
        return stack.getItem() instanceof MobCardItem && MobCardItem.getEntityType(stack).isPresent();
    }

    public static boolean isExperienceItem(ItemStack stack) {
        return stack.is(Items.EXPERIENCE_BOTTLE) || stack.is(ModItems.EXPERIENCE_CAPSULE.get());
    }

    // ---- Operation -------------------------------------------------------------------------------

    @Override
    protected void preOperationTick(ServerLevel level) {
        tickCounter++;
        if (tickCounter % Balance.XP_ITEM_INTERVAL_TICKS == 0) drainExperienceItem();
        if (tickCounter % Balance.EXPORT_INTERVAL_TICKS == 0) exportItems(level);
        int radius = modifiers().orbCollectRadius();
        if (radius > 0 && tickCounter % Balance.ORB_COLLECT_INTERVAL_TICKS == 0) collectOrbs(level, radius);
    }

    public Optional<EntityType<?>> mob() {
        return MobCardItem.getEntityType(cardSlot.getStackInSlot(0));
    }

    /** XP spent by one operation. */
    public int experiencePerOperation() {
        return mob().map(type -> MobSimulationData.of(type).xpCost() * modifiers().attempts()).orElse(0);
    }

    @Nullable
    @Override
    protected MachineStatus checkCanOperate(ServerLevel level) {
        Optional<EntityType<?>> mob = mob();
        if (mob.isEmpty()) return MachineStatus.NO_MOB_CARD;
        if (!MobCardItem.isSupported(mob.get())) return MachineStatus.INVALID_MOB;
        if (experience.getExperience() < experiencePerOperation()) return MachineStatus.NO_XP;
        if (TransactionalInserter.freeSlots(output) == 0 && isOutputCompletelyFull()) return MachineStatus.OUTPUT_FULL;
        return null;
    }

    @Override
    protected MachineStatus performOperation(int energyCost) {
        ServerLevel serverLevel = (ServerLevel) Objects.requireNonNull(level);
        Optional<EntityType<?>> mob = mob();
        if (mob.isEmpty()) return MachineStatus.NO_MOB_CARD;
        int xpCost = experiencePerOperation();
        if (experience.getExperience() < xpCost) return MachineStatus.NO_XP;

        LootEngine.Result result;
        try {
            result = MobLootSimulator.simulate(serverLevel, worldPosition, mob.get(),
                    modifiers().attempts(), modifiers().lootBonus(), filter, entityCache);
        } catch (RuntimeException e) {
            UltraLoot.LOGGER.error("Simulating {} failed at {}", mob.get(), worldPosition, e);
            return MachineStatus.INVALID_MOB;
        }
        if (result == null) return MachineStatus.INVALID_MOB;

        List<IItemHandler> targets = List.of(output);
        var plan = TransactionalInserter.plan(result.items(), targets, 0);
        if (plan.isEmpty()) return MachineStatus.OUTPUT_FULL; // nothing generated, nothing consumed

        // The output is our own ItemStackHandler, so the commit cannot diverge from the simulation.
        List<ItemStack> leftovers = TransactionalInserter.commit(plan.get(), targets);
        if (!leftovers.isEmpty()) throw new IllegalStateException("Mob Loot Generator output rejected a simulated insertion");
        experience.consume(xpCost);
        energy.consume(energyCost);
        statistics.recordOperation(energyCost, xpCost, result.itemCount(), result.filteredCount());
        return MachineStatus.RUNNING;
    }

    private boolean isOutputCompletelyFull() {
        for (int i = 0; i < output.getSlots(); i++) {
            ItemStack stack = output.getStackInSlot(i);
            if (stack.getCount() < Math.min(output.getSlotLimit(i), stack.getMaxStackSize())) return false;
        }
        return true;
    }

    @Override
    protected double energyBaseMultiplier() {
        return mob().map(type -> MobSimulationData.of(type).energyMultiplier()).orElse(1.0);
    }

    // ---- XP intake -------------------------------------------------------------------------------

    private void drainExperienceItem() {
        ItemStack stack = xpSlot.getStackInSlot(0);
        if (stack.isEmpty()) return;
        if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            if (experience.space() >= Balance.XP_BOTTLE_POINTS) {
                experience.insert(Balance.XP_BOTTLE_POINTS, false);
                xpSlot.extractItem(0, 1, false);
            }
        } else if (stack.is(ModItems.EXPERIENCE_CAPSULE.get())) {
            int stored = ExperienceCapsuleItem.stored(stack);
            int moved = experience.insert(stored, false);
            if (moved > 0) {
                ItemStack updated = stack.copy();
                ExperienceCapsuleItem.setStored(updated, stored - moved);
                xpSlot.setStackInSlot(0, updated);
            }
        }
    }

    /** Range Card on this family: absorbs XP orbs in a radius. Orbs are only removed for points actually stored. */
    private void collectOrbs(ServerLevel level, int radius) {
        if (experience.space() <= 0) return;
        AABB area = new AABB(worldPosition).inflate(radius);
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, area, ExperienceOrb::isAlive)) {
            int value = orb.getValue();
            if (value <= 0) continue;
            int absorbable = Math.min(orb.count, experience.space() / value);
            if (absorbable <= 0) break;
            experience.insert(absorbable * value, false);
            orb.count -= absorbable;
            if (orb.count <= 0) orb.discard();
        }
    }

    // ---- Export ----------------------------------------------------------------------------------

    private void exportItems(ServerLevel level) {
        Direction facing = facing();
        for (Direction dir : Direction.values()) {
            if (sides.get(facing, dir) != SideMode.ITEM_OUTPUT) continue;
            BlockCapabilityCache<IItemHandler, Direction> cache = exportTargets.computeIfAbsent(dir,
                    d -> BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, level, worldPosition.relative(d), d.getOpposite()));
            IItemHandler target = cache.getCapability();
            if (target != null) pushInto(target, Balance.EXPORT_ITEMS_PER_FACE);
        }
    }

    /** Moves up to {@code budget} items; extraction is simulated first so nothing can be lost in between. */
    private void pushInto(IItemHandler target, int budget) {
        for (int slot = 0; slot < output.getSlots() && budget > 0; slot++) {
            ItemStack available = output.extractItem(slot, budget, true);
            if (available.isEmpty()) continue;
            ItemStack rejected = ItemHandlerHelper.insertItemStacked(target, available, true);
            int movable = available.getCount() - rejected.getCount();
            if (movable <= 0) continue;
            ItemStack extracted = output.extractItem(slot, movable, false);
            ItemStack rest = ItemHandlerHelper.insertItemStacked(target, extracted, false);
            if (!rest.isEmpty()) output.insertItem(slot, rest, false);
            budget -= extracted.getCount() - rest.getCount();
        }
    }

    @Override
    protected void onSidesChanged() {
        super.onSidesChanged();
        exportTargets.clear();
    }

    // ---- Capabilities ----------------------------------------------------------------------------

    @Nullable
    @Override
    public IItemHandler getItemCapability(@Nullable Direction side) {
        if (side == null) return outputView;
        return switch (sideMode(side)) {
            case ITEM_OUTPUT -> outputView;
            case XP_INPUT -> xpSlotView;
            default -> null;
        };
    }

    @Nullable
    public IExperienceStorage getExperienceCapability(@Nullable Direction side) {
        if (side == null || sideMode(side) == SideMode.XP_INPUT) return experience;
        return null;
    }

    // ---- Accessors (menu) -------------------------------------------------------------------------

    public ExperienceTank experience() {
        return experience;
    }

    public ItemStackHandler output() {
        return output;
    }

    public ItemStackHandler cardSlot() {
        return cardSlot;
    }

    public ItemStackHandler xpSlot() {
        return xpSlot;
    }

    // ---- GUI ---------------------------------------------------------------------------------------

    @Override
    public void writeGuiData(CompoundTag tag) {
        super.writeGuiData(tag);
        tag.putInt("xp", experience.getExperience());
        tag.putInt("xpMax", experience.getCapacity());
        tag.putInt("xpPerOp", experiencePerOperation());
        mob().ifPresent(type -> {
            tag.putString("mob", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
            tag.putBoolean("mobSupported", MobCardItem.isSupported(type));
        });
        tag.putInt("outputFree", TransactionalInserter.freeSlots(output));
    }

    // ---- Persistence --------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Experience", experience.getExperience());
        tag.put("Output", output.serializeNBT(registries));
        tag.put("Card", cardSlot.serializeNBT(registries));
        tag.put("XpSlot", xpSlot.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        experience.setExperience(tag.getInt("Experience"));
        if (tag.contains("Output")) output.deserializeNBT(registries, tag.getCompound("Output"));
        if (tag.contains("Card")) cardSlot.deserializeNBT(registries, tag.getCompound("Card"));
        if (tag.contains("XpSlot")) xpSlot.deserializeNBT(registries, tag.getCompound("XpSlot"));
    }

    /** Drops every item; stored XP is released as experience orbs so it is not lost. */
    @Override
    public void dropContents(Level level) {
        super.dropContents(level);
        for (ItemStackHandler handler : List.of(output, cardSlot, xpSlot)) {
            for (int i = 0; i < handler.getSlots(); i++) dropStack(level, handler.getStackInSlot(i));
        }
        if (level instanceof ServerLevel serverLevel && experience.getExperience() > 0) {
            ExperienceOrb.award(serverLevel, Vec3.atCenterOf(worldPosition), experience.getExperience());
            experience.setExperience(0);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        entityCache.clear();
        exportTargets.clear();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MobLootGeneratorMenu(containerId, inventory, this);
    }
}
