package com.giacenzecrypto.giacenze_crypto;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Il gruppo wallet nelle tabelle del quadro RT si disegna con l'alias ({@code "101 ( Binance )"}), ma il modello resta
 * {@code "Wallet 101"}: quel valore viene riletto come chiave (tabella LiFo, Giacenze a data).
 */
class Principale_DichiarazioniAliasGruppoTest {

    private static String Disegnato(JTable t, int riga, int colonna) {
        Component c = t.prepareRenderer(t.getCellRenderer(riga, colonna), riga, colonna);
        return ((JLabel) c).getText();
    }

    @Test
    void ilModelloResta_ilRendererMostraLAlias() {
        DefaultTableModel m = new DefaultTableModel(new Object[][]{
            {"Wallet 101", "BTC"}, {"Wallet 102", "ETH"}, {"", "SOL"}}, new Object[]{"Wallet", "Moneta"});
        JTable t = new JTable(m);
        Map<String, String[]> alias = new HashMap<>();
        alias.put("Wallet 101", new String[]{"Wallet 101", "Binance", "N"});
        alias.put("Wallet 102", new String[]{"Wallet 102", " ", "N"});
        Principale_Dichiarazioni.MostraAliasGruppo(t, 0, () -> alias);

        assertEquals("101 ( Binance )", Disegnato(t, 0, 0));
        assertEquals("Wallet 102", Disegnato(t, 1, 0), "alias vuoto: resta il nome del gruppo");
        assertEquals("", Disegnato(t, 2, 0));
        assertEquals("BTC", Disegnato(t, 0, 1), "le altre colonne non cambiano");
        assertEquals("Wallet 101", m.getValueAt(0, 0), "il modello non va toccato");

        alias.put("Wallet 101", new String[]{"Wallet 101", "Kraken", "N"});
        assertEquals("101 ( Kraken )", Disegnato(t, 0, 0), "la mappa e' riletta a ogni disegno");
    }
}
