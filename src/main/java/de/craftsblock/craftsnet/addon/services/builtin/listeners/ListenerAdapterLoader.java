package de.craftsblock.craftsnet.addon.services.builtin.listeners;

import de.craftsblock.craftscore.event.ListenerAdapter;
import de.craftsblock.craftscore.event.ListenerRegistry;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.services.ServiceLoader;

/**
 * A concrete implementation of the {@link ServiceLoader} interface for managing instances of {@link ListenerAdapter}.
 * This class specifically focuses on loading instances of {@link ListenerAdapter} into the {@link ListenerRegistry}.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.1
 * @since 3.1.0-SNAPSHOT
 */
public class ListenerAdapterLoader implements ServiceLoader<ListenerAdapter> {

    private final CraftsNet craftsNet;

    /**
     * Creates a new instance of {@link ListenerAdapterLoader}.
     *
     * @param craftsNet The instance of {@link CraftsNet} that the {@link ListenerAdapterLoader} was registered on.
     */
    public ListenerAdapterLoader(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
    }

    /**
     * Loads an {@link ListenerAdapter} into the {@link ListenerRegistry} for further processing.
     *
     * @param provider The instance of the {@link ListenerAdapter} to be loaded.
     * @return {@code true} if the provider is successfully loaded and registered, {@code false} otherwise.
     */
    @Override
    public boolean load(ListenerAdapter provider) {
        craftsNet.getListenerRegistry().register(provider);
        return true;
    }

}
