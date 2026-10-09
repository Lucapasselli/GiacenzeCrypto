package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Importazione di un wallet su X Layer (chain id 196) senza chiavi API, dal nodo pubblico.
 *
 * <p>Nasce per il wallet della carta OKX ({@link OKX_WalletCarta}), un wallet ERC-4337 che nessun explorer
 * gratuito copre (Etherscan v2, Routescan, Blockscout no; l'API dati di OKX vuole una chiave del portale
 * sviluppatori). Il nodo pubblico dà l'archivio completo ma {@code eth_getLogs} accetta al massimo 100 blocchi e
 * X Layer fa circa un blocco al secondo: leggere lo storico a tratti fissi costerebbe ore. Si legge invece lo
 * <b>stato</b> del wallet (nonce ERC-4337, saldi delle monete note, saldo "scalato" degli aToken, OKB) con una sola
 * {@code eth_call} a Multicall3: dove lo stato è uguale ai due estremi di un tratto non è successo nulla, altrimenti
 * il tratto si divide a metà fino a 100 blocchi, e lì si chiedono i log. Circa 20 richieste per movimento,
 * qualunque sia il tempo trascorso.
 *
 * <p>Limiti, voluti: si vedono solo le monete dell'elenco {@link #TOKEN} (una moneta fuori elenco compare solo se
 * cade in un tratto già aperto da un altro cambiamento) e i movimenti di OKB nativo non si importano (si segnala il
 * cambio di saldo). Per il wallet della carta basta; un wallet X Layer qualsiasi resterebbe incompleto.
 *
 * <p>Classificazione (decisioni dell'utente del 2026-10-09, {@code nocommit/Documentazione/Analisi_Carta_OKX_XLayer.md}
 * § 6.3):
 * <ul>
 *   <li>gli interessi Aave maturati fino a un movimento dell'aToken diventano una REWARD, ordinata prima del
 *       movimento: Aave li registra a ogni movimento come {@code Mint}/{@code Burn} con {@code balanceIncrease}, e
 *       il loro {@code Transfer} da {@code 0x0} non è denaro in entrata;</li>
 *   <li>un pagamento con la carta (evento {@code Claimed} con il wallet come mittente) è un CASHOUT;</li>
 *   <li>il resto segue la forma della transazione: una moneta in uscita e una in entrata è uno scambio, una sola
 *       è un deposito o un prelievo. Ricariche e cashback restano depositi: li riconosce dopo
 *       {@link OKX_CartaAbbina}, che ha bisogno anche dei bill dell'exchange.</li>
 * </ul>
 */
final class Trans_XLayer {

    private Trans_XLayer() {}

    static final String RETE = "XLAYER";
    static final String MULTICALL3 = "0xca11bde05977b3631167028862be2a173976ca11";
    static final String ENTRYPOINT_V07 = "0x0000000071727de22e5e9d8baf0edac6f37da032";
    static final String INDIRIZZO_NULLO = "0x0000000000000000000000000000000000000000";

    static final String TOPIC_TRANSFER = OKX_WalletCarta.TOPIC_TRANSFER;
    /** {@code Mint(address,address,uint256,uint256,uint256)} degli aToken Aave v3: caller, onBehalfOf, value, balanceIncrease, index */
    static final String TOPIC_MINT_ATOKEN = "0x458f5fa412d0f69b08dd84872b0215675cc67bc1d5b6fd93300a1c3878b86196";
    /** {@code Burn(address,address,uint256,uint256,uint256)} degli aToken Aave v3: from, target, value, balanceIncrease, index */
    static final String TOPIC_BURN_ATOKEN = "0x4cf25bc1d991c17529c25213d3cc0cda295eeaad5f13f361969b12ea48015f90";
    /** {@code Claimed(address,address,address,uint256)} del contratto di regolamento della carta: from, to, token, amount */
    static final String TOPIC_CLAIMED = "0x913c992353dc81b7a8ba31496c484e9b6306bd2f6c509a649a38fdf5e1c953b2";

    static final String SEL_GET_NONCE = "35567e1a";
    static final String SEL_BALANCE_OF = "70a08231";
    static final String SEL_SCALED_BALANCE_OF = "1da24f3e";
    static final String SEL_GET_ETH_BALANCE = "4d2301cc";
    static final String SEL_AGGREGATE3 = "82ad56cb";
    static final String SEL_SYMBOL = "95d89b41";
    static final String SEL_DECIMALS = "313ce567";

    static final int BLOCCHI_PER_RICHIESTA = 100;

    static final String CAUSALE_INTERESSI = "Interessi Aave";
    static final String CAUSALE_CARTA = "Pagamento con carta OKX";

