package com.misanthropy.lightercitadel.mixin;

import com.github.alexthe666.citadel.server.tick.TickRateTracker;
import com.github.alexthe666.citadel.server.tick.modifier.TickRateModifier;
import com.misanthropy.lightercitadel.LighterCitadelConfig;
import com.misanthropy.lightercitadel.runtime.TickListAccess;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = TickRateTracker.class, remap = false)
public abstract class TickRateTrackerMixin implements TickListAccess {

    @Shadow
    public List<TickRateModifier> tickRateModifierList;

    @Shadow
    public List<Entity> specialTickRateEntities;

    @Shadow
    private long masterTickCount;

    @Shadow
    protected abstract void tickBlockedEntity(Entity entity);

    @Shadow
    public abstract boolean hasNormalTickRate(Entity entity);

    @Shadow
    protected abstract void sync();

    @Unique
    private ReferenceOpenHashSet<Entity> lightercitadel$index;


    @Inject(method = "masterTick", at = @At("HEAD"), cancellable = true)
    private void lightercitadel$masterTick(CallbackInfo ci) {
        if (!LighterCitadelConfig.fastMasterTick) {
            return;
        }
        ci.cancel();

        final List<Entity> entities = this.specialTickRateEntities;
        final List<TickRateModifier> modifiers = this.tickRateModifierList;

        this.masterTickCount++;

        if (entities.isEmpty() && modifiers.isEmpty()) {
            if (this.lightercitadel$index != null) {
                this.lightercitadel$index = null;
            }
            return;
        }

        lightercitadel$tickBlockedEntities(entities);
        lightercitadel$sweepBlockedEntities(entities);
        lightercitadel$tickModifiers(modifiers);
    }

    @Unique
    private void lightercitadel$tickBlockedEntities(List<Entity> entities) {
        final int initialSize = entities.size();
        for (int i = 0; i < initialSize && i < entities.size(); i++) {
            tickBlockedEntity(entities.get(i));
        }
    }

    @Unique
    private void lightercitadel$sweepBlockedEntities(List<Entity> entities) {
        final boolean purge = LighterCitadelConfig.purgeUnloadedEntities;
        final int size = entities.size();
        int write = 0;

        for (int read = 0; read < size; read++) {
            Entity entity = entities.get(read);
            boolean drop = (purge && entity.isRemoved()) || hasNormalTickRate(entity);
            if (!drop) {
                if (write != read) {
                    entities.set(write, entity);
                }
                write++;
            }
        }

        if (write != size) {
            for (int i = size - 1; i >= write; i--) {
                entities.remove(i);
            }
            this.lightercitadel$index = null;
        }
    }

    @Unique
    private void lightercitadel$tickModifiers(List<TickRateModifier> modifiers) {
        final int size = modifiers.size();
        if (size == 0) {
            return;
        }
        for (int i = 0; i < size && i < modifiers.size(); i++) {
            modifiers.get(i).masterTick();
        }

        final int afterTick = modifiers.size();
        int write = 0;
        for (int read = 0; read < afterTick; read++) {
            TickRateModifier modifier = modifiers.get(read);
            if (!modifier.doRemove()) {
                if (write != read) {
                    modifiers.set(write, modifier);
                }
                write++;
            }
        }
        if (write != afterTick) {
            for (int i = afterTick - 1; i >= write; i--) {
                modifiers.remove(i);
            }
            sync();
        }
    }


    @Inject(method = "getEntityTickLengthModifier", at = @At("HEAD"), cancellable = true)
    private void lightercitadel$fastTickLengthModifier(Entity entity, CallbackInfoReturnable<Float> cir) {
        if (LighterCitadelConfig.fastModifierQueries && this.tickRateModifierList.isEmpty()) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "getDayTimeIncrement", at = @At("HEAD"), cancellable = true)
    private void lightercitadel$fastDayTimeIncrement(long timeIn, CallbackInfoReturnable<Long> cir) {
        if (LighterCitadelConfig.fastModifierQueries && this.tickRateModifierList.isEmpty()) {
            cir.setReturnValue(timeIn);
        }
    }

    @Inject(method = "isTickingHandled", at = @At("HEAD"), cancellable = true)
    private void lightercitadel$fastIsTickingHandled(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        final List<Entity> entities = this.specialTickRateEntities;
        final int size = entities.size();
        if (size == 0) {
            cir.setReturnValue(false);
            return;
        }
        if (size < LighterCitadelConfig.tickListIndexThreshold) {
            return;
        }
        ReferenceOpenHashSet<Entity> index = this.lightercitadel$index;
        if (index == null || index.size() != size) {
            index = new ReferenceOpenHashSet<>(entities);
            this.lightercitadel$index = index;
        }
        cir.setReturnValue(index.contains(entity));
    }

    @Inject(method = "addTickBlockedEntity", at = @At("TAIL"))
    private void lightercitadel$onAddBlockedEntity(Entity entity, CallbackInfo ci) {
        ReferenceOpenHashSet<Entity> index = this.lightercitadel$index;
        if (index != null) {
            index.add(entity);
        }
    }


    @Override
    public void lightercitadel$purgeEntity(Entity entity) {
        final List<Entity> entities = this.specialTickRateEntities;
        if (entities.isEmpty()) {
            return;
        }
        if (entities.remove(entity)) {
            this.lightercitadel$index = null;
        }
    }

    @Override
    public void lightercitadel$purgeLevel(Level level) {
        final List<Entity> entities = this.specialTickRateEntities;
        if (entities.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (int i = entities.size() - 1; i >= 0; i--) {
            Entity entity = entities.get(i);
            if (entity == null || entity.level() == level) {
                entities.remove(i);
                changed = true;
            }
        }
        if (changed) {
            this.lightercitadel$index = null;
        }
    }

    @Override
    public void lightercitadel$purgeAll() {
        if (!this.specialTickRateEntities.isEmpty()) {
            this.specialTickRateEntities.clear();
        }
        this.lightercitadel$index = null;
    }
}
