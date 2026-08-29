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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

/**
 * Importatore CSV generico guidato da configurazione JSON.
 *
 * <p>
 * Questa classe permette di leggere file CSV provenienti da exchange o wallet
 * eterogenei senza dover creare ogni volta una nuova classe di importazione
 * dedicata. Il comportamento dell'import viene descritto tramite un file JSON
 * che definisce struttura del CSV, mappatura delle causali, regole sul segno,
 * eventuali rinominhe delle monete e criteri di consolidamento delle righe.</p>
 *
 * <p>
 * Il flusso generale è il seguente:</p>
 * <ol>
 * <li>Lettura della configurazione JSON.</li>
 * <li>Lettura e validazione del CSV.</li>
 * <li>Ordinamento cronologico delle righe valide.</li>
 * <li>Raggruppamento per ID transazione e vicinanza temporale.</li>
 * <li>Consolidamento del gruppo in uno o più movimenti interni compatibili con
 * il formato usato da {@code MovimentiCrypto}.</li>
 * <li>Scrittura finale dei movimenti nella struttura applicativa.</li>
 * </ol>
 *
 * <p>
 * La logica di raggruppamento supporta una tolleranza temporale parametrica:
 * tale tolleranza può essere applicata a tutte le causali oppure solo ad un
 * sottoinsieme specificato in {@code causaliDifferite}. Questo consente di
 * gestire CSV in cui righe appartenenti allo stesso scambio arrivano con
 * timestamp leggermente diversi.</p>
 */
public class ImportazioneGenerica {

    
    public static boolean importa(String fileCSV, String fileConfigurazione,
        boolean sovrascriEsistenti, Download progressb) {
    return importa(fileCSV, fileConfigurazione, sovrascriEsistenti, progressb, null, null);
}

    public static boolean importa(String fileCSV, String fileConfigurazione,
        boolean sovrascriEsistenti, Download progressb, String nomeExchangeOverride) {
    return importa(fileCSV, fileConfigurazione, sovrascriEsistenti, progressb, nomeExchangeOverride, null);
}

    /**
     * Cerca nel nome del file pattern di fuso orario come UTC, UTC+1, UTC+2, CET.
     * Restituisce il pattern trovato (es. "UTC+1", "CET") oppure null.
     */
    public static String estraiTZdaNomeFile(String nomeFile) {
        if (nomeFile == null || nomeFile.isBlank()) return null;
        Pattern p = Pattern.compile("(?i)\\b(UTC[+-]\\d{1,2}(?::\\d{2})?|UTC|CET)\\b");
        //Pattern p = Pattern.compile("\\(UTC([+-]?\\d*)\\)");
        Matcher m = p.matcher(nomeFile);
        if (m.find()) {
            return m.group(1).toUpperCase();
        }
        return null;
    }
    
    
    
    /**
     * Importa un file CSV utilizzando una configurazione JSON esterna.
     *
     * <p>
     * Il metodo esegue l'intero flusso di importazione:</p>
     * <ol>
     * <li>azzera i contatori globali di importazione;</li>
     * <li>carica il file di configurazione JSON;</li>
     * <li>legge e valida le righe del CSV;</li>
     * <li>raggruppa le righe per ID transazione;</li>
     * <li>applica, se previsto, una tolleranza temporale tra righe dello stesso
     * gruppo;</li>
     * <li>consolida ogni gruppo in movimenti finali;</li>
     * <li>scrive i movimenti nel formato atteso dall'applicazione.</li>
     * </ol>
     *
     * <p>
     * La tolleranza temporale è espressa in secondi nel JSON tramite
     * {@code tolleranzaSecondiConsolidamento}. Se {@code causaliDifferite} è
     * vuoto, la tolleranza viene applicata a tutte le causali; se invece
     * contiene valori, la tolleranza viene applicata solo ai gruppi che
     * coinvolgono almeno una di quelle causali. Negli altri casi il
     * consolidamento richiede timestamp identico.</p>
     *
     * <p>
     * Il metodo aggiorna anche gli indicatori di avanzamento e i contatori
     * globali usati dall'applicazione.</p>
     *
     * @param fileCSV percorso del file CSV da importare
     * @param fileConfigurazione percorso del file JSON contenente le regole di
     * importazione
     * @param sovrascriEsistenti se {@code true}, consente la sovrascrittura dei
     * movimenti già presenti
     * @param progressb oggetto opzionale per il reporting dell'avanzamento; può
     * essere {@code null}
     * @param nomeExchangeOverride
     * @return {@code true} se l'importazione termina correttamente,
     * {@code false} in caso di errore
     */
    public static boolean importa(String fileCSV, String fileConfigurazione,
            boolean sovrascriEsistenti, Download progressb, String nomeExchangeOverride, String fusoOverride) {

        Importazioni.AzzeraContatori();

        ConfigurazioneImport cfg;
        try {
            cfg = ConfigurazioneImport.carica(fileConfigurazione);
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return false;
        }

        if (nomeExchangeOverride != null && !nomeExchangeOverride.isBlank()) {
            cfg.nomeExchange = nomeExchangeOverride;
        }
        if (fusoOverride != null && !fusoOverride.isBlank()) {
            cfg.fuso = fusoOverride;
        }
        
        List<String[]> righe;
        try {
            righe = leggiCSV(fileCSV, cfg);
  /*          if (righe.isEmpty()) {
    Importazioni.movimentiSconosciuti +=
        "=== NESSUNA RIGA VALIDA LETTA DAL CSV ===\n" +
        "Verificare: separatore='" + cfg.separatore + "', " +
        "encoding='" + cfg.encoding + "', " +
        "righeIntestazione=" + cfg.righeIntestazione + "\n";
    return true; // torna true per mostrare il resoconto
}*/
      /*      Importazioni.movimentiSconosciuti +=
    "=== RIGHE LETTE: " + righe.size() + " ===\n";*/
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return false;
        }

        // Pre-consolidamento per (giorno, moneta) delle causali indicate (micro-interessi Bitget & c.)
        righe = consolidaCausaliPerGiorno(righe, cfg);

        if (progressb != null) {
            progressb.SetMassimo(righe.size());
            progressb.SetAvanzamento(0);
        }

        List<String[]> listaCompleta = new ArrayList<>();
        List<String[]> movimentiDifferiti = new ArrayList<>();

        int righeFatte = 0;
        for (List<String[]> gruppo : raggruppaRighe(righe, cfg)) {
            if (progressb != null && progressb.FineThread) {
                return false;
            }
            listaCompleta.addAll(consolidaGruppo(gruppo, cfg, movimentiDifferiti));
            righeFatte += gruppo.size();
            if (progressb != null) {
                progressb.SetAvanzamento(righeFatte);
            }
        }

        if (!movimentiDifferiti.isEmpty()) {

            Importazioni.ConsolidaMovimentiDifferiti(movimentiDifferiti, sovrascriEsistenti);
        }
        
       /* Importazioni.movimentiSconosciuti +=
    "=== MOVIMENTI DA SCRIVERE: " + listaCompleta.size() + " ===\n";*/
        
        int[] insScart = Importazioni.ScriviListaSuMappaCrypto(listaCompleta, sovrascriEsistenti);
        Importazioni.TransazioniAggiunte = insScart[0];
        Importazioni.TrasazioniScartate = insScart[1];
        Importazioni.Transazioni = insScart[0] + insScart[1];

        if (Importazioni.TransazioniAggiunte > 0) {
            Principale.TabellaCryptodaAggiornare = true;
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // LETTURA CSV
    // -------------------------------------------------------------------------
    /**
     * Legge il file CSV, ne filtra le righe utili e restituisce una lista di
     * record validati.
     *
     * <p>
     * La lettura avviene in tre fasi:</p>
     * <ol>
     * <li><b>Estrazione righe utili</b>: rimuove righe vuote, BOM, intestazioni
     * e separatori fittizi.</li>
     * <li><b>Validazione</b>: effettua lo split sui campi, verifica la presenza
     * della data e controlla che la data sia interpretabile.</li>
     * <li><b>Ordinamento</b>: ordina cronologicamente le righe valide in modo
     * da rendere deterministico il successivo raggruppamento.</li>
     * </ol>
     *
     * <p>
     * Le righe non valide vengono scartate e registrate nei contatori di
     * scarto.</p>
     *
     * @param fileCSV percorso del file CSV sorgente
     * @param cfg configurazione di importazione da applicare
     * @return lista delle righe valide già splittate in array di colonne
     * @throws IOException se si verifica un errore di lettura del file
     */
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
                if (riga == null) {
                    continue;
                }
                riga = riga.replace("\"", "").replaceAll("\uFEFF", "").trim();
                if (riga.isBlank()) {
                    continue;
                }
                if (numRiga <= cfg.righeIntestazione) {
                    if (numRiga == cfg.rigaIntestazione && cfg.autoDetectColonne) {
                        cfg.risolviColonneDaIntestazione(riga.split(sep, -1));
                    }
                    continue;
                }
                if (riga.matches("[-,]+")) {
                    continue;
                }
                righeRaw.add(riga);
            }
        }

        // FASE 2 – splitto e valido
        for (String r : righeRaw) {
            String[] campi = r.split(sep, -1);
            if (campi.length <= cfg.colonnaData) {
                scarta("RIGA TROPPO CORTA", r);
                continue;
            }
            String dataStr = safe(campi, cfg.colonnaData);
            String dataNorm = cfg.normalizzaData(dataStr);
            if (dataStr == null || dataStr.isBlank()) {
                scarta("DATA NON VALIDA", r);
                continue;
            }
            long dataLong = cfg.convertiDataInMillis(dataStr);
            if (dataLong <= 0) {
                scarta("DATA NON PARSABILE", r);
                continue;
            }
            risultato.add(campi);
        }

        // FASE 3 – ordino per data
        //A parità di data si ordina anche per colonna di raggruppamento, quando ne è configurata una
        //diversa dall'identificativo: il raggruppamento a valle è su righe CONSECUTIVE, e negli export le
        //gambe di ordini diversi allo stesso secondo sono interlacciate (verificato sull'export OKX del
        //23/07/2026: 16 righe di 8 ordini distinti, in ordine sparso). Senza questo pareggio ogni ordine
        //verrebbe spezzato. Non si tocca l'ordinamento degli altri formati, dove la colonna non c'è.
        int colonnaGruppo = cfg.colonnaIDGruppo;
        int colonnaID = cfg.colonnaIDTransazione;
        risultato.sort((a, b) -> {
            long da = cfg.convertiDataInMillis(safe(a, cfg.colonnaData));
            long db = cfg.convertiDataInMillis(safe(b, cfg.colonnaData));
            int cmp = Long.compare(da, db);
            if (cmp != 0 || colonnaGruppo < 0) return cmp;
            cmp = safe(a, colonnaGruppo).compareTo(safe(b, colonnaGruppo));
            if (cmp != 0 || colonnaID < 0) return cmp;
            //Terzo criterio, l'identificativo di riga: dentro un ordine eseguito in piu' parti decide quale
            //gamba entra per prima nell'accumulatore, e quindi quale identificativo finisce nel movimento
            //consolidato. L'import via API ordina gia' per (data, billId), quindi senza questo pareggio le due
            //strade potrebbero scegliere gambe diverse e produrre due [24] diversi per lo stesso scambio.
            return safe(a, colonnaID).compareTo(safe(b, colonnaID));
        });

