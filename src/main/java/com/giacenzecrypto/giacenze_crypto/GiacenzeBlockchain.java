package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Le giacenze di un wallet DeFi lette dalla blockchain, per la colonna di confronto di "Giacenze a data".
 *
 * <p>Si leggono dai <b>nodi pubblici con lo stato storico</b> (archivio), alla data della tabella: l'ultimo blocco con
 * timestamp <b>strettamente</b> minore di {@code DataRiferimento}, lo stesso {@code <} con cui
 * {@link Principale_GiacenzeaData#SommaQuantitaAData} sceglie i movimenti, cosi' una transazione a mezzanotte cade dalla
 * stessa parte nei due conti. Se la data e' oggi si legge l'ultimo blocco.
 *
 * <p><b>Un saldo non letto non e' mai zero</b>: un nodo senza storico, una chiamata fallita o un contratto che non
 * risponde danno {@link #NON_DISPONIBILE}. Per questo i nodi di {@link #NODI} sono solo quelli verificati il 10/10/2026
 * a due altezze diverse contro gli altri nodi della stessa rete: alcuni nodi pubblici rispondono {@code 0x0} invece di
 * un errore per lo stato che non hanno (drpc su OP prima di Bedrock, Alchemy pubblico su World Chain), e il nodo
 * ufficiale di HyperEVM ignora il blocco e restituisce sempre il saldo di oggi. Quei nodi qui non ci sono.
 *
 * <p>Logica senza Swing; l'accesso ai nodi e' {@link NodoPubblicoDefi.Nodo}, che i test sostituiscono.
 */
public class GiacenzeBlockchain {

    /** Testo della cella quando il saldo non si e' potuto leggere. */
    public static final String NON_DISPONIBILE = "n.d.";
    /** Testo della cella mentre la lettura e' in corso. */
    public static final String IN_LETTURA = "lettura...";

    static final String MULTICALL3 = Trans_XLayer.MULTICALL3;
    /** Chiamate per richiesta {@code aggregate3}: due per token (saldo e decimali), quindi 100 token. */
    static final int CHIAMATE_PER_LOTTO = 200;

    /**
     * Nodi con lo stato storico, rete per rete (verificati il 10/10/2026: saldo a meta' e a un quinto della catena
     * uguale fra i nodi elencati, diverso da quello di oggi). BASE e XLAYER non sono qui: usano gli stessi nodi della
     * loro importazione ({@link #Nodi}). MONAD non ha nessun nodo pubblico con lo storico: solo la giacenza di oggi.
     */
    static final Map<String, List<String>> NODI = Map.ofEntries(
            Map.entry("ETH", List.of("https://mainnet.gateway.tenderly.co", "https://eth-mainnet.public.blastapi.io",
                    "https://eth.drpc.org")),
            Map.entry("BSC", List.of("https://bsc-mainnet.public.blastapi.io")),
            Map.entry("ARB", List.of("https://arbitrum.gateway.tenderly.co", "https://arbitrum-one.public.blastapi.io")),
            Map.entry("POL", List.of("https://polygon.gateway.tenderly.co", "https://polygon.drpc.org")),
            Map.entry("AVAX", List.of("https://avalanche.gateway.tenderly.co", "https://api.avax.network/ext/bc/C/rpc")),
            Map.entry("CRO", List.of("https://evm.cronos.org", "https://cronos.drpc.org")),
            Map.entry("OP", List.of("https://mainnet.optimism.io")),
            Map.entry("GNOSIS", List.of("https://rpc.gnosischain.com", "https://gnosis.gateway.tenderly.co")),
            Map.entry("LINEA", List.of("https://rpc.linea.build", "https://linea.gateway.tenderly.co")),
            Map.entry("BLAST", List.of("https://rpc.blast.io")),
            Map.entry("UNICHAIN", List.of("https://mainnet.unichain.org", "https://unichain.gateway.tenderly.co")),
            Map.entry("WORLD", List.of("https://worldchain-mainnet.gateway.tenderly.co", "https://worldchain.drpc.org")),
            Map.entry("TAIKO", List.of("https://rpc.mainnet.taiko.xyz", "https://taiko.drpc.org")),
            Map.entry("ABSTRACT", List.of("https://api.mainnet.abs.xyz", "https://abstract.drpc.org")),
            Map.entry("KATANA", List.of("https://rpc.katana.network", "https://katana.drpc.org")),
            Map.entry("SONIC", List.of("https://rpc.soniclabs.com", "https://sonic.gateway.tenderly.co")),
            Map.entry("MANTLE", List.of("https://rpc.mantle.xyz", "https://mantle.drpc.org")),
            Map.entry("HYPEREVM", List.of("https://hyperliquid.drpc.org")),
            Map.entry("INK", List.of("https://rpc-gel.inkonchain.com", "https://ink.gateway.tenderly.co")),
            Map.entry("ROBINHOOD", List.of("https://robinhood.drpc.org")),
            Map.entry("BERA", List.of("https://rpc.berachain.com", "https://berachain.gateway.tenderly.co")),
            Map.entry("MONAD", List.of("https://rpc.monad.xyz", "https://monad.drpc.org")));

    /** Reti i cui nodi pubblici non hanno lo stato storico: si legge solo l'ultimo blocco. */
    static final java.util.Set<String> SENZA_STORICO = java.util.Set.of("MONAD");

    /** @return i nodi della rete, vuoto se la rete non si legge (SOL, BTC, reti non EVM) */
    static List<String> Nodi(String Rete) {
        if (Rete == null) return List.of();
        String r = Rete.toUpperCase();
        if (NodoPubblicoDefi.RETI.containsKey(r)) return NodoPubblicoDefi.RETI.get(r).nodiStato();
        if (r.equals(Trans_XLayer.RETE)) return List.of(OKX_WalletCarta.RPC_XLAYER, "https://xlayer.drpc.org");
        return NODI.getOrDefault(r, List.of());
    }

    /** @return {@code true} se le giacenze della rete si leggono dalla blockchain */
    public static boolean ReteSupportata(String Rete) {
        return !Nodi(Rete).isEmpty();
    }

    /**
     * Il sotto-wallet che corrisponde all'indirizzo sulla blockchain: l'importazione DeFi ci mette ogni movimento
     * ({@code TransazioneDefi}). Gli altri sotto-wallet di un wallet DeFi ("Piattaforma/DeFi", "Collaterale Bloccato",
     * "Piattaforma di scambio") li crea la classificazione e tengono monete che sulla blockchain hanno lasciato
     * l'indirizzo: un token messo in una farm e' ancora del wallet nell'archivio ma non e' piu' all'indirizzo.
     */
    public static final String SOTTOWALLET_INDIRIZZO = "Wallet";

    /**
     * @return {@code [indirizzo, rete]} se il wallet ({@code "0x... (RETE)"}) e' un wallet DeFi EVM di una rete che si
     *         legge, altrimenti {@code null}
     */
    static String[] IndirizzoERete(String Wallet) {
        if (Wallet == null) return null;
        String w = Wallet.trim();
        if (!Funzioni_WalletDeFi.isValidDefiWallet(w)) return null;
        String Indirizzo = w.split("\\(")[0].trim();
        String Rete = w.split("\\(")[1].replace(")", "").trim().toUpperCase();
        if (!ReteSupportata(Rete) || !Indirizzo.matches("0x[0-9a-fA-F]{40}")) return null;
        return new String[]{Indirizzo, Rete};
    }

    /**
     * Il wallet di cui leggere le giacenze, se la selezione di "Giacenze a data" lo permette: un singolo wallet DeFi
     * di una rete supportata, con il sotto-wallet {@link #SOTTOWALLET_INDIRIZZO}, oppure "Tutti" quando il wallet
     * non ha altri sotto-wallet. Con "Tutti" e altri sotto-wallet la Qta della riga comprende monete che non sono
     * all'indirizzo, e il confronto mostrerebbe differenze che non esistono.
     *
     * @param SottoWallets i sotto-wallet del wallet ({@code Mappa_Wallets_e_Dettagli}), {@code null} se ignoti
     * @return {@code [indirizzo, rete]}, oppure {@code null} se il confronto non si fa
     */
    public static String[] WalletDaLeggere(String Wallet, String SottoWallet, java.util.Collection<String> SottoWallets) {
        if (SottoWallet == null) return null;
        String sw = SottoWallet.trim();
        boolean Indirizzo = sw.equalsIgnoreCase(SOTTOWALLET_INDIRIZZO);
        boolean TuttiSoloIndirizzo = sw.equalsIgnoreCase("Tutti") && (SottoWallets == null
                || SottoWallets.stream().allMatch(x -> x == null || x.trim().equalsIgnoreCase(SOTTOWALLET_INDIRIZZO)));
        if (!Indirizzo && !TuttiSoloIndirizzo) return null;
        return IndirizzoERete(Wallet);
    }

    /** Come si legge una riga della tabella. */
    enum TipoRiga { NATIVA, TOKEN, ESCLUSA }

    /**
     * La moneta della rete ha come address il proprio simbolo (es. {@code ETH} su BASE), i token l'indirizzo del
     * contratto. Gli NFT e le righe non Crypto restano fuori: il {@code balanceOf} di una collezione conta i pezzi, non
     * la quantita' della riga.
     *
     * @param Nativa simbolo della moneta della rete ({@code Mappa_ChainExplorer[2]}), {@code null} se ignoto
     */
    static TipoRiga Classifica(String Moneta, String Tipo, String Address, String Nativa) {
        if (!"Crypto".equalsIgnoreCase(Tipo)) return TipoRiga.ESCLUSA;
        if (Address != null && Address.trim().matches("0x[0-9a-fA-F]{40}")) return TipoRiga.TOKEN;
        if (Nativa != null && Moneta != null && Moneta.trim().equalsIgnoreCase(Nativa)
                && (Address == null || Address.isBlank() || Address.trim().equalsIgnoreCase(Nativa))) {
            return TipoRiga.NATIVA;
        }
        return TipoRiga.ESCLUSA;
    }

    /** @return il simbolo della moneta della rete, {@code null} se la rete non e' nota */
    static String Nativa(String Rete) {
        String[] c = Rete == null ? null : Principale.Mappa_ChainExplorer.get(Rete.toUpperCase());
        return c != null && c.length > 2 && !c[2].isBlank() ? c[2] : null;
    }

    /** Una riga della tabella da confrontare: {@code chiave} e' {@link Principale_GiacenzeaData#ChiaveRiga}. */
    record Richiesta(String chiave, String moneta, String tipo, String address) {}

    /**
     * @param testi chiave della riga → quantita' letta ({@code toPlainString}) o {@link #NON_DISPONIBILE}; le righe
     *        escluse (NFT, monete senza indirizzo) non ci sono
     * @param blocco il blocco letto, {@code -1} se non determinato
     */
    record Esito(Map<String, String> testi, long blocco, List<String> avvisi) {}

    /**
     * Legge dai nodi pubblici della rete le giacenze delle righe, alla data della tabella.
     *
     * @param DataRiferimento lo stesso limite (escluso) di {@link Principale_GiacenzeaData#SommaQuantitaAData};
     *        {@code <= 0}: la data e' oggi, si legge l'ultimo blocco e non si scrive nulla in cache. Lo decide il
     *        chiamante e non un confronto con l'orologio: la lettura parte dopo il calcolo dei prezzi, anche minuti
     *        dopo, e un istante "quasi adesso" diventato passato riempirebbe la cache di saldi di blocchi recenti
     * @param interrotto vero se il risultato non serve piu' (l'utente ha ricalcolato la tabella)
     * @param nodo i nodi, {@code null} per quelli pubblici della rete
     */
    static Esito Leggi(String Indirizzo, String Rete, long DataRiferimento, List<Richiesta> Righe,
            BooleanSupplier interrotto, NodoPubblicoDefi.Nodo nodo) {
        Map<String, String> testi = new LinkedHashMap<>();
        List<String> avvisi = new ArrayList<>();
        String Nativa = Nativa(Rete);
        List<Richiesta> Native = new ArrayList<>();
        List<Richiesta> Token = new ArrayList<>();
        for (Richiesta r : Righe) {
            switch (Classifica(r.moneta(), r.tipo(), r.address(), Nativa)) {
                case NATIVA -> Native.add(r);
                case TOKEN -> Token.add(r);
                default -> { }
            }
        }
        if (Native.isEmpty() && Token.isEmpty()) return new Esito(testi, -1, avvisi);
        for (Richiesta r : Native) testi.put(r.chiave(), NON_DISPONIBILE);
        for (Richiesta r : Token) testi.put(r.chiave(), NON_DISPONIBILE);
        if (nodo == null) {
            nodo = new NodoPubblicoDefi.NodiPubblici(new NodoPubblicoDefi.Configurazione(Rete.toUpperCase(),
                    Nodi(Rete), List.of(), List.of(), List.of(), List.of()));
        }
        final NodoPubblicoDefi.Nodo n = nodo;
        OKX_WalletCarta.Rpc rpc = (metodo, parametri) -> n.chiama(NodoPubblicoDefi.Uso.STATO, metodo, parametri);
        boolean Oggi = DataRiferimento <= 0 || DataRiferimento > System.currentTimeMillis();
        long Blocco;
        try {
            if (Oggi) {
                Blocco = Long.decode(rpc.chiama("eth_blockNumber", new JsonArray()).getAsString());
            } else if (SENZA_STORICO.contains(Rete.toUpperCase())) {
                avvisi.add("I nodi pubblici di " + Rete + " non hanno le giacenze passate: si leggono solo quelle di oggi.");
                return new Esito(testi, -1, avvisi);
            } else {
                Blocco = UltimoBloccoPrima(Rete, DataRiferimento / 1000, rpc);
            }
        } catch (Exception ex) {
            avvisi.add("Blocco alla data non determinato su " + Rete + ": " + ex.getMessage());
            return new Esito(testi, -1, avvisi);
        }
        if (interrotto != null && interrotto.getAsBoolean()) return new Esito(testi, Blocco, avvisi);

        LeggiAlBlocco(Indirizzo, Rete, Blocco, Native, Token, !Oggi, interrotto, n, testi, avvisi);
        return new Esito(testi, Blocco, avvisi);
    }

    /**
     * Legge i saldi delle righe a un blocco preciso, mettendo in {@code testi} la quantita' di ogni chiave letta
     * (quelle non lette restano come sono, di solito {@link #NON_DISPONIBILE}).
     *
     * @param Cache se riprendere e salvare i saldi in {@code GIACENZEBLOCKCHAIN}: solo per un blocco gia' passato,
     *        mai per l'ultimo
     */
    static void LeggiAlBlocco(String Indirizzo, String Rete, long Blocco, List<Richiesta> Native, List<Richiesta> Token,
            boolean Cache, BooleanSupplier interrotto, NodoPubblicoDefi.Nodo n, Map<String, String> testi,
            List<String> avvisi) {
        OKX_WalletCarta.Rpc rpc = (metodo, parametri) -> n.chiama(NodoPubblicoDefi.Uso.STATO, metodo, parametri);
        boolean Oggi = !Cache;
        //Un blocco passato non cambia piu': i saldi gia' letti si riprendono dalla cache (mai quelli di oggi)
        String Prefisso = ChiaveSaldo(Rete, Indirizzo, Blocco, "");
        List<Richiesta> DaLeggereNative = new ArrayList<>();
        List<Richiesta> DaLeggereToken = new ArrayList<>();
        for (Richiesta r : Native) {
            String c = Oggi ? null : CacheLeggi(Prefisso + "NATIVA");
            if (c != null) testi.put(r.chiave(), c);
            else DaLeggereNative.add(r);
        }
        for (Richiesta r : Token) {
            String c = Oggi ? null : CacheLeggi(Prefisso + r.address().trim().toLowerCase());
            if (c != null) testi.put(r.chiave(), c);
            else DaLeggereToken.add(r);
        }
        if (DaLeggereNative.isEmpty() && DaLeggereToken.isEmpty()) return;

        String Esa = "0x" + Long.toHexString(Blocco);
        try {
            boolean ConMulticall = HaCodice(MULTICALL3, Esa, rpc);
            BigInteger SaldoNativo = null;
            Map<String, BigInteger[]> SaldiToken = new LinkedHashMap<>();//address -> [saldo, decimali]
            List<String> Indirizzi = new ArrayList<>();
            for (Richiesta r : DaLeggereToken) {
                String a = r.address().trim().toLowerCase();
                if (!Indirizzi.contains(a)) Indirizzi.add(a);
            }
            if (ConMulticall) {
                List<String[]> Chiamate = new ArrayList<>();
                if (!DaLeggereNative.isEmpty()) {
                    Chiamate.add(new String[]{MULTICALL3, Trans_XLayer.SEL_GET_ETH_BALANCE + Trans_XLayer.Parola(Indirizzo)});
                }
                for (String a : Indirizzi) {
                    Chiamate.add(new String[]{a, Trans_XLayer.SEL_BALANCE_OF + Trans_XLayer.Parola(Indirizzo)});
                    Chiamate.add(new String[]{a, Trans_XLayer.SEL_DECIMALS});
                }
                List<BigInteger> Valori = new ArrayList<>();
                for (int i = 0; i < Chiamate.size(); i += CHIAMATE_PER_LOTTO) {
                    if (interrotto != null && interrotto.getAsBoolean()) return;
                    List<String[]> Lotto = Chiamate.subList(i, Math.min(Chiamate.size(), i + CHIAMATE_PER_LOTTO));
                    JsonObject c = new JsonObject();
                    c.addProperty("to", MULTICALL3);
                    c.addProperty("data", Trans_XLayer.Aggregate3(Lotto));
                    JsonArray p = new JsonArray();
                    p.add(c);
                    p.add(Esa);
                    List<BigInteger> v = DecodificaAggregate3(rpc.chiama("eth_call", p).getAsString());
                    if (v.size() != Lotto.size()) throw new Exception("risposta di Multicall3 incompleta");
                    Valori.addAll(v);
                }
                int k = 0;
                if (!DaLeggereNative.isEmpty()) SaldoNativo = Valori.get(k++);
                for (String a : Indirizzi) SaldiToken.put(a, new BigInteger[]{Valori.get(k++), Valori.get(k++)});
            } else {
                //Prima che Multicall3 esistesse sulla rete: una chiamata per saldo
                if (!DaLeggereNative.isEmpty()) {
                    JsonArray p = new JsonArray();
                    p.add(Indirizzo);
                    p.add(Esa);
                    SaldoNativo = new BigInteger(Esadecimale(rpc.chiama("eth_getBalance", p).getAsString()), 16);
                }
                for (String a : Indirizzi) {
                    if (interrotto != null && interrotto.getAsBoolean()) return;
                    SaldiToken.put(a, new BigInteger[]{Chiama(a, Trans_XLayer.SEL_BALANCE_OF + Trans_XLayer.Parola(Indirizzo), Esa, n),
                        Chiama(a, Trans_XLayer.SEL_DECIMALS, Esa, n)});
                }
            }
            for (Richiesta r : DaLeggereNative) {
                String t = Quantita(SaldoNativo, BigInteger.valueOf(18));
                testi.put(r.chiave(), t);
                if (!Oggi && !t.equals(NON_DISPONIBILE)) CacheScrivi(Prefisso + "NATIVA", t);
            }
            for (Richiesta r : DaLeggereToken) {
                String a = r.address().trim().toLowerCase();
                BigInteger[] s = SaldiToken.get(a);
                String t = s == null ? NON_DISPONIBILE : Quantita(s[0], s[1]);
                testi.put(r.chiave(), t);
                if (!Oggi && !t.equals(NON_DISPONIBILE)) CacheScrivi(Prefisso + a, t);
            }
        } catch (Exception ex) {
            avvisi.add("Lettura delle giacenze su " + Rete + " al blocco " + Blocco + " non riuscita: " + ex.getMessage());
        }
    }

    //=====================================================================================================
    //=== DETTAGLIO MOVIMENTO: SALDI AL BLOCCO PRECEDENTE E A QUELLO DEL MOVIMENTO
    //=====================================================================================================

    /**
     * I saldi sulla blockchain delle monete di un movimento a fine del blocco precedente ({@code prima}) e a fine del
     * blocco del movimento ({@code dopo}), con la giacenza dell'archivio agli stessi confini nel sotto-wallet
     * {@link #SOTTOWALLET_INDIRIZZO}, l'unico che sta all'indirizzo.
     *
     * <p>I confini dell'archivio non sono "prima e dopo il movimento" ma "prima e dopo il secondo del movimento": una
     * transazione DeFi diventa spesso piu' movimenti (la commissione, lo scambio, il trasferimento interno) e la
     * blockchain non li separa, il blocco li contiene tutti. {@code altriMovimenti} conta gli altri movimenti del wallet
     * in quel secondo. Su una rete con piu' blocchi al secondo, un secondo blocco del wallet nello stesso secondo
     * finirebbe dalla parte sbagliata: caso raro, non gestito.
     *
     * @param prima chiave della moneta ({@link Principale_GiacenzeaData#ChiaveRiga}) → quantita' o {@link #NON_DISPONIBILE}
     * @param inLettura vero finche' la lettura e' in corso: i testi sono {@link #IN_LETTURA}
     */
    public record SaldiMovimento(long blocco, Map<String, String> prima, Map<String, String> dopo,
            Map<String, BigDecimal> archivioPrima, Map<String, BigDecimal> archivioDopo, int altriMovimenti,
            boolean inLettura, List<String> avvisi) {

        /** @return {@code true} se la moneta ha le righe della blockchain */
        public boolean Contiene(String Chiave) {
            return prima.containsKey(Chiave);
        }

        /** Testo della quantita' sulla blockchain. */
        public String Testo(String Chiave, boolean Dopo) {
            if (inLettura) return IN_LETTURA;
            String t = (Dopo ? dopo : prima).get(Chiave);
            return t == null ? NON_DISPONIBILE : t;
        }

        /** Giacenza dell'archivio al confine, zero se il wallet non aveva la moneta. */
        public BigDecimal Archivio(String Chiave, boolean Dopo) {
            BigDecimal a = (Dopo ? archivioDopo : archivioPrima).get(Chiave);
            return a == null ? BigDecimal.ZERO : a;
        }

        /** Gli stessi saldi tutti {@link #NON_DISPONIBILE}: la lettura e' fallita. */
        public SaldiMovimento NonLetti() {
            Map<String, String> m = new LinkedHashMap<>();
            for (String k : prima.keySet()) m.put(k, NON_DISPONIBILE);
            return new SaldiMovimento(blocco, m, m, archivioPrima, archivioDopo, altriMovimenti, false, avvisi);
        }

        /** @see GiacenzeBlockchain#Coincide */
        public Boolean Coincide(String Chiave, boolean Dopo) {
            return GiacenzeBlockchain.Coincide(Archivio(Chiave, Dopo).toPlainString(), Testo(Chiave, Dopo));
        }
    }

    /** @return il numero di blocco del movimento ({@code v[23]}), {@code -1} se manca o non e' un numero */
    static long BloccoMovimento(String[] Mov) {
        try {
            long b = Long.parseLong(Mov[23].trim());
            return b > 0 ? b : -1;
        } catch (RuntimeException ex) {
            return -1;
        }
    }

    /**
     * Le monete del movimento da leggere sulla blockchain: nessuna se il wallet non e' un wallet DeFi di una rete
     * leggibile, se manca il blocco o se le monete non sono ne' la moneta della rete ne' token.
     */
    static List<Richiesta> RichiesteMovimento(String[] Mov) {
        List<Richiesta> r = new ArrayList<>();
        if (Mov == null || Mov.length < 29 || BloccoMovimento(Mov) < 0) return r;
        String[] w = IndirizzoERete(Mov[3]);
        if (w == null) return r;
        String Rete = Funzioni.TrovaReteDaIMovimento(Mov);
        if (Rete == null || !Rete.equalsIgnoreCase(w[1])) return r;
        String Nativa = Nativa(Rete);
        int[][] Gambe = {{8, 9, 26}, {11, 12, 28}};
        for (int[] g : Gambe) {
            if (Mov[g[0]].isBlank() || Classifica(Mov[g[0]], Mov[g[1]], Mov[g[2]], Nativa) == TipoRiga.ESCLUSA) continue;
            String Chiave = Principale_GiacenzeaData.ChiaveRiga(Mov[g[0]], Mov[g[1]], Mov[g[2]], Rete);
            if (r.stream().noneMatch(x -> x.chiave().equals(Chiave))) {
                r.add(new Richiesta(Chiave, Mov[g[0]], Mov[g[1]], Mov[g[2]]));
            }
        }
        return r;
    }

    /** I saldi "in lettura" da mostrare subito, {@code null} se il movimento non ha righe della blockchain. */
    public static SaldiMovimento InLettura(String ID) {
        String[] Mov = Principale.MappaCryptoWallet.get(ID);
        List<Richiesta> r = RichiesteMovimento(Mov);
        if (r.isEmpty()) return null;
        Map<String, String> m = new LinkedHashMap<>();
        for (Richiesta x : r) m.put(x.chiave(), IN_LETTURA);
        return new SaldiMovimento(BloccoMovimento(Mov), m, m, Map.of(), Map.of(), 0, true, List.of());
    }

    /**
     * Legge i saldi attorno al movimento (rete) e le giacenze dell'archivio agli stessi confini (memoria).
     *
     * @param nodo i nodi, {@code null} per quelli pubblici della rete
     * @return {@code null} se il movimento non ha righe della blockchain
     */
    public static SaldiMovimento LeggiAttornoAlMovimento(String ID, NodoPubblicoDefi.Nodo nodo) {
        String[] Mov = Principale.MappaCryptoWallet.get(ID);
        List<Richiesta> Righe = RichiesteMovimento(Mov);
        if (Righe.isEmpty()) return null;
        String[] w = IndirizzoERete(Mov[3]);
        long Blocco = BloccoMovimento(Mov);
        String Nativa = Nativa(w[1]);
        List<Richiesta> Native = new ArrayList<>();
        List<Richiesta> Token = new ArrayList<>();
        for (Richiesta r : Righe) {
            if (Classifica(r.moneta(), r.tipo(), r.address(), Nativa) == TipoRiga.NATIVA) Native.add(r);
            else Token.add(r);
        }
        Map<String, String> Prima = new LinkedHashMap<>();
        Map<String, String> Dopo = new LinkedHashMap<>();
        for (Richiesta r : Righe) {
            Prima.put(r.chiave(), NON_DISPONIBILE);
            Dopo.put(r.chiave(), NON_DISPONIBILE);
        }
        List<String> avvisi = new ArrayList<>();
        if (SENZA_STORICO.contains(w[1])) {
            avvisi.add("I nodi pubblici di " + w[1] + " non hanno le giacenze passate.");
        } else {
            if (nodo == null) {
                nodo = new NodoPubblicoDefi.NodiPubblici(new NodoPubblicoDefi.Configurazione(w[1],
                        Nodi(w[1]), List.of(), List.of(), List.of(), List.of()));
            }
            //I blocchi di un movimento gia' in archivio sono passati: i saldi vanno in cache
            LeggiAlBlocco(w[0], w[1], Blocco - 1, Native, Token, true, null, nodo, Prima, avvisi);
            LeggiAlBlocco(w[0], w[1], Blocco, Native, Token, true, null, nodo, Dopo, avvisi);
        }
        //Giacenze dell'archivio nel sotto-wallet dell'indirizzo prima e dopo il secondo del movimento
        String Secondo = ID.length() >= 14 ? ID.substring(0, 14) : ID;
        List<String[]> PrimaDelSecondo = new ArrayList<>();
        List<String[]> NelSecondo = new ArrayList<>();
        for (String[] v : Principale.MappaCryptoWallet.headMap(Secondo, false).values()) {
            if (DellIndirizzo(v, Mov[3])) PrimaDelSecondo.add(v);
        }
        for (Map.Entry<String, String[]> e : Principale.MappaCryptoWallet.tailMap(Secondo, true).entrySet()) {
            if (!e.getKey().regionMatches(true, 0, Secondo, 0, Secondo.length())) break;
            if (DellIndirizzo(e.getValue(), Mov[3])) NelSecondo.add(e.getValue());
        }
        Map<String, BigDecimal> ArchivioPrima = SommaArchivio(PrimaDelSecondo, Mov[3].trim(), Righe);
        PrimaDelSecondo.addAll(NelSecondo);
        Map<String, BigDecimal> ArchivioDopo = SommaArchivio(PrimaDelSecondo, Mov[3].trim(), Righe);
        return new SaldiMovimento(Blocco, Prima, Dopo, ArchivioPrima, ArchivioDopo, Math.max(0, NelSecondo.size() - 1),
                false, avvisi);
    }

    /** Il movimento e' del wallet e del sotto-wallet che corrisponde all'indirizzo. */
    private static boolean DellIndirizzo(String[] v, String Wallet) {
        return v[3].trim().equalsIgnoreCase(Wallet.trim()) && v[4].trim().equalsIgnoreCase(SOTTOWALLET_INDIRIZZO);
    }

    private static Map<String, BigDecimal> SommaArchivio(List<String[]> Movimenti, String Wallet, List<Richiesta> Righe) {
        Map<String, Moneta> Somme = Principale_GiacenzeaData.SommaQuantitaAData(Movimenti, Long.MAX_VALUE, Wallet, "Tutti");
        Map<String, BigDecimal> r = new LinkedHashMap<>();
        for (Richiesta x : Righe) {
            Moneta m = Somme.get(x.chiave());
            r.put(x.chiave(), m == null ? BigDecimal.ZERO : new BigDecimal(m.Qta));
        }
        return r;
    }

    //=====================================================================================================
    //=== TABELLA DETTAGLIO DI "GIACENZE A DATA": UNA MONETA A FINE DI OGNI BLOCCO
    //=====================================================================================================

    /** Le righe del dettaglio fra cui si legge la blockchain: la moneta della riga scelta, se si puo' leggere. */
    public static Richiesta RichiestaDettaglio(String Moneta, String Tipo, String Address, String Rete, String ReteWallet) {
        if (Rete == null || ReteWallet == null || !Rete.equalsIgnoreCase(ReteWallet)) return null;
        if (Classifica(Moneta, Tipo, Address, Nativa(ReteWallet)) == TipoRiga.ESCLUSA) return null;
        return new Richiesta(Principale_GiacenzeaData.ChiaveRiga(Moneta, Tipo, Address, Rete), Moneta, Tipo, Address);
    }

    /**
     * Una riga costruita dal dettaglio, nell'ordine del model.
     *
     * @param blocco {@code v[23]} del movimento, {@code -1} se manca
     * @param rigaModello la riga del model, {@code -1} se il filtro "solo giacenze negative" non la mostra (conta lo
     *        stesso per la giacenza a fine secondo)
     * @param qtaResidua la Qta Residua dopo la riga
     */
    public record RigaDettaglio(String id, long blocco, int rigaModello, String qtaResidua) {}

    /**
     * Le righe mostrate di un secondo, tutte confrontate con lo stesso saldo: quello a fine del blocco piu' alto del
     * secondo, contro la Qta Residua dopo l'ultima riga del secondo ({@code archivio}).
     */
    public record BloccoDettaglio(long blocco, String archivio, List<Integer> righe, List<String> id) {}

    /**
     * Raggruppa per secondo (i primi 14 caratteri dell'ID) le righe del dettaglio. Il motivo e' quello del dettaglio
     * movimento ({@link #LeggiAttornoAlMovimento}): una transazione DeFi diventa piu' movimenti della stessa moneta
     * (commissione e scambio sulla moneta della rete, per esempio) e la Qta Residua della riga di mezzo non e' mai
     * esistita sulla blockchain. Il gruppo e' per secondo e non per {@code v[23]} perche' le righe generate dalla
     * classificazione possono non avere il blocco. Un secondo senza nessun blocco, o senza righe mostrate, non c'e'.
     */
    public static List<BloccoDettaglio> BlocchiDettaglio(List<RigaDettaglio> Righe) {
        Map<String, long[]> Blocco = new LinkedHashMap<>();
        Map<String, String> Archivio = new LinkedHashMap<>();
        Map<String, List<Integer>> Mostrate = new LinkedHashMap<>();
        Map<String, List<String>> Id = new LinkedHashMap<>();
        for (RigaDettaglio r : Righe) {
            String Secondo = r.id().length() >= 14 ? r.id().substring(0, 14) : r.id();
            Blocco.computeIfAbsent(Secondo, k -> new long[]{-1})[0] = Math.max(Blocco.get(Secondo)[0], r.blocco());
            Archivio.put(Secondo, r.qtaResidua());
            if (r.rigaModello() >= 0) {
                Mostrate.computeIfAbsent(Secondo, k -> new ArrayList<>()).add(r.rigaModello());
                Id.computeIfAbsent(Secondo, k -> new ArrayList<>()).add(r.id());
            }
        }
        List<BloccoDettaglio> l = new ArrayList<>();
        for (Map.Entry<String, long[]> e : Blocco.entrySet()) {
            if (e.getValue()[0] < 0 || !Mostrate.containsKey(e.getKey())) continue;
            l.add(new BloccoDettaglio(e.getValue()[0], Archivio.get(e.getKey()), Mostrate.get(e.getKey()), Id.get(e.getKey())));
        }
        return l;
    }

    /**
     * La giacenza da proporre per una riga del dettaglio perche' l'archivio, a fine del suo secondo, coincida con il
     * saldo letto a fine blocco: la Qta Residua della riga piu' la differenza fra blockchain e archivio. Sulla riga di
     * mezzo di un secondo non e' il saldo della blockchain, perche' i movimenti che seguono nello stesso blocco restano.
     *
     * @return {@code null} se non c'e' una differenza da allineare (saldo non letto, in lettura, uguale)
     */
    public static BigDecimal GiacenzaPerAllineare(Object QtaResidua, Object ArchivioFineSecondo, Object Blockchain) {
        if (!Boolean.FALSE.equals(Coincide(ArchivioFineSecondo, Blockchain))) return null;
        try {
            BigDecimal Differenza = new BigDecimal(Blockchain.toString().trim())
                    .subtract(new BigDecimal(ArchivioFineSecondo.toString().trim()));
            return new BigDecimal(QtaResidua.toString().trim()).add(Differenza).stripTrailingZeros();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** Quanti blocchi letti si consegnano insieme a {@code Parziale}. */
    static final int BLOCCHI_PER_CONSEGNA = 10;

    /**
     * La giacenza di una moneta dell'indirizzo a fine di ciascun blocco: per la colonna della tabella dettaglio, che
     * ha una moneta e molti blocchi (il contrario di {@link #LeggiAlBlocco}). Si legge un saldo per blocco, senza
     * Multicall3, e i decimali del token una volta sola. I blocchi gia' in cache ({@code GIACENZEBLOCKCHAIN}, stesse
     * chiavi della colonna di "Giacenze a data" e del dettaglio movimento) arrivano subito e non toccano la rete.
     *
     * <p>Se un nodo non risponde (non un revert, che e' la risposta del contratto) ci si ferma: ogni blocco
     * aspetterebbe gli stessi tentativi, e i blocchi rimasti sono {@link #NON_DISPONIBILE}.
     *
     * @param Parziale riceve i saldi a gruppi, man mano che arrivano (blocco → quantita' o {@link #NON_DISPONIBILE})
     * @param nodo i nodi, {@code null} per quelli pubblici della rete
     */
    public static void LeggiAiBlocchi(String Indirizzo, String Rete, Richiesta Moneta, java.util.Collection<Long> Blocchi,
            BooleanSupplier interrotto, NodoPubblicoDefi.Nodo nodo, java.util.function.Consumer<Map<Long, String>> Parziale,
            List<String> avvisi) {
        TipoRiga Tipo = Classifica(Moneta.moneta(), Moneta.tipo(), Moneta.address(), Nativa(Rete));
        if (Tipo == TipoRiga.ESCLUSA || Blocchi.isEmpty()) return;
        String Fine = Tipo == TipoRiga.NATIVA ? "NATIVA" : Moneta.address().trim().toLowerCase();
        List<Long> Ordinati = new ArrayList<>(new java.util.TreeSet<>(Blocchi));
        if (SENZA_STORICO.contains(Rete.toUpperCase())) {
            avvisi.add("I nodi pubblici di " + Rete + " non hanno le giacenze passate.");
            Map<Long, String> m = new LinkedHashMap<>();
            for (long b : Ordinati) m.put(b, NON_DISPONIBILE);
            Parziale.accept(m);
            return;
        }
        Map<Long, String> DallaCache = new LinkedHashMap<>();
        List<Long> DaLeggere = new ArrayList<>();
        for (long b : Ordinati) {
            String c = CacheLeggi(ChiaveSaldo(Rete, Indirizzo, b, Fine));
            if (c != null) DallaCache.put(b, c);
            else DaLeggere.add(b);
        }
        if (!DallaCache.isEmpty()) Parziale.accept(DallaCache);
        if (DaLeggere.isEmpty()) return;

        if (nodo == null) {
            nodo = new NodoPubblicoDefi.NodiPubblici(new NodoPubblicoDefi.Configurazione(Rete.toUpperCase(),
                    Nodi(Rete), List.of(), List.of(), List.of(), List.of()));
        }
        Map<Long, String> Lotto = new LinkedHashMap<>();
        int Letti = 0;
        try {
            BigInteger Decimali = BigInteger.valueOf(18);
            if (Tipo == TipoRiga.TOKEN) {
                Decimali = Chiama(Fine, Trans_XLayer.SEL_DECIMALS, "0x" + Long.toHexString(DaLeggere.get(DaLeggere.size() - 1)), nodo);
                if (Decimali == null) throw new Exception("il token non risponde a decimals()");
            }
            for (long b : DaLeggere) {
                if (interrotto != null && interrotto.getAsBoolean()) return;
                String Esa = "0x" + Long.toHexString(b);
                BigInteger Grezzo;
                if (Tipo == TipoRiga.NATIVA) {
                    JsonArray p = new JsonArray();
                    p.add(Indirizzo);
                    p.add(Esa);
                    Grezzo = new BigInteger(Esadecimale(nodo.chiama(NodoPubblicoDefi.Uso.STATO, "eth_getBalance", p).getAsString()), 16);
                } else {
                    Grezzo = Chiama(Fine, Trans_XLayer.SEL_BALANCE_OF + Trans_XLayer.Parola(Indirizzo), Esa, nodo);
                }
                String t = Quantita(Grezzo, Decimali);
                if (!t.equals(NON_DISPONIBILE)) CacheScrivi(ChiaveSaldo(Rete, Indirizzo, b, Fine), t);
                Lotto.put(b, t);
                Letti++;
                if (Lotto.size() >= BLOCCHI_PER_CONSEGNA) {
                    Parziale.accept(Lotto);
                    Lotto = new LinkedHashMap<>();
                }
            }
        } catch (Exception ex) {
            avvisi.add("Lettura delle giacenze su " + Rete + " interrotta dopo " + Letti + " blocchi su " + DaLeggere.size()
                    + ": " + ex.getMessage());
            for (int i = Letti; i < DaLeggere.size(); i++) Lotto.put(DaLeggere.get(i), NON_DISPONIBILE);
        }
        if (!Lotto.isEmpty()) Parziale.accept(Lotto);
    }

    /** Chiave in cache del saldo di una moneta ({@code NATIVA} o l'indirizzo del token) a fine blocco. */
    static String ChiaveSaldo(String Rete, String Indirizzo, long Blocco, String Moneta) {
        return "SALDO|" + Rete.toUpperCase() + "|" + Indirizzo.toLowerCase() + "|" + Blocco + "|" + Moneta;
    }

    /**
     * L'ultimo blocco con timestamp strettamente minore di {@code Secondo}; in cache per rete e istante quando
     * l'istante e' passato da piu' di un'ora (il blocco non puo' piu' cambiare).
     */
    static long UltimoBloccoPrima(String Rete, long Secondo, OKX_WalletCarta.Rpc rpc) throws Exception {
        String Chiave = "BLOCCO|" + Rete.toUpperCase() + "|" + Secondo;
        boolean Definitivo = Secondo < System.currentTimeMillis() / 1000 - 3600;
        if (Definitivo) {
            String c = CacheLeggi(Chiave);
            if (c != null) {
                try {
                    return Long.parseLong(c);
                } catch (NumberFormatException ignore) {
                    //cache rovinata: si ricalcola
                }
            }
        }
        long Primo = OKX_WalletCarta.BloccoAlTempo(Secondo, rpc);
        long Blocco = TempoBlocco(Primo, rpc) >= Secondo ? Primo - 1 : Primo;
        if (Blocco < 0) Blocco = 0;
        if (Definitivo) CacheScrivi(Chiave, Long.toString(Blocco));
        return Blocco;
    }

    private static long TempoBlocco(long numero, OKX_WalletCarta.Rpc rpc) throws Exception {
        JsonArray p = new JsonArray();
        p.add("0x" + Long.toHexString(numero));
        p.add(false);
        return Long.decode(rpc.chiama("eth_getBlockByNumber", p).getAsJsonObject().get("timestamp").getAsString());
    }

    private static boolean HaCodice(String Contratto, String Blocco, OKX_WalletCarta.Rpc rpc) throws Exception {
        JsonArray p = new JsonArray();
        p.add(Contratto);
        p.add(Blocco);
        JsonElement r = rpc.chiama("eth_getCode", p);
        return r != null && !r.isJsonNull() && r.getAsString().length() > 2;
    }

    /** Una chiamata di sola lettura; {@code null} se il contratto rifiuta o non restituisce una parola. */
    private static BigInteger Chiama(String Contratto, String Dati, String Blocco, NodoPubblicoDefi.Nodo n) throws Exception {
        JsonObject c = new JsonObject();
        c.addProperty("to", Contratto);
        c.addProperty("data", "0x" + Dati);
        JsonArray p = new JsonArray();
        p.add(c);
        p.add(Blocco);
        String r;
        try {
            r = n.chiama(NodoPubblicoDefi.Uso.STATO, "eth_call", p).getAsString();
        } catch (NodoPubblicoDefi.NodiPubblici.ErroreDefinitivo e) {
            return null;
        }
        if (r == null || r.length() < 66) return null;
        return new BigInteger(r.substring(2, 66), 16);
    }

    private static String Esadecimale(String v) {
        String s = v.startsWith("0x") || v.startsWith("0X") ? v.substring(2) : v;
        return s.isEmpty() ? "0" : s;
    }

    /**
     * Il valore {@code uint256} di ogni chiamata di {@code aggregate3}, {@code null} se la chiamata e' fallita o non
     * ha restituito una parola intera. A differenza di {@link Trans_XLayer#DecodificaAggregate3}, che per la scansione
     * per stati tratta il fallimento come zero, qui uno zero finto sembrerebbe un saldo vero.
     */
    static List<BigInteger> DecodificaAggregate3(String risultato) {
        List<BigInteger> valori = new ArrayList<>();
        if (risultato == null || risultato.length() <= 2) return valori;
        String h = risultato.substring(2);
        int base = Parola(h, 0).intValueExact() * 2;
        int n = Parola(h, base).intValueExact();
        int inizio = base + 64;
        for (int i = 0; i < n; i++) {
            int el = inizio + Parola(h, inizio + i * 64).intValueExact() * 2;
            boolean riuscita = Parola(h, el).signum() != 0;
            int dati = el + Parola(h, el + 64).intValueExact() * 2;
            int lunghezza = Parola(h, dati).intValueExact();
            valori.add(riuscita && lunghezza >= 32 ? Parola(h, dati + 64) : null);
        }
        return valori;
    }

    private static BigInteger Parola(String h, int pos) {
        return new BigInteger(h.substring(pos, pos + 64), 16);
    }

    /** Il saldo in unita' della moneta; {@link #NON_DISPONIBILE} se manca il saldo o i decimali non sono plausibili. */
    static String Quantita(BigInteger Grezzo, BigInteger Decimali) {
        if (Grezzo == null || Decimali == null || Decimali.signum() < 0 || Decimali.compareTo(BigInteger.valueOf(77)) > 0) {
            return NON_DISPONIBILE;
        }
        BigDecimal q = new BigDecimal(Grezzo).movePointLeft(Decimali.intValue());
        return q.signum() == 0 ? "0" : q.stripTrailingZeros().toPlainString();
    }

    /**
     * @return {@code TRUE} se la quantita' letta coincide con quella dell'archivio, {@code FALSE} se differisce,
     *         {@code null} se una delle due non e' un numero (non letta, in lettura, riga esclusa). Confronto
     *         numerico: le quantita' dell'archivio possono essere in notazione scientifica.
     */
    public static Boolean Coincide(Object QtaArchivio, Object QtaBlockchain) {
        if (QtaArchivio == null || QtaBlockchain == null) return null;
        try {
            BigDecimal a = new BigDecimal(QtaArchivio.toString().trim());
            BigDecimal b = new BigDecimal(QtaBlockchain.toString().trim());
            return a.compareTo(b) == 0;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String CacheLeggi(String Chiave) {
        if (DatabaseH2.connectionPersonale == null) return null;
        try {
            return DatabaseH2.GiacenzeWalletMonetaBlockchain_Leggi(Chiave);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static void CacheScrivi(String Chiave, String Valore) {
        if (DatabaseH2.connectionPersonale == null) return;
        try {
            DatabaseH2.GiacenzeWalletMonetaBlockchain_Scrivi(Chiave, Valore);
        } catch (RuntimeException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }
}
