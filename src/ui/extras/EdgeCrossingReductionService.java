package ui.extras;

import graph.Edge;
import graph.Vertex;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Heuristic layout improver that reduces straight-line edge crossings.
 *
 * The graph structure is untouched; only vertex positions are optimized.
 */
public final class EdgeCrossingReductionService {

    public Result improveLayout(
            List<Vertex> vertices,
            List<Edge> edges,
            Map<Integer, Point2D.Double> inputPositions
    ) {
        if (vertices == null || vertices.isEmpty()) {
            return new Result(Map.of(), 0, 0, PlanarityStatus.UNKNOWN, "nothing-to-layout");
        }

        Map<Integer, Point2D.Double> current = buildInitialPositions(vertices, inputPositions);
        Point2D.Double originalCentroid = centroid(current);
        int before = countCrossings(edges, current);

        PlanarityStatus assessment = assessPlanarity(vertices, edges);

        if (before == 0 || vertices.size() < 3 || edges.size() < 2) {
            PlanarityStatus status = before == 0 ? PlanarityStatus.PLANAR : assessment;
            return new Result(current, before, before, status, "already-good");
        }

        // If graph looks planar, first try to explicitly search for a zero-crossing embedding.
        if (assessment == PlanarityStatus.PLANAR || assessment == PlanarityStatus.UNKNOWN) {
            SearchOutcome zeroAttempt = runSearch(vertices, edges, current, originalCentroid, true, 4, 0xC0FFEE);
            if (zeroAttempt.bestCrossings == 0) {
                return new Result(zeroAttempt.bestLayout, before, 0, PlanarityStatus.PLANAR, "planar-zero-crossing");
            }
            // If we cannot realize a zero crossing, continue with general minimization.
            current = zeroAttempt.currentLayout;
        }

        SearchOutcome reduced = runSearch(vertices, edges, current, originalCentroid, false, 2, 0xBEEFL);
        PlanarityStatus finalStatus = assessment == PlanarityStatus.NON_PLANAR
                ? PlanarityStatus.NON_PLANAR
                : reduced.bestCrossings == 0 ? PlanarityStatus.PLANAR : PlanarityStatus.UNKNOWN;
        return new Result(reduced.bestLayout, before, reduced.bestCrossings, finalStatus, "heuristic-minimization");
    }

    private SearchOutcome runSearch(
            List<Vertex> vertices,
            List<Edge> edges,
            Map<Integer, Point2D.Double> seed,
            Point2D.Double originalCentroid,
            boolean stopAtZero,
            int restartFactor,
            long randomSeed
    ) {
        Map<Integer, Point2D.Double> globalBest = deepCopy(seed);
        int globalBestCrossings = countCrossings(edges, globalBest);
        Map<Integer, Point2D.Double> currentForNextRestart = deepCopy(seed);

        int restarts = Math.max(1, restartFactor);
        int iterations = Math.min(2200, 220 + vertices.size() * 28 + edges.size() * 2);

        for (int restart = 0; restart < restarts; restart++) {
            Map<Integer, Point2D.Double> current = deepCopy(currentForNextRestart);
            int currentCrossings = countCrossings(edges, current);
            Map<Integer, Point2D.Double> bestLocal = deepCopy(current);
            int bestLocalCrossings = currentCrossings;

            Random random = new Random(randomSeed + restart * 97L);
            double temperature = initialTemperature(current) * (1.0 + 0.35 * restart);

            for (int i = 0; i < iterations; i++) {
                Vertex picked = vertices.get(random.nextInt(vertices.size()));
                Point2D.Double oldPoint = current.get(picked.getId());
                if (oldPoint == null) {
                    continue;
                }

                Point2D.Double trialPoint = new Point2D.Double(
                        oldPoint.x + (random.nextDouble() * 2.0 - 1.0) * temperature,
                        oldPoint.y + (random.nextDouble() * 2.0 - 1.0) * temperature
                );
                current.put(picked.getId(), trialPoint);

                int trialCrossings = countCrossings(edges, current);
                boolean accept;
                if (trialCrossings <= currentCrossings) {
                    accept = true;
                } else {
                    double p = Math.exp((currentCrossings - trialCrossings) / Math.max(1.0, temperature));
                    accept = random.nextDouble() < p;
                }

                if (accept) {
                    currentCrossings = trialCrossings;
                    if (trialCrossings < bestLocalCrossings) {
                        bestLocalCrossings = trialCrossings;
                        bestLocal = deepCopy(current);
                        if (stopAtZero && bestLocalCrossings == 0) {
                            break;
                        }
                    }
                } else {
                    current.put(picked.getId(), oldPoint);
                }

                temperature = Math.max(2.0, temperature * 0.993);
            }

            if (bestLocalCrossings < globalBestCrossings) {
                globalBestCrossings = bestLocalCrossings;
                globalBest = deepCopy(bestLocal);
            }

            if (stopAtZero && globalBestCrossings == 0) {
                break;
            }

            currentForNextRestart = jitterAround(bestLocal, randomSeed + restart * 257L);
        }

        recenterToInputCentroid(globalBest, originalCentroid);
        return new SearchOutcome(globalBest, globalBestCrossings, currentForNextRestart);
    }

