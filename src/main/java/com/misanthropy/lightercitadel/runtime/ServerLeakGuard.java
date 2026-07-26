package com.misanthropy.lightercitadel.runtime;

import com.github.alexthe666.citadel.server.tick.ServerTickRateTracker;
import com.misanthropy.lightercitadel.LighterCitadelConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ServerLeakGuard {

    public static final ServerLeakGuard INSTANCE = new ServerLeakGuard();

    private ServerLeakGuard() {
    }

    @SubscribeEvent
    public void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!LighterCitadelConfig.purgeUnloadedEntities) {
            return;
        }
        Level level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ServerTickRateTracker tracker = CitadelDataCache.existingTracker(serverLevel.getServer());
        if (tracker == null || tracker.specialTickRateEntities.isEmpty()) {
            return;
        }
        ((TickListAccess) tracker).lightercitadel$purgeEntity(event.getEntity());
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (!LighterCitadelConfig.purgeUnloadedEntities) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        ServerTickRateTracker tracker = CitadelDataCache.existingTracker(serverLevel.getServer());
        if (tracker != null) {
            ((TickListAccess) tracker).lightercitadel$purgeLevel(serverLevel);
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        ServerTickRateTracker tracker = CitadelDataCache.existingTracker(event.getServer());
        if (tracker != null) {
            ((TickListAccess) tracker).lightercitadel$purgeAll();
        }
        CitadelDataCache.invalidate();
    }
}
