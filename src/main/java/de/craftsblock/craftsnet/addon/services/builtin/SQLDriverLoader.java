package de.craftsblock.craftsnet.addon.services.builtin;

import de.craftsblock.craftsnet.addon.services.ServiceLoader;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

/**
 * A concrete implementation of the {@link ServiceLoader} interface for managing instances of {@link Driver}.
 * This class specifically focuses on loading instances of {@link Driver} into the {@link DriverManager}.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since 3.0.0-SNAPSHOT
 */
public class SQLDriverLoader implements ServiceLoader<Driver> {

    /**
     * Loads an {@link Driver} into the {@link DriverManager} for further processing.
     *
     * <p>This method registers the provided {@link Driver} instance with the {@link DriverManager}.</p>
     *
     * @param provider The instance of the {@link Driver} to be loaded.
     * @return true if the provider is successfully loaded and registered, false otherwise.
     */
    @Override
    public boolean load(Driver provider) {
        try {
            // Check if the driver is already registered
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                if (driver.getClass().equals(provider.getClass()))
                    return false;
            }

            DriverManager.registerDriver(provider);
        } catch (SQLException e) {
            throw new RuntimeException("Could not register sql driver %s!".formatted(
                    provider.getClass().getName()
            ), e);
        }

        return true;
    }

}
