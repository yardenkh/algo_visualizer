package ui.extras;

import graph.Vertex;
import ui.GraphEditorController;
import ui.dialogs.RenameVertexDialog;

import java.awt.Component;
import java.util.List;

/**
 * Handles the Selection Actions rename flow for selected vertices.
 */
public final class ChangeVertexNameActionHandler {
    private final Component parent;
    private final GraphEditorController controller;

    public ChangeVertexNameActionHandler(Component parent, GraphEditorController controller) {
        this.parent = parent;
        this.controller = controller;
    }

    public void run() {
        List<Vertex> selected = controller.getSelectedScopeVertices();
        if (selected.isEmpty()) {
            return;
        }

        RenameVertexDialog dialog = new RenameVertexDialog(parent instanceof javax.swing.JFrame ? (javax.swing.JFrame) parent : null);
        dialog.open();
        if (!dialog.isConfirmed()) {
            return;
        }

        String text = dialog.getEnteredName();
        boolean append = dialog.isAppendSelected();
        if (append && (text == null || text.isEmpty())) {
            return;
        }

        controller.renameSelectedVertices(text, append);
    }
}


