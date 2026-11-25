package com.misanthropy.lightercitadel.mixin;

import com.github.alexthe666.citadel.ServerProxy;
import com.github.alexthe666.citadel.server.tick.ServerTickRateTracker;
import com.misanthropy.lightercitadel.runtime.CitadelDataCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerProxy.class, remap = false)
public class ServerProxyMixin {

    /**
     * Runs for every entity on every server tick. With no tick-rate modifiers and nothing on the
     * custom tick list, Citadel's answer is always "tick normally", so skip the list scan, the
     * modifier walk and the saved-data lookup.
     */
    @Inject(method = "canEntityTickServer", at = @At("HEAD"), cancellable = true)
    private void lightercitadel$fastEntityTickCheck(Level level, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel serverLevel)) {
            cir.setReturnValue(true);
            return;
        }
        ServerTickRateTracker tracker = CitadelDataCache.trackerFor(serverLevel.getServer());
        if (tracker != null && tracker.tickRateModifierList.isEmpty() && tracker.specialTickRateEntities.isEmpty()) {
            cir.setReturnValue(true);
        }
    }
}
