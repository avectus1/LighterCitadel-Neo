package com.misanthropy.lightercitadel.mixin;

import com.github.alexthe666.citadel.server.world.CitadelServerData;
import com.misanthropy.lightercitadel.runtime.CitadelDataCache;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CitadelServerData.class, remap = false)
public class CitadelServerDataMixin {

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void lightercitadel$getCached(MinecraftServer server, CallbackInfoReturnable<CitadelServerData> cir) {
        CitadelServerData cached = CitadelDataCache.lookup(server);
        if (cached != null) {
            cached.setDirty();
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "get", at = @At("RETURN"))
    private static void lightercitadel$storeInCache(MinecraftServer server, CallbackInfoReturnable<CitadelServerData> cir) {
        CitadelServerData data = cir.getReturnValue();
        if (data != null) {
            CitadelDataCache.store(server, data);
        }
    }
}
