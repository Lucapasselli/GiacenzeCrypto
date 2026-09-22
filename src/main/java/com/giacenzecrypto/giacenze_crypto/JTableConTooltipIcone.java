/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;

import java.awt.Point;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
/**
 *
 * @author lucap
 */
public class JTableConTooltipIcone extends JTable {

    public JTableConTooltipIcone() {
        super();
    }

    /**
     * Mostra un tooltip HTML esplicativo solo quando il mouse è sopra l'icona di allerta di una cella (colonne
     * 5 = movimento non classificato, 15 = transazione senza prezzo, 19 = movimento in ingresso mancante, con
     * calcolo della quantità/valore mancante tramite {@link #RecuperaQtaMancante}); altrove nella cella non
     * mostra alcun tooltip.
     * <p>
     * Per la colonna 19 il calcolo della quantità/valore mancante non può sempre basarsi sui campi "in uscita"
     * (v[8]/v[10]) della riga stessa: un deposito di solo trasferimento tra gruppi wallet (DTW) può risultare
     * "A" (giacenza LiFo insufficiente nel gruppo di origine, vedi {@code Calcoli_PlusvalenzeNew.java:1035}) pur
     * non avendo esso stesso una moneta in uscita — quei campi sono vuoti per costruzione su una riga di solo
     * ingresso. In quel caso si usano i campi "in ingresso" della riga (v[11]/v[13]/v[15]), sempre valorizzati.
     * @param e evento del mouse da cui ricavare la cella e la posizione del cursore
     * @return il tooltip HTML da mostrare, oppure {@code null} se il cursore non è sopra un'icona di allerta nota
     */
    @Override
    public String getToolTipText(MouseEvent e) {
        Point p = e.getPoint();
        int viewRow = rowAtPoint(p);
        int viewCol = columnAtPoint(p);

        if (viewRow == -1 || viewCol == -1) return null;

        //int modelRow = convertRowIndexToModel(viewRow);
        int modelCol = convertColumnIndexToModel(viewCol);

        Component comp = prepareRenderer(getCellRenderer(viewRow, viewCol), viewRow, viewCol);
        if (comp instanceof JLabel label) {
            Icon icon = label.getIcon();
            if (icon != null) {
                Rectangle cellRect = getCellRect(viewRow, viewCol, false);
                Insets insets = label.getInsets();
                String ID = this.getModel().getValueAt(viewRow, 0).toString();
                String mov[]=Principale.MappaCryptoWallet.get(ID);
                String MonetaU;
                String QtaRiferimento; //quantità su cui rapportare il valore mancante: quella uscente se presente, altrimenti quella entrante
                String ValRiferimento;
                if (Funzioni.isNumeric(mov[10], false)) {
                    //Riga con una moneta in uscita (vendita, prelievo, scambio...): comportamento originale
                    MonetaU = mov[8];
                    QtaRiferimento = mov[10];
                    ValRiferimento = mov[15];
                } else {
                    //Riga di solo deposito (es. DTW): non ha una moneta "in uscita" propria, ma l'eventuale
                    //"A" riguarda comunque la moneta ricevuta, la cui quantità/valore sono in v[11]/v[13]/v[15]
                    MonetaU = mov[11];
                    QtaRiferimento = mov[13];
                    ValRiferimento = mov[15];
                }
                String QtaMancanteLiFo="";
                String ValRimanenze="";
                if (Funzioni.isNumeric(QtaRiferimento, false))
                {
                    BigDecimal QtaU = new BigDecimal(QtaRiferimento);
                    QtaMancanteLiFo=RecuperaQtaMancante(ID);
                    if(QtaMancanteLiFo!=null&&Funzioni.isNumeric(QtaMancanteLiFo, false)
                            &&Funzioni.isNumeric(ValRiferimento, false)&&QtaU.compareTo(BigDecimal.ZERO)!=0)
                    {
                        BigDecimal ValTrans=new BigDecimal(ValRiferimento);
                        ValRimanenze=ValTrans.divide(QtaU, 10, RoundingMode.HALF_UP).multiply(new BigDecimal(QtaMancanteLiFo)).setScale(2, RoundingMode.HALF_UP).abs().toPlainString();
                    }
                    //if (QtaMancanteLiFo==null)
                }
                int iconWidth = icon.getIconWidth();
                int iconHeight = icon.getIconHeight();
                int iconX = cellRect.x + insets.left;
                int iconY = cellRect.y + (cellRect.height - iconHeight) / 2;

                if (e.getX() >= iconX && e.getX() <= iconX + iconWidth &&
                    e.getY() >= iconY && e.getY() <= iconY + iconHeight) {
                    
                    boolean opzioneNCAttiva = "NO".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi("PL_CosiderareMovimentiNC", "SI"));
                    return switch (modelCol) {
                        case 5 -> {
                            //Il movimento è escluso dal calcolo delle plusvalenze finché resta non classificato
                            //solo se l'opzione è attiva: lo segnaliamo qui, alla fonte, e non solo nel messaggio
                            //"MOVIMENTO IN INGRESSO MANCANTE" che ne subisce l'effetto altrove (v. case 19).
                            String nota = opzioneNCAttiva ? """
    ⚠ Con l'opzione <i>"I Movimenti di deposito e prelievo non classificati non vengono considerati nel
    calcolo delle plusvalenze fino alla loro classificazione"</i> attiva (Opzioni → Opzioni di Calcolo
    Plusvalenze), questo movimento è <b>escluso</b> da ogni calcolo finché non viene classificato.<br><br>
""" : "";
                            yield """
<html>
    <b>MOVIMENTO NON CLASSIFICATO</b><br><br>
    %s\
    Per sistemarlo:<br>
    &nbsp;• Usa la funzione <i><b>'Classificazione Depositi/Prelievi'</b></i><br>
    &nbsp;• Oppure clicca col tasto destro e seleziona <i><b>'Classifica Movimento'</b></i>
</html>
""".formatted(nota);
                        }
                        case 15 -> """
<html>
    <b>TRANSAZIONE SENZA PREZZO</b><br><br>
    Per sistemarla:<br>
    &nbsp;• Premi su <i><b>'Modifica Movimento'</b></i> e assegna un prezzo.<br>
    &nbsp;• Oppure clicca col tasto destro e seleziona <i><b>'Modifica Prezzo'</b></i><br>
    &nbsp;• Oppure classifica il movimento come Scam dalla funzione <i><b>'Classificazione Depositi/Prelievi'</b></i>
</html>
""";
                        case 19 -> {
                            //Stessa "A" (giacenza LiFo insufficiente), due cause ben diverse: una lacuna vera
                            //nello storico (-> Verifica Saldi Negativi) o quantità solo esclusa dal calcolo
                            //perché non classificata e l'opzione sopra è attiva (-> classificarla, o disattivarla).
                            //Il messaggio generico va bene solo per la prima; per la seconda è fuorviante.
                            boolean causaEsclusione = opzioneNCAttiva && EsistonoMovimentiNonClassificatiAMonte(mov, MonetaU);
                            if (causaEsclusione) {
                                yield String.format("""
<html>
    <b>MOVIMENTO IN INGRESSO MANCANTE</b><br><br>
    Mancano acquisti per <b>%s %s</b> corrispondenti ad un valore di <b>€ %s</b><br><br>
    Nella cronologia di %s risultano depositi/prelievi <b>non classificati</b>:<br>
    con l'opzione <i>"I Movimenti di deposito e prelievo non classificati non vengono considerati nel
    calcolo delle plusvalenze fino alla loro classificazione"</i> attiva, il loro valore non entra nel
    calcolo finché non vengono classificati.<br><br>
    Per sistemarlo:<br>
    &nbsp;• Classifica quei movimenti con <i><b>'Classificazione Depositi/Prelievi'</b></i>, oppure<br>
    &nbsp;• Disattiva l'opzione in <i><b>Opzioni → Opzioni di Calcolo Plusvalenze</b></i>, se vuoi che contribuiscano comunque.<br><br>
    Se il problema persiste anche dopo, usa <i><b>'Verifica Saldi Negativi'</b></i> nella tab <i><b>'Analisi Crypto'</b></i>.
</html>
""", QtaMancanteLiFo, MonetaU, ValRimanenze, MonetaU);
                            } else {
                                yield String.format("""
<html>
    <b>MOVIMENTO IN INGRESSO MANCANTE</b><br><br>
    Mancano acquisti per <b>%s %s</b> corrispondenti ad un valore di <b>€ %s</b><br><br>
    Per sistemarlo:<br>
    &nbsp;• Utilizzare l'apposita funzione <i><b>'Verifica Saldi Negativi'</b></i> presente nella tab <i><b>'Analisi Crypto'</b></i>.<br>
</html>
""", QtaMancanteLiFo, MonetaU, ValRimanenze);
                            }
                            //mov[38]="";
                        }
                        default -> "";
                    };
                }
            }
        }

