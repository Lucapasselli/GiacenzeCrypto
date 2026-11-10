package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link Importazioni#Ex_OKX_SuddividiPerOrdine} — la decisione di quali righe OKX raccolte in uno
 * stesso istante appartengano allo stesso movimento.
 *
 * <p>È la parte verificabile offline della correzione del difetto <b>C7</b>
 * ({@code Documentazione/Analisi_Bug_Criticita.md}): il consolidamento vero e proprio non è testabile qui,
 * perché {@code Ex_OKX_Consolida} richiede la ricerca prezzi, ma la suddivisione è una funzione pura ed è
 * il punto in cui entrambe le strade — CSV e API — sbagliavano, in due modi opposti.</p>
 *
 * <p>I casi riproducono la forma di un export OKX con più ordini nello stesso secondo.</p>
 */
class ImportOKXSuddivisionePerOrdineTest {

    /**
     * Costruisce una riga nel formato intermedio a 19 campi, con i soli campi che contano qui.
     * @param ordId identificativo dell'ordine, campo {@code [13]}; vuoto per i movimenti che non sono ordini
     * @param billId identificativo della riga, campo {@code [14]}
     */
    private static String[] riga(String orario, String causale, String moneta, String qta,
                                 String ordId, String billId) {
        String[] r = new String[19];
        r[0] = orario;
        r[1] = "OKX";
        r[2] = "Trading";
        r[4] = causale;
        r[5] = moneta;
        r[6] = qta;
        r[13] = ordId;
        r[14] = billId;
        Importazioni.RiempiVuotiArray(r);
        return r;
    }

    /** @return gli ordId presenti in un sottogruppo, per leggere il risultato senza frugare negli indici */
    private static List<String> ordiniDi(List<String[]> sottogruppo) {
        List<String> out = new ArrayList<>();
        for (String[] r : sottogruppo) out.add(r[13]);
        return out;
    }

    /**
     * Il caso che ha fatto emergere il difetto. In uno stesso secondo l'export contiene più righe che sono
     * <b>ordini distinti</b>, con le gambe mescolate fra loro. Raggruppandole per solo orario — quel che
     * faceva l'import da CSV — diventavano un unico scambio invece di uno per ordine.
     */
    @Test
    void ottoOrdiniNelloStessoSecondoRestanoOttoMovimenti() {
        List<String[]> gruppo = new ArrayList<>();
        //Ordine di comparsa volutamente interlacciato, come in un export vero
        gruppo.add(riga("2025-01-10 10:00:00", "Buy",  "BTC",  "0.0003", "ORD-A", "100"));
        gruppo.add(riga("2025-01-10 10:00:00", "Buy",  "BTC",  "0.0003", "ORD-B", "101"));
        gruppo.add(riga("2025-01-10 10:00:00", "Sell", "USDC", "-20", "ORD-C", "102"));
        gruppo.add(riga("2025-01-10 10:00:00", "Sell", "USDC", "-20", "ORD-B", "103"));
        gruppo.add(riga("2025-01-10 10:00:00", "Buy",  "BTC",  "0.0003", "ORD-C", "104"));
        gruppo.add(riga("2025-01-10 10:00:00", "Sell", "USDC", "-20", "ORD-A", "105"));

        List<List<String[]>> sotto = Importazioni.Ex_OKX_SuddividiPerOrdine(gruppo);

        assertEquals(3, sotto.size(), "un movimento per ordine, non uno per istante");
        //L'ordine di prima comparsa viene conservato: rende il risultato riproducibile fra due import
        assertEquals(List.of("ORD-A", "ORD-A"), ordiniDi(sotto.get(0)));
        assertEquals(List.of("ORD-B", "ORD-B"), ordiniDi(sotto.get(1)));
        assertEquals(List.of("ORD-C", "ORD-C"), ordiniDi(sotto.get(2)));
        //Ogni sottogruppo ha entrambe le gambe: è la condizione perché ne esca uno scambio e non due
        //movimenti isolati di deposito e prelievo
        for (List<String[]> s : sotto) {
            assertEquals(2, s.size());
            assertTrue(s.stream().anyMatch(r -> r[4].equals("Buy")));
            assertTrue(s.stream().anyMatch(r -> r[4].equals("Sell")));
        }
    }

