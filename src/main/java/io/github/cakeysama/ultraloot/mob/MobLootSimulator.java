package io.github.cakeysama.ultraloot.mob;

import com.mojang.authlib.GameProfile;
import io.github.cakeysama.ultraloot.loot.LootEngine;
import io.github.cakeysama.ultraloot.loot.LootFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
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

import java.util.UUID;

/**
 * Simulates mob kills with the mob's own loot table, exactly like {@code LivingEntity#dropFromLootTable} with a
 * player kill: THIS_ENTITY is a detached instance ({@link SimulatedEntityCache}, never added to the world), the
 * attacker is a FakePlayer (also never in the world) holding a sword with the Loot Card's Looting level, so
 * player-only drops and Looting bonuses behave as in vanilla.
 */
public final class MobLootSimulator {
    private static final GameProfile PROFILE = new GameProfile(UUID.fromString("6c1f6f9e-2b0a-4d6e-9a59-2f0b6d3c8a11"), "[Ultra Loot]");

    private MobLootSimulator() {}

    /** @return null when the mob cannot be simulated (not living, no loot table, construction failed). */
    @Nullable
    public static LootEngine.Result simulate(ServerLevel level, BlockPos pos, EntityType<?> type, int kills, int looting,
                                             LootFilter filter, SimulatedEntityCache cache) {
        Vec3 origin = Vec3.atCenterOf(pos);
        LivingEntity victim = cache.getLiving(level, type, origin);
        if (victim == null) return null;
        // Freshly constructed slimes are size 1; magma cream needs a size >= 2 cube (vanilla magma_cube table).
        if (victim instanceof MagmaCube cube && cube.getSize() < 2) cube.setSize(2, false);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(victim.getLootTable());
        if (table == LootTable.EMPTY) return null;

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
            return LootEngine.roll(table, params, kills, filter);
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
