package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Font;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Token truffa marcati SCAM all'importazione ({@link TokenTruffa}): scheletro dei simboli, le tre regole, le condizioni
 * comuni e le esclusioni, e il ripiego del font per i simboli che Noto Sans non disegna. Indirizzi inventati; la storia
 * vera di un wallet colpito da address poisoning, se c'è, sta in {@code nocommit/DatiTestChain/TOKENTRUFFA/}.
 */
class TokenTruffaTest {

    private static final Path DATI_REALI = Path.of("nocommit/DatiTestChain/TOKENTRUFFA");
    private static final String W = "0xaaaa000000000000000000000000000000000001";
    private static final String FALSO = "0xfa15e00000000000000000000000000000000001";
    private static final String VERO = "0x0e0e000000000000000000000000000000000002";

    /** Simboli dei falsi visti sul wallet ARB: lisu + khmer + armeno + cherokee, latino + cirillico, ecc. */
    private static final String USDC_LISU = "ꓴ឵ՏᎠ឵Ꮳ";
    private static final String USDC_CANADESE = "ᑌ‌​S‌​ᗞ​‌С";
    private static final String USDC_ACCENTO = "ÚSDС";
    private static final String ETH_CIRILLICO = "EТH";
    private static final String ETH_PUNTI = "E.ꓔ.H ";
    private static final String USDT_TETHER = "USD₮";
    private static final String USDT0_TETHER = "USD₮0";

    private static TokenTruffa.Config cfg;

    @BeforeAll
    static void Configurazione() throws Exception {
        cfg = TokenTruffa.Interpreta(Files.readString(Path.of("config/varie/TokenTruffa.json")), TokenTruffa.NOME);
        assertNotNull(cfg, "config/varie/TokenTruffa.json non interpretabile");
    }

