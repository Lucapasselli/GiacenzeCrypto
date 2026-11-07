package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;

import java.awt.Window;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Logica del tab "ISEE" di Dichiarazioni (il Quadro FC2 della DSU): anni, calcolo con il pre-scarico dei prezzi di
 * fine anno, righe delle tabelle, modifica dei dati di un rapporto e dati rimasti senza rapporto. Nessun campo Swing:
 * la parte grafica è {@link GUI_ISEE_Pannello}, il calcolo {@link Calcoli_ISEE}, i dati dell'utente
 * {@link RapportiISEE}.
 */
public final class Principale_ISEE {

    private Principale_ISEE() {
    }

    private static final DateTimeFormatter GG_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Gli anni di riferimento selezionabili: dal primo movimento all'anno scorso, il più recente per primo. */
    public static List<Integer> Anni(int annoCorrente) {
        int primo = annoCorrente - 2;
        if (!MappaCryptoWallet.isEmpty()) {
            try {
                primo = Math.min(primo, Integer.parseInt(MappaCryptoWallet.firstKey().substring(0, 4)));
            } catch (RuntimeException ignorato) {
                // ID non nel formato con la data in testa: resta l'anno proposto
            }
        }
        List<Integer> out = new ArrayList<>();
        for (int a = annoCorrente - 1; a >= primo; a--) {
            out.add(a);
        }
        return out;
    }

    /** L'anno proposto: il secondo precedente, cioè il patrimonio della DSU presentata quest'anno. */
    public static int AnnoProposto(int annoCorrente) {
        return annoCorrente - 2;
    }

    /** Etichetta della tendina: l'anno del patrimonio e la DSU in cui si usa. */
    public static String EtichettaAnno(int anno) {
        return "31/12/" + anno + " (DSU " + (anno + 2) + ")";
    }

    /**
     * Scarica in un lotto i prezzi di fine anno delle monete in giacenza, poi calcola i rapporti con i dati scritti
     * dall'utente. Da chiamare in un thread di background.
     *
     * @return {@code null} se l'utente ha interrotto
     */
    public static Calcoli_ISEE.Risultato Calcola(int anno, Download progress) {
        if (progress != null) {
            progress.SetLabel("Prezzi di fine " + anno + " in corso...");
        }
        Prezzi.PreScaricaPrezziMonete(Calcoli_ISEE.moneteAFineAnno(anno), Calcoli_ISEE.istanteFineAnno(anno),
                progress, "ISEE");
        if (progress != null && progress.FineThread()) {
            return null;
        }
        if (progress != null) {
            progress.SetLabel("Calcolo dei rapporti del " + anno + " in corso...");
        }
        return Calcoli_ISEE.calcola(anno, RapportiISEE.datiUtente());
    }

    // --- tabella dei rapporti --------------------------------------------------

    /** Colonne della tabella dei rapporti, nell'ordine del model. */
    public static final int COL_GRUPPO = 0, COL_TIPO = 1, COL_OPERATORE = 2, COL_IDENTIFICATIVO = 3,
            COL_INIZIO = 4, COL_FINE = 5, COL_VALORE = 6, COL_MEDIA = 7, COL_AVVISI = 8;

    /** Una riga della tabella dei rapporti. */
    public static Object[] Riga(Calcoli_ISEE.Rapporto r) {
        String valore = r.copreFineAnno || r.valore.signum() != 0 ? euro(r.valore) : "0";
        if (r.valoreIncompleto) {
            valore += " (incompleto)";
        }
        String media = "";
        if (r.giacenzaMedia != null) {
            media = euro(r.giacenzaMedia) + (r.mediaIncompleta ? " (incompleta)" : "");
        }
        return new Object[] {
            Calcoli_ISEE.aliasDi(r.gruppo),
            r.contoCorrente() ? "01 Conto corrente" : "99 Altri strumenti",
            r.operatore,
            r.identificativo,
            data(r.dataInizio),
            data(r.dataFine),
            valore,
            media,
            r.note.isEmpty() ? "" : r.note.size() == 1 ? "1 avviso" : r.note.size() + " avvisi"
        };
    }

