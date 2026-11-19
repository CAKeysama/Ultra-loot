package io.github.cakeysama.ultraloot.client.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/** Number formatting shared by the machine screens. */
public final class GuiFormat {
    private GuiFormat() {}

    public static String number(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    public static String multiplier(double value) {
        return String.format(Locale.ROOT, "x%.2f", value);
    }

    public static String percent(float fraction) {
        return Math.round(fraction * 100f) + "%";
    }

    public static Component energy(long value) {
        return Component.translatable("ultraloot.gui.unit.fe", number(value));
    }

    public static Component experience(long value) {
        return Component.translatable("ultraloot.gui.unit.xp", number(value));
    }

    public static Component seconds(int ticks) {
        return Component.translatable("ultraloot.gui.unit.seconds", String.format(Locale.ROOT, "%.1f", ticks / 20.0));
    }

    public static MutableComponent fraction(long value, long max) {
        return Component.literal(number(value) + " / " + number(max));
    }
}
