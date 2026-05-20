package ui.extras;

import java.awt.Color;

/**
 * Maps coloring class indices to distinct UI-friendly RGB colors.
 */
public final class ColorClassPalette {
    private static final Color[] BASE = {
            new Color(66, 133, 244),
            new Color(219, 68, 55),
            new Color(244, 180, 0),
            new Color(15, 157, 88),
            new Color(171, 71, 188),
            new Color(0, 172, 193),
            new Color(255, 112, 67),
            new Color(124, 179, 66),
            new Color(92, 107, 192),
            new Color(141, 110, 99)
    };

    private ColorClassPalette() {
    }

    public static int rgbFor(int colorClass) {
        if (colorClass < BASE.length) {
            return BASE[colorClass].getRGB();
        }
        float hue = (float) ((colorClass * 0.6180339887498949) % 1.0);
        return Color.getHSBColor(hue, 0.60f, 0.90f).getRGB();
    }
}

