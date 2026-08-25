/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author luca.passelli
 */
public class Calcoli_PlusvalenzeNew {
    
    /**
     *
     * @param movimento
     * @return  Funzione che si occupa di categorizzare le transazioni in 10 tipologie
     * che corrispondono poi a 10 calcoli diversi per Plusvalenza, Lifo e costi di carico
     * di ritorno a questa funzione viene tornato un numero che identifica la categoria<br>
     * Queste sono le categorie:<br>
     * <br>
     *      1 - Scambio tra Criptoattività Omogenee<br>
     *           Comprende:  NFT -> NFT<br>
     *                       Crypto -> Crypto     <br>
     *       2 - Scambio tra Criptoattività non omogenee<br>
     *           Comprende:  Crypto -> NFT<br>
     *                       NFT -> Crypto       <br>
     *       3 - Acquisto Criptoattività<br>
     *           Comprende:  FIAT -> NFT<br>
     *                       FIAT -> Crypto         <br>
     *       4 - Vendita Criptoattività<br>
     *           Comprende:  NFT -> FIAT<br>
     *                       Crypto -> FIAT          <br>       
     *       5 - Deposito Criptoattività x spostamento tra wallet<br>
     *           Comprende:  -> NFT          (Tipologia TI su IDTrans oppure Tipologia Vuota o DTW su quella della Transazione)<br>
     *                       -> Crypto       (Tipologia TI su IDTrans oppure Tipologia Vuota o DTW su quella della Transazione) <br>                        
     *       6 - Prelievo Criptoattività x spostamento tra wallet<br>
     *           Comprende:  NFT ->          (Tipologia TI su IDTrans oppure Tipologia Vuota o PTW su quella della Transazione)<br>
     *                       Crypto ->       (Tipologia TI su IDTrans oppure Tipologia Vuota o PTW su quella della Transazione) <br>
     *       7 - Deposito Criptoattività x rewards, stacking,cashback etc...<br>
     *           Comprende:  -> NFT          (Tipologia RW su IDTrans oppure Tipologia DAI su quella della Transazione)<br>
     *                       -> Crypto       (Tipologia RW su IDTrans oppure Tipologia DAI su quella della Transazione)<br>
     *       8 - Prelievo Criptoattività x servizi, acquisto beni etc...<br>
     *           Comprende:  NFT ->          (Tipologia CM su IDTrans oppure Tipologia PCO su quella della Transazione)<br>
     *                       Crypto ->       (Tipologia CM su IDTrans oppure Tipologia PCO su quella della Transazione)<br>
     *       9 - Deposito Criptoattività DCZ         (Deposito a costo di carico zero)<br>
     *           Comprende:  -> NFT          (Tipologia DCZ su quella della Transazione)<br>
     *                       -> Crypto       (Tipologia DCZ su quella della Transazione)<br>
     *       10 - Prelievo Criptoattività PWN        (Prelievo a plusvalenza Zero ma toglie dal Lifo) <br>
     *           Comprende:  NFT ->          (Tipologia PWN su quella della Transazione)<br>
     *                       Crypto ->       (Tipologia PWN su quella della Transazione)<br>
     *       11 - Deposito FIAT
     *           Comprende:    -> FIAT
     */
    
 
    
    
    /**
     *
     * @param CryptoStack
     * @param Tipologia
     * @return  In base alla Tipologia di movimento<br>
     * ritorna la tipologia di plusvalenza, costo di carico, rimonozione dallo stack LIFO, inserimento nello stack Lifo<br>
     * in un array di int<br><br>
     * dove:<br>
     * int[0]=Tipologia Plusvalenza<br>
     * int[1]=Tipologia Calcolo Costo di Carico<br>
     * int[2]=Tipologia Eliminazione vecchio costo di carico da stack LIFO<br>
     * int[3]=Tipologia inserimento nuovo costo di carico da stack LIFO<br><br><br>
     * Con la seguente logica:<br>
     * <br>
     * int[0]=0 :   Il campo plusvalenza va compilato con valore Zero <br>
     * int[0]=1 :   Il campo plusvalenza va compilato con il ValoreTransazione<br>
     * int[0]=2 :   Plusvalenza=Valore Transazione - Costo di Carico Moneta Uscita (Vecchio Costo di carico)<br><br>
     * int[1]=0 :   Il campo relativo al Nuovo Costo di Carico va valorizzato a Zero <br>
     * int[1]=1 :   Nuovo Costo di Carico= "" <br>
     * int[1]=2 :   Nuovo Costo di Carico = Costo di Carico preso tramite lifo da moneta Uscita (Vecchio costo di carico)<br>
     * int[1]=3 :   Nuovo Costo di Carico = Valore Transazione<br><br>
     * int[2]=0 :   Non tolgo dallo stack il vecchio costo di carico <br>
     * int[2]=1 :   Tolgo dallo stack il vecchio costo di carico<br><br>
     * int[3]=0 :   Costo Lifo Moneta Entrante = Zero <br><
     * int[3]=1 :   Non faccio nulla (non inserisco nessun valore)<br>
     * int[3]=2 :   Costo Lifo Moneta Entrante = Costo Lifo Moneta Uscente<br>
     * int[3]=3 :   Costo Lifo Moneta Entrante = Valore Transazione<br>
     * int[4]=0 :   Vecchio Costo di Carico=0<br>
     * int[4]=1 :   Vecchio Costo di carico=""<br>
     * int[4]=2 :   Vecchio Costo di carico=preso da lifo moneta<br>
     */
    
    
/**
 * Stato LIFO per ID transazione, ripopolato da ogni {@link #AggiornaPlusvalenze()}.
 * È una {@code ConcurrentHashMap} e non una {@code TreeMap} perché viene letta dall'EDT
 * (tooltip di {@link JTableConTooltipIcone}, {@link GUI_LiFoTransazione}) mentre un thread
 * di background può essere dentro il ricalcolo che la svuota e la ripopola: {@code get} non
 * controlla il modCount, quindi non solleva eccezione ma può attraversare un albero in fase di
 * ribilanciamento e restituire un risultato errato. È una difesa del solo lettore: la corsa fra
 * due ricalcoli è già chiusa dal {@code synchronized} su {@link #AggiornaPlusvalenze()}, e a
 * differenza di quella non è dimostrabile con un test deterministico.
 * <p>
 * La perdita dell'ordinamento è qui senza effetti, ed è stato verificato voce per voce:
 * il campo è {@code private} e non compare in nessun altro file, dentro la classe è usato
 * soltanto con {@code get} / {@code computeIfAbsent} / {@code clear} e non viene mai
 * iterato (nessun keySet/values/entrySet, nessun metodo di SortedMap). L'unico accesso
 * esterno è {@link #getIDLiFo(String)}, che restituisce un singolo elemento per chiave.
 * <b>Non è una sostituzione da replicare sulle altre mappe</b>: in {@code MappaCryptoWallet},
 * per esempio, l'ordine della TreeMap È l'ordine cronologico con cui il motore elabora i
 * movimenti, e sostituirla romperebbe il LIFO.
 */
private static final Map<String, LifoXID> MappaIDTrans_LifoxID = new ConcurrentHashMap<>();

/**
 * Opzione personale che disattiva il ricalcolo incrementale ("NO" = sempre passata completa).
 * <p>
 * Esiste come assicurazione, non come impostazione da usare tutti i giorni: se un numero risultasse
 * mai sospetto, spegnerla separa in un clic "è il calcolo" da "è l'incrementale". Su
 * un'applicazione fiscale è l'unica forma di bisezione disponibile a chi non può leggere il codice.
 */
public static final String OPZIONE_INCREMENTALE = "Plusvalenze_RicalcoloIncrementale";

/**
 * Ogni quanti movimenti elaborati viene salvato un checkpoint dello stato LIFO.
 * <p>
 * Il valore <b>non</b> è a fine trimestre, che sarebbe la scelta istintiva: i movimenti reali sono
 * distribuiti malissimo (48.282 su 101.103 in un solo trimestre nel dataset di riferimento), e un
 * checkpoint trimestrale lascerebbe fino a 48.000 movimenti da rielaborare. A blocchi fissi il
 * lavoro di ripartenza è limitato a un blocco: 46-91 ms misurati, contro ~1.000 della passata
 * completa. Una copia costa 0,31 ms e i 21 checkpoint di quel dataset occupano ~2,3 MB.
 */
static int MOVIMENTI_PER_CHECKPOINT = 5000;

/**
 * Quanti movimenti ha rielaborato l'ultima passata, e se è stata completa. Servono al messaggio di
 * log e ai test: senza un modo di osservare che il percorso incrementale sia stato davvero preso,
 * un test di equivalenza passerebbe anche se ogni passata fosse completa.
 */
static int UltimaPassata_Elaborati = 0;
static boolean UltimaPassata_Completa = true;

private static final long SEME_IMPRONTA = 0xcbf29ce484222325L;
private static final long PRIMO_IMPRONTA = 0x100000001b3L;

/**
 * Impronte dei movimenti come erano a fine ultima passata. {@code null} significa "nessuno stato
 * valido": la prossima passata sarà completa. Vedi {@link #InvalidaStatoIncrementale()}.
 */
private static Map<String, Long> ImpronteUltimaPassata = null;

/** Valore di {@link OpzioniRicalcolo#Epoca()} all'inizio dell'ultima passata. */
private static long EpocaUltimaPassata = 0;

/**
 * Stato LIFO fotografato ogni {@link #MOVIMENTI_PER_CHECKPOINT} movimenti, con per chiave l'ID del
 * movimento <b>prima</b> del quale la fotografia è stata presa. Ordinato come
 * {@code MappaCryptoWallet}, così {@code floorEntry} trova il punto di ripartenza utilizzabile.
 */
private static final NavigableMap<String, Map<String, Map<String, ArrayDeque<String[]>>>> Checkpoint =
        new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

/**
 * Ritorna lo stato LIFO (stack entrato/uscito) registrato per una transazione, popolato
 * durante l'ultima esecuzione di {@link #AggiornaPlusvalenze()}. Usato dalla GUI di dettaglio
 * transazione per mostrare quali lotti sono stati movimentati dal LIFO.
 *
 * @param id ID della transazione
 * @return il {@link LifoXID} della transazione, o {@code null} se non ancora calcolato
 */
public static LifoXID getIDLiFo(String id){
    return MappaIDTrans_LifoxID.get(id);
}

/**
 * Se attivo, il ricalcolo registra in {@link LifoXID} anche i due stack "completi"
 * ({@code StackEntratoPreMovimento} e {@code StackUscitoRimanenze}), cioè lo stato dell'intero
 * stack LIFO della moneta prima e dopo ogni movimento. Normalmente è {@code false}.
 * <p>
 * Quei due stack non entrano in nessun calcolo: servono soltanto alla maschera di dettaglio
 * {@link GUI_LiFoTransazione}, che li mostra un movimento alla volta e su richiesta esplicita
 * dell'utente. Registrarli sempre costava una copia dell'intero stack della moneta per ogni
 * movimento elaborato — O(movimenti × profondità dello stack) in allocazioni a ogni ricalcolo,
 * e altrettanta memoria trattenuta per tutta la sessione, dato che {@code MappaIDTrans_LifoxID}
 * conserva un {@link LifoXID} per movimento.
 * <p>
 * Il flag è uno stato del motore e non un parametro di {@link #AggiornaPlusvalenze()} di
 * proposito: finché la maschera di dettaglio è aperta anche un ricalcolo lanciato da altro
 * (import, rimozione SCAM di massa, modifica di un movimento) deve continuare a produrre i
 * dettagli, altrimenti la finestra si troverebbe con le tabelle vuote. Ed è per lo stesso
 * motivo che i dettagli restano prodotti <i>dentro</i> il ricalcolo invece che da una funzione
 * separata: non esiste un secondo percorso di calcolo che possa divergere da quello reale.
 */
private static volatile boolean DettagliLifoCompleti = false;

/**
 * Attiva o disattiva la registrazione degli stack LIFO completi (vedi
 * {@link #DettagliLifoCompleti}). Attivarlo non popola nulla da solo: i dettagli compaiono al
 * primo ricalcolo successivo, per cui chi li vuole subito usa
 * {@link #AggiornaPlusvalenzeConDettagliLifo()}. Disattivandolo i dettagli già registrati
 * vengono liberati immediatamente, senza bisogno di un altro ricalcolo.
 *
 * @param Attivi {@code true} per registrare i dettagli completi dai ricalcoli successivi
 */
public static void ImpostaDettagliLifoCompleti(boolean Attivi) {
    DettagliLifoCompleti = Attivi;
    if (!Attivi) {
        //Libero subito le copie già in memoria: sono la parte pesante di MappaIDTrans_LifoxID
        for (LifoXID lifo : MappaIDTrans_LifoxID.values()) {
            lifo.StackEntratoPreMovimento = new ArrayDeque<>();
            lifo.StackUscitoRimanenze = new ArrayDeque<>();
        }
    }
}

/**
 * @return {@code true} se il motore sta registrando gli stack LIFO completi
 */
public static boolean isDettagliLifoCompleti() {
    return DettagliLifoCompleti;
}

/**
 * Attiva i dettagli LIFO completi e ricalcola, così che {@link #getIDLiFo(String)} li
 * restituisca per ogni movimento. Da chiamare all'apertura di {@link GUI_LiFoTransazione} (o di
 * qualunque altra funzione che debba mostrare gli stack completi), ricordando di richiamare
 * {@link #ImpostaDettagliLifoCompleti(boolean)} con {@code false} quando non servono più.
 */
public static void AggiornaPlusvalenzeConDettagliLifo() {
    ImpostaDettagliLifoCompleti(true);
    AggiornaPlusvalenze();
}

/*
    ------ ELENCO NUOVE TIPOLOGIE E CARATTERISTICHE ------
C-VC -> VENDITA CRYPTO
C-AC -> ACQUISTO CRYPTO
C-RW-CB -> CASHBACK
C-RW-EN -> EARN
C-RW-RG -> REWARD GENERICA
C-RW-AD -> AIRDROP
C-RW-SR -> STAKING REWARDS
C-DC-DN -> DEPOSITO DI DONAZIONE
C-DC-TW -> TRASFERIMENTO TRA WALLET (ex.DTW)
C-DC-RW -> da finire di compilare, ma verrà fatto in un secondo momento e poi è tutto da implementare




//PWN -> Trasf. su wallet morto...tolto dal lifo (prelievo)
    //PCO -> Cashout o similare (prelievo)
    //PTW -> Trasferimento tra Wallet (prelievo)
    //PSC -> Scambio Crypto Differito (Scambio crypto non simultaneo ma differito nel tempo) (Non Utilizzato per ora)
    //DTW -> Trasferimento tra Wallet (deposito)
    //DAI -> Airdrop o similare (deposito)
    //DCZ -> Costo di carico 0 (deposito)
    //DAC -> Acquisto Crypto (deposito)  
    //DDO -> Donazioni (deposito)  
*/
 
