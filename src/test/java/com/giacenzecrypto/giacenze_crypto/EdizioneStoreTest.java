package com.giacenzecrypto.giacenze_crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Fissa il comportamento dell'edizione destinata al Microsoft Store, cioè della build ottenuta con
 * {@code -Dgiacenze.edizione=store}.
 *
 * <p>Quella edizione rinuncia allo scaricamento dei movimenti dai conti exchange e ai loghi di terzi
 * (vedi {@link VarStatiche#EdizioneStore()} e {@code test/Documentazione/Pubblicazione_MicrosoftStore.md}).
 * Buona parte degli interruttori sta dentro Swing o dentro la creazione del database e non è verificabile
 * qui; quello che si può fissare — e che conta, perché è testo che il revisore legge — è che l'edizione
 * Store <b>non annunci</b> chiavi API di exchange che non possiede.
 *
 * <p>L'edizione si legge da {@code version.properties} in un campo non finale apposta: la build di test è
 * sempre {@code completa}, e senza poterlo sostituire il ramo Store non sarebbe raggiungibile da nessun
 * test.
 */
public class EdizioneStoreTest {

    private final String EdizioneOriginale = VarStatiche.Edizione;

    @AfterEach
    public void RipristinaEdizione() {
        VarStatiche.Edizione = EdizioneOriginale;
    }

    @Test
    public void laBuildOrdinariaEQuellaCompleta() {
        //Se questo fallisce, il resto della suite sta girando su un'edizione ridotta senza saperlo
        assertEquals("completa", VarStatiche.Edizione);
        assertFalse(VarStatiche.EdizioneStore());
    }

    @Test
    public void ilRiconoscimentoDellEdizioneNonDipendeDalleMaiuscole() {
        VarStatiche.Edizione = "STORE";
        assertTrue(VarStatiche.EdizioneStore());
        VarStatiche.Edizione = "Store";
        assertTrue(VarStatiche.EdizioneStore());
    }

    @Test
    public void unValoreSconosciutoVienePresoComeEdizioneCompleta() {
        //Nel dubbio si assume il programma intero: l'edizione ridotta va scelta apposta, e una build
        //mal configurata che si spegnesse da sola sarebbe molto più difficile da riconoscere
        VarStatiche.Edizione = "";
        assertFalse(VarStatiche.EdizioneStore());
        VarStatiche.Edizione = "qualcosaltro";
        assertFalse(VarStatiche.EdizioneStore());
    }

    @Test
    public void nellEdizioneStoreIlGruppoDiBackupNonAnnunciaLeChiaviDegliExchange() {
        VarStatiche.Edizione = VarStatiche.EDIZIONE_STORE;
        String etichetta = Backup_Restore.Gruppo.CHIAVI_API.Etichetta();
        assertFalse(etichetta.toLowerCase().contains("exchange"),
                "L'edizione Store non conserva chiavi di exchange : l'etichetta non deve nominarle, "
                + "ma restano quelle degli explorer e dei fornitori di dati. Trovato : " + etichetta);
        assertTrue(etichetta.toLowerCase().contains("explorer"));
    }

    @Test
    public void nellEdizioneCompletaIlGruppoDiBackupNominaGliExchange() {
        assertTrue(Backup_Restore.Gruppo.CHIAVI_API.Etichetta().toLowerCase().contains("exchange"));
    }

    @Test
    public void gliAltriGruppiDiBackupNonCambianoConLEdizione() {
        //L'eccezione è una sola : se domani ne servisse un'altra deve essere una scelta, non un effetto
        for (Backup_Restore.Gruppo g : Backup_Restore.Gruppo.values()) {
            if (g == Backup_Restore.Gruppo.CHIAVI_API) {
                continue;
            }
            VarStatiche.Edizione = "completa";
            String completa = g.Etichetta();
            VarStatiche.Edizione = VarStatiche.EDIZIONE_STORE;
            assertEquals(completa, g.Etichetta(), "Il gruppo " + g + " non deve dipendere dall'edizione");
        }
    }
}
