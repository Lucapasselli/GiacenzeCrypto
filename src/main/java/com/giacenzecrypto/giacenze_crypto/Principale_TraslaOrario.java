package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.awt.Cursor;
import java.awt.Window;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Logica operativa della voce di menu contestuale "Trasla Orario": sposta avanti/indietro di N ore
 * (anche frazionarie) l'orario di uno o più movimenti selezionati, per correggere errori di fuso
 * orario commessi in importazione.
 *
 * <p>L'ID di un movimento comincia col suo timestamp ({@code yyyyMMddHHmmss_...}), quindi traslare
 * l'orario non è mai un aggiornamento in-place: equivale sempre a rimuovere il movimento e ricrearlo
 * con un ID nuovo. A differenza di {@code GUI_ModificaMovimento.ScriviMovimento()} (CASO A2), che per
 * lo stesso motivo <b>scioglie</b> un eventuale abbinamento (campo {@code [20]}), Trasla Orario lo
 * <b>preserva</b>: una correzione di fuso orario non cambia la natura fiscale del movimento, quindi
 * non ha senso che ne cancelli la classificazione manuale.</p>
 *
 * <p>Il meccanismo non passa da una dissociazione (che cancellerebbe cose da ricostruire: elimina gli
 * eventuali movimenti AU collegati, applica la matematica compensativa di
 * {@code GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale}, consuma il backup-prezzo
 * campo {@code [35]}, toglie il prefisso {@code 00}/{@code 04} dagli scambi differiti). Il campo
 * {@code [20]} è già, su ogni membro di un gruppo abbinato, l'elenco completo degli altri ID
 * coinvolti: basta leggerlo, decidere chi si sposta con il movimento selezionato e chi resta fermo, e
 * clonare le righe interessate riscrivendo solo i campi che dipendono dal tempo.</p>
 *
 * <p><b>Chi si sposta con chi.</b> Selezionare un movimento trasla anche gli eventuali membri del suo
 * gruppo (campo {@code [20]}) che sono <b>marcati {@code [22]=="AU"}</b> (generati automaticamente dal
 * programma) <b>e</b> il cui segmento [0] dell'ID (il timestamp) coincide col suo originale — cioè i
 * movimenti generati a partire da lui (il mirror di un trasferimento a Vault/Collaterale, la coppia
 * mirror di {@code Binance_DualInvestment}, le tre gambe "01/02/03" di uno scambio differito). Il solo
 * timestamp uguale <b>non</b> basta: due gambe reali di un trasferimento tra wallet (PTW/DTW) nascono
 * con lo stesso timestamp perché fotografano lo stesso istante, senza per questo essere "generate a
 * partire" l'una dall'altra. Gli altri membri del gruppo (tipicamente la controparte reale su un altro
 * wallet/exchange, marcata {@code "A"} o {@code "M"}) restano fermi: viene solo riscritto il testo del
 * loro {@code [20]} per puntare ai nuovi ID. È una scelta deliberata: Trasla Orario corregge l'errore
 * di fuso di UNA fonte di importazione, e la scelta di trascinare anche la controparte (che potrebbe
 * già avere l'orario giusto) spetta all'utente, selezionandola esplicitamente.</p>
 *
 * <p>Lo spostamento è aritmetica sull'istante assoluto (si somma il delta ai millisecondi epoch e si
 * riformatta nel fuso Europe/Rome), non sull'ora civile: è la scelta corretta per correggere "il fuso
 * usato in importazione era sbagliato di N ore fisse" — un'aritmetica sulle sole cifre di ora
 * darebbe risultati diversi a cavallo dei cambi ora legale di marzo/ottobre.</p>
 *
 * @author lucap
 */
public class Principale_TraslaOrario {

    private Principale_TraslaOrario() {
    }

