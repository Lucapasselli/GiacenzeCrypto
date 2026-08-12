package com.giacenzecrypto.giacenze_crypto;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Verifica che un archivio di backup sia ancora ripristinabile sulla versione corrente del programma.
 *
 * <h2>Perché non si confronta il numero di versione</h2>
 *
 * Sapere che un backup è stato fatto con la {@code 1.0.58.03} e che si sta girando la {@code 1.0.61.00}
 * non dice nulla di utile: fra le due possono non essere cambiati né i movimenti né le tabelle, oppure
 * può essere cambiato tutto. Quello che conta è la <b>forma dei dati</b>, e il manifest la porta con sé:
 *
 * <ul>
 *   <li>{@code colonneTabella} — il valore di {@link Importazioni#ColonneTabella} di allora. Un backup
 *       con <b>meno</b> colonne si ripristina benissimo, perché le righe corte vengono riempite da
 *       {@link Importazioni#RiempiVuotiArray(String[])} esattamente come già succede caricando un file
 *       di movimenti vecchio. Un backup con <b>più</b> colonne no: quelle in eccesso andrebbero perse
 *       in silenzio, e nessuno saprebbe cosa contenevano.</li>
 *   <li>l'elenco delle <b>colonne salvate per ogni tabella</b>. Al ripristino le colonne si riassociano
 *       per nome, quindi una colonna aggiunta dopo resta al proprio default e non fa danni; una colonna
 *       <i>sparita</i> dalla versione attuale viene scartata, ed è un avviso — a meno che non faccia
 *       parte della chiave primaria, nel qual caso le righe finirebbero sovrapposte fra loro e il
 *       ripristino va rifiutato.</li>
 * </ul>
 *
 * Il controllo gira <b>prima</b> che venga toccato un solo dato: è tutto derivabile dal manifest e dai
 * metadati delle tabelle, quindi non c'è ragione di scoprire un'incompatibilità a metà lavoro.
 */
public class Backup_Compatibilita {

    /** I tre esiti possibili. */
    public enum Esito {
        /** Ripristinabile senza riserve. */
        COMPATIBILE,
        /** Ripristinabile, ma qualcosa verrà scartato o lasciato al default. */
        CON_AVVISI,
        /** Da non ripristinare. */
        INCOMPATIBILE
    }

    /** Il verdetto completo, con le ragioni. */
    public static final class Verdetto {

        public Esito Esito = Backup_Compatibilita.Esito.COMPATIBILE;
        /** Motivi per cui il ripristino è rifiutato. */
        public final List<String> Errori = new ArrayList<>();
        /** Cose che l'utente deve sapere ma che non impediscono il ripristino. */
        public final List<String> Avvisi = new ArrayList<>();

        /** @return {@code true} se il ripristino può procedere */
        public boolean Ripristinabile() {
            return Esito != Backup_Compatibilita.Esito.INCOMPATIBILE;
        }

        /** @return errori e avvisi in un unico testo, già formattato in HTML per i dialoghi */
        public String Descrizione() {
            StringBuilder sb = new StringBuilder();
            for (String e : Errori) {
                sb.append("&bull; ").append(e).append("<br>");
            }
            if (!Errori.isEmpty() && !Avvisi.isEmpty()) {
                sb.append("<br>");
            }
            for (String a : Avvisi) {
                sb.append("&bull; ").append(a).append("<br>");
            }
            return sb.toString();
        }
    }

    /**
     * Le colonne che, se mancassero nella versione attuale, renderebbero ambigue le righe ripristinate.
     *
     * <p>Sono le chiavi primarie delle tabelle salvate. Scartare una colonna qualsiasi costa un valore di
     * default; scartare una colonna di chiave significa che due righe distinte del backup diventano la
     * stessa riga, e l'{@code INSERT} fallisce o — peggio — la seconda sovrascrive la prima.
     */
    private static final Map<String, String[]> CHIAVI = Map.ofEntries(
            Map.entry("personale/OPZIONI", new String[]{"OPZIONE"}),
            Map.entry("personale/PrezziNew", new String[]{"TIMESTAMP", "EXCHANGE", "SYMBOL", "RETE", "ADDRESS"}),
            Map.entry("personale/XXXEUR", new String[]{"DATASIMBOLO"}),
            Map.entry("personale/Prezzo_ora_Address_Chain", new String[]{"ORA_ADDRESS_CHAIN"}),
            Map.entry("personale/WALLETS", new String[]{"WALLET_RETE"}),
            Map.entry("personale/WALLETGRUPPO", new String[]{"WALLET"}),
            Map.entry("personale/GRUPPO_ALIAS", new String[]{"GRUPPO"}),
            Map.entry("personale/EMONEY", new String[]{"MONETA"}),
            Map.entry("personale/GIACENZEBLOCKCHAIN", new String[]{"WALLET_BLOCCO"}),
            Map.entry("personale/EXCHANGETOKENS", new String[]{"EXCHANGE_TOKEN"}),
            Map.entry("personale/DOCUMENTIFONTE", new String[]{"ID"}),
            Map.entry("personale/EXCHANGEAPI", new String[]{"NOME"}),
            Map.entry("principale/OPZIONI", new String[]{"OPZIONE"}),
            Map.entry("principale/RINOMINATOKEN", new String[]{"ADDRESS_CHAIN"}),
            Map.entry("principale/PROVIDERDEFI", new String[]{"RETE"}),
            Map.entry("prezzi/PrezziKO", new String[]{"TIMESTAMP", "SYMBOL", "RETE", "ADDRESS"}));

    /**
     * Verifica un archivio contro la build in esecuzione.
     *
     * @param m il manifest dell'archivio, come letto da {@link Backup_Restore#LeggiManifest(java.io.File)}
     * @return il verdetto, mai {@code null}
     */
    public static Verdetto Verifica(Backup_Restore.Manifest m) {
        Verdetto v = new Verdetto();
        if (m == null) {
            v.Esito = Esito.INCOMPATIBILE;
            v.Errori.add("L'archivio non contiene un manifest leggibile : potrebbe essere danneggiato "
                    + "o non essere un backup di questa applicazione.");
            return v;
        }

        //--- formato dell'archivio
        if (m.FormatoBackup <= 0) {
            v.Esito = Esito.INCOMPATIBILE;
            v.Errori.add("Il manifest non dichiara un formato di archivio valido.");
            return v;
        }
        if (m.FormatoBackup > Backup_Restore.FORMATO_BACKUP) {
            v.Esito = Esito.INCOMPATIBILE;
            v.Errori.add("L'archivio è in formato " + m.FormatoBackup + ", questa versione arriva al "
                    + Backup_Restore.FORMATO_BACKUP + " : è stato prodotto da una versione più recente "
                    + "del programma. Aggiorna Giacenze Crypto e riprova.");
            return v;
        }

        //--- forma della riga di movimento
        if (m.ColonneTabella > 0 && m.ColonneTabella > Importazioni.ColonneTabella) {
            v.Esito = Esito.INCOMPATIBILE;
            v.Errori.add("I movimenti del backup hanno " + m.ColonneTabella + " campi, questa versione ne "
                    + "gestisce " + Importazioni.ColonneTabella + " : i campi in più andrebbero persi "
                    + "senza che nessuno possa dire cosa contenevano.");
        } else if (m.ColonneTabella > 0 && m.ColonneTabella < Importazioni.ColonneTabella) {
            v.Avvisi.add("I movimenti del backup hanno " + m.ColonneTabella + " campi contro gli attuali "
                    + Importazioni.ColonneTabella + " : i campi mancanti verranno lasciati vuoti, come "
                    + "avviene caricando un archivio prodotto da una versione precedente.");
        }

        //--- forma di ogni tabella salvata
        for (Map.Entry<String, Backup_Restore.TabellaSalvata> e : m.Tabelle.entrySet()) {
            ConfrontaTabella(v, e.getKey(), e.getValue());
        }

        if (!v.Errori.isEmpty()) {
            v.Esito = Esito.INCOMPATIBILE;
        } else if (!v.Avvisi.isEmpty()) {
            v.Esito = Esito.CON_AVVISI;
        }
        return v;
    }

    /** Confronta le colonne salvate di una tabella con quelle che la tabella ha adesso. */
    private static void ConfrontaTabella(Verdetto v, String Chiave, Backup_Restore.TabellaSalvata ts) {
        Connection c = ConnessioneDi(Chiave);
        String nome = NomeDi(Chiave);
        if (c == null || nome.isEmpty()) {
            v.Avvisi.add("La tabella " + Chiave + " del backup non esiste più in questa versione : "
                    + "verrà saltata (" + ts.Righe + " righe non ripristinate).");
            return;
        }
        List<String> attuali = Backup_Restore.ColonneDi(c, nome);
        if (attuali.isEmpty()) {
            v.Avvisi.add("La tabella " + Chiave + " del backup non esiste più in questa versione : "
                    + "verrà saltata (" + ts.Righe + " righe non ripristinate).");
            return;
        }

        List<String> mancanti = new ArrayList<>();
        for (String col : ts.Colonne) {
            boolean trovata = false;
            for (String a : attuali) {
                if (a.equalsIgnoreCase(col)) {
                    trovata = true;
                    break;
                }
            }
            if (!trovata) {
                mancanti.add(col);
            }
        }
        if (mancanti.isEmpty()) {
            return;
        }

        //Una colonna di chiave primaria che sparisce non è un avviso: senza di essa righe distinte del
        //backup diventano indistinguibili fra loro
        List<String> chiaviPerse = new ArrayList<>();
        String[] chiavi = CHIAVI.get(Chiave);
        if (chiavi != null) {
            for (String k : chiavi) {
                for (String m : mancanti) {
                    if (k.equalsIgnoreCase(m)) {
                        chiaviPerse.add(m);
                    }
                }
            }
        }
        if (!chiaviPerse.isEmpty()) {
            v.Errori.add("Nella tabella " + Chiave + " il backup contiene le colonne di chiave "
                    + String.join(", ", chiaviPerse) + ", che questa versione non ha più : le righe "
                    + "ripristinate si sovrapporrebbero fra loro.");
        } else {
            v.Avvisi.add("Nella tabella " + Chiave + " le colonne " + String.join(", ", mancanti)
                    + " non esistono più e verranno scartate.");
        }
    }

    /** @return la connessione del database indicato dal prefisso della chiave, {@code null} se ignoto */
    private static Connection ConnessioneDi(String Chiave) {
        if (Chiave.startsWith("personale/")) {
            return DatabaseH2.connectionPersonale;
        }
        if (Chiave.startsWith("principale/")) {
            return DatabaseH2.connection;
        }
        if (Chiave.startsWith("prezzi/")) {
            return DatabaseH2.connectionPrezzi;
        }
        return null;
    }

    /** @return il nome della tabella dalla chiave {@code database/TABELLA} */
    private static String NomeDi(String Chiave) {
        int i = Chiave.indexOf('/');
        return i < 0 ? "" : Chiave.substring(i + 1);
    }
}
