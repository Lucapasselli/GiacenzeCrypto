package com.giacenzecrypto.giacenze_crypto;

import java.awt.BorderLayout;
import java.awt.Window;

/**
 * Finestra della gestione documentale: l'elenco dei file da cui i movimenti sono stati importati, aperto
 * come dialogo dal pulsante in <i>Opzioni</i>.
 *
 * <p>Del pannello non c'è nulla qui dentro : tutta la parte grafica sta in
 * {@link GUI_DocumentiFonte_Pannello}, che è la stessa istanza di classe montata anche nel tab
 * "Gestione Documentale" di {@link Principale}. Questa classe è solo la cornice — titolo, modalità,
 * dimensioni, pulsante di chiusura — e per questo non ha un {@code .form} : quello che il GUI Builder
 * deve poter disegnare è il pannello, non il contenitore.
 */
public class GUI_DocumentiFonte extends javax.swing.JDialog {

    private static final long serialVersionUID = 1L;

    private final GUI_DocumentiFonte_Pannello Pannello;

    /**
     * Apre la finestra, e al ritorno segnala al tab "Gestione Documentale" se c'è da rileggere.
     *
     * <p>Il tab e il dialogo sono due istanze <b>vive contemporaneamente</b> dello stesso pannello : se qui
     * si elimina un documento, la tabella dell'altro resta a mostrarlo. Alzare il flag alla chiusura è ciò
     * che chiude il giro, ed è lo stesso meccanismo di {@code GestioneTokenScamDaAggiornare}.
     *
     * @param Proprietario finestra rispetto a cui centrarsi
     */
    public static void Mostra(Window Proprietario) {
        GUI_DocumentiFonte g = new GUI_DocumentiFonte(Proprietario);
        g.setLocationRelativeTo(Proprietario);
        g.setVisible(true);
        if (g.Pannello.Modificato()) {
            Principale.GestioneDocumentaleDaAggiornare = true;
        }
    }

    /**
     * Crea la finestra attorno al pannello.
     * @param Proprietario finestra parent
     */
    private GUI_DocumentiFonte(Window Proprietario) {
        super(Proprietario, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Documenti di origine dei movimenti");

        Pannello = new GUI_DocumentiFonte_Pannello();
        //Qui il pulsante "Chiudi" serve, nel tab no : è il pannello a saperlo fare comparire.
        Pannello.ImpostaAzioneChiusura(this::dispose);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(Pannello, BorderLayout.CENTER);

        //Prima si impacchetta sul contenuto, poi si allarga: partendo da una dimensione fissa la riga in
        //fondo veniva tagliata, perché fra etichetta di riepilogo e cinque pulsanti con icona serve più
        //spazio di quanto se ne indovini a priori. Il minimo è quello che il layout dichiara di volere,
        //così la finestra non è mai rimpicciolibile fino a nascondere i pulsanti.
        pack();
        setMinimumSize(getSize());
        setSize(Math.max(getWidth(), 1200), Math.max(getHeight(), 600));
    }
}
