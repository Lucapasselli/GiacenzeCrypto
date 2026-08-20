package com.giacenzecrypto.giacenze_crypto;

import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Test di {@link TipiOKX}, la tabella di decodifica dei codici {@code type} dei bill OKX, ora letta da
 * {@code config/importmappe/OKX_Tipi.json} invece che scritta nel codice.
 *
 * <p>Come {@link MappeCausaliTest}, <b>non fissa il numero dei codici</b>: aggiungerne uno senza toccare il
 * programma è esattamente ciò per cui il file esiste. Fissa il meccanismo e l'invariante che protegge una
 * modifica fatta a mano — che ogni etichetta emessa sia una chiave di {@code OKX.json} — più le poche
 * decodifiche di riferimento che non devono cambiare per sbaglio.
 */
public class TipiOKXTest {

    @Test
    public void laTabellaSiCaricaEdEUsabile() {
        assertNotNull(TipiOKX.Carica(), "tabella OKX_Tipi non caricata");
    }

    /**
     * L'invariante che vale il test: la catena è in due passi (codice → etichetta → categoria) e il secondo
     * passo è {@code OKX.json}. Un refuso in un'etichetta modificata a mano non fa fallire nulla — rende
     * semplicemente sconosciuto un movimento che era classificato, in silenzio. Qui invece si vede.
     */
    @Test
    public void ogniEtichettaEmessaEUnaCausaleNotaAOKXJson() {
        TipiOKX tabella = TipiOKX.Carica();
        assertNotNull(tabella);
        Map<String, String> causali = Importazioni.Ex_OKX_MappaCausali();
        assertNotNull(causali, "mappa causali OKX non caricata");

        for (String etichetta : tabella.EtichetteEmesse()) {
            assertTrue(causali.containsKey(etichetta),
                    "l'etichetta \"" + etichetta + "\" prodotta da OKX_Tipi.json non è una chiave di OKX.json:"
                    + " i movimenti con quel codice finirebbero fra gli sconosciuti");
        }
    }

    /**
     * Gli stessi codici significano cose diverse sui due conti: sul Trading {@code 2} è la gamba di uno
     * scambio, sul Funding è un prelievo. È la ragione per cui il file ha due blocchi e non uno.
     */
    @Test
    public void loStessoCodiceSiLeggeDiversamenteSuiDueConti() {
        TipiOKX t = TipiOKX.Carica();
        assertNotNull(t);
        assertEquals("Sell", t.Causale("2", "-1", true, ""));
        assertEquals("Buy", t.Causale("2", "1", true, ""));
        assertEquals("withdrawal", t.Causale("2", "-1", false, ""));
        assertEquals("deposit", t.Causale("1", "1", false, ""));
    }

    /** Il verso dei giroconti si ricava dal segno di {@code balChg}, non dal codice */
    @Test
    public void ilVersoVieneDalSegnoDiBalChg() {
        TipiOKX t = TipiOKX.Carica();
        assertNotNull(t);
        assertEquals("Transfer out", t.Causale("75", "-1004.38", false, ""));
        assertEquals("Transfer in", t.Causale("75", "1004.38", false, ""));
        //Notazione scientifica: BigDecimal.toString la produce da solo sui token con molti decimali, e il
        //test testuale che vi si applicava è il bug M7. Una quantità positiva resta un'entrata.
        assertEquals("Transfer in", t.Causale("326", "2.5E-9", false, ""));
        assertEquals("Transfer out", t.Causale("326", "-2.5E-9", false, ""));
    }

    /**
     * Il ripiego per i codici non elencati resta nel codice e non è configurabile: è l'unica via con cui un
     * codice nuovo arriva nel riepilogo dei movimenti sconosciuti, insieme alla descrizione di OKX.
     */
    @Test
    public void unCodiceNonElencatoRestaSconosciutoEPortaConSeLaDescrizione() {
        TipiOKX t = TipiOKX.Carica();
        assertNotNull(t);
        assertEquals("OKX type 9999", t.Causale("9999", "1", false, ""));
        assertEquals("OKX type 9999 (Qualcosa di nuovo)", t.Causale("9999", "1", false, "Qualcosa di nuovo"));
        assertEquals("OKX type 9999", t.Causale("9999", "1", false, null));
    }

    /** {@code instType} vince su {@code subType} e si confronta senza distinguere maiuscole e minuscole */
    @Test
    public void laTraduzioneDellArchivioConservaLePrecedenzeDiPrima() {
        TipiOKX t = TipiOKX.Carica();
        assertNotNull(t);
        assertEquals("2", t.TipoDaArchivio("SPOT", "1"));
        assertEquals("2", t.TipoDaArchivio("spot", "999"));
        assertEquals("1", t.TipoDaArchivio("-", " 290 "));
        assertEquals("12", t.TipoDaArchivio("-", "200"));
        assertEquals("", t.TipoDaArchivio("-", "999"));
        assertEquals("", t.TipoDaArchivio(null, null));
    }

    /**
     * Le due forme del valore — etichetta fissa e coppia {@code positivo}/{@code negativo} — devono essere
     * entrambe accettate, perché il file è pensato per essere corretto a mano.
     */
    @Test
    public void entrambeLeFormeDelValoreSonoAccettate() {
        TipiOKX t = TipiOKX.Interpreta("""
            {"funding": {"7": "Received", "8": {"positivo": "Transfer in", "negativo": "Transfer out"}}}
            """, "prova");
        assertNotNull(t);
        assertEquals("Received", t.Causale("7", "1", false, ""));
        assertEquals("Received", t.Causale("7", "-1", false, ""));
        assertEquals("Transfer out", t.Causale("8", "-1", false, ""));
    }

    /**
     * Una tabella senza nessun codice è indistinguibile da un file corrotto e renderebbe sconosciuto ogni
     * movimento OKX: viene rifiutata, così {@code Carica()} ripiega sulla copia nel jar invece di importare
     * a vuoto.
     */
    @Test
    public void unaTabellaSenzaCodiciVieneRifiutata() {
        org.junit.jupiter.api.Assertions.assertNull(TipiOKX.Interpreta("{}", "prova"));
        org.junit.jupiter.api.Assertions.assertNull(TipiOKX.Interpreta("{\"trading\": {}}", "prova"));
        org.junit.jupiter.api.Assertions.assertNull(TipiOKX.Interpreta("non e' json", "prova"));
        org.junit.jupiter.api.Assertions.assertNull(TipiOKX.Interpreta(null, "prova"));
    }
}
