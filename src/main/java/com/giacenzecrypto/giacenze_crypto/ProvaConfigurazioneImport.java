package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.json.JSONObject;

/**
 * Prova di una configurazione di importazione JSON su un CSV, senza scrivere nulla: gli stessi passi di
 * {@link ImportazioneGenerica#importa} (lettura, regole di riga, annullamenti, somme per giorno, raggruppamento,
 * costruzione dei movimenti) fino a prima di {@code Importazioni.ScriviListaSuMappaCrypto}. Serve a chi scrive una
 * configurazione, da solo o con un'IA, per vedere cosa produrrebbe prima di toccare l'archivio.
 *
 * <p>Tre differenze volute dall'importazione vera:
 * <ul>
 * <li>nessun prezzo: tutto gira dentro {@link Prezzi#SenzaValorizzare}, quindi niente rete e niente attese. Restano i
 *     controvalori scritti nel CSV;</li>
 * <li>niente di quello che lavora sull'archivio dopo la scrittura: abbinamento degli SCAMBIO DIFFERITO, giroconti
 *     FIAT, deduplica contro i movimenti gia' presenti. L'esito lo dichiara;</li>
 * <li>nome dell'exchange e fuso non si chiedono: se la configurazione non li fissa si usano un nome di prova e il
 *     fuso del nome del file o UTC, e l'esito lo dice.</li>
 * </ul>
 * Nessun campo Swing: la finestra e' {@link GUI_ProvaConfigurazione}.
 */
public class ProvaConfigurazioneImport {

    /** Nome dell'exchange usato quando la configurazione lascia che sia l'utente a sceglierlo */
    static final String EXCHANGE_DI_PROVA = "Prova";

    /** Un movimento prodotto dalla prova, con le causali del CSV del gruppo di righe da cui nasce */
    public record Movimento(String[] v, String causali) {
        /** @return la categoria del movimento, l'ultimo segmento dell'ID (SC, DC, PC, RW, CM...) */
        public String categoria() {
            return v[0].substring(v[0].lastIndexOf('_') + 1);
        }
    }

    /** Quante volte una combinazione di causali del CSV compare come gruppo di righe, e cosa ne esce */
    public record Riepilogo(String causali, String tipi, int gruppi, Map<String, Integer> categorie) {
        /** @return le categorie prodotte nella forma "SC x3, CM x3" */
        public String testoCategorie() {
            if (categorie.isEmpty()) return "nessun movimento";
            List<String> pezzi = new ArrayList<>();
            categorie.forEach((k, n) -> pezzi.add(k + " x" + n));
            return String.join(", ", pezzi);
        }
    }

    /** Entrate e uscite di una moneta su un wallet, sommate su tutti i movimenti della prova */
    public record Saldo(String wallet, String moneta, BigDecimal entrate, BigDecimal uscite) {
        /** @return entrate meno uscite */
        public BigDecimal saldo() {
            return entrate.subtract(uscite);
        }
    }

    /** Tutto quello che la prova ha trovato */
    public static final class Esito {
        public File fileConfigurazione;
        public File fileCSV;
        /** SHA-256 della configurazione cosi' come e' stata provata: chi la condivide controlla che non sia cambiata */
        public String impronta = "";
        /** Errore che ha impedito la prova (configurazione illeggibile, CSV mancante), altrimenti {@code null} */
        public String errore;
        public String nomeExchange = "";
        public String fuso = "";
        /** Le scelte fatte al posto dell'utente o i limiti che valgono per questa prova */
        public final List<String> note = new ArrayList<>();
        public int righeLette;
        public final List<Movimento> movimenti = new ArrayList<>();
        /** Righe scartate, nella forma "MOTIVO | riga del CSV" */
        public final List<String> scarti = new ArrayList<>();
        public final List<String> chiaviSconosciute = new ArrayList<>();
        public final List<String> tipiSconosciuti = new ArrayList<>();
        public final List<Riepilogo> riepilogo = new ArrayList<>();
        public final List<Saldo> saldi = new ArrayList<>();

        /** @return vero se c'e' qualcosa da correggere nella configurazione */
        public boolean ciSonoProblemi() {
            return errore != null || !scarti.isEmpty() || !chiaviSconosciute.isEmpty() || !tipiSconosciuti.isEmpty();
        }

