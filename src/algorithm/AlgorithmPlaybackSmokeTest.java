package algorithm;

import algorithm.apsp.FloydWarshallAlgorithm;
import algorithm.apsp.MatrixMultiplicationApspAlgorithm;
import algorithm.apsp.RecursiveDoublingApspAlgorithm;
import algorithm.general.SeidelAlgorithm;
import algorithm.general.TransitiveClosureAlgorithm;
import algorithm.sssp.BreadthFirstSearchAlgorithm;
import algorithm.sssp.BellmanFordAlgorithm;
import algorithm.sssp.DepthFirstSearchAlgorithm;
import algorithm.sssp.DijkstraShortestPathAlgorithm;
import algorithm.maxflow.DinicAlgorithm;
import algorithm.maxflow.EdmondsKarpAlgorithm;
import algorithm.maxflow.FordFulkersonAlgorithm;
import algorithm.mst.KruskalAlgorithm;
import algorithm.mst.PrimAlgorithm;
import graph.DirectedGraph;

/** Minimal no-dependency smoke tests for algorithm playback generation. */
public class AlgorithmPlaybackSmokeTest {
    public static void main(String[] args) {
        smokeTestBfs();
        smokeTestDfs();
        smokeTestDijkstra();
        smokeTestDijkstraRejectsNegativeWeights();
        smokeTestBellmanFordSubroutineMetadata();
        smokeTestPlaybackController();
        smokeTestSubroutineOverlayLifecycle();
        smokeTestStepAnchoredSubroutineOpening();
        smokeTestMatrixAlgorithmsExposeSnapshots();
        smokeTestRecursiveAlgorithmsExposeStackFrames();
        smokeTestMaximumFlowAlgorithmsExposeResiduals();
        smokeTestMaximumFlowPathStepPrecedesMutation();
        smokeTestMstAlgorithmsExposeAcceptedAndRejectedDecisions();
        System.out.println("Algorithm playback smoke tests passed.");
    }

    private static void smokeTestBfs() {
        AlgorithmRequest request = createSampleRequest();
        GraphAlgorithm algorithm = new BreadthFirstSearchAlgorithm();
        assertValid(algorithm, request);

        AlgorithmResult result = algorithm.run(request);
        assertTrue(result.getSteps().size() > 5, "BFS should produce multiple timeline steps");
        assertTrue(!result.getPseudocodeLines().isEmpty(), "BFS should provide pseudocode lines");
        assertAllStepLinesInRange(result);
        String sourceAnnotation = result.getSteps().get(0).getVertexAnnotations().get(0);
        assertTrue("dist=0".equals(sourceAnnotation), "BFS source should start at distance 0");
    }

    private static void smokeTestDfs() {
        AlgorithmRequest request = createSampleRequest();
        GraphAlgorithm algorithm = new DepthFirstSearchAlgorithm();
        assertValid(algorithm, request);

        AlgorithmResult result = algorithm.run(request);
        assertAllStepLinesInRange(result);
        assertSubroutineMetadataConsistent(result, "DFS");
        assertTrue(result.getSteps().stream().anyMatch(step -> SubroutineLibrary.DFS_VISIT.equals(step.getCallableSubroutineId())),
                "DFS should expose callable DFS-VISIT anchors in the main timeline");
        assertTrue(result.getSteps().stream().anyMatch(step -> step.getTitle().contains("Backtrack")),
                "DFS should contain a backtrack step");
    }

