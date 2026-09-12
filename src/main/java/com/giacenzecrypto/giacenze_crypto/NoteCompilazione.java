package com.giacenzecrypto.giacenze_crypto;

import java.util.Map;
import org.json.JSONObject;

/**
 * Testi delle <b>note di compilazione</b> stampate in coda ai quadri W/RW e T/RT, letti da
 * {@code config/importmappe/NoteCompilazione.json} invece che scritti nel codice.
 *
 * <p><b>Perche' esiste.</b> Sono istruzioni fiscali: cambiano quando cambia la modulistica, e finche'
 * stavano dentro {@code Principale} e {@code Stampe} correggere una riga sbagliata voleva dire
 * distribuire una nuova versione del programma. Il file segue la stessa strada delle mappe causali e
 * di {@link TipiOKX}: e' incluso nel jar come copia di default, installato al primo avvio da
 * {@link MappeCausali#InstallaDefaultSeMancanti()} e riallineato a ogni apertura da
 * {@link Funzioni#AggiornamentoConfigDaRepositoryUnicaChiamata}. Una nota sbagliata si corregge quindi
 * nel repository e arriva a tutti al riavvio successivo, senza una release.
 *
 * <p><b>Gli anni sono segnaposto, non cifre.</b> Le note del rigo W8/RW8 citano l'anno d'imposta e
 * quello precedente ("credito che risulta dalla dichiarazione relativa ai redditi <i>2024</i>",
 * "acconti ... e l'anno <i>2025</i>"). Scritti come numeri, il file sarebbe da riscrivere ogni gennaio
 * e l'utente non potrebbe aggiustarlo da se': per questo valgono {@code {anno}}, {@code {annoPrec}} e
 * {@code {annoSucc}}, risolti al momento della stampa. Restano invece cifre vere i riferimenti a una
 * regola (l'entrata in vigore del regime nel 2023, gli anni delle minusvalenze riportabili): quelli non
 * seguono l'anno della dichiarazione.
 *
 * <p><b>Una nota per anno, quando serve.</b> La modulistica cambia ogni anno e una stessa istruzione
 * puo' valere per un'annualita' e non per quella dopo. Oltre alla chiave nuda ogni nota puo' quindi
 * avere delle varianti datate, {@code W8.2024}, {@code T.2025}: <b>una variante vale dall'anno che porta
 * in avanti</b>, finche' non ne compare una piu' recente. La chiave nuda e' il ripiego, cioe' cio' che
 * vale prima della prima variante — {@link #ANNO_PRIMA_VARIANTE} e' il punto oltre il quale non si cerca,
 * perche' il regime delle cripto-attivita' comincia nel 2023 e per gli anni precedenti il programma
 * avverte gia' che il report puo' non essere corretto.
 *
 * <p>Il ripiego e' a cascata, invece che «anno esatto o niente», perche' altrimenti la chiave nuda
 * dovrebbe essere due cose insieme: il testo degli anni <i>piu' vecchi</i> della prima variante e quello
 * degli anni <i>piu' recenti</i> dell'ultima. Con la cascata la chiave nuda e' solo la prima, l'ultima
 * variante copre da se' gli anni futuri, e soprattutto <b>non si duplica niente</b>: le annualita' in cui
 * l'istruzione non e' cambiata non compaiono affatto nel file.
 *
 * <p><b>Cosa non e' finito qui, deliberatamente.</b> Il blocco "OPZIONI SCELTE PER IL CALCOLO" non e'
 * una nota ma un resoconto: si compone a seconda delle caselle spuntate e dell'elenco degli E-Money
 * token dell'utente. Spostarlo vorrebbe dire mettere nel file dei frammenti che senza il codice non
 * significano nulla.
 *
 * <p><b>Ripiego.</b> {@link MappeCausali#CaricaConRipiego} prova prima il file su disco e poi la copia
 * nel jar, cosi' un file corretto a mano e andato storto non impedisce di stampare. Se anche quella
 * manca, o manca la chiave, {@link #Testo} restituisce un avviso <b>visibile nel report</b> invece di
 * una stringa vuota: una nota assente in silenzio da un documento fiscale e' peggio di una nota che
 * dichiara di mancare. Non c'e' cache, come per {@link MappeCausali}: un file corretto a mano ha
 * effetto alla stampa successiva senza riavviare.
 *
 * @author luca.passelli
 */
public class NoteCompilazione {

    /** Nome del file in {@code config/importmappe/}, senza estensione */
    public static final String NOME = "NoteCompilazione";

    // ─── Chiavi delle note. Vanno tenute allineate al file JSON. ───────────────────────────────
    public static final String W8 = "W8";
    public static final String W = "W";
    public static final String RW8 = "RW8";
    public static final String RW = "RW";
    public static final String FIAT = "FIAT";
    public static final String FIAT_LOOKUP_FALLITI = "FIAT_LOOKUP_FALLITI";
    public static final String FIAT_CONTI_CORRENTI = "FIAT_CONTI_CORRENTI";
    public static final String FIAT_IVAFE_LIQUIDITA = "FIAT_IVAFE_LIQUIDITA";
    public static final String T = "T";
    public static final String RT = "RT";

    /**
     * Anno oltre il quale non si cercano varianti datate. E' l'anno di entrata in vigore del regime delle
     * cripto-attivita' : prima di quello il programma avverte gia' che il report puo' non essere corretto
     * (vedi l'avviso "Anno pre 2023" in {@code Principale}), quindi non ha senso mantenere un testo per
     * ciascuna di quelle annualita'.
     */
    public static final int ANNO_PRIMA_VARIANTE = 2023;

