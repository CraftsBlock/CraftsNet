package de.craftsblock.craftsnet.events.requests.shares;

import de.craftsblock.craftscore.event.Event;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;

/**
 * Represents an event that is triggered when a share file has been loaded.
 * This event provides information about the loaded file.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1
 * @see de.craftsblock.craftscore.event.Event
 * @since CraftsNet-2.3.2
 */
public class ShareFileLoadedEvent extends Event {

    private static final FileNameMap contentTypes = URLConnection.getFileNameMap();

    private File file;
    private String contentType;

    /**
     * Creates a new ShareFileLoadedEvent with the specified loaded file.
     *
     * @param file The loaded file associated with this event.
     */
    public ShareFileLoadedEvent(@Nullable File file) {
        this.file = file;
    }

    /**
     * Gets the loaded file associated with this event.
     *
     * @return The loaded file.
     */
    @Nullable
    public File getFile() {
        return file;
    }

    /**
     * Sets the loaded file associated with this event.
     *
     * @param file The new loaded file.
     */
    public void setFile(@Nullable File file) {
        this.file = file;
    }

    /**
     * Determines the content type of the current file.
     *
     * @return The content type of the file.
     */
    @Nullable
    public String getRawContentType() {
        if (!file.exists()) return null;
        return contentTypes.getContentTypeFor(file.getName());
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
