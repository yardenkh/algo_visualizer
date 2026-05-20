package ui.dialogs;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Popup dialog for extra actions that operate on the currently selected subgraph.
 */
public final class SelectionExtrasDialog extends JDialog {
    private final Supplier<String> scopeSummarySupplier;
    private final JLabel scopeLabel;

    public SelectionExtrasDialog(
            JFrame owner,
            Runnable onFindVertexColoring,
            Runnable onFindEdgeColoring,
            Runnable onComplementGraph,
            Runnable onTurnIntoLineGraph,
            Runnable onFindRootGraph,
            Runnable onReduceEdgeCrossings,
            Supplier<String> scopeSummarySupplier
    ) {
        super(owner, "Extras", false);
        this.scopeSummarySupplier = Objects.requireNonNull(scopeSummarySupplier, "scopeSummarySupplier cannot be null");

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        scopeLabel = new JLabel();
        content.add(scopeLabel, BorderLayout.NORTH);

        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));

        JButton vertexColoringButton = new JButton("Find Vertex Coloring");
        vertexColoringButton.setAlignmentX(LEFT_ALIGNMENT);
        vertexColoringButton.addActionListener(e -> onFindVertexColoring.run());

        JButton edgeColoringButton = new JButton("Find Edge Coloring");
        edgeColoringButton.setAlignmentX(LEFT_ALIGNMENT);
        edgeColoringButton.addActionListener(e -> onFindEdgeColoring.run());

        JButton complementGraphButton = new JButton("Complement Graph");
        complementGraphButton.setAlignmentX(LEFT_ALIGNMENT);
        complementGraphButton.addActionListener(e -> onComplementGraph.run());

        JButton lineGraphButton = new JButton("Turn Into Line Graph");
        lineGraphButton.setAlignmentX(LEFT_ALIGNMENT);
        lineGraphButton.addActionListener(e -> onTurnIntoLineGraph.run());

        JButton rootGraphButton = new JButton("Find Root Graph");
        rootGraphButton.setAlignmentX(LEFT_ALIGNMENT);
        rootGraphButton.addActionListener(e -> onFindRootGraph.run());

        JButton reduceCrossingsButton = new JButton("Reduce Edge Crossings");
        reduceCrossingsButton.setAlignmentX(LEFT_ALIGNMENT);
        reduceCrossingsButton.addActionListener(e -> onReduceEdgeCrossings.run());

        options.add(vertexColoringButton);
        options.add(Box.createVerticalStrut(8));
        options.add(edgeColoringButton);
        options.add(Box.createVerticalStrut(8));
        options.add(complementGraphButton);
        options.add(Box.createVerticalStrut(8));
        options.add(lineGraphButton);
        options.add(Box.createVerticalStrut(8));
        options.add(rootGraphButton);
        options.add(Box.createVerticalStrut(8));
        options.add(reduceCrossingsButton);

        content.add(options, BorderLayout.CENTER);
        setContentPane(content);
        setMinimumSize(new Dimension(360, 370));
        setSize(400, 390);
        setLocationRelativeTo(owner);
    }

    /** Opens the popup and refreshes the selected-subgraph summary text. */
    public void open() {
        scopeLabel.setText(scopeSummarySupplier.get());
        setVisible(true);
        toFront();
    }
}

