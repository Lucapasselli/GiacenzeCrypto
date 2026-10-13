package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code versioneMinimaApp} nei file di {@code config/varie} e {@code config/importmappe}: un file su disco che chiede
 * una versione più recente non si legge, vale la copia nel jar ({@link MappeCausali#CaricaConRipiego}).
 */
class VersioneMinimaConfigTest {

    @TempDir
    Path tempDir;

    private String workdirPrima;

    @BeforeEach
    void usaUnaCartellaDiLavoroTemporanea() {
        Assumptions.assumeTrue(!VarStatiche.Versione.isBlank() && Character.isDigit(VarStatiche.Versione.charAt(0)),
                "versione del programma non risolta (resource filtering): il controllo non si applica");
        workdirPrima = VarStatiche.getWorkingDirectory();
        VarStatiche.setWorkingDirectory(tempDir + "/");
    }

    @AfterEach
    void ripristina() {
        if (workdirPrima != null) VarStatiche.setWorkingDirectory(workdirPrima);
    }

    private void scriviSimboli(String versioneMinima) throws Exception {
        Path f = Path.of(VarStatiche.getCartella_ConfigVarie(), ListeSimboli.NOME + ".json");
        Files.createDirectories(f.getParent());
        String chiave = versioneMinima == null ? "" : "\"versioneMinimaApp\": \"" + versioneMinima + "\",";
        Files.writeString(f, "{" + chiave + "\"prioritari\": {\"simboli\": [\"ZZZ\"]}}");
    }

    @Test
    void unFileCheRichiedeUnaVersionePiuNuovaNonSiLegge_valeLaCopiaNelJar() throws Exception {
        scriviSimboli("99.0.0");
        String[] letti = ListeSimboli.CaricaPrioritari();
        assertFalse(Arrays.asList(letti).contains("ZZZ"), "il file su disco richiede la versione 99: non va letto");
        assertTrue(Arrays.asList(letti).contains("USDT"), "vale la copia nel jar");
    }

    @Test
    void unFileConVersioneMinimaRaggiuntaOSenzaChiaveSiLegge() throws Exception {
        scriviSimboli("0.0.1");
        assertArrayEquals(new String[]{"ZZZ"}, ListeSimboli.CaricaPrioritari());
        scriviSimboli(null);
        assertArrayEquals(new String[]{"ZZZ"}, ListeSimboli.CaricaPrioritari());
    }

    @Test
    void laChiaveSiRiconoscePerVersioneEForma() {
        assertTrue(MappeCausali.RichiedeVersionePiuNuova("{\"versioneMinimaApp\":\"99.0.0\"}"));
        assertFalse(MappeCausali.RichiedeVersionePiuNuova("{\"versioneMinimaApp\":\"0.0.1\"}"));
        assertFalse(MappeCausali.RichiedeVersionePiuNuova("{\"versioneMinimaApp\":\"\"}"));
        assertFalse(MappeCausali.RichiedeVersionePiuNuova("{\"altro\":1}"));
        assertFalse(MappeCausali.RichiedeVersionePiuNuova("non json versioneMinimaApp"));
        assertFalse(MappeCausali.RichiedeVersionePiuNuova(null));
        assertEquals("1.2.3", MappeCausali.VersioneMinimaDichiarata("{\"versioneMinimaApp\":\" 1.2.3 \"}"));
    }
}
