package com.giacenzecrypto.giacenze_crypto;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;

/**
 * Tabella di decodifica dei codici {@code type} dei bill restituiti dalle API di OKX, letta da
 * {@code config/importmappe/OKX_Tipi.json} invece che scritta nel codice.
 *
 * <p><b>Perche' esiste.</b> OKX non pubblica la tabella dei codici: ogni volta che un utente segnala un
 * movimento non riconosciuto, o una classificazione sbagliata, la correzione era una nuova versione del
 * programma. Ora e' un file, allineato dal repository come le mappe causali (vedi {@link MappeCausali}) e
 * modificabile a mano nell'installazione dell'utente.
 *
 * <p><b>La catena resta in due passi</b>, e non va accorciata: questa tabella traduce il codice numerico
 * nella stessa <i>etichetta testuale</i> che comparirebbe nella colonna "Type"/"Action" dell'export CSV
 * ({@code "Buy"}, {@code "deposit"}, {@code "Transfer in"}, ...), e solo dopo {@code OKX.json} converte
 * quell'etichetta nella categoria interna. E' cosi' che import da CSV e import da API condividono un unico
 * classificatore; mettere qui direttamente la categoria ne creerebbe un secondo da tenere allineato.
 *
 * <p><b>Cio' che resta nel codice, deliberatamente.</b> Il ripiego per i codici non elencati
 * ({@code "OKX type <n> (<notes>)"}) non e' configurabile: e' l'unico modo in cui un codice nuovo si fa
 * vedere, perche' {@code Importazioni.Ex_OKX_SoloCausaliSconosciute} costruisce proprio da li' il
 * riepilogo dei movimenti da decodificare. Un file che riuscisse a "mappare tutto" spegnerebbe la sola
 * segnalazione disponibile.
 *
 * <p>Il ragionamento sul significato dei singoli codici — come sono stati riconosciuti, perche' un
 * giroconto non si importa, perche' il {@code type} 30 e' uno scambio — sta in {@code CLAUDE.md} e nel
 * javadoc di {@link CcxtInterop#causaleBillOKX}: qui c'e' solo il meccanismo.
 *
 * <p><b>Costo di lettura.</b> Come {@link MappeCausali} non c'e' cache, cosi' un file corretto a mano ha
 * effetto al primo import successivo senza riavviare. La tabella va pero' caricata <b>una volta per
 * scaricamento</b> e passata al ciclo: {@code causaleBillOKX} viene chiamata una volta per bill, e
 * {@code OKX_Bills.js} arriva a 50.000 bill per conto.
 *
 * @author luca.passelli
 */
public class TipiOKX {

    /** Nome del file in {@code config/importmappe/}, senza estensione */
    public static final String NOME = "OKX_Tipi";

    /**
     * Valore di una voce della tabella: l'etichetta da usare a seconda del verso del movimento.
     * <p>Un'etichetta fissa nel JSON (una semplice stringa) diventa una voce con {@code positivo} e
     * {@code negativo} uguali: il verso non entra nella decisione, ma la forma resta una sola.
     */
    static final class Voce {

        final String positivo;
        final String negativo;

        Voce(String positivo, String negativo) {
            this.positivo = positivo;
            this.negativo = negativo;
        }

        /** @param balChg variazione di saldo del bill; il segno sceglie fra le due etichette */
        String Etichetta(String balChg) {
            return Funzioni.isNegativo(balChg) ? negativo : positivo;
        }
    }

    private final Map<String, Voce> trading;
    private final Map<String, Voce> funding;
    private final Map<String, String> archivioInstType;
    private final Map<String, String> archivioSubType;

    private TipiOKX(Map<String, Voce> trading, Map<String, Voce> funding,
            Map<String, String> archivioInstType, Map<String, String> archivioSubType) {
        this.trading = trading;
        this.funding = funding;
        this.archivioInstType = archivioInstType;
        this.archivioSubType = archivioSubType;
    }

