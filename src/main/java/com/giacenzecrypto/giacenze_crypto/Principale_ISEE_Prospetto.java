package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.Mappa_EMoney;

import java.awt.Cursor;
import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

/**
 * Il prospetto DSU del tab "ISEE": i rapporti del Quadro FC2 di un anno archiviati in {@code Dichiarazioni/} come i
 * quadri W/RW e T/RT, cioè un PDF a tabelle (Sez. I e Sez. II con le colonne del modulo, il dettaglio di ogni rapporto,
 * gli avvisi, le opzioni e le note), un Excel e il manifest, generati insieme da {@link Principale_Dichiarazioni#Genera}.
 *
 * <p>Il PDF non si sovrappone al modulo INPS (scelta dell'utente del 2026-11-07): le cripto non sono ancora nel modello
 * DSU, il prospetto è una traccia da ricopiare. La media giornaliera dei 99 non va mai nelle colonne del modulo (la
 * Sez. II non ne ha una): sta in una colonna a parte, dichiarata informativa, e "non calcolata" se non c'è.</p>
 *
 * <p>Stesso schema delle altre estrazioni: metodi statici, nessun campo Swing. L'anno della dichiarazione è quello del
 * patrimonio (31/12), non quello di presentazione della DSU: vedi {@link Dichiarazioni.Famiglia#ISEE}.</p>
 */
public final class Principale_ISEE_Prospetto {

    private Principale_ISEE_Prospetto() {
    }

    /** Colonne della Sez. I nel PDF e nell'Excel: quelle del modulo, con il gruppo wallet in testa. */
    static final String[] COLONNE_SEZ_I = {"Gruppo wallet", "Tipo rapporto", "Identificativo rapporto",
        "Codice fiscale dell'operatore finanziario", "Saldo al 31/12", "Giacenza media", "Data inizio", "Data fine"};
    /** Colonne della Sez. II: quelle del modulo, poi la media giornaliera informativa (il modulo non la chiede). */
    static final String[] COLONNE_SEZ_II = {"Gruppo wallet", "Tipo rapporto", "Identificativo rapporto",
        "Codice fiscale dell'operatore finanziario", "Valore", "Data inizio", "Data fine",
        "Media giornaliera (informativa)"};

    /** Totali scritti nel manifest. */
    static final String TOT_DSU = "DSU";
    static final String TOT_RAPPORTI = "Rapporti";
    static final String TOT_SEZ_I_SALDI = "Sez. I - Totale saldi al 31/12 (A), rapporti del prospetto";
    static final String TOT_SEZ_I_MEDIE = "Sez. I - Totale giacenze medie (B), rapporti del prospetto";
    static final String TOT_SEZ_II = "Sez. II - Totale valori al 31/12";

    static final String NON_CALCOLATA = "non calcolata";

    /** Le voci della media giornaliera nel manifest: un calcolo senza media non le confronta con la definitiva. */
    static final String RIGO_MEDIA = "Media giornaliera informativa";
    static final String OPZIONE_MEDIA = "Media giornaliera dei rapporti 99";
    static final String ERRORE_MEDIA = "media giornaliera informativa incompleta in ";

    // --- archiviazione -------------------------------------------------------------

    /**
     * Genera e archivia il prospetto dell'ultimo calcolo, poi apre il PDF o l'Excel. Il calcolo lo rifà chi chiama,
     * subito prima: i file archiviati devono nascere dai dati di adesso.
     *
     * @return la dichiarazione archiviata, {@code null} se annullata o fallita
     */
    public static Dichiarazioni.Dichiarazione Genera(Calcoli_ISEE.Risultato ris, boolean apriExcel, Window owner) {
        if (ris == null) {
            return null;
        }
        String anno = String.valueOf(ris.anno);
        String filePdf = "Prospetto_DSU_" + (ris.anno + 2) + "_patrimonio_" + anno + ".pdf";
        String fileExcel = "Dettaglio_DSU_" + (ris.anno + 2) + "_patrimonio_" + anno + ".xlsx";
        Dichiarazioni.Dichiarazione bozza = Bozza(ris);
        Dichiarazioni.Dichiarazione esito = Principale_Dichiarazioni.Genera(bozza, List.of(filePdf, fileExcel), cartella -> {
            if (owner != null) {
                owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            }
            ScriviPdf(ris, bozza, new File(cartella, filePdf));
            try {
                ScriviExcel(ris, new File(cartella, fileExcel));
            } catch (IOException | RuntimeException ex) {
                throw new IOException("errore nell'Excel di dettaglio: " + ex, ex);
            }
        }, apriExcel ? fileExcel : filePdf, owner);
        if (owner != null) {
            owner.setCursor(Cursor.getDefaultCursor());
        }
        return esito;
    }