    /** Il dettaglio di un rapporto, come coppie voce/valore. */
    public static List<Object[]> Dettaglio(Calcoli_ISEE.Rapporto r) {
        List<Object[]> out = new ArrayList<>();
        if (r == null) {
            return out;
        }
        out.add(new Object[] {"Gruppo wallet", Calcoli_ISEE.aliasDi(r.gruppo) + " (" + r.gruppo + ")"});
        out.add(new Object[] {"Codice operatore dal periodo FIAT", r.operatorePeriodo
                + (r.statoEstero.isEmpty() ? "" : ", Stato estero " + StatiEsteri.etichetta(r.statoEstero))});
        out.add(new Object[] {"Giorni del rapporto nell'anno", data(r.da) + " - " + data(r.a)});
        if (r.contoCorrente()) {
            out.add(new Object[] {"Saldo al 31/12 (EUR)", r.copreFineAnno ? euro2(r.valore) : "rapporto cessato prima del 31/12"});
            out.add(new Object[] {"Giacenza media annua (EUR, giorni / 365)", r.giacenzaMedia == null ? "" : euro2(r.giacenzaMedia)});
        } else if (r.copreFineAnno) {
            for (Calcoli_ISEE.Posizione p : r.posizioni) {
                out.add(new Object[] {"Moneta " + p.moneta, p.qtaFine.stripTrailingZeros().toPlainString() + " = "
                        + (p.valoreFine == null ? "n.d. (prezzo mancante)" : euro2(p.valoreFine) + " EUR")});
            }
            if (r.conLiquidita) {
                out.add(new Object[] {"Liquidità in valuta (EUR)", euro2(r.liquidita)});
            }
            out.add(new Object[] {"Valore al 31/12 (EUR)", euro2(r.valore) + (r.valoreIncompleto ? " (incompleto)" : "")});
        } else {
            out.add(new Object[] {"Valore al 31/12 (EUR)", "0: rapporto cessato prima del 31/12, il valore sta sul rapporto successivo"});
        }
        if (!r.contoCorrente()) {
            out.add(new Object[] {"Giacenza media giornaliera (EUR, informativa)", r.giacenzaMedia == null
                    ? "non calcolata: pulsante \"Media giornaliera...\""
                    : euro2(r.giacenzaMedia) + (r.mediaIncompleta ? " (incompleta: " + r.giorniSenzaPrezzo
                    + (r.giorniSenzaPrezzo == 1 ? " giorno" : " giorni") + " con giacenza senza prezzo, vedi \"Prezzi mancanti\")" : "")});
        }
        for (String n : r.note) {
            out.add(new Object[] {"Avviso", n});
        }
        Calcoli_ISEE.DatiUtente d = RapportiISEE.Leggi().get(r.chiave);
        out.add(new Object[] {"Dati scritti a mano", d == null ? "nessuno" : DescriviDati(d)});
        return out;
    }

    /** I totali dell'anno per tipo, come testo breve. */
    public static String Riepilogo(Calcoli_ISEE.Risultato ris) {
        if (ris == null) {
            return "";
        }
        BigDecimal tot01 = BigDecimal.ZERO;
        BigDecimal tot99 = BigDecimal.ZERO;
        boolean incompleto = false;
        for (Calcoli_ISEE.Rapporto r : ris.rapporti) {
            if (r.contoCorrente()) {
                tot01 = tot01.add(Calcoli_ISEE.euro(r.valore));
            } else {
                tot99 = tot99.add(Calcoli_ISEE.euro(r.valore));
            }
            incompleto |= r.valoreIncompleto;
        }
        return ris.rapporti.size() + (ris.rapporti.size() == 1 ? " rapporto" : " rapporti")
                + ", valore al 31/12: 01 = " + euro(tot01) + " EUR, 99 = " + euro(tot99) + " EUR"
                + (incompleto ? " (incompleto)" : "");
    }

    // --- dati scritti dall'utente ------------------------------------------------

