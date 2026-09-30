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
                    Per raggiungere la giacenza desiderata è necessario generare un movimento di prelievo di %s.
                    """.formatted(DescrizioneQtaEValore(Moneta, AddressMoneta, Funzioni.TrovaReteDaIMovimento(mov),
                                    SQta.replace("-", ""), IDTrans, owner)))
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
                    un movimento di deposito di %s.
                    """.formatted(DescrizioneQtaEValore(Moneta, AddressMoneta, Funzioni.TrovaReteDaIMovimento(mov),
                                    SQta.replace("-", ""), IDTrans, owner)))
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
                                "Inserisci un'eventuale nota sul movimento",
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
            ElaboraLotti(Costi, v, null);
        }
        return Costi;
    }

    /**
     * Carica e scarica i lotti di un movimento sulle pile, con le regole descritte in
     * {@link #CalcolaCostiCaricoRimanenze(long)}. È l'unico posto in cui queste regole stanno in questa
     * classe: la passata di "Giacenze a data" e quella del dettaglio movimento
     * ({@link #GiacenzeAttornoAlMovimento(String)}) passano entrambe di qui.
     * <p>
     * Il filtro sui simboli si applica a ogni singolo carico/scarico e non al movimento intero: un PTW e il
     * suo DTW possono portare simboli diversi, e saltare il movimento perderebbe lo scarico della
     * controparte. Rete e gruppo si calcolano solo quando un simbolo corrisponde, che è il motivo per cui
     * la passata filtrata costa una frazione di quella completa.
     * @param Simboli se non {@code null}, solo le monete con uno di questi simboli toccano le pile
     */
    private static void ElaboraLotti(CostiCaricoRimanenze Costi, String[] v, java.util.Set<String> Simboli) {
        boolean Uscita = !v[9].isBlank() && !v[9].equalsIgnoreCase("FIAT") && !v[16].isBlank()
                && !v[18].contains("PTW") && (Simboli == null || Simboli.contains(v[8]));
        boolean Entrata = !v[12].isBlank() && !v[12].equalsIgnoreCase("FIAT") && !v[17].isBlank();
        if (!Uscita && !Entrata) {
            return;
        }
        String IDTS[] = v[0].split("_");
        if (IDTS.length > 4 && IDTS[4].equalsIgnoreCase("TI")) {
            //Movimento interno : non tocca le pile
            return;
        }
        String Rete = null;
        String Gruppo = null;

        //USCITA : scarico i lotti, tranne che per i movimenti interni e per i PTW (prelievi verso un
        //wallet proprio), che vengono scaricati dal DTW corrispondente quando arrivano a destinazione
        if (Uscita) {
            Rete = Funzioni.TrovaReteDaIMovimento(v);
            Gruppo = Costi.GruppoDelMovimento(v[3]);
            Costi.TogliLotti(Gruppo, ChiaveRiga(v[8], v[9], v[26], Rete), v[10]);
        }

        //ENTRATA : carico il lotto al costo di carico scritto dal motore delle plusvalenze
        if (Entrata) {
            if (!v[18].contains("DTW")) {
                if (Simboli == null || Simboli.contains(v[11])) {
                    if (Gruppo == null) {
                        Rete = Funzioni.TrovaReteDaIMovimento(v);
                        Gruppo = Costi.GruppoDelMovimento(v[3]);
                    }
                    Costi.InserisciLotto(Gruppo, ChiaveRiga(v[11], v[12], v[28], Rete), v[13], v[17], v[0]);
                }
            } else {
                //Deposito da un wallet proprio : se la controparte sta in un gruppo diverso il costo di
                //carico si sposta da quel gruppo a questo, altrimenti non si muove nulla
                String Mov[] = null;
                boolean CaricoQui = Simboli == null || Simboli.contains(v[11]);
                boolean ScaricoControparte = Simboli == null;
                if (!ScaricoControparte) {
                    //La controparte si guarda solo se il suo simbolo interessa : la ricerca costa
                    for (String IdC : v[20].split(",")) {
                        String C[] = IdC.isBlank() ? null : MappaCryptoWallet.get(IdC);
                        if (C != null && C[18].contains("PTW") && Simboli.contains(C[8])) {
                            ScaricoControparte = true;
                        }
                    }
                }
                if (!CaricoQui && !ScaricoControparte) {
                    return;
                }
                String Controparte[] = Calcoli_PlusvalenzeNew.RitornaIDeGruppoControparteSeGruppoDiverso(v);
                if (Controparte[0] != null) {
                    if (CaricoQui) {
                        if (Gruppo == null) {
                            Rete = Funzioni.TrovaReteDaIMovimento(v);
                            Gruppo = Costi.GruppoDelMovimento(v[3]);
                        }
                        Costi.InserisciLotto(Gruppo, ChiaveRiga(v[11], v[12], v[28], Rete), v[13], v[17], v[0]);
                    }
                    Mov = MappaCryptoWallet.get(Controparte[0]);
                    if (Mov != null && (Simboli == null || Simboli.contains(Mov[8]))) {
                        String ReteControparte = Funzioni.TrovaReteDaIMovimento(Mov);
                        Costi.TogliLotti(Controparte[1], ChiaveRiga(Mov[8], Mov[9], Mov[26], ReteControparte), Mov[10]);
                    }
                }
            }
        }
    }

    /**
     * Ogni quanti movimenti la passata dei costi di carico controlla se l'utente ha annullato. Non a ogni
     * movimento perché {@code Download.FineThread()} non è un semplice getter.
     */
    private static final int MOVIMENTI_PER_CONTROLLO_INTERRUZIONE = 2000;

    /** Indici dei tre livelli in {@link GiacenzeMoneta#QtaPrima} e compagni. */
    public static final int LIVELLO_GLOBALE = 0, LIVELLO_GRUPPO = 1, LIVELLO_WALLET = 2;

    /**
     * Giacenza di una moneta del movimento immediatamente prima e immediatamente dopo il movimento stesso,
     * su tre livelli: tutti i wallet, il gruppo wallet del movimento, il wallet ({@code v[3]}) del movimento.
     * I costi sono {@code null} per le monete FIAT, che non hanno pile LIFO.
     */
    public static final class GiacenzeMoneta {
        public final String Moneta;
        public final String Tipo;
        public final String Chiave;
        /** Prezzo unitario della moneta nel movimento stesso; {@code null} se il movimento non è valorizzato. */
        public final BigDecimal PrezzoUnitario;
        public final BigDecimal[] QtaPrima = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        public final BigDecimal[] QtaDopo = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        public final String[] CostoPrima = new String[3];
        public final String[] CostoDopo = new String[3];

        private GiacenzeMoneta(String Moneta, String Tipo, String Chiave, BigDecimal PrezzoUnitario) {
            this.Moneta = Moneta;
            this.Tipo = Tipo;
            this.Chiave = Chiave;
            this.PrezzoUnitario = PrezzoUnitario;
        }

        /** @return {@code true} se la moneta è FIAT (solo quantità, nessun costo di carico) */
        public boolean isFiat() {
            return Tipo != null && Tipo.equalsIgnoreCase("FIAT");
        }
    }

    /**
     * Le giacenze delle monete coinvolte in un movimento prima e dopo il movimento, per il dettaglio
     * movimento. Una sola passata su {@link Principale#MappaCryptoWallet} fino al movimento (in ordine di
     * ID, cioè nell'ordine del motore delle plusvalenze: una data di taglio confonderebbe i movimenti dello
     * stesso secondo), limitata ai simboli del movimento.
     * <p>
     * Le quantità sono le stesse somme della tabella "Giacenze a data" (stessa chiave di riga, stesso
     * criterio di gruppo e di wallet); i costi di carico sono le stesse pile di
     * {@link #CalcolaCostiCaricoRimanenze(long)}, lette con {@link CostiCaricoRimanenze}. Ne ereditano
     * quindi due approssimazioni, dichiarate e non nascoste:
     * <ul>
     * <li>il costo a livello di wallet è quello dei lotti più recenti del gruppo che coprono la giacenza
     * del wallet, perché le pile esistono per gruppo e non per wallet;</li>
     * <li>un prelievo verso un wallet proprio (PTW) non scarica i lotti del gruppo: lo fa il deposito
     * corrispondente (DTW) all'arrivo, se la destinazione è in un altro gruppo.</li>
     * </ul>
     * I costi vengono da {@code v[16]}/{@code v[17]}, cioè dall'ultimo giro del motore delle plusvalenze:
     * subito dopo una modifica non ancora ricalcolata sono quelli di prima della modifica.
     * <p>
     * Nessun prezzo viene cercato: il controvalore usa il prezzo unitario del movimento stesso, che per
     * "prima" e "dopo" è lo stesso istante. Così la navigazione fra un movimento e l'altro non tocca mai
     * la rete.
     *
     * @param ID ID del movimento
     * @return una voce per moneta coinvolta (uscita, poi entrata), vuota se il movimento non esiste
     */
    public static List<GiacenzeMoneta> GiacenzeAttornoAlMovimento(String ID) {
        return CalcolaDettaglio(ID).Monete;
    }

    /**
     * Quello che il dettaglio movimento calcola con una passata: le giacenze prima/dopo di
     * {@link #GiacenzeAttornoAlMovimento(String)} e il costo di carico informativo delle gambe su cui il
     * motore delle plusvalenze non ne ha scritto uno.
     */
    public static final class DettaglioGiacenze {
        public final List<GiacenzeMoneta> Monete = new ArrayList<>();
        /**
         * Costo di carico solo informativo della quantità mossa, {@code [0]} uscita e {@code [1]} entrata;
         * {@code null} se il motore ha già scritto il costo ({@code v[16]}/{@code v[17]}) o se non ha senso.
         * È il costo dei lotti più recenti che coprono la quantità, letti dalla pila del gruppo wallet
         * (o dalla pila unica, con il LIFO globale) subito prima del movimento, senza toglierli: la stessa
         * lettura che il motore fa per un PTW diretto a un altro gruppo. Per l'entrata vale solo sui
         * trasferimenti (DTW, TI), perché il PTW non scarica la pila e i lotti in cima sono quelli partiti;
         * un deposito non classificato non ha un costo da dedurre.
         */
        public final String[] CostoInformativo = new String[2];
    }

    /**
     * Esegue la passata del dettaglio movimento, vedi {@link DettaglioGiacenze}.
     * @param ID ID del movimento
     * @return il risultato, vuoto se il movimento non esiste o non muove monete
     */
    public static DettaglioGiacenze CalcolaDettaglio(String ID) {
        DettaglioGiacenze Dettaglio = new DettaglioGiacenze();
        List<GiacenzeMoneta> Ris = Dettaglio.Monete;
        String[] Mov = MappaCryptoWallet.get(ID);
        if (Mov == null) {
            return Dettaglio;
        }
        String ReteMov = Funzioni.TrovaReteDaIMovimento(Mov);
        String Wallet = Mov[3].trim();
        String Gruppo = Wallet.isEmpty() ? null : DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3], true);

        Map<String, GiacenzeMoneta> Monete = new java.util.LinkedHashMap<>();
        java.util.Set<String> Simboli = new java.util.HashSet<>();
        int Gambe[][] = {{8, 9, 10, 26}, {11, 12, 13, 28}};
        for (int[] g : Gambe) {
            if (Mov[g[0]].isBlank()) {
                continue;
            }
            String Chiave = ChiaveRiga(Mov[g[0]], Mov[g[1]], Mov[g[3]], ReteMov);
            if (!Monete.containsKey(Chiave)) {
                Monete.put(Chiave, new GiacenzeMoneta(Mov[g[0]], Mov[g[1]], Chiave,
                        PrezzoUnitarioNelMovimento(Mov, Mov[g[0]], Mov[g[2]])));
                Simboli.add(Mov[g[0]]);
            }
        }
        if (Monete.isEmpty()) {
            return Dettaglio;
        }

        CostiCaricoRimanenze Costi = new CostiCaricoRimanenze();
        Map<String, BigDecimal[]> Correnti = new java.util.HashMap<>();
        for (String Chiave : Monete.keySet()) {
            Correnti.put(Chiave, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (String[] v : MappaCryptoWallet.headMap(ID, false).values()) {
            SommaQuantita(v, Simboli, Correnti, Gruppo, Wallet);
            ElaboraLotti(Costi, v, Simboli);
        }
        FotografaGiacenze(Monete, Correnti, Costi, Gruppo, Wallet, true);
        if (Gruppo != null) {
            String IDTS[] = Mov[0].split("_");
            boolean Interno = IDTS.length > 4 && IDTS[4].equalsIgnoreCase("TI");
            if (Mov[16].isBlank() && isCrypto(Mov[8], Mov[9])) {
                Dettaglio.CostoInformativo[0] = Costi.CostoDelGruppo(Gruppo,
                        ChiaveRiga(Mov[8], Mov[9], Mov[26], ReteMov), QtaAssoluta(Mov[10]));
            }
            if (Mov[17].isBlank() && isCrypto(Mov[11], Mov[12]) && (Interno || Mov[18].contains("DTW"))) {
                Dettaglio.CostoInformativo[1] = Costi.CostoDelGruppo(Gruppo,
                        ChiaveRiga(Mov[11], Mov[12], Mov[28], ReteMov), QtaAssoluta(Mov[13]));
            }
        }
        SommaQuantita(Mov, Simboli, Correnti, Gruppo, Wallet);
        ElaboraLotti(Costi, Mov, Simboli);
        FotografaGiacenze(Monete, Correnti, Costi, Gruppo, Wallet, false);

        Ris.addAll(Monete.values());
        return Dettaglio;
    }

    /** @return {@code true} se la gamba ha una moneta e non è FIAT */
    private static boolean isCrypto(String Moneta, String Tipo) {
        return !Moneta.isBlank() && !Tipo.isBlank() && !Tipo.equalsIgnoreCase("FIAT");
    }

    /** @return il valore assoluto della quantità, o {@code "0"} se non è un numero */
    private static String QtaAssoluta(String Qta) {
        try {
            return new BigDecimal(Qta.trim()).abs().toPlainString();
        } catch (NumberFormatException ex) {
            return "0";
        }
    }

    /** Aggiunge le quantità del movimento ai contatori dei tre livelli, con i criteri di "Giacenze a data". */
    private static void SommaQuantita(String[] v, java.util.Set<String> Simboli,
            Map<String, BigDecimal[]> Correnti, String Gruppo, String Wallet) {
        boolean Uscita = !v[8].isBlank() && Simboli.contains(v[8]);
        boolean Entrata = !v[11].isBlank() && Simboli.contains(v[11]);
        if (!Uscita && !Entrata) {
            return;
        }
        String Rete = Funzioni.TrovaReteDaIMovimento(v);
        boolean StessoWallet = Wallet.equalsIgnoreCase(v[3].trim());
        boolean StessoGruppo = Gruppo != null && !v[3].isBlank()
                && Gruppo.equals(DatabaseH2.Pers_GruppoWallet_Leggi(v[3], true));
        int Gambe[][] = {{8, 9, 10, 26}, {11, 12, 13, 28}};
        for (int[] g : Gambe) {
            if (v[g[0]].isBlank() || !Simboli.contains(v[g[0]])) {
                continue;
            }
            BigDecimal Contatori[] = Correnti.get(ChiaveRiga(v[g[0]], v[g[1]], v[g[3]], Rete));
            if (Contatori == null) {
                continue;
            }
            BigDecimal Qta;
            try {
                Qta = new BigDecimal(v[g[2]].trim());
            } catch (NumberFormatException ex) {
                continue;
            }
            Contatori[LIVELLO_GLOBALE] = Contatori[LIVELLO_GLOBALE].add(Qta);
            if (StessoGruppo) {
                Contatori[LIVELLO_GRUPPO] = Contatori[LIVELLO_GRUPPO].add(Qta);
            }
            if (StessoWallet) {
                Contatori[LIVELLO_WALLET] = Contatori[LIVELLO_WALLET].add(Qta);
            }
        }
    }

    /** Copia i contatori correnti e il costo delle rimanenze corrispondente nella parte "prima" o "dopo". */
    private static void FotografaGiacenze(Map<String, GiacenzeMoneta> Monete, Map<String, BigDecimal[]> Correnti,
            CostiCaricoRimanenze Costi, String Gruppo, String Wallet, boolean Prima) {
        for (GiacenzeMoneta M : Monete.values()) {
            BigDecimal Contatori[] = Correnti.get(M.Chiave);
            BigDecimal Qta[] = Prima ? M.QtaPrima : M.QtaDopo;
            String Costo[] = Prima ? M.CostoPrima : M.CostoDopo;
            for (int l = 0; l < 3; l++) {
                Qta[l] = Contatori[l].stripTrailingZeros();
            }
            if (M.isFiat()) {
                continue;
            }
            Costo[LIVELLO_GLOBALE] = Costi.CostoDelleRimanenze("Tutti", M.Chiave, Qta[LIVELLO_GLOBALE].toPlainString());
            Costo[LIVELLO_GRUPPO] = Gruppo == null ? null
                    : Costi.CostoDelGruppo(Gruppo, M.Chiave, Qta[LIVELLO_GRUPPO].toPlainString());
            Costo[LIVELLO_WALLET] = Wallet.isEmpty() ? null
                    : Costi.CostoDelleRimanenze(Wallet, M.Chiave, Qta[LIVELLO_WALLET].toPlainString());
        }
    }

    /**
     * Prezzo unitario di una moneta nel movimento: quello della fonte in {@code v[40]} se la moneta è la
     * moneta di riferimento del prezzo (non arrotondato), altrimenti valore del movimento diviso quantità.
     * @return il prezzo, o {@code null} se il movimento non ha valore
     */
    static BigDecimal PrezzoUnitarioNelMovimento(String[] Mov, String Moneta, String Qta) {
        if (!Mov[40].isBlank()) {
            String VSplit[] = Mov[40].split("\\|", -1);
            if (VSplit.length > 2 && Prezzi.InfoPrezzo.SeparaNome(VSplit[0])[0].equalsIgnoreCase(Moneta)) {
                try {
                    return new BigDecimal(VSplit[2].trim()).abs();
                } catch (NumberFormatException ex) {
                    //si ripiega sul valore del movimento
                }
            }
        }
        if (Mov[15].isBlank()) {
            return null;
        }
        try {
            BigDecimal Q = new BigDecimal(Qta.trim()).abs();
            if (Q.signum() == 0) {
                return null;
            }
            return new BigDecimal(Mov[15].trim()).abs()
                    .divide(Q, VarStatiche.DecimaliCalcoli + 10, RoundingMode.HALF_UP).stripTrailingZeros();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Le righe {etichetta, valore HTML} della sezione giacenze del dettaglio movimento, una per moneta
     * coinvolta, da {@link #GiacenzeAttornoAlMovimento(String)}.
     * @param ID ID del movimento
     * @return le righe, vuota se il movimento non muove nessuna moneta
     */
    public static List<String[]> RigheDettaglio(String ID) {
        return RigheDettaglio(ID, GiacenzeAttornoAlMovimento(ID));
    }

    /**
     * Come {@link #RigheDettaglio(String)}, a partire da giacenze già calcolate.
     */
    public static List<String[]> RigheDettaglio(String ID, List<GiacenzeMoneta> Giacenze) {
        List<String[]> Righe = new ArrayList<>();
        String[] Mov = MappaCryptoWallet.get(ID);
        if (Mov == null) {
            return Righe;
        }
        String Gruppo = Mov[3].isBlank() ? "" : DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3], true);
        String Etichette[] = {"Tutti i wallet", Gruppo.isBlank() ? "" : "Gruppo " + Gruppo, Mov[3].trim()};
        for (GiacenzeMoneta M : Giacenze) {
            StringBuilder S = new StringBuilder("<html>");
            S.append("Prezzo unitario nel movimento: <b>")
                    .append(M.PrezzoUnitario == null ? "non disponibile" : "€ " + M.PrezzoUnitario.toPlainString())
                    .append("</b>");
            S.append("<table cellspacing=0 cellpadding=2>");
            //Il nome del livello sta su una riga a tutta larghezza: un wallet DeFi arriva a 50 caratteri e
            //come colonna allargherebbe la tabella oltre la cella, che la taglierebbe
            int Colonne = M.isFiat() ? 3 : 5;
            S.append("<tr><th></th><th align=right>Quantità</th>")
                    .append("<th align=right>Controvalore</th>");
            if (!M.isFiat()) {
                S.append("<th align=right>Costo di carico</th><th align=right>Costo unitario</th>");
            }
            S.append("</tr>");
            for (int l = 0; l < 3; l++) {
                if (Etichette[l].isBlank()) {
                    continue;
                }
                S.append("<tr><td colspan=").append(Colonne).append("><b>").append(Etichette[l]).append("</b></td></tr>");
                RigaGiacenza(S, M, "prima", M.QtaPrima[l], M.CostoPrima[l]);
                RigaGiacenza(S, M, "dopo", M.QtaDopo[l], M.CostoDopo[l]);
            }
            S.append("</table>");
            if (Mov[18].contains("PTW") && !M.isFiat()) {
                S.append("<i>Prelievo verso un wallet proprio: se la destinazione è in un altro gruppo,<br>")
                        .append("il costo di carico lascia questo gruppo solo all'arrivo.</i>");
            }
            S.append("</html>");
            Righe.add(new String[]{"Giacenze " + M.Moneta, S.toString()});
        }
        return Righe;
    }

    private static void RigaGiacenza(StringBuilder S, GiacenzeMoneta M, String Momento,
            BigDecimal Qta, String Costo) {
        S.append("<tr><td>&nbsp;&nbsp;").append(Momento).append("</td>");
        S.append("<td align=right>").append(Qta.toPlainString()).append("</td>");
        S.append("<td align=right>").append(M.PrezzoUnitario == null ? "n.d."
                : "€ " + M.PrezzoUnitario.multiply(Qta).setScale(2, RoundingMode.HALF_UP).toPlainString())
                .append("</td>");
        if (!M.isFiat()) {
            String Unitario = "-";
            if (Costo != null && Qta.signum() > 0) {
                Unitario = "€ " + new BigDecimal(Costo).divide(Qta, 10, RoundingMode.HALF_UP)
                        .stripTrailingZeros().toPlainString();
            }
            S.append("<td align=right>").append(Costo == null ? "-" : "€ " + Costo).append("</td>");
            S.append("<td align=right>").append(Unitario).append("</td>");
        }
        S.append("</tr>");
    }

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
            return CostoDaiGruppi(GruppiInSelezione(Wallet), Chiave, Qta);
        }

        /**
         * Come {@link #CostoDelleRimanenze(String, String, String)}, ma per un gruppo wallet indicato per
         * nome ("Wallet 02"), senza passare dalla stringa della combo. Con {@code PlusXWallet} spenta la
         * pila è una sola, esattamente come nella tabella "Giacenze a data".
         */
        public String CostoDelGruppo(String Gruppo, String Chiave, String Qta) {
            return CostoDaiGruppi(PlusXWallet ? List.of(Gruppo) : Pile.keySet(), Chiave, Qta);
        }

        /**
         * Come {@link #CostoDelleRimanenze(String, String, String)} ma senza arrotondare: serve a dividere
         * il costo per la quantità (costo unitario) senza che un costo di pochi centesimi, arrotondato a
         * due decimali, dia un unitario nullo su un token che vale frazioni di centesimo.
         * @return il costo esatto; zero se la giacenza è nulla o negativa o non si trova nessun lotto
         */
        public BigDecimal CostoEsattoDelleRimanenze(String Wallet, String Chiave, String Qta) {
            return CostoEsattoDaiGruppi(GruppiInSelezione(Wallet), Chiave, Qta);
        }

        private String CostoDaiGruppi(Collection<String> Gruppi, String Chiave, String Qta) {
            return CostoEsattoDaiGruppi(Gruppi, Chiave, Qta).setScale(2, RoundingMode.HALF_UP).toPlainString();
        }

        private BigDecimal CostoEsattoDaiGruppi(Collection<String> Gruppi, String Chiave, String Qta) {
            BigDecimal Richiesta;
            try {
                Richiesta = new BigDecimal(Qta.trim());
            } catch (NumberFormatException | NullPointerException ex) {
                return BigDecimal.ZERO;
            }
            if (Richiesta.signum() <= 0) {
                return BigDecimal.ZERO;
            }
            List<String[]> Lotti = new ArrayList<>();
            for (String Gruppo : Gruppi) {
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
            return Costo;
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

    /** Cifre significative con cui si mostra un valore unitario: un token da 1E-9 euro non deve diventare "0". */
    private static final java.math.MathContext CIFRE_UNITARIO = new java.math.MathContext(8, RoundingMode.HALF_UP);

    /**
     * Testo di un valore unitario (prezzo o costo di una singola unità) per le tabelle di "Giacenze a
     * data": otto cifre significative, mai in notazione scientifica, che {@code Double.toString} userebbe
     * sui token con molti decimali.
     * @return il testo, vuoto se il valore è {@code null}
     */
    public static String FormattaUnitario(Double Valore) {
        if (Valore == null) {
            return "";
        }
        BigDecimal R = BigDecimal.valueOf(Valore).round(CIFRE_UNITARIO).stripTrailingZeros();
        return R.signum() == 0 ? "0" : R.toPlainString();
    }

    /**
     * Le tre colonne derivate della riga della tabella principale di "Giacenze a data": valore unitario,
     * costo unitario e differenza fra valore e costo di carico.
     * <p>
     * Restano vuote (<b>{@code null}</b>, non zero) quando non c'è nulla da confrontare: moneta FIAT (non ha
     * pile LIFO), giacenza nulla o negativa (non ci sono rimanenze da valorizzare) e, per la differenza,
     * token senza prezzo — altrimenti mostrerebbe come perdita l'intero costo di carico di un token che
     * semplicemente non si riesce a valorizzare.
     * <p>
     * Il costo unitario divide il costo <b>esatto</b> delle rimanenze, non quello arrotondato a due
     * decimali mostrato in tabella; la differenza invece usa i due numeri mostrati, così la colonna
     * torna con quelle accanto.
     *
     * @param Tipo tipo della moneta (Crypto, FIAT, ...)
     * @param Qta giacenza mostrata sulla riga
     * @param PrezzoUnitario prezzo unitario alla data, {@code null} se il token non ha prezzo
     * @param ValoreRiga valore della riga, come in tabella
     * @param CostoRiga costo di carico della riga, come in tabella
     * @param CostoEsatto costo di carico delle rimanenze non arrotondato
     * @return {@code {valore unitario, costo unitario, differenza}}, ogni voce può essere {@code null}
     */
    public static Double[] ValoriDerivatiRiga(String Tipo, String Qta, BigDecimal PrezzoUnitario,
            double ValoreRiga, double CostoRiga, BigDecimal CostoEsatto) {
        Double Ris[] = new Double[3];
        if (Tipo == null || Tipo.equalsIgnoreCase("FIAT")) {
            return Ris;
        }
        BigDecimal Q;
        try {
            Q = new BigDecimal(Qta.trim());
        } catch (NumberFormatException | NullPointerException ex) {
            return Ris;
        }
        if (Q.signum() <= 0) {
            return Ris;
        }
        if (PrezzoUnitario != null) {
            Ris[0] = PrezzoUnitario.doubleValue();
            Ris[2] = BigDecimal.valueOf(ValoreRiga).subtract(BigDecimal.valueOf(CostoRiga)).doubleValue();
        }
        if (CostoEsatto != null) {
            Ris[1] = CostoEsatto.divide(Q, VarStatiche.DecimaliCalcoli + 10, RoundingMode.HALF_UP).doubleValue();
        }
        return Ris;
    }

    /**
     * La passata LIFO dietro la tabella dettaglio movimenti di "Giacenze a data": per ogni riga mostrata,
     * il costo di carico della quantità che resta dopo quel movimento.
     * <p>
     * Stessa regola della tabella principale — <b>la passata non è filtrata per wallet, la lettura sì</b> —
     * con le stesse pile ({@link #ElaboraLotti}): il chiamante fa scorrere <b>tutti</b> i movimenti
     * anteriori alla data con {@link #Avanza(String[])}, nell'ordine della mappa (cioè per ID), e dopo
     * ciascuno legge il costo delle rimanenze della selezione wallet con {@link #CostoResiduo(String)}.
     * Filtrare i movimenti a monte toglierebbe dal flusso i giroconti interni, che non portano costo
     * proprio e lasciano i lotti dov'erano. L'ultima riga mostrata coincide con la colonna "Costo Carico"
     * della tabella principale per la stessa moneta.
     * <p>
     * Il gruppo wallet letto si risolve una volta sola alla costruzione: per un singolo wallet è una
     * lettura del database che altrimenti si ripeterebbe a ogni riga.
     */
    public static final class CostiDettaglioToken {

        private final CostiCaricoRimanenze Costi = new CostiCaricoRimanenze();
        private final java.util.Set<String> Simboli;
        private final String Chiave;
        private final Collection<String> Gruppi;

        /**
         * @param Wallet selezione della combo wallet ("Tutti", un wallet, "Gruppo : X ( alias )")
         * @param Moneta simbolo della moneta esaminata
         * @param Tipo tipo della moneta, come sulla riga della tabella principale
         * @param Address address del token, o vuoto
         * @param Rete rete del token, o vuota
         */
        public CostiDettaglioToken(String Wallet, String Moneta, String Tipo, String Address, String Rete) {
            Simboli = java.util.Set.of(Moneta);
            Chiave = ChiaveRiga(Moneta, Tipo, Address, Rete);
            //Con "Tutti" è la vista viva delle chiavi della pila: si aggiorna da sola man mano che i lotti arrivano
            Gruppi = Costi.GruppiInSelezione(Wallet);
        }

        /** Carica e scarica i lotti del movimento, ignorando le monete diverse da quella esaminata. */
        public void Avanza(String[] Movimento) {
            ElaboraLotti(Costi, Movimento, Simboli);
        }

        /**
         * @param QtaResidua giacenza della selezione dopo l'ultimo movimento passato ad {@link #Avanza(String[])}
         * @return il costo di carico di quella quantità, con due decimali; {@code "0.00"} se è nulla o negativa
         */
        public String CostoResiduo(String QtaResidua) {
            return Costi.CostoDaiGruppi(Gruppi, Chiave, QtaResidua);
        }
    }

    /**
     * Costo di carico della quantità mossa da un movimento, come l'ha scritto il motore delle plusvalenze:
     * {@code v[17]} per la gamba in entrata, {@code v[16]} per quella in uscita.
     * @param Movimento il movimento
     * @param Entrata {@code true} per la gamba in entrata ({@code v[11]}-{@code v[13]}), {@code false} per quella in uscita
     * @return il costo con due decimali; <b>vuoto</b> (non zero) se il motore non ne ha scritto uno — giroconti
     * interni, PTW — o se la gamba è FIAT
     */
    public static String CostoCaricoMovimento(String[] Movimento, boolean Entrata) {
        String Tipo = Movimento[Entrata ? 12 : 9];
        String Costo = Movimento[Entrata ? 17 : 16];
        if (Tipo.isBlank() || Tipo.equalsIgnoreCase("FIAT") || Costo.isBlank()) {
            return "";
        }
        try {
            return new BigDecimal(Costo.trim()).abs().setScale(2, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException ex) {
            return "";
        }
    }

    /**
     * Le quattro colonne dei costi di una riga della tabella dettaglio movimenti di "Giacenze a data".
     * Non cerca nessun prezzo: la tabella si ricostruisce a ogni selezione e non deve toccare la rete.
     * <ol start="0">
     * <li>costo di carico della quantità mossa dal movimento ({@link #CostoCaricoMovimento});</li>
     * <li>prezzo unitario nel movimento ({@link #PrezzoUnitarioNelMovimento});</li>
     * <li>valore della quantità residua a quel prezzo unitario, cioè quanto varrebbe la giacenza residua se
     * il prezzo fosse rimasto quello del movimento;</li>
     * <li>costo di carico della quantità residua, dalle pile di {@link CostiDettaglioToken}.</li>
     * </ol>
     * Tutte vuote per una moneta FIAT, che non ha lotti.
     *
     * @param Costi la passata LIFO, già avanzata fino a questo movimento compreso
     * @param Movimento il movimento della riga
     * @param Entrata {@code true} se la riga è la gamba in entrata del movimento
     * @param Qta quantità della gamba
     * @param QtaResidua giacenza della selezione dopo il movimento
     * @return le quattro colonne, mai {@code null}
     */
    public static String[] ColonneCostiDettaglio(CostiDettaglioToken Costi, String[] Movimento,
            boolean Entrata, String Qta, String QtaResidua) {
        String Ris[] = {"", "", "", ""};
        String Tipo = Movimento[Entrata ? 12 : 9];
        if (Tipo.isBlank() || Tipo.equalsIgnoreCase("FIAT")) {
            return Ris;
        }
        Ris[0] = CostoCaricoMovimento(Movimento, Entrata);
        BigDecimal Prezzo = PrezzoUnitarioNelMovimento(Movimento, Movimento[Entrata ? 11 : 8], Qta);
        if (Prezzo != null) {
            Ris[1] = FormattaUnitario(Prezzo.doubleValue());
            try {
                Ris[2] = Prezzo.multiply(new BigDecimal(QtaResidua.trim())).setScale(2, RoundingMode.HALF_UP).toPlainString();
            } catch (NumberFormatException ex) {
                //la quantità residua è già stata sommata come numero: non succede, ma la cella resta vuota
            }
        }
        Ris[3] = Costi.CostoResiduo(QtaResidua);
        return Ris;
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

    /** Soglia minima (in euro) del valore di un movimento perché il suo prezzo unitario sia considerato affidabile. */
    private static final BigDecimal VALORE_MIN_MOVIMENTO_VICINO = new BigDecimal("10");
    /** Finestra di ricerca (ms) dei movimenti vicini alla data del movimento da creare: 5 minuti prima e dopo. */
    private static final long FINESTRA_MOVIMENTI_VICINI_MS = 5L * 60 * 1000;

    /**
     * Testo per i dialog di rettifica giacenza: quantità e simbolo del token, seguiti se possibile da
     * {@code " corrispondenti a circa € xx.xx"}. Se il prezzo non si trova, restituisce solo quantità e simbolo.
     * <p>
     * Il prezzo si cerca dal più economico al più costoso, per non rallentare la maschera:
     * <ol>
     *   <li>cache dei prezzi ({@link Prezzi#DammiPrezzoDaDatabasePersonale} e {@link Prezzi#DammiPrezzoDaDatabase},
     *       solo lettura);</li>
     *   <li>un movimento del token entro ±5 minuti con valore di almeno 10 euro, di cui si ricava il prezzo
     *       unitario (il più vicino nel tempo);</li>
     *   <li>ricerca online ({@link Prezzi#CambioXXXEUR}).</li>
     * </ol>
     * @param Moneta simbolo del token
     * @param Address indirizzo di contratto del token (può essere vuoto)
     * @param Rete rete del movimento di partenza
     * @param Qta quantità positiva del movimento da creare
     * @param IDTrans ID del movimento selezionato: il suo prefisso data è la data del movimento da creare
     * @param owner finestra su cui mostrare il cursore d'attesa durante l'eventuale ricerca online
     */
    static String DescrizioneQtaEValore(String Moneta, String Address, String Rete, String Qta, String IDTrans, Window owner) {
        String base = Qta + " " + Moneta;
        try {
            long ts = FunzioniDate.ConvertiDataIDinLong(IDTrans.split("_")[0]);
            if (ts <= 0) return base;
            BigDecimal qta = new BigDecimal(Qta);
            boolean addressValido = Address != null && !Address.isBlank() && Rete != null && !Rete.isBlank()
                    && Funzioni_WalletDeFi.isValidAddress(Address, Rete);
            BigDecimal prezzoUnitario = PrezzoUnitarioDaCache(Moneta, addressValido ? Address : "", addressValido ? Rete : "", ts, qta);
            if (prezzoUnitario == null) {
                prezzoUnitario = PrezzoUnitarioDaMovimentiVicini(Moneta, addressValido ? Address : "", ts);
            }
            if (prezzoUnitario == null) {
                if (owner != null) owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    Prezzi.InfoPrezzo IP = Prezzi.CambioXXXEUR(Moneta, Qta, ts, addressValido ? Address : "", addressValido ? Rete : "", "", false);
                    prezzoUnitario = PrezzoUnitarioDaInfoPrezzo(IP, qta);
                } finally {
                    if (owner != null) owner.setCursor(Cursor.getDefaultCursor());
                }
            }
            if (prezzoUnitario == null) return base;
            BigDecimal valore = prezzoUnitario.multiply(qta).abs();
            if (valore.signum() > 0 && valore.compareTo(new BigDecimal("0.005")) < 0) {
                return base + " corrispondenti a meno di € 0.01";
            }
            return base + " corrispondenti a circa € " + valore.setScale(2, RoundingMode.HALF_UP).toPlainString();
        } catch (Exception ex) {
            //Il prezzo è solo un'informazione in più nel messaggio: un errore non deve impedire la rettifica
            LoggerGC.ScriviErrore("Stima valore per il messaggio di rettifica giacenza non riuscita: " + ex.getMessage());
            return base;
        }
    }

    private static BigDecimal PrezzoUnitarioDaInfoPrezzo(Prezzi.InfoPrezzo IP, BigDecimal qta) {
        if (IP == null) return null;
        if (IP.prezzoUnitario != null) return IP.prezzoUnitario;
        if (IP.prezzoQta != null && qta.signum() != 0) {
            return IP.prezzoQta.divide(qta.abs(), 20, RoundingMode.HALF_UP);
        }
        return null;
    }

    /** Solo lettura delle cache: prezzi personalizzati (±60 min) e prezzi esatti (±5 min). Nessuna rete. */
    private static BigDecimal PrezzoUnitarioDaCache(String Moneta, String Address, String Rete, long ts, BigDecimal qta) {
        String simbolo = Address.isBlank()
                ? AliasPrezziToken.StessoPrezzo(Moneta, ts)
                : "";//con un address valido il simbolo non va passato, come fa CambioAddressEUR
        Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoDaDatabasePersonale(simbolo, ts, "", Rete, Address, 60, qta);
        if (IP == null) IP = Prezzi.DammiPrezzoDaDatabase(simbolo, ts, "", Rete, Address, 5, qta);
        return PrezzoUnitarioDaInfoPrezzo(IP, qta);
    }

    /**
     * Cerca fra i movimenti entro ±5 minuti da {@code ts} quello più vicino nel tempo che coinvolge il token
     * e vale almeno 10 euro, e ne ricava il prezzo unitario (valore del movimento / quantità del token).
     * Il token si riconosce per address se presente, altrimenti per simbolo.
     */
    private static BigDecimal PrezzoUnitarioDaMovimentiVicini(String Moneta, String Address, long ts) {
        //Le chiavi di MappaCryptoWallet cominciano con yyyyMMddHHmmss, quindi basta un subMap sulla finestra
        String da = FunzioniDate.FormattaDataID(ts - FINESTRA_MOVIMENTI_VICINI_MS);
        String a = FunzioniDate.FormattaDataID(ts + FINESTRA_MOVIMENTI_VICINI_MS) + "~";//"~" viene dopo le cifre: include ogni ID che comincia con quella data
        BigDecimal migliore = null;
        long distanzaMigliore = Long.MAX_VALUE;
        for (String[] v : MappaCryptoWallet.subMap(da, true, a, true).values()) {
            if (v.length <= 29 || !Funzioni.isNumeric(v[15], false)) continue;
            BigDecimal valore = new BigDecimal(v[15]).abs();
            if (valore.compareTo(VALORE_MIN_MOVIMENTO_VICINO) < 0) continue;
            //Un lato del movimento con il token: uscita ([8],[10],[26]) oppure ingresso ([11],[13],[28])
            String qtaToken = null;
            if (StessoToken(Moneta, Address, v[8], v[26])) qtaToken = v[10];
            else if (StessoToken(Moneta, Address, v[11], v[28])) qtaToken = v[13];
            if (qtaToken == null || !Funzioni.isNumeric(qtaToken, false)) continue;
            BigDecimal q = new BigDecimal(qtaToken).abs();
            if (q.signum() == 0) continue;
            long distanza = Math.abs(FunzioniDate.ConvertiDataIDinLong(v[0].split("_")[0]) - ts);
            if (distanza < distanzaMigliore) {
                distanzaMigliore = distanza;
                migliore = valore.divide(q, 20, RoundingMode.HALF_UP);
            }
        }
        return migliore;
    }

    private static boolean StessoToken(String Moneta, String Address, String monetaMov, String addressMov) {
        if (monetaMov == null || monetaMov.isBlank()) return false;
        if (!Address.isBlank()) return Address.equalsIgnoreCase(addressMov);
        return Moneta.equalsIgnoreCase(monetaMov);
    }
}
