package ui;

import graph.Edge;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Builds the right-side inspector/action panel used by {@link GraphEditorFrame}.
 *
 * <p>This class extracts UI construction details so the frame can focus on wiring and flow control.
 */
final class GraphEditorSidePanelBuilder {

    @FunctionalInterface
    interface ButtonDecorator {
        void decorate(AbstractButton button, String label, String shortcut);
    }

    private final GraphEditorFrame owner;
    private final GraphEditorController controller;
    private final ButtonDecorator decorator;
    private final JSpinner edgeWeightSpinner;
    private final JToggleButton undirectedModeToggle;
    private final JButton extrasButton;
    private final JTextArea algorithmInfoArea;
    private final Runnable onChooseDefaultEdgeColor;
    private final Runnable onToggleUndirectedCreation;
    private final Runnable onColorVertices;
    private final Runnable onColorSelectedEdge;
    private final Runnable onPromptSelectedEdgeWeight;
    private final Runnable onQuickLayout;
    private final Runnable onOpenExtras;

    GraphEditorSidePanelBuilder(
            GraphEditorFrame owner,
            GraphEditorController controller,
            ButtonDecorator decorator,
            JSpinner edgeWeightSpinner,
            JToggleButton undirectedModeToggle,
            JButton extrasButton,
            JTextArea algorithmInfoArea,
            Runnable onChooseDefaultEdgeColor,
            Runnable onToggleUndirectedCreation,
            Runnable onColorVertices,
            Runnable onColorSelectedEdge,
            Runnable onPromptSelectedEdgeWeight,
            Runnable onQuickLayout,
            Runnable onOpenExtras
    ) {
        this.owner = owner;
        this.controller = controller;
        this.decorator = decorator;
        this.edgeWeightSpinner = edgeWeightSpinner;
        this.undirectedModeToggle = undirectedModeToggle;
        this.extrasButton = extrasButton;
        this.algorithmInfoArea = algorithmInfoArea;
        this.onChooseDefaultEdgeColor = onChooseDefaultEdgeColor;
        this.onToggleUndirectedCreation = onToggleUndirectedCreation;
        this.onColorVertices = onColorVertices;
        this.onColorSelectedEdge = onColorSelectedEdge;
        this.onPromptSelectedEdgeWeight = onPromptSelectedEdgeWeight;
        this.onQuickLayout = onQuickLayout;
        this.onOpenExtras = onOpenExtras;
    }

    JScrollPane build() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        content.add(section("Connection Defaults", buildConnectionDefaultsPanel()));
        content.add(Box.createVerticalStrut(10));
        content.add(section("Selection Actions", buildSelectionActionsPanel()));
        content.add(Box.createVerticalStrut(10));
        content.add(section("Algorithm Playback", buildAlgorithmPanel()));
        content.add(Box.createVerticalStrut(10));
        content.add(section("Tips", buildTipsPanel()));
        content.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setPreferredSize(new Dimension(350, 0));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    private JPanel buildConnectionDefaultsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JButton chooseEdgeColor = new JButton();
        decorator.decorate(chooseEdgeColor, "Default Edge Color", "Ctrl+E");
        chooseEdgeColor.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        chooseEdgeColor.addActionListener(e -> onChooseDefaultEdgeColor.run());

        JPanel edgeWeightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        edgeWeightPanel.add(new JLabel("Default Edge Weight:"));
        edgeWeightPanel.add(edgeWeightSpinner);
        edgeWeightPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        JLabel helper = new JLabel("Used automatically for every new edge");
        helper.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        undirectedModeToggle.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        undirectedModeToggle.addActionListener(e -> onToggleUndirectedCreation.run());

        panel.add(chooseEdgeColor);
        panel.add(Box.createVerticalStrut(6));
        panel.add(edgeWeightPanel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(helper);
        panel.add(Box.createVerticalStrut(8));
        panel.add(undirectedModeToggle);
        return panel;
    }

    private JPanel buildSelectionActionsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JButton colorVertices = new JButton();
        decorator.decorate(colorVertices, "Color Selected Vertices", "Ctrl+Shift+V");
        colorVertices.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        colorVertices.addActionListener(e -> onColorVertices.run());

        JButton colorEdge = new JButton();
        decorator.decorate(colorEdge, "Color Selected Edge", "Ctrl+Shift+E");
        colorEdge.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        colorEdge.addActionListener(e -> {
            Edge selectedEdge = controller.getSelectedEdge();
            if (selectedEdge == null) {
                JOptionPane.showMessageDialog(owner, "Select an edge first.");
                return;
            }
            onColorSelectedEdge.run();
        });

        JButton setEdgeWeight = new JButton();
        decorator.decorate(setEdgeWeight, "Set Selected Edge Weight", "W");
        setEdgeWeight.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        setEdgeWeight.addActionListener(e -> onPromptSelectedEdgeWeight.run());

        JButton quickLayout = new JButton();
        decorator.decorate(quickLayout, "Auto Layout (Circle)", "Ctrl+L");
        quickLayout.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        quickLayout.addActionListener(e -> onQuickLayout.run());

        decorator.decorate(extrasButton, "Extras", "Ctrl+Shift+O");
        extrasButton.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        extrasButton.addActionListener(e -> onOpenExtras.run());

        panel.add(colorVertices);
        panel.add(Box.createVerticalStrut(6));
        panel.add(colorEdge);
        panel.add(Box.createVerticalStrut(6));
        panel.add(setEdgeWeight);
        panel.add(Box.createVerticalStrut(6));
        panel.add(quickLayout);
        panel.add(Box.createVerticalStrut(6));
        panel.add(extrasButton);
        return panel;
    }

    private JPanel buildAlgorithmPanel() {
        JPanel panel = new JPanel(new java.awt.BorderLayout(6, 6));

        JLabel hintLabel = new JLabel("Select vertices, then run and step with ← ■ →");
        panel.add(hintLabel, java.awt.BorderLayout.NORTH);

        algorithmInfoArea.setEditable(false);
        algorithmInfoArea.setLineWrap(true);
        algorithmInfoArea.setWrapStyleWord(true);
        algorithmInfoArea.setRows(8);
        algorithmInfoArea.setMargin(new java.awt.Insets(6, 6, 6, 6));
        panel.add(new JScrollPane(algorithmInfoArea), java.awt.BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildTipsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("- Shift+Click / Box for multi-select"));
        panel.add(new JLabel("- Drag selected vertices together"));
        panel.add(new JLabel("- Ctrl+A selects all vertices"));
        panel.add(new JLabel("- Delete removes selection"));
        panel.add(new JLabel("- Mouse wheel zooms, middle button pans"));
        panel.add(new JLabel("- Left/Right arrows step playback"));
        panel.add(new JLabel("- Escape stops playback"));
        panel.add(new JLabel("- Ctrl+Z undo last edit"));
        return panel;
    }

    private JScrollPane section(String title, JPanel content) {
        content.setBorder(BorderFactory.createTitledBorder(title));
        JScrollPane wrapper = new JScrollPane(content);
        wrapper.setBorder(BorderFactory.createEmptyBorder());
        wrapper.setPreferredSize(new Dimension(320, content.getPreferredSize().height + 24));
        wrapper.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        wrapper.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        return wrapper;
    }
}

