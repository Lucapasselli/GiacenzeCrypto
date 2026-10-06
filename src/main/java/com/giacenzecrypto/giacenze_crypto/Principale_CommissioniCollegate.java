package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Operazioni sulle commissioni collegate ({@link CommissioniCollegate}, campo 43) richiamate
 * dall'interfaccia: l'abbinamento delle commissioni già in archivio, il collegamento e lo scollegamento
 * manuali dal menu contestuale, e il testo mostrato nel dettaglio del movimento.
 *
 * <p>Nessuna di queste operazioni tocca un calcolo: il campo è solo informativo, quindi dopo averle
 * eseguite basta accendere il pulsante Salva, senza ricalcolo. Non scrivono nemmeno nello storico delle
 * modifiche, che racconta le modifiche ai dati del movimento e non a un'annotazione come questa.
 */
public final class Principale_CommissioniCollegate {

    private Principale_CommissioniCollegate() {
    }

    // =================================================================================================
    // ABBINAMENTO DELL'ARCHIVIO ESISTENTE
    // =================================================================================================

    /**
     * Distanza massima fra una commissione e i movimenti con lo stesso hash perché siano considerati la
     * stessa operazione. Un hash di transazione o un id d'ordine sono già univoci; il limite protegge dai
     * formati che in quel campo scrivono un valore ripetuto.
     */
    static final long DISTANZA_MAX_HASH_MS = 3600_000L;

    /**
     * Oltre questo numero di movimenti con lo stesso hash nella stessa ora il valore non identifica una
     * transazione (un formato che scrive in {@code [24]} qualcosa di ripetuto): la commissione viene
     * contata fra le ambigue invece di essere collegata a tutti.
     */
    static final int MAX_CANDIDATI_HASH = 20;

    /** Esito di {@link #AbbinaArchivio()}. */
    public static final class Esito {
        /** Commissioni collegate da questa passata. */
        public int Collegate;
        /** Commissioni che avevano già una chiave, lasciate com'erano. */
        public int GiaCollegate;
        /** Commissioni con più movimenti candidati nello stesso istante e senza hash che li distingua. */
        public int Ambigue;
        /** Commissioni senza nessun movimento candidato. */
        public int SenzaMovimento;
    }

    /**
     * Collega le commissioni dell'archivio che non hanno ancora una chiave ai movimenti a cui appartengono.
     * Rilanciabile: le commissioni già collegate non vengono toccate.
     *
     * <p>Criteri, nell'ordine:
     * <ol>
     *   <li><b>stesso hash</b> ({@code [24]}), stesso exchange/wallet ({@code [3]}) e al più
     *       {@link #DISTANZA_MAX_HASH_MS} di distanza: la commissione va a <b>tutti</b> quei movimenti,
     *       come fa l'import DeFi (il gas paga l'intera transazione) e come vale per un id d'ordine;</li>
     *   <li>altrimenti <b>stesso secondo</b> (la data dell'ID) e stesso {@code [3]}: se il movimento
     *       candidato è uno solo si collega, se sono più di uno la commissione resta com'è e viene
     *       contata fra le ambigue. Nei casi dubbi non si abbina, per scelta concordata.</li>
     * </ol>
     *
     * <p>Sono esclusi, come commissioni e come movimenti principali, i movimenti generati dalla
     * classificazione ({@code [22] = "AU"}): le commissioni di trasferimento non ricevono la chiave, e i
     * movimenti sintetici spariscono quando la classificazione viene annullata.
     */
    static Esito AbbinaArchivio() {
        Esito E = new Esito();

        //Una sola passata per gli indici dei candidati
        Map<String, List<String[]>> PerHash = new HashMap<>();
        Map<String, List<String[]>> PerIstante = new HashMap<>();
        List<String[]> Commissioni = new ArrayList<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if ("AU".equalsIgnoreCase(v[22])) continue;
            if (CommissioniCollegate.isCommissione(v)) {
                if (!CommissioniCollegate.Chiave(v).isEmpty()) E.GiaCollegate++;
                else Commissioni.add(v);
                continue;
            }
            if (!Funzioni.noData(v[24])) {
                PerHash.computeIfAbsent(ChiaveHash(v), k -> new ArrayList<>()).add(v);
            }
            PerIstante.computeIfAbsent(ChiaveIstante(v), k -> new ArrayList<>()).add(v);
        }

