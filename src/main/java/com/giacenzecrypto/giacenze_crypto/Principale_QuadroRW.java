package com.giacenzecrypto.giacenze_crypto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Logica operativa della tabella di sintesi del quadro W/RW ("RW_Tabella"), parte CRYPTO: dalle righe di
 * dettaglio del motore ({@code Principale.Mappa_RW_ListeXGruppoWallet}) ai righi del quadro, uno per gruppo
 * wallet e per tratto CRYPTO ({@link Calcoli_RW_PeriodiCrypto}). Estratta da {@code Principale.RW_CalcolaRW}
 * il 2026-09-26, quando i righi sono diventati per periodo: in {@code Principale} restano l'aggiunta alla
 * tabella e il totale dell'imposta.
 *
 * <p>Formato di un rigo ({@code String[10]}): [0] etichetta, [1] valore iniziale, [2] valore finale,
 * [3] giorni (media ponderata sul valore finale), [4] "ERRORI" o vuoto, [5] imposta, [6] chiave di sintesi
 * {@code "Wallet NN|CRYPTO|"} (rigo unico del gruppo) o {@code "Wallet NN|CRYPTO|inizio/fine"} (rigo di un
 * periodo), [7] "SI"/"NO" bollo pagato dall'intermediario, [8] natura "CRYPTO", [9] Stato estero (vuoto).</p>
 */
public class Principale_QuadroRW {

    private Principale_QuadroRW() {
    }

