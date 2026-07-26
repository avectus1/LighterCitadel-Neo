package com.misanthropy.lightercitadel.mixin.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = LivingEntityRenderer.class, priority = 1500)
public class LivingEntityRendererEventsMixin {

    @Unique
    private static final boolean LIGHTERCITADEL$MARKER = true;
}
