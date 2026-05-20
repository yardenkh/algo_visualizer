package ui;

import algorithm.AlgorithmPlaybackController;
import algorithm.AlgorithmRequest;
import algorithm.AlgorithmResult;
import algorithm.AlgorithmStep;
import algorithm.AlgorithmType;
import algorithm.GraphAlgorithm;
import algorithm.GraphAlgorithmRegistry;
import algorithm.GraphOrientationRequirement;
import algorithm.general.TopologicalOrderAlgorithm;
import graph.Edge;
import graph.Vertex;
import ui.dialogs.AlgorithmMatrixDialog;
import ui.dialogs.AlgorithmPseudocodeDialog;
import ui.dialogs.AlgorithmRunConfiguration;
import ui.dialogs.AlgorithmRunDialog;
import ui.dialogs.AlgorithmStackFramesDialog;
import ui.dialogs.LevelGraphDialog;
import ui.dialogs.PremadeGraphsDialog;
import ui.dialogs.ResidualGraphDialog;
import ui.dialogs.SelectionExtrasDialog;
import ui.dialogs.ShortcutsDialog;
import ui.dialogs.TopologicalOrderDialog;
import ui.extras.ComplementGraphActionHandler;
import ui.extras.EdgeColoringActionHandler;
import ui.extras.FindRootGraphActionHandler;
import ui.extras.LineGraphActionHandler;
import ui.extras.ReduceEdgeCrossingsActionHandler;
import ui.extras.VertexColoringActionHandler;
import ui.templates.GraphTemplate;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JSpinner;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Main application window for the graph editor.
 */
public class GraphEditorFrame extends JFrame {
    private final GraphEditorController controller;
    private final AlgorithmPlaybackController playbackController;
    private final GraphCanvasPanel canvas;

    private final JLabel statusLabel;
    private final JButton restoreWorkspaceTabButton;
    private final JSpinner edgeWeightSpinner;
    private final JTextArea algorithmInfoArea;
    private final JButton runAlgorithmButton;
    private final JButton stepBackwardButton;
    private final JButton stopPlaybackButton;
    private final JButton stepForwardButton;
    private Color pendingEdgeColor;
    private final JButton shortcutsButton;
    private final JButton premadeGraphsButton;
    private final JButton extrasButton;
    private final JColorChooser colorChooser;
    private final JToggleButton undirectedModeToggle;
    private final AlgorithmPseudocodeDialog pseudocodeDialog;
    private final ShortcutsDialog shortcutsDialog;
    private final SelectionExtrasDialog extrasDialog;
    private final TopologicalOrderDialog topologicalOrderDialog;
    private final AlgorithmMatrixDialog matrixDialog;
    private final AlgorithmStackFramesDialog stackFramesDialog;
    private final ResidualGraphDialog residualGraphDialog;
    private final LevelGraphDialog levelGraphDialog;
    private final AlgorithmWorkspacePanel workspace;
    private JSplitPane mainSplit;
    private AlgorithmWorkspacePanel.Mode appliedWorkspaceMode;
    private double tabbedDividerProportion = 0.80;
    private double dockedDividerProportion = 0.65;
    private boolean workspaceMinimized;
    private GraphEditorController.GraphStateSnapshot graphBackupBeforeMaxFlow;
    private boolean wasPlaybackActive;

    private JToggleButton selectToolButton;
    private JToggleButton addVertexToolButton;
    private JToggleButton connectToolButton;
    private JToggleButton deleteToolButton;
    private JToggleButton panToolButton;

    private JButton resetViewButton;
    private JButton clearGraphButton;

    private List<Vertex> activeTopologicalOrder;
    private List<Edge> activeTopologicalEdges;
    private final VertexColoringActionHandler vertexColoringActionHandler;
    private final EdgeColoringActionHandler edgeColoringActionHandler;
    private final ComplementGraphActionHandler complementGraphActionHandler;
    private final LineGraphActionHandler lineGraphActionHandler;
    private final FindRootGraphActionHandler findRootGraphActionHandler;
    private final ReduceEdgeCrossingsActionHandler reduceEdgeCrossingsActionHandler;

