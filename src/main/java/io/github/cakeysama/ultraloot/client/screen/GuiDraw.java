package io.github.cakeysama.ultraloot.client.screen;

import io.github.cakeysama.ultraloot.machine.MachineStatus;
import io.github.cakeysama.ultraloot.machine.MachineTier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Colours and small drawing helpers matching the generated GUI sheets. */
public final class GuiDraw {
    public static final int TEXT = 0xFFE6E9EE;
    public static final int LABEL = 0xFF9AA3AF;
    public static final int DIM = 0xFF6E7682;
    public static final int GOOD = 0xFF5BE08A;
    public static final int WARN = 0xFFFFB13B;
    public static final int BAD = 0xFFFF5A5A;
    public static final int ACCENT_LOOT = 0xFFE8B23A;
    public static final int ACCENT_MOB = 0xFF3FD0C9;
    public static final int ROW_HOVER = 0x22FFFFFF;
    public static final int ROW_ALT = 0x0CFFFFFF;
    public static final int SEPARATOR = 0xFF2E333B;

    private static final int INSET_FILL = 0xFF15171B;
    private static final int INSET_DARK = 0xFF0E1013;
    private static final int INSET_LIGHT = 0xFF5A616C;
    private static final int[] TIER_COLORS = {0xFFC8C8C8, 0xFFF2C94C, 0xFF3DDC84, 0xFFC9A0FF};
    private static final String ELLIPSIS = "...";

    private GuiDraw() {}

    /** Recessed box: dark top/left edge, light bottom/right edge, dark fill. */
    public static void inset(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, INSET_FILL);
        graphics.fill(x, y, x + w - 1, y + 1, INSET_DARK);
        graphics.fill(x, y, x + 1, y + h - 1, INSET_DARK);
        graphics.fill(x + 1, y + h - 1, x + w, y + h, INSET_LIGHT);
        graphics.fill(x + w - 1, y + 1, x + w, y + h, INSET_LIGHT);
    }

    public static int tierColor(MachineTier tier) {
        return TIER_COLORS[tier.index()];
    }

    public static int statusColor(MachineStatus status) {
        if (status.isWorking()) return GOOD;
        return switch (status) {
            case NO_PROFILE, NO_MOB_CARD -> TEXT;
            case NO_ENERGY, INVALID_PROFILE, INVALID_MOB -> BAD;
            default -> WARN;
        };
    }

    /** Draws text cut to {@code maxWidth} with an ellipsis. @return true if it was cut */
    public static boolean trimmed(GuiGraphics graphics, Font font, Component text, int x, int y, int maxWidth, int color) {
        if (font.width(text) <= maxWidth) {
            graphics.drawString(font, text, x, y, color, false);
            return false;
        }
        String cut = font.plainSubstrByWidth(text.getString(), Math.max(0, maxWidth - font.width(ELLIPSIS))) + ELLIPSIS;
        graphics.drawString(font, cut, x, y, color, false);
        return true;
    }

    /** Label on the left, value right-aligned within {@code width}; the label is cut first, then the value. */
    public static void row(GuiGraphics graphics, Font font, Component label, Component value, int x, int y, int width, int valueColor) {
        int valueWidth = Math.min(font.width(value), width - 24);
        trimmed(graphics, font, value, x + width - valueWidth, y, valueWidth, valueColor);
        trimmed(graphics, font, label, x, y, width - valueWidth - 4, LABEL);
    }

    public static void centered(GuiGraphics graphics, Font font, Component text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    public static int darken(int rgb, float factor) {
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >> 8) & 0xFF) * factor);
        int b = (int) ((rgb & 0xFF) * factor);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public static int lighten(int rgb, float amount) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        r += (int) ((255 - r) * amount);
        g += (int) ((255 - g) * amount);
        b += (int) ((255 - b) * amount);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
