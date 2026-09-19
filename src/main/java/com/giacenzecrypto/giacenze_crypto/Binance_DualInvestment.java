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

    private static final Pattern OFFSET_PATTERN = Pattern.compile("UTC([+-]\\d+)", Pattern.CASE_INSENSITIVE);

    /** Esito dell'abbinamento, per il resoconto mostrato all'utente. */
    public static class Esito {
        public int contrattiTotali = 0;
        public int abbinati = 0;
        public int ambigui = 0;
        public int nonTrovati = 0;
        public int nonAncoraLiquidati = 0;
        /** Una riga per ogni contratto non abbinato (ambiguo o non trovato), per la diagnosi. */
        public List<String> dettagli = new ArrayList<>();
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

            CandidatoRisultato purchase = trovaCandidato(sub[1], sub[0], tsSub, true, giaUsati);
            CandidatoRisultato settlement = trovaCandidato(settle[1], settle[0], tsSettle, false, giaUsati);

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
                        + "(CSV principale non ancora importato, o già abbinato in un giro precedente)");
                continue;
            }

            if (sub[1].equalsIgnoreCase(settle[1])) {
                CreaMovimentiDualInvestmentStessaMoneta(purchase.id, settlement.id);
            } else {
                GUI_ClassificazioneMovimento.CreaMovimentiScambioCryptoDifferito(purchase.id, settlement.id);
            }
            giaUsati.add(purchase.id);
            giaUsati.add(settlement.id);
            esito.abbinati++;
        }
        return esito;
    }

    static final String WALLET_DUAL_SAVINGS = "Dual Savings";

    /**
     * Abbina Purchase e Settlement di un contratto liquidato <b>nella stessa moneta sottoscritta</b>
     * (il caso più comune: capitale + interesse tornano nella valuta di partenza). Uno scambio
     * differito qui permuterebbe l'intero importo liquidato invece di limitare l'effetto fiscale alla
     * sola differenza, quindi si tratta invece come un giroconto verso/da un sotto-wallet dedicato
     * ({@link #WALLET_DUAL_SAVINGS}), sullo stesso schema TI (nessun effetto sul LIFO, stesso gruppo
     * wallet di {@link Principale_GiacenzeaData}) già usato per Vault/Piattaforma in
     * {@code GUI_ClassificazioneMovimento.CreaMovimentoTrasferimentoA/Da}:
     * <ul>
     *   <li><b>Purchase</b> (prelievo dal wallet principale): resta invariato nella quantità, solo
     *       campo5/campo18 diventano "PTW - Trasferimento a Dual Investment" e si crea la gamba
     *       speculare in entrata sul sotto-wallet (stessa moneta, stessa quantità, segno invertito);</li>
     *   <li><b>Settlement</b> (deposito sul wallet principale): la sua quantità viene <b>ridotta al solo
     *       capitale sottoscritto</b> (il resto sarebbe una doppia conta, perché la gamba speculare sul
     *       sotto-wallet muove solo il capitale) e diventa "DTW - Trasferimento da Dual Investment"; si
     *       crea la gamba speculare in uscita sul sotto-wallet per lo stesso capitale, e — solo se la
     *       liquidazione ha reso più del sottoscritto — un movimento REWARD indipendente sul wallet
     *       principale per la differenza esatta, prezzata in proporzione al valore già calcolato
     *       all'import per l'intero Settlement (stesso prezzo unitario, niente nuova ricerca prezzo).</li>
     * </ul>
     * A differenza del meccanismo generico Vault (che deduce la reward dal saldo aggregato del
     * sotto-wallet, perché non conosce quale prelievo appartiene a quale deposito), qui la reward si
     * calcola diretta dalla coppia di quantità esatte del contratto — note con certezza dal CSV di
     * dettaglio — senza bisogno di quell'euristica.
     * @param IDPurchase ID del movimento di sottoscrizione (prelievo dal wallet principale)
     * @param IDSettlement ID del movimento di liquidazione (deposito sul wallet principale)
     */
    static void CreaMovimentiDualInvestmentStessaMoneta(String IDPurchase, String IDSettlement) {
        String[] MovPurchase = MappaCryptoWallet.get(IDPurchase);
        String[] MovSettlement = MappaCryptoWallet.get(IDSettlement);

        // ── Gamba 1: Purchase -> gamba speculare in entrata sul sotto-wallet "Dual Savings" ──
        String[] IDSpezzatoP = MovPurchase[0].split("_");
        Moneta MonetaINSpec = new Moneta();
        MonetaINSpec.Moneta = MovPurchase[8];
        MonetaINSpec.Tipo = MovPurchase[9];
        MonetaINSpec.Qta = new BigDecimal(MovPurchase[10]).multiply(new BigDecimal(-1)).stripTrailingZeros().toPlainString();
        if (MovPurchase.length > 29) {
            MonetaINSpec.NomeEsteso = MovPurchase[25];
            MonetaINSpec.MonetaAddress = MovPurchase[26];
        }
        String IDMirrorPurchase = IDSpezzatoP[0] + "_" + IDSpezzatoP[1] + "_" + IDSpezzatoP[2] + "A_" + IDSpezzatoP[3] + "_DC";
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
        MappaCryptoWallet.put(IDMirrorPurchase, MTPurchase);

        MovPurchase[5] = "TRASFERIMENTO A DUAL INVESTMENT";
        MovPurchase[18] = "PTW - Trasferimento a Dual Investment";
        MovPurchase[20] = IDMirrorPurchase;

        // ── Gamba 2: Settlement -> capitale rientrato + eventuale reward separata ──
        BigDecimal QtaSottoscritta = new BigDecimal(MovPurchase[10]).abs();
        BigDecimal QtaLiquidata = new BigDecimal(MovSettlement[13]).abs();
        BigDecimal ValoreLiquidato = new BigDecimal(MovSettlement[15]);
        BigDecimal Reward = QtaLiquidata.subtract(QtaSottoscritta);

        String[] IDSpezzatoS = MovSettlement[0].split("_");
        Moneta MonetaOUTSpec = new Moneta();
        MonetaOUTSpec.Moneta = MovSettlement[11];
        MonetaOUTSpec.Tipo = MovSettlement[12];
        MonetaOUTSpec.Qta = QtaSottoscritta.multiply(new BigDecimal(-1)).stripTrailingZeros().toPlainString();
        if (MovSettlement.length > 29) {
            MonetaOUTSpec.NomeEsteso = MovSettlement[27];
            MonetaOUTSpec.MonetaAddress = MovSettlement[28];
        }
        // Valore della sola quota capitale, in proporzione al valore già calcolato per l'intero
        // Settlement all'import - stesso prezzo unitario, nessuna nuova ricerca prezzo.
        String ValoreCapitale = QtaSottoscritta.divide(QtaLiquidata, VarStatiche.DecimaliCalcoli, RoundingMode.HALF_UP)
                .multiply(ValoreLiquidato).setScale(2, RoundingMode.HALF_UP).toPlainString();

        String IDMirrorSettlement = IDSpezzatoS[0] + "_" + IDSpezzatoS[1] + "_0" + IDSpezzatoS[2] + "_" + IDSpezzatoS[3] + "_PC";
        String[] MTSettlement = MovimentiCrypto.creaMovimento(
                MonetaOUTSpec, null,
                MovSettlement[3], WALLET_DUAL_SAVINGS,
                FunzioniDate.ConvertiDataIDinLong(IDSpezzatoS[0]),
                ValoreCapitale, null,
                1, 1,
                null, null, "AU",
                MovSettlement.length > 29 ? MovSettlement[24] : null,
                null, null
        );
        MTSettlement[0] = IDMirrorSettlement;
        MTSettlement[1] = MovSettlement[1];
        MTSettlement[2] = "1 di 1";
        MTSettlement[5] = "TRASFERIMENTO INTERNO";
        MTSettlement[15] = ValoreCapitale;
        MTSettlement[18] = "PTW - Trasferimento Interno";
        MTSettlement[32] = "";
        MTSettlement[40] = "";
        if (MovSettlement.length > 29) {
            MTSettlement[23] = MovSettlement[23];
            MTSettlement[29] = MovSettlement[29];
        }
        MappaCryptoWallet.put(IDMirrorSettlement, MTSettlement);

        String IDReward = "";
        if (Reward.compareTo(BigDecimal.ZERO) > 0) {
            IDReward = IDSpezzatoS[0] + "_" + IDSpezzatoS[1] + "_00" + IDSpezzatoS[2] + "_" + IDSpezzatoS[3] + "_DC";
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
            String[] MTReward = MovimentiCrypto.creaMovimento(
                    null, MonetaINReward,
                    MovSettlement[3], MovSettlement[4],
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
            MTReward[18] = "DAI - Reward Dual Investment";
            MTReward[20] = IDMirrorSettlement;
            MTReward[32] = "";
            MTReward[40] = "";
            if (MovSettlement.length > 29) {
                MTReward[29] = MovSettlement[29];
            }
            MappaCryptoWallet.put(IDReward, MTReward);
        } else if (Reward.compareTo(BigDecimal.ZERO) < 0) {
            LoggerGC.ScriviErrore("Dual Investment: liquidato (" + QtaLiquidata + ") meno del sottoscritto ("
                    + QtaSottoscritta + "), contratto in perdita - nessuna reward creata per " + IDSettlement);
        }

        MovSettlement[13] = QtaSottoscritta.stripTrailingZeros().toPlainString();
        MovSettlement[15] = ValoreCapitale;
        MovSettlement[5] = "TRASFERIMENTO DA DUAL INVESTMENT";
        MovSettlement[18] = "DTW - Trasferimento da Dual Investment";
        MovSettlement[20] = IDReward.isBlank() ? IDMirrorSettlement : IDMirrorSettlement + "," + IDReward;
    }

    private static class CandidatoRisultato {
        String id;
        boolean ambiguo;
    }

    /**
     * Cerca in {@link Principale#MappaCryptoWallet} il movimento Dual Savings non ancora classificato
     * che corrisponde a {@code moneta}/{@code quantita}, più vicino nel tempo a {@code tsCercato}.
     * @param moneta moneta del movimento cercato
     * @param quantita quantità assoluta (senza segno) del movimento cercato
     * @param tsCercato istante approssimato (epoca UTC) del contratto, per scegliere fra più candidati
     * @param purchase {@code true} per un Purchase (categoria PC), {@code false} per un Settlement (categoria DC)
     * @param giaUsati ID già assegnati a un altro contratto in questa stessa esecuzione
     */
    private static CandidatoRisultato trovaCandidato(String moneta, String quantitaStr, long tsCercato,
            boolean purchase, Set<String> giaUsati) {
        BigDecimal quantita = new BigDecimal(quantitaStr);
        String causaleAttesa = purchase ? CAUSALE_PURCHASE : CAUSALE_SETTLEMENT;
        String migliore = null;
        long migliorScarto = Long.MAX_VALUE;
        int candidatiEntroTolleranza = 0;

        for (String[] mov : MappaCryptoWallet.values()) {
            if (mov == null || mov.length <= 18 || giaUsati.contains(mov[0])) continue;
            if (mov.length <= 7 || mov[7] == null || !mov[7].equalsIgnoreCase(causaleAttesa)) continue;
            boolean candidato = purchase
                    ? Importazioni.EPrelievoDaClassificare(mov)
                    : Importazioni.EDepositoDaClassificare(mov);
            if (!candidato) continue;

            String monetaMov = purchase ? mov[8] : mov[11];
            String quantitaMov = purchase ? mov[10] : mov[13];
            if (monetaMov == null || !monetaMov.equalsIgnoreCase(moneta)) continue;
            BigDecimal qtaMov;
            try {
                qtaMov = new BigDecimal(quantitaMov).abs();
            } catch (Exception ex) {
                continue;
            }
            if (qtaMov.compareTo(quantita) != 0) continue;

            // Il campo data visualizzato ([1]) non ha i secondi ("yyyy-MM-dd HH:mm"): ConvertiDatainLongSecondo
            // (che li richiede) falliva silenziosamente tornando 0 per ogni riga - da qui, prima di questa
            // correzione, 0 abbinamenti su 13 contratti sul dataset reale (candidati sempre "equidistanti" o
            // fuori da qualunque tolleranza). Il prefisso dell'ID (yyyyMMddHHmmss) porta invece i secondi
            // veri: necessario per distinguere due Purchase nello stesso minuto (frequente: due acquisti
            // Buy Low a pochi secondi di distanza), che con la sola granularità al minuto restano ambigui.
            long tsMov = mov[0].length() >= 14
                    ? FunzioniDate.ConvertiDataIDinLong(mov[0].substring(0, 14))
                    : FunzioniDate.ConvertiDatainLongMinuto(mov[1]);
            long scarto = Math.abs(tsMov - tsCercato);
            if (scarto < migliorScarto) {
                migliorScarto = scarto;
                migliore = mov[0];
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
