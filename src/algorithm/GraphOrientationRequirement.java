package algorithm;

/**
 * Declares what graph orientation an algorithm expects.
 */
public enum GraphOrientationRequirement {
    /** Algorithm expects directed semantics. */
    DIRECTED_ONLY,
    /** Algorithm expects undirected semantics. */
    UNDIRECTED_ONLY,
    /** Algorithm is valid in both directed and undirected settings. */
    EITHER
}

