package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Pre-scarico dei prezzi di un'importazione DeFi: dai dati appena scaricati dall'explorer (o da Moralis, o da
 * Helius per Solana) ricava <b>prima</b> di costruire le transazioni tutti i prezzi che
 * {@link TransazioneDefi#InserisciMonete} e {@link TransazioneDefi#RitornaRigheTabella} chiederanno, e li
 * scarica a gruppi:
 * <ul>
 *   <li>i token con address da DefiLlama ({@link PrezziDefiLlama#PreScaricaCoppie});</li>
 *   <li>la moneta della rete (valori e commissioni) e i token con un alias per simbolo dagli exchange
 *       ({@link Prezzi#PreScaricaPrezziSimboli}).</li>
 * </ul>
 * Come per ogni pre-scarico, sbagliare qui non cambia nessun prezzo: una coppia in piu' e' una richiesta
 * sprecata, una in meno e' un movimento che si scarica il prezzo da solo, come prima.
 *
 * <p>Gli istanti sono quelli della valorizzazione: {@code TransazioneDefi} prezza a
 * {@code ConvertiDatainLongSecondo(DataOra)}, con {@code DataOra} ricavata in testo dal timestamp
 * ({@link #IstanteImport}).
 */
public class PrezziImportDeFi {

    /** Le due liste di un pre-scarico. */
    record Raccolta(List<PrezziDefiLlama.Coppia> address, List<Prezzi.SimboloIstante> simboli) {
        Raccolta() {
            this(new ArrayList<>(), new ArrayList<>());
        }
    }

    /**
     * Un token trasferito: se ha un alias si prezza per simbolo (il simbolo dell'alias), altrimenti per
     * address su DefiLlama. E' lo stesso bivio di {@link Prezzi#DammiPrezzoInfoTransazione}.
     */
    private static void Token(Raccolta r, String address, String rete, long istante) {
        if (address == null || address.isBlank()) return;
        String alias = AliasPrezziToken.Alias(address, rete, istante);
        if (alias != null) r.simboli().add(new Prezzi.SimboloIstante(alias, istante));
        else r.address().add(new PrezziDefiLlama.Coppia(address, rete, istante));
    }

    private static boolean NonZero(String valore) {
        try {
            return valore != null && !valore.isBlank() && new BigDecimal(valore).signum() != 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * Importazione EVM da explorer. La moneta della rete serve quando la transazione muove valore o quando
     * la manda il wallet (commissione), e per ogni transazione interna con valore.
     */
    static Raccolta RaccogliEVM(JSONArray txlist, JSONArray interne, JSONArray tokentx,
            String wallet, String rete, String monetaRete) {
        Raccolta r = new Raccolta();
        if (txlist != null) {
            for (int i = 0; i < txlist.length(); i++) {
                JSONObject t = txlist.optJSONObject(i);
                if (t == null || !Funzioni.isNumeric(t.optString("timeStamp", ""), false)) continue;
                boolean commissione = t.optString("from", "").equalsIgnoreCase(wallet);
                if (commissione || NonZero(t.optString("value", "0"))) {
                    r.simboli().add(new Prezzi.SimboloIstante(monetaRete, IstanteImport(t.optLong("timeStamp") * 1000)));
                }
            }
        }
        if (interne != null) {
            for (int i = 0; i < interne.length(); i++) {
                JSONObject t = interne.optJSONObject(i);
                if (t == null || !Funzioni.isNumeric(t.optString("timeStamp", ""), false)) continue;
                if (NonZero(t.optString("value", "0"))) {
                    r.simboli().add(new Prezzi.SimboloIstante(monetaRete, IstanteImport(t.optLong("timeStamp") * 1000)));
                }
            }
        }
        if (tokentx != null) {
            for (int i = 0; i < tokentx.length(); i++) {
                JSONObject t = tokentx.optJSONObject(i);
                if (t == null || !Funzioni.isNumeric(t.optString("timeStamp", ""), false)) continue;
                Token(r, t.optString("contractAddress", ""), rete, IstanteImport(t.optLong("timeStamp") * 1000));
            }
        }
        return r;
    }

    /**
     * Una pagina della cronologia Moralis: trasferimenti ERC20, e la moneta della rete se ci sono
     * trasferimenti nativi o se la transazione la manda il wallet (commissione).
     */
    static Raccolta RaccogliMoralis(JSONArray risultati, String wallet, String rete, String monetaRete) {
        Raccolta r = new Raccolta();
        if (risultati == null) return r;
        for (int i = 0; i < risultati.length(); i++) {
            JSONObject tx = risultati.optJSONObject(i);
            if (tx == null) continue;
            String iso = tx.optString("block_timestamp", null);
            if (iso == null) continue;
            long istante = IstanteImport(FunzioniDate.ConvertiISO8601toMillis(iso));
            JSONArray erc20 = tx.optJSONArray("erc20_transfers");
            if (erc20 != null) {
                for (int j = 0; j < erc20.length(); j++) {
                    JSONObject tok = erc20.optJSONObject(j);
                    if (tok != null) Token(r, tok.optString("address", ""), rete, istante);
                }
            }
            JSONArray nativi = tx.optJSONArray("native_transfers");
            boolean commissione = tx.optString("from_address", "").equalsIgnoreCase(wallet)
                    && NonZero(tx.optString("transaction_fee", "0"));
            if (commissione || (nativi != null && nativi.length() > 0)) {
                r.simboli().add(new Prezzi.SimboloIstante(monetaRete, istante));
            }
        }
        return r;
    }

    /**
     * Importazione Solana: i {@code mint} dei {@code tokenBalanceChanges} del wallet (o senza proprietario
     * dichiarato, che {@code Trans_Solana} risolve dopo: chiederne uno in piu' costa una voce nella chiamata,
     * non una chiamata), e SOL se il wallet paga la commissione o il suo saldo nativo cambia.
     */
    static Raccolta RaccogliSolana(JSONArray transazioni, String wallet) {
        Raccolta r = new Raccolta();
        if (transazioni == null) return r;
        for (int i = 0; i < transazioni.length(); i++) {
            JSONObject tx = transazioni.optJSONObject(i);
            if (tx == null) continue;
            long istante = IstanteImport(tx.optLong("timestamp", 0) * 1000);
            boolean sol = tx.optString("feePayer", "").equalsIgnoreCase(wallet);
            JSONArray conti = tx.optJSONArray("accountData");
            if (conti != null) {
                for (int j = 0; j < conti.length(); j++) {
                    JSONObject conto = conti.optJSONObject(j);
                    if (conto == null) continue;
                    if (conto.optString("account", "").equalsIgnoreCase(wallet)
                            && NonZero(conto.optString("nativeBalanceChange", "0"))) sol = true;
                    JSONArray variazioni = conto.optJSONArray("tokenBalanceChanges");
                    if (variazioni == null) continue;
                    for (int k = 0; k < variazioni.length(); k++) {
                        JSONObject v = variazioni.optJSONObject(k);
                        if (v == null) continue;
                        String proprietario = v.optString("userAccount", "");
                        if (!proprietario.isBlank() && !proprietario.equalsIgnoreCase(wallet)) continue;
                        Token(r, v.optString("mint", ""), "SOL", istante);
                    }
                }
            }
            if (sol) r.simboli().add(new Prezzi.SimboloIstante("SOL", istante));
        }
        return r;
    }

    /** Scarica a gruppi tutto cio' che la raccolta ha trovato. */
    static void PreScarica(Raccolta r, String origine, Download progress) {
        PrezziDefiLlama.PreScaricaCoppie(r.address(), origine, progress);
        long inizio = System.currentTimeMillis();
        int richieste = Prezzi.PreScaricaPrezziSimboli(r.simboli(), progress, origine);
        if (richieste > 0) {
            System.out.println("Pre-scarico prezzi exchange (" + origine + "): " + richieste + " coppie (moneta, ora), "
                    + (System.currentTimeMillis() - inizio) + " ms");
        }
    }

    /**
     * L'istante con cui le importazioni DeFi prezzano: passano dalla data in testo al secondo
     * ({@code TransazioneDefi.DataOra}) e la riconvertono, quindi si fa lo stesso giro per ottenere lo
     * stesso numero (che e' anche la chiave di {@link PrezziDefiLlama#GiaChiestoAllIstante}).
     */
    static long IstanteImport(long millis) {
        return FunzioniDate.ConvertiDatainLongSecondo(FunzioniDate.ConvertiDatadaLongAlSecondo(millis));
    }
}
