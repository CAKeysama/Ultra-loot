package io.github.cakeysama.ultraloot.experience;

import io.github.cakeysama.ultraloot.UltraLoot;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

/** The public {@code ultraloot:experience} block capability (XP in integer points). */
public final class ExperienceCapability {
    public static final BlockCapability<IExperienceStorage, @Nullable Direction> BLOCK =
            BlockCapability.createSided(UltraLoot.id("experience"), IExperienceStorage.class);

    private ExperienceCapability() {}
}
