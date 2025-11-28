/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.giacenzecrypto.giacenze_crypto;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author luca.passelli
 */
/**
 * This class manages the connection to an H2 database.  It provides methods for creating tables, inserting, updating, and retrieving data.  It uses two database connections, one for general data and another for personalized data.
 */
public class DatabaseH2 {

    static String jdbcUrl = Statiche.getDBPrincipale();
    static String jdbcUrl2 = Statiche.getDBPersonale();
    static String jdbcPrezzi = Statiche.getDBPrezzi();
    static String usernameH2 = "sa";
    static String passwordH2 = "";
    static Connection connection;
    static Connection connectionPersonale;
    static Connection connectionPrezzi;
    static Map<String, String> Mappa_Wallet_Gruppo = new TreeMap<>();//memorizzo anche qua l'associazione dei gruppi per rendere più veloce la ricerca

    //per compattare database comando -> SHUTDOWN COMPACT //da valutare quando farlo
    public static boolean CreaoCollegaDatabase() {
        boolean successo=false;
        try {
            connection = DriverManager.getConnection(jdbcUrl, usernameH2, passwordH2);
            connectionPersonale = DriverManager.getConnection(jdbcUrl2, usernameH2, passwordH2);
            connectionPrezzi = DriverManager.getConnection(jdbcPrezzi, usernameH2, passwordH2);
            // Creazione delle tabelle se non esistono
        /*    String createTableSQL = "CREATE TABLE IF NOT EXISTS Address_Senza_Prezzo  (address_chain VARCHAR(255) PRIMARY KEY, data VARCHAR(255))";
            PreparedStatement preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute();*/
        

            connectionPrezzi.createStatement().execute("CREATE TABLE IF NOT EXISTS PrezziNew (" +
                        "timestamp BIGINT NOT NULL, " +
                        "exchange VARCHAR(100) NOT NULL, " +
                        "symbol VARCHAR(100) NOT NULL, " +
                        "rete VARCHAR(100) NOT NULL, " +
                        "address VARCHAR(255) NOT NULL, " +
                        "prezzo DOUBLE, " +
                        "PRIMARY KEY (timestamp, exchange, symbol, rete, address)" +
                        ")");
            
            connectionPersonale.createStatement().execute("CREATE TABLE IF NOT EXISTS PrezziNew (" +
                        "timestamp BIGINT NOT NULL, " +
                        "exchange VARCHAR(100) NOT NULL, " +
                        "symbol VARCHAR(100) NOT NULL, " +
                        "rete VARCHAR(100) NOT NULL, " +
                        "address VARCHAR(255) NOT NULL, " +
                        "prezzo DOUBLE, " +
                        "PRIMARY KEY (timestamp, exchange, symbol, rete, address)" +
                        ")");
        
            String createTableSQL = "CREATE TABLE IF NOT EXISTS Prezzo_ora_Address_Chain  (ora_address_chain VARCHAR(255) PRIMARY KEY, prezzo VARCHAR(255))";
            PreparedStatement preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute();
            preparedStatement = connectionPersonale.prepareStatement(createTableSQL);
            preparedStatement.execute();

         /*   createTableSQL = "CREATE TABLE IF NOT EXISTS USDTEUR  (data VARCHAR(255) PRIMARY KEY, prezzo VARCHAR(255))";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute();*/
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS XXXEUR  (dataSimbolo VARCHAR(255) PRIMARY KEY, prezzo VARCHAR(255))";
            preparedStatement = connection.prepareStatement(createTableSQL);           
            preparedStatement.execute(); 
            preparedStatement = connectionPersonale.prepareStatement(createTableSQL);
            preparedStatement.execute(); 
            
             
            createTableSQL = "CREATE TABLE IF NOT EXISTS GESTITIBINANCE  (Coppia VARCHAR(255) PRIMARY KEY)";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute(); 
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS GESTITICOINBASE  (Simbolo VARCHAR(255) PRIMARY KEY)";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute(); 
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS GESTITICOINGECKO  (Address_Chain VARCHAR(255) PRIMARY KEY, Simbolo VARCHAR(255), Nome VARCHAR (255))";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute(); 
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS TOKENSOLANA  (Address VARCHAR(255) PRIMARY KEY, Simbolo VARCHAR(255), Nome VARCHAR (255), Tipo VARCHAR (255))";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute();
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS GESTITICRYPTOHISTORY  (Symbol VARCHAR(255) PRIMARY KEY, Nome VARCHAR(255))";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute();
                       
            createTableSQL = "CREATE TABLE IF NOT EXISTS OPZIONI (Opzione VARCHAR(255) PRIMARY KEY, Valore VARCHAR(1000))";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute();  
            aggiornaDimensioneColonnaValoreH2Opzioni(connection);
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS GIACENZEBLOCKCHAIN (Wallet_Blocco VARCHAR(255) PRIMARY KEY, Valore VARCHAR(255))";
            preparedStatement = connectionPersonale.prepareStatement(createTableSQL);
            preparedStatement.execute();
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS WALLETS (Wallet_Rete VARCHAR(255) PRIMARY KEY, Wallet VARCHAR(255), Rete VARCHAR(255))";
            preparedStatement = connectionPersonale.prepareStatement(createTableSQL);
            preparedStatement.execute();
            
            //Questa tabella è stata creata principalmente per gestire i token di binance che vengono usati nei trades
            //Serve per poter aggiungere manualmente una lista di tokens di cui verranno richiesti i trades
            createTableSQL = "CREATE TABLE IF NOT EXISTS EXCHANGETOKENS (Exchange_Token VARCHAR(255) PRIMARY KEY, Exchange VARCHAR(255), Token VARCHAR(255))";
            preparedStatement = connectionPersonale.prepareStatement(createTableSQL);
            preparedStatement.execute();
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS RINOMINATOKEN (address_chain VARCHAR(255) PRIMARY KEY, VecchioNome VARCHAR(255), NuovoNome VARCHAR(255))";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute(); 
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS EXCHANGEAPI (Nome VARCHAR(255) PRIMARY KEY, Exchange VARCHAR(255), Chiave VARCHAR(255), Segreto VARCHAR(255),Opzionale VARCHAR(255))";
            preparedStatement = connectionPersonale.prepareStatement(createTableSQL);
            preparedStatement.execute(); 
            
            //Tabella che associa i Wallet ad un Gruppo per poter poi gestire correttamente i quadri RW
            createTableSQL = "CREATE TABLE IF NOT EXISTS WALLETGRUPPO  (Wallet VARCHAR(255) PRIMARY KEY, Gruppo VARCHAR(255))";
            preparedStatement = connectionPersonale.prepareStatement(createTableSQL);
            preparedStatement.execute();
            
            //Tabella Gruppi Wallet Fatta così NomeGruppoOriginale,Alias,PagaBollo(Valorizzato S o N)
            //NomeGruppoOriginale è il Campo Univoco e sarà Wallet 01, Wallet 02, Wallet 03 etc....
            createTableSQL = "CREATE TABLE IF NOT EXISTS GRUPPO_ALIAS  (Gruppo VARCHAR(255) PRIMARY KEY, Alias VARCHAR(255), PagaBollo VARCHAR(25))";
            preparedStatement = connectionPersonale.prepareStatement(createTableSQL);
            preparedStatement.execute();
            if (Pers_GruppoAlias_Leggi("Wallet 01")[0] == null) {
                for (int i = 1; i < 21; i++) {
                    String gruppo;
                    if (i < 10) {
                        gruppo = "Wallet 0" + String.valueOf(i);
                    } else {
                        gruppo = "Wallet " + String.valueOf(i);
                    }
                    //Questa cosa sotto la devo fare solo se non esiste
                    Pers_GruppoAlias_Scrivi(gruppo, gruppo, false);
                }
            }
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS EMONEY  (Moneta VARCHAR(255) PRIMARY KEY, Data VARCHAR(255))";
            preparedStatement = connectionPersonale.prepareStatement(createTableSQL);
            preparedStatement.execute();
            
            createTableSQL = "CREATE TABLE IF NOT EXISTS OPZIONI (Opzione VARCHAR(255) PRIMARY KEY, Valore VARCHAR(255))";
            preparedStatement = connectionPersonale.prepareStatement(createTableSQL);
            preparedStatement.execute(); 
            
            successo=true;
            

            //DROP TABLE IF EXISTS " + tableName;
            /*  String insertSQL = "INSERT INTO AddressSenzaPrezzo (address_chain, data) VALUES (?, ?)";
            preparedStatement = connection.prepareStatement(insertSQL);
                preparedStatement.setString(1, "Mario");
                preparedStatement.setString(2, "Pippo");
                preparedStatement.executeUpdate();*/
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
            
        }
        return successo;
    }
    
    
    
    
    public static void aggiornaDimensioneColonnaValoreH2Opzioni(Connection connection) {
    try (Statement stmt = connection.createStatement()) {
        // 1. Controlla la dimensione corrente della colonna
        ResultSet rs = stmt.executeQuery(
            "SELECT COLUMN_NAME, CHARACTER_MAXIMUM_LENGTH " +
            "FROM INFORMATION_SCHEMA.COLUMNS " +
            "WHERE TABLE_NAME = 'OPZIONI' AND COLUMN_NAME = 'VALORE'"
        );
        
        if (rs.next()) {
            int lunghezzaCorrente = rs.getInt("CHARACTER_MAXIMUM_LENGTH");
            //System.out.println("Dimensione corrente colonna VALORE: " + lunghezzaCorrente);
            
            if (lunghezzaCorrente < 1000) {
                // 2. Aggiorna a 1000
                stmt.execute("ALTER TABLE OPZIONI ALTER COLUMN VALORE VARCHAR(1000)");
                System.out.println("Colonna VALORE aggiornata a 1000 caratteri");
            } else {
               // System.out.println("Valore colonna già aggiornata");
            }
        } else {
            System.out.println("Tabella OPZIONI non trovata o colonna VALORE mancante");
        }
    } catch (SQLException e) {
        System.err.println("Errore aggiornamento dimensione colonna: " + e.getMessage());
        LoggerGC.ScriviErrore(e);
    }
}

    
    