        for (String[] Fee : Commissioni) {
            List<String[]> Principali = new ArrayList<>();
            if (!Funzioni.noData(Fee[24])) {
                long IstanteFee = Istante(Fee);
                for (String[] v : PerHash.getOrDefault(ChiaveHash(Fee), List.of())) {
                    if (IstanteFee > 0 && Math.abs(Istante(v) - IstanteFee) <= DISTANZA_MAX_HASH_MS) Principali.add(v);
                }
                if (Principali.size() > MAX_CANDIDATI_HASH) {
                    E.Ambigue++;
                    continue;
                }
            }
            if (Principali.isEmpty()) {
                List<String[]> Stesso = PerIstante.getOrDefault(ChiaveIstante(Fee), List.of());
                if (Stesso.size() > 1) {
                    E.Ambigue++;
                    continue;
                }
                Principali.addAll(Stesso);
            }
            if (Principali.isEmpty()) {
                E.SenzaMovimento++;
                continue;
            }
            List<String[]> C = new ArrayList<>();
            C.add(Fee);
            CommissioniCollegate.Collega(Principali, C);
            E.Collegate++;
        }
        return E;
    }

    private static String ChiaveHash(String[] v) {
        return v[3] + "|" + v[24].trim();
    }

    /** Data dell'ID (al secondo) più exchange/wallet */
    private static String ChiaveIstante(String[] v) {
        return v[0].split("_")[0] + "|" + v[3];
    }

    /** @return l'istante del movimento in millisecondi, dalla data dell'ID; 0 se non leggibile */
    private static long Istante(String[] v) {
        try {
            return FunzioniDate.ConvertiDataIDinLong(v[0].split("_")[0]);
        } catch (Exception ex) {
            return 0;
        }
    }

    /**
     * Chiede conferma, esegue {@link #AbbinaArchivio()} e ne mostra l'esito.
     * @param owner finestra su cui centrare i dialoghi
     * @return {@code true} se almeno una commissione è stata collegata (il chiamante deve accendere Salva)
     */
    public static boolean AbbinaArchivioConConferma(Window owner) {
        AppDialog.DialogResult result = AppDialog.builder(owner)
                .windowTitle("Commissioni collegate")
                .bodyTitle("Collegare le commissioni dell'archivio ai loro movimenti?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message("Le commissioni importate prima di questa funzione non sanno a quale movimento appartengono.")
                .details("Una commissione viene collegata quando:<br>"
                        + " - ha lo stesso hash o id d'ordine di uno o più movimenti dello stesso wallet (il gas di "
                        + "una transazione va a tutti i movimenti della transazione), oppure<br>"
                        + " - nello stesso secondo e sullo stesso wallet c'è un solo movimento.<br>"
                        + "Se nello stesso secondo ci sono più movimenti la commissione viene lasciata com'è: "
                        + "si può collegare a mano dal menu contestuale.<br><br>"
                        + "Il collegamento è solo informativo e non cambia nessun calcolo. Le commissioni già "
                        + "collegate non vengono toccate, quindi l'operazione si può ripetere.")
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("abbina", "Collega le commissioni")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .showDialog();
        if (result == null || !result.isAction("abbina")) return false;

        Esito E = AbbinaArchivio();
        LoggerGC.logInfo("Commissioni collegate: " + E.Collegate + " collegate, " + E.GiaCollegate
                + " già collegate, " + E.Ambigue + " ambigue, " + E.SenzaMovimento + " senza movimento");
        String Dettaglio = "Commissioni collegate ora: <b>" + E.Collegate + "</b><br>"
                + "Già collegate in precedenza: " + E.GiaCollegate + "<br>"
                + "Lasciate com'erano perché nello stesso secondo ci sono più movimenti: " + E.Ambigue + "<br>"
                + "Senza nessun movimento a cui collegarle: " + E.SenzaMovimento;
        if (E.Collegate > 0) {
            Dettaglio += "<br><br>Premi <b>Salva</b> nella sezione 'Transazioni Crypto' per rendere permanente il collegamento.";
        }
        Messaggi.SuccessMessage("Commissioni collegate", Dettaglio, owner);
        return E.Collegate > 0;
    }

    // =================================================================================================
    // COLLEGAMENTO E SCOLLEGAMENTO MANUALI
    // =================================================================================================

    /** @return le righe in mappa degli ID indicati, senza doppioni e senza ID inesistenti */
    private static List<String[]> Righe(List<String> IDs) {
        List<String[]> Ris = new ArrayList<>();
        if (IDs == null) return Ris;
        Set<String> Visti = new LinkedHashSet<>();
        for (String ID : IDs) {
            String[] v = ID == null ? null : MappaCryptoWallet.get(ID);
            if (v != null && Visti.add(v[0])) Ris.add(v);
        }
        return Ris;
    }

    /**
     * La selezione si può collegare se contiene almeno una commissione e almeno un altro movimento,
     * nessuno dei due generato dalla classificazione ({@code AU}).
     */
    public static boolean isCollegabile(List<String> IDs) {
        boolean Fee = false, Principale = false;
        for (String[] v : Righe(IDs)) {
            if ("AU".equalsIgnoreCase(v[22])) return false;
            if (CommissioniCollegate.isCommissione(v)) Fee = true;
            else Principale = true;
        }
        return Fee && Principale;
    }

    /**
     * La selezione si può scollegare se almeno una riga ha una chiave di commissioni da togliere. La chiave di un
     * contratto Dual Investment sta in un altro campo ({@link GruppoOperazione}) e qui non si tocca.
     */
    public static boolean isScollegabile(List<String> IDs) {
        for (String[] v : Righe(IDs)) {
            if (!CommissioniCollegate.Chiave(v).isEmpty()) return true;
        }
        return false;
    }

    /**
     * Collega le commissioni selezionate agli altri movimenti selezionati. Se qualcuno di loro era già in
     * un gruppo, i gruppi vengono fusi: nessuna commissione resta collegata a metà.
     * @return {@code true} se il collegamento è stato fatto
     */
    public static boolean CollegaSelezione(List<String> IDs, Window owner) {
        if (!isCollegabile(IDs)) {
            Messaggi.WarningMessage("Selezione non collegabile",
                    "Per collegare servono almeno una commissione e almeno un altro movimento, "
                    + "nessuno dei quali generato automaticamente dalla classificazione.", owner);
            return false;
        }
        List<String[]> Principali = new ArrayList<>();
        List<String[]> Commissioni = new ArrayList<>();
        Set<String> GruppiEsistenti = new LinkedHashSet<>();
        for (String[] v : Righe(IDs)) {
            if (CommissioniCollegate.isCommissione(v)) Commissioni.add(v);
            else Principali.add(v);
            if (!CommissioniCollegate.Chiave(v).isEmpty()) GruppiEsistenti.add(CommissioniCollegate.Chiave(v));
        }

        String Dettaglio = Commissioni.size() == 1 ? "La commissione selezionata verrà collegata"
                : "Le " + Commissioni.size() + " commissioni selezionate verranno collegate";
        Dettaglio += Principali.size() == 1 ? " al movimento selezionato." : " ai " + Principali.size() + " movimenti selezionati.";
        if (GruppiEsistenti.size() > 1) {
            Dettaglio += "<br><br>Alcuni di questi movimenti erano già collegati ad altre commissioni: i gruppi "
                    + "verranno uniti in uno solo, comprese le commissioni non selezionate.";
        }
        Dettaglio += "<br><br>Il collegamento è solo informativo e non cambia nessun calcolo.";
        AppDialog.DialogResult result = AppDialog.builder(owner)
                .windowTitle("Collega commissioni")
                .bodyTitle("Collegare le commissioni ai movimenti?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message("Commissioni: " + Commissioni.size() + ", movimenti: " + Principali.size() + ".")
                .details(Dettaglio)
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("collega", "Collega")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .showDialog();
        if (result == null || !result.isAction("collega")) return false;

        CommissioniCollegate.Collega(Principali, Commissioni);
        return true;
    }

    /**
     * Toglie la chiave alle righe selezionate. Un gruppo che dopo lo scollegamento resta con sole
     * commissioni o con soli movimenti non collega più niente, e perde la chiave anch'esso: altrimenti
     * resterebbe un'annotazione che punta nel vuoto.
     * @return {@code true} se qualcosa è stato scollegato
     */
    public static boolean ScollegaSelezione(List<String> IDs, Window owner) {
        if (!isScollegabile(IDs)) return false;
        AppDialog.DialogResult result = AppDialog.builder(owner)
                .windowTitle("Scollega commissioni")
                .bodyTitle("Scollegare i movimenti selezionati dalle loro commissioni?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("I movimenti e le commissioni selezionati non saranno più collegati al loro gruppo.")
                .details("Se in un gruppo restano solo commissioni, o solo movimenti, anche quelli perdono il "
                        + "collegamento.<br><br>Il collegamento è solo informativo e non cambia nessun calcolo.")
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("scollega", "Scollega")
                        .role(AppDialog.ActionRole.DANGER)
                        .build())
                .showDialog();
        if (result == null || !result.isAction("scollega")) return false;
        Scollega(IDs);
        return true;
    }

    /** Parte operativa di {@link #ScollegaSelezione}, senza dialoghi. */
    static void Scollega(List<String> IDs) {
        Set<String> Chiavi = new LinkedHashSet<>();
        for (String[] v : Righe(IDs)) {
            String K = CommissioniCollegate.Chiave(v);
            if (K.isEmpty()) continue;
            Chiavi.add(K);
            v[CommissioniCollegate.CAMPO] = "";
        }
        for (String K : Chiavi) {
            boolean Fee = false, Principale = false;
            List<String[]> Membri = new ArrayList<>();
            for (String ID : CommissioniCollegate.Membri(K)) {
                String[] v = MappaCryptoWallet.get(ID);
                Membri.add(v);
                if (CommissioniCollegate.isCommissione(v)) Fee = true;
                else if (!"AU".equalsIgnoreCase(v[22])) Principale = true;
            }
            if (!(Fee && Principale)) {
                for (String[] v : Membri) v[CommissioniCollegate.CAMPO] = "";
            }
        }
    }

    // =================================================================================================
    // DETTAGLIO DEL MOVIMENTO
    // =================================================================================================

    /**
     * Le righe sulle commissioni da mostrare nel dettaglio di un movimento: per un movimento le sue commissioni, per una
     * commissione i movimenti a cui appartiene (ed eventuali altre commissioni dello stesso gruppo). L'operazione intera
     * la mostra prima {@link OperazioniCalcolate#RigheDettaglio}, che chiama questa.
     * @return coppie {etichetta, valore HTML}, vuoto se il movimento non è collegato
     */
    public static List<String[]> RigheDettaglio(String ID) {
        List<String[]> Ris = new ArrayList<>();
        String[] Mov = MappaCryptoWallet.get(ID);
        String K = CommissioniCollegate.Chiave(Mov);
        if (K.isEmpty()) return Ris;
        boolean SonoCommissione = CommissioniCollegate.isCommissione(Mov);
        StringBuilder Commissioni = new StringBuilder();
        StringBuilder Movimenti = new StringBuilder();
        for (String Altro : CommissioniCollegate.Membri(K)) {
            if (Altro.equalsIgnoreCase(ID)) continue;
            String[] v = MappaCryptoWallet.get(Altro);
            StringBuilder Dest = CommissioniCollegate.isCommissione(v) ? Commissioni : Movimenti;
            if (Dest.length() > 0) Dest.append("<br>");
            Dest.append(Descrizione(v));
        }
        if (SonoCommissione) {
            Ris.add(new String[]{"Commissione del movimento",
                Movimenti.length() > 0 ? "<html>" + Movimenti + "</html>" : "nessun movimento: collegamento rimasto orfano"});
            if (Commissioni.length() > 0) Ris.add(new String[]{"Altre commissioni dello stesso movimento", "<html>" + Commissioni + "</html>"});
        } else {
            if (Commissioni.length() > 0) Ris.add(new String[]{"Commissioni collegate", "<html>" + Commissioni + "</html>"});
            else Ris.add(new String[]{"Commissioni collegate", "nessuna: collegamento rimasto senza commissioni"});
            if (Movimenti.length() > 0) Ris.add(new String[]{"Movimenti con le stesse commissioni", "<html>" + Movimenti + "</html>"});
        }
        return Ris;
    }

    /** Una riga leggibile: quantità e moneta mosse, poi l'ID fra parentesi */
    static String Descrizione(String[] v) {
        StringBuilder S = new StringBuilder();
        if (!Funzioni.noData(v[8])) S.append(v[10]).append(" ").append(v[8]);
        if (!Funzioni.noData(v[11])) {
            if (S.length() > 0) S.append(" -> ");
            S.append(v[13]).append(" ").append(v[11]);
        }
        return "<b>" + S + "</b> (" + v[0] + ")";
    }
}
