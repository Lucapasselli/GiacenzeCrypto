package com.giacenzecrypto.giacenze_crypto;

import java.awt.Component;
import java.awt.Cursor;
import java.io.File;
import java.util.ArrayList;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author luca.passelli
 */
public class Importazioni_Gestione extends javax.swing.JDialog {
    private static final long serialVersionUID = 7L;

    /**
     * Creates new form Gestione_Importazioni
     */
    static String Exchanges[]=new String[]{"----------","*Nome Personalizzato*",
    "Abra","Acx","AscendEX","BSDEX","BTC Markets","BTCPay Bybit","Bybit",
    "BYDFI","Binance","Binance US","Bison","Bitcoin Suisse","Bitcoin.de",
    "Bitfinex","Bithumb Glo.","Bitpanda","Bitpanda Pro","Bitrue","Bitstamp",
    "Bittrex","BlockFi","CEX","Cake Defl","Celsius","Changelly",
    "Circle","CoinEx","Coinbase","Coinbase Pro","Coinmate","Coinmerce",
    "Coinmetro","Coss","Crex24","Criptan","Crypto.com","Crypto.com Exchange",
    "DFX.swiss","Deribit","Digital Surge","Gate.lo","Gemini","HRBTC",
    "Haru","Hodinaut","Hotbit","Iconomi","Idex","Kraken",
    "KuCoin","Localbitcoins","Luxor","MEXC","Mercatox","NFTBank",
    "Nexo","Northcrypto","OKColn","OKX","Phemex","Pocket Bitcoin",
    "Poloniex","Relal","Revolut","STEX","SwissBorg","Swyftx","Tradeogre",
    "Uphold","Voyager","Yield App","Zerion"};
    
    static String Wallets[]=new String[]{"----------","*Nome Personalizzato*",
    "BitBox","Citcoin Core Client","Blochchain.com","Electrum","Exodus","Gate Hub","Ledger Live","Mycellum","Trezor"};
    
    static String BlockChain[]=new String[]{"----------",
    "Arbitrum (ARB)","Avalanche (AVAX)","Base (BASE)","Berachain (BERA)","Bitcoin (BTC)","Cardano (ADA)","Binance Chain (BNB)","Binance Smart Chain (BSC)",
    "Cronos Chain (CRO)","Dash (DASH)","Dogecoin (DOGE)","Polkadot (DOT)","Eos (EOS)","Ethereum (ETH)",
    "Fantom (FTM)","Gnosis Chain (GNOSIS)","Litecoin (LTC)","Terra Classic (LUNA)","Polygon (POL)","Tron (TRX)","Solana (SOL)","Monad (MONAD)",
    "Stellar (XLM)","Ripple (XRP)","Zcash (ZEC)",
    "Linea (LINEA)","Blast (BLAST)","Unichain (UNICHAIN)","World Chain (WORLD)","Taiko (TAIKO)","Abstract (ABSTRACT)","Katana (KATANA)","Sonic (SONIC)","Mantle (MANTLE)","Optimism (OP)","HyperEVM (HYPEREVM)","Ink (INK)","Robinhood Chain (ROBINHOOD)"};
    
    static String NomeWallet="";

    /**
     * Ordina alfabeticamente, ignorando maiuscole e minuscole, le voci di una lista di scelta, lasciando
     * in testa — nel loro ordine originale — le voci speciali: il separatore {@code ----------}, il
     * segnaposto {@code - nessuno -} e {@code *Nome Personalizzato*}, che non sono nomi di exchange e
     * devono restare le prime.
     * <p>L'ordinamento avviene qui e non negli array sorgente, così aggiungere un exchange significa
     * accodarlo senza doverlo inserire nel punto giusto.
     * @param voci le voci da ordinare, non modificato
     * @return un nuovo array con le voci speciali in testa e le altre in ordine alfabetico
     */
    static String[] ordinaVoci(String[] voci) {
        java.util.List<String> speciali = new ArrayList<>();
        java.util.List<String> nomi = new ArrayList<>();

        for (String v : voci) {
            if (isVoceSpeciale(v)) {
                speciali.add(v);
            } else {
                nomi.add(v);
            }
        }
        nomi.sort(String.CASE_INSENSITIVE_ORDER);

        java.util.List<String> ordinate = new ArrayList<>(speciali);
        ordinate.addAll(nomi);
        return ordinate.toArray(new String[0]);
    }

    /**
     * @return {@code true} se la voce è un separatore o un segnaposto, non un nome da ordinare
     *         <p>Stessa regola con cui {@link LoghiImport} decide a quali voci non mettere il logo: una
     *         voce speciale non è il nome di una piattaforma né in un elenco né nell'altro.
     */
    private static boolean isVoceSpeciale(String voce) {
        return LoghiImport.isVoceSpeciale(voce);
    }

    //=== IDENTIFICATIVI DEGLI IMPORT NATIVI ===
    //Sono codici interni, distinti dall'etichetta mostrata: così rinominare una voce nella combo non
    //tocca lo smistamento. Prima l'etichetta era anche il discriminante (confronti su "Binance CSV",
    //"OKX CSV", prefisso "[JSON]") e ogni rinomina rischiava di rompere silenziosamente un ramo.
    static final String NAT_CDC_APP        = "CDC_APP";
    static final String NAT_CDC_EXCHANGE   = "CDC_EXCHANGE";
    static final String NAT_BINANCE_OLD    = "BINANCE_OLD";
    static final String NAT_BINANCE_REPORT = "BINANCE_REPORT";
    static final String NAT_BINANCE_DUAL_INVESTMENT = "BINANCE_DUAL_INVESTMENT";
    static final String NAT_COINTRACKING   = "COINTRACKING";
    static final String NAT_TATAX_OLD      = "TATAX_OLD";
    static final String NAT_OKX_OLD        = "OKX_OLD";

    /**
     * Fornitori riconosciuti dalla parola contenuta nel nome del file di configurazione.
     * <p>Serve ai formati che non appartengono a un singolo exchange: l'esportazione di un servizio di
     * rendicontazione contiene i movimenti di piattaforme diverse — quale si sceglie poi nella finestra —
     * quindi la configurazione non fissa nessun {@code nomeExchange} da cui dedurre il raggruppamento.
     * Per gli exchange veri il problema non si pone, il nome della piattaforma è già nella configurazione.
     * <p>Le chiavi vanno scritte normalizzate: minuscole e senza caratteri non alfanumerici.
     */
    private static final java.util.Map<String, String> FORNITORI_DA_NOME_FILE = new java.util.LinkedHashMap<>();

    static {
        FORNITORI_DA_NOME_FILE.put("cointracking", "CoinTracking");
        //Refuso presente nei nomi dei file distribuiti: "Cointraking", senza la seconda c
        FORNITORI_DA_NOME_FILE.put("cointraking", "CoinTracking");
        FORNITORI_DA_NOME_FILE.put("tatax", "Tatax");
    }

