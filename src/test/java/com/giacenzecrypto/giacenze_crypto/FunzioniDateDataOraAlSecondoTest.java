package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * {@link FunzioniDate#DataOraAlSecondo}: i secondi mostrati nella tabella movimenti vengono dall'ID, e solo quando sono
 * un orario vero. Dati inventati.
 */
class FunzioniDateDataOraAlSecondoTest {

    @Test
    void secondiDallID() {
        assertEquals("2025-03-04 10:22:37",
                FunzioniDate.DataOraAlSecondo("20250304102237_Wallet_001_001_DC", "2025-03-04 10:22"));
    }

    @Test
    void secondiZeroSonoUnDatoVero() {
        //I reward giornalieri di Binance arrivano alle 00:00:00 UTC: ":00" non e' mancanza di dato
        assertEquals("2025-03-04 02:00:00",
                FunzioniDate.DataOraAlSecondo("20250304020000_Wallet_001_001_RW", "2025-03-04 02:00"));
    }

    @Test
    void secondiSentinellaDelleVecchieRettifiche() {
        //99 e 60 servivano a ordinare le rettifiche, non sono un orario: data al minuto, nessun ":00" inventato
        assertEquals("2024-12-31 00:00",
                FunzioniDate.DataOraAlSecondo("20241231000099_Wallet_001_001_DC", "2024-12-31 00:00"));
        assertEquals("2024-11-08 00:59",
                FunzioniDate.DataOraAlSecondo("20241108005960_Wallet_001_001_PC", "2024-11-08 00:59"));
    }

    @Test
    void secondiOltreIlLimiteDelMinuto() {
        assertEquals("2025-03-04 10:22",
                FunzioniDate.DataOraAlSecondo("20250304102260_Wallet_001_001_DC", "2025-03-04 10:22"));
        assertEquals("2025-03-04 10:22:59",
                FunzioniDate.DataOraAlSecondo("20250304102259_Wallet_001_001_DC", "2025-03-04 10:22"));
    }

    @Test
    void minutoDellIDDiversoDaQuelloDelCampoData() {
        //Se [1] e ID non raccontano lo stesso minuto non si sa quale sia giusto: non si abbina un secondo a un altro minuto
        assertEquals("2025-03-04 10:23",
                FunzioniDate.DataOraAlSecondo("20250304102237_Wallet_001_001_DC", "2025-03-04 10:23"));
        assertEquals("2025-03-05 10:22",
                FunzioniDate.DataOraAlSecondo("20250304102237_Wallet_001_001_DC", "2025-03-05 10:22"));
        assertEquals("2024-03-04 10:22",
                FunzioniDate.DataOraAlSecondo("20250304102237_Wallet_001_001_DC", "2024-03-04 10:22"));
    }

    @Test
    void idCortoONonNumerico() {
        assertEquals("2025-03-04 10:22", FunzioniDate.DataOraAlSecondo("2025030410", "2025-03-04 10:22"));
        assertEquals("2025-03-04 10:22", FunzioniDate.DataOraAlSecondo("2025030410223x_W", "2025-03-04 10:22"));
        assertEquals("2025-03-04 10:22", FunzioniDate.DataOraAlSecondo("", "2025-03-04 10:22"));
        assertEquals("2025-03-04 10:22", FunzioniDate.DataOraAlSecondo(null, "2025-03-04 10:22"));
    }

    @Test
    void dataNonNellaForma() {
        assertEquals("", FunzioniDate.DataOraAlSecondo("20250304102237_W", null));
        //Gia' con i secondi: non si aggiungono una seconda volta
        assertEquals("2025-03-04 10:22:37",
                FunzioniDate.DataOraAlSecondo("20250304102237_W", "2025-03-04 10:22:37"));
        assertEquals("", FunzioniDate.DataOraAlSecondo(null));
        assertEquals("", FunzioniDate.DataOraAlSecondo(new String[]{"20250304102237_W"}));
    }

    @Test
    void daRigaDiMovimento() {
        String[] v = new String[Importazioni.ColonneTabella];
        v[0] = "20250304102237_Wallet_001_001_DC";
        v[1] = "2025-03-04 10:22";
        assertEquals("2025-03-04 10:22:37", FunzioniDate.DataOraAlSecondo(v));
    }

    @Test
    void ordineAlfabeticoDentroLoStessoMinuto() {
        //La colonna si ordina come testo: i secondi devono far ordinare come l'ID, e la riga senza secondi sta prima
        String a = FunzioniDate.DataOraAlSecondo("20250304102205_W", "2025-03-04 10:22");
        String b = FunzioniDate.DataOraAlSecondo("20250304102237_W", "2025-03-04 10:22");
        String senza = FunzioniDate.DataOraAlSecondo("20250304102299_W", "2025-03-04 10:22");
        org.junit.jupiter.api.Assertions.assertTrue(senza.compareTo(a) < 0 && a.compareTo(b) < 0);
    }
}