    private Map<Integer, Point2D.Double> jitterAround(Map<Integer, Point2D.Double> base, long seed) {
        Random random = new Random(seed);
        Map<Integer, Point2D.Double> out = deepCopy(base);
        for (Point2D.Double point : out.values()) {
            point.x += (random.nextDouble() * 2.0 - 1.0) * 6.0;
            point.y += (random.nextDouble() * 2.0 - 1.0) * 6.0;
        }
        return out;
    }

    private PlanarityStatus assessPlanarity(List<Vertex> vertices, List<Edge> edges) {
        int n = vertices.size();
        int m = countUndirectedSimpleEdges(edges);
        if (n < 3) {
            return PlanarityStatus.PLANAR;
        }

        // Euler necessary condition for simple planar graphs.
        if (m > 3 * n - 6) {
            return PlanarityStatus.NON_PLANAR;
        }

        if (isBipartite(vertices, edges) && m > 2 * n - 4) {
            return PlanarityStatus.NON_PLANAR;
        }

        // Trees/forests are planar.
        if (m <= n - 1) {
            return PlanarityStatus.PLANAR;
        }

        // Remaining cases are not classified by these fast tests.
        return PlanarityStatus.UNKNOWN;
    }

    private int countUndirectedSimpleEdges(List<Edge> edges) {
        java.util.HashSet<Long> unique = new java.util.HashSet<>();
        for (Edge edge : edges) {
            int a = edge.getSource().getId();
            int b = edge.getDestination().getId();
            if (a == b) {
                continue;
            }
            int min = Math.min(a, b);
            int max = Math.max(a, b);
            long key = (((long) min) << 32) | (max & 0xffffffffL);
            unique.add(key);
        }
        return unique.size();
    }

