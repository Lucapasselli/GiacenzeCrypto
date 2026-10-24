package com.giacenzecrypto.giacenze_crypto;

import static com.giacenzecrypto.giacenze_crypto.Principale.MappaCryptoWallet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Le scelte di "Cambia Tipologia Reward" (menu contestuale dei movimenti), tutte sull'intera selezione.
 *
 * <p><b>Tipologia del provento</b> (REWARD, STAKING REWARD, EARN, CASHBACK, AIRDROP): cambia il campo 5, che
 * {@link Funzioni#RewardRilevante} legge per decidere, secondo le opzioni dei proventi, se la reward genera
 * plusvalenza. Vale solo per i proventi ({@link #isReward}); un rimborso di reward conserva il prefisso "RIMBORSO ".
 * Fino al 2026-10-25 la scelta toccava solo la prima riga selezionata anche con più righe selezionate, e su un
 * rimborso cancellava il prefisso.
 *
 * <p><b>Deposito da classificare</b>: riporta un provento importato (categoria {@code RW}) a deposito crypto non
 * classificato ({@code DC}), che poi si classifica come qualunque altro deposito, anche come scambio differito.
 * Serve quando l'exchange registra come provento quello che provento non è. Caso d'origine: lo swap BTT → BTTC
 * del 24/01/2022 su Binance, dove l'uscita è "Token Swap - Redenomination/Rebranding" e l'entrata, tre ore dopo,
 * "Airdrop Assets", cioè REWARD. La finestra dello scambio differito propone solo depositi DC/DF, quindi il BTTC
 * non compariva. Un abbinamento automatico all'importazione è stato scartato (decisione dell'utente): "Airdrop
 * Assets" porta anche airdrop veri. Il movimento cambia ID (categoria {@code DC}), descrizione e campo 18, e perde
 * i campi calcolati dal motore. Tutto il resto resta: prezzo, documento di origine, causale originale, lignaggio
 * dello storico (con una voce nuova). Il verso opposto non serve: un DC si classifica già come reward da
 * "Classifica Movimento".
 *
 * <p>Segue lo schema delle altre estrazioni: nessun campo Swing, chi chiama aggiorna le tabelle.
 */
public final class Principale_TipologiaReward {

    /** Etichette delle operazioni nello storico modifiche (tradotte in {@code GUI_StoricoMovimento}). */
    static final String OP_REWARD_IN_DEPOSITO = "RewardInDeposito";
    static final String OP_TIPOLOGIA_REWARD = "CambiaTipologiaReward";

    /** Le tipologie di provento che si possono scegliere, nell'ordine del dialogo. */
    public static final List<String> TIPOLOGIE = List.of("REWARD", "STAKING REWARD", "EARN", "CASHBACK", "AIRDROP");

    /**
     * Output del motore delle plusvalenze, da svuotare: lo stesso elenco di {@code Principale_TraslaOrario} più il
     * 17, che su un provento non e' mai un valore inserito a mano (lo e' solo sui DDO).
     */
    private static final int[] CampiMotoreDaSvuotare = {16, 17, 19, 31, 33, 38};

    private Principale_TipologiaReward() {}

    /**
     * Il movimento è un provento: categoria {@code RW} (importato o generato) oppure un deposito classificato come
     * provento ({@code DAI} nel campo 18). È la condizione che abilita "Cambia Tipologia Reward" nel menu
     * ({@code Funzioni.PopUpMenu}) e quella con cui {@link Funzioni#RewardRilevante} applica le opzioni dei proventi.
     * @param v il movimento, anche {@code null}
     * @return {@code true} se è un provento
     */
    public static boolean isReward(String[] v) {
        if (v == null) return false;
        String Parti[] = v[0].split("_");
        return (Parti.length >= 5 && Parti[4].equalsIgnoreCase("RW")) || (v[18] != null && v[18].contains("DAI -"));
    }

    /**
     * Cambia la tipologia dei proventi indicati, saltando i movimenti che non lo sono. Nessun dialogo: chi chiama
     * mostra gli scarti e ricalcola una volta sola alla fine.
     * @param IDs ID dei movimenti selezionati
     * @param Tipologia una di {@link #TIPOLOGIE}
     * @return per ogni ID non modificato il motivo; vuota se sono stati modificati tutti
     */
    public static Map<String, String> CambiaTipologia(Collection<String> IDs, String Tipologia) {
        if (!TIPOLOGIE.contains(Tipologia)) throw new IllegalArgumentException("Tipologia di provento sconosciuta: " + Tipologia);
        Map<String, String> Scartati = new LinkedHashMap<>();
        List<String> Modificati = new ArrayList<>();
        for (String ID : IDs) {
            String v[] = MappaCryptoWallet.get(ID);
            if (!isReward(v)) {
                Scartati.put(ID, v == null ? "movimento non trovato" : "non è un provento (reward)");
                continue;
            }
            //Un rimborso (reward in uscita) resta riconoscibile come tale, come quando lo crea creaMovimento
            boolean Rimborso = Funzioni.noData(v[11]) || !Funzioni.isBigDecimalNonZero(v[13]);
            String Campo5 = Rimborso ? "RIMBORSO " + Tipologia : Tipologia;
            if (Campo5.equals(v[5])) continue;
            String Prima = Importazioni.SerializzaRiga(v);
            v[5] = Campo5;
            MovimentiStorico.AccodaModifica(MovimentiStorico.AssicuraLignaggio(v), ID, ID, Prima, OP_TIPOLOGIA_REWARD);
            Modificati.add(ID);
        }
        if (!Modificati.isEmpty()) LoggerGC.logInfo("Tipologia " + Tipologia + " assegnata a: " + String.join(", ", Modificati));
        return Scartati;
    }

    /**
     * @param v il movimento
     * @return {@code null} se il movimento si può riportare a deposito da classificare, altrimenti il motivo
     */
    static String MotivoNonConvertibile(String[] v) {
        if (v == null) return "movimento non trovato";
        String Parti[] = v[0].split("_");
        if (Parti.length < 5 || !Parti[4].equalsIgnoreCase("RW")) return "non è un provento (reward)";
        if ("AU".equalsIgnoreCase(v[22])) return "è generato da una classificazione, va annullata quella";
        if (!Funzioni.noData(v[20])) return "è collegato ad altri movimenti";
        if (Funzioni.isBigDecimalNonZero(v[10]) || !Funzioni.isBigDecimalNonZero(v[13]) || Funzioni.noData(v[11])) return "non è un movimento in sola entrata";
        if (!"DC".equals(Importazioni.RitornaTipologiaTransazione("", v[12], 0))) return "la moneta ricevuta non è una cripto-attività";
        return null;
    }

    /**
     * Il movimento riportato a deposito da classificare, senza toccare la mappa. L'ID cambia solo nella categoria e
     * va reso univoco da chi lo inserisce.
     * @param v il provento, convertibile ({@link #MotivoNonConvertibile} nullo)
     * @return una copia del movimento con categoria {@code DC}
     */
    static String[] Converti(String[] v) {
        String Nuovo[] = v.clone();
        String Parti[] = v[0].split("_");
        Parti[4] = "DC";
        Nuovo[0] = String.join("_", Parti);
        Nuovo[5] = Importazioni.RitornaTipologiaTransazione("", v[12], 1);
        Nuovo[18] = "";
        for (int Campo : CampiMotoreDaSvuotare) Nuovo[Campo] = "";
        return Nuovo;
    }

    /**
     * Riporta a deposito da classificare i proventi indicati, saltando quelli che non si possono convertire. Nessun
     * dialogo: chi chiama mostra gli scarti e ricalcola una volta sola alla fine.
     * @param IDs ID dei movimenti selezionati
     * @return per ogni ID non convertito il motivo; vuota se sono stati convertiti tutti
     */
    public static Map<String, String> ConvertiInDepositi(Collection<String> IDs) {
        Map<String, String> Scartati = new LinkedHashMap<>();
        List<String> Convertiti = new ArrayList<>();
        for (String ID : IDs) {
            String v[] = MappaCryptoWallet.get(ID);
            String Motivo = MotivoNonConvertibile(v);
            if (Motivo != null) {
                Scartati.put(ID, Motivo);
                continue;
            }
            String Nuovo[] = Converti(v);
            MappaCryptoWallet.remove(ID);
            String IDUnivoco = MovimentiCrypto.getIDUnivoco(MappaCryptoWallet, Nuovo[0]);
            if (IDUnivoco == null) {
                MappaCryptoWallet.put(ID, v);
                Scartati.put(ID, "impossibile generare un ID univoco");
                continue;
            }
            Nuovo[0] = IDUnivoco;
            MappaCryptoWallet.put(IDUnivoco, Nuovo);
            String Lignaggio = MovimentiStorico.AssicuraLignaggio(Nuovo);
            MovimentiStorico.AccodaModifica(Lignaggio, IDUnivoco, ID, Importazioni.SerializzaRiga(v), OP_REWARD_IN_DEPOSITO);
            Convertiti.add(ID + " -> " + IDUnivoco);
        }
        if (!Convertiti.isEmpty()) LoggerGC.logInfo("Proventi riportati a deposito da classificare: " + String.join(", ", Convertiti));
        return Scartati;
    }
}
