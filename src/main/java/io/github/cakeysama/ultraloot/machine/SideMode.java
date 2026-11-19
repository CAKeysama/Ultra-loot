package io.github.cakeysama.ultraloot.machine;

import net.minecraft.util.StringRepresentable;

/** Role assigned to one face of a machine. Which roles a machine accepts depends on its {@link MachineFamily}. */
public enum SideMode implements StringRepresentable {
    NONE("none", 0x5A5F69),
    ENERGY_INPUT("energy_input", 0xE04848),
    ITEM_TARGET("item_target", 0xE8B23A),
    XP_INPUT("xp_input", 0x7CDB3C),
    ITEM_OUTPUT("item_output", 0x3F8FE0);

    private final String name;
    private final int color;

    SideMode(String name, int color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** RGB colour used by the side configuration UI. */
    public int color() {
        return color;
    }

    public String translationKey() {
        return "ultraloot.side_mode." + name;
    }

    public static SideMode byOrdinal(int ordinal) {
        SideMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
    }
}
