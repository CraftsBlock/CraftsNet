package de.craftsblock.craftsnet.autoregister.builtin.addons;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.services.ServiceLoader;
import de.craftsblock.craftsnet.addon.services.ServiceManager;
import de.craftsblock.craftsnet.autoregister.AutoRegisterHandler;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegisterInfo;

/**
 * A handler for automatically registering {@link ServiceLoader} implementations. This class extends
 * {@link AutoRegisterHandler} and provides a concrete implementation for handling the registration of
 * {@link ServiceLoader} instances into the service manager registry of {@link CraftsNet}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.4
 * @since 3.2.0-SNAPSHOT
 */
public class ServiceLoaderAutoRegisterHandler extends AutoRegisterHandler<ServiceLoader<?>> {

    private final ServiceManager serviceManager;

    /**
     * Constructs a new {@link ServiceLoaderAutoRegisterHandler}.
     *
     * @param craftsNet The {@link CraftsNet} instance used for managing the service manager registry.
     */
    public ServiceLoaderAutoRegisterHandler(CraftsNet craftsNet) {
        super(craftsNet);
        this.serviceManager = craftsNet.getServiceManager();
    }

    /**
     * Handles the registration of the provided {@link ServiceLoader}.
     *
     * <p>This method attempts to register the given {@link ServiceLoader} with the {@link CraftsNet#getServiceManager()}
     * of the associated {@link CraftsNet} instance. If registration is successful, the method
     * returns {@code true}.</p>
     *
     * @param serviceLoader The {@link ServiceLoader} to be registered.
     * @param args          Additional arguments (not used in this implementation but provided for extensibility).
     * @return {@code true} if the registration was successful, {@code false} otherwise.
     */
    @Override
    protected boolean handle(ServiceLoader<?> serviceLoader, AutoRegisterInfo info, Object... args) {
        if (serviceManager.isRegistered(serviceLoader)) return false;

        serviceManager.register(serviceLoader);
        return true;
    }

}
