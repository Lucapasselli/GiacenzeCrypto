package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Recupero dei movimenti che un'importazione ha scartato perché sconosciuti, rileggendo i documenti di origine.
 *
 * <p><b>Il problema.</b> Uno scaricamento via API parte dall'ultimo movimento già importato + 1 s
 * ({@code GUI_ExchangeAPI.ScaricaExchange}). Un record grezzo che le mappe non riconoscono (es. il bill OKX
 * {@code type 328}, mappato solo dal 2026-09-09) non viene scritto, e il primo movimento noto successivo sposta la
 * data di partenza oltre di lui: nessuno scaricamento lo richiede più, e l'archivio resta senza quel movimento per
 * sempre. È successo davvero, a un utente con 27 ricompense di staking OKSOL mancanti.
 *
 * <p><b>La soluzione.</b> I record grezzi sono ancora nel documento NDJSON dello scaricamento
 * ({@link DocumentiFonte}): li si riconverte con le mappe di oggi. Due tabelle in {@code personale.mv.db}:
 * <ul>
 *   <li>{@code SCARTI_IMPORT}: i record scartati, scritti da chi importa ({@link #Registra}) con stato
 *       {@code ATTESA}, oppure marcati {@code IGNORATO} quando l'utente rinuncia a recuperarli;</li>
 *   <li>{@code SCARTI_ANALISI}: per ogni documento, l'impronta delle mappe con cui è già stato riletto, così una
 *       rilettura si rifà solo quando le mappe cambiano.</li>
 * </ul>
 *
 * <p><b>Perché serve il registro, e non basta rileggere i documenti.</b> Un movimento cancellato dall'utente e
 * uno mai importato sono indistinguibili guardando solo l'archivio: rileggere tutto alla cieca riporterebbe
 * dentro ciò che l'utente ha eliminato. Il registro distingue i candidati <i>certi</i> (scartati perché
 * sconosciuti) da quelli <i>incerti</i> (trovati in un documento ma assenti dall'archivio, per esempio in
 * archivi importati prima che il registro esistesse). Nessuno dei due si recupera senza conferma.
 *
 * <p><b>Perché a unità e non a bill.</b> La presenza in archivio non si giudica bill per bill: un ordine eseguito
 * in più fill diventa un movimento solo e il suo {@code [24]} porta solo alcuni {@code billId}. Si giudica per
 * <i>unità</i>, cioè per ciò che l'import farebbe diventare un movimento: dentro un gruppo di
 * {@link Importazioni#Ex_OKX_Raggruppa} le righe di scambio formano un'unità sola, ogni altra riga la propria
 * (è come le tratta {@code Ex_OKX_Consolida}). Un'unità con un bill già in archivio e uno scartato è
 * <i>mista</i>: l'archivio ne contiene una metà, aggiungere l'altra produrrebbe un doppione o una gamba orfana,
 * quindi la si segnala e basta.
 *
 * <p>Il raggruppamento si fa senza consolidare, perché il consolidamento cerca il prezzo di ogni riga (rete);
 * solo i movimenti confermati passano dall'import vero ({@link #RecuperaOKX}).
 *
 * <p>Oggi copre OKX via API (bill del Funding, del Trading e dell'archivio trimestrale). I rendimenti Earn
 * ({@code OKX_Earn}) no: non sono bill e hanno un ID per giornata. Il registro è generico ({@code Origine}),
 * il rilettore va scritto per ogni fonte. Analisi completa in
 * {@code nocommit/Documentazione/Analisi_Recupero_Movimenti_Sconosciuti.md}.
 */
public final class ScartiImport {

    /** Origine dei bill OKX scaricati via API */
    public static final String ORIGINE_OKX = "OKX";

    static final String STATO_ATTESA = "ATTESA";
    static final String STATO_IGNORATO = "IGNORATO";

    /**
     * Versione del rilettore OKX: entra nell'impronta, quindi alzarla fa rileggere tutti i documenti. Va alzata
     * quando cambia il modo in cui si rileggono o si raggruppano i bill, non quando cambiano le mappe (quelle
     * entrano nell'impronta da sole).
     */
    static final int VERSIONE_RILETTORE_OKX = 1;

    /** Categorie che {@code Ex_OKX_Consolida} accumula in un solo scambio: le righe che le portano sono un'unità sola */
    private static final Set<String> CATEGORIE_SCAMBIO = Set.of("DUST-CONVERSION", "SCAMBIO CRYPTO-CRYPTO");

    private static final Pattern START_DATE = Pattern.compile("startDate=(\\d+)");

    private ScartiImport() {
    }

    //=====================================================================================================
    //=== REGISTRO
    //=====================================================================================================
    /**
     * Le righe OKX che la classificazione ha lasciato senza categoria, nella forma da registrare.
     * <p>Va chiamata subito dopo {@code Ex_OKX_OrdinaEClassifica} e prima che {@code [2]} diventi "Principale":
     * il conto fa parte della chiave.
     *
     * @param righeClassificate righe in formato intermedio a 19 campi con {@code [3]} già valorizzato
     * @return per ogni riga sconosciuta {@code {chiave, causale, data}}
     */
    static List<String[]> RigheSconosciute(List<String[]> righeClassificate) {
        List<String[]> scarti = new ArrayList<>();
        if (righeClassificate == null) {
            return scarti;
        }
        for (String[] r : righeClassificate) {
            if (r == null || r.length < 15 || !Funzioni.noData(r[3]) || Funzioni.noData(r[14])) {
                continue;
            }
            scarti.add(new String[]{Chiave(r), r[4], r[0]});
        }
        return scarti;
    }

    /** @return la chiave di una riga OKX nel registro: conto di provenienza e billId */
    static String Chiave(String[] riga) {
        return riga[2].trim() + ":" + riga[14].trim();
    }

    /**
     * Registra i record scartati da un'importazione. Un record già registrato non viene toccato: conserva il
     * documento in cui è stato visto la prima volta e, soprattutto, un eventuale {@code IGNORATO}.
     *
     * @param Origine una delle costanti {@code ORIGINE_*}
     * @param IdDocumento documento che contiene i record grezzi; con {@code 0} non si registra nulla, perché
     *        non ci sarebbe niente da rileggere
     * @param Scarti righe {@code {chiave, causale, data}} come da {@link #RigheSconosciute}
     */
    static void Registra(String Origine, int IdDocumento, List<String[]> Scarti) {
        if (IdDocumento <= 0 || Scarti == null || Scarti.isEmpty() || DatabaseH2.connectionPersonale == null) {
            return;
        }
        String sql = "INSERT INTO SCARTI_IMPORT (Origine, Chiave, IdDocumento, Causale, DataMovimento, Stato) "
                + "SELECT ?, ?, ?, ?, ?, ? WHERE NOT EXISTS "
                + "(SELECT 1 FROM SCARTI_IMPORT WHERE Origine = ? AND Chiave = ?)";
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(sql)) {
            for (String[] s : Scarti) {
                ps.setString(1, Origine);
                ps.setString(2, s[0]);
                ps.setInt(3, IdDocumento);
                ps.setString(4, Taglia(s[1], 255));
                ps.setString(5, Taglia(s[2], 19));
                ps.setString(6, STATO_ATTESA);
                ps.setString(7, Origine);
                ps.setString(8, s[0]);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            System.out.println("ScartiImport.Registra : " + e.getMessage());
        }
    }

    /** @return {@code true} se il registro contiene almeno un record scartato letto da quel documento */
    public static boolean HaScarti(int IdDocumento) {
        if (IdDocumento <= 0 || DatabaseH2.connectionPersonale == null) {
            return false;
        }
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                "SELECT 1 FROM SCARTI_IMPORT WHERE IdDocumento = ? LIMIT 1")) {
            ps.setInt(1, IdDocumento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            System.out.println("ScartiImport.HaScarti : " + e.getMessage());
            return false;
        }
    }

    /** @return chiave → stato di tutti i record registrati per quell'origine */
    static Map<String, String> Stati(String Origine) {
        Map<String, String> stati = new HashMap<>();
        if (DatabaseH2.connectionPersonale == null) {
            return stati;
        }
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                "SELECT Chiave, Stato FROM SCARTI_IMPORT WHERE Origine = ?")) {
            ps.setString(1, Origine);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    stati.put(rs.getString(1), rs.getString(2));
                }
            }
        } catch (Exception e) {
            System.out.println("ScartiImport.Stati : " + e.getMessage());
        }
        return stati;
    }

    /**
     * Segna come {@code IGNORATO} i record dei candidati: non verranno più proposti, qualunque cosa dicano le
     * mappe in futuro. Vale anche per i candidati incerti, che nel registro non c'erano.
     */
    static void Ignora(Collection<Candidato> Candidati) {
        if (Candidati == null || Candidati.isEmpty() || DatabaseH2.connectionPersonale == null) {
            return;
        }
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                "MERGE INTO SCARTI_IMPORT (Origine, Chiave, IdDocumento, Causale, DataMovimento, Stato) "
                + "KEY (Origine, Chiave) VALUES (?, ?, ?, ?, ?, ?)")) {
            for (Candidato c : Candidati) {
                for (String[] r : c.Righe) {
                    ps.setString(1, c.Origine);
                    ps.setString(2, Chiave(r));
                    ps.setInt(3, c.IdDocumento);
                    ps.setString(4, Taglia(r[4], 255));
                    ps.setString(5, Taglia(r[0], 19));
                    ps.setString(6, STATO_IGNORATO);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        } catch (Exception e) {
            System.out.println("ScartiImport.Ignora : " + e.getMessage());
        }
    }

    /** Toglie dal registro tutto ciò che riguarda un documento: chiamata quando il documento viene eliminato. */
    static void CancellaDocumento(int IdDocumento) {
        if (IdDocumento <= 0 || DatabaseH2.connectionPersonale == null) {
            return;
        }
        for (String tabella : new String[]{"SCARTI_IMPORT", "SCARTI_ANALISI"}) {
            try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                    "DELETE FROM " + tabella + " WHERE IdDocumento = ?")) {
                ps.setInt(1, IdDocumento);
                ps.executeUpdate();
            } catch (Exception e) {
                System.out.println("ScartiImport.CancellaDocumento : " + e.getMessage());
            }
        }
    }

    /** @return l'impronta delle mappe con cui il documento è stato riletto l'ultima volta, {@code null} se mai */
    static String ImprontaAnalisi(int IdDocumento) {
        if (DatabaseH2.connectionPersonale == null) {
            return null;
        }
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                "SELECT Impronta FROM SCARTI_ANALISI WHERE IdDocumento = ?")) {
            ps.setInt(1, IdDocumento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (Exception e) {
            System.out.println("ScartiImport.ImprontaAnalisi : " + e.getMessage());
            return null;
        }
    }

    /** Annota che il documento è stato riletto con quell'impronta e non ha lasciato nulla in sospeso. */
    static void SegnaAnalizzato(int IdDocumento, String Impronta) {
        if (IdDocumento <= 0 || DatabaseH2.connectionPersonale == null) {
            return;
        }
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                "MERGE INTO SCARTI_ANALISI (IdDocumento, Impronta) KEY (IdDocumento) VALUES (?, ?)")) {
            ps.setInt(1, IdDocumento);
            ps.setString(2, Impronta);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("ScartiImport.SegnaAnalizzato : " + e.getMessage());
        }
    }

    //=====================================================================================================
    //=== ANALISI
    //=====================================================================================================
    /** Un movimento che si potrebbe recuperare da un documento: l'unità di cui parla la nota di classe. */
    public static final class Candidato {

        public String Origine = ORIGINE_OKX;
        public int IdDocumento;
        /** Nome del documento come lo vede l'utente */
        public String NomeDocumento = "";
        /** Le righe grezze dell'unità, già riconvertite con le mappe di oggi */
        public List<String[]> Righe = new ArrayList<>();
        /** {@code true} se almeno un suo record è nel registro degli scarti: era stato scartato perché sconosciuto */
        public boolean Certo;
        /** {@code true} se l'archivio contiene già una parte dell'unità: non si recupera, si segnala */
        public boolean Misto;

        public String Data() {
            return Righe.isEmpty() ? "" : Righe.get(0)[0];
        }

        /** @return le causali originali dell'unità, senza ripetizioni */
        public String Causali() {
            Set<String> s = new LinkedHashSet<>();
            for (String[] r : Righe) {
                s.add(r[4]);
            }
            return String.join(", ", s);
        }

        /** @return quantità e moneta di ogni riga, es. {@code +0.0118 OKSOL} */
        public String Movimento() {
            List<String> pezzi = new ArrayList<>();
            for (String[] r : Righe) {
                String qta = r[6];
                if (!Funzioni.isNegativo(qta) && !qta.startsWith("+")) {
                    qta = "+" + qta;
                }
                pezzi.add(qta + " " + r[5]);
            }
            return String.join(", ", pezzi);
        }
    }

    /** Esito di una rilettura dei documenti. */
    public static final class Analisi {

        /** Unità assenti dall'archivio: certe e incerte, da proporre all'utente */
        public final List<Candidato> Recuperabili = new ArrayList<>();
        /** Unità già importate in parte: solo da segnalare */
        public final List<Candidato> Misti = new ArrayList<>();
        /** Documenti riletti senza nulla in sospeso, da segnare come analizzati con {@link #Impronta} */
        public final List<Integer> SenzaSospesi = new ArrayList<>();
        /** Impronta delle mappe usate */
        public String Impronta = "";

        public boolean Vuota() {
            return Recuperabili.isEmpty() && Misti.isEmpty();
        }
    }

    /**
     * Impronta della decodifica OKX di oggi: tabella dei codici, mappa delle causali e versione del rilettore.
     * Sul contenuto, non sui numeri di versione dei file: una correzione fatta a mano conta lo stesso.
     */
    static String ImprontaOKX(TipiOKX Tipi, Map<String, String> Mappa) {
        String testo = "rilettore=" + VERSIONE_RILETTORE_OKX + "\n" + Tipi.Impronta() + "\n" + new TreeMap<>(Mappa);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(testo.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(testo.hashCode());
        }
    }

    /**
     * Rilegge i documenti degli scaricamenti OKX via API e cerca i movimenti che l'archivio non ha.
     *
     * @param Archivio i movimenti, di norma {@link Principale#MappaCryptoWallet}
     * @param AncheGiaAnalizzati {@code false} per saltare i documenti già riletti con le mappe di oggi
     * @return l'analisi; vuota se le mappe non sono disponibili
     */
    public static Analisi AnalizzaOKX(Map<String, String[]> Archivio, boolean AncheGiaAnalizzati) {
        Analisi a = new Analisi();
        TipiOKX tipi = TipiOKX.Carica();
        Map<String, String> mappa = Importazioni.Ex_OKX_MappaCausali();
        if (tipi == null || mappa == null) {
            return a;
        }
        a.Impronta = ImprontaOKX(tipi, mappa);

        List<DocumentiFonte.Documento> documenti = new ArrayList<>();
        for (DocumentiFonte.Documento d : DocumentiFonte.Elenco()) {
            if (DocumentiFonte.TIPO_NDJSON.equals(d.Tipo) && ORIGINE_OKX.equalsIgnoreCase(d.Origine.trim())
                    && (AncheGiaAnalizzati || !a.Impronta.equals(ImprontaAnalisi(d.Id)))) {
                documenti.add(d);
            }
        }
        if (documenti.isEmpty()) {
            return a;
        }

        Set<String> presenti = BillPresenti(Archivio);
        Map<String, String> stati = Stati(ORIGINE_OKX);
        //Lo stesso bill compare in piu' documenti quando le finestre di scaricamento si sovrappongono: si
        //propone una volta sola, dal documento piu' vecchio (Elenco() e' dal piu' recente, quindi si rovescia)
        Set<String> giaProposti = new HashSet<>();
        for (int i = documenti.size() - 1; i >= 0; i--) {
            DocumentiFonte.Documento d = documenti.get(i);
            List<String[]> righe = LeggiRigheOKX(d.Id, tipi);
            if (righe == null) {
                continue;   //documento illeggibile o lettura interrotta: non si segna, si riprova la prossima volta
            }
            int sospesi = 0;
            for (List<String[]> unita : Unita(righe, mappa)) {
                Candidato c = Valuta(unita, mappa, presenti, stati);
                if (c == null) {
                    continue;
                }
                String chiaveUnita = Chiave(unita.get(0));
                if (!giaProposti.add(chiaveUnita)) {
                    continue;
                }
                c.IdDocumento = d.Id;
                c.NomeDocumento = d.NomeOriginale;
                (c.Misto ? a.Misti : a.Recuperabili).add(c);
                sospesi++;
            }
            if (sospesi == 0) {
                a.SenzaSospesi.add(d.Id);
            }
        }
        return a;
    }

    /** @return ogni {@code billId} citato nel {@code [24]} di un movimento OKX (anche le singole gambe di uno scambio) */
    static Set<String> BillPresenti(Map<String, String[]> Archivio) {
        Set<String> presenti = new HashSet<>();
        if (Archivio == null) {
            return presenti;
        }
        for (String[] v : Archivio.values()) {
            if (v == null || v.length <= 24 || !"OKX".equalsIgnoreCase(v[3]) || Funzioni.noData(v[24])) {
                continue;
            }
            //Gli scambi uniscono gli ID delle gambe con "-" (prima del 02/04/2026 con "_", vedi ChiaveDedupOKX)
            for (String pezzo : v[24].split("[-_]")) {
                if (!pezzo.isBlank()) {
                    presenti.add(pezzo.trim());
                }
            }
        }
        return presenti;
    }

    /**
     * Divide le righe di un documento nelle unità che l'import farebbe diventare movimenti: i gruppi di
     * {@link Importazioni#Ex_OKX_Raggruppa}, e dentro ogni gruppo le righe di scambio insieme e le altre una per una.
     */
    static List<List<String[]>> Unita(List<String[]> Righe, Map<String, String> Mappa) {
        List<List<String[]>> unita = new ArrayList<>();
        List<String[]> ordinate = Importazioni.Ex_OKX_Ordina(CcxtInterop.deduplicaBillOKX(Righe));
        for (List<String[]> gruppo : Importazioni.Ex_OKX_Raggruppa(ordinate)) {
            List<String[]> scambio = new ArrayList<>();
            for (String[] r : gruppo) {
                String cat = Mappa.get(r[4]);
                if (cat != null && CATEGORIE_SCAMBIO.contains(cat.trim().toUpperCase())) {
                    scambio.add(r);
                } else {
                    List<String[]> sola = new ArrayList<>();
                    sola.add(r);
                    unita.add(sola);
                }
            }
            if (!scambio.isEmpty()) {
                unita.add(scambio);
            }
        }
        return unita;
    }

    /**
     * Decide che cosa è un'unità rispetto all'archivio.
     *
     * @return il candidato (recuperabile o misto), oppure {@code null} se non c'è nulla da proporre: unità già in
     *         archivio, che non diventerebbe un movimento (sconosciuta o NON CONSIDERARE), o già ignorata
     */
    static Candidato Valuta(List<String[]> Unita, Map<String, String> Mappa, Set<String> Presenti, Map<String, String> Stati) {
        boolean presente = false;
        boolean registrato = false;
        boolean utile = false;
        boolean tuttiIgnorati = true;
        for (String[] r : Unita) {
            if (Presenti.contains(r[14].trim())) {
                presente = true;
            }
            String cat = Mappa.get(r[4]);
            boolean riconosciuta = cat != null && !cat.isBlank() && !cat.trim().equalsIgnoreCase("NON CONSIDERARE");
            if (!riconosciuta) {
                continue;
            }
            utile = true;
            String stato = Stati.get(Chiave(r));
            if (!STATO_IGNORATO.equals(stato)) {
                tuttiIgnorati = false;
            }
            if (STATO_ATTESA.equals(stato)) {
                registrato = true;
            }
        }
        if (!utile || tuttiIgnorati) {
            return null;
        }
        //Gia' in archivio: e' un problema solo se una sua parte era stata scartata. Senza registro un'unita'
        //presente e' semplicemente importata (l'ordine in piu' fill di cui parla la nota di classe)
        if (presente && !registrato) {
            return null;
        }
        Candidato c = new Candidato();
        c.Righe = Unita;
        c.Certo = registrato;
        c.Misto = presente;
        return c;
    }

    /**
     * Legge da un documento NDJSON di OKX i bill grezzi e li riconverte con la tabella dei codici di oggi, con gli
     * stessi passaggi che l'import fa prima di classificare: deduplica dei bill arrivati da finestre sovrapposte e
     * ricostruzione della gamba in uscita delle conversioni in Liquid Staking ({@code type} 330), dallo storico
     * On-chain Earn che lo stesso documento conserva. Senza quest'ultima un 330 sembrerebbe uno scambio recuperabile
     * con la sola entrata (visto sui dati di una segnalazione: 43 OKSOL dal nulla); un 330 senza posizione di
     * origine resta invece sconosciuto, come all'import.
     * <p>Tollera un documento troncato (scaricamento terminato a metà, gzip senza chiusura): si tengono le righe
     * complete lette fino al punto di rottura.
     *
     * @return le righe in formato intermedio a 19 campi, {@code null} se il documento non c'è o la conversione
     *         è stata interrotta
     */
    static List<String[]> LeggiRigheOKX(int IdDocumento, TipiOKX Tipi) {
        File f = DocumentiFonte.FileConservato(IdDocumento);
        if (f == null) {
            return null;
        }
        List<String[]> righe = new ArrayList<>();
        JsonArray storicoOnChain = new JsonArray();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new FileInputStream(f)), StandardCharsets.UTF_8))) {
            String linea;
            while (true) {
                try {
                    linea = br.readLine();
                } catch (IOException troncato) {
                    break;
                }
                if (linea == null) {
                    break;
                }
                List<String[]> daRiga = RigheDaRispostaOKX(linea, Tipi, storicoOnChain);
                if (daRiga == null) {
                    return null;
                }
                righe.addAll(daRiga);
            }
        } catch (IOException e) {
            System.out.println("ScartiImport.LeggiRigheOKX : " + e.getMessage());
            return null;
        }
        righe = CcxtInterop.deduplicaBillOKX(righe);
        CcxtInterop.AbbinaLiquidStakingOnChain(righe, storicoOnChain);
        return righe;
    }

    /**
     * @param Linea una riga del documento NDJSON ({@code {ts, script, argomenti, risposta}})
     * @param StoricoOnChain dove accumulare gli ordini On-chain Earn delle risposte di {@code OKX_Earn}
     * @return i bill della risposta riconvertiti, vuota se la riga non è di uno script con bill o non è leggibile,
     *         {@code null} se la conversione è stata interrotta
     */
    static List<String[]> RigheDaRispostaOKX(String Linea, TipiOKX Tipi, JsonArray StoricoOnChain) {
        List<String[]> righe = new ArrayList<>();
        JsonObject o;
        try {
            JsonElement e = JsonParser.parseString(Linea);
            if (!e.isJsonObject()) {
                return righe;
            }
            o = e.getAsJsonObject();
        } catch (Exception troncata) {
            return righe;   //l'ultima riga di un documento troncato
        }
        String script = o.has("script") && !o.get("script").isJsonNull() ? o.get("script").getAsString() : "";
        if (!o.has("risposta") || !o.get("risposta").isJsonObject()) {
            return righe;
        }
        JsonObject r = o.getAsJsonObject("risposta");
        if (script.equals("OKX_Bills")) {
            List<String[]> f = CcxtInterop.convertOKXBills(Array(r, "okx_fundingBills"), "Funding", Tipi);
            List<String[]> t = CcxtInterop.convertOKXBills(Array(r, "okx_tradingBills"), "Trading", Tipi);
            if (f == null || t == null) {
                return null;
            }
            righe.addAll(f);
            righe.addAll(t);
        } else if (script.equals("OKX_Earn")) {
            StoricoOnChain.addAll(Array(r, "staking_storico"));
        } else if (script.equals("OKX_Archivio")) {
            List<String[]> ra = CcxtInterop.convertOKXArchivio(Array(r, "okx_archivioBills"), Tipi);
            if (ra == null) {
                return null;
            }
            //Come allo scaricamento: dell'archivio si tiene solo cio' che e' successivo alla data di partenza,
            //il resto era gia' coperto dagli scaricamenti precedenti
            String argomenti = o.has("argomenti") && !o.get("argomenti").isJsonNull() ? o.get("argomenti").getAsString() : "";
            Matcher m = START_DATE.matcher(argomenti);
            if (m.find()) {
                ra = CcxtInterop.filtraArchivioOKXPerData(ra, Long.parseLong(m.group(1)), null);
            }
            righe.addAll(ra);
        }
        return righe;
    }

    private static JsonArray Array(JsonObject o, String chiave) {
        return o.has(chiave) && o.get(chiave).isJsonArray() ? o.getAsJsonArray(chiave) : new JsonArray();
    }

    //=====================================================================================================
    //=== RECUPERO
    //=====================================================================================================
    /**
     * Importa i candidati scelti dall'utente, passando dall'import OKX vero: stessa classificazione, stesso
     * consolidamento, stessi prezzi, stessa deduplica sul {@code [24]}. Il {@code [41]} è il documento da cui
     * il candidato è stato riletto, non uno nuovo.
     * <p>Va chiamato dentro gli scope di {@link Interruzione} e {@link AttesaConnessione}, perché il consolidamento
     * scarica i prezzi. I candidati misti non vanno passati.
     *
     * @return movimenti aggiunti all'archivio
     */
    static int RecuperaOKX(List<Candidato> Scelti) {
        Map<Integer, List<String[]>> perDocumento = new LinkedHashMap<>();
        for (Candidato c : Scelti) {
            if (!c.Misto) {
                //Copie: l'import modifica sul posto le righe che riceve ([2] diventa "Principale"), e quelle del
                //candidato servono ancora intatte a NonRecuperati / Ignora, la cui chiave contiene il conto
                List<String[]> righe = perDocumento.computeIfAbsent(c.IdDocumento, k -> new ArrayList<>());
                for (String[] r : c.Righe) {
                    righe.add(r.clone());
                }
            }
        }
        int aggiunti = 0;
        for (Map.Entry<Integer, List<String[]>> e : perDocumento.entrySet()) {
            if (Interruzione.Richiesta() || AttesaConnessione.Abortita()) {
                break;
            }
            int precedente = Importazioni.DocumentoFonteCorrente;
            Importazioni.DocumentoFonteCorrente = e.getKey();
            try {
                int[] esito = Importazioni.Ex_OKX_ImportaDaAPI(e.getValue());
                DocumentiFonte.AggiornaMovimenti(e.getKey(), esito[0]);
                aggiunti += esito[0];
            } finally {
                Importazioni.DocumentoFonteCorrente = precedente;
            }
        }
        return aggiunti;
    }

    /**
     * I candidati scelti che dopo il recupero l'archivio ancora non contiene. Può succedere se l'import non scrive
     * l'unità: una categoria che il consolidamento non tratta, un ID già occupato. Senza questo controllo un'unità
     * così verrebbe riproposta a ogni scaricamento e a ogni avvio, per sempre.
     *
     * @param Scelti i candidati passati a {@link #RecuperaOKX}
     * @param Archivio la mappa dei movimenti dopo il recupero
     * @return i candidati di cui nessun bill compare nell'archivio
     */
    static List<Candidato> NonRecuperati(List<Candidato> Scelti, Map<String, String[]> Archivio) {
        Set<String> presenti = BillPresenti(Archivio);
        List<Candidato> mancanti = new ArrayList<>();
        for (Candidato c : Scelti) {
            if (c.Misto) {
                continue;
            }
            boolean trovato = false;
            for (String[] r : c.Righe) {
                if (presenti.contains(r[14].trim())) {
                    trovato = true;
                    break;
                }
            }
            if (!trovato) {
                mancanti.add(c);
            }
        }
        return mancanti;
    }

    private static String Taglia(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
