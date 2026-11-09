
package de.craftsblock.craftsnet.api.session;

import de.craftsblock.craftsnet.api.session.drivers.SessionDriver;
import de.craftsblock.craftsnet.api.session.drivers.builtin.FileSessionDriver;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

/**
 * Handles the persistence of session data by providing functionality for loading, saving,
 * and destroying session files.
 *
 * <p>Session operations are executed sequentially. Concurrent operations are queued and
 * processed in their submission order.</p>
 *
 * <p>All internal state, including the active driver and job queue, is protected by
 * a dedicated lock. Driver operations are executed outside this lock to avoid blocking
 * queue management during potentially expensive I/O operations.</p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @see Session
 * @since 3.3.0-SNAPSHOT
 */
public class SessionStorage {

    private static volatile SessionDriver defaultDriver = new FileSessionDriver();

    /**
     * Changes the default {@link SessionDriver} used when creating sessions.
     *
     * @param defaultDriver The new default driver.
     * @since 3.3.5-SNAPSHOT
     */
    @ApiStatus.Experimental
    public static void setDefaultDriver(SessionDriver defaultDriver) {
        SessionStorage.defaultDriver = Objects.requireNonNull(defaultDriver, "defaultDriver");
    }

    private final Object lock = new Object();

    private final Session session;
    private final Queue<QueuedJob> actionQueue = new ArrayDeque<>();

    private SessionDriver driver;
    private boolean busy = false;

    /**
     * Constructs a new session storage instance.
     *
     * @param session The associated session.
     */
    public SessionStorage(Session session) {
        this.session = Objects.requireNonNull(session, "session");
        this.driver = defaultDriver;
    }

    /**
     * Checks whether the session exists in the underlying driver.
     *
     * @return {@code true} if the session exists.
     */
    public boolean exists() {
        if (!this.session.isSessionStarted()) {
            return false;
        }

        SessionDriver currentDriver;
        synchronized (lock) {
            currentDriver = this.driver;
        }

        try {
            return currentDriver.exists(
                    this.session,
                    this.session.getSessionInfo().getSessionID()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads the session data.
     * <p>
     * If the storage is busy, the operation is queued.
     */
    public void load() {
        if (!this.exists()) {
            return;
        }

        this.performJob(JobType.LOAD);
    }

    /**
     * Saves the session data.
     * <p>
     * If the storage is busy, the operation is queued.
     */
    public void save() {
        this.performJob(JobType.SAVE);
    }

    /**
     * Deletes the persistent session data.
     * <p>
     * If the storage is busy, the operation is queued.
     */
    protected void destroy() {
        this.performJob(JobType.DESTROY);
    }

    /**
     * Migrates the session to another driver.
     * <p>
     * The migration is queued if another operation is currently running.
     *
     * @param driver The new session driver.
     * @since 3.3.5-SNAPSHOT
     */
    public void migrate(SessionDriver driver) {
        Objects.requireNonNull(driver, "driver");

        QueuedJob job;
        synchronized (lock) {
            if (this.driver.equals(driver)) {
                return;
            }

            SessionDriver previousDriver = this.driver;
            this.driver = driver;

            job = new QueuedJob(
                    JobType.MIGRATE,
                    driver,
                    previousDriver
            );

            if (!this.session.isSessionStarted()) {
                return;
            }

            if (busy) {
                actionQueue.offer(job);
                return;
            }

            busy = true;
        }

        processJobs(job);
    }

    /**
     * Submits a session operation.
     * <p>
     * If another operation is running, the job is queued.
     * Otherwise, processing starts immediately.
     *
     * @param type The operation type.
     * @param args Additional arguments.
     */
    private void performJob(JobType type, Object... args) {
        if (!this.session.isSessionStarted()) {
            return;
        }

        QueuedJob job;
        synchronized (lock) {
            if (!this.session.isSessionStarted()) {
                return;
            }

            job = createJob(type, args, this.driver);

            if (busy) {
                actionQueue.offer(job);
                return;
            }

            busy = true;
        }

        processJobs(job);
    }

    /**
     * Creates a queued job.
     *
     * @param type   The operation type.
     * @param args   Additional arguments.
     * @param driver The driver associated with this job.
     * @return The created job.
     */
    private QueuedJob createJob(
            JobType type,
            Object[] args,
            SessionDriver driver
    ) {
        Objects.requireNonNull(type, "type");

        if (type == JobType.MIGRATE) {
            if (args.length == 0 || !(args[0] instanceof SessionDriver previousDriver)) {
                throw new IllegalArgumentException(
                        "MIGRATE requires a previous SessionDriver."
                );
            }

            return new QueuedJob(type, driver, previousDriver);
        }

        return new QueuedJob(type, driver, null);
    }

    /**
     * Processes jobs sequentially.
     * <p>
     * The queue lock is only acquired when accessing the queue.
     * Actual driver operations are executed outside the queue lock.
     * <p>
     * Failed jobs do not prevent subsequent jobs from being processed.
     *
     * @param initialJob The first job to execute.
     */
    private void processJobs(QueuedJob initialJob) {
        QueuedJob currentJob = initialJob;
        Throwable failure = null;

        try {
            while (currentJob != null) {
                try {
                    forcePerformJob(currentJob);
                } catch (RuntimeException | Error e) {
                    if (failure == null) {
                        failure = e;
                    } else if (failure != e) {
                        failure.addSuppressed(e);
                    }
                }

                synchronized (lock) {
                    currentJob = actionQueue.poll();

                    if (currentJob == null) {
                        busy = false;
                    }
                }
            }
        } finally {
            synchronized (lock) {
                busy = false;
            }
        }

        if (failure instanceof RuntimeException e) {
            throw e;
        }

        if (failure instanceof Error e) {
            throw e;
        }
    }

    /**
     * Executes a single job using its associated driver.
     *
     * @param job The job to execute.
     */
    private void forcePerformJob(QueuedJob job) {
        try {
            String sessionID = this.session.getSessionInfo().getSessionID();

            synchronized (session) {
                switch (job.type()) {
                    case LOAD -> job.driver().load(this.session, sessionID);

                    case SAVE -> job.driver().save(this.session, sessionID);

                    case DESTROY -> job.driver().destroy(this.session, sessionID);

                    case MIGRATE -> job.driver().migrate(
                            this.session,
                            sessionID,
                            job.previousDriver()
                    );
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks whether the storage is busy.
     *
     * @return {@code true} if an operation is currently running.
     */
    public boolean isBusy() {
        synchronized (lock) {
            return busy;
        }
    }

    /**
     * Retrieves the currently associated session driver.
     *
     * @return The current driver.
     */
    public SessionDriver getDriver() {
        synchronized (lock) {
            return driver;
        }
    }

    /**
     * Retrieves the associated session.
     *
     * @return The session.
     */
    public Session getSession() {
        return session;
    }

    /**
     * Represents a queued session operation.
     *
     * @param type           The operation type.
     * @param driver         The driver associated with this operation.
     * @param previousDriver The previous driver, used for migration.
     */
    private record QueuedJob(
            JobType type,
            SessionDriver driver,
            SessionDriver previousDriver
    ) {
    }

    /**
     * Represents the available session operations.
     */
    public enum JobType {

        /**
         * Loads session data.
         */
        LOAD,

        /**
         * Saves session data.
         */
        SAVE,

        /**
         * Destroys persistent session data.
         */
        DESTROY,

        /**
         * Migrates session data between drivers.
         */
        MIGRATE,
    }
}