    /** Una moneta su X Layer. Gli aToken si leggono con {@code scaledBalanceOf}, che cambia solo con depositi e prelievi. */
    record Token(String indirizzo, String simbolo, String nome, int decimali, boolean aToken) {}

    /**
     * Monete lette nello stato del wallet. Simboli e decimali verificati con {@code symbol()}/{@code decimals()} sul
     * nodo (07/10/2026). USD₮0 si chiama USDT0: il carattere ₮ non c'è nel font dell'applicazione.
     */
    static final Map<String, Token> TOKEN = Elenco(
            new Token("0x4ae46a509f6b1d9056937ba4500cb143933d2dc8", "USDG", "Global Dollar", 6, false),
            new Token("0x74b7f16337b8972027f6196a17a631ac6de26d22", "USDC", "USD Coin", 6, false),
            new Token("0x1e4a5963abfd975d8c9021ce480b42188849d41d", "USDT", "Tether USD", 6, false),
            new Token("0x779ded0c9e1022225f8e0630b35a9b54be713736", "USDT0", "USDT0", 6, false),
            new Token("0x228765a3c18065c923f23a0ccb6c7cefb3ea2223", "aXlrUSDG", "Aave XLayer USDG", 6, true));

    private static Map<String, Token> Elenco(Token... t) {
        Map<String, Token> m = new LinkedHashMap<>();
        for (Token x : t) m.put(x.indirizzo(), x);
        return java.util.Collections.unmodifiableMap(m);
    }

    /** Un movimento da scrivere: {@code tipo} è il {@code TipoTr} di {@code creaMovimento}, {@code null} per farlo decidere dalle gambe. */
    record Movimento(String tipo, String causale, Token uscita, BigInteger qtaUscita, Token entrata, BigInteger qtaEntrata,
            String controparte) {}

    /** Una transazione del wallet con i suoi movimenti, già classificati. */
    record Transazione(String hash, long blocco, long timestamp, List<Movimento> movimenti) {}

    record Esito(List<Transazione> transazioni, List<String> avvisi, boolean completo) {}

    //=====================================================================================================
    //=== SCARICAMENTO
    //=====================================================================================================

    /**
     * Scarica e classifica le transazioni del wallet dal blocco {@code bloccoDa} (incluso) all'ultimo.
     *
     * @param interrotto restituisce {@code true} quando l'utente ha chiesto di fermarsi; può essere {@code null}
     * @return le transazioni in ordine di blocco; {@code completo} è falso se lo scaricamento si è fermato prima
     */
    static Esito Scarica(String wallet, long bloccoDa, OKX_WalletCarta.Rpc rpc, java.util.function.BooleanSupplier interrotto,
            int idDocumento) {
        return Scarica(wallet, bloccoDa, rpc, interrotto, idDocumento, null);
    }

