package de.craftsblock.craftsnet.api.session.drivers.builtin;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.craftsblock.craftscore.buffer.ObjectSerializer;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.api.session.Session;
import de.craftsblock.craftsnet.api.session.drivers.SessionDriver;
import de.craftsblock.craftsnet.utils.FileHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

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
     * Check if the corresponding session file exists on the hard drive.
     *
     * @param session   The {@link Session} instance to be populated with data.
     * @param sessionID The unique identifier of the session.
     * @since 3.4.0-SNAPSHOT
     */
    @Override
    public boolean exists(Session session, String sessionID) {
        Path path = Path.of(STORAGE_LOCATION, session.getSessionInfo().getSessionID() + STORAGE_EXTENSION);
        try {
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            return Files.exists(path) && Files.isRegularFile(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads the session data from a file corresponding to the sessions ID.
     *
     * @param session   The session instance to populate with stored data.
     * @param sessionID The unique identifier of the session (the session's own ID is used to determine the file).
     * @throws RuntimeException If an I/O error occurs or deserialization fails.
     */
    @Override
    public void load(Session session, String sessionID) {
        if (!this.exists(session, sessionID)) {
            return;
        }

        Path path = Path.of(STORAGE_LOCATION, session.getSessionInfo().getSessionID() + STORAGE_EXTENSION);

        try {
            if (Files.size(path) <= 0) {
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not retrieve file size of session file!", e);
        }

        try (FileChannel channel = FileChannel.open(path);
             InputStream stream = Channels.newInputStream(channel)) {

            BufferUtil readBuffer = BufferUtil.wrap(stream.readAllBytes());

            while (readBuffer.hasRemainingBytes()) {
                String key = readBuffer.getUtf();
                byte[] value = readBuffer.getNBytes(readBuffer.getVarInt());
                session.put(key, ObjectSerializer.deserialize(value));
            }
        } catch (OverlappingFileLockException e) {
            throw new RuntimeException("Could not lock session file!", e);
        } catch (IOException e) {
            throw new RuntimeException("Could not load persistent session data!", e);
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
        BufferUtil saveBuffer = BufferUtil.allocate(0);
        session.forEach((key, value) -> {
            byte[] valueBytes = ObjectSerializer.serialize(value);
            int needed = 8 + key.getBytes(StandardCharsets.UTF_8).length + valueBytes.length;
            saveBuffer.ensure(needed)
                    .putUtf(key)
                    .putVarInt(valueBytes.length)
                    .with(raw -> raw.put(valueBytes));
        });

        Path path = Path.of(STORAGE_LOCATION, session.getSessionInfo().getSessionID() + STORAGE_EXTENSION);
        Path parent = path.toAbsolutePath().getParent();
        Files.createDirectories(parent);

        Path tempFile = Files.createTempFile(parent, "session-", ".tmp");

        try {
            try (FileChannel channel = FileChannel.open(tempFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                 OutputStream stream = Channels.newOutputStream(channel)) {

                int bufferSize = 1024;
                saveBuffer.trim().with(ByteBuffer::flip);

                while (saveBuffer.hasRemainingBytes(bufferSize)) {
                    stream.write(saveBuffer.getNBytes(bufferSize));
                }

                stream.write(saveBuffer.getRemainingBytes());
                stream.flush();

                channel.force(true);
            }

            Files.move(tempFile, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tempFile);
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
        try {
            Path data = Path.of(STORAGE_LOCATION, sessionID + STORAGE_EXTENSION);

            if (Files.notExists(data)) {
                return;
            }

            Files.delete(data);
        } catch (IOException e) {
            throw new RuntimeException("Could not delete session data!", e);
        }
    }

}
