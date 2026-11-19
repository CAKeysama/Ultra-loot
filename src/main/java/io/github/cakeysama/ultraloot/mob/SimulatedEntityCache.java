package io.github.cakeysama.ultraloot.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Holds one entity instance used purely as loot context. The instance is constructed with
 * {@link EntityType#create(net.minecraft.world.level.Level)} and is <b>never</b> added to the level: it does not
 * tick, cannot be seen, collides with nothing and is not saved. Owned by one machine; the server thread is the only
 * user.
 */
public final class SimulatedEntityCache {
    @Nullable
    private Entity entity;
    @Nullable
    private EntityType<?> type;
    @Nullable
    private ServerLevel level;

    /** @return a positioned, detached living instance, or null if the type is not a living entity. */
    @Nullable
    public LivingEntity getLiving(ServerLevel serverLevel, EntityType<?> entityType, Vec3 position) {
        return get(serverLevel, entityType, position) instanceof LivingEntity living ? living : null;
    }

    /** @return a positioned, detached instance, or null if the type cannot be instantiated. */
    @Nullable
    public Entity get(ServerLevel serverLevel, EntityType<?> entityType, Vec3 position) {
        if (entity == null || type != entityType || level != serverLevel) {
            entity = null;
            type = entityType;
            level = serverLevel;
            try {
                entity = entityType.create(serverLevel);
            } catch (RuntimeException e) {
                // Some modded entities cannot be constructed outside normal spawning; treat them as unsupported.
                entity = null;
            }
        }
        if (entity != null) entity.moveTo(position.x, position.y, position.z, 0f, 0f);
        return entity;
    }

    public void clear() {
        entity = null;
        type = null;
        level = null;
    }
}
