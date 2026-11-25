package com.misanthropy.lightercitadel.runtime;

import net.neoforged.bus.ListenerList;
import net.neoforged.bus.LockHelper;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Field;

/**
 * Answers "does anything listen to this event on NeoForge.EVENT_BUS?" without posting it.
 * <p>
 * NeoForge's EventBus keeps one {@link ListenerList} per event class (which also covers listeners
 * registered for parent event types) in a private {@link LockHelper}. The bus module doesn't open its
 * packages, so plain reflection can't reach that field; it is read once through sun.misc.Unsafe
 * instead. If anything about that fails, the probe reports "has listeners" and Citadel's events are
 * posted as normal.
 */
public final class EventListenerProbe {
    private static final LockHelper<Class<?>, ListenerList> LISTENER_LISTS = findListenerLists();
    private static final boolean AVAILABLE = LISTENER_LISTS != null;

    private final Class<? extends Event> eventClass;
    private volatile ListenerList list;

    private EventListenerProbe(Class<? extends Event> eventClass) {
        this.eventClass = eventClass;
    }

    @SuppressWarnings("unchecked")
    private static LockHelper<Class<?>, ListenerList> findListenerLists() {
        try {
            IEventBus bus = NeoForge.EVENT_BUS;
            Field field = bus.getClass().getDeclaredField("listenerLists");
            if (!LockHelper.class.isAssignableFrom(field.getType())) {
                return null;
            }
            Field theUnsafe = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
            Object value = unsafe.getObject(bus, unsafe.objectFieldOffset(field));
            return value instanceof LockHelper<?, ?> helper ? (LockHelper<Class<?>, ListenerList>) helper : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static EventListenerProbe of(Class<? extends Event> eventClass) {
        return new EventListenerProbe(eventClass);
    }

    public boolean hasListeners() {
        if (!AVAILABLE) {
            return true;
        }
        try {
            ListenerList list = this.list;
            if (list == null) {
                list = LISTENER_LISTS.get(this.eventClass);
                if (list == null) {
                    // The bus builds this list the first time the event is posted or listened to.
                    // Until then, post normally so it gets created with the right parent links.
                    return true;
                }
                this.list = list;
            }
            return list.getListeners().length > 0;
        } catch (Throwable t) {
            return true;
        }
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }
}
