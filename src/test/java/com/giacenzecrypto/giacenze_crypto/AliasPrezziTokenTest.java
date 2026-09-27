package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Alias address/rete → moneta quotata sugli exchange, spostati da {@code VarCondivise}/{@code Prezzi} a
 * {@code config/varie/AliasPrezziToken.json}.
 * <p>Come per {@link NoteCompilazioneTest}: finché erano righe di codice il compilatore garantiva almeno
 * che esistessero. Ora un address scritto male verrebbe saltato con una riga nel log, e il token
 * tornerebbe a DefiLlama senza che nessuno se ne accorga. Questi test rimettono quella garanzia, e
 * fissano la regola anti-impersonazione, che marca SCAM in automatico.
 */
class AliasPrezziTokenTest {

    private static final Path FILE = Path.of("config/varie/" + AliasPrezziToken.NOME + ".json");

    private static JSONObject json;
    private static AliasPrezziToken.Tabelle tabelle;

    @BeforeAll
    static void carica() throws Exception {
        String contenuto = Files.readString(FILE);
        json = new JSONObject(contenuto);
        tabelle = AliasPrezziToken.Interpreta(contenuto, AliasPrezziToken.NOME);
        VarCondivise.CompilaMappaRetiSupportate();
        AliasPrezziToken.Carica();
    }

    @Test
    void ogniVoceDelFileEValidaENessunaESaltata() {
        assertNotNull(tabelle);
        JSONArray voci = json.getJSONArray("alias");
        assertEquals(voci.length(), tabelle.alias().size(),
                "una voce saltata (address non valido per la rete, campi mancanti) o una chiave duplicata");
    }

    @Test
    void ogniReteESupportata() {
        JSONArray voci = json.getJSONArray("alias");
        for (int i = 0; i < voci.length(); i++) {
            String rete = voci.getJSONObject(i).getString("rete");
            assertNotNull(Principale.MappaRetiSupportate.get(rete), "rete sconosciuta: " + rete);
        }
    }

    @Test
    void ogniVoceDiRiferimentoHaIlSimboloOnChain() {
        for (Map.Entry<String, String[]> e : tabelle.riferimenti().entrySet()) {
            assertFalse(e.getValue()[0].isBlank(), "riferimento senza simboloToken: " + e.getKey());
        }
    }

    @Test
    void ogniCoppiaReteMonetaHaUnRiferimento() {
        //Una moneta in elenco su una rete senza nessun riferimento sarebbe una rete dove le sue
        //imitazioni non vengono piu' cercate
        Set<String> coppie = new HashSet<>();
        for (Map.Entry<String, String> e : tabelle.alias().entrySet()) {
            coppie.add(e.getKey().substring(e.getKey().lastIndexOf('_') + 1).toUpperCase() + "|" + e.getValue().toUpperCase());
        }
        Set<String> coperte = new HashSet<>();
        for (Map.Entry<String, String[]> e : tabelle.riferimenti().entrySet()) {
            coperte.add(e.getKey().substring(e.getKey().lastIndexOf('_') + 1).toUpperCase() + "|" + e.getValue()[1].toUpperCase());
        }
        coppie.removeAll(coperte);
        assertTrue(coppie.isEmpty(), "rete|moneta senza riferimento: " + coppie);
    }

    @Test
    void leImitazioniCheIlControlloPrendevaPrimaDelFileSonoAncoraPrese() {
        //Per ogni voce dell'elenco scritto nel codice, il vecchio controllo marcava un token con quel
        //simbolo su quella rete e un address diverso: la regola nuova non deve perderne nessuno
        for (Map.Entry<String, String> e : AliasPrezziToken.Predefinite().alias().entrySet()) {
            String rete = e.getKey().substring(e.getKey().lastIndexOf('_') + 1);
            assertNotNull(AliasPrezziToken.MotivoImpersonazione(e.getValue(), ADDRESS_QUALUNQUE, rete),
                    "finto " + e.getValue() + " su " + rete + " non piu' riconosciuto");
        }
    }

    @Test
    void leVociCheEranoNelCodiceCiSonoTutteConLoStessoValore() {
        //Il file e' il sostituto dell'elenco che stava in VarCondivise: la migrazione non deve perderne nessuna
        for (Map.Entry<String, String> e : AliasPrezziToken.Predefinite().alias().entrySet()) {
            assertEquals(e.getValue(), tabelle.alias().get(e.getKey()), "voce persa o cambiata: " + e.getKey());
        }
        for (Map.Entry<String, String> e : AliasPrezziToken.Predefinite().stessoPrezzo().entrySet()) {
            assertEquals(e.getValue(), tabelle.stessoPrezzo().get(e.getKey()), "stessoPrezzo perso: " + e.getKey());
        }
    }