    /**
     * Come {@link #Scarica(String, long, OKX_WalletCarta.Rpc, java.util.function.BooleanSupplier, int)}, raccontando
     * le fasi: ogni passo va nel log (lo stdout, che la finestra di scaricamento mostra) e, se c'è, in
     * {@code avanzamento} come riga di stato. La scansione per stati può durare minuti senza trovare nulla, e senza
     * questo la finestra restava ferma sulla stessa scritta.
     *
     * @param avanzamento riceve la riga di stato corrente; può essere {@code null}
     */
    static Esito Scarica(String wallet, long bloccoDa, OKX_WalletCarta.Rpc rpc, java.util.function.BooleanSupplier interrotto,
            int idDocumento, java.util.function.Consumer<String> avanzamento) {
        String w = wallet.toLowerCase();
        List<String> avvisi = new ArrayList<>();
        List<Transazione> transazioni = new ArrayList<>();
        Progresso p = new Progresso(avanzamento);
        try {
            p.stato("X Layer: lettura dell'ultimo blocco dal nodo pubblico...");
            long ultimo = Long.decode(rpc.chiama("eth_blockNumber", new JsonArray()).getAsString());
            if (bloccoDa > ultimo) {
                p.log("X Layer " + wallet + ": nessun blocco nuovo dopo il " + (bloccoDa - 1) + ".");
                return new Esito(transazioni, avvisi, true);
            }
            long da = Math.max(0, bloccoDa - 1);
            p.log("X Layer " + wallet + ": scansione dal blocco " + bloccoDa + " al " + ultimo
                    + " (" + (ultimo - da) + " blocchi), cercando dove cambia lo stato del wallet.");
            p.inizioScansione(da, ultimo);
            List<long[]> finestre = new ArrayList<>();
            List<BigInteger> statoDa = Stato(w, da, rpc);
            List<BigInteger> statoA = Stato(w, ultimo, rpc);
            p.letture += 2;
            Dividi(w, da, ultimo, statoDa, statoA, rpc, finestre, interrotto, p);
            if (interrotto != null && interrotto.getAsBoolean()) {
                p.log("X Layer: scansione interrotta.");
                return new Esito(transazioni, avvisi, false);
            }
            p.log("X Layer: scansione finita, " + p.letture + " letture dello stato, "
                    + finestre.size() + (finestre.size() == 1 ? " tratto cambiato." : " tratti cambiati."));

            //Hash delle transazioni, in ordine di blocco: i log di una transazione arrivano dalle due ricerche
            TreeMap<Long, TreeSet<String>> perBlocco = new TreeMap<>();
            int nf = 0;
            for (long[] f : finestre) {
                nf++;
                p.stato("Lettura dei log: tratto " + nf + " di " + finestre.size() + " (blocchi " + f[0] + "-" + f[1] + ")");
                if (f[2] != 0) {
                    avvisi.add("Movimento di OKB fra i blocchi " + f[0] + " e " + f[1] + ": i movimenti della moneta nativa non si importano.");
                }
                for (JsonElement el : Log(w, f[0], f[1], rpc)) {
                    JsonObject log = el.getAsJsonObject();
                    perBlocco.computeIfAbsent(Long.decode(Testo(log, "blockNumber")), k -> new TreeSet<>())
                            .add(Testo(log, "transactionHash").toLowerCase());
                }
            }
            int totaleTx = 0;
            for (TreeSet<String> h : perBlocco.values()) totaleTx += h.size();
            p.log("X Layer: " + totaleTx + (totaleTx == 1 ? " transazione" : " transazioni") + " da leggere.");
            Map<String, Token> metadati = new HashMap<>(TOKEN);
            int nt = 0;
            for (Map.Entry<Long, TreeSet<String>> e : perBlocco.entrySet()) {
                long ts = TempoBlocco(e.getKey(), rpc);
                for (String hash : e.getValue()) {
                    if (interrotto != null && interrotto.getAsBoolean()) {
                        p.log("X Layer: lettura delle transazioni interrotta.");
                        return new Esito(transazioni, avvisi, false);
                    }
                    nt++;
                    p.stato("Lettura della transazione " + nt + " di " + totaleTx + " ("
                            + FunzioniDate.ConvertiDatadaLongAlSecondo(ts * 1000) + ")");
                    JsonArray par = new JsonArray();
                    par.add(hash);
                    JsonObject ricevuta = rpc.chiama("eth_getTransactionReceipt", par).getAsJsonObject();
                    DocumentiFonte.AggiungiAllaSessione(idDocumento, "XLayer", "eth_getTransactionReceipt " + hash, ricevuta.toString());
                    if (!"0x1".equals(Testo(ricevuta, "status"))) continue;
                    List<Movimento> movimenti = Classifica(w, ricevuta.getAsJsonArray("logs"), metadati, rpc, avvisi);
                    if (!movimenti.isEmpty()) transazioni.add(new Transazione(hash, e.getKey(), ts, movimenti));
                }
            }
            p.log("X Layer: lette " + totaleTx + " transazioni, " + transazioni.size() + " con movimenti del wallet.");
            return new Esito(transazioni, avvisi, true);
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            p.log("X Layer: errore del nodo, " + ex.getMessage());
            avvisi.add("Nodo di X Layer: " + ex.getMessage());
            return new Esito(transazioni, avvisi, false);
        }
    }

    /**
     * Divide il tratto ({@code da}, {@code a}] finché lo stato cambia e il tratto supera 100 blocchi. Ogni tratto
     * foglia cambiato finisce in {@code finestre} come {@code {primo blocco, ultimo blocco, OKB cambiati ? 1 : 0}}.
     */
    private static void Dividi(String w, long da, long a, List<BigInteger> sDa, List<BigInteger> sA, OKX_WalletCarta.Rpc rpc,
            List<long[]> finestre, java.util.function.BooleanSupplier interrotto, Progresso p) throws Exception {
        if (interrotto != null && interrotto.getAsBoolean()) return;
        if (sDa.equals(sA)) {
            p.coperto(a, finestre.size());
            return;
        }
        if (a - da <= BLOCCHI_PER_RICHIESTA) {
            int okb = sDa.size() - 1;
            finestre.add(new long[]{da + 1, a, sDa.get(okb).equals(sA.get(okb)) ? 0 : 1});
            p.log("X Layer: cambio di stato fra i blocchi " + (da + 1) + " e " + a + " (tratto " + finestre.size() + ").");
            p.coperto(a, finestre.size());
            return;
        }
        long m = (da + a) >>> 1;
        List<BigInteger> sM = Stato(w, m, rpc);
        p.letture++;
        Dividi(w, da, m, sDa, sM, rpc, finestre, interrotto, p);
        Dividi(w, m, a, sM, sA, rpc, finestre, interrotto, p);
    }

