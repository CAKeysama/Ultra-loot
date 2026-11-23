package io.github.cakeysama.ultraloot.client.screen;

import io.github.cakeysama.ultraloot.client.screen.widget.ScrollableList;
import io.github.cakeysama.ultraloot.inventory.InventoryScanner;
import io.github.cakeysama.ultraloot.machine.MachineTier;
import io.github.cakeysama.ultraloot.menu.LootGeneratorMenu;
import io.github.cakeysama.ultraloot.menu.LootGeneratorMenu.ProfileInfo;
import io.github.cakeysama.ultraloot.menu.LootGeneratorMenu.ScanInfo;
import io.github.cakeysama.ultraloot.menu.MachineAction;
import io.github.cakeysama.ultraloot.network.MachineActionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LootGeneratorScreen extends MachineScreen<LootGeneratorMenu> {
    private static final int TEXT_X = 28;
    private static final int TEXT_WIDTH = 138;
    private static final int ENERGY_X = 9;
    private static final int BAR_Y = 20;
    private static final int STATUS_Y = 21;
    private static final int PROGRESS_Y = 32;
    private static final int PROGRESS_WIDTH = 102;
    private static final int BOTTOM_Y = 95;
    private static final int TOGGLE_X = 81;
    private static final int TOGGLE_WIDTH = 86;
    private static final int RESCAN_X = 107;
    private static final int RESCAN_WIDTH = 60;
    private static final int PICK_MODE_X = 7;
    private static final int PICK_MODE_WIDTH = 96;
    private static final int CHECKBOX = 9;

    private final ScrollableList profileList = new ScrollableList(7, 19, 148, 4);
    private final ScrollableList scanList = new ScrollableList(7, 19, 148, 4);
    private final Map<ResourceLocation, ItemStack> icons = new HashMap<>();

    @Nullable
    private CompoundTag cachedData;
    private List<ProfileView> profiles = List.of();
    private List<ScanInfo> scan = List.of();
    private boolean statusCut;

    /** A profile with its resolved display texts, sorted by category then name. */
    private record ProfileView(ProfileInfo info, Component name, Component category) {}

    public LootGeneratorScreen(LootGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected List<Tab> createTabs() {
        return List.of(
                tab(LootGeneratorMenu.TAB_OVERVIEW, "overview", GuiSprites.ICON_OVERVIEW),
                tab(LootGeneratorMenu.TAB_UPGRADES, "upgrades", GuiSprites.ICON_UPGRADES),
                tab(LootGeneratorMenu.TAB_LOOT, "loot_config", GuiSprites.ICON_LOOT),
                tab(LootGeneratorMenu.TAB_SCAN, "inventory_scan", GuiSprites.ICON_SCAN),
                tab(LootGeneratorMenu.TAB_SIDES, "side_config", GuiSprites.ICON_SIDES),
                tab(LootGeneratorMenu.TAB_STATS, "statistics", GuiSprites.ICON_STATS));
    }

    @Override
    protected int accent() {
        return GuiDraw.ACCENT_LOOT;
    }

    @Nullable
    @Override
    protected ScrollableList listFor(int tab) {
        return switch (tab) {
            case LootGeneratorMenu.TAB_LOOT -> profileList;
            case LootGeneratorMenu.TAB_SCAN -> scanList;
            default -> null;
        };
    }

    /** Re-reads the synced lists only when a new snapshot arrived. */
    private void refresh() {
        CompoundTag data = menu.data();
        if (data == cachedData) return;
        cachedData = data;
        List<ProfileView> views = new ArrayList<>();
        for (ProfileInfo info : menu.profiles()) {
            Component name = info.translationKey().isEmpty()
                    ? Component.literal(info.fallbackName())
                    : Component.translatableWithFallback(info.translationKey(), info.fallbackName());
            views.add(new ProfileView(info, name, categoryName(info.category())));
        }
        views.sort(Comparator.comparing((ProfileView v) -> v.info().category())
                .thenComparing(v -> v.name().getString()));
        profiles = List.copyOf(views);
        scan = menu.scanEntries();
        profileList.setCount(profiles.size());
        scanList.setCount(scan.size());
    }

    private static Component categoryName(String category) {
        String fallback = category.isEmpty() ? category
                : category.substring(0, 1).toUpperCase(Locale.ROOT) + category.substring(1).replace('_', ' ');
        return Component.translatableWithFallback("ultraloot.category." + category, fallback);
    }

    private boolean locked(ProfileInfo info) {
        return info.minTier() > menu.tier().number();
    }

    @Nullable
    private ProfileView selected() {
        ResourceLocation id = menu.selectedProfile();
        if (id == null) return null;
        for (ProfileView view : profiles) {
            if (view.info().id().equals(id)) return view;
        }
        return null;
    }

    private ItemStack itemIcon(ResourceLocation itemId) {
        return icons.computeIfAbsent(itemId, id -> new ItemStack(BuiltInRegistries.ITEM.get(id)));
    }

    private ItemStack blockIcon(ResourceLocation blockId) {
        return icons.computeIfAbsent(blockId, id -> {
            ItemStack stack = new ItemStack(BuiltInRegistries.BLOCK.get(id).asItem());
            return stack.isEmpty() ? new ItemStack(Items.CHEST) : stack;
        });
    }

    private static Component blockName(ResourceLocation blockId) {
        Block block = BuiltInRegistries.BLOCK.get(blockId);
        return block.getName();
    }

    private static int stateColor(InventoryScanner.EntryState state) {
        return switch (state) {
            case TARGET -> GuiDraw.GOOD;
            case FULL -> GuiDraw.WARN;
            case NOT_STORAGE -> 0xFF8A909A;
            case BLACKLISTED -> GuiDraw.BAD;
            case FACE_DISABLED -> 0xFF6A8CC8;
            case NOT_PICKED -> GuiDraw.DIM;
        };
    }

    private static Component pickModeName(boolean pickedOnly) {
        return Component.translatable(pickedOnly ? "ultraloot.gui.scan.picked_only" : "ultraloot.gui.scan.all_in_range");
    }

    /** Small checkbox: dark frame, accent fill and a pixel check mark when ticked. */
    private void renderCheckbox(GuiGraphics graphics, int x, int y, boolean ticked, boolean hovered) {
        graphics.fill(x, y, x + CHECKBOX, y + CHECKBOX, hovered ? GuiDraw.TEXT : GuiDraw.DIM);
        graphics.fill(x + 1, y + 1, x + CHECKBOX - 1, y + CHECKBOX - 1, ticked ? accent() : 0xFF16191E);
        if (!ticked) return;
        int c = 0xFF101216;
        graphics.fill(x + 2, y + 4, x + 3, y + 6, c);
        graphics.fill(x + 3, y + 5, x + 4, y + 7, c);
        graphics.fill(x + 4, y + 4, x + 5, y + 6, c);
        graphics.fill(x + 5, y + 3, x + 6, y + 5, c);
        graphics.fill(x + 6, y + 2, x + 7, y + 4, c);
    }

    // ---- Rendering --------------------------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        refresh();
        super.renderBg(graphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderTab(GuiGraphics graphics, int tab, int mx, int my) {
        switch (tab) {
            case LootGeneratorMenu.TAB_OVERVIEW -> renderOverview(graphics);
            case LootGeneratorMenu.TAB_UPGRADES -> renderUpgrades(graphics);
            case LootGeneratorMenu.TAB_LOOT -> renderLootConfig(graphics, mx, my);
            case LootGeneratorMenu.TAB_SCAN -> renderScan(graphics, mx, my);
            case LootGeneratorMenu.TAB_SIDES -> sides.render(graphics, font, mx, my);
            case LootGeneratorMenu.TAB_STATS -> renderStatistics(graphics);
            default -> {}
        }
    }

    private void renderOverview(GuiGraphics graphics) {
        renderEnergyBar(graphics, ENERGY_X, BAR_Y);
        statusCut = renderStatus(graphics, TEXT_X, STATUS_Y, TEXT_WIDTH);
        renderProgress(graphics, GuiSprites.PROGRESS_LOOT, TEXT_X, PROGRESS_Y, PROGRESS_WIDTH);

        int targets = menu.targetCount();
        row(graphics, "ultraloot.gui.targets", Component.literal(GuiFormat.number(targets)), TEXT_X, 46, TEXT_WIDTH,
                targets == 0 ? GuiDraw.WARN : GuiDraw.TEXT);
        row(graphics, "ultraloot.gui.stat.energy_per_operation", GuiFormat.number(menu.energyPerOperation()), TEXT_X, 57, TEXT_WIDTH);
        row(graphics, "ultraloot.gui.stat.time_per_operation", GuiFormat.seconds(menu.ticksPerOperation()), TEXT_X, 68, TEXT_WIDTH, GuiDraw.TEXT);
        graphics.fill(TEXT_X, 79, TEXT_X + TEXT_WIDTH, 80, GuiDraw.SEPARATOR);

        ResourceLocation selectedId = menu.selectedProfile();
        ProfileView selected = selected();
        if (selected != null) {
            graphics.renderItem(itemIcon(selected.info().icon()), TEXT_X, 83);
            int color = locked(selected.info()) ? GuiDraw.BAD : accent();
            GuiDraw.trimmed(graphics, font, selected.name(), TEXT_X + 20, 83, TEXT_WIDTH - 20, color);
            GuiDraw.trimmed(graphics, font, selected.category(), TEXT_X + 20, 93, TEXT_WIDTH - 20, GuiDraw.DIM);
        } else if (selectedId != null) {
            GuiDraw.trimmed(graphics, font, Component.literal(selectedId.toString()), TEXT_X, 88, TEXT_WIDTH, GuiDraw.BAD);
        } else {
            GuiDraw.trimmed(graphics, font, Component.translatable("ultraloot.gui.profile.none"), TEXT_X, 88, TEXT_WIDTH, GuiDraw.DIM);
        }

        int overflow = menu.overflowCount();
        if (overflow > 0) {
            row(graphics, "ultraloot.gui.buffered", Component.literal(GuiFormat.number(overflow)), TEXT_X, 104, TEXT_WIDTH, GuiDraw.WARN);
        }
    }

    private void renderLootConfig(GuiGraphics graphics, int mx, int my) {
        ResourceLocation selectedId = menu.selectedProfile();
        int hovered = profileList.rowAt(mx, my);
        int x = profileList.x() + 1;
        int width = profileList.width() - 2;
        if (profiles.isEmpty()) {
            GuiDraw.centered(graphics, font, Component.translatable("ultraloot.gui.profile.empty"), x + width / 2, 52, GuiDraw.DIM);
        }
        for (int i = 0; i < profileList.visibleRows(); i++) {
            int index = profileList.offset() + i;
            if (index >= profiles.size()) break;
            ProfileView view = profiles.get(index);
            int y = profileList.rowY(i);
            boolean isSelected = view.info().id().equals(selectedId);
            boolean isLocked = locked(view.info());
            if (isSelected) {
                graphics.fill(x, y, x + width, y + ScrollableList.ROW_HEIGHT, (accent() & 0x00FFFFFF) | 0x30000000);
                graphics.fill(x, y, x + 2, y + ScrollableList.ROW_HEIGHT, accent());
            } else if (index == hovered && !isLocked) {
                graphics.fill(x, y, x + width, y + ScrollableList.ROW_HEIGHT, GuiDraw.ROW_HOVER);
            } else if (index % 2 == 1) {
                graphics.fill(x, y, x + width, y + ScrollableList.ROW_HEIGHT, GuiDraw.ROW_ALT);
            }
            graphics.renderItem(itemIcon(view.info().icon()), x + 3, y + 1);
            int textWidth = width - 22 - (isLocked ? 14 : 2);
            int nameColor = isLocked ? GuiDraw.DIM : isSelected ? accent() : GuiDraw.TEXT;
            GuiDraw.trimmed(graphics, font, view.name(), x + 22, y + 1, textWidth, nameColor);
            GuiDraw.trimmed(graphics, font, view.category(), x + 22, y + 10, textWidth, GuiDraw.DIM);
            if (isLocked) GuiSprites.LOCK.draw(graphics, x + width - 13, y + 3);
        }

        renderFilters(graphics, 7, BOTTOM_Y, mx, my);
        renderButton(graphics, targetModeName(menu.targetMode()), TOGGLE_X, BOTTOM_Y, TOGGLE_WIDTH, mx, my, true);
    }

    private static Component targetModeName(InventoryScanner.TargetMode mode) {
        return Component.translatable("ultraloot.gui.target_mode." + mode.name().toLowerCase(Locale.ROOT));
    }

    private void renderScan(GuiGraphics graphics, int mx, int my) {
        int hovered = scanList.rowAt(mx, my);
        boolean picking = menu.pickedOnly();
        int x = scanList.x() + 1;
        int width = scanList.width() - 2;
        if (scan.isEmpty()) {
            GuiDraw.centered(graphics, font, Component.translatable("ultraloot.gui.scan.empty"), x + width / 2, 52, GuiDraw.DIM);
        }
        for (int i = 0; i < scanList.visibleRows(); i++) {
            int index = scanList.offset() + i;
            if (index >= scan.size()) break;
            ScanInfo entry = scan.get(index);
            int y = scanList.rowY(i);
            boolean ticked = picking && entry.picked();
            if (ticked) {
                graphics.fill(x, y, x + width, y + ScrollableList.ROW_HEIGHT, (accent() & 0x00FFFFFF) | 0x30000000);
                graphics.fill(x, y, x + 2, y + ScrollableList.ROW_HEIGHT, accent());
            } else if (index == hovered) {
                graphics.fill(x, y, x + width, y + ScrollableList.ROW_HEIGHT, GuiDraw.ROW_HOVER);
            } else if (index % 2 == 1) {
                graphics.fill(x, y, x + width, y + ScrollableList.ROW_HEIGHT, GuiDraw.ROW_ALT);
            }
            graphics.renderItem(blockIcon(entry.block()), x + 3, y + 1);
            int textWidth = width - 22 - 7 - (picking ? CHECKBOX + 3 : 0);
            int nameColor = picking && !entry.picked() ? GuiDraw.DIM : GuiDraw.TEXT;
            GuiDraw.trimmed(graphics, font, blockName(entry.block()), x + 22, y + 1, textWidth, nameColor);
            Component detail = Component.translatable("ultraloot.gui.scan.entry", entry.distance(), entry.freeSlots(), entry.totalSlots());
            GuiDraw.trimmed(graphics, font, detail, x + 22, y + 10, textWidth, GuiDraw.DIM);
            if (picking) {
                renderCheckbox(graphics, x + width - 8 - CHECKBOX, y + (ScrollableList.ROW_HEIGHT - CHECKBOX) / 2, entry.picked(),
                        index == hovered);
            }
            graphics.fill(x + width - 5, y + 2, x + width - 2, y + ScrollableList.ROW_HEIGHT - 2, stateColor(entry.state()));
        }

        renderButton(graphics, pickModeName(picking), PICK_MODE_X, BOTTOM_Y, PICK_MODE_WIDTH, mx, my, true);
        renderButton(graphics, Component.translatable("ultraloot.gui.scan.rescan"), RESCAN_X, BOTTOM_Y, RESCAN_WIDTH, mx, my, true);
    }

    // ---- Tooltips ---------------------------------------------------------------------------------------

    @Override
    protected List<Component> tabTooltip(int tab, double mx, double my) {
        refresh();
        return switch (tab) {
            case LootGeneratorMenu.TAB_OVERVIEW -> overviewTooltip(mx, my);
            case LootGeneratorMenu.TAB_LOOT -> lootTooltip(mx, my);
            case LootGeneratorMenu.TAB_SCAN -> scanTooltip(mx, my);
            case LootGeneratorMenu.TAB_SIDES -> sides.tooltip(mx, my);
            default -> List.of();
        };
    }

    private List<Component> overviewTooltip(double mx, double my) {
        if (overBar(mx, my, ENERGY_X, BAR_Y)) return energyTooltip();
        if (isOver(mx, my, TEXT_X, PROGRESS_Y, TEXT_WIDTH, 9)) return progressTooltip();
        if (statusCut && isOver(mx, my, TEXT_X, STATUS_Y - 1, TEXT_WIDTH, 9)) {
            return List.of(Component.translatable(menu.status().translationKey()));
        }
        if (isOver(mx, my, TEXT_X, 82, TEXT_WIDTH, 20)) {
            ProfileView selected = selected();
            if (selected != null) return profileTooltip(selected, true);
        }
        if (menu.overflowCount() > 0 && isOver(mx, my, TEXT_X, 103, TEXT_WIDTH, 9)) {
            return List.of(Component.translatable("ultraloot.gui.buffered.desc").withStyle(ChatFormatting.GRAY));
        }
        return List.of();
    }

    private List<Component> profileTooltip(ProfileView view, boolean selected) {
        List<Component> lines = new ArrayList<>();
        lines.add(view.name());
        lines.add(view.category().copy().withStyle(ChatFormatting.GRAY));
        if (view.info().energyMultiplier() != 1.0) {
            lines.add(Component.translatable("ultraloot.gui.profile.energy", GuiFormat.multiplier(view.info().energyMultiplier()))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (locked(view.info())) {
            lines.add(Component.translatable("ultraloot.gui.profile.locked", MachineTier.byNumber(view.info().minTier()).roman())
                    .withStyle(ChatFormatting.RED));
        } else if (selected) {
            lines.add(Component.translatable("ultraloot.gui.selected").withStyle(ChatFormatting.GREEN));
            lines.add(Component.translatable("ultraloot.gui.profile.clear_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private List<Component> lootTooltip(double mx, double my) {
        int index = profileList.rowAt(mx, my);
        if (index >= 0) {
            ProfileView view = profiles.get(index);
            return profileTooltip(view, view.info().id().equals(menu.selectedProfile()));
        }
        List<Component> filter = filterTooltip(mx, my, 7, BOTTOM_Y);
        if (filter != null) return filter;
        if (isOver(mx, my, TOGGLE_X, BOTTOM_Y, TOGGLE_WIDTH, GuiSprites.BUTTON_HEIGHT)) {
            InventoryScanner.TargetMode mode = menu.targetMode();
            return List.of(Component.translatable("ultraloot.gui.target_mode", targetModeName(mode)),
                    Component.translatable("ultraloot.gui.target_mode." + mode.name().toLowerCase(Locale.ROOT) + ".desc")
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("ultraloot.gui.click_to_switch").withStyle(ChatFormatting.DARK_GRAY));
        }
        return List.of();
    }

    private List<Component> scanTooltip(double mx, double my) {
        int index = scanList.rowAt(mx, my);
        if (index >= 0) {
            ScanInfo entry = scan.get(index);
            List<Component> lines = new ArrayList<>(List.of(blockName(entry.block()),
                    Component.translatable(entry.state().translationKey())
                            .withStyle(s -> s.withColor(TextColor.fromRgb(stateColor(entry.state()) & 0xFFFFFF))),
                    Component.translatable("ultraloot.gui.scan.position", entry.pos().getX(), entry.pos().getY(), entry.pos().getZ())
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("ultraloot.gui.scan.distance", entry.distance()).withStyle(ChatFormatting.GRAY),
                    Component.translatable("ultraloot.gui.scan.slots", entry.freeSlots(), entry.totalSlots()).withStyle(ChatFormatting.GRAY)));
            if (menu.pickedOnly()) {
                lines.add(Component.translatable(entry.picked() ? "ultraloot.gui.scan.click_unpick" : "ultraloot.gui.scan.click_pick")
                        .withStyle(ChatFormatting.DARK_GRAY));
            } else {
                lines.add(Component.translatable("ultraloot.gui.scan.pick_hint").withStyle(ChatFormatting.DARK_GRAY));
            }
            return lines;
        }
        if (isOver(mx, my, PICK_MODE_X, BOTTOM_Y, PICK_MODE_WIDTH, GuiSprites.BUTTON_HEIGHT)) {
            boolean picking = menu.pickedOnly();
            return List.of(pickModeName(picking),
                    Component.translatable(picking ? "ultraloot.gui.scan.picked_only.desc" : "ultraloot.gui.scan.all_in_range.desc")
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("ultraloot.gui.scan.summary", menu.targetCount(), menu.range(), menu.pickedCount())
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("ultraloot.gui.click_to_switch").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (isOver(mx, my, RESCAN_X, BOTTOM_Y, RESCAN_WIDTH, GuiSprites.BUTTON_HEIGHT)) {
            return List.of(Component.translatable("ultraloot.gui.scan.rescan.desc"));
        }
        return List.of();
    }

    // ---- Input ------------------------------------------------------------------------------------------

    @Override
    protected boolean tabClicked(int tab, double mx, double my, int button) {
        refresh();
        return switch (tab) {
            case LootGeneratorMenu.TAB_LOOT -> lootClicked(mx, my, button);
            case LootGeneratorMenu.TAB_SCAN -> scanClicked(mx, my, button);
            case LootGeneratorMenu.TAB_SIDES -> sides.mouseClicked(mx, my, button);
            default -> false;
        };
    }

    private boolean lootClicked(double mx, double my, int button) {
        int index = profileList.rowAt(mx, my);
        if (index >= 0) {
            ProfileInfo info = profiles.get(index).info();
            boolean isSelected = info.id().equals(menu.selectedProfile());
            if (button == 0 && !isSelected && !locked(info)) return send(MachineAction.SET_PROFILE, 0, info.id().toString());
            if (button == 1 && isSelected) return send(MachineAction.SET_PROFILE, 0, "");
            return false;
        }
        if (button != 0) return false;
        if (filterClicked(mx, my, 7, BOTTOM_Y)) return true;
        if (isOver(mx, my, TOGGLE_X, BOTTOM_Y, TOGGLE_WIDTH, GuiSprites.BUTTON_HEIGHT)) {
            InventoryScanner.TargetMode next = menu.targetMode() == InventoryScanner.TargetMode.STORAGE_ONLY
                    ? InventoryScanner.TargetMode.ANY_INVENTORY : InventoryScanner.TargetMode.STORAGE_ONLY;
            return send(MachineAction.SET_TARGET_MODE, next.ordinal(), "");
        }
        return false;
    }

    private boolean scanClicked(double mx, double my, int button) {
        if (button != 0) return false;
        int index = scanList.rowAt(mx, my);
        if (index >= 0) {
            if (!menu.pickedOnly()) return false;
            int[] pos = MachineAction.packPos(scan.get(index).pos().asLong());
            MachineActionPayload.send(menu, MachineAction.TOGGLE_PICKED, pos[0], pos[1]);
            return true;
        }
        if (isOver(mx, my, PICK_MODE_X, BOTTOM_Y, PICK_MODE_WIDTH, GuiSprites.BUTTON_HEIGHT)) {
            return send(MachineAction.SET_PICKED_ONLY, menu.pickedOnly() ? 0 : 1, "");
        }
        return isOver(mx, my, RESCAN_X, BOTTOM_Y, RESCAN_WIDTH, GuiSprites.BUTTON_HEIGHT) && send(MachineAction.RESCAN, 0, "");
    }

    private boolean send(MachineAction action, int a, String text) {
        MachineActionPayload.send(menu, action, a, 0, text);
        return true;
    }
}