    private boolean isBipartite(List<Vertex> vertices, List<Edge> edges) {
        Map<Integer, List<Integer>> adjacency = new LinkedHashMap<>();
        for (Vertex vertex : vertices) {
            adjacency.put(vertex.getId(), new ArrayList<>());
        }
        for (Edge edge : edges) {
            int a = edge.getSource().getId();
            int b = edge.getDestination().getId();
            if (a == b || !adjacency.containsKey(a) || !adjacency.containsKey(b)) {
                continue;
            }
            adjacency.get(a).add(b);
            adjacency.get(b).add(a);
        }

        Map<Integer, Integer> color = new LinkedHashMap<>();
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
        for (Vertex vertex : vertices) {
            int id = vertex.getId();
            if (color.containsKey(id)) {
                continue;
            }
            color.put(id, 0);
            queue.add(id);
            while (!queue.isEmpty()) {
                int current = queue.removeFirst();
                int currentColor = color.get(current);
                for (Integer neighbor : adjacency.getOrDefault(current, List.of())) {
                    Integer seen = color.get(neighbor);
                    if (seen == null) {
                        color.put(neighbor, 1 - currentColor);
                        queue.addLast(neighbor);
                    } else if (seen == currentColor) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** Counts pairwise straight-line crossings for edges that do not share endpoints. */
    public int countCrossings(List<Edge> edges, Map<Integer, Point2D.Double> positions) {
        if (edges == null || edges.size() < 2) {
            return 0;
        }
        int crossings = 0;
        for (int i = 0; i < edges.size(); i++) {
            Edge first = edges.get(i);
            Point2D.Double a = positions.get(first.getSource().getId());
            Point2D.Double b = positions.get(first.getDestination().getId());
            if (a == null || b == null) {
                continue;
            }
            for (int j = i + 1; j < edges.size(); j++) {
                Edge second = edges.get(j);
                if (sharesEndpoint(first, second)) {
                    continue;
                }
                Point2D.Double c = positions.get(second.getSource().getId());
                Point2D.Double d = positions.get(second.getDestination().getId());
                if (c == null || d == null) {
                    continue;
                }
                if (segmentsProperlyIntersect(a, b, c, d)) {
                    crossings++;
                }
            }
        }
        return crossings;
    }

    private boolean sharesEndpoint(Edge first, Edge second) {
        int a = first.getSource().getId();
        int b = first.getDestination().getId();
        int c = second.getSource().getId();
        int d = second.getDestination().getId();
        return a == c || a == d || b == c || b == d;
    }

    private boolean segmentsProperlyIntersect(Point2D.Double a, Point2D.Double b, Point2D.Double c, Point2D.Double d) {
        double o1 = orientation(a, b, c);
        double o2 = orientation(a, b, d);
        double o3 = orientation(c, d, a);
        double o4 = orientation(c, d, b);
        return (o1 * o2 < 0.0) && (o3 * o4 < 0.0);
    }

    private double orientation(Point2D.Double p, Point2D.Double q, Point2D.Double r) {
        return (q.x - p.x) * (r.y - p.y) - (q.y - p.y) * (r.x - p.x);
    }

    private Map<Integer, Point2D.Double> buildInitialPositions(
            List<Vertex> vertices,
            Map<Integer, Point2D.Double> inputPositions
    ) {
        Map<Integer, Point2D.Double> result = new LinkedHashMap<>();
        boolean missing = false;
        for (Vertex vertex : vertices) {
            Point2D.Double p = inputPositions.get(vertex.getId());
            if (p == null) {
                missing = true;
                break;
            }
            result.put(vertex.getId(), new Point2D.Double(p.x, p.y));
        }
        if (!missing) {
            return result;
        }

        double cx = 420.0;
        double cy = 300.0;
        double radius = Math.max(120.0, 24.0 * vertices.size());
        for (int i = 0; i < vertices.size(); i++) {
            double angle = (2.0 * Math.PI * i) / vertices.size();
            Vertex v = vertices.get(i);
            result.put(v.getId(), new Point2D.Double(
                    cx + radius * Math.cos(angle),
                    cy + radius * Math.sin(angle)
            ));
        }
        return result;
    }

    private double initialTemperature(Map<Integer, Point2D.Double> positions) {
        if (positions.isEmpty()) {
            return 40.0;
        }
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Point2D.Double p : positions.values()) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }
        double span = Math.max(maxX - minX, maxY - minY);
        return Math.max(30.0, span * 0.22);
    }

    private void recenterToInputCentroid(Map<Integer, Point2D.Double> candidate, Point2D.Double referenceCentroid) {
        Point2D.Double candidateCentroid = centroid(candidate);
        double dx = referenceCentroid.x - candidateCentroid.x;
        double dy = referenceCentroid.y - candidateCentroid.y;
        for (Point2D.Double p : candidate.values()) {
            p.x += dx;
            p.y += dy;
        }
    }

    private Point2D.Double centroid(Map<Integer, Point2D.Double> positions) {
        if (positions.isEmpty()) {
            return new Point2D.Double(0.0, 0.0);
        }
        double sx = 0.0;
        double sy = 0.0;
        for (Point2D.Double p : positions.values()) {
            sx += p.x;
            sy += p.y;
        }
        return new Point2D.Double(sx / positions.size(), sy / positions.size());
    }

    private Map<Integer, Point2D.Double> deepCopy(Map<Integer, Point2D.Double> source) {
        Map<Integer, Point2D.Double> copy = new LinkedHashMap<>();
        for (Map.Entry<Integer, Point2D.Double> entry : source.entrySet()) {
            Point2D.Double p = entry.getValue();
            copy.put(entry.getKey(), new Point2D.Double(p.x, p.y));
        }
        return copy;
    }

    /** Immutable layout optimization result. */
    public static final class Result {
        private final Map<Integer, Point2D.Double> positions;
        private final int crossingsBefore;
        private final int crossingsAfter;
        private final PlanarityStatus planarityStatus;
        private final String strategy;

        private Result(
                Map<Integer, Point2D.Double> positions,
                int crossingsBefore,
                int crossingsAfter,
                PlanarityStatus planarityStatus,
                String strategy
        ) {
            this.positions = positions;
            this.crossingsBefore = crossingsBefore;
            this.crossingsAfter = crossingsAfter;
            this.planarityStatus = planarityStatus;
            this.strategy = strategy;
        }

        public Map<Integer, Point2D.Double> getPositions() {
            return positions;
        }

        public int getCrossingsBefore() {
            return crossingsBefore;
        }

        public int getCrossingsAfter() {
            return crossingsAfter;
        }

        public PlanarityStatus getPlanarityStatus() {
            return planarityStatus;
        }

        public String getStrategy() {
            return strategy;
        }

        public boolean improved() {
            return crossingsAfter < crossingsBefore;
        }
    }

    public enum PlanarityStatus {
        PLANAR,
        NON_PLANAR,
        UNKNOWN
    }

    private record SearchOutcome(
            Map<Integer, Point2D.Double> bestLayout,
            int bestCrossings,
            Map<Integer, Point2D.Double> currentLayout
    ) {
    }
}