    /**
     * La fotografia dei valori del prospetto da archiviare nel manifest: un rigo per rapporto con le colonne del
     * modulo, i totali, gli errori (valore o media incompleti, giacenze negative) e gli input che cambiano i valori.
     */
    static Dichiarazioni.Dichiarazione Bozza(Calcoli_ISEE.Risultato ris) {
        Dichiarazioni.Dichiarazione d = new Dichiarazioni.Dichiarazione();
        d.Fam = Dichiarazioni.Famiglia.ISEE;
        d.Anno = String.valueOf(ris.anno);
        BigDecimal saldi01 = BigDecimal.ZERO;
        BigDecimal medie01 = BigDecimal.ZERO;
        BigDecimal valori99 = BigDecimal.ZERO;
        List<String> incompleti = new ArrayList<>();
        List<String> medieIncomplete = new ArrayList<>();
        List<String> negative = new ArrayList<>();
        for (Calcoli_ISEE.Rapporto r : ris.rapporti) {
            String nome = NomeRapporto(r);
            Map<String, String> rigo = new LinkedHashMap<>();
            rigo.put("Rapporto", nome);
            rigo.put("Sezione", r.contoCorrente() ? "I" : "II");
            rigo.put("Tipo rapporto", r.tipo);
            rigo.put("Identificativo", r.identificativo);
            rigo.put("Codice operatore", r.operatore);
            rigo.put("Valore al 31/12", Calcoli_ISEE.euro(r.valore).toPlainString());
            if (r.contoCorrente()) {
                rigo.put("Giacenza media", r.giacenzaMedia == null ? "" : Calcoli_ISEE.euro(r.giacenzaMedia).toPlainString());
            } else {
                rigo.put(RIGO_MEDIA, MediaInformativa(r, false));
            }
            rigo.put("Data inizio", Principale_ISEE.data(r.dataInizio));
            rigo.put("Data fine", Principale_ISEE.data(r.dataFine));
            rigo.put("Valore incompleto", r.valoreIncompleto ? "Sì" : "");
            rigo.put("Avvisi", String.valueOf(r.note.size()));
            d.Righi.add(rigo);

            if (r.contoCorrente()) {
                saldi01 = saldi01.add(Calcoli_ISEE.euro(r.valore));
                if (r.giacenzaMedia != null) {
                    medie01 = medie01.add(Calcoli_ISEE.euro(r.giacenzaMedia));
                }
            } else {
                valori99 = valori99.add(Calcoli_ISEE.euro(r.valore));
            }
            if (r.valoreIncompleto) {
                incompleti.add(nome);
            }
            if (r.mediaIncompleta) {
                medieIncomplete.add(nome);
            }
            if (r.haNegative()) {
                negative.add(nome);
            }
        }
        d.Totali.put(TOT_DSU, String.valueOf(ris.anno + 2));
        d.Totali.put(TOT_RAPPORTI, String.valueOf(ris.rapporti.size()));
        d.Totali.put(TOT_SEZ_I_SALDI, saldi01.toPlainString());
        d.Totali.put(TOT_SEZ_I_MEDIE, medie01.toPlainString());
        d.Totali.put(TOT_SEZ_II, valori99.toPlainString());
        d.Totali.put(Principale_Dichiarazioni.TOT_ISEE_VALORE, saldi01.add(valori99).toPlainString());

        List<String> errori = new ArrayList<>();
        if (!incompleti.isEmpty()) {
            errori.add("valore al 31/12 incompleto (prezzi mancanti) in " + String.join(", ", incompleti));
        }
        if (!negative.isEmpty()) {
            errori.add("giacenze negative contate zero in " + String.join(", ", negative));
        }
        if (!medieIncomplete.isEmpty()) {
            //Per ultimo: SenzaMedia lo toglie tagliando da qui
            errori.add(ERRORE_MEDIA + String.join(", ", medieIncomplete));
        }
        d.Errori = String.join(", ", errori);

        Map<String, String> o = d.Opzioni;
        o.put("Valore delle cripto al 31/12", "fotografia del quadro W/RW, token SCAM esclusi");
        o.put(OPZIONE_MEDIA, ris.mediaCalcolata
                ? "calcolata con i prezzi del calcolo fiscale (informativa)" : NON_CALCOLATA);
        List<String> emoney = new ArrayList<>();
        for (Map.Entry<String, String> e : Mappa_EMoney.entrySet()) {
            emoney.add(e.getKey() + " dal " + e.getValue());
        }
        o.put("Token E-Money", emoney.isEmpty() ? "nessuno" : String.join(", ", emoney));
        o.put("Token DeFi prezzati solo da DefiLlama e CoinGecko",
                "SI".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi(Prezzi.OPZIONE_DEFI_SOLO_DEFILLAMA_COINGECKO, "NO")) ? "Sì" : "No");
        if (FallimentiPiattaforme.InfluisconoSullAnno(d.Anno)) {
            o.put(FallimentiPiattaforme.NOME_OPZIONE, FallimentiPiattaforme.DescrizioneValore());
        }
        return d;
    }

    /**
     * Le differenze fra il prospetto definitivo dell'anno e il calcolo appena fatto, vuote se non c'è una definitiva o
     * se coincidono. La media giornaliera dei 99 si calcola solo a richiesta: se il calcolo non l'ha, la media non si
     * confronta (altrimenti ogni definitiva stampata con la media risulterebbe diversa da un semplice <i>Calcola</i>).
     */
    static List<String[]> DifferenzeDallaDefinitiva(Calcoli_ISEE.Risultato ris, Dichiarazioni.Dichiarazione def) {
        if (ris == null || def == null) {
            return List.of();
        }
        Dichiarazioni.Dichiarazione ora = Bozza(ris);
        if (!ris.mediaCalcolata) {
            return Dichiarazioni.Confronta(SenzaMedia(def), SenzaMedia(ora));
        }
        return Dichiarazioni.Confronta(def, ora);
    }

    /** Una copia della dichiarazione senza le voci della media giornaliera dei 99. */
    private static Dichiarazioni.Dichiarazione SenzaMedia(Dichiarazioni.Dichiarazione d) {
        Dichiarazioni.Dichiarazione c = new Dichiarazioni.Dichiarazione();
        c.Fam = d.Fam;
        c.Anno = d.Anno;
        c.Totali.putAll(d.Totali);
        for (Map<String, String> r : d.Righi) {
            Map<String, String> copia = new LinkedHashMap<>(r);
            copia.remove(RIGO_MEDIA);
            c.Righi.add(copia);
        }
        c.Opzioni.putAll(d.Opzioni);
        c.Opzioni.remove(OPZIONE_MEDIA);
        int i = d.Errori.indexOf(ERRORE_MEDIA);
        c.Errori = i < 0 ? d.Errori : d.Errori.substring(0, Math.max(0, i - 2));
        return c;
    }

    // --- PDF -----------------------------------------------------------------------

    /** Scrive il prospetto PDF. Nessun dialogo: lancia eccezione se il file non risulta valido. */
    static void ScriviPdf(Calcoli_ISEE.Risultato ris, Dichiarazioni.Dichiarazione bozza, File f) throws IOException {
        String anno = String.valueOf(ris.anno);
        String dsu = "DSU " + (ris.anno + 2);
        Stampe stampa = new Stampe(f.getPath());
        stampa.AttivaVesteProspetto("QUADRO FC2", dsu);
        stampa.ApriDocumento();
        stampa.AggiungiCopertina("Quadro FC2", "DSU - patrimonio mobiliare", "Patrimonio al",
                "31/12/" + anno + " (" + dsu + ")", new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));

        stampa.NuovaPagina();
        stampa.ContestoPagina("Quadro FC2 - " + dsu + " - patrimonio al 31/12/" + anno, false);
        stampa.AggiungiTitoloSezione("Quadro FC2 - Sez. I, depositi e conti correnti",
                "Rapporti 01: saldo al 31/12 e giacenza media annua, in euro arrotondati all'unità");
        List<String[]> sez1 = RigheSezioneI(ris);
        if (sez1.isEmpty()) {
            stampa.AggiungiParagrafoReport("Nessun rapporto 01.", false, 8.5f);
        } else {
            sez1.add(new String[] {"Totale dei rapporti del prospetto", "", "", "",
                Principale_ISEE.euro(new BigDecimal(bozza.Totali.get(TOT_SEZ_I_SALDI))),
                Principale_ISEE.euro(new BigDecimal(bozza.Totali.get(TOT_SEZ_I_MEDIE))), "", ""});
            stampa.AggiungiTabellaReport(COLONNE_SEZ_I, new float[] {12, 9, 22, 14, 10, 10, 9, 9},
                    new boolean[] {false, false, false, false, true, true, false, false}, sez1, Set.of(sez1.size() - 1));
        }

        stampa.AggiungiParagrafoReport("Quadro FC2 - Sez. II, altre forme di patrimonio mobiliare", true, 11f);
        stampa.AggiungiParagrafoReport("Rapporti 99: valore al 31/12, in euro arrotondati all'unità. La media "
                + "giornaliera non è una colonna del modulo.", false, 8.5f);
        List<String[]> sez2 = RigheSezioneII(ris);
        if (sez2.isEmpty()) {
            stampa.AggiungiParagrafoReport("Nessun rapporto 99.", false, 8.5f);
        } else {
            sez2.add(new String[] {"Totale", "", "", "",
                Principale_ISEE.euro(new BigDecimal(bozza.Totali.get(TOT_SEZ_II))), "", "", ""});
            stampa.AggiungiTabellaReport(COLONNE_SEZ_II, new float[] {12, 9, 22, 13, 10, 9, 9, 11},
                    new boolean[] {false, false, false, false, true, false, false, true}, sez2, Set.of(sez2.size() - 1));
        }
        if (!bozza.Errori.isBlank()) {
            stampa.AggiungiParagrafoReport("Attenzione: " + bozza.Errori + ".", true, 9f);
        }

        if (!ris.rapporti.isEmpty()) {
            stampa.NuovaPagina();
            stampa.ContestoPagina("Quadro FC2 - " + dsu + " - dettaglio dei rapporti", false);
            stampa.AggiungiTitoloSezione("Dettaglio dei rapporti",
                    "Monete al 31/12 e avvisi di ogni rapporto, importi in euro con due decimali");
            for (Calcoli_ISEE.Rapporto r : ris.rapporti) {
                stampa.AggiungiParagrafoReport(NomeRapporto(r) + " - operatore " + r.operatore
                        + " - giorni " + Principale_ISEE.data(r.da) + " / " + Principale_ISEE.data(r.a), true, 9.5f);
                stampa.AggiungiTabellaReport(Principale_ISEE.COLONNE_MONETE, new float[] {22, 22, 16, 40},
                        new boolean[] {false, true, true, false}, Testi(Principale_ISEE.Monete(r)), null);
                for (String n : r.note) {
                    stampa.AggiungiParagrafoReport("Avviso: " + n, false, 8f);
                }
            }
        }

        stampa.NuovaPagina();
        stampa.ContestoPagina("Quadro FC2 - " + dsu + " - opzioni e note", false);
        stampa.AggiungiTitoloSezione("Opzioni e note", "Gli input del calcolo che cambiano i valori del prospetto");
        List<String[]> opzioni = new ArrayList<>();
        for (Map.Entry<String, String> e : bozza.Opzioni.entrySet()) {
            opzioni.add(new String[] {e.getKey(), e.getValue()});
        }
        stampa.AggiungiTabellaReport(new String[] {"Opzione", "Valore"}, new float[] {35, 65}, null, opzioni, null);
        stampa.AggiungiHtml(NoteCompilazione.Testo(NoteCompilazione.DSU, anno, Map.of("annoDSU", String.valueOf(ris.anno + 2))));
        if (!stampa.ScriviPDF(false)) {
            throw new IOException("il prospetto PDF non risulta scritto");
        }
    }

    /** Le righe della Sez. I (rapporti 01) per il PDF. */
    static List<String[]> RigheSezioneI(Calcoli_ISEE.Risultato ris) {
        List<String[]> out = new ArrayList<>();
        for (Calcoli_ISEE.Rapporto r : ris.rapporti) {
            if (r.contoCorrente()) {
                out.add(new String[] {Calcoli_ISEE.aliasDi(r.gruppo), r.tipo, r.identificativo, r.operatore,
                    Valore(r), r.giacenzaMedia == null ? "" : Principale_ISEE.euro(r.giacenzaMedia),
                    Principale_ISEE.data(r.dataInizio), Principale_ISEE.data(r.dataFine)});
            }
        }
        return out;
    }

    /** Le righe della Sez. II (rapporti 99) per il PDF. */
    static List<String[]> RigheSezioneII(Calcoli_ISEE.Risultato ris) {
        List<String[]> out = new ArrayList<>();
        for (Calcoli_ISEE.Rapporto r : ris.rapporti) {
            if (!r.contoCorrente()) {
                out.add(new String[] {Calcoli_ISEE.aliasDi(r.gruppo), r.tipo, r.identificativo, r.operatore,
                    Valore(r), Principale_ISEE.data(r.dataInizio), Principale_ISEE.data(r.dataFine),
                    MediaInformativa(r, true)});
            }
        }
        return out;
    }

    /** Il valore al 31/12 in euro interi, con l'avviso se manca qualche prezzo. */
    private static String Valore(Calcoli_ISEE.Rapporto r) {
        return Principale_ISEE.euro(r.valore) + (r.valoreIncompleto ? " (incompleto)" : "");
    }

    /**
     * La media giornaliera di un 99: "non calcolata" finché non la si chiede, mai vuota né zero.
     *
     * @param formattata {@code true} con il separatore delle migliaia (PDF), {@code false} come numero (manifest)
     */
    private static String MediaInformativa(Calcoli_ISEE.Rapporto r, boolean formattata) {
        if (r.giacenzaMedia == null) {
            return NON_CALCOLATA;
        }
        return (formattata ? Principale_ISEE.euro(r.giacenzaMedia) : Calcoli_ISEE.euro(r.giacenzaMedia).toPlainString())
                + (r.mediaIncompleta ? " (incompleta)" : "");
    }

    /** {@code "Binance - 99"}: il nome del rapporto negli errori e nel manifest. */
    static String NomeRapporto(Calcoli_ISEE.Rapporto r) {
        return Calcoli_ISEE.aliasDi(r.gruppo) + " - " + r.tipo
                + (r.dataFine != null ? " fino al " + Principale_ISEE.data(r.dataFine)
                : r.dataInizio != null ? " dal " + Principale_ISEE.data(r.dataInizio) : "");
    }

    private static List<String[]> Testi(List<Object[]> righe) {
        List<String[]> out = new ArrayList<>();
        for (Object[] riga : righe) {
            String[] t = new String[riga.length];
            for (int i = 0; i < riga.length; i++) {
                t[i] = riga[i] == null ? "" : riga[i].toString();
            }
            out.add(t);
        }
        return out;
    }

    // --- Excel ---------------------------------------------------------------------

    /**
     * Scrive l'Excel di dettaglio: i righi del Quadro FC2, le monete al 31/12 per rapporto, gli avvisi e, se la media
     * giornaliera è stata calcolata, i giorni rimasti senza prezzo. Gli importi sono numeri con due decimali, il
     * PDF li arrotonda all'unità come la DSU.
     */
    static void ScriviExcel(Calcoli_ISEE.Risultato ris, File f) throws IOException {
        String anno = String.valueOf(ris.anno);
        try (FileOutputStream fos = new FileOutputStream(f)) {
            Workbook wb = new Workbook(fos, "excel1", "1.0");

            Worksheet ws = wb.newWorksheet("Quadro FC2 " + anno);
            Riga(ws, 0, new Object[] {"Gruppo wallet", "Alias", "Sezione", "Tipo rapporto",
                "Identificativo rapporto", "Codice fiscale dell'operatore finanziario", "Valore / saldo al 31/12 (EUR)",
                "Valore incompleto", "Giacenza media annua 01 (EUR)", "Media giornaliera 99 (EUR, informativa)",
                "Media incompleta", "Data inizio", "Data fine", "Primo giorno nell'anno", "Ultimo giorno nell'anno", "Avvisi"});
            int riga = 1;
            for (Calcoli_ISEE.Rapporto r : ris.rapporti) {
                Riga(ws, riga++, new Object[] {r.gruppo, Calcoli_ISEE.aliasDi(r.gruppo),
                    r.contoCorrente() ? "I" : "II", r.tipo, r.identificativo, r.operatore, Due(r.valore),
                    r.valoreIncompleto ? "Sì" : "",
                    r.contoCorrente() && r.giacenzaMedia != null ? Due(r.giacenzaMedia) : "",
                    r.contoCorrente() ? "" : r.giacenzaMedia == null ? NON_CALCOLATA : Due(r.giacenzaMedia),
                    r.mediaIncompleta ? "Sì" : "",
                    Principale_ISEE.data(r.dataInizio), Principale_ISEE.data(r.dataFine),
                    Principale_ISEE.data(r.da), Principale_ISEE.data(r.a), String.join(" | ", r.note)});
            }
            ws.finish();

            ws = wb.newWorksheet("Monete " + anno);
            Riga(ws, 0, new Object[] {"Gruppo wallet", "Alias", "Tipo rapporto",
                "Moneta", "Quantità al 31/12", "Valore al 31/12 (EUR)", "Nota"});
            riga = 1;
            for (Calcoli_ISEE.Rapporto r : ris.rapporti) {
                if (r.contoCorrente() || !r.copreFineAnno) {
                    continue;
                }
                for (Calcoli_ISEE.Posizione p : r.posizioni) {
                    Riga(ws, riga++, new Object[] {r.gruppo, Calcoli_ISEE.aliasDi(r.gruppo),
                        r.tipo, p.moneta, p.qtaFine.stripTrailingZeros(), p.valoreFine == null ? "n.d." : Due(p.valoreFine),
                        p.negativa() ? "giacenza negativa, conta zero" : p.valoreFine == null ? "prezzo mancante" : ""});
                }
                if (r.conLiquidita && r.liquidita.signum() != 0) {
                    Riga(ws, riga++, new Object[] {r.gruppo, Calcoli_ISEE.aliasDi(r.gruppo),
                        r.tipo, "Liquidità in valuta", "", Due(r.liquidita), "saldo in valuta del gruppo, in euro"});
                }
            }
            ws.finish();

            ws = wb.newWorksheet("Avvisi " + anno);
            Riga(ws, 0, new Object[] {"Gruppo wallet", "Alias", "Tipo rapporto", "Avviso"});
            riga = 1;
            for (Calcoli_ISEE.Rapporto r : ris.rapporti) {
                for (String n : r.note) {
                    Riga(ws, riga++, new Object[] {r.gruppo, Calcoli_ISEE.aliasDi(r.gruppo),
                        r.tipo, n});
                }
            }
            ws.finish();

            if (ris.mediaCalcolata) {
                ws = wb.newWorksheet("Prezzi mancanti " + anno);
                Riga(ws, 0, new Object[] {"Moneta", "Rete", "Address", "Giorno",
                    "Quantità", "Gruppi wallet"});
                riga = 1;
                for (Calcoli_ISEE.PrezzoMancante pm : ris.prezziMancanti) {
                    //Testi e non numeri: un address o un simbolo fatto di cifre non deve diventare un numero
                    ws.value(riga, 0, pm.moneta.Moneta);
                    ws.value(riga, 1, pm.moneta.Rete == null ? "" : pm.moneta.Rete);
                    ws.value(riga, 2, pm.moneta.MonetaAddress == null ? "" : pm.moneta.MonetaAddress);
                    ws.value(riga, 3, Principale_ISEE.data(pm.giorno));
                    ws.value(riga, 4, pm.qta.stripTrailingZeros());
                    ws.value(riga, 5, String.join(", ", pm.gruppi));
                    riga++;
                }
                ws.finish();
            }
            wb.finish();
        }
    }

    /**
     * Una riga dell'Excel: i {@code BigDecimal} come numeri, tutto il resto come testo. Non come
     * {@code Principale_Dichiarazioni.ScriviRiga}, che trasforma in numero ogni testo fatto di cifre: un numero di
     * conto o la partita IVA di un operatore perderebbero gli zeri in testa.
     */
    private static void Riga(Worksheet ws, int riga, Object[] valori) {
        for (int c = 0; c < valori.length; c++) {
            if (valori[c] instanceof BigDecimal b) {
                ws.value(riga, c, b);
            } else {
                ws.value(riga, c, valori[c] == null ? "" : valori[c].toString());
            }
        }
    }

    private static BigDecimal Due(BigDecimal v) {
        return v.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
