package io.github.cakeysama.ultraloot.client.screen;

import io.github.cakeysama.ultraloot.client.screen.GuiSprites.ButtonState;
import io.github.cakeysama.ultraloot.client.screen.widget.ScrollableList;
import io.github.cakeysama.ultraloot.client.screen.widget.SideConfigWidget;
import io.github.cakeysama.ultraloot.loot.LootFilter;
import io.github.cakeysama.ultraloot.machine.MachineFamily;
import io.github.cakeysama.ultraloot.machine.MachineStatus;
import io.github.cakeysama.ultraloot.menu.MachineAction;
import io.github.cakeysama.ultraloot.menu.MachineMenu;
import io.github.cakeysama.ultraloot.menu.TabSlot;
import io.github.cakeysama.ultraloot.network.MachineActionPayload;
import io.github.cakeysama.ultraloot.upgrade.UpgradeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared screen of both machine families: graphite panel, vertical tabs on the left, and the tabs every machine has
 * (Upgrades, Side Configuration, Statistics). All values come from the menu's synced snapshot.
 * <p>
 * Tab content is drawn in panel-relative coordinates (the pose is translated to the panel origin); the content inset
 * spans {@code x 5..170, y 17..114}.
 */
public abstract class MachineScreen<M extends MachineMenu> extends AbstractContainerScreen<M> {
    /** Last open tab per family while the game runs. */
    private static final Map<MachineFamily, Integer> LAST_TAB = new EnumMap<>(MachineFamily.class);

    private static final int TAB_TOP = 6;
    private static final int TAB_STEP = 24;
    private static final int TAB_HEIGHT = 22;
    private static final int TAB_X = -24;
    private static final int TAB_SELECTED_X = -27;
    private static final int TAB_ICON_X = -20;
    private static final int TIER_PIPS_X = 145;
    private static final int TIER_PIPS_Y = 6;
    private static final int FILTER_STEP = 18;
    private static final int BAR_INNER = 86;

    protected record Tab(int id, Component name, GuiSprites.Sprite icon) {}

    private final List<Tab> tabs;
    private final ResourceLocation panelTexture;
    protected final SideConfigWidget sides;
    private int activeTab;

