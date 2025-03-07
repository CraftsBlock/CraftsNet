package de.craftsblock.craftsnet.api.session;

import de.craftsblock.craftsnet.api.session.drivers.SessionDriver;
import de.craftsblock.craftsnet.api.session.drivers.builtin.FileSessionDriver;
import de.craftsblock.craftsnet.utils.ByteBuffer;

import java.io.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles the persistence of session data by providing functionality for loading, saving,
 * and destroying session files. The session data is serialized to a file and deserialized
 * back into memory, allowing for session state preservation between application runs.
 *
 * <p>The {@link  SessionFile} class ensures data integrity through file locking mechanisms
 * and supports thread-safe operations by preventing concurrent modifications.</p>
 *
 * <p>Session files are stored in a predefined directory, and their filenames are constructed
 * using the session identifier.</p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 3.1.0
 * @see Session
 * @see ByteBuffer
 * @since 3.3.0-SNAPSHOT
 */
public class SessionFile {

    private final SessionDriver driver;
    private final Session session;
    private final ConcurrentLinkedQueue<JobType> actionQueue = new ConcurrentLinkedQueue<>();

    private boolean busy = false;
    private boolean handlingActionQueue = false;

    /**
     * Constructs a new {@link SessionFile} instance for managing the specified session.
     *
     * @param session the session associated with this file handler.
     */
    public SessionFile(Session session) {
        this.session = session;
        this.driver = new FileSessionDriver();
    }

    /**
     * Loads session data from the corresponding session file, if it exists.
     * The data is deserialized and added to the session.
     *
     * <p>File locking ensures that no other thread or process can access the file
     * during the load operation.</p>
     */
    public void load() {
        if (!this.session.isSessionStarted()) return;
        if (availableOrQueue(JobType.LOAD)) return;

        busy = true;
        try {
            this.driver.load(this.session, this.session.getSessionInfo().getSessionID());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            completeJob();
        }
    }

    /**
     * Saves the current session data to the corresponding session file.
     * The data is serialized to ensure persistence across application runs.
     *
     * <p>File locking is used to prevent concurrent access during the save operation.</p>
     */
    public void save() {
        if (!this.session.isSessionStarted()) return;
        if (availableOrQueue(JobType.SAVE)) return;

        busy = true;
        try {
            this.driver.save(this.session, this.session.getSessionInfo().getSessionID());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            completeJob();
        }
    }

    /**
     * Deletes the session file associated with the current session.
     * Ensures that the file is removed if it exists.
     */
    protected void destroy() {
        if (!this.session.isSessionStarted()) return;
        if (availableOrQueue(JobType.DESTROY)) return;

        busy = true;
        try {
            this.driver.destroy(this.session, this.session.getSessionInfo().getSessionID());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            completeJob();
        }
    }

    /**
     * Completes all jobs in the queue and marks the session file as not busy.
     * Ensures thread safety and prevents concurrent modification issues.
     */
    private void completeJob() {
        if (handlingActionQueue) return;

        try {
            if (actionQueue.isEmpty()) return;

            handlingActionQueue = true;
            actionQueue.forEach(action -> {
                busy = false;
                switch (action) {
                    case LOAD -> this.load();
                    case SAVE -> this.save();
                    case DESTROY -> this.destroy();
                }

                actionQueue.remove(action);
            });
        } finally {
            busy = false;
            handlingActionQueue = false;
        }
    }

    /**
     * Checks if the session file is busy. If busy, queues the action for execution once the current job is completed.
     *
     * @param jobType The action to queue for later execution.
     * @return {@code true} if the runnable was queued, {@code false} if the manager was not busy and no action was queued.
     */
    public boolean availableOrQueue(JobType jobType) {
        if (!isBusy()) return false;

        if (actionQueue.contains(jobType)) return true;
        actionQueue.add(jobType);

        return true;
    }

    /**
     * Checks whether the session file is currently busy with an operation.
     *
     * @return {@code true} if the session file is busy, otherwise {@code false}.
     */
    public boolean isBusy() {
        return busy;
    }

    /**
     * Retrieves the {@link SessionDriver} instance associated with this session file.
     *
     * @return The associated {@link SessionDriver}.
     * @since 3.3.5-SNAPSHOT
     */
    public SessionDriver getDriver() {
        return driver;
    }

    /**
     * Retrieves the {@link Session} instance associated with this session file.
     *
     * @return The associated {@link Session}.
     */
    public Session getSession() {
        return session;
    }

    /**
     * Indicating the type of job the {@link SessionFile} is performing.
     *
     * @author Philipp Maywald
     * @author CraftsBlock
     * @version 1.0.0
     * @since 3.3.5-SNAPSHOT
     */
    public enum JobType {

        /**
         * Indicating a load process.
         */
        LOAD,

        /**
         * Indicating a save process.
         */
        SAVE,

        /**
         * Indicating a destroy process.
         */
        DESTROY,

    }

}