    private NoteCompilazione() {
    }

    /**
     * Il testo HTML della nota indicata, con i segnaposto dell'anno gia' risolti.
     *
     * @param chiave una delle costanti di questa classe
     * @param anno anno d'imposta, come stringa (es. {@code "2025"})
     * @return il markup HTML da passare a {@link Stampe#AggiungiHtml(String)}
     */
    public static String Testo(String chiave, String anno) {
        return Testo(chiave, anno, null);
    }

    /**
     * Come {@link #Testo(String, String)}, con in piu' dei segnaposto specifici della nota (importi
     * calcolati, elenchi di righi): nel file compaiono come {@code {nome}}.
     *
     * @param extra segnaposto aggiuntivi, {@code nome → valore}; {@code null} se non ce ne sono
     */
    public static String Testo(String chiave, String anno, Map<String, String> extra) {
        String html = Leggi(chiave, anno);
        if (html == null) {
            LoggerGC.ScriviErrore("NoteCompilazione: nota '" + chiave + "' non disponibile, ne' in "
                    + VarStatiche.getCartella_ConfigImportMappe() + NOME + ".json né fra le risorse del programma");
            return "<html><font size=\"2\" face=\"Courier New,Courier, mono\" ><b>ATTENZIONE :</b> "
                    + "il testo delle note di compilazione (" + chiave + ") non e' disponibile in questa "
                    + "installazione. Il file " + NOME + ".json non e' stato trovato o non contiene questa voce. "
                    + "Riavviare il programma con la connessione a internet attiva per riscaricarlo."
                    + "</font></html>";
        }
        html = SostituisciAnni(html, anno);
        if (extra != null) {
            for (Map.Entry<String, String> e : extra.entrySet()) {
                html = html.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
            }
        }
        return html;
    }

    /**
     * Risolve {@code {anno}}, {@code {annoPrec}} e {@code {annoSucc}}. Un anno non numerico lascia i
     * segnaposto com'erano: meglio un testo con un segnaposto visibile che uno con un anno inventato.
     */
    static String SostituisciAnni(String html, String anno) {
        int n;
        try {
            n = Integer.parseInt(anno.trim());
        } catch (RuntimeException ex) {
            return html;
        }
        return html.replace("{anno}", String.valueOf(n))
                .replace("{annoPrec}", String.valueOf(n - 1))
                .replace("{annoSucc}", String.valueOf(n + 1));
    }

    /**
     * Il testo grezzo della nota, dal file su disco o dalla copia nel jar. Prende la variante datata piu'
     * recente che non superi l'anno richiesto ({@code chiave.2025}, {@code chiave.2024}, ...) e ripiega
     * sulla chiave nuda. {@code null} se non c'e' nessuna delle due.
     */
    private static String Leggi(String chiave, String anno) {
        Map<String, String> note = MappeCausali.CaricaConRipiego(NOME, NoteCompilazione::Interpreta);
        if (note == null) {
            return null;
        }
        return note.get(ChiaveApplicabile(note.keySet(), chiave, anno));
    }

    /**
     * La chiave da usare per quell'anno: la variante datata piu' recente che non lo superi, altrimenti la
     * chiave nuda. Separata da {@link #Leggi} perche' e' l'unica regola non ovvia del meccanismo e va
     * potuta verificare senza passare dal file.
     *
     * @param disponibili le chiavi presenti nel file
     * @param anno anno d'imposta; se non e' un numero vale la chiave nuda — meglio il testo generico che
     *        una variante scelta a caso
     */
    static String ChiaveApplicabile(java.util.Set<String> disponibili, String chiave, String anno) {
        int n;
        try {
            n = Integer.parseInt(anno.trim());
        } catch (RuntimeException ex) {
            return chiave;
        }
        for (int a = n; a >= ANNO_PRIMA_VARIANTE; a--) {
            String datata = chiave + "." + a;
            if (disponibili.contains(datata)) {
                return datata;
            }
        }
        return chiave;
    }

    /**
     * Interpreta il JSON: un oggetto {@code note} con {@code chiave → testo}. Il testo puo' essere una
     * stringa unica o un array di righe, che vengono unite con un ritorno a capo — negli editor un
     * array e' molto piu' leggibile di una riga sola da 4 KB, e l'HTML non cambia.
     */
    static Map<String, String> Interpreta(String contenuto, String nome) {
        if (contenuto == null || contenuto.isBlank()) {
            return null;
        }
        try {
            JSONObject note = new JSONObject(contenuto).getJSONObject("note");
            Map<String, String> mappa = new java.util.HashMap<>();
            for (String chiave : note.keySet()) {
                Object v = note.get(chiave);
                if (v instanceof org.json.JSONArray a) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < a.length(); i++) {
                        if (i > 0) sb.append('\n');
                        sb.append(a.getString(i));
                    }
                    mappa.put(chiave, sb.toString());
                } else {
                    mappa.put(chiave, String.valueOf(v));
                }
            }
            return mappa.isEmpty() ? null : mappa;
        } catch (RuntimeException ex) {
            LoggerGC.ScriviErrore("NoteCompilazione: " + nome + ".json non interpretabile : " + ex);
            return null;
        }
    }
}