    /**
     * Riconosce il fornitore dalla parola contenuta nel nome di un file di configurazione.
     * @param nomeFile nome del file senza estensione
     * @return il nome del fornitore, oppure {@code null} se il nome non contiene nessuna delle parole note
     */
    static String FornitoreDaNomeFile(String nomeFile) {
        //Stessa normalizzazione dello slug dei loghi, meno i trattini: così "Cointraking (Vecchio
        //Layout)", "cointracking_2024" e "CoinTracking.info" cadono tutti sulla stessa chiave
        String normalizzato = LoghiImport.Slug(nomeFile).replace("-", "");
        for (java.util.Map.Entry<String, String> e : FORNITORI_DA_NOME_FILE.entrySet()) {
            if (normalizzato.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Una voce del menù "tipo di file da importare": o un import nativo, scritto nel programma, o una
     * configurazione JSON letta da {@code config/import/} (o dalla vecchia {@code ImportConfig/}).
     * <p>Le due famiglie convivono nello stesso elenco ordinato alfabeticamente, senza più il prefisso
     * {@code [JSON]} che le distingueva a vista: la distinzione ora è nei campi, non nel testo.
     */
    static final class VoceImport implements LoghiImport.VoceConTooltip {

        /** Exchange o fornitore dei dati: è la voce della prima combo e raggruppa le estrazioni */
        final String fornitore;
        /** Nome dell'estrazione dentro il fornitore: è la voce della seconda combo */
        final String estrazione;
        /** Identificativo dell'import nativo, {@code null} per le configurazioni JSON */
        final String idNativo;
        /** File di configurazione JSON, {@code null} per gli import nativi */
        final java.io.File fileJson;
        /** Testo del campo {@code descrizione} del JSON, mostrato come tooltip sulla voce; può essere vuoto */
        final String descrizione;

        VoceImport(String fornitore, String estrazione, String idNativo, java.io.File fileJson) {
            this(fornitore, estrazione, idNativo, fileJson, null);
        }

        VoceImport(String fornitore, String estrazione, String idNativo, java.io.File fileJson, String descrizione) {
            this.fornitore = fornitore;
            this.estrazione = estrazione;
            this.idNativo = idNativo;
            this.fileJson = fileJson;
            this.descrizione = descrizione == null ? "" : descrizione.trim();
        }

        /** @return la descrizione da mostrare come tooltip, {@code null} se assente */
        @Override
        public String tooltip() {
            return descrizione.isBlank() ? null : descrizione;
        }

        /** @return {@code true} se la voce è una configurazione JSON */
        boolean isJson() {
            return fileJson != null;
        }

        /**
         * @param id uno degli identificativi {@code NAT_*}
         * @return {@code true} se la voce è l'import nativo indicato
         */
        boolean isNativo(String id) {
            return id.equals(idNativo);
        }

        /** Nella seconda combo si mostra solo l'estrazione: il fornitore è già nella prima. */
        @Override
        public String toString() {
            return estrazione;
        }
    }

    //Il renderer con il logo sta in LoghiImport.RenderComboConLogo: lo usa anche la combo della rete
    //nella gestione dei wallet, e una copia per finestra sarebbe destinata a divergere.

    public Importazioni_Gestione() {
        ImageIcon icon = new ImageIcon(VarStatiche.getPathRisorse()+"logo.png");
        this.setIconImage(icon.getImage());
         this.setTitle("Import da File");
        setModalityType(ModalityType.APPLICATION_MODAL);
        initComponents();
        //Segnaposto finché non si sceglie che cosa importare: da quel momento il modello viene
        //rimpiazzato con Exchanges, Wallets o BlockChain in ComboBox_TipoImportItemStateChanged.
        //Prima nel .form c'era una copia completa e disallineata dell'array Exchanges, mai usata.
        ComboBox_Exchanges.setModel(new DefaultComboBoxModel<>(new String[]{" - nessuno -"}));
        ComboBox_Exchanges.setRenderer(new LoghiImport.RenderComboConLogo());
        popolaComboTipoFile();
    }

    /**
     * Registra la {@code JList} interna del popup di una combo con il {@link javax.swing.ToolTipManager},
     * così {@code JList.getToolTipText(MouseEvent)} interroga il renderer riga per riga e il tooltip
     * impostato da {@link LoghiImport.RenderComboConLogo} compare passando il mouse sulle voci a tendina.
     * <p>Chiamata dopo ogni {@code setModel}: {@code ToolTipManager.registerComponent} è idempotente
     * (rimuove i propri listener prima di riaggiungerli) e a modello sostituito la {@code JList} del
     * popup potrebbe non essere la stessa. Chiamarla qui, non nel costruttore, evita anche di forzare
     * la creazione del popup prima che la finestra sia realizzata.
     */
    private static void registraTooltipTendina(javax.swing.JComboBox<?> combo) {
        try {
            Object popup = combo.getUI().getAccessibleChild(combo, 0);
            if (popup instanceof javax.swing.plaf.basic.BasicComboPopup bcp) {
                javax.swing.ToolTipManager.sharedInstance().registerComponent(bcp.getList());
            }
        } catch (Exception ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }

    
/**
 * Elenca le configurazioni di import disponibili, unendo le due cartelle in cui possono trovarsi.
 * <p>Da {@code config/import/} (sincronizzata con il repository) vengono prese tutte le configurazioni;
 * dalla vecchia {@code ImportConfig/}, che non è più sincronizzata e resta solo per retrocompatibilità,
 * vengono prese quelle scritte dall'utente, cioè NON marcate {@code "centralizzato": true}: le altre
 * sono la copia obsoleta di file ora gestiti sotto {@code config/} e comparirebbero doppie.
 * <p>Lo scarto delle centralizzate scatta però solo se {@code config/import/} contiene già qualcosa.
 * Al primo avvio dopo l'aggiornamento la cartella nuova è vuota — viene riempita in background, e la
 * finestra di import può essere aperta prima che il download finisca, o senza connessione — e senza
 * questa condizione sparirebbero dall'elenco tutte le configurazioni distribuite col programma.
 * A parità di nome file vince comunque la versione in {@code config/import/}.
 * @return i file di configurazione da proporre, ordinati per nome
 */
private static java.util.List<java.io.File> elencoConfigurazioniImport() {
    java.util.Map<String, java.io.File> perNome = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    java.io.File[] nuovi = new java.io.File(VarStatiche.getCartella_ConfigImport())
            .listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
    if (nuovi != null) {
        for (java.io.File f : nuovi) {
            perNome.put(f.getName(), f);
        }
    }
    boolean cartellaNuovaPopolata = !perNome.isEmpty();

    java.io.File[] vecchi = new java.io.File(VarStatiche.getCartella_ImportConfig())
            .listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
    if (vecchi != null) {
        for (java.io.File f : vecchi) {
            if (perNome.containsKey(f.getName())) {
                continue;
            }
            if (cartellaNuovaPopolata) {
                try {
                    String contenuto = new String(java.nio.file.Files.readAllBytes(f.toPath()),
                            java.nio.charset.StandardCharsets.UTF_8);
                    if (new org.json.JSONObject(contenuto).optBoolean("centralizzato", false)) {
                        continue;
                    }
                } catch (Exception ex) {
                    //File illeggibile o non JSON: lo propongo comunque, sarà il caricamento a segnalare l'errore
                    LoggerGC.ScriviErrore(ex);
                }
            }
            perNome.put(f.getName(), f);
        }
    }

    return new java.util.ArrayList<>(perNome.values());
}

/**
 * Tutte le estrazioni disponibili, native e da configurazione JSON, raggruppate per fornitore.
 * <p>Costruita una volta all'apertura della finestra e usata per riempire le due combo: la prima elenca
 * le chiavi, la seconda le estrazioni del fornitore scelto.
 */
private final java.util.Map<String, java.util.List<VoceImport>> estrazioniPerFornitore =
        new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);

/**
 * Raccoglie gli import nativi e le configurazioni JSON e li raggruppa per fornitore.
 * <p>Il nome dell'estrazione è volutamente scritto senza il fornitore davanti: quello sta già nella
 * prima combo, ripeterlo allungherebbe le voci senza aggiungere informazione.
 */
private void raccogliEstrazioni() {
    estrazioniPerFornitore.clear();

    aggiungiEstrazione(new VoceImport("Crypto.com",   "App CSV",          NAT_CDC_APP,        null));
    aggiungiEstrazione(new VoceImport("Crypto.com",   "Exchange CSV",     NAT_CDC_EXCHANGE,   null));
    aggiungiEstrazione(new VoceImport("Binance",      "Formato storico",  NAT_BINANCE_OLD,    null));
    aggiungiEstrazione(new VoceImport("Binance",      "Financial Report", NAT_BINANCE_REPORT, null));
    aggiungiEstrazione(new VoceImport("Binance",      "Dettaglio Dual Investment", NAT_BINANCE_DUAL_INVESTMENT, null));
    aggiungiEstrazione(new VoceImport("CoinTracking", "Formato storico",  NAT_COINTRACKING,   null));
    aggiungiEstrazione(new VoceImport("Tatax",        "Formato storico",  NAT_TATAX_OLD,      null));
    aggiungiEstrazione(new VoceImport("OKX",          "Formato storico",  NAT_OKX_OLD,        null));

    try {
        for (java.io.File f : elencoConfigurazioniImport()) {
            // Il nome base è sempre il nome del file senza estensione
            String nomeBase = f.getName().replaceAll("(?i)\\.json$", "");

            //La parola nel nome del file viene riconosciuta prima ancora di leggere la configurazione,
            //così una esportazione CoinTracking o Tatax finisce sotto il proprio fornitore anche se il
            //file è illeggibile
            String daNomeFile = FornitoreDaNomeFile(nomeBase);
            //Senza nessun indizio la configurazione resta utilizzabile: diventa un fornitore a sé con
            //una sola estrazione, che è come si comportano le configurazioni scritte dall'utente
            String fornitore = daNomeFile != null ? daNomeFile : nomeBase;
            String estrazione = nomeBase;
            String descrizione = null;
            boolean nonAncoraSupportata = false;

            try {
                ImportazioneGenerica.ConfigurazioneImport cfg =
                        ImportazioneGenerica.ConfigurazioneImport.carica(f.getAbsolutePath());

                // Configurazione che usa una funzionalità del formato più recente di questa
                // installazione (vedi ConfigurazioneImport.versioneMinimaApp): non si propone, altrimenti
                // l'utente la sceglierebbe e otterrebbe un import silenziosamente sbagliato (campi nuovi
                // ignorati) invece che il messaggio esplicito di questo skip. Un caricamento diretto (non
                // da questa finestra) resta possibile: il vincolo è solo qui, non in
                // ConfigurazioneImport.carica.
                if (!Funzioni.VersioneAppAlmeno(VarStatiche.Versione, cfg.versioneMinimaApp)) {
                    LoggerGC.logInfo("Configurazione di import \"" + f.getName() + "\" richiede almeno la versione "
                            + cfg.versioneMinimaApp + " (questa è la " + VarStatiche.Versione + "): non proposta.");
                    nonAncoraSupportata = true;
                }

                if (cfg.fornitore != null && !cfg.fornitore.isBlank()) {
                    //Indicazione esplicita: vince su tutto
                    fornitore = cfg.fornitore.trim();
                } else if (daNomeFile == null && cfg.nomeExchange != null && !cfg.nomeExchange.isBlank()) {
                    //Formato di un singolo exchange: il nome della piattaforma è già nella configurazione
                    fornitore = cfg.nomeExchange.trim();
                }

                if (cfg.estrazione != null && !cfg.estrazione.isBlank()) {
                    estrazione = cfg.estrazione.trim();
                }

                if (cfg.testing) {
                    estrazione = estrazione + " (In fase di test, utilizzo consapevole)";
                }

                descrizione = cfg.descrizione;

            } catch (Exception ex) {
                LoggerGC.ScriviErrore(ex);
            }

            if (nonAncoraSupportata) {
                continue;
            }
            aggiungiEstrazione(new VoceImport(fornitore, estrazione, null, f, descrizione));
        }
    } catch (Exception ex) {
        LoggerGC.ScriviErrore(ex);
    }

    //Dentro ogni fornitore le estrazioni restano in ordine alfabetico, come le voci della prima combo
    for (java.util.List<VoceImport> elenco : estrazioniPerFornitore.values()) {
        elenco.sort(java.util.Comparator.comparing(v -> v.estrazione, String.CASE_INSENSITIVE_ORDER));
    }
}

/** Registra una estrazione sotto il proprio fornitore. */
private void aggiungiEstrazione(VoceImport voce) {
    estrazioniPerFornitore.computeIfAbsent(voce.fornitore, f -> new ArrayList<>()).add(voce);
}

/**
 * Riempie la combo dei fornitori con le chiavi raccolte, in ordine alfabetico ignorando maiuscole e
 * minuscole, e a cascata quella delle estrazioni.
 * <p>Le due combo sono volutamente vuote nel {@code .form}: il loro contenuto dipende dai file presenti
 * su disco, quindi non può stare nel modello generato dal Designer.
 */
private void popolaComboTipoFile() {
    raccogliEstrazioni();

    //setModel selezionerebbe la prima voce facendo scattare ComboBox_TipoFileItemStateChanged su una
    //finestra non ancora inizializzata: stacco il listener e allineo lo stato una volta sola alla fine
    java.awt.event.ItemListener[] listeners = ComboBox_TipoFile.getItemListeners();
    for (java.awt.event.ItemListener l : listeners) {
        ComboBox_TipoFile.removeItemListener(l);
    }
    //La TreeMap è già ordinata ignorando maiuscole e minuscole
    ComboBox_TipoFile.setModel(new DefaultComboBoxModel<>(
            estrazioniPerFornitore.keySet().toArray(new String[0])));
    for (java.awt.event.ItemListener l : listeners) {
        ComboBox_TipoFile.addItemListener(l);
    }
    ComboBox_TipoFile.setRenderer(new LoghiImport.RenderComboConLogo());
    registraTooltipTendina(ComboBox_TipoFile);

    popolaComboTipoEstrazione();
}

/**
 * Riempie la combo delle estrazioni con quelle del fornitore selezionato, e la abilita solo se c'è più
 * di una scelta da fare: con una sola estrazione la voce resta visibile — così si sa che cosa verrà
 * importato — ma non c'è nulla da scegliere.
 */
private void popolaComboTipoEstrazione() {
    Object fornitore = ComboBox_TipoFile.getSelectedItem();
    java.util.List<VoceImport> elenco = fornitore == null
            ? java.util.List.of()
            : estrazioniPerFornitore.getOrDefault(fornitore.toString(), java.util.List.of());

    java.awt.event.ItemListener[] listeners = ComboBox_TipoEstrazione.getItemListeners();
    for (java.awt.event.ItemListener l : listeners) {
        ComboBox_TipoEstrazione.removeItemListener(l);
    }
    ComboBox_TipoEstrazione.setModel(new DefaultComboBoxModel<>(elenco.toArray(new VoceImport[0])));
    for (java.awt.event.ItemListener l : listeners) {
        ComboBox_TipoEstrazione.addItemListener(l);
    }
    //Fa comparire il tooltip della descrizione anche sulle righe della tendina (non solo sulla combo
    //chiusa): la JList del popup può cambiare a modello sostituito, registerComponent è idempotente
    registraTooltipTendina(ComboBox_TipoEstrazione);

    boolean daScegliere = elenco.size() > 1;
    ComboBox_TipoEstrazione.setEnabled(daScegliere);
    Label_TipoEstrazione.setEnabled(daScegliere);

    aggiornaStatoPerVoceSelezionata();
}

/** @return l'estrazione selezionata, {@code null} se non ce n'è nessuna disponibile */
private VoceImport voceSelezionata() {
    return (VoceImport) ComboBox_TipoEstrazione.getSelectedItem();
}
   
   
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        Label_TipoFile = new javax.swing.JLabel();
        ComboBox_TipoFile = new javax.swing.JComboBox<>();
        Label_TipoEstrazione = new javax.swing.JLabel();
        ComboBox_TipoEstrazione = new javax.swing.JComboBox<>();
        Bottone_SelezionaFile = new javax.swing.JButton();
        Label_NomeExchange = new javax.swing.JLabel();
        ScrollPane_Attenzione = new javax.swing.JScrollPane();
        TextPane_Attenzione = new javax.swing.JTextPane();
        Bottone_Annulla = new javax.swing.JButton();
        CheckBox_Sovrascrivi = new javax.swing.JCheckBox();
        ComboBox_Exchanges = new javax.swing.JComboBox<>();
        Text_NomeWallet = new javax.swing.JTextField();
        Label_NomeWallet = new javax.swing.JLabel();
        ComboBox_TipoImport = new javax.swing.JComboBox<>();
        Label_TipoImport = new javax.swing.JLabel();
        Bottone_Manuale = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        Label_TipoFile.setFont(new java.awt.Font("Noto Sans", 0, 14)); // NOI18N
        Label_TipoFile.setText("Selezionare l'exchange o il fornitore dei dati");

        ComboBox_TipoFile.setFont(new java.awt.Font("Noto Sans", 1, 14)); // NOI18N
        ComboBox_TipoFile.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                ComboBox_TipoFileItemStateChanged(evt);
            }
        });

