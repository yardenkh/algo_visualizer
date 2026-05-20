package ui.dialogs;

import algorithm.recursion.StackFrameView;
import algorithm.recursion.StackFramesSnapshot;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Modeless stack-frames viewer synchronized with playback timeline.
 *
 * Used by recursive algorithms whose steps attach {@link StackFramesSnapshot}
 * payloads to {@link algorithm.AlgorithmStep}.
 */
public class AlgorithmStackFramesDialog extends JDialog {
    private final JLabel titleLabel;
    private final JLabel subtitleLabel;
    private final JTree stackTree;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultTreeModel treeModel;
    private final StackTreeRenderer renderer;
    private StackFramesSnapshot previousSnapshot;

    /** Builds the stack-frame viewer window and tree controls. */
    public AlgorithmStackFramesDialog(Frame owner) {
        super(owner, "Algorithm Stack Frames", false);
        setLayout(new BorderLayout(8, 8));

        JPanel header = new JPanel(new BorderLayout(6, 4));
        header.setBorder(BorderFactory.createEmptyBorder(8, 10, 2, 10));
        titleLabel = new JLabel("Stack Frames", SwingConstants.LEFT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 15f));
        subtitleLabel = new JLabel("Live recursive call stack");
        subtitleLabel.setForeground(new Color(95, 105, 125));
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(subtitleLabel, BorderLayout.SOUTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controls.setOpaque(false);
        JButton expandAll = new JButton("Expand All");
        JButton collapseAll = new JButton("Collapse All");
        expandAll.setMargin(new Insets(2, 8, 2, 8));
        collapseAll.setMargin(new Insets(2, 8, 2, 8));
        expandAll.addActionListener(e -> expandAll());
        collapseAll.addActionListener(e -> collapseAll());
        controls.add(expandAll);
        controls.add(collapseAll);
        header.add(controls, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        rootNode = new DefaultMutableTreeNode("root");
        treeModel = new DefaultTreeModel(rootNode);
        stackTree = new JTree(treeModel);
        stackTree.setRootVisible(false);
        stackTree.setShowsRootHandles(true);
        stackTree.setRowHeight(24);
        stackTree.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        stackTree.setBackground(new Color(248, 250, 255));
        stackTree.setToggleClickCount(1);

        renderer = new StackTreeRenderer();
        stackTree.setCellRenderer(renderer);

        JScrollPane scrollPane = new JScrollPane(stackTree);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(210, 218, 235)));
        add(scrollPane, BorderLayout.CENTER);

