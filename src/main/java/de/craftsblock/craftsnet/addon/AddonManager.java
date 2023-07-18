package de.craftsblock.craftsnet.addon;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class AddonManager {

    private final AddonLoader addonLoader = new AddonLoader();
    private final ConcurrentHashMap<String, Addon> addons = new ConcurrentHashMap<>();

    public AddonManager() throws IOException, ClassNotFoundException {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        File folder = new File("./addons/");
        if (!folder.isDirectory()) {
            folder.delete();
            folder.mkdirs();
        }
        for (File file : Objects.requireNonNull(folder.listFiles()))
            addonLoader.add(file);
        addonLoader.load();
    }

    public void stop() {

    }

    public void register(Addon addon) {
        addons.put(addon.getName(), addon);
    }

    public void unregister(Addon addon) {
        addons.remove(addon.getName());
    }

    public ConcurrentHashMap<String, Addon> getAddons() {
        return (ConcurrentHashMap<String, Addon>) Map.copyOf(addons);
    }

}
