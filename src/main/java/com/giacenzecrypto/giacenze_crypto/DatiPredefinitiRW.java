package com.giacenzecrypto.giacenze_crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
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

    /** Un exchange noto : id + nome + gruppo wallet preconfigurato + periodi fiscali. */
    public static final class ExchangePredef {

        public final String id;
        public final String nome;
        public final String gruppo;
        public final List<PeriodoFiscalePredef> periodiFiscali;

        ExchangePredef(String id, String nome, String gruppo, List<PeriodoFiscalePredef> periodiFiscali) {
            this.id = id;
            this.nome = nome;
            this.gruppo = gruppo;
            this.periodiFiscali = periodiFiscali;
        }
    }

    /** Un periodo fiscale predefinito di un exchange ({@code EXCHANGE_PERIODO}). */
    public static final class PeriodoFiscalePredef {

        public final String chiave;
        public final int progressivo;
        public final String dataInizio, dataFine, nome, statoEstero, identificativoFiscale, note, fonte;

        PeriodoFiscalePredef(String chiave, int progressivo, String dataInizio, String dataFine, String nome,
                String statoEstero, String identificativoFiscale, String note, String fonte) {
            this.chiave = chiave;
            this.progressivo = progressivo;
            this.dataInizio = dataInizio;
            this.dataFine = dataFine;
            this.nome = nome;
            this.statoEstero = statoEstero;
            this.identificativoFiscale = identificativoFiscale;
            this.note = note;
            this.fonte = fonte;
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

    /** Un periodo di detenzione predefinito ({@code GRUPPO_PERIODO_RW}). */
    public static final class PeriodoDetPredef {

        public final String chiave, tipo;
        public final int progressivo;
        public final String dataInizio, dataFine, valIniziale, notaIniziale, valFinale, notaFinale,
                calcoloIniziale, calcoloFinale, bollo;

        PeriodoDetPredef(String chiave, String tipo, int progressivo, String dataInizio, String dataFine,
                String valIniziale, String notaIniziale, String valFinale, String notaFinale,
                String calcoloIniziale, String calcoloFinale, String bollo) {
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
                    List<PeriodoFiscalePredef> pf = new ArrayList<>();
                    JSONArray pfArr = ex.optJSONArray("periodiFiscali");
                    if (pfArr != null) {
                        for (int j = 0; j < pfArr.length(); j++) {
                            JSONObject p = pfArr.getJSONObject(j);
                            pf.add(new PeriodoFiscalePredef(
                                    p.optString("chiave", id + "-" + p.optInt("progressivo", j + 1)),
                                    p.optInt("progressivo", j + 1),
                                    p.optString("dataInizio", ""), p.optString("dataFine", ""),
                                    p.optString("nome", ex.optString("nome", id)),
                                    p.optString("statoEstero", ""), p.optString("identificativoFiscale", ""),
                                    p.optString("note", ""), p.optString("fonte", "")));
                        }
                    }
                    exchange.add(new ExchangePredef(id, ex.optString("nome", id),
                            ex.optString("gruppo", "").trim(), pf));
                }
            }

            List<GruppoPeriodiPredef> gruppi = new ArrayList<>();
            JSONArray gArr = root.optJSONArray("periodiDetenzioneGruppi");
            if (gArr != null) {
                for (int i = 0; i < gArr.length(); i++) {
                    JSONObject g = gArr.getJSONObject(i);
                    String gruppo = g.optString("gruppo", "").trim();
                    if (gruppo.isEmpty()) {
                        continue;
                    }
                    List<PeriodoDetPredef> periodi = new ArrayList<>();
                    JSONArray pArr = g.optJSONArray("periodi");
                    if (pArr != null) {
                        for (int j = 0; j < pArr.length(); j++) {
                            JSONObject p = pArr.getJSONObject(j);
                            periodi.add(new PeriodoDetPredef(
                                    p.optString("chiave", gruppo + "-" + p.optString("tipo", "CRYPTO") + "-" + j),
                                    p.optString("tipo", "CRYPTO").trim(),
                                    p.optInt("progressivo", j + 1),
                                    p.optString("dataInizio", ""), p.optString("dataFine", ""),
                                    p.optString("valIniziale", ""), p.optString("notaIniziale", ""),
                                    p.optString("valFinale", ""), p.optString("notaFinale", ""),
                                    p.optString("calcoloIniziale", ""), p.optString("calcoloFinale", ""),
                                    p.optString("bollo", "")));
                        }
                    }
                    gruppi.add(new GruppoPeriodiPredef(gruppo, periodi));
                }
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
