/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.awt.Cursor;
import java.awt.Window;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JTable;

/**
 *
 * @author luca
 */
public class Principale_GiacenzeaData {
    
    /**
     * Rettifica la giacenza di un token alla data selezionata nella tabella "Giacenze a data", chiedendo
     * all'utente la nuova giacenza desiderata e generando un movimento artificiale di aggiustamento (deposito
     * o prelievo, a seconda che la nuova giacenza sia maggiore o minore di quella attuale). Se il token ha
     * giacenze negative precedenti, avvisa l'utente e richiede conferma esplicita prima di procedere.
     * L'operazione è consentita solo per token di tipo {@code "Crypto"} e non è applicabile se il filtro
     * wallet è impostato su {@code "tutti"}.
     * @param TabMovimenti tabella da cui leggere la riga selezionata (moneta, giacenza attuale, ecc.)
     * @param Wallet nome del wallet corrente, oppure {@code "tutti"} per disabilitare l'operazione
     * @param owner finestra parent dei dialog
     * @return {@code true} se è stata effettuata una modifica, {@code false} se l'operazione è stata annullata o non applicabile
     */
    public static boolean GiacenzeaData_Funzione_SistemaQta(JTable TabMovimenti,String Wallet, Window owner) {
        
        boolean tuttook=false;
        int rigaselezionata=Tabelle.Funzioni_getRigaSelezionata(TabMovimenti);
        if (rigaselezionata >= 0) {
            int scelta;
            String Moneta = TabMovimenti.getModel().getValueAt(rigaselezionata, 2).toString();
            String IDTrans = TabMovimenti.getModel().getValueAt(rigaselezionata, 8).toString();
            //Adesso recupero tipo moneta e address dalla transazione
            String mov[]=Principale.MappaCryptoWallet.get(IDTrans);
            String TipoMoneta;
            String AddressMoneta;
            if(Moneta.equals(mov[8]))
            {
                TipoMoneta=mov[9];
                AddressMoneta=mov[26];
            }
            else
            {
                //se non è il token in uscita allora è quello in ingresso
                TipoMoneta=mov[12];
                AddressMoneta=mov[28];
            }
                        
            String GiacenzaAttualeS = TabMovimenti.getModel().getValueAt(rigaselezionata, 7).toString();
            String GiacNegativaPrecedente = TabMovimenti.getModel().getValueAt(rigaselezionata, 9).toString();

          //  long DataRiferimento;
            BigDecimal GiacenzaAttuale = new BigDecimal(GiacenzaAttualeS);
            BigDecimal GiacenzaVoluta = new BigDecimal(0);
            BigDecimal QtaNuovoMovimento;
            
            if (Wallet==null || !Wallet.equalsIgnoreCase("tutti")){
            if (TipoMoneta.equalsIgnoreCase("Crypto")){
            
            //========== MESSAGGIO INIZIALE, CHIEDO DI INSERIRE LA NUOVA GIACENZA =========
            
         /*   String m = JOptionPane.showInputDialog(this, "<html>Il saldo alla data selezionata è : <b>" + GiacenzaAttuale.toPlainString() + "</b> <br>"
                    + "Indicare nel riquadro sottostante la giacenza che il token <b>" + Moneta + "</b> dovrà avere al termine dell'operazione: </html>", GiacenzaVoluta);*/
            AppDialog.DialogResult result = AppDialog.builder(owner)
        .windowTitle("Rettifica di Giacenza")
        .bodyTitle("Imposta giacenza finale")
        .showTitleInBody(true)
        .theme()
        .type(AppDialog.DialogType.INFO)
        .message("""
                Il saldo alla data selezionata è: %s.
                """.formatted(GiacenzaAttuale.toPlainString()))
        .details("""
                Indica nel campo sottostante la giacenza che il token %s
                dovrà avere al termine dell'operazione.
                """.formatted(Moneta))
        .inputField("Nuova giacenza", GiacenzaVoluta.toPlainString())
        .inputColumns(18)
        .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                .role(AppDialog.ActionRole.SECONDARY)
                .build())
        .action(AppDialog.DialogAction.builder("confirm", "Conferma")
                .role(AppDialog.ActionRole.PRIMARY)
                .build())
        .showDialog();

String m = result.isAction("confirm") ? result.getInputValue() : null;
            //  completato = m!=null; //se premo annulla nel messaggio non devo poi chiudere la finestra, quindi metto completato=false
            if (m != null) {
                m = m.replace(",", ".").trim();//sostituisco le virgole con i punti per la separazione corretta dei decimali
                if (Principale.Funzioni_isNumeric(m, false)) {
                    GiacenzaVoluta = new BigDecimal(m);
                    
            //========= SE SONO PRESENTI GIACENZE NEGATIVE PRECEDENTI AVVISO E CHIEDO SE SI VUOLE CONTINUARE =========
            
                    if (GiacNegativaPrecedente.equals("S")) {
                        //Se arrivo qua vuol dire che sto cercando di modificare la giacenza di un token che ha saldi negativi precedenti
                //In questo caso emetto un messaggio di alert che avvisa che sarebbe meglio correggere queste giacenze in ordine.
                        result = AppDialog.builder(owner)
                                .windowTitle("Avviso")
                                .bodyTitle("Attenzione")
                                .showTitleInBody(true)
                                .theme()
                                .type(AppDialog.DialogType.WARNING)
                                .message("""
                    Stai tentando di cambiare la giacenza di un token che ha avuto
                    saldi negativi in passato.
                    """)
                                .details("""
                    Sarebbe consigliabile correggere i movimenti in ordine cronologico
                    per evitare discrepanze nei calcoli.

                    Vuoi comunque proseguire con la modifica?
                    """)
                                .secondaryAction("si", "Si")
                                .primaryAction("no", "No")
                                .showDialog();

                        if (result.isAction("no")) {
                            return false;
                        }
                    }
                    
                    QtaNuovoMovimento = GiacenzaVoluta.subtract(GiacenzaAttuale);
                    String SQta = QtaNuovoMovimento.toPlainString();
                   // BigDecimal ValoreMovOrigine=new BigDecimal(TabMovimenti.getModel().getValueAt(rigaselezionata, 6).toString());
                    BigDecimal QtaMovOrigine = new BigDecimal(TabMovimenti.getModel().getValueAt(rigaselezionata, 5).toString());
                    if (QtaMovOrigine.compareTo(BigDecimal.ZERO) == 0) {
                        LoggerGC.ScriviErrore("QtaMovOrigine=0, esco dalla funzione");
                        return false;
                    }
                    //   BigDecimal ValoreUnitarioToken=ValoreMovOrigine.divide(QtaMovOrigine,DecimaliCalcoli+10, RoundingMode.HALF_UP).abs();

                    // ========== SE DEVO INSERIRE UN MOVIMENTO NEGATIVO CHIEDO COME CLASSIFICARLO ==========
                    if (SQta.contains("-")) {
                        scelta=0;
                        result = AppDialog.builder(owner)
                                .windowTitle("Classificazione del movimento")
                                .bodyTitle("Nuovo movimento di prelievo")
                                .showTitleInBody(true)
                                .theme()
                                .type(AppDialog.DialogType.WARNING)
                                .message("""
                    Per raggiungere la giacenza desiderata è necessario generare un movimento di prelievo di %s unità.
                    """.formatted(SQta.replace("-", "")))
                                .details("""
                                         
                    Scegli come classificare il movimento da creare.
                                         
                    Le opzioni sono le seguenti :
                                         
                    - <b>Nessuna Classificazione</b> -> Il movimento sarà da classificare successivamente
                                         
                    - <b>Cash Out</b> -> Verrà calcolata la plusvalenza
                                         
                    - <b>Commissione</b> -> Il movimento verrà gestito alla stegua di una commissione
                                         
                    - <b>Rettifica Giacenza</b> -> Non verrà calcolata la plusvalenza sul movimento
                                         
                    """)
                                .action(AppDialog.DialogAction.builder("cancel", "<html>Annulla</html>")
                                        .role(AppDialog.ActionRole.SECONDARY)
                                        .build())
                                .action(AppDialog.DialogAction.builder("later", "<html>Nessuna Classificazione</html>")
                                        .role(AppDialog.ActionRole.NEUTRAL)
                                        .build())
                                .action(AppDialog.DialogAction.builder("cashout", "<html>Cash out</html>")
                                        .role(AppDialog.ActionRole.NEUTRAL)
                                        .build())
                                .action(AppDialog.DialogAction.builder("commissione", "<html>Commissione</html>")
                                        .role(AppDialog.ActionRole.NEUTRAL)
                                        .build())
                                .action(AppDialog.DialogAction.builder("rettifica", "<html>Rettifica giacenza</html>")
                                        .role(AppDialog.ActionRole.NEUTRAL)
                                        .build())
                                .showDialog();

                        String sceltaAzione = result.getActionId();

                        if (sceltaAzione == null || sceltaAzione.equals("cancel")) {
                            return false;
                        }

                        String nota = AppDialog.showTextInputDialog(
                                owner,
                                "Nota movimento",
                                "Inserisci un'eventuale nota sul movimento",
                                "",
                                "Nota",
                                "Rettifica di Giacenza"
                        );

                        if (nota != null) {
                            owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                            String[] RTOri = MappaCryptoWallet.get(IDTrans);

                            Moneta M1 = new Moneta();
                            M1.Moneta = Moneta;
                            M1.MonetaAddress = AddressMoneta;
                            M1.Qta = SQta;
                            M1.Tipo = TipoMoneta;
                            M1.Rete = Funzioni.TrovaReteDaIMovimento(RTOri);

                            if (!nota.contains("Rettifica")) {
                                nota = "Rettifica<br>" + nota;
                            }

                            String[] IDOriSplittato = RTOri[0].split("_");
                            IDOriSplittato[4] = "PC";
                            String NuovoID = String.join("_", IDOriSplittato);
                            NuovoID = MovimentiCrypto.IncDecID(NuovoID, 1, true);

                            String tipoDaPassare = null;

                            switch (sceltaAzione) {
                                case "later" -> {
                                    // Non classifico ora il movimento
                                    scelta=1;
                                }
                                case "cashout" ->
                                {
                                    tipoDaPassare = "CASHOUT O SIMILARE";
                                    scelta=1;}
                                case "commissione" ->
                                {
                                    tipoDaPassare = "COMMISSIONE";
                                    scelta=1;}
                                case "rettifica" ->
                                {
                                    tipoDaPassare = "RETTIFICA GIACENZA";
                                    scelta=1;
                                }
                                default -> {
                                    return false;
                                }
                            }

                            String[] RT2 = MovimentiCrypto.creaMovimento(
                                    M1,
                                    null,
                                    RTOri[3],
                                    RTOri[4],
                                    0,
                                    null,
                                    null,
                                    1,
                                    1,
                                    NuovoID,
                                    nota,
                                    "M",
                                    null,
                                    tipoDaPassare,
                                    null
                            );

                            MappaCryptoWallet.put(RT2[0], RT2);
                        }
                    }

                    // ========== SE DEVO INSERIRE UN MOVIMENTO POSITIVO CHIEDO COME CLASSIFICARLO ==========
                    else {
                        scelta=0;
                        result = AppDialog.builder(owner)
                                .windowTitle("Classificazione del movimento")
                                .bodyTitle("Nuovo movimento di deposito")
                                .showTitleInBody(true)
                                .theme()
                                .type(AppDialog.DialogType.INFO)
                                .message("""
                    Per raggiungere la giacenza desiderata è necessario generare
                    un movimento di deposito di %s unità.
                    """.formatted(SQta.replace("-", "")))
                                .details("""
                                         
                    Scegli come classificare il movimento da creare.
                                         
                    Le opzioni sono le seguenti :
                                         
                    - <b>Nessuna Classificazione</b> -> Il movimento sarà da classificare successivamente
                                         
                    - <b>Provento</b> -> Lo considero alla stregua di un Provento da detenzione
                                         Verrà quindi generata una plusvalenza sul movimento pari al suo valore
                                         
                    - <b>Costo 0</b> -> Carico il movimento con Costo di carico = 0                                        
                                         
                    """)
                                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                                        .role(AppDialog.ActionRole.SECONDARY)
                                        .build())
                                .action(AppDialog.DialogAction.builder("later", "Nessuna Classificazione")
                                        .role(AppDialog.ActionRole.NEUTRAL)
                                        .build())
                                .action(AppDialog.DialogAction.builder("earn", "Provento")
                                        .role(AppDialog.ActionRole.NEUTRAL)
                                        .build())
                                .action(AppDialog.DialogAction.builder("cost0", "Costo 0")
                                        .role(AppDialog.ActionRole.NEUTRAL)
                                        .build())
                                .showDialog();

                        String sceltaAzione = result.getActionId();

                        if (sceltaAzione == null || sceltaAzione.equals("cancel")) {
                            return false;
                        }

                        String Nota = AppDialog.showTextInputDialog(
                                owner,
                                "Nota movimento",
                                "NInserisci un'eventuale nota sul movimento",
                                "",
                                "Nota",
                                "Rettifica di Giacenza"
                        );

                        // ========== INSERISCO IL MOVIMENTO SECONDO INDICAZIONI ==========
                        if (Nota != null) {
                            owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                            String[] RTOri = MappaCryptoWallet.get(IDTrans);
                            String[] IDOriSplittato = RTOri[0].split("_");

                            IDOriSplittato[4] = "DC";
                            String NuovoID = String.join("_", IDOriSplittato);
                            NuovoID = MovimentiCrypto.IncDecID(NuovoID, 1, false);

                            if (!Nota.contains("Rettifica")) {
                                Nota = "Rettifica<br>" + Nota;
                            }

                            Moneta M1 = new Moneta();
                            M1.Moneta = Moneta;
                            M1.MonetaAddress = AddressMoneta;
                            M1.Qta = SQta;
                            M1.Tipo = TipoMoneta;
                            M1.Rete = Funzioni.TrovaReteDaIMovimento(RTOri);

                            String TipoDaPassare = null;

                            switch (sceltaAzione) {
                                case "later" -> {
                                    scelta=1;
                                    // Non classifico ora il movimento
                                }
                                case "earn" ->{
                                    scelta=1;
                                    TipoDaPassare = "EARN";
                                }
                                case "cost0" ->{
                                    scelta=1;
                                    TipoDaPassare = "DEPOSITO A COSTO 0";
                                }
                                default -> {
                                    return false;
                                }
                            }

                            String[] RT1 = MovimentiCrypto.creaMovimento(
                                    null,
                                    M1,
                                    RTOri[3],
                                    RTOri[4],
                                    0,
                                    null,
                                    null,
                                    1,
                                    1,
                                    NuovoID,
                                    Nota,
                                    "M",
                                    null,
                                    TipoDaPassare,
                                    null
                            );

                            MappaCryptoWallet.put(RT1[0], RT1);
                        }
                    }
                    
                    //Avviso il programma che devo anche aggiornare la tabella crypto e ricalcolare le plusvalenze
                        
                    //Aggiorno tutto in un thread separato così viene fatto tutto in backgroud intanto che 
                    //Viene premuto sul messaggio di conferma
                 //   new Thread(() -> {
                      //  Funzioni_AggiornaTutto();
                        //Principale.TabellaCryptodaAggiornare=true;
                  //  }).start();
                    //Adesso avviso che il movimento è inserito e ricarico l'intera pagina
                    owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    if (scelta != 0 && scelta != -1) {
                        AppDialog.builder(owner)
                                .windowTitle("Movimento Creato")
                                .bodyTitle("Movimento di rettifica generato con successo!")
                                .showTitleInBody(true)
                                .theme()
                                .type(AppDialog.DialogType.INFO)
                                .details("Ricordarsi di salvare i movimenti nella sezione '<b>Transazioni Crypto</b>'.")
                                .primaryAction("ok", "OK")
                                .showDialog();
                        Principale.TabellaCryptodaAggiornare=true;
                        tuttook=true;
                        
                        //DA FARE!!!!!!
                        //Ora sistemo i valori sulla tabella principale
                        //Devo ricalcolare la tabella principale
                        //Ricaricare i dettagli e riposizionare il tutto
                        //E ricarico la tabella secondaria
                                // GiacenzeaData_CompilaTabellaToken();
                                //Tra le altre cose devo anche ricalcolare l'RW qualora sia stato già calcolato
                            }
                        } else {
                            AppDialog.builder(owner)
                                    .windowTitle("Attenzione")
                                    .bodyTitle("Valore non valido")
                                    .showTitleInBody(true)
                                    .theme()
                                    .type(AppDialog.DialogType.WARNING)
                                    .details("\"<b>"+m + "</b>\" non è un numero valido.")
                                    .primaryAction("ok", "OK")
                                    .showDialog();
                        }
                    }
                } else {
                    AppDialog.builder(owner)
                            .windowTitle("Attenzione")
                            .bodyTitle("Operazione non disponibile")
                            .showTitleInBody(true)
                            .theme()
                            .type(AppDialog.DialogType.WARNING)
                            .message("Questo tipo di operazione è consentita solo per le Crypto.")
                            .details("Per NFT e FIAT utilizzare l'inserimento manuale.")
                            .primaryAction("ok", "OK")
                            .showDialog();
                }
            } else {
                AppDialog.builder(owner)
                        .windowTitle("Attenzione")
                        .bodyTitle("Selezione non valida")
                        .showTitleInBody(true)
                        .theme()
                        .type(AppDialog.DialogType.WARNING)
                        .message("Questo tipo di operazione è consentita solo sui singoli Wallet.")
                        .details("Selezionare un singolo Wallet dal menù a tendina in alto.")
                        .primaryAction("ok", "OK")
                        .showDialog();
            }
        }
        owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        return tuttook;
    }
    
    
    

    /**
     * Costo di carico LIFO delle rimanenze per la tabella "Giacenze a data": una passata sola su
     * {@link Principale#MappaCryptoWallet} costruisce le pile dei lotti ancora in giacenza alla data
     * scelta, poi ogni riga della tabella interroga quelle pile con
     * {@link CostiCaricoRimanenze#CostoDelleRimanenze(String, String, String)}.
     * <p>
     * <b>Le regole con cui i lotti entrano ed escono sono le stesse della PARTE 2 di
     * {@code Calcoli_RT.CalcoliPlusvalenzeXAnno}</b> (movimenti interni ignorati, prelievi PTW scaricati
     * al momento del deposito e non della partenza, DTW che scarica il gruppo di provenienza): è la terza
     * copia di quelle regole nel programma — dopo il motore delle plusvalenze e il quadro RT — e dirlo è
     * meglio che nasconderlo. Il costo dei lotti non viene ricalcolato: si legge da {@code v[17]} (entrata)
     * ed è il costo di carico che il motore delle plusvalenze ha già scritto sul movimento, quindi il
     * numero mostrato qui non può divergere da quello fiscale per un calcolo diverso.
     * <p>
     * Due scelte che il disegno dà per assodate:
     * <ul>
     * <li><b>La passata non è filtrata per wallet, la lettura sì.</b> Le quantità della tabella si possono
     * filtrare a monte perché sommare è un'operazione locale; il LIFO no — togliere dal flusso i movimenti
     * di un altro wallet cambia quali lotti restano. Soprattutto, un trasferimento interno non porta con sé
     * nessun costo ({@code v[16]} e {@code v[17]} sono entrambi vuoti quando la controparte è dello stesso
     * gruppo), quindi una pila costruita sui soli movimenti del wallet selezionato mostrerebbe quantità
     * positive a costo zero per ogni moneta arrivata da un giroconto.</li>
     * <li><b>La pila è per gruppo wallet, come nel motore, ma la chiave della moneta è quella della riga
     * della tabella</b> ({@code Moneta;Tipo;Address;Rete}) e non il solo simbolo: due righe con lo stesso
     * simbolo su reti diverse sono due righe distinte e leggendo entrambe dalla stessa pila si conterebbe
     * due volte lo stesso lotto.</li>
     * </ul>
     *
     * @param DataRiferimento istante (escluso) fino al quale considerare i movimenti, come nel ciclo che riempie la tabella
     * @return le pile dei lotti residui, da interrogare riga per riga
     */
    public static CostiCaricoRimanenze CalcolaCostiCaricoRimanenze(long DataRiferimento) {
        return CalcolaCostiCaricoRimanenze(DataRiferimento, null);
    }

    /**
     * Come {@link #CalcolaCostiCaricoRimanenze(long)}, ma interrompibile: su un archivio da centomila
     * movimenti la passata dura abbastanza da dover rispondere al pulsante <i>Annulla</i> della finestra
     * di avanzamento, esattamente come fa il ciclo che riempie poi la tabella.
     * @param DataRiferimento istante (escluso) fino al quale considerare i movimenti
     * @param Interrotto sorgente dello stato di annullamento, interrogata ogni {@value #MOVIMENTI_PER_CONTROLLO_INTERRUZIONE} movimenti; {@code null} per non interrompere mai
     * @return le pile dei lotti residui; se l'elaborazione è stata interrotta sono <b>incomplete</b> e il chiamante non deve mostrarle
     */
    public static CostiCaricoRimanenze CalcolaCostiCaricoRimanenze(long DataRiferimento,
            java.util.function.BooleanSupplier Interrotto) {

        CostiCaricoRimanenze Costi = new CostiCaricoRimanenze();
        int Esaminati = 0;
        for (String[] v : MappaCryptoWallet.values()) {
            //Post-incremento a partire da zero : il primo controllo è sul primo movimento, così una
            //richiesta di annullamento già in piedi non fa comunque partire la passata
            if (Interrotto != null && Esaminati++ % MOVIMENTI_PER_CONTROLLO_INTERRUZIONE == 0
                    && Interrotto.getAsBoolean()) {
                return Costi;
            }
            if (!(FunzioniDate.ConvertiDatainLong(v[1]) < DataRiferimento)) {
                continue;
            }
            String Rete = Funzioni.TrovaReteDaIMovimento(v);
            String Gruppo = Costi.GruppoDelMovimento(v[3]);
            String IDTS[] = v[0].split("_");
            boolean MovimentoInterno = IDTS.length > 4 && IDTS[4].equalsIgnoreCase("TI");

            //USCITA : scarico i lotti, tranne che per i movimenti interni e per i PTW (prelievi verso un
            //wallet proprio), che vengono scaricati dal DTW corrispondente quando arrivano a destinazione
            if (!v[9].isBlank() && !v[9].equalsIgnoreCase("FIAT") && !v[16].isBlank()
                    && !v[18].contains("PTW") && !MovimentoInterno) {
                Costi.TogliLotti(Gruppo, ChiaveRiga(v[8], v[9], v[26], Rete), v[10]);
            }

            //ENTRATA : carico il lotto al costo di carico scritto dal motore delle plusvalenze
            if (!v[12].isBlank() && !v[12].equalsIgnoreCase("FIAT") && !v[17].isBlank() && !MovimentoInterno) {
                if (!v[18].contains("DTW")) {
                    Costi.InserisciLotto(Gruppo, ChiaveRiga(v[11], v[12], v[28], Rete), v[13], v[17], v[0]);
                } else {
                    //Deposito da un wallet proprio : se la controparte sta in un gruppo diverso il costo di
                    //carico si sposta da quel gruppo a questo, altrimenti non si muove nulla
                    String Controparte[] = Calcoli_PlusvalenzeNew.RitornaIDeGruppoControparteSeGruppoDiverso(v);
                    if (Controparte[0] != null) {
                        Costi.InserisciLotto(Gruppo, ChiaveRiga(v[11], v[12], v[28], Rete), v[13], v[17], v[0]);
                        String Mov[] = MappaCryptoWallet.get(Controparte[0]);
                        if (Mov != null) {
                            String ReteControparte = Funzioni.TrovaReteDaIMovimento(Mov);
                            Costi.TogliLotti(Controparte[1], ChiaveRiga(Mov[8], Mov[9], Mov[26], ReteControparte), Mov[10]);
                        }
                    }
                }
            }
        }
        return Costi;
    }

    /**
     * Ogni quanti movimenti la passata dei costi di carico controlla se l'utente ha annullato. Non a ogni
     * movimento perché {@code Download.FineThread()} non è un semplice getter.
     */
    private static final int MOVIMENTI_PER_CONTROLLO_INTERRUZIONE = 2000;

    /**
     * Compone la chiave con cui una moneta è identificata nella tabella "Giacenze a data", con la stessa
     * regola del ciclo che riempie la tabella: se la rete manca, manca anche l'address (i due vanno a
     * braccetto e tenerne uno solo spezzerebbe in due righe la stessa giacenza).
     * @param Moneta simbolo della criptoattività
     * @param Tipo tipo della moneta (Crypto, NFT, FIAT, ...)
     * @param Address address del contratto, o vuoto
     * @param Rete rete/chain di appartenenza, o vuota
     * @return la chiave {@code Moneta;Tipo;Address;Rete}
     */
    public static String ChiaveRiga(String Moneta, String Tipo, String Address, String Rete) {
        if (Rete == null || Rete.isBlank()) {
            Rete = "";
            Address = "";
        }
        if (Address == null) {
            Address = "";
        }
        if (Tipo == null) {
            Tipo = "";
        }
        return Moneta + ";" + Tipo + ";" + Address + ";" + Rete;
    }

    /**
     * Le pile LIFO dei lotti ancora in giacenza, per gruppo wallet e per riga della tabella
     * "Giacenze a data". Si costruisce con {@link #CalcolaCostiCaricoRimanenze(long)}.
     */
    public static final class CostiCaricoRimanenze {

        /** Gruppo wallet → chiave di riga → pila dei lotti residui ({@code {quantità, costo, ID movimento}}). */
        private final Map<String, Map<String, ArrayDeque<String[]>>> Pile = new TreeMap<>();

        /**
         * Copia dell'opzione {@code PlusXWallet}: se è spenta il motore delle plusvalenze usa una pila sola
         * ("Wallet 01") e qui si fa lo stesso, altrimenti le due letture divergerebbero.
         */
        private final boolean PlusXWallet;

        private CostiCaricoRimanenze() {
            String PlusXW = DatabaseH2.Pers_Opzioni_Leggi("PlusXWallet");
            PlusXWallet = (PlusXW != null && PlusXW.equalsIgnoreCase("SI"));
        }

        /** @return il gruppo wallet su cui tenere la pila dei movimenti del wallet indicato */
        private String GruppoDelMovimento(String Wallet) {
            if (!PlusXWallet) {
                return "Wallet 01";
            }
            return DatabaseH2.Pers_GruppoWallet_Leggi(Wallet, true);
        }

        /**
         * Aggiunge un lotto in cima alla pila. Quantità vuote, non numeriche o nulle non producono nessun
         * lotto; un costo non numerico vale zero (il lotto deve comunque esistere, o la quantità
         * successivamente scaricata non troverebbe nulla e si sfalserebbe tutto il resto della pila).
         */
        private void InserisciLotto(String Gruppo, String Chiave, String Qta, String Costo, String ID) {
            if (Gruppo == null || Qta == null || Qta.isBlank()) {
                return;
            }
            BigDecimal QtaLotto;
            try {
                QtaLotto = new BigDecimal(Qta.trim()).abs();
            } catch (NumberFormatException ex) {
                return;
            }
            if (QtaLotto.signum() == 0) {
                return;
            }
            BigDecimal CostoLotto;
            try {
                CostoLotto = new BigDecimal(Costo.trim()).abs();
            } catch (NumberFormatException ex) {
                CostoLotto = BigDecimal.ZERO;
            }
            Pile.computeIfAbsent(Gruppo, k -> new TreeMap<>())
                    .computeIfAbsent(Chiave, k -> new ArrayDeque<>())
                    .push(new String[]{QtaLotto.toPlainString(), CostoLotto.toPlainString(), ID});
        }

        /**
         * Scarica dalla pila, dall'ultimo entrato, i lotti necessari a coprire la quantità indicata. Un
         * lotto più capiente del necessario rientra in pila per la parte residua, col costo proporzionato.
         * Se la pila finisce prima non si segnala nulla: la mancanza è già contata dal motore delle
         * plusvalenze ("parte del LiFo mancante") e la riga si vede comunque in rosso per la giacenza negativa.
         */
        private void TogliLotti(String Gruppo, String Chiave, String Qta) {
            if (Gruppo == null || Qta == null || Qta.isBlank()) {
                return;
            }
            Map<String, ArrayDeque<String[]>> PerChiave = Pile.get(Gruppo);
            if (PerChiave == null) {
                return;
            }
            ArrayDeque<String[]> Pila = PerChiave.get(Chiave);
            if (Pila == null) {
                return;
            }
            BigDecimal Rimanente;
            try {
                Rimanente = new BigDecimal(Qta.trim()).abs();
            } catch (NumberFormatException ex) {
                return;
            }
            while (Rimanente.signum() > 0 && !Pila.isEmpty()) {
                String Lotto[] = Pila.pop();
                BigDecimal QtaLotto = new BigDecimal(Lotto[0]);
                BigDecimal CostoLotto = new BigDecimal(Lotto[1]);
                if (QtaLotto.compareTo(Rimanente) <= 0) {
                    Rimanente = Rimanente.subtract(QtaLotto);
                } else {
                    BigDecimal QtaResidua = QtaLotto.subtract(Rimanente);
                    BigDecimal CostoResiduo = CostoLotto
                            .divide(QtaLotto, VarStatiche.DecimaliCalcoli + 10, RoundingMode.HALF_UP)
                            .multiply(QtaResidua)
                            .setScale(VarStatiche.DecimaliCalcoli, RoundingMode.HALF_UP);
                    Pila.push(new String[]{QtaResidua.toPlainString(), CostoResiduo.toPlainString(), Lotto[2]});
                    Rimanente = BigDecimal.ZERO;
                }
            }
        }

        /**
         * Costo di carico dei lotti che coprono la giacenza mostrata su una riga della tabella.
         * <p>
         * I lotti dei gruppi wallet in gioco vengono uniti e riordinati per ID del movimento di origine —
         * che è cronologico, visto che l'ID comincia con {@code yyyyMMddHHmmss} — così la lettura è la
         * stessa qualunque sia l'ampiezza della selezione: su un gruppo intero, o su "Tutti", la quantità
         * mostrata copre l'intera pila e il costo è quello di tutte le rimanenze; su un singolo wallet copre
         * solo la parte più recente, che è la lettura LIFO della domanda "quanto è costato quello che resta".
         *
         * @param Wallet selezione della combo wallet ("Tutti", un nome di wallet, o "Gruppo : X ( alias )")
         * @param Chiave chiave della riga, da {@link Principale_GiacenzeaData#ChiaveRiga(String, String, String, String)}
         * @param Qta giacenza mostrata sulla riga
         * @return il costo di carico con due decimali; {@code "0.00"} se la giacenza è nulla o negativa (non ci sono rimanenze da valorizzare) o se non si trova nessun lotto
         */
        public String CostoDelleRimanenze(String Wallet, String Chiave, String Qta) {
            BigDecimal Richiesta;
            try {
                Richiesta = new BigDecimal(Qta.trim());
            } catch (NumberFormatException | NullPointerException ex) {
                return "0.00";
            }
            if (Richiesta.signum() <= 0) {
                return "0.00";
            }
            List<String[]> Lotti = new ArrayList<>();
            for (String Gruppo : GruppiInSelezione(Wallet)) {
                Map<String, ArrayDeque<String[]>> PerChiave = Pile.get(Gruppo);
                if (PerChiave == null) {
                    continue;
                }
                ArrayDeque<String[]> Pila = PerChiave.get(Chiave);
                if (Pila != null) {
                    Lotti.addAll(Pila);
                }
            }
            //Dal più recente al più vecchio : l'ID comincia con yyyyMMddHHmmss, quindi l'ordine
            //alfabetico decrescente è già quello cronologico inverso richiesto dal LIFO
            Lotti.sort((a, b) -> b[2].compareToIgnoreCase(a[2]));

            BigDecimal Costo = BigDecimal.ZERO;
            for (String[] Lotto : Lotti) {
                if (Richiesta.signum() <= 0) {
                    break;
                }
                BigDecimal QtaLotto = new BigDecimal(Lotto[0]);
                BigDecimal CostoLotto = new BigDecimal(Lotto[1]);
                if (QtaLotto.compareTo(Richiesta) <= 0) {
                    Richiesta = Richiesta.subtract(QtaLotto);
                    Costo = Costo.add(CostoLotto);
                } else {
                    Costo = Costo.add(CostoLotto
                            .divide(QtaLotto, VarStatiche.DecimaliCalcoli + 10, RoundingMode.HALF_UP)
                            .multiply(Richiesta));
                    Richiesta = BigDecimal.ZERO;
                }
            }
            return Costo.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }

        /**
         * I gruppi wallet le cui pile vanno lette per la selezione corrente della combo wallet.
         * Con {@code PlusXWallet} spenta la pila è una sola e la selezione non la restringe.
         */
        private Collection<String> GruppiInSelezione(String Wallet) {
            if (!PlusXWallet || Wallet == null || Wallet.isBlank() || Wallet.equalsIgnoreCase("tutti")) {
                return Pile.keySet();
            }
            if (Wallet.contains("Gruppo :")) {
                return List.of(Wallet.split(" : ")[1].split("\\(")[0].trim());
            }
            //Un wallet non più presente in WALLETGRUPPO non ha lotti da leggere : meglio nessun costo
            //che il costo di un gruppo scelto a caso
            String Gruppo = DatabaseH2.Pers_GruppoWallet_Leggi(Wallet, false);
            return Gruppo == null ? List.of() : List.of(Gruppo);
        }
    }

    //Queste 3 classi serviranno per sistemare la parte relativa al calcolo delle giacenzeadata
    //per ora non utilizzata
    
    public static class StatoTabelle {

        int rigaSelTabPrincipale = -1;
        String walletToken = "";
        int scrollValuePrincipale = 0;

        int rigaSelTabMov = -1;
        String movSelezionato = "";
        int scrollValueMovimenti = 0;
    }

    public static class ParametriCalcoloGiacenze {

        long dataRiferimento = 0;
        String wallet = "";
        String sottoWallet = "";
        boolean mostraQtaZero = false;
        boolean nascondiScam = false;
    }

    public static class RisultatoCalcoloGiacenze {

        Map<String, Object[]> tabellaToken = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        java.util.List<Object[]> righeTabella = new java.util.ArrayList<>();
        BigDecimal totaleEuro = BigDecimal.ZERO;
        String wallet = "";
        String sottoWallet = "";
        boolean interrotto = false;
    }
}
