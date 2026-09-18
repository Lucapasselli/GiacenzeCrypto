package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Storico delle modifiche ai movimenti: un buffer in memoria e <b>un solo</b> punto di scrittura reale.
 *
 * <p>Le modifiche ai movimenti vivono in {@link Principale#MappaCryptoWallet}, che è solo in memoria
 * finché {@link Importazioni#Scrivi_Movimenti_Crypto} non scrive davvero {@code movimenti.crypto.db}.
 * Scrivere subito lo storico su H2 renderebbe quindi permanenti modifiche che l'utente può ancora
 * scartare chiudendo senza salvare: le voci si accodano qui e vengono riversate da
 * {@link #SalvaBuffer(java.util.Map)}, chiamata da dentro {@code Scrivi_Movimenti_Crypto} e da
 * nessun'altra parte.</p>
 *
 * <p><b>La catena è identificata dal lignaggio, non dagli ID.</b> Il campo {@link #CAMPO_LIGNAGGIO}
 * del movimento porta un identificativo casuale, scritto la prima volta che quel movimento viene
 * modificato a mano e poi trasportato verbatim da ogni modifica successiva; la data di modifica ne
 * ordina le versioni. La prima versione di questa funzionalità ricostruiva invece la catena abbinando
 * "ID consumato" e "ID prodotto", e ne pagava il prezzo: una stringa ID può morire e rinascere da un
 * reimport CSV, quindi il confine fra due catene diverse che condividono la stessa stringa era
 * ambiguo, la risalita doveva fermarsi lì con un guard dedicato, e una modifica fatta prima di un
 * cambio di ID restava agganciata a un ID non più esistente — invisibile all'utente. Con il lignaggio
 * leggere è {@code WHERE Lignaggio = ?} e cancellare è una sola {@code DELETE}.</p>
 *
 * <p><b>Il prezzo di questa scelta</b> è che il lignaggio va trasportato da ogni punto che ricostruisce
 * un movimento invece di modificarlo sul posto. Oggi lo fanno da soli {@code GUI_ModificaMovimento} e
 * {@code Principale_TraslaOrario}, tramite la copia "tutto tranne" di
 * {@link MovimentiCrypto#CampiNonCopiabiliVerbatim} (il campo {@value #CAMPO_LIGNAGGIO} non è in quella
 * blocklist proprio perché deve essere copiato), e {@code Principale_Movimenti_SeparaUnisci}, che lo
 * elenca in {@code CampiDaRiportare}. Non serve invece in {@code GUI_ClassificazioneMovimento}, che
 * muta il movimento originale sul posto e crea le contropartite come movimenti nuovi, né in
 * {@code RiportaTransazioniASituazioneIniziale}, che riscrive {@code [0]} sullo stesso array.</p>
 *
 * <p>Analisi completa (pro e contro rispetto al disegno precedente, limiti noti) in
 * {@code nocommit/Documentazione/Analisi_Storico_Modifiche_Movimenti.md}.</p>
 *
 * @author luca.passelli
 */
public class MovimentiStorico {

    /**
     * Campo della riga movimento che porta il lignaggio.
     *
     * <p>Era uno dei tre campi liberi in coda alla riga (42/43/44). È fuori da
     * {@code Calcoli_PlusvalenzeNew.Impronta()} e da {@code Backup_Restore.CAMPI_CALCOLATI}, quindi
     * timbrarlo non provoca ricalcoli di plusvalenze e non muove il golden master — la stessa proprietà
     * che rende gratuito il campo 41 (documento di origine).</p>
     */
    public static final int CAMPO_LIGNAGGIO = 42;

    /**
     * Operazione delle voci prodotte dal CASO A1 di {@code GUI_ModificaMovimento} (modifica in place:
     * quantità, valore, note, address, con l'ID che resta sé stesso).
     *
     * <p>È rimasta solo un'<b>etichetta</b> per il visualizzatore. Nella versione precedente era anche
     * un filtro nelle query di risalita, perché una voce con {@code IdVecchio == IdNuovo} veniva letta
     * come una "morte" della stringa ID e fermava la cascata; con il lignaggio quelle voci sono
     * versioni come tutte le altre.</p>
     */
    public static final String OP_IN_PLACE = "ModificaInPlace";

    private MovimentiStorico() {
    }

    /** Una voce di modifica non ancora salvata su disco. */
    private record VoceModifica(String lignaggio, String idNuovo, String idVecchio, String rigaOriginale,
            String operazione, long dataModifica) {

    }

    private static final List<VoceModifica> BufferModifiche = new ArrayList<>();

    /** Lignaggi dei movimenti cancellati in questa sessione, da ripulire al prossimo salvataggio. */
    private static final List<String> BufferCancellazioni = new ArrayList<>();

    /**
     * Restituisce il lignaggio del movimento, generandolo e <b>timbrandolo sulla riga</b> se non c'è.
     *
     * <p>Va chiamata dai punti che stanno per modificare un movimento, e solo da quelli: un movimento
     * importato e mai toccato resta con il campo vuoto, che è anche l'informazione "non è mai stato
     * modificato a mano". È idempotente — una seconda modifica dello stesso movimento riusa il
     * lignaggio già presente, ed è così che tutte le versioni finiscono nella stessa catena.</p>
     *
     * <p><b>Un lignaggio valorizzato non significa "esiste uno storico"</b>: viene timbrato prima che
     * la modifica sia confermata, e il CASO A2 di {@code GUI_ModificaMovimento} può ancora annullarla,
     * lasciando un movimento con lignaggio e zero righe. È uno stato legittimo. Per sapere se esiste
     * davvero una versione precedente si usa {@link DatabaseH2#StoricoMovimenti_Esiste(String)}, mai il
     * campo pieno.</p>
     *
     * <p>L'identificativo è un {@link UUID} casuale e non un progressivo: due archivi diversi che
     * modificano movimenti separatamente non devono poter produrre lo stesso valore, perché un
     * ripristino di backup li mette nella stessa tabella.</p>
     *
     * @param Movimento riga del movimento, modificata sul posto se il campo era vuoto
     * @return il lignaggio, mai vuoto
     */
    public static String AssicuraLignaggio(String[] Movimento) {
        if (Movimento == null || Movimento.length <= CAMPO_LIGNAGGIO) {
            return "";
        }
        if (Funzioni.noData(Movimento[CAMPO_LIGNAGGIO])) {
            Movimento[CAMPO_LIGNAGGIO] = UUID.randomUUID().toString();
        }
        return Movimento[CAMPO_LIGNAGGIO];
    }

    /**
     * Il lignaggio di un movimento vivo, letto dalla mappa.
     *
     * @param ID ID del movimento
     * @return il lignaggio, oppure stringa vuota se il movimento non esiste o non è mai stato modificato
     */
    public static String LignaggioDi(String ID) {
        String[] Movimento = ID == null ? null : Principale.MappaCryptoWallet.get(ID);
        if (Movimento == null || Movimento.length <= CAMPO_LIGNAGGIO
                || Funzioni.noData(Movimento[CAMPO_LIGNAGGIO])) {
            return "";
        }
        return Movimento[CAMPO_LIGNAGGIO];
    }

    /**
     * Accoda una modifica. {@code DataModifica} è catturata <b>qui</b>, nell'istante della chiamata, e
     * non dentro {@link #SalvaBuffer(java.util.Map)} al momento del flush: è il valore che ordina le
     * versioni dentro il lignaggio, e più modifiche fatte nella stessa sessione non salvata devono
     * conservare l'ordine in cui l'utente le ha fatte.
     *
     * @param Lignaggio lignaggio della catena, da {@link #AssicuraLignaggio(java.lang.String[])}
     * @param IdNuovo ID del movimento risultante dalla modifica
     * @param IdVecchio ID che la modifica ha consumato; uguale a {@code IdNuovo} per le voci in place.
     *        Non serve più a ricostruire la catena — la ricostruisce il lignaggio — ma resta ciò che
     *        permette di mostrare all'utente con quale ID il movimento era stato registrato
     * @param RigaOriginale la riga com'era prima della modifica, serializzata da
     *        {@link Importazioni#SerializzaRiga(java.lang.String[])}
     * @param Operazione etichetta dell'operazione ({@code "ModificaMovimento"}, {@code "TraslaOrario"},
     *        {@link #OP_IN_PLACE})
     */
    public static void AccodaModifica(String Lignaggio, String IdNuovo, String IdVecchio,
            String RigaOriginale, String Operazione) {
        if (Funzioni.noData(Lignaggio)) {
            //Senza lignaggio la voce non sarebbe leggibile né cancellabile: meglio non scriverla che
            //lasciare una riga che nessuno potrà più collegare a un movimento
            LoggerGC.logInfo("Storico movimenti: modifica di " + IdNuovo + " senza lignaggio, voce ignorata");
            return;
        }
        BufferModifiche.add(new VoceModifica(Lignaggio, IdNuovo, IdVecchio, RigaOriginale, Operazione,
                System.currentTimeMillis()));
    }

    /**
     * Accoda la cancellazione di un movimento: al prossimo salvataggio il suo lignaggio viene ripulito,
     * <b>se</b> nessun altro movimento vivo lo porta ancora.
     *
     * @param Lignaggio lignaggio del movimento cancellato, da leggere <b>prima</b> di rimuoverlo dalla
     *        mappa; una stringa vuota (movimento mai modificato) non accoda nulla
     */
    public static void AccodaCancellazione(String Lignaggio) {
        if (!Funzioni.noData(Lignaggio)) {
            BufferCancellazioni.add(Lignaggio);
        }
    }

    /**
     * Unico punto che tocca davvero {@code MOVIMENTI_STORICO}. Va chiamata da
     * {@link Importazioni#Scrivi_Movimenti_Crypto}, solo <b>dopo</b> che la scrittura del file dei
     * movimenti è andata a buon fine, e da nessun'altra parte.
     *
     * <p>Ogni voce esce dal proprio buffer solo dopo che la sua scrittura/cancellazione è riuscita: un
     * errore a metà lascia intatte le voci successive per il tentativo di salvataggio seguente, invece
     * di perderle in silenzio. È il motivo per cui i metodi di {@link DatabaseH2} usati qui
     * restituiscono {@code boolean} e non {@code void}: lo stile della classe logga l'eccezione e
     * prosegue, quindi senza un valore di ritorno la voce verrebbe rimossa comunque.</p>
     *
     * <p>Le cancellazioni girano dopo le modifiche: un movimento creato e poi cancellato nella stessa
     * sessione non salvata va scritto e subito ripulito, non lasciato a metà.</p>
     *
     * @param MovimentiVivi la mappa dei movimenti che si sta persistendo — <b>non</b> letta da
     *        {@code Principale.MappaCryptoWallet}: il controllo di vitalità deve rispondere
     *        sull'insieme che sta davvero andando su disco, che non in tutti i percorsi è la mappa
     *        globale
     */
    public static void SalvaBuffer(Map<String, String[]> MovimentiVivi) {
        for (Iterator<VoceModifica> it = BufferModifiche.iterator(); it.hasNext();) {
            VoceModifica m = it.next();
            if (!DatabaseH2.StoricoMovimenti_Scrivi(m.lignaggio(), m.idNuovo(), m.idVecchio(),
                    m.rigaOriginale(), m.operazione(), m.dataModifica())) {
                return;
            }
            it.remove();
        }
        for (Iterator<String> it = BufferCancellazioni.iterator(); it.hasNext();) {
            String Lignaggio = it.next();
            //Controllo di vitalità: separando un movimento le due gambe ereditano lo stesso lignaggio,
            //quindi cancellarne una non deve portarsi via la storia che l'altra usa ancora. La voce esce
            //comunque dal buffer: il lignaggio resterà da ripulire alla cancellazione dell'ultima gamba.
            if (!LignaggioVivo(MovimentiVivi, Lignaggio)) {
                if (!DatabaseH2.StoricoMovimenti_CancellaLignaggio(Lignaggio)) {
                    return;
                }
            }
            it.remove();
        }
    }

    /** @return {@code true} se almeno un movimento della mappa porta ancora quel lignaggio */
    private static boolean LignaggioVivo(Map<String, String[]> Movimenti, String Lignaggio) {
        if (Movimenti == null) {
            return false;
        }
        for (String[] v : Movimenti.values()) {
            if (v != null && v.length > CAMPO_LIGNAGGIO && Lignaggio.equals(v[CAMPO_LIGNAGGIO])) {
                return true;
            }
        }
        return false;
    }

    /** Svuota i buffer senza scrivere nulla: serve ai test per partire da uno stato noto. */
    static void AzzeraBuffer() {
        BufferModifiche.clear();
        BufferCancellazioni.clear();
    }

    /** @return quante voci (modifiche + cancellazioni) sono in attesa di essere salvate */
    static int VociInAttesa() {
        return BufferModifiche.size() + BufferCancellazioni.size();
    }

    /**
     * Tutte le versioni precedenti di un movimento, <b>buffer non ancora salvato compreso</b>: è quello
     * che rende lo storico visibile anche prima che l'utente prema Salva in "Transazioni Crypto", e non
     * solo dopo che {@link #SalvaBuffer(java.util.Map)} l'ha riversato su {@code MOVIMENTI_STORICO}.
     * Stesso formato di riga di {@link DatabaseH2#StoricoMovimenti_Leggi(String)}
     * ({@code {IdVecchio, DataModifica, RigaOriginale, Operazione, IdNuovo}}), unione delle due fonti e
     * riordinata per data di modifica decrescente: non si può assumere che il buffer sia sempre più
     * recente del DB, perché un salvataggio può cadere fra due modifiche fatte a mano nella stessa
     * sessione.
     *
     * @param Lignaggio lignaggio del movimento (campo {@link #CAMPO_LIGNAGGIO}); se vuoto l'elenco è vuoto
     * @return righe, mai {@code null}
     */
    public static List<String[]> Versioni(String Lignaggio) {
        List<String[]> Righe = new ArrayList<>();
        if (Funzioni.noData(Lignaggio)) {
            return Righe;
        }
        Righe.addAll(DatabaseH2.StoricoMovimenti_Leggi(Lignaggio));
        for (VoceModifica m : BufferModifiche) {
            if (Lignaggio.equals(m.lignaggio())) {
                Righe.add(new String[]{m.idVecchio(), String.valueOf(m.dataModifica()), m.rigaOriginale(),
                    m.operazione(), m.idNuovo()});
            }
        }
        Righe.sort((a, b) -> Long.compare(Long.parseLong(b[1]), Long.parseLong(a[1])));
        return Righe;
    }

    /**
     * Se di un lignaggio esiste almeno una versione precedente da mostrare, salvata su disco o ancora
     * in buffer.
     *
     * <p>È questa, e non {@code DatabaseH2.StoricoMovimenti_Esiste}, la domanda giusta per accendere il
     * pulsante "Versioni precedenti": quella guarda solo il DB, quindi una modifica appena fatta e non
     * ancora salvata lo terrebbe spento pur avendo una versione precedente da vedere.</p>
     *
     * @param Lignaggio lignaggio da cercare
     */
    public static boolean EsisteStorico(String Lignaggio) {
        return !Versioni(Lignaggio).isEmpty();
    }
}
