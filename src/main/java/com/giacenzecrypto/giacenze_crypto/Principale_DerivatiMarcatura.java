package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;

/**
 * Marcatura retroattiva dei derivati (M3): mette il marcatore {@link Derivati#CAMPO} sui movimenti importati <b>prima</b> che l'import lo
 * scrivesse (o con una configurazione che non lo scriveva), riconoscendoli dalla causale originale {@code [7]} e dal tipo che le
 * configurazioni di import dichiarano in {@code causaliDerivati} ({@code Bybit Derivati}, {@code Bybit UTA}, {@code Binance CSV}...).
 *
 * <p><b>Solo identificazione e solo su richiesta</b> (voce <i>Marca come derivato</i> del menu dei movimenti, con anteprima): il
 * marcatore non cambia nessun calcolo, un movimento marcato con campo 18 vuoto resta da classificare e non muove nessun anno gia'
 * dichiarato. Serve a vederlo come derivato (colonne, filtro, totale, avviso) e a poterlo classificare con
 * {@link Principale_Derivati}. Ogni marcatura entra nello storico delle modifiche ({@link #OP_MARCA}).
 *
 * <p><b>Cosa si marca.</b> Un movimento senza marcatore, categoria deposito, prelievo o commissione (le forme che l'import marca: i
 * derivati arrivano sempre come righe singole da classificare), il cui exchange {@code [3]} e la cui causale {@code [7]} compaiono in una
 * configurazione. L'abbinamento e' per exchange e causale, quindi una causale che due configurazioni dello stesso exchange usano
 * per cose diverse ({@code trade} del conto spot e del conto derivati di Bybit) si distingue dalla categoria: gli scambi spot
 * sono {@code SC}, non righe singole. Se due configurazioni dello stesso exchange danno tipi diversi alla stessa causale non si marca.
 *
 * <p><b>Spostamento sul conto derivati</b> (dalla 1.0.67, opzione P2 di D15): le configurazioni di oggi mettono il conto derivati di Bybit
 * e di Binance sul sotto-wallet {@value OKXDerivati#SOTTOWALLET} ({@code nomeWallet}/{@code walletPerCausale}). I movimenti importati prima
 * stanno su {@code Principale}, e una nuova importazione non li sposta (il sotto-wallet non e' nella chiave della deduplica): la stessa
 * voce li sposta, marcati o no, con una voce nello storico ({@link #OP_SPOSTA}). Nessun effetto con P1, con P2 decide la pila.
 */
public final class Principale_DerivatiMarcatura {

    private Principale_DerivatiMarcatura() {
    }

    /** Operazione dello storico delle modifiche. */
    static final String OP_MARCA = "MarcaDerivato";

    /** Operazione dello storico: movimento spostato sul sotto-wallet dei derivati. */
    static final String OP_SPOSTA = "SpostaContoDerivati";

    /** Voce del menu dei movimenti. */
    public static final String VOCE_MENU = "Marca come derivato";

    /** Il piano: quali movimenti si marcano e con quale tipo, e perche' gli altri no. */
    public static final class Piano {
        public final Map<String, String> voci = new LinkedHashMap<>();
        /** Movimenti da spostare sul sotto-wallet dei derivati (marcati o da marcare). */
        public final java.util.Set<String> sposta = new java.util.LinkedHashSet<>();
        public int giaMarcati = 0;
        public int nonRiconosciuti = 0;
    }

