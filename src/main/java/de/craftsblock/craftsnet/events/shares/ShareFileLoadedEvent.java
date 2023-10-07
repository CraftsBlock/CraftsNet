package de.craftsblock.craftsnet.events.shares;

import de.craftsblock.craftscore.event.Event;
import org.apache.tika.Tika;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * Represents an event that is triggered when a share file has been loaded.
 * This event provides information about the loaded file.
 *
 * @author CraftsBlock
 * @version 1.1
 * @see de.craftsblock.craftscore.event.Event
 * @since 2.3.2
 */
public class ShareFileLoadedEvent extends Event {

    private File file;
    private String contentType;
    private final Tika tika;

    /**
     * Creates a new ShareFileLoadedEvent with the specified loaded file.
     *
     * @param file The loaded file associated with this event.
     * @param tika The tika class for determining the content type
     */
    public ShareFileLoadedEvent(@Nullable File file, @NotNull Tika tika) {
        this.file = file;
        this.tika = tika;
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
     * @throws IOException if the loading of the content type is not completed properly.
     */
    @Nullable
    public String getRawContentType() throws IOException {
        if (!file.exists()) return null;
        return tika.detect(file);
    }

    /**
     * Returns the {@link ShareFileLoadedEvent#contentType} if it is not null, otherwise the content type of the file.
     *
     * @return The content type of the file.
     * @throws IOException if the loading of the content type is not completed properly.
     */
    @Nullable
    public String getContentType() throws IOException {
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
