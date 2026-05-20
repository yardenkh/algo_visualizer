package ui;

import algorithm.AlgorithmPlaybackController;
import algorithm.SubroutineTimeline;
import ui.dialogs.AlgorithmMatrixDialog;
import ui.dialogs.AlgorithmPseudocodeDialog;
import ui.dialogs.AlgorithmStackFramesDialog;
import ui.dialogs.LevelGraphDialog;
import ui.dialogs.ResidualGraphDialog;
import ui.dialogs.TopologicalOrderDialog;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Unified algorithm workspace that hosts all secondary visualization panels.
 *
 * Layout modes:
 *   TABBED       – all views live in a single JTabbedPane (default)
 *   DOCKED_SPLIT – the graph canvas and algorithm workspace share a JSplitPane
 *
 * Each view panel (pseudocode, matrix, stack frames, residual graph, level graph,
 * topological order) is a self-contained embeddable panel that the workspace can
 * show in a tab or pop out to a separate floating window.
 *
 * Entry point: {@link GraphEditorFrame} creates one instance and delegates all
 * algorithm-view sync calls to it via {@link #syncAll(AlgorithmPlaybackController)}.
 *
 * See also: {@link AlgorithmPseudocodeDialog}, {@link AlgorithmMatrixDialog},
 *           {@link AlgorithmStackFramesDialog}, {@link ResidualGraphDialog},
 *           {@link LevelGraphDialog}, {@link TopologicalOrderDialog}.
 */
public final class AlgorithmWorkspacePanel extends JPanel {

    /** The two layout modes for the workspace. */
    public enum Mode { TABBED, DOCKED_SPLIT }

    // -----------------------------------------------------------------------
    // Tab identifiers
    // -----------------------------------------------------------------------
    public static final String TAB_PSEUDOCODE  = "Pseudocode";
    public static final String TAB_MATRIX      = "Matrix";
    public static final String TAB_STACK       = "Stack Frames";
    public static final String TAB_RESIDUAL    = "Residual Graph";
    public static final String TAB_LEVEL       = "Level Graph";
    public static final String TAB_TOPO        = "Topological Order";

    // -----------------------------------------------------------------------
    // Child dialog/panel references (kept for pop-out support)
    // -----------------------------------------------------------------------
    private final AlgorithmPseudocodeDialog pseudocodeDialog;
    private final AlgorithmMatrixDialog     matrixDialog;
    private final AlgorithmStackFramesDialog stackFramesDialog;
    private final ResidualGraphDialog       residualDialog;
    private final LevelGraphDialog          levelGraphDialog;
    private final TopologicalOrderDialog    topoDialog;

    // -----------------------------------------------------------------------
    // Layout state
    // -----------------------------------------------------------------------
    private Mode mode = Mode.TABBED;
    private final JTabbedPane tabs;
    private final JPanel      headerBar;
    private final JToggleButton modeToggle;
    private final JLabel        statusLabel;
    private final List<Consumer<Mode>> modeChangeListeners = new ArrayList<>();
    private final List<Runnable> minimizeListeners = new ArrayList<>();

    /** Maps logical tab key -> display title currently shown to the user. */
    private final Map<String, String> tabTitleByKey = new LinkedHashMap<>();
    /** Tracks detached (popped-out) windows by their logical tab key for deterministic cleanup. */
    private final Map<String, JFrame> floatingFramesByKey = new LinkedHashMap<>();

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Builds the workspace panel.
     *
     * @param owner        owner frame used as parent for floating (pop-out) dialogs
     * @param pseudocode   the pseudocode dialog whose embeddable panel is used here
     * @param matrix       the matrix dialog whose embeddable panel is used here
     * @param stack        the stack frames dialog whose embeddable panel is used here
     * @param residual     the residual graph dialog whose embeddable panel is used here
     * @param level        the level graph dialog whose embeddable panel is used here
     * @param topo         the topological order dialog whose embeddable panel is used here
     */
    public AlgorithmWorkspacePanel(
            Frame owner,
            AlgorithmPseudocodeDialog pseudocode,
            AlgorithmMatrixDialog matrix,
            AlgorithmStackFramesDialog stack,
            ResidualGraphDialog residual,
            LevelGraphDialog level,
            TopologicalOrderDialog topo
    ) {
        super(new BorderLayout());

        this.pseudocodeDialog  = pseudocode;
        this.matrixDialog      = matrix;
        this.stackFramesDialog = stack;
        this.residualDialog    = residual;
        this.levelGraphDialog  = level;
        this.topoDialog        = topo;

        // Header bar: mode toggle + status label
        headerBar   = buildHeaderBar();
        statusLabel = (JLabel) findComponent(headerBar, JLabel.class);
        modeToggle  = (JToggleButton) findComponent(headerBar, JToggleButton.class);

        // Tabbed pane
        tabs = new JTabbedPane(JTabbedPane.BOTTOM);
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setBorder(BorderFactory.createEmptyBorder());

        add(headerBar, BorderLayout.NORTH);
        add(tabs,      BorderLayout.CENTER);

        setPreferredSize(new Dimension(760, 480));
        setMinimumSize(new Dimension(320, 200));
    }

    // -----------------------------------------------------------------------
    // Public sync API (called by GraphEditorFrame on every playback change)
    // -----------------------------------------------------------------------

    /**
     * Central sync entry point.  Called whenever the playback controller changes state.
     *
     * Shows/hides/refreshes each tab according to what data is available in the current step.
     */
    public void syncAll(AlgorithmPlaybackController playback) {
        if (!playback.hasActiveSession()) {
            closeAllAlgorithmViews();
            updateStatusLabel(playback);
            return;
        }
        syncPseudocodeTab(playback);
        syncMatrixTab(playback);
        syncStackTab(playback);
        syncResidualTab(playback);
        syncLevelGraphTab(playback);
        syncTopoTab(playback);
        updateStatusLabel(playback);
    }

    // -----------------------------------------------------------------------
    // Mode control
    // -----------------------------------------------------------------------

    /** Returns the current layout mode. */
    public Mode getMode() {
        return mode;
    }

    /** Registers a callback invoked when the workspace layout mode changes. */
    public void addModeChangeListener(Consumer<Mode> listener) {
        if (listener != null) {
            modeChangeListeners.add(listener);
        }
    }

    /** Registers a callback invoked when the workspace minimize button is pressed. */
    public void addMinimizeListener(Runnable listener) {
        if (listener != null) {
            minimizeListeners.add(listener);
        }
    }

    /**
     * Switches between TABBED and DOCKED_SPLIT layout modes.
     *
     * In DOCKED_SPLIT mode, a horizontal JSplitPane is installed in the
     * parent frame so the workspace shares the main window with the canvas.
     * In TABBED mode the workspace is a standalone dockable side panel.
     */
    public void setMode(Mode newMode) {
        if (this.mode == newMode) {
            return;
        }
        this.mode = newMode;
        modeToggle.setSelected(newMode == Mode.DOCKED_SPLIT);
        modeToggle.setText(newMode == Mode.DOCKED_SPLIT ? "⊟ Undock" : "⊞ Dock Split");
        for (Consumer<Mode> listener : modeChangeListeners) {
            listener.accept(newMode);
        }
        revalidate();
        repaint();
    }

    // -----------------------------------------------------------------------
    // Tab visibility helpers
    // -----------------------------------------------------------------------

    /**
     * Shows a tab with the given title and content panel, inserting it at the
     * correct alphabetical position if it is not already present.
     */
    public void showTab(String title, JPanel content) {
        showTab(title, title, content);
    }

    /**
     * Shows a tab by logical key.
     *
     * If the key already exists, content is refreshed in-place instead of creating a duplicate tab.
     */
    public void showTab(String key, String title, JPanel content) {
        String effectiveKey = key == null || key.isBlank() ? title : key;
        String effectiveTitle = title == null || title.isBlank() ? effectiveKey : title;

        JPanel wrapped = wrapInScroll(content, effectiveTitle);
        int idx = findTabIndexByKey(effectiveKey);
        if (idx >= 0) {
            tabs.setTitleAt(idx, effectiveTitle);
            tabs.setComponentAt(idx, wrapped);
            tabs.setTabComponentAt(idx, buildTabHeader(effectiveKey, effectiveTitle));
            tabTitleByKey.put(effectiveKey, effectiveTitle);
            return;
        }

        tabs.addTab(effectiveTitle, wrapped);
        int newIndex = tabs.indexOfTab(effectiveTitle);
        tabs.setTabComponentAt(newIndex, buildTabHeader(effectiveKey, effectiveTitle));
        tabs.setSelectedIndex(newIndex);
        tabTitleByKey.put(effectiveKey, effectiveTitle);
    }

    /** Hides (removes) the tab with the given title if currently present. */
    public void hideTab(String title) {
        hideTabByKey(title);
    }

    /** Hides (removes) the tab with the given logical key if currently present. */
    public void hideTabByKey(String key) {
        int idx = findTabIndexByKey(key);
        if (idx >= 0) {
            tabs.removeTabAt(idx);
        }
        tabTitleByKey.remove(key);
    }

    /** Switches focus to the named tab if visible. */
    public void selectTab(String title) {
        int idx = findTabIndexByKey(title);
        if (idx < 0) {
            idx = tabs.indexOfTab(title);
        }
        if (idx >= 0) {
            tabs.setSelectedIndex(idx);
        }
    }

    // -----------------------------------------------------------------------
    // Per-panel sync helpers
    // -----------------------------------------------------------------------

    private void syncPseudocodeTab(AlgorithmPlaybackController playback) {
        if (!playback.hasActiveSession()) {
            return;
        }
        showTab(TAB_PSEUDOCODE, pseudocodeDialog.createEmbeddablePanel());

        if (playback.hasActiveSubroutineOverlay()) {
            SubroutineTimeline sub = playback.getActiveSubroutineTimeline();
            if (sub != null) {
                pseudocodeDialog.showSubroutine(sub.getDisplayName(), sub.getPseudocodeLines());
                if (playback.isCurrentStepInsideActiveSubroutineOverlay()) {
                    pseudocodeDialog.showSubroutineTab();
                    pseudocodeDialog.highlightSubroutineLine(playback.getCurrentSubroutinePseudocodeLine());
                    pseudocodeDialog.highlightMainLine(-1);
                } else {
                    pseudocodeDialog.showMainTab();
                    pseudocodeDialog.highlightMainLine(playback.getCurrentPseudocodeLine());
                    pseudocodeDialog.highlightSubroutineLine(-1);
                }
            }
        } else {
            pseudocodeDialog.removeSubroutineTab();
            pseudocodeDialog.showMainTab();
            pseudocodeDialog.highlightMainLine(playback.getCurrentPseudocodeLine());
            pseudocodeDialog.highlightSubroutineLine(-1);
        }
        pseudocodeDialog.setCanOpenSubroutineFromCurrentStep(playback.canOpenSubroutineAtCurrentStep());
        pseudocodeDialog.setExecutionWarning(playback.getExecutionNotice());
    }

    private void syncMatrixTab(AlgorithmPlaybackController playback) {
        if (!playback.hasActiveSession() || playback.getCurrentStep() == null
                || playback.getCurrentStep().getMatrixSnapshot() == null) {
            hideTab(TAB_MATRIX);
            return;
        }
        matrixDialog.showMatrix(playback.getCurrentAlgorithmName(), playback.getCurrentStep().getMatrixSnapshot());
        showTab(TAB_MATRIX, matrixDialog.createEmbeddablePanel());
    }

    private void syncStackTab(AlgorithmPlaybackController playback) {
        if (!playback.hasActiveSession() || playback.getCurrentStep() == null
                || playback.getCurrentStep().getStackFramesSnapshot() == null) {
            hideTab(TAB_STACK);
            return;
        }
        stackFramesDialog.showStack(playback.getCurrentAlgorithmName(), playback.getCurrentStep().getStackFramesSnapshot());
        showTab(TAB_STACK, stackFramesDialog.createEmbeddablePanel());
    }

    private void syncResidualTab(AlgorithmPlaybackController playback) {
        if (!playback.hasActiveSession() || playback.getCurrentStep() == null
                || playback.getCurrentStep().getResidualGraphSnapshot() == null) {
            hideTab(TAB_RESIDUAL);
            return;
        }
        residualDialog.showSnapshot(playback.getCurrentAlgorithmName(), playback.getCurrentStep().getResidualGraphSnapshot());
        showTab(TAB_RESIDUAL, residualDialog.createEmbeddablePanel());
    }

    private void syncLevelGraphTab(AlgorithmPlaybackController playback) {
        if (!playback.hasActiveSession() || playback.getCurrentStep() == null
                || playback.getCurrentStep().getLevelGraphSnapshot() == null) {
            hideTab(TAB_LEVEL);
            return;
        }
        levelGraphDialog.showSnapshot(playback.getCurrentAlgorithmName(), playback.getCurrentStep().getLevelGraphSnapshot());
        showTab(TAB_LEVEL, levelGraphDialog.createEmbeddablePanel());
    }

    private void syncTopoTab(AlgorithmPlaybackController playback) {
        if (!playback.hasActiveSession()
                || !"Topological Order".equals(playback.getCurrentAlgorithmName())) {
            hideTab(TAB_TOPO);
            return;
        }
        showTab(TAB_TOPO, topoDialog.createEmbeddablePanel());
    }

    // -----------------------------------------------------------------------
    // Header bar
    // -----------------------------------------------------------------------

    private JPanel buildHeaderBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(38, 45, 60));
        bar.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        JLabel title = new JLabel("Algorithm Workspace");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));

        JToggleButton toggle = new JToggleButton("⊞ Dock Split");
        toggle.setFocusPainted(false);
        toggle.setMargin(new Insets(2, 8, 2, 8));
        toggle.setFont(toggle.getFont().deriveFont(11f));
        toggle.setToolTipText("Toggle between tabbed and docked-split layout");
        toggle.addActionListener(e -> setMode(toggle.isSelected() ? Mode.DOCKED_SPLIT : Mode.TABBED));

        JButton minimize = new JButton("-");
        minimize.setFocusPainted(false);
        minimize.setMargin(new Insets(1, 8, 1, 8));
        minimize.setFont(minimize.getFont().deriveFont(Font.BOLD, 12f));
        minimize.setToolTipText("Minimize algorithm workspace");
        minimize.addActionListener(e -> {
            for (Runnable listener : minimizeListeners) {
                listener.run();
            }
        });

        JLabel status = new JLabel("No algorithm running");
        status.setForeground(new Color(180, 200, 220));
        status.setFont(status.getFont().deriveFont(11f));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        right.add(status);
        right.add(toggle);
        right.add(minimize);

        bar.add(title, BorderLayout.WEST);
        bar.add(right,  BorderLayout.EAST);
        return bar;
    }

    private void updateStatusLabel(AlgorithmPlaybackController playback) {
        if (statusLabel == null) {
            return;
        }
        statusLabel.setText(playback.hasActiveSession() ? playback.getProgressText() : "No algorithm running");
    }

    // -----------------------------------------------------------------------
    // Tab header component (title + pop-out button)
    // -----------------------------------------------------------------------

    private JPanel buildTabHeader(String key, String title) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        header.setOpaque(false);
        JLabel lbl = new JLabel(title);
        lbl.setFont(lbl.getFont().deriveFont(12f));

        JButton close = new JButton("x");
        close.setFocusPainted(false);
        close.setBorderPainted(false);
        close.setContentAreaFilled(false);
        close.setOpaque(false);
        close.setVisible(false);
        close.setMargin(new Insets(0, 2, 0, 2));
        close.setFont(close.getFont().deriveFont(Font.BOLD, 10f));
        close.setToolTipText("Close tab");
        close.addActionListener(e -> hideTabByKey(key));

        JButton popOut = new JButton("↗");
        popOut.setFocusPainted(false);
        popOut.setMargin(new Insets(0, 3, 0, 3));
        popOut.setFont(popOut.getFont().deriveFont(10f));
        popOut.setToolTipText("Pop out \"" + title + "\" into its own window");
        popOut.addActionListener(e -> popOutTab(key));

        MouseAdapter selectTab = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectTabByKey(key);
            }
        };

        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                close.setVisible(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    // Keep close visible while moving between children inside the same tab header.
                    close.setVisible(isPointerInside(header));
                });
            }
        };
        header.addMouseListener(hover);
        lbl.addMouseListener(hover);
        popOut.addMouseListener(hover);
        close.addMouseListener(hover);

        header.addMouseListener(selectTab);
        lbl.addMouseListener(selectTab);

        header.add(lbl);
        header.add(popOut);
        header.add(close);
        return header;
    }

    /**
     * Pops a tab out into its own floating window.
     *
     * The corresponding dialog is shown as a floating JFrame child. Closing it
     * does not stop playback — it just hides the floating window so the user
     * can re-embed it later by navigating to a step that shows the data again.
     */
    private void popOutTab(String key) {
        int idx = findTabIndexByKey(key);
        if (idx < 0) {
            return;
        }
        String title = tabs.getTitleAt(idx);
        JPanel content = (JPanel) tabs.getComponentAt(idx);
        tabs.removeTabAt(idx);
        tabTitleByKey.remove(key);

        JFrame floater = new JFrame(title);
        floater.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        floater.setLayout(new BorderLayout());
        floater.add(content, BorderLayout.CENTER);
        floater.setSize(820, 520);
        floater.setLocationByPlatform(true);
        floater.setVisible(true);
        floatingFramesByKey.put(key, floater);
    }

    /**
     * Removes all algorithm-created workspace tabs and disposes detached tab windows.
     *
     * Used when playback is terminated so the workspace returns to its default idle state.
     */
    public void closeAllAlgorithmViews() {
        tabs.removeAll();
        tabTitleByKey.clear();
        for (JFrame frame : floatingFramesByKey.values()) {
            if (frame != null) {
                frame.dispose();
            }
        }
        floatingFramesByKey.clear();

        pseudocodeDialog.resetForNoActiveRun();
        revalidate();
        repaint();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Wraps a panel in a scroll pane, but only if not already scrollable.
     */
    private static JPanel wrapInScroll(JPanel content, String title) {
        // Return as-is; individual panels manage their own scroll where needed
        return content;
    }

    /** Locates a tab index by logical key. */
    private int findTabIndexByKey(String key) {
        String title = tabTitleByKey.get(key);
        if (title != null) {
            return tabs.indexOfTab(title);
        }
        return -1;
    }

    /** Selects a tab by its logical key when the tab header is clicked. */
    private void selectTabByKey(String key) {
        int idx = findTabIndexByKey(key);
        if (idx >= 0 && idx != tabs.getSelectedIndex()) {
            tabs.setSelectedIndex(idx);
        }
    }

    /** Returns true while the mouse pointer is inside the given component bounds. */
    private static boolean isPointerInside(JComponent component) {
        return component != null && component.getMousePosition(true) != null;
    }


    /**
     * Finds the first component of a given type in a container (non-recursive, header bar only).
     */
    private static Component findComponent(JPanel panel, Class<?> type) {
        for (Component c : panel.getComponents()) {
            if (type.isInstance(c)) {
                return c;
            }
            if (c instanceof JPanel sub) {
                for (Component inner : sub.getComponents()) {
                    if (type.isInstance(inner)) {
                        return inner;
                    }
                }
            }
        }
        return null;
    }
}







