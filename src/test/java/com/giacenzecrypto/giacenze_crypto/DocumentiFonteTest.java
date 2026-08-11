package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link DocumentiFonte}, la conservazione dei file da cui i movimenti sono stati importati e il
 * loro collegamento al campo {@code [41]}.
 *
 * <p>Gira su un database H2 temporaneo, come {@link DatabaseH2UpsertTest}, e sulla stessa working directory
 * temporanea: la cartella {@code DocumentiFonte/} viene quindi creata lì e non tocca i dati dell'utente.
 *
 * <p>Le cose che questi test difendono, tutte già costate un ragionamento in fase di progetto:
 * <ul>
 *   <li><b>l'impronta è quella del contenuto originale, non del file compresso</b>: gzip include il
 *       timestamp di modifica, quindi comprimere due volte lo stesso CSV produce due file diversi e usarne
 *       l'impronta romperebbe il riconoscimento dei reimport;</li>
 *   <li><b>{@code Annulla} è sicuro solo su una registrazione nuova</b>: reimportare un file già importato
 *       aggiunge zero movimenti <i>e</i> riusa l'id esistente, e cancellarlo lascerebbe orfani i movimenti
 *       della prima importazione;</li>
 *   <li><b>il punto di strozzatura non sovrascrive un {@code [41]} già valorizzato</b> e non esplode su
 *       righe più corte di 45;</li>
 *   <li><b>le chiavi API non devono comparire nel documento</b>, nemmeno quando viaggiano dentro l'URL
 *       degli explorer.</li>
 * </ul>
 */
class DocumentiFonteTest {

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
    void puliscePartenza() {
        for (DocumentiFonte.Documento d : DocumentiFonte.Elenco()) {
            DocumentiFonte.Annulla(d.Id);
        }
        Importazioni.DocumentoFonteCorrente = 0;
        Principale.MappaCryptoWallet.clear();
    }

    /** Crea un file di prova nella cartella temporanea. */
    private File fileConContenuto(String nome, String contenuto) throws Exception {
        File f = new File(tempDir.toFile(), nome);
        Files.writeString(f.toPath(), contenuto, StandardCharsets.UTF_8);
        return f;
    }

