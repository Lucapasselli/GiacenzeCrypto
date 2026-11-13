package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali_Ripristino.Anteprima;
import com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali_Ripristino.Contenuto;
import com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali_Ripristino.Esito;
import com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali_Ripristino.FileNonValido;
import com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali_Ripristino.Piano;
import com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali_Ripristino.Risultato;
import com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali_Ripristino.Storico;
import com.giacenzecrypto.giacenze_crypto.Principale_Opzioni_ModificheManuali_Ripristino.Voce;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Lettore e anteprima del ripristino delle modifiche manuali, su movimenti inventati. */
class Principale_Opzioni_ModificheManuali_RipristinoTest {

    private static final String L = "lignaggio-1";

    private static String[] mov(String id, String data, String moneta, String qta, String prezzo) {
        String[] v = new String[Importazioni.ColonneTabella];
        Arrays.fill(v, "");
        v[0] = id;
        v[1] = data;
        v[3] = "ExchangeProva";
        v[8] = moneta;
        v[10] = qta;
        v[15] = prezzo;
        return v;
    }

    private static String[] versione(String idVecchio, long data, String[] prima, String operazione, String idNuovo) {
        return new String[]{idVecchio, String.valueOf(data), Importazioni.SerializzaRiga(prima), operazione, idNuovo};
    }

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

    /** Esporta e rilegge: è il percorso vero del file. */
    private static Contenuto file(Map<String, String[]> finale, Map<String, List<String[]>> storico) throws Exception {
        return Principale_Opzioni_ModificheManuali_Ripristino.Leggi(Testo(Costruisci(finale, storico)));
    }

    private static Esito unico(Anteprima a, String tipo) {
        List<Voce> v = a.voci().stream().filter(x -> x.tipo().equals(tipo)).toList();
        assertEquals(1, v.size(), "una voce di tipo " + tipo + ": " + a.voci());
        return v.get(0).esito();
    }

    // --- lettura ---------------------------------------------------------------------------------------------

    @Test
    void ilFileRilettoHaGliStessiRecord() throws Exception {
        String[] prima = mov("A_1", "2025/01/01 10:00", "BTC", "1", "10.00");
        String[] dopo = mov("A_1", "2025/01/01 10:00", "BTC", "1", "12.50");
        dopo[42] = L;
        Map<String, List<String[]>> storico = Map.of(L, catena(versione("A_1", 1000, prima, "ModificaMovimento", "A_1")));

        Esportazione e = Costruisci(mappa(dopo), storico);
        Contenuto c = Principale_Opzioni_ModificheManuali_Ripristino.Leggi(Testo(e));

        assertEquals(e.record().size(), c.record().size());
        assertEquals(FORMATO, c.formato());
        assertEquals(Importazioni.ColonneTabella, c.campi());
        assertEquals("12.50", c.record().stream().filter(r -> r[C_TIPO].equals(TIPO_FINALE)).findFirst().get()[COLONNE_FISSE + 15]);
    }

    @Test
    void ilLettoreRifiutaIFileCheNonConosce() {
        assertThrows(FileNonValido.class, () -> Principale_Opzioni_ModificheManuali_Ripristino.Leggi("qualcosa;di;altro\r\nx\r\n"));
        assertThrows(FileNonValido.class, () -> Principale_Opzioni_ModificheManuali_Ripristino
                .Leggi("#GiacenzeCrypto ModificheManuali;formato=99;campi=46;\r\nTipo\r\n"));
        assertThrows(FileNonValido.class, () -> Principale_Opzioni_ModificheManuali_Ripristino
                .Leggi("#GiacenzeCrypto ModificheManuali;formato=1;campi=10;\r\nTipo\r\n"));
        FileNonValido corta = assertThrows(FileNonValido.class, () -> Principale_Opzioni_ModificheManuali_Ripristino
                .Leggi("#GiacenzeCrypto ModificheManuali;formato=1;campi=46;\r\nTipo\r\nINIZIALE;a;b\r\n"));
        assertTrue(corta.getMessage().contains("Riga 3"));
    }

    // --- modifiche puntuali ----------------------------------------------------------------------------------

    private Contenuto prezzoCambiato() throws Exception {
        String[] prima = mov("A_1", "2025/01/01 10:00", "BTC", "1", "10.00");
        String[] dopo = mov("A_1", "2025/01/01 10:00", "BTC", "1", "12.50");
        dopo[42] = L;
        return file(mappa(dopo), Map.of(L, catena(versione("A_1", 1000, prima, "ModificaMovimento", "A_1"))));
    }

    @Test
    void modificaPuntualeApplicabileSuArchivioPulito() throws Exception {
        //L'archivio nuovo ha un altro documento di origine e costi ricalcolati: non contano
        String[] nuovo = mov("A_1", "2025/01/01 10:00", "BTC", "1", "10.00");
        nuovo[41] = "7";
        nuovo[16] = "123.00";

        Anteprima a = Principale_Opzioni_ModificheManuali_Ripristino.Analizza(prezzoCambiato(), mappa(nuovo));

        assertEquals(Esito.APPLICABILE, unico(a, "Modifica puntuale"));
    }