    /**
     * I righi CRYPTO del quadro.
     *
     * @param ListePerGruppo  gruppo → righe di dettaglio del motore. <b>Modificata</b>: nei tratti col bollo
     *                        pagato, con {@code MostraGiacenze}, le righe calcolate sono sostituite dalla
     *                        fotografia, che è ciò che la tabella di dettaglio deve poi mostrare
     * @param Fotografie      gruppo → giacenze di inizio/fine per tratto ({@code Funzioni.RW_GiacenzeInizioFineAnno})
     * @param Tratti          gruppo → tratti CRYPTO dell'anno
     * @param Alias           gruppo → riga di {@code GRUPPO_ALIAS} ({@code [1]} alias, {@code [2]} "S" = bollo)
     * @param InizioSuOrigine opzione "il valore iniziale va sul wallet di origine"
     * @param SoloValoriIniFin Rilevanza A
     * @param MostraGiacenze  opzione "per i wallet che pagano il bollo mostra solo giacenze"
     * @return chiave → rigo, in ordine case-insensitive (i righi di un gruppo spezzato in ordine di data)
     */
    public static Map<String, String[]> RighiCrypto(Map<String, List<String[]>> ListePerGruppo,
            Map<String, List<String[]>> Fotografie, Function<String, List<String[]>> Tratti,
            Function<String, String[]> Alias, boolean InizioSuOrigine, boolean SoloValoriIniFin, boolean MostraGiacenze) {
        Map<String, List<String[]>> CacheTratti = new HashMap<>();
        Function<String, List<String[]>> TrattiDi = g -> CacheTratti.computeIfAbsent(g, Tratti);
        Map<String, String[]> Quadro = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, String[]> TrattoXChiave = new HashMap<>();
        Map<String, String> GruppoXChiave = new HashMap<>();

        //1 - Tratti col bollo pagato dall'intermediario (con l'opzione "mostra solo giacenze") : le righe
        //calcolate lasciano il posto alla fotografia di inizio/fine tratto. Lo faccio prima di aggregare,
        //perche' i calcoli del motore servono fino a qui e la fotografia da qui in avanti.
        if (MostraGiacenze) {
            for (String key : new ArrayList<>(ListePerGruppo.keySet())) {
                List<String[]> tr = TrattiDi.apply(key);
                if (tr.stream().noneMatch(Principale_QuadroRW::TrattoConBollo)) continue;
                List<String[]> nuova = new ArrayList<>();
                for (String[] lista : ListePerGruppo.get(key)) {
                    if (!TrattoConBollo(TrattoDelRigo(tr, lista[9]))) nuova.add(lista);
                }
                List<String[]> foto = Fotografie == null ? null : Fotografie.get(key);
                if (foto != null) {
                    for (String[] lista : foto) {
                        if (TrattoConBollo(TrattoDelRigo(tr, lista[9]))) nuova.add(lista);
                    }
                }
                ListePerGruppo.put(key, nuova);
            }
        }

        //2 - Aggregazione delle righe di dettaglio nei righi del quadro, uno per gruppo e tratto
        for (String key : ListePerGruppo.keySet()) {
            //Questo serve solo per compilare anche i quadri sui wallet senza movimentazioni ne giacenze
            for (String[] t : TrattiDi.apply(key)) {
                Rigo(Quadro, TrattoXChiave, GruppoXChiave, key, TrattiDi.apply(key), t);
            }
            for (String[] lista : ListePerGruppo.get(key)) {
                //Un errore marca il rigo del suo tratto, non quelli dei tratti successivi del gruppo
                boolean ErroreRiga = lista[4].equals("0000-00-00 00:00") || lista[15].toLowerCase().contains("error");
                //Rigo del gruppo finale nel tratto della riga (lista[6]-> Gruppo Wallet Finale)
                List<String[]> TrattiFine = TrattiDi.apply(lista[6]);
                String[] TrattoFine = TrattoDelRigo(TrattiFine, lista[9]);
                String RW1[] = Rigo(Quadro, TrattoXChiave, GruppoXChiave, lista[6], TrattiFine, TrattoFine);
                boolean Fotografia = MostraGiacenze && TrattoConBollo(TrattoFine);
                //Se il wallet iniziale è diverso da quello finale e l'opzione "inizio su wallet origine" è attiva
                //il valore iniziale va al wallet iniziale (lista[1]), nel suo tratto che contiene la data di
                //origine del lotto (lista[4]). lista[1] è vuoto nelle righe di giacenza negativa.
                if ((!lista[1].equals(lista[6])) && InizioSuOrigine && !lista[1].isBlank()) {
                    List<String[]> TrattiOri = TrattiDi.apply(lista[1]);
                    String[] TrattoOri = TrattoDelRigo(TrattiOri, lista[4]);
                    //Se il tratto di origine è una fotografia (bollo pagato) non si somma nulla
                    if (!(MostraGiacenze && TrattoConBollo(TrattoOri))) {
                        String RW2[] = Rigo(Quadro, TrattoXChiave, GruppoXChiave, lista[1], TrattiOri, TrattoOri);
                        RW2[1] = new BigDecimal(lista[5]).add(new BigDecimal(RW2[1])).toPlainString();//RW2[1] è il valore iniziale
                    }
                } else {
                    RW1[1] = new BigDecimal(lista[5]).add(new BigDecimal(RW1[1])).toPlainString();//RW1[1] è il valore iniziale
                }

                RW1[2] = new BigDecimal(lista[10]).add(new BigDecimal(RW1[2])).toPlainString();
                if (ErroreRiga) RW1[4] = "ERRORI";
                if (Fotografia) {
                    //Fotografia inizio/fine tratto : i giorni sono quelli del tratto (o dall'apertura del wallet)
                    RW1[3] = new BigDecimal(lista[11]).setScale(2, RoundingMode.HALF_UP).toPlainString();
                } else {
                    //RW[6]=gg*prezzo+i precedenti gg* prezzo -> Serve per poi trovare i gg ponderati
                    RW1[6] = new BigDecimal(lista[10]).multiply(new BigDecimal(lista[11])).add(new BigDecimal(RW1[6])).toPlainString();
                    //se il valore finale è diverso da zero allora proseguo con il calcolo dei gg ponderati
                    if (new BigDecimal(RW1[2]).compareTo(new BigDecimal(0)) != 0) {
                        RW1[3] = new BigDecimal(RW1[6]).divide(new BigDecimal(RW1[2]), 2, RoundingMode.HALF_UP).toPlainString();
                    } else if (SoloValoriIniFin) {
                        RW1[3] = new BigDecimal(lista[11]).setScale(2, RoundingMode.HALF_UP).toPlainString();
                    }
                }
                //IC (365 fisso, decisione dell'utente anche per i righi di periodo)
                RW1[5] = new BigDecimal(RW1[2]).divide(new BigDecimal("365"), VarStatiche.DecimaliCalcoli + 10, RoundingMode.HALF_UP).multiply(new BigDecimal(RW1[3])).multiply(new BigDecimal("0.002")).setScale(2, RoundingMode.HALF_UP).toPlainString();
            }
        }

        //3 - Etichetta con l'alias, chiave di sintesi, bollo del tratto
        for (Map.Entry<String, String[]> Voce : Quadro.entrySet()) {
            String[] RWx = Voce.getValue();
            String Gruppo = GruppoXChiave.get(Voce.getKey());
            String[] Tratto = TrattoXChiave.get(Voce.getKey());
            boolean Spezzato = Voce.getKey().contains("|") && Tratto != null;
            String[] Valori = Alias.apply(Gruppo);
            String AliasGruppo = Valori == null ? null : Valori[1];
            String DateTratto = Spezzato
                    ? Tratto[Calcoli_RW_PeriodiCrypto.TC_DATA_INIZIO] + "/" + Tratto[Calcoli_RW_PeriodiCrypto.TC_DATA_FINE] : "";
            RWx[0] = RWx[0].split(" ")[0].trim() + " ( " + AliasGruppo + " )"
                    + (Spezzato ? " [" + DateTratto.replace("/", " / ") + "]" : "");
            RWx[6] = Gruppo + "|CRYPTO|" + DateTratto;
            //Bollo : quello del tratto (del periodo, o del gruppo fuori dai periodi), vedi Calcoli_RW_PeriodiCrypto
            boolean BolloPagato = Tratto != null ? TrattoConBollo(Tratto)
                    : Valori != null && "S".equalsIgnoreCase(Valori[2]);
            if (BolloPagato) {
                RWx[5] = "0.00";
            }
            RWx[7] = BolloPagato ? "SI" : "NO";
        }
        return Quadro;
    }

