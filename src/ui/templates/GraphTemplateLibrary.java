package ui.templates;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Large built-in library of premade graph templates.
 *
 * This class is effectively a static graph-factory catalog. The dialog layer uses it to
 * populate template thumbnails, while the editor controller converts chosen templates into
 * live graph state.
 *
 * Related files:
 * - {@link ui.dialogs.PremadeGraphsDialog}
 * - {@link ui.GraphEditorController#loadGraphTemplate(ui.templates.GraphTemplate)}
 * - {@code project roadmap.md}
 */
public final class GraphTemplateLibrary {
    private static final List<GraphTemplate> TEMPLATES = buildTemplates();

    private GraphTemplateLibrary() {
    }

    /** @return all built-in templates. */
    public static List<GraphTemplate> allTemplates() {
        return TEMPLATES;
    }

    /** Returns templates filtered by library category. */
    public static List<GraphTemplate> byType(GraphTemplateType type) {
        if (type == null || type == GraphTemplateType.ALL_GRAPHS) {
            return TEMPLATES;
        }
        List<GraphTemplate> filtered = new ArrayList<>();
        for (GraphTemplate template : TEMPLATES) {
            if (template.getType() == type) {
                filtered.add(template);
            }
        }
        return filtered;
    }

    /** Builds the full static template catalog once at class-load time. */
    private static List<GraphTemplate> buildTemplates() {
        List<GraphTemplate> items = new ArrayList<>();

        items.add(path("Path 5", 5));
        items.add(path("Path 8", 8));
        items.add(cycle("Cycle 6", 6));
        items.add(cycle("Cycle 10", 10));
        items.add(star("Star 8", 8));
        items.add(complete("Complete K5", 5));
        items.add(complete("Complete K7", 7));

        items.add(binaryTree("Binary Tree 15", 4));
        items.add(binaryTree("Binary Tree 31", 5));
        items.add(treeFromEdges("Balanced Ternary Tree", 13, List.of(
                e(0,1),e(0,2),e(0,3), e(1,4),e(1,5),e(1,6), e(2,7),e(2,8),e(2,9), e(3,10),e(3,11),e(3,12)
        )));
        items.add(treeFromEdges("Caterpillar Tree", 12, List.of(
                e(0,1),e(1,2),e(2,3),e(3,4),e(4,5), e(1,6),e(2,7),e(2,8),e(3,9),e(4,10),e(4,11)
        )));

        items.add(layeredDag("Layered DAG 3x4", 3, 4));
        items.add(layeredDag("Layered DAG 4x5", 4, 5));
        items.add(dagLong("Dependency DAG 12"));
        items.add(dagDiamond("Diamond DAG"));
        items.add(dagLong("Pipeline DAG 16", 16));

        items.add(weightedWheel("Weighted Wheel 10", 10));
        items.add(weightedMesh("Weighted Mesh 4x4", 4, 4));
        items.add(weightedSparse("Weighted Sparse 20", 20));
        items.add(negativeCycleDemo("Negative Cycle Demo"));

        items.add(grid("Grid 4x4", 4, 4));
        items.add(grid("Grid 6x6", 6, 6));
        items.add(grid("Rect Grid 8x4", 8, 4));

        items.add(bipartite("Bipartite K3,5", 3, 5));
        items.add(bipartite("Bipartite K6,6", 6, 6));
        items.add(bipartite("Bipartite K4,8", 4, 8));

        items.add(flowNetwork("Flow Network Small"));
        items.add(withType(flowNetworkLarge("Flow Network Large"), GraphTemplateType.FLOW));
        items.add(withType(layeredDag("Transport Layer Network", 5, 4), GraphTemplateType.FLOW));

        items.add(withType(weightedWheel("Wheel 12", 12), GraphTemplateType.SPECIAL));
        items.add(lollipop("Lollipop Graph", 6, 7));
        items.add(barbell("Barbell Graph", 5, 5));
        items.add(hypercubeQ3("Hypercube Q3"));
        items.add(hypercubeQ4Projection("Hypercube Q4 Projection"));

        return List.copyOf(items);
    }

    /** Creates a simple path graph laid out left-to-right. */
    private static GraphTemplate path(String name, int n) {
        Map<Integer, Point2D.Double> pos = new LinkedHashMap<>();
        List<GraphTemplate.TemplateEdge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double x = n == 1 ? 0.5 : 0.08 + 0.84 * i / (n - 1.0);
            pos.put(i, new Point2D.Double(x, 0.5));
            if (i > 0) {
                edges.add(new GraphTemplate.TemplateEdge(i - 1, i, 1, 0));
            }
        }
        return new GraphTemplate(name, GraphTemplateType.BASICS, pos, edges);
    }

    /** Creates a directed cycle laid out on a circle. */
    private static GraphTemplate cycle(String name, int n) {
        Map<Integer, Point2D.Double> pos = circlePositions(n, 0.5, 0.5, 0.38);
        List<GraphTemplate.TemplateEdge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            edges.add(new GraphTemplate.TemplateEdge(i, (i + 1) % n, 1, 0));
        }
        return new GraphTemplate(name, GraphTemplateType.BASICS, pos, edges);
    }

    /** Creates a star-shaped graph with one center and many leaves. */
    private static GraphTemplate star(String name, int leaves) {
        Map<Integer, Point2D.Double> pos = new LinkedHashMap<>();
        List<GraphTemplate.TemplateEdge> edges = new ArrayList<>();
        pos.put(0, new Point2D.Double(0.5, 0.5));
        for (int i = 1; i <= leaves; i++) {
            double angle = 2 * Math.PI * (i - 1) / leaves;
            pos.put(i, new Point2D.Double(0.5 + 0.38 * Math.cos(angle), 0.5 + 0.38 * Math.sin(angle)));
            edges.add(new GraphTemplate.TemplateEdge(0, i, 1, 0));
            edges.add(new GraphTemplate.TemplateEdge(i, 0, 1, 0));
        }
        return new GraphTemplate(name, GraphTemplateType.BASICS, pos, edges);
    }

    /** Creates a complete directed graph template. */
    private static GraphTemplate complete(String name, int n) {
        Map<Integer, Point2D.Double> pos = circlePositions(n, 0.5, 0.5, 0.38);
        List<GraphTemplate.TemplateEdge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    edges.add(new GraphTemplate.TemplateEdge(i, j, 1, 0));
                }
            }
        }
        return new GraphTemplate(name, GraphTemplateType.BASICS, pos, edges);
    }

    /** Creates a balanced binary tree by generating parent-child edges first. */
    private static GraphTemplate binaryTree(String name, int levels) {
        int n = (1 << levels) - 1;
        List<GraphTemplate.TemplateEdge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int l = 2 * i + 1;
            int r = 2 * i + 2;
            if (l < n) {
                edges.add(new GraphTemplate.TemplateEdge(i, l, 1, 0));
            }
            if (r < n) {
                edges.add(new GraphTemplate.TemplateEdge(i, r, 1, 0));
            }
        }
        return treeFromEdges(name, n, edges);
    }

    /**
     * Generic tree layout helper used by specific tree generators.
     */
    private static GraphTemplate treeFromEdges(String name, int n, List<GraphTemplate.TemplateEdge> edges) {
        Map<Integer, Point2D.Double> pos = new LinkedHashMap<>();
        int perRow = (int) Math.ceil(Math.sqrt(n));
        for (int i = 0; i < n; i++) {
            int row = i / perRow;
            int col = i % perRow;
            pos.put(i, new Point2D.Double(0.08 + 0.84 * col / Math.max(1, perRow - 1.0), 0.12 + 0.76 * row / Math.max(1, perRow - 1.0)));
        }
        return new GraphTemplate(name, GraphTemplateType.TREES, pos, edges);
    }

    /** Builds a layered DAG template useful for topological and shortest-path demos. */
    private static GraphTemplate layeredDag(String name, int layers, int perLayer) {
        Map<Integer, Point2D.Double> pos = new LinkedHashMap<>();
        List<GraphTemplate.TemplateEdge> edges = new ArrayList<>();
        int id = 0;
        int[][] ids = new int[layers][perLayer];
        for (int layer = 0; layer < layers; layer++) {
            for (int j = 0; j < perLayer; j++) {
                ids[layer][j] = id;
                pos.put(id, new Point2D.Double(0.1 + 0.8 * layer / Math.max(1, layers - 1.0), 0.12 + 0.76 * j / Math.max(1, perLayer - 1.0)));
                id++;
            }
        }
        for (int layer = 0; layer < layers - 1; layer++) {
            for (int a = 0; a < perLayer; a++) {
                for (int b = 0; b < perLayer; b++) {
                    if (Math.abs(a - b) <= 1 || (a + b) % 3 == 0) {
                        edges.add(new GraphTemplate.TemplateEdge(ids[layer][a], ids[layer + 1][b], 1 + (a + b) % 9, 0));
                    }
                }
            }
        }
        return new GraphTemplate(name, GraphTemplateType.DAGS, pos, edges);
    }

    private static GraphTemplate dagLong(String name) {
        return dagLong(name, 12);
    }

    private static GraphTemplate dagLong(String name, int n) {
        Map<Integer, Point2D.Double> pos = new LinkedHashMap<>();
        List<GraphTemplate.TemplateEdge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            pos.put(i, new Point2D.Double(0.08 + 0.84 * i / Math.max(1, n - 1.0), 0.2 + 0.6 * ((i % 3) / 2.0)));
            if (i + 1 < n) edges.add(new GraphTemplate.TemplateEdge(i, i + 1, 1, 0));
            if (i + 3 < n) edges.add(new GraphTemplate.TemplateEdge(i, i + 3, 2, 0));
        }
        return new GraphTemplate(name, GraphTemplateType.DAGS, pos, edges);
    }

    private static GraphTemplate dagDiamond(String name) {
        Map<Integer, Point2D.Double> pos = Map.of(
                0, new Point2D.Double(0.1, 0.5),
                1, new Point2D.Double(0.35, 0.25),
                2, new Point2D.Double(0.35, 0.75),
                3, new Point2D.Double(0.6, 0.2),
                4, new Point2D.Double(0.6, 0.5),
                5, new Point2D.Double(0.6, 0.8),
                6, new Point2D.Double(0.9, 0.5)
        );
        List<GraphTemplate.TemplateEdge> edges = List.of(
                e(0,1),e(0,2),e(1,3),e(1,4),e(2,4),e(2,5),e(3,6),e(4,6),e(5,6)
        );
        return new GraphTemplate(name, GraphTemplateType.DAGS, pos, edges);
    }

    private static GraphTemplate weightedWheel(String name, int n) {
        Map<Integer, Point2D.Double> pos = new LinkedHashMap<>();
        List<GraphTemplate.TemplateEdge> edges = new ArrayList<>();
        pos.put(0, new Point2D.Double(0.5, 0.5));
        for (int i = 1; i < n; i++) {
            double angle = 2 * Math.PI * (i - 1) / (n - 1);
            pos.put(i, new Point2D.Double(0.5 + 0.38 * Math.cos(angle), 0.5 + 0.38 * Math.sin(angle)));
            edges.add(new GraphTemplate.TemplateEdge(0, i, 1 + (i % 9), 0));
            edges.add(new GraphTemplate.TemplateEdge(i, 0, 1 + ((i + 3) % 9), 0));
            if (i > 1) edges.add(new GraphTemplate.TemplateEdge(i - 1, i, 2 + (i % 7), 0));
        }
        edges.add(new GraphTemplate.TemplateEdge(n - 1, 1, 3, 0));
        return new GraphTemplate(name, GraphTemplateType.WEIGHTED, pos, edges);
    }

    private static GraphTemplate weightedMesh(String name, int rows, int cols) {
        Map<Integer, Point2D.Double> pos = new LinkedHashMap<>();
        List<GraphTemplate.TemplateEdge> edges = new ArrayList<>();
        int id = 0;
        int[][] ids = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                ids[r][c] = id;
                pos.put(id, new Point2D.Double(0.1 + 0.8 * c / Math.max(1, cols - 1.0), 0.12 + 0.76 * r / Math.max(1, rows - 1.0)));
                id++;
            }
        }
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (c + 1 < cols) edges.add(new GraphTemplate.TemplateEdge(ids[r][c], ids[r][c + 1], 1 + (r + c) % 9, 0));
                if (r + 1 < rows) edges.add(new GraphTemplate.TemplateEdge(ids[r][c], ids[r + 1][c], 1 + (r * 2 + c) % 9, 0));
            }
        }
        return new GraphTemplate(name, GraphTemplateType.WEIGHTED, pos, edges);
    }

    private static GraphTemplate weightedSparse(String name, int n) {
        Map<Integer, Point2D.Double> pos = circlePositions(n, 0.5, 0.5, 0.4);
        List<GraphTemplate.TemplateEdge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int j = (i + 3) % n;
            int k = (i + 7) % n;
            edges.add(new GraphTemplate.TemplateEdge(i, j, 1 + (i % 9), 0));
            if (i % 2 == 0) edges.add(new GraphTemplate.TemplateEdge(i, k, 2 + (i % 8), 0));
        }
        return new GraphTemplate(name, GraphTemplateType.WEIGHTED, pos, edges);
    }

    private static GraphTemplate negativeCycleDemo(String name) {
        Map<Integer, Point2D.Double> pos = Map.of(
                0, new Point2D.Double(0.2, 0.5),
                1, new Point2D.Double(0.45, 0.25),
                2, new Point2D.Double(0.45, 0.75),
                3, new Point2D.Double(0.75, 0.5)
        );
        List<GraphTemplate.TemplateEdge> edges = List.of(
                new GraphTemplate.TemplateEdge(0,1,2,0),
                new GraphTemplate.TemplateEdge(1,2,-7,0),
                new GraphTemplate.TemplateEdge(2,0,2,0),
                new GraphTemplate.TemplateEdge(1,3,3,0),
                new GraphTemplate.TemplateEdge(0,3,8,0)
        );
        return new GraphTemplate(name, GraphTemplateType.WEIGHTED, pos, edges);
    }

    /** Creates a grid graph with optional rectangular layout. */
    private static GraphTemplate grid(String name, int cols, int rows) {
        Map<Integer, Point2D.Double> pos = new LinkedHashMap<>();
        List<GraphTemplate.TemplateEdge> edges = new ArrayList<>();
        int id = 0;
        int[][] ids = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                ids[r][c] = id;
                pos.put(id, new Point2D.Double(0.08 + 0.84 * c / Math.max(1, cols - 1.0), 0.08 + 0.84 * r / Math.max(1, rows - 1.0)));
                id++;
            }
        }
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (c + 1 < cols) edges.add(e(ids[r][c], ids[r][c + 1]));
                if (r + 1 < rows) edges.add(e(ids[r][c], ids[r + 1][c]));
            }
        }
        return new GraphTemplate(name, GraphTemplateType.GRIDS, pos, edges);
    }

    private static GraphTemplate bipartite(String name, int left, int right) {
        Map<Integer, Point2D.Double> pos = new LinkedHashMap<>();
        List<GraphTemplate.TemplateEdge> edges = new ArrayList<>();
        int id = 0;
        for (int i = 0; i < left; i++) {
            pos.put(id++, new Point2D.Double(0.2, 0.1 + 0.8 * i / Math.max(1, left - 1.0)));
        }
        for (int i = 0; i < right; i++) {
            pos.put(id++, new Point2D.Double(0.8, 0.1 + 0.8 * i / Math.max(1, right - 1.0)));
        }
        for (int l = 0; l < left; l++) {
            for (int r = 0; r < right; r++) {
                if ((l + r) % 2 == 0 || (l * 3 + r) % 5 == 0) {
                    edges.add(e(l, left + r));
                }
            }
        }
        return new GraphTemplate(name, GraphTemplateType.BIPARTITE, pos, edges);
    }

    private static GraphTemplate flowNetwork(String name) {
        Map<Integer, Point2D.Double> pos = Map.of(
                0, new Point2D.Double(0.08, 0.5),
                1, new Point2D.Double(0.3, 0.2),
                2, new Point2D.Double(0.3, 0.8),
                3, new Point2D.Double(0.55, 0.2),
                4, new Point2D.Double(0.55, 0.8),
                5, new Point2D.Double(0.8, 0.5),
                6, new Point2D.Double(0.94, 0.5)
        );
        List<GraphTemplate.TemplateEdge> edges = List.of(
                new GraphTemplate.TemplateEdge(0,1,16,0), new GraphTemplate.TemplateEdge(0,2,13,0),
                new GraphTemplate.TemplateEdge(1,3,12,0), new GraphTemplate.TemplateEdge(2,1,4,0),
                new GraphTemplate.TemplateEdge(2,4,14,0), new GraphTemplate.TemplateEdge(3,2,9,0),
                new GraphTemplate.TemplateEdge(3,5,20,0), new GraphTemplate.TemplateEdge(4,3,7,0),
                new GraphTemplate.TemplateEdge(4,5,4,0), new GraphTemplate.TemplateEdge(5,6,99,0)
        );
        return new GraphTemplate(name, GraphTemplateType.FLOW, pos, edges);
    }

    private static GraphTemplate flowNetworkLarge(String name) {
        return layeredDag(name, 6, 5);
    }

    private static GraphTemplate wheel(String name, int n) {
        return withType(weightedWheel(name, n), GraphTemplateType.SPECIAL);
    }

    private static GraphTemplate lollipop(String name, int clique, int tail) {
        GraphTemplate complete = complete(name, clique);
        Map<Integer, Point2D.Double> pos = new LinkedHashMap<>(complete.getPositions());
        List<GraphTemplate.TemplateEdge> edges = new ArrayList<>(complete.getEdges());
        int start = clique;
        for (int i = 0; i < tail; i++) {
            int id = start + i;
            pos.put(id, new Point2D.Double(0.55 + 0.35 * i / Math.max(1, tail - 1.0), 0.5));
            if (i == 0) edges.add(e(0, id));
            if (i > 0) edges.add(e(id - 1, id));
        }
        return new GraphTemplate(name, GraphTemplateType.SPECIAL, pos, edges);
    }

    private static GraphTemplate barbell(String name, int leftClique, int rightClique) {
        Map<Integer, Point2D.Double> pos = new LinkedHashMap<>();
        List<GraphTemplate.TemplateEdge> edges = new ArrayList<>();
        for (int i = 0; i < leftClique; i++) {
            double angle = 2 * Math.PI * i / leftClique;
            pos.put(i, new Point2D.Double(0.25 + 0.16 * Math.cos(angle), 0.5 + 0.24 * Math.sin(angle)));
        }
        int offset = leftClique;
        for (int i = 0; i < rightClique; i++) {
            double angle = 2 * Math.PI * i / rightClique;
            pos.put(offset + i, new Point2D.Double(0.75 + 0.16 * Math.cos(angle), 0.5 + 0.24 * Math.sin(angle)));
        }
        for (int i = 0; i < leftClique; i++) {
            for (int j = 0; j < leftClique; j++) if (i != j) edges.add(e(i, j));
        }
        for (int i = 0; i < rightClique; i++) {
            for (int j = 0; j < rightClique; j++) if (i != j) edges.add(e(offset + i, offset + j));
        }
        edges.add(e(0, offset));
        return new GraphTemplate(name, GraphTemplateType.SPECIAL, pos, edges);
    }

    private static GraphTemplate hypercubeQ3(String name) {
        Map<Integer, Point2D.Double> pos = Map.of(
                0,new Point2D.Double(0.2,0.2),1,new Point2D.Double(0.4,0.2),2,new Point2D.Double(0.2,0.4),3,new Point2D.Double(0.4,0.4),
                4,new Point2D.Double(0.6,0.4),5,new Point2D.Double(0.8,0.4),6,new Point2D.Double(0.6,0.6),7,new Point2D.Double(0.8,0.6)
        );
        List<GraphTemplate.TemplateEdge> edges = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int b = 0; b < 3; b++) {
                int j = i ^ (1 << b);
                if (i < j) edges.add(e(i, j));
            }
        }
        return new GraphTemplate(name, GraphTemplateType.SPECIAL, pos, edges);
    }

    private static GraphTemplate hypercubeQ4Projection(String name) {
        int n = 16;
        Map<Integer, Point2D.Double> pos = circlePositions(n, 0.5, 0.5, 0.4);
        List<GraphTemplate.TemplateEdge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int b = 0; b < 4; b++) {
                int j = i ^ (1 << b);
                if (i < j) edges.add(e(i, j));
            }
        }
        return new GraphTemplate(name, GraphTemplateType.SPECIAL, pos, edges);
    }

    /** Utility for generating circular coordinates used by several templates. */
    private static Map<Integer, Point2D.Double> circlePositions(int n, double cx, double cy, double r) {
        Map<Integer, Point2D.Double> pos = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            pos.put(i, new Point2D.Double(cx + r * Math.cos(a), cy + r * Math.sin(a)));
        }
        return pos;
    }

    private static GraphTemplate.TemplateEdge e(int s, int d) {
        return new GraphTemplate.TemplateEdge(s, d, 1, 0);
    }

    private static GraphTemplate.TemplateEdge e(int s, int d, int w) {
        return new GraphTemplate.TemplateEdge(s, d, w, 0);
    }

    private static GraphTemplate withType(GraphTemplate template, GraphTemplateType type) {
        return new GraphTemplate(template.getName(), type, template.getPositions(), template.getEdges());
    }
}



