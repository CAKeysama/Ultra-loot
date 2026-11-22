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

    private static final String[] TIER_NUMERALS = {"I", "II", "III"};

    private static String percent(double fraction) {
        return Long.toString(Math.round(fraction * 100));
    }

    // ---- Wear bar: hidden while new; green while intact, amber while decaying, red when corrupted. ----

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return CardWear.wear(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(13.0 * CardWear.quality(CardWear.wear(stack)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        double decay = CardWear.decay(stack);
        if (decay <= 0) return 0x55FF55;
        return decay < 1 ? 0xFFAA00 : 0xFF5555;
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
        int wear = CardWear.wear(stack);
        double decay = CardWear.decay(wear);
        tooltip.add(Component.translatable("ultraloot.mob_card.tier", TIER_NUMERALS[Math.clamp(data.tier(), 1, 3) - 1])
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("ultraloot.mob_card.xp_cost", data.xpCost()).withStyle(ChatFormatting.GREEN));
        double energy = data.energyMultiplier() * CardWear.energyFactor(wear);
        if (energy != 1.0) {
            tooltip.add(Component.translatable("ultraloot.mob_card.energy", percent(energy) + "%")
                    .withStyle(ChatFormatting.GOLD));
        }
        int integrity = (int) Math.round(CardWear.quality(wear) * 100);
        ChatFormatting integrityColor = decay <= 0 ? ChatFormatting.GREEN : decay < 1 ? ChatFormatting.GOLD : ChatFormatting.RED;
        tooltip.add(Component.translatable("ultraloot.mob_card.integrity", integrity + "%").withStyle(integrityColor));
        if (decay >= 1) {
            tooltip.add(Component.translatable("ultraloot.mob_card.corrupted").withStyle(ChatFormatting.RED));
        } else if (decay > 0) {
            tooltip.add(Component.translatable("ultraloot.mob_card.decaying", percent(decay) + "%")
                    .withStyle(ChatFormatting.GOLD));
        }
        if (wear > 0) tooltip.add(Component.translatable("ultraloot.mob_card.repair_hint").withStyle(ChatFormatting.DARK_GRAY));
        if (!isSupported(type.get())) {
            tooltip.add(Component.translatable("ultraloot.mob_card.unsupported").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
