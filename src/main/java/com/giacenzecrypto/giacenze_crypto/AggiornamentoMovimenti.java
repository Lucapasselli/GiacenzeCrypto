package com.giacenzecrypto.giacenze_crypto;

import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.util.function.LongSupplier;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Coordinatore del ricalcolo e del ricarico delle tabelle dopo che i movimenti sono cambiati.
 *
 * <p>Chi modifica i movimenti alza {@link Principale#TabellaCryptodaAggiornare}. Fino al 2026-11-15 il flag lo consumava
 * solo {@code formWindowGainedFocus}, cioe' un evento che deve consegnare il window manager: su Linux, con piu'
 * dialoghi modali chiusi a raffica, la finestra principale puo' non riceverlo mai e la tabella restava com'era finche'
 * l'utente non cliccava (riprodotto senza window manager, vedi
 * {@code nocommit/Documentazione/Analisi_Aggiornamento_Tabelle_Dopo_Operazioni.md}). Ora il flag ha tre consumatori
 * equivalenti e innocui se ripetuti:
 * <ol>
 *   <li>il <b>focus</b> della principale, come sempre e <b>senza condizioni</b> (nessuna regressione);</li>
 *   <li>la <b>chiusura di un qualunque dialogo</b> ({@code WINDOW_CLOSED}), che genera l'evento anche quando il focus no;</li>
 *   <li>un <b>timer di sicurezza</b>, che agisce solo se il flag resta alzato da almeno {@link #ATTESA_MINIMA_MS}.</li>
 * </ol>
 * Gli ultimi due (i soli nuovi) non partono finche' c'e' un impedimento: un dialogo modale visibile, una finestra di
 * avanzamento {@link Download} (anche non modale), un ricalcolo gia' in corso, una cella in modifica. La mappa
 * {@code MappaCryptoWallet} e' un {@code TreeMap} non sincronizzato, quindi non si ricalcola sotto un thread che scrive.
 *
 * <p>Limite noto: un'operazione che scrive la mappa da un thread <b>senza alcuna finestra visibile</b> e per piu' di
 * {@link #ATTESA_MINIMA_MS} dopo aver alzato il flag non e' vista. Oggi non ne esistono: gli import alzano il flag a
 * scrittura finita, quasi sempre sotto un {@link Download}.
 *
 * <p>La logica non tocca Swing: l'{@link Ambiente} e l'orologio sono iniettati ({@code AggiornamentoMovimentiTest}).
 * Le righe di log cominciano per {@code [Aggiornamento]}.
 */
public final class AggiornamentoMovimenti {

    /** Da quanto il flag deve essere visto alzato perche' lo consumi il timer (da solo o dopo la chiusura di un dialogo). */
    static final long ATTESA_MINIMA_MS = 1000;
    private static final int PERIODO_TIMER_MS = 500;
    private static final long PERIODO_AVVISO_MS = 10000;

    /** Chi ha consumato il flag. */
    public enum Innesco {
        FOCUS("focus"), CHIUSURA_DIALOGO("chiusura dialogo"), TIMER("timer");

        final String testo;

        Innesco(String testo) {
            this.testo = testo;
        }
    }

    /** Il mondo visto dal coordinatore: in produzione {@link Principale} e Swing, nei test un finto. */
    public interface Ambiente {

        /** Il flag e' alzato. */
        boolean inSospeso();

        /** Abbassa il flag. */
        void abbassa();

        /** Il motivo per cui adesso non si puo' ricalcolare, o {@code null} se si puo'. */
        String impedimento();

        /** Ricalcola e ricarica le tabelle (sull'EDT). */
        void aggiorna();
    }

    private final Ambiente ambiente;
    private final LongSupplier orologio;
    private long inSospesoDal = 0;
    private long ultimoAvviso = 0;
    private boolean inEsecuzione = false;

    public AggiornamentoMovimenti(Ambiente ambiente, LongSupplier orologio) {
        this.ambiente = ambiente;
        this.orologio = orologio;
    }

    /**
     * Consuma il flag se alzato. Il focus ({@link Innesco#FOCUS}) lo fa sempre, gli altri solo senza impedimenti e
     * (il timer e la chiusura di un dialogo) dopo {@link #ATTESA_MINIMA_MS}.
     *
     * @return vero se ha ricalcolato
     */
    public synchronized boolean Prova(Innesco innesco) {
        if (inEsecuzione || !ambiente.inSospeso()) {
            return false;
        }
        long ora = orologio.getAsLong();
        if (inSospesoDal == 0) {
            inSospesoDal = ora;
            System.out.println("[Aggiornamento] in sospeso");
        }
        if (innesco != Innesco.FOCUS) {
            String impedimento = ambiente.impedimento();
            if (impedimento != null) {
                AvvisaSeBloccato(ora, impedimento);
                return false;
            }
            if (ora - inSospesoDal < ATTESA_MINIMA_MS) {
                return false;
            }
        }
        long durata = ora - inSospesoDal;
        inSospesoDal = 0;
        ultimoAvviso = 0;
        ambiente.abbassa();
        inEsecuzione = true;
        try {
            System.out.println("[Aggiornamento] eseguito da: " + innesco.testo + " dopo " + durata + " ms");
            ambiente.aggiorna();
        } catch (Throwable e) {
            //il flag e' gia' basso: se l'errore si ripresentasse a ogni giro il timer non deve martellare
            System.err.println("ERRORE nell'aggiornamento delle tabelle: " + e);
            e.printStackTrace();
        } finally {
            inEsecuzione = false;
        }
        return true;
    }

    /** Un giro del timer: ricorda da quando il flag e' alzato, poi prova. */
    public void Tick() {
        if (!ambiente.inSospeso()) {
            synchronized (this) {
                inSospesoDal = 0;
                ultimoAvviso = 0;
            }
            return;
        }
        Prova(Innesco.TIMER);
    }

    private void AvvisaSeBloccato(long ora, String impedimento) {
        if (ora - inSospesoDal >= PERIODO_AVVISO_MS && ora - ultimoAvviso >= PERIODO_AVVISO_MS) {
            ultimoAvviso = ora;
            System.out.println("[Aggiornamento] ANCORA in sospeso da " + (ora - inSospesoDal) / 1000 + " s: " + impedimento);
        }
    }

    // ---------------------------------------------------------------- collegamento con la finestra principale

    private static AggiornamentoMovimenti istanza;
    private static Timer timer;

    /**
     * Da chiamare una volta, a finestra principale costruita (EDT): avvia il timer e il listener globale di chiusura dei
     * dialoghi.
     */
    public static void Avvia(Principale principale) {
        if (istanza != null) {
            return;
        }
        istanza = new AggiornamentoMovimenti(new Ambiente() {
            @Override
            public boolean inSospeso() {
                return Principale.TabellaCryptodaAggiornare;
            }

            @Override
            public void abbassa() {
                Principale.TabellaCryptodaAggiornare = false;
            }

            @Override
            public String impedimento() {
                return ImpedimentoSwing(principale);
            }

            @Override
            public void aggiorna() {
                principale.Funzioni_AggiornaTutto();
            }
        }, System::currentTimeMillis);
        timer = new Timer(PERIODO_TIMER_MS, e -> istanza.Tick());
        timer.setCoalesce(true);
        timer.start();
        Toolkit.getDefaultToolkit().addAWTEventListener((AWTEvent ev) -> {
            if (ev.getID() == WindowEvent.WINDOW_CLOSED && ev.getSource() instanceof Dialog) {
                //dopo che la pila di chiusure ha finito, non dentro di essa
                SwingUtilities.invokeLater(() -> istanza.Prova(Innesco.CHIUSURA_DIALOGO));
            }
        }, AWTEvent.WINDOW_EVENT_MASK);
    }

    /** Il consumo da focus: sempre, come prima del coordinatore. */
    public static void FocusRicevuto() {
        if (istanza != null) {
            istanza.Prova(Innesco.FOCUS);
        }
    }

    /** Il motivo per cui ora non si puo' ricalcolare guardando Swing, o {@code null}. */
    static String ImpedimentoSwing(Principale principale) {
        if (principale == null || !principale.isShowing()) {
            return "finestra principale non visibile";
        }
        for (Window w : Window.getWindows()) {
            if (!w.isShowing()) {
                continue;
            }
            if (w instanceof Dialog d && d.isModal()) {
                return "dialogo modale aperto (" + w.getClass().getSimpleName() + ")";
            }
            if (w instanceof Download) {
                return "finestra di avanzamento aperta";
            }
        }
        java.awt.Component f = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (f != null) {
            JTable t = f instanceof JTable jt ? jt : (JTable) SwingUtilities.getAncestorOfClass(JTable.class, f);
            if (t != null && t.isEditing()) {
                return "cella di tabella in modifica";
            }
        }
        return null;
    }
}
