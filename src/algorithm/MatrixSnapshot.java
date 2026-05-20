package algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable matrix payload attached to a timeline step for matrix visualizations.
 * Consumed by {@link ui.dialogs.AlgorithmMatrixDialog}.
 */
public final class MatrixSnapshot {
    private final String title;
    private final List<String> rowLabels;
    private final List<String> columnLabels;
    private final List<List<String>> cells;
    private final int highlightedRow;
    private final int highlightedColumn;

    /** Creates one immutable matrix snapshot for a timeline step. */
    public MatrixSnapshot(
            String title,
            List<String> rowLabels,
            List<String> columnLabels,
            List<List<String>> cells,
            int highlightedRow,
            int highlightedColumn
    ) {
        this.title = title == null ? "" : title;
        this.rowLabels = Collections.unmodifiableList(new ArrayList<>(rowLabels));
        this.columnLabels = Collections.unmodifiableList(new ArrayList<>(columnLabels));

        List<List<String>> frozenCells = new ArrayList<>();
        for (List<String> row : cells) {
            frozenCells.add(Collections.unmodifiableList(new ArrayList<>(row)));
        }
        this.cells = Collections.unmodifiableList(frozenCells);

        this.highlightedRow = highlightedRow;
        this.highlightedColumn = highlightedColumn;
    }

    /** @return matrix title shown in the matrix dialog header. */
    public String getTitle() {
        return title;
    }

    /** @return row labels for the matrix view. */
    public List<String> getRowLabels() {
        return rowLabels;
    }

    /** @return column labels for the matrix view. */
    public List<String> getColumnLabels() {
        return columnLabels;
    }

    /** @return immutable matrix cell text. */
    public List<List<String>> getCells() {
        return cells;
    }

    /** @return highlighted row index, or -1 when none is active. */
    public int getHighlightedRow() {
        return highlightedRow;
    }

    /** @return highlighted column index, or -1 when none is active. */
    public int getHighlightedColumn() {
        return highlightedColumn;
    }
}
