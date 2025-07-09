package de.craftsblock.craftsnet.addon;

import de.craftsblock.craftsnet.addon.loaders.AddonLoader;
import de.craftsblock.craftsnet.utils.reflection.ReflectionUtils;

/**
 * An instance of {@link Addon} which is only capable of holding dummy
 * data of an addon that did not specify a valid main class.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.2
 * @see Addon
 * @since 3.3.5-SNAPSHOT
 */
public final class HollowAddon extends Addon {

    /**
     * Constructor which checks the caller class to prevent direct initialization.
     *
     * @throws IllegalStateException If this constructor is instantiated by another class as {@link AddonLoader}.
     */
    public HollowAddon() {
        ReflectionUtils.restrictToCallers(AddonLoader.class);
    }

}