    private static void smokeTestDijkstra() {
        AlgorithmRequest request = createSampleRequest();
        GraphAlgorithm algorithm = new DijkstraShortestPathAlgorithm();
        assertValid(algorithm, request);

        AlgorithmResult result = algorithm.run(request);
        assertAllStepLinesInRange(result);
        String targetAnnotation = result.getSteps().get(result.getSteps().size() - 1).getVertexAnnotations().get(3);
        assertTrue("dist=3".equals(targetAnnotation), "Dijkstra should compute distance 3 to vertex 3");

        java.util.Set<String> relaxInvocationIds = new java.util.LinkedHashSet<>();
        for (AlgorithmStep step : result.getSteps()) {
            if (step.getCallableSubroutineId() != null) {
                relaxInvocationIds.add(step.getCallableSubroutineId());
            }
        }
        assertTrue(relaxInvocationIds.size() >= 2,
                "Dijkstra should expose multiple RELAX invocation-specific subroutine IDs when multiple edges are inspected");
        for (String invocationId : relaxInvocationIds) {
            SubroutineTimeline timeline = result.getSubroutineTimelines().get(invocationId);
            assertTrue(timeline != null, "Each callable RELAX invocation should have a matching subroutine timeline");
            assertTrue(!timeline.getSteps().isEmpty(), "Each RELAX invocation timeline should contain at least one step");
        }
        assertSubroutineMetadataConsistent(result, "Dijkstra");
    }

    private static void smokeTestDijkstraRejectsNegativeWeights() {
        DirectedGraph graph = new DirectedGraph();
        graph.addEdge(0, 1, -5);
        AlgorithmRequest request = new AlgorithmRequest(graph, graph.getVertices(), graph.getEdges(), graph.getVertex(0));
        String validation = new DijkstraShortestPathAlgorithm().validate(request);
        assertTrue(validation != null && validation.contains("non-negative"),
                "Dijkstra should reject negative weights");
    }

    private static void smokeTestPlaybackController() {
        AlgorithmPlaybackController playback = new AlgorithmPlaybackController();
        AlgorithmResult result = new BreadthFirstSearchAlgorithm().run(createSampleRequest());
        playback.start(result);
        assertTrue(playback.hasActiveSession(), "Playback should be active after start");
        playback.stepForward();
        assertTrue(playback.canStepBackward(), "Playback should support stepping backward after moving forward");
        playback.stop();
        assertTrue(!playback.hasActiveSession(), "Playback should stop cleanly");
    }

    private static void smokeTestSubroutineOverlayLifecycle() {
        AlgorithmPlaybackController playback = new AlgorithmPlaybackController();

        AlgorithmStep step0 = simpleStep("Main 0", "before call", 0, "RELAX");
        AlgorithmStep step1 = simpleStep("Main 1", "after call", 1, null);
        AlgorithmStep sub0 = simpleStep("Relax 0", "inspect edge", 0, null);
        AlgorithmStep sub1 = simpleStep("Relax 1", "update distance", 1, null);
        AlgorithmStep alt0 = simpleStep("Alt 0", "other call site", 0, null);

        java.util.Map<String, SubroutineTimeline> subroutines = java.util.Map.of(
                "RELAX",
                new SubroutineTimeline(
                        "RELAX",
                        "RELAX(u,v)",
                        java.util.List.of("RELAX(u, v, w)", "if d[v] > d[u] + w(u,v)", "d[v] <- d[u] + w(u,v)"),
                        java.util.List.of(sub0, sub1)
                ),
                "ALT",
                new SubroutineTimeline(
                        "ALT",
                        "ALT()",
                        java.util.List.of("ALT()"),
                        java.util.List.of(alt0)
                )
        );

        AlgorithmResult base = new AlgorithmResult(
                "Synthetic",
                java.util.List.of("line0", "line1"),
                java.util.List.of(step0, step1),
                false,
                "",
                subroutines,
                java.util.Map.of(0, "RELAX", 1, "ALT")
        );

        playback.start(base);
        assertTrue(playback.canOpenAnySubroutine(), "At least one callable subroutine line should be available for the run");
        assertTrue(playback.canOpenSubroutineAtCurrentStep(), "Callable subroutine should be available at step 0");

        boolean wrongLineOpen = playback.openSubroutineForLine(1);
        assertTrue(!wrongLineOpen,
                "Subroutine opening should be rejected for callable lines that are not the active call site");

        boolean opened = playback.openSubroutineForLine(0);
        assertTrue(opened, "Subroutine overlay should open");
        assertTrue(playback.hasActiveSubroutineOverlay(), "Playback should report active subroutine overlay");
        assertTrue(playback.getCurrentSteps().size() == 4, "Subroutine steps should be injected once into timeline");
        assertTrue(playback.getCurrentStepIndex() == 1,
                "Opening the subroutine should jump to its first injected step");

        boolean openedAgain = playback.openSubroutineForLine(0);
        assertTrue(!openedAgain, "Nested or duplicate subroutine overlay opening should be blocked");

        playback.stepForward();
        playback.stepForward();
        assertTrue(playback.getCurrentStepIndex() == 3, "Stepping through expanded subroutine steps should work");

        boolean closed = playback.closeActiveSubroutineOverlay();
        assertTrue(closed, "Subroutine overlay should close");
        assertTrue(!playback.hasActiveSubroutineOverlay(), "Overlay flag should clear after closing");
        assertTrue(playback.getCurrentSteps().size() == 2, "Timeline should restore to base steps");
        assertTrue(playback.getCurrentStepIndex() == 0,
                "Closing during subroutine playback should return to the original call site");
        assertTrue(playback.canOpenSubroutineAtCurrentStep(),
                "The same call site should be reopenable after returning to it");

        boolean reopened = playback.openSubroutineAtCurrentStep();
        assertTrue(reopened, "Subroutine should reopen from the same call site after returning to it");
    }

