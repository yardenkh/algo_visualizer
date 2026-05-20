package ui.extras;

import graph.Edge;
import graph.Vertex;
import ui.GraphEditorController;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles Selection->Extras->Reduce Edge Crossings action.
 */
public final class ReduceEdgeCrossingsActionHandler {
    private final Component parent;
    private final GraphEditorController controller;
    private final EdgeCrossingReductionService service;

    public ReduceEdgeCrossingsActionHandler(Component parent, GraphEditorController controller) {
        this.parent = parent;
        this.controller = controller;
        this.service = new EdgeCrossingReductionService();
    }

    public void run() {
        if (!controller.hasSelectedSubgraph()) {
            JOptionPane.showMessageDialog(parent, "Select vertices first to define the graph scope.");
            return;
        }

        List<Vertex> vertices = controller.getSelectedScopeVertices();
        List<Edge> edges = controller.getSelectedScopeEdgesForCurrentMode();
        Map<Integer, Point2D.Double> positions = new LinkedHashMap<>();
        for (Vertex vertex : vertices) {
            Point2D.Double p = controller.getPosition(vertex);
            if (p != null) {
                positions.put(vertex.getId(), new Point2D.Double(p.x, p.y));
            }
        }

        EdgeCrossingReductionService.Result result = service.improveLayout(vertices, edges, positions);
        if (result.improved()) {
            // Predictable behavior: reset manual edge controls in scope before applying the new embedding.
            controller.clearManualEdgeControls(edges, false);
            controller.applyVertexPositions(result.getPositions());
        }

        String message = "Crossings before: " + result.getCrossingsBefore()
                + " | after: " + result.getCrossingsAfter();
        message += System.lineSeparator() + "Planarity check: " + switch (result.getPlanarityStatus()) {
            case PLANAR -> "planar (or found planar embedding)";
            case NON_PLANAR -> "non-planar by fast test";
            case UNKNOWN -> "undetermined by fast test";
        };
        message += System.lineSeparator() + "Layout strategy: " + result.getStrategy();
        if (!result.improved()) {
            message += System.lineSeparator() + "No lower-crossing layout was found; graph was left unchanged.";
        }
        JOptionPane.showMessageDialog(
                parent,
                message,
                "Reduce Edge Crossings",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}