    /** {@code true} se nel tratto CRYPTO il bollo è pagato dall'intermediario. */
    static boolean TrattoConBollo(String[] tratto) {
        return tratto != null && Principale_GruppiWalletRW.BOLLO_SI.equals(tratto[Calcoli_RW_PeriodiCrypto.TC_BOLLO]);
    }

    /**
     * Il tratto CRYPTO che contiene la data (i primi 10 caratteri, ISO) di una riga di dettaglio. Una data
     * prima dell'anno (o vuota, come nelle righe di giacenza negativa) cade nel primo tratto, una dopo
     * nell'ultimo.
     */
    static String[] TrattoDelRigo(List<String[]> tratti, String data) {
        if (tratti == null || tratti.isEmpty()) return null;
        String giorno = data != null && data.length() >= 10 ? data.substring(0, 10) : "";
        String[] t = Calcoli_RW_PeriodiCrypto.trattoAllaData(tratti, giorno);
        if (t != null) return t;
        return giorno.compareTo(tratti.get(0)[Calcoli_RW_PeriodiCrypto.TC_DATA_INIZIO]) < 0
                ? tratti.get(0) : tratti.get(tratti.size() - 1);
    }

    /**
     * Il rigo del quadro (creato se manca) del gruppo nel tratto indicato. Chiave = nome del gruppo se il gruppo
     * ha un solo tratto nell'anno (il rigo di sempre), altrimenti "gruppo|data inizio tratto".
     */
    private static String[] Rigo(Map<String, String[]> Quadro, Map<String, String[]> TrattoXChiave,
            Map<String, String> GruppoXChiave, String Gruppo, List<String[]> Tratti, String[] Tratto) {
        String Chiave = Tratti == null || Tratti.size() <= 1 || Tratto == null
                ? Gruppo : Gruppo + "|" + Tratto[Calcoli_RW_PeriodiCrypto.TC_DATA_INIZIO];
        String[] RW1 = Quadro.get(Chiave);
        if (RW1 == null) {
            RW1 = new String[10];
            RW1[0] = Gruppo.split(" ").length > 1 ? Gruppo.split(" ")[1] + " (" + Gruppo + ")" : Gruppo;
            RW1[1] = "0.00";//Valore iniziale
            RW1[2] = "0.00";//Valore Finale
            RW1[3] = "0.00";//gg di Detenzione
            RW1[4] = "";    //Errori
            RW1[5] = "0.00";//IC Calcolata
            RW1[6] = "0.00";//gg*valore+gg2*valore2+..... (poi sovrascritto con la chiave di sintesi)
            RW1[7] = "NO";
            RW1[8] = "CRYPTO";//Natura del rigo
            RW1[9] = "";      //Codice Stato estero (solo righi FIAT)
            Quadro.put(Chiave, RW1);
            GruppoXChiave.put(Chiave, Gruppo);
            if (Tratto != null) TrattoXChiave.put(Chiave, Tratto);
        }
        return RW1;
    }
}