    @Test
    void modificaPuntualeGiaApplicataEInConflittoENonTrovata() throws Exception {
        Contenuto c = prezzoCambiato();
        assertEquals(Esito.GIA_APPLICATA, unico(Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c,
                mappa(mov("A_1", "2025/01/01 10:00", "BTC", "1", "12.50"))), "Modifica puntuale"));
        Anteprima conflitto = Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c,
                mappa(mov("A_1", "2025/01/01 10:00", "BTC", "1", "99.00")));
        assertEquals(Esito.CONFLITTO, unico(conflitto, "Modifica puntuale"));
        assertTrue(conflitto.problemi().get(0).contains("[15] Valore in euro"));
        assertEquals(Esito.NON_TROVATO, unico(Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, mappa()), "Modifica puntuale"));
    }

    @Test
    void quantitaScrittaDiversamenteMaUgualeNonEConflitto() throws Exception {
        //1.50 e 1.5 sono lo stesso valore
        String[] prima = mov("A_1", "2025/01/01 10:00", "BTC", "1", "1.5");
        String[] dopo = mov("A_1", "2025/01/01 10:00", "BTC", "1", "2.5");
        dopo[42] = L;
        Contenuto c = file(mappa(dopo), Map.of(L, catena(versione("A_1", 1000, prima, "ModificaMovimento", "A_1"))));

        Anteprima a = Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, mappa(mov("A_1", "2025/01/01 10:00", "BTC", "1.0", "1.50")));

        assertEquals(Esito.APPLICABILE, unico(a, "Modifica puntuale"));
    }

    @Test
    void rinominaSiRitrovaPerIdPerChiaveEGiaApplicata() throws Exception {
        String[] prima = mov("20250101100000_E_1_1_DC", "2025/01/01 10:00", "BTC", "1", "10.00");
        String[] dopo = mov("20250101110000_E_1_1_DC", "2025/01/01 11:00", "BTC", "1", "10.00");
        dopo[42] = L;
        Contenuto c = file(mappa(dopo), Map.of(L, catena(versione(prima[0], 1000, prima, "TraslaOrario", dopo[0]))));

        assertEquals(Esito.APPLICABILE, unico(Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, mappa(prima)), "Rinomina"));
        //Stessa data, exchange, moneta e quantità ma un altro ID: si ritrova per chiave
        String[] altroId = mov("20250101100000_E_1_7_DC", "2025/01/01 10:00", "BTC", "1", "10.00");
        assertEquals(Esito.APPLICABILE, unico(Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, mappa(altroId)), "Rinomina"));
        String[] gia = mov("20250101110000_E_1_1_DC", "2025/01/01 11:00", "BTC", "1", "10.00");
        assertEquals(Esito.GIA_APPLICATA, unico(Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, mappa(gia)), "Rinomina"));
    }

    @Test
    void duePartiIdenticheNonSiPrendonoLoStessoMovimento() throws Exception {
        //Due modifiche su due movimenti con la stessa chiave: servono due movimenti in archivio
        String[] p1 = mov("20250101100000_E_1_1_DC", "2025/01/01 10:00", "BTC", "1", "10.00");
        String[] p2 = mov("20250101100000_E_1_2_DC", "2025/01/01 10:00", "BTC", "1", "10.00");
        String[] d1 = mov(p1[0], "2025/01/01 10:00", "BTC", "1", "11.00");
        String[] d2 = mov(p2[0], "2025/01/01 10:00", "BTC", "1", "12.00");
        d1[42] = "l1";
        d2[42] = "l2";
        Contenuto c = file(mappa(d1, d2), Map.of(
                "l1", catena(versione(p1[0], 1000, p1, "ModificaMovimento", p1[0])),
                "l2", catena(versione(p2[0], 2000, p2, "ModificaMovimento", p2[0]))));

        //Archivio con un solo movimento di quella chiave, con gli ID che non corrispondono: la seconda non si ritrova
        Anteprima a = Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c,
                mappa(mov("20250101100000_E_1_9_DC", "2025/01/01 10:00", "BTC", "1", "10.00")));

        assertEquals(1, a.conta(Esito.APPLICABILE));
        assertEquals(1, a.conta(Esito.NON_TROVATO));
    }

    @Test
    void unioneSeparazioneECancellato() throws Exception {
        String[] m1 = mov("M_1", "2025/01/01 10:00", "BTC", "1", "5.00");
        String[] m2 = mov("M_2", "2025/01/01 10:00", "BTC", "2", "6.00");
        String[] unito = mov("N_1", "2025/01/01 10:00", "BTC", "3", "11.00");
        unito[42] = L;
        Contenuto unione = file(mappa(unito), Map.of(L, catena(
                versione("M_1", 1000, m1, "UnisciMovimenti", "N_1"), versione("M_2", 1000, m2, "UnisciMovimenti", "N_1"))));
        assertEquals(Esito.APPLICABILE, unico(Principale_Opzioni_ModificheManuali_Ripristino.Analizza(unione, mappa(m1, m2)), "Unione"));
        assertEquals(Esito.NON_TROVATO, unico(Principale_Opzioni_ModificheManuali_Ripristino.Analizza(unione, mappa(m1)), "Unione"));

        String[] prima = mov("S_1", "2025/01/01 10:00", "BTC", "1", "10.00");
        String[] g1 = mov("S_1a", "2025/01/01 10:00", "BTC", "1", "10.00");
        String[] g2 = mov("S_1b", "2025/01/01 10:00", "BTC", "1", "10.00");
        g1[42] = L;
        g2[42] = L;
        Contenuto sep = file(mappa(g1, g2), Map.of(L, catena(versione("S_1", 1000, prima, "ModificaMovimento", "S_1a"))));
        assertEquals(Esito.MANUALE, unico(Principale_Opzioni_ModificheManuali_Ripristino.Analizza(sep, mappa(prima)), "Separazione"));

        Contenuto cancellato = file(mappa(), Map.of(L, catena(versione("A_1", 1000, prima, "ModificaMovimento", "A_1"))));
        assertEquals(Esito.NESSUNA_AZIONE,
                unico(Principale_Opzioni_ModificheManuali_Ripristino.Analizza(cancellato, mappa(prima)), "Movimento non più presente"));
    }

    // --- senza storico ---------------------------------------------------------------------------------------

    @Test
    void inseritiAManoEMarcatiM() throws Exception {
        String[] inserito = mov("MAN_1", "2025/01/01 10:00", "BTC", "1", "1.00");
        inserito[39] = "E";
        String[] marcato = mov("MAR_1", "2025/01/02 10:00", "ETH", "2", "2.00");
        marcato[22] = "M";
        Contenuto c = file(mappa(inserito, marcato), Map.of());

        Anteprima libero = Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, mappa());
        assertEquals(Esito.APPLICABILE, unico(libero, CATEGORIA_INSERITO));
        assertEquals(Esito.NON_TROVATO, unico(libero, CATEGORIA_MARCATO_M));

        Anteprima occupato = Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, mappa(
                mov("MAN_1", "2025/01/01 10:00", "XRP", "9", "9.00"), mov("MAR_1", "2025/01/02 10:00", "ETH", "2", "7.00")));
        assertEquals(Esito.CONFLITTO, unico(occupato, CATEGORIA_INSERITO));
        assertEquals(Esito.DA_VERIFICARE, unico(occupato, CATEGORIA_MARCATO_M));

        Anteprima uguali = Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, mappa(inserito, marcato));
        assertEquals(2, uguali.conta(Esito.GIA_APPLICATA));
    }

    // --- classificazioni -------------------------------------------------------------------------------------

    private static final String PC = "20250102100000_Ex_1_1_PC";
    private static final String CM = "20250102100000_Ex_1_2_CM";

    /** Un prelievo classificato come trasferimento: la quantità è scesa di 0.1 per la commissione generata. */
    private Contenuto classificato() throws Exception {
        String[] prelievo = mov(PC, "2025/01/02 10:00", "BTC", "0.9", "9.00");
        prelievo[18] = "PTW - Trasferimento";
        prelievo[20] = PC + "," + CM;
        String[] commissione = mov(CM, "2025/01/02 10:00", "BTC", "0.1", "1.00");
        commissione[22] = "AU";
        commissione[20] = PC + "," + CM;
        return file(mappa(prelievo, commissione), Map.of());
    }

    @Test
    void classificazioneApplicabileConIdOChiave() throws Exception {
        Contenuto c = classificato();
        //Stesso ID: il prelievo non classificato ha la quantità intera
        assertEquals(Esito.APPLICABILE, unico(Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c,
                mappa(mov(PC, "2025/01/02 10:00", "BTC", "1", "10.00"))), "Classificazione"));
        //ID diverso: si ritrova per chiave con la quantità di prima della classificazione (0.9 + 0.1)
        assertEquals(Esito.APPLICABILE, unico(Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c,
                mappa(mov("20250102100000_Ex_1_9_PC", "2025/01/02 10:00", "BTC", "1", "10.00"))), "Classificazione"));
    }

    @Test
    void classificazioneNonTrovataInConflittoEGiaApplicata() throws Exception {
        Contenuto c = classificato();
        assertEquals(Esito.NON_TROVATO, unico(Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, mappa()), "Classificazione"));
        //Quantità diversa da quella di prima della classificazione: non è lo stesso movimento
        assertEquals(Esito.NON_TROVATO, unico(Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c,
                mappa(mov("20250102100000_Ex_1_9_PC", "2025/01/02 10:00", "BTC", "5", "10.00"))), "Classificazione"));
        //L'ID della riga generata è già di un altro movimento
        assertEquals(Esito.CONFLITTO, unico(Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, mappa(
                mov(PC, "2025/01/02 10:00", "BTC", "1", "10.00"), mov(CM, "2025/01/02 10:00", "ETH", "7", "7.00"))), "Classificazione"));
        //Stesso gruppo già classificato in un altro modo
        String[] altro = mov(PC, "2025/01/02 10:00", "BTC", "1", "10.00");
        altro[20] = PC + ",ALTRO_ID";
        assertEquals(Esito.CONFLITTO, unico(Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, mappa(altro)), "Classificazione"));
        //Archivio che ha già il gruppo com'è nel file
        String[] prelievo = mov(PC, "2025/01/02 10:00", "BTC", "0.9", "9.00");
        prelievo[18] = "PTW - Trasferimento";
        prelievo[20] = PC + "," + CM;
        String[] commissione = mov(CM, "2025/01/02 10:00", "BTC", "0.1", "1.00");
        commissione[22] = "AU";
        commissione[20] = PC + "," + CM;
        assertEquals(Esito.GIA_APPLICATA, unico(Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, mappa(prelievo, commissione)), "Classificazione"));
    }

    @Test
    void lAnalisiNonModificaLArchivio() throws Exception {
        String[] nuovo = mov("A_1", "2025/01/01 10:00", "BTC", "1", "10.00");
        Map<String, String[]> archivio = mappa(nuovo);
        String prima = Importazioni.SerializzaRiga(nuovo);

        Principale_Opzioni_ModificheManuali_Ripristino.Analizza(prezzoCambiato(), archivio);

        assertEquals(1, archivio.size());
        assertEquals(prima, Importazioni.SerializzaRiga(archivio.get("A_1")));
    }

    @Test
    void ilRiepilogoElencaTipiEdEsiti() throws Exception {
        Anteprima a = Principale_Opzioni_ModificheManuali_Ripristino.Analizza(prezzoCambiato(), mappa());
        assertTrue(a.riepilogo().contains("Modifica puntuale: 1 (1 non ritrovate)"), a.riepilogo());
        assertEquals(1, a.problemi().size());
    }

    // --- applicazione (fase 2) -------------------------------------------------------------------------------

    /** Un registratore al posto dello storico vero: tiene le voci e dice se una data c'è già. */
    private static final class Registratore implements Storico {

        final List<String[]> voci = new ArrayList<>();

        @Override
        public boolean contiene(String lignaggio, long millis, String idVecchio) {
            return voci.stream().anyMatch(v -> v[0].equals(lignaggio) && v[5].equals(String.valueOf(millis)) && v[2].equals(idVecchio));
        }

        @Override
        public void aggiungi(String lignaggio, String idNuovo, String idVecchio, String riga, String operazione, long millis) {
            voci.add(new String[]{lignaggio, idNuovo, idVecchio, riga, operazione, String.valueOf(millis)});
        }
    }

    private static Risultato applica(Contenuto c, Map<String, String[]> archivio, Registratore storico) {
        Piano piano = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio);
        return Principale_Opzioni_ModificheManuali_Ripristino.Applica(piano, archivio, storico);
    }

    @Test
    void applicaUnaModificaPuntualeESoloQuella() throws Exception {
        String[] nuovo = mov("A_1", "2025/01/01 10:00", "BTC", "1", "10.00");
        nuovo[41] = "7"; //documento di origine del nuovo archivio: non si tocca
        nuovo[16] = "123.00"; //campo ricalcolato: non si tocca
        nuovo[21] = "nota del reimport";
        Map<String, String[]> archivio = mappa(nuovo);
        Registratore storico = new Registratore();

        Risultato r = applica(prezzoCambiato(), archivio, storico);

        assertEquals(1, r.applicate());
        assertEquals("12.50", archivio.get("A_1")[15]);
        assertEquals("7", archivio.get("A_1")[41]);
        assertEquals("123.00", archivio.get("A_1")[16]);
        assertEquals("nota del reimport", archivio.get("A_1")[21]);
        assertEquals(L, archivio.get("A_1")[42], "il lignaggio del file");
        assertEquals(1, storico.voci.size());
        assertEquals("1000", storico.voci.get(0)[5], "la data originale della modifica, non quella del ripristino");
        assertEquals(L, storico.voci.get(0)[0]);
    }

    @Test
    void applicareDueVolteDaTuttoGiaApplicato() throws Exception {
        Map<String, String[]> archivio = mappa(mov("A_1", "2025/01/01 10:00", "BTC", "1", "10.00"));
        Registratore storico = new Registratore();
        Contenuto c = prezzoCambiato();

        assertEquals(1, applica(c, archivio, storico).applicate());
        Piano secondo = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio);

        assertEquals(0, secondo.eseguibili());
        assertEquals(Esito.GIA_APPLICATA, secondo.anteprima().voci().get(0).esito());
        assertEquals(0, Principale_Opzioni_ModificheManuali_Ripristino.Applica(secondo, archivio, storico).applicate());
        assertEquals(1, storico.voci.size(), "lo storico non si raddoppia");
    }

    @Test
    void ilConflittoNonSiApplica() throws Exception {
        String[] diverso = mov("A_1", "2025/01/01 10:00", "BTC", "1", "99.00");
        Map<String, String[]> archivio = mappa(diverso);
        Registratore storico = new Registratore();

        Risultato r = applica(prezzoCambiato(), archivio, storico);

        assertEquals(0, r.applicate());
        assertEquals("99.00", archivio.get("A_1")[15]);
        assertTrue(storico.voci.isEmpty());
    }

    private Contenuto traslaOrario(String[] prima, String[] dopo) throws Exception {
        dopo[42] = L;
        return file(mappa(dopo), Map.of(L, catena(versione(prima[0], 1000, prima, "TraslaOrario", dopo[0]))));
    }

    @Test
    void laRinominaCambiaAncheLaChiaveDellaMappa() throws Exception {
        String[] prima = mov("20250101100000_E_1_1_DC", "2025/01/01 10:00", "BTC", "1", "10.00");
        String[] dopo = mov("20250101110000_E_1_1_DC", "2025/01/01 11:00", "BTC", "1", "10.00");
        Contenuto c = traslaOrario(prima, dopo);
        Map<String, String[]> archivio = mappa(mov(prima[0], prima[1], "BTC", "1", "10.00"));
        Registratore storico = new Registratore();

        assertEquals(1, applica(c, archivio, storico).applicate());

        assertEquals(1, archivio.size());
        assertTrue(archivio.containsKey(dopo[0]));
        assertEquals(dopo[0], archivio.get(dopo[0])[0]);
        assertEquals("2025/01/01 11:00", archivio.get(dopo[0])[1]);
        assertEquals("TraslaOrario", storico.voci.get(0)[4]);
    }

    @Test
    void laRinominaAvvisaSeIlVecchioIdECitato() throws Exception {
        String[] prima = mov("20250101100000_E_1_1_DC", "2025/01/01 10:00", "BTC", "1", "10.00");
        String[] dopo = mov("20250101110000_E_1_1_DC", "2025/01/01 11:00", "BTC", "1", "10.00");
        Contenuto c = traslaOrario(prima, dopo);
        String[] altro = mov("20250105100000_E_1_1_PC", "2025/01/05 10:00", "ETH", "2", "2.00");
        altro[20] = altro[0] + "," + prima[0];

        Anteprima a = Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, mappa(mov(prima[0], prima[1], "BTC", "1", "10.00"), altro));

        assertTrue(a.voci().get(0).dettaglio().contains("citato nel campo [20]"), a.voci().get(0).dettaglio());
    }

    @Test
    void laRinominaConIdFinaleOccupatoEUnConflitto() throws Exception {
        String[] prima = mov("20250101100000_E_1_1_DC", "2025/01/01 10:00", "BTC", "1", "10.00");
        String[] dopo = mov("20250101110000_E_1_1_DC", "2025/01/01 11:00", "BTC", "1", "10.00");
        Contenuto c = traslaOrario(prima, dopo);
        Map<String, String[]> archivio = mappa(mov(prima[0], prima[1], "BTC", "1", "10.00"),
                mov(dopo[0], "2025/01/01 11:00", "ETH", "5", "5.00"));

        Risultato r = applica(c, archivio, new Registratore());

        assertEquals(0, r.applicate());
        assertEquals(Esito.CONFLITTO, Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, archivio).voci().get(0).esito());
    }

    @Test
    void applicaUnUnioneEUnInseritoAMano() throws Exception {
        String[] m1 = mov("M_1", "2025/01/01 10:00", "BTC", "1", "5.00");
        String[] m2 = mov("M_2", "2025/01/01 10:00", "BTC", "2", "6.00");
        String[] unito = mov("N_1", "2025/01/01 10:00", "BTC", "3", "11.00");
        unito[42] = L;
        unito[41] = "50"; //documento del vecchio archivio: non vale nel nuovo
        String[] inserito = mov("MAN_1", "2025/01/02 10:00", "ETH", "1", "1.00");
        inserito[39] = "E";
        inserito[41] = "99";
        Contenuto c = file(mappa(unito, inserito), Map.of(L, catena(
                versione("M_1", 1000, m1, "UnisciMovimenti", "N_1"), versione("M_2", 1000, m2, "UnisciMovimenti", "N_1"))));
        String[] n1 = mov("M_1", "2025/01/01 10:00", "BTC", "1", "5.00");
        n1[41] = "5";
        String[] n2 = mov("M_2", "2025/01/01 10:00", "BTC", "2", "6.00");
        n2[41] = "6";
        Map<String, String[]> archivio = mappa(n1, n2);
        Registratore storico = new Registratore();

        Risultato r = applica(c, archivio, storico);

        assertEquals(2, r.applicate());
        assertEquals(2, archivio.size(), "le due righe unite diventano una, piu' l'inserito");
        assertEquals(L, archivio.get("N_1")[42]);
        assertEquals(DocumentiFonte.Unione(List.of(n1, n2)), archivio.get("N_1")[41],
                "il documento di origine e' l'unione di quelli abbinati, non quello del file");
        assertEquals("", archivio.get("MAN_1")[41], "il documento del vecchio archivio non esiste nel nuovo");
        assertEquals("", archivio.get("MAN_1")[42]);
        assertEquals(2, storico.voci.size(), "due passaggi dell'unione");
        assertEquals(0, Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio).eseguibili(), "idempotente");
    }

    @Test
    void unaCatenaPoiClassificataSiApplicaConIlGruppo() throws Exception {
        //Il prelievo e' stato modificato (prezzo) e poi classificato: la differenza contiene anche il [20] della classificazione
        String[] prima = mov(PC, "2025/01/02 10:00", "BTC", "1", "10.00");
        String[] dopo = mov(PC, "2025/01/02 10:00", "BTC", "0.9", "9.00");
        dopo[42] = L;
        dopo[18] = "PTW - Trasferimento";
        dopo[20] = PC + "," + CM;
        dopo[21] = "nota";
        String[] commissione = mov(CM, "2025/01/02 10:00", "BTC", "0.1", "1.00");
        commissione[22] = "AU";
        commissione[20] = PC + "," + CM;
        Contenuto c = file(mappa(dopo, commissione), Map.of(L, catena(versione(PC, 1000, prima, "ModificaMovimento", PC))));
        String[] reimportato = mov(PC, "2025/01/02 10:00", "BTC", "1", "10.00");
        reimportato[41] = "7";
        reimportato[16] = "55.00";
        Map<String, String[]> archivio = mappa(reimportato);
        Registratore storico = new Registratore();

        Piano piano = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio);
        assertEquals(1, piano.anteprima().conta(Esito.RINVIATA));
        assertEquals(1, piano.anteprima().conta(Esito.APPLICABILE));
        Risultato r = Principale_Opzioni_ModificheManuali_Ripristino.Applica(piano, archivio, storico);

        assertEquals(1, r.applicate());
        String[] a = archivio.get(PC);
        assertEquals("0.9", a[10]);
        assertEquals("PTW - Trasferimento", a[18]);
        assertEquals("nota", a[21]);
        assertEquals(L, a[42], "il lignaggio della catena");
        assertEquals("7", a[41], "il documento di origine del nuovo archivio");
        assertEquals("55.00", a[16], "il costo di carico lo ricalcola il motore");
        assertEquals("AU", archivio.get(CM)[22]);
        assertEquals(1, storico.voci.size());
        assertEquals(0, Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio).eseguibili(), "idempotente");
    }

    @Test
    void laClassificazioneSiApplicaEIdOChiaveDiversaDiventaQuelloDelFile() throws Exception {
        Contenuto c = classificato();
        String[] reimportato = mov("20250102100000_Ex_1_9_PC", "2025/01/02 10:00", "BTC", "1", "10.00");
        reimportato[41] = "5";
        Map<String, String[]> archivio = mappa(reimportato);

        Risultato r = applica(c, archivio, new Registratore());

        assertEquals(1, r.applicate());
        assertEquals(2, archivio.size(), "l'originale e la commissione generata");
        assertEquals(PC, archivio.get(PC)[0], "la chiave segue l'ID del file, cosi' il [20] resta coerente");
        assertEquals(PC + "," + CM, archivio.get(PC)[20]);
        assertEquals("5", archivio.get(PC)[41]);
        assertEquals("0.9", archivio.get(PC)[10]);
        assertEquals(0, Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio).eseguibili());
    }

    @Test
    void laRigaGeneraTaPrendeLOrigineDelMovimentoCorrispondenteNelNuovoArchivio() throws Exception {
        String[] prelievo = mov(PC, "2025/01/02 10:00", "BTC", "0.9", "9.00");
        prelievo[18] = "PTW - Trasferimento";
        prelievo[20] = PC + "," + CM;
        prelievo[41] = "12|3,4"; //origine 12 e due supporti del vecchio archivio
        String[] commissione = mov(CM, "2025/01/02 10:00", "BTC", "0.1", "1.00");
        commissione[22] = "AU";
        commissione[20] = PC + "," + CM;
        commissione[41] = "12";
        Contenuto c = file(mappa(prelievo, commissione), Map.of());
        String[] reimportato = mov(PC, "2025/01/02 10:00", "BTC", "1", "10.00");
        reimportato[41] = "40";
        Map<String, String[]> archivio = mappa(reimportato);

        applica(c, archivio, new Registratore());

        assertEquals("40", archivio.get(CM)[41], "origine 40: quella del movimento, non la 12 del vecchio archivio");
        assertEquals("40", archivio.get(PC)[41], "i supporti del vecchio archivio si perdono");
    }

    @Test
    void unoScambioDifferitoSiRimetteConIlPrefissoNelLID() throws Exception {
        String base = "20250102100000_Ex_1_1_PC";
        String rinominato = "20250102100000_00Ex_1_1_PC";
        String[] originale = mov(rinominato, "2025/01/02 10:00", "BTC", "1", "10.00");
        originale[18] = "PTW - Scambio Differito";
        originale[20] = rinominato + ",20250102100500_Ex_1_5_SC";
        String[] generato = mov("20250102100500_Ex_1_5_SC", "2025/01/02 10:05", "ETH", "3", "10.00");
        generato[22] = "AU";
        generato[20] = originale[20];
        Contenuto c = file(mappa(originale, generato), Map.of());
        Map<String, String[]> archivio = mappa(mov(base, "2025/01/02 10:00", "BTC", "1", "10.00"));

        Risultato r = applica(c, archivio, new Registratore());

        assertEquals(1, r.applicate());
        assertTrue(archivio.containsKey(rinominato));
        assertFalse(archivio.containsKey(base), "la chiave vecchia sparisce");
        assertTrue(archivio.containsKey("20250102100500_Ex_1_5_SC"));
    }

    @Test
    void unaRinominaSeguitaDaClassificazioneSiApplicaInsieme() throws Exception {
        String pc2 = "20250102110000_Ex_1_1_PC";
        String cm2 = "20250102110000_Ex_1_2_CM";
        String[] prima = mov(PC, "2025/01/02 10:00", "BTC", "1", "10.00");
        String[] dopo = mov(pc2, "2025/01/02 11:00", "BTC", "0.9", "9.00");
        dopo[42] = L;
        dopo[18] = "PTW - Trasferimento";
        dopo[20] = pc2 + "," + cm2;
        String[] commissione = mov(cm2, "2025/01/02 11:00", "BTC", "0.1", "1.00");
        commissione[22] = "AU";
        commissione[20] = dopo[20];
        Contenuto c = file(mappa(dopo, commissione), Map.of(L, catena(versione(PC, 1000, prima, "TraslaOrario", pc2))));
        Map<String, String[]> archivio = mappa(mov(PC, "2025/01/02 10:00", "BTC", "1", "10.00"));
        Registratore storico = new Registratore();

        Risultato r = applica(c, archivio, storico);

        assertEquals(1, r.applicate());
        assertEquals(2, archivio.size());
        assertFalse(archivio.containsKey(PC));
        assertEquals(L, archivio.get(pc2)[42]);
        assertEquals("2025/01/02 11:00", archivio.get(pc2)[1]);
        assertTrue(archivio.containsKey(cm2));
        assertEquals(1, storico.voci.size());
        assertEquals(0, Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio).eseguibili(), "idempotente");
    }

    @Test
    void unUnioneSeguitaDaClassificazioneTogliTuttiIMovimentiDiPartenza() throws Exception {
        String id1 = "20250102100000_Ex_1_1_PC";
        String id2 = "20250102100000_Ex_1_2_PC";
        String idN = "20250102100000_Ex_1_3_PC";
        String cm = "20250102100000_Ex_1_4_CM";
        String[] m1 = mov(id1, "2025/01/02 10:00", "BTC", "0.5", "5.00");
        String[] m2 = mov(id2, "2025/01/02 10:00", "BTC", "0.5", "5.00");
        String[] unito = mov(idN, "2025/01/02 10:00", "BTC", "0.9", "9.00");
        unito[42] = L;
        unito[18] = "PTW - Trasferimento";
        unito[20] = idN + "," + cm;
        String[] commissione = mov(cm, "2025/01/02 10:00", "BTC", "0.1", "1.00");
        commissione[22] = "AU";
        commissione[20] = unito[20];
        Contenuto c = file(mappa(unito, commissione), Map.of(L, catena(
                versione(id1, 1000, m1, "UnisciMovimenti", idN), versione(id2, 1000, m2, "UnisciMovimenti", idN))));
        String[] n1 = mov(id1, "2025/01/02 10:00", "BTC", "0.5", "5.00");
        n1[41] = "5";
        String[] n2 = mov(id2, "2025/01/02 10:00", "BTC", "0.5", "5.00");
        n2[41] = "6";
        Map<String, String[]> archivio = mappa(n1, n2);
        Registratore storico = new Registratore();

        Risultato r = applica(c, archivio, storico);

        assertEquals(1, r.applicate());
        assertEquals(2, archivio.size(), "le due righe diventano una, piu' la commissione");
        assertEquals(L, archivio.get(idN)[42]);
        assertEquals(2, storico.voci.size());
        assertEquals(0, Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio).eseguibili(), "idempotente");
    }

    @Test
    void unOriginaleInArchivioConUnAltraFormaEUnConflittoENonSiSovrascrive() throws Exception {
        Contenuto c = classificato();
        //Stesso ID ma quantita' 5: l'importazione e' cambiata, copiare i campi del file la cancellerebbe in silenzio
        String[] diverso = mov(PC, "2025/01/02 10:00", "BTC", "5", "10.00");
        Map<String, String[]> archivio = mappa(diverso);

        Piano piano = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio);
        Risultato r = Principale_Opzioni_ModificheManuali_Ripristino.Applica(piano, archivio, new Registratore());

        assertEquals(Esito.CONFLITTO, piano.anteprima().voci().get(0).esito());
        assertEquals(0, r.applicate());
        assertEquals("5", archivio.get(PC)[10]);
        assertEquals(1, archivio.size());
    }

    @Test
    void conUnIdGeneratoOccupatoIlGruppoNonSiApplicaPerNiente() throws Exception {
        Contenuto c = classificato();
        Map<String, String[]> archivio = mappa(mov(PC, "2025/01/02 10:00", "BTC", "1", "10.00"),
                mov(CM, "2025/01/02 10:00", "ETH", "7", "7.00"));

        Risultato r = applica(c, archivio, new Registratore());

        assertEquals(0, r.applicate());
        assertEquals("1", archivio.get(PC)[10], "l'originale non e' stato toccato");
    }

    @Test
    void sePrimaDiApplicareUnIdSiOccupaIlGruppoNonFaNulla() throws Exception {
        Contenuto c = classificato();
        Map<String, String[]> archivio = mappa(mov(PC, "2025/01/02 10:00", "BTC", "1", "10.00"));
        Piano piano = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio);
        archivio.put(CM, mov(CM, "2025/01/02 10:00", "ETH", "7", "7.00")); //nel frattempo qualcuno prende l'ID

        Risultato r = Principale_Opzioni_ModificheManuali_Ripristino.Applica(piano, archivio, new Registratore());

        assertEquals(0, r.applicate());
        assertEquals(1, r.saltate().size());
        assertEquals("1", archivio.get(PC)[10], "tutto o niente: nemmeno l'originale e' stato modificato");
    }

    // --- scelta delle modifiche da riapplicare ---------------------------------------------------------------

    /** Due movimenti con il prezzo cambiato, in catene diverse. */
    private Contenuto dueModifichePuntuali() throws Exception {
        String[] p1 = mov("A_1", "2025/01/01 10:00", "BTC", "1", "10.00");
        String[] d1 = mov("A_1", "2025/01/01 10:00", "BTC", "1", "12.50");
        d1[42] = "L1";
        String[] p2 = mov("B_1", "2025/01/02 10:00", "ETH", "2", "20.00");
        String[] d2 = mov("B_1", "2025/01/02 10:00", "ETH", "2", "25.00");
        d2[42] = "L2";
        return file(mappa(d1, d2), Map.of(
                "L1", catena(versione("A_1", 1000, p1, "ModificaMovimento", "A_1")),
                "L2", catena(versione("B_1", 2000, p2, "ModificaMovimento", "B_1"))));
    }

    private static Map<String, String[]> archivioDelleDue() {
        return mappa(mov("A_1", "2025/01/01 10:00", "BTC", "1", "10.00"), mov("B_1", "2025/01/02 10:00", "ETH", "2", "20.00"));
    }

    @Test
    void siApplicanoSoloLeModificheScelte() throws Exception {
        Contenuto c = dueModifichePuntuali();
        Map<String, String[]> archivio = archivioDelleDue();
        Registratore storico = new Registratore();
        Piano piano = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio);
        int indiceB = piano.anteprima().voci().stream().map(Voce::chiave).toList().indexOf("B_1");

        Risultato r = Principale_Opzioni_ModificheManuali_Ripristino.Applica(piano, archivio, storico, java.util.Set.of(indiceB));

        assertEquals(1, r.applicate());
        assertEquals("10.00", archivio.get("A_1")[15], "la non scelta resta com'era");
        assertEquals("", archivio.get("A_1")[42], "e non riceve il lignaggio");
        assertEquals("25.00", archivio.get("B_1")[15]);
        assertEquals(1, storico.voci.size(), "lo storico e' solo quello della modifica applicata");
        //La rimasta e' ancora applicabile: si puo' fare in un secondo momento
        Piano dopo = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio);
        assertEquals(1, dopo.eseguibili());
        assertEquals(1, Principale_Opzioni_ModificheManuali_Ripristino.Applica(dopo, archivio, storico).applicate());
        assertEquals("12.50", archivio.get("A_1")[15]);
    }

    @Test
    void unaSceltaVuotaNonApplicaNulla() throws Exception {
        Map<String, String[]> archivio = archivioDelleDue();
        Piano piano = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(dueModifichePuntuali(), archivio);

        Risultato r = Principale_Opzioni_ModificheManuali_Ripristino.Applica(piano, archivio, new Registratore(), java.util.Set.of());

        assertEquals(0, r.applicate());
        assertEquals("10.00", archivio.get("A_1")[15]);
        assertEquals("20.00", archivio.get("B_1")[15]);
    }

    @Test
    void unGruppoClassificatoSiPuoNonRiapplicare() throws Exception {
        Contenuto c = classificato();
        Map<String, String[]> archivio = mappa(mov(PC, "2025/01/02 10:00", "BTC", "1", "10.00"));
        Piano piano = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, archivio);

        Risultato r = Principale_Opzioni_ModificheManuali_Ripristino.Applica(piano, archivio, new Registratore(), java.util.Set.of());

        assertEquals(0, r.applicate());
        assertEquals(1, archivio.size());
        assertEquals("1", archivio.get(PC)[10]);
    }

    @Test
    void leVociDiconoQualeMovimentoEECosaCambia() throws Exception {
        Piano piano = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(dueModifichePuntuali(), archivioDelleDue());

        Voce v = piano.anteprima().voci().get(0);

        assertEquals("2025/01/01 10:00", v.quando());
        assertTrue(v.movimento().startsWith("ExchangeProva: 1 BTC"), v.movimento());
        assertTrue(v.dettaglio().contains("[15] Valore in euro: 10.00 -> 12.50"), v.dettaglio());
        assertTrue(piano.eseguibile(0));
    }

    @Test
    void ilGruppoClassificatoDescriveUnMovimentoDelGruppo() throws Exception {
        Piano piano = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(classificato(),
                mappa(mov(PC, "2025/01/02 10:00", "BTC", "1", "10.00")));

        Voce v = piano.anteprima().voci().get(0);

        assertEquals("2025/01/02 10:00", v.quando());
        assertFalse(v.movimento().isEmpty());
    }

    // --- dettaglio completo (pannello della finestra di scelta) ----------------------------------------------

    private static final String FONTE_LUNGA = "BTC|1740823200000|60000.123456789|Una-fonte-prezzi-con-un-nome-molto-lungo-ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Test
    void ilDettaglioCompletoNonAbbreviaIValoriLunghiMaQuelloBreveSi() throws Exception {
        String[] p1 = mov("A_1", "2025/01/01 10:00", "BTC", "1", "10.00");
        String[] d1 = mov("A_1", "2025/01/01 10:00", "BTC", "1", "12.50");
        d1[40] = FONTE_LUNGA;
        d1[42] = "L1";
        Contenuto c = file(mappa(d1), Map.of("L1", catena(versione("A_1", 1000, p1, "ModificaPrezzo", "A_1"))));

        Voce v = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(c, mappa(mov("A_1", "2025/01/01 10:00", "BTC", "1", "10.00")))
                .anteprima().voci().get(0);

        assertFalse(v.dettaglio().contains(FONTE_LUNGA), "il dettaglio breve abbrevia il valore lungo");
        assertTrue(v.dettaglio().contains("..."));
        assertTrue(v.completo().contains(FONTE_LUNGA), "il completo ha il valore intero");
        assertTrue(v.completo().contains("ora:     10.00") && v.completo().contains("diventa: 12.50"), v.completo());
        assertTrue(v.completo().contains("ora:     (vuoto)"), "il valore vuoto si scrive");
    }

    @Test
    void ilConflittoSpiegaNelCompletoTuttiITreValori() throws Exception {
        Voce v = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(prezzoCambiato(),
                mappa(mov("A_1", "2025/01/01 10:00", "BTC", "1", "99.00"))).anteprima().voci().get(0);

        assertEquals(Esito.CONFLITTO, v.esito());
        assertTrue(v.completo().contains("nell'archivio: 99.00"), v.completo());
        assertTrue(v.completo().contains("il file si aspetta: 10.00"), v.completo());
        assertTrue(v.completo().contains("oppure, se già applicata: 12.50"), v.completo());
    }

    @Test
    void ilCompletoDiUnUnioneEDiUnGruppoElencaITuttiIMovimenti() throws Exception {
        String[] m1 = mov("M_1", "2025/01/01 10:00", "BTC", "1", "5.00");
        String[] m2 = mov("M_2", "2025/01/01 10:00", "BTC", "2", "6.00");
        String[] unito = mov("N_1", "2025/01/01 10:00", "BTC", "3", "11.00");
        unito[42] = L;
        Contenuto unione = file(mappa(unito), Map.of(L, catena(
                versione("M_1", 1000, m1, "UnisciMovimenti", "N_1"), versione("M_2", 1000, m2, "UnisciMovimenti", "N_1"))));
        Voce vu = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(unione, mappa(m1, m2)).anteprima().voci().get(0);
        assertTrue(vu.completo().contains("M_1") && vu.completo().contains("M_2") && vu.completo().contains("N_1"), vu.completo());

        Voce vg = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(classificato(),
                mappa(mov(PC, "2025/01/02 10:00", "BTC", "1", "10.00"))).anteprima().voci().get(0);
        assertTrue(vg.completo().contains("Originali ritrovati") && vg.completo().contains(PC), vg.completo());
        assertTrue(vg.completo().contains("Righe generate dal programma") && vg.completo().contains(CM), vg.completo());
    }

    @Test
    void unaVoceSenzaCompletoUsaIlDettaglio() {
        assertEquals("perche", new Voce("T", "K", Esito.NON_TROVATO, "perche").completo());
        assertEquals("perche", new Voce("T", "K", Esito.NON_TROVATO, "perche", "d", "m").completo());
    }

    @Test
    void unAzioneCheTrovaLArchivioCambiatoNonFaNulla() throws Exception {
        Map<String, String[]> archivio = mappa(mov("A_1", "2025/01/01 10:00", "BTC", "1", "10.00"));
        Piano piano = Principale_Opzioni_ModificheManuali_Ripristino.Pianifica(prezzoCambiato(), archivio);
        archivio.clear(); //nel frattempo il movimento sparisce

        Risultato r = Principale_Opzioni_ModificheManuali_Ripristino.Applica(piano, archivio, new Registratore());

        assertEquals(0, r.applicate());
        assertEquals(1, r.saltate().size());
        assertTrue(archivio.isEmpty());
    }

    @Test
    void unaDivisioneDellaCartaMiniPayNonSiRipristinaESiRimandaAlCsv() throws Exception {
        //Un movimento della catena diviso per pagamento: una riga di partenza e N finali con lo stesso lignaggio
        String[] prima = mov("C_1", "2025/01/01 18:00", "USDT", "9", "8.00");
        String[] p1 = mov("C_1a", "2025/01/01 15:26", "USDT", "2", "1.80");
        String[] p2 = mov("C_1b", "2025/01/01 15:45", "USDT", "7", "6.30");
        p1[42] = L;
        p2[42] = L;
        Contenuto c = file(mappa(p1, p2), Map.of(L, catena(versione("C_1", 1000, prima, MiniPay_SpeseCarta.OP_DIVISIONE, "C_1a"))));
        Anteprima a = Principale_Opzioni_ModificheManuali_Ripristino.Analizza(c, mappa(prima));
        assertEquals(0, a.conta(Esito.APPLICABILE), "non si applica niente: ne' i pezzi ne' la riga della catena");
        Voce v = a.voci().stream().filter(x -> x.tipo().equals("Separazione")).findFirst().orElseThrow();
        assertEquals(Esito.MANUALE, v.esito());
        assertTrue(v.dettaglio().contains("CSV"), v.dettaglio());
    }
}
