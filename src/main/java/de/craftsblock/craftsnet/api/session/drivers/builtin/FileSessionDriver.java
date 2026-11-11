package de.craftsblock.craftsnet.api.session.drivers.builtin;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.craftsblock.craftscore.buffer.ObjectSerializer;
import de.craftsblock.craftsnet.api.session.Session;
import de.craftsblock.craftsnet.api.session.SessionInfo;
import de.craftsblock.craftsnet.api.session.drivers.SessionDriver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A file based implementation of {@link SessionDriver} that persists session data to disk.
 *
 * <p>Session files are stored in the {@link #STORAGE_LOCATION} directory.
 * Each file is named after its session ID with the {@link #STORAGE_EXTENSION} extension.</p>
 *
 * <p>All session paths are validated and normalized before accessing the filesystem.
 * Existing files are additionally resolved using {@link Path#toRealPath(LinkOption...)}
 * to prevent path traversal and symbolic link attacks.</p>
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

    static {
        try {
            Files.createDirectories(Path.of(STORAGE_LOCATION));
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    /**
     * Resolves and validates a session file path.
     *
     * <p>The session directory is created if it does not exist.
     * Existing session files are resolved using {@code toRealPath()}
     * to ensure that they remain inside the session directory.</p>
     *
     * <p>Non-existing session files are allowed. This is necessary
     * for operations such as saving a new session or checking whether
     * a session exists.</p>
     *
     * @param sessionID The session identifier.
     * @return The validated session file path.
     * @throws IOException If the session ID or resolved path is invalid.
     */
    private Path resolveSessionPath(String sessionID) throws IOException {
        if (SessionInfo.isValidSessionID(sessionID)) {
            throw new IOException("Invalid session ID!");
        }

        Path directory = Path.of(STORAGE_LOCATION)
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(directory);
        Path realDirectory = directory.toRealPath();

        Path target = realDirectory
                .resolve(sessionID + STORAGE_EXTENSION)
                .normalize();

        if (!target.startsWith(realDirectory)) {
            throw new IOException("Session path escapes storage directory!");
        }

        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(
                    target,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS
            );
        } catch (NoSuchFileException e) {
            return target;
        }

        if (attributes.isSymbolicLink()) {
            throw new IOException("Symbolic links are not allowed for session files!");
        }

        Path realTarget;
        try {
            realTarget = target.toRealPath();
        } catch (NoSuchFileException e) {
            return target;
        }

        if (!realTarget.startsWith(realDirectory)) {
            throw new IOException("Resolved session path escapes storage directory!");
        }

        return realTarget;
    }

    /**
     * Checks whether the corresponding session file exists.
     *
     * @param session   The session instance.
     * @param sessionID The unique session identifier.
     * @return {@code true} if the session file exists and is a regular file.
     */
    @Override
    public boolean exists(Session session, String sessionID) {
        try {
            Path path = resolveSessionPath(sessionID);
            return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            throw new RuntimeException("Could not check session file!", e);
        }
    }

    /**
     * Loads session data from the corresponding session file.
     *
     * <p>If the session file does not exist, this method simply returns
     * without throwing an exception.</p>
     *
     * @param session   The session instance to populate.
     * @param sessionID The unique session identifier.
     */
    @Override
    public void load(Session session, String sessionID) {
        try {
            Path path = resolveSessionPath(sessionID);
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                return;
            }

            try (FileChannel channel = FileChannel.open(
                    path,
                    StandardOpenOption.READ,
                    LinkOption.NOFOLLOW_LINKS
            );
                 InputStream stream = Channels.newInputStream(channel)) {

                byte[] data = stream.readAllBytes();

                if (data.length == 0) {
                    return;
                }

                BufferUtil readBuffer = BufferUtil.wrap(data);

                while (readBuffer.hasRemainingBytes()) {
                    String key = readBuffer.getUtf();
                    byte[] value = readBuffer.getNBytes(readBuffer.getVarInt());

                    session.put(key, ObjectSerializer.deserialize(value));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load persistent session data!", e);
        }
    }

    /**
     * Saves session data to the corresponding session file.
     *
     * <p>Data is first written to a temporary file and then atomically
     * moved into its final destination to avoid partially written sessions.</p>
     *
     * @param session   The session containing the data to save.
     * @param sessionID The unique session identifier.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void save(Session session, String sessionID) throws IOException {
        Path path = resolveSessionPath(sessionID);

        BufferUtil saveBuffer = BufferUtil.allocate(0);
        session.forEach((key, value) -> {
            byte[] valueBytes = ObjectSerializer.serialize(value);

            int needed = 8
                    + key.getBytes(StandardCharsets.UTF_8).length
                    + valueBytes.length;

            saveBuffer.ensure(needed)
                    .putUtf(key)
                    .putVarInt(valueBytes.length)
                    .with(raw -> raw.put(valueBytes));
        });

        Path parent = path.getParent();
        Path tempFile = Files.createTempFile(parent, "session-", ".tmp");

        try {
            try (FileChannel channel = FileChannel.open(
                    tempFile,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
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
     * Deletes the corresponding session file.
     *
     * <p>If the session file does not exist, nothing happens.</p>
     *
     * @param session   The session instance.
     * @param sessionID The unique session identifier.
     */
    @Override
    public void destroy(Session session, String sessionID) {
        try {
            Path path = resolveSessionPath(sessionID);
            Files.deleteIfExists(path);
        } catch (NoSuchFileException ignored) {
        } catch (IOException e) {
            throw new RuntimeException("Could not delete session data!", e);
        }
    }

}