    /**
     * Apre la modifica dei dati di un rapporto e li salva. I campi partono dai dati <b>salvati</b>, non da quelli
     * calcolati: confermare senza toccare nulla non deve trasformare l'operatore del periodo FIAT o il primo
     * movimento in una scelta dell'utente, che poi non seguirebbe più le correzioni del periodo.
     *
     * @return {@code true} se i dati sono cambiati (il chiamante ricalcola)
     */
    public static boolean ModificaDati(Calcoli_ISEE.Rapporto r, Window owner) {
        if (r == null) {
            return false;
        }
        Calcoli_ISEE.DatiUtente salvati = RapportiISEE.Leggi().get(r.chiave);
        String titolo = Calcoli_ISEE.aliasDi(r.gruppo) + ", rapporto "
                + (r.contoCorrente() ? "01 conto corrente" : "99 altri strumenti");
        GUI_ModificaRapportoISEE dlg = new GUI_ModificaRapportoISEE(owner, titolo, salvati,
                Calcoli_ISEE.aliasDi(r.gruppo), r.operatorePeriodo);
        dlg.setVisible(true);
        if (!dlg.confermato) {
            return false;
        }
        if (!RapportiISEE.Scrivi(r.chiave, dlg.risultato)) {
            Messaggi.WarningMessage("Dati non salvati", "La scrittura nel database non è riuscita: i dettagli sono nel log.", owner);
            return false;
        }
        return true;
    }

    /**
     * I dati scritti dall'utente che non corrispondono più a nessun periodo FIAT del loro gruppo (periodo cancellato
     * o rinumerato): il calcolo non li legge più.
     */
    public static Map<String, Calcoli_ISEE.DatiUtente> DatiScollegati() {
        Map<String, Calcoli_ISEE.DatiUtente> tutti = RapportiISEE.Leggi();
        Set<String> orfane = RapportiISEE.Orfane(tutti.keySet(), Principale_GruppiWalletRW::caricaPeriodi);
        Map<String, Calcoli_ISEE.DatiUtente> out = new LinkedHashMap<>();
        for (String k : orfane) {
            out.put(k, tutti.get(k));
        }
        return out;
    }

    /** Descrizione leggibile di una chiave di rapporto: gruppo, tipo e periodo FIAT. */
    public static String DescriviChiave(String chiave) {
        String[] p = chiave.split("\\|", -1);
        if (p.length != 3) {
            return chiave;
        }
        String periodo = Calcoli_ISEE.SENZA_PERIODO.equals(p[2]) ? "senza periodo FIAT"
                : p[2].matches("P\\d+") ? "periodo FIAT n. " + p[2].substring(1) : "periodo FIAT predefinito " + p[2];
        return Calcoli_ISEE.aliasDi(p[0]) + ", tipo " + p[1] + ", " + periodo;
    }

    /** I dati scritti, in una riga. */
    public static String DescriviDati(Calcoli_ISEE.DatiUtente d) {
        List<String> parti = new ArrayList<>();
        if (!d.identificativo.isBlank()) {
            parti.add("identificativo " + d.identificativo);
        }
        if (!d.operatore.isBlank()) {
            parti.add("operatore " + d.operatore);
        }
        if (d.apertura != null) {
            parti.add("apertura " + data(d.apertura));
        }
        if (d.chiusura != null) {
            parti.add("chiusura " + data(d.chiusura));
        }
        return parti.isEmpty() ? "nessuno" : String.join(", ", parti);
    }

    /** Cancella i dati scollegati di una chiave, dopo conferma. */
    public static boolean EliminaDatiScollegati(String chiave, Calcoli_ISEE.DatiUtente d, Window owner) {
        AppDialog.DialogResult res = AppDialog.builder(owner)
                .windowTitle("Dati senza rapporto")
                .bodyTitle("Eliminare questi dati?")
                .showTitleInBody(false)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message(DescriviChiave(chiave))
                .details(DescriviDati(d) + "\n\nNessun periodo FIAT del gruppo corrisponde più a questi dati, quindi il "
                        + "calcolo non li usa. Se valgono per un rapporto ancora presente, vanno riscritti su quel rapporto.")
                .action(AppDialog.DialogAction.builder("annulla", "Annulla").role(AppDialog.ActionRole.SECONDARY).build())
                .action(AppDialog.DialogAction.builder("elimina", "Elimina").role(AppDialog.ActionRole.DANGER).build())
                .showDialog();
        return res != null && res.isAction("elimina") && RapportiISEE.Cancella(chiave);
    }

    // --- formati ---------------------------------------------------------------

    private static String data(LocalDate d) {
        return d == null ? "" : d.format(GG_MM_AAAA);
    }

    /** Euro arrotondati all'unità come nella DSU, con il separatore delle migliaia. */
    static String euro(BigDecimal v) {
        return NumberFormat.getIntegerInstance(Locale.ITALY).format(Calcoli_ISEE.euro(v));
    }

    private static String euro2(BigDecimal v) {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.ITALY);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return f.format(v);
    }
}
