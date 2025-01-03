package de.craftsblock.craftsnet.autoregister;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.autoregister.meta.AutoRegisterInfo;

/**
 * The {@link AutoRegisterHandler} class is an abstract base class designed to handle objects
 * during the auto registration process. Each handler is responsible for processing a specific type of object
 * in the auto register system.
 *
 * <p>Handlers extend this class and implement the {@link #handle(Object, AutoRegisterInfo, Object...)} method to define how
 * the auto register process should interact with the specific type {@link T}. The handler also has access to
 * the {@link CraftsNet} instance, which can be used to interact with the main application context.</p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.1.0
 * @since 3.2.0-SNAPSHOT
 */
public abstract class AutoRegisterHandler<T> {

    protected final CraftsNet craftsNet;

    /**
     * Constructs an {@link AutoRegisterHandler} with the specified {@link CraftsNet} instance.
     *
     * @param craftsNet The main {@link CraftsNet} instance, which provides access to the application's context.
     */
    public AutoRegisterHandler(CraftsNet craftsNet) {
        this.craftsNet = craftsNet;
    }

    /**
     * Handles the provided object {@link T} during the auto registration process. This method must be implemented
     * by subclasses to define the specific logic for handling objects of type {@link  T}.
     *
     * @param t    The object to handle.
     * @param args Additional arguments that may be passed during the registration process.
     * @return {@code true} if the object was handled successfully, {@code false} otherwise.
     */
    protected abstract boolean handle(T t, AutoRegisterInfo info, Object... args);

    /**
     * Gets the {@link CraftsNet} instance associated with this handler.
     *
     * @return The {@link CraftsNet} instance.
     */
    public CraftsNet getCraftsNet() {
        return craftsNet;
    }

}
