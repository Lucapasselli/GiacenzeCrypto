package com.giacenzecrypto.giacenze_crypto;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Layout delle colonne della tabella dettaglio movimenti di "Giacenze a data" ({@link LayoutColonneMovimenti#PROFILO_GIACENZE_DETTAGLIO})
 * e colore verde/rosso di Quantita' e Differenza.
 */
class LayoutColonneGiacenzeDettaglioTest {

    private static final LayoutColonneMovimenti.Profilo PROFILO = LayoutColonneMovimenti.PROFILO_GIACENZE_DETTAGLIO;

    private static JTable tabella() {
        return new JTable(new DefaultTableModel(new Object[0][0], new String[]{
            "Data", "Wallet", "Moneta", "Address", "Tipo", "Qta", "Valore", "Qta Residua", "ID", "Saldi", "null", "null", "null",
            "Costo Mov", "Prezzo", "Valore Res", "Costo Res", "Differenza"}));
    }

    private static List<Integer> modelliInVista(JTable t) {
        List<Integer> l = new ArrayList<>();
        for (int v = 0; v < t.getColumnModel().getColumnCount(); v++) {
            l.add(t.getColumnModel().getColumn(v).getModelIndex());
        }
        return l;
    }

    @Test
    void ilDefaultMostraIlCostoDelMovimentoPrimaDellaQtaResiduaENonMostraLeColonneInterne() {
        JTable t = tabella();
        LayoutColonneMovimenti.applica(t, null, PROFILO);
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 13, 7, 14, 15, 16, 17), modelliInVista(t));
    }

    @Test
    void unaColonnaNascostaSparisceDallaVistaEIFissiRestano() {
        JTable t = tabella();
        //L'utente ha tolto Wallet e Prezzo, e ha provato a togliere anche la Qta Residua (fissa)
        LayoutColonneMovimenti scelta = new LayoutColonneMovimenti(PROFILO, List.of(0, 2, 5, 13, 16), Map.of(2, 70));
        LayoutColonneMovimenti.applica(t, scelta, PROFILO);
        assertEquals(List.of(0, 2, 5, 13, 16, 7), modelliInVista(t));
    }

    @Test
    void ilLayoutSiSalvaERileggeConSuoProfilo() {
        JTable t = tabella();
        LayoutColonneMovimenti.applica(t, new LayoutColonneMovimenti(PROFILO, List.of(5, 0, 17, 7), Map.of(5, 90)), PROFILO);
        String json = LayoutColonneMovimenti.daTabella(t, PROFILO).toJson();
        LayoutColonneMovimenti letto = LayoutColonneMovimenti.fromJson(json, PROFILO);
        assertNotNull(letto);
        assertEquals(List.of(5, 0, 17, 7), letto.ordine());
        assertSame(PROFILO, letto.profilo());
        //Un layout salvato con una colonna interna (8 = ID) la scarta
        assertEquals(List.of(0, 7), LayoutColonneMovimenti.fromJson("{\"v\":1,\"col\":[{\"m\":0},{\"m\":8},{\"m\":7}]}", PROFILO).ordine());
    }

    @Test
    void ilProfiloDeiMovimentiNonCambia() {
        assertEquals(LayoutColonneMovimenti.ORDINE_DEFAULT, LayoutColonneMovimenti.predefinito().ordine());
        assertSame(LayoutColonneMovimenti.PROFILO_MOVIMENTI, LayoutColonneMovimenti.predefinito().profilo());
    }

    @Test
    void ilColoreSegnoEVerdeSePositivoRossoSeNegativo() {
        Color normale = Color.BLACK;
        assertEquals(Tabelle.verdeScuro, Tabelle.ColoreSegno("12.34", normale));
        assertEquals(Tabelle.rosso, Tabelle.ColoreSegno("-0.01", normale));
        assertEquals(normale, Tabelle.ColoreSegno("0.00", normale));
        assertEquals(normale, Tabelle.ColoreSegno("", normale));
        assertEquals(normale, Tabelle.ColoreSegno(null, normale));
        assertEquals(normale, Tabelle.ColoreSegno("abc", normale));
        //Un esponente negativo non e' un segno meno: 2.5E-9 e' positivo
        assertEquals(Tabelle.verdeScuro, Tabelle.ColoreSegno("2.5E-9", normale));
    }
}
