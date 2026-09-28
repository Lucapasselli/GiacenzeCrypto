package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Path;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Fissa {@link GirocontiFiat} : i giroconti in euro fra Coinbase retail e Coinbase Pro / GDAX
 * ({@code Exchange Deposit} sul retail, {@code deposit} su GDAX) diventano un trasferimento tra
 * wallet, e {@link Calcoli_RW_Fiat#isGirocontoStessoGruppo} smette di contarli come apporto quando i
 * due wallet stanno nello stesso gruppo.
 */
class GirocontiFiatTest {

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(),
                "Impossibile creare il database H2 temporaneo per i test");
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    @BeforeEach
    void svuotaMappa() {
        MappaCryptoWallet.clear();
    }

    /** Mappa_Wallet_Gruppo è statica e letta prima del DB : non deve arrivare ad altri test. */
    @AfterEach
    void rimuoveGruppi() {
        DatabaseH2.Pers_GruppoWallet_Cancella("Coinbase");
        DatabaseH2.Pers_GruppoWallet_Cancella("Coinbase Pro");
        MappaCryptoWallet.clear();
    }

    /** Regola del lato retail, come in config/import/Coinbase CSV.json. */
    private static GirocontiFiat.Regola regolaRetail() {
        return GirocontiFiat.Regola.daJson(new JSONObject("""
            {"controparte": "Coinbase Pro",
             "causali": ["Exchange Deposit", "Exchange Withdrawal", "Pro Deposit", "Pro Withdrawal"],
             "causaliControparte": ["deposit", "withdrawal"],
             "secondiTolleranza": 60}"""));
    }

    /** Regola del lato GDAX, come in config/import/Coinbase Pro GDAX.json. */
    private static GirocontiFiat.Regola regolaGdax() {
        return GirocontiFiat.Regola.daJson(new JSONObject("""
            {"controparte": "Coinbase",
             "causali": ["deposit", "withdrawal"],
             "causaliControparte": ["Exchange Deposit", "Exchange Withdrawal", "Pro Deposit", "Pro Withdrawal"],
             "secondiTolleranza": 60}"""));
    }

    /** PF o DF in EUR, a seconda della categoria dell'ID. */
    private static String[] fiat(String id, String exchange, String causale, String qta) {
        String[] v = new String[Importazioni.ColonneTabella];
        v[0] = id;
        v[1] = "2021-10-12 10:00";
        v[2] = "1 di 1";
        v[3] = exchange;
        v[4] = "Principale";
        v[7] = causale;
        if (id.endsWith("_PF")) {
            v[5] = "PRELIEVO FIAT";
            v[8] = "EUR"; v[9] = "FIAT"; v[10] = "-" + qta;
        } else {
            v[5] = "DEPOSITO FIAT";
            v[11] = "EUR"; v[12] = "FIAT"; v[13] = qta;
        }
        v[15] = qta;
        v[22] = "A";
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(id, v);
        return v;
    }

    @Test
    void ilGirocontoRetailVersoGdaxDiventaTrasferimentoTraWallet() {
        String[] banca = fiat("20211012100039_Coinbase_000_001_DF", "Coinbase", "Deposit", "1000");
        String[] uscita = fiat("20211012100041_Coinbase_001_001_PF", "Coinbase", "Exchange Deposit", "1000");
        String[] entrata = fiat("20211012100044_Coinbase Pro_000_001_DF", "Coinbase Pro", "deposit", "1000");

        assertEquals(1, GirocontiFiat.Abbina("Coinbase", regolaRetail()));

        assertTrue(uscita[18].startsWith("PTW"), uscita[18]);
        assertTrue(entrata[18].startsWith("DTW"), entrata[18]);
        assertEquals(entrata[0], uscita[20]);
        assertEquals(uscita[0], entrata[20]);
        assertTrue(banca[18].isBlank(), "il bonifico dalla banca resta un deposito FIAT vero");
        //la categoria non cambia : il saldo si sposta davvero da un wallet all'altro
        assertNotNull(MappaCryptoWallet.get(uscita[0]));
        assertNotNull(MappaCryptoWallet.get(entrata[0]));
    }

    @Test
    void laRegolaDelLatoGdaxTrovaLaStessaCoppia() {
        String[] uscita = fiat("20211012100041_Coinbase_001_001_PF", "Coinbase", "Exchange Deposit", "1000");
        String[] entrata = fiat("20211012100044_Coinbase Pro_000_001_DF", "Coinbase Pro", "deposit", "1000");

        assertEquals(1, GirocontiFiat.Abbina("Coinbase Pro", regolaGdax()));
        assertTrue(uscita[18].startsWith("PTW"));
        assertTrue(entrata[18].startsWith("DTW"));
        //una seconda passata non trova piu' nulla da abbinare
        assertEquals(0, GirocontiFiat.Abbina("Coinbase", regolaRetail()));
    }

    @Test
    void ilPrelievoDaGdaxVersoRetailSiAbbinaNelVersoOpposto() {
        String[] uscita = fiat("20221202175224_Coinbase Pro_001_001_PF", "Coinbase Pro", "withdrawal", "0.1");
        String[] entrata = fiat("20221202175224_Coinbase_000_001_DF", "Coinbase", "Pro Withdrawal", "0.1");

        assertEquals(1, GirocontiFiat.Abbina("Coinbase", regolaRetail()));
        assertTrue(uscita[18].startsWith("PTW"));
        assertTrue(entrata[18].startsWith("DTW"));
    }

    @Test
    void importoDiversoTempoFuoriTolleranzaOCausaleEstraneaNonSiAbbinano() {
        fiat("20211012100041_Coinbase_001_001_PF", "Coinbase", "Exchange Deposit", "1000");
        fiat("20211012100044_Coinbase Pro_000_001_DF", "Coinbase Pro", "deposit", "999");
        fiat("20211013100041_Coinbase_001_001_PF", "Coinbase", "Exchange Deposit", "500");
        fiat("20211013100541_Coinbase Pro_000_001_DF", "Coinbase Pro", "deposit", "500");
        //un bonifico dalla banca e un prelievo GDAX verso la banca nello stesso secondo : nessuno dei due e' un giroconto
        fiat("20211014100041_Coinbase_000_001_DF", "Coinbase", "Deposit", "300");
        fiat("20211014100041_Coinbase Pro_001_001_PF", "Coinbase Pro", "withdrawal", "300");

        assertEquals(0, GirocontiFiat.Abbina("Coinbase", regolaRetail()));
        for (String[] v : MappaCryptoWallet.values()) {
            assertTrue(v[18].isBlank(), v[0]);
        }
    }

    @Test
    void fraPiuCandidatiVinceIlPiuVicinoEOgnunoSiUsaUnaVolta() {
        String[] u1 = fiat("20180219221408_Coinbase_001_001_PF", "Coinbase", "Exchange Deposit", "10");
        String[] u2 = fiat("20180219221410_Coinbase_002_001_PF", "Coinbase", "Exchange Deposit", "10");
        String[] e1 = fiat("20180219221409_Coinbase Pro_000_001_DF", "Coinbase Pro", "deposit", "10");
        String[] e2 = fiat("20180219221411_Coinbase Pro_000_001_DF", "Coinbase Pro", "deposit", "10");

        assertEquals(2, GirocontiFiat.Abbina("Coinbase", regolaRetail()));
        assertEquals(e1[0], u1[20]);
        assertEquals(e2[0], u2[20]);
    }

    @Test
    void unGirocontoNelloStessoGruppoNonEUnApportoVersoUnAltroGruppoSi() {
        String[] uscita = fiat("20211012100041_Coinbase_001_001_PF", "Coinbase", "Exchange Deposit", "1000");
        String[] entrata = fiat("20211012100044_Coinbase Pro_000_001_DF", "Coinbase Pro", "deposit", "1000");
        String[] banca = fiat("20211012100039_Coinbase_000_001_DF", "Coinbase", "Deposit", "1000");
        GirocontiFiat.Abbina("Coinbase", regolaRetail());

        DatabaseH2.Pers_GruppoWallet_Scrivi("Coinbase", "Wallet 02");
        DatabaseH2.Pers_GruppoWallet_Scrivi("Coinbase Pro", "Wallet 02");
        assertTrue(Calcoli_RW_Fiat.isGirocontoStessoGruppo(uscita, "Wallet 02"));
        assertTrue(Calcoli_RW_Fiat.isGirocontoStessoGruppo(entrata, "Wallet 02"));
        assertFalse(Calcoli_RW_Fiat.isGirocontoStessoGruppo(banca, "Wallet 02"));

        DatabaseH2.Pers_GruppoWallet_Scrivi("Coinbase Pro", "Wallet 03");
        assertFalse(Calcoli_RW_Fiat.isGirocontoStessoGruppo(uscita, "Wallet 02"));
        assertFalse(Calcoli_RW_Fiat.isGirocontoStessoGruppo(entrata, "Wallet 03"));
    }

    @Test
    void laCommissioneCollegataNonContaComeControparte() {
        //PTW verso un altro gruppo, con in [20] anche una commissione sullo stesso wallet di partenza
        String[] uscita = fiat("20211012100041_Coinbase_001_001_PF", "Coinbase", "Exchange Deposit", "1000");
        String[] entrata = fiat("20211012100044_Coinbase Pro_000_001_DF", "Coinbase Pro", "deposit", "1000");
        String[] commissione = fiat("20211012100041_Coinbase_002_001_CM", "Coinbase", "fee", "1");
        uscita[18] = "PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
        entrata[18] = "DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
        uscita[20] = String.join(",", List.of(entrata[0], commissione[0]));
        DatabaseH2.Pers_GruppoWallet_Scrivi("Coinbase", "Wallet 02");
        DatabaseH2.Pers_GruppoWallet_Scrivi("Coinbase Pro", "Wallet 03");

        assertFalse(Calcoli_RW_Fiat.isGirocontoStessoGruppo(uscita, "Wallet 02"));
    }

    @Test
    void declassificareUnGirocontoRimetteLaDescrizioneFiatOriginale() {
        String[] uscita = fiat("20211012100041_Coinbase_001_001_PF", "Coinbase", "Exchange Deposit", "1000");
        String[] entrata = fiat("20211012100044_Coinbase Pro_000_001_DF", "Coinbase Pro", "deposit", "1000");
        GirocontiFiat.Abbina("Coinbase", regolaRetail());
        assertEquals("TRASFERIMENTO TRA WALLET", uscita[5]);

        //Come fa l'import con "sovrascrivi" o il "disassocia" del dialogo : si parte da una gamba sola
        GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale(
                (uscita[0] + "," + uscita[20]).split(","), uscita[0]);

        assertEquals("PRELIEVO FIAT", uscita[5]);
        assertEquals("DEPOSITO FIAT", entrata[5]);
        assertTrue(uscita[18].isBlank() && uscita[20].isBlank());
        assertTrue(entrata[18].isBlank() && entrata[20].isBlank());
        //e una nuova passata li riabbina
        assertEquals(1, GirocontiFiat.Abbina("Coinbase", regolaRetail()));
    }

    @Test
    void laRigaDelDialogoLeggeIlPrelievoFiatDallaGambaInUscita() {
        fiat("20211012100041_Coinbase_001_001_PF", "Coinbase", "Exchange Deposit", "1000");
        String[] riga = GUI_ClassificazioneMovimento.DammiRigaTabellaDaID("20211012100041_Coinbase_001_001_PF");
        assertEquals("EUR", riga[4]);
        assertEquals("-1000", riga[5]);
    }

    @Test
    void declassificareUnGirocontoConCommissioneRestituisceLImportoAlPrelievo() {
        String[] uscita = fiat("20211012100041_Coinbase_001_001_PF", "Coinbase", "Exchange Deposit", "1000");
        String[] entrata = fiat("20211012100044_Coinbase Pro_000_001_DF", "Coinbase Pro", "deposit", "999");
        GUI_ClassificazioneMovimento.CreaMovimentiTrasferimentosuWalletProprio(uscita[0], entrata[0]);
        assertEquals(0, new java.math.BigDecimal(uscita[10]).compareTo(new java.math.BigDecimal("-999")));
        String idCommissione = uscita[20].split(",")[1];
        assertNotNull(MappaCryptoWallet.get(idCommissione));

        GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale(
                (uscita[0] + "," + uscita[20]).split(","), uscita[0]);

        assertEquals(0, new java.math.BigDecimal(uscita[10]).compareTo(new java.math.BigDecimal("-1000")));
        assertEquals(0, new java.math.BigDecimal(uscita[15]).compareTo(new java.math.BigDecimal("1000")));
        assertNull(MappaCryptoWallet.get(idCommissione), "la commissione automatica sparisce");
        assertEquals("PRELIEVO FIAT", uscita[5]);
    }

    @Test
    void ilDialogoOffreAiFiatSoloIlGiroconto() {
        assertArrayEquals(new String[]{GUI_ClassificazioneMovimento.CB_NESSUNASELEZIONE, GUI_ClassificazioneMovimento.CB_PF_TRASFERIMENTO},
                GUI_ClassificazioneMovimento.OpzioniSingolo("20211012100041_Coinbase_001_001_PF"));
        assertArrayEquals(new String[]{GUI_ClassificazioneMovimento.CB_NESSUNASELEZIONE, GUI_ClassificazioneMovimento.CB_DF_TRASFERIMENTO},
                GUI_ClassificazioneMovimento.OpzioniSingolo("20211012100044_Coinbase Pro_000_001_DF"));
        assertSame(GUI_ClassificazioneMovimento.CB_DC_SINGOLO, GUI_ClassificazioneMovimento.OpzioniSingolo("20211012100044_Binance_000_001_DC"));
        assertSame(GUI_ClassificazioneMovimento.CB_PC_SINGOLO, GUI_ClassificazioneMovimento.OpzioniSingolo("20211012100044_Binance_000_001_PC"));
        //CompilaTabellaMovimetiAssociabili riconosce il trasferimento da questo testo
        assertTrue(GUI_ClassificazioneMovimento.CB_PF_TRASFERIMENTO.contains("TRASFERIMENTO TRA WALLET"));
        assertTrue(GUI_ClassificazioneMovimento.CB_DF_TRASFERIMENTO.contains("TRASFERIMENTO TRA WALLET"));
    }

    @Test
    void unGirocontoFiatAmmetteLaCommissioneMaNonUnImportoInPiu() {
        fiat("20211012100041_Coinbase_001_001_PF", "Coinbase", "Exchange Deposit", "1000");
        fiat("20211012100044_Coinbase Pro_000_001_DF", "Coinbase Pro", "deposit", "999");
        fiat("20211012100045_Coinbase Pro_000_002_DF", "Coinbase Pro", "deposit", "1000.01");
        assertTrue(GUI_ClassificazioneMovimento.QtaGirocontoFiatAmmessa(
                "20211012100041_Coinbase_001_001_PF", "20211012100044_Coinbase Pro_000_001_DF"));
        assertFalse(GUI_ClassificazioneMovimento.QtaGirocontoFiatAmmessa(
                "20211012100041_Coinbase_001_001_PF", "20211012100045_Coinbase Pro_000_002_DF"));
    }

    @Test
    void unFiatNonClassificatoNonEUnErroreMaSiPuoClassificare() {
        String[] uscita = fiat("20211012100041_Coinbase_001_001_PF", "Coinbase", "Exchange Deposit", "1000");
        //il contatore "non classificati" di Principale usa la variante senza FIAT
        assertFalse(Funzioni.isDepositoPrelievoClassificabile(null, uscita, false));
        assertTrue(Funzioni.isDepositoPrelievoClassificabile(null, uscita, true));
    }

    @Test
    void ilResocontoMostraIGirocontiAbbinatiSoloSeCeNeSono() {
        Importazioni.AzzeraContatori();
        assertEquals("", GirocontiFiat.TestoResoconto());
        GirocontiFiat.AbbinatiImportazione = 16;
        assertTrue(GirocontiFiat.TestoResoconto().contains("GIROCONTI FIAT ABBINATI : 16"));
        //la prossima importazione riparte da zero
        Importazioni.AzzeraContatori();
        assertEquals(0, GirocontiFiat.AbbinatiImportazione);
    }

    @Test
    void leConfigurazioniCoinbaseDichiaranoLaRegolaDaEntrambiILati() throws Exception {
        ImportazioneGenerica.ConfigurazioneImport retail =
                ImportazioneGenerica.ConfigurazioneImport.carica("config/import/Coinbase CSV.json");
        ImportazioneGenerica.ConfigurazioneImport gdax =
                ImportazioneGenerica.ConfigurazioneImport.carica("config/import/Coinbase Pro GDAX.json");

        assertEquals(gdax.nomeExchange, retail.girocontoFiat.controparte);
        assertEquals(retail.nomeExchange, gdax.girocontoFiat.controparte);
        assertEquals(retail.girocontoFiat.causali, gdax.girocontoFiat.causaliControparte);
        assertEquals(gdax.girocontoFiat.causali, retail.girocontoFiat.causaliControparte);
    }
}
