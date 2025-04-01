package de.craftsblock.craftsnet.api.session;

import de.craftsblock.craftscore.annotations.Experimental;
import de.craftsblock.craftsnet.api.session.drivers.SessionDriver;
import de.craftsblock.craftsnet.api.session.drivers.builtin.FileSessionDriver;
import de.craftsblock.craftsnet.utils.ByteBuffer;
import org.jetbrains.annotations.ApiStatus;

import java.io.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Handles the persistence of session data by providing functionality for loading, saving,
 * and destroying session files. The session data is serialized to a file and deserialized
 * back into memory, allowing for session state preservation between application runs.
 *
 * <p>The {@link  SessionStorage} class ensures data integrity through file locking mechanisms
 * and supports thread-safe operations by preventing concurrent modifications.</p>
 *
 * <p>Session files are stored in a predefined directory, and their filenames are constructed
 * using the session identifier.</p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 3.3.0
 * @see Session
 * @see ByteBuffer
 * @since 3.3.0-SNAPSHOT
 */
public class SessionStorage {

    private static SessionDriver defaultDriver = new FileSessionDriver();

    /**
     * Changes the default {@link SessionDriver} that will be used when
     * creating sessions.
     *
     * @param defaultDriver The new default {@link SessionDriver}.
     * @since 3.3.5-SNAPSHOT
     * @experimental This method will change in future releases so use with caution.
     */
    @Experimental
    @ApiStatus.Experimental
    public static void setDefaultDriver(SessionDriver defaultDriver) {
        SessionStorage.defaultDriver = defaultDriver;
    }

    private final Session session;
    private final ConcurrentLinkedDeque<JobType> actionQueue = new ConcurrentLinkedDeque<>();

    private SessionDriver driver;

    private boolean busy = false;
    private boolean handlingActionQueue = false;

    /**
     * Constructs a new {@link SessionStorage} instance for managing the specified session.
     *
     * @param session the session associated with this file handler.
     */
    public SessionStorage(Session session) {
        this.session = session;
        this.driver = defaultDriver;
    }

    /**
     * Loads session data from the corresponding session file, if it exists.
     * The data is deserialized and added to the session.
     *
     * <p>File locking ensures that no other thread or process can access the file
     * during the load operation.</p>
     */
    public void load() {
        this.performJob(JobType.LOAD);
    }

    /**
     * Saves the current session data to the corresponding session file.
     * The data is serialized to ensure persistence across application runs.
     *
     * <p>File locking is used to prevent concurrent access during the save operation.</p>
     */
    public void save() {
        this.performJob(JobType.SAVE);
    }

    /**
     * Deletes the session file associated with the current session.
     * Ensures that the file is removed if it exists.
     */
    protected void destroy() {
        this.performJob(JobType.DESTROY);
    }

    /**
     * Migrates this session to another {@link SessionDriver}.
     *
     * @param driver The new {@link SessionDriver}.
     * @since 3.3.5-SNAPSHOT
     */
    protected void migrate(SessionDriver driver) {
        if (this.driver.equals(driver)) return;
        SessionDriver priorDriver = this.driver;
        this.driver = driver;
        this.performJob(JobType.MIGRATE, priorDriver);
    }

    /**
     * Performs a job if the storage is currently not busy. Otherwise,
     * it queues the call of the job. If the session is currently not
     * persistent nothing will happen.
     *
     * @param type The {@link JobType} which will be performed.
     * @param args An array of objects which can be passed down to the driver.
     * @since 3.3.5-SNAPSHOT
     */
    private void performJob(JobType type, Object... args) {
        if (!this.session.isSessionStarted()) return;
        if (availableOrQueue(type)) return;

        busy = true;
        try {
            String sessionID = this.session.getSessionInfo().getSessionID();
            switch (type) {
                case LOAD -> this.driver.load(this.session, sessionID);
                case SAVE -> this.driver.save(this.session, sessionID);
                case DESTROY -> this.driver.destroy(this.session, sessionID);
                case MIGRATE -> this.driver.migrate(this.session, sessionID, (SessionDriver) args[0]);
            }
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

        if (actionQueue.getLast().equals(jobType)) return true;
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
     * Indicating the type of job the {@link SessionStorage} is performing.
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

        /**
         * Indicating a migrate process.
         */
        MIGRATE,

    }

}