    /**
     * Racconto dello scaricamento: le righe di {@link #log} vanno nello stdout (il pannello di log della finestra di
     * scaricamento) e anche nella riga di stato; quelle di {@link #stato} solo nella riga di stato, perché durante la
     * bisezione sono centinaia. La percentuale della scansione è la parte di blocchi già esclusa o già divisa fino
     * in fondo: la bisezione scende a sinistra per prima, quindi avanza in ordine di blocco.
     */
    private static final class Progresso {
        private final java.util.function.Consumer<String> uscita;
        int letture;
        private long inizio, fine;
        private int ultimaDecina = -1;

        Progresso(java.util.function.Consumer<String> uscita) {
            this.uscita = uscita;
        }

        void log(String testo) {
            System.out.println(testo);
            stato(testo);
        }

        void stato(String testo) {
            if (uscita != null) uscita.accept(testo);
        }

        void inizioScansione(long da, long a) {
            inizio = da;
            fine = a;
        }

        /** La scansione ha chiuso tutto fino al blocco {@code blocco}. Nel log una riga ogni 10%. */
        void coperto(long blocco, int tratti) {
            int perc = fine > inizio ? (int) ((blocco - inizio) * 100 / (fine - inizio)) : 100;
            String testo = "Scansione degli stati: " + perc + "% (blocco " + blocco + " di " + fine + "), "
                    + letture + " letture, " + tratti + (tratti == 1 ? " tratto cambiato" : " tratti cambiati");
            if (perc / 10 > ultimaDecina) {
                ultimaDecina = perc / 10;
                System.out.println("X Layer: s" + testo.substring(1));
            }
            stato(testo);
        }
    }

    /** I log {@code Transfer} con il wallet come mittente o destinatario, di qualsiasi moneta, fra due blocchi. */
    private static List<JsonElement> Log(String w, long da, long a, OKX_WalletCarta.Rpc rpc) throws Exception {
        List<JsonElement> log = new ArrayList<>();
        String parola = "0x" + Parola(w);
        for (int lato = 0; lato < 2; lato++) {
            JsonObject filtro = new JsonObject();
            filtro.addProperty("fromBlock", "0x" + Long.toHexString(da));
            filtro.addProperty("toBlock", "0x" + Long.toHexString(a));
            JsonArray topics = new JsonArray();
            topics.add(TOPIC_TRANSFER);
            if (lato == 0) {
                topics.add(parola);
            } else {
                topics.add(com.google.gson.JsonNull.INSTANCE);
                topics.add(parola);
            }
            filtro.add("topics", topics);
            JsonArray par = new JsonArray();
            par.add(filtro);
            rpc.chiama("eth_getLogs", par).getAsJsonArray().forEach(log::add);
        }
        return log;
    }

    //=====================================================================================================
    //=== STATO DEL WALLET
    //=====================================================================================================

    /**
     * Lo stato del wallet a un blocco: nonce ERC-4337, un saldo per moneta dell'elenco, saldo OKB per ultimo. Prima
     * che Multicall3 esistesse la chiamata restituisce {@code 0x}: lo stato è tutto a zero, come per un wallet vuoto.
     */
    static List<BigInteger> Stato(String w, long blocco, OKX_WalletCarta.Rpc rpc) throws Exception {
        JsonObject chiamata = new JsonObject();
        chiamata.addProperty("to", MULTICALL3);
        chiamata.addProperty("data", DatiStato(w));
        JsonArray par = new JsonArray();
        par.add(chiamata);
        par.add("0x" + Long.toHexString(blocco));
        List<BigInteger> valori = DecodificaAggregate3(rpc.chiama("eth_call", par).getAsString());
        int attesi = TOKEN.size() + 2;
        while (valori.size() < attesi) valori.add(BigInteger.ZERO);
        return valori;
    }

    /** Dati di {@code aggregate3} per lo stato: nonce, saldi delle monete (scalati per gli aToken), OKB. */
    static String DatiStato(String w) {
        List<String[]> chiamate = new ArrayList<>();
        chiamate.add(new String[]{ENTRYPOINT_V07, SEL_GET_NONCE + Parola(w) + Parola("0")});
        for (Token t : TOKEN.values()) {
            chiamate.add(new String[]{t.indirizzo(), (t.aToken() ? SEL_SCALED_BALANCE_OF : SEL_BALANCE_OF) + Parola(w)});
        }
        chiamate.add(new String[]{MULTICALL3, SEL_GET_ETH_BALANCE + Parola(w)});
        return Aggregate3(chiamate);
    }

