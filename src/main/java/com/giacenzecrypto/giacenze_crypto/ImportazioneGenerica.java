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

    public static boolean importa(String fileCSV,
            String fileConfigurazione,
            boolean sovrascriEsistenti,
            Download progressb) {
        Importazioni.AzzeraContatori();

        //Leggo il file di configurazione e ne salvo i parametri
        ConfigurazioneImport cfg;
        try {
            cfg = ConfigurazioneImport.carica(fileConfigurazione);
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return false;
        }

        //Leggo il file csve creo la lista dei soli righi con dati validi
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
            if (progressb != null) {
                progressb.SetAvanzamento(i + 1);
                if (progressb.FineThread) {
                    return false;
                }
            }

            //La chiave del gruppo tiene conto della data e di eventuali idtransazione presenti nel file csv
            //Stessa chiave significa avere movimenti correlati o associabili es. uno scambio gestito su due righi
            String[] riga = righe.get(i);
            String chiave = calcolaChiaveGruppo(riga, cfg);
            if (chiave == null || chiave.isBlank()) {
                continue;
            }

            if (chiave.equals(ultimaChiaveGruppo)) {
                gruppoCorrente.add(riga);
            } else {
                //Se passo ad altra chiave consolido i movimenti con la stessa chiave
                if (!gruppoCorrente.isEmpty()) {
                    listaCompleta.addAll(consolidaGruppo(gruppoCorrente, cfg, movimentiDifferiti));
                }
                gruppoCorrente = new ArrayList<>();
                gruppoCorrente.add(riga);
                ultimaChiaveGruppo = chiave;
            }
        }

        //Consolido il movimento che manca
        if (!gruppoCorrente.isEmpty()) {
            listaCompleta.addAll(consolidaGruppo(gruppoCorrente, cfg, movimentiDifferiti));
        }

        //Controllo se ho dei movimenti differiti e li associo
        if (!movimentiDifferiti.isEmpty()) {
            Importazioni.ConsolidaMovimentiDifferiti(movimentiDifferiti, sovrascriEsistenti);
        }

        int[] insScart = Importazioni.ScriviListaSuMappaCrypto(listaCompleta, sovrascriEsistenti);
        Importazioni.TransazioniAggiunte = insScart[0];
        Importazioni.TrasazioniScartate = insScart[1];
        Importazioni.Transazioni = insScart[0] + insScart[1];

        if (Importazioni.TransazioniAggiunte > 0) {
            Principale.TabellaCryptodaAggiornare = true;
        }

        return true;
    }

    private static List<String[]> leggiCSV(String fileCSV, ConfigurazioneImport cfg) throws IOException {
        List<String> righeRaw = new ArrayList<>();
        List<String[]> risultato = new ArrayList<>();
        String sep = cfg.separatore;
        String encoding = cfg.encoding != null ? cfg.encoding : "UTF-8";

        //---- FASE 1 ---- Estraggo dal CSV le sole righe con i dati che interessano
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileCSV), encoding))) {
            String riga;
            int numRiga = 0;
            while ((riga = br.readLine()) != null) {
                numRiga++;
                if (riga == null) {
                    continue;
                }
                riga = riga.replace("\"", "").replaceAll("^\uFEFF", "").trim();
                if (riga.isBlank()) {
                    continue;
                }
                if (numRiga <= cfg.righeIntestazione) {
                    if (numRiga == cfg.rigaIntestazione && cfg.autoDetectColonne) {
                        cfg.risolviColonneDaIntestazione(riga.split(sep, -1));
                    }
                    continue;
                }
                if (riga.matches("^[\\-\\s;,:|]+$")) {
                    continue;
                }
                //Quello che metto in righe raw sono effettivamente le righe con i dati
                righeRaw.add(riga);
            }
        }

        //---- FASE 2 ---- 
        //Creao un array con i campi per ogni riga e scarto le righe con dati non validi
        //Aggiungo poi questi array ad una lista
        for (String r : righeRaw) {
            String[] campi = r.split(sep, -1);
            if (campi.length <= cfg.colonnaData) {
                scarta("RIGA TROPPO CORTA", r);
                continue;
            }

            String dataStr = cfg.normalizzaData(safe(campi, cfg.colonnaData));
            if (dataStr == null || dataStr.isBlank()) {
                scarta("DATA NON VALIDA", r);
                continue;
            }

            long dataLong = FunzioniDate.ConvertiDatainLongSecondo(dataStr);
            if (dataLong == 0) {
                scarta("DATA NON PARSABILE", r);
                continue;
            }
            risultato.add(campi);
        }

        //---- FASE 3 ---- Ordino la lista in base alla data
        risultato.sort((a, b) -> {
            long da = FunzioniDate.ConvertiDatainLongSecondo(cfg.normalizzaData(safe(a, cfg.colonnaData)));
            long db = FunzioniDate.ConvertiDatainLongSecondo(cfg.normalizzaData(safe(b, cfg.colonnaData)));
            return Long.compare(da, db);
        });

        return risultato;
    }

    private static String calcolaChiaveGruppo(String[] riga, ConfigurazioneImport cfg) {
        String data = cfg.normalizzaData(safe(riga, cfg.colonnaData));
        if (data == null || data.isBlank()) {
            return null;
        }
        if (cfg.colonnaIDTransazione >= 0 && cfg.colonnaIDTransazione < riga.length) {
            String id = safe(riga, cfg.colonnaIDTransazione);
            if (!id.isBlank()) {
                return id + "|" + data;
            }
        }
        return data;
    }

    private static List<String[]> consolidaGruppo(List<String[]> gruppo,
            ConfigurazioneImport cfg,
            List<String[]> differiti) {
        List<String[]> risultato = new ArrayList<>();

        if (!cfg.consolidaRigheStessaData || gruppo.size() == 1) {
            for (String[] r : gruppo) {
                String[] mov = costruisciMovimento(r, null, cfg);
                if (mov != null) {
                    risultato.add(mov);
                }
            }
            return risultato;
        }

        String[] rigaUscita = null;
        String[] rigaEntrata = null;
        List<String[]> righeCommissione = new ArrayList<>();
        
        //Questa parte è da rifare, infatti qua devo gestire solo lo scambio crypto o gli acquisti in gruppo
        //gli altri movimenti vanno gestiti singoli
        TransazioneDefi Scambio=new TransazioneDefi();
        
        for (String[] r : gruppo) {
            String segno = determinaSegno(r, cfg);
            if ("-".equals(segno)) {
                rigaUscita = r;
            } else if ("+".equals(segno)) {
                rigaEntrata = r;
            } else if ("FEE".equals(segno)) {
               // System.out.println(safe(r, cfg.colonnaQuantita));
                righeCommissione.add(r);              
            }
        }

        if (rigaUscita != null && rigaEntrata != null) {
            String[] mov = costruisciMovimentoScambio(rigaUscita, rigaEntrata, cfg);
            if (mov != null) {
                risultato.add(mov);
            }
        } else {
            for (String[] r : gruppo) {
                String[] mov = costruisciMovimento(r, null, cfg);
                if (mov != null) {
                    risultato.add(mov);
                }
            }
        }

        for (String[] rc : righeCommissione) {
            String[] mov = costruisciMovimento(rc, null, cfg);
            if (mov != null) {
                risultato.add(mov);
            }
        }

        return risultato;
    }

    private static String[] costruisciMovimento(String[] riga,
            String tipoForzato,
            ConfigurazioneImport cfg) {
        try {
            String data = cfg.normalizzaData(safe(riga, cfg.colonnaData));
            if (data == null || data.isBlank()) {
                return null;
            }
            long dataLong = FunzioniDate.ConvertiDatainLongSecondo(data);
            if (dataLong == 0) {
                return null;
            }

            String exchange = nvl(cfg.nomeExchange, "Exchange Generico");
            String wallet = nvl(cfg.nomeWallet, "Principale");

            if (cfg.colonnaWallet >= 0 && cfg.colonnaWallet < riga.length && !safe(riga, cfg.colonnaWallet).isBlank()) {
                wallet = safe(riga, cfg.colonnaWallet);
            }

            String causaleCSV = safe(riga, cfg.colonnaCausale);
            String walletOverride = cfg.walletPerCausale.get(causaleCSV);
            if (walletOverride != null && !walletOverride.isBlank()) {
                wallet = walletOverride;
            }
            String tipoMovimento = tipoForzato != null ? tipoForzato : cfg.convertiCausale(causaleCSV);

            if (tipoMovimento == null || tipoMovimento.isBlank()) {
                scarta("CAUSALE SCONOSCIUTA: " + causaleCSV, Arrays.toString(riga));
                return null;
            }
            if (tipoMovimento.equalsIgnoreCase("IGNORA") || tipoMovimento.equalsIgnoreCase("NON CONSIDERARE")) {
                return null;
            }

            String moneta = cfg.normalizzaMoneta(safe(riga, cfg.colonnaMoneta));
            String qtaStr = normalizzaNumero(safe(riga, cfg.colonnaQuantita));
            String monetaFee = cfg.normalizzaMoneta(safe(riga, cfg.colonnaMonetaFee));
            String qtaFee = normalizzaNumero(safe(riga, cfg.colonnaQuantitaFee));
            String valoreEuro = normalizzaNumero(safe(riga, cfg.colonnaValoreEuro));
            String prezzo = normalizzaNumero(safe(riga, cfg.colonnaPrezzo));
            String idTrans = safe(riga, cfg.colonnaIDTransazione);
            
          /*  System.out.println(qtaStr);
            System.out.println(cfg.colonnaSegno);*/

            if (cfg.colonnaSegno >= 0) {
                String segno = safe(riga, cfg.colonnaSegno);
                if ("-".equals(segno) && !qtaStr.startsWith("-")) {
                    qtaStr = "-" + qtaStr;
                }
            }

            Moneta mOUT = null;
            Moneta mIN = null;
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

            RegolaSegno regola = cfg.regolaSegno(causaleCSV);
            if (regola == RegolaSegno.FORZATO_USCITA && mIN != null && mOUT == null) {
                mOUT = new Moneta();
                mOUT.InserisciValori(moneta, mIN.Qta, "", "");
                mOUT.AssegnaTipoAuto();
                mIN = null;
            } else if (regola == RegolaSegno.FORZATO_ENTRATA && mOUT != null && mIN == null) {
                mIN = new Moneta();
               // mOUT.Qta=
                mIN.InserisciValori(moneta, mOUT.Qta, "", "");
                mIN.AssegnaTipoAuto();
                mOUT = null;
            }

            String prezzoMov = !valoreEuro.isBlank() ? valoreEuro : prezzo;
            
           // System.out.println(mOUT.Qta+" : "+mOUT.Moneta);

            String[] rt = MovimentiCrypto.creaMovimento(
                    mOUT,
                    mIN,
                    exchange,
                    wallet,
                    dataLong,
                    prezzoMov,
                    "CSV",
                    1,
                    1,
                    null,
                    null,
                    "A",
                    idTrans,
                    tipoMovimento,
                    exchange + "." + idTrans
            );

            if (rt == null) {
                return null;
            }

            if (!monetaFee.isBlank() && !qtaFee.isBlank() && rt.length > 12) {
                rt[11] = monetaFee;
                rt[12] = qtaFee;
            }
            if (rt.length > 7) {
                rt[7] = causaleCSV;
            }
            if (rt.length > 39) {
                rt[39] = "D";
            }

            for (Map.Entry<String, Integer> e : cfg.campiExtra.entrySet()) {
                try {
                    int campoMov = Integer.parseInt(e.getKey());
                    int colCsv = e.getValue();
                    if (campoMov >= 0 && campoMov < rt.length) {
                        rt[campoMov] = safe(riga, colCsv);
                    }
                } catch (Exception ex) {
                    LoggerGC.ScriviErrore(ex);
                }
            }

            return rt;
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return null;
        }
    }

    private static String[] costruisciMovimentoScambio(String[] rigaOUT,
            String[] rigaIN,
            ConfigurazioneImport cfg) {
        try {
            String data = cfg.normalizzaData(safe(rigaOUT, cfg.colonnaData));
            if (data == null || data.isBlank()) {
                return null;
            }
            long dataLong = FunzioniDate.ConvertiDatainLongSecondo(data);
            if (dataLong == 0) {
                return null;
            }

            String monetaOUT = cfg.normalizzaMoneta(safe(rigaOUT, cfg.colonnaMoneta));
            String qtaOUT = normalizzaNumero(safe(rigaOUT, cfg.colonnaQuantita));
            String monetaIN = cfg.normalizzaMoneta(safe(rigaIN, cfg.colonnaMoneta));
            String qtaIN = normalizzaNumero(safe(rigaIN, cfg.colonnaQuantita));
            String idTrans = safe(rigaOUT, cfg.colonnaIDTransazione);
            String valoreEuro = normalizzaNumero(safe(rigaOUT, cfg.colonnaValoreEuro));

            Moneta mOUT = new Moneta();
            mOUT.InserisciValori(monetaOUT, qtaOUT, "", "");
            mOUT.AssegnaTipoAuto();

            Moneta mIN = new Moneta();
            mIN.InserisciValori(monetaIN, qtaIN, "", "");
            mIN.AssegnaTipoAuto();

            String[] rt = MovimentiCrypto.creaMovimento(
                    mOUT,
                    mIN,
                    nvl(cfg.nomeExchange, "Exchange Generico"),
                    nvl(cfg.nomeWallet, "Principale"),
                    dataLong,
                    valoreEuro,
                    "CSV",
                    1,
                    1,
                    null,
                    null,
                    "A",
                    idTrans,
                    "SCAMBIO CRYPTO-CRYPTO",
                    nvl(cfg.nomeExchange, "Exchange Generico") + "." + idTrans
            );

            if (rt == null) {
                return null;
            }
            if (rt.length > 39) {
                rt[39] = "D";
            }
            return rt;
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return null;
        }
    }

    private static String determinaSegno(String[] riga, ConfigurazioneImport cfg) {
        String causale = safe(riga, cfg.colonnaCausale);
       // System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA "+causale);
        String tipo = cfg.convertiCausale(causale);
        //System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB "+tipo);
        if (tipo != null && tipo.toUpperCase().contains("COMMISSIONI")) {
            return "FEE";
        }
        
        if (cfg.colonnaSegno >= 0 && cfg.colonnaSegno < riga.length) {
            String s = safe(riga, cfg.colonnaSegno);
            if ("-".equals(s)) {
                return "-";
            }
            if ("+".equals(s)) {
                return "+";
            }
        }

        if (cfg.colonnaQuantita >= 0 && cfg.colonnaQuantita < riga.length) {
            String q = normalizzaNumero(safe(riga, cfg.colonnaQuantita));
            if (q.startsWith("-")) {
                return "-";
            }
            if (!q.isBlank() && Funzioni.isNumeric(q, false)) {
                return "+";
            }
        }

       // String causale = safe(riga, cfg.colonnaCausale);
       // System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA "+causale);
        //String tipo = cfg.convertiCausale(causale);
        
        return "+";
    }

    private static String safe(String[] arr, int idx) {
        if (arr == null || idx < 0 || idx >= arr.length || arr[idx] == null) {
            return "";
        }
        return arr[idx].trim();
    }

    private static String normalizzaNumero(String s) {
        if (s == null) {
            return "";
        }
        s = s.trim();
        if (s.isBlank()) {
            return "";
        }
        return s.replace(" ", "").replace(",", ".");
    }

    private static String nvl(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    private static void scarta(String motivo, String riga) {
        Importazioni.movimentiSconosciuti += motivo + ": " + riga + "\n";
        Importazioni.TrasazioniSconosciute++;
    }

    public static class ConfigurazioneImport {

        public String nomeExchange = "Exchange Generico";
        public String nomeWallet = "Principale";
        public String separatore = ",";
        public String encoding = "UTF-8";
        public int righeIntestazione = 1;
        public int rigaIntestazione = 1;
        public boolean autoDetectColonne = false;
        public String formatoData = "yyyy-MM-dd HH:mm:ss";
        public String fuso = "UTC";
        public int colonnaData = 0;
        public int colonnaMoneta = 1;
        public int colonnaQuantita = 2;
        public int colonnaSegno = -1;
        public int colonnaCausale = 3;
        public int colonnaValoreEuro = -1;
        public int colonnaPrezzo = -1;
        public int colonnaMonetaFee = -1;
        public int colonnaQuantitaFee = -1;
        public int colonnaIDTransazione = -1;
        public int colonnaWallet = -1;
        public boolean consolidaRigheStessaData = false;
        public Map<String, String> mappaCausali = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        public Set<String> causaliUscita = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        public Set<String> causaliEntrata = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        public Set<String> causaliAperte = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        public Map<String, Integer> mappaNomiColonne = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        public Map<String, String> rinominaMonete = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        public List<String> rimuoviDaNomeMoneta = new ArrayList<>();
        public Map<String, Integer> campiExtra = new TreeMap<>();
        public Map<String, String> walletPerCausale = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        public static ConfigurazioneImport carica(String percorso) throws Exception {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(percorso))) {
                String riga;
                while ((riga = br.readLine()) != null) {
                    sb.append(riga);
                }
            }

            JSONObject root = new JSONObject(sb.toString());
            ConfigurazioneImport cfg = new ConfigurazioneImport();

            if (root.has("nomeExchange")) cfg.nomeExchange = root.getString("nomeExchange");
            if (root.has("nomeWallet")) cfg.nomeWallet = root.getString("nomeWallet");
            if (root.has("separatore")) cfg.separatore = root.getString("separatore");
            if (root.has("encoding")) cfg.encoding = root.getString("encoding");
            if (root.has("righeIntestazione")) cfg.righeIntestazione = root.getInt("righeIntestazione");
            if (root.has("rigaIntestazione")) cfg.rigaIntestazione = root.getInt("rigaIntestazione");
            if (root.has("autoDetectColonne")) cfg.autoDetectColonne = root.getBoolean("autoDetectColonne");
            if (root.has("formatoData")) cfg.formatoData = root.getString("formatoData");
            if (root.has("fuso")) cfg.fuso = root.getString("fuso");

            if (root.has("colonne")) {
                JSONObject col = root.getJSONObject("colonne");
                if (col.has("data")) cfg.colonnaData = col.getInt("data");
                if (col.has("moneta")) cfg.colonnaMoneta = col.getInt("moneta");
                if (col.has("quantita")) cfg.colonnaQuantita = col.getInt("quantita");
                if (col.has("segno")) cfg.colonnaSegno = col.getInt("segno");
                if (col.has("causale")) cfg.colonnaCausale = col.getInt("causale");
                if (col.has("valoreEuro")) cfg.colonnaValoreEuro = col.getInt("valoreEuro");
                if (col.has("prezzo")) cfg.colonnaPrezzo = col.getInt("prezzo");
                if (col.has("monetaFee")) cfg.colonnaMonetaFee = col.getInt("monetaFee");
                if (col.has("quantitaFee")) cfg.colonnaQuantitaFee = col.getInt("quantitaFee");
                if (col.has("idTransazione")) cfg.colonnaIDTransazione = col.getInt("idTransazione");
                if (col.has("wallet")) cfg.colonnaWallet = col.getInt("wallet");
            }

            if (root.has("consolidaRigheStessaData")) cfg.consolidaRigheStessaData = root.getBoolean("consolidaRigheStessaData");

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
            
            if (root.has("causaliAperte")) {
                JSONArray arr = root.getJSONArray("causaliAperte");
                for (int i = 0; i < arr.length(); i++) cfg.causaliAperte.add(arr.getString(i));
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

        public void risolviColonneDaIntestazione(String[] intestazione) {
            for (int i = 0; i < intestazione.length; i++) {
                mappaNomiColonne.put(intestazione[i].trim(), i);
            }
        }

        public String convertiCausale(String causaleCSV) {
            if (causaleCSV == null) {
                return null;
            }
            String r = mappaCausali.get(causaleCSV.trim());
            if (r != null) {
                return r;
            }
            for (Map.Entry<String, String> e : mappaCausali.entrySet()) {
                if (causaleCSV.toLowerCase().contains(e.getKey().toLowerCase())) {
                    return e.getValue();
                }
            }
            return null;
        }

        public String normalizzaData(String dataCSV) {
            if (dataCSV == null) {
                return null;
            }
            String s = dataCSV.trim();
            if (s.isBlank() || s.matches("[\\-\\s:/]+")) {
                return null;
            }

            try {
                LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ofPattern(formatoData));
                if (fuso != null && !fuso.isBlank()) {
                    try {
                        ZoneId zone = fuso.equalsIgnoreCase("UTC") ? ZoneOffset.UTC : ZoneId.of(fuso);
                        ldt = ldt.atZone(zone).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                    } catch (Exception ex) {
                    }
                }
                return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ex) {
                try {
                    String dt = FunzioniDate.Formatta_Data_UTC(s);
                    return (dt == null || dt.isBlank()) ? null : dt;
                } catch (Exception ex2) {
                    return null;
                }
            }
        }

        public String normalizzaMoneta(String moneta) {
            String m = moneta == null ? "" : moneta.trim();
            if (m.isBlank()) {
                return m;
            }
            for (String daRimuovere : rimuoviDaNomeMoneta) {
                m = m.replace(daRimuovere, "");
            }
            m = m.trim();
            String rin = rinominaMonete.get(m);
            return rin != null ? rin : m;
        }

        public RegolaSegno regolaSegno(String causale) {
            if (causale != null) {
                if (causaliUscita.contains(causale)) return RegolaSegno.FORZATO_USCITA;
                if (causaliEntrata.contains(causale)) return RegolaSegno.FORZATO_ENTRATA;
            }
            return RegolaSegno.NESSUNA;
        }
    }

    public enum RegolaSegno {
        NESSUNA, FORZATO_USCITA, FORZATO_ENTRATA
    }
}
