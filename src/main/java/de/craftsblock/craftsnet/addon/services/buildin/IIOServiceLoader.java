package de.craftsblock.craftsnet.addon.services.buildin;

import de.craftsblock.craftsnet.addon.services.ServiceLoader;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.IIOServiceProvider;

/**
 * A concrete implementation of the {@link ServiceLoader} interface for managing instances of {@link IIOServiceProvider}.
 * This class specifically focuses on loading instances of {@link IIOServiceProvider} into the default
 * {@link IIORegistry}.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.0
 */
public class IIOServiceLoader implements ServiceLoader<IIOServiceProvider> {

    /**
     * Loads an {@link IIOServiceProvider} into the default {@link IIORegistry} for further processing.
     *
     * <p>This method registers the provided {@link IIOServiceProvider} instance with the default {@link IIORegistry}.
     * The registration allows subsequent retrieval and use of the service provider through the I/O registry.</p>
     *
     * @param provider The instance of the {@link IIOServiceProvider} to be loaded.
     * @return true if the provider is successfully loaded and registered, false otherwise.
     */
    @Override
    public boolean load(IIOServiceProvider provider) {
        IIORegistry.getDefaultInstance().registerServiceProvider(provider);
        return true;
    }

}
