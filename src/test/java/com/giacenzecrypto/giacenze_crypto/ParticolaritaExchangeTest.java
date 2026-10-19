package com.giacenzecrypto.giacenze_crypto;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tiene insieme la finestra di importazione e la guida {@link DocumentiAiuto#PARTICOLARITA_EXCHANGE}: il collegamento
 * sotto le combo apre la pagina sull'ancora del fornitore selezionato ({@link DocumentiAiuto#AncoraFornitore}), e
 * un'ancora che la pagina non ha non dà nessun errore, il browser mostra semplicemente l'inizio della pagina.
 * <p>Senza questa verifica un exchange aggiunto con una nuova configurazione di {@code config/import/} avrebbe un
 * collegamento che non porta da nessuna parte, e una nuova estrazione di un exchange già documentato non sarebbe
 * spiegata.
 */
public class ParticolaritaExchangeTest {

    private static final File PAGINA = new File("docs/documentazione/particolarita-exchange.md");

    /**
     * Fornitori la cui sezione non è ancora stata scritta: la pagina è partita da Binance e si completa un exchange
     * alla volta. Quando una sezione viene scritta il suo slug va tolto da qui, e il test lo pretende.
     */
    private static final Set<String> DA_SCRIVERE = Set.of(
            "bitget", "bybit", "bybit-eu", "coinbase", "coinbase-pro", "cointracking", "crypto-com",
            "gate-io", "kucoin", "nexo", "nexo-pro", "okx", "tatax");

    /** Stessa cartella di lavoro degli altri test che leggono {@code config/}: la radice del progetto */
    @BeforeEach
    public void riportaLaCartellaDiLavoroSullaRadiceDelProgetto() {
        VarStatiche.setWorkingDirectory(System.getProperty("user.dir") + "/");
    }

    @Test
    public void ogniFornitoreDistribuitoHaLaSuaSezioneOppureEInAttesa() throws Exception {
        Map<String, String> sezioni = Sezioni(Files.readString(PAGINA.toPath(), StandardCharsets.UTF_8));
        Set<String> fornitori = SlugFornitoriDistribuiti();
        assertTrue(fornitori.contains("binance"), "l'elenco dei fornitori non è stato letto: " + fornitori);

        List<String> senzaSezione = new ArrayList<>();
        for (String slug : fornitori) {
            if (!sezioni.containsKey(slug) && !DA_SCRIVERE.contains(slug)) {
                senzaSezione.add(slug);
            }
        }
        assertTrue(senzaSezione.isEmpty(), "fornitori senza sezione \"## Nome {#slug}\" in " + PAGINA + ": "
                + senzaSezione);

        for (String slug : DA_SCRIVERE) {
            assertTrue(!sezioni.containsKey(slug),
                    "la sezione {#" + slug + "} ora c'è: togliere \"" + slug + "\" da DA_SCRIVERE");
            assertTrue(fornitori.contains(slug), "\"" + slug + "\" in DA_SCRIVERE non è più un fornitore");
        }
    }

    @Test
    public void ogniEstrazioneDiUnFornitoreDocumentatoECitataNellaSuaSezione() throws Exception {
        //La sezione di un exchange spiega tutte le sue voci: un'estrazione nuova (un file accessorio, un formato
        //diverso) va aggiunta anche alla guida, con il nome che compare nella seconda combo
        Map<String, String> sezioni = Sezioni(Files.readString(PAGINA.toPath(), StandardCharsets.UTF_8));
        List<String> mancanti = new ArrayList<>();
        for (Map.Entry<String, List<Importazioni_Gestione.VoceImport>> e
                : Importazioni_Gestione.RaccogliEstrazioni().entrySet()) {
            String testo = sezioni.get(DocumentiAiuto.AncoraFornitore(e.getKey()));
            if (testo == null) {
                continue;
            }
            for (Importazioni_Gestione.VoceImport voce : e.getValue()) {
                //Una configurazione "testing" porta in coda all'estrazione un avviso che la guida non ripete
                String estrazione = voce.estrazione.replace(" (In fase di test, utilizzo consapevole)", "");
                if (Distribuita(voce) && !testo.contains(estrazione)) {
                    mancanti.add(e.getKey() + " - " + estrazione);
                }
            }
        }
        assertTrue(mancanti.isEmpty(), "estrazioni non citate nella sezione del loro exchange: " + mancanti);
    }

    @Test
    public void lAncoraEQuellaDelLogoDelFornitore() {
        assertEquals("binance", DocumentiAiuto.AncoraFornitore("Binance"));
        assertEquals("bybit-eu", DocumentiAiuto.AncoraFornitore("Bybit EU"));
        assertEquals("crypto-com", DocumentiAiuto.AncoraFornitore("Crypto.com"));
        assertEquals("gate-io", DocumentiAiuto.AncoraFornitore("Gate.io"));
        //la nota sulla versione minima, in coda al nome di alcune configurazioni, non entra nell'ancora
        assertEquals("kucoin", DocumentiAiuto.AncoraFornitore("KuCoin (utilizzabile dalla versione 1.0.64.06)"));
        assertEquals("Nexo Pro", DocumentiAiuto.NomeFornitore("Nexo Pro (utilizzabile dalla versione 1.0.65.01)"));
    }

    @Test
    public void lIndirizzoPortaLAncoraSoloSeCe() {
        String pagina = DocumentiAiuto.Url(DocumentiAiuto.PARTICOLARITA_EXCHANGE);
        assertEquals(pagina + "#binance", DocumentiAiuto.Url(DocumentiAiuto.PARTICOLARITA_EXCHANGE, "binance"));
        assertEquals(pagina, DocumentiAiuto.Url(DocumentiAiuto.PARTICOLARITA_EXCHANGE, null));
        assertEquals(pagina, DocumentiAiuto.Url(DocumentiAiuto.PARTICOLARITA_EXCHANGE, " "));
    }

    /**
     * @return gli slug dei fornitori che hanno almeno una voce distribuita col programma: import nativi e
     *         configurazioni di {@code config/import/}. Le configurazioni scritte dall'utente nella vecchia
     *         {@code ImportConfig/} non hanno, e non possono avere, una sezione nella guida.
     */
    private static Set<String> SlugFornitoriDistribuiti() {
        Set<String> slug = new TreeSet<>();
        for (Map.Entry<String, List<Importazioni_Gestione.VoceImport>> e
                : Importazioni_Gestione.RaccogliEstrazioni().entrySet()) {
            if (e.getValue().stream().anyMatch(ParticolaritaExchangeTest::Distribuita)) {
                slug.add(DocumentiAiuto.AncoraFornitore(e.getKey()));
            }
        }
        return slug;
    }

    private static boolean Distribuita(Importazioni_Gestione.VoceImport voce) {
        return voce.fileJson == null
                || voce.fileJson.getAbsoluteFile().getParentFile().equals(
                        new File(VarStatiche.getCartella_ConfigImport()).getAbsoluteFile());
    }

    /**
     * Divide la pagina nelle sezioni di secondo livello ({@code ## Nome {#ancora}}).
     * @return ancora → testo della sezione, fino alla successiva di secondo livello
     */
    static Map<String, String> Sezioni(String markdown) {
        Map<String, String> sezioni = new LinkedHashMap<>();
        Matcher m = Pattern.compile("(?m)^## .*\\{#([a-z0-9-]+)\\}\\s*$").matcher(markdown);
        String ancora = null;
        int inizio = 0;
        while (m.find()) {
            if (ancora != null) {
                sezioni.put(ancora, markdown.substring(inizio, m.start()));
            }
            ancora = m.group(1);
            inizio = m.end();
        }
        if (ancora != null) {
            sezioni.put(ancora, markdown.substring(inizio));
        }
        return sezioni;
    }
}