    /**
     * Legge la tabella da {@code config/importmappe/OKX_Tipi.json}, con ripiego sulla copia inclusa nel jar.
     *
     * @return la tabella, oppure {@code null} se il file non e' disponibile ne' su disco ne' fra le risorse.
     *         Il chiamante <b>deve</b> interrompere l'importazione in quel caso: proseguire con una tabella
     *         vuota renderebbe sconosciuto ogni movimento OKX, che e' un modo silenzioso di non importare
     *         nulla. Il messaggio da mostrare e' {@link MappeCausali#MessaggioMappaNonDisponibile}.
     */
    public static TipiOKX Carica() {
        return MappeCausali.CaricaConRipiego(NOME, TipiOKX::Interpreta);
    }

    /**
     * Traduce il codice {@code type} di un bill nell'etichetta testuale corrispondente.
     *
     * @param tipo valore del campo {@code type} del bill
     * @param balChg variazione di saldo, da cui si ricava il verso del movimento
     * @param isTrading {@code true} se il bill viene dal conto Trading, {@code false} dal Funding
     * @param notes campo {@code notes} del bill, che sul Funding porta l'etichetta in chiaro; puo' essere
     *              vuoto o {@code null}
     * @return l'etichetta, oppure {@code "OKX type <n>"} (con {@code notes} fra parentesi se presente) se il
     *         codice non e' fra quelli elencati nel file
     */
    public String Causale(String tipo, String balChg, boolean isTrading, String notes) {
        String chiave = tipo == null ? "" : tipo.trim();
        Voce voce = (isTrading ? trading : funding).get(chiave);
        if (voce != null) {
            return voce.Etichetta(balChg);
        }
        //Non mappato: finira' fra i movimenti sconosciuti, con il codice grezzo e la descrizione di OKX
        String sconosciuto = "OKX type " + tipo;
        if (notes != null && !notes.isBlank()) {
            sconosciuto = sconosciuto + " (" + notes.trim() + ")";
        }
        return sconosciuto;
    }

    /**
     * Traduce la coppia {@code instType}/{@code subType} del CSV dell'archivio trimestrale nel codice
     * {@code type} dei bill JSON, cosi' che la classificazione resti governata da {@link #Causale} e non
     * esista un secondo classificatore da tenere allineato.
     *
     * <p>{@code instType} ha la precedenza su {@code subType} e viene confrontato senza distinguere
     * maiuscole e minuscole; i codici numerici del {@code subType} sono confrontati dopo un {@code trim()}.
     *
     * @param instType valore della colonna {@code instType} ({@code "SPOT"} oppure {@code "-"})
     * @param subType valore della colonna {@code subType}
     * @return il codice {@code type} corrispondente, oppure stringa vuota se la combinazione non e' elencata,
     *         cosi' che il movimento finisca fra quelli sconosciuti invece di essere classificato a caso
     */
    public String TipoDaArchivio(String instType, String subType) {
        if (instType != null) {
            String tipo = archivioInstType.get(instType.trim());
            if (tipo != null) return tipo;
        }
        if (subType == null) return "";
        String tipo = archivioSubType.get(subType.trim());
        return tipo == null ? "" : tipo;
    }

    /**
     * @return tutte le etichette che la tabella puo' emettere, senza ripetizioni. Serve al test che verifica
     *         che ognuna sia una chiave di {@code OKX.json}: un refuso in un'etichetta modificata a mano
     *         trasformerebbe un codice riconosciuto in un movimento sconosciuto, in silenzio.
     */
    public Collection<String> EtichetteEmesse() {
        Collection<String> etichette = new LinkedHashSet<>();
        for (Voce v : trading.values()) {
            etichette.add(v.positivo);
            etichette.add(v.negativo);
        }
        for (Voce v : funding.values()) {
            etichette.add(v.positivo);
            etichette.add(v.negativo);
        }
        return etichette;
    }