        setSize(520, 480);
        setLocationByPlatform(true);
    }

    /** Shows the current recursive stack snapshot for the active algorithm step. */
    public void showStack(String algorithmName, StackFramesSnapshot snapshot) {
        if (snapshot == null) {
            previousSnapshot = null;
            return;
        }

        setTitle("Stack Frames - " + algorithmName);
        titleLabel.setText(snapshot.getTitle().isBlank() ? "Stack Frames" : snapshot.getTitle());
        subtitleLabel.setText(snapshot.getFrames().isEmpty()
                ? "(stack is empty)"
                : "Top frame first, click arrows to collapse/expand individual frames");

        rebuildTree(snapshot);

    }

    /** Rebuilds the entire tree model from the newest stack snapshot. */
    private void rebuildTree(StackFramesSnapshot snapshot) {
        Map<String, String> previousValues = flatten(previousSnapshot);
        Map<String, String> currentValues = flatten(snapshot);

        Set<String> changedKeys = new HashSet<>();
        for (Map.Entry<String, String> entry : currentValues.entrySet()) {
            String before = previousValues.get(entry.getKey());
            if (before == null || !before.equals(entry.getValue())) {
                changedKeys.add(entry.getKey());
            }
        }

        rootNode.removeAllChildren();

        if (snapshot.getFrames().isEmpty()) {
            DefaultMutableTreeNode empty = new DefaultMutableTreeNode(new VarNodeData("(stack is empty)", "", false));
            rootNode.add(empty);
        } else {
            int frameNo = snapshot.getFrames().size();
            for (int i = 0; i < snapshot.getFrames().size(); i++) {
                StackFrameView frame = snapshot.getFrames().get(i);
                String frameId = frame.getMethodName() + "#" + i;

                FrameNodeData frameData = new FrameNodeData(frameNo--, frame.getMethodName(), methodColor(frame.getMethodName()));
                DefaultMutableTreeNode frameNode = new DefaultMutableTreeNode(frameData);

                for (Map.Entry<String, String> variable : frame.getVariables().entrySet()) {
                    String key = frameId + "|" + variable.getKey();
                    boolean changed = changedKeys.contains(key);
                    frameNode.add(new DefaultMutableTreeNode(new VarNodeData(variable.getKey(), variable.getValue(), changed)));
                }

                rootNode.add(frameNode);
            }
        }

        treeModel.reload();
        expandAll();
        previousSnapshot = snapshot;
    }

    /** Expands all frame nodes. */
    private void expandAll() {
        for (int i = 0; i < stackTree.getRowCount(); i++) {
            stackTree.expandRow(i);
        }
    }

    /** Collapses all frame nodes. */
    private void collapseAll() {
        for (int i = stackTree.getRowCount() - 1; i >= 0; i--) {
            TreePath path = stackTree.getPathForRow(i);
            if (path != null && path.getPathCount() > 1) {
                stackTree.collapsePath(path);
            }
        }
    }

    /** Flattens a snapshot into key/value strings so changed variables can be detected. */
    private static Map<String, String> flatten(StackFramesSnapshot snapshot) {
        Map<String, String> map = new LinkedHashMap<>();
        if (snapshot == null) {
            return map;
        }
        for (int i = 0; i < snapshot.getFrames().size(); i++) {
            StackFrameView frame = snapshot.getFrames().get(i);
            String frameId = frame.getMethodName() + "#" + i;
            for (Map.Entry<String, String> variable : frame.getVariables().entrySet()) {
                map.put(frameId + "|" + variable.getKey(), variable.getValue());
            }
        }
        return map;
    }

    /** Produces a deterministic color per method name for easier visual tracking. */
    private static Color methodColor(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            return new Color(120, 130, 150);
        }
        int hash = Math.abs(methodName.hashCode());
        int r = 80 + (hash % 120);
        int g = 80 + ((hash / 7) % 120);
        int b = 80 + ((hash / 13) % 120);
        return new Color(r, g, b);
    }

    /** Tree node data for stack frame headers. */
    private static final class FrameNodeData {
        private final int index;
        private final String methodName;
        private final Color methodColor;

        private FrameNodeData(int index, String methodName, Color methodColor) {
            this.index = index;
            this.methodName = methodName;
            this.methodColor = methodColor;
        }
    }

    /** Tree node data for individual variables within a frame. */
    private static final class VarNodeData {
        private final String key;
        private final String value;
        private final boolean changed;

        private VarNodeData(String key, String value, boolean changed) {
            this.key = key;
            this.value = value;
            this.changed = changed;
        }
    }

    /** Custom tree renderer for modern frame/variable styling. */
    private static final class StackTreeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean sel,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus
        ) {
            super.getTreeCellRendererComponent(tree, value, false, expanded, leaf, row, false);
            setOpaque(true);

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object user = node.getUserObject();

            if (user instanceof FrameNodeData data) {
                setText("Frame #" + data.index + "    " + data.methodName);
                setForeground(data.methodColor.darker());
                setBackground(new Color(238, 244, 255));
                setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
                setFont(getFont().deriveFont(Font.BOLD));
                return this;
            }

            if (user instanceof VarNodeData data) {
                setText(data.key.isBlank() ? data.value : (data.key + " = " + data.value));
                if (data.changed) {
                    setForeground(new Color(0, 113, 41));
                    setBackground(new Color(232, 250, 238));
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(new Color(55, 55, 55));
                    setBackground(Color.WHITE);
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
                return this;
            }

            setForeground(new Color(80, 80, 80));
            setBackground(Color.WHITE);
            return this;
        }
    }

    /**
     * Returns a lightweight embeddable panel sharing the same JTree and header labels.
     *
     * Safe to embed in a workspace tab without reparenting the dialog's own content pane.
     */
    public JPanel createEmbeddablePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel header = new JPanel(new BorderLayout(6, 4));
        header.setBorder(BorderFactory.createEmptyBorder(6, 8, 2, 8));
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(subtitleLabel, BorderLayout.SOUTH);
        panel.add(header, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(stackTree);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(210, 218, 235)));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
}