    /** Codifica ABI di {@code aggregate3((address,bool,bytes)[])} con {@code allowFailure} sempre vero. */
    static String Aggregate3(List<String[]> chiamate) {
        int n = chiamate.size();
        List<String> elementi = new ArrayList<>();
        for (String[] c : chiamate) {
            String dati = c[1];
            String riempito = dati + "0".repeat((64 - dati.length() % 64) % 64);
            elementi.add(Parola(c[0]) + Parola("1") + Parola("60") + Parola(Integer.toHexString(dati.length() / 2)) + riempito);
        }
        StringBuilder sb = new StringBuilder("0x").append(SEL_AGGREGATE3).append(Parola("20")).append(Parola(Integer.toHexString(n)));
        int offset = n * 32;
        for (String e : elementi) {
            sb.append(Parola(Integer.toHexString(offset)));
            offset += e.length() / 2;
        }
        elementi.forEach(sb::append);
        return sb.toString();
    }

    /** Il valore {@code uint256} restituito da ogni chiamata di {@code aggregate3}; zero se fallita o vuota. */
    static List<BigInteger> DecodificaAggregate3(String risultato) {
        List<BigInteger> valori = new ArrayList<>();
        if (risultato == null || risultato.length() <= 2) return valori;
        String h = risultato.substring(2);
        int base = ParolaA(h, 0).intValueExact() * 2;
        int n = ParolaA(h, base).intValueExact();
        int inizio = base + 64;
        for (int i = 0; i < n; i++) {
            int el = inizio + ParolaA(h, inizio + i * 64).intValueExact() * 2;
            boolean riuscita = ParolaA(h, el).signum() != 0;
            int dati = el + ParolaA(h, el + 64).intValueExact() * 2;
            int lunghezza = ParolaA(h, dati).intValueExact();
            valori.add(riuscita && lunghezza >= 32 ? ParolaA(h, dati + 64) : BigInteger.ZERO);
        }
        return valori;
    }

    //=====================================================================================================
    //=== CLASSIFICAZIONE
    //=====================================================================================================

    /**
     * I movimenti di una transazione, dai suoi log. Logica pura salvo la lettura dei metadati di una moneta fuori
     * elenco ({@code rpc} può essere {@code null} nei test).
     */
    static List<Movimento> Classifica(String w, JsonArray logs, Map<String, Token> metadati, OKX_WalletCarta.Rpc rpc,
            List<String> avvisi) {
        Map<String, BigInteger> netto = new LinkedHashMap<>();
        Map<String, BigInteger> interessi = new LinkedHashMap<>();
        Map<String, String> controparte = new HashMap<>();
        String tokenCarta = null;
        String raccoglitore = "";
        for (JsonElement el : logs) {
            JsonObject log = el.getAsJsonObject();
            JsonArray tp = log.getAsJsonArray("topics");
            if (tp == null || tp.size() == 0) continue;
            String evento = tp.get(0).getAsString().toLowerCase();
            String indirizzo = Testo(log, "address").toLowerCase();
            String dati = Testo(log, "data");
            if (evento.equals(TOPIC_TRANSFER) && tp.size() >= 3) {
                String da = Indirizzo(tp.get(1).getAsString()), a = Indirizzo(tp.get(2).getAsString());
                BigInteger valore = ParolaDati(dati, 0);
                if (da.equals(w) && !a.equals(w)) {
                    netto.merge(indirizzo, valore.negate(), BigInteger::add);
                    if (!a.equals(INDIRIZZO_NULLO)) controparte.putIfAbsent(indirizzo, a);
                } else if (a.equals(w) && !da.equals(w)) {
                    netto.merge(indirizzo, valore, BigInteger::add);
                    if (!da.equals(INDIRIZZO_NULLO)) controparte.putIfAbsent(indirizzo, da);
                }
            } else if (evento.equals(TOPIC_MINT_ATOKEN) && tp.size() >= 3 && Indirizzo(tp.get(2).getAsString()).equals(w)
                    && isAToken(indirizzo, metadati)) {
                interessi.merge(indirizzo, ParolaDati(dati, 1), BigInteger::add);
            } else if (evento.equals(TOPIC_BURN_ATOKEN) && tp.size() >= 2 && Indirizzo(tp.get(1).getAsString()).equals(w)
                    && isAToken(indirizzo, metadati)) {
                interessi.merge(indirizzo, ParolaDati(dati, 1), BigInteger::add);
            } else if (evento.equals(TOPIC_CLAIMED) && tp.size() >= 3 && Indirizzo(tp.get(1).getAsString()).equals(w)) {
                tokenCarta = Indirizzo(Parola(dati.length() >= 66 ? dati.substring(2, 66) : "0"));
                raccoglitore = Indirizzo(tp.get(2).getAsString());
            }
        }

        List<Movimento> movimenti = new ArrayList<>();
        //Prima gli interessi: devono precedere il movimento nella stessa transazione, che li consuma per LIFO
        for (Map.Entry<String, BigInteger> e : interessi.entrySet()) {
            if (e.getValue().signum() <= 0) continue;
            Token t = Metadati(e.getKey(), metadati, rpc, avvisi);
            movimenti.add(new Movimento("REWARD", CAUSALE_INTERESSI, null, null, t, e.getValue(), e.getKey()));
            netto.merge(e.getKey(), e.getValue().negate(), BigInteger::add);
        }

        List<String> uscite = new ArrayList<>(), entrate = new ArrayList<>();
        for (Map.Entry<String, BigInteger> e : netto.entrySet()) {
            if (e.getValue().signum() < 0) uscite.add(e.getKey());
            else if (e.getValue().signum() > 0) entrate.add(e.getKey());
        }
        if (tokenCarta != null && uscite.contains(tokenCarta)) {
            uscite.remove(tokenCarta);
            Token t = Metadati(tokenCarta, metadati, rpc, avvisi);
            movimenti.add(new Movimento("CASHOUT O SIMILARE", CAUSALE_CARTA, t, netto.get(tokenCarta).negate(), null, null, raccoglitore));
        }
        if (uscite.size() == 1 && entrate.size() == 1) {
            String u = uscite.get(0), en = entrate.get(0);
            movimenti.add(new Movimento(null, "Scambio", Metadati(u, metadati, rpc, avvisi), netto.get(u).negate(),
                    Metadati(en, metadati, rpc, avvisi), netto.get(en), controparte.getOrDefault(u, controparte.getOrDefault(en, ""))));
        } else {
            if (uscite.size() + entrate.size() > 1) {
                avvisi.add("Transazione con piu' monete, importata come depositi e prelievi separati: da controllare.");
            }
            for (String u : uscite) {
                movimenti.add(new Movimento(null, "Prelievo", Metadati(u, metadati, rpc, avvisi), netto.get(u).negate(), null, null,
                        controparte.getOrDefault(u, "")));
            }
            for (String en : entrate) {
                movimenti.add(new Movimento(null, "Deposito", null, null, Metadati(en, metadati, rpc, avvisi), netto.get(en),
                        controparte.getOrDefault(en, "")));
            }
        }
        return movimenti;
    }

