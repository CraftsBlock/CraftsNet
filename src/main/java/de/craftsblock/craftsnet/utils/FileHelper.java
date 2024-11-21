package de.craftsblock.craftsnet.utils;

import de.craftsblock.craftsnet.CraftsNet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A utility class that helps manage the creation of temporary files in the file system.
 * It determines where to store temporary files, either in the system's default temporary folder
 * or a custom directory based on available space or specific configurations.
 * <p>
 * This class is useful for handling file operations that require temporary storage during runtime.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.0.7-SNAPSHOT
 */
public class FileHelper {

    private final Path tempDir;

    /**
     * Constructs a {@code FileHelper} instance that sets up a directory for temporary files.
     * The directory is determined based on available disk space and an optional configuration flag.
     * <p>
     * If {@code forceNormalFileSystem} is true, temporary files will be stored in a "temp" folder in the current
     * working directory. Otherwise, the system's default temporary file location will be used, unless
     * there is less than 250 MB of available space, in which case the "temp" folder will be used instead.
     *
     * @param craftsNet             the main application instance providing the logger for warnings.
     * @param forceNormalFileSystem if true, forces the use of the "temp" folder in the current directory.
     */
    public FileHelper(CraftsNet craftsNet, boolean forceNormalFileSystem) {
        if (forceNormalFileSystem) {
            File temp = new File("./temp/");
            temp.mkdirs();
            tempDir = temp.toPath();
            return;
        }

        try {
            File temp = Files.createTempFile("log_", ".").toFile();
            double size = (double) temp.getUsableSpace() / 1000 / 1000;
            temp.delete();
            if (size < 250) {
                craftsNet.logger().warning("Your temporary folder has less than 250 MB free space!");
                craftsNet.logger().warning("Switched to the file system on which craftsnet is running for temporary files.");
                craftsNet.logger().warning("If you want to hide this warning, add `--placeTempFileInNormal` to your start command.");
                File t = new File("./temp/");
                t.mkdirs();
                tempDir = t.toPath();
                return;
            }

            tempDir = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a temporary file with the given prefix and suffix in the appropriate directory.
     * If a custom directory was selected during initialization, the file will be created there.
     * Otherwise, the system's default temporary file directory is used.
     * <p>
     * If no POSIX file permissions are explicitly provided, the file will be created with default
     * POSIX permissions: read and write access for the owner.
     * </p>
     *
     * @param prefix the prefix string to be used in generating the file's name.
     * @param suffix the suffix string to be used in generating the file's name.
     * @param attrs  an optional list of file attributes to set atomically when creating the file.
     * @return the path to the created temporary file.
     * @throws IOException if an I/O error occurs while creating the file.
     */
    public Path createTempFile(String prefix, String suffix, FileAttribute<?>... attrs) throws IOException {
        if (Stream.of(attrs).noneMatch(attr -> "posix:permissions".equalsIgnoreCase(attr.name()))) {
            FileAttribute<Set<PosixFilePermission>> defaultPerms = PosixFilePermissions.asFileAttribute(Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            ));

            attrs = Stream.concat(Stream.of(attrs), Stream.of(defaultPerms)).toArray(FileAttribute[]::new);
        }

        return tempDir == null
                ? Files.createTempFile(prefix, suffix, attrs)
                : Files.createTempFile(tempDir, prefix, suffix, attrs);
    }

}
