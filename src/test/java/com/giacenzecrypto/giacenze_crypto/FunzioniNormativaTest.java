package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link Funzioni#cancellaOrfaniNormativa(String, Set)}, la parte di
 * {@link Funzioni#AggiornamentoNormativaDaRepository()} che non richiede rete: cancella dalla cartella
 * locale i file assenti dall'albero remoto, ricorsivamente (a differenza dell'analoga
 * {@code cancellaConfigOrfani} di {@code config/}, che non ricorre nelle sottocartelle e in più
 * richiede il flag {@code "centralizzato"} - qui nessuno dei due vincoli si applica: l'intera
 * cartella {@code Normativa/} è uno specchio 1:1 del repository).
 */
class FunzioniNormativaTest {

    @TempDir
    Path tempDir;

    @Test
    void cancellaIFileAssentiDallElencoRemoto() throws Exception {
        Path daCancellare = tempDir.resolve("Prassi_AgenziaEntrate/vecchio.pdf");
        Files.createDirectories(daCancellare.getParent());
        Files.writeString(daCancellare, "contenuto");

        List<String> cancellati = Funzioni.cancellaOrfaniNormativa(tempDir.toString(),
                Set.of("Prassi_AgenziaEntrate/nuovo.pdf"));

        assertEquals(List.of("Prassi_AgenziaEntrate/vecchio.pdf"), cancellati);
        assertFalse(Files.exists(daCancellare));
    }

    @Test
    void tieneIFilePresentiNellElencoRemoto() throws Exception {
        Path daTenere = tempDir.resolve("Leggi/Originale/LeggiBilancio/legge.akn.xml");
        Files.createDirectories(daTenere.getParent());
        Files.writeString(daTenere, "<xml/>");

        List<String> cancellati = Funzioni.cancellaOrfaniNormativa(tempDir.toString(),
                Set.of("Leggi/Originale/LeggiBilancio/legge.akn.xml"));

        assertTrue(cancellati.isEmpty());
        assertTrue(Files.exists(daTenere));
    }

    @Test
    void ricorreNelleSottocartelleAnnidateSuPiuLivelli() throws Exception {
        //A differenza di config/ (un solo livello sotto la sua radice), Normativa/ e' annidata su piu'
        //livelli (es. Leggi/Originale/LeggiBilancio/...): la cancellazione deve raggiungerli tutti.
        Path annidato = tempDir.resolve("Leggi/Originale/NormeRichiamate/vecchia_norma.akn.xml");
        Files.createDirectories(annidato.getParent());
        Files.writeString(annidato, "<xml/>");

        List<String> cancellati = Funzioni.cancellaOrfaniNormativa(tempDir.toString(), Set.of());

        assertEquals(List.of("Leggi/Originale/NormeRichiamate/vecchia_norma.akn.xml"), cancellati);
    }

    @Test
    void nonEsplodeSuCartellaLocaleAncoraInesistente() {
        List<String> cancellati = Funzioni.cancellaOrfaniNormativa(
                tempDir.resolve("non-esiste-ancora").toString(), Set.of("qualcosa.pdf"));

        assertTrue(cancellati.isEmpty());
    }
}
