/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class MultiSelectPopup {
    private final JWindow window;
    private final JPanel mainPanel;
    private final JTextField filterField;
    private final JPanel checkBoxPanel;
    private final List<JCheckBox> checkBoxes = new ArrayList<>();
    private final JButton applyButton;
    private final JButton cancelButton;

    private Runnable applyAction = () -> {};
    private Runnable cancelAction = () -> {};

    public MultiSelectPopup(Window owner) {
    this(owner, new ArrayList<>());
}
    
    
    public MultiSelectPopup(Window owner, List<String> options) {
        window = new JWindow(owner);
        mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        mainPanel.setBackground(Color.WHITE);

        // Top panel con filtro e seleziona/deseleziona
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        filterField = new JTextField();
        filterField.setPreferredSize(new Dimension(150, 25));
        topPanel.add(filterField, BorderLayout.CENTER);

        JPanel selectButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        JButton selectAllButton = new JButton("Seleziona tutto");
        JButton deselectAllButton = new JButton("Deseleziona tutto");
        selectAllButton.setMargin(new Insets(2, 5, 2, 5));
        deselectAllButton.setMargin(new Insets(2, 5, 2, 5));
        selectButtonsPanel.add(selectAllButton);
        selectButtonsPanel.add(deselectAllButton);

        JPanel topContainer = new JPanel(new BorderLayout(2, 2));
        topContainer.add(topPanel, BorderLayout.NORTH);
        topContainer.add(selectButtonsPanel, BorderLayout.SOUTH);

        mainPanel.add(topContainer, BorderLayout.NORTH);

        // Checkbox panel scrollabile
        checkBoxPanel = new JPanel();
        checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(checkBoxPanel);
        scrollPane.setPreferredSize(new Dimension(200, 150));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottoni
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        applyButton = new JButton("Applica");
        cancelButton = new JButton("Annulla");
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        window.add(mainPanel);
        window.pack();

        // Popola checkbox
        for (String option : options) {
            JCheckBox checkBox = new JCheckBox(option);
            checkBoxPanel.add(checkBox);
            checkBoxes.add(checkBox);
        }

        // Listeners
        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });

        selectAllButton.addActionListener(e -> {
            for (JCheckBox cb : checkBoxes) {
                cb.setSelected(true);
            }
        });

        deselectAllButton.addActionListener(e -> {
            for (JCheckBox cb : checkBoxes) {
                cb.setSelected(false);
            }
        });

        applyButton.addActionListener(e -> applyAction.run());
        cancelButton.addActionListener(e -> {
            cancelAction.run();
            hide();
        });

        // Chiudi popup su ESC
        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hide();
                }
            }
        });

        window.setFocusableWindowState(true);
    }
    
  /*  public void updateOptions(List<String> options) {
    checkBoxPanel.removeAll();
    checkBoxes.clear();

    for (String option : options) {
        JCheckBox checkBox = new JCheckBox(option);
        checkBoxPanel.add(checkBox);
        checkBoxes.add(checkBox);
    }

    checkBoxPanel.revalidate();
    checkBoxPanel.repaint();
}*/
public void updateOptions(List<String> options, List<String> preSelected) {
    checkBoxes.clear();
    checkBoxPanel.removeAll();

    List<String> selected = new ArrayList<>();
    List<String> unselected = new ArrayList<>();

    for (String opt : options) {
        if (preSelected.contains(opt)) {
            selected.add(opt);
        } else {
            unselected.add(opt);
        }
    }

    selected.sort(String::compareToIgnoreCase);
    unselected.sort(String::compareToIgnoreCase);

    List<String> ordered = new ArrayList<>();
    ordered.addAll(selected);
    ordered.addAll(unselected);

    for (String opt : ordered) {
        JCheckBox cb = new JCheckBox(opt, preSelected.contains(opt));
        checkBoxes.add(cb);
        checkBoxPanel.add(cb);
    }

    checkBoxPanel.revalidate();
    checkBoxPanel.repaint();
}
    private void filter() {
        String text = filterField.getText().toLowerCase();
        for (JCheckBox cb : checkBoxes) {
            cb.setVisible(cb.getText().toLowerCase().contains(text));
        }
        checkBoxPanel.revalidate();
        checkBoxPanel.repaint();
    }
    
    public boolean isVisible() {
    return window.isVisible();
}

    
    public List<JCheckBox> getCheckBoxes() {
    return checkBoxes;
}

    public List<String> getSelectedOptions() {
        List<String> selected = new ArrayList<>();
        for (JCheckBox cb : checkBoxes) {
            if (cb.isSelected()) {
                selected.add(cb.getText());
            }
        }
        return selected;
    }

    public void setApplyAction(Runnable action) {
        this.applyAction = action;
    }

    public void setCancelAction(Runnable action) {
        this.cancelAction = action;
    }

    public void show(Component invoker) {
    if (window.isVisible()) {
        return; // Già visibile, non fare nulla
    }
    Point p = invoker.getLocationOnScreen();
    window.setLocation(p.x, p.y + invoker.getHeight());
    window.pack();
    window.setVisible(true);
    filterField.requestFocusInWindow();
}

    public void showAt(int x, int y) {
    window.setLocation(x, y);
    window.setVisible(true);
    filterField.requestFocusInWindow();
}
    
    public void hide() {
        window.setVisible(false);
    }
}



