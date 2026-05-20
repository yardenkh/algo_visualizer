package ui.extras;

import ui.GraphEditorController;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Handles Selection->Extras->Complement Graph action.
 */
public final class ComplementGraphActionHandler {
    private final Component parent;
    private final GraphEditorController controller;

    public ComplementGraphActionHandler(Component parent, GraphEditorController controller) {
        this.parent = parent;
        this.controller = controller;
    }

    public void run() {
        if (!controller.hasSelectedSubgraph()) {
            JOptionPane.showMessageDialog(parent, "Select vertices first to define the graph scope.");
            return;
        }

        GraphEditorController.ComplementGraphResult result = controller.complementSelectedSubgraph(1, 0);
        JOptionPane.showMessageDialog(
                parent,
                "Complement graph applied to selected subgraph. Removed "
                        + result.getRemovedEdges()
                        + " edge(s), added "
                        + result.getAddedEdges()
                        + " edge(s) [mode: "
                        + result.getModeLabel()
                        + "].",
                "Complement Graph",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}

