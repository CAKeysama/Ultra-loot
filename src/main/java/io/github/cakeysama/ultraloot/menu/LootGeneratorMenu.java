package io.github.cakeysama.ultraloot.menu;

import io.github.cakeysama.ultraloot.config.Balance;
import io.github.cakeysama.ultraloot.inventory.InventoryScanner;
import io.github.cakeysama.ultraloot.machine.LootGeneratorBlockEntity;
import io.github.cakeysama.ultraloot.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LootGeneratorMenu extends MachineMenu {
    public static final int TAB_OVERVIEW = 0;
    public static final int TAB_UPGRADES = 1;
    public static final int TAB_LOOT = 2;
    public static final int TAB_SCAN = 3;
    public static final int TAB_SIDES = 4;
    public static final int TAB_STATS = 5;
    public static final int TAB_COUNT = 6;

    /** A selectable loot profile as seen by the client. */
    public record ProfileInfo(ResourceLocation id, String translationKey, String fallbackName, ResourceLocation icon,
                              String category, int minTier, double energyMultiplier) {}

    /** One inventory found by the scanner. */
    public record ScanInfo(BlockPos pos, ResourceLocation block, int distance, int freeSlots, int totalSlots,
                           InventoryScanner.EntryState state, boolean picked) {}

    @Nullable
    private final LootGeneratorBlockEntity generator;

    /** Server constructor. */
    public LootGeneratorMenu(int containerId, Inventory inventory, LootGeneratorBlockEntity generator) {
        super(ModMenus.LOOT_GENERATOR.get(), containerId, inventory, generator.getBlockPos(), generator);
        this.generator = generator;
        addUpgradeSlots(TAB_UPGRADES, generator.upgrades());
        addPlayerInventory(inventory);
    }

    /** Client constructor. */
    public LootGeneratorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        super(ModMenus.LOOT_GENERATOR.get(), containerId, inventory, buf.readBlockPos(), null);
        this.generator = null;
        addUpgradeSlots(TAB_UPGRADES, new ItemStackHandler(Balance.UPGRADE_SLOTS));
        addPlayerInventory(inventory);
    }

    @Override
    protected void handleFamilyAction(MachineAction action, int a, int b, String text) {
        if (generator == null) return;
        switch (action) {
            case SET_PROFILE -> generator.selectProfile(text.isEmpty() ? null : ResourceLocation.tryParse(text));
            case SET_TARGET_MODE -> generator.setTargetMode(InventoryScanner.TargetMode.byOrdinal(a));
            case RESCAN -> generator.requestManualRescan();
            case SET_PICKED_ONLY -> generator.setPickedOnly(a == 1);
            case TOGGLE_PICKED -> generator.togglePicked(BlockPos.of(MachineAction.unpackPos(a, b)));
            default -> {}
        }
    }

    // ---- Client getters ------------------------------------------------------------------------------

    @Nullable
    public ResourceLocation selectedProfile() {
        return data().contains("profile") ? ResourceLocation.tryParse(data().getString("profile")) : null;
    }

    public InventoryScanner.TargetMode targetMode() {
        return InventoryScanner.TargetMode.byOrdinal(data().getInt("targetMode"));
    }

    public boolean pickedOnly() {
        return data().getBoolean("pickedOnly");
    }

    public int pickedCount() {
        return data().getInt("pickedCount");
    }

    public int targetCount() {
        return data().getInt("targets");
    }

    /** Item units held because a target rejected them after simulation (machine pauses until flushed). */
    public int overflowCount() {
        return data().getInt("overflow");
    }

    public List<ProfileInfo> profiles() {
        ListTag list = data().getList("profiles", Tag.TAG_COMPOUND);
        List<ProfileInfo> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag p = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(p.getString("id"));
            ResourceLocation icon = ResourceLocation.tryParse(p.getString("icon"));
            if (id == null || icon == null) continue;
            result.add(new ProfileInfo(id, p.getString("key"), p.getString("name"), icon, p.getString("category"),
                    p.getInt("minTier"), p.getDouble("energy")));
        }
        return result;
    }

    public List<ScanInfo> scanEntries() {
        ListTag list = data().getList("scan", Tag.TAG_COMPOUND);
        List<ScanInfo> result = new ArrayList<>(list.size());
        InventoryScanner.EntryState[] states = InventoryScanner.EntryState.values();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            ResourceLocation block = ResourceLocation.tryParse(e.getString("block"));
            int state = e.getInt("state");
            if (block == null || state < 0 || state >= states.length) continue;
            result.add(new ScanInfo(BlockPos.of(e.getLong("pos")), block, e.getInt("dist"), e.getInt("free"),
                    e.getInt("total"), states[state], e.getBoolean("picked")));
        }
        return result;
    }
}
