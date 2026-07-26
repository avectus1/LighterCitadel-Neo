package com.misanthropy.lightercitadel.runtime;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.ListenerList;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventListenerHelper;

import java.lang.reflect.Field;

public final class EventListenerProbe {

    private static final int BUS_ID;
    private static final boolean AVAILABLE;

    static {
        int busId = -1;
        boolean available = false;
        try {
            Field field = MinecraftForge.EVENT_BUS.getClass().getDeclaredField("busID");
            field.setAccessible(true);
            busId = field.getInt(MinecraftForge.EVENT_BUS);
            available = busId >= 0;
        } catch (Throwable ignored) {
        }
        BUS_ID = busId;
        AVAILABLE = available;
    }

    private final ListenerList list;

    private EventListenerProbe(ListenerList list) {
        this.list = list;
    }

    public static EventListenerProbe of(Class<? extends Event> eventClass) {
        if (!AVAILABLE) {
            return new EventListenerProbe(null);
        }
        try {
            return new EventListenerProbe(EventListenerHelper.getListenerList(eventClass));
        } catch (Throwable ignored) {
            return new EventListenerProbe(null);
        }
    }

    public boolean hasListeners() {
        if (list == null) {
            return true;
        }
        try {
            return list.getListeners(BUS_ID).length > 0;
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }
}
