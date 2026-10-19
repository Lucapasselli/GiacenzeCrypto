package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Fissa come la finestra di importazione distingue una configurazione JSON scritta dall'utente da una
 * distribuita col programma: il criterio è il flag {@code "centralizzato"}, lo stesso che usano la
 * sincronizzazione con il repository, il backup e la lettura della vecchia cartella {@code ImportConfig/}.
 * Una configurazione personalizzata ha il prefisso "Personalizzata: ", è in corsivo, sta in coda alle voci
 * ufficiali del suo fornitore e ha sempre un tooltip che lo dice.
 */
class ConfigImportPersonalizzataTest {

    /** Stessa cartella di lavoro degli altri test che leggono {@code config/}: la radice del progetto */
    @BeforeEach
    void riportaLaCartellaDiLavoroSullaRadiceDelProgetto() {
        VarStatiche.setWorkingDirectory(System.getProperty("user.dir") + "/");
    }

    @Test
    void ogniConfigurazioneDistribuitaECentralizzata() throws Exception {
        //Senza il flag una configurazione ufficiale comparirebbe come personalizzata, finirebbe nei backup e non
        //verrebbe cancellata se tolta dal repository: fino al 2026-10-19 ne mancavano dieci
        List<String> senza = new ArrayList<>();
        File[] elenco = new File("config/import").listFiles((d, n) -> n.toLowerCase().endsWith(".json"));
        assertNotNull(elenco);
        for (File f : elenco) {
            JSONObject o = new JSONObject(Files.readString(f.toPath(), StandardCharsets.UTF_8));
            if (!o.optBoolean("centralizzato", false)) {
                senza.add(f.getName());
            }
        }
        assertTrue(senza.isEmpty(), "configurazioni di config/import senza \"centralizzato\": true: " + senza);
    }

    @Test
    void nessunaVoceDistribuitaRisultaPersonalizzata() {
        List<String> personalizzate = new ArrayList<>();
        for (Map.Entry<String, List<Importazioni_Gestione.VoceImport>> e
                : Importazioni_Gestione.RaccogliEstrazioni().entrySet()) {
            for (Importazioni_Gestione.VoceImport v : e.getValue()) {
                if (v.personalizzata) {
                    personalizzate.add(e.getKey() + " - " + v.estrazione + " (" + v.fileJson + ")");
                }
            }
        }
        assertTrue(personalizzate.isEmpty(), "voci distribuite segnate come personalizzate: " + personalizzate);
    }

    @Test
    void unaVocePersonalizzataSiRiconosceDaNomeCorsivoETooltip() {
        var v = new Importazioni_Gestione.VoceImport("Bybit", "Mio Bybit", null, new File("ImportConfig/Mio Bybit.json"),
                "Funding & UTA <insieme>", true);
        assertEquals("Personalizzata: Mio Bybit", v.toString());
        assertTrue(v.inCorsivo());
        String t = v.tooltip();
        assertTrue(t.contains("Configurazione personalizzata"), t);
        assertTrue(t.contains("Mio Bybit.json") && t.contains("ImportConfig"), "il tooltip dice quale file e dove: " + t);
        assertTrue(t.contains("Funding &amp; UTA &lt;insieme&gt;"), "la descrizione in testo semplice va resa innocua: " + t);

        var senzaDescrizione = new Importazioni_Gestione.VoceImport("Kraken", "Kraken", null, new File("x.json"), null, true);
        assertNotNull(senzaDescrizione.tooltip(), "una personalizzata ha il tooltip anche senza descrizione");
    }

    @Test
    void leVociUfficialiENativeRestanoComeErano() {
        var ufficiale = new Importazioni_Gestione.VoceImport("Bybit", "Funding", null, new File("x.json"),
                "<html>Funding</html>", false);
        assertEquals("Funding", ufficiale.toString());
        assertFalse(ufficiale.inCorsivo());
        assertEquals("<html>Funding</html>", ufficiale.tooltip());

        var nativa = new Importazioni_Gestione.VoceImport("Binance", "Formato storico", "NAT_BINANCE_OLD", null, null, true);
        assertFalse(nativa.personalizzata, "un import nativo non è mai una configurazione personalizzata");
        assertEquals("Formato storico", nativa.toString());
    }
}
