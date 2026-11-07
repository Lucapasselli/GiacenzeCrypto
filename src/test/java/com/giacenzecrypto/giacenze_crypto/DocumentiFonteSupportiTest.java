package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Più documenti su un movimento (2026-11-06): il campo {@code [41]} è {@code origine|supporto,supporto}. L'origine è il
 * file da cui il movimento nasce, i supporti lo spiegano o lo classificano (Dual Investment, Subscription, in futuro un
 * estratto di bonifici). Le versioni dalla 1.0.60 leggono solo la parte prima della barra.
 */
class DocumentiFonteSupportiTest {

    @BeforeEach
    @AfterEach
    void svuota() {
        MappaCryptoWallet.clear();
    }

    private static String[] movimento(String id, String campo41) {
        String[] v = new String[Importazioni.ColonneTabella];
        v[0] = id;
        v[1] = "2024-01-01 10:00";
        v[3] = "Binance";
        v[4] = "Principale";
        v[5] = "DEPOSITO CRYPTO";
        v[11] = "BTC";
        v[12] = "Crypto";
        v[13] = "1";
        v[15] = "100";
        v[22] = "A";
        v[41] = campo41;
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(id, v);
        return v;
    }

    @Test
    void formatoDelCampo() {
        assertEquals(7, DocumentiFonte.IdDaCampo41("7|12,15"));
        assertEquals(List.of(12, 15), DocumentiFonte.Supporti("7|12,15"));
        assertEquals("7", DocumentiFonte.SoloOrigine("7|12,15"));
        assertEquals(0, DocumentiFonte.IdDaCampo41("|12"), "solo supporto: nessuna origine");
        assertEquals(List.of(12), DocumentiFonte.Supporti("|12"));
        assertEquals(List.of(), DocumentiFonte.Supporti("7"));
        assertEquals(List.of(), DocumentiFonte.Supporti(null));
        assertTrue(DocumentiFonte.Collegato("7|12,15", 7));
        assertTrue(DocumentiFonte.Collegato("7|12,15", 15));
        assertFalse(DocumentiFonte.Collegato("7|12,15", 9));
        assertEquals("9|12", DocumentiFonte.ConOrigine("|12", "9"));
        assertEquals("9", DocumentiFonte.ConOrigine("", "9"));
    }

    @Test
    void aggiungiETogliSupporto_ripassareNonDuplica() {
        String[] v = movimento("20240101100000_Binance_1_1_DC", "7");
        assertTrue(DocumentiFonte.AggiungiSupporto(v, 12));
        assertEquals("7|12", v[41]);
        assertFalse(DocumentiFonte.AggiungiSupporto(v, 12), "lo stesso file ripassato non si ripete");
        assertFalse(DocumentiFonte.AggiungiSupporto(v, 7), "l'origine non diventa anche supporto");
        assertTrue(DocumentiFonte.AggiungiSupporto(v, 15));
        assertEquals("7|12,15", v[41]);
        assertTrue(DocumentiFonte.TogliSupporti(v, List.of(12)));
        assertEquals("7|15", v[41]);
        assertTrue(DocumentiFonte.TogliSupporti(v, List.of(15)));
        assertEquals("7", v[41], "senza supporti il campo torna il solo id, come prima");
    }

    @Test
    void collegaAlGruppo_eScollega() {
        String[] a = movimento("20240101100000_Binance_1_1_PC", "7");
        String[] b = movimento("20240101100000_Binance_1_2_DC", "7");
        a[20] = b[0];
        b[20] = a[0];
        assertEquals(2, DocumentiFonte.CollegaAlGruppo(a, 12));
        assertEquals(0, DocumentiFonte.CollegaAlGruppo(a, 12), "idempotente");
        assertEquals("7|12", b[41]);
        assertEquals(2, DocumentiFonte.Scollega(12));
        assertEquals("7", a[41]);
        assertEquals("7", b[41]);
    }

