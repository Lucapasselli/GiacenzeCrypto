/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.awt.Window;
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
 * tentata una ricerca prezzi online (che bloccherebbe l'EDT).</p>
 *
 * <p>Come per le altre classi {@code Principale_*}, i metodi sono statici, la classe non conserva alcun
 * riferimento a componenti Swing e l'aggiornamento delle tabelle resta a carico di {@link Principale}.</p>
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
     * Separa un movimento che coinvolge due monete in due movimenti distinti: un prelievo per la moneta
     * in uscita e un deposito per quella in entrata; il movimento originale viene eliminato.
     *
     * <p>Le due gambe ereditano dall'originale wallet, data/ora, prezzo, note, hash, rete, address dei
     * token e i campi di provenienza ({@link #CampiDaRiportare}); campo 5, campo 18 e categoria vengono
     * invece ricalcolati da {@link MovimentiCrypto#creaMovimento} in base al tipo delle monete, quindi i
     * due movimenti risultano <b>non classificati</b> e ricompaiono nella tabella "Depositi e Prelievi"
     * pronti per essere riclassificati. Il valore in euro dell'originale viene assegnato a entrambe le
     * gambe, perché è lo stesso controvalore visto una volta come uscita e una volta come entrata.</p>
     *
     * <p>L'ID del prelievo mantiene i quattro campi identificativi dell'originale (cambia solo la
     * categoria finale), mentre il deposito riceve una {@code A} sul terzo campo, la stessa convenzione
     * già usata da {@code GUI_ClassificazioneMovimento.CreaMovimentoTrasferimentoA} per garantire che
     * nell'ordinamento della mappa il prelievo preceda sempre il deposito.</p>
     *
     * @param ID ID del movimento da separare
     * @param owner finestra su cui centrare i dialoghi di conferma ed errore
     * @return {@code true} se la separazione è stata effettuata (il chiamante deve aggiornare le tabelle)
     */
    public static boolean SeparaInDepositoPrelievo(String ID, Window owner) {
        String Movimento[] = MappaCryptoWallet.get(ID);
        if (Movimento == null) return false;

        String IDSpezzato[] = Movimento[0].split("_");
        if (IDSpezzato.length < 5) {
            Messaggi.WarningMessage("ID movimento non valido",
                    "L'ID del movimento non ha il formato atteso e non può essere separato.", owner);
            return false;
        }

        if (!DueMoneteCoinvolte(Movimento)) {
            Messaggi.WarningMessage("Movimento non separabile",
                    "Il movimento non coinvolge due monete: non c'è nulla da separare.", owner);
            return false;
        }

        //I movimenti automatici non possono essere modificati strutturalmente: verrebbero comunque
        //rigenerati alla prossima importazione (stessa regola applicata da GUI_ModificaMovimento)
        if (Movimento[22] != null && Movimento[22].equalsIgnoreCase("AU")) {
            Messaggi.WarningMessage("Movimento automatico",
                    "Questo movimento è stato generato automaticamente dal programma e non può essere separato.<br>"
                    + "Su questo tipo di movimento è consentito variare solamente Prezzo e Note.", owner);
            return false;
        }

        //Se il movimento è collegato ad altri (campo 20) separarlo lascerebbe i movimenti collegati orfani
        if (Movimento[20] != null && !Movimento[20].isBlank()) {
            Messaggi.WarningMessage("Movimento collegato ad altri",
                    "Questo movimento è collegato ad altri movimenti e non può essere separato direttamente.<br>"
                    + "Riportalo prima alla situazione iniziale (ad esempio dalla modifica del movimento) e riprova.", owner);
            return false;
        }

        String MonetaUscita = NomeVisualizzato(Movimento[8], Movimento[25]);
        String MonetaEntrata = NomeVisualizzato(Movimento[11], Movimento[27]);

        AppDialog.DialogResult result = AppDialog.builder(owner)
                .windowTitle("Separazione del movimento")
                .bodyTitle("Separare il movimento in due?")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("Il movimento verrà sostituito da due movimenti distinti.")
                .details("Dal movimento <b>" + Movimento[5] + "</b> verranno creati:<br>"
                        + " - un <b>prelievo</b> di " + MonetaUscita + "<br>"
                        + " - un <b>deposito</b> di " + MonetaEntrata + "<br>"
                        + "Il movimento originale verrà eliminato e i due nuovi movimenti risulteranno "
                        + "da classificare nella scheda \"Depositi e Prelievi\".<br>"
                        + "Si vuole proseguire?")
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("separa", "Separa il movimento")
                        .role(AppDialog.ActionRole.DANGER)
                        .build())
                .showDialog();

        if (result == null || !result.isAction("separa")) {
            return false;
        }

        return EseguiSeparazione(Movimento, owner);
    }

    /**
     * Esegue materialmente la separazione, una volta superati i controlli e ottenuta la conferma
     * dell'utente: crea le due gambe, elimina l'originale e inserisce i nuovi movimenti in mappa.
     * @param Movimento movimento da separare
     * @param owner finestra su cui centrare l'eventuale messaggio di errore
     * @return {@code true} se la separazione è stata effettuata
     */
    static boolean EseguiSeparazione(String Movimento[], Window owner) {
        String IDSpezzato[] = Movimento[0].split("_");
        String Rete = Funzioni.TrovaReteDaIMovimento(Movimento);
        long Timestamp = FunzioniDate.ConvertiDataIDinLong(IDSpezzato[0]);

        String Prezzo = PrezzoSicuro(Movimento[15]);

        //Gamba in uscita: diventerà un prelievo (PC/PF) mantenendo i campi identificativi dell'ID originale
        Moneta MonetaOUT = CostruisciMoneta(Movimento[8], Movimento[9], Movimento[10], Movimento[25], Movimento[26], Rete, true);
        String IDPrelievo = IDSpezzato[0] + "_" + IDSpezzato[1] + "_" + IDSpezzato[2] + "_" + IDSpezzato[3] + "_" + IDSpezzato[4];

        //Gamba in entrata: diventerà un deposito (DC/DF), posizionato subito dopo il prelievo
        Moneta MonetaIN = CostruisciMoneta(Movimento[11], Movimento[12], Movimento[13], Movimento[27], Movimento[28], Rete, false);
        String IDDeposito = IDSpezzato[0] + "_" + IDSpezzato[1] + "_" + IDSpezzato[2] + "A_" + IDSpezzato[3] + "_" + IDSpezzato[4];

        String Prelievo[] = MovimentiCrypto.creaMovimento(
                MonetaOUT, null,
                Movimento[3], Movimento[4],
                Timestamp,
                Prezzo, null,
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
                Prezzo, null,
                1, 1,
                IDDeposito,
                Movimento[21],
                "M",
                Movimento[24],
                null,               //TipoTr null: descrizione e categoria dedotte dal tipo di moneta
                null
        );

        if (Prelievo == null || Deposito == null) {
            Messaggi.WarningMessage("Separazione non riuscita",
                    "Non è stato possibile costruire i due movimenti separati.<br>"
                    + "Il movimento originale è stato lasciato invariato.", owner);
            return false;
        }

        RiportaCampiOriginali(Prelievo, Movimento);
        RiportaCampiOriginali(Deposito, Movimento);

        //Prezzo, flag di valorizzazione e info prezzo vengono riportati verbatim dall'originale: entrambe
        //le gambe valgono quanto valeva il movimento di partenza (lo stesso controvalore, visto una volta
        //in uscita e una in entrata) e non devono ereditare la fonte "Personalizzato" di creaMovimento
        for (String Gamba[] : new String[][]{Prelievo, Deposito}) {
            Gamba[15] = Prezzo;
            Gamba[32] = Movimento[32];
            Gamba[40] = Movimento[40];
        }

        //Elimino l'originale prima di assegnare gli ID definitivi, così il suo ID torna disponibile
        MappaCryptoWallet.remove(Movimento[0]);

        if (!InserisciMovimento(Prelievo, owner)) {
            MappaCryptoWallet.put(Movimento[0], Movimento);
            return false;
        }
        if (!InserisciMovimento(Deposito, owner)) {
            //Tolgo la gamba già inserita e ripristino l'originale: meglio non lasciare il movimento a metà.
            //Il confronto è per identità perché, se l'ID non fosse quello appena assegnato, si rischierebbe
            //di cancellare un movimento di terzi che occupa lo stesso ID
            if (MappaCryptoWallet.get(Prelievo[0]) == Prelievo) MappaCryptoWallet.remove(Prelievo[0]);
            MappaCryptoWallet.put(Movimento[0], Movimento);
            return false;
        }

        LoggerGC.logInfo("Movimento " + Movimento[0] + " separato in prelievo " + Prelievo[0] + " e deposito " + Deposito[0]);
        return true;
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

        if (!InserisciMovimento(Scambio, owner)) {
            //Ripristino i due movimenti di partenza
            MappaCryptoWallet.put(Prelievo[0], Prelievo);
            MappaCryptoWallet.put(Deposito[0], Deposito);
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
     * @param owner finestra su cui centrare l'eventuale messaggio di errore
     * @return {@code true} se l'inserimento è riuscito
     */
    private static boolean InserisciMovimento(String Movimento[], Window owner) {
        String IDUnivoco = MovimentiCrypto.getIDUnivoco(MappaCryptoWallet, Movimento[0]);
        if (IDUnivoco == null) {
            Messaggi.WarningMessage("Errore generazione ID",
                    "Impossibile generare un ID univoco per il movimento " + Movimento[0] + ".<br>"
                    + "L'operazione è stata annullata.", owner);
            return false;
        }
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
