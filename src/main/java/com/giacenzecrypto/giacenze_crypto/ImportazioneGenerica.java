package com.giacenzecrypto.giacenze_crypto;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONObject;


public class ImportazioneGenerica {

    public static boolean importa(String fileCSV, String fileConfigurazione,
            boolean sovrascriEsistenti, Download progressb) {

        Importazioni.AzzeraContatori();

        // Leggo il file di configurazione
        ConfigurazioneImport cfg;
        try {
            cfg = ConfigurazioneImport.carica(fileConfigurazione);
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return false;
        }

        // Leggo il file csv e creo la lista dei soli righi con dati validi
        List<String[]> righe;
        try {
            righe = leggiCSV(fileCSV, cfg);
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return false;
        }

        if (progressb != null) {
            progressb.SetMassimo(righe.size());
            progressb.SetAvanzamento(0);
        }

        List<String[]> listaCompleta = new ArrayList<>();
        List<String[]> gruppoCorrente = new ArrayList<>();
        String ultimaChiaveGruppo = null;
        List<String[]> movimentiDifferiti = new ArrayList<>();

        for (int i = 0; i < righe.size(); i++) {
            if (progressb != null) progressb.SetAvanzamento(i + 1);
            if (progressb != null && progressb.FineThread) return false;

            String[] riga = righe.get(i);
            String chiave = calcolaChiaveGruppo(riga, cfg);
            if (chiave == null || chiave.isBlank()) continue;

            if (chiave.equals(ultimaChiaveGruppo)) {
                gruppoCorrente.add(riga);
            } else {
                if (!gruppoCorrente.isEmpty()) {
                    listaCompleta.addAll(consolidaGruppo(gruppoCorrente, cfg, movimentiDifferiti));
                }
                gruppoCorrente = new ArrayList<>();
                gruppoCorrente.add(riga);
                ultimaChiaveGruppo = chiave;
            }
        }

        if (!gruppoCorrente.isEmpty()) {
            listaCompleta.addAll(consolidaGruppo(gruppoCorrente, cfg, movimentiDifferiti));
        }

        if (!movimentiDifferiti.isEmpty()) {
            Importazioni.ConsolidaMovimentiDifferiti(movimentiDifferiti, sovrascriEsistenti);
        }

        int[] insScart = Importazioni.ScriviListaSuMappaCrypto(listaCompleta, sovrascriEsistenti);
        Importazioni.TransazioniAggiunte = insScart[0];
        Importazioni.TrasazioniScartate = insScart[1];
        Importazioni.Transazioni = insScart[0] + insScart[1];

        if (Importazioni.TransazioniAggiunte > 0) Principale.TabellaCryptodaAggiornare = true;
        return true;
    }

    // -------------------------------------------------------------------------
    // LETTURA CSV
    // -------------------------------------------------------------------------

