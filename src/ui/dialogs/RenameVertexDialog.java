package ui.dialogs;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Modal dialog used to rename selected vertices.
 */
public final class RenameVertexDialog extends JDialog {
    private final JTextField nameField;
    private final JCheckBox appendCheckBox;
    private boolean confirmed;

    public RenameVertexDialog(JFrame owner) {
        super(owner, "Change Vertex Name", true);

        nameField = new JTextField(24);
        appendCheckBox = new JCheckBox("Append");
        JButton changeNameButton = new JButton("Change Name");
        JButton cancelButton = new JButton("Cancel");

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(4, 4, 4, 4);
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Enter new name"), gbc);
        gbc.gridx = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        form.add(nameField, gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = java.awt.GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        form.add(appendCheckBox, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(cancelButton);
        buttons.add(changeNameButton);

        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        setMinimumSize(new Dimension(360, 160));
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(owner);

        changeNameButton.addActionListener(this::confirmAndClose);
        cancelButton.addActionListener(e -> closeWithoutChanges());
        nameField.addActionListener(this::confirmAndClose);

        getRootPane().setDefaultButton(changeNameButton);
        bindEscapeToCancel();
    }

    public void open() {
        confirmed = false;
        nameField.setText("");
        appendCheckBox.setSelected(false);
        setVisible(true);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getEnteredName() {
        return nameField.getText();
    }

    public boolean isAppendSelected() {
        return appendCheckBox.isSelected();
    }

    private void confirmAndClose(ActionEvent event) {
        confirmed = true;
        setVisible(false);
    }

    private void closeWithoutChanges() {
        confirmed = false;
        setVisible(false);
    }

    private void bindEscapeToCancel() {
        javax.swing.KeyStroke escape = javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, "cancel");
        getRootPane().getActionMap().put("cancel", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeWithoutChanges();
            }
        });
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            confirmed = false;
        }
        super.setVisible(b);
    }
}


