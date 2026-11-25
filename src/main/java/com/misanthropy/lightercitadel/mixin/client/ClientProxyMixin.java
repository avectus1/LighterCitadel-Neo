package com.misanthropy.lightercitadel.mixin.client;

import com.github.alexthe666.citadel.ClientProxy;
import com.github.alexthe666.citadel.client.tick.ClientTickRateTracker;
import com.misanthropy.lightercitadel.LighterCitadelConfig;
import com.misanthropy.lightercitadel.client.HoverAnimationState;
import com.misanthropy.lightercitadel.client.PatreonIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientProxy.class, remap = false)
public abstract class ClientProxyMixin {
    @Shadow
    private ItemStack lastHoveredItem;

    /**
     * Client twin of ServerProxyMixin: runs for every entity every client tick.
     */
    @Inject(method = "canEntityTickClient", at = @At("HEAD"), cancellable = true)
    private void lightercitadel$fastEntityTickCheck(Level level, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!LighterCitadelConfig.fastEntityTickCheckClient) {
            return;
        }
        ClientTickRateTracker tracker = ClientTickRateTracker.getForClient(Minecraft.getInstance());
        if (tracker.tickRateModifierList.isEmpty() && tracker.specialTickRateEntities.isEmpty()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tickMouseOverAnimations", at = @At("HEAD"), cancellable = true)
    private void lightercitadel$tickMouseOverAnimations(CallbackInfo ci) {
        if (!LighterCitadelConfig.optimizeHoverAnimations) {
            return;
        }
        ci.cancel();
        HoverAnimationState.tick(this.lastHoveredItem);
        this.lastHoveredItem = null;
    }

    @Inject(method = "getMouseOverProgress", at = @At("HEAD"), cancellable = true)
    private void lightercitadel$getMouseOverProgress(ItemStack stack, CallbackInfoReturnable<Float> cir) {
        if (LighterCitadelConfig.optimizeHoverAnimations) {
            cir.setReturnValue(HoverAnimationState.progress(stack));
        }
    }

    /**
     * Citadel's patreon follower renderer runs for every rendered player every frame and starts by
     * building the player's name Component/String and scanning the patreon list. Answer the cheap
     * "this renders nothing" cases first and only let Citadel run for actual patreons.
     */
    @Inject(method = "playerRender", at = @At("HEAD"), cancellable = true)
    private void lightercitadel$playerRender(RenderPlayerEvent.Pre event, CallbackInfo ci) {
        if (LighterCitadelConfig.disablePatreonFollowers) {
            ci.cancel();
            return;
        }
        if (!LighterCitadelConfig.optimizePatreonFollowerRender) {
            return;
        }
        if (ClientProxy.hideFollower || event.isCanceled()) {
            ci.cancel();
            return;
        }
        Player player = event.getEntity();
        if (!player.isModelPartShown(PlayerModelPart.CAPE) || player.isSpectator()) {
            ci.cancel();
            return;
        }
        if (PatreonIndex.isEmpty() || !PatreonIndex.isPatreon(player.getGameProfile().getName())) {
            ci.cancel();
        }
    }
}
