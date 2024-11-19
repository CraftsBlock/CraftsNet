package de.craftsblock.craftsnet.events;

import de.craftsblock.craftscore.event.CancellableEvent;

/**
 * An abstract class extending {@link CancellableEvent}, providing the ability to set and retrieve
 * a custom cancellation reason. It can be used by listeners to communicate why an event was canceled.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see CancellableEvent
 * @since 3.0.7-SNAPSHOT
 */
public abstract class EventWithCancelReason extends CancellableEvent {

    private String cancelReason;

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
