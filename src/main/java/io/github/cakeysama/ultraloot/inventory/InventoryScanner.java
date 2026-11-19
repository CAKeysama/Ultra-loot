package io.github.cakeysama.ultraloot.inventory;

import io.github.cakeysama.ultraloot.config.Balance;
import io.github.cakeysama.ultraloot.machine.AbstractMachineBlockEntity;
import io.github.cakeysama.ultraloot.machine.SideConfiguration;
import io.github.cakeysama.ultraloot.machine.SideMode;
import io.github.cakeysama.ultraloot.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Finds item inventories around a Loot Generator. Runs on an interval (never every tick), only looks at block
 * entities of chunks that are already loaded (it never loads chunks) and caches each target with a
 * {@link BlockCapabilityCache}, which NeoForge invalidates automatically when the block changes.
 * <p>
 * Range is a cube: a block is in range when {@code max(|dx|, |dy|, |dz|) <= range}.
 * Double chests are reported once. Other Ultra Loot machines are never targets. An inventory touching the machine
 * is only used when the face it touches is configured as {@link SideMode#ITEM_TARGET}.
 */
public final class InventoryScanner {
    private InventoryScanner() {}

    public enum TargetMode {
        /** Only blocks in {@code #ultraloot:storage_targets}. */
        STORAGE_ONLY,
        /** Any block exposing an item handler, except {@code #ultraloot:scan_blacklist}. */
        ANY_INVENTORY;

        public static TargetMode byOrdinal(int ordinal) {
            return ordinal == 1 ? ANY_INVENTORY : STORAGE_ONLY;
        }
    }

    public enum EntryState {
        TARGET, FULL, NOT_STORAGE, BLACKLISTED, FACE_DISABLED;

        public String translationKey() {
            return "ultraloot.scan_state." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** An inventory found in range, for the GUI. */
    public record Entry(BlockPos pos, ResourceLocation block, int distance, int freeSlots, int totalSlots, EntryState state) {}

    /** A usable target (state TARGET or FULL at scan time; fullness is re-evaluated on every operation). */
    public record Target(BlockPos pos, int distance, BlockCapabilityCache<IItemHandler, @Nullable Direction> cache) {
        @Nullable
        public IItemHandler handler() {
            return cache.getCapability();
        }
    }

    public record Result(List<Target> targets, List<Entry> entries) {
        public static final Result EMPTY = new Result(List.of(), List.of());
    }

    public static Result scan(ServerLevel level, BlockPos origin, Direction facing, SideConfiguration sides, int range, TargetMode mode) {
        List<Target> targets = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();
        int minCx = SectionPos.blockToSectionCoord(origin.getX() - range);
        int maxCx = SectionPos.blockToSectionCoord(origin.getX() + range);
        int minCz = SectionPos.blockToSectionCoord(origin.getZ() - range);
        int maxCz = SectionPos.blockToSectionCoord(origin.getZ() + range);

        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue; // never load chunks
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();
                    int distance = distance(origin, pos);
                    if (distance == 0 || distance > range) continue;
                    if (be instanceof AbstractMachineBlockEntity) continue;
                    BlockState state = be.getBlockState();
                    if (!isCanonicalChestHalf(origin, range, pos, state)) continue;

                    IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, be, null);
                    if (handler == null || handler.getSlots() == 0) continue;

                    int free = TransactionalInserter.freeSlots(handler);
                    EntryState entryState;
                    if (state.is(ModTags.Blocks.SCAN_BLACKLIST)) {
                        entryState = EntryState.BLACKLISTED;
                    } else if (mode == TargetMode.STORAGE_ONLY && !state.is(ModTags.Blocks.STORAGE_TARGETS)) {
                        entryState = EntryState.NOT_STORAGE;
                    } else if (touchesDisabledFace(origin, facing, sides, pos, state)) {
                        entryState = EntryState.FACE_DISABLED;
                    } else {
                        entryState = free == 0 && isCompletelyFull(handler) ? EntryState.FULL : EntryState.TARGET;
                    }

                    entries.add(new Entry(pos.immutable(), BuiltInRegistries.BLOCK.getKey(state.getBlock()), distance, free, handler.getSlots(), entryState));
                    if (entryState == EntryState.TARGET || entryState == EntryState.FULL) {
                        targets.add(new Target(pos.immutable(), distance,
                                BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, level, pos.immutable(), null)));
                    }
                }
            }
        }
        // Deterministic order: nearest first, then by position, so round robin is stable between scans.
        Comparator<BlockPos> posOrder = Comparator.naturalOrder();
        targets.sort(Comparator.comparingInt(Target::distance).thenComparing(Target::pos, posOrder));
        entries.sort(Comparator.comparingInt(Entry::distance).thenComparing(Entry::pos, posOrder));
        if (entries.size() > Balance.MAX_SCAN_ENTRIES) entries = new ArrayList<>(entries.subList(0, Balance.MAX_SCAN_ENTRIES));
        return new Result(List.copyOf(targets), List.copyOf(entries));
    }

    public static int distance(BlockPos a, BlockPos b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.max(Math.abs(a.getY() - b.getY()), Math.abs(a.getZ() - b.getZ())));
    }

    /** A double chest exposes the combined inventory on both halves; keep only one of them. */
    private static boolean isCanonicalChestHalf(BlockPos origin, int range, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ChestBlock) || !state.hasProperty(ChestBlock.TYPE)
                || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return true;
        }
        BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
        if (distance(origin, other) > range || distance(origin, other) == 0) return true;
        return pos.compareTo(other) < 0;
    }

    private static boolean touchesDisabledFace(BlockPos origin, Direction facing, SideConfiguration sides, BlockPos pos, BlockState state) {
        if (isDisabledNeighbour(origin, facing, sides, pos)) return true;
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return isDisabledNeighbour(origin, facing, sides, pos.relative(ChestBlock.getConnectedDirection(state)));
        }
        return false;
    }

    private static boolean isDisabledNeighbour(BlockPos origin, Direction facing, SideConfiguration sides, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (origin.relative(d).equals(pos)) return sides.get(facing, d) != SideMode.ITEM_TARGET;
        }
        return false;
    }

    private static boolean isCompletelyFull(IItemHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            var stack = handler.getStackInSlot(i);
            if (stack.getCount() < Math.min(handler.getSlotLimit(i), stack.getMaxStackSize())) return false;
        }
        return true;
    }
}
