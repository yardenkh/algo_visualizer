package ui;

import algorithm.AlgorithmPlaybackController;
import algorithm.AlgorithmStep;
import algorithm.EdgeKey;
import graph.Edge;
import graph.Vertex;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Interactive canvas for drawing and editing the graph.
 */
public class GraphCanvasPanel extends JPanel {
    private static final int VERTEX_RADIUS = 18;
    private static final int HIT_RADIUS_PX = 12;
    private static final double AUTO_BEND_OFFSET = 28.0;
    private static final double HANDLE_RADIUS_WORLD = 7.0;

    private final GraphEditorController controller;
    private final AlgorithmPlaybackController playbackController;
    private EditorTool activeTool;

    private double zoom;
    private final Point2D.Double pan;

    private Point lastMousePoint;
    private Point2D.Double lastMouseWorld;
    private boolean draggingVertices;
    private boolean panning;
    private boolean draggingEdgeBend;
    private Edge bendingEdge;

    private Rectangle selectionRect;
    private Vertex pendingConnectSource;

    private int pendingEdgeWeight;
    private int pendingEdgeColor;

    public GraphCanvasPanel(GraphEditorController controller, AlgorithmPlaybackController playbackController) {
        this.controller = Objects.requireNonNull(controller, "controller cannot be null");
        this.playbackController = Objects.requireNonNull(playbackController, "playbackController cannot be null");
        this.activeTool = EditorTool.SELECT;
        this.zoom = 1.0;
        this.pan = new Point2D.Double(100, 100);
        this.pendingEdgeWeight = 1;
        this.pendingEdgeColor = Color.DARK_GRAY.getRGB();

        setFocusable(true);
        setBackground(new Color(245, 247, 250));

        controller.addChangeListener(this::repaint);
        playbackController.addChangeListener(this::repaint);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                handleMousePressed(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                handleZoom(e);
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        addMouseWheelListener(mouseAdapter);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    controller.deleteSelection();
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    pendingConnectSource = null;
                    controller.clearSelection();
                }
                if (e.getKeyCode() == KeyEvent.VK_A && e.isControlDown()) {
                    selectAllVertices();
                }
            }
        });
    }

    /**
     * Activates a new editor tool and clears transient UI state related to the previous tool.
     */
    public void setActiveTool(EditorTool tool) {
        this.activeTool = tool;
        this.pendingConnectSource = null;
        this.selectionRect = null;
        setCursor(tool == EditorTool.PAN ? Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR) : Cursor.getDefaultCursor());
        repaint();
    }

    public EditorTool getActiveTool() {
        return activeTool;
    }

    public void setPendingEdgeWeight(int pendingEdgeWeight) {
        this.pendingEdgeWeight = pendingEdgeWeight;
    }

    public void setPendingEdgeColor(int pendingEdgeColor) {
        this.pendingEdgeColor = pendingEdgeColor;
    }

    /** Restores default zoom/pan viewport transform. */
    public void resetView() {
        zoom = 1.0;
        pan.x = 100;
        pan.y = 100;
        repaint();
    }

    /**
     * Paint pipeline: grid -> world-transformed graph -> screen-space overlays.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        paintGrid(g2);

        AffineTransform previous = g2.getTransform();
        g2.translate(pan.x, pan.y);
        g2.scale(zoom, zoom);

        AlgorithmStep currentStep = playbackController.getCurrentStep();
        paintEdges(g2, currentStep);
        paintVertices(g2, currentStep);

        g2.setTransform(previous);

        if (selectionRect != null) {
            g2.setColor(new Color(66, 133, 244, 45));
            g2.fill(selectionRect);
            g2.setColor(new Color(66, 133, 244));
            g2.draw(selectionRect);
        }

        g2.dispose();
    }

    private void paintGrid(Graphics2D g2) {
        g2.setColor(new Color(232, 236, 242));
        int step = 40;
        for (int x = (int) (pan.x % step); x < getWidth(); x += step) {
            g2.drawLine(x, 0, x, getHeight());
        }
        for (int y = (int) (pan.y % step); y < getHeight(); y += step) {
            g2.drawLine(0, y, getWidth(), y);
        }
    }

    // Draws all edges, including playback/selection emphasis and weight labels.
    private void paintEdges(Graphics2D g2, AlgorithmStep currentStep) {
        Map<EdgeKey, Integer> playbackColors = currentStep == null ? Map.of() : currentStep.getEdgeColors();
        Map<EdgeKey, String> edgeLabels = currentStep == null ? Map.of() : currentStep.getEdgeAnnotations();
        boolean undirectedPairedMode = controller.getEdgeCreationMode() == GraphEditorController.EdgeCreationMode.UNDIRECTED_PAIRED;
        Set<Long> renderedUndirectedPairs = undirectedPairedMode ? new HashSet<>() : Set.of();
        for (Edge edge : controller.getEdges()) {
            boolean hasReverse = edge.getSource().getId() != edge.getDestination().getId()
                    && controller.getGraph().containsEdge(edge.getDestination().getId(), edge.getSource().getId());
            if (undirectedPairedMode && hasReverse) {
                long pairKey = undirectedPairKey(edge);
                if (renderedUndirectedPairs.contains(pairKey)) {
                    continue;
                }
                renderedUndirectedPairs.add(pairKey);
            }

            EdgeGeometry geometry = edgeGeometry(edge);
            if (geometry == null) {
                continue;
            }

            Integer playbackColor = playbackColors.get(EdgeKey.from(edge));
            boolean selected = controller.isEdgeSelected(edge);
            Color edgeColor = playbackColor != null
                    ? new Color(playbackColor, false)
                    : selected
                    ? new Color(230, 81, 0)
                    : colorOrDefault(edge.getColor(), Color.DARK_GRAY);
            g2.setColor(edgeColor);
            g2.setStroke(new BasicStroke(playbackColor != null || selected ? 3f : 1.8f));

            g2.draw(geometry.curve);
            if (!(undirectedPairedMode && hasReverse)) {
                drawArrow(g2, geometry);
            }
            String label = edgeLabels.getOrDefault(EdgeKey.from(edge), String.valueOf(edge.getWeight()));
            drawEdgeLabel(g2, label, geometry);

            if (selected) {
                drawBendHandle(g2, geometry);
            }
        }
    }

    private static long undirectedPairKey(Edge edge) {
        int u = edge.getSource().getId();
        int v = edge.getDestination().getId();
        int a = Math.min(u, v);
        int b = Math.max(u, v);
        return (((long) a) << 32) | (b & 0xffffffffL);
    }

    // Draws vertices plus per-step annotation badges when playback is active.
    private void paintVertices(Graphics2D g2, AlgorithmStep currentStep) {
        Map<Integer, Integer> playbackColors = currentStep == null ? Map.of() : currentStep.getVertexColors();
        Map<Integer, String> annotations = currentStep == null ? Map.of() : currentStep.getVertexAnnotations();

        Font annotationFont = g2.getFont().deriveFont(Font.PLAIN, 11f);
        for (Vertex vertex : controller.getVertices()) {
            Point2D.Double point = controller.getPosition(vertex);
            if (point == null) {
                continue;
            }

            int x = (int) Math.round(point.x - VERTEX_RADIUS);
            int y = (int) Math.round(point.y - VERTEX_RADIUS);
            int diameter = VERTEX_RADIUS * 2;

            Integer playbackColor = playbackColors.get(vertex.getId());
            Color fill = playbackColor != null
                    ? new Color(playbackColor, false)
                    : colorOrDefault(vertex.getColor(), new Color(66, 133, 244));
            g2.setColor(fill);
            g2.fillOval(x, y, diameter, diameter);

            boolean selected = controller.isSelected(vertex) || Objects.equals(pendingConnectSource, vertex);
            g2.setColor(selected ? new Color(255, 112, 67) : new Color(28, 59, 101));
            g2.setStroke(new BasicStroke(selected ? 3f : 1.8f));
            g2.drawOval(x, y, diameter, diameter);

            String label = vertex.getName();
            g2.setColor(Color.WHITE);
            int textWidth = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, (float) (point.x - textWidth / 2.0), (float) (point.y + 5));

            String annotation = annotations.get(vertex.getId());
            if (annotation != null && !annotation.isBlank()) {
                drawVertexAnnotation(g2, annotationFont, annotation, point);
            }
        }
    }

    private void drawArrow(Graphics2D g2, EdgeGeometry geometry) {
        Point2D.Double destination = geometry.destination;
        Point2D.Double tangent = tangentAt(geometry, 0.98);
        double angle = Math.atan2(tangent.y, tangent.x);
        double arrowLength = 12;
        double phi = Math.toRadians(30);

        double x1 = destination.x - VERTEX_RADIUS * Math.cos(angle);
        double y1 = destination.y - VERTEX_RADIUS * Math.sin(angle);

        double x2 = x1 - arrowLength * Math.cos(angle - phi);
        double y2 = y1 - arrowLength * Math.sin(angle - phi);

        double x3 = x1 - arrowLength * Math.cos(angle + phi);
        double y3 = y1 - arrowLength * Math.sin(angle + phi);

        g2.draw(new Line2D.Double(x1, y1, x2, y2));
        g2.draw(new Line2D.Double(x1, y1, x3, y3));
    }

    private void drawEdgeLabel(Graphics2D g2, String text, EdgeGeometry geometry) {
        Point2D.Double p = pointAt(geometry, 0.5);
        double midX = p.x;
        double midY = p.y;

        int width = g2.getFontMetrics().stringWidth(text);
        g2.setColor(new Color(255, 255, 255, 200));
        g2.fillRoundRect((int) midX - width / 2 - 4, (int) midY - 12, width + 8, 16, 8, 8);
        g2.setColor(new Color(32, 32, 32));
        g2.drawString(text, (float) (midX - width / 2.0), (float) (midY));
    }

    private void drawBendHandle(Graphics2D g2, EdgeGeometry geometry) {
        Point2D.Double c = geometry.handleCenter;
        double r = HANDLE_RADIUS_WORLD;
        g2.setColor(new Color(255, 255, 255, 220));
        g2.fill(new java.awt.geom.Ellipse2D.Double(c.x - r, c.y - r, r * 2, r * 2));
        g2.setColor(new Color(230, 81, 0));
        g2.setStroke(new BasicStroke(1.4f));
        g2.draw(new java.awt.geom.Ellipse2D.Double(c.x - r, c.y - r, r * 2, r * 2));
    }

    private void drawVertexAnnotation(Graphics2D g2, Font font, String annotation, Point2D.Double point) {
        Font previousFont = g2.getFont();
        g2.setFont(font);
        int width = g2.getFontMetrics().stringWidth(annotation);
        int x = (int) Math.round(point.x - width / 2.0) - 5;
        int y = (int) Math.round(point.y + VERTEX_RADIUS + 6);
        g2.setColor(new Color(255, 255, 255, 220));
        g2.fillRoundRect(x, y, width + 10, 16, 8, 8);
        g2.setColor(new Color(60, 72, 88));
        g2.drawRoundRect(x, y, width + 10, 16, 8, 8);
        g2.drawString(annotation, x + 5, y + 12);
        g2.setFont(previousFont);
    }

    /**
     * Central mouse-press dispatcher.
     *
     * It resolves world coordinates and hit targets, then delegates behavior by active tool.
     */
    private void handleMousePressed(MouseEvent e) {
        lastMousePoint = e.getPoint();
        lastMouseWorld = toWorld(e.getPoint());

        if (SwingMouseUtil.isMiddleClick(e) || activeTool == EditorTool.PAN) {
            panning = true;
            return;
        }

        // Prioritize dragging the bend handle of the currently selected edge.
        if (activeTool == EditorTool.SELECT) {
            Edge selected = controller.getSelectedEdge();
            if (selected != null && isOnBendHandle(selected, lastMouseWorld)) {
                bendingEdge = selected;
                draggingEdgeBend = true;
                return;
            }
        }

        Vertex vertex = findVertexAt(e.getPoint());
        Edge edge = findEdgeAt(e.getPoint());

        if (activeTool == EditorTool.SELECT && edge != null && isOnBendHandle(edge, lastMouseWorld)) {
            controller.selectOnlyEdge(edge);
            bendingEdge = edge;
            draggingEdgeBend = true;
            return;
        }

        switch (activeTool) {
            case ADD_VERTEX -> controller.addVertexAt(lastMouseWorld.x, lastMouseWorld.y);
            case CONNECT -> handleConnectClick(vertex);
            case DELETE -> handleDeleteClick(vertex, edge);
            case SELECT -> handleSelectPress(e, vertex, edge);
            default -> {
            }
        }
    }

    /** Handles drag updates for panning, vertex dragging, or marquee selection. */
    private void handleMouseDragged(MouseEvent e) {
        if (lastMousePoint == null || lastMouseWorld == null) {
            return;
        }

        if (panning) {
            int dx = e.getX() - lastMousePoint.x;
            int dy = e.getY() - lastMousePoint.y;
            pan.x += dx;
            pan.y += dy;
            lastMousePoint = e.getPoint();
            repaint();
            return;
        }

        Point2D.Double nowWorld = toWorld(e.getPoint());

        if (draggingEdgeBend && bendingEdge != null) {
            EdgeGeometry geometry = edgeGeometry(bendingEdge);
            if (geometry != null) {
                Point2D.Double offset = new Point2D.Double(
                        nowWorld.x - geometry.midpoint.x,
                        nowWorld.y - geometry.midpoint.y
                );
                controller.setManualEdgeControlOffset(bendingEdge, offset, false);
            }
            return;
        }

        if (draggingVertices) {
            controller.moveSelectedVertices(nowWorld.x - lastMouseWorld.x, nowWorld.y - lastMouseWorld.y);
            lastMouseWorld = nowWorld;
            return;
        }

        if (activeTool == EditorTool.SELECT) {
            selectionRect = createRect(lastMousePoint, e.getPoint());
            repaint();
        }
    }

    /** Finalizes drag operation and commits marquee selection if present. */
    private void handleMouseReleased(MouseEvent e) {
        if (draggingEdgeBend && bendingEdge != null) {
            EdgeGeometry geometry = edgeGeometry(bendingEdge);
            if (geometry != null) {
                Point2D.Double world = toWorld(e.getPoint());
                Point2D.Double offset = new Point2D.Double(
                        world.x - geometry.midpoint.x,
                        world.y - geometry.midpoint.y
                );
                controller.setManualEdgeControlOffset(bendingEdge, offset, true);
            }
        }

        if (selectionRect != null && activeTool == EditorTool.SELECT) {
            Rectangle2D.Double worldRect = toWorld(selectionRect);
            controller.selectVerticesInRect(worldRect, e.isShiftDown());
        }

        draggingVertices = false;
        draggingEdgeBend = false;
        bendingEdge = null;
        panning = false;
        selectionRect = null;
        lastMousePoint = null;
        lastMouseWorld = null;
        repaint();
    }

    /**
     * Zooms around the cursor anchor point (keeps the pointed world coordinate stable).
     */
    private void handleZoom(MouseWheelEvent e) {
        double factor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
        factor = Math.max(0.25, Math.min(4.0, zoom * factor)) / zoom;

        Point p = e.getPoint();
        Point2D.Double worldBefore = toWorld(p);
        zoom *= factor;
        Point2D.Double worldAfter = toWorld(p);

        pan.x += (worldAfter.x - worldBefore.x) * zoom;
        pan.y += (worldAfter.y - worldBefore.y) * zoom;
        repaint();
    }

    /**
     * Connect-tool state machine:
     * first click picks source, second click creates directed edge.
     */
    private void handleConnectClick(Vertex vertex) {
        if (vertex == null) {
            pendingConnectSource = null;
            controller.clearSelection();
            return;
        }

        if (pendingConnectSource == null) {
            pendingConnectSource = vertex;
            controller.selectOnlyVertex(vertex);
            return;
        }

        controller.connect(pendingConnectSource, vertex, pendingEdgeWeight, pendingEdgeColor);
        pendingConnectSource = null;
    }

    /** Deletes whichever graph element is under the cursor, preferring vertices over edges. */
    private void handleDeleteClick(Vertex vertex, Edge edge) {
        if (vertex != null) {
            controller.removeVertex(vertex);
            return;
        }
        if (edge != null) {
            controller.removeEdge(edge);
        }
    }

    /**
     * Handles select-tool press behavior:
     * vertex selection/toggling, edge selection, or clearing selection on empty space.
     */
    private void handleSelectPress(MouseEvent e, Vertex vertex, Edge edge) {
        if (vertex != null) {
            if (e.isShiftDown()) {
                controller.toggleVertexSelection(vertex);
            } else if (!controller.isSelected(vertex)) {
                controller.selectOnlyVertex(vertex);
            }
            draggingVertices = true;
            return;
        }

        if (edge != null) {
            controller.selectOnlyEdge(edge);
            if (e.getClickCount() >= 2) {
                controller.clearManualEdgeBend(edge, true);
            }
            return;
        }

        if (!e.isShiftDown()) {
            controller.clearSelection();
        }
    }

    /**
     * Performs vertex hit-testing in world space.
     *
     * Iterates in reverse visual order so top-most/latest vertices are prioritized.
     */
    private Vertex findVertexAt(Point screenPoint) {
        Point2D.Double world = toWorld(screenPoint);
        List<Vertex> vertices = new ArrayList<>(controller.getVertices());
        for (int i = vertices.size() - 1; i >= 0; i--) {
            Vertex vertex = vertices.get(i);
            Point2D.Double point = controller.getPosition(vertex);
            if (point == null) {
                continue;
            }
            double distance = point.distance(world);
            if (distance <= VERTEX_RADIUS) {
                return vertex;
            }
        }
        return null;
    }

    /**
     * Performs nearest-segment edge hit-testing with zoom-aware threshold.
     */
    private Edge findEdgeAt(Point screenPoint) {
        Point2D.Double world = toWorld(screenPoint);
        double threshold = HIT_RADIUS_PX / zoom;
        Edge best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Edge edge : controller.getEdges()) {
            EdgeGeometry geometry = edgeGeometry(edge);
            if (geometry == null) {
                continue;
            }
            double distance = distanceToCurve(geometry, world);
            if (distance <= threshold && distance < bestDistance) {
                bestDistance = distance;
                best = edge;
            }
        }
        return best;
    }

    private EdgeGeometry edgeGeometry(Edge edge) {
        Point2D.Double source = controller.getPosition(edge.getSource());
        Point2D.Double destination = controller.getPosition(edge.getDestination());
        if (source == null || destination == null) {
            return null;
        }

        double dx = destination.x - source.x;
        double dy = destination.y - source.y;
        double length = Math.hypot(dx, dy);
        if (length < 0.001) {
            destination = new Point2D.Double(source.x + 0.001, source.y);
            dx = destination.x - source.x;
            dy = destination.y - source.y;
            length = Math.hypot(dx, dy);
        }

        Point2D.Double unit = new Point2D.Double(dx / length, dy / length);
        Point2D.Double normal = new Point2D.Double(-unit.y, unit.x);
        Point2D.Double midpoint = new Point2D.Double((source.x + destination.x) * 0.5, (source.y + destination.y) * 0.5);

        Point2D.Double manualOffset = controller.getManualEdgeControlOffset(edge);
        Point2D.Double bendOffset = manualOffset != null
                ? manualOffset
                : new Point2D.Double(normal.x * autoBend(edge), normal.y * autoBend(edge));
        Point2D.Double control = new Point2D.Double(
                midpoint.x + bendOffset.x,
                midpoint.y + bendOffset.y
        );

        QuadCurve2D.Double curve = new QuadCurve2D.Double(
                source.x,
                source.y,
                control.x,
                control.y,
                destination.x,
                destination.y
        );
        return new EdgeGeometry(source, destination, midpoint, normal, bendOffset, control, curve, control);
    }

    private double autoBend(Edge edge) {
        int sourceId = edge.getSource().getId();
        int destinationId = edge.getDestination().getId();
        if (sourceId == destinationId) {
            return AUTO_BEND_OFFSET;
        }
        // Default behavior is straight edges; opposite-direction pairs are not auto-bent.
        return 0.0;
    }

    private boolean isOnBendHandle(Edge edge, Point2D.Double worldPoint) {
        EdgeGeometry geometry = edgeGeometry(edge);
        if (geometry == null) {
            return false;
        }
        return geometry.handleCenter.distance(worldPoint) <= (HANDLE_RADIUS_WORLD + (3.0 / zoom));
    }

    private double distanceToCurve(EdgeGeometry geometry, Point2D.Double worldPoint) {
        double best = Double.MAX_VALUE;
        Point2D.Double prev = pointAt(geometry, 0.0);
        int samples = 28;
        for (int i = 1; i <= samples; i++) {
            double t = i / (double) samples;
            Point2D.Double current = pointAt(geometry, t);
            double dist = Line2D.ptSegDist(prev.x, prev.y, current.x, current.y, worldPoint.x, worldPoint.y);
            if (dist < best) {
                best = dist;
            }
            prev = current;
        }
        return best;
    }

    private static Point2D.Double pointAt(EdgeGeometry geometry, double t) {
        double omt = 1.0 - t;
        double x = omt * omt * geometry.source.x + 2.0 * omt * t * geometry.control.x + t * t * geometry.destination.x;
        double y = omt * omt * geometry.source.y + 2.0 * omt * t * geometry.control.y + t * t * geometry.destination.y;
        return new Point2D.Double(x, y);
    }

    private static Point2D.Double tangentAt(EdgeGeometry geometry, double t) {
        double x = 2.0 * (1.0 - t) * (geometry.control.x - geometry.source.x)
                + 2.0 * t * (geometry.destination.x - geometry.control.x);
        double y = 2.0 * (1.0 - t) * (geometry.control.y - geometry.source.y)
                + 2.0 * t * (geometry.destination.y - geometry.control.y);
        double len = Math.hypot(x, y);
        if (len < 1e-6) {
            return new Point2D.Double(1.0, 0.0);
        }
        return new Point2D.Double(x / len, y / len);
    }

    private record EdgeGeometry(
            Point2D.Double source,
            Point2D.Double destination,
            Point2D.Double midpoint,
            Point2D.Double normal,
            Point2D.Double bendOffset,
            Point2D.Double control,
            QuadCurve2D.Double curve,
            Point2D.Double handleCenter
    ) {
    }

    /** Converts a screen-space point into world-space coordinates. */
    private Point2D.Double toWorld(Point point) {
        return new Point2D.Double((point.x - pan.x) / zoom, (point.y - pan.y) / zoom);
    }

    /** Converts a screen-space rectangle into world-space selection bounds. */
    private Rectangle2D.Double toWorld(Rectangle rect) {
        Point2D.Double p1 = toWorld(new Point(rect.x, rect.y));
        Point2D.Double p2 = toWorld(new Point(rect.x + rect.width, rect.y + rect.height));
        return new Rectangle2D.Double(
                Math.min(p1.x, p2.x),
                Math.min(p1.y, p2.y),
                Math.abs(p2.x - p1.x),
                Math.abs(p2.y - p1.y)
        );
    }

    /** Utility for building normalized drag rectangles from two points. */
    private static Rectangle createRect(Point a, Point b) {
        return new Rectangle(
                Math.min(a.x, b.x),
                Math.min(a.y, b.y),
                Math.abs(a.x - b.x),
                Math.abs(a.y - b.y)
        );
    }

    /** Selects all vertices using a large world-space capture rectangle. */
    private void selectAllVertices() {
        controller.clearSelection();
        Rectangle2D.Double all = new Rectangle2D.Double(-100_000, -100_000, 200_000, 200_000);
        controller.selectVerticesInRect(all, true);
    }

    /** Normalizes optional int-based color values to usable Color objects. */
    private static Color colorOrDefault(int colorValue, Color fallback) {
        return colorValue == 0 ? fallback : new Color(colorValue, false);
    }

    /** Utility methods for mouse button checks. */
    private static final class SwingMouseUtil {
        private SwingMouseUtil() {
        }

        private static boolean isMiddleClick(MouseEvent e) {
            return e.getButton() == MouseEvent.BUTTON2;
        }
    }
}