    private static boolean isAToken(String indirizzo, Map<String, Token> metadati) {
        Token t = metadati.get(indirizzo);
        return t != null && t.aToken();
    }

    /** Metadati di una moneta: dall'elenco, oppure da {@code symbol()} e {@code decimals()} sul nodo. */
    private static Token Metadati(String indirizzo, Map<String, Token> metadati, OKX_WalletCarta.Rpc rpc, List<String> avvisi) {
        Token t = metadati.get(indirizzo);
        if (t != null) return t;
        String simbolo = indirizzo;
        int decimali = 18;
        try {
            if (rpc != null) {
                String s = DecodificaStringa(Chiama(rpc, indirizzo, SEL_SYMBOL));
                if (!s.isBlank()) simbolo = s;
                decimali = new BigInteger(Chiama(rpc, indirizzo, SEL_DECIMALS).substring(2), 16).intValueExact();
            }
        } catch (Exception e) {
            avvisi.add("Moneta " + indirizzo + ": simbolo o decimali non leggibili, controllare la quantita'.");
        }
        t = new Token(indirizzo, simbolo, simbolo, decimali, false);
        metadati.put(indirizzo, t);
        avvisi.add("Moneta " + simbolo + " (" + indirizzo + ") fuori dall'elenco di X Layer: i suoi movimenti possono essere incompleti.");
        return t;
    }

    private static String Chiama(OKX_WalletCarta.Rpc rpc, String a, String selettore) throws Exception {
        JsonObject c = new JsonObject();
        c.addProperty("to", a);
        c.addProperty("data", "0x" + selettore);
        JsonArray par = new JsonArray();
        par.add(c);
        par.add("latest");
        return rpc.chiama("eth_call", par).getAsString();
    }

    /** Stringa ABI ({@code string} dinamica); stringa vuota se non decodificabile. */
    static String DecodificaStringa(String r) {
        try {
            String h = r.substring(2);
            int off = ParolaA(h, 0).intValueExact() * 2;
            int len = ParolaA(h, off).intValueExact();
            byte[] b = new byte[len];
            for (int i = 0; i < len; i++) b[i] = (byte) Integer.parseInt(h.substring(off + 64 + i * 2, off + 66 + i * 2), 16);
            return new String(b, java.nio.charset.StandardCharsets.UTF_8).replaceAll("[^A-Za-z0-9._-]", "");
        } catch (Exception e) {
            return "";
        }
    }

    //=====================================================================================================
    //=== RIGHE DEI MOVIMENTI
    //=====================================================================================================

