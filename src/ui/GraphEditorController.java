package ui;

import algorithm.AlgorithmRequest;
import graph.DirectedGraph;
import graph.Edge;
import graph.Vertex;
import ui.extras.LineGraphRootService;
import ui.templates.GraphTemplate;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Application-level controller for graph editing operations.
 * Keeps model mutation logic outside Swing components.
 */
public class GraphEditorController {
    public enum EdgeCreationMode {
        DIRECTED,
        UNDIRECTED_PAIRED
    }

    private final DirectedGraph graph;
    private final Map<Integer, Point2D.Double> positions;
    private final Set<Integer> selectedVertexIds;
    private final List<Runnable> changeListeners;
    private final List<Runnable> modificationListeners;
    private Edge selectedEdge;
    private final LineGraphRootService lineGraphRootService;
    private final Map<Long, Point2D.Double> edgeBends;
    private final Deque<GraphStateSnapshot> undoStack;
    private GraphStateSnapshot lastStableSnapshot;
    private boolean suspendHistory;
    private EdgeCreationMode edgeCreationMode;
    private SubgraphClipboard clipboard;
    private int clipboardPasteCount;

    private static final int MAX_UNDO_STATES = 200;

    public GraphEditorController() {
        this.graph = new DirectedGraph();
        this.positions = new LinkedHashMap<>();
        this.selectedVertexIds = new LinkedHashSet<>();
        this.changeListeners = new ArrayList<>();
        this.modificationListeners = new ArrayList<>();
        this.lineGraphRootService = new LineGraphRootService();
        this.edgeBends = new LinkedHashMap<>();
        this.undoStack = new ArrayDeque<>();
        this.suspendHistory = false;
        this.edgeCreationMode = EdgeCreationMode.DIRECTED;
        this.clipboard = null;
        this.clipboardPasteCount = 0;
        this.lastStableSnapshot = exportGraphState();
    }

    public DirectedGraph getGraph() {
        return graph;
    }

    /** Current edge creation policy used by the connect tool. */
    public EdgeCreationMode getEdgeCreationMode() {
        return edgeCreationMode;
    }

    /** Sets edge creation policy. Optionally normalizes existing edges for undirected paired mode. */
    public void setEdgeCreationMode(EdgeCreationMode mode, boolean normalizeExisting) {
        if (mode == null) {
            return;
        }
        boolean changed = this.edgeCreationMode != mode;
        this.edgeCreationMode = mode;
        if (mode == EdgeCreationMode.UNDIRECTED_PAIRED && normalizeExisting) {
            normalizeToUndirectedPairs();
            return;
        }
        if (changed) {
            notifyChanged();
        }
    }

    /** Counts edges as logical undirected pairs when paired mode is active. */
    public int getDisplayedEdgeCount() {
        if (edgeCreationMode != EdgeCreationMode.UNDIRECTED_PAIRED) {
            return graph.edgeCount();
        }
        Set<Long> seen = new LinkedHashSet<>();
        int loops = 0;
        for (Edge edge : graph.getEdges()) {
            int u = edge.getSource().getId();
            int v = edge.getDestination().getId();
            if (u == v) {
                loops++;
                continue;
            }
            int a = Math.min(u, v);
            int b = Math.max(u, v);
            seen.add(edgeKey(a, b));
        }
        return seen.size() + loops;
    }

    public Collection<Vertex> getVertices() {
        return graph.getVertices();
    }

