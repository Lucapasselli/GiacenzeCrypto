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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class AppDialog extends JDialog {

    private JTextField inputField;
    private JTextArea textArea;
    private JComboBox<String> comboBox;

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
    private final String inputValue;

    public DialogResult(String actionId, CloseReason closeReason) {
        this(actionId, closeReason, null);
    }

    public DialogResult(String actionId, CloseReason closeReason, String inputValue) {
        this.actionId = actionId;
        this.closeReason = closeReason;
        this.inputValue = inputValue;
    }

    /** @return l'ID dell'azione con cui il dialog è stato chiuso, oppure {@code null} se chiuso dalla finestra */
    public String getActionId() {
        return actionId;
    }

    /** @return il motivo di chiusura del dialog (azione cliccata o finestra chiusa) */
    public CloseReason getCloseReason() {
        return closeReason;
    }

    /** @return il valore inserito nel campo di input del dialog, oppure {@code null} se non presente */
    public String getInputValue() {
        return inputValue;
    }

    /**
     * @param id ID di azione da confrontare
     * @return {@code true} se il dialog è stato chiuso con l'azione avente questo ID
     */
    public boolean isAction(String id) {
        return Objects.equals(this.actionId, id);
    }

    /** @return {@code true} se il dialog è stato chiuso tramite la chiusura della finestra invece che con un'azione */
    public boolean isClosedByWindow() {
        return closeReason == CloseReason.WINDOW_CLOSE;
    }

    /** @return {@code true} se il dialog ha restituito un valore di input */
    public boolean hasInputValue() {
        return inputValue != null;
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

        /** @return l'identificativo di questa azione */
        public String getId() {
            return id;
        }

        /** @return il testo mostrato sul pulsante di questa azione */
        public String getText() {
            return text;
        }

        /** @return il ruolo visivo di questa azione (primaria, secondaria, pericolosa, neutra) */
        public ActionRole getRole() {
            return role;
        }

        /** @return {@code true} se cliccando il pulsante il dialog si chiude automaticamente */
        public boolean isCloseOnClick() {
            return closeOnClick;
        }

        /** @return il gestore da eseguire al click sul pulsante, oppure {@code null} se non impostato */
        public Consumer<AppDialog> getHandler() {
            return handler;
        }

        /**
         * @param id identificativo dell'azione
         * @param text testo del pulsante
         * @return un nuovo {@link Builder} per costruire questa {@link DialogAction}
         */
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
            
            

            /** @param role ruolo visivo da assegnare all'azione @return questo builder, per il chaining */
            public Builder role(ActionRole role) {
                this.role = Objects.requireNonNull(role);
                return this;
            }

            /** @param closeOnClick se {@code true}, il dialog si chiude automaticamente al click su questa azione @return questo builder, per il chaining */
            public Builder closeOnClick(boolean closeOnClick) {
                this.closeOnClick = closeOnClick;
                return this;
            }

            /** @param handler gestore da eseguire al click sull'azione @return questo builder, per il chaining */
            public Builder onClick(Consumer<AppDialog> handler) {
                this.handler = handler;
                return this;
            }

            /** @return la {@link DialogAction} costruita con i parametri impostati su questo builder */
            public DialogAction build() {
                return new DialogAction(this);
            }
        }
    }
    
    public static String showTextInputDialog(
        Window owner,
        String windowTitle,
        String bodyTitle,
        String message,
        String inputLabel,
        String initialValue) {

    DialogResult result = AppDialog.builder(owner)
            .windowTitle(windowTitle)
            .bodyTitle(bodyTitle)
            .showTitleInBody(bodyTitle != null && !bodyTitle.isBlank())
            .theme()
            .type(DialogType.INFO)
            .message(message)
            .inputField(inputLabel, initialValue)
            .action(DialogAction.builder("cancel", "Annulla")
                    .role(ActionRole.SECONDARY)
                    .build())
            .action(DialogAction.builder("ok", "Conferma")
                    .role(ActionRole.PRIMARY)
                    .build())
            .showDialog();

    if (!result.isAction("ok")) {
        return null;
    }

    return result.getInputValue();
}

    public static String showComboBoxDialog(
        Window owner,
        String windowTitle,
        String bodyTitle,
        String message,
        String comboLabel,
        String... options) {

    DialogResult result = AppDialog.builder(owner)
            .windowTitle(windowTitle)
            .bodyTitle(bodyTitle)
            .showTitleInBody(bodyTitle != null && !bodyTitle.isBlank())
            .theme()
            .type(DialogType.WARNING)
            .message(message)
            .comboField(comboLabel, options)
            .action(DialogAction.builder("cancel", "Annulla")
                    .role(ActionRole.SECONDARY)
                    .build())
            .action(DialogAction.builder("ok", "Conferma")
                    .role(ActionRole.PRIMARY)
                    .build())
            .showDialog();

    if (!result.isAction("ok")) {
        return null;
    }

    return result.getInputValue();
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

        /**
         * @param mode {@link ThemeMode#LIGHT} o {@link ThemeMode#DARK}
         * @return la palette colori/font predefinita per il tema indicato
         */
        public static UiTheme of(ThemeMode mode) {
            Font title = new Font(FontApplicazione.FAMIGLIA, Font.BOLD, 18);
            Font message = new Font(FontApplicazione.FAMIGLIA, Font.PLAIN, 13);
            Font details = new Font(FontApplicazione.FAMIGLIA, Font.PLAIN, 12);
            Font button = new Font(FontApplicazione.FAMIGLIA, Font.BOLD, 13);

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
        
        private boolean inputEnabled = false;
        private String inputLabel;
        private String inputInitialValue = "";
        private int inputColumns = 28;

        private boolean textAreaEnabled = false;
        private String textAreaLabel;
        private String textAreaInitialValue = "";
        private int textAreaRows = 8;
        private int textAreaColumns = 34;

        private boolean comboEnabled = false;
        private String comboLabel;
        private String[] comboOptions = new String[0];

        private final List<DialogAction> actions = new ArrayList<>();

        /**
         * Aggiunge al dialog una tendina a scelta singola.
         * @param label etichetta della tendina
         * @param options opzioni selezionabili
         * @return questo builder, per il chaining
         */
        public Builder comboField(String label, String... options) {
            this.comboEnabled = true;
            this.comboLabel = label;
            this.comboOptions = options != null ? options : new String[0];
            return this;
        }

        /**
         * Aggiunge al dialog un campo di testo per l'input.
         * @param label etichetta del campo
         * @return questo builder, per il chaining
         */
        public Builder inputField(String label) {
            this.inputEnabled = true;
            this.inputLabel = label;
            return this;
        }

        /**
         * Come {@link #inputField(String)}, con un valore iniziale precompilato.
         * @param label etichetta del campo
         * @param initialValue valore iniziale del campo (usato {@code ""} se {@code null})
         * @return questo builder, per il chaining
         */
        public Builder inputField(String label, String initialValue) {
            this.inputEnabled = true;
            this.inputLabel = label;
            this.inputInitialValue = initialValue != null ? initialValue : "";
            return this;
        }

        /** @param inputColumns larghezza del campo di input in colonne (minimo 10) @return questo builder, per il chaining */
        public Builder inputColumns(int inputColumns) {
            this.inputColumns = Math.max(10, inputColumns);
            return this;
        }

        /**
         * Aggiunge al dialog un'area di testo multiriga, per i testi in cui i ritorni a capo contano
         * (le note di un movimento, per esempio). A differenza di {@link #inputField(String)} il tasto Invio
         * va a capo nel testo invece di premere il pulsante di default: l'area consuma il tasto prima che
         * arrivi alle scorciatoie del dialog. Il Tab, al contrario, sposta il fuoco sui pulsanti come nel
         * resto dei dialoghi, invece di inserire una tabulazione.
         * @param label etichetta mostrata sopra l'area ({@code null} o vuota per non mostrarla)
         * @return questo builder, per il chaining
         */
        public Builder textAreaField(String label) {
            this.textAreaEnabled = true;
            this.textAreaLabel = label;
            return this;
        }

        /**
         * Come {@link #textAreaField(String)}, con un testo iniziale precompilato.
         * @param label etichetta mostrata sopra l'area ({@code null} o vuota per non mostrarla)
         * @param initialValue testo iniziale dell'area (usato {@code ""} se {@code null})
         * @return questo builder, per il chaining
         */
        public Builder textAreaField(String label, String initialValue) {
            this.textAreaEnabled = true;
            this.textAreaLabel = label;
            this.textAreaInitialValue = initialValue != null ? initialValue : "";
            return this;
        }

        /**
         * @param rows righe visibili dell'area di testo (minimo 2)
         * @param columns larghezza dell'area in colonne (minimo 10)
         * @return questo builder, per il chaining
         */
        public Builder textAreaSize(int rows,int columns) {
            this.textAreaRows = Math.max(2, rows);
            this.textAreaColumns = Math.max(10, columns);
            return this;
        }

        private Builder(Window owner) {
            this.owner = owner;
        }

        /** @param title titolo usato sia per la finestra sia per il corpo del dialog @return questo builder, per il chaining */
        public Builder title(String title) {
            this.windowTitle = title;
            this.bodyTitle = title;
            return this;
        }

        /** @param windowTitle titolo della finestra del dialog @return questo builder, per il chaining */
        public Builder windowTitle(String windowTitle) {
            this.windowTitle = windowTitle;
            return this;
        }

        /** @param bodyTitle titolo mostrato nel corpo del dialog @return questo builder, per il chaining */
        public Builder bodyTitle(String bodyTitle) {
            this.bodyTitle = bodyTitle;
            return this;
        }

        /** @param showTitleInBody se {@code true} mostra il titolo anche nel corpo del dialog @return questo builder, per il chaining */
        public Builder showTitleInBody(boolean showTitleInBody) {
            this.showTitleInBody = showTitleInBody;
            return this;
        }

        /** Nasconde il titolo dal corpo del dialog. @return questo builder, per il chaining */
        public Builder hideBodyTitle() {
            this.showTitleInBody = false;
            return this;
        }

        /** Mostra il titolo nel corpo del dialog. @return questo builder, per il chaining */
        public Builder showBodyTitle() {
            this.showTitleInBody = true;
            return this;
        }

        /** @param message messaggio principale del dialog (usato {@code ""} se {@code null}) @return questo builder, per il chaining */
        public Builder message(String message) {
            this.message = message != null ? message : "";
            return this;
        }

        /** @param details testo di dettaglio, mostrato sotto il messaggio principale @return questo builder, per il chaining */
        public Builder details(String details) {
            this.details = details;
            return this;
        }

        /** @param type tipo di dialog (icona/stile), usato {@link DialogType#INFO} se {@code null} @return questo builder, per il chaining */
        public Builder type(DialogType type) {
            this.type = type != null ? type : DialogType.INFO;
            return this;
        }

        /**
         * Applica il tema (chiaro/scuro) corrente dell'applicazione, letto da {@link Principale#tema}.
         * @return questo builder, per il chaining
         */
        public Builder theme() {
            this.theme = AppDialog.UiTheme.of(Principale.tema.equalsIgnoreCase("scuro")
                    ? AppDialog.ThemeMode.DARK
                    : AppDialog.ThemeMode.LIGHT);
            return this;
        }

        /** @param modal se {@code true} il dialog è modale @return questo builder, per il chaining */
        public Builder modal(boolean modal) {
            this.modal = modal;
            return this;
        }

        /** @param resizable se {@code true} il dialog è ridimensionabile @return questo builder, per il chaining */
        public Builder resizable(boolean resizable) {
            this.resizable = resizable;
            return this;
        }

        /** @param minWidth larghezza minima del dialog in pixel (minimo 360) @return questo builder, per il chaining */
        public Builder minWidth(int minWidth) {
            this.minWidth = Math.max(360, minWidth);
            return this;
        }

        /** @param maxButtons numero massimo di pulsanti azione consentiti (minimo 1) @return questo builder, per il chaining */
        public Builder maxButtons(int maxButtons) {
            this.maxButtons = Math.max(1, maxButtons);
            return this;
        }

        /**
         * Aggiunge un'azione (pulsante) al dialog.
         * @param action azione da aggiungere, ignorata se {@code null}
         * @return questo builder, per il chaining
         * @throws IllegalStateException se è già stato raggiunto il numero massimo di pulsanti ({@link #maxButtons})
         */
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

        /** Aggiunge un'azione con ruolo {@link ActionRole#PRIMARY}. @param id identificativo @param text testo del pulsante @return questo builder, per il chaining */
        public Builder primaryAction(String id, String text) {
            return action(DialogAction.builder(id, text)
                    .role(ActionRole.PRIMARY)
                    .build());
        }

        /** Aggiunge un'azione con ruolo {@link ActionRole#SECONDARY}. @param id identificativo @param text testo del pulsante @return questo builder, per il chaining */
        public Builder secondaryAction(String id, String text) {
            return action(DialogAction.builder(id, text)
                    .role(ActionRole.SECONDARY)
                    .build());
        }

        /** Aggiunge un'azione con ruolo {@link ActionRole#DANGER}. @param id identificativo @param text testo del pulsante @return questo builder, per il chaining */
        public Builder dangerAction(String id, String text) {
            return action(DialogAction.builder(id, text)
                    .role(ActionRole.DANGER)
                    .build());
        }

        /** Aggiunge un'azione con ruolo {@link ActionRole#NEUTRAL}. @param id identificativo @param text testo del pulsante @return questo builder, per il chaining */
        public Builder neutralAction(String id, String text) {
            return action(DialogAction.builder(id, text)
                    .role(ActionRole.NEUTRAL)
                    .build());
        }

        /**
         * Costruisce il dialog con i parametri impostati su questo builder, aggiungendo un'azione "OK" di
         * default se non ne è stata specificata nessuna.
         * @return il nuovo {@link AppDialog} costruito
         */
        public AppDialog build() {
            if (actions.isEmpty()) {
                primaryAction("ok", "OK");
            }
            return new AppDialog(this);
        }

        /**
         * Costruisce il dialog (vedi {@link #build()}) e lo mostra subito.
         * @return il risultato dell'interazione dell'utente
         */
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

    /**
     * @param owner finestra parent del dialog
     * @return un nuovo {@link Builder} per configurare e mostrare un {@link AppDialog}
     */
    public static Builder builder(Window owner) {
        return new Builder(owner);
    }

    /**
     * Mostra il dialog (bloccando se modale) e ne restituisce il risultato una volta chiuso.
     * @return il risultato dell'interazione dell'utente
     */
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
            /** Registra la chiusura del dialog tramite la finestra (nessuna azione selezionata). */
            @Override
            public void windowClosing(WindowEvent e) {
                result = new DialogResult(null, CloseReason.WINDOW_CLOSE);
            }
        });

        pack();
        setMinimumSize(new Dimension(config.minWidth, getPreferredSize().height));
        setLocationRelativeTo(getOwner());

        SwingUtilities.invokeLater(() -> {
            if (config.inputEnabled && inputField != null) {
                inputField.requestFocusInWindow();
                inputField.selectAll();
            } else if (config.textAreaEnabled && textArea != null) {
                //Caret in fondo e niente selectAll: qui il testo iniziale è spesso quello da correggere,
                //e selezionarlo tutto lo farebbe cancellare dal primo tasto premuto
                textArea.requestFocusInWindow();
                textArea.setCaretPosition(textArea.getDocument().getLength());
            } else if (config.comboEnabled && comboBox != null) {
                comboBox.requestFocusInWindow();
            } else if (!actionButtons.isEmpty()) {
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
        
        if (config.inputEnabled) {
    panel.add(Box.createVerticalStrut(12));

    if (config.inputLabel != null && !config.inputLabel.isBlank()) {
        JLabel inputLabel = new JLabel(config.inputLabel);
        inputLabel.setFont(theme.messageFont);
        inputLabel.setForeground(theme.textPrimary);
        inputLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(inputLabel);
        panel.add(Box.createVerticalStrut(6));
    }

    inputField = new JTextField(config.inputInitialValue, config.inputColumns);
    inputField.setFont(theme.messageFont.deriveFont(16f));
    inputField.setForeground(theme.textPrimary);
    inputField.setBackground(theme.surface);
    inputField.setCaretColor(theme.textPrimary);
    inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(theme.border),
            new EmptyBorder(8, 10, 8, 10)
    ));
    inputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    inputField.setAlignmentX(Component.LEFT_ALIGNMENT);

    panel.add(inputField);
}

        if (config.textAreaEnabled) {
            panel.add(Box.createVerticalStrut(12));

            if (config.textAreaLabel != null && !config.textAreaLabel.isBlank()) {
                JLabel areaLabel = new JLabel(config.textAreaLabel);
                areaLabel.setFont(theme.messageFont);
                areaLabel.setForeground(theme.textPrimary);
                areaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(areaLabel);
                panel.add(Box.createVerticalStrut(6));
            }

            textArea = new JTextArea(config.textAreaInitialValue, config.textAreaRows, config.textAreaColumns);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setFont(theme.messageFont.deriveFont(14f));
            textArea.setForeground(theme.textPrimary);
            textArea.setBackground(theme.surface);
            textArea.setCaretColor(theme.textPrimary);
            textArea.setBorder(new EmptyBorder(8, 10, 8, 10));

            //Il Tab dentro una JTextArea inserirebbe una tabulazione, intrappolando il fuoco: qui deve
            //spostarlo sui pulsanti come in tutti gli altri dialoghi. L'Invio invece resta all'area, che
            //lo consuma con la propria mappa WHEN_FOCUSED prima delle scorciatoie di finestra del dialog
            textArea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                    Set.of(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
            textArea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                    Set.of(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)));

            JScrollPane areaScroll = new JScrollPane(textArea);
            areaScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
            areaScroll.setBorder(BorderFactory.createLineBorder(theme.border));
            areaScroll.getViewport().setBackground(theme.surface);
            areaScroll.setBackground(theme.surface);

            panel.add(areaScroll);
        }

        if (config.comboEnabled) {
            panel.add(Box.createVerticalStrut(12));

            if (config.comboLabel != null && !config.comboLabel.isBlank()) {
                JLabel comboLabelComp = new JLabel(config.comboLabel);
                comboLabelComp.setFont(theme.messageFont);
                comboLabelComp.setForeground(theme.textPrimary);
                comboLabelComp.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(comboLabelComp);
                panel.add(Box.createVerticalStrut(6));
            }

            comboBox = new JComboBox<>(config.comboOptions);
            comboBox.setFont(theme.messageFont);
            comboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            panel.add(comboBox);
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
            /** Aggiorna il pulsante di default e lo stile dei pulsanti quando questo pulsante riceve il focus. */
            @Override
            public void focusGained(FocusEvent e) {
                focusedButtonIndex = actionButtons.indexOf(button);
                getRootPane().setDefaultButton(button);
                refreshButtonStyles();
            }

            /** Aggiorna lo stile dei pulsanti quando questo pulsante perde il focus. */
            @Override
            public void focusLost(FocusEvent e) {
                SwingUtilities.invokeLater(AppDialog.this::refreshButtonStyles);
            }
        });

        button.addActionListener(e -> {
            String value;
            if (inputField != null) {
                value = inputField.getText();
            } else if (textArea != null) {
                value = textArea.getText();
            } else if (comboBox != null) {
                Object sel = comboBox.getSelectedItem();
                value = sel != null ? sel.toString() : null;
            } else {
                value = null;
            }
            result = new DialogResult(action.getId(), CloseReason.ACTION, value);

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
            /** Disegna un cerchio pieno, con antialiasing, colorato in base al tipo di dialog. */
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 0, getWidth(), getHeight());

                //La spunta è disegnata, non scritta: nessuno dei caratteri di spunta
                //(U+2713, U+2714) esiste in Noto Sans, il font incluso nel jar, e un carattere
                //mancante in un font fisico diventa un rettangolo vuoto invece di essere
                //cercato altrove come faceva il font logico. Vedi FontApplicazione.
                if (type == DialogType.SUCCESS) {
                    float l = getWidth(), h = getHeight();
                    java.awt.geom.GeneralPath spunta = new java.awt.geom.GeneralPath();
                    spunta.moveTo(l * 0.28f, h * 0.52f);
                    spunta.lineTo(l * 0.43f, h * 0.68f);
                    spunta.lineTo(l * 0.72f, h * 0.33f);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(Math.max(2f, l * 0.09f),
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.draw(spunta);
                }

                g2.dispose();
            }
        };
        circle.setOpaque(false);
        circle.setPreferredSize(new Dimension(38, 38));
        circle.setLayout(new GridBagLayout());

        if (!getSymbol(type).isEmpty()) {
            JLabel symbol = new JLabel(getSymbol(type));
            symbol.setForeground(Color.WHITE);
            symbol.setFont(new Font(FontApplicazione.FAMIGLIA, Font.BOLD, 18));
            circle.add(symbol);
        }

        panel.add(circle);
        return panel;
    }

    private String getSymbol(DialogType type) {
        return switch (type) {
            case INFO -> "i";
            case WARNING -> "!";
            case ERROR -> "×";
            //SUCCESS non ha simbolo testuale: la spunta la disegna il cerchio, sopra.
            case SUCCESS -> "";
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
            /** Sposta il focus sul pulsante precedente (frecce sinistra/su). */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                focusPreviousButton();
            }
        });

        actionMap.put("dialog.nextButton", new AbstractAction() {
            /** Sposta il focus sul pulsante successivo (frecce destra/giù). */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                focusNextButton();
            }
        });

        actionMap.put("dialog.pressFocused", new AbstractAction() {
            /** Attiva il pulsante attualmente a fuoco (tasto Invio). */
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                pressFocusedButton();
            }
        });

        actionMap.put("dialog.close", new AbstractAction() {
            /** Chiude il dialog senza selezionare alcuna azione (tasto Esc). */
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
    if (config.inputEnabled || config.comboEnabled) {
        JButton defaultButton = getRootPane().getDefaultButton();
        if (defaultButton != null) {
            defaultButton.doClick();
        }
        return;
    }

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
