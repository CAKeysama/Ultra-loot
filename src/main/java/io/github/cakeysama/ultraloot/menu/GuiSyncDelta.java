package io.github.cakeysama.ultraloot.menu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Keeps large, rarely changing parts of the GUI snapshot (the Loot Generator's profile list) out of the payloads that
 * are sent every few ticks while a machine works. The server leaves a sticky key out when it equals the value the
 * client already has; the client keeps its previous value for any sticky key the payload omits.
 */
public final class GuiSyncDelta {
    /**
     * Keys whose values are only re-sent when they change. A sticky key must be written in every snapshot of the
     * menus that use it (an empty list rather than no key), otherwise the client would keep a stale value.
     */
    public static final List<String> STICKY_KEYS = List.of("profiles");

    private GuiSyncDelta() {}

    /** Server: the payload to send for {@code current}, given the snapshot sent last time (null on the first send). */
    public static CompoundTag strip(CompoundTag current, @Nullable CompoundTag lastSent) {
        if (lastSent == null) return current;
        CompoundTag out = new CompoundTag();
        for (String key : current.getAllKeys()) {
            Tag value = current.get(key);
            if (STICKY_KEYS.contains(key) && Objects.equals(value, lastSent.get(key))) continue;
            out.put(key, value);
        }
        return out;
    }

    /** Client: the full snapshot after receiving {@code payload}, given the previous full snapshot. */
    public static CompoundTag merge(CompoundTag payload, CompoundTag previous) {
        for (String key : STICKY_KEYS) {
            if (!payload.contains(key) && previous.contains(key)) payload.put(key, Objects.requireNonNull(previous.get(key)));
        }
        return payload;
    }
}
