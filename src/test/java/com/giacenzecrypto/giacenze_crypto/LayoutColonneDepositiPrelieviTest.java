package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Layout delle colonne della tabella dei depositi/prelievi da classificare ({@link LayoutColonneMovimenti#PROFILO_DEPOSITI_PRELIEVI}).
 */
class LayoutColonneDepositiPrelieviTest {

    private static final LayoutColonneMovimenti.Profilo PROFILO = LayoutColonneMovimenti.PROFILO_DEPOSITI_PRELIEVI;

    private static JTable tabella() {
        return new JTable(new DefaultTableModel(new Object[0][0], new String[]{
            "ID", "Data", "Exchange", "Tipo", "Moneta", "Qta", "Dettaglio", "Prezzo", "Defi/CSV", "Controparte",
            "Gruppo Wallet", "Note"}));
    }

    private static List<Integer> modelliInVista(JTable t) {
        List<Integer> l = new ArrayList<>();
        for (int v = 0; v < t.getColumnModel().getColumnCount(); v++) {
            l.add(t.getColumnModel().getColumn(v).getModelIndex());
        }
        return l;
    }

    @Test
    void ilDefaultMetteIlGruppoAccantoAlWalletLeNoteInFondoENascondeLID() {
        JTable t = tabella();
        LayoutColonneMovimenti.applica(t, null, PROFILO);
        assertEquals(List.of(1, 2, 10, 3, 4, 5, 6, 7, 8, 9, 11), modelliInVista(t));
    }

    @Test
    void unaColonnaNascostaSparisceEIFissiRestano() {
        JTable t = tabella();
        //L'utente ha tenuto solo Data, Note e Moneta, e ha provato a togliere Tipo e Qta (fisse)
        LayoutColonneMovimenti.applica(t, new LayoutColonneMovimenti(PROFILO, List.of(1, 11, 4), Map.of(11, 300)), PROFILO);
        assertEquals(List.of(1, 11, 4, 3, 5), modelliInVista(t));
        LayoutColonneMovimenti letto = LayoutColonneMovimenti.fromJson(LayoutColonneMovimenti.daTabella(t, PROFILO).toJson(), PROFILO);
        assertNotNull(letto);
        assertEquals(List.of(1, 11, 4, 3, 5), letto.ordine());
        //L'ID salvato per errore viene scartato
        assertEquals(List.of(1, 11), LayoutColonneMovimenti.fromJson("{\"v\":1,\"col\":[{\"m\":0},{\"m\":1},{\"m\":11}]}", PROFILO).ordine());
    }
}
