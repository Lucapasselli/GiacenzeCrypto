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
 * traslazione senza alcuna interazione con l'utente (i dialoghi di richiesta ore, di avviso sui
 * movimenti da blockchain e di scelta del ricalcolo prezzi vivono in
 * {@link Principale_TraslaOrario#TraslaOrario} e non sono testabili in un ambiente headless, stesso
 * schema di {@code Principale_Movimenti_SeparaUnisciTest} con {@code Esegui*}).</p>
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
    void campiCalcolatiDalMotoreVengonoSvuotati() {
        //16/19/33/38 (motore plusvalenze) e 31 (data fine trasferimento) devono restare vuoti sul
        //movimento traslato e non portarsi dietro lo stato vecchio: sono output del motore delle
        //plusvalenze, che li ricalcola al prossimo giro di AggiornaPlusvalenze
        String Deposito[] = movimento(DATA_ID + "_WalletTest_001_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "20000.00");
        Deposito[16] = "1000.00";
        Deposito[19] = "50.00";
        Deposito[31] = DATA;
        Deposito[33] = "50.00";
        Deposito[38] = "A";

        Principale_TraslaOrario.EseguiTraslazione(List.of(Deposito[0]), UN_ORA);

        String Traslato[] = MappaCryptoWallet.get("20240315113000_WalletTest_001_001_DC");
        for (int Campo : new int[]{16, 19, 31, 33, 38}) {
            assertEquals("", Traslato[Campo], "il campo " + Campo + " non deve essere riportato verbatim");
        }
    }

    @Test
    void campo35BackupPrezzoClassificazioneManuale_vieneConservatoVerbatim() {
        //Backup del prezzo pre-classificazione (ACQUISTO/DONAZIONE/PRESTITO/LIQUIDAZIONE): non è
        //output del motore, è classificazione manuale dell'utente e deve sopravvivere alla traslazione
        String Acquisto[] = movimento(DATA_ID + "_WalletTest_001_001_DC", "ACQUISTO CRYPTO",
                "", "", "", "ETH", "Crypto", "8", "20000.00");
        Acquisto[35] = "999.00";

        Principale_TraslaOrario.EseguiTraslazione(List.of(Acquisto[0]), UN_ORA);

        String Traslato[] = MappaCryptoWallet.get("20240315113000_WalletTest_001_001_DC");
        assertEquals("999.00", Traslato[35]);
    }

    @Test
    void campo17CostoDiCaricoDonazione_vieneConservatoVerbatimSoloSeDDO() {
        //Campo 17 ha un doppio uso: per i DDO è il costo di carico della donazione inserito a mano
        //(input utente, va conservato); per tutti gli altri movimenti è output del motore (va svuotato)
        String Donazione[] = movimento(DATA_ID + "_WalletTest_001_001_DC", "DONAZIONE",
                "", "", "", "ETH", "Crypto", "8", "20000.00");
        Donazione[18] = "DDO - Donazione";
        Donazione[17] = "18000.00";

        String NonDDO[] = movimento(DATA_ID + "_WalletTest_002_001_DC", "DEPOSITO CRYPTO",
                "", "", "", "BTC", "Crypto", "1", "20000.00");
        NonDDO[17] = "18000.00";

        Principale_TraslaOrario.EseguiTraslazione(List.of(Donazione[0], NonDDO[0]), UN_ORA);

        assertEquals("18000.00", MappaCryptoWallet.get("20240315113000_WalletTest_001_001_DC")[17],
                "per i DDO il campo 17 è input utente e va conservato");
        assertEquals("", MappaCryptoWallet.get("20240315113000_WalletTest_002_001_DC")[17],
                "per gli altri movimenti il campo 17 è output del motore e va svuotato");
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
    void movimentoConCommissioneCollegata_traslaEntrambiEPreservaLaClassificazione() {
        //Un prelievo con una commissione automatica collegata: la commissione ha lo stesso segmento[0]
        //(timestamp) del prelievo, quindi è un membro "posseduto" e si trasla insieme a lui invece di
        //essere sciolta
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.50", "", "", "", "20000.00");
        Prelievo[18] = "PCO - CASHOUT O SIMILARE";
        Prelievo[20] = DATA_ID + "_WalletTest_002_001_CM";

        String Commissione[] = movimento(DATA_ID + "_WalletTest_002_001_CM", "COMMISSIONE",
                "BTC", "Crypto", "-0.01", "", "", "", "400.00");
        Commissione[22] = "AU";
        Commissione[20] = Prelievo[0];

        int Traslati = Principale_TraslaOrario.EseguiTraslazione(List.of(Prelievo[0]), UN_ORA);

        assertEquals(1, Traslati);
        assertNull(MappaCryptoWallet.get(Prelievo[0]), "l'ID originale del prelievo sparisce");
        assertNull(MappaCryptoWallet.get(Commissione[0]), "l'ID originale della commissione sparisce (traslata, non eliminata)");

        String PrelievoTraslato[] = MappaCryptoWallet.get("20240315113000_WalletTest_001_001_PC");
        assertNotNull(PrelievoTraslato, "il prelievo ricompare con l'ID traslato");
        assertEquals("PCO - CASHOUT O SIMILARE", PrelievoTraslato[18], "la classificazione manuale sopravvive");

        String CommissioneTraslata[] = MappaCryptoWallet.get("20240315113000_WalletTest_002_001_CM");
        assertNotNull(CommissioneTraslata, "la commissione ricompare traslata dello stesso delta");
        assertEquals("-0.01", CommissioneTraslata[10], "la commissione viene replicata verbatim, non ricalcolata");
        assertEquals(CommissioneTraslata[0], PrelievoTraslato[20], "il prelievo punta al nuovo ID della commissione");
        assertEquals(PrelievoTraslato[0], CommissioneTraslata[20], "la commissione punta al nuovo ID del prelievo");
    }

    @Test
    void entrambiIMembriDelGruppoSelezionati_ilSecondoUsaGiaIlRiferimentoAggiornato() {
        //Selezione multipla: prelievo e commissione selezionati insieme nello stesso batch. Traslando
        //prima il prelievo, il [20] della commissione (letto fresco, non da uno snapshot di inizio
        //batch) punta già al nuovo ID del prelievo quando tocca a lei
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "PRELIEVO CRYPTO",
                "BTC", "Crypto", "-0.50", "", "", "", "20000.00");
        Prelievo[18] = "PCO - CASHOUT O SIMILARE";
        Prelievo[20] = DATA_ID + "_WalletTest_002_001_CM";

        String Commissione[] = movimento(DATA_ID + "_WalletTest_002_001_CM", "COMMISSIONE",
                "BTC", "Crypto", "-0.01", "", "", "", "400.00");
        Commissione[22] = "AU";
        Commissione[20] = Prelievo[0];

        //La commissione è AU: non è "traslabile" come capofila, ma il prelievo la trascina con sé
        int Traslati = Principale_TraslaOrario.EseguiTraslazione(List.of(Prelievo[0], Commissione[0]), UN_ORA);

        assertEquals(1, Traslati, "solo il prelievo è un capofila valido: la commissione è AU");
        assertNull(MappaCryptoWallet.get(Commissione[0]));
        String CommissioneTraslata[] = MappaCryptoWallet.get("20240315113000_WalletTest_002_001_CM");
        assertNotNull(CommissioneTraslata, "la commissione viene comunque traslata, trascinata dal prelievo");
        assertNotNull(MappaCryptoWallet.get("20240315113000_WalletTest_001_001_PC"));
    }

    @Test
    void membroEsternoDelGruppo_restaFermoEVieneAggiornatoSoloIlRiferimentoIncrociato() {
        //Trasferimento tra wallet a quantità uguali (nessuna commissione/reward), con prelievo e
        //deposito registrati a orari diversi (segmenti[0] diversi, caso realistico: due exchange
        //diversi): traslare solo il prelievo non deve spostare il deposito, solo aggiornare il suo [20]
        String DataDeposito = "20240315120000"; // due ore dopo il prelievo
        String Prelievo[] = movimento(DATA_ID + "_WalletTest_001_001_PC", "TRASFERIMENTO TRA WALLET",
                "BTC", "Crypto", "-0.5", "", "", "", "20000.00");
        Prelievo[18] = "PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
        Prelievo[20] = DataDeposito + "_WalletTest_002_001_DC";

        String Deposito[] = movimento(DataDeposito + "_WalletTest_002_001_DC", "TRASFERIMENTO TRA WALLET",
                "", "", "", "BTC", "Crypto", "0.5", "20000.00");
        Deposito[18] = "DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
        Deposito[20] = Prelievo[0];

        Principale_TraslaOrario.EseguiTraslazione(List.of(Prelievo[0]), UN_ORA);

        String PrelievoTraslato[] = MappaCryptoWallet.get("20240315113000_WalletTest_001_001_PC");
        assertNotNull(PrelievoTraslato);

        String DepositoInvariato[] = MappaCryptoWallet.get(Deposito[0]);
        assertNotNull(DepositoInvariato, "il deposito, membro esterno, non viene spostato");
        assertEquals(PrelievoTraslato[0], DepositoInvariato[20], "il suo riferimento incrociato punta però al nuovo ID del prelievo");
        assertEquals(Deposito[0], PrelievoTraslato[20], "il prelievo continua a puntare al deposito, mai spostato");
    }

    @Test
    void membroEsternoConLoStessoTimestamp_restaFermoPerchéNonAU() {
        //Riproduce un bug segnalato dall'utente: prelievo e deposito di un trasferimento tra wallet
        //reale (PTW/DTW) nascono con lo STESSO segmento[0] (fotografano lo stesso istante), ma nessuno
        //dei due è "AU" (uno è "A", l'altro "M"). Il solo timestamp uguale non deve bastare a
        //trascinare la controparte: senza il controllo su [22]=="AU" il deposito verrebbe erroneamente
        //traslato insieme al prelievo selezionato
        String Prelievo[] = movimento(DATA_ID + "_MoonPay.Principale_001_001_PC", "TRASFERIMENTO TRA WALLET",
                "BNB", "Crypto", "-0.0656", "", "", "", "35.43");
        Prelievo[18] = "PTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
        Prelievo[22] = "M";
        Prelievo[20] = DATA_ID + "_0xWallet.BSC_001_001_DC";

        String Deposito[] = movimento(DATA_ID + "_0xWallet.BSC_001_001_DC", "TRASFERIMENTO TRA WALLET",
                "", "", "", "BNB", "Crypto", "0.0656", "35.43");
        Deposito[18] = "DTW - Trasferimento tra Wallet di proprietà (no plusvalenza)";
        Deposito[22] = "A";
        Deposito[20] = Prelievo[0];

        int Traslati = Principale_TraslaOrario.EseguiTraslazione(List.of(Prelievo[0]), UN_ORA);

        assertEquals(1, Traslati);
        String PrelievoTraslato[] = MappaCryptoWallet.get("20240315113000_MoonPay.Principale_001_001_PC");
        assertNotNull(PrelievoTraslato, "il prelievo selezionato si trasla");

        String DepositoInvariato[] = MappaCryptoWallet.get(Deposito[0]);
        assertNotNull(DepositoInvariato, "il deposito, non selezionato e non AU, non deve muoversi anche se condivide il timestamp");
        assertEquals(PrelievoTraslato[0], DepositoInvariato[20], "il suo riferimento incrociato punta però al nuovo ID del prelievo");
        assertEquals(Deposito[0], PrelievoTraslato[20], "il prelievo continua a puntare al deposito, mai spostato");
    }

    // =============================================================================================
    // GRUPPI A PIÙ DI DUE MEMBRI (rientro da Vault/Collaterale, scambio differito)
    // =============================================================================================

    @Test
    void rientroDaVaultConMirrorECorrettivo_vengonoReplicatiVerbatimSenzaRiscansione() {
        //Un deposito "rientro da Vault" con un mirror PC (sub-wallet Vault) e un correttivo/reward DC
        //(giacenza negativa compensata in fase di classificazione): entrambi condividono il segmento[0]
        //del deposito, quindi si traslano con lui. Nessuna riscansione della mappa: gli importi devono
        //restare identici a quelli già decisi in fase di classificazione, solo spostati nel tempo
        String Deposito[] = movimento(DATA_ID + "_WalletTest_001_001_DC", "TRASFERIMENTO DA PIATTAFORMA",
                "", "", "", "ETH", "Crypto", "10", "20000.00");
        Deposito[18] = "PTW - Trasferimento Interno";
        Deposito[20] = DATA_ID + "_WalletTest_002_001_PC," + DATA_ID + "_WalletTest_003_001_DC";

        String MirrorPC[] = movimento(DATA_ID + "_WalletTest_002_001_PC", "TRASFERIMENTO DA PIATTAFORMA",
                "ETH", "Crypto", "-10", "", "", "", "20000.00");
        MirrorPC[18] = "PTW - Trasferimento Interno";
        MirrorPC[22] = "AU";
        MirrorPC[20] = Deposito[0] + "," + DATA_ID + "_WalletTest_003_001_DC";

        String Correttivo[] = movimento(DATA_ID + "_WalletTest_003_001_DC", "REWARD",
                "", "", "", "ETH", "Crypto", "0.3", "6000.00");
        Correttivo[18] = "DAI - Reward";
        Correttivo[22] = "AU";
        Correttivo[20] = Deposito[0] + "," + MirrorPC[0];

        int Traslati = Principale_TraslaOrario.EseguiTraslazione(List.of(Deposito[0]), UN_ORA);

        assertEquals(1, Traslati);
        String DepositoT[] = MappaCryptoWallet.get("20240315113000_WalletTest_001_001_DC");
        String MirrorT[] = MappaCryptoWallet.get("20240315113000_WalletTest_002_001_PC");
        String CorrettivoT[] = MappaCryptoWallet.get("20240315113000_WalletTest_003_001_DC");
        assertNotNull(DepositoT);
        assertNotNull(MirrorT, "il mirror sul Vault si trasla insieme al deposito");
        assertNotNull(CorrettivoT, "il correttivo/reward si trasla insieme al deposito, replicato non ricalcolato");
        assertEquals("-10", MirrorT[10], "quantità del mirror invariata");
        assertEquals("0.3", CorrettivoT[13], "quantità del correttivo invariata");
        assertEquals("6000.00", CorrettivoT[15], "prezzo del correttivo invariato, nessun ricalcolo");

        //[20] rimappato su tutti e tre verso i nuovi ID reciproci
        assertTrue(DepositoT[20].contains(MirrorT[0]) && DepositoT[20].contains(CorrettivoT[0]));
        assertTrue(MirrorT[20].contains(DepositoT[0]) && MirrorT[20].contains(CorrettivoT[0]));
        assertTrue(CorrettivoT[20].contains(DepositoT[0]) && CorrettivoT[20].contains(MirrorT[0]));
    }

    @Test
    void scambioDifferito_traslareSoloIlPrelievoMuoveSoloLuiEIlTrasferimentoAssociato() {
        //Scambio differito a 5 movimenti: Prelievo(00)+Trasferimento1(01) condividono il timestamp
        //originale del prelievo; Scambio(02)+Trasferimento2(03)+Deposito(04) quello del deposito.
        //Traslare solo il prelievo deve muovere lui e Trasferimento1, lasciando fermi gli altri tre
        //(con [20] aggiornato)
        String T1 = DATA_ID; // timestamp originale del prelievo
        String T2 = "20240315120000"; // timestamp originale del deposito, ore dopo

        String Prelievo[] = movimento(T1 + "_00WalletTest_001_001_PC", "TRASFERIMENTO PER SCAMBIO",
                "USDT", "Crypto", "-1000", "", "", "", "1.00");
        String Trasf1[] = movimento(T1 + "_01WalletTest_001_001_DC", "TRASFERIMENTO PER SCAMBIO",
                "", "", "", "USDT", "Crypto", "1000", "1.00");
        Trasf1[22] = "AU";
        String Scambio[] = movimento(T2 + "_02WalletTest_001_001_SC", "SCAMBIO CRYPTO",
                "USDT", "Crypto", "-1000", "ETH", "Crypto", "0.5", "2000.00");
        Scambio[22] = "AU";
        String Trasf2[] = movimento(T2 + "_03WalletTest_001_001_PC", "TRASFERIMENTO PER SCAMBIO",
                "ETH", "Crypto", "-0.5", "", "", "", "2000.00");
        Trasf2[22] = "AU";
        String Deposito[] = movimento(T2 + "_04WalletTest_001_001_DC", "TRASFERIMENTO PER SCAMBIO",
                "", "", "", "ETH", "Crypto", "0.5", "2000.00");

        String Lista = Prelievo[0] + "," + Trasf1[0] + "," + Scambio[0] + "," + Trasf2[0] + "," + Deposito[0];
        for (String[] M : List.of(Prelievo, Trasf1, Scambio, Trasf2, Deposito)) {
            //[20] di ciascuno: la lista completa degli altri quattro
            String Altri[] = Lista.split(",");
            StringBuilder Sb = new StringBuilder();
            for (String Altro : Altri) {
                if (!Altro.equals(M[0])) {
                    if (Sb.length() > 0) Sb.append(",");
                    Sb.append(Altro);
                }
            }
            M[20] = Sb.toString();
        }

        int Traslati = Principale_TraslaOrario.EseguiTraslazione(List.of(Prelievo[0]), UN_ORA);

        assertEquals(1, Traslati);
        assertNull(MappaCryptoWallet.get(Prelievo[0]));
        assertNull(MappaCryptoWallet.get(Trasf1[0]));
        String PrelievoT[] = MappaCryptoWallet.get("20240315113000_00WalletTest_001_001_PC");
        String Trasf1T[] = MappaCryptoWallet.get("20240315113000_01WalletTest_001_001_DC");
        assertNotNull(PrelievoT, "il prelievo si trasla");
        assertNotNull(Trasf1T, "trasferimento1 si trasla insieme al prelievo (stesso timestamp originale)");

        //Scambio, Trasferimento2 e Deposito restano fermi ai loro ID originali...
        assertNotNull(MappaCryptoWallet.get(Scambio[0]), "lo scambio, timestamp del deposito, non si sposta");
        assertNotNull(MappaCryptoWallet.get(Trasf2[0]), "trasferimento2, timestamp del deposito, non si sposta");
        assertNotNull(MappaCryptoWallet.get(Deposito[0]), "il deposito non si sposta");

        //...ma il loro [20] è stato aggiornato per puntare ai nuovi ID di prelievo e trasferimento1
        String ScambioT[] = MappaCryptoWallet.get(Scambio[0]);
        assertTrue(ScambioT[20].contains(PrelievoT[0]) && ScambioT[20].contains(Trasf1T[0]),
                "il riferimento incrociato dello scambio punta ai nuovi ID di prelievo/trasferimento1");
        assertFalse(ScambioT[20].contains(Prelievo[0]) || ScambioT[20].contains(Trasf1[0]),
                "non deve restare alcun riferimento ai vecchi ID");
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
