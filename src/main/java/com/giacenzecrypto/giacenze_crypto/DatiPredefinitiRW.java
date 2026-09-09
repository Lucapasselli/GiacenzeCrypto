package com.giacenzecrypto.giacenze_crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Dati <b>predefiniti</b> del quadro W/RW per gruppo wallet, letti da
 * {@code config/importmappe/RW_Predefiniti.json} invece che scritti nel codice.
 *
 * <p><b>Perche' esiste.</b> Fino al 2026-09-08 l'elenco degli exchange noti, i loro periodi fiscali
 * (entita' legale / Stato estero MiCA), i gruppi wallet preconfigurati {@code Wallet 101..114} e i
 * loro periodi di detenzione erano hardcoded in {@link DatabaseH2} : correggere una P.IVA o
 * aggiungere una finestra di bollo significava una nuova release. Ora sono un file, allineato dal
 * repository come le mappe causali (vedi {@link MappeCausali}) e modificabile a mano
 * nell'installazione dell'utente.</p>
 *
 * <p><b>Due posti nel file, una sola destinazione.</b> I {@code periodiFiscali} scritti sotto un
 * exchange sono <b>righi FIAT del suo gruppo wallet</b> ({@code gruppo}) : {@link #Interpreta} li
 * fonde con il blocco {@code periodiDetenzioneGruppi} e {@link #periodiDetenzioneGruppi()} li
 * restituisce insieme. Stanno sotto l'exchange solo perche' e' li' che si leggono accanto al resto
 * dei suoi dati ; dal 2026-09-09 non esiste piu' nessuna tabella dei periodi dell'exchange.</p>
 *
 * <p><b>Il reconcile tocca solo le righe {@code SISTEMA}.</b> {@link Principale_GruppiWalletRW}
 * riversa questo file nelle tabelle {@code personale.mv.db} all'avvio ({@code Pers_RW_SeminaERiconcilia})
 * confrontando l'hash del file con quello dell'ultimo giro : le righe con {@code Origine} valorizzata a
 * {@code UTENTE} — o {@code NULL} sui DB creati prima di questa feature — non vengono <b>mai</b>
 * toccate. La {@code chiave} di ogni periodo e' la sua identita' stabile fra un aggiornamento e
 * l'altro.</p>
 *
 * <p>Ripiego sulla copia inclusa nel jar sotto {@code /ImportMappe/} : {@link #Carica()} torna
 * {@code null} solo se il file manca da entrambe le parti o non e' valido, e in quel caso il
 * chiamante <b>non semina e non riconcilia</b> (le tabelle restano com'erano), come fa
 * {@link TipiOKX#Carica()}.</p>
 */
public class DatiPredefinitiRW {

    /** Nome del file in {@code config/importmappe/}, senza estensione. */
    public static final String NOME = "RW_Predefiniti";

    /** Un exchange noto : id + nome + gruppo wallet preconfigurato. */
    public static final class ExchangePredef {

        public final String id;
        public final String nome;
        public final String gruppo;

        ExchangePredef(String id, String nome, String gruppo) {
            this.id = id;
            this.nome = nome;
            this.gruppo = gruppo;
        }
    }

    /** I periodi di detenzione predefiniti di un gruppo wallet ({@code GRUPPO_PERIODO_RW}). */
    public static final class GruppoPeriodiPredef {

        public final String gruppo;
        public final List<PeriodoDetPredef> periodi;

        GruppoPeriodiPredef(String gruppo, List<PeriodoDetPredef> periodi) {
            this.gruppo = gruppo;
            this.periodi = periodi;
        }
    }

    /**
     * Un periodo di detenzione predefinito ({@code GRUPPO_PERIODO_RW}). I campi fiscali
     * ({@code statoEstero}, {@code identificativoFiscale}, {@code note}, {@code fonte}) valgono solo
     * sui righi {@code FIAT}. L'identificativo ISEE non c'e' e non ci deve essere : e' sempre inserito
     * a mano, anche sulle righe che arrivano dal programma.
     */
    public static final class PeriodoDetPredef {

        public final String chiave, tipo;
        public final int progressivo;
        public final String dataInizio, dataFine, valIniziale, notaIniziale, valFinale, notaFinale,
                calcoloIniziale, calcoloFinale, bollo,
                statoEstero, identificativoFiscale, note, fonte;

        PeriodoDetPredef(String chiave, String tipo, int progressivo, String dataInizio, String dataFine,
                String valIniziale, String notaIniziale, String valFinale, String notaFinale,
                String calcoloIniziale, String calcoloFinale, String bollo,
                String statoEstero, String identificativoFiscale, String note, String fonte) {
            this.chiave = chiave;
            this.tipo = tipo;
            this.progressivo = progressivo;
            this.dataInizio = dataInizio;
            this.dataFine = dataFine;
            this.valIniziale = valIniziale;
            this.notaIniziale = notaIniziale;
            this.valFinale = valFinale;
            this.notaFinale = notaFinale;
            this.calcoloIniziale = calcoloIniziale;
            this.calcoloFinale = calcoloFinale;
            this.bollo = bollo;
            this.statoEstero = statoEstero;
            this.identificativoFiscale = identificativoFiscale;
            this.note = note;
            this.fonte = fonte;
        }
    }

    private final String versione;
    private final List<ExchangePredef> exchange;
    private final List<GruppoPeriodiPredef> periodiDetenzioneGruppi;
    private final String hash;

    private DatiPredefinitiRW(String versione, List<ExchangePredef> exchange,
            List<GruppoPeriodiPredef> periodiDetenzioneGruppi, String hash) {
        this.versione = versione;
        this.exchange = exchange;
        this.periodiDetenzioneGruppi = periodiDetenzioneGruppi;
        this.hash = hash;
    }

    public String versione() {
        return versione;
    }

    public List<ExchangePredef> exchange() {
        return exchange;
    }

    public List<GruppoPeriodiPredef> periodiDetenzioneGruppi() {
        return periodiDetenzioneGruppi;
    }

    /** L'exchange con l'id indicato (case-insensitive), o {@code null}. */
    public ExchangePredef exchangePerId(String id) {
        if (id == null) {
            return null;
        }
        for (ExchangePredef e : exchange) {
            if (e.id.equalsIgnoreCase(id.trim())) {
                return e;
            }
        }
        return null;
    }

    /**
     * sha256 (esadecimale) del testo del file : e' la chiave con cui
     * {@code Pers_RW_SeminaERiconcilia} decide se rifare il giro di reconcile.
     */
    public String hash() {
        return hash;
    }

    /**
     * Legge il file da {@code config/importmappe/RW_Predefiniti.json}, con ripiego sulla copia nel jar.
     *
     * @return i dati predefiniti, oppure {@code null} se il file non e' disponibile ne' su disco ne'
     *         fra le risorse (o non e' valido). Il chiamante in quel caso non semina e non riconcilia.
     */
    public static DatiPredefinitiRW Carica() {
        return MappeCausali.CaricaConRipiego(NOME, DatiPredefinitiRW::Interpreta);
    }

    static DatiPredefinitiRW Interpreta(String contenuto, String nome) {
        if (contenuto == null) {
            return null;
        }
        try {
            JSONObject root = new JSONObject(contenuto);
            String versione = root.optString("versione", "");

            // I periodi di detenzione, per gruppo. Si riempiono da due punti del file : il blocco
            // "periodiDetenzioneGruppi" e i "periodiFiscali" scritti sotto ogni exchange, che sono
            // righi FIAT del gruppo preconfigurato di quell'exchange - stanno li' perche' e' li' che
            // si leggono insieme al resto dei suoi dati, non perche' siano un'altra cosa.
            Map<String, List<PeriodoDetPredef>> perGruppo = new LinkedHashMap<>();

            List<ExchangePredef> exchange = new ArrayList<>();
            JSONArray exArr = root.optJSONArray("exchange");
            if (exArr != null) {
                for (int i = 0; i < exArr.length(); i++) {
                    JSONObject ex = exArr.getJSONObject(i);
                    String id = ex.optString("id", "").trim();
                    if (id.isEmpty()) {
                        LoggerGC.ScriviErrore("DatiPredefinitiRW: exchange senza id in " + nome + ", lo ignoro");
                        continue;
                    }
                    String gruppo = ex.optString("gruppo", "").trim();
                    JSONArray pfArr = ex.optJSONArray("periodiFiscali");
                    if (pfArr != null && !gruppo.isEmpty()) {
                        List<PeriodoDetPredef> periodi =
                                perGruppo.computeIfAbsent(gruppo, k -> new ArrayList<>());
                        for (int j = 0; j < pfArr.length(); j++) {
                            JSONObject p = pfArr.getJSONObject(j);
                            periodi.add(periodoDet(p, "FIAT",
                                    id + "-fiat-" + p.optInt("progressivo", j + 1), j));
                        }
                    } else if (pfArr != null) {
                        LoggerGC.ScriviErrore("DatiPredefinitiRW: l'exchange " + id + " ha periodiFiscali "
                                + "ma nessun gruppo wallet in " + nome + ", li ignoro");
                    }
                    exchange.add(new ExchangePredef(id, ex.optString("nome", id), gruppo));
                }
            }

            JSONArray gArr = root.optJSONArray("periodiDetenzioneGruppi");
            if (gArr != null) {
                for (int i = 0; i < gArr.length(); i++) {
                    JSONObject g = gArr.getJSONObject(i);
                    String gruppo = g.optString("gruppo", "").trim();
                    if (gruppo.isEmpty()) {
                        continue;
                    }
                    List<PeriodoDetPredef> periodi =
                            perGruppo.computeIfAbsent(gruppo, k -> new ArrayList<>());
                    JSONArray pArr = g.optJSONArray("periodi");
                    if (pArr != null) {
                        for (int j = 0; j < pArr.length(); j++) {
                            JSONObject p = pArr.getJSONObject(j);
                            String tipo = p.optString("tipo", "CRYPTO").trim();
                            periodi.add(periodoDet(p, tipo, gruppo + "-" + tipo + "-" + j, j));
                        }
                    }
                }
            }

            List<GruppoPeriodiPredef> gruppi = new ArrayList<>();
            for (Map.Entry<String, List<PeriodoDetPredef>> e : perGruppo.entrySet()) {
                gruppi.add(new GruppoPeriodiPredef(e.getKey(), e.getValue()));
            }

            if (exchange.isEmpty()) {
                LoggerGC.ScriviErrore("DatiPredefinitiRW: " + nome + " non contiene nessun exchange, lo considero non valido");
                return null;
            }
            return new DatiPredefinitiRW(versione, exchange, gruppi, sha256(contenuto));
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return null;
        }
    }

    /** Un periodo di detenzione letto da un oggetto JSON, con {@code tipo} e chiave di ripiego imposti dal chiamante. */
    private static PeriodoDetPredef periodoDet(JSONObject p, String tipo, String chiaveDiRipiego, int indice) {
        return new PeriodoDetPredef(
                p.optString("chiave", chiaveDiRipiego),
                tipo,
                p.optInt("progressivo", indice + 1),
                p.optString("dataInizio", ""), p.optString("dataFine", ""),
                p.optString("valIniziale", ""), p.optString("notaIniziale", ""),
                p.optString("valFinale", ""), p.optString("notaFinale", ""),
                p.optString("calcoloIniziale", ""), p.optString("calcoloFinale", ""),
                p.optString("bollo", ""),
                p.optString("statoEstero", ""), p.optString("identificativoFiscale", ""),
                p.optString("note", ""), p.optString("fonte", ""));
    }

    private static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