        /**
         * Il resoconto in testo semplice, da incollare in un chatbot insieme alla configurazione: l'IA vede cosa e'
         * uscito e cosa correggere. Le righe scartate sono quelle del CSV cosi' come sono, la finestra lo ricorda.
         */
        public String TestoPerIA() {
            StringBuilder sb = new StringBuilder();
            sb.append("Resoconto della prova di una configurazione di importazione di Giacenze Crypto ")
                    .append(VarStatiche.Versione).append("\n");
            sb.append("Configurazione: ").append(fileConfigurazione == null ? "" : fileConfigurazione.getName()).append("\n");
            sb.append("CSV: ").append(fileCSV == null ? "" : fileCSV.getName()).append("\n");
            if (errore != null) {
                sb.append("\nERRORE, la prova non e' stata eseguita:\n").append(errore).append("\n");
                return sb.toString();
            }
            sb.append("Exchange: ").append(nomeExchange).append(", fuso: ").append(fuso).append("\n");
            sb.append("Righe del CSV lette: ").append(righeLette).append(", movimenti prodotti: ").append(movimenti.size())
                    .append(", righe scartate: ").append(scarti.size()).append("\n");
            for (String n : note) sb.append("Nota: ").append(n).append("\n");
            if (!chiaviSconosciute.isEmpty()) {
                sb.append("\nOPZIONI NON RICONOSCIUTE (ignorate dal programma): ")
                        .append(String.join(", ", chiaviSconosciute)).append("\n");
            }
            if (!tipiSconosciuti.isEmpty()) {
                sb.append("\nVALORI CHE NON SONO TIPI INTERNI (le righe diventano movimenti generici):\n");
                for (String t : tipiSconosciuti) sb.append("- ").append(t).append("\n");
            }
            if (!scarti.isEmpty()) {
                sb.append("\nRIGHE SCARTATE (motivo | riga):\n");
                ElencoLimitato(sb, scarti, 100);
            }
            sb.append("\nRIEPILOGO PER CAUSALE (causali del gruppo di righe | tipi interni | gruppi | movimenti prodotti, "
                    + "categorie: SC scambio, AC acquisto, VC vendita, DC deposito crypto, PC prelievo crypto, "
                    + "DF deposito FIAT, PF prelievo FIAT, RW provento, CM commissione, TI trasferimento interno):\n");
            for (Riepilogo r : riepilogo) {
                sb.append("- ").append(r.causali()).append(" | ").append(r.tipi()).append(" | ").append(r.gruppi())
                        .append(" | ").append(r.testoCategorie()).append("\n");
            }
            sb.append("\nSALDO FINALE PER MONETA (wallet | moneta | entrate | uscite | saldo):\n");
            for (Saldo s : saldi) {
                sb.append("- ").append(s.wallet()).append(" | ").append(s.moneta()).append(" | ")
                        .append(s.entrate().toPlainString()).append(" | ").append(s.uscite().toPlainString()).append(" | ")
                        .append(s.saldo().toPlainString()).append("\n");
            }
            sb.append("\nMOVIMENTI (data | categoria | tipo | uscita | entrata | wallet | causali del CSV):\n");
            List<String> righe = new ArrayList<>();
            for (Movimento m : movimenti) righe.add(TestoMovimento(m));
            ElencoLimitato(sb, righe, 60);
            return sb.toString();
        }
    }

    private static void ElencoLimitato(StringBuilder sb, List<String> righe, int massimo) {
        for (int i = 0; i < Math.min(righe.size(), massimo); i++) sb.append("- ").append(righe.get(i)).append("\n");
        if (righe.size() > massimo) sb.append("... e altre ").append(righe.size() - massimo).append("\n");
    }

    /** @return il movimento in una riga di testo */
    static String TestoMovimento(Movimento m) {
        String[] v = m.v();
        return v[1] + " | " + m.categoria() + " | " + v[5] + " | " + Gamba(v[10], v[8]) + " | " + Gamba(v[13], v[11])
                + " | " + v[4] + " | " + m.causali();
    }

    /** @return quantita' e moneta di una gamba, o stringa vuota se la gamba manca */
    static String Gamba(String qta, String moneta) {
        if (moneta == null || moneta.isBlank()) return "";
        return (qta == null ? "" : qta) + " " + moneta;
    }

