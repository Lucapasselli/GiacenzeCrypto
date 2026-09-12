package com.giacenzecrypto.giacenze_crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Note di compilazione dei quadri, spostate da {@code Principale}/{@code Stampe} a
 * {@code config/importmappe/NoteCompilazione.json} per poterle correggere senza una nuova versione.
 * <p>
 * Il guadagno ha un costo: prima erano stringhe Java e il compilatore garantiva almeno che ci fossero.
 * Ora una chiave sbagliata o un segnaposto scritto male si vedrebbero soltanto stampando il report, a
 * dichiarazione in mano. Questi test rimettono quella garanzia dove si e' persa.
 * <p>
 * Nota: il guardiano dei glifi di {@link FontApplicazioneTest} non copriva queste stringhe nemmeno
 * prima — quel test salta di proposito i blocchi di testo, proprio perche' sono le note fiscali, che
 * finiscono nel PDF e non nell'interfaccia. Spostarle nel JSON non ha quindi tolto nulla.
 */
class NoteCompilazioneTest {

    private static final Path FILE = Path.of("config/importmappe/NoteCompilazione.json");

    /** Le chiavi dichiarate come costanti pubbliche in {@link NoteCompilazione}. */
    private static final String[] CHIAVI = {
        NoteCompilazione.W, NoteCompilazione.W8, NoteCompilazione.RW, NoteCompilazione.RW8,
        NoteCompilazione.FIAT, NoteCompilazione.FIAT_LOOKUP_FALLITI,
        NoteCompilazione.FIAT_CONTI_CORRENTI, NoteCompilazione.FIAT_IVAFE_LIQUIDITA,
        NoteCompilazione.T, NoteCompilazione.RT
    };

    private static Map<String, String> note() throws Exception {
        Map<String, String> m = NoteCompilazione.Interpreta(Files.readString(FILE), NoteCompilazione.NOME);
        assertNotNull(m, "NoteCompilazione.json non interpretabile");
        return m;
    }

    @Test
    void ogniChiaveUsataDalCodiceEsisteNelFile() throws Exception {
        Map<String, String> note = note();
        for (String chiave : CHIAVI) {
            assertTrue(note.containsKey(chiave) && !note.get(chiave).isBlank(),
                    "manca la nota '" + chiave + "' in " + FILE);
        }
    }

    /**
     * Vincola <b>il file committato in questo repository</b>, non quello che un'installazione riceve:
     * il JSON si aggiorna da GitHub indipendentemente dalla versione del programma, quindi una copia
     * piu' recente che porta gia' la nota di una release futura e' legittima e a runtime non da'
     * nessun fastidio ({@link NoteCompilazione#Testo} legge per chiave, le altre le ignora). Qui serve
     * solo a intercettare una chiave scritta male in una modifica a mano di questo file.
     * <p>Il suffisso dell'anno va tolto prima del confronto: {@code W8.2024} e' la stessa nota di
     * {@code W8}, e le varianti datate sono proprio cio' che si aggiunge senza toccare il codice.
     */
    @Test
    void ilFileNonHaNoteDiPiuDiQuanteNeUsiIlCodice() throws Exception {
        List<String> dichiarate = List.of(CHIAVI);
        List<String> orfane = new ArrayList<>();
        for (String k : note().keySet()) {
            if (!dichiarate.contains(k.replaceAll("\\.\\d{4}$", ""))) orfane.add(k);
        }
        //Una nota che nessuno stampa non e' un errore fatale, ma e' quasi sempre una chiave scritta
        //male: senza questo controllo resterebbe nel file a non fare niente.
        assertTrue(orfane.isEmpty(), "note nel file che il codice non usa (chiave sbagliata?): " + orfane);
    }

    @Test
    void unaVarianteDatataValeDalSuoAnnoInAvantiEQuellaNudaPerGliAnniPrima() throws Exception {
        java.util.Set<String> chiavi = note().keySet();
        assertTrue(chiavi.contains("T") && chiavi.contains("T.2024") && chiavi.contains("T.2025"),
                "il file deve avere la chiave nuda e le varianti 2024 e 2025 del quadro T");
        //La chiave nuda copre il 2023, l'anno in cui il 730 non aveva ancora il quadro T.
        assertEquals("T", NoteCompilazione.ChiaveApplicabile(chiavi, "T", "2023"));
        assertEquals("T.2024", NoteCompilazione.ChiaveApplicabile(chiavi, "T", "2024"));
        assertEquals("T.2025", NoteCompilazione.ChiaveApplicabile(chiavi, "T", "2025"));
        //Il 2026 non ha una variante propria : deve prendere l'ultima disponibile, non ricadere sulla
        //chiave nuda, che e' il testo *vecchio*. E' il caso che il vecchio "if (Anno<2025) ... else"
        //copriva con l'else, e perderlo vorrebbe dire stampare le istruzioni di due anni prima.
        assertEquals("T.2025", NoteCompilazione.ChiaveApplicabile(chiavi, "T", "2026"));
        //Sotto il 2023 non si cercano varianti : il programma avverte gia' che il report puo' sbagliare.
        assertEquals("T", NoteCompilazione.ChiaveApplicabile(chiavi, "T", "2022"));
        //Una nota senza nessuna variante resta quella che e'.
        assertEquals("RW8", NoteCompilazione.ChiaveApplicabile(chiavi, "RW8", "2025"));
        //Il rigo W8 del 730 e' cambiato due volte: nel 2023 aveva la sola colonna 7, dal 2024 le
        //colonne 2/3/4, dal 2025 col rinvio al rigo 301 del 730-3.
        assertEquals("W8", NoteCompilazione.ChiaveApplicabile(chiavi, "W8", "2023"));
        assertEquals("W8.2024", NoteCompilazione.ChiaveApplicabile(chiavi, "W8", "2024"));
        assertEquals("W8.2025", NoteCompilazione.ChiaveApplicabile(chiavi, "W8", "2026"));
    }

