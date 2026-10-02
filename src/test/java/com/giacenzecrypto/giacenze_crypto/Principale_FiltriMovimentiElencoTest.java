package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.Principale_FiltriMovimenti.FiltriMovimenti;
import com.giacenzecrypto.giacenze_crypto.Principale_FiltriMovimenti.RigaFiltro;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.giacenzecrypto.giacenze_crypto.Principale_FiltriMovimenti.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * La scheda "Filtri" dei movimenti e l'imbuto delle righe di dettaglio: l'elenco dei filtri attivi, il
 * modo di toglierne uno solo, e il filtro per colonna che ricorda i valori scelti ({@code Tabelle.FiltroValori}).
 *
 * <p>Fissa soprattutto due invarianti: l'elenco dei criteri ha sempre tanti elementi quanti ne conta
 * {@code Attivi()} (il numero del pulsante e la scheda devono dire la stessa cosa), e togliere un criterio
 * ne toglie <b>uno</b>.
 */
class Principale_FiltriMovimentiElencoTest {

    private final List<JTable> tabelleRegistrate = new ArrayList<>();

    @AfterEach
    void pulisce() {
        for (JTable t : tabelleRegistrate) {
            Tabelle.tableFilters.remove(t);
            Tabelle.tableFiltersListener.remove(t);
        }
    }

    private static FiltriMovimenti tuttiAttivi() {
        return new FiltriMovimenti("Binance", "BTC", "7", 0, 99999999L, true, true, true, true);
    }

    // ==================== criteri della finestra Filtri ====================

    @Test
    void iCriteriSonoTantiQuantiGliAttivi() {
        FiltriMovimenti f = tuttiAttivi();
        assertEquals(7, f.Attivi());
        assertEquals(f.Attivi(), f.Criteri(null).size());
        assertEquals(0, Nessuno(0, 99999999L).Criteri(null).size());
    }

    @Test
    void laDescrizioneEFattaDagliStessiCriteri() {
        FiltriMovimenti f = tuttiAttivi();
        List<String> da = f.Criteri(id -> "estratto.csv").stream().map(FiltriMovimenti.Criterio::Testo).toList();
        assertEquals(da, f.Descrizione(id -> "estratto.csv"));
        assertTrue(da.contains("Wallet : Binance"));
        assertTrue(da.contains("Moneta : BTC"));
        assertTrue(da.contains("Documento di origine : n. 7 — estratto.csv"));
        assertTrue(da.contains("Trasferimenti interni nascosti"));
    }

    @Test
    void ognunoDeiCriteriSiTogliesoloLui() {
        FiltriMovimenti f = tuttiAttivi();
        for (FiltriMovimenti.Criterio c : f.Criteri(null)) {
            FiltriMovimenti senza = c.SenzaQuesto();
            assertEquals(f.Attivi() - 1, senza.Attivi(), "togliendo '" + c.Nome() + "' ne resta uno in meno");
            List<String> nomiRimasti = senza.Criteri(null).stream().map(FiltriMovimenti.Criterio::Nome).toList();
            assertFalse(nomiRimasti.contains(c.Nome()), c.Nome() + " doveva sparire");
            //Le date non sono un criterio e non si perdono
            assertEquals(f.DataInizio(), senza.DataInizio());
            assertEquals(f.DataFine(), senza.DataFine());
        }
    }

    @Test
    void unGruppoDiWalletSiChiamaGruppoENonWallet() {
        FiltriMovimenti f = new FiltriMovimenti("Wallet 01 : Famiglia (3)", TUTTI, DOC_TUTTI, 0, 1, false, false, false, false);
        FiltriMovimenti.Criterio c = f.Criteri(null).get(0);
        assertEquals("Gruppo di wallet", c.Nome());
        assertEquals("Famiglia", c.Valore());
    }

    @Test
    void conDocumentoCambiaSoloIlDocumento() {
        FiltriMovimenti f = tuttiAttivi().ConDocumento("9");
        assertEquals("9", f.Documento());
        assertEquals("Binance", f.Wallet());
        assertTrue(f.NascondiTokenScam());
    }

    @Test
    void righeCaricamento_ilBidoneRestituisceIlRecordSenzaQuelCriterio() {
        List<FiltriMovimenti> ricevuti = new ArrayList<>();
        List<RigaFiltro> righe = RigheCaricamento(tuttiAttivi(), null, ricevuti::add);
        assertEquals(7, righe.size());
        assertTrue(righe.stream().allMatch(r -> ORIGINE_CARICAMENTO.equals(r.Origine())));

        RigaFiltro moneta = righe.stream().filter(r -> r.Nome().equals("Moneta")).findFirst().orElseThrow();
        moneta.Rimuovi().run();
        assertEquals(1, ricevuti.size());
        assertEquals(TUTTI, ricevuti.get(0).Token());
        assertEquals("Binance", ricevuti.get(0).Wallet(), "gli altri criteri restano");
    }

