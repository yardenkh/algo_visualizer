package algorithm.sssp;

import algorithm.*;
import algorithm.recursion.DfsStackFrameAdapter;
import algorithm.recursion.StackFramesSnapshot;
import graph.Edge;
import graph.Vertex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * DFS traversal with recursive stack visualization.
 *
 * Timeline approach:
 * 1. Initialize state (colors, times)
 * 2. Iterate through each unvisited starting vertex
 * 3. For each starting vertex, recursively explore using DFS-VISIT
 * 4. Emit a step at each state change (discover, inspect edge, recurse, backtrack, finish)
 * 5. Track stack frames as recursion depth increases/decreases
 *
 * Visualization payloads:
 * - Vertex colors: SCOPE_NEUTRAL → DISCOVERED (gray) → FRONTIER (active path) → FINISHED
 * - Edge colors: Tree edges highlighted, back edges marked
 * - Stack frames: Shows DFS-VISIT call stack with current vertex, depth, and phase
 *
 * Time complexity: O(V + E)
 *
 * @author Graph Editor
 */
public final class DepthFirstSearchAlgorithm implements GraphAlgorithm {
    // CLRS-inspired style (not verbatim) to keep textbook-like notation in the UI.
    private static final List<String> PSEUDOCODE = List.of(
            "procedure DFS(G)",
            "  for each u in V[G] do color[u] <- WHITE",
            "  DFS-VISIT(s)",
            "procedure DFS-VISIT(u)",
            "  color[u] <- GRAY; record discovery time",
            "  for each v in Adj[u] do",
            "    if color[v] = WHITE then classify (u, v) as tree edge; DFS-VISIT(v)",
            "    else if color[v] = GRAY then classify as back edge else classify as cross/forward",
            "  color[u] <- BLACK; record finish time",
            "  for each still-unvisited vertex x do DFS-VISIT(x)",
            "end procedure"
    );
    private static final DfsStackFrameAdapter STACK_ADAPTER = new DfsStackFrameAdapter();

    @Override
    public String getDisplayName() {
        return "Depth-First Search (DFS)";
    }

    @Override
    public String getDescription() {
        return "Explores the selected graph by diving along one path before backtracking.";
    }

    @Override
    public List<String> getPseudocodeLines() {
        return PSEUDOCODE;
    }

    @Override
    public String validate(AlgorithmRequest request) {
        if (request == null || request.isEmpty()) {
            return "Select at least one vertex before running DFS.";
        }
        if (request.getSourceVertex() == null) {
            return "Choose a source vertex for DFS.";
        }
        return null;
    }

    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<AlgorithmStep> steps = new ArrayList<>();
        Set<Integer> discovered = new LinkedHashSet<>();
        Set<Integer> finished = new LinkedHashSet<>();
        List<Integer> activePath = new ArrayList<>();
        Set<EdgeKey> treeEdges = new LinkedHashSet<>();
        Map<Integer, Integer> discoveryTime = new LinkedHashMap<>();
        Map<Integer, Integer> finishTime = new LinkedHashMap<>();
        List<DfsStackFrameAdapter.DfsFrameState> stackFrames = new ArrayList<>();
        int[] time = {0};
        SubroutineStepCollector visitCollector = new SubroutineStepCollector();

        steps.add(snapshot(request, discovered, finished, activePath, treeEdges, null, null,
                discoveryTime, finishTime, stackFrames,
                1,
                "Initialize DFS",
                "Starting depth-first search from " + request.getSourceVertex().getName() + "."));

        steps.add(snapshotWithSubroutine(request, discovered, finished, activePath, treeEdges, null, request.getSourceVertex(),
                discoveryTime, finishTime, stackFrames,
                2,
                "DFS-VISIT(" + request.getSourceVertex().getName() + ")",
                "Entering DFS-VISIT for the chosen start vertex.",
                SubroutineLibrary.DFS_VISIT));

        exploreFrom(request.getSourceVertex(), request, steps, visitCollector, discovered, finished, activePath, treeEdges,
                discoveryTime, finishTime, stackFrames, 0, time);

