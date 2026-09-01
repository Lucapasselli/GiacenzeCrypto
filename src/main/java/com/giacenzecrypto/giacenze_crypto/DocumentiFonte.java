package com.giacenzecrypto.giacenze_crypto;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Conserva una copia dei file da cui sono stati importati i movimenti e li lega ai movimenti stessi
 * attraverso il campo {@code [41]}, che contiene l'identificativo numerico del documento di origine.
 *
 * <p>Le copie stanno in {@link VarStatiche#getCartella_DocumentiFonte()}, <b>sempre compresse in gzip</b>
 * (scelta dell'utente: i CSV degli exchange e soprattutto l'NDJSON degli scarichi API si comprimono molto,
 * e i documenti non vengono mai cancellati). Il registro è la tabella {@code DOCUMENTIFONTE} di
 * {@code personale.mv.db}: è dato dell'utente, non registro né cache.
 *
 * <p>Tre cose non ovvie, tutte e tre già costate un ragionamento:
 * <ul>
 *   <li><b>L'impronta si calcola sul contenuto originale, non sul file compresso.</b> Il formato gzip include
 *       il timestamp di modifica e un byte di sistema operativo, quindi comprimere due volte lo stesso CSV
 *       produce due file diversi: usare l'impronta del {@code .gz} distruggerebbe il riconoscimento dei
 *       reimport, su cui si basano sia il riuso dell'id sia la regola di annullamento.</li>
 *   <li><b>{@link #Apri(int)} non può aprire il file conservato.</b> Nessun programma esterno apre un
 *       {@code .csv.gz} con un doppio clic, quindi il documento viene prima scompattato in
 *       {@code Temporanei/} con il suo nome originale. Che quella cartella venga svuotata dopo 24 h è
 *       corretto: la copia estratta è di comodo, l'archivio resta il {@code .gz}.</li>
 *   <li><b>{@link #Annulla(int)} va invocato solo su una registrazione con {@code Nuovo == true}.</b>
 *       Reimportare un file già importato è precisamente il caso che aggiunge zero movimenti <i>e</i> riusa
 *       l'id esistente per via del riconoscimento per impronta: cancellarlo lascerebbe orfani i movimenti
 *       della <i>prima</i> importazione.</li>
 * </ul>
 *
 * <p>Nello stile delle estrazioni recenti: tutti i metodi sono {@code static}, la classe non ha campi Swing
 * e non conosce {@link Principale}.
 */
public class DocumentiFonte {

    /** Tipi logici, scritti nella colonna {@code Tipo} del registro. Restano logici anche se il file è {@code .gz}. */
    public static final String TIPO_CSV = "CSV";
    public static final String TIPO_JSON = "JSON";
    public static final String TIPO_NDJSON = "NDJSON";

    /** Sessioni di scarico API aperte, id documento -> flusso su cui appendere le risposte. Vedi {@link #ApriSessione}. */
    private static final Map<Integer, Writer> SessioniAperte = new HashMap<>();

    /**
     * Descrizione leggibile dell'ultima importazione avviata ({@code "Crypto.com App CSV"},
     * {@code "OKX CSV (formato storico)"}, il nome del file di configurazione per i CSV generici,
     * …) e nome del file da cui proveniva. Serve a {@link Importazioni_Resoconto} per il pulsante
     * "Invia segnalazione": dai soli righi d'errore non si capirebbe a quale import si riferiscono.
     * Volutamente <b>non</b> riazzerati in un {@code finally} (a differenza di
     * {@link Importazioni#DocumentoFonteCorrente}): il resoconto viene aperto subito dopo
     * l'import e ne legge il valore alla costruzione, non all'invio.
     */
    public static volatile String UltimaDescrizioneImport = "";
    public static volatile String UltimoFileImport = "";

    /**
     * Esito di una registrazione.
     *
     * <p>Il flag {@link #Nuovo} non è decorativo: distingue il documento appena creato da uno riconosciuto per
     * impronta e riusato, e solo il primo può essere annullato.
     */
    public static final class Registrazione {

        /** Identificativo del documento, {@code 0} se la registrazione non è riuscita (l'import prosegue senza timbrare). */
        public int Id = 0;
        /** {@code true} se il documento è stato creato ora, {@code false} se è stato riconosciuto e riusato. */
        public boolean Nuovo = false;

        public Registrazione() {
        }

        public Registrazione(int Id, boolean Nuovo) {
            this.Id = Id;
            this.Nuovo = Nuovo;
        }
    }

    /** Riga del registro, nella forma che serve all'interfaccia. */
    public static final class Documento {

        public int Id = 0;
        /** Nome del file conservato dentro {@code DocumentiFonte/}, compreso il suffisso {@code .gz} */
        public String PercorsoRelativo = "";
        /** Nome del file come lo aveva l'utente, senza {@code .gz}: è questo che va mostrato */
        public String NomeOriginale = "";
        public String Tipo = "";
        /** Da dove veniva: nome del file di configurazione, dell'exchange, dell'importatore nativo */
        public String Origine = "";
        public long DataImport = 0;
        public String Hash = "";
        /** Quanti movimenti sono stati aggiunti da questo documento (somma di tutte le importazioni che lo hanno usato) */
        public int Movimenti = 0;
    }

    //=====================================================================================================
    //=== REGISTRAZIONE
    //=====================================================================================================
    /**
     * Copia in {@code DocumentiFonte/} il file appena scelto dall'utente, comprimendolo, e lo registra.
     *
     * <p>Se un documento con la stessa impronta è già stato conservato non ne crea un secondo: ritorna l'id
     * esistente con {@code Nuovo == false}. È il caso normale di chi rilancia un import con "sovrascrivi".
     *
     * @param Origine file sorgente scelto dall'utente
     * @param Tipo uno fra {@link #TIPO_CSV}, {@link #TIPO_JSON}, {@link #TIPO_NDJSON}
     * @param DescrizioneOrigine da dove viene (nome dell'importatore nativo o del file di configurazione)
     * @return la registrazione; {@code Id == 0} se non è stato possibile conservare il documento
     */
    public static Registrazione Registra(File Origine, String Tipo, String DescrizioneOrigine) {
        if (Origine == null || !Origine.isFile()) {
            return new Registrazione();
        }
        try {
            //A1: l'impronta si calcola sul contenuto originale (vedi la nota di classe), quindi prima si legge
            //il file per l'impronta e solo dopo, se serve davvero, lo si comprime
            String impronta = ImprontaFile(Origine);

            int esistente = CercaPerImpronta(impronta);
            if (esistente > 0) {
                return new Registrazione(esistente, false);
            }

            int Id = ProssimoId();
            if (Id <= 0) {
                return new Registrazione();
            }
            String nomeOriginale = NomePulito(Origine.getName());
            String relativo = Id + "_" + nomeOriginale + ".gz";

            File destinazione = new File(CartellaDocumenti(), relativo);
            try (InputStream in = new BufferedInputStream(new FileInputStream(Origine));
                 OutputStream out = new GZIPOutputStream(new FileOutputStream(destinazione))) {
                in.transferTo(out);
            }

            if (!InserisciRiga(Id, relativo, nomeOriginale, Tipo, DescrizioneOrigine, impronta)) {
                destinazione.delete();
                return new Registrazione();
            }
            return new Registrazione(Id, true);
        } catch (Exception e) {
            System.out.println("DocumentiFonte.Registra : " + e.getMessage());
            return new Registrazione();
        }
    }

    /**
     * Come {@link #Registra} ma partendo da un contenuto in memoria: serve alle importazioni via API, dove il
     * documento su disco non esiste e va creato perché ci sia qualcosa a cui puntare.
     *
     * @param Contenuto testo da conservare
     * @param NomeFile nome da dare al documento, estensione compresa (senza {@code .gz})
     * @param Tipo uno fra {@link #TIPO_CSV}, {@link #TIPO_JSON}, {@link #TIPO_NDJSON}
     * @param DescrizioneOrigine da dove viene
     * @return la registrazione; {@code Id == 0} se non è stato possibile conservare il documento
     */
    public static Registrazione RegistraContenuto(String Contenuto, String NomeFile, String Tipo, String DescrizioneOrigine) {
        if (Contenuto == null) {
            return new Registrazione();
        }
        try {
            byte[] dati = Contenuto.getBytes(StandardCharsets.UTF_8);
            String impronta = Impronta(dati);

            int esistente = CercaPerImpronta(impronta);
            if (esistente > 0) {
                return new Registrazione(esistente, false);
            }

            int Id = ProssimoId();
            if (Id <= 0) {
                return new Registrazione();
            }
            String nomeOriginale = NomePulito(NomeFile);
            String relativo = Id + "_" + nomeOriginale + ".gz";

            File destinazione = new File(CartellaDocumenti(), relativo);
            try (OutputStream out = new GZIPOutputStream(new FileOutputStream(destinazione))) {
                out.write(dati);
            }

            if (!InserisciRiga(Id, relativo, nomeOriginale, Tipo, DescrizioneOrigine, impronta)) {
                destinazione.delete();
                return new Registrazione();
            }
            return new Registrazione(Id, true);
        } catch (Exception e) {
            System.out.println("DocumentiFonte.RegistraContenuto : " + e.getMessage());
            return new Registrazione();
        }
    }

    /**
     * Conserva il file, esegue l'importazione con il campo {@code [41]} timbrato su ogni movimento che ne
     * deriva, aggiorna il conteggio del documento e, se l'importazione non ha prodotto nulla di nuovo,
     * annulla la registrazione appena creata.
     *
     * <p>Esiste perché l'ordine delle operazioni non è banale e va identico in tutti i punti di ingresso:
     * <ol>
     *   <li>la copia esiste <b>prima</b> che l'import parta, così se l'import esplode a metà il documento
     *       resta comunque conservato;</li>
     *   <li>{@link Importazioni#DocumentoFonteCorrente} viene riazzerato in un {@code finally} che avvolge
     *       direttamente la chiamata: lo scarico da tutti gli exchange incatena più importazioni sullo stesso
     *       thread e la seconda non deve ereditare l'id della prima;</li>
     *   <li>{@code TransazioniAggiunte} si legge <b>dentro</b> il {@code finally}, perché è uno statico che
     *       l'importazione successiva sovrascriverebbe (è la stessa ragione per cui esiste
     *       {@link Importazioni.Esito});</li>
     *   <li>l'annullamento è condizionato a {@code Nuovo}: reimportare un file già importato aggiunge zero
     *       movimenti <i>e</i> riusa l'id esistente, e cancellarlo lascerebbe orfani i movimenti della prima
     *       importazione.</li>
     * </ol>
     *
     * @param Origine file scelto dall'utente
     * @param Tipo uno fra {@link #TIPO_CSV}, {@link #TIPO_JSON}
     * @param DescrizioneOrigine da dove viene (importatore nativo o file di configurazione)
     * @param Importazione l'importazione vera e propria, che ritorna il proprio esito
     * @return quello che ha ritornato {@code Importazione}
     */
    public static boolean EseguiImportDaFile(File Origine, String Tipo, String DescrizioneOrigine,
            java.util.function.BooleanSupplier Importazione) {
        UltimaDescrizioneImport = DescrizioneOrigine == null ? "" : DescrizioneOrigine;
        UltimoFileImport = Origine == null ? "" : Origine.getName();
        Registrazione R = Registra(Origine, Tipo, DescrizioneOrigine);
        int aggiunte;
        Importazioni.DocumentoFonteCorrente = R.Id;
        boolean esito;
        try {
            esito = Importazione.getAsBoolean();
        } finally {
            aggiunte = Importazioni.TransazioniAggiunte;
            Importazioni.DocumentoFonteCorrente = 0;
        }
        ChiudiRegistrazione(R, aggiunte);
        return esito;
    }

    /**
     * Chiude una registrazione a importazione finita: aggiorna il conteggio dei movimenti derivati oppure,
     * se la registrazione era nuova e non ne è derivato nulla, la annulla.
     *
     * @param R la registrazione ritornata da {@link #Registra} / {@link #RegistraContenuto} / {@link #ApriSessione}
     * @param MovimentiAggiunti movimenti effettivamente aggiunti da questa importazione
     */
    public static void ChiudiRegistrazione(Registrazione R, int MovimentiAggiunti) {
        if (R == null || R.Id <= 0) {
            return;
        }
        if (R.Nuovo && MovimentiAggiunti <= 0) {
            Annulla(R.Id);
        } else {
            AggiornaMovimenti(R.Id, MovimentiAggiunti);
        }
    }

    //=====================================================================================================
    //=== SESSIONI DI SCARICO DALLE API
    //=====================================================================================================
    /**
     * Apre una sessione di scarico da API: un solo documento NDJSON (una risposta per riga) per l'intero
     * scarico, non uno per chiamata.
     *
     * <p>La cardinalità è il motivo di questa scelta: {@code OKX_Bills} pagina a 100 record per volta,
     * {@code Binance_Trades} gira una volta per token e {@code OKX_Archivio} raccoglie fino a 21 trimestri.
     * Un documento per risposta produrrebbe centinaia di file per un solo scarico.
     *
     * <p>La riga di registro viene inserita subito, perché l'id serve a timbrare i movimenti <i>mentre</i>
     * lo scarico è in corso.
     *
     * @param DescrizioneOrigine exchange da cui si sta scaricando
     * @return l'id del documento, {@code 0} se la sessione non si è potuta aprire
     */
    public static int ApriSessione(String DescrizioneOrigine) {
        UltimaDescrizioneImport = Funzioni.noData(DescrizioneOrigine) ? "" : DescrizioneOrigine;
        UltimoFileImport = "";
        try {
            int Id = ProssimoId();
            if (Id <= 0) {
                return 0;
            }
            String nomeOriginale = NomePulito(Funzioni.noData(DescrizioneOrigine) ? "scarico" : DescrizioneOrigine)
                    + "_" + Formatta(System.currentTimeMillis(), "yyyyMMdd_HHmmss") + ".ndjson";
            String relativo = Id + "_" + nomeOriginale + ".gz";

            File destinazione = new File(CartellaDocumenti(), relativo);
            Writer w = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(destinazione)), StandardCharsets.UTF_8);

            //L'impronta resta vuota: uno scarico API è unico per definizione, non c'è nessun reimport da riconoscere
            if (!InserisciRiga(Id, relativo, nomeOriginale, TIPO_NDJSON, DescrizioneOrigine, "")) {
                w.close();
                destinazione.delete();
                return 0;
            }
            synchronized (SessioniAperte) {
                SessioniAperte.put(Id, w);
            }
            return Id;
        } catch (Exception e) {
            System.out.println("DocumentiFonte.ApriSessione : " + e.getMessage());
            return 0;
        }
    }

    /**
     * Appende una risposta alla sessione aperta.
     *
     * <p><b>Le credenziali non devono mai finire qui dentro.</b> {@code apiKey}, {@code secret} e
     * {@code passphrase} sono argomenti posizionali degli script Node: chi chiama deve passare il nome dello
     * script e i soli argomenti non segreti, mai {@code process.argv} per intero. Questo file contiene già
     * l'intera storia transazionale dell'utente; aggiungerci le chiavi API lo renderebbe un singolo punto di
     * compromissione dell'account.
     *
     * @param Id id di sessione ritornato da {@link #ApriSessione}, {@code 0} per non registrare nulla
     * @param NomeScript nome dello script Node interrogato
     * @param Argomenti argomenti <b>non segreti</b> della chiamata, già in forma leggibile
     * @param RispostaJson la risposta ricevuta
     */
    public static void AggiungiAllaSessione(int Id, String NomeScript, String Argomenti, String RispostaJson) {
        if (Id <= 0) {
            return;
        }
        Writer w;
        synchronized (SessioniAperte) {
            w = SessioniAperte.get(Id);
        }
        if (w == null) {
            return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"ts\":").append(System.currentTimeMillis());
            sb.append(",\"script\":\"").append(EscapeJson(NomeScript)).append("\"");
            sb.append(",\"argomenti\":\"").append(EscapeJson(Argomenti)).append("\"");
            //Se la risposta non è JSON (errore in chiaro, HTML di un proxy, testo troncato) la si registra
            //come stringa: una riga NDJSON malformata renderebbe illeggibile l'intero documento
            String risposta = "null";
            if (!Funzioni.noData(RispostaJson)) {
                char primo = RispostaJson.trim().charAt(0);
                risposta = (primo == '{' || primo == '[') ? RispostaJson : "\"" + EscapeJson(RispostaJson) + "\"";
            }
            sb.append(",\"risposta\":").append(risposta);
            sb.append("}\n");
            synchronized (w) {
                w.write(sb.toString());
            }
        } catch (Exception e) {
            System.out.println("DocumentiFonte.AggiungiAllaSessione : " + e.getMessage());
        }
    }

    /**
     * Appende alla sessione la risposta di una chiamata HTTP fatta a un explorer di blockchain.
     *
     * <p>Gli explorer vogliono la chiave API <b>dentro l'URL</b> ({@code &apikey=...}), quindi l'URL non può
     * essere registrato così com'è: {@link #UrlSenzaChiave} la sostituisce prima che finisca nel documento.
     *
     * @param Id id di sessione, {@code 0} per non registrare nulla
     * @param Url URL interrogato, chiave API compresa: viene oscurata qui
     * @param Risposta corpo della risposta
     */
    public static void AggiungiRispostaWeb(int Id, String Url, String Risposta) {
        if (Id <= 0) {
            return;
        }
        AggiungiAllaSessione(Id, "explorer", UrlSenzaChiave(Url), Risposta);
    }

    /** @return l'URL con il valore dei parametri di chiave API sostituito da {@code ***} */
    static String UrlSenzaChiave(String Url) {
        if (Funzioni.noData(Url)) {
            return "";
        }
        //Solo i nomi che sono davvero chiavi: un "token=USDT" è un dato del movimento, non un segreto,
        //e oscurarlo renderebbe il documento meno leggibile senza proteggere niente
        return Url.replaceAll("(?i)([?&](apikey|api_key|apikey_secret|key)=)[^&]*", "$1***");
    }

    /** Chiude il flusso della sessione. Da chiamare sempre, anche se lo scarico è fallito. */
    public static void ChiudiSessione(int Id) {
        if (Id <= 0) {
            return;
        }
        Writer w;
        synchronized (SessioniAperte) {
            w = SessioniAperte.remove(Id);
        }
        if (w == null) {
            return;
        }
        try {
            w.close();
        } catch (Exception e) {
            System.out.println("DocumentiFonte.ChiudiSessione : " + e.getMessage());
        }
    }

    //=====================================================================================================
    //=== AGGIORNAMENTO E ANNULLAMENTO
    //=====================================================================================================
    /**
     * Somma al conteggio del documento i movimenti che ne sono appena derivati.
     * <p>Somma e non assegna: un documento reimportato con "sovrascrivi" può aggiungerne altri, e il totale
     * deve restare il numero di movimenti che gli si possono attribuire.
     */
    public static void AggiornaMovimenti(int Id, int Aggiunti) {
        if (Id <= 0 || Aggiunti <= 0) {
            return;
        }
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                "UPDATE DOCUMENTIFONTE SET Movimenti = Movimenti + ? WHERE Id = ?")) {
            ps.setInt(1, Aggiunti);
            ps.setInt(2, Id);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("DocumentiFonte.AggiornaMovimenti : " + e.getMessage());
        }
    }

    /**
     * Elimina copia e riga di registro.
     *
     * <p><b>Va invocato solo su una registrazione con {@code Nuovo == true}</b>: su un documento riusato
     * cancellerebbe il file a cui puntano i movimenti di un'importazione precedente. Vedi la nota di classe.
     */
    public static void Annulla(int Id) {
        if (Id <= 0) {
            return;
        }
        ChiudiSessione(Id);
        Documento d = Leggi(Id);
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                "DELETE FROM DOCUMENTIFONTE WHERE Id = ?")) {
            ps.setInt(1, Id);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("DocumentiFonte.Annulla : " + e.getMessage());
        }
        if (d != null && !Funzioni.noData(d.PercorsoRelativo)) {
            new File(CartellaDocumenti(), d.PercorsoRelativo).delete();
        }
    }

    //=====================================================================================================
    //=== LETTURA
    //=====================================================================================================
    /** @return la riga di registro, {@code null} se l'id non esiste (più) */
    public static Documento Leggi(int Id) {
        if (Id <= 0) {
            return null;
        }
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                "SELECT Id, PercorsoRelativo, NomeOriginale, Tipo, Origine, DataImport, Hash, Movimenti "
                + "FROM DOCUMENTIFONTE WHERE Id = ?")) {
            ps.setInt(1, Id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return DaResultSet(rs);
                }
            }
        } catch (Exception e) {
            System.out.println("DocumentiFonte.Leggi : " + e.getMessage());
        }
        return null;
    }

    /** @return tutti i documenti registrati, dal più recente al più vecchio */
    public static List<Documento> Elenco() {
        List<Documento> elenco = new ArrayList<>();
        try (Statement st = DatabaseH2.connectionPersonale.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT Id, PercorsoRelativo, NomeOriginale, Tipo, Origine, DataImport, Hash, Movimenti "
                     + "FROM DOCUMENTIFONTE ORDER BY Id DESC")) {
            while (rs.next()) {
                elenco.add(DaResultSet(rs));
            }
        } catch (Exception e) {
            System.out.println("DocumentiFonte.Elenco : " + e.getMessage());
        }
        return elenco;
    }

    /** @return il file compresso conservato, {@code null} se il documento non esiste più o il file è sparito */
    public static File FileConservato(int Id) {
        Documento d = Leggi(Id);
        if (d == null || Funzioni.noData(d.PercorsoRelativo)) {
            return null;
        }
        File f = new File(CartellaDocumenti(), d.PercorsoRelativo);
        return f.isFile() ? f : null;
    }

    /**
     * Scompatta il documento in {@code Temporanei/} con il suo nome originale e ne ritorna il percorso.
     * <p>Il documento conservato è un {@code .gz}, che nessun programma esterno apre con un doppio clic.
     * La copia estratta è di comodo, e la cancellazione automatica di {@code Temporanei/} dopo 24 h la
     * riguarda legittimamente.
     *
     * @return il file scompattato, {@code null} se il documento non è disponibile
     */
    public static File EstraiPerApertura(int Id) {
        File conservato = FileConservato(Id);
        if (conservato == null) {
            return null;
        }
        Documento d = Leggi(Id);
        try {
            File cartella = new File(VarStatiche.getCartella_Temporanei());
            if (!cartella.exists()) {
                cartella.mkdirs();
            }
            File estratto = new File(cartella, Id + "_" + d.NomeOriginale);
            try (InputStream in = new GZIPInputStream(new FileInputStream(conservato));
                 OutputStream out = new FileOutputStream(estratto)) {
                in.transferTo(out);
            }
            return estratto;
        } catch (Exception e) {
            System.out.println("DocumentiFonte.EstraiPerApertura : " + e.getMessage());
            return null;
        }
    }

    /**
     * Scompatta il documento e lo apre con l'applicazione predefinita del sistema.
     * @return {@code true} se il documento è stato aperto
     */
    public static boolean Apri(int Id) {
        File estratto = EstraiPerApertura(Id);
        if (estratto == null) {
            return false;
        }
        try {
            if (!java.awt.Desktop.isDesktopSupported()) {
                return false;
            }
            java.awt.Desktop.getDesktop().open(estratto);
            return true;
        } catch (Exception e) {
            System.out.println("DocumentiFonte.Apri : " + e.getMessage());
            return false;
        }
    }

    //=====================================================================================================
    //=== IL CAMPO [41]
    //=====================================================================================================
    /**
     * Estrae l'id del documento dal campo {@code [41]} di un movimento.
     *
     * <p>Tollera un eventuale sotto-campo dopo {@code |} (il sotto-delimitatore convenzionale, già usato da
     * {@code [40]}): oggi la granularità è il solo file, ma un futuro {@code "12|487"} — documento 12, riga
     * 487 — continuerebbe a essere letto correttamente da qui.
     *
     * @param Campo41 il contenuto del campo, anche {@code null}
     * @return l'id del documento, {@code 0} se assente o non numerico
     */
    public static int IdDaCampo41(String Campo41) {
        if (Funzioni.noData(Campo41)) {
            return 0;
        }
        String s = Campo41.trim();
        int barra = s.indexOf('|');
        if (barra >= 0) {
            s = s.substring(0, barra).trim();
        }
        try {
            int id = Integer.parseInt(s);
            return id > 0 ? id : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Descrizione del documento di origine da mostrare nel dettaglio di un movimento.
     * <p>Degrada dolcemente: se il registro non ha più quell'id (backup ripristinato su un'altra
     * installazione) o il file è sparito, lo dice invece di fallire.
     *
     * @param Campo41 il contenuto del campo {@code [41]} del movimento
     * @return la descrizione, stringa vuota se il movimento non ha un documento di origine
     */
    public static String Descrizione(String Campo41) {
        int Id = IdDaCampo41(Campo41);
        if (Id <= 0) {
            return "";
        }
        Documento d = Leggi(Id);
        if (d == null) {
            return "documento " + Id + " non più disponibile";
        }
        String s = Id + " - " + d.NomeOriginale;
        if (d.DataImport > 0) {
            s = s + " (importato il " + Formatta(d.DataImport, "dd/MM/yyyy") + ")";
        }
        if (FileConservato(Id) == null) {
            s = s + " - file non più presente";
        }
        return s;
    }

    /** @return {@code true} se il movimento ha un documento di origine il cui file è ancora conservato */
    public static boolean Apribile(String Campo41) {
        return FileConservato(IdDaCampo41(Campo41)) != null;
    }

    //=====================================================================================================
    //=== RIEPILOGO DEI MOVIMENTI AGGANCIATI
    //=====================================================================================================
    /**
     * Quello che si sa di un documento guardando i movimenti che vi puntano, e che nel registro non c'è.
     *
     * <p>Il conteggio del registro ({@link Documento#Movimenti}) è storico: dice quanti movimenti quel
     * documento ha prodotto al momento dell'importazione, e resta fermo se poi l'utente ne cancella o ne
     * separa qualcuno. Questo invece è il conteggio <b>attuale</b>, ed è l'unico su cui si possa decidere
     * se un documento sia eliminabile.
     */
    public static final class Riepilogo {

        /** Movimenti attualmente in mappa che puntano a questo documento */
        public int Movimenti = 0;
        /** Wallet/exchange su cui quei movimenti si trovano (campo {@code [3]}) */
        public final java.util.TreeSet<String> Wallet = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        /** Data del movimento più vecchio, nel formato {@code yyyyMMddHHmmss} del prefisso degli ID */
        public String PrimaData = "";
        /** Data del movimento più recente, stesso formato */
        public String UltimaData = "";

        /** @return il periodo coperto nella forma {@code aaaa-mm-gg / aaaa-mm-gg}, vuoto se non ci sono movimenti */
        public String Periodo() {
            if (Funzioni.noData(PrimaData) || Funzioni.noData(UltimaData)) {
                return "";
            }
            return DataLeggibile(PrimaData) + " / " + DataLeggibile(UltimaData);
        }

        /** @return i wallet separati da virgola */
        public String WalletInRiga() {
            return String.join(", ", Wallet);
        }

        private static String DataLeggibile(String DataID) {
            return DataID.length() >= 8
                    ? DataID.substring(0, 4) + "-" + DataID.substring(4, 6) + "-" + DataID.substring(6, 8)
                    : DataID;
        }
    }

    /**
     * Riepiloga in <b>un solo passaggio</b> su {@link Principale#MappaCryptoWallet} i movimenti agganciati a
     * ciascun documento.
     *
     * <p>Un passaggio solo, e non una scansione per documento: la mappa dei movimenti è la struttura più
     * grande dell'applicazione e il pannello di gestione li elenca tutti insieme.
     *
     * <p>Le date si confrontano come <b>stringhe</b>, non come istanti: il prefisso degli ID è
     * {@code yyyyMMddHHmmss}, quindi l'ordine lessicografico è già quello cronologico — ed è lo stesso
     * ordine su cui si basa tutto il motore di calcolo. Convertirle costerebbe un parsing per movimento e,
     * sulle date non valide, una riga di log per ognuna.
     *
     * @return id documento → riepilogo, senza voci per i documenti a cui non punta nessun movimento
     */
    public static Map<Integer, Riepilogo> Riepiloghi() {
        Map<Integer, Riepilogo> mappa = new HashMap<>();
        for (String v[] : Principale.MappaCryptoWallet.values()) {
            if (v == null || v.length <= 41) {
                continue;
            }
            int Id = IdDaCampo41(v[41]);
            if (Id <= 0) {
                continue;
            }
            Riepilogo r = mappa.computeIfAbsent(Id, k -> new Riepilogo());
            r.Movimenti++;
            if (!Funzioni.noData(v[3])) {
                r.Wallet.add(v[3].trim());
            }
            String data = v[0] == null ? "" : v[0].split("_")[0];
            if (!data.isBlank()) {
                if (r.PrimaData.isEmpty() || data.compareTo(r.PrimaData) < 0) {
                    r.PrimaData = data;
                }
                if (r.UltimaData.isEmpty() || data.compareTo(r.UltimaData) > 0) {
                    r.UltimaData = data;
                }
            }
        }
        return mappa;
    }

    /**
     * Scompatta il documento nella cartella indicata, conservandone il nome originale.
     *
     * <p>Se in quella cartella esiste già un file con quel nome — due export diversi possono chiamarsi
     * entrambi {@code transactions.csv} — si antepone l'id, che è univoco per costruzione, invece di
     * sovrascrivere qualcosa che l'utente non si aspetta di perdere.
     *
     * @param Id documento da esportare
     * @param Cartella cartella di destinazione scelta dall'utente
     * @return il file scritto, {@code null} se il documento non è disponibile o la scrittura è fallita
     */
    public static File EsportaDecompresso(int Id, File Cartella) {
        File conservato = FileConservato(Id);
        if (conservato == null || Cartella == null) {
            return null;
        }
        Documento d = Leggi(Id);
        try {
            if (!Cartella.exists()) {
                Cartella.mkdirs();
            }
            File destinazione = new File(Cartella, d.NomeOriginale);
            if (destinazione.exists()) {
                destinazione = new File(Cartella, Id + "_" + d.NomeOriginale);
            }
            try (InputStream in = new GZIPInputStream(new FileInputStream(conservato));
                 OutputStream out = new FileOutputStream(destinazione)) {
                in.transferTo(out);
            }
            return destinazione;
        } catch (Exception e) {
            System.out.println("DocumentiFonte.EsportaDecompresso : " + e.getMessage());
            return null;
        }
    }

    /** @return la dimensione su disco del documento conservato (compresso), {@code 0} se il file non c'è */
    public static long Dimensione(int Id) {
        File f = FileConservato(Id);
        return f == null ? 0 : f.length();
    }

    /**
     * @param Id documento
     * @return la dimensione del documento <b>prima</b> della compressione, {@code 0} se non determinabile.
     *
     * <p>Si legge dagli ultimi 4 byte del file: il formato gzip chiude con {@code ISIZE}, la lunghezza
     * dell'originale in little-endian. Non serve quindi scompattare niente per mostrarla in un elenco —
     * cosa che su documenti da qualche megabyte renderebbe lento l'unico pannello che li elenca tutti.
     * Il campo è a 32 bit, quindi il valore è esatto solo sotto i 4 GB: nessun export di exchange ci arriva
     * neanche lontanamente, e in caso contrario il numero sarebbe comunque solo indicativo.
     */
    public static long DimensioneOriginale(int Id) {
        File f = FileConservato(Id);
        if (f == null || f.length() < 4) {
            return 0;
        }
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r")) {
            raf.seek(f.length() - 4);
            byte b[] = new byte[4];
            raf.readFully(b);
            return ((long) (b[0] & 0xFF))
                    | ((long) (b[1] & 0xFF) << 8)
                    | ((long) (b[2] & 0xFF) << 16)
                    | ((long) (b[3] & 0xFF) << 24);
        } catch (Exception e) {
            System.out.println("DocumentiFonte.DimensioneOriginale : " + e.getMessage());
            return 0;
        }
    }

    //=====================================================================================================
    //=== UTILITÀ INTERNE
    //=====================================================================================================
    /** @return la cartella dei documenti, creandola se manca */
    private static File CartellaDocumenti() {
        File cartella = new File(VarStatiche.getCartella_DocumentiFonte());
        if (!cartella.exists()) {
            cartella.mkdirs();
        }
        return cartella;
    }

    /**
     * @return il prossimo id libero, {@code 0} in caso di errore.
     *         <p>{@code MAX(Id)+1} invece di una sequenza H2: il progetto non ne usa e non c'è concorrenza,
     *         l'applicazione è a istanza singola per via del lock di H2.
     */
    private static int ProssimoId() {
        try (Statement st = DatabaseH2.connectionPersonale.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(Id),0)+1 FROM DOCUMENTIFONTE")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            System.out.println("DocumentiFonte.ProssimoId : " + e.getMessage());
        }
        return 0;
    }

    /** @return l'id del documento con quell'impronta, {@code 0} se non c'è (o se l'impronta è vuota) */
    private static int CercaPerImpronta(String Impronta) {
        if (Funzioni.noData(Impronta)) {
            return 0;
        }
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                "SELECT Id FROM DOCUMENTIFONTE WHERE Hash = ?")) {
            ps.setString(1, Impronta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            System.out.println("DocumentiFonte.CercaPerImpronta : " + e.getMessage());
        }
        return 0;
    }

    private static boolean InserisciRiga(int Id, String Relativo, String NomeOriginale, String Tipo, String Origine, String Impronta) {
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(
                "INSERT INTO DOCUMENTIFONTE (Id, PercorsoRelativo, NomeOriginale, Tipo, Origine, DataImport, Hash, Movimenti) "
                + "VALUES (?,?,?,?,?,?,?,0)")) {
            ps.setInt(1, Id);
            ps.setString(2, Relativo);
            ps.setString(3, NomeOriginale);
            ps.setString(4, Tipo == null ? "" : Tipo);
            ps.setString(5, Origine == null ? "" : Origine);
            ps.setLong(6, System.currentTimeMillis());
            ps.setString(7, Impronta == null ? "" : Impronta);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("DocumentiFonte.InserisciRiga : " + e.getMessage());
            return false;
        }
    }

    private static Documento DaResultSet(ResultSet rs) throws java.sql.SQLException {
        Documento d = new Documento();
        d.Id = rs.getInt("Id");
        d.PercorsoRelativo = Funzioni.noData(rs.getString("PercorsoRelativo")) ? "" : rs.getString("PercorsoRelativo");
        d.NomeOriginale = Funzioni.noData(rs.getString("NomeOriginale")) ? "" : rs.getString("NomeOriginale");
        d.Tipo = Funzioni.noData(rs.getString("Tipo")) ? "" : rs.getString("Tipo");
        d.Origine = Funzioni.noData(rs.getString("Origine")) ? "" : rs.getString("Origine");
        d.DataImport = rs.getLong("DataImport");
        d.Hash = Funzioni.noData(rs.getString("Hash")) ? "" : rs.getString("Hash");
        d.Movimenti = rs.getInt("Movimenti");
        return d;
    }

    /**
     * Ripulisce il nome del file da tutto ciò che non può stare in un nome sul disco.
     * <p>Il prefisso numerico che gli viene anteposto rende comunque impossibile la collisione fra due
     * {@code transactions.csv} scaricati in momenti diversi.
     */
    private static String NomePulito(String Nome) {
        if (Funzioni.noData(Nome)) {
            return "documento";
        }
        String s = Nome.trim().replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
        if (s.length() > 120) {
            s = s.substring(s.length() - 120);
        }
        return s;
    }

    /** @return il timestamp formattato secondo {@code Schema}, nel fuso locale */
    private static String Formatta(long Timestamp, String Schema) {
        return java.time.format.DateTimeFormatter.ofPattern(Schema)
                .format(java.time.Instant.ofEpochMilli(Timestamp).atZone(java.time.ZoneId.systemDefault()));
    }

    /** @return SHA-256 esadecimale del contenuto del file, letto a blocchi */
    private static String ImprontaFile(File f) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[16384];
            try (InputStream in = new BufferedInputStream(new FileInputStream(f))) {
                int letti;
                while ((letti = in.read(buffer)) > 0) {
                    md.update(buffer, 0, letti);
                }
            }
            return Esadecimale(md.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    /** @return SHA-256 esadecimale del contenuto */
    private static String Impronta(byte[] dati) throws IOException {
        try {
            return Esadecimale(MessageDigest.getInstance("SHA-256").digest(dati));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private static String Esadecimale(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) {
            sb.append(Character.forDigit((x >> 4) & 0xF, 16));
            sb.append(Character.forDigit(x & 0xF, 16));
        }
        return sb.toString();
    }

    private static String EscapeJson(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
