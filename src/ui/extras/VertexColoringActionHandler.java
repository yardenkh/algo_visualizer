package ui.extras;

import algorithm.coloring.VertexColoringService;
import graph.Vertex;
import ui.GraphEditorController;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles Selection->Extras->Find Vertex Coloring action.
 */
public final class VertexColoringActionHandler {
    private final Component parent;
    private final GraphEditorController controller;
    private final VertexColoringService service;

    public VertexColoringActionHandler(Component parent, GraphEditorController controller) {
        this.parent = parent;
        this.controller = controller;
        this.service = new VertexColoringService();
    }

    public void run() {
        List<Vertex> vertices = controller.getSelectedScopeVertices();
        if (vertices.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Select at least one vertex first.");
            return;
        }

        try {
            VertexColoringService.Result result = service.compute(vertices, controller.getSelectedScopeEdgesForCurrentMode());
            Map<Integer, Integer> rgbByVertex = new LinkedHashMap<>();
            for (Map.Entry<Integer, Integer> entry : result.getColorByVertexId().entrySet()) {
                rgbByVertex.put(entry.getKey(), ColorClassPalette.rgbFor(entry.getValue()));
            }
            controller.applyVertexColors(rgbByVertex);
            JOptionPane.showMessageDialog(
                    parent,
                    "Vertex coloring complete. Used " + result.getColorCount() + " color(s).",
                    "Vertex Coloring",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "Vertex Coloring", JOptionPane.WARNING_MESSAGE);
        }
    }
}

