package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Abbinamento automatico, a fine importazione, dei <b>giroconti in valuta FIAT</b> fra due wallet
 * dello stesso utente (es. gli euro spostati da Coinbase a Coinbase Pro / GDAX).
 *
 * <p>Senza abbinamento quei giroconti restano un {@code PF} su un wallet e un {@code DF}
 * sull'altro, indistinguibili da un vero bonifico da/verso la banca : chi filtra i depositi FIAT
 * per sapere quanto ha versato sugli exchange conta due volte gli stessi soldi, e
 * {@link Calcoli_RW_Fiat} li legge come apporto/prelievo. Abbinati, diventano un normale
 * trasferimento tra wallet ({@code PTW}/{@code DTW} in {@code [18]}, riferimenti incrociati in
 * {@code [20]}) tramite {@link GUI_ClassificazioneMovimento#CreaMovimentiTrasferimentosuWalletProprio},
 * la stessa funzione della classificazione manuale. La categoria resta {@code PF}/{@code DF}, cosi'
 * il saldo si sposta davvero da un wallet all'altro.</p>
 *
 * <p>La regola sta nella configurazione di importazione (chiave {@code girocontoFiat}) e viene
 * dichiarata <b>da entrambi i lati</b> : l'abbinamento gira sull'intero archivio a ogni
 * importazione di uno dei due file, quindi funziona in qualunque ordine si importino e sistema
 * anche i movimenti gia' in archivio (basta reimportare il file). Una coppia e' riconosciuta solo
 * se <b>entrambe</b> le causali CSV originali ({@code [7]}) sono fra quelle dichiarate : sul conto
 * retail di Coinbase e' la causale stessa a dire che si tratta di un giroconto
 * ({@code Exchange Deposit}, {@code Pro Withdrawal}, ...), e questo tiene fuori i bonifici dalla
 * banca ({@code Deposit}). Importo identico, segno opposto, stessa valuta, scarto di tempo entro
 * la tolleranza. Una riga senza controparte resta com'e'.</p>
 */
public class GirocontiFiat {

    /**
     * Coppie abbinate dall'importazione in corso, mostrate da {@link Importazioni_Resoconto}.
     * Azzerato da {@link Importazioni#AzzeraContatori()}, incrementato da {@link ImportazioneGenerica}.
     * Serve perché un reimport che abbina giroconti gia' in archivio mostra "0 importate" e altrimenti
     * sembrerebbe non aver fatto nulla.
     */
    public static int AbbinatiImportazione = 0;

    /**
     * Paragrafo HTML per il riquadro del resoconto, o stringa vuota se l'importazione non ha abbinato
     * nessun giroconto.
     */
    public static String TestoResoconto() {
        if (AbbinatiImportazione <= 0) {
            return "";
        }
        return "<b><center>GIROCONTI FIAT ABBINATI : " + AbbinatiImportazione + "</b><br><br>"
                + "<center>Depositi e prelievi in valuta fra due wallet di tua proprietà (ad esempio gli euro"
                + " spostati da Coinbase a Coinbase Pro) sono stati riconosciuti e classificati come"
                + " trasferimento tra wallet: non sono versamenti dalla banca né prelievi verso la banca.<br>"
                + "<center>Il conteggio comprende anche i movimenti che erano già in archivio.";
    }

    /** Regola letta dalla chiave {@code girocontoFiat} di una configurazione di importazione. */
    public static class Regola {

        /** Nome exchange ({@code [3]}) dell'altro wallet del giroconto. */
        public String controparte = "";
        /** Causali CSV ({@code [7]}) dei giroconti su questo wallet. */
        public Set<String> causali = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        /** Causali CSV ({@code [7]}) dei giroconti sul wallet controparte. */
        public Set<String> causaliControparte = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        /** Scarto massimo, in secondi, fra le due gambe. */
        public long secondiTolleranza = 60;

        public static Regola daJson(JSONObject o) {
            Regola r = new Regola();
            r.controparte = o.optString("controparte", "").trim();
            aggiungi(r.causali, o.optJSONArray("causali"));
            aggiungi(r.causaliControparte, o.optJSONArray("causaliControparte"));
            r.secondiTolleranza = o.optLong("secondiTolleranza", 60);
            return r;
        }

        private static void aggiungi(Set<String> insieme, JSONArray arr) {
            if (arr == null) {
                return;
            }
            for (int i = 0; i < arr.length(); i++) {
                insieme.add(arr.getString(i).trim());
            }
        }
    }

    /**
     * Abbina, sull'intero {@code MappaCryptoWallet}, i giroconti FIAT fra il wallet {@code exchange}
     * e {@code regola.controparte}. Ogni gamba viene usata al massimo una volta, e fra piu' candidati
     * vince il piu' vicino nel tempo.
     *
     * @return il numero di coppie abbinate
     */
    public static int Abbina(String exchange, Regola regola) {
        if (regola == null || regola.controparte.isBlank() || regola.causali.isEmpty()
                || regola.causaliControparte.isEmpty()) {
            return 0;
        }
        List<String[]> nostri = new ArrayList<>();
        List<String[]> loro = new ArrayList<>();
        for (String[] v : MappaCryptoWallet.values()) {
            if (isCandidato(v, exchange, regola.causali)) {
                nostri.add(v);
            } else if (isCandidato(v, regola.controparte, regola.causaliControparte)) {
                loro.add(v);
            }
        }
        if (nostri.isEmpty() || loro.isEmpty()) {
            return 0;
        }

        long tolleranzaMs = regola.secondiTolleranza * 1000;
        Set<String> usati = new HashSet<>();
        List<String[]> coppie = new ArrayList<>();
        for (String[] a : nostri) {
            String[] migliore = null;
            long scartoMigliore = Long.MAX_VALUE;
            for (String[] b : loro) {
                if (usati.contains(b[0]) || !sonoControparti(a, b)) {
                    continue;
                }
                long scarto = Math.abs(istante(a) - istante(b));
                if (scarto <= tolleranzaMs && scarto < scartoMigliore) {
                    migliore = b;
                    scartoMigliore = scarto;
                }
            }
            if (migliore != null) {
                usati.add(migliore[0]);
                coppie.add(new String[]{a[0], migliore[0]});
            }
        }

        for (String[] c : coppie) {
            boolean primoPrelievo = "PF".equals(categoria(MappaCryptoWallet.get(c[0])));
            String idPrelievo = primoPrelievo ? c[0] : c[1];
            String idDeposito = primoPrelievo ? c[1] : c[0];
            GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(idPrelievo, idDeposito);
        }
        return coppie.size();
    }

    /** {@code PF}/{@code DF} non ancora classificato di {@code exchange}, con causale CSV fra quelle date. */
    static boolean isCandidato(String[] v, String exchange, Set<String> causali) {
        if (v == null || v.length <= 20 || v[3] == null || !v[3].trim().equalsIgnoreCase(exchange)) {
            return false;
        }
        String cat = categoria(v);
        if (!"PF".equals(cat) && !"DF".equals(cat)) {
            return false;
        }
        return v[18] != null && v[18].isBlank()
                && v[7] != null && causali.contains(v[7].trim())
                && quantita(v) != null;
    }

    /** Un prelievo e un deposito della stessa valuta per lo stesso importo. */
    static boolean sonoControparti(String[] a, String[] b) {
        String catA = categoria(a);
        String catB = categoria(b);
        if (catA.equals(catB)) {
            return false;
        }
        if (!valuta(a).equalsIgnoreCase(valuta(b))) {
            return false;
        }
        return quantita(a).abs().compareTo(quantita(b).abs()) == 0;
    }

    private static String categoria(String[] v) {
        String[] id = v[0].split("_");
        return id.length > 4 ? id[4].toUpperCase() : "";
    }

    /** Valuta della sola gamba presente : uscita per un {@code PF}, entrata per un {@code DF}. */
    private static String valuta(String[] v) {
        return "PF".equals(categoria(v)) ? v[8].trim() : v[11].trim();
    }

    private static BigDecimal quantita(String[] v) {
        String q = "PF".equals(categoria(v)) ? v[10] : v[13];
        try {
            BigDecimal b = new BigDecimal(q.trim());
            return b.signum() == 0 ? null : b;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** Istante del movimento dal prefisso {@code yyyyMMddHHmmss} dell'ID. */
    private static long istante(String[] v) {
        return FunzioniDate.ConvertiDataIDinLong(v[0].split("_")[0]);
    }

    private GirocontiFiat() {
    }
}