    /**
     * I tipi di derivato dichiarati dalle configurazioni di import.
     *
     * @param cartella cartella delle configurazioni ({@code config/import})
     * @return per exchange (minuscolo) la mappa causale -> tipo; una causale con tipi discordanti fra configurazioni e' esclusa
     */
    public static Map<String, Map<String, String>> TipiDaConfigurazioni(File cartella) {
        Map<String, Map<String, String>> risultato = new TreeMap<>();
        Map<String, java.util.Set<String>> discordi = new TreeMap<>();
        File[] file = cartella == null ? null : cartella.listFiles((d, n) -> n.toLowerCase().endsWith(".json"));
        if (file == null) return risultato;
        java.util.Arrays.sort(file);
        for (File f : file) {
            try {
                JSONObject j = new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
                JSONObject tipi = j.optJSONObject("causaliDerivati");
                String exchange = j.optString("nomeExchange", "").trim();
                if (tipi == null || exchange.isEmpty()) continue;
                Map<String, String> m = risultato.computeIfAbsent(exchange.toLowerCase(), k -> new LinkedHashMap<>());
                for (String causale : tipi.keySet()) {
                    String tipo = tipi.optString(causale, "").trim();
                    if (tipo.isEmpty()) continue;
                    String chiave = causale.trim();
                    String precedente = m.get(chiave);
                    if (precedente != null && !precedente.equals(tipo)) {
                        discordi.computeIfAbsent(exchange.toLowerCase(), k -> new java.util.HashSet<>()).add(chiave);
                    } else {
                        m.put(chiave, tipo);
                    }
                }
            } catch (Exception e) {
                //una configurazione illeggibile non ferma le altre: si segnala e si prosegue
                LoggerGC.logInfo("Marcatura derivati: configurazione non letta " + f.getName() + " (" + e.getMessage() + ")");
            }
        }
        for (Map.Entry<String, java.util.Set<String>> e : discordi.entrySet()) {
            Map<String, String> m = risultato.get(e.getKey());
            if (m != null) m.keySet().removeAll(e.getValue());
        }
        return risultato;
    }

    /**
     * Le causali che le configurazioni di import mettono sul sotto-wallet dei derivati ({@code walletPerCausale}, altrimenti
     * {@code nomeWallet}), fra quelle di {@code causaliDerivati} e di {@code walletPerCausale}. I giroconti ({@link Derivati#MARGINE})
     * non ci sono: prima non si importavano. Una causale che due configurazioni dello stesso exchange mettono su wallet diversi e' esclusa.
     *
     * @return per exchange (minuscolo) l'insieme delle causali
     */
    public static Map<String, java.util.Set<String>> ContiDaConfigurazioni(File cartella) {
        Map<String, Map<String, String>> destinazioni = new TreeMap<>();
        Map<String, java.util.Set<String>> discordi = new TreeMap<>();
        File[] file = cartella == null ? null : cartella.listFiles((d, n) -> n.toLowerCase().endsWith(".json"));
        if (file == null) return new TreeMap<>();
        java.util.Arrays.sort(file);
        for (File f : file) {
            try {
                JSONObject j = new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
                String exchange = j.optString("nomeExchange", "").trim().toLowerCase();
                if (exchange.isEmpty()) continue;
                String wallet = j.optString("nomeWallet", "").trim();
                JSONObject tipi = j.optJSONObject("causaliDerivati");
                JSONObject perCausale = j.optJSONObject("walletPerCausale");
                java.util.Set<String> causali = new java.util.TreeSet<>();
                if (tipi != null) {
                    for (String c : tipi.keySet()) if (!Derivati.MARGINE.equalsIgnoreCase(tipi.optString(c, "").trim())) causali.add(c.trim());
                }
                if (perCausale != null) {
                    for (String c : perCausale.keySet()) {
                        String t = tipi == null ? "" : tipi.optString(c, "").trim();
                        if (!Derivati.MARGINE.equalsIgnoreCase(t)) causali.add(c.trim());
                    }
                }
                Map<String, String> m = destinazioni.computeIfAbsent(exchange, k -> new TreeMap<>());
                for (String c : causali) {
                    String dest = perCausale != null && perCausale.has(c) ? perCausale.optString(c, "").trim() : wallet;
                    String prima = m.get(c);
                    if (prima != null && !prima.equalsIgnoreCase(dest)) discordi.computeIfAbsent(exchange, k -> new java.util.HashSet<>()).add(c);
                    else m.put(c, dest);
                }
            } catch (Exception e) {
                LoggerGC.logInfo("Conto derivati: configurazione non letta " + f.getName() + " (" + e.getMessage() + ")");
            }
        }
        Map<String, java.util.Set<String>> risultato = new TreeMap<>();
        for (Map.Entry<String, Map<String, String>> e : destinazioni.entrySet()) {
            java.util.Set<String> esclusi = discordi.getOrDefault(e.getKey(), java.util.Set.of());
            for (Map.Entry<String, String> c : e.getValue().entrySet()) {
                if (!esclusi.contains(c.getKey()) && ContoDerivati.IsSottoWallet(c.getValue())) {
                    risultato.computeIfAbsent(e.getKey(), k -> new java.util.TreeSet<>()).add(c.getKey());
                }
            }
        }
        return risultato;
    }

