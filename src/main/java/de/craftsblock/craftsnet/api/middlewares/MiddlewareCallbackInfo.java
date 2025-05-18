package de.craftsblock.craftsnet.api.middlewares;

/**
 * The {@link MiddlewareCallbackInfo} is used to pass information between the different
 * {@link Middleware middlewares}. It can further more also affect the further behaviour
 * of the invoker after the invocation of the {@link Middleware middlewares}.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.3.6-SNAPSHOT
 */
public class MiddlewareCallbackInfo {

    private String cancelReason;
    private boolean cancelled = false;

    /**
     * Sets the cancelled flag for the callback, indicating whether the callback is cancelled or not.
     *
     * @param cancelled {@code true} to cancel the callback, {@code false} to allow processing.
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Checks if the callback has been cancelled.
     *
     * @return {@code true} if the callback is cancelled, {@code false} otherwise.
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets a custom cancel reason which is printed to the console
     *
     * @param cancelReason The cancel reason which is printed to the console
     */
    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    /**
     * Gets the custom cancel reason which was set by one of the listeners.
     *
     * @return The cancel reason
     */
    public String getCancelReason() {
        return cancelReason;
    }

    /**
     * Checks and returns whether a custom cancel reason was set by one of the listeners.
     *
     * @return true if a custom cancel reason was set, false otherwise.
     */
    public boolean hasCancelReason() {
        return this.cancelReason != null;
    }

}
