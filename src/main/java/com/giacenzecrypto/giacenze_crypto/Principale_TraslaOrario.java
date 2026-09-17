package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.awt.Cursor;
import java.awt.Window;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Logica operativa della voce di menu contestuale "Trasla Orario": sposta avanti/indietro di N ore
 * (anche frazionarie) l'orario di uno o più movimenti selezionati, per correggere errori di fuso
 * orario commessi in importazione.
 *
 * <p>L'ID di un movimento comincia col suo timestamp ({@code yyyyMMddHHmmss_...}), quindi traslare
 * l'orario non è mai un aggiornamento in-place: equivale sempre a rimuovere il movimento e ricrearlo
 * con un ID nuovo, esattamente come fa {@code GUI_ModificaMovimento.ScriviMovimento()} (CASO A2)
 * quando l'utente cambia manualmente la data di un movimento dalla maschera di modifica. Questa
 * classe adatta lo stesso schema a un'operazione di massa dal popup, seguendo il pattern già
 * stabilito da {@link Principale_Movimenti_SeparaUnisci}.</p>
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

    //La blocklist dei campi da non ricopiare verbatim stava qui e ora vive in
    //MovimentiCrypto.CampiNonCopiabiliVerbatim, accanto a creaMovimento di cui descrive il contratto:
    //la condivide con GUI_ModificaMovimento, che prima ricopiava solo 5 campi scelti a mano (bug M10).
    //Due copie della stessa lista tornerebbero a divergere, ed è esattamente così che M10 è nato.

    /**
     * Punto di ingresso chiamato dal popup dei movimenti.
     *
     * @param IDs   ID dei movimenti selezionati (anche uno solo)
     * @param owner finestra su cui centrare i dialoghi
     * @return {@code true} se almeno un movimento è stato traslato (il chiamante deve aggiornare le tabelle)
     */
    public static boolean TraslaOrario(List<String> IDs, Window owner) {
        if (IDs == null || IDs.isEmpty()) return false;

        //I movimenti generati automaticamente (AU) non possono essere strutturalmente modificati,
        //stessa regola già applicata da GUI_ModificaMovimento e da
        //Principale_Movimenti_SeparaUnisci.VerificaSeparazione: vengono esclusi, non bloccano il resto
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

        int ConAbbinamenti = 0;
        for (String ID : Traslabili) {
            String Movimento[] = MappaCryptoWallet.get(ID);
            if (Movimento[20] != null && !Movimento[20].isBlank()) ConAbbinamenti++;
        }

        if (ConAbbinamenti > 0 && !ConfermaScioglimentoAbbinamenti(ConAbbinamenti, Esclusi, owner)) {
            return false;
        }

        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        int Traslati;
        try {
            Traslati = EseguiTraslazione(Traslabili, DeltaMillis);
        } finally {
            owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        if (Traslati == 0) return false;
        LoggerGC.logInfo("Trasla Orario: " + Traslati + " movimento/i traslato/i di " + Ore.toPlainString() + " ore");
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
     * Avvisa che alcuni dei movimenti traslabili fanno parte di un abbinamento e chiede conferma:
     * traslarli scioglie il gruppo (elimina i movimenti automatici collegati, ricalcola gli importi
     * dei residui), non si limita a "scollegarli".
     * @return {@code true} se l'utente conferma di proseguire
     */
    private static boolean ConfermaScioglimentoAbbinamenti(int ConAbbinamenti, int Esclusi, Window owner) {
        String DettaglioEsclusi = Esclusi > 0
                ? ("<br>" + Esclusi + " movimento/i selezionato/i " + (Esclusi == 1 ? "è stato escluso" : "sono stati esclusi")
                    + " perché generato/i automaticamente dal programma.")
                : "";

        AppDialog.DialogResult result = AppDialog.builder(owner)
                .windowTitle("Traslazione orario")
                .bodyTitle("Sciogliere gli abbinamenti e proseguire?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message(ConAbbinamenti + " dei movimenti selezionati fa" + (ConAbbinamenti == 1 ? "" : "nno") + " parte di un abbinamento.")
                .details("Traslando l'orario il gruppo verrà sciolto: gli eventuali movimenti automatici "
                        + "collegati (commissioni, reward) saranno eliminati e gli importi dei movimenti "
                        + "rimanenti del gruppo ricalcolati per compensare la rimozione, come già accade oggi "
                        + "riportando un movimento alla situazione iniziale." + DettaglioEsclusi + "<br>"
                        + "Si vuole proseguire?")
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("continua", "Sciogli e prosegui")
                        .role(AppDialog.ActionRole.DANGER)
                        .build())
                .showDialog();

        return result != null && result.isAction("continua");
    }

    /**
     * Esegue la traslazione sui movimenti indicati, senza alcuna interazione con l'utente: i controlli
     * di idoneità e le conferme sono già stati fatti dal chiamante ({@link #TraslaOrario}), o non
     * servono perché è un test a costruire direttamente la lista.
     * @param IDs         ID dei movimenti da traslare
     * @param DeltaMillis spostamento in millisecondi, sommato all'istante assoluto di ciascun movimento
     * @return il numero di movimenti effettivamente traslati
     */
    static int EseguiTraslazione(List<String> IDs, long DeltaMillis) {
        int Traslati = 0;
        for (String ID : IDs) {
            if (TraslaMovimento(ID, DeltaMillis)) Traslati++;
        }
        return Traslati;
    }

    /**
     * Trasla il singolo movimento del delta indicato. Rilegge sempre lo stato corrente della mappa
     * prima di agire: se l'ID non è più presente è stato già consumato dallo scioglimento del gruppo
     * di un movimento precedente nello stesso batch (es. una commissione/reward AU collegata).
     * @return {@code true} se il movimento è stato traslato
     */
    private static boolean TraslaMovimento(String ID, long DeltaMillis) {
        String mov[] = MappaCryptoWallet.get(ID);
        if (mov == null || NonTraslabile(mov)) return false;

        //Tengo il riferimento all'array (non l'ID) e lo passo a RimuoviMovimentazioneXID: se il
        //movimento fa parte di un abbinamento, la disassociazione lo muta in-place (azzera
        //[18]/[19]/[20]/[31], corregge [10]/[15] per le commissioni/reward riassorbite, e per lo
        //scambio differito con prefisso 00/04 riscrive anche mov[0] col suo ID ripristinato) prima di
        //rimuoverlo dalla mappa. Va chiamata sempre, non solo se [20] non è vuoto: è la stessa funzione
        //che GUI_ModificaMovimento usa per ogni modifica strutturale, e rimuove comunque l'entry a fine
        //chiamata anche quando non c'è nulla da disassociare.
        Funzioni.RimuoviMovimentazioneXID(ID);

        //Leggo lo stato DOPO la disassociazione, non prima: è quello che la traslazione deve
        //preservare. Leggerlo prima catturerebbe una classificazione/importo che la disassociazione
        //stessa sta per correggere, e il forzarli indietro più sotto vanificherebbe lo scioglimento
        //del gruppo appena fatto.
        String CategoriaOriginale = mov[0].split("_")[4];
        String Tipo = mov[5];
        String Sottotipo = mov[18];
        String Prezzo = mov[15];
        String FlagValorizzato = mov[32];
        String InfoPrezzo = mov[40];
        String IstanteOnChain = mov[29];

        String Segmenti[] = mov[0].split("_");
        if (Segmenti.length < 5) {
            MappaCryptoWallet.put(mov[0], mov);
            return false;
        }

        long NuovoMillis = FunzioniDate.ConvertiDataIDinLong(Segmenti[0]) + DeltaMillis;
        String NuovoTimestamp = FunzioniDate.FormattaDataID(NuovoMillis);
        String NuovoIDBase = NuovoTimestamp + "_" + Segmenti[1] + "_" + Segmenti[2] + "_" + Segmenti[3] + "_" + Segmenti[4];
        String NuovoID = MovimentiCrypto.getIDUnivoco(MappaCryptoWallet, NuovoIDBase);
        if (NuovoID == null) {
            MappaCryptoWallet.put(mov[0], mov);
            LoggerGC.logInfo("Trasla Orario: impossibile generare un ID univoco per " + NuovoIDBase + ", movimento lasciato invariato");
            return false;
        }

        String Rete = Funzioni.TrovaReteDaIMovimento(mov);
        Moneta MonetaOUT = CostruisciMoneta(mov[8], mov[9], mov[10], mov[25], mov[26], Rete);
        Moneta MonetaIN = CostruisciMoneta(mov[11], mov[12], mov[13], mov[27], mov[28], Rete);

        String RT[] = MovimentiCrypto.creaMovimento(
                MonetaOUT, MonetaIN,
                mov[3], mov[4],
                NuovoMillis,
                PrezzoSicuro(Prezzo), null,
                1, 1,
                NuovoID,
                mov[21],
                "M",
                mov[24],
                Tipo,
                null
        );

        if (RT == null) {
            MappaCryptoWallet.put(mov[0], mov);
            LoggerGC.logInfo("Trasla Orario: creazione del movimento traslato non riuscita per " + ID + ", lasciato invariato");
            return false;
        }

        //La traslazione oraria non deve mai alterare la classificazione del movimento: NuovoID
        //aveva già la categoria originale come 5° campo (verificato univoco sopra), creaMovimento
        //l'avrebbe però potuta ricalcolare in base al solo tipo delle monete.
        RT[0] = NuovoID;
        RT[5] = Tipo;
        RT[18] = Sottotipo;

        //Prezzo, valorizzazione e fonte riportati verbatim: creaMovimento arrotonderebbe il prezzo e
        //marcherebbe comunque la fonte come "Personalizzato" (stesso schema di AssegnaPrezzo in
        //Principale_Movimenti_SeparaUnisci)
        RT[15] = Prezzo;
        RT[32] = FlagValorizzato;
        RT[40] = InfoPrezzo;

        //Istante on-chain (DeFi): va traslato dello stesso delta, altrimenti resterebbe disallineato
        //dal nuovo [0]/[1] e la logica di abbinamento futura, che lo preferisce all'ID, vanificherebbe
        //la correzione appena fatta
        if (Funzioni.isNumeric(IstanteOnChain, false)) {
            RT[29] = String.valueOf(new BigDecimal(IstanteOnChain).longValueExact() + DeltaMillis);
        }

        //Il lignaggio si timbra sul movimento vecchio PRIMA del ciclo di ricopia: è il ciclo stesso a
        //portarlo sul movimento traslato (il campo non è nella blocklist proprio per questo), quindi
        //timbrarlo dopo lascerebbe il movimento nuovo senza catena. Sta qui e non più in alto perché
        //tutti i punti di rinuncia della traslazione sono ormai alle spalle: un movimento lasciato
        //invariato non deve ritrovarsi marcato come modificato.
        String Lignaggio = MovimentiStorico.AssicuraLignaggio(mov);

        //Questo ciclo deve restare DOPO tutte le assegnazioni esplicite sopra (0/5/18/15/32/40/29):
        //CampiNonCopiabiliVerbatim contiene già quegli indici quindi il ciclo non li tocca comunque, ma
        //l'ordine resta l'invariante da rispettare se in futuro uno di quei campi venisse tolto dalla
        //blocklist per errore.
        for (int Campo = 0; Campo < RT.length && Campo < mov.length; Campo++) {
            if (!MovimentiCrypto.CampiNonCopiabiliVerbatim.contains(Campo)) RT[Campo] = mov[Campo];
        }
        Importazioni.RiempiVuotiArray(RT);

        //Storico: anche la traslazione consuma un ID e ne crea un altro, quindi va registrata come una
        //modifica — ed è una funzione in cui vedere com'era il movimento prima serve almeno quanto in
        //Modifica Movimento.
        //`mov` qui è lo stato DOPO la disassociazione, e mov[0] l'ID realmente consumato: la
        //disassociazione può averlo riscritto (scambi differiti), quindi si legge adesso e non a monte.
        MovimentiStorico.AccodaModifica(Lignaggio, RT[0], mov[0], Importazioni.SerializzaRiga(mov), "TraslaOrario");

        MappaCryptoWallet.put(RT[0], RT);
        LoggerGC.logInfo("Movimento " + ID + " traslato in " + RT[0]);
        return true;
    }

    /**
     * Normalizza il prezzo da passare a {@code creaMovimento}: senza questo guard, un prezzo
     * numericamente zero ma scritto in una forma che {@link MovimentiCrypto#PrezzoPrezzato} rifiuta
     * (es. {@code "0"}, {@code "0E-8"}) farebbe cadere {@code creaMovimento} nella ricerca prezzo
     * online, sull'EDT, per ogni movimento del batch.
     */
    private static String PrezzoSicuro(String Prezzo) {
        return MovimentiCrypto.PrezzoPrezzato(Prezzo) ? Prezzo : "0.00";
    }

    /** @return la moneta da passare a {@code creaMovimento}, o {@code null} se simbolo/quantità non sono validi */
    private static Moneta CostruisciMoneta(String Simbolo, String Tipo, String Qta, String NomeEsteso, String Address, String Rete) {
        if (Simbolo == null || Simbolo.isBlank() || !Funzioni.isBigDecimalNonZero(Qta)) return null;
        Moneta Mon = new Moneta();
        Mon.Moneta = Simbolo;
        Mon.Tipo = Tipo;
        Mon.Qta = Qta; //il segno è già quello giusto nel movimento originale
        Mon.NomeEsteso = NomeEsteso;
        Mon.MonetaAddress = Address;
        Mon.Rete = Rete;
        return Mon;
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
