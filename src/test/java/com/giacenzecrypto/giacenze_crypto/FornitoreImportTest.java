package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

/**
 * Fissa la regola con cui una configurazione di import viene attribuita a un fornitore quando il formato
 * non appartiene a un singolo exchange.
 * <p>Gli exchange si raggruppano da soli, perché la configurazione contiene già il nome della piattaforma.
 * CoinTracking e Tatax no: le loro esportazioni contengono i movimenti di piattaforme diverse, e l'unico
 * indizio è la parola nel nome del file. Sono le due sole eccezioni previste.
 */
public class FornitoreImportTest {

    @Test
    public void leEsportazioniCoinTrackingFinisconoSottoUnSoloFornitore() {
        //Il nome distribuito porta un refuso storico — "Cointraking", senza la seconda c — che deve
        //continuare a essere riconosciuto insieme alla grafia corretta
        assertEquals("CoinTracking", Importazioni_Gestione.FornitoreDaNomeFile("Cointraking"));
        assertEquals("CoinTracking", Importazioni_Gestione.FornitoreDaNomeFile("Cointraking (Vecchio Layout)"));
        assertEquals("CoinTracking", Importazioni_Gestione.FornitoreDaNomeFile("CoinTracking.info"));
        assertEquals("CoinTracking", Importazioni_Gestione.FornitoreDaNomeFile("cointracking_2026"));
        assertEquals("CoinTracking", Importazioni_Gestione.FornitoreDaNomeFile("Export CoinTracking Nuovo"));
    }

    @Test
    public void leEsportazioniTataxFinisconoSottoUnSoloFornitore() {
        assertEquals("Tatax", Importazioni_Gestione.FornitoreDaNomeFile("Tatax (Nuova Versione)"));
        assertEquals("Tatax", Importazioni_Gestione.FornitoreDaNomeFile("tatax"));
        assertEquals("Tatax", Importazioni_Gestione.FornitoreDaNomeFile("TATAX csv 2026"));
    }

    @Test
    public void unaConfigurazioneQualsiasiNonVieneAttribuitaAQuestiFornitori() {
        //Fuori dalle due eccezioni la regola non deve intervenire: il fornitore va ricavato dal
        //nomeExchange della configurazione, altrimenti si accorperebbero import di piattaforme diverse
        assertNull(Importazioni_Gestione.FornitoreDaNomeFile("Binance CSV"));
        assertNull(Importazioni_Gestione.FornitoreDaNomeFile("OKX_Funding"));
        assertNull(Importazioni_Gestione.FornitoreDaNomeFile("Kraken Ledgers"));
        assertNull(Importazioni_Gestione.FornitoreDaNomeFile("La mia configurazione"));
        assertNull(Importazioni_Gestione.FornitoreDaNomeFile(""));
    }
}
