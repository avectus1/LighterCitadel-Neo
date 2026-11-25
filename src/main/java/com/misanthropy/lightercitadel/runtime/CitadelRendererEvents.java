package com.misanthropy.lightercitadel.runtime;

import com.github.alexthe666.citadel.client.event.EventLivingRenderer;

/**
 * Gates woven into Citadel's LivingEntityRenderer handlers. Called once per handler per rendered entity.
 */
public final class CitadelRendererEvents {
    private static final EventListenerProbe SETUP_ROTATIONS = EventListenerProbe.of(EventLivingRenderer.SetupRotations.class);
    private static final EventListenerProbe PRE_SETUP_ANIMATIONS = EventListenerProbe.of(EventLivingRenderer.PreSetupAnimations.class);
    private static final EventListenerProbe POST_SETUP_ANIMATIONS = EventListenerProbe.of(EventLivingRenderer.PostSetupAnimations.class);
    private static final EventListenerProbe POST_RENDER_MODEL = EventListenerProbe.of(EventLivingRenderer.PostRenderModel.class);

    private CitadelRendererEvents() {
    }

    public static boolean wantsSetupRotations() {
        return SETUP_ROTATIONS.hasListeners();
    }

    public static boolean wantsPreSetupAnimations() {
        return PRE_SETUP_ANIMATIONS.hasListeners();
    }

    public static boolean wantsPostSetupAnimations() {
        return POST_SETUP_ANIMATIONS.hasListeners();
    }

    public static boolean wantsPostRenderModel() {
        return POST_RENDER_MODEL.hasListeners();
    }
}
