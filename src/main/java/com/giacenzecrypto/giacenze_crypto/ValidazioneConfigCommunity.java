package com.giacenzecrypto.giacenze_crypto;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * I controlli di sicurezza su una configurazione di importazione condivisa con la community: gli stessi del server
 * ({@code ServizioPrezzi/src/validaConfigurazione.js}), ripetuti qui perche' il programma non si fida nemmeno del
 * proprio server. Si applicano prima di un invio e a ogni file scaricato dal catalogo, prima di salvarlo.
 * Vedi {@code nocommit/Documentazione/Analisi_Configurazioni_Community.md}, "Minacce e difese".
 *
 * <p>Le regole che lo schema non esprime: dimensioni, separatore da un elenco fisso (un separatore di piu'
 * caratteri finisce in {@code String.split()} come espressione regolare: un file malevolo potrebbe bloccare il
 * programma), niente {@code <} nei testi mostrati (Swing interpreta come HTML un testo che comincia con
 * {@code <html>}), niente {@code centralizzato}, niente dati personali riconoscibili. Piu' quelle dello schema che
 * il programma sa gia' verificare: chiavi note ({@code CHIAVI_NOTE}), tipi interni noti, e una lettura completa con
 * {@link ImportazioneGenerica.ConfigurazioneImport#carica}.
 */
public final class ValidazioneConfigCommunity {

    static final int MAX_BYTE = 64 * 1024;
    static final int MAX_PROFONDITA = 6;
    static final int MAX_ELEMENTI = 1000;
    static final int MAX_TESTO = 500;
    static final int MAX_DESCRIZIONE = 2000;
    static final int MAX_COMMENTO = 4000;
    static final Set<String> SEPARATORI_AMMESSI = Set.of(",", ";", "\t", "|", "\\|");

    /** Nome e forma dei dati personali che non devono finire in un file pubblico */
    private record DatoPersonale(String nome, Pattern forma) {}

    private static final List<DatoPersonale> DATI_PERSONALI = List.of(
            new DatoPersonale("indirizzo EVM o hash di transazione", Pattern.compile("0x[0-9a-fA-F]{40}")),
            new DatoPersonale("indirizzo email", Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")),
            new DatoPersonale("IBAN", Pattern.compile("\\b[A-Z]{2}\\d{2}(?:\\s?[A-Z0-9]{4}){3,7}\\b")),
            new DatoPersonale("indirizzo Bitcoin",
                    Pattern.compile("\\b(?:bc1[ac-hj-np-z02-9]{20,80}|[13][a-km-zA-HJ-NP-Z1-9]{25,34})\\b")),
            //base58 lunga con cifre, maiuscole e minuscole (Solana e simili): esclude le parole normali
            new DatoPersonale("indirizzo in base58", Pattern.compile(
                    "\\b(?=[1-9A-HJ-NP-Za-km-z]*\\d)(?=[1-9A-HJ-NP-Za-km-z]*[a-z])(?=[1-9A-HJ-NP-Za-km-z]*[A-Z])"
                    + "[1-9A-HJ-NP-Za-km-z]{32,44}\\b")));

    /** Caratteri di controllo: il tab e' ammesso solo come separatore */
    private static final Pattern CONTROLLO = Pattern.compile("[\\x00-\\x08\\x0A-\\x1F\\x7F\\u2028\\u2029]");

    private ValidazioneConfigCommunity() {
    }

    /**
     * @param testo il JSON della configurazione
     * @return i problemi trovati, vuoto se la configurazione si puo' condividere o salvare
     */
    public static List<String> Controlla(String testo) {
        List<String> errori = new ArrayList<>();
        if (testo == null || testo.isBlank()) {
            errori.add("configurazione vuota");
            return errori;
        }
        if (testo.getBytes(StandardCharsets.UTF_8).length > MAX_BYTE) {
            errori.add("configurazione oltre " + MAX_BYTE + " byte");
            return errori;
        }
        JSONObject root;
        try {
            root = new JSONObject(testo);
        } catch (Exception ex) {
            errori.add("JSON non valido: " + ex.getMessage());
            return errori;
        }
        Struttura(root, "", 0, errori);
        if (!errori.isEmpty()) return errori;

        for (String k : ImportazioneGenerica.ConfigurazioneImport.ChiaviSconosciute(root)) {
            errori.add(k + ": opzione che il programma non conosce");
        }
        for (String t : ImportazioneGenerica.ConfigurazioneImport.TipiSconosciuti(root)) {
            errori.add(t + ": non e' un tipo di movimento del programma");
        }
        if (root.has("separatore") && !SEPARATORI_AMMESSI.contains(String.valueOf(root.opt("separatore")))) {
            errori.add("separatore non ammesso: solo , ; tabulazione o |");
        }
        if (root.optBoolean("centralizzato", false)) {
            errori.add("centralizzato non ammesso: riservato alle configurazioni distribuite con il programma");
        }
        if (errori.isEmpty()) {
            //Tipi sbagliati, regole incoerenti: li trova la lettura vera, che e' quella che usera' l'importazione
            Path tmp = null;
            try {
                tmp = Files.createTempFile("community-", ".json");
                Files.writeString(tmp, testo, StandardCharsets.UTF_8);
                ImportazioneGenerica.ConfigurazioneImport.carica(tmp.toString());
            } catch (Exception ex) {
                errori.add("la configurazione non si puo' usare: " + ex.getMessage());
            } finally {
                try {
                    if (tmp != null) Files.deleteIfExists(tmp);
                } catch (Exception ignora) {
                    //file temporaneo: lo pulira' il sistema
                }
            }
        }
        return errori;
    }

    /** Dimensioni, testi e dati personali su tutto l'albero, chiavi comprese */
    private static void Struttura(Object valore, String percorso, int profondita, List<String> errori) {
        if (errori.size() >= 20) return;
        if (profondita > MAX_PROFONDITA) {
            errori.add((percorso.isEmpty() ? "/" : percorso) + ": annidamento oltre " + MAX_PROFONDITA + " livelli");
            return;
        }
        if (valore instanceof JSONArray a) {
            if (a.length() > MAX_ELEMENTI) errori.add(percorso + ": oltre " + MAX_ELEMENTI + " elementi");
            for (int i = 0; i < a.length(); i++) Struttura(a.get(i), percorso + "[" + i + "]", profondita + 1, errori);
        } else if (valore instanceof JSONObject o) {
            if (o.length() > MAX_ELEMENTI) errori.add((percorso.isEmpty() ? "/" : percorso) + ": oltre " + MAX_ELEMENTI + " voci");
            for (String k : o.keySet()) {
                String sotto = percorso.isEmpty() ? k : percorso + "." + k;
                Testo(k, sotto + " (chiave)", Commento(sotto), false, errori);
                Struttura(o.get(k), sotto, profondita + 1, errori);
            }
        } else if (valore instanceof String s) {
            Testo(s, percorso, Commento(percorso), percorso.equals("separatore"), errori);
        }
    }

    /** I commenti (chiavi che cominciano con _) non li mostra mai nessuno: possono essere lunghi e contenere "<" */
    private static boolean Commento(String percorso) {
        int punto = percorso.lastIndexOf('.');
        return percorso.substring(punto + 1).startsWith("_");
    }

    private static void Testo(String s, String percorso, boolean commento, boolean tabAmmesso, List<String> errori) {
        int limite = commento ? MAX_COMMENTO : percorso.equals("descrizione") ? MAX_DESCRIZIONE : MAX_TESTO;
        if (s.length() > limite) errori.add(percorso + ": testo oltre " + limite + " caratteri");
        if (CONTROLLO.matcher(s).find() || (!tabAmmesso && s.indexOf('\t') >= 0)) {
            errori.add(percorso + ": caratteri di controllo non ammessi");
        }
        if (!commento && s.indexOf('<') >= 0) {
            errori.add(percorso + ": il carattere < non e' ammesso (il programma interpreterebbe l'HTML)");
        }
        for (DatoPersonale d : DATI_PERSONALI) {
            if (d.forma().matcher(s).find()) {
                errori.add(percorso + ": sembra contenere un " + d.nome() + ", da togliere prima di condividere");
            }
        }
    }
}
