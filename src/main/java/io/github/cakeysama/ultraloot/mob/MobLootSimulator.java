package io.github.cakeysama.ultraloot.mob;

import com.mojang.authlib.GameProfile;
import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.loot.LootEngine;
import io.github.cakeysama.ultraloot.loot.LootFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates mob kills, exactly like {@code LivingEntity#dropFromLootTable} with a player kill: THIS_ENTITY is a
 * detached instance ({@link SimulatedEntityCache}, never added to the world), the attacker and the direct attacker are
 * a FakePlayer (also never in the world) holding an iron sword with the Loot Card's Looting level, the damage source is
 * a player attack and LAST_DAMAGE_PLAYER is that FakePlayer, so player-only drops and Looting behave as in vanilla.
 * Conditions that need a specific killer (creeper discs: skeleton arrow; froglights: frog) or a burning victim fail,
 * as they would for a player with a sword. The loot sources come from {@link HybridLootResolver}. Server thread only.
 */
public final class MobLootSimulator {
    private static final GameProfile PROFILE = new GameProfile(UUID.fromString("6c1f6f9e-2b0a-4d6e-9a59-2f0b6d3c8a11"), "[Ultra Loot]");
    /** Unsupported mob/problem pairs already logged once (diagnostics without log spam). */
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    private MobLootSimulator() {}

    /**
     * @param result        items (null when the mob cannot be simulated)
     * @param problem       why the loot sources could not be resolved (null if the mob is simply not living)
     * @param corruptedKills kills that yielded Rotten Flesh because the card is decaying
     */
    public record Outcome(@Nullable LootEngine.Result result, @Nullable HybridLootResolver.Problem problem,
                          @Nullable HybridLootResolver.Source source, int corruptedKills) {
        static Outcome unsupported(@Nullable HybridLootResolver.Problem problem) {
            return new Outcome(null, problem, null, 0);
        }
    }

    /** The server's loot table registry, as seen by {@link HybridLootResolver}. */
    public static HybridLootResolver.Tables tables(ServerLevel level) {
        var registries = level.getServer().reloadableRegistries();
        Registry<LootTable> registry = registries.get().registryOrThrow(Registries.LOOT_TABLE);
        return new HybridLootResolver.Tables() {
            @Override
            public boolean exists(ResourceKey<LootTable> key) {
                return registry.containsKey(key);
            }

            @Override
            public LootTable get(ResourceKey<LootTable> key) {
                return registries.getLootTable(key);
            }
        };
    }

    /** Resolves the loot sources of a mob without simulating (used when imprinting a card and by the GUI). */
    public static HybridLootResolver.Resolution resolve(ServerLevel level, LivingEntity victim, MobSimulationData data) {
        return HybridLootResolver.resolve(victim.getLootTable(), data, tables(level));
    }

    /**
     * Simulates {@code kills} independent kills.
     *
     * @param decay card decay 0..1: each kill yields one Rotten Flesh instead of its loot with this probability
     */
    public static Outcome simulate(ServerLevel level, BlockPos pos, EntityType<?> type, int kills, int looting,
                                   LootFilter filter, SimulatedEntityCache cache, MobSimulationData data, double decay) {
        Vec3 origin = Vec3.atCenterOf(pos);
        LivingEntity victim = cache.getLiving(level, type, origin);
        if (victim == null) return Outcome.unsupported(null);
        // Freshly constructed slimes are size 1; magma cream needs a size >= 2 cube (vanilla magma_cube table).
        if (victim instanceof MagmaCube cube && cube.getSize() < 2) cube.setSize(2, false);
        HybridLootResolver.Resolution resolution = resolve(level, victim, data);
        if (!resolution.ok()) {
            if (REPORTED.add(type + "/" + resolution.problem())) {
                UltraLoot.LOGGER.warn("Ultra Loot cannot simulate {}: {} (native table {}, data map {})",
                        BuiltInRegistries.ENTITY_TYPE.getKey(type), resolution.problem(), victim.getLootTable().location(), data);
            }
            return Outcome.unsupported(resolution.problem());
        }

        FakePlayer killer = FakePlayerFactory.get(level, PROFILE);
        killer.moveTo(origin.x, origin.y, origin.z, 0f, 0f);
        killer.setItemInHand(InteractionHand.MAIN_HAND, weapon(level, looting));
        try {
            DamageSource source = level.damageSources().playerAttack(killer);
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, victim)
                    .withParameter(LootContextParams.ORIGIN, origin)
                    .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                    .withParameter(LootContextParams.ATTACKING_ENTITY, killer)
                    .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                    .withLuck(killer.getLuck())
                    .create(LootContextParamSets.ENTITY);
            List<ItemStack> kept = new ArrayList<>();
            int filtered = 0;
            int corrupted = 0;
            RandomSource random = level.getRandom();
            for (int kill = 0; kill < kills; kill++) {
                List<ItemStack> drops;
                if (decay > 0 && random.nextDouble() < decay) {
                    corrupted++;
                    drops = List.of(new ItemStack(Items.ROTTEN_FLESH));
                } else {
                    drops = new ArrayList<>();
                    for (LootTable table : resolution.tables()) drops.addAll(table.getRandomItems(params));
                }
                for (ItemStack stack : drops) {
                    if (stack.isEmpty()) continue;
                    if (filter.test(stack)) {
                        kept.add(stack);
                    } else {
                        filtered += stack.getCount();
                    }
                }
            }
            return new Outcome(new LootEngine.Result(kept, filtered), null, resolution.source(), corrupted);
        } finally {
            killer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    private static ItemStack weapon(ServerLevel level, int looting) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        if (looting > 0) {
            Holder<Enchantment> enchantment = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(Enchantments.LOOTING);
            sword.enchant(enchantment, looting);
        }
        return sword;
    }
}
