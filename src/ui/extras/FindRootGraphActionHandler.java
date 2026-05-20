package ui.extras;

import ui.GraphEditorController;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Handles Selection->Extras->Find Root Graph action.
 */
public final class FindRootGraphActionHandler {
    private final Component parent;
    private final GraphEditorController controller;

    public FindRootGraphActionHandler(Component parent, GraphEditorController controller) {
        this.parent = parent;
        this.controller = controller;
    }

    public void run() {
        if (!controller.hasSelectedSubgraph()) {
            JOptionPane.showMessageDialog(parent, "Select vertices first to define the graph scope.");
            return;
        }

        GraphEditorController.RootGraphResult result = controller.findRootGraphFromSelectedLineGraph();
        if (!result.isSuccess()) {
            JOptionPane.showMessageDialog(
                    parent,
                    result.getMessage(),
                    "Find Root Graph",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JOptionPane.showMessageDialog(
                parent,
                "Root graph reconstructed. New graph has "
                        + result.getNewVertexCount()
                        + " vertex/vertices and "
                        + result.getNewEdgeCount()
                        + " edge(s).",
                "Find Root Graph",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}