    /**
     * Campi che sono output del motore delle plusvalenze ({@code Calcoli_PlusvalenzeNew}), non
     * classificazione manuale: su un movimento tradotto vanno sempre svuotati, esattamente come
     * arriverebbero vuoti su un movimento nuovo (nato da {@code creaMovimento}+{@code RiempiVuotiArray})
     * — {@code AggiornaPlusvalenze} li ricalcola al prossimo giro, e portarsi dietro un valore stantio
     * dal movimento vecchio non è diverso dal non azzerarlo affatto. Il campo {@code [17]} non è qui
     * perché ha un doppio uso: per i DDO (donazioni) è costo di carico inserito a mano dall'utente, non
     * output del motore — va trattato caso per caso, vedi {@link #TraslaGruppo}.
     */
    private static final int[] CAMPI_MOTORE_DA_SVUOTARE = {16, 19, 31, 33, 38};

    /**
     * Punto di ingresso chiamato dal popup dei movimenti.
     *
     * @param IDs   ID dei movimenti selezionati (anche uno solo)
     * @param owner finestra su cui centrare i dialoghi
     * @return {@code true} se almeno un movimento è stato traslato (il chiamante deve aggiornare le tabelle)
     */
    public static boolean TraslaOrario(List<String> IDs, Window owner) {
        if (IDs == null || IDs.isEmpty()) return false;

        //I movimenti generati automaticamente (AU) non possono essere strutturalmente modificati come
        //capofila di una selezione (possono però muoversi come membri posseduti del gruppo di un altro
        //movimento, vedi TraslaGruppo): vengono esclusi qui, non bloccano il resto
        List<String> Traslabili = new ArrayList<>();
        int Esclusi = 0;
        for (String ID : IDs) {
            String Movimento[] = MappaCryptoWallet.get(ID);
            if (Movimento == null) continue;
            if (NonTraslabile(Movimento)) {
                Esclusi++;
                continue;
            }
            Traslabili.add(ID);
        }

        if (Traslabili.isEmpty()) {
            Messaggi.WarningMessage("Nessun movimento traslabile",
                    "Nessuno dei movimenti selezionati può essere traslato: i movimenti generati "
                    + "automaticamente dal programma non possono essere modificati strutturalmente.", owner);
            return false;
        }

        if (!ConfermaSeOrigineBlockchain(Traslabili, owner)) return false;

        BigDecimal Ore = ChiediOre(owner);
        if (Ore == null) return false;

        long DeltaMillis;
        try {
            DeltaMillis = Ore.multiply(BigDecimal.valueOf(3_600_000L)).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (ArithmeticException ex) {
            Messaggi.WarningMessage("Valore non valido", "Lo spostamento indicato è troppo grande.", owner);
            return false;
        }
        if (DeltaMillis == 0) {
            Messaggi.WarningMessage("Nessuno spostamento", "Il valore inserito corrisponde a uno spostamento nullo.", owner);
            return false;
        }

        boolean RicalcolaPrezzi = false;
        boolean AncheIPersonalizzati = false;
        AppDialog.DialogResult RisultatoRicalcolo = ChiediRicalcoloPrezzi(owner);
        if (RisultatoRicalcolo == null) return false; //annullato
        if (RisultatoRicalcolo.isAction("ricalcola")) {
            RicalcolaPrezzi = true;
            AppDialog.DialogResult RisultatoPersonalizzati = ChiediIncludiPersonalizzati(owner);
            if (RisultatoPersonalizzati == null) return false; //annullato
            AncheIPersonalizzati = RisultatoPersonalizzati.isAction("includi");
        }

        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Map<String, String> Traslati;
        try {
            Traslati = EseguiTraslazioneConMappa(Traslabili, DeltaMillis);
        } finally {
            owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        if (Traslati.isEmpty()) return false;
        LoggerGC.logInfo("Trasla Orario: " + Traslati.size() + " movimento/i traslato/i di " + Ore.toPlainString() + " ore");

        if (RicalcolaPrezzi) {
            RicalcolaPrezziMovimenti(Traslati.values(), AncheIPersonalizzati, owner);
        }

        return true;
    }

    /** Chiede all'utente di quante ore traslare i movimenti selezionati. @return il valore inserito, {@code null} se annullato o non valido */
    private static BigDecimal ChiediOre(Window owner) {
        AppDialog.DialogResult result = AppDialog.builder(owner)
                .windowTitle("Trasla Orario")
                .bodyTitle("Di quante ore traslare i movimenti selezionati?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message("Indica lo spostamento da applicare all'orario dei movimenti selezionati.")
                .details("Positivo per spostare avanti nel tempo, negativo per spostare indietro. "
                        + "Sono ammessi valori decimali (es. 5.5 per cinque ore e trenta minuti).")
                .inputField("Ore da traslare", "0")
                .inputColumns(10)
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("confirm", "Conferma")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .showDialog();

        if (result == null || !result.isAction("confirm")) return null;
        String Valore = result.getInputValue();
        if (Valore == null) return null;
        Valore = Valore.replace(",", ".").trim();
        if (!Funzioni.isNumeric(Valore, false)) {
            Messaggi.WarningMessage("Valore non valido", "Il valore inserito non è un numero valido.", owner);
            return null;
        }
        return new BigDecimal(Valore);
    }

    /**
     * Se almeno uno dei movimenti traslabili proviene da una lettura diretta della blockchain (chain
     * riconosciuta da {@link Funzioni#TrovaReteDaIMovimento}, cioè scaricato da
     * {@code TransazioneDefi}/{@code Trans_Solana}/{@code Trans_Bitcoin}), avvisa che il suo orario è
     * quasi certamente già quello corretto registrato sul blocco e chiede conferma prima di procedere.
     * @return {@code true} se si può proseguire (nessun movimento on-chain coinvolto, o l'utente ha confermato)
     */
    private static boolean ConfermaSeOrigineBlockchain(List<String> Traslabili, Window owner) {
        int DaBlockchain = 0;
        for (String ID : Traslabili) {
            String Movimento[] = MappaCryptoWallet.get(ID);
            if (Movimento != null && Funzioni.TrovaReteDaIMovimento(Movimento) != null
                    && !Funzioni.TrovaReteDaIMovimento(Movimento).isBlank()) {
                DaBlockchain++;
            }
        }
        if (DaBlockchain == 0) return true;

        AppDialog.DialogResult result = AppDialog.builder(owner)
                .windowTitle("Traslazione orario")
                .bodyTitle("Movimenti provenienti dalla blockchain")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message(DaBlockchain + " dei movimenti selezionati " + (DaBlockchain == 1 ? "proviene" : "provengono")
                        + " da una lettura diretta della blockchain.")
                .details("Per questi movimenti l'orario registrato è quello del blocco: è quasi certamente già "
                        + "corretto, e traslarlo introdurrebbe uno scarto rispetto alla realtà on-chain invece di "
                        + "correggerlo. Si vuole proseguire comunque?")
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("continua", "Prosegui comunque")
                        .role(AppDialog.ActionRole.DANGER)
                        .build())
                .showDialog();

        return result != null && result.isAction("continua");
    }

    /** Chiede se ricalcolare i prezzi dei movimenti traslati dopo lo spostamento. @return il risultato del dialogo, {@code null} se annullato */
    private static AppDialog.DialogResult ChiediRicalcoloPrezzi(Window owner) {
        return AppDialog.builder(owner)
                .windowTitle("Traslazione orario")
                .bodyTitle("Ricalcolare anche i prezzi?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .message("Lo spostamento dell'orario cambia l'istante a cui viene cercato il prezzo del movimento.")
                .details("Se si sceglie di non ricalcolare, il prezzo attuale del movimento resta invariato "
                        + "(riportato verbatim, come oggi). Se si sceglie di ricalcolare, per i soli movimenti "
                        + "effettivamente selezionati (non per gli eventuali movimenti collegati traslati insieme) "
                        + "viene cercato un nuovo prezzo al nuovo orario.")
                .action(AppDialog.DialogAction.builder("mantieni", "No, mantieni i prezzi attuali")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("ricalcola", "Sì, ricalcola")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build())
                .showDialog();
    }

    /** Chiede se includere anche i movimenti con prezzo "Personalizzato" nel ricalcolo. @return il risultato del dialogo, {@code null} se annullato */
    private static AppDialog.DialogResult ChiediIncludiPersonalizzati(Window owner) {
        return AppDialog.builder(owner)
                .windowTitle("Traslazione orario")
                .bodyTitle("Includere anche i prezzi inseriti a mano?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("Alcuni dei movimenti selezionati potrebbero avere un prezzo con fonte \"Personalizzato\".")
                .details("Il prezzario personale è associato all'orario esatto in cui è stato inserito, quindi non "
                        + "\"segue\" il movimento al nuovo orario: includerlo nel ricalcolo non ritrova lo stesso "
                        + "prezzo personale, lo <b>sostituisce</b> con un nuovo prezzo di mercato cercato al nuovo "
                        + "istante. Escludendolo, i prezzi personalizzati restano quelli inseriti a mano.")
                .action(AppDialog.DialogAction.builder("escludi", "No, lascia invariati i personalizzati")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("includi", "Sì, ricalcola anche quelli")
                        .role(AppDialog.ActionRole.DANGER)
                        .build())
                .showDialog();
    }

    /**
     * Esegue la traslazione sui movimenti indicati, senza alcuna interazione con l'utente: i controlli
     * di idoneità e le conferme sono già stati fatti dal chiamante ({@link #TraslaOrario}), o non
     * servono perché è un test a costruire direttamente la lista.
     * @param IDs         ID dei movimenti da traslare
     * @param DeltaMillis spostamento in millisecondi, sommato all'istante assoluto di ciascun movimento
     * @return il numero di movimenti effettivamente traslati (i "capofila" selezionati; gli eventuali
     *         membri del gruppo trascinati con loro non sono contati separatamente)
     */
    static int EseguiTraslazione(List<String> IDs, long DeltaMillis) {
        return EseguiTraslazioneConMappa(IDs, DeltaMillis).size();
    }

    /**
     * Come {@link #EseguiTraslazione}, ma restituisce anche il nuovo ID assegnato a ciascun capofila
     * traslato, necessario a {@link #TraslaOrario} per sapere quali righe sottoporre a un eventuale
     * ricalcolo prezzi.
     * @return mappa vecchio ID → nuovo ID dei soli movimenti capofila effettivamente traslati
     */
    private static Map<String, String> EseguiTraslazioneConMappa(List<String> IDs, long DeltaMillis) {
        Map<String, String> VecchioANuovoCapofila = new LinkedHashMap<>();
        for (String ID : IDs) {
            String NuovoID = TraslaGruppo(ID, DeltaMillis);
            if (NuovoID != null) VecchioANuovoCapofila.put(ID, NuovoID);
        }
        return VecchioANuovoCapofila;
    }

    /**
     * Trasla il gruppo "posseduto" dal movimento {@code ID}: {@code ID} stesso, più gli eventuali
     * membri del suo campo {@code [20]} marcati {@code [22]=="AU"} il cui segmento [0] dell'ID (il
     * timestamp) coincide col suo — vedi il javadoc di classe per l'elenco dei casi. Gli altri membri
     * del gruppo (se presenti) restano fermi: viene solo riscritto il testo del loro {@code [20]} per
     * puntare ai nuovi ID.
     * @return il nuovo ID assegnato a {@code ID}, o {@code null} se non è stato possibile traslarlo
     *         (già consumato da un passo precedente dello stesso batch, non più in mappa, non
     *         traslabile, o impossibile generare un ID univoco per uno dei membri posseduti)
     */
    private static String TraslaGruppo(String ID, long DeltaMillis) {
        String Capofila[] = MappaCryptoWallet.get(ID);
        if (Capofila == null || NonTraslabile(Capofila)) return null;

        String TimestampOriginale = ID.split("_")[0];

        //Gruppo completo, letto fresco dalla mappa: ID stesso più tutti gli ID citati nel suo [20].
        //Leggerlo qui (non da uno snapshot preso a inizio batch) è ciò che permette a un batch che
        //seleziona più gambe collegate di autocorreggersi: se una gamba è già stata traslata da un
        //passo precedente dello stesso batch, il [20] letto ora la vede già con l'ID nuovo
        Set<String> Gruppo = new LinkedHashSet<>();
        Gruppo.add(ID);
        if (Capofila[20] != null && !Capofila[20].isBlank()) {
            for (String Membro : Capofila[20].split(",")) {
                if (!Membro.isBlank()) Gruppo.add(Membro);
            }
        }

        List<String> MembriDaTraslare = new ArrayList<>();
        List<String> MembriEsterni = new ArrayList<>();
        for (String Membro : Gruppo) {
            if (Membro.equals(ID)) {
                MembriDaTraslare.add(Membro);
                continue;
            }
            //Lo stesso timestamp da solo non basta: due gambe reali di un trasferimento tra wallet
            //(PTW/DTW) nascono con lo stesso timestamp perché fotografano lo stesso istante, ma non
            //per questo sono "generate a partire" dal movimento selezionato — sono la controparte su
            //un altro wallet, che per design deve restare ferma. Il marcatore certo è [22]=="AU"
            //(vedi il javadoc di classe): solo un membro AU nato con quel timestamp è davvero un
            //movimento derivato dal capofila.
            String Segmenti[] = Membro.split("_");
            String RigaMembro[] = MappaCryptoWallet.get(Membro);
            boolean IsAU = RigaMembro != null && RigaMembro.length > 22
                    && RigaMembro[22] != null && RigaMembro[22].equalsIgnoreCase("AU");
            if (IsAU && Segmenti.length > 0 && Segmenti[0].equals(TimestampOriginale)) {
                MembriDaTraslare.add(Membro);
            } else {
                MembriEsterni.add(Membro);
            }
        }

        Map<String, String> VecchioANuovo = new LinkedHashMap<>();
        List<String[]> RigheTradotte = new ArrayList<>();
        for (String Membro : MembriDaTraslare) {
            String Riga[] = MappaCryptoWallet.get(Membro);
            if (Riga == null) continue; //già consumato da un passo precedente dello stesso batch

            String Segmenti[] = Riga[0].split("_");
            if (Segmenti.length < 5) continue; //ID malformato, non traslabile

            long NuovoMillis = FunzioniDate.ConvertiDataIDinLong(Segmenti[0]) + DeltaMillis;
            String NuovoTimestamp = FunzioniDate.FormattaDataID(NuovoMillis);
            String NuovoIDBase = NuovoTimestamp + "_" + Segmenti[1] + "_" + Segmenti[2] + "_" + Segmenti[3] + "_" + Segmenti[4];
            String NuovoID = MovimentiCrypto.getIDUnivoco(MappaCryptoWallet, NuovoIDBase);
            if (NuovoID == null) {
                //Rinuncio sull'intero gruppo: spostarne solo una parte lascerebbe [20] incoerente
                LoggerGC.logInfo("Trasla Orario: impossibile generare un ID univoco per " + NuovoIDBase + ", gruppo di " + ID + " lasciato invariato");
                return null;
            }

            //Il lignaggio si timbra sull'array vecchio PRIMA del clone, cosi' il clone lo eredita
            //automaticamente e la riga originale (quella serializzata subito sotto come "come era
            //prima") lo porta gia' con se'
            String Lignaggio = MovimentiStorico.AssicuraLignaggio(Riga);

            //Clone verbatim: a differenza di una ricostruzione via MovimentiCrypto.creaMovimento, non
            //serve alcuna blocklist di campi da "non dimenticare" — moneta/quantità/wallet/prezzo non
            //cambiano mai in una traslazione, solo i campi elencati sotto dipendono dal tempo
            String RigaTradotta[] = Riga.clone();
            RigaTradotta[0] = NuovoID;
            String DataFormattata = FunzioniDate.ConvertiDatadaLongAlSecondo(NuovoMillis).trim();
            RigaTradotta[1] = DataFormattata.substring(0, DataFormattata.length() - 3);
            if (Funzioni.isNumeric(RigaTradotta[29], false)) {
                RigaTradotta[29] = String.valueOf(new BigDecimal(RigaTradotta[29]).longValueExact() + DeltaMillis);
            }

            for (int Campo : CAMPI_MOTORE_DA_SVUOTARE) {
                if (Campo < RigaTradotta.length) RigaTradotta[Campo] = "";
            }
            //Campo 17: output del motore per la generalità dei movimenti, ma per i DDO è il costo di
            //carico della donazione inserito a mano — in quel caso è classificazione manuale, non va
            //svuotato. Campo 35 (backup prezzo della classificazione manuale) resta sempre verbatim:
            //il clone lo ha già ricopiato, nessuna azione da fare
            boolean IsDDO = RigaTradotta.length > 18 && RigaTradotta[18] != null && RigaTradotta[18].contains("DDO");
            if (!IsDDO && RigaTradotta.length > 17) RigaTradotta[17] = "";

            MovimentiStorico.AccodaModifica(Lignaggio, NuovoID, Riga[0], Importazioni.SerializzaRiga(Riga), "TraslaOrario");

            VecchioANuovo.put(Membro, NuovoID);
            RigheTradotte.add(RigaTradotta);
        }

        if (RigheTradotte.isEmpty()) return null; //nulla da spostare: il capofila stesso era già consumato

        //Rimappo [20] sulle righe appena tradotte, sostituendo gli ID vecchi dei membri posseduti con
        //quelli nuovi; i riferimenti ai membri esterni restano invariati nel testo
        for (String[] RigaTradotta : RigheTradotte) {
            RigaTradotta[20] = RimappaListaID(RigaTradotta[20], VecchioANuovo);
        }

        for (String VecchioID : VecchioANuovo.keySet()) {
            MappaCryptoWallet.remove(VecchioID);
        }
        for (String[] RigaTradotta : RigheTradotte) {
            MappaCryptoWallet.put(RigaTradotta[0], RigaTradotta);
        }

        //Membri esterni non toccati: se il loro [20] cita uno degli ID appena traslati, il riferimento
        //va aggiornato sul posto, senza spostare la riga
        for (String Esterno : MembriEsterni) {
            String RigaEsterna[] = MappaCryptoWallet.get(Esterno);
            if (RigaEsterna == null || RigaEsterna.length <= 20) continue;
            String Rimappato = RimappaListaID(RigaEsterna[20], VecchioANuovo);
            if (!Rimappato.equals(RigaEsterna[20])) RigaEsterna[20] = Rimappato;
        }

        String NuovoIDCapofila = VecchioANuovo.get(ID);
        if (NuovoIDCapofila != null) {
            String Messaggio = "Movimento " + ID + " traslato in " + NuovoIDCapofila;
            if (RigheTradotte.size() > 1) Messaggio += " (con " + (RigheTradotte.size() - 1) + " movimento/i collegato/i)";
            LoggerGC.logInfo(Messaggio);
        }
        return NuovoIDCapofila;
    }

    /**
     * Sostituisce, dentro una lista di ID separati da virgola (il formato del campo {@code [20]}), gli
     * ID presenti come chiave in {@code VecchioANuovo} con il rispettivo valore. Gli ID non presenti
     * nella mappa (membri esterni, non traslati) restano invariati.
     */
    private static String RimappaListaID(String Lista, Map<String, String> VecchioANuovo) {
        if (Lista == null || Lista.isBlank()) return Lista == null ? "" : Lista;
        String Parti[] = Lista.split(",");
        StringBuilder Nuova = new StringBuilder();
        for (String Parte : Parti) {
            if (Nuova.length() > 0) Nuova.append(",");
            Nuova.append(VecchioANuovo.getOrDefault(Parte, Parte));
        }
        return Nuova.toString();
    }

    /**
     * Ricalcola il prezzo dei movimenti indicati al loro nuovo orario, sostituendo quello attuale.
     * Esegue in background (può fare I/O di rete) mostrando una finestra di avanzamento, sullo stesso
     * schema di {@code Principale.Opzioni_Varie_RicalcolaPrezziActionPerformed}.
     * @param NuoviID              ID (già traslati) dei movimenti su cui ricalcolare il prezzo
     * @param AncheIPersonalizzati se {@code false}, i movimenti con fonte prezzo "Personalizzato" vengono saltati
     */
    private static void RicalcolaPrezziMovimenti(java.util.Collection<String> NuoviID, boolean AncheIPersonalizzati, Window owner) {
        List<String[]> DaRiprezzare = new ArrayList<>();
        for (String ID : NuoviID) {
            String Riga[] = MappaCryptoWallet.get(ID);
            if (Riga == null) continue;
            if (!AncheIPersonalizzati && Riga.length > 40 && "Personalizzato".equals(new Prezzi.InfoPrezzo(Riga[40]).Fonte)) {
                continue;
            }
            DaRiprezzare.add(Riga);
        }
        if (DaRiprezzare.isEmpty()) return;

        Download progress = new Download();
        progress.setLocationRelativeTo(owner);

        Thread thread = new Thread() {
            @Override
            public void run() {
                progress.Titolo("Ricalcolo prezzi in corso....");
                Prezzi.PreScaricaPrezzi(DaRiprezzare, 0, progress);

                progress.SetLabel("Ricalcolo prezzi in corso....");
                progress.SetMassimo(DaRiprezzare.size());
                int RigheModificate = 0;
                int i = 0;
                for (String[] Riga : DaRiprezzare) {
                    i++;
                    progress.SetAvanzamento(i);
                    String NuovoPrezzo = Prezzi.DammiPrezzoDaTransazione(Riga, 2);
                    if (NuovoPrezzo != null && !NuovoPrezzo.equals("0.00") && !NuovoPrezzo.equals(Riga[15])) {
                        Riga[15] = NuovoPrezzo;
                        //Stessa convenzione della "Ricalcola prezzi" globale: si svuota, non si scrive "SI",
                        //lasciando a Prezzi.isMovimentoPrezzato la decisione al prossimo caricamento tabella
                        Riga[32] = "";
                        RigheModificate++;
                    }
                }
                Messaggi.SuccessMessage("Ricalcolo prezzi completato",
                        "Sono stati aggiornati <b>" + RigheModificate + "</b> prezzi su " + DaRiprezzare.size() + " movimenti traslati.", progress);
                progress.ChiudiFinestra();
            }
        };
        thread.start();
        progress.setVisible(true);
    }

    /**
     * @return {@code true} se il movimento non può essere traslato strutturalmente: ID malformato, o
     *         generato automaticamente dal programma (stessa regola di {@code GUI_ModificaMovimento} e
     *         di {@code Principale_Movimenti_SeparaUnisci.VerificaSeparazione})
     */
    private static boolean NonTraslabile(String Movimento[]) {
        return Movimento[0].split("_").length < 5 || (Movimento[22] != null && Movimento[22].equalsIgnoreCase("AU"));
    }
}
