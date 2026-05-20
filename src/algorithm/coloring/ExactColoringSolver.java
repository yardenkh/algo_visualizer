package algorithm.coloring;

import java.util.Arrays;

/**
 * Exact minimum-color solver for undirected simple graphs.
 *
 * Uses branch-and-bound with DSATUR-style branching order.
 */
public final class ExactColoringSolver {
    private final boolean[][] adjacency;
    private final int vertexCount;
    private final int[] degree;

    private final int[] workingColors;
    private int[] bestColors;
    private int bestColorCount;

    public ExactColoringSolver(boolean[][] adjacency) {
        this.adjacency = adjacency;
        this.vertexCount = adjacency.length;
        this.degree = new int[vertexCount];
        this.workingColors = new int[vertexCount];
        Arrays.fill(this.workingColors, -1);
        validateAndComputeDegree();
    }

    public ColoringSolution solve() {
        if (vertexCount == 0) {
            return new ColoringSolution(0, new int[0]);
        }

        int[] greedy = greedyUpperBound();
        bestColorCount = 1 + Arrays.stream(greedy).max().orElse(-1);
        bestColors = Arrays.copyOf(greedy, greedy.length);

        branch(0, 0);
        return new ColoringSolution(bestColorCount, bestColors);
    }

    private void branch(int coloredCount, int usedColorCount) {
        if (usedColorCount >= bestColorCount) {
            return;
        }
        if (coloredCount == vertexCount) {
            bestColorCount = usedColorCount;
            bestColors = Arrays.copyOf(workingColors, workingColors.length);
            return;
        }

        int next = pickNextVertex();
        boolean[] forbidden = computeForbiddenColors(next, usedColorCount);

        for (int color = 0; color < usedColorCount; color++) {
            if (forbidden[color]) {
                continue;
            }
            workingColors[next] = color;
            branch(coloredCount + 1, usedColorCount);
            workingColors[next] = -1;
        }

        if (usedColorCount + 1 < bestColorCount) {
            workingColors[next] = usedColorCount;
            branch(coloredCount + 1, usedColorCount + 1);
            workingColors[next] = -1;
        }
    }

    private int pickNextVertex() {
        int bestVertex = -1;
        int bestSaturation = -1;
        int bestDegree = -1;

        for (int v = 0; v < vertexCount; v++) {
            if (workingColors[v] != -1) {
                continue;
            }
            int saturation = computeSaturation(v);
            if (saturation > bestSaturation
                    || (saturation == bestSaturation && degree[v] > bestDegree)) {
                bestVertex = v;
                bestSaturation = saturation;
                bestDegree = degree[v];
            }
        }
        return bestVertex;
    }

    private int computeSaturation(int vertex) {
        boolean[] seen = new boolean[bestColorCount];
        int count = 0;
        for (int other = 0; other < vertexCount; other++) {
            if (!adjacency[vertex][other]) {
                continue;
            }
            int color = workingColors[other];
            if (color >= 0 && !seen[color]) {
                seen[color] = true;
                count++;
            }
        }
        return count;
    }

    private boolean[] computeForbiddenColors(int vertex, int usedColorCount) {
        boolean[] forbidden = new boolean[usedColorCount];
        for (int other = 0; other < vertexCount; other++) {
            if (!adjacency[vertex][other]) {
                continue;
            }
            int color = workingColors[other];
            if (color >= 0 && color < usedColorCount) {
                forbidden[color] = true;
            }
        }
        return forbidden;
    }

    private int[] greedyUpperBound() {
        int[] colors = new int[vertexCount];
        Arrays.fill(colors, -1);
        int usedColorCount = 0;

        for (int colored = 0; colored < vertexCount; colored++) {
            int next = pickGreedyVertex(colors, usedColorCount);
            boolean[] forbidden = new boolean[usedColorCount];
            for (int other = 0; other < vertexCount; other++) {
                if (!adjacency[next][other]) {
                    continue;
                }
                int color = colors[other];
                if (color >= 0) {
                    forbidden[color] = true;
                }
            }

            int chosen = -1;
            for (int c = 0; c < usedColorCount; c++) {
                if (!forbidden[c]) {
                    chosen = c;
                    break;
                }
            }
            if (chosen == -1) {
                chosen = usedColorCount;
                usedColorCount++;
            }
            colors[next] = chosen;
        }
        return colors;
    }

    private int pickGreedyVertex(int[] colors, int usedColorCount) {
        int bestVertex = -1;
        int bestSaturation = -1;
        int bestDegree = -1;
        for (int v = 0; v < vertexCount; v++) {
            if (colors[v] != -1) {
                continue;
            }
            int saturation = computeSaturation(v, colors, usedColorCount);
            if (saturation > bestSaturation
                    || (saturation == bestSaturation && degree[v] > bestDegree)) {
                bestVertex = v;
                bestSaturation = saturation;
                bestDegree = degree[v];
            }
        }
        return bestVertex;
    }

    private int computeSaturation(int vertex, int[] colors, int usedColorCount) {
        boolean[] seen = new boolean[Math.max(1, usedColorCount)];
        int count = 0;
        for (int other = 0; other < vertexCount; other++) {
            if (!adjacency[vertex][other]) {
                continue;
            }
            int color = colors[other];
            if (color >= 0) {
                if (color >= seen.length) {
                    seen = Arrays.copyOf(seen, color + 1);
                }
                if (!seen[color]) {
                    seen[color] = true;
                    count++;
                }
            }
        }
        return count;
    }

    private void validateAndComputeDegree() {
        for (int i = 0; i < vertexCount; i++) {
            if (adjacency[i].length != vertexCount) {
                throw new IllegalArgumentException("Adjacency matrix must be square.");
            }
            if (adjacency[i][i]) {
                throw new IllegalArgumentException("Self-loops are not supported for proper coloring.");
            }
        }

        for (int i = 0; i < vertexCount; i++) {
            for (int j = 0; j < vertexCount; j++) {
                if (adjacency[i][j] != adjacency[j][i]) {
                    throw new IllegalArgumentException("Graph must be undirected for this solver.");
                }
                if (adjacency[i][j]) {
                    degree[i]++;
                }
            }
        }
    }

    /** Immutable exact-coloring result. */
    public static final class ColoringSolution {
        private final int colorCount;
        private final int[] colors;

        private ColoringSolution(int colorCount, int[] colors) {
            this.colorCount = colorCount;
            this.colors = Arrays.copyOf(colors, colors.length);
        }

        public int getColorCount() {
            return colorCount;
        }

        public int[] getColors() {
            return Arrays.copyOf(colors, colors.length);
        }
    }
}