    /**
     * Converte il contenuto del file nella tabella.
     *
     * @param contenuto testo JSON del file
     * @param nome nome della tabella, solo per i messaggi di errore
     * @return la tabella, oppure {@code null} se il JSON non e' valido o i due blocchi dei conti sono
     *         entrambi vuoti (indistinguibile da un file corrotto, e renderebbe sconosciuto ogni movimento)
     */
    static TipiOKX Interpreta(String contenuto, String nome) {
        if (contenuto == null) {
            return null;
        }
        try {
            JSONObject root = new JSONObject(contenuto);
            Map<String, Voce> trading = LeggiConti(root, "trading");
            Map<String, Voce> funding = LeggiConti(root, "funding");
            if (trading.isEmpty() && funding.isEmpty()) {
                LoggerGC.ScriviErrore("TipiOKX: la tabella " + nome + " non contiene nessun codice type,"
                        + " la considero non valida");
                return null;
            }
            //instType e' testuale ("SPOT"), quindi confronto senza distinzione di maiuscole come le mappe
            //causali; il subType e' numerico e la distinzione non lo riguarda.
            Map<String, String> instType = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            instType.putAll(LeggiTesti(root, "archivioInstType"));
            Map<String, String> subType = new TreeMap<>();
            subType.putAll(LeggiTesti(root, "archivioSubType"));
            return new TipiOKX(trading, funding, instType, subType);
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
            return null;
        }
    }

    /** Legge un blocco di conto ({@code trading} / {@code funding}), accettando entrambe le forme del valore */
    private static Map<String, Voce> LeggiConti(JSONObject root, String blocco) {
        Map<String, Voce> mappa = new TreeMap<>();
        if (!root.has(blocco)) return mappa;
        JSONObject obj = root.getJSONObject(blocco);
        for (String chiave : obj.keySet()) {
            Object valore = obj.get(chiave);
            if (valore instanceof JSONObject verso) {
                //Forma a due etichette: l'etichetta dipende dal segno di balChg
                String positivo = verso.optString("positivo", "");
                String negativo = verso.optString("negativo", "");
                if (positivo.isBlank() || negativo.isBlank()) {
                    LoggerGC.ScriviErrore("TipiOKX: il codice " + chiave + " del blocco " + blocco
                            + " ha una sola delle due etichette positivo/negativo, lo ignoro");
                    continue;
                }
                mappa.put(chiave.trim(), new Voce(positivo, negativo));
            } else {
                //Forma a etichetta fissa: il verso non entra nella decisione
                String etichetta = valore == null ? "" : valore.toString();
                if (etichetta.isBlank()) {
                    LoggerGC.ScriviErrore("TipiOKX: il codice " + chiave + " del blocco " + blocco
                            + " ha un'etichetta vuota, lo ignoro");
                    continue;
                }
                mappa.put(chiave.trim(), new Voce(etichetta, etichetta));
            }
        }
        return mappa;
    }

    /**
     * Legge un blocco stringa → stringa, restituendo una mappa vuota se il blocco manca.
     * <p>Una voce malformata viene scartata da sola, come in {@link #LeggiConti}: il file è pensato per
     * essere corretto a mano, e un refuso in una riga dell'archivio non deve buttare via anche le
     * correzioni fatte sui codici {@code type}.
     */
    private static Map<String, String> LeggiTesti(JSONObject root, String blocco) {
        Map<String, String> mappa = new TreeMap<>();
        if (!root.has(blocco)) return mappa;
        JSONObject obj = root.getJSONObject(blocco);
        for (String chiave : obj.keySet()) {
            String valore = obj.optString(chiave, "").trim();
            if (valore.isEmpty()) {
                LoggerGC.ScriviErrore("TipiOKX: la voce " + chiave + " del blocco " + blocco
                        + " non ha un valore utilizzabile, la ignoro");
                continue;
            }
            mappa.put(chiave.trim(), valore);
        }
        return mappa;
    }
}
