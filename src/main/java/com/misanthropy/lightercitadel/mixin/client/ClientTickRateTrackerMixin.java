package com.misanthropy.lightercitadel.mixin.client;

import com.github.alexthe666.citadel.client.tick.ClientTickRateTracker;
import com.github.alexthe666.citadel.server.tick.TickRateTracker;
import com.misanthropy.lightercitadel.LighterCitadelConfig;
import com.misanthropy.lightercitadel.client.ClientTrackerCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientTickRateTracker.class, remap = false)
public abstract class ClientTickRateTrackerMixin {

    @Inject(method = "getForClient", at = @At("HEAD"), cancellable = true)
    private static void lightercitadel$getCached(Minecraft minecraft, CallbackInfoReturnable<ClientTickRateTracker> cir) {
        if (!LighterCitadelConfig.cacheClientTracker) {
            return;
        }
        ClientTickRateTracker cached = ClientTrackerCache.lookup(minecraft);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "getForClient", at = @At("RETURN"))
    private static void lightercitadel$storeInCache(Minecraft minecraft, CallbackInfoReturnable<ClientTickRateTracker> cir) {
        ClientTickRateTracker tracker = cir.getReturnValue();
        if (tracker != null) {
            ClientTrackerCache.store(minecraft, tracker);
        }
    }

    @Inject(method = "modifySoundPitch", at = @At("HEAD"), cancellable = true)
    private void lightercitadel$fastSoundPitch(SoundInstance soundInstance, CallbackInfoReturnable<Float> cir) {
        if (LighterCitadelConfig.fastSoundPitch && lightercitadel$noModifiers()) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "getClientTickRate", at = @At("HEAD"), cancellable = true)
    private void lightercitadel$fastClientTickRate(CallbackInfoReturnable<Float> cir) {
        if (LighterCitadelConfig.fastSoundPitch && lightercitadel$noModifiers()) {
            cir.setReturnValue(50.0F);
        }
    }

    @Unique
    private boolean lightercitadel$noModifiers() {
        return ((TickRateTracker) (Object) this).tickRateModifierList.isEmpty();
    }
}