    private static void smokeTestStepAnchoredSubroutineOpening() {
        AlgorithmPlaybackController playback = new AlgorithmPlaybackController();

        AlgorithmStep call0 = simpleStep("Main call 0", "first call site", 0, "RELAX#0");
        AlgorithmStep nonCallSameLine = simpleStep("Main non-call", "same line but not callable", 0, null);
        AlgorithmStep call1 = simpleStep("Main call 1", "second call site", 0, "RELAX#1");

        java.util.Map<String, SubroutineTimeline> subroutines = java.util.Map.of(
                "RELAX#0", new SubroutineTimeline("RELAX#0", "RELAX", java.util.List.of("RELAX"), java.util.List.of(simpleStep("r0", "", 0, null))),
                "RELAX#1", new SubroutineTimeline("RELAX#1", "RELAX", java.util.List.of("RELAX"), java.util.List.of(simpleStep("r1", "", 0, null)))
        );

        AlgorithmResult base = new AlgorithmResult(
                "Synthetic step anchored",
                java.util.List.of("line0"),
                java.util.List.of(call0, nonCallSameLine, call1),
                false,
                "",
                subroutines,
                java.util.Map.of(0, "RELAX")
        );

        playback.start(base);
        assertTrue(playback.canOpenSubroutineAtCurrentStep(),
                "Current callable step should allow opening even if line map points to a different ID");
        assertTrue(playback.openSubroutineForLine(0),
                "Opening by line should resolve using current step callable id, not the shared line map value");
        assertTrue("RELAX#0".equals(playback.getActiveSubroutineId()),
                "First call site should open its own subroutine timeline");
        playback.closeActiveSubroutineOverlay();

        playback.stepForward();
        assertTrue(!playback.canOpenSubroutineAtCurrentStep(),
                "A non-call step on the same pseudocode line should not be treated as callable");
        assertTrue(!playback.openSubroutineForLine(0),
                "Line map alone should not open a subroutine when current step has no callable ID");

        playback.stepForward();
        assertTrue(playback.openSubroutineForLine(0),
                "Second callable step should open its own subroutine timeline");
        assertTrue("RELAX#1".equals(playback.getActiveSubroutineId()),
                "Second call site should open the second invocation timeline");
    }

    private static AlgorithmStep simpleStep(String title, String message, int line, String callableSubroutineId) {
        return new AlgorithmStep(
                title,
                message,
                line,
                java.util.Map.of(),
                java.util.Map.of(),
                java.util.Map.of(),
                java.util.Map.of(),
                null,
                null,
                null,
                null,
                callableSubroutineId
        );
    }

    private static void smokeTestMatrixAlgorithmsExposeSnapshots() {
        AlgorithmRequest request = createSampleRequest();
        assertHasMatrixSnapshots(new FloydWarshallAlgorithm().run(request), "Floyd-warshall");
        assertHasMatrixSnapshots(new MatrixMultiplicationApspAlgorithm().run(request), "matrix multiplication");
        assertHasMatrixSnapshots(new RecursiveDoublingApspAlgorithm().run(request), "recursive doubling");
        assertHasMatrixSnapshots(new TransitiveClosureAlgorithm().run(request), "transitive closure");
        assertHasMatrixSnapshots(new SeidelAlgorithm().run(request), "seidel");
    }

