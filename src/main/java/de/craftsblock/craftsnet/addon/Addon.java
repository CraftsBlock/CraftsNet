package de.craftsblock.craftsnet.addon;

import de.craftsblock.craftscore.event.ListenerRegistry;
import de.craftsblock.craftsnet.api.RouteRegistry;
import de.craftsblock.craftsnet.utils.Logger;

import java.io.File;

public abstract class Addon {

    private String name;
    private RouteRegistry handler;
    private ListenerRegistry registry;
    private Logger logger;

    public void onLoad() {
    }

    public abstract void onEnable();

    public abstract void onDisable();

    public String getName() {
        return name;
    }

    public final RouteRegistry routeRegistry() {
        return handler;
    }

    public final ListenerRegistry listenerRegistry() {
        return registry;
    }

    public Logger logger() {
        return logger;
    }

    public final File getDataFolder() {
        File folder = new File("./addons/" + getName() + "/");
        if (!folder.exists())
            folder.mkdirs();
        else if (folder.exists() && !folder.isDirectory()) {
            folder.delete();
            folder = getDataFolder();
        }
        return folder;
    }

}
