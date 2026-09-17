package com.giacenzecrypto.giacenze_crypto;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test della voce di menu contestuale "Trasla Orario" ({@link Principale_TraslaOrario}): sposta
 * avanti/indietro di N ore l'orario di uno o più movimenti selezionati.
 *
 * <p>I test invocano {@link Principale_TraslaOrario#EseguiTraslazione(List, long)}, che esegue la
 * traslazione senza alcuna interazione con l'utente (i dialoghi di richiesta ore e di conferma
 * scioglimento abbinamenti vivono in {@link Principale_TraslaOrario#TraslaOrario} e non sono
 * testabili in un ambiente headless, stesso schema di {@code Principale_Movimenti_SeparaUnisciTest}
 * con {@code Esegui*}).</p>
 */
class Principale_TraslaOrarioTest {

    private static final String DATA_ID = "20240315103000"; // 2024-03-15 10:30:00
    private static final String DATA = "2024-03-15 10:30";
    private static final long TIMESTAMP = 1710495000000L; // istante corrispondente a DATA_ID
    private static final long UN_ORA = 3_600_000L;

    @BeforeEach
    void svuotaMappa() {
        MappaCryptoWallet.clear();
        //La traslazione accoda una voce di storico: il buffer va azzerato fra un test e l'altro,
        //altrimenti le voci si accumulerebbero e verrebbero riversate dal primo salvataggio dei
        //movimenti fatto da un altro test nella stessa JVM
        MovimentiStorico.AzzeraBuffer();
    }

    /** Costruisce un movimento grezzo minimale e lo inserisce in mappa (stesso schema di Principale_Movimenti_SeparaUnisciTest). */
    private static String[] movimento(String ID, String Campo5,
            String MonetaU, String TipoU, String QtaU,
            String MonetaE, String TipoE, String QtaE, String Prezzo) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = DATA;
        v[2] = "1 di 1";
        v[3] = "Wallet Test";
        v[5] = Campo5;
        v[6] = (MonetaU + " -> " + MonetaE).trim();
        v[7] = "Causale originale";
        v[8] = MonetaU;
        v[9] = TipoU;
        v[10] = QtaU;
        v[11] = MonetaE;
        v[12] = TipoE;
        v[13] = QtaE;
        v[15] = Prezzo;
        v[18] = "";
        v[20] = "";
        v[21] = "Nota di prova";
        v[22] = "M";
        v[24] = "0xHashDiProva";
        v[29] = String.valueOf(TIMESTAMP);
        v[32] = "SI";
        v[40] = "|||Personalizzato";
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(ID, v);
        return v;
    }

    // =============================================================================================
    // STORICO DELLE MODIFICHE
    // =============================================================================================

    @Test
    void traslazione_accodaUnaVoceDiStoricoEPropagaIlLignaggio() {
        //Trasla Orario consuma un ID e ne crea un altro, quindi va registrata come una modifica. La
        //seconda metà del test è l'unico presidio automatico sulla PROPAGAZIONE del lignaggio: il campo
        //42 viene timbrato sul movimento vecchio e deve arrivare su quello nuovo tramite il ciclo di
        //ricopia "tutto tranne". Se qualcuno aggiungesse il 42 a CampiNonCopiabiliVerbatim, o spostasse
        //il timbro dopo il ciclo, il movimento traslato perderebbe la catena delle sue versioni senza
        //che nient'altro se ne accorga.
        String Scambio[] = movimento(DATA_ID + "_WalletTest_001_001_SC", "SCAMBIO CRYPTO",
                "BTC", "Crypto", "-0.5", "ETH", "Crypto", "8", "20000.00");

        int Traslati = Principale_TraslaOrario.EseguiTraslazione(List.of(Scambio[0]), 2 * UN_ORA);

        assertEquals(1, Traslati);
        assertEquals(1, MovimentiStorico.VociInAttesa(),
                "la traslazione deve accodare una voce di storico");
        //Solo accodata: finché l'utente non salva i movimenti non deve toccare il database

        String Lignaggio = Scambio[MovimentiStorico.CAMPO_LIGNAGGIO];
        assertFalse(Lignaggio.isBlank(), "la traslazione deve timbrare il lignaggio sul movimento");
        assertEquals(Lignaggio,
                MappaCryptoWallet.get("20240315123000_WalletTest_001_001_SC")[MovimentiStorico.CAMPO_LIGNAGGIO],
                "il movimento traslato deve portarsi dietro il lignaggio, altrimenti perde le sue versioni");
    }

    // =============================================================================================
    // TRASLAZIONE DI UN MOVIMENTO SENZA ABBINAMENTI
    // =============================================================================================

    @Test
    void movimentoSenzaAbbinamento_traslaIDDataEIstanteOnChain() {
        String Scambio[] = movimento(DATA_ID + "_WalletTest_001_001_SC", "SCAMBIO CRYPTO",
                "BTC", "Crypto", "-0.5", "ETH", "Crypto", "8", "20000.00");
        Scambio[41] = "12";

        int Traslati = Principale_TraslaOrario.EseguiTraslazione(List.of(Scambio[0]), 2 * UN_ORA);

        assertEquals(1, Traslati);
        assertNull(MappaCryptoWallet.get(DATA_ID + "_WalletTest_001_001_SC"), "l'ID originale deve sparire");

        String Traslato[] = MappaCryptoWallet.get("20240315123000_WalletTest_001_001_SC");
        assertNotNull(Traslato, "il movimento deve ricomparire con l'ID traslato di due ore");
        assertEquals("2024-03-15 12:30", Traslato[1]);
        assertEquals("BTC", Traslato[8]);
        assertEquals("-0.5", Traslato[10]);
        assertEquals("ETH", Traslato[11]);
        assertEquals("8", Traslato[13]);
        //Classificazione invariata: né descrizione né categoria devono cambiare per un semplice spostamento
        assertEquals("SCAMBIO CRYPTO", Traslato[5]);
        assertEquals("", Traslato[18]);
        //Prezzo/valorizzazione/fonte riportati verbatim, non ricalcolati
        assertEquals("20000.00", Traslato[15]);
        assertEquals("SI", Traslato[32]);
        assertEquals("|||Personalizzato", Traslato[40]);
        //Campi di provenienza conservati, documento di origine incluso
        assertEquals("Causale originale", Traslato[7]);
        assertEquals("0xHashDiProva", Traslato[24]);
        assertEquals("12", Traslato[41]);
        //L'istante on-chain trasla dello stesso delta, altrimenti resterebbe disallineato dal nuovo ID
        assertEquals(String.valueOf(TIMESTAMP + 2 * UN_ORA), Traslato[29]);
    }

    @Test
    void campoDiProvenienzaNonAncoraUsato_vieneCopiatoAutomaticamente() {
        //Il ciclo di ricopia è una blocklist, non una whitelist: un campo di provenienza aggiunto in
        //futuro alla riga movimento deve sopravvivere alla traslazione senza bisogno di aggiungerlo a
        //un elenco. Il segnaposto era il campo 42 finché era libero; da quando porta il lignaggio dello
        //storico ha un significato suo, e usarlo qui proverebbe solo che il lignaggio si propaga (cosa
        //che verifica già un altro test): si usa il 43, ancora inutilizzato.
        String Deposito[] = movimento(DATA_ID + "_WalletTest_001_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "20000.00");
        Deposito[43] = "valore futuro ipotetico";

        Principale_TraslaOrario.EseguiTraslazione(List.of(Deposito[0]), UN_ORA);

        assertEquals("valore futuro ipotetico", MappaCryptoWallet.get("20240315113000_WalletTest_001_001_DC")[43]);
    }

    @Test
    void campiCalcolatiDalMotoreEBackupPrezzo_nonVengonoCopiatiVerbatim() {
        //16/17/19/33/38 (motore plusvalenze), 31 (data fine trasferimento) e 35 (backup prezzo della
        //classificazione manuale) devono restare vuoti sul movimento traslato e non portarsi dietro lo
        //stato vecchio: la blocklist li esclude esplicitamente dalla copia automatica
        String Deposito[] = movimento(DATA_ID + "_WalletTest_001_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "20000.00");
        Deposito[16] = "1000.00";
        Deposito[17] = "1000.00";
        Deposito[19] = "50.00";
        Deposito[31] = DATA;
        Deposito[33] = "50.00";
        Deposito[35] = "999.00";
        Deposito[38] = "A";

        Principale_TraslaOrario.EseguiTraslazione(List.of(Deposito[0]), UN_ORA);

        String Traslato[] = MappaCryptoWallet.get("20240315113000_WalletTest_001_001_DC");
        for (int Campo : new int[]{16, 17, 19, 31, 33, 35, 38}) {
            assertEquals("", Traslato[Campo], "il campo " + Campo + " non deve essere riportato verbatim");
        }
    }

    @Test
    void traslazioneConOreFrazionarie_spostaDiMezzoraInAvanti() {
        String Deposito[] = movimento(DATA_ID + "_WalletTest_001_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "20000.00");

        Principale_TraslaOrario.EseguiTraslazione(List.of(Deposito[0]), UN_ORA / 2);

        assertNotNull(MappaCryptoWallet.get("20240315110000_WalletTest_001_001_DC"),
                "10:30 + 30 minuti deve dare 11:00:00");
    }

    @Test
    void traslazioneIndietro_conDeltaNegativo() {
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.5", "", "", "", "20000.00");

        Principale_TraslaOrario.EseguiTraslazione(List.of(Prelievo[0]), -UN_ORA);

        assertNotNull(MappaCryptoWallet.get("20240315093000_WalletTest_001_001_PC"),
                "10:30 - 1 ora deve dare 09:30:00");
    }

    // =============================================================================================
    // MOVIMENTI GENERATI AUTOMATICAMENTE (AU)
    // =============================================================================================

    @Test
    void movimentoAutomatico_nonVieneTraslato() {
        String Reward[] = movimento(DATA_ID + "_WalletTest_001_001_RW", "STAKING REWARD",
                "", "", "", "ETH", "Crypto", "0.01", "20.00");
        Reward[22] = "AU";

        int Traslati = Principale_TraslaOrario.EseguiTraslazione(List.of(Reward[0]), UN_ORA);

        assertEquals(0, Traslati, "i movimenti generati automaticamente non possono essere traslati");
        assertNotNull(MappaCryptoWallet.get(Reward[0]), "il movimento deve restare invariato al suo ID originale");
    }

    @Test
    void selezioneMista_traslaSoloIMovimentiNonAutomatici() {
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.5", "", "", "", "20000.00");
        String Reward[] = movimento(DATA_ID + "_WalletTest_002_001_RW", "STAKING REWARD",
                "", "", "", "ETH", "Crypto", "0.01", "20.00");
        Reward[22] = "AU";

        int Traslati = Principale_TraslaOrario.EseguiTraslazione(List.of(Prelievo[0], Reward[0]), UN_ORA);

        assertEquals(1, Traslati);
        assertNotNull(MappaCryptoWallet.get("20240315113000_WalletTest_001_001_PC"));
        assertNotNull(MappaCryptoWallet.get(Reward[0]), "il reward automatico resta al suo posto");
    }

    // =============================================================================================
    // ABBINAMENTI (campo 20)
    // =============================================================================================

    @Test
    void movimentoConCommissioneCollegata_sciogieIlGruppoEdEliminaLaCommissioneAutomatica() {
        //Un prelievo con una commissione automatica collegata: stessa struttura che
        //GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale sa già sciogliere
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.50", "", "", "", "20000.00");
        Prelievo[18] = "PCO - CASHOUT O SIMILARE";
        Prelievo[20] = DATA_ID + "_WalletTest_002_001_CM";

        String Commissione[] = movimento(DATA_ID + "_WalletTest_002_001_CM", "COMMISSIONE",
                "BTC", "Crypto", "-0.01", "", "", "", "400.00");
        Commissione[22] = "AU";

        int Traslati = Principale_TraslaOrario.EseguiTraslazione(List.of(Prelievo[0]), UN_ORA);

        assertEquals(1, Traslati);
        assertNull(MappaCryptoWallet.get(Commissione[0]), "la commissione automatica collegata viene eliminata");
        assertNull(MappaCryptoWallet.get(Prelievo[0]), "l'ID originale del prelievo sparisce");

        String Traslato[] = MappaCryptoWallet.get("20240315113000_WalletTest_001_001_PC");
        assertNotNull(Traslato, "il prelievo ricompare con l'ID traslato");
        assertEquals("", Traslato[18], "la classificazione manuale viene azzerata dallo scioglimento del gruppo");
        assertEquals("", Traslato[20], "l'abbinamento viene azzerato");
    }

    @Test
    void entrambiIMembriDelGruppoSelezionati_laCommissioneGiaConsumataVieneSaltataSenzaErrori() {
        //Caso di selezione multipla che ha motivato il guard "rileggi dalla mappa prima di agire":
        //il prelievo e la sua commissione automatica sono selezionati insieme. Disassociando il
        //prelievo (primo della lista, essendo una TreeMap sull'ID) la commissione viene già eliminata
        //da RiportaTransazioniASituazioneIniziale, quindi quando il ciclo la raggiunge deve limitarsi
        //a saltarla, senza sollevare eccezioni né ritentare di traslarla.
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.50", "", "", "", "20000.00");
        Prelievo[18] = "PCO - CASHOUT O SIMILARE";
        Prelievo[20] = DATA_ID + "_WalletTest_002_001_CM";

        String Commissione[] = movimento(DATA_ID + "_WalletTest_002_001_CM", "COMMISSIONE",
                "BTC", "Crypto", "-0.01", "", "", "", "400.00");
        Commissione[22] = "AU";

        int Traslati = Principale_TraslaOrario.EseguiTraslazione(List.of(Prelievo[0], Commissione[0]), UN_ORA);

        assertEquals(1, Traslati, "solo il prelievo viene traslato: la commissione è AU e comunque già consumata");
        assertNull(MappaCryptoWallet.get(Commissione[0]));
        assertNotNull(MappaCryptoWallet.get("20240315113000_WalletTest_001_001_PC"));
    }

    @Test
    void movimentoSenzaAbbinamento_none_haEffettiSuAltriMovimenti() {
        //Controllo di non regressione: RimuoviMovimentazioneXID va chiamata sempre, ma senza [20]
        //valorizzato deve limitarsi a spostare il movimento, senza toccarne altri
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.5", "", "", "", "20000.00");
        String AltroMovimento[] = movimento(DATA_ID + "_WalletTest_002_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "20000.00");

        Principale_TraslaOrario.EseguiTraslazione(List.of(Prelievo[0]), UN_ORA);

        assertNotNull(MappaCryptoWallet.get(AltroMovimento[0]), "un movimento non selezionato non deve essere toccato");
    }

    // =============================================================================================
    // COLLISIONI DI ID NEL BATCH
    // =============================================================================================

    @Test
    void collisioneConMovimentoEsistente_risoltaConIDUnivoco() {
        //B resta al suo posto (non è nella selezione); A viene traslato di un secondo e finirebbe
        //esattamente sull'ID di B se non fosse per getIDUnivoco
        String B[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.1", "", "", "", "1000.00");
        B[0] = "20240315103001_WalletTest_001_001_PC"; // un secondo dopo A, stesso wallet/progressivo/tipo
        MappaCryptoWallet.remove(DATA_ID + "_WalletTest_001_001_PC");
        MappaCryptoWallet.put(B[0], B);

        String A[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.2", "", "", "", "2000.00");

        int Traslati = Principale_TraslaOrario.EseguiTraslazione(List.of(A[0]), 1000L); // +1 secondo

        assertEquals(1, Traslati);
        assertNotNull(MappaCryptoWallet.get(B[0]), "B, non selezionato, deve restare invariato");
        assertEquals("-0.1", MappaCryptoWallet.get(B[0])[10]);

        //A deve esistere sotto un ID diverso da quello di B, con i suoi dati intatti
        String IDDiA = MappaCryptoWallet.keySet().stream()
                .filter(id -> id.startsWith("20240315103001_WalletTest_001_") && !id.equals(B[0]))
                .findFirst().orElse(null);
        assertNotNull(IDDiA, "il movimento traslato deve esistere sotto un ID reso univoco");
        assertEquals("-0.2", MappaCryptoWallet.get(IDDiA)[10]);
    }

    // =============================================================================================
    // CAMBIO ORA LEGALE
    // =============================================================================================

    @Test
    void traslazioneACavalloDelCambioOraLegale_usaAritmeticaSullIstanteAssoluto() {
        //Notte tra il 30 e il 31 marzo 2024: alle 02:00 CET si passa direttamente alle 03:00 CEST.
        //Un movimento delle 01:30 traslato di un'ora deve arrivare alle 03:30, non alle 02:30 (che quel
        //giorno non esiste): è aritmetica sull'istante assoluto, non sulle sole cifre dell'ora.
        String Movimento[] = movimento("20240331013000_WalletTest_001_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "20000.00");

        Principale_TraslaOrario.EseguiTraslazione(List.of(Movimento[0]), UN_ORA);

        assertNotNull(MappaCryptoWallet.get("20240331033000_WalletTest_001_001_DC"),
                "01:30 + 1 ora a cavallo del cambio ora legale deve dare 03:30, non 02:30");
    }
}
