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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        
            String createTableSQL = "CREATE TABLE IF NOT EXISTS Prezzo_ora_Address_Chain  (ora_address_chain VARCHAR(255) PRIMARY KEY, prezzo VARCHAR(255))";
            PreparedStatement preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute();
            preparedStatement = connectionPersonale.prepareStatement(createTableSQL);
            preparedStatement.execute();

            createTableSQL = "CREATE TABLE IF NOT EXISTS USDTEUR  (data VARCHAR(255) PRIMARY KEY, prezzo VARCHAR(255))";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute();
            
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
                       
            createTableSQL = "CREATE TABLE IF NOT EXISTS OPZIONI (Opzione VARCHAR(255) PRIMARY KEY, Valore VARCHAR(255))";
            preparedStatement = connection.prepareStatement(createTableSQL);
            preparedStatement.execute();  
            
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
            
        }
        return successo;
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
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
            CDC_Grafica.Mappa_EMoney.put(Moneta, Data);
        } catch (SQLException ex) {
        Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(DatabaseH2.class.getName()).log(Level.WARNING, "Nessuna riga eliminata per Moneta: " + Moneta);
            } else {
                CDC_Grafica.Mappa_EMoney.remove(Moneta);
            }
            statement.close();
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, "Errore durante l'eliminazione di Moneta: " + Moneta, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Mappa_Wallet;
    }    
   
        public static void Pers_Wallets_Scrivi(String Wallet,String Rete) {
        try {
            
            String checkIfExistsSQL = "SELECT COUNT(*) FROM WALLETS WHERE Wallet_Rete = ?";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            checkStatement.setString(1, Wallet+"_"+Rete);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                //Questa non dovrebbe servire a nulla perchè non devo mai aggiornare i valori di questa tabella ma solo cancellare e ricreare
                String updateSQL = "UPDATE WALLETS SET Wallet = ? WHERE Wallet_Rete = ?";
                PreparedStatement updateStatement = connectionPersonale.prepareStatement(updateSQL);
                updateStatement.setString(1, Wallet);
                updateStatement.setString(2, Wallet+"_"+Rete);
                updateStatement.executeUpdate();

            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO WALLETS (Wallet_Rete, Wallet, Rete) VALUES (?, ?, ?)";
                PreparedStatement insertStatement = connectionPersonale.prepareStatement(insertSQL);
                insertStatement.setString(1, Wallet+"_"+Rete);
                insertStatement.setString(2, Wallet);
                insertStatement.setString(3, Rete);
                insertStatement.executeUpdate();

            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
        public static void Pers_Emoney_PopolaMappaEmoney() {

        CDC_Grafica.Mappa_EMoney.clear();
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT * FROM EMONEY";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            //System.out.println(resultSet.getFetchSize());
            while (resultSet.next()) {
                String Moneta = resultSet.getString("Moneta"); 
                String Data = resultSet.getString("Data");
                CDC_Grafica.Mappa_EMoney.put(Moneta, Data);
                //System.out.println(Moneta);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
    public static void Pers_GruppoWallet_Scrivi(String Wallet, String Gruppo) {
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
    /**
     *
     * @param Wallet
     * @return
     */
    public static String Pers_GruppoWallet_Leggi(String Wallet) {
        if (Wallet == null || Wallet.isEmpty()) {
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
                Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (Risultato == null) {
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
        try {
            String checkIfExistsSQL = "SELECT COUNT(*) FROM GRUPPO_ALIAS WHERE Gruppo = ?";
            try (PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL)) {
                checkStatement.setString(1, Gruppo);
                int rowCount = 0;
                try (ResultSet resultSet = checkStatement.executeQuery()) {
                    if (resultSet.next()) {
                        rowCount = resultSet.getInt(1);
                    }
                }
                if (rowCount > 0) {
                    String updateSQL = "UPDATE GRUPPO_ALIAS SET Alias = ?, Pagabollo = ? WHERE Gruppo = ?";
                    try (PreparedStatement updateStatement = connectionPersonale.prepareStatement(updateSQL)) {
                        updateStatement.setString(1, Alias);
                        updateStatement.setString(2, Pagabollo);
                        updateStatement.setString(3, Gruppo);
                        updateStatement.executeUpdate();
                    }
                } else {
                    String insertSQL = "INSERT INTO GRUPPO_ALIAS (Gruppo, Alias, Pagabollo) VALUES (?, ?, ?)";
                    try (PreparedStatement insertStatement = connectionPersonale.prepareStatement(insertSQL)) {
                        insertStatement.setString(1, Gruppo);
                        insertStatement.setString(2, Alias);
                        insertStatement.setString(3, Pagabollo);
                        insertStatement.executeUpdate();
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
    
        
        
    public static void Pers_ExchangeTokens_Scrivi(String Exchange, String Token) {

        Map<String, Object> values = new HashMap<>();
        values.put("Exchange_Token", Exchange + "_" + Token);
        values.put("Exchange", Exchange);
        values.put("Token", Token);
        U_ScriviRecord("EXCHANGETOKENS", values, "Exchange_Token",connectionPersonale);

    }      
        
    public static void U_ScriviRecord(String tableName, Map<String, Object> fieldValues, String primaryKeyColumn,Connection con) {
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
        Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
        Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        //Con questa query ritorno sia il vecchio che il nuovo nome
    }
    
 /*   public static void AddressSenzaPrezzo_Scrivi(String address_chain, String data) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM Address_Senza_Prezzo WHERE address_chain = '" + address_chain + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE Address_Senza_Prezzo SET data = '" + data + "' WHERE address_chain = '" + address_chain + "'";
                PreparedStatement updateStatement = connection.prepareStatement(updateSQL);
                // updateStatement.setString(1, data);
                //updateStatement.setString(2, address_chain);
                updateStatement.executeUpdate();

            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO Address_Senza_Prezzo (address_chain, data) VALUES ('" + address_chain + "','" + data + "')";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                //insertStatement.setString(1, address_chain);
                //insertStatement.setString(2, data);
                insertStatement.executeUpdate();

            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }*/

 /*   public static String AddressSenzaPrezzo_Leggi(String address_chain) {
        String Risultato = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT address_chain,data FROM Address_Senza_Prezzo WHERE address_chain = '" + address_chain + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("data");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    }*/

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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    }

    public static void PrezzoAddressChain_Scrivi(String ora_address_chain, String prezzo,boolean personalizzato) {
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String Obsoleto_USDTEUR_Leggi(String data) {
        String Risultato = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT data,prezzo FROM USDTEUR WHERE data = '" + data + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("prezzo");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    }

    public static void Obsoleto_USDTEUR_Scrivi(String data, String prezzo) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM USDTEUR WHERE data = '" + data + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE USDTEUR SET prezzo = '" + prezzo + "' WHERE data = '" + data + "'";
                PreparedStatement updateStatement = connection.prepareStatement(updateSQL);
                updateStatement.executeUpdate();
            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO USDTEUR (data, prezzo) VALUES ('" + data + "','" + prezzo + "')";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (Risultato!=null && Risultato.equalsIgnoreCase("null"))
            {
                System.out.println("DatabaseH2.XXXEUR_LEGGI prezzo Errato "+dataSimbolo);
            return null;
            }
        return Risultato;
    }

     /*  /**
        * Questa funzione ritorna il prezzo personalizzato di un token se c'è altrimenti ritorna null<br>
        * Il Prezzo è rapportato al quantitativo non è unitario
        * @param moneta
        * Moneta di cui devo cercare il prezzo<br>
        * Dati Obbligatori : NomeToken e la Qta<br>
        * Dati Opzionali : Address e Rete<br>
        * @param timestamp
        * timestamp in formato long relativo all'ora esatta in cui devo cercare il prezzo
        * @return 
        * ritorna null in caso non vi siano prezzi personalizzati<br>
        * ritorna il prezzo rapportato alle qta richieste in caso contrario
        */
 /*      public static String LeggiPrezzoPersonalizzato(Moneta moneta,long timestamp) {
        String dataora=OperazioniSuDate.ConvertiDatadaLongallOra(timestamp);
        String dataSimbolo=dataora+"_"+moneta.Moneta;
        
        String Risultato = null;
        
        try {
            String checkIfExistsSQL;
            PreparedStatement checkStatement;
            if (moneta.MonetaAddress!=null && moneta.Rete!=null){
                //se è un movimento in defi allora cerco il prezzo nella defi
                String ora_address_chain=dataora+"_"+moneta.MonetaAddress+"_"+moneta.Rete;
                if (!moneta.Rete.equalsIgnoreCase("SOL"))ora_address_chain=ora_address_chain.toUpperCase();
                // Connessione al database
                checkIfExistsSQL = "SELECT ora_address_chain,prezzo FROM Prezzo_ora_Address_Chain WHERE ora_address_chain = '" + ora_address_chain + "'";
                checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
                var resultSet = checkStatement.executeQuery();
                if (resultSet.next()) {
                    Risultato = resultSet.getString("prezzo");
                }
            }
            else{
                // altrimenti lo cerco per il solo nome
                // Connessione al database
                checkIfExistsSQL = "SELECT dataSimbolo,prezzo FROM XXXEUR WHERE dataSimbolo = '" + dataSimbolo + "'";
                checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
                var resultSet = checkStatement.executeQuery();
                if (resultSet.next()) {
                    Risultato = resultSet.getString("prezzo");
                }          
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (Risultato!=null && Risultato.equalsIgnoreCase("null"))
            {
                System.out.println("DatabaseH2.XXXEUR_LEGGI prezzo Errato "+dataSimbolo);
            return null;
            }
        if (Risultato!=null&&moneta.Qta!=null){
            //Devo calcolare ora il prezzo rapportato alla Qta
            Risultato=new BigDecimal(Risultato).multiply(new BigDecimal(moneta.Qta)).toPlainString();
        }
        else return null;
        return Risultato;
    }*/
       
    public static void XXXEUR_Scrivi(String dataSimbolo, String prezzo,boolean personalizzato) {
        try {
            String SQL;
            Connection connessione;
            if (personalizzato) connessione=connectionPersonale;
            else connessione=connection;
            // Connessione al database
            SQL = "SELECT COUNT(*) FROM XXXEUR WHERE dataSimbolo = '" + dataSimbolo + "'";
            PreparedStatement checkStatement = connessione.prepareStatement(SQL);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                SQL = "UPDATE XXXEUR SET prezzo = '" + prezzo + "' WHERE dataSimbolo = '" + dataSimbolo + "'";
                PreparedStatement updateStatement = connessione.prepareStatement(SQL);
                updateStatement.executeUpdate();
            } else {
                // La riga non esiste, esegui l'inserimento
                SQL = "INSERT INTO XXXEUR (dataSimbolo, prezzo) VALUES ('" + dataSimbolo + "','" + prezzo + "')";
                PreparedStatement insertStatement = connessione.prepareStatement(SQL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    } 
                        
        public static String Obsoleto_GestitiCryptohistory_Leggi(String Gestito) {
        String Risultato = null;
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Symbol FROM GESTITICRYPTOHISTORY WHERE Symbol = '" + Gestito + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato = resultSet.getString("Symbol");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    } 
        
        
    public static String[] Obsoleto_GestitiCryptohistory_LeggiInteraRiga(String Gestito) {
        String Risultato[] = new String[2];
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT Symbol,Nome FROM GESTITICRYPTOHISTORY WHERE Symbol = '" + Gestito + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                Risultato[0] = resultSet.getString("Symbol");
                Risultato[1] = resultSet.getString("Nome");
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, "Errore durante l'inserimento/aggiornamento del token", ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    }  
        
        public static void Obsoleto_GestitiCryptohistory_ScriviNuovaTabella(List<String[]> Gestiti) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "DROP TABLE IF EXISTS GESTITICRYPTOHISTORY";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.execute();
            checkIfExistsSQL = "CREATE TABLE IF NOT EXISTS GESTITICRYPTOHISTORY  (Symbol VARCHAR(255) PRIMARY KEY, Nome VARCHAR(255))";
            checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.execute(); 
            for (String gestito[]:Gestiti){
                // La riga non esiste, esegui l'inserimento
                gestito[1]=gestito[1].replace("'", "").replace("*", "");
                String insertSQL = "INSERT INTO GESTITICRYPTOHISTORY (Symbol,Nome) VALUES ('" + gestito[0] + "','"+ gestito[1] + "')";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Risultato;
    }
        
        public static void Opzioni_Scrivi(String Opzione, String Valore) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM OPZIONI WHERE Opzione = '" + Opzione + "'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE OPZIONI SET Valore = '" + Valore + "' WHERE Opzione = '" + Opzione + "'";
                PreparedStatement updateStatement = connection.prepareStatement(updateSQL);
                updateStatement.executeUpdate();
            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO OPZIONI (Opzione, Valore) VALUES ('" + Opzione + "','" + Valore + "')";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    } 
    
                public static void Pers_Opzioni_Scrivi(String Opzione, String Valore) {
        try {
            // Connessione al database
            String checkIfExistsSQL = "SELECT COUNT(*) FROM OPZIONI WHERE Opzione = '" + Opzione + "'";
            PreparedStatement checkStatement = connectionPersonale.prepareStatement(checkIfExistsSQL);
            int rowCount = 0;
            // Esegui la query e controlla il risultato
            var resultSet = checkStatement.executeQuery();
            if (resultSet.next()) {
                rowCount = resultSet.getInt(1);
            }
            if (rowCount > 0) {
                // La riga esiste, esegui l'aggiornamento
                String updateSQL = "UPDATE OPZIONI SET Valore = '" + Valore + "' WHERE Opzione = '" + Opzione + "'";
                PreparedStatement updateStatement = connectionPersonale.prepareStatement(updateSQL);
                updateStatement.executeUpdate();
            } else {
                // La riga non esiste, esegui l'inserimento
                String insertSQL = "INSERT INTO OPZIONI (Opzione, Valore) VALUES ('" + Opzione + "','" + Valore + "')";
                PreparedStatement insertStatement = connectionPersonale.prepareStatement(insertSQL);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
                
                
    public static void Pers_Opzioni_CancellaOpzione(String Opzione) {
               //completamente da gestire
        try {
            String checkIfExistsSQL = "DELETE FROM OPZIONI WHERE Opzione='"+Opzione+"'";
            PreparedStatement checkStatement = connection.prepareStatement(checkIfExistsSQL);
            checkStatement.executeUpdate();

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseH2.class.getName()).log(Level.SEVERE, null, ex);
        }
        //Con questa query ritorno sia il vecchio che il nuovo nome
    }
                
                
}