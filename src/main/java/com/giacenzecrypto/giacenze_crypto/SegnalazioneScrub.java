package com.giacenzecrypto.giacenze_crypto;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redazione best-effort del testo di un log prima dell'invio come segnalazione (vedi
 * nocommit/Documentazione/Analisi_Segnalazioni_Log.md, Decisione 4, profilo 4A). Rimuove le
 * forme note di dato identificante: indirizzi on-chain, hash di transazione, path della home,
 * credenziali passate come argomento agli script Node, chiavi in query-string, email, IBAN.
 *
 * <p><b>Non &egrave; una garanzia.</b> Un formato non previsto passa: per questo l'anteprima nel
 * dialogo di invio &egrave; sempre modificabile e l'utente pu&ograve; cancellare a mano ci&ograve;
 * che non vuole spedire. La classe &egrave; volutamente una funzione pura di {@link String} a
 * {@link String}, testabile senza GUI (precedente: {@link DocumentiFonte#UrlSenzaChiave}).
 *
 * <p>Due criteri sono deliberatamente <i>conservativi</i> per non divorare contenuto diagnostico
 * legittimo: gli ID dei movimenti (<code>yyyyMMddHHmmss_n_XX</code>), gli <code>ordId</code>/
 * <code>billId</code> di OKX e gli ID Earn (<code>EARN-COIN-yyyymmdd</code>) devono
 * <b>sopravvivere</b> — sono spesso proprio ci&ograve; che serve vedere. Per questo il
 * riconoscimento base58 (indirizzi Solana) salta qualunque token che contenga <code>_</code> o
 * <code>-</code>.
 */
public final class SegnalazioneScrub {

    private static final String RIMOSSO_INDIRIZZO = "[indirizzo rimosso]";
    private static final String RIMOSSO_HASH = "[hash rimosso]";
    private static final String RIMOSSO_GENERICO = "[rimosso]";
    private static final String RIMOSSO_EMAIL = "[email rimossa]";
    private static final String RIMOSSO_IBAN = "[iban rimosso]";
    private static final String UTENTE = "utente";

    // Indirizzo EVM: 0x + 40 esadecimali. Il prefisso 0x lo rende inequivocabile.
    private static final Pattern EVM = Pattern.compile("0x[0-9a-fA-F]{40}\\b");
    // Hash a 64 esadecimali (transazioni EVM/Bitcoin). Cattura anche eventuali SHA-256 innocui:
    // redigerli fa perdere poca informazione, non redigere un hash di transazione ne perde molta.
    private static final Pattern HASH64 = Pattern.compile("\\b[0-9a-fA-F]{64}\\b");
    // Candidato base58 (Solana / chiavi): 32-44 caratteri dell'alfabeto base58. La decisione se
    // redigerlo &egrave; in codice (salta se contiene _ o -), non nella regex.
    private static final Pattern BASE58 = Pattern.compile("\\b[1-9A-HJ-NP-Za-km-z]{32,44}\\b");
    // Path della home: /home/<x>, /Users/<x>, C:\Users\<x>.
    private static final Pattern HOME_UNIX = Pattern.compile("(/(?:home|Users)/)[^/\\s\"']+");
    private static final Pattern HOME_WIN = Pattern.compile("([A-Za-z]:\\\\Users\\\\)[^\\\\/\\s\"']+");
    // Credenziale = parola chiave + separatore [:=] + valore. Il separatore &egrave; obbligatorio
    // (niente spazio nudo), cos&igrave; la sola parola "token" o "password" in mezzo a una frase
    // non scatta e non si divora testo diagnostico.
    private static final Pattern CREDENZIALE = Pattern.compile(
            "(?i)\\b(api[_-]?key|apikey|secret|passphrase|password|access[_-]?token|token|authorization)\\b"
            + "\\s*[:=]\\s*\"?([^\\s\"'&,;]{4,})");
    // Header/campo "Bearer <token>": qui il separatore &egrave; lo spazio, ma la parola chiave
    // "bearer" seguita da un token lungo &egrave; abbastanza specifica.
    private static final Pattern BEARER = Pattern.compile("(?i)\\bbearer\\s+([A-Za-z0-9._\\-]{10,})");
    // Chiave dentro una query-string (come DocumentiFonte.UrlSenzaChiave).
    private static final Pattern QUERY_CHIAVE = Pattern.compile(
            "(?i)([?&](?:api_?key|apikey|key|token|secret|access_token)=)[^&\\s\"']+");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]{2,}");
    private static final Pattern IBAN = Pattern.compile("\\b[A-Z]{2}\\d{2}[A-Z0-9]{10,30}\\b");

    private SegnalazioneScrub() {
    }

    /**
     * @param testo testo grezzo del log (o {@code null})
     * @return lo stesso testo con le forme identificanti note sostituite da segnaposto; stringa
     *         vuota se {@code testo} &egrave; {@code null}
     */
    public static String redigi(String testo) {
        if (testo == null) {
            return "";
        }
        String s = testo;
        s = EVM.matcher(s).replaceAll(RIMOSSO_INDIRIZZO);
        s = HASH64.matcher(s).replaceAll(RIMOSSO_HASH);
        s = redigiBase58(s);
        s = HOME_UNIX.matcher(s).replaceAll("$1" + UTENTE);
        s = HOME_WIN.matcher(s).replaceAll("$1" + UTENTE);
        s = CREDENZIALE.matcher(s).replaceAll("$1=" + RIMOSSO_GENERICO);
        s = BEARER.matcher(s).replaceAll("bearer " + RIMOSSO_GENERICO);
        s = QUERY_CHIAVE.matcher(s).replaceAll("$1" + RIMOSSO_GENERICO);
        s = EMAIL.matcher(s).replaceAll(RIMOSSO_EMAIL);
        s = IBAN.matcher(s).replaceAll(RIMOSSO_IBAN);
        return s;
    }

    /**
     * Sostituisce i candidati base58 di 32-44 caratteri, ma <b>solo</b> se non contengono
     * {@code _} o {@code -} (un ID di movimento o un ID Earn non &egrave; un indirizzo).
     */
    private static String redigiBase58(String s) {
        Matcher m = BASE58.matcher(s);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String tok = m.group();
            String sostituto = (tok.indexOf('_') >= 0 || tok.indexOf('-') >= 0) ? tok : RIMOSSO_INDIRIZZO;
            m.appendReplacement(out, Matcher.quoteReplacement(sostituto));
        }
        m.appendTail(out);
        return out.toString();
    }
}