        Label_TipoEstrazione.setFont(new java.awt.Font("Noto Sans", 0, 14)); // NOI18N
        Label_TipoEstrazione.setText("Selezionare il tipo di estrazione");
        Label_TipoEstrazione.setEnabled(false);

        ComboBox_TipoEstrazione.setFont(new java.awt.Font("Noto Sans", 1, 14)); // NOI18N
        ComboBox_TipoEstrazione.setEnabled(false);
        ComboBox_TipoEstrazione.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                ComboBox_TipoEstrazioneItemStateChanged(evt);
            }
        });

        Bottone_SelezionaFile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Upload.png"))); // NOI18N
        Bottone_SelezionaFile.setText("<html><center><h2>Seleziona file da importare</h2></html>");
        Bottone_SelezionaFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_SelezionaFileActionPerformed(evt);
            }
        });

        Label_NomeExchange.setFont(new java.awt.Font("Noto Sans", 0, 14)); // NOI18N
        Label_NomeExchange.setText("Scegli il nome dell'Exchange/Wallet/Blockchain da Importare");
        Label_NomeExchange.setEnabled(false);

        TextPane_Attenzione.setEditable(false);
        TextPane_Attenzione.setContentType("text/html"); // NOI18N
        TextPane_Attenzione.setFont(new java.awt.Font("Noto Sans", 0, 14)); // NOI18N
        TextPane_Attenzione.setText("<html>\n<head>\n<style>\n  body {\n    font-family: 'Noto Sans', Arial, sans-serif;\n    font-size: 13px;\n    margin: 10px 14px;\n    line-height: 1.6;\n  }\n  p { margin: 5px 0; }\n  b { font-weight: bold; }\n  .mono {\n    font-family: monospace;\n    font-size: 12px;\n    font-weight: bold;\n  }\n  ul {\n    margin: 6px 0 4px 18px;\n    padding: 0;\n  }\n  li { margin-bottom: 3px; }\n</style>\n</head>\n<body>\n\n<p><b>Attenzione <br>\nImportazione da cointracking.info o Tatax</b></p>\n\n<p>Per una corretta importazione &egrave; necessario:</p>\n\n<ul>\n  <li>Importare i dati <b>un solo exchange / wallet per volta</b></li>\n  <li>Impostare il <b>nome dell&#39;exchange o wallet</b> nel campo sottostante</li>\n</ul>\n\n<p><i>Esempio:</i> &nbsp;<span class=\"mono\">Binance</span></p>\n\n</body>\n</html>\n");
        TextPane_Attenzione.setEnabled(false);
        ScrollPane_Attenzione.setViewportView(TextPane_Attenzione);

        Bottone_Annulla.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Annulla.png"))); // NOI18N
        Bottone_Annulla.setText("<html><h2>Annulla</h2></html>");
        Bottone_Annulla.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_AnnullaActionPerformed(evt);
            }
        });

        CheckBox_Sovrascrivi.setFont(new java.awt.Font("Noto Sans", 0, 14)); // NOI18N
        CheckBox_Sovrascrivi.setText("Sovrascrivere movimenti già presenti");

        ComboBox_Exchanges.setFont(new java.awt.Font("Noto Sans", 0, 14)); // NOI18N
        ComboBox_Exchanges.setEnabled(false);
        ComboBox_Exchanges.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                ComboBox_ExchangesItemStateChanged(evt);
            }
        });

        Text_NomeWallet.setEnabled(false);
        Text_NomeWallet.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                Text_NomeWalletKeyReleased(evt);
            }
        });

        Label_NomeWallet.setFont(new java.awt.Font("Noto Sans", 0, 14)); // NOI18N
        Label_NomeWallet.setText("Indicare nome o indirizzo del Wallet");
        Label_NomeWallet.setEnabled(false);

        ComboBox_TipoImport.setFont(new java.awt.Font("Noto Sans", 0, 14)); // NOI18N
        ComboBox_TipoImport.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "------------", "Exchange", "Wallet", "Transazioni Blockchain" }));
        ComboBox_TipoImport.setEnabled(false);
        ComboBox_TipoImport.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                ComboBox_TipoImportItemStateChanged(evt);
            }
        });

        Label_TipoImport.setFont(new java.awt.Font("Noto Sans", 0, 14)); // NOI18N
        Label_TipoImport.setText("Scegliere che cosa si vuole importare");
        Label_TipoImport.setEnabled(false);

        Bottone_Manuale.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/24_Libro.png"))); // NOI18N
        Bottone_Manuale.setText("<html><h2>Istruzioni</h2></html>");
        Bottone_Manuale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Bottone_ManualeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Label_NomeExchange, javax.swing.GroupLayout.DEFAULT_SIZE, 589, Short.MAX_VALUE)
                    .addComponent(CheckBox_Sovrascrivi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Label_TipoImport, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ComboBox_TipoImport, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ComboBox_Exchanges, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Text_NomeWallet)
                    .addComponent(Label_TipoFile, javax.swing.GroupLayout.DEFAULT_SIZE, 589, Short.MAX_VALUE)
                    .addComponent(Label_NomeWallet, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Bottone_SelezionaFile, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 109, Short.MAX_VALUE)
                        .addComponent(Bottone_Annulla, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(ComboBox_TipoFile, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(Label_TipoEstrazione, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ComboBox_TipoEstrazione, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ScrollPane_Attenzione, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 403, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Bottone_Manuale, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Label_TipoFile, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ComboBox_TipoFile, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Label_TipoEstrazione, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ComboBox_TipoEstrazione, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(CheckBox_Sovrascrivi, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Label_TipoImport, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ComboBox_TipoImport, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(Label_NomeExchange, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ComboBox_Exchanges, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(Label_NomeWallet, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Text_NomeWallet, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(ScrollPane_Attenzione))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(Bottone_Annulla, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(Bottone_Manuale, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(Bottone_SelezionaFile, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void ComboBox_TipoFileItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_ComboBox_TipoFileItemStateChanged
        //Cambiando fornitore cambiano le estrazioni disponibili; lo stato dei campi viene poi
        //riallineato da popolaComboTipoEstrazione sulla nuova selezione
        popolaComboTipoEstrazione();
    }//GEN-LAST:event_ComboBox_TipoFileItemStateChanged

    private void ComboBox_TipoEstrazioneItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_ComboBox_TipoEstrazioneItemStateChanged
        aggiornaStatoPerVoceSelezionata();
    }//GEN-LAST:event_ComboBox_TipoEstrazioneItemStateChanged

    /**
     * Abilita i campi della finestra in base alla voce selezionata nella combo del tipo di file.
     * <p>Richiamata sia dall'evento della combo sia una volta al termine di {@link #popolaComboTipoFile()},
     * perché il riempimento avviene con il listener staccato.
     */
    private void aggiornaStatoPerVoceSelezionata() {
        VoceImport voceSelezionata = voceSelezionata();

        //Descrizione dell'import come tooltip: sulla combo delle estrazioni (chiusa e a tendina, via
        //renderer) e anche su quella dei fornitori, che resta sempre abilitata quando l'estrazione è
        //unica e la sua combo è disattivata.
        String tip = voceSelezionata != null ? voceSelezionata.tooltip() : null;
        ComboBox_TipoEstrazione.setToolTipText(tip);
        ComboBox_TipoFile.setToolTipText(tip);

        if (voceSelezionata == null) {
            disabilitaCampiImport();
            return;
        }

        if (voceSelezionata.isJson()) {

            try {
                ImportazioneGenerica.ConfigurazioneImport cfg
                        = ImportazioneGenerica.ConfigurazioneImport.carica(voceSelezionata.fileJson.getAbsolutePath());

                String nomeExchange = cfg.nomeExchange != null ? cfg.nomeExchange.trim() : "";

                if (nomeExchange.isBlank()) {
                    ComboBox_Exchanges.setModel(new DefaultComboBoxModel<>(ordinaVoci(Exchanges)));

                    Label_TipoImport.setEnabled(true);
                    ComboBox_TipoImport.setEnabled(true);
                    TextPane_Attenzione.setEnabled(true);
                    ComboBox_TipoImport.setSelectedIndex(0);

                    Label_NomeExchange.setEnabled(true);
                    ComboBox_Exchanges.setEnabled(true);
                    Bottone_SelezionaFile.setEnabled(false);

                } else {
                    Label_TipoImport.setEnabled(false);
                    ComboBox_TipoImport.setEnabled(false);
                    Label_NomeExchange.setEnabled(false);
                    ComboBox_Exchanges.setEnabled(false);
                    Text_NomeWallet.setEnabled(false);
                    TextPane_Attenzione.setEnabled(false);
                    Bottone_SelezionaFile.setEnabled(true);
                }

            } catch (Exception ex) {
                LoggerGC.ScriviErrore(ex);
                disabilitaCampiImport();
            }
        } else if (voceSelezionata.isNativo(NAT_COINTRACKING)
                || voceSelezionata.isNativo(NAT_TATAX_OLD)) {

            // --- Comportamento originale CoinTracking / Tatax ---
            Label_TipoImport.setEnabled(true);
            ComboBox_TipoImport.setEnabled(true);
            TextPane_Attenzione.setEnabled(true);
            ComboBox_TipoImport.setSelectedIndex(0);
            Bottone_SelezionaFile.setEnabled(false);

        } else {

            // --- Tutte le altre voci native ---
            Label_NomeExchange.setEnabled(false);
            Label_TipoImport.setEnabled(false);
            ComboBox_Exchanges.setEnabled(false);
            ComboBox_TipoImport.setEnabled(false);
            Text_NomeWallet.setEnabled(false);
            TextPane_Attenzione.setEnabled(false);
            Bottone_SelezionaFile.setEnabled(true);
        }
    }

    /** Disabilita i campi dipendenti dal tipo di file, usato quando la voce selezionata non è utilizzabile. */
    private void disabilitaCampiImport() {
        Label_NomeExchange.setEnabled(false);
        Label_TipoImport.setEnabled(false);
        ComboBox_Exchanges.setEnabled(false);
        ComboBox_TipoImport.setEnabled(false);
        Text_NomeWallet.setEnabled(false);
        TextPane_Attenzione.setEnabled(false);
        Bottone_SelezionaFile.setEnabled(false);
    }

    private void Bottone_AnnullaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_AnnullaActionPerformed
        // TODO add your handling code here:
        this.dispose();
    }//GEN-LAST:event_Bottone_AnnullaActionPerformed
   
    
    
    /**
     * Apre la scelta dei file da importare partendo dall'ultima cartella usata, e la ricorda.
     * @param multipla {@code true} per permettere di scegliere piu' file in una volta
     * @return i file scelti (almeno uno), oppure {@code null} se l'utente ha annullato
     */
    private File[] ScegliFileDaImportare(boolean multipla) {
        JFileChooser fc = new JFileChooser(DatabaseH2.Pers_Opzioni_Leggi("Directory_ImportazioniGestione"));
        fc.setMultiSelectionEnabled(multipla);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File[] files = multipla ? fc.getSelectedFiles() : new File[]{fc.getSelectedFile()};
        if (files == null || files.length == 0 || files[0] == null) {
            return null;
        }
        DatabaseH2.Pers_Opzioni_Scrivi("Directory_ImportazioniGestione", files[0].getParent());
        return files;
    }

    /**
     * Controlla i file scelti per "Crypto.com - App CSV" prima di importarli: per ognuno che sembra il Fiat
     * Wallet, la carta o nemmeno un export dell'app, dice dove va caricato e lascia scegliere se saltarlo o
     * importarlo comunque (il riconoscimento puo' sbagliare, quindi non blocca mai).
     * @param files i file scelti
     * @return i file da importare, oppure {@code null} se non ne resta nessuno
     */
    private File[] FileCDCAppDaImportare(File[] files) {
        java.util.List<File> daImportare = new ArrayList<>();
        for (File file : files) {
            CDC_FiatECardWallet.TipoFileCDC tipo = CDC_FiatECardWallet.RiconosciFile(file);
            String messaggio = switch (tipo) {
                case FIAT_WALLET -> "Il file \"" + file.getName() + "\" sembra l'estratto del Fiat Wallet di Crypto.com,\n"
                        + "non quello delle transazioni crypto dell'App.\n\n"
                        + "Il Fiat Wallet va caricato dalla scheda \"Fiat Wallet Crypto.com\",\n"
                        + "con il pulsante \"Carica Dati Fiat Wallet\".\n"
                        + "Importato qui, i suoi movimenti risulterebbero sconosciuti.";
                case CARD_WALLET -> "Il file \"" + file.getName() + "\" sembra l'estratto della Carta Crypto.com,\n"
                        + "non quello delle transazioni crypto dell'App.\n\n"
                        + "La carta va caricata dalla scheda \"Carta Crypto.com\",\n"
                        + "con il pulsante \"Carica Dati Carta\".\n"
                        + "Importato qui, i suoi movimenti risulterebbero sconosciuti.";
                case SCONOSCIUTO -> "Il file \"" + file.getName() + "\" non sembra un export dell'App Crypto.com:\n"
                        + "manca l'intestazione \"Timestamp (UTC),Transaction Description,...\".\n\n"
                        + "Controllare di aver scelto il file giusto, o il tipo di importazione giusto.";
                default -> null;
            };
            if (messaggio == null) {
                daImportare.add(file);
                continue;
            }
            Object[] opzioni = {"Salta il file", "Importa comunque"};
            int scelta = JOptionPane.showOptionDialog(this, messaggio, "File non riconosciuto",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, opzioni, opzioni[0]);
            if (scelta == 1) {
                daImportare.add(file);
            }
        }
        return daImportare.isEmpty() ? null : daImportare.toArray(new File[0]);
    }

    /** L'importazione di un singolo file: restituisce quello che restituisce {@link DocumentiFonte#EseguiImportDaFile}. */
    private interface ImportDaFile {
        boolean importa(File file, Download progressb);
    }

    /**
     * Importa uno dopo l'altro i file scelti con lo stesso import, in background e con una sola finestra di
     * avanzamento, e alla fine mostra un solo resoconto con la somma di tutti.
     * <p>Ogni file resta un documento di origine a se' ({@code EseguiImportDaFile} per file), cosi' la
     * deduplica, il campo [41] e l'eventuale cancellazione del documento funzionano come per un file solo.
     * Un Interrompi ferma il file in corso e non fa partire i successivi.
     * @param files i file da importare, nell'ordine in cui sono stati scelti
     * @param ripristinaStdout {@code true} per gli import che a fine lavoro staccavano il log dalla finestra
     * @param importazione l'import di un file
     */
    private void ImportaFileInSequenza(File[] files, boolean ripristinaStdout, ImportDaFile importazione) {
        Component c = this;
        Download progressb = new Download();
        Bottone_SelezionaFile.setEnabled(false);
        Bottone_Annulla.setEnabled(false);
        Thread thread = new Thread() {
            /** Esegue in background l'importazione dei file scelti. */
            @Override
            public void run() {
                try {
                    c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    RiepilogoImport riepilogo = new RiepilogoImport();
                    for (int i = 0; i < files.length; i++) {
                        if (riepilogo.interrotto || progressb.FineThread) {
                            break;
                        }
                        if (files.length > 1) {
                            progressb.setTitle("File " + (i + 1) + " di " + files.length + " - " + files[i].getName());
                        }
                        Importazioni.AzzeraContatori();
                        boolean ok = importazione.importa(files[i], progressb);
                        if (ok && Importazioni.TransazioniAggiunte > 0) {
                            Principale.TabellaCryptodaAggiornare = true;
                        }
                        riepilogo.aggiungi(files[i]);
                    }
                    c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    riepilogo.mostra(c);
                    if (ripristinaStdout) {
                        progressb.RipristinaStdout();
                    }
                    dispose();
                } catch (Exception ex) {
                    LoggerGC.ScriviErrore(ex);
                } finally {
                    c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    Bottone_SelezionaFile.setEnabled(true);
                    Bottone_Annulla.setEnabled(true);
                    progressb.dispose();
                }
            }
        };
        progressb.SetThread(thread);
        thread.start();
        progressb.setDefaultCloseOperation(0);
        progressb.setLocationRelativeTo(this);
        progressb.setVisible(true);
    }

    /**
     * Somma gli esiti di piu' file importati di seguito per mostrarne un solo resoconto.
     * <p>Ogni import chiama {@link Importazioni#AzzeraContatori()}, che oltre ai contatori azzera anche le
     * causali derivati segnalate e i giroconti FIAT abbinati: qui si raccolgono file per file e si rimettono
     * al loro posto prima di aprire il resoconto, che li legge da li'. Con un file solo il resoconto e'
     * identico a quello di prima della selezione multipla.
     */
    private static final class RiepilogoImport {
        private final Importazioni.Esito esito = new Importazioni.Esito();
        private final java.util.Set<String> derivati = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        private int giroconti = 0;
        private int numeroFile = 0;
        /** L'ultimo file e' stato interrotto dall'utente: i successivi non vanno importati. */
        boolean interrotto = false;

        /** Raccoglie l'esito dell'import appena finito, letto dai contatori statici. */
        void aggiungi(File file) {
            esito.Somma(Importazioni.Esito.daiContatori(file.getName()));
            derivati.addAll(Importazioni.CausaliDerivatiSegnalate);
            giroconti += GirocontiFiat.AbbinatiImportazione;
            interrotto = DocumentiFonte.UltimoImportInterrotto;
            numeroFile++;
        }

        /** Mostra il resoconto (modale) di tutti i file importati. */
        void mostra(Component c) {
            if (numeroFile == 0) {
                return;
            }
            Importazioni.CausaliDerivatiSegnalate.clear();
            Importazioni.CausaliDerivatiSegnalate.addAll(derivati);
            GirocontiFiat.AbbinatiImportazione = giroconti;
            DocumentiFonte.UltimoImportInterrotto = interrotto;
            Importazioni_Resoconto res = new Importazioni_Resoconto();
            if (numeroFile == 1) {
                res.ImpostaValori(Importazioni.Transazioni, Importazioni.TransazioniAggiunte, Importazioni.TrasazioniScartate,
                        Importazioni.TrasazioniSconosciute, Importazioni.movimentiSconosciuti);
            } else {
                //Somma() concatena le origini ("a.csv + b.csv + ..."): per il titolo basta il tipo di import
                //e il numero di file, i nomi restano come intestazioni nell'elenco dei movimenti sconosciuti
                String descrizione = DocumentiFonte.UltimaDescrizioneImport == null ? "" : DocumentiFonte.UltimaDescrizioneImport;
                esito.Origine = (descrizione.isBlank() ? "" : descrizione + " ") + "(" + numeroFile + " file)";
                res.ImpostaValori(esito);
            }
            res.setLocationRelativeTo(c);
            res.setVisible(true);
        }
    }

    private void Bottone_SelezionaFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_SelezionaFileActionPerformed

        // boolean selezioneok[]=new boolean[]{false};
        //this.setCursor(Cursor.WAIT_CURSOR);
final VoceImport voce = voceSelezionata();

if (voce == null) {
    return;
}

if (voce.isJson()) {

    final String percorsoJson = voce.fileJson.getAbsolutePath();

    if (!voce.fileJson.exists()) {
        JOptionPane.showMessageDialog(
                this,
                "Configurazione JSON non trovata.",
                "Attenzione",
                JOptionPane.WARNING_MESSAGE
        );
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        return;
    }

    final String[] nomeExchangeFinale = new String[1];
    final String[] fusoFinale = new String[]{""};

    try {
        ImportazioneGenerica.ConfigurazioneImport cfg =
                ImportazioneGenerica.ConfigurazioneImport.carica(percorsoJson);

        String nomeExchangeDaJson = (cfg.nomeExchange != null)
                ? cfg.nomeExchange.trim()
                : "";

        fusoFinale[0] = cfg.fuso != null ? cfg.fuso.trim() : "";

        // Se il nome exchange è già nel JSON uso quello
        if (!nomeExchangeDaJson.isBlank()) {
            nomeExchangeFinale[0] = nomeExchangeDaJson;
        } else {
            // Altrimenti lo chiedo dalla combo
            nomeExchangeFinale[0] = ComboBox_Exchanges.getSelectedItem().toString().trim();

            if (nomeExchangeFinale[0].equalsIgnoreCase("----------")
                    || nomeExchangeFinale[0].isBlank()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Selezionare un Exchange/Wallet prima di procedere.",
                        "Attenzione",
                        JOptionPane.WARNING_MESSAGE
                );
                this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                return;
            }

            // Gestione nome personalizzato
            if (nomeExchangeFinale[0].equalsIgnoreCase("Nome Personalizzato")) {
                nomeExchangeFinale[0] = JOptionPane.showInputDialog(
                        this,
                        "Inserisci il nome personalizzato",
                        "Nome Exchange",
                        JOptionPane.PLAIN_MESSAGE
                );

                if (nomeExchangeFinale[0] == null || nomeExchangeFinale[0].isBlank()) {
                    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    return;
                }
            }
        }

    } catch (Exception ex) {
        LoggerGC.ScriviErrore(ex);
        JOptionPane.showMessageDialog(
                this,
                "Errore nella lettura della configurazione JSON.",
                "Errore",
                JOptionPane.ERROR_MESSAGE
        );
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        return;
    }

    //Selezione multipla: si possono importare in una volta sola piu' file dello stesso formato
    File[] files = ScegliFileDaImportare(true);
    if (files == null) {
        return;
    }

    // Risolvo il fuso orario file per file, se non specificato nel JSON
    boolean PrioritaNomeFile=false;
    //Se c'e il ? affianco al fuso allora do priorità al fuso sul file invece che quello scritto
    if (fusoFinale[0].contains("?")){
        PrioritaNomeFile=true;
        fusoFinale[0]=fusoFinale[0].replace("?", "");
    }
    //Il fuso si legge dal nome di ciascun file; se non c'e' ne' li' ne' nella configurazione si chiede una
    //volta sola e vale per tutti i file che ne sono privi
    final String[] fusoPerFile = new String[files.length];
    String fusoScelto = null;
    for (int i = 0; i < files.length; i++) {
        String fusoFile = ImportazioneGenerica.estraiTZdaNomeFile(files[i].getName());
        boolean fusoNelNome = fusoFile != null && !fusoFile.isBlank();
        if (PrioritaNomeFile && fusoNelNome) {
            fusoPerFile[i] = fusoFile;
        } else if (!fusoFinale[0].isBlank()) {
            fusoPerFile[i] = fusoFinale[0];
        } else if (fusoNelNome) {
            fusoPerFile[i] = fusoFile;
        } else {
            if (fusoScelto == null) {
                fusoScelto = AppDialog.showComboBoxDialog(
                        this,
                        "Fuso orario non specificato",
                        "Seleziona il fuso orario",
                        "Il fuso orario non è specificato nella configurazione né nel nome del file.\n\n"
                        + "Selezionare il fuso orario corretto per i dati da importare:",
                        "Fuso orario:",
                        "UTC", "UTC+1", "UTC+2", "CET", "Europe/Rome"
                );
                if (fusoScelto == null) {
                    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    return;
                }
            }
            fusoPerFile[i] = fusoScelto;
        }
    }

    final boolean SovrascriEsistenti = this.CheckBox_Sovrascrivi.isSelected();
    final java.util.List<File> elenco = java.util.Arrays.asList(files);

    ImportaFileInSequenza(files, false, (file, progressb) -> {
        String FileDaImportare = file.getAbsolutePath();
        String fuso = fusoPerFile[elenco.indexOf(file)];
        return DocumentiFonte.EseguiImportDaFile(
                file, DocumentiFonte.TIPO_CSV, new File(percorsoJson).getName(), progressb,
                () -> ImportazioneGenerica.importa(
                        FileDaImportare,
                        percorsoJson,
                        SovrascriEsistenti,
                        progressb,
                        nomeExchangeFinale[0],
                        fuso
                ));
    });
}
        
        else if (voce.isNativo(NAT_BINANCE_DUAL_INVESTMENT)) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            //Un file solo: non e' un'importazione ma l'abbinamento dei contratti di un file di dettaglio,
            //con un resoconto suo (ImpostaValoriDualInvestment) che non si somma su piu' file
            File[] files = ScegliFileDaImportare(false);
            if (files != null) {
                String FileDaImportare = files[0].getAbsolutePath();
                try {
                    Binance_DualInvestment.Esito esito = Binance_DualInvestment.Abbina(new File(FileDaImportare));
                    if (esito.abbinati > 0) {
                        Principale.TabellaCryptodaAggiornare = true;
                    }
                    Importazioni_Resoconto res = new Importazioni_Resoconto();
                    this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    res.ImpostaValoriDualInvestment(esito);
                    res.setLocationRelativeTo(this);
                    res.setVisible(true);
                } catch (Exception ex) {
                    LoggerGC.ScriviErrore(ex);
                    Messaggi.WarningMessage("Abbinamento Dual Investment",
                            "Errore durante la lettura del file: " + ex.getMessage(), this);
                }
                dispose();
            }
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        else if (voce.isNativo(NAT_CDC_APP)) {
            File[] files = ScegliFileDaImportare(true);
            if (files != null) {
                //Controllo preventivo: Fiat Wallet e carta hanno la stessa intestazione dell'App, e caricati
                //qui finiscono tutti fra le causali sconosciute
                files = FileCDCAppDaImportare(files);
            }
            if (files != null) {
                boolean SovrascriEsistenti = this.CheckBox_Sovrascrivi.isSelected();
                ImportaFileInSequenza(files, false, (file, progressb) -> DocumentiFonte.EseguiImportDaFile(
                        file, DocumentiFonte.TIPO_CSV, "Crypto.com App CSV", progressb,
                        () -> Importazioni.Ex_CDCAPP_Importa(file.getAbsolutePath(), SovrascriEsistenti, progressb)));
            }
        } else if (voce.isNativo(NAT_CDC_EXCHANGE)) {
            File[] files = ScegliFileDaImportare(true);
            if (files != null) {
                Component c = this;
                boolean SovrascriEsistenti = CheckBox_Sovrascrivi.isSelected();
                //in questo caso siccome cointracking sbaglia molto spesso i prezzi delle shitcoin imposto il prezzo a zero
                //su tutti gli scambi nel caso in cui binance non abbia i prezi corretti
                final boolean PrezzoZeroF = ComboBox_TipoImport.getSelectedItem().toString().trim().equalsIgnoreCase("Transazioni Blockchain");
                ImportaFileInSequenza(files, true, (file, progressb) -> DocumentiFonte.EseguiImportDaFile(
                        file, DocumentiFonte.TIPO_CSV, "Crypto.com Exchange CSV", progressb,
                        () -> Importazioni.Ex_CryptoComExchange_Importa(file.getAbsolutePath(), SovrascriEsistenti, c, PrezzoZeroF, progressb)));
            }
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } else if (voce.isNativo(NAT_COINTRACKING)) {

            if (ComboBox_TipoImport.getSelectedItem().toString().trim().equalsIgnoreCase("Transazioni Blockchain")) {
                NomeWallet = Text_NomeWallet.getText().trim() + " " + ComboBox_Exchanges.getSelectedItem().toString().trim().substring(ComboBox_Exchanges.getSelectedItem().toString().indexOf("("), ComboBox_Exchanges.getSelectedItem().toString().indexOf(")") + 1);

            } else {
                NomeWallet = ComboBox_Exchanges.getSelectedItem().toString().trim();
            }
            if (NomeWallet.equalsIgnoreCase("*Nome Personalizzato*")) {
                NomeWallet = "";
                Object[] options = Principale.Mappa_Wallet.keySet().toArray();
                JLabel label = new JLabel("<html>Indica o scegli il Nome che vuoi dare al Wallet<br>"
                        + "</html>");
                JComboBox<Object> comboBox = new JComboBox<>(options);
                comboBox.insertItemAt("", 0);
                comboBox.setSelectedIndex(0);
                comboBox.setEditable(true);

                Object[] message = {
                    label,
                    comboBox
                };

                int result = JOptionPane.showConfirmDialog(
                        this,
                        message,
                        "Scegli il nome del Wallet",
                        JOptionPane.OK_CANCEL_OPTION
                );
                if (result == JOptionPane.OK_OPTION) {
                    if (comboBox.getSelectedItem() != null) {
                        NomeWallet = comboBox.getSelectedItem().toString();
                    }

                }
            }

            //Se alla fine non ho un nome valido torno alla schermata principale
            if (NomeWallet == null || NomeWallet.isBlank()) {
                return;
            }

            File[] files = ScegliFileDaImportare(true);
            if (files == null) {
                return;
            }
            Component c = this;
            boolean SovrascriEsistenti = CheckBox_Sovrascrivi.isSelected();
            //in questo caso siccome cointracking sbaglia molto spesso i prezzi delle shitcoin imposto il prezzo a zero
            //su tutti gli scambi nel caso in cui binance non abbia i prezi corretti
            final boolean PrezzoZeroF = ComboBox_TipoImport.getSelectedItem().toString().trim().equalsIgnoreCase("Transazioni Blockchain");
            final String NomeWalletF = NomeWallet;
            ImportaFileInSequenza(files, false, (file, progressb) -> DocumentiFonte.EseguiImportDaFile(
                    file, DocumentiFonte.TIPO_CSV, "CoinTracking CSV", progressb,
                    () -> Importazioni.Ex_CoinTracking_Importa(file.getAbsolutePath(), SovrascriEsistenti, NomeWalletF, c, PrezzoZeroF, progressb)));
            /* else {

                //QUA Devo gestire il joptionpane che mi avvisa di scegliere un exchange dalla lista
                //Poi devo anche gestire la corretta importazione del nome dell'exchange
                JOptionPane.showInternalConfirmDialog(null, "Attenzione, non è stata fatta nessuna scelta dal menù a tendina",
                            "Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null);
                
                

            }*/

        } else if (voce.isNativo(NAT_TATAX_OLD)) {

            if (ComboBox_TipoImport.getSelectedItem().toString().trim().equalsIgnoreCase("Transazioni Blockchain")) {
                NomeWallet = Text_NomeWallet.getText().trim() + " " + ComboBox_Exchanges.getSelectedItem().toString().trim().substring(ComboBox_Exchanges.getSelectedItem().toString().indexOf("("), ComboBox_Exchanges.getSelectedItem().toString().indexOf(")") + 1);

            } else {
                NomeWallet = ComboBox_Exchanges.getSelectedItem().toString().trim();
            }
            if (NomeWallet.equalsIgnoreCase("*Nome Personalizzato*")) {
                NomeWallet = "";
                Object[] options = Principale.Mappa_Wallet.keySet().toArray();
                JLabel label = new JLabel("<html>Indica o scegli il Nome che vuoi dare al Wallet<br>"
                        + "</html>");
                JComboBox<Object> comboBox = new JComboBox<>(options);
                comboBox.insertItemAt("", 0);
                comboBox.setSelectedIndex(0);
                comboBox.setEditable(true);

                Object[] message = {
                    label,
                    comboBox
                };

                int result = JOptionPane.showConfirmDialog(
                        this,
                        message,
                        "Scegli il nome del Wallet",
                        JOptionPane.OK_CANCEL_OPTION
                );
                if (result == JOptionPane.OK_OPTION) {
                    if (comboBox.getSelectedItem() != null) {
                        NomeWallet = comboBox.getSelectedItem().toString();
                    }

                }
            }

            //Se alla fine non ho un nome valido torno alla schermata principale
            if (NomeWallet == null || NomeWallet.isBlank()) {
                return;
            }

            File[] files = ScegliFileDaImportare(true);
            if (files == null) {
                return;
            }
            Component c = this;
            boolean SovrascriEsistenti = CheckBox_Sovrascrivi.isSelected();
            //in questo caso siccome cointracking sbaglia molto spesso i prezzi delle shitcoin imposto il prezzo a zero
            //su tutti gli scambi nel caso in cui binance non abbia i prezi corretti
            final boolean PrezzoZeroF = ComboBox_TipoImport.getSelectedItem().toString().trim().equalsIgnoreCase("Transazioni Blockchain");
            final String NomeWalletF = NomeWallet;
            ImportaFileInSequenza(files, true, (file, progressb) -> DocumentiFonte.EseguiImportDaFile(
                    file, DocumentiFonte.TIPO_CSV, "Tatax CSV", progressb,
                    () -> Importazioni.Ex_Tatax_Importa(file.getAbsolutePath(), SovrascriEsistenti, NomeWalletF, c, PrezzoZeroF, progressb)));
            /* else {

                //QUA Devo gestire il joptionpane che mi avvisa di scegliere un exchange dalla lista
                //Poi devo anche gestire la corretta importazione del nome dell'exchange
                JOptionPane.showInternalConfirmDialog(null, "Attenzione, non è stata fatta nessuna scelta dal menù a tendina",
                            "Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null);
                
                

            }*/

        } else if (voce.isNativo(NAT_BINANCE_OLD)) {
            File[] files = ScegliFileDaImportare(true);
            if (files == null) {
                return;
            }
            Component c = this;
            boolean SovrascriEsistenti = this.CheckBox_Sovrascrivi.isSelected();
            ImportaFileInSequenza(files, false, (file, progressb) -> DocumentiFonte.EseguiImportDaFile(
                    file, DocumentiFonte.TIPO_CSV, "Binance CSV (formato storico)", progressb,
                    () -> Importazioni.Ex_Binance_Importa(file.getAbsolutePath(), SovrascriEsistenti, c, progressb)));
        } else if (voce.isNativo(NAT_BINANCE_REPORT)) {
            File[] files = ScegliFileDaImportare(true);
            if (files == null) {
                return;
            }
            Component c = this;
            boolean SovrascriEsistenti = this.CheckBox_Sovrascrivi.isSelected();
            ImportaFileInSequenza(files, false, (file, progressb) -> DocumentiFonte.EseguiImportDaFile(
                    file, DocumentiFonte.TIPO_CSV, "Binance Financial Report", progressb,
                    () -> Importazioni.Ex_BinanceTaxReport_Importa(file.getAbsolutePath(), SovrascriEsistenti, c, progressb)));
        } else if (voce.isNativo(NAT_OKX_OLD)) {
            File[] files = ScegliFileDaImportare(true);
            if (files == null) {
                return;
            }
            Component c = this;
            boolean SovrascriEsistenti = this.CheckBox_Sovrascrivi.isSelected();
            ImportaFileInSequenza(files, false, (file, progressb) -> DocumentiFonte.EseguiImportDaFile(
                    file, DocumentiFonte.TIPO_CSV, "OKX CSV (formato storico)", progressb,
                    () -> Importazioni.Ex_OKX_Importa(file.getAbsolutePath(), SovrascriEsistenti, c, progressb)));
        }


    }//GEN-LAST:event_Bottone_SelezionaFileActionPerformed

    private void ComboBox_TipoImportItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_ComboBox_TipoImportItemStateChanged
        // TODO add your handling code here:
        if (ComboBox_TipoImport.getSelectedItem().toString().trim().equalsIgnoreCase("Exchange"))
        {
            Label_TipoImport.setEnabled(true);
            ComboBox_TipoImport.setEnabled(true);
            TextPane_Attenzione.setEnabled(true);
            ComboBox_Exchanges.setEnabled(true);
            Label_NomeExchange.setEnabled(true);
            
            ComboBox_Exchanges.setModel(new DefaultComboBoxModel<>(ordinaVoci(Exchanges)));
            Bottone_SelezionaFile.setEnabled(false);

        }else if (ComboBox_TipoImport.getSelectedItem().toString().trim().equalsIgnoreCase("Wallet"))
        {
            Label_TipoImport.setEnabled(true);
            ComboBox_TipoImport.setEnabled(true);
            TextPane_Attenzione.setEnabled(true);
            ComboBox_Exchanges.setEnabled(true);
            Label_NomeExchange.setEnabled(true);
            ComboBox_Exchanges.setModel(new DefaultComboBoxModel<>(ordinaVoci(Wallets)));
            Bottone_SelezionaFile.setEnabled(false);

        }else if (ComboBox_TipoImport.getSelectedItem().toString().trim().equalsIgnoreCase("Transazioni BlockChain"))
        {
            Label_TipoImport.setEnabled(true);
            ComboBox_TipoImport.setEnabled(true);
            TextPane_Attenzione.setEnabled(true);
            ComboBox_Exchanges.setEnabled(true);
            Label_NomeExchange.setEnabled(true);
            ComboBox_Exchanges.setModel(new DefaultComboBoxModel<>(ordinaVoci(BlockChain)));
            Bottone_SelezionaFile.setEnabled(false);

        }
        else
          {
            Label_NomeExchange.setEnabled(false);
            Label_NomeExchange.setEnabled(false);
            ComboBox_Exchanges.setEnabled(false);
            Text_NomeWallet.setEnabled(false);
            Bottone_SelezionaFile.setEnabled(false);


          }  

    }//GEN-LAST:event_ComboBox_TipoImportItemStateChanged

    private void ComboBox_ExchangesItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_ComboBox_ExchangesItemStateChanged
        // TODO add your handling code here:
        if ((ComboBox_TipoImport.getSelectedItem().toString().trim().equalsIgnoreCase("Exchange")||
                ComboBox_TipoImport.getSelectedItem().toString().trim().equalsIgnoreCase("Wallet"))&&
                !ComboBox_Exchanges.getSelectedItem().toString().trim().equalsIgnoreCase("----------"))
        {
            Bottone_SelezionaFile.setEnabled(true);
            Label_NomeWallet.setEnabled(false);
            Text_NomeWallet.setEnabled(false);

        }else if (ComboBox_TipoImport.getSelectedItem().toString().trim().equalsIgnoreCase("Transazioni BlockChain")&&
                !ComboBox_Exchanges.getSelectedItem().toString().trim().equalsIgnoreCase("----------"))
        {
            Label_NomeWallet.setEnabled(true);
            Text_NomeWallet.setEnabled(true);
            Bottone_SelezionaFile.setEnabled(false);
            if (!this.Text_NomeWallet.getText().trim().equalsIgnoreCase("")) Bottone_SelezionaFile.setEnabled(true);
        
           // System.out.println("ss");

        }
        else
          {
            Bottone_SelezionaFile.setEnabled(false);
            Label_NomeWallet.setEnabled(false);
            Text_NomeWallet.setEnabled(false);
          //  System.out.println("hh");
          }  
    }//GEN-LAST:event_ComboBox_ExchangesItemStateChanged

    private void Text_NomeWalletKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_Text_NomeWalletKeyReleased
        // TODO add your handling code here:
        if (!this.Text_NomeWallet.getText().trim().equalsIgnoreCase(""))
            Bottone_SelezionaFile.setEnabled(true);
    }//GEN-LAST:event_Text_NomeWalletKeyReleased

    private void Bottone_ManualeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Bottone_ManualeActionPerformed
        // TODO add your handling code here:
       
        AppDialog.DialogResult result = AppDialog.builder(this)
                .windowTitle("Scegli le istruzioni da scaricare")
                .bodyTitle("Quali istruzioni scaricare")
                .showTitleInBody(true)
                .theme()
                .type(AppDialog.DialogType.INFO)
                .details("Scegliere quali, tra le istruzioni disponibili, scaricare o vedere : ")
                .action(AppDialog.DialogAction.builder("cancel", "Annulla")
                        .role(AppDialog.ActionRole.SECONDARY)
                        .build())
                .action(AppDialog.DialogAction.builder("generale", "IMPORT CSV")
                        .role(AppDialog.ActionRole.NEUTRAL)
                        .build())
                .action(AppDialog.DialogAction.builder("video", "VIDEO SU IMPORT CSV")
                        .role(AppDialog.ActionRole.NEUTRAL)
                        .build())
                .action(AppDialog.DialogAction.builder("personalizzata", "IMPORTAZIONI PERSONALIZZATE")
                        .role(AppDialog.ActionRole.NEUTRAL)
                        .build())
                .showDialog();

        if (result != null && result.getActionId() != null ){
            
            if (result.getActionId().equals("generale"))
                {DocumentiAiuto.Apri(DocumentiAiuto.EXPORT_IMPORT_CSV);}
            if (result.getActionId().equals("personalizzata"))
                {DocumentiAiuto.Apri(DocumentiAiuto.CREAZIONE_JSON_IMPORTAZIONI);}
            if (result.getActionId().equals("video"))
                {Funzioni.ApriWeb("https://youtu.be/ZwYyV0-LbXk?si=Jb1jfk0ofNazshn3");}
        }
        
    }//GEN-LAST:event_Bottone_ManualeActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Importazioni_Gestione.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Importazioni_Gestione.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Importazioni_Gestione.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Importazioni_Gestione.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Importazioni_Gestione().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Bottone_Annulla;
    private javax.swing.JButton Bottone_Manuale;
    private javax.swing.JButton Bottone_SelezionaFile;
    private javax.swing.JCheckBox CheckBox_Sovrascrivi;
    private javax.swing.JComboBox<String> ComboBox_Exchanges;
    private javax.swing.JComboBox<VoceImport> ComboBox_TipoEstrazione;
    private javax.swing.JComboBox<String> ComboBox_TipoFile;
    private javax.swing.JComboBox<String> ComboBox_TipoImport;
    private javax.swing.JLabel Label_NomeExchange;
    private javax.swing.JLabel Label_NomeWallet;
    private javax.swing.JLabel Label_TipoEstrazione;
    private javax.swing.JLabel Label_TipoFile;
    private javax.swing.JLabel Label_TipoImport;
    private javax.swing.JScrollPane ScrollPane_Attenzione;
    private javax.swing.JTextPane TextPane_Attenzione;
    private javax.swing.JTextField Text_NomeWallet;
    // End of variables declaration//GEN-END:variables
}
