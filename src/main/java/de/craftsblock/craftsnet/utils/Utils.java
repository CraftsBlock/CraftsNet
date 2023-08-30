package de.craftsblock.craftsnet.utils;

import org.jetbrains.annotations.Nullable;

public class Utils {

    @Nullable
    public static Thread getThreadByName(String name) {
        Thread[] threads = Thread.getAllStackTraces().keySet().toArray(new Thread[0]);
        for (Thread t : threads) if (t.getName().equals(name)) return t;
        return null;
    }

}
