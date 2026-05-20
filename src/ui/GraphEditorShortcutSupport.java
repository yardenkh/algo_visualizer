package ui;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;

/**
 * Centralizes keyboard shortcut registration for {@link GraphEditorFrame}.
 */
final class GraphEditorShortcutSupport {
    private GraphEditorShortcutSupport() {
    }

    static final class Bindings {
        JRootPane rootPane;

        JButton runAlgorithmButton;
        JButton shortcutsButton;
        JButton premadeGraphsButton;
        JButton extrasButton;
        JButton clearGraphButton;

        JToggleButton selectToolButton;
        JToggleButton addVertexToolButton;
        JToggleButton connectToolButton;
        JToggleButton deleteToolButton;
        JToggleButton panToolButton;

        Runnable onPlaybackStepBackward;
        Runnable onPlaybackStepForward;
        Runnable onPlaybackStop;
        Runnable onResetView;
        Runnable onPromptSelectedEdgeWeight;
        Runnable onAdjustDefaultEdgeWeightUp;
        Runnable onAdjustDefaultEdgeWeightDown;
        Runnable onApplyCircleLayout;
        Runnable onChooseDefaultEdgeColor;
        Runnable onColorVertices;
        Runnable onColorEdge;
        Runnable onUndo;
        Runnable onCopySelection;
        Runnable onPasteSelection;
    }

    static void install(Bindings bindings) {
        InputMap inputMap = bindings.rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "playback-backward");
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "playback-forward");
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "playback-stop");

        inputMap.put(KeyStroke.getKeyStroke("S"), "tool-select");
        inputMap.put(KeyStroke.getKeyStroke("V"), "tool-add-vertex");
        inputMap.put(KeyStroke.getKeyStroke("C"), "tool-connect");
        inputMap.put(KeyStroke.getKeyStroke("X"), "tool-delete");
        inputMap.put(KeyStroke.getKeyStroke("P"), "tool-pan");
        inputMap.put(KeyStroke.getKeyStroke("0"), "view-reset");

        inputMap.put(KeyStroke.getKeyStroke("R"), "algorithm-run");
        inputMap.put(KeyStroke.getKeyStroke("F1"), "show-shortcuts");
        inputMap.put(KeyStroke.getKeyStroke("W"), "edge-weight-dialog");
        inputMap.put(KeyStroke.getKeyStroke("UP"), "default-edge-weight-up");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "default-edge-weight-down");

        inputMap.put(KeyStroke.getKeyStroke("control L"), "layout-circle");
        inputMap.put(KeyStroke.getKeyStroke("control E"), "default-edge-color");
        inputMap.put(KeyStroke.getKeyStroke("control shift V"), "color-vertices");
        inputMap.put(KeyStroke.getKeyStroke("control shift E"), "color-edge");
        inputMap.put(KeyStroke.getKeyStroke("control shift O"), "open-extras");
        inputMap.put(KeyStroke.getKeyStroke("control shift BACK_SPACE"), "clear-graph");
        inputMap.put(KeyStroke.getKeyStroke("control G"), "premade-graphs");
        inputMap.put(KeyStroke.getKeyStroke("control Z"), "undo-graph-edit");
        inputMap.put(KeyStroke.getKeyStroke("control C"), "copy-selection");
        inputMap.put(KeyStroke.getKeyStroke("control V"), "paste-selection");

        ActionMap actionMap = bindings.rootPane.getActionMap();
        actionMap.put("playback-backward", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.onPlaybackStepBackward.run();
            }
        });
        actionMap.put("playback-forward", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.onPlaybackStepForward.run();
            }
        });
        actionMap.put("playback-stop", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.onPlaybackStop.run();
            }
        });

        actionMap.put("tool-select", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.selectToolButton.doClick();
            }
        });
        actionMap.put("tool-add-vertex", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.addVertexToolButton.doClick();
            }
        });
        actionMap.put("tool-connect", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.connectToolButton.doClick();
            }
        });
        actionMap.put("tool-delete", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.deleteToolButton.doClick();
            }
        });
        actionMap.put("tool-pan", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.panToolButton.doClick();
            }
        });

        actionMap.put("view-reset", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.onResetView.run();
            }
        });

        actionMap.put("algorithm-run", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.runAlgorithmButton.doClick();
            }
        });
        actionMap.put("show-shortcuts", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.shortcutsButton.doClick();
            }
        });
        actionMap.put("clear-graph", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.clearGraphButton.doClick();
            }
        });
        actionMap.put("layout-circle", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.onApplyCircleLayout.run();
            }
        });
        actionMap.put("default-edge-color", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.onChooseDefaultEdgeColor.run();
            }
        });
        actionMap.put("color-vertices", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.onColorVertices.run();
            }
        });
        actionMap.put("color-edge", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.onColorEdge.run();
            }
        });
        actionMap.put("edge-weight-dialog", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.onPromptSelectedEdgeWeight.run();
            }
        });
        actionMap.put("default-edge-weight-up", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.onAdjustDefaultEdgeWeightUp.run();
            }
        });
        actionMap.put("default-edge-weight-down", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.onAdjustDefaultEdgeWeightDown.run();
            }
        });
        actionMap.put("premade-graphs", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.premadeGraphsButton.doClick();
            }
        });
        actionMap.put("open-extras", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.extrasButton.doClick();
            }
        });
        actionMap.put("undo-graph-edit", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.onUndo.run();
            }
        });
        actionMap.put("copy-selection", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.onCopySelection.run();
            }
        });
        actionMap.put("paste-selection", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindings.onPasteSelection.run();
            }
        });
    }

    static String buildShortcutsText() {
        return String.join(System.lineSeparator(),
                "Global",
                "  F1                      Open shortcuts window",
                "  Ctrl+G                  Open premade graph library",
                "  R                       Run algorithm dialog",
                "  Left / Right            Step backward / forward",
                "  Esc                     Stop playback",
                "  Ctrl+Z                  Undo last graph edit",
                "  Ctrl+C                  Copy selected subgraph",
                "  Ctrl+V                  Paste copied subgraph",
                "  Delete / Backspace      Delete selection",
                "",
                "Tools",
                "  S                       Select tool",
                "  V                       Add Vertex tool",
                "  C                       Connect tool",
                "  X                       Delete tool",
                "  P                       Pan tool",
                "  0                       Reset view",
                "",
                "Graph Actions",
                "  Ctrl+Shift+Backspace    Clear graph",
                "  Ctrl+L                  Auto layout (circle)",
                "  Ctrl+E                  Default edge color",
                "  Ctrl+Shift+V            Color selected vertices",
                "  Ctrl+Shift+E            Color selected edge",
                "  Ctrl+Shift+O            Open Extras actions",
                "  W                       Set selected edge weight",
                "",
                "Canvas",
                "  Ctrl+A                  Select all vertices",
                "  Middle mouse drag       Pan",
                "  Mouse wheel             Zoom"
        );
    }
}

