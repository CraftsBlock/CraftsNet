package de.craftsblock.craftsnet.api.session.drivers.builtin;

import de.craftsblock.craftsnet.api.session.Session;
import de.craftsblock.craftsnet.api.session.drivers.SessionDriver;
import de.craftsblock.craftsnet.utils.ByteBuffer;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * A file based implementation of {@link SessionDriver} that persists session data to disk.
 * <p>
 * This implementation stores session data in files located in the {@link #STORAGE_LOCATION} directory.
 * Each session is saved as a file with a name based on its session ID appended with the
 * {@link #STORAGE_EXTENSION}.
 * </p>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @see Session
 * @see SessionDriver
 * @since 3.3.5-SNAPSHOT
 */
public class FileSessionDriver implements SessionDriver {

    /**
     * The default directory where session files are stored.
     */
    public static final String STORAGE_LOCATION = "./sessions";

    /**
     * The file extension used for session files.
     */
    public static final String STORAGE_EXTENSION = ".cnetsess";

    /**
     * Loads the session data from a file corresponding to the sessions ID.
     *
     * @param session   The session instance to populate with stored data.
     * @param sessionID The unique identifier of the session (the session's own ID is used to determine the file).
     * @throws RuntimeException If an I/O error occurs or deserialization fails.
     */
    @Override
    public void load(Session session, String sessionID) {
        Path path = Path.of(STORAGE_LOCATION, session.getSessionInfo().getSessionID() + STORAGE_EXTENSION);
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
                    session.put(key, reader.readObject());
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Saves the session data to a file corresponding to the sessions ID.
     *
     * @param session   The session instance containing data to be saved.
     * @param sessionID The unique identifier of the session (the session's own ID is used to determine the file).
     * @throws IOException If an error occurs during the file write operation.
     */
    @Override
    public void save(Session session, String sessionID) throws IOException {
        ByteBuffer saveBuffer = new ByteBuffer(0, false);
        for (Map.Entry<String, Object> entry : session.entrySet())
            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 ObjectOutputStream writer = new ObjectOutputStream(out)) {
                writer.writeObject(entry.getValue());

                byte[] objBytes = out.toByteArray();
                saveBuffer.writeUTF(entry.getKey());
                saveBuffer.writeVarInt(objBytes.length);
                saveBuffer.write(objBytes);
            } catch (IOException e) {
                session.getSessionInfo().getLogger().error(e, "Skipping key " + entry.getKey());
            }

        Path path = Path.of(STORAGE_LOCATION, session.getSessionInfo().getSessionID() + STORAGE_EXTENSION);
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
    }

    /**
     * Deletes the session file corresponding to the given session ID.
     *
     * @param session   The session instance (not used directly in deletion).
     * @param sessionID The unique identifier of the session whose file should be deleted.
     */
    @Override
    public void destroy(Session session, String sessionID) {
        File data = new File(STORAGE_LOCATION, sessionID + STORAGE_EXTENSION);
        if (!data.exists()) return;
        data.delete();
    }

}
