package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Un numero scritto con la virgola decimale in un CSV separato da virgole (segnalazione di un utente, 2026-11-06, su un
 * CSV Binance di correzione): fra virgolette si legge, senza virgolette spezza il campo, con il punto delle migliaia
 * non è un numero. Fino al 2026-11-06 nei due ultimi casi la riga entrava con la quantità sbagliata ({@code 12,5} letto
 * 12) o spariva senza comparire fra gli scarti. Ora finisce fra le righe scartate del resoconto.
 */
class ImportazioneGenericaNumeriIlleggibiliTest {

    @TempDir
    static Path tempDir;

    private static final String INTESTAZIONE = "User_ID,UTC_Time,Account,Operation,Coin,Change,Remark\n";

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        //Un deposito senza controvalore fa consultare la cache dei prezzi: serve il DB
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
        System.setProperty("prezzi.servizio.abilitato", "false");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        System.clearProperty("prezzi.servizio.abilitato");
        Importazioni.AzzeraContatori();
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    /** Quantità dei movimenti importati, nell'ordine */
    private static List<String> importa(String csv, ConfigurazioneImport cfg) throws Exception {
        Path f = tempDir.resolve(System.nanoTime() + ".csv");
        Files.writeString(f, csv, StandardCharsets.UTF_8);
        Importazioni.AzzeraContatori();
        List<String[]> righe = ImportazioneGenerica.leggiCSV(f.toString(), cfg);
        righe = ImportazioneGenerica.scartaAnnullamenti(righe, cfg);
        righe = ImportazioneGenerica.consolidaCausaliPerGiorno(righe, cfg);
        List<String> quantita = new ArrayList<>();
        for (List<String[]> gruppo : ImportazioneGenerica.raggruppaRighe(righe, cfg)) {
            for (String[] m : ImportazioneGenerica.consolidaGruppo(gruppo, cfg, new ArrayList<>())) quantita.add(m[13]);
        }
        return quantita;
    }

    private static ConfigurazioneImport binance() throws Exception {
        return ConfigurazioneImport.carica("config/import/Binance CSV.json");
    }

    @Test
    void virgolaDecimale_fraVirgoletteSiLegge_altrimentiLaRigaEScartata() throws Exception {
        List<String> quantita = importa(INTESTAZIONE
                + "1,2024-01-01 10:00:00,Spot,Deposit,BTC,0.25,\n"
                + "1,2024-01-02 10:00:00,Spot,Deposit,BTC,\"0,5\",\n"
                + "1,2024-01-03 10:00:00,Spot,Deposit,BTC,12,5,\n"
                + "1,2024-01-04 10:00:00,Spot,Deposit,BTC,\"1.234,56\",\n"
                + "\"1\",\"2024-01-05 10:00:00\",\"Spot\",\"Deposit\",\"BTC\",\"7,75\",\"\"\n"
                //Remark pieno: una colonna piena in piu'
                + "1,2024-01-07 10:00:00,Spot,Deposit,BTC,4,5,correzione\n", binance());

        assertEquals(List.of("0.25", "0.5", "7.75"), quantita);
        String scarti = Importazioni.Sconosciuti();
        assertEquals(3, Importazioni.TrasazioniSconosciute, scarti);
        assertTrue(scarti.contains("NUMERO SPEZZATO DAL SEPARATORE") && scarti.contains("BTC,12,5,"), scarti);
        assertTrue(scarti.contains("PIU' COLONNE DELL'INTESTAZIONE (8 invece di 7)") && scarti.contains("correzione"), scarti);
        assertTrue(scarti.contains("NUMERO NON LEGGIBILE: 1.234,56"), scarti);
    }

    /**
     * Limite noto: una riga scritta a mano senza il separatore finale ha tanti campi quanti l'intestazione, e il 5 di
     * {@code 3,5} prende il posto di Remark. Non si riconosce: negli export veri di Bitget e KuCoin una commissione
     * {@code 0} seguita da un'altra colonna {@code 0} ha la stessa forma, e scartarle toglierebbe centinaia di righe buone.
     */
    @Test
    void rigaAPariCampi_conVirgolaDecimale_nonSiRiconosce() throws Exception {
        List<String> quantita = importa(INTESTAZIONE
                + "1,2024-01-06 10:00:00,Spot,Deposit,BTC,3,5\n", binance());
        assertEquals(List.of("3"), quantita);
        assertEquals("", Importazioni.Sconosciuti());
    }

    @Test
    void separatoriVuotiInCoda_eSegnapostoSenzaCifre_nonSonoScarti() throws Exception {
        //Alcuni export chiudono le righe di dati con separatori in piu', e scrivono "--" dove non c'e' un valore
        List<String> quantita = importa(INTESTAZIONE
                + "1,2024-01-01 10:00:00,Spot,Deposit,BTC,0.25,,,\n"
                + "1,2024-01-02 10:00:00,Spot,Deposit,BTC,--,\n", binance());

        assertEquals(List.of("0.25"), quantita);
        assertEquals("", Importazioni.Sconosciuti());
    }

    @Test
    void rigaDiTitoloAlPostoDellIntestazione_nessunControlloSulleColonne() throws Exception {
        //La riga 1 non copre le colonne lette: non e' l'intestazione, e confrontarla farebbe scartare tutto
        ConfigurazioneImport cfg = binance();
        cfg.righeIntestazione = 2;
        cfg.rigaIntestazione = 1;
        List<String> quantita = importa("Estratto conto Binance\n" + INTESTAZIONE
                + "1,2024-01-01 10:00:00,Spot,Deposit,BTC,0.25,nota,altra\n", cfg);

        assertEquals(List.of("0.25"), quantita);
        assertEquals("", Importazioni.Sconosciuti());
    }
}