    // ==================== filtri di riga: ricerca e colonne ====================

    @Test
    void filtroColonna_ricordaIValoriScelti() {
        Tabelle.FiltroValori f = new Tabelle.FiltroValori(8, List.of("BTC", "ETH"));
        assertEquals(8, f.colonna());
        assertEquals(List.of("BTC", "ETH"), f.valori());
    }

    @Test
    void righeDiRiga_unaRigaPerColonnaECiascunaSiTogliedaSola() {
        Map<Integer, RowFilter<DefaultTableModel, Integer>> filtri = new HashMap<>();
        filtri.put(8, new Tabelle.FiltroValori(8, List.of("BTC", "ETH")));
        filtri.put(3, new Tabelle.FiltroValori(3, List.of("Binance")));
        List<Integer> tolte = new ArrayList<>();
        AtomicInteger ricercaPulita = new AtomicInteger();

        List<RigaFiltro> righe = RigheDiRiga("  btc ", ricercaPulita::incrementAndGet, filtri,
                c -> "Col" + c, tolte::add);

        assertEquals(3, righe.size(), "ricerca + due colonne");
        assertEquals(ORIGINE_RICERCA, righe.get(0).Origine());
        assertEquals("btc", righe.get(0).Valori(), "il testo di ricerca viene ripulito dagli spazi");
        assertEquals("Col3", righe.get(1).Nome(), "le colonne in ordine di indice");
        assertEquals("Binance", righe.get(1).Valori());
        assertEquals("BTC, ETH", righe.get(2).Valori());

        righe.get(2).Rimuovi().run();
        assertEquals(List.of(8), tolte, "il bidone toglie solo la colonna della sua riga, tutti i suoi valori insieme");
        righe.get(0).Rimuovi().run();
        assertEquals(1, ricercaPulita.get());
    }

    @Test
    void righeDiRiga_senzaFiltriNonHaNessunaRiga() {
        assertTrue(RigheDiRiga("", () -> { }, Map.of(), c -> "x", c -> { }).isEmpty());
        assertTrue(RigheDiRiga(null, () -> { }, null, c -> "x", c -> { }).isEmpty());
    }

    @Test
    void elencaValori_accorciaEMostraIlVuoto() {
        assertEquals("(vuoto)", ElencaValori(List.of("")));
        List<String> molti = new ArrayList<>();
        for (int i = 1; i <= VALORI_MAX_MOSTRATI + 3; i++) molti.add("v" + i);
        String testo = ElencaValori(molti);
        assertTrue(testo.startsWith("v1, v2"));
        assertTrue(testo.endsWith("(+3)"), testo);
    }

    // ==================== imbuto pieno o vuoto ====================

    @Test
    void filtroRigaAttivo_perColonnaSoloSeIlFiltroAmmetteEsattamenteQuelValore() {
        Map<Integer, RowFilter<DefaultTableModel, Integer>> filtri = new HashMap<>();
        filtri.put(11, new Tabelle.FiltroValori(11, List.of("BTC")));
        filtri.put(8, new Tabelle.FiltroValori(8, List.of("BTC", "ETH")));

        assertTrue(FiltroRigaAttivo(new FiltroRiga(11, "BTC", ""), "", filtri), "filtro identico: imbuto pieno");
        assertFalse(FiltroRigaAttivo(new FiltroRiga(11, "ETH", ""), "", filtri), "altro valore sulla stessa colonna");
        assertFalse(FiltroRigaAttivo(new FiltroRiga(8, "BTC", ""), "", filtri),
                "con due valori scelti il clic li sostituirebbe: l'imbuto resta vuoto");
        assertFalse(FiltroRigaAttivo(new FiltroRiga(3, "BTC", ""), "", filtri), "colonna senza filtro");
        assertFalse(FiltroRigaAttivo(new FiltroRiga(3, "BTC", ""), "", null));
        assertFalse(FiltroRigaAttivo(null, "", filtri));
    }

