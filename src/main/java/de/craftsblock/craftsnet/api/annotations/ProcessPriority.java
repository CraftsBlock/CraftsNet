package de.craftsblock.craftsnet.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define the processing priority of a method.
 * Methods annotated with this annotation can specify a priority level
 * to influence their execution order in a prioritized processing system.
 */
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
        LOWEST,
        LOW,
        NORMAL,
        HIGH,
        HIGHEST,
        MONITOR;

        /**
         * Returns the next priority level in the sequence.
         *
         * @return The next priority level in the sequence.
         */
        public Priority next() {
            return Priority.next(this);
        }

        /**
         * Returns the priority level that follows the given priority level.
         *
         * @param current The current priority level.
         * @return The next priority level in the sequence after the given one.
         */
        public static Priority next(Priority current) {
            return switch (current) {
                case LOWEST -> LOW;
                case LOW -> NORMAL;
                case NORMAL -> HIGH;
                case HIGH -> HIGHEST;
                case HIGHEST -> MONITOR;
                default -> null;
            };
        }
    }

}
