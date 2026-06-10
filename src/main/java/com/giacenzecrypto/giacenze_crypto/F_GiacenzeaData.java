/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.awt.Cursor;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JTable;

/**
 *
 * @author luca
 */
public class F_GiacenzeaData {
    
    //ritorna true se ho fatto modifiche altrimenti ritorna false
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
