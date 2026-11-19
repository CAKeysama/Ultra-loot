package io.github.cakeysama.ultraloot.menu;

import io.github.cakeysama.ultraloot.config.Balance;
import io.github.cakeysama.ultraloot.machine.MobLootGeneratorBlockEntity;
import io.github.cakeysama.ultraloot.registry.ModMenus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class MobLootGeneratorMenu extends MachineMenu {
    public static final int TAB_OVERVIEW = 0;
    public static final int TAB_UPGRADES = 1;
    public static final int TAB_MOB = 2;
    public static final int TAB_INVENTORY = 3;
    public static final int TAB_SIDES = 4;
    public static final int TAB_STATS = 5;
    public static final int TAB_COUNT = 6;

    /** Mob Card slot (Mob Configuration tab). */
    public static final int CARD_X = 26;
    public static final int CARD_Y = 34;
    /** XP item slot: Bottle o' Enchanting / Experience Capsule (Overview tab). */
    public static final int XP_SLOT_X = 134;
    public static final int XP_SLOT_Y = 90;
    /** Output grid, 9x3 (Inventory tab). */
    public static final int OUTPUT_X = 8;
    public static final int OUTPUT_Y = 30;

    @Nullable
    private final MobLootGeneratorBlockEntity generator;
    private int outputStart;

    public MobLootGeneratorMenu(int containerId, Inventory inventory, MobLootGeneratorBlockEntity generator) {
        super(ModMenus.MOB_LOOT_GENERATOR.get(), containerId, inventory, generator.getBlockPos(), generator);
        this.generator = generator;
        addMachineSlots(generator.upgrades(), generator.cardSlot(), generator.xpSlot(), generator.output());
        addPlayerInventory(inventory);
    }

    public MobLootGeneratorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        super(ModMenus.MOB_LOOT_GENERATOR.get(), containerId, inventory, buf.readBlockPos(), null);
        this.generator = null;
        addMachineSlots(new ItemStackHandler(Balance.UPGRADE_SLOTS), new ItemStackHandler(1), new ItemStackHandler(1),
                new ItemStackHandler(Balance.MOB_OUTPUT_SLOTS));
        addPlayerInventory(inventory);
    }

    private void addMachineSlots(IItemHandler upgrades, IItemHandler card, IItemHandler xp, IItemHandler output) {
        addUpgradeSlots(TAB_UPGRADES, upgrades);
        addSlot(new TabSlot(this, TAB_MOB, card, 0, CARD_X, CARD_Y, MobLootGeneratorBlockEntity::isValidCard, 1));
        addSlot(new TabSlot(this, TAB_OVERVIEW, xp, 0, XP_SLOT_X, XP_SLOT_Y, MobLootGeneratorBlockEntity::isExperienceItem, 64));
        outputStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                // Output is take-only: players can never insert into it.
                addSlot(new TabSlot(this, TAB_INVENTORY, output, col + row * 9, OUTPUT_X + col * 18, OUTPUT_Y + row * 18,
                        stack -> false, 64));
            }
        }
    }

    /** Shift-click from the player inventory: Mob Card -> card slot, XP items -> XP slot. */
    @Override
    protected boolean moveToMachine(ItemStack stack) {
        int cardIndex = outputStart - 2;
        int xpIndex = outputStart - 1;
        if (MobLootGeneratorBlockEntity.isValidCard(stack)) return moveItemStackTo(stack, cardIndex, cardIndex + 1, false);
        if (MobLootGeneratorBlockEntity.isExperienceItem(stack)) return moveItemStackTo(stack, xpIndex, xpIndex + 1, false);
        return false;
    }

    // ---- Client getters ------------------------------------------------------------------------------

    public int experience() {
        return data().getInt("xp");
    }

    public int experienceMax() {
        return Math.max(1, data().contains("xpMax") ? data().getInt("xpMax") : Balance.MOB_XP_CAPACITY[tier.index()]);
    }

    public int experiencePerOperation() {
        return data().getInt("xpPerOp");
    }

    public Optional<EntityType<?>> mob() {
        if (!data().contains("mob")) return Optional.empty();
        ResourceLocation id = ResourceLocation.tryParse(data().getString("mob"));
        return id == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(id);
    }

    public boolean mobSupported() {
        return data().getBoolean("mobSupported");
    }

    public int outputFreeSlots() {
        return data().getInt("outputFree");
    }
}
