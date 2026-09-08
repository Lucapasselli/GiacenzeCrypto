package com.giacenzecrypto.giacenze_crypto;

import java.awt.Color;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import com.toedter.calendar.JDateChooser;

/**
 * Piccoli aiuti condivisi dai dialoghi di modifica riga del quadro W/RW
 * ({@link GUI_ModificaExchange}, {@link GUI_ModificaPeriodoExchange},
 * {@link GUI_ModificaPeriodoDetenzione}) : conversione data ISO {@code <->} {@link JDateChooser} e
 * adattamento del chooser al tema (vedi la nota su {@code JDateChooser} in {@code CLAUDE.md} :
 * l'editor si riscrive il foreground a {@code Color.BLACK} a ogni {@code setDate}, quindi sui chooser
 * fissi di finestra si schiarisce lo <i>sfondo</i> e si lascia il testo nero).
 */
final class RwDialoghi {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private RwDialoghi() {
    }

    /** Nuovo {@link JDateChooser} in formato {@code yyyy-MM-dd}, già adattato al tema. */
    static JDateChooser nuovoDateChooser() {
        JDateChooser c = new JDateChooser();
        configura(c);
        return c;
    }

    /**
     * Applica a un {@link JDateChooser} già costruito (es. da {@code initComponents()} del GUI
     * builder) il formato {@code yyyy-MM-dd} e l'adattamento al tema.
     */
    static void configura(JDateChooser c) {
        if (c == null) {
            return;
        }
        c.setDateFormatString("yyyy-MM-dd");
        try {
            c.getCalendarButton().setToolTipText("Scegli la data dal calendario");
            c.setBackground(Color.lightGray);
        } catch (RuntimeException ignore) {
        }
    }

    /** {@code yyyy-MM-dd} → data del chooser ({@code null}/non valida = campo vuoto). */
    static void impostaData(JDateChooser chooser, String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            chooser.setDate(null);
            return;
        }
        try {
            LocalDate d = LocalDate.parse(isoDate.trim(), ISO);
            chooser.setDate(Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        } catch (RuntimeException e) {
            chooser.setDate(null);
        }
    }

    /** Data del chooser → {@code yyyy-MM-dd}, o stringa vuota se non impostata. */
    static String leggiData(JDateChooser chooser) {
        Date d = chooser.getDate();
        if (d == null) {
            return "";
        }
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(ISO);
    }
}
