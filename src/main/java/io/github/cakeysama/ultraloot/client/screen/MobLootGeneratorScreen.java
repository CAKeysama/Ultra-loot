package io.github.cakeysama.ultraloot.client.screen;

import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.client.screen.widget.ScrollableList;
import io.github.cakeysama.ultraloot.config.Balance;
import io.github.cakeysama.ultraloot.menu.MobLootGeneratorMenu;
import io.github.cakeysama.ultraloot.menu.TabSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class MobLootGeneratorScreen extends MachineScreen<MobLootGeneratorMenu> {
    private static final int ENERGY_X = 9;
    private static final int XP_X = 25;
    private static final int BAR_Y = 20;
    private static final int TEXT_X = 44;
    private static final int TEXT_WIDTH = 122;
    private static final int STATUS_Y = 21;
    private static final int PROGRESS_Y = 32;
    private static final int PROGRESS_WIDTH = 98;
    /** Mob preview box (panel-relative). */
    private static final int PREVIEW_X = 118;
    private static final int PREVIEW_Y = 19;
    private static final int PREVIEW_W = 49;
    private static final int PREVIEW_H = 72;
    private static final int MOB_TEXT_X = 48;
    private static final int MOB_TEXT_WIDTH = 66;
    private static final int MOB_ROWS_X = 10;
    private static final int MOB_ROWS_WIDTH = 104;
    private static final int FILTER_X = 48;
    private static final int FILTER_Y = 95;

    @Nullable
    private EntityType<?> previewType;
    @Nullable
    private LivingEntity preview;
    private boolean statusCut;

    public MobLootGeneratorScreen(MobLootGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected List<Tab> createTabs() {
        return List.of(
                tab(MobLootGeneratorMenu.TAB_OVERVIEW, "overview", GuiSprites.ICON_OVERVIEW),
                tab(MobLootGeneratorMenu.TAB_UPGRADES, "upgrades", GuiSprites.ICON_UPGRADES),
                tab(MobLootGeneratorMenu.TAB_MOB, "mob_config", GuiSprites.ICON_MOB),
                tab(MobLootGeneratorMenu.TAB_INVENTORY, "inventory", GuiSprites.ICON_INVENTORY),
                tab(MobLootGeneratorMenu.TAB_SIDES, "side_config", GuiSprites.ICON_SIDES),
                tab(MobLootGeneratorMenu.TAB_STATS, "statistics", GuiSprites.ICON_STATS));
    }

    @Override
    protected int accent() {
        return GuiDraw.ACCENT_MOB;
    }

    @Nullable
    @Override
    protected ScrollableList listFor(int tab) {
        return null;
    }

    private Component mobName() {
        return menu.mob().map(EntityType::getDescription)
                .orElseGet(() -> Component.translatable("ultraloot.gui.mob.none"));
    }

    // ---- Rendering --------------------------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        if (activeTab() == MobLootGeneratorMenu.TAB_MOB) renderPreview(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTab(GuiGraphics graphics, int tab, int mx, int my) {
        switch (tab) {
            case MobLootGeneratorMenu.TAB_OVERVIEW -> renderOverview(graphics);
            case MobLootGeneratorMenu.TAB_UPGRADES -> renderUpgrades(graphics);
            case MobLootGeneratorMenu.TAB_MOB -> renderMobConfig(graphics, mx, my);
            case MobLootGeneratorMenu.TAB_INVENTORY -> renderInventory(graphics);
            case MobLootGeneratorMenu.TAB_SIDES -> sides.render(graphics, font, mx, my);
            case MobLootGeneratorMenu.TAB_STATS -> renderStatistics(graphics);
            default -> {}
        }
    }

    private void renderOverview(GuiGraphics graphics) {
        renderEnergyBar(graphics, ENERGY_X, BAR_Y);
        renderVerticalFill(graphics, GuiSprites.XP_FILL, XP_X, BAR_Y, (float) menu.experience() / menu.experienceMax());
        statusCut = renderStatus(graphics, TEXT_X, STATUS_Y, TEXT_WIDTH);
        renderProgress(graphics, GuiSprites.PROGRESS_MOB, TEXT_X, PROGRESS_Y, PROGRESS_WIDTH);

        int mobColor = menu.mob().isEmpty() ? GuiDraw.DIM : menu.mobSupported() ? GuiDraw.TEXT : GuiDraw.BAD;
        row(graphics, "ultraloot.gui.mob", mobName(), TEXT_X, 46, TEXT_WIDTH, mobColor);
        row(graphics, "ultraloot.gui.stat.xp_per_operation", GuiFormat.number(menu.experiencePerOperation()), TEXT_X, 57, TEXT_WIDTH);
        row(graphics, "ultraloot.gui.stat.energy_per_operation", GuiFormat.number(menu.energyPerOperation()), TEXT_X, 68, TEXT_WIDTH);
        int free = menu.outputFreeSlots();
        row(graphics, "ultraloot.gui.output", Component.translatable("ultraloot.gui.output.free", free, Balance.MOB_OUTPUT_SLOTS),
                TEXT_X, 79, TEXT_WIDTH, free == 0 ? GuiDraw.WARN : GuiDraw.TEXT);

        Component hint = Component.translatable("ultraloot.gui.xp_slot");
        int hintWidth = Math.min(font.width(hint), MobLootGeneratorMenu.XP_SLOT_X - 5 - TEXT_X);
        GuiDraw.trimmed(graphics, font, hint, MobLootGeneratorMenu.XP_SLOT_X - 5 - hintWidth, MobLootGeneratorMenu.XP_SLOT_Y + 5,
                hintWidth, GuiDraw.DIM);
    }

    private void renderMobConfig(GuiGraphics graphics, int mx, int my) {
        Optional<EntityType<?>> mob = menu.mob();
        GuiDraw.trimmed(graphics, font, mobName(), MOB_TEXT_X, 29, MOB_TEXT_WIDTH, mob.isPresent() ? GuiDraw.TEXT : GuiDraw.DIM);
        if (mob.isPresent()) {
            boolean supported = menu.mobSupported();
            GuiDraw.trimmed(graphics, font, Component.translatable(supported ? "ultraloot.gui.mob.supported" : "ultraloot.gui.mob.unsupported"),
                    MOB_TEXT_X, 40, MOB_TEXT_WIDTH, supported ? GuiDraw.GOOD : GuiDraw.BAD);
        }
        row(graphics, "ultraloot.gui.stat.xp_per_operation", GuiFormat.number(menu.experiencePerOperation()), MOB_ROWS_X, 58, MOB_ROWS_WIDTH);
        row(graphics, "ultraloot.gui.stat.kills", GuiFormat.number(menu.attempts()), MOB_ROWS_X, 69, MOB_ROWS_WIDTH);
        row(graphics, "ultraloot.gui.stat.looting", GuiFormat.number(menu.lootBonus()), MOB_ROWS_X, 80, MOB_ROWS_WIDTH);

        GuiDraw.inset(graphics, PREVIEW_X, PREVIEW_Y, PREVIEW_W, PREVIEW_H);
        if (mob.isEmpty()) {
            GuiDraw.centered(graphics, font, Component.literal("?"), PREVIEW_X + PREVIEW_W / 2, PREVIEW_Y + PREVIEW_H / 2 - 4, GuiDraw.DIM);
        }

        GuiDraw.trimmed(graphics, font, Component.translatable("ultraloot.gui.filter.label"), MOB_ROWS_X, FILTER_Y + 4,
                FILTER_X - MOB_ROWS_X - 3, GuiDraw.LABEL);
        renderFilters(graphics, FILTER_X, FILTER_Y, mx, my);
    }

    /** Renders the configured mob in the preview box (absolute coordinates, entity cached per type). */
    private void renderPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        EntityType<?> type = menu.mob().orElse(null);
        if (type != previewType) {
            previewType = type;
            preview = createPreview(type);
        }
        if (preview == null) return;
        float height = Math.max(0.3f, preview.getBbHeight());
        float width = Math.max(0.3f, preview.getBbWidth());
        int scale = (int) Math.max(4, Math.min(30, Math.min((PREVIEW_H - 14) / height, (PREVIEW_W - 12) / width)));
        int x1 = leftPos + PREVIEW_X + 1;
        int y1 = topPos + PREVIEW_Y + 1;
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x1, y1, x1 + PREVIEW_W - 2, y1 + PREVIEW_H - 2,
                scale, 0.0625f, mouseX, mouseY, preview);
    }

    @Nullable
    private LivingEntity createPreview(@Nullable EntityType<?> type) {
        if (type == null || minecraft == null || minecraft.level == null) return null;
        try {
            Entity entity = type.create(minecraft.level);
            return entity instanceof LivingEntity living ? living : null;
        } catch (RuntimeException e) {
            // A modded entity that cannot exist detached on the client simply gets no preview.
            UltraLoot.LOGGER.debug("No GUI preview for {}", type, e);
            return null;
        }
    }

    private void renderInventory(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("ultraloot.gui.output"), MobLootGeneratorMenu.OUTPUT_X, 19, GuiDraw.TEXT, false);
        int free = menu.outputFreeSlots();
        Component freeText = Component.translatable("ultraloot.gui.output.free", free, Balance.MOB_OUTPUT_SLOTS);
        graphics.drawString(font, freeText, 168 - font.width(freeText), 19, free == 0 ? GuiDraw.WARN : GuiDraw.LABEL, false);
        List<FormattedCharSequence> lines = font.split(Component.translatable("ultraloot.gui.output.hint"), 156);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            FormattedCharSequence line = lines.get(i);
            graphics.drawString(font, line, 88 - font.width(line) / 2, 91 + i * 10, GuiDraw.DIM, false);
        }
    }

    // ---- Tooltips and slot hints -------------------------------------------------------------------------

    @Override
    protected List<Component> tabTooltip(int tab, double mx, double my) {
        return switch (tab) {
            case MobLootGeneratorMenu.TAB_OVERVIEW -> overviewTooltip(mx, my);
            case MobLootGeneratorMenu.TAB_MOB -> mobTooltip(mx, my);
            case MobLootGeneratorMenu.TAB_SIDES -> sides.tooltip(mx, my);
            default -> List.of();
        };
    }

    private List<Component> overviewTooltip(double mx, double my) {
        if (overBar(mx, my, ENERGY_X, BAR_Y)) return energyTooltip();
        if (overBar(mx, my, XP_X, BAR_Y)) {
            return List.of(Component.translatable("ultraloot.gui.experience"),
                    Component.translatable("ultraloot.gui.unit.xp", GuiFormat.fraction(menu.experience(), menu.experienceMax()))
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("ultraloot.gui.per_operation", GuiFormat.experience(menu.experiencePerOperation()))
                            .withStyle(ChatFormatting.GRAY));
        }
        if (isOver(mx, my, TEXT_X, PROGRESS_Y, TEXT_WIDTH, 9)) return progressTooltip();
        if (statusCut && isOver(mx, my, TEXT_X, STATUS_Y - 1, TEXT_WIDTH, 9)) {
            return List.of(Component.translatable(menu.status().translationKey()));
        }
        if (menu.mob().isPresent() && isOver(mx, my, TEXT_X, 45, TEXT_WIDTH, 9)) return mobInfo();
        return List.of();
    }

    private List<Component> mobInfo() {
        boolean supported = menu.mobSupported();
        return List.of(mobName(),
                Component.translatable(supported ? "ultraloot.gui.mob.supported" : "ultraloot.gui.mob.unsupported")
                        .withStyle(supported ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private List<Component> mobTooltip(double mx, double my) {
        if (menu.mob().isPresent() && (isOver(mx, my, MOB_TEXT_X, 28, MOB_TEXT_WIDTH, 22)
                || isOver(mx, my, PREVIEW_X, PREVIEW_Y, PREVIEW_W, PREVIEW_H))) {
            return mobInfo();
        }
        if (isOver(mx, my, MOB_ROWS_X, 68, MOB_ROWS_WIDTH, 9)) {
            return List.of(Component.translatable("ultraloot.gui.stat.kills.desc").withStyle(ChatFormatting.GRAY));
        }
        if (isOver(mx, my, MOB_ROWS_X, 79, MOB_ROWS_WIDTH, 9)) {
            return List.of(Component.translatable("ultraloot.gui.stat.looting.desc").withStyle(ChatFormatting.GRAY));
        }
        List<Component> filter = filterTooltip(mx, my, FILTER_X, FILTER_Y);
        return filter != null ? filter : List.of();
    }

    @Nullable
    @Override
    protected GuiSprites.Sprite slotGhost(Slot slot) {
        if (slot instanceof TabSlot tabSlot) {
            if (tabSlot.tab() == MobLootGeneratorMenu.TAB_OVERVIEW) return GuiSprites.GHOST_XP;
            if (tabSlot.tab() == MobLootGeneratorMenu.TAB_MOB) return GuiSprites.GHOST_MOB_CARD;
        }
        return super.slotGhost(slot);
    }

    @Override
    protected List<Component> slotHint(Slot slot) {
        if (slot instanceof TabSlot tabSlot) {
            if (tabSlot.tab() == MobLootGeneratorMenu.TAB_OVERVIEW) {
                return List.of(Component.translatable("ultraloot.gui.xp_slot"),
                        Component.translatable("ultraloot.gui.xp_slot.hint").withStyle(ChatFormatting.GRAY));
            }
            if (tabSlot.tab() == MobLootGeneratorMenu.TAB_MOB) {
                return List.of(Component.translatable("ultraloot.gui.card_slot"),
                        Component.translatable("ultraloot.gui.card_slot.hint").withStyle(ChatFormatting.GRAY));
            }
        }
        return super.slotHint(slot);
    }

    // ---- Input ------------------------------------------------------------------------------------------

    @Override
    protected boolean tabClicked(int tab, double mx, double my, int button) {
        return switch (tab) {
            case MobLootGeneratorMenu.TAB_MOB -> button == 0 && filterClicked(mx, my, FILTER_X, FILTER_Y);
            case MobLootGeneratorMenu.TAB_SIDES -> sides.mouseClicked(mx, my, button);
            default -> false;
        };
    }
}
