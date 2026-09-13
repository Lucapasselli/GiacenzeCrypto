package com.giacenzecrypto.giacenze_crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test della cascata "elimina il documento e con lui i suoi movimenti", cioè di
 * {@link Principale_DocumentiFonte#EliminaMovimentiDelDocumento(int)}.
 *
 * <p>Non tocca il registro dei documenti né i file : la parte verificata qui è solo quella che agisce su
 * {@link Principale#MappaCryptoWallet}, che è anche l'unica non reversibile senza ripescare un backup.
 * Le due conferme e l'ordine scrittura → {@code Annulla} stanno nel metodo chiamante, che apre dei dialoghi
 * e non è eseguibile senza schermo.
 *
 * <p>Quello che questi test difendono :
 * <ul>
 *   <li><b>si eliminano solo i movimenti di quel documento</b> : il campo {@code [41]} è l'unico criterio,
 *       e i movimenti degli altri documenti — o senza documento — restano dove sono;</li>
 *   <li><b>il conteggio è quello vero</b> : {@code RimuoviMovimentazioneXID} porta via anche i movimenti
 *       generati automaticamente del gruppo, che quindi non vanno contati una seconda volta quando il
 *       ciclo arriva alla loro chiave;</li>
 *   <li><b>la passata si ripete</b> : sugli scambi differiti il ripristino <i>rinomina</i> i movimenti
 *       rimasti, così un movimento del documento può ricomparire sotto una chiave nuova dopo che la sua è
 *       già stata visitata. Senza la ripetizione resterebbe in archivio a puntare a un documento che non
 *       esiste più — ed è esattamente l'orfano che questa funzione esiste per evitare.</li>
 * </ul>
 */
class Principale_DocumentiFonteEliminazioneTest {

    private static final String DATA = "01/03/2025 10:00:00";
    private static final String DATA_ID = "20250301100000";

    @BeforeEach
    void puliscePartenza() {
        MappaCryptoWallet.clear();
    }

    @Test
    void eliminaSoloIMovimentiDelDocumentoIndicato() {
        movimento(DATA_ID + "_001_001_001_DC", "7", "DEPOSITO CRYPTO");
        movimento(DATA_ID + "_002_001_001_DC", "7", "DEPOSITO CRYPTO");
        movimento(DATA_ID + "_003_001_001_DC", "8", "DEPOSITO CRYPTO");
        movimento(DATA_ID + "_004_001_001_DC", "", "DEPOSITO CRYPTO");

        assertEquals(2, Principale_DocumentiFonte.EliminaMovimentiDelDocumento(7));

        assertEquals(2, MappaCryptoWallet.size());
        assertTrue(MappaCryptoWallet.containsKey(DATA_ID + "_003_001_001_DC"),
                "il movimento di un altro documento non va toccato");
        assertTrue(MappaCryptoWallet.containsKey(DATA_ID + "_004_001_001_DC"),
                "il movimento senza documento di origine non va toccato");
    }

    @Test
    void nonContaDueVolteIMovimentiGeneratiAutomaticamenteDelloStessoGruppo() {
        //Un prelievo classificato con la sua commissione automatica : eliminando il prelievo,
        //RimuoviMovimentazioneXID si porta via anche la commissione
        String IDPrelievo = DATA_ID + "_001_001_001_PC";
        String IDCommissione = DATA_ID + "_001_001_002_CM";

        String prelievo[] = movimento(IDPrelievo, "7", "PRELIEVO CRYPTO");
        prelievo[10] = "-1.0";
        prelievo[15] = "100.00";
        prelievo[18] = "Prelievo con commissione";
        prelievo[20] = IDCommissione;

        String commissione[] = movimento(IDCommissione, "7", "COMMISSIONI");
        commissione[10] = "-0.1";
        commissione[15] = "10.00";
        commissione[22] = "AU";

        assertEquals(1, Principale_DocumentiFonte.EliminaMovimentiDelDocumento(7),
                "la commissione automatica sparisce insieme al prelievo, non è un secondo movimento eliminato");
        assertTrue(MappaCryptoWallet.isEmpty());
    }

    @Test
    void unMovimentoRinominatoDalRipristinoNonRestaOrfano() {
        //Scambio differito : i due movimenti hanno l'ID con il secondo campo prefissato ("00"/"04") e
        //campo18 "PTW/DTW - Scambio Differito". Il ripristino toglie quel prefisso e li rimette in mappa
        //con una chiave nuova — che può ordinarsi PRIMA di quella già visitata.
        String IDPrelievo = DATA_ID + "_00001_001_001_PC";
        String IDDeposito = DATA_ID + "_00002_001_001_DC";

        String prelievo[] = movimento(IDPrelievo, "7", "PRELIEVO CRYPTO");
        prelievo[10] = "-1.0";
        prelievo[15] = "100.00";
        prelievo[18] = "PTW - Scambio Differito";
        prelievo[20] = IDDeposito;

        String deposito[] = movimento(IDDeposito, "7", "DEPOSITO CRYPTO");
        deposito[13] = "10";
        deposito[15] = "100.00";
        deposito[18] = "DTW - Scambio Differito";
        deposito[20] = IDPrelievo;

        int eliminati = Principale_DocumentiFonte.EliminaMovimentiDelDocumento(7);

        assertTrue(MappaCryptoWallet.isEmpty(),
                "nessun movimento del documento deve sopravvivere, nemmeno sotto la chiave rinominata dal "
                + "ripristino : resterebbe a puntare a un documento cancellato. Rimasti : "
                + MappaCryptoWallet.keySet());
        assertTrue(eliminati >= 1, "il conteggio deve riportare i movimenti eliminati davvero");
    }

    /** Crea un movimento minimo con il documento di origine indicato e lo mette in mappa. */
    private static String[] movimento(String ID, String Documento, String Campo5) {
        String v[] = new String[Importazioni.ColonneTabella];
        v[0] = ID;
        v[1] = DATA;
        v[3] = "Wallet Test";
        v[4] = "Principale";
        v[5] = Campo5;
        v[8] = "BTC";
        v[9] = "Crypto";
        v[10] = "-1.0";
        v[15] = "100.00";
        v[22] = "M";
        v[32] = "SI";
        v[41] = Documento;
        Importazioni.RiempiVuotiArray(v);
        MappaCryptoWallet.put(ID, v);
        return v;
    }
}
