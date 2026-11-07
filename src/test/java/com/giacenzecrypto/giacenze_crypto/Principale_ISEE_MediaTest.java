package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Principale_ISEE_Media}: i tratti dei giorni senza prezzo, la lettura del prezzo scritto dall'utente e la
 * scrittura dei prezzi personalizzati, che la valorizzazione della media deve poi trovare (anche quando valgono zero).
 */
class Principale_ISEE_MediaTest {

    @TempDir
    static Path tempDir;

    /** Un address BSC qualunque, ben formato. */
    private static final String ADDRESS = "0x1111111111111111111111111111111111111111";

    @BeforeAll
    static void apreDatabaseTemporaneo() {
        VarStatiche.setWorkingDirectory(tempDir.toString() + "/");
        assertTrue(DatabaseH2.CreaoCollegaDatabase(), "Impossibile creare il database H2 temporaneo per i test");
        VarCondivise.CompilaMappaChain();
    }

    @AfterAll
    static void chiudeDatabase() throws Exception {
        DatabaseH2.connection.close();
        DatabaseH2.connectionPersonale.close();
        DatabaseH2.connectionPrezzi.close();
    }

    private static Moneta moneta(String simbolo, String rete, String address) {
        Moneta m = new Moneta();
        m.Moneta = simbolo;
        m.Tipo = "Crypto";
        m.Rete = rete;
        m.MonetaAddress = address;
        m.Qta = "1";
        return m;
    }

    private static Calcoli_ISEE.PrezzoMancante mancante(Moneta m, String giorno, String qta) {
        Calcoli_ISEE.PrezzoMancante pm = new Calcoli_ISEE.PrezzoMancante(m, LocalDate.parse(giorno), new BigDecimal(qta));
        pm.gruppi.add("Wallet 01");
        return pm;
    }

    @Test
    void iGiorniConsecutiviDellaStessaMonetaFannoUnTratto() {
        Moneta eth = moneta("ETH", "", "");
        Moneta xyz = moneta("XYZ", "", "");
        Calcoli_ISEE.Risultato ris = new Calcoli_ISEE.Risultato(2024, new HashMap<>(), null);
        // ETH dal 1/1 al 3/1, poi il 5/1 (il 4/1 aveva il prezzo). XYZ il 2/1.
        ris.prezziMancanti.add(mancante(eth, "2024-01-01", "2"));
        ris.prezziMancanti.add(mancante(eth, "2024-01-02", "2"));
        ris.prezziMancanti.add(mancante(eth, "2024-01-03", "1.5"));
        ris.prezziMancanti.add(mancante(eth, "2024-01-05", "1.5"));
        ris.prezziMancanti.add(mancante(xyz, "2024-01-02", "100"));

        List<Principale_ISEE_Media.Tratto> tratti = Principale_ISEE_Media.Tratti(ris);

        assertEquals(3, tratti.size());
        assertEquals("ETH", tratti.get(0).moneta.Moneta);
        assertEquals(LocalDate.of(2024, 1, 1), tratti.get(0).dal());
        assertEquals(LocalDate.of(2024, 1, 3), tratti.get(0).al());
        assertEquals(3, tratti.get(0).giorni.size());
        assertEquals(0, new BigDecimal("1.5").compareTo(tratti.get(0).qtaMin()));
        assertEquals(0, new BigDecimal("2").compareTo(tratti.get(0).qtaMax()));
        assertEquals(LocalDate.of(2024, 1, 5), tratti.get(1).dal());
        assertEquals(1, tratti.get(1).giorni.size());
        assertEquals("XYZ", tratti.get(2).moneta.Moneta);
    }

