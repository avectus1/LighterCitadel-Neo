package com.misanthropy.lightercitadel.mixin.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Marker only. Its priority puts it after Citadel's LivingEntityRendererMixin, so by the time
 * LighterCitadelMixinPlugin#postApply sees the class, Citadel's handlers are merged and can be guarded.
 */
@Mixin(value = LivingEntityRenderer.class, priority = 1500)
public class LivingEntityRendererEventsMixin {
    @Unique
    private static final boolean LIGHTERCITADEL$MARKER = true;
}
