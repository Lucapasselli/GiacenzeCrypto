package com.giacenzecrypto.giacenze_crypto;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Le configurazioni di importazione condivise con la community, dal lato del programma: cosa si puo' condividere,
 * il salvataggio di quelle scaricate in {@code config/community/} e l'elenco delle proprie proposte. Nessun campo
 * Swing: le finestre sono {@link GUI_CondividiConfigurazione} e {@link GUI_ConfigurazioniCommunity}.
 *
 * <p>Un file scaricato si salva solo se il suo SHA-256 e' quello del catalogo e se supera
 * {@link ValidazioneConfigCommunity}: il programma non si fida nemmeno del proprio server.
 */
public final class CommunityConfigurazioni {

    /** Prefisso dei file salvati: {@code community-<id>.json} */
    static final String PREFISSO_FILE = "community-";

    private CommunityConfigurazioni() {
    }

    /**
     * Si condivide solo una configurazione provata con successo: letta senza errori, nessuna riga scartata,
     * nessuna opzione o tipo sconosciuto, almeno un movimento.
     */
    public static boolean ProvaSenzaProblemi(ProvaConfigurazioneImport.Esito e) {
        return e != null && !e.ciSonoProblemi() && !e.movimenti.isEmpty();
    }

    /** L'esito della prova da allegare alla proposta: conteggi e riepilogo per causale, nessuna riga del CSV */
    public static JsonObject RiepilogoPerInvio(ProvaConfigurazioneImport.Esito e) {
        JsonObject r = new JsonObject();
        r.addProperty("righe", e.righeLette);
        r.addProperty("movimenti", e.movimenti.size());
        r.addProperty("scarti", e.scarti.size());
        JsonArray causali = new JsonArray();
        for (ProvaConfigurazioneImport.Riepilogo c : e.riepilogo) {
            if (causali.size() >= 60) break;
            causali.add(c.causali() + " | " + c.tipi() + " | " + c.gruppi() + " | " + c.testoCategorie());
        }
        r.add("causali", causali);
        return r;
    }

    /** @return lo SHA-256 esadecimale dei byte */
    static String Sha256(byte[] dati) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(dati));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** @return il file locale di una configurazione del catalogo (puo' non esistere) */
    public static File FileLocale(int id) {
        return new File(VarStatiche.getCartella_ConfigCommunity(), PREFISSO_FILE + id + ".json");
    }

    /**
     * Controlla e salva una configurazione scaricata.
     * @param voce la voce del catalogo, con lo SHA-256 atteso
     * @param dati i byte scaricati
     * @return il file salvato
     * @throws IOException se i byte non sono quelli del catalogo o la configurazione non supera i controlli
     */
    public static File SalvaScaricata(CommunityConfigClient.VoceCatalogo voce, byte[] dati) throws IOException {
        if (!Sha256(dati).equalsIgnoreCase(voce.sha256())) {
            throw new IOException("il file scaricato non corrisponde al catalogo (impronta SHA-256 diversa)");
        }
        String testo = new String(dati, StandardCharsets.UTF_8);
        List<String> errori = ValidazioneConfigCommunity.Controlla(testo);
        if (!errori.isEmpty()) {
            throw new IOException("il file non supera i controlli del programma: " + String.join("; ", errori));
        }
        File f = FileLocale(voce.id());
        Files.createDirectories(f.toPath().getParent());
        Path tmp = f.toPath().resolveSibling(f.getName() + ".tmp");
        Files.write(tmp, dati);
        Files.move(tmp, f.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return f;
    }

    /** @return vero se la configurazione locale e' identica a quella del catalogo */
    public static boolean Aggiornata(CommunityConfigClient.VoceCatalogo voce) {
        File f = FileLocale(voce.id());
        try {
            return f.isFile() && Sha256(Files.readAllBytes(f.toPath())).equalsIgnoreCase(voce.sha256());
        } catch (IOException ex) {
            return false;
        }
    }

    /** Elimina la copia locale. @return vero se c'era ed e' stata eliminata */
    public static boolean Elimina(int id) {
        return FileLocale(id).delete();
    }

    // ------------------------------------------------------------------ proposte inviate

    /** Una propria proposta, con il codice per ritirarla */
    public record PropostaInviata(int id, String nomeFile, String codice, long inviata) {
    }

    /**
     * L'elenco delle proprie proposte sta in un file della cartella di lavoro, non nelle opzioni (valori da 255
     * caratteri) e non in {@code config/community/} (sarebbe preso per una configurazione).
     */
    static File FileProposte() {
        return new File(VarStatiche.getWorkingDirectory() + "CommunityProposteInviate.json");
    }

    /** @return le proposte inviate da questa installazione, piu' recenti prima */
    public static List<PropostaInviata> ProposteInviate() {
        List<PropostaInviata> elenco = new ArrayList<>();
        File f = FileProposte();
        if (!f.isFile()) return elenco;
        try {
            JSONArray a = new JSONArray(Files.readString(f.toPath(), StandardCharsets.UTF_8));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                elenco.add(new PropostaInviata(o.getInt("id"), o.optString("nomeFile"), o.getString("codice"),
                        o.optLong("inviata")));
            }
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
        }
        elenco.sort((x, y) -> Long.compare(y.inviata(), x.inviata()));
        return elenco;
    }

    /** Ricorda una proposta appena inviata */
    public static void RegistraProposta(int id, String nomeFile, String codice) {
        List<PropostaInviata> elenco = ProposteInviate();
        elenco.removeIf(p -> p.id() == id);
        elenco.add(0, new PropostaInviata(id, nomeFile, codice, System.currentTimeMillis()));
        Scrivi(elenco);
    }

    /** Dimentica una proposta (ritirata) */
    public static void TogliProposta(int id) {
        List<PropostaInviata> elenco = ProposteInviate();
        if (elenco.removeIf(p -> p.id() == id)) Scrivi(elenco);
    }

    private static void Scrivi(List<PropostaInviata> elenco) {
        JSONArray a = new JSONArray();
        for (PropostaInviata p : elenco) {
            a.put(new JSONObject().put("id", p.id()).put("nomeFile", p.nomeFile()).put("codice", p.codice())
                    .put("inviata", p.inviata()));
        }
        try {
            Files.writeString(FileProposte().toPath(), a.toString(2), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }
}
