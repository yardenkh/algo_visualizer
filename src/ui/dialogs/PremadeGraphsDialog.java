package ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import ui.templates.GraphTemplate;
import ui.templates.GraphTemplateLibrary;
import ui.templates.GraphTemplatePreviewPanel;
import ui.templates.GraphTemplateType;

/**
 * Popup graph library with type filters and selectable thumbnail cards.
 * This dialog fronts the premade-template system built in {@link ui.templates.GraphTemplateLibrary}
 * and is used by {@link ui.GraphEditorFrame} during the premade-graph loading flow.
 */
public class PremadeGraphsDialog extends JDialog {
    private GraphTemplate selectedTemplate;
    private final JComboBox<GraphTemplateType> typeCombo;
    private final JPanel cardsPanel;
    private final JLabel countLabel;

    /** Builds the library UI, filter controls, and selection footer. */
    public PremadeGraphsDialog(Frame owner) {
        super(owner, "Premade Graphs", true);
        setLayout(new BorderLayout(10, 10));

        typeCombo = new JComboBox<>(GraphTemplateType.values());
        cardsPanel = new JPanel(new GridLayout(0, 4, 10, 10));
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        countLabel = new JLabel();

        JPanel top = new JPanel(new BorderLayout(8, 8));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Type:"));
        controls.add(typeCombo);
        top.add(controls, BorderLayout.WEST);
        top.add(countLabel, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> {
            selectedTemplate = null;
            dispose();
        });

        JButton load = new JButton("Load Selected Graph");
        load.addActionListener(e -> dispose());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(cancel);
        bottom.add(load);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        typeCombo.addActionListener(e -> rebuildCards());
        typeCombo.setSelectedItem(GraphTemplateType.ALL_GRAPHS);
        rebuildCards();

        setSize(1080, 700);
        setMinimumSize(new Dimension(900, 520));
        setLocationRelativeTo(owner);
    }

    /** @return currently selected template, or null when canceled/nothing selected. */
    public GraphTemplate getSelectedTemplate() {
        return selectedTemplate;
    }

    /**
     * Rebuilds the thumbnail grid when the selected graph type changes.
     */
    private void rebuildCards() {
        cardsPanel.removeAll();
        GraphTemplateType type = (GraphTemplateType) typeCombo.getSelectedItem();
        List<GraphTemplate> templates = new ArrayList<>(GraphTemplateLibrary.byType(type));
        countLabel.setText(templates.size() + " graphs");

        ButtonGroup group = new ButtonGroup();
        for (GraphTemplate template : templates) {
            JToggleButton card = createCard(template);
            group.add(card);
            cardsPanel.add(card);
        }

        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    /** Creates one clickable thumbnail card for a single template. */
    private JToggleButton createCard(GraphTemplate template) {
        JToggleButton button = new JToggleButton();
        button.setLayout(new BorderLayout(4, 4));
        button.setPreferredSize(new Dimension(220, 155));
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createLineBorder(new Color(200, 207, 220), 1));

        JLabel name = new JLabel(template.getName(), JLabel.CENTER);
        name.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));

        GraphTemplatePreviewPanel preview = new GraphTemplatePreviewPanel();
        preview.setTemplate(template);
        preview.setPreferredSize(new Dimension(180, 100));

        JLabel type = new JLabel(template.getType().toString(), JLabel.CENTER);
        type.setForeground(new Color(95, 105, 125));

        button.add(name, BorderLayout.NORTH);
        button.add(preview, BorderLayout.CENTER);
        button.add(type, BorderLayout.SOUTH);

        button.addActionListener(e -> selectedTemplate = template);
        return button;
    }
}