    /**
     * Il fuso con cui l'importazione vera leggerebbe il file (stessa regola di {@code Importazioni_Gestione}): con il
     * {@code ?} finale vince quello del nome del file, poi quello della configurazione, poi quello del nome del file.
     * @return il fuso, oppure {@code null} se l'importazione vera lo chiederebbe all'utente
     */
    static String FusoPerFile(String fusoConfigurazione, String nomeFile) {
        String fuso = fusoConfigurazione == null ? "" : fusoConfigurazione.trim();
        boolean prioritaNomeFile = fuso.contains("?");
        fuso = fuso.replace("?", "");
        String fusoFile = ImportazioneGenerica.estraiTZdaNomeFile(nomeFile);
        boolean fusoNelNome = fusoFile != null && !fusoFile.isBlank();
        if (prioritaNomeFile && fusoNelNome) return fusoFile;
        if (!fuso.isBlank()) return fuso;
        if (fusoNelNome) return fusoFile;
        return null;
    }

    /**
     * Esegue la prova. Non scrive nell'archivio e lascia azzerati i contatori di {@link Importazioni}, che usa come
     * l'importazione vera: va lanciata quando non e' in corso un'importazione.
     * @param json la configurazione da provare
     * @param csv il file da leggere
     * @return l'esito, mai {@code null}
     */
    public static Esito Esegui(File json, File csv) {
        Esito e = new Esito();
        e.fileConfigurazione = json;
        e.fileCSV = csv;
        //Prima la sintassi, con i numeri di riga: carica() legge il file in una riga sola e li perde
        try {
            byte[] dati = Files.readAllBytes(json.toPath());
            e.impronta = CommunityConfigurazioni.Sha256(dati);
            new JSONObject(new String(dati, StandardCharsets.UTF_8));
        } catch (Exception ex) {
            e.errore = "Il file JSON non e' scritto correttamente: " + ex.getMessage();
            return e;
        }
        ConfigurazioneImport cfg;
        try {
            cfg = ConfigurazioneImport.carica(json.getAbsolutePath());
        } catch (Exception ex) {
            e.errore = "La configurazione non si puo' usare: " + ex.getMessage();
            return e;
        }
        if (csv == null || !csv.isFile()) {
            e.errore = "File CSV non trovato: " + csv;
            return e;
        }
        e.chiaviSconosciute.addAll(cfg.chiaviSconosciute);
        e.tipiSconosciuti.addAll(cfg.tipiSconosciuti);

        if (cfg.nomeExchange == null || cfg.nomeExchange.isBlank()) {
            cfg.nomeExchange = EXCHANGE_DI_PROVA;
            e.note.add("la configurazione non fissa nomeExchange: l'importazione lo chiedera', la prova usa \""
                    + EXCHANGE_DI_PROVA + "\"");
        }
        e.nomeExchange = cfg.nomeExchange;
        String fuso = FusoPerFile(cfg.fuso, csv.getName());
        if (fuso == null) {
            fuso = "UTC";
            e.note.add("il fuso non e' ne' nella configurazione ne' nel nome del file: l'importazione lo chiedera', la prova usa UTC");
        }
        cfg.fuso = fuso;

        Importazioni.AzzeraContatori();
        try {
            List<Movimento> movimenti = Prezzi.SenzaValorizzare(() -> CostruisciMovimenti(cfg, csv, e));
            e.fuso = cfg.fuso;   //fusoDaIntestazione puo' averlo cambiato leggendo il file
            if (movimenti == null) return e;
            movimenti.sort(Comparator.comparing((Movimento m) -> m.v()[0], String.CASE_INSENSITIVE_ORDER));
            e.movimenti.addAll(movimenti);
            for (String r : Importazioni.movimentiSconosciuti.split("\n")) {
                if (!r.isBlank()) e.scarti.add(r);
            }
            if (!Importazioni.CausaliDerivatiSegnalate.isEmpty()) {
                e.note.add("causali di derivati (il programma li tratta come cripto-attivita' e lo segnala): "
                        + String.join(", ", Importazioni.CausaliDerivatiSegnalate));
            }
        } finally {
            Importazioni.AzzeraContatori();
        }
        CalcolaSaldi(e);
        if (e.riepilogo.stream().anyMatch(r -> r.tipi().toUpperCase().contains("SCAMBIO DIFFERITO"))) {
            e.note.add("ci sono righe SCAMBIO DIFFERITO: l'importazione vera prova ad abbinarle in scambi dopo averle"
                    + " scritte, la prova le mostra separate");
        }
        if (cfg.girocontoFiat != null) {
            e.note.add("girocontoFiat: l'abbinamento dei giroconti avviene sull'archivio dopo l'importazione, la prova non lo mostra");
        }
        e.note.add("prezzi non cercati: i controvalori sono solo quelli scritti nel CSV");
        e.note.add("i movimenti gia' presenti in archivio non sono stati controllati: l'importazione vera non li duplica");
        return e;
    }