    protected MachineScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = MachineMenu.GUI_WIDTH;
        this.imageHeight = MachineMenu.GUI_HEIGHT;
        this.tabs = createTabs();
        this.panelTexture = GuiSprites.panel(menu.family());
        this.sides = new SideConfigWidget(menu, 49, 22);
    }

    /** Tabs in display order; a tab's id is its index and matches the menu's TAB_* constant. */
    protected abstract List<Tab> createTabs();

    /** Family accent colour (ARGB). */
    protected abstract int accent();

    /** Draws the active tab's content (panel-relative coordinates). */
    protected abstract void renderTab(GuiGraphics graphics, int tab, int mx, int my);

    protected abstract List<Component> tabTooltip(int tab, double mx, double my);

    /** @return true if the click triggered an action (a click sound is played) */
    protected abstract boolean tabClicked(int tab, double mx, double my, int button);

    /** The scrollable list shown on {@code tab}, or null. */
    @Nullable
    protected abstract ScrollableList listFor(int tab);

    protected static Tab tab(int id, String key, GuiSprites.Sprite icon) {
        return new Tab(id, Component.translatable("ultraloot.gui.tab." + key), icon);
    }

    // ---- Tabs -------------------------------------------------------------------------------------------

    @Override
    protected void init() {
        super.init();
        selectTab(Mth.clamp(LAST_TAB.getOrDefault(menu.family(), 0), 0, tabs.size() - 1));
    }

    protected int activeTab() {
        return activeTab;
    }

    private void selectTab(int tab) {
        activeTab = tab;
        menu.setActiveTab(tab);
        LAST_TAB.put(menu.family(), tab);
    }

    private static int tabY(int index) {
        return TAB_TOP + index * TAB_STEP;
    }

    private int tabAt(double mx, double my) {
        if (mx < TAB_SELECTED_X || mx >= 0) return -1;
        for (int i = 0; i < tabs.size(); i++) {
            int y = tabY(i);
            if (my >= y && my < y + TAB_HEIGHT) return i;
        }
        return -1;
    }

    private void renderTabButton(GuiGraphics graphics, Tab tab, boolean selected, boolean hovered) {
        int y = tabY(tab.id());
        if (selected) {
            GuiSprites.TAB_SELECTED.draw(graphics, TAB_SELECTED_X, y);
            graphics.fill(TAB_SELECTED_X + 2, y + 3, TAB_SELECTED_X + 3, y + TAB_HEIGHT - 3, accent());
        } else {
            GuiSprites.TAB.draw(graphics, TAB_X, y);
        }
        if (!selected && !hovered) graphics.setColor(0.68f, 0.7f, 0.74f, 1f);
        tab.icon().draw(graphics, TAB_ICON_X, y + 3);
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    // ---- Rendering --------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!menu.getCarried().isEmpty()) return;
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            renderTooltip(graphics, mouseX, mouseY);
            return;
        }
        double mx = mouseX - leftPos;
        double my = mouseY - topPos;
        List<Component> tooltip = hoveredSlot != null ? slotHint(hoveredSlot) : tooltipAt(mx, my);
        if (!tooltip.isEmpty()) graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    private List<Component> tooltipAt(double mx, double my) {
        int tab = tabAt(mx, my);
        if (tab >= 0) return List.of(tabs.get(tab).name());
        if (mx >= TIER_PIPS_X && mx < TIER_PIPS_X + 22 && my >= TIER_PIPS_Y - 1 && my < TIER_PIPS_Y + 5) {
            return List.of(Component.translatable("ultraloot.gui.tier", menu.tier().roman()));
        }
        return tabTooltip(activeTab, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        int hoveredTab = tabAt(mx, my);
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos, topPos, 0);
        for (Tab tab : tabs) {
            if (tab.id() != activeTab) renderTabButton(graphics, tab, false, tab.id() == hoveredTab);
        }
        graphics.blit(panelTexture, 0, 0, 0, 0, imageWidth, imageHeight);
        renderTabButton(graphics, tabs.get(activeTab), true, false);

        for (Slot slot : menu.slots) {
            if (slot instanceof TabSlot tabSlot && tabSlot.tab() == activeTab) {
                GuiSprites.SLOT.draw(graphics, slot.x - 1, slot.y - 1);
                GuiSprites.Sprite ghost = slot.hasItem() ? null : slotGhost(slot);
                if (ghost != null) ghost.draw(graphics, slot.x, slot.y);
            }
        }
        ScrollableList list = listFor(activeTab);
        if (list != null) list.renderFrame(graphics, mx, my);
        renderTab(graphics, activeTab, mx, my);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiDraw.trimmed(graphics, font, title, 9, 5, TIER_PIPS_X - 13, accent());
        int tierColor = GuiDraw.tierColor(menu.tier());
        for (int i = 0; i < 4; i++) {
            int x = TIER_PIPS_X + i * 6;
            graphics.fill(x, TIER_PIPS_Y, x + 4, TIER_PIPS_Y + 4, 0xFF101216);
            if (i < menu.tier().number()) graphics.fill(x, TIER_PIPS_Y, x + 3, TIER_PIPS_Y + 3, tierColor);
        }
    }

    // ---- Slot hints -------------------------------------------------------------------------------------

    @Nullable
    private UpgradeType upgradeSlotType(Slot slot) {
        for (UpgradeType type : UpgradeType.values()) {
            if (menu.upgradeSlotIndex(type) == slot.index) return type;
        }
        return null;
    }

    /** Icon drawn in an empty machine slot. */
    @Nullable
    protected GuiSprites.Sprite slotGhost(Slot slot) {
        UpgradeType type = upgradeSlotType(slot);
        return type != null ? GuiSprites.ghost(type) : null;
    }

    /** Tooltip of an empty hovered slot. */
    protected List<Component> slotHint(Slot slot) {
        UpgradeType type = upgradeSlotType(slot);
        if (type == null) return List.of();
        return List.of(Component.translatable(type.translationKey()),
                Component.translatable("ultraloot.gui.upgrade_slot.hint").withStyle(ChatFormatting.GRAY));
    }

    // ---- Shared widgets ---------------------------------------------------------------------------------

    protected void renderEnergyBar(GuiGraphics graphics, int x, int y) {
        GuiSprites.BAR_FRAME.draw(graphics, x, y);
        int filled = Math.round((float) menu.energy() / menu.energyMax() * BAR_INNER);
        GuiSprites.ENERGY_FILL.drawBottom(graphics, x + 1, y + 1, filled);
    }

    protected static boolean overBar(double mx, double my, int x, int y) {
        return mx >= x && mx < x + GuiSprites.BAR_FRAME.width() && my >= y && my < y + GuiSprites.BAR_FRAME.height();
    }

    protected List<Component> energyTooltip() {
        return List.of(Component.translatable("ultraloot.gui.energy"),
                Component.translatable("ultraloot.gui.unit.fe", GuiFormat.fraction(menu.energy(), menu.energyMax())).withStyle(ChatFormatting.GRAY),
                Component.translatable("ultraloot.gui.per_operation", GuiFormat.energy(menu.energyPerOperation()))
                        .withStyle(ChatFormatting.GRAY));
    }

    protected void renderVerticalFill(GuiGraphics graphics, GuiSprites.Sprite fill, int x, int y, float fraction) {
        GuiSprites.BAR_FRAME.draw(graphics, x, y);
        fill.drawBottom(graphics, x + 1, y + 1, Math.round(Mth.clamp(fraction, 0f, 1f) * BAR_INNER));
    }

    /** Status dot and text. @return true if the text was cut (callers then offer a tooltip) */
    protected boolean renderStatus(GuiGraphics graphics, int x, int y, int width) {
        MachineStatus status = menu.status();
        int color = GuiDraw.statusColor(status);
        GuiDraw.statusIndicator(graphics, status, x, y - 1);
        return GuiDraw.trimmed(graphics, font, Component.translatable(status.translationKey()), x + 10, y - 1, width - 10, color);
    }

    protected void renderProgress(GuiGraphics graphics, GuiSprites.Sprite fill, int x, int y, int width) {
        GuiDraw.inset(graphics, x, y, width, 9);
        fill.drawLeft(graphics, x + 1, y + 1, Math.round(menu.progressFraction() * (width - 2)));
        String percent = GuiFormat.percent(menu.progressFraction());
        graphics.drawString(font, percent, 166 - font.width(percent), y + 1, GuiDraw.LABEL, false);
    }

    protected List<Component> progressTooltip() {
        return List.of(Component.translatable("ultraloot.gui.progress", GuiFormat.percent(menu.progressFraction())),
                Component.translatable("ultraloot.gui.time_per_operation", GuiFormat.seconds(menu.ticksPerOperation()))
                        .withStyle(ChatFormatting.GRAY));
    }

    protected void row(GuiGraphics graphics, String key, Component value, int x, int y, int width, int color) {
        GuiDraw.row(graphics, font, Component.translatable(key), value, x, y, width, color);
    }

    protected void row(GuiGraphics graphics, String key, String value, int x, int y, int width) {
        row(graphics, key, Component.literal(value), x, y, width, GuiDraw.TEXT);
    }

    private boolean filterEnabled(LootFilter filter) {
        return filter == LootFilter.ALL || menu.filterUnlocked();
    }

    private static LootFilter filterAt(double mx, double my, int x, int y) {
        if (my < y || my >= y + GuiSprites.BUTTON_HEIGHT || mx < x) return null;
        int index = (int) ((mx - x) / FILTER_STEP);
        if (index >= LootFilter.values().length || mx - x - index * FILTER_STEP >= 16) return null;
        return LootFilter.byOrdinal(index);
    }

    /** Four 16x16 filter buttons starting at (x, y). */
    protected void renderFilters(GuiGraphics graphics, int x, int y, double mx, double my) {
        LootFilter current = menu.filter();
        LootFilter hovered = filterAt(mx, my, x, y);
        for (LootFilter filter : LootFilter.values()) {
            int bx = x + filter.ordinal() * FILTER_STEP;
            boolean enabled = filterEnabled(filter);
            ButtonState state = filter == current ? ButtonState.SELECTED
                    : !enabled ? ButtonState.DISABLED
                    : filter == hovered ? ButtonState.HOVER : ButtonState.NORMAL;
            GuiSprites.button(graphics, bx, y, 16, state);
            if (!enabled) graphics.setColor(0.4f, 0.4f, 0.42f, 1f);
            GuiSprites.filterIcon(filter).draw(graphics, bx + 2, y + 2);
            graphics.setColor(1f, 1f, 1f, 1f);
            if (filter == current) graphics.fill(bx + 3, y + 13, bx + 13, y + 14, accent());
        }
    }

    @Nullable
    protected List<Component> filterTooltip(double mx, double my, int x, int y) {
        LootFilter filter = filterAt(mx, my, x, y);
        if (filter == null) return null;
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("ultraloot.gui.filter", Component.translatable(filter.translationKey())));
        lines.add(Component.translatable("ultraloot.gui.filter." + filter.name().toLowerCase(Locale.ROOT) + ".desc")
                .withStyle(ChatFormatting.GRAY));
        if (filter == menu.filter()) {
            lines.add(Component.translatable("ultraloot.gui.selected").withStyle(ChatFormatting.GREEN));
        } else if (!filterEnabled(filter)) {
            lines.add(Component.translatable("ultraloot.gui.filter.locked").withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    protected boolean filterClicked(double mx, double my, int x, int y) {
        LootFilter filter = filterAt(mx, my, x, y);
        if (filter == null || filter == menu.filter() || !filterEnabled(filter)) return false;
        MachineActionPayload.send(menu, MachineAction.SET_FILTER, filter.ordinal(), 0);
        return true;
    }

    /** Text button of fixed height 16. */
    protected void renderButton(GuiGraphics graphics, Component text, int x, int y, int width, double mx, double my, boolean enabled) {
        boolean hovered = isOver(mx, my, x, y, width, GuiSprites.BUTTON_HEIGHT);
        ButtonState state = !enabled ? ButtonState.DISABLED : hovered ? ButtonState.HOVER : ButtonState.NORMAL;
        GuiSprites.button(graphics, x, y, width, state);
        int textWidth = Math.min(font.width(text), width - 6);
        GuiDraw.trimmed(graphics, font, text, x + (width - textWidth) / 2, y + 4, width - 6, enabled ? GuiDraw.TEXT : GuiDraw.DIM);
    }

    protected static boolean isOver(double mx, double my, int x, int y, int width, int height) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }

    // ---- Shared tabs ------------------------------------------------------------------------------------

    protected void renderUpgrades(GuiGraphics graphics) {
        boolean loot = menu.family() == MachineFamily.LOOT_GENERATOR;
        int left = 10;
        int right = 92;
        int width = 74;
        graphics.fill(10, 48, 166, 49, GuiDraw.SEPARATOR);
        row(graphics, "ultraloot.gui.stat.speed", GuiFormat.multiplier(menu.speedMultiplier()), left, 53, width);
        row(graphics, loot ? "ultraloot.gui.stat.attempts" : "ultraloot.gui.stat.kills", GuiFormat.number(menu.attempts()), right, 53, width);
        if (loot) {
            row(graphics, "ultraloot.gui.stat.range", GuiFormat.number(menu.range()), left, 65, width);
            row(graphics, "ultraloot.gui.stat.luck", "+" + menu.lootBonus(), right, 65, width);
        } else {
            row(graphics, "ultraloot.gui.stat.orb_radius", menu.orbRadius() > 0 ? GuiFormat.number(menu.orbRadius()) : "-", left, 65, width);
            row(graphics, "ultraloot.gui.stat.looting", GuiFormat.number(menu.lootBonus()), right, 65, width);
        }
        row(graphics, "ultraloot.gui.stat.energy_multiplier", GuiFormat.multiplier(menu.energyMultiplier()), left, 77, width);
        row(graphics, "ultraloot.gui.stat.energy_per_operation", GuiFormat.number(menu.energyPerOperation()), right, 77, width);
        row(graphics, "ultraloot.gui.stat.time_per_operation", GuiFormat.seconds(menu.ticksPerOperation()), left, 89, width, GuiDraw.TEXT);
        GuiDraw.centered(graphics, font, Component.translatable("ultraloot.gui.upgrades.hint"), 88, 102, GuiDraw.DIM);
    }

    protected void renderStatistics(GuiGraphics graphics) {
        List<Component[]> rows = new ArrayList<>();
        long items = menu.statItems();
        long operations = menu.statOperations();
        rows.add(stat("items", Component.literal(GuiFormat.number(items))));
        rows.add(stat("operations", Component.literal(GuiFormat.number(operations))));
        rows.add(stat("average", Component.literal(operations == 0 ? "-"
                : String.format(Locale.ROOT, "%.2f", (double) items / operations))));
        rows.add(stat("energy", GuiFormat.energy(menu.statEnergy())));
        if (menu.family() == MachineFamily.MOB_LOOT_GENERATOR) {
            rows.add(stat("experience", GuiFormat.experience(menu.statExperience())));
        }
        rows.add(stat("filtered", Component.literal(GuiFormat.number(menu.statFiltered()))));
        for (int i = 0; i < rows.size(); i++) {
            int y = 23 + i * 14;
            GuiDraw.row(graphics, font, rows.get(i)[0], rows.get(i)[1], 10, y, 156, GuiDraw.TEXT);
            if (i < rows.size() - 1) graphics.fill(10, y + 10, 166, y + 11, GuiDraw.SEPARATOR);
        }
    }

    private static Component[] stat(String key, Component value) {
        return new Component[]{Component.translatable("ultraloot.gui.stats." + key), value};
    }

    // ---- Input ------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = mouseX - leftPos;
        double my = mouseY - topPos;
        int tab = tabAt(mx, my);
        if (tab >= 0) {
            if (button == 0 && tab != activeTab) {
                selectTab(tab);
                playClickSound();
            }
            return true;
        }
        ScrollableList list = listFor(activeTab);
        if (list != null && button == 0 && list.mouseClicked(mx, my)) return true;
        if (tabClicked(activeTab, mx, my, button)) {
            playClickSound();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        ScrollableList list = listFor(activeTab);
        if (list != null && list.mouseDragged(mouseY - topPos)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        ScrollableList list = listFor(activeTab);
        if (list != null && list.mouseReleased()) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        ScrollableList list = listFor(activeTab);
        if (list != null && list.scroll(mouseX - leftPos, mouseY - topPos, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        boolean onTabs = mouseX >= guiLeft + TAB_SELECTED_X && mouseX < guiLeft
                && mouseY >= guiTop + TAB_TOP && mouseY < guiTop + tabY(tabs.size());
        return !onTabs && super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, button);
    }

    protected void playClickSound() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