    /** Una riga di deposito (DC) o prelievo (PC) di un token, nella forma di TransazioneDefi.RitornaRigheTabella. */
    private static String[] Riga(String istante, String tipo, String simbolo, String nome, String address, String qta, String prezzato) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = istante + "_BC.ARB." + W + ".0x" + istante + "_1_1_" + tipo;
        v[3] = W + " (ARB)";
        v[4] = "Wallet";
        if (tipo.equals("PC")) {
            v[8] = simbolo;
            v[10] = qta;
            v[25] = nome;
            v[26] = address;
        } else {
            v[11] = simbolo;
            v[13] = qta;
            v[27] = nome;
            v[28] = address;
        }
        v[15] = "SI".equals(prezzato) ? "10.00" : "0.00";
        v[32] = prezzato;
        v[34] = "ARB";
        return v;
    }

    private static TokenTruffa.Riga NonFirmata(String[] v) {
        return new TokenTruffa.Riga(v, true, false, true);
    }

    private static List<TokenTruffa.Marcatura> Valuta(List<String[]> archivio, TokenTruffa.Riga... righe) {
        return TokenTruffa.Valuta(List.of(righe), archivio, cfg, null);
    }

    @Test
    void loScheletroRiconosceISosiaVisti() {
        Map<Integer, String> agg = cfg.confondibiliAggiuntivi();
        assertEquals("USDC", TokenTruffa.Scheletro(USDC_LISU, agg));
        assertEquals("USDC", TokenTruffa.Scheletro(USDC_CANADESE, agg));
        assertEquals("USDC", TokenTruffa.Scheletro(USDC_ACCENTO, agg));
        assertEquals("ETH", TokenTruffa.Scheletro(ETH_CIRILLICO, agg));
        assertEquals("ETH", TokenTruffa.Scheletro(ETH_PUNTI, agg));
        for (String s : List.of(USDC_LISU, USDC_CANADESE, USDC_ACCENTO, ETH_CIRILLICO, ETH_PUNTI)) {
            assertNotNull(TokenTruffa.TickerImitato(s, cfg), s);
        }
    }

    @Test
    void iSimboliVeriConCaratteriParticolariNonSonoSosia() {
        //Tether scrive di suo "USD" col segno del tugrik su varie reti: non è una lettera straniera
        assertNull(TokenTruffa.TickerImitato(USDT_TETHER, cfg));
        assertNull(TokenTruffa.TickerImitato(USDT0_TETHER, cfg));
        //Token veri di CoinGecko con scritture miste o caratteri invisibili, ma che non imitano nessun ticker
        assertNull(TokenTruffa.TickerImitato("ǝԁǝԁ", cfg)); //Pepe Inverted
        assertNull(TokenTruffa.TickerImitato("τDoge", cfg));
        assertNull(TokenTruffa.TickerImitato("​​Stable", cfg));
        assertNull(TokenTruffa.TickerImitato("laïka", cfg));
        //ASCII: non è mai R1, anche quando è identico (lo prende solo R0)
        assertNull(TokenTruffa.TickerImitato("ETH", cfg));
        assertNull(TokenTruffa.TickerImitato("USDC", cfg));
    }

    @Test
    void r0PrelievoDiUnTokenMaiEntrato() {
        String[] v = Riga("20260101120000", "PC", "ETH", "ETH", FALSO, "-0.125", "NO");
        List<TokenTruffa.Marcatura> m = Valuta(List.of(), NonFirmata(v));
        assertEquals(1, m.size());
        assertEquals(TokenTruffa.Regola.R0, m.get(0).regola());
        assertEquals(FALSO + "_ARB", m.get(0).chiave());
    }

    @Test
    void r0NonScattaSeIlTokenEraEntratoOLaStoriaNonECompleta() {
        String[] entrata = Riga("20251201120000", "DC", "ETH", "ETH", FALSO, "0.2", "NO");
        String[] uscita = Riga("20260101120000", "PC", "ETH", "ETH", FALSO, "-0.125", "NO");
        assertTrue(Valuta(List.<String[]>of(entrata), NonFirmata(uscita)).isEmpty(), "entrato in archivio");
        assertTrue(TokenTruffa.Valuta(List.of(new TokenTruffa.Riga(entrata, true, true, true), NonFirmata(uscita)),
                List.of(), cfg, null).isEmpty(), "entrato prima nella stessa importazione");
        assertTrue(Valuta(List.of(), new TokenTruffa.Riga(uscita, true, false, false)).isEmpty(), "storia parziale (nodi pubblici)");
    }

    @Test
    void leCondizioniComuniTengonoFuoriIlDubbio() {
        String[] base = Riga("20260101120000", "PC", ETH_CIRILLICO, ETH_CIRILLICO, FALSO, "-1", "NO");
        assertEquals(1, Valuta(List.of(), NonFirmata(base)).size());
        assertTrue(Valuta(List.of(), new TokenTruffa.Riga(base, true, true, true)).isEmpty(), "firmata dal wallet");
        assertTrue(Valuta(List.of(), new TokenTruffa.Riga(base, false, false, true)).isEmpty(), "firma non verificabile");
        assertTrue(Valuta(List.of(), NonFirmata(Riga("20260101120000", "PC", ETH_CIRILLICO, "", FALSO, "-1", "SI"))).isEmpty(),
                "con prezzo");
        assertTrue(Valuta(List.of(), NonFirmata(Riga("20260101120000", "PC", ETH_CIRILLICO, "", FALSO, "-0", "NO"))).isEmpty(),
                "quantità zero");
        assertTrue(Valuta(List.of(), NonFirmata(Riga("20260101120000", "PC", ETH_CIRILLICO, "", "ETH", "-1", "NO"))).isEmpty(),
                "moneta della rete, non un contratto");
        String[] prezzatoAltrove = Riga("20250101120000", "DC", ETH_CIRILLICO, "", FALSO, "1", "SI");
        assertTrue(Valuta(List.<String[]>of(prezzatoAltrove), NonFirmata(base)).isEmpty(), "prezzato in archivio");
        assertTrue(TokenTruffa.Valuta(List.of(NonFirmata(base)), List.of(), cfg, (a, r, s) -> true).isEmpty(), "escluso");
        assertTrue(Valuta(List.of(), NonFirmata(Riga("20260101120000", "PC", ETH_CIRILLICO + " **", "", FALSO, "-1", "NO")))
                .isEmpty(), "già SCAM");
    }

    @Test
    void r1SosiaInEntrataER2LinkEsplicito() {
        List<TokenTruffa.Marcatura> m = Valuta(List.of(),
                NonFirmata(Riga("20260101120000", "DC", USDC_ACCENTO, USDC_ACCENTO, FALSO, "300", "NO")),
                NonFirmata(Riga("20260101130000", "DC", "Visit https://esempio.invalid to claim", "​ ETH", VERO, "5", "NO")));
        assertEquals(List.of(TokenTruffa.Regola.R1, TokenTruffa.Regola.R2), m.stream().map(TokenTruffa.Marcatura::regola).toList());
        assertEquals("USDC", m.get(0).dettaglio());
        //Un dominio senza link non basta: Ether.fi, Curve.fi, Crypto.com sono token veri
        assertTrue(Valuta(List.of(), NonFirmata(Riga("20260101120000", "DC", "ETHFI", "Ether.fi", VERO, "5", "NO"))).isEmpty());
        assertTrue(Valuta(List.of(), NonFirmata(Riga("20260101120000", "DC", "EVER", "TheEver.io", VERO, "5", "NO"))).isEmpty());
    }

    @Test
    void unTokenSoloUnaVolta() {
        List<TokenTruffa.Marcatura> m = Valuta(List.of(),
                NonFirmata(Riga("20260101120000", "PC", USDC_LISU, USDC_LISU, FALSO, "-300", "NO")),
                NonFirmata(Riga("20260101130000", "PC", USDC_LISU, USDC_LISU, FALSO, "-701", "NO")));
        assertEquals(1, m.size());
    }

    @Test
    void ilResocontoElencaIToken() {
        String r = TokenTruffa.Resoconto(List.of(new TokenTruffa.Marcatura(FALSO + "_ARB", FALSO, "ARB", "ETH", TokenTruffa.Regola.R0, "")));
        assertTrue(r.contains("Token marcati SCAM in automatico: 1"), r);
        assertTrue(r.contains("0xfa15...0001"), r);
        assertEquals("", TokenTruffa.Resoconto(List.of()));
    }

    @Test
    void ilFontRipiegaSoloDoveServeENonConLeEmoji() {
        FontApplicazione.Registra();
        Font f = FontApplicazione.Font(Font.PLAIN, 12);
        assertSame(f, FontApplicazione.PerTesto(f, "Giacenze USDC 1.000,00"), "testo normale: stesso font");
        assertSame(f, FontApplicazione.PerTesto(f, "🐷"), "emoji a colori: resta il font di partenza");
        Font r = FontApplicazione.PerTesto(f, USDC_LISU);
        assertEquals(f.getSize2D(), r.getSize2D());
        assertEquals(f.getStyle(), r.getStyle());
        //Il ripiego non cambia le metriche dei caratteri che Noto Sans ha già
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics g = img.getGraphics();
        assertEquals(g.getFontMetrics(f).stringWidth("Giacenze Crypto 123"), g.getFontMetrics(r).stringWidth("Giacenze Crypto 123"));
    }

    /**
     * La storia vera di un wallet ARB colpito da address poisoning (righe dell'importazione con le informazioni sulla
     * firma, da {@code DumpRigheImport}): le regole devono prendere solo i token falsi verificati on-chain. Attese in
     * {@code attese.txt}: una riga {@code regola address} per token.
     */
    @Test
    void storiaVeraDiUnWalletAvvelenato() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(DATI_REALI), "dati di prova assenti (" + DATI_REALI + ")");
        List<Path> cartelle;
        try (var s = Files.list(DATI_REALI)) {
            cartelle = s.filter(Files::isDirectory).filter(p -> Files.exists(p.resolve("righe.tsv"))).toList();
        }
        Assumptions.assumeFalse(cartelle.isEmpty());
        for (Path c : cartelle) {
            List<String> tsv = Files.readAllLines(c.resolve("righe.tsv"), StandardCharsets.UTF_8);
            List<String> intestazione = Arrays.asList(tsv.get(0).split("\t", -1));
            int fv = intestazione.indexOf("firmaVerificabile"), fi = intestazione.indexOf("firmata"),
                    sc = intestazione.indexOf("storiaCompleta"), ri = intestazione.indexOf("riga");
            List<TokenTruffa.Riga> righe = new ArrayList<>();
            for (String l : tsv.subList(1, tsv.size())) {
                String[] t = l.split("\t", -1);
                String[] v = Arrays.copyOf(t[ri].split(";", -1), Importazioni.ColonneTabella);
                for (int i = 0; i < v.length; i++) if (v[i] == null) v[i] = "";
                righe.add(new TokenTruffa.Riga(v, Boolean.parseBoolean(t[fv]), Boolean.parseBoolean(t[fi]), Boolean.parseBoolean(t[sc])));
            }
            Set<String> trovate = TokenTruffa.Valuta(righe, List.of(), cfg, null).stream()
                    .map(m -> m.regola() + " " + m.address().toLowerCase()).collect(Collectors.toSet());
            Set<String> attese = Files.readAllLines(c.resolve("attese.txt"), StandardCharsets.UTF_8).stream()
                    .map(String::trim).filter(x -> !x.isEmpty() && !x.startsWith("#")).collect(Collectors.toSet());
            assertEquals(attese, trovate, c.getFileName().toString());
        }
    }
}