    public GraphEditorFrame() {
        super("Professional Graph Editor");

        controller = new GraphEditorController();
        playbackController = new AlgorithmPlaybackController();
        canvas = new GraphCanvasPanel(controller, playbackController);
        statusLabel = new JLabel("Ready");
        restoreWorkspaceTabButton = new JButton("Algorithm Workspace");
        edgeWeightSpinner = new JSpinner(new SpinnerNumberModel(1, -1_000_000, 1_000_000, 1));
        algorithmInfoArea = new JTextArea();
        runAlgorithmButton = new JButton("Run Algorithm");
        stepBackwardButton = new JButton("←");
        stopPlaybackButton = new JButton("■");
        stepForwardButton = new JButton("→");
        pendingEdgeColor = Color.DARK_GRAY;
        shortcutsButton = new JButton("Shortcuts");
        premadeGraphsButton = new JButton("Premade Graphs");
        extrasButton = new JButton("Extras");
        colorChooser = new JColorChooser();
        undirectedModeToggle = new JToggleButton("Undirected Creation: OFF");
        pseudocodeDialog = new AlgorithmPseudocodeDialog(this);
        shortcutsDialog = new ShortcutsDialog(this);
        topologicalOrderDialog = new TopologicalOrderDialog(this);
        matrixDialog = new AlgorithmMatrixDialog(this);
        stackFramesDialog = new AlgorithmStackFramesDialog(this);
        residualGraphDialog = new ResidualGraphDialog(this);
        levelGraphDialog = new LevelGraphDialog(this);
        vertexColoringActionHandler = new VertexColoringActionHandler(this, controller);
        edgeColoringActionHandler = new EdgeColoringActionHandler(this, controller);
        complementGraphActionHandler = new ComplementGraphActionHandler(this, controller);
        lineGraphActionHandler = new LineGraphActionHandler(this, controller);
        findRootGraphActionHandler = new FindRootGraphActionHandler(this, controller);
        reduceEdgeCrossingsActionHandler = new ReduceEdgeCrossingsActionHandler(this, controller);
        extrasDialog = new SelectionExtrasDialog(
                this,
                vertexColoringActionHandler::run,
                edgeColoringActionHandler::run,
                complementGraphActionHandler::run,
                lineGraphActionHandler::run,
                findRootGraphActionHandler::run,
                reduceEdgeCrossingsActionHandler::run,
                this::buildSelectedSubgraphSummary
        );

        workspace = new AlgorithmWorkspacePanel(
                this,
                pseudocodeDialog,
                matrixDialog,
                stackFramesDialog,
                residualGraphDialog,
                levelGraphDialog,
                topologicalOrderDialog
        );

        activeTopologicalOrder = List.of();
        activeTopologicalEdges = List.of();
        wasPlaybackActive = false;
        workspaceMinimized = false;
        pseudocodeDialog.setOnCloseRequest(this::stopPlaybackFromUser);
        pseudocodeDialog.setOnOpenSubroutineRequest(this::openSubroutineFromPseudocodeDialog);
        pseudocodeDialog.setOnCloseSubroutineRequest(this::closeSubroutineFromPseudocodeDialog);

        workspace.addModeChangeListener(this::applyWorkspaceMode);
        workspace.addMinimizeListener(this::minimizeWorkspace);

        // Keep edge creation defaults synchronized with the current controls.
        edgeWeightSpinner.addChangeListener(e -> canvas.setPendingEdgeWeight((int) edgeWeightSpinner.getValue()));
        canvas.setPendingEdgeWeight((int) edgeWeightSpinner.getValue());
        canvas.setPendingEdgeColor(pendingEdgeColor.getRGB());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(1180, 760));
        setSize(1400, 860);
        setLocationByPlatform(true);

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildRightPanel(), BorderLayout.EAST);
        add(buildStatusBar(), BorderLayout.SOUTH);
        applyWorkspaceMode(AlgorithmWorkspacePanel.Mode.TABBED);

        controller.addChangeListener(this::refreshStatus);
        controller.addChangeListener(this::refreshExtrasUi);
        // Playback only stops via ESC button or pseudocode-window close.
        playbackController.addChangeListener(() -> {
            boolean activeNow = playbackController.hasActiveSession();
            refreshStatus();
            refreshPlaybackUi();
            syncAllViews();
            if (wasPlaybackActive && !activeNow) {
                cleanupAfterPlaybackTermination();
            }
            maybeRestoreGraphAfterPlaybackStop();
            wasPlaybackActive = activeNow;
        });

        installKeyboardShortcuts();
        shortcutsDialog.setContent(GraphEditorShortcutSupport.buildShortcutsText());
        updateUndirectedToggleText();
        refreshStatus();
        refreshPlaybackUi();
        refreshExtrasUi();
    }

    /** Builds the top toolbar with tool buttons, playback controls, and utility actions. */
    private JToolBar buildToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        javax.swing.ButtonGroup toolGroup = new javax.swing.ButtonGroup();
        selectToolButton = addToolButton(toolbar, "Select", "S", EditorTool.SELECT, true);
        addVertexToolButton = addToolButton(toolbar, "Add Vertex", "V", EditorTool.ADD_VERTEX, false);
        connectToolButton = addToolButton(toolbar, "Connect", "C", EditorTool.CONNECT, false);
        deleteToolButton = addToolButton(toolbar, "Delete", "X", EditorTool.DELETE, false);
        panToolButton = addToolButton(toolbar, "Pan", "P", EditorTool.PAN, false);
        toolGroup.add(selectToolButton);
        toolGroup.add(addVertexToolButton);
        toolGroup.add(connectToolButton);
        toolGroup.add(deleteToolButton);
        toolGroup.add(panToolButton);

        toolbar.addSeparator();

        resetViewButton = new JButton();
        decorateButtonWithShortcut(resetViewButton, "Reset View", "0");
        resetViewButton.addActionListener(e -> canvas.resetView());
        toolbar.add(resetViewButton);

        clearGraphButton = new JButton();
        decorateButtonWithShortcut(clearGraphButton, "Clear Graph", "Ctrl+Shift+Backspace");
        clearGraphButton.addActionListener(e -> confirmAndClearGraph());
        toolbar.add(clearGraphButton);

        toolbar.addSeparator();

        decorateButtonWithShortcut(runAlgorithmButton, "Run Algorithm", "R");
        runAlgorithmButton.addActionListener(this::runAlgorithm);
        toolbar.add(runAlgorithmButton);

        configurePlaybackButton(stepBackwardButton, "Step backward one algorithm state", "Left", e -> playbackController.stepBackward());
        configurePlaybackButton(stopPlaybackButton, "Stop the current algorithm playback", "Esc", e -> stopPlaybackFromUser());
        configurePlaybackButton(stepForwardButton, "Step forward one algorithm state", "Right", e -> playbackController.stepForward());

        toolbar.add(stepBackwardButton);
        toolbar.add(stopPlaybackButton);
        toolbar.add(stepForwardButton);

        toolbar.addSeparator();

        decorateButtonWithShortcut(premadeGraphsButton, "Premade Graphs", "Ctrl+G");
        premadeGraphsButton.addActionListener(e -> openPremadeGraphsDialog());
        toolbar.add(premadeGraphsButton);

        decorateButtonWithShortcut(shortcutsButton, "Shortcuts", "F1");
        shortcutsButton.addActionListener(e -> openShortcutsDialog());
        toolbar.add(shortcutsButton);

        return toolbar;
    }

    /** Builds the right-side inspector/action panel. */
    private JScrollPane buildRightPanel() {
        GraphEditorSidePanelBuilder builder = new GraphEditorSidePanelBuilder(
                this,
                controller,
                this::decorateButtonWithShortcut,
                edgeWeightSpinner,
                undirectedModeToggle,
                extrasButton,
                algorithmInfoArea,
                this::promptChooseDefaultEdgeColor,
                this::toggleUndirectedCreationMode,
                this::promptColorSelectedVertices,
                this::promptColorSelectedEdge,
                this::promptForSelectedEdgeWeight,
                this::applyCircleLayout,
                this::openExtrasDialog
        );
        return builder.build();
    }

    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        restoreWorkspaceTabButton.setMargin(new Insets(1, 8, 1, 8));
        restoreWorkspaceTabButton.setVisible(false);
        restoreWorkspaceTabButton.setToolTipText("Restore algorithm workspace");
        restoreWorkspaceTabButton.addActionListener(e -> restoreWorkspaceFromTab());
        panel.add(restoreWorkspaceTabButton);
        panel.add(statusLabel);
        return panel;
    }

    private JToggleButton addToolButton(JToolBar toolbar, String title, String shortcut, EditorTool tool, boolean selected) {
        JToggleButton button = new JToggleButton();
        decorateButtonWithShortcut(button, title, shortcut);
        button.setSelected(selected);
        button.addActionListener(e -> canvas.setActiveTool(tool));
        toolbar.add(button);
        return button;
    }

    private void configurePlaybackButton(
            JButton button,
            String tooltip,
            String shortcut,
            java.awt.event.ActionListener listener
    ) {
        decorateButtonWithShortcut(button, button.getText(), shortcut);
        button.setToolTipText(tooltip + " (" + shortcut + ")");
        button.setMargin(new Insets(2, 10, 2, 10));
        button.addActionListener(listener);
    }

    private void decorateButtonWithShortcut(AbstractButton button, String label, String shortcut) {
        button.setText("<html><center>" + label + "<br><span style='font-size:9px;color:#666666'>" + shortcut + "</span></center></html>");
        button.setToolTipText(label + " (" + shortcut + ")");
    }

    private void openShortcutsDialog() {
        shortcutsDialog.setContent(GraphEditorShortcutSupport.buildShortcutsText());
        shortcutsDialog.setVisible(true);
        shortcutsDialog.toFront();
    }

    /** Opens the Selection->Extras popup for selected-subgraph actions. */
    private void openExtrasDialog() {
        if (!controller.hasSelectedSubgraph()) {
            JOptionPane.showMessageDialog(this, "Select at least one vertex first.");
            return;
        }
        extrasDialog.open();
    }


    /** Confirms destructive clear-all action before erasing the graph. */
    private void confirmAndClearGraph() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Delete all vertices and edges?",
                "Confirm Clear",
                JOptionPane.YES_NO_OPTION
        );
        if (result == JOptionPane.YES_OPTION) {
            controller.clearGraph();
        }
    }

    /** Prompts the user for a new weight for the currently selected edge. */
    private void promptForSelectedEdgeWeight() {
        Edge edge = controller.getSelectedEdge();
        if (edge == null) {
            JOptionPane.showMessageDialog(this, "Select an edge first.");
            return;
        }
        String value = JOptionPane.showInputDialog(this, "Edge weight:", edge.getWeight());
        if (value == null) {
            return;
        }
        try {
            controller.setSelectedEdgeWeight(Integer.parseInt(value.trim()));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number: " + value);
        }
    }

    /** Opens the premade-graphs picker and, if confirmed, replaces current graph contents. */
    private void openPremadeGraphsDialog() {
        PremadeGraphsDialog dialog = new PremadeGraphsDialog(this);
        dialog.setVisible(true);
        GraphTemplate template = dialog.getSelectedTemplate();
        if (template == null) {
            return;
        }

        if (controller.hasGraphContent()) {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Load premade graph '" + template.getName() + "'? This will replace your current graph.",
                    "Load Premade Graph",
                    JOptionPane.YES_NO_OPTION
            );
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }

        if (playbackController.hasActiveSession()) {
            playbackController.stop("Playback stopped because a premade graph was loaded.");
        }
        activeTopologicalOrder = List.of();
        activeTopologicalEdges = List.of();
        topologicalOrderDialog.setOrderData(activeTopologicalOrder, activeTopologicalEdges);
        controller.loadGraphTemplate(template);
        canvas.requestFocusInWindow();
    }

    /**
     * Starts the algorithm selection flow, validates inputs, runs the chosen algorithm,
     * then initializes all visualization dialogs from the produced timeline.
     */
    private void runAlgorithm(ActionEvent event) {
        if (controller.getGraph().vertexCount() == 0) {
            JOptionPane.showMessageDialog(this, "Create a graph before running an algorithm.");
            return;
        }

        List<Vertex> scopeVertices = controller.getAlgorithmScopeVertices();
        String scopeLabel = controller.getSelectedVertexIds().isEmpty()
                ? "Scope: whole graph (no vertices selected)"
                : "Scope: selected subgraph (" + scopeVertices.size() + " vertices)";

        AlgorithmRunDialog dialog = new AlgorithmRunDialog(this, GraphAlgorithmRegistry.getAlgorithms(), scopeVertices, scopeLabel);
        dialog.setVisible(true);
        AlgorithmRunConfiguration configuration = dialog.getConfiguration();
        if (configuration == null) {
            return;
        }

        GraphAlgorithm algorithm = configuration.getAlgorithm();
        if (algorithm == null) {
            return;
        }

        if (algorithm.getType() == AlgorithmType.MAXIMUM_FLOW) {
            graphBackupBeforeMaxFlow = controller.exportGraphState();
        }

        AlgorithmRequest request = controller.createAlgorithmRequest(
                configuration.getSourceVertexId(),
                configuration.getSinkVertexId(),
                algorithm.getOrientationRequirement() == GraphOrientationRequirement.UNDIRECTED_ONLY
        );

        if (!confirmOrientationCompatibility(algorithm, request)) {
            if (algorithm.getType() == AlgorithmType.MAXIMUM_FLOW) {
                graphBackupBeforeMaxFlow = null;
            }
            return;
        }

        String validationError = algorithm.validate(request);
        if (validationError != null) {
            if (algorithm.getType() == AlgorithmType.MAXIMUM_FLOW) {
                graphBackupBeforeMaxFlow = null;
            }
            JOptionPane.showMessageDialog(this, validationError, "Cannot Run Algorithm", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AlgorithmResult result = algorithm.run(request);
        playbackController.start(result);
        pseudocodeDialog.showForRun(result.getAlgorithmName(), result.getPseudocodeLines(), result.getExecutionNotice());
        pseudocodeDialog.setCallableSubroutineByLine(result.getCallableSubroutineByLine());
        pseudocodeDialog.setCanOpenSubroutineFromCurrentStep(playbackController.canOpenSubroutineAtCurrentStep());
        pseudocodeDialog.highlightMainLine(playbackController.getCurrentPseudocodeLine());

        if (algorithm instanceof TopologicalOrderAlgorithm topological) {
            activeTopologicalOrder = topological.getLastOrder();
            activeTopologicalEdges = topological.getLastEdges();
            topologicalOrderDialog.setOrderData(activeTopologicalOrder, activeTopologicalEdges);
            syncTopologicalOrderDialog();
        } else {
            activeTopologicalOrder = List.of();
            activeTopologicalEdges = List.of();
            topologicalOrderDialog.setOrderData(activeTopologicalOrder, activeTopologicalEdges);
        }

        workspace.syncAll(playbackController);
        workspace.selectTab(AlgorithmWorkspacePanel.TAB_PSEUDOCODE);
        canvas.requestFocusInWindow();
    }

    /**
     * Prompts when selected scope orientation does not match the algorithm contract.
     *
     * Directed-only algorithms can still run on paired edges if the user explicitly confirms.
     * Undirected-only algorithms can still run on asymmetric scopes if the user explicitly confirms.
     */
    private boolean confirmOrientationCompatibility(GraphAlgorithm algorithm, AlgorithmRequest request) {
        if (algorithm == null || request == null) {
            return true;
        }
        GraphOrientationRequirement requirement = algorithm.getOrientationRequirement();
        boolean symmetricScope = request.isSymmetricBidirectionalScope();
        boolean asymmetricScope = request.hasAsymmetricScopedEdges();

        if (requirement == GraphOrientationRequirement.DIRECTED_ONLY && symmetricScope) {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "'" + algorithm.getDisplayName() + "' is directed-only, but the selected scope looks undirected "
                            + "(each edge has a reverse pair).\n\n"
                            + "Run anyway using the underlying directed arcs?",
                    "Directed Algorithm Warning",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            return answer == JOptionPane.YES_OPTION;
        }

        if (requirement == GraphOrientationRequirement.UNDIRECTED_ONLY && asymmetricScope) {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "'" + algorithm.getDisplayName() + "' is undirected-only, but the selected scope contains "
                            + "asymmetric directed edges.\n\n"
                            + "Run anyway by interpreting the scope as currently drawn?",
                    "Undirected Algorithm Warning",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            return answer == JOptionPane.YES_OPTION;
        }

        return true;
    }

    /** Refreshes bottom status-bar summary (graph size, tool, selection, playback state). */
    private void refreshStatus() {
        String selected = controller.getSelectedEdge() != null
                ? "edge selected"
                : controller.getSelectedVertexIds().size() + " vertex selected";
        String playback = playbackController.hasActiveSession() ? " | " + playbackController.getProgressText() : "";
        int displayedEdges = controller.getDisplayedEdgeCount();
        int storedEdges = controller.getGraph().edgeCount();
        String edgeText = displayedEdges == storedEdges
                ? String.valueOf(displayedEdges)
                : (displayedEdges + " (stored " + storedEdges + ")");
        statusLabel.setText("Vertices: " + controller.getGraph().vertexCount()
                + " | Edges: " + edgeText
                + " | " + selected
                + " | Tool: " + canvas.getActiveTool()
                + playback);
    }

    /** Refreshes the right-panel playback info and navigation button enablement. */
    private void refreshPlaybackUi() {
        stepBackwardButton.setEnabled(playbackController.canStepBackward());
        stopPlaybackButton.setEnabled(playbackController.hasActiveSession());
        stepForwardButton.setEnabled(playbackController.canStepForward());
        algorithmInfoArea.setText(playbackController.getProgressText() + System.lineSeparator() + System.lineSeparator() + playbackController.getInfoMessage());
        algorithmInfoArea.setCaretPosition(0);
    }

    /**
     * Central playback sync entry point — delegates all view updates to the workspace panel.
     */
    private void syncAllViews() {
        workspace.syncAll(playbackController);
        // Also keep topological-order state updated
        if (playbackController.hasActiveSession()
                && "Topological Order".equals(playbackController.getCurrentAlgorithmName())
                && !activeTopologicalOrder.isEmpty()) {
            int shownCount = 0;
            List<AlgorithmStep> steps = playbackController.getCurrentSteps();
            int currentIndex = playbackController.getCurrentStepIndex();
            for (int i = 0; i <= currentIndex && i < steps.size(); i++) {
                if (steps.get(i).getTitle().startsWith("Dequeue ")) {
                    shownCount++;
                }
            }
            topologicalOrderDialog.updateShownCount(shownCount);
        }
    }

    /** Stops playback from a user-driven action and triggers full workspace cleanup via playback listeners. */
    private void stopPlaybackFromUser() {
        if (playbackController.hasActiveSession()) {
            playbackController.stop("Algorithm playback stopped.");
        }
    }

    /**
     * Applies the given workspace layout mode to the main frame.
     *
     * TABBED: workspace appears as a bottom panel below the graph canvas.
     * DOCKED_SPLIT: a JSplitPane splits the canvas and workspace horizontally.
     */
    private void applyWorkspaceMode(AlgorithmWorkspacePanel.Mode mode) {
        if (workspaceMinimized) {
            appliedWorkspaceMode = mode;
            return;
        }

        rememberCurrentSplitProportion();

        // Remove previous center content
        if (mainSplit != null) {
            remove(mainSplit);
            mainSplit = null;
        } else {
            remove(canvas);
        }
        // Also remove workspace if it was in SOUTH
        remove(workspace);

        if (mode == AlgorithmWorkspacePanel.Mode.DOCKED_SPLIT) {
            mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvas, workspace);
            mainSplit.setResizeWeight(dockedDividerProportion);
            mainSplit.setDividerSize(6);
            mainSplit.setContinuousLayout(true);
            add(mainSplit, BorderLayout.CENTER);
        } else {
            JSplitPane vertSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, canvas, workspace);
            vertSplit.setResizeWeight(tabbedDividerProportion);
            vertSplit.setDividerSize(6);
            vertSplit.setContinuousLayout(true);
            mainSplit = vertSplit;
            add(mainSplit, BorderLayout.CENTER);
        }
        installDividerTracking(mainSplit, mode);
        restoreSplitProportion(mainSplit, mode);
        appliedWorkspaceMode = mode;
        revalidate();
        repaint();
    }

    /** Captures current divider proportion before replacing split mode. */
    private void rememberCurrentSplitProportion() {
        if (mainSplit == null || appliedWorkspaceMode == null) {
            return;
        }
        rememberSplitProportion(mainSplit, appliedWorkspaceMode);
    }

    /** Tracks user divider drags so each mode remembers its own size. */
    private void installDividerTracking(JSplitPane splitPane, AlgorithmWorkspacePanel.Mode mode) {
        if (splitPane == null) {
            return;
        }
        splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt ->
                rememberSplitProportion(splitPane, mode));
    }

    private void rememberSplitProportion(JSplitPane splitPane, AlgorithmWorkspacePanel.Mode mode) {
        int span = mode == AlgorithmWorkspacePanel.Mode.DOCKED_SPLIT
                ? splitPane.getWidth()
                : splitPane.getHeight();
        if (span <= 1) {
            return;
        }
        double proportion = clampDividerProportion(splitPane.getDividerLocation() / (double) span);
        if (mode == AlgorithmWorkspacePanel.Mode.DOCKED_SPLIT) {
            dockedDividerProportion = proportion;
        } else {
            tabbedDividerProportion = proportion;
        }
    }

    private void restoreSplitProportion(JSplitPane splitPane, AlgorithmWorkspacePanel.Mode mode) {
        double proportion = mode == AlgorithmWorkspacePanel.Mode.DOCKED_SPLIT
                ? dockedDividerProportion
                : tabbedDividerProportion;
        double clamped = clampDividerProportion(proportion);
        splitPane.setDividerLocation(clamped);
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(clamped));
    }

    private static double clampDividerProportion(double value) {
        return Math.max(0.10, Math.min(0.90, value));
    }

    /** Minimizes workspace into a small restore tab in the status area. */
    private void minimizeWorkspace() {
        if (workspaceMinimized) {
            return;
        }
        rememberCurrentSplitProportion();
        if (mainSplit != null) {
            remove(mainSplit);
            mainSplit = null;
        } else {
            remove(workspace);
        }
        add(canvas, BorderLayout.CENTER);
        workspaceMinimized = true;
        restoreWorkspaceTabButton.setVisible(true);
        revalidate();
        repaint();
    }

    /** Restores workspace from the minimized tab, preserving previously remembered split size. */
    private void restoreWorkspaceFromTab() {
        if (!workspaceMinimized) {
            return;
        }
        remove(canvas);
        workspaceMinimized = false;
        restoreWorkspaceTabButton.setVisible(false);
        applyWorkspaceMode(workspace.getMode());
    }

    /** Opens the single reusable sub-routine overlay for the hovered pseudocode call-site line. */
    private void openSubroutineFromPseudocodeDialog(int pseudocodeLineIndex) {
        boolean opened = pseudocodeLineIndex >= 0 && playbackController.openSubroutineForLine(pseudocodeLineIndex);
        if (!opened) {
            opened = playbackController.openSubroutineAtCurrentStep();
        }
        if (!opened) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /** Closes the active sub-routine overlay and restores base timeline playback. */
    private void closeSubroutineFromPseudocodeDialog() {
        playbackController.closeActiveSubroutineOverlay();
    }

    /** Keeps the topological-order dialog synchronized with the main playback position. */
    private void syncTopologicalOrderDialog() {
        if (!playbackController.hasActiveSession()
                || !"Topological Order".equals(playbackController.getCurrentAlgorithmName())
                || activeTopologicalOrder.isEmpty()) {
            topologicalOrderDialog.updateShownCount(0);
            return;
        }

        int shownCount = 0;
        List<AlgorithmStep> steps = playbackController.getCurrentSteps();
        int currentIndex = playbackController.getCurrentStepIndex();
        for (int i = 0; i <= currentIndex && i < steps.size(); i++) {
            if (steps.get(i).getTitle().startsWith("Dequeue ")) {
                shownCount++;
            }
        }
        topologicalOrderDialog.updateShownCount(shownCount);
    }


    /** Restores original graph snapshot after stopping maximum-flow playback. */
    private void maybeRestoreGraphAfterPlaybackStop() {
        if (playbackController.hasActiveSession()) {
            return;
        }
        if (graphBackupBeforeMaxFlow == null) {
            return;
        }
        controller.restoreGraphState(graphBackupBeforeMaxFlow);
        graphBackupBeforeMaxFlow = null;
    }

    /** Clears algorithm-specific transient view state after playback termination. */
    private void cleanupAfterPlaybackTermination() {
        activeTopologicalOrder = List.of();
        activeTopologicalEdges = List.of();
        topologicalOrderDialog.setOrderData(activeTopologicalOrder, activeTopologicalEdges);
        topologicalOrderDialog.updateShownCount(0);
    }

    /** Applies a quick circular layout to all current vertices. */
    private void applyCircleLayout() {
        int count = controller.getGraph().vertexCount();
        if (count == 0) {
            return;
        }

        double radius = Math.max(120, 30.0 * count);
        double centerX = 300;
        double centerY = 220;
        Map<Integer, Point2D.Double> updatedPositions = new LinkedHashMap<>();

        int index = 0;
        for (Vertex vertex : controller.getGraph().getVertices()) {
            double angle = (2 * Math.PI * index) / count;
            updatedPositions.put(vertex.getId(), new Point2D.Double(
                    centerX + radius * Math.cos(angle),
                    centerY + radius * Math.sin(angle)
            ));
            index++;
        }
        controller.applyVertexPositions(updatedPositions);
    }

    /** Registers global keyboard shortcuts on the frame root pane. */
    private void installKeyboardShortcuts() {
        GraphEditorShortcutSupport.Bindings bindings = new GraphEditorShortcutSupport.Bindings();
        bindings.rootPane = getRootPane();
        bindings.runAlgorithmButton = runAlgorithmButton;
        bindings.shortcutsButton = shortcutsButton;
        bindings.premadeGraphsButton = premadeGraphsButton;
        bindings.extrasButton = extrasButton;
        bindings.clearGraphButton = clearGraphButton;
        bindings.selectToolButton = selectToolButton;
        bindings.addVertexToolButton = addVertexToolButton;
        bindings.connectToolButton = connectToolButton;
        bindings.deleteToolButton = deleteToolButton;
        bindings.panToolButton = panToolButton;

        bindings.onPlaybackStepBackward = playbackController::stepBackward;
        bindings.onPlaybackStepForward = playbackController::stepForward;
        bindings.onPlaybackStop = this::stopPlaybackFromUser;
        bindings.onResetView = canvas::resetView;
        bindings.onPromptSelectedEdgeWeight = this::promptForSelectedEdgeWeight;
        bindings.onAdjustDefaultEdgeWeightUp = () -> adjustDefaultEdgeWeight(1);
        bindings.onAdjustDefaultEdgeWeightDown = () -> adjustDefaultEdgeWeight(-1);
        bindings.onApplyCircleLayout = this::applyCircleLayout;
        bindings.onChooseDefaultEdgeColor = this::promptChooseDefaultEdgeColor;
        bindings.onColorVertices = this::promptColorSelectedVertices;
        bindings.onColorEdge = this::promptColorSelectedEdge;
        bindings.onUndo = controller::undo;
        bindings.onCopySelection = () -> {
            if (!controller.copySelectionToClipboard()) {
                Toolkit.getDefaultToolkit().beep();
            }
        };
        bindings.onPasteSelection = () -> {
            if (!controller.pasteClipboard()) {
                Toolkit.getDefaultToolkit().beep();
            }
        };

        GraphEditorShortcutSupport.install(bindings);
    }

    /** Human-readable scope summary shown at the top of the Extras popup. */
    private String buildSelectedSubgraphSummary() {
        int vertexCount = controller.getSelectedScopeVertices().size();
        int edgeCount = controller.getSelectedScopeEdgeCountForDisplay();
        return "Applies to selected subgraph: " + vertexCount + " vertex/vertices, " + edgeCount + " edge(s).";
    }

    /** Enables/disables Extras action depending on whether a subgraph is currently selected. */
    private void refreshExtrasUi() {
        boolean hasScope = controller.hasSelectedSubgraph();
        extrasButton.setEnabled(hasScope);
        if (hasScope) {
            extrasButton.setToolTipText("Extras (Ctrl+Shift+O)");
        } else {
            extrasButton.setToolTipText("Extras (Ctrl+Shift+O) - select vertices first");
        }
    }

    /** Handles switching between directed creation and undirected paired creation modes. */
    private void toggleUndirectedCreationMode() {
        boolean turningOn = undirectedModeToggle.isSelected();
        if (turningOn) {
            JCheckBox remember = new JCheckBox("Remember this choice for this session");
            Object[] message = {
                    "How should undirected creation mode be applied?",
                    "- Creation behavior only: existing graph stays unchanged",
                    "- Normalize existing graph: add missing reverse edges and sync pair metadata",
                    remember
            };
            String[] options = {"Creation Only", "Normalize Existing", "Cancel"};
            int choice = JOptionPane.showOptionDialog(
                    this,
                    message,
                    "Enable Undirected Creation",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );
            if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
                undirectedModeToggle.setSelected(false);
                return;
            }
            controller.setEdgeCreationMode(
                    GraphEditorController.EdgeCreationMode.UNDIRECTED_PAIRED,
                    choice == 1
            );
        } else {
            controller.setEdgeCreationMode(GraphEditorController.EdgeCreationMode.DIRECTED, false);
        }
        updateUndirectedToggleText();
        canvas.requestFocusInWindow();
    }

    private void updateUndirectedToggleText() {
        boolean on = controller.getEdgeCreationMode() == GraphEditorController.EdgeCreationMode.UNDIRECTED_PAIRED;
        undirectedModeToggle.setSelected(on);
        undirectedModeToggle.setText(on ? "Undirected Creation: ON" : "Undirected Creation: OFF");
    }

    /** Adjusts the default edge weight spinner while respecting configured min/max bounds. */
    private void adjustDefaultEdgeWeight(int delta) {
        SpinnerNumberModel model = (SpinnerNumberModel) edgeWeightSpinner.getModel();
        int current = ((Number) model.getNumber()).intValue();
        int min = ((Number) model.getMinimum()).intValue();
        int max = ((Number) model.getMaximum()).intValue();
        int next = Math.max(min, Math.min(max, current + delta));
        if (next != current) {
            edgeWeightSpinner.setValue(next);
        }
    }

    /** Prompts and applies a new default edge color. */
    private void promptChooseDefaultEdgeColor() {
        Color color = chooseColor("Choose Default Edge Color", pendingEdgeColor);
        if (color != null) {
            pendingEdgeColor = color;
            canvas.setPendingEdgeColor(color.getRGB());
        }
    }

    /** Prompts and applies a color to all selected vertices. */
    private void promptColorSelectedVertices() {
        Color color = chooseColor("Choose Vertex Color", new Color(66, 133, 244));
        if (color != null) {
            controller.setSelectedVerticesColor(color.getRGB());
        }
    }

    /** Prompts and applies a color to the currently selected edge. */
    private void promptColorSelectedEdge() {
        Edge selectedEdge = controller.getSelectedEdge();
        if (selectedEdge == null) {
            JOptionPane.showMessageDialog(this, "Select an edge first.");
            return;
        }
        Color initial = new Color(selectedEdge.getColor() == 0 ? Color.DARK_GRAY.getRGB() : selectedEdge.getColor(), false);
        Color color = chooseColor("Choose Edge Color", initial);
        if (color != null) {
            controller.setSelectedEdgeColor(color.getRGB());
        }
    }

    /** Opens a reusable chooser so recent colors remain available across all color actions. */
    private Color chooseColor(String title, Color initial) {
        final Color[] selected = {null};
        colorChooser.setColor(initial == null ? Color.WHITE : initial);
        JDialog dialog = JColorChooser.createDialog(
                this,
                title,
                true,
                colorChooser,
                e -> selected[0] = colorChooser.getColor(),
                null
        );
        dialog.setVisible(true);
        return selected[0];
    }
}
