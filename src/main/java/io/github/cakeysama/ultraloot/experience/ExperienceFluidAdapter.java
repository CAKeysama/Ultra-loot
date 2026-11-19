package io.github.cakeysama.ultraloot.experience;

import io.github.cakeysama.ultraloot.config.Balance;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Fill-only fluid view of an XP storage for pipes of other mods. Accepts any fluid tagged {@code c:experience} and
 * converts it to points ({@link Balance#xpFluidRatio} mB per point). Only whole points are accepted, so no fluid is
 * lost to rounding. XP is never stored or exposed as a fluid.
 */
public class ExperienceFluidAdapter implements IFluidHandler {
    private final IExperienceStorage storage;

    public ExperienceFluidAdapter(IExperienceStorage storage) {
        this.storage = storage;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return (storage.getCapacity() - storage.getExperience()) * Balance.xpFluidRatio;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.is(Tags.Fluids.EXPERIENCE);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
        int ratio = Balance.xpFluidRatio;
        int points = resource.getAmount() / ratio;
        int accepted = storage.receiveExperience(points, action.simulate());
        return accepted * ratio;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
