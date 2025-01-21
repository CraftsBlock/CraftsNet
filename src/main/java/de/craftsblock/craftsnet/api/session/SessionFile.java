package de.craftsblock.craftsnet.api.session;

import de.craftsblock.craftsnet.utils.ByteBuffer;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
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
 * @version 3.0.0
 * @see Session
 * @see ByteBuffer
 * @since 3.3.0-SNAPSHOT
 */
public class SessionFile {

    /**
     * The default directory where session files are stored.
     */
    public static final String STORAGE_LOCATION = "./sessions";

    /**
     * The file extension used for session files.
     */
    public static final String STORAGE_EXTENSION = ".cnetsess";

    private final Session session;
    private final ConcurrentLinkedQueue<String> actionQueue = new ConcurrentLinkedQueue<>();

    private boolean busy = false;
    private boolean handlingActionQueue = false;

    /**
     * Constructs a new {@link  SessionFile} instance for managing the specified session.
     *
     * @param session the session associated with this file handler.
     */
    public SessionFile(Session session) {
        this.session = session;
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
        if (availableOrQueue("load")) return;

        busy = true;
        try {
            Path path = Path.of(STORAGE_LOCATION, this.session.getSessionInfo().getSessionID() + STORAGE_EXTENSION);
            try {
                if (path.getParent() != null && !Files.exists(path.getParent()))
                    Files.createDirectories(path.getParent());

                if (!Files.exists(path)) return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try (FileChannel channel = FileChannel.open(path);
                 InputStream stream = Channels.newInputStream(channel);
                 FileLock ignored = channel.lock(0, Long.MAX_VALUE, true)) {

                ByteBuffer readBuffer = new ByteBuffer(stream.readAllBytes());

                while (readBuffer.isReadable()) {
                    String key = readBuffer.readUTF();
                    byte[] obj = readBuffer.readNBytes(readBuffer.readVarInt());

                    try (ByteArrayInputStream in = new ByteArrayInputStream(obj);
                         ObjectInputStream reader = new ObjectInputStream(in)) {
                        this.session.put(key, reader.readObject());
                    } catch (IOException | ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
        if (availableOrQueue("save")) return;

        busy = true;
        try {
            ByteBuffer saveBuffer = new ByteBuffer(0, false);
            for (Map.Entry<String, Object> entry : this.session.entrySet())
                try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                     ObjectOutputStream writer = new ObjectOutputStream(out)) {
                    writer.writeObject(entry.getValue());

                    byte[] objBytes = out.toByteArray();
                    saveBuffer.writeUTF(entry.getKey());
                    saveBuffer.writeVarInt(objBytes.length);
                    saveBuffer.write(objBytes);
                } catch (IOException e) {
                    this.session.getSessionInfo().getLogger().error(e, "Skipping key " + entry.getKey());
                }

            Path path = Path.of(STORAGE_LOCATION, this.session.getSessionInfo().getSessionID() + STORAGE_EXTENSION);
            if (path.getParent() != null && !Files.exists(path.getParent()))
                Files.createDirectories(path.getParent());

            if (!Files.exists(path)) Files.createFile(path);

            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE);
                 OutputStream stream = Channels.newOutputStream(channel)) {
                FileLock lock;
                if (!Thread.currentThread().isInterrupted()) lock = channel.lock();
                else lock = null;

                try {
                    int bufferSize = 1024;
                    while (saveBuffer.isReadable(bufferSize))
                        stream.write(saveBuffer.readNBytes(bufferSize));

                    stream.write(saveBuffer.readRemaining());
                } finally {
                    if (lock != null) lock.release();
                }
            }
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
        if (availableOrQueue("destroy")) return;

        busy = true;
        try {
            File data = new File(STORAGE_LOCATION, this.session.getSessionInfo().getSessionID() + STORAGE_EXTENSION);
            if (!data.exists()) return;

            data.delete();
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
                    case "load" -> this.load();
                    case "save" -> this.save();
                    case "destroy" -> this.destroy();
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
     * @param action The action to queue for later execution.
     * @return {@code true} if the runnable was queued, {@code false} if the manager was not busy and no action was queued.
     */
    public boolean availableOrQueue(String action) {
        if (!isBusy()) return false;

        if (actionQueue.contains(action)) return true;
        actionQueue.add(action);

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
     * Retrieves the {@link Session} instance associated with this session file.
     *
     * @return the associated {@code Session}.
     */
    public Session getSession() {
        return session;
    }

}