    /** La parte che deve girare a valorizzazione spenta: dalla lettura del CSV ai movimenti */
    private static List<Movimento> CostruisciMovimenti(ConfigurazioneImport cfg, File csv, Esito e) {
        List<String[]> righe;
        try {
            righe = ImportazioneGenerica.leggiCSV(csv.getAbsolutePath(), cfg);
        } catch (Exception ex) {
            e.errore = "Il CSV non si puo' leggere: " + ex.getMessage();
            return null;
        }
        e.righeLette = righe.size();
        righe = ImportazioneGenerica.scartaAnnullamenti(righe, cfg);
        righe = ImportazioneGenerica.consolidaCausaliPerGiorno(righe, cfg);

        List<Movimento> risultato = new ArrayList<>();
        Map<String, Integer> gruppiPerChiave = new LinkedHashMap<>();
        Map<String, String> tipiPerChiave = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> categoriePerChiave = new LinkedHashMap<>();
        for (List<String[]> gruppo : ImportazioneGenerica.raggruppaRighe(righe, cfg)) {
            TreeSet<String> causali = new TreeSet<>();
            TreeSet<String> tipi = new TreeSet<>();
            for (String[] riga : gruppo) {
                causali.add(cfg.getCausaleCSV(riga));
                String tipo = cfg.tipoMovimentoNelGruppo(riga, gruppo);
                tipi.add(tipo == null || tipo.isBlank() ? "(nessuno)" : tipo);
            }
            String chiave = String.join(" + ", causali);
            List<String[]> movs;
            try {
                movs = ImportazioneGenerica.consolidaGruppo(gruppo, cfg, new ArrayList<>());
            } catch (Exception ex) {
                LoggerGC.ScriviErrore(ex);
                Importazioni.movimentiSconosciuti += "ERRORE " + ex + " | " + chiave + "\n";
                continue;
            }
            gruppiPerChiave.merge(chiave, 1, Integer::sum);
            tipiPerChiave.putIfAbsent(chiave, String.join(", ", tipi));
            Map<String, Integer> categorie = categoriePerChiave.computeIfAbsent(chiave, k -> new TreeMap<>());
            for (String[] v : movs) {
                Movimento m = new Movimento(v, chiave);
                risultato.add(m);
                categorie.merge(m.categoria(), 1, Integer::sum);
            }
        }
        for (String chiave : gruppiPerChiave.keySet()) {
            e.riepilogo.add(new Riepilogo(chiave, tipiPerChiave.get(chiave), gruppiPerChiave.get(chiave),
                    categoriePerChiave.get(chiave)));
        }
        e.riepilogo.sort(Comparator.comparing(Riepilogo::gruppi).reversed().thenComparing(Riepilogo::causali));
        return risultato;
    }

    /** Somma entrate e uscite di ogni moneta per wallet */
    private static void CalcolaSaldi(Esito e) {
        Map<String, BigDecimal[]> somme = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Movimento m : e.movimenti) {
            String[] v = m.v();
            Somma(somme, v[4], v[8], v[10], 1);
            Somma(somme, v[4], v[11], v[13], 0);
        }
        somme.forEach((k, s) -> {
            String[] p = k.split("\u0000", 2);
            e.saldi.add(new Saldo(p[0], p[1], s[0], s[1]));
        });
    }

    private static void Somma(Map<String, BigDecimal[]> somme, String wallet, String moneta, String qta, int indice) {
        if (moneta == null || moneta.isBlank() || qta == null || qta.isBlank()) return;
        BigDecimal q;
        try {
            q = new BigDecimal(qta.trim()).abs();
        } catch (NumberFormatException ex) {
            return;
        }
        BigDecimal[] s = somme.computeIfAbsent((wallet == null ? "" : wallet) + "\u0000" + moneta,
                k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        s[indice] = s[indice].add(q);
    }
}
