/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.awt.Window;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Logica operativa delle due voci del menu contestuale che trasformano la struttura di un movimento:
 * <ul>
 *   <li><b>Separa in Deposito/Prelievo</b> — spezza un movimento che coinvolge due monete nelle sue due
 *       gambe indipendenti (un prelievo per la moneta in uscita, un deposito per quella in entrata) ed
 *       elimina l'originale;</li>
 *   <li><b>Crea movimento di scambio da Deposito/Prelievo</b> — l'operazione inversa: fonde un deposito e
 *       un prelievo non classificati, contemporanei e sullo stesso wallet, in un unico movimento di
 *       scambio/acquisto/vendita ed elimina i due originali.</li>
 * </ul>
 *
 * <p>Entrambe le operazioni delegano la costruzione dei nuovi movimenti a
 * {@link MovimentiCrypto#creaMovimento}, così campo 5 (descrizione), campo 18 (sottotipo) e categoria
 * (ultimo segmento dell'ID) vengono ricavati dal solo tipo delle monete coinvolte, seguendo la tabella
 * di fallback documentata in {@code Documentazione/Analisi_Campo5_Campo18_Categoria.md}: nessuna
 * combinazione nuova viene introdotta e i casi FIAT/NFT sono gestiti automaticamente.</p>
 *
 * <p>Il prezzo viene sempre passato esplicitamente a {@code creaMovimento}, in modo che non venga mai
 * tentata una ricerca prezzi online al suo interno: le eventuali ricerche sono decise e fatte qui, in
 * {@link #PreparaGambeSeparazione}, e mai a sorpresa.</p>
 *
 * <p>La separazione è divisa in tre passi, così che il chiamante possa metterne solo uno in background:
 * {@link #VerificaSeparazione} (controlli preliminari e dati per il dialogo di conferma),
 * {@link #PreparaGambeSeparazione} (costruzione delle gambe e ricerca dei prezzi, <b>fuori dall'EDT</b>) e
 * {@link #ApplicaSeparazione} (sostituzione in mappa, <b>sull'EDT</b>).</p>
 *
 * <p>Come per le altre classi {@code Principale_*}, i metodi sono statici e la classe non apre finestre né
 * conserva riferimenti a componenti Swing: i problemi vengono restituiti come {@link Esito} e sono
 * {@link Principale} e le altre classi di GUI a mostrarli, così come l'aggiornamento delle tabelle e la
 * mascherina di attesa durante la ricerca dei prezzi.</p>
 *
 * @author lucap
 */
public class Principale_Movimenti_SeparaUnisci {

    /**
     * Campi che {@link MovimentiCrypto#creaMovimento} non popola e che descrivono la provenienza del
     * movimento (progressivo, causale originale, blocco, address controparte/provenienza/destinazione,
     * fonte dati...): vanno riportati sui movimenti generati per non perdere informazioni.
     * Sono volutamente esclusi i campi calcolati dai motori di calcolo (16/17 costo di carico,
     * 19/33 plusvalenza, 38 flag di anomalia) e il campo 35 (backup temporaneo del prezzo usato
     * dalla classificazione manuale).
     */
    private static final int[] CampiDaRiportare = {2, 7, 14, 23, 30, 31, 36, 37, 39};

    // =================================================================================================
    // ABILITAZIONE DELLE VOCI DI MENU
    // =================================================================================================

    /**
     * Verifica se un movimento può essere separato in deposito + prelievo, ovvero se coinvolge
     * effettivamente due monete (simbolo valorizzato e quantità diversa da zero su entrambi i lati).
     * @param ID ID del movimento da valutare, può essere {@code null}
     * @return {@code true} se la voce "Separa in Deposito/Prelievo" va abilitata
     */
    public static boolean isSeparabileInDepositoPrelievo(String ID) {
        if (ID == null) return false;
        String Movimento[] = MappaCryptoWallet.get(ID);
        if (Movimento == null) return false;
        if (ID.split("_").length < 5) return false;
        return DueMoneteCoinvolte(Movimento);
    }

    /**
     * Verifica se i movimenti selezionati possono essere fusi in un unico movimento di scambio: devono
     * essere esattamente due, uno di deposito e uno di prelievo, entrambi non ancora classificati
     * (campo 18 vuoto e non generati automaticamente), sullo stesso wallet e contemporanei al secondo.
     * Le tipologie di token possono essere diverse tra loro (Crypto/FIAT/NFT in qualunque combinazione).
     * @param IDs lista degli ID attualmente selezionati in tabella, può essere {@code null}
     * @return {@code true} se la voce "Crea movimento di scambio da Deposito/Prelievo" va abilitata
     */
    public static boolean isUnibileInScambio(List<String> IDs) {
        return TrovaCoppiaDepositoPrelievo(IDs) != null;
    }

    /**
     * Individua, tra gli ID selezionati, la coppia deposito/prelievo fondibile in uno scambio.
     * @param IDs lista degli ID selezionati
     * @return array {@code [movimento di prelievo, movimento di deposito]}, oppure {@code null} se la
     *         selezione non soddisfa tutte le condizioni
     */
    static String[][] TrovaCoppiaDepositoPrelievo(List<String> IDs) {
        if (IDs == null || IDs.size() != 2) return null;

        String Primo[] = MappaCryptoWallet.get(IDs.get(0));
        String Secondo[] = MappaCryptoWallet.get(IDs.get(1));
        if (Primo == null || Secondo == null) return null;
        if (Primo[0].equalsIgnoreCase(Secondo[0])) return null;

        //Entrambi devono essere depositi/prelievi ancora da classificare (FIAT compresi)
        if (!MovimentoNonClassificato(Primo) || !MovimentoNonClassificato(Secondo)) return null;

        //Uno dei due deve essere il prelievo e l'altro il deposito
        String Prelievo, Deposito;
        if (isPrelievo(Primo) && isDeposito(Secondo)) {
            Prelievo = Primo[0];
            Deposito = Secondo[0];
        } else if (isPrelievo(Secondo) && isDeposito(Primo)) {
            Prelievo = Secondo[0];
            Deposito = Primo[0];
        } else return null;

        String MovPrelievo[] = MappaCryptoWallet.get(Prelievo);
        String MovDeposito[] = MappaCryptoWallet.get(Deposito);

        //Il prelievo deve avere una moneta in uscita e il deposito una moneta in entrata
        if (!MonetaValida(MovPrelievo[8], MovPrelievo[10])) return null;
        if (!MonetaValida(MovDeposito[11], MovDeposito[13])) return null;

        //Stesso wallet
        if (MovPrelievo[3] == null || !MovPrelievo[3].trim().equalsIgnoreCase(MovDeposito[3].trim())) return null;

        //Stesso istante (timestamp oppure data dell'ID, entrambi al secondo)
        if (!StessoIstante(MovPrelievo, MovDeposito)) return null;

        return new String[][]{MovPrelievo, MovDeposito};
    }

    /** @return {@code true} se il movimento ha simbolo e quantità validi sia in uscita che in entrata */
    private static boolean DueMoneteCoinvolte(String Movimento[]) {
        return MonetaValida(Movimento[8], Movimento[10]) && MonetaValida(Movimento[11], Movimento[13]);
    }

    /** @return {@code true} se simbolo e quantità individuano una moneta realmente movimentata */
    private static boolean MonetaValida(String Simbolo, String Qta) {
        return Simbolo != null && !Simbolo.isBlank() && Funzioni.isBigDecimalNonZero(Qta);
    }

    /**
     * @return {@code true} se il movimento è un deposito/prelievo (anche FIAT) non ancora classificato,
     *         secondo la stessa definizione usata dalla tabella "Depositi e Prelievi"
     */
    private static boolean MovimentoNonClassificato(String Movimento[]) {
        return Funzioni.isDepositoPrelievoClassificabile(null, Movimento, true)
                && Movimento[18] != null && Movimento[18].isBlank();
    }

    /** @return {@code true} se la categoria del movimento è di prelievo (PC o PF) */
    private static boolean isPrelievo(String Movimento[]) {
        String Categoria = Categoria(Movimento);
        return Categoria.equalsIgnoreCase("PC") || Categoria.equalsIgnoreCase("PF");
    }

    /** @return {@code true} se la categoria del movimento è di deposito (DC o DF) */
    private static boolean isDeposito(String Movimento[]) {
        String Categoria = Categoria(Movimento);
        return Categoria.equalsIgnoreCase("DC") || Categoria.equalsIgnoreCase("DF");
    }

    /** @return l'ultimo segmento dell'ID (la categoria del movimento), oppure stringa vuota se l'ID è malformato */
    private static String Categoria(String Movimento[]) {
        String Parti[] = Movimento[0].split("_");
        if (Parti.length < 5) return "";
        return Parti[4].trim();
    }

    /**
     * Confronta due movimenti al secondo: prima sui timestamp (campo 29) se entrambi valorizzati,
     * altrimenti sulla data contenuta nel primo segmento dell'ID (formato {@code yyyyMMddHHmmss}).
     * @return {@code true} se i due movimenti sono contemporanei
     */
    private static boolean StessoIstante(String Movimento1[], String Movimento2[]) {
        if (Funzioni.isBigDecimalNonZero(Movimento1[29]) && Funzioni.isBigDecimalNonZero(Movimento2[29])
                && Movimento1[29].trim().equals(Movimento2[29].trim())) {
            return true;
        }
        return Movimento1[0].split("_")[0].equals(Movimento2[0].split("_")[0]);
    }

    // =================================================================================================
    // FUNZIONE 1 - SEPARA IN DEPOSITO/PRELIEVO
    // =================================================================================================

    /**
     * Esito di una delle fasi della separazione. La classe non apre finestre: si limita a descrivere il
     * problema e lascia alla GUI il compito di mostrarlo.
     */
    public static final class Esito {

        /** {@code true} se la fase è andata a buon fine. */
        public final boolean Riuscito;
        /** Titolo del messaggio da mostrare all'utente, {@code null} se la fase è riuscita. */
        public final String Titolo;
        /** Testo del messaggio da mostrare all'utente (HTML, come gli altri messaggi del programma). */
        public final String Messaggio;

        private Esito(boolean Riuscito, String Titolo, String Messaggio) {
            this.Riuscito = Riuscito;
            this.Titolo = Titolo;
            this.Messaggio = Messaggio;
        }

        private static Esito Ok() {
            return new Esito(true, null, null);
        }

        private static Esito Errore(String Titolo, String Messaggio) {
            return new Esito(false, Titolo, Messaggio);
        }
    }

    /**
     * Stato di una separazione in corso: raccoglie l'esito dei controlli preliminari, i dati necessari a
     * comporre il dialogo di conferma e, dopo {@link #PreparaGambeSeparazione}, le due gambe già prezzate
     * e pronte per essere inserite in mappa.
     */
    public static final class Separazione {

        /** Movimento da separare, {@code null} se i controlli preliminari non sono stati superati. */
        private final String Originale[];
        /** Esito dei controlli preliminari. */
        public final Esito Controlli;
        /** Descrizione del movimento (campo 5), da mostrare nel dialogo di conferma. */
        public final String Descrizione;
        /** Nome della moneta in uscita, da mostrare nel dialogo di conferma. */
        public final String MonetaUscita;
        /** Nome della moneta in entrata, da mostrare nel dialogo di conferma. */
        public final String MonetaEntrata;

        private String Prelievo[];
        private String Deposito[];

        private Separazione(String Originale[], Esito Controlli) {
            this.Originale = Originale;
            this.Controlli = Controlli;
            this.Descrizione = Originale == null ? "" : Originale[5];
            this.MonetaUscita = Originale == null ? "" : NomeVisualizzato(Originale[8], Originale[25]);
            this.MonetaEntrata = Originale == null ? "" : NomeVisualizzato(Originale[11], Originale[27]);
        }

        /** @return {@code true} se il movimento ha superato i controlli preliminari e può essere separato */
        public boolean isSeparabile() {
            return Controlli.Riuscito;
        }
    }

    /**
     * Passo 1 della separazione: verifica che il movimento possa essere separato e raccoglie i dati per il
     * dialogo di conferma. Non modifica nulla e non fa alcuna ricerca prezzi, quindi è invocabile
     * direttamente sull'EDT.
     *
     * @param ID ID del movimento da separare
     * @return lo stato della separazione; se {@link Separazione#isSeparabile()} è {@code false},
     *         {@link Separazione#Controlli} contiene il messaggio da mostrare all'utente
     */
    public static Separazione VerificaSeparazione(String ID) {
        String Movimento[] = ID == null ? null : MappaCryptoWallet.get(ID);
        if (Movimento == null) {
            return new Separazione(null, Esito.Errore("Movimento non trovato",
                    "Il movimento selezionato non è più presente tra le transazioni."));
        }

        if (Movimento[0].split("_").length < 5) {
            return new Separazione(null, Esito.Errore("ID movimento non valido",
                    "L'ID del movimento non ha il formato atteso e non può essere separato."));
        }

        if (!DueMoneteCoinvolte(Movimento)) {
            return new Separazione(null, Esito.Errore("Movimento non separabile",
                    "Il movimento non coinvolge due monete: non c'è nulla da separare."));
        }

        //I movimenti automatici non possono essere modificati strutturalmente: verrebbero comunque
        //rigenerati alla prossima importazione (stessa regola applicata da GUI_ModificaMovimento)
        if (Movimento[22] != null && Movimento[22].equalsIgnoreCase("AU")) {
            return new Separazione(null, Esito.Errore("Movimento automatico",
                    "Questo movimento è stato generato automaticamente dal programma e non può essere separato.<br>"
                    + "Su questo tipo di movimento è consentito variare solamente Prezzo e Note."));
        }

        //Se il movimento è collegato ad altri (campo 20) separarlo lascerebbe i movimenti collegati orfani
        if (Movimento[20] != null && !Movimento[20].isBlank()) {
            return new Separazione(null, Esito.Errore("Movimento collegato ad altri",
                    "Questo movimento è collegato ad altri movimenti e non può essere separato direttamente.<br>"
                    + "Riportalo prima alla situazione iniziale (ad esempio dalla modifica del movimento) e riprova."));
        }

        return new Separazione(Movimento, Esito.Ok());
    }

    /**
     * Passo 2 della separazione: costruisce le due gambe (un prelievo per la moneta in uscita e un deposito
     * per quella in entrata) e ne determina il valore in euro. <b>Può interrogare il database prezzi e le
     * API degli exchange, quindi va invocato fuori dall'EDT</b>, con una mascherina di attesa a carico del
     * chiamante; la mappa dei movimenti non viene ancora toccata.
     *
     * <p>Le due gambe ereditano dall'originale wallet, data/ora, note, hash, rete, address dei token e i
     * campi di provenienza ({@link #CampiDaRiportare}); campo 5, campo 18 e categoria vengono invece
     * ricalcolati da {@link MovimentiCrypto#creaMovimento} in base al tipo delle monete, quindi i due
     * movimenti risultano <b>non classificati</b> e ricompaiono nella tabella "Depositi e Prelievi" pronti
     * per essere riclassificati. Il valore in euro è invece deciso gamba per gamba da
     * {@link #RisolviPrezzoGamba}.</p>
     *
     * <p>L'ID del prelievo mantiene i quattro campi identificativi dell'originale (cambia solo la
     * categoria finale), mentre il deposito riceve una {@code A} sul terzo campo, la stessa convenzione
     * già usata da {@code GUI_ClassificazioneMovimento.CreaMovimentoTrasferimentoA} per garantire che
     * nell'ordinamento della mappa il prelievo preceda sempre il deposito.</p>
     *
     * @param S stato restituito da {@link #VerificaSeparazione}
     * @return l'esito della preparazione; in caso di errore la mappa dei movimenti è rimasta invariata
     */
    public static Esito PreparaGambeSeparazione(Separazione S) {
        if (!S.isSeparabile()) return S.Controlli;

        String Movimento[] = S.Originale;
        String IDSpezzato[] = Movimento[0].split("_");
        String Rete = Funzioni.TrovaReteDaIMovimento(Movimento);
        long Timestamp = FunzioniDate.ConvertiDataIDinLong(IDSpezzato[0]);

        //Gamba in uscita: diventerà un prelievo (PC/PF) mantenendo i campi identificativi dell'ID originale
        Moneta MonetaOUT = CostruisciMoneta(Movimento[8], Movimento[9], Movimento[10], Movimento[25], Movimento[26], Rete, true);
        String IDPrelievo = IDSpezzato[0] + "_" + IDSpezzato[1] + "_" + IDSpezzato[2] + "_" + IDSpezzato[3] + "_" + IDSpezzato[4];

        //Gamba in entrata: diventerà un deposito (DC/DF), posizionato subito dopo il prelievo
        Moneta MonetaIN = CostruisciMoneta(Movimento[11], Movimento[12], Movimento[13], Movimento[27], Movimento[28], Rete, false);
        String IDDeposito = IDSpezzato[0] + "_" + IDSpezzato[1] + "_" + IDSpezzato[2] + "A_" + IDSpezzato[3] + "_" + IDSpezzato[4];

        //Qui sta il lavoro potenzialmente lento: la gamba la cui moneta non è quella da cui proviene il
        //prezzo del movimento originale va riprezzata da capo
        String PrezzoOUT[] = RisolviPrezzoGamba(MonetaOUT, Movimento, Rete, Timestamp);
        String PrezzoIN[] = RisolviPrezzoGamba(MonetaIN, Movimento, Rete, Timestamp);

        String Prelievo[] = MovimentiCrypto.creaMovimento(
                MonetaOUT, null,
                Movimento[3], Movimento[4],
                Timestamp,
                PrezzoOUT[0], null,
                1, 1,
                IDPrelievo,
                Movimento[21],
                "M",
                Movimento[24],
                null,               //TipoTr null: descrizione e categoria dedotte dal tipo di moneta
                null
        );

        String Deposito[] = MovimentiCrypto.creaMovimento(
                null, MonetaIN,
                Movimento[3], Movimento[4],
                Timestamp,
                PrezzoIN[0], null,
                1, 1,
                IDDeposito,
                Movimento[21],
                "M",
                Movimento[24],
                null,               //TipoTr null: descrizione e categoria dedotte dal tipo di moneta
                null
        );

        if (Prelievo == null || Deposito == null) {
            return Esito.Errore("Separazione non riuscita",
                    "Non è stato possibile costruire i due movimenti separati.<br>"
                    + "Il movimento originale è stato lasciato invariato.");
        }

        RiportaCampiOriginali(Prelievo, Movimento);
        RiportaCampiOriginali(Deposito, Movimento);

        //Prezzo, flag di valorizzazione e info prezzo vengono imposti gamba per gamba: creaMovimento
        //arrotonderebbe il prezzo e marcherebbe comunque la fonte come "Personalizzato"
        AssegnaPrezzo(Prelievo, PrezzoOUT);
        AssegnaPrezzo(Deposito, PrezzoIN);

        S.Prelievo = Prelievo;
        S.Deposito = Deposito;
        return Esito.Ok();
    }

    /**
     * Passo 3 della separazione: sostituisce in mappa il movimento originale con le due gambe preparate.
     * <b>Va invocato sull'EDT</b>, perché {@link Principale#MappaCryptoWallet} è la stessa mappa che i
     * modelli delle tabelle scorrono durante il ridisegno.
     *
     * @param S stato già passato per {@link #PreparaGambeSeparazione}
     * @return l'esito dell'inserimento; in caso di errore il movimento originale viene ripristinato
     */
    public static Esito ApplicaSeparazione(Separazione S) {
        if (!S.isSeparabile()) return S.Controlli;
        if (S.Prelievo == null || S.Deposito == null) {
            return Esito.Errore("Separazione non riuscita",
                    "Le due gambe del movimento non sono state preparate.<br>"
                    + "Il movimento originale è stato lasciato invariato.");
        }

        String Movimento[] = S.Originale;

        //Elimino l'originale prima di assegnare gli ID definitivi, così il suo ID torna disponibile
        MappaCryptoWallet.remove(Movimento[0]);

        if (!InserisciMovimento(S.Prelievo)) {
            MappaCryptoWallet.put(Movimento[0], Movimento);
            return ErroreIDUnivoco(S.Prelievo[0]);
        }
        if (!InserisciMovimento(S.Deposito)) {
            //Tolgo la gamba già inserita e ripristino l'originale: meglio non lasciare il movimento a metà.
            //Il confronto è per identità perché, se l'ID non fosse quello appena assegnato, si rischierebbe
            //di cancellare un movimento di terzi che occupa lo stesso ID
            if (MappaCryptoWallet.get(S.Prelievo[0]) == S.Prelievo) MappaCryptoWallet.remove(S.Prelievo[0]);
            MappaCryptoWallet.put(Movimento[0], Movimento);
            return ErroreIDUnivoco(S.Deposito[0]);
        }

        LoggerGC.logInfo("Movimento " + Movimento[0] + " separato in prelievo " + S.Prelievo[0] + " e deposito " + S.Deposito[0]);
        return Esito.Ok();
    }

    /**
     * Esegue l'intera separazione in un colpo solo. Usata dai test, che non hanno una GUI da aggiornare né
     * una mascherina di attesa da mostrare: il codice dell'applicazione richiama i tre passi separatamente
     * per poter mettere in background la sola ricerca dei prezzi.
     * @param Movimento movimento da separare, già presente in mappa
     * @return {@code true} se la separazione è stata effettuata
     */
    static boolean EseguiSeparazione(String Movimento[]) {
        Separazione S = VerificaSeparazione(Movimento[0]);
        if (!S.isSeparabile()) return false;
        if (!PreparaGambeSeparazione(S).Riuscito) return false;
        return ApplicaSeparazione(S).Riuscito;
    }

    // =================================================================================================
    // PREZZO DELLE GAMBE SEPARATE
    // =================================================================================================

    /**
     * Determina il valore in euro di una delle due gambe appena separate.
     *
     * <p>Il movimento di partenza ha un solo controvalore (campo 15) ma due monete, e quel controvalore è
     * stato ricavato dal prezzo di <b>una</b> delle due: assegnarlo tale e quale a entrambe le gambe è
     * corretto solo finché le due monete valgono davvero lo stesso importo, cosa non garantita. Il campo 40
     * dice da dove viene il prezzo, nel formato {@code Moneta|timestamp|prezzoUnitario|Fonte}: il primo
     * segmento è il token su cui il prezzo è stato rilevato. Da lì i tre casi:</p>
     * <ul>
     *   <li><b>fonte generica</b> (primo segmento vuoto: prezzo imposto dall'utente o comunque passato
     *       dall'esterno, come {@code "|||Personalizzato"}) — non è legata a un token in particolare e vale
     *       per il movimento nel suo complesso, quindi entrambe le gambe ereditano prezzo e fonte;</li>
     *   <li><b>fonte riferita alla moneta della gamba</b> — il controvalore descrive già questa moneta:
     *       prezzo e fonte vengono ereditati senza toccare nulla;</li>
     *   <li><b>fonte riferita all'altra moneta</b> — il controvalore descrive l'altro token: il prezzo di
     *       questa gamba va ricercato da capo e la fonte aggiornata di conseguenza.</li>
     * </ul>
     *
     * <p>Se il movimento di partenza non è valorizzato non c'è alcun prezzo da ereditare né motivo di
     * cercarne uno: entrambe le gambe restano non valorizzate come l'originale.</p>
     *
     * @param Gamba moneta della gamba, {@code null} se la gamba non è valida
     * @param Movimento movimento che si sta separando
     * @param Rete rete blockchain del movimento
     * @param Timestamp data/ora del movimento in millisecondi epoch
     * @return array {@code {prezzo (campo 15), flag di valorizzazione (campo 32), info prezzo (campo 40)}}
     */
    private static String[] RisolviPrezzoGamba(Moneta Gamba, String Movimento[], String Rete, long Timestamp) {
        String Ereditato[] = {PrezzoSicuro(Movimento[15]), Movimento[32], Movimento[40]};

        if (Gamba == null || !Valorizzato(Movimento)) return Ereditato;
        if (GambaCopertaDallaFonte(Movimento[40], Gamba, Rete)) return Ereditato;

        Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoInfoTransazione(Gamba, null, Timestamp, Rete, Movimento[3]);
        BigDecimal Qta = Gamba.GetQtaBD();
        if (IP == null || IP.prezzoUnitario == null || Qta == null) {
            LoggerGC.logInfo("Separazione del movimento " + Movimento[0] + ": prezzo di " + Gamba.Moneta
                    + " non trovato, la gamba resta non valorizzata");
            return new String[]{"0.00", "NO", ""};
        }

        //Il totale si ricalcola sempre da quantità e prezzo unitario: prezzoQta può riferirsi a una
        //quantità diversa da quella della gamba (stessa cautela già presa da creaMovimento)
        String Prezzo = Qta.abs().multiply(IP.prezzoUnitario).setScale(2, RoundingMode.HALF_UP).toPlainString();
        return new String[]{Prezzo, "SI", IP.Ritorna40()};
    }

    /**
     * Stabilisce se il prezzo del movimento che si sta separando descrive anche la moneta di questa gamba.
     * @param InfoPrezzo campo 40 del movimento originale
     * @param Gamba moneta della gamba
     * @param Rete rete blockchain del movimento
     * @return {@code true} se la gamba può ereditare prezzo e fonte, {@code false} se il prezzo va ricercato
     */
    static boolean GambaCopertaDallaFonte(String InfoPrezzo, Moneta Gamba, String Rete) {
        String TokenFonte = TokenFontePrezzo(InfoPrezzo);
        //Fonte generica: non è ricavata da un token specifico, quindi copre l'intero movimento
        if (TokenFonte.isBlank()) return true;
        if (Gamba == null) return false;
        if (MovimentiCrypto.normalizzaMoneta(Gamba.Moneta).equalsIgnoreCase(TokenFonte)) return true;

        //Prima di cercare un prezzo, Prezzi rinomina i token noti in base ad address+rete (es. l'USDT su BSC
        //diventa l'USDT quotato su Binance): nel campo 40 finisce quel nome, non il simbolo grezzo del movimento
        String NomeMappato = Principale.Mappa_AddressRete_Nome.get(Gamba.MonetaAddress + "_" + Rete);
        return NomeMappato != null && NomeMappato.trim().equalsIgnoreCase(TokenFonte);
    }

    /**
     * Legge dal campo 40 il token a cui l'informazione di prezzo si riferisce.
     * @param InfoPrezzo campo 40 del movimento, può essere {@code null} o vuoto
     * @return il nome del token, oppure stringa vuota se la fonte non è legata a un token specifico o se il
     *         campo non è nel formato atteso
     */
    static String TokenFontePrezzo(String InfoPrezzo) {
        if (InfoPrezzo == null || InfoPrezzo.isBlank()) return "";
        String Token = new Prezzi.InfoPrezzo(InfoPrezzo).Moneta;
        return Token == null ? "" : Token.trim();
    }

    /**
     * Applica a una gamba il prezzo risolto da {@link #RisolviPrezzoGamba}.
     * @param Gamba movimento della gamba
     * @param Prezzo array {@code {prezzo, flag di valorizzazione, info prezzo}}
     */
    private static void AssegnaPrezzo(String Gamba[], String Prezzo[]) {
        Gamba[15] = Prezzo[0];
        Gamba[32] = Prezzo[1];
        Gamba[40] = Prezzo[2];
    }

    /** @return l'esito di errore da mostrare quando non si riesce ad assegnare un ID univoco a un movimento */
    private static Esito ErroreIDUnivoco(String ID) {
        return Esito.Errore("Errore generazione ID",
                "Impossibile generare un ID univoco per il movimento " + ID + ".<br>"
                + "L'operazione è stata annullata.");
    }

    // =================================================================================================
    // FUNZIONE 2 - CREA MOVIMENTO DI SCAMBIO DA DEPOSITO/PRELIEVO
    // =================================================================================================

    /**
     * Fonde un deposito e un prelievo non classificati, contemporanei e sullo stesso wallet, in un unico
     * movimento di scambio; i due movimenti di partenza vengono eliminati.
     *
     * <p>La categoria del movimento risultante (SC, AC, VC, SF...) e la relativa descrizione vengono
     * dedotte da {@link MovimentiCrypto#creaMovimento} in base al tipo delle due monete, quindi tutte le
     * combinazioni Crypto/FIAT/NFT sono gestite automaticamente. Il nuovo movimento eredita i dati del
     * prelievo (e, per i campi non valorizzati, quelli del deposito).</p>
     *
     * <p>Il prezzo del movimento fuso è quello della gamba prezzata; se entrambe lo sono, viene scelta
     * quella la cui moneta è più affidabile come riferimento secondo
     * {@link MovimentiCrypto#DammiMonetaPrioritaria} (FIAT, poi stablecoin, poi le crypto a maggiore
     * capitalizzazione). Se nessuna delle due è prezzata il movimento resta non valorizzato.</p>
     *
     * @param IDs lista degli ID selezionati (devono essere esattamente due)
     * @param owner finestra su cui centrare i dialoghi di conferma ed errore
     * @return {@code true} se la fusione è stata effettuata (il chiamante deve aggiornare le tabelle)
     */
    public static boolean CreaScambioDaDepositoPrelievo(List<String> IDs, Window owner) {
        String Coppia[][] = TrovaCoppiaDepositoPrelievo(IDs);
        if (Coppia == null) {
            Messaggi.WarningMessage("Movimenti non fondibili",
                    "Per creare uno scambio servono esattamente due movimenti non classificati, uno di deposito e "
                    + "uno di prelievo, sullo stesso wallet e con la stessa data/ora al secondo.", owner);
            return false;
        }

        String Prelievo[] = Coppia[0];
        String Deposito[] = Coppia[1];

        //Come per la separazione, i movimenti già collegati ad altri non possono essere eliminati
        //senza lasciare orfani i movimenti collegati
        if ((Prelievo[20] != null && !Prelievo[20].isBlank()) || (Deposito[20] != null && !Deposito[20].isBlank())) {
            Messaggi.WarningMessage("Movimenti collegati ad altri",
                    "Uno dei due movimenti è collegato ad altri movimenti e non può essere fuso direttamente.<br>"
                    + "Riportalo prima alla situazione iniziale (ad esempio dalla modifica del movimento) e riprova.", owner);
            return false;
        }

        String MonetaUscita = NomeVisualizzato(Prelievo[8], Prelievo[25]);
        String MonetaEntrata = NomeVisualizzato(Deposito[11], Deposito[27]);

        AppDialog.DialogResult result = AppDialog.builder(owner)
                .windowTitle("Creazione movimento di scambio")
                .bodyTitle("Unire i due movimenti in uno scambio?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("I due movimenti verranno sostituiti da un unico movimento di scambio.")
                .details("Verrà creato un movimento che scambia:<br>"
                        + " - in uscita " + MonetaUscita + "<br>"
                        + " - in entrata " + MonetaEntrata + "<br>"
                        + "sul wallet <b>" + Prelievo[3] + "</b>.<br>"
                        + "I due movimenti di deposito e prelievo di partenza verranno eliminati.<br>"
                        + "Si vuole proseguire?")
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("unisci", "Crea lo scambio")
                        .role(AppDialog.ActionRole.DANGER)
                        .build())
                .showDialog();

        if (result == null || !result.isAction("unisci")) {
            return false;
        }

        return EseguiFusione(Prelievo, Deposito, owner);
    }

    /**
     * Esegue materialmente la fusione, una volta superati i controlli e ottenuta la conferma dell'utente:
     * crea il movimento di scambio, elimina i due movimenti di partenza e inserisce il nuovo in mappa.
     * @param Prelievo movimento di prelievo da fondere
     * @param Deposito movimento di deposito da fondere
     * @param owner finestra su cui centrare l'eventuale messaggio di errore
     * @return {@code true} se la fusione è stata effettuata
     */
    static boolean EseguiFusione(String Prelievo[], String Deposito[], Window owner) {
        String Rete = Funzioni.TrovaReteDaIMovimento(Prelievo);
        if (Funzioni.noData(Rete)) Rete = Funzioni.TrovaReteDaIMovimento(Deposito);

        Moneta MonetaOUT = CostruisciMoneta(Prelievo[8], Prelievo[9], Prelievo[10], Prelievo[25], Prelievo[26], Rete, true);
        Moneta MonetaIN = CostruisciMoneta(Deposito[11], Deposito[12], Deposito[13], Deposito[27], Deposito[28], Rete, false);

        //Il valore in euro del movimento fuso è quello della gamba prezzata più affidabile
        String GambaPrezzata[] = ScegliGambaPrezzata(MonetaOUT, Prelievo, MonetaIN, Deposito);
        boolean Valorizzato = GambaPrezzata != null;
        String Prezzo = Valorizzato ? PrezzoSicuro(GambaPrezzata[15]) : "0.00";

        //Il movimento risultante nasce sull'ID del prelievo: la categoria finale viene ricalcolata
        long Timestamp = FunzioniDate.ConvertiDataIDinLong(Prelievo[0].split("_")[0]);

        String Scambio[] = MovimentiCrypto.creaMovimento(
                MonetaOUT, MonetaIN,
                Prelievo[3], PrimoValorizzato(Prelievo[4], Deposito[4]),
                Timestamp,
                Prezzo, null,
                1, 1,
                Prelievo[0],
                UnisciNote(Prelievo[21], Deposito[21]),
                "M",
                PrimoValorizzato(Prelievo[24], Deposito[24]),
                null,               //TipoTr null: descrizione e categoria dedotte dal tipo delle monete
                null
        );

        if (Scambio == null) {
            Messaggi.WarningMessage("Creazione scambio non riuscita",
                    "Non è stato possibile costruire il movimento di scambio.<br>"
                    + "I due movimenti di partenza sono stati lasciati invariati.", owner);
            return false;
        }

        //I campi di provenienza vengono presi dal prelievo e, se vuoti, dal deposito
        RiportaCampiOriginali(Scambio, Prelievo);
        for (int Campo : CampiDaRiportare) {
            if (Funzioni.noData(Scambio[Campo])) Scambio[Campo] = Deposito[Campo];
        }

        //Prezzo e info prezzo vengono riportati verbatim dalla gamba scelta, senza l'arrotondamento e
        //senza la fonte "Personalizzato" che creaMovimento applicherebbe
        Scambio[15] = Prezzo;
        Scambio[32] = Valorizzato ? "SI" : "NO";
        Scambio[40] = Valorizzato ? GambaPrezzata[40] : "";

        //Elimino i due originali prima di assegnare l'ID definitivo, così i loro ID tornano disponibili
        MappaCryptoWallet.remove(Prelievo[0]);
        MappaCryptoWallet.remove(Deposito[0]);

        if (!InserisciMovimento(Scambio)) {
            //Ripristino i due movimenti di partenza
            MappaCryptoWallet.put(Prelievo[0], Prelievo);
            MappaCryptoWallet.put(Deposito[0], Deposito);
            Esito Errore = ErroreIDUnivoco(Scambio[0]);
            Messaggi.WarningMessage(Errore.Titolo, Errore.Messaggio, owner);
            return false;
        }

        LoggerGC.logInfo("Movimenti " + Prelievo[0] + " e " + Deposito[0] + " uniti nello scambio " + Scambio[0]);
        return true;
    }

    // =================================================================================================
    // FUNZIONI DI SUPPORTO
    // =================================================================================================

    /**
     * Costruisce la moneta da passare a {@link MovimentiCrypto#creaMovimento} a partire dai campi grezzi
     * del movimento di origine; il segno della quantità distingue da solo uscita ed entrata.
     * @return la moneta costruita, oppure {@code null} se simbolo o quantità non sono validi
     */
    private static Moneta CostruisciMoneta(String Simbolo, String Tipo, String Qta, String NomeEsteso, String Address, String Rete, boolean Uscita) {
        if (!MonetaValida(Simbolo, Qta)) return null;
        Moneta Mon = new Moneta();
        Mon.Moneta = Simbolo;
        Mon.Tipo = Tipo;
        Mon.Qta = ForzaSegno(Qta, Uscita);
        Mon.NomeEsteso = NomeEsteso;
        Mon.MonetaAddress = Address;
        Mon.Rete = Rete;
        return Mon;
    }

    /**
     * Sceglie il prezzo del movimento di scambio tra quelli delle due gambe fuse, considerando solo le
     * gambe effettivamente valorizzate (campo 32 = "SI"). Se lo sono entrambe, viene preferita quella la
     * cui moneta {@link MovimentiCrypto#DammiMonetaPrioritaria} considera più affidabile.
     * @return il movimento da cui prendere prezzo e info prezzo, oppure {@code null} se nessuna delle
     *         due gambe è valorizzata
     */
    private static String[] ScegliGambaPrezzata(Moneta MonetaOUT, String Prelievo[], Moneta MonetaIN, String Deposito[]) {
        boolean PrelievoPrezzato = Valorizzato(Prelievo);
        boolean DepositoPrezzato = Valorizzato(Deposito);

        if (PrelievoPrezzato && DepositoPrezzato) {
            //Entrambe prezzate: uso la moneta che il programma considera più affidabile come riferimento
            Moneta Prioritaria = MovimentiCrypto.DammiMonetaPrioritaria(MonetaOUT, MonetaIN);
            if (Prioritaria == MonetaIN) return Deposito;
            return Prelievo;
        }
        if (PrelievoPrezzato) return Prelievo;
        if (DepositoPrezzato) return Deposito;
        return null;
    }

    /**
     * Normalizza il prezzo da passare a {@code creaMovimento}. La condizione da rispettare non è la
     * semplice numericità ma {@link MovimentiCrypto#PrezzoPrezzato}, che è il test usato da
     * {@code creaMovimento} per decidere se accettare il prezzo passato: un valore numericamente nullo
     * ma scritto diversamente da {@code "0.00"} (es. {@code "0"}, {@code "0.0"}, {@code "0E-8"}) verrebbe
     * rifiutato e, non avendo le Monete né prezzo né info prezzo, si finirebbe nel ramo di **ricerca
     * prezzi online**, che bloccherebbe l'interfaccia.
     * @param Prezzo prezzo del movimento di origine
     * @return il prezzo se accettabile da {@code creaMovimento}, altrimenti {@code "0.00"}
     */
    private static String PrezzoSicuro(String Prezzo) {
        return MovimentiCrypto.PrezzoPrezzato(Prezzo) ? Prezzo : "0.00";
    }

    /** @return {@code true} se il movimento è valorizzato (campo 32 = "SI") con un prezzo numerico */
    private static boolean Valorizzato(String Movimento[]) {
        return Movimento[32] != null && Movimento[32].equalsIgnoreCase("SI")
                && Funzioni.isNumeric(Movimento[15], false);
    }

    /**
     * Riporta sul movimento appena creato i campi di provenienza che {@code creaMovimento} non popola.
     * @param Nuovo movimento appena creato
     * @param Originale movimento da cui copiare i campi
     */
    private static void RiportaCampiOriginali(String Nuovo[], String Originale[]) {
        for (int Campo : CampiDaRiportare) {
            if (Campo < Originale.length && Campo < Nuovo.length) Nuovo[Campo] = Originale[Campo];
        }
        Importazioni.RiempiVuotiArray(Nuovo);
    }

    /**
     * Assegna al movimento un ID univoco e lo inserisce in {@link Principale#MappaCryptoWallet}.
     * @param Movimento movimento da inserire (il suo campo 0 viene aggiornato con l'ID definitivo)
     * @return {@code true} se l'inserimento è riuscito
     */
    private static boolean InserisciMovimento(String Movimento[]) {
        String IDUnivoco = MovimentiCrypto.getIDUnivoco(MappaCryptoWallet, Movimento[0]);
        if (IDUnivoco == null) return false;
        Movimento[0] = IDUnivoco;
        MappaCryptoWallet.put(IDUnivoco, Movimento);
        return true;
    }

    /**
     * Forza il segno di una quantità senza toccarne la restante rappresentazione testuale:
     * {@code creaMovimento} distingue uscita ed entrata dal solo segno, quindi lo si impone invece di
     * fidarsi della convenzione con cui il movimento di origine è stato scritto. Viene manipolato solo
     * il segno iniziale, perché un {@code replace("-","")} indiscriminato rovinerebbe le quantità in
     * notazione scientifica (es. {@code 1.5E-8}).
     * @param Qta quantità da normalizzare
     * @param Uscita {@code true} per renderla negativa, {@code false} per renderla positiva
     * @return la quantità con il segno voluto
     */
    private static String ForzaSegno(String Qta, boolean Uscita) {
        String Assoluto = Qta.trim().startsWith("-") ? Qta.trim().substring(1) : Qta.trim();
        if (Assoluto.startsWith("+")) Assoluto = Assoluto.substring(1);
        return Uscita ? "-" + Assoluto : Assoluto;
    }

    /** @return il nome esteso della moneta se disponibile, altrimenti il simbolo */
    private static String NomeVisualizzato(String Simbolo, String NomeEsteso) {
        if (NomeEsteso != null && !NomeEsteso.isBlank()) return NomeEsteso;
        return Simbolo;
    }

    /** @return il primo dei due valori che non sia vuoto/nullo, oppure stringa vuota se lo sono entrambi */
    private static String PrimoValorizzato(String Valore1, String Valore2) {
        if (!Funzioni.noData(Valore1)) return Valore1;
        if (!Funzioni.noData(Valore2)) return Valore2;
        return "";
    }

    /** @return le note dei due movimenti fusi, concatenate se diverse ed entrambe presenti */
    private static String UnisciNote(String Note1, String Note2) {
        if (Funzioni.noData(Note1)) return Funzioni.noData(Note2) ? "" : Note2;
        if (Funzioni.noData(Note2) || Note1.trim().equalsIgnoreCase(Note2.trim())) return Note1;
        return Note1 + " - " + Note2;
    }
}
