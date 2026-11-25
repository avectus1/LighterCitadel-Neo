package com.misanthropy.lightercitadel.client;

import com.github.alexthe666.citadel.item.ItemWithHoverAnimation;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2FloatMap;
import it.unimi.dsi.fastutil.objects.Reference2FloatOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/**
 * Replacement for Citadel's tooltip hover animation maps (ClientProxy#prevMouseOverProgresses and
 * #mouseOverProgresses).
 * <ul>
 *     <li>primitive float values instead of boxed Floats,</li>
 *     <li>the "previous" map is cleared every tick instead of growing forever,</li>
 *     <li>interpolates with the partial tick. Citadel 2.7 for 1.21.1 multiplies by the last frame's
 *     duration in nanoseconds instead, which makes the animation jump straight to its ends.</li>
 * </ul>
 * Keys are compared by identity, exactly like Citadel's HashMap (ItemStack has no equals/hashCode).
 */
public final class HoverAnimationState {
    private static final float DEFAULT_MAX_TIME = 5.0F;

    private static final Reference2FloatOpenHashMap<ItemStack> PREVIOUS = newMap();
    private static final Reference2FloatOpenHashMap<ItemStack> CURRENT = newMap();

    private HoverAnimationState() {
    }

    private static Reference2FloatOpenHashMap<ItemStack> newMap() {
        Reference2FloatOpenHashMap<ItemStack> map = new Reference2FloatOpenHashMap<>(8);
        map.defaultReturnValue(0.0F);
        return map;
    }

    /**
     * Same steps as ClientProxy#tickMouseOverAnimations.
     */
    public static void tick(ItemStack hovered) {
        if (CURRENT.isEmpty() && hovered == null) {
            if (!PREVIOUS.isEmpty()) {
                PREVIOUS.clear();
            }
            return;
        }

        PREVIOUS.clear();
        PREVIOUS.putAll(CURRENT);

        if (hovered != null) {
            float current = CURRENT.getFloat(hovered);
            if (current < maxHoverTime(hovered)) {
                CURRENT.put(hovered, current + 1.0F);
            }
        }

        if (!CURRENT.isEmpty()) {
            ObjectIterator<Reference2FloatMap.Entry<ItemStack>> it = CURRENT.reference2FloatEntrySet().fastIterator();
            while (it.hasNext()) {
                Reference2FloatMap.Entry<ItemStack> entry = it.next();
                if (hovered != null && entry.getKey() == hovered) {
                    continue;
                }
                float value = entry.getFloatValue();
                if (value == 0.0F) {
                    it.remove();
                } else {
                    entry.setValue(value - 1.0F);
                }
            }
        }
    }

    public static float progress(ItemStack stack) {
        float previous = PREVIOUS.getFloat(stack);
        float current = CURRENT.getFloat(stack);
        if (previous == 0.0F && current == 0.0F) {
            return 0.0F;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        return (previous + (current - previous) * partialTick) / maxHoverTime(stack);
    }

    private static float maxHoverTime(ItemStack stack) {
        return stack.getItem() instanceof ItemWithHoverAnimation hover ? hover.getMaxHoverOverTime(stack) : DEFAULT_MAX_TIME;
    }

    public static int size() {
        return CURRENT.size();
    }

    public static void clearAll() {
        PREVIOUS.clear();
        CURRENT.clear();
    }
}
