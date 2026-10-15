package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.awt.Window;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * La voce "Parte del LIFO mancante" del pulsante Errori: spiega le cause e conta quelle che si possono riconoscere
 * dai soli movimenti.
 * <p>
 * La pila del LiFo e il controllo delle giacenze ({@link Funzioni#ControllaSaldiNegativi}) non contano le stesse
 * cose: le giacenze sommano tutti i movimenti, la pila solo quelli che {@link Calcoli_PlusvalenzeNew#ElaboraMovimento}
 * carica. Solo i movimenti in entrata mai importati si vedono in 'Verifica Saldi Negativi'. Per le altre cause la
 * giacenza è giusta e la scheda resta vuota, per questo il dialogo non vi rimanda d'ufficio.
 */
public final class Principale_LifoMancante {

    private Principale_LifoMancante() {
    }

    /** Quanti movimenti elencare al più nel dialogo per ogni causa: oltre, si dice solo quanti altri ce ne sono. */
    private static final int MAX_ELENCATI = 10;

    /** Dove portare l'utente dopo il dialogo: la navigazione fra le schede resta in {@link Principale}. */
    public enum Scelta { NIENTE, MOSTRA_MOVIMENTI, SALDI_NEGATIVI, CLASSIFICA }

    /** Depositi che finiscono nella giacenza ma non nella pila del LiFo, per causa (ID dei movimenti). */
    public static final class Esito {
        /** Depositi DTW senza nessun prelievo PTW fra i movimenti collegati: non caricano nulla. */
        public final List<String> TrasferimentiSenzaPrelievo = new ArrayList<>();
        /** Depositi con un campo 18 che il motore non riconosce: non entrano in nessun ramo e non caricano nulla. */
        public final List<String> CausaliNonRiconosciute = new ArrayList<>();
    }

    /**
     * Cerca i depositi di cripto che il motore non carica nella pila pur contandoli la giacenza. Esclusi i token
     * SCAM, le quantità nulle e i depositi non classificati (quelli dipendono dall'opzione e li conta già il
     * pulsante Errori).
     * <p>
     * I depositi di categoria {@code TI} senza campo 18 non sono collegati a niente, ma restano fuori di proposito:
     * li genera l'importazione insieme al prelievo {@code TI} dello stesso exchange, che a sua volta non scarica la
     * pila, quindi si pareggiano.
     * @param Movimenti movimenti da esaminare
     * @return gli ID trovati, per causa
     */
    public static Esito Controlla(Collection<String[]> Movimenti) {
        Esito E = new Esito();
        for (String[] v : Movimenti) {
            //Stessa individuazione del deposito di cripto del motore: niente in uscita, in entrata non FIAT
            if (!v[9].trim().isBlank() || v[11].trim().isBlank() || v[12].trim().equalsIgnoreCase("FIAT")) continue;
            if (Funzioni.isSCAM(v[11]) || !Funzioni.isBigDecimalNonZero(v[13])) continue;
            String[] IDTS = v[0].split("_");
            String TipoID = IDTS.length > 4 ? IDTS[4] : "";
            if (!CausaleDepositoRiconosciuta(TipoID, v[18])) {
                E.CausaliNonRiconosciute.add(v[0]);
            } else if (!v[18].contains("DAI") && !TipoID.equalsIgnoreCase("RW") && v[18].contains("DTW")
                    && !HaPrelievoCollegato(v)) {
                E.TrasferimentiSenzaPrelievo.add(v[0]);
            }
        }
        return E;
    }

    /**
     * Se il motore ha un ramo per questo deposito. <b>Copia della catena di {@code if} dei depositi in
     * {@link Calcoli_PlusvalenzeNew#ElaboraMovimento}</b>, stesse condizioni e stessi confronti (anche il
     * {@code equals} sensibile alle maiuscole su {@code AC}): chi aggiunge un ramo là lo aggiunge qui, o il dialogo
     * segnala come non riconosciuti dei depositi che il motore carica.
     */
    static boolean CausaleDepositoRiconosciuta(String TipoID, String Campo18) {
        return TipoID.equalsIgnoreCase("RW") || Campo18.contains("DAI")
                || TipoID.equalsIgnoreCase("TI") || Campo18.contains("DTW")
                || Campo18.contains("DCZ")
                || Campo18.contains("DAC") || TipoID.equals("AC")
                || Campo18.contains("DDO")
                || Campo18.isBlank();
    }

    /**
     * Se fra i movimenti citati nel campo 20 ce n'è almeno uno, ancora presente, classificato PTW. Il motore cerca la
     * controparte solo col calcolo per gruppo wallet attivo, ma senza prelievo il trasferimento è comunque
     * sbilanciato: la moneta entra senza che nessun prelievo l'abbia tolta, e il deposito non carica la pila.
     */
    private static boolean HaPrelievoCollegato(String[] v) {
        if (v[20] == null || v[20].isBlank()) return false;
        for (String ID : v[20].split(",")) {
            String[] Mov = MappaCryptoWallet.get(ID.trim());
            if (Mov != null && Mov[18] != null && Mov[18].contains("PTW")) return true;
        }
        return false;
    }

    /**
     * Mostra il dialogo.
     * @param owner finestra su cui centrare il dialogo
     * @param NumNonClassificati movimenti di deposito e prelievo non classificati (contatore del pulsante Errori)
     * @return dove portare l'utente
     */
    public static Scelta Gestisci(Window owner, int NumNonClassificati) {
        boolean OpzioneNC = "NO".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi("PL_CosiderareMovimentiNC", "SI"));
        boolean PerGruppo = "SI".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi("PlusXWallet"));
        boolean CausaNC = OpzioneNC && NumNonClassificati > 0;
        Esito E = Controlla(new ArrayList<>(MappaCryptoWallet.values()));

        StringBuilder t = new StringBuilder()
                .append("Alcuni movimenti in uscita non trovano nella pila del LiFo abbastanza moneta da cui prendere ")
                .append("il costo di carico: la parte mancante entra nel calcolo con costo di carico zero.<br><br>")
                .append("<b>Le cause possibili sono:</b><br>")
                .append("&nbsp;• <b>Movimenti in entrata mai importati</b> (acquisti, earn, depositi): la giacenza va sotto ")
                .append("zero e la funzione <b>'Verifica Saldi Negativi'</b> la mostra.<br>");
        if (CausaNC) {
            t.append("&nbsp;• <b>Depositi non classificati</b> (").append(NumNonClassificati)
                    .append(" movimenti non classificati, fra depositi e prelievi): con l'opzione ")
                    .append("<i>\"I Movimenti di deposito e prelievo non classificati non vengono considerati nel calcolo ")
                    .append("delle plusvalenze fino alla loro classificazione\"</i> attiva non caricano la pila del LiFo, ")
                    .append("ma contano nella giacenza, quindi in 'Verifica Saldi Negativi' non compaiono.<br>");
        }
        if (PerGruppo) {
            t.append("&nbsp;• <b>Trasferimenti tra gruppi wallet</b>: il deposito prende il costo dalla pila del gruppo di ")
                    .append("provenienza al momento del deposito. Se lì in quel momento la moneta non basta, anche solo ")
                    .append("perché il deposito ha un orario precedente al prelievo, la mancanza compare sul trasferimento.<br>");
        }
        if (!E.TrasferimentiSenzaPrelievo.isEmpty()) {
            t.append("<br><b>Trasferimenti senza il prelievo collegato: ").append(E.TrasferimentiSenzaPrelievo.size())
                    .append("</b><br>")
                    .append("Depositi classificati come trasferimento fra wallet senza nessun prelievo collegato: non ")
                    .append("caricano la pila del LiFo ma contano nella giacenza, quindi in 'Verifica Saldi Negativi' non ")
                    .append("compaiono. Vanno riclassificati: apri il movimento con <i>Modifica movimento</i>, che annulla ")
                    .append("la classificazione, e classificalo di nuovo.<br>")
                    .append(Elenco(E.TrasferimentiSenzaPrelievo));
        }
        if (!E.CausaliNonRiconosciute.isEmpty()) {
            t.append("<br><b>Depositi con una classificazione che il calcolo non riconosce: ")
                    .append(E.CausaliNonRiconosciute.size()).append("</b><br>")
                    .append("Il dettaglio della classificazione non corrisponde a nessun tipo di deposito previsto: il ")
                    .append("calcolo non li carica nella pila del LiFo ma contano nella giacenza. Vanno riclassificati ")
                    .append("come sopra.<br>")
                    .append(Elenco(E.CausaliNonRiconosciute));
        }
        if (CausaNC) {
            t.append("<br>Nella tabella movimenti, il suggerimento sull'icona di errore della plusvalenza dice, movimento ")
                    .append("per movimento, se prima di esso ci sono depositi o prelievi non classificati della stessa moneta.");
        }

        AppDialog.Builder B = AppDialog.builder(owner)
                .title("Parte del LiFo mancante")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message("")
                .details(t.toString())
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("movimenti", "Mostra i movimenti")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .action(AppDialog.DialogAction.builder("saldi", "Verifica Saldi Negativi")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build());
        if (CausaNC) {
            B.action(AppDialog.DialogAction.builder("classifica", "Classifica i movimenti")
                    .role(AppDialog.ActionRole.PRIMARY)
                    .build());
        }
        AppDialog.DialogResult R = B.showDialog();
        if (R == null) return Scelta.NIENTE;
        if (R.isAction("movimenti")) return Scelta.MOSTRA_MOVIMENTI;
        if (R.isAction("saldi")) return Scelta.SALDI_NEGATIVI;
        if (R.isAction("classifica")) return Scelta.CLASSIFICA;
        return Scelta.NIENTE;
    }

    /** Elenco HTML dei movimenti, al più {@link #MAX_ELENCATI}: data, exchange, quantità, moneta e classificazione. */
    private static String Elenco(List<String> IDs) {
        StringBuilder S = new StringBuilder();
        for (int i = 0; i < IDs.size() && i < MAX_ELENCATI; i++) {
            String[] v = MappaCryptoWallet.get(IDs.get(i));
            if (v == null) continue;
            S.append(" - ").append(v[1]).append(" ").append(v[3]).append(" ").append(v[13]).append(" ").append(v[11]);
            if (!v[18].isBlank()) S.append(" - ").append(v[18]);
            S.append("<br>");
        }
        if (IDs.size() > MAX_ELENCATI) S.append(" ... e altri ").append(IDs.size() - MAX_ELENCATI).append("<br>");
        return S.toString();
    }
}