    /*  FLAG DI ANOMALIA NEL CAMPO 38 (cumulativi, una lettera per tipo di problema):
        "A" -> giacenza LIFO insufficiente (movimento in ingresso mancante)
        "E" -> campo numerico non valido nel movimento (quantità o valore non numerici,
               trattati come zero nel calcolo invece di interrompere il ricalcolo)
        "M" -> ID transazione malformato (meno di 5 segmenti: il tipo movimento non è
               ricavabile dall'ID, la classificazione si basa solo sul campo 18)
        Le lettere si sommano (es. "AE") e ogni reset rimuove solo la propria lettera,
        così i tipi di segnalazione non si sovrascrivono a vicenda.
        Il conteggio/filtro "LiFo mancante" in Principale considera solo la "A". */

    //Aggiunge la lettera di anomalia indicata al campo 38 senza toccare le altre
    private static void AggiungiFlagAnomalia(String[] mov, String lettera) {
        if (mov != null && !mov[38].contains(lettera)) mov[38] = mov[38] + lettera;
    }

    //C2: parsing sicuro dei campi numerici provenienti dai dati importati.
    //Un valore vuoto o non numerico viene trattato come zero invece di interrompere
    //l'intero ricalcolo con una NumberFormatException (che lascerebbe la mappa
    //movimenti aggiornata solo in parte); il caso non numerico viene segnalato
    //nel log e con la lettera "E" nel campo 38 del movimento.
    static BigDecimal toBigDecimalSicuro(String Valore, String IDTransazione) {
        if (Valore == null || Valore.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(Valore.trim());
        } catch (NumberFormatException ex) {
            LoggerGC.ScriviErrore("Valore non numerico \"" + Valore + "\" nel movimento " + IDTransazione + ": considerato 0 nel calcolo plusvalenze");
            AggiungiFlagAnomalia(MappaCryptoWallet.get(IDTransazione), "E");
            return BigDecimal.ZERO;
        }
    }

/**
 * Estrae dallo stack LIFO di {@code Moneta} i lotti necessari a coprire {@code Qta}, partendo
 * dall'ultimo entrato (LIFO), e ne calcola il costo di carico complessivo. Se un singolo lotto
 * copre solo parzialmente la quantità richiesta, il lotto viene consumato per intero e si
 * prosegue con quello precedente; se un lotto è più grande del necessario viene rimesso in
 * stack per la parte residua (proporzionando il costo). Se lo stack si esaurisce prima di aver
 * coperto tutta {@code Qta} il movimento viene marcato con l'anomalia "A" (giacenza LIFO
 * insufficiente) nel campo 38, salvo il caso di token SCAM. Quantità o moneta vuote, o quantità
 * non numerica, non hanno alcun effetto sul LIFO (quest'ultimo caso viene segnalato con "E").
 *
 * @param CryptoStack mappa moneta → stack LIFO dei lotti (moneta, quantità, costo, IDTransazione origine) del gruppo wallet corrente
 * @param Moneta simbolo della criptoattività da cui estrarre
 * @param Qta quantità da estrarre (il segno non è rilevante, viene usato il valore assoluto)
 * @param toglidaStack se {@code true} lo stack viene effettivamente modificato e lo storico LIFO della transazione registrato in {@link LifoXID}; se {@code false} si opera su un clone (sola lettura/simulazione) e non si segnala alcuna anomalia
 * @param IDTransazione ID della transazione corrente, usato per il log, l'anomalia e per indicizzare {@link #MappaIDTrans_LifoxID}
 * @return il costo di carico complessivo estratto, come stringa decimale (scala {@code VarStatiche.DecimaliPlus}); stringa vuota se {@code Moneta} o {@code Qta} sono vuoti o {@code Qta} non è numerica
 */
public static String StackLIFO_TogliQta(Map<String, ArrayDeque<String[]>> CryptoStack, String Moneta,String Qta,boolean toglidaStack,String IDTransazione) {
    
    LifoXID lifoID=MappaIDTrans_LifoxID.computeIfAbsent(IDTransazione, k -> new LifoXID());
   // lifoID.StackEntrato.push(valori);
   String mov[]=MappaCryptoWallet.get(IDTransazione);
   //mov[38]="";
   
    //Se la qta o la moneta sono vuoti non ritorno nulla, quei campi devono essere obbligatoriamente valorizzati
    if (Moneta.isBlank() || Qta.isBlank()) return "";

    //C2: una quantità non numerica viene trattata come i campi vuoti (nessun effetto sul LIFO)
    //invece di far crashare l'intero ricalcolo; il movimento viene segnalato con la "E" nel campo 38
    BigDecimal qtaRichiesta;
    try {
        qtaRichiesta = new BigDecimal(Qta.trim());
    } catch (NumberFormatException ex) {
        LoggerGC.ScriviErrore("Quantità non numerica \"" + Qta + "\" nel movimento " + IDTransazione + ": movimento senza effetto sul LIFO");
        AggiungiFlagAnomalia(mov, "E");
        return "";
    }

    //Se lo stack è vuoto salvo l'errore e ritorno 0.00 come costo di carico
    ArrayDeque<String[]> originalStack = CryptoStack.get(Moneta);
    if (originalStack == null) 
    {
       originalStack=new ArrayDeque<>();
       // return "0.00";
    }
    // Se non devo togliere dallo stack originale, lo clono
    ArrayDeque<String[]> stack = toglidaStack ? originalStack : originalStack.clone();

    BigDecimal qtaRimanente = qtaRichiesta.abs();
    BigDecimal costoTransazione = BigDecimal.ZERO;


while (qtaRimanente.compareTo(BigDecimal.ZERO) > 0 && !stack.isEmpty()) {
        String[] ultimoRecupero = stack.pop();
        BigDecimal qtaEstratta = toBigDecimalSicuro(ultimoRecupero[1], IDTransazione).abs();
        BigDecimal costoEstratto = toBigDecimalSicuro(ultimoRecupero[2], IDTransazione).abs();

        if (qtaEstratta.compareTo(qtaRimanente) <= 0) {
            // Caso semplice: uso tutta la quantità
            //imposto il nuovo valore su qtarimanente che è uguale a qtarimanente-qtaestratta
            qtaRimanente = qtaRimanente.subtract(qtaEstratta);
            //recupero il valore di quella transazione e la aggiungo al costoTransazione
            costoTransazione = costoTransazione.add(costoEstratto);
            
            //Inserisco nello stack lifo della transazione i dati relativi alla moneta uscente
            //per riproporli poi nella maschera di dettaglio del Lifo
            if (toglidaStack){
            String valoriDaTogliere[]=new String[4];
            valoriDaTogliere[0]=Moneta;
            valoriDaTogliere[1]=qtaEstratta.abs().toPlainString();
            valoriDaTogliere[2]=costoEstratto.toPlainString();
            valoriDaTogliere[3]=ultimoRecupero[3];
            lifoID.StackUscito.addLast(valoriDaTogliere);//lo inserisco in coda allo stack (devo ordinarli inversamente)
            }
        } else {
            // Caso in cui la quantità richiesta è inferiore a quella in stack
            //in quersto caso dove la qta estratta dallo stack è maggiore di quella richiesta devo fare dei calcoli ovvero
            //recuperare il prezzo della sola qta richiesta e aggiungerla al costo di transazione totale
            //recuperare il prezzo della qta rimanente e la qta rimanente e riaggiungerla allo stack
            //non ho più qta rimanente
                        
            BigDecimal qtaRimanenteStack = qtaEstratta.subtract(qtaRimanente);

            BigDecimal costoUnitario = costoEstratto
                .divide(qtaEstratta, VarStatiche.DecimaliCalcoli + 10, RoundingMode.HALF_UP);

            BigDecimal valoreRimanenteStack = costoUnitario
                .multiply(qtaRimanenteStack)
                //.setScale(2, RoundingMode.HALF_UP)
                .setScale(VarStatiche.DecimaliCalcoli, RoundingMode.HALF_UP)
                .stripTrailingZeros();

            String[] valori = new String[] {
                Moneta,
                qtaRimanenteStack.toPlainString(),
                valoreRimanenteStack.toPlainString(),
                ultimoRecupero[3]
            };
           // lifoID.StackEntrato.push(valori);
            stack.push(valori);

            BigDecimal valoreUsato = costoEstratto.subtract(valoreRimanenteStack);
            costoTransazione = costoTransazione.add(valoreUsato);

           
            //Questa cosa la faccio solo se il flag toglidastack è attivo il che significa solo se è un movimento
            //che realmente movimenta lo stack
            if (toglidaStack){
            //Inserisco nello stack lifo della transazione i dati relativi alla moneta uscente
            //per riproporli poi nella maschera di dettaglio del Lifo
            String valoriDaTogliere[]=new String[4];
            valoriDaTogliere[0]=Moneta;                                         //Moneta di riferimento
            valoriDaTogliere[1]=qtaRimanente.abs().toPlainString();             //qta tolta dallo stack
            valoriDaTogliere[2]=valoreUsato.toPlainString();                    //costo della qta tolra
            valoriDaTogliere[3]=ultimoRecupero[3];                              //ID della Transazione
            lifoID.StackUscito.addLast(valoriDaTogliere);//lo inserisco in coda allo stack (devo ordinarli inversamente)
            //Stack Uscito Rimanenze sono appunto quello che rimane delle stack dopo il movimento
           // lifoID.StackUscitoRimanenze=stack.clone();
            }
             qtaRimanente = BigDecimal.ZERO;
           // 
        }
    }
    //La copia dell'intero stack residuo serve solo alla maschera di dettaglio: la registro
    //soltanto se i dettagli completi sono stati richiesti (vedi DettagliLifoCompleti)
    if (toglidaStack && DettagliLifoCompleti) {
        lifoID.StackUscitoRimanenze = stack.clone();
    }

    if (qtaRimanente.compareTo(BigDecimal.ZERO) > 0 && stack.isEmpty()) {
//Adesso verifico se sono rimaste ancora parti da togliere di cui non ho però l'equivalente nello stack e lo segnalo
        String valoriDaTogliere[] = new String[4];
        valoriDaTogliere[0] = Moneta;                                         //Moneta di riferimento
        valoriDaTogliere[1] = qtaRimanente.abs().toPlainString();             //qta tolta dallo stack
        valoriDaTogliere[2] = "0";                                            //costo della qta tolra
        valoriDaTogliere[3] = "";                                             //ID della Transazione
        lifoID.StackUscito.addLast(valoriDaTogliere);//lo inserisco in coda allo stack (devo ordinarli inversamente)

//Segnalo l'errore anche direttamente sul movimento ma solo se questo non è scam
        //A2: mov può essere null se l'ID non è (più) presente nella mappa movimenti:
        //in quel caso non c'è nessun movimento su cui segnalare l'anomalia
        //System.out.println("Errore");
        if (mov != null) {
            if (!Funzioni.isSCAM(Moneta))
            {
               // System.out.println("Lifomancante : "+mov[1]);
                AggiungiFlagAnomalia(mov, "A");
            }else{
                mov[38]=mov[38].replace("A", "");
            }
        }
       // System.out.println("Errore "+IDTransazione);
    }else if (mov!=null&&!mov[38].isBlank()){
        //Segnalo che il movimento non ha mancanze nel LiFo (le altre lettere restano)
        mov[38]=mov[38].replace("A", "");
    }

    return costoTransazione.setScale(VarStatiche.DecimaliPlus, RoundingMode.HALF_UP).toPlainString();
   // return costoTransazione.setScale(4, RoundingMode.HALF_UP).toPlainString();
}       
 
 
    
/**
 * Inserisce (push) un nuovo lotto in cima allo stack LIFO di {@code Moneta}, registrando anche
 * lo stato pre-movimento e il lotto stesso in {@link LifoXID} per la transazione corrente, ai
 * fini della visualizzazione di dettaglio. Una quantità vuota o non numerica non viene inserita
 * nel LIFO (nessun lotto aggiunto) e la transazione viene segnalata con l'anomalia "E".
 *
 * @param CryptoStack mappa moneta → stack LIFO dei lotti del gruppo wallet corrente, modificata in place
 * @param Moneta simbolo della criptoattività del lotto in ingresso
 * @param Qta quantità del lotto (il segno non è rilevante, viene usato il valore assoluto)
 * @param Valore costo di carico del lotto
 * @param IDTransazione ID della transazione corrente, usato per il log, l'anomalia e per indicizzare {@link #MappaIDTrans_LifoxID}
 */
   public static void StackLIFO_InserisciValore(Map<String, ArrayDeque<String[]>> CryptoStack, String Moneta,String Qta,String Valore,String IDTransazione) {
    
    //C2: una quantità vuota o non numerica non può entrare nel LIFO: la segnalo
    //(log + lettera "E" nel campo 38) e non inserisco nulla invece di crashare
    BigDecimal qtaInserita;
    try {
        qtaInserita = new BigDecimal(Qta.trim());
    } catch (NumberFormatException | NullPointerException ex) {
        LoggerGC.ScriviErrore("Quantità non valida \"" + Qta + "\" nel movimento " + IDTransazione + ": valore non inserito nel LIFO");
        AggiungiFlagAnomalia(MappaCryptoWallet.get(IDTransazione), "E");
        return;
    }

   // ArrayDeque<String[]> stack;
    String valori[]=new String[4];
    valori[0]=Moneta;
    valori[1]=qtaInserita.abs().toPlainString();
    valori[2]=Valore;
    valori[3]=IDTransazione;
   /* if (CryptoStack.get(Moneta)==null){
        stack = new ArrayDeque<>();
        stack.push(valori);
        CryptoStack.put(Moneta, stack);
    }else{
        stack=CryptoStack.get(Moneta);
        stack.push(valori);
        CryptoStack.put(Moneta, stack);
    }*/
    // Funzione equivalente e semplificata di quella sopra    
    //Aggiungo allo stack della moneta anche questo valore
    ArrayDeque<String[]> stack = CryptoStack.computeIfAbsent(Moneta, k -> new ArrayDeque<>());
    LifoXID lifoID=MappaIDTrans_LifoxID.computeIfAbsent(IDTransazione, k -> new LifoXID());
    //Come sopra: lo stack pre-movimento è dato di sola visualizzazione, non di calcolo
    if (DettagliLifoCompleti) lifoID.StackEntratoPreMovimento=stack.clone();
    stack.push(valori);
    
    //Aggiungo allo stack relativo all'id anche questo valore relativamente a quello che è entrato
    //Questo mi servirà per poi visualizzare per ogni transazione lo stack della stessa
    lifoID.StackEntrato.push(valori);

}
    
/**
 * Ricalcola da zero plusvalenza, costo di carico e stato dello stack LIFO per tutti i movimenti
 * in {@code Principale.MappaCryptoWallet}, in ordine cronologico e raggruppati per gruppo wallet
 * (uno stack LIFO indipendente per ogni gruppo, se l'opzione {@code PlusXWallet} è attiva,
 * altrimenti un unico stack "Wallet 01"). È il metodo che orchestra l'intero motore fiscale, ma
 * il calcolo del singolo movimento sta in {@link #ElaboraMovimento}: qui restano soltanto la
 * lettura delle opzioni ({@link OpzioniRicalcolo}, una volta sola prima del ciclo), la creazione
 * dello stato LIFO e il ciclo sui movimenti. Aggiorna i movimenti in {@code MappaCryptoWallet}
 * in place (campi plusvalenza, costo di carico, flag di anomalia nel campo 38) e ripopola
 * {@link #MappaIDTrans_LifoxID}, che viene svuotata a ogni chiamata.
 * <p>
 * <b>L'unico stato che attraversa i movimenti è {@code MappaGrWallet_CryptoStack}</b>, creato qui
 * e passato a {@code ElaboraMovimento}: è il presupposto del ricalcolo incrementale (vedi
 * {@code nocommit/Documentazione/Analisi_Ricalcolo_Incrementale_Plusvalenze.md}), perché è l'unica
 * cosa che andrà salvata nei checkpoint per poter ripartire da metà storico.
 * <p>
 * Il metodo è {@code synchronized} (voce M6 di Analisi_Bug_Criticita.md): è invocato sia
 * dall'EDT sia da thread di background (rimozione SCAM di massa, import), e due ricalcoli
 * sovrapposti si riscriverebbero a vicenda i campi 16/17/19/33 producendo plusvalenze errate
 * senza alcun errore visibile. Il lock serializza le esecuzioni invece di saltarle: la seconda
 * chiamata attende e ricalcola comunque, così non restano mai valori stantii in quei campi.
 */
     public static synchronized void AggiornaPlusvalenze(){

        long Avvio = System.currentTimeMillis();

       //A1: le opzioni personali e la data soglia 2023 sono costanti per tutto il ricalcolo,
       //le leggo una volta sola qui invece che a ogni movimento del loop.
       //L'epoca va calcolata SEMPRE qui, prima del ciclo: Mappa_Wallet_Gruppo si popola
       //pigramente durante la passata, quindi confrontare un valore preso prima con uno preso
       //dopo darebbe sempre differenza e spegnerebbe l'incrementale in silenzio.
       OpzioniRicalcolo Opzioni = new OpzioniRicalcolo();
       long Epoca = Opzioni.Epoca();

       Map<String, Long> Impronte = CalcolaImpronte();

       String Ripartenza = null;   //chiave da cui rielaborare; null = passata completa
       String Motivo;
       if (DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_INCREMENTALE, "SI").equalsIgnoreCase("NO")) {
           Motivo = "incrementale disattivato dalle opzioni";
       } else if (ImpronteUltimaPassata == null) {
           Motivo = "nessuno stato valido dalla passata precedente";
       } else if (Epoca != EpocaUltimaPassata) {
           Motivo = "opzioni o tabelle personali cambiate";
       } else {
           Ripartenza = ChiaveSporcaMinima(Impronte);
           if (Ripartenza == null) {
               //Nessun movimento è cambiato: i campi in mappa sono già quelli giusti.
               UltimaPassata_Elaborati = 0;
               UltimaPassata_Completa = false;
               System.out.println("Plusvalenze: nessuna modifica, ricalcolo non necessario ("
                       + (System.currentTimeMillis() - Avvio) + " ms)");
               return;
           }
           Motivo = null;
       }

