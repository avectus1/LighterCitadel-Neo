//package com.misanthropy.lightercitadel.debug;
//
//import com.github.alexthe666.citadel.Citadel;
//import com.github.alexthe666.citadel.server.tick.ServerTickRateTracker;
//import com.github.alexthe666.citadel.server.tick.modifier.CelestialTickRateModifier;
//import com.github.alexthe666.citadel.server.tick.modifier.GlobalTickRateModifier;
//import com.github.alexthe666.citadel.server.tick.modifier.LocalPositionTickRateModifier;
//import com.github.alexthe666.citadel.server.tick.modifier.TickRateModifier;
//import com.misanthropy.lightercitadel.LighterCitadel;
//import com.misanthropy.lightercitadel.LighterCitadelConfig;
//import com.misanthropy.lightercitadel.client.HoverAnimationState;
//import com.misanthropy.lightercitadel.runtime.CitadelDataCache;
//import com.misanthropy.lightercitadel.runtime.TickListAccess;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.Items;
//import net.minecraft.world.level.Level;
//import net.minecraftforge.event.server.ServerStartedEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//
//public final class SelfTest {
//
//    public static final String PROPERTY = "lightercitadel.selftest";
//
//    public static boolean enabled() {
//        return Boolean.getBoolean(PROPERTY);
//    }
//
//    private final List<String> failures = new ArrayList<>();
//    private int checks;
//
//    @SubscribeEvent
//    public void onServerStarted(ServerStartedEvent event) {
//        ServerLevel level = event.getServer().overworld();
//        ServerTickRateTracker tracker = CitadelDataCache.trackerFor(event.getServer());
//
//        LighterCitadel.LOGGER.info("---LighterCitadel Self-test---");
//        try {
//            testEmptyStateFastPaths(level, tracker);
//            testCelestialDayTime(tracker);
//            testLocalModifierEngagesSlowPath(level, tracker);
//            testTickingHandledIndex(level, tracker);
//            testModifierExpiry(tracker);
//            testRemovedEntityIsDropped(level, tracker);
//            testHoverAnimationEquivalence();
//        } catch (Throwable t) {
//            failures.add("harness threw: " + t);
//            LighterCitadel.LOGGER.error("Self-test harness threw", t);
//        } finally {
//            reset(tracker);
//        }
//
//        if (failures.isEmpty()) {
//            LighterCitadel.LOGGER.info("=== LighterCitadel self-test: {} checks PASSED ===", checks);
//        } else {
//            LighterCitadel.LOGGER.error("=== LighterCitadel self-test: {} of {} checks FAILED ===",
//                    failures.size(), checks);
//            failures.forEach(f -> LighterCitadel.LOGGER.error("  FAIL: {}", f));
//        }
//    }
//
//
//    private void testEmptyStateFastPaths(ServerLevel level, ServerTickRateTracker tracker) {
//        reset(tracker);
//        Entity pig = spawn(level, 0, 0);
//        try {
//            check("empty: tick length modifier is 1.0",
//                    tracker.getEntityTickLengthModifier(pig) == 1.0F);
//            check("empty: entity has normal tick rate", tracker.hasNormalTickRate(pig));
//            check("empty: ticking is not handled", !tracker.isTickingHandled(pig));
//            check("empty: day time increment is unchanged", tracker.getDayTimeIncrement(1L) == 1L);
//            check("empty: server tick length is vanilla", tracker.getServerTickLengthMs() == 50);
//            check("empty: entity is allowed to tick", Citadel.PROXY.canEntityTickServer(level, pig));
//            check("empty: fast path did not enlist the entity", tracker.specialTickRateEntities.isEmpty());
//        } finally {
//            pig.discard();
//        }
//    }
//
//    private void testCelestialDayTime(ServerTickRateTracker tracker) {
//        reset(tracker);
//        tracker.tickRateModifierList.add(new CelestialTickRateModifier(1_000_000, 2.0F));
//        check("celestial x2: day time increment doubles", tracker.getDayTimeIncrement(1L) == 2L);
//
//        reset(tracker);
//        tracker.tickRateModifierList.add(new CelestialTickRateModifier(1_000_000, 0.5F));
//        long advanced = 0;
//        for (int i = 0; i < 8; i++) {
//            advanced += tracker.getDayTimeIncrement(1L);
//            tracker.masterTick();
//        }
//        check("celestial x0.5: day time advances on half the ticks (got " + advanced + ")", advanced == 4L);
//        reset(tracker);
//    }
//
//    private void testLocalModifierEngagesSlowPath(ServerLevel level, ServerTickRateTracker tracker) {
//        reset(tracker);
//        Entity pig = spawn(level, 0, 0);
//        try {
//            tracker.tickRateModifierList.add(new LocalPositionTickRateModifier(
//                    pig.position(), 16.0D, Level.OVERWORLD, 1_000_000, 2.0F));
//
//            check("local x2: tick length modifier is 2.0",
//                    tracker.getEntityTickLengthModifier(pig) == 2.0F);
//            check("local x2: entity no longer has a normal tick rate", !tracker.hasNormalTickRate(pig));
//            check("local x2: entity is refused a normal tick",
//                    !Citadel.PROXY.canEntityTickServer(level, pig));
//            check("local x2: entity was enlisted for custom ticking",
//                    tracker.specialTickRateEntities.contains(pig));
//            check("local x2: isTickingHandled agrees", tracker.isTickingHandled(pig));
//            check("local x2: enlisted entity is refused again",
//                    !Citadel.PROXY.canEntityTickServer(level, pig));
//            check("local x2: it was not enlisted twice", tracker.specialTickRateEntities.size() == 1);
//
//            Entity distant = spawn(level, 400, 400);
//            try {
//                check("local x2: distant entity is unaffected",
//                        tracker.getEntityTickLengthModifier(distant) == 1.0F);
//                check("local x2: distant entity ticks normally",
//                        Citadel.PROXY.canEntityTickServer(level, distant));
//            } finally {
//                distant.discard();
//            }
//        } finally {
//            pig.discard();
//            reset(tracker);
//        }
//    }
//
//    private void testTickingHandledIndex(ServerLevel level, ServerTickRateTracker tracker) {
//        reset(tracker);
//        int total = LighterCitadelConfig.tickListIndexThreshold * 2 + 5;
//        List<Entity> members = new ArrayList<>();
//        List<Entity> strangers = new ArrayList<>();
//        try {
//            for (int i = 0; i < total; i++) {
//                Entity e = spawn(level, i, 0);
//                members.add(e);
//                tracker.specialTickRateEntities.add(e);
//
//                for (Entity member : members) {
//                    check("index: enlisted entity reported as handled at size " + members.size(),
//                            tracker.isTickingHandled(member));
//                }
//            }
//            for (int i = 0; i < 5; i++) {
//                Entity e = spawn(level, 1000 + i, 0);
//                strangers.add(e);
//                check("index: non-enlisted entity reported as unhandled", !tracker.isTickingHandled(e));
//            }
//
//            Entity dropped = members.get(members.size() / 2);
//            ((TickListAccess) tracker).lightercitadel$purgeEntity(dropped);
//            check("index: purged entity is no longer handled", !tracker.isTickingHandled(dropped));
//            check("index: remaining entities still handled", tracker.isTickingHandled(members.get(0)));
//        } finally {
//            members.forEach(Entity::discard);
//            strangers.forEach(Entity::discard);
//            reset(tracker);
//        }
//    }
//
//    private void testModifierExpiry(ServerTickRateTracker tracker) {
//        reset(tracker);
//        TickRateModifier shortLived = new GlobalTickRateModifier(4, 1.0F);
//        TickRateModifier longLived = new GlobalTickRateModifier(1_000_000, 1.0F);
//        tracker.tickRateModifierList.add(shortLived);
//        tracker.tickRateModifierList.add(longLived);
//
//        for (int i = 0; i < 3; i++) {
//            tracker.masterTick();
//        }
//        check("expired: modifier still alive before its duration elapses",
//                tracker.tickRateModifierList.contains(shortLived));
//
//        tracker.masterTick();
//        check("expired: modifier removed once duration elapsed",
//                !tracker.tickRateModifierList.contains(shortLived));
//        check("expired: unrelated modifier survived", tracker.tickRateModifierList.contains(longLived));
//        check("expired: exactly one modifier remains", tracker.tickRateModifierList.size() == 1);
//        reset(tracker);
//    }
//
//    private void testRemovedEntityIsDropped(ServerLevel level, ServerTickRateTracker tracker) {
//        reset(tracker);
//        Entity pig = spawn(level, 0, 0);
//        tracker.tickRateModifierList.add(new LocalPositionTickRateModifier(
//                pig.position(), 16.0D, Level.OVERWORLD, 1_000_000, 2.0F));
//        tracker.specialTickRateEntities.add(pig);
//
//        pig.discard();
//        check("leaks: entity reports removed after discard", pig.isRemoved());
//
//        tracker.masterTick();
//        check("leaks: removed entity dropped from the custom tick list",
//                !tracker.specialTickRateEntities.contains(pig));
//
//        Entity other = spawn(level, 0, 0);
//        tracker.specialTickRateEntities.add(other);
//        ((TickListAccess) tracker).lightercitadel$purgeLevel(level);
//        check("leaks: level purge emptied the custom tick list", tracker.specialTickRateEntities.isEmpty());
//        other.discard();
//        reset(tracker);
//    }
//
//    private void testHoverAnimationEquivalence() {
//        HoverAnimationState.clearAll();
//
//        ItemStack[] stacks = {
//                new ItemStack(Items.DIAMOND), new ItemStack(Items.STICK), new ItemStack(Items.APPLE)
//        };
//        ReferenceHoverAnimations reference = new ReferenceHoverAnimations();
//        Random random = new Random(20240726L);
//        boolean matched = true;
//
//        for (int tick = 0; tick < 500 && matched; tick++) {
//            int roll = random.nextInt(stacks.length + 1);
//            ItemStack hovered = roll == stacks.length ? null : stacks[roll];
//
//            HoverAnimationState.tick(hovered);
//            reference.tick(hovered);
//
//            for (ItemStack stack : stacks) {
//                if (HoverAnimationState.currentOf(stack) != reference.current(stack)
//                        || HoverAnimationState.previousOf(stack) != reference.previous(stack)) {
//                    matched = false;
//                    failures.add("hover: diverged from Citadel's algorithm at tick " + tick
//                            + " (ours current=" + HoverAnimationState.currentOf(stack)
//                            + " prev=" + HoverAnimationState.previousOf(stack)
//                            + ", citadel current=" + reference.current(stack)
//                            + " prev=" + reference.previous(stack) + ")");
//                    checks++;
//                    break;
//                }
//            }
//        }
//        check("hover: 500 ticks match Citadel's algorithm exactly", matched);
//        check("hover: no stale entries retained after decay", HoverAnimationState.size() <= stacks.length);
//
//        HoverAnimationState.clearAll();
//    }
//
//    private static final class ReferenceHoverAnimations {
//        private final Map<ItemStack, Float> prev = new HashMap<>();
//        private final Map<ItemStack, Float> current = new HashMap<>();
//
//        void tick(ItemStack lastHoveredItem) {
//            prev.putAll(current);
//            if (lastHoveredItem != null) {
//                float p = current.getOrDefault(lastHoveredItem, 0F);
//                float maxTime = 5F;
//                if (p < maxTime) {
//                    current.put(lastHoveredItem, p + 1);
//                }
//            }
//            if (!current.isEmpty()) {
//                Iterator<Map.Entry<ItemStack, Float>> it = current.entrySet().iterator();
//                while (it.hasNext()) {
//                    Map.Entry<ItemStack, Float> next = it.next();
//                    float progress = next.getValue();
//                    if (lastHoveredItem == null || next.getKey() != lastHoveredItem) {
//                        if (progress == 0) {
//                            it.remove();
//                        } else {
//                            next.setValue(progress - 1);
//                        }
//                    }
//                }
//            }
//        }
//
//        float current(ItemStack stack) {
//            return current.getOrDefault(stack, 0F);
//        }
//
//        float previous(ItemStack stack) {
//            return prev.getOrDefault(stack, 0F);
//        }
//    }
//
//
//    private Entity spawn(ServerLevel level, double x, double z) {
//        Entity entity = EntityType.PIG.create(level);
//        assert entity != null;
//        entity.setPos(x, 100, z);
//        level.addFreshEntity(entity);
//        return entity;
//    }
//
//    private void reset(ServerTickRateTracker tracker) {
//        tracker.tickRateModifierList.clear();
//        ((TickListAccess) tracker).lightercitadel$purgeAll();
//    }
//
//    private void check(String description, boolean condition) {
//        checks++;
//        if (!condition) {
//            failures.add(description);
//        }
//    }
//}
