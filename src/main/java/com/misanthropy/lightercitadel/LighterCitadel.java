package com.misanthropy.lightercitadel;

import com.misanthropy.lightercitadel.runtime.EventListenerProbe;
import com.misanthropy.lightercitadel.runtime.ServerLeakGuard;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(LighterCitadel.MODID)
public final class LighterCitadel {
    public static final String MODID = "lightercitadel";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public LighterCitadel(IEventBus modBus) {
        LighterCitadelConfig.load();
        NeoForge.EVENT_BUS.register(ServerLeakGuard.INSTANCE);
        if (FMLEnvironment.dist.isClient()) {
            com.misanthropy.lightercitadel.client.ClientLeakGuard.register();
        }

        LOGGER.info("LighterCitadel - fast path: {}, data cache: {}, mtick: {}, hover animations: {}, follower render: {}, renderer event skip: {}",
                LighterCitadelConfig.fastEntityTickCheckServer,
                LighterCitadelConfig.cacheServerDataLookup,
                LighterCitadelConfig.fastMasterTick,
                LighterCitadelConfig.optimizeHoverAnimations,
                LighterCitadelConfig.optimizePatreonFollowerRender,
                LighterCitadelConfig.skipUnlistenedRendererEvents);

        if (LighterCitadelConfig.skipUnlistenedRendererEvents && FMLEnvironment.dist.isClient()) {
            if (!EventListenerProbe.isAvailable()) {
                LOGGER.warn("skip_unlistened_renderer_events is enabled but NeoForge's event bus listener lists could not be read. Citadel's renderer events will be posted as normal.");
            }
        }
    }
}