    /**
     * Le righe della tabella movimenti di una transazione. I prezzi li cerca {@code creaMovimento}; numerate
     * 1..n nell'ordine dei movimenti, quindi la REWARD degli interessi viene prima del movimento che la consuma.
     */
    static List<String[]> Righe(String wallet, Transazione t) {
        return Righe(wallet, t, null);
    }

    /** Come {@link #Righe(String, Transazione)}; con {@code prezzoFisso} non si cerca nessun prezzo (test). */
    static List<String[]> Righe(String wallet, Transazione t, String prezzoFisso) {
        List<String[]> righe = new ArrayList<>();
        String nomeWallet = wallet + " (" + RETE + ")";
        int n = t.movimenti().size();
        for (int i = 0; i < n; i++) {
            Movimento m = t.movimenti().get(i);
            String[] rt = MovimentiCrypto.creaMovimento(Moneta(m.uscita(), m.qtaUscita(), true), Moneta(m.entrata(), m.qtaEntrata(), false),
                    nomeWallet, "Wallet", t.timestamp() * 1000, prezzoFisso, null, i + 1, n, null, null, "A", t.hash(), m.tipo(), null);
            if (rt == null) continue;
            rt[7] = m.causale();
            rt[23] = String.valueOf(t.blocco());
            if (rt.length > 30) rt[30] = m.controparte() == null ? "" : m.controparte();
            Importazioni.RiempiVuotiArray(rt);
            righe.add(rt);
        }
        return righe;
    }

    private static Moneta Moneta(Token t, BigInteger qta, boolean uscita) {
        if (t == null || qta == null) return null;
        BigDecimal q = new BigDecimal(qta).movePointLeft(t.decimali()).stripTrailingZeros();
        Moneta m = new Moneta();
        m.InserisciValori(t.simbolo(), (uscita ? q.negate() : q).toPlainString(), t.indirizzo(), "Crypto");
        m.SetNomeEsteso(t.nome());
        m.SetRete(RETE);
        return m;
    }

    /**
     * Le transazioni nella forma che si aspetta l'importazione DeFi ({@code GUI_GestioneWallets.AggiornaWallets}):
     * una {@link TransazioneDefi} per transazione, con le righe già pronte, costruite (e prezzate) solo quando
     * l'importazione le chiede.
     */
    static Map<String, TransazioneDefi> ComeTransazioniDefi(String wallet, List<Transazione> transazioni) {
        Map<String, TransazioneDefi> mappa = new LinkedHashMap<>();
        for (Transazione t : transazioni) {
            TransazioneDefi td = new TransazioneDefi();
            td.Rete = RETE;
            td.Wallet = wallet;
            td.HashTransazione = t.hash();
            td.Blocco = String.valueOf(t.blocco());
            td.DataOra = FunzioniDate.ConvertiDatadaLongAlSecondo(t.timestamp() * 1000);
            td.RighePronte = () -> Righe(wallet, t);
            mappa.put(wallet + "." + t.hash(), td);
        }
        return mappa;
    }

    //=====================================================================================================
    //=== AGGIORNAMENTO DEL WALLET DELLA CARTA DOPO UNO SCARICAMENTO OKX
    //=====================================================================================================

    record EsitoCarta(int aggiunti, List<String> avvisi, boolean completo) {}

    /**
     * Aggiorna il wallet della carta OKX dall'ultimo blocco già importato, come "Aggiorna" in Inserisci Wallet, e
     * rifà l'abbinamento ricariche/cashback ({@link OKX_CartaAbbina}). Lo chiama lo scaricamento OKX via API (scelta
     * dell'utente del 2026-10-09: un aggiornamento solo per exchange e carta). Niente se il wallet non è stato
     * individuato o se l'utente l'ha tolto dai wallet DeFi.
     * <p>Come l'importazione DeFi: un documento di origine per l'aggiornamento, e i movimenti si scrivono solo se
     * scansione e prezzi arrivano in fondo, altrimenti nulla (il blocco di partenza è l'ultimo movimento scritto).
     *
     * @param interrotto vero quando l'utente ha premuto Interrompi
     */
    static EsitoCarta AggiornaWalletCarta(java.util.function.BooleanSupplier interrotto) {
        return AggiornaWalletCarta(interrotto, null);
    }