    /**
     * Devo predisporre la cancellazione dei record dei prezzi nulli all'apertura del gestionale
     * Cancella le righe senza prezzo o nulle dal database
     */
    public static void CancellaPrezziVuoti() {
        try {
            String SQL = "DELETE FROM XXXEUR WHERE PREZZO='ND'";
            try (PreparedStatement checkStatement = connection.prepareStatement(SQL)) {
                checkStatement.executeUpdate();
            }
            SQL = "DELETE FROM XXXEUR WHERE PREZZO='null'";
            try (PreparedStatement checkStatement = connection.prepareStatement(SQL)) {
                checkStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }
    
public static void InserisciPrezzoPresonalizzato(long Timestamp, String Fonte, String Moneta, String VU, String Rete, String Address, String Gruppo,long timestampDaCancellare) {
    try {
        Double ValoreUnitario = Double.valueOf(VU);
        //System.out.println(Timestamp);
        //System.out.println(timestampDaCancellare);
        //System.out.println(Moneta+"-"+Timestamp+"-"+Rete+"-"+Address+"-"+Rete+"-"+Gruppo);
        // Controllo validity address
        if (!isEmpty(Address) && !Funzioni_WalletDeFi.isValidAddress(Address, Rete)) {
            Address = "";
        }

        if (Rete==null||Principale.Mappa_ChainExplorer.get(Rete) == null) Rete = "";

        // Fonte -> rimuovo parte dopo "("
        if (Fonte.contains("(")) Fonte = Fonte.split("\\(")[0].trim();
        if (isEmpty(Gruppo)) Gruppo = "TUTTI";
        Fonte = Fonte + " (" + Gruppo + ")";

        // --- Determino modalità ---
        boolean perNome = (isEmpty(Address) || isEmpty(Rete)) && !isEmpty(Moneta);
        boolean perAddress = (!isEmpty(Address) || !isEmpty(Rete));
        
        //CANCELLO i vecchi valori se esistono nel vecchio database XXXEUR     
        String cancellaXXXEUR = """
            DELETE FROM XXXEUR
            WHERE dataSimbolo = ?
        """;
        String Datasimbolo=FunzioniDate.ConvertiDatadaLongallOra(Timestamp) + " " + Moneta;
        try (PreparedStatement del = DatabaseH2.connectionPersonale.prepareStatement(cancellaXXXEUR)) {
            del.setString(1, Datasimbolo);
            del.executeUpdate();
        }
        //Adesso cancello i vecchi valori personalizzati per address
        String cancellaAddressEUR = """
            DELETE FROM Prezzo_ora_Address_Chain
            WHERE ora_address_chain = ?
        """;
        Datasimbolo=FunzioniDate.ConvertiDatadaLongallOra(Timestamp) + "_" + Address+"_"+Rete;
        try (PreparedStatement del = DatabaseH2.connectionPersonale.prepareStatement(cancellaAddressEUR)) {
            del.setString(1, Datasimbolo);
            del.executeUpdate();
        }

        //timestampDaCancellare
       /* try (PreparedStatement del = DatabaseH2.connection.prepareStatement(cancellaXXXEUR)) {
            del.setString(1, Datasimbolo);
            del.executeUpdate();
        }*/

        // --- 1) DELETE esistente ---
        String deleteSql;
        if (perNome) {
            deleteSql = """
                DELETE FROM PrezziNew 
                WHERE timestamp = ?
                  AND exchange ILIKE ?
                  AND symbol = ?
                  AND rete = ''
                  AND address = ''
                """;
        } else if (perAddress) {
            deleteSql = """
                DELETE FROM PrezziNew
                WHERE timestamp = ?
                  AND exchange ILIKE ?
                  AND symbol = ''
                  AND rete = ?
                  AND address = ?
                """;
        } else {
            return;
        }

        try (PreparedStatement del = DatabaseH2.connectionPersonale.prepareStatement(deleteSql)) {
            del.setLong(1, Timestamp);
            del.setString(2, "%" + Gruppo + "%");
            if (perNome) {
                del.setString(3, Moneta);
            } else {
                del.setString(3, Rete);
                del.setString(4, Address);
            }
            del.executeUpdate();
        }
        try (PreparedStatement del = DatabaseH2.connectionPersonale.prepareStatement(deleteSql)) {
            del.setLong(1, timestampDaCancellare);
            del.setString(2, "%" + Gruppo + "%");
            if (perNome) {
                del.setString(3, Moneta);
            } else {
                del.setString(3, Rete);
                del.setString(4, Address);
            }
            del.executeUpdate();
        }

        // --- 2) MERGE nuovo valore ---
        String mergeSql = """
            MERGE INTO PrezziNew (timestamp, exchange, symbol, prezzo, rete, address)
            KEY (timestamp, exchange, symbol, rete, address)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = DatabaseH2.connectionPersonale.prepareStatement(mergeSql)) {

            if (perNome) {
                ps.setLong(1, Timestamp);
                ps.setString(2, Fonte);
                ps.setString(3, Moneta);
                ps.setDouble(4, ValoreUnitario);
                ps.setString(5, "");
                ps.setString(6, "");
            } else {
                ps.setLong(1, Timestamp);
                ps.setString(2, Fonte);
                ps.setString(3, "");
                ps.setDouble(4, ValoreUnitario);
                ps.setString(5, Rete);
                ps.setString(6, Address);
            }

            //System.out.println("InserisciPrezzoPresonalizzato : "+ps.toString());
            ps.executeUpdate();
        }

    } catch (NumberFormatException | SQLException ex) {
        LoggerGC.ScriviErrore(ex);
    }
}

    private static boolean isEmpty(String s) {
    return s == null || s.isBlank();
}

    
    /**
 * Gestisce l'inserimento o l'aggiornamento di dati nella tabella EMONEY.
 * Se una voce con la stessa moneta esiste già, viene aggiornata la data; altrimenti, viene creata una nuova voce.
 * La funzione aggiorna anche una mappa di riferimento in memoria (CDC_Grafica.Mappa_EMoney).  
 * L'utilizzo di questa mappa è finalizzato ad accelerare le ricerche successive, a scapito di una potenziale incoerenza tra mappa e database in caso di problemi.  È necessario garantire la coerenza tra la mappa e il database tramite meccanismi di sincronizzazione appropriati, se si desidera una maggiore robustezza del sistema.
 *
 * @param Moneta La valuta (es. "EUR", "USD").  Non deve essere nullo o vuoto.
 * @param Data La data nel formato appropriato per il database. Non deve essere nullo.
 * //@throws SQLException Se si verifica un errore durante l'interazione con il database.
 * @throws IllegalArgumentException Se `Moneta` è nullo o vuoto, o se `Data` è nullo.
 */
       public static void Pers_Emoney_Scrivi(String Moneta, String Data) {
        // Validazione degli input
        if (Moneta == null || Moneta.isEmpty()) {
            throw new IllegalArgumentException("Moneta non può essere nullo o vuoto.");
        }
        if (Data == null) {
            throw new IllegalArgumentException("Data non può essere nullo.");
        }
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM EMONEY WHERE Moneta = ?";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            checkStatement.setString(1, Moneta);

            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE EMONEY SET Data = ? WHERE Moneta = ?";
                PreparedStatement updateStatement = connectionPersonale.prepareStatement(updateSQL);
                updateStatement.setString(1, Data);
                updateStatement.setString(2, Moneta);
                updateStatement.executeUpdate();

            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO EMONEY (Moneta, Data) VALUES (?, ?)";
                PreparedStatement insertStatement = connectionPersonale.prepareStatement(insertSQL);
                insertStatement.setString(1, Moneta);
                insertStatement.setString(2, Data);
                insertStatement.executeUpdate();

            }
            //Se aggiungo una riga al DB la aggiungo anche alla mappa di riferimento
            //Lavorare con le mappe risulta infatti + veloce del DB e uso quella come base per le ricerche
            Principale.Mappa_EMoney.put(Moneta, Data);
        } catch (SQLException ex) {
        LoggerGC.ScriviErrore(ex);
        throw new RuntimeException("Errore durante l'accesso al database: " + ex.getMessage(), ex);
    }
    }
        
        /**
         * Cancella la moneta dall'elenco delle Emoney.
        * @param Moneta
         */
        public static void Pers_Emoney_Cancella(String Moneta) {
        if (Moneta == null || Moneta.isEmpty()) {
            throw new IllegalArgumentException("Moneta non può essere nullo o vuoto.");
        }
        try {
            String sql = "DELETE FROM EMONEY WHERE Moneta = ?";
            PreparedStatement statement = connectionPersonale.prepareStatement(sql);
            statement.setString(1, Moneta);
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                //Logger.getLogger(DatabaseH2.class.getName()).log(Level.WARNING, "Nessuna riga eliminata per Moneta: " + Moneta);
                System.out.println("DatabaseH2.Pers_Emoney_Cancella - Nessuna riga eliminate per Moneta: "+Moneta);
            } else {
                Principale.Mappa_EMoney.remove(Moneta);
            }
            statement.close();
        } catch (SQLException ex) {
            //Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, "Errore durante l'eliminazione di Moneta: " + Moneta, ex);
            LoggerGC.ScriviErrore(ex);
            throw new RuntimeException("Errore durante l'accesso al database: " + ex.getMessage(), ex);
        }
    }
        
        public static String Pers_Emoney_Leggi(String Moneta) {
        if (Moneta == null || Moneta.isEmpty()) {
            throw new IllegalArgumentException("Moneta non può essere nullo o vuoto.");
        }
        String Risultato = null;
        try {
            String checkIfExistsSQL = "SELECT Moneta, Data FROM EMONEY WHERE Moneta = ?";
            try (PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL)) {
                checkStatement.setString(1, Moneta);
                try (ResultSet resultSet = checkStatement.executeQuery()) {
                    if (resultSet.next()) {
                        Risultato = resultSet.getString("Data");
                    }
                }
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Risultato;
    }
     
        public static Map<String, String> Pers_Wallets_LeggiTabella() {
            //List<String> tabella= new ArrayList<>();
            Map<String, String> Mappa_Wallet = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            //String Risultato;
        try {
            String checkIfExistsSQL = "SELECT Wallet, Rete FROM WALLETS";
            try (PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL)) {
                try (ResultSet resultSet = checkStatement.executeQuery()) {
                    while (resultSet.next()) {
                        //Il risultato lo restituisco in questa forma per retrocompatibilità
                        //I dati prima venivano infatti messi in un file
                        String Risultato = resultSet.getString("Wallet")+";"+resultSet.getString("Rete");
                        String ID = resultSet.getString("Wallet")+"_"+resultSet.getString("Rete");
                        Mappa_Wallet.put(ID, Risultato);
                        
                        //tabella.add(Risultato);
                    }
                }
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Mappa_Wallet;
    }    
   
    
        
    public static void Pers_Wallets_Scrivi(String Wallet, String Rete) {
      /*  if (Wallet == null || Wallet.isEmpty()) {
            throw new IllegalArgumentException("Wallet non può essere nullo o vuoto.");
        }
        if (Rete == null || Rete.isEmpty()) {
            throw new IllegalArgumentException("Rete non può essere nulla o vuota.");
        }*/

        String walletRete = Wallet + "_" + Rete;

        String sql = """
        MERGE INTO WALLETS (Wallet_Rete, Wallet, Rete)
        KEY (Wallet_Rete)
        VALUES (?, ?, ?)
    """;

        try (PreparedStatement ps = connectionPersonale.prepareStatement(sql)) {
            ps.setString(1, walletRete);
            ps.setString(2, Wallet);
            ps.setString(3, Rete);
            ps.executeUpdate();
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }

        
        
        
        
        public static void Pers_Wallets_Cancella(String IDWallet) {
               //completamente da gestire
        try {
            String checkIfExistsSQL = "DELETE FROM WALLETS WHERE Wallet_Rete = ?";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            checkStatement.setString(1, IDWallet);
            checkStatement.executeUpdate();

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }
        
        public static void Pers_Emoney_PopolaMappaEmoney() {

        Principale.Mappa_EMoney.clear();
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT * FROM EMONEY";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            //System.out.println(resultSet.getFetchSize());
            while (resultSet.next()) {
                String Moneta = resultSet.getString("Moneta"); 
                String Data = resultSet.getString("Data");
                Principale.Mappa_EMoney.put(Moneta, Data);
                //System.out.println(Moneta);
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }


        //Con questa query ritorno sia il vecchio che il nuovo nome
    }        
        
        private static String NormalizzaCampo(String campo){
            //Questa funzione serve per pulire le stringhe dai campi che potrebbero far casino nelle query del database
            return campo.replace("'", "''");
        }
    
    /**
     * Posiziona il Wallet in uno specifico gruppo
     *
     * @param Wallet Il Wallet di riferimento
     * @param Gruppo Il Gruppo Wallet dove deve finire
     * 
     * @throws IllegalArgumentException se il wallet o il Gruppo sono blank o null
     */
    public static void Pers_GruppoWallet_Scrivi_OLD(String Wallet, String Gruppo) {
        if (Wallet == null || Wallet.isEmpty()) {
            throw new IllegalArgumentException("Wallet non pu\u00f2 essere nullo o vuoto.");
        }
        if (Gruppo == null || Gruppo.isEmpty()) {
            throw new IllegalArgumentException("Gruppo non pu\u00f2 essere nullo o vuoto.");
        }       
        try {
            //Wallet = NormalizzaCampo(Wallet);
            //Gruppo = NormalizzaCampo(Gruppo);
            String checkIfExistsSQL = "SELECT COUNT(*) FROM WALLETGRUPPO WHERE Wallet = ?";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            checkStatement.setString(1, Wallet);
            int rowCount = 0;
            ResultSet resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                String updateSQL = "UPDATE WALLETGRUPPO SET Gruppo = ? WHERE Wallet = ?";
                PreparedStatement updateStatement = connectionPersonale.prepareStatement(updateSQL);
                updateStatement.setString(1, Gruppo);
                updateStatement.setString(2, Wallet);
                updateStatement.executeUpdate();
            } else {
                String insertSQL = "INSERT INTO WALLETGRUPPO (Wallet, Gruppo ) VALUES (?, ?)";
                PreparedStatement insertStatement = connectionPersonale.prepareStatement(insertSQL);
                insertStatement.setString(1, Wallet);
                insertStatement.setString(2, Gruppo);
                insertStatement.executeUpdate();
            }
            Mappa_Wallet_Gruppo.put(Wallet, Gruppo);
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }
        
    /**
     * Posiziona il Wallet in uno specifico gruppo
     *
     * @param Wallet Il Wallet di riferimento
     * @param Gruppo Il Gruppo Wallet dove deve finire
     *
     * @throws IllegalArgumentException se il wallet o il Gruppo sono blank o
     * null
     */
    public static void Pers_GruppoWallet_Scrivi(String Wallet, String Gruppo) {
        if (Wallet == null || Wallet.isEmpty()) {
            throw new IllegalArgumentException("Wallet non pu\u00f2 essere nullo o vuoto.");
        }
        if (Gruppo == null || Gruppo.isEmpty()) {
            throw new IllegalArgumentException("Gruppo non pu\u00f2 essere nullo o vuoto.");
        }

        String sql = """
        MERGE INTO WALLETGRUPPO (Wallet, Gruppo)
        KEY (Wallet)
        VALUES (?, ?)
    """;

        try (PreparedStatement ps = connectionPersonale.prepareStatement(sql)) {
            ps.setString(1, Wallet);
            ps.setString(2, Gruppo);
            ps.executeUpdate();

            // Aggiorna la mappa in memoria
            Mappa_Wallet_Gruppo.put(Wallet, Gruppo);

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }

    
    
    
    
    /**
     *
     * @param Wallet
     * @param ritornaWallet1seNull
     * @return
     */
    public static String Pers_GruppoWallet_Leggi(String Wallet,boolean ritornaWallet1seNull) {
        if (Wallet == null || Wallet.isEmpty()) {
            //return null;
            throw new IllegalArgumentException("Wallet non può essere nullo o vuoto.");
        }
        String Risultato = Mappa_Wallet_Gruppo.get(Wallet);
        if (Risultato == null) {
            try {
                //Wallet = NormalizzaCampo(Wallet);
                String checkIfExistsSQL = "SELECT Wallet,Gruppo FROM WALLETGRUPPO WHERE Wallet = ?";
                try (PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL)) {
                    checkStatement.setString(1, Wallet);
                    try (ResultSet resultSet = checkStatement.executeQuery()) {
                        if (resultSet.next()) {
                            Risultato = resultSet.getString("Gruppo");
                           // System.out.println(Wallet+" - "+Risultato);
                        }
                    }
                }
            } catch (SQLException ex) {
                LoggerGC.ScriviErrore(ex);
            }
        }
        
        if (Risultato == null&&ritornaWallet1seNull) {
            Pers_GruppoWallet_Scrivi(Wallet, "Wallet 01");
            Risultato = "Wallet 01";
        }
        return Risultato;
    }
        
    /**
     *
     * @param Gruppo
     * @return
     */
    public static String[] Pers_GruppoAlias_Leggi(String Gruppo) {
        String[] Risultato = new String[3];
        if (Gruppo == null || Gruppo.isEmpty()) {
            throw new IllegalArgumentException("Gruppo non pu\u00f2 essere nullo o vuoto.");
        }
        try {
            String checkIfExistsSQL = "SELECT Gruppo,Alias,PagaBollo FROM GRUPPO_ALIAS WHERE Gruppo = ?";
            try (PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL)) {
                checkStatement.setString(1, Gruppo);
                try (ResultSet resultSet = checkStatement.executeQuery()) {
                    if (resultSet.next()) {
                        Risultato[0] = resultSet.getString("Gruppo");
                        Risultato[1] = resultSet.getString("Alias");
                        Risultato[2] = resultSet.getString("PagaBollo");
                    }
                }
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Risultato;
    }
        
    /**
     *
     * @return
     */
    public static Map<String, String[]> Pers_GruppoAlias_LeggiTabella() {
        Map<String, String[]> Mappa_GruppiAlias = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try {
            String checkIfExistsSQL = "SELECT Gruppo, Alias, PagaBollo FROM GRUPPO_ALIAS";
            try (PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL); 
                    ResultSet resultSet = checkStatement.executeQuery()) {
                while (resultSet.next()) {
                    String[] Risultato = new String[3];
                    Risultato[0] = resultSet.getString("Gruppo");
                    Risultato[1] = resultSet.getString("Alias");
                    Risultato[2] = resultSet.getString("PagaBollo");
                    Mappa_GruppiAlias.put(Risultato[0], Risultato);
                }
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Mappa_GruppiAlias;
    }
       
 
    /**
     *
     * @param Gruppo
     * @param Alias
     * @param PagaBollo
     */
    public static void Pers_GruppoAlias_Scrivi(String Gruppo, String Alias, boolean PagaBollo) {
    String Pagabollo = PagaBollo ? "S" : "N";
       
    Map<String, Object> values = new HashMap<>();
    values.put("Gruppo", Gruppo);
    values.put("Alias", Alias);
    values.put("Pagabollo", Pagabollo);
    U_ScriviRecord("GRUPPO_ALIAS", values, "Gruppo",connectionPersonale);
       
   /* String sql = """
        MERGE INTO GRUPPO_ALIAS (Gruppo, Alias, Pagabollo)
        KEY (Gruppo)
        VALUES (?, ?, ?)
    """;

    try (PreparedStatement ps = connectionPersonale.prepareStatement(sql)) {
        ps.setString(1, Gruppo);
        ps.setString(2, Alias);
        ps.setString(3, Pagabollo);
        ps.executeUpdate();
    } catch (SQLException ex) {
        LoggerGC.ScriviErrore(ex);
    }*/
}

    
    
    
        public static String[] Pers_ExchangeApi_Leggi(String Nome) {
                String Risultato[] = new String[4];
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Nome,Exchange,Chiave,Segreto FROM EXCHANGEAPI WHERE Nome = '" + Nome + "'";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato[0] = resultSet.getString("Nome");
                Risultato[1] = resultSet.getString("Exchange");
                Risultato[2] = resultSet.getString("Chiave");
                Risultato[3] = resultSet.getString("Segreto");
            }

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Risultato;
        //Con questa query ritorno sia il vecchio che il nuovo nome
    }   
        
        
            public static Map<String, String[]> Pers_ExchangeApi_LeggiTabella() {
            //List<String> tabella= new ArrayList<>();
            Map<String, String[]> Mappa_Wallet = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            //String Risultato;
        try {
            String checkIfExistsSQL = "SELECT Nome,Exchange,Chiave,Segreto FROM EXCHANGEAPI";
            try (PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL)) {
                try (ResultSet resultSet = checkStatement.executeQuery()) {
                    while (resultSet.next()) {
                        String record[]=new String[4];
                        record[0] = resultSet.getString("Nome");
                        record[1] = resultSet.getString("Exchange");
                        record[2] = resultSet.getString("Chiave");
                        record[3] = resultSet.getString("Segreto");
                        Mappa_Wallet.put(record[0], record);
                    }
                }
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Mappa_Wallet;
    }     
            
            
        public static void Pers_ExchangeApi_Scrivi(String Exchange,String Chiave,String Segreto) {
        try {
            
            String checkIfExistsSQL = "SELECT COUNT(*) FROM EXCHANGEAPI WHERE Nome = ?";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            checkStatement.setString(1, Exchange);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                //Questa non dovrebbe servire a nulla perchè non devo mai aggiornare i valori di questa tabella ma solo cancellare e ricreare
                String updateSQL = "UPDATE EXCHANGEAPI SET Exchange = ?,Chiave = ?,Segreto = ? WHERE Nome = ?";
                PreparedStatement updateStatement = connectionPersonale.prepareStatement(updateSQL);
                updateStatement.setString(1, Exchange);
                updateStatement.setString(2, Chiave);
                updateStatement.setString(3, Segreto);
                updateStatement.setString(4, Exchange);
                updateStatement.executeUpdate();

            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO EXCHANGEAPI (Nome, Exchange, Chiave, Segreto) VALUES (?, ?, ?, ?)";
                PreparedStatement insertStatement = connectionPersonale.prepareStatement(insertSQL);
                insertStatement.setString(1, Exchange);
                insertStatement.setString(2, Exchange);
                insertStatement.setString(3, Chiave);
                insertStatement.setString(4, Segreto);
                insertStatement.executeUpdate();

            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }    
    
        
    //Serve per inserire nel database i token di cui devo trovare le coppie usando le api di binance per scaricare i movimenti di trade    
    public static void Pers_ExchangeTokens_Scrivi(String Exchange, String Token) {
        Map<String, Object> values = new HashMap<>();
        values.put("Exchange_Token", Exchange + "_" + Token);
        values.put("Exchange", Exchange);
        values.put("Token", Token);
        U_ScriviRecord("EXCHANGETOKENS", values, "Exchange_Token",connectionPersonale);

    }      
        
    public static void U_ScriviRecord_OLD(String tableName, Map<String, Object> fieldValues, String primaryKeyColumn,Connection con) {
    try {
        // Prendo il valore della chiave primaria
        Object primaryKeyValue = fieldValues.get(primaryKeyColumn);
        if (primaryKeyValue == null) {
            throw new IllegalArgumentException("La mappa deve contenere il campo chiave: " + primaryKeyColumn);
        }

        // 1. Controllo se la riga esiste
        String checkSQL = "SELECT COUNT(*) FROM " + tableName + " WHERE " + primaryKeyColumn + " = ?";
        try (PreparedStatement checkStmt = con.prepareStatement(checkSQL)) {
            checkStmt.setObject(1, primaryKeyValue);
            ResultSet rs = checkStmt.executeQuery();
            boolean exists = false;
            if (rs.next()) {
                exists = rs.getInt(1) > 0;
            }

            if (exists) {
                // 2. UPDATE dinamico
                StringBuilder updateSQL = new StringBuilder("UPDATE " + tableName + " SET ");
                for (String col : fieldValues.keySet()) {
                    if (!col.equals(primaryKeyColumn)) {
                        updateSQL.append(col).append(" = ?, ");
                    }
                }
                updateSQL.setLength(updateSQL.length() - 2); // tolgo ultima virgola
                updateSQL.append(" WHERE ").append(primaryKeyColumn).append(" = ?");

                try (PreparedStatement updateStmt = con.prepareStatement(updateSQL.toString())) {
                    int i = 1;
                    for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
                        if (!entry.getKey().equals(primaryKeyColumn)) {
                            updateStmt.setObject(i++, entry.getValue());
                        }
                    }
                    updateStmt.setObject(i, primaryKeyValue); // condizione WHERE
                    updateStmt.executeUpdate();
                }

            } else {
                // 3. INSERT dinamico
                StringBuilder insertSQL = new StringBuilder("INSERT INTO " + tableName + " (");
                StringBuilder valuesSQL = new StringBuilder("VALUES (");
                for (String col : fieldValues.keySet()) {
                    insertSQL.append(col).append(", ");
                    valuesSQL.append("?, ");
                }
                insertSQL.setLength(insertSQL.length() - 2);
                valuesSQL.setLength(valuesSQL.length() - 2);
                insertSQL.append(") ").append(valuesSQL).append(")");

                try (PreparedStatement insertStmt = con.prepareStatement(insertSQL.toString())) {
                    int i = 1;
                    for (Object value : fieldValues.values()) {
                        insertStmt.setObject(i++, value);
                    }
                    insertStmt.executeUpdate();
                }
            }
        }

    } catch (SQLException ex) {
        LoggerGC.ScriviErrore(ex);
    }
}
    
    
    
    /**
 * Inserisce o aggiorna dinamicamente un record in una tabella del database utilizzando un'unica istruzione SQL {@code MERGE INTO}.
 * <p>
 * Il metodo analizza la mappa di campi {@code fieldValues}, individua la chiave primaria specificata da {@code primaryKeyColumn}
 * e costruisce dinamicamente una query {@code MERGE INTO} che effettua un upsert (inserimento o aggiornamento) del record.
 * <p>
 * Esempio di query generata:
 * <pre>
 * MERGE INTO CLIENTI (ID, Nome, Saldo)
 * KEY (ID)
 * VALUES (?, ?, ?)
 * </pre>
 *
 * <h3>Comportamento</h3>
 * <ul>
 *   <li>Se il record con la chiave primaria esiste → viene aggiornato con i nuovi valori.</li>
 *   <li>Se il record non esiste → viene inserita una nuova riga.</li>
 * </ul>
 *
 * <h3>Note</h3>
 * <ul>
 *   <li>È necessario che la tabella abbia una chiave primaria o un vincolo univoco sulla colonna specificata da {@code primaryKeyColumn}.</li>
 *   <li>Tutte le colonne specificate nella mappa vengono incluse nella clausola {@code VALUES}.</li>
 *   <li>Il metodo è compatibile con H2 e altri database che supportano la sintassi {@code MERGE INTO}.</li>
 * </ul>
 *
 * @param tableName         nome della tabella in cui scrivere il record (es. {@code "CLIENTI"}).
 * @param fieldValues       mappa contenente le coppie {@code nomeColonna → valore} del record da inserire o aggiornare.
 *                          Deve includere anche la chiave primaria.
 * @param primaryKeyColumn  nome della colonna che rappresenta la chiave primaria nella tabella.
 * @param con               connessione JDBC aperta verso il database.
 *
 * @throws IllegalArgumentException se la mappa {@code fieldValues} non contiene la chiave primaria specificata.
 * @throws SQLException se si verifica un errore SQL durante l'esecuzione della query.
 *
 * @see java.sql.Connection
 * @see java.sql.PreparedStatement
 * @see java.sql.SQLException
 */

    public static void U_ScriviRecord(String tableName, Map<String, Object> fieldValues, String primaryKeyColumn, Connection con) {
    try {
        //1 Validazione chiave primaria
        Object primaryKeyValue = fieldValues.get(primaryKeyColumn);
        if (primaryKeyValue == null) {
            throw new IllegalArgumentException("La mappa deve contenere il campo chiave: " + primaryKeyColumn);
        }

        //2 Costruzione dinamica delle colonne
        StringBuilder columnsSQL = new StringBuilder();
        StringBuilder placeholdersSQL = new StringBuilder();
        for (String col : fieldValues.keySet()) {
            columnsSQL.append(col).append(", ");
            placeholdersSQL.append("?, ");
        }
        columnsSQL.setLength(columnsSQL.length() - 2);       // rimuove ", "
        placeholdersSQL.setLength(placeholdersSQL.length() - 2);

       //3 Costruzione della query MERGE dinamica
        String sql = String.format("""
            MERGE INTO %s (%s)
            KEY (%s)
            VALUES (%s)
        """, tableName, columnsSQL, primaryKeyColumn, placeholdersSQL);

        //4 Esecuzione con PreparedStatement
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            for (Object value : fieldValues.values()) {
                ps.setObject(i++, value);
            }
            ps.executeUpdate();
        }

    } catch (SQLException ex) {
        LoggerGC.ScriviErrore(ex);
    }
}


        
    public static List<Map<String, Object>> U_LeggiRecords(
            String tableName,
            List<String> columnsToRead,
            Map<String, Object> filters,
            Connection con) {

        List<Map<String, Object>> results = new ArrayList<>();

        try {
            // 1. Costruisco la SELECT
            String cols = (columnsToRead == null || columnsToRead.isEmpty())
                    ? "*"
                    : String.join(", ", columnsToRead);

            StringBuilder sql = new StringBuilder("SELECT " + cols + " FROM " + tableName);

            if (filters != null && !filters.isEmpty()) {
                sql.append(" WHERE ");
                for (String col : filters.keySet()) {
                    sql.append(col).append(" = ? AND ");
                }
                sql.setLength(sql.length() - 5); // tolgo ultimo " AND "
            }

            try (PreparedStatement stmt = con.prepareStatement(sql.toString())) {
                // 2. Aggiungo i parametri del filtro
                if (filters != null) {
                    int i = 1;
                    for (Object value : filters.values()) {
                        stmt.setObject(i++, value);
                    }
                }

                // 3. Eseguo query e riempio la lista
                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            row.put(meta.getColumnName(i), rs.getObject(i));
                        }
                        results.add(row);
                    }
                }
            }

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }

    return results;
}
      
        
        
    public static List<String> Pers_ExchangeTokens_LeggiTokensExchange(String Exchange) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("Exchange", Exchange);

        //Questa la lista di tutti i token dell'exchange
        List<Map<String, Object>> rows = U_LeggiRecords(
                "EXCHANGETOKENS",
                List.of("Token".toUpperCase()),
                filters,
                connectionPersonale
        );

        // Ritorno solo la lista di String relativa alla colonna Token
        List<String> tokens = rows.stream()
                .map(r -> (String) r.get("Token".toUpperCase()))
                .toList();
        
        return tokens;
    }
        
        public static void Pers_ExchangeTokens_Cancella(String Exchange,String Token) {
        //completamente da gestire
        Map<String, Object> filters = new HashMap<>();
        filters.put("Exchange_Token", Exchange + "_" + Token);
        U_CancellaRecords("EXCHANGETOKENS", filters);

    }    
        
     public static void U_CancellaRecords(String tableName, Map<String, Object> filters) {
    try {
        if (filters == null || filters.isEmpty()) {
            throw new IllegalArgumentException("È necessario specificare almeno una condizione per cancellare i record.");
        }

        // 1. Costruisco la DELETE dinamica
        StringBuilder sql = new StringBuilder("DELETE FROM " + tableName + " WHERE ");
        for (String col : filters.keySet()) {
            sql.append(col).append(" = ? AND ");
        }
        sql.setLength(sql.length() - 5); // rimuovo ultimo " AND "

        try (PreparedStatement stmt = connectionPersonale.prepareStatement(sql.toString())) {
            // 2. Setto i parametri
            int i = 1;
            for (Object value : filters.values()) {
                stmt.setObject(i++, value);
            }

            // 3. Eseguo la DELETE
            stmt.executeUpdate();
        }

    } catch (SQLException ex) {
        LoggerGC.ScriviErrore(ex);
    }
}
   
    
            public static void Pers_ExchangeApi_Cancella(String Exchange) {
               //completamente da gestire
        try {
            String checkIfExistsSQL = "DELETE FROM EXCHANGEAPI WHERE Nome = ?";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            checkStatement.setString(1, Exchange);
            checkStatement.executeUpdate();

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }    
        
    
    /**
     *
     * @param address_chain
     * @param VecchioNome
     * @param NuovoNome
     */
    public static void RinominaToken_Scrivi(String address_chain, String VecchioNome, String NuovoNome) {
        if (address_chain == null || address_chain.isEmpty()) {
            throw new IllegalArgumentException("address_chain non pu\u00f2 essere nullo o vuoto.");
        }
        if (VecchioNome == null || VecchioNome.isEmpty()) {
            throw new IllegalArgumentException("VecchioNome non pu\u00f2 essere nullo o vuoto.");
        }
        if (NuovoNome == null || NuovoNome.isEmpty()) {
            throw new IllegalArgumentException("NuovoNome non pu\u00f2 essere nullo o vuoto.");
        }
        try {
            String checkIfExistsSQL = "SELECT COUNT(*) FROM RINOMINATOKEN WHERE address_chain = ?";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.setString(1, address_chain);
            int rowCount = 0;
            ResultSet resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                String updateSQL = "UPDATE RINOMINATOKEN SET NuovoNome = ? WHERE address_chain = ?";
                PreparedStatement updateStatement = connection.prepareStatement(updateSQL);
                updateStatement.setString(1, NuovoNome);
                updateStatement.setString(2, address_chain);
                updateStatement.executeUpdate();
            } else {
                String insertSQL = "INSERT INTO RINOMINATOKEN (address_chain, VecchioNome, NuovoNome) VALUES (?, ?, ?)";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                insertStatement.setString(1, address_chain);
                insertStatement.setString(2, VecchioNome);
                insertStatement.setString(3, NuovoNome);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }
        
        public static String[] RinominaToken_Leggi(String address_chain) {
                String Risultato[] = new String[2];
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT address_chain,VecchioNome,NuovoNome FROM RINOMINATOKEN WHERE address_chain = '" + address_chain + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato[0] = resultSet.getString("VecchioNome");
                Risultato[1] = resultSet.getString("NuovoNome");
            }

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Risultato;
        //Con questa query ritorno sia il vecchio che il nuovo nome
    }
        
        public static String GiacenzeWalletMonetaBlockchain_Leggi(String wallet_blocco) {
                String Valore = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Valore FROM GIACENZEBLOCKCHAIN WHERE Wallet_Blocco = '" + wallet_blocco + "'";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Valore = resultSet.getString("Valore");
            }

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Valore;
        //Con questa query ritorno sia il vecchio che il nuovo nome
    }    
        
        public static void GiacenzeWalletMonetaBlockchain_Scrivi(String wallet_blocco, String Valore) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM GIACENZEBLOCKCHAIN WHERE Wallet_Blocco = '" + wallet_blocco + "'";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE GIACENZEBLOCKCHAIN SET Valore = '" + Valore + "' WHERE Wallet_Blocco = '" + wallet_blocco + "'";
                PreparedStatement updateStatement = connectionPersonale.prepareStatement(updateSQL);
                updateStatement.executeUpdate();
            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO GIACENZEBLOCKCHAIN (Wallet_Blocco, Valore) VALUES ('" + wallet_blocco + "','" + Valore + "')";
                PreparedStatement insertStatement = connectionPersonale.prepareStatement(insertSQL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    } 
        
        public static Map<String, String> RinominaToken_LeggiTabella() {
        Map<String, String> Mappa_NomiToken = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT * FROM RINOMINATOKEN";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            while (resultSet.next()) {
                String Nome = resultSet.getString("NuovoNome");
                String ID = resultSet.getString("address_chain");
                Mappa_NomiToken.put(ID, Nome);
            }

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Mappa_NomiToken;
        //Con questa query ritorno sia il vecchio che il nuovo nome
    }
        
        public static void RinominaToken_CancellaRiga(String address_chain) {
               //completamente da gestire
        try {
            String checkIfExistsSQL = "DELETE FROM RINOMINATOKEN WHERE address_chain='"+address_chain+"'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.executeUpdate();

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        //Con questa query ritorno sia il vecchio che il nuovo nome
    }
    

    public static String PrezzoAddressChain_Leggi(String ora_address_chain) {
        //System.out.println(ora_address_chain);
        String Risultato = null;
        String OAC[]=ora_address_chain.split("_");
        if (OAC.length<3)
        {
            System.out.println("Errore nella funzione DatabaseH2.PrezzoAddressChain_Leggi, stringa con errore : "+ora_address_chain);
            return null;
        }
        if (!OAC[2].equals("SOL")) ora_address_chain=ora_address_chain.toUpperCase();
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT ora_address_chain,prezzo FROM Prezzo_ora_Address_Chain WHERE ora_address_chain = '" + ora_address_chain + "'";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("prezzo");
            }
            if (Risultato==null){
                //Risultato è null se non ho trovato prezzi personalizzati, nel qual caso cerco tra i prezzi globali
                checkIfExistsSQL = "SELECT ora_address_chain,prezzo FROM Prezzo_ora_Address_Chain WHERE ora_address_chain = '" + ora_address_chain + "'";
                checkStatement = connection.prepareStatement(checkIfExistsSQL);
                resultSet = checkStatement.executeQuery();
                if (resultSet.next()) {
                    Risultato = resultSet.getString("prezzo");
                }
            }

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Risultato;
    }
    
    public static String PrezzoAddressChainPers_Leggi(String ora_address_chain) {
        //System.out.println(ora_address_chain);
        String Risultato = null;
        String OAC[]=ora_address_chain.split("_");
        if (OAC.length<3)
        {
            System.out.println("Errore nella funzione DatabaseH2.PrezzoAddressChain_Leggi, stringa con errore : "+ora_address_chain);
            return null;
        }
        if (!OAC[2].equals("SOL")) ora_address_chain=ora_address_chain.toUpperCase();
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT ora_address_chain,prezzo FROM Prezzo_ora_Address_Chain WHERE ora_address_chain = '" + ora_address_chain + "'";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("prezzo");
            }

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Risultato;
    }

    public static void PrezzoAddressChain_Scrivi_OLD(String ora_address_chain, String prezzo,boolean personalizzato) {
        try {
            Connection connessione;
            if (personalizzato) connessione=connectionPersonale;
            else connessione=connection;
            // Connessione al database
            //String OAC[]=ora_address_chain.split("_");
            //ora_address_chain=ora_address_chain.toUpperCase();
            
            //Se rete solana l'address è case sensitive per cui non posso gestirlo metterlo maiuscolo
            //System.out.println(ora_address_chain);
            String OAC[]=ora_address_chain.split("_");
            if (!OAC[2].equals("SOL")) ora_address_chain=ora_address_chain.toUpperCase();
            
            String checkIfExistsSQL = "SELECT COUNT(*) FROM Prezzo_ora_Address_Chain WHERE ora_address_chain = '" + ora_address_chain + "'";
            //System.out.println(checkIfExistsSQL);
            PreparedStatement checkStatement = connessione.prepareStatement(checkIfExistsSQL);
            //checkStatement.setString(1, address_chain);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE Prezzo_ora_Address_Chain SET prezzo = '" + prezzo + "' WHERE ora_address_chain = '" + ora_address_chain + "'";
                PreparedStatement updateStatement = connessione.prepareStatement(updateSQL);
                // updateStatement.setString(1, data);
                //updateStatement.setString(2, address_chain);
                updateStatement.executeUpdate();
                // System.out.println("Riga aggiornata con successo.");

            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO Prezzo_ora_Address_Chain (ora_address_chain, prezzo) VALUES ('" + ora_address_chain + "','" + prezzo + "')";
                PreparedStatement insertStatement = connessione.prepareStatement(insertSQL);
                //insertStatement.setString(1, address_chain);
                //insertStatement.setString(2, data);
                insertStatement.executeUpdate();
                // System.out.println("Nuova riga inserita con successo.");

            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }
    
    public static void OLD_PrezzoAddressChain_Scrivi(String ora_address_chain, String prezzo, boolean personalizzato) {
    try {
        // Seleziona la connessione corretta
        Connection connessione = personalizzato ? connectionPersonale : connection;

        // Suddivide e normalizza la chiave
        //Se rete solana l'address è case sensitive per cui non posso gestirlo metterlo maiuscolo
        String[] OAC = ora_address_chain.split("_");
        if (OAC.length >= 3 && !OAC[2].equalsIgnoreCase("SOL")) {
            ora_address_chain = ora_address_chain.toUpperCase();
        }

        String sql = """
            MERGE INTO Prezzo_ora_Address_Chain (ora_address_chain, prezzo)
            KEY (ora_address_chain)
            VALUES (?, ?)
        """;

        try (PreparedStatement ps = connessione.prepareStatement(sql)) {
            ps.setString(1, ora_address_chain);
            ps.setString(2, prezzo);
            ps.executeUpdate();
        }

    } catch (SQLException ex) {
        LoggerGC.ScriviErrore(ex);
    }
}



  
    
       public static String XXXEUR_Leggi(String dataSimbolo) {
       
        String Risultato = null;
        try {
            // Connessione al database
            String SQL = "SELECT dataSimbolo,prezzo FROM XXXEUR WHERE dataSimbolo = '" + dataSimbolo + "'";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(SQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("prezzo");
            }
            if (Risultato==null){
                //Risultato è null se non ho trovato prezzi personalizzati, nel qual caso cerco tra i prezzi globali
                SQL = "SELECT dataSimbolo,prezzo FROM XXXEUR WHERE dataSimbolo = '" + dataSimbolo + "'";
                checkStatement = connection.prepareStatement(SQL);
                resultSet = checkStatement.executeQuery();
                if (resultSet.next()) {
                    Risultato = resultSet.getString("prezzo");
                }
            }

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        if (Risultato!=null && Risultato.equalsIgnoreCase("null"))
            {
                System.out.println("DatabaseH2.XXXEUR_LEGGI prezzo Errato "+dataSimbolo);
            return null;
            }
        return Risultato;
    }
   
           public static String XXXEUR_LeggiPers(String dataSimbolo) {
       
        String Risultato = null;
        try {
            // Connessione al database
            String SQL = "SELECT dataSimbolo,prezzo FROM XXXEUR WHERE dataSimbolo = '" + dataSimbolo + "'";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(SQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("prezzo");
            }

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        if (Risultato!=null && Risultato.equalsIgnoreCase("null"))
            {
                System.out.println("DatabaseH2.XXXEUR_LEGGI prezzo Errato "+dataSimbolo);
            return null;
            }
        return Risultato;
    }
    
    public static void OLD_XXXEUR_Scrivi(String dataSimbolo, String prezzo, boolean personalizzato) {
        if (dataSimbolo == null || dataSimbolo.isEmpty()) {
            throw new IllegalArgumentException("dataSimbolo non può essere nullo o vuoto.");
        }

        Connection connessione = personalizzato ? connectionPersonale : connection;

        String sql = """
        MERGE INTO XXXEUR (dataSimbolo, prezzo)
        KEY (dataSimbolo)
        VALUES (?, ?)
    """;

        try (PreparedStatement ps = connessione.prepareStatement(sql)) {
            ps.setString(1, dataSimbolo);
            ps.setString(2, prezzo);
            ps.executeUpdate();
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }

    /**
     *Questa funzione ritorna null se la coppia su binance non esiste altrimenti ritorna il nome della coppia
     * 
     * @param Coppia
     * @return
     */
    public static String CoppieBinance_Leggi(String Coppia) {
        String Risultato = null;
        try {
            String checkIfExistsSQL = "SELECT Coppia FROM GESTITIBINANCE WHERE Coppia = ?";
            try (PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL)) {
                checkStatement.setString(1, Coppia);
                try (ResultSet resultSet = checkStatement.executeQuery()) {
                    if (resultSet.next()) {
                        Risultato = resultSet.getString("Coppia");
                    }
                }
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Risultato;
    }
    
        /**
     *Questa funzione ritorna null se il simbolo su coinbase non esiste altrimenti ritorna il simbolo
     * 
     * @param Simbolo
     * @return
     */
    public static String GestitiCoinbase_Leggi(String Simbolo) {
        String Risultato = null;
        try {
            String checkIfExistsSQL = "SELECT Simbolo FROM GESTITICOINBASE WHERE Simbolo = ?";
            try (PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL)) {
                checkStatement.setString(1, Simbolo);
                try (ResultSet resultSet = checkStatement.executeQuery()) {
                    if (resultSet.next()) {
                        Risultato = resultSet.getString("Simbolo");
                    }
                }
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Risultato;
    }
        
    /**
     *
     * @param Gestito
     * @return
     */
    public static String GestitiCoingecko_Leggi(String Gestito) {
        String Risultato = null;
        if (Gestito.split("_").length<2)return null;
        String Rete = Gestito.split("_")[1];
        if (!Rete.equals("SOL")) {
            Gestito = Gestito.toUpperCase();
        }
        try (PreparedStatement checkStatement = connection.prepareStatement("SELECT Address_Chain FROM GESTITICOINGECKO WHERE Address_Chain = ?")) {
            checkStatement.setString(1, Gestito);
            try (ResultSet resultSet = checkStatement.executeQuery()) {
                if (resultSet.next()) {
                    Risultato = resultSet.getString("Address_Chain");
                }
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Risultato;
    }
            
        /**
         * 
           * @param Gestito
           * @return 
         */
        public static String[] GestitiCoingecko_LeggiInteraRiga(String Gestito) {
        
        String Risultato[]=new String[3];
        String base[]=Gestito.split("_");
        if (base.length<2)return null;
        //System.out.println(Gestito);
        String Rete=Gestito.split("_")[1];
        if (!Rete.equals("SOL"))Gestito=Gestito.toUpperCase();
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Address_Chain,Simbolo,Nome FROM GESTITICOINGECKO WHERE Address_Chain = '" + Gestito + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            ResultSet resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato[0] = resultSet.getString("Address_Chain");
                Risultato[1] = resultSet.getString("Simbolo");
                Risultato[2] = resultSet.getString("Nome");
            }
            resultSet.close();
            checkStatement.close();

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Risultato;
    } 
                        
      
  
        
        
        public static String GestitiCoinCap_Leggi(String Gestito) {
        String Risultato = null;
        try (PreparedStatement checkStatement = connection.prepareStatement("SELECT NOME FROM GESTITICOINCAP WHERE Symbol = ?")) {
            checkStatement.setString(1, Gestito);
            try (ResultSet resultSet = checkStatement.executeQuery()) {
                if (resultSet.next()) {
                    Risultato = resultSet.getString("NOME");
                }
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Risultato;
    } 
        
        
   
    
        public static String[] GestitiCoinCap_LeggiInteraRiga(String Gestito) {
        String Risultato[] = new String[2];
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Symbol,Nome FROM GESTITICOINCAP WHERE Symbol = '" + Gestito + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato[0] = resultSet.getString("Symbol");
                Risultato[1] = resultSet.getString("Nome");
            }

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Risultato;
    } 
    
        public static void CoppieBinance_ScriviNuovaTabella(List<String> Coppie) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "DROP TABLE IF EXISTS GESTITIBINANCE";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.execute();
            checkIfExistsSQL = "CREATE TABLE IF NOT EXISTS GESTITIBINANCE  (Coppia VARCHAR(255) PRIMARY KEY)";
            checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.execute(); 
            for (String coppia:Coppie){
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO GESTITIBINANCE (Coppia) VALUES ('" + coppia + "')";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    } 
        
    public static void GestitiCoinbase_ScriviNuovaTabella(List<String> Simboli) {
        try {
            String checkIfExistsSQL = "DROP TABLE IF EXISTS GESTITICOINBASE";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.execute();
            checkIfExistsSQL = "CREATE TABLE IF NOT EXISTS GESTITICOINBASE  (Simbolo VARCHAR(255) PRIMARY KEY)";
            checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.execute();
            for (String Simbolo : Simboli) {
                String insertSQL = "INSERT INTO GESTITICOINBASE (Simbolo) VALUES (?)";
                try (PreparedStatement insertStatement = connection.prepareStatement(insertSQL)) {
                    insertStatement.setString(1, Simbolo);
                    insertStatement.executeUpdate();
                }
            }
            checkStatement.close();
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    } 
        
        
        public static void GestitiCoingecko_ScriviNuovaTabella(List<String[]> Gestiti) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "DROP TABLE IF EXISTS GESTITICOINGECKO";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.execute();
            checkIfExistsSQL = "CREATE TABLE IF NOT EXISTS GESTITICOINGECKO  (Address_Chain VARCHAR(255) PRIMARY KEY, Simbolo VARCHAR(255), Nome VARCHAR (255))";
            checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.execute(); 
            for (String[] gestito:Gestiti){
                // La riga non esiste, esegui l'inserimento
                gestito[1]=gestito[1].replace("'", "");
                gestito[2]=gestito[2].replace("'", "");
                gestito[2]=gestito[2].split("\\(")[0].trim();//questo serve per eliminare i nomi delle chain tra parentesi nel nome qualora vi fosse
                String insertSQL = "INSERT INTO GESTITICOINGECKO (Address_Chain,Simbolo,Nome) VALUES ('" + gestito[0] + "','"+ gestito[1] + "','" + gestito[2] + "')";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }
        
      public static void TokenSolana_AggiungiToken(String Address, String Simbolo, String Nome, String Tipo) {
        try {
            // Controllo se il token esiste già
            String checkIfExistsSQL = "SELECT COUNT(*) FROM TOKENSOLANA WHERE Address = ?";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.setString(1, Address);
            ResultSet resultSet = checkStatement.executeQuery();

            int rowCount = 0;
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }

            if (rowCount > 0) {
                // Il token esiste già, aggiorno i dati
                String updateSQL = "UPDATE TOKENSOLANA SET Simbolo = ?, Nome = ?, Tipo = ? WHERE Address = ?";
                PreparedStatement updateStatement = connection.prepareStatement(updateSQL);
                updateStatement.setString(1, Simbolo);
                updateStatement.setString(2, Nome);
                updateStatement.setString(3, Tipo);
                updateStatement.setString(4, Address);
                updateStatement.executeUpdate();
                updateStatement.close();
            } else {
                // Il token non esiste, lo inserisco
                String insertSQL = "INSERT INTO TOKENSOLANA (Address, Simbolo, Nome, Tipo) VALUES (?, ?, ?, ?)";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                insertStatement.setString(1, Address);
                insertStatement.setString(2, Simbolo);
                insertStatement.setString(3, Nome);
                insertStatement.setString(4, Tipo);
                insertStatement.executeUpdate();
                insertStatement.close();
            }

            // Chiudo il ResultSet e lo Statement per evitare memory leak
            resultSet.close();
            checkStatement.close();

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
            //Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, "Errore durante l'inserimento/aggiornamento del token", ex);
        }
    } 
     
        public static String[] TokenSolana_Leggi(String Address) {
        String Risultato[]=new String[3];
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Simbolo,Nome,Tipo FROM TOKENSOLANA WHERE Address = '" + Address + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato[0] = resultSet.getString("Simbolo");
                Risultato[1] = resultSet.getString("Nome");
                Risultato[2] = resultSet.getString("Tipo");
            }else return null;

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Risultato;
    }  
        
      
        
                public static void GestitiCoinCap_ScriviNuovaTabella(List<String[]> Gestiti) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "DROP TABLE IF EXISTS GESTITICOINCAP";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.execute();
            checkIfExistsSQL = "CREATE TABLE IF NOT EXISTS GESTITICOINCAP  (Symbol VARCHAR(255) PRIMARY KEY, Nome VARCHAR(255))";
            checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.execute(); 
            for (String gestito[]:Gestiti){
                // La riga non esiste, esegui l'inserimento
                gestito[1]=gestito[1].replace("'", "").replace("*", "");
                //Inserisco il simbolo della moneta solo se questa non è già stata gestita, anche perchè il simbolo è un campo univoco e mi darebbe errore
                if (GestitiCoinCap_Leggi(gestito[0])==null)
                {
                    String insertSQL = "INSERT INTO GESTITICOINCAP (Symbol,Nome) VALUES ('" + gestito[0] + "','" + gestito[1] + "')";
                    PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                    insertStatement.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }
        
        public static String Opzioni_Leggi(String Opzione) {
        String Risultato = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Opzione,valore FROM OPZIONI WHERE Opzione = '" + Opzione + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("valore");
            }

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Risultato;
    }
    
    public static String Pers_Opzioni_Leggi(String Opzione) {
        String Risultato = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Opzione,valore FROM OPZIONI WHERE Opzione = '" + Opzione + "'";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("valore");
            }

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
        return Risultato;
    }
        
        
    /**
     * Inserisce o aggiorna un'opzione nella tabella OPZIONI generali
     *
     * @param Opzione Nome dell'opzione da salvare.
     * @param Valore Valore associato all'opzione.
     */
    public static void Opzioni_Scrivi(String Opzione, String Valore) {
        if (Opzione == null || Opzione.isEmpty()) {
            throw new IllegalArgumentException("Opzione non può essere nulla o vuota.");
        }

        try {
            String sql = """
            MERGE INTO OPZIONI (Opzione, Valore)
            KEY (Opzione)
            VALUES (?, ?)
        """;

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, Opzione);
                ps.setString(2, Valore);
                ps.executeUpdate();
            }

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }

    
     /**
     * Inserisce o aggiorna un'opzione nella tabella OPZIONI personali
     *
     * @param Opzione Nome dell'opzione da salvare.
     * @param Valore Valore associato all'opzione.
     */           
    public static void Pers_Opzioni_Scrivi(String Opzione, String Valore) {
        if (Opzione == null || Opzione.isEmpty()) {
            throw new IllegalArgumentException("Opzione non può essere nulla o vuota.");
        }

        String sql = """
        MERGE INTO OPZIONI (Opzione, Valore)
        KEY (Opzione)
        VALUES (?, ?)
    """;

        try (PreparedStatement ps = connectionPersonale.prepareStatement(sql)) {
            ps.setString(1, Opzione);
            ps.setString(2, Valore);
            ps.executeUpdate();
        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }

                
                
    public static void Pers_Opzioni_CancellaOpzione(String Opzione) {
        try {
            String checkIfExistsSQL = "DELETE FROM OPZIONI WHERE Opzione='"+Opzione+"'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.executeUpdate();

        } catch (SQLException ex) {
            LoggerGC.ScriviErrore(ex);
        }
    }
                
                
}