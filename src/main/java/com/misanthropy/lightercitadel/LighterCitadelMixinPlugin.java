package com.misanthropy.lightercitadel;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LighterCitadelMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("lightercitadel");
    private static final String LIVING_ENTITY_RENDERER = "net.minecraft.client.renderer.entity.LivingEntityRenderer";
    private static final String GATES = "com/misanthropy/lightercitadel/runtime/CitadelRendererEvents";

    /**
     * Citadel's injector handlers in LivingEntityRenderer, mapped to the gate that tells whether anyone
     * listens to the event that handler posts. Mixin renames merged handlers to
     * {@code handler$xxx$citadel$...}, so names are matched by suffix as well.
     */
    private static final Map<String, String> RENDERER_EVENT_GATES = Map.of(
            "citadel_setupRotations", "wantsSetupRotations",
            "citadel_render_setupAnim_before", "wantsPreSetupAnimations",
            "citadel_render_setupAnim_after", "wantsPostSetupAnimations",
            "citadel_render_renderToBuffer", "wantsPostRenderModel"
    );

    private static int wovenHandlers;

    @Override
    public void onLoad(String mixinPackage) {
        LighterCitadelConfig.load();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String simple = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        return switch (simple) {
            case "CitadelServerDataMixin" -> LighterCitadelConfig.cacheServerDataLookup;
            case "ServerProxyMixin" -> LighterCitadelConfig.fastEntityTickCheckServer;
            case "TickRateTrackerMixin" -> LighterCitadelConfig.fastMasterTick
                    || LighterCitadelConfig.fastModifierQueries
                    || LighterCitadelConfig.purgeUnloadedEntities;
            case "ClientTickRateTrackerMixin" -> LighterCitadelConfig.cacheClientTracker
                    || LighterCitadelConfig.fastSoundPitch;
            case "ClientProxyMixin" -> LighterCitadelConfig.fastEntityTickCheckClient
                    || LighterCitadelConfig.optimizeHoverAnimations
                    || LighterCitadelConfig.optimizePatreonFollowerRender
                    || LighterCitadelConfig.disablePatreonFollowers;
            case "LivingEntityRendererEventsMixin" -> LighterCitadelConfig.skipUnlistenedRendererEvents;
            default -> true;
        };
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (!LighterCitadelConfig.skipUnlistenedRendererEvents || !LIVING_ENTITY_RENDERER.equals(targetClassName)) {
            return;
        }
        try {
            weaveRendererEventGuards(targetClass);
            if (wovenHandlers == 0) {
                LOGGER.warn("skip_unlistened_renderer_events: none of Citadel's renderer handlers were found in LivingEntityRenderer, nothing was changed.");
            } else {
                LOGGER.info("skip_unlistened_renderer_events: guarded {} of Citadel's renderer event handlers", wovenHandlers);
            }
        } catch (Throwable t) {
            LOGGER.error("Could not weave renderer event guards, Citadel's renderer events will be posted as normal", t);
        }
    }

    /**
     * Prepends {@code if (!CitadelRendererEvents.wantsX()) return;} to each of Citadel's handlers.
     * Our marker mixin has a higher priority than Citadel's, so Citadel's handlers are already merged here.
     */
    private static void weaveRendererEventGuards(ClassNode node) {
        for (MethodNode method : node.methods) {
            String gate = findGate(method.name);
            if (gate == null || alreadyWoven(method)) {
                continue;
            }
            if (Type.getReturnType(method.desc) != Type.VOID_TYPE) {
                continue;
            }
            LabelNode proceed = new LabelNode();
            InsnList guard = new InsnList();
            guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, GATES, gate, "()Z", false));
            guard.add(new JumpInsnNode(Opcodes.IFNE, proceed));
            guard.add(new InsnNode(Opcodes.RETURN));
            guard.add(proceed);
            guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
            method.instructions.insert(guard);
            method.maxStack = Math.max(method.maxStack, 1);
            wovenHandlers++;
        }
    }

    /**
     * Merged handler names look like {@code handler$zbi000$citadel$setupRotations}: Mixin adds a
     * prefix and treats Citadel's "citadel_" method prefix as a mod id. Compare with '$' turned
     * into '_' so every spelling matches.
     */
    private static String findGate(String methodName) {
        String normalized = methodName.replace('$', '_');
        for (Map.Entry<String, String> entry : RENDERER_EVENT_GATES.entrySet()) {
            String key = entry.getKey();
            if (normalized.equals(key) || normalized.endsWith("_" + key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean alreadyWoven(MethodNode method) {
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof MethodInsnNode call && GATES.equals(call.owner)) {
                return true;
            }
        }
        return false;
    }

    public static int wovenHandlers() {
        return wovenHandlers;
    }
}
