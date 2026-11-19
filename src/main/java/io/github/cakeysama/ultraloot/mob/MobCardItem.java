package io.github.cakeysama.ultraloot.mob;

import io.github.cakeysama.ultraloot.registry.ModDataComponents;
import io.github.cakeysama.ultraloot.registry.ModItems;
import io.github.cakeysama.ultraloot.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Optional;

/** A card that selects the mob simulated by the Mob Loot Generator. The mob is stored in {@code ultraloot:mob}. */
public class MobCardItem extends Item {
    public MobCardItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(EntityType<?> type) {
        ItemStack stack = new ItemStack(ModItems.MOB_CARD.get());
        stack.set(ModDataComponents.MOB.get(), BuiltInRegistries.ENTITY_TYPE.getKey(type));
        return stack;
    }

    public static Optional<EntityType<?>> getEntityType(ItemStack stack) {
        ResourceLocation id = stack.get(ModDataComponents.MOB.get());
        if (id == null) return Optional.empty();
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id);
    }

    /** Living, non-boss mobs only; the blacklist tag lets packs exclude more. */
    public static boolean isSupported(EntityType<?> type) {
        return type.getCategory() != MobCategory.MISC && !type.is(ModTags.Entities.MOB_CARD_BLACKLIST);
    }

    /** Spawn-egg colours used to tint the card emblem (layer 1); grey when the mob has no egg. */
    public static int tintColor(ItemStack stack, int layer) {
        if (layer != 1) return 0xFFFFFFFF;
        return getEntityType(stack)
                .map(SpawnEggItem::byId)
                .map(egg -> 0xFF000000 | egg.getColor(0))
                .orElse(0xFF9A9A9A);
    }

    @Override
    public Component getName(ItemStack stack) {
        return getEntityType(stack)
                .map(type -> (Component) Component.translatable("item.ultraloot.mob_card.named", type.getDescription()))
                .orElseGet(() -> super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Optional<EntityType<?>> type = getEntityType(stack);
        if (type.isEmpty()) {
            tooltip.add(Component.translatable("ultraloot.mob_card.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        MobSimulationData data = MobSimulationData.of(type.get());
        tooltip.add(Component.translatable("ultraloot.mob_card.xp_cost", data.xpCost()).withStyle(ChatFormatting.GREEN));
        if (data.energyMultiplier() != 1.0) {
            tooltip.add(Component.translatable("ultraloot.mob_card.energy",
                    String.format(java.util.Locale.ROOT, "%.2fx", data.energyMultiplier())).withStyle(ChatFormatting.RED));
        }
        if (!isSupported(type.get())) {
            tooltip.add(Component.translatable("ultraloot.mob_card.unsupported").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
