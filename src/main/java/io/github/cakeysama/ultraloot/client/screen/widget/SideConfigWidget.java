package io.github.cakeysama.ultraloot.client.screen.widget;

import io.github.cakeysama.ultraloot.client.screen.GuiDraw;
import io.github.cakeysama.ultraloot.client.screen.GuiSprites;
import io.github.cakeysama.ultraloot.machine.RelativeSide;
import io.github.cakeysama.ultraloot.machine.SideMode;
import io.github.cakeysama.ultraloot.menu.MachineAction;
import io.github.cakeysama.ultraloot.menu.MachineMenu;
import io.github.cakeysama.ultraloot.network.MachineActionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.util.List;
import java.util.Locale;

/**
 * Unfolded-cube side configuration: FRONT in the centre, TOP above, BOTTOM below, LEFT/RIGHT beside it and BACK at
 * the far right. Each face is filled with its {@link SideMode#color()}; left-click cycles forward, right-click back.
 * The server only accepts modes allowed by the machine family. Coordinates are relative to the GUI panel.
 */
public class SideConfigWidget {
    private static final int SIZE = 18;
    private static final int STEP = 20;
    private static final int LEGEND_Y = 86;
    private static final int LEGEND_ROW = 11;
    private static final int LEGEND_X = 10;
    private static final int LEGEND_COLUMN = 78;

    private final MachineMenu menu;
    private final int originX;
    private final int originY;

    /** @param originX x of the LEFT face; @param originY y of the TOP face */
    public SideConfigWidget(MachineMenu menu, int originX, int originY) {
        this.menu = menu;
        this.originX = originX;
        this.originY = originY;
    }

    private int faceX(RelativeSide side) {
        return originX + STEP * switch (side) {
            case LEFT -> 0;
            case FRONT, TOP, BOTTOM -> 1;
            case RIGHT -> 2;
            case BACK -> 3;
        };
    }

    private int faceY(RelativeSide side) {
        return originY + STEP * switch (side) {
            case TOP -> 0;
            case BOTTOM -> 2;
            default -> 1;
        };
    }

    private RelativeSide faceAt(double mx, double my) {
        for (RelativeSide side : RelativeSide.values()) {
            int x = faceX(side);
            int y = faceY(side);
            if (mx >= x && mx < x + SIZE && my >= y && my < y + SIZE) return side;
        }
        return null;
    }

    public void render(GuiGraphics graphics, Font font, double mx, double my) {
        RelativeSide hovered = faceAt(mx, my);
        for (RelativeSide side : RelativeSide.values()) {
            int x = faceX(side);
            int y = faceY(side);
            int color = menu.sideMode(side).color();
            graphics.fillGradient(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, GuiDraw.lighten(color, 0.15f), GuiDraw.darken(color, 0.7f));
            GuiSprites.SIDE_OVERLAY.draw(graphics, x, y);
            if (side == hovered) GuiSprites.SIDE_HOVER.draw(graphics, x, y);
            String letter = Component.translatable("ultraloot.gui.side_short." + side.name().toLowerCase(Locale.ROOT)).getString();
            graphics.drawString(font, letter, x + (SIZE - font.width(letter)) / 2, y + 5, 0xFFFFFFFF, true);
        }

        List<SideMode> modes = menu.family().allowedModes();
        for (int i = 0; i < modes.size(); i++) {
            SideMode mode = modes.get(i);
            int x = legendX(i);
            int y = legendY(i);
            graphics.fill(x, y, x + 7, y + 7, 0xFF101216);
            graphics.fill(x + 1, y + 1, x + 6, y + 6, 0xFF000000 | mode.color());
            GuiDraw.trimmed(graphics, font, Component.translatable(mode.translationKey()), x + 10, y, LEGEND_COLUMN - 10, GuiDraw.TEXT);
        }
    }

    private static int legendX(int index) {
        return LEGEND_X + (index % 2) * (LEGEND_COLUMN + 4);
    }

    private static int legendY(int index) {
        return LEGEND_Y + (index / 2) * LEGEND_ROW;
    }

    public List<Component> tooltip(double mx, double my) {
        RelativeSide side = faceAt(mx, my);
        if (side != null) {
            SideMode mode = menu.sideMode(side);
            return List.of(
                    Component.translatable(side.translationKey()),
                    Component.translatable(mode.translationKey()).withStyle(s -> s.withColor(TextColor.fromRgb(mode.color()))),
                    Component.translatable("ultraloot.gui.sides.hint").withStyle(ChatFormatting.GRAY));
        }
        List<SideMode> modes = menu.family().allowedModes();
        for (int i = 0; i < modes.size(); i++) {
            int x = legendX(i);
            int y = legendY(i);
            if (mx >= x && mx < x + LEGEND_COLUMN && my >= y - 1 && my < y + 9) {
                return List.of(Component.translatable(modes.get(i).translationKey()));
            }
        }
        return List.of();
    }

    /** @return true if a face was clicked (the request was sent) */
    public boolean mouseClicked(double mx, double my, int button) {
        RelativeSide side = faceAt(mx, my);
        if (side == null || (button != 0 && button != 1)) return false;
        MachineActionPayload.send(menu, MachineAction.CYCLE_SIDE, side.ordinal(), button == 0 ? 1 : -1);
        return true;
    }
}
