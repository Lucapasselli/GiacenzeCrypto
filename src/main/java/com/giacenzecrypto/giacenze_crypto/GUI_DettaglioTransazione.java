/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.TabellaCryptodaAggiornare;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 *
 * @author luca.passelli
 */
public class GUI_DettaglioTransazione extends javax.swing.JDialog {
//private static final long serialVersionUID = 8L;

    /**
     * Creates new form Importazioni_Resoconto
     * @param IDTransazione
     */
    
     private static final Map<Integer,String> mappa_ID=new TreeMap<>(); 
     private static int Riferimento=0;
     
     private String IDdt=null;//ID del contesto attuale

     /** Cresce a ogni compilazione: un calcolo delle giacenze arrivato dopo un cambio di movimento si scarta. */
     private int GenerazioneGiacenze=0;


        /**
         * Popola la tabella di dettaglio del dialogo con tutti i campi rilevanti del movimento
         * identificato da {@code IDTransazione} (data, wallet, rete, causale, monete di
         * uscita/entrata con relativo valore unitario calcolato o preso dalla fonte prezzo salvata,
         * costo di carico, plusvalenza, dati on-chain, note, movimenti correlati, ecc.), mostrando
         * solo le righe i cui campi non sono vuoti. Abilita il pulsante "DeFi" solo se il movimento
         * ha un hash di transazione e una rete riconosciuta.
         * @param IDTransazione ID del movimento di cui mostrare il dettaglio
         */
        public void TransazioniCrypto_CompilaTextPaneDatiMovimento(String IDTransazione) {
            IDdt=IDTransazione;
            SerializzaIDMovimenti(IDTransazione);
            this.setTitle("Dettaglio Movimento");
            
            

            //Cancello Contenuto Tabella Dettagli
            DefaultTableModel ModelloTabellaCrypto = (DefaultTableModel) Tabella.getModel();
            Tabelle.Funzioni_PulisciTabella(ModelloTabellaCrypto);
            Tabelle.CopiaPulitadaTAG(Tabella);

        
        //come prima cosa mi occupo del pulsante defi, deve essere attivo se abbiamo movimenti in defi e disattivo in caso contrario 
        //per controllare verifico di avere il transaction hash e il nome della rete quindi

        String Transazione[]=Principale.MappaCryptoWallet.get(IDTransazione);

        String Titolo="<html>"+Funzioni.getOradaID(Transazione[0])+"<br>"+Transazione[5]+"</html>";
        TextPane_Titolo.setText(Titolo);
        //Accenstro il testo
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        StyledDocument doc = TextPane_Titolo.getStyledDocument();
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        String ReteDefi=Funzioni.TrovaReteDaID(IDTransazione);

        String THash=Transazione[24];

            if(!THash.isEmpty()&&ReteDefi!=null){
                Bottone_DeFi.setEnabled(true);
            }else{
                Bottone_DeFi.setEnabled(false);
            }

        //Il pulsante dello storico si accende solo se di questo movimento esiste davvero una versione
        //precedente: lo storico si popola solo quando l'utente modifica un movimento a mano.
        //MovimentiStorico.EsisteStorico, non "il movimento ha un lignaggio" né il solo DB: il lignaggio
        //viene timbrato prima della conferma di una modifica che l'utente può ancora annullare (e
        //resterebbe senza righe), e una modifica appena fatta può non essere ancora salvata su disco.
        Bottone_Storico.setEnabled(MovimentiStorico.EsisteStorico(MovimentiStorico.LignaggioDi(IDTransazione)));
        
        String Valore;
        String Val[];
        
        //Parte per la verifica dei prezzi dalle fonti e il recupero dei prezzi unitari precisi
            BigDecimal PrzTotaleNonArrotondato = null;
            if (!Transazione[40].isBlank()) {
                String VSplit[] = Transazione[40].split("\\|");
                //Il simbolo nudo: nel campo 40 puo' essere seguito dal nome, "BIT (BitDAO)"
                String MonRif = Prezzi.InfoPrezzo.SeparaNome(VSplit[0])[0];
                String PrzUnitarioMonRif = VSplit[2];

                if (!Transazione[8].isBlank() && Transazione[8].equalsIgnoreCase(MonRif)) {
                    PrzTotaleNonArrotondato = new BigDecimal(PrzUnitarioMonRif).multiply(new BigDecimal(Transazione[10]));
                }
                if (!Transazione[11].isBlank() && Transazione[11].equalsIgnoreCase(MonRif)) {
                    PrzTotaleNonArrotondato = new BigDecimal(PrzUnitarioMonRif).multiply(new BigDecimal(Transazione[13]));
                }
            }
        
        Valore=Transazione[1];
        if (!Valore.isBlank()){
            Valore="<html><b>"+Funzioni.getOradaID(Transazione[0])+"</html>";
            Val=new String[]{"Data e Ora ",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
        
        Valore=Transazione[3];
        if (!Valore.isBlank()){
            Val=new String[]{"Exchange/Wallet ",Valore+" ("+Transazione[4]+")"};
            ModelloTabellaCrypto.addRow(Val);
        }
        
        Valore=Funzioni.TrovaReteDaID(Transazione[0]);
        if (Valore!=null&&!Valore.isBlank()){
            Val=new String[]{"Rete ",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
            Valore = Transazione[5];
            if (!Valore.isBlank()) {
                if (Transazione[20].isBlank()) {
                    Val = new String[]{"Causale Movimento ", "<html><b>" + Valore + "</b> (" + Transazione[6] + ")</html>"};
                } else {
                    String Wallets[] = GUI_DettaglioTransazione.WalletDelTrasferimento(Transazione);
                    String WalletPrelievo = Wallets[0];
                    String WalletDeposito = Wallets[1];
                    Val = new String[]{"Causale Movimento ", "<html><b>" + Valore + "</b> (" + Transazione[6] + ")<br>"
                            +"Trasferimento da <b>"+ WalletPrelievo+"</b> a <b>"+WalletDeposito+"</html>"};
                }
                ModelloTabellaCrypto.addRow(Val);
            }
        
        Valore=Transazione[31];
        if (!Valore.isBlank()){
            Val=new String[]{"Data e Ora fine trasferimento",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 
       /* Valore=Transazione[20];
        if (!Valore.isBlank()){
            Val=new String[]{"Movimenti Correlati ","<html>"+Valore.replaceAll(",", "<br>")+"</html>"};
            ModelloTabellaCrypto.addRow(Val);
        }*/
        Valore=Transazione[18];
        if (!Valore.isBlank()){
            Val=new String[]{"Dett. ",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
        
        Valore=Transazione[7];
        if (!Valore.isBlank()){
            Val=new String[]{"Causale Originale ",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
        
        Valore=Transazione[8];
        if (!Valore.isBlank()){
            String Testo="<html><p style=\"color:"+Tabelle.Rosso+";\"><b>"+Transazione[10]+ " " + Transazione[8].split("\\(")[0];
            if (!Transazione[25].isBlank()&&!Transazione[8].equalsIgnoreCase(Transazione[25])){
                Testo=Testo+" </b></p>("+Transazione[25]+")";
            }
            Testo=Testo+"</html>";
            Val=new String[]{"Uscita: ",Testo};
            
            ModelloTabellaCrypto.addRow(Val);
            //Adesso Aggiungo anche il Valore Unitario in euro della moneta in ingresso e quella in uscita
            if (!Transazione[15].isBlank()){
                BigDecimal ValUnitario=new BigDecimal(0);
                if (new BigDecimal(Transazione[10]).compareTo(BigDecimal.ZERO)!=0)
                {
                    ValUnitario=new BigDecimal(Transazione[15]).divide(new BigDecimal(Transazione[10]),10, RoundingMode.HALF_UP).stripTrailingZeros().abs();
                }
                Valore="<html>€ "+ValUnitario.toPlainString()+"</html>";
                Val=new String[]{"Valore Unitario "+Transazione[8]+" (Calcolato)",Valore};
                if (PrzTotaleNonArrotondato!=null) {
                        ValUnitario = PrzTotaleNonArrotondato.divide(new BigDecimal(Transazione[10]), 20, RoundingMode.HALF_UP).stripTrailingZeros().abs();
                        Valore = "<html>€ " + ValUnitario.toPlainString() + "</html>";
                        Val = new String[]{"Valore Unitario " + Transazione[8], Valore};
                    }
                ModelloTabellaCrypto.addRow(Val);
            }
        }
        Valore=Transazione[9];
        if (!Valore.isBlank()){
            Val=new String[]{"Uscita: Tipologia",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
      /*  Valore=Transazione[25];
        if (!Valore.isBlank()){
            Val=new String[]{"Uscita: Nome Completo",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } */
        
        Valore=Transazione[26];
        if (!Valore.isBlank()){
            Val=new String[]{"Uscita: Address Token",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 
        
        Valore=Transazione[16];
        if (!Valore.isBlank()){
            Val=new String[]{"Uscita: Costo Carico","€ "+Valore};
            ModelloTabellaCrypto.addRow(Val);
            Valore=CostoUnitario(Transazione[16],Transazione[10]);
            if (Valore!=null){
                Val=new String[]{"Uscita: Costo Carico Unitario","€ "+Valore};
                ModelloTabellaCrypto.addRow(Val);
            }
        }
        //Il motore non ha scritto un costo (giroconto nello stesso gruppo, movimento interno, non
        //classificato): se ne mostra uno solo informativo, calcolato in background con le giacenze
        int PosizioneInformativo[]={-1,-1};
        String IDTS[]=Transazione[0].split("_");
        boolean MovimentoInterno=IDTS.length>4&&IDTS[4].equalsIgnoreCase("TI");
        if (Transazione[16].isBlank()&&!Transazione[8].isBlank()&&!Transazione[9].isBlank()
                &&!Transazione[9].equalsIgnoreCase("FIAT")){
            PosizioneInformativo[0]=ModelloTabellaCrypto.getRowCount();
            ModelloTabellaCrypto.addRow(new String[]{"Uscita: Costo Carico","calcolo in corso..."});
        }
        
        Valore=Transazione[11];
        if (!Valore.isBlank()){            
            String Testo="<html><p style=\"color:"+Tabelle.Verde+";\"><b>"+Transazione[13]+ " " + Transazione[11].split("\\(")[0];
            if (!Transazione[27].isBlank()&&!Transazione[27].equalsIgnoreCase(Transazione[11])){
                Testo=Testo+" </b></p>("+Transazione[27]+")";
            }
            Testo=Testo+"</html>";
            Val=new String[]{"Entrata: ",Testo};
            ModelloTabellaCrypto.addRow(Val);
            
            if (!Transazione[15].isBlank()) {
                if (new BigDecimal(Transazione[13]).compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal ValUnitario;
                Val=null;
                if (new BigDecimal(Transazione[15]).compareTo(BigDecimal.ZERO) != 0) {
                    ValUnitario = new BigDecimal(Transazione[15]).divide(new BigDecimal(Transazione[13]), 10, RoundingMode.HALF_UP).stripTrailingZeros().abs();
                    Valore = "<html>€ " + ValUnitario.toPlainString() + "</html>";
                    Val = new String[]{"Valore Unitario " + Transazione[11] + " (Calcolato)", Valore};

                }
                if (PrzTotaleNonArrotondato != null) {
                    ValUnitario = PrzTotaleNonArrotondato.divide(new BigDecimal(Transazione[13]), 20, RoundingMode.HALF_UP).stripTrailingZeros().abs();
                    Valore = "<html>€ " + ValUnitario.toPlainString() + "</html>";
                    Val = new String[]{"Valore Unitario " + Transazione[11], Valore};
                }
                if(Val!=null)ModelloTabellaCrypto.addRow(Val);
                }
            }
        }
        Valore=Transazione[12];
        if (!Valore.isBlank()){
            Val=new String[]{"Entrata: Tipologia",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 
       /* Valore=Transazione[27];
        if (!Valore.isBlank()){
            Val=new String[]{"Entrata: Nome Completo",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } */
        
        Valore=Transazione[28];
        if (!Valore.isBlank()){
            Val=new String[]{"Entrata: Address Token",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 
        
        Valore=Transazione[17];
        if (!Valore.isBlank()){
            Val=new String[]{"Entrata: Costo Carico","€ "+Valore};
            ModelloTabellaCrypto.addRow(Val);
            Valore=CostoUnitario(Transazione[17],Transazione[13]);
            if (Valore!=null){
                Val=new String[]{"Entrata: Costo Carico Unitario","€ "+Valore};
                ModelloTabellaCrypto.addRow(Val);
            }
        }
        if (Transazione[17].isBlank()&&!Transazione[11].isBlank()&&!Transazione[12].isBlank()
                &&!Transazione[12].equalsIgnoreCase("FIAT")
                &&(MovimentoInterno||Transazione[18].contains("DTW"))){
            PosizioneInformativo[1]=ModelloTabellaCrypto.getRowCount();
            ModelloTabellaCrypto.addRow(new String[]{"Entrata: Costo Carico","calcolo in corso..."});
        }
        
        Valore=Transazione[15];
        if (!Valore.isBlank()){
            Valore="<html>€ "+Valore+"</html>";
            Val=new String[]{"Valore transazione ",Valore};
            ModelloTabellaCrypto.addRow(Val);      
        }
        Valore=Transazione[14];
        if (!Valore.isBlank()){
            Valore="<html>"+Valore+"</html>";
            Val=new String[]{"Valore transazione da CSV",Valore};
            ModelloTabellaCrypto.addRow(Val);      
        }
        
        Valore=Transazione[19];
        if (!Valore.isBlank()){
            Valore="<html><b>€ "+Valore+"</html>";
            Val=new String[]{"Plusvalenza ",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }

        //Giacenze prima e dopo il movimento: una passata sulla mappa fino a questo movimento, qualche
        //decina di millisecondi su un archivio da centomila movimenti. Si calcola in background e si
        //mostra nei due riquadri sotto la tabella (uscita rossa, entrata verde), così le frecce restano
        //immediate. Il pannello torna subito in attesa: non deve restare il risultato del movimento di prima
        MostraGiacenzeMessaggio("Giacenze prima e dopo il movimento: calcolo in corso...");
        CaricaGiacenzeInBackground(IDTransazione, PosizioneInformativo);
        

        
        Valore=Transazione[29];
        if (!Valore.isBlank()){
            Val=new String[]{"BC: Timestamp",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 
        
        Valore=Transazione[23];
        if (!Valore.isBlank()){
            Val=new String[]{"BC: Numero Blocco",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }        

        Valore=Transazione[24];
        if (!Valore.isBlank()){
            Val=new String[]{"BC: Hash Transazione",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 

        Valore=Transazione[30];
        if (!Valore.isBlank()){
            Val=new String[]{"BC: Address Controparte",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 
        
        Valore=Transazione[36];
        if (!Valore.isBlank()){
            Val=new String[]{"Address di Provenienza",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 
        
        Valore=Transazione[37];
        if (!Valore.isBlank()){
            Val=new String[]{"Address di Destinazione",Valore};
            ModelloTabellaCrypto.addRow(Val);
        } 


        Valore=Transazione[21];
        if (!Valore.isBlank()){
            Valore=("<html>"+Valore+"</html>");
            Val=new String[]{"Note ",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
        
        String Valori[]=Transazione[20].split(",");
         for (String Valori1 : Valori) {
             Valore = Valori1;
             if (!Valore.isBlank()){
                 Valore=("<html>"+Valore+"</html>");
                 Val=new String[]{"Movimenti Correlati ",Valore};
                 ModelloTabellaCrypto.addRow(Val);
             }
         }
        //Commissioni collegate (campo 43): per un movimento le sue commissioni, per una commissione il
        //movimento a cui appartiene. Solo informativo, vedi CommissioniCollegate
        for (String[] Riga : Principale_CommissioniCollegate.RigheDettaglio(IDTransazione)) {
            ModelloTabellaCrypto.addRow(Riga);
        }
        Valore=Transazione[0];
        if (!Valore.isBlank()){
            Val=new String[]{"ID ",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }
        
                Valore=Transazione[40];
        if (!Valore.isBlank()){
            String VSplit[]=Valore.split("\\|",-1);        
            Val=new String[]{"Info Prezzo : Fonte ",VSplit[3]};
            ModelloTabellaCrypto.addRow(Val);
            if (Funzioni.isNumeric(VSplit[1], false)){
                Val=new String[]{"Info Prezzo : Orario Fonte ",FunzioniDate.ConvertiDatadaLongAlSecondo(Long.parseLong(VSplit[1]))};
                ModelloTabellaCrypto.addRow(Val); 
            }                    
            if (VSplit[0]!=null&&!VSplit[0].isBlank()){
                Val=new String[]{"Info Prezzo : Moneta di riferimento transazione",VSplit[0]};
                ModelloTabellaCrypto.addRow(Val);
            }
            if (VSplit[2]!=null&&!VSplit[2].isBlank()){
                Val=new String[]{"Info Prezzo : Prezzo unitario ","€ "+VSplit[2]};
                ModelloTabellaCrypto.addRow(Val);
            }
        }

        //Documento da cui il movimento è stato importato. Mostra sempre il nome originale, mai quello del
        //file conservato (che è compresso): il ".gz" è un dettaglio di come lo si archivia, non di cosa sia
        Valore=DocumentiFonte.Descrizione(Transazione[41]);
        if (!Valore.isBlank()){
            Val=new String[]{"Documento di origine",Valore};
            ModelloTabellaCrypto.addRow(Val);
        }

        Tabelle.ColoraTabellaSemplice(Tabella);
        Tabelle.updateRowHeights(Tabella);

    }
    
    /**
     * Costo di carico per unità della quantità mossa: il costo scritto dal motore delle plusvalenze
     * ({@code v[16]} per l'uscita, {@code v[17]} per l'entrata) diviso la quantità della stessa gamba.
     * @return il costo unitario senza zeri finali, {@code null} se costo o quantità non sono numeri o la quantità è zero
     */
    static String CostoUnitario(String Costo, String Qta) {
        try {
            BigDecimal Q = new BigDecimal(Qta.trim()).abs();
            if (Q.signum() == 0) {
                return null;
            }
            return new BigDecimal(Costo.trim()).abs().divide(Q, 10, RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString();
        } catch (NumberFormatException | NullPointerException ex) {
            return null;
        }
    }

    /**
     * Calcola in background le giacenze prima/dopo il movimento
     * ({@link Principale_GiacenzeaData#CalcolaDettaglio(String)}), le mostra nei riquadri sotto la tabella
     * e mette i costi di carico informativi al posto delle righe di attesa della tabella. Le righe si
     * sostituiscono dall'ultima alla prima, così le posizioni di quelle più in alto restano valide. Se nel
     * frattempo è stato mostrato un altro movimento il risultato si scarta.
     * @param PosizioneInformativo righe di attesa del costo informativo di uscita e di entrata, -1 se assenti
     */
    private void CaricaGiacenzeInBackground(String IDTransazione, int PosizioneInformativo[]) {
        int Generazione = ++GenerazioneGiacenze;
        String Transazione[] = Principale.MappaCryptoWallet.get(IDTransazione);
        new SwingWorker<Principale_GiacenzeaData.DettaglioGiacenze, Void>() {
            @Override
            protected Principale_GiacenzeaData.DettaglioGiacenze doInBackground() {
                return Principale_GiacenzeaData.CalcolaDettaglio(IDTransazione);
            }

            @Override
            protected void done() {
                if (Generazione != GenerazioneGiacenze) {
                    return;
                }
                String Informativo[] = {null, null};
                try {
                    Principale_GiacenzeaData.DettaglioGiacenze D = get();
                    MostraGiacenze(Principale_GiacenzeaData.TabelleDettaglio(IDTransazione, D.Monete));
                    Informativo = D.CostoInformativo;
                } catch (Exception ex) {
                    LoggerGC.ScriviErrore(ex);
                    MostraGiacenzeMessaggio("Giacenze prima e dopo il movimento: calcolo non riuscito");
                }
                DefaultTableModel Modello = (DefaultTableModel) Tabella.getModel();
                String Lati[] = {"Uscita", "Entrata"};
                String Qta[] = {Transazione[10], Transazione[13]};
                for (int l = 1; l >= 0; l--) {
                    if (PosizioneInformativo[l] < 0 || PosizioneInformativo[l] >= Modello.getRowCount()) {
                        continue;
                    }
                    java.util.List<String[]> RigheCosto = new java.util.ArrayList<>();
                    if (Informativo[l] != null) {
                        RigheCosto.add(new String[]{Lati[l] + ": Costo Carico",
                            "<html>€ " + Informativo[l] + " <i>(solo informativo)</i></html>"});
                        String Unitario = CostoUnitario(Informativo[l], Qta[l]);
                        if (Unitario != null) {
                            RigheCosto.add(new String[]{Lati[l] + ": Costo Carico Unitario",
                                "<html>€ " + Unitario + " <i>(solo informativo)</i></html>"});
                        }
                    }
                    SostituisciRiga(Modello, PosizioneInformativo[l], RigheCosto);
                }
                Tabelle.updateRowHeights(Tabella);
            }
        }.execute();
    }

    /** Rosso e verde dei riquadri: pieni e scuri, con testo bianco leggibile sia nel tema chiaro sia nello scuro. */
    private static final java.awt.Color COLORE_USCITA = new java.awt.Color(0xC0392B);
    private static final java.awt.Color COLORE_ENTRATA = new java.awt.Color(0x287D4C);
    private static final java.awt.Color COLORE_ENTRAMBE = new java.awt.Color(0x546E7A);

    /** Sostituisce il contenuto del pannello delle giacenze con una sola riga di testo (attesa, errore). */
    private void MostraGiacenzeMessaggio(String Testo) {
        javax.swing.JLabel Etichetta = new javax.swing.JLabel(Testo, javax.swing.SwingConstants.CENTER);
        Pannello_Giacenze.removeAll();
        Pannello_Giacenze.add(Etichetta, java.awt.BorderLayout.CENTER);
        Pannello_Giacenze.setVisible(true);
        Pannello_Giacenze.revalidate();
        Pannello_Giacenze.repaint();
    }

    /**
     * Mostra le giacenze di ogni moneta del movimento in un riquadro affiancato agli altri, uscita a
     * sinistra e entrata a destra. Senza nessuna moneta il pannello sparisce e la tabella si riprende lo spazio.
     */
    private void MostraGiacenze(java.util.List<Principale_GiacenzeaData.TabellaGiacenzeMoneta> Tabelle) {
        Pannello_Giacenze.removeAll();
        if (Tabelle.isEmpty()) {
            Pannello_Giacenze.setVisible(false);
        } else {
            javax.swing.JPanel Riquadri = new javax.swing.JPanel(new java.awt.GridLayout(1, Tabelle.size(), 8, 0));
            for (Principale_GiacenzeaData.TabellaGiacenzeMoneta T : Tabelle) {
                Riquadri.add(CreaRiquadroGiacenze(T));
            }
            Pannello_Giacenze.add(Riquadri, java.awt.BorderLayout.CENTER);
            Pannello_Giacenze.setVisible(true);
        }
        Pannello_Giacenze.revalidate();
        Pannello_Giacenze.repaint();
    }

    /**
     * Un riquadro: barra del titolo colorata (rossa per la moneta in uscita, verde per quella in entrata,
     * grigia se la stessa moneta è su tutti e due i lati), tabella con le giacenze prima e dopo e, se serve,
     * l'avvertenza sul costo di carico. Si può selezionare e copiare con Ctrl+C come la tabella principale.
     */
    private javax.swing.JComponent CreaRiquadroGiacenze(Principale_GiacenzeaData.TabellaGiacenzeMoneta T) {
        java.awt.Color Colore = T.Uscita && T.Entrata ? COLORE_ENTRAMBE : T.Uscita ? COLORE_USCITA : COLORE_ENTRATA;
        String Lato = T.Uscita && T.Entrata ? "USCITA e ENTRATA" : T.Uscita ? "USCITA" : "ENTRATA";

        javax.swing.JPanel Titolo = new javax.swing.JPanel(new java.awt.BorderLayout(12, 0));
        Titolo.setBackground(Colore);
        Titolo.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
        javax.swing.JLabel Nome = new javax.swing.JLabel(Lato + ": " + T.Moneta);
        Nome.setForeground(java.awt.Color.WHITE);
        Nome.setFont(Nome.getFont().deriveFont(java.awt.Font.BOLD));
        javax.swing.JLabel Prezzo = new javax.swing.JLabel("prezzo unitario nel movimento: " + T.Prezzo);
        Prezzo.setForeground(java.awt.Color.WHITE);
        Titolo.add(Nome, java.awt.BorderLayout.WEST);
        Titolo.add(Prezzo, java.awt.BorderLayout.EAST);

        DefaultTableModel Modello = new DefaultTableModel(T.Intestazioni, 0) {
            @Override
            public boolean isCellEditable(int riga, int colonna) {
                return false;
            }
        };
        for (String[] Riga : T.Righe) {
            Modello.addRow(Riga);
        }
        JTable Piccola = new JTable(Modello);
        Piccola.setCellSelectionEnabled(true);
        Piccola.setRowHeight(22);
        Piccola.setShowGrid(false);
        Piccola.setIntercellSpacing(new java.awt.Dimension(0, 0));
        Piccola.getTableHeader().setReorderingAllowed(false);
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(Piccola);
        Piccola.setDefaultRenderer(Object.class, new RendererGiacenze());
        javax.swing.table.TableColumnModel Colonne = Piccola.getColumnModel();
        //Il livello (nome del wallet) è la colonna che si taglia per prima: le si lascia più spazio
        Colonne.getColumn(0).setMinWidth(110);
        Colonne.getColumn(0).setPreferredWidth(150);
        Colonne.getColumn(1).setMinWidth(52);
        Colonne.getColumn(1).setPreferredWidth(52);
        Colonne.getColumn(1).setMaxWidth(60);
        for (int c = 2; c < Colonne.getColumnCount(); c++) {
            Colonne.getColumn(c).setMinWidth(70);
            Colonne.getColumn(c).setPreferredWidth(c == 2 ? 70 : c == 5 ? 92 : 85);
        }
        javax.swing.JScrollPane Scorri = new javax.swing.JScrollPane(Piccola);
        Scorri.setBorder(null);

        javax.swing.JPanel Riquadro = new javax.swing.JPanel(new java.awt.BorderLayout());
        Riquadro.setBorder(javax.swing.BorderFactory.createLineBorder(Colore, 2));
        Riquadro.add(Titolo, java.awt.BorderLayout.NORTH);
        Riquadro.add(Scorri, java.awt.BorderLayout.CENTER);
        if (!T.Nota.isBlank()) {
            javax.swing.JLabel Nota = new javax.swing.JLabel("<html><i>" + T.Nota + "</i></html>");
            Nota.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8));
            Riquadro.add(Nota, java.awt.BorderLayout.SOUTH);
        }
        return Riquadro;
    }

    /**
     * Celle delle tabelle delle giacenze: numeri allineati a destra, riga "dopo" in grassetto, e uno sfondo
     * alternato a ogni livello (tutti i wallet, gruppo, wallet) come nella tabella principale, così le coppie prima/dopo si distinguono.
     * Il nome del livello, se troncato, compare per intero nel suggerimento.
     */
    private static final class RendererGiacenze extends javax.swing.table.DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable Tab, Object Valore, boolean Selezionata,
                boolean Fuoco, int Riga, int Colonna) {
            super.getTableCellRendererComponent(Tab, Valore, Selezionata, false, Riga, Colonna);
            setHorizontalAlignment(Colonna >= 2 ? javax.swing.SwingConstants.RIGHT : javax.swing.SwingConstants.LEFT);
            boolean Dopo = "dopo".equals(Tab.getValueAt(Riga, 1));
            setFont(Tab.getFont().deriveFont(Dopo || Colonna == 0 ? java.awt.Font.BOLD : java.awt.Font.PLAIN));
            setToolTipText(Valore == null || Valore.toString().isBlank() ? null : Valore.toString());
            //Gli stessi colori della tabella principale, una fascia per ogni coppia prima/dopo
            setBackground(Tab.isCellSelected(Riga, Colonna) ? Tabelle.SfondoSelezione(Riga)
                    : Tabelle.SfondoRigaAlternata(Riga / 2));
            return this;
        }
    }

    /** Toglie la riga di attesa in {@code Posizione} e ci inserisce al suo posto le righe date (anche nessuna). */
    private static void SostituisciRiga(DefaultTableModel Modello, int Posizione, java.util.List<String[]> Righe) {
        Modello.removeRow(Posizione);
        int r = Posizione;
        for (String[] Riga : Righe) {
            Modello.insertRow(r++, Riga);
        }
    }

    public GUI_DettaglioTransazione() {
        setModalityType(ModalityType.APPLICATION_MODAL);
        initComponents();
        Tabelle.Tabelle_ApplicaHeaderBoldCentrato(Tabella);
        //Come in Principale: il popup non è figlio di nessun contenitore finché non viene mostrato
        Icone.AdattaIconeAlTema(PopupMenu);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        PopupMenu = new javax.swing.JPopupMenu();
        MenuItem_CopiaID = new javax.swing.JMenuItem();
        MenuItem_Copia = new javax.swing.JMenuItem();
        MenuItem_DocumentoFonte = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        MenuItem_ModificaPrezzo = new javax.swing.JMenuItem();
        MenuItem_ModificaNote = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        MenuItem_EsportaTabella = new javax.swing.JMenuItem();
        ScrollTabella = new javax.swing.JScrollPane();
        Tabella = new javax.swing.JTable();
        Pannello_Giacenze = new javax.swing.JPanel();
        Bottone_DeFi = new javax.swing.JButton();
        Bottone_Storico = new javax.swing.JButton();
        Bottone_MovPrecedente = new javax.swing.JButton();
        Bottone_MovSuccessivo = new javax.swing.JButton();
        TextPane_Titolo = new javax.swing.JTextPane();
        Bottone_Modifica = new javax.swing.JButton();
        Bottone_ModificaPrezzo = new javax.swing.JButton();

        MenuItem_CopiaID.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Copia.png"))); // NOI18N
        MenuItem_CopiaID.setText("Copia ID Transazione");
        MenuItem_CopiaID.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_CopiaIDActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_CopiaID);

        MenuItem_Copia.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Copia.png"))); // NOI18N
        MenuItem_Copia.setText("Copia selezione");
        MenuItem_Copia.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_CopiaActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_Copia);

        MenuItem_DocumentoFonte.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Documento.png"))); // NOI18N
        MenuItem_DocumentoFonte.setText("Apri documento di origine");
        MenuItem_DocumentoFonte.setToolTipText("Apre il file da cui il movimento è stato importato");
        MenuItem_DocumentoFonte.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_DocumentoFonteActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_DocumentoFonte);
        PopupMenu.add(jSeparator6);

        MenuItem_ModificaPrezzo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Prezzo.png"))); // NOI18N
        MenuItem_ModificaPrezzo.setText("Modifica Prezzo");
        MenuItem_ModificaPrezzo.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                MenuItem_ModificaPrezzoMouseReleased(evt);
            }
        });
        MenuItem_ModificaPrezzo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_ModificaPrezzoActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_ModificaPrezzo);

        MenuItem_ModificaNote.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Nuovo.png"))); // NOI18N
        MenuItem_ModificaNote.setText("Modifica Note");
        MenuItem_ModificaNote.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_ModificaNoteActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_ModificaNote);
        PopupMenu.add(jSeparator7);

        MenuItem_EsportaTabella.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Tabella.png"))); // NOI18N
        MenuItem_EsportaTabella.setText("Esporta Tabella in Excel");
        MenuItem_EsportaTabella.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MenuItem_EsportaTabellaActionPerformed(evt);
            }
        });
        PopupMenu.add(MenuItem_EsportaTabella);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        Tabella.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Nome", "Valore"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        Tabella.setCellSelectionEnabled(true);
        Tabella.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                TabellaMouseReleased(evt);
            }
        });
        ScrollTabella.setViewportView(Tabella);
        if (Tabella.getColumnModel().getColumnCount() > 0) {
            Tabella.getColumnModel().getColumn(0).setMinWidth(200);
            Tabella.getColumnModel().getColumn(0).setPreferredWidth(200);
            Tabella.getColumnModel().getColumn(0).setMaxWidth(200);
        }

        Pannello_Giacenze.setLayout(new java.awt.BorderLayout());

        Bottone_DeFi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Catena.png"))); // NOI18N
        Bottone_DeFi.setText("Dettaglio DeFi");
        Bottone_DeFi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_DeFiActionPerformed(evt);
            }
        });

        Bottone_Storico.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Libro.png"))); // NOI18N
        Bottone_Storico.setText("Versioni precedenti");
        Bottone_Storico.setEnabled(false);
        Bottone_Storico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_StoricoActionPerformed(evt);
            }
        });

        Bottone_MovPrecedente.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/40_FrecciaSinistra.png"))); // NOI18N
        Bottone_MovPrecedente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_MovPrecedenteActionPerformed(evt);
            }
        });

        Bottone_MovSuccessivo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/40_FrecciaDestra.png"))); // NOI18N
        Bottone_MovSuccessivo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_MovSuccessivoActionPerformed(evt);
            }
        });

        TextPane_Titolo.setEditable(false);
        TextPane_Titolo.setContentType("text/html"); // NOI18N
        TextPane_Titolo.setFont(new java.awt.Font("Noto Sans", 1, 14)); // NOI18N

        Bottone_Modifica.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Modifica.png"))); // NOI18N
        Bottone_Modifica.setText("Modifica Movimento");
        Bottone_Modifica.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_ModificaActionPerformed(evt);
            }
        });

