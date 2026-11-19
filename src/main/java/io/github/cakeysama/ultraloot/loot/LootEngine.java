package io.github.cakeysama.ultraloot.loot;

import io.github.cakeysama.ultraloot.mob.SimulatedEntityCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Rolls loot profiles. Each attempt is an independent roll of the table, so the table's own counts, weights and
 * conditions always apply (the Quantity Card adds attempts; it never multiplies stack sizes).
 */
public final class LootEngine {
    private LootEngine() {}

    /** Items that survived the filter, plus how many item units the filter removed. */
    public record Result(List<ItemStack> items, int filteredCount) {
        public int itemCount() {
            int total = 0;
            for (ItemStack stack : items) total += stack.getCount();
            return total;
        }
    }

    @Nullable
    public static LootTable resolveTable(ServerLevel level, LootProfile profile) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(profile.table());
        return table == LootTable.EMPTY ? null : table;
    }

    /** @return null when the context cannot be built (e.g. the barter entity is unavailable). */
    @Nullable
    public static LootParams buildParams(ServerLevel level, BlockPos pos, LootProfile profile, int luck, SimulatedEntityCache entities) {
        Vec3 origin = Vec3.atCenterOf(pos);
        LootParams.Builder builder = new LootParams.Builder(level).withLuck(luck);
        return switch (profile.context()) {
            case CHEST -> builder.withParameter(LootContextParams.ORIGIN, origin).create(LootContextParamSets.CHEST);
            case FISHING -> {
                // A detached bobber (never added to the world, open water by default) lets the treasure pool's
                // fishing_hook predicate pass, as it does for a player fishing in open water.
                Entity hook = entities.get(level, EntityType.FISHING_BOBBER, origin);
                yield builder.withParameter(LootContextParams.ORIGIN, origin)
                        .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                        .withOptionalParameter(LootContextParams.THIS_ENTITY, hook)
                        .create(LootContextParamSets.FISHING);
            }
            case ARCHAEOLOGY -> builder.withParameter(LootContextParams.ORIGIN, origin).create(LootContextParamSets.ARCHAEOLOGY);
            case BARTER -> {
                LivingEntity piglin = entities.getLiving(level, EntityType.PIGLIN, origin);
                yield piglin == null ? null : builder.withParameter(LootContextParams.THIS_ENTITY, piglin)
                        .create(LootContextParamSets.PIGLIN_BARTER);
            }
            case EMPTY -> builder.create(LootContextParamSets.EMPTY);
        };
    }

    public static Result roll(LootTable table, LootParams params, int attempts, LootFilter filter) {
        List<ItemStack> kept = new ArrayList<>();
        int filtered = 0;
        for (int i = 0; i < attempts; i++) {
            for (ItemStack stack : table.getRandomItems(params)) {
                if (stack.isEmpty()) continue;
                if (filter.test(stack)) {
                    kept.add(stack);
                } else {
                    filtered += stack.getCount();
                }
            }
        }
        return new Result(kept, filtered);
    }
}