    /** @return {@code true} se il movimento sta fuori dal sotto-wallet dei derivati ma la sua configurazione lo mette li' */
    static boolean DaSpostare(String[] v, Map<String, java.util.Set<String>> conti) {
        if (v == null || v.length <= Derivati.CAMPO || ContoDerivati.SulConto(v) || v[3] == null || v[7] == null) return false;
        if (Derivati.IsRigaContrattoOMargine(v) || "AU".equalsIgnoreCase(v[22])) return false;
        String cat = categoria(v);
        if (!cat.equals("DC") && !cat.equals("PC") && !cat.equals("CM") && !cat.equals("DF") && !cat.equals("PF")) return false;
        java.util.Set<String> c = conti.get(v[3].trim().toLowerCase());
        return c != null && c.contains(v[7].trim());
    }

    private static String categoria(String[] v) {
        String[] parti = v[0].split("_");
        return parti.length > 4 ? parti[4].trim().toUpperCase() : "";
    }

    /**
     * Il tipo con cui marcare il movimento, oppure {@code null} se non va marcato.
     *
     * @param tipi {@link #TipiDaConfigurazioni}
     */
    static String TipoPerMovimento(String[] v, Map<String, Map<String, String>> tipi) {
        if (v == null || v.length <= Derivati.CAMPO || Derivati.isDerivato(v)) return null;
        String cat = categoria(v);
        if (!cat.equals("DC") && !cat.equals("PC") && !cat.equals("CM") && !cat.equals("DF") && !cat.equals("PF")) return null;
        if (v[3] == null || v[7] == null) return null;
        Map<String, String> m = tipi.get(v[3].trim().toLowerCase());
        if (m == null) return null;
        String tipo = m.get(v[7].trim());
        if (tipo == null) return null;
        return cat.equals("CM") ? Derivati.COMMISSIONE : tipo;
    }

    /** Costruisce il piano per i movimenti indicati, senza toccare nulla. */
    public static Piano Pianifica(Collection<String> IDs, Map<String, Map<String, String>> tipi) {
        return Pianifica(IDs, tipi, Map.of());
    }

    /** Come {@link #Pianifica(Collection, Map)}, con lo spostamento sul sotto-wallet dei derivati ({@link #ContiDaConfigurazioni}). */
    public static Piano Pianifica(Collection<String> IDs, Map<String, Map<String, String>> tipi, Map<String, java.util.Set<String>> conti) {
        Piano piano = new Piano();
        for (String id : IDs) {
            String[] v = MappaCryptoWallet.get(id);
            if (v == null) continue;
            if (DaSpostare(v, conti)) piano.sposta.add(id);
            if (Derivati.isDerivato(v)) {
                piano.giaMarcati++;
                continue;
            }
            String tipo = TipoPerMovimento(v, tipi);
            if (tipo == null) {
                if (!piano.sposta.contains(id)) piano.nonRiconosciuti++;
                continue;
            }
            piano.voci.put(id, tipo);
        }
        return piano;
    }

    /**
     * Marca i movimenti del piano, ricontrollando ognuno (fra anteprima e conferma l'archivio puo' essere cambiato).
     *
     * @return quanti movimenti sono stati marcati. Chi chiama ricalcola una volta sola alla fine
     */
    public static int Applica(Piano piano, Map<String, Map<String, String>> tipi) {
        return Applica(piano, tipi, Map.of());
    }

    /** Come {@link #Applica(Piano, Map)}, con lo spostamento sul sotto-wallet dei derivati, anch'esso ricontrollato. */
    public static int Applica(Piano piano, Map<String, Map<String, String>> tipi, Map<String, java.util.Set<String>> conti) {
        int fatti = 0, spostati = 0;
        java.util.Set<String> tutti = new java.util.LinkedHashSet<>(piano.voci.keySet());
        tutti.addAll(piano.sposta);
        for (String id : tutti) {
            String[] v = MappaCryptoWallet.get(id);
            String tipo = piano.voci.containsKey(id) ? TipoPerMovimento(v, tipi) : null;
            boolean sposta = piano.sposta.contains(id) && DaSpostare(v, conti);
            if (tipo == null && !sposta) continue;
            String prima = Importazioni.SerializzaRiga(v);
            if (tipo != null) {
                Derivati.Marca(v, tipo);
                fatti++;
            }
            if (sposta) {
                v[4] = OKXDerivati.SOTTOWALLET;
                spostati++;
            }
            MovimentiStorico.AccodaModifica(MovimentiStorico.AssicuraLignaggio(v), id, id, prima, sposta ? OP_SPOSTA : OP_MARCA);
        }
        if (fatti > 0) LoggerGC.logInfo("Marcati come derivati " + fatti + " movimenti");
        if (spostati > 0) LoggerGC.logInfo("Spostati sul sotto-wallet " + OKXDerivati.SOTTOWALLET + " " + spostati + " movimenti");
        return fatti + spostati;
    }

