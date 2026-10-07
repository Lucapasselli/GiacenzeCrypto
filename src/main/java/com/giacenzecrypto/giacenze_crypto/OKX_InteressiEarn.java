package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ricostruzione degli interessi Simple Earn di OKX che lo storico interessi non restituisce piu'.
 *
 * <p>{@code finance/savings/lending-history} restituisce solo l'ultimo mese (documentazione OKX, verificato:
 * 720 record orari), e gli interessi non hanno nessun codice di bill. Un giorno che esce dalla finestra prima
 * di essere scaricato e' quindi perso: e' successo per i primi mesi del 2026 (prima importazione Earn ad
 * agosto), per il tratto fra due scaricamenti lontani piu' di un mese, e per i periodi Earn del 2023-24,
 * importati da CSV che gli interessi non li contengono.
 *
 * <p>Il <b>totale</b> pero' e' ricostruibile esattamente: in un periodo di detenzione (dal prodotto vuoto al
 * prodotto di nuovo vuoto, oppure fino a oggi) l'interesse maturato e' il saldo finale piu' i riscatti meno le
 * sottoscrizioni. Sottoscrizioni e riscatti sono i bill Funding 75/76, disponibili per tutto lo storico
 * ({@code savings_flussi} di {@code OKX_Earn.js}), il saldo attuale e' {@code savings_balance}. Per il periodo
 * ancora aperto OKX da' anche il cumulato ({@code earnings}), che fa da controllo: se non coincide non si
 * registra nulla.
 *
 * <p>La mancanza di un periodo (totale meno interessi gia' in archivio) si divide sui giorni scoperti, quelli
 * con capitale nel prodotto e senza interessi registrati, in proporzione al capitale: e' una <b>stima</b> a tasso
 * costante, esatta solo quando il giorno scoperto e' uno. Ne esce un REWARD per giorno, con ID
 * {@code EARN-<moneta>-<aaaammgg>-RIC}: un giorno ricostruito non e' piu' scoperto, quindi uno scaricamento
 * successivo non aggiunge nulla.
 *
 * <p>E' un <b>calcolo</b>, non un dato restituito dall'exchange: per questo si fa solo con
 * {@link #OPZIONE_RICOSTRUZIONE}, spenta di default. Spenta, l'import usa solo lo storico interessi ufficiale e se
 * questo comincia dopo il giorno richiesto lo dice ({@link #TestoAvvisoStoricoCorto}).
 *
 * <p>Gli anni fino al 2025 ({@link #PRIMO_GIORNO_SEMPRE_REGISTRATO}) si registrano solo con
 * {@link #OPZIONE_ANCHE_ANNI_PASSATI}: sono gli anni in cui la ricostruzione non esisteva, e gli interessi
 * mancanti l'utente puo' averli gia' sistemati a mano. Dal 2026 si suppone che lo scaricamento sia sempre passato
 * di qui, quindi i dati o non ci sono o sono corretti, e si registra sempre. La ripartizione si fa comunque
 * sull'intero periodo e poi si tolgono quei giorni, cosi' la quota del 2025 non scivola sui giorni del 2026.
 *
 * <p>Limite noto: la fine di un periodo si riconosce quando i riscatti raggiungono le sottoscrizioni. Un riscatto
 * parziale che supera il capitale lasciando nel prodotto parte degli interessi chiude il periodo in anticipo e
 * sposta quel resto nel successivo.
 */
final class OKX_InteressiEarn {

    private OKX_InteressiEarn() {}

    /** Causale delle righe ricostruite, mappata in {@code OKX.json} come i rendimenti normali ("Deposit yield"). */
    static final String CAUSALE = "Simple Earn interessi ricostruiti";

    /** Coda dell'ID delle righe ricostruite, dopo {@code EARN-<moneta>-<aaaammgg>} dei rendimenti scaricati. */
    static final String SUFFISSO = "-RIC";

    /** Opzione (personale.mv.db): calcola gli interessi che lo storico non restituisce piu'. Spenta di default. */
    static final String OPZIONE_RICOSTRUZIONE = "OKX_InteressiEarn_Ricostruzione";

    /** Opzione (personale.mv.db): registra anche gli interessi ricostruiti degli anni fino al 2025. Spenta di default. */
    static final String OPZIONE_ANCHE_ANNI_PASSATI = "OKX_InteressiEarn_AncheAnniPassati";

    /** Primo giorno registrato anche con l'opzione spenta: il taglio vale solo per gli anni fino al 2025. */
    static final String PRIMO_GIORNO_SEMPRE_REGISTRATO = "2026-01-01";

    /** Firma dell'ultimo avviso mostrato (personale.mv.db): lo stesso avviso non si ripete a ogni scaricamento. */
    static final String OPZIONE_ULTIMO_AVVISO = "OKX_InteressiEarn_UltimoAvviso";

    /** ID dei rendimenti Simple Earn, scaricati ({@code convertOKXEarn}) o ricostruiti. */
    static final Pattern CHIAVE_EARN = Pattern.compile("^EARN-(.+)-(\\d{8})(" + Pattern.quote(SUFFISSO) + ")?$");

    /** Interessi di un periodo oltre questa frazione del capitale massimo: dati incompleti, non si registra nulla. */
    static final BigDecimal RENDIMENTO_MASSIMO_PERIODO = new BigDecimal("0.5");

    /** Scarto ammesso fra il calcolo e il cumulato {@code earnings} di OKX per il periodo aperto. */
    static final BigDecimal TOLLERANZA_EARNINGS_RELATIVA = new BigDecimal("0.01");
    static final BigDecimal TOLLERANZA_EARNINGS_ASSOLUTA = new BigDecimal("0.000001");

    /** Sotto questa quota media per giorno scoperto la mancanza e' rumore (sfasamento orario fra saldo e storico). */
    static final BigDecimal QUOTA_MINIMA_GIORNO = new BigDecimal("0.00000001");

    static final int SCALA = 8;

    /** Sottoscrizione ({@code qta} positiva, entra nel prodotto) o riscatto (negativa). */
    record Flusso(long ts, String giorno, BigDecimal qta) {}

    /** Saldo attuale del prodotto e cumulato degli interessi del periodo aperto ({@code null} se assente). */
    record Saldo(BigDecimal amt, BigDecimal earnings) {}

    /** Periodo per cui e' stata calcolata una mancanza, per l'avviso. */
    record Periodo(String moneta, String dal, String al, BigDecimal mancanti, int giorni) {}

    static final class Esito {
        final List<String[]> righe = new ArrayList<>();
        final List<Periodo> ricostruiti = new ArrayList<>();
        final List<Periodo> anniPassati = new ArrayList<>();
        final List<String> anomalie = new ArrayList<>();

        boolean vuoto() {
            return righe.isEmpty() && ricostruiti.isEmpty() && anniPassati.isEmpty() && anomalie.isEmpty();
        }
    }

    private static final class Corso {
        final List<Flusso> flussi = new ArrayList<>();
        BigDecimal capitale = BigDecimal.ZERO;
        BigDecimal capitaleMassimo = BigDecimal.ZERO;
        boolean aperto = true;
    }

    /**
     * Toglie dai rendimenti appena scaricati il giorno piu' vecchio di ogni moneta quando la finestra restituita
     * comincia dopo il giorno richiesto: e' un giorno tagliato a meta' dal limite di un mese (il 07/10 lo storico
     * partiva dal 07/09 alle 09:00), e registrato cosi' resterebbe parziale per sempre, perche' il suo ID
     * giornaliero ferma la deduplica. Lo completa la ricostruzione, quindi si toglie solo per le monete che si
     * possono ricostruire ({@link #MoneteRicostruibili}): per le altre una riga parziale e' meglio di nessuna.
     *
     * @param righeEarn righe di {@code convertOKXEarn}, modificate sul posto
     * @param inizioRichiesto istante da cui e' stato chiesto lo storico interessi
     * @param moneteRicostruibili monete per cui il giorno tolto verra' ricostruito
     * @return il numero di righe tolte
     */
    static int ScartaPrimoGiornoTroncato(List<String[]> righeEarn, long inizioRichiesto, java.util.Set<String> moneteRicostruibili) {
        if (righeEarn == null || righeEarn.isEmpty()) return 0;
        String giornoRichiesto = Giorno(inizioRichiesto);
        Map<String, String> primoGiorno = new HashMap<>();
        for (String[] r : righeEarn) {
            Matcher m = CHIAVE_EARN.matcher(r[14]);
            if (!m.matches()) continue;
            String g = GiornoDaChiave(m.group(2));
            primoGiorno.merge(m.group(1), g, (a, b) -> a.compareTo(b) <= 0 ? a : b);
        }
        int tolte = 0;
        for (var it = righeEarn.iterator(); it.hasNext();) {
            String[] r = it.next();
            Matcher m = CHIAVE_EARN.matcher(r[14]);
            if (!m.matches()) continue;
            String g = GiornoDaChiave(m.group(2));
            if (g.equals(primoGiorno.get(m.group(1))) && g.compareTo(giornoRichiesto) > 0
                    && moneteRicostruibili.contains(m.group(1).toUpperCase())) {
                System.out.println("OKX Earn: " + r[5] + " del " + g + " scartato, giorno troncato dal limite di un mese dello storico interessi");
                it.remove();
                tolte++;
            }
        }
        return tolte;
    }

    /**
     * Monete che la ricostruzione puo' trattare: quelle con almeno una sottoscrizione, se sottoscrizioni e
     * riscatti sono arrivati per intero. Altrimenti nessuna.
     */
    static java.util.Set<String> MoneteRicostruibili(JsonObject jsonEarn) {
        java.util.Set<String> monete = new java.util.HashSet<>();
        if (jsonEarn == null || !jsonEarn.has("savings_flussi") || !jsonEarn.get("savings_flussi").isJsonArray()
                || !jsonEarn.has("savings_flussi_completo") || !jsonEarn.get("savings_flussi_completo").getAsBoolean()) {
            return monete;
        }
        for (JsonElement el : jsonEarn.getAsJsonArray("savings_flussi")) {
            if (!el.isJsonObject()) continue;
            JsonObject b = el.getAsJsonObject();
            if ("75".equals(Testo(b, "type")) && !Testo(b, "ccy").isEmpty()) monete.add(Testo(b, "ccy").toUpperCase());
        }
        return monete;
    }

    /**
     * Legge la risposta di {@code OKX_Earn.js} e gli interessi gia' registrati, e calcola le righe da aggiungere.
     *
     * @param jsonEarn risposta di {@code OKX_Earn.js}
     * @param righe righe di questo scaricamento (i rendimenti appena scaricati contano come registrati)
     * @param archivio movimenti in archivio
     * @param ancheAnniPassati {@link #OPZIONE_ANCHE_ANNI_PASSATI}
     * @param inizioRichiesto istante da cui e' stato chiesto lo storico interessi
     * @param adesso istante dello scaricamento
     */
    static Esito Ricostruisci(JsonObject jsonEarn, List<String[]> righe, Collection<String[]> archivio,
            boolean ancheAnniPassati, long inizioRichiesto, long adesso) {
        Esito esito = new Esito();
        if (jsonEarn == null || !jsonEarn.has("savings_flussi") || !jsonEarn.get("savings_flussi").isJsonArray()) {
            return esito;   //Script senza i flussi: nulla da ricostruire
        }
        if (!jsonEarn.has("savings_flussi_completo") || !jsonEarn.get("savings_flussi_completo").getAsBoolean()) {
            esito.anomalie.add("Sottoscrizioni e riscatti Simple Earn non scaricati per intero: interessi mancanti non ricostruiti.");
            return esito;
        }

        Map<String, List<Flusso>> flussi = new TreeMap<>();
        for (JsonElement el : jsonEarn.getAsJsonArray("savings_flussi")) {
            if (!el.isJsonObject()) continue;
            JsonObject b = el.getAsJsonObject();
            String ccy = Testo(b, "ccy").toUpperCase();
            String balChg = Testo(b, "balChg");
            String ts = Testo(b, "ts");
            if (ccy.isEmpty() || !Funzioni.isNumeric(balChg, false) || !Funzioni.isNumeric(ts, false)) continue;
            long t = Long.parseLong(ts);
            //Sottoscrizione: esce dal Funding (balChg negativo) ed entra nel prodotto
            flussi.computeIfAbsent(ccy, k -> new ArrayList<>()).add(new Flusso(t, Giorno(t), new BigDecimal(balChg).negate()));
        }

        Map<String, Saldo> saldi = new TreeMap<>();
        if (jsonEarn.has("savings_balance") && jsonEarn.get("savings_balance").isJsonArray()) {
            for (JsonElement el : jsonEarn.getAsJsonArray("savings_balance")) {
                if (!el.isJsonObject()) continue;
                JsonObject s = el.getAsJsonObject();
                String ccy = Testo(s, "ccy").toUpperCase();
                String amt = Testo(s, "amt");
                String earnings = Testo(s, "earnings");
                if (ccy.isEmpty() || !Funzioni.isNumeric(amt, false)) continue;
                saldi.put(ccy, new Saldo(new BigDecimal(amt),
                        Funzioni.isNumeric(earnings, false) ? new BigDecimal(earnings) : null));
            }
        }

        String oggi = Giorno(adesso);
        Map<String, BigDecimal> maturatiOggi = new TreeMap<>();
        long primoRecord = Long.MAX_VALUE;
        if (jsonEarn.has("savings_lending") && jsonEarn.get("savings_lending").isJsonArray()) {
            java.util.Set<String> visti = new java.util.HashSet<>();
            for (JsonElement el : jsonEarn.getAsJsonArray("savings_lending")) {
                if (!el.isJsonObject()) continue;
                JsonObject l = el.getAsJsonObject();
                String ccy = Testo(l, "ccy").toUpperCase();
                String earnings = Testo(l, "earnings");
                String ts = Testo(l, "ts");
                if (ccy.isEmpty() || !Funzioni.isNumeric(earnings, false) || !Funzioni.isNumeric(ts, false)) continue;
                primoRecord = Math.min(primoRecord, Long.parseLong(ts));
                if (!visti.add(ccy + "|" + ts) || !Giorno(Long.parseLong(ts)).equals(oggi)) continue;
                maturatiOggi.merge(ccy, new BigDecimal(earnings), BigDecimal::add);
            }
        }

        //Giorni coperti dalla finestra appena scaricata: chi non ha interessi li' li ha avuti davvero a zero (OKX
        //manda record a zero, o nessun record), quindi non ricevono quote. Il giorno piu' vecchio di una finestra
        //tagliata resta fuori: e' quello scartato da ScartaPrimoGiornoTroncato.
        java.util.Set<String> giorniCoperti = new java.util.HashSet<>();
        if (primoRecord != Long.MAX_VALUE) {
            LocalDate primoRicevuto = LocalDate.parse(Giorno(primoRecord));
            LocalDate richiesto = LocalDate.parse(Giorno(inizioRichiesto));
            LocalDate d = primoRicevuto.isAfter(richiesto) ? primoRicevuto.plusDays(1) : richiesto;
            for (LocalDate fine = LocalDate.parse(oggi); d.isBefore(fine); d = d.plusDays(1)) giorniCoperti.add(d.toString());
        }

        String primoGiorno = ancheAnniPassati ? null : PRIMO_GIORNO_SEMPRE_REGISTRATO;
        return Calcola(flussi, saldi, Registrati(righe, archivio), maturatiOggi, giorniCoperti, oggi, primoGiorno);
    }

    /**
     * Interessi gia' registrati per moneta e giorno: quelli in archivio ({@code [24]}) e quelli appena scaricati
     * ({@code [14]}), contati una volta sola per ID. Si riconoscono dall'ID e non dalla causale, perche' "Deposit
     * yield" (type 89) puo' venire anche da altri prodotti.
     */
    static Map<String, Map<String, BigDecimal>> Registrati(List<String[]> righe, Collection<String[]> archivio) {
        Map<String, BigDecimal> perChiave = new HashMap<>();
        if (archivio != null) {
            for (String[] v : archivio) {
                if (v.length > 24 && "OKX".equalsIgnoreCase(v[3]) && CHIAVE_EARN.matcher(v[24]).matches()
                        && Funzioni.isNumeric(v[13], false)) {
                    perChiave.merge(v[24].toUpperCase(), new BigDecimal(v[13]), BigDecimal::add);
                }
            }
        }
        if (righe != null) {
            for (String[] r : righe) {
                if (r[14] != null && CHIAVE_EARN.matcher(r[14]).matches() && Funzioni.isNumeric(r[6], false)) {
                    perChiave.putIfAbsent(r[14].toUpperCase(), new BigDecimal(r[6]));
                }
            }
        }
        Map<String, Map<String, BigDecimal>> registrati = new TreeMap<>();
        for (Map.Entry<String, BigDecimal> e : perChiave.entrySet()) {
            Matcher m = CHIAVE_EARN.matcher(e.getKey());
            if (!m.matches()) continue;
            registrati.computeIfAbsent(m.group(1), k -> new TreeMap<>())
                    .merge(GiornoDaChiave(m.group(2)), e.getValue(), BigDecimal::add);
        }
        return registrati;
    }

    /**
     * Il calcolo, senza rete e senza archivio.
     *
     * @param flussi sottoscrizioni e riscatti per moneta
     * @param saldi saldo attuale per moneta (assente = prodotto vuoto)
     * @param registrati interessi gia' registrati per moneta e giorno ({@code aaaa-MM-gg})
     * @param maturatiOggi interessi di oggi, che entreranno con lo scaricamento di domani
     * @param oggi giorno dello scaricamento, mai ricostruito
     * @param primoGiornoRegistrabile i giorni precedenti si calcolano ma non si registrano; {@code null} = tutti
     */
    static Esito Calcola(Map<String, List<Flusso>> flussi, Map<String, Saldo> saldi,
            Map<String, Map<String, BigDecimal>> registrati, Map<String, BigDecimal> maturatiOggi,
            String oggi, String primoGiornoRegistrabile) {
        return Calcola(flussi, saldi, registrati, maturatiOggi, java.util.Set.of(), oggi, primoGiornoRegistrabile);
    }

    /**
     * Come sopra, con i giorni coperti dallo storico interessi appena scaricato: senza interessi registrati
     * hanno avuto interesse zero, quindi contano nel totale ma non ricevono quote.
     */
    static Esito Calcola(Map<String, List<Flusso>> flussi, Map<String, Saldo> saldi,
            Map<String, Map<String, BigDecimal>> registrati, Map<String, BigDecimal> maturatiOggi,
            java.util.Set<String> giorniCoperti, String oggi, String primoGiornoRegistrabile) {
        Esito esito = new Esito();
        TreeSet<String> monete = new TreeSet<>(flussi.keySet());
        for (Map.Entry<String, Saldo> s : saldi.entrySet()) if (s.getValue().amt().signum() > 0) monete.add(s.getKey());
        int indiceMoneta = 0;

        for (String moneta : monete) {
            int secondo = indiceMoneta++ % 60;
            List<Flusso> lista = new ArrayList<>(flussi.getOrDefault(moneta, List.of()));
            lista.sort((a, b) -> Long.compare(a.ts(), b.ts()));
            Saldo saldo = saldi.get(moneta);
            BigDecimal saldoAttuale = saldo == null ? BigDecimal.ZERO : saldo.amt();

            //Periodi di detenzione
            List<Corso> periodi = new ArrayList<>();
            Corso corso = null;
            boolean anomala = false;
            for (Flusso f : lista) {
                if (corso == null) {
                    if (f.qta().signum() <= 0) {
                        esito.anomalie.add(moneta + ": riscatto del " + f.giorno() + " senza una sottoscrizione precedente.");
                        anomala = true;
                        break;
                    }
                    corso = new Corso();
                }
                corso.flussi.add(f);
                corso.capitale = corso.capitale.add(f.qta());
                corso.capitaleMassimo = corso.capitaleMassimo.max(corso.capitale);
                if (f.qta().signum() < 0 && corso.capitale.signum() <= 0) {
                    corso.aperto = false;
                    periodi.add(corso);
                    corso = null;
                }
            }
            if (anomala) continue;
            if (corso != null) periodi.add(corso);
            Corso ultimo = periodi.isEmpty() ? null : periodi.get(periodi.size() - 1);
            if (saldoAttuale.signum() > 0) {
                if (ultimo == null) {
                    esito.anomalie.add(moneta + ": saldo Earn di " + Testo(saldoAttuale) + " senza sottoscrizioni.");
                    continue;
                }
                //Prodotto non vuoto dopo un riscatto che sembrava chiuderlo: il resto e' interesse del periodo
                ultimo.aperto = true;
            } else if (ultimo != null && ultimo.aperto) {
                esito.anomalie.add(moneta + ": sottoscrizioni non riscattate ma saldo Earn nullo, periodo dal "
                        + ultimo.flussi.get(0).giorno() + " non ricostruito.");
                periodi.remove(periodi.size() - 1);
            }

            Map<String, BigDecimal> giorniRegistrati = registrati.getOrDefault(moneta, Map.of());
            //Una riga per giorno anche quando due periodi si toccano (riscatto e nuova sottoscrizione lo stesso
            //giorno) e tutti e due lo trovano scoperto: le quote si sommano, l'ID resta unico
            Map<String, BigDecimal> quotePerGiorno = new TreeMap<>();
            for (int ip = 0; ip < periodi.size(); ip++) {
                Corso p = periodi.get(ip);
                //Un giorno registrato condiviso con il periodo successivo si conta una volta sola, in quello
                String inizioSuccessivo = ip + 1 < periodi.size() ? periodi.get(ip + 1).flussi.get(0).giorno() : null;
                BigDecimal interessi = p.aperto ? saldoAttuale.subtract(p.capitale) : p.capitale.negate();
                String dal = p.flussi.get(0).giorno();
                String al = p.aperto ? oggi : p.flussi.get(p.flussi.size() - 1).giorno();

                if (p.aperto && saldo != null && saldo.earnings() != null) {
                    BigDecimal scarto = interessi.subtract(saldo.earnings()).abs();
                    BigDecimal ammesso = TOLLERANZA_EARNINGS_ASSOLUTA.max(saldo.earnings().abs().multiply(TOLLERANZA_EARNINGS_RELATIVA));
                    if (scarto.compareTo(ammesso) > 0) {
                        esito.anomalie.add(moneta + ": interessi calcolati " + Testo(interessi) + " contro "
                                + Testo(saldo.earnings()) + " dichiarati da OKX, periodo dal " + dal + " non ricostruito.");
                        continue;
                    }
                }
                if (interessi.signum() < 0) {
                    esito.anomalie.add(moneta + ": riscattato meno del sottoscritto nel periodo " + dal + " - " + al + ", non ricostruito.");
                    continue;
                }
                if (interessi.compareTo(p.capitaleMassimo.multiply(RENDIMENTO_MASSIMO_PERIODO)) > 0) {
                    esito.anomalie.add(moneta + ": interessi di " + Testo(interessi) + " su un capitale di "
                            + Testo(p.capitaleMassimo) + " nel periodo " + dal + " - " + al + ", dati incompleti, non ricostruito.");
                    continue;
                }

                //Giorni del periodo: peso = capitale nel prodotto quel giorno (il maggiore fra inizio e fine giornata)
                LocalDate primo = LocalDate.parse(dal);
                LocalDate ultimoGiorno = p.aperto ? LocalDate.parse(oggi).minusDays(1) : LocalDate.parse(al);
                BigDecimal registratiPeriodo = BigDecimal.ZERO;
                for (Map.Entry<String, BigDecimal> g : giorniRegistrati.entrySet()) {
                    if (g.getKey().compareTo(dal) >= 0 && g.getKey().compareTo(al) <= 0 && !g.getKey().equals(inizioSuccessivo)) {
                        registratiPeriodo = registratiPeriodo.add(g.getValue());
                    }
                }
                if (p.aperto) registratiPeriodo = registratiPeriodo.add(maturatiOggi.getOrDefault(moneta, BigDecimal.ZERO));
                //Gli interessi gia' registrati possono avere piu' decimali dei flussi (somme di accrediti orari)
                BigDecimal mancanti = interessi.subtract(registratiPeriodo).setScale(SCALA, RoundingMode.HALF_UP);

                Map<String, BigDecimal> pesi = new TreeMap<>();
                BigDecimal capitale = BigDecimal.ZERO;
                int indice = 0;
                for (LocalDate d = primo; !d.isAfter(ultimoGiorno); d = d.plusDays(1)) {
                    String giorno = d.toString();
                    BigDecimal inizio = capitale;
                    while (indice < p.flussi.size() && p.flussi.get(indice).giorno().equals(giorno)) {
                        capitale = capitale.add(p.flussi.get(indice).qta());
                        indice++;
                    }
                    BigDecimal peso = inizio.max(capitale);
                    if (peso.signum() > 0 && !giorniRegistrati.containsKey(giorno) && !giorniCoperti.contains(giorno)) pesi.put(giorno, peso);
                }

                if (mancanti.signum() <= 0) {
                    if (mancanti.negate().compareTo(TOLLERANZA_EARNINGS_ASSOLUTA) > 0) {
                        System.out.println("OKX Earn: " + moneta + " " + dal + " - " + al + ", registrati "
                                + Testo(mancanti.negate()) + " piu' degli interessi calcolati");
                    }
                    continue;
                }
                if (pesi.isEmpty() || mancanti.divide(BigDecimal.valueOf(pesi.size()), SCALA, RoundingMode.DOWN)
                        .compareTo(QUOTA_MINIMA_GIORNO) < 0) {
                    System.out.println("OKX Earn: " + moneta + " " + dal + " - " + al + ", mancano " + Testo(mancanti)
                            + " ma " + (pesi.isEmpty() ? "nessun giorno e' scoperto" : "la quota per giorno e' trascurabile"));
                    continue;
                }

                BigDecimal pesoTotale = pesi.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal assegnati = BigDecimal.ZERO;
                BigDecimal passati = BigDecimal.ZERO;
                int giorniPassati = 0, giorniRegistrabili = 0;
                String primoPassato = null, ultimoPassato = null, primoRegistrato = null, ultimoRegistrato = null;
                int n = 0;
                for (Map.Entry<String, BigDecimal> g : pesi.entrySet()) {
                    n++;
                    BigDecimal quota = n == pesi.size() ? mancanti.subtract(assegnati)
                            : mancanti.multiply(g.getValue()).divide(pesoTotale, SCALA, RoundingMode.DOWN);
                    assegnati = assegnati.add(quota);
                    if (quota.signum() <= 0) continue;
                    String giorno = g.getKey();
                    if (primoGiornoRegistrabile != null && giorno.compareTo(primoGiornoRegistrabile) < 0) {
                        passati = passati.add(quota);
                        giorniPassati++;
                        if (primoPassato == null) primoPassato = giorno;
                        ultimoPassato = giorno;
                        continue;
                    }
                    quotePerGiorno.merge(giorno, quota, BigDecimal::add);
                    giorniRegistrabili++;
                    if (primoRegistrato == null) primoRegistrato = giorno;
                    ultimoRegistrato = giorno;
                }
                if (giorniRegistrabili > 0) {
                    esito.ricostruiti.add(new Periodo(moneta, primoRegistrato, ultimoRegistrato,
                            mancanti.subtract(passati), giorniRegistrabili));
                }
                if (giorniPassati > 0) {
                    esito.anniPassati.add(new Periodo(moneta, primoPassato, ultimoPassato, passati, giorniPassati));
                }
            }
            for (Map.Entry<String, BigDecimal> q : quotePerGiorno.entrySet()) {
                esito.righe.add(Riga(moneta, q.getKey(), q.getValue(), secondo));
            }
        }
        return esito;
    }

    /** Riga intermedia a 19 campi, come quelle di {@code convertOKXEarn}. */
    private static String[] Riga(String moneta, String giorno, BigDecimal quota, int secondo) {
        String chiave = "EARN-" + moneta + "-" + giorno.replace("-", "") + SUFFISSO;
        String[] r = new String[19];
        //Un secondo diverso per moneta e l'ID anche come ordine: due righe nello stesso istante restano due movimenti
        r[0]  = giorno + " 23:59:" + (secondo < 10 ? "0" : "") + secondo;
        r[1]  = "OKX";
        r[2]  = "Funding";
        r[3]  = "";
        r[4]  = CAUSALE;
        r[5]  = moneta;
        r[6]  = quota.stripTrailingZeros().toPlainString();
        r[11] = "";
        r[12] = "";
        r[13] = chiave;
        r[14] = chiave;
        r[15] = "NO";
        r[16] = "";
        Importazioni.RiempiVuotiArray(r);
        return r;
    }

    /**
     * Testo dell'avviso di fine calcolo, oppure stringa vuota se non c'e' nulla da dire.
     */
    static String TestoAvviso(Esito esito) {
        if (esito == null || esito.vuoto()) return "";
        StringBuilder sb = new StringBuilder();
        if (!esito.ricostruiti.isEmpty()) {
            sb.append("Interessi Simple Earn ricostruiti per i giorni che OKX\n")
              .append("non restituisce più (lo storico copre solo l'ultimo mese):\n\n");
            for (Periodo p : esito.ricostruiti) sb.append(RigaPeriodo(p));
            sb.append("\nIl totale di ogni periodo è esatto (saldo + riscatti - sottoscrizioni),\n")
              .append("la divisione per giorno è una stima in proporzione al capitale.\n")
              .append("Causale \"").append(CAUSALE).append("\".\n");
        }
        if (!esito.anniPassati.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Non registrati perché negli anni fino al 2025:\n\n");
            for (Periodo p : esito.anniPassati) sb.append(RigaPeriodo(p));
            sb.append("\nPer registrarli attiva l'opzione \"Interessi Earn calcolati anche\n")
              .append("negli anni fino al 2025\" in Exchange API, scheda Particolarità OKX,\n")
              .append("e scarica di nuovo.\n");
        }
        if (!esito.anomalie.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Non ricostruiti:\n\n");
            for (String a : esito.anomalie) sb.append("   ").append(a).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Con la ricostruzione spenta: se lo storico interessi restituito comincia dopo il giorno richiesto, i giorni
     * precedenti (se c'erano interessi) non sono stati importati e il primo giorno ricevuto e' parziale. Restituisce
     * il testo dell'avviso, oppure stringa vuota se lo storico copre tutto il periodo richiesto.
     */
    static String TestoAvvisoStoricoCorto(JsonObject jsonEarn, long inizioRichiesto) {
        if (jsonEarn == null || !jsonEarn.has("savings_lending") || !jsonEarn.get("savings_lending").isJsonArray()) return "";
        long primo = Long.MAX_VALUE;
        for (JsonElement el : jsonEarn.getAsJsonArray("savings_lending")) {
            if (!el.isJsonObject()) continue;
            String ts = Testo(el.getAsJsonObject(), "ts");
            if (Funzioni.isNumeric(ts, false)) primo = Math.min(primo, Long.parseLong(ts));
        }
        if (primo == Long.MAX_VALUE) return "";
        String giornoPrimo = Giorno(primo);
        String giornoRichiesto = Giorno(inizioRichiesto);
        if (giornoPrimo.compareTo(giornoRichiesto) <= 0) return "";
        return "OKX ha restituito gli interessi Simple Earn solo dal " + giornoPrimo + "\n"
                + "(lo storico interessi copre solo l'ultimo mese), lo scaricamento\n"
                + "partiva dal " + giornoRichiesto + ".\n\n"
                + "Gli interessi dei giorni precedenti, se ce ne sono, non sono stati importati,\n"
                + "e quelli del " + giornoPrimo + " sono parziali.\n\n"
                + "Il programma può calcolarli da saldo, sottoscrizioni e riscatti se attivi\n"
                + "\"Calcola gli interessi Earn che OKX non restituisce più\" in Exchange API,\n"
                + "scheda Particolarità OKX.";
    }

    /**
     * Firma della parte dell'avviso che si ripete identica a ogni scaricamento (anni fino al 2025 non registrati e
     * anomalie): mostrata una volta, non si ripropone finche' non cambia. Le righe ricostruite invece si mostrano
     * sempre, tanto compaiono una volta sola.
     */
    static String FirmaRipetibile(Esito esito) {
        StringBuilder sb = new StringBuilder();
        for (Periodo p : esito.anniPassati) sb.append(RigaPeriodo(p));
        for (String a : esito.anomalie) sb.append(a).append('\n');
        return Integer.toHexString(sb.toString().hashCode());
    }

    private static String RigaPeriodo(Periodo p) {
        return "   " + p.moneta() + "   " + Testo(p.mancanti()) + "   in " + p.giorni() + (p.giorni() == 1 ? " giorno" : " giorni")
                + (p.dal().equals(p.al()) ? ", il " + p.dal() : ", dal " + p.dal() + " al " + p.al()) + "\n";
    }

    private static String Giorno(long istante) {
        return FunzioniDate.ConvertiDatadaLongAlSecondo(istante).substring(0, 10);
    }

    private static String GiornoDaChiave(String aaaammgg) {
        return aaaammgg.substring(0, 4) + "-" + aaaammgg.substring(4, 6) + "-" + aaaammgg.substring(6, 8);
    }

    private static String Testo(JsonObject o, String campo) {
        return o.has(campo) && !o.get(campo).isJsonNull() ? o.get(campo).getAsString().trim() : "";
    }

    private static String Testo(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
