package com.giacenzecrypto.giacenze_crypto;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author lucap
 */
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Component;
import java.awt.Cursor;
import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.*;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.json.JSONObject;

public class CcxtInterop {
    
    public static final String NODE_VERSION = "v24.7.0";
    public static final Path NODE_DIR = Paths.get(VarStatiche.getWorkingDirectory()+"tools", "node").toAbsolutePath().normalize();;

    /**
     * Giorni di storico oltre i quali lo scaricamento OKX via API non è più affidabile. Corrisponde a quanto
     * ha effettivamente restituito {@code /api/v5/asset/bills} sui dati reali (una sola pagina, ferma a poco
     * meno di un mese), che è il più corto dei due endpoint interrogati.
     */
    public static final int GIORNI_STORICO_OKX = 30;

    /**
     * Nome dell'opzione (in {@code personale.mv.db}) in cui viene ricordato il dominio regionale di OKX
     * su cui la chiave API dell'utente esiste davvero. Vedi
     * {@link #fetchMovimento(String, String, String, long, String, String, String, String)}.
     */
    public static final String OPZIONE_HOSTNAME_OKX = "OKX_Hostname";

    /**
     * Primo anno coperto dall'archivio storico di OKX: i dati partono dal 1° febbraio 2021, quindi 2021 Q1
     * è il trimestre più vecchio che abbia senso chiedere.
     */
    public static final int ANNO_MIN_ARCHIVIO_OKX = 2021;

    /**
     * Nome dell'opzione (in {@code personale.mv.db}) in cui viene ricordato l'anno da cui l'utente vuole che
     * parta il recupero dell'archivio storico di OKX.
     *
     * <p>Non e' una preferenza estetica: la richiesta di generazione di un trimestre e' l'operazione piu'
     * limitata di tutta l'integrazione (CCXT le assegna un costo che si traduce in oltre due ore fra due
     * richieste), e ogni trimestre in meno e' una richiesta risparmiata. Un conto aperto nel 2025 non ha
     * motivo di far chiedere a OKX i 16 trimestri che vanno dal 2021 in poi.
     */
    public static final String OPZIONE_ANNO_ARCHIVIO_OKX = "OKX_AnnoInizioArchivio";

    /**
     * Nome dell'opzione (in {@code personale.mv.db}) che ricorda se la scelta sullo storico e' gia' stata
     * proposta almeno una volta.
     *
     * <p>Serve a non far dipendere l'unico accesso a quella scelta dall'eta' dei dati. Il dialogo si apre
     * quando lo scaricamento partirebbe da piu' di {@value #GIORNI_STORICO_OKX} giorni fa, il che va bene per
     * chi non aggiorna da un po', ma taglia fuori proprio il caso per cui l'archivio esiste: chi ha importato
     * solo una parte della propria storia e ha quindi movimenti <b>recenti</b> e un buco dietro. Quell'utente
     * non vedrebbe mai il dialogo, e senza dialogo non c'e' modo di spostare indietro la data ne' di chiedere
     * l'archivio. Proponendolo comunque la prima volta, la scelta gli viene almeno offerta.
     */
    public static final String OPZIONE_ARCHIVIO_PROPOSTO_OKX = "OKX_ArchivioProposto";

    /**
     * Valore scritto in {@link #OPZIONE_ARCHIVIO_PROPOSTO_OKX} quando la scelta e' stata proposta.
     *
     * <p><b>E' versionato di proposito.</b> La proposta viene fatta una volta sola, e va bene finche'
     * l'utente ha scelto sapendo come stavano le cose. Quando invece cambia il quadro - come con il
     * difetto C8, che teneva lo storico del conto Funding fermo ai 100 movimenti piu' recenti e faceva
     * anche dire al dialogo che il Funding "non ha limiti" - la scelta gia' data e' stata presa su
     * un'informazione sbagliata, e chi ha un buco nei depositi resterebbe senza alcun modo di spostare
     * indietro la data. Cambiando la costante il confronto non riconosce piu' il valore vecchio e il
     * dialogo si riaffaccia <b>una volta sola</b> su ogni installazione, senza tornare a chiederlo ad
     * ogni scaricamento.
     */
    static final String VALORE_ARCHIVIO_PROPOSTO_OKX = "SI-C8";

    /**
     * Nome dell'opzione (in {@code personale.mv.db}) che ricorda se l'ultimo recupero dell'archivio storico
     * e' rimasto a meta', cioe' se qualche trimestre e' ancora in preparazione presso OKX.
     *
     * <p>Serve perche' dal 04/08/2026 la data di partenza non e' piu' modificabile a mano: e' sempre quella
     * dell'ultimo movimento OKX gia' scaricato. Senza questa opzione il dialogo — che si apre quando la
     * partenza e' piu' vecchia di {@value #GIORNI_STORICO_OKX} giorni — non si riaprirebbe piu' dopo un
     * recupero parziale, perche' lo scaricamento ordinario ha nel frattempo portato la partenza a ieri. I
     * trimestri lasciati indietro resterebbero irraggiungibili, mentre il messaggio del recupero parziale
     * promette esattamente il contrario ("basta rilanciare lo scaricamento piu' tardi").
     *
     * <p>Si accende quando un trimestre torna in uno stato diverso da "scaricato" e si spegne appena un
     * recupero arriva in fondo: e' uno stato che si risolve da solo, non una preferenza.
     */
    public static final String OPZIONE_ARCHIVIO_DA_COMPLETARE_OKX = "OKX_ArchivioDaCompletare";

    /**
     * Trimestri dell'archivio storico OKX rimasti indietro all'ultimo tentativo, separati da virgola
     * (per esempio {@code "2023Q2,2023Q1"}), in {@code personale.mv.db}.
     *
     * <p>Serve perche' dal 13/08/2026 l'elenco dei trimestri e' <b>limitato alla data di partenza dello
     * scaricamento</b> (vedi {@link #trimestriArchivioOKX(long, long, int)}): senza questa memoria, un
     * recupero rimasto a meta' non si completerebbe piu'. Alla corsa successiva la partenza e' l'ultimo
     * movimento importato — recente, perche' l'importazione precedente e' andata a buon fine — e i
     * trimestri vecchi non comparirebbero piu' nell'elenco.
     *
     * <p>Si e' scelto di ricordare <b>i trimestri</b> e non una data di partenza arretrata: sono
     * esattamente quelli gia' richiesti dall'utente e non arrivati, mentre una data avrebbe rimesso in
     * gioco in silenzio anche tutto cio' che c'e' in mezzo — cioe' anni gia' chiusi e verificati, che e'
     * proprio quello che il vincolo sulla data di partenza serve a evitare.
     */
    public static final String OPZIONE_ARCHIVIO_SOSPESI_OKX = "OKX_ArchivioTrimestriSospesi";

    /**
     * Elenca i trimestri da chiedere all'archivio storico di OKX per coprire il periodo che va da
     * {@code dalTimestamp} a oggi, <b>dal più recente al più vecchio</b>.
     *
     * <p>Due esclusioni, entrambe imposte dall'endpoint e non da noi:
     * <ul>
     *   <li>il <b>trimestre in corso</b> non è generabile, e va comunque coperto dallo scaricamento
     *       ordinario a 3 mesi di {@code OKX_Bills.js};</li>
     *   <li>non si scende sotto il {@value #ANNO_MIN_ARCHIVIO_OKX}, perché prima non ci sono dati.</li>
     * </ul>
     *
     * <p>L'ordine dal più recente al più vecchio non è estetico: lo script invia <b>una sola</b> richiesta di
     * generazione per esecuzione, quindi il primo trimestre dell'elenco è quello che l'utente ottiene per
     * primo, ed è ragionevole che sia il più vicino ai movimenti che gli mancano.
     *
     * <p>I confini di trimestre sono calcolati nel fuso locale mentre OKX ragiona nel proprio: sui pochi
     * giorni di scarto si preferisce abbondare, perché un trimestre chiesto in più non fa danni (i movimenti
     * già presenti si deduplicano sull'identificativo) mentre uno in meno lascerebbe un buco silenzioso.
     *
     * @param dalTimestamp inizio del periodo da coprire, millisecondi epoch
     * @param adesso istante corrente, millisecondi epoch
     * @return i trimestri nella forma {@code <anno>Q<n>}, dal più recente al più vecchio; lista vuota se non
     *         c'è nulla da chiedere
     */
    static List<String> trimestriArchivioOKX(long dalTimestamp, long adesso) {
        return trimestriArchivioOKX(dalTimestamp, adesso, ANNO_MIN_ARCHIVIO_OKX);
    }

    /**
     * Come {@link #trimestriArchivioOKX(long, long)}, ma con il pavimento sull'anno scelto dall'utente al
     * posto del {@value #ANNO_MIN_ARCHIVIO_OKX} di sistema.
     *
     * <p>{@code annoMinimo} e' un <b>pavimento</b>, non una data di partenza: alzarlo accorcia l'elenco,
     * abbassarlo non lo allunga oltre {@code dalTimestamp}. E' cosi' di proposito, perche' la domanda che
     * l'utente si sente porre e' "prima di quale anno non c'e' niente da cercare", non "da quando scarico".
     *
     * <p><b>La data di partenza limita l'elenco</b>, ed e' il chiamante a passarla. Fra il 04/08/2026 e il
     * 13/08/2026 esisteva una variante senza argomenti che passava {@code 0}, cioe' chiedeva ogni trimestre
     * dall'anno scelto a oggi qualunque fosse la partenza: serviva a far completare da solo un recupero
     * rimasto a meta', ma il prezzo era che ogni scaricamento rimetteva in gioco anche anni fiscali gia'
     * chiusi e verificati, con il rischio di farvi entrare movimenti nuovi non voluti. Quella variante e'
     * stata tolta; i trimestri non arrivati si recuperano ricordandoli per nome, vedi
     * {@link #OPZIONE_ARCHIVIO_SOSPESI_OKX} e {@link #trimestriDaChiedereOKX}.
     *
     * @param annoMinimo primo anno da considerare; valori piu' bassi di {@value #ANNO_MIN_ARCHIVIO_OKX}
     *                   vengono riportati a quello, perche' prima l'archivio non ha dati
     */
    static List<String> trimestriArchivioOKX(long dalTimestamp, long adesso, int annoMinimo) {
        if (annoMinimo < ANNO_MIN_ARCHIVIO_OKX) annoMinimo = ANNO_MIN_ARCHIVIO_OKX;
        final int pavimento = annoMinimo;
        java.time.ZoneId fuso = java.time.ZoneId.systemDefault();
        java.time.LocalDate inizio = java.time.Instant.ofEpochMilli(dalTimestamp).atZone(fuso).toLocalDate();
        java.time.LocalDate oggi = java.time.Instant.ofEpochMilli(adesso).atZone(fuso).toLocalDate();

        if (inizio.getYear() < pavimento) {
            inizio = java.time.LocalDate.of(pavimento, 1, 1);
        }

        int annoInizio = inizio.getYear();
        int trimInizio = (inizio.getMonthValue() - 1) / 3 + 1;
        int annoFine = oggi.getYear();
        int trimFine = (oggi.getMonthValue() - 1) / 3 + 1;

        //Si arretra di uno: il trimestre in corso non è generabile.
        trimFine--;
        if (trimFine == 0) { trimFine = 4; annoFine--; }

        List<String> trimestri = new ArrayList<>();
        int anno = annoFine, trim = trimFine;
        while (anno > annoInizio || (anno == annoInizio && trim >= trimInizio)) {
            if (anno < pavimento) break;
            trimestri.add(anno + "Q" + trim);
            trim--;
            if (trim == 0) { trim = 4; anno--; }
        }
        return trimestri;
    }


    
    /**
     * Verifica che la distribuzione standalone di Node.js ({@link #NODE_VERSION}) sia presente nella cartella
     * {@code tools/node} della directory di lavoro; se manca, la scarica dal sito ufficiale (rilevando
     * automaticamente sistema operativo e architettura) e la estrae.
     * @throws IOException in caso di errore di rete o di estrazione dell'archivio
     */
    public static void ensureNodeInstalled() throws IOException {
        

        
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").contains("64") ? "x64" : "x86";

        String platform;
        String extension;
        if (os.contains("win")) {
            platform = "win-" + arch;
            extension = "zip";
        } else if (os.contains("mac")) {
            platform = "darwin-" + arch;
            extension = "tar.xz";
        } else {
            platform = "linux-" + arch;
            extension = "tar.xz";
        }

        String filename = "node-" + NODE_VERSION + "-" + platform + "." + extension;
        String url = "https://nodejs.org/dist/" + NODE_VERSION + "/" + filename;
        
        //Verifico se è presente una specifica versione di node, qualora non lo sia scarico la nuova
        if (Files.exists(Paths.get(NODE_DIR.toString()+"/node-" + NODE_VERSION + "-" + platform))) {
           // System.out.println("✅ Node.js già presente");
            return;
        }
        System.out.println("Node.js non presente.");
        System.out.println("⬇️ Scarico Node.js standalone...");
        Path downloadPath = Paths.get(VarStatiche.getWorkingDirectory()+"tools", filename);
        Files.createDirectories(downloadPath.getParent());

        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, downloadPath, StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("Scaricato: " + filename);
        System.out.println("Node DIR = "+NODE_DIR);
        extractArchive(downloadPath, NODE_DIR, extension);
        System.out.println("Estratto Node.js");

        // su Windows, dentro la cartella estratta ci sarà node.exe + npm.cmd
        // su Linux/macOS, sono in /bin
    }
    

    
    
    /**
     * Installa la libreria Node.js CCXT (se non già presente in {@code node_modules}) tramite {@code npm install},
     * usando la distribuzione Node.js gestita da {@link #getNodeExePath()}.
     * @throws IOException in caso di errore di avvio/lettura del processo npm
     * @throws InterruptedException se il thread viene interrotto durante l'attesa del processo
     * @throws RuntimeException se {@code npm install} termina con un codice di uscita diverso da zero
     */
    public static void installCcxt() throws IOException, InterruptedException {
        
        
        Path nodePath = getNodeExePath();
       // System.out.println("nodePath="+nodePath);

        // Non reindirizziamo stderr su stdout
        // builder.redirectErrorStream(true);
        // Calcola la cartella base di Node in modo multipiattaforma
    /*    Path nodeBaseDir = nodePath.getParent(); // es: .../node-vXX-PLATFORM[/bin]
        if (!nodeBaseDir.getFileName().toString().equals("bin")) {
            // Se siamo su Windows, node.exe sta direttamente in base dir, altrimenti sotto bin
            nodeBaseDir = nodeBaseDir.getParent();
        }      */
        
    Path nodeModulesDir = NODE_DIR.resolve("node_modules");
    //Path nodeModulesDir = nodeBaseDir.resolve("node_modules").toAbsolutePath();
    Path ccxtDir = nodeModulesDir.resolve("ccxt");

    if (Files.exists(ccxtDir) && Files.isDirectory(ccxtDir)) {
        //System.out.println("CCXT già installato in: " + ccxtDir);
        return;
    }    
    
    Path npmPath = getNpmPath();
    System.out.println("npmPath="+npmPath);
    

    System.out.println("Installo ccxt...");
    ProcessBuilder builder = new ProcessBuilder(npmPath.toString(), "install", "ccxt");
    System.out.println(VarStatiche.getWorkingDirectory() + "tools/node");
    builder.directory(NODE_DIR.toFile());
    //builder.directory(nodeModulesDir.toFile());
    //builder.directory(new File(Statiche.getWorkingDirectory() + "tools/node"));  // directory di lavoro
    System.out.println("Comando: " + String.join(" ", builder.command()));
    System.out.println("Working directory: " + builder.directory().getAbsolutePath());
    System.out.println("Attendere, la prima installazione potrebbe durare diversi minuti");
    Map<String, String> env = builder.environment();

    // Inserisci la directory che contiene node.exe nel PATH
    String currentPath = env.get("PATH");
    env.put("PATH", nodePath.getParent().toAbsolutePath().toString() + File.pathSeparator + currentPath);

    Process process = builder.start();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
         BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
        String line;
        while ((line = reader.readLine()) != null) System.out.println("[npm] " + line);
        while ((line = errReader.readLine()) != null) System.err.println("[npm-err] " + line);
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) throw new RuntimeException("npm install ccxt fallito");
    System.out.println("ccxt installato con successo");
}

 /**
  * Come {@link #installCcxt}, ma generico per un qualsiasi modulo npm indicato per nome.
  * @param Modulo nome del pacchetto npm da installare
  * @throws IOException in caso di errore di avvio/lettura del processo npm
  * @throws InterruptedException se il thread viene interrotto durante l'attesa del processo
  * @throws RuntimeException se {@code npm install} termina con un codice di uscita diverso da zero
  */
 public static void installModuleNode(String Modulo) throws IOException, InterruptedException {
    //Per il momento mi servono installati ccxt,express e cors    
        
        Path nodePath = getNodeExePath();
        System.out.println("nodePath="+nodePath);
        
    Path nodeModulesDir = NODE_DIR.resolve("node_modules");
    //Path nodeModulesDir = nodeBaseDir.resolve("node_modules").toAbsolutePath();
    Path ModuloDir = nodeModulesDir.resolve(Modulo);

    if (Files.exists(ModuloDir) && Files.isDirectory(ModuloDir)) {
        System.out.println("express già installato in: " + ModuloDir);
        return;
    }    
    
    Path npmPath = getNpmPath();
    System.out.println("npmPath="+npmPath);
    

    System.out.println("Installo "+Modulo+"...");
    ProcessBuilder builder = new ProcessBuilder(npmPath.toString(), "install", Modulo);
    System.out.println(VarStatiche.getWorkingDirectory() + "tools/node");
    builder.directory(NODE_DIR.toFile());
    //builder.directory(nodeModulesDir.toFile());
    //builder.directory(new File(Statiche.getWorkingDirectory() + "tools/node"));  // directory di lavoro
    System.out.println("Comando: " + String.join(" ", builder.command()));
    System.out.println("Working directory: " + builder.directory().getAbsolutePath());
    Map<String, String> env = builder.environment();

    // Inserisci la directory che contiene node.exe nel PATH
    String currentPath = env.get("PATH");
    env.put("PATH", nodePath.getParent().toAbsolutePath().toString() + File.pathSeparator + currentPath);

    Process process = builder.start();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
         BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
        String line;
        while ((line = reader.readLine()) != null) System.out.println("[npm] " + line);
        while ((line = errReader.readLine()) != null) System.err.println("[npm-err] " + line);
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) throw new RuntimeException("npm install "+Modulo+" fallito");
    System.out.println(Modulo+" installato con successo");
}    
    
    
    /** @return il percorso dell'eseguibile {@code npm} della distribuzione Node.js gestita da questa classe */
    public static Path getNpmPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return NODE_DIR.resolve("node-" + NODE_VERSION + "-win-x64").resolve("npm.cmd");
        } else {
            return NODE_DIR.resolve("node-" + NODE_VERSION + "-" + (os.contains("mac") ? "darwin-x64" : "linux-x64"))
                           .resolve("bin").resolve("npm");
        }
    }

    
