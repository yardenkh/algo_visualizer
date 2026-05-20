package ui.templates;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import javax.swing.JPanel;

/**
 * Tiny thumbnail preview of a premade graph template.
 *
 * Used inside {@link ui.dialogs.PremadeGraphsDialog} cards so users can visually scan
 * the library before loading a graph into the editor.
 */
public class GraphTemplatePreviewPanel extends JPanel {
    private GraphTemplate template;

    /** Initializes the small preview panel with a neutral background. */
    public GraphTemplatePreviewPanel() {
        setBackground(new Color(250, 252, 255));
    }

    /** Replaces the currently previewed template and triggers repaint. */
    public void setTemplate(GraphTemplate template) {
        this.template = template;
        repaint();
    }

    /** Paints a simplified miniature of the current template. */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (template == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int pad = 8;

        g2.setColor(new Color(208, 216, 230));
        g2.drawRect(1, 1, w - 3, h - 3);

        g2.setStroke(new BasicStroke(1.1f));
        for (GraphTemplate.TemplateEdge edge : template.getEdges()) {
            Point2D.Double s = template.getPositions().get(edge.getSourceId());
            Point2D.Double d = template.getPositions().get(edge.getDestinationId());
            if (s == null || d == null) {
                continue;
            }
            double x1 = pad + s.x * (w - 2.0 * pad);
            double y1 = pad + s.y * (h - 2.0 * pad);
            double x2 = pad + d.x * (w - 2.0 * pad);
            double y2 = pad + d.y * (h - 2.0 * pad);
            g2.setColor(new Color(125, 139, 167));
            g2.draw(new Line2D.Double(x1, y1, x2, y2));
        }

        g2.setColor(new Color(48, 96, 178));
        for (Point2D.Double p : template.getPositions().values()) {
            int x = (int) Math.round(pad + p.x * (w - 2.0 * pad));
            int y = (int) Math.round(pad + p.y * (h - 2.0 * pad));
            g2.fillOval(x - 3, y - 3, 6, 6);
        }

        g2.dispose();
    }
}