    /** Rilegge il contenuto di un documento conservato, scompattandolo. */
    private String contenutoConservato(int Id) throws Exception {
        File gz = DocumentiFonte.FileConservato(Id);
        assertNotNull(gz, "il documento " + Id + " dovrebbe essere conservato");
        try (InputStream in = new GZIPInputStream(new java.io.FileInputStream(gz))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ------------------------------------------------------------------
    // REGISTRAZIONE E COMPRESSIONE
    // ------------------------------------------------------------------

    @Test
    void registra_conservaIlFileCompressoEIlContenutoResta() throws Exception {
        File csv = fileConContenuto("movimenti.csv", "data;moneta;qta\n2026-01-01;BTC;0.5\n");

        DocumentiFonte.Registrazione R = DocumentiFonte.Registra(csv, DocumentiFonte.TIPO_CSV, "Prova");

        assertTrue(R.Id > 0, "la registrazione deve assegnare un id");
        assertTrue(R.Nuovo, "il primo documento è nuovo");

        File conservato = DocumentiFonte.FileConservato(R.Id);
        assertNotNull(conservato);
        assertTrue(conservato.getName().endsWith(".gz"),
                "il documento va conservato compresso: " + conservato.getName());
        assertEquals("data;moneta;qta\n2026-01-01;BTC;0.5\n", contenutoConservato(R.Id));
    }

    @Test
    void registra_ilNomeOriginaleNonPortaIlSuffissoGz() throws Exception {
        File csv = fileConContenuto("export_okx.csv", "riga\n");

        DocumentiFonte.Registrazione R = DocumentiFonte.Registra(csv, DocumentiFonte.TIPO_CSV, "OKX");

        DocumentiFonte.Documento d = DocumentiFonte.Leggi(R.Id);
        assertEquals("export_okx.csv", d.NomeOriginale,
                "il nome mostrato all'utente è quello che aveva il file, non quello dell'archivio");
        assertTrue(d.PercorsoRelativo.endsWith(".gz"));
        assertEquals(DocumentiFonte.TIPO_CSV, d.Tipo, "il tipo resta logico anche se il file è compresso");
    }

    @Test
    void registra_stessoContenutoDueVolte_riusaLoStessoDocumento() throws Exception {
        File primo = fileConContenuto("uno.csv", "contenuto identico\n");
        File secondo = fileConContenuto("due.csv", "contenuto identico\n");

        DocumentiFonte.Registrazione R1 = DocumentiFonte.Registra(primo, DocumentiFonte.TIPO_CSV, "Prova");
        DocumentiFonte.Registrazione R2 = DocumentiFonte.Registra(secondo, DocumentiFonte.TIPO_CSV, "Prova");

        assertEquals(R1.Id, R2.Id, "stesso contenuto = stesso documento, anche con nome file diverso");
        assertTrue(R1.Nuovo);
        assertFalse(R2.Nuovo, "il secondo non è nuovo: è stato riconosciuto per impronta");
        assertEquals(1, DocumentiFonte.Elenco().size(), "non deve esistere una seconda copia");
    }

    @Test
    void registra_contenutiDiversi_creanoDocumentiDistinti() throws Exception {
        DocumentiFonte.Registrazione R1 = DocumentiFonte.Registra(
                fileConContenuto("a.csv", "primo\n"), DocumentiFonte.TIPO_CSV, "Prova");
        DocumentiFonte.Registrazione R2 = DocumentiFonte.Registra(
                fileConContenuto("b.csv", "secondo\n"), DocumentiFonte.TIPO_CSV, "Prova");

        assertNotEquals(R1.Id, R2.Id);
        assertTrue(R2.Nuovo);
        assertEquals(2, DocumentiFonte.Elenco().size());
    }

    @Test
    void registraContenuto_scriveUnDocumentoPartendoDallaMemoria() throws Exception {
        DocumentiFonte.Registrazione R = DocumentiFonte.RegistraContenuto(
                "{\"a\":1}", "risposta.json", DocumentiFonte.TIPO_JSON, "API di prova");

        assertTrue(R.Id > 0);
        assertEquals("{\"a\":1}", contenutoConservato(R.Id));
    }

    @Test
    void registra_fileInesistente_nonRegistraNulla() {
        DocumentiFonte.Registrazione R = DocumentiFonte.Registra(
                new File(tempDir.toFile(), "questo_non_esiste.csv"), DocumentiFonte.TIPO_CSV, "Prova");

        assertEquals(0, R.Id, "senza file non c'è nulla da conservare");
        assertFalse(R.Nuovo);
        assertTrue(DocumentiFonte.Elenco().isEmpty());
    }

    // ------------------------------------------------------------------
    // ANNULLAMENTO — la regola che protegge i movimenti della prima importazione
    // ------------------------------------------------------------------

    @Test
    void annulla_eliminaCopiaERigaDiRegistro() throws Exception {
        DocumentiFonte.Registrazione R = DocumentiFonte.Registra(
                fileConContenuto("temporaneo.csv", "riga\n"), DocumentiFonte.TIPO_CSV, "Prova");
        File conservato = DocumentiFonte.FileConservato(R.Id);

        DocumentiFonte.Annulla(R.Id);

        assertNull(DocumentiFonte.Leggi(R.Id), "la riga di registro deve sparire");
        assertFalse(conservato.exists(), "anche la copia deve sparire");
    }

    @Test
    void chiudiRegistrazione_documentoNuovoSenzaMovimenti_vieneAnnullato() throws Exception {
        DocumentiFonte.Registrazione R = DocumentiFonte.Registra(
                fileConContenuto("vuoto.csv", "intestazione\n"), DocumentiFonte.TIPO_CSV, "Prova");

        DocumentiFonte.ChiudiRegistrazione(R, 0);

        assertNull(DocumentiFonte.Leggi(R.Id),
                "un import che non aggiunge nulla non deve lasciare un documento a cui nessuno punta");
    }

    @Test
    void chiudiRegistrazione_documentoRiusatoSenzaMovimenti_NONvieneAnnullato() throws Exception {
        File csv = fileConContenuto("gia_importato.csv", "riga\n");
        DocumentiFonte.Registrazione Prima = DocumentiFonte.Registra(csv, DocumentiFonte.TIPO_CSV, "Prova");
        DocumentiFonte.ChiudiRegistrazione(Prima, 7);   //la prima importazione ha aggiunto 7 movimenti

        //Reimportare lo stesso file: zero movimenti nuovi, ma l'id è quello di prima
        DocumentiFonte.Registrazione Seconda = DocumentiFonte.Registra(csv, DocumentiFonte.TIPO_CSV, "Prova");
        assertFalse(Seconda.Nuovo);
        DocumentiFonte.ChiudiRegistrazione(Seconda, 0);

        assertNotNull(DocumentiFonte.Leggi(Prima.Id),
                "annullare qui lascerebbe orfani i 7 movimenti della prima importazione");
        assertNotNull(DocumentiFonte.FileConservato(Prima.Id));
    }

    @Test
    void chiudiRegistrazione_sommaIMovimentiInveceDiSovrascriverli() throws Exception {
        File csv = fileConContenuto("ripetuto.csv", "riga\n");
        DocumentiFonte.Registrazione R = DocumentiFonte.Registra(csv, DocumentiFonte.TIPO_CSV, "Prova");

        DocumentiFonte.ChiudiRegistrazione(R, 3);
        DocumentiFonte.ChiudiRegistrazione(DocumentiFonte.Registra(csv, DocumentiFonte.TIPO_CSV, "Prova"), 2);

        assertEquals(5, DocumentiFonte.Leggi(R.Id).Movimenti);
    }

    // ------------------------------------------------------------------
    // IL CAMPO [41]
    // ------------------------------------------------------------------

    @Test
    void idDaCampo41_leggeUnIdSemplice() {
        assertEquals(12, DocumentiFonte.IdDaCampo41("12"));
        assertEquals(12, DocumentiFonte.IdDaCampo41(" 12 "));
    }

    @Test
    void idDaCampo41_tolleraIlSottoCampoDopoLaBarra() {
        //Oggi la granularità è il solo file, ma un futuro "documento|riga" deve continuare a leggersi
        assertEquals(12, DocumentiFonte.IdDaCampo41("12|487"));
    }

    @Test
    void idDaCampo41_valoriAssentiOMalformati_valgonoZero() {
        assertEquals(0, DocumentiFonte.IdDaCampo41(null));
        assertEquals(0, DocumentiFonte.IdDaCampo41(""));
        assertEquals(0, DocumentiFonte.IdDaCampo41("   "));
        assertEquals(0, DocumentiFonte.IdDaCampo41("pippo"));
        assertEquals(0, DocumentiFonte.IdDaCampo41("0"));
        assertEquals(0, DocumentiFonte.IdDaCampo41("-3"));
    }

    @Test
    void descrizione_documentoMancante_degradaSenzaFallire() {
        assertEquals("", DocumentiFonte.Descrizione(""));
        assertEquals("documento 999 non più disponibile", DocumentiFonte.Descrizione("999"));
        assertFalse(DocumentiFonte.Apribile("999"));
    }

    @Test
    void descrizione_documentoPresente_mostraIdENomeOriginale() throws Exception {
        DocumentiFonte.Registrazione R = DocumentiFonte.Registra(
                fileConContenuto("binance_2026.csv", "riga\n"), DocumentiFonte.TIPO_CSV, "Binance");

        String descrizione = DocumentiFonte.Descrizione(String.valueOf(R.Id));

        assertTrue(descrizione.startsWith(R.Id + " - binance_2026.csv"), descrizione);
        assertFalse(descrizione.contains(".gz"), "all'utente non si mostra il nome dell'archivio: " + descrizione);
        assertTrue(DocumentiFonte.Apribile(String.valueOf(R.Id)));
    }

    // ------------------------------------------------------------------
    // TIMBRATURA AL PUNTO DI STROZZATURA
    // ------------------------------------------------------------------

    /** @return una riga di movimento minima, con ID valido e tutti i campi vuoti */
    private String[] movimento(String Id) {
        String v[] = new String[Importazioni.ColonneTabella];
        Importazioni.RiempiVuotiArray(v);
        v[0] = Id;
        return v;
    }

    @Test
    void scriviListaSuMappaCrypto_timbraIlDocumentoSuTuttiIMovimenti() {
        List<String[]> lista = new ArrayList<>();
        lista.add(movimento("2026-01-01_001_1_1_DC"));
        lista.add(movimento("2026-01-02_002_1_1_PC"));

        Importazioni.ScriviListaSuMappaCrypto(lista, true, 12);

        for (String[] v : lista) {
            assertEquals("12", v[41], "ogni movimento importato deve puntare al documento di origine");
        }
    }

    @Test
    void scriviListaSuMappaCrypto_senzaDocumento_lasciaIlCampoVuoto() {
        List<String[]> lista = new ArrayList<>();
        lista.add(movimento("2026-01-01_001_1_1_DC"));

        Importazioni.ScriviListaSuMappaCrypto(lista, true, 0);

        assertTrue(Funzioni.noData(lista.get(0)[41]),
                "un movimento senza documento di origine deve restare con il campo vuoto");
    }

    @Test
    void scriviListaSuMappaCrypto_nonSovrascriveUn41GiaValorizzato() {
        List<String[]> lista = new ArrayList<>();
        String v[] = movimento("2026-01-01_001_1_1_DC");
        v[41] = "7";
        lista.add(v);

        Importazioni.ScriviListaSuMappaCrypto(lista, true, 12);

        assertEquals("7", v[41], "chi conosce l'origine esatta della riga l'ha già scritta");
    }

    @Test
    void scriviListaSuMappaCrypto_rigaPiuCortaDi45_nonSollevaEccezione() {
        //Difesa: le righe che arrivano qui dovrebbero essere tutte lunghe ColonneTabella, ma un accesso
        //diretto a [41] su una riga corta farebbe fallire l'importazione a metà
        List<String[]> lista = new ArrayList<>();
        String corto[] = new String[10];
        for (int i = 0; i < corto.length; i++) corto[i] = "";
        corto[0] = "2026-01-01_001_1_1_DC";
        lista.add(corto);

        assertDoesNotThrow(() -> Importazioni.ScriviListaSuMappaCrypto(lista, true, 12));
    }

    @Test
    void scriviListaSuMappaCrypto_senzaIdEsplicito_usaIlDocumentoCorrente() {
        List<String[]> lista = new ArrayList<>();
        String v[] = movimento("2026-01-01_001_1_1_DC");
        lista.add(v);

        Importazioni.DocumentoFonteCorrente = 33;
        try {
            Importazioni.ScriviListaSuMappaCrypto(lista, true);
        } finally {
            Importazioni.DocumentoFonteCorrente = 0;
        }

        assertEquals("33", v[41]);
    }

    // ------------------------------------------------------------------
    // SESSIONI E RISERVATEZZA DELLE CREDENZIALI
    // ------------------------------------------------------------------

    @Test
    void sessione_raccoglieLeRisposteInUnSoloDocumentoNdjson() throws Exception {
        int Id = DocumentiFonte.ApriSessione("OKX");
        assertTrue(Id > 0);

        DocumentiFonte.AggiungiAllaSessione(Id, "OKX_Bills", "okx startDate=1", "{\"data\":[1]}");
        DocumentiFonte.AggiungiAllaSessione(Id, "OKX_Bills", "okx startDate=2", "{\"data\":[2]}");
        DocumentiFonte.ChiudiSessione(Id);

        String contenuto = contenutoConservato(Id);
        String righe[] = contenuto.split("\n");
        assertEquals(2, righe.length, "una riga per risposta, non un documento per risposta");
        assertTrue(righe[0].contains("\"script\":\"OKX_Bills\""), righe[0]);
        assertTrue(righe[0].contains("{\"data\":[1]}"), righe[0]);
        assertTrue(righe[1].contains("{\"data\":[2]}"), righe[1]);
    }

    @Test
    void sessione_rispostaNonJson_nonRompeIlDocumento() throws Exception {
        int Id = DocumentiFonte.ApriSessione("Explorer");
        DocumentiFonte.AggiungiAllaSessione(Id, "explorer", "http://x", "Errore: \"quota\" superata");
        DocumentiFonte.ChiudiSessione(Id);

        String contenuto = contenutoConservato(Id).trim();
        assertTrue(contenuto.startsWith("{") && contenuto.endsWith("}"),
                "una riga malformata renderebbe illeggibile l'intero documento: " + contenuto);
        assertTrue(contenuto.contains("\\\"quota\\\""), contenuto);
    }

    @Test
    void urlSenzaChiave_oscuraLaChiaveApiDegliExplorer() {
        //Gli explorer vogliono la chiave dentro l'URL: registrarla darebbe accesso all'account di chi
        //mettesse le mani sul documento, che contiene già l'intera storia transazionale
        String pulito = DocumentiFonte.UrlSenzaChiave(
                "https://api.etherscan.io/api?module=account&address=0xABC&apikey=SEGRETISSIMO&sort=asc");

        assertFalse(pulito.contains("SEGRETISSIMO"), pulito);
        assertTrue(pulito.contains("apikey=***"), pulito);
        assertTrue(pulito.contains("address=0xABC"), "il resto dell'URL deve restare leggibile: " + pulito);
    }

    @Test
    void urlSenzaChiave_riconosceLeVarianti() {
        assertTrue(DocumentiFonte.UrlSenzaChiave("http://x?api_key=abc").contains("api_key=***"));
        assertTrue(DocumentiFonte.UrlSenzaChiave("http://x?a=1&APIKEY=abc").contains("APIKEY=***"));
        assertEquals("", DocumentiFonte.UrlSenzaChiave(null));
    }

    @Test
    void sessioneSenzaMovimenti_vieneScartata() {
        int Id = DocumentiFonte.ApriSessione("OKX");
        DocumentiFonte.AggiungiAllaSessione(Id, "OKX_Bills", "okx", "{\"data\":[]}");
        DocumentiFonte.ChiudiSessione(Id);

        DocumentiFonte.ChiudiRegistrazione(new DocumentiFonte.Registrazione(Id, true), 0);

        assertNull(DocumentiFonte.Leggi(Id),
                "uno scaricamento che non aggiunge nulla non deve lasciare il proprio NDJSON");
    }

    // ------------------------------------------------------------------
    // ESTRAZIONE PER L'APERTURA
    // ------------------------------------------------------------------

    @Test
    void estraiPerApertura_scompattaConIlNomeOriginale() throws Exception {
        DocumentiFonte.Registrazione R = DocumentiFonte.Registra(
                fileConContenuto("da_aprire.csv", "colonna\nvalore\n"), DocumentiFonte.TIPO_CSV, "Prova");

        File estratto = DocumentiFonte.EstraiPerApertura(R.Id);

        assertNotNull(estratto, "il documento conservato è un .gz, va scompattato prima di poterlo aprire");
        assertTrue(estratto.getName().endsWith("da_aprire.csv"), estratto.getName());
        assertEquals("colonna\nvalore\n", Files.readString(estratto.toPath(), StandardCharsets.UTF_8));
    }

    @Test
    void estraiPerApertura_documentoInesistente_ritornaNull() {
        assertNull(DocumentiFonte.EstraiPerApertura(999));
        assertNull(DocumentiFonte.FileConservato(999));
    }

    // ------------------------------------------------------------------
    // PANNELLO DI GESTIONE — riepilogo dei movimenti agganciati
    // ------------------------------------------------------------------

    /** Inserisce in mappa un movimento con il suo documento di origine. */
    private void movimentoInMappa(String ID, String Wallet, String Documento) {
        String v[] = movimento(ID);
        v[3] = Wallet;
        v[41] = Documento;
        Principale.MappaCryptoWallet.put(ID, v);
    }

    @Test
    void riepiloghi_conta_movimenti_wallet_e_periodo() {
        movimentoInMappa("20210105120000_A_001_1_DC", "Binance", "12");
        movimentoInMappa("20230531093000_A_002_1_PC", "OKX", "12");
        movimentoInMappa("20220310080000_A_003_1_DC", "Binance", "12");
        movimentoInMappa("20240101000000_A_004_1_DC", "Kraken", "7");

        var riepiloghi = DocumentiFonte.Riepiloghi();

        DocumentiFonte.Riepilogo r12 = riepiloghi.get(12);
        assertEquals(3, r12.Movimenti);
        assertEquals("Binance, OKX", r12.WalletInRiga(), "i wallet vanno elencati una volta sola, ordinati");
        assertEquals("2021-01-05 / 2023-05-31", r12.Periodo(),
                "il periodo è quello del primo e dell'ultimo movimento, non l'ordine di inserimento");
        assertEquals(1, riepiloghi.get(7).Movimenti);
    }

    @Test
    void riepiloghi_ignoraIMovimentiSenzaDocumento() {
        movimentoInMappa("20210105120000_A_001_1_DC", "Binance", "");
        movimentoInMappa("20210105120000_A_002_1_DC", "Binance", "pippo");

        assertTrue(DocumentiFonte.Riepiloghi().isEmpty());
    }

    @Test
    void riepiloghi_documentoSenzaMovimenti_nonCompare() {
        assertNull(DocumentiFonte.Riepiloghi().get(12),
                "un documento a cui non punta nessun movimento non ha un riepilogo, e va trattato come zero");
    }

    @Test
    void righeDiRiepilogo_mostranoIlConteggioATTUALE_nonQuelloDelRegistro() throws Exception {
        //È la differenza che rende il pannello utile: il registro ricorda quanti movimenti il documento ha
        //prodotto all'import, ma se poi l'utente li cancella quel numero resta fermo. Eliminare un documento
        //si può decidere solo sul conteggio attuale.
        DocumentiFonte.Registrazione R = DocumentiFonte.Registra(
                fileConContenuto("storico.csv", "riga\n"), DocumentiFonte.TIPO_CSV, "Prova");
        DocumentiFonte.ChiudiRegistrazione(R, 5);        //all'epoca ne aveva prodotti 5
        movimentoInMappa("20210105120000_A_001_1_DC", "Binance", String.valueOf(R.Id));   //oggi ne resta 1

        assertEquals(5, DocumentiFonte.Leggi(R.Id).Movimenti, "il registro conserva il dato storico");

        Object[] riga = Principale_DocumentiFonte.RigheDiRiepilogo().get(0);
        assertEquals(R.Id, riga[0]);
        assertEquals("storico.csv", riga[1]);
        assertEquals(1, riga[Principale_DocumentiFonte.COLONNA_MOVIMENTI],
                "il pannello deve mostrare i movimenti che ci sono adesso");
        assertEquals("Binance", riga[8]);
        assertEquals("2021-01-05 / 2021-01-05", riga[9]);
    }

    @Test
    void righeDiRiepilogo_documentoSenzaMovimenti_mostraZero() throws Exception {
        DocumentiFonte.Registrazione R = DocumentiFonte.Registra(
                fileConContenuto("orfano.csv", "riga\n"), DocumentiFonte.TIPO_CSV, "Prova");
        DocumentiFonte.ChiudiRegistrazione(R, 3);

        Object[] riga = Principale_DocumentiFonte.RigheDiRiepilogo().get(0);
        assertEquals(0, riga[Principale_DocumentiFonte.COLONNA_MOVIMENTI]);
        assertEquals("", riga[8]);
        assertEquals("", riga[9], "senza movimenti non c'è nessun periodo da mostrare");
    }

    @Test
    void dimensioneOriginale_leggeLaDimensionePrimaDellaCompressione() throws Exception {
        //Un contenuto molto ripetitivo si comprime moltissimo: mostrare solo il .gz direbbe all'utente
        //che un CSV da 40 KB ne occupa 200, che è vero sul disco ma non è la dimensione del documento
        String contenuto = "riga di prova ripetuta\n".repeat(2000);
        DocumentiFonte.Registrazione R = DocumentiFonte.Registra(
                fileConContenuto("grosso.csv", contenuto), DocumentiFonte.TIPO_CSV, "Prova");

        long originale = DocumentiFonte.DimensioneOriginale(R.Id);
        long suDisco = DocumentiFonte.Dimensione(R.Id);

        assertEquals(contenuto.getBytes(StandardCharsets.UTF_8).length, originale,
                "la dimensione originale si legge dal trailer gzip, senza scompattare");
        assertTrue(suDisco < originale, "il compresso deve essere piu' piccolo: " + suDisco + " / " + originale);
    }

    @Test
    void dimensioneOriginale_documentoInesistente_valeZero() {
        assertEquals(0, DocumentiFonte.DimensioneOriginale(999));
    }

    // ------------------------------------------------------------------
    // ELIMINABILITÀ — il controllo autorevole, non quello del pulsante
    // ------------------------------------------------------------------

    @Test
    void nonEliminabili_segnalaSoloIDocumentiConMovimentiAgganciati() throws Exception {
        DocumentiFonte.Registrazione Usato = DocumentiFonte.Registra(
                fileConContenuto("usato.csv", "a\n"), DocumentiFonte.TIPO_CSV, "Prova");
        DocumentiFonte.Registrazione Libero = DocumentiFonte.Registra(
                fileConContenuto("libero.csv", "b\n"), DocumentiFonte.TIPO_CSV, "Prova");
        movimentoInMappa("20210105120000_A_001_1_DC", "Binance", String.valueOf(Usato.Id));

        assertTrue(Principale_DocumentiFonte.NonEliminabili(List.of(Libero.Id)).isEmpty(),
                "un documento senza movimenti è eliminabile");

        List<String> bloccati = Principale_DocumentiFonte.NonEliminabili(List.of(Usato.Id, Libero.Id));
        assertEquals(1, bloccati.size());
        assertTrue(bloccati.get(0).startsWith("usato.csv"), bloccati.get(0));
        assertTrue(bloccati.get(0).contains("1 movimenti"), bloccati.get(0));
    }

    @Test
    void nonEliminabili_guardaIMovimentiDiADESSO_nonIlConteggioDelRegistro() throws Exception {
        //Il registro ricorda 5 movimenti prodotti all'import; se l'utente li ha poi cancellati tutti, il
        //documento è eliminabile. Basarsi sul registro lo terrebbe bloccato per sempre
        DocumentiFonte.Registrazione R = DocumentiFonte.Registra(
                fileConContenuto("svuotato.csv", "a\n"), DocumentiFonte.TIPO_CSV, "Prova");
        DocumentiFonte.ChiudiRegistrazione(R, 5);

        assertEquals(5, DocumentiFonte.Leggi(R.Id).Movimenti);
        assertTrue(Principale_DocumentiFonte.NonEliminabili(List.of(R.Id)).isEmpty());
    }

    @Test
    void nonEliminabili_selezioneVuota_nonBloccaNulla() {
        assertTrue(Principale_DocumentiFonte.NonEliminabili(null).isEmpty());
        assertTrue(Principale_DocumentiFonte.NonEliminabili(List.of()).isEmpty());
    }

    // ------------------------------------------------------------------
    // ESPORTAZIONE DECOMPRESSA
    // ------------------------------------------------------------------

    @Test
    void esportaDecompresso_scriveIlFileInChiaroColNomeOriginale() throws Exception {
        DocumentiFonte.Registrazione R = DocumentiFonte.Registra(
                fileConContenuto("da_esportare.csv", "colonna\nvalore\n"), DocumentiFonte.TIPO_CSV, "Prova");
        File destinazione = new File(tempDir.toFile(), "export_" + R.Id);

        File scritto = DocumentiFonte.EsportaDecompresso(R.Id, destinazione);

        assertNotNull(scritto);
        assertEquals("da_esportare.csv", scritto.getName(), "l'esportazione non deve lasciare un .gz");
        assertEquals("colonna\nvalore\n", Files.readString(scritto.toPath(), StandardCharsets.UTF_8));
    }

    @Test
    void esportaDecompresso_nomeGiaPresente_anteponeLIdInveceDiSovrascrivere() throws Exception {
        //Due export di exchange diversi possono chiamarsi entrambi "transactions.csv": sovrascrivere
        //silenziosamente quello già esportato sarebbe una perdita di dati non segnalata
        DocumentiFonte.Registrazione R = DocumentiFonte.Registra(
                fileConContenuto("transactions.csv", "nuovo\n"), DocumentiFonte.TIPO_CSV, "Prova");
        File destinazione = new File(tempDir.toFile(), "export_collisione");
        destinazione.mkdirs();
        Files.writeString(new File(destinazione, "transactions.csv").toPath(), "preesistente\n");

        File scritto = DocumentiFonte.EsportaDecompresso(R.Id, destinazione);

        assertEquals(R.Id + "_transactions.csv", scritto.getName());
        assertEquals("preesistente\n",
                Files.readString(new File(destinazione, "transactions.csv").toPath(), StandardCharsets.UTF_8),
                "il file che c'era già non deve essere toccato");
        assertEquals("nuovo\n", Files.readString(scritto.toPath(), StandardCharsets.UTF_8));
    }

    @Test
    void esportaDecompresso_documentoInesistente_ritornaNull() {
        assertNull(DocumentiFonte.EsportaDecompresso(999, tempDir.toFile()));
    }

    @Test
    void dimensione_documentoInesistente_valeZero() {
        assertEquals(0, DocumentiFonte.Dimensione(999));
    }

    @Test
    void dimensioneLeggibile_usaLUnitaAdatta() {
        assertEquals("-", Principale_DocumentiFonte.DimensioneLeggibile(0));
        assertEquals("512 B", Principale_DocumentiFonte.DimensioneLeggibile(512));
        assertEquals("1,0 KB".replace(',', '.'),
                Principale_DocumentiFonte.DimensioneLeggibile(1024).replace(',', '.'));
        assertTrue(Principale_DocumentiFonte.DimensioneLeggibile(5 * 1024 * 1024).endsWith("MB"));
    }
}
