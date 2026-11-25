package com.misanthropy.lightercitadel.runtime;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public interface TickListAccess {
    void lightercitadel$purgeEntity(Entity entity);

    void lightercitadel$purgeLevel(Level level);

    void lightercitadel$purgeAll();
}
