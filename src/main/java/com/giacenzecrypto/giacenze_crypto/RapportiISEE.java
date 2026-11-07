package com.giacenzecrypto.giacenze_crypto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * I dati dei rapporti ISEE scritti dall'utente (identificativo, codice operatore, apertura, chiusura), nella tabella
 * {@code ISEE_RAPPORTI} di {@code personale.mv.db}. Una riga per chiave di {@link Calcoli_ISEE.Rapporto#chiave}
 * ({@code gruppo|tipo|identità del periodo FIAT}), uguale negli anni: le date sono quelle vere del rapporto, non
 * dell'anno di riferimento. Il motore {@link Calcoli_ISEE} le riceve da {@link #datiUtente()}.
 *
 * <p>Fino al 2026-11-05 l'identificativo ISEE stava sul periodo FIAT ({@code GRUPPO_PERIODO_RW.IdentificativoISEE}),
 * uno per tutto il gruppo. {@link #MigraIdentificativiPeriodi()} lo sposta qui a ogni apertura del database e
 * svuota la colonna, così vale anche per un backup vecchio ripristinato e un dato cancellato nel tab non ritorna.</p>
 */
public final class RapportiISEE {

    private RapportiISEE() {
    }

    static void CreaTabella(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS ISEE_RAPPORTI ("
                    + "Chiave VARCHAR(400) PRIMARY KEY, Identificativo VARCHAR(255), Operatore VARCHAR(40), "
                    + "DataApertura VARCHAR(10), DataChiusura VARCHAR(10))");
        }
    }

    /** Tutti i dati scritti dall'utente, per chiave. */
    public static Map<String, Calcoli_ISEE.DatiUtente> Leggi() {
        Map<String, Calcoli_ISEE.DatiUtente> out = new TreeMap<>();
        Connection c = DatabaseH2.connectionPersonale;
        if (c == null) {
            return out;
        }
        try (Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT Chiave, Identificativo, Operatore, DataApertura, DataChiusura FROM ISEE_RAPPORTI")) {
            while (rs.next()) {
                Calcoli_ISEE.DatiUtente d = new Calcoli_ISEE.DatiUtente();
                d.identificativo = testo(rs.getString(2));
                d.operatore = testo(rs.getString(3));
                d.apertura = data(rs.getString(4));
                d.chiusura = data(rs.getString(5));
                out.put(rs.getString(1), d);
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return out;
    }

    /** I dati per il motore: una lettura sola, poi la mappa. */
    public static Function<String, Calcoli_ISEE.DatiUtente> datiUtente() {
        return Leggi()::get;
    }

    /**
     * Scrive i dati di un rapporto. Tutti vuoti = la riga si cancella (il rapporto torna ai dati dei periodi FIAT).
     *
     * @return {@code false} se la scrittura non è riuscita
     */
    public static boolean Scrivi(String chiave, Calcoli_ISEE.DatiUtente d) {
        if (chiave == null || chiave.isBlank()) {
            throw new IllegalArgumentException("Chiave del rapporto vuota.");
        }
        if (vuoto(d)) {
            return Cancella(chiave);
        }
        Connection c = DatabaseH2.connectionPersonale;
        try (PreparedStatement ps = c.prepareStatement("MERGE INTO ISEE_RAPPORTI "
                + "(Chiave, Identificativo, Operatore, DataApertura, DataChiusura) KEY (Chiave) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, chiave);
            ps.setString(2, testo(d.identificativo));
            ps.setString(3, testo(d.operatore).toUpperCase());
            ps.setString(4, d.apertura == null ? "" : d.apertura.toString());
            ps.setString(5, d.chiusura == null ? "" : d.chiusura.toString());
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
            return false;
        }
    }

    public static boolean Cancella(String chiave) {
        Connection c = DatabaseH2.connectionPersonale;
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM ISEE_RAPPORTI WHERE Chiave = ?")) {
            ps.setString(1, chiave);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
            return false;
        }
    }

    /**
     * Le chiavi salvate che non corrispondono più a nessun periodo FIAT del loro gruppo: il periodo è stato
     * cancellato o rinumerato, oppure il gruppo ha avuto il suo primo periodo dopo che i dati erano stati scritti
     * su un tratto successivo. Il motore non le legge più: il tab le mostra perché l'utente le riassegni o le
     * cancelli. Il confronto è con i tratti dei periodi, non con i rapporti di un anno, così un rapporto chiuso
     * negli anni passati non risulta orfano.
     *
     * @param periodiDi gruppo → periodi in forma GUI ({@link Principale_GruppiWalletRW#caricaPeriodi})
     */
    public static Set<String> Orfane(Collection<String> chiavi, Function<String, List<String[]>> periodiDi) {
        Set<String> out = new TreeSet<>();
        Map<String, Set<String>> valide = new TreeMap<>();
        for (String chiave : chiavi) {
            String[] p = chiave.split("\\|", -1);
            if (p.length != 3 || !(Calcoli_ISEE.TIPO_CONTO_CORRENTE.equals(p[1])
                    || Calcoli_ISEE.TIPO_ALTRI_STRUMENTI.equals(p[1]))) {
                out.add(chiave);
                continue;
            }
            Set<String> id = valide.computeIfAbsent(p[0], g -> {
                Set<String> s = new HashSet<>();
                for (Calcoli_ISEE.Segmento seg : Calcoli_ISEE.segmenti(periodiDi.apply(g))) {
                    s.addAll(seg.identitaDiRicerca());
                }
                return s;
            });
            if (!id.contains(p[2])) {
                out.add(chiave);
            }
        }
        return out;
    }

    /**
     * Sposta l'identificativo ISEE scritto sui periodi FIAT nella tabella dei rapporti e svuota la colonna. Per ogni
     * tratto con lo stesso operatore prende il valore del periodo più recente che ne ha uno e lo scrive sia sul
     * rapporto 01 sia sul 99 (il campo valeva per tutto il gruppo), solo dove l'utente non ha già scritto un
     * identificativo. Si può rilanciare: un periodo già svuotato non porta più nulla.
     *
     * @return quanti rapporti hanno ricevuto un identificativo
     */
    public static int MigraIdentificativiPeriodi() {
        int scritti = 0;
        Map<String, Calcoli_ISEE.DatiUtente> esistenti = Leggi();
        for (String gruppo : gruppiConIdentificativoISEE()) {
            List<String> svuotare = new ArrayList<>();
            for (Calcoli_ISEE.Segmento s : Calcoli_ISEE.segmenti(Principale_GruppiWalletRW.caricaPeriodi(gruppo))) {
                String valore = "";
                for (String[] r : s.righe) {
                    String v = testo(r[Principale_GruppiWalletRW.COL_IDENT_ISEE]);
                    if (!v.isEmpty()) {
                        valore = v; // righe in ordine di inizio: vince il più recente
                        svuotare.add(r[Principale_GruppiWalletRW.COL_PROGRESSIVO]);
                    }
                }
                if (valore.isEmpty()) {
                    continue;
                }
                for (String tipo : List.of(Calcoli_ISEE.TIPO_CONTO_CORRENTE, Calcoli_ISEE.TIPO_ALTRI_STRUMENTI)) {
                    String chiave = Calcoli_ISEE.chiave(gruppo, tipo, s.identita);
                    Calcoli_ISEE.DatiUtente d = esistenti.get(chiave);
                    if (d == null) {
                        d = new Calcoli_ISEE.DatiUtente();
                    } else if (!testo(d.identificativo).isEmpty()) {
                        continue;
                    }
                    d.identificativo = valore;
                    if (Scrivi(chiave, d)) {
                        esistenti.put(chiave, d);
                        scritti++;
                    }
                }
            }
            for (String prog : svuotare) {
                svuotaIdentificativoISEE(gruppo, prog);
            }
        }
        return scritti;
    }

    private static Set<String> gruppiConIdentificativoISEE() {
        Set<String> out = new TreeSet<>();
        try (Statement st = DatabaseH2.connectionPersonale.createStatement();
                ResultSet rs = st.executeQuery("SELECT DISTINCT Gruppo FROM GRUPPO_PERIODO_RW WHERE TipoRigo = '"
                        + Principale_GruppiWalletRW.TIPO_FIAT + "' AND TRIM(IdentificativoISEE) <> ''")) {
            while (rs.next()) {
                out.add(rs.getString(1));
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return out;
    }

    /** Diretto e non da {@code Pers_GruppoPeriodoRW_Scrivi}, dove {@code null} vuol dire "colonna non toccata". */
    private static void svuotaIdentificativoISEE(String gruppo, String progressivo) {
        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement("UPDATE GRUPPO_PERIODO_RW "
                + "SET IdentificativoISEE = NULL WHERE Gruppo = ? AND TipoRigo = ? AND Progressivo = ?")) {
            ps.setString(1, gruppo);
            ps.setString(2, Principale_GruppiWalletRW.TIPO_FIAT);
            ps.setInt(3, Integer.parseInt(testo(progressivo)));
            ps.executeUpdate();
        } catch (SQLException | NumberFormatException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }

    private static boolean vuoto(Calcoli_ISEE.DatiUtente d) {
        return d == null || (testo(d.identificativo).isEmpty() && testo(d.operatore).isEmpty()
                && d.apertura == null && d.chiusura == null);
    }

    private static String testo(String s) {
        return s == null ? "" : s.trim();
    }

    private static LocalDate data(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