        boolean AndataABuonFine = false;
        try {
            Map<String, Map<String, ArrayDeque<String[]>>> MappaGrWallet_CryptoStack;
            String DaDove;

            if (Ripartenza == null) {
                //Passata completa: si riparte da zero e si buttano gli stati precedenti
                MappaIDTrans_LifoxID.clear();
                Checkpoint.clear();
                MappaGrWallet_CryptoStack = new TreeMap<>();
                DaDove = MappaCryptoWallet.isEmpty() ? null : MappaCryptoWallet.firstKey();
            } else {
                //Ripartenza dal checkpoint più recente che NON superi la chiave sporca
                Map.Entry<String, Map<String, Map<String, ArrayDeque<String[]>>>> Punto =
                        Checkpoint.floorEntry(Ripartenza);
                if (Punto == null) {
                    MappaGrWallet_CryptoStack = new TreeMap<>();
                    DaDove = MappaCryptoWallet.firstKey();
                } else {
                    MappaGrWallet_CryptoStack = CopiaStatoLifo(Punto.getValue());
                    DaDove = Punto.getKey();
                }
                //I checkpoint oltre il punto di ripartenza sono stati calcolati su dati vecchi
                Checkpoint.tailMap(DaDove, false).clear();
                //6.1: i dettagli LIFO non si svuotano (servono ai movimenti che non si rielaborano)
                //ma vanno tolti quelli dei movimenti spariti E quelli dei movimenti che stiamo per
                //rielaborare: StackLIFO_* fa push su un LifoXID recuperato con computeIfAbsent, e
                //senza rimozione le pile di dettaglio crescerebbero a ogni passata.
                final String Da = DaDove;
                MappaIDTrans_LifoxID.keySet().removeIf(id -> !MappaCryptoWallet.containsKey(id)
                        || String.CASE_INSENSITIVE_ORDER.compare(id, Da) >= 0);
            }

            int Elaborati = 0;
            if (DaDove != null) {
                //Avanzamento della barra dello splash all'avvio. Il denominatore e' la dimensione della
                //coda che verra' rielaborata, non l'intera mappa: in una passata incrementale il motore
                //tocca solo una parte dei movimenti e usare il totale lascerebbe la barra al palo.
                //size() su una vista tailMap costa una scansione, quindi si paga solo se lo splash c'e'.
                final double DaElaborare = SplashAvvio.attivo()
                        ? Math.max(1, MappaCryptoWallet.tailMap(DaDove, true).size()) : 1;

                for (String[] v : MappaCryptoWallet.tailMap(DaDove, true).values()) {
                    if (Elaborati % 2000 == 0) {
                        SplashAvvio.avanzamentoFase(Elaborati / DaElaborare);
                    }
                    //Il checkpoint fotografa lo stato PRIMA del movimento che gli fa da chiave
                    if (Elaborati % MOVIMENTI_PER_CHECKPOINT == 0) {
                        Checkpoint.put(v[0], CopiaStatoLifo(MappaGrWallet_CryptoStack));
                    }
                    ElaboraMovimento(v, MappaGrWallet_CryptoStack, Opzioni);
                    Elaborati++;
                }
                //Le impronte dei movimenti rielaborati vanno riprese DOPO le scritture del motore
                //(v[17] è insieme ingresso e uscita): così l'impronta memorizzata è sempre
                //"stato a fine passata" e la prossima non li vede sporchi per finta.
                for (String[] v : MappaCryptoWallet.tailMap(DaDove, true).values()) {
                    Impronte.put(v[0], Impronta(v));
                }
            }

            ImpronteUltimaPassata = Impronte;
            EpocaUltimaPassata = Epoca;
            UltimaPassata_Elaborati = Elaborati;
            UltimaPassata_Completa = (Ripartenza == null);
            AndataABuonFine = true;

            System.out.println("Plusvalenze: " + (Ripartenza == null ? "passata completa (" + Motivo + ")"
                    : "incrementale da " + DaDove) + " - " + Elaborati + " movimenti su "
                    + MappaCryptoWallet.size() + " in " + (System.currentTimeMillis() - Avvio) + " ms");
        } finally {
            //14.1: se il motore esplode a metà, la mappa resta scritta a metà. Senza questo
            //azzeramento il prefisso corrotto risulterebbe "già verificato" e nessuna passata
            //successiva lo toccherebbe più: un errore transitorio diventerebbe permanente.
            if (!AndataABuonFine) InvalidaStatoIncrementale();
        }
    }

    /**
     * Butta via impronte e checkpoint: la prossima {@link #AggiornaPlusvalenze()} sarà completa.
     * Da chiamare da fuori solo se si cambia qualcosa che il motore legge e che né la riga del
     * movimento né {@link OpzioniRicalcolo#Epoca()} riescono a vedere. Non serve dopo una normale
     * modifica ai movimenti: quella la rileva l'impronta da sola.
     */
    public static synchronized void InvalidaStatoIncrementale() {
        ImpronteUltimaPassata = null;
        Checkpoint.clear();
    }

    /**
     * Impronta corrente di tutto ciò che il motore legge e che <b>non</b> sta dentro la riga di un
     * movimento: opzioni di calcolo, gruppi wallet, token EMoney.
     *
     * <p>Esiste per {@link Backup_Restore}, che la scrive nel manifest del backup. Confrontarla dopo un
     * ripristino dice, con un solo numero, se tutte le impostazioni che influenzano il calcolo sono
     * tornate quelle di allora — senza doverle confrontare una per una, e senza doversi ricordare di
     * aggiungere al confronto le opzioni introdotte in futuro: quelle entrano in
     * {@link OpzioniRicalcolo#Epoca()}, che è l'unico elenco già mantenuto a mano.
     *
     * @return il valore di {@link OpzioniRicalcolo#Epoca()} calcolato adesso
     */
    public static long EpocaCorrente() {
        return new OpzioniRicalcolo().Epoca();
    }

    /**
     * Prima chiave (nell'ordine della mappa) in cui i dati di adesso differiscono da quelli di fine
     * passata precedente: movimento modificato, aggiunto o cancellato. {@code null} se non è
     * cambiato niente.
     * <p>
     * Le due mappe hanno lo stesso ordinamento, quindi si percorrono in parallelo senza fare
     * ricerche: la prima divergenza incontrata <b>è</b> la minima.
     */
    private static String ChiaveSporcaMinima(Map<String, Long> Attuali) {
        Iterator<Map.Entry<String, Long>> Adesso = Attuali.entrySet().iterator();
        Iterator<Map.Entry<String, Long>> Prima = ImpronteUltimaPassata.entrySet().iterator();
        Map.Entry<String, Long> a = Adesso.hasNext() ? Adesso.next() : null;
        Map.Entry<String, Long> p = Prima.hasNext() ? Prima.next() : null;

        while (a != null && p != null) {
            int Confronto = String.CASE_INSENSITIVE_ORDER.compare(a.getKey(), p.getKey());
            if (Confronto < 0) return a.getKey();          //movimento aggiunto
            if (Confronto > 0) return p.getKey();          //movimento cancellato
            if (!a.getValue().equals(p.getValue())) return a.getKey();   //movimento modificato
            a = Adesso.hasNext() ? Adesso.next() : null;
            p = Prima.hasNext() ? Prima.next() : null;
        }
        if (a != null) return a.getKey();                  //aggiunti in coda
        if (p != null) return p.getKey();                  //cancellati in coda
        return null;
    }

    /** Impronta di ogni movimento della mappa, nell'ordine della mappa. */
    private static Map<String, Long> CalcolaImpronte() {
        Map<String, Long> Impronte = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String[] v : MappaCryptoWallet.values()) Impronte.put(v[0], Impronta(v));
        return Impronte;
    }

    /**
     * Impronta a 64 bit dei soli campi che il motore <b>legge</b> (vedi il paragrafo 4.3
     * dell'analisi), più un digest dei movimenti elencati in {@code v[20]}.
     * <p>
     * Il digest dei collegati non è un di più: un PTW legge campi del suo DTW, che nell'ordine
     * della mappa può venire <b>dopo</b>. Senza, modificare il DTW non sporcherebbe il PTW che lo
     * precede, la ripartenza avverrebbe troppo avanti e resterebbe un costo di carico sbagliato
     * senza alcun errore visibile (paragrafo 5.1, dimostrato da
     * {@code CalcoliPlusvalenzeNewEquivalenzaIncrementaleTest}).
     * <p>
     * Fuori dall'impronta restano le uscite {@code v[16]}, {@code v[19]}, {@code v[33]},
     * {@code v[38]} e {@code v[31]}. {@code v[17]} invece c'è: per i DDO è un ingresso inserito
     * dall'utente, e il motore lo riscrive uguale, quindi a passata conclusa è stabile.
     */
    private static long Impronta(String[] v) {
        long h = SEME_IMPRONTA;
        h = Mescola(h, v[0]);  h = Mescola(h, v[1]);  h = Mescola(h, v[3]);
        h = Mescola(h, v[5]);  h = Mescola(h, v[8]);  h = Mescola(h, v[9]);
        h = Mescola(h, v[10]); h = Mescola(h, v[11]); h = Mescola(h, v[12]);
        h = Mescola(h, v[13]); h = Mescola(h, v[15]); h = Mescola(h, v[17]);
        h = Mescola(h, v[18]); h = Mescola(h, v[20]); h = Mescola(h, v[22]);

        if (!v[20].isBlank()) {
            for (String IdM : v[20].split(",")) {
                String[] Mov = MappaCryptoWallet.get(IdM);
                h = Mescola(h, IdM);
                //La presenza fa parte dell'impronta: un collegato che sparisce (o ricompare)
                //cambia il risultato del movimento che lo cita
                h = Mescola(h, Mov == null ? "ASSENTE" : "PRESENTE");
                if (Mov != null) {
                    h = Mescola(h, Mov[3]);  h = Mescola(h, Mov[8]);  h = Mescola(h, Mov[10]);
                    h = Mescola(h, Mov[18]); h = Mescola(h, Mov[22]);
                }
            }
        }
        return h;
    }

    /**
     * Mescola un campo nell'impronta. La <b>lunghezza entra prima del contenuto</b>: senza,
     * {@code "AB"+"C"} e {@code "A"+"BC"} darebbero la stessa impronta pur essendo due movimenti
     * diversi, ed è un'ambiguità sistematica, molto più probabile di una collisione casuale a
     * 64 bit (paragrafo 14.3).
     */
    private static long Mescola(long h, String s) {
        if (s == null) s = "";
        h = (h ^ s.length()) * PRIMO_IMPRONTA;
        for (int i = 0; i < s.length(); i++) h = (h ^ s.charAt(i)) * PRIMO_IMPRONTA;
        return h;
    }

    /**
     * Copia di uno stato LIFO: duplica mappe e pile ma <b>condivide gli array dei lotti</b>,
     * perché il motore non li modifica mai in place (il consumo parziale in
     * {@link #StackLIFO_TogliQta} alloca un array nuovo). Misurata a 0,31 ms in media su un
     * dataset da 101.103 movimenti.
     * <p>
     * <b>Invariante non difesa dal compilatore</b>: se un domani {@code StackLIFO_TogliQta}
     * scrivesse dentro i lotti invece di riallocare, tutti i checkpoint si corromperebbero in
     * silenzio e retroattivamente. Il guardiano è
     * {@code CalcoliPlusvalenzeNewEquivalenzaIncrementaleTest}.
     */
    private static Map<String, Map<String, ArrayDeque<String[]>>> CopiaStatoLifo(
            Map<String, Map<String, ArrayDeque<String[]>>> Stato) {
        Map<String, Map<String, ArrayDeque<String[]>>> Copia = new TreeMap<>();
        for (Map.Entry<String, Map<String, ArrayDeque<String[]>>> Gruppo : Stato.entrySet()) {
            Map<String, ArrayDeque<String[]>> PerMoneta = new TreeMap<>();
            for (Map.Entry<String, ArrayDeque<String[]>> Moneta : Gruppo.getValue().entrySet()) {
                PerMoneta.put(Moneta.getKey(), Moneta.getValue().clone());
            }
            Copia.put(Gruppo.getKey(), PerMoneta);
        }
        return Copia;
    }

    /**
     * Elabora un singolo movimento: ne determina la categoria fiscale, calcola plusvalenza e costo
     * di carico, movimenta le pile LIFO e riscrive in place i campi 16/17/19/33 (piu' la lettera di
     * anomalia nel campo 38, e il campo 31 del movimento controparte nel caso DTW fra gruppi
     * diversi). E' il corpo del ciclo di {@link #AggiornaPlusvalenze()}, estratto senza alcuna
     * modifica di comportamento.
     * <p>
     * L'estrazione non e' cosmetica: rende esplicito che l'unico stato che attraversa i movimenti e'
     * {@code MappaGrWallet_CryptoStack}, e che tutto il resto e' o dentro la riga o dentro
     * {@link OpzioniRicalcolo}. E' il presupposto del ricalcolo incrementale descritto in
     * {@code nocommit/Documentazione/Analisi_Ricalcolo_Incrementale_Plusvalenze.md}, dove lo stesso
     * metodo verra' invocato a partire da un checkpoint invece che dal primo movimento.
     *
     * @param v riga del movimento, formato {@code String[45]} di {@code Principale.MappaCryptoWallet}
     * @param MappaGrWallet_CryptoStack stato LIFO gruppo wallet -> moneta -> pila dei lotti, letto e modificato
     * @param Opzioni opzioni personali lette una sola volta prima del ciclo
     */
    //Volutamente package-private e non private: CalcoliPlusvalenzeNewEquivalenzaIncrementaleTest
    //lo invoca direttamente per verificare che una ripartenza da checkpoint dia lo stesso
    //risultato della passata unica (paragrafo 7 dell'analisi sul ricalcolo incrementale).
    static void ElaboraMovimento(String[] v,
            Map<String, Map<String, ArrayDeque<String[]>>> MappaGrWallet_CryptoStack,
            OpzioniRicalcolo Opzioni) {
        String GruppoWallet=DatabaseH2.Pers_GruppoWallet_Leggi(v[3],true);
           // System.out.println(GruppoWallet);
        if(!Opzioni.PlusXWallet)GruppoWallet="Wallet 01";
            
        //Questa funzione inizializza la mappa CryptoStack nel caso non esista già nella mappa MappaGrWallet_CryptoStack, nel qual caso recupera il suo valore         
        Map<String, ArrayDeque<String[]>> CryptoStack = MappaGrWallet_CryptoStack.computeIfAbsent(GruppoWallet, k -> new TreeMap<>());

        String TipoMU = Funzioni.RitornaTipoCrypto(v[8].trim(),v[1].trim(),v[9].trim());
       // if (v[12]==null)System.out.println(v[11]+"_"+v[1]+"_"+v[12]);
        String TipoME = Funzioni.RitornaTipoCrypto(v[11].trim(),v[1].trim(),v[12].trim());
        String IDTransazione=v[0];
        String IDTS[]=IDTransazione.split("_");
        //C2/A3: tolgo le eventuali segnalazioni "errore dati" e "ID malformato" della
        //passata precedente, verranno rimesse subito sotto o durante l'elaborazione
        //se il problema è ancora presente
        v[38]=v[38].replace("E", "").replace("M", "");
        //A3: un ID malformato (meno di 5 segmenti) non deve far crashare il ricalcolo:
        //il tipo movimento dell'ID resta vuoto, la classificazione si basa solo su v[18]
        //e il movimento viene segnalato con la lettera "M" nel campo 38
        String TipoID = IDTS.length > 4 ? IDTS[4] : "";
        if (IDTS.length <= 4) {
            LoggerGC.ScriviErrore("ID transazione malformato (meno di 5 segmenti): \"" + IDTransazione + "\"");
            AggiungiFlagAnomalia(v, "M");
        }
        String MonetaU=v[8];
        String QtaU=v[10];
        String MonetaE=v[11];
        String QtaE=v[13];
        String Valore=v[15];
        String CostoCaricoDonazioni=v[17];
        String VecchioPrezzoCarico="0.00";
        String NuovoPrezzoCarico="0.00";
        String Plusvalenza="0.00";
        String CalcoloPlusvalenza="N";
        long dataLong=FunzioniDate.ConvertiDatainLongMinuto(v[1]);
        boolean DataSuperiore2023=true;
        if (dataLong<Opzioni.long2023){DataSuperiore2023=false;}

        
        //TIPOLOGIA = 0 (Vendita Crypto)
        //System.out.println("aaa "+IDTransazione);
        if (TipoID.equals("VC")){
            //tolgo dal Lifo della moneta venduta il costo di carico e lo salvo
            VecchioPrezzoCarico=StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true,IDTransazione);
            
            //la moneta ricevuta non ha prezzo di carico, la valorizzo a campo vuoto
            NuovoPrezzoCarico="";
            
            //Calcolo la plusvalenza
            Plusvalenza=toBigDecimalSicuro(Valore,IDTransazione).subtract(toBigDecimalSicuro(VecchioPrezzoCarico,IDTransazione)).toPlainString(); 
            CalcoloPlusvalenza="S";
        }           
        //TIPOLOGIA = 1  (Scambio Cripto Attività medesime Caratteristiche)
        else if (!TipoMU.equalsIgnoreCase("FIAT") && !TipoME.equalsIgnoreCase("FIAT")//non devono essere fiata
                && TipoMU.equalsIgnoreCase(TipoME)&&//moneta uscita e entrata dello stesso tipo
                !TipoMU.isBlank() && !TipoME.isBlank()) //non devno essere campi nulli (senza scambi)
        {
            
            if (DataSuperiore2023||!Opzioni.Pre2023ScambiRilevanti){//se la data è superiore al 2023 oppure gli scambi pre 2023 non voglio renderli rilvenati
                //Tolgo dallo stack il costo di carico della cripèto uscita
                VecchioPrezzoCarico=StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true,IDTransazione);
                
                //Inserisco il costo di carico nello stack della cripto entrata
                NuovoPrezzoCarico=VecchioPrezzoCarico;
                StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico,IDTransazione);
                
                //La plusvalenza va valorizzata a zero
                Plusvalenza="0.00";
                CalcoloPlusvalenza="N";
             }else {//altrimenti calcolo la plusvalenza
                //Tolgo dallo stack il vecchio costo di carico
                VecchioPrezzoCarico=StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true,IDTransazione);
                
                //il prezzo di carico della moneta entrante diventa il valore della moneta stessa
                //lo aggiungo quindi allo stack del lifo
                NuovoPrezzoCarico=Valore;
                StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico,IDTransazione);
                
                //La plusvalenza è uguale al valore della moneta entrante meno il costo di carico della moneta uscente
                Plusvalenza=toBigDecimalSicuro(Valore,IDTransazione).subtract(toBigDecimalSicuro(VecchioPrezzoCarico,IDTransazione)).toPlainString();
                CalcoloPlusvalenza="S";
            }                                      
        } 
        
        
        //TIPOLOGIA = 2 (Scambio Cripto Attività Diverse Caratteristiche)
        else if (!TipoMU.equalsIgnoreCase("FIAT") && !TipoME.equalsIgnoreCase("FIAT")
                && !TipoMU.equalsIgnoreCase(TipoME)&&
                !TipoMU.isBlank() && !TipoME.isBlank())  
        {
                //Tolgo dallo stack il vecchio costo di carico
                VecchioPrezzoCarico=StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true,IDTransazione);
                
                //il prezzo di carico della moneta entrante diventa il valore della moneta stessa
                //lo aggiungo quindi allo stack del lifo
                NuovoPrezzoCarico=Valore;
                StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico,IDTransazione);
                
                //La plusvalenza è uguale al valore della moneta entrante meno il costo di carico della moneta uscente
                Plusvalenza=toBigDecimalSicuro(Valore,IDTransazione).subtract(toBigDecimalSicuro(VecchioPrezzoCarico,IDTransazione)).toPlainString();
                CalcoloPlusvalenza="S";
                                   
        }
        
        
        //TIPOLOGIA = 3 (Acquisto di Cripto attività tramite FIAT)
        else if (TipoMU.equalsIgnoreCase("FIAT") && !TipoME.equalsIgnoreCase("FIAT")&&
                !TipoMU.isBlank() && !TipoME.isBlank())  
        {
            
                NuovoPrezzoCarico=Valore;
                StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico,IDTransazione);
                
                Plusvalenza="0.00";
                CalcoloPlusvalenza="N";
                                     
                VecchioPrezzoCarico=""; 
                
                
                
        }
        
        //TIPOLOGIA = 4 (Vendita Criptoattività per FIAT)
        else if (!TipoMU.equalsIgnoreCase("FIAT") && TipoME.equalsIgnoreCase("FIAT")&&
                !TipoMU.isBlank() && !TipoME.isBlank())  
        {
            //tolgo dal Lifo della moneta venduta il costo di carico e lo salvo
            VecchioPrezzoCarico=StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true,IDTransazione);
            
            //la moneta ricevuta non ha prezzo di carico, la valorizzo a campo vuoto
            NuovoPrezzoCarico="";
            
            //Calcolo la plusvalenza
            Plusvalenza=toBigDecimalSicuro(Valore,IDTransazione).subtract(toBigDecimalSicuro(VecchioPrezzoCarico,IDTransazione)).toPlainString();
            CalcoloPlusvalenza="S";                
             
        } 
        
        
        //TIPOLOGIA = 5 , 7 e 9 -> Deposito Criptoattività di vario tipo
        else if (TipoMU.isBlank() && !TipoME.equalsIgnoreCase("FIAT")) 
        {
            //Se arrivo qua vuol dire che questo è un deposito, poi a secondo di che tipo di deposito è
            //valorizzo la tipologia corretta
            
            //TIPOLOGIA = 7; ( Deposito Criptoattività x rewards, stacking,cashback etc... - Plusvalenza immediata)
            if (TipoID.equalsIgnoreCase("RW") || v[18].contains("DAI")) {
                //Se è un cashback ed è attiva l'assimilazione ai cashback fiat allora lo gestisco come tale, altrimenti passo alle if successive
                if (Funzioni.CashbackComeFIAT(IDTransazione)){
                    NuovoPrezzoCarico = Valore;
                    StackLIFO_InserisciValore(CryptoStack, MonetaE, QtaE, NuovoPrezzoCarico,IDTransazione);
                    Plusvalenza = "0.00";
                    CalcoloPlusvalenza="N";
                    VecchioPrezzoCarico = "";
                }            
               // Funzioni.RewardRilevante(IDTransazione);
                //Se data superiore a 2023 e la reward è fiscalmente rilvente oppure se
                //la data è inferiore al 2023, la reward è rilevante e non è attiva l'opzione per cui tutte le reward pre2023 sono da mettere a costo carico a zero
                //allore considero la reward rilevante
                //altrimenti non rilevante
                else if ((DataSuperiore2023&&Funzioni.RewardRilevante(IDTransazione)) || 
                        (!DataSuperiore2023&&!Opzioni.Pre2023EarnCostoZero&&Funzioni.RewardRilevante(IDTransazione))
                        ) 
                {
                    NuovoPrezzoCarico = Valore;

                    StackLIFO_InserisciValore(CryptoStack, MonetaE, QtaE, NuovoPrezzoCarico,IDTransazione);

                    Plusvalenza = Valore;
                    CalcoloPlusvalenza="S";

                    VecchioPrezzoCarico = "";
                } else {
                    NuovoPrezzoCarico = "0.00";
                    StackLIFO_InserisciValore(CryptoStack, MonetaE, QtaE, NuovoPrezzoCarico,IDTransazione);

                    Plusvalenza = "0.00";
                    CalcoloPlusvalenza="N";

                    VecchioPrezzoCarico = "";
                }

            }

            //Tipologia = 5; (Deposito Criptoattività x spostamento tra wallet)
            else if (TipoID.equalsIgnoreCase("TI") || v[18].contains("DTW")) {
                
                
                
                
            //else if (TipoMU.isBlank()&&(IDTS[4].equalsIgnoreCase("TI") || v[18].isBlank() || v[18].contains("DTW"))) {
                //il compito è trovare la controparte del movimento qualora questa si riferisse ad un diverso gruppo wallet
                //e da li spostare il costo di carico
               // String IDControparte = null;
               // String GruppoWalletControparte = null;
                String temp[]=RitornaIDeGruppoControparteSeGruppoDiverso(v);
                //questa funzione mi torna dei valori diversi da null se
                //il wallet controparte è diverso da quello originale e se la plusvalenza va calcolata divisa per gruppo wallet
                String IDControparte=temp[0];
                String GruppoWalletControparte = temp[1];
                
           
                //Se ID controparte è diverso da null vuol dire che devo gestire il calcolo delle plusvalenze, altrimenti no
                if (IDControparte != null) {
                    Plusvalenza = "0.00";
                    CalcoloPlusvalenza="N";
                    VecchioPrezzoCarico = "";
                    
                    //DA VEDERE PERCHE' IL CRYPTO STACK E' DIVERSO
                String Mov[] = Principale.MappaCryptoWallet.get(IDControparte);
                Map<String, ArrayDeque<String[]>> CryptoStack2=MappaGrWallet_CryptoStack.get(GruppoWalletControparte);// = new TreeMap<>();
                    //A2: se il movimento controparte è stato cancellato nel frattempo
                    //tratto il movimento come non collegato invece di crashare con NPE
                    if (Mov == null) {
                        LoggerGC.ScriviErrore("Movimento controparte \"" + IDControparte + "\" non trovato per il movimento " + IDTransazione);
                        NuovoPrezzoCarico = "";
                    } else {
                Mov[31]=v[1];
                    if (CryptoStack2 == null) {
                        //In teoria qua non ci dovrei mai entrare
                        NuovoPrezzoCarico = "";
                    } else {
                        NuovoPrezzoCarico = StackLIFO_TogliQta(CryptoStack2, Mov[8], Mov[10], true,IDTransazione);
                        StackLIFO_InserisciValore(CryptoStack, MonetaE, QtaE, NuovoPrezzoCarico,IDTransazione);
                    }
                    }

                } else {
                    Plusvalenza = "0.00";
                    CalcoloPlusvalenza="N";

                    NuovoPrezzoCarico = "";

                    VecchioPrezzoCarico = "";
                }

            }
            
            //Tipologia = 9; (Deposito a costo di carico zero)
            else if(v[18].contains("DCZ")){
                 
                 NuovoPrezzoCarico="0.00";
                 StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico,IDTransazione);
                 
                 Plusvalenza="0.00";
                 CalcoloPlusvalenza="N";
                 
                 VecchioPrezzoCarico="";
            }
            
            //Tipologia = 3; (Acquisto Crypto)
            else if(v[18].contains("DAC")||TipoID.equals("AC")){
                
                NuovoPrezzoCarico=Valore;
                StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico,IDTransazione);
                
                Plusvalenza="0.00";
                CalcoloPlusvalenza="N";
                                     
                VecchioPrezzoCarico=""; 
            }
            //Tipologia = 3; (Donazioni)
            else if(v[18].contains("DDO")){
                
                NuovoPrezzoCarico=CostoCaricoDonazioni;
                StackLIFO_InserisciValore(CryptoStack, MonetaE,QtaE,NuovoPrezzoCarico,IDTransazione);
                
                Plusvalenza="0.00";
                CalcoloPlusvalenza="N";
                                     
                VecchioPrezzoCarico=""; 
            }
            //Tipologia = XY; (Deposito non categorizzato) -> Vengono caricati sul LiFo a costo di carico Zero
            else if(v[18].isBlank()){
                // nel caso la variabile considera movimenti non classficati sia a trueconsidero il movimento come deposito a zero
                 if (Opzioni.ConsideraMovimentiNC) {
                    NuovoPrezzoCarico = "0.00";
                    StackLIFO_InserisciValore(CryptoStack, MonetaE, QtaE, NuovoPrezzoCarico,IDTransazione);

                    Plusvalenza = "0.00";
                    CalcoloPlusvalenza = "N";

                    VecchioPrezzoCarico = "";
                } else {
                    //altrimenti non lo considero
                    Plusvalenza = "0.00";
                    CalcoloPlusvalenza = "N";

                    NuovoPrezzoCarico = "";

                    VecchioPrezzoCarico = "";
                }
            }
        } 
        
        //TIPOLOGIA = 6 , 8 e 10 -> Prelievo Criptoattività di vario tipo
        else if (!TipoMU.equalsIgnoreCase("FIAT") && TipoME.isBlank()) 
        {
            //Se arrivo qua vuol dire che questo è un Prelievo, poi a secondo di che tipo di deposito è
            //valorizzo la tipologia corretta                         
            
            //Tipologia = 4 Sto facendo il rimborso di un cashback o altro quindi lo considero come vendita
            if (TipoID.equalsIgnoreCase("RW")) {
                if (Funzioni.CashbackComeFIAT(IDTransazione)) {
                    //Rimborso casback in presenza di cashback considerato come fiat
                    //non calcolo plusvalenza, in sostanza lo tratto come tratto le donazioni
                    //ritorno il cashback e stop
                    VecchioPrezzoCarico = StackLIFO_TogliQta(CryptoStack, MonetaU, QtaU, true, IDTransazione);

                    NuovoPrezzoCarico = "";

                    Plusvalenza = "0.00";
                    CalcoloPlusvalenza = "N";
                } else {
                    //tolgo dal Lifo della moneta venduta il costo di carico e lo salvo
                    VecchioPrezzoCarico = StackLIFO_TogliQta(CryptoStack, MonetaU, QtaU, true, IDTransazione);

                    //la moneta ricevuta non ha prezzo di carico, la valorizzo a campo vuoto
                    NuovoPrezzoCarico = "";

                    //Calcolo la plusvalenza
                    Plusvalenza = toBigDecimalSicuro(Valore,IDTransazione).subtract(toBigDecimalSicuro(VecchioPrezzoCarico,IDTransazione)).toPlainString();
                    CalcoloPlusvalenza = "S";
                }
            }
            //Tipologia = 8;//Prelievo Criptoattività x servizi, acquisto beni etc... //per ora uguale alla tipologia 4
            else if (TipoID.equalsIgnoreCase("CM") || v[18].contains("PCO")) {
                //Se contiene commissioni e è fleggato noplusvalenze commissioni lo considero come una donazione a livello fiscale
                //System.out.println(v[15]);
                if (v[5].toUpperCase().contains("COMMISSION") && Opzioni.NoPlusCommissioni) {
                    VecchioPrezzoCarico=StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true,IDTransazione);
                
                    NuovoPrezzoCarico="";
                
                    Plusvalenza="0.00";
                    CalcoloPlusvalenza="N";  
                } else {
                    //tolgo dal Lifo della moneta venduta il costo di carico e lo salvo
                    VecchioPrezzoCarico = StackLIFO_TogliQta(CryptoStack, MonetaU, QtaU, true, IDTransazione);

                    //la moneta ricevuta non ha prezzo di carico, la valorizzo a campo vuoto
                    NuovoPrezzoCarico = "";

                    //Calcolo la plusvalenza
                    //  if (Funzioni.Funzioni_isNumeric(Valore, false)&&Funzioni.Funzioni_isNumeric(VecchioPrezzoCarico, false))
                    Plusvalenza = toBigDecimalSicuro(Valore,IDTransazione).subtract(toBigDecimalSicuro(VecchioPrezzoCarico,IDTransazione)).toPlainString();
                    CalcoloPlusvalenza = "S";
                    // else Plusvalenza="ERRORE";
                }
            }
            //Tipologia = 6;//Prelievo Criptoattività x spostamento tra wallet
            else if (TipoID.equalsIgnoreCase("TI")||v[18].contains("PTW")) {
                     
                //Se è segnalato che manca stack del LiFo lo tolgo perchè è un movimento interno.
                v[38]=v[38].replace("A", "");
                Plusvalenza="0.00";
                CalcoloPlusvalenza="N";
                 
                NuovoPrezzoCarico="";
                 
                String temp[]=RitornaIDeGruppoControparteSeGruppoDiverso(v);
                String GruppoWalletControparte = temp[1];
                 
                if (v[18].contains("PTW") && GruppoWalletControparte!=null &&!GruppoWallet.equalsIgnoreCase(GruppoWalletControparte)) {
                    //Inserisco il prezzo di carico del token in uscita solo se va poi a finire su un gruppoWallet diverso
                    //e solo se ho attiva l'opzione che vuole il calcolo delle plusvalenze divise per wallet
                    //altrimenti lo tratto alla stregua di un trasferimento interno e non metto nulla, tanto è un movimento completamente irrilevante
                    //In ogni caso non lo tolgo dal LiFo perchè lo toglierò dal LiFo nel momento in cui c'è il deposito nel nuovo wallet
                    VecchioPrezzoCarico = StackLIFO_TogliQta(CryptoStack, MonetaU, QtaU, false,IDTransazione);
                } else
                    VecchioPrezzoCarico = "";

            } 
            
            //Tipologia = 10;//(Prelievo a plusvalenza Zero ma toglie dal Lifo) FURTO o DONAZIONE
            else if(v[18].contains("PWN")){
                
                VecchioPrezzoCarico=StackLIFO_TogliQta(CryptoStack,MonetaU,QtaU,true,IDTransazione);
                
                NuovoPrezzoCarico="";
                
                Plusvalenza="0.00";
                CalcoloPlusvalenza="N";                    
                
            }
            //Tipologia = XY;//(Movimento non categorizzato) - Lo Considero come un cashOut
            else if(v[18].isBlank()){
                if (Opzioni.ConsideraMovimentiNC) {
                    //tolgo dal Lifo della moneta venduta il costo di carico e lo salvo
                    VecchioPrezzoCarico = StackLIFO_TogliQta(CryptoStack, MonetaU, QtaU, true,IDTransazione);

                    //la moneta ricevuta non ha prezzo di carico, la valorizzo a campo vuoto
                    NuovoPrezzoCarico = "";

                    //Calcolo la plusvalenza
                    Plusvalenza = toBigDecimalSicuro(Valore,IDTransazione).subtract(toBigDecimalSicuro(VecchioPrezzoCarico,IDTransazione)).toPlainString();
                    CalcoloPlusvalenza = "S";
                } else {
                    Plusvalenza = "0.00";
                    CalcoloPlusvalenza = "N";
                    NuovoPrezzoCarico = "";
                    VecchioPrezzoCarico = "";
                }
            }
        } 
        //TIPOLOGIA = 11 -> Deposito FIAT o Prelievo FIAT
        else if ((TipoMU.isBlank() && TipoME.equalsIgnoreCase("FIAT"))||(TipoME.isBlank() && TipoMU.equalsIgnoreCase("FIAT"))) 
        {
                
                NuovoPrezzoCarico="";
                
                Plusvalenza="0.00";
                CalcoloPlusvalenza="N";
                                       
                VecchioPrezzoCarico="";
                
        }
        else {
            LoggerGC.ScriviErrore("CategorizzaTransazione x Plusvalenze - Nessuna Tipologia Individuata");
            System.out.println("Tipologie Uscita ed entrata usate -> "+TipoMU+" - "+TipoME);
        }           

                v[16]=VecchioPrezzoCarico;
                v[17]=NuovoPrezzoCarico;
                v[19]=Plusvalenza;
                v[33]=CalcoloPlusvalenza;


    }

    /**
     * Le opzioni personali (e la data soglia 2023) che il motore legge <b>una volta sola</b> prima
     * del ciclo: sono costanti per l'intero ricalcolo e valgono per tutti i movimenti.
     * <p>
     * <b>E' anche l'elenco completo degli ingressi del motore che non stanno dentro la riga del
     * movimento</b>: cambiarne uno cambia il risultato di tutto lo storico senza che nessun
     * movimento risulti modificato. Chi aggiunge qui una nuova opzione deve tenerne conto nel
     * ricalcolo incrementale (paragrafo 6 di
     * {@code nocommit/Documentazione/Analisi_Ricalcolo_Incrementale_Plusvalenze.md}), dove questo
     * elenco e' quello che decide quando il ricalcolo deve tornare completo.
     */
    static final class OpzioniRicalcolo {

        /** Se {@code false} i movimenti non classificati non vengono conteggiati ({@code PL_CosiderareMovimentiNC}). */
        final boolean ConsideraMovimentiNC;
        /** Se {@code true} ogni gruppo wallet ha una pila LIFO indipendente ({@code PlusXWallet}). */
        final boolean PlusXWallet;
        /** Se {@code true} le commissioni non generano plusvalenza ({@code Plusvalenze_NoPlusvalenzeCommissioni}). */
        final boolean NoPlusCommissioni;
        /** Se {@code true} le reward precedenti al 2023 entrano nel LIFO a costo zero ({@code Plusvalenze_Pre2023EarnCostoZero}). */
        final boolean Pre2023EarnCostoZero;
        /** Se {@code true} gli scambi fra cripto dello stesso tipo sono rilevanti anche prima del 2023 ({@code Plusvalenze_Pre2023ScambiRilevanti}). */
        final boolean Pre2023ScambiRilevanti;
        /** 1 gennaio 2023 nel formato long dei minuti, soglia delle due regole qui sopra. */
        final long long2023;

        OpzioniRicalcolo() {
            //Con questa opzione decido che fare in caso di movimenti non classificati, se conteggiarli o meno
            ConsideraMovimentiNC = !DatabaseH2.Pers_Opzioni_Leggi("PL_CosiderareMovimentiNC", "SI").equalsIgnoreCase("NO");

            //controllo se devo o meno prendere in considerazione i gruppi wallet per il calcolo della plusvalenza
            String PlusXW = DatabaseH2.Pers_Opzioni_Leggi("PlusXWallet");
            PlusXWallet = (PlusXW != null && PlusXW.equalsIgnoreCase("SI"));

            String NoPlusCom = DatabaseH2.Pers_Opzioni_Leggi("Plusvalenze_NoPlusvalenzeCommissioni");
            NoPlusCommissioni = (NoPlusCom != null && NoPlusCom.equalsIgnoreCase("SI"));

            String Pre2023Earn = DatabaseH2.Pers_Opzioni_Leggi("Plusvalenze_Pre2023EarnCostoZero");
            Pre2023EarnCostoZero = (Pre2023Earn != null && Pre2023Earn.equalsIgnoreCase("SI"));

            String Pre2023Scambi = DatabaseH2.Pers_Opzioni_Leggi("Plusvalenze_Pre2023ScambiRilevanti");
            Pre2023ScambiRilevanti = (Pre2023Scambi != null && Pre2023Scambi.equalsIgnoreCase("SI"));

            long2023 = FunzioniDate.ConvertiDatainLongMinuto("2023-01-01 00:00");
        }

        /**
         * Impronta di <b>tutto ciò che il motore legge e che non sta dentro la riga di un
         * movimento</b>. Se cambia, non esiste un punto di ripartenza: il verdetto cambia su tutto
         * lo storico senza che nessun movimento risulti modificato, e il ricalcolo torna completo.
         * <p>
         * Comprende anche le opzioni che il motore non legge direttamente ma attraverso
         * {@code Funzioni.RewardRilevante} e {@code Funzioni.CashbackComeFIAT}, e le due mappe in
         * memoria da cui dipendono gruppi wallet e token EMoney. Sono piccole (una voce per wallet,
         * una per token EMoney), quindi si possono hashare per intero a ogni passata: <b>è
         * volutamente diverso da un contatore di versione</b> da incrementare dove quelle tabelle
         * vengono scritte, che sarebbe l'ennesima cosa da ricordarsi in N punti del programma.
         * <p>
         * <b>Chi aggiunge un'opzione al motore la aggiunga anche qui</b>: è l'unico elenco di
         * questo lavoro che va tenuto aggiornato a mano. Dimenticarla produce risultati stantii.
         */
        long Epoca() {
            long h = SEME_IMPRONTA;
            h = Mescola(h, ConsideraMovimentiNC ? "1" : "0");
            h = Mescola(h, PlusXWallet ? "1" : "0");
            h = Mescola(h, NoPlusCommissioni ? "1" : "0");
            h = Mescola(h, Pre2023EarnCostoZero ? "1" : "0");
            h = Mescola(h, Pre2023ScambiRilevanti ? "1" : "0");
            h = Mescola(h, Long.toString(long2023));
            //Lette dentro Funzioni.RewardRilevante
            h = Mescola(h, DatabaseH2.Pers_Opzioni_Leggi("PDD_CashBack", "SI"));
            h = Mescola(h, DatabaseH2.Pers_Opzioni_Leggi("PDD_Staking", "SI"));
            h = Mescola(h, DatabaseH2.Pers_Opzioni_Leggi("PDD_Airdrop", "SI"));
            h = Mescola(h, DatabaseH2.Pers_Opzioni_Leggi("PDD_Earn", "SI"));
            h = Mescola(h, DatabaseH2.Pers_Opzioni_Leggi("PDD_Reward", "SI"));
            //Lette dentro Funzioni.CashbackComeFIAT
            h = Mescola(h, DatabaseH2.Pers_Opzioni_Leggi("CashBackComeFIAT", "NO"));
            h = Mescola(h, DatabaseH2.Pers_Opzioni_Leggi("CashBackComeFIATAnno"));
            //Attivare i dettagli LIFO completi obbliga a una passata intera: i movimenti prima del
            //punto di ripartenza resterebbero senza dettagli, ed è esattamente per averli tutti che
            //AggiornaPlusvalenzeConDettagliLifo() viene chiamata
            h = Mescola(h, DettagliLifoCompleti ? "1" : "0");
            //Wallet -> gruppo: cambiare un gruppo ridistribuisce le pile LIFO di tutto lo storico
            for (Map.Entry<String, String> e : DatabaseH2.Mappa_Wallet_Gruppo.entrySet()) {
                h = Mescola(h, e.getKey());
                h = Mescola(h, e.getValue());
            }
            //Token EMoney: spostare la data di un token riclassifica movimenti passati
            for (Map.Entry<String, String> e : Principale.Mappa_EMoney.entrySet()) {
                h = Mescola(h, e.getKey());
                h = Mescola(h, e.getValue());
            }
            return h;
        }
    }




   

  /* public static String RitornaTipoCrypto(String Token,String Data,String Tipologia) {
       String Tipo=Tipologia;
       String DataEmoney=CDC_Grafica.Mappa_EMoney.get(Token);
       if(Tipologia.equalsIgnoreCase("Crypto")&&DataEmoney!=null){
           long dataemoney=OperazioniSuDate.ConvertiDatainLong(DataEmoney);
           long datascambio=OperazioniSuDate.ConvertiDatainLong(Data);
           if (datascambio>=dataemoney) Tipo="EMoney";
       }
       return Tipo;
   }*/

    /**
     * Se l'opzione {@code PlusXWallet} è attiva, individua il movimento "controparte" di un
     * trasferimento tra wallet ({@code v}, di tipo DTW o PTW, importato o manuale) quando questo
     * appartiene a un gruppo wallet diverso: serve a sapere se il costo di carico va spostato da
     * uno stack LIFO di gruppo a un altro invece di restare nello stesso gruppo. Gestisce sia il
     * trasferimento semplice (2-3 movimenti collegati) sia lo scambio differito (più di 3
     * movimenti collegati, dove la controparte è il PTW automatico marcato "AU").
     *
     * @param v riga del movimento (formato {@code String[45]} di {@code Principale.MappaCryptoWallet})
     * @return array di 2 elementi: {@code [0]} ID del movimento controparte, {@code [1]} il suo gruppo wallet; entrambi {@code null} se l'opzione è disattiva, il movimento non è un trasferimento, o la controparte è nello stesso gruppo
     */
    public static String[] RitornaIDeGruppoControparteSeGruppoDiverso(String v[]) {
        
        String IDeGruppo[] = new String[2];

        boolean PlusXWallet = false;
        String PlusXW = DatabaseH2.Pers_Opzioni_Leggi("PlusXWallet");
        if (PlusXW != null && PlusXW.equalsIgnoreCase("SI")) {
            PlusXWallet = true;
        }
        if (!PlusXWallet) return IDeGruppo;
        //Se non voglio la distinzione per wallet ritorno subito la stringa nulla
        
        
        //il compito è trovare la controparte del movimento qualora questa si riferisse ad un diverso gruppo wallet
        //e da li spostare il costo di carico
        String GruppoWallet = DatabaseH2.Pers_GruppoWallet_Leggi(v[3],true);
        String IDControparte = null;
        String GruppoWalletControparte = null;
        //comincio impostando le prime condizioni
        //v[20] non deve essere nullo ovvero devo avere transazioni allegate
        //v[18] deve essere un deposito derivante da trasferimenti e deve essere o un movimento importato o uno manuale (v[22]=ad A o M)
        if (!v[20].isBlank() && (v[18].contains("DTW") || v[18].contains("PTW")) && (v[22].equals("A") || v[22].equals("M"))) {

            //Se è un movimento di Trasferimento tra wallet (2 o 3 movimenti a seconda se ci sono le commissioni) il movimento controparte è l'unico PTW
            //Se è un movimento di scambio differito (5 movimenti) il movimento controparte è un PTW classificato come AU (posizione 22)
            //Tutto questo lo faccio però solo se il movimento di controparte PTW fa parte di un altro gruppo di wallet, altrimentio non faccio nulla.
            String Movimenti[] = v[20].split(",");

            if (Movimenti.length > 3)//Sono in presenza di uno scambio differito
            {
                for (String IdM : Movimenti) {
                    String Mov[] = Principale.MappaCryptoWallet.get(IdM);
                    //devo trovare la controparte che in questo caso è il movimento di prelievo creato automaticamente dal sistema
                    //inoltre devo verificare che il gruppo wallet del deposito sia differente dal gruppo wallet del prelievo
                    //perchè se fanno parte dello stesso gruppo non devo fare nulla
                    //se fanno parte dello stesso gruppo infatti è lo stesso movimento di scambio a spostare il costo di carico

                    //A2: un ID collegato può non esistere più nella mappa (movimento cancellato)
                    if (Mov == null) {
                        LoggerGC.ScriviErrore("Movimento collegato \"" + IdM + "\" non trovato per il movimento " + v[0]);
                        continue;
                    }
                    if (v[18].contains("DTW") && Mov[18].contains("PTW") && Mov[22].contains("AU")
                            && !GruppoWallet.equals(DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3],true))) {
                        IDControparte = IdM;
                        GruppoWalletControparte = DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3],true);
                    }
                    //non faccio niente in caso di prelievo PTW perchè quello non è mai rilevante essendo un mero trasferimento interno
                }
            } else {//Scambio tra wallet

                for (String IdM : Movimenti) {
                    String Mov[] = Principale.MappaCryptoWallet.get(IdM);
                    //devo trovare la controparte che in questo caso è l'unico movimento di prelievo
                    //inoltre vedo verificare che il gruppo wallet del deposito sia differente dal gruppo wallet del prelievo
                    //perchè se fanno parte dello stesso gruppo non devo fare nulla
                    //A2: un ID collegato può non esistere più nella mappa (movimento cancellato)
                    if (Mov == null) {
                        LoggerGC.ScriviErrore("Movimento collegato \"" + IdM + "\" non trovato per il movimento " + v[0]);
                        continue;
                    }
                    if (v[18].contains("DTW") && Mov[18].contains("PTW")
                            && !GruppoWallet.equals(DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3],true))) {
                        IDControparte = IdM;
                        GruppoWalletControparte = DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3],true);
                    } else if (v[18].contains("PTW") && Mov[18].contains("DTW")
                            && !GruppoWallet.equals(DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3],true))) {
                        IDControparte = IdM;
                        GruppoWalletControparte = DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3],true);
                    }

                }

            }

        }
        
        IDeGruppo[0] = IDControparte;
        IDeGruppo[1] = GruppoWalletControparte;
        return IDeGruppo;
    }
    
    
    
    
    
    
          public static class LifoXID {

          

          ArrayDeque<String[]> StackEntrato=new ArrayDeque<>();
          ArrayDeque<String[]> StackEntratoPreMovimento=new ArrayDeque<>();
          ArrayDeque<String[]> StackUscito=new ArrayDeque<>();
          ArrayDeque<String[]> StackUscitoRimanenze=new ArrayDeque<>();
          
          
          
          /**
           * Sostituisce interamente lo stack dei lotti entrati nel LIFO da questa transazione.
           *
           * @param PMStack nuovo stack da usare come {@code StackEntrato}
           */
          public void sostituisci_CryptoStackEntrato(ArrayDeque<String[]> PMStack)
          {
            StackEntrato=PMStack;
          }
          /**
           * Aggiunge un singolo lotto (in coda) allo stack dei lotti entrati per questa transazione.
           *
           * @param Dettaglio riga del lotto (moneta, quantità, costo, ID transazione origine)
           */
          public void aggiungi_Entrato_Dettagli(String Dettaglio[])
          {
            StackEntrato.add(Dettaglio);
          }

          /**
           * @return lo stack dei lotti inseriti nel LIFO da questa transazione
           */
          public ArrayDeque<String[]> Get_CryptoStackEntrato()
          {
            return StackEntrato;
          }
          /**
           * @return una copia dello stack LIFO della moneta così com'era immediatamente prima che questa transazione lo modificasse;
           *         vuoto se il ricalcolo non è stato eseguito con i dettagli completi attivi (vedi {@link Calcoli_PlusvalenzeNew#AggiornaPlusvalenzeConDettagliLifo()})
           */
          public ArrayDeque<String[]> Get_CryptoStackEntratoPreMovimento()
          {
            return StackEntratoPreMovimento;
          }

          /**
           * @return lo stack dei lotti estratti dal LIFO da questa transazione (vedi {@link Calcoli_PlusvalenzeNew#StackLIFO_TogliQta})
           */
          public ArrayDeque<String[]> Get_CryptoStackUscito()
          {
            return StackUscito;
          }

          /**
           * @return una copia dello stack LIFO della moneta rimasto dopo l'estrazione operata da questa transazione;
           *         vuoto se il ricalcolo non è stato eseguito con i dettagli completi attivi (vedi {@link Calcoli_PlusvalenzeNew#AggiornaPlusvalenzeConDettagliLifo()})
           */
          public ArrayDeque<String[]> Get_CryptoStackUscitoRimanenze()
          {
            return StackUscitoRimanenze;
          }
      
      }     

}   
    
    



        /* SEZIONE SPIEGAZIONI
        In sezione funzione suddivido gli scambi crypto per categorie in modo da poter più facilmente poi gestire il calcolo della plusvalenza etc...
        Per Ogni tipologia gli andrò a dire come comportarsi con le seguenti situazioni:
            - Calcolo Plusvalenza
            - Valore del Nuovo Costo di Carico
            - Se Togliere o meno dallo stack del lifo della moneta uscente il vecchio costo di carico
            - Se mettere e con che valore mettere nello stack lifo relativo alla moneta entrante il valore del nuovo costo di carico
        
        In questa funzione in particolare divito gli scambi in 10 categorie:
            1 - Scambio tra Criptoattività Omogenee
                Comprende:  NFT -> NFT
                            Crypto -> Crypto     
            2 - Scambio tra Criptoattività non omogenee
                Comprende:  Crypto -> NFT
                            NFT -> Crypto       
            3 - Acquisto Criptoattività
                Comprende:  FIAT -> NFT
                            FIAT -> Crypto         
            4 - Vendita Criptoattività
                Comprende:  NFT -> FIAT
                            Crypto -> FIAT                 
            5 - Deposito Criptoattività x spostamento tra wallet
                Comprende:  -> NFT          (Tipologia TI su IDTrans oppure Tipologia Vuota o DTW su quella della Transazione)
                            -> Crypto       (Tipologia TI su IDTrans oppure Tipologia Vuota o DTW su quella della Transazione)                         
            6 - Prelievo Criptoattività x spostamento tra wallet
                Comprende:  NFT ->          (Tipologia TI su IDTrans oppure Tipologia Vuota o PTW su quella della Transazione)
                            Crypto ->       (Tipologia TI su IDTrans oppure Tipologia Vuota o PTW su quella della Transazione) 
            7 - Deposito Criptoattività x rewards, stacking,cashback etc...
                Comprende:  -> NFT          (Tipologia RW su IDTrans oppure Tipologia DAI su quella della Transazione)
                            -> Crypto       (Tipologia RW su IDTrans oppure Tipologia DAI su quella della Transazione)
            8 - Prelievo Criptoattività x servizi, acquisto beni etc...
                Comprende:  NFT ->          (Tipologia CM su IDTrans oppure Tipologia PCO su quella della Transazione)
                            Crypto ->       (Tipologia CM su IDTrans oppure Tipologia PCO su quella della Transazione)
            9 - Deposito Criptoattività DCZ         (Deposito a costo di carico zero)
                Comprende:  -> NFT          (Tipologia DCZ su quella della Transazione)
                            -> Crypto       (Tipologia DCZ su quella della Transazione)
            10 - Prelievo Criptoattività PWN        (Prelievo a plusvalenza Zero ma toglie dal Lifo) 
                Comprende:  NFT ->          (Tipologia PWN su quella della Transazione)
                            Crypto ->       (Tipologia PWN su quella della Transazione)
            11 - Deposito FIAT
                Comprende:    -> FIAT
        
        
        
        Per ogni tipologia trovata devo indicare le seguenti caratteristiche:
        
        TipologiaPlus -> Indica come va calcolata la Plusvalenza per ogni tipologia di scambio (Va messo poi nel Campo 19 della tabella)
            TipologiaPlus=0 :   Il campo plusvalenza va compilato con valore Zero 
                    Cosa rientra :  Tipologia 1 (Scambio criptoatt. omogenee)
                                    Tipologia 3 (Acquisto criptoattivita)
                                    Tipologia 5 (Deposito x spostamento tra Wallet di Proprietà)
                                    Tipologia 6 (Prelievo x spostamento tra Wallet di Proprietà)
                                    Tipologia 9 (Deposito forzato a costo di carico zero)
                                    Tipologia 10(Prelievo a plus zero che movimenta il solo lifo... Caso molto particolare)
            TipologiaPlus=1 :   Il campo plusvalenza va compilato con il ValoreTransazione
                    Cosa rientra :  Tipologia 7 (Deposito criptoattività derivanti da Stacking,cashback,rewards,earn etc...)
            TipologiaPlus=2 :   Plusvalenza=Valore Transazione - Costo di Carico Moneta Uscita (Vecchio Costo di carico)
                    Cosa rientra :  Tipologia 2 (Scambio CriptoAttività non omogeneo)
                                    Tipologia 4 (Vendita criptoattività)
                                    Tipologia 8 (Prelievo/Vendita di Criptoattività in cambio di beni o servizi)
        
        TipologiaNCC -> Indica come va calcolato il nuovo costo di carico per ogni tipologia di scambio (Va messo poi nel Campo 17 della tabella)
            TipologiaNCC=0 :    Il campo relativo al Nuovo Costo di Carico va valorizzato a Zero    
                    Cosa rientra :  Tipologia 4 (Vendita criptoattività)
                                    Tipologia 9 (Deposito forzato a costo di carico zero)
                                    Tipologia 8 (Prelievo/Vendita di Criptoattività in cambio di beni o servizi)
                                    Tipologia 10(Prelievo a plus zero che movimenta il solo lifo... Caso molto particolare)
            TipologiaNCC=1 :    Nuovo Costo di Carico = Costo di Carico preso tramite lifo da moneta Uscita (Vecchio costo di carico)   
                    Cosa rientra :  Tipologia 1 (Scambio criptoatt. omogenee)
                                    Tipologia 5 (Deposito x spostamento tra Wallet di Proprietà)
                                    Tipologia 6 (Prelievo x spostamento tra Wallet di Proprietà)
            TipologiaNCC=2 :    Nuovo Costo di Carico = Valore Transazione   
                    Cosa rientra :  Tipologia 2 (Scambio CriptoAttività non omogeneo)
                                    Tipologia 3 (Acquisto criptoattivita)
                                    Tipologia 7 (Deposito criptoattività derivanti da Stacking,cashback,rewards,earn etc...)
        
        TipologiaStackLIFOVecchioCosto -> Indica se devo togliero o meno dallo stack il vecchio costo di carico della moneta uscente
            TipologiaStackLIFOVecchioCosto=0 :  Non tolgo dallo stack il vecchio costo di carico
                    Cosa rientra :  Tipologia 3 (Acquisto criptoattivita)
                                    Tipologia 5 (Deposito x spostamento tra Wallet di Proprietà)
                                    Tipologia 6 (Prelievo x spostamento tra Wallet di Proprietà)
                                    Tipologia 7 (Deposito criptoattività derivanti da Stacking,cashback,rewards,earn etc...)
                                    Tipologia 9 (Deposito forzato a costo di carico zero)
            TipologiaStackLIFOVecchioCosto=1 :  Tolgo dallo stack il vecchio costo di carico
                    Cosa rientra :  Tipologia 1 (Scambio criptoatt. omogenee)
                                    Tipologia 2 (Scambio CriptoAttività non omogeneo)
                                    Tipologia 4 (Vendita criptoattività)
                                    Tipologia 8 (Prelievo/Vendita di Criptoattività in cambio di beni o servizi)
                                    Tipologia 10(Prelievo a plus zero che movimenta il solo lifo... Caso molto particolare)
        
        TipologiaStackLIFONuovoCosto -> Indica che valore devo o se devo inserire nello stack del Lifo sulla moneta entrante
            TipologiaStackLIFONuovoCosto=0 :   Costo Lifo Moneta Entrante = Zero
                    Cosa rientra :  Tipologia 9 (Deposito forzato a costo di carico zero)
            TipologiaStackLIFONuovoCosto=1 :   Non faccio nulla (non inserisco nessun valore)
                    Cosa rientra :  Tipologia 4 (Vendita criptoattività)
                                    Tipologia 5 (Deposito x spostamento tra Wallet di Proprietà)
                                    Tipologia 6 (Prelievo x spostamento tra Wallet di Proprietà)
                                    Tipologia 8 (Prelievo/Vendita di Criptoattività in cambio di beni o servizi
                                    Tipologia 10(Prelievo a plus zero che movimenta il solo lifo... Caso molto particolare))
            TipologiaStackLIFONuovoCosto=2 :   Costo Lifo Moneta Entrante = Costo Lifo Moneta Uscente
                    Cosa rientra :  Tipologia 1 (Scambio criptoatt. omogenee)
            TipologiaStackLIFONuovoCosto=3 :   Costo Lifo Moneta Entrante = Valore Transazione
                    Cosa rientra :  Tipologia 2 (Scambio CriptoAttività non omogeneo)
                                    Tipologia 3 (Acquisto criptoattivita)
                                    Tipologia 7 (Deposito criptoattività derivanti da Stacking,cashback,rewards,earn etc...)
        
        
        Quindi ricapitolanto per tipologia abbiamo le seguenti caratteristiche
        Tipologia 1 (Scambio criptoatt. omogenee) :
                TipologiaPlus=0 (Plusvalenza=0)
                TipologiaNCC=2 (Nuovo Costo di Carico = Costo di Carico preso tramite lifo da moneta Uscita)
                TipologiaStackLIFOVecchioCosto=1  (Tolgo dallo stack il vecchio costo di carico)
                TipologiaStackLIFONuovoCosto=2  (Costo Lifo Moneta Entrante = Costo Lifo Moneta Uscente)
        Tipologia 2 (Scambio CriptoAttività non omogeneo) :
                TipologiaPlus=2 :   Plusvalenza=Valore Transazione - Costo di Carico Moneta Uscita (Vecchio Costo di carico)
                TipologiaNCC=3 :    Nuovo Costo di Carico = Valore Transazione
                TipologiaStackLIFOVecchioCosto=1 :  Tolgo dallo stack il vecchio costo di carico
                TipologiaStackLIFONuovoCosto=3 :   Costo Lifo Moneta Entrante = Valore Transazione
        Tipologia 3 (Acquisto criptoattivita)
                TipologiaPlus=0 :   Il campo plusvalenza va compilato con valore Zero
                TipologiaNCC=3 :    Nuovo Costo di Carico = Valore Transazione 
                TipologiaStackLIFOVecchioCosto=0 :  Non tolgo dallo stack il vecchio costo di carico
                TipologiaStackLIFONuovoCosto=3 :   Costo Lifo Moneta Entrante = Valore Transazione
        Tipologia 4 (Vendita criptoattività)
                TipologiaPlus=2 :   Plusvalenza=Valore Transazione - Costo di Carico Moneta Uscita (Vecchio Costo di carico)
                TipologiaNCC=1 :    Il campo relativo al Nuovo Costo di Carico va valorizzato a "" 
                TipologiaStackLIFOVecchioCosto=1 :  Tolgo dallo stack il vecchio costo di carico
                TipologiaStackLIFONuovoCosto=1 :   Non faccio nulla (non inserisco nessun valore)
        Tipologia 5 o 6 (Deposito o Prelievo x spostamento tra Wallet di Proprietà)
                TipologiaPlus=0 :   Il campo plusvalenza va compilato con valore Zero
                TipologiaNCC=2 :    Nuovo Costo di Carico = Costo di Carico preso tramite lifo da moneta Uscita (Vecchio costo di carico)
                TipologiaStackLIFOVecchioCosto=0 :  Non tolgo dallo stack il vecchio costo di carico
                TipologiaStackLIFONuovoCosto=1 :   Non faccio nulla (non inserisco nessun valore)
        Tipologia 7 (Deposito criptoattività derivanti da Stacking,cashback,rewards,earn etc...)
                TipologiaPlus=1 :   Il campo plusvalenza va compilato con il ValoreTransazione
                TipologiaNCC=3 :    Nuovo Costo di Carico = Valore Transazione 
                TipologiaStackLIFOVecchioCosto=0 :  Non tolgo dallo stack il vecchio costo di carico
                TipologiaStackLIFONuovoCosto=3 :   Costo Lifo Moneta Entrante = Valore Transazione
        Tipologia 8 (Prelievo/Vendita di Criptoattività in cambio di beni o servizi)
                TipologiaPlus=2 :   Plusvalenza=Valore Transazione - Costo di Carico Moneta Uscita (Vecchio Costo di carico)
                TipologiaNCC=1 :    Il campo relativo al Nuovo Costo di Carico va valorizzato a ""  
                TipologiaStackLIFOVecchioCosto=1 :  Tolgo dallo stack il vecchio costo di carico
                TipologiaStackLIFONuovoCosto=1 :   Non faccio nulla (non inserisco nessun valore)
        Tipologia 9 (Deposito forzato a costo di carico zero)
                TipologiaPlus=0 :   Il campo plusvalenza va compilato con valore Zero
                TipologiaNCC=0 :    Il campo relativo al Nuovo Costo di Carico va valorizzato a Zero
                TipologiaStackLIFOVecchioCosto=0 :  Non tolgo dallo stack il vecchio costo di carico
                TipologiaStackLIFONuovoCosto=0 :   Costo Lifo Moneta Entrante = Zero
        Tipologia 10(Prelievo a plus zero che movimenta il solo lifo... Caso molto particolare))
                TipologiaPlus=0 :   Il campo plusvalenza va compilato con valore Zero
                TipologiaNCC=1 :    Il campo relativo al Nuovo Costo di Carico va valorizzato a "" 
                TipologiaStackLIFOVecchioCosto=1 :  Tolgo dallo stack il vecchio costo di carico
                TipologiaStackLIFONuovoCosto=1 :   Non faccio nulla (non inserisco nessun valore)
        
        
        */