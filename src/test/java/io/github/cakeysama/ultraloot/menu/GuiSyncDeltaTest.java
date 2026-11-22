package io.github.cakeysama.ultraloot.menu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiSyncDeltaTest {
    private static CompoundTag snapshot(int progress, String... profiles) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("progress", progress);
        ListTag list = new ListTag();
        for (String p : profiles) list.add(StringTag.valueOf(p));
        tag.put("profiles", list);
        return tag;
    }

    @Test
    void firstSnapshotIsSentWhole() {
        CompoundTag first = snapshot(1, "a", "b");
        assertSame(first, GuiSyncDelta.strip(first, null));
    }

    @Test
    void unchangedProfilesAreLeftOutAndRestoredByTheClient() {
        CompoundTag sent = snapshot(1, "a", "b");
        CompoundTag next = snapshot(2, "a", "b");
        CompoundTag payload = GuiSyncDelta.strip(next, sent);
        assertFalse(payload.contains("profiles"), "unchanged profile list was re-sent");
        assertEquals(2, payload.getInt("progress"));

        CompoundTag client = GuiSyncDelta.merge(payload, sent.copy());
        assertEquals(next, client, "client snapshot must equal the server snapshot");
    }

    @Test
    void changedProfilesAreSent() {
        CompoundTag sent = snapshot(1, "a", "b");
        CompoundTag next = snapshot(1, "a", "c");
        CompoundTag payload = GuiSyncDelta.strip(next, sent);
        assertTrue(payload.contains("profiles"));
        assertEquals(next, GuiSyncDelta.merge(payload, sent.copy()));
    }

    @Test
    void emptyProfileListReplacesAFullOne() {
        CompoundTag sent = snapshot(1, "a");
        CompoundTag next = snapshot(1);
        assertEquals(next, GuiSyncDelta.merge(GuiSyncDelta.strip(next, sent), sent.copy()));
    }

    @Test
    void nonStickyKeysAreNeverCarriedOver() {
        CompoundTag previous = snapshot(1, "a");
        previous.putString("mob", "minecraft:zombie");
        CompoundTag payload = new CompoundTag();
        payload.putInt("progress", 3);
        CompoundTag merged = GuiSyncDelta.merge(payload, previous);
        assertFalse(merged.contains("mob"), "a removed key (card taken out) must not survive on the client");
        assertTrue(merged.contains("profiles"));
    }
}
