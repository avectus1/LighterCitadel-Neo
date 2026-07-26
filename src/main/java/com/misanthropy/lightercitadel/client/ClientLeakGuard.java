package com.misanthropy.lightercitadel.client;

import com.github.alexthe666.citadel.client.tick.ClientTickRateTracker;
import com.misanthropy.lightercitadel.LighterCitadelConfig;
import com.misanthropy.lightercitadel.runtime.TickListAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "lightercitadel")
public final class ClientLeakGuard {

    private ClientLeakGuard() {
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!LighterCitadelConfig.purgeUnloadedEntities || !(event.getLevel() instanceof ClientLevel)) {
            return;
        }
        ClientTickRateTracker tracker = ClientTrackerCache.existing();
        if (tracker == null || tracker.specialTickRateEntities.isEmpty()) {
            return;
        }
        ((TickListAccess) tracker).lightercitadel$purgeEntity(event.getEntity());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!LighterCitadelConfig.purgeUnloadedEntities || !(event.getLevel() instanceof ClientLevel level)) {
            return;
        }
        ClientTickRateTracker tracker = ClientTrackerCache.existing();
        if (tracker != null) {
            ((TickListAccess) tracker).lightercitadel$purgeLevel(level);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientTickRateTracker tracker = ClientTrackerCache.existing();
        if (tracker != null) {
            ((TickListAccess) tracker).lightercitadel$purgeAll();
        }
        HoverAnimationState.clearAll();
    }
}
