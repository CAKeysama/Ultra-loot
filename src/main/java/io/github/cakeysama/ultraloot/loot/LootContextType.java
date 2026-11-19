package io.github.cakeysama.ultraloot.loot;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * How a loot table is executed. Each type maps to the vanilla parameter set the table was written for, so tables
 * are never run with a context they cannot use.
 */
public enum LootContextType implements StringRepresentable {
    /** Structure chests: origin = machine position. */
    CHEST("chest"),
    /** Fishing: origin, a plain fishing rod and a detached open-water bobber (treasure can roll). */
    FISHING("fishing"),
    /** Suspicious sand/gravel tables. */
    ARCHAEOLOGY("archaeology"),
    /** Piglin bartering: uses a piglin instance that is never added to the world. */
    BARTER("barter"),
    /** Tables that need no parameters (custom data-pack tables). */
    EMPTY("empty");

    public static final Codec<LootContextType> CODEC = StringRepresentable.fromEnum(LootContextType::values);

    private final String name;

    LootContextType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
