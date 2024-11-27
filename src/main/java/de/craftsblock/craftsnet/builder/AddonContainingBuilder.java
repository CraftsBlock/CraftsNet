package de.craftsblock.craftsnet.builder;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.loaders.AddonLoader;
import de.craftsblock.craftsnet.addon.meta.AddonConfiguration;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Builder class for configuring the CraftsNet with a set of addons.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @see CraftsNetBuilder
 * @since 3.0.7-SNAPSHOT
 */
public class AddonContainingBuilder extends CraftsNetBuilder {

    private final Collection<Class<? extends Addon>> addons;

    /**
     * Creates a new instance of {@link AddonContainingBuilder} with providing
     * an array containing {@link Addon} classes.
     *
     * @param addons An array containing all {@link Addon} classes which should be loaded.
     */
    @SafeVarargs
    public AddonContainingBuilder(Class<? extends Addon>... addons) {
        this(List.of(addons));
    }

    /**
     * Creates a new instance of {@link AddonContainingBuilder} with providing
     * a collection containing {@link Addon} classes.
     *
     * @param addons A collection containing all {@link Addon} classes which should be loaded.
     */
    public AddonContainingBuilder(Collection<Class<? extends Addon>> addons) {
        this.addons = addons;
    }

    /**
     * Maps an addon class to a custom name.
     *
     * @param type The addon class.
     * @param name The name to associate with the addon class.
     */
    public AddonContainingBuilder map(Class<? extends Addon> type, String name) {
        AddonConfiguration.map(type, name);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param port {@inheritDoc}
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withWebServer(int port) {
        super.withWebServer(port);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param type {@inheritDoc}
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withWebServer(ActivateType type) {
        super.withWebServer(type);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param type {@inheritDoc}
     * @param port {@inheritDoc}
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withWebServer(ActivateType type, int port) {
        super.withWebServer(type, port);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param port {@inheritDoc}
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withWebSocketServer(int port) {
        super.withWebSocketServer(port);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param type {@inheritDoc}
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withWebSocketServer(ActivateType type) {
        super.withWebSocketServer(type);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param type {@inheritDoc}
     * @param port {@inheritDoc}
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withWebSocketServer(ActivateType type, int port) {
        super.withWebSocketServer(type, port);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param type {@inheritDoc}
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withAddonSystem(ActivateType type) {
        super.withAddonSystem(type);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param type {@inheritDoc}
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withCommandSystem(ActivateType type) {
        super.withCommandSystem(type);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param type {@inheritDoc}
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withFileLogger(ActivateType type) {
        super.withFileLogger(type);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param tempFilesOnNormalFileSystem {@inheritDoc}
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withTempFilesOnNormalFileSystem(boolean tempFilesOnNormalFileSystem) {
        super.withTempFilesOnNormalFileSystem(tempFilesOnNormalFileSystem);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param skipVersionCheck {@inheritDoc}
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withSkipVersionCheck(boolean skipVersionCheck) {
        super.withSkipVersionCheck(skipVersionCheck);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param logger {@inheritDoc}
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withCustomLogger(Logger logger) {
        super.withCustomLogger(logger);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param type {@inheritDoc}
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withLogger(ActivateType type) {
        super.withLogger(type);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withDebug(boolean enabled) {
        super.withDebug(enabled);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withSSL(boolean enabled) {
        super.withSSL(enabled);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withLogRotate(@Range(from = 0, to = Long.MAX_VALUE) long logRotate) {
        super.withLogRotate(logRotate);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withoutLogRotate() {
        withLogRotate(0);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public CraftsNet build() throws IOException {
        CraftsNet craftsNet = super.build();
        AddonLoader loader = new AddonLoader(craftsNet);

        List<AddonConfiguration> configurations = new ArrayList<>();
        for (Class<? extends Addon> addon : addons)
            configurations.addAll(AddonConfiguration.of(addon));
        loader.load(new TreeSet<>(configurations).stream().toList());

        return craftsNet;
    }

    /**
     * Gets all the currently registered {@link Addon} classes which should be loaded.
     *
     * @return A {@link Collection} of {@link Addon} classes.
     */
    public Collection<Class<? extends Addon>> getAddons() {
        return addons;
    }

}
