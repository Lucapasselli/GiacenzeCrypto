package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * La parte pura di {@link Aggiornamenti}: lettura delle release di GitHub, scelta per canale e decisione se
 * proporre la versione. Nessuna rete: le release sono JSON costruiti qui, con la stessa forma dell'API.
 */
class AggiornamentiTest {

    private static JSONObject release(String tag, boolean prerelease, boolean draft) {
        JSONObject asset = new JSONObject()
                .put("name", "GiacenzeCrypto_x_Installer_Windows.exe")
                .put("browser_download_url", "https://github.com/x/releases/download/" + tag + "/setup.exe")
                .put("digest", "sha256:abc123")
                .put("size", 1234);
        return new JSONObject()
                .put("tag_name", tag)
                .put("prerelease", prerelease)
                .put("draft", draft)
                .put("html_url", "https://github.com/x/releases/tag/" + tag)
                .put("assets", new JSONArray().put(asset));
    }

    private static Aggiornamenti.Rilascio rilascio(String versione, boolean beta) {
        return new Aggiornamenti.Rilascio(versione, beta, "", List.of());
    }

    @Test
    void leggiRilascio_versioneDalTag_allegatiConSha() {
        Aggiornamenti.Rilascio r = Aggiornamenti.LeggiRilascio(release("Giacenze_Crypto_1.0.65.07", true, false));
        assertEquals("1.0.65.07", r.Versione());
        assertTrue(r.Beta());
        assertEquals(1, r.Allegati().size());
        assertEquals("abc123", r.Allegati().get(0).Sha256(), "il prefisso sha256: si toglie");
        assertEquals(1234, r.Allegati().get(0).Dimensione());
    }

    @Test
    void leggiRilascio_bozzeETagEstranei_scartati() {
        assertNull(Aggiornamenti.LeggiRilascio(release("Giacenze_Crypto_1.0.66", false, true)), "bozza");
        assertNull(Aggiornamenti.LeggiRilascio(release("v1.0.66", false, false)), "prefisso diverso");
        assertNull(Aggiornamenti.LeggiRilascio(release("Giacenze_Crypto_1.0.66-rc", false, false)), "non numerica");
    }

    @Test
    void leggiRilasci_elenco_saltaLeNonValide() {
        JSONArray elenco = new JSONArray()
                .put(release("Giacenze_Crypto_1.0.65.08", true, false))
                .put(release("Giacenze_Crypto_1.0.66", false, true))
                .put(release("Giacenze_Crypto_1.0.65", false, false));
        assertEquals(2, Aggiornamenti.LeggiRilasci(elenco).size());
    }

    @Test
    void migliore_stabileIgnoraLePrerelease() {
        List<Aggiornamenti.Rilascio> l = List.of(rilascio("1.0.65", false), rilascio("1.0.65.08", true));
        assertEquals("1.0.65", Aggiornamenti.Migliore(l, Aggiornamenti.Canale.STABILE).Versione());
        assertEquals("1.0.65.08", Aggiornamenti.Migliore(l, Aggiornamenti.Canale.BETA).Versione());
    }

    @Test
    void migliore_betaVedeLaStabileSuccessiva() {
        // La stabile che chiude la serie è più recente di tutte le sue beta: chi è in beta la riceve
        List<Aggiornamenti.Rilascio> l = List.of(rilascio("1.0.65.09", true), rilascio("1.0.66", false),
                rilascio("1.0.65", false));
        assertEquals("1.0.66", Aggiornamenti.Migliore(l, Aggiornamenti.Canale.BETA).Versione());
    }

    @Test
    void migliore_confrontoNumericoNonLessicografico() {
        List<Aggiornamenti.Rilascio> l = List.of(rilascio("1.0.65.09", true), rilascio("1.0.65.10", true));
        assertEquals("1.0.65.10", Aggiornamenti.Migliore(l, Aggiornamenti.Canale.BETA).Versione());
    }

    @Test
    void valuta_versioneNuova_disponibile() {
        Aggiornamenti.Risultato r = Aggiornamenti.Valuta("1.0.65.07", rilascio("1.0.65.08", true), "");
        assertEquals(Aggiornamenti.Stato.DISPONIBILE, r.Stato());
        assertEquals("1.0.65.08", r.Rilascio().Versione());
    }

    @Test
    void valuta_stessaOPrecedente_aggiornato() {
        assertEquals(Aggiornamenti.Stato.AGGIORNATO,
                Aggiornamenti.Valuta("1.0.65", rilascio("1.0.65", false), "").Stato());
        // Uscire dalla beta non riporta indietro: la stabile 1.0.65 è precedente alla 1.0.65.07 in uso
        assertEquals(Aggiornamenti.Stato.AGGIORNATO,
                Aggiornamenti.Valuta("1.0.65.07", rilascio("1.0.65", false), "").Stato());
    }

    @Test
    void valuta_versioneIgnorata_nonProposta() {
        assertEquals(Aggiornamenti.Stato.AGGIORNATO,
                Aggiornamenti.Valuta("1.0.65", rilascio("1.0.66", false), "1.0.66").Stato());
        assertEquals(Aggiornamenti.Stato.DISPONIBILE,
                Aggiornamenti.Valuta("1.0.65", rilascio("1.0.67", false), "1.0.66").Stato(),
                "ignorata una, la successiva si propone");
    }

    @Test
    void valuta_versioneInUsoSconosciuta_nonLanciaENonPropone() {
        // Il vecchio controllo su SourceForge lanciava NumberFormatException/ArrayIndexOutOfBounds qui
        assertEquals(Aggiornamenti.Stato.SCONOSCIUTO,
                Aggiornamenti.Valuta("sconosciuta", rilascio("1.0.66", false), "").Stato());
        assertEquals(Aggiornamenti.Stato.SCONOSCIUTO,
                Aggiornamenti.Valuta("${project.version}", rilascio("1.0.66", false), "").Stato());
    }

    @Test
    void valuta_nessunaRelease_sconosciutoNonAggiornato() {
        assertEquals(Aggiornamenti.Stato.SCONOSCIUTO, Aggiornamenti.Valuta("1.0.65", null, "").Stato());
    }

    @Test
    void canalePredefinito_versioneIntermedia_beta() {
        assertEquals(Aggiornamenti.Canale.BETA, Aggiornamenti.CanalePredefinito("1.0.65.07", null));
        assertEquals(Aggiornamenti.Canale.STABILE, Aggiornamenti.CanalePredefinito("1.0.65", null));
        assertEquals(Aggiornamenti.Canale.STABILE, Aggiornamenti.CanalePredefinito("sconosciuta", ""));
    }

    @Test
    void canalePredefinito_sceltaSalvataVince() {
        assertEquals(Aggiornamenti.Canale.STABILE, Aggiornamenti.CanalePredefinito("1.0.65.07", "STABILE"));
        assertEquals(Aggiornamenti.Canale.BETA, Aggiornamenti.CanalePredefinito("1.0.65", "BETA"));
        assertEquals(Aggiornamenti.Canale.STABILE, Aggiornamenti.CanalePredefinito("1.0.65", "boh"),
                "un valore non riconosciuto ricade sul predefinito");
    }
}