    /** Testo dell'anteprima: dice l'effetto, non come trattare il caso. */
    public static String TestoAnteprima(Piano piano) {
        StringBuilder t = new StringBuilder();
        if (piano.voci.isEmpty() && piano.sposta.isEmpty()) {
            t.append("Nessun movimento selezionato ha una causale che le configurazioni di import riconoscono come derivato.\n");
        } else if (!piano.voci.isEmpty()) {
            Map<String, Integer> perTipo = new TreeMap<>();
            Map<String, Integer> perAnno = new TreeMap<>();
            Map<String, Integer> perExchange = new TreeMap<>();
            for (Map.Entry<String, String> e : piano.voci.entrySet()) {
                String[] v = MappaCryptoWallet.get(e.getKey());
                perTipo.merge(e.getValue(), 1, Integer::sum);
                perAnno.merge(v[1] == null || v[1].length() < 4 ? "?" : v[1].substring(0, 4), 1, Integer::sum);
                perExchange.merge(v[3], 1, Integer::sum);
            }
            t.append("Movimenti che verranno marcati come derivati: ").append(piano.voci.size()).append("\n");
            t.append("Exchange: ").append(perExchange).append("\n");
            t.append("Tipi: ").append(perTipo).append("\n");
            t.append("Anni: ").append(perAnno).append("\n\n");
            t.append("Marcare dice solo che cosa sono: non cambia nessuna plusvalenza, giacenza o anno già dichiarato. ")
             .append("I movimenti restano da classificare e potranno essere classificati con la voce Classifica come derivato. ")
             .append("Compariranno nelle colonne Derivato e Risultato derivati, nel totale dei redditi da derivati e negli avvisi sui derivati.\n");
        }
        if (!piano.sposta.isEmpty()) {
            Map<String, Integer> perExchange = new TreeMap<>();
            for (String id : piano.sposta) perExchange.merge(MappaCryptoWallet.get(id)[3], 1, Integer::sum);
            t.append(piano.voci.isEmpty() ? "" : "\n").append("Movimenti che passano sul sotto-wallet ").append(OKXDerivati.SOTTOWALLET).append(": ")
             .append(piano.sposta.size()).append(" ").append(perExchange).append("\n")
             .append("Sono profitti, perdite, funding, commissioni e rimborsi del conto derivati, che le importazioni di questa versione mettono ")
             .append("sul sotto-wallet ").append(OKXDerivati.SOTTOWALLET).append(" e quelle precedenti mettevano sul wallet principale. ")
             .append("Lo spostamento non cambia nessun calcolo se il conto derivati non è rilevante (opzione predefinita). ")
             .append("I giroconti fra il conto derivati e gli altri conti dell'exchange entrano reimportando gli stessi file: finché mancano, ")
             .append("il sotto-wallet può risultare negativo fra gli errori.\n");
        }
        if (piano.giaMarcati > 0) t.append("\nGià marcati: ").append(piano.giaMarcati).append(".\n");
        if (piano.nonRiconosciuti > 0 && !piano.voci.isEmpty()) {
            t.append("Non riconosciuti come derivati (restano come sono): ").append(piano.nonRiconosciuti).append(".\n");
        }
        return t.toString();
    }

    /** @return le configurazioni dell'utente, lette dalla cartella di lavoro */
    public static Map<String, Map<String, String>> TipiCorrenti() {
        return TipiDaConfigurazioni(new File(VarStatiche.getCartella_ConfigImport()));
    }

    /** @return {@link #ContiDaConfigurazioni} sulle configurazioni dell'utente */
    public static Map<String, java.util.Set<String>> ContiCorrenti() {
        return ContiDaConfigurazioni(new File(VarStatiche.getCartella_ConfigImport()));
    }

    /** Solo per i test: l'elenco delle voci come lista. */
    static List<String> Id(Piano p) {
        return List.copyOf(p.voci.keySet());
    }
}
