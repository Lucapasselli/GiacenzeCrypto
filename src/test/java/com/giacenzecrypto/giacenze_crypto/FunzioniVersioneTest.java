package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Caratterizza {@link Funzioni#ConfrontaVersioni} e {@link Funzioni#VersioneAppAlmeno}, la base del
 * gate di {@code ConfigurazioneImport.versioneMinimaApp} (vedi {@code Importazioni_Gestione.
 * raccogliEstrazioni}): una configurazione di import che usa una funzionalità del formato più
 * recente di questa installazione non deve comparire nella finestra di import, altrimenti l'utente
 * la sceglierebbe e otterrebbe un import silenziosamente sbagliato invece di non vederla proposta.
 */
class FunzioniVersioneTest {

    @Test
    void confronta_maggioreMinoreUguale() {
        assertTrue(Funzioni.ConfrontaVersioni("1.0.65", "1.0.64") > 0);
        assertTrue(Funzioni.ConfrontaVersioni("1.0.64", "1.0.65") < 0);
        assertEquals(0, Funzioni.ConfrontaVersioni("1.0.64", "1.0.64"));
    }

    @Test
    void confronta_numeroDiSegmentiDiverso_mancantiValgonoZero() {
        assertTrue(Funzioni.ConfrontaVersioni("1.0.58.03", "1.0.58") > 0, "un quarto segmento presente conta come maggiore di zero");
        assertEquals(0, Funzioni.ConfrontaVersioni("1.0.65", "1.0.65.0"));
        assertTrue(Funzioni.ConfrontaVersioni("1.2", "1.0.65") > 0, "il secondo segmento (2) pesa più del terzo mancante");
    }

    @Test
    void confronta_nonSiFermaAlPrimoSegmentoUguale() {
        // Un confronto lessicografico sbaglierebbe "1.9" vs "1.10" (9 > 1 come cifra); qui è numerico.
        assertTrue(Funzioni.ConfrontaVersioni("1.10", "1.9") > 0);
    }

    @Test
    void versioneMinimaVuota_nessunVincolo() {
        assertTrue(Funzioni.VersioneAppAlmeno("1.0.64", ""));
        assertTrue(Funzioni.VersioneAppAlmeno("1.0.64", null));
    }

    @Test
    void versioneAppVecchia_sottoLaMinima_falso() {
        assertFalse(Funzioni.VersioneAppAlmeno("1.0.64", "1.0.65"));
    }

    @Test
    void versioneAppAllaMinimaOSopra_vero() {
        assertTrue(Funzioni.VersioneAppAlmeno("1.0.65", "1.0.65"));
        assertTrue(Funzioni.VersioneAppAlmeno("1.0.66", "1.0.65"));
        assertTrue(Funzioni.VersioneAppAlmeno("1.1.0", "1.0.65"));
    }

    @Test
    void versioneAppNonInterpretabile_nonBlocca() {
        // "sconosciuta" (VarStatiche.leggiVersione quando il filtering Maven non è avvenuto):
        // per prudenza non si nasconde la configurazione, si lascia decidere a ConfigurazioneImport.carica.
        assertTrue(Funzioni.VersioneAppAlmeno("sconosciuta", "1.0.65"));
        assertTrue(Funzioni.VersioneAppAlmeno("", "1.0.65"));
        assertTrue(Funzioni.VersioneAppAlmeno(null, "1.0.65"));
    }

    @Test
    void versioneConQuartoSegmento_sbloccaLaMinimaDellaStessaVersione() {
        //Le configurazioni Gate.io chiedono "1.0.64.01": la 1.0.64 rilasciata non deve vederle,
        //la build 1.0.64.01 e tutte le successive sì ("01" vale 1, non una stringa)
        assertFalse(Funzioni.VersioneAppAlmeno("1.0.64", "1.0.64.01"));
        assertTrue(Funzioni.VersioneAppAlmeno("1.0.64.01", "1.0.64.01"));
        assertTrue(Funzioni.VersioneAppAlmeno("1.0.64.1", "1.0.64.01"));
        assertTrue(Funzioni.VersioneAppAlmeno("1.0.65", "1.0.64.01"));
    }

    @Test
    void configurazioniGateIo_richiedonoLa1_0_64_01() throws Exception {
        for (String nome : new String[]{
            "Gate.io Spot Trades.json", "Gate.io Withdrawals.json", "Gate.io Deposits.json"
        }) {
            ImportazioneGenerica.ConfigurazioneImport cfg =
                    ImportazioneGenerica.ConfigurazioneImport.carica("config/import/" + nome);
            assertEquals("1.0.64.01", cfg.versioneMinimaApp, nome);
            //nomeExchange diventa il campo [3] dei movimenti e la chiave del gruppo wallet: deve
            //restare "Gate.io" anche se l'etichetta del fornitore nella finestra di import cambia
            assertEquals("Gate.io", cfg.nomeExchange, nome);
        }
    }

    /**
     * Punto E2E, non solo la funzione pura: legge la versione REALMENTE filtrata da Maven in questo
     * build ({@code VarStatiche.Versione}, da {@code version.properties} ← {@code pom.xml}) e verifica
     * che il gate si comporti secondo quella versione, qualunque essa sia — non un valore fisso
     * che smetterebbe di avere senso al primo bump del pom: il fallimento indicherebbe solo un
     * disallineamento reale fra la versione minima dei config e quella del pom.
     */
    @Test
    void gateSuiConfigGateIo_coerenteConLaVersioneRealmenteFiltrataDalPom() throws Exception {
        String versioneApp = VarStatiche.Versione;
        assertNotEquals("sconosciuta", versioneApp,
                "version.properties non filtrato da Maven in questa build: il test non può dire nulla");

        boolean appAlmenoMinima = Funzioni.ConfrontaVersioni(versioneApp, "1.0.64.01") >= 0;
        for (String nome : new String[]{
            "Gate.io Spot Trades.json", "Gate.io Withdrawals.json", "Gate.io Deposits.json"
        }) {
            ImportazioneGenerica.ConfigurazioneImport cfg =
                    ImportazioneGenerica.ConfigurazioneImport.carica("config/import/" + nome);
            boolean proposta = Funzioni.VersioneAppAlmeno(versioneApp, cfg.versioneMinimaApp);
            assertEquals(appAlmenoMinima, proposta,
                    nome + ": app " + versioneApp + " vs minima " + cfg.versioneMinimaApp);
        }
    }
}