    /**
     * L'errore speculare, quello dell'import da API: chiudere il gruppo alla prima coppia Buy+Sell spezzava
     * in due scambi un ordine eseguito in due parti. In un export vero capita per alcuni ordini.
     */
    @Test
    void unOrdineEseguitoInDueParteRestaUnMovimentoSolo() {
        List<String[]> gruppo = List.of(
                riga("2025-01-11 10:00:00", "Sell", "USDC", "-5",           "ORD-X", "200"),
                riga("2025-01-11 10:00:00", "Buy",  "BTC",  "0.0001",       "ORD-X", "201"),
                riga("2025-01-11 10:00:00", "Sell", "USDC", "-15",          "ORD-X", "202"),
                riga("2025-01-11 10:00:00", "Buy",  "BTC",  "0.0002",       "ORD-X", "203"));

        List<List<String[]>> sotto = Importazioni.Ex_OKX_SuddividiPerOrdine(gruppo);

        assertEquals(1, sotto.size(), "le due esecuzioni sono lo stesso ordine");
        assertEquals(4, sotto.get(0).size());
    }

    /**
     * I movimenti del conto Funding non sono ordini e non hanno {@code ordId}: restano insieme, in coda, e
     * non vengono attribuiti a nessuno degli ordini presenti nello stesso istante.
     */
    @Test
    void iMovimentiSenzaOrdineFinisconoInUnGruppoResiduoInCoda() {
        List<String[]> gruppo = List.of(
                riga("2025-01-12 10:00:00", "Deposit", "ETH", "0.003", "", "300"),
                riga("2025-01-12 10:00:00", "Buy",     "BTC", "0.001", "ORD-Y", "301"),
                riga("2025-01-12 10:00:00", "Sell",    "USDC", "-60", "ORD-Y", "302"),
                riga("2025-01-12 10:00:00", "Received", "USDC", "10", "", "303"));

        List<List<String[]>> sotto = Importazioni.Ex_OKX_SuddividiPerOrdine(gruppo);

        assertEquals(2, sotto.size());
        assertEquals(List.of("ORD-Y", "ORD-Y"), ordiniDi(sotto.get(0)), "prima gli ordini");
        assertEquals(2, sotto.get(1).size(), "poi il residuo, tutto insieme");
        assertEquals("Deposit", sotto.get(1).get(0)[4]);
        assertEquals("Received", sotto.get(1).get(1)[4]);
    }

    /**
     * Compatibilità all'indietro: le righe dell'import del CSV storico ({@code Ex_OKX_Importa}) hanno
     * {@code [13]} vuoto per costruzione ({@code Importazioni.java:455}). Il gruppo deve tornare intatto,
     * così quel percorso continua a comportarsi esattamente come prima della correzione.
     */
    @Test
    void senzaNessunOrdineIlGruppoTornaIntatto() {
        List<String[]> gruppo = List.of(
                riga("2025-01-13 10:00:00", "Buy",  "BTC",  "0.001", "", "400"),
                riga("2025-01-13 10:00:00", "Sell", "USDC", "-60", "", "401"));

        List<List<String[]>> sotto = Importazioni.Ex_OKX_SuddividiPerOrdine(gruppo);

        assertEquals(1, sotto.size());
        assertSame(gruppo, sotto.get(0), "nessuna copia e nessun riordino quando non c'è nulla da dividere");
        assertFalse(Importazioni.Ex_OKX_GruppoHaOrdini(gruppo));
    }

    @Test
    void gruppoVuotoONulloNonProduceSottogruppi() {
        assertTrue(Importazioni.Ex_OKX_SuddividiPerOrdine(null).isEmpty());
        assertTrue(Importazioni.Ex_OKX_SuddividiPerOrdine(List.of()).isEmpty());
        assertFalse(Importazioni.Ex_OKX_GruppoHaOrdini(null));
    }

