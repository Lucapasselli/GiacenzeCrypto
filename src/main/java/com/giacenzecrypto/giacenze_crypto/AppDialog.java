/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class AppDialog extends JDialog {

    public enum ThemeMode {
        LIGHT, DARK
    }

    public enum DialogType {
        INFO, WARNING, ERROR, SUCCESS
    }

    public enum ActionRole {
        PRIMARY, SECONDARY, DANGER, NEUTRAL
    }

    public enum CloseReason {
        ACTION, WINDOW_CLOSE
    }

    public static final class DialogResult {
        private final String actionId;
        private final CloseReason closeReason;

        public DialogResult(String actionId, CloseReason closeReason) {
            this.actionId = actionId;
            this.closeReason = closeReason;
        }

        public String getActionId() {
            return actionId;
        }

        public CloseReason getCloseReason() {
            return closeReason;
        }

        public boolean isAction(String id) {
            return Objects.equals(this.actionId, id);
        }

        public boolean isClosedByWindow() {
            return closeReason == CloseReason.WINDOW_CLOSE;
        }
    }

    public static final class DialogAction {
        private final String id;
        private final String text;
        private final ActionRole role;
        private final boolean closeOnClick;
        private final Consumer<AppDialog> handler;

        private DialogAction(Builder builder) {
            this.id = builder.id;
            this.text = builder.text;
            this.role = builder.role;
            this.closeOnClick = builder.closeOnClick;
            this.handler = builder.handler;
        }

        public String getId() {
            return id;
        }

        public String getText() {
            return text;
        }

        public ActionRole getRole() {
            return role;
        }

        public boolean isCloseOnClick() {
            return closeOnClick;
        }

        public Consumer<AppDialog> getHandler() {
            return handler;
        }

        public static Builder builder(String id, String text) {
            return new Builder(id, text);
        }

        public static final class Builder {
            private final String id;
            private final String text;
            private ActionRole role = ActionRole.NEUTRAL;
            private boolean closeOnClick = true;
            private Consumer<AppDialog> handler;

            public Builder(String id, String text) {
                this.id = Objects.requireNonNull(id, "id");
                this.text = Objects.requireNonNull(text, "text");
            }

            public Builder role(ActionRole role) {
                this.role = Objects.requireNonNull(role);
                return this;
            }

            public Builder closeOnClick(boolean closeOnClick) {
                this.closeOnClick = closeOnClick;
                return this;
            }

            public Builder onClick(Consumer<AppDialog> handler) {
                this.handler = handler;
                return this;
            }

            public DialogAction build() {
                return new DialogAction(this);
            }
        }
    }

    public static final class UiTheme {
        public final Color background;
        public final Color surface;
        public final Color border;
        public final Color textPrimary;
        public final Color textSecondary;
        public final Color accent;
        public final Color warning;
        public final Color success;
        public final Color error;
        public final Color neutralButton;
        public final Color neutralButtonText;
        public final Color primaryButtonText;
        public final Color focusRing;
        public final Font titleFont;
        public final Font messageFont;
        public final Font detailsFont;
        public final Font buttonFont;

        private UiTheme(Color background,
                        Color surface,
                        Color border,
                        Color textPrimary,
                        Color textSecondary,
                        Color accent,
                        Color warning,
                        Color success,
                        Color error,
                        Color neutralButton,
                        Color neutralButtonText,
                        Color primaryButtonText,
                        Color focusRing,
                        Font titleFont,
                        Font messageFont,
                        Font detailsFont,
                        Font buttonFont) {
            this.background = background;
            this.surface = surface;
            this.border = border;
            this.textPrimary = textPrimary;
            this.textSecondary = textSecondary;
            this.accent = accent;
            this.warning = warning;
            this.success = success;
            this.error = error;
            this.neutralButton = neutralButton;
            this.neutralButtonText = neutralButtonText;
            this.primaryButtonText = primaryButtonText;
            this.focusRing = focusRing;
            this.titleFont = titleFont;
            this.messageFont = messageFont;
            this.detailsFont = detailsFont;
            this.buttonFont = buttonFont;
        }

        public static UiTheme of(ThemeMode mode) {
            Font title = new Font("SansSerif", Font.BOLD, 18);
            Font message = new Font("SansSerif", Font.PLAIN, 13);
            Font details = new Font("SansSerif", Font.PLAIN, 12);
            Font button = new Font("SansSerif", Font.BOLD, 13);

            if (mode == ThemeMode.DARK) {
                return new UiTheme(
                        new Color(28, 28, 28),
                        new Color(37, 37, 37),
                        new Color(62, 62, 62),
                        new Color(235, 235, 235),
                        new Color(170, 170, 170),
                        new Color(74, 144, 226),
                        new Color(214, 154, 43),
                        new Color(46, 160, 67),
                        new Color(211, 58, 44),
                        new Color(50, 50, 50),
                        new Color(235, 235, 235),
                        Color.WHITE,
                        new Color(140, 190, 255),
                        title, message, details, button
                );
            }

            return new UiTheme(
                    new Color(247, 247, 247),
                    Color.WHITE,
                    new Color(221, 221, 221),
                    new Color(34, 34, 34),
                    new Color(102, 102, 102),
                    new Color(52, 120, 246),
                    new Color(214, 154, 43),
                    new Color(46, 160, 67),
                    new Color(211, 58, 44),
                    new Color(245, 245, 245),
                    new Color(34, 34, 34),
                    Color.WHITE,
                    new Color(102, 163, 255),
                    title, message, details, button
            );
        }
    }

    public static final class Builder {
        private final Window owner;

        private String windowTitle = "Messaggio";
        private String bodyTitle = "Messaggio";
        private boolean showTitleInBody = true;

        private String message = "";
        private String details;

        private DialogType type = DialogType.INFO;
        private UiTheme theme = UiTheme.of(ThemeMode.LIGHT);

        private boolean modal = true;
        private boolean resizable = false;
        private int maxButtons = 8;
        private int minWidth = 540;

        private final List<DialogAction> actions = new ArrayList<>();

        private Builder(Window owner) {
            this.owner = owner;
        }

        public Builder title(String title) {
            this.windowTitle = title;
            this.bodyTitle = title;
            return this;
        }

        public Builder windowTitle(String windowTitle) {
            this.windowTitle = windowTitle;
            return this;
        }

        public Builder bodyTitle(String bodyTitle) {
            this.bodyTitle = bodyTitle;
            return this;
        }

        public Builder showTitleInBody(boolean showTitleInBody) {
            this.showTitleInBody = showTitleInBody;
            return this;
        }

        public Builder hideBodyTitle() {
            this.showTitleInBody = false;
            return this;
        }

        public Builder showBodyTitle() {
            this.showTitleInBody = true;
            return this;
        }

        public Builder message(String message) {
            this.message = message != null ? message : "";
            return this;
        }

        public Builder details(String details) {
            this.details = details;
            return this;
        }

        public Builder type(DialogType type) {
            this.type = type != null ? type : DialogType.INFO;
            return this;
        }

        public Builder theme() {
            this.theme = AppDialog.UiTheme.of(Principale.tema.equalsIgnoreCase("scuro")
                    ? AppDialog.ThemeMode.DARK
                    : AppDialog.ThemeMode.LIGHT);
            return this;
        }

        public Builder modal(boolean modal) {
            this.modal = modal;
            return this;
        }

        public Builder resizable(boolean resizable) {
            this.resizable = resizable;
            return this;
        }

        public Builder minWidth(int minWidth) {
            this.minWidth = Math.max(360, minWidth);
            return this;
        }

        public Builder maxButtons(int maxButtons) {
            this.maxButtons = Math.max(1, maxButtons);
            return this;
        }

        public Builder action(DialogAction action) {
            if (action == null) {
                return this;
            }

            if (actions.size() >= maxButtons) {
                throw new IllegalStateException("Numero massimo di pulsanti superato: " + maxButtons);
            }

            actions.add(action);
            return this;
        }

        public Builder primaryAction(String id, String text) {
            return action(DialogAction.builder(id, text)
                    .role(ActionRole.PRIMARY)
                    .build());
        }

        public Builder secondaryAction(String id, String text) {
            return action(DialogAction.builder(id, text)
                    .role(ActionRole.SECONDARY)
                    .build());
        }

        public Builder dangerAction(String id, String text) {
            return action(DialogAction.builder(id, text)
                    .role(ActionRole.DANGER)
                    .build());
        }

        public Builder neutralAction(String id, String text) {
            return action(DialogAction.builder(id, text)
                    .role(ActionRole.NEUTRAL)
                    .build());
        }

        public AppDialog build() {
            if (actions.isEmpty()) {
                primaryAction("ok", "OK");
            }
            return new AppDialog(this);
        }

        public DialogResult showDialog() {
            return build().showDialog();
        }
    }

    private final Builder config;
    private DialogResult result = new DialogResult(null, CloseReason.WINDOW_CLOSE);

    private final List<JButton> actionButtons = new ArrayList<>();
    private int focusedButtonIndex = -1;

    private AppDialog(Builder builder) {
        super(builder.owner,
                builder.windowTitle,
                builder.modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS);
        this.config = builder;
        initUI();
    }

    public static Builder builder(Window owner) {
        return new Builder(owner);
    }

    public DialogResult showDialog() {
        setVisible(true);
        return result;
    }

    private void initUI() {
        UiTheme theme = config.theme;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(config.resizable);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(theme.background);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.border),
                new EmptyBorder(20, 20, 16, 20)
        ));

        JPanel contentPanel = new JPanel(new BorderLayout(16, 0));
        contentPanel.setOpaque(false);

        contentPanel.add(buildIconPanel(config.type, theme), BorderLayout.WEST);
        contentPanel.add(buildTextPanel(theme), BorderLayout.CENTER);

        root.add(contentPanel, BorderLayout.CENTER);
        root.add(buildButtonsPanel(theme), BorderLayout.SOUTH);

        setContentPane(root);
        installButtonNavigation();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                result = new DialogResult(null, CloseReason.WINDOW_CLOSE);
            }
        });

        pack();
        setMinimumSize(new Dimension(config.minWidth, getPreferredSize().height));
        setLocationRelativeTo(getOwner());

        SwingUtilities.invokeLater(() -> {
            if (!actionButtons.isEmpty()) {
                int index = focusedButtonIndex >= 0 ? focusedButtonIndex : 0;
                focusButtonAt(index);
                refreshButtonStyles();
            }
        });
    }

    private JPanel buildTextPanel(UiTheme theme) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        boolean hasBodyTitle = config.showTitleInBody
                && config.bodyTitle != null
                && !config.bodyTitle.isBlank();

        if (hasBodyTitle) {
            JLabel titleLabel = new JLabel(config.bodyTitle);
            titleLabel.setFont(theme.titleFont);
            titleLabel.setForeground(theme.textPrimary);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(titleLabel);
            panel.add(Box.createVerticalStrut(10));
        }

        JLabel messageLabel = new JLabel(toHtml(config.message, theme.textPrimary, 13, true, 380));
        messageLabel.setFont(theme.messageFont);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(messageLabel);

        if (config.details != null && !config.details.isBlank()) {
            JLabel detailsLabel = new JLabel(toHtml(config.details, theme.textSecondary, 12, false, 400));
            detailsLabel.setFont(theme.detailsFont);
            detailsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(Box.createVerticalStrut(10));
            panel.add(detailsLabel);
        }

        return panel;
    }

    private JPanel buildButtonsPanel(UiTheme theme) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(18, 0, 0, 0));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsPanel.setOpaque(false);

        actionButtons.clear();
        JButton defaultButton = null;

        for (DialogAction action : config.actions) {
            JButton button = createButton(action, theme);
            actionButtons.add(button);

            if (defaultButton == null && action.getRole() == ActionRole.PRIMARY) {
                defaultButton = button;
            }

            buttonsPanel.add(button);
        }

        if (defaultButton == null && !actionButtons.isEmpty()) {
            defaultButton = actionButtons.get(0);
        }

        if (defaultButton != null) {
            focusedButtonIndex = actionButtons.indexOf(defaultButton);
            getRootPane().setDefaultButton(defaultButton);
        }

        wrapper.add(buttonsPanel, BorderLayout.CENTER);
        return wrapper;
    }

    private JButton createButton(DialogAction action, UiTheme theme) {
        JButton button = new JButton(action.getText());
        button.putClientProperty("dialog.role", action.getRole());
        button.setFont(theme.buttonFont);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);

        applyNormalStyle(button, action.getRole(), theme);

        button.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focusedButtonIndex = actionButtons.indexOf(button);
                getRootPane().setDefaultButton(button);
                refreshButtonStyles();
            }

            @Override
            public void focusLost(FocusEvent e) {
                SwingUtilities.invokeLater(AppDialog.this::refreshButtonStyles);
            }
        });

        button.addActionListener(e -> {
            result = new DialogResult(action.getId(), CloseReason.ACTION);

            if (action.getHandler() != null) {
                action.getHandler().accept(AppDialog.this);
            }

            if (action.isCloseOnClick()) {
                dispose();
            }
        });

        return button;
    }

    private void applyNormalStyle(JButton button, ActionRole role, UiTheme theme) {
        button.setFont(theme.buttonFont);

        switch (role) {
            case PRIMARY -> {
                button.setBackground(theme.accent);
                button.setForeground(theme.primaryButtonText);
            }
            case DANGER -> {
                button.setBackground(theme.error);
                button.setForeground(theme.primaryButtonText);
            }
            case SECONDARY, NEUTRAL -> {
                button.setBackground(theme.neutralButton);
                button.setForeground(theme.neutralButtonText);
            }
        }

        button.setBorder(createNormalBorder(theme, role));
    }

    private void applyFocusedStyle(JButton button, ActionRole role, UiTheme theme) {
        button.setFont(theme.buttonFont);

        switch (role) {
            case PRIMARY -> {
                button.setBackground(theme.accent);
                button.setForeground(theme.primaryButtonText);
            }
            case DANGER -> {
                button.setBackground(theme.error);
                button.setForeground(theme.primaryButtonText);
            }
            case SECONDARY, NEUTRAL -> {
                button.setBackground(theme.neutralButton);
                button.setForeground(theme.neutralButtonText);
            }
        }

        button.setBorder(createFocusedBorder(theme, role));
    }

    private Border createButtonBorder(Color outerColor, Color innerColor, int outerThickness, Insets padding) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(outerColor, outerThickness),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(innerColor, 1),
                        new EmptyBorder(padding)
                )
        );
    }

    private Border createNormalBorder(UiTheme theme, ActionRole role) {
        Color inner = switch (role) {
            case PRIMARY -> theme.accent;
            case DANGER -> theme.error;
            case SECONDARY, NEUTRAL -> theme.border;
        };

        return createButtonBorder(inner, inner, 1, new Insets(9, 16, 9, 16));
    }

    private Border createFocusedBorder(UiTheme theme, ActionRole role) {
        Color roleColor = switch (role) {
            case PRIMARY -> theme.accent;
            case DANGER -> theme.error;
            case SECONDARY, NEUTRAL -> theme.border;
        };

        return createButtonBorder(theme.focusRing, roleColor, 2, new Insets(8, 15, 8, 15));
    }

    private void refreshButtonStyles() {
        UiTheme theme = config.theme;
        JButton focusedButton = getFocusedActionButton();

        for (JButton button : actionButtons) {
            ActionRole role = (ActionRole) button.getClientProperty("dialog.role");
            boolean focused = button == focusedButton;

            if (focused) {
                applyFocusedStyle(button, role, theme);
            } else {
                applyNormalStyle(button, role, theme);
            }
        }

        repaint();
    }

    private JPanel buildIconPanel(DialogType type, UiTheme theme) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(52, 52));

        Color color = switch (type) {
            case INFO -> theme.accent;
            case WARNING -> theme.warning;
            case ERROR -> theme.error;
            case SUCCESS -> theme.success;
        };

        JPanel circle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        circle.setOpaque(false);
        circle.setPreferredSize(new Dimension(38, 38));
        circle.setLayout(new GridBagLayout());

        JLabel symbol = new JLabel(getSymbol(type));
        symbol.setForeground(Color.WHITE);
        symbol.setFont(new Font("SansSerif", Font.BOLD, 18));
        circle.add(symbol);

        panel.add(circle);
        return panel;
    }

    private String getSymbol(DialogType type) {
        return switch (type) {
            case INFO -> "i";
            case WARNING -> "!";
            case ERROR -> "×";
            case SUCCESS -> "✓";
        };
    }

    private String toHtml(String text, Color color, int size, boolean bold, int width) {
        String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        String weight = bold ? "font-weight:600;" : "";
        String safeText = text == null ? "" : text.replace("\n", "<br>");
        return "<html><div style='width:" + width + "px;color:" + hex + ";font-size:" + size + "px;" + weight + "'>"
                + safeText +
                "</div></html>";
    }

    private void installButtonNavigation() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "dialog.prevButton");
        inputMap.put(KeyStroke.getKeyStroke("UP"), "dialog.prevButton");
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "dialog.nextButton");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "dialog.nextButton");
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "dialog.pressFocused");
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "dialog.close");

        actionMap.put("dialog.prevButton", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                focusPreviousButton();
            }
        });

        actionMap.put("dialog.nextButton", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                focusNextButton();
            }
        });

        actionMap.put("dialog.pressFocused", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                pressFocusedButton();
            }
        });

        actionMap.put("dialog.close", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                result = new DialogResult(null, CloseReason.WINDOW_CLOSE);
                dispose();
            }
        });
    }

    private void focusPreviousButton() {
        if (actionButtons.isEmpty()) {
            return;
        }

        if (focusedButtonIndex < 0) {
            focusedButtonIndex = 0;
        } else {
            focusedButtonIndex = (focusedButtonIndex - 1 + actionButtons.size()) % actionButtons.size();
        }

        focusButtonAt(focusedButtonIndex);
    }

    private void focusNextButton() {
        if (actionButtons.isEmpty()) {
            return;
        }

        if (focusedButtonIndex < 0) {
            focusedButtonIndex = 0;
        } else {
            focusedButtonIndex = (focusedButtonIndex + 1) % actionButtons.size();
        }

        focusButtonAt(focusedButtonIndex);
    }

    private void focusButtonAt(int index) {
        if (index < 0 || index >= actionButtons.size()) {
            return;
        }

        JButton button = actionButtons.get(index);
        focusedButtonIndex = index;
        button.requestFocusInWindow();
        getRootPane().setDefaultButton(button);
        refreshButtonStyles();
    }

    private void pressFocusedButton() {
        JButton button = getFocusedActionButton();

        if (button != null) {
            button.doClick();
            return;
        }

        JButton defaultButton = getRootPane().getDefaultButton();
        if (defaultButton != null) {
            defaultButton.doClick();
        }
    }

    private JButton getFocusedActionButton() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

        for (JButton button : actionButtons) {
            if (button == focusOwner) {
                return button;
            }
        }

        if (focusedButtonIndex >= 0 && focusedButtonIndex < actionButtons.size()) {
            return actionButtons.get(focusedButtonIndex);
        }

        return null;
    }
}

/*
ESEMPIO DI UTILIZZO CON MOLTI PULSANTI
AppDialog.DialogResult result = AppDialog.builder(frame)
        .title("Operazione disponibile")
        .type(AppDialog.DialogType.INFO)
        .theme()
        .message("Seleziona l'azione da eseguire sul set di dati corrente.")
        .neutralAction("preview", "Anteprima")
        .secondaryAction("export-csv", "Esporta CSV")
        .secondaryAction("export-pdf", "Esporta PDF")
        .secondaryAction("recalc", "Ricalcola")
        .secondaryAction("open-log", "Apri log")
        .neutralAction("copy", "Copia")
        .dangerAction("reset", "Reset")
        .primaryAction("confirm", "Conferma")
        .showDialog();


*/