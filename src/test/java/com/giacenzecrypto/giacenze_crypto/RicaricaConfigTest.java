package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Il registro dei file di {@code config/} in memoria ({@link RicaricaConfig}) e la scansione delle cartelle. */
class RicaricaConfigTest {

    private static List<String> fileDi(String cartella) throws IOException {
        try (Stream<Path> s = Files.list(Path.of("config", cartella))) {
            return s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json")).sorted().toList();
        }
    }

    private static String senzaEstensione(String n) {
        return n.substring(0, n.length() - ".json".length());
    }

    @Test
    void ognifileDiVarieEImportmappeStaInUnSoloElenco() throws Exception {
        Set<String> inMemoria = new HashSet<>();
        for (RicaricaConfig.Voce v : RicaricaConfig.VOCI) {
            assertTrue(inMemoria.add(v.cartella() + "/" + v.file()), "voce doppia: " + v.cartella() + "/" + v.file());
            assertFalse(RicaricaConfig.LETTI_A_OGNI_USO.contains(v.cartella() + "/" + v.file()),
                    "in memoria e letto a ogni uso insieme: " + v.file());
        }
        for (String cartella : new String[]{"varie", "importmappe"}) {
            for (String n : fileDi(cartella)) {
                String chiave = cartella + "/" + senzaEstensione(n);
                assertTrue(inMemoria.contains(chiave) ^ RicaricaConfig.LETTI_A_OGNI_USO.contains(chiave),
                        chiave + " non e' registrato in RicaricaConfig: va in VOCI (se resta in memoria) o in LETTI_A_OGNI_USO");
            }
        }
    }

    @Test
    void nessunaVoceDelRegistroPuntaAUnFileCheNonEsiste() throws Exception {
        for (RicaricaConfig.Voce v : RicaricaConfig.VOCI) {
            assertTrue(fileDi(v.cartella()).contains(v.file() + ".json"), v.cartella() + "/" + v.file());
        }
        for (String chiave : RicaricaConfig.LETTI_A_OGNI_USO) {
            String[] p = chiave.split("/");
            assertTrue(fileDi(p[0]).contains(p[1] + ".json"), chiave);
        }
    }

    @Test
    void ogniFileDiVarieEImportmappeEUnJsonValidoEViaggiaConIlProgramma() throws Exception {
        List<String> varie = Arrays.asList(MappeCausali.FILE_VARIE);
        List<String> sistema = Arrays.asList(MappeCausali.FILE_DI_SISTEMA);
        for (String n : fileDi("varie")) {
            assertDoesNotThrow(() -> new JSONObject(Files.readString(Path.of("config/varie", n))), n);
            assertTrue(varie.contains(senzaEstensione(n)), n + " non e' in MappeCausali.FILE_VARIE: non verrebbe installato");
        }
        for (String n : fileDi("importmappe")) {
            assertDoesNotThrow(() -> new JSONObject(Files.readString(Path.of("config/importmappe", n))), n);
            assertTrue(sistema.contains(senzaEstensione(n)), n + " non e' in MappeCausali.FILE_DI_SISTEMA: non verrebbe installato");
        }
    }

    @Test
    void Ricarica_eseguiSoloLeVociDeiFileAggiornati() {
        List<String> eseguite = new ArrayList<>();
        List<RicaricaConfig.Voce> voci = List.of(
                new RicaricaConfig.Voce("varie", "A", () -> eseguite.add("A"), "a"),
                new RicaricaConfig.Voce("varie", "B", () -> eseguite.add("B"), "b"),
                new RicaricaConfig.Voce("importmappe", "A", () -> eseguite.add("mA"), "ma"));
        List<String> fatte = RicaricaConfig.Ricarica(Map.of("varie", List.of("A.json", "Z.json"), "import", List.of("B.json")), voci);
        assertEquals(List.of("A"), eseguite, "B e' in un'altra cartella, mA non e' stato aggiornato");
        assertEquals(List.of("a"), fatte);
        assertTrue(RicaricaConfig.Ricarica(null, voci).isEmpty());
        assertTrue(RicaricaConfig.Ricarica(Map.of(), voci).isEmpty());
    }

    @Test
    void Ricarica_unErroreSuUnaVoceNonFermaLeAltre() {
        List<String> eseguite = new ArrayList<>();
        List<RicaricaConfig.Voce> voci = List.of(
                new RicaricaConfig.Voce("varie", "A", () -> { throw new IllegalStateException("prova"); }, "a"),
                new RicaricaConfig.Voce("varie", "B", () -> eseguite.add("B"), "b"));
        List<String> fatte = RicaricaConfig.Ricarica(Map.of("varie", List.of("A.json", "B.json")), voci);
        assertEquals(List.of("B"), eseguite);
        assertEquals(List.of("b"), fatte);
    }
}