    @Test
    void filtroRigaAttivo_perDocumentoConfrontaIlCriterioCorrente() {
        FiltroRiga doc = new FiltroRiga(COLONNA_DOCUMENTO, "7", "");
        assertTrue(FiltroRigaAttivo(doc, "7", Map.of()));
        assertTrue(FiltroRigaAttivo(doc, " 7 ", Map.of()), "gli spazi non contano");
        assertFalse(FiltroRigaAttivo(doc, "8", Map.of()));
        assertFalse(FiltroRigaAttivo(doc, DOC_TUTTI, Map.of()));
        assertFalse(FiltroRigaAttivo(doc, DOC_CON, Map.of()), "'con documento' non e' questo documento");
        assertFalse(FiltroRigaAttivo(doc, null, Map.of()));
    }

    @Test
    void ilClicSuUnImbutoPienoTogliesoloQuelFiltro() {
        //La sequenza del clic: attivo -> si toglie la colonna, gli altri filtri restano
        JTable t = tabella();
        Tabelle.Tabelle_ImpostaFiltroColonna(t, 0, List.of("Kraken"), "");
        Tabelle.Tabelle_ImpostaFiltroColonna(t, 1, List.of("BTC"), "");
        FiltroRiga f = new FiltroRiga(1, "BTC", "");
        assertTrue(FiltroRigaAttivo(f, "", Tabelle.tableFilters.get(t)));

        Tabelle.Tabelle_RimuoviFiltroColonna(t, f.Colonna(), "");
        assertFalse(FiltroRigaAttivo(f, "", Tabelle.tableFilters.get(t)));
        assertEquals(2, t.getRowCount(), "resta il filtro sul wallet");
    }

    // ==================== il filtro per colonna dentro Tabelle ====================

    private JTable tabella() {
        DefaultTableModel m = new DefaultTableModel(new Object[][]{
            {"Binance", "BTC"}, {"Binance", "ETH"}, {"Kraken", "BTC"}, {"Kraken", "ETH"}},
            new String[]{"Wallet", "Moneta"});
        JTable t = new JTable(m);
        t.setRowSorter(new TableRowSorter<>(m));
        tabelleRegistrate.add(t);
        return t;
    }

    @Test
    void impostaFiltroColonna_filtraESostituisceIlPrecedente() {
        JTable t = tabella();
        AtomicInteger notifiche = new AtomicInteger();
        Tabelle.tableFiltersListener.put(t, notifiche::incrementAndGet);

        Tabelle.Tabelle_ImpostaFiltroColonna(t, 1, List.of("BTC"), "");
        assertEquals(2, t.getRowCount());
        assertEquals(1, notifiche.get(), "il listener viene avvisato");

        Tabelle.Tabelle_ImpostaFiltroColonna(t, 1, List.of("ETH"), "");
        assertEquals(2, t.getRowCount());
        assertEquals("ETH", t.getValueAt(0, 1), "il nuovo valore sostituisce il vecchio, non si somma");
        assertEquals(List.of("ETH"), ((Tabelle.FiltroValori) Tabelle.tableFilters.get(t).get(1)).valori());

        //Colonne diverse si restringono a vicenda
        Tabelle.Tabelle_ImpostaFiltroColonna(t, 0, List.of("Kraken"), "");
        assertEquals(1, t.getRowCount());
    }

    @Test
    void rimuoviFiltroColonna_togliesoloQuellaColonna() {
        JTable t = tabella();
        Tabelle.Tabelle_ImpostaFiltroColonna(t, 0, List.of("Kraken"), "");
        Tabelle.Tabelle_ImpostaFiltroColonna(t, 1, List.of("BTC"), "");
        assertEquals(1, t.getRowCount());

        assertTrue(Tabelle.Tabelle_RimuoviFiltroColonna(t, 1, ""));
        assertEquals(2, t.getRowCount(), "resta il filtro sul wallet");
        assertFalse(Tabelle.Tabelle_RimuoviFiltroColonna(t, 1, ""), "una colonna senza filtro non cambia nulla");
    }

    @Test
    void laRicercaLiberaRestaInAndConIFiltriDiColonna() {
        JTable t = tabella();
        Tabelle.Tabelle_ImpostaFiltroColonna(t, 1, List.of("BTC"), "kraken");
        assertEquals(1, t.getRowCount());
    }

    @Test
    void impostaFiltroColonna_senzaValoriTogliesoloQuelFiltro() {
        JTable t = tabella();
        Tabelle.Tabelle_ImpostaFiltroColonna(t, 1, List.of("BTC"), "");
        Tabelle.Tabelle_ImpostaFiltroColonna(t, 1, List.of(), "");
        assertEquals(4, t.getRowCount());
    }
}
