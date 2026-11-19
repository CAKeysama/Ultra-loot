package io.github.cakeysama.ultraloot.inventory;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionalInserterTest {
    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    private static int count(IItemHandler handler, Item item) {
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (s.is(item)) total += s.getCount();
        }
        return total;
    }

    private static int total(IItemHandler handler) {
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) total += handler.getStackInSlot(i).getCount();
        return total;
    }

    private static ItemStackHandler full(int slots) {
        ItemStackHandler h = new ItemStackHandler(slots);
        for (int i = 0; i < slots; i++) h.setStackInSlot(i, stack(Items.COBBLESTONE, 64));
        return h;
    }

    private static List<ItemStack> snapshot(IItemHandler handler) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < handler.getSlots(); i++) list.add(handler.getStackInSlot(i).copy());
        return list;
    }

    private static void assertUnchanged(List<ItemStack> before, IItemHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            assertTrue(ItemStack.matches(before.get(i), handler.getStackInSlot(i)), "slot " + i + " changed");
        }
    }

    private static List<ItemStack> run(List<ItemStack> stacks, List<? extends IItemHandler> targets, int start) {
        var plan = TransactionalInserter.plan(stacks, targets, start);
        assertTrue(plan.isPresent(), "expected a plan");
        return TransactionalInserter.commit(plan.get(), targets);
    }

    @Test
    void roundRobinDistributesOneStackPerTarget() {
        ItemStackHandler a = new ItemStackHandler(9);
        ItemStackHandler b = new ItemStackHandler(9);
        ItemStackHandler c = new ItemStackHandler(9);
        List<ItemStackHandler> targets = List.of(a, b, c);
        var plan = TransactionalInserter.plan(List.of(stack(Items.DIAMOND, 1), stack(Items.EMERALD, 1),
                stack(Items.GOLD_INGOT, 1), stack(Items.IRON_INGOT, 1)), targets, 0).orElseThrow();
        assertEquals(List.of(0, 1, 2, 0), plan.insertions().stream().map(TransactionalInserter.Insertion::target).toList());
        assertEquals(1, plan.nextStart());
        assertTrue(TransactionalInserter.commit(plan, targets).isEmpty());
        assertEquals(1, count(a, Items.DIAMOND));
        assertEquals(1, count(a, Items.IRON_INGOT));
        assertEquals(1, count(b, Items.EMERALD));
        assertEquals(1, count(c, Items.GOLD_INGOT));
    }

    @Test
    void roundRobinCursorContinuesAcrossOperations() {
        ItemStackHandler a = new ItemStackHandler(9);
        ItemStackHandler b = new ItemStackHandler(9);
        List<ItemStackHandler> targets = List.of(a, b);
        var first = TransactionalInserter.plan(List.of(stack(Items.DIAMOND, 1)), targets, 0).orElseThrow();
        TransactionalInserter.commit(first, targets);
        var second = TransactionalInserter.plan(List.of(stack(Items.EMERALD, 1)), targets, first.nextStart()).orElseThrow();
        TransactionalInserter.commit(second, targets);
        assertEquals(1, count(a, Items.DIAMOND));
        assertEquals(1, count(b, Items.EMERALD));
        assertEquals(0, second.nextStart());
    }

    @Test
    void startIndexIsWrapped() {
        ItemStackHandler a = new ItemStackHandler(1);
        ItemStackHandler b = new ItemStackHandler(1);
        var plan = TransactionalInserter.plan(List.of(stack(Items.DIAMOND, 1)), List.of(a, b), 7).orElseThrow();
        assertEquals(1, plan.insertions().getFirst().target());
        plan = TransactionalInserter.plan(List.of(stack(Items.DIAMOND, 1)), List.of(a, b), -1).orElseThrow();
        assertEquals(1, plan.insertions().getFirst().target());
    }

    @Test
    void fullTargetsAreSkipped() {
        ItemStackHandler fullOne = full(3);
        ItemStackHandler free = new ItemStackHandler(3);
        List<ItemStack> before = snapshot(fullOne);
        List<ItemStackHandler> targets = List.of(fullOne, free);
        assertTrue(run(List.of(stack(Items.DIAMOND, 5), stack(Items.EMERALD, 3)), targets, 0).isEmpty());
        assertUnchanged(before, fullOne);
        assertEquals(5, count(free, Items.DIAMOND));
        assertEquals(3, count(free, Items.EMERALD));
    }

    @Test
    void stackSpillsOverToTheNextTarget() {
        ItemStackHandler small = new ItemStackHandler(1);
        small.setStackInSlot(0, stack(Items.DIAMOND, 60));
        ItemStackHandler other = new ItemStackHandler(1);
        List<ItemStackHandler> targets = List.of(small, other);
        assertTrue(run(List.of(stack(Items.DIAMOND, 10)), targets, 0).isEmpty());
        assertEquals(64, count(small, Items.DIAMOND));
        assertEquals(6, count(other, Items.DIAMOND));
    }

    @Test
    void allOrNothingWhenEverythingDoesNotFit() {
        ItemStackHandler a = new ItemStackHandler(2);
        ItemStackHandler b = new ItemStackHandler(1);
        b.setStackInSlot(0, stack(Items.DIAMOND, 10));
        List<ItemStack> beforeA = snapshot(a);
        List<ItemStack> beforeB = snapshot(b);
        // 3 distinct items need 3 empty slots; only 2 exist (b's slot holds diamonds).
        Optional<TransactionalInserter.Plan> plan = TransactionalInserter.plan(
                List.of(stack(Items.EMERALD, 1), stack(Items.GOLD_INGOT, 1), stack(Items.IRON_INGOT, 1)), List.of(a, b), 0);
        assertTrue(plan.isEmpty());
        assertUnchanged(beforeA, a);
        assertUnchanged(beforeB, b);
    }

    @Test
    void partialFitIsRejectedEntirely() {
        ItemStackHandler a = new ItemStackHandler(1);
        assertTrue(TransactionalInserter.plan(List.of(stack(Items.DIAMOND, 64), stack(Items.DIAMOND, 1)), List.of(a), 0).isEmpty());
        assertTrue(a.getStackInSlot(0).isEmpty());
    }

    @Test
    void noTargetsMeansNoPlan() {
        assertTrue(TransactionalInserter.plan(List.of(stack(Items.DIAMOND, 1)), List.of(), 0).isEmpty());
    }

    @Test
    void emptyBatchAlwaysSucceedsAndKeepsCursor() {
        var plan = TransactionalInserter.plan(List.of(), List.of(), 3).orElseThrow();
        assertTrue(plan.insertions().isEmpty());
        assertEquals(3, plan.nextStart());
        plan = TransactionalInserter.plan(List.of(ItemStack.EMPTY), List.of(new ItemStackHandler(1)), 0).orElseThrow();
        assertTrue(plan.insertions().isEmpty());
    }

    @Test
    void nonStackablesNeedOneSlotEach() {
        ItemStackHandler two = new ItemStackHandler(2);
        List<ItemStack> swords = List.of(stack(Items.IRON_SWORD, 1), stack(Items.IRON_SWORD, 1), stack(Items.IRON_SWORD, 1));
        assertTrue(TransactionalInserter.plan(swords, List.of(two), 0).isEmpty());
        assertEquals(0, total(two));

        ItemStackHandler three = new ItemStackHandler(3);
        assertTrue(run(swords, List.of(three), 0).isEmpty());
        assertEquals(3, count(three, Items.IRON_SWORD));
        for (int i = 0; i < 3; i++) assertEquals(1, three.getStackInSlot(i).getCount());
    }

    @Test
    void partialStacksAreMergedBeforeUsingEmptySlots() {
        ItemStackHandler h = new ItemStackHandler(3);
        h.setStackInSlot(1, stack(Items.DIAMOND, 60));
        assertTrue(run(List.of(stack(Items.DIAMOND, 10)), List.of(h), 0).isEmpty());
        assertEquals(6, h.getStackInSlot(0).getCount());
        assertEquals(64, h.getStackInSlot(1).getCount());
        assertTrue(h.getStackInSlot(2).isEmpty());
    }

    @Test
    void mergingAllowsFittingIntoAnInventoryWithoutFreeSlots() {
        ItemStackHandler h = new ItemStackHandler(2);
        h.setStackInSlot(0, stack(Items.DIAMOND, 32));
        h.setStackInSlot(1, stack(Items.EMERALD, 32));
        assertTrue(run(List.of(stack(Items.DIAMOND, 32), stack(Items.EMERALD, 32)), List.of(h), 0).isEmpty());
        assertEquals(64, count(h, Items.DIAMOND));
        assertEquals(64, count(h, Items.EMERALD));
        assertTrue(TransactionalInserter.plan(List.of(stack(Items.DIAMOND, 1)), List.of(h), 0).isEmpty());
    }

    @Test
    void slotLimitsAreRespected() {
        ItemStackHandler limited = new ItemStackHandler(2) {
            @Override
            public int getSlotLimit(int slot) {
                return 16;
            }
        };
        assertTrue(TransactionalInserter.plan(List.of(stack(Items.DIAMOND, 33)), List.of(limited), 0).isEmpty());
        assertTrue(run(List.of(stack(Items.DIAMOND, 32)), List.of(limited), 0).isEmpty());
        assertEquals(16, limited.getStackInSlot(0).getCount());
        assertEquals(16, limited.getStackInSlot(1).getCount());
    }

    @Test
    void maxStackSizeIsRespected() {
        ItemStackHandler h = new ItemStackHandler(2);
        // Ender pearls stack to 16.
        assertTrue(TransactionalInserter.plan(List.of(stack(Items.ENDER_PEARL, 16), stack(Items.ENDER_PEARL, 16), stack(Items.ENDER_PEARL, 1)),
                List.of(h), 0).isEmpty());
        assertTrue(run(List.of(stack(Items.ENDER_PEARL, 16), stack(Items.ENDER_PEARL, 16)), List.of(h), 0).isEmpty());
        assertEquals(16, h.getStackInSlot(0).getCount());
        assertEquals(16, h.getStackInSlot(1).getCount());
    }

    @Test
    void itemValidityIsRespected() {
        ItemStackHandler diamondsOnly = new ItemStackHandler(3) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.is(Items.DIAMOND);
            }
        };
        ItemStackHandler any = new ItemStackHandler(3);
        List<ItemStackHandler> targets = List.of(diamondsOnly, any);
        assertTrue(run(List.of(stack(Items.EMERALD, 1), stack(Items.DIAMOND, 1)), targets, 0).isEmpty());
        assertEquals(0, count(diamondsOnly, Items.EMERALD));
        assertEquals(1, count(any, Items.EMERALD));
        assertEquals(1, count(any, Items.DIAMOND) + count(diamondsOnly, Items.DIAMOND));
        assertTrue(TransactionalInserter.plan(List.of(stack(Items.EMERALD, 1)), List.of(diamondsOnly), 0).isEmpty());
    }

    @Test
    void planDoesNotTouchTargetsUntilCommit() {
        ItemStackHandler h = new ItemStackHandler(3);
        var plan = TransactionalInserter.plan(List.of(stack(Items.DIAMOND, 5)), List.of(h), 0).orElseThrow();
        assertEquals(0, total(h));
        TransactionalInserter.commit(plan, List.of(h));
        assertEquals(5, total(h));
    }

    @Test
    void planDoesNotMutateInputStacks() {
        ItemStack diamonds = stack(Items.DIAMOND, 10);
        run(List.of(diamonds), List.of(new ItemStackHandler(1)), 0);
        assertEquals(10, diamonds.getCount());
    }

    @Test
    void commitReturnsWhatATargetUnexpectedlyRejects() {
        // Accepts everything in simulation but refuses real insertions (a misbehaving modded inventory).
        ItemStackHandler liar = new ItemStackHandler(3) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return simulate ? super.insertItem(slot, stack, true) : stack;
            }
        };
        var plan = TransactionalInserter.plan(List.of(stack(Items.DIAMOND, 7)), List.of(liar), 0).orElseThrow();
        List<ItemStack> leftovers = TransactionalInserter.commit(plan, List.of(liar));
        assertEquals(7, leftovers.stream().mapToInt(ItemStack::getCount).sum(), "no item may be voided");
    }

    @Test
    void commitReroutesRejectedItemsToOtherTargets() {
        ItemStackHandler liar = new ItemStackHandler(3) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return simulate ? super.insertItem(slot, stack, true) : stack;
            }
        };
        ItemStackHandler honest = new ItemStackHandler(3);
        var plan = TransactionalInserter.plan(List.of(stack(Items.DIAMOND, 7)), List.of(liar, honest), 0).orElseThrow();
        assertTrue(TransactionalInserter.commit(plan, List.of(liar, honest)).isEmpty());
        assertEquals(7, count(honest, Items.DIAMOND));
    }

    @Test
    void largeBatchConservesItemCount() {
        ItemStackHandler a = new ItemStackHandler(27);
        ItemStackHandler b = new ItemStackHandler(27);
        a.setStackInSlot(0, stack(Items.BONE, 50));
        List<ItemStack> batch = new ArrayList<>();
        int expected = 0;
        Item[] items = {Items.BONE, Items.STRING, Items.GUNPOWDER, Items.ROTTEN_FLESH, Items.IRON_SWORD, Items.SADDLE};
        for (int i = 0; i < 40; i++) {
            Item item = items[i % items.length];
            int n = Math.min(item.getDefaultMaxStackSize(), 1 + (i * 7) % 20);
            batch.add(stack(item, n));
            expected += n;
        }
        assertTrue(run(batch, List.of(a, b), 0).isEmpty());
        assertEquals(expected + 50, total(a) + total(b));
    }

    @Test
    void freeSlotsCountsEmptySlots() {
        ItemStackHandler h = new ItemStackHandler(4);
        h.setStackInSlot(2, stack(Items.DIAMOND, 1));
        assertEquals(3, TransactionalInserter.freeSlots(h));
        assertEquals(0, TransactionalInserter.freeSlots(full(5)));
    }
}
