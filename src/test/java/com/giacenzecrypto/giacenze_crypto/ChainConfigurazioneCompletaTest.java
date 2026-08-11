package com.giacenzecrypto.giacenze_crypto;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifica che ogni blockchain dichiarata in {@link Principale#Mappa_ChainExplorer} sia agganciata in
 * <b>tutti</b> i punti che servono a farla funzionare davvero.
 *
 * <p>Aggiungere una chain non è una modifica sola: sono dieci punti sparsi in sei file, elencati in
 * {@code test/Documentazione/Analisi_Chain_Aggiungibili.md}. Quattro di questi <b>non danno nessun
 * errore</b> se dimenticati — la chain scarica, sembra funzionare, e sbaglia in un angolo:
 *
 * <ul>
 *   <li>senza {@code MappaRetiSupportate} la rete viene azzerata nel percorso prezzi e il prezzo per
 *       contratto non parte;</li>
 *   <li>senza la voce in {@code Importazioni_Gestione.BlockChain} la chain non è scegliibile
 *       nell'importazione;</li>
 *   <li>senza la voce in {@code GeneraLoghi.PIATTAFORMA_CHAIN} i token di quella chain restano senza
 *       logo, perché il generatore non sa da dove scaricarlo;</li>
 *   <li>senza la voce in {@code Principale.GOPLUS_CHAIN_ID} il controllo scam considera la rete non
 *       supportata e <b>non verifica nulla, in silenzio</b>.</li>
 * </ul>
 *
 * <p>Questo test trasforma quei quattro silenzi in un fallimento. L'unica eccezione dichiarata è
 * proprio GoPlus: non tutte le chain sono coperte dal suo servizio, quindi lì si controlla solo la
 * direzione opposta (nessuna voce inventata, e il chain id coerente con quello dell'endpoint).
 */
public class ChainConfigurazioneCompletaTest {

    private static final Path SORGENTE_COMBO =
            Path.of("src/main/java/com/giacenzecrypto/giacenze_crypto/GUI_GestioneWallets.java");
    private static final Path FORM_COMBO =
            Path.of("src/main/java/com/giacenzecrypto/giacenze_crypto/GUI_GestioneWallets.form");

    @BeforeAll
    public static void CompilaLeMappe() {
        VarCondivise.CompilaMappaChain();
        VarCondivise.CompilaMappaRetiSupportate();
    }

    /**
     * Codice rete → etichetta completa, ricavato dall'elenco della finestra di importazione. È la
     * stessa regola con cui {@link LoghiImport#MappaLoghiRete} associa il codice salvato nei dati
     * all'etichetta da cui prende il nome il logo.
     */
    private static Map<String, String> EtichettePerCodice(String[] voci) {
        Map<String, String> perCodice = new LinkedHashMap<>();
        Pattern p = Pattern.compile("\\(([A-Z0-9]+)\\)\\s*$");
        for (String voce : voci) {
            Matcher m = p.matcher(voce.trim());
            if (m.find()) perCodice.putIfAbsent(m.group(1), voce.trim());
        }
        return perCodice;
    }

    /** Le reti che hanno un explorer integrato, cioè quelle di cui si occupa questo test. */
    private static Set<String> RetiConExplorer() {
        return new LinkedHashSet<>(Principale.Mappa_ChainExplorer.keySet());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> MappaPrivata(Class<?> classe, String nomeCampo) throws Exception {
        Field f = classe.getDeclaredField(nomeCampo);
        f.setAccessible(true);
        return (Map<String, String>) f.get(null);
    }

    /**
     * Chain id letto dall'URL dell'explorer, quando è un endpoint Etherscan V2. Le istanze Blockscout
     * e gli explorer propri (Cronos, Solana, mempool.space) non lo espongono: lì non c'è niente da
     * confrontare e si restituisce {@code null}.
     */
    private static String ChainIdDaUrl(String url) {
        Matcher m = Pattern.compile("[?&]chainid=(\\d+)").matcher(url);
        return m.find() ? m.group(1) : null;
    }

    private static List<String> StringheDiUnArrayJava(String sorgente, String ancora) {
        int inizio = sorgente.indexOf(ancora);
        assertTrue(inizio >= 0, "in " + SORGENTE_COMBO.getFileName() + " non c'è più il blocco " + ancora);
        int fine = sorgente.indexOf("}", inizio);
        assertTrue(fine > inizio, "il blocco " + ancora + " non risulta chiuso");

        List<String> valori = new ArrayList<>();
        Matcher m = Pattern.compile("\"([^\"]*)\"").matcher(sorgente.substring(inizio, fine));
        while (m.find()) valori.add(m.group(1));
        return valori;
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void OgniChainConExplorerEUnaReteSupportata() {
        for (String rete : RetiConExplorer()) {
            assertTrue(Principale.MappaRetiSupportate.containsKey(rete),
                    "la rete " + rete + " ha un explorer ma non è in MappaRetiSupportate: il percorso "
                    + "prezzi la azzererebbe e il prezzo per contratto non partirebbe");
        }
    }

    @Test
    public void OgniChainEvmAccettaUnWalletDelSuoFormato() {
        //Un indirizzo EVM valido qualunque: quello che si controlla è la rete, non l'indirizzo
        String indirizzo = "0x0000000000000000000000000000000000000001";
        for (String rete : RetiConExplorer()) {
            if (rete.equals("SOL") || rete.equals("BTC")) continue; //non EVM, formato indirizzo proprio
            assertTrue(Funzioni_WalletDeFi.isValidDefiWallet(indirizzo + " (" + rete + ")"),
                    "isValidDefiWallet rifiuta i wallet sulla rete " + rete + ": manca dalla stringa "
                    + "RetiSupportate e la chain non sarebbe inseribile");
        }
    }

    @Test
    public void OgniChainHaIlLinkAllExplorerDelleTransazioni() {
        for (String rete : RetiConExplorer()) {
            String url = Funzioni_WalletDeFi.UrlExplorerTx(rete, "0xabc");
            assertNotNull(url, "UrlExplorerTx non conosce la rete " + rete + ": il link alla transazione "
                    + "sparirebbe, anche nella domanda di \"Chiedi a IA\"");
            assertTrue(url.startsWith("http"), "l'URL dell'explorer di " + rete + " non è un indirizzo web: " + url);
        }
    }

    @Test
    public void OgniChainESceglibileNellaFinestraDiImportazione() {
        Map<String, String> perCodice = EtichettePerCodice(Importazioni_Gestione.BlockChain);
        for (String rete : RetiConExplorer()) {
            assertTrue(perCodice.containsKey(rete),
                    "la rete " + rete + " non è nell'elenco Importazioni_Gestione.BlockChain: non si "
                    + "potrebbe scegliere in importazione");
        }
    }

    @Test
    public void LeDueTendineDelleRetiDiconoLaStessaCosa() throws IOException {
        String sorgente = Files.readString(SORGENTE_COMBO, StandardCharsets.UTF_8);
        List<String> daJava = StringheDiUnArrayJava(sorgente, "ComboBox_Rete.setModel(");

        String form = Files.readString(FORM_COMBO, StandardCharsets.UTF_8);
        int inizio = form.indexOf("<Component class=\"javax.swing.JComboBox\" name=\"ComboBox_Rete\">");
        assertTrue(inizio >= 0, "in GUI_GestioneWallets.form non c'è più il ComboBox_Rete");
        int fine = form.indexOf("</Component>", inizio);
        String blocco = form.substring(inizio, fine);

        List<String> daForm = new ArrayList<>();
        Matcher m = Pattern.compile("<StringItem index=\"\\d+\" value=\"([^\"]*)\"").matcher(blocco);
        while (m.find()) daForm.add(m.group(1));

        //Il .form è la sorgente di verità del Designer: se le due liste divergono, la prossima
        //riapertura in NetBeans rigenera initComponents() e le modifiche fatte a mano spariscono
        assertEquals(daForm, daJava, "l'elenco delle reti nel .form e quello in initComponents() non coincidono");

        Matcher c = Pattern.compile("<StringArray count=\"(\\d+)\"").matcher(blocco);
        assertTrue(c.find(), "manca l'attributo count dello StringArray delle reti");
        assertEquals(daForm.size(), Integer.parseInt(c.group(1)),
                "l'attributo count dello StringArray non corrisponde al numero di voci");
    }

    @Test
    public void OgniChainESceglibileAllaCreazioneDelWallet() throws IOException {
        String sorgente = Files.readString(SORGENTE_COMBO, StandardCharsets.UTF_8);
        Map<String, String> perCodice = EtichettePerCodice(
                StringheDiUnArrayJava(sorgente, "ComboBox_Rete.setModel(").toArray(String[]::new));

        for (String rete : RetiConExplorer()) {
            assertTrue(perCodice.containsKey(rete),
                    "la rete " + rete + " non è nella tendina di GUI_GestioneWallets: non si potrebbe "
                    + "creare un wallet su quella chain");
        }
    }

    @Test
    public void LEtichettaDiUnaReteEIdenticaOvunque() throws Exception {
        Map<String, String> daImport = EtichettePerCodice(Importazioni_Gestione.BlockChain);
        String sorgente = Files.readString(SORGENTE_COMBO, StandardCharsets.UTF_8);
        Map<String, String> daWallet = EtichettePerCodice(
                StringheDiUnArrayJava(sorgente, "ComboBox_Rete.setModel(").toArray(String[]::new));

        //L'etichetta non è una didascalia: è la chiave con cui GeneraLoghi trova la piattaforma e con
        //cui LoghiImport ricava il nome del file del logo. Due grafie diverse compilano entrambe e
        //fanno sparire il logo senza dire niente
        for (String rete : RetiConExplorer()) {
            String a = daImport.get(rete);
            String b = daWallet.get(rete);
            if (a == null || b == null) continue; //assenze già segnalate dai test dedicati
            assertEquals(a, b, "la rete " + rete + " è scritta in due modi diversi nelle due tendine");
        }
    }

    @Test
    public void OgniChainSaDoveProcurarsiILoghi() throws Exception {
        Map<String, String> piattaforme = MappaPrivata(GeneraLoghi.class, "PIATTAFORMA_CHAIN");
        Map<String, String> monete = MappaPrivata(GeneraLoghi.class, "MONETA_CHAIN");
        Map<String, String> perCodice = EtichettePerCodice(Importazioni_Gestione.BlockChain);

        for (String rete : RetiConExplorer()) {
            String etichetta = perCodice.get(rete);
            if (etichetta == null) continue; //assenza già segnalata dal test dedicato
            assertTrue(piattaforme.containsKey(etichetta) || monete.containsKey(etichetta),
                    "GeneraLoghi non sa da dove scaricare il logo di " + etichetta + ": va aggiunta a "
                    + "PIATTAFORMA_CHAIN (chain con token) o a MONETA_CHAIN (solo moneta nativa)");
        }
    }

    @Test
    public void IlControlloScamNonInventaChainNeSbagliaChainId() throws Exception {
        Map<String, String> goplus = MappaPrivata(Principale.class, "GOPLUS_CHAIN_ID");

        for (Map.Entry<String, String> e : goplus.entrySet()) {
            String rete = e.getKey();
            String[] explorer = Principale.Mappa_ChainExplorer.get(rete);
            assertNotNull(explorer, "GOPLUS_CHAIN_ID cita la rete " + rete + ", che non ha nessun "
                    + "explorer configurato: o è un residuo o manca la chain in Mappa_ChainExplorer");

            //Dove l'endpoint è Etherscan V2 il chain id è scritto due volte: devono coincidere,
            //altrimenti si verificherebbe un token su una chain diversa da quella da cui proviene
            String daUrl = ChainIdDaUrl(explorer[0]);
            if (daUrl != null) {
                assertEquals(daUrl, e.getValue(), "per la rete " + rete + " il chain id di GoPlus non "
                        + "corrisponde a quello dell'endpoint dell'explorer");
            }
        }
    }
}
