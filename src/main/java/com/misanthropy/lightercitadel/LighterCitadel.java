package com.misanthropy.lightercitadel;

//import com.misanthropy.lightercitadel.debug.SelfTest;
import com.misanthropy.lightercitadel.runtime.EventListenerProbe;
import com.misanthropy.lightercitadel.runtime.ServerLeakGuard;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(LighterCitadel.MODID)
public class LighterCitadel {

    public static final String MODID = "lightercitadel";
    public static final Logger LOGGER = LogManager.getLogger("lightercitadel");

    public LighterCitadel() {
        LighterCitadelConfig.load();

        MinecraftForge.EVENT_BUS.register(ServerLeakGuard.INSTANCE);

//        if (SelfTest.enabled()) {
//            MinecraftForge.EVENT_BUS.register(new SelfTest());
//            LOGGER.info("Self-test will run once the server has started.");
//        }

        LOGGER.info("LighterCitadel - fast path: {}, data cache: {}, mtick: {}, "
                        + "hover animations: {}, follower render: {}, renderer event skip: {}",
                LighterCitadelConfig.fastEntityTickCheckServer,
                LighterCitadelConfig.cacheServerDataLookup,
                LighterCitadelConfig.fastMasterTick,
                LighterCitadelConfig.optimizeHoverAnimations,
                LighterCitadelConfig.optimizePatreonFollowerRender,
                LighterCitadelConfig.skipUnlistenedRendererEvents);

        if (LighterCitadelConfig.skipUnlistenedRendererEvents && !EventListenerProbe.isAvailable()) {
            LOGGER.warn("skip_unlistened_renderer_events is enabled but the bus id of the forge could not be read.. "
                    + "Citadel's renderer events will be posted as normal.");
        }
    }
}
