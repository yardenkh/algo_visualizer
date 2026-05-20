package ui.dialogs;

import algorithm.MatrixSnapshot;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;

/**
 * Modeless matrix viewer synchronized with algorithm playback steps.
 *
 * This dialog is used by matrix-based algorithms (for example Floyd-Warshall and
 * matrix APSP variants) to show the currently active matrix state.
 */
public class AlgorithmMatrixDialog extends JDialog {
    private final JLabel titleLabel;
    private final JTable table;
    private final MatrixRenderer renderer;

    /** Builds the modeless matrix dialog and its table renderer. */
    public AlgorithmMatrixDialog(Frame owner) {
        super(owner, "Algorithm Matrix", false);
        setLayout(new BorderLayout(6, 6));

        titleLabel = new JLabel("Matrix", SwingConstants.LEFT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 2, 8));
        add(titleLabel, BorderLayout.NORTH);

        table = new JTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(24);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        table.setFillsViewportHeight(true);
        table.setEnabled(false);

        renderer = new MatrixRenderer();
        table.setDefaultRenderer(Object.class, renderer);
        table.getTableHeader().setReorderingAllowed(false);

        add(new JScrollPane(table), BorderLayout.CENTER);
        setSize(700, 420);
        setLocationByPlatform(true);
    }

    /**
     * Returns a lightweight embeddable panel that shares the same JTable and title label.
     *
     * This panel can be embedded inside a workspace tab without Swing content-pane reparenting.
     * Multiple calls always return a NEW panel (safe to embed in multiple containers).
     */
    public JPanel createEmbeddablePanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Displays or refreshes the matrix from the current timeline step.
     */
    public void showMatrix(String algorithmName, MatrixSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        setTitle("Matrix - " + algorithmName);
        titleLabel.setText(snapshot.getTitle().isBlank() ? "Matrix" : snapshot.getTitle());

        int rows = snapshot.getCells().size();
        int cols = snapshot.getColumnLabels().size();
        Object[] headers = new Object[cols + 1];
        headers[0] = "";
        for (int c = 0; c < cols; c++) {
            headers[c + 1] = snapshot.getColumnLabels().get(c);
        }

        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (int r = 0; r < rows; r++) {
            Object[] row = new Object[cols + 1];
            row[0] = snapshot.getRowLabels().get(r);
            for (int c = 0; c < cols; c++) {
                row[c + 1] = snapshot.getCells().get(r).get(c);
            }
            model.addRow(row);
        }

        table.setModel(model);
        renderer.setHighlights(snapshot.getHighlightedRow(), snapshot.getHighlightedColumn());

        for (int c = 0; c < table.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setPreferredWidth(c == 0 ? 70 : 80);
        }

        repaint();
    }

    /** Table renderer that highlights current row/column focus from the algorithm step. */
    private static final class MatrixRenderer extends DefaultTableCellRenderer {
        private int highlightedRow;
        private int highlightedColumn;

        /** Updates which matrix row/column should be visually emphasized. */
        private void setHighlights(int highlightedRow, int highlightedColumn) {
            this.highlightedRow = highlightedRow;
            this.highlightedColumn = highlightedColumn;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            Component component = super.getTableCellRendererComponent(table, value, false, false, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);

            if (column == 0) {
                component.setBackground(new Color(237, 243, 255));
                component.setForeground(new Color(30, 52, 95));
                return component;
            }

            int matrixCol = column - 1;
            if (row == highlightedRow && matrixCol == highlightedColumn) {
                component.setBackground(new Color(255, 234, 167));
                component.setForeground(new Color(20, 20, 20));
            } else if (row == highlightedRow || matrixCol == highlightedColumn) {
                component.setBackground(new Color(247, 252, 255));
                component.setForeground(new Color(40, 40, 40));
            } else {
                component.setBackground(Color.WHITE);
                component.setForeground(new Color(55, 55, 55));
            }
            return component;
        }
    }
}
