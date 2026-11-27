package com.giacenzecrypto.giacenze_crypto;

import com.giacenzecrypto.giacenze_crypto.ImportazioneGenerica.ConfigurazioneImport;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Il token del contratto ({@code DERIV.<contratto>.LONG|SHORT}) dai file CSV degli exchange, con la stessa forma di quello di
 * {@link OKXDerivati}: <b>la regola dei derivati</b> (decisione dell'utente del 22/11/2026). Dove il file riporta per ogni esecuzione il
 * contratto, il verso, la quantita' e il prezzo, il contratto diventa un token. Il token da solo non ha effetti fiscali (prezzo zero, mai
 * cercato). Il LIFO ({@link DerivatiLifo}) sostituisce il profitto dell'exchange solo negli episodi verificati, cioe' quando con i prezzi
 * del file si riottiene il profitto che l'exchange dichiara, altrimenti resta il profitto dell'exchange.
 *
 * <p>Lo configura la chiave {@code contrattiDerivati} di una configurazione d'importazione ({@link Config}). Il passo gira <b>dopo</b> la
 * scrittura dei movimenti ({@link ImportazioneGenerica#importaRighe}), fuori dal raggruppamento e dal consolidamento delle righe dello
 * stesso secondo, che fonderebbero il token con le altre monete. Per ogni esecuzione:</p>
 * <ul>
 *   <li>crea la riga del token sul sotto-wallet {@value OKXDerivati#SOTTOWALLET}, se non c'e' gia' (stesso exchange e stesso {@code [24]}),
 *       con i dati del contratto in {@code [47]}: {@code pnl} e' il profitto lordo del file (quello che l'exchange calcola con il suo
 *       prezzo medio), {@code valoreContratto} 1 (quantita' nel sottostante per i lineari, in dollari per gli inversi);</li>
 *   <li>scrive lo stesso {@code [24]} sulla riga del profitto ({@link Derivati#PNL}) nata dalla stessa riga del file, gia' in archivio o
 *       appena scritta, <b>solo se e' vuoto</b> e solo con un abbinamento senza dubbi (stesso exchange, secondo, moneta e quantita'). E' il
 *       collegamento ({@link DerivatiLifo#ChiaveFill}) che fa azzerare al LIFO il profitto dell'exchange invece di contarlo due volte.
 *       Una chiusura senza riga del profitto collegata resta al profitto dell'exchange ({@link DerivatiLifo#AggiornaRisultati}).</li>
 * </ul>
 * <p>Il margine impegnato da ogni posizione non c'e' nei file (Bybit): nessuna riga di margine, e la colonna 7 del rigo codice 9 con il
 * margine cripto-attivita' e' zero (stampata come 1, decisione dell'utente). Le esecuzioni dello stesso secondo dello stesso contratto si
 * ordinano dall'andamento della posizione, perche' l'ordine del file non e' affidabile.</p>
 */
final class DerivatiDaCsv {

    private DerivatiDaCsv() {
    }

    /** Chiave del campo dati: commissione gia' compresa nella riga del profitto, in moneta di regolamento, positiva se pagata. */
    static final String CHIAVE_COMMISSIONE_INCLUSA = "commissioneInclusa";

    /** La chiave {@code contrattiDerivati} di una configurazione. */
    static final class Config {
        /** Causali del file (come le compone la configurazione) delle righe che sono esecuzioni di un contratto. */
        final Set<String> causali = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        int colonnaContratto = -1;
        /** Colonne il cui testo, unito da {@code |}, dice apertura o chiusura e verso (la chiave di {@link #versi}). */
        int[] colonneVerso = new int[0];
        /** Testo delle colonne del verso -> {@code apre long}, {@code chiude long}, {@code apre short}, {@code chiude short}. */
        final Map<String, String> versi = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        int colonnaQuantita = -1;
        int colonnaPosizione = -1;
        int colonnaPrezzo = -1;
        /** Profitto o perdita lordi della chiusura (prima della commissione). */
        int colonnaProfitto = -1;
        int colonnaCommissione = -1;
        /** Colonna da cui nasce la quantita' del movimento del profitto (per abbinarlo): la variazione netta o il profitto lordo. */
        int colonnaMovimento = -1;
        /** La riga del profitto e' al netto della commissione (Bybit, conto Derivati): il LIFO del token e' al netto della stessa commissione. */
        boolean commissioneInclusa = false;
        /** Identificativo dell'esecuzione; senza, uno sintetico stabile fra un import e l'altro dello stesso file. */
        int colonnaIdEsecuzione = -1;
        int colonnaOrdine = -1;

        static final Set<String> CHIAVI = Set.of("causali", "colonnaContratto", "colonneVerso", "versi", "colonnaQuantita",
                "colonnaPosizione", "colonnaPrezzo", "colonnaProfitto", "colonnaCommissione", "colonnaMovimento", "commissioneInclusa",
                "colonnaIdEsecuzione", "colonnaOrdine");

        static Config Leggi(JSONObject o) {
            Config c = new Config();
            JSONArray causali = o.optJSONArray("causali");
            if (causali != null) for (int i = 0; i < causali.length(); i++) c.causali.add(causali.getString(i).trim());
            c.colonnaContratto = o.optInt("colonnaContratto", -1);
            JSONArray verso = o.optJSONArray("colonneVerso");
            if (verso != null) {
                c.colonneVerso = new int[verso.length()];
                for (int i = 0; i < verso.length(); i++) c.colonneVerso[i] = verso.getInt(i);
            }
            JSONObject versi = o.optJSONObject("versi");
            if (versi != null) for (String k : versi.keySet()) c.versi.put(k.trim(), versi.getString(k).trim().toLowerCase());
            c.colonnaQuantita = o.optInt("colonnaQuantita", -1);
            c.colonnaPosizione = o.optInt("colonnaPosizione", -1);
            c.colonnaPrezzo = o.optInt("colonnaPrezzo", -1);
            c.colonnaProfitto = o.optInt("colonnaProfitto", -1);
            c.colonnaCommissione = o.optInt("colonnaCommissione", -1);
            c.colonnaMovimento = o.optInt("colonnaMovimento", -1);
            c.commissioneInclusa = o.optBoolean("commissioneInclusa", false);
            c.colonnaIdEsecuzione = o.optInt("colonnaIdEsecuzione", -1);
            c.colonnaOrdine = o.optInt("colonnaOrdine", -1);
            return c;
        }

        /** @return {@code true} se le colonne indispensabili ci sono */
        boolean Valida() {
            return !causali.isEmpty() && colonnaContratto >= 0 && colonneVerso.length > 0 && !versi.isEmpty() && colonnaQuantita >= 0
                    && colonnaPrezzo >= 0;
        }
    }

    /** Un'esecuzione letta dal file. */
    static final class Esecuzione {
        long istanteMs;
        String moneta;
        String simbolo;
        /** Contratto nella forma {@code BASE-QUOTA[-SCADENZA]-EXCHANGE}, {@code null} se il simbolo non si riconosce. */
        String contratto;
        boolean apre;
        boolean lungo;
        BigDecimal contratti;
        BigDecimal posizione;
        BigDecimal prezzo;
        BigDecimal profitto;
        BigDecimal commissione;
        /** Quantita' con segno del movimento del profitto nato dalla stessa riga, {@code null} se la colonna non c'e'. */
        BigDecimal qtaMovimento;
        String id;
        String ordine = "";
        int sequenza;
    }

    /**
     * Le esecuzioni delle righe del file (prima del raggruppamento: le righe dei contratti non si sommano per giorno). Una riga che non si
     * legge (verso sconosciuto, quantita' o prezzo mancanti, moneta vuota) non da' un token, e il suo profitto resta quello dell'exchange.
     */
    static List<Esecuzione> Leggi(List<String[]> righe, ConfigurazioneImport cfg) {
        List<Esecuzione> out = new ArrayList<>();
        Config c = cfg == null ? null : cfg.contrattiDerivati;
        if (c == null || !c.Valida() || righe == null) return out;
        String piattaforma = Piattaforma(cfg.nomeExchange);
        Map<String, Integer> occorrenze = new HashMap<>();
        for (String[] r : righe) {
            if (!c.causali.contains(cfg.getCausaleCSV(r).trim())) continue;
            StringBuilder chiaveVerso = new StringBuilder();
            for (int i = 0; i < c.colonneVerso.length; i++) {
                if (i > 0) chiaveVerso.append('|');
                chiaveVerso.append(Testo(r, c.colonneVerso[i]));
            }
            String verso = c.versi.get(chiaveVerso.toString());
            String moneta = Testo(r, cfg.colonnaMoneta);
            String simbolo = Testo(r, c.colonnaContratto).toUpperCase();
            BigDecimal qta = Numero(r, c.colonnaQuantita);
            BigDecimal prezzo = Numero(r, c.colonnaPrezzo);
            long istante = cfg.convertiDataInMillis(Testo(r, cfg.colonnaData));
            if (verso == null || !verso.matches("(apre|chiude) (long|short)") || moneta.isEmpty() || simbolo.isEmpty() || qta == null
                    || qta.signum() == 0 || prezzo == null || prezzo.signum() <= 0 || istante <= 0) {
                //Niente contenuto della riga nel log: ha ID di esecuzione e importi, e il log puo' finire in "Invia log"
                LoggerGC.logInfo("Derivati da CSV: riga di contratto non letta (" + r.length + " colonne, verso "
                        + (verso == null ? "non riconosciuto" : "riconosciuto") + ")");
                continue;
            }
            Esecuzione e = new Esecuzione();
            e.istanteMs = istante / 1000 * 1000;
            e.moneta = moneta;
            e.simbolo = simbolo;
            e.contratto = NormalizzaContratto(simbolo, moneta, piattaforma);
            e.apre = verso.startsWith("apre");
            e.lungo = verso.endsWith("long");
            e.contratti = qta.abs();
            BigDecimal pos = Numero(r, c.colonnaPosizione);
            e.posizione = pos == null ? null : pos.abs();
            e.prezzo = prezzo;
            BigDecimal profitto = Numero(r, c.colonnaProfitto);
            e.profitto = profitto == null ? BigDecimal.ZERO : profitto;
            BigDecimal commissione = Numero(r, c.colonnaCommissione);
            e.commissione = commissione == null ? BigDecimal.ZERO : commissione;
            e.qtaMovimento = Numero(r, c.colonnaMovimento);
            e.ordine = Testo(r, c.colonnaOrdine);
            String id = Testo(r, c.colonnaIdEsecuzione);
            if (id.isEmpty()) {
                //Sintetico e stabile: le stesse colonne danno lo stesso identificativo a ogni import. Due righe identiche si distinguono
                //per il numero di volta in cui compaiono nel file
                String base = Testo(r, cfg.colonnaData) + "|" + simbolo + "|" + chiaveVerso + "|" + qta.stripTrailingZeros().toPlainString()
                        + "|" + (pos == null ? "" : pos.stripTrailingZeros().toPlainString()) + "|" + prezzo.stripTrailingZeros().toPlainString()
                        + "|" + e.profitto.stripTrailingZeros().toPlainString();
                int n = occorrenze.merge(base, 1, Integer::sum);
                id = piattaforma + "-" + Impronta(base + (n > 1 ? "|" + n : ""));
            }
            e.id = id.replaceAll("[;,\\s|=_]", "");
            out.add(e);
        }
        OrdinaNelSecondo(out);
        return out;
    }

    /**
     * Ordine delle esecuzioni dello stesso secondo e dello stesso contratto: ognuna parte dalla posizione lasciata dalla precedente
     * (prima = posizione dopo meno i contratti per un'apertura, piu' per una chiusura). L'ordine trovato diventa il millisecondo dell'istante
     * ({@code ts} del campo dati), che {@link OKXDerivati#ORDINE_ESECUZIONE} legge. Senza la catena (posizione mancante) resta l'ordine
     * inverso del file, che e' dal piu' recente.
     */
    static void OrdinaNelSecondo(List<Esecuzione> esecuzioni) {
        Map<String, List<Esecuzione>> gruppi = new LinkedHashMap<>();
        for (Esecuzione e : esecuzioni) gruppi.computeIfAbsent(e.istanteMs + "|" + e.simbolo, k -> new ArrayList<>()).add(e);
        for (List<Esecuzione> g : gruppi.values()) {
            List<Esecuzione> ordinate = Catena(g);
            if (ordinate == null) {
                ordinate = new ArrayList<>(g);
                java.util.Collections.reverse(ordinate);
            }
            for (int i = 0; i < ordinate.size(); i++) ordinate.get(i).sequenza = i;
        }
    }

    private static List<Esecuzione> Catena(List<Esecuzione> g) {
        if (g.size() == 1) return g;
        for (Esecuzione e : g) if (e.posizione == null) return null;
        List<Esecuzione> restano = new ArrayList<>(g);
        List<Esecuzione> ordinate = new ArrayList<>();
        //Prima: quella la cui posizione di partenza non e' la posizione d'arrivo di nessun'altra
        Esecuzione corrente = null;
        for (Esecuzione e : restano) {
            BigDecimal prima = Prima(e);
            boolean seguita = false;
            for (Esecuzione f : restano) if (f != e && f.posizione.compareTo(prima) == 0) seguita = true;
            if (!seguita) {
                corrente = e;
                break;
            }
        }
        if (corrente == null) return null;
        while (corrente != null) {
            ordinate.add(corrente);
            restano.remove(corrente);
            Esecuzione prossima = null;
            for (Esecuzione f : restano) {
                if (Prima(f).compareTo(corrente.posizione) == 0) {
                    prossima = f;
                    break;
                }
            }
            corrente = prossima;
        }
        return restano.isEmpty() ? ordinate : null;
    }

    private static BigDecimal Prima(Esecuzione e) {
        return e.apre ? e.posizione.subtract(e.contratti) : e.posizione.add(e.contratti);
    }

    /** Esito di {@link #Applica}. */
    static final class Esito {
        int token;
        int collegati;
    }

    /**
     * Crea i token che mancano e collega le righe del profitto. Va chiamata dopo la scrittura dei movimenti del file.
     *
     * @return quanti token sono stati creati e quante righe del profitto collegate
     */
    static Esito Applica(List<Esecuzione> esecuzioni, ConfigurazioneImport cfg) {
        Esito esito = new Esito();
        if (esecuzioni == null || esecuzioni.isEmpty() || AttesaConnessione.Abortita()) return esito;
        Map<String, String[]> mappa = Principale.MappaCryptoWallet;
        String exchange = cfg.nomeExchange == null ? "" : cfg.nomeExchange.trim();
        Config c = cfg.contrattiDerivati;

        //Token gia' in archivio (stesso exchange e stesso identificativo) e righe del profitto per secondo e moneta
        Set<String> tokenPresenti = new HashSet<>();
        Map<String, List<String[]>> profitti = new HashMap<>();
        for (String[] v : mappa.values()) {
            if (v == null || v.length <= Derivati.CAMPO_DATI || v[3] == null || !v[3].trim().equalsIgnoreCase(exchange)) continue;
            if (DerivatiLifo.IsRigaToken(v)) {
                if (v[24] != null && !v[24].isBlank()) tokenPresenti.add(v[24].trim());
                continue;
            }
            if (!Derivati.Tipo(v).equals(Derivati.PNL) || v[0] == null || v[0].length() < 14) continue;
            String moneta = !Funzioni.noData(v[11]) ? v[11] : v[8];
            profitti.computeIfAbsent(v[0].substring(0, 14) + "|" + (moneta == null ? "" : moneta.trim().toUpperCase()),
                    k -> new ArrayList<>()).add(v);
        }

        //1 - Token
        List<String[]> nuovi = new ArrayList<>();
        Set<String> inLista = new HashSet<>();
        for (Esecuzione e : esecuzioni) {
            if (tokenPresenti.contains(e.id) || !inLista.add(e.id)) continue;
            String[] t = RigaToken(e, exchange, c);
            if (t != null) nuovi.add(t);
        }
        nuovi = FiltraPerSaldo(nuovi, mappa.values());
        if (!nuovi.isEmpty()) {
            int[] scritti = Importazioni.ScriviListaSuMappaCryptoToken(nuovi);
            esito.token = scritti[0];
            Importazioni.TransazioniAggiunte += scritti[0];
            Importazioni.Transazioni += scritti[0];
        }

        //2 - Collegamento con le righe del profitto: per secondo, moneta e quantita'. Lo stesso numero di esecuzioni e di righe vuote
        //con quella chiave si abbina in ordine (righe uguali: lo stesso valore), un numero diverso no (righe sommate o mancanti)
        if (c.colonnaMovimento >= 0) {
            Map<String, List<Esecuzione>> perChiave = new LinkedHashMap<>();
            for (Esecuzione e : esecuzioni) {
                if (e.qtaMovimento == null || e.qtaMovimento.signum() == 0) continue;
                perChiave.computeIfAbsent(PrefissoID(e.istanteMs) + "|" + e.moneta.toUpperCase() + "|"
                        + e.qtaMovimento.stripTrailingZeros().toPlainString(), k -> new ArrayList<>()).add(e);
            }
            for (Map.Entry<String, List<Esecuzione>> en : perChiave.entrySet()) {
                String[] parti = en.getKey().split("\\|", 3);
                BigDecimal qta = new BigDecimal(parti[2]);
                List<Esecuzione> daCollegare = new ArrayList<>();
                Set<String> idGruppo = new HashSet<>();
                for (Esecuzione e : en.getValue()) idGruppo.add(e.id);
                List<String[]> righeLibere = new ArrayList<>();
                Set<String> giaCollegati = new HashSet<>();
                for (String[] v : profitti.getOrDefault(parti[0] + "|" + parti[1], List.of())) {
                    BigDecimal q = QuantitaConSegno(v);
                    if (q == null || q.compareTo(qta) != 0) continue;
                    if (Funzioni.noData(v[24])) righeLibere.add(v);
                    else if (idGruppo.contains(v[24].trim())) giaCollegati.add(v[24].trim());
                }
                for (Esecuzione e : en.getValue()) if (!giaCollegati.contains(e.id)) daCollegare.add(e);
                if (daCollegare.isEmpty() || daCollegare.size() != righeLibere.size()) continue;
                righeLibere.sort((a, b) -> a[0].compareTo(b[0]));
                for (int i = 0; i < daCollegare.size(); i++) {
                    righeLibere.get(i)[24] = daCollegare.get(i).id;
                    esito.collegati++;
                }
            }
        }

        if (esito.token > 0 || esito.collegati > 0) {
            LoggerGC.logInfo("Derivati da CSV (" + exchange + "): " + esito.token + " token creati, " + esito.collegati
                    + " righe del profitto collegate al loro token");
            DerivatiLifo.AggiornaRisultati(mappa);
            Principale.TabellaCryptodaAggiornare = true;
        }
        return esito;
    }

    /** Chiave del campo dati: contratti chiusi secondo il file quando il token ne chiude meno (posizione aperta prima del file). */
    static final String CHIAVE_CONTRATTI_BORSA = "contrattiBorsa";

    /**
     * Toglie o riduce le chiusure che non hanno abbastanza contratti aperti fra i token, come {@link OKXDerivati#FiltraPerSaldoAperto}: una
     * posizione aperta prima del primo file importato (o righe mancanti) farebbe scendere il token sotto zero, con giacenze negative e
     * LiFo mancante fra gli errori. Una chiusura senza contratti aperti non diventa un token; una che ne chiude piu' di quelli aperti chiude
     * solo quelli, e i contratti del file restano nel campo dati ({@value #CHIAVE_CONTRATTI_BORSA}). In entrambi i casi l'episodio non
     * torna con i prezzi del file e il profitto resta quello dell'exchange. Le righe gia' in archivio non si toccano. Reimportare dopo aver
     * importato un file piu' vecchio, con le aperture, crea i token che mancavano.
     *
     * @param nuovi i token da scrivere
     * @param archivio i movimenti dell'archivio
     * @return i token da scrivere, con le chiusure ridotte
     */
    static List<String[]> FiltraPerSaldo(List<String[]> nuovi, Iterable<String[]> archivio) {
        Set<String[]> daScrivere = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        daScrivere.addAll(nuovi);
        List<String[]> tutte = new ArrayList<>(nuovi);
        for (String[] v : archivio) if (DerivatiLifo.IsRigaToken(v)) tutte.add(v);
        tutte.sort(OKXDerivati.ORDINE_ESECUZIONE);
        Map<String, BigDecimal> aperti = new HashMap<>();
        List<String[]> tenute = new ArrayList<>();
        for (String[] v : tutte) {
            boolean apre = !Funzioni.noData(v[11]);
            String simbolo = (apre ? v[11] : v[8]).trim().toUpperCase();
            BigDecimal q;
            try {
                q = new BigDecimal(apre ? v[13] : v[10]).abs();
            } catch (RuntimeException e) {
                continue;
            }
            boolean nuova = daScrivere.contains(v);
            if (apre) {
                aperti.merge(simbolo, q, BigDecimal::add);
                if (nuova) tenute.add(v);
                continue;
            }
            BigDecimal disponibili = aperti.getOrDefault(simbolo, BigDecimal.ZERO);
            if (nuova && disponibili.signum() <= 0) {
                LoggerGC.logInfo("Derivati da CSV: chiusura senza contratti aperti fra i token, nessun token (" + v[0] + ")");
                continue;
            }
            if (nuova && q.compareTo(disponibili) > 0) {
                Map<String, String> d = Derivati.Dati(v);
                d.put(CHIAVE_CONTRATTI_BORSA, q.stripTrailingZeros().toPlainString());
                d.put("contratti", disponibili.stripTrailingZeros().toPlainString());
                Derivati.ImpostaDati(v, d);
                v[10] = "-" + disponibili.stripTrailingZeros().toPlainString();
                LoggerGC.logInfo("Derivati da CSV: chiusura di " + q.toPlainString() + " contratti con " + disponibili.toPlainString()
                        + " aperti fra i token, ridotta (" + v[0] + ")");
                q = disponibili;
            }
            aperti.put(simbolo, disponibili.subtract(q));
            if (nuova) tenute.add(v);
        }
        return tenute;
    }

    /** La riga del token di un'esecuzione, nella forma di {@link OKXDerivati#CreaMovimenti}. */
    static String[] RigaToken(Esecuzione e, String exchange, Config c) {
        String contratto = e.contratto != null ? e.contratto : e.simbolo + "-" + Piattaforma(exchange);
        Map<String, String> dati = new LinkedHashMap<>();
        dati.put("piattaforma", exchange);
        dati.put("contratto", contratto);
        dati.put("simbolo", e.simbolo);
        dati.put("verso", e.lungo ? "long" : "short");
        dati.put("azione", e.apre ? "apre" : "chiude");
        dati.put("contratti", e.contratti.stripTrailingZeros().toPlainString());
        dati.put("prezzo", e.prezzo.stripTrailingZeros().toPlainString());
        //Senza un contratto riconosciuto il valore resta assente: la posizione e' incompleta e tiene il profitto dell'exchange
        if (e.contratto != null) dati.put("valoreContratto", "1");
        dati.put("moneta", e.moneta);
        if (e.posizione != null) dati.put("posizione", e.posizione.stripTrailingZeros().toPlainString());
        if (!e.apre) dati.put("pnl", e.profitto.stripTrailingZeros().toPlainString());
        dati.put("commissione", e.commissione.stripTrailingZeros().toPlainString());
        if (!e.apre && c.commissioneInclusa && e.commissione.signum() != 0) {
            dati.put(CHIAVE_COMMISSIONE_INCLUSA, e.commissione.stripTrailingZeros().toPlainString());
        }
        dati.put("ordine", e.ordine);
        dati.put("scambio", e.id);
        dati.put("ts", String.valueOf(e.istanteMs + e.sequenza));

        Moneta token = new Moneta();
        token.Moneta = Derivati.NomeTokenContratto(contratto, e.lungo);
        token.Qta = (e.apre ? e.contratti : e.contratti.negate()).toPlainString();
        token.AssegnaTipoAuto();
        String identificazione = exchange + e.id.replaceAll("[^A-Za-z0-9]", "");
        if (identificazione.length() > 40) identificazione = identificazione.substring(0, 40);
        String[] t = MovimentiCrypto.creaMovimento(token, null, exchange, OKXDerivati.SOTTOWALLET, e.istanteMs, "0.00",
                OKXDerivati.FONTE_PREZZO, 903, 1, null, null, "A", e.id, e.apre ? "DEPOSITO A COSTO 0" : "RETTIFICA GIACENZA",
                identificazione);
        if (t == null) return null;
        t[5] = e.apre ? OKXDerivati.CAMPO5_APERTURA : OKXDerivati.CAMPO5_CHIUSURA;
        t[18] = e.apre ? OKXDerivati.CAMPO18_APERTURA : OKXDerivati.CAMPO18_CHIUSURA;
        Derivati.Marca(t, Derivati.CONTRATTO);
        BigDecimal unitario = OKXDerivati.PrezzoUnitarioMoneta(e.moneta, e.istanteMs);
        if (unitario != null) dati.put(DerivatiLifo.CHIAVE_PREZZO_MONETA, unitario.stripTrailingZeros().toPlainString());
        t[Derivati.CAMPO_DATI] = Derivati.ScriviDati(dati);
        return t;
    }

    private static final Pattern INVERSO_FUTURES = Pattern.compile("^([A-Z0-9]+)USD([FGHJKMNQUVXZ]\\d{2})$");
    private static final Pattern LINEARE_SCADENZA = Pattern.compile("^([A-Z0-9]+)-(\\d{1,2}[A-Z]{3}\\d{2})$");

    /**
     * Il contratto nella forma {@code BASE-QUOTA[-SCADENZA]-EXCHANGE}: la base e la quota le leggono {@link DerivatiLifo#Inverso}
     * (inverso quando la moneta di regolamento e' la base) e il guadagno non realizzato del quadro W/RW (prezzo della base nella quota),
     * l'exchange in coda tiene distinti i token di exchange diversi sullo stesso contratto (la posizione si riconosce dal simbolo del
     * token). Riconosce i perpetui lineari ({@code BTCUSDT}, {@code BTCUSDC}, {@code BTCPERP} in USDC), gli inversi ({@code BTCUSD}
     * regolato in BTC), i futures inversi trimestrali ({@code BTCUSDZ22}) e i futures USDC a scadenza ({@code BTC-27DEC24}).
     *
     * @return il contratto, {@code null} se il simbolo non si riconosce o non torna con la moneta di regolamento
     */
    static String NormalizzaContratto(String simbolo, String moneta, String piattaforma) {
        if (simbolo == null || moneta == null) return null;
        String s = simbolo.trim().toUpperCase();
        String m = moneta.trim().toUpperCase();
        String coda = "-" + piattaforma;
        Matcher inv = INVERSO_FUTURES.matcher(s);
        if (inv.matches() && inv.group(1).equals(m)) return inv.group(1) + "-USD-" + inv.group(2) + coda;
        Matcher lin = LINEARE_SCADENZA.matcher(s);
        if (lin.matches() && m.equals("USDC")) return lin.group(1) + "-USDC-" + lin.group(2) + coda;
        for (String quota : new String[]{"USDT", "USDC"}) {
            if (s.endsWith(quota) && s.length() > quota.length() && m.equals(quota)) {
                return s.substring(0, s.length() - quota.length()) + "-" + quota + coda;
            }
        }
        if (s.endsWith("PERP") && s.length() > 4 && m.equals("USDC")) return s.substring(0, s.length() - 4) + "-USDC" + coda;
        if (s.endsWith("USD") && s.length() > 3 && s.substring(0, s.length() - 3).equals(m)) return m + "-USD" + coda;
        return null;
    }

    /** Nome dell'exchange come parte del contratto: maiuscolo, solo lettere e cifre. */
    static String Piattaforma(String exchange) {
        String p = exchange == null ? "" : exchange.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return p.isEmpty() ? "CSV" : p;
    }

    /** I primi 14 caratteri dell'ID di un movimento con quell'istante (come li scrive {@link MovimentiCrypto#creaMovimento}). */
    static String PrefissoID(long istanteMs) {
        return FunzioniDate.ConvertiDatadaLongAlSecondo(istanteMs).replaceAll(" |-|:", "");
    }

    private static BigDecimal QuantitaConSegno(String[] v) {
        try {
            if (!Funzioni.noData(v[13]) && Funzioni.isNumeric(v[13], false)) return new BigDecimal(v[13]);
            if (!Funzioni.noData(v[10]) && Funzioni.isNumeric(v[10], false)) return new BigDecimal(v[10]);
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    private static String Testo(String[] r, int colonna) {
        if (r == null || colonna < 0 || colonna >= r.length || r[colonna] == null) return "";
        return r[colonna].replace("\"", "").trim();
    }

    private static BigDecimal Numero(String[] r, int colonna) {
        String s = Testo(r, colonna);
        if (s.isEmpty() || !Funzioni.isNumeric(s, false)) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String Impronta(String testo) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(testo.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", h[i]));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return Integer.toHexString(testo.hashCode());
        }
    }
}
