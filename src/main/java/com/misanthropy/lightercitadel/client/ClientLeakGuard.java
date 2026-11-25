package com.misanthropy.lightercitadel.client;

import com.github.alexthe666.citadel.client.tick.ClientTickRateTracker;
import com.misanthropy.lightercitadel.LighterCitadelConfig;
import com.misanthropy.lightercitadel.runtime.TickListAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public final class ClientLeakGuard {
    private ClientLeakGuard() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClientLeakGuard::onEntityLeaveLevel);
        NeoForge.EVENT_BUS.addListener(ClientLeakGuard::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(ClientLeakGuard::onLoggingOut);
    }

    private static ClientTickRateTracker tracker() {
        ClientTickRateTracker tracker = ClientTrackerCache.existing();
        if (tracker == null && !LighterCitadelConfig.cacheClientTracker) {
            tracker = ClientTickRateTracker.getForClient(net.minecraft.client.Minecraft.getInstance());
        }
        return tracker;
    }

    private static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!LighterCitadelConfig.purgeUnloadedEntities || !(event.getLevel() instanceof ClientLevel)) {
            return;
        }
        ClientTickRateTracker tracker = tracker();
        if (tracker == null || tracker.specialTickRateEntities.isEmpty()) {
            return;
        }
        if (tracker instanceof TickListAccess access) {
            access.lightercitadel$purgeEntity(event.getEntity());
        }
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (!LighterCitadelConfig.purgeUnloadedEntities || !(event.getLevel() instanceof ClientLevel level)) {
            return;
        }
        ClientTickRateTracker tracker = tracker();
        if (tracker instanceof TickListAccess access) {
            access.lightercitadel$purgeLevel(level);
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientTickRateTracker tracker = tracker();
        if (tracker instanceof TickListAccess access) {
            access.lightercitadel$purgeAll();
        }
        HoverAnimationState.clearAll();
    }
}
