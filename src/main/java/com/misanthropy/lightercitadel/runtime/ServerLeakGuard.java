package com.misanthropy.lightercitadel.runtime;

import com.github.alexthe666.citadel.server.tick.ServerTickRateTracker;
import com.misanthropy.lightercitadel.LighterCitadelConfig;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class ServerLeakGuard {
    public static final ServerLeakGuard INSTANCE = new ServerLeakGuard();

    private ServerLeakGuard() {
    }

    @SubscribeEvent
    public void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!LighterCitadelConfig.purgeUnloadedEntities || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ServerTickRateTracker tracker = CitadelDataCache.existingTracker(level.getServer());
        if (tracker == null || tracker.specialTickRateEntities.isEmpty()) {
            return;
        }
        if (tracker instanceof TickListAccess access) {
            access.lightercitadel$purgeEntity(event.getEntity());
        }
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (!LighterCitadelConfig.purgeUnloadedEntities || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ServerTickRateTracker tracker = CitadelDataCache.existingTracker(level.getServer());
        if (tracker instanceof TickListAccess access) {
            access.lightercitadel$purgeLevel(level);
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        try {
            ServerTickRateTracker tracker = CitadelDataCache.existingTracker(event.getServer());
            if (tracker instanceof TickListAccess access) {
                access.lightercitadel$purgeAll();
            }
        } finally {
            CitadelDataCache.invalidate();
        }
    }
}