        return risultato;
    }

    /**
 * Legge solo il campo "nomeExchange" dal JSON di configurazione.
 * Restituisce null se il campo è assente o vuoto.
 */
public static String leggiNomeExchangeDaJson(String percorsoJson) {
    try (BufferedReader br = new BufferedReader(new FileReader(percorsoJson))) {
        StringBuilder sb = new StringBuilder();
        String riga;
        while ((riga = br.readLine()) != null) sb.append(riga);
        JSONObject root = new JSONObject(sb.toString());
        if (root.has("nomeExchange")) {
            String nome = root.getString("nomeExchange").trim();
            if (!nome.isBlank()) return nome;
        }
    } catch (Exception ex) {
        LoggerGC.ScriviErrore(ex);
    }
    return null; // assente o vuoto → l'utente dovrà scegliere
}

    /**
     * Somma per {@code (giorno, moneta)} le righe le cui causali CSV sono elencate in
     * {@code cfg.causaliConsolidaPerGiorno}, restituendo una sola riga sintetica per bucket (data e
     * altre colonne dalla prima riga del giorno, quantità sostituita dalla somma). Le righe non
     * interessate passano invariate. Il risultato è riordinato per data. Se la lista di causali è
     * vuota la lista in ingresso è restituita così com'è.
     *
     * <p>Il giorno è quello di {@code normalizzaData}, cioè nel fuso di sistema: lo stesso file
     * consolidato su macchine con fuso diverso può spostare un accredito a cavallo di mezzanotte in
     * un giorno diverso. Accettabile per micro-interessi; irrilevante lontano dalla mezzanotte.</p>
     */
    static List<String[]> consolidaCausaliPerGiorno(List<String[]> righe, ConfigurazioneImport cfg) {
        if (cfg.causaliConsolidaPerGiorno.isEmpty()) return righe;

        List<String[]> out = new ArrayList<>();
        java.util.LinkedHashMap<String, String[]> template = new java.util.LinkedHashMap<>();
        java.util.HashMap<String, BigDecimal> somma = new java.util.HashMap<>();

        for (String[] riga : righe) {
            String causaleCSV = cfg.getCausaleCSV(riga);
            if (!cfg.causaliConsolidaPerGiorno.contains(causaleCSV)) {
                out.add(riga);
                continue;
            }
            String dataNorm = cfg.normalizzaData(safe(riga, cfg.colonnaData));
            String qtaStr = normalizzaNumero(safe(riga, cfg.colonnaQuantita));
            if (dataNorm == null || dataNorm.length() < 10
                    || qtaStr.isBlank() || !Funzioni.isNumeric(qtaStr, false)) {
                out.add(riga); // non consolidabile: lasciata singola
                continue;
            }
            String chiave = dataNorm.substring(0, 10) + "|" + cfg.normalizzaMoneta(safe(riga, cfg.colonnaMoneta));
            BigDecimal acc = somma.get(chiave);
            if (acc == null) {
                somma.put(chiave, new BigDecimal(qtaStr));
                template.put(chiave, riga.clone());
            } else {
                somma.put(chiave, acc.add(new BigDecimal(qtaStr)));
            }
        }

        for (java.util.Map.Entry<String, String[]> e : template.entrySet()) {
            String[] r = e.getValue();
            if (cfg.colonnaQuantita >= 0 && cfg.colonnaQuantita < r.length) {
                r[cfg.colonnaQuantita] = somma.get(e.getKey()).stripTrailingZeros().toPlainString();
            }
            // La riga sintetica somma le quantità del giorno ma eredita le altre colonne dalla prima
            // riga: la fee di quella riga sarebbe attribuita all'intero accredito giornaliero, quindi
            // la azzero.
            if (cfg.colonnaQuantitaFee >= 0 && cfg.colonnaQuantitaFee < r.length) {
                r[cfg.colonnaQuantitaFee] = "0";
            }
            out.add(r);
        }

        out.sort((a, b) -> Long.compare(
                cfg.convertiDataInMillis(safe(a, cfg.colonnaData)),
                cfg.convertiDataInMillis(safe(b, cfg.colonnaData))));
        return out;
    }

    /**
     * Raggruppa le righe <b>già ordinate per data</b> in blocchi di righe correlate (le gambe di uno
     * stesso movimento). Due righe consecutive finiscono nello stesso gruppo quando:
     * <ul>
     *   <li>condividono il valore della colonna di raggruppamento
     *       ({@link ConfigurazioneImport#colonnaRaggruppamento(String)}, che
     *       {@code raggruppamentoPerCausale} può sovrascrivere per singola causale — es. Coinbase
     *       {@code Convert} raggruppa sulla colonna Notes invece che sul timestamp), e</li>
     *   <li>se {@code consolidaRigheStessaData} è attivo, cadono entro
     *       {@code tolleranzaSecondiConsolidamento} l'una dall'altra; la finestra &gt; 0 vale solo per
     *       le causali in {@code causaliDifferite} (o per tutte, se la lista è vuota).</li>
     * </ul>
     * Il raggruppamento è su righe <b>consecutive</b>: le gambe di un movimento devono restare
     * adiacenti nello stream ordinato. Una gamba con chiave diversa apre sempre un gruppo nuovo, quindi
     * movimenti distinti non si mescolano anche quando sono vicini nel tempo.
     */
    static List<List<String[]>> raggruppaRighe(List<String[]> righe, ConfigurazioneImport cfg) {
        List<List<String[]>> gruppi = new ArrayList<>();
        List<String[]> gruppoCorrente = new ArrayList<>();
        String idGruppoCorrente = null;
        long tsUltimaRigaGruppo = -1;

        for (String[] riga : righe) {
            String causaleCorrente = cfg.getCausaleCSV(riga);
            int colonnaGruppo = cfg.colonnaRaggruppamento(causaleCorrente);
            boolean usaIDTransazione = colonnaGruppo >= 0;
            String idCorrente = usaIDTransazione ? safe(riga, colonnaGruppo) : "";
            long tsCorrente = cfg.convertiDataInMillis(safe(riga, cfg.colonnaData));

            if (gruppoCorrente.isEmpty()) {
                gruppoCorrente.add(riga);
                idGruppoCorrente = idCorrente;
                tsUltimaRigaGruppo = tsCorrente;
                continue;
            }

            String causaleUltima = cfg.getCausaleCSV(gruppoCorrente.get(gruppoCorrente.size() - 1));
            boolean usaDifferitaGlobale = cfg.causaliDifferite.isEmpty();
            boolean coinvolgeDifferita = usaDifferitaGlobale
                    || cfg.causaliDifferite.contains(causaleCorrente)
                    || cfg.causaliDifferite.contains(causaleUltima);
            long tolleranzaMs = coinvolgeDifferita ? cfg.tolleranzaSecondiConsolidamento * 1000L : 0L;

            boolean stessoID = !usaIDTransazione
                    || (!idCorrente.isBlank() && idCorrente.equals(idGruppoCorrente));
            boolean entroTolleranza = cfg.consolidaRigheStessaData
                    && Math.abs(tsCorrente - tsUltimaRigaGruppo) <= tolleranzaMs;

            if (stessoID && entroTolleranza) {
                gruppoCorrente.add(riga);
                tsUltimaRigaGruppo = tsCorrente;
            } else {
                gruppi.add(gruppoCorrente);
                gruppoCorrente = new ArrayList<>();
                gruppoCorrente.add(riga);
                idGruppoCorrente = idCorrente;
                tsUltimaRigaGruppo = tsCorrente;
            }
        }
        if (!gruppoCorrente.isEmpty()) gruppi.add(gruppoCorrente);
        return gruppi;
    }

    // -------------------------------------------------------------------------
    // PUNTO 2 – consolidaGruppo RISCRITTO
    // -------------------------------------------------------------------------
    /**
     * Consolida un gruppo di righe CSV correlate in uno o più movimenti finali.
     *
     * <p>
     * Il gruppo contiene righe che condividono lo stesso ID transazione e che,
     * in base alla configurazione, sono state considerate temporalmente
     * compatibili.</p>
     *
     * <p>
     * La logica di consolidamento è la seguente:</p>
     * <ul>
     * <li>se il gruppo contiene una sola riga, il movimento viene costruito
     * direttamente;</li>
     * <li>se il gruppo contiene più righe, ogni riga viene analizzata in base
     * alla causale;</li>
     * <li>le causali mappate come {@code IGNORA} o {@code NON CONSIDERARE}
     * vengono saltate;</li>
     * <li>le causali presenti in {@code causaliChiuse} vengono trattate come
     * movimenti singoli, anche se fanno parte di un gruppo multi-riga;</li>
     * <li>le altre righe vengono accumulate in una {@code TransazioneDefi} e
     * convertite in movimenti finali tramite la logica già esistente
     * dell'applicazione.</li>
     * </ul>
     *
     * <p>
     * Questa impostazione consente di gestire in modo uniforme casi misti, dove
     * nello stesso gruppo possono coesistere righe da trattare singolarmente e
     * righe che devono invece essere consolidate come scambio o movimento
     * composto.</p>
     *
     * @param gruppo insieme di righe CSV già raggruppate logicamente
     * @param cfg configurazione attiva dell'importazione
     * @param differiti lista dei movimenti differiti da consolidare
     * successivamente
     * @return lista dei movimenti finali costruiti a partire dal gruppo
     */
    static List<String[]> consolidaGruppo(List<String[]> gruppo,
            ConfigurazioneImport cfg, List<String[]> differiti) {

        List<String[]> risultato = new ArrayList<>();

        // Caso 1: movimento singolo – consolido direttamente
        if (gruppo.size() == 1) {
            List<String[]> movs = costruisciMovimenti(gruppo.get(0), null, cfg);
            if (movs != null) risultato.addAll(movs);
            return risultato;
        }

        // Caso multi-riga: per ogni riga verifico se la causale è in movimentoChiuso
        // (gestione singola) oppure no (gestione TransazioneDefi come in Importazioni.java)
        TransazioneDefi scambio = new TransazioneDefi();

        // Tengo traccia della data e del wallet per la chiamata finale a RitornaScambi
        String dataDiGruppo = null;
        String dataRawDiGruppo = null;
        String walletPrincipale = null;
        String walletID = null;
        String noteDiGruppo = "";
        boolean haMovimentiDefi = false;

        // Commissioni incontrate nel gruppo, emesse in coda: {moneta, qta assoluta, causale CSV, id originale}
        List<String[]> feeDaEmettere = new ArrayList<>();

        for (String[] riga : gruppo) {
            String causaleCSV = cfg.getCausaleCSV(riga);
            String tipoMovimento = cfg.tipoMovimentoPerRiga(riga);

            if (tipoMovimento == null || tipoMovimento.isBlank()) {
                scarta("CAUSALE SCONOSCIUTA: " + causaleCSV, Arrays.toString(riga));
                continue;
            }

            if (tipoMovimento.equalsIgnoreCase("IGNORA")
                    || tipoMovimento.equalsIgnoreCase("NON CONSIDERARE")) {
                continue;
            }

            if (dataDiGruppo == null) {
                dataRawDiGruppo = safe(riga, cfg.colonnaData);
                dataDiGruppo = cfg.normalizzaData(dataRawDiGruppo);

                walletPrincipale = cfg.nomeWallet != null ? cfg.nomeWallet : "Principale";
                String overrideWallet = cfg.walletPerCausale.get(causaleCSV);
                if (overrideWallet != null && !overrideWallet.isBlank()) {
                    walletPrincipale = overrideWallet;
                }

                String exchange = cfg.nomeExchange != null ? cfg.nomeExchange : "Exchange Generico";
                walletID = exchange + "." + safe(riga, cfg.colonnaIDTransazione);

                if (cfg.colonnaNote >= 0) noteDiGruppo = safe(riga, cfg.colonnaNote);
            }

            // Se la causale è in movimentoChiuso => tratto come movimento singolo
            /*if (cfg.causaliChiuse.contains(causaleCSV) ||
                (tipoMovimento != null && cfg.causaliChiuse.contains(tipoMovimento))) {*/
            if (cfg.causaliChiuse.contains(tipoMovimento)) {
                List<String[]> movs = costruisciMovimenti(riga, null, cfg);
                if (movs != null) risultato.addAll(movs);
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
                if (!qtaStr.startsWith("-")) {
                    qtaStr = "-" + qtaStr;
                }
            } else if (regola == ConfigurazioneImport.RegolaSegno.FORZATO_ENTRATA) {
                qtaStr = qtaStr.replace("-", "");
            }

            if (qtaStr.isBlank() || !Funzioni.isNumeric(qtaStr, false)) {
                continue;
            }

            Moneta mon = new Moneta();
            mon.InserisciValori(moneta, qtaStr, "", "");
            mon.AssegnaTipoAuto();

            // Commissioni della riga: vanno scorporate dalla quantità, non lasciate dentro.
            // Se il CSV riporta la quantità già al netto della fee (tipico degli export che danno la
            // variazione di saldo, come il Trading di OKX), importarla così com'è equivarrebbe a dedurre
            // la commissione dal costo di carico, cosa che per le cripto-attività l'art. 68 c.9-bis del
            // TUIR non consente. Si ricostruisce quindi il lordo e la fee diventa un movimento a sé.
            // La ricostruzione va fatta PRIMA della ricerca prezzo, che lavora sulla quantità.
            String monetaFee = cfg.normalizzaMoneta(safe(riga, cfg.colonnaMonetaFee));
            String qtaFee = normalizzaNumero(safe(riga, cfg.colonnaQuantitaFee));
            boolean haFee = !qtaFee.isBlank()
                    && !monetaFee.isBlank()
                    && Funzioni.isNumeric(qtaFee, false)
                    && new BigDecimal(qtaFee).compareTo(BigDecimal.ZERO) != 0;

            if (haFee) {
                BigDecimal qtaFeeBD = new BigDecimal(qtaFee).abs();
                ricostruisciLordoConFee(mon, monetaFee, qtaFeeBD, cfg);
                feeDaEmettere.add(new String[]{monetaFee, qtaFeeBD.stripTrailingZeros().toPlainString(),
                    causaleCSV, idOrig});
            }

            // Recupero prezzo se disponibile. Per le causali con controvalore "al netto della sola
            // commissione" (Coinbase 'Convert') il costo di carico della gamba e' Total - commissione,
            // non il Subtotal: lo spread resta nel costo di carico, la commissione (solo in euro, le
            // monete sono gia' nette) no. Nessun movimento COMMISSIONI separato per queste.
            String controvaloreNettoLeg = controvaloreAlNetto(riga, cfg);
            String valEuro = controvaloreNettoLeg != null ? controvaloreNettoLeg
                    : normalizzaNumero(safe(riga, cfg.colonnaValoreEuro));
            String prezzoUn = normalizzaNumero(safe(riga, cfg.colonnaPrezzo));
            long dataLong = cfg.convertiDataInMillis(dataRawDiGruppo);

            Prezzi.InfoPrezzo ip = null;
            if (valEuro.isBlank() && prezzoUn.isBlank()) {
                ip = Prezzi.DammiPrezzoInfoTransazione(mon, null, dataLong, null, cfg.fontePrezzoPreferita);
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
            } else {
                mon.SetPrezzo("0");  // ← fallback obbligatorio, mai null
            }

            // Determino wallet secondario
            String walletSec = walletPrincipale;
            String owallet = cfg.walletPerCausale.get(causaleCSV);
            if (owallet != null && !owallet.isBlank()) {
                walletSec = owallet;
            }

            scambio.InserisciMoneteCEX(mon, walletSec, causaleCSV, idOrig);
            haMovimentiDefi = true;
        }

        // Se ho accumulato movimenti nel TransazioneDefi, li elaboro con RitornaScambi
        if (haMovimentiDefi && !scambio.isEmpty()) {
            List<String[]> movScambio = Importazioni.RitornaScambi(
                    scambio, dataDiGruppo, cfg.nomeExchange, null);
            if (movScambio != null) {
                // RitornaScambi non riceve la nota: la riporto qui nel campo [21] (le due gambe di un
                // Convert condividono la stessa identica stringa Notes).
                if (cfg.colonnaNote >= 0 && !noteDiGruppo.isBlank()) {
                    String notaNorm = MovimentiCrypto.normalizzaNome(noteDiGruppo);
                    for (String[] m : movScambio) {
                        if (m != null && m.length > 21 && (m[21] == null || m[21].isBlank())) {
                            m[21] = notaNorm;
                        }
                    }
                }
                risultato.addAll(movScambio);
            }
        }

        // Movimenti di commissione, dopo lo scambio: la fee cede la moneta che lo scambio ha appena
        // accreditato, quindi deve pescare da un carico che esiste già.
        // Anche a parità di secondo l'ordine è garantito dalle chiavi: il calcolo plusvalenze scorre
        // MappaCryptoWallet.values(), che è una TreeMap con String.CASE_INSENSITIVE_ORDER, lo scambio ha
        // identificativo "<exchange>" e la commissione "<exchange>C" (la "C" la accoda creaMovimento ai
        // movimenti CM in uscita). Il confronto è case-INsensitive, quindi avviene sui caratteri minuscoli:
        // '_' (0x5F) precede 'c' (0x63) e lo scambio resta primo. Con un confronto sensibile alle maiuscole
        // l'ordine sarebbe invertito ('C' = 0x43), quindi non è un dettaglio da dare per scontato.
        long dataFeeLong = feeDaEmettere.isEmpty() ? 0 : cfg.convertiDataInMillis(dataRawDiGruppo);
        for (String[] fee : feeDaEmettere) {
            Moneta mFee = new Moneta();
            mFee.InserisciValori(fee[0], "-" + fee[1], "", "");
            mFee.AssegnaTipoAuto();

            // Stesso recupero prezzo dello scambio principale (righe piu' sopra): senza questo, creaMovimento
            // non trova ne' un Prezzo ne' un InfoPrezzo gia' assegnati sulla moneta e ripiega sul proprio
            // calcolo interno usando "Wallet" (qui cfg.nomeExchange) come fonte - che e' il nome exchange
            // cosi' com'e' scritto nel JSON (spesso maiuscolo, es. "OKX"), non fontePrezzoPreferita. La
            // commissione finiva cosi' su una fonte diversa da quella dello scambio a cui appartiene.
            Prezzi.InfoPrezzo ipFee = Prezzi.DammiPrezzoInfoTransazione(mFee, null, dataFeeLong, null, cfg.fontePrezzoPreferita);
            if (ipFee != null) {
                mFee.SetPrezzo(ipFee.prezzoQta != null ? ipFee.prezzoQta.toPlainString() : "0");
                mFee.InfoPrezzo = ipFee;
            }

            String[] rtFee = MovimentiCrypto.creaMovimento(
                    mFee, null,
                    cfg.nomeExchange, walletPrincipale,
                    dataFeeLong,
                    null, "CSV",
                    1, 1, null, null, "A",
                    fee[3], "COMMISSIONI", cfg.nomeExchange);

            if (rtFee != null) {
                if (rtFee.length > 7) {
                    rtFee[7] = fee[2];
                }
                if (rtFee.length > 39) {
                    rtFee[39] = "D";
                }
                risultato.add(rtFee);
            }
        }

        return risultato;
    }

    // -------------------------------------------------------------------------
    // PUNTO 1 – costruisciMovimento con segno SENZA abs()
    // -------------------------------------------------------------------------
    /**
     * Costruisce un singolo movimento interno a partire da una riga CSV.
     *
     * <p>
     * Il metodo traduce una riga del CSV nel formato standard atteso da
     * {@code MovimentiCrypto.creaMovimento(...)}. La causale originale del CSV
     * viene convertita nella tipologia interna tramite la mappa definita nel
     * JSON.</p>
     *
     * <p>
     * La quantità mantiene sempre il proprio segno reale:</p>
     * <ul>
     * <li>se il CSV fornisce già il segno, questo viene rispettato;</li>
     * <li>se esiste una colonna separata per il segno, questa viene
     * applicata;</li>
     * <li>le regole {@code causaliUscita} e {@code causaliEntrata} possono
     * forzare il segno;</li>
     * <li>non viene mai usato {@code abs()}, per evitare di perdere
     * l'informazione direzionale del movimento.</li>
     * </ul>
     *
     * <p>
     * Il metodo gestisce inoltre:</p>
     * <ul>
     * <li>override del wallet per causale;</li>
     * <li>fee opzionali;</li>
     * <li>valore euro o prezzo unitario, se presenti;</li>
     * <li>campi extra configurabili da JSON;</li>
     * <li>marcatura del movimento come proveniente da import CSV.</li>
     * </ul>
     *
     * @param riga riga CSV già validata e splittata
     * @param tipoForzato tipologia interna da usare forzatamente; se
     * {@code null} viene ricavata dalla causale CSV
     * @param cfg configurazione attiva dell'importazione
     * @return array di stringhe nel formato interno del movimento, oppure
     * {@code null} se la riga non produce un movimento valido
     */
    /**
     * Riporta al lordo la quantità di {@code m} quando il CSV la fornisce già decurtata della commissione,
     * <b>modificando l'oggetto sul posto</b>. Non fa nulla se la fee è su un'altra moneta o se il flag
     * corrispondente della configurazione è spento.
     *
     * <p>Serve perché per le cripto-attività le commissioni non sono deducibili (art. 68 c.9-bis del TUIR,
     * circolare 30/E del 27/10/23): importare la quantità netta equivarrebbe a dedurre la commissione dal
     * costo di carico. La quantità torna quindi al lordo e la fee viene emessa come movimento
     * {@code COMMISSIONI} a sé stante dal chiamante.
     *
     * <p>È l'<b>unico</b> punto in cui vive la formula: la usano sia {@link #costruisciMovimenti} (righe
     * singole) sia {@link #consolidaGruppo} (gruppi multi-riga), così lo stesso flag di configurazione non
     * può significare due cose diverse a seconda del formato del file.
     *
     * <p><b>Nota aperta sul lato uscita.</b> La formula qui sotto è quella storica, mantenuta invariata
     * perché è quella con cui è stato tarato l'import CoinTracking, l'unica configurazione che accende
     * {@code ricostruisciLordoSeFeeSuMonetaUscita}. Somma però la fee anche quando la quantità è negativa,
     * il che <i>riduce</i> il modulo dell'uscita ({@code -485,86 + 1,18 = -484,68}), mentre il commento
     * originale del metodo indicava {@code -485,86 - 1,18 = -487,04}, cioè un'uscita più grande. Le due
     * letture corrispondono a due convenzioni diverse su cosa contenga la colonna del CSV (quantità già
     * comprensiva della fee oppure al netto), e senza un export CoinTracking di riferimento non è
     * decidibile. Sul lato entrata non c'è ambiguità: il lordo è sempre maggiore del netto. Gli import di
     * OKX non sono toccati dalla questione, perché tengono il flag sull'uscita spento (la commissione è
     * sempre addebitata sulla gamba in entrata).
     *
     * @param m moneta da correggere, può essere {@code null}
     * @param monetaFee simbolo della moneta in cui è espressa la commissione
     * @param qtaFeeAssoluta quantità della commissione, in valore assoluto
     * @param cfg configurazione attiva dell'importazione
     */
    private static void ricostruisciLordoConFee(Moneta m, String monetaFee,
            BigDecimal qtaFeeAssoluta, ConfigurazioneImport cfg) {

        if (m == null || monetaFee == null || monetaFee.isBlank()) return;

        String nome = m.GetNome();
        if (!monetaFee.equalsIgnoreCase(nome)) return;

        BigDecimal qtaNetta = new BigDecimal(m.GetQta());
        boolean inUscita = qtaNetta.signum() < 0;

        if (inUscita && !cfg.ricostruisciLordoSeFeeSuMonetaUscita) return;
        if (!inUscita && !cfg.ricostruisciLordoSeFeeSuMonetaEntrata) return;

        //Entrata: ne è arrivata di meno, il lordo è più grande (2130 + 1,18 = 2131,18).
        //Uscita: formula storica, vedi la nota aperta nel javadoc.
        BigDecimal qtaLorda = qtaNetta.add(qtaFeeAssoluta);
        m.InserisciValori(nome, qtaLorda.stripTrailingZeros().toPlainString(), "", "");
        m.AssegnaTipoAuto();
    }

    /**
     * Importo (>= 0) della commissione della riga, nella valuta di controvalore. Un valore assente, non
     * numerico o non positivo vale 0: in alcune righe dust di Coinbase la colonna 'Fees and/or Spread'
     * porta un rapporto di spread con segno, non un importo di commissione.
     */
    static BigDecimal commissioneControvalore(String[] riga, ConfigurazioneImport cfg) {
        if (cfg.colControvaloreCommissione < 0) return BigDecimal.ZERO;
        String f = normalizzaNumero(safe(riga, cfg.colControvaloreCommissione));
        if (f.isBlank() || !Funzioni.isNumeric(f, false)) return BigDecimal.ZERO;
        BigDecimal v = new BigDecimal(f);
        return v.signum() > 0 ? v : BigDecimal.ZERO;
    }

    /**
     * Controvalore in euro "al netto della sola commissione" per la riga: {@code |Total| - commissione},
     * dove {@code Total} e' la colonna comprensiva di commissione e spread. È l'<b>unico</b> punto in cui
     * vive la formula, condivisa da {@link #costruisciMovimenti} (righe singole, es. Coinbase 'Buy') e
     * {@link #consolidaGruppo} (gruppi multi-riga, es. Coinbase 'Convert'), così la stessa causale non
     * puo' avere un costo di carico diverso a seconda del formato della riga.
     *
     * <p>Lo spread resta nel costo di carico: per le cripto-attivita' non e' deducibile la commissione
     * (art. 68 c.9-bis TUIR, circ. 30/E del 27/10/23), ma lo spread non e' una commissione.
     *
     * @return la stringa {@code BigDecimal} del controvalore, oppure {@code null} se la riga non e' fra
     *         quelle in {@code causaliControvaloreNetto} o manca la colonna del totale.
     */
    static String controvaloreAlNetto(String[] riga, ConfigurazioneImport cfg) {
        if (cfg.colControvaloreTotale < 0 || cfg.causaliControvaloreNetto.isEmpty()) return null;
        String causale = cfg.getCausaleCSV(riga);
        if (causale == null || !cfg.causaliControvaloreNetto.contains(causale.trim())) return null;
        String tot = normalizzaNumero(safe(riga, cfg.colControvaloreTotale));
        if (tot.isBlank() || !Funzioni.isNumeric(tot, false)) return null;
        BigDecimal netto = new BigDecimal(tot).abs().subtract(commissioneControvalore(riga, cfg));
        return netto.stripTrailingZeros().toPlainString();
    }

    static List<String[]> costruisciMovimenti(String[] riga, String tipoForzato, ConfigurazioneImport cfg) {
    try {
        List<String[]> risultato = new ArrayList<>();

        String dataRaw = safe(riga, cfg.colonnaData);
        String data = cfg.normalizzaData(dataRaw);
        if (data == null || data.isBlank()) {
            return null;
        }

        long dataLong = cfg.convertiDataInMillis(dataRaw);
        if (dataLong <= 0) {
            return null;
        }

        String exchange = nvl(cfg.nomeExchange, "Exchange Generico");
        String wallet = nvl(cfg.nomeWallet, "Principale");

        if (cfg.colonnaWallet >= 0 && cfg.colonnaWallet < riga.length && !safe(riga, cfg.colonnaWallet).isBlank()) {
            wallet = safe(riga, cfg.colonnaWallet);
        }

        String causaleCSV = cfg.getCausaleCSV(riga);

        String walletOverride = cfg.walletPerCausale.get(causaleCSV);
        if (walletOverride != null && !walletOverride.isBlank()) {
            wallet = walletOverride;
        }

        String tipoMovimento = tipoForzato != null ? tipoForzato : cfg.tipoMovimentoPerRiga(riga);
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

        // Gestione segno da colonna dedicata
        if (cfg.colonnaSegno >= 0 && cfg.colonnaSegno < riga.length) {
            String segno = safe(riga, cfg.colonnaSegno);
            if ("-".equals(segno) && !qtaStr.startsWith("-")) {
                qtaStr = "-" + qtaStr;
            } else if ("+".equals(segno)) {
                qtaStr = qtaStr.replace("-", "");
            }
        }

        // Forzatura segno in base alla causale CSV
        ConfigurazioneImport.RegolaSegno regola = cfg.regolaSegno(causaleCSV);
        if (regola == ConfigurazioneImport.RegolaSegno.FORZATO_USCITA) {
            if (!qtaStr.startsWith("-")) {
                qtaStr = "-" + qtaStr;
            }
        } else if (regola == ConfigurazioneImport.RegolaSegno.FORZATO_ENTRATA) {
            qtaStr = qtaStr.replace("-", "");
        }

        Moneta mOUT = null;
        Moneta mIN = null;
        BigDecimal qtaPrimaria = null;

        // Lato principale letto da moneta/quantita
        if (!qtaStr.isBlank() && Funzioni.isNumeric(qtaStr, false)) {
            BigDecimal qta = new BigDecimal(qtaStr);
            qtaPrimaria = qta;
            if (qta.compareTo(BigDecimal.ZERO) < 0) {
                mOUT = new Moneta();
                mOUT.InserisciValori(moneta, qta.stripTrailingZeros().toPlainString(), "", "");
                mOUT.AssegnaTipoAuto();
            } else if (qta.compareTo(BigDecimal.ZERO) > 0) {
                mIN = new Moneta();
                mIN.InserisciValori(moneta, qta.stripTrailingZeros().toPlainString(), "", "");
                mIN.AssegnaTipoAuto();
            }
        }

        // Eventuale lato uscita separato sulla stessa riga
        if (cfg.colonnaQuantitaUscita >= 0 && cfg.colonnaMonetaUscita >= 0) {
            String qtaOut = normalizzaNumero(safe(riga, cfg.colonnaQuantitaUscita));
            String monOut = cfg.normalizzaMoneta(safe(riga, cfg.colonnaMonetaUscita));

            if (!qtaOut.isBlank() && Funzioni.isNumeric(qtaOut, false)) {
                BigDecimal qOut = new BigDecimal(qtaOut);

                // Formato "gamba doppia con segno sull'uscita": la riga ha SEMPRE due colonne moneta,
                // ma sono le due gambe di UNO scambio solo quando le monete differiscono. Quando la
                // moneta di uscita coincide con quella principale (export tipo Nexo: 'Interest',
                // 'Top up', 'Withdrawal', term deposit… ripetono la stessa moneta su Input e Output),
                // la riga e' un movimento a gamba singola: il verso lo da il segno della colonna di
                // uscita (l'unica che lo porta), l'importo lo da la colonna principale (che e' quello
                // effettivo: su 'Top up' Input e Output differiscono di poco per via della fee di rete).
                // Eccezione: alcuni 'Top up Crypto' promozionali di Nexo ('Campaign Rewards') mettono
                // l'importo solo su Input e lasciano Output a 0 — se la principale e' zero (o assente) si
                // ripiega su quella di uscita, altrimenti la riga verrebbe scartata con qta=0.
                // Senza questo, un accredito di interessi 'NEXO -> NEXO' diventerebbe un finto scambio.
                boolean stessaMoneta = !monOut.isBlank() && monOut.equalsIgnoreCase(moneta);

                if (cfg.gambaDoppiaConSegnoSuUscita && stessaMoneta) {
                    mOUT = null;
                    mIN = null;
                    BigDecimal importo = (qtaPrimaria != null && qtaPrimaria.signum() != 0 ? qtaPrimaria : qOut).abs();
                    if (qOut.signum() != 0 && importo.signum() != 0) {
                        Moneta m = new Moneta();
                        String q = (qOut.signum() < 0 ? importo.negate() : importo).stripTrailingZeros().toPlainString();
                        m.InserisciValori(monOut, q, "", "");
                        m.AssegnaTipoAuto();
                        if (qOut.signum() < 0) mOUT = m; else mIN = m;
                    }
                } else if (qOut.compareTo(BigDecimal.ZERO) > 0) {
                    mOUT = new Moneta();
                    mOUT.InserisciValori(monOut, "-" + qOut.stripTrailingZeros().toPlainString(), "", "");
                    mOUT.AssegnaTipoAuto();
                } else if (qOut.compareTo(BigDecimal.ZERO) < 0) {
                    mOUT = new Moneta();
                    mOUT.InserisciValori(monOut, qOut.stripTrailingZeros().toPlainString(), "", "");
                    mOUT.AssegnaTipoAuto();
                }
            }
        }

        // Controvalore "al netto della sola commissione" (Coinbase Buy): il Total del CSV comprende
        // commissione + spread; il costo di carico e' Total - commissione (lo spread resta). Per le
        // causali in causaliControvaloreConCommissione la riga porta la sola gamba crypto in entrata:
        // si sintetizza la gamba FIAT in uscita di quell'importo, così l'acquisto diventa un vero
        // scambio FIAT->crypto (categoria AC invece di deposito crypto), e la commissione esce come
        // movimento COMMISSIONI a se'.
        String controvaloreNetto = controvaloreAlNetto(riga, cfg);
        String valutaControv = "";
        BigDecimal commControv = BigDecimal.ZERO;
        boolean causaleConCommissione = controvaloreNetto != null
                && cfg.causaliControvaloreConCommissione.contains(causaleCSV.trim());
        if (controvaloreNetto != null) {
            valutaControv = cfg.colControvaloreValuta >= 0
                    ? cfg.normalizzaMoneta(safe(riga, cfg.colControvaloreValuta)) : "EUR";
            commControv = commissioneControvalore(riga, cfg);
            if (causaleConCommissione && mOUT == null && mIN != null && !valutaControv.isBlank()) {
                mOUT = new Moneta();
                mOUT.InserisciValori(valutaControv, "-" + controvaloreNetto, "", "");
                mOUT.AssegnaTipoAuto();
            }
        }

        String prezzoMov = controvaloreNetto != null ? controvaloreNetto
                : (!valoreEuro.isBlank() ? valoreEuro : prezzo);

        // Nota da riportare nel campo [21] del movimento
        String nota = cfg.colonnaNote >= 0 ? safe(riga, cfg.colonnaNote) : null;

        // Fee: sempre trattata come valore assoluto positivo di lavoro
        boolean haFee = !qtaFee.isBlank()
                && !monetaFee.isBlank()
                && Funzioni.isNumeric(qtaFee, false)
                && new BigDecimal(qtaFee).compareTo(BigDecimal.ZERO) != 0;

        BigDecimal qtaFeeBD = BigDecimal.ZERO;
        if (haFee) {
            qtaFeeBD = new BigDecimal(qtaFee).abs();
            ricostruisciLordoConFee(mOUT, monetaFee, qtaFeeBD, cfg);
            ricostruisciLordoConFee(mIN, monetaFee, qtaFeeBD, cfg);
        }

        // Movimento principale
        String[] rt = MovimentiCrypto.creaMovimento(
                mOUT, mIN, exchange, wallet, dataLong,
                prezzoMov, "CSV", 1, 1, null, nota, "A",
                idTrans, tipoMovimento, exchange
        );

        if (rt != null) {
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
                    if (campoMov < 0 || campoMov >= rt.length) {
                        continue;
                    }
                    String valore = safe(riga, colCsv);
                    // Il campo [29] è il timestamp epoch del movimento, già valorizzato da
                    // creaMovimento. Un campiExtra che vi versa una colonna di testo (era il caso
                    // di "Binance CSV.json" con "29": 6 -> colonna Remark: "Binance Earn", ecc.)
                    // ne distrugge il timestamp. Qui si rifiuta sempre una scrittura non numerica
                    // su [29] e si mantiene il valore calcolato.
                    if (campoMov == 29 && !Funzioni.isNumeric(valore, false)) {
                        LoggerGC.ScriviErrore("campiExtra: ignorata scrittura non numerica sul campo 29 (timestamp): \"" + valore + "\"");
                        continue;
                    }
                    rt[campoMov] = valore;
                } catch (Exception ex) {
                    LoggerGC.ScriviErrore(ex);
                }
            }

            risultato.add(rt);
        }

        // Movimento commissione separato
        if (haFee) {
            Moneta mFee = new Moneta();
            mFee.InserisciValori(monetaFee, "-" + qtaFeeBD.stripTrailingZeros().toPlainString(), "", "");
            mFee.AssegnaTipoAuto();

            String[] rtFee = MovimentiCrypto.creaMovimento(
                    mFee,
                    null,
                    exchange,
                    wallet,
                    dataLong,
                    null,
                    "CSV",
                    1, 1,
                    null, null,
                    "A",
                    idTrans,
                    "COMMISSIONI",
                    exchange
            );

            if (rtFee != null) {
                if (rtFee.length > 7) {
                    rtFee[7] = causaleCSV;
                }
                if (rtFee.length > 39) {
                    rtFee[39] = "D";
                }
                risultato.add(rtFee);
            }
        }

        // Movimento COMMISSIONI in valuta di controvalore (Coinbase Buy: commissione in EUR, gia'
        // esclusa dal costo di carico da controvaloreAlNetto). Non emesso se la commissione non e' un
        // importo positivo.
        if (causaleConCommissione && commControv.signum() > 0 && !valutaControv.isBlank()) {
            Moneta mComm = new Moneta();
            mComm.InserisciValori(valutaControv, "-" + commControv.stripTrailingZeros().toPlainString(), "", "");
            mComm.AssegnaTipoAuto();

            String[] rtComm = MovimentiCrypto.creaMovimento(
                    mComm, null, exchange, wallet, dataLong,
                    null, "CSV", 1, 1, null, nota, "A",
                    idTrans, "COMMISSIONI", exchange
            );
            if (rtComm != null) {
                if (rtComm.length > 7) rtComm[7] = causaleCSV;
                if (rtComm.length > 39) rtComm[39] = "D";
                risultato.add(rtComm);
            }
        }

        return risultato.isEmpty() ? null : risultato;

    } catch (Exception ex) {
        LoggerGC.ScriviErrore(ex);
        return null;
    }
}
    
 
    // -------------------------------------------------------------------------
    // UTILITY
    // -------------------------------------------------------------------------
    /**
     * Restituisce il contenuto di una colonna in modo sicuro.
     *
     * <p>
     * Se l'array è nullo, l'indice è fuori range oppure il valore è nullo,
     * restituisce stringa vuota invece di lanciare eccezioni.</p>
     *
     * @param arr array sorgente
     * @param idx indice della colonna da leggere
     * @return contenuto trimmato della colonna, oppure stringa vuota
     */
    private static String safe(String[] arr, int idx) {
        if (arr == null || idx < 0 || idx >= arr.length || arr[idx] == null) {
            return "";
        }
        return arr[idx].trim();
    }

    /**
     * Normalizza una rappresentazione numerica proveniente dal CSV.
     *
     * <p>
     * Il metodo:</p>
     * <ul>
     * <li>rimuove spazi superflui (compreso lo spazio unificatore {@code U+00A0});</li>
     * <li>rimuove il simbolo di valuta anteposto o posposto ({@code € $ £ ¥}), come
     * negli export Coinbase ({@code €0.05019}, {@code -€1.48341}) e Nexo ({@code $1059.55}):
     * il segno meno resta dov'è, quindi {@code -€1.48} diventa {@code -1.48};</li>
     * <li>converte la virgola decimale in punto;</li>
     * <li>trasforma la notazione scientifica in formato decimale espanso
     * tramite {@link BigDecimal#toPlainString()}.</li>
     * </ul>
     * <p>Non gestisce il separatore delle migliaia: gli export trattati usano il punto
     * come separatore decimale e nessun raggruppamento.</p>
     *
     * <p>
     * L'ultimo punto è importante per evitare problemi nei casi in cui il CSV
     * contenga valori come {@code 9.4E-7}, che devono essere preservati come
     * quantità decimale effettiva.</p>
     *
     * @param s stringa numerica da normalizzare
     * @return numero normalizzato, oppure stringa vuota se il valore è assente
     */
    private static String normalizzaNumero(String s) {
        if (s == null) {
            return "";
        }
        s = s.trim();
        if (s.isBlank()) {
            return "";
        }
        s = s.replace(" ", "").replace(" ", "")
             .replace("€", "").replace("$", "").replace("£", "").replace("¥", "")
             .replace(",", ".");

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

    /**
     * Restituisce una stringa di fallback se il valore passato è nullo o vuoto.
     *
     * @param s valore originale
     * @param def valore di default
     * @return {@code s} se valorizzata, altrimenti {@code def}
     */
    private static String nvl(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    /**
     * Registra una riga come scartata durante il processo di importazione.
     *
     * <p>
     * Il metodo aggiorna il log testuale dei movimenti sconosciuti e incrementa
     * i contatori globali degli scarti. Viene usato per righe non valide,
     * causali non riconosciute o record non importabili.</p>
     *
     * @param motivo descrizione sintetica del motivo dello scarto
     * @param riga contenuto originale della riga o sua rappresentazione
     * testuale
     */
    private static void scarta(String motivo, String riga) {
        Importazioni.movimentiSconosciuti += motivo + " | " + riga + "\n";
        Importazioni.TrasazioniScartate++;    // <- aggiunta
        Importazioni.TrasazioniSconosciute++;
    }

    // =========================================================================
    // CLASSE CONFIGURAZIONE
    // =========================================================================
    /**
     * Contenitore della configurazione JSON usata per pilotare l'importazione
     * CSV.
     *
     * <p>
     * Questa classe descrive sia la struttura fisica del file sorgente
     * (separatore, encoding, colonne, formato data), sia le regole funzionali
     * necessarie per trasformare le righe CSV nei movimenti interni
     * dell'applicazione.</p>
     *
     * <p>
     * Tra gli aspetti configurabili rientrano:</p>
     * <ul>
     * <li>mappatura delle causali CSV verso le tipologie interne;</li>
     * <li>regole di segno per entrate e uscite;</li>
     * <li>causali da trattare come movimenti chiusi/singoli;</li>
     * <li>causali per cui è ammessa una tolleranza temporale di
     * consolidamento;</li>
     * <li>rinomina o pulizia dei simboli moneta;</li>
     * <li>override del wallet in base alla causale;</li>
     * <li>copia di colonne CSV in campi extra del movimento finale.</li>
     * </ul>
     *
     * <p>
     * L'obiettivo è rendere l'importatore il più possibile riusabile per
     * exchange diversi senza duplicare codice Java per ogni nuovo formato
     * CSV.</p>
     */
    public static class ConfigurazioneImport {

        public String nomeExchange = "";
        /**
         * Exchange da preferire quando si cerca il prezzo di un movimento importato da questa
         * configurazione (colonna {@code exchange} di {@code PrezziNew} - stesso id usato da CCXT,
         * minuscolo: {@code "binance"}, {@code "okx"}...). Tutti e 7 gli exchange configurati vengono
         * comunque interrogati allo scaricamento ({@code Prezzi.RecuperaPrezziDaCCXT}): questo campo
         * sceglie solo quale dei prezzi già scaricati usare quando più di uno è disponibile, non
         * limita da chi si scarica. Vuoto (default) = nessuna preferenza, comportamento di sempre.
         * Vale solo al momento dell'import (vedi ImportazioneGenerica.consolidaGruppo): le rivalorizzazioni
         * fiscali successive (Calcoli_RT/Calcoli_RW) non lo consultano.
         */
        public String fontePrezzoPreferita = "";
        /**
         * Exchange o fornitore dei dati sotto cui raggruppare questa configurazione nella finestra di
         * import: è la voce della prima combo, e il suo slug è anche il nome del file del logo in
         * {@code config/loghi/}.
         * <p>Va indicato solo quando il fornitore non si ricava da solo, perché nell'ordine normale
         * basta {@link #nomeExchange} (formati di un singolo exchange) oppure una parola nota nel nome
         * del file (formati multi-exchange come CoinTracking e Tatax, vedi
         * {@code Importazioni_Gestione.FornitoreDaNomeFile}). In mancanza di tutto si usa il nome del
         * file di configurazione.
         */
        public String fornitore = "";
        /**
         * Nome dell'estrazione all'interno del fornitore, cioè la voce della seconda combo: {@code "CSV"},
         * {@code "Funding"}, {@code "Trading"}… Va scritto senza ripetere il nome del fornitore, che è già
         * nella prima combo. Se vuoto si usa il nome del file di configurazione.
         */
        public String estrazione = "";
        /**
         * Testo libero che descrive le caratteristiche di questo import (da dove si scarica il file,
         * cosa copre, limiti noti). Non incide sull'importazione: viene mostrato come tooltip sulla
         * voce corrispondente nella finestra {@code Importazioni_Gestione}. Vuoto = nessun tooltip.
         */
        public String descrizione = "";
        public String nomeWallet = "Principale";
        public String separatore = ",";
        public String encoding = "UTF-8";
        public boolean testing = false;
        public int righeIntestazione = 1;
        public int rigaIntestazione = 1;
        public boolean autoDetectColonne = false;
        public String formatoData = "yyyy-MM-dd HH:mm:ss";
        public String fuso = "UTC";

        // Indici colonne (-1 = non presente)
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
        //Colonna su cui raggruppare le righe, quando NON coincide con quella dell'identificativo.
        //Serve perché i due ruoli sono distinti: `idTransazione` dice *chi è* la riga (finisce nel campo [24]
        //del movimento ed è ciò su cui lavora la deduplica), `idGruppo` dice *con chi sta*. Nell'export di
        //trading di OKX ogni gamba e ogni fill hanno un `id` diverso ma condividono l'`Order id`: usare il
        //primo anche per raggruppare spezzerebbe ogni scambio in gambe isolate.
        //Quando non è configurata si raggruppa come sempre sull'identificativo, quindi gli altri file di
        //ImportConfig/ non cambiano comportamento.
        public int colonnaIDGruppo = -1;
        public int colonnaWallet = -1;
        //Colonna del CSV da riportare nel campo Note del movimento ([21]).
        public int colonnaNote = -1;

        //Raggruppamento specifico per causale CSV: sovrascrive colonnaRaggruppamento() per le sole
        //causali elencate. Serve quando due gambe dello stesso movimento condividono una colonna
        //(es. Coinbase 'Convert': stessa stringa Notes) ma NON il timestamp esatto, per cui il
        //raggruppo di default le spezzerebbe. Le altre causali non cambiano comportamento.
        public Map<String, Integer> raggruppamentoPerCausale = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        //--- Controvalore "al netto della sola commissione" (Coinbase Buy/Convert) ---------------------
        //Il costo di carico per queste causali NON e' una colonna diretta ma [colControvaloreTotale] -
        //[colControvaloreCommissione]: il Total del CSV comprende commissione E spread; per le cripto la
        //commissione non e' deducibile (art. 68 c.9-bis TUIR) ma lo spread si', quindi lo spread resta.
        //La commissione conta solo se importo positivo (in alcune righe dust e' un rapporto con segno).
        public int colControvaloreTotale = -1;
        public int colControvaloreCommissione = -1;
        public int colControvaloreValuta = -1;   // valuta della gamba FIAT sintetica e del movimento COMMISSIONI
        public Set<String> causaliControvaloreNetto = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        //Sottoinsieme di causaliControvaloreNetto: la riga porta la sola gamba crypto in entrata, quindi
        //oltre a correggere il controvalore si sintetizza la gamba FIAT in uscita (l'acquisto diventa un
        //vero scambio FIAT->crypto) e la commissione esce come movimento COMMISSIONI a se'.
        public Set<String> causaliControvaloreConCommissione = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        //--- Classificazione in base al testo libero della colonna note ------------------------------
        //Alcuni export mettono la natura del movimento solo nelle note: Coinbase scrive un premio
        //"Coinbase Earn" come 'Receive' con nota "Received X TOK from Coinbase Earn", indistinguibile
        //da un vero trasferimento se non dal testo. Ogni regola: se la causale CSV combacia (o è
        //assente) e la colonna note contiene la sottostringa, il tipo interno diventa quello indicato.
        public List<RegolaCausaleNota> causalePerNota = new ArrayList<>();

        /** Regola "se la nota contiene X (e la causale CSV è Y) allora il tipo è Z". */
        public static final class RegolaCausaleNota {
            public final String causale;      // null = qualsiasi causale
            public final String notaContiene; // già in minuscolo, confronto case-insensitive
            public final String tipo;
            public RegolaCausaleNota(String causale, String notaContiene, String tipo) {
                this.causale = (causale == null || causale.isBlank()) ? null : causale.trim();
                this.notaContiene = notaContiene == null ? "" : notaContiene.toLowerCase();
                this.tipo = tipo;
            }
        }

        /**
         * Tipo interno del movimento per la riga: se una regola di {@code causalePerNota} corrisponde
         * (causale CSV giusta e colonna note contenente la sottostringa) restituisce il tipo della
         * regola, altrimenti la normale {@link #convertiCausale(String)} della causale CSV.
         */
        public String tipoMovimentoPerRiga(String[] riga) {
            String causaleCSV = getCausaleCSV(riga);
            if (!causalePerNota.isEmpty() && colonnaNote >= 0) {
                String nota = safe(riga, colonnaNote).toLowerCase();
                if (!nota.isEmpty()) {
                    for (RegolaCausaleNota r : causalePerNota) {
                        if (!r.notaContiene.isEmpty() && nota.contains(r.notaContiene)
                                && (r.causale == null || r.causale.equalsIgnoreCase(causaleCSV.trim()))) {
                            return r.tipo;
                        }
                    }
                }
            }
            return convertiCausale(causaleCSV);
        }

        /** @return la colonna da usare per decidere se due righe appartengono allo stesso movimento */
        public int colonnaRaggruppamento() {
            return colonnaIDGruppo >= 0 ? colonnaIDGruppo : colonnaIDTransazione;
        }

        /** @return la colonna di raggruppamento per la causale CSV data, o quella di default se non ha override */
        public int colonnaRaggruppamento(String causaleCSV) {
            Integer c = causaleCSV == null ? null : raggruppamentoPerCausale.get(causaleCSV.trim());
            return c != null ? c : colonnaRaggruppamento();
        }
        public int colonnaMonetaUscita  = -1;
        public int colonnaQuantitaUscita = -1;
        
        //Questi 2 parametri servono per dire al programma come trattare le fee nei file che hanno tutto in una riga ovvero   
        // true  = ricostruisce il lordo + genera rigo commissione separato
        // false = genera solo il rigo commissione, non tocca la quantità principale
        public boolean ricostruisciLordoSeFeeSuMonetaUscita  = false;  // default: non ricostruisce il lordo su mOUT
        public boolean ricostruisciLordoSeFeeSuMonetaEntrata = false;  // default: non ricostruisce il lordo su mIN

        // Export in cui OGNI riga ha due colonne moneta (moneta/quantita + monetaUscita/quantitaUscita)
        // ma sono le due gambe di uno scambio SOLO quando le monete differiscono; quando coincidono la
        // riga e' un movimento a gamba singola e il verso sta nel segno della colonna di uscita.
        // Tipico dell'export Nexo (Input Currency/Amount + Output Currency/Amount): 'Exchange' ha monete
        // diverse, 'Interest'/'Top up'/'Withdrawal'/term deposit ripetono la stessa moneta.
        public boolean gambaDoppiaConSegnoSuUscita = false;

        // Causali (valore CSV) le cui righe vengono sommate per (giorno, moneta) in un unico movimento
        // PRIMA del raggruppamento. Serve per gli export che accreditano micro-interessi molte volte al
        // giorno (Bitget: 28k righe 'Interest'): senza, l'archivio si gonfia di decine di migliaia di
        // movimenti EARN minuscoli. La riga sintetica eredita data (primo accredito del giorno) e le
        // altre colonne dalla prima riga del gruppo, con la sola quantita' sostituita dalla somma.
        public Set<String> causaliConsolidaPerGiorno = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        public boolean consolidaRigheStessaData = false;
        public long tolleranzaSecondiConsolidamento = 2;

        public Map<String, String> mappaCausali = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        public Set<String> causaliUscita = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        public Set<String> causaliEntrata = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        /**
         * NUOVO – causali che, anche in gruppo multi-riga, vanno trattate come
         * movimento singolo (non accumulate nel TransazioneDefi). Es:
         * commissioni, earn, reward, trasferimenti interni.
         */
        public Set<String> causaliChiuse = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        public Map<String, String> rinominaMonete = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        public List<RimuoviRegola> rimuoviDaNomeMoneta = new ArrayList<>();
        public boolean rimuoviCaseSensitive = false; // default: case insensitive
        public Map<String, Integer> campiExtra = new TreeMap<>();
        public Map<String, String> walletPerCausale = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        public Map<String, Integer> mappaNomiColonne = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        public Set<String> causaliDifferite = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        // Causale composita (max 3 colonne concatenate con separatoreCausale)
        public int colonnaCausale2 = -1;
        public int colonnaCausale3 = -1;
        public String separatoreCausale = ".";
        public boolean causaliUppercase = false;

        public boolean centralizzato = false;

        // Mappa nome-header → nome-campo per auto-detect colonne da intestazione
        public Map<String, String> mappaAutoDetect = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        // -----------------------------------------------------------------
        // Caricamento da JSON
        // -----------------------------------------------------------------
        /**
         * Carica una configurazione di importazione da file JSON.
         *
         * <p>
         * Il metodo legge il file di configurazione, applica i valori trovati e
         * lascia invariati i default per tutti i campi non specificati.</p>
         *
         * <p>
         * Supporta anche alcune chiavi legacy o alternative, come ad esempio
         * {@code movimentoChiuso} come alias di {@code causaliChiuse}.</p>
         *
         * @param percorso percorso del file JSON di configurazione
         * @return istanza popolata di {@code ConfigurazioneImport}
         * @throws Exception in caso di errori di lettura o parsing del JSON
         */
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

            if (root.has("nomeExchange")) {
                cfg.nomeExchange = root.getString("nomeExchange");
            }
            if (root.has("fontePrezzoPreferita")) {
                cfg.fontePrezzoPreferita = root.getString("fontePrezzoPreferita");
            }
            if (root.has("fornitore")) {
                cfg.fornitore = root.getString("fornitore");
            }
            if (root.has("estrazione")) {
                cfg.estrazione = root.getString("estrazione");
            }
            if (root.has("descrizione")) {
                cfg.descrizione = root.getString("descrizione");
            }
            if (root.has("nomeWallet")) {
                cfg.nomeWallet = root.getString("nomeWallet");
            }
            if (root.has("testing")) {
                cfg.testing = root.optBoolean("testing", false);
            }
            if (root.has("separatore")) {
                cfg.separatore = root.getString("separatore");
            }
            if (root.has("encoding")) {
                cfg.encoding = root.getString("encoding");
            }
            if (root.has("righeIntestazione")) {
                cfg.righeIntestazione = root.getInt("righeIntestazione");
            }
            if (root.has("rigaIntestazione")) {
                cfg.rigaIntestazione = root.getInt("rigaIntestazione");
            }
            if (root.has("autoDetectColonne")) {
                cfg.autoDetectColonne = root.getBoolean("autoDetectColonne");
            }
            if (root.has("formatoData")) {
                cfg.formatoData = root.getString("formatoData");
            }
            if (root.has("fuso")) {
                cfg.fuso = root.getString("fuso");
            }
            if (root.has("consolidaRigheStessaData")) {
                cfg.consolidaRigheStessaData = root.getBoolean("consolidaRigheStessaData");
            }
            if (root.has("tolleranzaSecondiConsolidamento")) {
                cfg.tolleranzaSecondiConsolidamento = root.getLong("tolleranzaSecondiConsolidamento");
            }
            if (root.has("causaliDifferite")) {
                JSONArray arr = root.getJSONArray("causaliDifferite");
                for (int i = 0; i < arr.length(); i++) {
                    cfg.causaliDifferite.add(arr.getString(i));
                }
            }

            if (root.has("ricostruisciLordoSeFeeSuMonetaUscita")) {
                cfg.ricostruisciLordoSeFeeSuMonetaUscita = root.getBoolean("ricostruisciLordoSeFeeSuMonetaUscita");
            }
            if (root.has("ricostruisciLordoSeFeeSuMonetaEntrata"))
                cfg.ricostruisciLordoSeFeeSuMonetaEntrata = root.getBoolean("ricostruisciLordoSeFeeSuMonetaEntrata");
            if (root.has("gambaDoppiaConSegnoSuUscita"))
                cfg.gambaDoppiaConSegnoSuUscita = root.getBoolean("gambaDoppiaConSegnoSuUscita");
            if (root.has("causaliConsolidaPerGiorno")) {
                JSONArray arr = root.getJSONArray("causaliConsolidaPerGiorno");
                for (int i = 0; i < arr.length(); i++) cfg.causaliConsolidaPerGiorno.add(arr.getString(i));
            }

            if (root.has("colonne")) {
                JSONObject col = root.getJSONObject("colonne");
                if (col.has("data")) {
                    cfg.colonnaData = col.getInt("data");
                }
                if (col.has("moneta")) {
                    cfg.colonnaMoneta = col.getInt("moneta");
                }
                if (col.has("quantita")) {
                    cfg.colonnaQuantita = col.getInt("quantita");
                }
                if (col.has("monetaUscita"))   cfg.colonnaMonetaUscita   = col.getInt("monetaUscita");
                if (col.has("quantitaUscita")) cfg.colonnaQuantitaUscita = col.getInt("quantitaUscita");
                if (col.has("segno")) {
                    cfg.colonnaSegno = col.getInt("segno");
                }
                if (col.has("causale")) {
                    cfg.colonnaCausale = col.getInt("causale");
                }
                if (col.has("valoreEuro")) {
                    cfg.colonnaValoreEuro = col.getInt("valoreEuro");
                }
                if (col.has("prezzo")) {
                    cfg.colonnaPrezzo = col.getInt("prezzo");
                }
                if (col.has("monetaFee")) {
                    cfg.colonnaMonetaFee = col.getInt("monetaFee");
                }
                if (col.has("quantitaFee")) {
                    cfg.colonnaQuantitaFee = col.getInt("quantitaFee");
                }
                if (col.has("idTransazione")) {
                    cfg.colonnaIDTransazione = col.getInt("idTransazione");
                }
                if (col.has("idGruppo")) {
                    cfg.colonnaIDGruppo = col.getInt("idGruppo");
                }
                if (col.has("wallet")) {
                    cfg.colonnaWallet = col.getInt("wallet");
                }
                if (col.has("causale2")) cfg.colonnaCausale2 = col.getInt("causale2");
                if (col.has("causale3")) cfg.colonnaCausale3 = col.getInt("causale3");
                if (col.has("note")) cfg.colonnaNote = col.getInt("note");
            }

            if (root.has("raggruppamentoPerCausale")) {
                JSONObject rp = root.getJSONObject("raggruppamentoPerCausale");
                for (String k : rp.keySet()) cfg.raggruppamentoPerCausale.put(k, rp.getInt(k));
            }

            if (root.has("causalePerNota")) {
                JSONArray arr = root.getJSONArray("causalePerNota");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    cfg.causalePerNota.add(new ConfigurazioneImport.RegolaCausaleNota(
                            o.has("causale") ? o.getString("causale") : null,
                            o.optString("notaContiene", ""),
                            o.getString("tipo")));
                }
            }

            if (root.has("colonneControvalore")) {
                JSONObject cv = root.getJSONObject("colonneControvalore");
                if (cv.has("totale"))      cfg.colControvaloreTotale      = cv.getInt("totale");
                if (cv.has("commissione")) cfg.colControvaloreCommissione = cv.getInt("commissione");
                if (cv.has("valuta"))      cfg.colControvaloreValuta      = cv.getInt("valuta");
                if (cv.has("causali")) {
                    JSONArray a = cv.getJSONArray("causali");
                    for (int i = 0; i < a.length(); i++) cfg.causaliControvaloreNetto.add(a.getString(i));
                }
                if (cv.has("causaliConMovimentoCommissione")) {
                    JSONArray a = cv.getJSONArray("causaliConMovimentoCommissione");
                    for (int i = 0; i < a.length(); i++) cfg.causaliControvaloreConCommissione.add(a.getString(i));
                }
            }

            if (root.has("mappaCausali")) {
                JSONObject mc = root.getJSONObject("mappaCausali");
                for (String k : mc.keySet()) {
                    cfg.mappaCausali.put(k, mc.getString(k));
                }
            }
            if (root.has("causaliUscita")) {
                JSONArray arr = root.getJSONArray("causaliUscita");
                for (int i = 0; i < arr.length(); i++) {
                    cfg.causaliUscita.add(arr.getString(i));
                }
            }
            if (root.has("causaliEntrata")) {
                JSONArray arr = root.getJSONArray("causaliEntrata");
                for (int i = 0; i < arr.length(); i++) {
                    cfg.causaliEntrata.add(arr.getString(i));
                }
            }

            // NUOVO: causaliChiuse (movimentiChiuso nel JSON per compatibilità)
            String keyChiuse = root.has("causaliChiuse") ? "causaliChiuse"
                    : root.has("movimentoChiuso") ? "movimentoChiuso" : null;
            if (keyChiuse != null) {
                JSONArray arr = root.getJSONArray(keyChiuse);
                for (int i = 0; i < arr.length(); i++) {
                    cfg.causaliChiuse.add(arr.getString(i));
                }
            }

            if (root.has("rinominaMonete")) {
                JSONObject rm = root.getJSONObject("rinominaMonete");
                for (String k : rm.keySet()) {
                    cfg.rinominaMonete.put(k, rm.getString(k));
                }
            }
            if (root.has("rimuoviCaseSensitive")) {
                cfg.rimuoviCaseSensitive = root.getBoolean("rimuoviCaseSensitive");
            }
            if (root.has("rimuoviDaNomeMoneta")) {
                JSONArray arr = root.getJSONArray("rimuoviDaNomeMoneta");
                for (int i = 0; i < arr.length(); i++) {
                    RimuoviRegola r = RimuoviRegola.parse(arr.getString(i));
                    if (r != null) {
                        cfg.rimuoviDaNomeMoneta.add(r);
                    }
                }
            }
            if (root.has("campiExtra")) {
                JSONObject ce = root.getJSONObject("campiExtra");
                for (String k : ce.keySet()) {
                    cfg.campiExtra.put(k, ce.getInt(k));
                }
            }
            if (root.has("walletPerCausale")) {
                JSONObject wc = root.getJSONObject("walletPerCausale");
                for (String k : wc.keySet()) {
                    cfg.walletPerCausale.put(k, wc.getString(k));
                }
            }
            if (root.has("separatoreCausale")) cfg.separatoreCausale = root.getString("separatoreCausale");
            if (root.has("causaliUppercase"))  cfg.causaliUppercase  = root.getBoolean("causaliUppercase");
            if (root.has("centralizzato"))     cfg.centralizzato     = root.getBoolean("centralizzato");
            if (root.has("mappaAutoDetect")) {
                JSONObject mad = root.getJSONObject("mappaAutoDetect");
                for (String k : mad.keySet()) cfg.mappaAutoDetect.put(k, mad.getString(k));
            }

            return cfg;
        }
        
    private ZoneId risolviFuso() {
    String f = fuso == null ? "" : fuso.trim();
    if (f.isBlank()) return ZoneId.systemDefault();

    try {
        if (f.equalsIgnoreCase("UTC")) return ZoneOffset.UTC;

        String up = f.toUpperCase();
        if (up.startsWith("UTC+") || up.startsWith("UTC-") || up.startsWith("GMT+") || up.startsWith("GMT-")) {
            String offset = f.substring(3).trim();
            // ZoneOffset.of richiede "+HH" (2 cifre), normalizzo "+1" → "+01"
            if (offset.matches("[+-]\\d")) {
                offset = offset.charAt(0) + "0" + offset.charAt(1);
            }
            return ZoneOffset.of(offset);
        }

        return ZoneId.of(f);
    } catch (Exception ex) {
        return ZoneId.systemDefault();
    }
}

private LocalDateTime parseDataRaw(String dataCSV) {
    if (dataCSV == null) return null;

    String s = dataCSV.trim();
    if (s.isBlank() || s.matches("-+")) return null;

    // Rimuove suffisso timezone tipo " +00:00" o " -05:30" se presente
    s = s.replaceAll("\\s[+-]\\d{2}:\\d{2}$", "");

    try {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(formatoData);

        if (formatoData.contains("yy") && !formatoData.contains("yyy")) {
            java.time.temporal.TemporalAccessor ta = fmt.parse(s);

            int anno;
            if (ta.isSupported(ChronoField.YEAR)) {
                anno = ta.get(ChronoField.YEAR);
            } else {
                int yy = ta.get(ChronoField.YEAR_OF_ERA);
                anno = (yy < 100) ? (2000 + yy) : yy;
            }

            int mese = ta.get(ChronoField.MONTH_OF_YEAR);
            int giorno = ta.get(ChronoField.DAY_OF_MONTH);
            int ora = ta.isSupported(ChronoField.HOUR_OF_DAY) ? ta.get(ChronoField.HOUR_OF_DAY) : 0;
            int min = ta.isSupported(ChronoField.MINUTE_OF_HOUR) ? ta.get(ChronoField.MINUTE_OF_HOUR) : 0;
            int sec = ta.isSupported(ChronoField.SECOND_OF_MINUTE) ? ta.get(ChronoField.SECOND_OF_MINUTE) : 0;

            if (anno < 100) anno += 2000;

            return LocalDateTime.of(anno, mese, giorno, ora, min, sec);
        }

        return LocalDateTime.parse(s, fmt);
    } catch (Exception ex) {
        return null;
    }
}

/**
 * Converte una data grezza letta dal CSV (formato configurabile via {@code risolviFuso()}/{@code parseDataRaw})
 * in millisecondi epoch, applicando il fuso orario di origine configurato per questo import.
 * @param dataCSV data grezza da convertire, così come letta dal CSV
 * @return i millisecondi epoch corrispondenti, oppure {@code 0L} se la data non è parsabile
 */
public long convertiDataInMillis(String dataCSV) {
    try {
        LocalDateTime ldt = parseDataRaw(dataCSV);
        if (ldt == null) return 0L;

        ZoneId zonaOrigine = risolviFuso();
        ZonedDateTime zdt = ldt.atZone(zonaOrigine);

        return zdt.toInstant().toEpochMilli();
    } catch (Exception ex) {
        return 0L;
    }
}

/**
 * Converte una data grezza letta dal CSV dal fuso orario di origine configurato per questo import al fuso
 * orario di sistema, restituendola nel formato standard interno {@code yyyy-MM-dd HH:mm:ss}.
 * @param dataCSV data grezza da normalizzare, così come letta dal CSV
 * @return la data normalizzata, oppure {@code null} se non è parsabile
 */
public String normalizzaData(String dataCSV) {
    try {
        LocalDateTime ldt = parseDataRaw(dataCSV);
        if (ldt == null) return null;

        ZoneId zonaOrigine = risolviFuso();
        ZoneId zonaDestinazione = ZoneId.systemDefault();

        LocalDateTime convertita = ldt.atZone(zonaOrigine)
                                     .withZoneSameInstant(zonaDestinazione)
                                     .toLocalDateTime();

        return convertita.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    } catch (Exception ex) {
        return null;
    }
}
        

        

        
        

        // -----------------------------------------------------------------
        // Risoluzione colonne da intestazione
        // -----------------------------------------------------------------
        /**
         * Costruisce la mappa nome-colonna -> indice a partire dalla riga di
         * intestazione del CSV.
         *
         * <p>
         * La mappa viene usata quando è attiva l'autodetection delle colonne,
         * così da poter riferire i campi del CSV tramite nome invece che
         * tramite posizione fissa.</p>
         *
         * @param intestazione array delle intestazioni del CSV
         */
        public void risolviColonneDaIntestazione(String[] intestazione) {
            for (int i = 0; i < intestazione.length; i++) {
                mappaNomiColonne.put(intestazione[i].trim(), i);
            }
            // Applica la mappa auto-detect: nome header → nome campo
            for (Map.Entry<String, String> e : mappaAutoDetect.entrySet()) {
                Integer idx = mappaNomiColonne.get(e.getKey().trim());
                if (idx == null) continue;
                switch (e.getValue().toLowerCase().trim()) {
                    case "data"          -> colonnaData          = idx;
                    case "causale"       -> colonnaCausale       = idx;
                    case "moneta"        -> colonnaMoneta        = idx;
                    case "quantita"      -> colonnaQuantita      = idx;
                    case "monetafee"     -> colonnaMonetaFee     = idx;
                    case "quantitafee"   -> colonnaQuantitaFee   = idx;
                    case "idtransazione" -> colonnaIDTransazione = idx;
                    case "idgruppo"      -> colonnaIDGruppo       = idx;
                    case "valoreeuro"    -> colonnaValoreEuro    = idx;
                    case "segno"         -> colonnaSegno         = idx;
                    case "wallet"        -> colonnaWallet        = idx;
                }
            }
        }

        /**
         * Restituisce la causale CSV dalla riga, componendola da una o più
         * colonne (colonnaCausale, colonnaCausale2, colonnaCausale3) separate
         * da {@code separatoreCausale}. Se {@code causaliUppercase} è true,
         * ogni parte viene convertita in maiuscolo prima della concatenazione.
         */
        public String getCausaleCSV(String[] riga) {
            String c1 = safe(riga, colonnaCausale);
            if (causaliUppercase) c1 = c1.toUpperCase();
            if (colonnaCausale2 < 0) return c1;
            String c2 = safe(riga, colonnaCausale2);
            if (causaliUppercase) c2 = c2.toUpperCase();
            StringBuilder sb = new StringBuilder(c1).append(separatoreCausale).append(c2);
            if (colonnaCausale3 >= 0) {
                String c3 = safe(riga, colonnaCausale3);
                if (causaliUppercase) c3 = c3.toUpperCase();
                sb.append(separatoreCausale).append(c3);
            }
            return sb.toString();
        }

        // -----------------------------------------------------------------
        // Conversione causale
        // -----------------------------------------------------------------
        /**
         * Converte una causale originale del CSV nella tipologia interna
         * dell'applicazione.
         *
         * <p>
         * La conversione avviene tramite match esatto sulla mappa definita nel
         * JSON. Se non viene trovata alcuna corrispondenza, il metodo
         * restituisce {@code null} e la riga potrà essere scartata o gestita
         * dal chiamante.</p>
         *
         * <p>
         * Il match parziale/contains è stato volutamente disabilitato per
         * evitare associazioni ambigue o accidentali tra causali simili.</p>
         *
         * @param causaleCSV causale originale presente nel file CSV
         * @return tipologia interna corrispondente, oppure {@code null} se non
         * mappata
         */
        public String convertiCausale(String causaleCSV) {
            if (causaleCSV == null) {
                return null;
            }
            String r = mappaCausali.get(causaleCSV.trim());
            if (r != null) {
                return r;
            }

            /*for (Map.Entry<String, String> e : mappaCausali.entrySet())
                if (causaleCSV.toLowerCase().contains(e.getKey().toLowerCase())) return e.getValue();*/
            return null;
        }

 

        /**
         * Normalizza la data leggendo il valore direttamente da una colonna
         * della riga CSV.
         *
         * @param riga riga CSV già splittata
         * @param col indice della colonna contenente la data
         * @return data normalizzata nel formato standard, oppure {@code null}
         * se non interpretabile
         */
        public String normalizzaData(String[] riga, int col) {
            return normalizzaData(safe(riga, col));
        }

        private static String safe(String[] arr, int idx) {
            if (arr == null || idx < 0 || idx >= arr.length || arr[idx] == null) {
                return "";
            }
            return arr[idx].trim();
        }

        // -----------------------------------------------------------------
        // Normalizzazione moneta
        // -----------------------------------------------------------------
        /**
         * Normalizza il simbolo moneta letto dal CSV.
         *
         * <p>
         * La normalizzazione può includere:</p>
         * <ul>
         * <li>rimozione di suffissi/prefissi indesiderati;</li>
         * <li>trim finale del valore;</li>
         * <li>rinomina del simbolo tramite la mappa
         * {@code rinominaMonete}.</li>
         * </ul>
         *
         * <p>
         * Questo permette di uniformare simboli diversi che rappresentano la
         * stessa moneta o di ripulire formati proprietari del CSV sorgente.</p>
         *
         * @param moneta simbolo moneta letto dal CSV
         * @return simbolo normalizzato
         */
        public String normalizzaMoneta(String moneta) {
    if (moneta == null) return "";
    String m = moneta.trim();
    if (m.isBlank()) return m;

    for (RimuoviRegola regola : rimuoviDaNomeMoneta) {
        if (regola == null || regola.parola == null || regola.parola.isBlank()) continue;

        // Trova la posizione della parola (con o senza case sensitive)
        int idx = rimuoviCaseSensitive
                ? m.indexOf(regola.parola)
                : m.toLowerCase().indexOf(regola.parola.toLowerCase());

        if (idx < 0) continue; // parola non trovata, salto

        switch (regola.modo) {
            case TRONCA_DOPO:
                // Tronca dalla parola in poi (la parola inclusa viene rimossa)
                m = m.substring(0, idx);
                break;
            case TRONCA_PRIMA:
                // Tronca tutto ciò che precede la parola (la parola inclusa viene rimossa)
                m = m.substring(idx + regola.parola.length());
                break;
            case NESSUNO:
            default:
                // Rimuove solo la parola, preserva il resto
                // Usa replace con la porzione esatta di m per rispettare il case originale
                String trovata = m.substring(idx, idx + regola.parola.length());
                m = m.replace(trovata, "");
                break;
        }
        m = m.trim();
    }

    // Rinomina finale
    String rin = rinominaMonete.get(m);
    return rin != null ? rin : m;
}

        // -----------------------------------------------------------------
        // Regola segno
        // -----------------------------------------------------------------
        /**
         * Restituisce la regola di segno da applicare ad una causale CSV.
         *
         * <p>
         * Se la causale appartiene a {@code causaliUscita}, il segno viene
         * forzato negativo. Se appartiene a {@code causaliEntrata}, il segno
         * viene forzato positivo. In tutti gli altri casi non viene applicata
         * alcuna forzatura.</p>
         *
         * @param causale causale originale del CSV
         * @return regola di segno da applicare
         */
        public RegolaSegno regolaSegno(String causale) {
            if (causale != null) {
                if (causaliUscita.contains(causale)) {
                    return RegolaSegno.FORZATO_USCITA;
                }
                if (causaliEntrata.contains(causale)) {
                    return RegolaSegno.FORZATO_ENTRATA;
                }
            }
            return RegolaSegno.NESSUNA;
        }

        /**
         * Regole possibili di forzatura del segno della quantità.
         *
         * <ul>
         * <li>{@code NESSUNA}: il segno resta quello originale del CSV;</li>
         * <li>{@code FORZATO_USCITA}: la quantità viene resa negativa;</li>
         * <li>{@code FORZATO_ENTRATA}: la quantità viene resa positiva.</li>
         * </ul>
         */
        public enum RegolaSegno {
            NESSUNA,
            FORZATO_USCITA,
            FORZATO_ENTRATA
        }
    }
    
    
      /**
 * Descrive una singola regola di pulizia del nome moneta.
 * - parola: il testo da cercare (senza il ?)
 * - modoTronca: NESSUNO = rimuovi solo la parola
 *               TRONCA_DOPO = rimuovi la parola e tutto ciò che viene dopo  (.STAKING?)
 *               TRONCA_PRIMA = rimuovi la parola e tutto ciò che viene prima  (?.STAKING)
 */
public static class RimuoviRegola {
    public enum Modo { NESSUNO, TRONCA_DOPO, TRONCA_PRIMA }
    public final String parola;
    public final Modo   modo;

    public RimuoviRegola(String parola, Modo modo) {
        this.parola = parola;
        this.modo   = modo;
    }

    /** Parsa una stringa dal JSON: "abc?", "?abc", "abc" */
    public static RimuoviRegola parse(String s) {
        if (s == null || s.isBlank()) return null;
        if (s.endsWith("?"))
            return new RimuoviRegola(s.substring(0, s.length() - 1), Modo.TRONCA_DOPO);
        if (s.startsWith("?"))
            return new RimuoviRegola(s.substring(1), Modo.TRONCA_PRIMA);
        return new RimuoviRegola(s, Modo.NESSUNO);
    }
}
    
}