    /**
     * La chiusura anticipata alla prima coppia Buy+Sell resta attiva solo in assenza di {@code ordId}: è il
     * ripiego per l'import del CSV storico. Con gli ordini noti deve essere disattivata, altrimenti
     * spezzerebbe il gruppo prima che la suddivisione possa intervenire.
     *
     * <p>Il gruppo misto — righe con e senza {@code ordId} nello stesso istante — disattiva il ripiego per
     * tutte, comprese quelle senza ordine, che finiscono insieme nel sottogruppo residuo. È innocuo, e non
     * per fortuna: le righe prive di {@code ordId} sono solo quelle del conto Funding, le cui causali
     * ({@code Deposit}, {@code Withdrawal}, {@code Received}, {@code Deposit yield}, {@code Transfer
     * in/out}, {@code Simple Earn …}) {@link Importazioni#Ex_OKX_MappaCausali} mappa su
     * {@code TRASFERIMENTO-CRYPTO}, {@code REWARD} o {@code NON CONSIDERARE}. Nessuna di queste entra
     * nell'accumulatore degli scambi, dove le quantità verrebbero sommate: producono un movimento per riga.
     * Le righe di trade portano invece sempre un {@code ordId}, sia da {@code account/bills-archive} sia
     * dall'archivio trimestrale — verificato che il CSV d'archivio espone la colonna {@code ordId}.
     */
    @Test
    void laPresenzaDiUnSoloOrdineBastaADisattivareIlRipiego() {
        List<String[]> misto = List.of(
                riga("2025-01-12 10:00:00", "Deposit", "ETH", "0.003", "", "500"),
                riga("2025-01-12 10:00:00", "Buy",     "BTC", "0.001", "ORD-Z", "501"));

        assertTrue(Importazioni.Ex_OKX_GruppoHaOrdini(misto));
    }

    // ==================== chiave di deduplica degli ID OKX (difetto C12) ====================

    @Test
    void loStessoScambioNelleDueFormeDellIDDaLaStessaChiave() {
        //Fino al 02/04/2026 RitornaScambi univa le gambe con "_", da allora con "-": confrontate carattere
        //per carattere le due forme non si riconoscono, e lo scambio veniva reinserito dallo scaricamento
        //via API. In pratica le due forme convivono nello storico di uno stesso utente.
        assertEquals(Importazioni.ChiaveDedupOKX("1000000000000000001-1000000000000000002"),
                     Importazioni.ChiaveDedupOKX("1000000000000000001_1000000000000000002"));
    }

    @Test
    void unBillIdSempliceRestaSeStesso() {
        assertEquals("1234567890", Importazioni.ChiaveDedupOKX("1234567890"));
        assertEquals("1234567890", Importazioni.ChiaveDedupOKX("  1234567890  "));
        assertEquals("", Importazioni.ChiaveDedupOKX(null));
        assertEquals("", Importazioni.ChiaveDedupOKX(""));
    }

    @Test
    void gliIDDeiRendimentiEarnNonVengonoToccati() {
        //Contengono "-" come separatore di campi, non di gambe: una normalizzazione generica li
        //rimescolerebbe, e sono 86 movimenti nel solo archivio di sviluppo.
        assertEquals("EARN-USDC-20260714", Importazioni.ChiaveDedupOKX("EARN-USDC-20260714"));
        assertEquals("EARN-BTC-20260716", Importazioni.ChiaveDedupOKX("EARN-BTC-20260716"));
    }

    @Test
    void laNormalizzazioneAgisceSoloSullaFormaCifreSottolineaturaCifre() {
        //Volutamente stretta: tutto ciò che non è esattamente <cifre>_<cifre> passa immutato, così una
        //forma futura non viene riscritta a indovinare.
        assertEquals("abc_123", Importazioni.ChiaveDedupOKX("abc_123"));
        assertEquals("123_abc", Importazioni.ChiaveDedupOKX("123_abc"));
        assertEquals("1_2_3", Importazioni.ChiaveDedupOKX("1_2_3"));
        assertEquals("_123", Importazioni.ChiaveDedupOKX("_123"));
        assertEquals("123_", Importazioni.ChiaveDedupOKX("123_"));
    }

    @Test
    void dueScambiDiversiRestanoDiversi() {
        //La normalizzazione non deve far collidere due movimenti veri: sarebbe un movimento perso, cioè
        //il danno opposto e peggiore di quello che corregge.
        assertNotEquals(Importazioni.ChiaveDedupOKX("100_200"), Importazioni.ChiaveDedupOKX("200_100"));
        assertNotEquals(Importazioni.ChiaveDedupOKX("100_200"), Importazioni.ChiaveDedupOKX("100200"));
    }
}
