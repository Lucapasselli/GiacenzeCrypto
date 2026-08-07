package com.giacenzecrypto.giacenze_crypto;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;

/**
 * Mappe di conversione delle causali degli import nativi (causale grezza dell'exchange → categoria
 * interna), lette da file JSON invece che scritte nel codice.
 * <p>Il file JSON è l'unica fonte: nel codice non esiste più nessuna copia della mappa. Questo permette
 * di correggere o aggiungere una causale distribuendo un file, senza rilasciare una nuova versione del
 * programma. I file vivono in {@code config/importmappe/} e vengono allineati al repository all'avvio da
 * {@link Funzioni#AggiornamentoConfigDaRepository}; una copia di default è inclusa nel jar sotto
 * {@code /ImportMappe/} e viene usata come ripiego se il file su disco manca o è illeggibile.
 * <p>La mappa viene riletta da disco a ogni import, così un file aggiornato a mano ha effetto subito;
 * gli aggiornamenti scaricati dal repository, invece, sono scaricati in background e diventano effettivi
 * al riavvio successivo.
 *
 * @author luca.passelli
 */
public class MappeCausali {

    /** Import nativo del CSV Binance storico (voce {@code Binance_Old}) */
    public static final String BINANCE_OLD = "Binance_Old";
    /** Import nativo del Binance Financial/Tax Report */
    public static final String BINANCE_FINANCIAL_REPORT = "BinanceFinancialReport";
    /** Import OKX, condiviso fra CSV storico (voce {@code OKX_Old}) e import via API/CCXT */
    public static final String OKX = "OKX";
    /** Import nativo del CSV dell'app Crypto.com */
    public static final String CRYPTOCOM_APP = "CryptoCom_App";
    /** Import nativo del CSV di Crypto.com Exchange */
    public static final String CRYPTOCOM_EXCHANGE = "CryptoCom_Exchange";
    /** Import nativo del CSV Tatax vecchio formato (voce {@code Tatax_Old}) */
    public static final String TATAX_OLD = "Tatax_Old";

    /** Elenco delle mappe distribuite con il programma, usato per installare i default dal jar */
    static final String[] MAPPE_DI_SISTEMA = new String[]{
        BINANCE_OLD, BINANCE_FINANCIAL_REPORT, OKX, CRYPTOCOM_APP, CRYPTOCOM_EXCHANGE, TATAX_OLD
    };

    /** Cartella delle risorse nel jar che contiene le copie di default delle mappe */
    private static final String RISORSA_JAR = "/ImportMappe/";

    /**
     * Legge da disco la mappa causali indicata, con ripiego sulla copia inclusa nel jar.
     * <p>La lettura avviene a ogni chiamata: non c'è cache, così un file corretto a mano ha effetto
     * al primo import successivo senza riavviare.
     *
     * @param nome nome della mappa senza estensione, una delle costanti di questa classe
     * @return la mappa causale → categoria, case-insensitive come quelle che sostituisce, oppure
     *         {@code null} se la mappa non è disponibile né su disco né nel jar. Il chiamante
     *         <b>deve</b> interrompere l'import in quel caso: proseguire con una mappa vuota
     *         classificherebbe ogni movimento come sconosciuto.
     */
    public static Map<String, String> Carica(String nome) {
        Map<String, String> mappa = LeggiDaDisco(nome);
        if (mappa != null) {
            return mappa;
        }
        mappa = LeggiDaJar(nome);
        if (mappa != null) {
            System.out.println("MappeCausali: uso la copia di default nel jar per la mappa " + nome);
            return mappa;
        }
        LoggerGC.ScriviErrore("MappeCausali: mappa causali '" + nome + "' non disponibile, né in "
                + VarStatiche.getCartella_ConfigImportMappe() + " né fra le risorse del programma");
        return null;
    }