    private static void smokeTestRecursiveAlgorithmsExposeStackFrames() {
        AlgorithmRequest request = createSampleRequest();
        assertHasStackSnapshots(new DepthFirstSearchAlgorithm().run(request), "DFS");
        assertHasStackSnapshots(new SeidelAlgorithm().run(request), "Seidel");
    }

    private static void smokeTestMaximumFlowAlgorithmsExposeResiduals() {
        AlgorithmRequest request = createFlowRequest();
        AlgorithmResult ff = new FordFulkersonAlgorithm().run(request);
        AlgorithmResult ek = new EdmondsKarpAlgorithm().run(request);
        assertHasResidualSnapshots(ff, "Ford-Fulkerson");
        assertHasResidualSnapshots(ek, "Edmonds-Karp");
        assertSubroutineMetadataConsistent(ff, "Ford-Fulkerson");
        assertSubroutineMetadataConsistent(ek, "Edmonds-Karp");
        AlgorithmResult dinic = new DinicAlgorithm().run(request);
        assertHasResidualSnapshots(dinic, "Dinic");
        assertSubroutineMetadataConsistent(dinic, "Dinic");
        assertTrue(dinic.getSteps().stream().anyMatch(step -> step.getLevelGraphSnapshot() != null),
                "Dinic should emit level-graph snapshots");
        assertTrue(dinic.getSteps().stream().anyMatch(step -> !step.getEdgeAnnotations().isEmpty()),
                "Maximum-flow steps should annotate edges as flow/capacity");
    }

    private static void smokeTestBellmanFordSubroutineMetadata() {
        AlgorithmRequest request = createSampleRequest();
        AlgorithmResult result = new BellmanFordAlgorithm().run(request);
        assertSubroutineMetadataConsistent(result, "Bellman-Ford");
    }

    private static void smokeTestMaximumFlowPathStepPrecedesMutation() {
        AlgorithmRequest request = createFlowRequest();
        assertPathStepPrecedesMutation(new FordFulkersonAlgorithm().run(request), "Ford-Fulkerson");
        assertPathStepPrecedesMutation(new EdmondsKarpAlgorithm().run(request), "Edmonds-Karp");
        assertPathStepPrecedesMutation(new DinicAlgorithm().run(request), "Dinic");
    }

    private static void smokeTestMstAlgorithmsExposeAcceptedAndRejectedDecisions() {
        AlgorithmRequest request = createSampleRequest();
        assertMstDecisionColors(new PrimAlgorithm().run(request), true, "Prim");

        AlgorithmRequest kruskalRequest = new AlgorithmRequest(
                request.getGraph(), request.getScopeVertices(), request.getScopeEdges(), null
        );
        assertMstDecisionColors(new KruskalAlgorithm().run(kruskalRequest), false, "Kruskal");
    }

    private static void assertPathStepPrecedesMutation(AlgorithmResult result, String name) {
        for (int i = 0; i + 1 < result.getSteps().size(); i++) {
            AlgorithmStep step = result.getSteps().get(i);
            if (!step.getTitle().startsWith("Augmenting path")) {
                continue;
            }
            AlgorithmStep next = result.getSteps().get(i + 1);
            assertTrue(next.getTitle().toLowerCase().contains("augment"),
                    name + " should follow a path-highlighting step with an augmentation step");
            assertTrue(!step.getEdgeAnnotations().equals(next.getEdgeAnnotations())
                            || !step.getResidualGraphSnapshot().getEdges().equals(next.getResidualGraphSnapshot().getEdges()),
                    name + " should mutate flow/residual state only after the path-highlighting step");
            return;
        }
        throw new AssertionError(name + " should emit at least one separate augmenting-path step");
    }

