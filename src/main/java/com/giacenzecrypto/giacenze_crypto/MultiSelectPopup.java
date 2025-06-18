/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Comparator;
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
    
    private boolean isDraggingSelection = false;
private boolean dragSelectionEnabled = false;
private boolean dragSelectState = false; 
private JCheckBox lastHighlighted = null;
private final List<JCheckBox> currentlyHighlighted = new ArrayList<>();

    public MultiSelectPopup(Window owner) {
    this(owner, new ArrayList<>());
}
    
    
    public MultiSelectPopup(Window owner, List<String> options) {
        
        // popup.enableDragSelection();
        window = new JWindow(owner);
        mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        //mainPanel.setBackground(Color.WHITE);

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
    
    
public void updateOptions(List<String> allOptions, List<String> selectedOptions) {
    checkBoxes.clear();
    checkBoxPanel.removeAll();

    List<String> selected = new ArrayList<>();
    List<String> unselected = new ArrayList<>();

    for (String opt : allOptions) {
        if (selectedOptions.contains(opt)) {
            selected.add(opt);
        } else {
            unselected.add(opt);
        }
    }

    Comparator<String> numericAwareComparator = (s1, s2) -> {
        try {
            Double n1 = Double.valueOf(s1);
            Double n2 = Double.valueOf(s2);
            return Double.compare(n1, n2);
        } catch (NumberFormatException e1) {
            try {
                Double.valueOf(s1);
                return -1; // s1 è numero, s2 no → s1 prima
            } catch (NumberFormatException e2) {
                try {
                    Double.valueOf(s2);
                    return 1; // s2 è numero, s1 no → s2 prima
                } catch (NumberFormatException e3) {
                    return s1.compareToIgnoreCase(s2); // entrambi stringhe
                }
            }
        }
    };

    selected.sort(numericAwareComparator);
    unselected.sort(numericAwareComparator);

    List<String> combined = new ArrayList<>();
    combined.addAll(selected);
    combined.addAll(unselected);


   for (String opt : combined) {
    JCheckBox cb = new JCheckBox(opt, selected.contains(opt));
    checkBoxes.add(cb);
    checkBoxPanel.add(cb);
}

checkBoxPanel.revalidate();
checkBoxPanel.repaint();

// ATTENZIONE: Abilita la selezione dopo che i nuovi checkbox sono visibili
SwingUtilities.invokeLater(() -> enableDragSelection(checkBoxPanel));

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
    
    
    public Dimension getPreferredSize() {
    window.pack();  // Assicura che le dimensioni siano aggiornate
    return window.getSize();
}
    
   


private void enableDragSelection(JPanel panel) {
    if (dragSelectionEnabled) return;
    dragSelectionEnabled = true;

    panel.addMouseListener(new MouseAdapter() {
        @Override
public void mousePressed(MouseEvent e) {
    if (!SwingUtilities.isLeftMouseButton(e)) return;

    JCheckBox cb = getCheckBoxAtPoint(panel, e.getPoint());
    if (cb != null) {
        clearHighlight();  // pulisce il gruppo precedente!
        isDraggingSelection = true;
        dragSelectState = !cb.isSelected();
        applySelection(cb);
        lastHighlighted = cb;
    }
}

        @Override
        public void mouseReleased(MouseEvent e) {
            isDraggingSelection = false;
            lastHighlighted = null;
        }
    });

    panel.addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
            if (!isDraggingSelection) return;

            JCheckBox cb = getCheckBoxAtPoint(panel, e.getPoint());
            if (cb != null && cb != lastHighlighted) {
                applySelection(cb);
                lastHighlighted = cb;
            }
        }
    });
}





private void applySelection(JCheckBox cb) {
    cb.setSelected(dragSelectState);
    highlightCheckBox(cb, true);
    if (!currentlyHighlighted.contains(cb)) {
        currentlyHighlighted.add(cb);
    }
}




private JCheckBox getCheckBoxAtPoint(JPanel panel, Point p) {
    for (Component comp : panel.getComponents()) {
        if (comp instanceof JCheckBox cb) {
            Point localPoint = SwingUtilities.convertPoint(panel, p, cb);
            localPoint.x=0;
            if (cb.contains(localPoint)) {
                return cb;
            }
        }
    }
    return null;
}


private void clearHighlight() {
    for (JCheckBox cb : currentlyHighlighted) {
        highlightCheckBox(cb, false);
    }
    currentlyHighlighted.clear();
}


private void highlightCheckBox(JCheckBox cb, boolean highlight) {
    if (highlight) {
        cb.setBackground(new Color(200, 220, 255));
        cb.setOpaque(true);
    } else {
        cb.setBackground(null);
        cb.setOpaque(false);
    }
    cb.repaint();
}

    
}



