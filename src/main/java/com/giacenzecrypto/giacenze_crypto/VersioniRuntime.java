package com.giacenzecrypto.giacenze_crypto;

import java.util.regex.Pattern;
import org.json.JSONObject;

/**
 * Le versioni di Node.js e di CCXT usate da {@link CcxtInterop}, lette da {@code config/varie/Runtime.json}.
 *
 * <p>Erano due costanti nel codice e un aggiornamento (il bug C14: uno script che usa un metodo assente dal CCXT già
 * installato) voleva una versione nuova del programma. Il file contiene <b>solo il numero</b>: gli indirizzi di
 * scaricamento restano nel codice, e il numero è controllato rigorosamente perché finisce in un percorso su disco, in un
 * indirizzo e nella riga di comando di npm. Nessuna copia nel codice: il default è il jar.
 *
 * <p>Nei pacchetti che portano Node e CCXT dentro ({@code NODE_ESTERNO}, flatpak e MSIX) le versioni non si usano mai:
 * quel codice esce prima di leggerle.
 */
final class VersioniRuntime {

    private VersioniRuntime() {}

    /** Nome del file in {@code config/varie/}, senza estensione. */
    static final String NOME = "Runtime";

    /** Node con la {@code v} davanti, come nell'indirizzo di nodejs.org/dist. */
    private static final Pattern NODE = Pattern.compile("v\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    private static final Pattern CCXT = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

    record Versioni(String node, String ccxt) {}

    private static volatile Versioni versioni;

    /** @return le versioni in uso; {@link IllegalStateException} se il file non è disponibile né su disco né nel jar */
    static Versioni versioni() {
        Versioni v = versioni;
        if (v == null) {
            v = MappeCausali.CaricaConRipiego(NOME, MappeCausali.Cartella.VARIE, VersioniRuntime::Interpreta);
            if (v == null) {
                throw new IllegalStateException("Versioni di Node e CCXT non disponibili: manca config/varie/" + NOME
                        + ".json, anche fra le risorse del programma");
            }
            versioni = v;
        }
        return v;
    }

    /** Rilegge il file, dopo l'aggiornamento in background. */
    static void Ricarica() {
        Versioni v = MappeCausali.CaricaConRipiego(NOME, MappeCausali.Cartella.VARIE, VersioniRuntime::Interpreta);
        if (v != null) versioni = v;
    }

    /** @return le versioni, oppure {@code null} se il contenuto non è valido (chiavi mancanti o numeri non nella forma attesa) */
    static Versioni Interpreta(String contenuto, String nome) {
        if (contenuto == null || contenuto.isBlank()) return null;
        try {
            JSONObject r = new JSONObject(contenuto);
            String node = r.getJSONObject("node").getString("versione").trim();
            String ccxt = r.getJSONObject("ccxt").getString("versione").trim();
            if (!NODE.matcher(node).matches() || !CCXT.matcher(ccxt).matches()) {
                LoggerGC.ScriviErrore("VersioniRuntime: versione non valida in " + nome + ".json (node " + node + ", ccxt " + ccxt + ")");
                return null;
            }
            return new Versioni(node, ccxt);
        } catch (Exception e) {
            LoggerGC.ScriviErrore("VersioniRuntime: " + nome + ".json non interpretabile : " + e);
            return null;
        }
    }
}