        return null;
    }
    
    private String RecuperaQtaMancante(String ID){
        Calcoli_PlusvalenzeNew.LifoXID lifoID=Calcoli_PlusvalenzeNew.getIDLiFo(ID);
        if (lifoID==null)return null;
        ArrayDeque<String[]> StackUscito=lifoID.Get_CryptoStackUscito();
        ArrayDeque<String[]> stack=StackUscito.clone();
        //System.out.println(stack.size());
        while (!stack.isEmpty()) {
            String[] ultimoRecupero = stack.pop();
            String mov[]=Principale.MappaCryptoWallet.get(ultimoRecupero[3]);
            if (mov==null){
                return ultimoRecupero[1];
            }
        }
        return "";
    }

    /**
     * Vero se, per {@code Moneta} nello stesso gruppo wallet di {@code mov} (stesso criterio usato da
     * {@code Calcoli_PlusvalenzeNew.ElaboraMovimento}: tutti nello stesso gruppo "Wallet 01" se l'opzione
     * {@code PlusXWallet} è disattiva), esiste almeno un deposito/prelievo <b>non classificato</b> (categoria
     * {@code DC}/{@code PC}, campo18 vuoto) datato non successivamente a {@code mov}. Usato dal case 19 per
     * distinguere una vera lacuna nello storico da una quantità solo esclusa dal calcolo perché non
     * classificata (vedi {@code Funzioni.MovimentoRilevante}, opzione {@code PL_CosiderareMovimentiNC}).
     * <p>
     * Scansione lineare su {@link Principale#MappaCryptoWallet}: costo trascurabile per una chiamata legata
     * all'hover del mouse su un'icona, non al rendering della tabella.
     * @param mov riga del movimento sotto il cursore
     * @param Moneta moneta la cui cronologia va controllata (quella coinvolta nell'anomalia "A")
     * @return {@code true} se esiste almeno un movimento non classificato della stessa moneta e gruppo wallet
     */
    private static boolean EsistonoMovimentiNonClassificatiAMonte(String[] mov, String Moneta) {
        if (Moneta == null || Moneta.isBlank() || mov == null || mov[1] == null) return false;
        boolean PlusXWallet = "SI".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi("PlusXWallet"));
        String GruppoWallet = PlusXWallet ? DatabaseH2.Pers_GruppoWallet_Leggi(mov[3], true) : "Wallet 01";
        for (String[] v : Principale.MappaCryptoWallet.values()) {
            if (v[1] == null || v[1].compareTo(mov[1]) > 0) continue; //solo movimenti non successivi
            if (v[18] == null || !v[18].isBlank()) continue; //deve essere non classificato
            if (!Moneta.equalsIgnoreCase(v[8]) && !Moneta.equalsIgnoreCase(v[11])) continue;
            String[] idts = v[0].split("_");
            if (idts.length <= 4 || !(idts[4].equals("DC") || idts[4].equals("PC"))) continue;
            String GruppoV = PlusXWallet ? DatabaseH2.Pers_GruppoWallet_Leggi(v[3], true) : "Wallet 01";
            if (GruppoWallet.equalsIgnoreCase(GruppoV)) return true;
        }
        return false;
    }
}
