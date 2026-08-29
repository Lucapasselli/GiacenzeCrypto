package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Copre le aggiunte al motore di import generico introdotte per Nexo e Bitget:
 * <ul>
 *   <li>{@code gambaDoppiaConSegnoSuUscita} - riga con due colonne moneta resa a gamba singola quando
 *       Input e Output sono la stessa moneta (Nexo: 'Interest', 'Withdrawal'…), a doppia gamba quando
 *       differiscono ('Exchange');</li>
 *   <li>{@code consolidaCausaliPerGiorno} - somma per (giorno, moneta) le causali indicate (i ~28k
 *       'Interest' di Bitget);</li>
 *   <li>più conversioni nello stesso secondo (Bitget): il percorso multi-riga normale
 *       ({@code TransazioneDefi} + {@code RitornaScambi}) produce un movimento per token ceduto, con
 *       identificativi distinti, senza bisogno di logiche di scissione dedicate.</li>
 * </ul>
 */
class ImportazioneGenericaConsolidamentiTest {

    // ----- gambaDoppiaConSegnoSuUscita (layout tipo Nexo) -----------------------------------------

    /** [0]=id [1]=type [2]=InputCur [3]=InputAmt [4]=OutputCur [5]=OutputAmt [6]=USD [7]=note [8]=data */
    private static ConfigurazioneImport cfgNexo() {
        ConfigurazioneImport cfg = new ConfigurazioneImport();
        cfg.colonnaData = 8;
        cfg.colonnaCausale = 1;
        cfg.colonnaMoneta = 4;
        cfg.colonnaQuantita = 5;
        cfg.colonnaMonetaUscita = 2;
        cfg.colonnaQuantitaUscita = 3;
        cfg.colonnaValoreEuro = 6;   // un prezzo qualunque: evita che creaMovimento vada in rete
        cfg.gambaDoppiaConSegnoSuUscita = true;
        cfg.formatoData = "yyyy-MM-dd HH:mm:ss";
        cfg.fuso = "UTC";
        cfg.mappaCausali.put("Interest", "EARN");
        cfg.mappaCausali.put("Withdrawal", "TRASFERIMENTO-CRYPTO");
        cfg.mappaCausali.put("Exchange", "SCAMBIO CRYPTO-CRYPTO");
        return cfg;
    }

    private static String[] rigaNexo(String type, String inCur, String inAmt, String outCur, String outAmt) {
        return new String[]{"NXT" + type.hashCode(), type, inCur, inAmt, outCur, outAmt, "10", "n", "2025-01-01 12:00:00"};
    }

    @Test
    void interesse_stessaMoneta_diventaUnSoloAccredito() {
        String[] mov = ImportazioneGenerica.costruisciMovimenti(
                rigaNexo("Interest", "NEXO", "0.5", "NEXO", "0.5"), null, cfgNexo()).get(0);

        assertNotNull(mov);
        assertEquals("NEXO", mov[11], "la moneta deve stare fra quelle in entrata");
        assertEquals("", mov[8], "non deve esserci nessuna gamba in uscita: sarebbe un finto scambio NEXO->NEXO");
        assertEquals("0.5", mov[13]);
    }

    @Test
    void prelievo_stessaMoneta_conInputNegativo_diventaUnaSolaUscita() {
        String[] mov = ImportazioneGenerica.costruisciMovimenti(
                rigaNexo("Withdrawal", "LTC", "-0.16522", "LTC", "0.16522"), null, cfgNexo()).get(0);

        assertNotNull(mov);
        assertEquals("LTC", mov[8], "il verso lo detta il segno dell'Input: e' un'uscita");
        assertEquals("", mov[11], "non deve esserci nessuna gamba in entrata");
    }

    @Test
    void topUp_stessaMoneta_conInputEOutputLeggermenteDiversi_restaGambaSingola() {
        // Nexo su 'Top up Crypto' scrive Input 97.87620900 e Output 97.87620937 (differenza = fee di
        // rete): la coincidenza esatta non c'e', ma la moneta e' la stessa e resta un solo deposito,
        // con l'importo della colonna principale.
        String[] mov = ImportazioneGenerica.costruisciMovimenti(
                rigaNexo("Top up Crypto", "USDT", "97.87620900", "USDT", "97.87620937"), null,
                cfgNexoCon("Top up Crypto", "TRASFERIMENTO-CRYPTO")).get(0);

        assertNotNull(mov);
        assertEquals("USDT", mov[11]);
        assertEquals("", mov[8], "niente gamba di uscita: non e' uno scambio USDT->USDT");
        assertEquals("97.87620937", mov[13]);
    }

