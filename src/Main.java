import ui.GraphEditorFrame;

import javax.swing.SwingUtilities;

/**
 * Application entry point.
 *
 * Starts the Swing UI on the Event Dispatch Thread and shows
 * the main editor window.
 */
public class Main {
    /**
     * Boots the graph editor desktop application.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GraphEditorFrame frame = new GraphEditorFrame();
            frame.setVisible(true);
        });
    }
}