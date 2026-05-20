package ui.extras;

import algorithm.coloring.EdgeColoringService;
import graph.Edge;
import ui.GraphEditorController;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles Selection->Extras->Find Edge Coloring action.
 */
public final class EdgeColoringActionHandler {
    private final Component parent;
    private final GraphEditorController controller;
    private final EdgeColoringService service;

    public EdgeColoringActionHandler(Component parent, GraphEditorController controller) {
        this.parent = parent;
        this.controller = controller;
        this.service = new EdgeColoringService();
    }

    public void run() {
        if (!controller.hasSelectedSubgraph()) {
            JOptionPane.showMessageDialog(parent, "Select vertices first to define the graph scope.");
            return;
        }
        List<Edge> edges = controller.getSelectedScopeEdgesForCurrentMode();
        if (edges.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "The selected subgraph has no edges to color.");
            return;
        }

        try {
            EdgeColoringService.Result result = service.compute(edges);
            Map<Edge, Integer> rgbByEdge = new LinkedHashMap<>();
            for (Map.Entry<Edge, Integer> entry : result.getColorByEdge().entrySet()) {
                rgbByEdge.put(entry.getKey(), ColorClassPalette.rgbFor(entry.getValue()));
            }
            controller.applyEdgeColors(rgbByEdge);
            JOptionPane.showMessageDialog(
                    parent,
                    "Edge coloring complete. Used " + result.getColorCount() + " color(s).",
                    "Edge Coloring",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "Edge Coloring", JOptionPane.WARNING_MESSAGE);
        }
    }
}

