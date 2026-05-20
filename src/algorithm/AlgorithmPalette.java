package algorithm;

import java.awt.Color;

/** Shared visualization colors for algorithm playback. */
public final class AlgorithmPalette {
    public static final int SCOPE_NEUTRAL = new Color(189, 199, 216).getRGB();
    public static final int DISCOVERED = new Color(79, 195, 247).getRGB();
    public static final int FRONTIER = new Color(255, 202, 40).getRGB();
    public static final int ACTIVE = new Color(255, 112, 67).getRGB();
    public static final int FINISHED = new Color(102, 187, 106).getRGB();
    public static final int ACCEPTED = new Color(67, 160, 71).getRGB();
    public static final int REJECTED = new Color(229, 57, 53).getRGB();
    public static final int ACTIVE_EDGE = new Color(255, 112, 67).getRGB();
    public static final int TREE_EDGE = new Color(66, 165, 245).getRGB();
    public static final int PATH_EDGE = new Color(102, 187, 106).getRGB();

    private AlgorithmPalette() {
    }
}

