package com.giacenzecrypto.giacenze_crypto;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Strumento di sviluppo, non fa parte dell'applicazione: scarica da CoinGecko i loghi degli exchange e
 * delle blockchain e li salva in {@code config/loghi/}, da dove vengono poi distribuiti agli utenti
 * insieme alle altre configurazioni (vedi {@link Funzioni#AggiornamentoConfigDaRepository}).
 * <p>Il download si fa qui, una volta, e il risultato si committa: gli utenti non devono spendere la
 * quota CoinGecko dell'applicazione, che serve già per i prezzi.
 * <p>Uso: {@code java -cp target/classes com.giacenzecrypto.giacenze_crypto.GeneraLoghi [cartella]}
 * (occorrono anche okhttp e org.json nel classpath, quindi in pratica il jar completo).
 * <p>Il nome del file è lo slug della voce come compare nei menù di
 * {@link Importazioni_Gestione} — minuscolo, i caratteri non alfanumerici sostituiti da {@code -} —
 * così chi disegna la lista risale al logo dalla sola etichetta.
 *
 * @author luca.passelli
 */
public class GeneraLoghi {

    /** Lato massimo del PNG salvato: abbondante per gli schermi ad alta densità, file comunque piccoli */
    private static final int LATO_MAX = 128;

    private static final OkHttpClient CLIENT = new OkHttpClient();

    /**
     * Nomi che nelle liste di {@link Importazioni_Gestione} sono scritti diversamente da CoinGecko,
     * a volte per un refuso mai corretto. Solo corrispondenze certe: dove il dubbio resta, meglio nessun
     * logo che il marchio di un altro exchange.
     */
    private static final Map<String, String> ALIAS_EXCHANGE = new LinkedHashMap<>();

    static {
        ALIAS_EXCHANGE.put("Gate.lo", "Gate.io");                 //refuso: l al posto di i
        ALIAS_EXCHANGE.put("OKColn", "OKCoin");                   //refuso: ln al posto di in
        ALIAS_EXCHANGE.put("Bithumb Glo.", "Bithumb Global");
        ALIAS_EXCHANGE.put("Cake Defl", "Cake DeFi");             //refuso: fl al posto di Fi
        ALIAS_EXCHANGE.put("Relal", "Relai");                     //refuso: l finale al posto di i
        ALIAS_EXCHANGE.put("HRBTC", "HitBTC");
        ALIAS_EXCHANGE.put("CEX", "CEX.IO");
        ALIAS_EXCHANGE.put("Coinbase", "Coinbase Exchange");
        ALIAS_EXCHANGE.put("Coinbase Pro", "Coinbase Exchange");  //Coinbase Pro è confluito in Coinbase
        ALIAS_EXCHANGE.put("Crypto.com", "Crypto.com Exchange");
        ALIAS_EXCHANGE.put("Bitpanda Pro", "One Trading");        //Bitpanda Pro è diventato One Trading
        ALIAS_EXCHANGE.put("Digital Surge", "DigitalSurge");
        ALIAS_EXCHANGE.put("Yield App", "YIELD App");
    }

    /**
     * Blockchain → identificativo CoinGecko. Le catene che ospitano token sono in
     * {@code /asset_platforms}, quelle senza smart contract vanno prese dal logo della loro moneta in
     * {@code /coins/markets}. La mappa è esplicita di proposito: indovinare per somiglianza di nome
     * qui produrrebbe accostamenti sbagliati.
     */
    private static final Map<String, String> PIATTAFORMA_CHAIN = new LinkedHashMap<>();
    private static final Map<String, String> MONETA_CHAIN = new LinkedHashMap<>();

    static {
        PIATTAFORMA_CHAIN.put("Arbitrum (ARB)", "arbitrum-one");
        PIATTAFORMA_CHAIN.put("Avalanche (AVAX)", "avalanche");
        PIATTAFORMA_CHAIN.put("Base (BASE)", "base");
        PIATTAFORMA_CHAIN.put("Binance Chain (BNB)", "binancecoin");
        PIATTAFORMA_CHAIN.put("Binance Smart Chain (BSC)", "binance-smart-chain");
        PIATTAFORMA_CHAIN.put("Cronos Chain (CRO)", "cronos");
        PIATTAFORMA_CHAIN.put("Eos (EOS)", "eos");
        PIATTAFORMA_CHAIN.put("Ethereum (ETH)", "ethereum");
        PIATTAFORMA_CHAIN.put("Fantom (FTM)", "fantom");
        PIATTAFORMA_CHAIN.put("Gnosis Chain (GNOSIS)", "xdai");
        PIATTAFORMA_CHAIN.put("Monad (MONAD)", "monad");
        PIATTAFORMA_CHAIN.put("Polkadot (DOT)", "polkadot");
        PIATTAFORMA_CHAIN.put("Polygon (POL)", "polygon-pos");
        PIATTAFORMA_CHAIN.put("Solana (SOL)", "solana");
        PIATTAFORMA_CHAIN.put("Stellar (XLM)", "stellar");
        PIATTAFORMA_CHAIN.put("Terra Classic (LUNA)", "terra");
        PIATTAFORMA_CHAIN.put("Tron (TRX)", "tron");

        MONETA_CHAIN.put("Bitcoin (BTC)", "bitcoin");
        MONETA_CHAIN.put("Cardano (ADA)", "cardano");
        MONETA_CHAIN.put("Dash (DASH)", "dash");
        MONETA_CHAIN.put("Dogecoin (DOGE)", "dogecoin");
        MONETA_CHAIN.put("Litecoin (LTC)", "litecoin");
        MONETA_CHAIN.put("Ripple (XRP)", "ripple");
        MONETA_CHAIN.put("Zcash (ZEC)", "zcash");
    }

    /**
     * Dominio ufficiale delle voci che CoinGecko non conosce: il suo endpoint {@code /exchanges} elenca
     * solo gli exchange di cui traccia i mercati, quindi ne mancano i wallet, i servizi di
     * rendicontazione fiscale e parecchi exchange chiusi o non più tracciati. Per questi il logo si
     * prende dall'icona del sito.
     * <p>Sono elencati solo i domini di cui si è certi: associare per somiglianza il dominio sbagliato
     * mostrerebbe il marchio di un'altra azienda accanto al nome, che è peggio di nessun logo.
     */
    private static final Map<String, String> DOMINI = new LinkedHashMap<>();

    static {
        //Acx non è elencato: acx.io oggi risponde con una pagina segnaposto che non ha più
        //relazione con l'exchange australiano chiuso, e ne prenderemmo il logo sbagliato
        DOMINI.put("Abra", "abra.com");
        DOMINI.put("AscendEX", "ascendex.com");
        DOMINI.put("BSDEX", "bsdex.de");
        DOMINI.put("Bison", "bisonapp.com");
        DOMINI.put("Bitcoin Suisse", "bitcoinsuisse.com");
        DOMINI.put("Bitcoin.de", "bitcoin.de");
        DOMINI.put("Bithumb Glo.", "bithumb.com");
        DOMINI.put("Bitpanda", "bitpanda.com");
        DOMINI.put("Bitstamp", "bitstamp.net");
        DOMINI.put("Bittrex", "bittrex.com");
        DOMINI.put("BlockFi", "blockfi.com");
        DOMINI.put("Cake Defl", "cakedefi.com");
        DOMINI.put("Celsius", "celsius.network");
        DOMINI.put("Changelly", "changelly.com");
        DOMINI.put("Circle", "circle.com");
        DOMINI.put("Coinmate", "coinmate.io");
        DOMINI.put("Coinmerce", "coinmerce.io");
        DOMINI.put("Coinmetro", "coinmetro.com");
        DOMINI.put("Crex24", "crex24.com");
        DOMINI.put("Criptan", "criptan.es");
        DOMINI.put("DFX.swiss", "dfx.swiss");
        DOMINI.put("Deribit", "deribit.com");
        DOMINI.put("Digital Surge", "digitalsurge.com.au");
        DOMINI.put("Gate.lo", "gate.io");
        DOMINI.put("Haru", "haruinvest.com");
        DOMINI.put("Hodinaut", "hodlnaut.com");
        DOMINI.put("Hotbit", "hotbit.io");
        DOMINI.put("Iconomi", "iconomi.com");
        //Idex non è elencato: idex.io rimanda a kuma.bid, il marchio in cui IDEX è confluito,
        //e accanto alla voce "Idex" comparirebbe il logo di KUMA
        DOMINI.put("Localbitcoins", "localbitcoins.com");
        DOMINI.put("Luxor", "luxor.tech");
        DOMINI.put("Mercatox", "mercatox.com");
        DOMINI.put("NFTBank", "nftbank.ai");
        DOMINI.put("Nexo", "nexo.com");
        DOMINI.put("Northcrypto", "northcrypto.com");
        DOMINI.put("OKColn", "okcoin.com");
        DOMINI.put("Pocket Bitcoin", "pocketbitcoin.com");
        DOMINI.put("Relal", "relai.app");
        DOMINI.put("Revolut", "revolut.com");
        DOMINI.put("STEX", "stex.com");
        DOMINI.put("SwissBorg", "swissborg.com");
        DOMINI.put("Swyftx", "swyftx.com");
        DOMINI.put("Tradeogre", "tradeogre.com");
        DOMINI.put("Uphold", "uphold.com");
        DOMINI.put("Voyager", "investvoyager.com");
        DOMINI.put("Yield App", "yield.app");
        DOMINI.put("Zerion", "zerion.io");
        DOMINI.put("HRBTC", "hitbtc.com");

        //Wallet: nessuno è su CoinGecko
        DOMINI.put("BitBox", "bitbox.swiss");
        DOMINI.put("Citcoin Core Client", "bitcoincore.org");
        DOMINI.put("Blochchain.com", "blockchain.com");
        DOMINI.put("Electrum", "electrum.org");
        DOMINI.put("Exodus", "exodus.com");
        DOMINI.put("Gate Hub", "gatehub.net");
        DOMINI.put("Ledger Live", "ledger.com");
        DOMINI.put("Mycellum", "mycelium.com");
        DOMINI.put("Trezor", "trezor.io");
    }

    /**
     * Servizi di rendicontazione che compaiono fra i tipi di file importabili ma non fra gli exchange.
     */
    private static final Map<String, String> DOMINI_EXTRA = new LinkedHashMap<>();

    static {
        DOMINI_EXTRA.put("CoinTracking", "cointracking.info");
        DOMINI_EXTRA.put("Tatax", "ta.tax"); //il dominio è ta.tax, non tatax.it
    }

    /**
     * Immagini indicate a mano perché i servizi di icone danno un risultato inservibile: o troppo
     * piccolo, o bianco su trasparente — invisibile sul tema chiaro — o un cerchio pieno senza il
     * marchio. Hanno la precedenza su ogni altra fonte.
     */
    private static final Map<String, String> URL_SPECIFICI = new LinkedHashMap<>();

    static {
        //il favicon 196px per tema chiaro: quello predefinito è bianco e sparirebbe sullo sfondo chiaro
        URL_SPECIFICI.put("BitBox",
                "https://bitbox.swiss/assets/favicons/favicon-196-be2bf1dec26f852b949102c2bac31743.png");
        //apple-touch-icon: il favicon che restituiscono i servizi è un cerchio pieno senza marchio
        URL_SPECIFICI.put("Tatax", "https://ta.tax/icon.png");
        //apple-touch-icon: il favicon di Trezor è solo 32x32
        URL_SPECIFICI.put("Trezor", "https://trezor.io/favicon/apple-touch-icon.png");
    }

    /**
     * Impronta dell'icona generica che il servizio di Google restituisce, con codice 200, per i domini
     * che non conosce. Serve a scartarla: un mappamondo grigio accanto al nome sembrerebbe un logo
     * sbagliato invece che un logo mancante. Viene calcolata a ogni esecuzione interrogando un dominio
     * inesistente, così resta valida anche se Google cambia l'immagine.
     */
    private static String improntaIconaGenerica = null;

    /**
     * @param args {@code [0]} cartella di destinazione, per difetto {@code config/loghi}
     * @throws Exception se la lettura delle API o la scrittura dei file fallisce in modo irrecuperabile
     */
    public static void main(String[] args) throws Exception {
        Path cartella = Paths.get(args.length > 0 ? args[0] : "config/loghi");
        Files.createDirectories(cartella);

        List<String> scaricati = new ArrayList<>();
        List<String> mancanti = new ArrayList<>();

        CalcolaImprontaIconaGenerica();
        ScaricaExchange(cartella, scaricati, mancanti);
        ScaricaChain(cartella, scaricati, mancanti);
        ScaricaDiretti(cartella, scaricati, mancanti);

        System.out.println();
        System.out.println("=== " + scaricati.size() + " loghi salvati in " + cartella.toAbsolutePath() + " ===");
        if (!mancanti.isEmpty()) {
            System.out.println();
            System.out.println("=== " + mancanti.size() + " voci senza logo, da procurare a mano ===");
            for (String m : mancanti) {
                System.out.println("   " + m);
            }
        }
    }

    /** Scarica i loghi degli exchange e dei wallet elencati in {@link Importazioni_Gestione}. */
    private static void ScaricaExchange(Path cartella, List<String> scaricati, List<String> mancanti) throws Exception {
        //Indice nome normalizzato → url immagine, costruito una volta sola sulle pagine dell'elenco
        Map<String, String> perNome = new TreeMap<>();
        for (int pagina = 1; pagina <= 4; pagina++) {
            JSONArray elenco = new JSONArray(Get("https://api.coingecko.com/api/v3/exchanges?per_page=250&page=" + pagina));
            for (int i = 0; i < elenco.length(); i++) {
                JSONObject ex = elenco.getJSONObject(i);
                String nome = ex.optString("name", "");
                String img = ex.optString("image", "");
                if (!nome.isBlank() && !img.isBlank()) {
                    perNome.putIfAbsent(Normalizza(nome), img);
                }
            }
            Thread.sleep(1500); //la quota gratuita di CoinGecko è stretta, meglio non tirarla
        }
        System.out.println("Exchange noti a CoinGecko: " + perNome.size());
        System.out.println();

        List<String> voci = new ArrayList<>();
        for (String v : Importazioni_Gestione.Exchanges) {
            voci.add(v);
        }
        for (String v : Importazioni_Gestione.Wallets) {
            voci.add(v);
        }

        for (String voce : voci) {
            if (voce.trim().startsWith("-") || voce.trim().startsWith("*")) {
                continue; //separatori e segnaposto non hanno logo
            }
            String cercato = ALIAS_EXCHANGE.getOrDefault(voce, voce);
            String url = perNome.get(Normalizza(cercato));

            if (url != null && Salva(url, cartella.resolve(Slug(voce) + ".png"))) {
                scaricati.add(voce);
                System.out.println("   " + voce + " -> " + Slug(voce) + ".png   [CoinGecko]");
                continue;
            }

            //CoinGecko non lo conosce (o l'immagine non è leggibile): ripiego sull'icona del sito
            if (SalvaDaDominio(voce, DOMINI.get(voce), cartella)) {
                scaricati.add(voce);
            } else {
                mancanti.add(voce + (DOMINI.containsKey(voce) ? "  (dominio " + DOMINI.get(voce) + ")" : "  (dominio ignoto)"));
            }
        }
    }

    /**
     * Prova a salvare il logo di una voce partendo dall'icona del suo sito, scartando l'icona generica
     * che i servizi restituiscono per i domini sconosciuti.
     * @param voce etichetta della voce, usata per il nome del file
     * @param dominio dominio ufficiale, può essere {@code null} se non se ne conosce uno affidabile
     * @param cartella cartella di destinazione
     * @return {@code true} se un logo valido è stato salvato
     */
    private static boolean SalvaDaDominio(String voce, String dominio, Path cartella) {
        Path destinazione = cartella.resolve(Slug(voce) + ".png");

        String specifico = URL_SPECIFICI.get(voce);
        if (specifico != null && Salva(specifico, destinazione)) {
            System.out.println("   " + voce + " -> " + Slug(voce) + ".png   [indicato a mano]");
            return true;
        }

        if (dominio == null || dominio.isBlank()) {
            return false;
        }

        //DuckDuckGo risponde 404 sui domini che non conosce, quindi non rischia icone generiche
        if (Salva("https://icons.duckduckgo.com/ip3/" + dominio + ".ico", destinazione)) {
            System.out.println("   " + voce + " -> " + Slug(voce) + ".png   [" + dominio + "]");
            return true;
        }
        if (Salva("https://www.google.com/s2/favicons?sz=128&domain=" + dominio, destinazione)) {
            //Google risponde 200 con un mappamondo anche per i domini che non conosce: va riconosciuto
            if (improntaIconaGenerica != null && improntaIconaGenerica.equals(Impronta(destinazione))) {
                try {
                    Files.deleteIfExists(destinazione);
                } catch (Exception ignored) {
                    //il file resta, ma la voce è comunque segnalata come mancante
                }
                return false;
            }
            System.out.println("   " + voce + " -> " + Slug(voce) + ".png   [" + dominio + "]");
            return true;
        }
        return false;
    }

    /** Interroga il servizio di Google su un dominio inesistente per riconoscerne l'icona di ripiego. */
    private static void CalcolaImprontaIconaGenerica() {
        try {
            Path tmp = Files.createTempFile("iconagenerica", ".png");
            if (Salva("https://www.google.com/s2/favicons?sz=128&domain=dominio-che-non-esiste-9f3a2b.invalid", tmp)) {
                improntaIconaGenerica = Impronta(tmp);
                System.out.println("Icona generica riconosciuta, verrà scartata (" + improntaIconaGenerica + ")");
            }
            Files.deleteIfExists(tmp);
        } catch (Exception ex) {
            System.out.println("Impossibile riconoscere l'icona generica: " + ex.getMessage());
        }
    }

    /** @return l'impronta SHA-1 del contenuto del file, o {@code null} se illeggibile */
    private static String Impronta(Path file) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(file));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    /** Scarica i loghi delle blockchain, dalle piattaforme quando esistono e dalle monete altrimenti. */
    private static void ScaricaChain(Path cartella, List<String> scaricati, List<String> mancanti) throws Exception {
        System.out.println();
        Map<String, String> perPiattaforma = new TreeMap<>();
        JSONArray piattaforme = new JSONArray(Get("https://api.coingecko.com/api/v3/asset_platforms"));
        for (int i = 0; i < piattaforme.length(); i++) {
            JSONObject p = piattaforme.getJSONObject(i);
            JSONObject img = p.optJSONObject("image");
            if (img != null && !img.optString("large", "").isBlank()) {
                perPiattaforma.put(p.optString("id", ""), img.getString("large"));
            }
        }
        Thread.sleep(1500);

        Map<String, String> perMoneta = new TreeMap<>();
        List<String> ids = new ArrayList<>(MONETA_CHAIN.values());
        //binancecoin sta fra le monete anche se l'ho classificato come piattaforma: la Binance Chain
        //originale non è un asset platform di CoinGecko, il suo logo è quello della moneta BNB
        ids.add("binancecoin");
        JSONArray monete = new JSONArray(Get(
                "https://api.coingecko.com/api/v3/coins/markets?vs_currency=eur&ids=" + String.join(",", ids)));
        for (int i = 0; i < monete.length(); i++) {
            JSONObject c = monete.getJSONObject(i);
            perMoneta.put(c.optString("id", ""), c.optString("image", ""));
        }

        for (String voce : Importazioni_Gestione.BlockChain) {
            if (voce.trim().startsWith("-") || voce.trim().startsWith("*")) {
                continue;
            }
            String url = null;
            String idP = PIATTAFORMA_CHAIN.get(voce);
            if (idP != null) {
                url = perPiattaforma.get(idP);
                if (url == null) {
                    url = perMoneta.get(idP); //ripiego sulla moneta, es. Binance Chain
                }
            }
            if (url == null) {
                String idM = MONETA_CHAIN.get(voce);
                if (idM != null) {
                    url = perMoneta.get(idM);
                }
            }
            if (url == null || url.isBlank()) {
                mancanti.add(voce);
                continue;
            }
            if (Salva(url, cartella.resolve(Slug(voce) + ".png"))) {
                scaricati.add(voce);
                System.out.println("   " + voce + " -> " + Slug(voce) + ".png");
            } else {
                mancanti.add(voce);
            }
        }
    }

    /** Scarica i loghi dei servizi di rendicontazione, che non compaiono in nessuna delle liste. */
    private static void ScaricaDiretti(Path cartella, List<String> scaricati, List<String> mancanti) {
        System.out.println();
        for (Map.Entry<String, String> e : DOMINI_EXTRA.entrySet()) {
            if (SalvaDaDominio(e.getKey(), e.getValue(), cartella)) {
                scaricati.add(e.getKey());
            } else {
                mancanti.add(e.getKey() + "  (dominio " + e.getValue() + ")");
            }
        }
    }

    /**
     * Scarica un'immagine, la riduce entro {@link #LATO_MAX} mantenendo le proporzioni e la salva in PNG.
     * @return {@code true} se il file è stato scritto
     */
    private static boolean Salva(String url, Path destinazione) {
        try {
            Request req = new Request.Builder().url(url).header("User-Agent", "GiacenzeCrypto").build();
            byte[] dati;
            try (Response resp = CLIENT.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    System.out.println("   [!] " + destinazione.getFileName() + ": codice " + resp.code());
                    return false;
                }
                dati = resp.body().bytes();
            }

            BufferedImage originale = ImageIO.read(new ByteArrayInputStream(dati));
            if (originale == null) {
                System.out.println("   [!] " + destinazione.getFileName() + ": formato immagine non leggibile");
                return false;
            }

            int l = Math.max(originale.getWidth(), originale.getHeight());
            double fattore = l > LATO_MAX ? (double) LATO_MAX / l : 1.0;
            int w = (int) Math.round(originale.getWidth() * fattore);
            int h = (int) Math.round(originale.getHeight() * fattore);

            BufferedImage ridotta = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = ridotta.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(originale, 0, 0, w, h, null);
            g.dispose();

            ImageIO.write(ridotta, "png", destinazione.toFile());
            return true;

        } catch (Exception ex) {
            System.out.println("   [!] " + destinazione.getFileName() + ": " + ex.getMessage());
            return false;
        }
    }

    /**
     * @return il corpo della risposta come stringa, riprovando con attesa crescente sul codice 429:
     *         la quota gratuita di CoinGecko è di poche richieste al minuto e viene superata facilmente
     */
    private static String Get(String url) throws Exception {
        int attesa = 20;
        for (int tentativo = 1; tentativo <= 6; tentativo++) {
            Request req = new Request.Builder().url(url)
                    .header("User-Agent", "GiacenzeCrypto")
                    .header("Accept", "application/json")
                    .build();
            try (Response resp = CLIENT.newCall(req).execute()) {
                if (resp.isSuccessful()) {
                    return resp.body().string();
                }
                if (resp.code() != 429) {
                    throw new IllegalStateException("Risposta CoinGecko " + resp.code() + " per " + url);
                }
            }
            System.out.println("   [quota CoinGecko superata, riprovo fra " + attesa + "s]");
            Thread.sleep(attesa * 1000L);
            attesa = Math.min(attesa * 2, 120);
        }
        throw new IllegalStateException("Quota CoinGecko sempre superata per " + url);
    }

    /** @return la forma usata per confrontare i nomi: minuscolo, senza nulla che non sia alfanumerico */
    static String Normalizza(String nome) {
        return nome == null ? "" : nome.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /**
     * @param voce l'etichetta così come compare nei menù
     * @return il nome del file del logo, senza estensione. La regola è quella di
     *         {@link LoghiImport#Slug(String)}, condivisa di proposito: chi scrive i file e chi li
     *         cerca devono usare la stessa
     */
    static String Slug(String voce) {
        return LoghiImport.Slug(voce);
    }
}
