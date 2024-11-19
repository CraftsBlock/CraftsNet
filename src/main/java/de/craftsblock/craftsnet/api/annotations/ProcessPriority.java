package de.craftsblock.craftsnet.api.annotations;

import java.lang.annotation.*;

/**
 * Annotation to define the processing priority of a method.
 * Methods annotated with this annotation can specify a priority level
 * to influence their execution order in a prioritized processing system.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.0.0
 * @since CraftsNet-3.0.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ProcessPriority {

    /**
     * Specifies the priority level for the annotated method.
     * Default value is Priority.NORMAL.
     *
     * @return The priority level for the annotated method.
     */
    Priority value() default Priority.NORMAL;

    /**
     * Enumeration representing different priority levels.
     * Priorities are defined as LOWEST, LOW, NORMAL, HIGH, HIGHEST, and MONITOR.
     */
    enum Priority {
        /**
         * Indicates the lowest priority. Will be executed first.
         */
        LOWEST,

        /**
         * Indicates a low priority. Will be executed second.
         */
        LOW,

        /**
         * Indicates a normal priority. Will be executed third.
         */
        NORMAL,

        /**
         * Indicates a high priority. Will be executed fourth.
         */
        HIGH,

        /**
         * Indicates the highest priority. Will be executed fifth.
         */
        HIGHEST,

        /**
         * Indicates a monitoring priority. Will be executed last.
         */
        MONITOR

    }

}
