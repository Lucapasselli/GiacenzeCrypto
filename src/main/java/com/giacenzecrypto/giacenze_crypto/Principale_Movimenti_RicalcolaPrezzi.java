package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.awt.Window;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Logica operativa della voce di menu contestuale "Ricalcola Prezzi": rifà la ricerca del prezzo sui
 * soli movimenti selezionati, <b>compresi i token che risultano irrecuperabili</b> ({@code PrezziKO}).
 *
 * <p><b>Cosa ha in comune e cosa no con "Ricalcola i prezzi delle transazioni" (Opzioni).</b> Il ciclo di
 * valorizzazione è lo stesso — pre-scarico a lotti, poi {@link Prezzi#DammiPrezzoDaTransazione} sui
 * movimenti con {@code [14]} vuoto, sostituendo {@code [15]} solo con un valore trovato e diverso da
 * {@code 0.00}, e svuotando {@code [32]} perché sia {@link Prezzi#isMovimentoPrezzato} a riprendere la
 * decisione. Cambiano due cose. (1) L'insieme dei movimenti è la selezione, non "dall'anno X in poi".
 * (2) Prima di cercare si <b>cancellano da {@code PrezziKO} le righe dei token coinvolti, agli istanti dei
 * movimenti selezionati</b>: senza, sia il pre-scarico ({@code FiltraRichiesteGiaCoperte}) sia
 * {@code CambioXXXEUR}/{@code CambioAddressEUR} uscirebbero al primo controllo dichiarando il prezzo
 * irrecuperabile, e il "ricalcolo" non interrogherebbe nessuno proprio dove serve.</p>
 *
 * <p><b>Quali righe di {@code PrezziKO} si cancellano.</b> Il ciclo che le scrive ha due chiavi, e vanno
 * coperte entrambe: {@code (SIMBOLO, istante, "", "")} da {@code CambioXXXEUR} — il simbolo è quello
 * <i>dopo</i> la normalizzazione di {@code Mappa_MoneteStessoPrezzo} e, per gli address di
 * {@code Mappa_AddressRete_Nome}, quello dell'alias, quindi qui si prendono tutte le varianti — e
 * {@code ("", istante, rete, address)} da {@code CambioAddressEUR}. L'istante è quello che il ciclo passa
 * senza modificarlo: {@code ConvertiDatainLongMinuto(v[1])}. Non si toccano i {@code PrezziKO} di altri
 * istanti, né i marcatori orari degli exchange ({@code PrezziOraCCXT}): un'ora già interrogata con
 * successo da tutti gli exchange non viene richiesta di nuovo, ed è voluto.</p>
 *
 * <p><b>Annullamento.</b> L'operazione non tocca nessun movimento finché non ha finito: i nuovi prezzi si
 * raccolgono e si applicano tutti insieme in coda. Se l'utente interrompe (pulsante "Interrompi" o chiusura
 * della finestra di avanzamento) i movimenti restano com'erano e le righe di {@code PrezziKO} cancellate
 * <b>vengono rimesse</b> — altrimenti un token davvero senza prezzo tornerebbe ad essere cercato a ogni
 * ricalcolo. Restano i prezzi eventualmente già scaricati nella cache: sono dati veri e non cambiano
 * nessun movimento.</p>
 *
 * <p>Il ricalcolo delle tabelle resta al chiamante, una sola volta per tutta la selezione.</p>
 *
 * @author lucap
 */
public class Principale_Movimenti_RicalcolaPrezzi {

    private Principale_Movimenti_RicalcolaPrezzi() {
    }

    /** Una riga di {@code PrezziKO}. */
    record RigaKO(String symbol, long timestamp, String rete, String address) {
    }

    /**
     * Movimenti della selezione a cui ha senso rifare il prezzo: quelli che esistono e non hanno un
     * controvalore già dichiarato in {@code [14]} (il ciclo di valorizzazione, come quello globale, li
     * salta comunque).
     * @param IDs ID selezionati, anche {@code null}
     * @return le righe, nell'ordine della selezione e senza doppioni
     */
    static List<String[]> MovimentiRicalcolabili(List<String> IDs) {
        List<String[]> risultato = new ArrayList<>();
        if (IDs == null) return risultato;
        Set<String> visti = new LinkedHashSet<>();
        for (String ID : IDs) {
            if (ID == null || !visti.add(ID)) continue;
            String[] v = MappaCryptoWallet.get(ID);
            if (v == null || v.length <= 40) continue;
            if (v[14] != null && !v[14].isBlank()) continue;
            risultato.add(v);
        }
        return risultato;
    }

    /**
     * Abilitazione della voce di menu: almeno un movimento della selezione è ricalcolabile.
     * @param IDs ID dei movimenti selezionati
     * @return {@code true} se c'è qualcosa da ricalcolare
     */
    public static boolean isRicalcolabile(List<String> IDs) {
        return !MovimentiRicalcolabili(IDs).isEmpty();
    }

    /**
     * Le chiavi di {@code PrezziKO} che un movimento può aver generato, come coppie
     * {@code istante|SIMBOLO} (riga per simbolo) e {@code istante|address} (riga per address).
     * Le monete FIAT, SCAM ed E-Money non passano dalla ricerca su exchange e non ne hanno.
     */
    private static void ChiaviKODelMovimento(String[] v, Set<String> perSimbolo, Set<String> perAddress) {
        long istante = FunzioniDate.ConvertiDatainLongMinuto(v[1]);
        if (istante <= 0) return;
        String rete = "";
        try {
            String r = Funzioni.TrovaReteDaIMovimento(v);
            if (r != null) rete = r;
        } catch (RuntimeException ex) {
            rete = "";
        }
        for (int k = 0; k < 2; k++) {
            String moneta = k == 0 ? v[8] : v[11];
            String tipo = k == 0 ? v[9] : v[12];
            if (moneta == null || moneta.isBlank()) continue;
            if (tipo != null && tipo.trim().equalsIgnoreCase("FIAT")) continue;
            String address = k == 0 ? v[26] : v[28];
            boolean haAddress = address != null && !address.isBlank();

            //Simbolo: quello del movimento, quello normalizzato e l'alias dell'address, ognuno in
            //maiuscolo come lo scrive PrezzoIrrecuperabileDaDB_Scrivi
            Set<String> simboli = new LinkedHashSet<>();
            simboli.add(moneta);
            simboli.add(Principale.Mappa_MoneteStessoPrezzo.getOrDefault(moneta, moneta));
            if (haAddress && !rete.isBlank()) {
                String alias = Principale.Mappa_AddressRete_Nome.get(address + "_" + rete);
                if (alias != null) {
                    simboli.add(alias);
                    simboli.add(Principale.Mappa_MoneteStessoPrezzo.getOrDefault(alias, alias));
                }
            }
            for (String s : simboli) perSimbolo.add(istante + "|" + s.toUpperCase());
            if (haAddress) perAddress.add(istante + "|" + address);
        }
    }

    /**
     * Le righe di {@code PrezziKO} che verrebbero cancellate per questi movimenti. Non cancella nulla:
     * serve sia a dire all'utente quante sono prima di chiedergli conferma, sia a poterle rimettere.
     * @param Movimenti movimenti da ricalcolare
     * @return le righe trovate, senza doppioni
     */
    static List<RigaKO> TrovaKO(List<String[]> Movimenti) {
        Set<String> perSimbolo = new LinkedHashSet<>();
        Set<String> perAddress = new LinkedHashSet<>();
        for (String[] v : Movimenti) ChiaviKODelMovimento(v, perSimbolo, perAddress);

        Map<String, RigaKO> trovate = new LinkedHashMap<>();
        String sqlSimbolo = "SELECT symbol, timestamp, rete, address FROM PrezziKO WHERE timestamp = ? AND symbol = ?";
        String sqlAddress = "SELECT symbol, timestamp, rete, address FROM PrezziKO "
                + "WHERE timestamp = ? AND symbol = '' AND LOWER(address) = LOWER(?)";
        LeggiKO(sqlSimbolo, perSimbolo, trovate);
        LeggiKO(sqlAddress, perAddress, trovate);
        return new ArrayList<>(trovate.values());
    }

    private static void LeggiKO(String sql, Set<String> chiavi, Map<String, RigaKO> trovate) {
        if (chiavi.isEmpty()) return;
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(sql)) {
            for (String chiave : chiavi) {
                int sep = chiave.indexOf('|');
                String valore = chiave.substring(sep + 1);
                ps.setLong(1, Long.parseLong(chiave.substring(0, sep)));
                ps.setString(2, valore);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        RigaKO r = new RigaKO(rs.getString(1), rs.getLong(2), rs.getString(3), rs.getString(4));
                        trovate.put(r.timestamp() + "|" + r.symbol() + "|" + r.rete() + "|" + r.address(), r);
                    }
                }
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }

    /**
     * Cancella da {@code PrezziKO} le righe indicate (chiave primaria completa).
     * @return quante ne ha cancellate
     */
    static int EliminaKO(List<RigaKO> righe) {
        int cancellate = 0;
        String sql = "DELETE FROM PrezziKO WHERE timestamp = ? AND symbol = ? AND rete = ? AND address = ?";
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(sql)) {
            for (RigaKO r : righe) {
                ps.setLong(1, r.timestamp());
                ps.setString(2, r.symbol());
                ps.setString(3, r.rete());
                ps.setString(4, r.address());
                cancellate += ps.executeUpdate();
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return cancellate;
    }

    /**
     * Rimette in {@code PrezziKO} le righe cancellate, dopo un'interruzione. Scrive direttamente e non con
     * {@link Prezzi#PrezzoIrrecuperabileDaDB_Scrivi}, che scarta gli istanti "troppo recenti": qui si
     * ripristina uno stato che esisteva già. {@code MERGE} perché nel frattempo il ciclo può aver già
     * riscritto la stessa riga.
     */
    static void RipristinaKO(List<RigaKO> righe) {
        String sql = "MERGE INTO PrezziKO (symbol, timestamp, rete, address) KEY (symbol, timestamp, rete, address) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseH2.connectionPrezzi.prepareStatement(sql)) {
            for (RigaKO r : righe) {
                ps.setString(1, r.symbol());
                ps.setLong(2, r.timestamp());
                ps.setString(3, r.rete());
                ps.setString(4, r.address());
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }

    /**
     * Avvisa l'utente, chiede conferma e ricalcola i prezzi dei movimenti indicati.
     *
     * <p>Rilegge la selezione dalla mappa (la lista arriva dal popup e potrebbe essere datata). Senza
     * connessione non parte nemmeno: cancellare i {@code PrezziKO} per poi non poter cercare nulla
     * lascerebbe solo il danno. Blocca fino alla fine dell'operazione (finestra di avanzamento modale).</p>
     *
     * @param IDs   ID dei movimenti selezionati
     * @param owner finestra proprietaria dei dialoghi
     * @return {@code true} se almeno un prezzo è stato modificato (e quindi le tabelle vanno ricalcolate)
     */
    public static boolean RicalcolaPrezzi(List<String> IDs, Window owner) {
        List<String[]> movimenti = MovimentiRicalcolabili(IDs);
        if (movimenti.isEmpty()) return false;

        if (!Funzioni.CeConnessioneInternet()) {
            Messaggi.WarningMessage("Ricalcola Prezzi",
                    "Non c'è connessione a Internet.",
                    "Il ricalcolo dei prezzi ha bisogno della rete: non è stato fatto nulla.", owner);
            return false;
        }

        List<RigaKO> koCoinvolti = TrovaKO(movimenti);
        if (!Messaggi.ConfermaRicalcoloPrezzi(movimenti.size(), koCoinvolti.size(), owner)) return false;

        EliminaKO(koCoinvolti);

        Download progress = new Download();
        progress.setLocationRelativeTo(owner);
        boolean[] modificato = new boolean[1];

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    AttendiFinestraVisibile(progress);
                    progress.Titolo("Ricalcolo prezzi in corso....");
                    //Pre-scarico a lotti, come il ricalcolo globale: riempie solo la cache
                    Prezzi.PreScaricaPrezzi(movimenti, 0, progress);

                    progress.SetLabel("Ricalcolo prezzi in corso....");
                    progress.SetMassimo(movimenti.size());
                    progress.SetAvanzamento(0);

                    //Nuovi prezzi raccolti a parte: i movimenti si toccano solo a operazione conclusa
                    Map<String[], String> nuoviPrezzi = new LinkedHashMap<>();
                    int i = 0;
                    for (String[] v : movimenti) {
                        if (Interrotto()) break;
                        i++;
                        progress.SetAvanzamento(i);
                        String pr = Prezzi.DammiPrezzoDaTransazione(v, 2);
                        if (pr == null) pr = "0.00";
                        if (!v[15].equals(pr) && !pr.equals("0.00")) nuoviPrezzi.put(v, pr);
                    }

                    if (Interrotto()) {
                        RipristinaKO(koCoinvolti);
                        //Chiusa con la X la finestra non c'è più e non può fare da genitore al messaggio
                        if (progress.isDisplayable()) {
                            Messaggi.InfoMessage("Ricalcola Prezzi", "Operazione annullata",
                                    "Nessun prezzo è stato modificato e i token senza prezzo sono tornati come prima.", progress);
                        }
                        return;
                    }

                    BigDecimal differenza = BigDecimal.ZERO;
                    BigDecimal differenzaRilevanti = BigDecimal.ZERO;
                    BigDecimal differenzaNuovi = BigDecimal.ZERO;
                    int rilevanti = 0;
                    int nuovi = 0;
                    for (Map.Entry<String[], String> e : nuoviPrezzi.entrySet()) {
                        String[] v = e.getKey();
                        BigDecimal delta = new BigDecimal(e.getValue()).subtract(new BigDecimal(v[15]));
                        if ("S".equals(v[33])) {
                            rilevanti++;
                            differenzaRilevanti = differenzaRilevanti.add(delta);
                        }
                        if (v[15].equals("0.00")) {
                            nuovi++;
                            differenzaNuovi = differenzaNuovi.add(delta);
                        }
                        differenza = differenza.add(delta);
                        v[15] = e.getValue();
                        //Stessa convenzione del ricalcolo globale: si svuota, non si scrive "SI"
                        v[32] = "";
                    }
                    modificato[0] = !nuoviPrezzi.isEmpty();

                    int senzaPrezzo = 0;
                    for (String[] v : movimenti) if (v[15].equals("0.00")) senzaPrezzo++;
                    String testo = "Sono stati modificati <b>" + nuoviPrezzi.size() + "</b> prezzi su " + movimenti.size()
                            + " movimenti, per una differenza totale di <b>€ " + differenza + "</b><br>"
                            + "di cui :<br>"
                            + " - <b>" + nuovi + "</b> sono relativi all'attribuzione di un prezzo a movimenti che prima non lo avevano, per un totale di <b>€ " + differenzaNuovi + "</b><br>"
                            + " - <b>" + rilevanti + "</b> sono relativi a movimenti fiscalmente rilevanti per un totale di <b>€ " + differenzaRilevanti + "</b><br>";
                    if (senzaPrezzo > 0) {
                        testo += "<br>Restano <b>" + senzaPrezzo + "</b> movimenti con prezzo ancora a zero: il prezzo non è stato trovato.";
                    }
                    Messaggi.SuccessMessage("Riepilogo", testo, progress);
                } catch (RuntimeException ex) {
                    LoggerGC.ScriviErrore(ex);
                    RipristinaKO(koCoinvolti);
                    Messaggi.WarningMessage("Ricalcola Prezzi", "Ricalcolo non completato",
                            "Si è verificato un errore: nessun prezzo è stato modificato.", progress);
                } finally {
                    progress.ChiudiFineLavoro();
                }
            }
        };

        //Lo scope dell'interruzione si apre qui, nel proprietario dell'operazione: senza, Richiesta() è
        //sempre false e il pre-scarico e la ricerca dei prezzi non vedrebbero mai il pulsante "Interrompi"
        Interruzione.Apri();
        try {
            thread.start();
            progress.setVisible(true);//blocca finché il thread non chiude la finestra
        } finally {
            Interruzione.Chiudi();
        }
        return modificato[0];
    }

    /**
     * Il thread parte prima che la finestra modale sia mostrata dall'EDT: se finisse (e la chiudesse) prima,
     * {@code setVisible(true)} arriverebbe a finestra già "chiusa" e resterebbe aperta per sempre.
     */
    private static void AttendiFinestraVisibile(Download progress) {
        try {
            for (int t = 0; t < 100 && !progress.isVisible(); t++) Thread.sleep(20);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /** L'utente ha premuto "Interrompi" o chiuso la finestra di avanzamento. */
    private static boolean Interrotto() {
        return Interruzione.Richiesta() || Principale.InterrompiCiclo;
    }
}
