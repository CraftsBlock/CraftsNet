package de.craftsblock.craftsnet.events.requests.shares;

import de.craftsblock.craftscore.event.CancellableEvent;
import de.craftsblock.craftsnet.api.http.Exchange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents an event that is triggered when a share file has been loaded.
 * This event provides information about the loaded file.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.4
 * @see CancellableEvent
 * @since 2.3.2-SNAPSHOT
 */
public class ShareFileLoadedEvent extends CancellableEvent {

    private static final FileNameMap contentTypes = URLConnection.getFileNameMap();

    private final Exchange exchange;
    private Path path;
    private String contentType;

    /**
     * Creates a new ShareFileLoadedEvent with the specified loaded file.
     *
     * @param exchange The exchange used by the share to handle its connection
     * @param path     The loaded file path associated with this event.
     */
    public ShareFileLoadedEvent(@NotNull Exchange exchange, @NotNull Path path) {
        this.exchange = exchange;
        this.path = path;
    }

    /**
     * Gets the exchange which holds all http information about the connection with the shared location.
     *
     * @return The exchange object storing important information
     */
    public Exchange getExchange() {
        return exchange;
    }

    /**
     * Gets the loaded file path associated with this event.
     *
     * @return The loaded file path.
     * @since 3.3.5-SNAPSHOT
     */
    public Path getPath() {
        return path;
    }

    /**
     * Gets the loaded file associated with this event.
     *
     * @return The loaded file.
     */
    @NotNull
    public File getFile() {
        return path.toFile();
    }

    /**
     * Sets the loaded file path associated with this event.
     *
     * @param path The new loaded file path.
     * @since 3.3.5-SNAPSHOT
     */
    public void setPath(@NotNull Path path) {
        this.path = path;
    }

    /**
     * Sets the loaded file associated with this event.
     *
     * @param file The new loaded file.
     */
    public void setFile(@NotNull File file) {
        this.setPath(file.toPath());
    }

    /**
     * Determines the content type of the current file.
     *
     * @return The content type of the file.
     */
    @Nullable
    public String getRawContentType() {
        if (Files.notExists(path)) return null;
        return contentTypes.getContentTypeFor(path.getFileName().toString());
    }

    /**
     * Returns the {@link ShareFileLoadedEvent#contentType} if it is not null, otherwise the content type of the file.
     *
     * @return The content type of the file.
     */
    @Nullable
    public String getContentType() {
        return contentType != null ? contentType : getRawContentType();
    }

    /**
     * Forces the sending of a specific content type.
     *
     * @param contentType The content type that should be sent
     */
    public void setContentType(@Nullable String contentType) {
        this.contentType = contentType;
    }

}
