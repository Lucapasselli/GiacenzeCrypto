package com.giacenzecrypto.giacenze_crypto;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Token truffa marcati SCAM già all'importazione dei wallet EVM, solo quando è <b>certo</b>. Analisi e misure in
 * {@code nocommit/Documentazione/Analisi_Token_Truffa_Import.md}.
 * <p>
 * Valgono insieme: transazione non firmata dal wallet ({@link TransazioneDefi#FirmataDalWallet}, conosciuta solo dalla
 * lettura EVM generica), nessun prezzo di mercato ({@code v[32] = "NO"}), quantità diversa da zero, deposito o prelievo
 * di un token con indirizzo di contratto. Poi una di tre prove:
 * <ul>
 * <li>{@link Regola#R0}: esce dal wallet un token che lì non è mai entrato. Un token vero non può farlo: è l'address
 * poisoning con finti trasferimenti, anche con simboli identici a quelli veri ("ETH"). Solo con la storia completa del
 * wallet ({@link TransazioneDefi#StoriaCompleta}).</li>
 * <li>{@link Regola#R1}: il simbolo è un sosia di un ticker noto (lo scheletro UTS #39 coincide) e contiene lettere di
 * un altro alfabeto o caratteri invisibili. Un simbolo che usa di suo un carattere particolare ma non una lettera
 * straniera, come "USD" seguito dal segno del tugrik di Tether, non scatta.</li>
 * <li>{@link Regola#R2}: simbolo o nome con un link esplicito ({@code https://}, {@code t.me/}...).</li>
 * </ul>
 * Non si marca mai un token già rinominato dall'utente (potrebbe aver tolto lui il {@code " **"}), nell'elenco CoinGecko,
 * con un alias di prezzo, E-Money, già prezzato in archivio o fra i simboli legittimi della configurazione. La marcatura
 * è quella di "Identifica come SCAM": rinomina per indirizzo e rete in {@code RINOMINATOKEN}, reversibile dalla stessa voce.
 */
final class TokenTruffa {

    private TokenTruffa() {}

    /** Nome del file in {@code config/varie/}, senza estensione. */
    static final String NOME = "TokenTruffa";

    /** Risorsa nel jar: sottoinsieme di {@code confusables.txt} con i soli prototipi ASCII. */
    static final String RISORSA_CONFONDIBILI = "/Unicode/confondibili-ascii.txt";

    enum Regola {
        R0("prelievo di un token mai entrato nel wallet"),
        R1("simbolo sosia di un token noto"),
        R2("link nel nome del token");

        final String descrizione;

        Regola(String descrizione) {
            this.descrizione = descrizione;
        }
    }

    /**
     * Il contenuto di {@code TokenTruffa.json}.
     *
     * @param scheletriTicker scheletro di ogni ticker imitato → il ticker
     */
    record Config(Map<String, String> scheletriTicker, Set<String> simboliLegittimi, Map<Integer, String> confondibiliAggiuntivi,
                  List<String> marcatoriLink) {}

    /** Una riga dell'importazione con quello che la transazione sa della firma del wallet. */
    record Riga(String[] v, boolean firmaVerificabile, boolean firmataDalWallet, boolean storiaCompleta) {}

    /** Un token da marcare. {@code chiave} è {@code address_rete} come in {@code RINOMINATOKEN}. */
    record Marcatura(String chiave, String address, String rete, String simbolo, Regola regola, String dettaglio) {}

    /** Ciò che toglie un token dalle regole a prescindere dalla riga: rinominato, CoinGecko, alias, E-Money. */
    interface Esclusioni {
        boolean Esclude(String address, String rete, String simbolo);
    }

    /** @return la configurazione (disco, poi copia nel jar), oppure {@code null} se non è disponibile */
    static Config Configurazione() {
        return MappeCausali.CaricaConRipiego(NOME, MappeCausali.Cartella.VARIE, TokenTruffa::Interpreta);
    }

    static Config Interpreta(String contenuto, String nome) {
        if (contenuto == null || contenuto.isBlank()) return null;
        try {
            JSONObject r = new JSONObject(contenuto);
            Map<Integer, String> aggiuntivi = new HashMap<>();
            JSONObject a = r.optJSONObject("confondibiliAggiuntivi");
            if (a != null) {
                for (String k : a.keySet()) {
                    if (k.codePointCount(0, k.length()) == 1) aggiuntivi.put(k.codePointAt(0), a.getString(k));
                }
            }
            Map<String, String> scheletri = new LinkedHashMap<>();
            JSONArray t = r.getJSONArray("tickerImitati");
            for (int i = 0; i < t.length(); i++) {
                String s = t.getString(i).trim();
                if (!s.isEmpty()) scheletri.putIfAbsent(Scheletro(s, aggiuntivi), s);
            }
            Set<String> legittimi = new HashSet<>();
            JSONArray l = r.optJSONArray("simboliLegittimi");
            if (l != null) for (int i = 0; i < l.length(); i++) legittimi.add(l.getString(i).trim());
            List<String> marcatori = new ArrayList<>();
            JSONArray m = r.optJSONArray("marcatoriLink");
            if (m != null) for (int i = 0; i < m.length(); i++) marcatori.add(m.getString(i).trim().toLowerCase(Locale.ROOT));
            if (scheletri.isEmpty()) return null;
            return new Config(Collections.unmodifiableMap(scheletri), Collections.unmodifiableSet(legittimi),
                    Collections.unmodifiableMap(aggiuntivi), Collections.unmodifiableList(marcatori));
        } catch (Exception e) {
            LoggerGC.ScriviErrore("TokenTruffa: " + nome + ".json non interpretabile : " + e);
            return null;
        }
    }

    private static volatile Map<Integer, String> confondibili;

    /** La tabella dei confondibili verso i caratteri ASCII, letta una volta dal jar. */
    static Map<Integer, String> Confondibili() {
        Map<Integer, String> c = confondibili;
        if (c != null) return c;
        Map<Integer, String> m = new HashMap<>();
        try (InputStream in = TokenTruffa.class.getResourceAsStream(RISORSA_CONFONDIBILI)) {
            if (in == null) {
                LoggerGC.ScriviErrore("TokenTruffa: risorsa " + RISORSA_CONFONDIBILI + " mancante");
            } else {
                BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String riga; (riga = br.readLine()) != null; ) {
                    if (riga.isBlank() || riga.startsWith("#")) continue;
                    String[] p = riga.split(";");
                    StringBuilder prototipo = new StringBuilder();
                    for (String h : p[1].trim().split(" ")) prototipo.appendCodePoint(Integer.parseInt(h, 16));
                    m.put(Integer.parseInt(p[0].trim(), 16), prototipo.toString());
                }
            }
        } catch (Exception e) {
            LoggerGC.ScriviErrore("TokenTruffa: confondibili non letti : " + e);
        }
        confondibili = Collections.unmodifiableMap(m);
        return confondibili;
    }

    /** Caratteri che non si vedono e non sono di categoria FORMAT (riempitivi hangul, vocali inerenti khmer...). */
    private static boolean Riempitivo(int c) {
        return c == 0x17B4 || c == 0x17B5 || c == 0x3164 || c == 0x115F || c == 0x1160 || c == 0xFFA0 || c == 0x2800
                || c == 0x180E || c == 0x034F;
    }

    private static boolean Invisibile(int c) {
        int t = Character.getType(c);
        return t == Character.FORMAT || t == Character.CONTROL || Riempitivo(c);
    }

    /**
     * Lo scheletro di un simbolo, come in UTS #39: forma NFD, via invisibili e segni diacritici, ogni carattere
     * sostituito dal suo prototipo (prima {@code aggiuntivi}, poi la tabella Unicode), di nuovo NFD; poi solo lettere e
     * cifre, in maiuscolo. Due simboli con lo stesso scheletro si confondono a vista.
     */
    static String Scheletro(String s, Map<Integer, String> aggiuntivi) {
        if (s == null) return "";
        Map<Integer, String> tabella = Confondibili();
        StringBuilder sb = new StringBuilder();
        Normalizer.normalize(s, Normalizer.Form.NFD).codePoints().forEach(c -> {
            int t = Character.getType(c);
            if (Invisibile(c) || t == Character.NON_SPACING_MARK || t == Character.ENCLOSING_MARK || Character.isWhitespace(c)
                    || Character.isSpaceChar(c)) return;
            String p = aggiuntivi.get(c);
            if (p == null) p = tabella.get(c);
            if (p == null) sb.appendCodePoint(c);
            else sb.append(p);
        });
        StringBuilder out = new StringBuilder();
        Normalizer.normalize(sb, Normalizer.Form.NFD).codePoints().forEach(c -> {
            if (Character.isLetterOrDigit(c)) out.appendCodePoint(c);
        });
        return out.toString().toUpperCase(Locale.ROOT);
    }

    /** Il simbolo ha lettere di un alfabeto diverso dal latino, o caratteri invisibili. */
    static boolean AltroAlfabetoOInvisibili(String s) {
        if (s == null) return false;
        return s.codePoints().anyMatch(c -> {
            if (Invisibile(c)) return true;
            if (!Character.isLetter(c)) return false;
            Character.UnicodeScript sc = Character.UnicodeScript.of(c);
            return sc != Character.UnicodeScript.LATIN && sc != Character.UnicodeScript.COMMON
                    && sc != Character.UnicodeScript.INHERITED;
        });
    }

    /** @return il ticker imitato da {@code simbolo} (regola R1), oppure {@code null} */
    static String TickerImitato(String simbolo, Config cfg) {
        if (simbolo == null || simbolo.isBlank() || IsAscii(simbolo) || !AltroAlfabetoOInvisibili(simbolo)) return null;
        if (cfg.simboliLegittimi().contains(simbolo.trim())) return null;
        return cfg.scheletriTicker().get(Scheletro(simbolo, cfg.confondibiliAggiuntivi()));
    }

    /** @return il marcatore di link trovato in uno dei testi (regola R2), oppure {@code null} */
    static String Link(Config cfg, String... testi) {
        for (String t : testi) {
            if (t == null) continue;
            String b = t.toLowerCase(Locale.ROOT);
            for (String m : cfg.marcatoriLink()) if (!m.isEmpty() && b.contains(m)) return m;
        }
        return null;
    }

    private static boolean IsAscii(String s) {
        return s.chars().allMatch(c -> c < 128);
    }

    private static boolean IsContratto(String a) {
        return a != null && a.matches("(?i)0x[0-9a-f]{40}");
    }

    private static BigDecimal Numero(String s) {
        try {
            return s == null || s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static String Rete(String[] v) {
        return v[34] != null && !v[34].isBlank() ? v[34] : "";
    }

    private static String Tipo(String[] v) {
        return v[0] == null ? "" : v[0].substring(v[0].lastIndexOf('_') + 1);
    }

    /**
     * Le marcature delle righe nuove, una per token: la prima regola che lo prende, nell'ordine delle righe.
     *
     * @param nuove le righe dell'importazione con le informazioni sulla firma
     * @param archivio i movimenti già in archivio (per R0, i token già entrati nel wallet, e per i token già prezzati)
     */
    static List<Marcatura> Valuta(List<Riga> nuove, Collection<String[]> archivio, Config cfg, Esclusioni esclusioni) {
        if (cfg == null || nuove == null || nuove.isEmpty()) return List.of();
        //Token con un prezzo in archivio o fra le righe nuove: un token prezzato non è certo una truffa
        Set<String> prezzati = new HashSet<>();
        //Token entrati in ogni wallet (v[3] è "wallet (rete)"): in un wallet da cui esce, un token vero è entrato prima
        Set<String> entrati = new HashSet<>();
        List<String[]> tutte = new ArrayList<>(archivio);
        for (Riga r : nuove) tutte.add(r.v());
        for (String[] v : tutte) {
            if (v == null || v.length < 35) continue;
            if ("SI".equals(v[32]) && Numero(v[15]).signum() > 0) {
                for (String a : new String[]{v[26], v[28]}) {
                    if (IsContratto(a)) prezzati.add((a + "_" + Rete(v)).toLowerCase(Locale.ROOT));
                }
            }
        }
        for (String[] v : archivio) {
            if (v != null && v.length > 28 && IsContratto(v[28]) && v[3] != null) entrati.add((v[3] + "|" + v[28]).toLowerCase(Locale.ROOT));
        }
        List<Riga> ordinate = new ArrayList<>(nuove);
        ordinate.sort(Comparator.comparing((Riga r) -> r.v()[0] == null ? "" : r.v()[0]));
        Map<String, Marcatura> marcature = new LinkedHashMap<>();
        for (Riga r : ordinate) {
            String[] v = r.v();
            if (v == null || v.length < 35) continue;
            String tipo = Tipo(v);
            boolean uscita = "PC".equals(tipo);
            String address = uscita ? v[26] : v[28];
            String rete = Rete(v);
            String simbolo = uscita ? v[8] : v[11];
            String nome = uscita ? v[25] : v[27];
            String chiave = (address + "_" + rete).toLowerCase(Locale.ROOT);
            Marcatura m = null;
            if ((uscita || "DC".equals(tipo)) && r.firmaVerificabile() && !r.firmataDalWallet() && IsContratto(address)
                    && !rete.isBlank() && "NO".equals(v[32]) && Numero(uscita ? v[10] : v[13]).signum() != 0
                    && !marcature.containsKey(chiave) && !prezzati.contains(chiave) && simbolo != null && !simbolo.isBlank()
                    && !Funzioni.isSCAM(simbolo) && !cfg.simboliLegittimi().contains(simbolo.trim())
                    && (esclusioni == null || !esclusioni.Esclude(address, rete, simbolo))) {
                String ticker;
                String link;
                if (uscita && r.storiaCompleta() && !entrati.contains((v[3] + "|" + address).toLowerCase(Locale.ROOT))) {
                    m = new Marcatura(address + "_" + rete, address, rete, simbolo, Regola.R0, "");
                } else if ((ticker = TickerImitato(simbolo, cfg)) != null) {
                    m = new Marcatura(address + "_" + rete, address, rete, simbolo, Regola.R1, ticker);
                } else if ((link = Link(cfg, simbolo, nome)) != null) {
                    m = new Marcatura(address + "_" + rete, address, rete, simbolo, Regola.R2, link);
                }
            }
            if (m != null) marcature.put(chiave, m);
            //Dopo la valutazione: il token entrato con questa riga vale per le righe successive
            if (IsContratto(v[28]) && v[3] != null) entrati.add((v[3] + "|" + v[28]).toLowerCase(Locale.ROOT));
        }
        return new ArrayList<>(marcature.values());
    }

    /** Le esclusioni lette dal programma: rinomine dell'utente, elenco CoinGecko, alias di prezzo, E-Money. */
    static Esclusioni EsclusioniDelProgramma() {
        Map<String, String> rinominati = DatabaseH2.RinominaToken_LeggiTabella();
        EmoneyToken.Elenco emt = EmoneyToken.Carica();
        Set<String> emoney = new HashSet<>();
        if (emt != null) for (String s : emt.simboli()) emoney.add(s.toUpperCase(Locale.ROOT));
        return (address, rete, simbolo) -> rinominati.containsKey(address + "_" + rete)
                || DatabaseH2.GestitiCoingecko_Leggi(address + "_" + rete) != null
                || AliasPrezziToken.InElenco(address.toLowerCase(Locale.ROOT), rete)
                || AliasPrezziToken.InElenco(address, rete)
                || emoney.contains(simbolo.trim().toUpperCase(Locale.ROOT))
                || DatabaseH2.Pers_Emoney_Leggi(simbolo.trim()) != null;
    }

    /**
     * Valuta le righe nuove contro l'archivio e marca SCAM i token presi: rinomina in {@code RINOMINATOKEN} (solo se il
     * token non ne ha già una) e nome aggiornato nelle righe nuove. Un errore non ferma mai l'importazione.
     *
     * @return i token marcati
     */
    static List<Marcatura> MarcaAllImportazione(List<Riga> nuove, Collection<String[]> archivio) {
        try {
            List<Marcatura> marcature = Valuta(nuove, archivio, Configurazione(), EsclusioniDelProgramma());
            List<Marcatura> fatte = new ArrayList<>();
            for (Marcatura m : marcature) {
                String[] esistente = DatabaseH2.RinominaToken_Leggi(m.chiave());
                if (esistente != null && (esistente[0] != null || esistente[1] != null)) continue;
                String nuovoNome = m.simbolo() + " **";
                DatabaseH2.RinominaToken_Scrivi(m.chiave(), m.simbolo(), nuovoNome);
                for (Riga r : nuove) {
                    String[] v = r.v();
                    if (!m.rete().equalsIgnoreCase(Rete(v))) continue;
                    if (m.address().equalsIgnoreCase(v[26])) v[8] = nuovoNome;
                    if (m.address().equalsIgnoreCase(v[28])) v[11] = nuovoNome;
                }
                fatte.add(m);
                LoggerGC.logInfo("TokenTruffa: " + m.simbolo() + " (" + m.chiave() + ") marcato SCAM, " + m.regola().descrizione
                        + (m.dettaglio().isEmpty() ? "" : " " + m.dettaglio()));
            }
            return fatte;
        } catch (Exception e) {
            LoggerGC.ScriviErrore(e);
            return List.of();
        }
    }

    /** Il testo per il messaggio di fine importazione (HTML), vuoto se non è stato marcato nulla. */
    static String Resoconto(List<Marcatura> fatte) {
        if (fatte == null || fatte.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("<br><br>Token marcati SCAM in automatico: " + fatte.size());
        int n = 0;
        for (Marcatura m : fatte) {
            if (++n > 10) {
                sb.append("<br>... e altri ").append(fatte.size() - 10);
                break;
            }
            String a = m.address();
            sb.append("<br>").append(Html(m.simbolo())).append(" (").append(m.rete()).append(", ")
                    .append(a.substring(0, 6)).append("...").append(a.substring(a.length() - 4)).append("): ")
                    .append(m.regola().descrizione);
            if (m.regola() == Regola.R1) sb.append(" ").append(m.dettaglio());
        }
        sb.append("<br>Si toglie da \"Analisi Crypto\", \"Gestione Token Scam\", \"Rimuovi da SCAM\".");
        return sb.toString();
    }

    private static String Html(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
