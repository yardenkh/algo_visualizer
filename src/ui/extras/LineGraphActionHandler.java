package ui.extras;

import ui.GraphEditorController;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Handles Selection->Extras->Turn Into Line Graph action.
 */
public final class LineGraphActionHandler {
    private final Component parent;
    private final GraphEditorController controller;

    public LineGraphActionHandler(Component parent, GraphEditorController controller) {
        this.parent = parent;
        this.controller = controller;
    }

    public void run() {
        if (!controller.hasSelectedSubgraph()) {
            JOptionPane.showMessageDialog(parent, "Select vertices first to define the graph scope.");
            return;
        }

        GraphEditorController.LineGraphResult result = controller.turnSelectedSubgraphIntoLineGraph();
        JOptionPane.showMessageDialog(
                parent,
                "Line graph created. Original edges: "
                        + result.getOriginalEdgeCount()
                        + ", new vertices: "
                        + result.getNewVertexCount()
                        + ", new edges: "
                        + result.getNewEdgeCount()
                        + ".",
                "Turn Into Line Graph",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}

