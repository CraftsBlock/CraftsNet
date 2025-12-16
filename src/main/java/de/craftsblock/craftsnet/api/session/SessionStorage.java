package de.craftsblock.craftsnet.api.session;

import de.craftsblock.craftsnet.api.session.drivers.SessionDriver;
import de.craftsblock.craftsnet.api.session.drivers.builtin.FileSessionDriver;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

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
 * @version 3.4.3
 * @see Session
 * @since 3.3.0-SNAPSHOT
 */
public class SessionStorage {

    private static SessionDriver defaultDriver = new FileSessionDriver();

    /**
     * Changes the default {@link SessionDriver} that will be used when
     * creating sessions.
     * <p>
     * <b>NOTE:</b> This method will change in future releases so use with caution.
     *
     * @param defaultDriver The new default {@link SessionDriver}.
     * @since 3.3.5-SNAPSHOT
     */
    @ApiStatus.Experimental
    public static void setDefaultDriver(SessionDriver defaultDriver) {
        SessionStorage.defaultDriver = defaultDriver;
    }

    private final Session session;
    private final Queue<QueuedJob> actionQueue = new LinkedBlockingQueue<>();

    private SessionDriver driver;

    private boolean busy = false;

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
     * Checks if the session exists in the underlying {@link SessionDriver driver}.
     *
     * @return {@code true} if the session exists in the {@link SessionDriver driver}, {@code false} otherwise.
     * @since 3.4.0-SNAPSHOT
     */
    public boolean exists() {
        try {
            if (!this.session.isSessionStarted()) return false;
            return this.driver.exists(this.session, this.session.getSessionInfo().getSessionID());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads the sessions data using the currently set {@link SessionDriver driver}.
     * If the {@link SessionStorage storage} is currently busy, the load
     * action will be queued and performed later on.
     */
    public void load() {
        if (!this.exists()) return;
        this.performJob(JobType.LOAD);
    }

    /**
     * Saves the sessions data using the currently set {@link SessionDriver driver}.
     * If the {@link SessionStorage storage} is currently busy, the save
     * action will be queued and performed later on.
     */
    public void save() {
        if (!this.session.isSessionStarted()) return;
        this.performJob(JobType.SAVE);
    }

    /**
     * Deletes the persistent session data holder using the currently set
     * {@link SessionDriver driver}.
     * If the {@link SessionStorage storage} is currently busy, the destroy
     * action will be queued and performed later on.
     */
    protected void destroy() {
        this.performJob(JobType.DESTROY);
    }

    /**
     * Migrates this session from the currently set {@link SessionDriver driver}
     * to another {@link SessionDriver driver}.
     * If the {@link SessionStorage storage} is currently busy, the migrate
     * action will be queued and performed later on.
     *
     * @param driver The new {@link SessionDriver}.
     * @since 3.3.5-SNAPSHOT
     */
    public void migrate(SessionDriver driver) {
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
            this.forcePerformJob(type, args);
        } finally {
            completeJob();
        }
    }

    /**
     * Force performs a job. If the session is currently not persistent nothing
     * will happen.
     *
     * @param type The {@link JobType} which will be performed.
     * @param args An array of objects which can be passed down to the driver.
     * @since 3.4.0-SNAPSHOT
     */
    private void forcePerformJob(JobType type, Object... args) {
        try {
            String sessionID = this.session.getSessionInfo().getSessionID();
            synchronized (session) {
                switch (type) {
                    case LOAD -> this.driver.load(this.session, sessionID);
                    case SAVE -> this.driver.save(this.session, sessionID);
                    case DESTROY -> this.driver.destroy(this.session, sessionID);
                    case MIGRATE -> this.driver.migrate(this.session, sessionID, (SessionDriver) args[0]);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Completes all jobs in the queue and marks the session file as not busy.
     * Ensures thread safety and prevents concurrent modification issues.
     */
    private synchronized void completeJob() {
        try {
            if (actionQueue.isEmpty()) return;

            QueuedJob job;
            while ((job = actionQueue.poll()) != null)
                this.forcePerformJob(job.type(), job.args());

            if (!actionQueue.isEmpty()) this.completeJob();
        } finally {
            busy = false;
        }
    }

    /**
     * Checks if the session file is busy. If busy, queues the action for execution once the current job is completed.
     *
     * @param type The action to queue for later execution.
     * @param args The args which should be passed to the driver.
     * @return {@code true} if the runnable was queued, {@code false} if the manager was not busy and no action was queued.
     */
    public boolean availableOrQueue(JobType type, Object... args) {
        if (!isBusy()) return false;

        actionQueue.offer(new QueuedJob(type, args));
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
     * Represents a queued job.
     *
     * @param type The {@link JobType}.
     * @param args Additional arguments.
     * @author Philipp Maywald
     * @author CraftsBlock
     * @version 1.0.0
     * @since 3.4.0-SNAPSHOT
     */
    private record QueuedJob(JobType type, Object... args) {

    }

    /**
     * Indicating the type of job the {@link SessionStorage} is performing.
     *
     * @author Philipp Maywald
     * @author CraftsBlock
     * @version 1.0.1
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
         *
         * @since 3.3.5-SNAPSHOT
         */
        MIGRATE,

    }

}
