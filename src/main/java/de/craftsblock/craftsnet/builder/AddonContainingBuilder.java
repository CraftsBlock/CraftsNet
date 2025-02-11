package de.craftsblock.craftsnet.builder;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.Addon;
import de.craftsblock.craftsnet.addon.loaders.AddonLoader;
import de.craftsblock.craftsnet.addon.meta.AddonConfiguration;
import de.craftsblock.craftsnet.logging.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Builder class for configuring the CraftsNet with a set of addons.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.2.4
 * @see CraftsNetBuilder
 * @since 3.1.0-SNAPSHOT
 */
public class AddonContainingBuilder extends CraftsNetBuilder {

    private final Collection<Class<? extends Addon>> addons;

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
     * @return The {@link AddonContainingBuilder} instance.
     */
    public AddonContainingBuilder map(Class<? extends Addon> type, String name) {
        AddonConfiguration.map(type, name);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param args {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public AddonContainingBuilder withArgs(String[] args) {
        super.withArgs(args);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param codeSource {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public AddonContainingBuilder addCodeSource(CodeSource codeSource) {
        super.addCodeSource(codeSource);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param codeSource {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public AddonContainingBuilder removeCodeSource(CodeSource codeSource) {
        super.removeCodeSource(codeSource);
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
     * @param size {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public AddonContainingBuilder withSessionCache(int size) {
        super.withSessionCache(size);
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
     * @param allowed {@inheritDoc}
     * @return {@inheritDoc}
     * @since 3.3.3-SNAPSHOT
     */
    @Override
    public AddonContainingBuilder withApplyResponseEncoding(boolean allowed) {
        super.withApplyResponseEncoding(allowed);
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
     * @param skip {@inheritDoc}
     * @return {@inheritDoc}
     * @since 3.3.3-SNAPSHOT
     */
    @Override
    public AddonContainingBuilder withSkipDefaultRoute(boolean skip) {
        super.withSkipDefaultRoute(skip);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param skip {@inheritDoc}
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withSkipVersionCheck(boolean skip) {
        super.withSkipVersionCheck(skip);
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
     * @param enabled {@inheritDoc}
     * @return {@inheritDoc}
     */
    public AddonContainingBuilder withDebug(boolean enabled) {
        super.withDebug(enabled);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param enabled {@inheritDoc}
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
        for (Class<? extends Addon> addon : addons)
            this.removeCodeSource(addon.getProtectionDomain().getCodeSource());

        return super.build();
    }

    /**
     * Load all the addons that have been registered in the builder.
     *
     * @param craftsNet The instance of CraftsNet that the addons should be registered on.
     */
    @ApiStatus.Internal
    public void loadAddons(CraftsNet craftsNet) {
        List<AddonConfiguration> configurations = new ArrayList<>();

        AddonLoader loader = new AddonLoader(craftsNet);
        for (Class<? extends Addon> addon : this.addons)
            configurations.addAll(AddonConfiguration.of(addon));
        loader.load(new TreeSet<>(configurations).stream().toList());
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
