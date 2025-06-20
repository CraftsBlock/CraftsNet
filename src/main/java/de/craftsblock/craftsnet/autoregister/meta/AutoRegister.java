package de.craftsblock.craftsnet.autoregister.meta;

import de.craftsblock.craftsnet.addon.meta.Startup;

import java.lang.annotation.*;

/**
 * The {@link  AutoRegister} is an annotation used to mark classes for automatic registration
 * during the startup process. This annotation can be used in combination with a {@link Startup}
 * value that determines when the auto registration should occur.
 *
 * <p>The default value of the annotation is {@link Startup#ENABLE}, meaning auto registration
 * is performed after the addon has been enabled.</p>
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.1.2
 * @since 3.2.0-SNAPSHOT
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoRegister {

    /**
     * The {@link Startup} value that determines when the auto registration should be performed.
     * The default value is {@link Startup#ENABLE}, meaning auto registration is performed after
     * the addon has been enabled.
     *
     * @return The {@link Startup} value indicating when to enable auto-registration.
     */
    Startup startup() default Startup.ENABLE;

    /**
     * The {@link Instantiate} value that determines which instance type logic should be applied
     * when a new instance of the object is created.
     * The default values is {@link Instantiate#SAME}, meaning auto registration will use the same
     * object rather than creating new ones every time.
     *
     * @return The {@link Instantiate} value indicating which instantiate logic should be used.
     * @since 3.3.2-SNAPSHOT
     */
    Instantiate instantiate() default Instantiate.SAME;

}