    private static List<String[]> leggiCSV(String fileCSV, ConfigurazioneImport cfg) throws IOException {
        List<String> righeRaw = new ArrayList<>();
        List<String[]> risultato = new ArrayList<>();
        String sep = cfg.separatore;
        String encoding = cfg.encoding != null ? cfg.encoding : "UTF-8";

        // FASE 1 – estraggo le righe con dati
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileCSV), encoding))) {
            String riga;
            int numRiga = 0;
            while ((riga = br.readLine()) != null) {
                numRiga++;
                if (riga == null) continue;
                riga = riga.replace("\"", "").replaceAll("\uFEFF", "").trim();
                if (riga.isBlank()) continue;
                if (numRiga <= cfg.righeIntestazione) {
                    if (numRiga == cfg.rigaIntestazione && cfg.autoDetectColonne)
                        cfg.risolviColonneDaIntestazione(riga.split(sep, -1));
                    continue;
                }
                if (riga.matches("[-,]+")) continue;
                righeRaw.add(riga);
            }
        }

        // FASE 2 – splitto e valido
        for (String r : righeRaw) {
            String[] campi = r.split(sep, -1);
            if (campi.length <= cfg.colonnaData) { scarta("RIGA TROPPO CORTA", r); continue; }
            String dataStr = cfg.normalizzaData(safe(campi, cfg.colonnaData));
            if (dataStr == null || dataStr.isBlank()) { scarta("DATA NON VALIDA", r); continue; }
            long dataLong = FunzioniDate.ConvertiDatainLongSecondo(dataStr);
            if (dataLong <= 0) { scarta("DATA NON PARSABILE", r); continue; }
            risultato.add(campi);
        }

        // FASE 3 – ordino per data
        risultato.sort((a, b) -> {
            long da = FunzioniDate.ConvertiDatainLongSecondo(cfg.normalizzaData(safe(a, cfg.colonnaData)));
            long db = FunzioniDate.ConvertiDatainLongSecondo(cfg.normalizzaData(safe(b, cfg.colonnaData)));
            return Long.compare(da, db);
        });

        return risultato;
    }

    // -------------------------------------------------------------------------
    // RAGGRUPPAMENTO
    // -------------------------------------------------------------------------

    private static String calcolaChiaveGruppo(String[] riga, ConfigurazioneImport cfg) {
        String data = cfg.normalizzaData(safe(riga, cfg.colonnaData));
        if (data == null || data.isBlank()) return null;
        if (cfg.colonnaIDTransazione >= 0 && cfg.colonnaIDTransazione < riga.length) {
            String id = safe(riga, cfg.colonnaIDTransazione);
            if (!id.isBlank()) return id + "|" + data;
        }
        return data;
    }

    // -------------------------------------------------------------------------
    // PUNTO 2 – consolidaGruppo RISCRITTO
    // -------------------------------------------------------------------------

    private static List<String[]> consolidaGruppo(List<String[]> gruppo,
            ConfigurazioneImport cfg, List<String[]> differiti) {

        List<String[]> risultato = new ArrayList<>();

        // Caso 1: movimento singolo – consolido direttamente
        if (gruppo.size() == 1) {
            String[] mov = costruisciMovimento(gruppo.get(0), null, cfg);
            if (mov != null) risultato.add(mov);
            return risultato;
        }

        // Caso multi-riga: per ogni riga verifico se la causale è in movimentoChiuso
        // (gestione singola) oppure no (gestione TransazioneDefi come in Importazioni.java)
        TransazioneDefi scambio = new TransazioneDefi();

        // Tengo traccia della data e del wallet per la chiamata finale a RitornaScambi
        String dataDiGruppo = null;
        String walletPrincipale = null;
        String walletID = null;
        boolean haMovimentiDefi = false;

        for (String[] riga : gruppo) {
            String causaleCSV = safe(riga, cfg.colonnaCausale);
            String tipoMovimento = cfg.convertiCausale(causaleCSV);

            if (dataDiGruppo == null) {
                dataDiGruppo = cfg.normalizzaData(safe(riga, cfg.colonnaData));
                walletPrincipale = cfg.nomeWallet != null ? cfg.nomeWallet : "Principale";
                String overrideWallet = cfg.walletPerCausale.get(causaleCSV);
                if (overrideWallet != null && !overrideWallet.isBlank()) walletPrincipale = overrideWallet;
                String exchange = cfg.nomeExchange != null ? cfg.nomeExchange : "Exchange Generico";
                walletID = exchange + "|" + safe(riga, cfg.colonnaIDTransazione);
            }

            // Se la causale è in movimentoChiuso => tratto come movimento singolo
            if (cfg.causaliChiuse.contains(causaleCSV) ||
                (tipoMovimento != null && cfg.causaliChiuse.contains(tipoMovimento))) {
                String[] mov = costruisciMovimento(riga, null, cfg);
                if (mov != null) risultato.add(mov);
                continue;
            }

            // Altrimenti accumulo nel TransazioneDefi come fa Importazioni.java
            // con InserisciMoneteCEX
            String qtaStr = normalizzaNumero(safe(riga, cfg.colonnaQuantita));
            String moneta = cfg.normalizzaMoneta(safe(riga, cfg.colonnaMoneta));
            String idOrig = safe(riga, cfg.colonnaIDTransazione);

            // Applico la regola di segno del JSON (causaliUscita / causaliEntrata)
            ConfigurazioneImport.RegolaSegno regola = cfg.regolaSegno(causaleCSV);
            if (regola == ConfigurazioneImport.RegolaSegno.FORZATO_USCITA) {
                if (!qtaStr.startsWith("-")) qtaStr = "-" + qtaStr;
            } else if (regola == ConfigurazioneImport.RegolaSegno.FORZATO_ENTRATA) {
                qtaStr = qtaStr.replace("-", "");
            }

            if (qtaStr.isBlank() || !Funzioni.isNumeric(qtaStr, false)) continue;

            Moneta mon = new Moneta();
            mon.InserisciValori(moneta, qtaStr, "", "");
            mon.AssegnaTipoAuto();

            // Recupero prezzo se disponibile
            String valEuro = normalizzaNumero(safe(riga, cfg.colonnaValoreEuro));
            String prezzoUn = normalizzaNumero(safe(riga, cfg.colonnaPrezzo));
            long dataLong = FunzioniDate.ConvertiDatainLongSecondo(dataDiGruppo);

            Prezzi.InfoPrezzo ip = null;
            if (valEuro.isBlank() && prezzoUn.isBlank()) {
                ip = Prezzi.DammiPrezzoInfoTransazione(mon, null, dataLong, null, "");
            }
            if (ip != null) {
                mon.SetPrezzo(ip.prezzoQta != null ? ip.prezzoQta.toPlainString() : "0");
                mon.InfoPrezzo = ip;
            } else if (!valEuro.isBlank()) {
                mon.SetPrezzo(valEuro);
                mon.setFontePrezzo("CSV");
            } else if (!prezzoUn.isBlank()) {
                mon.SetPrezzo(prezzoUn);
                mon.setFontePrezzo("CSV");
            }
            else {
                mon.SetPrezzo("0");  // ← fallback obbligatorio, mai null
            }

            // Determino wallet secondario
            String walletSec = walletPrincipale;
            String owallet = cfg.walletPerCausale.get(causaleCSV);
            if (owallet != null && !owallet.isBlank()) walletSec = owallet;

            scambio.InserisciMoneteCEX(mon, walletSec, causaleCSV, idOrig);
            haMovimentiDefi = true;
        }

        // Se ho accumulato movimenti nel TransazioneDefi, li elaboro con RitornaScambi
        if (haMovimentiDefi && !scambio.isEmpty()) {
            List<String[]> movScambio = Importazioni.RitornaScambi(
                    scambio, dataDiGruppo, walletPrincipale, walletID);
            if (movScambio != null) risultato.addAll(movScambio);
        }

        return risultato;
    }

    // -------------------------------------------------------------------------
    // PUNTO 1 – costruisciMovimento con segno SENZA abs()
    // -------------------------------------------------------------------------

    private static String[] costruisciMovimento(String[] riga, String tipoForzato, ConfigurazioneImport cfg) {
        try {
            String data = cfg.normalizzaData(safe(riga, cfg.colonnaData));
            if (data == null || data.isBlank()) return null;
            long dataLong = FunzioniDate.ConvertiDatainLongSecondo(data);
            if (dataLong <= 0) return null;

            String exchange = nvl(cfg.nomeExchange, "Exchange Generico");
            String wallet = nvl(cfg.nomeWallet, "Principale");

            if (cfg.colonnaWallet >= 0 && cfg.colonnaWallet < riga.length && !safe(riga, cfg.colonnaWallet).isBlank())
                wallet = safe(riga, cfg.colonnaWallet);

            String causaleCSV = safe(riga, cfg.colonnaCausale);

            String walletOverride = cfg.walletPerCausale.get(causaleCSV);
            if (walletOverride != null && !walletOverride.isBlank()) wallet = walletOverride;

            String tipoMovimento = tipoForzato != null ? tipoForzato : cfg.convertiCausale(causaleCSV);
            if (tipoMovimento == null || tipoMovimento.isBlank()) {
                scarta("CAUSALE SCONOSCIUTA: " + causaleCSV, Arrays.toString(riga));
                return null;
            }
            if (tipoMovimento.equalsIgnoreCase("IGNORA") || tipoMovimento.equalsIgnoreCase("NON CONSIDERARE"))
                return null;

            String moneta    = cfg.normalizzaMoneta(safe(riga, cfg.colonnaMoneta));
            String qtaStr    = normalizzaNumero(safe(riga, cfg.colonnaQuantita));
            String monetaFee = cfg.normalizzaMoneta(safe(riga, cfg.colonnaMonetaFee));
            String qtaFee    = normalizzaNumero(safe(riga, cfg.colonnaQuantitaFee));
            String valoreEuro = normalizzaNumero(safe(riga, cfg.colonnaValoreEuro));
            String prezzo    = normalizzaNumero(safe(riga, cfg.colonnaPrezzo));
            String idTrans   = safe(riga, cfg.colonnaIDTransazione);

            // Gestione segno da colonna dedicata (se presente)
            if (cfg.colonnaSegno >= 0 && cfg.colonnaSegno < riga.length) {
                String segno = safe(riga, cfg.colonnaSegno);
                if ("-".equals(segno) && !qtaStr.startsWith("-")) qtaStr = "-" + qtaStr;
                else if ("+".equals(segno)) qtaStr = qtaStr.replace("-", "");
            }

            // ----------------------------------------------------------------
            // PUNTO 1: il segno della qta NON viene mai azzerato con abs().
            // La qta arriva già con il suo segno dal CSV (o dalla colonna segno).
            // In più, se la causale è in causaliUscita/causaliEntrata, FORZO il segno.
            // ----------------------------------------------------------------
            ConfigurazioneImport.RegolaSegno regola = cfg.regolaSegno(causaleCSV);
            if (regola == ConfigurazioneImport.RegolaSegno.FORZATO_USCITA) {
                if (!qtaStr.startsWith("-")) qtaStr = "-" + qtaStr;
            } else if (regola == ConfigurazioneImport.RegolaSegno.FORZATO_ENTRATA) {
                qtaStr = qtaStr.replace("-", "");
            }

            // Costruisco mOUT / mIN in base al segno della qta (come fa MovimentiCrypto)
            Moneta mOUT = null;
            Moneta mIN  = null;

            if (!qtaStr.isBlank() && Funzioni.isNumeric(qtaStr, false)) {
                BigDecimal qta = new BigDecimal(qtaStr);
                if (qta.compareTo(BigDecimal.ZERO) < 0) {
                    mOUT = new Moneta();
                    mOUT.InserisciValori(moneta, qta.toPlainString(), "", "");
                    mOUT.AssegnaTipoAuto();
                } else if (qta.compareTo(BigDecimal.ZERO) > 0) {
                    mIN = new Moneta();
                    mIN.InserisciValori(moneta, qta.toPlainString(), "", "");
                    mIN.AssegnaTipoAuto();
                }
            }

            String prezzoMov = !valoreEuro.isBlank() ? valoreEuro : prezzo;

            String[] rt = MovimentiCrypto.creaMovimento(
                    mOUT, mIN, exchange, wallet, dataLong,
                    prezzoMov, "CSV", 1, 1, null, null, "A",
                    idTrans, tipoMovimento, exchange + "." + idTrans);

            if (rt == null) return null;

            if (!monetaFee.isBlank() && !qtaFee.isBlank() && rt.length > 12) {
                rt[11] = monetaFee;
                rt[12] = qtaFee;
            }
            if (rt.length > 7)  rt[7]  = causaleCSV;
            if (rt.length > 39) rt[39] = "D";

            // Campi extra dal JSON
            for (Map.Entry<String, Integer> e : cfg.campiExtra.entrySet()) {
                try {
                    int campoMov = Integer.parseInt(e.getKey());
                    int colCsv   = e.getValue();
                    if (campoMov >= 0 && campoMov < rt.length)
                        rt[campoMov] = safe(riga, colCsv);
                } catch (Exception ex) { LoggerGC.ScriviErrore(ex); }
            }

            return rt;

        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // costruisciMovimentoScambio (usato per scambi già splittati in OUT+IN)
    // -------------------------------------------------------------------------

    private static String[] costruisciMovimentoScambio(String[] rigaOUT, String[] rigaIN,
            ConfigurazioneImport cfg) {
        try {
            String data = cfg.normalizzaData(safe(rigaOUT, cfg.colonnaData));
            if (data == null || data.isBlank()) return null;
            long dataLong = FunzioniDate.ConvertiDatainLongSecondo(data);
            if (dataLong <= 0) return null;

            String monetaOUT = cfg.normalizzaMoneta(safe(rigaOUT, cfg.colonnaMoneta));
            String qtaOUT    = normalizzaNumero(safe(rigaOUT, cfg.colonnaQuantita));
            String monetaIN  = cfg.normalizzaMoneta(safe(rigaIN,  cfg.colonnaMoneta));
            String qtaIN     = normalizzaNumero(safe(rigaIN,  cfg.colonnaQuantita));
            String idTrans   = safe(rigaOUT, cfg.colonnaIDTransazione);
            String valoreEuro = normalizzaNumero(safe(rigaOUT, cfg.colonnaValoreEuro));

            // Garantisco segni corretti: OUT negativo, IN positivo
            if (!qtaOUT.startsWith("-")) qtaOUT = "-" + qtaOUT;
            qtaIN = qtaIN.replace("-", "");

            Moneta mOUT = new Moneta();
            mOUT.InserisciValori(monetaOUT, qtaOUT, "", "");
            mOUT.AssegnaTipoAuto();

            Moneta mIN = new Moneta();
            mIN.InserisciValori(monetaIN, qtaIN, "", "");
            mIN.AssegnaTipoAuto();

            String[] rt = MovimentiCrypto.creaMovimento(
                    mOUT, mIN,
                    nvl(cfg.nomeExchange, "Exchange Generico"),
                    nvl(cfg.nomeWallet,   "Principale"),
                    dataLong, valoreEuro, "CSV", 1, 1, null, null, "A",
                    idTrans, "SCAMBIO CRYPTO-CRYPTO",
                    nvl(cfg.nomeExchange, "Exchange Generico") + "." + idTrans);

            if (rt == null) return null;
            if (rt.length > 39) rt[39] = "D";
            return rt;

        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // determinaSegno (usato per identificare OUT/IN/FEE nei gruppi multi-riga)
    // -------------------------------------------------------------------------

    private static String determinaSegno(String[] riga, ConfigurazioneImport cfg) {
        String causale = safe(riga, cfg.colonnaCausale);
        String tipo    = cfg.convertiCausale(causale);

        if (tipo != null && tipo.toUpperCase().contains("COMMISSIONI")) return "FEE";

        if (cfg.colonnaSegno >= 0 && cfg.colonnaSegno < riga.length) {
            String s = safe(riga, cfg.colonnaSegno);
            if ("-".equals(s)) return "-";
            if ("+".equals(s)) return "+";
        }

        if (cfg.colonnaQuantita >= 0 && cfg.colonnaQuantita < riga.length) {
            String q = normalizzaNumero(safe(riga, cfg.colonnaQuantita));
            if (q.startsWith("-")) return "-";
            if (!q.isBlank() && Funzioni.isNumeric(q, false)) return "+";
        }

        // Fallback su causaliUscita/causaliEntrata
        ConfigurazioneImport.RegolaSegno regola = cfg.regolaSegno(causale);
        if (regola == ConfigurazioneImport.RegolaSegno.FORZATO_USCITA)  return "-";
        if (regola == ConfigurazioneImport.RegolaSegno.FORZATO_ENTRATA) return "+";

        return "";
    }

    // -------------------------------------------------------------------------
    // UTILITY
    // -------------------------------------------------------------------------

    private static String safe(String[] arr, int idx) {
        if (arr == null || idx < 0 || idx >= arr.length || arr[idx] == null) return "";
        return arr[idx].trim();
    }

    private static String normalizzaNumero(String s) {
    if (s == null) return "";
    s = s.trim();
    if (s.isBlank()) return "";
    s = s.replace(" ", "").replace(",", ".");

    // Converti notazione scientifica (es. 9.4E-7, 1.2e+3) in decimale piano
    try {
        if (s.toUpperCase().contains("E")) {
            s = new BigDecimal(s).toPlainString();
        }
    } catch (NumberFormatException ex) {
        // se non è un numero valido lascia passare così com'è
    }

    return s;
}

    private static String nvl(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

private static void scarta(String motivo, String riga) {
    Importazioni.movimentiSconosciuti += motivo + " | " + riga + "\n";
    Importazioni.TrasazioniScartate++;    // <- aggiunta
    Importazioni.TrasazioniSconosciute++;
}

    // =========================================================================
    // CLASSE CONFIGURAZIONE
    // =========================================================================

    public static class ConfigurazioneImport {

        public String nomeExchange    = "Exchange Generico";
        public String nomeWallet      = "Principale";
        public String separatore      = ",";
        public String encoding        = "UTF-8";
        public int    righeIntestazione = 1;
        public int    rigaIntestazione  = 1;
        public boolean autoDetectColonne = false;
        public String formatoData     = "yyyy-MM-dd HH:mm:ss";
        public String fuso            = "UTC";

        // Indici colonne (-1 = non presente)
        public int colonnaData          = 0;
        public int colonnaMoneta        = 1;
        public int colonnaQuantita      = 2;
        public int colonnaSegno         = -1;
        public int colonnaCausale       = 3;
        public int colonnaValoreEuro    = -1;
        public int colonnaPrezzo        = -1;
        public int colonnaMonetaFee     = -1;
        public int colonnaQuantitaFee   = -1;
        public int colonnaIDTransazione = -1;
        public int colonnaWallet        = -1;

        public boolean consolidaRigheStessaData = false;

        public Map<String, String>  mappaCausali     = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        public Set<String>          causaliUscita    = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        public Set<String>          causaliEntrata   = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        /**
         * NUOVO – causali che, anche in gruppo multi-riga, vanno trattate
         * come movimento singolo (non accumulate nel TransazioneDefi).
         * Es: commissioni, earn, reward, trasferimenti interni.
         */
        public Set<String>          causaliChiuse    = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        public Map<String, String>  rinominaMonete   = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        public List<String>         rimuoviDaNomeMoneta = new ArrayList<>();
        public Map<String, Integer> campiExtra       = new TreeMap<>();
        public Map<String, String>  walletPerCausale = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        public Map<String, Integer>  mappaNomiColonne = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        // -----------------------------------------------------------------
        // Caricamento da JSON
        // -----------------------------------------------------------------

        public static ConfigurazioneImport carica(String percorso) throws Exception {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(percorso))) {
                String riga;
                while ((riga = br.readLine()) != null) sb.append(riga);
            }
            JSONObject root = new JSONObject(sb.toString());
            ConfigurazioneImport cfg = new ConfigurazioneImport();

            if (root.has("nomeExchange"))          cfg.nomeExchange    = root.getString("nomeExchange");
            if (root.has("nomeWallet"))             cfg.nomeWallet      = root.getString("nomeWallet");
            if (root.has("separatore"))             cfg.separatore      = root.getString("separatore");
            if (root.has("encoding"))               cfg.encoding        = root.getString("encoding");
            if (root.has("righeIntestazione"))      cfg.righeIntestazione = root.getInt("righeIntestazione");
            if (root.has("rigaIntestazione"))       cfg.rigaIntestazione  = root.getInt("rigaIntestazione");
            if (root.has("autoDetectColonne"))      cfg.autoDetectColonne = root.getBoolean("autoDetectColonne");
            if (root.has("formatoData"))            cfg.formatoData     = root.getString("formatoData");
            if (root.has("fuso"))                   cfg.fuso            = root.getString("fuso");
            if (root.has("consolidaRigheStessaData")) cfg.consolidaRigheStessaData = root.getBoolean("consolidaRigheStessaData");

            if (root.has("colonne")) {
                JSONObject col = root.getJSONObject("colonne");
                if (col.has("data"))           cfg.colonnaData          = col.getInt("data");
                if (col.has("moneta"))         cfg.colonnaMoneta        = col.getInt("moneta");
                if (col.has("quantita"))       cfg.colonnaQuantita      = col.getInt("quantita");
                if (col.has("segno"))          cfg.colonnaSegno         = col.getInt("segno");
                if (col.has("causale"))        cfg.colonnaCausale       = col.getInt("causale");
                if (col.has("valoreEuro"))     cfg.colonnaValoreEuro    = col.getInt("valoreEuro");
                if (col.has("prezzo"))         cfg.colonnaPrezzo        = col.getInt("prezzo");
                if (col.has("monetaFee"))      cfg.colonnaMonetaFee     = col.getInt("monetaFee");
                if (col.has("quantitaFee"))    cfg.colonnaQuantitaFee   = col.getInt("quantitaFee");
                if (col.has("idTransazione"))  cfg.colonnaIDTransazione = col.getInt("idTransazione");
                if (col.has("wallet"))         cfg.colonnaWallet        = col.getInt("wallet");
            }

            if (root.has("mappaCausali")) {
                JSONObject mc = root.getJSONObject("mappaCausali");
                for (String k : mc.keySet()) cfg.mappaCausali.put(k, mc.getString(k));
            }
            if (root.has("causaliUscita")) {
                JSONArray arr = root.getJSONArray("causaliUscita");
                for (int i = 0; i < arr.length(); i++) cfg.causaliUscita.add(arr.getString(i));
            }
            if (root.has("causaliEntrata")) {
                JSONArray arr = root.getJSONArray("causaliEntrata");
                for (int i = 0; i < arr.length(); i++) cfg.causaliEntrata.add(arr.getString(i));
            }

            // NUOVO: causaliChiuse (movimentiChiuso nel JSON per compatibilità)
            String keyChiuse = root.has("causaliChiuse") ? "causaliChiuse"
                             : root.has("movimentoChiuso") ? "movimentoChiuso" : null;
            if (keyChiuse != null) {
                JSONArray arr = root.getJSONArray(keyChiuse);
                for (int i = 0; i < arr.length(); i++) cfg.causaliChiuse.add(arr.getString(i));
            }

            if (root.has("rinominaMonete")) {
                JSONObject rm = root.getJSONObject("rinominaMonete");
                for (String k : rm.keySet()) cfg.rinominaMonete.put(k, rm.getString(k));
            }
            if (root.has("rimuoviDaNomeMoneta")) {
                JSONArray arr = root.getJSONArray("rimuoviDaNomeMoneta");
                for (int i = 0; i < arr.length(); i++) cfg.rimuoviDaNomeMoneta.add(arr.getString(i));
            }
            if (root.has("campiExtra")) {
                JSONObject ce = root.getJSONObject("campiExtra");
                for (String k : ce.keySet()) cfg.campiExtra.put(k, ce.getInt(k));
            }
            if (root.has("walletPerCausale")) {
                JSONObject wc = root.getJSONObject("walletPerCausale");
                for (String k : wc.keySet()) cfg.walletPerCausale.put(k, wc.getString(k));
            }

            return cfg;
        }

        // -----------------------------------------------------------------
        // Risoluzione colonne da intestazione
        // -----------------------------------------------------------------
        
        public void risolviColonneDaIntestazione(String[] intestazione) {
            for (int i = 0; i < intestazione.length; i++)
                mappaNomiColonne.put(intestazione[i].trim(), i);
        }

        // -----------------------------------------------------------------
        // Conversione causale
        // -----------------------------------------------------------------

        public String convertiCausale(String causaleCSV) {
            if (causaleCSV == null) return null;
            String r = mappaCausali.get(causaleCSV.trim());
            if (r != null) return r;
            for (Map.Entry<String, String> e : mappaCausali.entrySet())
                if (causaleCSV.toLowerCase().contains(e.getKey().toLowerCase())) return e.getValue();
            return null;
        }

        // -----------------------------------------------------------------
        // Normalizzazione data
        // -----------------------------------------------------------------

        public String normalizzaData(String dataCSV) {
            if (dataCSV == null) return null;
            String s = dataCSV.trim();
            if (s.isBlank() || s.matches("-+")) return null;
            try {
                LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ofPattern(formatoData));
                if (fuso != null && !fuso.isBlank()) {
                    try {
                        ZoneId zone = fuso.equalsIgnoreCase("UTC") ? ZoneOffset.UTC : ZoneId.of(fuso);
                        ldt = ldt.atZone(zone).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                    } catch (Exception ex) { /* ignoro errori fuso */ }
                }
                return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ex) {
                try {
                    String dt = FunzioniDate.Formatta_Data_UTC(s);
                    return (dt == null || dt.isBlank()) ? null : dt;
                } catch (Exception ex2) { return null; }
            }
        }

        public String normalizzaData(String[] riga, int col) {
            return normalizzaData(safe(riga, col));
        }

        private static String safe(String[] arr, int idx) {
            if (arr == null || idx < 0 || idx >= arr.length || arr[idx] == null) return "";
            return arr[idx].trim();
        }

        // -----------------------------------------------------------------
        // Normalizzazione moneta
        // -----------------------------------------------------------------

        public String normalizzaMoneta(String moneta) {
            if (moneta == null) return "";
            String m = moneta.trim();
            if (m.isBlank()) return m;
            for (String daRimuovere : rimuoviDaNomeMoneta) m = m.replace(daRimuovere, "");
            m = m.trim();
            String rin = rinominaMonete.get(m);
            return rin != null ? rin : m;
        }

        // -----------------------------------------------------------------
        // Regola segno
        // -----------------------------------------------------------------

        public RegolaSegno regolaSegno(String causale) {
            if (causale != null) {
                if (causaliUscita.contains(causale))  return RegolaSegno.FORZATO_USCITA;
                if (causaliEntrata.contains(causale)) return RegolaSegno.FORZATO_ENTRATA;
            }
            return RegolaSegno.NESSUNA;
        }

        public enum RegolaSegno {
            NESSUNA,
            FORZATO_USCITA,
            FORZATO_ENTRATA
        }
    }
}