    private static ConfigurazioneImport cfgNexoCon(String causaleCsv, String tipo) {
        ConfigurazioneImport cfg = cfgNexo();
        cfg.mappaCausali.put(causaleCsv, tipo);
        return cfg;
    }

    @Test
    void topUp_conOutputAmountZero_prendeLImportoDallInput_nonScartato() {
        // Bug reale: il 'Top up Crypto' promozionale 'Campaign Rewards' di Nexo (10/07/2024) porta
        // Input Amount 45.06797732 e Output Amount 0.00000000 (l'unica riga del file cosi'). Prendendo
        // l'importo solo dalla colonna principale (Output = 0) il movimento veniva scartato con "qta=0".
        List<String[]> movs = ImportazioneGenerica.costruisciMovimenti(
                rigaNexo("Top up Crypto", "NEXO", "45.06797732", "NEXO", "0.00000000"), null,
                cfgNexoCon("Top up Crypto", "TRASFERIMENTO-CRYPTO"));

        assertNotNull(movs);
        assertFalse(movs.isEmpty(), "con Output = 0 la riga non deve essere scartata");
        String[] mov = movs.get(0);
        assertEquals("NEXO", mov[11], "Input positivo -> gamba in entrata");
        assertEquals("", mov[8], "nessuna gamba in uscita");
        assertEquals("45.06797732", mov[13], "l'importo si prende dall'Input quando l'Output e' 0");
    }

    // ----- causalePerNota: 'Top up Crypto' promozionale riclassificato da deposito a premio ---------

    @Test
    void topUpPromozionale_notaCampaignRewards_diventaReward_nonDepositoCrypto() {
        ConfigurazioneImport cfg = cfgNexoCon("Top up Crypto", "TRASFERIMENTO-CRYPTO");
        cfg.colonnaNote = 7;
        cfg.causalePerNota.add(new ConfigurazioneImport.RegolaCausaleNota(
                "Top up Crypto", "Campaign Rewards", "REWARD"));

        String[] riga = {"NXT1", "Top up Crypto", "NEXO", "45.06797732", "NEXO", "0.00000000",
                "48.11", "approved / Campaign Rewards", "2024-07-10 14:37:43"};

        String[] mov = ImportazioneGenerica.costruisciMovimenti(riga, null, cfg).get(0);

        assertNotNull(mov);
        assertEquals("REWARD", mov[5], "campo 5");
        assertEquals("RW", mov[0].split("_")[4], "categoria RW, non DC");
        assertEquals("NEXO", mov[11]);
        assertEquals("45.06797732", mov[13]);
    }

    @Test
    void topUpNormale_notaConHashDiTransazione_restaDepositoCrypto() {
        ConfigurazioneImport cfg = cfgNexoCon("Top up Crypto", "TRASFERIMENTO-CRYPTO");
        cfg.colonnaNote = 7;
        cfg.causalePerNota.add(new ConfigurazioneImport.RegolaCausaleNota(
                "Top up Crypto", "Campaign Rewards", "REWARD"));

        String[] riga = {"NXT2", "Top up Crypto", "USDT", "97.87620900", "USDT", "97.87620937",
                "97.89", "approved / 0x070cdd2b57f593890282a29f6085cca1b96b8dddf0b2d736c82d9587ec523c61",
                "2024-01-01 00:00:00"};

        String[] mov = ImportazioneGenerica.costruisciMovimenti(riga, null, cfg).get(0);

        assertEquals("DEPOSITO CRYPTO", mov[5], "un Top up con hash on-chain resta un deposito");
        assertEquals("DC", mov[0].split("_")[4]);
    }

    @Test
    void exchange_moneteDiverse_restaUnoScambioADueGambe() {
        String[] mov = ImportazioneGenerica.costruisciMovimenti(
                rigaNexo("Exchange", "USDT", "-1.8", "BTC", "0.00001806"), null, cfgNexo()).get(0);

        assertNotNull(mov);
        assertEquals("USDT", mov[8], "Input -> gamba in uscita");
        assertEquals("BTC", mov[11], "Output -> gamba in entrata");
    }

    // ----- consolidaCausaliPerGiorno (layout minimo) ---------------------------------------------

    /** [0]=data [1]=causale [2]=moneta [3]=quantita */
    private static ConfigurazioneImport cfgGiorno() {
        ConfigurazioneImport cfg = new ConfigurazioneImport();
        cfg.colonnaData = 0;
        cfg.colonnaCausale = 1;
        cfg.colonnaMoneta = 2;
        cfg.colonnaQuantita = 3;
        cfg.formatoData = "yyyy-MM-dd HH:mm:ss";
        cfg.fuso = "UTC";
        cfg.causaliConsolidaPerGiorno.add("Interest");
        return cfg;
    }