        Bottone_ModificaPrezzo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Prezzo.png"))); // NOI18N
        Bottone_ModificaPrezzo.setText("Modifica Prezzo");
        Bottone_ModificaPrezzo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_ModificaPrezzoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ScrollTabella)
                    .addComponent(Pannello_Giacenze, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Bottone_MovPrecedente)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TextPane_Titolo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_MovSuccessivo))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Bottone_Modifica, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_ModificaPrezzo, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_DeFi, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Bottone_Storico, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 454, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(TextPane_Titolo, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(Bottone_MovPrecedente)
                        .addComponent(Bottone_MovSuccessivo)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ScrollTabella, javax.swing.GroupLayout.DEFAULT_SIZE, 477, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Pannello_Giacenze, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Bottone_DeFi)
                    .addComponent(Bottone_Storico)
                    .addComponent(Bottone_Modifica)
                    .addComponent(Bottone_ModificaPrezzo))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Svuota la mappa statica ID→ordine dei movimenti, in modo che venga ricostruita da capo dalla
     * prossima chiamata a {@link #TransazioniCrypto_CompilaTextPaneDatiMovimento(String)} (usato
     * quando si apre una nuova istanza del dialogo dopo che la mappa dei movimenti è cambiata).
     */
    public void AzzeraMap(){
        mappa_ID.clear();
    }
    
    private void SerializzaIDMovimenti(String IDTransazione){
        int i=0;
        //Metto in questa mappa tutti i movimenti ordinati per ID
        //Poi salvo anche il numero del movimento visualizzato
        if (mappa_ID.isEmpty())
            for (String[] movimento: Principale.MappaCryptoWallet.values()){
                mappa_ID.put(i, movimento[0]);
                if (movimento[0].equals(IDTransazione))Riferimento=i;
                i++;
            }
    }
    
    private void Bottone_MovSuccessivoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_MovSuccessivoActionPerformed
        // TODO add your handling code here:
        if (mappa_ID.get(Riferimento+1)!=null){
            Riferimento=Riferimento+1;
            String IDTransazione=mappa_ID.get(Riferimento);
            TransazioniCrypto_CompilaTextPaneDatiMovimento(IDTransazione);
        }
    }//GEN-LAST:event_Bottone_MovSuccessivoActionPerformed

    private void Bottone_MovPrecedenteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_MovPrecedenteActionPerformed
        // TODO add your handling code here:
        if (mappa_ID.get(Riferimento-1)!=null){
            Riferimento=Riferimento-1;
            String IDTransazione=mappa_ID.get(Riferimento);
            TransazioniCrypto_CompilaTextPaneDatiMovimento(IDTransazione);
        }
    }//GEN-LAST:event_Bottone_MovPrecedenteActionPerformed

    private void Bottone_DeFiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_DeFiActionPerformed
        // TODO add your handling code here:
        Funzioni_WalletDeFi.ApriExplorer(mappa_ID.get(Riferimento));
    }//GEN-LAST:event_Bottone_DeFiActionPerformed

    private void Bottone_StoricoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_Storico
        GUI_StoricoMovimento.Mostra(mappa_ID.get(Riferimento), this);
    }//GEN-LAST:event_Bottone_Storico

    private void Bottone_ModificaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_ModificaActionPerformed
        // TODO add your handling code here:
        Funzione_ModificaMovimento(mappa_ID.get(Riferimento),this);
    }//GEN-LAST:event_Bottone_ModificaActionPerformed

    private void TabellaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_TabellaMouseReleased
        // TODO add your handling code here:
        Funzioni_RichiamaPopUpdaTabella(Tabella,evt);
    }//GEN-LAST:event_TabellaMouseReleased

    private void MenuItem_CopiaIDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_CopiaIDActionPerformed
        // TODO add your handling code here:
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(Principale.PopUp_IDTrans);
        clipboard.setContents(stringSelection, null);
    }//GEN-LAST:event_MenuItem_CopiaIDActionPerformed

    private void MenuItem_CopiaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_CopiaActionPerformed
        // TODO add your handling code here:
        Funzioni.simulaCtrlC();
    }//GEN-LAST:event_MenuItem_CopiaActionPerformed

    private void MenuItem_DocumentoFonteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_DocumentoFonteActionPerformed
        Principale_DocumentiFonte.ApriDocumentoDiOrigine(Principale.PopUp_IDTrans, this);
    }//GEN-LAST:event_MenuItem_DocumentoFonteActionPerformed


    
    
    private void MenuItem_ModificaPrezzoMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_MenuItem_ModificaPrezzoMouseReleased
        // TODO add your handling code here:
    }//GEN-LAST:event_MenuItem_ModificaPrezzoMouseReleased

    private void MenuItem_ModificaPrezzoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_ModificaPrezzoActionPerformed
        ApriModificaPrezzo(IDdt);
    }//GEN-LAST:event_MenuItem_ModificaPrezzoActionPerformed

    private void Bottone_ModificaPrezzoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_ModificaPrezzoActionPerformed
        ApriModificaPrezzo(IDdt);
    }//GEN-LAST:event_Bottone_ModificaPrezzoActionPerformed

    /**
     * Chiude il dettaglio, apre {@link GUI_ModificaPrezzo} sul movimento e, alla sua chiusura, riapre il
     * dettaglio nella stessa posizione. Usato sia dal pulsante sia dalla voce del menu contestuale: per
     * questo prende l'ID mostrato ({@code IDdt}) e non {@code Principale.PopUp_IDTrans}, che dopo una
     * navigazione con le frecce, o senza aver mai aperto il menu, può riferirsi a un altro movimento.
     * @param IDtrans ID del movimento di cui modificare il prezzo
     */
    private void ApriModificaPrezzo(String IDtrans) {
        if (IDtrans == null || Principale.MappaCryptoWallet.get(IDtrans) == null) {
            return;
        }
        Component c=this;
        Download progress=new Download();
        progress.MostraProgressAttesa("Scaricamento Prezzi", "Attendi scaricamento dei prezzi...");
        progress.setLocationRelativeTo(this);
        Point p = this.getLocation();
        this.dispose();
        Thread thread;
        thread = new Thread() {
            /** Apre in background il dialogo {@link GUI_ModificaPrezzo} per il movimento mostrato. */
            public void run() {

                //Il costruttore chiude la finestra di attesa appena le tabelle sono pronte
                GUI_ModificaPrezzo t =new GUI_ModificaPrezzo(IDtrans, progress);
                t.setLocationRelativeTo(c);
                t.setVisible(true);
                //Il dettaglio si riapre solo dopo la chiusura del dialogo (modale, setVisible è bloccante)
                SwingUtilities.invokeLater(() -> {
                    //Il prezzo cambiato va nei costi di carico e nelle plusvalenze prima di riaprire il dettaglio,
                    //altrimenti il dettaglio mostrerebbe i valori dell'ultimo ricalcolo
                    AggiornaTuttoSeModificato();
                    GUI_DettaglioTransazione d =new GUI_DettaglioTransazione();
                    d.AzzeraMap();
                    d.TransazioniCrypto_CompilaTextPaneDatiMovimento(IDtrans);
                    d.setLocation(p);
                    d.setVisible(true);
                });
            }
        };
        thread.start();
        progress.setVisible(true);
    }

    /**
     * Se un movimento è stato modificato ({@link Principale#TabellaCryptodaAggiornare}) esegue subito
     * l'aggiornamento completo della finestra principale, lo stesso che partirebbe al suo ritorno in primo
     * piano, e abbassa il segnale così che quel ritorno non lo ripeta. Rispetta l'opzione del ricalcolo
     * manuale delle plusvalenze, perché passa da {@code Funzioni_AggiornaTutto()}.
     */
    private static void AggiornaTuttoSeModificato() {
        if (!TabellaCryptodaAggiornare) {
            return;
        }
        for (java.awt.Frame f : java.awt.Frame.getFrames()) {
            if (f instanceof Principale P && f.isDisplayable()) {
                TabellaCryptodaAggiornare = false;
                P.Funzioni_AggiornaTutto();
                return;
            }
        }
    }

    private void MenuItem_ModificaNoteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_ModificaNoteActionPerformed
        // TODO add your handling code here:
        if (Funzioni.GUIModificaNote(Principale.PopUp_Component, Principale.PopUp_IDTrans))TabellaCryptodaAggiornare=true;
        TransazioniCrypto_CompilaTextPaneDatiMovimento(IDdt);

    }//GEN-LAST:event_MenuItem_ModificaNoteActionPerformed

    private void MenuItem_EsportaTabellaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MenuItem_EsportaTabellaActionPerformed

        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Download progress = new Download();
        progress.MostraProgressAttesa("Export in Excel", "Esportazione in corso...");
        progress.setLocationRelativeTo(this);

        // Esegui l'export in background
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            /** Esporta in background la tabella corrente in un file Excel. */
            @Override
            protected Void doInBackground() throws Exception {
                if (Principale.PopUp_Tabella != null) {
                    Funzioni.Export_CreaExcelDaTabella(Principale.PopUp_Tabella);
                }
                return null;
            }

            /** Chiude la finestra di progresso e ripristina il cursore al termine dell'export. */
            @Override
            protected void done() {
                progress.dispose();
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        };

        worker.execute();
        progress.setVisible(true);// Questo blocca finché done() non chiama dispose()

    }//GEN-LAST:event_MenuItem_EsportaTabellaActionPerformed

    
    private void Funzioni_RichiamaPopUpdaTabella(JTable tabella, java.awt.event.MouseEvent evt) {
        
        System.out.println("ID:"+IDdt);

        if (!Funzioni.PopUp_ClickInternoASelezione(tabella, evt)) {
            tabella.requestFocusInWindow();
            int row = tabella.rowAtPoint(evt.getPoint());
            int col = tabella.columnAtPoint(evt.getPoint());

            if (row != -1 && col != -1) {
                tabella.setRowSelectionInterval(row, row);
                tabella.setColumnSelectionInterval(col, col);
                tabella.changeSelection(row, col, false, false);
            }

        }
        int rigaSelezionata = tabella.getSelectedRow();
        if (rigaSelezionata != -1) {

            //Questo popup lavora sempre su un solo movimento: allineo comunque la lista delle righe
            //selezionate, altrimenti resterebbe quella dell'ultimo menu contestuale aperto altrove
            Principale.PopUp_IDTransSelezionati = new java.util.ArrayList<>();
            if (IDdt != null && Principale.MappaCryptoWallet.get(IDdt) != null) {
                Principale.PopUp_IDTransSelezionati.add(IDdt);
            }

            Funzioni.PopUpMenu(this, evt, PopupMenu, IDdt);

        }
    }
    
    
    
      /**
       * Apre il dialogo {@link GUI_ModificaMovimento} per modificare il movimento indicato. Se il
       * movimento fa parte di un gruppo di movimenti collegati (es. uno scambio, con commissione o
       * reward generati automaticamente) e non è esso stesso un movimento automatico, chiede prima
       * conferma all'utente e, in caso affermativo, riporta l'intero gruppo alla situazione
       * originale (tramite {@link GUI_ClassificazioneMovimento#RiportaTransazioniASituazioneIniziale})
       * prima di aprire il form di modifica. Al termine della modifica riapre automaticamente il
       * dialogo di dettaglio sul movimento risultante (che può avere un nuovo ID se ne è stato
       * generato uno).
       * @param ID ID del movimento da modificare
       * @param c componente da cui recuperare la finestra ancestor per i messaggi di conferma
       */
      public void Funzione_ModificaMovimento(String ID,Component c){
            GUI_ModificaMovimento a = new GUI_ModificaMovimento();
            String riga[]=Principale.MappaCryptoWallet.get(ID);

          String PartiCoinvolte[] = (riga[0] + "," + riga[20]).split(",");
          if (PartiCoinvolte.length > 1 && !riga[22].equalsIgnoreCase("AU")) 
          {//devo permettere di modificare i movimenti automatici generati dagli scambi per poter cambiare eventualmente il prezzo
              if (Messaggi.Personalizzati_SINO_ModificaMovimento(SwingUtilities.getWindowAncestor(c))) {

                  ID = GUI_ClassificazioneMovimento.RiportaTransazioniASituazioneIniziale(PartiCoinvolte, ID);
                  //Il ripristino ha già cambiato l'archivio (movimenti automatici tolti, ID ripristinati),
                  //anche se la modifica che segue venisse annullata
                  TabellaCryptodaAggiornare = true;

                  //String id=mappa_ID.get(Riferimento);
                  Point p = this.getLocation();
                  this.dispose();
                  a.CompilaCampidaID(ID);
                  a.setLocation(p);
                  a.setVisible(true);
                  if (!a.IDNuovo.isEmpty()) {
                      ID = a.IDNuovo;
                  }

                  //Quando finisco la modifica apro di nuovo la maschera con il movimento, già ricalcolato
                  AggiornaTuttoSeModificato();
                  GUI_DettaglioTransazione t = new GUI_DettaglioTransazione();
                  t.AzzeraMap();
                  t.TransazioniCrypto_CompilaTextPaneDatiMovimento(ID);
                  t.setLocation(p);
                  t.setVisible(true);
              } else {
                  Messaggi.WarningMessage("Operazione Annullata", "", SwingUtilities.getWindowAncestor(c));
              }

          } else {
                //String id=mappa_ID.get(Riferimento);
                Point p = this.getLocation();
                this.dispose();
                a.CompilaCampidaID(ID);
                a.setLocation(p);               
                a.setVisible(true);
                if (!a.IDNuovo.isEmpty())ID=a.IDNuovo;
                
                //Quando finisco la modifica apro di nuovo la maschera con il movimento
                
               // CDC_Grafica.
               String IDTr=ID;
               SwingUtilities.invokeLater(() -> {
            AggiornaTuttoSeModificato();
            GUI_DettaglioTransazione t =new GUI_DettaglioTransazione();
                t.AzzeraMap();
                t.TransazioniCrypto_CompilaTextPaneDatiMovimento(IDTr);
                t.setLocation(p);
                t.setVisible(true);
        });

            }
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(GUI_DettaglioTransazione.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GUI_DettaglioTransazione.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GUI_DettaglioTransazione.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GUI_DettaglioTransazione.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GUI_DettaglioTransazione().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_DeFi;
    private javax.swing.JButton Bottone_Modifica;
    private javax.swing.JButton Bottone_ModificaPrezzo;
    private javax.swing.JButton Bottone_MovPrecedente;
    private javax.swing.JButton Bottone_MovSuccessivo;
    private javax.swing.JButton Bottone_Storico;
    private javax.swing.JMenuItem MenuItem_Copia;
    private javax.swing.JMenuItem MenuItem_CopiaID;
    private javax.swing.JMenuItem MenuItem_DocumentoFonte;
    private javax.swing.JMenuItem MenuItem_EsportaTabella;
    private javax.swing.JMenuItem MenuItem_ModificaNote;
    private javax.swing.JMenuItem MenuItem_ModificaPrezzo;
    private javax.swing.JPanel Pannello_Giacenze;
    private javax.swing.JPopupMenu PopupMenu;
    private javax.swing.JScrollPane ScrollTabella;
    private javax.swing.JTable Tabella;
    private javax.swing.JTextPane TextPane_Titolo;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    // End of variables declaration//GEN-END:variables

    /**
     * I due estremi di un trasferimento PTW/DTW, per la riga "Trasferimento da ... a ..." dei dettagli.
     * <p>La controparte si cerca fra gli ID di {@code v[20]} con il marcatore opposto e la stessa moneta
     * (uscente del PTW = entrante del DTW). Non basta prendere "l'unico altro ID": {@code v[20]} elenca anche la
     * commissione di trasferimento creata dalla classificazione ({@code CM}), il reward di un rientro da Vault e,
     * in uno scambio differito, le altre tre gambe con altri PTW/DTW. Fino al 2026-10-04 con più di un ID la riga
     * restava "da  a " vuota, cioè su ogni trasferimento che aveva generato una commissione.
     * @return {@code {walletPrelievo, walletDeposito}}, stringhe vuote se non determinabili
     */
    public static String[] WalletDelTrasferimento(String[] v) {
        String WalletPrelievo = "";
        String WalletDeposito = "";
        boolean prelievo = v[18].contains("PTW");
        if (prelievo) WalletPrelievo = v[3];
        else if (v[18].contains("DTW")) WalletDeposito = v[3];
        else return new String[]{WalletPrelievo, WalletDeposito};
        for (String IdM : v[20].split(",")) {
            String Mov[] = Principale.MappaCryptoWallet.get(IdM.trim());
            if (Mov == null) continue;
            if (prelievo && Mov[18].contains("DTW") && Mov[11].equals(v[8])) {
                WalletDeposito = Mov[3];
                break;
            }
            if (!prelievo && Mov[18].contains("PTW") && Mov[8].equals(v[11])) {
                WalletPrelievo = Mov[3];
                break;
            }
        }
        return new String[]{WalletPrelievo, WalletDeposito};
    }

}
