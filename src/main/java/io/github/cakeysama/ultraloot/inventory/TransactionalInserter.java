package io.github.cakeysama.ultraloot.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Plans and commits the insertion of a batch of stacks into a list of item handlers as a single transaction.
 * <p>
 * {@link #plan} copies every target into an in-memory model (respecting slot limits, max stack sizes and
 * {@code isItemValid}) and distributes the stacks round robin: each stack starts at the next target and spills over
 * to the following ones; full or incompatible targets are skipped. If anything does not fit, no plan is returned and
 * the world is untouched. {@link #commit} applies a plan; anything a handler unexpectedly rejects at commit time is
 * returned to the caller so it can be kept (never voided or dropped).
 */
public final class TransactionalInserter {
    private TransactionalInserter() {}

    /** One portion of a stack routed to a target index. */
    public record Insertion(int target, ItemStack stack) {}

    /**
     * @param nextStart round-robin cursor to use for the following operation
     */
    public record Plan(List<Insertion> insertions, int nextStart) {}

    public static Optional<Plan> plan(List<ItemStack> stacks, List<? extends IItemHandler> targets, int start) {
        if (stacks.isEmpty()) return Optional.of(new Plan(List.of(), start));
        if (targets.isEmpty()) return Optional.empty();

        List<VirtualInventory> models = new ArrayList<>(targets.size());
        for (IItemHandler target : targets) models.add(new VirtualInventory(target));

        int n = targets.size();
        int cursor = Math.floorMod(start, n);
        List<Insertion> insertions = new ArrayList<>();
        for (ItemStack original : stacks) {
            if (original.isEmpty()) continue;
            ItemStack remaining = original.copy();
            for (int k = 0; k < n && !remaining.isEmpty(); k++) {
                int t = (cursor + k) % n;
                int before = remaining.getCount();
                remaining = models.get(t).insert(remaining);
                int moved = before - remaining.getCount();
                if (moved > 0) insertions.add(new Insertion(t, original.copyWithCount(moved)));
            }
            if (!remaining.isEmpty()) return Optional.empty();
            cursor = (cursor + 1) % n;
        }
        return Optional.of(new Plan(insertions, cursor));
    }

    /** Applies the plan. @return stacks that a target rejected despite the simulation (normally empty). */
    public static List<ItemStack> commit(Plan plan, List<? extends IItemHandler> targets) {
        List<ItemStack> leftovers = new ArrayList<>();
        for (Insertion insertion : plan.insertions()) {
            ItemStack rest = ItemHandlerHelper.insertItemStacked(targets.get(insertion.target()), insertion.stack().copy(), false);
            if (!rest.isEmpty()) {
                // Try every other target before giving the stack back to the caller.
                for (int t = 0; t < targets.size() && !rest.isEmpty(); t++) {
                    if (t != insertion.target()) rest = ItemHandlerHelper.insertItemStacked(targets.get(t), rest, false);
                }
                if (!rest.isEmpty()) leftovers.add(rest);
            }
        }
        return leftovers;
    }

    /** Snapshot of a handler used for simulation; mirrors {@link ItemHandlerHelper#insertItemStacked} ordering. */
    static final class VirtualInventory {
        private final IItemHandler handler;
        private final ItemStack[] slots;

        VirtualInventory(IItemHandler handler) {
            this.handler = handler;
            this.slots = new ItemStack[handler.getSlots()];
            for (int i = 0; i < slots.length; i++) slots[i] = handler.getStackInSlot(i).copy();
        }

        ItemStack insert(ItemStack stack) {
            ItemStack remaining = stack.copy();
            if (remaining.isStackable()) {
                for (int i = 0; i < slots.length && !remaining.isEmpty(); i++) {
                    if (!slots[i].isEmpty() && ItemStack.isSameItemSameComponents(slots[i], remaining)) {
                        remaining = fill(i, remaining);
                    }
                }
            }
            for (int i = 0; i < slots.length && !remaining.isEmpty(); i++) {
                if (slots[i].isEmpty()) remaining = fill(i, remaining);
            }
            return remaining;
        }

        private ItemStack fill(int slot, ItemStack stack) {
            if (!handler.isItemValid(slot, stack)) return stack;
            int limit = Math.min(handler.getSlotLimit(slot), stack.getMaxStackSize());
            ItemStack existing = slots[slot];
            int space = limit - existing.getCount();
            if (space <= 0) return stack;
            int moved = Math.min(space, stack.getCount());
            if (existing.isEmpty()) {
                slots[slot] = stack.copyWithCount(moved);
            } else {
                existing.grow(moved);
            }
            return stack.copyWithCount(stack.getCount() - moved);
        }

        int freeSlots() {
            int free = 0;
            for (ItemStack s : slots) if (s.isEmpty()) free++;
            return free;
        }
    }

    /** Number of empty slots of a handler (used for GUI statistics). */
    public static int freeSlots(IItemHandler handler) {
        return new VirtualInventory(handler).freeSlots();
    }
}
