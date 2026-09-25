/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.awt.Window;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Logica operativa delle tre voci del menu contestuale che trasformano la struttura di un movimento:
 * <ul>
 *   <li><b>Separa in Deposito/Prelievo</b> — spezza un movimento che coinvolge due monete nelle sue due
 *       gambe indipendenti (un prelievo per la moneta in uscita, un deposito per quella in entrata) ed
 *       elimina l'originale;</li>
 *   <li><b>Crea movimento di scambio da Deposito/Prelievo</b> — l'operazione inversa: fonde un deposito e
 *       un prelievo non classificati, sullo stesso wallet e distanti al più un secondo, in un unico
 *       movimento di scambio/acquisto/vendita ed elimina i due originali.</li>
 *   <li><b>Unisci movimenti omogenei</b> — fonde N movimenti dello stesso tipo (stessa categoria, stesso
 *       campo 5, stesso campo 18), sulla stessa moneta e sullo stesso wallet/sotto-wallet, distanti al più
 *       un secondo l'uno dall'altro, sommandone le quantità in un unico movimento. A differenza dello
 *       scambio, qui i movimenti di partenza sono eventi economici indipendenti (es. 3 depositi separati),
 *       non le due facce di una stessa transazione: il controvalore del risultato è quindi la
 *       <b>somma</b> dei controvalori originali, non la scelta del più affidabile.</li>
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
     * fonte dati, documento di origine...): vanno riportati sui movimenti generati per non perdere
     * informazioni.
     * Sono volutamente esclusi i campi calcolati dai motori di calcolo (16/17 costo di carico,
     * 19/33 plusvalenza, 38 flag di anomalia) e il campo 35 (backup temporaneo del prezzo usato
     * dalla classificazione manuale).
     *
     * <p>Il 41 (documento di origine) è qui perché le due gambe della separazione sono <b>ricostruite</b> da
     * {@link MovimentiCrypto#creaMovimento}, non copiate: senza questa lista il legame al file da cui il
     * movimento proveniva andrebbe perso proprio nel momento in cui lo si manipola.
     *
     * <p>Il 42 (lignaggio dello storico modifiche) è qui per lo stesso motivo del 41: le gambe sono
     * ricostruite, e senza questa riga il collegamento alle versioni precedenti si perderebbe proprio
     * nel momento in cui si manipola il movimento. Conseguenza voluta: separando, le <b>due</b> gambe
     * ereditano lo stesso lignaggio e condividono quindi la storia del movimento da cui vengono — per
     * questo {@code MovimentiStorico.SalvaBuffer} non cancella un lignaggio ancora portato da un
     * movimento vivo.
     */
    private static final int[] CampiDaRiportare = {2, 7, 14, 23, 30, 31, 36, 37, 39, 41, 42};

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
     * (campo 18 vuoto e non generati automaticamente), sullo stesso wallet e distanti al più un secondo
     * (le due gambe di uno stesso scambio non sempre vengono registrate nello stesso identico istante).
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

        //Stesso istante, con la tolleranza di un secondo
        if (!IstantiCompatibili(MovPrelievo, MovDeposito)) return null;

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

    /** Scarto massimo ammesso tra i due movimenti da fondere, in millisecondi. */
    private static final long TOLLERANZA_ISTANTE_MS = 1000;

    /**
     * Confronta l'istante di due movimenti ammettendo uno scarto di {@link #TOLLERANZA_ISTANTE_MS}: le
     * due gambe di uno stesso scambio non sempre arrivano registrate nello stesso identico secondo.
     *
     * <p>La data contenuta nell'ID viene confrontata <b>prima</b> come stringa: se il secondo è lo stesso
     * i due movimenti sono fondibili comunque, anche se i rispettivi campi 29 divergessero di più di un
     * secondo — allargare una tolleranza non deve far rifiutare ciò che prima veniva accettato.</p>
     *
     * @return {@code true} se i due movimenti possono essere considerati contemporanei
     */
    private static boolean IstantiCompatibili(String Movimento1[], String Movimento2[]) {
        if (DataID(Movimento1).equals(DataID(Movimento2))) return true;
        long Istanti[] = IstantiConfrontabili(Movimento1, Movimento2);
        return Istanti != null && Math.abs(Istanti[0] - Istanti[1]) <= TOLLERANZA_ISTANTE_MS;
    }

    /**
     * Estrae dai due movimenti due istanti confrontabili <b>tra loro</b>: i timestamp del campo 29 se
     * valorizzati su entrambi, altrimenti le date contenute nel primo segmento dell'ID. Le due fonti non
     * vengono mai mischiate, perché il campo 29 ha la precisione al millisecondo mentre la data dell'ID è
     * troncata al secondo: confrontare l'una con l'altra produrrebbe scarti fittizi fino a un secondo,
     * cioè esattamente la tolleranza da misurare.
     * @return array {@code [istante1, istante2]} in millisecondi, oppure {@code null} se non ricavabili
     */
    private static long[] IstantiConfrontabili(String Movimento1[], String Movimento2[]) {
        if (Funzioni.isBigDecimalNonZero(Movimento1[29]) && Funzioni.isBigDecimalNonZero(Movimento2[29])) {
            return new long[]{new BigDecimal(Movimento1[29].trim()).longValue(),
                              new BigDecimal(Movimento2[29].trim()).longValue()};
        }
        long Istante1 = FunzioniDate.ConvertiDataIDinLong(DataID(Movimento1));
        long Istante2 = FunzioniDate.ConvertiDataIDinLong(DataID(Movimento2));
        if (Istante1 == 0 || Istante2 == 0) return null;
        return new long[]{Istante1, Istante2};
    }

    /** @return il primo segmento dell'ID del movimento, cioè la sua data nel formato {@code yyyyMMddHHmmss} */
    private static String DataID(String Movimento[]) {
        return Movimento[0].split("_")[0];
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
     * Fonde un deposito e un prelievo non classificati, sullo stesso wallet e distanti al più un secondo,
     * in un unico movimento di scambio; i due movimenti di partenza vengono eliminati.
     *
     * <p>La categoria del movimento risultante (SC, AC, VC, SF...) e la relativa descrizione vengono
     * dedotte da {@link MovimentiCrypto#creaMovimento} in base al tipo delle due monete, quindi tutte le
     * combinazioni Crypto/FIAT/NFT sono gestite automaticamente. Il nuovo movimento eredita i dati del
     * prelievo (e, per i campi non valorizzati, quelli del deposito).</p>
     *
     * <p>La data del movimento fuso è la <b>maggiore</b> delle due: quando le due gambe non sono
     * registrate nello stesso secondo, il momento in cui lo scambio è concluso è quello dell'ultima.</p>
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
                    + "uno di prelievo, sullo stesso wallet e distanti al massimo un secondo.", owner);
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

        //Il movimento risultante nasce sull'ID del prelievo, con la data della gamba più recente: la
        //categoria finale viene comunque ricalcolata da creaMovimento
        String IDBase = IDConDataPiuRecente(Prelievo, Deposito);
        long Timestamp = FunzioniDate.ConvertiDataIDinLong(IDBase.split("_")[0]);

        String Scambio[] = MovimentiCrypto.creaMovimento(
                MonetaOUT, MonetaIN,
                Prelievo[3], PrimoValorizzato(Prelievo[4], Deposito[4]),
                Timestamp,
                Prezzo, null,
                1, 1,
                IDBase,
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
     * Compone l'ID su cui far nascere il movimento fuso: l'identificazione e i progressivi restano quelli
     * del prelievo — da cui provengono anche tutti gli altri campi — mentre la data è la maggiore delle
     * due, perché le due gambe possono essere registrate a un secondo di distanza e lo scambio si
     * considera concluso con l'ultima.
     *
     * <p>Le date sono confrontate come <b>stringhe</b>: il formato {@code yyyyMMddHHmmss} è a larghezza
     * fissa, quindi l'ordine lessicografico è già quello cronologico. Se una delle due non ha quella
     * forma si resta sull'ID del prelievo, cioè sul comportamento precedente.</p>
     *
     * @return l'ID da passare a {@link MovimentiCrypto#creaMovimento} come ID da rispettare
     */
    private static String IDConDataPiuRecente(String Prelievo[], String Deposito[]) {
        String DataPrelievo = DataID(Prelievo);
        String DataDeposito = DataID(Deposito);
        if (!DataPrelievo.matches("\\d{14}") || !DataDeposito.matches("\\d{14}")) return Prelievo[0];
        if (DataDeposito.compareTo(DataPrelievo) <= 0) return Prelievo[0];
        return DataDeposito + Prelievo[0].substring(DataPrelievo.length());
    }

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

    // =================================================================================================
    // FUNZIONE 3 - UNISCI MOVIMENTI OMOGENEI (somma le quantità)
    // =================================================================================================

    /**
     * Verifica se i movimenti selezionati possono essere fusi in un unico movimento sommandone le
     * quantità: vedi {@link #TrovaGruppoOmogeneo} per le condizioni esatte.
     * @param IDs lista degli ID attualmente selezionati in tabella, può essere {@code null}
     * @return {@code true} se la voce "Unisci movimenti omogenei" va abilitata
     */
    public static boolean isUnibileInUnico(List<String> IDs) {
        return TrovaGruppoOmogeneo(IDs) != null;
    }

    /**
     * Individua, tra gli ID selezionati, il gruppo di movimenti omogenei fondibile in uno solo.
     *
     * <p>Condizioni, tutte necessarie: almeno due ID, tutti esistenti in mappa; ciascun movimento
     * coinvolge <b>una sola</b> moneta (un deposito/prelievo/reward, mai uno scambio a due gambe, per cui
     * "sommare le quantità" non avrebbe un significato univoco); stessa moneta (simbolo, address e rete);
     * stesso wallet <b>e</b> sotto-wallet; stessa categoria, stesso campo 5 e stesso campo 18 (non basta la
     * categoria: due RW con campo 18 diverso, es. AIRDROP e CASHBACK, sono fiscalmente distinte); nessuno
     * collegato ad altri movimenti (campo 20) o generato automaticamente (campo 22 = "AU", verrebbe
     * comunque rigenerato alla prossima importazione); tutti compatibili a coppie entro la tolleranza di
     * {@link #TOLLERANZA_ISTANTE_MS} — non solo rispetto al primo, perché tre movimenti a 0s/0.9s/1.8s
     * avrebbero gli estremi a 1.8s di distanza pur essendo ciascuno entro un secondo dal successivo.
     *
     * @param IDs lista degli ID selezionati (eventuali doppioni vengono ignorati)
     * @return i movimenti del gruppo in ordine di ID, cioè cronologico e uguale a quello della mappa (così
     *         il risultato non dipende dall'ordine in cui l'utente ha selezionato le righe), oppure
     *         {@code null} se la selezione non soddisfa tutte le condizioni
     */
    static List<String[]> TrovaGruppoOmogeneo(List<String> IDs) {
        if (IDs == null) return null;
        Set<String> IDDistinti = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String ID : IDs) {
            if (ID != null) IDDistinti.add(ID);
        }
        if (IDDistinti.size() < 2) return null;

        List<String[]> Gruppo = new ArrayList<>();
        for (String ID : IDDistinti) {
            String Movimento[] = MappaCryptoWallet.get(ID);
            if (Movimento == null) return null;
            Gruppo.add(Movimento);
        }

        String Primo[] = Gruppo.get(0);
        if (!MovimentoUnaSolaMoneta(Primo) || !MovimentoFondibileInUnico(Primo)) return null;
        boolean Uscita = MonetaValida(Primo[8], Primo[10]);

        for (int i = 1; i < Gruppo.size(); i++) {
            String Corrente[] = Gruppo.get(i);
            if (!MovimentoUnaSolaMoneta(Corrente) || !MovimentoFondibileInUnico(Corrente)) return null;
            if (MonetaValida(Corrente[8], Corrente[10]) != Uscita) return null;
            if (!StessoTipo(Primo, Corrente)) return null;
            if (!StessoWalletESottoWallet(Primo, Corrente)) return null;
            if (!StessaMoneta(Primo, Corrente, Uscita)) return null;
        }

        //Tolleranza a coppie, non solo rispetto al primo: vedi il javadoc del metodo
        for (int i = 0; i < Gruppo.size(); i++) {
            for (int j = i + 1; j < Gruppo.size(); j++) {
                if (!IstantiCompatibili(Gruppo.get(i), Gruppo.get(j))) return null;
            }
        }

        return Gruppo;
    }

    /** @return {@code true} se il movimento coinvolge esattamente una moneta (mai zero, mai due) */
    private static boolean MovimentoUnaSolaMoneta(String Movimento[]) {
        return MonetaValida(Movimento[8], Movimento[10]) ^ MonetaValida(Movimento[11], Movimento[13]);
    }

    /** @return {@code true} se il movimento non è collegato ad altri (campo 20) né generato automaticamente (campo 22 = "AU") */
    private static boolean MovimentoFondibileInUnico(String Movimento[]) {
        if (Movimento[20] != null && !Movimento[20].isBlank()) return false;
        return Movimento[22] == null || !Movimento[22].equalsIgnoreCase("AU");
    }

    /** @return {@code true} se i due movimenti hanno la stessa categoria, lo stesso campo 5 e lo stesso campo 18 */
    private static boolean StessoTipo(String M1[], String M2[]) {
        return CampoUguale(Categoria(M1), Categoria(M2))
                && CampoUguale(M1[5], M2[5])
                && CampoUguale(M1[18], M2[18]);
    }

    /** @return {@code true} se i due movimenti sono sullo stesso wallet e sullo stesso sotto-wallet */
    private static boolean StessoWalletESottoWallet(String M1[], String M2[]) {
        return CampoUguale(M1[3], M2[3]) && CampoUguale(M1[4], M2[4]);
    }

    /**
     * @param Uscita {@code true} per confrontare la moneta in uscita (campi 8/26), {@code false} per
     *               quella in entrata (campi 11/28)
     * @return {@code true} se i due movimenti muovono lo stesso token: stesso simbolo, stesso address e
     *         stessa rete (non basta il simbolo: due token con lo stesso ticker su reti diverse sono
     *         monete diverse)
     */
    private static boolean StessaMoneta(String M1[], String M2[], boolean Uscita) {
        boolean StessoSimboloEAddress = Uscita
                ? CampoUguale(M1[8], M2[8]) && CampoUguale(M1[26], M2[26])
                : CampoUguale(M1[11], M2[11]) && CampoUguale(M1[28], M2[28]);
        if (!StessoSimboloEAddress) return false;
        return CampoUguale(Funzioni.TrovaReteDaIMovimento(M1), Funzioni.TrovaReteDaIMovimento(M2));
    }

    /** @return {@code true} se i due campi sono uguali (case-insensitive), trattando {@code null} come stringa vuota */
    private static boolean CampoUguale(String A, String B) {
        String a = A == null ? "" : A.trim();
        String b = B == null ? "" : B.trim();
        return a.equalsIgnoreCase(b);
    }

    /**
     * Fonde in un unico movimento tutti i movimenti dello stesso tipo indicati, sommandone le quantità;
     * chiede sempre conferma esplicita all'utente, con avvisi aggiuntivi quando la fusione comporta una
     * perdita di informazione (vedi {@link #EseguiUnioneOmogenei}).
     *
     * @param IDs lista degli ID selezionati
     * @param owner finestra su cui centrare i dialoghi di conferma ed errore
     * @return {@code true} se l'unione è stata effettuata (il chiamante deve aggiornare le tabelle)
     */
    public static boolean UnisciMovimentiOmogenei(List<String> IDs, Window owner) {
        List<String[]> Gruppo = TrovaGruppoOmogeneo(IDs);
        if (Gruppo == null) {
            Messaggi.WarningMessage("Movimenti non fondibili",
                    "Per unire più movimenti servono almeno due movimenti dello stesso tipo (stessa "
                    + "categoria, stessa descrizione e stessa classificazione), sulla stessa moneta e sullo "
                    + "stesso wallet, nessuno collegato ad altri movimenti né generato automaticamente, e "
                    + "distanti al massimo un secondo l'uno dall'altro.", owner);
            return false;
        }

        boolean Uscita = MonetaValida(Gruppo.get(0)[8], Gruppo.get(0)[10]);
        String Moneta = NomeVisualizzato(Uscita ? Gruppo.get(0)[8] : Gruppo.get(0)[11],
                Uscita ? Gruppo.get(0)[25] : Gruppo.get(0)[27]);
        boolean TuttiValorizzati = Gruppo.stream().allMatch(Principale_Movimenti_SeparaUnisci::Valorizzato);
        boolean PrezziDiversi = TuttiValorizzati && PrezziUnitariDiversi(Gruppo, Uscita);

        StringBuilder Dettagli = new StringBuilder();
        Dettagli.append("I ").append(Gruppo.size())
                .append(" movimenti selezionati verranno sostituiti da un unico movimento che somma le quantità:<br>");
        for (int i = 0; i < Gruppo.size(); i++) {
            //Con selezioni molto ampie (es. decine di dust) l'elenco completo renderebbe il dialogo
            //più alto dello schermo: resta comunque tutto nella nota e nello storico del movimento
            if (i == RIGHE_MAX_DIALOGO_UNIONE) {
                Dettagli.append(" - ... e altri ").append(Gruppo.size() - i).append(" movimenti<br>");
                break;
            }
            String Qta = Uscita ? Gruppo.get(i)[10] : Gruppo.get(i)[13];
            Dettagli.append(" - ").append(new BigDecimal(Qta).abs().stripTrailingZeros().toPlainString())
                    .append(" ").append(Moneta).append("<br>");
        }
        Dettagli.append("Totale: <b>").append(SommaQuantita(Gruppo, Uscita).abs().stripTrailingZeros().toPlainString())
                .append(" ").append(Moneta).append("</b> sul wallet <b>").append(Gruppo.get(0)[3]).append("</b>.<br>");
        Dettagli.append("I movimenti di partenza verranno eliminati; le loro righe originali restano "
                + "consultabili nello storico modifiche del movimento unito.<br>");

        //Se una nuova importazione o un nuovo scaricamento dovessero ripresentare le stesse righe
        //originarie (stesso giorno, stesso exchange, stesse quantità), la deduplica non le riconoscerebbe
        //più: nessun movimento in mappa ha più quelle quantità singole dopo la fusione
        Dettagli.append("<br><b>Attenzione:</b> se in futuro reimporti lo stesso file o riscarichi lo "
                + "stesso periodo da API, questi movimenti torneranno ad essere riconosciuti come nuovi e "
                + "si aggiungeranno a quello unificato, duplicando l'importo.<br>");

        if (PrezziDiversi) {
            Dettagli.append("<br><b>Attenzione:</b> i movimenti selezionati hanno un prezzo unitario "
                    + "diverso tra loro. Unendoli, il costo di carico diventa un unico valore medio per "
                    + "l'intera quantità: se in futuro verrà prelevata solo una parte di questa giacenza, "
                    + "il calcolo LIFO userà quel costo medio invece del costo specifico di ciascun "
                    + "movimento originale, con un possibile effetto sulla plusvalenza calcolata.<br>");
        }
        if (!TuttiValorizzati) {
            Dettagli.append("<br>Non tutti i movimenti selezionati hanno un prezzo: il movimento "
                    + "risultante verrà lasciato non valorizzato e andrà riprezzato.<br>");
        }
        if (AltriMovimentiIntercalati(Gruppo, Uscita)) {
            Dettagli.append("<br><b>Attenzione:</b> tra i movimenti selezionati sono registrati altri "
                    + "movimenti della stessa moneta sullo stesso wallet. Il movimento unito verrà collocato "
                    + "all'istante dell'ultimo dei movimenti selezionati, quindi dopo di essi: il calcolo "
                    + "LIFO li elaborerà in un ordine diverso da quello attuale.<br>");
        }
        if (DistintiNonVuoti(Gruppo, 41).size() > 1) {
            Dettagli.append("<br>I movimenti provengono da documenti di origine diversi: il movimento "
                    + "unito resterà collegato a uno solo di essi, gli altri perderanno il collegamento.<br>");
        }
        if (DistintiNonVuoti(Gruppo, 42).size() > 1) {
            Dettagli.append("<br>Più di un movimento ha uno storico delle modifiche proprio: verrà "
                    + "mantenuto solo quello di un movimento, lo storico precedente degli altri andrà perso "
                    + "(le loro righe attuali resteranno comunque nello storico del movimento unito).<br>");
        }
        Dettagli.append("<br>Si vuole proseguire?");

        AppDialog.DialogResult result = AppDialog.builder(owner)
                .windowTitle("Unione movimenti")
                .bodyTitle("Unire i movimenti selezionati in uno solo?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("I movimenti verranno sostituiti da un unico movimento con la quantità totale.")
                .details(Dettagli.toString())
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("unisci", "Unisci i movimenti")
                        .role(AppDialog.ActionRole.DANGER)
                        .build())
                .showDialog();

        if (result == null || !result.isAction("unisci")) {
            return false;
        }

        return EseguiUnioneOmogenei(Gruppo, owner);
    }

    /**
     * Esegue materialmente l'unione, una volta superati i controlli e ottenuta la conferma dell'utente.
     *
     * <p>Il movimento risultante è un <b>clone</b> del movimento più recente del gruppo (non una
     * ricostruzione via {@link MovimentiCrypto#creaMovimento}): a differenza della fusione in scambio, qui
     * i movimenti sono già classificati allo stesso modo, e ricostruirli con {@code TipoTr=null}
     * ricalcolerebbe categoria/campo 5/campo 18 dal solo tipo delle monete, perdendo classificazioni come
     * REWARD, CASHBACK o le sottotipologie custom (es. "PCO - CASHOUT O SIMILARE"). Il clone eredita quindi
     * verbatim tutti i campi di provenienza dal movimento base, e solo quantità, controvalore, nota e i
     * campi calcolati dal motore vengono toccati esplicitamente.</p>
     *
     * <p>Il controvalore è la <b>somma</b> dei controvalori originali, e non quello di una singola gamba
     * scelta come più affidabile: a differenza dello scambio, i movimenti qui non sono le due facce di una
     * stessa transazione ma eventi economici indipendenti. Se anche uno solo dei movimenti non è
     * valorizzato, il risultato resta non valorizzato: sommare un prezzo parziale darebbe un totale
     * silenziosamente sbagliato.</p>
     *
     * <p>Due campi non sono semplice output del motore e vengono quindi <b>sommati</b>, non svuotati (stessa
     * distinzione fatta da {@code Principale_TraslaOrario}): il 17 sui DDO, dove è il costo di carico della
     * donazione inserito a mano, e il 35, backup del prezzo di mercato che la classificazione ripristina
     * quando il movimento viene riportato alla situazione iniziale.</p>
     *
     * <p>Lo storico modifiche del movimento unito riceve una voce per <b>ogni</b> movimento di partenza,
     * con la sua riga originale: è ciò che rende l'unione ricostruibile, dato che le singole quantità
     * altrimenti sopravvivrebbero solo nella nota.</p>
     *
     * @param Gruppo movimenti da unire, così come restituiti da {@link #TrovaGruppoOmogeneo}
     * @param owner finestra su cui centrare l'eventuale messaggio di errore
     * @return {@code true} se l'unione è stata effettuata
     */
    static boolean EseguiUnioneOmogenei(List<String[]> Gruppo, Window owner) {
        boolean Uscita = MonetaValida(Gruppo.get(0)[8], Gruppo.get(0)[10]);
        String MovimentoBase[] = MovimentoPiuRecente(Gruppo);
        String Nuovo[] = Arrays.copyOf(MovimentoBase, MovimentoBase.length);

        String QtaFusa = SommaQuantita(Gruppo, Uscita).stripTrailingZeros().toPlainString();
        if (Uscita) Nuovo[10] = QtaFusa; else Nuovo[13] = QtaFusa;

        //Se il movimento base non ha un campo di provenienza lo si prende dal primo che ce l'ha, come fa
        //la fusione in scambio: in particolare documento di origine (41) e lignaggio (42), che altrimenti
        //andrebbero persi solo perché il movimento più recente non li porta
        for (int Campo : CampiProvenienzaUnione) {
            if (Campo < Nuovo.length && Funzioni.noData(Nuovo[Campo])) {
                for (String M[] : Gruppo) {
                    if (Campo < M.length && !Funzioni.noData(M[Campo])) {
                        Nuovo[Campo] = M[Campo];
                        break;
                    }
                }
            }
        }

        boolean TuttiValorizzati = Gruppo.stream().allMatch(Principale_Movimenti_SeparaUnisci::Valorizzato);
        if (TuttiValorizzati) {
            BigDecimal SommaValore = BigDecimal.ZERO;
            for (String M[] : Gruppo) SommaValore = SommaValore.add(new BigDecimal(PrezzoSicuro(M[15])));
            Nuovo[15] = SommaValore.setScale(2, RoundingMode.HALF_UP).toPlainString();
            Nuovo[32] = "SI";
            //La fonte generica "Personalizzato" è la stessa convenzione usata quando il prezzo è imposto
            //dall'esterno: il totale appena calcolato non va più toccato da un'eventuale ri-prezzatura
            //automatica, che soprascriverebbe la somma con qta*prezzo di un singolo token
            Nuovo[40] = "|||Personalizzato";
        } else {
            Nuovo[15] = "0.00";
            Nuovo[32] = "NO";
            Nuovo[40] = "";
        }

        Nuovo[21] = NotaUnione(Gruppo, Uscita);
        Nuovo[22] = "M";

        //Campi calcolati dal motore delle plusvalenze: azzerati e non riportati con lo stato vecchio, li
        //ricalcola AggiornaPlusvalenze al prossimo giro
        for (int Campo : CampiMotoreDaSvuotare) Nuovo[Campo] = "";
        boolean IsDDO = Nuovo[18] != null && Nuovo[18].contains("DDO");
        Nuovo[17] = IsDDO ? SommaCampo(Gruppo, 17, -1) : "";
        Nuovo[35] = DistintiNonVuoti(Gruppo, 35).isEmpty() ? "" : SommaCampo(Gruppo, 35, 15);

        Importazioni.RiempiVuotiArray(Nuovo);

        for (String M[] : Gruppo) MappaCryptoWallet.remove(M[0]);

        if (!InserisciMovimento(Nuovo)) {
            for (String M[] : Gruppo) MappaCryptoWallet.put(M[0], M);
            Esito Errore = ErroreIDUnivoco(Nuovo[0]);
            Messaggi.WarningMessage(Errore.Titolo, Errore.Messaggio, owner);
            return false;
        }

        //Storico: il movimento unito tiene un solo lignaggio e riceve una voce per ogni riga di partenza;
        //i lignaggi degli altri movimenti non sono più portati da nessuno e vanno ripuliti al salvataggio,
        //come per una cancellazione (altrimenti resterebbero nel database senza più un movimento collegato)
        String Lignaggio = MovimentiStorico.AssicuraLignaggio(Nuovo);
        for (String M[] : Gruppo) {
            MovimentiStorico.AccodaModifica(Lignaggio, Nuovo[0], M[0], Importazioni.SerializzaRiga(M), OP_UNIONE);
        }
        for (String AltroLignaggio : DistintiNonVuoti(Gruppo, MovimentiStorico.CAMPO_LIGNAGGIO)) {
            if (!AltroLignaggio.equals(Lignaggio)) MovimentiStorico.AccodaCancellazione(AltroLignaggio);
        }

        StringBuilder IDOriginali = new StringBuilder();
        for (String M[] : Gruppo) IDOriginali.append(M[0]).append(" ");
        LoggerGC.logInfo("Movimenti " + IDOriginali.toString().trim() + " uniti nel movimento " + Nuovo[0]);
        return true;
    }

    /** Etichetta dell'operazione nello storico modifiche (tradotta in {@code GUI_StoricoMovimento}). */
    static final String OP_UNIONE = "UnisciMovimenti";

    /** Oltre questo numero di movimenti il dialogo di conferma riassume invece di elencare. */
    private static final int RIGHE_MAX_DIALOGO_UNIONE = 10;

    /**
     * Campi di provenienza che, se vuoti sul movimento base, vengono presi dal primo movimento del gruppo
     * che li ha: causale originale, ID/blocco, hash, address, documento di origine e lignaggio.
     */
    private static final int[] CampiProvenienzaUnione = {7, 14, 23, 24, 30, 36, 37, 39, 41, 42};

    /**
     * Output del motore delle plusvalenze, da svuotare sul movimento unito: stesso elenco di
     * {@code Principale_TraslaOrario.CAMPI_MOTORE_DA_SVUOTARE}. Il 17 e il 35 sono trattati a parte.
     */
    private static final int[] CampiMotoreDaSvuotare = {16, 19, 31, 33, 38};

    /**
     * @return il movimento più recente del gruppo, cioè l'ultimo: {@link #TrovaGruppoOmogeneo} restituisce
     *         il gruppo già in ordine di ID, quindi a parità di secondo decide il resto dell'ID e non
     *         l'ordine in cui l'utente ha selezionato le righe
     */
    private static String[] MovimentoPiuRecente(List<String[]> Gruppo) {
        return Gruppo.get(Gruppo.size() - 1);
    }

    /** @return la somma algebrica delle quantità del lato movimentato (negativa per le uscite) */
    private static BigDecimal SommaQuantita(List<String[]> Gruppo, boolean Uscita) {
        BigDecimal Somma = BigDecimal.ZERO;
        for (String M[] : Gruppo) Somma = Somma.add(new BigDecimal((Uscita ? M[10] : M[13]).trim()));
        return Somma;
    }

    /**
     * Somma un campo numerico sui movimenti del gruppo.
     * @param Campo campo da sommare
     * @param Ripiego campo da usare al posto di {@code Campo} dove questo è vuoto, oppure {@code -1}
     * @return la somma a 2 decimali, oppure stringa vuota se anche un solo valore non è numerico: un
     *         totale parziale sarebbe un valore sbagliato che sembra giusto
     */
    private static String SommaCampo(List<String[]> Gruppo, int Campo, int Ripiego) {
        BigDecimal Somma = BigDecimal.ZERO;
        for (String M[] : Gruppo) {
            String Valore = M[Campo];
            if (Funzioni.noData(Valore) && Ripiego >= 0) Valore = M[Ripiego];
            if (!Funzioni.isNumeric(Valore, false)) return "";
            Somma = Somma.add(new BigDecimal(Valore.trim()));
        }
        return Somma.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * @return {@code true} se fra il primo e l'ultimo movimento del gruppo (in ordine di mappa, cioè
     *         nell'ordine in cui li elabora il LIFO) c'è un altro movimento, non selezionato, che muove la
     *         stessa moneta sullo stesso wallet: il movimento unito, collocato sull'ultimo, lo scavalcherà
     */
    static boolean AltriMovimentiIntercalati(List<String[]> Gruppo, boolean Uscita) {
        String Primo[] = Gruppo.get(0);
        String Ultimo[] = Gruppo.get(Gruppo.size() - 1);
        String Simbolo = Uscita ? Primo[8] : Primo[11];
        Set<String> IDGruppo = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String M[] : Gruppo) IDGruppo.add(M[0]);

        for (String M[] : MappaCryptoWallet.subMap(Primo[0], true, Ultimo[0], true).values()) {
            if (IDGruppo.contains(M[0])) continue;
            if (!CampoUguale(M[3], Primo[3])) continue;
            if (CampoUguale(M[8], Simbolo) || CampoUguale(M[11], Simbolo)) return true;
        }
        return false;
    }

    /**
     * Confronta i prezzi unitari (controvalore/quantità) dei movimenti del gruppo, già verificati tutti
     * valorizzati dal chiamante. Una tolleranza relativa dell'1% assorbe i normali arrotondamenti del
     * controvalore a 2 decimali senza mascherare una differenza di prezzo reale.
     * @return {@code true} se almeno un movimento ha un prezzo unitario che si scosta di oltre l'1% dal
     *         primo prezzo unitario valido incontrato
     */
    private static boolean PrezziUnitariDiversi(List<String[]> Gruppo, boolean Uscita) {
        BigDecimal Riferimento = null;
        for (String M[] : Gruppo) {
            BigDecimal Qta = new BigDecimal(Uscita ? M[10] : M[13]).abs();
            if (Qta.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal PrezzoUnitario = new BigDecimal(PrezzoSicuro(M[15])).divide(Qta, 12, RoundingMode.HALF_UP);
            if (Riferimento == null) {
                Riferimento = PrezzoUnitario;
            } else if (Riferimento.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal ScartoRelativo = PrezzoUnitario.subtract(Riferimento).abs().divide(Riferimento, 6, RoundingMode.HALF_UP);
                if (ScartoRelativo.compareTo(new BigDecimal("0.01")) > 0) return true;
            } else if (PrezzoUnitario.compareTo(BigDecimal.ZERO) != 0) {
                return true;
            }
        }
        return false;
    }

    /** @return i valori distinti, non vuoti, del campo indicato tra i movimenti del gruppo */
    private static Set<String> DistintiNonVuoti(List<String[]> Gruppo, int Campo) {
        Set<String> Valori = new LinkedHashSet<>();
        for (String M[] : Gruppo) {
            if (Campo < M.length && !Funzioni.noData(M[Campo])) Valori.add(M[Campo].trim());
        }
        return Valori;
    }

    /**
     * Costruisce la nota del movimento unito: una frase riassuntiva sempre presente (così l'unione resta
     * tracciabile anche senza consultare il log), seguita dalle note originali distinte concatenate e,
     * quando i movimenti fusi portano hash/causali diversi in campo 24, dal loro elenco — quel campo ne
     * può contenere solo uno, quindi gli altri andrebbero altrimenti persi senza lasciare traccia.
     */
    private static String NotaUnione(List<String[]> Gruppo, boolean Uscita) {
        String DataBase = DataID(MovimentoPiuRecente(Gruppo));
        String Orario = DataBase.length() == 14
                ? DataBase.substring(8, 10) + ":" + DataBase.substring(10, 12) + ":" + DataBase.substring(12, 14)
                : "";

        //Niente virgole né punti e virgola: sono i caratteri che MovimentiCrypto.normalizzaNome toglie
        //dalle note, e il ";" è il separatore di movimenti.crypto.db
        StringBuilder Riepilogo = new StringBuilder("Movimento generato unendo " + Gruppo.size() + " movimenti da ");
        for (int i = 0; i < Gruppo.size(); i++) {
            if (i > 0) Riepilogo.append(" + ");
            String Qta = Uscita ? Gruppo.get(i)[10] : Gruppo.get(i)[13];
            Riepilogo.append(new BigDecimal(Qta.trim()).abs().stripTrailingZeros().toPlainString());
        }
        if (!Orario.isBlank()) Riepilogo.append(" avvenuti alle ").append(Orario);
        Riepilogo.append(".");

        Set<String> NoteOriginali = new LinkedHashSet<>();
        for (String M[] : Gruppo) {
            if (!Funzioni.noData(M[21])) NoteOriginali.add(M[21].trim());
        }
        if (!NoteOriginali.isEmpty()) {
            Riepilogo.append(" Note originali: ").append(String.join(" - ", NoteOriginali)).append(".");
        }

        Set<String> HashDistinti = DistintiNonVuoti(Gruppo, 24);
        if (HashDistinti.size() > 1) {
            Riepilogo.append(" Riferimenti originali: ").append(String.join(" / ", HashDistinti)).append(".");
        }

        return Riepilogo.toString().replace(";", "");
    }
}
