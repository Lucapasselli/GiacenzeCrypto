package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abbina i movimenti "Dual Savings Purchase"/"Dual Savings Settlement" di Binance (importati come
 * TRASFERIMENTO-CRYPTO, categoria PC/DC, campo18 vuoto) usando il file di dettaglio scaricabile dalla
 * sezione "Advanced Earn - Dual Investment History" dell'exchange (colonne: {@code Product, Order
 * Type, Product Id, Subscription Date, Type, Subscription Amount, Target Price, Settlement Date,
 * Fixing Price, APY, Settlement Amount, Status}).
 *
 * <p>Il CSV normale delle transazioni non porta alcun identificativo di contratto (solo {@code
 * User_ID,UTC_Time,Account,Operation,Coin,Change,Remark}): l'abbinamento automatico "scambio
 * differito" per tolleranza tempo/prezzo ({@link Importazioni#ConsolidaMovimentiDifferiti}) non regge
 * qui, perché fra Purchase e Settlement possono passare mesi. Il file di dettaglio dà invece, per ogni
 * contratto, sia l'importo sottoscritto sia quello liquidato: la coppia (moneta, quantità) individua i
 * candidati in {@link Principale#MappaCryptoWallet}, e la data del contratto (corretta per l'offset UTC
 * dichiarato nel nome del file, come {@code Importazioni.extractUtcOffsetBinance} fa per il CSV
 * principale) scioglie i casi in cui più candidati condividono la stessa coppia moneta/quantità nello
 * stesso periodo — frequente nel dataset reale (es. tre acquisti Buy Low da "100 USDT" in date diverse).
 *
 * <p>Il filtro decisivo è il campo {@code [7]} del movimento, che {@code ImportazioneGenerica}
 * valorizza con la causale grezza del CSV ({@code rt[7] = causaleCSV}): "TRASFERIMENTO-CRYPTO" è
 * condiviso con veri prelievi/depositi esterni (withdraw, deposit, Send, C2C Transfer...), quindi
 * cercare solo per categoria PC/DC rischierebbe di abbinare un trasferimento ordinario. Senza questo
 * filtro non c'è altro modo di distinguere le due cose una volta scritte in mappa.
 *
 * <p>Ogni contratto matcha <b>un solo</b> Purchase e <b>un solo</b> Settlement: un movimento già usato
 * da un contratto non viene riproposto a un altro. Un contratto ancora aperto (Status diverso da
 * "Settled") non ha un Settlement da abbinare e viene semplicemente contato, non è un errore.
 *
 * <p>La coppia trovata è trattata in due modi diversi a seconda che la moneta liquidata sia la stessa
 * di quella sottoscritta o no. Se è <b>diversa</b> (es. sottoscritto USDT, liquidato BTC) è una vera
 * permuta cripto-cripto e passa da {@link GUI_ClassificazioneMovimento#CreaMovimentiScambioCryptoDifferito}.
 * Se è <b>la stessa</b> (il caso più comune: il contratto scade "in favore" della moneta sottoscritta,
 * capitale + interesse nella stessa moneta) uno scambio non avrebbe senso fiscale — permuterebbe
 * l'intero importo liquidato invece di limitare l'effetto alla sola differenza — e la coppia passa
 * invece da {@link #CreaMovimentiDualInvestmentStessaMoneta}, che sposta il capitale come trasferimento
 * interno verso/da un sotto-wallet "Dual Savings" e isola la differenza come un movimento REWARD
 * indipendente.
 */
public class Binance_DualInvestment {

    static final String CAUSALE_PURCHASE = "Dual Savings Purchase";
    static final String CAUSALE_SETTLEMENT = "Dual Savings Settlement";

    //Campo 18 che l'abbinamento lascia sul Purchase/Settlement, da cui si riconosce un contratto già abbinato
    static final String CAMPO18_PURCHASE_STESSA_MONETA = "PTW - Trasferimento a Dual Investment";
    static final String CAMPO18_SETTLEMENT_STESSA_MONETA = "DTW - Trasferimento da Dual Investment";
    static final String CAMPO18_PURCHASE_DIFFERITO = "PTW - Scambio Differito";
    static final String CAMPO18_SETTLEMENT_DIFFERITO = "DTW - Scambio Differito";

    private static final Pattern OFFSET_PATTERN = Pattern.compile("UTC([+-]\\d+)", Pattern.CASE_INSENSITIVE);

    /** Esito dell'abbinamento, per il resoconto mostrato all'utente. */
    public static class Esito {
        public int contrattiTotali = 0;
        public int abbinati = 0;
        /** Contratti già abbinati da una versione precedente, a cui questo giro ha scritto il gruppo {@code DUAL-…}. */
        public int aggiornati = 0;
        /** Contratti già abbinati che portavano già il gruppo del contratto: nulla da fare. */
        public int giaAPosto = 0;
        public int ambigui = 0;
        public int nonTrovati = 0;
        public int nonAncoraLiquidati = 0;
        /** Contratti a moneta uguale già abbinati nella forma precedente al 2026-10-05, portati alla forma attuale. */
        public int migrati = 0;
        /** Contratti a moneta diversa il cui scambio differito era stato sovrascritto da un altro (bug C17) e ricostruito. */
        public int riparati = 0;
        /** Una riga per ogni contratto non abbinato (ambiguo o non trovato), per la diagnosi. */
        public List<String> dettagli = new ArrayList<>();
        /** Una riga per ogni scambio ricostruito, con la data: aggiunge una permuta in quell'anno. */
        public List<String> riparazioni = new ArrayList<>();
    }

    /**
     * Legge il file di dettaglio e abbina ogni contratto liquidato al suo Purchase/Settlement in
     * {@link Principale#MappaCryptoWallet}, chiamando {@link GUI_ClassificazioneMovimento#CreaMovimentiScambioCryptoDifferito}
     * sulla coppia trovata. Non ricalcola nulla: il chiamante deve invocare {@code Funzioni_AggiornaTutto()}
     * una sola volta a fine ciclo (vedi CLAUDE.md, "Mass operations must not reuse the single-item
     * function in a loop").
     * @param fileDettaglio il CSV "Advanced Earn - Dual Investment History" scaricato da Binance
     * @return il conteggio di quanti contratti sono stati abbinati, ambigui, non trovati o non ancora liquidati
     * @throws IOException se il file non è leggibile
     */
    public static Esito Abbina(File fileDettaglio) throws IOException {
        Esito esito = new Esito();
        //A differenza di ImportazioneGenerica.consolidaGruppo, che segnala solo le causali
        //effettivamente incontrate riga per riga, qui l'avviso fiscale sui derivati deve comparire
        //sempre: un Dual Investment è per definizione un contratto a termine, indipendentemente da
        //quanti contratti risultino poi abbinati.
        Importazioni.CausaliDerivatiSegnalate.clear();
        Importazioni.SegnalaCausaleDerivato(CAUSALE_PURCHASE);
        Importazioni.SegnalaCausaleDerivato(CAUSALE_SETTLEMENT);
        int offsetOre = estraiOffset(fileDettaglio.getName());
        Set<String> giaUsati = new HashSet<>();

        List<String[]> righe = leggiRighe(fileDettaglio);
        for (String[] campi : righe) {
            if (campi.length < 12) continue;
            esito.contrattiTotali++;

            String status = campi[11].trim();
            if (!status.equalsIgnoreCase("Settled")) {
                esito.nonAncoraLiquidati++;
                continue;
            }

            String[] sub = parseImportoMoneta(campi[5]);
            String[] settle = parseImportoMoneta(campi[10]);
            if (sub == null || settle == null) {
                esito.nonTrovati++;
                esito.dettagli.add("Prodotto " + campi[0] + " (id " + campi[2] + "): importo non interpretabile");
                continue;
            }

            long tsSub = convertiAEpocaUtc(campi[3].trim(), offsetOre);
            long tsSettle = convertiAEpocaUtc(campi[7].trim(), offsetOre);

            boolean stessaMoneta = sub[1].equalsIgnoreCase(settle[1]);
            String chiave = GruppoOperazione.ChiaveDual(campi[2]);
            //Un contratto già abbinato porta ancora le quantità del file, con un'eccezione: il Settlement a
            //moneta uguale abbinato prima del 2026-10-05 era ridotto al solo capitale (il resto stava nella reward)
            CandidatoRisultato purchase = trovaCandidato(sub[1], sub[0], List.of(sub[0]), tsSub, true, giaUsati, chiave,
                    stessaMoneta ? CAMPO18_PURCHASE_STESSA_MONETA : CAMPO18_PURCHASE_DIFFERITO);
            CandidatoRisultato settlement = trovaCandidato(settle[1], settle[0],
                    stessaMoneta ? List.of(settle[0], sub[0]) : List.of(settle[0]),
                    tsSettle, false, giaUsati, chiave,
                    stessaMoneta ? CAMPO18_SETTLEMENT_STESSA_MONETA : CAMPO18_SETTLEMENT_DIFFERITO);

            if (purchase.ambiguo || settlement.ambiguo) {
                esito.ambigui++;
                esito.dettagli.add("Prodotto " + campi[0] + " (id " + campi[2] + "): abbinamento ambiguo, "
                        + "più movimenti compatibili trovati - lasciato da classificare a mano");
                continue;
            }
            if (purchase.id == null || settlement.id == null) {
                esito.nonTrovati++;
                esito.dettagli.add("Prodotto " + campi[0] + " (id " + campi[2] + "): "
                        + (purchase.id == null ? "Purchase" : "Settlement") + " non trovato in archivio "
                        + "(CSV principale non ancora importato, o già abbinato a un altro contratto)");
                continue;
            }
            if (purchase.giaAbbinato != settlement.giaAbbinato) {
                esito.nonTrovati++;
                esito.dettagli.add("Prodotto " + campi[0] + " (id " + campi[2] + "): "
                        + (purchase.giaAbbinato ? "Purchase già abbinato ma Settlement ancora da classificare"
                                : "Settlement già abbinato ma Purchase ancora da classificare")
                        + " - lasciato com'è, da rivedere a mano");
                continue;
            }

            //Gli array in mappa, presi prima: nel caso a moneta diversa la rinumerazione cambia gli ID ma
            //riscrive gli stessi oggetti, e da [20] si risale ai movimenti generati
            String[] RigaPurchase = MappaCryptoWallet.get(purchase.id);
            String[] RigaSettlement = MappaCryptoWallet.get(settlement.id);
            giaUsati.add(purchase.id);
            giaUsati.add(settlement.id);
            if (purchase.giaAbbinato) {
                //Già abbinato da una versione precedente: i movimenti generati esistono e non vanno rifatti
                //(raddoppierebbero le gambe speculari). Si completa quello che allora non si faceva: la forma
                //attuale del caso a moneta uguale, lo scambio sovrascritto da un altro contratto (bug C17),
                //il gruppo del contratto. La riparazione va PRIMA del gruppo: col [20] ancora sbagliato il
                //gruppo di questo contratto finirebbe sui movimenti dell'altro.
                boolean Cambiato = false;
                if (stessaMoneta) {
                    switch (AggiornaFormaStessaMoneta(RigaPurchase, RigaSettlement)) {
                        case RIPARATO -> {
                            esito.migrati++;
                            Cambiato = true;
                        }
                        case FALLITO -> {
                            //Tipicamente un contratto della forma vecchia da cui è stata eliminata la sola reward
                            //(bug C18): il Settlement è ancora ridotto e il suo [20] punta a movimenti cancellati
                            esito.nonTrovati++;
                            esito.dettagli.add("Prodotto " + campi[0] + " (id " + campi[2] + "): contratto abbinato da "
                                    + "una versione precedente con movimenti collegati mancanti o inattesi - da sistemare "
                                    + "a mano (annullare la classificazione e riportare il Settlement all'importo liquidato "
                                    + settle[0] + " " + settle[1] + ")");
                            continue;
                        }
                        case NON_SERVE -> {
                        }
                    }
                } else {
                    switch (MovimentiCollegati.RiparaScambioDifferito(RigaPurchase, RigaSettlement, WALLET_DUAL_SAVINGS)) {
                        case RIPARATO -> {
                            esito.riparati++;
                            esito.riparazioni.add("Contratto " + campi[2] + ": ricostruito lo scambio " + sub[1] + " -> "
                                    + settle[1] + " del " + RigaSettlement[1]);
                            Cambiato = true;
                        }
                        case FALLITO -> {
                            //Il [20] cita ancora i movimenti dell'altro contratto: scrivere il gruppo adesso li
                            //porterebbe in questo, quindi il contratto resta del tutto com'era
                            esito.nonTrovati++;
                            esito.dettagli.add("Prodotto " + campi[0] + " (id " + campi[2] + "): lo scambio "
                                    + "differito era stato sovrascritto da un altro contratto e non si è potuto ricostruire "
                                    + "- lasciato com'è, dettagli nel log");
                            continue;
                        }
                        case NON_SERVE -> {
                        }
                    }
                }
                if (MarcaContratto(campi[2], RigaPurchase, RigaSettlement)) Cambiato = true;
                if (Cambiato) esito.aggiornati++;
                else esito.giaAPosto++;
                continue;
            }
            if (stessaMoneta && new BigDecimal(settle[0]).compareTo(new BigDecimal(sub[0])) < 0) {
                //Con la stessa moneta un contratto non liquida meno del sottoscritto: se succede i dati non sono
                //quelli attesi, e inventare movimenti sarebbe peggio che lasciarlo da classificare
                esito.nonTrovati++;
                esito.dettagli.add("Prodotto " + campi[0] + " (id " + campi[2] + "): liquidato meno del sottoscritto "
                        + "nella stessa moneta - lasciato da classificare a mano");
                continue;
            }
            if (stessaMoneta) {
                if (!CreaMovimentiDualInvestmentStessaMoneta(purchase.id, settlement.id)) {
                    esito.nonTrovati++;
                    esito.dettagli.add("Prodotto " + campi[0] + " (id " + campi[2] + "): impossibile creare i movimenti "
                            + "del contratto - lasciato da classificare, dettagli nel log");
                    continue;
                }
            } else {
                //Stesso sotto-wallet del caso a moneta uguale, così i movimenti sintetici di un Dual
                //Investment si distinguono dagli altri scambi differiti (Auto-Invest ecc.)
                if (!GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(purchase.id, settlement.id, WALLET_DUAL_SAVINGS)) {
                    esito.nonTrovati++;
                    esito.dettagli.add("Prodotto " + campi[0] + " (id " + campi[2] + "): impossibile generare gli ID "
                            + "dello scambio differito - lasciato da classificare, dettagli nel log");
                    continue;
                }
            }
            MarcaContratto(campi[2], RigaPurchase, RigaSettlement);
            esito.abbinati++;
        }
        return esito;
    }

    static final String WALLET_DUAL_SAVINGS = "Dual Savings";

    /**
     * Scrive la chiave di operazione del contratto ({@link GruppoOperazione}, campo 45) su Purchase e Settlement, e
     * solo su loro. I movimenti che l'abbinamento ha generato (gambe sul sotto-wallet, reward, o i tre dello scambio
     * differito) non la portano: fanno parte del gruppo {@code [20]} di Purchase e Settlement e la ricavano da lì
     * ({@link GruppoOperazione#ChiaveEffettiva}), così una riclassificazione non può lasciarla a metà. Solo informativo:
     * nessun calcolo legge il campo.
     *
     * <p>Le commissioni collegate (campo 43) non si toccano: una commissione collegata al Purchase resta collegata al
     * Purchase. Fino al 2026-10-05 la chiave del contratto stava nello stesso campo e la commissione veniva fusa nel
     * gruppo del contratto, perdendo il movimento a cui apparteneva.
     *
     * @param IdContratto id del contratto, colonna 3 del CSV di dettaglio; se vuoto non si scrive nulla
     * @return {@code true} se almeno una riga ha cambiato chiave (false se il contratto era già marcato)
     */
    static boolean MarcaContratto(String IdContratto, String[] Purchase, String[] Settlement) {
        String Chiave = GruppoOperazione.ChiaveDual(IdContratto);
        if (Chiave.isEmpty() || Purchase == null || Settlement == null) return false;
        boolean Cambiata = false;
        for (String[] v : new String[][]{Purchase, Settlement}) {
            if (!Chiave.equals(GruppoOperazione.Chiave(v))) {
                GruppoOperazione.Scrivi(v, Chiave);
                Cambiata = true;
            }
        }
        return Cambiata;
    }

    /**
     * Abbina Purchase e Settlement di un contratto liquidato <b>nella stessa moneta sottoscritta</b>
     * (il caso più comune: capitale + interesse tornano nella valuta di partenza). Uno scambio
     * differito qui permuterebbe l'intero importo liquidato invece di limitare l'effetto fiscale alla
     * sola differenza, quindi si tratta invece come un giroconto verso/da un sotto-wallet dedicato
     * ({@link #WALLET_DUAL_SAVINGS}), sullo stesso schema TI (nessun effetto sul LIFO, stesso gruppo
     * wallet di {@link Principale_GiacenzeaData}) già usato per Vault/Piattaforma in
     * {@code GUI_ClassificazioneMovimento.CreaMovimentoTrasferimentoA/Da}:
     * <ul>
     *   <li><b>Purchase</b> (prelievo dal wallet principale): campo 5/18 diventano "PTW - Trasferimento a
     *       Dual Investment", quantità e valore restano quelli importati. Gamba speculare in entrata sul
     *       sotto-wallet per la quantità sottoscritta;</li>
     *   <li><b>reward</b>, solo se la liquidazione ha reso più del sottoscritto: la differenza esatta entra
     *       <b>sul sotto-wallet</b>, prezzata in proporzione al valore già calcolato all'import per l'intero
     *       Settlement (stesso prezzo unitario, nessuna nuova ricerca prezzo);</li>
     *   <li><b>Settlement</b> (deposito sul wallet principale): campo 5/18 diventano "DTW - Trasferimento da
     *       Dual Investment", quantità e valore restano quelli importati. Gamba speculare in uscita dal
     *       sotto-wallet per l'<b>intero</b> liquidato, che il sotto-wallet ha: capitale più reward.</li>
     * </ul>
     *
     * <p><b>Purchase e Settlement non vengono mai modificati nelle quantità</b> (dal 2026-10-05). Prima il
     * Settlement era ridotto al capitale e la reward stava sul wallet principale: annullare la classificazione
     * dal Settlement lasciava la quantità ridotta e perdeva la reward (bug C18), e il Settlement ridotto non
     * coincideva più con la riga del CSV nella deduplica dei reimport. Ora annullare toglie solo i movimenti
     * generati. Fiscalmente non cambia nulla: il sotto-wallet ha lo stesso exchange, quindi lo stesso gruppo
     * wallet, e il LIFO vede la stessa reward con lo stesso valore nello stesso istante. I contratti abbinati
     * prima si portano a questa forma con {@link #AggiornaFormaStessaMoneta}.
     *
     * <p>Tutti i movimenti del contratto formano <b>un solo gruppo {@code [20]}</b>, ognuno con l'elenco
     * completo degli altri: annullare o eliminare uno qualunque riporta l'intero contratto allo stato
     * importato, e ripassando il file di dettaglio lo si riabbina. Nello stesso gruppo wallet nessun lettore di
     * {@code [20]} sposta costi.
     *
     * <p>Nel sotto-wallet la reward e l'uscita cadono nello stesso secondo: l'ID della reward ({@code 00} davanti
     * al terzo segmento) ordina prima di quello dell'uscita ({@code 0}), così il controllo delle giacenze
     * negative, che scorre la mappa per ID, non vede mai il sotto-wallet sotto zero.
     *
     * @param IDPurchase ID del movimento di sottoscrizione (prelievo dal wallet principale)
     * @param IDSettlement ID del movimento di liquidazione (deposito sul wallet principale)
     * @return {@code false} se non è stato fatto nulla: movimenti inesistenti, liquidato minore del
     *         sottoscritto, o un ID dei movimenti generati già occupato
     */
    static boolean CreaMovimentiDualInvestmentStessaMoneta(String IDPurchase, String IDSettlement) {
        String[] MovPurchase = MappaCryptoWallet.get(IDPurchase);
        String[] MovSettlement = MappaCryptoWallet.get(IDSettlement);
        if (MovPurchase == null || MovSettlement == null) return false;

        BigDecimal QtaSottoscritta = new BigDecimal(MovPurchase[10]).abs();
        BigDecimal QtaLiquidata = new BigDecimal(MovSettlement[13]).abs();
        BigDecimal ValoreLiquidato = new BigDecimal(MovSettlement[15]);
        BigDecimal Reward = QtaLiquidata.subtract(QtaSottoscritta);
        String[] IDSpezzatoP = MovPurchase[0].split("_");
        String[] IDSpezzatoS = MovSettlement[0].split("_");
        String IDMirrorPurchase = IDSpezzatoP[0] + "_" + IDSpezzatoP[1] + "_" + IDSpezzatoP[2] + "A_" + IDSpezzatoP[3] + "_DC";
        String IDMirrorSettlement = IDSpezzatoS[0] + "_" + IDSpezzatoS[1] + "_0" + IDSpezzatoS[2] + "_" + IDSpezzatoS[3] + "_PC";
        String IDReward = Reward.signum() > 0
                ? IDSpezzatoS[0] + "_" + IDSpezzatoS[1] + "_00" + IDSpezzatoS[2] + "_" + IDSpezzatoS[3] + "_DC" : "";
        if (Reward.signum() < 0) {
            LoggerGC.ScriviErrore("Dual Investment: liquidato (" + QtaLiquidata + ") meno del sottoscritto ("
                    + QtaSottoscritta + ") per " + IDSettlement + ", contratto non abbinato");
            return false;
        }
        if (MappaCryptoWallet.containsKey(IDMirrorPurchase) || MappaCryptoWallet.containsKey(IDMirrorSettlement)
                || (!IDReward.isEmpty() && MappaCryptoWallet.containsKey(IDReward))) {
            LoggerGC.ScriviErrore("Dual Investment: ID dei movimenti generati già occupato per " + IDPurchase
                    + " / " + IDSettlement + ", contratto non abbinato");
            return false;
        }

        // ── Purchase -> gamba speculare in entrata sul sotto-wallet "Dual Savings" ──
        Moneta MonetaINSpec = new Moneta();
        MonetaINSpec.Moneta = MovPurchase[8];
        MonetaINSpec.Tipo = MovPurchase[9];
        MonetaINSpec.Qta = QtaSottoscritta.stripTrailingZeros().toPlainString();
        if (MovPurchase.length > 29) {
            MonetaINSpec.NomeEsteso = MovPurchase[25];
            MonetaINSpec.MonetaAddress = MovPurchase[26];
        }
        String[] MTPurchase = MovimentiCrypto.creaMovimento(
                null, MonetaINSpec,
                MovPurchase[3], WALLET_DUAL_SAVINGS,
                FunzioniDate.ConvertiDataIDinLong(IDSpezzatoP[0]),
                MovPurchase[15], null,
                1, 1,
                null, null, "AU",
                MovPurchase.length > 29 ? MovPurchase[24] : null,
                null, null
        );
        MTPurchase[0] = IDMirrorPurchase;
        MTPurchase[1] = MovPurchase[1];
        MTPurchase[2] = "1 di 1";
        MTPurchase[5] = "TRASFERIMENTO INTERNO";
        MTPurchase[15] = MovPurchase[15];
        MTPurchase[18] = "DTW - Trasferimento Interno";
        MTPurchase[32] = "";
        MTPurchase[40] = "";
        if (MovPurchase.length > 29) {
            MTPurchase[23] = MovPurchase[23];
            MTPurchase[29] = MovPurchase[29];
        }
        //Tipo di derivato (campo 44): le gambe generate appartengono alla stessa operazione Dual
        Derivati.Marca(MTPurchase, Derivati.Tipo(MovPurchase));
        //Documento di origine: ogni generato prende quello del movimento da cui nasce
        MTPurchase[41] = MovPurchase[41];

        // ── Reward sul sotto-wallet, per la differenza esatta ──
        String[] MTReward = null;
        if (Reward.signum() > 0) {
            String ValoreReward = Reward.divide(QtaLiquidata, VarStatiche.DecimaliCalcoli, RoundingMode.HALF_UP)
                    .multiply(ValoreLiquidato).setScale(2, RoundingMode.HALF_UP).toPlainString();
            Moneta MonetaINReward = new Moneta();
            MonetaINReward.Moneta = MovSettlement[11];
            MonetaINReward.Tipo = MovSettlement[12];
            MonetaINReward.Qta = Reward.stripTrailingZeros().toPlainString();
            if (MovSettlement.length > 29) {
                MonetaINReward.NomeEsteso = MovSettlement[27];
                MonetaINReward.MonetaAddress = MovSettlement[28];
            }
            MTReward = MovimentiCrypto.creaMovimento(
                    null, MonetaINReward,
                    MovSettlement[3], WALLET_DUAL_SAVINGS,
                    FunzioniDate.ConvertiDataIDinLong(IDSpezzatoS[0]),
                    ValoreReward, null,
                    1, 1,
                    null, null, "AU",
                    null, null, null
            );
            MTReward[0] = IDReward;
            MTReward[1] = MovSettlement[1];
            MTReward[2] = "1 di 1";
            MTReward[5] = "REWARD";
            MTReward[15] = ValoreReward;
            MTReward[18] = CAMPO18_REWARD;
            MTReward[32] = "";
            MTReward[40] = "";
            if (MovSettlement.length > 29) {
                MTReward[29] = MovSettlement[29];
            }
            Derivati.Marca(MTReward, Derivati.Tipo(MovSettlement));
            MTReward[41] = MovSettlement[41];
        }

        // ── Settlement -> gamba speculare in uscita dal sotto-wallet per l'intero liquidato ──
        Moneta MonetaOUTSpec = new Moneta();
        MonetaOUTSpec.Moneta = MovSettlement[11];
        MonetaOUTSpec.Tipo = MovSettlement[12];
        MonetaOUTSpec.Qta = QtaLiquidata.negate().stripTrailingZeros().toPlainString();
        if (MovSettlement.length > 29) {
            MonetaOUTSpec.NomeEsteso = MovSettlement[27];
            MonetaOUTSpec.MonetaAddress = MovSettlement[28];
        }
        String[] MTSettlement = MovimentiCrypto.creaMovimento(
                MonetaOUTSpec, null,
                MovSettlement[3], WALLET_DUAL_SAVINGS,
                FunzioniDate.ConvertiDataIDinLong(IDSpezzatoS[0]),
                MovSettlement[15], null,
                1, 1,
                null, null, "AU",
                MovSettlement.length > 29 ? MovSettlement[24] : null,
                null, null
        );
        MTSettlement[0] = IDMirrorSettlement;
        MTSettlement[1] = MovSettlement[1];
        MTSettlement[2] = "1 di 1";
        MTSettlement[5] = "TRASFERIMENTO INTERNO";
        MTSettlement[15] = MovSettlement[15];
        MTSettlement[18] = "PTW - Trasferimento Interno";
        MTSettlement[32] = "";
        MTSettlement[40] = "";
        if (MovSettlement.length > 29) {
            MTSettlement[23] = MovSettlement[23];
            MTSettlement[29] = MovSettlement[29];
        }
        Derivati.Marca(MTSettlement, Derivati.Tipo(MovSettlement));
        MTSettlement[41] = MovSettlement[41];

        MovPurchase[5] = "TRASFERIMENTO A DUAL INVESTMENT";
        MovPurchase[18] = CAMPO18_PURCHASE_STESSA_MONETA;
        MovSettlement[5] = "TRASFERIMENTO DA DUAL INVESTMENT";
        MovSettlement[18] = CAMPO18_SETTLEMENT_STESSA_MONETA;

        List<String[]> Contratto = new ArrayList<>(List.of(MovPurchase, MTPurchase, MovSettlement, MTSettlement));
        if (MTReward != null) Contratto.add(MTReward);
        CollegaTutti(Contratto);
        MappaCryptoWallet.put(IDMirrorPurchase, MTPurchase);
        MappaCryptoWallet.put(IDMirrorSettlement, MTSettlement);
        if (MTReward != null) MappaCryptoWallet.put(IDReward, MTReward);
        return true;
    }

    /** Campo 18 della reward di un contratto a moneta uguale, da cui la si riconosce. */
    static final String CAMPO18_REWARD = "DAI - Reward Dual Investment";

    /**
     * Scrive su ogni riga il {@code [20]} con gli ID di tutte le altre: il gruppo completo e simmetrico che
     * l'annullamento della classificazione si aspetta.
     * @return {@code true} se almeno un {@code [20]} è cambiato
     */
    private static boolean CollegaTutti(List<String[]> Righe) {
        boolean Cambiato = false;
        for (String[] v : Righe) {
            StringBuilder Altri = new StringBuilder();
            for (String[] a : Righe) {
                if (a == v) continue;
                if (Altri.length() > 0) Altri.append(',');
                Altri.append(a[0]);
            }
            if (!Altri.toString().equals(v[20])) {
                v[20] = Altri.toString();
                Cambiato = true;
            }
        }
        return Cambiato;
    }

    /** @return le righe in mappa degli ID elencati nei {@code [20]} indicati, senza doppioni; {@code null} se uno manca */
    private static List<String[]> Collegati(String[]... Righe) {
        List<String[]> Ris = new ArrayList<>();
        for (String[] r : Righe) {
            if (r[20] == null || r[20].isBlank()) continue;
            for (String ID : r[20].split(",")) {
                if (ID.isBlank()) continue;
                String[] v = MappaCryptoWallet.get(ID.trim());
                if (v == null) return null;
                if (!Ris.contains(v) && !java.util.Arrays.asList(Righe).contains(v)) Ris.add(v);
            }
        }
        return Ris;
    }

    /**
     * Porta un contratto a moneta uguale già abbinato alla forma attuale di
     * {@link #CreaMovimentiDualInvestmentStessaMoneta}. Prima del 2026-10-05 il Settlement era ridotto al
     * capitale, la reward stava sul wallet principale, la gamba in uscita dal sotto-wallet portava solo il
     * capitale e i {@code [20]} andavano in un verso solo (bug C18). Qui:
     * <ul>
     *   <li>il Settlement riprende l'intero liquidato, capitale più reward, e il valore somma dei due (al
     *       centesimo: capitale e reward erano stati arrotondati ciascuno per conto suo);</li>
     *   <li>la reward passa sul sotto-wallet {@link #WALLET_DUAL_SAVINGS}, con la stessa quantità e lo stesso
     *       valore;</li>
     *   <li>la gamba in uscita dal sotto-wallet porta l'intero liquidato;</li>
     *   <li>i cinque movimenti formano un solo gruppo {@code [20]}.</li>
     * </ul>
     * Non cambia nessun risultato fiscale: tutto avviene nello stesso gruppo wallet, dove i trasferimenti
     * interni non spostano costi e il motore non legge il valore del Settlement. Rilanciabile: su un contratto
     * già nella forma attuale non fa nulla. Lavora solo sulle righe, senza il file di dettaglio, così lo si
     * può richiamare anche da altri punti.
     * @return {@link MovimentiCollegati.EsitoRiparazione#RIPARATO} se qualcosa è cambiato, {@link MovimentiCollegati.EsitoRiparazione#NON_SERVE} se era
     *         già nella forma attuale, {@link MovimentiCollegati.EsitoRiparazione#FALLITO} se i movimenti collegati mancano o non sono
     *         quelli attesi (nulla viene toccato)
     */
    static MovimentiCollegati.EsitoRiparazione AggiornaFormaStessaMoneta(String[] Purchase, String[] Settlement) {
        if (Purchase == null || Settlement == null) return MovimentiCollegati.EsitoRiparazione.FALLITO;
        List<String[]> Generati = Collegati(Purchase, Settlement);
        if (Generati == null) {
            LoggerGC.ScriviErrore("Dual Investment: contratto " + Purchase[0] + " / " + Settlement[0]
                    + " con movimenti collegati mancanti, forma non aggiornata");
            return MovimentiCollegati.EsitoRiparazione.FALLITO;
        }
        String[] Reward = null, MirrorUscita = null, MirrorEntrata = null;
        for (String[] v : Generati) {
            if (CAMPO18_REWARD.equalsIgnoreCase(v[18])) Reward = v;
            else if (WALLET_DUAL_SAVINGS.equals(v[4]) && v[18].contains("PTW")) MirrorUscita = v;
            else if (WALLET_DUAL_SAVINGS.equals(v[4]) && v[18].contains("DTW")) MirrorEntrata = v;
        }
        if (MirrorUscita == null || MirrorEntrata == null || Generati.size() != (Reward == null ? 2 : 3)) {
            LoggerGC.ScriviErrore("Dual Investment: contratto " + Purchase[0] + " / " + Settlement[0]
                    + " con movimenti collegati inattesi, forma non aggiornata");
            return MovimentiCollegati.EsitoRiparazione.FALLITO;
        }
        boolean Cambiato = false;
        if (Reward != null && !WALLET_DUAL_SAVINGS.equals(Reward[4])) {
            //Forma precedente: il Settlement riprende la reward, che passa sul sotto-wallet
            Settlement[13] = new BigDecimal(Settlement[13]).add(new BigDecimal(Reward[13])).toPlainString();
            Settlement[15] = new BigDecimal(Settlement[15]).add(new BigDecimal(Reward[15]))
                    .setScale(2, RoundingMode.HALF_UP).toPlainString();
            Reward[4] = WALLET_DUAL_SAVINGS;
            Cambiato = true;
        }
        //L'uscita dal sotto-wallet porta l'intero liquidato
        BigDecimal Liquidato = new BigDecimal(Settlement[13]).abs();
        if (new BigDecimal(MirrorUscita[10]).abs().compareTo(Liquidato) != 0
                || !MirrorUscita[15].equals(Settlement[15])) {
            MirrorUscita[10] = Liquidato.negate().toPlainString();
            MirrorUscita[15] = Settlement[15];
            Cambiato = true;
        }
        List<String[]> Contratto = new ArrayList<>(List.of(Purchase, MirrorEntrata, Settlement, MirrorUscita));
        if (Reward != null) Contratto.add(Reward);
        if (CollegaTutti(Contratto)) Cambiato = true;
        return Cambiato ? MovimentiCollegati.EsitoRiparazione.RIPARATO : MovimentiCollegati.EsitoRiparazione.NON_SERVE;
    }

    /** @return {@code true} se {@code Qta} coincide numericamente con una delle quantità indicate */
    private static boolean QuantitaFraQuelle(BigDecimal Qta, List<String> Quantita) {
        for (String q : Quantita) if (Qta.compareTo(new BigDecimal(q)) == 0) return true;
        return false;
    }

    private static class CandidatoRisultato {
        String id;
        boolean ambiguo;
        /** Il movimento è già stato abbinato (campo 18 PTW/DTW del Dual Investment), non è da classificare. */
        boolean giaAbbinato;
    }

    /**
     * Cerca in {@link Principale#MappaCryptoWallet} il movimento Dual Savings non ancora classificato
     * che corrisponde a {@code moneta}/{@code quantita}, più vicino nel tempo a {@code tsCercato}.
     * @param moneta moneta del movimento cercato
     * @param quantita quantità assoluta (senza segno) del movimento cercato
     * @param tsCercato istante approssimato (epoca UTC) del contratto, per scegliere fra più candidati
     * @param purchase {@code true} per un Purchase (categoria PC), {@code false} per un Settlement (categoria DC)
     * @param giaUsati ID già assegnati a un altro contratto in questa stessa esecuzione
     * @param quantitaGiaAbbinata quantità che il movimento può portare <b>dopo</b> l'abbinamento (vedi sotto):
     *        più d'una solo per il Settlement a moneta uguale, ridotto al capitale prima del 2026-10-05
     * @param chiaveContratto chiave {@code DUAL-…} del contratto cercato
     * @param campo18GiaAbbinato campo 18 che il movimento porta se è già stato abbinato
     * <p>Fra i candidati entrano anche i movimenti già abbinati da una versione precedente, riconosciuti dal
     * campo 18: servono a completare il gruppo del contratto, che allora non si scriveva. Quelli che portano
     * già il gruppo di un <i>altro</i> contratto sono esclusi, quello che porta proprio il gruppo cercato
     * vince su ogni altro (un secondo giro dello stesso file non sceglie mai diversamente dal primo).
     */
    private static CandidatoRisultato trovaCandidato(String moneta, String quantitaStr, List<String> quantitaGiaAbbinata,
            long tsCercato, boolean purchase, Set<String> giaUsati, String chiaveContratto, String campo18GiaAbbinato) {
        BigDecimal quantita = new BigDecimal(quantitaStr);
        String causaleAttesa = purchase ? CAUSALE_PURCHASE : CAUSALE_SETTLEMENT;
        String migliore = null;
        long migliorScarto = Long.MAX_VALUE;
        boolean migliorGiaAbbinato = false;
        int candidatiEntroTolleranza = 0;

        for (String[] mov : MappaCryptoWallet.values()) {
            if (mov == null || mov.length <= 18 || giaUsati.contains(mov[0])) continue;
            if (mov.length <= 7 || mov[7] == null || !mov[7].equalsIgnoreCase(causaleAttesa)) continue;
            boolean candidato = purchase
                    ? Importazioni.EPrelievoDaClassificare(mov)
                    : Importazioni.EDepositoDaClassificare(mov);
            boolean giaAbbinato = !candidato && campo18GiaAbbinato.equalsIgnoreCase(mov[18] == null ? "" : mov[18].trim());
            if (!candidato && !giaAbbinato) continue;
            String chiaveMov = GruppoOperazione.Chiave(mov);
            boolean chiavePropria = giaAbbinato && !chiaveContratto.isEmpty() && chiaveContratto.equals(chiaveMov);
            if (giaAbbinato && GruppoOperazione.isContrattoDual(chiaveMov) && !chiavePropria) continue;

            String monetaMov = purchase ? mov[8] : mov[11];
            String quantitaMov = purchase ? mov[10] : mov[13];
            if (monetaMov == null || !monetaMov.equalsIgnoreCase(moneta)) continue;
            BigDecimal qtaMov;
            try {
                qtaMov = new BigDecimal(quantitaMov).abs();
            } catch (Exception ex) {
                continue;
            }
            if (giaAbbinato ? !QuantitaFraQuelle(qtaMov, quantitaGiaAbbinata) : qtaMov.compareTo(quantita) != 0) continue;

            // Il campo data visualizzato ([1]) non ha i secondi ("yyyy-MM-dd HH:mm"): ConvertiDatainLongSecondo
            // (che li richiede) falliva silenziosamente tornando 0 per ogni riga - da qui, prima di questa
            // correzione, 0 abbinamenti su 13 contratti sul dataset reale (candidati sempre "equidistanti" o
            // fuori da qualunque tolleranza). Il prefisso dell'ID (yyyyMMddHHmmss) porta invece i secondi
            // veri: necessario per distinguere due Purchase nello stesso minuto (frequente: due acquisti
            // Buy Low a pochi secondi di distanza), che con la sola granularità al minuto restano ambigui.
            long tsMov = mov[0].length() >= 14
                    ? FunzioniDate.ConvertiDataIDinLong(mov[0].substring(0, 14))
                    : FunzioniDate.ConvertiDatainLongMinuto(mov[1]);
            long scarto = chiavePropria ? -1 : Math.abs(tsMov - tsCercato);
            if (scarto < migliorScarto) {
                migliorScarto = scarto;
                migliore = mov[0];
                migliorGiaAbbinato = giaAbbinato;
                candidatiEntroTolleranza = 1;
            } else if (scarto == migliorScarto) {
                candidatiEntroTolleranza++;
            }
        }

        // Nessun tetto massimo sulla distanza: la data del "Settlement" nel file di dettaglio è la
        // scadenza nominale del contratto, non l'istante in cui Binance accredita l'importo, e i due
        // possono differire parecchio. Moneta, quantità esatta (BigDecimal) e causale (campo [7]) hanno
        // già selezionato candidati altamente specifici; la data serve solo a scegliere fra loro quando
        // sono più di uno, non a scartare l'unico trovato.
        CandidatoRisultato r = new CandidatoRisultato();
        if (candidatiEntroTolleranza > 1) {
            r.ambiguo = true;
        } else {
            r.id = migliore;
            r.giaAbbinato = migliorGiaAbbinato;
        }
        return r;
    }

    /**
     * Estrae l'offset UTC dal nome del file di dettaglio (es. {@code "...202609182038UTC+2.csv"}),
     * come {@link Importazioni#extractUtcOffsetBinance} fa per il CSV principale ma senza richiedere le
     * parentesi, assenti nel nome che Binance dà a questo export.
     * @param nomeFile nome del file da cui estrarre l'offset
     * @return l'offset UTC in ore, o {@code 0} se non trovato nel nome del file
     */
    static int estraiOffset(String nomeFile) {
        Matcher m = OFFSET_PATTERN.matcher(nomeFile);
        if (!m.find()) return 0;
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * Converte una data del file di dettaglio (fuso dichiarato nel nome del file, non necessariamente
     * UTC) in un'epoca approssimata UTC, sottraendo l'offset. Serve solo a <b>ordinare per vicinanza</b>
     * i candidati con la stessa moneta/quantità, non a un confronto esatto: un errore sistematico
     * sull'offset non cambia quale candidato è il più vicino, perché si applica ugualmente a tutti.
     * @param data data nel formato {@code yyyy-MM-dd HH:mm:ss} o {@code yyyy-MM-dd HH:mm} (la Settlement
     *        Date del file di dettaglio a volte non riporta i secondi)
     * @param offsetOre offset UTC dichiarato nel nome del file, in ore
     * @return l'epoca approssimata in millisecondi, o {@code 0} se {@code data} non è parsabile
     */
    static long convertiAEpocaUtc(String data, int offsetOre) {
        try {
            String normalizzata = data.trim();
            DateTimeFormatter fmt = normalizzata.length() > 16
                    ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime ldt = LocalDateTime.parse(normalizzata, fmt);
            return ldt.toEpochSecond(ZoneOffset.ofHours(offsetOre)) * 1000L;
        } catch (Exception ex) {
            return 0L;
        }
    }

    /**
     * Interpreta un campo importo del file di dettaglio (es. {@code "0.00001474 BTC"}).
     * @param campo il campo da interpretare
     * @return {@code {quantitaAssoluta, moneta}}, o {@code null} se non interpretabile
     */
    static String[] parseImportoMoneta(String campo) {
        if (campo == null) return null;
        String[] parti = campo.trim().split("\\s+");
        if (parti.length != 2) return null;
        try {
            BigDecimal q = new BigDecimal(parti[0]).abs();
            return new String[]{q.toPlainString(), parti[1]};
        } catch (Exception ex) {
            return null;
        }
    }

    /** Legge il CSV di dettaglio, saltando l'intestazione e le righe vuote/troppo corte. */
    private static List<String[]> leggiRighe(File file) throws IOException {
        List<String[]> risultato = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String riga;
            boolean primaRiga = true;
            while ((riga = br.readLine()) != null) {
                riga = riga.replace("﻿", "").trim();
                if (riga.isEmpty()) continue;
                if (primaRiga) {
                    primaRiga = false;
                    if (riga.toLowerCase().startsWith("product,")) continue; // intestazione
                }
                risultato.add(riga.split(",", -1));
            }
        }
        return risultato;
    }
}
