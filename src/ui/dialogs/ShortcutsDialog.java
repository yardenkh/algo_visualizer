package ui.dialogs;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Frame;

/**
 * Modeless help window that lists all available keyboard shortcuts.
 * Content is supplied by {@link ui.GraphEditorFrame} so the dialog stays as a passive viewer.
 */
public class ShortcutsDialog extends JDialog {
    private final JTextArea textArea;

    /** Builds the read-only shortcuts window. */
    public ShortcutsDialog(Frame owner) {
        super(owner, "Keyboard Shortcuts", false);
        setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(false);
        textArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        add(new JScrollPane(textArea), BorderLayout.CENTER);
        setSize(560, 420);
        setLocationByPlatform(true);
    }

    /** Replaces the visible shortcuts text and scrolls back to the top. */
    public void setContent(String content) {
        textArea.setText(content);
        textArea.setCaretPosition(0);
    }
}
