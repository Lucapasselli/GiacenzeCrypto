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
    return importa(fileCSV, fileConfigurazione, sovrascriEsistenti, progressb, null);
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
            boolean sovrascriEsistenti, Download progressb,String nomeExchangeOverride) {

        Importazioni.AzzeraContatori();

        ConfigurazioneImport cfg;
        try {
            cfg = ConfigurazioneImport.carica(fileConfigurazione);
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return false;
        }

        // Applico l'override se fornito
    if (nomeExchangeOverride != null && !nomeExchangeOverride.isBlank()) {
        cfg.nomeExchange = nomeExchangeOverride;
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

        if (progressb != null) {
            progressb.SetMassimo(righe.size());
            progressb.SetAvanzamento(0);
        }

        List<String[]> listaCompleta = new ArrayList<>();
        List<String[]> gruppoCorrente = new ArrayList<>();
        List<String[]> movimentiDifferiti = new ArrayList<>();

        String idGruppoCorrente = null;
        long tsUltimaRigaGruppo = -1;

        for (int i = 0; i < righe.size(); i++) {
            if (progressb != null) {
                progressb.SetAvanzamento(i + 1);
            }
            if (progressb != null && progressb.FineThread) {
                return false;
            }

            String[] riga = righe.get(i);

            boolean usaIDTransazione = cfg.colonnaIDTransazione >= 0;

            String idCorrente = usaIDTransazione
                    ? safe(riga, cfg.colonnaIDTransazione)
                    : "";

            String dataCsvCorrente = safe(riga, cfg.colonnaData);
            long tsCorrente = cfg.convertiDataInMillis(dataCsvCorrente);

            if (gruppoCorrente.isEmpty()) {
                gruppoCorrente.add(riga);
                idGruppoCorrente = idCorrente;
                tsUltimaRigaGruppo = tsCorrente;
                continue;
            }

            String causaleCorrente = safe(riga, cfg.colonnaCausale);
            String[] ultimaRigaGruppo = gruppoCorrente.get(gruppoCorrente.size() - 1);
            String causaleUltima = safe(ultimaRigaGruppo, cfg.colonnaCausale);

            boolean usaDifferitaGlobale = cfg.causaliDifferite.isEmpty();
            boolean coinvolgeDifferita = usaDifferitaGlobale
                    || cfg.causaliDifferite.contains(causaleCorrente)
                    || cfg.causaliDifferite.contains(causaleUltima);
            long tolleranzaMs = coinvolgeDifferita ? cfg.tolleranzaSecondiConsolidamento * 1000L : 0L;


            boolean stessoID = !usaIDTransazione
                    || (!idCorrente.isBlank() && idCorrente.equals(idGruppoCorrente));

            //vedo se è dentro la tolleranza solamente se consolidastessariga è true
            boolean entroTolleranza = cfg.consolidaRigheStessaData && Math.abs(tsCorrente - tsUltimaRigaGruppo) <= tolleranzaMs;

            if (stessoID && entroTolleranza) {
                gruppoCorrente.add(riga);
                tsUltimaRigaGruppo = tsCorrente;
            } else {
                listaCompleta.addAll(consolidaGruppo(gruppoCorrente, cfg, movimentiDifferiti));
                gruppoCorrente = new ArrayList<>();
                gruppoCorrente.add(riga);
                idGruppoCorrente = idCorrente;
                tsUltimaRigaGruppo = tsCorrente;
            }
        }

        if (!gruppoCorrente.isEmpty()) {
            listaCompleta.addAll(consolidaGruppo(gruppoCorrente, cfg, movimentiDifferiti));
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
        risultato.sort((a, b) -> {
            long da = cfg.convertiDataInMillis(safe(a, cfg.colonnaData));
            long db = cfg.convertiDataInMillis(safe(b, cfg.colonnaData));
            return Long.compare(da, db);
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
    private static List<String[]> consolidaGruppo(List<String[]> gruppo,
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
        boolean haMovimentiDefi = false;

        for (String[] riga : gruppo) {
            String causaleCSV = safe(riga, cfg.colonnaCausale);
            String tipoMovimento = cfg.convertiCausale(causaleCSV);

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

            // Recupero prezzo se disponibile
            String valEuro = normalizzaNumero(safe(riga, cfg.colonnaValoreEuro));
            String prezzoUn = normalizzaNumero(safe(riga, cfg.colonnaPrezzo));
            long dataLong = cfg.convertiDataInMillis(dataRawDiGruppo);

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
                risultato.addAll(movScambio);
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
    private static List<String[]> costruisciMovimenti(String[] riga, String tipoForzato, ConfigurazioneImport cfg) {
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

        // Lato principale letto da moneta/quantita
        if (!qtaStr.isBlank() && Funzioni.isNumeric(qtaStr, false)) {
            BigDecimal qta = new BigDecimal(qtaStr);
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
                if (qOut.compareTo(BigDecimal.ZERO) > 0) {
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

        String prezzoMov = !valoreEuro.isBlank() ? valoreEuro : prezzo;

        // Fee: sempre trattata come valore assoluto positivo di lavoro
        boolean haFee = !qtaFee.isBlank()
                && !monetaFee.isBlank()
                && Funzioni.isNumeric(qtaFee, false)
                && new BigDecimal(qtaFee).compareTo(BigDecimal.ZERO) != 0;

        BigDecimal qtaFeeBD = BigDecimal.ZERO;
        if (haFee) {
            qtaFeeBD = new BigDecimal(qtaFee).abs();

            String nomeMonetaOut = mOUT != null ? mOUT.GetNome() : "";
            String nomeMonetaIn = mIN != null ? mIN.GetNome() : "";

            boolean feeSuMonetaUscita = mOUT != null && monetaFee.equalsIgnoreCase(nomeMonetaOut);
            boolean feeSuMonetaEntrata = mIN != null && monetaFee.equalsIgnoreCase(nomeMonetaIn);

            if (feeSuMonetaUscita && cfg.ricostruisciLordoSeFeeSuMonetaUscita) {
                // mOUT è già negativo: es. -485.86
                // Lordo corretto: -485.86 - 1.18 = -487.04
                BigDecimal qtaNettaOut = new BigDecimal(mOUT.GetQta());
                BigDecimal qtaLordaOut = qtaNettaOut.add(qtaFeeBD);
                mOUT.InserisciValori(nomeMonetaOut, qtaLordaOut.stripTrailingZeros().toPlainString(), "", "");
                mOUT.AssegnaTipoAuto();
            }

            if (feeSuMonetaEntrata && cfg.ricostruisciLordoSeFeeSuMonetaEntrata) {
                // mIN è positivo: es. 2130
                // Lordo corretto: 2130 + 1.18 = 2131.18
                BigDecimal qtaNettaIn = new BigDecimal(mIN.GetQta());
                BigDecimal qtaLordaIn = qtaNettaIn.add(qtaFeeBD);
                mIN.InserisciValori(nomeMonetaIn, qtaLordaIn.stripTrailingZeros().toPlainString(), "", "");
                mIN.AssegnaTipoAuto();
            }
        }

        // Movimento principale
        String[] rt = MovimentiCrypto.creaMovimento(
                mOUT, mIN, exchange, wallet, dataLong,
                prezzoMov, "CSV", 1, 1, null, null, "A",
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
                    if (campoMov >= 0 && campoMov < rt.length) {
                        rt[campoMov] = safe(riga, colCsv);
                    }
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
     * <li>rimuove spazi superflui;</li>
     * <li>converte la virgola decimale in punto;</li>
     * <li>trasforma la notazione scientifica in formato decimale espanso
     * tramite {@link BigDecimal#toPlainString()}.</li>
     * </ul>
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
        public int colonnaWallet = -1;
        public int colonnaMonetaUscita  = -1;
        public int colonnaQuantitaUscita = -1;
        
        //Questi 2 parametri servono per dire al programma come trattare le fee nei file che hanno tutto in una riga ovvero   
        // true  = ricostruisce il lordo + genera rigo commissione separato
        // false = genera solo il rigo commissione, non tocca la quantità principale
        public boolean ricostruisciLordoSeFeeSuMonetaUscita  = false;  // default: non ricostruisce il lordo su mOUT
        public boolean ricostruisciLordoSeFeeSuMonetaEntrata = false;  // default: non ricostruisce il lordo su mIN

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
                if (col.has("wallet")) {
                    cfg.colonnaWallet = col.getInt("wallet");
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

            return cfg;
        }
        
    private ZoneId risolviFuso() {
    String f = fuso == null ? "" : fuso.trim();
    if (f.isBlank()) return ZoneId.systemDefault();

    try {
        if (f.equalsIgnoreCase("UTC")) return ZoneOffset.UTC;

        String up = f.toUpperCase();
        if (up.startsWith("UTC+") || up.startsWith("UTC-")) {
            return ZoneOffset.of(f.substring(3).trim());
        }
        if (up.startsWith("GMT+") || up.startsWith("GMT-")) {
            return ZoneOffset.of(f.substring(3).trim());
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