    @Test
    void iNativiWrappedSiPrezzanoComeIlNativo() {
        assertEquals("ETH", Principale.Mappa_AddressRete_Nome.get("0x4200000000000000000000000000000000000006_BASE"));
        assertEquals("BNB", Principale.Mappa_AddressRete_Nome.get("0xbb4cdb9cbd36b01bd1cbaebf2de08d9173bc095c_BSC"));
        assertEquals("SOL", Principale.Mappa_AddressRete_Nome.get("So11111111111111111111111111111111111111112_SOL"));
        assertEquals("BNB", Principale.Mappa_MoneteStessoPrezzo.get("WBNB"));
        assertEquals("ETH", Principale.Mappa_MoneteStessoPrezzo.get("weth"), "la mappa resta case-insensitive");
    }

    @Test
    void wpolNonEMappato() {
        //Prima di settembre 2024 il ticker POL non era MATIC: un alias senza date prezzerebbe male lo storico
        assertNull(Principale.Mappa_AddressRete_Nome.get("0x0d500b1d8e8ef31e21c99d1db9a6444d3adf1270_POL"));
        assertNull(Principale.Mappa_MoneteStessoPrezzo.get("WPOL"));
        assertNull(Principale.Mappa_MoneteStessoPrezzo.get("WMATIC"));
    }

    @Test
    void unaVoceSbagliataVieneSaltataSenzaPerdereLeAltre() {
        String contenuto = """
            {"alias":[
              {"rete":"ETH","address":"0x123","prezzoDa":"ETH"},
              {"rete":"ETH","address":"0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2","prezzoDa":"ETH","tipo":"canonico","simboloToken":"WETH"}
            ]}""";
        AliasPrezziToken.Tabelle t = AliasPrezziToken.Interpreta(contenuto, "prova");
        assertEquals(1, t.alias().size());
        assertEquals(1, t.riferimenti().size());
        assertNull(AliasPrezziToken.Interpreta("non e' json", "prova"));
    }

    // --- Anti-impersonazione ---

    /** Un address valido che non e' in nessuna voce. */
    private static final String ADDRESS_QUALUNQUE = "0x1111111111111111111111111111111111111111";

    @Test
    void unFintoUsdcSullaReteDelCanonicoEImpersonazione() {
        assertNotNull(AliasPrezziToken.MotivoImpersonazione("USDC", ADDRESS_QUALUNQUE, "ARB"));
    }

    @Test
    void unFintoUsdtEImpersonazioneAncheSeIlVeroSiChiamaUsdt0() {
        //Su Arbitrum l'USDT canonico e' USDT0: il confronto col simbolo di prezzo e' quello che lo prende
        assertNotNull(AliasPrezziToken.MotivoImpersonazione("USDT", ADDRESS_QUALUNQUE, "ARB"));
    }

    @Test
    void unBridgedInElencoNonEMaiImpersonazione() {
        //USDC.e su Arbitrum, spesso importato col simbolo "USDC"
        assertNull(AliasPrezziToken.MotivoImpersonazione("USDC", "0xff970a61a04b1ca14834a43f5de4533ebddb5cc8", "ARB"));
    }

    @Test
    void suUnaReteSenzaCanonicoIlRiferimentoEIlBridged() {
        //Su BSC l'USDT e' solo BSC-USD (bridged): e' lui il riferimento, e un finto USDT e' l'imitazione
        //piu' comune in assoluto
        assertNotNull(AliasPrezziToken.MotivoImpersonazione("USDT", ADDRESS_QUALUNQUE, "BSC"));
    }

    @Test
    void doveCeIlCanonicoIlBridgedNonEUnRiferimento() {
        //Su Arbitrum il riferimento dell'USDC e' il nativo; USDC.e non deve far marcare altri "USDC.e"
        assertNull(AliasPrezziToken.MotivoImpersonazione("USDC.e", ADDRESS_QUALUNQUE, "ARB"));
    }

    @Test
    void ilRipiegoNonSpegneIlControllo() {
        assertFalse(AliasPrezziToken.Predefinite().riferimenti().isEmpty());
    }

    @Test
    void laMonetaNativaNonEImpersonazione() {
        //La moneta nativa non ha un address di contratto: un segnaposto non valido non deve farla marcare SCAM
        assertNull(AliasPrezziToken.MotivoImpersonazione("ETH", "ETH", "ARB"));
    }

    @Test
    void ilTokenCanonicoNonImitaSeStesso() {
        assertNull(AliasPrezziToken.MotivoImpersonazione("USDC", "0xaf88d065e77c8cc2239327c5edb3a432268e5831", "ARB"));
    }

    @Test
    void unNomeQualsiasiNonEImpersonazione() {
        assertNull(AliasPrezziToken.MotivoImpersonazione("PEPE", ADDRESS_QUALUNQUE, "ARB"));
    }

    @Test
    void nessunAddressDuplicatoFraReti() {
        Set<String> viste = new HashSet<>();
        JSONArray voci = json.getJSONArray("alias");
        for (int i = 0; i < voci.length(); i++) {
            JSONObject v = voci.getJSONObject(i);
            assertTrue(viste.add((v.getString("address") + "_" + v.getString("rete")).toLowerCase()),
                    "voce duplicata: " + v);
        }
    }
}
