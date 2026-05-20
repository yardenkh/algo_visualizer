package ui.dialogs;

import graph.Edge;
import graph.Vertex;

import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Modeless companion window for the topological-order algorithm.
 * It visualizes the partial linear order produced so far and draws only forward-going
 * arcs among the currently revealed vertices.
 */
public class TopologicalOrderDialog extends JDialog {
    private final OrderPanel panel;
    private int shownCount;

    /** Builds the order-visualization window. */
    public TopologicalOrderDialog(Frame owner) {
        super(owner, "Topological Order Demonstration", false);
        setLayout(new BorderLayout());

        panel = new OrderPanel();
        panel.setPreferredSize(new Dimension(900, 320));
        add(panel, BorderLayout.CENTER);

        setSize(980, 380);
        setLocationByPlatform(true);
    }

    /** Loads the full order data and shows the dialog from the beginning of the run. */
    public void showOrder(List<Vertex> order, List<Edge> edges) {
        setOrderData(order, edges);
    }

    /** Loads/refreshes order data without opening a standalone popup window. */
    public void setOrderData(List<Vertex> order, List<Edge> edges) {
        panel.setData(order, edges);
        setShownCount(0);
    }

    /** Updates how many vertices from the final order are currently revealed. */
    public void updateShownCount(int value) {
        setShownCount(value);
    }

    /** Resets the reveal counter and hides the dialog. */
    public void resetAndHide() {
        setShownCount(0);
        setVisible(false);
    }

    /**
     * Returns a lightweight embeddable panel containing the topological-order canvas.
     * Shares the same {@link OrderPanel} so it reacts to the same data updates.
     */
    public JPanel createEmbeddablePanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    /** Clamps and forwards the reveal count into the drawing panel. */
    private void setShownCount(int value) {
        int max = panel.vertexCount();
        shownCount = Math.max(0, Math.min(max, value));
        panel.setShownCount(shownCount);
    }

    /** Custom panel that draws the linear order and forward arcs. */
    private static final class OrderPanel extends JPanel {
        private List<Vertex> order = List.of();
        private List<Edge> edges = List.of();
        private int shownCount;

        /** Replaces the full order/edge data shown by the panel. */
        private void setData(List<Vertex> order, List<Edge> edges) {
            this.order = new ArrayList<>(order);
            this.edges = new ArrayList<>(edges);
            this.shownCount = 0;
            repaint();
        }

        /** @return total number of vertices in the current topological order. */
        private int vertexCount() {
            return order.size();
        }

        /** Updates reveal count and repaints the panel. */
        private void setShownCount(int shownCount) {
            this.shownCount = shownCount;
            repaint();
        }

        /** Paints the currently revealed prefix of the topological order. */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int n = Math.max(1, shownCount);
            int baseY = getHeight() - 90;
            int startX = 80;
            int gap = Math.max(70, (getWidth() - 160) / Math.max(1, n - 1));

            List<Vertex> visible = order.subList(0, Math.min(shownCount, order.size()));
            java.util.Map<Integer, Integer> xById = new java.util.HashMap<>();
            for (int i = 0; i < visible.size(); i++) {
                xById.put(visible.get(i).getId(), startX + i * gap);
            }

            g2.setColor(new Color(80, 97, 120));
            for (Edge edge : edges) {
                Integer x1 = xById.get(edge.getSource().getId());
                Integer x2 = xById.get(edge.getDestination().getId());
                if (x1 == null || x2 == null || x2 <= x1) {
                    continue;
                }
                int ctrlY = baseY - 40 - Math.abs(x2 - x1) / 5;
                QuadCurve2D curve = new QuadCurve2D.Double(x1, baseY, (x1 + x2) / 2.0, ctrlY, x2, baseY);
                g2.draw(curve);
            }

            for (int i = 0; i < visible.size(); i++) {
                Vertex v = visible.get(i);
                int x = startX + i * gap;
                g2.setColor(new Color(79, 129, 189));
                g2.fillOval(x - 18, baseY - 18, 36, 36);
                g2.setColor(Color.WHITE);
                String label = v.getName();
                int w = g2.getFontMetrics().stringWidth(label);
                g2.drawString(label, x - w / 2, baseY + 5);
            }

            g2.setColor(new Color(50, 50, 50));
            g2.drawString("Shown vertices: " + shownCount + " / " + order.size(), 10, 20);
            g2.dispose();
        }
    }
}
