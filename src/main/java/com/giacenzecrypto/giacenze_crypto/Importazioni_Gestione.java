package com.giacenzecrypto.giacenze_crypto;

import java.awt.Component;
import java.awt.Cursor;
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
    "Stellar (XLM)","Ripple (XRP)","Zcash (ZEC)"};
    
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
    static final class VoceImport {

        /** Exchange o fornitore dei dati: è la voce della prima combo e raggruppa le estrazioni */
        final String fornitore;
        /** Nome dell'estrazione dentro il fornitore: è la voce della seconda combo */
        final String estrazione;
        /** Identificativo dell'import nativo, {@code null} per le configurazioni JSON */
        final String idNativo;
        /** File di configurazione JSON, {@code null} per gli import nativi */
        final java.io.File fileJson;

        VoceImport(String fornitore, String estrazione, String idNativo, java.io.File fileJson) {
            this.fornitore = fornitore;
            this.estrazione = estrazione;
            this.idNativo = idNativo;
            this.fileJson = fileJson;
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

            try {
                ImportazioneGenerica.ConfigurazioneImport cfg =
                        ImportazioneGenerica.ConfigurazioneImport.carica(f.getAbsolutePath());

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

            } catch (Exception ex) {
                LoggerGC.ScriviErrore(ex);
            }

            aggiungiEstrazione(new VoceImport(fornitore, estrazione, null, f));
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
        TextPane_Attenzione.setText("<html>\n<head>\n<style>\n  body {\n    font-family: 'Segoe UI', Arial, sans-serif;\n    font-size: 13px;\n    margin: 10px 14px;\n    line-height: 1.6;\n  }\n  p { margin: 5px 0; }\n  b { font-weight: bold; }\n  .mono {\n    font-family: monospace;\n    font-size: 12px;\n    font-weight: bold;\n  }\n  ul {\n    margin: 6px 0 4px 18px;\n    padding: 0;\n  }\n  li { margin-bottom: 3px; }\n</style>\n</head>\n<body>\n\n<p><b>&#9888; Attenzione <br>\nImportazione da cointracking.info o Tatax</b></p>\n\n<p>Per una corretta importazione &egrave; necessario:</p>\n\n<ul>\n  <li>Importare i dati <b>un solo exchange / wallet per volta</b></li>\n  <li>Impostare il <b>nome dell&#39;exchange o wallet</b> nel campo sottostante</li>\n</ul>\n\n<p><i>Esempio:</i> &nbsp;<span class=\"mono\">Binance</span></p>\n\n</body>\n</html>\n");
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

    Component c = this;
    Download progressb = new Download();
    Bottone_SelezionaFile.setEnabled(false);
    Bottone_Annulla.setEnabled(false);

    String Directory = DatabaseH2.Pers_Opzioni_Leggi("Directory_Importazioni_Gestione");
    JFileChooser fc = new JFileChooser(Directory);
    int returnVal = fc.showOpenDialog(c);

    //Se l'utente annulla la scelta del file getSelectedFile() è null: senza questa uscita anticipata
    //la lettura del nome qui sotto sollevava una NullPointerException e i due pulsanti restavano
    //disabilitati, lasciando la finestra inutilizzabile
    if (returnVal != JFileChooser.APPROVE_OPTION) {
        Bottone_SelezionaFile.setEnabled(true);
        Bottone_Annulla.setEnabled(true);
        progressb.dispose();
        return;
    }

    // Risolvo il fuso orario se non specificato nel JSON
    boolean PrioritaNomeFile=false;
    //Se c'e il ? affianco al fuso allora do priorità al fuso sul file invece che quello scritto
    if (fusoFinale[0].contains("?")){
        PrioritaNomeFile=true;
        fusoFinale[0]=fusoFinale[0].replace("?", "");
    }
    String nomeFile = fc.getSelectedFile().getName();
    String fusoFile = ImportazioneGenerica.estraiTZdaNomeFile(nomeFile);
    if (PrioritaNomeFile&&fusoFile != null && !fusoFile.isBlank()){
        fusoFinale[0] = fusoFile;
    }
    if (returnVal == JFileChooser.APPROVE_OPTION && fusoFinale[0].isBlank()) {
        
        if (fusoFile != null && !fusoFile.isBlank()) {
            fusoFinale[0] = fusoFile;
        } else {
            String fusoScelto = AppDialog.showComboBoxDialog(
                    this,
                    "Fuso orario non specificato",
                    "Seleziona il fuso orario",
                    "Il fuso orario non è specificato nella configurazione né nel nome del file.\n\n"
                    + "Selezionare il fuso orario corretto per i dati da importare:",
                    "Fuso orario:",
                    "UTC", "UTC+1", "UTC+2", "CET", "Europe/Rome"
            );
            if (fusoScelto == null) {
                Bottone_SelezionaFile.setEnabled(true);
                Bottone_Annulla.setEnabled(true);
                return;
            }
            fusoFinale[0] = fusoScelto;
        }
    }

    final boolean SovrascriEsistenti = this.CheckBox_Sovrascrivi.isSelected();

    Thread thread = new Thread() {
        /** Esegue in background l'import generico configurato dal JSON selezionato. */
        @Override
        public void run() {
            try {
                if (returnVal == JFileChooser.APPROVE_OPTION) {

                    String FileDaImportare = fc.getSelectedFile().getAbsolutePath();
                    DatabaseH2.Pers_Opzioni_Scrivi(
                            "Directory_Importazioni_Gestione",
                            fc.getSelectedFile().getParent()
                    );

                    c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    Importazioni.AzzeraContatori();

                    boolean ok = ImportazioneGenerica.importa(
                            FileDaImportare,
                            percorsoJson,
                            SovrascriEsistenti,
                            progressb,
                            nomeExchangeFinale[0],
                            fusoFinale[0]
                    );

                    if (ok && Importazioni.TransazioniAggiunte > 0) {
                        Principale.TabellaCryptodaAggiornare = true;
                    }

                    Importazioni_Resoconto res = new Importazioni_Resoconto();
                    c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    res.ImpostaValori(
                            Importazioni.Transazioni,
                            Importazioni.TransazioniAggiunte,
                            Importazioni.TrasazioniScartate,
                            Importazioni.TrasazioniSconosciute,
                            Importazioni.movimentiSconosciuti
                    );
                    res.setLocationRelativeTo(c);
                    res.setVisible(true);
                    dispose();
                }

            } catch (Exception ex) {
                LoggerGC.ScriviErrore(ex);

            } finally {
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
        
        else if (voce.isNativo(NAT_CDC_APP)) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            String Directory = DatabaseH2.Pers_Opzioni_Leggi("Directory_ImportazioniGestione");
            JFileChooser fc = new JFileChooser(Directory);
            int returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                //   selezioneok[0]=true;
                String FileDaImportare = fc.getSelectedFile().getAbsolutePath();
                DatabaseH2.Pers_Opzioni_Scrivi("Directory_ImportazioniGestione", fc.getSelectedFile().getParent());
                //System.out.println(Directory);
                boolean SovrascriEsistenti = this.CheckBox_Sovrascrivi.isSelected();
                Importazioni.AzzeraContatori();
                Importazioni.Ex_CDCAPP_Importa(FileDaImportare, SovrascriEsistenti);
                Importazioni_Resoconto res = new Importazioni_Resoconto();
                res.ImpostaValori(Importazioni.Transazioni, Importazioni.TransazioniAggiunte, Importazioni.TrasazioniScartate, Importazioni.TrasazioniSconosciute, Importazioni.movimentiSconosciuti);
                res.setLocationRelativeTo(this);
                res.setVisible(true);
                dispose();
            }
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } else if (voce.isNativo(NAT_CDC_EXCHANGE)) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            String Directory = DatabaseH2.Pers_Opzioni_Leggi("Directory_ImportazioniGestione");
            JFileChooser fc = new JFileChooser(Directory);
            int returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                //   selezioneok[0]=true;
                Component c = this;
                Download progressb = new Download();
                Bottone_SelezionaFile.setEnabled(false);
                Bottone_Annulla.setEnabled(false);
                Importazioni.AzzeraContatori();

                Thread thread;
                thread = new Thread() {
                    /** Esegue in background l'import di un CSV Crypto.com Exchange. */
                    public void run() {
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            String FileDaImportare = fc.getSelectedFile().getAbsolutePath();
                            DatabaseH2.Pers_Opzioni_Scrivi("Directory_ImportazioniGestione", fc.getSelectedFile().getParent());
                            boolean SovrascriEsistenti = CheckBox_Sovrascrivi.isSelected();
                            Importazioni.AzzeraContatori();
                            boolean PrezzoZero = false;
                            if (ComboBox_TipoImport.getSelectedItem().toString().trim().equalsIgnoreCase("Transazioni Blockchain")) {
                                //in questo caso siccome cointracking sbaglia molto spesso i prezzi delle shitcoin imposto il prezzo a zero
                                //su tutti gli scambi nel caso in cui binance non abbia i prezi corretti
                                PrezzoZero = true;

                            }
                            Importazioni.Ex_CryptoComExchange_Importa(FileDaImportare, SovrascriEsistenti, c, PrezzoZero, progressb);
                            Importazioni_Resoconto res = new Importazioni_Resoconto();
                            res.ImpostaValori(Importazioni.Transazioni, Importazioni.TransazioniAggiunte, Importazioni.TrasazioniScartate, Importazioni.TrasazioniSconosciute, Importazioni.movimentiSconosciuti);
                            res.setLocationRelativeTo(c);
                            res.setVisible(true);
                            progressb.RipristinaStdout();
                            dispose();
                        }
                        Bottone_SelezionaFile.setEnabled(true);
                        Bottone_Annulla.setEnabled(true);
                        progressb.dispose();

                    }

                };
                progressb.SetThread(thread);
                thread.start();
                progressb.setDefaultCloseOperation(0);
                progressb.setLocationRelativeTo(this);
                progressb.setVisible(true);
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

            Component c = this;
            Download progressb = new Download();
            Bottone_SelezionaFile.setEnabled(false);
            Bottone_Annulla.setEnabled(false);
            String Directory = DatabaseH2.Pers_Opzioni_Leggi("Directory_ImportazioniGestione");
            JFileChooser fc = new JFileChooser(Directory);
            int returnVal = fc.showOpenDialog(c);

            Thread thread;
            thread = new Thread() {
                /** Esegue in background l'import di un CSV CoinTracking. */
                public void run() {
                    //  try {

                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        //     selezioneok[0] = true;
                        String FileDaImportare = fc.getSelectedFile().getAbsolutePath();
                        DatabaseH2.Pers_Opzioni_Scrivi("Directory_ImportazioniGestione", fc.getSelectedFile().getParent());
                        boolean SovrascriEsistenti = CheckBox_Sovrascrivi.isSelected();
                        Importazioni.AzzeraContatori();
                        boolean PrezzoZero = false;
                        if (ComboBox_TipoImport.getSelectedItem().toString().trim().equalsIgnoreCase("Transazioni Blockchain")) {
                            // nomewallet = Text_NomeWallet.getText().trim() + " " + ComboBox_Exchanges.getSelectedItem().toString().trim().substring(ComboBox_Exchanges.getSelectedItem().toString().indexOf("("), ComboBox_Exchanges.getSelectedItem().toString().indexOf(")") + 1);
                            //in questo caso siccome cointracking sbaglia molto spesso i prezzi delle shitcoin imposto il prezzo a zero
                            //su tutti gli scambi nel caso in cui binance non abbia i prezi corretti
                            PrezzoZero = true;

                        }
                        Importazioni.Ex_CoinTracking_Importa(FileDaImportare, SovrascriEsistenti, NomeWallet, c, PrezzoZero, progressb);

                        Importazioni_Resoconto res = new Importazioni_Resoconto();
                        res.ImpostaValori(Importazioni.Transazioni, Importazioni.TransazioniAggiunte, Importazioni.TrasazioniScartate, Importazioni.TrasazioniSconosciute, Importazioni.movimentiSconosciuti);
                        res.setLocationRelativeTo(c);
                        res.setVisible(true);
                        dispose();

                    }
                    Bottone_SelezionaFile.setEnabled(true);
                    Bottone_Annulla.setEnabled(true);
                    progressb.dispose();

                }

            };
            progressb.SetThread(thread);
            thread.start();
            progressb.setDefaultCloseOperation(0);
            progressb.setLocationRelativeTo(this);
            progressb.setVisible(true);
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

            Component c = this;
            Download progressb = new Download();
            Bottone_SelezionaFile.setEnabled(false);
            Bottone_Annulla.setEnabled(false);
            String Directory = DatabaseH2.Pers_Opzioni_Leggi("Directory_ImportazioniGestione");
            JFileChooser fc = new JFileChooser(Directory);
            int returnVal = fc.showOpenDialog(c);

            Thread thread;
            thread = new Thread() {
                /** Esegue in background l'import di un CSV Tatax. */
                public void run() {
                    if (returnVal == JFileChooser.APPROVE_OPTION) {

                        String FileDaImportare = fc.getSelectedFile().getAbsolutePath();
                        DatabaseH2.Pers_Opzioni_Scrivi("Directory_ImportazioniGestione", fc.getSelectedFile().getParent());
                        boolean SovrascriEsistenti = CheckBox_Sovrascrivi.isSelected();
                        Importazioni.AzzeraContatori();

                        boolean PrezzoZero = false;
                        if (ComboBox_TipoImport.getSelectedItem().toString().trim().equalsIgnoreCase("Transazioni Blockchain")) {
                            //in questo caso siccome cointracking sbaglia molto spesso i prezzi delle shitcoin imposto il prezzo a zero
                            //su tutti gli scambi nel caso in cui binance non abbia i prezi corretti
                            PrezzoZero = true;

                        }

                        Importazioni.Ex_Tatax_Importa(FileDaImportare, SovrascriEsistenti, NomeWallet, c, PrezzoZero, progressb);

                        Importazioni_Resoconto res = new Importazioni_Resoconto();
                        res.ImpostaValori(Importazioni.Transazioni, Importazioni.TransazioniAggiunte, Importazioni.TrasazioniScartate, Importazioni.TrasazioniSconosciute, Importazioni.movimentiSconosciuti);
                        res.setLocationRelativeTo(c);
                        res.setVisible(true);

                        progressb.RipristinaStdout();
                        dispose();

                    }
                    Bottone_SelezionaFile.setEnabled(true);
                    Bottone_Annulla.setEnabled(true);
                    progressb.dispose();

                }

            };
            progressb.SetThread(thread);
            thread.start();
            progressb.setDefaultCloseOperation(0);
            progressb.setLocationRelativeTo(this);
            progressb.setVisible(true);
            /* else {

                //QUA Devo gestire il joptionpane che mi avvisa di scegliere un exchange dalla lista
                //Poi devo anche gestire la corretta importazione del nome dell'exchange
                JOptionPane.showInternalConfirmDialog(null, "Attenzione, non è stata fatta nessuna scelta dal menù a tendina",
                            "Attenzione",JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null);
                
                

            }*/

        } else if (voce.isNativo(NAT_BINANCE_OLD)) {
            Component c = this;
            Download progressb = new Download();
            Bottone_SelezionaFile.setEnabled(false);
            Bottone_Annulla.setEnabled(false);
            String Directory = DatabaseH2.Pers_Opzioni_Leggi("Directory_ImportazioniGestione");
            JFileChooser fc = new JFileChooser(Directory);
            int returnVal = fc.showOpenDialog(c);
            boolean SovrascriEsistenti = this.CheckBox_Sovrascrivi.isSelected();
            Thread thread;
            thread = new Thread() {
                /** Esegue in background l'import di un CSV Binance. */
                public void run() {

                    // JFileChooser fc = new JFileChooser();
                    // int returnVal = fc.showOpenDialog(this);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        //  selezioneok[0] = true;
                        String FileDaImportare = fc.getSelectedFile().getAbsolutePath();
                        DatabaseH2.Pers_Opzioni_Scrivi("Directory_ImportazioniGestione", fc.getSelectedFile().getParent());
                        c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        Importazioni.AzzeraContatori();
                        Importazioni.Ex_Binance_Importa(FileDaImportare, SovrascriEsistenti, c, progressb);
                        Importazioni_Resoconto res = new Importazioni_Resoconto();
                        c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        res.ImpostaValori(Importazioni.Transazioni, Importazioni.TransazioniAggiunte, Importazioni.TrasazioniScartate, Importazioni.TrasazioniSconosciute, Importazioni.movimentiSconosciuti);
                        res.setLocationRelativeTo(c);
                        res.setVisible(true);
                        //  if (selezioneok[0]) {
                        dispose();
                        // }

                    }
                    Bottone_SelezionaFile.setEnabled(true);
                    Bottone_Annulla.setEnabled(true);
                    progressb.dispose();

                }

            };
            thread.start();
            progressb.setDefaultCloseOperation(0);
            progressb.setLocationRelativeTo(this);
            progressb.setVisible(true);
        } else if (voce.isNativo(NAT_BINANCE_REPORT)) {
            Component c = this;
            Download progressb = new Download();
            Bottone_SelezionaFile.setEnabled(false);
            Bottone_Annulla.setEnabled(false);
            String Directory = DatabaseH2.Pers_Opzioni_Leggi("Directory_ImportazioniGestione");
            JFileChooser fc = new JFileChooser(Directory);
            int returnVal = fc.showOpenDialog(c);
            boolean SovrascriEsistenti = this.CheckBox_Sovrascrivi.isSelected();
            Thread thread;
            thread = new Thread() {
                /** Esegue in background l'import di un Binance Tax Report. */
                public void run() {

                    // JFileChooser fc = new JFileChooser();
                    // int returnVal = fc.showOpenDialog(this);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        //  selezioneok[0] = true;
                        String FileDaImportare = fc.getSelectedFile().getAbsolutePath();
                        DatabaseH2.Pers_Opzioni_Scrivi("Directory_ImportazioniGestione", fc.getSelectedFile().getParent());
                        c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        Importazioni.AzzeraContatori();
                        Importazioni.Ex_BinanceTaxReport_Importa(FileDaImportare, SovrascriEsistenti, c, progressb);
                        Importazioni_Resoconto res = new Importazioni_Resoconto();
                        c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        res.ImpostaValori(Importazioni.Transazioni, Importazioni.TransazioniAggiunte, Importazioni.TrasazioniScartate, Importazioni.TrasazioniSconosciute, Importazioni.movimentiSconosciuti);
                        res.setLocationRelativeTo(c);
                        res.setVisible(true);
                        //  if (selezioneok[0]) {
                        dispose();
                        // }

                    }
                    Bottone_SelezionaFile.setEnabled(true);
                    Bottone_Annulla.setEnabled(true);
                    progressb.dispose();

                }

            };
            thread.start();
            progressb.setDefaultCloseOperation(0);
            progressb.setLocationRelativeTo(this);
            progressb.setVisible(true);
        } else if (voce.isNativo(NAT_OKX_OLD)) {
            Component c = this;
            Download progressb = new Download();
            Bottone_SelezionaFile.setEnabled(false);
            Bottone_Annulla.setEnabled(false);
            String Directory = DatabaseH2.Pers_Opzioni_Leggi("Directory_ImportazioniGestione");
            JFileChooser fc = new JFileChooser(Directory);
            int returnVal = fc.showOpenDialog(c);
            boolean SovrascriEsistenti = this.CheckBox_Sovrascrivi.isSelected();
            Thread thread;
            thread = new Thread() {
                /** Esegue in background l'import di un CSV OKX. */
                public void run() {

                    // JFileChooser fc = new JFileChooser();
                    // int returnVal = fc.showOpenDialog(this);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        //  selezioneok[0] = true;
                        String FileDaImportare = fc.getSelectedFile().getAbsolutePath();
                        DatabaseH2.Pers_Opzioni_Scrivi("Directory_ImportazioniGestione", fc.getSelectedFile().getParent());
                        c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        Importazioni.AzzeraContatori();
                        Importazioni.Ex_OKX_Importa(FileDaImportare, SovrascriEsistenti, c, progressb);
                        Importazioni_Resoconto res = new Importazioni_Resoconto();
                        c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        res.ImpostaValori(Importazioni.Transazioni, Importazioni.TransazioniAggiunte, Importazioni.TrasazioniScartate, Importazioni.TrasazioniSconosciute, Importazioni.movimentiSconosciuti);
                        res.setLocationRelativeTo(c);
                        res.setVisible(true);
                        //  if (selezioneok[0]) {
                        dispose();
                        // }

                    }
                    Bottone_SelezionaFile.setEnabled(true);
                    Bottone_Annulla.setEnabled(true);
                    progressb.dispose();

                }

            };
            thread.start();
            progressb.setDefaultCloseOperation(0);
            progressb.setLocationRelativeTo(this);
            progressb.setVisible(true);
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
                {Funzioni.ApriWeb("https://sourceforge.net/projects/giacenze-crypto-com/files/Documentazione/ExportImportCSV.pdf/download");}
            if (result.getActionId().equals("personalizzata"))
                {Funzioni.ApriWeb("https://sourceforge.net/projects/giacenze-crypto-com/files/Documentazione/CreazioneJSONperImportazioniPersonalizzate.pdf/download");}
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
