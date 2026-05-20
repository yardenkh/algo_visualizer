package ui.dialogs;

import algorithm.EdgeKey;
import algorithm.ResidualGraphSnapshot;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Modeless residual-graph window synchronized with maximum-flow playback steps. */
public class ResidualGraphDialog extends JDialog {
    private static final int VERTEX_RADIUS = 18;
    private final ResidualPanel panel;

    public ResidualGraphDialog(Frame owner) {
        super(owner, "Residual Graph", false);
        setLayout(new BorderLayout());
        panel = new ResidualPanel();
        add(new JScrollPane(panel), BorderLayout.CENTER);
        setSize(980, 600);
        setLocationByPlatform(true);
    }

    public void showSnapshot(String algorithmName, ResidualGraphSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        setTitle("Residual Graph - " + algorithmName);
        panel.setSnapshot(snapshot);
    }

    /**
     * Returns a lightweight embeddable panel containing the residual-graph canvas.
     * The panel shares the same {@link ResidualPanel} rendering component.
     */
    public JPanel createEmbeddablePanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(new JScrollPane(panel), BorderLayout.CENTER);
        return wrapper;
    }

    private static final class ResidualPanel extends JPanel {
        private ResidualGraphSnapshot snapshot;
        private double zoom = 1.0;
        private double panX;
        private double panY;
        private Point lastDrag;
        private boolean panning;

        private ResidualPanel() {
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON2) {
                        panning = true;
                        lastDrag = e.getPoint();
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!panning || lastDrag == null) {
                        return;
                    }
                    panX += e.getX() - lastDrag.x;
                    panY += e.getY() - lastDrag.y;
                    lastDrag = e.getPoint();
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON2) {
                        panning = false;
                        lastDrag = null;
                    }
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    double factor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
                    double next = Math.max(0.25, Math.min(4.0, zoom * factor));
                    if (Math.abs(next - zoom) < 1e-9) {
                        return;
                    }
                    double sx = e.getX();
                    double sy = e.getY();
                    panX = sx - (sx - panX) * (next / zoom);
                    panY = sy - (sy - panY) * (next / zoom);
                    zoom = next;
                    repaint();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);
        }

        private void setSnapshot(ResidualGraphSnapshot snapshot) {
            this.snapshot = snapshot;
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(900, 520);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (snapshot == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Map<Integer, Point2D.Double> points = projectedLayout(snapshot, getWidth(), getHeight());
            Set<EdgeKey> highlighted = new LinkedHashSet<>(snapshot.getHighlightedEdges());
            Set<EdgeKey> edgeSet = new LinkedHashSet<>();
            for (ResidualGraphSnapshot.ResidualEdge edge : snapshot.getEdges()) {
                edgeSet.add(new EdgeKey(edge.getSourceId(), edge.getDestinationId()));
            }

            for (ResidualGraphSnapshot.ResidualEdge edge : snapshot.getEdges()) {
                Point2D.Double s = points.get(edge.getSourceId());
                Point2D.Double d = points.get(edge.getDestinationId());
                if (s == null || d == null) {
                    continue;
                }
                EdgeKey key = new EdgeKey(edge.getSourceId(), edge.getDestinationId());
                boolean dual = edgeSet.contains(new EdgeKey(edge.getDestinationId(), edge.getSourceId()));
                boolean highlightedEdge = highlighted.contains(key);
                drawEdge(g2, s, d, edge.getCapacity(), dual, edge.getSourceId() < edge.getDestinationId(), highlightedEdge);
            }

            for (Integer id : snapshot.getVertexIds()) {
                Point2D.Double p = points.get(id);
                if (p == null) {
                    continue;
                }
                g2.setColor(new Color(77, 128, 191));
                g2.fillOval((int) p.x - VERTEX_RADIUS, (int) p.y - VERTEX_RADIUS, VERTEX_RADIUS * 2, VERTEX_RADIUS * 2);
                g2.setColor(Color.WHITE);
                String label = snapshot.getVertexLabels().getOrDefault(id, "V" + (id + 1));
                int w = g2.getFontMetrics().stringWidth(label);
                g2.drawString(label, (int) p.x - w / 2, (int) p.y + 5);
            }

            g2.dispose();
        }

        private Map<Integer, Point2D.Double> projectedLayout(ResidualGraphSnapshot snapshot, int width, int height) {
            Map<Integer, Point2D.Double> base = snapshot.getVertexPositions();
            if (base.isEmpty()) {
                Map<Integer, Point2D.Double> fallback = new LinkedHashMap<>();
                int i = 0;
                int n = Math.max(1, snapshot.getVertexIds().size());
                for (Integer id : snapshot.getVertexIds()) {
                    double angle = (2 * Math.PI * i++) / n;
                    fallback.put(id, new Point2D.Double(450 + 220 * Math.cos(angle), 260 + 220 * Math.sin(angle)));
                }
                base = fallback;
            }

            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            for (Point2D.Double p : base.values()) {
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
            }

            double graphW = Math.max(1.0, maxX - minX);
            double graphH = Math.max(1.0, maxY - minY);
            double margin = 50.0;
            double fitScale = Math.min((width - 2 * margin) / graphW, (height - 2 * margin) / graphH);
            fitScale = Math.max(0.1, fitScale);
            double scale = fitScale * zoom;

            double offsetX = (width - graphW * fitScale) * 0.5 + panX;
            double offsetY = (height - graphH * fitScale) * 0.5 + panY;

            Map<Integer, Point2D.Double> projected = new LinkedHashMap<>();
            for (Map.Entry<Integer, Point2D.Double> e : base.entrySet()) {
                Point2D.Double p = e.getValue();
                projected.put(e.getKey(), new Point2D.Double(
                        offsetX + (p.x - minX) * scale,
                        offsetY + (p.y - minY) * scale
                ));
            }
            return projected;
        }

        private static void drawEdge(Graphics2D g2, Point2D.Double s, Point2D.Double d, int capacity, boolean dual, boolean primaryDirection, boolean highlighted) {
            Color color = highlighted ? new Color(255, 112, 67) : new Color(90, 100, 120);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(highlighted ? 3.0f : 1.6f));

            double dx = d.x - s.x;
            double dy = d.y - s.y;
            double len = Math.hypot(dx, dy);
            if (len < 1e-6) {
                return;
            }

            double ux = dx / len;
            double uy = dy / len;
            double nx = -uy;
            double ny = ux;
            // For opposite-direction edges the line normal already flips with edge direction,
            // so using the same positive magnitude places the pair on opposite sides.
            double anchorOffset = dual ? 10.0 : 0.0;

            Point2D.Double shiftedSource = new Point2D.Double(s.x + nx * anchorOffset, s.y + ny * anchorOffset);
            Point2D.Double shiftedDestination = new Point2D.Double(d.x + nx * anchorOffset, d.y + ny * anchorOffset);

            Point2D.Double from = new Point2D.Double(
                    shiftedSource.x + ux * VERTEX_RADIUS,
                    shiftedSource.y + uy * VERTEX_RADIUS
            );
            Point2D.Double to = new Point2D.Double(
                    shiftedDestination.x - ux * VERTEX_RADIUS,
                    shiftedDestination.y - uy * VERTEX_RADIUS
            );

            double lineDx = to.x - from.x;
            double lineDy = to.y - from.y;
            double lineLen = Math.hypot(lineDx, lineDy);
            if (lineLen < 1e-6) {
                return;
            }

            double midX = (from.x + to.x) * 0.5;
            double midY = (from.y + to.y) * 0.5;
            String text = String.valueOf(capacity);
            int textWidth = g2.getFontMetrics().stringWidth(text);
            double gapHalf = Math.max(12.0, textWidth * 0.5 + 8.0);

            Point2D.Double leftEnd = new Point2D.Double(
                    midX - (lineDx / lineLen) * gapHalf,
                    midY - (lineDy / lineLen) * gapHalf
            );
            Point2D.Double rightStart = new Point2D.Double(
                    midX + (lineDx / lineLen) * gapHalf,
                    midY + (lineDy / lineLen) * gapHalf
            );

            g2.draw(new Line2D.Double(from, leftEnd));
            g2.draw(new Line2D.Double(rightStart, to));
            drawArrow(g2, to.x, to.y, lineDx, lineDy, highlighted ? 12 : 10);

            g2.setColor(new Color(255, 255, 255, 220));
            g2.fillRoundRect((int) midX - textWidth / 2 - 5, (int) midY - 12, textWidth + 10, 16, 8, 8);
            g2.setColor(new Color(30, 30, 30));
            g2.drawString(text, (float) (midX - textWidth / 2.0), (float) midY);
        }

        private static void drawArrow(Graphics2D g2, double x, double y, double tx, double ty, double len) {
            double angle = Math.atan2(ty, tx);
            double phi = Math.toRadians(30);
            double x2 = x - len * Math.cos(angle - phi);
            double y2 = y - len * Math.sin(angle - phi);
            double x3 = x - len * Math.cos(angle + phi);
            double y3 = y - len * Math.sin(angle + phi);
            g2.draw(new Line2D.Double(x, y, x2, y2));
            g2.draw(new Line2D.Double(x, y, x3, y3));
        }
    }
}
