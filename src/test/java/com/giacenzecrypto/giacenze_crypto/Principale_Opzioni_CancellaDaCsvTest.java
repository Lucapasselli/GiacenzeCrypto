package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

/**
 * Cancellazione di movimenti da un elenco CSV: lettura del file e ricerca nell'archivio. Dati inventati.
 */
class Principale_Opzioni_CancellaDaCsvTest {

    private static String[] mov(String id, String exchange, String tipo, String campo18, String gruppo20, String campo22) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        v[1] = id.substring(0, 4) + "-" + id.substring(4, 6) + "-" + id.substring(6, 8) + " "
                + id.substring(8, 10) + ":" + id.substring(10, 12);
        v[3] = exchange;
        v[4] = "Principale";
        v[5] = tipo;
        v[18] = campo18;
        v[20] = gruppo20;
        v[22] = campo22;
        return v;
    }

    private static TreeMap<String, String[]> archivio(String[]... righe) {
        TreeMap<String, String[]> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String[] v : righe) m.put(v[0], v);
        return m;
    }

    private static final String A = "20240101100000_Prova_001_001_DC";
    private static final String B = "20240102100000_Prova$_001_001_DC";
    private static final String C = "20240103100000_Altro_001_001_PC";
    private static final String D = "20240104100000_Altro_001_001_DC";

    private static TreeMap<String, String[]> base() {
        return archivio(
                mov(A, "Prova", "DEPOSITO CRYPTO", "", "", ""),
                mov(B, "Prova", "DEPOSITO CRYPTO (a costo zero)", "DCZ - Deposito a costo zero", "", ""),
                mov(C, "Altro", "TRASFERIMENTO TRA WALLET", "PTW - Trasferimento", D, ""),
                mov(D, "Altro", "TRASFERIMENTO TRA WALLET", "DTW - Trasferimento", C, ""));
    }

    @Test
    void ElencoDiIdSenzaIntestazione() throws Exception {
        Principale_Opzioni_CancellaDaCsv.Criteri c = Principale_Opzioni_CancellaDaCsv.Leggi("﻿" + B + "\r\n" + A + "\r\n\r\n");
        assertFalse(c.conIntestazione());
        assertEquals(2, c.criteri().size());
        Principale_Opzioni_CancellaDaCsv.Esito e = Principale_Opzioni_CancellaDaCsv.Trova(c, base());
        //nell'ordine dell'archivio, non del file
        assertEquals(List.of(A, B), e.ids());
        assertEquals(2, e.rigaDi().get(A));
        assertTrue(e.senzaCorrispondenza().isEmpty());
    }

    @Test
    void IdConIntestazioneEMaiuscoleDiverse() throws Exception {
        Principale_Opzioni_CancellaDaCsv.Criteri c = Principale_Opzioni_CancellaDaCsv.Leggi("ID\n" + A.toLowerCase() + "\n");
        assertTrue(c.conIntestazione());
        assertEquals(List.of(A), Principale_Opzioni_CancellaDaCsv.Trova(c, base()).ids());
    }

    @Test
    void IdInesistenteCompareFraLeRigheSenzaCorrispondenza() throws Exception {
        Principale_Opzioni_CancellaDaCsv.Criteri c = Principale_Opzioni_CancellaDaCsv.Leggi("ID\n" + A + "\n20991231000000_Prova_001_001_DC\n");
        Principale_Opzioni_CancellaDaCsv.Esito e = Principale_Opzioni_CancellaDaCsv.Trova(c, base());
        assertEquals(List.of(A), e.ids());
        assertEquals(1, e.senzaCorrispondenza().size());
        assertEquals(3, e.senzaCorrispondenza().get(0).riga());
        assertEquals(List.of(1, 0), e.trovatiPerCriterio());
    }

    @Test
    void CombinazioneDiCampiTuttiUguali() throws Exception {
        //exchange Prova E tipo DCZ: solo B, non A che è dello stesso exchange
        Principale_Opzioni_CancellaDaCsv.Criteri c = Principale_Opzioni_CancellaDaCsv.Leggi(
                "[3];campo 18\nProva;DCZ - Deposito a costo zero\n");
        assertEquals(List.of(3, 18), c.colonne());
        assertEquals(List.of(B), Principale_Opzioni_CancellaDaCsv.Trova(c, base()).ids());
    }

    @Test
    void CellaVuotaValeQualsiasiValoreERigheInOR() throws Exception {
        Principale_Opzioni_CancellaDaCsv.Criteri c = Principale_Opzioni_CancellaDaCsv.Leggi(
                "3,5\nProva,\n,TRASFERIMENTO TRA WALLET\n");
        Principale_Opzioni_CancellaDaCsv.Esito e = Principale_Opzioni_CancellaDaCsv.Trova(c, base());
        assertEquals(List.of(A, B, C, D), e.ids());
        assertEquals(List.of(2, 2), e.trovatiPerCriterio());
        assertEquals(3, e.rigaDi().get(C));
    }

    @Test
    void ConfrontoEsattoSenzaMaiuscoleNeNumeriNormalizzati() throws Exception {
        TreeMap<String, String[]> m = base();
        m.get(A)[10] = "-0.6";
        Principale_Opzioni_CancellaDaCsv.Criteri c = Principale_Opzioni_CancellaDaCsv.Leggi("10;3\n-0.60;\n;prova\n");
        Principale_Opzioni_CancellaDaCsv.Esito e = Principale_Opzioni_CancellaDaCsv.Trova(c, m);
        assertTrue(e.ids().isEmpty());
        assertEquals(2, e.senzaCorrispondenza().size());
        //gli spazi ai lati invece non contano
        c = Principale_Opzioni_CancellaDaCsv.Leggi("10\n  -0.6 \n");
        assertEquals(List.of(A), Principale_Opzioni_CancellaDaCsv.Trova(c, m).ids());
    }

    @Test
    void VirgoletteAllaManieraCsv() throws Exception {
        TreeMap<String, String[]> m = base();
        m.get(A)[7] = "Metodo; \"bonifico\"";
        Principale_Opzioni_CancellaDaCsv.Criteri c = Principale_Opzioni_CancellaDaCsv.Leggi("7;3\n\"Metodo; \"\"bonifico\"\"\";Prova\n");
        assertEquals(List.of(A), Principale_Opzioni_CancellaDaCsv.Trova(c, m).ids());
    }

    @Test
    void RigaVuotaSiSaltaEnonCorrispondeATutto() throws Exception {
        Principale_Opzioni_CancellaDaCsv.Criteri c = Principale_Opzioni_CancellaDaCsv.Leggi("ID;3\n;\n" + A + ";\n");
        assertEquals(1, c.righeVuote());
        assertEquals(1, c.criteri().size());
        assertEquals(List.of(A), Principale_Opzioni_CancellaDaCsv.Trova(c, base()).ids());
    }

    @Test
    void ColonnaSconosciutaFermaLaLettura() {
        Principale_Opzioni_CancellaDaCsv.FileNonValido ex = assertThrows(Principale_Opzioni_CancellaDaCsv.FileNonValido.class,
                () -> Principale_Opzioni_CancellaDaCsv.Leggi("ID;Exchange\n" + A + ";Prova\n"));
        assertTrue(ex.getMessage().contains("«Exchange»"), ex.getMessage());
        //fuori dai campi del movimento
        assertThrows(Principale_Opzioni_CancellaDaCsv.FileNonValido.class,
                () -> Principale_Opzioni_CancellaDaCsv.Leggi(Importazioni.ColonneTabella + "\nx\n"));
        //una colonna sola che non ha la forma di un ID non è un elenco senza intestazione
        assertThrows(Principale_Opzioni_CancellaDaCsv.FileNonValido.class,
                () -> Principale_Opzioni_CancellaDaCsv.Leggi("Binance\n"));
    }

    @Test
    void ColonnaRipetutaRigaTroppoLungaVirgolettaAperta() {
        assertThrows(Principale_Opzioni_CancellaDaCsv.FileNonValido.class,
                () -> Principale_Opzioni_CancellaDaCsv.Leggi("3;[3]\nProva;Altro\n"));
        Principale_Opzioni_CancellaDaCsv.FileNonValido ex = assertThrows(Principale_Opzioni_CancellaDaCsv.FileNonValido.class,
                () -> Principale_Opzioni_CancellaDaCsv.Leggi("ID\n" + A + "\n" + B + ";Prova\n"));
        assertTrue(ex.getMessage().contains("riga 3"), ex.getMessage());
        assertThrows(Principale_Opzioni_CancellaDaCsv.FileNonValido.class,
                () -> Principale_Opzioni_CancellaDaCsv.Leggi("7\n\"aperta\n"));
        assertThrows(Principale_Opzioni_CancellaDaCsv.FileNonValido.class,
                () -> Principale_Opzioni_CancellaDaCsv.Leggi("\n  \n"));
    }

    @Test
    void IntestazioniAmmesse() {
        assertEquals(0, Principale_Opzioni_CancellaDaCsv.Colonna(" id "));
        assertEquals(5, Principale_Opzioni_CancellaDaCsv.Colonna("5"));
        assertEquals(5, Principale_Opzioni_CancellaDaCsv.Colonna("[ 5 ]"));
        assertEquals(18, Principale_Opzioni_CancellaDaCsv.Colonna("Campo 18"));
        assertEquals(-1, Principale_Opzioni_CancellaDaCsv.Colonna("-1"));
        assertEquals(-1, Principale_Opzioni_CancellaDaCsv.Colonna("Wallet"));
        assertEquals(-1, Principale_Opzioni_CancellaDaCsv.Colonna(""));
    }

    @Test
    void GruppoCollegatoIncompletoEGenerati() throws Exception {
        TreeMap<String, String[]> m = base();
        m.get(B)[22] = "AU";
        //solo il prelievo del trasferimento: il deposito resta, il gruppo è incompleto
        Principale_Opzioni_CancellaDaCsv.Esito e = Principale_Opzioni_CancellaDaCsv.Trova(
                Principale_Opzioni_CancellaDaCsv.Leggi(C + "\n" + B + "\n"), m);
        assertEquals(1, e.conGruppoIncompleto());
        assertEquals(1, e.generati());
        //tutti e due: il gruppo è completo
        e = Principale_Opzioni_CancellaDaCsv.Trova(Principale_Opzioni_CancellaDaCsv.Leggi(C + "\n" + D + "\n"), m);
        assertEquals(0, e.conGruppoIncompleto());
        String testo = GUI_CancellaDaCsv.TestoIntestazione("prova.csv",
                Principale_Opzioni_CancellaDaCsv.Leggi(C + "\n"), Principale_Opzioni_CancellaDaCsv.Trova(
                        Principale_Opzioni_CancellaDaCsv.Leggi(C + "\n"), m));
        assertTrue(testo.contains("tornano da classificare"), testo);
    }

    @Test
    void ModelloDellAnteprima() throws Exception {
        TreeMap<String, String[]> m = base();
        m.get(A)[13] = "1.5";
        m.get(A)[11] = "BTC";
        Principale_Opzioni_CancellaDaCsv.Esito e = Principale_Opzioni_CancellaDaCsv.Trova(Principale_Opzioni_CancellaDaCsv.Leggi(A + "\n"), m);
        DefaultTableModel t = GUI_CancellaDaCsv.CreaModello(e, m);
        assertEquals(1, t.getRowCount());
        assertEquals(1, t.getValueAt(0, 0));
        assertEquals("1.5 BTC", t.getValueAt(0, 6));
        assertEquals("", t.getValueAt(0, 5));
        assertEquals(A, t.getValueAt(0, 7));
    }
}
