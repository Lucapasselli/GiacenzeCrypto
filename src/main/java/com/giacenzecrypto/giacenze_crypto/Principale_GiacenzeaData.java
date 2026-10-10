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
     * L'operazione è consentita per i token di tipo {@code "Crypto"} e per le valute {@code "FIAT"} (per queste
     * senza chiedere la classificazione, vedi {@link #CreaRettificaFiat}) e non è applicabile se il filtro
     * wallet è impostato su {@code "tutti"}.
     * @param TabMovimenti tabella da cui leggere la riga selezionata (moneta, giacenza attuale, ecc.)
     * @param Wallet nome del wallet corrente, oppure {@code "tutti"} per disabilitare l'operazione
     * @param owner finestra parent dei dialog
     * @return {@code true} se è stata effettuata una modifica, {@code false} se l'operazione è stata annullata o non applicabile
     */
    /**
     * Rettifica di una moneta FIAT: a differenza delle crypto non si chiede come classificare il movimento,
     * perché per la valuta non c'è plusvalenza da decidere. Si crea un deposito FIAT (categoria DF) o un
     * prelievo FIAT (PF) della differenza, subito dopo o subito prima del movimento selezionato come per le
     * crypto, con la nota scelta dall'utente.
     * @param mov movimento selezionato, da cui si prendono wallet, rete e posizione nel tempo
     * @param Moneta simbolo della valuta
     * @param AddressMoneta address della moneta nel movimento (di norma vuoto per le FIAT)
     * @param TipoMoneta tipo della moneta, {@code FIAT}
     * @param Qta quantità del movimento da creare, positiva per un deposito e negativa per un prelievo
     * @param owner finestra parent dei dialog
     * @return {@code true} se il movimento è stato inserito, {@code false} se l'utente ha annullato
     */
    private static boolean CreaRettificaFiat(String[] mov, String Moneta, String AddressMoneta, String TipoMoneta,
            BigDecimal Qta, Window owner) {
        boolean prelievo = Qta.signum() < 0;
        String QtaAssoluta = Qta.abs().toPlainString();
        String Descrizione = Moneta.equalsIgnoreCase("EUR")
                ? QtaAssoluta + " " + Moneta
                : DescrizioneQtaEValore(Moneta, AddressMoneta, Funzioni.TrovaReteDaIMovimento(mov), QtaAssoluta, mov[0], owner);

        String Nota = AppDialog.showTextInputDialog(
                owner,
                "Nota movimento",
                prelievo ? "Nuovo movimento di prelievo FIAT" : "Nuovo movimento di deposito FIAT",
                "Per raggiungere la giacenza desiderata verrà generato un %s FIAT di %s.\n\nInserisci un'eventuale nota sul movimento."
                        .formatted(prelievo ? "prelievo" : "deposito", Descrizione),
                "Nota",
                "Rettifica di Giacenza"
        );
        if (Nota == null) {
            return false;
        }
        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        if (!Nota.contains("Rettifica")) {
            Nota = "Rettifica<br>" + Nota;
        }

        String[] IDOriSplittato = mov[0].split("_");
        IDOriSplittato[4] = prelievo ? "PF" : "DF";
        //Il prelievo va subito dopo il movimento selezionato, il deposito subito prima, come per le crypto
        String NuovoID = MovimentiCrypto.IncDecID(String.join("_", IDOriSplittato), 1, prelievo);

        Moneta M1 = new Moneta();
        M1.Moneta = Moneta;
        M1.MonetaAddress = AddressMoneta;
        M1.Qta = Qta.toPlainString();
        M1.Tipo = TipoMoneta;
        M1.Rete = Funzioni.TrovaReteDaIMovimento(mov);

        //Senza tipo esplicito creaMovimento ricava DEPOSITO FIAT / PRELIEVO FIAT (DF / PF) dal tipo della moneta
        String[] RT = MovimentiCrypto.creaMovimento(
                prelievo ? M1 : null,
                prelievo ? null : M1,
                mov[3],
                mov[4],
                0,
                null,
                null,
                1,
                1,
                NuovoID,
                Nota,
                "M",
                null,
                null,
                null
        );
        if (RT == null) {
            owner.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            return false;
        }
        MappaCryptoWallet.put(RT[0], RT);
        return true;
    }

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

            //Dettaglio di "Giacenze a data" con la colonna della blockchain: se il saldo letto e' diverso si propone la
            //giacenza che allinea l'archivio, a fine del secondo del movimento, al saldo a fine blocco
            String TestoBlockchain = "";
            if (TabMovimenti.getModel().getColumnCount() > 18
                    && TabMovimenti.getClientProperty(Tabelle.PROP_CONFRONTO_BLOCKCHAIN) instanceof Map<?, ?> Confronto) {
                Object Archivio = Confronto.get(IDTrans);
                Object Blockchain = TabMovimenti.getModel().getValueAt(rigaselezionata, 18);
                BigDecimal Proposta = GiacenzeBlockchain.GiacenzaPerAllineare(GiacenzaAttualeS, Archivio, Blockchain);
                if (Proposta != null) {
                    GiacenzaVoluta = Proposta;
                    TestoBlockchain = """

                        Sulla blockchain, alla fine del blocco di questo movimento, la giacenza è <b>%s</b>, \
                        nell'archivio, dopo tutti i movimenti dello stesso blocco, è %s. \
                        Il campo propone la giacenza che le fa coincidere%s.
                        """.formatted(Blockchain.toString().trim(), Archivio.toString().trim(),
                            new BigDecimal(Archivio.toString().trim()).compareTo(GiacenzaAttuale) == 0 ? ""
                            : " (il blocco ha altri movimenti dopo questo, che restano come sono)");
                }
            }
            
            if (Wallet==null || !Wallet.equalsIgnoreCase("tutti")){
            boolean isFiat = TipoMoneta.equalsIgnoreCase("FIAT");
            if (TipoMoneta.equalsIgnoreCase("Crypto") || isFiat){
            
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
                """.formatted(Moneta) + TestoBlockchain)
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
                    if (QtaNuovoMovimento.signum() == 0) {
                        AppDialog.builder(owner)
                                .windowTitle("Rettifica di Giacenza")
                                .bodyTitle("Nessun movimento da creare")
                                .showTitleInBody(true)
                                .theme()
                                .type(AppDialog.DialogType.INFO)
                                .message("La giacenza alla data selezionata è già %s.".formatted(GiacenzaAttuale.toPlainString()))
                                .primaryAction("ok", "OK")
                                .showDialog();
                        return false;
                    }

                    // ========== FIAT: NIENTE CLASSIFICAZIONE, SOLO DEPOSITO O PRELIEVO FIAT ==========
                    if (isFiat) {
                        if (!CreaRettificaFiat(mov, Moneta, AddressMoneta, TipoMoneta, QtaNuovoMovimento, owner)) {
                            return false;
                        }
                        scelta = 1;
                    }
                    //   BigDecimal ValoreUnitarioToken=ValoreMovOrigine.divide(QtaMovOrigine,DecimaliCalcoli+10, RoundingMode.HALF_UP).abs();

                    // ========== SE DEVO INSERIRE UN MOVIMENTO NEGATIVO CHIEDO COME CLASSIFICARLO ==========
                    else if (QtaNuovoMovimento.signum() < 0) {
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
                            .message("Questo tipo di operazione è consentita solo per Crypto e FIAT.")
                            .details("Per gli NFT utilizzare l'inserimento manuale.")
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
        /** La moneta è la gamba in uscita del movimento. */
        public boolean Uscita;
        /** La moneta è la gamba in entrata del movimento (entrambe se la stessa moneta è su tutti e due i lati). */
        public boolean Entrata;

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
            //Il primo giro dell'array è l'uscita (colonne 8-10), il secondo l'entrata (11-13)
            if (g[0] == 8) {
                Monete.get(Chiave).Uscita = true;
            } else {
                Monete.get(Chiave).Entrata = true;
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
     * Valore del movimento ({@code v[15]}, salvato con due decimali) da cui in su dividerlo per la quantità dà
     * un prezzo unitario affidabile: sotto i 10 euro l'arrotondamento al centesimo pesa più dello 0,05%, e per
     * importi minuscoli (ricompense da frazioni di centesimo) il prezzo risulta sbagliato o addirittura zero.
     */
    private static final BigDecimal VALORE_MIN_PREZZO_DA_VALORE = new BigDecimal("10");
    /**
     * Primo e ultimo istante coperti dalla cache dei prezzi al minuto ({@code PrezziNew} di {@code prezzi.mv.db}), o
     * {@code null} se non è stato calcolato. Serve a non interrogare la cache per un movimento che sta fuori dal suo
     * intervallo: ogni interrogazione, anche a vuoto, costa circa 0,1 ms e per una tabella di migliaia di righe
     * antecedenti la cache (su ETH: 2022-2025) sono centinaia di millisecondi buttati.
     */
    private static volatile long[] CoperturaPrezzi = null;

    /**
     * Rilegge l'intervallo coperto dalla cache dei prezzi. Va chiamata all'inizio di ogni costruzione della tabella
     * dettaglio, non una volta sola: un import o un ricalcolo possono aver aggiunto prezzi fuori dall'intervallo
     * precedente. Costa una {@code MIN/MAX} sulla prima colonna della chiave, cioè nulla.
     */
    public static void AggiornaCoperturaPrezzi() {
        long[] Nuova = null;
        try (java.sql.Statement St = DatabaseH2.connectionPrezzi.createStatement();
                java.sql.ResultSet Rs = St.executeQuery("SELECT MIN(timestamp), MAX(timestamp) FROM PrezziNew")) {
            if (Rs.next()) {
                long Min = Rs.getLong(1);
                boolean Vuota = Rs.wasNull();
                long Max = Rs.getLong(2);
                Nuova = Vuota ? new long[]{Long.MAX_VALUE, Long.MIN_VALUE} : new long[]{Min, Max};
            }
        } catch (Exception ex) {
            //database non disponibile: nessuna copertura nota, si interroga sempre
        }
        CoperturaPrezzi = Nuova;
    }

    /** {@code false} solo se la copertura è nota e l'istante, con la finestra di ±{@code Minuti}, sta fuori. */
    private static boolean CacheCopreIstante(long ts, long Minuti) {
        long[] Copertura = CoperturaPrezzi;
        if (Copertura == null) {
            return true;
        }
        long Margine = Minuti * 60_000L;
        return ts + Margine >= Copertura[0] && ts - Margine <= Copertura[1];
    }

    /** Finestre (minuti, ±) con cui si cerca la quotazione più vicina quando non c'è nulla nell'istante del movimento. */
    private static final long[] FINESTRE_QUOTAZIONE_VICINA_MIN = {60, 360, 1440};
    /** Valore minimo (euro) sotto il quale il rapporto valore/quantità non si usa nemmeno come ripiego: errore fino al 0,5%. */
    private static final BigDecimal VALORE_MIN_PREZZO_RIPIEGO = BigDecimal.ONE;

    /**
     * Prezzo unitario di una moneta nel movimento, in ordine di affidabilità:
     * <ol>
     * <li>quello della fonte in {@code v[40]}, non arrotondato, se la moneta è la moneta di riferimento del prezzo;</li>
     * <li>valore del movimento diviso quantità, ma solo se il valore è di almeno 10 euro;</li>
     * <li>il <b>prezzo di mercato della moneta nell'istante del movimento</b>, letto dalle cache dei prezzi e
     * dall'archivio orario storico (solo lettura, nessuna rete) o ricavato da un movimento vicino di valore adeguato;</li>
     * <li>valore diviso quantità, come ultimo ripiego, se il valore è di almeno 1 euro.</li>
     * </ol>
     * Il motivo del terzo passo: il valore del movimento ({@code v[15]}) ha due decimali, quindi per importi
     * piccoli il prezzo ricavato dividendo è sbagliato — una ricompensa di {@code 0.00000013} ETH vale
     * {@code 0.00} e darebbe un prezzo di zero euro, che azzererebbe il valore di tutta la giacenza residua
     * (0,015 ETH a 0 euro accanto a un costo di carico di 29,59). Un prezzo <b>nullo non è un prezzo</b>.
     * @return il prezzo, o {@code null} se non si trova un prezzo affidabile
     */
    static BigDecimal PrezzoUnitarioNelMovimento(String[] Mov, String Moneta, String Qta) {
        return PrezzoUnitarioNelMovimento(Mov, Moneta, Qta, true);
    }

    /**
     * Come {@link #PrezzoUnitarioNelMovimento(String[], String, String)}, ma con {@code CercaMercato} {@code false}
     * si ferma alle fonti che non leggono nulla (il prezzo in {@code v[40]} e il rapporto valore/quantità per valori di
     * almeno 10 euro). Il prezzo di mercato costa letture di database per riga — e la quotazione più vicina a
     * finestre larghe scandisce tutti i prezzi di tutte le monete (la chiave della cache comincia col timestamp, non
     * col simbolo): su una tabella di migliaia di righe vale secondi. La tabella dettaglio movimenti costruisce
     * quindi le righe con questa variante e completa i prezzi mancanti in background ({@link #CompletaPrezzoDettaglio}).
     */
    static BigDecimal PrezzoUnitarioNelMovimento(String[] Mov, String Moneta, String Qta, boolean CercaMercato) {
        if (!Mov[40].isBlank()) {
            String VSplit[] = Mov[40].split("\\|", -1);
            if (VSplit.length > 2 && Prezzi.InfoPrezzo.SeparaNome(VSplit[0])[0].equalsIgnoreCase(Moneta)) {
                try {
                    BigDecimal PrezzoFonte = new BigDecimal(VSplit[2].trim()).abs();
                    if (PrezzoFonte.signum() > 0) {
                        return PrezzoFonte;
                    }
                } catch (NumberFormatException ex) {
                    //si ripiega sul valore del movimento
                }
            }
        }
        BigDecimal Q = null;
        BigDecimal Valore = null;
        try {
            Q = new BigDecimal(Qta.trim()).abs();
            Valore = new BigDecimal(Mov[15].trim()).abs();
        } catch (NumberFormatException | NullPointerException ex) {
            //valore o quantità non numerici: niente rapporto
        }
        BigDecimal Rapporto = null;
        if (Q != null && Valore != null && Q.signum() > 0 && Valore.signum() > 0) {
            Rapporto = Valore.divide(Q, VarStatiche.DecimaliCalcoli + 10, RoundingMode.HALF_UP).stripTrailingZeros();
            if (Valore.compareTo(VALORE_MIN_PREZZO_DA_VALORE) >= 0) {
                return Rapporto;
            }
        }
        if (!CercaMercato) {
            //Solo ciò che non costa letture: il resto si completa dopo (PrezzoDaCompletare)
            return null;
        }
        BigDecimal Mercato = PrezzoDiMercatoNelMomento(Mov, Moneta, Q, true);
        if (Mercato != null) {
            return Mercato;
        }
        if (Rapporto != null && Valore.compareTo(VALORE_MIN_PREZZO_RIPIEGO) >= 0) {
            return Rapporto;
        }
        return null;
    }

    /**
     * Il prezzo unitario di mercato della moneta alla data del movimento, senza passare dal valore del
     * movimento: cache dei prezzi, poi movimenti vicini. Mai la rete: la tabella dettaglio si ricostruisce a
     * ogni selezione.
     * @return il prezzo, o {@code null} per le monete FIAT, se non si trova nulla o se le cache non sono leggibili
     */
    private static BigDecimal PrezzoDiMercatoNelMomento(String[] Mov, String Moneta, BigDecimal Q, boolean QuotazioneVicina) {
        int Gamba;
        if (Mov[8].equalsIgnoreCase(Moneta)) {
            Gamba = 0;
        } else if (Mov[11].equalsIgnoreCase(Moneta)) {
            Gamba = 1;
        } else {
            return null;
        }
        String Tipo = Mov[Gamba == 0 ? 9 : 12];
        if (Tipo.isBlank() || Tipo.equalsIgnoreCase("FIAT")) {
            return null;
        }
        try {
            long ts = FunzioniDate.ConvertiDatainLong(Mov[1]);
            if (ts <= 0) {
                return null;
            }
            String Address = Mov[Gamba == 0 ? 26 : 28];
            String Rete = Funzioni.TrovaReteDaIMovimento(Mov);
            if (Rete == null) {
                Rete = "";
            }
            boolean AddressValido = !Address.isBlank() && !Rete.isBlank() && Funzioni_WalletDeFi.isValidAddress(Address, Rete);
            String AddressUsato = AddressValido ? Address : "";
            String ReteUsata = AddressValido ? Rete : "";
            BigDecimal Prezzo = PrezzoUnitarioDaCache(Moneta, AddressUsato, ReteUsata, ts, Q == null ? BigDecimal.ONE : Q, 5, true);
            if (Prezzo == null && AddressUsato.isBlank()) {
                //L'archivio orario dei prezzi storici, che copre i movimenti anteriori alle cache al minuto
                String Orario = DatabaseH2.XXXEUR_Leggi(FunzioniDate.ConvertiDatadaLongallOra(ts) + " "
                        + AliasPrezziToken.StessoPrezzo(Moneta, ts));
                if (Orario != null && Funzioni.isNumeric(Orario, false)) {
                    Prezzo = new BigDecimal(Orario);
                }
            }
            if (Prezzo == null) {
                Prezzo = PrezzoUnitarioDaMovimentiVicini(Moneta, AddressUsato, ts);
            }
            //Nessuna quotazione al minuto per quell'istante (le ricompense minuscole non hanno mai fatto
            //scaricare i prezzi): la quotazione più vicina nel tempo, a finestre sempre più larghe
            for (long Minuti : FINESTRE_QUOTAZIONE_VICINA_MIN) {
                if (Prezzo != null || !QuotazioneVicina) {
                    break;
                }
                Prezzo = PrezzoUnitarioDaCache(Moneta, AddressUsato, ReteUsata, ts, Q == null ? BigDecimal.ONE : Q, Minuti, true);
            }
            return Prezzo != null && Prezzo.signum() > 0 ? Prezzo : null;
        } catch (Exception ex) {
            //Il prezzo è solo un'informazione in più: cache non aperte (o un errore di lettura) lasciano la cella vuota
            return null;
        }
    }

    /**
     * Una moneta del movimento pronta per essere mostrata nel dettaglio movimento, senza nessuna
     * dipendenza da Swing: il dialogo ne ricava un riquadro con una tabella.
     */
    public static final class TabellaGiacenzeMoneta {
        /** Moneta, per il titolo del riquadro. */
        public final String Moneta;
        /** La moneta esce dal movimento. */
        public final boolean Uscita;
        /** La moneta entra nel movimento. */
        public final boolean Entrata;
        /** Prezzo unitario nel movimento già formattato ("€ 2500.00"), o {@code "non disponibile"}. */
        public final String Prezzo;
        /** Intestazioni delle colonne: livello, momento, quantità, controvalore e, per le crypto, i due costi. */
        public final String[] Intestazioni;
        /** Una riga per livello e momento (prima e dopo), con le stesse colonne delle intestazioni. */
        public final List<String[]> Righe = new ArrayList<>();
        /** Avvertenze (costo di carico, righe della blockchain), vuota se non serve. */
        public final String Nota;
        /**
         * Per ogni riga di {@link #Righe}: per quelle della blockchain se la quantita' coincide con la giacenza
         * dell'archivio del wallet allo stesso confine, {@code null} per le altre e per quelle non confrontabili.
         */
        public final List<Boolean> Confronto = new ArrayList<>();

        private TabellaGiacenzeMoneta(GiacenzeMoneta M, String Prezzo, String[] Intestazioni, String Nota) {
            this.Moneta = M.Moneta;
            this.Uscita = M.Uscita;
            this.Entrata = M.Entrata;
            this.Prezzo = Prezzo;
            this.Intestazioni = Intestazioni;
            this.Nota = Nota;
        }
    }

    /**
     * Le tabelle delle giacenze per il dettaglio movimento, una per moneta coinvolta (uscita, poi entrata).
     * Ogni tabella ha tre livelli (tutti i wallet, gruppo, wallet del movimento: il gruppo manca se il wallet
     * non ne ha) e per ciascuno la riga "prima" e quella "dopo". Il nome del livello è solo sulla riga "prima".
     * @param ID ID del movimento
     * @return le tabelle, vuota se il movimento non muove nessuna moneta
     */
    public static List<TabellaGiacenzeMoneta> TabelleDettaglio(String ID) {
        return TabelleDettaglio(ID, GiacenzeAttornoAlMovimento(ID));
    }

    /**
     * Come {@link #TabelleDettaglio(String)}, a partire da giacenze già calcolate.
     */
    public static List<TabellaGiacenzeMoneta> TabelleDettaglio(String ID, List<GiacenzeMoneta> Giacenze) {
        return TabelleDettaglio(ID, Giacenze, null);
    }

    /**
     * Come {@link #TabelleDettaglio(String, List)}, con in coda le due righe della giacenza sulla blockchain (fine
     * del blocco precedente e fine del blocco del movimento) per le monete che le hanno.
     * @param Blockchain i saldi letti o in lettura, {@code null} se il movimento non li ha
     */
    public static List<TabellaGiacenzeMoneta> TabelleDettaglio(String ID, List<GiacenzeMoneta> Giacenze,
            GiacenzeBlockchain.SaldiMovimento Blockchain) {
        List<TabellaGiacenzeMoneta> Risultato = new ArrayList<>();
        String[] Mov = MappaCryptoWallet.get(ID);
        if (Mov == null) {
            return Risultato;
        }
        String Gruppo = Mov[3].isBlank() ? "" : DatabaseH2.Pers_GruppoWallet_Leggi(Mov[3], true);
        String Etichette[] = {"Tutti i wallet", Gruppo.isBlank() ? "" : "Gruppo " + Gruppo, Mov[3].trim()};
        for (GiacenzeMoneta M : Giacenze) {
            String[] Intestazioni = M.isFiat()
                    ? new String[]{"", "", "Quantità", "Valore"}
                    : new String[]{"", "", "Quantità", "Valore", "Costo carico", "Costo unit."};
            String Nota = "";
            if (Mov[18].contains("PTW") && !M.isFiat()) {
                Nota = "Prelievo verso un wallet proprio: se la destinazione è in un altro gruppo, "
                        + "il costo di carico lascia questo gruppo solo all'arrivo.";
            }
            boolean ConBlockchain = Blockchain != null && Blockchain.Contiene(M.Chiave);
            if (ConBlockchain && !Blockchain.inLettura()) {
                //Breve: la nota e' una riga sola sotto un riquadro stretto (il testo intero e' nel suggerimento)
                //Il colore confronta il sotto-wallet dell'indirizzo prima e dopo tutto il secondo del movimento: il blocco
                //contiene anche gli altri movimenti della stessa transazione (commissione, scambio...)
                String NotaChain = "Blockchain a fine blocco " + (Blockchain.blocco() - 1) + " e " + Blockchain.blocco()
                        + ", confrontata col sotto-wallet " + GiacenzeBlockchain.SOTTOWALLET_INDIRIZZO
                        + (Blockchain.altriMovimenti() > 0 ? " (con gli altri " + Blockchain.altriMovimenti()
                                + " movimenti del blocco)" : "")
                        + ": " + Blockchain.Archivio(M.Chiave, false).stripTrailingZeros().toPlainString() + " / "
                        + Blockchain.Archivio(M.Chiave, true).stripTrailingZeros().toPlainString() + ".";
                Nota = Nota.isEmpty() ? NotaChain : Nota + " " + NotaChain;
            }
            TabellaGiacenzeMoneta T = new TabellaGiacenzeMoneta(M,
                    M.PrezzoUnitario == null ? "non disponibile" : "€ " + M.PrezzoUnitario
                            .round(new java.math.MathContext(10, RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString(),
                    Intestazioni, Nota);
            for (int l = 0; l < 3; l++) {
                if (Etichette[l].isBlank()) {
                    continue;
                }
                T.Righe.add(RigaGiacenza(M, Etichette[l], "prima", M.QtaPrima[l], M.CostoPrima[l]));
                T.Righe.add(RigaGiacenza(M, "", "dopo", M.QtaDopo[l], M.CostoDopo[l]));
                T.Confronto.add(null);
                T.Confronto.add(null);
            }
            if (ConBlockchain) {
                for (boolean Dopo : new boolean[]{false, true}) {
                    String Qta = Blockchain.Testo(M.Chiave, Dopo);
                    String Controvalore = "";
                    try {
                        if (M.PrezzoUnitario != null) {
                            Controvalore = "€ " + M.PrezzoUnitario.multiply(new BigDecimal(Qta))
                                    .setScale(2, RoundingMode.HALF_UP).toPlainString();
                        }
                    } catch (NumberFormatException ex) {
                        //quantita' non letta: nessun controvalore
                    }
                    String[] Riga = new String[Intestazioni.length];
                    java.util.Arrays.fill(Riga, M.isFiat() ? "" : "-");
                    Riga[0] = Dopo ? "" : "Blockchain";
                    Riga[1] = Dopo ? "dopo" : "prima";
                    Riga[2] = Qta;
                    Riga[3] = Controvalore;
                    T.Righe.add(Riga);
                    T.Confronto.add(Blockchain.inLettura() ? null : Blockchain.Coincide(M.Chiave, Dopo));
                }
            }
            Risultato.add(T);
        }
        return Risultato;
    }

    private static String[] RigaGiacenza(GiacenzeMoneta M, String Livello, String Momento,
            BigDecimal Qta, String Costo) {
        String Controvalore = M.PrezzoUnitario == null ? "n.d."
                : "€ " + M.PrezzoUnitario.multiply(Qta).setScale(2, RoundingMode.HALF_UP).toPlainString();
        if (M.isFiat()) {
            return new String[]{Livello, Momento, Qta.toPlainString(), Controvalore};
        }
        String Unitario = "-";
        if (Costo != null && Qta.signum() > 0) {
            Unitario = "€ " + new BigDecimal(Costo).divide(Qta, 10, RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString();
        }
        return new String[]{Livello, Momento, Qta.toPlainString(), Controvalore,
            Costo == null ? "-" : "€ " + Costo, Unitario};
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
         * Quantità e costo dei lotti già convertiti in {@link BigDecimal}, per identità del lotto. Il costo di una
         * riga rilegge tutti i lotti della pila, e nel dettaglio movimenti lo fa per ogni riga: rifare la
         * conversione da testo ogni volta costava più di tutto il resto della costruzione della tabella (su un
         * token con centinaia di lotti, migliaia di righe). I lotti sono immutabili — un residuo parziale è un
         * lotto nuovo — quindi la chiave per identità non può restare vecchia.
         */
        private final java.util.IdentityHashMap<String[], BigDecimal[]> LottiNumerici = new java.util.IdentityHashMap<>();

        /**
         * Quantità e costo totali di ciascuna pila ({@code {quantità, costo}}), aggiornati a ogni carico e scarico.
         * Sono la scorciatoia della lettura più comune, quella che copre tutta la pila: con "Tutti" o con un gruppo
         * intero la giacenza mostrata è l'intera pila e il costo è la somma di tutti i lotti, che altrimenti si
         * ricalcolerebbe scorrendoli uno a uno ad ogni riga. Le somme di {@link BigDecimal} sono esatte, quindi il
         * risultato coincide con quello dello scorrimento.
         */
        private final java.util.IdentityHashMap<ArrayDeque<String[]>, BigDecimal[]> TotaliPila = new java.util.IdentityHashMap<>();

        private BigDecimal[] NumeriDelLotto(String[] Lotto) {
            return LottiNumerici.computeIfAbsent(Lotto, l -> new BigDecimal[]{new BigDecimal(l[0]), new BigDecimal(l[1])});
        }

        /** Mette un lotto in cima alla pila tenendo aggiornati i totali. */
        private void MettiLotto(ArrayDeque<String[]> Pila, String[] Lotto) {
            Pila.push(Lotto);
            BigDecimal[] N = NumeriDelLotto(Lotto);
            BigDecimal[] T = TotaliPila.computeIfAbsent(Pila, p -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            T[0] = T[0].add(N[0]);
            T[1] = T[1].add(N[1]);
        }

        /** Toglie il lotto in cima alla pila tenendo aggiornati i totali. */
        private String[] PrendiLotto(ArrayDeque<String[]> Pila) {
            String[] Lotto = Pila.pop();
            BigDecimal[] N = NumeriDelLotto(Lotto);
            BigDecimal[] T = TotaliPila.get(Pila);
            if (T != null) {
                T[0] = T[0].subtract(N[0]);
                T[1] = T[1].subtract(N[1]);
            }
            return Lotto;
        }

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
            MettiLotto(Pile.computeIfAbsent(Gruppo, k -> new TreeMap<>())
                    .computeIfAbsent(Chiave, k -> new ArrayDeque<>()),
                    new String[]{QtaLotto.toPlainString(), CostoLotto.toPlainString(), ID});
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
                String Lotto[] = PrendiLotto(Pila);
                BigDecimal[] Numeri = NumeriDelLotto(Lotto);
                BigDecimal QtaLotto = Numeri[0];
                BigDecimal CostoLotto = Numeri[1];
                if (QtaLotto.compareTo(Rimanente) <= 0) {
                    Rimanente = Rimanente.subtract(QtaLotto);
                } else {
                    BigDecimal QtaResidua = QtaLotto.subtract(Rimanente);
                    BigDecimal CostoResiduo = CostoLotto
                            .divide(QtaLotto, VarStatiche.DecimaliCalcoli + 10, RoundingMode.HALF_UP)
                            .multiply(QtaResidua)
                            .setScale(VarStatiche.DecimaliCalcoli, RoundingMode.HALF_UP);
                    MettiLotto(Pila, new String[]{QtaResidua.toPlainString(), CostoResiduo.toPlainString(), Lotto[2]});
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
            BigDecimal QtaTotale = BigDecimal.ZERO;
            BigDecimal CostoTotale = BigDecimal.ZERO;
            for (String Gruppo : Gruppi) {
                Map<String, ArrayDeque<String[]>> PerChiave = Pile.get(Gruppo);
                if (PerChiave == null) {
                    continue;
                }
                ArrayDeque<String[]> Pila = PerChiave.get(Chiave);
                if (Pila != null) {
                    Lotti.addAll(Pila);
                    BigDecimal[] T = TotaliPila.get(Pila);
                    if (T != null) {
                        QtaTotale = QtaTotale.add(T[0]);
                        CostoTotale = CostoTotale.add(T[1]);
                    }
                }
            }
            //La giacenza copre l'intera pila: ogni lotto entra per intero, il costo è la somma di tutti
            if (!Lotti.isEmpty() && Richiesta.compareTo(QtaTotale) >= 0) {
                return CostoTotale;
            }
            //Dal più recente al più vecchio : l'ID comincia con yyyyMMddHHmmss, quindi l'ordine
            //alfabetico decrescente è già quello cronologico inverso richiesto dal LIFO
            Lotti.sort((a, b) -> b[2].compareToIgnoreCase(a[2]));

            BigDecimal Costo = BigDecimal.ZERO;
            for (String[] Lotto : Lotti) {
                if (Richiesta.signum() <= 0) {
                    break;
                }
                BigDecimal[] Numeri = NumeriDelLotto(Lotto);
                BigDecimal QtaLotto = Numeri[0];
                BigDecimal CostoLotto = Numeri[1];
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
     * Le cinque colonne dei costi di una riga della tabella dettaglio movimenti di "Giacenze a data".
     * Non cerca nessun prezzo: la tabella si ricostruisce a ogni selezione e non deve toccare la rete.
     * <ol start="0">
     * <li>costo di carico della quantità mossa dal movimento ({@link #CostoCaricoMovimento});</li>
     * <li>prezzo unitario nel movimento ({@link #PrezzoUnitarioNelMovimento});</li>
     * <li>valore della quantità residua a quel prezzo unitario, cioè quanto varrebbe la giacenza residua se
     * il prezzo fosse rimasto quello del movimento;</li>
     * <li>costo di carico della quantità residua, dalle pile di {@link CostiDettaglioToken};</li>
     * <li>differenza fra il valore e il costo della quantità residua: <b>vuota</b>, non zero, se manca uno dei
     * due, o se la giacenza residua è ≤ 0 (non ci sono rimanenze da valorizzare e un costo "0.00" farebbe
     * comparire l'intero valore come utile — la stessa regola della tabella principale).</li>
     * </ol>
     * Tutte vuote per una moneta FIAT, che non ha lotti.
     *
     * @param Costi la passata LIFO, già avanzata fino a questo movimento compreso
     * @param Movimento il movimento della riga
     * @param Entrata {@code true} se la riga è la gamba in entrata del movimento
     * @param Qta quantità della gamba
     * @param QtaResidua giacenza della selezione dopo il movimento
     * @return le cinque colonne, mai {@code null}
     */
    public static String[] ColonneCostiDettaglio(CostiDettaglioToken Costi, String[] Movimento,
            boolean Entrata, String Qta, String QtaResidua) {
        String Ris[] = {"", "", "", "", ""};
        String Tipo = Movimento[Entrata ? 12 : 9];
        if (Tipo.isBlank() || Tipo.equalsIgnoreCase("FIAT")) {
            return Ris;
        }
        Ris[0] = CostoCaricoMovimento(Movimento, Entrata);
        Ris[3] = Costi.CostoResiduo(QtaResidua);
        //Qui solo le fonti che non leggono nulla: gli altri prezzi si completano dopo, in background (PrezzoDaCompletare)
        CompletaColonnePrezzo(Ris, PrezzoUnitarioNelMovimento(Movimento, Movimento[Entrata ? 11 : 8], Qta, false), QtaResidua);
        return Ris;
    }

    /**
     * Prezzo unitario, valore e differenza della quantità residua una volta noto il prezzo, e il costo residuo già
     * in {@code Ris[3]}. Con prezzo {@code null} lascia vuote le tre colonne.
     */
    private static void CompletaColonnePrezzo(String[] Ris, BigDecimal Prezzo, String QtaResidua) {
        if (Prezzo == null) {
            return;
        }
        Ris[1] = FormattaUnitario(Prezzo.doubleValue());
        try {
            BigDecimal Residua = new BigDecimal(QtaResidua.trim());
            Ris[2] = Prezzo.multiply(Residua).setScale(2, RoundingMode.HALF_UP).toPlainString();
            if (Residua.signum() > 0 && !Ris[3].isEmpty()) {
                Ris[4] = new BigDecimal(Ris[2]).subtract(new BigDecimal(Ris[3])).toPlainString();
            }
        } catch (NumberFormatException ex) {
            //la quantità residua è già stata sommata come numero: non succede, ma le celle restano vuote
        }
    }

    /**
     * @param Colonne le cinque colonne di {@link #ColonneCostiDettaglio}
     * @return {@code true} se la riga è una moneta con lotti ma il prezzo non si è trovato con le fonti immediate,
     * quindi va cercato con {@link #CompletaPrezzoDettaglio}
     */
    public static boolean PrezzoDaCompletare(String[] Colonne) {
        return !Colonne[3].isEmpty() && Colonne[1].isEmpty();
    }

    /**
     * La ricerca del prezzo di una riga rimasta senza prezzo ({@link #PrezzoDaCompletare}): cache dei prezzi,
     * archivio orario, movimenti vicini, quotazione più vicina a finestre di ±1, 6 e 24 ore. Pensata per girare in
     * un thread di background.
     *
     * @param Movimento il movimento della riga
     * @param Entrata {@code true} se la riga è la gamba in entrata
     * @param Qta quantità della gamba
     * @param QtaResidua giacenza residua della riga
     * @param CostoResiduo costo di carico residuo già calcolato ({@code Ris[3]})
     * @return {prezzo unitario, valore qta residua, differenza} (le stesse colonne 1, 2 e 4), o {@code null} se non
     * si trova nessuna quotazione
     */
    public static String[] CompletaPrezzoDettaglio(String[] Movimento, boolean Entrata, String Qta, String QtaResidua,
            String CostoResiduo) {
        BigDecimal Prezzo = PrezzoUnitarioNelMovimento(Movimento, Movimento[Entrata ? 11 : 8], Qta, true);
        if (Prezzo == null) {
            return null;
        }
        String Ris[] = {"", "", "", CostoResiduo, ""};
        CompletaColonnePrezzo(Ris, Prezzo, QtaResidua);
        return new String[]{Ris[1], Ris[2], Ris[4]};
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

    /**
     * Le quantita' per moneta della scheda "Giacenze a data", prima di prezzi e costi: somma le gambe in uscita e in
     * entrata dei movimenti anteriori a {@code DataRiferimento} che passano il filtro di wallet e sotto-wallet (o
     * del gruppo, se {@code Wallet} e' {@code "Gruppo : ..."}). La chiave e' {@code Moneta;Tipo;Address;Rete}.
     * Estratta tale e quale da {@code Principale.GiacenzeaData_CompilaTabellaToken} il 10/10/2026 perche' anche
     * il confronto delle giacenze di BASE (nodo pubblico contro archivio) usasse la stessa somma della scheda.
     *
     * @param DataRiferimento istante escluso (la scheda passa la mezzanotte del giorno dopo la data scelta)
     */
    public static Map<String, Moneta> SommaQuantitaAData(java.util.Collection<String[]> Movimenti, long DataRiferimento,
            String Wallet, String SottoWallet) {
        Map<String, Moneta> QtaCrypto = new java.util.TreeMap<>();//nel primo oggetto metto l'ID, come secondo oggetto metto il bigdecimal con la qta
        for (String[] movimento : Movimenti) {
            //Come prima cosa devo verificare che la data del movimento sia inferiore o uguale alla data scritta in alto
            //altrimenti non vado avanti
            String Rete = Funzioni.TrovaReteDaIMovimento(movimento);
          //  System.out.println(movimento[0]+" - "+Rete);
            //System.out.println(Rete);
            long DataMovimento = FunzioniDate.ConvertiDatainLong(movimento[1]);
            if (DataMovimento < DataRiferimento) {
                // adesso verifico il wallet
                String gruppoWallet = "";
                if (Wallet.contains("Gruppo :")) {
                    gruppoWallet = Wallet.split(" : ")[1].split("\\(")[0].trim();
                }
                if (Wallet.equalsIgnoreCase("tutti") //Se wallet è tutti faccio l'analisi
                        || (Wallet.equalsIgnoreCase(movimento[3].trim()) && SottoWallet.equalsIgnoreCase("tutti"))//Se wallet è uguale a quello della riga analizzata e sottowallet è tutti proseguo con l'analisi
                        || (Wallet.equalsIgnoreCase(movimento[3].trim()) && SottoWallet.equalsIgnoreCase(movimento[4].trim()))//Se wallet e sottowallet corrispondono a quelli analizzati proseguo
                        || DatabaseH2.Pers_GruppoWallet_Leggi(movimento[3],true).equals(gruppoWallet)//Se il Wallet fa parte del Gruppo Selezionato proseguo l'analisi
                        ) {
                    // GiacenzeaData_Wallet_ComboBox.getSelectedItem()
                    //Faccio la somma dei movimenti in usicta
                    Moneta Monete[] = new Moneta[2];//in questo array metto la moneta in entrata e quellain uscita
                    //in paricolare la moneta in uscita nella posizione 0 e quella in entrata nella posizione 1
                    Monete[0] = new Moneta();
                    Monete[1] = new Moneta();
                    Monete[0].MonetaAddress = movimento[26];
                    Monete[1].MonetaAddress = movimento[28];
                    //ovviamente gli address se non rispettano le 2 condizioni precedenti sono null
                    Monete[0].Moneta = movimento[8];
                    Monete[0].Tipo = movimento[9];
                    Monete[0].Qta = movimento[10];
                    Monete[0].Rete = Rete;
                    Monete[1].Moneta = movimento[11];
                    Monete[1].Tipo = movimento[12];
                    Monete[1].Qta = movimento[13];
                    Monete[1].Rete = Rete;
                    //Se non c'è l'address della moneta allora la rete non la metto visto che non è importante
                    //Stessa cosa se non ho la rete non mi serve mettere l'address
                    if (Rete == null||Rete.isBlank()){
                       Monete[0].MonetaAddress="";
                       Monete[1].MonetaAddress="";
                       Monete[0].Rete="";
                       Monete[1].Rete="";
                       Rete = "";
                    }
                    if(Monete[0].MonetaAddress.isBlank()&&Monete[1].MonetaAddress.isBlank()){
                     /*  Rete = "";
                       Monete[0].Rete="";
                       Monete[1].Rete="";*/
                    }

                    //questo ciclo for serve per inserire i valori sia della moneta uscita che di quella entrata
                    for (int a = 0; a < 2; a++) {
                        //ANALIZZO MOVIMENTI
                        if (!Monete[a].Moneta.isBlank() && QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo + ";" + Monete[a].MonetaAddress + ";" + Rete) != null) {
                            //Movimento già presente da implementare
                            Moneta M1 = QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo + ";" + Monete[a].MonetaAddress + ";" + Rete);
                            M1.Qta = new BigDecimal(M1.Qta)
                                    .add(new BigDecimal(Monete[a].Qta)).stripTrailingZeros().toPlainString();

                        } else if (!Monete[a].Moneta.isBlank()) {
                            //Movimento Nuovo da inserire
                            Moneta M1 = new Moneta();
                            M1.InserisciValori(Monete[a].Moneta, Monete[a].Qta, Monete[a].MonetaAddress, Monete[a].Tipo);
                            M1.Rete = Rete;
                          //  System.out.println("KEY=" + Monete[a].Moneta + ";" + Monete[a].Tipo + ";" + Monete[a].MonetaAddress + ";" + Rete);
                            QtaCrypto.put(Monete[a].Moneta + ";" + Monete[a].Tipo + ";" + Monete[a].MonetaAddress + ";" + Rete, M1);

                        }
                    }
                }
            }
        }
        return QtaCrypto;
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
        return PrezzoUnitarioDaCache(Moneta, Address, Rete, ts, qta, 5, false);
    }

    /**
     * Come sopra, ma con la finestra dei prezzi esatti allargata a ±{@code MinutiEsatti}: la quotazione più vicina
     * vince. Con {@code UsaCopertura} la cache dei prezzi non si interroga se l'istante è fuori dal suo intervallo
     * ({@link #AggiornaCoperturaPrezzi()}); i prezzi personalizzati si leggono sempre, sono pochi.
     */
    private static BigDecimal PrezzoUnitarioDaCache(String Moneta, String Address, String Rete, long ts, BigDecimal qta,
            long MinutiEsatti, boolean UsaCopertura) {
        String simbolo = Address.isBlank()
                ? AliasPrezziToken.StessoPrezzo(Moneta, ts)
                : "";//con un address valido il simbolo non va passato, come fa CambioAddressEUR
        Prezzi.InfoPrezzo IP = Prezzi.DammiPrezzoDaDatabasePersonale(simbolo, ts, "", Rete, Address, 60, qta);
        if (IP == null && (!UsaCopertura || CacheCopreIstante(ts, MinutiEsatti))) {
            IP = Prezzi.DammiPrezzoDaDatabase(simbolo, ts, "", Rete, Address, MinutiEsatti, qta);
        }
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