    private static String[] rigaGiorno(String data, String causale, String moneta, String qta) {
        return new String[]{data, causale, moneta, qta};
    }

    @Test
    void interessiSommatiPerGiornoEMoneta_ilRestoInvariato() {
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaGiorno("2025-01-01 06:00:00", "Interest", "NEXO", "0.1"));
        righe.add(rigaGiorno("2025-01-01 12:00:00", "Interest", "NEXO", "0.2"));
        righe.add(rigaGiorno("2025-01-01 08:00:00", "Interest", "BTC", "0.001"));
        righe.add(rigaGiorno("2025-01-02 06:00:00", "Interest", "NEXO", "0.3"));
        righe.add(rigaGiorno("2025-01-01 09:00:00", "Exchange", "USDT", "-5"));

        List<String[]> out = ImportazioneGenerica.consolidaCausaliPerGiorno(righe, cfgGiorno());

        assertEquals(4, out.size(), "3 bucket di interessi + la riga Exchange intatta");

        String nexoGiorno1 = out.stream()
                .filter(r -> r[1].equals("Interest") && r[2].equals("NEXO") && r[0].startsWith("2025-01-01"))
                .map(r -> r[3]).findFirst().orElse(null);
        assertEquals("0.3", nexoGiorno1, "0.1 + 0.2 sullo stesso giorno e moneta");

        assertTrue(out.stream().anyMatch(r -> r[1].equals("Exchange") && r[3].equals("-5")),
                "la riga non elencata in causaliConsolidaPerGiorno passa invariata");
        assertTrue(out.stream().anyMatch(r -> r[2].equals("BTC") && r[3].equals("0.001")),
                "una moneta diversa nello stesso giorno resta un bucket a se'");
    }

    @Test
    void senzaCausaliDaConsolidare_laListaEQuellaDiPartenza() {
        ConfigurazioneImport cfg = cfgGiorno();
        cfg.causaliConsolidaPerGiorno.clear();
        List<String[]> righe = new ArrayList<>();
        righe.add(rigaGiorno("2025-01-01 06:00:00", "Interest", "NEXO", "0.1"));
        assertSame(righe, ImportazioneGenerica.consolidaCausaliPerGiorno(righe, cfg));
    }

    // ----- consolidaGruppo: più conversioni nello stesso secondo -> un movimento per token, ID distinti

    @Test
    void piuConversioniStessoSecondo_unMovimentoPerToken_conIdDistinti() {
        // Layout Bitget ridotto: [0]=order [1]=data [2]=coin [3]=type [4]=amount [5]=valEuro-fittizio.
        // Due token diversi (USDC, ZORA) convertiti in BGB nello stesso secondo: il percorso multi-riga
        // aggrega BGB per simbolo e RitornaScambi produce il prodotto incrociato 2x1 = 2 movimenti, con
        // contatore _001_ / _002_ e [24] = "<order uscita>-<order BGB>" distinti.
        ConfigurazioneImport cfg = new ConfigurazioneImport();
        cfg.nomeExchange = "Bitget";
        cfg.colonnaIDTransazione = 0;
        cfg.colonnaIDGruppo = 1;
        cfg.colonnaData = 1;
        cfg.colonnaCausale = 3;
        cfg.colonnaMoneta = 2;
        cfg.colonnaQuantita = 4;
        cfg.colonnaValoreEuro = 5;   // controvalore fittizio: evita la ricerca prezzi online
        cfg.formatoData = "yyyy-MM-dd HH:mm:ss";
        cfg.fuso = "UTC";
        cfg.mappaCausali.put("Sell", "SCAMBIO CRYPTO-CRYPTO");
        cfg.mappaCausali.put("Buy", "SCAMBIO CRYPTO-CRYPTO");

        String d = "2025-04-25 08:33:21";
        List<String[]> gruppo = List.of(
                new String[]{"1000000000000000001", d, "USDC", "Sell", "-5.68", "5"},
                new String[]{"1000000000000000002", d, "BGB", "Buy", "1.26", "5"},
                new String[]{"1000000000000000003", d, "ZORA", "Sell", "-7.56", "7"},
                new String[]{"1000000000000000004", d, "BGB", "Buy", "1.67", "7"});

        List<String[]> movimenti = ImportazioneGenerica.consolidaGruppo(gruppo, cfg, new ArrayList<>());

        assertEquals(2, movimenti.size(), "due token ceduti, due movimenti");
        assertNotEquals(movimenti.get(0)[0], movimenti.get(1)[0],
                "gli identificativi [0] devono differire: con lo stesso [0] uno sovrascriverebbe l'altro nella mappa");
        assertNotEquals(movimenti.get(0)[24], movimenti.get(1)[24], "anche il campo [24] (deduplica) deve differire");
    }
}
