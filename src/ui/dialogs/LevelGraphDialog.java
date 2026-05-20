package ui.dialogs;

import algorithm.EdgeKey;
import algorithm.LevelGraphSnapshot;

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
import java.awt.geom.QuadCurve2D;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Modeless level-graph window used by Dinic playback. */
public class LevelGraphDialog extends JDialog {
    private static final int VERTEX_RADIUS = 20;
    private final LevelPanel panel;

    public LevelGraphDialog(Frame owner) {
        super(owner, "Level Graph", false);
        setLayout(new BorderLayout());
        panel = new LevelPanel();
        add(new JScrollPane(panel), BorderLayout.CENTER);
        setSize(980, 600);
        setLocationByPlatform(true);
    }

    public void showSnapshot(String algorithmName, LevelGraphSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        setTitle("Level Graph - " + algorithmName);
        panel.setSnapshot(snapshot);
    }

    /**
     * Returns a lightweight embeddable panel containing the level-graph canvas.
     * The panel shares the same {@link LevelPanel} rendering component.
     */
    public JPanel createEmbeddablePanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(new JScrollPane(panel), BorderLayout.CENTER);
        return wrapper;
    }

    private static final class LevelPanel extends JPanel {
        private LevelGraphSnapshot snapshot;
        private double zoom = 1.0;
        private double panX;
        private double panY;
        private Point lastDrag;
        private boolean panning;

        private LevelPanel() {
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

        private void setSnapshot(LevelGraphSnapshot snapshot) {
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
            Set<EdgeKey> edgeSet = new LinkedHashSet<>(snapshot.getLevelEdges());

            g2.setStroke(new BasicStroke(2.0f));
            for (EdgeKey edge : snapshot.getLevelEdges()) {
                Point2D.Double s = points.get(edge.getSourceId());
                Point2D.Double d = points.get(edge.getDestinationId());
                if (s == null || d == null) {
                    continue;
                }
                boolean dual = edgeSet.contains(new EdgeKey(edge.getDestinationId(), edge.getSourceId()));
                drawEdge(g2, s, d, dual, edge.getSourceId() < edge.getDestinationId());
            }

            for (Integer id : snapshot.getVertexIds()) {
                Point2D.Double p = points.get(id);
                if (p == null) {
                    continue;
                }
                Integer level = snapshot.getLevels().get(id);
                g2.setColor(level == null ? new Color(120, 120, 120) : new Color(76, 129, 190));
                g2.fillOval((int) p.x - VERTEX_RADIUS, (int) p.y - VERTEX_RADIUS, VERTEX_RADIUS * 2, VERTEX_RADIUS * 2);
                g2.setColor(Color.WHITE);
                String label = snapshot.getVertexLabels().getOrDefault(id, "V" + (id + 1));
                int w = g2.getFontMetrics().stringWidth(label);
                g2.drawString(label, (int) p.x - w / 2, (int) p.y + 3);
                if (level != null) {
                    String lv = "L=" + level;
                    int lw = g2.getFontMetrics().stringWidth(lv);
                    g2.setColor(new Color(30, 30, 30));
                    g2.drawString(lv, (int) p.x - lw / 2, (int) p.y + 34);
                }
            }

            g2.dispose();
        }

        private Map<Integer, Point2D.Double> projectedLayout(LevelGraphSnapshot snapshot, int width, int height) {
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

        private static void drawEdge(Graphics2D g2, Point2D.Double s, Point2D.Double d, boolean dual, boolean primaryDirection) {
            g2.setColor(new Color(58, 120, 58));
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
            double bend = dual ? (primaryDirection ? 22.0 : -22.0) : 0.0;

            Point2D.Double shiftedSource = new Point2D.Double(s.x + nx * anchorOffset, s.y + ny * anchorOffset);
            Point2D.Double shiftedDestination = new Point2D.Double(d.x + nx * anchorOffset, d.y + ny * anchorOffset);

            double sdx = shiftedDestination.x - shiftedSource.x;
            double sdy = shiftedDestination.y - shiftedSource.y;
            double slen = Math.hypot(sdx, sdy);
            if (slen < 1e-6) {
                return;
            }

            Point2D.Double from = new Point2D.Double(
                    shiftedSource.x + sdx * VERTEX_RADIUS / slen,
                    shiftedSource.y + sdy * VERTEX_RADIUS / slen
            );
            Point2D.Double to = new Point2D.Double(
                    shiftedDestination.x - sdx * VERTEX_RADIUS / slen,
                    shiftedDestination.y - sdy * VERTEX_RADIUS / slen
            );
            Point2D.Double ctrl = new Point2D.Double((from.x + to.x) * 0.5 + nx * bend, (from.y + to.y) * 0.5 + ny * bend);

            double t1 = 0.46;
            double t2 = 0.54;
            Point2D.Double p1 = quadPoint(from, ctrl, to, t1);
            Point2D.Double p2 = quadPoint(from, ctrl, to, t2);
            Point2D.Double c1 = quadSplitControl(from, ctrl, t1);
            Point2D.Double c2 = quadSplitControl(to, ctrl, 1.0 - t2);

            g2.draw(new QuadCurve2D.Double(from.x, from.y, c1.x, c1.y, p1.x, p1.y));
            g2.draw(new QuadCurve2D.Double(p2.x, p2.y, c2.x, c2.y, to.x, to.y));

            double mx = 0.25 * from.x + 0.5 * ctrl.x + 0.25 * to.x;
            double my = 0.25 * from.y + 0.5 * ctrl.y + 0.25 * to.y;
            g2.setColor(new Color(255, 255, 255));
            g2.fillRoundRect((int) mx - 8, (int) my - 8, 16, 12, 6, 6);
            g2.setColor(new Color(58, 120, 58));

            double tx = to.x - ctrl.x;
            double ty = to.y - ctrl.y;
            drawArrow(g2, to.x, to.y, tx, ty);
        }

        private static void drawArrow(Graphics2D g2, double x, double y, double tx, double ty) {
            double len = 11.0;
            double angle = Math.atan2(ty, tx);
            double phi = Math.toRadians(30);
            double x2 = x - len * Math.cos(angle - phi);
            double y2 = y - len * Math.sin(angle - phi);
            double x3 = x - len * Math.cos(angle + phi);
            double y3 = y - len * Math.sin(angle + phi);
            g2.draw(new Line2D.Double(x, y, x2, y2));
            g2.draw(new Line2D.Double(x, y, x3, y3));
        }

        private static Point2D.Double quadPoint(Point2D.Double p0, Point2D.Double p1, Point2D.Double p2, double t) {
            double mt = 1.0 - t;
            return new Point2D.Double(
                    mt * mt * p0.x + 2 * mt * t * p1.x + t * t * p2.x,
                    mt * mt * p0.y + 2 * mt * t * p1.y + t * t * p2.y
            );
        }

        private static Point2D.Double quadSplitControl(Point2D.Double start, Point2D.Double ctrl, double t) {
            return new Point2D.Double(
                    start.x + (ctrl.x - start.x) * t,
                    start.y + (ctrl.y - start.y) * t
            );
        }
    }
}
