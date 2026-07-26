package com.misanthropy.lightercitadel;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LighterCitadelMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        LighterCitadelConfig.load();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        int dot = mixinClassName.lastIndexOf('.');
        String simpleName = dot < 0 ? mixinClassName : mixinClassName.substring(dot + 1);

        return switch (simpleName) {
            case "CitadelServerDataMixin" -> LighterCitadelConfig.cacheServerDataLookup;
            case "ServerProxyMixin" -> LighterCitadelConfig.fastEntityTickCheckServer;
            case "TickRateTrackerMixin" -> LighterCitadelConfig.fastMasterTick
                    || LighterCitadelConfig.fastModifierQueries
                    || LighterCitadelConfig.purgeUnloadedEntities;
            case "ClientTickRateTrackerMixin" ->
                    LighterCitadelConfig.cacheClientTracker || LighterCitadelConfig.fastSoundPitch;
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


    private static final String LIVING_ENTITY_RENDERER = "net.minecraft.client.renderer.entity.LivingEntityRenderer";
    private static final String GATES = "com/misanthropy/lightercitadel/runtime/CitadelRendererEvents";

    private static final Map<String, String> RENDERER_EVENT_GATES = Map.of(
            "citadel_setupRotations", "wantsSetupRotations",
            "citadel_render_setupAnim_before", "wantsPreSetupAnimations",
            "citadel_render_setupAnim_after", "wantsPostSetupAnimations",
            "citadel_render_renderToBuffer", "wantsPostRenderModel");

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (!LighterCitadelConfig.skipUnlistenedRendererEvents || !LIVING_ENTITY_RENDERER.equals(targetClassName)) {
            return;
        }
        try {
            weaveRendererEventGuards(targetClass);
        } catch (Throwable t) {
            System.err.println("[LighterCitadel] Could not weave renderer event guards, leaving Citadel's "
                    + "renderer events untouched: " + t);
        }
    }

    private static void weaveRendererEventGuards(ClassNode targetClass) {
        for (MethodNode method : targetClass.methods) {
            String gate = findGate(method.name);
            if (gate == null || alreadyWoven(method)) {
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
        }
    }

    private static String findGate(String methodName) {
        for (Map.Entry<String, String> entry : RENDERER_EVENT_GATES.entrySet()) {
            String citadelName = entry.getKey();
            if (methodName.equals(citadelName) || methodName.endsWith('$' + citadelName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean alreadyWoven(MethodNode method) {
        for (int i = 0; i < method.instructions.size(); i++) {
            if (method.instructions.get(i) instanceof MethodInsnNode insn && GATES.equals(insn.owner)) {
                return true;
            }
        }
        return false;
    }
}