/** @return il percorso dell'eseguibile {@code node} della distribuzione Node.js gestita da questa classe */
public static Path getNodeExePath() {
    String os = System.getProperty("os.name").toLowerCase();
    String nodeExecutable = os.contains("win") ? "node.exe" : "node";

    // Supponiamo di avere le distribuzioni in una cartella "nodejs" interna al progetto
    // es. nodejs/node-v18.20.3-win-x64/node.exe oppure nodejs/node-v18.20.3-linux-x64/bin/node
    String baseName;
    if (os.contains("win")) {
        baseName = "node-" + NODE_VERSION + "-win-x64";
        return NODE_DIR.resolve(baseName).resolve("node.exe");
    } else if (os.contains("mac")) {
        baseName = "node-" + NODE_VERSION + "-darwin-x64";
        return NODE_DIR.resolve(baseName).resolve("bin").resolve("node");
    } else {
        // Linux
        baseName = "node-" + NODE_VERSION + "-linux-x64";
        return NODE_DIR.resolve(baseName).resolve("bin").resolve("node");
    }
}
   
    
    
    private static void extractArchive(Path archive, Path targetDir, String extension) throws IOException {
        if (extension.equals("zip")) {
            try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(archive.toFile()))) {
                ZipEntry entry;
                while ((entry = zipIn.getNextEntry()) != null) {
                    Path filePath = targetDir.resolve(entry.getName()).normalize();
                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        Files.createDirectories(filePath.getParent());
                        Files.copy(zipIn, filePath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            
        } else {
            // Richiede tar + xz nel sistema (presente su Linux/macOS)
          /*  new ProcessBuilder("tar", "-xf", archive.toString(), "-C", targetDir.getParent().toString())
                .inheritIO().start();*/
          // Creare la cartella targetDir se non esiste
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        //  System.out.println("AAA "+targetDir.toString());
            new ProcessBuilder("tar", "-xf", archive.toString(), "-C", targetDir.toString())
            .inheritIO().start();
        }
    }
    
    
    /**
     * Avvia in background (tramite {@link SwingWorker}) il recupero dei movimenti da un exchange via CCXT
     * ({@link #fetchMovimenti}), mostrando una finestra di progresso indeterminata mentre verifica/installa
     * Node.js e CCXT ed esegue la chiamata.
     * @param exchangeId identificativo CCXT dell'exchange
     * @param apiKey API key dell'account
     * @param secret API secret dell'account
     * @param startDate data di inizio da cui recuperare i movimenti, millisecondi epoch
     * @param Tokens elenco di token (separati da virgola) di cui recuperare esplicitamente i trade
     * @param c componente rispetto a cui centrare la finestra di progresso
     */

    /**
     * Interpreta il valore salvato in {@link #OPZIONE_ANNO_ARCHIVIO_OKX}, riportandolo dentro l'intervallo
     * che ha senso chiedere a OKX.
     *
     * <p>E' tenuto separato dalla lettura del database per poterlo provare senza: e' la parte che decide, e
     * un valore fuori scala qui si tradurrebbe in un elenco di trimestri sbagliato e quindi in richieste
     * sprecate su un endpoint che ne concede pochissime.
     *
     * @param valoreSalvato quanto letto dalle preferenze; vuoto, nullo o non numerico se mai scelto
     * @param annoCorrente anno di riferimento, passato dal chiamante per non dipendere dall'orologio
     * @return l'anno da usare come pavimento, sempre fra {@value #ANNO_MIN_ARCHIVIO_OKX} e {@code annoCorrente}
     */
    static int annoInizioArchivioOKX(String valoreSalvato, int annoCorrente) {
        if (valoreSalvato == null || valoreSalvato.isBlank()) return ANNO_MIN_ARCHIVIO_OKX;
        int anno;
        try {
            anno = Integer.parseInt(valoreSalvato.trim());
        } catch (NumberFormatException e) {
            //Preferenza illeggibile: si torna al comportamento di sistema invece di indovinare
            return ANNO_MIN_ARCHIVIO_OKX;
        }
        if (anno < ANNO_MIN_ARCHIVIO_OKX) return ANNO_MIN_ARCHIVIO_OKX;
        //Un anno nel futuro non lascerebbe alcun trimestre da chiedere: si tratta come "quest'anno"
        if (anno > annoCorrente) return annoCorrente;
        return anno;
    }

    /** @return {@code true} se l'utente ha gia' scelto una volta da che anno partire */
    static boolean annoInizioArchivioOKXGiaScelto() {
        return !DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_ANNO_ARCHIVIO_OKX, "").isBlank();
    }

    /** @return l'anno di partenza dell'archivio scelto dall'utente, o {@value #ANNO_MIN_ARCHIVIO_OKX} */
    static int annoInizioArchivioOKX() {
        return annoInizioArchivioOKX(
                DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_ANNO_ARCHIVIO_OKX, ""),
                java.time.LocalDate.now().getYear());
    }

    /**
     * Dice se di OKX non e' ancora stato importato nulla, cioe' se lo scaricamento in partenza e' il primo.
     *
     * <p>E' la condizione a cui e' legata la domanda sull'anno di partenza dell'archivio: quella scelta ha
     * senso una volta sola, quando non c'e' alcun movimento da cui dedurre da quando cercare. Appena un
     * movimento OKX esiste, la partenza la detta lui e chiedere l'anno non cambierebbe nulla.
     *
     * <p>Guarda i movimenti in memoria e non la tabella degli exchange, perche' e' la stessa fonte da cui
     * {@code GUI_ExchangeAPI} ricava la data proposta: le due letture non possono discordare.
     *
     * @return {@code true} se in archivio non c'e' alcun movimento dell'exchange OKX
     */
    static boolean archivioOKXVuoto() {
        if (Principale.MappaCryptoWallet == null) return true;
        for (String[] Trans : Principale.MappaCryptoWallet.values()) {
            if (Trans != null && Trans.length > 3 && Trans[3] != null
                    && Trans[3].trim().equalsIgnoreCase("OKX")) {
                return false;
            }
        }
        return true;
    }

    /** @return {@code true} se un recupero dell'archivio storico e' rimasto a meta', vedi
     *          {@link #OPZIONE_ARCHIVIO_DA_COMPLETARE_OKX} */
    static boolean archivioOKXDaCompletare() {
        return "SI".equalsIgnoreCase(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_ARCHIVIO_DA_COMPLETARE_OKX, ""));
    }

    /**
     * L'importazione e' stata abbandonata dopo che l'archivio era gia' stato chiesto: le sue righe sono
     * state buttate via insieme alle altre, quindi <b>nessun</b> trimestre di questa corsa puo' contare
     * come fatto. Si riscrive l'elenco completo di quelli chiesti (meno gli {@code ignorato}, che OKX non
     * copre) e si riaccende il flag che riapre il dialogo dello storico.
     */
    static void ArchivioOKXAbbandonato() {
        trimestriSospesiOKX(ArchivioOKX_SospesiSeAbbandonata);
        archivioOKXDaCompletare(true);
    }

    /** Ricorda se restano trimestri da ritirare, vedi {@link #OPZIONE_ARCHIVIO_DA_COMPLETARE_OKX}. */
    static void archivioOKXDaCompletare(boolean daCompletare) {
        DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_ARCHIVIO_DA_COMPLETARE_OKX, daCompletare ? "SI" : "NO");
    }

    /**
     * @return i trimestri rimasti indietro all'ultimo tentativo, dal piu' recente al piu' vecchio; lista
     *         vuota se non ce n'e' nessuno. Vedi {@link #OPZIONE_ARCHIVIO_SOSPESI_OKX}
     */
    static List<String> trimestriSospesiOKX() {
        return trimestriSospesiOKX(DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_ARCHIVIO_SOSPESI_OKX, ""));
    }

    /**
     * Parte pura di {@link #trimestriSospesiOKX()}, separata per poter essere provata senza database.
     * Le voci che non hanno la forma {@code <anno>Q<n>} vengono ignorate: l'opzione e' testo su un file
     * dell'utente, e una riga sporca non deve trasformarsi in una richiesta assurda a OKX.
     * @param salvato valore grezzo dell'opzione, eventualmente {@code null}
     */
    static List<String> trimestriSospesiOKX(String salvato) {
        List<String> esito = new ArrayList<>();
        if (salvato == null) return esito;
        for (String voce : salvato.split(",")) {
            String t = voce.trim().toUpperCase();
            if (t.matches("\\d{4}Q[1-4]") && !esito.contains(t)) esito.add(t);
        }
        return esito;
    }

    /**
     * Trimestri da ricordare come sospesi se l'importazione arriva in fondo (quelli non arrivati) e se
     * invece viene abbandonata (tutti quelli chiesti, tranne gli {@code ignorato}).
     *
     * <p>Sono due liste e non una perche' il salvataggio <b>non puo' avvenire dentro
     * {@link #ScaricaArchivioOKX}</b>: da li' le righe dell'archivio possono ancora essere buttate via da
     * un'uscita anticipata di {@link #fetchMovimenti} — errore sui rendimenti Earn, INTERROMPI, il
     * cancello finale — e un elenco di sospesi gia' scritto direbbe "arrivato" di un trimestre i cui
     * movimenti non sono mai stati importati. Con l'elenco limitato alla data di partenza (vedi C11)
     * quel trimestre non tornerebbe piu' in nessun elenco: sarebbe perso in silenzio.
     *
     * <p>Sono statiche perche' la classe lo e' tutta e la catena di chiamate e' a un livello solo:
     * {@link #ScaricaArchivioOKX} le riempie, {@link #fetchMovimenti} sceglie quale delle due scrivere.
     */
    static List<String> ArchivioOKX_SospesiSeCompletata = new ArrayList<>();
    /** @see #ArchivioOKX_SospesiSeCompletata */
    static List<String> ArchivioOKX_SospesiSeAbbandonata = new ArrayList<>();

    /** Salva i trimestri rimasti indietro, vedi {@link #OPZIONE_ARCHIVIO_SOSPESI_OKX}. */
    static void trimestriSospesiOKX(List<String> trimestri) {
        DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_ARCHIVIO_SOSPESI_OKX,
                trimestri == null ? "" : String.join(",", trimestri));
    }

    /**
     * L'elenco dei trimestri da chiedere davvero: quelli coperti dalla data di partenza, piu' quelli
     * rimasti indietro da un tentativo precedente, senza doppioni e ordinati dal piu' recente.
     *
     * <p>I due insiemi rispondono a due domande diverse e vanno tenuti distinti: il primo e' "fin dove
     * arretrare", ed e' limitato dalla partenza dello scaricamento perche' oltre ci sono anni gia' chiusi
     * che non vanno rimessi in gioco; il secondo e' "cosa mi manca di quello che ho gia' chiesto", e non
     * ha nulla a che vedere con la partenza di oggi.
     *
     * @param dal data di partenza dello scaricamento, millisecondi epoch
     * @param adesso istante corrente, millisecondi epoch
     * @param annoMinimo pavimento sull'anno scelto dall'utente
     * @param sospesi trimestri rimasti indietro dall'ultimo tentativo
     */
    static List<String> trimestriDaChiedereOKX(long dal, long adesso, int annoMinimo, List<String> sospesi) {
        List<String> esito = new ArrayList<>(trimestriArchivioOKX(dal, adesso, annoMinimo));
        for (String t : sospesi) if (!esito.contains(t)) esito.add(t);
        //Dal piu' recente al piu' vecchio: e' l'ordine in cui lo script chiede i trimestri, e il primo
        //dell'elenco e' quello che l'utente ottiene per primo.
        esito.sort(java.util.Comparator.reverseOrder());
        return esito;
    }

    /**
     * Chiede da quale anno far partire il recupero dell'archivio storico, e ricorda la scelta.
     *
     * <p>La domanda si fa perche' la richiesta di generazione di un trimestre e' la chiamata piu' limitata di
     * tutta l'integrazione con OKX: chiedere il 2021 a chi ha aperto il conto nel 2025 significa spendere
     * una dozzina di richieste per scaricare trimestri vuoti. Ogni voce dell'elenco riporta quanti trimestri
     * comporta, cosi' il costo della scelta e' visibile mentre la si fa e non dopo.
     *
     * <p>La domanda si fa <b>solo al primo scaricamento</b>, cioe' quando di OKX non c'e' ancora alcun
     * movimento in archivio ({@link #archivioOKXVuoto()}). Dal secondo in poi la partenza e' la data
     * dell'ultimo movimento gia' scaricato, e un pavimento sull'anno non toglierebbe ne' aggiungerebbe
     * alcun trimestre: chiederlo sarebbe una domanda senza effetto.
     *
     * @param owner finestra rispetto a cui centrare il dialogo
     * @param adesso istante di riferimento, millisecondi epoch
     * @return l'anno scelto, oppure {@code -1} se l'utente ha annullato
     */
    static int ChiediAnnoInizioArchivioOKX(java.awt.Window owner, long adesso) {
        int annoCorrente = java.time.Instant.ofEpochMilli(adesso)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate().getYear();
        int annoSalvato = annoInizioArchivioOKX();

        List<String> voci = new ArrayList<>();
        for (int anno = annoCorrente; anno >= ANNO_MIN_ARCHIVIO_OKX; anno--) {
            //Qui la partenza e' volutamente 0, e NON la data dello scaricamento: la domanda del combo e'
            //"prima di quale anno non c'e' niente da cercare", e ogni voce deve mostrare quanto costa
            //quella scelta. Passando la partenza reale, tutti gli anni piu' vecchi mostrerebbero lo
            //stesso numero e la scelta perderebbe senso. Il conteggio dei trimestri che verranno
            //davvero chiesti e' quello del dialogo dello storico, che usa trimestriDaChiedereOKX.
            int quanti = trimestriArchivioOKX(0, adesso, anno).size();
            voci.add(anno + (anno == ANNO_MIN_ARCHIVIO_OKX ? " (tutto lo storico)" : "")
                    + " — " + quanti + (quanti == 1 ? " trimestre" : " trimestri"));
        }
        if (voci.isEmpty()) return ANNO_MIN_ARCHIVIO_OKX;

        String scelta = AppDialog.showComboBoxDialog(owner,
                "Archivio storico OKX",
                "Da quale anno recuperare lo storico?",
                "OKX prepara l'archivio un trimestre per volta, e la richiesta di generazione è "
                + "fortemente limitata: ogni trimestre in meno è tempo risparmiato.\n\n"
                + "Indica l'anno prima del quale non c'è nulla da cercare, tipicamente quello in cui hai "
                + "aperto il conto. I trimestri più recenti di questa scelta vengono comunque scaricati.\n\n"
                + "Più indietro del " + ANNO_MIN_ARCHIVIO_OKX + " non si può andare in ogni caso: l'archivio "
                + "di OKX parte dal 1° febbraio " + ANNO_MIN_ARCHIVIO_OKX + " e prima di allora non ci sono "
                + "dati.\n\nLa domanda viene fatta una volta sola, adesso che di OKX non hai ancora nulla in "
                + "archivio: dai prossimi scaricamenti in poi la partenza è la data dell'ultimo movimento "
                + "già scaricato."
                + (annoSalvato > ANNO_MIN_ARCHIVIO_OKX ? "\n\nScelta attuale: " + annoSalvato + "." : ""),
                "Non cercare nulla prima del:",
                voci.toArray(new String[0]));

        if (scelta == null || scelta.isBlank()) return -1;

        int anno;
        try {
            anno = Integer.parseInt(scelta.trim().substring(0, 4));
        } catch (RuntimeException e) {
            //L'etichetta e' costruita qui sopra, quindi non dovrebbe succedere: se succede si torna al
            //comportamento di sistema invece di far fallire lo scaricamento
            LoggerGC.ScriviErrore(e);
            return ANNO_MIN_ARCHIVIO_OKX;
        }
        DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_ANNO_ARCHIVIO_OKX, String.valueOf(anno));
        return anno;
    }

    /** Cosa fare quando lo scaricamento OKX partirebbe da più indietro di quanto le API a 3 mesi coprano. */
    public enum SceltaStorico {
        /** Solo lo scaricamento ordinario: i movimenti Trading più vecchi di 3 mesi non arriveranno. */
        PROCEDI,
        /** Scaricamento ordinario più il recupero dall'archivio storico trimestrale del conto Trading. */
        PROCEDI_CON_ARCHIVIO,
        /** L'utente ha rinunciato: non deve partire nulla. */
        ANNULLA
    }

    /**
     * Decide se aprire il dialogo sullo storico OKX. È tenuta separata dal dialogo per poterla provare senza
     * interfaccia: è una condizione a tre ingressi ed è il punto in cui un errore si tradurrebbe o in un
     * modale a ogni scaricamento, o — peggio — in una funzione irraggiungibile proprio a chi serve.
     *
     * <p>Si apre in tre casi:
     * <ul>
     *   <li>lo scaricamento partirebbe da più di {@value #GIORNI_STORICO_OKX} giorni fa, cioè da oltre la
     *       finestra che le API coprono;</li>
     *   <li>oppure la scelta non è <b>mai</b> stata proposta. Senza questo secondo caso resterebbe fuori
     *       proprio chi ha importato solo una parte della propria storia: i suoi movimenti sono recenti,
     *       quindi il primo controllo non scatta, ma il buco dietro resta e non avrebbe alcun modo di
     *       chiedere l'archivio;</li>
     *   <li>oppure l'ultimo recupero dell'archivio è rimasto a metà. È il caso normale — OKX genera i
     *       trimestri con calma — e senza questo terzo ingresso i trimestri rimasti indietro non
     *       sarebbero più raggiungibili: lo scaricamento ordinario porta intanto la partenza a ieri, e
     *       il primo controllo smette di scattare. Vedi
     *       {@link #OPZIONE_ARCHIVIO_DA_COMPLETARE_OKX}.</li>
     * </ul>
     *
     * @param exchangeId exchange in corso; per tutti gli altri il dialogo non si applica
     * @param startDate data da cui partirebbe lo scaricamento, millisecondi epoch
     * @param adesso istante di riferimento, millisecondi epoch
     * @param giaProposto se la scelta è già stata proposta almeno una volta
     * @param archivioDaCompletare se restano trimestri da ritirare da un recupero precedente
     */
    static boolean serveDialogoStoricoOKX(String exchangeId, long startDate, long adesso, boolean giaProposto,
            boolean archivioDaCompletare) {
        if (exchangeId == null || !exchangeId.trim().equalsIgnoreCase("OKX")) return false;
        long giorni = (adesso - startDate) / (24L * 60 * 60 * 1000);
        return giorni > GIORNI_STORICO_OKX || !giaProposto || archivioDaCompletare;
    }

    /**
     * Esito del dialogo sullo storico OKX: cosa fare, e <b>da quando</b>.
     *
     * <p>La data e' sempre quella con cui il dialogo e' stato chiamato — l'ultimo movimento OKX gia'
     * scaricato, o il 1° gennaio 2017 quando di OKX non c'e' ancora nulla. Fino al 04/08/2026 il dialogo
     * permetteva di spostarla indietro a mano; la scelta e' stata tolta perche' una partenza diversa da
     * quella dei dati e' un'informazione che l'utente non ha modo di verificare, mentre l'ultimo movimento
     * e' un fatto. Il campo resta perche' e' cio' che il chiamante usa per partire, e tenerlo qui evita che
     * il dialogo e lo scaricamento possano parlare di due date diverse.
     *
     * @param scelta cosa fare
     * @param startDate data da cui partire, millisecondi epoch
     */
    record EsitoStorico(SceltaStorico scelta, long startDate) { }

    /**
     * Chiede all'utente come procedere quando lo scaricamento OKX dovrebbe partire da una data più vecchia di
     * quanto {@code account/bills-archive} sia in grado di restituire.
     *
     * <p>Il limite riguarda ormai il <b>solo conto Trading</b>: dal 03/08/2026 il Funding passa da
     * {@code asset/bills-history}, che restituisce l'intero storico (verificato: 524 giorni in una sola
     * pagina). Per il Trading esiste invece l'archivio trimestrale, asincrono ma rapido — 105 secondi sui
     * dati reali — che questo dialogo permette di attivare.
     *
     * <p><b>La data di partenza non si sceglie.</b> È sempre quella dell'ultimo movimento OKX già
     * scaricato, o l'inizio dello storico quando di OKX non c'è ancora nulla in archivio: sono le uniche
     * due date che corrispondono a un fatto verificabile. L'unica domanda che resta è l'anno prima del
     * quale non cercare, e viene fatta <b>solo al primo scaricamento</b>, quando non esiste un ultimo
     * movimento da cui partire. Fino al 04/08/2026 questo dialogo aveva due pulsanti in più per
     * modificare a mano data e anno.
     *
     * @param exchangeId exchange su cui si sta per scaricare; per tutti gli altri il controllo non si applica
     * @param startDate data da cui partirebbe lo scaricamento, millisecondi epoch
     * @param c componente rispetto a cui centrare la finestra
     * @return la scelta dell'utente; {@link SceltaStorico#PROCEDI} anche quando il dialogo non serve
     */
    static EsitoStorico SceltaStoricoOKX(String exchangeId, long startDate, Component c) {
        boolean giaProposto = VALORE_ARCHIVIO_PROPOSTO_OKX.equalsIgnoreCase(
                DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_ARCHIVIO_PROPOSTO_OKX, ""));
        boolean daCompletare = archivioOKXDaCompletare();
        if (!serveDialogoStoricoOKX(exchangeId, startDate, System.currentTimeMillis(), giaProposto,
                daCompletare)) {
            return new EsitoStorico(SceltaStorico.PROCEDI, startDate);
        }

        java.awt.Window owner = (c instanceof java.awt.Window w) ? w
                : (c == null ? null : SwingUtilities.getWindowAncestor(c));

        long giorni = (System.currentTimeMillis() - startDate) / (24L * 60 * 60 * 1000);
        //Dev'essere lo stesso elenco che ScaricaArchivioOKX chiedera' davvero, altrimenti il conteggio
        //mostrato nel dialogo direbbe una cosa e lo scaricamento ne farebbe un'altra.
        List<String> trimestri = trimestriDaChiedereOKX(startDate, System.currentTimeMillis(),
                annoInizioArchivioOKX(), trimestriSospesiOKX());

        //Primo scaricamento: non c'è un ultimo movimento da cui partire, quindi la partenza è quella di
        //default e l'anno da cui cercare è una domanda che ha senso porre. È l'unico caso in cui si pone.
        boolean primoScaricamento = archivioOKXVuoto();
        boolean annoGiaScelto = annoInizioArchivioOKXGiaScelto();
        int annoScelto = annoInizioArchivioOKX();
        //Il dialogo si apre per tre motivi diversi (vedi serveDialogoStoricoOKX) e il titolo deve dire
        //quello giusto: con la partenza a ieri, "partirebbe da troppo indietro" sarebbe falso.
        boolean soloDaCompletare = daCompletare && giorni <= GIORNI_STORICO_OKX;
        String titolo;
        if (giorni > GIORNI_STORICO_OKX)  titolo = "Lo scaricamento partirebbe da troppo indietro";
        else if (daCompletare)            titolo = "Restano dei trimestri da ritirare";
        else                              titolo = "Puoi recuperare anche lo storico più vecchio";

        AppDialog.Builder dialogo = AppDialog.builder(owner)
                .windowTitle("Storico OKX oltre il limite delle API")
                .bodyTitle(titolo)
                .showTitleInBody(false)
                .theme()
                .type(AppDialog.DialogType.WARNING)
                .message("")
                .details((soloDaCompletare
                            ? "L'ultimo recupero dell'archivio storico è rimasto a metà: qualche trimestre "
                              + "era ancora in preparazione presso OKX.\n\n"
                            : "")
                        + "Lo scaricamento parte dal "
                        + FunzioniDate.ConvertiDatadaLongAlSecondo(startDate)
                        + (primoScaricamento
                            ? ": di OKX non hai ancora alcun movimento in archivio, quindi si parte "
                              + "dall'inizio dello storico.\n\n"
                            : ", cioè " + giorni + " giorni fa: è la data dell'ultimo movimento OKX già in "
                              + "archivio, e da lì si riparte sempre.\n\n")
                        + "Il conto Funding (depositi, prelievi, giroconti) non ha limiti di finestra: viene "
                          + "scaricato per intero, ma sempre e solo dalla data di partenza in avanti.\n\n"
                        + "Il conto Trading invece torna al massimo gli ultimi 3 mesi. I movimenti più vecchi si "
                        + "recuperano dall'archivio storico di OKX, che li fornisce un trimestre alla volta: "
                        + "servono " + trimestri.size() + " trimestri per coprire tutto lo storico"
                        + (annoGiaScelto
                            ? " dal " + annoScelto + " a oggi, l'anno che hai indicato come inizio. Quelli "
                              + "già scaricati in passato vengono ritirati senza nuove richieste.\n\n"
                            : ", ma potrai restringerli indicando l'anno in cui hai aperto il conto (mai "
                              + "prima del " + ANNO_MIN_ARCHIVIO_OKX + ", da cui parte l'archivio di OKX).\n\n")
                        + "Il programma li chiede tutti insieme e attende fino a 10 minuti, ritirando ogni file "
                        + "appena è pronto; sui dati reali 11 trimestri sono stati generati in 5 minuti. Quello "
                        + "che non fa in tempo non va perso: basta rilanciare lo scaricamento più tardi e viene "
                        + "raccolto senza rifare la richiesta.\n\n"
                        + "Come vuoi procedere?")
                .action(AppDialog.DialogAction.builder("annulla", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("continua", "Solo ultimi 3 mesi")
                        .role(AppDialog.ActionRole.DANGER)
                        .build())
                .action(AppDialog.DialogAction.builder("archivio", "Recupera anche l'archivio")
                        .role(AppDialog.ActionRole.PRIMARY)
                        .build());

        AppDialog.DialogResult result = dialogo.showDialog();

        if (result == null) return new EsitoStorico(SceltaStorico.ANNULLA, startDate);

        //Da qui in poi si esce con una risposta: la proposta e' stata fatta e non va rifatta ogni volta
        DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_ARCHIVIO_PROPOSTO_OKX, VALORE_ARCHIVIO_PROPOSTO_OKX);

        if (result.isAction("archivio")) {
            //L'anno si chiede solo al primo scaricamento e solo a chi l'archivio lo vuole davvero: e' la
            //scelta che determina quante richieste di generazione verranno spese su un endpoint che ne
            //concede poche, e dal secondo scaricamento in poi non avrebbe piu' alcun effetto, perche' la
            //partenza la detta l'ultimo movimento gia' scaricato.
            //Annullare la scelta dell'anno annulla l'intero scaricamento, non solo l'archivio: chi voleva il
            //solo scaricamento ordinario ha il suo pulsante, e ripetere costa un clic.
            if (primoScaricamento
                    && ChiediAnnoInizioArchivioOKX(owner, System.currentTimeMillis()) < 0) {
                return new EsitoStorico(SceltaStorico.ANNULLA, startDate);
            }
            return new EsitoStorico(SceltaStorico.PROCEDI_CON_ARCHIVIO, startDate);
        }
        if (result.isAction("continua")) {
            //"Solo ultimi 3 mesi" e' anche la risposta a un promemoria: chi la sceglie non va richiamato al
            //prossimo scaricamento. Nulla va perso, perche' l'elenco dei trimestri riparte comunque
            //dall'anno scelto e i sospesi si ripresentano appena si chiede di nuovo l'archivio.
            if (daCompletare) archivioOKXDaCompletare(false);
            return new EsitoStorico(SceltaStorico.PROCEDI, startDate);
        }
        return new EsitoStorico(SceltaStorico.ANNULLA, startDate);
    }

    public static Importazioni.Esito fetchMovimentiConBar(String exchangeId, String apiKey, String secret, long startDate,String Tokens,Component c) {
        return fetchMovimentiConBar(exchangeId, apiKey, secret, startDate, Tokens, "", c);
    }

    /**
     * Come {@link #fetchMovimentiConBar(String, String, String, long, String, Component)}, ma con la terza
     * credenziale richiesta da alcuni exchange (la passphrase di OKX, che CCXT chiama {@code password}).
     * @param exchangeId identificativo CCXT dell'exchange
     * @param apiKey API key dell'account
     * @param secret API secret dell'account
     * @param passphrase terza credenziale (passphrase OKX); stringa vuota per gli exchange che non la prevedono
     * @param startDate data di inizio da cui recuperare i movimenti, millisecondi epoch
     * @param Tokens elenco di token (separati da virgola) di cui recuperare esplicitamente i trade
     * @param c componente rispetto a cui centrare la finestra di progresso
     * @return l'esito dello scaricamento da mostrare nel resoconto, oppure {@code null} se l'importazione non
     *         è stata portata a termine (rinuncia dell'utente, interruzione o errore già segnalato)
     */
    public static Importazioni.Esito fetchMovimentiConBar(String exchangeId, String apiKey, String secret, long startDate,String Tokens,String passphrase,Component c) {
             // TODO add your handling code here:
        //CcxtInterop a = new CcxtInterop();

        //L'avviso va dato prima di aprire la finestra di avanzamento: se l'utente rinuncia non deve
        //essere partito nulla, né il download di Node né la chiamata all'exchange.
        final EsitoStorico esitoScelta = SceltaStoricoOKX(exchangeId, startDate, c);
        if (esitoScelta.scelta() == SceltaStorico.ANNULLA) return null;
        final boolean conArchivio = (esitoScelta.scelta() == SceltaStorico.PROCEDI_CON_ARCHIVIO);
        //L'utente può aver spostato indietro la data nel dialogo: da qui in poi vale la sua.
        final long dataInizio = esitoScelta.startDate();

        //L'esito viene raccolto qui dal thread in background e letto solo quando la finestra di progresso
        //(modale) si è chiusa, cioè quando lo scaricamento è finito: il resoconto lo mostra il chiamante,
        //così più exchange scaricati di seguito possono confluire in un'unica finestra.
        final Importazioni.Esito Esiti[] = new Importazioni.Esito[1];

        c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Download progress = new Download();
        progress.setIndeterminate(true);
        progress.SetLabel("Scaricamento da API in corso...");
        //progress.RipristinaStdout();
        //progress.MostraProgressAttesa("Export in Excel", "Esportazione in corso...");
        progress.setLocationRelativeTo(c);

        // Esegui l'export in background
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
        /** Verifica/installa Node.js e CCXT, poi esegue {@link #fetchMovimenti} in background. */
        @Override
        protected Void doInBackground() throws Exception {
        try {
        System.out.println("Verifico Installazione di Node");
        ensureNodeInstalled();
        System.out.println("Verifico Installazione di CCXT");
        installCcxt();
        System.out.println("Eseguo la chiamata");
        
        Esiti[0]=fetchMovimenti(exchangeId, apiKey, secret,dataInizio,Tokens,passphrase,progress,c,conArchivio);
        } catch (IOException ex) {
            Logger.getLogger(Principale.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Principale.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
        }
        
        /** Chiude la finestra di progresso e ripristina il cursore normale al termine dell'operazione. */
        @Override
        protected void done() {
        progress.dispose();
        c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        };

        //Da qui in poi INTERROMPI vale per tutta l'operazione, fase di scaricamento prezzi compresa.
        //Lo scope si apre e si chiude QUI, nel proprietario dell'operazione, e non nel costruttore di
        //Download: le finestre di avanzamento si annidano, e legare lo scope alla finestra lascerebbe il
        //conteggio disallineato appena una di esse salta la chiusura. Vedi Interruzione.
        Interruzione.Apri();
        try {
            worker.execute();
            progress.setVisible(true);// Questo blocca finché done() non chiama dispose()
        } finally {
            Interruzione.Chiudi();
        }

        return Esiti[0];
    }
    
    

    /**
     * Recupera tramite CCXT (script Node) tutti i movimenti storici di un account exchange e li scrive in
     * {@link Principale#MappaCryptoWallet}. Per {@code "Binance"}: scarica depositi, prelievi, movimenti fiat,
     * conversioni, asset dividend ed earn ({@link #fetchMovimento}), determina i token effettivamente
     * movimentati (giacenza netta diversa da zero, uniti a quelli passati esplicitamente in {@code Tokens} e a
     * quelli forzati manualmente in {@link DatabaseH2#Pers_ExchangeTokens_LeggiTokensExchange}) e infine
     * recupera anche i trade spot per quei token, importando tutto nel database tramite
     * {@link Importazioni#ScriviListaSuMappaCrypto}. L'elaborazione può essere interrotta anticipatamente
     * impostando {@link Principale#InterrompiCiclo} o in presenza di errori Node.js segnalati da {@code progress}.
     * @param exchangeId identificativo CCXT dell'exchange (attualmente gestito solo {@code "Binance"}, con un ramo di test per {@code "Binancet"})
     * @param apiKey API key dell'account
     * @param secret API secret dell'account
     * @param startDate data di inizio da cui recuperare i movimenti, millisecondi epoch
     * @param Tokens elenco di token (separati da virgola) di cui recuperare esplicitamente i trade
     * @param passphrase terza credenziale (passphrase OKX); stringa vuota per gli exchange che non la prevedono
     * @param progress finestra di progresso su cui riportare l'avanzamento e verificare interruzione/errori
     * @param c componente parent per gli eventuali dialog di avviso
     * @return l'esito dell'importazione (conteggi e movimenti sconosciuti) da mostrare nel resoconto, oppure
     *         {@code null} se l'elaborazione è stata interrotta o non è arrivata all'importazione
     */
    public static Importazioni.Esito fetchMovimenti(String exchangeId, String apiKey, String secret, long startDate,String Tokens,String passphrase,Download progress,Component c) {
        return fetchMovimenti(exchangeId, apiKey, secret, startDate, Tokens, passphrase, progress, c, false);
    }

    /**
     * Come {@link #fetchMovimenti(String, String, String, long, String, String, Download, Component)}, ma con
     * la possibilità di recuperare anche l'archivio storico trimestrale del conto Trading di OKX.
     *
     * @param conArchivio {@code true} per aggiungere allo scaricamento ordinario il recupero dall'archivio
     *        storico; ignorato per gli exchange diversi da OKX
     * @return l'esito dell'importazione, oppure {@code null} se l'elaborazione è stata interrotta
     */
    public static Importazioni.Esito fetchMovimenti(String exchangeId, String apiKey, String secret, long startDate,String Tokens,String passphrase,Download progress,Component c,boolean conArchivio) {
        //Scaricamento da API: il documento di origine non esiste su disco, va creato. Uno solo per
        //scaricamento, in NDJSON, perché uno "scarico da exchange" non è una chiamata sola: OKX_Bills pagina
        //a 100 record, Binance_Trades gira una volta per token e OKX_Archivio raccoglie fino a 21 trimestri.
        //Un documento per risposta produrrebbe centinaia di file per un solo scaricamento.
        int IdSessione = DocumentiFonte.ApriSessione(exchangeId);
        IdDocumentoSessione = IdSessione;
        Importazioni.DocumentoFonteCorrente = IdSessione;
        Importazioni.Esito Esito;
        try {
            Esito = fetchMovimentiInterno(exchangeId, apiKey, secret, startDate, Tokens, passphrase, progress, c, conArchivio);
        } finally {
            IdDocumentoSessione = 0;
            Importazioni.DocumentoFonteCorrente = 0;
            DocumentiFonte.ChiudiSessione(IdSessione);
        }
        //Uno scaricamento che non aggiunge nulla (nessuna novità, interruzione, errore) butta via il proprio
        //NDJSON: il registro resterebbe altrimenti pieno di documenti a cui nessun movimento fa riferimento.
        DocumentiFonte.ChiudiRegistrazione(new DocumentiFonte.Registrazione(IdSessione, true),
                Esito == null ? 0 : Esito.Aggiunte);
        return Esito;
    }

    /** Id del documento NDJSON dello scaricamento in corso, a cui {@link #fetchMovimento} appende le risposte. */
    private static int IdDocumentoSessione = 0;

    /** Corpo di {@link #fetchMovimenti(String, String, String, long, String, String, Download, Component, boolean)}, eseguito con la sessione di documento già aperta. */
    private static Importazioni.Esito fetchMovimentiInterno(String exchangeId, String apiKey, String secret, long startDate,String Tokens,String passphrase,Download progress,Component c,boolean conArchivio) {
       // Map<String, JsonObject> Mappa_Json = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        List<JsonObject> Jsons = new ArrayList<>();
        
        //BINACE TEST
        if (exchangeId.equalsIgnoreCase("Binancet")) {
            long inizioanno=Long.parseLong("1609465487000");
            //1 - RECUPERO TUTTI I MOVIMENTI TRANNE I TRADES
           // String estrazioni[] = new String[]{"depositi", "prelievi", "Binance_Conversioni", "Binance_EarnFlessibili", "Binance_EarnLocked"};
            String estrazioni[] = new String[]{"check_node_path"};
            for (String script : estrazioni) {
                JsonObject json = fetchMovimento(exchangeId, apiKey, secret, inizioanno, "", script);
                if (json != null) {
                    Jsons.add(json);
                }
            }

            
            //2 - RECUPERO I TOKEN COINVOLTI NELLE TRANSAZIONI
            //Adesso che ho scaricato tutti i movimenti recupero la lista dei movimenti nel formato standard di GiacenzeCrypto
            List<String[]> lista = getListaMovimenti(Jsons, exchangeId);

            //Adesso devo recuperare tutti i token movimentati per poter poi creare un array di token da passare per il recupero dei trades
            for (String[] riga : lista) {
                //Se il token non è presente nella lista lo aggiungo
                if (!riga[8].isBlank() && !Tokens.contains(riga[8])) {
                    Tokens = Tokens + "," + riga[8];
                }
                if (!riga[11].isBlank() && !Tokens.contains(riga[11])) {
                    Tokens = Tokens + "," + riga[11];
                }
            }
            
            DatabaseH2.Pers_ExchangeTokens_LeggiTokensExchange("Binance");
            
            //Recupero la lista dei token con le varie somme e prendo solo quelli che hanno una somma diversa da zero
            //solo su quelli vado a cercare i trades, infatti i token che vanno a zero molto probabilmente non sono stati scambiati oltre le varie conversions
            

            //3 - RECUPERO I TRADES DEI TOKEN COINVOLTI + QUELLI RICHIESTI IN ORIGINE
            //Importazioni.inserisciListaMovimentisuMappaCryptoWallet(
            //Recuperato la lista di token da richiedere procedo con il recupero dei trades
           // JsonObject json = fetchMovimento(exchangeId, apiKey, secret, startDate, Tokens, "Binance_Trades");
           // lista.addAll(getListaMovimento(json, exchangeId));

            //4 - IMPORTO TUTTO NEL DATABASE
            //Recuperati tutti i movimenti posso procedere all'aggiunta al database vera e propria
            if(!Principale.InterrompiCiclo)
                Importazioni.ScriviListaSuMappaCrypto(lista,true);
            //Solo se non ho premutol il tasto annulla, in quel caso non faccio nulla
            else{
                JOptionPane.showConfirmDialog(c, "Elaborazione interrotta dall'utente!",
                                "Attenzione", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null);
            }

        }
        
        //BINACE
        if (exchangeId.equalsIgnoreCase("Binance")) {
            
            //Intanto aggiungo in una lista univoca i tokens
            Set<String> setTokens = new HashSet<>();
            String tokens[]=Tokens.split(",");
            setTokens.addAll(Arrays.asList(tokens));
            
            //0 - RECUPERO TUTTI I TOKEN DI BINANCE CON GIACENZA DIVERSA DA ZERO
            //Recupero la lista dei token con le varie somme e prendo solo quelli che hanno una somma diversa da zero
            //solo su quelli vado a cercare i trades, infatti i token che vanno a zero molto probabilmente non sono stati scambiati oltre le varie conversions
            Map<String, Moneta> QtaCrypto = new TreeMap<>();//nel primo oggetto metto l'ID, come secondo oggetto metto la moneta con tutti i dati
            for (String[] movimento : Principale.MappaCryptoWallet.values()) {
                if (movimento[3].trim().equalsIgnoreCase("Binance")) {
                    Moneta Monete[] = new Moneta[2];//in questo array metto la moneta in entrata e quellain uscita
                    //in paricolare la moneta in uscita nella posizione 0 e quella in entrata nella posizione 1
                    Monete[0] = new Moneta();
                    Monete[1] = new Moneta();
                    Monete[0].Moneta = movimento[8];
                    Monete[0].Tipo = movimento[9];
                    Monete[0].Qta = movimento[10];
                    Monete[1].Moneta = movimento[11];
                    Monete[1].Tipo = movimento[12];
                    Monete[1].Qta = movimento[13];
                    //questo ciclo for serve per recuperare le qta di tutte le monete
                    for (int a = 0; a < 2; a++) {
                        //ANALIZZO MOVIMENTI
                        if (!Monete[a].Moneta.isBlank() && QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo) != null) {
                            //Movimento già presente da implementare
                            Moneta M1 = QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo);
                            M1.Qta = new BigDecimal(M1.Qta)
                                    .add(new BigDecimal(Monete[a].Qta)).stripTrailingZeros().toPlainString();

                        } else if (!Monete[a].Moneta.isBlank()) {
                            //Movimento Nuovo da inserire
                            Moneta M1 = new Moneta();
                            M1.InserisciValori(Monete[a].Moneta, Monete[a].Qta, "", Monete[a].Tipo);//il campo vuoto sarebbe risevato all'address che non mi serve in questo momento
                            QtaCrypto.put(Monete[a].Moneta + ";" + Monete[a].Tipo, M1);

                        }
                    }
                }
            }           
            //Adesso che ho le qta di tutte le monete le metto quelli che hanno qta zero in una lista separata da virgole e la do in pasto alla funzione che recupera i trades
            for(Moneta m:QtaCrypto.values()){
                if (BG(m.Qta).compareTo(BigDecimal.ZERO)!=0){
                    //Tokens=Tokens+","+m.Moneta;
                    setTokens.add(m.Moneta);
                }
            }
            
            //1 - RECUPERO TUTTI I MOVIMENTI TRANNE I TRADES
            progress.setTitle("Scaricamento dei dati di "+exchangeId+" tramite API");
            String estrazioni[] = new String[]{"depositi", "prelievi", "Binance_MovimentiFiat","Binance_Conversioni","Binance_ConversioniSmall", "Binance_AssetDividend","Binance_EarnFlessibili","Binance_EarnLocked"};
            //String estrazioni[] = new String[]{"Binance_StakingSOL"};
            int chiamate=estrazioni.length+1;
            int j=0;
            for (String script : estrazioni) {
                //Interrompo la funzione se ho premuto interrompi o se ho degli errori bloccanti sulla funzione
                if (Principale.InterrompiCiclo||progress.ErroriNodeJS())
                {
                    JOptionPane.showConfirmDialog(null, "Impot terminato prematuramente!!","Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
                    return null;
                }
                j++;
                progress.SetMessaggioAvanzamento("Comunicazione con endpoint "+j+" di "+chiamate+" in corso...");
                JsonObject json = fetchMovimento(exchangeId, apiKey, secret, startDate, "", script);
                if (json != null) {
                    Jsons.add(json);
                }
            }

            
            //2 - RECUPERO I TOKEN COINVOLTI NELLE TRANSAZIONI PER CUI LA LORO SOMMA SIA DIVERSA DA ZERO
            //Adesso che ho scaricato tutti i movimenti recupero la lista dei movimenti nel formato standard di GiacenzeCrypto
            List<String[]> lista = getListaMovimenti(Jsons, exchangeId);


            //Recupero la lista dei token con le varie somme e prendo solo quelli che hanno una somma diversa da zero
            //solo su quelli vado a cercare i trades, infatti i token che vanno a zero molto probabilmente non sono stati scambiati oltre le varie conversions
            QtaCrypto = new TreeMap<>();//nel primo oggetto metto l'ID, come secondo oggetto metto la moneta con tutti i dati
            for (String[] movimento : lista) {
                Moneta Monete[] = new Moneta[2];//in questo array metto la moneta in entrata e quellain uscita
                //in paricolare la moneta in uscita nella posizione 0 e quella in entrata nella posizione 1
                Monete[0] = new Moneta();
                Monete[1] = new Moneta();
                Monete[0].Moneta = movimento[8];
                Monete[0].Tipo = movimento[9];
                Monete[0].Qta = movimento[10];
                Monete[1].Moneta = movimento[11];
                Monete[1].Tipo = movimento[12];
                Monete[1].Qta = movimento[13];                
                //questo ciclo for serve per recuperare le qta di tutte le monete
                for (int a = 0; a < 2; a++) {
                    //ANALIZZO MOVIMENTI
                    if (!Monete[a].Moneta.isBlank() && QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo) != null) {
                        //Movimento già presente da implementare
                        Moneta M1 = QtaCrypto.get(Monete[a].Moneta + ";" + Monete[a].Tipo);
                        M1.Qta = new BigDecimal(M1.Qta)
                                .add(new BigDecimal(Monete[a].Qta)).stripTrailingZeros().toPlainString();

                    } else if (!Monete[a].Moneta.isBlank()) {
                        //Movimento Nuovo da inserire
                        Moneta M1 = new Moneta();
                        M1.InserisciValori(Monete[a].Moneta, Monete[a].Qta, "", Monete[a].Tipo);//il campo vuoto sarebbe risevato all'address che non mi serve in questo momento
                        QtaCrypto.put(Monete[a].Moneta + ";" + Monete[a].Tipo, M1);

                    }
                }
            }
            
            //Adesso che ho le qta di tutte le monete le metto quelli che hanno qta zero in una lista separata da virgole e la do in pasto alla funzione che recupera i trades
            for(Moneta m:QtaCrypto.values()){
                if (BG(m.Qta).compareTo(BigDecimal.ZERO)!=0){
                    setTokens.add(m.Moneta);
                    //Tokens=Tokens+","+m.Moneta;
                }
            }
            //Come ultima cosa aggiungo i token Forzati manualmente alla lista
            List<String> lis=DatabaseH2.Pers_ExchangeTokens_LeggiTokensExchange("Binance");
            for (String l:lis){
                setTokens.add(l);
            }
            
            //Butto tutti i token nella stringa da passare allo script
            String tok="";
            for (String t:setTokens){
                tok=tok+","+t;
            }
            
            //Come ultima cosa aggiungo i token Forzati manualmente alla lista
            
            //3 - RECUPERO I TRADES DEI TOKEN COINVOLTI + QUELLI RICHIESTI IN ORIGINE
            //Importazioni.inserisciListaMovimentisuMappaCryptoWallet(
            //Recuperato la lista di token da richiedere procedo con il recupero dei trades
            j++;
            progress.SetMessaggioAvanzamento("Comunicazione con endpoint "+j+" di "+chiamate+" in corso...");
            JsonObject json = fetchMovimento(exchangeId, apiKey, secret, startDate, tok, "Binance_Trades");
            lista.addAll(getListaMovimento(json, exchangeId));
            if (Principale.InterrompiCiclo||progress.ErroriNodeJS())
                {
                    JOptionPane.showConfirmDialog(null, "Impot terminato prematuramente!!","Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
                    return null;
                }

            //4 - IMPORTO TUTTO NEL DATABASE
            //Recuperati tutti i movimenti posso procedere all'aggiunta al database vera e propria
            //Se non è andato tutto a buon fine non porto a termine l'importazione
            //Importazioni.inserisciListaMovimentisuMappaCryptoWallet(lista);
            int risultato[]=Importazioni.ScriviListaSuMappaCrypto(lista,true);
            if (risultato[0]!=0)
            {
                Principale.TabellaCryptodaAggiornare=true;
            }
            //ScriviListaSuMappaCrypto non tocca i contatori statici di Importazioni: l'esito va costruito
            //dai valori che restituisce, altrimenti si mostrerebbero i numeri di un'importazione precedente.
            //Il ramo CCXT di Binance non produce causali sconosciute: quello che non sa convertire non lo genera.
            return new Importazioni.Esito(exchangeId, risultato[0]+risultato[1], risultato[0], risultato[1], 0, "");
        }

        //OKX
        if (exchangeId.equalsIgnoreCase("OKX")) {
            //A differenza di Binance non serve scoprire prima i token movimentati: gli endpoint bills di OKX
            //restituiscono tutti i movimenti senza che sia necessario indicare la coppia scambiata.
            progress.setTitle("Scaricamento dei dati di "+exchangeId+" tramite API");
            progress.SetMessaggioAvanzamento("Comunicazione con OKX in corso...");

            //Il dominio regionale di OKX su cui la chiave esiste viene individuato dallo script alla prima
            //esecuzione e poi ricordato: è una proprietà dell'account, non cambia da uno scaricamento all'altro.
            String hostnameOKX = DatabaseH2.Pers_Opzioni_Leggi(OPZIONE_HOSTNAME_OKX);
            if (hostnameOKX == null) hostnameOKX = "";

            JsonObject json = fetchMovimento(exchangeId, apiKey, secret, startDate, "", "OKX_Bills", passphrase, hostnameOKX);

            if (json != null && json.has("okx_hostname")) {
                String riconosciuto = json.get("okx_hostname").getAsString();
                if (!riconosciuto.isBlank() && !riconosciuto.equals(hostnameOKX)) {
                    DatabaseH2.Pers_Opzioni_Scrivi(OPZIONE_HOSTNAME_OKX, riconosciuto);
                    System.out.println("Dominio OKX riconosciuto e memorizzato : "+riconosciuto);
                }
                //Il dominio va ripreso anche nella variabile locale, non solo salvato: OKX_Archivio.js non
                //sonda i domini per conto suo (ripiega su www.okx.com) e gli script che seguono in QUESTA
                //stessa esecuzione lo ricevono da qui. Senza, alla prima corsa di un account non-www
                //l'archivio storico falliva tutti i trimestri con 50119 "la chiave API non esiste", e solo
                //la corsa successiva - che rilegge l'opzione dal database - funzionava.
                if (!riconosciuto.isBlank()) hostnameOKX = riconosciuto;
            }

            if (Principale.InterrompiCiclo||progress.ErroriNodeJS()) {
                JOptionPane.showConfirmDialog(null, "Impot terminato prematuramente!!","Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
                return null;
            }
            if (json == null) {
                JOptionPane.showConfirmDialog(null, "Nessuna risposta da OKX: importazione non eseguita.",
                        "Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null);
                return null;
            }
            if (json.has("error")) {
                JOptionPane.showConfirmDialog(null, "Errore nella comunicazione con OKX:\n"+json.get("error").getAsString(),
                        "Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null);
                return null;
            }

            //Le righe dei due conti confluiscono in un'unica lista: il raggruppamento per orario deve poterle
            //vedere insieme, perché le due gambe di uno scambio possono arrivare da conti diversi.
            List<String[]> righe = new ArrayList<>();
            JsonArray fundingBills = json.has("okx_fundingBills") ? json.getAsJsonArray("okx_fundingBills") : new JsonArray();
            JsonArray tradingBills = json.has("okx_tradingBills") ? json.getAsJsonArray("okx_tradingBills") : new JsonArray();
            List<String[]> rf = convertOKXBills(fundingBills, "Funding");
            List<String[]> rt = convertOKXBills(tradingBills, "Trading");
            if (rf == null || rt == null) {   //null = interruzione richiesta dall'utente
                JOptionPane.showConfirmDialog(null, "Impot terminato prematuramente!!","Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
                return null;
            }
            righe.addAll(rf);
            righe.addAll(rt);

            //Archivio storico del conto Trading, solo se richiesto: recupera i trimestri che account/bills-archive
            //non copre. Le due finestre si SOVRAPPONGONO: l'archivio copre il trimestre intero, bills-archive gli
            //ultimi ~3 mesi, quindi le operazioni che cadono in entrambe arrivano due volte. I doppioni vengono
            //tolti subito sotto, da deduplicaBillOKX: non si eliminano da soli piu' avanti nella catena, perche'
            //il consolidamento somma le gambe per moneta e la deduplica per ID che c'e' in Ex_OKX_ImportaDaAPI
            //guarda solo i movimenti gia' salvati, non quelli dello stesso scaricamento.
            //Se da qui in poi l'importazione viene abbandonata, le righe dell'archivio vengono buttate via
            //insieme alle altre, e il flag "restano trimestri da completare" va riacceso: e' l'unica cosa
            //che riapre il dialogo dello storico alla corsa successiva, e senza di esso un utente con
            //movimenti recenti non avrebbe piu' modo di richiedere l'archivio. Non e' uno spreco: i file
            //gia' generati restano pronti su OKX e vengono ritirati senza rispendere una richiesta.
            boolean archivioDaRecuperare = false;
            if (conArchivio) {
                List<String[]> ra = ScaricaArchivioOKX(exchangeId, apiKey, secret, startDate, passphrase,
                        hostnameOKX, progress);
                if (ra == null) {
                    //null = interruzione dell'utente oppure errore dell'API: in entrambi i casi non si importa
                    //nulla, quindi nessun trimestre di questa corsa conta come fatto.
                    ArchivioOKXAbbandonato();
                    //L'avviso specifico dell'errore l'ha gia' mostrato ScaricaArchivioOKX: qui si parla solo
                    //del caso interruzione, altrimenti l'utente vedrebbe due finestre di seguito.
                    if (Principale.InterrompiCiclo) {
                        JOptionPane.showConfirmDialog(null, "Impot terminato prematuramente!!","Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
                    }
                    return null;
                }
                righe.addAll(ra);
                archivioDaRecuperare = true;
            }

            //Si deduplica anche quando l'archivio non e' stato chiesto: costa una scansione su una lista
            //gia' in memoria e non dipende da quale ramo l'ha riempita. Riguarda le sole righe dei bill:
            //i rendimenti Earn qui sotto non ci passano, ma non ne hanno bisogno, perche' il loro ID
            //(EARN-<moneta>-<aaaammgg>) e' uno per moneta e per giornata gia' per costruzione.
            righe = deduplicaBillOKX(righe);

            //I rendimenti dei prodotti Earn non sono nei bill: hanno endpoint propri e vanno chiesti a parte.
            //Si riparte dalla mezzanotte del giorno di startDate, non da startDate: l'aggregazione è per
            //giornata e una giornata va scaricata intera, altrimenti se ne importerebbe solo la coda.
            progress.SetMessaggioAvanzamento("Rendimenti dei prodotti Earn...");
            long inizioGiornata = FunzioniDate.ConvertiDatainLongSecondo(
                    FunzioniDate.ConvertiDatadaLongAlSecondo(startDate).substring(0, 10) + " 00:00:00");
            //Gli accrediti sono orari: al primo scaricamento, dove startDate è il 2017 di default, si
            //chiederebbero anni di storico un'ora alla volta, si sbatterebbe contro il tetto di pagine e
            //l'utente vedrebbe un avviso di scaricamento incompleto senza potervi rimediare. Ci si ferma
            //quindi allo stesso orizzonte di 3 mesi degli altri endpoint OKX.
            long limiteEarn = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000;
            if (inizioGiornata < limiteEarn) {
                System.out.println("Rendimenti Earn: richiesta limitata a 3 mesi, da "
                        + FunzioniDate.ConvertiDatadaLongAlSecondo(limiteEarn));
                inizioGiornata = limiteEarn;
            }
            JsonObject jsonEarn = fetchMovimento(exchangeId, apiKey, secret, inizioGiornata, "", "OKX_Earn", passphrase, hostnameOKX);

            //Interruzione durante i rendimenti Earn: senza questo controllo il ramo "else" qui sotto la
            //scambiava per un'assenza di rendimenti - jsonEarn e' null tanto quando OKX non risponde quanto
            //quando l'utente ha premuto INTERROMPI e il processo Node e' stato ucciso - e l'importazione
            //proseguiva fino in fondo, scaricando poi i prezzi dei movimenti appena inseriti. Premere
            //INTERROMPI deve fermare tutto, non solo lo script in corso.
            if (Principale.InterrompiCiclo || progress.ErroriNodeJS()) {
                if (archivioDaRecuperare) ArchivioOKXAbbandonato();
                JOptionPane.showConfirmDialog(null, "Impot terminato prematuramente!!","Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
                return null;
            }
            //Un errore dell'API sui rendimenti ferma l'importazione come tutte le altre chiamate dello stesso
            //scaricamento. jsonEarn e' null solo in caso di guaio: fetchMovimento restituisce null se lo
            //script non parte, se esce con codice diverso da zero o se il JSON porta un "error"; quando
            //semplicemente non ci sono prodotti Earn lo script risponde con savings_lending vuoto.
            //Fino al 13/08/2026 questo caso proseguiva con "Nessun rendimento Earn restituito da OKX",
            //importando i bill e lasciando fuori i rendimenti senza che l'utente lo sapesse.
            if (jsonEarn == null) {
                if (archivioDaRecuperare) ArchivioOKXAbbandonato();
                JOptionPane.showConfirmDialog(null, "Errore nella comunicazione con OKX durante lo scaricamento\n"
                        + "dei rendimenti dei prodotti Earn: importazione non eseguita.\n\n"
                        + "Nel log trovi il dettaglio dell'errore.",
                        "Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null);
                System.out.println("Nessun rendimento Earn restituito da OKX: importazione interrotta.");
                return null;
            }
            if (jsonEarn.has("savings_lending") && jsonEarn.get("savings_lending").isJsonArray()) {
                List<String[]> re = convertOKXEarn(jsonEarn.getAsJsonArray("savings_lending"));
                if (re == null) {
                    if (archivioDaRecuperare) ArchivioOKXAbbandonato();
                    JOptionPane.showConfirmDialog(null, "Impot terminato prematuramente!!","Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
                    return null;
                }
                righe.addAll(re);
                if (jsonEarn.has("savings_lending_completo") && !jsonEarn.get("savings_lending_completo").getAsBoolean()) {
                    JOptionPane.showConfirmDialog(null,
                            "Lo scaricamento dei rendimenti Earn non ha coperto tutto il periodo richiesto.\n\n"
                            + "Gli interessi sono accreditati ogni ora e OKX li restituisce cento alla volta, "
                            + "quindi lo storico più vecchio può non essere raggiungibile. Le giornate scaricate "
                            + "sono comunque corrette: nel log trovi da quando partono.",
                            "Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null);
                }
            } else {
                //Risposta valida ma senza l'array: nessun prodotto Earn sul conto, non c'e' nulla da aggiungere
                System.out.println("Nessun rendimento Earn restituito da OKX.");
            }

            //Ultimo cancello prima di toccare la mappa dei movimenti: la conversione dei rendimenti e la
            //deduplica non sono istantanee e l'utente puo' aver premuto INTERROMPI proprio li'. Da qui in
            //poi l'importazione scrive, e dietro le scritture parte lo scaricamento dei prezzi.
            if (Principale.InterrompiCiclo || progress.ErroriNodeJS()) {
                if (archivioDaRecuperare) ArchivioOKXAbbandonato();
                JOptionPane.showConfirmDialog(null, "Impot terminato prematuramente!!","Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
                return null;
            }

            //Se lo script non è riuscito a scorrere tutto lo storico richiesto è meglio saperlo prima di importare
            boolean completo = !json.has("okx_completo") || json.get("okx_completo").getAsBoolean();
            if (!completo) {
                JOptionPane.showConfirmDialog(null,
                        "Lo scaricamento da OKX non è stato completato interamente:\n"
                        + "alcuni movimenti potrebbero mancare. Controlla il log.",
                        "Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null);
            }

            Importazioni.Ex_OKX_ImportaDaAPI(righe);

            //Il consolidamento fa da solo la parte piu' lunga dell'importazione — scarica i prezzi mancanti,
            //un processo Node per quotazione — quindi INTERROMPI puo' arrivare li' dentro. In quel caso
            //Ex_OKX_ImportaDaAPI esce senza scrivere nulla e qui si chiude come per ogni altra interruzione.
            if (Interruzione.Richiesta()) {
                if (archivioDaRecuperare) ArchivioOKXAbbandonato();
                JOptionPane.showConfirmDialog(null, "Impot terminato prematuramente!!","Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,null);
                return null;
            }
            //I movimenti dell'archivio sono ora in mappa: solo adesso i trimestri arrivati possono
            //smettere di essere sospesi. Scriverlo prima avrebbe dichiarato "fatto" un trimestre le cui
            //righe una delle uscite anticipate qui sopra avrebbe potuto ancora buttare via.
            if (archivioDaRecuperare) {
                trimestriSospesiOKX(ArchivioOKX_SospesiSeCompletata);
                archivioOKXDaCompletare(!ArchivioOKX_SospesiSeCompletata.isEmpty());
            }
            if (Importazioni.TrasazioniSconosciute > 0) {
                //I codici bill di OKX non ancora mappati finiscono nel resoconto, ma restano anche a log
                System.out.println("Causali OKX non mappate:\n"+Importazioni.movimentiSconosciuti);
            }
            //Ex_OKX_ImportaDaAPI azzera e rivalorizza i contatori statici, quindi qui sono suoi di sicuro
            return Importazioni.Esito.daiContatori(exchangeId);
        }

        //Exchange non gestito da questa funzione: nessun resoconto da mostrare
        return null;
    }
    
    /** @param S valore decimale come stringa
     *  @return {@code S} come {@link BigDecimal} */
    public static BigDecimal BG(String S){
        return new BigDecimal(S);
    }

    /**
     * Converte i "bills" grezzi di OKX (l'equivalente API delle righe del CSV) nel formato intermedio a 19
     * campi atteso da {@link Importazioni#Ex_OKX_RaggruppaEConsolida}, così che import da CSV e import da API
     * condividano interamente classificazione e consolidamento.
     *
     * <p><b>Le causali vengono espresse con le stesse etichette testuali del CSV</b> ({@code "Buy"},
     * {@code "Sell"}, {@code "deposit"}, {@code "withdrawal"}), non con i codici numerici di OKX: in questo
     * modo {@link Importazioni#Ex_OKX_MappaCausali} resta l'unica tabella di conversione delle causali OKX.
     *
     * <p>La decodifica dei codici {@code type} è in {@link #causaleBillOKX}. Per il conto Trading
     * {@code Buy}/{@code Sell} si ricavano dal <b>segno di {@code balChg}</b>, senza bisogno del
     * {@code subType}: le due righe di uno swap hanno lo stesso {@code ts} e segni opposti, che è
     * esattamente ciò che il raggruppamento per orario si aspetta. I codici non riconosciuti restano
     * <b>non mappati</b>, con il codice grezzo in causale: finiscono così nel riepilogo dei movimenti
     * sconosciuti già gestito dall'import, che diventa l'elenco esatto dei codici ancora da decodificare.
     * È una scelta deliberata: meglio un movimento segnalato che un movimento classificato male.
     *
     * <p><b>Le commissioni vengono scorporate.</b> Nei bill di OKX {@code balChg} è la variazione <i>netta</i>
     * di saldo, cioè la quantità già decurtata della commissione ({@code balChg = sz + fee}, con {@code fee}
     * negativo; verificato su tutti i bill reali scaricati, dove la fee è sempre addebitata sulla gamba in
     * <b>entrata</b> e sempre nella stessa moneta del bill). Importare direttamente {@code balChg}
     * porterebbe in carico solo il netto, cioè <b>dedurrebbe di fatto la commissione dal costo di carico</b>:
     * per le cripto-attività l'art. 68 c.9-bis del TUIR (e la circolare 30/E del 27/10/23) non lo consente,
     * a differenza di quanto vale per le attività finanziarie con il c.6.
     *
     * <p>Si riporta quindi in {@code [6]} la quantità <b>lorda</b> ({@code balChg - fee}) e la commissione in
     * {@code [11]}/{@code [12]}, da cui {@link Importazioni#Ex_OKX_Consolida} genera un secondo movimento di
     * tipo {@code COMMISSIONI}. Il costo di carico resta così sull'intero importo scambiato e la commissione
     * diventa una cessione a sé stante, con la sua eventuale plusvalenza accessoria.
     *
     * <p>La commissione deve essere elaborata <b>dopo</b> lo scambio, perché cede la moneta che lo scambio ha
     * appena accreditato. Lo garantisce l'ordinamento delle chiavi: {@code Calcoli_PlusvalenzeNew} scorre
     * {@code MappaCryptoWallet.values()}, e la mappa è una {@code TreeMap} con
     * {@code String.CASE_INSENSITIVE_ORDER}. Qui lo scambio ha identificativo {@code OKX$IDE$} e la
     * commissione {@code OKX<billId>C}, e {@code '$'} precede le cifre. Attenzione che il confronto è
     * <b>case-insensitive</b>, quindi confronta i caratteri in minuscolo: è per questo che il suffisso
     * {@code "C"} che {@code creaMovimento} accoda ai movimenti {@code CM} in uscita li porta dopo e non
     * prima ({@code '_'} = 0x5F precede {@code 'c'} = 0x63, mentre precederebbe {@code 'C'} = 0x43 in un
     * confronto sensibile alle maiuscole).
     *
     * @param bills array JSON dei bill grezzi restituiti da {@code OKX_Bills.js}, oppure {@code null}
     * @param Wallet nome del conto di provenienza, {@code "Funding"} oppure {@code "Trading"}
     * @return le righe in formato intermedio a 19 campi (non ordinate)
     */
    /**
     * Traduce il codice numerico {@code type} di un bill OKX nella causale testuale corrispondente, cioè
     * nella stessa etichetta che comparirebbe nella colonna "Type"/"Action" dell'export CSV.
     *
     * <p>OKX non pubblica la tabella dei codici in forma consultabile: quelli qui sotto sono stati
     * <b>ricavati per confronto</b> fra i bill scaricati via API e le righe degli export CSV dello stesso
     * periodo (cartella {@code okx/} del dataset di prova), verificando la corrispondenza su data, moneta e
     * importo. I conteggi coincidono esattamente: 29 bill Trading {@code type=1} ↔ 29 righe
     * "Transfer in"/"Transfer out", 22 bill {@code type=12} ↔ 22 righe "Transfer" con azione vuota.
     *
     * <table><caption>Codici riconosciuti</caption>
     * <tr><th>conto</th><th>type</th><th>riga CSV corrispondente</th><th>trattamento</th></tr>
     * <tr><td>Trading</td><td>2</td><td>Spot Buy / Spot Sell</td><td>scambio</td></tr>
     * <tr><td>Trading</td><td>1</td><td>Transfer in / Transfer out</td><td>giroconto interno</td></tr>
     * <tr><td>Trading</td><td>12</td><td>Transfer (azione vuota, coppie +/-)</td><td>giroconto interno</td></tr>
     * <tr><td>Funding</td><td>1</td><td>Deposit</td><td>trasferimento crypto</td></tr>
     * <tr><td>Funding</td><td>2</td><td>Withdrawal</td><td>trasferimento crypto</td></tr>
     * <tr><td>Funding</td><td>48</td><td>Received</td><td>trasferimento crypto</td></tr>
     * <tr><td>Funding</td><td>130</td><td>From unified trading account</td><td>giroconto interno</td></tr>
     * <tr><td>Funding</td><td>131</td><td>To unified trading account</td><td>giroconto interno</td></tr>
     * <tr><td>Funding</td><td>75</td><td>Simple Earn subscription</td><td>giroconto interno</td></tr>
     * <tr><td>Funding</td><td>76</td><td>Simple Earn redemption</td><td>giroconto interno</td></tr>
     * <tr><td>Funding</td><td>326 / 327</td><td>Data migration out / in</td><td>giroconto interno</td></tr>
     * </table>
     *
     * <p>Dal 03/08/2026 la tabella non è più ricavata solo per confronto con il CSV: l'endpoint
     * {@code asset/bills-history} restituisce anche il campo {@code notes}, che porta l'etichetta in chiaro
     * del tipo ({@code "Simple Earn subscription"}, {@code "Data migration out"}, …). È da lì che vengono le
     * righe 75 e 326/327 di questa tabella, e per questo i tipi non riconosciuti riportano {@code notes} nella
     * causale: il prossimo codice ignoto arriva già con la propria descrizione, senza bisogno di indagini.
     *
     * <p>Tutti i giroconti interni a OKX sono resi con le etichette {@code "Transfer in"}/{@code "Transfer out"},
     * che {@link Importazioni#Ex_OKX_MappaCausali} associa a {@code NON CONSIDERARE}: sono spostamenti fra
     * conti dello stesso utente e non vengono importati, come già si fa per Binance. Le tre righe della
     * tabella che sono giroconti verso un prodotto Earn ricadono nella stessa categoria per lo stesso motivo.
     *
     * <p>{@code Received} è invece un accredito proveniente da un <b>altro</b> account OKX: è crypto che entra
     * davvero nel wallet, quindi viene trattato come un deposito e non come un giroconto.
     *
     * <p>I codici {@code 326}/{@code 327} compaiono solo a coppie di segno opposto sulla stessa moneta, sullo
     * stesso istante, e non hanno alcuna riga corrispondente nell'export CSV. Il campo {@code notes} li
     * identifica come {@code "Data migration out"} e {@code "Data migration in"}: sono le due gambe della
     * <b>migrazione dei dati fra entità regionali di OKX</b>, non movimenti reali. Restano fra i giroconti
     * interni, cioè non importati, che è il trattamento corretto: le due gambe si compensano esattamente.
     *
     * @param tipo valore del campo {@code type} del bill
     * @param balChg variazione di saldo, usata per ricavare il verso del movimento
     * @param isTrading {@code true} se il bill viene dal conto Trading, {@code false} dal Funding
     * @return la causale testuale, oppure {@code "OKX type <n>"} se il codice non è fra quelli riconosciuti
     */
    static String causaleBillOKX(String tipo, String balChg, boolean isTrading) {
        return causaleBillOKX(tipo, balChg, isTrading, "");
    }

    /**
     * Come {@link #causaleBillOKX(String, String, boolean)}, ma per i codici non riconosciuti riporta anche
     * l'etichetta in chiaro che OKX espone nel campo {@code notes} del bill.
     *
     * @param tipo valore del campo {@code type} del bill
     * @param balChg variazione di saldo, usata per ricavare il verso del movimento
     * @param isTrading {@code true} se il bill viene dal conto Trading, {@code false} dal Funding
     * @param notes campo {@code notes} del bill; può essere vuoto o {@code null}
     * @return la causale testuale, oppure {@code "OKX type <n> (<notes>)"} se il codice non è riconosciuto
     */
    static String causaleBillOKX(String tipo, String balChg, boolean isTrading, String notes) {
        String sconosciuto = "OKX type " + tipo;
        if (notes != null && !notes.isBlank()) sconosciuto = sconosciuto + " (" + notes.trim() + ")";
        //Un giroconto interno è reso col verso giusto: le due etichette sono entrambe NON CONSIDERARE,
        //ma tenerle distinte mantiene leggibile il movimento in caso di diagnostica.
        String giroconto = Funzioni.isNegativo(balChg) ? "Transfer out" : "Transfer in";

        if (isTrading) {
            switch (tipo) {
                case "2":  return Funzioni.isNegativo(balChg) ? "Sell" : "Buy";
                case "1":
                case "12": return giroconto;
                default:   return sconosciuto;   //non mappato: finirà tra i movimenti sconosciuti
            }
        }
        switch (tipo) {
            case "1":   return "deposit";
            case "2":   return "withdrawal";
            case "48":  return "Received";
            //61: la conversione rapida di OKX ("Convert"), quella con cui si scambiano due monete senza
            //passare da un ordine di mercato. Riconosciuta il 04/08/2026 sui bill reali: le 12 righe
            //compaiono sempre a coppie di segno opposto sullo stesso istante (SHIB->USDT, MRST->USDT,
            //USDT<->USDC), che è esattamente ciò che il raggruppamento per orario si aspetta da uno scambio.
            //L'etichetta "Convert" è già una chiave di Ex_OKX_MappaCausali, usata dall'import del CSV.
            case "61":  return "Convert";
            //75/76: sottoscrizione e riscatto di Simple Earn, cioè spostamenti fra il conto Funding e il
            //prodotto Earn dello stesso utente. Il 75 è stato riconosciuto il 03/08/2026 dal campo notes.
            case "75":
            case "76":
            //80/82: sottoscrizione e riscatto dei "Flash Deals", che sono lo stesso meccanismo di Simple
            //Earn con un altro nome. Sui dati reali il capitale esce e rientra identico (380.59095613 USDT
            //usciti il 30/12/2022 e rientrati il 06/01/2023): è un giroconto, il rendimento arriva a parte
            //con il codice 89.
            case "80":
            case "82":
            case "130":
            case "131":
            //311: l'accredito che arriva dal conto Trading. Non è il 130 con un altro nome: le due righe
            //del 20/01/2024 hanno per contropartita esatta — stesso istante, stesso importo di segno
            //opposto — due righe subType 290 dell'archivio del conto Trading, vedi tipoDaArchivioOKX.
            case "311":
            //326/327: le due gambe della migrazione fra entità regionali di OKX, vedi javadoc
            case "326":
            case "327": return giroconto;
            //89: il rendimento dei Flash Deals, accreditato in una moneta diversa da quella investita
            //(93.84434534 MRST a fronte di USDT vincolati). È un provento, non un giroconto, e va trattato
            //come gli interessi di Simple Earn.
            case "89":  return "Flash Deals Earnings";
            //189: omaggio promozionale ("mystery box"), 9127.55842556 SHIB il 31/07/2022. Entra davvero nel
            //wallet senza contropartita: come gli altri accrediti gratuiti vale un reward.
            case "189": return "Mystery box bonus";
            default:    return sconosciuto;      //non mappato: finirà tra i movimenti sconosciuti
        }
    }

    /**
     * Converte lo storico degli interessi di OKX Simple Earn ({@code finance/savings/lending-history}) nelle
     * righe in formato intermedio a 19 campi, <b>aggregando per giorno e per moneta</b>.
     *
     * <p>L'aggregazione non è un'ottimizzazione ma una necessità: OKX accredita gli interessi <b>ogni ora</b>
     * (verificato sui dati reali: un record per moneta per ora, con importi dell'ordine di {@code 0.00000003}
     * BTC), il che su tre monete fa circa 72 movimenti al giorno e 26.000 all'anno, ognuno con la propria
     * ricerca prezzo. Sommandoli per giornata restano circa 3 movimenti al giorno: il rendimento imponibile è
     * lo stesso, cambia solo il prezzo usato per convertirlo in euro — uno di fine giornata invece di 24.
     *
     * <p>Il movimento porta la data dell'<b>ultimo</b> accredito della giornata per quella moneta e la causale
     * {@code "Deposit yield"}, che {@link Importazioni#Ex_OKX_MappaCausali} associa già a {@code REWARD}: non
     * nasce quindi nessuna combinazione campo5/campo18/categoria nuova.
     *
     * <p>La <b>giornata in corso viene esclusa</b>, perché è ancora incompleta: verrebbe importata a metà e,
     * essendo l'ID deterministico, la deduplica impedirebbe poi di completarla. Verrà importata per intero
     * allo scaricamento successivo.
     *
     * <p>L'ID originale {@code [14]} è costruito come {@code EARN-<moneta>-<aaaammgg>}: è deterministico, così
     * uno scaricamento ripetuto sullo stesso periodo riconosce le giornate già importate invece di duplicarle.
     *
     * @param lending array JSON dei record restituiti da {@code OKX_Earn.js}, oppure {@code null}
     * @return le righe in formato intermedio a 19 campi, una per giornata e moneta
     */
    public static List<String[]> convertOKXEarn(JsonArray lending) {
        List<String[]> lista = new ArrayList<>();
        if (lending == null) return lista;

        String oggi = FunzioniDate.ConvertiDatadaLongAlSecondo(System.currentTimeMillis()).substring(0, 10);

        //chiave = giorno + moneta, così la somma è per giornata e valuta
        Map<String, BigDecimal> somme = new TreeMap<>();
        Map<String, String> monete = new TreeMap<>();
        Map<String, Long> ultimoTs = new TreeMap<>();
        //I record non hanno un id: moneta+timestamp è l'unica chiave disponibile (sui dati reali è univoca).
        //Serve perché qui si somma, e un record ripetuto dalla paginazione gonfierebbe il totale del giorno
        //senza che nulla se ne accorga.
        Set<String> visti = new HashSet<>();

        for (JsonElement el : lending) {
            if (Principale.InterrompiCiclo) return null;
            JSONObject obj = new JSONObject(el.toString());

            String ccy      = obj.optString("ccy", "");
            String earnings = obj.optString("earnings", "");
            String ts       = obj.optString("ts", "");

            if (ccy.isEmpty() || earnings.isEmpty() || ts.isEmpty()) continue;
            if (!Funzioni.isNumeric(earnings, false) || !Funzioni.isNumeric(ts, false)) continue;
            if (new BigDecimal(earnings).compareTo(BigDecimal.ZERO) == 0) continue;
            if (!visti.add(ccy + "|" + ts)) continue;   //stesso accredito già conteggiato

            long tsLong = Long.parseLong(ts);
            String giorno = FunzioniDate.ConvertiDatadaLongAlSecondo(tsLong).substring(0, 10);
            if (giorno.equals(oggi)) continue;   //giornata ancora in corso: si importa domani, intera

            String chiave = giorno + "|" + ccy;
            somme.merge(chiave, new BigDecimal(earnings), BigDecimal::add);
            monete.put(chiave, ccy);
            ultimoTs.merge(chiave, tsLong, Math::max);
        }

        for (Map.Entry<String, BigDecimal> voce : somme.entrySet()) {
            String chiave = voce.getKey();
            if (voce.getValue().compareTo(BigDecimal.ZERO) == 0) continue;

            String DatoRiga[] = new String[19];
            DatoRiga[0]  = FunzioniDate.ConvertiDatadaLongAlSecondo(ultimoTs.get(chiave));  //Timestamp
            DatoRiga[1]  = "OKX";                                                           //Exchange
            DatoRiga[2]  = "Funding";                                                       //Conto: l'Earn fa capo al Funding
            DatoRiga[3]  = "";                                                              //Categoria: la assegna l'import
            DatoRiga[4]  = "Deposit yield";                                                 //Causale già mappata su REWARD
            DatoRiga[5]  = monete.get(chiave);                                              //Moneta
            DatoRiga[6]  = voce.getValue().stripTrailingZeros().toPlainString();            //Interessi del giorno
            DatoRiga[11] = "";                                                              //Nessuna fee sugli interessi
            DatoRiga[12] = "";
            DatoRiga[14] = "EARN-" + monete.get(chiave) + "-" + chiave.substring(0, 10).replace("-", "");
            DatoRiga[15] = "NO";
            DatoRiga[16] = "";
            Importazioni.RiempiVuotiArray(DatoRiga);
            lista.add(DatoRiga);
        }
        return lista;
    }

    /**
     * Traduce la coppia {@code instType}/{@code subType} del CSV d'archivio nel codice {@code type} usato dai
     * bill JSON, così che la classificazione resti governata da {@link #causaleBillOKX} e non esista un
     * secondo classificatore da tenere allineato.
     *
     * <p>Il CSV dell'archivio <b>non ha la colonna {@code type}</b>: espone solo {@code subType}, che è un
     * vocabolario diverso. La corrispondenza qui sotto non è dedotta dalla documentazione ma <b>ricavata sui
     * dati</b>: i 112 {@code billId} dell'archivio 2026 Q2 sono tutti presenti anche fra i bill scaricati via
     * API, quindi per ogni riga è stato possibile leggere il {@code type} corrispondente.
     *
     * <table><caption>Corrispondenza verificata il 03/08/2026, riga 290 aggiunta il 04/08/2026</caption>
     * <tr><th>instType</th><th>subType</th><th>type</th><th>significato</th></tr>
     * <tr><td>SPOT</td><td>1 / 2</td><td>2</td><td>gambe di uno scambio spot</td></tr>
     * <tr><td>-</td><td>11 / 12</td><td>1</td><td>giroconto col conto Funding</td></tr>
     * <tr><td>-</td><td>200 / 202</td><td>12</td><td>giroconto interno</td></tr>
     * <tr><td>-</td><td>290</td><td>1</td><td>uscita verso il conto Funding</td></tr>
     * </table>
     *
     * <p>Il {@code subType} 290 non viene dall'incrocio dei {@code billId} come gli altri — l'archivio è
     * l'unica fonte che lo espone — ma dalla sua contropartita sul conto Funding: le due righe del
     * 20/01/2024 (ETH {@code 0.000000654} e BTC {@code 0.0000000043100594}) hanno stesso istante e stesso
     * importo di segno opposto rispetto a due bill {@code type=311}, che OKX etichetta "Transfer in from
     * trading account". È quindi l'uscita di un giroconto, e va sul {@code type} 1 come 11/12.
     *
     * <p>Il verso non si ricava dal {@code subType} ma sempre dal segno di {@code balChg}, come già fa
     * {@code causaleBillOKX}: sui dati reali le 44 righe {@code subType=1} hanno tutte {@code balChg}
     * positivo e le 44 {@code subType=2} tutte negativo, quindi le due letture coincidono e quella per segno
     * vale anche per i tipi non spot.
     *
     * @param instType valore della colonna {@code instType} ({@code "SPOT"} oppure {@code "-"})
     * @param subType valore della colonna {@code subType}
     * @return il codice {@code type} corrispondente, oppure stringa vuota se la combinazione è ignota, così
     *         che il movimento finisca fra quelli sconosciuti invece di essere classificato a caso
     */
    /**
     * Recupera dall'archivio storico trimestrale di OKX i movimenti del conto Trading più vecchi dei 3 mesi
     * coperti da {@code account/bills-archive}, e li restituisce già nel formato intermedio a 19 campi.
     *
     * <p>Lo script {@code OKX_Archivio.js} lavora in tre fasi: interroga tutti i trimestri (la lettura costa
     * pochissimo e trova gratis i file già generati in passato), chiede la generazione di <b>tutti</b> quelli
     * mai richiesti, poi attende fino a 10 minuti ritirando ciascun file appena è pronto.
     *
     * <p>Fino al 04/08/2026 ne veniva chiesto <b>uno solo per esecuzione</b>, sulla base del costo che CCXT
     * assegna a quella chiamata. Misurato sul campo, quel limite non è applicato dal server: 15 richieste
     * consecutive sono state tutte accettate e 11 file erano pronti entro 5 minuti. Un rifiuto resta però
     * gestito, perché non è escluso che una soglia più alta esista: al primo "troppe richieste" lo script si
     * ferma, tiene quel che ha ottenuto e lascia il resto alla prossima esecuzione, che ripartirà di lì
     * ritirando gratis quanto nel frattempo è stato generato.
     *
     * <p>L'esito di ciascun trimestre viene riportato all'utente, altrimenti un recupero parziale — che è la
     * norma quando i trimestri sono molti — sarebbe indistinguibile da uno completo.
     *
     * @param exchangeId identificativo CCXT dell'exchange
     * @param apiKey API key dell'account
     * @param secret API secret dell'account
     * @param startDate data di partenza dello scaricamento, millisecondi epoch; <b>limita</b> l'elenco dei
     *                  trimestri, ai quali si aggiungono solo quelli rimasti indietro da un tentativo
     *                  precedente (vedi {@link #trimestriDaChiedereOKX})
     * @param passphrase passphrase di OKX
     * @param hostname dominio regionale già riconosciuto, stringa vuota se ancora ignoto
     * @param progress finestra di progresso su cui riportare l'avanzamento
     * @return le righe in formato intermedio a 19 campi, lista vuota se non c'è nulla da recuperare, oppure
     *         {@code null} se l'utente ha interrotto o se una richiesta è andata in errore — in quest'ultimo
     *         caso l'avviso è già stato mostrato qui, e l'intera importazione non deve essere eseguita
     */
    static List<String[]> ScaricaArchivioOKX(String exchangeId, String apiKey, String secret, long startDate,
            String passphrase, String hostname, Download progress) {

        List<String> sospesi = trimestriSospesiOKX();
        List<String> trimestri = trimestriDaChiedereOKX(startDate, System.currentTimeMillis(),
                annoInizioArchivioOKX(), sospesi);
        //Finche' non si sa come e' finita, l'unica cosa certa e' che quello che c'era prima resta da fare
        ArchivioOKX_SospesiSeCompletata = new ArrayList<>(sospesi);
        ArchivioOKX_SospesiSeAbbandonata = new ArrayList<>(trimestri);
        if (trimestri.isEmpty()) {
            System.out.println("Archivio storico OKX: nessun trimestre da recuperare.");
            //Non c'e' nulla da chiedere: un eventuale sospeso non e' piu' completabile e tenerlo acceso
            //farebbe riaprire il dialogo a ogni scaricamento senza che l'utente possa farci nulla.
            archivioOKXDaCompletare(false);
            trimestriSospesiOKX(new ArrayList<>());
            ArchivioOKX_SospesiSeCompletata = new ArrayList<>();
            ArchivioOKX_SospesiSeAbbandonata = new ArrayList<>();
            return new ArrayList<>();
        }
        if (!sospesi.isEmpty()) {
            System.out.println("Archivio storico OKX, trimestri rimasti indietro dall'ultimo tentativo: "
                    + String.join(", ", sospesi));
        }

        if (progress != null) progress.SetMessaggioAvanzamento("Archivio storico OKX (può richiedere qualche minuto)...");
        System.out.println("Archivio storico OKX, trimestri richiesti: " + String.join(", ", trimestri));

        JsonObject json = fetchMovimento(exchangeId, apiKey, secret, startDate,
                String.join(",", trimestri), "OKX_Archivio", passphrase, hostname);

        if (Principale.InterrompiCiclo) return null;
        if (json == null) {
            //Fino al 13/08/2026 qui si proseguiva importando lo scaricamento ordinario. Un errore su questa
            //chiamata e' pero' un errore dello stesso scaricamento: proseguire significa scrivere movimenti
            //e scaricarne i prezzi mentre una parte della storia e' rimasta fuori. Del ritentare se ne
            //occupa il chiamante, che su ra == null chiama ArchivioOKXAbbandonato().
            JOptionPane.showConfirmDialog(null, "Nessuna risposta da OKX per l'archivio storico.\n\n"
                    + "L'importazione è stata interrotta e non è stato inserito nulla:\n"
                    + "nel log trovi il dettaglio dell'errore. Rilancia lo scaricamento\n"
                    + "quando il problema è risolto.",
                    "Attenzione", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null);
            return null;
        }

        //Il riepilogo per trimestre è la parte utile del messaggio: dice cosa è entrato e cosa va ritentato.
        StringBuilder riepilogo = new StringBuilder();
        boolean daRitentare = false;
        boolean conErrori = false;
        List<String> rimasti = new ArrayList<>();
        List<String> ignorati = new ArrayList<>();
        if (json.has("okx_periodi") && json.get("okx_periodi").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("okx_periodi")) {
                JsonObject p = el.getAsJsonObject();
                String periodo = p.has("periodo") ? p.get("periodo").getAsString() : "?";
                String stato = p.has("stato") ? p.get("stato").getAsString() : "?";
                riepilogo.append("  ").append(periodo).append(" : ").append(stato);
                if (p.has("righe")) riepilogo.append(" (").append(p.get("righe").getAsString()).append(" movimenti)");
                if (p.has("dettaglio")) riepilogo.append(" - ").append(p.get("dettaglio").getAsString());
                riepilogo.append("\n");
                if (!stato.equals("scaricato")) daRitentare = true;
                //"errore" e' l'unico stato che segnala un guaio: "in preparazione" e "da richiedere" sono
                //passaggi normali dell'archivio, che si completa da solo alla corsa successiva.
                if (stato.equals("errore")) conErrori = true;
                //I trimestri non arrivati vanno ricordati per nome: dal 13/08/2026 l'elenco e' limitato
                //alla data di partenza, e senza questa memoria alla corsa successiva — con la partenza
                //ormai recente — non verrebbero piu' chiesti. "ignorato" no: e' un trimestre che OKX non
                //copre, chiederlo di nuovo non servirebbe a niente.
                if (!stato.equals("scaricato") && !stato.equals("ignorato")) rimasti.add(periodo);
                //Un trimestre che OKX non copre non va richiesto di nuovo nemmeno se la corsa viene
                //abbandonata: non c'e' nulla da recuperare li'.
                if (stato.equals("ignorato")) ignorati.add(periodo);
            }
        }
        System.out.println("Archivio storico OKX:\n" + riepilogo);
        ArchivioOKX_SospesiSeCompletata = rimasti;
        ArchivioOKX_SospesiSeAbbandonata.removeAll(ignorati);

        //Con la data di partenza ancorata all'ultimo movimento, questo flag e' l'unica cosa che riporta
        //l'utente dentro il dialogo per ritirare i trimestri rimasti indietro: senza, il prossimo
        //scaricamento partirebbe da ieri e il dialogo non si aprirebbe piu'.
        archivioOKXDaCompletare(daRitentare);

        //Un trimestre andato in errore ferma tutta l'importazione, come ogni altra chiamata fallita di
        //questo scaricamento: i trimestri riusciti non si perdono, perche' i file gia' generati restano
        //pronti sul server di OKX e la corsa successiva li ritira senza rispendere una richiesta.
        if (conErrori) {
            JOptionPane.showConfirmDialog(null,
                    "Archivio storico OKX: una o più richieste hanno dato errore.\n\n" + riepilogo + "\n"
                    + "L'importazione è stata interrotta e non è stato inserito nulla. I trimestri già "
                    + "generati restano pronti su OKX: risolto il problema, rilancia lo scaricamento e "
                    + "verranno ritirati senza rifare la richiesta.",
                    "Archivio storico OKX", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null);
            return null;
        }

        JsonArray archivio = json.has("okx_archivioBills") ? json.getAsJsonArray("okx_archivioBills") : new JsonArray();
        List<String[]> righe = filtraArchivioOKXPerData(convertOKXArchivio(archivio), startDate, sospesi);

        if (daRitentare) {
            JOptionPane.showConfirmDialog(null,
                    "Archivio storico OKX: recupero parziale.\n\n" + riepilogo + "\n"
                    + "I trimestri ancora in preparazione vengono generati da OKX anche adesso: rilancia lo "
                    + "scaricamento fra qualche minuto e verranno ritirati senza rifare la richiesta. Quelli "
                    + "eventualmente indicati come «da richiedere» sono rimasti fuori perché OKX ha smesso di "
                    + "accettare richieste: verranno chiesti al prossimo tentativo.",
                    "Archivio storico OKX", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
        }
        return righe;
    }

    /**
     * Scarta dalle righe dell'archivio storico OKX quelle anteriori alla data di partenza dello
     * scaricamento.
     *
     * <p><b>Perche' serve.</b> L'archivio si chiede per <b>trimestri interi</b>, quindi il trimestre che
     * contiene {@code startDate} torna dal suo primo giorno: la coda che precede la partenza copre un
     * periodo gia' importato. Il ramo dei bill ordinari questo filtro ce l'ha ({@code Scripts/OKX_Bills.js},
     * {@code if (ts < startTime) continue}); il ramo dell'archivio no, e si reggeva sulla sola deduplica
     * per ID a valle. Che bastasse era un'illusione: quella deduplica confronta {@code [24]} carattere per
     * carattere, e uno scambio importato prima del 02/04/2026 porta le due gambe unite da {@code _} invece
     * che da {@code -}, quindi non viene riconosciuto e il movimento entra due volte (difetto <b>C12</b>).
     *
     * <p><b>L'eccezione dei trimestri sospesi non e' un dettaglio.</b> Un trimestre rimasto indietro da un
     * tentativo precedente ({@link #OPZIONE_ARCHIVIO_SOSPESI_OKX}) e' di norma <b>anteriore</b> alla
     * partenza, perche' nel frattempo lo scaricamento ordinario ha portato avanti l'ultimo movimento. Un
     * filtro cieco lo svuoterebbe, ma {@code okx_periodi} lo dichiarerebbe comunque {@code scaricato} e la
     * corsa successiva lo toglierebbe dai sospesi: il trimestre diventerebbe irrecuperabile in silenzio,
     * cioe' proprio cio' che i sospesi esistono per evitare.
     *
     * <p>Il confronto e' {@code ts < startDate}, e {@code startDate} e' gia' l'ultimo movimento + 1 s: e'
     * la stessa soglia, con lo stesso verso, del filtro di {@code OKX_Bills.js}. Tenerla identica sulle due
     * strade vale piu' del secondo di margine che entrambe perdono.
     *
     * <p>Una riga con data illeggibile viene <b>tenuta</b>: senza data non si puo' dimostrare che sia gia'
     * stata importata, e scartarla perderebbe un movimento per un dato che manca.
     *
     * @param righe righe in formato intermedio a 19 campi; {@code null} passa attraverso
     * @param startDate data di partenza dello scaricamento, millisecondi epoch
     * @param sospesi trimestri chiesti in recupero, che il filtro non tocca; puo' essere {@code null}
     * @return una nuova lista senza le righe gia' coperte dalla partenza, nello stesso ordine
     */
    static List<String[]> filtraArchivioOKXPerData(List<String[]> righe, long startDate, List<String> sospesi) {
        if (righe == null) return null;
        Set<String> recuperi = new HashSet<>();
        if (sospesi != null) recuperi.addAll(sospesi);

        List<String[]> tenute = new ArrayList<>();
        int scartate = 0;
        for (String[] r : righe) {
            long ts = (r != null && r.length > 0 && r[0] != null && !r[0].isBlank())
                    ? FunzioniDate.ConvertiDatainLongSecondo(r[0]) : 0;
            if (ts <= 0 || ts >= startDate || recuperi.contains(trimestreOKXdiData(ts))) {
                tenute.add(r);
            } else {
                scartate++;
            }
        }

        if (scartate > 0) {
            System.out.println("Archivio storico OKX: " + scartate + " righe scartate perche' anteriori a "
                    + FunzioniDate.ConvertiDatadaLongAlSecondo(startDate) + ", data da cui parte lo scaricamento"
                    + (recuperi.isEmpty() ? "." : " (trimestri in recupero esclusi dal filtro: "
                            + String.join(", ", new TreeSet<>(recuperi)) + ")."));
        }
        return tenute;
    }

    /**
     * Etichetta del trimestre ({@code 2024Q2}) in cui cade un istante, nella stessa forma e nello stesso
     * fuso usati da {@link #trimestriArchivioOKX(long, long, int)} per comporre l'elenco: le due cose si
     * confrontano fra loro in {@link #filtraArchivioOKXPerData}, e derivarle da fusi diversi sposterebbe di
     * trimestre le righe a cavallo del confine.
     *
     * @param ms istante, millisecondi epoch
     * @return l'etichetta del trimestre
     */
    static String trimestreOKXdiData(long ms) {
        java.time.LocalDate d = java.time.Instant.ofEpochMilli(ms)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        return d.getYear() + "Q" + ((d.getMonthValue() - 1) / 3 + 1);
    }

    /**
     * Ricostruisce {@code balChg} a partire da {@code sz}, che nel CSV dell'archivio è il campo pulito,
     * <b>modificando l'oggetto sul posto</b>.
     *
     * <p>Le due fonti riportano lo stesso movimento con precisione diversa: l'API dà
     * {@code balChg = 0.00046266282}, il CSV dà {@code 0.0004626628199999993}, cioè lo stesso numero con la
     * sbavatura di un passaggio in virgola mobile. {@code sz} invece coincide esattamente in entrambe
     * ({@code 0.00046359}), quindi ricalcolare {@code balChg = segno × |sz| + fee} restituisce cifra per
     * cifra il valore dell'API. Senza questo passaggio lo stesso scambio importato dalle due strade
     * darebbe quantità diverse a partire dalla quindicesima cifra: irrilevante per l'imposta, ma è
     * sporcizia che non ha motivo di entrare nei calcoli.
     *
     * <p>La riscrittura avviene solo se {@code sz} è valorizzato e il valore ricostruito coincide con quello
     * dichiarato entro un margine minimo. Sui dati reali il controllo passa su tutte le 88 righe spot,
     * mentre le 24 righe di giroconto hanno {@code sz} a zero e restano com'erano: se un domani OKX
     * cambiasse il significato di {@code sz}, il controllo se ne accorgerebbe e non toccherebbe nulla.
     *
     * @param o riga dell'archivio, già in forma di bill
     */
    private static void ripulisciBalChgArchivioOKX(JsonObject o) {
        if (!o.has("sz") || !o.has("balChg")) return;
        String sz = o.get("sz").getAsString();
        String balChg = o.get("balChg").getAsString();
        String fee = o.has("fee") ? o.get("fee").getAsString() : "0";
        if (!Funzioni.isNumeric(sz, false) || !Funzioni.isNumeric(balChg, false)) return;
        if (!Funzioni.isNumeric(fee, false)) fee = "0";

        BigDecimal szBD = new BigDecimal(sz);
        if (szBD.compareTo(BigDecimal.ZERO) == 0) return;   //giroconti: sz non valorizzato

        BigDecimal vecchio = new BigDecimal(balChg);
        BigDecimal feeBD = new BigDecimal(fee).abs().negate();
        BigDecimal nuovo = (vecchio.signum() < 0 ? szBD.abs().negate() : szBD.abs()).add(feeBD);

        //Tolleranza relativa: si corregge la sbavatura, non si riscrive un valore diverso
        BigDecimal margine = vecchio.abs().multiply(new BigDecimal("0.000000001")).add(new BigDecimal("1E-18"));
        if (nuovo.subtract(vecchio).abs().compareTo(margine) <= 0) {
            o.addProperty("balChg", nuovo.toPlainString());
        }
    }

    static String tipoDaArchivioOKX(String instType, String subType) {
        if (instType != null && instType.trim().equalsIgnoreCase("SPOT")) return "2";
        if (subType == null) return "";
        switch (subType.trim()) {
            case "11":
            case "12":
            case "290": return "1";
            case "200":
            case "202": return "12";
            default:    return "";
        }
    }

    /**
     * Converte le righe del CSV dell'archivio storico ({@code Scripts/OKX_Archivio.js}) nel formato
     * intermedio a 19 campi, riusando integralmente {@link #convertOKXBills}.
     *
     * <p>L'unico adattamento è la sintesi del campo {@code type} tramite {@link #tipoDaArchivioOKX}, assente
     * dal CSV. Tutto il resto — verso, scorporo delle commissioni, causali, ID originale — passa dalla stessa
     * strada dello scaricamento a 3 mesi, ed è questo che fa sì che i movimenti dell'archivio abbiano
     * <b>gli stessi identificativi</b> di quelli già importati via API: i periodi sovrapposti si deduplicano
     * da soli invece di generare doppioni.
     *
     * <p>L'apostrofo iniziale con cui il CSV forza il testo per Excel ({@code '3702022358013911044}) viene
     * tolto anche qui e non solo nello script: è la condizione perché gli ID coincidano, e va garantita nel
     * punto in cui l'ID viene consumato.
     *
     * @param righe array JSON delle righe del CSV, oppure {@code null}
     * @return le righe in formato intermedio a 19 campi
     */
    public static List<String[]> convertOKXArchivio(JsonArray righe) {
        if (righe == null) return new ArrayList<>();

        JsonArray comeBill = new JsonArray();
        for (JsonElement el : righe) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject().deepCopy();

            String instType = o.has("instType") ? o.get("instType").getAsString() : "";
            String subType  = o.has("subType")  ? o.get("subType").getAsString()  : "";
            String tipo = tipoDaArchivioOKX(instType, subType);
            //Se la combinazione è ignota si conserva il subType nel codice, così il riepilogo dei movimenti
            //sconosciuti dice esattamente quale valore va decodificato.
            o.addProperty("type", tipo.isEmpty() ? "archivio subType " + subType : tipo);

            if (o.has("billId")) {
                String id = o.get("billId").getAsString();
                if (id.startsWith("'")) o.addProperty("billId", id.substring(1));
            }

            ripulisciBalChgArchivioOKX(o);
            comeBill.add(o);
        }
        return convertOKXBills(comeBill, "Trading");
    }

    /**
     * Toglie dalle righe grezze a 19 campi i bill che compaiono piu' di una volta, tenendo la prima
     * occorrenza.
     *
     * <p><b>Perche' serve.</b> Lo scaricamento OKX mette insieme finestre temporali che si sovrappongono:
     * {@code account/bills-archive} copre gli ultimi ~3 mesi, mentre l'archivio trimestrale di
     * {@code OKX_Archivio.js} copre il trimestre per intero. Le operazioni che cadono in entrambe arrivano
     * quindi due volte, con lo stesso {@code billId}.
     *
     * <p><b>Perche' va fatto qui e non dopo.</b> {@code Ex_OKX_Consolida} raggruppa le righe per istante e
     * accumula le gambe in {@code TransazioneDefi.MappaToken}, che e' indicizzata per simbolo di moneta e
     * <b>somma le quantita'</b>. Quattro gambe invece di due non producono due scambi identici — che la
     * deduplica per ID a valle potrebbe ancora riconoscere — ma <b>un solo scambio con una gamba
     * raddoppiata</b>, cioe' prezzo unitario dimezzato e costo di carico sbagliato, piu' un deposito o
     * prelievo fantasma con l'avanzo spaiato. Una volta consolidato il danno non e' piu' reversibile:
     * l'unico punto utile e' prima del raggruppamento.
     *
     * <p>Misurato sui dati reali di giugno 2026: 44 operazioni duplicate, che spiegavano esattamente il 100%
     * dello scostamento fra lo scaricamento con archivio e quello senza.
     *
     * <p><b>Chiave.</b> Conto di provenienza ({@code [2]}, {@code "Funding"} o {@code "Trading"}) piu'
     * {@code billId} ({@code [14]}). Il conto entra nella chiave perche' i due endpoint numerano i propri
     * bill in modo indipendente: senza, un'ipotetica collisione fra un bill del Funding e uno del Trading
     * farebbe sparire un movimento vero. Le due fonti che si sovrappongono sono entrambe {@code "Trading"}
     * ({@code convertOKXArchivio} chiama {@code convertOKXBills(..., "Trading")}), quindi la deduplica che
     * serve continua a funzionare.
     *
     * <p>Si tiene la <b>prima</b> occorrenza, cioe' quella dello scaricamento ordinario, che nell'ordine di
     * composizione della lista precede l'archivio. Le due copie sono comunque allineate cifra per cifra,
     * perche' {@code ripulisciBalChgArchivioOKX} riporta il {@code balChg} del CSV d'archivio al valore
     * dell'API.
     *
     * <p>Una riga senza {@code billId} viene <b>sempre tenuta</b>: senza identificativo non c'e' modo di
     * riconoscere un doppione, e scartarla significherebbe perdere un movimento per un dato che manca.
     *
     * @param righe righe in formato intermedio a 19 campi, in ordine di composizione
     * @return una nuova lista senza i bill ripetuti, nello stesso ordine
     */
    static List<String[]> deduplicaBillOKX(List<String[]> righe) {
        List<String[]> uniche = new ArrayList<>();
        Set<String> visti = new HashSet<>();
        int scartate = 0;

        for (String[] r : righe) {
            String billId = (r != null && r.length > 14 && r[14] != null) ? r[14].trim() : "";
            if (billId.isEmpty()) {
                uniche.add(r);
                continue;
            }
            String conto = (r.length > 2 && r[2] != null) ? r[2].trim() : "";
            if (visti.add(conto + "|" + billId)) {
                uniche.add(r);
            } else {
                scartate++;
            }
        }

        if (scartate > 0) {
            System.out.println("Import OKX: " + scartate + " righe scartate perche' lo stesso bill era gia' "
                    + "arrivato da un'altra finestra temporale (sovrapposizione archivio / bills-archive).");
        }
        return uniche;
    }

    public static List<String[]> convertOKXBills(JsonArray bills, String Wallet) {
        List<String[]> lista = new ArrayList<>();
        if (bills == null) return lista;

        boolean isTrading = Wallet.equalsIgnoreCase("Trading");

        for (JsonElement el : bills) {
            if (Principale.InterrompiCiclo) return null;
            JSONObject obj = new JSONObject(el.toString());

            String billId = obj.optString("billId", "");
            String ccy    = obj.optString("ccy", "");
            String balChg = obj.optString("balChg", "");
            String tipo   = obj.optString("type", "");
            String ts     = obj.optString("ts", "");
            String fee    = obj.optString("fee", "");
            //Etichetta in chiaro del tipo, presente sui bill del Funding: serve solo a rendere
            //autoesplicativi i codici non ancora mappati nel riepilogo dei movimenti sconosciuti.
            String notes  = obj.optString("notes", "");
            //Identificativo dell'ordine: presente su tutti i bill di trade, assente sui movimenti del
            //Funding, che non sono ordini. Serve a raggruppare per ordine invece che a naso.
            String ordId  = obj.optString("ordId", "");

            if (billId.isEmpty() || ccy.isEmpty() || balChg.isEmpty() || ts.isEmpty()) continue;
            if (!Funzioni.isNumeric(balChg, false) || !Funzioni.isNumeric(ts, false)) continue;

            //La commissione è sempre nella stessa moneta del bill (verificato su tutti i bill reali) ed è
            //già compresa in balChg: la si scorpora per riportare in carico il lordo. Il campo manca del
            //tutto sui bill del Funding, quindi la sua assenza è la norma e non un errore.
            BigDecimal feeBD = BigDecimal.ZERO;
            if (!fee.isEmpty() && Funzioni.isNumeric(fee, false)) feeBD = new BigDecimal(fee).abs().negate();

            //Quantità lorda: balChg è il netto, la fee è negativa, quindi sottrarla la riaggiunge.
            //Si ricava da balChg e non da sz perché il segno di sz non è affidabile su tutti i tipi di bill,
            //mentre balChg porta sempre il verso reale del movimento.
            BigDecimal lordo = new BigDecimal(balChg).subtract(feeBD);

            //Un bill che non muove nulla, nemmeno in commissioni, non è un movimento
            if (lordo.compareTo(BigDecimal.ZERO) == 0 && feeBD.compareTo(BigDecimal.ZERO) == 0) continue;

            //Causale nelle stesse etichette usate dal CSV, così la mappa causali resta una sola
            String causale = causaleBillOKX(tipo, balChg, isTrading, notes);

            String DatoRiga[] = new String[19];
            DatoRiga[0]  = FunzioniDate.ConvertiDatadaLongAlSecondo(Long.parseLong(ts));   //Timestamp
            DatoRiga[1]  = "OKX";                                                          //Exchange
            DatoRiga[2]  = Wallet;                                                         //Conto di provenienza, noto senza euristiche
            DatoRiga[3]  = "";                                                             //Categoria interna: la assegna Ex_OKX_RaggruppaEConsolida
            DatoRiga[4]  = causale;                                                        //Causale originale
            DatoRiga[5]  = ccy;                                                            //Moneta
            DatoRiga[6]  = lordo.stripTrailingZeros().toPlainString();                     //Qta LORDA, già con segno
            //Fee lasciate vuote quando sono nulle: Ex_OKX_Consolida genera il movimento di commissione sulla
            //sola base di [12] non vuoto, e uno "0" produrrebbe un movimento fantasma su ogni bill senza fee.
            if (feeBD.compareTo(BigDecimal.ZERO) != 0) {
                DatoRiga[11] = ccy;                                                        //Moneta fee
                DatoRiga[12] = feeBD.stripTrailingZeros().toPlainString();                 //Qta fee, negativa: è un'uscita
            } else {
                DatoRiga[11] = "";
                DatoRiga[12] = "";
            }
            DatoRiga[13] = ordId;                                                          //ID dell'ordine, vuoto sul Funding
            DatoRiga[14] = billId;                                                         //ID originale OKX
            //"NO": Funding e Trading vengono scaricati entrambi nella stessa esecuzione, quindi un
            //trasferimento interno compare già come bill su tutti e due i conti. Generare anche il
            //movimento opposto lo conterebbe due volte.
            DatoRiga[15] = "NO";
            DatoRiga[16] = "";                                                             //Wallet destinazione
            Importazioni.RiempiVuotiArray(DatoRiga);
            lista.add(DatoRiga);
        }
        return lista;
    }

    /**
     * Converte un unico oggetto JSON grezzo restituito da uno script CCXT (contenente eventualmente più
     * sezioni: depositi, prelievi, trade, conversioni, movimenti fiat, earn/reward) nella lista di righe nel
     * formato standard dei movimenti dell'applicazione, delegando a ciascun metodo {@code convert*} specifico.
     * @param json oggetto JSON grezzo restituito dallo script Node
     * @param Exchange nome dell'exchange di provenienza
     * @return la lista di righe di movimento convertite
     */
    public static List<String[]> getListaMovimento(JsonObject json,String Exchange) {
             List<String[]> lista = new ArrayList<>();       
             // Depositi
            JsonArray deposits = json.has("deposits") ? json.getAsJsonArray("deposits") : new JsonArray();
            lista.addAll(convertDepositi(deposits,Exchange));
          /*  for (JsonElement d : deposits) {
                System.out.println(d.toString());
            }*/

            // Prelievi
            JsonArray withdrawals = json.has("withdrawals") ? json.getAsJsonArray("withdrawals") : new JsonArray();
            lista.addAll(convertPrelievi(withdrawals,Exchange));
           /* for (JsonElement w : withdrawals) {
                System.out.println(w.toString());
            }*/

            // Trades
            JsonArray trades = json.has("trades") ? json.getAsJsonArray("trades") : new JsonArray();
            lista.addAll(convertTrades(trades,Exchange));
           /* for (JsonElement t : trades) {
                System.out.println(t.toString());
            }*/

            // Conversioni
            JsonArray conversions = json.has("Binance_smallAssetConversions") ? json.getAsJsonArray("Binance_smallAssetConversions") : new JsonArray();
            lista.addAll(convertBinanceConversioniSmall(conversions,Exchange));
            JsonArray conversions2 = json.has("Binance_Convert") ? json.getAsJsonArray("Binance_Convert") : new JsonArray();
            lista.addAll(convertBinanceConversioni(conversions2,Exchange));
           /* for (JsonElement c2 : conversions) {
                System.out.println(c2.toString());
            }*/

           
           // Depositi/prelievi e acquisti/vendite FIAT
            JsonObject Binance_fiat = json.has("Binance_fiat") ? json.getAsJsonObject("Binance_fiat") : new JsonObject();
            lista.addAll(convertBinanceMovimentiFiat(Binance_fiat,Exchange));

            
            
            // Savings / Earn
            JsonArray savings = json.has("savings") ? json.getAsJsonArray("savings") : new JsonArray();
           /* for (JsonElement s : savings) {
                System.out.println(s.toString());
            }*/

            // Staking
            JsonArray staking = json.has("staking") ? json.getAsJsonArray("staking") : new JsonArray();
          /*  for (JsonElement s2 : staking) {
                System.out.println(s2.toString());
            }*/
            
            // Binance Earn Flessibile
            JsonArray earnFlexible = json.has("Binance_earnFlexible") ? json.getAsJsonArray("Binance_earnFlexible") : new JsonArray();
            lista.addAll(convertBinanceEarn(earnFlexible,Exchange));
          /*  for (JsonElement s2 : earnFlexible) {
                System.out.println(s2.toString());
            }*/
            
            // Binance Earn Bloccato
            JsonArray earnLocked = json.has("Binance_EarnLocked") ? json.getAsJsonArray("Binance_EarnLocked") : new JsonArray();
            lista.addAll(convertBinanceEarn(earnLocked,Exchange));
            /*for (JsonElement s2 : earnLocked) {
                System.out.println(s2.toString());
            }*/
            
            // Binance Earn Bloccato
            JsonArray rewards = json.has("Binance_Rewards") ? json.getAsJsonArray("Binance_Rewards") : new JsonArray();
            lista.addAll(convertBinanceRewards(rewards,Exchange));
            /*for (JsonElement s2 : earnLocked) {
                System.out.println(s2.toString());
            }*/
            

            return lista;
    }
    
    /**
     * Come {@link #getListaMovimento}, ma applicato a più oggetti JSON grezzi (uno per ogni chiamata script
     * effettuata), concatenando i risultati.
     * @param Jsons oggetti JSON grezzi restituiti dagli script Node
     * @param Exchange nome dell'exchange di provenienza
     * @return la lista concatenata di tutte le righe di movimento convertite
     */
    public static List<String[]> getListaMovimenti(List<JsonObject> Jsons,String Exchange) {
        List<String[]> lista = new ArrayList<>();
        for(JsonObject json:Jsons){
            lista.addAll(getListaMovimento(json,Exchange));
        }
       return lista; 
    }
    
    /**
     * Esegue uno specifico script Node CCXT (identificato per nome, in {@code Scripts/<script>.js}) passandogli
     * credenziali, data di inizio e token come argomenti a riga di comando, e ne restituisce l'output JSON.
     * L'esecuzione può essere interrotta anticipatamente impostando {@link Principale#InterrompiCiclo}.
     * @param exchangeId identificativo CCXT dell'exchange
     * @param apiKey API key dell'account
     * @param secret API secret dell'account
     * @param startDate data di inizio da cui recuperare i movimenti, millisecondi epoch
     * @param Tokens elenco di token (separati da virgola) da passare allo script
     * @param script nome dello script Node da eseguire (senza estensione {@code .js})
     * @return l'oggetto JSON restituito dallo script, oppure {@code null} se node/script non sono trovati o l'esecuzione fallisce
     */
    public static JsonObject fetchMovimento(String exchangeId, String apiKey, String secret, long startDate,String Tokens,String script) {
        return fetchMovimento(exchangeId, apiKey, secret, startDate, Tokens, script, "");
    }

    /**
     * Come {@link #fetchMovimento(String, String, String, long, String, String)}, ma passa allo script anche la
     * terza credenziale richiesta da alcuni exchange (la passphrase di OKX, che CCXT chiama {@code password}).
     * <p>La passphrase viene aggiunta <b>in coda</b> agli argomenti a riga di comando, dopo {@code Tokens}: tutti
     * gli script esistenti destrutturano {@code process.argv} per posizione, quindi inserirla in mezzo li
     * romperebbe tutti.
     * @param exchangeId identificativo CCXT dell'exchange
     * @param apiKey API key dell'account
     * @param secret API secret dell'account
     * @param startDate data di inizio da cui recuperare i movimenti, millisecondi epoch
     * @param Tokens elenco di token (separati da virgola) da passare allo script
     * @param script nome dello script Node da eseguire (senza estensione {@code .js})
     * @param passphrase terza credenziale (passphrase OKX); stringa vuota per gli exchange che non la prevedono
     * @return l'oggetto JSON restituito dallo script, oppure {@code null} se node/script non sono trovati o l'esecuzione fallisce
     */
    public static JsonObject fetchMovimento(String exchangeId, String apiKey, String secret, long startDate,String Tokens,String script,String passphrase) {
        return fetchMovimento(exchangeId, apiKey, secret, startDate, Tokens, script, passphrase, "");
    }

    /**
     * Come {@link #fetchMovimento(String, String, String, long, String, String, String)}, ma passa allo script
     * anche il dominio da interrogare.
     * <p>Serve a OKX, che non ha un dominio unico: le entità regionali sono separate e una chiave API creata
     * su una di esse non esiste sulle altre (il dominio sbagliato risponde 50119 "API key doesn't exist" anche
     * con credenziali valide). Lo script individua il dominio giusto da solo alla prima esecuzione; qui gli si
     * ripassa quello già riconosciuto, per evitare di rifare ogni volta la stessa ricerca.
     * <p>Anche questo argomento è aggiunto <b>in coda</b>, dopo la passphrase, per la stessa ragione: gli altri
     * script destrutturano {@code process.argv} per posizione.
     * @param exchangeId identificativo CCXT dell'exchange
     * @param apiKey API key dell'account
     * @param secret API secret dell'account
     * @param startDate data di inizio da cui recuperare i movimenti, millisecondi epoch
     * @param Tokens elenco di token (separati da virgola) da passare allo script
     * @param script nome dello script Node da eseguire (senza estensione {@code .js})
     * @param passphrase terza credenziale (passphrase OKX); stringa vuota per gli exchange che non la prevedono
     * @param hostname dominio da interrogare; stringa vuota per lasciare allo script il compito di individuarlo
     * @return l'oggetto JSON restituito dallo script, oppure {@code null} se node/script non sono trovati o l'esecuzione fallisce
     */
    public static JsonObject fetchMovimento(String exchangeId, String apiKey, String secret, long startDate,String Tokens,String script,String passphrase,String hostname) {
    try {
        if (passphrase == null) passphrase = "";
        if (hostname == null) hostname = "";
        
        
        
        Path nodePath = getNodeExePath();
        Path scriptPath = Paths.get(VarStatiche.getPathRisorse()
                + "Scripts/"
                + script
                + ".js");

        if (!Files.exists(nodePath)) {
            System.err.println("Errore: node non trovato a " + nodePath.toAbsolutePath());
            return null;
        }
        if (!Files.exists(scriptPath)) {
            System.err.println("Errore: script JS non trovato a " + scriptPath.toAbsolutePath());
            return null;
        }

        System.out.println("Eseguo script : "+script+".js");
        ProcessBuilder builder = new ProcessBuilder(
                nodePath.toString(),
                scriptPath.toAbsolutePath().toString(),
                exchangeId.toLowerCase(), apiKey, secret, String.valueOf(startDate),Tokens,passphrase,hostname
        );
        builder.directory(scriptPath.getParent().toFile());
        // Non reindirizziamo stderr su stdout
        // builder.redirectErrorStream(true);
        // Calcola la cartella base di Node in modo multipiattaforma
      /*  Path nodeBaseDir = nodePath.getParent(); // es: .../node-vXX-PLATFORM[/bin]
        if (!nodeBaseDir.getFileName().toString().equals("bin")) {
            // Se siamo su Windows, node.exe sta direttamente in base dir, altrimenti sotto bin
            nodeBaseDir = nodeBaseDir.getParent();
        }
*/
        //Path nodeModulesPath = nodeBaseDir.resolve("node_modules").toAbsolutePath();
        Path nodeModulesPath = NODE_DIR.resolve("node_modules").toAbsolutePath();
        
        Map<String, String> env = builder.environment();

        // Aggiungi node_modules a NODE_PATH (se esiste già, concatena)
        String existingNodePath = env.get("NODE_PATH");
        //System.out.println("existingNodePath : "+existingNodePath);
        String newNodePath = nodeModulesPath.toString();
        //String newNodePath = NODE_DIR.toString();
        //System.out.println("newNodePath : "+newNodePath);
        if (existingNodePath != null && !existingNodePath.isEmpty()) {
            newNodePath += File.pathSeparator + existingNodePath;
        }
        env.put("NODE_PATH", newNodePath);
        System.out.println("newNodePath : "+newNodePath);

        Process process = builder.start();

        // Thread per log (stderr)
        new Thread(() -> {
            try (BufferedReader logReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String logLine;
                while ((logLine = logReader.readLine()) != null) {
                    if(Principale.InterrompiCiclo)
                    {
                        System.out.println("Premuto tasto INTERROMPI, blocco l'esecuzione dello script");
                        process.destroy();
                        return;
                    }
                    System.out.println("[Node-LOG] " + logLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Lettura JSON finale (stdout)
        StringBuilder output = new StringBuilder();
        try (BufferedReader jsonReader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String jsonLine;
            while ((jsonLine = jsonReader.readLine()) != null) {
                output.append(jsonLine);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Errore: processo Node.js terminato con codice " + exitCode);
            return null;
        }

        // Parse JSON con Gson
        Gson gson = new Gson();
        System.out.println(output.toString());
        JsonObject json = gson.fromJson(output.toString(), JsonObject.class);

        //Traccia della risposta nel documento di origine dello scaricamento in corso.
        //ATTENZIONE: apiKey, secret e passphrase NON devono comparire qui. Il documento contiene già
        //l'intera storia transazionale dell'utente; aggiungerci le credenziali lo renderebbe un singolo
        //punto di compromissione dell'account. Si registrano solo gli argomenti non segreti.
        DocumentiFonte.AggiungiAllaSessione(IdDocumentoSessione, script,
                exchangeId + " startDate=" + startDate
                + (Tokens == null || Tokens.isBlank() ? "" : " tokens=" + Tokens)
                + (hostname.isBlank() ? "" : " hostname=" + hostname),
                json == null ? null : json.toString());

        if (json.has("error") && !json.get("error").isJsonNull() && !json.get("error").getAsString().isEmpty()) {
            System.err.println("Errore dallo script JS: " + json.get("error").getAsString());
            return null;
        } else {
            return json;
                     
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return null;
}



    
/**
 * Converte un array JSON di depositi (formato CCXT) nelle righe di movimento standard dell'applicazione,
 * ordinandoli per data di inserimento/completamento e determinando il tipo (Crypto/FIAT) in base alla
 * presenza dell'indirizzo di contratto. Può essere interrotta impostando {@link Principale#InterrompiCiclo}.
 * @param jsonList array JSON dei depositi grezzi, oppure {@code null}
 * @param Exchange nome dell'exchange di provenienza
 * @return la lista di righe di movimento convertite (deposito), oppure {@code null} se l'elaborazione è stata interrotta, oppure lista vuota se {@code jsonList} è {@code null}
 */
public static List<String[]> convertDepositi(JsonArray jsonList,String Exchange) {
        List<String[]> lista = new ArrayList<>();
        
         // Ordiniamo per completeTime (servono per avere gruppi ordinati)
        List<JsonObject> objects = new ArrayList<>();
        if (jsonList==null)return lista;
        for (JsonElement el : jsonList) {
            objects.add(el.getAsJsonObject());
        }
        objects.sort((o1, o2) -> {
            long t1 = Long.parseLong(
                o1.has("insertTime") ? o1.get("insertTime").getAsString() : o1.get("completeTime").getAsString()
            );
            long t2 = Long.parseLong(
                o2.has("insertTime") ? o2.get("insertTime").getAsString() : o2.get("completeTime").getAsString()
            );
            return Long.compare(t1, t2);
        });
        
        
        
        
        int totMov = 1;
        int i = 1;
        String OldData="0";

        for (JsonElement el : objects) {
            if(Principale.InterrompiCiclo)return null;
            JSONObject obj = new JSONObject(el.toString());

            String coin = obj.optString("coin", "");
            String amount = obj.optString("amount", "");
            String network = obj.optString("network", "");
            String txId = obj.optString("txId", "");
            String address = obj.optString("address", "");
            String insertTime = obj.optString("insertTime", "completeTime");
            //String completeTime = obj.optString("completeTime", insertTime);

            long time = Long.parseLong(insertTime);
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);


            
            // Tipo moneta: se c'è l'address --> Crypto
            // se non c'è l'address --> FIAT solo se coin = EUR o USD
            String tipoMoneta;
            if (!address.isEmpty()) {
                tipoMoneta = "Crypto";
            } else {
                if (coin.equalsIgnoreCase("EUR") || coin.equalsIgnoreCase("USD")) {
                    tipoMoneta = "FIAT";
                } else {
                    tipoMoneta = "Crypto";
                }
            }

            Moneta Mon=new Moneta();
            Mon.Moneta=coin;
            Mon.Tipo=tipoMoneta;
            Mon.Qta=amount;

            String[] RT = MovimentiCrypto.creaMovimento(null, Mon, Exchange, "Principale",
                    time, null, null, totMov, i, null,
                    "Rete di provenienza : "+ network, "A", txId, null, null);
            if (RT != null) {
                RT[2] = i + " di " + totMov;                             // Numero movimenti
                RT[37] = address;
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }
        }

        return lista;
    }
    

/**
 * Converte i movimenti fiat di Binance (depositi/prelievi in {@code orders} e acquisti/vendite crypto con
 * fiat che non passano dai trade, es. con carta, in {@code payments}) nelle righe di movimento standard
 * dell'applicazione, generando anche la relativa riga di commissione. Considera solo i movimenti con stato
 * {@code "Successful"}. Può essere interrotta impostando {@link Principale#InterrompiCiclo}.
 * @param JObjetc oggetto JSON grezzo con le sezioni {@code orders} e {@code payments}
 * @param Exchange nome dell'exchange di provenienza
 * @return la lista di righe di movimento convertite, oppure {@code null} se l'elaborazione è stata interrotta
 */
public static List<String[]> convertBinanceMovimentiFiat(JsonObject JObjetc,String Exchange) {
        List<String[]> lista = new ArrayList<>();
        
        //Dentro orders ci sono i depositi e prelievi FIAT
        JsonArray orders= JObjetc.has("orders") ? JObjetc.getAsJsonArray("orders") : new JsonArray();
        //Dentro payments ci sono invece  gli acquisti e vendite crypto tramite fiat che non passano per i trades
        //Ad esempio acquisti con carte
        JsonArray payments= JObjetc.has("payments") ? JObjetc.getAsJsonArray("payments") : new JsonArray();
        
         // Ordiniamo per completeTime (servono per avere gruppi ordinati)
        List<JsonObject> ordersOBJ = new ArrayList<>();
        List<JsonObject> paymentsOBJ = new ArrayList<>();
        if (orders==null&&payments==null)return lista;
        
        
        for (JsonElement el : orders) {
            ordersOBJ.add(el.getAsJsonObject());
        }
        
        for (JsonElement el : payments) {
            paymentsOBJ.add(el.getAsJsonObject());
        }
        
        ordersOBJ.sort((o1, o2) -> {
            long t1 = Long.parseLong(
                o1.has("updateTime") ? o1.get("updateTime").getAsString() : o1.get("createTime").getAsString()
            );
            long t2 = Long.parseLong(
                o2.has("updateTime") ? o2.get("updateTime").getAsString() : o2.get("createTime").getAsString()
            );
            return Long.compare(t1, t2);
        });
        
        paymentsOBJ.sort((o1, o2) -> {
            long t1 = Long.parseLong(
                o1.has("updateTime") ? o1.get("updateTime").getAsString() : o1.get("createTime").getAsString()
            );
            long t2 = Long.parseLong(
                o2.has("updateTime") ? o2.get("updateTime").getAsString() : o2.get("createTime").getAsString()
            );
            return Long.compare(t1, t2);
        });
        
        //Cominciamo dai depositi prelievi FIAT
        int totMov = 1;
        //int i = 1;
        String OldData="0";

        for (JsonElement el : ordersOBJ) {
            if(Principale.InterrompiCiclo)return null;
            JSONObject obj = new JSONObject(el.toString());

            String coin = obj.optString("fiatCurrency", "");
            String amount = obj.optString("indicatedAmount", "");
            String amountp = obj.optString("amount", "");
            String feeamount = obj.optString("totalFee", "");
            String metodo = obj.optString("method", "");//Metodo di pagamento
            String direzione = obj.optString("movimento", "");//può essere deposito o prelievo
            String insertTime = obj.optString("updateTime", "createTime");
            String status=obj.optString("status", "");//deve essere Successful perchè sia valido


            long time = Long.parseLong(insertTime);
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);

            if (status.trim().equalsIgnoreCase("Successful")) {
                Moneta Mon = new Moneta();
                Mon.Moneta = coin;
                Mon.Tipo = "FIAT";
                String[] RT;
                if (direzione.equalsIgnoreCase("deposito")) {
                    Mon.Qta = amount;
                    RT = MovimentiCrypto.creaMovimento(null, Mon, Exchange, "Principale",
                            time, null, null, totMov, 1, null,
                            null, "A", null, null, null);
                } else //Il fatto che sia prelievo è sottointeso visto che ci sono solo 2 opzioni
                {
                    Mon.Qta = ValoreNegativo(amountp);
                    RT = MovimentiCrypto.creaMovimento(Mon, null, Exchange, "Principale",
                            time, null, null, totMov, 1, null,
                            null, "A", null, null, null);
                }
                if (RT != null) {
                    RT[2] = "1 di 2";                                        // Numero movimenti
                    RT[7] = "Metodo di pagamento : " + metodo;                // Causale originale
                    RT[39] = "A"; //Fonte dati A = API Exchange
                    Importazioni.RiempiVuotiArray(RT);
                    lista.add(RT);
                }

                //Adesso è il turno delle commissioni
                Moneta Fee = new Moneta();
                Fee.Moneta = coin;
                Fee.Tipo = "FIAT";
                Fee.Qta = ValoreNegativo(feeamount);
                RT = MovimentiCrypto.creaMovimento(Fee, null, Exchange, "Principale",
                        time, null, null, totMov, 2, null,
                        null, "A", null, "COMMISSIONE", null);
                if (RT != null) {
                    RT[2] = "2 di 2";                                        // Numero movimenti
                    RT[39] = "A"; //Fonte dati A = API Exchange
                    Importazioni.RiempiVuotiArray(RT);
                    lista.add(RT);
                }
            }
        }

        
        
        //Adesso è il turno dei pagamenti
        totMov = 1;
        //int i = 1;
        OldData="0";

        for (JsonElement el : paymentsOBJ) {
            if(Principale.InterrompiCiclo)return null;
            JSONObject obj = new JSONObject(el.toString());
            
            boolean inserisciFee=false;
            boolean inserisciArrivoFIAT=false;
            
            Moneta FIAT = new Moneta();
            Moneta CRYPTO = new Moneta();
            Moneta FEE = new Moneta();

            String feeamount = obj.optString("totalFee", "");
            String direzione = obj.optString("movimento", "");//può essere acquisto o vendita
            String souceAmount = obj.optString("sourceAmount", "");
            String metodo = obj.optString("paymentMethod", "");//Metodo di pagamento
            //se il metodo è diverso da Cash Balance devo anche inserire un movimento di DEPOSITO FIAT
            if (!metodo.trim().equalsIgnoreCase("Cash Balance"))inserisciArrivoFIAT=true;
            
            FIAT.Moneta = obj.optString("fiatCurrency", "");
            FIAT.Qta = souceAmount;
            FIAT.Tipo="FIAT";
            CRYPTO.Moneta = obj.optString("cryptoCurrency", "");
            CRYPTO.Qta = obj.optString("obtainAmount", "");
            CRYPTO.Tipo="Crypto";
            
            //Adesso a seconda del movimento calcolo anche la fee
            if (direzione.equalsIgnoreCase("acquisto")){//Acquisto
                //FIAT.Qta=new BigDecimal(FIAT.Qta).subtract(new BigDecimal(feeamount)).toPlainString();
                FEE.Moneta=FIAT.Moneta;
                FEE.Qta=feeamount;
                FEE.Tipo=FIAT.Tipo;
                if (new BigDecimal(FEE.Qta).compareTo(BigDecimal.ZERO)!=0){
                    inserisciFee=true;
                }
                
            }else{//Vendita
                //Non ho i dati nei miei file quindi per le vendite per ora non calcolo le fee ma prendo il prezzo lordo
                inserisciFee=false;
            }

            String insertTime = obj.optString("updateTime", "createTime");
            String status=obj.optString("status", "");//deve essere Completed perchè sia valido


            long time = Long.parseLong(insertTime);
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            int numMovimenti=1;
            int movScambio=1;
            int movCommissione=2;
            if(inserisciArrivoFIAT)
            {
                movScambio++;
                numMovimenti++;
                movCommissione++;
            }
            if(inserisciFee)numMovimenti++;
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);

            if (status.trim().equalsIgnoreCase("Completed")) {
                String[] RT;
                if (inserisciArrivoFIAT) {
                    //Inserisco l'arrivo nel wallet delle FIAT
                    RT = MovimentiCrypto.creaMovimento(null, FIAT, Exchange, "Principale",
                            time, null, null, totMov, 1, null,
                            null, "A", null, null, null);
                    if (RT != null) {
                        RT[2] = "1 di "+numMovimenti;                          // Numero movimenti
                        RT[7] = "Metodo di pagamento : "+metodo;               // Causale originale
                        RT[39] = "A"; //Fonte dati A = API Exchange
                        Importazioni.RiempiVuotiArray(RT);
                        lista.add(RT);
                    }
                }

                if (direzione.equalsIgnoreCase("acquisto")){
                    FIAT.Qta=ValoreNegativo(new BigDecimal(FIAT.Qta).subtract(new BigDecimal(feeamount)).toPlainString());
                    RT = MovimentiCrypto.creaMovimento(FIAT, CRYPTO, Exchange, "Principale",
                            time, null, null, totMov, movScambio, null,
                            null, "A", null, null, null);
                } else //Il fatto che sia prelievo è sottointeso visto che ci sono solo 2 opzioni
                {
                    CRYPTO.Qta=ValoreNegativo(CRYPTO.Qta);
                    RT = MovimentiCrypto.creaMovimento(CRYPTO, FIAT, Exchange, "Principale",
                            time, null, null, totMov, movScambio, null,
                            null, "A", null, null, null);
                }
                if (RT != null) {
                    RT[2] = movScambio+" di "+numMovimenti;                    // Numero movimenti
                    RT[7] = "Metodo di pagamento : " + metodo;                 // Causale originale
                    RT[39] = "A"; //Fonte dati A = API Exchange
                    Importazioni.RiempiVuotiArray(RT);
                    lista.add(RT);
                }

                //Adesso è il turno delle commissioni
                if (inserisciFee) {
                    FEE.Qta=ValoreNegativo(FEE.Qta);
                    RT = MovimentiCrypto.creaMovimento(FEE, null, Exchange, "Principale",
                            time, null, null, totMov, movCommissione, null,
                            null, "A", null, "COMMISSIONE", null);
                    if (RT != null) {
                        RT[2] = movCommissione+" di "+numMovimenti;            // Numero movimenti
                        RT[39] = "A"; //Fonte dati A = API Exchange
                        Importazioni.RiempiVuotiArray(RT);
                        lista.add(RT);
                    }
                }
            }
        }
        
        
        
        
        return lista;
    }    

    /** @param qta quantità come stringa decimale (segno indifferente) @return il valore assoluto di {@code qta} reso negativo */
    public static String ValoreNegativo(String qta){
        return new BigDecimal(qta).abs().multiply(new BigDecimal(-1)).toPlainString();
    }

   /**
    * Converte un array JSON di prelievi (formato CCXT) nelle righe di movimento standard dell'applicazione,
    * analogamente a {@link #convertDepositi} ma per i prelievi (quantità resa negativa).
    * @param jsonList array JSON dei prelievi grezzi, oppure {@code null}
    * @param Exchange nome dell'exchange di provenienza
    * @return la lista di righe di movimento convertite (prelievo), oppure {@code null} se l'elaborazione è stata interrotta, oppure lista vuota se {@code jsonList} è {@code null}
    */
   public static List<String[]> convertPrelievi(JsonArray jsonList,String Exchange) {
        List<String[]> lista = new ArrayList<>();
        
         // Ordiniamo per completeTime (servono per avere gruppi ordinati)
        List<JsonObject> objects = new ArrayList<>();
        if (jsonList==null)return lista;
        for (JsonElement el : jsonList) {
            objects.add(el.getAsJsonObject());
        }
        objects.sort((o1, o2) -> {
            long t1 = Long.parseLong(
                o1.has("insertTime") ? o1.get("insertTime").getAsString() : o1.get("completeTime").getAsString()
            );
            long t2 = Long.parseLong(
                o2.has("insertTime") ? o2.get("insertTime").getAsString() : o2.get("completeTime").getAsString()
            );
            return Long.compare(t1, t2);
        });
        
        
        
        
        int totMov = 1;
        int i = 1;
        String OldData="0";

        for (JsonElement el : objects) {
            if(Principale.InterrompiCiclo)return null;
            JSONObject obj = new JSONObject(el.toString());

            String coin = obj.optString("coin", "");
            String amount = obj.optString("amount", "");
            String network = obj.optString("network", "");
            String txId = obj.optString("txId", "");
            String address = obj.optString("address", "");
            //String transferType = obj.optString("transferType", "0"); // 0 deposito, 1 prelievo
            String fee = obj.optString("transactionFee", "0"); // 0 deposito, 1 prelievo
            String insertTime = obj.optString("insertTime", "completeTime");
            //String completeTime = obj.optString("completeTime", insertTime);

            long time = Long.parseLong(insertTime);
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);


            
            // Tipo moneta: se c'è l'address --> Crypto
            // se non c'è l'address --> FIAT solo se coin = EUR o USD
            String tipoMoneta;
            if (!address.isEmpty()) {
                tipoMoneta = "Crypto";
            } else {
                if (coin.equalsIgnoreCase("EUR") || coin.equalsIgnoreCase("USD")) {
                    tipoMoneta = "FIAT";
                } else {
                    tipoMoneta = "Crypto";
                }
            }

            Moneta Mon=new Moneta();
            Mon.Moneta=coin;
            Mon.Tipo=tipoMoneta;
            Mon.Qta=ValoreNegativo(amount);

            String[] RT = MovimentiCrypto.creaMovimento(Mon, null, Exchange, "Principale",
                    time, null, null, totMov, i, null,
                    "Rete di trasferimento : "+ network, "A", txId, null, null);
            if (RT != null) {
                RT[2] = "1 di 2";                                        // Numero movimenti
                RT[37] = address;
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }

            //SECONDA PARTE RELATIVA ALLE FEE
            Moneta Fee=new Moneta();
            Fee.Moneta=coin;
            Fee.Tipo=tipoMoneta;
            Fee.Qta=ValoreNegativo(fee);
            RT = MovimentiCrypto.creaMovimento(Fee, null, Exchange, "Principale",
                    time, null, null, totMov, 2, null,
                    "Rete di trasferimento : "+ network, "A", txId, "COMMISSIONE", null);
            if (RT != null) {
                RT[2] = "2 di 2";                                        // Numero movimenti
                RT[37] = address;
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }
        }

        return lista;
    }
    
   /**
    * Converte un array JSON di trade spot (formato CCXT) nelle righe di movimento standard dell'applicazione
    * (scambio tra le due monete della coppia, con relativa commissione), ordinandoli per timestamp e
    * determinando moneta uscente/entrante in base al verso (buy/sell) del trade.
    * @param jsonList array JSON dei trade grezzi, oppure {@code null}
    * @param Exchange nome dell'exchange di provenienza
    * @return la lista di righe di movimento convertite (scambio + commissione), oppure {@code null} se l'elaborazione è stata interrotta, oppure lista vuota se {@code jsonList} è {@code null}
    */
   public static List<String[]> convertTrades(JsonArray jsonList,String Exchange) {
        List<String[]> lista = new ArrayList<>();
        
         // Ordiniamo per completeTime (servono per avere gruppi ordinati)
        List<JsonObject> objects = new ArrayList<>();
        if (jsonList==null)return lista;
        for (JsonElement el : jsonList) {
            objects.add(el.getAsJsonObject());
        }
        objects.sort((o1, o2) -> {
            long t1 = Long.parseLong(o1.get("timestamp").getAsString());
            long t2 = Long.parseLong(o2.get("timestamp").getAsString());
            return Long.compare(t1, t2);
        });
        
        
        
        
        int totMov = 1;
        int i = 1;
        String OldData="0";

        for (JsonElement el : objects) {
            if(Principale.InterrompiCiclo)return null;
            JSONObject obj = new JSONObject(el.toString());
            Moneta mu=new Moneta();
            Moneta me=new Moneta();
            
            String verso = obj.optString("side", "");
            String Simboli[] = obj.optString("symbol", "").split("/");
            if (verso.equalsIgnoreCase("sell"))
            {
                mu.Moneta=Simboli[0];
                mu.Qta=obj.getJSONObject("info").optString("qty", "");
                mu.Tipo = (mu.Moneta.equalsIgnoreCase("EUR") || mu.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
                me.Moneta=Simboli[1];
                me.Qta=obj.getJSONObject("info").optString("quoteQty", "");
                me.Tipo = (me.Moneta.equalsIgnoreCase("EUR") || me.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
            }
            else
            {
                mu.Moneta=Simboli[1];
                mu.Qta=obj.getJSONObject("info").optString("quoteQty", "");
                mu.Tipo = (mu.Moneta.equalsIgnoreCase("EUR") || mu.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
                me.Moneta=Simboli[0];
                me.Qta=obj.getJSONObject("info").optString("qty", "");
                me.Tipo = (me.Moneta.equalsIgnoreCase("EUR") || me.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
            }
            
            mu.Qta=new BigDecimal(mu.Qta).abs().stripTrailingZeros().multiply(new BigDecimal(-1)).toPlainString();
            me.Qta=new BigDecimal(me.Qta).abs().stripTrailingZeros().toPlainString();
            
            Moneta mc=new Moneta();
            mc.Moneta = obj.getJSONObject("fee").optString("currency", "");
            mc.Qta = new BigDecimal(obj.getJSONObject("fee").optString("cost", "")).stripTrailingZeros().toPlainString();
            mc.Tipo = (mc.Moneta.equalsIgnoreCase("EUR") || mc.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
            String Time = obj.optString("timestamp", "");
            //String completeTime = obj.optString("completeTime", insertTime);

            long time = Long.parseLong(Time);
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);


            String[] RT = MovimentiCrypto.creaMovimento(mu, me, Exchange, "Principale",
                    time, null, null, totMov, i, null,
                    null, "A", null, null, null);
            if (RT != null) {
                RT[2] = "1 di 2";                                        // Numero movimenti
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }

            //SECONDA PARTE RELATIVA ALLE FEE
            mc.Qta=new BigDecimal(mc.Qta).abs().multiply(new BigDecimal(-1)).toPlainString();
            RT = MovimentiCrypto.creaMovimento(mc, null, Exchange, "Principale",
                    time, null, null, totMov, 2, null,
                    null, "A", null, "COMMISSIONE", null);
            if (RT != null) {
                RT[2] = "2 di 2";                                        // Numero movimenti
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }
        }

        return lista;
    }     
   
      /**
       * Converte un array JSON di conversioni "small asset" (dust) Binance nelle righe di movimento standard
       * dell'applicazione (scambio tra le due monete + relativa commissione di servizio).
       * @param jsonList array JSON delle conversioni grezze, oppure {@code null}
       * @param Exchange nome dell'exchange di provenienza
       * @return la lista di righe di movimento convertite, oppure {@code null} se l'elaborazione è stata interrotta, oppure lista vuota se {@code jsonList} è {@code null}
       */
      public static List<String[]> convertBinanceConversioniSmall(JsonArray jsonList,String Exchange) {
        List<String[]> lista = new ArrayList<>();
         // Ordiniamo per completeTime (servono per avere gruppi ordinati)
        List<JsonObject> objects = new ArrayList<>();
        if (jsonList==null)return lista;
        for (JsonElement el : jsonList) {
           // System.out.println("Conversioni!!!"+jsonList);
            objects.add(el.getAsJsonObject());
        }
        objects.sort((o1, o2) -> {
            long t1 = Long.parseLong(o1.get("operateTime").getAsString());
            long t2 = Long.parseLong(o2.get("operateTime").getAsString());
            return Long.compare(t1, t2);
        });
        
        
        
        
        int totMov = 1;
        int i = 1;
        String OldData="0";

        for (JsonElement el : objects) {
            if(Principale.InterrompiCiclo)return null;
            //System.out.println("Conversioni!!! : "+el);
            JSONObject obj = new JSONObject(el.toString());
            Moneta mu=new Moneta();
            Moneta me=new Moneta();
            
            
          //  String Simboli[] = obj.optString("symbol", "").split("/");

                mu.Moneta=obj.optString("fromAsset", "");
                mu.Qta=obj.optString("amount", "");
                mu.Tipo = (mu.Moneta.equalsIgnoreCase("EUR") || mu.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
                me.Moneta=obj.optString("toAsset", "");
                me.Qta=obj.optString("transferedAmount", "");
                me.Tipo = (me.Moneta.equalsIgnoreCase("EUR") || me.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
 

            mu.Qta=new BigDecimal(mu.Qta).abs().stripTrailingZeros().multiply(new BigDecimal(-1)).toPlainString();
            me.Qta=new BigDecimal(me.Qta).abs().stripTrailingZeros().toPlainString();
            
            Moneta mc=new Moneta();
            mc.Moneta = obj.optString("toAsset", "");
            mc.Qta = new BigDecimal(obj.optString("serviceChargeAmount", "")).stripTrailingZeros().toPlainString();
            mc.Tipo = (mc.Moneta.equalsIgnoreCase("EUR") || mc.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
            
            String Time = obj.optString("operateTime", "");
            //String completeTime = obj.optString("completeTime", insertTime);

            long time = Long.parseLong(Time);
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);


            String[] RT = MovimentiCrypto.creaMovimento(mu, me, Exchange, "Principale",
                    time, null, null, totMov, i, null,
                    null, "A", null, null, null);
            if (RT != null) {
                RT[2] = i + " di " + totMov;                             // Numero movimenti
                RT[7] = "asset/dribblet (API)";                          // Causale originale
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }

            //SECONDA PARTE RELATIVA ALLE FEE
            mc.Qta=new BigDecimal(mc.Qta).abs().multiply(new BigDecimal(-1)).toPlainString();
            RT = MovimentiCrypto.creaMovimento(mc, null, Exchange, "Principale",
                    time, null, null, totMov, 2, null,
                    null, "A", null, "COMMISSIONE", null);
            if (RT != null) {
                RT[2] = i + " di " + totMov;                             // Numero movimenti
                RT[7] = "asset/dribblet (API)";                          // Causale originale
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }
        }

        return lista;
    }
   
        /**
         * Converte un array JSON di conversioni della feature Binance Convert (senza commissione separata,
         * a differenza di {@link #convertBinanceConversioniSmall}) nelle righe di movimento standard dell'applicazione.
         * @param jsonList array JSON delle conversioni grezze, oppure {@code null}
         * @param Exchange nome dell'exchange di provenienza
         * @return la lista di righe di movimento convertite, oppure {@code null} se l'elaborazione è stata interrotta, oppure lista vuota se {@code jsonList} è {@code null}
         */
        public static List<String[]> convertBinanceConversioni(JsonArray jsonList,String Exchange) {
        List<String[]> lista = new ArrayList<>();
         // Ordiniamo per completeTime (servono per avere gruppi ordinati)
        List<JsonObject> objects = new ArrayList<>();
        if (jsonList==null)return lista;
        for (JsonElement el : jsonList) {
           // System.out.println("Conversioni!!!"+jsonList);
            objects.add(el.getAsJsonObject());
        }
        objects.sort((o1, o2) -> {
            long t1 = Long.parseLong(o1.get("createTime").getAsString());
            long t2 = Long.parseLong(o2.get("createTime").getAsString());
            return Long.compare(t1, t2);
        });
        
        
        
        
        int totMov = 1;
        int i = 1;
        String OldData="0";

        for (JsonElement el : objects) {
            if(Principale.InterrompiCiclo)return null;
            //System.out.println("Conversioni!!! : "+el);
            JSONObject obj = new JSONObject(el.toString());
            Moneta mu=new Moneta();
            Moneta me=new Moneta();
            
            
          //  String Simboli[] = obj.optString("symbol", "").split("/");

                mu.Moneta=obj.optString("fromAsset", "");
                mu.Qta=obj.optString("fromAmount", "");
                mu.Tipo = (mu.Moneta.equalsIgnoreCase("EUR") || mu.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
                me.Moneta=obj.optString("toAsset", "");
                me.Qta=obj.optString("toAmount", "");
                me.Tipo = (me.Moneta.equalsIgnoreCase("EUR") || me.Moneta.equalsIgnoreCase("USD")) ? "FIAT" : "Crypto";
 

            mu.Qta=new BigDecimal(mu.Qta).abs().stripTrailingZeros().multiply(new BigDecimal(-1)).toPlainString();
            me.Qta=new BigDecimal(me.Qta).abs().stripTrailingZeros().toPlainString();
                      
            String Time = obj.optString("operateTime", "");
            if (Time.isBlank())Time=obj.optString("createTime", "");
            //String completeTime = obj.optString("completeTime", insertTime);

            long time = Long.parseLong(Time);
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);


            String[] RT = MovimentiCrypto.creaMovimento(mu, me, Exchange, "Principale",
                    time, null, null, totMov, i, null,
                    null, "A", null, null, null);
            if (RT != null) {
                RT[2] = i + " di " + totMov;                             // Numero movimenti
                RT[7] = "convert/tradeFlow (API)";                       // Causale originale
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }
        }

        return lista;
    }
    
   /**
    * Converte un array JSON di reward Simple Earn (flessibile o bloccato) Binance nelle righe di movimento
    * standard dell'applicazione, classificate come reward in entrata.
    * @param jsonList array JSON delle reward Earn grezze, oppure {@code null}
    * @param Exchange nome dell'exchange di provenienza
    * @return la lista di righe di movimento convertite, oppure {@code null} se l'elaborazione è stata interrotta, oppure lista vuota se {@code jsonList} è {@code null}
    */
   public static List<String[]> convertBinanceEarn(JsonArray jsonList,String Exchange) {
        List<String[]> lista = new ArrayList<>();
        
         // Ordiniamo per completeTime (servono per avere gruppi ordinati)
        List<JsonObject> objects = new ArrayList<>();
        if (jsonList==null)return lista;
        for (JsonElement el : jsonList) {
            objects.add(el.getAsJsonObject());
        }
        objects.sort((o1, o2) -> {
            long t1 = Long.parseLong(
                o1.get("time").getAsString()
            );
            long t2 = Long.parseLong(
                o2.get("time").getAsString()
            );
            return Long.compare(t1, t2);
        });
        
        
        
        
        int totMov = 1;
        int i = 1;
        String OldData="0";

        for (JsonElement el : objects) {
            if(Principale.InterrompiCiclo)return null;
            JSONObject obj = new JSONObject(el.toString());

            String coin = obj.optString("asset", "");
            String amount = obj.optString("amount", "");
            amount = obj.optString("rewards", amount);
            String tipo = obj.optString("type", "");
            String insertTime = obj.optString("time", "");


            long time = Long.parseLong(insertTime);
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);


            
            // Tipo moneta: se c'è l'address --> Crypto
            // se non c'è l'address --> FIAT solo se coin = EUR o USD
            String tipoMoneta;
                if (coin.equalsIgnoreCase("EUR") || coin.equalsIgnoreCase("USD")) {
                    tipoMoneta = "FIAT";
                } else {
                    tipoMoneta = "Crypto";
                }
            

            Moneta Mon=new Moneta();
            Mon.Moneta=coin;
            Mon.Tipo=tipoMoneta;
            Mon.Qta=amount;

            String[] RT = MovimentiCrypto.creaMovimento(null, Mon, Exchange, "Principale",
                    time, null, null, totMov, i, null,
                    null, "A", null, tipoMoneta.equals("FIAT") ? null : "EARN", null);
            if (RT != null) {
                RT[2] = i + " di " + totMov;                             // Numero movimenti
                RT[7] = tipo;                                            // Causale originale
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }
        }

        return lista;
    }
   
   
      /**
       * Converte un array JSON di asset dividend/reward Binance (ordinati per data di distribuzione) nelle
       * righe di movimento standard dell'applicazione, classificate come reward in entrata.
       * @param jsonList array JSON delle reward grezze, oppure {@code null}
       * @param Exchange nome dell'exchange di provenienza
       * @return la lista di righe di movimento convertite, oppure {@code null} se l'elaborazione è stata interrotta, oppure lista vuota se {@code jsonList} è {@code null}
       */
      public static List<String[]> convertBinanceRewards(JsonArray jsonList,String Exchange) {
        List<String[]> lista = new ArrayList<>();
        
         // Ordiniamo per completeTime (servono per avere gruppi ordinati)
        List<JsonObject> objects = new ArrayList<>();
        if (jsonList==null)return lista;
        for (JsonElement el : jsonList) {
            objects.add(el.getAsJsonObject());
        }
        objects.sort((o1, o2) -> {
            long t1 = Long.parseLong(
                o1.get("divTime").getAsString()
            );
            long t2 = Long.parseLong(
                o2.get("divTime").getAsString()
            );
            return Long.compare(t1, t2);
        });
        
        
        
        
        int totMov = 1;
        int i = 1;
        String OldData="0";

        for (JsonElement el : objects) {
            if(Principale.InterrompiCiclo)return null;
            JSONObject obj = new JSONObject(el.toString());

            String coin = obj.optString("asset", "");
            String amount = obj.optString("amount", "");
            String tipo = obj.optString("enInfo", "");
            String insertTime = obj.optString("divTime", "");
            
            //Queste tipologie non le voglio conteggiare perchè già conteggiate in altro ciclo
            if (!tipo.equalsIgnoreCase("Flexible")&&!tipo.equalsIgnoreCase("Locked")){
//&&!tipo.equalsIgnoreCase("BNB Vault")
            //System.out.println("Inserito " +amount+" "+coin+" - tipologia:"+tipo);

            long time = Long.parseLong(insertTime);
            String data = FunzioniDate.ConvertiDatadaLongAlSecondo(time);
            //Questo serve per incrementae il numero sull'id in caso di movimenti contemporanei
            //Altrimenti andrei a sovrascrivere il movimento precedente
            if (OldData.equals(data))totMov++;
            else {
                totMov=1;
                OldData=data;
            }
            
            String dataForId = data.replaceAll(" |-|:", "");
            String dataa = data.trim().substring(0, data.length()-3);


            
            // Tipo moneta: se c'è l'address --> Crypto
            // se non c'è l'address --> FIAT solo se coin = EUR o USD
            String tipoMoneta;
                if (coin.equalsIgnoreCase("EUR") || coin.equalsIgnoreCase("USD")) {
                    tipoMoneta = "FIAT";
                } else {
                    tipoMoneta = "Crypto";
                }
            

            Moneta Mon=new Moneta();
            Mon.Moneta=coin;
            Mon.Tipo=tipoMoneta;
            Mon.Qta=amount;

            String TipoTr = null;
            if (!tipoMoneta.equals("FIAT")) {
                TipoTr = tipo.toLowerCase().contains("staking") ? "STAKING REWARD" : "EARN";
            }

            String[] RT = MovimentiCrypto.creaMovimento(null, Mon, Exchange, "Principale",
                    time, null, null, totMov, i, null,
                    null, "A", null, TipoTr, null);
            if (RT != null) {
                RT[2] = i + " di " + totMov;                             // Numero movimenti
                RT[7] = tipo;                                            // Causale originale
                RT[39] = "A"; //Fonte dati A = API Exchange
                Importazioni.RiempiVuotiArray(RT);
                lista.add(RT);
            }
        }
}
        return lista;
    }
   
   
   
   

    /**
     * Metodo attualmente privo di implementazione (corpo vuoto); non è l'entry point dell'applicazione (vedi {@link Giacenze_Crypto#main}).
     * @param args argomenti a riga di comando (non utilizzati)
     */
    public static void main(String[] args) {

    }
}

