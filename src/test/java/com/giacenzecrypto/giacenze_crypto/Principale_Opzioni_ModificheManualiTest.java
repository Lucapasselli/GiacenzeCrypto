package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Costruzione e scrittura dell'esportazione CSV delle modifiche manuali, su movimenti inventati. */
class Principale_Opzioni_ModificheManualiTest {

    private static final String L = "lignaggio-1";

    private static String[] riga(String id, String data, String prezzo, String lignaggio) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        v[1] = data;
        v[3] = "ExchangeProva";
        v[15] = prezzo;
        v[42] = lignaggio;
        return v;
    }

    /** Una versione dello storico: {IdVecchio, DataModifica, RigaOriginale, Operazione, IdNuovo}. */
    private static String[] versione(String idVecchio, long data, String[] prima, String operazione, String idNuovo) {
        return new String[]{idVecchio, String.valueOf(data), Importazioni.SerializzaRiga(prima), operazione, idNuovo};
    }

    /** {@code List.of(String[])} verrebbe letto come varargs: qui la lista ha per elemento l'array. */
    private static List<String[]> catena(String[]... versioni) {
        return new ArrayList<>(Arrays.asList(versioni));
    }

    private static Map<String, String[]> mappa(String[]... righe) {
        Map<String, String[]> m = new LinkedHashMap<>();
        for (String[] r : righe) {
            m.put(r[0], r);
        }
        return m;
    }

    private static List<String[]> dellTipo(Esportazione e, String tipo) {
        return e.record().stream().filter(r -> r[C_TIPO].equals(tipo)).toList();
    }

    @Test
    void modificaInPlaceMostraSoloIlCampoCambiato() {
        String[] prima = riga("A_1", "2025/01/01 10:00", "10.00", "");
        String[] dopo = riga("A_1", "2025/01/01 10:00", "12.50", L);
        dopo[19] = "999.00"; //campo ricalcolato dal motore: non e' una modifica dell'utente
        Map<String, List<String[]>> storico = Map.of(L,
                catena(versione("A_1", 1000, prima, MovimentiStorico.OP_IN_PLACE, "A_1")));

        Esportazione e = Costruisci(mappa(dopo), storico);

        assertEquals(1, e.movimentiConStorico());
        String[] fin = dellTipo(e, TIPO_FINALE).get(0);
        assertEquals("[15] Valore in euro", fin[C_CAMPI]);
        assertTrue(fin[C_DIFFERENZE].contains("10.00 -> 12.50"));
        assertEquals("A_1", fin[C_ID_PRIMA]);
        assertEquals("A_1", fin[C_ID_DOPO]);
        assertEquals("10.00", dellTipo(e, TIPO_INIZIALE).get(0)[COLONNE_FISSE + 15]);
        assertEquals("12.50", fin[COLONNE_FISSE + 15]);
        assertEquals(1, dellTipo(e, TIPO_PASSAGGIO).size());
        assertEquals("1000", dellTipo(e, TIPO_PASSAGGIO).get(0)[C_MILLIS]);
    }

    @Test
    void catenaConCambioDiIdHaUnaSolaRigaIniziale() {
        String[] a = riga("A_1", "2025/01/01 10:00", "10.00", "");
        String[] b = riga("B_1", "2025/01/01 11:00", "10.00", L);
        String[] c = riga("C_1", "2025/01/01 12:00", "10.00", L);
        Map<String, List<String[]>> storico = Map.of(L, catena(
                versione("A_1", 1000, a, "TraslaOrario", "B_1"),
                versione("B_1", 2000, b, "TraslaOrario", "C_1")));

        Esportazione e = Costruisci(mappa(c), storico);

        assertEquals(1, dellTipo(e, TIPO_INIZIALE).size());
        assertEquals("A_1", dellTipo(e, TIPO_INIZIALE).get(0)[C_ID_PRIMA], "l'iniziale e' A, non B");
        assertEquals("C_1", dellTipo(e, TIPO_FINALE).get(0)[C_ID_DOPO]);
        assertEquals(2, dellTipo(e, TIPO_PASSAGGIO).size());
        assertTrue(dellTipo(e, TIPO_FINALE).get(0)[C_CAMPI].contains("[1] Data e ora"));
    }

    @Test
    void unioneHaPiuRigheInizialiEUnaFinale() {
        String[] m1 = riga("M_1", "2025/01/01 10:00", "5.00", "");
        String[] m2 = riga("M_2", "2025/01/01 10:00", "6.00", "");
        String[] unito = riga("N_1", "2025/01/01 10:00", "11.00", L);
        //stesso istante: nessuna delle due righe e' figlia dell'altra
        Map<String, List<String[]>> storico = Map.of(L, catena(
                versione("M_1", 1000, m1, "UnisciMovimenti", "N_1"),
                versione("M_2", 1000, m2, "UnisciMovimenti", "N_1")));

        Esportazione e = Costruisci(mappa(unito), storico);

        assertEquals(2, dellTipo(e, TIPO_INIZIALE).size());
        assertEquals(1, dellTipo(e, TIPO_FINALE).size());
        assertEquals("M_1 | M_2", dellTipo(e, TIPO_FINALE).get(0)[C_ID_PRIMA]);
        assertTrue(dellTipo(e, TIPO_FINALE).get(0)[C_DIFFERENZE].startsWith("Più movimenti di partenza"));
    }

    @Test
    void separazioneHaPiuRigheFinali() {
        String[] prima = riga("S_1", "2025/01/01 10:00", "10.00", "");
        String[] g1 = riga("S_1a", "2025/01/01 10:00", "10.00", L);
        String[] g2 = riga("S_1b", "2025/01/01 10:00", "10.00", L);
        Map<String, List<String[]>> storico = Map.of(L,
                catena(versione("S_1", 1000, prima, "ModificaMovimento", "S_1a")));

        Esportazione e = Costruisci(mappa(g1, g2), storico);

        assertEquals(2, dellTipo(e, TIPO_FINALE).size());
        assertEquals(1, e.movimentiConStorico(), "una catena, due movimenti finali");
    }

    @Test
    void movimentoCancellatoHaSoloLInizialeConLaNota() {
        String[] prima = riga("A_1", "2025/01/01 10:00", "10.00", "");
        Map<String, List<String[]>> storico = Map.of(L,
                catena(versione("A_1", 1000, prima, MovimentiStorico.OP_IN_PLACE, "A_1")));

        Esportazione e = Costruisci(mappa(), storico);

        assertTrue(dellTipo(e, TIPO_FINALE).isEmpty());
        assertEquals(STATO_NON_PRESENTE, dellTipo(e, TIPO_INIZIALE).get(0)[C_DIFFERENZE]);
    }

    @Test
    void movimentoSenzaVersioniNonEntra() {
        String[] mai = riga("A_1", "2025/01/01 10:00", "10.00", "");
        String[] lignaggioSenzaVersioni = riga("B_1", "2025/01/01 10:00", "10.00", "annullata");

        Esportazione e = Costruisci(mappa(mai, lignaggioSenzaVersioni), Map.of());

        assertTrue(e.record().isEmpty());
    }

    @Test
    void senzaStoricoElencaInseritiAManoEMarcatiM() {
        String[] inserito = riga("MAN_1", "2025/01/01 10:00", "1.00", "");
        inserito[39] = "E";
        String[] marcato = riga("MAR_1", "2025/01/01 11:00", "1.00", "");
        marcato[22] = "M";
        String[] normale = riga("EXT_1", "2025/01/03 10:00", "1.00", "");

        Esportazione e = Costruisci(mappa(inserito, marcato, normale), Map.of());

        List<String[]> r = dellTipo(e, TIPO_SENZA_STORICO);
        assertEquals(2, r.size());
        assertEquals(CATEGORIA_INSERITO, r.get(0)[C_OPERAZIONE]);
        assertEquals(CATEGORIA_MARCATO_M, r.get(1)[C_OPERAZIONE]);
    }

    @Test
    void classificazioneRaggruppaOriginaliEGenerati() {
        String[] originale = riga("ORI_1", "2025/01/02 10:00", "1.00", "");
        String[] generato = riga("GEN_1", "2025/01/02 10:00", "1.00", "");
        generato[22] = "AU";
        generato[20] = "ORI_1,GEN_1";
        originale[20] = "ORI_1,GEN_1";
        String[] altroOriginale = riga("ORI_2", "2025/01/05 10:00", "1.00", "");
        String[] altroGenerato = riga("GEN_2", "2025/01/05 10:00", "1.00", "");
        altroGenerato[22] = "AU";
        altroGenerato[20] = "ORI_2,GEN_2";
        altroOriginale[20] = "ORI_2,GEN_2";
        String[] estraneo = riga("EXT_1", "2025/01/03 10:00", "1.00", "");

        Esportazione e = Costruisci(mappa(originale, generato, altroOriginale, altroGenerato, estraneo), Map.of());

        List<String[]> r = dellTipo(e, TIPO_CLASSIFICAZIONE);
        assertEquals(4, r.size());
        assertEquals(4, e.classificati());
        assertEquals("GEN_1", r.get(0)[C_CHIAVE], "il piu' piccolo ID del gruppo");
        assertEquals("GEN_1", r.get(1)[C_CHIAVE]);
        assertEquals(CATEGORIA_GENERATO, r.get(0)[C_OPERAZIONE]);
        assertEquals(CATEGORIA_ORIGINALE, r.get(1)[C_OPERAZIONE]);
        assertEquals("GEN_2", r.get(2)[C_CHIAVE]);
        assertFalse(r.stream().anyMatch(x -> x[C_ID_DOPO].equals("EXT_1")));
    }

    @Test
    void unOriginaleClassificatoMarcatoMNonSiEsportaDueVolte() {
        String[] originale = riga("ORI_1", "2025/01/02 10:00", "1.00", "");
        originale[22] = "M";
        originale[20] = "ORI_1,GEN_1";
        String[] generato = riga("GEN_1", "2025/01/02 10:00", "1.00", "");
        generato[22] = "AU";
        generato[20] = "ORI_1,GEN_1";

        Esportazione e = Costruisci(mappa(originale, generato), Map.of());

        assertEquals(2, dellTipo(e, TIPO_CLASSIFICAZIONE).size());
        assertTrue(dellTipo(e, TIPO_SENZA_STORICO).isEmpty());
        assertEquals(0, e.senzaStorico());
    }

    @Test
    void ilFileHaFormatoIntestazioneERigheACampiFissi(@TempDir Path cartella) throws Exception {
        String[] prima = riga("A_1", "2025/01/01 10:00", "2.5E-9", "");
        prima[21] = "nota; con punto e virgola\ne a capo";
        String[] dopo = riga("A_1", "2025/01/01 10:00", "3.5E-9", L);
        Map<String, List<String[]>> storico = Map.of(L,
                catena(versione("A_1", 1000, prima, MovimentiStorico.OP_IN_PLACE, "A_1")));
        Esportazione e = Costruisci(mappa(dopo), storico);
        File csv = cartella.resolve("prova.csv").toFile();

        ScriviCsv(e, csv);

        String testo = Files.readString(csv.toPath(), StandardCharsets.UTF_8);
        assertTrue(testo.startsWith("﻿#GiacenzeCrypto ModificheManuali;formato=" + FORMATO + ";campi=46;"));
        String[] righe = testo.split("\r\n");
        assertEquals(2 + e.record().size(), righe.length);
        int colonne = COLONNE_FISSE + Importazioni.ColonneTabella;
        for (int i = 1; i < righe.length; i++) {
            assertEquals(colonne, righe[i].split(";", -1).length, "riga " + i + " ha un numero fisso di colonne");
        }
        assertTrue(righe[1].startsWith("Tipo;Chiave;"));
        assertTrue(testo.contains("2.5E-9"), "la notazione scientifica resta com'e'");
        assertTrue(testo.contains("nota con punto e virgola<br>e a capo"), "; tolto, a capo come <br> nelle note");
    }
}
