package com.giacenzecrypto.giacenze_crypto;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Tabella dei valori dei contratti derivati OKX ({@code ctVal}) che l'endpoint pubblico non dà più, da
 * {@code config/varie/ValoriContratti.json}: i futures a scadenza già scaduti (OKX elenca solo i contratti quotati) e i
 * valori di prima di un cambio (marzo 2020, contratti in USDT moltiplicati per 100 o 1.000).
 *
 * <p>Perché non "un contratto quotato con lo stesso sottostante": le famiglie nuove hanno valori diversi
 * ({@code ETH-USD_UM} 0,001 ETH contro 0,1 ETH di {@code ETH-USDT-SWAP}) e i futures in USDT non sono più quotati
 * (verificato il 17/11/2026, {@code Decisioni_Derivati.md}). Una regola vale per <b>famiglia</b> (l'{@code instId} senza
 * l'ultimo segmento: {@code BTC-USDT-250328} → {@code BTC-USDT}, tipo FUTURES) e tipo, con un periodo facoltativo sul
 * giorno del fill ({@code dal} compreso, {@code al} escluso), e dà un {@code valore} oppure {@code comeSwap}: il valore del
 * perpetuo della stessa famiglia, solo se OKX non elenca il futures.</p>
 *
 * <p>Cambiare un valore cambia i redditi da derivati senza una versione, come gli alias dei prezzi. Lo protegge il controllo
 * per posizione di {@link DerivatiLifo}: un valore sbagliato non riproduce il profitto dell'exchange e quella posizione
 * resta al profitto dell'exchange. Nessun ripiego nel codice: il default è la copia nel jar.</p>
 */
final class ValoriContratti {

    private ValoriContratti() {}

    static final String NOME = "ValoriContratti";

    /** Una riga della tabella. {@code valore} null vuol dire {@code comeSwap}. */
    record Regola(String famiglia, String tipo, String dal, String al, String valore) {

        boolean comeSwap() {
            return valore == null;
        }

        boolean vale(String fam, String t, String giorno) {
            if (!tipo.equals(t)) return false;
            if (famiglia.startsWith("*")) {
                String suffisso = famiglia.substring(1);
                if (!fam.endsWith(suffisso) || fam.length() <= suffisso.length()) return false;
            } else if (!famiglia.equals(fam)) {
                return false;
            }
            if (dal == null && al == null) return true;
            if (giorno == null) return false;   //regola con un periodo e fill senza data: non si sa se vale
            return (dal == null || giorno.compareTo(dal) >= 0) && (al == null || giorno.compareTo(al) < 0);
        }
    }

    /** Famiglia e tipo di un {@code instId}: {@code null} se non è un perpetuo ({@code -SWAP}) né un futures (data di 6 cifre). */
    static String[] FamigliaETipo(String instId) {
        if (instId == null) return null;
        int i = instId.lastIndexOf('-');
        if (i <= 0) return null;
        String ultimo = instId.substring(i + 1);
        String famiglia = instId.substring(0, i);
        if ("SWAP".equals(ultimo)) return new String[]{famiglia, "SWAP"};
        if (ultimo.matches("\\d{6}")) return new String[]{famiglia, "FUTURES"};
        return null;
    }

    private static volatile List<Regola> regole;

    /** La prima regola che vale per il contratto in quel giorno ({@code yyyy-MM-dd}, può essere null), oppure null. */
    static Regola Cerca(String instId, String giorno) {
        String[] ft = FamigliaETipo(instId);
        if (ft == null) return null;
        List<Regola> r = regole;
        if (r == null) {
            r = Carica();
            regole = r;
        }
        for (Regola x : r) {
            if (x.vale(ft[0], ft[1], giorno)) return x;
        }
        return null;
    }

    static void Ricarica() {
        regole = Carica();
    }

    /** Solo per i test: sostituisce la tabella ({@code List.of()} = nessuna regola). */
    static void ImpostaPerTest(List<Regola> r) {
        regole = r;
    }

    private static List<Regola> Carica() {
        List<Regola> r = MappeCausali.CaricaConRipiego(NOME, MappeCausali.Cartella.VARIE, ValoriContratti::Interpreta);
        return r != null ? r : List.of();
    }

    /** @return le regole OKX, oppure {@code null} se il file non è valido (allora vale la copia nel jar) */
    static List<Regola> Interpreta(String contenuto, String nome) {
        if (contenuto == null || contenuto.isBlank()) return null;
        try {
            JSONArray a = new JSONObject(contenuto).getJSONArray("okx");
            List<Regola> out = new ArrayList<>();
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                String famiglia = o.getString("famiglia").trim();
                String tipo = o.getString("tipo").trim();
                String dal = Giorno(o.optString("dal", ""));
                String al = Giorno(o.optString("al", ""));
                String valore = o.optString("valore", "").trim();
                boolean comeSwap = o.optBoolean("comeSwap", false);
                boolean valido = !famiglia.isEmpty() && ("FUTURES".equals(tipo) || "SWAP".equals(tipo))
                        && (o.optString("dal", "").isBlank() || dal != null) && (o.optString("al", "").isBlank() || al != null)
                        && (comeSwap ? valore.isEmpty() && "FUTURES".equals(tipo)
                                : Funzioni.isNumeric(valore, false) && new java.math.BigDecimal(valore).signum() > 0);
                if (!valido) {
                    LoggerGC.ScriviErrore("ValoriContratti: regola non valida in " + nome + ".json: " + o);
                    return null;
                }
                out.add(new Regola(famiglia, tipo, dal, al, comeSwap ? null : valore));
            }
            return List.copyOf(out);
        } catch (Exception e) {
            LoggerGC.ScriviErrore("ValoriContratti: " + nome + ".json non interpretabile: " + e);
            return null;
        }
    }

    /** Il giorno {@code yyyy-MM-dd} di una data del programma ({@code yyyy-MM-dd HH:mm[:ss]}, anche {@code yy-MM-dd}), oppure null. */
    static String Giorno(String data) {
        if (data == null) return null;
        String d = data.trim();
        if (d.length() > 2 && d.charAt(2) == '-') d = "20" + d;
        if (d.length() < 10) return null;
        d = d.substring(0, 10);
        return d.matches("\\d{4}-\\d{2}-\\d{2}") ? d : null;
    }
}
