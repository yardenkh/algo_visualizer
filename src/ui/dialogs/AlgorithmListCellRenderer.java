package ui.dialogs;

import algorithm.GraphAlgorithm;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import java.awt.Component;

/**
 * Displays algorithm names cleanly inside combo boxes.
 * Instead of showing object identities, this renderer uses
 * {@link algorithm.GraphAlgorithm#getDisplayName()}.
 */
public class AlgorithmListCellRenderer extends DefaultListCellRenderer {
    /** Converts algorithm objects into readable combo-box labels. */
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Object displayValue = value;
        if (value instanceof GraphAlgorithm algorithm) {
            displayValue = algorithm.getDisplayName();
        }
        return super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
    }
}