    @Test
    void filtroPerDocumento() {
        assertTrue(Principale_FiltriMovimenti.PassaDocumento("12", "7|12"), "un documento collegato passa il filtro");
        assertTrue(Principale_FiltriMovimenti.PassaDocumento("7", "7|12"));
        assertFalse(Principale_FiltriMovimenti.PassaDocumento("9", "7|12"));
        assertTrue(Principale_FiltriMovimenti.PassaDocumento(Principale_FiltriMovimenti.DOC_CON, "7|12"));
        assertTrue(Principale_FiltriMovimenti.PassaDocumento(Principale_FiltriMovimenti.DOC_SENZA, "|12"),
                "con / senza documento guardano la sola origine");
        assertFalse(Principale_FiltriMovimenti.PassaDocumento(Principale_FiltriMovimenti.DOC_CON, "|12"));
    }

    @Test
    void riepilogo_origineECollegatiAParte() {
        movimento("20240101100000_Binance_1_1_DC", "7|12");
        movimento("20240102100000_Binance_1_1_DC", "7");
        movimento("20240103100000_Binance_1_1_DC", "|12");
        Map<Integer, DocumentiFonte.Riepilogo> r = DocumentiFonte.Riepiloghi();
        assertEquals(2, r.get(7).Movimenti);
        assertEquals(0, r.get(7).Collegati);
        assertEquals(0, r.get(12).Movimenti);
        assertEquals(2, r.get(12).Collegati);
        assertEquals("2024-01-01 / 2024-01-03", r.get(12).Periodo(), "il periodo di un supporto viene dai collegati");
    }

    @Test
    void unione_origineDelPrimo_supportiDiTutti() {
        String[] a = movimento("20240101100000_Binance_1_1_DC", "|12");
        String[] b = movimento("20240102100000_Binance_1_1_DC", "7|15");
        String[] c = movimento("20240103100000_Binance_1_1_DC", "8|12");
        assertEquals("7|12,15", DocumentiFonte.Unione(List.of(a, b, c)));
    }

    /**
     * Annullare una classificazione toglie dagli originali i supporti che la classificazione aveva collegato (sono anche
     * sui suoi generati), non quelli messi a mano su un originale.
     */
    @Test
    void annullamento_togliSoloISupportiDellaClassificazione() {
        String[] originale = movimento("20240101100000_Binance_1_1_DC", "7|20,30"); //20 a mano, 30 dalla classificazione
        String[] generato = movimento("20240101100000_Binance_1_2_DC", "7|30");
        generato[22] = "AU";
        originale[18] = "DTW - Trasferimento da Dual Investment";
        originale[20] = generato[0];
        generato[20] = originale[0];

        GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale(
                new String[]{originale[0], generato[0]}, originale[0]);

        assertNull(MappaCryptoWallet.get(generato[0]), "il generato sparisce");
        assertEquals("7|20", originale[41], "resta il collegamento messo a mano, va via quello della classificazione");
    }

    @Test
    void generatoEreditaSoloLOrigine() {
        assertEquals("7", DocumentiFonte.SoloOrigine("7|20"));
        assertEquals("", DocumentiFonte.SoloOrigine("|20"));
    }

    @Test
    void dichiarazioni_includonoIDocumentiCollegati() {
        String[] a = movimento("20240101100000_Binance_1_1_DC", "7|12");
        Dichiarazioni.DocumentiDiOrigine d = Dichiarazioni.RaccogliDocumenti(List.<String[]>of(a), "2024", w -> "Wallet 01", id -> null);
        assertEquals(Set.of(7, 12), Set.copyOf(d.Anno.stream().map(x -> x.Id).toList()));
        assertEquals(0, d.MovimentiAnnoSenzaDocumento);
        String[] b = movimento("20240102100000_Binance_1_1_DC", "|12");
        d = Dichiarazioni.RaccogliDocumenti(List.<String[]>of(b), "2024", w -> "Wallet 01", id -> null);
        assertEquals(1, d.MovimentiAnnoSenzaDocumento, "senza origine conta come senza documento");
    }
}