    private static void assertMstDecisionColors(AlgorithmResult result, boolean requireAcceptedVertex, String name) {
        boolean hasAcceptedEdge = result.getSteps().stream()
                .anyMatch(step -> step.getEdgeColors().containsValue(AlgorithmPalette.ACCEPTED));
        boolean hasRejectedEdge = result.getSteps().stream()
                .anyMatch(step -> step.getEdgeColors().containsValue(AlgorithmPalette.REJECTED));
        boolean hasAcceptedVertex = result.getSteps().stream()
                .anyMatch(step -> step.getVertexColors().containsValue(AlgorithmPalette.ACCEPTED));

        assertTrue(hasAcceptedEdge, name + " should color accepted MST edges");
        assertTrue(hasRejectedEdge, name + " should color rejected MST edges");
        if (requireAcceptedVertex) {
            assertTrue(hasAcceptedVertex, name + " should color accepted MST vertices");
        }
    }

    private static void assertHasMatrixSnapshots(AlgorithmResult result, String name) {
        boolean hasSnapshot = result.getSteps().stream().anyMatch(step -> step.getMatrixSnapshot() != null);
        assertTrue(hasSnapshot, name + " should emit at least one matrix snapshot step");
    }

    private static void assertHasStackSnapshots(AlgorithmResult result, String name) {
        boolean hasSnapshot = result.getSteps().stream().anyMatch(step -> step.getStackFramesSnapshot() != null);
        assertTrue(hasSnapshot, name + " should emit at least one stack-frame snapshot step");
    }

    private static void assertHasResidualSnapshots(AlgorithmResult result, String name) {
        boolean hasSnapshot = result.getSteps().stream().anyMatch(step -> step.getResidualGraphSnapshot() != null);
        assertTrue(hasSnapshot, name + " should emit at least one residual-graph snapshot step");
    }

    private static void assertSubroutineMetadataConsistent(AlgorithmResult result, String name) {
        if (result.getSubroutineTimelines().isEmpty() && result.getCallableSubroutineByLine().isEmpty()) {
            return;
        }
        for (AlgorithmStep step : result.getSteps()) {
            String id = step.getCallableSubroutineId();
            if (id == null) {
                continue;
            }
            SubroutineTimeline timeline = result.getSubroutineTimelines().get(id);
            assertTrue(timeline != null, name + " callable step references missing subroutine id: " + id);
            assertTrue(!timeline.getSteps().isEmpty(), name + " subroutine timeline should contain steps for id: " + id);
        }
        for (Integer line : result.getCallableSubroutineByLine().keySet()) {
            assertTrue(line >= 0 && line < result.getPseudocodeLines().size(),
                    name + " exposes callable line out of pseudocode range: " + line);
        }
    }

    private static AlgorithmRequest createSampleRequest() {
        DirectedGraph graph = new DirectedGraph();
        graph.addEdge(0, 1, 1);
        graph.addEdge(0, 2, 4);
        graph.addEdge(1, 2, 1);
        graph.addEdge(1, 3, 2);
        graph.addEdge(2, 3, 1);
        return new AlgorithmRequest(graph, graph.getVertices(), graph.getEdges(), graph.getVertex(0));
    }

    private static AlgorithmRequest createFlowRequest() {
        DirectedGraph graph = new DirectedGraph();
        graph.addEdge(0, 1, 10);
        graph.addEdge(0, 2, 5);
        graph.addEdge(1, 2, 15);
        graph.addEdge(1, 3, 10);
        graph.addEdge(2, 3, 10);
        return new AlgorithmRequest(graph, graph.getVertices(), graph.getEdges(), graph.getVertex(0), graph.getVertex(3));
    }

    private static void assertValid(GraphAlgorithm algorithm, AlgorithmRequest request) {
        String validation = algorithm.validate(request);
        assertTrue(validation == null, "Unexpected validation error: " + validation);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertAllStepLinesInRange(AlgorithmResult result) {
        int lineCount = result.getPseudocodeLines().size();
        for (AlgorithmStep step : result.getSteps()) {
            int line = step.getActivePseudocodeLine();
            assertTrue(line >= 0 && line < lineCount,
                    "Step line index out of range: " + line + " (lineCount=" + lineCount + ")");
        }
    }
}
