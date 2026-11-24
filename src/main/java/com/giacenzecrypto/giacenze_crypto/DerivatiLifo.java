package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Il differenziale di ogni chiusura di una posizione su un contratto, calcolato <b>in LIFO</b> sui lotti aperti
 * (art. 67 c.1-bis TUIR: chiudendo rapporti c-quater si considerano chiusi per primi quelli acquisiti più di recente),
 * e confrontato con il profitto che l'exchange ha riconosciuto sulla stessa chiusura (che usa il prezzo medio della
 * posizione). Tappa B di {@code nocommit/Documentazione/Decisioni_Derivati.md} D3, opzione C.
 *
 * <p><b>Il LIFO è il metodo fiscale</b> (decisione dell'utente, 2026-11-16): il reddito c-quater di una chiusura è il suo
 * differenziale in LIFO, in euro, scritto in {@code [46]} della riga di chiusura del token da {@link #AggiornaRisultati}
 * (passo finale di {@code AggiornaPlusvalenze}). Il profitto dell'exchange resta il <i>movimento di denaro</i>
 * ({@code DDV}/{@code PDV}, lato cripto con il doppio effetto D10) ma, per un fill che ha il suo token chiuso, il suo
 * {@code [46]} è zero: il reddito dei derivati non si conta due volte. Il confronto con l'exchange resta, per dire dove
 * i due metodi divergono (posizione chiusa in più riprese a cavallo d'anno). Senza prezzi, valore del contratto o prezzo
 * della moneta, o se i prezzi non spiegano il profitto dell'exchange (verifica con il suo prezzo medio), il fill ripiega sul profitto dell'exchange, com'era.
 *
 * <p><b>Dati.</b> Legge le sole righe {@link Derivati#CONTRATTO} (token del contratto, {@link OKXDerivati}) e i dati del
 * contratto in {@code [47]}: {@code contratti}, {@code prezzo}, {@code valoreContratto} (ctVal), {@code verso},
 * {@code moneta}, {@code pnl}. Senza prezzo o senza valore del contratto la posizione è segnata
 * {@link Posizione#datiIncompleti} e non si calcola nulla.
 *
 * <p><b>Formule</b> (per ogni pezzo di lotto consumato, in moneta di regolamento):
 * <ul>
 *   <li>contratto <b>lineare</b> (regolato nella moneta di quotazione, USDT/USDC): long {@code n × ctVal × (pc − pa)},
 *       short {@code n × ctVal × (pa − pc)};</li>
 *   <li>contratto <b>inverso</b> (regolato nel sottostante, es. BTC-USD-SWAP, {@code ctVal} in dollari per contratto):
 *       long {@code n × ctVal × (1/pa − 1/pc)}, short {@code n × ctVal × (1/pc − 1/pa)}.</li>
 * </ul>
 * con {@code pa} prezzo del lotto aperto e {@code pc} prezzo della chiusura. Verificate sui bill reali di OKX: il pnl di
 * una chiusura a lotto singolo coincide con la formula lineare.
 */
public final class DerivatiLifo {

    private DerivatiLifo() {
    }

    private static final MathContext MC = new MathContext(30, RoundingMode.HALF_UP);

    /** Scarto massimo, in rapporto al profitto, fra quello dell'exchange e quello ricostruito con il prezzo medio. */
    static final BigDecimal TOLLERANZA_RELATIVA = new BigDecimal("0.005");
    /** Scarto assoluto minimo, in moneta di regolamento (i profitti arrivano arrotondati a pochi decimali). */
    static final BigDecimal TOLLERANZA_ASSOLUTA = new BigDecimal("0.000001");

    /** Un lotto aperto: quanti contratti restano, a quale prezzo, aperto quando. */
    public static final class Lotto {
        public final String idApertura;
        public final String data;
        public final BigDecimal prezzo;
        public BigDecimal contratti;

        Lotto(String idApertura, String data, BigDecimal prezzo, BigDecimal contratti) {
            this.idApertura = idApertura;
            this.data = data;
            this.prezzo = prezzo;
            this.contratti = contratti;
        }
    }

    /** Un pezzo di lotto consumato da una chiusura. */
    public static final class Pezzo {
        public final String dataApertura;
        public final BigDecimal prezzoApertura;
        public final BigDecimal contratti;
        public final BigDecimal differenziale;

        Pezzo(String dataApertura, BigDecimal prezzoApertura, BigDecimal contratti, BigDecimal differenziale) {
            this.dataApertura = dataApertura;
            this.prezzoApertura = prezzoApertura;
            this.contratti = contratti;
            this.differenziale = differenziale;
        }
    }

    /** Una chiusura, anche parziale, con il suo differenziale in LIFO e il profitto dell'exchange. */
    public static final class Chiusura {
        public final String id;
        public final String data;
        public final BigDecimal contratti;
        public final BigDecimal prezzo;
        /** Posizione (episodio) a cui appartiene la chiusura, vedi {@link Posizione#episodioVerificato}. */
        public int episodio;
        /** Differenziale in LIFO, in moneta di regolamento; {@code null} se i dati non bastano. */
        public BigDecimal differenzialeLifo;
        /** Profitto riconosciuto dall'exchange su questa chiusura, {@code null} se il dato manca. */
        public final BigDecimal pnlBorsa;
        public final List<Pezzo> pezzi = new ArrayList<>();
        /** Contratti chiusi che nessun lotto aperto copriva (la posizione era aperta con la forma di prima). */
        public BigDecimal scoperti = BigDecimal.ZERO;
        /**
         * Il profitto dell'exchange si ricostruisce, a meno di arrotondamenti, applicando il suo metodo (prezzo medio della posizione)
         * ai prezzi di esecuzione: {@code false} se non coincide o manca. Vedi {@link #TOLLERANZA_RELATIVA}.
         */
        public boolean verificata = false;
        /** Profitto atteso con il prezzo medio, {@code null} se non calcolabile. */
        public BigDecimal pnlConPrezzoMedio;

        Chiusura(String id, String data, BigDecimal contratti, BigDecimal prezzo, BigDecimal pnlBorsa) {
            this.id = id;
            this.data = data;
            this.contratti = contratti;
            this.prezzo = prezzo;
            this.pnlBorsa = pnlBorsa;
        }

        /** @return LIFO meno exchange, {@code null} se uno dei due manca */
        public BigDecimal differenza() {
            return differenzialeLifo == null || pnlBorsa == null ? null : differenzialeLifo.subtract(pnlBorsa);
        }

        public String anno() {
            return data.length() >= 4 ? data.substring(0, 4) : "";
        }
    }

    /**
     * Tutte le posizioni su un contratto in un verso (un token). Il token resta lo stesso negli anni, quindi le sue righe
     * contengono più <b>episodi</b>: ciascuno va da quando i contratti aperti passano da zero a più di zero a quando tornano
     * a zero. La verifica dei prezzi vale per episodio ({@link #episodioVerificato}).
     */
    public static final class Posizione {
        public final String token;
        public final String contratto;
        public final boolean lungo;
        public final String moneta;
        public final boolean inverso;
        public final List<Chiusura> chiusure = new ArrayList<>();
        public final Deque<Lotto> aperti = new ArrayDeque<>();
        /** Mancano prezzi o valore del contratto in almeno un episodio (vedi {@link #episodiIncompleti}). */
        public boolean datiIncompleti = false;
        /** Episodi in cui mancano prezzi o valore del contratto: nessun differenziale calcolato per le loro chiusure. */
        public final java.util.Set<Integer> episodiIncompleti = new java.util.HashSet<>();
        /** Primo anno (da {@code v[1]}) in cui a un episodio mancano prezzi o valore del contratto, vedi {@link #verificataFinoAllAnno}. */
        final Map<Integer, String> primoAnnoIncompleto = new java.util.HashMap<>();
        /** Indice dell'episodio in corso, -1 prima della prima operazione. */
        int episodioCorrente = -1;
        /**
         * Le righe di token di ogni episodio, in ordine di esecuzione (ID). Le legge il giro di un movimento
         * ({@code Principale_GiroMovimento}): e' l'unica copia della regola degli episodi, non va ricostruita altrove.
         */
        public final Map<Integer, List<String>> righeEpisodio = new LinkedHashMap<>();

        /**
         * {@code true} se tornano tutte le chiusure dell'episodio di {@code c} fino al 31/12 del suo anno, e fino ad allora non
         * mancano prezzi né valore del contratto. E' la condizione per il reddito in LIFO di {@code c}. Diversa da
         * {@link #episodioVerificato}, che guarda l'episodio intero: un episodio aperto al 31/12 riceve le chiusure dell'anno dopo,
         * e una che non torna non deve far tornare al profitto dell'exchange le chiusure di un anno già dichiarato (D18.9). Il primo
         * anno che non torna riceve il conguaglio ({@link #CHIAVE_CONGUAGLIO}).
         */
        public boolean verificataFinoAllAnno(Chiusura c) {
            String anno = c.anno();
            String incompleto = primoAnnoIncompleto.get(c.episodio);
            if (incompleto != null && incompleto.compareTo(anno) <= 0) return false;
            boolean almenoUna = false;
            for (Chiusura d : chiusure) {
                if (d.episodio != c.episodio || d.anno().compareTo(anno) > 0) continue;
                if (!d.verificata) return false;
                almenoUna = true;
            }
            return almenoUna;
        }

        /**
         * {@code true} se l'episodio ha dati completi, almeno una chiusura e <b>ogni</b> sua chiusura verificata
         * ({@link Chiusura#verificata}): solo allora i prezzi di esecuzione sono affidabili e il reddito di quelle chiusure si
         * calcola in LIFO. Altrimenti resta il profitto dell'exchange. E' il controllo che protegge dai dati incoerenti, per esempio
         * prezzi in unita' diverse nello stesso export. Per episodio e non per token: una chiusura che non torna in una posizione
         * successiva non deve cambiare il reddito di quelle già chiuse (anni già dichiarati).
         */
        public boolean episodioVerificato(int episodio) {
            if (episodiIncompleti.contains(episodio)) return false;
            boolean almenoUna = false;
            for (Chiusura c : chiusure) {
                if (c.episodio != episodio) continue;
                if (!c.verificata) return false;
                almenoUna = true;
            }
            return almenoUna;
        }

        /** {@code true} se ogni episodio con chiusure è verificato ({@link #episodioVerificato}). */
        public boolean verificata() {
            if (chiusure.isEmpty()) return false;
            for (Chiusura c : chiusure) if (!episodioVerificato(c.episodio)) return false;
            return true;
        }

        /** {@code true} se almeno un episodio con chiusure è verificato e almeno uno no. */
        public boolean verificataInParte() {
            boolean si = false, no = false;
            for (Chiusura c : chiusure) {
                if (episodioVerificato(c.episodio)) si = true;
                else no = true;
            }
            return si && no;
        }

        Posizione(String token, String contratto, boolean lungo, String moneta, boolean inverso) {
            this.token = token;
            this.contratto = contratto;
            this.lungo = lungo;
            this.moneta = moneta;
            this.inverso = inverso;
        }

        public BigDecimal contrattiAperti() {
            BigDecimal t = BigDecimal.ZERO;
            for (Lotto l : aperti) t = t.add(l.contratti);
            return t;
        }

        /** Somma per anno di chiusura di (LIFO, exchange, differenza); le chiusure senza dato non entrano. */
        public Map<String, BigDecimal[]> perAnno() {
            Map<String, BigDecimal[]> m = new TreeMap<>();
            for (Chiusura c : chiusure) {
                if (c.differenzialeLifo == null || c.pnlBorsa == null) continue;
                BigDecimal[] t = m.computeIfAbsent(c.anno(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
                t[0] = t[0].add(c.differenzialeLifo);
                t[1] = t[1].add(c.pnlBorsa);
                t[2] = t[2].add(c.differenzialeLifo.subtract(c.pnlBorsa));
            }
            return m;
        }

        /** @return {@code true} se in qualche anno LIFO ed exchange danno un totale diverso (oltre l'arrotondamento a 8 decimali) */
        public boolean DivergePerAnno() {
            BigDecimal soglia = new BigDecimal("0.00000001");
            for (BigDecimal[] t : perAnno().values()) if (t[2].abs().compareTo(soglia) > 0) return true;
            return false;
        }
    }

    /** @return {@code true} se il movimento è la riga di token di un contratto, con i dati per calcolare */
    static boolean IsRigaToken(String[] v) {
        return v != null && v.length > Derivati.CAMPO_DATI && Derivati.Tipo(v).equals(Derivati.CONTRATTO);
    }

    /**
     * Chiave della posizione di un movimento di contratto: il simbolo del token, che porta contratto e verso.
     *
     * @return il simbolo, {@code null} se il movimento non è una riga di token
     */
    public static String ChiavePosizione(String[] v) {
        if (!IsRigaToken(v)) return null;
        String m = !v[11].isBlank() ? v[11] : v[8];
        return m == null || m.isBlank() ? null : m;
    }

    /**
     * Simbolo del token della posizione di un movimento di contratto <b>o di margine</b> (il contratto e il verso stanno nel campo dati).
     *
     * @return il simbolo, {@code null} se il movimento non e' di contratto ne' di margine o il campo dati e' incompleto
     */
    public static String ChiavePosizioneDaMovimento(String[] v) {
        if (v == null || v.length <= Derivati.CAMPO_DATI || !Derivati.IsRigaContrattoOMargine(v)) return null;
        Map<String, String> d = Derivati.Dati(v);
        String contratto = d.get("contratto");
        String verso = d.get("verso");
        if (contratto == null || contratto.isBlank() || verso == null) return null;
        return Derivati.NomeTokenContratto(contratto, !verso.equals("short"));
    }

    /**
     * Calcola tutte le posizioni a partire dai movimenti.
     *
     * @param movimenti i movimenti dell'archivio, in qualunque ordine (si ordinano per istante di esecuzione)
     * @return le posizioni per simbolo del token
     */
    public static Map<String, Posizione> Calcola(Iterable<String[]> movimenti) {
        List<String[]> righe = new ArrayList<>();
        for (String[] v : movimenti) if (IsRigaToken(v)) righe.add(v);
        righe.sort(OKXDerivati.ORDINE_ESECUZIONE);

        Map<String, Posizione> posizioni = new LinkedHashMap<>();
        //Prezzo medio della posizione, il metodo dell'exchange: serve solo a verificare che i prezzi di esecuzione spieghino il suo profitto.
        //Lineare: media pesata dei prezzi. Inverso: media pesata dei reciproci (somma q/p), come si fa per i contratti inversi.
        Map<String, BigDecimal[]> medie = new LinkedHashMap<>();   //[contratti aperti, somma q x prezzo (o q / prezzo)]
        for (String[] v : righe) {
            Map<String, String> d = Derivati.Dati(v);
            String token = ChiavePosizione(v);
            if (token == null) continue;
            String contratto = d.getOrDefault("contratto", "");
            boolean lungo = !"short".equals(d.get("verso"));
            String moneta = d.getOrDefault("moneta", "");
            Posizione p = posizioni.computeIfAbsent(token, k -> new Posizione(token, contratto, lungo, moneta, Inverso(contratto, moneta)));

            BigDecimal n = Num(d.get("contratti"));
            BigDecimal prezzo = Num(d.get("prezzo"));
            BigDecimal ctVal = Num(d.get("valoreContratto"));
            boolean apre = "apre".equals(d.get("azione"));
            if (n == null || n.signum() <= 0) continue;

            BigDecimal[] media = medie.computeIfAbsent(token, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            //Ogni operazione con nessun contratto aperto comincia un episodio nuovo, e il prezzo medio riparte da zero. Un'apertura
            //comincia la posizione. Una chiusura senza contratti aperti (posizione aperta prima dei token, o righe mancanti) sta in
            //un episodio a se', che non sara' mai verificato (contratti scoperti): se entrasse nell'episodio appena finito lo farebbe
            //tornare al profitto dell'exchange, o darebbe un conguaglio negli anni dopo
            if (p.contrattiAperti().signum() == 0) {
                p.episodioCorrente++;
                p.aperti.clear();
                media[0] = BigDecimal.ZERO;
                media[1] = BigDecimal.ZERO;
            }
            p.righeEpisodio.computeIfAbsent(p.episodioCorrente, k -> new ArrayList<>()).add(v[0]);
            if (prezzo == null || prezzo.signum() <= 0 || ctVal == null || ctVal.signum() <= 0) {
                p.datiIncompleti = true;
                p.episodiIncompleti.add(p.episodioCorrente);
                String anno = v[1] != null && v[1].length() >= 4 ? v[1].substring(0, 4) : "";
                p.primoAnnoIncompleto.merge(p.episodioCorrente, anno, (a, b) -> a.compareTo(b) <= 0 ? a : b);
            }
            if (apre) {
                p.aperti.push(new Lotto(v[0], v[1], prezzo == null ? BigDecimal.ZERO : prezzo, n));
                if (prezzo != null && prezzo.signum() > 0) {
                    media[0] = media[0].add(n);
                    media[1] = media[1].add(p.inverso ? n.divide(prezzo, MC) : n.multiply(prezzo));
                }
                continue;
            }
            Chiusura c = new Chiusura(v[0], v[1], n, prezzo == null ? BigDecimal.ZERO : prezzo, Num(d.get("pnl")));
            c.episodio = p.episodioCorrente;
            BigDecimal daChiudere = n;
            BigDecimal somma = BigDecimal.ZERO;
            while (daChiudere.signum() > 0 && !p.aperti.isEmpty()) {
                Lotto l = p.aperti.peek();
                BigDecimal q = l.contratti.min(daChiudere);
                BigDecimal diff = Differenziale(p, q, l.prezzo, c.prezzo, ctVal);
                c.pezzi.add(new Pezzo(l.data, l.prezzo, q, diff));
                if (diff != null) somma = somma.add(diff);
                l.contratti = l.contratti.subtract(q);
                daChiudere = daChiudere.subtract(q);
                if (l.contratti.signum() == 0) p.aperti.pop();
            }
            c.scoperti = daChiudere;
            c.differenzialeLifo = p.episodiIncompleti.contains(c.episodio) || prezzo == null ? null : somma;
            //Verifica: il profitto che l'exchange dichiara torna con il suo prezzo medio sui nostri prezzi?
            if (prezzo != null && prezzo.signum() > 0 && ctVal != null && ctVal.signum() > 0 && media[0].signum() > 0 && media[1].signum() > 0
                    && c.scoperti.signum() == 0) {
                BigDecimal mediaPrezzo = p.inverso ? media[0].divide(media[1], MC) : media[1].divide(media[0], MC);
                //per un inverso mediaPrezzo e' il prezzo medio armonico: il confronto usa il suo reciproco
                BigDecimal atteso;
                if (p.inverso) {
                    BigDecimal ia = BigDecimal.ONE.divide(mediaPrezzo, MC);
                    BigDecimal ic = BigDecimal.ONE.divide(prezzo, MC);
                    atteso = n.multiply(ctVal).multiply(p.lungo ? ia.subtract(ic) : ic.subtract(ia));
                    media[1] = media[1].subtract(n.divide(mediaPrezzo, MC));
                } else {
                    atteso = n.multiply(ctVal).multiply(p.lungo ? prezzo.subtract(mediaPrezzo) : mediaPrezzo.subtract(prezzo));
                    media[1] = media[1].subtract(n.multiply(mediaPrezzo));
                }
                media[0] = media[0].subtract(n);
                c.pnlConPrezzoMedio = atteso;
                if (c.pnlBorsa != null) {
                    BigDecimal tolleranza = TOLLERANZA_ASSOLUTA.max(c.pnlBorsa.abs().multiply(TOLLERANZA_RELATIVA));
                    c.verificata = atteso.subtract(c.pnlBorsa).abs().compareTo(tolleranza) <= 0;
                }
            }
            p.chiusure.add(c);
        }
        return posizioni;
    }

    /** Differenziale di {@code q} contratti aperti a {@code pa} e chiusi a {@code pc}; {@code null} se manca un dato. */
    static BigDecimal Differenziale(Posizione p, BigDecimal q, BigDecimal pa, BigDecimal pc, BigDecimal ctVal) {
        if (pa == null || pc == null || ctVal == null || pa.signum() <= 0 || pc.signum() <= 0 || ctVal.signum() <= 0) return null;
        BigDecimal base = q.multiply(ctVal);
        if (p.inverso) {
            BigDecimal ia = BigDecimal.ONE.divide(pa, MC);
            BigDecimal ic = BigDecimal.ONE.divide(pc, MC);
            return base.multiply(p.lungo ? ia.subtract(ic) : ic.subtract(ia)).round(new MathContext(18, RoundingMode.HALF_UP));
        }
        return base.multiply(p.lungo ? pc.subtract(pa) : pa.subtract(pc));
    }

    /** Inverso = regolato nel sottostante ({@code BTC-USD-SWAP}, moneta BTC); i contratti {@code _UM_} sono sempre lineari. */
    static boolean Inverso(String contratto, String moneta) {
        if (contratto == null || moneta == null || contratto.contains("_UM_")) return false;
        String[] parti = contratto.split("-");
        return parti.length > 0 && parti[0].equalsIgnoreCase(moneta.trim());
    }

    /** Chiave del campo dati con il prezzo in euro di un'unita' della moneta di regolamento all'istante dell'operazione. */
    public static final String CHIAVE_PREZZO_MONETA = "prezzoMoneta";

    /**
     * Chiave del campo dati della chiusura di token che porta il conguaglio, in euro: la differenza fra il profitto dell'exchange
     * e il reddito in LIFO delle chiusure degli anni precedenti dello stesso episodio. La stessa cifra sta in {@code [46]}.
     * Scritta e tolta da {@link #AggiornaRisultati} a ogni passata.
     */
    public static final String CHIAVE_CONGUAGLIO = "conguaglio";
    /** Anni delle chiusure in LIFO compensate dal conguaglio, separati da virgola. */
    public static final String CHIAVE_CONGUAGLIO_ANNI = "conguaglioAnni";

    /**
     * Scrive in {@code [46]} il reddito dei derivati secondo il LIFO. Per ogni chiusura di token di cui si conosce il
     * differenziale e il prezzo in euro della moneta di regolamento: {@code [46]} della riga di chiusura = differenziale in
     * euro (due decimali, come ogni altro {@code [46]}), e {@code [46]} = {@code 0.00} sulle righe di pnl classificate
     * ({@code DDV}/{@code PDV} con {@code [44]} PNL) dello stesso fill, che altrimenti conterebbero il profitto dell'exchange una
     * seconda volta. Ogni altra riga, e ogni fill senza i dati, resta come l'ha lasciata il motore.
     *
     * <p>Si richiama alla fine di ogni passata di {@code AggiornaPlusvalenze}: il motore riscrive {@code [46]} dei
     * movimenti che rielabora, questo passo ricalcola le posizioni da zero (solo le righe di token) e rimette tutto a posto.
     * Il prezzo della moneta si cerca nell'ordine: campo dati, righe di margine del fill, riga del pnl.
     *
     * <p>Il LIFO di una chiusura vale se il suo episodio torna fino al 31/12 del suo anno ({@link Posizione#verificataFinoAllAnno}).
     * Dal primo anno che non torna il reddito è il profitto dell'exchange, e una chiusura di quell'anno ({@link #DestinazioneConguaglio})
     * riceve in {@code [46]} il <b>conguaglio</b>: profitto dell'exchange meno LIFO, in euro, delle chiusure degli anni prima, con la
     * spiegazione ricavabile da {@link #TestoConguaglio}. Così gli anni prima non cambiano e il totale dell'episodio resta quello
     * dell'exchange (D18.9).
     *
     * @param mappa la mappa dei movimenti
     * @return quante chiusure hanno ricevuto il risultato in LIFO
     */
    public static int AggiornaRisultati(Map<String, String[]> mappa) {
        Map<String, List<String[]>> perBill = new LinkedHashMap<>();
        boolean haToken = false;
        for (String[] v : mappa.values()) {
            String fill = ChiaveFill(v);
            if (fill == null || v.length <= Derivati.CAMPO_RISULTATO || !Derivati.isDerivato(v)) continue;
            perBill.computeIfAbsent(fill, k -> new ArrayList<>()).add(v);
            if (IsRigaToken(v)) {
                haToken = true;
                if (v[8] != null && !v[8].isBlank()) {   //riga di chiusura: risultato e conguaglio ricalcolati qui sotto
                    v[Derivati.CAMPO_RISULTATO] = "";
                    Map<String, String> d = Derivati.Dati(v);
                    if (d.remove(CHIAVE_CONGUAGLIO) != null | d.remove(CHIAVE_CONGUAGLIO_ANNI) != null) Derivati.ImpostaDati(v, d);
                }
            }
        }
        //Le righe di pnl tornano al valore del motore prima di decidere quali azzerare, tutte e non solo quelle dei fill che
        //hanno ancora un token. Una passata incrementale non rielabora le righe prima della ripartenza: se una posizione
        //verificata smette di esserlo (una chiusura successiva che non torna) o se le righe di token di un fill spariscono
        //(cancellate a mano), le sue righe di pnl resterebbero a 0.00 dalla passata precedente, e il reddito di quella chiusura
        //sparirebbe. Con la passata completa il motore le avrebbe rimesse a posto.
        boolean contoP2 = ContoDerivati.Attivo();
        for (List<String[]> righe : perBill.values()) {
            for (String[] v : righe) RipristinaRisultatoMotore(v, contoP2, mappa);
        }
        if (!haToken) return 0;

        java.util.Set<String> fillCoperti = new java.util.HashSet<>();
        int n = 0;
        for (Posizione p : Calcola(mappa.values()).values()) {
            //Per episodio: profitto dell'exchange meno LIFO, in euro, delle chiusure scritte in LIFO, e i loro anni
            Map<Integer, BigDecimal> scarto = new java.util.HashMap<>();
            Map<Integer, java.util.TreeSet<String>> anniScarto = new java.util.HashMap<>();
            java.util.Set<Integer> conguagliati = new java.util.HashSet<>();
            for (int i = 0; i < p.chiusure.size(); i++) {   //in ordine di esecuzione
                Chiusura c = p.chiusure.get(i);
                String[] riga = mappa.get(c.id);
                if (riga == null || riga.length <= Derivati.CAMPO_RISULTATO) continue;
                List<String[]> fill = perBill.get(ChiaveFill(riga));
                //Solo se i prezzi spiegano il profitto dell'exchange nell'episodio fino a fine anno: altrimenti (unita' diverse,
                //righe mancanti) il LIFO sarebbe un numero senza fondamento. Una chiusura successiva che non torna non tocca gli anni prima
                if (!p.verificataFinoAllAnno(c)) {
                    //Il primo anno che non torna riprende la differenza lasciata dalle chiusure in LIFO degli anni prima: il totale
                    //dell'episodio resta quello del profitto dell'exchange. Le chiusure in LIFO sono tutte negli anni prima (dal primo
                    //anno che non torna nessuna lo e' piu'), quindi qui lo scarto e' completo
                    BigDecimal s = scarto.get(c.episodio);
                    if (s != null && conguagliati.add(c.episodio)) {
                        String[] destinazione = DestinazioneConguaglio(p, i, mappa, perBill);
                        BigDecimal euro = s.setScale(2, RoundingMode.HALF_UP);
                        if (destinazione != null && euro.signum() != 0) {
                            destinazione[Derivati.CAMPO_RISULTATO] = euro.toPlainString();
                            Map<String, String> d = Derivati.Dati(destinazione);
                            d.put(CHIAVE_CONGUAGLIO, euro.toPlainString());
                            d.put(CHIAVE_CONGUAGLIO_ANNI, String.join(",", anniScarto.get(c.episodio)));
                            Derivati.ImpostaDati(destinazione, d);
                        }
                    }
                    continue;
                }
                if (c.differenzialeLifo == null || c.scoperti.signum() > 0) continue;
                //Il reddito si conta solo quando il profitto dell'exchange dello stesso fill e' classificato come derivato: altrimenti
                //quel denaro entra come deposito da classificare e il reddito risulterebbe due volte se poi lo si classifica diversamente
                if (!PnlClassificati(fill)) continue;
                //Un token da un file CSV (DerivatiDaCsv) senza la riga del profitto collegata: quel profitto non si saprebbe azzerare e
                //conterebbe due volte. Resta il profitto dell'exchange
                if (RichiedeRigaProfitto(riga) && !HaRigaProfitto(fill)) continue;
                BigDecimal prezzo = PrezzoMoneta(fill);
                if (prezzo == null) continue;
                //Commissione compresa nella riga del profitto (DerivatiDaCsv, Bybit conto Derivati): la riga del profitto torna a zero come le
                //altre, quindi il reddito della chiusura e' il LIFO meno la commissione, arrotondato una volta sola. Lasciarla alla riga del
                //profitto come importo a se' la farebbe sparire nell'arrotondamento a due decimali, quando e' di pochi millesimi
                BigDecimal inclusa = Num(Derivati.Dati(riga).get(DerivatiDaCsv.CHIAVE_COMMISSIONE_INCLUSA));
                BigDecimal netto = inclusa == null ? c.differenzialeLifo : c.differenzialeLifo.subtract(inclusa);
                BigDecimal lifo = netto.multiply(prezzo).setScale(2, RoundingMode.HALF_UP);
                riga[Derivati.CAMPO_RISULTATO] = lifo.toPlainString();
                scarto.merge(c.episodio, RisultatoPnlAValore(fill, contoP2, mappa).subtract(lifo), BigDecimal::add);
                anniScarto.computeIfAbsent(c.episodio, k -> new java.util.TreeSet<>()).add(c.anno());
                fillCoperti.add(ChiaveFill(riga));
                n++;
            }
        }
        //Le righe di pnl dei fill in LIFO: il LIFO ha preso il loro risultato al valore del momento. Resta solo la parte che il motore
        //calcola diversamente (con il conto derivati, una perdita pagata dal conto vale il costo dei lotti scaricati): zero altrove
        for (String bill : fillCoperti) {
            for (String[] v : perBill.get(bill)) {
                if (!Derivati.HaMarcatore(v) || !Derivati.Tipo(v).equals(Derivati.PNL)) continue;
                BigDecimal motore = Derivati.Risultato(v);
                BigDecimal valore = RisultatoAValore(v, contoP2, mappa);
                BigDecimal resto = motore == null || valore == null ? BigDecimal.ZERO : motore.subtract(valore);
                v[Derivati.CAMPO_RISULTATO] = resto.setScale(2, RoundingMode.HALF_UP).toPlainString();
            }
        }
        return n;
    }

    /**
     * Il risultato di una riga di pnl al valore del momento, quello che il LIFO ridistribuisce fra le chiusure: il {@code [46]} del motore,
     * salvo una perdita pagata dal conto derivati con l'opzione Conto derivati, che per il motore vale il costo dei lotti scaricati e qui
     * vale meno il suo valore ({@code [15]}). La differenza fra i due resta sulla riga quando il fill passa in LIFO.
     *
     * @return il risultato, {@code null} se la riga non ne ha uno
     */
    static BigDecimal RisultatoAValore(String[] v, boolean contoP2, Map<String, String[]> mappa) {
        if (AddebitoSulConto(v, contoP2, mappa)) {
            BigDecimal valore = Num(v[15]);
            return valore == null ? null : valore.negate();
        }
        return Derivati.Risultato(v);
    }

    /** Somma di {@link #RisultatoAValore} delle righe di pnl classificate del fill. */
    static BigDecimal RisultatoPnlAValore(List<String[]> righeDelFill, boolean contoP2, Map<String, String[]> mappa) {
        BigDecimal t = BigDecimal.ZERO;
        if (righeDelFill == null) return t;
        for (String[] v : righeDelFill) {
            if (!Derivati.Tipo(v).equals(Derivati.PNL) || !Derivati.HaMarcatore(v)) continue;
            BigDecimal r = RisultatoAValore(v, contoP2, mappa);
            if (r != null) t = t.add(r);
        }
        return t;
    }

    /**
     * Con l'opzione Conto derivati, la riga e' un addebito ({@code PDV}) di una moneta non FIAT pagato dal conto derivati: il motore le da'
     * il costo dei lotti scaricati invece del valore. Stesso riconoscimento del motore ({@link ContoDerivati#SulContoP2}).
     */
    static boolean AddebitoSulConto(String[] v, boolean contoP2, Map<String, String[]> mappa) {
        if (!contoP2 || v == null || v.length <= Derivati.CAMPO_RISULTATO || v[18] == null) return false;
        boolean prelievo = (v[11] == null || v[11].isBlank()) && v[8] != null && !v[8].isBlank() && !"FIAT".equalsIgnoreCase(v[9]);
        return prelievo && !v[18].contains(Derivati.MARCATORE_ACCREDITO) && v[18].contains(Derivati.MARCATORE_ADDEBITO)
                && ContoDerivati.SulContoP2(v, mappa);
    }

    /**
     * Chiave del fill di un movimento: exchange e identificativo dell'operazione ({@code [3]|[24]}), la stessa della classificazione
     * all'importazione. Solo {@code [24]} metterebbe insieme due fill di exchange diversi con lo stesso identificativo.
     *
     * @return la chiave, {@code null} se il movimento non ha {@code [24]}
     */
    static String ChiaveFill(String[] v) {
        if (v == null || v.length <= 24 || v[24] == null || v[24].isBlank()) return null;
        return (v[3] == null ? "" : v[3]) + "|" + v[24];
    }

    /**
     * Riga di token che riceve il conguaglio di un episodio: la prima chiusura del primo anno che non torna, fra quelle con il pnl
     * classificato, altrimenti la prima di quell'anno comunque. Mai in un anno successivo: il conguaglio riguarda il passaggio dal
     * LIFO al profitto dell'exchange, che avviene in quell'anno, e un anno dopo sposterebbe il reddito di un anno. Con il pnl non
     * ancora classificato il conguaglio sta sulla riga di token e conta da solo: il profitto dell'exchange di quella chiusura entra
     * quando la si classifica, come per ogni altra chiusura senza LIFO.
     *
     * @param prima indice in {@code p.chiusure} della prima chiusura che non torna dell'episodio
     * @return la riga, {@code null} se nessuna chiusura di quell'anno sta nella mappa
     */
    static String[] DestinazioneConguaglio(Posizione p, int prima, Map<String, String[]> mappa, Map<String, List<String[]>> perBill) {
        Chiusura c = p.chiusure.get(prima);
        String anno = c.anno();
        String[] primaDellAnno = null;
        for (int j = prima; j < p.chiusure.size(); j++) {
            Chiusura d = p.chiusure.get(j);
            if (d.episodio != c.episodio || !d.anno().equals(anno)) continue;
            String[] riga = mappa.get(d.id);
            if (riga == null || riga.length <= Derivati.CAMPO_RISULTATO) continue;
            if (PnlClassificati(perBill.get(ChiaveFill(riga)))) return riga;
            if (primaDellAnno == null) primaDellAnno = riga;
        }
        return primaDellAnno;
    }

    /**
     * Rimette in {@code [46]} di una riga di pnl classificata il valore che le darebbe il motore
     * ({@link Derivati#RisultatoDaClassificazione}), negli stessi rami: deposito di una moneta non FIAT con {@code DDV}, prelievo
     * di una moneta non FIAT con {@code PDV}. Ogni altra riga resta com'e'.
     */
    static void RipristinaRisultatoMotore(String[] v, boolean contoP2, Map<String, String[]> mappa) {
        if (v == null || v.length <= Derivati.CAMPO_RISULTATO || !Derivati.Tipo(v).equals(Derivati.PNL) || !Derivati.HaMarcatore(v)) return;
        boolean deposito = (v[8] == null || v[8].isBlank()) && v[11] != null && !v[11].isBlank() && !"FIAT".equalsIgnoreCase(v[12]);
        boolean prelievo = (v[11] == null || v[11].isBlank()) && v[8] != null && !v[8].isBlank() && !"FIAT".equalsIgnoreCase(v[9]);
        boolean accredito = v[18].contains(Derivati.MARCATORE_ACCREDITO);
        if (accredito ? !deposito : !prelievo) return;
        //P2: un addebito pagato dal conto derivati vale per il motore il costo dei lotti scaricati dalla pila del conto (ContoDerivati).
        //Stesso riconoscimento del motore (SulContoP2): il sotto-wallet dei derivati o il collaterale di un rientro abbinato
        if (!accredito && contoP2 && ContoDerivati.SulContoP2(v, mappa)) {
            BigDecimal costo = Num(v[16]);
            v[Derivati.CAMPO_RISULTATO] = ContoDerivati.Differenza(BigDecimal.ZERO, costo == null ? BigDecimal.ZERO : costo);
            return;
        }
        BigDecimal valore = Num(v[15]);
        v[Derivati.CAMPO_RISULTATO] = Derivati.RisultatoDaClassificazione(accredito, valore == null ? BigDecimal.ZERO : valore);
    }

    /**
     * Quante righe di token con un reddito ({@code [46]}: LIFO o conguaglio) appartengono a fill di cui la vista mostra qualche riga
     * ma non quella del token: tipicamente un filtro sul solo wallet {@code Principale}, che lascia fuori il sotto-wallet
     * {@value OKXDerivati#SOTTOWALLET}. Serve alla nota accanto al totale dei redditi da derivati, che altrimenti sembrerebbe zero.
     *
     * @param mostrati le righe mostrate dalla tabella
     * @param mappa tutti i movimenti
     */
    public static int RedditiFuoriVista(Iterable<String[]> mostrati, Map<String, String[]> mappa) {
        java.util.Set<String> idMostrati = new java.util.HashSet<>();
        java.util.Set<String> bill = new java.util.HashSet<>();
        for (String[] v : mostrati) {
            if (v == null) continue;
            idMostrati.add(v[0]);
            String fill = ChiaveFill(v);
            if (fill != null && v.length > Derivati.CAMPO_RISULTATO && Derivati.isDerivato(v)) bill.add(fill);
        }
        if (bill.isEmpty() || mappa == null) return 0;
        int n = 0;
        for (String[] v : mappa.values()) {
            if (v == null || !IsRigaToken(v) || !bill.contains(ChiaveFill(v)) || idMostrati.contains(v[0])) continue;
            if (Derivati.Risultato(v) != null) n++;
        }
        return n;
    }

    /** Somma del {@code [46]} che il motore da' alle righe di pnl del fill (il profitto dell'exchange in euro), letta prima di azzerarle. */
    static BigDecimal RisultatoPnlMotore(List<String[]> righeDelFill) {
        BigDecimal t = BigDecimal.ZERO;
        if (righeDelFill == null) return t;
        for (String[] v : righeDelFill) {
            if (!Derivati.Tipo(v).equals(Derivati.PNL) || !Derivati.HaMarcatore(v)) continue;
            BigDecimal r = Derivati.Risultato(v);
            if (r != null) t = t.add(r);
        }
        return t;
    }

    /**
     * Spiegazione del conguaglio di una chiusura di token ({@link #CHIAVE_CONGUAGLIO}), vuota se la riga non ne ha uno. Testo per
     * l'utente: dice l'effetto, senza punto e virgola.
     */
    public static String TestoConguaglio(String[] v) {
        Map<String, String> d = Derivati.Dati(v);
        String euro = d.get(CHIAVE_CONGUAGLIO);
        if (euro == null || euro.isBlank()) return "";
        String anni = d.getOrDefault(CHIAVE_CONGUAGLIO_ANNI, "").replace(",", ", ");
        String anno = v[1] != null && v[1].length() >= 4 ? v[1].substring(0, 4) : "";
        return "Conguaglio di " + euro + " euro. La posizione era già aperta e le sue chiusure del " + anni
                + " hanno il reddito calcolato in LIFO. Nel " + anno + " una chiusura della stessa posizione non si ricostruisce dai prezzi"
                + " di esecuzione, quindi da qui il reddito torna al profitto dell'exchange. Questa chiusura riprende la differenza fra il"
                + " profitto dell'exchange e il LIFO delle chiusure del " + anni + ", così il totale della posizione resta quello dell'exchange"
                + " e il reddito degli anni precedenti non cambia.";
    }

    /**
     * @return {@code true} se il token viene dal file di un exchange diverso da OKX ({@link DerivatiDaCsv}): il suo profitto sta in una
     *         riga collegata solo quando l'abbinamento e' riuscito, e senza quella il LIFO non si applica
     */
    static boolean RichiedeRigaProfitto(String[] token) {
        String p = Derivati.Dati(token).get("piattaforma");
        return p != null && !p.isBlank() && !p.equalsIgnoreCase("OKX");
    }

    /** @return {@code true} se il fill ha almeno una riga di profitto ({@link Derivati#PNL}) */
    static boolean HaRigaProfitto(List<String[]> righeDelFill) {
        if (righeDelFill == null) return false;
        for (String[] v : righeDelFill) if (Derivati.Tipo(v).equals(Derivati.PNL)) return true;
        return false;
    }

    /** @return {@code true} se ogni riga di pnl del fill (se ce ne sono) e' classificata come derivato ({@code DDV}/{@code PDV}) */
    static boolean PnlClassificati(List<String[]> righeDelFill) {
        if (righeDelFill == null) return true;
        for (String[] v : righeDelFill) {
            if (Derivati.Tipo(v).equals(Derivati.PNL) && !Derivati.HaMarcatore(v)) return false;
        }
        return true;
    }

    /** Prezzo in euro di un'unita' della moneta di regolamento di un fill, dalle sue righe; {@code null} se nessuna lo da'. */
    static BigDecimal PrezzoMoneta(List<String[]> righeDelFill) {
        if (righeDelFill == null) return null;
        //1 - il campo dati, scritto dall'import con il prezzo vero di mercato
        for (String[] v : righeDelFill) {
            BigDecimal d = Num(Derivati.Dati(v).get(CHIAVE_PREZZO_MONETA));
            if (d != null && d.signum() > 0) return d;
        }
        //2 - il giroconto del fill: valore in euro (due decimali) diviso la sua quantita' (OKXDerivati.ImpostaQuantita tiene il valore
        //allineato quando la quantita' cambia, e nella forma "conto" la quantita' non e' piu' il margine)
        for (String[] v : righeDelFill) {
            if (!Derivati.Tipo(v).equals(Derivati.MARGINE)) continue;
            BigDecimal qta = Num(v[13] != null && !v[13].isBlank() ? v[13] : v[10]);
            if (qta != null) qta = qta.abs();
            BigDecimal valore = Num(v[15]);
            if (qta != null && qta.signum() > 0 && valore != null && valore.signum() > 0) return valore.divide(qta, MC);
        }
        //3 - la riga del pnl
        for (String[] v : righeDelFill) {
            if (!Derivati.Tipo(v).equals(Derivati.PNL)) continue;
            BigDecimal qta = Num(!v[10].isBlank() ? v[10] : v[13]);
            BigDecimal valore = Num(v[15]);
            if (qta != null && qta.signum() != 0 && valore != null && valore.signum() > 0) return valore.divide(qta.abs(), MC);
        }
        return null;
    }

    private static BigDecimal Num(String s) {
        if (s == null || s.isBlank() || !Funzioni.isNumeric(s.trim(), false)) return null;
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
