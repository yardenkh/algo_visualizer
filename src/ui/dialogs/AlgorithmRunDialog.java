package ui.dialogs;

import algorithm.AlgorithmType;
import algorithm.GraphAlgorithm;
import graph.Vertex;
import ui.AlgorithmDetailsFormatter;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal dialog for choosing which algorithm to run and from which source vertex.
 *
 * This dialog is the configuration front-end for the run flow started in
 * {@link ui.GraphEditorFrame}. It filters algorithms by {@link algorithm.AlgorithmType},
 * shows formatted descriptions, and returns an {@link AlgorithmRunConfiguration}.
 *
 * Roadmap: see {@code project roadmap.md}.
 */
public class AlgorithmRunDialog extends JDialog {
    private AlgorithmRunConfiguration configuration;

    /**
     * Builds and wires the dialog UI.
     *
     * Key dynamic behaviors:
     * - changing type filters available algorithms
     * - changing algorithm refreshes description and source requirements
     */
    public AlgorithmRunDialog(Frame owner, List<GraphAlgorithm> algorithms, List<Vertex> scopeVertices, String scopeLabel) {
        super(owner, "Run Algorithm", true);
        setLayout(new BorderLayout(10, 10));

        JComboBox<AlgorithmType> typeCombo = new JComboBox<>(AlgorithmType.values());
        DefaultComboBoxModel<GraphAlgorithm> algorithmModel = new DefaultComboBoxModel<>();
        JComboBox<GraphAlgorithm> algorithmCombo = new JComboBox<>(algorithmModel);
        algorithmCombo.setRenderer(new AlgorithmListCellRenderer());

        JComboBox<VertexOption> sourceCombo = new JComboBox<>();
        JComboBox<VertexOption> sinkCombo = new JComboBox<>();
        for (Vertex vertex : scopeVertices) {
            VertexOption option = new VertexOption(vertex);
            sourceCombo.addItem(option);
            sinkCombo.addItem(option);
        }

        JTextArea descriptionArea = new JTextArea();
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setRows(12);
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        Runnable refreshAlgorithmList = () -> {
            AlgorithmType selectedType = (AlgorithmType) typeCombo.getSelectedItem();
            List<GraphAlgorithm> filtered = filterByType(algorithms, selectedType);
            algorithmModel.removeAllElements();
            for (GraphAlgorithm algorithm : filtered) {
                algorithmModel.addElement(algorithm);
            }
            if (!filtered.isEmpty()) {
                algorithmCombo.setSelectedIndex(0);
            }
        };

        Runnable refreshDescriptionAndSource = () -> {
            GraphAlgorithm algorithm = (GraphAlgorithm) algorithmCombo.getSelectedItem();
            if (algorithm == null) {
                descriptionArea.setText("No algorithm available for this type.");
                sourceCombo.setEnabled(false);
                sinkCombo.setEnabled(false);
                return;
            }
            descriptionArea.setText(AlgorithmDetailsFormatter.format(algorithm));
            descriptionArea.setCaretPosition(0);
            sourceCombo.setEnabled(algorithm.requiresSourceVertex());
            sinkCombo.setEnabled(algorithm.requiresSinkVertex());
        };

        typeCombo.addActionListener(e -> {
            refreshAlgorithmList.run();
            refreshDescriptionAndSource.run();
        });
        algorithmCombo.addActionListener(e -> refreshDescriptionAndSource.run());

        typeCombo.setSelectedItem(AlgorithmType.ALL_ALGORITHMS);
        refreshAlgorithmList.run();
        refreshDescriptionAndSource.run();

        JPanel form = new JPanel(new BorderLayout(8, 8));
        JPanel controls = new JPanel(new java.awt.GridLayout(0, 1, 6, 6));
        controls.add(new JLabel(scopeLabel));
        controls.add(new JLabel("Type:"));
        controls.add(typeCombo);
        controls.add(new JLabel("Algorithm:"));
        controls.add(algorithmCombo);
        controls.add(new JLabel("Source vertex:"));
        controls.add(sourceCombo);
        controls.add(new JLabel("Sink vertex:"));
        controls.add(sinkCombo);
        form.add(controls, BorderLayout.NORTH);
        form.add(new JScrollPane(descriptionArea), BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        JButton runButton = new JButton("Run");
        runButton.addActionListener(e -> {
            GraphAlgorithm algorithm = (GraphAlgorithm) algorithmCombo.getSelectedItem();
            VertexOption sourceOption = (VertexOption) sourceCombo.getSelectedItem();
            VertexOption sinkOption = (VertexOption) sinkCombo.getSelectedItem();
            Integer sourceId = (algorithm != null && algorithm.requiresSourceVertex() && sourceOption != null)
                    ? sourceOption.vertex.getId()
                    : null;
            Integer sinkId = (algorithm != null && algorithm.requiresSinkVertex() && sinkOption != null)
                    ? sinkOption.vertex.getId()
                    : null;
            configuration = new AlgorithmRunConfiguration(algorithm, sourceId, sinkId);
            dispose();
        });
        runButton.setEnabled(algorithmModel.getSize() > 0 && !scopeVertices.isEmpty());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(cancelButton);
        footer.add(runButton);

        add(form, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        setSize(560, 500);
        setLocationRelativeTo(owner);
    }

    /** Filters algorithms for the selected type tab in the dialog. */
    private static List<GraphAlgorithm> filterByType(List<GraphAlgorithm> all, AlgorithmType type) {
        if (type == null || type == AlgorithmType.ALL_ALGORITHMS) {
            return new ArrayList<>(all);
        }
        List<GraphAlgorithm> filtered = new ArrayList<>();
        for (GraphAlgorithm algorithm : all) {
            if (algorithm.getType() == type) {
                filtered.add(algorithm);
            }
        }
        return filtered;
    }

    /** @return the user selection captured when Run was pressed, or null if canceled. */
    public AlgorithmRunConfiguration getConfiguration() {
        return configuration;
    }

    /** Simple combo-box wrapper that shows vertex names while preserving ids. */
    private static final class VertexOption {
        private final Vertex vertex;

        private VertexOption(Vertex vertex) {
            this.vertex = vertex;
        }

        @Override
        public String toString() {
            return vertex.getName();
        }
    }
}