    /** Come {@link #AggiornaWalletCarta(java.util.function.BooleanSupplier)}; {@code avanzamento} riceve la riga di stato. */
    static EsitoCarta AggiornaWalletCarta(java.util.function.BooleanSupplier interrotto, java.util.function.Consumer<String> avanzamento) {
        List<String> avvisi = new ArrayList<>();
        if (DatabaseH2.connectionPersonale == null) return new EsitoCarta(0, avvisi, true);
        String w = DatabaseH2.Pers_Opzioni_Leggi(OKX_WalletCarta.OPZIONE_WALLET);
        if (w == null || w.isBlank() || DatabaseH2.Pers_Wallets_LeggiTabella().get(w.trim() + "_" + RETE) == null) {
            return new EsitoCarta(0, avvisi, true);
        }
        w = w.trim();
        long ultimoBlocco = UltimoBloccoImportato(Principale.MappaCryptoWallet, w);
        int idDocumento = DocumentiFonte.ApriSessione("DeFi");
        Importazioni.DocumentoFonteCorrente = idDocumento;
        int aggiunti = 0;
        boolean completo = false;
        try {
            Esito e = Scarica(w, ultimoBlocco + 1, OKX_WalletCarta.RpcXLayer(), interrotto, idDocumento, avanzamento);
            avvisi.addAll(e.avvisi());
            if (e.completo()) {
                List<String[]> righe = new ArrayList<>();
                int n = 0, tot = e.transazioni().size();
                if (tot > 0) System.out.println("X Layer: ricerca dei prezzi per " + tot + (tot == 1 ? " transazione." : " transazioni."));
                for (Transazione t : e.transazioni()) {
                    if (interrotto.getAsBoolean()) break;
                    n++;
                    if (avanzamento != null) avanzamento.accept("Prezzi della transazione " + n + " di " + tot);
                    righe.addAll(Righe(w, t));
                }
                if (!interrotto.getAsBoolean()) {
                    for (String[] st : righe) {
                        Principale.Funzione_AggiornaMappaWallets(st);
                        st[0] = MovimentiCrypto.getIDUnivoco(Principale.MappaCryptoWallet, st[0]);
                        if (idDocumento > 0 && Funzioni.noData(st[41])) st[41] = String.valueOf(idDocumento);
                        Importazioni.InserisciMovimentosuMappaCryptoWallet(st[0], st);
                        aggiunti++;
                    }
                    completo = true;
                }
            }
        } finally {
            Importazioni.DocumentoFonteCorrente = 0;
            DocumentiFonte.ChiudiSessione(idDocumento);
            DocumentiFonte.ChiudiRegistrazione(new DocumentiFonte.Registrazione(idDocumento, true), aggiunti);
        }
        if (aggiunti > 0) {
            OKX_CartaAbbina.Abbina();
            Principale.TabellaCryptodaAggiornare = true;
        }
        for (String a : avvisi) LoggerGC.ScriviErrore("X Layer " + w + ": " + a);
        return new EsitoCarta(aggiunti, avvisi, completo);
    }

    /** Il blocco più alto fra i movimenti già importati del wallet ({@code [23]}), 0 se nessuno. */
    static long UltimoBloccoImportato(Map<String, String[]> archivio, String wallet) {
        String nome = wallet + " (" + RETE + ")";
        long massimo = 0;
        for (String[] v : archivio.values()) {
            if (v == null || v.length <= 23 || !nome.equalsIgnoreCase(v[3].trim())) continue;
            try {
                massimo = Math.max(massimo, Long.parseLong(v[23].trim()));
            } catch (NumberFormatException e) {
                //movimento senza blocco (inserito a mano): non dice da dove ripartire
            }
        }
        return massimo;
    }

    //=====================================================================================================
    //=== UTILITA'
    //=====================================================================================================

    private static long TempoBlocco(long numero, OKX_WalletCarta.Rpc rpc) throws Exception {
        JsonArray par = new JsonArray();
        par.add("0x" + Long.toHexString(numero));
        par.add(false);
        return Long.decode(rpc.chiama("eth_getBlockByNumber", par).getAsJsonObject().get("timestamp").getAsString());
    }

    /** Valore esadecimale (con o senza {@code 0x}, anche un indirizzo) allineato a destra su 32 byte. */
    static String Parola(String h) {
        String s = h.toLowerCase().startsWith("0x") ? h.substring(2) : h;
        return "0".repeat(Math.max(0, 64 - s.length())) + s.toLowerCase();
    }

    private static BigInteger ParolaA(String h, int pos) {
        return new BigInteger(h.substring(pos, pos + 64), 16);
    }

    /** La parola {@code i} (da 0) del campo {@code data} di un log; zero se non c'è. */
    static BigInteger ParolaDati(String dati, int i) {
        if (dati == null || dati.length() < 2 + (i + 1) * 64) return BigInteger.ZERO;
        return new BigInteger(dati.substring(2 + i * 64, 2 + (i + 1) * 64), 16);
    }

    /** Ultimi 20 byte di una parola a 32 byte, in minuscolo. */
    static String Indirizzo(String parola) {
        String h = parola.toLowerCase();
        return "0x" + h.substring(h.length() - 40);
    }

    private static String Testo(JsonObject o, String campo) {
        return o.has(campo) && !o.get(campo).isJsonNull() ? o.get(campo).getAsString().trim() : "";
    }
}
