package me.waliedyassen.runescript.config.var.splitarray;

import lombok.Data;
import me.waliedyassen.runescript.type.PrimitiveType;

/**
 * Contains data about a specific config split array property.
 *
 * @author Walied K. Yassen
 */
@Data
public final class ConfigSplitArrayData {

    /**
     * The name of the split array property.
     */
    private final String name;

    /**
     * The opcode of the split array property.
     */
    private final int code;

    /**
     * Whether or not this split array property is required.
     */
    private final boolean required;

    /**
     * The size type of the config split array property.
     */
    private final PrimitiveType sizeType;

    /**
     * The amount of components that are in this data set.
     */
    private final int componentsCount;

    /**
     * The maximum size of the config split array property.
     */
    private final int maxSize;
}
