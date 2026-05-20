package ui.dialogs;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Modeless popup that shows algorithm pseudocode and highlights the active line.
 *
 * This window is synchronized by the main playback flow in {@link ui.GraphEditorFrame}.
 */
public class AlgorithmPseudocodeDialog extends JDialog {
    private final JTabbedPane tabs;
    private final JList<String> mainLineList;
    private final JList<String> subroutineLineList;
    private final JLabel executionWarningLabel;
    private final java.util.List<JButton> openSubroutineButtons;
    private final JButton closeSubroutineButton;

    private Runnable onCloseRequest;
    private IntConsumer onOpenSubroutineRequest;
    private Runnable onCloseSubroutineRequest;

    private Map<Integer, String> callableSubroutineByLine;
    private int hoveredMainIndex;
    private int activeMainLineIndex;
    private boolean canOpenSubroutineFromCurrentStep;
    private int subroutineTabIndex;
    private JLabel subroutineTabTitleLabel;

    /** Builds the pseudocode viewer window and close callback hook. */
    public AlgorithmPseudocodeDialog(Frame owner) {
        super(owner, "Algorithm Pseudocode", false);
        setLayout(new BorderLayout());

        callableSubroutineByLine = Map.of();
        hoveredMainIndex = -1;
        activeMainLineIndex = -1;
        canOpenSubroutineFromCurrentStep = false;
        subroutineTabIndex = -1;
        subroutineTabTitleLabel = null;
        openSubroutineButtons = new java.util.ArrayList<>();

        tabs = new JTabbedPane();

        mainLineList = createCodeList(true);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(wrapList(mainLineList), BorderLayout.CENTER);
        tabs.addTab("Main Algorithm", mainPanel);

        subroutineLineList = createCodeList(false);
        add(tabs, BorderLayout.CENTER);

        executionWarningLabel = new JLabel();
        executionWarningLabel.setForeground(new Color(170, 18, 18));
        executionWarningLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        JButton openSubroutineButton = createOpenSubroutineButton();
        closeSubroutineButton = new JButton("Close Sub-routine Tab");
        closeSubroutineButton.setMargin(new Insets(2, 8, 2, 8));
        closeSubroutineButton.setVisible(false);
        closeSubroutineButton.addActionListener(e -> {
            if (onCloseSubroutineRequest != null) {
                onCloseSubroutineRequest.run();
            }
        });

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(2, 6, 4, 6));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.add(openSubroutineButton);
        left.add(javax.swing.Box.createHorizontalStrut(8));
        left.add(closeSubroutineButton);
        left.setOpaque(false);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.add(executionWarningLabel);
        right.setOpaque(false);
        footer.add(left, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (onCloseRequest != null) {
                    onCloseRequest.run();
                }
            }
        });

        setSize(760, 460);
        setLocationByPlatform(true);
    }

    /** Registers a callback used when the dialog is closed by the user. */
    public void setOnCloseRequest(Runnable onCloseRequest) {
        this.onCloseRequest = onCloseRequest;
    }

    /** Registers callback fired when user requests opening a callable sub-routine line. */
    public void setOnOpenSubroutineRequest(IntConsumer onOpenSubroutineRequest) {
        this.onOpenSubroutineRequest = onOpenSubroutineRequest;
    }

    /** Registers callback fired when user requests closing the active sub-routine tab. */
    public void setOnCloseSubroutineRequest(Runnable onCloseSubroutineRequest) {
        this.onCloseSubroutineRequest = onCloseSubroutineRequest;
    }

    /** Loads pseudocode lines for a new run and makes the dialog visible. */
    public void showForRun(String algorithmName, List<String> pseudocodeLines, String executionWarning) {
        setTitle("Pseudocode - " + algorithmName + " (CLRS style)");
        mainLineList.setListData(withLineNumbers(pseudocodeLines));
        mainLineList.clearSelection();
        hoveredMainIndex = -1;
        activeMainLineIndex = -1;
        removeSubroutineTab();
        tabs.setSelectedIndex(0);
        setExecutionWarning(executionWarning);
    }

    /**
     * Returns a lightweight embeddable panel containing the same tab pane and footer.
     * Safe to embed in a workspace tab without reparenting the dialog's own content pane.
     */
    public JPanel createEmbeddablePanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(tabs, BorderLayout.CENTER);
        // Re-create a minimal footer with open-subroutine affordance and the warning label.
        JPanel foot = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 2));
        foot.setOpaque(false);
        foot.add(createOpenSubroutineButton());
        foot.add(javax.swing.Box.createHorizontalStrut(8));
        foot.add(executionWarningLabel);
        wrapper.add(foot, BorderLayout.SOUTH);
        return wrapper;
    }

    /**
     * Sets callable line metadata for the main pseudocode tab.
     * Keys are zero-based line indexes in the main pseudocode list.
     */
    public void setCallableSubroutineByLine(Map<Integer, String> callableSubroutineByLine) {
        if (callableSubroutineByLine == null) {
            this.callableSubroutineByLine = Map.of();
        } else {
            this.callableSubroutineByLine = Collections.unmodifiableMap(new LinkedHashMap<>(callableSubroutineByLine));
        }
        refreshOpenSubroutineButtons();
        mainLineList.repaint();
    }

    /** Enables/disables opening sub-routines from main pseudocode hover actions. */
    public void setCanOpenSubroutineFromCurrentStep(boolean enabled) {
        this.canOpenSubroutineFromCurrentStep = enabled;
        refreshOpenSubroutineButtons();
        mainLineList.repaint();
    }

    /** Displays (or refreshes) the single reusable sub-routine tab. */
    public void showSubroutine(String displayName, List<String> pseudocodeLines) {
        if (subroutineTabIndex < 0) {
            JPanel subPanel = new JPanel(new BorderLayout());
            subPanel.add(wrapList(subroutineLineList), BorderLayout.CENTER);
            tabs.addTab("Sub-routine", subPanel);
            subroutineTabIndex = tabs.getTabCount() - 1;
            tabs.setTabComponentAt(subroutineTabIndex, buildSubroutineTabHeader());
        }
        String effectiveTitle = displayName == null || displayName.isBlank() ? "Sub-routine" : displayName;
        tabs.setTitleAt(subroutineTabIndex, effectiveTitle);
        if (subroutineTabTitleLabel != null) {
            subroutineTabTitleLabel.setText(effectiveTitle);
        }
        subroutineLineList.setListData(withLineNumbers(pseudocodeLines));
        subroutineLineList.clearSelection();
        closeSubroutineButton.setVisible(true);
        refreshOpenSubroutineButtons();
    }

    /** Removes the active sub-routine tab and returns focus to main pseudocode. */
    public void removeSubroutineTab() {
        if (subroutineTabIndex >= 0 && subroutineTabIndex < tabs.getTabCount()) {
            tabs.removeTabAt(subroutineTabIndex);
        }
        subroutineTabIndex = -1;
        subroutineTabTitleLabel = null;
        closeSubroutineButton.setVisible(false);
        refreshOpenSubroutineButtons();
    }

    /** Builds a closable sub-routine tab header with a hover-only close affordance. */
    private JPanel buildSubroutineTabHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        header.setOpaque(false);

        JLabel title = new JLabel("Sub-routine");
        title.setFont(title.getFont().deriveFont(12f));
        subroutineTabTitleLabel = title;

        JButton close = new JButton("x");
        close.setFocusPainted(false);
        close.setBorderPainted(false);
        close.setContentAreaFilled(false);
        close.setOpaque(false);
        close.setVisible(false);
        close.setMargin(new Insets(0, 2, 0, 2));
        close.setFont(close.getFont().deriveFont(Font.BOLD, 10f));
        close.setToolTipText("Close sub-routine tab");
        close.addActionListener(e -> {
            if (onCloseSubroutineRequest != null) {
                onCloseSubroutineRequest.run();
            }
        });

        MouseAdapter selectTab = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (subroutineTabIndex >= 0 && subroutineTabIndex < tabs.getTabCount()) {
                    tabs.setSelectedIndex(subroutineTabIndex);
                }
            }
        };

        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                close.setVisible(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                SwingUtilities.invokeLater(() -> close.setVisible(header.getMousePosition(true) != null));
            }
        };

        header.addMouseListener(hover);
        title.addMouseListener(hover);
        close.addMouseListener(hover);
        header.addMouseListener(selectTab);
        title.addMouseListener(selectTab);

        header.add(title);
        header.add(close);
        return header;
    }

    /** Highlights active line in the main pseudocode tab. */
    public void highlightMainLine(int zeroBasedIndex) {
        activeMainLineIndex = zeroBasedIndex;
        highlightLine(mainLineList, zeroBasedIndex);
    }

    /** Highlights active line in the sub-routine pseudocode tab. */
    public void highlightSubroutineLine(int zeroBasedIndex) {
        highlightLine(subroutineLineList, zeroBasedIndex);
    }

    /** Switches visual focus to the main tab without changing current highlights. */
    public void showMainTab() {
        tabs.setSelectedIndex(0);
    }

    /** Switches visual focus to the sub-routine tab if present. */
    public void showSubroutineTab() {
        if (subroutineTabIndex >= 0 && subroutineTabIndex < tabs.getTabCount()) {
            tabs.setSelectedIndex(subroutineTabIndex);
        }
    }

    /** Updates the warning label shown under the pseudocode list. */
    public void setExecutionWarning(String message) {
        executionWarningLabel.setText(message == null ? "" : message);
        executionWarningLabel.setVisible(message != null && !message.isBlank());
    }

    /** Clears transient algorithm-run UI state after playback is terminated. */
    public void resetForNoActiveRun() {
        removeSubroutineTab();
        callableSubroutineByLine = Map.of();
        canOpenSubroutineFromCurrentStep = false;
        hoveredMainIndex = -1;
        activeMainLineIndex = -1;
        mainLineList.setListData(new String[0]);
        mainLineList.clearSelection();
        subroutineLineList.setListData(new String[0]);
        subroutineLineList.clearSelection();
        tabs.setSelectedIndex(0);
        setExecutionWarning("");
        refreshOpenSubroutineButtons();
        updateMainListCursor();
        mainLineList.repaint();
    }

    private JButton createOpenSubroutineButton() {
        JButton button = new JButton("Open Sub-routine");
        button.setMargin(new Insets(2, 8, 2, 8));
        button.setVisible(false);
        button.setEnabled(false);
        button.setToolTipText("Open the sub-routine for the current call site in the main pseudocode.");
        button.addActionListener(e -> {
            if (onOpenSubroutineRequest != null) {
                onOpenSubroutineRequest.accept(mainLineList.getSelectedIndex());
            }
        });
        openSubroutineButtons.add(button);
        refreshOpenSubroutineButtons();
        return button;
    }

    private void refreshOpenSubroutineButtons() {
        boolean hasCallableSubroutines = !callableSubroutineByLine.isEmpty();
        boolean canOpenNow = hasCallableSubroutines && canOpenSubroutineFromCurrentStep;
        for (JButton button : openSubroutineButtons) {
            button.setVisible(hasCallableSubroutines);
            button.setEnabled(canOpenNow);
        }
        updateMainListCursor();
    }

    private JScrollPane wrapList(JList<String> list) {
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(205, 198, 182)));
        return scrollPane;
    }

    private JList<String> createCodeList(boolean mainList) {
        JList<String> list = new JList<>();
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFocusable(false);
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        list.setBackground(new Color(253, 251, 245));
        list.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        if (mainList) {
            list.setCellRenderer(new MainLineRenderer());
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    hoveredMainIndex = list.locationToIndex(e.getPoint());
                    updateMainListCursor();
                    list.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoveredMainIndex = -1;
                    updateMainListCursor();
                    list.repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    int index = list.locationToIndex(e.getPoint());
                    if (index < 0 || !canOpenSubroutineFromCurrentStep) {
                        return;
                    }
                    if (index == activeMainLineIndex
                            && callableSubroutineByLine.containsKey(index)
                            && onOpenSubroutineRequest != null) {
                        onOpenSubroutineRequest.accept(index);
                    }
                }
            };
            list.addMouseMotionListener(mouse);
            list.addMouseListener(mouse);
        } else {
            list.setCellRenderer(new LineRenderer());
        }

        return list;
    }

    private void updateMainListCursor() {
        boolean showAction = canOpenSubroutineFromCurrentStep
                && hoveredMainIndex >= 0
                && hoveredMainIndex == activeMainLineIndex
                && callableSubroutineByLine.containsKey(hoveredMainIndex);
        mainLineList.setCursor(showAction ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
    }

    private static void highlightLine(JList<String> list, int zeroBasedIndex) {
        if (zeroBasedIndex < 0 || zeroBasedIndex >= list.getModel().getSize()) {
            list.clearSelection();
            return;
        }
        list.setSelectedIndex(zeroBasedIndex);
        list.ensureIndexIsVisible(zeroBasedIndex);
    }

    /** Adds human-friendly line numbers to the raw pseudocode list. */
    private static String[] withLineNumbers(List<String> lines) {
        int width = Math.max(2, String.valueOf(lines.size()).length());
        String[] numbered = new String[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            numbered[i] = String.format("%" + width + "d | %s", i + 1, lines.get(i));
        }
        return numbered;
    }

    /** Renderer used for regular pseudocode lines. */
    private static class LineRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (isSelected) {
                component.setBackground(new Color(255, 238, 179));
                component.setForeground(new Color(28, 28, 28));
            } else {
                component.setBackground(new Color(253, 251, 245));
                component.setForeground(new Color(44, 44, 44));
            }
            return component;
        }
    }

    /** Main-list renderer that adds hover affordance for callable sub-routine lines. */
    private final class MainLineRenderer extends LineRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            boolean callable = callableSubroutineByLine.containsKey(index);
            boolean hovered = index == hoveredMainIndex;
            if (callable && hovered && canOpenSubroutineFromCurrentStep) {
                label.setText(value + "    [open sub-routine]");
                if (!isSelected) {
                    label.setForeground(new Color(24, 84, 170));
                }
            }
            return label;
        }
    }
}