    @Test
    void ilPrezzoScrittoAccettaVirgolaEPunto() {
        assertEquals(0, new BigDecimal("0.05").compareTo(Principale_ISEE_Media.LeggiPrezzo("0,05")));
        assertEquals(0, new BigDecimal("0.05").compareTo(Principale_ISEE_Media.LeggiPrezzo("0.05")));
        assertEquals(0, new BigDecimal("1234.56").compareTo(Principale_ISEE_Media.LeggiPrezzo("1.234,56")));
        assertEquals(0, new BigDecimal("1234.56").compareTo(Principale_ISEE_Media.LeggiPrezzo("1,234.56")));
        assertEquals(0, BigDecimal.ZERO.compareTo(Principale_ISEE_Media.LeggiPrezzo(" 0 ")));
        assertNull(Principale_ISEE_Media.LeggiPrezzo("-1"));
        assertNull(Principale_ISEE_Media.LeggiPrezzo("abc"));
        assertNull(Principale_ISEE_Media.LeggiPrezzo(""));
    }

    @Test
    void valeZero_ilPrezzoScrittoSiTrovaSoloNelSuoGiorno_eLaMediaLoUsa() {
        Moneta xyz = moneta("XYZ", "", "");
        List<Calcoli_ISEE.PrezzoMancante> giorni = List.of(
                mancante(xyz, "2024-03-10", "1000"),
                mancante(xyz, "2024-03-11", "1000"));

        assertTrue(Principale_ISEE_Media.ScriviPrezzi(giorni, BigDecimal.ZERO, null));

        // Il prezzo del 10/03 sta alle 00:00 dell'11/03: si trova lì, non il giorno prima né due giorni dopo.
        long fine10 = Calcoli_ISEE.istanteFineGiornata(LocalDate.of(2024, 3, 10));
        Prezzi.InfoPrezzo ip = Prezzi.DammiPrezzoDaDatabasePersonale("XYZ", fine10, "", "", "", 60, BigDecimal.ONE);
        assertNotNull(ip);
        assertEquals(0, BigDecimal.ZERO.compareTo(ip.prezzoUnitario));
        assertNull(Prezzi.DammiPrezzoDaDatabasePersonale("XYZ",
                Calcoli_ISEE.istanteFineGiornata(LocalDate.of(2024, 3, 9)), "", "", "", 60, BigDecimal.ONE));
        assertNull(Prezzi.DammiPrezzoDaDatabasePersonale("XYZ",
                Calcoli_ISEE.istanteFineGiornata(LocalDate.of(2024, 3, 12)), "", "", "", 60, BigDecimal.ONE));

        // La valorizzazione della media lo trova: zero, non "senza prezzo".
        BigDecimal v = Calcoli_ISEE.VALORE_MERCATO.valore(xyz, new BigDecimal("1000"), LocalDate.of(2024, 3, 10));
        assertNotNull(v, "un prezzo scritto a zero non è un prezzo mancante");
        assertEquals(0, BigDecimal.ZERO.compareTo(v));
    }

    @Test
    void unTokenPerAddressSiScriveConAddressERete() {
        Moneta token = moneta("BabyToken", "BSC", ADDRESS);
        assertTrue(Principale_ISEE_Media.ScriviPrezzi(List.of(mancante(token, "2024-06-30", "5000000")),
                new BigDecimal("0.0000012"), null));

        long fine = Calcoli_ISEE.istanteFineGiornata(LocalDate.of(2024, 6, 30));
        Prezzi.InfoPrezzo ip = Prezzi.DammiPrezzoDaDatabasePersonale("", fine, "", "BSC", ADDRESS, 60, BigDecimal.ONE);
        assertNotNull(ip, "si cerca per address e rete, come la valorizzazione di un token con address");
        assertEquals(0, new BigDecimal("0.0000012").compareTo(ip.prezzoUnitario));
        // 5.000.000 x 0,0000012 = 6 euro
        BigDecimal v = Calcoli_ISEE.VALORE_MERCATO.valore(token, new BigDecimal("5000000"), LocalDate.of(2024, 6, 30));
        assertNotNull(v);
        assertEquals(0, new BigDecimal("6").compareTo(v));
    }
}