    /**
     * Messaggio da mostrare all'utente quando {@link #Carica(String)} non trova la mappa.
     * @param nome nome della mappa non disponibile
     * @return il testo dell'avviso
     */
    public static String MessaggioMappaNonDisponibile(String nome) {
        return "Impossibile eseguire l'importazione: manca la mappa delle causali \"" + nome + "\".\n\n"
                + "Il file dovrebbe trovarsi in:\n" + VarStatiche.getCartella_ConfigImportMappe() + nome + ".json\n\n"
                + "Riavviare il programma con la connessione a internet attiva per scaricarlo di nuovo.";
    }

    /**
     * Copia in {@code config/importmappe/} le mappe di default incluse nel jar che non sono già presenti
     * su disco. Da richiamare all'avvio, dopo la creazione delle cartelle: serve al primo avvio, anche
     * senza rete, e dà all'aggiornamento da repository un file locale con cui confrontare lo sha.
     * <p>I file già esistenti non vengono mai toccati, così un aggiornamento scaricato dal repository o
     * una modifica dell'utente non vengono sovrascritti dalla copia (più vecchia) contenuta nel jar.
     */
    public static void InstallaDefaultSeMancanti() {
        for (String nome : MAPPE_DI_SISTEMA) {
            try {
                Path destinazione = Paths.get(VarStatiche.getCartella_ConfigImportMappe(), nome + ".json");
                if (Files.exists(destinazione)) {
                    continue;
                }
                try (InputStream in = MappeCausali.class.getResourceAsStream(RISORSA_JAR + nome + ".json")) {
                    if (in == null) {
                        LoggerGC.ScriviErrore("MappeCausali: risorsa di default mancante nel jar per la mappa " + nome);
                        continue;
                    }
                    Files.createDirectories(destinazione.getParent());
                    Files.write(destinazione, in.readAllBytes());
                    System.out.println("MappeCausali: installata la mappa di default " + nome + ".json");
                }
            } catch (Exception ex) {
                LoggerGC.ScriviErrore(ex);
            }
        }
    }

    /** @return la mappa letta da {@code config/importmappe/<nome>.json}, oppure {@code null} se assente o non valida */
    private static Map<String, String> LeggiDaDisco(String nome) {
        try {
            Path percorso = Paths.get(VarStatiche.getCartella_ConfigImportMappe(), nome + ".json");
            if (!Files.exists(percorso)) {
                return null;
            }
            return Interpreta(new String(Files.readAllBytes(percorso), StandardCharsets.UTF_8), nome);
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return null;
        }
    }

    /** @return la mappa letta dalla risorsa {@code /ImportMappe/<nome>.json} del jar, oppure {@code null} */
    private static Map<String, String> LeggiDaJar(String nome) {
        try (InputStream in = MappeCausali.class.getResourceAsStream(RISORSA_JAR + nome + ".json")) {
            if (in == null) {
                return null;
            }
            return Interpreta(new String(in.readAllBytes(), StandardCharsets.UTF_8), nome);
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return null;
        }
    }

    /**
     * Converte il contenuto di un file mappa nella {@code TreeMap} case-insensitive attesa dagli import.
     * @param contenuto testo JSON del file
     * @param nome nome della mappa, solo per i messaggi di errore
     * @return la mappa, oppure {@code null} se il JSON non è valido o il blocco {@code mappaCausali} è vuoto
     */
    private static Map<String, String> Interpreta(String contenuto, String nome) {
        try {
            JSONObject root = new JSONObject(contenuto);
            if (!root.has("mappaCausali")) {
                LoggerGC.ScriviErrore("MappeCausali: il file della mappa " + nome + " non contiene il blocco mappaCausali");
                return null;
            }
            JSONObject mc = root.getJSONObject("mappaCausali");
            Map<String, String> mappa = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (String chiave : mc.keySet()) {
                mappa.put(chiave, mc.getString(chiave));
            }
            //Una mappa vuota è indistinguibile da un file corrotto e renderebbe sconosciuto ogni movimento
            if (mappa.isEmpty()) {
                LoggerGC.ScriviErrore("MappeCausali: la mappa " + nome + " è vuota, la considero non valida");
                return null;
            }
            return mappa;
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return null;
        }
    }
}