        for (Vertex vertex : request.getScopeVertices()) {
            if (!discovered.contains(vertex.getId())) {
                steps.add(snapshotWithSubroutine(request, discovered, finished, activePath, treeEdges, null, vertex,
                        discoveryTime, finishTime, stackFrames,
                        9,
                        "Start new component",
                        vertex.getName() + " belongs to a disconnected part of the selected graph, so DFS starts a new tree there.",
                        SubroutineLibrary.DFS_VISIT));
                exploreFrom(vertex, request, steps, visitCollector, discovered, finished, activePath, treeEdges,
                        discoveryTime, finishTime, stackFrames, 0, time);
            }
        }

        steps.add(snapshot(request, discovered, finished, activePath, treeEdges, null, null,
                discoveryTime, finishTime, stackFrames,
                10,
                "DFS complete",
                "Depth-first search finished for the selected graph scope."));

        SubroutineTimeline visitTimeline = visitCollector.build(
                SubroutineLibrary.DFS_VISIT, "DFS-VISIT(u)", SubroutineLibrary.DFS_VISIT_PSEUDOCODE);
        SubroutineStepCollector.AlgorithmResultSubroutineData sub =
                SubroutineStepCollector.wrap(visitTimeline, List.of(2, 6, 9));

        return new AlgorithmResult(getDisplayName(), getPseudocodeLines(), steps, false, "", sub.timelines, sub.callableByLine);
    }

    private void exploreFrom(
            Vertex current,
            AlgorithmRequest request,
            List<AlgorithmStep> steps,
            SubroutineStepCollector visitCollector,
            Set<Integer> discovered,
            Set<Integer> finished,
            List<Integer> activePath,
            Set<EdgeKey> treeEdges,
            Map<Integer, Integer> discoveryTime,
            Map<Integer, Integer> finishTime,
            List<DfsStackFrameAdapter.DfsFrameState> stackFrames,
            int depth,
            int[] time
    ) {
        discovered.add(current.getId());
        activePath.add(current.getId());
        discoveryTime.put(current.getId(), ++time[0]);
        stackFrames.add(new DfsStackFrameAdapter.DfsFrameState(current, depth, "discover", time[0]));

        // DFS-VISIT subroutine line 0: entry + line 1: color GRAY
        AlgorithmStep visitEntry = snapshot(request, discovered, finished, activePath, treeEdges, null, current,
                discoveryTime, finishTime, stackFrames, 0,
                "DFS-VISIT entry " + current.getName(), "DFS-VISIT(" + current.getName() + ") called.");
        AlgorithmStep visitGray = snapshot(request, discovered, finished, activePath, treeEdges, null, current,
                discoveryTime, finishTime, stackFrames, 1,
                "color[" + current.getName() + "] <- GRAY", current.getName() + " marked GRAY (discovered).");
        AlgorithmStep visitTime = snapshot(request, discovered, finished, activePath, treeEdges, null, current,
                discoveryTime, finishTime, stackFrames, 3,
                "d[" + current.getName() + "] <- " + time[0], "Discovery time set to " + time[0] + ".");
        visitCollector.add(visitEntry);
        visitCollector.add(visitGray);
        visitCollector.add(visitTime);

        steps.add(snapshot(request, discovered, finished, activePath, treeEdges, null, current,
                discoveryTime, finishTime, stackFrames,
                3,
                "Discover " + current.getName(),
                current.getName() + " is discovered and pushed onto the active DFS path."));

        for (Edge edge : request.getOutgoingEdges(current)) {
            Vertex destination = edge.getDestination();
            replaceTop(stackFrames, new DfsStackFrameAdapter.DfsFrameState(current, depth, "inspect->" + destination.getName(), time[0]));

            AlgorithmStep visitInspect = snapshot(request, discovered, finished, activePath, treeEdges, edge, current,
                    discoveryTime, finishTime, stackFrames, 4,
                    "for each v in Adj[" + current.getName() + "]",
                    "Inspecting neighbor " + destination.getName() + " from DFS-VISIT.");
            visitCollector.add(visitInspect);

            steps.add(snapshot(request, discovered, finished, activePath, treeEdges, edge, current,
                    discoveryTime, finishTime, stackFrames,
                    4,
                    "Inspect edge " + current.getName() + " -> " + destination.getName(),
                    "DFS inspects edge " + current.getName() + " -> " + destination.getName() + "."));

            if (!discovered.contains(destination.getId())) {
                treeEdges.add(EdgeKey.from(edge));
                replaceTop(stackFrames, new DfsStackFrameAdapter.DfsFrameState(current, depth, "tree-edge-condition", time[0]));

                AlgorithmStep visitWhite = snapshot(request, discovered, finished, activePath, treeEdges, edge, current,
                        discoveryTime, finishTime, stackFrames, 5,
                        "color[v] = WHITE",
                        destination.getName() + " is WHITE. Setting π[" + destination.getName() + "] <- " + current.getName() + ".");
                visitCollector.add(visitWhite);

                steps.add(snapshot(request, discovered, finished, activePath, treeEdges, edge, current,
                        discoveryTime, finishTime, stackFrames,
                        6,
                        "if color[v] = WHITE then...",
                        "Color of " + destination.getName() + " is WHITE (undiscovered), so this is a tree edge. Classifying and recursing."));

                replaceTop(stackFrames, new DfsStackFrameAdapter.DfsFrameState(current, depth, "recurse->" + destination.getName(), time[0]));
                steps.add(snapshotWithSubroutine(request, discovered, finished, activePath, treeEdges, edge, current,
                        discoveryTime, finishTime, stackFrames,
                        6,
                        "DFS-VISIT(" + destination.getName() + ")",
                        "Making recursive call to DFS-VISIT(" + destination.getName() + ").",
                        SubroutineLibrary.DFS_VISIT));
                exploreFrom(destination, request, steps, visitCollector, discovered, finished, activePath, treeEdges,
                        discoveryTime, finishTime, stackFrames, depth + 1, time);
                replaceTop(stackFrames, new DfsStackFrameAdapter.DfsFrameState(current, depth, "backtrack", time[0]));
                steps.add(snapshot(request, discovered, finished, activePath, treeEdges, null, current,
                        discoveryTime, finishTime, stackFrames,
                        6,
                        "Backtrack to " + current.getName(),
                        "DFS returns to " + current.getName() + " after finishing the deeper recursive branch."));
            } else if (!finished.contains(destination.getId())) {
                replaceTop(stackFrames, new DfsStackFrameAdapter.DfsFrameState(current, depth, "back-edge", time[0]));
                steps.add(snapshot(request, discovered, finished, activePath, treeEdges, edge, current,
                        discoveryTime, finishTime, stackFrames,
                        7,
                        "else if color[v] = GRAY then...",
                        destination.getName() + " is already on the active DFS path, so this edge points back into the current recursion."));
            } else {
                replaceTop(stackFrames, new DfsStackFrameAdapter.DfsFrameState(current, depth, "skip-finished", time[0]));
                steps.add(snapshot(request, discovered, finished, activePath, treeEdges, edge, current,
                        discoveryTime, finishTime, stackFrames,
                        7,
                        "else classify as cross/forward",
                        destination.getName() + " was already fully explored, so DFS classifies this as a cross or forward edge."));
            }
        }

        activePath.remove(activePath.size() - 1);
        finished.add(current.getId());
        finishTime.put(current.getId(), ++time[0]);
        replaceTop(stackFrames, new DfsStackFrameAdapter.DfsFrameState(current, depth, "finish", time[0]));

        // DFS-VISIT subroutine lines 8-10: color BLACK, finish time
        AlgorithmStep visitBlack = snapshot(request, discovered, finished, activePath, treeEdges, null, current,
                discoveryTime, finishTime, stackFrames, 8,
                "color[" + current.getName() + "] <- BLACK", current.getName() + " marked BLACK (finished).");
        AlgorithmStep visitFinishTime = snapshot(request, discovered, finished, activePath, treeEdges, null, current,
                discoveryTime, finishTime, stackFrames, 10,
                "f[" + current.getName() + "] <- " + time[0], "Finish time set to " + time[0] + ".");
        visitCollector.add(visitBlack);
        visitCollector.add(visitFinishTime);

        steps.add(snapshot(request, discovered, finished, activePath, treeEdges, null, current,
                discoveryTime, finishTime, stackFrames,
                8,
                "Finish " + current.getName(),
                current.getName() + " is finished. DFS now backtracks to its caller, if any."));

        stackFrames.remove(stackFrames.size() - 1);
    }

    private AlgorithmStep snapshot(
            AlgorithmRequest request,
            Set<Integer> discovered,
            Set<Integer> finished,
            List<Integer> activePath,
            Set<EdgeKey> treeEdges,
            Edge activeEdge,
            Vertex current,
            Map<Integer, Integer> discoveryTime,
            Map<Integer, Integer> finishTime,
            List<DfsStackFrameAdapter.DfsFrameState> stackFrames,
            int activeLine,
            String title,
            String message
    ) {
        return snapshotWithSubroutine(
                request,
                discovered,
                finished,
                activePath,
                treeEdges,
                activeEdge,
                current,
                discoveryTime,
                finishTime,
                stackFrames,
                activeLine,
                title,
                message,
                null
        );
    }

    private AlgorithmStep snapshotWithSubroutine(
            AlgorithmRequest request,
            Set<Integer> discovered,
            Set<Integer> finished,
            List<Integer> activePath,
            Set<EdgeKey> treeEdges,
            Edge activeEdge,
            Vertex current,
            Map<Integer, Integer> discoveryTime,
            Map<Integer, Integer> finishTime,
            List<DfsStackFrameAdapter.DfsFrameState> stackFrames,
            int activeLine,
            String title,
            String message,
            String callableSubroutineId
    ) {
        Map<Integer, Integer> vertexColors = new LinkedHashMap<>();
        Map<EdgeKey, Integer> edgeColors = new LinkedHashMap<>();
        Map<Integer, String> annotations = new LinkedHashMap<>();

        for (Vertex vertex : request.getScopeVertices()) {
            int id = vertex.getId();
            vertexColors.put(id, AlgorithmPalette.SCOPE_NEUTRAL);
            if (discovered.contains(id)) {
                vertexColors.put(id, AlgorithmPalette.DISCOVERED);
            }
            if (activePath.contains(id)) {
                vertexColors.put(id, AlgorithmPalette.FRONTIER);
            }
            if (finished.contains(id)) {
                vertexColors.put(id, AlgorithmPalette.FINISHED);
            }
            if (current != null && current.getId() == id) {
                vertexColors.put(id, AlgorithmPalette.ACTIVE);
            }

            String start = discoveryTime.containsKey(id) ? String.valueOf(discoveryTime.get(id)) : "-";
            String end = finishTime.containsKey(id) ? String.valueOf(finishTime.get(id)) : "-";
            annotations.put(id, "d=" + start + " f=" + end);
        }

        for (Edge edge : request.getScopeEdges()) {
            EdgeKey key = EdgeKey.from(edge);
            if (treeEdges.contains(key)) {
                edgeColors.put(key, AlgorithmPalette.TREE_EDGE);
            }
        }
        if (activeEdge != null) {
            edgeColors.put(EdgeKey.from(activeEdge), AlgorithmPalette.ACTIVE_EDGE);
        }

        StackFramesSnapshot stackSnapshot = STACK_ADAPTER.toSnapshot(stackFrames, "DFS Call Stack");
        return new AlgorithmStep(
                title,
                message,
                activeLine,
                vertexColors,
                edgeColors,
                annotations,
                java.util.Collections.emptyMap(),
                null,
                stackSnapshot,
                null,
                null,
                callableSubroutineId
        );
    }

    private static void replaceTop(List<DfsStackFrameAdapter.DfsFrameState> stackFrames, DfsStackFrameAdapter.DfsFrameState frame) {
        if (stackFrames.isEmpty()) {
            stackFrames.add(frame);
            return;
        }
        stackFrames.set(stackFrames.size() - 1, frame);
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.SSSP;
    }
}