    public Collection<Edge> getEdges() {
        return graph.getEdges();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void addModificationListener(Runnable listener) {
        if (listener != null) {
            modificationListeners.add(listener);
        }
    }

    public List<Vertex> getSelectedVertices() {
        List<Vertex> selected = new ArrayList<>();
        for (Integer id : selectedVertexIds) {
            Vertex vertex = graph.getVertex(id);
            if (vertex != null) {
                selected.add(vertex);
            }
        }
        selected.sort(Comparator.comparingInt(Vertex::getId));
        return Collections.unmodifiableList(selected);
    }

    public List<Vertex> getAlgorithmScopeVertices() {
        List<Vertex> scope = selectedVertexIds.isEmpty() ? new ArrayList<>(graph.getVertices()) : new ArrayList<>(getSelectedVertices());
        scope.sort(Comparator.comparingInt(Vertex::getId));
        return Collections.unmodifiableList(scope);
    }

    /**
     * Returns vertices currently selected by the user, sorted by id.
     *
     * Extras actions use this as the exact subgraph scope; unlike algorithm scope,
     * this method never falls back to the whole graph.
     */
    public List<Vertex> getSelectedScopeVertices() {
        return getSelectedVertices();
    }

    /**
     * Returns edges induced by the currently selected vertices.
     * An edge belongs to the scope only if both endpoints are selected.
     */
    public List<Edge> getSelectedScopeEdges() {
        if (selectedVertexIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Edge> scopeEdges = new ArrayList<>();
        for (Edge edge : graph.getEdges()) {
            if (selectedVertexIds.contains(edge.getSource().getId())
                    && selectedVertexIds.contains(edge.getDestination().getId())) {
                scopeEdges.add(edge);
            }
        }
        return Collections.unmodifiableList(scopeEdges);
    }

    /**
     * Returns selected-scope edges using the active creation semantics.
     * In undirected paired mode, opposite arcs are collapsed to one logical edge.
     */
    public List<Edge> getSelectedScopeEdgesForCurrentMode() {
        List<Edge> scopeEdges = new ArrayList<>(getSelectedScopeEdges());
        if (edgeCreationMode != EdgeCreationMode.UNDIRECTED_PAIRED) {
            return Collections.unmodifiableList(scopeEdges);
        }
        return Collections.unmodifiableList(collapseBidirectionalPairs(scopeEdges));
    }

    /** Count of selected-scope edges shown to the user in the current mode. */
    public int getSelectedScopeEdgeCountForDisplay() {
        return getSelectedScopeEdgesForCurrentMode().size();
    }

    /** @return true when at least one vertex is selected (selected-subgraph exists). */
    public boolean hasSelectedSubgraph() {
        return !selectedVertexIds.isEmpty();
    }

    public AlgorithmRequest createAlgorithmRequest(Integer sourceVertexId) {
        return createAlgorithmRequest(sourceVertexId, null);
    }

    /** Builds an algorithm request with optional source and sink vertices. */
    public AlgorithmRequest createAlgorithmRequest(Integer sourceVertexId, Integer sinkVertexId) {
        return createAlgorithmRequest(sourceVertexId, sinkVertexId, false);
    }

    /**
     * Builds an algorithm request with optional source/sink and optional pair collapse.
     *
     * When {@code collapseBidirectionalPairs} is true, opposite arcs u->v and v->u are
     * represented as one logical edge for undirected-style algorithms.
     */
    public AlgorithmRequest createAlgorithmRequest(Integer sourceVertexId, Integer sinkVertexId, boolean collapseBidirectionalPairs) {
        List<Vertex> scopeVertices = getAlgorithmScopeVertices();
        List<Edge> scopeEdges = new ArrayList<>();
        Set<Integer> scopeIds = new LinkedHashSet<>();
        for (Vertex vertex : scopeVertices) {
            scopeIds.add(vertex.getId());
        }
        for (Edge edge : graph.getEdges()) {
            if (scopeIds.contains(edge.getSource().getId()) && scopeIds.contains(edge.getDestination().getId())) {
                scopeEdges.add(edge);
            }
        }
        if (collapseBidirectionalPairs) {
            scopeEdges = collapseBidirectionalPairs(scopeEdges);
        }

        Map<Integer, Point2D.Double> scopedPositions = new LinkedHashMap<>();
        for (Vertex vertex : scopeVertices) {
            Point2D.Double p = positions.get(vertex.getId());
            if (p != null) {
                scopedPositions.put(vertex.getId(), new Point2D.Double(p.x, p.y));
            }
        }

        Vertex sourceVertex = null;
        if (sourceVertexId != null) {
            sourceVertex = graph.getVertex(sourceVertexId);
        } else if (!scopeVertices.isEmpty()) {
            sourceVertex = scopeVertices.get(0);
        }

        Vertex sinkVertex = null;
        if (sinkVertexId != null) {
            sinkVertex = graph.getVertex(sinkVertexId);
        }
        return new AlgorithmRequest(graph, scopeVertices, scopeEdges, sourceVertex, sinkVertex, scopedPositions);
    }

    private List<Edge> collapseBidirectionalPairs(List<Edge> edges) {
        Map<Long, Edge> unique = new LinkedHashMap<>();
        for (Edge edge : edges) {
            int u = edge.getSource().getId();
            int v = edge.getDestination().getId();
            if (u == v) {
                unique.put(edgeKey(u, v), edge);
                continue;
            }
            int a = Math.min(u, v);
            int b = Math.max(u, v);
            long key = edgeKey(a, b);
            Edge existing = unique.get(key);
            if (existing == null
                    || (edge.getSource().getId() < existing.getSource().getId())
                    || (edge.getSource().getId() == existing.getSource().getId()
                    && edge.getDestination().getId() < existing.getDestination().getId())) {
                unique.put(key, edge);
            }
        }
        return new ArrayList<>(unique.values());
    }

    public void applyVertexPositions(Map<Integer, Point2D.Double> updatedPositions) {
        if (updatedPositions == null || updatedPositions.isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, Point2D.Double> entry : updatedPositions.entrySet()) {
            Point2D.Double current = positions.get(entry.getKey());
            Point2D.Double newValue = entry.getValue();
            if (current != null && newValue != null) {
                current.x = newValue.x;
                current.y = newValue.y;
            }
        }
        notifyChanged();
        notifyModified();
    }

    /** @return true when at least one undo step is available. */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /** Restores the most recent graph state captured before an edit. */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        GraphStateSnapshot snapshot = undoStack.pop();
        boolean previous = suspendHistory;
        suspendHistory = true;
        try {
            restoreGraphState(snapshot);
            lastStableSnapshot = exportGraphState();
            return true;
        } finally {
            suspendHistory = previous;
        }
    }

    /** Returns user-defined control offset for an edge, or null when auto mode is active. */
    public Point2D.Double getManualEdgeControlOffset(Edge edge) {
        if (edge == null) {
            return null;
        }
        Point2D.Double value = edgeBends.get(edgeKey(edge));
        return value == null ? null : new Point2D.Double(value.x, value.y);
    }

    /**
     * Updates manual control offset for an edge.
     *
     * @param commit if true, creates an undoable edit; if false, acts as drag preview
     */
    public void setManualEdgeControlOffset(Edge edge, Point2D.Double controlOffset, boolean commit) {
        if (edge == null || !graph.containsEdge(edge)) {
            return;
        }
        if (controlOffset == null || Math.hypot(controlOffset.x, controlOffset.y) < 1.0) {
            edgeBends.remove(edgeKey(edge));
        } else {
            edgeBends.put(edgeKey(edge), new Point2D.Double(controlOffset.x, controlOffset.y));
        }
        notifyChanged();
        if (commit) {
            notifyModified();
        }
    }

    /** Removes manual bend and optionally records the change in undo history. */
    public void clearManualEdgeBend(Edge edge, boolean commit) {
        if (edge == null) {
            return;
        }
        if (edgeBends.remove(edgeKey(edge)) != null) {
            notifyChanged();
            if (commit) {
                notifyModified();
            }
        }
    }

    /** Clears manual control offsets for all given edges. */
    public void clearManualEdgeControls(Iterable<Edge> edges, boolean commit) {
        if (edges == null) {
            return;
        }
        boolean changed = false;
        for (Edge edge : edges) {
            if (edge == null) {
                continue;
            }
            if (edgeBends.remove(edgeKey(edge)) != null) {
                changed = true;
            }
        }
        if (changed) {
            notifyChanged();
            if (commit) {
                notifyModified();
            }
        }
    }

    public Vertex addVertexAt(double x, double y) {
        Vertex vertex = graph.addVertex(nextVertexId());
        positions.put(vertex.getId(), new Point2D.Double(x, y));
        selectedVertexIds.clear();
        selectedVertexIds.add(vertex.getId());
        selectedEdge = null;
        notifyChanged();
        notifyModified();
        return vertex;
    }

    public Edge connect(Vertex source, Vertex destination, int weight, int color) {
        if (source == null || destination == null) {
            return null;
        }
        Edge edge = graph.addEdge(source, destination, weight, color);
        if (edgeCreationMode == EdgeCreationMode.UNDIRECTED_PAIRED && source.getId() != destination.getId()) {
            Edge reverse = graph.addEdge(destination, source, weight, color);
            reverse.setWeight(weight);
            reverse.setColor(color);
            syncPairManualBend(edge, reverse);
        }
        selectedEdge = edge;
        selectedVertexIds.clear();
        notifyChanged();
        notifyModified();
        return edge;
    }

    public void removeVertex(Vertex vertex) {
        if (vertex == null) {
            return;
        }
        graph.removeVertex(vertex);
        positions.remove(vertex.getId());
        selectedVertexIds.remove(vertex.getId());
        pruneEdgeBends();
        if (selectedEdge != null && selectedEdge.contains(vertex)) {
            selectedEdge = null;
        }
        notifyChanged();
        notifyModified();
    }

    public void removeEdge(Edge edge) {
        if (edge == null) {
            return;
        }
        Edge reverse = pairedEdge(edge);
        graph.removeEdge(edge);
        edgeBends.remove(edgeKey(edge));
        if (edgeCreationMode == EdgeCreationMode.UNDIRECTED_PAIRED && reverse != null && graph.containsEdge(reverse)) {
            graph.removeEdge(reverse);
            edgeBends.remove(edgeKey(reverse));
        }
        if (Objects.equals(selectedEdge, edge)) {
            selectedEdge = null;
        }
        if (Objects.equals(selectedEdge, reverse)) {
            selectedEdge = null;
        }
        notifyChanged();
        notifyModified();
    }

    public void selectOnlyVertex(Vertex vertex) {
        selectedVertexIds.clear();
        if (vertex != null) {
            selectedVertexIds.add(vertex.getId());
        }
        selectedEdge = null;
        notifyChanged();
    }

    public void toggleVertexSelection(Vertex vertex) {
        if (vertex == null) {
            return;
        }
        int id = vertex.getId();
        if (selectedVertexIds.contains(id)) {
            selectedVertexIds.remove(id);
        } else {
            selectedVertexIds.add(id);
        }
        selectedEdge = null;
        notifyChanged();
    }

    public void selectOnlyEdge(Edge edge) {
        selectedEdge = edge;
        selectedVertexIds.clear();
        notifyChanged();
    }

    /** In paired mode, selecting one direction visually selects both directions. */
    public boolean isEdgeSelected(Edge edge) {
        if (edge == null || selectedEdge == null) {
            return false;
        }
        if (Objects.equals(edge, selectedEdge)) {
            return true;
        }
        if (edgeCreationMode != EdgeCreationMode.UNDIRECTED_PAIRED) {
            return false;
        }
        Edge reverse = pairedEdge(selectedEdge);
        return Objects.equals(edge, reverse);
    }

    public void clearSelection() {
        if (selectedEdge == null && selectedVertexIds.isEmpty()) {
            return;
        }
        selectedEdge = null;
        selectedVertexIds.clear();
        notifyChanged();
    }

    /** Copies the current selection into an internal clipboard. Returns false when nothing can be copied. */
    public boolean copySelectionToClipboard() {
        List<Vertex> verticesToCopy;
        List<Edge> edgesToCopy;

        if (!selectedVertexIds.isEmpty()) {
            verticesToCopy = new ArrayList<>(getSelectedScopeVertices());
            edgesToCopy = new ArrayList<>(getSelectedScopeEdges());
        } else if (selectedEdge != null && graph.containsEdge(selectedEdge)) {
            verticesToCopy = new ArrayList<>();
            verticesToCopy.add(selectedEdge.getSource());
            if (selectedEdge.getDestination().getId() != selectedEdge.getSource().getId()) {
                verticesToCopy.add(selectedEdge.getDestination());
            }
            edgesToCopy = new ArrayList<>();
            edgesToCopy.add(selectedEdge);
            if (edgeCreationMode == EdgeCreationMode.UNDIRECTED_PAIRED) {
                Edge reverse = pairedEdge(selectedEdge);
                if (reverse != null) {
                    edgesToCopy.add(reverse);
                }
            }
        } else {
            return false;
        }

        Map<Integer, Point2D.Double> selectedPositions = new LinkedHashMap<>();
        double cx = 0.0;
        double cy = 0.0;
        int positioned = 0;
        for (Vertex vertex : verticesToCopy) {
            Point2D.Double point = positions.get(vertex.getId());
            if (point == null) {
                continue;
            }
            selectedPositions.put(vertex.getId(), new Point2D.Double(point.x, point.y));
            cx += point.x;
            cy += point.y;
            positioned++;
        }
        if (positioned == 0) {
            return false;
        }

        cx /= positioned;
        cy /= positioned;

        List<ClipboardVertex> copiedVertices = new ArrayList<>();
        for (Vertex vertex : verticesToCopy) {
            Point2D.Double point = selectedPositions.get(vertex.getId());
            if (point == null) {
                continue;
            }
            copiedVertices.add(new ClipboardVertex(
                    vertex.getId(),
                    vertex.getName(),
                    vertex.getColor(),
                    point.x - cx,
                    point.y - cy
            ));
        }

        List<ClipboardEdge> copiedEdges = new ArrayList<>();
        for (Edge edge : edgesToCopy) {
            if (!selectedPositions.containsKey(edge.getSource().getId())
                    || !selectedPositions.containsKey(edge.getDestination().getId())) {
                continue;
            }
            Point2D.Double bend = edgeBends.get(edgeKey(edge));
            copiedEdges.add(new ClipboardEdge(
                    edge.getSource().getId(),
                    edge.getDestination().getId(),
                    edge.getWeight(),
                    edge.getColor(),
                    bend == null ? 0.0 : bend.x,
                    bend == null ? 0.0 : bend.y
            ));
        }

        clipboard = new SubgraphClipboard(copiedVertices, copiedEdges, cx, cy);
        clipboardPasteCount = 0;
        return !copiedVertices.isEmpty();
    }

    /** Pastes the copied subgraph and selects the newly created vertices. */
    public boolean pasteClipboard() {
        if (clipboard == null || clipboard.vertices.isEmpty()) {
            return false;
        }

        clipboardPasteCount++;
        double offset = 34.0 * clipboardPasteCount;

        Map<Integer, Integer> newIdByOldId = new LinkedHashMap<>();
        selectedVertexIds.clear();
        selectedEdge = null;

        for (ClipboardVertex vertex : clipboard.vertices) {
            int newId = nextVertexId();
            Vertex created = graph.addVertex(newId);
            created.setName(vertex.name);
            created.setColor(vertex.color);
            positions.put(newId, new Point2D.Double(
                    clipboard.centerX + vertex.relativeX + offset,
                    clipboard.centerY + vertex.relativeY + offset
            ));
            newIdByOldId.put(vertex.oldId, newId);
            selectedVertexIds.add(newId);
        }

        for (ClipboardEdge edge : clipboard.edges) {
            Integer newSourceId = newIdByOldId.get(edge.sourceId);
            Integer newDestinationId = newIdByOldId.get(edge.destinationId);
            if (newSourceId == null || newDestinationId == null) {
                continue;
            }
            Vertex source = graph.getVertex(newSourceId);
            Vertex destination = graph.getVertex(newDestinationId);
            if (source == null || destination == null) {
                continue;
            }
            Edge created = graph.addEdge(source, destination, edge.weight, edge.color);
            if (Math.hypot(edge.bendX, edge.bendY) >= 1.0) {
                edgeBends.put(edgeKey(created), new Point2D.Double(edge.bendX, edge.bendY));
            }
        }

        notifyChanged();
        notifyModified();
        return true;
    }

    public void moveSelectedVertices(double dx, double dy) {
        if (dx == 0 && dy == 0) {
            return;
        }
        for (Integer id : selectedVertexIds) {
            Point2D.Double point = positions.get(id);
            if (point != null) {
                point.x += dx;
                point.y += dy;
            }
        }
        notifyChanged();
        notifyModified();
    }

    public void selectVerticesInRect(Rectangle2D.Double rect, boolean append) {
        if (!append) {
            selectedVertexIds.clear();
        }
        for (Vertex vertex : graph.getVertices()) {
            Point2D.Double position = positions.get(vertex.getId());
            if (position != null && rect.contains(position)) {
                selectedVertexIds.add(vertex.getId());
            }
        }
        selectedEdge = null;
        notifyChanged();
    }

    public void setSelectedVerticesColor(int rgb) {
        for (Integer id : selectedVertexIds) {
            Vertex vertex = graph.getVertex(id);
            if (vertex != null) {
                vertex.setColor(rgb);
            }
        }
        notifyChanged();
        notifyModified();
    }

    /**
     * Renames the currently selected vertices.
     *
     * <p>When {@code append} is false, every selected vertex receives the provided text exactly.
     * When {@code append} is true, the provided text is appended to each vertex's current name.
     * Empty append text is treated as a no-op.
     *
     * @param text text to apply to the selected vertices; null is treated as empty
     * @param append whether to append the text instead of replacing names
     * @return true when at least one vertex name changed
     */
    public boolean renameSelectedVertices(String text, boolean append) {
        List<Vertex> vertices = getSelectedScopeVertices();
        if (vertices.isEmpty()) {
            return false;
        }

        String value = text == null ? "" : text;
        if (append && value.isEmpty()) {
            return false;
        }

        boolean updated = false;
        for (Vertex vertex : vertices) {
            String currentName = vertex.getName();
            String newName = append ? currentName + value : value;
            if (!Objects.equals(currentName, newName)) {
                vertex.setName(newName);
                updated = true;
            }
        }

        if (updated) {
            notifyChanged();
            notifyModified();
        }
        return updated;
    }

    /**
     * Applies colors to vertices by id and notifies listeners once.
     *
     * @param colorsByVertexId map vertex-id -> RGB color
     */
    public void applyVertexColors(Map<Integer, Integer> colorsByVertexId) {
        if (colorsByVertexId == null || colorsByVertexId.isEmpty()) {
            return;
        }
        boolean updated = false;
        for (Map.Entry<Integer, Integer> entry : colorsByVertexId.entrySet()) {
            Vertex vertex = graph.getVertex(entry.getKey());
            if (vertex != null) {
                Integer rgb = entry.getValue();
                if (rgb != null) {
                    vertex.setColor(rgb);
                    updated = true;
                }
            }
        }
        if (updated) {
            notifyChanged();
            notifyModified();
        }
    }

    /**
     * Applies colors to edges and notifies listeners once.
     *
     * @param colorsByEdge map edge instance -> RGB color
     */
    public void applyEdgeColors(Map<Edge, Integer> colorsByEdge) {
        if (colorsByEdge == null || colorsByEdge.isEmpty()) {
            return;
        }
        boolean updated = false;
        for (Map.Entry<Edge, Integer> entry : colorsByEdge.entrySet()) {
            Edge edge = entry.getKey();
            Integer rgb = entry.getValue();
            if (edge != null && rgb != null && graph.containsEdge(edge)) {
                edge.setColor(rgb);
                if (edgeCreationMode == EdgeCreationMode.UNDIRECTED_PAIRED) {
                    Edge reverse = pairedEdge(edge);
                    if (reverse != null) {
                        reverse.setColor(rgb);
                    }
                }
                updated = true;
            }
        }
        if (updated) {
            notifyChanged();
            notifyModified();
        }
    }

    /**
     * Replaces the selected induced subgraph edge set with its complement.
     *
     * No self-loops are created. For directed mode, ordered pairs are used.
     * For undirected mode, exactly one edge is created per unordered pair.
     *
     * @param newEdgeWeight weight used for all newly created complement edges
     * @param newEdgeColor color used for all newly created complement edges
     * @return operation summary used by UI feedback
     */
    public ComplementGraphResult complementSelectedSubgraph(int newEdgeWeight, int newEdgeColor) {
        List<Vertex> scopeVertices = getSelectedScopeVertices();
        if (scopeVertices.isEmpty()) {
            return new ComplementGraphResult(0, 0, false);
        }

        Map<Integer, Integer> indexById = new LinkedHashMap<>();
        for (int i = 0; i < scopeVertices.size(); i++) {
            indexById.put(scopeVertices.get(i).getId(), i);
        }

        boolean[][] hadEdge = new boolean[scopeVertices.size()][scopeVertices.size()];
        for (Edge edge : graph.getEdges()) {
            Integer sourceIndex = indexById.get(edge.getSource().getId());
            Integer destinationIndex = indexById.get(edge.getDestination().getId());
            if (sourceIndex == null || destinationIndex == null || sourceIndex.equals(destinationIndex)) {
                continue;
            }
            hadEdge[sourceIndex][destinationIndex] = true;
        }

        boolean undirectedMode = edgeCreationMode == EdgeCreationMode.UNDIRECTED_PAIRED || shouldTreatScopeAsUndirected(hadEdge);

        List<Edge> toRemove = new ArrayList<>();
        for (Edge edge : graph.getEdges()) {
            if (indexById.containsKey(edge.getSource().getId())
                    && indexById.containsKey(edge.getDestination().getId())) {
                toRemove.add(edge);
            }
        }
        int removed = undirectedMode ? countLogicalUndirectedEdges(toRemove) : toRemove.size();
        for (Edge edge : toRemove) {
            graph.removeEdge(edge);
            edgeBends.remove(edgeKey(edge));
        }

        int added = 0;
        if (undirectedMode) {
            for (int i = 0; i < scopeVertices.size(); i++) {
                for (int j = i + 1; j < scopeVertices.size(); j++) {
                    if (!hadEdge[i][j] && !hadEdge[j][i]) {
                        graph.addEdge(scopeVertices.get(i), scopeVertices.get(j), newEdgeWeight, newEdgeColor);
                        if (edgeCreationMode == EdgeCreationMode.UNDIRECTED_PAIRED) {
                            graph.addEdge(scopeVertices.get(j), scopeVertices.get(i), newEdgeWeight, newEdgeColor);
                        }
                        added++;
                    }
                }
            }
        } else {
            for (int i = 0; i < scopeVertices.size(); i++) {
                for (int j = 0; j < scopeVertices.size(); j++) {
                    if (i == j) {
                        continue;
                    }
                    if (!hadEdge[i][j]) {
                        graph.addEdge(scopeVertices.get(i), scopeVertices.get(j), newEdgeWeight, newEdgeColor);
                        added++;
                    }
                }
            }
        }

        selectedEdge = null;
        notifyChanged();
        notifyModified();
        return new ComplementGraphResult(removed, added, undirectedMode);
    }

    private boolean shouldTreatScopeAsUndirected(boolean[][] hadEdge) {
        int n = hadEdge.length;
        boolean sawAnyEdge = false;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    continue;
                }
                if (hadEdge[i][j]) {
                    sawAnyEdge = true;
                }
                if (hadEdge[i][j] != hadEdge[j][i]) {
                    return false;
                }
            }
        }
        // Empty selections default to directed semantics in this editor.
        return sawAnyEdge;
    }

    /**
     * Replaces the whole current graph with the line graph of the selected subgraph.
     *
     * In this transformation:
     * - each selected original edge becomes one new vertex
     * - two new vertices are connected iff their original edges share an endpoint
     * - colors/weights/markings are reset to defaults on the new graph
     */
    public LineGraphResult turnSelectedSubgraphIntoLineGraph() {
        List<Edge> originalEdges = new ArrayList<>(getSelectedScopeEdgesForCurrentMode());
        int originalEdgeCount = originalEdges.size();

        graph.clear();
        positions.clear();
        edgeBends.clear();
        selectedVertexIds.clear();
        selectedEdge = null;

        if (originalEdgeCount == 0) {
            notifyChanged();
            notifyModified();
            return new LineGraphResult(0, 0, 0);
        }

        List<Vertex> lineVertices = new ArrayList<>();
        for (int i = 0; i < originalEdgeCount; i++) {
            Vertex vertex = graph.addVertex(i);
            vertex.setColor(0);
            vertex.setName("L" + (i + 1));
            lineVertices.add(vertex);
        }
        applyAutoCirclePositions(lineVertices);

        int newEdgeCount = 0;
        for (int i = 0; i < originalEdges.size(); i++) {
            for (int j = i + 1; j < originalEdges.size(); j++) {
                if (shareEndpoint(originalEdges.get(i), originalEdges.get(j))) {
                    graph.addEdge(lineVertices.get(i), lineVertices.get(j), 1, 0);
                    newEdgeCount++;
                }
            }
        }

        notifyChanged();
        notifyModified();
        return new LineGraphResult(originalEdgeCount, lineVertices.size(), newEdgeCount);
    }

    /**
     * Attempts to interpret the selected subgraph as a line graph and reconstructs a root graph.
     *
     * If recognition fails, the current graph remains unchanged.
     */
    public RootGraphResult findRootGraphFromSelectedLineGraph() {
        List<Vertex> scopeVertices = getSelectedScopeVertices();
        if (scopeVertices.isEmpty()) {
            return RootGraphResult.failure("Select at least one vertex first.");
        }

        Map<Integer, Integer> indexById = new LinkedHashMap<>();
        for (int i = 0; i < scopeVertices.size(); i++) {
            indexById.put(scopeVertices.get(i).getId(), i);
        }

        boolean[][] adjacency = new boolean[scopeVertices.size()][scopeVertices.size()];
        for (Edge edge : getSelectedScopeEdges()) {
            Integer source = indexById.get(edge.getSource().getId());
            Integer destination = indexById.get(edge.getDestination().getId());
            if (source == null || destination == null) {
                continue;
            }
            if (source.equals(destination)) {
                return RootGraphResult.failure("Find Root Graph supports only simple graphs (no self-loops).");
            }
            // Project editor edges to a simple undirected interpretation for recognition.
            adjacency[source][destination] = true;
            adjacency[destination][source] = true;
        }

        LineGraphRootService.ReconstructionResult reconstruction = lineGraphRootService.reconstruct(adjacency);
        if (!reconstruction.isSuccess()) {
            return RootGraphResult.failure(reconstruction.getMessage());
        }

        graph.clear();
        positions.clear();
        edgeBends.clear();
        selectedVertexIds.clear();
        selectedEdge = null;

        List<Vertex> rebuiltVertices = new ArrayList<>();
        for (int i = 0; i < reconstruction.getRootVertexCount(); i++) {
            Vertex vertex = graph.addVertex(i);
            vertex.setColor(0);
            rebuiltVertices.add(vertex);
        }
        applyAutoCirclePositions(rebuiltVertices);

        for (LineGraphRootService.IntEdge rootEdge : reconstruction.getRootEdges()) {
            Vertex source = graph.getVertex(rootEdge.getA());
            Vertex destination = graph.getVertex(rootEdge.getB());
            if (source != null && destination != null) {
                graph.addEdge(source, destination, 1, 0);
            }
        }

        notifyChanged();
        notifyModified();
        return RootGraphResult.success(rebuiltVertices.size(), reconstruction.getRootEdges().size());
    }

    private void applyAutoCirclePositions(List<Vertex> vertices) {
        if (vertices.isEmpty()) {
            return;
        }
        if (vertices.size() == 1) {
            positions.put(vertices.get(0).getId(), new Point2D.Double(420, 300));
            return;
        }

        double radius = Math.max(140.0, 28.0 * vertices.size());
        double centerX = 420.0;
        double centerY = 300.0;
        for (int i = 0; i < vertices.size(); i++) {
            double angle = (2.0 * Math.PI * i) / vertices.size();
            positions.put(
                    vertices.get(i).getId(),
                    new Point2D.Double(
                            centerX + radius * Math.cos(angle),
                            centerY + radius * Math.sin(angle)
                    )
            );
        }
    }

    private boolean shareEndpoint(Edge first, Edge second) {
        int a = first.getSource().getId();
        int b = first.getDestination().getId();
        int c = second.getSource().getId();
        int d = second.getDestination().getId();
        return a == c || a == d || b == c || b == d;
    }

    private int countLogicalUndirectedEdges(Collection<Edge> edges) {
        Set<Long> pairs = new LinkedHashSet<>();
        int loops = 0;
        for (Edge edge : edges) {
            int u = edge.getSource().getId();
            int v = edge.getDestination().getId();
            if (u == v) {
                loops++;
                continue;
            }
            int a = Math.min(u, v);
            int b = Math.max(u, v);
            pairs.add(edgeKey(a, b));
        }
        return pairs.size() + loops;
    }

    public void setSelectedEdgeColor(int rgb) {
        if (selectedEdge == null) {
            return;
        }
        selectedEdge.setColor(rgb);
        if (edgeCreationMode == EdgeCreationMode.UNDIRECTED_PAIRED) {
            Edge reverse = pairedEdge(selectedEdge);
            if (reverse != null) {
                reverse.setColor(rgb);
            }
        }
        notifyChanged();
        notifyModified();
    }

    public void setSelectedEdgeWeight(int weight) {
        if (selectedEdge == null) {
            return;
        }
        selectedEdge.setWeight(weight);
        if (edgeCreationMode == EdgeCreationMode.UNDIRECTED_PAIRED) {
            Edge reverse = pairedEdge(selectedEdge);
            if (reverse != null) {
                reverse.setWeight(weight);
            }
        }
        notifyChanged();
        notifyModified();
    }

    public void deleteSelection() {
        if (selectedEdge != null) {
            removeEdge(selectedEdge);
            return;
        }

        if (selectedVertexIds.isEmpty()) {
            return;
        }

        List<Integer> idsToDelete = new ArrayList<>(selectedVertexIds);
        selectedVertexIds.clear();
        for (Integer id : idsToDelete) {
            Vertex vertex = graph.getVertex(id);
            if (vertex != null) {
                graph.removeVertex(vertex);
                positions.remove(id);
            }
        }
        pruneEdgeBends();
        notifyChanged();
        notifyModified();
    }

    public void clearGraph() {
        graph.clear();
        positions.clear();
        edgeBends.clear();
        selectedVertexIds.clear();
        selectedEdge = null;
        notifyChanged();
        notifyModified();
    }

    public Set<Integer> getSelectedVertexIds() {
        return Collections.unmodifiableSet(selectedVertexIds);
    }

    public Edge getSelectedEdge() {
        return selectedEdge;
    }

    public Point2D.Double getPosition(Vertex vertex) {
        return positions.get(vertex.getId());
    }

    public boolean isSelected(Vertex vertex) {
        return vertex != null && selectedVertexIds.contains(vertex.getId());
    }

    public boolean hasGraphContent() {
        return graph.vertexCount() > 0 || graph.edgeCount() > 0;
    }

    public void loadGraphTemplate(GraphTemplate template) {
        if (template == null) {
            return;
        }

        graph.clear();
        positions.clear();
        edgeBends.clear();
        selectedVertexIds.clear();
        selectedEdge = null;

        for (Map.Entry<Integer, Point2D.Double> entry : template.getPositions().entrySet()) {
            int id = entry.getKey();
            graph.addVertex(id);
            Point2D.Double p = entry.getValue();
            positions.put(id, new Point2D.Double(p.x * 900.0, p.y * 620.0));
        }

        for (GraphTemplate.TemplateEdge e : template.getEdges()) {
            Vertex source = graph.getVertex(e.getSourceId());
            Vertex destination = graph.getVertex(e.getDestinationId());
            if (source != null && destination != null) {
                graph.addEdge(source, destination, e.getWeight(), e.getColor());
            }
        }

        edgeBends.clear();

        notifyChanged();
        notifyModified();
    }

    /**
     * Creates a full in-memory snapshot of the current graph/editor state.
     * Used by flow-network mode to restore the original graph after visualization.
     */
    public GraphStateSnapshot exportGraphState() {
        Map<Integer, VertexState> vertexStates = new LinkedHashMap<>();
        for (Vertex vertex : graph.getVertices()) {
            Point2D.Double p = positions.get(vertex.getId());
            Point2D.Double copy = p == null ? new Point2D.Double(0, 0) : new Point2D.Double(p.x, p.y);
            vertexStates.put(vertex.getId(), new VertexState(vertex.getId(), vertex.getName(), vertex.getColor(), copy));
        }

        List<EdgeState> edgeStates = new ArrayList<>();
        for (Edge edge : graph.getEdges()) {
            Point2D.Double offset = edgeBends.get(edgeKey(edge));
            edgeStates.add(new EdgeState(
                    edge.getSource().getId(),
                    edge.getDestination().getId(),
                    edge.getWeight(),
                    edge.getColor(),
                    offset == null ? 0.0 : offset.x,
                    offset == null ? 0.0 : offset.y
            ));
        }

        return new GraphStateSnapshot(vertexStates, edgeStates, edgeBends);
    }

    /** Restores graph/editor state from a snapshot created by {@link #exportGraphState()}. */
    public void restoreGraphState(GraphStateSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        graph.clear();
        positions.clear();
        edgeBends.clear();
        selectedVertexIds.clear();
        selectedEdge = null;

        for (VertexState state : snapshot.vertices.values()) {
            Vertex vertex = graph.addVertex(state.id);
            vertex.setName(state.name);
            vertex.setColor(state.color);
            positions.put(state.id, new Point2D.Double(state.position.x, state.position.y));
        }

        for (EdgeState edge : snapshot.edges) {
            Vertex source = graph.getVertex(edge.sourceId);
            Vertex destination = graph.getVertex(edge.destinationId);
            if (source != null && destination != null) {
                graph.addEdge(source, destination, edge.weight, edge.color);
                if (Math.hypot(edge.bendX, edge.bendY) >= 1.0) {
                    edgeBends.put(edgeKey(edge.sourceId, edge.destinationId), new Point2D.Double(edge.bendX, edge.bendY));
                }
            }
        }

        for (Map.Entry<Long, Point2D.Double> entry : snapshot.edgeBends.entrySet()) {
            long key = entry.getKey();
            if (edgeBends.containsKey(key)) {
                continue;
            }
            int sourceId = sourceIdFromKey(key);
            int destinationId = destinationIdFromKey(key);
            Point2D.Double value = entry.getValue();
            if (graph.containsEdge(sourceId, destinationId)
                    && value != null
                    && Math.hypot(value.x, value.y) >= 1.0) {
                edgeBends.put(key, new Point2D.Double(value.x, value.y));
            }
        }

        notifyChanged();
        notifyModified();
    }

    /** Immutable snapshot container for restore-after-playback flows. */
    public static final class GraphStateSnapshot {
        private final Map<Integer, VertexState> vertices;
        private final List<EdgeState> edges;
        private final Map<Long, Point2D.Double> edgeBends;

        private GraphStateSnapshot(Map<Integer, VertexState> vertices, List<EdgeState> edges, Map<Long, Point2D.Double> edgeBends) {
            this.vertices = Collections.unmodifiableMap(new LinkedHashMap<>(vertices));
            this.edges = Collections.unmodifiableList(new ArrayList<>(edges));
            Map<Long, Point2D.Double> copied = new LinkedHashMap<>();
            for (Map.Entry<Long, Point2D.Double> entry : edgeBends.entrySet()) {
                Point2D.Double value = entry.getValue();
                copied.put(entry.getKey(), value == null ? null : new Point2D.Double(value.x, value.y));
            }
            this.edgeBends = Collections.unmodifiableMap(copied);
        }
    }

    /** Summary of a complement transformation. */
    public static final class ComplementGraphResult {
        private final int removedEdges;
        private final int addedEdges;
        private final boolean undirectedMode;

        private ComplementGraphResult(int removedEdges, int addedEdges, boolean undirectedMode) {
            this.removedEdges = removedEdges;
            this.addedEdges = addedEdges;
            this.undirectedMode = undirectedMode;
        }

        public int getRemovedEdges() {
            return removedEdges;
        }

        public int getAddedEdges() {
            return addedEdges;
        }

        public String getModeLabel() {
            return undirectedMode ? "undirected" : "directed";
        }
    }

    /** Summary of a line-graph transformation. */
    public static final class LineGraphResult {
        private final int originalEdgeCount;
        private final int newVertexCount;
        private final int newEdgeCount;

        private LineGraphResult(int originalEdgeCount, int newVertexCount, int newEdgeCount) {
            this.originalEdgeCount = originalEdgeCount;
            this.newVertexCount = newVertexCount;
            this.newEdgeCount = newEdgeCount;
        }

        public int getOriginalEdgeCount() {
            return originalEdgeCount;
        }

        public int getNewVertexCount() {
            return newVertexCount;
        }

        public int getNewEdgeCount() {
            return newEdgeCount;
        }
    }

    /** Summary of a Find Root Graph operation. */
    public static final class RootGraphResult {
        private final boolean success;
        private final String message;
        private final int newVertexCount;
        private final int newEdgeCount;

        private RootGraphResult(boolean success, String message, int newVertexCount, int newEdgeCount) {
            this.success = success;
            this.message = message;
            this.newVertexCount = newVertexCount;
            this.newEdgeCount = newEdgeCount;
        }

        public static RootGraphResult success(int newVertexCount, int newEdgeCount) {
            return new RootGraphResult(true, "", newVertexCount, newEdgeCount);
        }

        public static RootGraphResult failure(String message) {
            return new RootGraphResult(false, message, 0, 0);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getNewVertexCount() {
            return newVertexCount;
        }

        public int getNewEdgeCount() {
            return newEdgeCount;
        }
    }

    private static final class VertexState {
        private final int id;
        private final String name;
        private final int color;
        private final Point2D.Double position;

        private VertexState(int id, String name, int color, Point2D.Double position) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.position = position;
        }
    }

    private static final class SubgraphClipboard {
        private final List<ClipboardVertex> vertices;
        private final List<ClipboardEdge> edges;
        private final double centerX;
        private final double centerY;

        private SubgraphClipboard(List<ClipboardVertex> vertices, List<ClipboardEdge> edges, double centerX, double centerY) {
            this.vertices = Collections.unmodifiableList(new ArrayList<>(vertices));
            this.edges = Collections.unmodifiableList(new ArrayList<>(edges));
            this.centerX = centerX;
            this.centerY = centerY;
        }
    }

    private static final class ClipboardVertex {
        private final int oldId;
        private final String name;
        private final int color;
        private final double relativeX;
        private final double relativeY;

        private ClipboardVertex(int oldId, String name, int color, double relativeX, double relativeY) {
            this.oldId = oldId;
            this.name = name;
            this.color = color;
            this.relativeX = relativeX;
            this.relativeY = relativeY;
        }
    }

    private static final class ClipboardEdge {
        private final int sourceId;
        private final int destinationId;
        private final int weight;
        private final int color;
        private final double bendX;
        private final double bendY;

        private ClipboardEdge(int sourceId, int destinationId, int weight, int color, double bendX, double bendY) {
            this.sourceId = sourceId;
            this.destinationId = destinationId;
            this.weight = weight;
            this.color = color;
            this.bendX = bendX;
            this.bendY = bendY;
        }
    }

    private static final class EdgeState {
        private final int sourceId;
        private final int destinationId;
        private final int weight;
        private final int color;
        private final double bendX;
        private final double bendY;

        private EdgeState(int sourceId, int destinationId, int weight, int color, double bendX, double bendY) {
            this.sourceId = sourceId;
            this.destinationId = destinationId;
            this.weight = weight;
            this.color = color;
            this.bendX = bendX;
            this.bendY = bendY;
        }
    }

    private Edge pairedEdge(Edge edge) {
        if (edge == null) {
            return null;
        }
        int u = edge.getSource().getId();
        int v = edge.getDestination().getId();
        if (u == v) {
            return null;
        }
        return graph.getEdge(v, u);
    }

    private void syncPairManualBend(Edge forward, Edge reverse) {
        if (forward == null || reverse == null) {
            return;
        }
        Point2D.Double bend = edgeBends.get(edgeKey(forward));
        if (bend == null) {
            return;
        }
        edgeBends.put(edgeKey(reverse), new Point2D.Double(-bend.x, -bend.y));
    }

    private void normalizeToUndirectedPairs() {
        boolean changed = false;
        List<Edge> snapshot = new ArrayList<>(graph.getEdges());
        for (Edge edge : snapshot) {
            int u = edge.getSource().getId();
            int v = edge.getDestination().getId();
            if (u == v) {
                continue;
            }
            Edge reverse = graph.getEdge(v, u);
            if (reverse == null) {
                reverse = graph.addEdge(v, u, edge.getWeight(), edge.getColor());
                changed = true;
            }
            if (reverse.getWeight() != edge.getWeight()) {
                reverse.setWeight(edge.getWeight());
                changed = true;
            }
            if (reverse.getColor() != edge.getColor()) {
                reverse.setColor(edge.getColor());
                changed = true;
            }
            syncPairManualBend(edge, reverse);
        }
        if (changed) {
            notifyChanged();
            notifyModified();
        } else {
            notifyChanged();
        }
    }

    private void pruneEdgeBends() {
        List<Long> stale = new ArrayList<>();
        for (Long key : edgeBends.keySet()) {
            int sourceId = sourceIdFromKey(key);
            int destinationId = destinationIdFromKey(key);
            if (!graph.containsEdge(sourceId, destinationId)) {
                stale.add(key);
            }
        }
        for (Long key : stale) {
            edgeBends.remove(key);
        }
    }

    private static long edgeKey(Edge edge) {
        return edgeKey(edge.getSource().getId(), edge.getDestination().getId());
    }

    private static long edgeKey(int sourceId, int destinationId) {
        return (((long) sourceId) << 32) | (destinationId & 0xffffffffL);
    }

    private static int sourceIdFromKey(long key) {
        return (int) (key >> 32);
    }

    private static int destinationIdFromKey(long key) {
        return (int) key;
    }

    private int nextVertexId() {
        int maxId = -1;
        for (Vertex vertex : graph.getVertices()) {
            maxId = Math.max(maxId, vertex.getId());
        }
        return maxId + 1;
    }

    private void notifyChanged() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    private void notifyModified() {
        if (!suspendHistory) {
            if (lastStableSnapshot == null) {
                lastStableSnapshot = exportGraphState();
            }
            undoStack.push(lastStableSnapshot);
            while (undoStack.size() > MAX_UNDO_STATES) {
                undoStack.removeLast();
            }
            lastStableSnapshot = exportGraphState();
        }
        for (Runnable listener : modificationListeners) {
            listener.run();
        }
    }
}