    @Test
    void unAnnoNonNumericoPrendeLaChiaveNudaInveceDiUnaVarianteACaso() throws Exception {
        assertEquals("T", NoteCompilazione.ChiaveApplicabile(note().keySet(), "T", "boh"));
    }

    @Test
    void ogniNotaApreIlMarkupHtml() throws Exception {
        //Solo l'apertura: T_2025 e RT_2025 non chiudono &lt;/font&gt;&lt;/html&gt; e non lo facevano
        //nemmeno quando erano blocchi di testo Java. HTMLWorker li accetta lo stesso, e questa
        //migrazione vale proprio perche' non cambia una virgola del testo stampato. Ora pero' sono
        //due tag in un file di configurazione: si possono chiudere quando si vuole, senza una release.
        note().forEach((chiave, testo) -> assertTrue(testo.contains("<html>"), chiave + " : manca <html>"));
    }

    @Test
    void iSegnapostoDegliAnniVengonoRisolti() {
        String t = NoteCompilazione.Testo(NoteCompilazione.RW8, "2025");
        assertFalse(t.contains("{anno}"), "{anno} non risolto");
        assertFalse(t.contains("{annoPrec}"), "{annoPrec} non risolto");
        assertTrue(t.contains("2025"), "l'anno d'imposta non compare nella nota");
        assertTrue(t.contains("2024"), "l'anno precedente non compare nella nota");
    }

    @Test
    void unAnnoNonNumericoLasciaISegnapostoVisibiliInveceDiInventareUnaData() {
        //Meglio un segnaposto a video che un anno sbagliato su una dichiarazione.
        assertTrue(NoteCompilazione.SostituisciAnni("redditi {annoPrec}", "boh").contains("{annoPrec}"));
    }

    @Test
    void nessunSegnapostoRestaIrrisoltoNelleNoteSenzaValoriCalcolati() throws Exception {
        Map<String, String> note = note();
        Pattern p = Pattern.compile("\\{(\\w+)\\}");
        //Le varianti datate vanno controllate come le altre : sono testo che finisce in stampa.
        for (String chiave : note.keySet()) {
            //Le tre note FIAT condizionali ricevono un valore calcolato dal chiamante : quelle sono
            //verificate a parte, qui restano fuori.
            String nuda = chiave.replaceAll("\\.\\d{4}$", "");
            if (nuda.equals(NoteCompilazione.FIAT_LOOKUP_FALLITI)
                    || nuda.equals(NoteCompilazione.FIAT_CONTI_CORRENTI)
                    || nuda.equals(NoteCompilazione.FIAT_IVAFE_LIQUIDITA)) {
                continue;
            }
            String t = NoteCompilazione.SostituisciAnni(note.get(chiave), "2025");
            Matcher m = p.matcher(t);
            assertFalse(m.find(), chiave + " : segnaposto sconosciuto '" + (m.hitEnd() ? "" : m.group()) + "'");
        }
    }

    @Test
    void leNoteConValoriCalcolatiLiRicevonoDalChiamante() throws Exception {
        String t = NoteCompilazione.Testo(NoteCompilazione.FIAT_CONTI_CORRENTI, "2025",
                Map.of("totale", "1.234"));
        assertTrue(t.contains("1.234"), "il totale IVAFE non e' stato sostituito");
        assertFalse(t.contains("{totale}"));

        String r = NoteCompilazione.Testo(NoteCompilazione.FIAT_LOOKUP_FALLITI, "2025",
                Map.of("righi", "Wallet 01, Wallet 02"));
        assertTrue(r.contains("Wallet 01, Wallet 02"));
        assertFalse(r.contains("{righi}"));
    }

    @Test
    void unaChiaveInesistenteProduceUnAvvisoVisibileNonUnaNotaVuota() {
        //Una nota che sparisce in silenzio da un documento fiscale e' peggio di una che dichiara di
        //mancare : il testo di ripiego deve finire nel report e farsi leggere.
        String t = NoteCompilazione.Testo("CHIAVE_CHE_NON_ESISTE", "2025");
        assertFalse(t.isBlank());
        assertTrue(t.contains("ATTENZIONE"), "il ripiego non avvisa l'utente");
        assertTrue(t.contains("CHIAVE_CHE_NON_ESISTE"), "il ripiego non dice quale nota manca");
    }

    @Test
    void ilFileDichiaraSegnapostoEAvvertenzaPerChiLoModifica() throws Exception {
        JSONObject j = new JSONObject(Files.readString(FILE));
        assertTrue(j.has("descrizione") && j.has("segnaposto") && j.has("avvertenza"),
                "il file va corretto a mano da chi non ha il codice sotto mano: "
                + "descrizione, segnaposto e avvertenza devono restare");
    }

    @Test
    void laCopiaDiDefaultEDistribuitaNelJar() {
        //config/importmappe finisce in /ImportMappe/ del jar tramite il <resource> del pom : se il
        //file non c'e', InstallaDefaultSeMancanti non ha nulla da installare al primo avvio.
        assertNotNull(NoteCompilazione.class.getResourceAsStream("/ImportMappe/"
                + NoteCompilazione.NOME + ".json"),
                "NoteCompilazione.json non e' fra le risorse del programma");
    }
}
