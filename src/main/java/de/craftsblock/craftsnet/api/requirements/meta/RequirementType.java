package de.craftsblock.craftsnet.api.requirements.meta;

/**
 * Represents the different types of requirements that can be defined for annotations.
 *
 * <p>This enum is used in conjunction with {@link RequirementMeta} to specify how a requirement
 * should be processed. Each type has a distinct purpose and influences the behavior of
 * requirement handling:</p>
 *
 * <ul>
 *     <li>{@link #FLAG}: Indicates that the presence of the annotation itself is part of
 *     the requirement, without needing any additional data.</li>
 *     <li>{@link #STORING}: Indicates that the annotation contains specific data which
 *     should be extracted and stored for later use.</li>
 * </ul>
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @version 1.0.0
 * @since 3.1.0-SNAPSHOT
 */
public enum RequirementType {

    /**
     * A requirement type indicating that the presence of the annotation alone
     * is the part of the requirement.
     */
    FLAG,

    /**
     * A requirement type indicating that the annotation holds specific data
     * that must be extracted and stored.
     */
    STORING

}
